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

package org.apache.directory.server.ldap.replication;


import org.apache.directory.shared.ldap.codec.util.LdapURLEncodingException;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.util.LdapURL;
import org.apache.directory.shared.ldap.util.StringTools;

/**
 * A configuration for a replica peer. We may have many replications relation
 * set for a server, each one of them being described with this structure. 
 *
 * @org.apache.xbean.XBean
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ReplicaPeerConfiguration
{
    /** A flag used when the replication use the RefreshOnly system */
    private boolean refreshOnly;
    
    /** The time to wait between two consecutive RefreshOnly replication*/
    private long interval;
    
    /** Default interval is 5 minutes */
    private static final long DEFAULT_INTERVAL = 1000L*5L*60L;
    
    /** The default host */
    private static final String DEFAULT_HOST = "localhost";
    
    /** The default ssl port */
    private static final int DEFAULT_PORT = 10389;
    
    /** The default port */
    private static final int DEFAULT_SSL_PORT = 10636;
    
    /** The producer we want to replicate */
    private LdapURL producer;
    
    /** The principal to use to connect to the producer */
    private DN principalDN;
    
    /** The principal's password */
    private String password;
    
    /** The producer's host */
    private String host;
    
    /** The producer's port */
    private int port;
    
    /** The base DN used for replication */
    private DN baseDN;
    
    /** A flag to tell the server to use an SSL connection */
    private boolean useSSL;

    
    /**
     * 
     * Creates a new instance of ConsumerConfiguration.
     *
     */
    public ReplicaPeerConfiguration()
    {
        interval = DEFAULT_INTERVAL;
    }
    
    /**
     * Set the type of replication wanted. If false, it will default
     * to RefreshAndPersist.
     * @param refreshOnly true if the refreshOnly replication is requested
     */
    public void setRefreshOnly( boolean refreshOnly )
    {
        this.refreshOnly = refreshOnly;
    }

    /**
     * @return the refreshOnly flag
     */
    public boolean isRefreshOnly()
    {
        return refreshOnly;
    }

    /**
     * Set the delay between two RefreshOnly replication. Its given in seconds.
     * @param interval the interval to set
     */
    public void setInterval( long interval )
    {
        // Convert to milliseconds
        this.interval = interval*1000L;
    }

    /**
     * @return the interval
     */
    public long getInterval()
    {
        return interval;
    }

    /**
     * @return the baseDN
     */
    public DN getBaseDN()
    {
        return baseDN;
    }

    /**
     * @param principalDN the principalDN to set
     */
    public void setPrincipalDN( String principalDN ) throws LdapInvalidDnException
    {
        this.principalDN = new DN( principalDN );
    }


    /**
     * @return the principalDN
     */
    public DN getPrincipalDN()
    {
        return principalDN;
    }

    
    /**
     * @param password the password to set
     */
    public void setPassword( String password )
    {
        this.password = password;
    }

    /**
     * @return the password
     */
    public String getPassword()
    {
        return password;
    }

    /**
     * @param producer the producer to set
     */
    public void setProducer( String producer ) throws LdapURLEncodingException
    {
        this.producer = new LdapURL( producer );
        
        // Update the other fields
        baseDN = this.producer.getDn();
        useSSL = "ldaps".equalsIgnoreCase( this.producer.getScheme() );
        host = this.producer.getHost();
        
        if ( StringTools.isEmpty( host ) )
        {
            host = DEFAULT_HOST;
        }
        
        port = this.producer.getPort();
        
        if ( port == -1 )
        {
            if ( useSSL )
            {
                port = DEFAULT_SSL_PORT;
            }
            else
            {
                port = DEFAULT_PORT;
            }
        }
    }

    
    /**
     * @return the producer
     */
    public LdapURL getProducer()
    {
        return producer;
    }


    /**
     * @return true if the connection with the producer is done using SSL
     */
    public boolean isUseSSL()
    {
        return useSSL;
    }


    /**
     * @return the producer's host
     */
    public String getHost()
    {
        return host;
    }


    /**
     * @return the producer's port
     */
    public int getPort()
    {
        return port;
    }
}
