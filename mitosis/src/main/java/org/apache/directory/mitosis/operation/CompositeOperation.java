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
package org.apache.directory.mitosis.operation;


import org.apache.directory.mitosis.common.CSN;
import org.apache.directory.mitosis.common.CSNVector;
import org.apache.directory.mitosis.configuration.ReplicationConfiguration;
import org.apache.directory.mitosis.store.ReplicationLogIterator;
import org.apache.directory.mitosis.store.ReplicationStore;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.schema.registries.Registries;

import javax.naming.Name;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;


/**
 * An {@link Operation} that contains other {@link Operation}s.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class CompositeOperation extends Operation
{
    /**
     * Declares the Serial Version Uid.
     *
     * @see <a
     *      href="http://c2.com/cgi/wiki?AlwaysDeclareSerialVersionUid">Always
     *      Declare Serial Version Uid</a>
     */
    private static final long serialVersionUID = 6252675003841951356L;

    
    /**
     * A dummy replication store, used 
     */
    private static final ReplicationStore DUMMY_STORE = new ReplicationStore()
    {

        public void open( DirectoryService directoryService, ReplicationConfiguration cfg )
        {
        }


        public void close()
        {
        }


        public String getReplicaId()
        {
            return null;
        }


        public Set<String> getKnownReplicaIds()
        {
            return null;
        }


        public Name getDN( UUID uuid )
        {
            return null;
        }


        public boolean putUUID( UUID uuid, Name dn )
        {
            return false;
        }


        public boolean removeUUID( UUID uuid )
        {
            return false;
        }


        public void putLog( Operation operation )
        {
        }


        public ReplicationLogIterator getLogs( CSN fromCSN, boolean inclusive )
        {
            return null;
        }


        public ReplicationLogIterator getLogs( CSNVector updateVector, boolean inclusive )
        {
            return null;
        }


        public int removeLogs( CSN toCSN, boolean inclusive )
        {
            return 0;
        }


        public int getLogSize()
        {
            return 0;
        }


        public int getLogSize( String replicaId )
        {
            return 0;
        }


        public CSNVector getUpdateVector()
        {
            return null;
        }


        public CSNVector getPurgeVector()
        {
            return null;
        }
    };

    private final List<Operation> children = new ArrayList<Operation>();


    /**
     * Creates a new instance of CompositeOperation. This should not be called
     * outside of this package.
     *
     * @param registries The server registries
     */
    /* No qualifier */ CompositeOperation( Registries registries )
    {
        super( registries, OperationType.COMPOSITE_OPERATION );
    }


    /**
     * Creates a new instance of CompositeOperation.
     *
     * @param registries The server registries
     * @param csn the operation CSN
     */
    public CompositeOperation( Registries registries, CSN csn )
    {
        super( registries, OperationType.COMPOSITE_OPERATION, csn );
    }


    /**
     * Add a new operation to this composite operation
     *
     * @param op The added operation
     */
    public void add( Operation op )
    {
        assert op != null;
        assert op.getCSN().equals( csn );
        
        children.add( op );
    }


    /**
     * Remove all the inner operations.
     */
    public void clear()
    {
        children.clear();
    }


    /**
     * Apply the replication on each internal operation.
     * 
     * @param nexus the partition on which the modification will be done
     * @param store the replication store
     * @param coreSession the current session
     */
    protected void execute0( PartitionNexus nexus, ReplicationStore store, CoreSession coreSession ) 
        throws Exception
    {
        for ( Operation op : children )
        {
            op.execute( nexus, DUMMY_STORE, coreSession );
        }
    }
    
    
    /**
     * @return the number of included operations
     */
    public int size()
    {
        return children.size();
    }
    
    
    /**
     * @return The list of all included operations
     */
    public List<Operation> getChildren()
    {
        return children;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return children.toString();
    }
}
