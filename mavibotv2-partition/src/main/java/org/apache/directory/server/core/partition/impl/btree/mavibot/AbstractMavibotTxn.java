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
package org.apache.directory.server.core.partition.impl.btree.mavibot;

import java.io.IOException;

import org.apache.directory.mavibot.btree.BTree;
import org.apache.directory.mavibot.btree.BTreeInfo;
import org.apache.directory.mavibot.btree.Page;
import org.apache.directory.mavibot.btree.RecordManager;
import org.apache.directory.mavibot.btree.RecordManagerHeader;
import org.apache.directory.mavibot.btree.Transaction;

/**
 * The Mavibot transaction abstract class
 *  
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
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
