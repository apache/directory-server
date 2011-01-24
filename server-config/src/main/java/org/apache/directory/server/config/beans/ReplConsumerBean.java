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


import org.apache.directory.server.config.ConfigurationElement;
import org.apache.directory.shared.ldap.model.name.Dn;


/**
 * A class used to store the Replication Consumer configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ReplConsumerBean extends AdsBaseBean
{
    /** The consumer id */
    @ConfigurationElement(attributeType = "ads-replConsumerId", isRdn = true)
    private String replConsumerId;

    /** The replication unique ID */
    @ConfigurationElement(attributeType = "ads-dsReplicaId")
    private String dsReplicaId;

    /** The Alias Dereferencing mode */
    @ConfigurationElement(attributeType = "ads-replAliasDerefMode")
    private String replAliasDerefMode;

    /** The Search Base Dn */
    @ConfigurationElement(attributeType = "ads-searchBaseDn")
    private Dn searchBaseDn;

    /** The last CSN sent */
    @ConfigurationElement(attributeType = "ads-replLastSentCsn")
    private String replLastSentCsn;

    /** The search scope */
    @ConfigurationElement(attributeType = "ads-replSearchScope")
    private String replSearchScope;

    /** The replication search filter */
    @ConfigurationElement(attributeType = "ads-replSearchFilter")
    private String replSearchFilter;

    /** Tells if we should persist */
    @ConfigurationElement(attributeType = "ads-replRefreshNPersist")
    private boolean replRefreshNPersist;

    /** Tells if TLS should be used during replication */
    @ConfigurationElement(attributeType = "ads-replUseTls")
    private boolean replUseTls;

    /** Tells if the certificate validation should be strict or not */
    @ConfigurationElement(attributeType = "ads-replStrictCertValidation")
    private boolean replStrictCertValidation;

    /** The peer certificate */
    @ConfigurationElement(attributeType = "ads-replPeerCertificate")
    private String replPeerCertificate;


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
     * @param replConsumerId the replConsumerId to set
     */
    public void setReplConsumerId( String replConsumerId )
    {
        this.replConsumerId = replConsumerId;
    }


    /**
     * @return the dsreplicaid
     */
    public String getDsreplicaid()
    {
        return dsReplicaId;
    }


    /**
     * @param dsreplicaid the dsreplicaid to set
     */
    public void setDsreplicaid( String dsreplicaid )
    {
        this.dsReplicaId = dsreplicaid;
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
     * @return the searchBaseDn
     */
    public Dn getSearchBaseDn()
    {
        return searchBaseDn;
    }


    /**
     * @param searchBaseDn the searchBaseDn to set
     */
    public void setSearchBaseDn(Dn searchBaseDn)
    {
        this.searchBaseDn = searchBaseDn;
    }


    /**
     * @return the replLastSentCsn
     */
    public String getReplLastSentCsn()
    {
        return replLastSentCsn;
    }


    /**
     * @param replLastSentCsn the replLastSentCsn to set
     */
    public void setReplLastSentCsn( String replLastSentCsn )
    {
        this.replLastSentCsn = replLastSentCsn;
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
    public String getReplPeerCertificate()
    {
        return replPeerCertificate;
    }


    /**
     * @param replPeerCertificate the replPeerCertificate to set
     */
    public void setReplPeerCertificate( String replPeerCertificate )
    {
        this.replPeerCertificate = replPeerCertificate;
    }


    /**
     * {@inheritDoc}
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "Replication consumer :\n" );
        sb.append( super.toString( tabs + "  " ) );

        sb.append( tabs ).append( "  consumer ID : " ).append( replConsumerId ).append( '\n' );
        sb.append( tabs ).append( "  replica ID : " ).append( dsReplicaId ).append( '\n' );
        sb.append( tabs ).append( "  last sent CSN : " ).append( replLastSentCsn ).append( '\n' );
        sb.append( tabs ).append( "  search base Dn : " ).append( searchBaseDn.getName() ).append( '\n' );
        sb.append( tabs ).append( "  search filter : " ).append( replSearchFilter ).append( '\n' );
        sb.append( tabs ).append( "  search scope : " ).append( replSearchScope ).append( '\n' );
        sb.append( tabs ).append( "  alias dereferencing mode : " ).append( replAliasDerefMode ).append( '\n' );

        sb.append( toString( tabs, "  peer certificate", replPeerCertificate ) );
        sb.append( toString( tabs, "  refresh and persist mode", replRefreshNPersist ) );
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
