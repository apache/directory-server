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


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Comparator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An AVL tree testcase.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Execution(ExecutionMode.CONCURRENT)
public class ArrayTreeTest
{
    private static final Integer MINUS_ONE = Integer.valueOf( -1 );
    private static final Integer ZERO = Integer.valueOf( 0 );
    private static final Integer ONE = Integer.valueOf( 1 );
    private static final Integer TWO = Integer.valueOf( 2 );
    private static final Integer THREE = Integer.valueOf( 3 );
    private static final Integer FOUR = Integer.valueOf( 4 );
    private static final Integer FIVE = Integer.valueOf( 5 );
    private static final Integer SIX = Integer.valueOf( 6 );
    private static final Integer SEVEN = Integer.valueOf( 7 );
    private static final Integer EIGHT = Integer.valueOf( 8 );
    private static final Integer NINE = Integer.valueOf( 9 );
    private static final Integer TEN = Integer.valueOf( 10 );
    private static final Integer ELEVEN = Integer.valueOf( 11 );
    private static final Integer TWELVE = Integer.valueOf( 12 );
    private static final Integer THIRTY_ONE = Integer.valueOf( 31 );
    private static final Integer THIRTY_TWO = Integer.valueOf( 32 );
    private static final Integer THIRTY_SEVEN = Integer.valueOf( 37 );
    private static final Integer SEVENTY = Integer.valueOf( 70 );
    private static final Integer SEVENTY_NINE = Integer.valueOf( 79 );

    private static final Logger LOG = LoggerFactory.getLogger( ArrayTreeTest.class );


    private ArrayTree<Integer> createTree()
    {
        ArrayTree<Integer> tree;
        tree = new ArrayTree<Integer>( new Comparator<Integer>()
        {
            public int compare( Integer i1, Integer i2 )
            {
                if ( i1 == null )
                {
                    return ( i2 == null ? 0 : -1 );
                }

                return i1.compareTo( i2 );
            }

        } );
        return tree;
    }


    @Test
    public void testEmpty()
    {
        ArrayTree<Integer> tree = createTree();
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
        ArrayTree<Integer> tree = createTree();
        tree.insert( 7 );
        assertFalse( tree.isEmpty() );
        assertNotNull( tree.getFirst() );
        assertNotNull( tree.getLast() );

        tree.insert( 10 );
        assertEquals( 2, tree.size() );
        assertNotNull( tree.getFirst() );
        assertNotNull( tree.getLast() );
        assertFalse( tree.getFirst().equals( tree.getLast() ) );
        assertEquals( SEVEN, tree.getFirst() );
        assertEquals( TEN, tree.getLast() );

        tree.insert( 3 );
        assertEquals( THREE, tree.getFirst() );
        assertEquals( TEN, tree.getLast() );

        tree.insert( 11 );
        assertEquals( THREE, tree.getFirst() );
        assertEquals( ELEVEN, tree.getLast() );
    }


    @Test
    public void testSingleRightRotation()
    {
        ArrayTree<Integer> tree = createTree();
        // right rotation
        tree.insert( 3 );
        tree.insert( 2 );
        tree.insert( 1 );

        assertEquals( "1, 2, 3", tree.toString() );
    }


    @Test
    public void testSingleLeftRotation()
    {
        ArrayTree<Integer> tree = createTree();
        // left rotation
        tree.insert( 1 );
        tree.insert( 2 );
        tree.insert( 3 );

        assertEquals( "1, 2, 3", tree.toString() );
    }


    @Test
    public void testDoubleLeftRotation() // left-right totation
    {
        ArrayTree<Integer> tree = createTree();
        // double left rotation
        tree.insert( 1 );
        tree.insert( 3 );
        tree.insert( 2 );

        assertEquals( "1, 2, 3", tree.toString() );
    }


    @Test
    public void testDoubleRightRotation() // right-left totation
    {
        ArrayTree<Integer> tree = createTree();
        // double left rotation
        tree.insert( 3 );
        tree.insert( 1 );
        tree.insert( 2 );
        assertEquals( "1, 2, 3", tree.toString() );
    }


    @Test
    public void testLinks()
    {
        ArrayTree<Integer> tree = createTree();
        tree.insert( 37 );
        tree.insert( 1 );
        tree.insert( 2 );
        tree.insert( 10 );
        tree.insert( 11 );
        tree.insert( 25 );
        tree.insert( 5 );

        assertEquals( ONE, tree.getFirst() );
        assertEquals( THIRTY_SEVEN, tree.getLast() );
    }


