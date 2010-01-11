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
package org.apache.directory.server.ldap.replication;

import java.util.HashSet;
import java.util.Set;


/**
 * The replication sub-system.
 *
 * @org.apache.xbean.XBean
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ReplicationSystem
{
    /** The list of servers this server is connected to */
    private Set<ReplicaPeerConfiguration> replicaPeers;
    
    
    public ReplicationSystem()
    {
        replicaPeers = new HashSet<ReplicaPeerConfiguration>();
    }
    
    
    /**
     * Set the list of replica peers this server is connected to
     *
     * @param replicaPeers The list of replicas
     */
    public void setReplicaPeers( Set<ReplicaPeerConfiguration> replicaPeers )
    {
        this.replicaPeers = replicaPeers;
    }
    
    public Set<ReplicaPeerConfiguration> getReplicaPeers()
    {
        return replicaPeers;
    }
}
