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


import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;


/**
 * An AvlTreeMap implementation with support to store both key and value.
 * This implementation also supports duplicate keys. The values of a same key
 * will be stored in a AvlTree.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AvlTreeMapImpl<K, V> implements AvlTreeMap<K, V>
{
    /** the root of the tree */
    private LinkedAvlMapNode<K, V> root;

    /** The Comparator used for comparing the keys */
    private Comparator<K> keyComparator;

    /** The Comparator used for comparing the values */
    private Comparator<V> valueComparator;

    /** node representing the start of the doubly linked list formed with the tree nodes */
    private LinkedAvlMapNode<K, V> first;

    /** node representing the end of the doubly linked list formed with the tree nodes */
    private LinkedAvlMapNode<K, V> last;

    /** flag to allow storing duplicate keys */
    private boolean allowDuplicates;

    /** size of the map */
    private int size;

    /**
     * Creates a new instance of AVLTreeMap without support for duplicate keys.
     *
     * @param keyComparator the comparator to be used for comparing keys
     * @param valueComparator the comparator to be used for comparing values
     */
    public AvlTreeMapImpl( Comparator<K> keyComparator, Comparator<V> valueComparator )
    {
        this( keyComparator, valueComparator, false );
    }


    /**
     * Creates a new instance of AVLTreeMap without support for duplicate keys.
     *
     * @param keyComparator the comparator to be used for comparing keys
     * @param valueComparator the comparator to be used for comparing values
     * @param allowDuplicates are duplicates keyComparators allowed?
     */
    public AvlTreeMapImpl( Comparator<K> keyComparator, Comparator<V> valueComparator, boolean allowDuplicates )
    {
        this.keyComparator = keyComparator;
        this.valueComparator = valueComparator;
        this.allowDuplicates = allowDuplicates;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Comparator<K> getKeyComparator()
    {
        return keyComparator;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Comparator<V> getValueComparator()
    {
        return valueComparator;
    }


    public void dump()
    {
        dump( root, "" );
    }


    private void dump( LinkedAvlMapNode<K, V> node, String indention )
    {
        if ( node.right != null )
        {
            dump( node.right, indention + "  " );
        }

        if ( node.value.isSingleton() )
        {
            System.out.println( indention + "<" + node.key + "," + node.value.getSingleton() + ">" );
        }
        else
        {
            String values = node.value.getOrderedSet().getKeys().stream().map( Objects::toString ).collect( joining() );
            System.out.println( indention + "<" + node.key + "," + values + ">" );
        }

        if ( node.left != null )
        {
            dump( node.left, indention + "  " );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public V insert( K key, V value )
    {
        if ( key == null || value == null )
        {
            throw new IllegalArgumentException( "key or value cannot be null" );
        }

        if ( root == null )
        {
            root = new LinkedAvlMapNode<>( key, value );
            first = root;
            last = root;
            size++;
            return null;
        }

        ValueHolder<V> holder = new ValueHolder<>();
        root = insert( root, key, value, holder );
        return holder.getSingleton();
    }


    private LinkedAvlMapNode<K, V> insert( LinkedAvlMapNode<K, V> node, K key, V value,
        ValueHolder<V> holder )
    {
        int cmp = keyComparator.compare( key, node.key );
        if ( cmp < 0 )
        {
            if ( node.left == null )
            {
                LinkedAvlMapNode<K, V> left = new LinkedAvlMapNode<>( key, value );
                node.left = left;
                insertInList( left, node, cmp );
                size++;
            }
            else
            {
                node.left = insert( node.left, key, value, holder );
            }
        }
        else if ( cmp > 0 )
        {
            if ( node.right == null )
            {
                LinkedAvlMapNode<K, V> right = new LinkedAvlMapNode<>( key, value );
                node.right = right;
                size++;
                insertInList( right, node, cmp );
            }
            else
            {
                node.right = insert( node.right, key, value, holder );
            }
        }
        else
        {
            V returnValue;

            if ( allowDuplicates )
            {
                returnValue = insertDupKey( value, node ); // key already exists add another value
            }
            else
            {
                // replace the existing value with the new value
                returnValue = node.value.setSingleton( value );
            }

            if ( returnValue != null )
            {
                holder.value = new SingletonOrOrderedSet<>( returnValue );
            }
            return node;
        }

        node.height = 1 + Math.max( height( node.left ), height( node.right ) );
        return balance( node );
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
    private LinkedAvlMapNode<K, V> balance( LinkedAvlMapNode<K, V> node )
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


    private V insertDupKey( V value, LinkedAvlMapNode<K, V> existingNode )
    {
        AvlTree<V> dupsTree = null;

        if ( existingNode.value.isOrderedSet() )
        {
            dupsTree = existingNode.value.getOrderedSet();
        }
        else
        {
            // create avlTree, insert singleton into it, then switch modes 
            dupsTree = new AvlTreeImpl<>( valueComparator );
            dupsTree.insert( existingNode.value.getSingleton() );
            existingNode.value.switchToOrderedSet( dupsTree );
        }

        // check if value already exists
        if ( dupsTree.find( value ) != null )
        {
            return value;
        }

        // insert value into duplicate key holder
        dupsTree.insert( value );

        return null;
    }


    private void removeFromList( LinkedAvlMapNode<K, V> node )
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


    private void insertInList( LinkedAvlMapNode<K, V> node, LinkedAvlMapNode<K, V> parentNode, int pos )
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


    /**
     * {@inheritDoc}
     */
    @Override
    public SingletonOrOrderedSet<V> remove( K key )
    {
        if ( key == null )
        {
            throw new IllegalArgumentException( "key cannot be null" );
        }

        if ( root == null )
        {
            return null;
        }

        ValueHolder<V> holder = new ValueHolder<>();
        root = remove( root, key, null, holder );
        return holder.value;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public V remove( K key, V value )
    {
        if ( key == null || value == null )
        {
            throw new IllegalArgumentException( "key or value cannot be null" );
        }

        if ( root == null )
        {
            return null;
        }

        ValueHolder<V> holder = new ValueHolder<>();
        root = remove( root, key, value, holder );
        return holder.getSingleton();
    }


    /**
     * Removes the specified key and its associated value from the given subtree.
     * 
     * @param node the subtree
     * @param key the key
     * @return the updated subtree
     */
    private LinkedAvlMapNode<K, V> remove( LinkedAvlMapNode<K, V> node, K key, V value,
        ValueHolder<V> holder )
    {
        if ( node == null )
        {
            return null;
        }

        int cmp = keyComparator.compare( key, node.key );
        if ( cmp < 0 )
        {
            node.left = remove( node.left, key, value, holder );
        }
        else if ( cmp > 0 )
        {
            node.right = remove( node.right, key, value, holder );
        }
        else
        {
            if ( value == null )
            {
                holder.value = node.value;
            }
            else
            {
                if ( allowDuplicates )
                {
                    if ( node.value.isOrderedSet() )
                    {
                        AvlTree<V> dupsTree = node.value.getOrderedSet();
                        V removedVal = dupsTree.remove( value );

                        if ( removedVal == null )
                        {
                            // value was not present
                            return node;
                        }

                        holder.value = new SingletonOrOrderedSet<>( removedVal );

                        if ( !dupsTree.isEmpty() )
                        {
                            return node;
                        }

                        // the node holds no values anymore and will be deleted
                    }
                    else
                    {
                        if ( valueComparator.compare( node.value.getSingleton(), value ) != 0 )
                        {
                            return node; // node was not removed, no need to balance
                        }

                        holder.value = node.value;
                    }
                }
                else
                {
                    if ( valueComparator.compare( node.value.getSingleton(), value ) != 0 )
                    {
                        return node; // node was not removed, no need to balance
                    }

                    holder.value = node.value;
                }
            }

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
                LinkedAvlMapNode<K, V> y = node;
                node = mostLeftChild( y.right );
                node.right = deleteMin( y.right );
                node.left = y.left;
            }
        }

        node.height = 1 + Math.max( height( node.left ), height( node.right ) );
        return balance( node );
    }


    private LinkedAvlMapNode<K, V> mostLeftChild( LinkedAvlMapNode<K, V> node )
    {
        LinkedAvlMapNode<K, V> current = node;
        while ( current.left != null )
        {
            current = current.left;
        }
        return current;
    }


    private LinkedAvlMapNode<K, V> deleteMin( LinkedAvlMapNode<K, V> node )
    {
        if ( node.left == null )
        {
            return node.right;
        }

        node.left = deleteMin( node.left );
        node.height = 1 + Math.max( height( node.left ), height( node.right ) );
        return balance( node );
    }


    private LinkedAvlMapNode<K, V> rotateRight( LinkedAvlMapNode<K, V> x )
    {
        LinkedAvlMapNode<K, V> y = x.left;
        x.left = y.right;
        y.right = x;
        x.height = 1 + Math.max( height( x.left ), height( x.right ) );
        y.height = 1 + Math.max( height( y.left ), height( y.right ) );
        return y;
    }


    private LinkedAvlMapNode<K, V> rotateLeft( LinkedAvlMapNode<K, V> x )
    {
        LinkedAvlMapNode<K, V> y = x.right;
        x.right = y.left;
        y.left = x;
        x.height = 1 + Math.max( height( x.left ), height( x.right ) );
        y.height = 1 + Math.max( height( y.left ), height( y.right ) );
        return y;
    }


    private int height( LinkedAvlMapNode<K, V> n )
    {
        return n == null ? -1 : n.height;
    }


    public int getBalance( LinkedAvlMapNode<K, V> n )
    {
        return height( n.left ) - height( n.right );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty()
    {
        return root == null;
    }


    /**
     * {@inheritDoc}
     */
    //NOTE: This method is internally used by AVLTreeMarshaller
    @Override
    public int getSize()
    {
        return size;
    }


    /**
     * Set the root of the tree.
     * 
     * Note : this method is used by the deserialization method
     *
     * @param root the root of the tree
     */
    /* no protection */void setRoot( LinkedAvlMapNode<K, V> root )
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
    /* no protection */void setFirst( LinkedAvlMapNode<K, V> first )
    {
        this.first = first;
    }


    /**
     * Set the last element of the tree
     * 
     * Note : this method is used by the deserialization method
     *
     * @param last the last element to be added
     */
    /* no protection */void setLast( LinkedAvlMapNode<K, V> last )
    {
        this.last = last;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public LinkedAvlMapNode<K, V> getRoot()
    {
        return root;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public List<K> getKeys()
    {
        List<K> keys = new ArrayList<>();
        LinkedAvlMapNode<K, V> node = first;

        while ( node != null )
        {
            keys.add( node.key );
            node = node.next;
        }

        return keys;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void printTree()
    {
        if ( isEmpty() )
        {
            System.out.println( "Tree is empty" );
            return;
        }

        System.out.println( root );

        visit( root.right, root, 0 );

        visit( root.left, root, 0 );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public LinkedAvlMapNode<K, V> getFirst()
    {
        return first;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public LinkedAvlMapNode<K, V> getLast()
    {
        return last;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public LinkedAvlMapNode<K, V> findGreater( K key )
    {
        LinkedAvlMapNode<K, V> result = fetchNonNullNode( key, root, root );

        if ( result == null )
        {
            return null;
        }
        else if ( keyComparator.compare( key, result.key ) < 0 )
        {
            return result;
        }

        return result.next;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public LinkedAvlMapNode<K, V> findGreaterOrEqual( K key )
    {
        LinkedAvlMapNode<K, V> result = fetchNonNullNode( key, root, root );

        if ( result == null )
        {
            return null;
        }
        else if ( keyComparator.compare( key, result.key ) <= 0 )
        {
            return result;
        }

        return result.next;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public LinkedAvlMapNode<K, V> findLess( K key )
    {
        LinkedAvlMapNode<K, V> result = fetchNonNullNode( key, root, root );

        if ( result == null )
        {
            return null;
        }
        else if ( keyComparator.compare( key, result.key ) > 0 )
        {
            return result;
        }

        return result.previous;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public LinkedAvlMapNode<K, V> findLessOrEqual( K key )
    {
        LinkedAvlMapNode<K, V> result = fetchNonNullNode( key, root, root );

        if ( result == null )
        {
            return null;
        }
        else if ( keyComparator.compare( key, result.key ) >= 0 )
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
    private LinkedAvlMapNode<K, V> fetchNonNullNode( K key, LinkedAvlMapNode<K, V> startNode,
        LinkedAvlMapNode<K, V> parent )
    {

        if ( startNode == null )
        {
            return parent;
        }

        int c = keyComparator.compare( key, startNode.key );

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


    /**
     * {@inheritDoc}
     */
    @Override
    public LinkedAvlMapNode<K, V> find( K key )
    {
        return find( key, root );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public LinkedAvlMapNode<K, V> find( K key, V value )
    {
        if ( key == null || value == null )
        {
            return null;
        }

        LinkedAvlMapNode<K, V> node = find( key, root );

        if ( node == null )
        {
            return null;
        }

        if ( node.value.isOrderedSet() )
        {
            AvlTree<V> dupsTree = node.value.getOrderedSet();

            if ( dupsTree.find( value ) == null )
            {
                return null;
            }
        }
        else
        {
            if ( valueComparator.compare( node.value.getSingleton(), value ) != 0 )
            {
                return null;
            }
        }

        return node;
    }


    private LinkedAvlMapNode<K, V> find( K key, LinkedAvlMapNode<K, V> startNode )
    {
        int c;

        if ( startNode == null )
        {
            return null;
        }

        c = keyComparator.compare( key, startNode.key );

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


    private void visit( LinkedAvlMapNode<K, V> node, LinkedAvlMapNode<K, V> parentNode, int depth )
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


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDupsAllowed()
    {
        return allowDuplicates;
    }


    /**
     * removes all the nodes from the tree
     */
    public void removeAll()
    {
        LinkedAvlMapNode<K, V> tmp;

        while ( first != null )
        {
            tmp = first;
            first = tmp.next;
            tmp = null;
        }

        last = null;
        root = null;
        size = 0;
    }

    private static final class ValueHolder<T>
    {

        private SingletonOrOrderedSet<T> value;

        T getSingleton()
        {
            if ( value != null )
            {
                return value.getSingleton();
            }
            return null;
        }
    }
}
