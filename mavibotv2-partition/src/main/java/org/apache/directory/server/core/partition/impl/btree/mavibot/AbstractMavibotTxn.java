package org.apache.directory.server.core.partition.impl.btree.mavibot;

import java.io.IOException;

import org.apache.directory.mavibot.btree.BTree;
import org.apache.directory.mavibot.btree.BTreeInfo;
import org.apache.directory.mavibot.btree.Page;
import org.apache.directory.mavibot.btree.RecordManager;
import org.apache.directory.mavibot.btree.RecordManagerHeader;
import org.apache.directory.mavibot.btree.Transaction;

public abstract class AbstractMavibotTxn implements MavibotTxn
{
    protected Transaction transaction;
    
    protected AbstractMavibotTxn( Transaction transaction )
    {
        this.transaction = transaction;
    }
    
    
    @Override
    public void commit() throws IOException
    {
        transaction.commit();
    }

    
    @Override
    public void abort() throws IOException
    {
        transaction.abort();
    }

    
    @Override
    public boolean isClosed()
    {
        return transaction.isClosed();
    }

    
    @Override
    public void close() throws IOException
    {
        transaction.close();
    }

    
    @Override
    public long getRevision()
    {
        return transaction.getRevision();
    }

    
    @Override
    public <K, V> Page<K, V> getPage( BTreeInfo<K, V> btreeInfo, long offset ) throws IOException
    {
        return transaction.getPage( btreeInfo, offset );
    }

    
    @Override
    public long getCreationDate()
    {
        return transaction.getCreationDate();
    }

    
    @Override
    public RecordManager getRecordManager()
    {
        return transaction.getRecordManager();
    }

    
    @Override
    public RecordManagerHeader getRecordManagerHeader()
    {
        return transaction.getRecordManagerHeader();
    }

    
    @Override
    public <K, V> BTree<K, V> getBTree( String name )
    {
        return transaction.getBTree( name );
    }

    
    public Transaction getTransaction()
    {
        return transaction;
    }
}
