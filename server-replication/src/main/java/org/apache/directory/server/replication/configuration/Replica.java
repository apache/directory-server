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
package org.apache.directory.server.replication.configuration;


import java.net.InetSocketAddress;


/**
 * Document me!
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class Replica
{
    private ReplicaId id;
    private InetSocketAddress address;
    
    
    public void setId( ReplicaId id )
    {
        this.id = id;
    }
    
    
    public ReplicaId getId()
    {
        return id;
    }


    public void setAddress( InetSocketAddress address )
    {
        this.address = address;
    }


    public InetSocketAddress getAddress()
    {
        return address;
    }
}