    @Test
    public void testRemove()
    {
        ArrayTree<Integer> tree = createTree();
        tree.insert( 3 );
        tree.insert( 2 );
        tree.insert( 1 );

        LOG.debug( getLinkedText( tree ) );
        tree.remove( 2 );
        LOG.debug( getLinkedText( tree ) );
        assertEquals( "1, 3", tree.toString() );

        tree.remove( 1 );
        assertEquals( "3", tree.toString() );
        assertEquals( THREE, tree.getFirst() );

        assertNull( tree.remove( 777 ) );// key not present
        assertEquals( THREE, tree.remove( 3 ) );
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
        ArrayTree<Integer> tree = createTree();
        for ( int i = 0; i < 3; i++ )
        {
            tree.insert( i );
        }

        assertEquals( "[0]-->[1]-->[2]-->NULL", getLinkedText( tree ) );

        tree.remove( 1 );
        assertEquals( "[0]-->[2]-->NULL", getLinkedText( tree ) );

        tree.insert( 4 );
        tree.insert( 3 );

        assertEquals( "[0]-->[2]-->[3]-->[4]-->NULL", getLinkedText( tree ) );

        tree.remove( 0 );
        assertEquals( "[2]-->[3]-->[4]-->NULL", getLinkedText( tree ) );

        tree.remove( 3 );
        assertEquals( "[2]-->[4]-->NULL", getLinkedText( tree ) );
    }


    @Test
    public void testFind()
    {
        ArrayTree<Integer> tree = createTree();
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
    public void testFindMaxMin()
    {
        ArrayTree<Integer> tree = createTree();
        tree.insert( 72 );
        tree.insert( 79 );

        tree.remove( 72 );// should call findMax internally
        assertEquals( SEVENTY_NINE, tree.getFirst() );

        tree.insert( 11 );
        tree.remove( 79 ); // should call findMin internally
        assertEquals( ELEVEN, tree.getFirst() );
    }


    @Test
    public void testGetKeys()
    {
        ArrayTree<Integer> tree = createTree();
        tree.insert( 72 );
        tree.insert( 79 );
        tree.insert( 1 );
        tree.insert( 2 );
        tree.insert( 3 );
        tree.insert( 7 );
        tree.insert( 34 );

        assertEquals( 7, tree.getKeys().size() );
    }


    @Test
    public void testTreeRoationAtLeftChildAfterDeletingRoot()
    {
        ArrayTree<Integer> tree = createTree();
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
            assertNotNull( tree.find( key ), "Should find " + key );
        }
    }


    private String getLinkedText( ArrayTree<Integer> tree )
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


    @Test
    public void testRemoveOneNode()
    {
        ArrayTree<Integer> tree = createTree();
        tree.insert( 1 );
        assertEquals( 1, tree.size() );

        tree.remove( 1 );
        assertEquals( 0, tree.size() );
    }


    @Test
    public void testRemoveOneNodeWithRight()
    {
        ArrayTree<Integer> tree = createTree();
        tree.insert( 1 );
        tree.insert( 2 );
        assertEquals( 2, tree.size() );
        assertEquals( "1, 2", tree.toString() );

        tree.remove( 1 );
        assertEquals( 1, tree.size() );
        assertEquals( TWO, tree.getFirst() );
    }


    //-----------------------------------------------------------------------
    // Test insert( K value )
    //-----------------------------------------------------------------------
    @Test
    public void testInsertNullValue()
    {
        ArrayTree<Integer> tree = createTree();
        assertEquals( 0, tree.size() );
        assertNull( tree.insert( null ) );
        assertEquals( 0, tree.size() );
    }


    @Test
    public void testInsertInEmptyTree()
    {
        ArrayTree<Integer> tree = createTree();
        assertEquals( 0, tree.size() );
        assertNull( tree.insert( 0 ) );
        assertEquals( 1, tree.size() );
        assertEquals( ZERO, tree.get( 0 ) );
        assertEquals( ZERO, tree.getFirst() );
        assertEquals( ZERO, tree.getLast() );
    }


    @Test
    public void testInsertInOnElementTreeAtTheBeginning()
    {
        ArrayTree<Integer> tree = createTree();
        // Initialize the array
        assertNull( tree.insert( 5 ) );
        assertEquals( 1, tree.size() );

        Integer existing = tree.insert( ZERO );
        assertNull( existing );
        assertEquals( 2, tree.size() );
        assertEquals( ZERO, tree.getFirst() );
        assertEquals( FIVE, tree.getLast() );
        assertEquals( "0, 5", tree.toString() );
    }


    @Test
    public void testInsertInOnElementTreeAtTheEnd()
    {
        ArrayTree<Integer> tree = createTree();
        // Initialize the array
        assertNull( tree.insert( 5 ) );
        assertEquals( 1, tree.size() );

        Integer existing = tree.insert( TEN );
        assertNull( existing );
        assertEquals( 2, tree.size() );
        assertEquals( FIVE, tree.getFirst() );
        assertEquals( TEN, tree.getLast() );
        assertEquals( "5, 10", tree.toString() );
    }


