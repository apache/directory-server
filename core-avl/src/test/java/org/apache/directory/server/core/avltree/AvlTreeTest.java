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

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
public class AvlTreeTest
{

    AvlTree<Integer> tree;
    
    @Before
    public void createTree()
    {
      tree = new AvlTree<Integer>( new Comparator<Integer>() 
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
      assertNull( tree.getFirst() );
      assertNull( tree.getLast() );
      
      tree.remove( 97 ); // remove a non-existing key
      assertTrue( tree.isEmpty() );
      
      tree.printTree();
    }
    
    
    @Test 
    public void testFirstAndLast()
    {
        tree.insert( 7 );
        assertFalse( tree.isEmpty() );
        assertNotNull( tree.getFirst() );
        assertNotNull( tree.getLast() );
        
        tree.insert( 10 );
        assertEquals( 2, tree.getSize() );
        assertNotNull( tree.getFirst() );
        assertNotNull( tree.getLast() );
        assertFalse( tree.getFirst().equals( tree.getLast() ) );
        assertTrue( tree.getFirst().getKey().equals( 7 ) );
        assertTrue( tree.getLast().getKey().equals( 10 ) );
        
        tree.insert( 3 );
        assertTrue( tree.getFirst().getKey().equals( 3 ) );
        assertTrue( tree.getLast().getKey().equals( 10 ) );
        
        tree.insert( 11 );
        assertTrue( tree.getFirst().getKey().equals( 3 ) );
        assertTrue( tree.getLast().getKey().equals( 11 ) );
    }
    
    
    @Test
    public void testInsert()
    {
        assertNull( tree.insert( 3 ) );
        assertFalse( tree.isEmpty() );
        
        assertTrue( 3 == tree.insert( 3 ) );// should be ignored
        assertTrue( 1 == tree.getSize() );

        assertNotNull( tree.getFirst() );
        assertNotNull( tree.getLast() );
        assertTrue( tree.getFirst() == tree.getLast() );
        
        tree.remove( 3 );
        
        tree.insert( 37 );
        tree.insert( 70 );
        tree.insert( 12 );
        assertTrue( 3 == tree.getSize() );
        
        tree.insert( 90 );
        tree.insert( 25 );
        tree.insert( 99 );
        tree.insert( 91 );
        tree.insert( 24 );
        tree.insert( 28 );
        tree.insert( 26 );
        
        tree.printTree();
        
        tree.remove( 24 ); // this causes a single left rotation on node with key 12
        tree.printTree();
        assertTrue( tree.getRoot().getLeft().key == 26 );
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
    public void testLinks()
    {
        tree.insert( 37 );
        tree.insert( 1 );
        tree.insert( 2 );
        tree.insert( 10 );
        tree.insert( 11 );
        tree.insert( 25 );
        tree.insert( 5 );
        
        assertTrue( 1 == tree.getFirst().key );
        assertTrue( 37 == tree.getLast().key );
    }
    
    @Test
    public void testRemove()
    {
        tree.insert( 3 );
        tree.insert( 2 );
        tree.insert( 1 );
        
        System.out.println(getLinkedText());
        tree.remove( 2 );
        System.out.println(getLinkedText());
        assertEquals("1,3", getInorderForm());
        
        tree.remove( 1 );
        assertEquals("3", getInorderForm());
        assertTrue( 3 == tree.getRoot().key );
        
        assertNull( tree.remove( 777 ) );// key not present
        assertTrue( 3 == tree.remove( 3 ) );
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
        
        assertTrue( 37 == tree.getRoot().key ); // check the root value
        
        tree.remove( 38 ); // a double right rotation has to happen after this
        assertTrue( 27 == tree.getRoot().key ); // check the root value after double right rotation
        assertEquals( "21,27,37", getInorderForm() );
        
        tree.printTree();
    }

    @Test
    public void testLinkedNodes()
    {
        for( int i=0; i< 3; i++)
        {
           tree.insert( i ); 
        }
        
        assertEquals( "[0]-->[1]-->[2]-->NULL", getLinkedText());
        
        tree.remove( 1 );
        assertEquals( "[0]-->[2]-->NULL", getLinkedText());
        
        tree.insert( 4 );
        tree.insert( 3 );
        
        assertEquals( "[0]-->[2]-->[3]-->[4]-->NULL", getLinkedText());
        
        tree.remove( 0 );
        assertEquals( "[2]-->[3]-->[4]-->NULL", getLinkedText());
        
        tree.remove( 3 );
        assertEquals( "[2]-->[4]-->NULL", getLinkedText());
    }
    
    @Test
    public void testFind()
    {
        tree.insert( 1 );
        tree.insert( 70 );
        tree.insert( 21 );
        tree.insert( 12 );
        tree.insert( 27 );
        tree.insert( 11 );
        
        assertNotNull( tree.find( 11 ) );
        assertNull( tree.find( 0 ));
        
        tree.setRoot( null );
        assertNull( tree.find( 3 ));
    }
    
    @Test
    public void testFindGreater()
    {
        assertNull( tree.findGreater( 1 ) );
        
        tree.insert( 1 );
        assertNull( tree.findGreater( 1 ) );
        
        tree.insert( 0 );
        tree.insert( 3 );
        tree.insert( 7 );
        tree.insert( 6 );
        tree.insert( 2 );
        tree.insert( 8 );
        
        assertFalse( 1 == tree.findGreater( 1 ).key );
        assertTrue( 2 == tree.findGreater( 1 ).key );
        assertTrue( 6 == tree.findGreater( 4 ).key );
        assertNull( tree.findGreater( 8 ) );
    }
    
    @Test
    public void testFindLess()
    {
        assertNull( tree.findLess( 1 ) );
        
        tree.insert( 1 );
        assertNull( tree.findLess( 1 ) );

        tree.insert( 2 );
        tree.insert( 7 );
        tree.insert( 3 );
        tree.insert( 6 );
        tree.insert( 0 );
        tree.insert( 37 );
        
        assertFalse( 0 == tree.findLess( 5 ).key );
        assertTrue( 3 == tree.findLess( 5 ).key );
        assertTrue( 7 == tree.findLess( 8 ).key );
        assertNull( tree.findLess( 0 ) );
    }
    
    @Test
    public void testFindMaxMin()
    {
        tree.insert( 72 );
        tree.insert( 79 );
        
        tree.remove( 72 );// should call findMax internally
        assertTrue( 79 == tree.getRoot().key );
        
        tree.insert( 11 );
        tree.remove( 79 ); // should call findMin internally
        assertTrue( 11 == tree.getRoot().key );
    }
    
    @Test
    public void testGetKeys()
    {
        tree.insert( 72 );
        tree.insert( 79 );
        tree.insert( 1 );
        tree.insert( 2 );
        tree.insert( 3 );
        tree.insert( 7 );
        tree.insert( 34 );
        
        assertTrue( 7 == tree.getKeys().size() );
    }
    
    @Test
    public void testTreeRoationAtLeftChildAfterDeletingRoot()
    {
        int[] keys = { 86, 110, 122, 2, 134, 26, 14, 182 }; // order is important to produce the expected tree
        int[] expectedKeys = { 2, 14, 26, 86, 122, 134, 182 };
        
        for( int key:keys )
        {
            tree.insert( key );
        }
        
        tree.remove( 110 );

        for ( int key : expectedKeys )
        {
            assertNotNull( "Should find " + key, tree.find( key ) );
        }
    }
    

    @Test
    public void testDetachNodesAtLeftChildAfterDeletingRoot()
    {
        int[] keys = { 110, 122, 2, 134, 86, 14, 26, 182 }; // order is important to produce the expected tree
        
        for( int key:keys )
        {
            tree.insert( key );
        }

        tree.remove( 110 );
        
        assertEquals( 26, ( int ) tree.find( 14 ).right.key );
    }


    @Test
    public void testRemoveInRightSubtree()
    {
        int[] keys = { 8, 4, 13, 6, 15, 7, 10, 5, 14, 2, 11, 3, 9, 1 }; // order is important to produce the expected tree
        
        for( int key:keys )
        {
            tree.insert( key );
        }

        tree.remove( 13 );
        
        assertEquals( 11, ( int ) tree.find( 8 ).right.key );
    }

   
    @Test
    public void testRemoveInLeftSubtree()
    {
        int[] keys = { 8, 4, 12, 6, 7, 16, 10, 5, 11, 9, 17, 5, 14, 2, 13, 1, 3 }; // order is important to produce the expected tree
        
        for( int key:keys )
        {
            tree.insert( key );
        }

        tree.remove( 16 );

        assertEquals( 8, ( int ) tree.getRoot().key );
        assertEquals( 12, ( int ) tree.getRoot().right.key );
        assertEquals( 14, ( int ) tree.getRoot().right.right.key );
        assertEquals( 13, ( int ) tree.find( 14 ).left.key );
    }
    
    
    private String getLinkedText() 
    {
        LinkedAvlNode<Integer> first = tree.getFirst();
        StringBuilder sb = new StringBuilder();
        
        while( first != null )
        {
            sb.append( first )
              .append( "-->" );
            
            first = first.next;
        }
        sb.append( "NULL" );
        return sb.toString();
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
