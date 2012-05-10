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

    final boolean useFreeList;


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
            tree = new_node( null, value );
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
                    node.left = new_node( node, value );
                    break;
                }
                else
                    node = node.left;
            }
            else if ( cmp > 0 )
            {
                if ( node.right == null )
                {
                    node.right = new_node( node, value );
                    break;
                }
                else
                    node = node.right;
            }
            else
                assert false : "should never happen";
        }

        // node with _value_ already exists
        if ( cmp == 0 )
            return false;
        rebalance_up( node );
        ++size;

        return true;
    }

    Stack<AvlNode<T>> free_list = new Stack<AvlNode<T>>();


    private final AvlNode<T> new_node( AvlNode<T> parent, T value )
    {
        if ( !useFreeList || free_list.isEmpty() )
            return new AvlNode<T>( parent, value );
        else
        {
            AvlNode<T> node = free_list.pop();
            return node.reset( parent, value );
        }
    }


    private final void recycle_node( AvlNode<T> node )
    {
        if ( !useFreeList )
            return;

        // keep free list size not bigger than tree size
        while ( free_list.size() > size )
            free_list.pop();
        if ( free_list.size() == size )
            return;

        free_list.push( node );
    }


    private void rebalance_up( AvlNode<T> node )
    {
        while ( node != null )
        {
            int height_before = node.height;
            update_height( node );

            // rebalance
            if ( node.balance == -2 )
                node = big_right_rotation( node );
            else if ( node.balance == 2 )
                node = big_left_rotation( node );

            if ( node.parent == null )
                tree = node;

            // if parent node is not affected
            if ( height_before == node.height )
                break;

            node = node.parent;
        }
    }


    public final boolean remove( T value )
    {
        AvlNode<T> node = tree;
        if ( node == null )
            return false;

        // find the node to be removed
        for ( int cmp = value.compareTo( node.value ); cmp != 0; cmp = value.compareTo( node.value ) )
        {
            node = ( cmp < 0 ) ? node.left : node.right;
            if ( node == null )
                return false;
        }

        // find a replacement node (if needed)
        final int LEFT = -1;
        final int RIGHT = 1;
        final int NONE = 0;
        int replaceFrom = NONE;
        if ( node.left != null && node.right == null )
        {
            replaceFrom = LEFT;
        }
        else if ( node.right != null && node.left == null )
        {
            replaceFrom = RIGHT;
        }
        else if ( node.right != null && node.left != null )
        {
            if ( node.balance < 0 )
            {
                replaceFrom = LEFT;
            }
            else if ( node.balance > 0 )
            {
                replaceFrom = RIGHT;
            }
            else
            {
                replaceFrom = LEFT; // TODO: asymmetry
            }
        }
        else
        { // node is itself a leaf, replacement is not needed
            if ( node.parent == null )
            { // the tree root, single node in the tree
                tree = null;
                --size;
                recycle_node( node );
                return true;
            }
            else
            { // non-root leaf node
                // detach from parent
                if ( node.parent.left == node )
                    node.parent.left = null;
                else
                    node.parent.right = null;

                AvlNode<T> dead = node;
                node = node.parent; // update heights/rebalance from node's parents up (the bottom of this method)
                recycle_node( dead );
                replaceFrom = NONE;
            }
        }

        if ( replaceFrom != NONE )
        {
            AvlNode<T> leaf = null;
            if ( replaceFrom == LEFT )
            {
                leaf = node.left;
                while ( leaf.left != null || leaf.right != null )
                {
                    if ( leaf.right != null )
                        leaf = leaf.right;
                    else
                        leaf = small_right_rotation( leaf ); // the rotation should ensure (leaf.right != null) on the next iteration
                }
            }
            else if ( replaceFrom == RIGHT )
            {
                leaf = node.right;
                while ( leaf.right != null || leaf.left != null )
                {
                    if ( leaf.left != null )
                        leaf = leaf.left;
                    else
                        leaf = small_left_rotation( leaf ); // the rotation should ensure (leaf.left != null) on the next iteration
                }
            }
            else
                assert false : "should never happen";

            assert leaf != null : "replacement leaf should always exist at this point";

            // detach leaf from its parent
            if ( leaf.parent.left == leaf )
                leaf.parent.left = null;
            else if ( leaf.parent.right == leaf )
                leaf.parent.right = null;
            else
                assert false : "broken parent/child reference in the tree";

            node.value = leaf.value; // replace node value with leaf's value
            node = leaf.parent; // change recursion point down so that below down-up update picks up
            // everything from leaf's parent up

            recycle_node( leaf );
        }

        rebalance_up( node );

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
                node = node.left;
            else if ( cmp > 0 )
                node = node.right;
            else
                return true;
        }
        return false;

    }


    private static final <T extends Comparable<T>> void update_height( AvlNode<T> node )
    {
        int left_height = ( node.left == null ) ? -1 : node.left.height;
        int right_height = ( node.right == null ) ? -1 : node.right.height;
        node.height = 1 + ( right_height > left_height ? right_height : left_height );
        node.balance = right_height - left_height;
    }


    private static final <T extends Comparable<T>> AvlNode<T> small_left_rotation( AvlNode<T> node )
    {
        assert node.balance > 0 : "null right child in small_left";

        // update child references
        AvlNode<T> right = node.right;
        node.right = right.left;
        right.left = node;

        // update parent references
        if ( node.right != null )
            node.right.parent = node;
        right.parent = node.parent;

        if ( right.parent != null )
        {
            if ( right.parent.left == node )
                node.parent.left = right;
            else
                node.parent.right = right;
        }

        node.parent = right;

        update_height( node );
        update_height( right );

        return right;
    }


    private static final <T extends Comparable<T>> AvlNode<T> small_right_rotation( AvlNode<T> node )
    {
        assert node.balance < 0 : "null left child in small_right";

        // update child references
        AvlNode<T> left = node.left;
        node.left = left.right;
        left.right = node;

        // update parent references
        if ( node.left != null )
            node.left.parent = node;
        left.parent = node.parent;

        if ( left.parent != null )
        {
            if ( left.parent.left == node )
                node.parent.left = left;
            else
                node.parent.right = left;
        }

        node.parent = left;

        update_height( node );
        update_height( left );

        return left;
    }


    private static final <T extends Comparable<T>> AvlNode<T> big_left_rotation( AvlNode<T> node )
    {
        assert node.right != null : "null right child in big_left";

        if ( node.right.balance < 0 )
            node.right = small_right_rotation( node.right );

        update_height( node );

        return small_left_rotation( node );
    }


    private static final <T extends Comparable<T>> AvlNode<T> big_right_rotation( AvlNode<T> node )
    {
        assert node.left != null : "null right child in big_right";

        if ( node.left.balance > 0 )
            node.left = small_left_rotation( node.left );

        update_height( node );

        return small_right_rotation( node );
    }
}
