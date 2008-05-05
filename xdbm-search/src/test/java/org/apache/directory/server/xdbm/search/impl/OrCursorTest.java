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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.naming.directory.Attributes;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.server.core.cursor.InvalidCursorPositionException;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmStore;
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
import org.apache.directory.server.xdbm.tools.StoreUtils;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.FilterParser;
import org.apache.directory.shared.ldap.filter.OrNode;
import org.apache.directory.shared.ldap.filter.SubstringNode;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Test class for OrCursor.
 * 
 * Note: The results of OrCursor need not be symmetric, in the sense that the results obtained<br>
 * in a particular order (say first to last ) need not be same as the results obtained in the<br> 
 * order last to first, cause each underlying cursor of OrCursor will have elements in their own order.<br>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class OrCursorTest
{
    private static final Logger LOG = LoggerFactory.getLogger( OrCursorTest.class.getSimpleName() );

    File wkdir;
    Store<Attributes> store;
    Registries registries = null;
    AttributeTypeRegistry attributeRegistry;
    EvaluatorBuilder evaluatorBuilder;
    CursorBuilder cursorBuilder;

    static Cursor<IndexEntry<?,Attributes>> storedCursor;
    
    public OrCursorTest() throws Exception
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
        store = new JdbmStore<Attributes>();
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
    @SuppressWarnings( "unchecked" )
    public void testOrCursorUsingCursorBuilder() throws Exception
    {
        String filter = "(|(cn=J*)(sn=W*))";

        ExprNode exprNode = FilterParser.parse( filter );
        
        Cursor<IndexEntry<?,Attributes>> cursor = ( Cursor<IndexEntry<?,Attributes>> ) cursorBuilder.build( exprNode );

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
    
    
    @Test( expected = InvalidCursorPositionException.class )
    @SuppressWarnings( "unchecked" )
    public void testOrCursor() throws Exception
    {
        List<Evaluator<? extends ExprNode,Attributes>> evaluators = new ArrayList<Evaluator<? extends ExprNode,Attributes>>();
        List<Cursor<IndexEntry<?,Attributes>>> cursors = new ArrayList<Cursor<IndexEntry<?,Attributes>>>();
        Evaluator<? extends ExprNode, Attributes> eval;
        Cursor<IndexEntry<?,Attributes>> cursor;
        
        OrNode orNode = new OrNode();

        ExprNode exprNode = new SubstringNode( "cn", "J", null );
        eval = new SubstringEvaluator( ( SubstringNode ) exprNode, store, registries );
        Cursor subStrCursor1 = new SubstringCursor( store, ( SubstringEvaluator ) eval );
        cursors.add( subStrCursor1 );
        evaluators.add( eval );  
        orNode.addNode( exprNode );
        
        try
        {
            cursor = ( Cursor<IndexEntry<?,Attributes>> ) new OrCursor( cursors, evaluators );
            fail("should throw IllegalArgumentException");
        }
        catch( IllegalArgumentException ie ){ }
        
        exprNode = new SubstringNode( "sn", "W", null );
        eval = new SubstringEvaluator( ( SubstringNode ) exprNode, store, registries );
        evaluators.add( eval );
        Cursor subStrCursor2 = new SubstringCursor( store, ( SubstringEvaluator ) eval );
        cursors.add( subStrCursor2 );
        
        orNode.addNode( exprNode );
        
        cursor = ( Cursor<IndexEntry<?,Attributes>> ) new OrCursor( cursors, evaluators );
        storedCursor = cursor;
        
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
        
        assertTrue( cursor.isElementReused() );
        
        cursor.get();
    }
    
    
    @Test( expected = UnsupportedOperationException.class )
    public void testAfter() throws Exception
    {
        storedCursor.after( new ForwardIndexEntry() );
    }
    
    
    @Test( expected = UnsupportedOperationException.class )
    public void testBefore() throws Exception
    {
        storedCursor.before( new ForwardIndexEntry() );
    }

}
