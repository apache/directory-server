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

/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class TxnManagerFactory
{
    private static TxnManagerInternal<?> txnManager;
    
    private static TxnLogManager<?> txnLogManager;
    
    public static <ID> void init(Comparator<ID> idComparator, Serializer idSerializer)
    {
        DefaultTxnManager<ID> dTxnManager;
        dTxnManager = new DefaultTxnManager<ID>();
        txnManager = dTxnManager;
        
        DefaultTxnLogManager<ID> dTxnLogManager;
        dTxnLogManager = new DefaultTxnLogManager<ID>();
        txnLogManager = dTxnLogManager;
        
        // TODO init txn manager and log manager
        
        dTxnManager.init( dTxnLogManager, idComparator, idSerializer );
    }
    
    
    public static <ID> TxnManager<ID> txnManagerInstance()
    {
        return ( (TxnManager<ID>) txnManager );
    }
    
    
    public static <ID> TxnLogManager<ID> txnLogManagerInstance()
    {
        return ( (TxnLogManager<ID>) txnLogManager );
    }
    
    
    static <ID> TxnManagerInternal<ID> txnManagerInternalInstance()
    {
        return ( (TxnManagerInternal<ID>) txnManager );
    }
}
