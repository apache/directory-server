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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.xdbm.ForwardIndexEntry;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.csn.CsnFactory;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.cursor.Tuple;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.StringValue;
import org.apache.directory.shared.ldap.model.filter.LessEqNode;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.LdapSyntax;
import org.apache.directory.shared.ldap.model.schema.MatchingRule;
import org.apache.directory.shared.ldap.model.schema.comparators.StringComparator;
import org.apache.directory.shared.ldap.model.schema.parsers.SyntaxCheckerDescription;
import org.junit.Test;


/**
 * Tests the LessEqEvaluator and LessEqCursor classes for correct operation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LessEqTest extends TestBase
{

    @Test
    public void testCursorIndexed() throws Exception
    {
        AttributeType at = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.POSTALCODE_AT_OID );
        LessEqNode node = new LessEqNode( at, new StringValue( at, "3" ) );
        LessEqEvaluator evaluator = new LessEqEvaluator( node, store, schemaManager );
        LessEqCursor<String, Long> cursor = new LessEqCursor<String, Long>( store, evaluator );
        assertNotNull( cursor );
        assertFalse( cursor.available() );
        assertFalse( cursor.isClosed() );

        // ---------- test bad get() ----------

        try
        {
            cursor.get();
            fail();
        }
        catch ( InvalidCursorPositionException e )
        {
        }

        // ---------- test beforeFirst() ----------

        cursor.beforeFirst();
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 1L, ( long ) cursor.get().getId() );
        assertEquals( "1", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 2L, ( long ) cursor.get().getId() );
        assertEquals( "1", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 3L, ( long ) cursor.get().getId() );
        assertEquals( "1", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 4L, ( long ) cursor.get().getId() );
        assertEquals( "2", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 5L, ( long ) cursor.get().getId() );
        assertEquals( "3", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test first() ----------

        cursor = new LessEqCursor( store, evaluator );

        cursor.first();

        assertTrue( cursor.available() );
        assertEquals( 1L, ( long ) cursor.get().getId() );
        assertEquals( "1", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 2L, ( long ) cursor.get().getId() );
        assertEquals( "1", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 3L, ( long ) cursor.get().getId() );
        assertEquals( "1", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 4L, ( long ) cursor.get().getId() );
        assertEquals( "2", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 5L, ( long ) cursor.get().getId() );
        assertEquals( "3", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test afterLast() ----------

        cursor = new LessEqCursor( store, evaluator );

        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 5L, ( long ) cursor.get().getId() );
        assertEquals( "3", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 4L, ( long ) cursor.get().getId() );
        assertEquals( "2", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 3L, ( long ) cursor.get().getId() );
        assertEquals( "1", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 2L, ( long ) cursor.get().getId() );
        assertEquals( "1", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 1L, ( long ) cursor.get().getId() );
        assertEquals( "1", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test last() ----------

        cursor = new LessEqCursor( store, evaluator );

        cursor.last();

        assertTrue( cursor.available() );
        assertEquals( 5L, ( long ) cursor.get().getId() );
        assertEquals( "3", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 4L, ( long ) cursor.get().getId() );
        assertEquals( "2", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 3L, ( long ) cursor.get().getId() );
        assertEquals( "1", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 2L, ( long ) cursor.get().getId() );
        assertEquals( "1", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 1L, ( long ) cursor.get().getId() );
        assertEquals( "1", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test before() ----------

        cursor = new LessEqCursor( store, evaluator );
        ForwardIndexEntry<String, Long> indexEntry = new ForwardIndexEntry<String, Long>();
        indexEntry.setValue( "2" );

        assertFalse( cursor.available() );
        cursor.before( indexEntry );
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 4L, ( long ) cursor.get().getId() );
        assertEquals( "2", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 5L, ( long ) cursor.get().getId() );
        assertEquals( "3", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        cursor = new LessEqCursor( store, evaluator );
        indexEntry = new ForwardIndexEntry<String, Long>();
        indexEntry.setValue( "7" );
        cursor.before( indexEntry );
        assertFalse( cursor.available() );
        assertTrue( cursor.previous() );
        assertEquals( 5L, ( long ) cursor.get().getId() );
        assertEquals( "3", cursor.get().getValue() );
        cursor.close();

        cursor = new LessEqCursor( store, evaluator );
        indexEntry = new ForwardIndexEntry<String, Long>();
        indexEntry.setValue( "3" );
        cursor.before( indexEntry );
        assertFalse( cursor.available() );
        assertTrue( cursor.next() );
        assertEquals( 5L, ( long ) cursor.get().getId() );
        assertEquals( "3", cursor.get().getValue() );
        cursor.close();

        // ---------- test after() ----------

        cursor = new LessEqCursor( store, evaluator );
        indexEntry = new ForwardIndexEntry<String, Long>();
        indexEntry.setValue( "1" );

        assertFalse( cursor.available() );
        cursor.after( indexEntry );
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 4L, ( long ) cursor.get().getId() );
        assertEquals( "2", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 5L, ( long ) cursor.get().getId() );
        assertEquals( "3", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        cursor = new LessEqCursor( store, evaluator );
        indexEntry = new ForwardIndexEntry<String, Long>();
        indexEntry.setValue( "7" );
        cursor.after( indexEntry );
        assertFalse( cursor.available() );
        assertTrue( cursor.previous() );
        assertEquals( 5L, ( long ) cursor.get().getId() );
        assertEquals( "3", cursor.get().getValue() );
        cursor.close();

        cursor = new LessEqCursor( store, evaluator );
        indexEntry = new ForwardIndexEntry<String, Long>();
        indexEntry.setValue( "3" );
        cursor.after( indexEntry );
        assertFalse( cursor.available() );
        assertTrue( cursor.previous() );
        assertEquals( 5L, ( long ) cursor.get().getId() );
        assertEquals( "3", cursor.get().getValue() );
        cursor.close();
    }


    @Test
    public void testCursorNotIndexed() throws Exception
    {
        AttributeType at = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.POSTOFFICEBOX_AT_OID );
        LessEqNode node = new LessEqNode( at, new StringValue( at, "3" ) );
        LessEqEvaluator evaluator = new LessEqEvaluator( node, store, schemaManager );
        LessEqCursor<String, Long> cursor = new LessEqCursor<String, Long>( store, evaluator );
        assertNotNull( cursor );
        assertFalse( cursor.available() );
        assertFalse( cursor.isClosed() );

        // ---------- test bad get() ----------

        try
        {
            cursor.get();
            fail();
        }
        catch ( InvalidCursorPositionException e )
        {
        }

        // ---------- test beforeFirst() ----------

        Set<Tuple<String, Long>> set = new HashSet<Tuple<String, Long>>();
        cursor.beforeFirst();
        assertFalse( cursor.available() );

        while ( cursor.next() )
        {
            assertTrue( cursor.available() );
            set.add( new Tuple<String, Long>( cursor.get().getValue(), cursor.get().getId() ) );
        }
        assertEquals( 5, set.size() );
        assertTrue( set.contains( new Tuple<String, Long>( "1", 1L ) ) );
        assertTrue( set.contains( new Tuple<String, Long>( "1", 2L ) ) );
        assertTrue( set.contains( new Tuple<String, Long>( "1", 3L ) ) );
        assertTrue( set.contains( new Tuple<String, Long>( "2", 4L ) ) );
        assertTrue( set.contains( new Tuple<String, Long>( "3", 5L ) ) );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        assertFalse( cursor.isClosed() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test beforeFirst() ----------

        set.clear();
        cursor = new LessEqCursor( store, evaluator );
        cursor.first();

        assertTrue( cursor.available() );
        set.add( new Tuple<String, Long>( cursor.get().getValue(), cursor.get().getId() ) );

        while ( cursor.next() )
        {
            assertTrue( cursor.available() );
            set.add( new Tuple<String, Long>( cursor.get().getValue(), cursor.get().getId() ) );
        }
        assertEquals( 5, set.size() );
        assertTrue( set.contains( new Tuple<String, Long>( "1", 1L ) ) );
        assertTrue( set.contains( new Tuple<String, Long>( "1", 2L ) ) );
        assertTrue( set.contains( new Tuple<String, Long>( "1", 3L ) ) );
        assertTrue( set.contains( new Tuple<String, Long>( "2", 4L ) ) );
        assertTrue( set.contains( new Tuple<String, Long>( "3", 5L ) ) );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        assertFalse( cursor.isClosed() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test afterLast() ----------

        set.clear();
        cursor = new LessEqCursor( store, evaluator );
        cursor.afterLast();
        assertFalse( cursor.available() );

        while ( cursor.previous() )
        {
            assertTrue( cursor.available() );
            set.add( new Tuple<String, Long>( cursor.get().getValue(), cursor.get().getId() ) );
        }
        assertEquals( 5, set.size() );
        assertTrue( set.contains( new Tuple<String, Long>( "1", 1L ) ) );
        assertTrue( set.contains( new Tuple<String, Long>( "1", 2L ) ) );
        assertTrue( set.contains( new Tuple<String, Long>( "1", 3L ) ) );
        assertTrue( set.contains( new Tuple<String, Long>( "2", 4L ) ) );
        assertTrue( set.contains( new Tuple<String, Long>( "3", 5L ) ) );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // ---------- test last() ----------

        set.clear();
        cursor = new LessEqCursor( store, evaluator );
        cursor.last();

        assertTrue( cursor.available() );
        set.add( new Tuple<String, Long>( cursor.get().getValue(), cursor.get().getId() ) );

        while ( cursor.previous() )
        {
            assertTrue( cursor.available() );
            set.add( new Tuple<String, Long>( cursor.get().getValue(), cursor.get().getId() ) );
        }
        assertEquals( 5, set.size() );
        assertTrue( set.contains( new Tuple<String, Long>( "1", 1L ) ) );
        assertTrue( set.contains( new Tuple<String, Long>( "1", 2L ) ) );
        assertTrue( set.contains( new Tuple<String, Long>( "1", 3L ) ) );
        assertTrue( set.contains( new Tuple<String, Long>( "2", 4L ) ) );
        assertTrue( set.contains( new Tuple<String, Long>( "3", 5L ) ) );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // ---------- test before() ----------

        cursor = new LessEqCursor( store, evaluator );
        ForwardIndexEntry<String, Long> indexEntry = new ForwardIndexEntry<String, Long>();
        indexEntry.setValue( "2" );
        try
        {
            cursor.before( indexEntry );
            fail( "Should never get here." );
        }
        catch ( UnsupportedOperationException e )
        {
        }

        // ---------- test after() ----------

        cursor = new LessEqCursor( store, evaluator );
        indexEntry = new ForwardIndexEntry<String, Long>();
        indexEntry.setValue( "2" );
        try
        {
            cursor.after( indexEntry );
            fail( "Should never get here." );
        }
        catch ( UnsupportedOperationException e )
        {
        }
    }


    // -----------------------------------------------------------------------
    // Evaluator Test Cases
    // -----------------------------------------------------------------------

    @Test
    public void testEvaluatorIndexed() throws Exception
    {
        AttributeType at = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.POSTALCODE_AT_OID );
        LessEqNode node = new LessEqNode( at, new StringValue( at, "3" ) );

        LessEqEvaluator evaluator = new LessEqEvaluator( node, store, schemaManager );
        ForwardIndexEntry<String, Long> indexEntry = new ForwardIndexEntry<String, Long>();
        assertEquals( node, evaluator.getExpression() );
        assertEquals( SchemaConstants.POSTALCODE_AT_OID, evaluator.getAttributeType().getOid() );
        assertNotNull( evaluator.getNormalizer() );
        assertNotNull( evaluator.getComparator() );

        indexEntry.setId( 1L );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String, Long>();
        indexEntry.setId( 4L );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String, Long>();
        indexEntry.setId( 5L );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String, Long>();
        indexEntry.setId( 6L );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String, Long>();
        indexEntry.setId( 7L );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String, Long>();
        indexEntry.setId( 8L );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String, Long>();
        indexEntry.setId( 9L );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String, Long>();
        indexEntry.setId( 10L );
        assertFalse( evaluator.evaluate( indexEntry ) );
    }


    @Test
    public void testEvaluatorWithDescendantValue() throws Exception
    {
        AttributeType at = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.STREET_AT_OID );
        LessEqNode node = new LessEqNode( at, new StringValue( at, "2" ) );

        LessEqEvaluator evaluator = new LessEqEvaluator( node, store, schemaManager );
        ForwardIndexEntry<String, Long> indexEntry = new ForwardIndexEntry<String, Long>();
        assertEquals( node, evaluator.getExpression() );
        assertEquals( SchemaConstants.STREET_AT_OID, evaluator.getAttributeType().getOid() );
        assertNotNull( evaluator.getNormalizer() );
        assertNotNull( evaluator.getComparator() );

        Dn dn = new Dn( schemaManager, "cn=jane doe,o=good times co." );
        Entry attrs = new DefaultEntry( schemaManager, dn );
        attrs.add( "objectClass", "person" );
        attrs.add( "c-street", "1" );
        attrs.add( "cn", "jane doe" );
        attrs.add( "sn", "doe" );
        attrs.add( "entryCSN", new CsnFactory( 1 ).newInstance().toString() );
        attrs.add( "entryUUID", UUID.randomUUID().toString() );
        
        AddOperationContext addContext = new AddOperationContext( null, attrs );
        ((Partition)store).add( addContext );

        indexEntry.setId( 12L );
        assertTrue( evaluator.evaluate( indexEntry ) );
    }


    @Test
    public void testEvaluatorWithoutDescendants() throws Exception
    {
        AttributeType at = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.C_POSTALCODE_AT_OID );
        LessEqNode node = new LessEqNode( at, new StringValue( at, "2" ) );

        LessEqEvaluator evaluator = new LessEqEvaluator( node, store, schemaManager );
        ForwardIndexEntry<String, Long> indexEntry = new ForwardIndexEntry<String, Long>();
        assertEquals( node, evaluator.getExpression() );
        assertEquals( SchemaConstants.C_POSTALCODE_AT_OID, evaluator.getAttributeType().getOid() );
        assertNotNull( evaluator.getNormalizer() );
        assertNotNull( evaluator.getComparator() );

        indexEntry.setId( 1L );
        assertFalse( evaluator.evaluate( indexEntry ) );
    }


    @Test
    public void testEvaluatorNotIndexed() throws Exception
    {
        AttributeType at = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.POSTOFFICEBOX_AT_OID );
        LessEqNode node = new LessEqNode( at, new StringValue( at, "3" ) );

        LessEqEvaluator evaluator = new LessEqEvaluator( node, store, schemaManager );
        ForwardIndexEntry<String, Long> indexEntry = new ForwardIndexEntry<String, Long>();
        assertEquals( node, evaluator.getExpression() );
        assertEquals( SchemaConstants.POSTOFFICEBOX_AT_OID, evaluator.getAttributeType().getOid() );
        assertNotNull( evaluator.getNormalizer() );
        assertNotNull( evaluator.getComparator() );

        indexEntry.setId( 1L );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String, Long>();
        indexEntry.setId( 4L );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String, Long>();
        indexEntry.setId( 5L );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String, Long>();
        indexEntry.setId( 6L );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String, Long>();
        indexEntry.setId( 7L );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String, Long>();
        indexEntry.setId( 8L );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String, Long>();
        indexEntry.setId( 9L );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String, Long>();
        indexEntry.setId( 10L );
        assertFalse( evaluator.evaluate( indexEntry ) );
    }


    @Test(expected = IllegalStateException.class)
    public void testEvaluatorAttributeNoMatchingRule() throws Exception
    {
        LdapSyntax syntax = new BogusSyntax( 1 );
        AttributeType at = new AttributeType( SchemaConstants.ATTRIBUTE_TYPES_AT_OID + ".2000" );
        at.addName( "bogus" );
        at.setSchemaName( "other" );
        at.setSyntax( syntax );

        assertTrue( schemaManager.add( syntax ) );
        assertTrue( schemaManager.add( at ) );

        try
        {
            LessEqNode node = new LessEqNode( at, new StringValue( at, "3" ) );

            new LessEqEvaluator( node, store, schemaManager );
        }
        finally
        {
            assertTrue( schemaManager.delete( at ) );
            assertTrue( schemaManager.delete( syntax ) );
        }
    }


    @Test
    public void testEvaluatorAttributeOrderingMatchingRule() throws Exception
    {
        LdapSyntax syntax = new BogusSyntax( 2 );

        MatchingRule mr = new MatchingRule( "1.1" );
        mr.setSyntax( syntax );
        mr.setLdapComparator( new StringComparator( "1.1" ) );

        AttributeType at = new AttributeType( SchemaConstants.ATTRIBUTE_TYPES_AT_OID + ".3000" );
        at.addName( "bogus" );
        at.setSchemaName( "other" );
        at.setSyntax( syntax );
        at.setOrdering( mr );

        assertTrue( schemaManager.add( syntax ) );
        assertTrue( schemaManager.add( mr ) );
        assertTrue( schemaManager.add( at ) );

        SyntaxCheckerDescription desc = new SyntaxCheckerDescription( at.getSyntax().getOid() );
        desc.setDescription( "bogus" );
        desc.setFqcn( BogusSyntax.class.getName() );
        List<String> names = new ArrayList<String>();
        names.add( "bogus" );
        desc.setNames( names );
        desc.setObsolete( false );

        LessEqNode node = new LessEqNode( at, new StringValue( at, "3" ) );
        new LessEqEvaluator( node, store, schemaManager );
        schemaManager.delete( at );
    }
}
