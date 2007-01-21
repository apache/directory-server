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

import org.apache.directory.mitosis.common.CSN;
import org.apache.directory.mitosis.common.CSNVector;
import org.apache.directory.mitosis.common.Replica;
import org.apache.directory.mitosis.common.ReplicaId;
import org.apache.directory.mitosis.common.UUID;
import org.apache.directory.mitosis.configuration.ReplicationConfiguration;
import org.apache.directory.mitosis.operation.Operation;
import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.shared.ldap.name.LdapDN;

/**
 * Provides an abstract storage that stores data required to perform
 * replication, such as {@link UUID}-{@link LdapDN} mapping and
 * LDAP {@link Operation}s.  It also calculates the Update Vector (UV)
 * and the Purge Vector (PV) of a replica.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date#
 */
public interface ReplicationStore
{
    /**
     * Opens this storage.
     */
    void open( DirectoryServiceConfiguration serviceCfg, ReplicationConfiguration cfg );

    /**
     * Closes this storage and releases the resources allocated when it's
     * opened.
     */
    void close();

    /**
     * Returns the {@link ReplicaId} of the {@link Replica} that this storage
     * is associated with.
     */
    ReplicaId getReplicaId();

    /**
     * Returns the set of {@link ReplicaId}s of the {@link Replica}s that
     * belongs to the same cluster.
     */
    Set<ReplicaId> getKnownReplicaIds();


    // UUID to DN table operations

    /**
     * Finds the {@link Name} of an entry with the specified {@link UUID}.
     */
    Name getDN( UUID uuid );

    /**
     * Associates the specified name and UUID so a user can
     * find an entry's name from a UUID.
     */
    boolean putUUID( UUID uuid, Name dn );

    /**
     * Removed the specified UUID mapping from this storage.
     * @return <tt>true</tt> if and only if the mapping has been removed
     */
    boolean removeUUID( UUID uuid );


    // Log entry operations

    /**
     * Puts the specified operation into this storage.
     */
    void putLog( Operation operation );

    /**
     * Queries all operations that is greater than the specified {@link CSN}.
     * 
     * @param inclusive <tt>true</tt> if you want to include <tt>fromCSN</tt>
     *                  itself in the result set.
     */
    ReplicationLogIterator getLogs( CSN fromCSN, boolean inclusive );

    /**
     * Queries all operations that is greater than the specified
     * {@link CSNVector}.
     * 
     * @param inclusive <tt>true</tt> if you want to include
     *                  <tt>updateVector</tt> itself in the result set.
     */
    ReplicationLogIterator getLogs( CSNVector updateVector, boolean inclusive );

    /**
     * Removes all operations that is less than the specified {@link CSN}.
     * 
     * @param inclusive <tt>true</tt> if you want to delete the
     *                  <tt>toCSN</tt> itself, too.
     * @return the number of deleted {@link Operation}s
     */
    int removeLogs( CSN toCSN, boolean inclusive );

    /**
     * Returns the number of {@link Operation}s logged in this storage.
     */
    int getLogSize();

    /**
     * Returns the number of {@link Operation}s logged by
     * the {@link Replica} with the specified {@link ReplicaId}
     * in this storage .
     */
    int getLogSize( ReplicaId replicaId );

    /**
     * Calculates the Update Vector (UV) from this storage. 
     */
    CSNVector getUpdateVector();

    /**
     * Calculates the Purge Vector (PV) from this storage. 
     */
    CSNVector getPurgeVector();
}
