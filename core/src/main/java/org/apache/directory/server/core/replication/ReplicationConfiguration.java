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
package org.apache.directory.server.core.replication;

import java.util.List;

/**
 * The replication configuration.
 * 
 * @org.apache.xbean.XBean
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: $, $Date: $
 *
 */
public class ReplicationConfiguration
{
    /** The list of replication providers */
    private List<ReplicationProvider> providers;
    
    /**
     * Default constructor
     */
    public ReplicationConfiguration()
    {
        // Nothing
    }


    /**
     * Sets the providers in the server.
     *
     * @param providers the providers to be used in the server.
     */
    public void setProviders( List<ReplicationProvider> providers )
    {
        this.providers = providers;
    }
    
    
    /**
     * @return The list of replication providers
     */
    public List<ReplicationProvider> getproviders()
    {
        return providers;
    }
}
