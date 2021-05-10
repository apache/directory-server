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

import javax.net.ssl.X509TrustManager;

import org.apache.directory.api.ldap.model.constants.LdapConstants;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.Network;
import org.apache.directory.ldap.client.api.NoVerificationTrustManager;


/**
 * A class for holding the syncrepl consumer's configuration. the following parameters
 * are part of the Syncrepl Consumer configuration :<br>
 * <ul>
 *   <li>remoteHost : the remote server's name, defaults to 'localhost'</li>
 *   <li>remotePort : the remote server's LDAP port, defaults to 10389</li>
 *   <li>replUserDn : The replication User's DN</li>
 *   <li>replUserPassword : The replication User's password</li>
 *   <li>refreshNPersist : the replication mode, defaults to 'true'</li>
 *   <li>refreshInterval : the interval between replications when in refreshOnly mode, defaults to 60s</li>
 *   <li>baseDn : the base from which to fetch entries on the remote server</li>
 *   <li>filter : the filter to select entries,defaults to (ObjectClass=*)</li>
 *   <li>attributes : the list of attributes to replicate, defaults to all</li>
 *   <li>searchSizeLimit : the maximum number of entries to fetch, defaults to no limit</li>
 *   <li>searchTimeout : the maximum delay to wait for entries, defaults to no limit</li>
 *   <li>searchScope : the scope, defaults to SUBTREE</li>
 *   <li>aliasDerefMode : set the aliss derefence policy, defaults to NEVER </li>
 *   <li>replicaId : the replica identifier</li>
 *   <li>configEntryDn : the configuration entry's DN</li>
 *   <li>chaseReferrals : tells if we chase referrals, defaults to false</li>
 *   <li>cookie : the replication cookie</li>
 *   <li>useTls : the connection uses TLS, defaults to true</li>
 *   <li>strictCertVerification : strictly verify the certificate, defaults to true</li>
 *   <li>trustManager : the trustManager to use, defaults to @link{ReplicationTrustManager}</li>
 *   <li></li>
 * </ul>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SyncReplConfiguration implements ReplicationConsumerConfig
{
    /** host name of the syncrepl remote server, default value is localhost */
    private String remoteHost;

    /** port number of the syncrepl provider server, default is 10389 */
    private int remotePort;

    /** The producer, as <host>:<port> */
    private String producer;

    /** replication user's Dn */
    private String replUserDn;

    /** password for binding with replication user dn */
    private byte[] replUserPassword;

    /** flag to represent refresh and persist or refresh only mode, defaults to true */
    private boolean refreshNPersist = true;

    /** time interval for successive sync requests, default is 60 seconds */
    private long refreshInterval = 60L * 1000L;

    /** the base Dn whose content will be searched for replicating */
    private String baseDn;

    /** the ldap filter for fetching the entries, default value is (objectClass=*) */
    private String filter = LdapConstants.OBJECT_CLASS_STAR;

    /** names of attributes to be replicated, default value is all user attributes */
    private Set<String> attributes;

    /** the maximum number of search results to be fetched
     * default value is 0 (i.e no limit) */
    private int searchSizeLimit = 0;

    /** the timeout value to be used while doing a search 
     * default value is 0 (i.e no limit)*/
    private int searchTimeout = 0;

    /** the search scope, default is sub tree level */
    private SearchScope searchScope = SearchScope.SUBTREE;

    /** alias dereferencing mode, default is set to 'never deref aliases' */
    private AliasDerefMode aliasDerefMode = AliasDerefMode.NEVER_DEREF_ALIASES;

    /** the cookie received from server */
    private byte[] cookie;

    /** the replica's id */
    private int replicaId;

    /** The configuration entry DN */
    private Dn configEntryDn = null;

    /** flag to indicate whether to chase referrals or not, default is false hence passes ManageDsaITControl with syncsearch request*/
    private boolean chaseReferrals = false;

    /** flag to indicate the use of TLS, default is true */
    private boolean useTls = true;

    /** flag to indicate the use of strict certificate verification, default is true */
    private boolean strictCertVerification = true;

    /** the X509 certificate trust manager used, default value set to {@link ReplicationTrustManager} */
    private X509TrustManager trustManager = ReplicationTrustManager.getInstance();

    /** flag to indicate if this node is part of a MMR setup, default value is true */
    private boolean mmrMode = true;


    /**
     * Creates a new instance of SyncreplConfiguration
     */
    public SyncReplConfiguration()
    {
        attributes = new HashSet<>();
        // the default list of attributes
        attributes.add( SchemaConstants.ALL_USER_ATTRIBUTES );
        
        remoteHost = Network.LOOPBACK_HOSTNAME;
        
        remotePort = 10389;
        
        producer = remoteHost + ":" + remotePort;
    }


    /**
     * @return the remote Host
     */
    public String getRemoteHost()
    {
        return remoteHost;
    }


    /**
     * @param remoteHost the remote Host to set
     */
    public void setRemoteHost( String remoteHost )
    {
        this.remoteHost = remoteHost;
        producer = remoteHost + ":" + remotePort;
    }


    /**
     * A convenient method that concatenates the host and port of the producer
     * @return The &lt;host&gt;:&lt;port&gt; the consumer is connected to
     */
    public String getProducer()
    {
        return producer;
    }


    /**
     * @return the port
     */
    public int getRemotePort()
    {
        return remotePort;
    }


    /**
     * @param remotePort the remote port to set
     */
    public void setRemotePort( int remotePort )
    {
        this.remotePort = remotePort;
        producer = remoteHost + ":" + remotePort;
    }


    /**
     * @return the replication user's Dn
     */
    public String getReplUserDn()
    {
        return replUserDn;
    }


    /**
     * @param replUserdDn the Dn of the replication user
     */
    public void setReplUserDn( String replUserdDn )
    {
        this.replUserDn = replUserdDn;
    }


    /**
     * @return the replication user's password
     */
    public byte[] getReplUserPassword()
    {
        return replUserPassword;
    }


    /**
     * @param replUserPassword the replication user's password
     */
    public void setReplUserPassword( byte[] replUserPassword )
    {
        this.replUserPassword = replUserPassword;
    }


    /**
     * @return the refreshPersist
     */
    public boolean isRefreshNPersist()
    {
        return refreshNPersist;
    }


    /**
     * @param refreshNPersist the falg indicating to run the consumer in refreshAndPersist mode
     */
    public void setRefreshNPersist( boolean refreshNPersist )
    {
        this.refreshNPersist = refreshNPersist;
    }


    /**
     * @return the refresh interval
     */
    public long getRefreshInterval()
    {
        return refreshInterval;
    }


    /**
     * @param refreshInterval the consumerInterval to set
     */
    public void setRefreshInterval( long refreshInterval )
    {
        if ( refreshInterval <= 0 )
        {
            throw new IllegalArgumentException( "refresh interval should be more than zero" );
        }
        this.refreshInterval = refreshInterval;
    }


    /**
     * @return the baseDn
     */
    public String getBaseDn()
    {
        return baseDn;
    }


    /**
     * @param baseDn the baseDn to set
     */
    public void setBaseDn( String baseDn )
    {
        this.baseDn = baseDn;
    }


    /**
     * @return the filter
     */
    public String getFilter()
    {
        return filter;
    }


    /**
     * @param filter the filter to set
     */
    public void setFilter( String filter )
    {
        this.filter = filter;
    }


    /**
     * @return the attributes
     */
    public String[] getAttributes()
    {
        return attributes.toArray( new String[]
            {} );
    }


    /**
     * @param attrs the attributes to set
     */
    public void setAttributes( String[] attrs )
    {
        attributes.clear();

        for ( String attr : attrs )
        {
            attributes.add( attr );
        }
    }


    /**
     * @return the searchSizeLimit
     */
    public int getSearchSizeLimit()
    {
        return searchSizeLimit;
    }


    /**
     * @param searchSizeLimit the searchSizeLimit to set
     */
    public void setSearchSizeLimit( int searchSizeLimit )
    {
        if ( searchSizeLimit < 0 )
        {
            throw new IllegalArgumentException( "search size limit value cannot be negative " + searchSizeLimit );
        }

        this.searchSizeLimit = searchSizeLimit;
    }


    /**
     * @return the searchTimeout
     */
    public int getSearchTimeout()
    {
        return searchTimeout;
    }


    /**
     * @param searchTimeout the searchTimeout to set
     */
    public void setSearchTimeout( int searchTimeout )
    {
        if ( searchTimeout < 0 )
        {
            throw new IllegalArgumentException( "search timeout value cannot be negative " + searchTimeout );
        }

        this.searchTimeout = searchTimeout;
    }


    /**
     * @return the searchScope
     */
    public SearchScope getSearchScope()
    {
        return searchScope;
    }


    /**
     * @param searchScope the searchScope to set
     */
    public void setSearchScope( SearchScope searchScope )
    {
        this.searchScope = searchScope;
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
        this.replicaId = replicaId;
    }


    /**
     * @return The ALiasDerefMode parameter
     */
    public AliasDerefMode getAliasDerefMode()
    {
        return aliasDerefMode;
    }


    /**
     * @param aliasDerefMode Should be either NEVER_DEREF_ALIASES or DEREF_FINDING_BASE_OBJ
     */
    public void setAliasDerefMode( AliasDerefMode aliasDerefMode )
    {
        if ( aliasDerefMode != AliasDerefMode.NEVER_DEREF_ALIASES
            && aliasDerefMode != AliasDerefMode.DEREF_FINDING_BASE_OBJ )
        {
            throw new IllegalArgumentException(
                "alias deref mode should only be set to either 'NEVER_DEREF_ALIASES' or 'DEREF_FINDING_BASE_OBJ'" );
        }

        this.aliasDerefMode = aliasDerefMode;
    }


    /**
     * @return The replication cookie
     */
    public byte[] getCookie()
    {
        return cookie;
    }


    /**
     * @param cookie The cookie to set
     */
    public void setCookie( byte[] cookie )
    {
        this.cookie = cookie;
    }


    /**
     * Tells if we chase referrals
     * @return true if we chase referals
     */
    public boolean isChaseReferrals()
    {
        return chaseReferrals;
    }


    /**
     * @param chaseReferrals Lust be false, always.
     */
    public void setChaseReferrals( boolean chaseReferrals )
    {
        if ( chaseReferrals )
        {
            throw new UnsupportedOperationException( "client-api currently doesn't support chasing referrals" );
        }

        this.chaseReferrals = chaseReferrals;
    }


    /**
     * @return The DN of the configuration entry
     */
    public Dn getConfigEntryDn()
    {
        return configEntryDn;
    }


    /**
     * @return true if we use TLS
     */
    public boolean isUseTls()
    {
        return useTls;
    }


    /**
     * set the option to turn on/off use of TLS
     * 
     * @param useTls If we have to use TLS
     */
    public void setUseTls( boolean useTls )
    {
        this.useTls = useTls;
    }


    /**
     * @return true if the certificate verification is enforced 
     */
    public boolean isStrictCertVerification()
    {
        return strictCertVerification;
    }


    /**
     * set the strict certificate verification
     * 
     * @param strictCertVerification If we require a certificate validation
     */
    public void setStrictCertVerification( boolean strictCertVerification )
    {
        if ( strictCertVerification )
        {
            trustManager = ReplicationTrustManager.getInstance();
        }
        else
        {
            trustManager = new NoVerificationTrustManager();
        }

        this.strictCertVerification = strictCertVerification;
    }


    /**
     * @return The Trustmanager instance
     */
    public X509TrustManager getTrustManager()
    {
        return trustManager;
    }


    /**
     * @param configEntryDn the configEntryDn to set
     */
    public void setConfigEntryDn( Dn configEntryDn )
    {
        this.configEntryDn = configEntryDn;
    }


    /**
     * @return true if this node is part of MMR setup
     */
    public boolean isMmrMode()
    {
        return mmrMode;
    }


    /**
     * enable/disable MMR option
     *
     * @param mmrMode The type of replication
     */
    public void setMmrMode( boolean mmrMode )
    {
        this.mmrMode = mmrMode;
    }


    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "[" );
        sb.append( "rid:" ).append( replicaId ).append( ", " );
        sb.append( "base:'" ).append( baseDn ).append( "', " );
        sb.append( "filter:" ).append( filter ).append( ", " );
        sb.append( "scope:" ).append( searchScope ).append( ", " );
        sb.append( "alias:" ).append( aliasDerefMode ).append( ", " );
        sb.append( "chase referrals:" ).append( chaseReferrals ).append( ", " );

        boolean isFirst = true;

        if ( attributes != null )
        {
            sb.append( "attributes:{" );

            for ( String attribute : attributes )
            {
                if ( isFirst )
                {
                    isFirst = false;
                }
                else
                {
                    sb.append( "/" );
                }

                sb.append( attribute );
            }

            sb.append( "}, " );
        }

        if ( refreshNPersist )
        {
            sb.append( "refresh:" ).append( refreshInterval ).append( ", " );
        }
        else
        {
            sb.append( "refreshOnly, " );
        }

        if ( mmrMode )
        {
            sb.append( "MMR, " );
        }
        else
        {
            sb.append( "MS, " );
        }

        sb.append( "provider:" ).append( producer ).append( ", " );
        sb.append( "user:'" ).append( replUserDn ).append( "', " );

        if ( strictCertVerification )
        {
            sb.append( "strict" ).append( ", " );
        }

        sb.append( "TLS:" ).append( useTls ).append( "]" );

        return sb.toString();
    }
}
