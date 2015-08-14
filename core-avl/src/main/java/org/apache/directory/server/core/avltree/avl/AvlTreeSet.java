/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.core.avltree.avl;


import java.util.Iterator;
import java.util.Stack;


/**
 * AVL Tree Set
 * 
 * @author Vladimir Lysyy (http://bobah.net)
 *
 */
public class AvlTreeSet<T extends Comparable<T>> implements Iterable<T>
{
    private AvlNode<T> tree;
    private int size = 0;

    private final boolean useFreeList;

    private Stack<AvlNode<T>> freeList = new Stack<AvlNode<T>>();


    public AvlTreeSet()
    {
        this( false );
    }


    public AvlTreeSet( boolean useFreeList )
    {
        this.useFreeList = useFreeList;
    }


    public final int height()
    {
        return ( tree == null ) ? 0 : tree.height + 1;
    }


    public final int size()
    {
        return size;
    }


    public final Iterator<T> iterator()
    {
        return new AvlTreeIterator<T>( tree );
    }


    public final boolean insert( T value )
    {
        // empty tree case
        if ( tree == null )
        {
            tree = newNode( null, value );
            ++size;

            return true;
        }

        AvlNode<T> node = tree;

        // find the place and insert the value
        int cmp = value.compareTo( node.value );

        for ( ; cmp != 0; cmp = value.compareTo( node.value ) )
        {
            if ( cmp < 0 )
            {
                if ( node.left == null )
                {
                    node.left = newNode( node, value );
                    break;
                }
                else
                {
                    node = node.left;
                }
            }
            else if ( cmp > 0 )
            {
                if ( node.right == null )
                {
                    node.right = newNode( node, value );
                    break;
                }
                else
                {
                    node = node.right;
                }
            }
            else
            {
                assert false : "should never happen";
            }
        }

        // node with _value_ already exists
        if ( cmp == 0 )
        {
            return false;
        }

        rebalanceUp( node );
        ++size;

        return true;
    }


    private AvlNode<T> newNode( AvlNode<T> parent, T value )
    {
        if ( !useFreeList || freeList.isEmpty() )
        {
            return new AvlNode<T>( parent, value );
        }
        else
        {
            AvlNode<T> node = freeList.pop();

            return node.reset( parent, value );
        }
    }


    private void recycleNode( AvlNode<T> node )
    {
        if ( !useFreeList )
        {
            return;
        }

        // keep free list size not bigger than tree size
        while ( freeList.size() > size )
        {
            freeList.pop();
        }

        if ( freeList.size() == size )
        {
            return;
        }

        freeList.push( node );
    }


    private void rebalanceUp( AvlNode<T> node )
    {
        while ( node != null )
        {
            int heightBefore = node.height;
            updateHeight( node );

            // rebalance
            if ( node.balance == -2 )
            {
                node = bigRightRotation( node );
            }
            else if ( node.balance == 2 )
            {
                node = bigLeftRotation( node );
            }

            if ( node.parent == null )
            {
                tree = node;
            }

            // if parent node is not affected
            if ( heightBefore == node.height )
            {
                break;
            }

            node = node.parent;
        }
    }


