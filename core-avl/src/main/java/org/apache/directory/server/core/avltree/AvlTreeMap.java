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
 * An AVL tree implementation with support to store both key and value.
 * This implementation also supports duplicate keys. The values of a same key
 * will be stored in a AvlTree.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AvlTreeMap<K,V>
{
    /** the root of the tree */
    private LinkedAvlMapNode<K,V> root;

    /** The Comparator used for comparing the keys */
    private Comparator<K> keyComparator;

    /** The Comparator used for comparing the values */
    private Comparator<V> valueComparator;
    
    /** node representing the start of the doubly linked list formed with the tree nodes */
    private LinkedAvlMapNode<K,V> first;
    
    /** node representing the end of the doubly linked list formed with the tree nodes */
    private LinkedAvlMapNode<K,V> last;
    
    /** flag to allow storing duplicate keys */
    private boolean allowDuplicates;


    /**
     * Creates a new instance of AVLTreeMap without support for duplicate keys.
     *
     * @param comparator1 the comparator to be used for comparing keys
     */
    public AvlTreeMap( Comparator<K> keyComparator, Comparator<V> valueComparator )
    {
        this( keyComparator, valueComparator, false );
    }
    

    public AvlTreeMap( Comparator<K> keyComparator, Comparator<V> valueComparator, boolean allowDuplicates )
    {
        this.keyComparator = keyComparator;
        this.valueComparator = valueComparator;
        this.allowDuplicates = allowDuplicates;
    }

    
    /**
     * @return the key comparator associated with this tree 
     */
    public Comparator<K> getKeyComparator()
    {
        return keyComparator;
    }
    

    /**
     * @return the value comparator associated with this tree 
     */
    public Comparator<V> getValueComparator()
    {
        return valueComparator;
    }
    
    
    /**
     * Inserts a LinkedAvlMapNode with the given key and value.
     *
     * @param key the item to be inserted
     * @return the replaced key if it already exists
     * TODO - should we not return the value if the key already existed?
     * Note: Replaces a nodes value if duplicate keys are not allowed.
     */
    public K insert( K key, V value )
    {
        LinkedAvlMapNode<K,V> node, temp;
        LinkedAvlMapNode<K,V> parent = null;
        int c;
        
        if ( root == null )
        {
          root = new LinkedAvlMapNode<K,V>( key, value );
          first = root;
          last = root;
          return null;
        }
        
        node = new LinkedAvlMapNode<K,V>( key, value );
        
        temp = root;
        
        List<LinkedAvlMapNode<K,V>> treePath = new ArrayList<LinkedAvlMapNode<K,V>>();

        while( temp != null )
        {
            treePath.add(0, temp ); // last node first, for the sake of balance factor computation
            parent = temp;
            
            c = keyComparator.compare( key, temp.getKey() );
            
            if( c == 0 )
            {
                if( allowDuplicates )
                {
                    return insertDupKey( key, value, temp ); // key already exists add another value
                }
                else
                {
                    // replcae the existing value with the new value
                    temp.value = value;
                    return key;
                }
            }
            
            if( c < 0 )
            {
                temp.isLeft = true;
                temp = temp.getLeft();
            }
            else
            {
                temp.isLeft = false;
                temp = temp.getRight();
            }
        }
        
        if( ( c = keyComparator.compare( key, parent.getKey() ) ) < 0 )
        {
            parent.setLeft( node );
        }
        else
        {
            parent.setRight( node );
        }
        
        insertInList( node, parent, c );
        
        treePath.add( 0, node );
        balance(treePath);
        
        return null;
    }
    
    
    @SuppressWarnings("unchecked")
    private K insertDupKey( K key, V value, LinkedAvlMapNode existingNode )
    {
        AvlTree<V> dupsTree = null;
        
        if( existingNode.value instanceof AvlTree )
        {
            dupsTree = ( AvlTree<V> ) existingNode.value;
        }
        else
        {
            dupsTree = new AvlTree<V>( valueComparator );
            dupsTree.insert( ( V ) existingNode.value );
            existingNode.value = dupsTree;
        }
        
        // check if value already exists
        if( dupsTree.find( value ) != null )
        {
        	return key;
        }
        
        // insert value into duplicate key holder
        dupsTree.insert( value );
        
        return null;
    }
    
    
    private void removeFromList(LinkedAvlMapNode<K,V> node)
    {
        if( node.next == null && node.previous == null ) // should happen in case of tree having single node
        {
            first = last = null;
        }
        else if( node.next == null ) // last node
        {
            node.previous.next = null;
            last = node.previous;
        }
        else if( node.previous == null ) // first node
        {
            node.next.previous = null;
            first = node.next;
        }
        else // somewhere in middle
        {
            node.previous.next = node.next;
            node.next.previous = node.previous;
        }
        
    }
    
    
    private void insertInList(LinkedAvlMapNode<K,V> node, LinkedAvlMapNode<K,V> parentNode, int pos)
    {

        if( pos < 0 )
        {
            if( last == null )
            {
              last = parentNode;  
            }
            
            if( parentNode.previous == null )
            {
                first = node;
            }
            else
            {
                parentNode.previous.next = node ;
                node.previous = parentNode.previous;
            }
            
            node.next = parentNode;
            parentNode.previous = node;
        }
        else if( pos > 0 )
        {
            if( parentNode.next == null )
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
     * Removes the LinkedAvlMapNode present in the tree with the given key and value
     *
     * @param key the key of the node to be removed
     * @param value the value of the node, if null the entire node will be removed 
     *              including any values having the same key
     * @return the removed key, if any, or null if the key does not exist
     */
    @SuppressWarnings("unchecked")
    public K remove( K key, V value )
    {
        LinkedAvlMapNode<K,V> temp = null;
        LinkedAvlMapNode<K,V> y = null;
        
        List<LinkedAvlMapNode<K,V>> treePath = new ArrayList<LinkedAvlMapNode<K,V>>();
        
        treePath = find( key, root, treePath);
        
        if( treePath == null )
        {
            return null;
        }
        
        temp = treePath.remove( 0 );

        // check if the value matches
        if( value != null )
        {
            if( temp.value instanceof AvlTree )
            {
                AvlTree<V> dupsTree = ( AvlTree<V> ) temp.value;
                V removedVal = dupsTree.remove( value );
                
                // if the removal is successful and the tree is not empty
                // we don't need to balance the tree, cause just one value
                // of the same key was removed
                // if the tree is empty because of the removal, the entire 
                // node will be removed which might require balancing, so we continue
                // further down in this function
                if( ( removedVal != null ) && ! dupsTree.isEmpty() )
                {
                    return key;//no need to balance
                }
            }
            else
            {
                if( valueComparator.compare( temp.value, value ) != 0 )
                {
                    return null;// no need to balance
                }
            }
        }
        
        // remove from the doubly linked
        removeFromList(temp);        
        
        if( temp.isLeaf() )
        {
            if( temp == root )
            {
              root = null;
              return key;
            }
            
            if( !treePath.isEmpty() )
            {
                detachNodes( temp, treePath.get( 0 ) );
            }
        }
        else
        {
            if( temp.left != null )
            {
                List<LinkedAvlMapNode<K,V>> leftTreePath = findMax( temp.left );
                y = leftTreePath.remove( 0 );
                
                if( leftTreePath.isEmpty() ) // y is the left child of root and y is a leaf
                {
                    detachNodes( y, temp );
                }
                else
                {
                    detachNodes( y, leftTreePath.remove( 0 ) );
                }
                
                leftTreePath.addAll( treePath );
                treePath = leftTreePath;
                
                y.right = temp.right; // assign the right here left will be assigned in replaceNode()

                if( temp == root )
                {
                    y.left = temp.left;
                    root = y;
                }
                else
                {
                    replaceNode( temp, y, treePath.get( 0 ) );
                }
            }
            else if( temp.right != null )
            {
                List<LinkedAvlMapNode<K,V>> rightTreePath = findMin( temp.right );
                y = rightTreePath.remove( 0 );
                
                if( rightTreePath.isEmpty() )
                {
                    detachNodes( y, temp ); // y is the right child of root and y is a leaf
                }
                else
                {
                    detachNodes( y, rightTreePath.remove( 0 ) );
                }
                
                rightTreePath.addAll( treePath );
                treePath = rightTreePath;
                
                y.right = temp.right; // assign the right here left will be assigned in replaceNode()
                
                if( temp == root )
                {
                    y.right = temp.right;
                    root = y;
                }
                else
                {
                    replaceNode( temp, y, treePath.get( 0 ) );
                }
            }
        }
       
       treePath.add( 0, y ); // y can be null but getBalance returns 0 so np
       balance( treePath );
       
       return key;
    }
    
    
    /**
     * Balances the tree by visiting the nodes present in the List of nodes present in the
     * treePath parameter.<br><br>
     *
     * This really does the balancing if the height of the tree is greater than 2 and the<br> 
     * balance factor is greater than +1 or less than -1.<br><br>
     * For an excellent info please read the 
     * <a href="http://en.wikipedia.org/wiki/Avl_tree">Wikipedia article on AVL tree</a>.
     * 
     * @param treePath the traversed list of LinkedAvlNodes after performing an insert/delete operation.
     */
    private void balance( List<LinkedAvlMapNode<K,V>> treePath )
    {
        LinkedAvlMapNode<K,V> parentNode = null;
        
        int size = treePath.size();
        
        for( LinkedAvlMapNode<K,V> node: treePath )
        {
            int balFactor = getBalance( node );

            if( node != root )
            {
                if( treePath.indexOf( node ) < ( size - 1 ) )
                {
                    parentNode = treePath.get( treePath.indexOf( node ) + 1 );
                }
            }

            if( balFactor > 1 )
            {
                if( getBalance( node.right ) <= -1)
                {
                    //------rotate double-left--------
                    rotateSingleRight( node.right, node );
                    rotateSingleLeft( node, parentNode );
                }
                else // rotate single-left
                {
                   rotateSingleLeft( node, parentNode );
                }
            }
            else if( balFactor < -1 )
            {
                if( getBalance( node.left ) >= 1)
                {
                   //------rotate double-right--------
                   rotateSingleLeft( node.left, node ); 
                   rotateSingleRight( node, parentNode );
                }
                else
                {
                    rotateSingleRight( node, parentNode );
                }
            }
        }
    }
    

    /**
     * Tests if the tree is logically empty.
     * 
     * @return true if the tree is empty, false otherwise
     */
    public boolean isEmpty()
    {
      return root == null;   
    }

    
    /**
     * returns the number of nodes present in this tree.
     * 
     * @return the number of nodes present in this tree
     */
    //NOTE: This method is internally used by AVLTreeMarshaller
    public int getSize()
    {
        if ( root == null )
        {
            return 0;
        }
        
        if( root.isLeaf() )
        {
            return 1;
        }
      
        LinkedAvlMapNode<K,V> x = first.next;
      
        while( x != null )
        {
            x.setIndex( x.previous.getIndex() + 1 );  
            x = x.next;
        }
      
        return last.getIndex() + 1;
    }
    
    
    /**
     * Set the root of the tree.
     * 
     * Note : this method is used by the deserialization method
     *
     * @param root the root of the tree
     */
    /* no protection */ void setRoot( LinkedAvlMapNode<K,V> root )
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
    /* no protection */  void setFirst( LinkedAvlMapNode<K,V> first )
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
    /* no protection */  void setLast( LinkedAvlMapNode<K,V> last )
    {
        this.last = last;
    }


    /**
     * @return the root element of this tree (ie, not the first, but the
     * topmost element)
     */
    public LinkedAvlMapNode<K,V> getRoot()
    {
        return root;
    }
    
    
    /**
     * @return a list of the stored keys in this tree
     */
    public List<K> getKeys()
    {
        List<K> keys = new ArrayList<K>();
        LinkedAvlMapNode<K,V> node = first;
        
        while( node != null )
        {
            keys.add( node.key );
            node = node.next;
        }
        
        return keys;
    }

    /**
     * Prints the contents of AVL tree in pretty format
     */
    public void printTree() 
    {
        if( isEmpty() )
        {
            System.out.println( "Tree is empty" );
            return;
        }
        
        getRoot().setDepth( 0 );

        System.out.println( getRoot() );
        
        visit( getRoot().getRight(), getRoot() );
        
        visit( getRoot().getLeft(), getRoot() );
    }
    

    /**
     * @return The first element of this tree
     */
    public LinkedAvlMapNode<K,V> getFirst()
    {
        return first;
    }

    
    /**
     * @return The last element in this tree
     */
    public LinkedAvlMapNode<K,V> getLast()
    {
        return last;
    }

    
    /**
     * Rotate the node left side once.
     *
     * @param node the LinkedAvlNode to be rotated
     * @param parentNode parent LinkedAvlNode of node
     */
    private void rotateSingleLeft(LinkedAvlMapNode<K,V> node, LinkedAvlMapNode<K,V> parentNode)
    {
        LinkedAvlMapNode<K,V> temp;
        //------rotate single-left--------
        
        temp = node.right;
        node.right = temp.left;
        temp.left = node;
        
        if( node == root )
        {
          root = temp;  
        }
        else if( parentNode != null )
        {
            if( parentNode.left == node )
            {
                parentNode.left = temp;
            }
            else if( parentNode.right == node )
            {
                parentNode.right = temp;
            }
        }
    }
    
    
    /**
     * Rotate the node right side once.
     *
     * @param node the LinkedAvlNode to be rotated
     * @param parentNode parent LinkedAvlNode of node
     */
    private void rotateSingleRight(LinkedAvlMapNode<K,V> node, LinkedAvlMapNode<K,V> parentNode)
    {
        LinkedAvlMapNode<K,V> temp;
        //------rotate single-right--------
        
        temp = node.left;
        node.left = temp.right;
        temp.right = node;
       
        if( node == root )
        {
          root = temp;  
        }
        else if( parentNode != null )
        {
            if( parentNode.left == node )
            {
                parentNode.left = temp;
            }
            else if( parentNode.right == node )
            {
                parentNode.right = temp;
            }
        }
        /*
         when the 'parentNode' param is null then the node under rotation is a child of ROOT.
         Most likely this condition executes when the root node is deleted and balancing is required.
         */
        else if( root != null )
        {
            if( root.left == node )
            {
                root.left = temp;
            }
            // no need to check for right node
        }
    }
        

    /**
     * Detach a LinkedAvlNode from its parent
     *
     * @param node the LinkedAvlNode to be detached
     * @param parentNode the parent LinkedAvlNode of the node
     */
    private void detachNodes(LinkedAvlMapNode<K,V> node, LinkedAvlMapNode<K,V> parentNode)
    {
        if( parentNode != null )
        {
            if( node == parentNode.left )
            {
                parentNode.left = node.left;
            }
            else if( node == parentNode.right )
            {
                parentNode.right = node.left;
            }
        }
    }


    /**
     * 
     * Replace a LinkedAvlNode to be removed with a new existing LinkedAvlNode 
     *
     * @param deleteNode the LinkedAvlNode to be deleted
     * @param replaceNode the LinkedAvlNode to replace the deleteNode
     * @param parentNode the parent LinkedAvlNode of deleteNode
     */
    private void replaceNode(LinkedAvlMapNode<K,V> deleteNode, LinkedAvlMapNode<K,V> replaceNode, LinkedAvlMapNode<K,V> parentNode)
    {
        if( parentNode != null )
        {
            replaceNode.left = deleteNode.left;
            
            if( deleteNode == parentNode.left )
            {
                parentNode.left = replaceNode;
            }
            else if( deleteNode == parentNode.right )
            {
                parentNode.right = replaceNode;
            }
        }
    }
    
    
    /**
     * 
     * Find a LinkedAvlNode with the given key value in the tree starting from the startNode.
     *
     * @param key the key to find
     * @param startNode starting node of a subtree/tree
     * @param path the list to be filled with traversed nodes
     * @return the list of traversed LinkedAvlNodes.
     */
    private List<LinkedAvlMapNode<K,V>> find( K key, LinkedAvlMapNode<K,V> startNode, List<LinkedAvlMapNode<K,V>> path )
    {
        int c;
        
        if( startNode == null )
        {
            return null;
        }
        
        path.add( 0, startNode );
        c = keyComparator.compare( key, startNode.key );
        
        if( c == 0 )
        {
            return path;
        }
        else if( c > 0 )
        {
            return find( key, startNode.right, path );
        }
        else if( c < 0 )
        {
            return find( key, startNode.left, path );
        }
        
        return null;
    }


    /**
     * Finds a LinkedAvlMapNode<K,V> whose key is higher than the given key.
     *
     * @param key the key
     * @return the LinkedAvlMapNode<K,V> whose key is greater than the given key ,<br>
     *         null if there is no node with a higher key than the given key.
     */
    public LinkedAvlMapNode<K,V> findGreater( K key )
    {
        LinkedAvlMapNode<K,V> result = fetchNonNullNode( key, root, root);

        if( result == null )
        {
            return null;
        }
        else if( keyComparator.compare( key, result.key ) < 0 )
        {
            return result;
        }

        return result.next;
    }


    /**
     * Finds a LinkedAvlMapNode<K,V> whose key is higher than the given key.
     *
     * @param key the key
     * @return the LinkedAvlMapNode<K,V> whose key is greater than the given key ,<br>
     *         null if there is no node with a higher key than the given key.
     */
    public LinkedAvlMapNode<K,V> findGreaterOrEqual( K key )
    {
        LinkedAvlMapNode<K,V> result = fetchNonNullNode( key, root, root);

        if( result == null )
        {
            return null;
        }
        else if( keyComparator.compare( key, result.key ) <= 0 )
        {
            return result;
        }

        return result.next;
    }


    /**
     * Finds a LinkedAvlMapNode<K,V> whose key is lower than the given key.
     *
     * @param key the key
     * @return the LinkedAvlMapNode<K,V> whose key is lower than the given key ,<br>
     *         null if there is no node with a lower key than the given key.
     */
    public LinkedAvlMapNode<K,V> findLess( K key )
    {
        LinkedAvlMapNode<K,V> result = fetchNonNullNode( key, root, root);

        if( result == null )
        {
            return null;
        }
        else if( keyComparator.compare( key, result.key ) > 0 )
        {
            return result;
        }

        return result.previous;
    }


    /**
     * Finds a LinkedAvlMapNode<K,V> whose key is lower than the given key.
     *
     * @param key the key
     * @return the LinkedAvlMapNode<K,V> whose key is lower than the given key ,<br>
     *         null if there is no node with a lower key than the given key.
     */
    public LinkedAvlMapNode<K,V> findLessOrEqual( K key )
    {
        LinkedAvlMapNode<K,V> result = fetchNonNullNode( key, root, root);

        if( result == null )
        {
            return null;
        }
        else if( keyComparator.compare( key, result.key ) >= 0 )
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
    private LinkedAvlMapNode<K,V> fetchNonNullNode( K key, LinkedAvlMapNode<K,V> startNode, LinkedAvlMapNode<K,V> parent )
    {
        
        if( startNode == null )
        {
            return parent;
        }
        
        int c = keyComparator.compare( key, startNode.key );
        
        parent = startNode;

        if( c > 0 )
        {
            return fetchNonNullNode( key, startNode.right, parent );
        }
        else if( c < 0 )
        {
            return fetchNonNullNode( key, startNode.left, parent );
        }
        
        return startNode;
    }
    
    /**
     * 
     * Find a LinkedAvlNode with the given key value in the tree.
     *
     * @param key the key to find
     * @return the list of traversed LinkedAvlNode.
     */
    public LinkedAvlMapNode<K,V> find( K key )
    {
        return find( key, root);
    }
    

    @SuppressWarnings("unchecked")
    public LinkedAvlMapNode<K,V> find( K key, V value )
    {
        if( key == null || value == null )
        {
            return null;
        }
        
        LinkedAvlMapNode<K,V> node = find( key, root);
        
        if( node == null )
        {
            return null;
        }
        
        if( node.value instanceof AvlTree )
        {
            AvlTree<V> dupsTree = ( AvlTree<V> ) node.value;
            
            if( dupsTree.find( value ) == null )
            {
                return null;
            }
        }
        else
        {
            if( valueComparator.compare( node.value, value ) != 0 )
            {
                return null;
            }
        }

        return node;
    }

    
    private LinkedAvlMapNode<K,V> find( K key, LinkedAvlMapNode<K,V> startNode)
    {
        int c;
        
        if( startNode == null )
        {
            return null;
        }
        
        c = keyComparator.compare( key, startNode.key );
        
        if( c > 0 )
        {
            startNode.isLeft = false;
            return find( key, startNode.right );
        }
        else if( c < 0 )
        {
            startNode.isLeft = true;
            return find( key, startNode.left );
        }
        
        return startNode;
    }
    
    
    /**
     * Find the LinkedAvlNode having the max key value in the tree starting from the startNode.
     *
     * @param startNode starting node of a subtree/tree
     * @return the list of traversed LinkedAvlNodes.
     */
    private List<LinkedAvlMapNode<K,V>> findMax( LinkedAvlMapNode<K,V> startNode )
    {
        LinkedAvlMapNode<K,V> x = startNode;
        LinkedAvlMapNode<K,V> y = null;
        List<LinkedAvlMapNode<K,V>> path;
        
        if( x == null )
        {
            return null;
        }
        
        while( x.right != null )
        {
            x.isLeft = false;
            y = x;
            x = x.right;
        }
        
        path = new ArrayList<LinkedAvlMapNode<K,V>>(2);
        path.add( x );
        
        if ( y != null )
        {
          path.add( y );  
        }
        
        return path;
    }

    
    /**
     * Find the LinkedAvlNode having the min key value in the tree starting from the startNode.
     *
     * @param startNode starting node of a subtree/tree
     * @return the list of traversed LinkedAvlNodes.
     */
    private List<LinkedAvlMapNode<K,V>> findMin( LinkedAvlMapNode<K,V> startNode )
    {
        LinkedAvlMapNode<K,V> x = startNode;
        LinkedAvlMapNode<K,V> y = null;
        List<LinkedAvlMapNode<K,V>> path;
       
        if( x == null )
        {
            return null;
        }
       
        while( x.left != null )
        {
            x.isLeft = true;
            y = x;
            x = x.left;
        }
        
        path = new ArrayList<LinkedAvlMapNode<K,V>>(2);
        path.add( x );
        
        if ( y != null )
        {
          path.add( y );  
        }
        
        return path;
    }
   
    
    /**
     * Get balance-factor of the given LinkedAvlNode.
     *
     * @param node a LinkedAvlNode 
     * @return balance-factor of the node
     */
    private int getBalance( LinkedAvlMapNode<K,V> node )
    {
        if( node == null)
        {
            return 0;
        }
        
        return node.getBalance();
    }
    
    
    private void visit( LinkedAvlMapNode<K,V> node, LinkedAvlMapNode<K,V> parentNode ) 
    {
        if( node == null )
        {
            return;
        }
        
        if( !node.isLeaf() )
        {
            node.setDepth( parentNode.getDepth() + 1 );
        }
        
        for( int i=0; i < parentNode.getDepth(); i++ )
        {
            System.out.print( "|  " );
        }

        String type = "";
        if( node == parentNode.left )
        {
            type = "L";
        }
        else if( node == parentNode.right )
        {
            type = "R";
        }
        
        System.out.println( "|--" + node + type );
        
        if ( node.getRight() != null )
        {
            visit( node.getRight(), node );
        }
        
        if( node.getLeft() != null )
        {
            visit( node.getLeft(), node );
        }
    }


    public boolean isDupsAllowed()
    {
        return allowDuplicates;
    }
}
