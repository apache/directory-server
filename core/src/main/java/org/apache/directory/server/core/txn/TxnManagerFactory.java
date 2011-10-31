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


import java.util.Comparator;
import org.apache.directory.server.core.api.partition.index.Serializer;
import org.apache.directory.server.core.log.DefaultLog;
import org.apache.directory.server.core.log.Log;
import org.apache.directory.server.core.log.InvalidLogException;

import java.io.IOException;


/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class TxnManagerFactory
{
    /** The only txn manager */
    private static TxnManagerInternal<?> txnManager;

    /** The only txn log manager */
    private static TxnLogManager<?> txnLogManager;

    /** log suffix */
    private static String LOG_SUFFIX = "log";


    /**
     * 
     * Initializes the txn managemenet layer. It creates the only instances of txn manager and txn log manager. 
     *
     * @param idComparator comparator for the ID type.
     * @param idSerializer seriazlier for the ID type.
     * @param logFolderPath log folder path for the log manager.
     * @param logBufferSize in memory buffer size for the log manager.
     * @param logFileSize max targer log file size for the log manager.
     * @throws IOException thrown if initialization fails.
     */
    @SuppressWarnings("unchecked")
    public static <ID> void init( Comparator<ID> idComparator, Serializer idSerializer, String logFolderPath,
        int logBufferSize, int logFileSize ) throws IOException
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

        DefaultTxnManager<ID> dTxnManager;
        dTxnManager = new DefaultTxnManager<ID>();
        txnManager = dTxnManager;

        DefaultTxnLogManager<ID> dTxnLogManager;
        dTxnLogManager = new DefaultTxnLogManager<ID>();
        txnLogManager = dTxnLogManager;
        dTxnLogManager.init( log, ( TxnManagerInternal<ID> ) txnManager );

        dTxnManager.init( dTxnLogManager, idComparator, idSerializer );

    }


    @SuppressWarnings("unchecked")
    public static <ID> TxnManager<ID> txnManagerInstance()
    {
        return ( ( TxnManager<ID> ) txnManager );
    }


    @SuppressWarnings("unchecked")
    public static <ID> TxnLogManager<ID> txnLogManagerInstance()
    {
        return ( ( TxnLogManager<ID> ) txnLogManager );
    }


    @SuppressWarnings("unchecked")
    static <ID> TxnManagerInternal<ID> txnManagerInternalInstance()
    {
        return ( ( TxnManagerInternal<ID> ) txnManager );
    }
}
