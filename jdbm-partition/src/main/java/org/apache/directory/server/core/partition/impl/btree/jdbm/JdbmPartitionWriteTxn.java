package org.apache.directory.server.core.partition.impl.btree.jdbm;

import java.io.IOException;

import org.apache.directory.server.core.api.partition.PartitionWriteTxn;

import jdbm.RecordManager;
import jdbm.recman.BaseRecordManager;
import jdbm.recman.CacheRecordManager;

public class JdbmPartitionWriteTxn extends PartitionWriteTxn
{
    /** The associated record manager */
    private RecordManager recordManager;
    
    /** A flag used to flush data immediately or not */
    private boolean syncOnWrite = false;
    
    /**
     * 
     * @param recordManager
     * @param syncOnWrite
     */
    public JdbmPartitionWriteTxn( RecordManager recordManager, boolean syncOnWrite )
    {
        this.recordManager = recordManager;
        this.syncOnWrite = syncOnWrite;
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void commit() throws IOException
    {
        recordManager.commit();
        
        // And flush the journal
        BaseRecordManager baseRecordManager = null;

        if ( recordManager instanceof CacheRecordManager )
        {
            baseRecordManager = ( ( BaseRecordManager ) ( ( CacheRecordManager ) recordManager ).getRecordManager() );
        }
        else
        {
            baseRecordManager = ( ( BaseRecordManager ) recordManager );
        }


        if ( syncOnWrite )
        {
            baseRecordManager.getTransactionManager().synchronizeLog();
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void abort() throws IOException
    {
        recordManager.rollback();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClosed()
    {
        return false;
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException
    {
        commit();
    }
}
