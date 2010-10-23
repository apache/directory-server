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


/**
 * A class used to store the Replication Producer configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ReplProviderBean extends AdsBaseBean
{
    /** The provider id */
    private String replproviderid;
    
    /** The replication unique ID */
    private String dsreplicaid;
    
    /** The Search Base DN */
    private String searchbasedn;
    
    /** The replication provider host name */
    private String replprovhostname;
    
    /** The replication provider port */
    private String replprovport;
    
    /** The Alias Dereferencing mode */
    private String replaliasderefmode;
    
    /** The replication provider attribute */
    private String replattribute;
    
    /** The refresh interval */
    private String replrefreshinterval;
    
    /** Tells if we should persist */
    private boolean replrefreshnpersist;
    
    /** The search scope */
    private String replsearchscope;
    
    /** The replication search filter */
    private String replsearchfilter;

    /** The search size limit */
    private int replsearchsizelimit;
    
    /** The search time limit */
    private int replsearchtimeout;
    
    /** The replication user DN */
    private String repluserdn;
    
    /** The replication user password */
    private String repluserpassword;
    
    /** The replication cookie */
    private String replcookie;

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
        return replproviderid;
    }


    /**
     * @param replProviderId the replProviderId to set
     */
    public void setReplProviderId( String replProviderId )
    {
        this.replproviderid = replProviderId;
    }

    
    /**
     * @return the dsreplicaid
     */
    public String getDsReplicaId()
    {
        return dsreplicaid;
    }


    /**
     * @param dsreplicaid the Replica ID to set
     */
    public void setDsReplicaId( String dsReplicaId )
    {
        this.dsreplicaid = dsReplicaId;
    }


    /**
     * @return the searchBaseDn
     */
    public String getSearchBaseDn()
    {
        return searchbasedn;
    }


    /**
     * @param searchbasedn the searchBaseDn to set
     */
    public void setSearchBaseDn( String searchBaseDn )
    {
        this.searchbasedn = searchBaseDn;
    }


    /**
     * @return the replProvHostName
     */
    public String getReplProvHostName()
    {
        return replprovhostname;
    }


    /**
     * @param replProvHostName the replProvHostName to set
     */
    public void setReplProvHostName( String replProvHostName )
    {
        this.replprovhostname = replProvHostName;
    }


    /**
     * @return the replProvPort
     */
    public String getReplProvPort()
    {
        return replprovport;
    }


    /**
     * @param replProvPort the replProvPort to set
     */
    public void setReplProvPort( String replProvPort )
    {
        this.replprovport = replProvPort;
    }


    /**
     * @return the replAliasDerefMode
     */
    public String getReplAliasDerefMode()
    {
        return replaliasderefmode;
    }


    /**
     * @param replAliasDerefMode the replAliasDerefMode to set
     */
    public void setReplAliasDerefMode( String replAliasDerefMode )
    {
        this.replaliasderefmode = replAliasDerefMode;
    }


    /**
     * @return the replAttribute
     */
    public String getReplAttribute()
    {
        return replattribute;
    }


    /**
     * @param replAttribute the replAttribute to set
     */
    public void setReplAttribute( String replAttribute )
    {
        this.replattribute = replAttribute;
    }


    /**
     * @return the replRefreshInterval
     */
    public String getReplRefreshInterval()
    {
        return replrefreshinterval;
    }


    /**
     * @param replRefreshInterval the replRefreshInterval to set
     */
    public void setReplRefreshInterval( String replRefreshInterval )
    {
        this.replrefreshinterval = replRefreshInterval;
    }


    /**
     * @return the replRefreshNPersist
     */
    public boolean isReplRefreshNPersist()
    {
        return replrefreshnpersist;
    }


    /**
     * @param replRefreshNPersist the replRefreshNPersist to set
     */
    public void setReplRefreshNPersist( boolean replRefreshNPersist )
    {
        this.replrefreshnpersist = replRefreshNPersist;
    }


    /**
     * @return the replSearchScope
     */
    public String getReplSearchScope()
    {
        return replsearchscope;
    }


    /**
     * @param replSearchScope the replSearchScope to set
     */
    public void setReplSearchScope( String replSearchScope )
    {
        this.replsearchscope = replSearchScope;
    }


    /**
     * @return the replSearchFilter
     */
    public String getReplSearchFilter()
    {
        return replsearchfilter;
    }


    /**
     * @param replsearchfilter the replSearchFilter to set
     */
    public void setReplSearchFilter( String replSearchFilter )
    {
        this.replsearchfilter = replSearchFilter;
    }


    /**
     * @return the replSearchSizeLimit
     */
    public int isReplSearchSizeLimit()
    {
        return replsearchsizelimit;
    }


    /**
     * @param replSearchSizeLimit the replSearchSizeLimit to set
     */
    public void setReplSearchSizeLimit( int replSearchSizeLimit )
    {
        this.replsearchsizelimit = replSearchSizeLimit;
    }


    /**
     * @return the replSearchTimeOut
     */
    public int isReplSearchTimeOut()
    {
        return replsearchtimeout;
    }


    /**
     * @param replSearchTimeOut the replSearchTimeOut to set
     */
    public void setReplSearchTimeLimit( int replSearchTimeOut )
    {
        this.replsearchtimeout = replSearchTimeOut;
    }


    /**
     * @return the replUserDn
     */
    public String isReplUserDn()
    {
        return repluserdn;
    }


    /**
     * @param replUserDn the replUserDn to set
     */
    public void setReplUserDn( String replUserDn )
    {
        this.repluserdn = replUserDn;
    }


    /**
     * @return the replUserPassword
     */
    public String isReplUserPassword()
    {
        return repluserpassword;
    }


    /**
     * @param replUserPassword the replUserPassword to set
     */
    public void setReplUserPassword( String replUserPassword )
    {
        this.repluserpassword = replUserPassword;
    }


    /**
     * @return the replCookie
     */
    public String isReplCookie()
    {
        return replcookie;
    }


    /**
     * @param replCookie the replCookie to set
     */
    public void setReplCookie( String replCookie )
    {
        this.replcookie = replCookie;
    }

    
    /**
     * {@inheritDoc}
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( tabs ).append( "Replication provider :\n" );
        sb.append( super.toString( tabs + "  " ) );

        sb.append( tabs ).append( "  provider ID : " ).append( replproviderid ).append( '\n' );
        sb.append( tabs ).append( "  replica ID : " ).append( dsreplicaid ).append( '\n' );
        sb.append( tabs ).append( "  search base DN : " ).append( searchbasedn ).append( '\n' );
        sb.append( tabs ).append( "  provider host name : " ).append( replprovhostname ).append( '\n' );
        sb.append( tabs ).append( "  provider port : " ).append( replprovport ).append( '\n' );
        sb.append( toString( tabs, "  alias dereferencing mode", replaliasderefmode ) );
        sb.append( toString( tabs, "  attribute", replattribute ) );
        sb.append( tabs ).append( "  refresh interval : " ).append( replrefreshinterval ).append( '\n' );
        sb.append( toString( tabs, "  refresh and persist mode", replrefreshnpersist ) );
        sb.append( toString( tabs, "  search scope", replsearchscope ) );
        sb.append( toString( tabs, "  search filter", replsearchfilter ) );
        sb.append( tabs ).append( "  search size limit : " ).append( replsearchsizelimit ).append( '\n' );
        sb.append( tabs ).append( "  search time limit : " ).append( replsearchtimeout ).append( '\n' );
        sb.append( toString( tabs, "  user DN", repluserdn ) );
        sb.append( toString( tabs, "  user password", repluserpassword ) );
        sb.append( toString( tabs, "  cookie", replcookie ) );

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
