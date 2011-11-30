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
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.partition.impl.avl.AvlPartition;
import org.apache.directory.server.core.shared.partition.OperationExecutionManagerFactory;
import org.apache.directory.server.core.shared.txn.TxnManagerFactory;
import org.apache.directory.server.core.api.partition.index.IndexCursor;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.XdbmStoreUtils;
import org.apache.directory.server.xdbm.impl.avl.AvlIndex;
import org.apache.directory.server.xdbm.search.Optimizer;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.filter.FilterParser;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.model.schema.normalizers.ConcreteNameComponentNormalizer;
import org.apache.directory.shared.ldap.model.schema.normalizers.NameComponentNormalizer;
import org.apache.directory.shared.ldap.model.schema.syntaxCheckers.UuidSyntaxChecker;
import org.apache.directory.shared.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.shared.util.Strings;
import org.apache.directory.shared.util.exception.Exceptions;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * Tests the cursor functionality with deeply nested filters.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NestedFilterTest
{
    private static final Logger LOG = LoggerFactory.getLogger( NestedFilterTest.class.getSimpleName() );

    File wkdir;
    Store store;
    static SchemaManager schemaManager = null;
    EvaluatorBuilder evaluatorBuilder;
    CursorBuilder cursorBuilder;
    Optimizer optimizer;
    static FilterNormalizingVisitor visitor;


    @BeforeClass
    static public void setup() throws Exception
    {
        // setup the standard registries
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = NestedFilterTest.class.getResource( "" ).getPath();
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

        NameComponentNormalizer ncn = new ConcreteNameComponentNormalizer( schemaManager );
        visitor = new FilterNormalizingVisitor( ncn, schemaManager );
    }


    @Before
    public void createStore() throws Exception
    {
        // setup the working directory for the store
        wkdir = File.createTempFile( getClass().getSimpleName(), "db" );
        wkdir.delete();
        wkdir = new File( wkdir.getParentFile(), getClass().getSimpleName() );
        wkdir.mkdirs();
        
        File logDir = new File( wkdir.getPath() + File.separatorChar + "txnlog" + File.separatorChar );
        logDir.mkdirs();
        TxnManagerFactory.init( logDir.getPath(), 1 << 13, 1 << 14 );
        OperationExecutionManagerFactory.init();

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

        XdbmStoreUtils.loadExampleData( ( Partition )store, schemaManager );

        evaluatorBuilder = new EvaluatorBuilder( ( Partition )store, schemaManager );
        cursorBuilder = new CursorBuilder( ( Partition )store, evaluatorBuilder );
        optimizer = new DefaultOptimizer( ( Partition )store );

        LOG.debug( "Created new store" );
    }


    @After
    public void destryStore() throws Exception
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
    public void testNestedAndnOr() throws Exception
    {
        String filter = "(|(&(cn=J*)(sn=w*))(ou=apache))";

        ExprNode exprNode = FilterParser.parse( schemaManager, filter );
        exprNode.accept( visitor );
        optimizer.annotate( exprNode );

        IndexCursor<?> cursor = cursorBuilder.build( exprNode );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 5 ), cursor.get().getId() );
        assertEquals( "walker", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 7 ), cursor.get().getId() );
        assertEquals( "apache", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 9 ), cursor.get().getId() );
        assertEquals( "apache", cursor.get().getValue() );

        assertFalse( cursor.next() );
    }


    @Test
    public void testNestedAndnNot() throws Exception
    {
        String filter = "(&(&(cn=Jo*)(sn=w*))(!(ou=apache)))";

        ExprNode exprNode = FilterParser.parse(schemaManager, filter);
        optimizer.annotate( exprNode );

        IndexCursor<?> cursor = cursorBuilder.build( exprNode );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 5 ), cursor.get().getId() );
        assertEquals( "walker", cursor.get().getValue() );

        assertFalse( cursor.next() );
    }


    @Test
    public void testNestedNotnOrnAnd() throws Exception
    {
        String filter = "(&(|(postalCode=5)(postalCode=6))(!(ou=sales)))";

        UuidSyntaxChecker uuidSynChecker = new UuidSyntaxChecker();

        ExprNode exprNode = FilterParser.parse( schemaManager, filter );
        optimizer.annotate( exprNode );

        IndexCursor<?> cursor = cursorBuilder.build( exprNode );

        Set<UUID> set = new HashSet<UUID>();
        while ( cursor.next() )
        {
            assertTrue( cursor.available() );
            set.add( cursor.get().getId() );
            assertTrue( uuidSynChecker.isValidSyntax( cursor.get().getValue() ) );
        }
        assertEquals( 2, set.size() );
        assertTrue( set.contains( Strings.getUUIDString( 7 ) ) );
        assertTrue( set.contains( Strings.getUUIDString( 8 ) ) );

        assertFalse( cursor.next() );
    }


    @Test
    public void testNestedOrnNot() throws Exception
    {
        String filter = "(!(|(|(cn=Jo*)(sn=w*))(!(ou=apache))))";

        ExprNode exprNode = FilterParser.parse( schemaManager, filter );
        optimizer.annotate( exprNode );

        IndexCursor<?> cursor = cursorBuilder.build( exprNode );
    }
}
