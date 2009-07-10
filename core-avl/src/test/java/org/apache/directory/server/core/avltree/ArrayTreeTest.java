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

import java.util.Comparator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An AVL tree testcase.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ArrayTreeTest
{

    ArrayTree<Integer> tree;

    private static final Logger LOG = LoggerFactory.getLogger( ArrayTreeTest.class );


    @Before
    public void createTree()
    {
        tree = new ArrayTree<Integer>( new Comparator<Integer>()
        {

            public int compare( Integer i1, Integer i2 )
            {
                return i1.compareTo( i2 );
            }

        } );
    }


    @Test
    public void testEmpty()
    {
        assertTrue( tree.isEmpty() );
        assertNull( tree.getFirst() );
        assertNull( tree.getLast() );

        tree.remove( 97 ); // remove a non-existing key
        assertTrue( tree.isEmpty() );

        if ( LOG.isDebugEnabled() )
        {
            tree.printTree();
        }
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
        assertTrue( tree.getFirst().equals( 7 ) );
        assertTrue( tree.getLast().equals( 10 ) );

        tree.insert( 3 );
        assertTrue( tree.getFirst().equals( 3 ) );
        assertTrue( tree.getLast().equals( 10 ) );

        tree.insert( 11 );
        assertTrue( tree.getFirst().equals( 3 ) );
        assertTrue( tree.getLast().equals( 11 ) );
    }


    @Test
    public void testInsert()
    {
        assertTrue( tree.isEmpty() );

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

        if ( LOG.isDebugEnabled() )
        {
            tree.printTree();
        }

        tree.remove( 24 ); // this causes a single left rotation on node with key 12
        if ( LOG.isDebugEnabled() )
        {
            tree.printTree();
        }
        assertTrue( tree.findGreater( 24 ) == 25 );
    }


    @Test
    public void testSingleRightRotation()
    {
        // right rotation
        tree.insert( 3 );
        tree.insert( 2 );
        tree.insert( 1 );

        assertEquals( "1, 2, 3", tree.toString() );
    }


    @Test
    public void testSingleLeftRotation()
    {
        // left rotation
        tree.insert( 1 );
        tree.insert( 2 );
        tree.insert( 3 );

        assertEquals( "1, 2, 3", tree.toString() );
    }


    @Test
    public void testDoubleLeftRotation() // left-right totation
    {
        // double left rotation
        tree.insert( 1 );
        tree.insert( 3 );
        tree.insert( 2 );

        assertEquals( "1, 2, 3", tree.toString() );
    }


    @Test
    public void testDoubleRightRotation() // right-left totation
    {
        // double left rotation
        tree.insert( 3 );
        tree.insert( 1 );
        tree.insert( 2 );
        assertEquals( "1, 2, 3", tree.toString() );
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

        assertTrue( 1 == tree.getFirst() );
        assertTrue( 37 == tree.getLast() );
    }


    @Test
    public void testRemove()
    {
        tree.insert( 3 );
        tree.insert( 2 );
        tree.insert( 1 );

        LOG.debug( getLinkedText() );
        tree.remove( 2 );
        LOG.debug( getLinkedText() );
        assertEquals( "1, 3", tree.toString() );

        tree.remove( 1 );
        assertEquals( "3", tree.toString() );
        assertTrue( 3 == tree.getFirst() );

        assertNull( tree.remove( 777 ) );// key not present
        assertTrue( 3 == tree.remove( 3 ) );
        assertTrue( tree.isEmpty() );

        tree.insert( 37 );
        tree.insert( 39 );
        tree.insert( 27 );
        tree.insert( 38 );
        tree.insert( 21 );
        tree.insert( 26 );
        tree.insert( 43 );
        assertEquals( "21, 26, 27, 37, 38, 39, 43", tree.toString() );

        tree.remove( 26 ); // remove a non-root non-leaf node in the left sub tree of root
        assertEquals( "21, 27, 37, 38, 39, 43", tree.toString() );

        tree.remove( 43 );
        assertEquals( "21, 27, 37, 38, 39", tree.toString() );

        tree.remove( 39 );
        assertEquals( "21, 27, 37, 38", tree.toString() );

        tree.remove( 38 ); // a double right rotation has to happen after this

        assertEquals( "21, 27, 37", tree.toString() );

        if ( LOG.isDebugEnabled() )
        {
            tree.printTree();
        }
    }


    @Test
    public void testLinkedNodes()
    {
        for ( int i = 0; i < 3; i++ )
        {
            tree.insert( i );
        }

        assertEquals( "[0]-->[1]-->[2]-->NULL", getLinkedText() );

        tree.remove( 1 );
        assertEquals( "[0]-->[2]-->NULL", getLinkedText() );

        tree.insert( 4 );
        tree.insert( 3 );

        assertEquals( "[0]-->[2]-->[3]-->[4]-->NULL", getLinkedText() );

        tree.remove( 0 );
        assertEquals( "[2]-->[3]-->[4]-->NULL", getLinkedText() );

        tree.remove( 3 );
        assertEquals( "[2]-->[4]-->NULL", getLinkedText() );
    }


    @Test
    public void testFind()
    {
        assertNull( tree.find( 3 ) );

        tree.insert( 1 );
        tree.insert( 70 );
        tree.insert( 21 );
        tree.insert( 12 );
        tree.insert( 27 );
        tree.insert( 11 );

        assertNotNull( tree.find( 11 ) );
        assertNull( tree.find( 0 ) );
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

        assertFalse( 1 == tree.findGreater( 1 ) );
        assertTrue( 2 == tree.findGreater( 1 ) );
        assertTrue( 6 == tree.findGreater( 4 ) );
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

        assertFalse( 0 == tree.findLess( 5 ) );
        assertTrue( 3 == tree.findLess( 5 ) );
        assertTrue( 7 == tree.findLess( 8 ) );
        assertNull( tree.findLess( 0 ) );
    }


    @Test
    public void testFindMaxMin()
    {
        tree.insert( 72 );
        tree.insert( 79 );

        tree.remove( 72 );// should call findMax internally
        assertTrue( 79 == tree.getFirst() );

        tree.insert( 11 );
        tree.remove( 79 ); // should call findMin internally
        assertTrue( 11 == tree.getFirst() );
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
        int[] keys =
            { 86, 110, 122, 2, 134, 26, 14, 182 }; // order is important to produce the expected tree
        int[] expectedKeys =
            { 2, 14, 26, 86, 122, 134, 182 };

        for ( int key : keys )
        {
            tree.insert( key );
        }

        tree.remove( 110 );

        for ( int key : expectedKeys )
        {
            assertNotNull( "Should find " + key, tree.find( key ) );
        }
    }


    private String getLinkedText()
    {
        Integer first = tree.getFirst();
        StringBuilder sb = new StringBuilder();

        while ( first != null )
        {
            sb.append( "[" ).append( first ).append( "]-->" );

            first = tree.findGreater( first );
        }
        
        sb.append( "NULL" );
        return sb.toString();
    }


    private String getInorderForm()
    {
        StringBuilder sb = new StringBuilder();

        return tree.toString();
    }


    private void traverse( LinkedAvlNode<Integer> startNode, List<LinkedAvlNode<Integer>> path )
    {
        //1. pre-order

        if ( startNode.left != null )
        {
            traverse( startNode.left, path );
        }

        path.add( startNode ); //2. in-order NOTE: change this line's position to change the type of traversal

        if ( startNode.right != null )
        {
            traverse( startNode.right, path );
        }
        //3. post-order
    }


    @Test
    public void testRemoveEmptyTree()
    {
        tree.remove( null );

        assertEquals( 0, tree.getSize() );

        tree.remove( 1 );

        assertEquals( 0, tree.getSize() );
    }


    @Test
    public void testRemoveOneNode()
    {
        tree.insert( 1 );
        assertEquals( 1, tree.getSize() );

        tree.remove( 1 );
        assertEquals( 0, tree.getSize() );
    }


    @Test
    public void testRemoveOneNodeWithRight()
    {
        tree.insert( 1 );
        tree.insert( 2 );
        assertEquals( 2, tree.getSize() );
        assertEquals( "1, 2", tree.toString() );

        tree.remove( 1 );
        assertEquals( 1, tree.getSize() );
        assertEquals( Integer.valueOf( 2 ), tree.getFirst() );
    }


    @Test
    public void testNext()
    {
        tree.insert( 1 );
        tree.insert( 70 );
        tree.insert( 21 );
        tree.insert( 12 );
        tree.insert( 27 );
        tree.insert( 11 );

        assertEquals( Integer.valueOf( 1 ), tree.getFirst() );
        assertEquals( Integer.valueOf( 11 ), tree.getNext() );
        assertEquals( Integer.valueOf( 12 ), tree.getNext() );
        assertEquals( Integer.valueOf( 21 ), tree.getNext() );
        assertEquals( Integer.valueOf( 27 ), tree.getNext() );
        assertEquals( Integer.valueOf( 70 ), tree.getNext() );
        assertNull( tree.getNext() );
    }


    @Test
    public void testPrevious()
    {
        tree.insert( 1 );
        tree.insert( 70 );
        tree.insert( 21 );
        tree.insert( 12 );
        tree.insert( 27 );
        tree.insert( 11 );

        assertEquals( Integer.valueOf( 70 ), tree.getLast() );
        assertEquals( Integer.valueOf( 27 ), tree.getPrevious() );
        assertEquals( Integer.valueOf( 21 ), tree.getPrevious() );
        assertEquals( Integer.valueOf( 12 ), tree.getPrevious() );
        assertEquals( Integer.valueOf( 11 ), tree.getPrevious() );
        assertEquals( Integer.valueOf( 1 ), tree.getPrevious() );
        assertNull( tree.getPrevious() );
    }
}
