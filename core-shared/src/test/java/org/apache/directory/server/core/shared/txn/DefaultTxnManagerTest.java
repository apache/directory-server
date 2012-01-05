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
package org.apache.directory.server.core.shared.txn;


import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.core.api.txn.TxnConflictException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


/**
 * A test for the TxnManager class
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DefaultTxnManagerTest
{
    /** Log buffer size : 4096 bytes */
    private int logBufferSize = 1 << 12;

    /** Log File Size : 8192 bytes */
    private long logFileSize = 1 << 13;

    /** log suffix */
    private static String LOG_SUFFIX = "log";

    /** Txn manager */
    private TxnManagerInternal txnManager;

    /** Creates a temporary folder for each test */
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();


    /**
     * Get the Log folder
     */
    private String getLogFolder() throws IOException
    {
        File newFolder = folder.newFolder( LOG_SUFFIX );
        String file = newFolder.getAbsolutePath();

        return file;
    }


    @Before
    public void setup()
    {
        try
        {
            TxnManagerFactory txnManagerFactory = new TxnManagerFactory( getLogFolder(), logBufferSize, logFileSize );
            txnManager = txnManagerFactory.txnManagerInternalInstance();
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            fail();
        }
    }


    @After
    public void teardown() throws IOException
    {
        FileUtils.deleteDirectory( new File( getLogFolder() ) );
    }


    @Test
    public void testBeginCommitReadOnlyTxn() throws Exception
    {
        try
        {
            txnManager.beginTransaction( true );

            Transaction transaction = txnManager.getCurTxn();

            assertTrue( transaction != null );
            assertTrue( transaction instanceof ReadOnlyTxn );

            txnManager.commitTransaction();
        }
        catch ( TxnConflictException e )
        {
            fail();
        }
        catch ( IOException e )
        {
            fail();
        }
    }


    @Test
    public void testBeginAbortReadOnlyTxn()
    {
        try
        {
            txnManager.beginTransaction( true );

            Transaction transaction = txnManager.getCurTxn();

            assertTrue( transaction != null );
            assertTrue( transaction instanceof ReadOnlyTxn );

            txnManager.abortTransaction();
        }
        catch ( Exception e )
        {
            fail();
        }
    }


    @Test
    public void testBeginCommitReadWriteTxn()
    {
        try
        {
            txnManager.beginTransaction( false );

            Transaction transaction = txnManager.getCurTxn();

            assertTrue( transaction != null );
            assertTrue( transaction instanceof ReadWriteTxn );

            txnManager.commitTransaction();
        }
        catch ( TxnConflictException e )
        {
            fail();
        }
        catch ( Exception e )
        {
            fail();
        }
    }


    @Test
    public void testBeginAbortReadWriteTxn()
    {
        try
        {
            txnManager.beginTransaction( false );

            Transaction transaction = txnManager.getCurTxn();

            assertTrue( transaction != null );
            assertTrue( transaction instanceof ReadWriteTxn );

            txnManager.abortTransaction();
        }
        catch ( Exception e )
        {
            fail();
        }
    }


    @Test
    public void testDependencyList()
    {
        List<ReadWriteTxn> dependentTxns;
        try
        {
            txnManager.beginTransaction( false );
            Transaction txn1 = txnManager.getCurTxn();
            txnManager.commitTransaction();

            txnManager.beginTransaction( false );
            Transaction txn2 = txnManager.getCurTxn();
            txnManager.commitTransaction();

            txnManager.beginTransaction( true );
            Transaction txn3 = txnManager.getCurTxn();

            dependentTxns = txn3.getTxnsToCheck();

            assertTrue( dependentTxns.contains( txn1 ) );
            assertTrue( dependentTxns.contains( txn2 ) );
            assertTrue( dependentTxns.contains( txn3 ) == false );

            txnManager.commitTransaction();

            txnManager.beginTransaction( false );
            Transaction txn4 = txnManager.getCurTxn();

            dependentTxns = txn4.getTxnsToCheck();

            assertTrue( dependentTxns.contains( txn1 ) );
            assertTrue( dependentTxns.contains( txn2 ) );
            assertTrue( dependentTxns.contains( txn3 ) == false );
            assertTrue( dependentTxns.contains( txn4 ) );

            txnManager.commitTransaction();
        }
        catch ( TxnConflictException e )
        {
            fail();
        }
        catch ( Exception e )
        {
            fail();
        }
    }
}
