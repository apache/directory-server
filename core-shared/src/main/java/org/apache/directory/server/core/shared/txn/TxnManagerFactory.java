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


import java.util.Comparator;
import org.apache.directory.server.core.api.partition.index.Serializer;
import org.apache.directory.server.core.api.txn.TxnLogManager;
import org.apache.directory.server.core.api.txn.TxnManager;
import org.apache.directory.server.core.shared.log.DefaultLog;
import org.apache.directory.server.core.api.log.Log;
import org.apache.directory.server.core.api.log.InvalidLogException;

import java.io.IOException;


/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class TxnManagerFactory
{
    /** The only txn manager */
    private TxnManagerInternal txnManager;

    /** The only txn log manager */
    private TxnLogManager txnLogManager;

    /** log suffix */
    private String LOG_SUFFIX = "log";
    
    private boolean inited;


    /**
     * 
     * Initializes the txn managemenet layer. It creates the only instances of txn manager and txn log manager. 
     *
     * @param logFolderPath log folder path for the log manager.
     * @param logBufferSize in memory buffer size for the log manager.
     * @param logFileSize max targer log file size for the log manager.
     * @throws IOException thrown if initialization fails.
     */
    public TxnManagerFactory( String logFolderPath,
        int logBufferSize, long logFileSize ) throws IOException
    {       
        Log log = new DefaultLog();

        try
        {
            log.init( logFolderPath, LOG_SUFFIX, logBufferSize, logFileSize );
        }
        catch ( InvalidLogException e )
        {
            throw new IOException( e );
        }

        txnManager = new DefaultTxnManager();

        txnLogManager = new DefaultTxnLogManager( log, this );

        ( ( DefaultTxnManager ) txnManager ).init( txnLogManager );
        
        inited = true;

    }
    
    public void uninit()
    {
        if ( inited == false )
        {
            return;
        }
        
        ( ( DefaultTxnManager ) txnManager ).uninit();
        ( (DefaultTxnLogManager) txnLogManager).uninit();
        inited = false;
    }
    
    
    public TxnManager txnManagerInstance()
    {
        return txnManager;
    }


    public TxnLogManager txnLogManagerInstance()
    {
        return txnLogManager;
    }


    TxnManagerInternal txnManagerInternalInstance()
    {
        return txnManager;
    }
}
