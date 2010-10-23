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
 * A class used to store the Replication Consumer configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ReplConsumerBean extends AdsBaseBean
{
    /** The replicaConsumer unique ID */
    private String dsreplicaid;
    
    /** The Alias Dereferencing mode */
    private String replaliasderefmode;
    
    /** The Search Base DN */
    private String searchbasedn;
    
    /** The last CSN sent */
    private String repllastsentcsn;
    
    /** The search scope */
    private String replsearchscope;
    
    /** The replication search filter */
    private String replsearchfilter;
    
    /** Tells if we should persist */
    private boolean replrefreshnpersist;
    
    /** Tells if TLS should be used during replication */
    private boolean replusetls;
    
    /** Tells if the certificate validation should be strict or not */
    private boolean replstrictcertvalidation;
    
    /** The peer certificate */
    private String replpeercertificate;

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
     * @return the dsreplicaid
     */
    public String getDsreplicaid()
    {
        return dsreplicaid;
    }

    
    /**
     * @param dsreplicaid the dsreplicaid to set
     */
    public void setDsreplicaid( String dsreplicaid )
    {
        this.dsreplicaid = dsreplicaid;
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
     * @return the searchBaseDN
     */
    public String getSearchBaseDN()
    {
        return searchbasedn;
    }

    
    /**
     * @param searchBaseDN the searchBaseDN to set
     */
    public void setSearchBaseDN( String searchBaseDN )
    {
        this.searchbasedn = searchBaseDN;
    }

    
    /**
     * @return the replLastSentCsn
     */
    public String getReplLastSentCsn()
    {
        return repllastsentcsn;
    }

    
    /**
     * @param replLastSentCsn the replLastSentCsn to set
     */
    public void setReplLastSentCsn( String replLastSentCsn )
    {
        this.repllastsentcsn = replLastSentCsn;
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
     * @param replSearchFilter the replSearchFilter to set
     */
    public void setReplSearchFilter( String replSearchFilter )
    {
        this.replsearchfilter = replSearchFilter;
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
     * @return the replUseTls
     */
    public boolean isReplUseTls()
    {
        return replusetls;
    }

    
    /**
     * @param replUseTls the replUseTls to set
     */
    public void setReplUseTls( boolean replUseTls )
    {
        this.replusetls = replUseTls;
    }

    
    /**
     * @return the replStrictCertValidation
     */
    public boolean isReplStrictCertValidation()
    {
        return replstrictcertvalidation;
    }

    
    /**
     * @param replStrictCertValidation the replStrictCertValidation to set
     */
    public void setReplStrictCertValidation( boolean replStrictCertValidation )
    {
        this.replstrictcertvalidation = replStrictCertValidation;
    }

    
    /**
     * @return the replPeerCertificate
     */
    public String getReplPeerCertificate()
    {
        return replpeercertificate;
    }

    
    /**
     * @param replPeerCertificate the replPeerCertificate to set
     */
    public void setReplPeerCertificate( String replPeerCertificate )
    {
        this.replpeercertificate = replPeerCertificate;
    }

    
    /**
     * {@inheritDoc}
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( tabs ).append( "replication consumer :\n" );
        sb.append( super.toString( tabs + "  " ) );

        sb.append( tabs ).append( "  replica ID : " ).append( dsreplicaid ).append( '\n' );
        sb.append( tabs ).append( "  last sent CSN : " ).append( repllastsentcsn ).append( '\n' );
        sb.append( tabs ).append( "  search base DN : " ).append( searchbasedn ).append( '\n' );
        sb.append( tabs ).append( "  search filter : " ).append( replsearchfilter ).append( '\n' );
        sb.append( tabs ).append( "  search scope : " ).append( replsearchscope ).append( '\n' );
        sb.append( tabs ).append( "  alias dereferencing mode : " ).append( replaliasderefmode ).append( '\n' );

        sb.append( toString( tabs, "  peer certificate", replpeercertificate ) );
        sb.append( toStringBoolean( tabs, "  refresh and persist mode", replrefreshnpersist ) );
        sb.append( toStringBoolean( tabs, "  struct certivicate validation", replstrictcertvalidation ) );
        sb.append( toStringBoolean( tabs, "  use TLS", replusetls ) );

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