    @Test
    public void testInsertInOnElementTreeExistingElement()
    {
        ArrayTree<Integer> tree = createTree();
        // Initialize the array
        assertNull( tree.insert( 5 ) );
        assertEquals( 1, tree.size() );

        Integer existing = tree.insert( FIVE );
        assertEquals( 1, tree.size() );
        assertEquals( FIVE, existing );
        assertEquals( FIVE, tree.getFirst() );
        assertEquals( FIVE, tree.getLast() );
        assertEquals( "5", tree.toString() );
    }


    @Test
    public void testInsertInEvenTreeAtTheBeginning()
    {
        ArrayTree<Integer> tree = createTree();
        // Initialize the array
        assertNull( tree.insert( ZERO ) );
        assertNull( tree.insert( TWO ) );
        assertNull( tree.insert( FOUR ) );
        assertNull( tree.insert( SIX ) );
        assertNull( tree.insert( EIGHT ) );
        assertNull( tree.insert( TEN ) );
        assertEquals( 6, tree.size() );

        Integer existing = tree.insert( MINUS_ONE );
        assertNull( existing );
        assertEquals( 7, tree.size() );
        assertEquals( MINUS_ONE, tree.getFirst() );
        assertEquals( TEN, tree.getLast() );
        assertEquals( "-1, 0, 2, 4, 6, 8, 10", tree.toString() );
    }


    @Test
    public void testInsertInEvenTreeAtTheEnd()
    {
        ArrayTree<Integer> tree = createTree();
        // Initialize the array
        assertNull( tree.insert( ZERO ) );
        assertNull( tree.insert( TWO ) );
        assertNull( tree.insert( FOUR ) );
        assertNull( tree.insert( SIX ) );
        assertNull( tree.insert( EIGHT ) );
        assertNull( tree.insert( TEN ) );
        assertEquals( 6, tree.size() );

        Integer existing = tree.insert( TWELVE );
        assertNull( existing );
        assertEquals( 7, tree.size() );
        assertEquals( ZERO, tree.getFirst() );
        assertEquals( TWELVE, tree.getLast() );
        assertEquals( "0, 2, 4, 6, 8, 10, 12", tree.toString() );
    }


    @Test
    public void testInsertInEvenTreeInTheMiddle()
    {
        ArrayTree<Integer> tree = createTree();
        // Initialize the array
        assertNull( tree.insert( ZERO ) );
        assertNull( tree.insert( TWO ) );
        assertNull( tree.insert( FOUR ) );
        assertNull( tree.insert( SIX ) );
        assertNull( tree.insert( EIGHT ) );
        assertNull( tree.insert( TEN ) );
        assertEquals( 6, tree.size() );

        // Insert 1
        Integer existing = tree.insert( ONE );
        assertNull( existing );
        assertEquals( 7, tree.size() );
        assertEquals( ZERO, tree.getFirst() );
        assertEquals( TEN, tree.getLast() );
        assertEquals( "0, 1, 2, 4, 6, 8, 10", tree.toString() );

        // Insert 5
        existing = tree.insert( FIVE );
        assertNull( existing );
        assertEquals( 8, tree.size() );
        assertEquals( ZERO, tree.getFirst() );
        assertEquals( TEN, tree.getLast() );
        assertEquals( "0, 1, 2, 4, 5, 6, 8, 10", tree.toString() );

        // Insert 9
        existing = tree.insert( NINE );
        assertNull( existing );
        assertEquals( 9, tree.size() );
        assertEquals( ZERO, tree.getFirst() );
        assertEquals( TEN, tree.getLast() );
        assertEquals( "0, 1, 2, 4, 5, 6, 8, 9, 10", tree.toString() );
    }


    @Test
    public void testInsertInEvenTreeExistingEelemnt()
    {
        ArrayTree<Integer> tree = createTree();
        // Initialize the array
        assertNull( tree.insert( ZERO ) );
        assertNull( tree.insert( TWO ) );
        assertNull( tree.insert( FOUR ) );
        assertNull( tree.insert( SIX ) );
        assertNull( tree.insert( EIGHT ) );
        assertNull( tree.insert( TEN ) );
        assertEquals( 6, tree.size() );

        // Insert 0
        Integer existing = tree.insert( ZERO );
        assertEquals( ZERO, existing );
        assertEquals( 6, tree.size() );
        assertEquals( ZERO, tree.getFirst() );
        assertEquals( TEN, tree.getLast() );
        assertEquals( "0, 2, 4, 6, 8, 10", tree.toString() );

        // Insert 6
        existing = tree.insert( SIX );
        assertEquals( SIX, existing );
        assertEquals( 6, tree.size() );
        assertEquals( ZERO, tree.getFirst() );
        assertEquals( TEN, tree.getLast() );
        assertEquals( "0, 2, 4, 6, 8, 10", tree.toString() );

        // Insert 10
        existing = tree.insert( TEN );
        assertEquals( TEN, existing );
        assertEquals( 6, tree.size() );
        assertEquals( ZERO, tree.getFirst() );
        assertEquals( TEN, tree.getLast() );
        assertEquals( "0, 2, 4, 6, 8, 10", tree.toString() );
    }


