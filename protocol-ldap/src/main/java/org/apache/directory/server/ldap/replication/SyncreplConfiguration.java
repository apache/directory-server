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

import org.apache.directory.ldap.client.api.NoVerificationTrustManager;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.util.Strings;

import javax.net.ssl.X509TrustManager;


/**
 * 
 * A class for holding the syncrepl consumer's configuration.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SyncreplConfiguration
{
    /** host name of the syncrepl provider server, default value is localhost */
    private String providerHost = "localhost";

    /** port number of the syncrepl provider server, default is 389 */
    private int port = 389;

    /** replication user's Dn */
    private String replUserDn;

    /** password for binding with replication user dn */
    private byte[] replUserPassword;

    /** flag to represent refresh and persist or refresh only mode, defaults to true */
    private boolean refreshNPersist = true;

    /** time interval for successive sync requests, default is 60 seconds */
    private long refreshInterval = 60 * 1000;

    /** the base Dn whose content will be searched for replicating */
    private String baseDn;

    /** the ldap filter for fetching the entries, default value is (objectClass=*) */
    private String filter = "(objectClass=*)";

    /** names of attributes to be replicated, default value is all user attributes */
    private Set<String> attributes;

    /** the numer for setting the limit on number of search results to be fetched
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

    /** a flag to indicate to store the cookie in a file, default is false
     *  NOTE: a value of true indicates that the cookie will be stored
     *  on file system, which is useful while testing the consumer
     *  without loading config partition
     */
    private boolean storeCookieInFile = false;

    private static final String REPL_CONFIG_AREA = "ou=replProviders,ou=config";

    /** flag to indicate whether to chase referrals or not, default is false hence passes ManageDsaITControl with syncsearch request*/
    private boolean chaseReferrals = false;

    /** flag to indicate the use of TLS, default is true */
    private boolean useTls = true;

    /** flag to indicate the use of strict certificate verification, default is true */
    private boolean strictCertVerification = true;

    /** the X509 certificate trust manager used, default value set to {@link NoVerificationTrustManager} */
    private X509TrustManager trustManager = new NoVerificationTrustManager();


    public SyncreplConfiguration()
    {
        attributes = new HashSet<String>();
        attributes.add( SchemaConstants.ALL_USER_ATTRIBUTES );
        attributes.add( SchemaConstants.ENTRY_UUID_AT.toLowerCase() );
        attributes.add( SchemaConstants.ENTRY_CSN_AT.toLowerCase() );
        attributes.add( SchemaConstants.REF_AT.toLowerCase() );
    }


    /**
     * @return the providerHost
     */
    public String getProviderHost()
    {
        return providerHost;
    }


    /**
     * @param providerHost the providerHost to set
     */
    public void setProviderHost( String providerHost )
    {
        this.providerHost = providerHost;
    }


    /**
     * @return the port
     */
    public int getPort()
    {
        return port;
    }


    /**
     * @param port the port to set
     */
    public void setPort( int port )
    {
        this.port = port;
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
    public String getReplUserPassword()
    {
        return Strings.utf8ToString(replUserPassword);
    }


    /**
     * @param replUserPassword the replication user's password
     */
    public void setReplUserPassword( String replUserPassword )
    {
        setReplUserPassword( replUserPassword.getBytes() );
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
     * @param attr the attributes to set
     */
    public void setAttributes( String[] attr )
    {
        if ( attr == null )
        {
            throw new IllegalArgumentException( "attributes to be replicated cannot be null or empty" );
        }

        // if user specified some attributes then remove the * from attributes
        // NOTE: if the user specifies * in the given array that eventually gets added later
        if ( attr.length > 0 )
        {
            attributes.remove( SchemaConstants.ALL_USER_ATTRIBUTES );
        }

        for ( String at : attr )
        {
            at = at.trim();

            if ( !attributes.contains( at.toLowerCase() ) )
            {
                attributes.add( at );
            }
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
        if ( searchTimeout < 0 )
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


    public AliasDerefMode getAliasDerefMode()
    {
        return aliasDerefMode;
    }


    public void setAliasDerefMode( AliasDerefMode aliasDerefMode )
    {
        if ( aliasDerefMode != AliasDerefMode.NEVER_DEREF_ALIASES
            || aliasDerefMode != AliasDerefMode.DEREF_FINDING_BASE_OBJ )
        {
            throw new IllegalArgumentException(
                "alias deref mode should only be set to either 'NEVER_DEREF_ALIASES' or 'DEREF_FINDING_BASE_OBJ'" );
        }

        this.aliasDerefMode = aliasDerefMode;
    }


    public byte[] getCookie()
    {
        return cookie;
    }


    public void setCookie( byte[] cookie )
    {
        this.cookie = cookie;
    }


    public boolean isStoreCookieInFile()
    {
        return storeCookieInFile;
    }


    public void setStoreCookieInFile( boolean storeCookieInFile )
    {
        this.storeCookieInFile = storeCookieInFile;
    }


    public boolean isChaseReferrals()
    {
        return chaseReferrals;
    }


    public void setChaseReferrals( boolean chaseReferrals )
    {
        if ( chaseReferrals )
        {
            throw new UnsupportedOperationException( "client-api currently doesn't support chasing referrals" );
        }

        this.chaseReferrals = chaseReferrals;
    }


    public String getConfigEntryDn()
    {
        return "ads-dsReplicaId=" + replicaId + "," + REPL_CONFIG_AREA;
    }


    public boolean isUseTls()
    {
        return useTls;
    }


    /**
     * set the option to turn on/off use of TLS
     * 
     * @param useTls
     */
    public void setUseTls( boolean useTls )
    {
        this.useTls = useTls;
    }


    public boolean isStrictCertVerification()
    {
        return strictCertVerification;
    }


    /**
     * set the strict certificate verification
     * 
     * @param strictCertVerification
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


    public X509TrustManager getTrustManager()
    {
        return trustManager;
    }
}
