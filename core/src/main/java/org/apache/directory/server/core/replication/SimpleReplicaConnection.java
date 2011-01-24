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

import org.apache.directory.shared.ldap.model.name.Dn;


/**
 * A connection to a replica. This is an abstract class, extended by the 
 * SimpleReplicaConnection or the SaslReplicaConnection.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 *
 */
public abstract class SimpleReplicaConnection extends ReplicaConnection
{
    /** The Dn to use to bind to the remote server */
    private Dn principal;

    /** The password */
    private String credentials;

    /**
     * @return the principal
     */
    public Dn getPrincipal()
    {
        return principal;
    }

    /**
     * @param principal the principal to set
     */
    public void setPrincipal( Dn principal )
    {
        this.principal = principal;
    }

    /**
     * @return the credentials
     */
    public String getCredentials()
    {
        return credentials;
    }

    /**
     * @param credentials the credentials to set
     */
    public void setCredentials( String credentials )
    {
        this.credentials = credentials;
    }
}
