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
package org.apache.directory.server.config.beans;

import java.util.Set;



/**
 * A class used to store the DirectoryService configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DirectoryServiceBean
{
    /** The DS instance Id */
    private String instanceId;

    /** The directory instance replication ID */
    private int replicaId;

    /** The list of declared interceptors */
    private Set<InterceptorBean> interceptors;

    /**
     * Create a new DnsServerBean instance
     */
    public DirectoryServiceBean()
    {
    }


    /**
     * Sets the Instance ID for this DirectoryService
     * @param instanceId The instance ID
     */
    public void setInstanceId( String instanceId )
    {
        this.instanceId = instanceId;
    }


    /**
     * @return The DirectoryService instanceId
     */
    public String getInstanceId()
    {
        return instanceId;
    }


    /**
     * @return the replicaId
     */
    public int getReplicaId()
    {
        return replicaId;
    }


    /**
     * @param replicaId the replicaId to set
     */
    public void setReplicaId( int replicaId )
    {
        if ( ( replicaId < 0 ) || ( replicaId > 999 ) )
        {
            this.replicaId = 0;
        }
        else
        {
            this.replicaId = replicaId;
        }
    }


    /**
     * Returns interceptors in the server.
     *
     * @return the interceptors in the server.
     */
    public Set<InterceptorBean> getInterceptors()
    {
        return interceptors;
    }


    /**
     * Sets the interceptors in the server.
     *
     * @param interceptors the interceptors to be used in the server.
     */
    public void setInterceptors( Set<InterceptorBean> interceptors )
    {
        this.interceptors = interceptors;
    }
}
