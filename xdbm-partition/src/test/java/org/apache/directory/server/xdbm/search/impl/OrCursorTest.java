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
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.FilterParser;
import org.apache.directory.api.ldap.model.filter.OrNode;
import org.apache.directory.api.ldap.model.filter.SubstringNode;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.util.Strings;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.MockCoreSession;
import org.apache.directory.server.core.api.MockDirectoryService;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.partition.impl.avl.AvlPartition;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.StoreUtils;
import org.apache.directory.server.xdbm.impl.avl.AvlIndex;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.server.xdbm.search.cursor.OrCursor;
import org.apache.directory.server.xdbm.search.cursor.SubstringCursor;
import org.apache.directory.server.xdbm.search.evaluator.SubstringEvaluator;
import org.apache.directory.shared.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
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
 */
public class OrCursorTest extends AbstractCursorTest
{
    private static final Logger LOG = LoggerFactory.getLogger( OrCursorTest.class.getSimpleName() );

    File wkdir;
    static SchemaManager schemaManager = null;


    @BeforeClass
    public static void setup() throws Exception
    {
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = OrCursorTest.class.getResource( "" ).getPath();
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
            fail( "Schema load failed : " + Exceptions.printErrors( schemaManager.getErrors() ) );
        }

        loaded = schemaManager.loadWithDeps( loader.getSchema( "collective" ) );

        if ( !loaded )
        {
            fail( "Schema load failed : " + Exceptions.printErrors( schemaManager.getErrors() ) );
        }
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
        ( ( Partition ) store ).setId( "example" );
        store.setCacheSize( 10 );
        store.setPartitionPath( wkdir.toURI() );
        store.setSyncOnWrite( false );

        store.addIndex( new AvlIndex( SchemaConstants.OU_AT_OID ) );
        store.addIndex( new AvlIndex( SchemaConstants.CN_AT_OID ) );
        ( ( Partition ) store ).setSuffixDn( new Dn( schemaManager, "o=Good Times Co." ) );
        ( ( Partition ) store ).initialize();

        StoreUtils.loadExampleData( store, schemaManager );

        evaluatorBuilder = new EvaluatorBuilder( store, schemaManager );
        cursorBuilder = new CursorBuilder( store, evaluatorBuilder );
        directoryService = new MockDirectoryService();
        directoryService.setSchemaManager( schemaManager );
        session = new MockCoreSession( new LdapPrincipal(), directoryService );

        LOG.debug( "Created new store" );
    }


    @After
    public void destroyStore() throws Exception
    {
        if ( store != null )
        {
            ( ( Partition ) store ).destroy();
        }

        store = null;
        if ( wkdir != null )
        {
            FileUtils.deleteDirectory( wkdir );
        }

        wkdir = null;
    }


    @Test
    public void testOrCursorUsingCursorBuilder() throws Exception
    {
        String filter = "(|(cn=J*)(sn=W*))";

        ExprNode exprNode = FilterParser.parse( schemaManager, filter );

        Cursor<Entry> cursor = buildCursor( exprNode );

        cursor.afterLast();

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        Entry entry = cursor.get();
        assertEquals( Strings.getUUID( 8 ), entry.get( "entryUUID" ).getString() );
        assertEquals( "Jack Daniels", entry.get( "cn" ).getString() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        entry = cursor.get();
        assertEquals( Strings.getUUID( 9 ), entry.get( "entryUUID" ).getString() );
        assertEquals( "Jim Bean", entry.get( "cn" ).getString() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        entry = cursor.get();
        assertEquals( Strings.getUUID( 10 ), entry.get( "entryUUID" ).getString() );
        assertEquals( "Jim Bean", entry.get( "cn" ).getString() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        entry = cursor.get();
        assertEquals( Strings.getUUID( 11 ), entry.get( "entryUUID" ).getString() );
        assertEquals( "Johnny Walker", entry.get( "cn" ).getString() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        entry = cursor.get();
        assertEquals( Strings.getUUID( 6 ), entry.get( "entryUUID" ).getString() );
        assertEquals( "JIM BEAN", entry.get( "cn" ).getString() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        entry = cursor.get();
        assertEquals( Strings.getUUID( 5 ), entry.get( "entryUUID" ).getString() );
        assertEquals( "JOhnny WAlkeR", entry.get( "cn" ).getString() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        cursor.beforeFirst();

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        entry = cursor.get();
        assertEquals( Strings.getUUID( 5 ), entry.get( "entryUUID" ).getString() );
        assertEquals( "JOhnny WAlkeR", entry.get( "cn" ).getString() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        entry = cursor.get();
        assertEquals( Strings.getUUID( 6 ), entry.get( "entryUUID" ).getString() );
        assertEquals( "JIM BEAN", entry.get( "cn" ).getString() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        entry = cursor.get();
        assertEquals( Strings.getUUID( 11 ), entry.get( "entryUUID" ).getString() );
        assertEquals( "Johnny Walker", entry.get( "cn" ).getString() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        entry = cursor.get();
        assertEquals( Strings.getUUID( 10 ), entry.get( "entryUUID" ).getString() );
        assertEquals( "Jim Bean", entry.get( "cn" ).getString() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        entry = cursor.get();
        assertEquals( Strings.getUUID( 9 ), entry.get( "entryUUID" ).getString() );
        assertEquals( "Jim Bean", entry.get( "cn" ).getString() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        entry = cursor.get();
        assertEquals( Strings.getUUID( 8 ), entry.get( "entryUUID" ).getString() );
        assertEquals( "Jack Daniels", entry.get( "cn" ).getString() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        cursor.close();
        assertTrue( cursor.isClosed() );
    }


    @Test(expected = InvalidCursorPositionException.class)
    @SuppressWarnings("unchecked")
    public void testOrCursor() throws Exception
    {
        List<Evaluator<? extends ExprNode>> evaluators = new ArrayList<Evaluator<? extends ExprNode>>();
        List<Cursor<IndexEntry<?, String>>> cursors = new ArrayList<Cursor<IndexEntry<?, String>>>();
        Evaluator<? extends ExprNode> eval;
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
        assertEquals( Strings.getUUID( 8 ), cursor.get().getId() );
        assertEquals( "jack daniels", cursor.get().getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 6 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 9 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 10 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 5 ), cursor.get().getId() );
        assertEquals( "walker", cursor.get().getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 11 ), cursor.get().getId() );
        assertEquals( "johnny walker", cursor.get().getKey() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // from last        
        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.last() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 11 ), cursor.get().getId() );
        assertEquals( "johnny walker", cursor.get().getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 5 ), cursor.get().getId() );
        assertEquals( "walker", cursor.get().getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 10 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 9 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 6 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 8 ), cursor.get().getId() );
        assertEquals( "jack daniels", cursor.get().getKey() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        try
        {
            cursor.after( new IndexEntry() );
            fail( "should fail with UnsupportedOperationException " );
        }
        catch ( UnsupportedOperationException uoe )
        {
        }

        try
        {
            cursor.before( new IndexEntry() );
            fail( "should fail with UnsupportedOperationException " );
        }
        catch ( UnsupportedOperationException uoe )
        {
        }

        try
        {
            cursor.get();
        }
        finally
        {
            cursor.close();
        }
    }
}
