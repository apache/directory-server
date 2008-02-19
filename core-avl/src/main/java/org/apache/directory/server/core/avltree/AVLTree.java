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
 * @version $Rev$, $Date$
 */
public class AVLTree<K>
{

    /** the root of the tree */
	private LinkedAvlNode<K> root;

	/** The Comparator used for comparing the keys */
	private Comparator<K> comparator;

	/**
	 * Creates a new instance of AVLTree.
	 *
	 * @param comparator the comparator to be used for comparing keys
	 */
	public AVLTree( Comparator<K> comparator)
	{
	    this.comparator = comparator;
	}
	
	/**
	 * Inserts a LinkedAvlNode with the given key
	 *
	 * @param key the item to be inserted.<br> 
	 * Note: Ignores if a node with the given key already exists.
	 */
	public void insert( K key )
	{
	    LinkedAvlNode<K> node, temp;
	    LinkedAvlNode<K> parent = null;
	    int c;
	    
	    if( root == null )
	    {
	      root = new LinkedAvlNode<K>( key );
	      return;
	    }
	    
	    node = new LinkedAvlNode<K>( key );
	    temp = root;
	    
	    List<LinkedAvlNode<K>> treePath = new ArrayList<LinkedAvlNode<K>>();

	    while( temp != null )
	    {
	        treePath.add(0, temp ); // last node first, for the sake of balance factor computation
	        parent = temp;
	        
	        c = comparator.compare( key, temp.getKey() );
	        
	        if( c == 0 )
	        {
	            return; // key already exists
	        }
	        
	        if( c < 0 )
	        {
	          temp = temp.getLeft();  
	        }
	        else
	        {
	          temp = temp.getRight();
	        }
	    }
	    
	    if( ( c = comparator.compare( key, parent.getKey() ) ) < 0 )
	    {
	        parent.setLeft( node );
	    }
	    else
	    {
	        parent.setRight( node );
	    }
	 
	    treePath.add( 0, node );
	    balance(treePath);
	}
	
	
	/**
     * Removes the LinkedAvlNode present in the tree with the given key value
     *
     * @param key the value of the node to be removed
     */
    public void remove( K key )
    {
        LinkedAvlNode<K> temp = null;
        LinkedAvlNode<K> y = null;
        LinkedAvlNode<K> x = null;
        
        List<LinkedAvlNode<K>> treePath = new ArrayList<LinkedAvlNode<K>>();
        
        treePath = find( key, root, treePath);
        
        if( treePath == null )
        {
            return;
        }
        
        temp = treePath.remove( 0 );
        
        if( temp.isLeaf() )
        {
            if( temp == root )
            {
              root = null;
              return;
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
                List<LinkedAvlNode<K>> leftTreePath = findMax( temp.left );
                y = leftTreePath.remove( 0 );
                
                if( leftTreePath.isEmpty() ) // y is the left child of root and y is a leaf
                {
                    detachNodes( y, temp );
                }
                else
                {
                    detachNodes( y, leftTreePath.get( 0 ) );
                }
                
                leftTreePath.addAll( treePath );
                treePath = leftTreePath;
                
                y.right = temp.right;

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
                List<LinkedAvlNode<K>> rightTreePath = findMin( temp.right );
                y = rightTreePath.remove( 0 );
                
                if( rightTreePath.isEmpty() )
                {
                    detachNodes( y, temp ); // y is the right child of root and y is a leaf
                }
                else
                {
                    detachNodes( y, rightTreePath.get( 0 ) );
                }
                
                rightTreePath.addAll( treePath );
                treePath = rightTreePath;
                
                y.right = temp.right;
                
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
        
       balance( treePath );
    }
    
    
	/**
	 * Balances the tree by visiting the nodes present in the List of nodes present in the
	 * treePath parameter.<br><br>
	 *
	 * This really does the balancing if the hight of the tree is greater than 2 and the<br> 
	 * balance factor is greater than +1 or less than -1.<br><br>
	 * For an excellent info please read the <a href="http://en.wikipedia.org/wiki/Avl_tree">Wikipedia article on AVL tree</a>.
	 * 
	 * @param treePath the traversed list of LinkedAvlNodes after performing an insert/delete operation.
	 */
	private void balance( List<LinkedAvlNode<K>> treePath )
	{
	    LinkedAvlNode<K> parentNode = null;
	    
	    if(root.getHeight() <= 2)
	    {
	        return;
	    }
	    
	    int size = treePath.size();
	    
	    for( LinkedAvlNode<K> node: treePath )
	    {
	        int balFactor = getBalance( node );

            if( node != root )
            {
                if( treePath.indexOf( node ) < ( size - 1 ) )
                    parentNode = treePath.get( treePath.indexOf( node ) + 1 );
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

    
    public LinkedAvlNode<K> getRoot()
    {
        return root;
    }
    

    /**
     * Prints the contents of AVL tree in pretty format
     */
    public void printTree() {
        
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
    

    //-------------- private methods ----------
    
	/**
	 * Rotate the node left side once.
	 *
	 * @param node the LinkedAvlNode to be rotated
	 * @param parentNode parent LinkedAvlNode of node
	 */
	private void rotateSingleLeft(LinkedAvlNode<K> node, LinkedAvlNode<K> parentNode)
	{
	    LinkedAvlNode<K> temp;
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
            if( parentNode.left != null )
            {
                parentNode.left = temp;
            }
            else
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
	private void rotateSingleRight(LinkedAvlNode<K> node, LinkedAvlNode<K> parentNode)
	{
	    LinkedAvlNode<K> temp;
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
            if( parentNode.left != null )
            {
                parentNode.left = temp;
            }
            else
            {
                parentNode.right = temp;
            }
        }
	}
		

	/**
	 * Detach a LinkedAvlNode from its parent
	 *
	 * @param node the LinkedAvlNode to be detached
	 * @param parentNode the parent LinkedAvlNode of the node
	 */
	private void detachNodes(LinkedAvlNode<K> node, LinkedAvlNode<K> parentNode)
	{
	    if( parentNode != null )
	    {
	        if( node == parentNode.left )
	        {
	            parentNode.left = null;
	        }
	        else if( node == parentNode.right )
	        {
	            parentNode.right = null;
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
    private void replaceNode(LinkedAvlNode<K> deleteNode, LinkedAvlNode<K> replaceNode, LinkedAvlNode<K> parentNode)
    {
        if( parentNode != null )
        {
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
    private List<LinkedAvlNode<K>> find( K key, LinkedAvlNode<K> startNode, List<LinkedAvlNode<K>> path )
    {
        int c;
        
        if( startNode == null )
        {
            return null;
        }
        
        path.add( 0, startNode );
        c = comparator.compare( key, startNode.key );
        
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
	    
	    if( x == null )
	    {
	        return null;
	    }
	    
	    while( x.right != null )
	    {
	        y = x;
	        x = x.right;
	    }
	    
	    path = new ArrayList<LinkedAvlNode<K>>(2);
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
       
        if( x == null )
        {
            return null;
        }
       
        while( x.left != null )
        {
            y = x;
            x = x.left;
        }
        
        path = new ArrayList<LinkedAvlNode<K>>(2);
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
	    int rh = ( node.right == null ? 0 : node.right.getHeight() );
	    int lh = ( node.left == null ? 0 : node.left.getHeight() );
	    
	    return ( rh - lh );
	}
	
    
    private void visit( LinkedAvlNode<K> node, LinkedAvlNode<K> parentNode ) 
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
}
