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
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.xdbm.ForwardIndexEntry;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.StoreUtils;
import org.apache.directory.server.xdbm.impl.avl.AvlIndex;
import org.apache.directory.server.xdbm.impl.avl.AvlStore;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.filter.FilterParser;
import org.apache.directory.shared.ldap.model.filter.NotNode;
import org.apache.directory.shared.ldap.model.filter.SubstringNode;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.model.schema.syntaxCheckers.UuidSyntaxChecker;
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
 * Test cases for NotCursor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NotCursorTest
{
    private static final Logger LOG = LoggerFactory.getLogger( NotCursorTest.class.getSimpleName() );

    UuidSyntaxChecker uuidSynChecker = new UuidSyntaxChecker();

    File wkdir;
    Store<Entry, Long> store;
    static SchemaManager schemaManager = null;
    EvaluatorBuilder evaluatorBuilder;
    CursorBuilder cursorBuilder;


    @BeforeClass
    static public void setup() throws Exception
    {
        // setup the standard registries
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = NotCursorTest.class.getResource( "" ).getPath();
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

        loaded = schemaManager.loadWithDeps( loader.getSchema( "collective" ) );

        if ( !loaded )
        {
            fail( "Schema load failed : " + Exceptions.printErrors(schemaManager.getErrors()) );
        }
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
        store = new AvlStore<Entry>();
        store.setId( "example" );
        store.setCacheSize( 10 );
        store.setPartitionPath( wkdir.toURI() );
        store.setSyncOnWrite( false );

        store.addIndex( new AvlIndex( SchemaConstants.OU_AT_OID ) );
        store.addIndex( new AvlIndex( SchemaConstants.CN_AT_OID ) );
        StoreUtils.loadExampleData( store, schemaManager );

        evaluatorBuilder = new EvaluatorBuilder( store, schemaManager );
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
