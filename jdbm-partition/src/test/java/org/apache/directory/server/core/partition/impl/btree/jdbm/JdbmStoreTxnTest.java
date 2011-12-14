
package org.apache.directory.server.core.partition.impl.btree.jdbm;
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Random;
import java.util.UUID;

import javax.naming.directory.SearchControls;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.partition.OperationExecutionManager;
import org.apache.directory.server.core.api.partition.index.IndexCursor;
import org.apache.directory.server.core.api.txn.TxnManager;
import org.apache.directory.server.core.shared.partition.OperationExecutionManagerFactory;
import org.apache.directory.server.core.shared.txn.TxnManagerFactory;
import org.apache.directory.server.xdbm.XdbmStoreUtils;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.csn.CsnFactory;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.filter.PresenceNode;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
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

public class JdbmStoreTxnTest
{
    static File wkdir;
    static JdbmPartition store;
    private static SchemaManager schemaManager = null;
    private static LdifSchemaLoader loader;
    
    /** Operation execution manager */
    private static OperationExecutionManager executionManager;

    /** Txn manager */
    private static TxnManager txnManager;
    
    /** log dir */
    private static File logDir;
    
    /** txn and operation execution manager factories */
    private static TxnManagerFactory txnManagerFactory;
    private static OperationExecutionManagerFactory executionManagerFactory;

    @BeforeClass
    public static void setup() throws Exception
    {
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = JdbmStoreTest.class.getResource( "" ).getPath();
            int targetPos = path.indexOf( "target" );
            workingDirectory = path.substring( 0, targetPos + 6 );
        }

        
        logDir = new File( workingDirectory + File.separatorChar + "txnlog" + File.separatorChar );
        logDir.mkdirs();
        txnManagerFactory = new TxnManagerFactory( logDir.getPath(), 1 << 13, 1 << 14 );
        executionManagerFactory = new OperationExecutionManagerFactory( txnManagerFactory );
        executionManager = executionManagerFactory.instance();
        txnManager = txnManagerFactory.txnManagerInstance();

        File schemaRepository = new File( workingDirectory, "schema" );
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy( true );
        loader = new LdifSchemaLoader( schemaRepository );
        schemaManager = new DefaultSchemaManager( loader );

        boolean loaded = schemaManager.loadAllEnabled();

        if ( !loaded )
        {
            fail( "Schema load failed : " + Exceptions.printErrors(schemaManager.getErrors()) );
        }
    }


    @Before
    public void createStore() throws Exception
    {
        // setup the working directory for the store
        wkdir = File.createTempFile( getClass().getSimpleName(), "db" );
        wkdir.delete();
        wkdir = new File( wkdir.getParentFile(), getClass().getSimpleName() );
        
        // initialize the store
        store = new JdbmPartition( schemaManager, txnManagerFactory, executionManagerFactory );
        store.setId( "example" );
        store.setCacheSize( 10 );
        store.setPartitionPath( wkdir.toURI() );
        store.setSyncOnWrite( false );

        JdbmIndex ouIndex = new JdbmIndex( SchemaConstants.OU_AT_OID );
        ouIndex.setWkDirPath( wkdir.toURI() );
        store.addIndex( ouIndex );
        
        JdbmIndex uidIndex = new JdbmIndex( SchemaConstants.UID_AT_OID );
        uidIndex.setWkDirPath( wkdir.toURI() );
        store.addIndex( uidIndex );

        Dn suffixDn = new Dn( schemaManager, "o=Good Times Co." );
        store.setSuffixDn( suffixDn );

        store.initialize();

        XdbmStoreUtils.loadExampleData( store, schemaManager, executionManager );
    }


    @After
    public void destroyStore() throws Exception
    {
        if ( store != null )
        {
            // make sure all files are closed so that they can be deleted on Windows.
            store.destroy();
        }

        store = null;
        
        if ( logDir != null )
        {
            FileUtils.deleteDirectory( logDir);
        }

        if ( wkdir != null )
        {
            FileUtils.deleteDirectory( wkdir );
        }

        wkdir = null;
    }
    
    @Test
    public void testAddsConcurrentWithSearch()
    {
        try
        {
            int numThreads = 10;
            AddsConcurrentWithSearchTestThread threads[] = new AddsConcurrentWithSearchTestThread[numThreads];
            
            
            for ( int idx =0; idx < numThreads; idx++ )
            {
                threads[idx] = new AddsConcurrentWithSearchTestThread();
                threads[idx].start();
            }
            
            txnManager.beginTransaction( false );
            
            // dn id 12
            Dn martinDn = new Dn( schemaManager, "cn=Marting King,ou=Sales,o=Good Times Co." );
            DefaultEntry entry = new DefaultEntry( schemaManager, martinDn );
            entry.add( "objectClass", "top", "person", "organizationalPerson" );
            entry.add( "ou", "Sales" );
            entry.add( "cn", "Martin King" );
            entry.add( "entryCSN", new CsnFactory( 1 ).newInstance().toString() );
            entry.add( "entryUUID", Strings.getUUIDString( 12 ).toString() );

            AddOperationContext addContext = new AddOperationContext( null, entry );
            executionManager.add( store, addContext );
            
            // Sleep some
            Thread.sleep( 100 );
            
            // dn id 13
            Dn jimmyDn = new Dn( schemaManager, "cn=Jimmy Wales, ou=Sales,o=Good Times Co." );
            entry = new DefaultEntry( schemaManager, jimmyDn );
            entry.add( "objectClass", "top", "person", "organizationalPerson" );
            entry.add( "ou", "Marketing" );
            entry.add( "cn", "Jimmy Wales" );
            entry.add( "entryCSN", new CsnFactory( 1 ).newInstance().toString() );
            entry.add( "entryUUID", Strings.getUUIDString( 13 ).toString() );
            
            addContext = new AddOperationContext( null, entry );
            executionManager.add( store, addContext );
            
            txnManager.commitTransaction();
            
            for ( int idx =0; idx < numThreads; idx++ )
            {
                threads[idx].join();
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            assertTrue( false );
        }
    }
    
    
    class AddsConcurrentWithSearchTestThread extends Thread
    {
        private void doSearch() throws Exception
        {
            int numEntries = 0;
            
            SearchControls controls = new SearchControls();
            controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
            ExprNode filter = new PresenceNode( schemaManager.getAttributeType( SchemaConstants.OBJECT_CLASS_AT ) );
            
            Dn baseDn = new Dn( schemaManager, "ou=Sales,o=Good Times Co." );
            
            txnManager.beginTransaction( true );

            IndexCursor<UUID> cursor = store.getSearchEngine().cursor( baseDn, AliasDerefMode.NEVER_DEREF_ALIASES, filter, controls );
            
            while ( cursor.next() )
            {
                numEntries++;
            }
            
            assertTrue( numEntries == 2 || numEntries == 4 );
            //System.out.println("Num entries: " + numEntries );
            
            txnManager.commitTransaction();
        }


        public void run()
        {         
            try
            {
                Random sleepRandomizer = new Random();
                int sleepTime = sleepRandomizer.nextInt( 10 ) * 100;
                
                Thread.sleep( sleepTime );
                
                doSearch();
            }
            catch( Exception e )
            {
                e.printStackTrace();
                fail();
                assertTrue( false );
            }
            
            
            
        }
    } // end of class RemoveInsertTestThread

}
