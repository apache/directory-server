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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.ForwardIndexEntry;
import org.apache.directory.server.xdbm.tools.StoreUtils;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmStore;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.entry.ServerStringValue;
import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.csn.CsnFactory;
import org.apache.directory.shared.ldap.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.filter.GreaterEqNode;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.SchemaUtils;
import org.apache.directory.shared.ldap.schema.comparators.SerializableComparator;
import org.apache.directory.shared.ldap.schema.comparators.StringComparator;
import org.apache.directory.shared.ldap.schema.ldif.extractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.normalizers.NoOpNormalizer;
import org.apache.directory.shared.ldap.schema.parsers.SyntaxCheckerDescription;
import org.apache.directory.shared.ldap.schema.registries.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.schema.loader.ldif.LdifSchemaLoader;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * Tests the GreaterEqEvaluator and GreaterEqCursor classes for correct operation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $$Rev$$
 */
public class GreaterEqTest
{
    public static final Logger LOG = LoggerFactory.getLogger( GreaterEqTest.class );


    File wkdir;
    Store<ServerEntry> store;
    static Registries registries = null;
    static AttributeTypeRegistry attributeRegistry;


    @BeforeClass
    public static void setup() throws Exception
    {
        // setup the standard registries
    	String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = GreaterEqTest.class.getResource( "" ).getPath();
            int targetPos = path.indexOf( "target" );
            workingDirectory = path.substring( 0, targetPos + 6 );
        }

        File schemaRepository = new File( workingDirectory, "schema" );
        SchemaLdifExtractor extractor = new SchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy();
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        registries = new Registries();
        loader.loadAllEnabled( registries );
        loader.loadWithDependencies( loader.getSchema( "collective" ), registries );
        SerializableComparator.setRegistry( registries.getComparatorRegistry() );

