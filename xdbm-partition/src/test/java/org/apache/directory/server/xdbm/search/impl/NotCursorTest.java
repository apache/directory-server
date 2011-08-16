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

import java.util.HashSet;
import java.util.Set;

import org.apache.directory.server.xdbm.ForwardIndexEntry;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.filter.FilterParser;
import org.apache.directory.shared.ldap.model.filter.NotNode;
import org.apache.directory.shared.ldap.model.filter.SubstringNode;
import org.apache.directory.shared.ldap.model.schema.syntaxCheckers.UuidSyntaxChecker;
import org.junit.Before;
import org.junit.Test;


/**
 * 
 * Test cases for NotCursor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NotCursorTest extends TestBase
{
    UuidSyntaxChecker uuidSynChecker = new UuidSyntaxChecker();

    EvaluatorBuilder<Long> evaluatorBuilder;
    CursorBuilder<Long> cursorBuilder;


    @Before
    public void createBuilders() throws Exception
    {
        evaluatorBuilder = new EvaluatorBuilder<Long>( store, schemaManager );
        cursorBuilder = new CursorBuilder<Long>( store, evaluatorBuilder );
    }


    @Test
    public void testNotCursor() throws Exception
    {
        String filter = "(!(cn=J*))";

        ExprNode exprNode = FilterParser.parse( schemaManager, filter );

        IndexCursor<?, Entry, Long> cursor = cursorBuilder.build( exprNode );

        assertFalse( cursor.available() );

        cursor.beforeFirst();

        Set<Long> set = new HashSet<Long>();
        while ( cursor.next() )
        {
            assertTrue( cursor.available() );
            set.add( cursor.get().getId() );
            assertTrue( uuidSynChecker.isValidSyntax( cursor.get().getValue() ) );
        }
        assertEquals( 5, set.size() );
        assertTrue( set.contains( 1L ) );
        assertTrue( set.contains( 2L ) );
        assertTrue( set.contains( 3L ) );
        assertTrue( set.contains( 4L ) );
        assertTrue( set.contains( 7L ) );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        cursor.close();
        assertTrue( cursor.isClosed() );
    }


    @Test
    public void testNotCursorWithManualFilter() throws Exception
    {
        NotNode notNode = new NotNode();

        ExprNode exprNode = new SubstringNode( schemaManager.getAttributeType( "cn" ), "J", null );
        Evaluator<? extends ExprNode, Entry, Long> eval = new SubstringEvaluator( (SubstringNode) exprNode, store,
            schemaManager );
        notNode.addNode( exprNode );

        NotCursor<String, Long> cursor = new NotCursor( store, eval ); //cursorBuilder.build( andNode );
        cursor.beforeFirst();

        Set<Long> set = new HashSet<Long>();
        while ( cursor.next() )
        {
            assertTrue( cursor.available() );
            set.add( cursor.get().getId() );
            assertTrue( uuidSynChecker.isValidSyntax( cursor.get().getValue() ) );
        }
        assertEquals( 5, set.size() );
        assertTrue( set.contains( 1L ) );
        assertTrue( set.contains( 2L ) );
        assertTrue( set.contains( 3L ) );
        assertTrue( set.contains( 4L ) );
        assertTrue( set.contains( 7L ) );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        cursor.afterLast();

        set.clear();
        while ( cursor.previous() )
        {
            assertTrue( cursor.available() );
            set.add( cursor.get().getId() );
            assertTrue( uuidSynChecker.isValidSyntax( cursor.get().getValue() ) );
        }
        assertEquals( 5, set.size() );
        assertTrue( set.contains( 1L ) );
        assertTrue( set.contains( 2L ) );
        assertTrue( set.contains( 3L ) );
        assertTrue( set.contains( 4L ) );
        assertTrue( set.contains( 7L ) );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        try
        {
            cursor.get();
            fail( "should fail with InvalidCursorPositionException" );
        }
        catch ( InvalidCursorPositionException ice )
        {
        }

        try
        {
            cursor.after( new ForwardIndexEntry<String, Entry, Long>() );
            fail( "should fail with UnsupportedOperationException " );
        }
        catch ( UnsupportedOperationException uoe )
        {
        }

        try
        {
            cursor.before( new ForwardIndexEntry<String, Entry, Long>() );
            fail( "should fail with UnsupportedOperationException " );
        }
        catch ( UnsupportedOperationException uoe )
        {
        }
    }

}
