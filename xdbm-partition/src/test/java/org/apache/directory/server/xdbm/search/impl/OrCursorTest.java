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
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.apache.directory.server.xdbm.ForwardIndexEntry;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.filter.FilterParser;
import org.apache.directory.shared.ldap.model.filter.OrNode;
import org.apache.directory.shared.ldap.model.filter.SubstringNode;
import org.junit.Before;
import org.junit.Test;


/**
 * 
 * Test class for OrCursor.
 * 
 * Note: The results of OrCursor need not be symmetric, in the sense that the results obtained<br>
 * in a particular order (say first to last ) need not be same as the results obtained in the<br> 
 * order last to first, cause each underlying cursor of OrCursor will have elements in their own order.<br>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class OrCursorTest extends TestBase
{
    EvaluatorBuilder<Long> evaluatorBuilder;
    CursorBuilder<Long> cursorBuilder;


    @Before
    public void createBuilder() throws Exception
    {
        evaluatorBuilder = new EvaluatorBuilder<Long>( store, schemaManager );
        cursorBuilder = new CursorBuilder<Long>( store, evaluatorBuilder );
    }


    @Test
    public void testOrCursorUsingCursorBuilder() throws Exception
    {
        String filter = "(|(cn=J*)(sn=W*))";

        ExprNode exprNode = FilterParser.parse( schemaManager, filter );

        IndexCursor<?, Entry, Long> cursor = cursorBuilder.build( exprNode );

        cursor.afterLast();

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 5, ( long ) cursor.get().getId() );
        assertEquals( "walker", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 11, ( long ) cursor.get().getId() );
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
        assertEquals( 11, ( long ) cursor.get().getId() );
        assertEquals( "johnny walker", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 5, ( long ) cursor.get().getId() );
        assertEquals( "walker", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        cursor.close();
        assertTrue( cursor.isClosed() );
    }


    @Test(expected = InvalidCursorPositionException.class)
    @SuppressWarnings("unchecked")
    public void testOrCursor() throws Exception
    {
        List<Evaluator<? extends ExprNode, Entry, Long>> evaluators = new ArrayList<Evaluator<? extends ExprNode, Entry, Long>>();
        List<Cursor<IndexEntry<?, Long>>> cursors = new ArrayList<Cursor<IndexEntry<?, Long>>>();
        Evaluator<? extends ExprNode, Entry, Long> eval;
        Cursor<IndexEntry<?, Long>> cursor;

        OrNode orNode = new OrNode();

        ExprNode exprNode = new SubstringNode( schemaManager.getAttributeType( "cn" ), "J", null );
        eval = new SubstringEvaluator( ( SubstringNode ) exprNode, store, schemaManager );
        Cursor subStrCursor1 = new SubstringCursor( store, ( SubstringEvaluator ) eval );
        cursors.add( subStrCursor1 );
        evaluators.add( eval );
        orNode.addNode( exprNode );

        //        try
        //        {
        //            new OrCursor( cursors, evaluators );
        //            fail( "should throw IllegalArgumentException" );
        //        }
        //        catch( IllegalArgumentException ie ){ }

        exprNode = new SubstringNode( schemaManager.getAttributeType( "sn" ), "W", null );
        eval = new SubstringEvaluator( ( SubstringNode ) exprNode, store, schemaManager );
        evaluators.add( eval );
        Cursor subStrCursor2 = new SubstringCursor( store, ( SubstringEvaluator ) eval );
        cursors.add( subStrCursor2 );

        orNode.addNode( exprNode );

        cursor = new OrCursor( cursors, evaluators );

        cursor.beforeFirst();
        assertFalse( cursor.available() );

        // from first
        assertTrue( cursor.first() );
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
        assertEquals( "walker", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 11, ( long ) cursor.get().getId() );
        assertEquals( "johnny walker", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // from last        
        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.last() );
        assertTrue( cursor.available() );
        assertEquals( 11, ( long ) cursor.get().getId() );
        assertEquals( "johnny walker", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 5, ( long ) cursor.get().getId() );
        assertEquals( "walker", cursor.get().getValue() );

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

        try
        {
            cursor.after( new ForwardIndexEntry() );
            fail( "should fail with UnsupportedOperationException " );
        }
        catch ( UnsupportedOperationException uoe )
        {
        }

        try
        {
            cursor.before( new ForwardIndexEntry() );
            fail( "should fail with UnsupportedOperationException " );
        }
        catch ( UnsupportedOperationException uoe )
        {
        }

        cursor.get();
    }
}
