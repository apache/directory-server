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

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An AVLTreeMap testcase.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class AvlTreeMapTest
{

    private static final Logger LOG = LoggerFactory.getLogger( AvlTreeTest.class );

    Comparator<Integer> comparator = new Comparator<Integer>()
    {

        public int compare( Integer i1, Integer i2 )
        {
            return i1.compareTo( i2 );
        }

    };


    private AvlTreeMapImpl<Integer, Integer> createTree()
    {
        return new AvlTreeMapImpl<Integer, Integer>( comparator, comparator, true );
    }


    @Test
    public void testEmpty()
    {
        AvlTreeMap<Integer, Integer> tree = createTree();
        assertTrue( tree.isDupsAllowed() );
        assertTrue( tree.isEmpty() );
        assertNull( tree.getFirst() );
        assertNull( tree.getLast() );

        tree.remove( 97, 0 ); // remove a non-existing key
        assertTrue( tree.isEmpty() );

        if ( LOG.isDebugEnabled() )
        {

        }
    }


    @Test
    public void testFirstAndLast()
    {
        AvlTreeMap<Integer, Integer> tree = createTree();
        tree.insert( 7, 1 );
        assertFalse( tree.isEmpty() );
        assertNotNull( tree.getFirst() );
        assertNotNull( tree.getLast() );
        assertTrue( tree.getFirst().getKey().equals( 7 ) );
        assertTrue( tree.getFirst().getValue().getSingleton().equals( 1 ) );

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
    public void testInsertWithReplace()
    {
        AvlTreeMap<Integer, Integer> tree = createTree();
        // to override the value tree should disable duplicate keys
        tree = new AvlTreeMapImpl<Integer, Integer>( comparator, comparator, false );

        assertNull( tree.insert( 43, 891 ) );
        assertEquals( 891, tree.find( 43 ).getValue().getSingleton().intValue() );

        assertNotNull( tree.insert( 43, 16 ) );
        assertEquals( 16, tree.find( 43 ).getValue().getSingleton().intValue() );
    }


    @Test
    public void testInsert()
    {
        AvlTreeMap<Integer, Integer> tree = createTree();
        assertNull( tree.insert( 3, 1 ) );
        assertFalse( tree.isEmpty() );

        assertTrue( 1 == tree.insert( 3, 1 ) );
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

        tree.remove( 24, 3 ); // this causes a single left rotation on node with key 12

        assertTrue( tree.getRoot().getLeft().key == 26 );
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testDuplicateKeyInsert()
    {
        AvlTreeMap<Integer, Integer> tree = createTree();
        assertNull( tree.insert( 3, 1 ) );
        assertNull( tree.insert( 3, 2 ) ); // duplicate key
        assertNull( tree.insert( 3, 3 ) );
        assertFalse( tree.isEmpty() );

        if ( LOG.isDebugEnabled() )
        {

        }

        assertTrue( 1 == tree.insert( 3, 1 ) );// should be ignored
        assertTrue( 1 == tree.getSize() );

        LinkedAvlMapNode node = tree.find( 3 );
        assertNotNull( node );

        assertTrue( node.value.getOrderedSet().getClass() == AvlTreeImpl.class );

        AvlTree dupsTree = ( ( SingletonOrOrderedSet ) node.value ).getOrderedSet();
        assertEquals( 3, dupsTree.getSize() );
    }


    @Test
    public void testRemove()
    {
        AvlTreeMap<Integer, Integer> tree = createTree();
        tree.insert( 3, 3 );
        tree.insert( 2, 2 );
        tree.insert( 1, 1 );

        tree.remove( 2, 2 );
        assertEquals( "1,3", getInorderForm( tree ) );

        tree.remove( 1, 1 );
        assertEquals( "3", getInorderForm( tree ) );
        assertTrue( 3 == tree.getRoot().key );

        assertNull( tree.remove( 777, 0 ) );// key not present
        assertTrue( 3 == tree.remove( 3, 3 ) );
        assertTrue( tree.isEmpty() );

        tree.insert( 37, 37 );
        tree.insert( 39, 39 );
        tree.insert( 27, 27 );
        tree.insert( 38, 38 );
        tree.insert( 21, 21 );
        tree.insert( 26, 26 );
        tree.insert( 43, 43 );
        assertEquals( "21,26,27,37,38,39,43", getInorderForm( tree ) );

        tree.remove( 26, 26 ); // remove a non-root non-leaf node in the left sub tree of root
        assertEquals( "21,27,37,38,39,43", getInorderForm( tree ) );

        tree.remove( 43, 43 );
        assertEquals( "21,27,37,38,39", getInorderForm( tree ) );

        tree.remove( 39, 39 );
        assertEquals( "21,27,37,38", getInorderForm( tree ) );

        assertTrue( 37 == tree.getRoot().key ); // check the root value

        tree.remove( 38, 38 ); // a double right rotation has to happen after this
        assertTrue( 27 == tree.getRoot().key ); // check the root value after double right rotation
        assertEquals( "21,27,37", getInorderForm( tree ) );

        if ( LOG.isDebugEnabled() )
        {

        }
    }


    @Test
    public void testRemoveWithDuplicateKeys()
    {
        AvlTreeMap<Integer, Integer> tree = createTree();
        tree.insert( 1, 1 );

        // insert duplicates
        tree.insert( 3, 3 );
        tree.insert( 3, 2 );
        tree.insert( 3, 1 );

        tree.insert( 2, 3 );

        tree.remove( 2, 3 );
        assertEquals( "1,3", getInorderForm( tree ) );
        assertEquals( 2, tree.getSize() );

        tree.remove( 3, 3 );
        // removing a duplicate key,value shouldn't change the size  
        assertEquals( "1,3", getInorderForm( tree ) );
        assertEquals( 2, tree.getSize() );

        tree.remove( 3, 3 );
        assertEquals( "1,3", getInorderForm( tree ) );
        assertEquals( 2, tree.getSize() );

        // add some more
        tree.insert( 3, 3 );
        tree.insert( 3, 4 );
        assertEquals( 2, tree.getSize() );

        tree.remove( 3, 3 );
        assertEquals( "1,3", getInorderForm( tree ) );
        assertEquals( 2, tree.getSize() );

        tree.remove( 3, 2 );
        tree.remove( 3, 1 );
        tree.remove( 3, 4 ); // is the last value in the dupsTree should remove the whole
        // node with key 3

        assertEquals( 1, tree.getSize() );
    }


    /**
     * checks the root node value(s) when duplicates are allowed and
     * only single node(size one) is present 
     */
    @Test
    public void testRemoveDuplictesOnRoot()
    {
        AvlTreeMap<Integer, Integer> tree = createTree();
        assertTrue( tree.isDupsAllowed() );
        tree.insert( 3, 4 );
        tree.insert( 3, 5 );

        assertEquals( 1, tree.getSize() );

        assertTrue( 4 == tree.remove( 3, 4 ) );
        assertNotNull( tree.getRoot() ); // still root should be not null
        assertEquals( 1, tree.getSize() );

        assertTrue( 5 == tree.remove( 3, 5 ) );
        assertNull( tree.getRoot() );

        tree.insert( 1, 1 );
        tree.insert( 1, 2 );
        tree.insert( 1, 3 );
        assertNotNull( tree.getRoot() );
        assertEquals( 1, tree.getSize() );

        tree.remove( 1 );
        assertNull( tree.getRoot() );
    }


    @Test
    public void testRemoveWithoutDupKeys()
    {
        AvlTreeMap<Integer, Integer> tree = createTree();
        tree = new AvlTreeMapImpl<Integer, Integer>( comparator, comparator, false );
        assertFalse( tree.isDupsAllowed() );

        tree.insert( 3, 4 );
        assertTrue( 4 == tree.insert( 3, 5 ) );
        assertEquals( 1, tree.getSize() );

        assertNotNull( tree.remove( 3, 5 ) );
        assertNull( tree.getRoot() );
    }


    @Test
    public void testRemoveWithKeyOnly()
    {
        AvlTreeMap<Integer, Integer> tree = createTree();
        assertTrue( tree.isDupsAllowed() );
        tree.insert( 3, 1 );
        tree.insert( 3, 2 );
        tree.insert( 4, 4 );

        SingletonOrOrderedSet<Integer> set = tree.remove( 3 );
        assertNotNull( set );
        assertNull( tree.find( 3 ) );

        AvlTree<Integer> valueTree = set.getOrderedSet();
        assertNotNull( valueTree );
        assertTrue( 2 == valueTree.getSize() );
        assertNotNull( valueTree.find( 1 ) );
        assertNotNull( valueTree.find( 2 ) );

        tree = new AvlTreeMapImpl<Integer, Integer>( comparator, comparator, false );
        tree.insert( 7, 4 );

        set = tree.remove( 7 );
        assertNotNull( set );
        assertNull( tree.find( 7 ) );
        assertTrue( 4 == set.getSingleton() );
    }


    @Test
    public void testSingleRightRotation()
    {
        AvlTreeMap<Integer, Integer> tree = createTree();
        // right rotation
        tree.insert( 3, 3 );
        tree.insert( 2, 2 );
        tree.insert( 1, 1 );

        assertEquals( "1,2,3", getInorderForm( tree ) );
    }


    @Test
    public void testRemoveAll()
    {
        AvlTreeMapImpl<Integer, Integer> tree = createTree();
        assertNull( tree.insert( 3, 1 ) );
        tree.insert( 37, 2 );

        assertFalse( tree.isEmpty() );
        assertEquals( 2, tree.getSize() );

        tree.removeAll();

        assertTrue( tree.isEmpty() );
        assertEquals( 0, tree.getSize() );
        assertNull( tree.getFirst() );
        assertNull( tree.getLast() );
        assertNull( tree.getRoot() );

        // re-insert
        assertNull( tree.insert( 3, 1 ) );
        tree.insert( 37, 2 );
        assertFalse( tree.isEmpty() );
        assertEquals( 2, tree.getSize() );
    }


    private String getInorderForm( AvlTreeMap<Integer, Integer> tree )
    {
        StringBuilder sb = new StringBuilder();
        List<LinkedAvlMapNode<Integer, Integer>> path = new ArrayList<LinkedAvlMapNode<Integer, Integer>>();

        traverse( tree.getRoot(), path );
        int i;
        for ( i = 0; i < path.size() - 1; i++ )
        {
            sb.append( path.get( i ).key )
                .append( ',' );
        }

        sb.append( path.get( i ).key );

        return sb.toString();
    }


    private void traverse( LinkedAvlMapNode<Integer, Integer> startNode, List<LinkedAvlMapNode<Integer, Integer>> path )
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

}
