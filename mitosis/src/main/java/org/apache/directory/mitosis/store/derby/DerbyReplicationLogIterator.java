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
package org.apache.directory.mitosis.store.derby;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.directory.mitosis.common.CSN;
import org.apache.directory.mitosis.common.ReplicaId;
import org.apache.directory.mitosis.common.DefaultCSN;
import org.apache.directory.mitosis.operation.Operation;
import org.apache.directory.mitosis.operation.OperationCodec;
import org.apache.directory.mitosis.store.ReplicationLogIterator;
import org.apache.directory.mitosis.store.ReplicationStoreException;


class DerbyReplicationLogIterator implements ReplicationLogIterator
{
    private final OperationCodec codec;
    private final Connection con;
    private final Statement stmt;
    private final ResultSet rs;


    DerbyReplicationLogIterator( OperationCodec codec, Connection con, Statement stmt, ResultSet rs )
    {
        this.codec = codec;
        this.con = con;
        this.stmt = stmt;
        this.rs = rs;
    }


    public boolean next()
    {
        try
        {
            return rs.next();
        }
        catch ( SQLException e )
        {
            throw new ReplicationStoreException( e );
        }
    }


    public void close()
    {
        SQLUtil.cleanup( con, stmt, rs );
    }


    public CSN getCSN()
    {
        try
        {
            ReplicaId replicaId = new ReplicaId( rs.getString( 1 ) );
            long timestamp = rs.getLong( 2 );
            int operationSequence = rs.getInt( 3 );
            return new DefaultCSN( timestamp, replicaId, operationSequence );
        }
        catch ( Exception e )
        {
            throw new ReplicationStoreException( e );
        }
    }


    public Operation getOperation()
    {
        try
        {
            return codec.decode( rs.getBytes( 4 ) );
        }
        catch ( Exception e )
        {
            throw new ReplicationStoreException( e );
        }
    }
}
