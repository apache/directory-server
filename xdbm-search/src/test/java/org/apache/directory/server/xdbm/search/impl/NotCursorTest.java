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


import static org.junit.Assert.*;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.server.core.cursor.InvalidCursorPositionException;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmStore;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.schema.SerializableComparator;
import org.apache.directory.server.schema.bootstrap.ApacheSchema;
import org.apache.directory.server.schema.bootstrap.ApachemetaSchema;
import org.apache.directory.server.schema.bootstrap.BootstrapSchemaLoader;
import org.apache.directory.server.schema.bootstrap.CollectiveSchema;
import org.apache.directory.server.schema.bootstrap.CoreSchema;
import org.apache.directory.server.schema.bootstrap.Schema;
import org.apache.directory.server.schema.bootstrap.SystemSchema;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.server.schema.registries.DefaultOidRegistry;
import org.apache.directory.server.schema.registries.DefaultRegistries;
import org.apache.directory.server.schema.registries.OidRegistry;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.server.xdbm.ForwardIndexEntry;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.server.xdbm.tools.StoreUtils;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.FilterParser;
import org.apache.directory.shared.ldap.filter.NotNode;
import org.apache.directory.shared.ldap.filter.SubstringNode;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * Test cases for NotCursor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class NotCursorTest
{
    private static final Logger LOG = LoggerFactory.getLogger( NotCursorTest.class.getSimpleName() );

    File wkdir;
    Store<ServerEntry> store;
    Registries registries = null;
    AttributeTypeRegistry attributeRegistry;
    EvaluatorBuilder evaluatorBuilder;
    CursorBuilder cursorBuilder;

    public NotCursorTest() throws Exception
    {
        // setup the standard registries
        BootstrapSchemaLoader loader = new BootstrapSchemaLoader();
        OidRegistry oidRegistry = new DefaultOidRegistry();
        registries = new DefaultRegistries( "bootstrap", loader, oidRegistry );
        SerializableComparator.setRegistry( registries.getComparatorRegistry() );

        // load essential bootstrap schemas
        Set<Schema> bootstrapSchemas = new HashSet<Schema>();
        bootstrapSchemas.add( new ApachemetaSchema() );
        bootstrapSchemas.add( new ApacheSchema() );
        bootstrapSchemas.add( new CoreSchema() );
        bootstrapSchemas.add( new SystemSchema() );
        bootstrapSchemas.add( new CollectiveSchema() );
        loader.loadWithDependencies( bootstrapSchemas, registries );
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
        StoreUtils.loadExampleData( store, registries );
        
        evaluatorBuilder = new EvaluatorBuilder( store, registries );
        cursorBuilder = new CursorBuilder( store, evaluatorBuilder );
        
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
    public void testNotCursor() throws Exception
    {
        String filter = "(!(cn=J*))";

        ExprNode exprNode = FilterParser.parse( filter );
        
        Cursor<IndexEntry<?, ServerEntry>> cursor = cursorBuilder.build( exprNode );
        
        assertFalse( cursor.available() );
        
        cursor.beforeFirst();
        
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 1, ( long ) cursor.get().getId() );
        assertEquals( "2.5.4.10=good times co.", cursor.get().getValue() );
        
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 7, ( long ) cursor.get().getId() );
        assertEquals( "2.5.4.11=apache,2.5.4.11=board of directors,2.5.4.10=good times co.", cursor.get().getValue() );
        
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 3, ( long ) cursor.get().getId() );
        assertEquals( "2.5.4.11=board of directors,2.5.4.10=good times co.", cursor.get().getValue() );
        
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 4, ( long ) cursor.get().getId() );
        assertEquals( "2.5.4.11=engineering,2.5.4.10=good times co.", cursor.get().getValue() );
        
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 2, ( long ) cursor.get().getId() );
        assertEquals( "2.5.4.11=sales,2.5.4.10=good times co.", cursor.get().getValue() );
        
        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        
        cursor.close();
        assertTrue( cursor.isClosed() );
    }
    
    
    @Test
    public void testNotCursorWithManualFilter() throws Exception
    {
        NotNode notNode = new NotNode();
        
        ExprNode exprNode = new SubstringNode( "cn", "J", null );
        Evaluator<? extends ExprNode, ServerEntry> eval = new SubstringEvaluator( ( SubstringNode ) exprNode, store, registries );
        notNode.addNode( exprNode );
        
        Cursor<IndexEntry<?,ServerEntry>> cursor = new NotCursor( store, eval ); //cursorBuilder.build( andNode );
        
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 1, ( long ) cursor.get().getId() );
        assertEquals( "2.5.4.10=good times co.", cursor.get().getValue() );
        
        cursor.first();
        
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 7, ( long ) cursor.get().getId() );
        assertEquals( "2.5.4.11=apache,2.5.4.11=board of directors,2.5.4.10=good times co.", cursor.get().getValue() );
        
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 3, ( long ) cursor.get().getId() );
        assertEquals( "2.5.4.11=board of directors,2.5.4.10=good times co.", cursor.get().getValue() );
        
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 4, ( long ) cursor.get().getId() );
        assertEquals( "2.5.4.11=engineering,2.5.4.10=good times co.", cursor.get().getValue() );
        
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 2, ( long ) cursor.get().getId() );
        assertEquals( "2.5.4.11=sales,2.5.4.10=good times co.", cursor.get().getValue() );
        
        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        cursor.afterLast();
        
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 2, ( long ) cursor.get().getId() );
        assertEquals( "2.5.4.11=sales,2.5.4.10=good times co.", cursor.get().getValue() );
        
        cursor.last();
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 4, ( long ) cursor.get().getId() );
        assertEquals( "2.5.4.11=engineering,2.5.4.10=good times co.", cursor.get().getValue() );
        
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 3, ( long ) cursor.get().getId() );
        assertEquals( "2.5.4.11=board of directors,2.5.4.10=good times co.", cursor.get().getValue() );
        
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 7, ( long ) cursor.get().getId() );
        assertEquals( "2.5.4.11=apache,2.5.4.11=board of directors,2.5.4.10=good times co.", cursor.get().getValue() );
        
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 1, ( long ) cursor.get().getId() );
        assertEquals( "2.5.4.10=good times co.", cursor.get().getValue() );
        
        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        
        assertTrue( cursor.isElementReused() );
 
        try 
        {
            cursor.get();
            fail( "should fail with InvalidCursorPositionException" );
        }
        catch( InvalidCursorPositionException ice ) { }
        
        try
        {
            cursor.after( new ForwardIndexEntry() );
            fail( "should fail with UnsupportedOperationException " );
        }
        catch( UnsupportedOperationException uoe ) {}
        
        try
        {
            cursor.before( new ForwardIndexEntry() );
            fail( "should fail with UnsupportedOperationException " );
        }
        catch( UnsupportedOperationException uoe ) {}
    }

}
