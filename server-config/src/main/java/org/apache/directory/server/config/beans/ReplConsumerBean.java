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


import java.util.ArrayList;
import java.util.List;

import org.apache.directory.server.config.ConfigurationElement;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;


/**
 * A class used to store the Replication Consumer configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ReplConsumerBean extends AdsBaseBean
{
    /** The consumer id */
    @ConfigurationElement(attributeType = SchemaConstants.ADS_REPL_CONSUMER_ID, isRdn = true)
    private String replConsumerId;

    /** The Search Base Dn */
    @ConfigurationElement(attributeType = SchemaConstants.ADS_SEARCH_BASE_DN)
    private String searchBaseDn;

    /** The replication provider host name */
    @ConfigurationElement(attributeType = SchemaConstants.ADS_REPL_PROV_HOST_NAME)
    private String replProvHostName;

    /** The replication provider port */
    @ConfigurationElement(attributeType = SchemaConstants.ADS_REPL_PROV_PORT)
    private int replProvPort;

    /** The Alias Dereferencing mode */
    @ConfigurationElement(attributeType = SchemaConstants.ADS_REPL_ALIAS_DEREF_MODE)
    private String replAliasDerefMode;

    /** The replication provider attribute */
    @ConfigurationElement(attributeType = SchemaConstants.ADS_REPL_ATTRIBUTES)
    private List<String> replAttributes = new ArrayList<String>();

    /** The refresh interval */
    @ConfigurationElement(attributeType = SchemaConstants.ADS_REPL_REFRESH_INTERVAL)
    private long replRefreshInterval;

    /** Tells if we should persist */
    @ConfigurationElement(attributeType = SchemaConstants.ADS_REPL_REFRESH_N_PERSIST)
    private boolean replRefreshNPersist;

    /** The search scope */
    @ConfigurationElement(attributeType = SchemaConstants.ADS_REPL_SEARCH_SCOPE)
    private String replSearchScope;

    /** The replication search filter */
    @ConfigurationElement(attributeType = SchemaConstants.ADS_REPL_SEARCH_FILTER)
    private String replSearchFilter;

    /** The search size limit */
    @ConfigurationElement(attributeType = SchemaConstants.ADS_REPL_SEARCH_SIZE_LIMIT)
    private int replSearchSizeLimit;

    /** The search time limit */
    @ConfigurationElement(attributeType = SchemaConstants.ADS_REPL_SEARCH_TIMEOUT)
    private int replSearchTimeout;

    /** The replication user Dn */
    @ConfigurationElement(attributeType = SchemaConstants.ADS_REPL_USER_DN)
    private String replUserDn;

    /** The replication user password */
    @ConfigurationElement(attributeType = SchemaConstants.ADS_REPL_USER_PASSWORD)
    private byte[] replUserPassword;

    /** The replication cookie */
    @ConfigurationElement(attributeType = SchemaConstants.ADS_REPL_COOKIE)
    private String replCookie;

    /** Tells if TLS should be used during replication */
    @ConfigurationElement(attributeType = SchemaConstants.ADS_REPL_USE_TLS)
    private boolean replUseTls;

    /** Tells if the certificate validation should be strict or not */
    @ConfigurationElement(attributeType = SchemaConstants.ADS_REPL_STRICT_CERT_VALIDATION)
    private boolean replStrictCertValidation;

    /** The peer certificate */
    @ConfigurationElement(attributeType = SchemaConstants.ADS_REPL_PEER_CERTIFICATE)
    private byte[] replPeerCertificate;

    /** The FQCN of replication client implementation */
    @ConfigurationElement(attributeType = SchemaConstants.ADS_REPL_CONSUMER_IMPL)
    private String replConsumerImpl;


    /**
     * Create a new Replication Consumer instance
     */
    public ReplConsumerBean()
    {
        super();

        // Enabled by default
        setEnabled( true );
    }


    /**
     * @return the replConsumerId
     */
    public String getReplConsumerId()
    {
        return replConsumerId;
    }


    /**
     * @param replConsumerId the replication consumer id to set
     */
    public void setReplConsumerId( String replConsumerId )
    {
        this.replConsumerId = replConsumerId;
    }


    /**
     * @return the searchBaseDn
     */
    public String getSearchBaseDn()
    {
        return searchBaseDn;
    }


    /**
     * @param searchBaseDN the searchBaseDn to set
     */
    public void setSearchBaseDn( String searchBaseDn )
    {
        this.searchBaseDn = searchBaseDn;
    }


    /**
     * @return the replProvHostName
     */
    public String getReplProvHostName()
    {
        return replProvHostName;
    }


    /**
     * @param replProvHostName the replProvHostName to set
     */
    public void setReplProvHostName( String replProvHostName )
    {
        this.replProvHostName = replProvHostName;
    }


    /**
     * @return the replProvPort
     */
    public int getReplProvPort()
    {
        return replProvPort;
    }


    /**
     * @param replProvPort the replProvPort to set
     */
    public void setReplProvPort( int replProvPort )
    {
        this.replProvPort = replProvPort;
    }


    /**
     * @return the replAliasDerefMode
     */
    public String getReplAliasDerefMode()
    {
        return replAliasDerefMode;
    }


    /**
     * @param replAliasDerefMode the replAliasDerefMode to set
     */
    public void setReplAliasDerefMode( String replAliasDerefMode )
    {
        this.replAliasDerefMode = replAliasDerefMode;
    }


    /**
     * @return the replAttributes
     */
    public List<String> getReplAttributes()
    {
        return replAttributes;
    }


    /**
     * @param replAttributes the replAttribute to set
     */
    public void setReplAttributes( List<String> replAttributes )
    {
        this.replAttributes = replAttributes;
    }


    /**
     * @param replAttributes the replAttribute to add
     */
    public void addReplAttributes( String... replAttributes )
    {
        for ( String at : replAttributes )
        {
            this.replAttributes.add( at );
        }
    }


    /**
     * @return the replRefreshInterval
     */
    public long getReplRefreshInterval()
    {
        return replRefreshInterval;
    }


    /**
     * @param replRefreshInterval the replRefreshInterval to set
     */
    public void setReplRefreshInterval( long replRefreshInterval )
    {
        this.replRefreshInterval = replRefreshInterval;
    }


    /**
     * @return the replRefreshNPersist
     */
    public boolean isReplRefreshNPersist()
    {
        return replRefreshNPersist;
    }


    /**
     * @param replRefreshNPersist the replRefreshNPersist to set
     */
    public void setReplRefreshNPersist( boolean replRefreshNPersist )
    {
        this.replRefreshNPersist = replRefreshNPersist;
    }


    /**
     * @return the replSearchScope
     */
    public String getReplSearchScope()
    {
        return replSearchScope;
    }


    /**
     * @param replSearchScope the replSearchScope to set
     */
    public void setReplSearchScope( String replSearchScope )
    {
        this.replSearchScope = replSearchScope;
    }


    /**
     * @return the replSearchFilter
     */
    public String getReplSearchFilter()
    {
        return replSearchFilter;
    }


    /**
     * @param replSearchFilter the replSearchFilter to set
     */
    public void setReplSearchFilter( String replSearchFilter )
    {
        this.replSearchFilter = replSearchFilter;
    }


    /**
     * @return the replSearchSizeLimit
     */
    public int isReplSearchSizeLimit()
    {
        return replSearchSizeLimit;
    }


    /**
     * @param replSearchSizeLimit the replSearchSizeLimit to set
     */
    public void setReplSearchSizeLimit( int replSearchSizeLimit )
    {
        this.replSearchSizeLimit = replSearchSizeLimit;
    }


    /**
     * @return the replSearchTimeOut
     */
    public int getReplSearchTimeOut()
    {
        return replSearchTimeout;
    }


    /**
     * @param replSearchTimeOut the replSearchTimeOut to set
     */
    public void setReplSearchTimeLimit( int replSearchTimeOut )
    {
        this.replSearchTimeout = replSearchTimeOut;
    }


    /**
     * @return the replUserDn
     */
    public String getReplUserDn()
    {
        return replUserDn;
    }


    /**
     * @param replUserDn the replUserDn to set
     */
    public void setReplUserDn( String replUserDn )
    {
        this.replUserDn = replUserDn;
    }


    /**
     * @return the replUserPassword
     */
    public byte[] getReplUserPassword()
    {
        return replUserPassword;
    }


    /**
     * @param replUserPassword the replUserPassword to set
     */
    public void setReplUserPassword( byte[] replUserPassword )
    {
        this.replUserPassword = replUserPassword;
    }


    /**
     * @return the replCookie
     */
    public String getReplCookie()
    {
        return replCookie;
    }


    /**
     * @param replCookie the replCookie to set
     */
    public void setReplCookie( String replCookie )
    {
        this.replCookie = replCookie;
    }


    /**
     * @return the replUseTls
     */
    public boolean isReplUseTls()
    {
        return replUseTls;
    }


    /**
     * @param replUseTls the replUseTls to set
     */
    public void setReplUseTls( boolean replUseTls )
    {
        this.replUseTls = replUseTls;
    }


    /**
     * @return the replStrictCertValidation
     */
    public boolean isReplStrictCertValidation()
    {
        return replStrictCertValidation;
    }


    /**
     * @param replStrictCertValidation the replStrictCertValidation to set
     */
    public void setReplStrictCertValidation( boolean replStrictCertValidation )
    {
        this.replStrictCertValidation = replStrictCertValidation;
    }


    /**
     * @return the replPeerCertificate
     */
    public byte[] getReplPeerCertificate()
    {
        return replPeerCertificate;
    }


    /**
     * @param replPeerCertificate the replPeerCertificate to set
     */
    public void setReplPeerCertificate( byte[] replPeerCertificate )
    {
        this.replPeerCertificate = replPeerCertificate;
    }


    /**
     * @return the replConsumerImpl
     */
    public String getReplConsumerImpl()
    {
        return replConsumerImpl;
    }


    /**
     * @param replConsumerImpl the replConsumerImpl to set
     */
    public void setReplConsumerImpl( String replConsumerImpl )
    {
        this.replConsumerImpl = replConsumerImpl;
    }


    /**
     * {@inheritDoc}
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "Replication provider :\n" );
        sb.append( super.toString( tabs + "  " ) );

        sb.append( tabs ).append( "  provider ID : " ).append( replConsumerId ).append( '\n' );
        sb.append( tabs ).append( "  search base Dn : " ).append( searchBaseDn ).append( '\n' );
        sb.append( tabs ).append( "  provider host name : " ).append( replProvHostName ).append( '\n' );
        sb.append( tabs ).append( "  provider port : " ).append( replProvPort ).append( '\n' );
        sb.append( toString( tabs, "  alias dereferencing mode", replAliasDerefMode ) );
        sb.append( toString( tabs, "  attributes", String.valueOf( replAttributes ) ) );
        sb.append( tabs ).append( "  refresh interval : " ).append( replRefreshInterval ).append( '\n' );
        sb.append( toString( tabs, "  refresh and persist mode", replRefreshNPersist ) );
        sb.append( toString( tabs, "  search scope", replSearchScope ) );
        sb.append( toString( tabs, "  search filter", replSearchFilter ) );
        sb.append( tabs ).append( "  search size limit : " ).append( replSearchSizeLimit ).append( '\n' );
        sb.append( tabs ).append( "  search time limit : " ).append( replSearchTimeout ).append( '\n' );
        sb.append( toString( tabs, "  user Dn", replUserDn ) );
        sb.append( toString( tabs, "  user password", String.valueOf( replUserPassword ) ) ); // do not reveal the password, just null or not
        sb.append( toString( tabs, "  cookie", replCookie ) );
        sb.append( tabs ).append( "  consumer implementation's FQCN : " ).append( replConsumerImpl ).append( '\n' );

        // a hex dump would be good but printing the address is better to just know null or not
        sb.append( toString( tabs, "  peer certificate", String.valueOf( replPeerCertificate ) ) );

        sb.append( toString( tabs, "  struct certivicate validation", replStrictCertValidation ) );
        sb.append( toString( tabs, "  use TLS", replUseTls ) );

        return sb.toString();
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return toString( "" );
    }
}
