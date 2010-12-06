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

import org.apache.directory.server.config.AttributeType;
import org.apache.directory.server.config.RDN;
import org.apache.directory.shared.ldap.name.DN;


/**
 * A class used to store the Replication Producer configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ReplProviderBean extends AdsBaseBean
{
    /** The provider id */
    @AttributeType("ads-replProviderId")
    @RDN
    private String replProviderId;
    
    /** The replication unique ID */
    @AttributeType("ads-dsReplicaId")
    private String dsReplicaId;
    
    /** The Search Base DN */
    @AttributeType("ads-searchBaseDN")
    private DN searchBaseDN;
    
    /** The replication provider host name */
    @AttributeType("ads-replProvHostName")
    private String replProvHostName;
    
    /** The replication provider port */
    @AttributeType("ads-replProvPort")
    private String replProvPort;
    
    /** The Alias Dereferencing mode */
    @AttributeType("ads-replAliasDerefMode")
    private String replAliasDerefMode;
    
    /** The replication provider attribute */
    @AttributeType("ads-replAttribute")
    private String replAttribute;
    
    /** The refresh interval */
    @AttributeType("ads-replRefreshInterval")
    private String replRefreshInterval;
    
    /** Tells if we should persist */
    @AttributeType("ads-replRefreshNPersist")
    private boolean replRefreshNPersist;
    
    /** The search scope */
    @AttributeType("ads-replSearchScope")
    private String replSearchScope;
    
    /** The replication search filter */
    @AttributeType("ads-replSearchFilter")
    private String replSearchFilter;

    /** The search size limit */
    @AttributeType("ads-replSearchSizeLimit")
    private int replSearchSizeLimit;
    
    /** The search time limit */
    @AttributeType("ads-replSearchTimeout")
    private int replSearchTimeout;
    
    /** The replication user DN */
    @AttributeType("ads-replUserDN")
    private DN replUserDN;
    
    /** The replication user password */
    @AttributeType("ads-replUserPassword")
    private String replUserPassword;
    
    /** The replication cookie */
    @AttributeType("ads-replCookie")
    private String replCookie;

    /**
     * Create a new Replication Consumer instance
     */
    public ReplProviderBean()
    {
        super();
        
        // Enabled by default
        setEnabled( true );
    }


    /**
     * @return the replProviderId
     */
    public String getReplProviderId()
    {
        return replProviderId;
    }


    /**
     * @param replProviderId the replProviderId to set
     */
    public void setReplProviderId( String replProviderId )
    {
        this.replProviderId = replProviderId;
    }

    
    /**
     * @return the dsreplicaid
     */
    public String getDsReplicaId()
    {
        return dsReplicaId;
    }


    /**
     * @param dsReplicaId the Replica ID to set
     */
    public void setDsReplicaId( String dsReplicaId )
    {
        this.dsReplicaId = dsReplicaId;
    }


    /**
     * @return the searchBaseDn
     */
    public DN getSearchBaseDn()
    {
        return searchBaseDN;
    }


    /**
     * @param searchBaseDN the searchBaseDn to set
     */
    public void setSearchBaseDn( DN searchBaseDn )
    {
        this.searchBaseDN = searchBaseDn;
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
    public String getReplProvPort()
    {
        return replProvPort;
    }


    /**
     * @param replProvPort the replProvPort to set
     */
    public void setReplProvPort( String replProvPort )
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
     * @return the replAttribute
     */
    public String getReplAttribute()
    {
        return replAttribute;
    }


    /**
     * @param replAttribute the replAttribute to set
     */
    public void setReplAttribute( String replAttribute )
    {
        this.replAttribute = replAttribute;
    }


    /**
     * @return the replRefreshInterval
     */
    public String getReplRefreshInterval()
    {
        return replRefreshInterval;
    }


    /**
     * @param replRefreshInterval the replRefreshInterval to set
     */
    public void setReplRefreshInterval( String replRefreshInterval )
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
    public int isReplSearchTimeOut()
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
    public DN isReplUserDn()
    {
        return replUserDN;
    }


    /**
     * @param replUserDn the replUserDn to set
     */
    public void setReplUserDn( DN replUserDn )
    {
        this.replUserDN = replUserDn;
    }


    /**
     * @return the replUserPassword
     */
    public String isReplUserPassword()
    {
        return replUserPassword;
    }


    /**
     * @param replUserPassword the replUserPassword to set
     */
    public void setReplUserPassword( String replUserPassword )
    {
        this.replUserPassword = replUserPassword;
    }


    /**
     * @return the replCookie
     */
    public String isReplCookie()
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
     * {@inheritDoc}
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( tabs ).append( "Replication provider :\n" );
        sb.append( super.toString( tabs + "  " ) );

        sb.append( tabs ).append( "  provider ID : " ).append( replProviderId ).append( '\n' );
        sb.append( tabs ).append( "  replica ID : " ).append( dsReplicaId ).append( '\n' );
        sb.append( tabs ).append( "  search base DN : " ).append( searchBaseDN.getName() ).append( '\n' );
        sb.append( tabs ).append( "  provider host name : " ).append( replProvHostName ).append( '\n' );
        sb.append( tabs ).append( "  provider port : " ).append( replProvPort ).append( '\n' );
        sb.append( toString( tabs, "  alias dereferencing mode", replAliasDerefMode ) );
        sb.append( toString( tabs, "  attribute", replAttribute ) );
        sb.append( tabs ).append( "  refresh interval : " ).append( replRefreshInterval ).append( '\n' );
        sb.append( toString( tabs, "  refresh and persist mode", replRefreshNPersist ) );
        sb.append( toString( tabs, "  search scope", replSearchScope ) );
        sb.append( toString( tabs, "  search filter", replSearchFilter ) );
        sb.append( tabs ).append( "  search size limit : " ).append( replSearchSizeLimit ).append( '\n' );
        sb.append( tabs ).append( "  search time limit : " ).append( replSearchTimeout ).append( '\n' );
        sb.append( toString( tabs, "  user DN", replUserDN ) );
        sb.append( toString( tabs, "  user password", replUserPassword ) );
        sb.append( toString( tabs, "  cookie", replCookie ) );

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
