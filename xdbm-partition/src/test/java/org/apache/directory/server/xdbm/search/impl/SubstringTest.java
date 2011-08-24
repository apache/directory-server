/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.xdbm.search.impl;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.directory.server.xdbm.ForwardIndexEntry;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.filter.SubstringNode;
import org.junit.Test;


/**
 * Tests the SubstringCursor and the SubstringEvaluator.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SubstringTest extends TestBase
{

    @Test
    public void testIndexedCnStartsWithJ() throws Exception
    {
        SubstringNode node = new SubstringNode( schemaManager.getAttributeType( "cn" ), "j", null );
        SubstringEvaluator<Long> evaluator = new SubstringEvaluator<Long>( node, store, schemaManager );
        SubstringCursor<Long> cursor = new SubstringCursor<Long>( store, evaluator );

        assertEquals( node, evaluator.getExpression() );

        cursor.beforeFirst();

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 8, ( long ) cursor.get().getId() );
        assertEquals( "jack daniels", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 9, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 10, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 5, ( long ) cursor.get().getId() );
        assertEquals( "johnny walker", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 11, ( long ) cursor.get().getId() );
        assertEquals( "johnny walker", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        assertFalse( cursor.isClosed() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test first ----------

        cursor = new SubstringCursor<Long>( store, evaluator );
        cursor.first();

        assertTrue( cursor.available() );
        assertEquals( 8, ( long ) cursor.get().getId() );
        assertEquals( "jack daniels", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 9, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 10, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 5, ( long ) cursor.get().getId() );
        assertEquals( "johnny walker", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 11, ( long ) cursor.get().getId() );
        assertEquals( "johnny walker", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // ---------- test afterLast ----------

        cursor = new SubstringCursor<Long>( store, evaluator );
        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 11, ( long ) cursor.get().getId() );
        assertEquals( "johnny walker", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 5, ( long ) cursor.get().getId() );
        assertEquals( "johnny walker", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 10, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 9, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 8, ( long ) cursor.get().getId() );
        assertEquals( "jack daniels", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // ---------- test last ----------

        cursor = new SubstringCursor<Long>( store, evaluator );
        cursor.last();
        assertTrue( cursor.available() );

        assertEquals( 11, ( long ) cursor.get().getId() );
        assertEquals( "johnny walker", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 5, ( long ) cursor.get().getId() );
        assertEquals( "johnny walker", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 10, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 9, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 8, ( long ) cursor.get().getId() );
        assertEquals( "jack daniels", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
    }


    @Test
    public void testIndexedCnStartsWithJim() throws Exception
    {
        SubstringNode node = new SubstringNode( schemaManager.getAttributeType( "cn" ), "jim", null );
        SubstringEvaluator<Long> evaluator = new SubstringEvaluator<Long>( node, store, schemaManager );
        SubstringCursor<Long> cursor = new SubstringCursor<Long>( store, evaluator );

        assertEquals( node, evaluator.getExpression() );

        cursor.beforeFirst();

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 9, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 10, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // ---------- test first ----------

        cursor = new SubstringCursor<Long>( store, evaluator );
        cursor.first();

        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 9, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 10, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // ---------- test afterLast ----------

        cursor = new SubstringCursor<Long>( store, evaluator );
        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 10, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 9, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // ---------- test last ----------

        cursor = new SubstringCursor<Long>( store, evaluator );
        cursor.last();
        assertTrue( cursor.available() );

        assertEquals( 10, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 9, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
    }


    @Test
    public void testIndexedCnEndsWithBean() throws Exception
    {
        SubstringNode node = new SubstringNode( schemaManager.getAttributeType( "cn" ), null, "bean" );
        SubstringEvaluator<Long> evaluator = new SubstringEvaluator<Long>( node, store, schemaManager );
        SubstringCursor<Long> cursor = new SubstringCursor<Long>( store, evaluator );

        assertEquals( node, evaluator.getExpression() );

        cursor.beforeFirst();

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 9, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 10, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // ---------- test first ----------

        cursor = new SubstringCursor<Long>( store, evaluator );
        cursor.first();

        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 9, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 10, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // ---------- test afterLast ----------

        cursor = new SubstringCursor<Long>( store, evaluator );
        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 10, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 9, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // ---------- test last ----------

        cursor = new SubstringCursor<Long>( store, evaluator );
        cursor.last();
        assertTrue( cursor.available() );

        assertEquals( 10, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 9, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
    }


    @Test
    public void testNonIndexedSnStartsWithB() throws Exception
    {
        SubstringNode node = new SubstringNode( schemaManager.getAttributeType( "sn" ), "b", null );
        SubstringEvaluator<Long> evaluator = new SubstringEvaluator<Long>( node, store, schemaManager );
        SubstringCursor<Long> cursor = new SubstringCursor<Long>( store, evaluator );

        assertEquals( node, evaluator.getExpression() );

        cursor.beforeFirst();

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertEquals( "bean", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();

        // ---------- test first ----------

        cursor = new SubstringCursor<Long>( store, evaluator );
        cursor.first();

        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertEquals( "bean", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // ---------- test afterLast ----------

        cursor = new SubstringCursor<Long>( store, evaluator );
        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertEquals( "bean", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // ---------- test last ----------

        cursor = new SubstringCursor<Long>( store, evaluator );
        cursor.last();
        assertTrue( cursor.available() );

        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertEquals( "bean", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
    }


    @Test
    public void testIndexedSnEndsWithEr() throws Exception
    {
        SubstringNode node = new SubstringNode( schemaManager.getAttributeType( "sn" ), null, "er" );
        SubstringEvaluator<Long> evaluator = new SubstringEvaluator<Long>( node, store, schemaManager );
        SubstringCursor<Long> cursor = new SubstringCursor<Long>( store, evaluator );

        assertEquals( node, evaluator.getExpression() );

        cursor.beforeFirst();

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 5, ( long ) cursor.get().getId() );
        assertEquals( "walker", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // ---------- test first ----------

        cursor = new SubstringCursor<Long>( store, evaluator );
        cursor.first();

        assertTrue( cursor.available() );
        assertEquals( 5, ( long ) cursor.get().getId() );
        assertEquals( "walker", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // ---------- test afterLast ----------

        cursor = new SubstringCursor<Long>( store, evaluator );
        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 5, ( long ) cursor.get().getId() );
        assertEquals( "walker", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // ---------- test last ----------

        cursor = new SubstringCursor<Long>( store, evaluator );
        cursor.last();
        assertTrue( cursor.available() );

        assertTrue( cursor.available() );
        assertEquals( 5L, ( long ) cursor.get().getId() );
        assertEquals( "walker", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
    }


    @Test
    public void testNonIndexedAttributes() throws Exception
    {
        SubstringNode node = new SubstringNode( schemaManager.getAttributeType( "sn" ), "walk", null );
        SubstringEvaluator<Long> evaluator = new SubstringEvaluator<Long>( node, store, schemaManager );
        ForwardIndexEntry<String, Long> indexEntry = new ForwardIndexEntry<String, Long>();
        indexEntry.setId( 5L );
        assertTrue( evaluator.evaluate( indexEntry ) );
        indexEntry.setId( 3L );
        indexEntry.setEntry( null );
        assertFalse( evaluator.evaluate( indexEntry ) );
        indexEntry.setId( 6L );
        indexEntry.setEntry( null );
        assertFalse( evaluator.evaluate( indexEntry ) );

        node = new SubstringNode( schemaManager.getAttributeType( "sn" ), "wa", null );
        evaluator = new SubstringEvaluator<Long>( node, store, schemaManager );
        indexEntry = new ForwardIndexEntry<String, Long>();
        indexEntry.setId( 5L );
        indexEntry.setEntry( store.lookup( 5L ) );
        assertTrue( evaluator.evaluate( indexEntry ) );

        node = new SubstringNode( schemaManager.getAttributeType( "searchGuide" ), "j", null );
        evaluator = new SubstringEvaluator<Long>( node, store, schemaManager );
        indexEntry = new ForwardIndexEntry<String, Long>();
        indexEntry.setId( 6L );
        indexEntry.setEntry( store.lookup( 6L ) );
        assertFalse( evaluator.evaluate( indexEntry ) );

        node = new SubstringNode( schemaManager.getAttributeType( "st" ), "j", null );
        evaluator = new SubstringEvaluator<Long>( node, store, schemaManager );
        indexEntry = new ForwardIndexEntry<String, Long>();
        indexEntry.setId( 6L );
        indexEntry.setEntry( store.lookup( 6L ) );
        assertFalse( evaluator.evaluate( indexEntry ) );

        node = new SubstringNode( schemaManager.getAttributeType( "name" ), "j", null );
        evaluator = new SubstringEvaluator<Long>( node, store, schemaManager );
        indexEntry = new ForwardIndexEntry<String, Long>();
        indexEntry.setId( 6L );
        indexEntry.setEntry( store.lookup( 6L ) );
        assertTrue( evaluator.evaluate( indexEntry ) );

        node = new SubstringNode( schemaManager.getAttributeType( "name" ), "s", null );
        evaluator = new SubstringEvaluator<Long>( node, store, schemaManager );
        indexEntry = new ForwardIndexEntry<String, Long>();
        indexEntry.setId( 6L );
        indexEntry.setEntry( store.lookup( 6L ) );
        assertTrue( evaluator.evaluate( indexEntry ) );
    }


    @Test
    public void testEvaluatorIndexed() throws Exception
    {
        SubstringNode node = new SubstringNode( schemaManager.getAttributeType( "cn" ), "jim", null );
        SubstringEvaluator<Long> evaluator = new SubstringEvaluator<Long>( node, store, schemaManager );
        ForwardIndexEntry<String, Long> indexEntry = new ForwardIndexEntry<String, Long>();
        indexEntry.setId( 6L );
        assertTrue( evaluator.evaluate( indexEntry ) );
        indexEntry.setId( 3L );
        indexEntry.setEntry( null );
        assertFalse( evaluator.evaluate( indexEntry ) );

        node = new SubstringNode( schemaManager.getAttributeType( "cn" ), "j", null );
        evaluator = new SubstringEvaluator<Long>( node, store, schemaManager );
        indexEntry = new ForwardIndexEntry<String, Long>();
        indexEntry.setId( 6L );
        indexEntry.setEntry( store.lookup( 6L ) );
        assertTrue( evaluator.evaluate( indexEntry ) );

        node = new SubstringNode( schemaManager.getAttributeType( "cn" ), "s", null );
        evaluator = new SubstringEvaluator<Long>( node, store, schemaManager );
        indexEntry = new ForwardIndexEntry<String, Long>();
        indexEntry.setId( 6L );
        indexEntry.setEntry( store.lookup( 6L ) );
        assertFalse( evaluator.evaluate( indexEntry ) );
    }


    @Test
    public void testEvaluatorNotIndexed() throws Exception
    {
    }


    @Test(expected = InvalidCursorPositionException.class)
    public void testInvalidCursorPositionException() throws Exception
    {
        SubstringNode node = new SubstringNode( schemaManager.getAttributeType( "sn" ), "b", null );
        SubstringEvaluator<Long> evaluator = new SubstringEvaluator<Long>( node, store, schemaManager );
        SubstringCursor<Long> cursor = new SubstringCursor<Long>( store, evaluator );
        cursor.get();
    }


    @Test(expected = InvalidCursorPositionException.class)
    public void testInvalidCursorPositionException2() throws Exception
    {
        SubstringNode node = new SubstringNode( schemaManager.getAttributeType( "cn" ), "j", null );
        SubstringEvaluator<Long> evaluator = new SubstringEvaluator<Long>( node, store, schemaManager );
        SubstringCursor<Long> cursor = new SubstringCursor<Long>( store, evaluator );
        cursor.get();
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportBeforeWithoutIndex() throws Exception
    {
        SubstringNode node = new SubstringNode( schemaManager.getAttributeType( "sn" ), "j", null );
        SubstringEvaluator<Long> evaluator = new SubstringEvaluator<Long>( node, store, schemaManager );
        SubstringCursor<Long> cursor = new SubstringCursor<Long>( store, evaluator );

        // test before()
        ForwardIndexEntry<String, Long> entry = new ForwardIndexEntry<String, Long>();
        entry.setValue( SchemaConstants.SN_AT_OID );
        cursor.before( entry );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportAfterWithoutIndex() throws Exception
    {
        SubstringNode node = new SubstringNode( schemaManager.getAttributeType( "sn" ), "j", null );
        SubstringEvaluator<Long> evaluator = new SubstringEvaluator<Long>( node, store, schemaManager );
        SubstringCursor<Long> cursor = new SubstringCursor<Long>( store, evaluator );

        // test before()
        ForwardIndexEntry<String, Long> entry = new ForwardIndexEntry<String, Long>();
        entry.setValue( SchemaConstants.SN_AT_OID );
        cursor.after( entry );
    }
}
