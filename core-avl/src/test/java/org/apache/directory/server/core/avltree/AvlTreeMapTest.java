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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An AVLTreeMap testcase.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AvlTreeMapTest
{

    AvlTreeMap<Integer, Integer> tree;

    private static final Logger LOG = LoggerFactory.getLogger( AvlTreeTest.class );


    @Before
    public void createTree()
    {
        Comparator<Integer> comparator = new Comparator<Integer>()
        {

            public int compare( Integer i1, Integer i2 )
            {
                return i1.compareTo( i2 );
            }

        };
    
        tree = new AvlTreeMap<Integer, Integer>( comparator, comparator );
    }


    @Test
    public void testEmpty()
    {
        assertTrue( tree.isEmpty() );
        assertNull( tree.getFirst() );
        assertNull( tree.getLast() );

        tree.remove( 97, 0 ); // remove a non-existing key
        assertTrue( tree.isEmpty() );

        if ( LOG.isDebugEnabled() )
        {
            tree.printTree();
        }
    }


    @Test
    public void testFirstAndLast()
    {
        tree.insert( 7, 1 );
        assertFalse( tree.isEmpty() );
        assertNotNull( tree.getFirst() );
        assertNotNull( tree.getLast() );
        assertTrue( tree.getFirst().getKey().equals( 7 ) );
        assertTrue( tree.getFirst().getValue().equals( 1 ) );
        
        tree.insert( 10, 2 );
        assertEquals( 2, tree.getSize() );
        assertNotNull( tree.getFirst() );
        assertNotNull( tree.getLast() );
        assertFalse( tree.getFirst().equals( tree.getLast() ) );
        assertTrue( tree.getFirst().getKey().equals( 7 ) );
        assertTrue( tree.getLast().getKey().equals( 10 ) );

        tree.insert( 3, 0 );
        assertTrue( tree.getFirst().getKey().equals( 3 ) );
        assertTrue( tree.getLast().getKey().equals( 10 ) );

        tree.insert( 11, 1 );
        assertTrue( tree.getFirst().getKey().equals( 3 ) );
        assertTrue( tree.getLast().getKey().equals( 11 ) );
    }


    @Test
    public void testInsert()
    {
        assertNull( tree.insert( 3, 1 ) );
        assertFalse( tree.isEmpty() );

        assertTrue( 3 == tree.insert( 3, 1 ) );// should be ignored
        assertTrue( 1 == tree.getSize() );

        assertNotNull( tree.getFirst() );
        assertNotNull( tree.getLast() );
        assertTrue( tree.getFirst() == tree.getLast() );

        tree.remove( 3, 1 );

        tree.insert( 37, 2 );
        tree.insert( 70, 1 );
        tree.insert( 12, 0 );
        assertTrue( 3 == tree.getSize() );

        tree.insert( 90, 3 );
        tree.insert( 25, 0 );
        tree.insert( 99, 7 );
        tree.insert( 91, 5 );
        tree.insert( 24, 3 );
        tree.insert( 28, 4 );
        tree.insert( 26, 5 );

        if ( LOG.isDebugEnabled() )
        {
            tree.printTree();
        }

        tree.remove( 24, 3 ); // this causes a single left rotation on node with key 12
        if ( LOG.isDebugEnabled() )
        {
            tree.printTree();
        }
        assertTrue( tree.getRoot().getLeft().key == 26 );
    }


    @Test
    public void testDuplicateKeyInsert()
    {
        assertNull( tree.insert( 3, 1 ) );
        assertNull( tree.insert( 3, 2 ) ); // duplicate key
        assertNull( tree.insert( 3, 3 ) );
        assertFalse( tree.isEmpty() );

        if( LOG.isDebugEnabled() )
        {
            tree.printTree();
        }
        
        assertTrue( 3 == tree.insert( 3, 1 ) );// should be ignored
        assertTrue( 1 == tree.getSize() );
        
        LinkedAvlMapNode node = tree.find( 3 );
        assertNotNull( node );
     
        assertTrue( node.value.getClass() ==  AvlTree.class );
        
        AvlTree dupsTree = ( AvlTree ) node.value;
        assertEquals( 3, dupsTree.getSize() );
    }
    

    @Test
    public void testRemove()
    {
        tree.insert( 3, 3 );
        tree.insert( 2, 2 );
        tree.insert( 1, 1 );
        
        tree.remove( 2, 2 );
        assertEquals("1,3", getInorderForm());
        
        tree.remove( 1, 1 );
        assertEquals("3", getInorderForm());
        assertTrue( 3 == tree.getRoot().key );
        
        assertNull( tree.remove( 777, 0 ) );// key not present
        assertTrue( 3 == tree.remove( 3, null ) );
        assertTrue(tree.isEmpty());
        
        tree.insert( 37, 37 );
        tree.insert( 39, 39 );
        tree.insert( 27, 27 );
        tree.insert( 38, 38 );
        tree.insert( 21, 21 );
        tree.insert( 26, 26 );
        tree.insert( 43, 43 );
        assertEquals( "21,26,27,37,38,39,43", getInorderForm() );

        tree.remove( 26, 26 ); // remove a non-root non-leaf node in the left sub tree of root
        assertEquals( "21,27,37,38,39,43", getInorderForm() );

        tree.remove( 43, 43 );
        assertEquals( "21,27,37,38,39", getInorderForm() );

        tree.remove( 39, 39 );
        assertEquals( "21,27,37,38", getInorderForm() );
        
        assertTrue( 37 == tree.getRoot().key ); // check the root value
        
        tree.remove( 38, 38 ); // a double right rotation has to happen after this
        assertTrue( 27 == tree.getRoot().key ); // check the root value after double right rotation
        assertEquals( "21,27,37", getInorderForm() );
        
        if( LOG.isDebugEnabled() ) 
        {
            tree.printTree();
        }
    }

    
    @Test
    public void testRemoveWithDuplicateKeys()
    {
        tree.insert( 1, 1 );
        
        // insert deuplicates
        tree.insert( 3, 3 );
        tree.insert( 3, 2 );
        tree.insert( 3, 1 );
        
        tree.insert( 2, 3 );
        
        tree.remove( 2, 3 );
        assertEquals("1,3", getInorderForm());
        assertEquals( 2, tree.getSize() );
        
        tree.remove( 3, 3 );
        // removing a duplicate key,value shouldn't change the size  
        assertEquals("1,3", getInorderForm());
        assertEquals( 2, tree.getSize() );
        
        tree.remove( 3, null );
        assertEquals("1", getInorderForm());
        assertEquals( 1, tree.getSize() );
        
        // add some more
        tree.insert( 3, 3 );
        tree.insert( 3, 4 );
        assertEquals( 2, tree.getSize() );
        
        tree.remove( 3, 3 );
        assertEquals("1,3", getInorderForm());
        assertEquals( 2, tree.getSize() );
        
        tree.remove( 3, 4 ); // is the last value in the dupsTree should remove the whole
        // node with key 3
        assertEquals( 1, tree.getSize() );
    }
    
    
    @Test
    public void testSingleRightRotation()
    {
        // right rotation
        tree.insert( 3, 3 );
        tree.insert( 2, 2 );
        tree.insert( 1, 1 );

        assertEquals( "1,2,3", getInorderForm() );
    }

    
    private String getInorderForm()
    {
      StringBuilder sb = new StringBuilder();
      List<LinkedAvlMapNode<Integer,Integer>> path = new ArrayList<LinkedAvlMapNode<Integer,Integer>>();
      
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

    
    private void traverse( LinkedAvlMapNode<Integer,Integer> startNode, List<LinkedAvlMapNode<Integer,Integer>> path )
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
