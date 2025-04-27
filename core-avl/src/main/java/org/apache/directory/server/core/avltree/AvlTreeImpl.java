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
package org.apache.directory.server.core.avltree;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


/**
 * An AVL tree implementation
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AvlTreeImpl<K> implements AvlTree<K>
{
    /** the root of the tree */
    private LinkedAvlNode<K> root;

    /** The Comparator used for comparing the keys */
    private Comparator<K> comparator;

    /** node representing the start of the doubly linked list formed with the tree nodes */
    private LinkedAvlNode<K> first;

    /** node representing the end of the doubly linked list formed with the tree nodes */
    private LinkedAvlNode<K> last;

    /** size of the tree */
    private int size;

    /**
     * Creates a new instance of AVLTree.
     *
     * @param comparator the comparator to be used for comparing keys
     */
    public AvlTreeImpl( Comparator<K> comparator )
    {
        this.comparator = comparator;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.avltree.AvlTree#getComparator()
     */
    @Override
    public Comparator<K> getComparator()
    {
        return comparator;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.avltree.AvlTree#insert(K)
     */
    @Override
    public K insert( K key )
    {

        if ( root == null )
        {
            root = new LinkedAvlNode<>( key );
            first = root;
            last = root;
            size++;
            return null;
        }

        KeyHolder<K> holder = new KeyHolder<>();
        root = insert( root, key, holder );
        return holder.key;
    }


    private LinkedAvlNode<K> insert( LinkedAvlNode<K> node, K key, KeyHolder<K> holder )
    {
        int cmp = comparator.compare( key, node.key );
        if ( cmp < 0 )
        {
            if ( node.left == null )
            {
                LinkedAvlNode<K> left = new LinkedAvlNode<>( key );
                node.left = left;
                insertInList( left, node, cmp );
                size++;
            }
            else
            {
                node.left = insert( node.left, key, holder );
            }
        }
        else if ( cmp > 0 )
        {
            if ( node.right == null )
            {
                LinkedAvlNode<K> right = new LinkedAvlNode<>( key );
                node.right = right;
                size++;
                insertInList( right, node, cmp );
            }
            else
            {
                node.right = insert( node.right, key, holder );
            }
        }
        else
        {
            holder.key = node.key;
            return node;
        }

        node.height = 1 + Math.max( height( node.left ), height( node.right ) );
        return balance( node );
    }


    private void removeFromList( LinkedAvlNode<K> node )
    {
        if ( node.next == null && node.previous == null ) // should happen in case of tree having single node
        {
            first = null;
            last = null;
        }
        else if ( node.next == null ) // last node
        {
            node.previous.next = null;
            last = node.previous;
        }
        else if ( node.previous == null ) // first node
        {
            node.next.previous = null;
            first = node.next;
        }
        else
        // somewhere in middle
        {
            node.previous.next = node.next;
            node.next.previous = node.previous;
        }

    }


    private void insertInList( LinkedAvlNode<K> node, LinkedAvlNode<K> parentNode, int pos )
    {

        if ( pos < 0 )
        {
            if ( last == null )
            {
                last = parentNode;
            }

            if ( parentNode.previous == null )
            {
                first = node;
            }
            else
            {
                parentNode.previous.next = node;
                node.previous = parentNode.previous;
            }

            node.next = parentNode;
            parentNode.previous = node;
        }
        else if ( pos > 0 )
        {
            if ( parentNode.next == null )
            {
                last = node;
            }
            else
            {
                parentNode.next.previous = node;
                node.next = parentNode.next;
            }
            node.previous = parentNode;
            parentNode.next = node;
        }

    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.avltree.AvlTree#remove(K)
     */
    @Override
    public K remove( K key )
    {

        if ( root == null )
        {
            return null;
        }

        KeyHolder<K> holder = new KeyHolder<>();
        root = remove( root, key, holder );
        return holder.key;
    }


    /**
     * Removes the specified key and its associated value from the given subtree.
     * 
     * @param node the subtree
     * @param key the key
     * @return the updated subtree
     */
    private LinkedAvlNode<K> remove( LinkedAvlNode<K> node, K key, KeyHolder<K> holder )
    {
        if ( node == null )
        {
            return null;
        }

        int cmp = comparator.compare( key, node.key );
        if ( cmp < 0 )
        {
            node.left = remove( node.left, key, holder );
        }
        else if ( cmp > 0 )
        {
            node.right = remove( node.right, key, holder );
        }
        else
        {
            holder.key = node.key;

            removeFromList( node );
            size--;

            if ( node.left == null )
            {
                return node.right;
            }
            else if ( node.right == null )
            {
                return node.left;
            }
            else
            {
                LinkedAvlNode<K> y = node;
                node = mostLeftChild( y.right );
                node.right = deleteMin( y.right );
                node.left = y.left;
            }
        }

        node.height = 1 + Math.max( height( node.left ), height( node.right ) );
        return balance( node );
    }


    private LinkedAvlNode<K> mostLeftChild( LinkedAvlNode<K> node )
    {
        LinkedAvlNode<K> current = node;
        while ( current.left != null )
        {
            current = current.left;
        }
        return current;
    }


    private LinkedAvlNode<K> deleteMin( LinkedAvlNode<K> node )
    {
        if ( node.left == null )
        {
            return node.right;
        }

        node.left = deleteMin( node.left );
        node.height = 1 + Math.max( height( node.left ), height( node.right ) );
        return balance( node );
    }


    private synchronized LinkedAvlNode<K> rotateRight( LinkedAvlNode<K> x )
    {
        LinkedAvlNode<K> y = x.left;
        x.left = y.right;
        y.right = x;
        x.height = 1 + Math.max( height( x.left ), height( x.right ) );
        y.height = 1 + Math.max( height( y.left ), height( y.right ) );
        return y;
    }


    private synchronized LinkedAvlNode<K> rotateLeft( LinkedAvlNode<K> x )
    {
        LinkedAvlNode<K> y = x.right;
        x.right = y.left;
        y.left = x;
        x.height = 1 + Math.max( height( x.left ), height( x.right ) );
        y.height = 1 + Math.max( height( y.left ), height( y.right ) );
        return y;
    }


    private int height( LinkedAvlNode<K> n )
    {
        return n == null ? -1 : n.height;
    }


    public int getBalance( LinkedAvlNode<K> n )
    {
        return height( n.left ) - height( n.right );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.avltree.AvlTree#isEmpty()
     */
    @Override
    public boolean isEmpty()
    {
        return root == null;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.avltree.AvlTree#getSize()
     */
    //NOTE: This method is internally used by AVLTreeMarshaller
    @Override
    public int getSize()
    {
        return size;
    }


    /**
     * Set the size of the tree.
     * 
     * Note : this method is used by the deserialization method
     *
     * @param size the size of the tree
     */
    /* no protection */void setSize( int size )
    {
        this.size = size;
    }


    /**
     * Set the root of the tree.
     * 
     * Note : this method is used by the deserialization method
     *
     * @param root the root of the tree
     */
    /* no protection */void setRoot( LinkedAvlNode<K> root )
    {
        this.root = root;
    }


    /**
     * Set the first element of the tree
     * 
     * Note : this method is used by the deserialization method
     *
     * @param first the first element to be added
     */
    /* no protection */void setFirst( LinkedAvlNode<K> first )
    {
        this.first = first;
        size++;
    }


    /**
     * Set the last element of the tree
     * 
     * Note : this method is used by the deserialization method
     *
     * @param last the last element to be added
     */
    /* no protection */void setLast( LinkedAvlNode<K> last )
    {
        this.last = last;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.avltree.AvlTree#getRoot()
     */
    @Override
    public LinkedAvlNode<K> getRoot()
    {
        return root;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.avltree.AvlTree#getKeys()
     */
    @Override
    public List<K> getKeys()
    {
        List<K> keys = new ArrayList<>();
        LinkedAvlNode<K> node = first;

        while ( node != null )
        {
            keys.add( node.key );
            node = node.next;
        }

        return keys;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.avltree.AvlTree#printTree()
     */
    @Override
    public void printTree()
    {
        if ( isEmpty() )
        {
            System.out.println( "Tree is empty" );
            return;
        }

        System.out.println( getRoot() );

        visit( root.right, getRoot(), 0 );

        visit( root.left, getRoot(), 0 );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.avltree.AvlTree#getFirst()
     */
    @Override
    public LinkedAvlNode<K> getFirst()
    {
        return first;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.avltree.AvlTree#getLast()
     */
    @Override
    public LinkedAvlNode<K> getLast()
    {
        return last;
    }


    /**
     * Balances the tree by visiting the nodes present in the List of nodes present in the
     * treePath parameter.<br><br>
     *
     * This really does the balancing if the height of the tree is greater than 2 and the<br> 
     * balance factor is greater than +1 or less than -1.<br><br>
     * For an excellent info please read the 
     * <a href="http://en.wikipedia.org/wiki/Avl_tree">Wikipedia article on AVL tree</a>.
     */
    private LinkedAvlNode<K> balance( LinkedAvlNode<K> node )
    {
        if ( getBalance( node ) < -1 )
        {
            if ( getBalance( node.right ) > 0 )
            {
                node.right = rotateRight( node.right );
            }
            node = rotateLeft( node );
        }
        else if ( getBalance( node ) > 1 )
        {
            if ( getBalance( node.left ) < 0 )
            {
                node.left = rotateLeft( node.left );
            }
            node = rotateRight( node );
        }
        return node;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.avltree.AvlTree#findGreater(K)
     */
    @Override
    public LinkedAvlNode<K> findGreater( K key )
    {
        LinkedAvlNode<K> result = fetchNonNullNode( key, root, root );

        if ( result == null )
        {
            return null;
        }
        else if ( comparator.compare( key, result.key ) < 0 )
        {
            return result;
        }

        return result.next;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.avltree.AvlTree#findGreaterOrEqual(K)
     */
    @Override
    public LinkedAvlNode<K> findGreaterOrEqual( K key )
    {
        LinkedAvlNode<K> result = fetchNonNullNode( key, root, root );

        if ( result == null )
        {
            return null;
        }
        else if ( comparator.compare( key, result.key ) <= 0 )
        {
            return result;
        }

        return result.next;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.avltree.AvlTree#findLess(K)
     */
    @Override
    public LinkedAvlNode<K> findLess( K key )
    {
        LinkedAvlNode<K> result = fetchNonNullNode( key, root, root );

        if ( result == null )
        {
            return null;
        }
        else if ( comparator.compare( key, result.key ) > 0 )
        {
            return result;
        }

        return result.previous;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.avltree.AvlTree#findLessOrEqual(K)
     */
    @Override
    public LinkedAvlNode<K> findLessOrEqual( K key )
    {
        LinkedAvlNode<K> result = fetchNonNullNode( key, root, root );

        if ( result == null )
        {
            return null;
        }
        else if ( comparator.compare( key, result.key ) >= 0 )
        {
            return result;
        }

        return result.previous;
    }


    /*
     * This method returns the last visited non-null node in case if the node with the given key
     * is not present. This method should not be used as general purpose lookup method.
     * This is written to assist the findGreater, findLess methods. 
     */
    private LinkedAvlNode<K> fetchNonNullNode( K key, LinkedAvlNode<K> startNode, LinkedAvlNode<K> parent )
    {

        if ( startNode == null )
        {
            return parent;
        }

        int c = comparator.compare( key, startNode.key );

        parent = startNode;

        if ( c > 0 )
        {
            return fetchNonNullNode( key, startNode.right, parent );
        }
        else if ( c < 0 )
        {
            return fetchNonNullNode( key, startNode.left, parent );
        }

        return startNode;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.avltree.AvlTree#find(K)
     */
    @Override
    public LinkedAvlNode<K> find( K key )
    {
        return find( key, root );
    }


    private LinkedAvlNode<K> find( K key, LinkedAvlNode<K> startNode )
    {
        int c;

        if ( startNode == null )
        {
            return null;
        }

        c = comparator.compare( key, startNode.key );

        if ( c > 0 )
        {
            return find( key, startNode.right );
        }
        else if ( c < 0 )
        {
            return find( key, startNode.left );
        }

        return startNode;
    }


    private void visit( LinkedAvlNode<K> node, LinkedAvlNode<K> parentNode, int depth )
    {
        if ( node == null )
        {
            return;
        }

        for ( int i = 0; i < depth; i++ )
        {
            System.out.print( "|  " );
        }

        String type = "";
        if ( node == parentNode.left )
        {
            type = "L";
        }
        else if ( node == parentNode.right )
        {
            type = "R";
        }

        System.out.println( "|--" + node + type );

        if ( node.getRight() != null )
        {
            visit( node.getRight(), node, depth + 1 );
        }

        if ( node.getLeft() != null )
        {
            visit( node.getLeft(), node, depth + 1 );
        }
    }

    private static final class KeyHolder<T>
    {

        private T key;

    }
}
