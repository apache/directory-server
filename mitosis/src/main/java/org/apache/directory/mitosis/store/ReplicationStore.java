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
package org.apache.directory.mitosis.store;

import java.util.Set;

import javax.naming.Name;

import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.mitosis.common.CSN;
import org.apache.directory.mitosis.common.CSNVector;
import org.apache.directory.mitosis.common.ReplicaId;
import org.apache.directory.mitosis.common.UUID;
import org.apache.directory.mitosis.configuration.ReplicationConfiguration;
import org.apache.directory.mitosis.operation.Operation;

public interface ReplicationStore {

    void open( DirectoryServiceConfiguration serviceCfg, ReplicationConfiguration cfg );
    
    void close();
    
    ReplicaId getReplicaId();
    
    Set getKnownReplicaIds();
    
    // UUID to DN table operations
    
    Name getDN( UUID uuid );
    
    boolean putUUID( UUID uuid, Name dn );
    
    boolean removeUUID( UUID uuid );
    
    // Log entry operations
    
    void putLog( Operation operation );
    
    ReplicationLogIterator getLogs( CSN fromCSN, boolean inclusive );

    ReplicationLogIterator getLogs( CSNVector updateVector, boolean inclusive );

    int removeLogs( CSN toCSN, boolean inclusive );
    
    int getLogSize();
    
    int getLogSize( ReplicaId replicaId );
    
    CSNVector getUpdateVector();
    
    CSNVector getPurgeVector();
}
