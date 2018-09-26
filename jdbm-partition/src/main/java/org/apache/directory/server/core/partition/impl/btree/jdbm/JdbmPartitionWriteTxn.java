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
package org.apache.directory.server.core.partition.impl.btree.jdbm;

import java.io.IOException;

import org.apache.directory.server.core.api.partition.PartitionWriteTxn;

import jdbm.RecordManager;
import jdbm.recman.BaseRecordManager;
import jdbm.recman.CacheRecordManager;

/**
 * The JDBM partition write transaction
 *  
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class JdbmPartitionWriteTxn implements PartitionWriteTxn
{
    /** The associated record manager */
    private RecordManager recordManager;
    
    /** A flag used to flush data immediately or not */
    private boolean syncOnWrite = false;
    
    /**
     * Create an instance of JdbmPartitionWriteTxn
     * 
     * @param recordManager The RecordManager instance
     * @param syncOnWrite If we want to data to be flushed on each write
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
