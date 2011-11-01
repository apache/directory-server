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

import java.io.IOException;
import org.apache.directory.server.core.api.partition.index.Serializer;
import java.util.Comparator;

/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface TxnManager<ID>
{
    /**
     * Starts a new txn and associates it with the current thread.
     *
     * @param readOnly whether the txn is read only
     * @throws IOException
     */
    void beginTransaction( boolean readOnly ) throws IOException;
   
    /**
     * Tries to commit the current txn associated with the current thread. ReadWrite txns have to be verified against txns
     * that committed after they started for any conflicting change and conflicting
     * exception is thrown if verificatin fails.
     *
     * @throws IOException
     * @throws TxnConflictException
     */
    void commitTransaction() throws IOException, TxnConflictException;
    
    /**
     * Aborts the current txn associated with the current thread.
     *
     * @throws IOException
     */
    void abortTransaction() throws IOException;
    
    /**
     * Returns the id comparator used by the txn manager. 
     *
     * @return id comparator
     */
    Comparator<ID> getIDComparator();
    
    
    /**
     * Returns the id serializer used by the txn manager.
     *
     * @return id serializer 
     */
    Serializer getIDSerializer();
}
