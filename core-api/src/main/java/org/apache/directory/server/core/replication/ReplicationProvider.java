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

import org.apache.directory.shared.ldap.exception.LdapURLEncodingException;
import org.apache.directory.shared.ldap.filter.LdapURL;

/**
 * The replication provider data structure.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ReplicationProvider
{
    /** The provider unique identifier */
    private int id;
    
    /** The type of replication (refreshOnly or refreshAndPersist */
    private ReplicationType type = ReplicationType.REFRESH_AND_PERSIST;
    
    /** The sizeLimit for the searchRequest. Default to unlimited. */
    private long sizeLimit = 0;
    
    /** The timeLimit for the search request. Default to unlimited. */
    private int timeLimit = 0;
    
    /** The search operation to conduct */
    private LdapURL url;
    
    /** The connection to the replica */
    private ReplicaConnection connection;
    
    
    /**
     * @return the connection
     */
    public ReplicaConnection getConnection()
    {
        return connection;
    }


    /**
     * @param connection the connection to set
     */
    public void setConnection( ReplicaConnection connection )
    {
        this.connection = connection;
    }


    public ReplicationProvider()
    {
        type = ReplicationType.REFRESH_AND_PERSIST;
    }
    
    
    /**
     * @return the providerId
     */
    public int getId()
    {
        return id;
    }


    /**
     * @param id the provider Id to set
     */
    public void setId( int id )
    {
        this.id = id;
    }

    
    /**
     * @return the type
     */
    public ReplicationType getReplicationType()
    {
        return type;
    }


    /**
     * @param type the replication type to set
     */
    public void setType( String type )
    {
        this.type = ReplicationType.getInstance( type );
    }


    /**
     * @return the sizeLimit
     */
    public long getSizeLimit()
    {
        return sizeLimit;
    }


    /**
     * @param sizeLimit the sizeLimit to set
     */
    public void setSizeLimit( long sizeLimit )
    {
        this.sizeLimit = sizeLimit;
    }


    /**
     * @return the timeLimit
     */
    public int getTimeLimit()
    {
        return timeLimit;
    }


    /**
     * @param timeLimit the timeLimit to set
     */
    public void setTimeLimit( int timeLimit )
    {
        this.timeLimit = timeLimit;
    }
    
    
    /**
     * @return the url
     */
    public LdapURL getUrl()
    {
        return url;
    }
    
    
    /**
     * 
     * @param url The URL to use for the replication search request
     */
    public void setUrl( String url )
    {
        try
        {
            this.url = new LdapURL( url );
        }
        catch ( LdapURLEncodingException luee )
        {
            this.url = null;
        }
    }
}
