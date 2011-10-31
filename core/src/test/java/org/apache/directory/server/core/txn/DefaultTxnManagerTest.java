
package org.apache.directory.server.core.txn;

import java.io.IOException;

import org.apache.directory.server.core.log.InvalidLogException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class DefaultTxnManagerTest
{
    /** Log buffer size : 4096 bytes */
    private int logBufferSize = 1 << 12;

    /** Log File Size : 8192 bytes */
    private long logFileSize = 1 << 13;

    /** log suffix */
    private static String LOG_SUFFIX = "log";

    /** Txn manager */
    private TxnManagerInternal<Long> txnManager;

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
    public void setup() throws IOException, InvalidLogException
    {
        TxnManagerFactory.<Long> init( LongComparator.INSTANCE, LongSerializer.INSTANCE, getLogFolder(), logBufferSize,
            logFileSize );
        txnManager = TxnManagerFactory.<Long> txnManagerInternalInstance();
    }


    @After
    public void teardown() throws IOException
    {
    }


    @Test
    public void testBeginCommitReadOnlyTxn()
    {
        try
        {
            txnManager.beginTransaction( true );

            assertTrue( txnManager.getCurTxn() != null );
            assertTrue( txnManager.getCurTxn() instanceof ReadOnlyTxn );

            txnManager.commitTransaction();
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            fail();
        }
        catch ( TxnConflictException e )
        {
            e.printStackTrace();
            fail();
        }
    }


    @Test
    public void testBeginAbortReadOnlyTxn()
    {
        try
        {
            txnManager.beginTransaction( true );

            assertTrue( txnManager.getCurTxn() != null );
            assertTrue( txnManager.getCurTxn() instanceof ReadOnlyTxn );

            txnManager.abortTransaction();
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            fail();
        }
    }


    @Test
    public void testBeginCommitReadWriteTxn()
    {
        try
        {
            txnManager.beginTransaction( false );

            assertTrue( txnManager.getCurTxn() != null );
            assertTrue( txnManager.getCurTxn() instanceof ReadWriteTxn );

            txnManager.commitTransaction();
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            fail();
        }
        catch ( TxnConflictException e )
        {
            e.printStackTrace();
            fail();
        }
    }


    @Test
    public void testBeginAbortReadWriteTxn()
    {
        try
        {
            txnManager.beginTransaction( false );

            assertTrue( txnManager.getCurTxn() != null );
            assertTrue( txnManager.getCurTxn() instanceof ReadWriteTxn );

            txnManager.abortTransaction();
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            fail();
        }
    }


    @Test
    public void testDependencyList()
    {
        List<ReadWriteTxn<Long>> dependentTxns;
        try
        {
            Transaction<Long> txn1 = null;
            txnManager.beginTransaction( false );
            txn1 = txnManager.getCurTxn();
            txnManager.commitTransaction();

            Transaction<Long> txn2 = null;
            txnManager.beginTransaction( false );
            txn2 = txnManager.getCurTxn();
            txnManager.commitTransaction();

            Transaction<Long> txn3 = null;
            txnManager.beginTransaction( true );
            txn3 = txnManager.getCurTxn();

            dependentTxns = txn3.getTxnsToCheck();
            assertTrue( dependentTxns.contains( txn1 ) );
            assertTrue( dependentTxns.contains( txn2 ) );
            assertTrue( dependentTxns.contains( txn3 ) == false );

            txnManager.commitTransaction();

            Transaction<Long> txn4 = null;
            txnManager.beginTransaction( false );
            txn4 = txnManager.getCurTxn();;
            dependentTxns = txn4.getTxnsToCheck();
            assertTrue( dependentTxns.contains( txn1 ) );
            assertTrue( dependentTxns.contains( txn2 ) );
            assertTrue( dependentTxns.contains( txn3 ) == false );
            assertTrue( dependentTxns.contains( txn4 ) );

            txnManager.commitTransaction();

        }
        catch ( IOException e )
        {
            e.printStackTrace();
            fail();
        }
        catch ( TxnConflictException e )
        {
            e.printStackTrace();
            fail();
        }
    }

}
