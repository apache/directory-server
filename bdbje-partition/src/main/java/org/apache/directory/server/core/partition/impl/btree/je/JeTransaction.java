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

package org.apache.directory.server.core.partition.impl.btree.je;


import java.io.IOException;
import java.util.Stack;

import org.apache.directory.server.core.api.partition.PartitionReadTxn;
import org.apache.directory.server.core.api.partition.PartitionWriteTxn;

import com.sleepycat.je.Transaction;


/**
 * PartitionTxn implementation with encapsulated JE transaction handle.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class JeTransaction implements PartitionReadTxn, PartitionWriteTxn
{
    private Transaction txn;
    private boolean closed;

    // FIXME this txnStack and its usage MUST be commented out before releasing
    static Stack<StackTraceElement[]> txnStack = new Stack<>();


    public JeTransaction( Transaction txn )
    {
        this.txn = txn;
        txnStack.push( Thread.currentThread().getStackTrace() );
    }


    public Transaction getTxn()
    {
        return txn;
    }


    @Override
    public void commit() throws IOException
    {
        txn.commit();
        closed = true;
        txnStack.pop();
    }


    @Override
    public void abort() throws IOException
    {
        txn.abort();
        closed = true;
        txnStack.pop();
    }


    @Override
    public boolean isClosed()
    {
        return closed;
    }


    @Override
    public void close() throws IOException
    {
        commit();
    }
}