    @Test
    public void testInsertInFullTree()
    {
        ArrayTree<Integer> tree = createTree();
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;

        // Initialize the array
        for ( int i = 0; i < 32; i++ )
        {
            tree.insert( i );

            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                sb.append( ", " );
            }

            sb.append( i );
        }

        assertEquals( 32, tree.size() );
        assertEquals( sb.toString(), tree.toString() );

        assertEquals( ZERO, tree.getFirst() );
        assertEquals( THIRTY_ONE, tree.getLast() );

        // Now insert at the beginning
        tree.insert( MINUS_ONE );
        assertEquals( 33, tree.size() );
        assertEquals( MINUS_ONE, tree.getFirst() );
        assertEquals( THIRTY_ONE, tree.getLast() );

        // Insert at the end
        tree.insert( THIRTY_TWO );
        assertEquals( 34, tree.size() );
        assertEquals( MINUS_ONE, tree.getFirst() );
        assertEquals( THIRTY_TWO, tree.getLast() );
    }


    //-----------------------------------------------------------------------
    // Test remove( K value )
    //-----------------------------------------------------------------------
    @Test
    public void testRemoveEmptyTree()
    {
        ArrayTree<Integer> tree = createTree();
        assertNull( tree.remove( null ) );

        assertEquals( 0, tree.size() );

        assertNull( tree.remove( ONE ) );

        assertEquals( 0, tree.size() );
    }


    @Test
    public void testRemoveFromOnElementTree()
    {
        ArrayTree<Integer> tree = createTree();
        // Initialize the array
        assertNull( tree.insert( 5 ) );
        assertEquals( 1, tree.size() );

        Integer existing = tree.remove( FIVE );
        assertEquals( FIVE, existing );
        assertEquals( 0, tree.size() );
        assertNull( tree.getFirst() );
        assertNull( tree.getLast() );
        assertEquals( "[]", tree.toString() );
    }


    @Test
    public void testRemovefromOnElementTreeNotExistingElement()
    {
        ArrayTree<Integer> tree = createTree();
        // Initialize the array
        assertNull( tree.insert( 5 ) );
        assertEquals( 1, tree.size() );

        assertNull( tree.remove( TEN ) );
        assertEquals( 1, tree.size() );
        assertEquals( FIVE, tree.getFirst() );
        assertEquals( FIVE, tree.getLast() );
        assertEquals( "5", tree.toString() );
    }


    @Test
    public void testRemoveFromFullTreeFromTheBeginning()
    {
        ArrayTree<Integer> tree = createTree();
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;

        // Initialize the array
        for ( int i = 0; i < 32; i++ )
        {
            tree.insert( i );

            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                sb.append( ", " );
            }

            sb.append( i );
        }

        assertEquals( 32, tree.size() );
        assertEquals( sb.toString(), tree.toString() );

        int size = 32;

        for ( int i = 0; i < 32; i++ )
        {
            assertEquals( Integer.valueOf( i ), tree.remove( i ) );
            assertEquals( size - i - 1, tree.size() );

            if ( i < 31 )
            {
                assertEquals( Integer.valueOf( i + 1 ), tree.getFirst() );
                assertEquals( THIRTY_ONE, tree.getLast() );
            }
        }

        assertEquals( 0, tree.size() );
        assertNull( tree.getFirst() );
        assertNull( tree.getLast() );
    }


    @Test
    public void testRemoveFromFullTreeFromTheEnd()
    {
        ArrayTree<Integer> tree = createTree();
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;

        // Initialize the array
        for ( int i = 0; i < 32; i++ )
        {
            tree.insert( i );

            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                sb.append( ", " );
            }

            sb.append( i );
        }

        assertEquals( 32, tree.size() );
        assertEquals( sb.toString(), tree.toString() );

        int size = 32;

        for ( int i = 0; i < 32; i++ )
        {
            assertEquals( Integer.valueOf( i ), tree.remove( i ) );
            assertEquals( size - i - 1, tree.size() );

            if ( i < 31 )
            {
                assertEquals( Integer.valueOf( i + 1 ), tree.getFirst() );
                assertEquals( THIRTY_ONE, tree.getLast() );
            }
        }

        assertEquals( 0, tree.size() );
        assertNull( tree.getFirst() );
        assertNull( tree.getLast() );
    }


    //-----------------------------------------------------------------------
    // Test isEmpty()
    //-----------------------------------------------------------------------
    @Test
    public void testIsEmptyEmptyTree()
    {
        ArrayTree<Integer> tree = createTree();
        assertTrue( tree.isEmpty() );

        tree.insert( ONE );

        assertFalse( tree.isEmpty() );

        tree.remove( ONE );

        assertTrue( tree.isEmpty() );
    }


    //-----------------------------------------------------------------------
    // Test size()
    //-----------------------------------------------------------------------
    @Test
    public void testSizeEmptyTree()
    {
        ArrayTree<Integer> tree = createTree();
        assertEquals( 0, tree.size() );

        tree.insert( ONE );

        assertEquals( 1, tree.size() );

        tree.remove( ONE );

        assertEquals( 0, tree.size() );
    }


    //-----------------------------------------------------------------------
    // Test find()
    //-----------------------------------------------------------------------
    @Test
    public void testFindEmptyTree()
    {
        ArrayTree<Integer> tree = createTree();
        assertNull( tree.find( ONE ) );
    }


    @Test
    public void testFindNullFromEmptyTree()
    {
        ArrayTree<Integer> tree = createTree();
        assertNull( tree.find( null ) );
    }


    @Test
    public void testFindExistingElement()
    {
        ArrayTree<Integer> tree = createTree();
        tree.insert( ZERO );
        tree.insert( TWO );
        tree.insert( FOUR );
        tree.insert( SIX );
        tree.insert( EIGHT );

        assertEquals( ZERO, tree.find( 0 ) );
        assertEquals( TWO, tree.find( 2 ) );
        assertEquals( FOUR, tree.find( 4 ) );
        assertEquals( SIX, tree.find( 6 ) );
        assertEquals( EIGHT, tree.find( 8 ) );
    }


    @Test
    public void testFindNonExistingElement()
    {
        ArrayTree<Integer> tree = createTree();
        tree.insert( ZERO );
        tree.insert( TWO );
        tree.insert( FOUR );
        tree.insert( SIX );
        tree.insert( EIGHT );

        assertNull( tree.find( -1 ) );
        assertNull( tree.find( 1 ) );
        assertNull( tree.find( 3 ) );
        assertNull( tree.find( 5 ) );
        assertNull( tree.find( 7 ) );
        assertNull( tree.find( 9 ) );
    }


    //-----------------------------------------------------------------------
    // Test getFirst()
    //-----------------------------------------------------------------------
    @Test
    public void testGetFirstEmptyTree()
    {
        ArrayTree<Integer> tree = createTree();
        assertNull( tree.getFirst() );
    }


    @Test
    public void testGetFirst()
    {
        ArrayTree<Integer> tree = createTree();
        tree.insert( FIVE );
        assertEquals( FIVE, tree.getFirst() );

        tree.insert( TEN );
        assertEquals( FIVE, tree.getFirst() );

        tree.insert( ONE );
        assertEquals( ONE, tree.getFirst() );

        tree.insert( TWO );
        assertEquals( ONE, tree.getFirst() );

        tree.remove( ONE );
        assertEquals( TWO, tree.getFirst() );
    }


    //-----------------------------------------------------------------------
    // Test getLast()
    //-----------------------------------------------------------------------
    @Test
    public void testGetLastEmptyTree()
    {
        ArrayTree<Integer> tree = createTree();
        assertNull( tree.getLast() );
    }


    @Test
    public void testGetLast()
    {
        ArrayTree<Integer> tree = createTree();
        tree.insert( FIVE );
        assertEquals( FIVE, tree.getLast() );

        tree.insert( TEN );
        assertEquals( TEN, tree.getLast() );

        tree.insert( EIGHT );
        assertEquals( TEN, tree.getLast() );

        tree.insert( ELEVEN );
        assertEquals( ELEVEN, tree.getLast() );

        tree.remove( ELEVEN );
        assertEquals( TEN, tree.getLast() );
    }


    //-----------------------------------------------------------------------
    // Test findGreater()
    //-----------------------------------------------------------------------
    @Test
    public void testFindGreaterEmptyTree()
    {
        ArrayTree<Integer> tree = createTree();
        assertNull( tree.findGreater( ONE ) );
    }


    @Test
    public void testFindGreaterNullFromEmptyTree()
    {
        ArrayTree<Integer> tree = createTree();
        assertNull( tree.findGreater( null ) );
    }


    @Test
    public void testFindGreaterFromOneElementTree()
    {
        ArrayTree<Integer> tree = createTree();
        tree.insert( TWO );

        assertNull( tree.findGreater( null ) );
        assertEquals( TWO, tree.findGreater( ONE ) );
        assertNull( tree.findGreater( TWO ) );
        assertNull( tree.findGreater( THREE ) );
    }


    @Test
    public void testFindGreaterFromTwoElementsTree()
    {
        ArrayTree<Integer> tree = createTree();
        tree.insert( TWO );
        tree.insert( FOUR );

        assertNull( tree.findGreater( null ) );
        assertEquals( TWO, tree.findGreater( ONE ) );
        assertEquals( FOUR, tree.findGreater( TWO ) );
        assertEquals( FOUR, tree.findGreater( THREE ) );
        assertNull( tree.findGreater( FOUR ) );
        assertNull( tree.findGreater( FIVE ) );
    }


    @Test
    public void testFindGreater()
    {
        ArrayTree<Integer> tree = createTree();
        tree.insert( ZERO );
        tree.insert( TWO );
        tree.insert( FOUR );
        tree.insert( SIX );
        tree.insert( EIGHT );
        tree.insert( TEN );

        assertEquals( ZERO, tree.findGreater( MINUS_ONE ) );
        assertNotSame( ONE, tree.findGreater( ONE ) );
        assertEquals( TWO, tree.findGreater( ONE ) );
        assertEquals( SIX, tree.findGreater( FIVE ) );
        tree.remove( SIX );
        assertEquals( EIGHT, tree.findGreater( FIVE ) );
        assertNull( tree.findGreater( TEN ) );
    }


    //-----------------------------------------------------------------------
    // Test findGreaterOrEqual()
    //-----------------------------------------------------------------------
    @Test
    public void testFindGreaterOrEqualEmptyTree()
    {
        ArrayTree<Integer> tree = createTree();
        assertNull( tree.findGreaterOrEqual( ONE ) );
    }


    @Test
    public void testFindGreaterOrEqualNullFromEmptyTree()
    {
        ArrayTree<Integer> tree = createTree();
        assertNull( tree.findGreaterOrEqual( null ) );
    }


    @Test
    public void testFindGreaterOrEqualFromOneElementTree()
    {
        ArrayTree<Integer> tree = createTree();
        tree.insert( TWO );

        assertNull( tree.findGreaterOrEqual( null ) );
        assertEquals( TWO, tree.findGreaterOrEqual( ONE ) );
        assertEquals( TWO, tree.findGreaterOrEqual( TWO ) );
        assertNull( tree.findGreaterOrEqual( THREE ) );
    }


    @Test
    public void testFindGreaterOrEqualFromTwoElementsTree()
    {
        ArrayTree<Integer> tree = createTree();
        tree.insert( TWO );
        tree.insert( FOUR );

        assertNull( tree.findGreaterOrEqual( null ) );
        assertEquals( TWO, tree.findGreaterOrEqual( ONE ) );
        assertEquals( TWO, tree.findGreaterOrEqual( TWO ) );
        assertEquals( FOUR, tree.findGreaterOrEqual( THREE ) );
        assertEquals( FOUR, tree.findGreaterOrEqual( FOUR ) );
        assertNull( tree.findGreaterOrEqual( FIVE ) );
    }


    @Test
    public void testFindGreaterOrEqual()
    {
        ArrayTree<Integer> tree = createTree();
        tree.insert( ZERO );
        tree.insert( TWO );
        tree.insert( FOUR );
        tree.insert( SIX );
        tree.insert( EIGHT );
        tree.insert( TEN );

        assertEquals( ZERO, tree.findGreater( MINUS_ONE ) );
        assertNotSame( ONE, tree.findGreaterOrEqual( ONE ) );
        assertEquals( TWO, tree.findGreaterOrEqual( ONE ) );
        assertEquals( TWO, tree.findGreaterOrEqual( TWO ) );
        assertEquals( SIX, tree.findGreaterOrEqual( FIVE ) );
        tree.remove( SIX );
        assertEquals( EIGHT, tree.findGreaterOrEqual( FIVE ) );
        assertEquals( TEN, tree.findGreaterOrEqual( TEN ) );
        assertNull( tree.findGreaterOrEqual( ELEVEN ) );
    }


    //-----------------------------------------------------------------------
    // Test findLess()
    //-----------------------------------------------------------------------
    @Test
    public void testFindLessEmptyTree()
    {
        ArrayTree<Integer> tree = createTree();
        assertNull( tree.findLess( ONE ) );
    }


    @Test
    public void testFindLessNullFromEmptyTree()
    {
        ArrayTree<Integer> tree = createTree();
        assertNull( tree.findLess( null ) );
    }


    @Test
    public void testFindLessFromOneElementTree()
    {
        ArrayTree<Integer> tree = createTree();
        tree.insert( TWO );

        assertNull( tree.findLess( null ) );
        assertNull( tree.findLess( ONE ) );
        assertNull( tree.findLess( TWO ) );
        assertEquals( TWO, tree.findLess( THREE ) );
    }


    @Test
    public void testFindLessFromTwoElementsTree()
    {
        ArrayTree<Integer> tree = createTree();
        tree.insert( TWO );
        tree.insert( FOUR );

        assertNull( tree.findLess( null ) );
        assertNull( tree.findLess( ONE ) );
        assertNull( tree.findLess( TWO ) );
        assertEquals( TWO, tree.findLess( THREE ) );
        assertEquals( TWO, tree.findLess( FOUR ) );
        assertEquals( FOUR, tree.findLess( FIVE ) );
    }


    @Test
    public void testFindLess()
    {
        ArrayTree<Integer> tree = createTree();
        tree.insert( ZERO );
        tree.insert( TWO );
        tree.insert( FOUR );
        tree.insert( SIX );
        tree.insert( EIGHT );
        tree.insert( TEN );

        assertNull( tree.findLess( ZERO ) );
        assertNotSame( ONE, tree.findLess( ONE ) );
        assertEquals( ZERO, tree.findLess( ONE ) );
        assertEquals( FOUR, tree.findLess( FIVE ) );
        tree.remove( FOUR );
        assertEquals( TWO, tree.findLess( FIVE ) );
        assertEquals( EIGHT, tree.findLess( TEN ) );
        assertEquals( TEN, tree.findLess( SEVENTY ) );
    }


    //-----------------------------------------------------------------------
    // Test findLessOrEqual()
    //-----------------------------------------------------------------------
    @Test
    public void testFindLessOrEqualEmptyTree()
    {
        ArrayTree<Integer> tree = createTree();
        assertNull( tree.findLessOrEqual( ONE ) );
    }


    @Test
    public void testFindLessOrEqualNullFromEmptyTree()
    {
        ArrayTree<Integer> tree = createTree();
        assertNull( tree.findLessOrEqual( null ) );
    }


    @Test
    public void testFindLessOrEqualFromOneElementTree()
    {
        ArrayTree<Integer> tree = createTree();
        tree.insert( TWO );

        assertNull( tree.findLessOrEqual( null ) );
        assertNull( tree.findLessOrEqual( ONE ) );
        assertEquals( TWO, tree.findLessOrEqual( TWO ) );
        assertEquals( TWO, tree.findLessOrEqual( THREE ) );
    }


    @Test
    public void testFindLessOrEqualFromTwoElementsTree()
    {
        ArrayTree<Integer> tree = createTree();
        tree.insert( TWO );
        tree.insert( FOUR );

        assertNull( tree.findLessOrEqual( null ) );
        assertNull( tree.findLessOrEqual( ONE ) );
        assertEquals( TWO, tree.findLessOrEqual( TWO ) );
        assertEquals( TWO, tree.findLessOrEqual( THREE ) );
        assertEquals( FOUR, tree.findLessOrEqual( FOUR ) );
        assertEquals( FOUR, tree.findLessOrEqual( FIVE ) );
    }


    @Test
    public void testFindLessOrEqual()
    {
        ArrayTree<Integer> tree = createTree();
        tree.insert( ZERO );
        tree.insert( TWO );
        tree.insert( FOUR );
        tree.insert( SIX );
        tree.insert( EIGHT );
        tree.insert( TEN );

        assertNull( tree.findLessOrEqual( MINUS_ONE ) );
        assertEquals( ZERO, tree.findLessOrEqual( ZERO ) );
        assertEquals( ZERO, tree.findLessOrEqual( ONE ) );
        assertEquals( TWO, tree.findLessOrEqual( THREE ) );
        assertEquals( FOUR, tree.findLessOrEqual( FIVE ) );
        tree.remove( FOUR );
        assertEquals( TWO, tree.findLessOrEqual( FIVE ) );
        assertEquals( EIGHT, tree.findLessOrEqual( NINE ) );
        assertEquals( TEN, tree.findLessOrEqual( TEN ) );
        assertEquals( TEN, tree.findLessOrEqual( ELEVEN ) );
    }


    //-----------------------------------------------------------------------
    // Test get( int position )
    //-----------------------------------------------------------------------
    @Test
    public void testGetEmptyTree()
    {
        ArrayTree<Integer> tree = createTree();
        try
        {
            assertNull( tree.get( 0 ) );
            fail();
        }
        catch ( ArrayIndexOutOfBoundsException aioobe )
        {
            assertTrue( true );
        }
    }


    @Test
    public void testGetEmptyTreeAIOOBException()
    {
        ArrayTree<Integer> tree = createTree();
        try
        {
            tree.get( -1 );
            fail();
        }
        catch ( ArrayIndexOutOfBoundsException aioobe )
        {
            assertTrue( true );
        }

        try
        {
            tree.get( 1 );
            fail();
        }
        catch ( ArrayIndexOutOfBoundsException aioobe )
        {
            assertTrue( true );
        }
    }


    //-----------------------------------------------------------------------
    // Test getAfterPosition( K key )
    //-----------------------------------------------------------------------
    @Test
    public void testGetAfterPositionEmptyTree()
    {
        ArrayTree<Integer> tree = createTree();
        assertEquals( -1, tree.getAfterPosition( ZERO ) );
    }


    @Test
    public void testGetAfterPositionOneElementTree()
    {
        ArrayTree<Integer> tree = createTree();
        tree.insert( TWO );

        assertEquals( -1, tree.getAfterPosition( null ) );
        assertEquals( 0, tree.getAfterPosition( ZERO ) );
        assertEquals( -1, tree.getAfterPosition( TWO ) );
        assertEquals( -1, tree.getAfterPosition( FOUR ) );
    }


    @Test
    public void testGetAfterPositionTwoElementsTree()
    {
        ArrayTree<Integer> tree = createTree();
        tree.insert( TWO );
        tree.insert( FOUR );

        assertEquals( -1, tree.getAfterPosition( null ) );
        assertEquals( 0, tree.getAfterPosition( ZERO ) );
        assertEquals( 1, tree.getAfterPosition( TWO ) );
        assertEquals( 1, tree.getAfterPosition( THREE ) );
        assertEquals( -1, tree.getAfterPosition( FOUR ) );
    }


    @Test
    public void testGetAfterPositionNElementsTree()
    {
        ArrayTree<Integer> tree = createTree();
        tree.insert( TWO );
        tree.insert( FOUR );
        tree.insert( SIX );
        tree.insert( EIGHT );

        assertEquals( -1, tree.getAfterPosition( null ) );
        assertEquals( 0, tree.getAfterPosition( ZERO ) );
        assertEquals( 1, tree.getAfterPosition( TWO ) );
        assertEquals( 1, tree.getAfterPosition( THREE ) );
        assertEquals( 2, tree.getAfterPosition( FOUR ) );
        assertEquals( 2, tree.getAfterPosition( FIVE ) );
        assertEquals( 3, tree.getAfterPosition( SIX ) );
        assertEquals( 3, tree.getAfterPosition( SEVEN ) );
        assertEquals( -1, tree.getAfterPosition( EIGHT ) );
    }


    //-----------------------------------------------------------------------
    // Test getBeforePosition( K key )
    //-----------------------------------------------------------------------
    @Test
    public void testGetBeforePositionEmptyTree()
    {
        ArrayTree<Integer> tree = createTree();
        assertEquals( -1, tree.getBeforePosition( ZERO ) );
    }


    @Test
    public void testGetBeforePositionOneElementTree()
    {
        ArrayTree<Integer> tree = createTree();
        tree.insert( TWO );

        assertEquals( -1, tree.getBeforePosition( null ) );
        assertEquals( -1, tree.getBeforePosition( ZERO ) );
        assertEquals( -1, tree.getBeforePosition( TWO ) );
        assertEquals( 0, tree.getBeforePosition( FOUR ) );
    }


    @Test
    public void testGetBeforePositionTwoElementsTree()
    {
        ArrayTree<Integer> tree = createTree();
        tree.insert( TWO );
        tree.insert( FOUR );

        assertEquals( -1, tree.getBeforePosition( null ) );
        assertEquals( -1, tree.getBeforePosition( ZERO ) );
        assertEquals( -1, tree.getBeforePosition( TWO ) );
        assertEquals( 0, tree.getBeforePosition( THREE ) );
        assertEquals( 0, tree.getBeforePosition( FOUR ) );
        assertEquals( 1, tree.getBeforePosition( FIVE ) );
    }


    @Test
    public void testGetBeforePositionNElementsTree()
    {
        ArrayTree<Integer> tree = createTree();
        tree.insert( TWO );
        tree.insert( FOUR );
        tree.insert( SIX );
        tree.insert( EIGHT );

        assertEquals( -1, tree.getBeforePosition( null ) );
        assertEquals( -1, tree.getBeforePosition( ZERO ) );
        assertEquals( -1, tree.getBeforePosition( TWO ) );
        assertEquals( 0, tree.getBeforePosition( THREE ) );
        assertEquals( 0, tree.getBeforePosition( FOUR ) );
        assertEquals( 1, tree.getBeforePosition( FIVE ) );
        assertEquals( 1, tree.getBeforePosition( SIX ) );
        assertEquals( 2, tree.getBeforePosition( SEVEN ) );
        assertEquals( 2, tree.getBeforePosition( EIGHT ) );
        assertEquals( 3, tree.getBeforePosition( NINE ) );
    }
}
