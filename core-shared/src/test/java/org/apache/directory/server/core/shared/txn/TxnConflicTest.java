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
package org.apache.directory.server.core.txn;


import java.io.IOException;

import org.apache.directory.server.core.log.InvalidLogException;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class TxnConflicTest
{
    /** Log buffer size : 4096 bytes */
    private int logBufferSize = 1 << 12;

    /** Log File Size : 8192 bytes */
    private long logFileSize = 1 << 13;

    /** log suffix */
    private static String LOG_SUFFIX = "log";

    /** Txn manager */
    private TxnManagerInternal<Long> txnManager;

    /** Txn log manager */
    private TxnLogManager<Long> txnLogManager;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();


    /**
     * Get the Log folder
     */
    private String getLogFolder() throws IOException
    {
        String file = folder.newFolder( LOG_SUFFIX ).getAbsolutePath();

        return file;
    }


    @Before
    @SuppressWarnings("unchecked")
    public void setup() throws IOException, InvalidLogException
    {
        try
        {
            // Init the txn manager
            TxnManagerFactory.<Long> init( LongComparator.INSTANCE, LongSerializer.INSTANCE, getLogFolder(),
                logBufferSize, logFileSize );
            txnManager = TxnManagerFactory.<Long> txnManagerInternalInstance();
            txnLogManager = TxnManagerFactory.<Long> txnLogManagerInstance();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            fail();
        }
    }


    @Test
    public void testExclusiveChangeConflict()
    {
        boolean conflicted;

        try
        {
            Dn dn1 = new Dn( "cn=Test", "ou=department", "dc=example,dc=com" );
            Dn dn2 = new Dn( "gn=Test1", "cn=Test", "ou=department", "dc=example,dc=com" );

            ReadWriteTxn<Long> firstTxn;
            ReadWriteTxn<Long> checkedTxn;

            txnManager.beginTransaction( false );
            txnLogManager.addWrite( dn1, SearchScope.OBJECT );
            firstTxn = ( ReadWriteTxn<Long> ) txnManager.getCurTxn();
            txnManager.commitTransaction();

            txnManager.beginTransaction( false );
            txnLogManager.addWrite( dn1, SearchScope.OBJECT );
            checkedTxn = ( ReadWriteTxn<Long> ) txnManager.getCurTxn();

            conflicted = checkedTxn.hasConflict( firstTxn );
            assertTrue( conflicted == true );
            txnManager.commitTransaction();

            txnManager.beginTransaction( false );
            txnLogManager.addRead( dn1, SearchScope.OBJECT );
            checkedTxn = ( ReadWriteTxn<Long> ) txnManager.getCurTxn();

            conflicted = checkedTxn.hasConflict( firstTxn );
            assertTrue( conflicted == true );
            txnManager.commitTransaction();

            txnManager.beginTransaction( false );
            txnLogManager.addWrite( dn2, SearchScope.OBJECT );
            checkedTxn = ( ReadWriteTxn<Long> ) txnManager.getCurTxn();

            conflicted = checkedTxn.hasConflict( firstTxn );
            assertTrue( conflicted == false );
            txnManager.commitTransaction();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            fail();
        }
    }


    @Test
    public void testSubtreeChangeConflict()
    {
        boolean conflicted;

        try
        {
            Dn dn1 = new Dn( "cn=Test", "ou=department", "dc=example,dc=com" );
            Dn dn2 = new Dn( "gn=Test1", "cn=Test", "ou=department", "dc=example,dc=com" );
            Dn dn3 = new Dn( "ou=department", "dc=example,dc=com" );

            ReadWriteTxn<Long> firstTxn;
            ReadWriteTxn<Long> checkedTxn;

            txnManager.beginTransaction( false );
            txnLogManager.addWrite( dn1, SearchScope.SUBTREE );
            firstTxn = ( ReadWriteTxn<Long> ) txnManager.getCurTxn();
            txnManager.commitTransaction();

            txnManager.beginTransaction( false );
            txnLogManager.addRead( dn1, SearchScope.OBJECT );
            checkedTxn = ( ReadWriteTxn<Long> ) txnManager.getCurTxn();

            conflicted = checkedTxn.hasConflict( firstTxn );
            assertTrue( conflicted == true );
            txnManager.commitTransaction();

            txnManager.beginTransaction( false );
            txnLogManager.addWrite( dn2, SearchScope.OBJECT );
            checkedTxn = ( ReadWriteTxn<Long> ) txnManager.getCurTxn();

            conflicted = checkedTxn.hasConflict( firstTxn );
            assertTrue( conflicted == true );
            txnManager.commitTransaction();

            txnManager.beginTransaction( false );
            txnLogManager.addRead( dn1, SearchScope.SUBTREE );
            checkedTxn = ( ReadWriteTxn<Long> ) txnManager.getCurTxn();

            conflicted = checkedTxn.hasConflict( firstTxn );
            assertTrue( conflicted == true );
            txnManager.commitTransaction();

            txnManager.beginTransaction( false );
            txnLogManager.addWrite( dn3, SearchScope.OBJECT );
            checkedTxn = ( ReadWriteTxn<Long> ) txnManager.getCurTxn();

            conflicted = checkedTxn.hasConflict( firstTxn );
            assertTrue( conflicted == false );
            txnManager.commitTransaction();
        }
        catch ( Exception e )
        {

        }
    }
}
