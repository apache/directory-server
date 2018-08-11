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
package org.apache.directory.server.core.api.partition;

import java.io.Closeable;
import java.io.IOException;

/**
 * The Transaction interface that partitions have to implement.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface PartitionTxn extends Closeable
{
    /**
     * Commit a write transaction. It will apply the changes on 
     * the database.Last, not least, a new version will be created.
     * If called by a Read transaction, it will simply close it.
     * 
     * @throws IOException If the commit failed
     */
    void commit() throws IOException;
    
    
    /**
     * Abort a transaction. If it's a {@link PartitionReadTxn}, it will unlink this transaction
     * from the version it used. If it's a {@link PartitionWriteTxn}; it will drop all the pending
     * changes. The latest version will remain the same.
     * 
     * @throws IOException If the abort failed
     */
    void abort() throws IOException;

    
    /**
     * Tells if the transaction has been committed/aborted or not.
     *  
     * @return <tt>true</tt> if the transaction has been completed.
     */
    boolean isClosed();
    
    
    /**
     * Commit the transaction. It's called when we get out of a try-with-resource.
     */
    void close() throws IOException;
}
