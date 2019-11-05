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
    public Comparator<K> getComparator()
    {
        return comparator;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.avltree.AvlTree#insert(K)
     */
    public K insert( K key )
    {
        LinkedAvlNode<K> node, temp;
        LinkedAvlNode<K> parent = null;
        int c;

        if ( root == null )
        {
            root = new LinkedAvlNode<>( key );
            first = root;
            last = root;
            size++;
            return null;
        }

        node = new LinkedAvlNode<>( key );

        temp = root;

        List<LinkedAvlNode<K>> treePath = new ArrayList<>();

        while ( temp != null )
        {
            treePath.add( 0, temp ); // last node first, for the sake of balance factor computation
            parent = temp;

            c = comparator.compare( key, temp.getKey() );

            if ( c == 0 )
            {
                return key; // key already exists
            }

            if ( c < 0 )
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

        c = comparator.compare( key, parent.getKey() );
        if ( c < 0 )
        {
            parent.setLeft( node );
        }
        else
        {
            parent.setRight( node );
        }

        insertInList( node, parent, c );

        treePath.add( 0, node );
        balance( treePath );

        size++;
        return null;
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
    public K remove( K key )
    {
        LinkedAvlNode<K> temp = null;
        LinkedAvlNode<K> y = null;

        List<LinkedAvlNode<K>> treePath = new ArrayList<>();

        treePath = find( key, root, treePath );

        if ( treePath == null )
        {
            return null;
        }

        temp = treePath.remove( 0 );

        // remove from the doubly linked
        removeFromList( temp );

        if ( temp.isLeaf() )
        {
            if ( temp == root )
            {
                root = null;
                size--;
                return key;
            }

            if ( !treePath.isEmpty() )
            {
                detachNodes( temp, treePath.get( 0 ) );
            }
        }
        else
        {
            if ( temp.left != null )
            {
                List<LinkedAvlNode<K>> leftTreePath = findMax( temp.left );
                y = leftTreePath.remove( 0 );

                if ( leftTreePath.isEmpty() ) // y is the left child of root and y is a leaf
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

                if ( temp == root )
                {
                    y.left = temp.left;
                    root = y;
                }
                else
                {
                    replaceNode( temp, y, treePath.get( 0 ) );
                }
            }
            else if ( temp.right != null )
            {
                List<LinkedAvlNode<K>> rightTreePath = findMin( temp.right );
                y = rightTreePath.remove( 0 );

                if ( rightTreePath.isEmpty() )
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

                if ( temp == root )
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

        size--;
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
    private void balance( List<LinkedAvlNode<K>> treePath )
    {
        LinkedAvlNode<K> parentNode = null;

        int treePathSize = treePath.size();

        for ( LinkedAvlNode<K> node : treePath )
        {
            int balFactor = getBalance( node );

            if ( node != root && treePath.indexOf( node ) < ( treePathSize - 1 ) )
            {
                parentNode = treePath.get( treePath.indexOf( node ) + 1 );
            }

            if ( balFactor > 1 )
            {
                if ( getBalance( node.right ) <= -1 )
                {
                    //------rotate double-left--------
                    rotateSingleRight( node.right, node );
                    rotateSingleLeft( node, parentNode );
                }
                else
                // rotate single-left
                {
                    rotateSingleLeft( node, parentNode );
                }
            }
            else if ( balFactor < -1 )
            {
                if ( getBalance( node.left ) >= 1 )
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


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.avltree.AvlTree#isEmpty()
     */
    public boolean isEmpty()
    {
        return root == null;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.avltree.AvlTree#getSize()
     */
    //NOTE: This method is internally used by AVLTreeMarshaller
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
    public LinkedAvlNode<K> getRoot()
    {
        return root;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.avltree.AvlTree#getKeys()
     */
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
    public void printTree()
    {
        if ( isEmpty() )
        {
            System.out.println( "Tree is empty" );
            return;
        }

        getRoot().setDepth( 0 );

        System.out.println( getRoot() );

        visit( getRoot().getRight(), getRoot() );

        visit( getRoot().getLeft(), getRoot() );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.avltree.AvlTree#getFirst()
     */
    public LinkedAvlNode<K> getFirst()
    {
        return first;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.avltree.AvlTree#getLast()
     */
    public LinkedAvlNode<K> getLast()
    {
        return last;
    }


    /**
     * Rotate the node left side once.
     *
     * @param node the LinkedAvlNode to be rotated
     * @param parentNode parent LinkedAvlNode of node
     */
    private void rotateSingleLeft( LinkedAvlNode<K> node, LinkedAvlNode<K> parentNode )
    {
        LinkedAvlNode<K> temp;
        //------rotate single-left--------

        temp = node.right;
        node.right = temp.left;
        temp.left = node;

        if ( node == root )
        {
            root = temp;
        }
        else if ( parentNode != null )
        {
            if ( parentNode.left == node )
            {
                parentNode.left = temp;
            }
            else if ( parentNode.right == node )
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
    private void rotateSingleRight( LinkedAvlNode<K> node, LinkedAvlNode<K> parentNode )
    {
        LinkedAvlNode<K> temp;
        //------rotate single-right--------

        temp = node.left;
        node.left = temp.right;
        temp.right = node;

        if ( node == root )
        {
            root = temp;
        }
        else if ( parentNode != null )
        {
            if ( parentNode.left == node )
            {
                parentNode.left = temp;
            }
            else if ( parentNode.right == node )
            {
                parentNode.right = temp;
            }
        }
        /*
         when the 'parentNode' param is null then the node under rotation is a child of ROOT.
         Most likely this condition executes when the root node is deleted and balancing is required.
         */
        else if ( root != null && root.left == node )
        {
            root.left = temp;
            // no need to check for right node
        }
    }


    /**
     * Detach a LinkedAvlNode from its parent
     *
     * @param node the LinkedAvlNode to be detached
     * @param parentNode the parent LinkedAvlNode of the node
     */
    private void detachNodes( LinkedAvlNode<K> node, LinkedAvlNode<K> parentNode )
    {
        if ( parentNode != null )
        {
            if ( node == parentNode.left )
            {
                parentNode.left = node.left;
            }
            else if ( node == parentNode.right )
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
    private void replaceNode( LinkedAvlNode<K> deleteNode, LinkedAvlNode<K> replaceNode, LinkedAvlNode<K> parentNode )
    {
        if ( parentNode != null )
        {
            replaceNode.left = deleteNode.left;

            if ( deleteNode == parentNode.left )
            {
                parentNode.left = replaceNode;
            }
            else if ( deleteNode == parentNode.right )
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
    private List<LinkedAvlNode<K>> find( K key, LinkedAvlNode<K> startNode, List<LinkedAvlNode<K>> path )
    {
        int c;

        if ( startNode == null )
        {
            return null;
        }

        path.add( 0, startNode );
        c = comparator.compare( key, startNode.key );

        if ( c == 0 )
        {
            return path;
        }
        else if ( c > 0 )
        {
            return find( key, startNode.right, path );
        }
        else
        {
            return find( key, startNode.left, path );
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.avltree.AvlTree#findGreater(K)
     */
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
            startNode.isLeft = false;
            return find( key, startNode.right );
        }
        else if ( c < 0 )
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
    private List<LinkedAvlNode<K>> findMax( LinkedAvlNode<K> startNode )
    {
        LinkedAvlNode<K> x = startNode;
        LinkedAvlNode<K> y = null;
        List<LinkedAvlNode<K>> path;

        if ( x == null )
        {
            return null;
        }

        while ( x.right != null )
        {
            x.isLeft = false;
            y = x;
            x = x.right;
        }

        path = new ArrayList<>( 2 );
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
    private List<LinkedAvlNode<K>> findMin( LinkedAvlNode<K> startNode )
    {
        LinkedAvlNode<K> x = startNode;
        LinkedAvlNode<K> y = null;
        List<LinkedAvlNode<K>> path;

        if ( x == null )
        {
            return null;
        }

        while ( x.left != null )
        {
            x.isLeft = true;
            y = x;
            x = x.left;
        }

        path = new ArrayList<>( 2 );
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
    private int getBalance( LinkedAvlNode<K> node )
    {
        if ( node == null )
        {
            return 0;
        }

        return node.getBalance();
    }


    private void visit( LinkedAvlNode<K> node, LinkedAvlNode<K> parentNode )
    {
        if ( node == null )
        {
            return;
        }

        if ( !node.isLeaf() )
        {
            node.setDepth( parentNode.getDepth() + 1 );
        }

        for ( int i = 0; i < parentNode.getDepth(); i++ )
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
            visit( node.getRight(), node );
        }

        if ( node.getLeft() != null )
        {
            visit( node.getLeft(), node );
        }
    }
}
