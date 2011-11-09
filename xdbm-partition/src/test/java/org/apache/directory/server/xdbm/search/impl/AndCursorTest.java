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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.partition.impl.avl.AvlPartition;
import org.apache.directory.server.core.api.partition.index.ForwardIndexEntry;
import org.apache.directory.server.core.api.partition.index.IndexCursor;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.StoreUtils;
import org.apache.directory.server.xdbm.impl.avl.AvlIndex;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.filter.AndNode;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.filter.FilterParser;
import org.apache.directory.shared.ldap.model.filter.PresenceNode;
import org.apache.directory.shared.ldap.model.filter.SubstringNode;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.shared.util.exception.Exceptions;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * Test class for AndCursor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AndCursorTest
{
    private static final Logger LOG = LoggerFactory.getLogger( AndCursorTest.class.getSimpleName() );

    File wkdir;
    Store store;
    EvaluatorBuilder evaluatorBuilder;
    CursorBuilder cursorBuilder;
    private static SchemaManager schemaManager;


    @BeforeClass
    public static void setup() throws Exception
    {
        // setup the standard registries
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = AndCursorTest.class.getResource( "" ).getPath();
            int targetPos = path.indexOf( "target" );
            workingDirectory = path.substring( 0, targetPos + 6 );
        }

        File schemaRepository = new File( workingDirectory, "schema" );
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy( true );
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        schemaManager = new DefaultSchemaManager( loader );

        boolean loaded = schemaManager.loadAllEnabled();

        if ( !loaded )
        {
            fail( "Schema load failed : " + Exceptions.printErrors(schemaManager.getErrors()) );
        }

        loaded = schemaManager.loadWithDeps( "collective" );

        if ( !loaded )
        {
            fail( "Schema load failed : " + Exceptions.printErrors(schemaManager.getErrors()) );
        }
    }


    public AndCursorTest() throws Exception
    {
    }


    @Before
    public void createStore() throws Exception
    {
        // setup the working directory for the store
        wkdir = File.createTempFile( getClass().getSimpleName(), "db" );
        wkdir.delete();
        wkdir = new File( wkdir.getParentFile(), getClass().getSimpleName() );
        wkdir.mkdirs();

        // initialize the store
        store = new AvlPartition( schemaManager );
        ((Partition)store).setId( "example" );
        store.setCacheSize( 10 );
        store.setPartitionPath( wkdir.toURI() );
        store.setSyncOnWrite( false );

        store.addIndex( new AvlIndex( SchemaConstants.OU_AT_OID ) );
        store.addIndex( new AvlIndex( SchemaConstants.CN_AT_OID ) );
        ((Partition)store).setSuffixDn( new Dn( schemaManager, "o=Good Times Co." ) );
        ((Partition)store).initialize();

        ((Partition)store).initialize();
        
        StoreUtils.loadExampleData( store, schemaManager );

        evaluatorBuilder = new EvaluatorBuilder( store, schemaManager );
        cursorBuilder = new CursorBuilder( store, evaluatorBuilder );

        LOG.debug( "Created new store" );
    }


    @After
    public void destroyStore() throws Exception
    {
        if ( store != null )
        {
            ((Partition)store).destroy();
        }

        store = null;
        if ( wkdir != null )
        {
            FileUtils.deleteDirectory( wkdir );
        }

        wkdir = null;
    }


    @Test
    public void testAndCursorWithCursorBuilder() throws Exception
    {
        String filter = "(&(cn=J*)(sn=*))";

        ExprNode exprNode = FilterParser.parse(schemaManager, filter);

        IndexCursor<?> cursor = cursorBuilder.build( exprNode );

        cursor.beforeFirst();

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( StoreUtils.getUUIDString( 8 ), cursor.get().getId() );
        assertEquals( "jack daniels", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( StoreUtils.getUUIDString( 6 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( StoreUtils.getUUIDString( 5 ), cursor.get().getId() );
        assertEquals( "johnny walker", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        cursor.close();
        assertTrue( cursor.isClosed() );
    }


    @Test
    public void testAndCursorWithManualFilter() throws Exception
    {
        AndNode andNode = new AndNode();

        List<Evaluator<? extends ExprNode>> evaluators = new ArrayList<Evaluator<? extends ExprNode>>();
        Evaluator<? extends ExprNode> eval;

        ExprNode exprNode = new SubstringNode( schemaManager.getAttributeType( "cn" ), "J", null );
        eval = new SubstringEvaluator( (SubstringNode) exprNode, store, schemaManager );
        IndexCursor<?> wrapped = new SubstringCursor( store, ( SubstringEvaluator ) eval );

        /* adding this results in NPE  adding Presence evaluator not 
         Substring evaluator but adding Substring cursor as wrapped cursor */
        // evaluators.add( eval ); 

        andNode.addNode( exprNode );

        exprNode = new PresenceNode( schemaManager.getAttributeType( "sn" ) );
        eval = new PresenceEvaluator( (PresenceNode) exprNode, store, schemaManager );
        evaluators.add( eval );

        andNode.addNode( exprNode );

        IndexCursor<?> cursor = new AndCursor( wrapped, evaluators ); //cursorBuilder.build( andNode );

        cursor.beforeFirst();

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( StoreUtils.getUUIDString( 8 ), cursor.get().getId() );
        assertEquals( "jack daniels", cursor.get().getValue() );

        cursor.first();

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( StoreUtils.getUUIDString( 6 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( StoreUtils.getUUIDString( 5 ), cursor.get().getId() );
        assertEquals( "johnny walker", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        cursor.afterLast();

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( StoreUtils.getUUIDString( 5 ), cursor.get().getId() );
        assertEquals( "johnny walker", cursor.get().getValue() );

        cursor.last();

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( StoreUtils.getUUIDString( 6 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( StoreUtils.getUUIDString( 8 ), cursor.get().getId() );
        assertEquals( "jack daniels", cursor.get().getValue() );

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

    }

}
