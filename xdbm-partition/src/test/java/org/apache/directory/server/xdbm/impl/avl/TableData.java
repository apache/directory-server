/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.xdbm.impl.avl;


import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.xdbm.Table;


/**
 * Some data for a table.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class TableData
{
    /**
     * Used to inject data into a Table not supporting duplicates.
     *
     * @return
     */
    public static void injectNoDupsData( PartitionTxn txn, Table<Integer, Integer> table ) throws Exception
    {
        if ( table.isDupsEnabled() )
        {
            throw new IllegalArgumentException( "table must NOT support duplicates" );
        }

        table.put( txn, 0, 3 );
        table.put( txn, 1, 2 );
        table.put( txn, 2, 0 ); // overwritten
        table.put( txn, 2, 1 );
        table.put(txn,  2, 1 ); // has no effect
        table.put( txn, 3, 0 );
        table.put( txn, 23, 8934 );
    }


    public static void injectDupsData( PartitionTxn txn, Table<Integer, Integer> table ) throws Exception
    {
        if ( !table.isDupsEnabled() )
        {
            throw new IllegalArgumentException( "table MUST support duplicates" );
        }

        table.put( txn, 0, 3 ); // 1 
        table.put( txn, 1, 2 ); // 2 - 3 duplicates
        table.put( txn, 1, 4 ); // 3
        table.put( txn, 1, 6 ); // 4
        table.put( txn, 2, 1 ); // 5
        table.put( txn, 3, 0 ); // 6
        table.put( txn, 3, 0 ); // has no effect
        table.put( txn, 3, 8 ); // 7
        table.put( txn, 3, 9 ); // 8
        table.put( txn, 3, 10 ); // 9
        table.put( txn, 23, 8934 ); // 10
    }
}
