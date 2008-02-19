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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * An AVL tree testcase.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AVLTreeTest
{

    AVLTree<Integer> tree;
    
    @Before
    public void createTree()
    {
      tree = new AVLTree<Integer>( new Comparator<Integer>() 
          {

            public int compare( Integer i1, Integer i2 )
            {
                return i1.compareTo( i2 );
            }
          
          });  
    }
    
    @Test
    public void testEmpty()
    {
      assertTrue( tree.isEmpty() );
      
      tree.remove( 97 ); // remove a non-existing key
      assertTrue( tree.isEmpty() );
    }
    
    
    @Test
    public void testInsert()
    {
        tree.insert( 3 );
        assertFalse( tree.isEmpty() );
        
        tree.insert( 3 );// should be ignored
        assertEquals( 1, tree.getRoot().getHeight() );
    }
    
    
    @Test
    public void testSingleRightRotation()
    {
     // right rotation
      tree.insert( 3 );
      tree.insert( 2 );
      tree.insert( 1 );
      
      assertEquals("1,2,3", getInorderForm());
    }

    @Test
    public void testSingleLeftRotation()
    {
     // left rotation
      tree.insert( 1 );
      tree.insert( 2 );
      tree.insert( 3 );
      
      assertEquals("1,2,3", getInorderForm());
    }

    
    @Test
    public void testDoubleLeftRotation() // left-right totation
    {
     // double left rotation
      tree.insert( 1 );
      tree.insert( 3 );
      tree.insert( 2 );
      
      assertEquals("1,2,3", getInorderForm());
    }
    
    @Test
    public void testDoubleRightRotation() // right-left totation
    {
     // double left rotation
      tree.insert( 3 );
      tree.insert( 1 );
      tree.insert( 2 );
      assertEquals("1,2,3", getInorderForm());
    }
    
    
    @Test
    public void testRemove()
    {
        tree.insert( 3 );
        tree.insert( 2 );
        tree.insert( 1 );
        
        tree.remove( 2 );
        assertEquals("1,3", getInorderForm());
        
        tree.remove( 1 );
        assertEquals("3", getInorderForm());
        assertEquals( 3, tree.getRoot().key );
        
        tree.remove( 3 );
        assertTrue(tree.isEmpty());
        
        tree.insert( 37 );
        tree.insert( 39 );
        tree.insert( 27 );
        tree.insert( 38 );
        tree.insert( 21 );
        tree.insert( 26 );
        tree.insert( 43 );
        assertEquals( "21,26,27,37,38,39,43", getInorderForm() );

        tree.remove( 26 ); // remove a non-root non-leaf node in the left sub tree of root
        assertEquals( "21,27,37,38,39,43", getInorderForm() );
        
        tree.remove( 43 );
        assertEquals( "21,27,37,38,39", getInorderForm() );
        
        tree.remove( 39 );
        assertEquals( "21,27,37,38", getInorderForm() );
        assertEquals( 37, tree.getRoot().key ); // check the root value
        
        tree.remove( 38 ); // a double right rotation has to happen after this
        assertEquals( 27, tree.getRoot().key ); // check the root value after double right rotation
        assertEquals( "21,27,37", getInorderForm() );
        
        tree.printTree();
    }


    private String getInorderForm()
    {
      StringBuilder sb = new StringBuilder();
      List<LinkedAvlNode<Integer>> path = new ArrayList<LinkedAvlNode<Integer>>();
      
      traverse( tree.getRoot(), path );
      int i;
      for( i=0; i < path.size() -1; i++ )
      {
          sb.append( path.get( i ).key )
            .append( ',' );
      }
      
      sb.append( path.get( i ).key );
      
      return sb.toString();
    }
    
    private void traverse( LinkedAvlNode<Integer> startNode, List<LinkedAvlNode<Integer>> path )
    {
      //1. pre-order
        
      if( startNode.left != null )
      {
          traverse( startNode.left, path );
      }
      
      path.add( startNode ); //2. in-order NOTE: change this line's position to change the type of traversal

      if( startNode.right != null )
      {
         traverse( startNode.right, path ); 
      }

      //3. post-order
    }
}