    public final boolean remove( T value )
    {
        AvlNode<T> node = tree;

        if ( node == null )
        {
            return false;
        }

        // find the node to be removed
        for ( int cmp = value.compareTo( node.value ); cmp != 0; cmp = value.compareTo( node.value ) )
        {
            node = ( cmp < 0 ) ? node.left : node.right;

            if ( node == null )
            {
                return false;
            }
        }

        // find a replacement node (if needed)
        final int left = -1;
        final int right = 1;
        final int none = 0;
        int replaceFrom = none;

        if ( node.left != null && node.right == null )
        {
            replaceFrom = left;
        }
        else if ( node.right != null && node.left == null )
        {
            replaceFrom = right;
        }
        else if ( node.right != null && node.left != null )
        {
            if ( node.balance < 0 )
            {
                replaceFrom = left;
            }
            else if ( node.balance > 0 )
            {
                replaceFrom = right;
            }
            else
            {
                replaceFrom = left; // TODO: asymmetry
            }
        }
        else
        { // node is itself a leaf, replacement is not needed
            if ( node.parent == null )
            { // the tree root, single node in the tree
                tree = null;
                --size;
                recycleNode( node );

                return true;
            }
            else
            { // non-root leaf node
              // detach from parent
                if ( node.parent.left == node )
                {
                    node.parent.left = null;
                }
                else
                {
                    node.parent.right = null;
                }

                AvlNode<T> dead = node;
                // update heights/rebalance from node's parents up (the bottom of this method)
                node = node.parent;
                recycleNode( dead );
                replaceFrom = none;
            }
        }

        if ( replaceFrom != none )
        {
            AvlNode<T> leaf = null;

            if ( replaceFrom == left )
            {
                leaf = node.left;

                while ( leaf.left != null || leaf.right != null )
                {
                    if ( leaf.right != null )
                    {
                        leaf = leaf.right;
                    }
                    else
                    {
                        // the rotation should ensure (leaf.right != null) on the next iteration
                        leaf = smallRightRotation( leaf );
                    }
                }
            }
            else if ( replaceFrom == right )
            {
                leaf = node.right;

                while ( leaf.right != null || leaf.left != null )
                {
                    if ( leaf.left != null )
                    {
                        leaf = leaf.left;
                    }
                    else
                    {
                        // the rotation should ensure (leaf.left != null) on the next iteration
                        leaf = smallLeftRotation( leaf );
                    }
                }
            }
            else
            {
                assert false : "should never happen";
            }

            assert leaf != null : "replacement leaf should always exist at this point";

            // detach leaf from its parent
            if ( leaf.parent.left == leaf )
            {
                leaf.parent.left = null;
            }
            else if ( leaf.parent.right == leaf )
            {
                leaf.parent.right = null;
            }
            else
            {
                assert false : "broken parent/child reference in the tree";
            }

            node.value = leaf.value; // replace node value with leaf's value
            node = leaf.parent; // change recursion point down so that below down-up update picks up
            // everything from leaf's parent up

            recycleNode( leaf );
        }

        rebalanceUp( node );

        --size;

        return true;
    }


    public final boolean contains( T value )
    {
        AvlNode<T> node = tree;

        while ( node != null )
        {
            int cmp = value.compareTo( node.value );

            if ( cmp < 0 )
            {
                node = node.left;
            }
            else if ( cmp > 0 )
            {
                node = node.right;
            }
            else
            {
                return true;
            }
        }

        return false;

    }


    private static <T extends Comparable<T>> void updateHeight( AvlNode<T> node )
    {
        int leftHeight = ( node.left == null ) ? -1 : node.left.height;
        int rightHeight = ( node.right == null ) ? -1 : node.right.height;
        node.height = 1 + ( rightHeight > leftHeight ? rightHeight : leftHeight );
        node.balance = rightHeight - leftHeight;
    }


    private static <T extends Comparable<T>> AvlNode<T> smallLeftRotation( AvlNode<T> node )
    {
        assert node.balance > 0 : "null right child in smallLeft";

        // update child references
        AvlNode<T> right = node.right;
        node.right = right.left;
        right.left = node;

        // update parent references
        if ( node.right != null )
        {
            node.right.parent = node;
        }

        right.parent = node.parent;

        if ( right.parent != null )
        {
            if ( right.parent.left == node )
            {
                node.parent.left = right;
            }
            else
            {
                node.parent.right = right;
            }
        }

        node.parent = right;

        updateHeight( node );
        updateHeight( right );

        return right;
    }


    private static <T extends Comparable<T>> AvlNode<T> smallRightRotation( AvlNode<T> node )
    {
        assert node.balance < 0 : "null left child in smallRight";

        // update child references
        AvlNode<T> left = node.left;
        node.left = left.right;
        left.right = node;

        // update parent references
        if ( node.left != null )
        {
            node.left.parent = node;
        }

        left.parent = node.parent;

        if ( left.parent != null )
        {
            if ( left.parent.left == node )
            {
                node.parent.left = left;
            }
            else
            {
                node.parent.right = left;
            }
        }

        node.parent = left;

        updateHeight( node );
        updateHeight( left );

        return left;
    }


    private static <T extends Comparable<T>> AvlNode<T> bigLeftRotation( AvlNode<T> node )
    {
        assert node.right != null : "null right child in bigLeft";

        if ( node.right.balance < 0 )
        {
            node.right = smallRightRotation( node.right );
        }

        updateHeight( node );

        return smallLeftRotation( node );
    }


    private static <T extends Comparable<T>> AvlNode<T> bigRightRotation( AvlNode<T> node )
    {
        assert node.left != null : "null right child in bigRight";

        if ( node.left.balance > 0 )
        {
            node.left = smallLeftRotation( node.left );
        }

        updateHeight( node );

        return smallRightRotation( node );
    }
}