        attributeRegistry = registries.getAttributeTypeRegistry();
    }


    @Before
    public void createStore() throws Exception
    {
        destryStore();

        // setup the working directory for the store
        wkdir = File.createTempFile( getClass().getSimpleName(), "db" );
        wkdir.delete();
        wkdir = new File( wkdir.getParentFile(), getClass().getSimpleName() );
        wkdir.mkdirs();

        // initialize the store
        store = new JdbmStore<ServerEntry>();
        store.setName( "example" );
        store.setCacheSize( 10 );
        store.setWorkingDirectory( wkdir );
        store.setSyncOnWrite( false );

        store.addIndex( new JdbmIndex( SchemaConstants.OU_AT_OID ) );
        store.addIndex( new JdbmIndex( SchemaConstants.CN_AT_OID ) );
        store.addIndex( new JdbmIndex( SchemaConstants.POSTALCODE_AT_OID ) );

        StoreUtils.loadExampleData( store, registries );
        LOG.debug( "Created new store" );
    }


    @After
    public void destryStore() throws Exception
    {
        if ( store != null )
        {
            store.destroy();
        }

        store = null;
        if ( wkdir != null )
        {
            FileUtils.deleteDirectory( wkdir );
        }

        wkdir = null;
    }


    @Test
    public void testCursorIndexed() throws Exception
    {
        AttributeType at = attributeRegistry.lookup( SchemaConstants.POSTALCODE_AT_OID );
        GreaterEqNode node = new GreaterEqNode( SchemaConstants.POSTALCODE_AT_OID, new ServerStringValue( at, "3" ) );
        GreaterEqEvaluator evaluator = new GreaterEqEvaluator( node, store, registries );
        GreaterEqCursor cursor = new GreaterEqCursor( store, evaluator );
        assertNotNull( cursor );
        assertFalse( cursor.available() );
        assertTrue( cursor.isElementReused() );
        assertFalse( cursor.isClosed() );

        // ---------- test bad get() ----------

        try { cursor.get(); fail(); }
        catch( InvalidCursorPositionException e ) {}

        // ---------- test beforeFirst() ----------

        cursor.beforeFirst();
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 5L, ( long ) cursor.get().getId() );
        assertEquals( "3", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 6L, ( long ) cursor.get().getId() );
        assertEquals( "4", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 7L, ( long ) cursor.get().getId() );
        assertEquals( "5", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 8L, ( long ) cursor.get().getId() );
        assertEquals( "6", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test first() ----------

        cursor = new GreaterEqCursor( store, evaluator );

        cursor.first();

        assertTrue( cursor.available() );
        assertEquals( 5L, ( long ) cursor.get().getId() );
        assertEquals( "3", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 6L, ( long ) cursor.get().getId() );
        assertEquals( "4", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 7L, ( long ) cursor.get().getId() );
        assertEquals( "5", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 8L, ( long ) cursor.get().getId() );
        assertEquals( "6", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test afterLast() ----------

        cursor = new GreaterEqCursor( store, evaluator );

        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 8L, ( long ) cursor.get().getId() );
        assertEquals( "6", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 7L, ( long ) cursor.get().getId() );
        assertEquals( "5", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 6L, ( long ) cursor.get().getId() );
        assertEquals( "4", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 5L, ( long ) cursor.get().getId() );
        assertEquals( "3", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test last() ----------

        cursor = new GreaterEqCursor( store, evaluator );

        cursor.last();

        assertTrue( cursor.available() );
        assertEquals( 8L, ( long ) cursor.get().getId() );
        assertEquals( "6", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 7L, ( long ) cursor.get().getId() );
        assertEquals( "5", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 6L, ( long ) cursor.get().getId() );
        assertEquals( "4", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 5L, ( long ) cursor.get().getId() );
        assertEquals( "3", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test before() ----------

        cursor = new GreaterEqCursor( store, evaluator );
        ForwardIndexEntry<String,ServerEntry> indexEntry = new ForwardIndexEntry<String,ServerEntry>();
        indexEntry.setValue( "5" );

        assertFalse( cursor.available() );
        cursor.before( indexEntry );
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 7L, ( long ) cursor.get().getId() );
        assertEquals( "5", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 8L, ( long ) cursor.get().getId() );
        assertEquals( "6", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        cursor = new GreaterEqCursor( store, evaluator );
        indexEntry = new ForwardIndexEntry<String,ServerEntry>();
        indexEntry.setValue( "7" );
        cursor.before( indexEntry );
        assertFalse( cursor.available() );
        assertTrue( cursor.previous() );
        assertEquals( 8L, ( long ) cursor.get().getId() );
        assertEquals( "6", cursor.get().getValue() );
        cursor.close();

        cursor = new GreaterEqCursor( store, evaluator );
        indexEntry = new ForwardIndexEntry<String,ServerEntry>();
        indexEntry.setValue( "3" );
        cursor.before( indexEntry );
        assertFalse( cursor.available() );
        assertTrue( cursor.next() );
        assertEquals( 5L, ( long ) cursor.get().getId() );
        assertEquals( "3", cursor.get().getValue() );
        cursor.close();

        // ---------- test after() ----------

        cursor = new GreaterEqCursor( store, evaluator );
        indexEntry = new ForwardIndexEntry<String,ServerEntry>();
        indexEntry.setValue( "4" );

        assertFalse( cursor.available() );
        cursor.after( indexEntry );
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 7L, ( long ) cursor.get().getId() );
        assertEquals( "5", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 8L, ( long ) cursor.get().getId() );
        assertEquals( "6", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        cursor = new GreaterEqCursor( store, evaluator );
        indexEntry = new ForwardIndexEntry<String,ServerEntry>();
        indexEntry.setValue( "7" );
        cursor.after( indexEntry );
        assertFalse( cursor.available() );
        assertTrue( cursor.previous() );
        assertEquals( 8L, ( long ) cursor.get().getId() );
        assertEquals( "6", cursor.get().getValue() );
        cursor.close();

        cursor = new GreaterEqCursor( store, evaluator );
        indexEntry = new ForwardIndexEntry<String,ServerEntry>();
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
        AttributeType at = attributeRegistry.lookup( SchemaConstants.POSTOFFICEBOX_AT_OID );
        GreaterEqNode node = new GreaterEqNode( SchemaConstants.POSTOFFICEBOX_AT_OID, new ServerStringValue( at, "3" ) );
        GreaterEqEvaluator evaluator = new GreaterEqEvaluator( node, store, registries );
        GreaterEqCursor cursor = new GreaterEqCursor( store, evaluator );
        assertNotNull( cursor );
        assertFalse( cursor.available() );
        assertTrue( cursor.isElementReused() );
        assertFalse( cursor.isClosed() );

        // ---------- test bad get() ----------

        try { cursor.get(); fail(); }
        catch( InvalidCursorPositionException e ) {}

        // ---------- test beforeFirst() ----------

        cursor.beforeFirst();
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 7L, ( long ) cursor.get().getId() );
        assertEquals( "5", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 8L, ( long ) cursor.get().getId() );
        assertEquals( "6", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 6L, ( long ) cursor.get().getId() );
        assertEquals( "4", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 5L, ( long ) cursor.get().getId() );
        assertEquals( "3", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        assertFalse( cursor.isClosed() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test first() ----------

        cursor = new GreaterEqCursor( store, evaluator );
        cursor.first();

        assertTrue( cursor.available() );
        assertEquals( 7L, ( long ) cursor.get().getId() );
        assertEquals( "5", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 8L, ( long ) cursor.get().getId() );
        assertEquals( "6", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 6L, ( long ) cursor.get().getId() );
        assertEquals( "4", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 5L, ( long ) cursor.get().getId() );
        assertEquals( "3", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        assertFalse( cursor.isClosed() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test afterLast() ----------

        cursor = new GreaterEqCursor( store, evaluator );
        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 5L, ( long ) cursor.get().getId() );
        assertEquals( "3", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 6L, ( long ) cursor.get().getId() );
        assertEquals( "4", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 8L, ( long ) cursor.get().getId() );
        assertEquals( "6", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 7L, ( long ) cursor.get().getId() );
        assertEquals( "5", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // ---------- test last() ----------

        cursor = new GreaterEqCursor( store, evaluator );
        cursor.last();

        assertTrue( cursor.available() );
        assertEquals( 5L, ( long ) cursor.get().getId() );
        assertEquals( "3", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 6L, ( long ) cursor.get().getId() );
        assertEquals( "4", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 8L, ( long ) cursor.get().getId() );
        assertEquals( "6", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 7L, ( long ) cursor.get().getId() );
        assertEquals( "5", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // ---------- test before() ----------

        cursor = new GreaterEqCursor( store, evaluator );
        ForwardIndexEntry<String,ServerEntry> indexEntry = new ForwardIndexEntry<String,ServerEntry>();
        indexEntry.setValue( "2" );
        try { cursor.before( indexEntry ); fail( "Should never get here." );}
        catch ( UnsupportedOperationException e ) {}

        // ---------- test after() ----------

        cursor = new GreaterEqCursor( store, evaluator );
        indexEntry = new ForwardIndexEntry<String,ServerEntry>();
        indexEntry.setValue( "2" );
        try { cursor.after( indexEntry ); fail( "Should never get here." );}
        catch ( UnsupportedOperationException e ) {}
    }


    // -----------------------------------------------------------------------
    // Evaluator Test Cases
    // -----------------------------------------------------------------------


    @Test
    public void testEvaluatorIndexed() throws Exception
    {
        AttributeType at = attributeRegistry.lookup( SchemaConstants.POSTALCODE_AT_OID );
        GreaterEqNode node = new GreaterEqNode( SchemaConstants.POSTALCODE_AT_OID, new ServerStringValue( at, "3" ) );
        GreaterEqEvaluator evaluator = new GreaterEqEvaluator( node, store, registries );
        ForwardIndexEntry<String,ServerEntry> indexEntry = new ForwardIndexEntry<String,ServerEntry>();
        assertEquals( node, evaluator.getExpression() );
        assertEquals( SchemaConstants.POSTALCODE_AT_OID, evaluator.getAttributeType().getOid() );
        assertNotNull( evaluator.getNormalizer() );
        assertNotNull( evaluator.getComparator() );

        indexEntry.setId( 1L );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String,ServerEntry>();
        indexEntry.setId( 4L );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String,ServerEntry>();
        indexEntry.setId( 5L );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String,ServerEntry>();
        indexEntry.setId( 6L );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String,ServerEntry>();
        indexEntry.setId( 7L );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String,ServerEntry>();
        indexEntry.setId( 8L );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String,ServerEntry>();
        indexEntry.setId( 9L );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String,ServerEntry>();
        indexEntry.setId( 10L );
        assertFalse( evaluator.evaluate( indexEntry ) );
    }


    @Test
    public void testEvaluatorWithDescendantValue() throws Exception
    {
        AttributeType at = attributeRegistry.lookup( SchemaConstants.STREET_AT_OID );
        GreaterEqNode node = new GreaterEqNode( SchemaConstants.STREET_AT_OID, new ServerStringValue( at, "2" ) );
        GreaterEqEvaluator evaluator = new GreaterEqEvaluator( node, store, registries );
        ForwardIndexEntry<String,ServerEntry> indexEntry = new ForwardIndexEntry<String,ServerEntry>();
        assertEquals( node, evaluator.getExpression() );
        assertEquals( SchemaConstants.STREET_AT_OID, evaluator.getAttributeType().getOid() );
        assertNotNull( evaluator.getNormalizer() );
        assertNotNull( evaluator.getComparator() );

        LdapDN dn = new LdapDN( "cn=jane doe,o=good times co." );
        dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
        ServerEntry attrs = new DefaultServerEntry( registries, dn );
        attrs.add( "objectClass", "person" );
        attrs.add( "c-street", "3" );
        attrs.add( "cn", "jane doe" );
        attrs.add( "sn", "doe" );
        attrs.add( "entryCSN", new CsnFactory( 1 ).newInstance().toString() );
        attrs.add( "entryUUID", SchemaUtils.uuidToBytes( UUID.randomUUID() ) );
        store.add(  attrs );

        indexEntry.setId( 12L );
        assertTrue( evaluator.evaluate( indexEntry ) );
    }


    @Test
    public void testEvaluatorWithoutDescendants() throws Exception
    {
        AttributeType at = attributeRegistry.lookup( SchemaConstants.C_POSTALCODE_AT_OID );
        GreaterEqNode node = new GreaterEqNode( SchemaConstants.C_POSTALCODE_AT_OID, new ServerStringValue( at, "2" ) );

        GreaterEqEvaluator evaluator = new GreaterEqEvaluator( node, store, registries );
        ForwardIndexEntry<String,ServerEntry> indexEntry = new ForwardIndexEntry<String,ServerEntry>();
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
        AttributeType at = attributeRegistry.lookup( SchemaConstants.POSTOFFICEBOX_AT_OID );
        GreaterEqNode node = new GreaterEqNode( SchemaConstants.POSTOFFICEBOX_AT_OID, new ServerStringValue( at, "3" ) );

        GreaterEqEvaluator evaluator = new GreaterEqEvaluator( node, store, registries );
        ForwardIndexEntry<String,ServerEntry> indexEntry = new ForwardIndexEntry<String, ServerEntry>();
        assertEquals( node, evaluator.getExpression() );
        assertEquals( SchemaConstants.POSTOFFICEBOX_AT_OID, evaluator.getAttributeType().getOid() );
        assertNotNull( evaluator.getNormalizer() );
        assertNotNull( evaluator.getComparator() );

        indexEntry.setId( 1L );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String,ServerEntry>();
        indexEntry.setId( 4L );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String,ServerEntry>();
        indexEntry.setId( 5L );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String,ServerEntry>();
        indexEntry.setId( 6L );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String,ServerEntry>();
        indexEntry.setId( 7L );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String,ServerEntry>();
        indexEntry.setId( 8L );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String,ServerEntry>();
        indexEntry.setId( 9L );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String,ServerEntry>();
        indexEntry.setId( 10L );
        assertFalse( evaluator.evaluate( indexEntry ) );
    }


    @Test ( expected = IllegalStateException.class )
    public void testEvaluatorAttributeNoMatchingRule() throws Exception
    {
        AttributeType at = new AttributeType( SchemaConstants.ATTRIBUTE_TYPES_AT_OID + ".2000" );
        at.addName( "bogus" );
        at.setSchemaName( "other" );
        at.setSyntax( new BogusSyntax( 1 ) );
        
        registries.getAttributeTypeRegistry().register( at );
        registries.getLdapSyntaxRegistry().register( at.getSyntax() );
        SyntaxCheckerDescription desc = new SyntaxCheckerDescription( at.getSyntax().getOid() );
        desc.setDescription( "bogus" );
        desc.setFqcn( BogusSyntax.class.getName() );
        List<String> names = new ArrayList<String>();
        names.add( "bogus" );
        desc.setNames( names );
        desc.setObsolete( false );
        registries.getSyntaxCheckerRegistry().register( at.getSyntax().getSyntaxChecker() );

        GreaterEqNode node = new GreaterEqNode( at.getOid(), new ServerStringValue( at, "3" ) );
        new GreaterEqEvaluator( node, store, registries );
        registries.getAttributeTypeRegistry().unregister( at.getOid() );
    }


    @Test
    public void testEvaluatorAttributeOrderingMatchingRule() throws Exception
    {
        MatchingRule mr = new MatchingRule( "1.1" );
        mr.setSyntax( new BogusSyntax( 1 ) );
        mr.setLdapComparator( new StringComparator() );
        mr.setNormalizer( new NoOpNormalizer( "1.1" ) );
        
        AttributeType at = new AttributeType( SchemaConstants.ATTRIBUTE_TYPES_AT_OID + ".5000" );
        at.addName( "bogus" );
        at.setSchemaName( "other" );
        at.setSyntax( new BogusSyntax( 2 ) );
        at.setOrdering( mr );
            
        registries.getAttributeTypeRegistry().register( at );
        registries.getLdapSyntaxRegistry().register( at.getSyntax() );
        SyntaxCheckerDescription desc = new SyntaxCheckerDescription( at.getSyntax().getOid() );
        desc.setDescription( "bogus" );
        desc.setFqcn( BogusSyntax.class.getName() );
        List<String> names = new ArrayList<String>();
        names.add( "bogus" );
        desc.setNames( names );
        desc.setObsolete( false );
        registries.getSyntaxCheckerRegistry().register( at.getSyntax().getSyntaxChecker() );

        GreaterEqNode node = new GreaterEqNode( at.getOid(), new ServerStringValue( at, "3" ) );
        new GreaterEqEvaluator( node, store, registries );
        registries.getAttributeTypeRegistry().unregister( at.getOid() );
    }
}