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
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * A class used to store the LdapServer configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapServerBean extends DSBasedServerBean
{
    /** */
    private boolean confidentialityrequired;
    
    /** The maximum number of entries returned by the server */
    private int maxsizelimit;
    
    /** The maximum time to execute a request on the server */
    private int maxtimelimit;
    
    /** The SASL host */
    private String saslhost;
    
    /** The SASL  principal */
    private String saslprincipal;
    
    /** The SASL realms */
    private Set<String> saslrealms = new HashSet<String>();
    
    /** The keystore file */
    private String keystorefile;
    
    /** The certificate password */
    private String certificatepassword;
    
    /** tells if the replication is enabled */
    private boolean enablereplprovider; 
    
    private int replconsumer;
    private int replprovider;
    
    /** The list of supported mechanisms */
    private Set<SaslMechanisHandlerBean> saslmechhandlers = new HashSet<SaslMechanisHandlerBean>();
    
    /** The list of supported extended operation handlers */
    private List<ExtendedOpHandlerBean> extendedophandlers = new ArrayList<ExtendedOpHandlerBean>();

    /**
     * Create a new LdapServerBean instance
     */
    public LdapServerBean()
    {
        super();
        
        // Enabled by default
        setEnabled( true );
    }

    
    /**
     * @return the ldapServerConfidentialityRequired
     */
    public boolean isLdapServerConfidentialityRequired()
    {
        return confidentialityrequired;
    }

    
    /**
     * @param ldapServerConfidentialityRequired the ldapServerConfidentialityRequired to set
     */
    public void setLdapServerConfidentialityRequired( boolean ldapServerConfidentialityRequired )
    {
        this.confidentialityrequired = ldapServerConfidentialityRequired;
    }

    
    /**
     * @return the ldapServerMaxSizeLimit
     */
    public int getLdapServerMaxSizeLimit()
    {
        return maxsizelimit;
    }

    
    /**
     * @param ldapServerMaxSizeLimit the ldapServerMaxSizeLimit to set
     */
    public void setLdapServerMaxSizeLimit( int ldapServerMaxSizeLimit )
    {
        this.maxsizelimit = ldapServerMaxSizeLimit;
    }

    
    /**
     * @return the ldapServerMaxTimeLimit
     */
    public int getLdapServerMaxTimeLimit()
    {
        return maxtimelimit;
    }

    
    /**
     * @param ldapServerMaxTimeLimit the ldapServerMaxTimeLimit to set
     */
    public void setLdapServerMaxTimeLimit( int ldapServerMaxTimeLimit )
    {
        this.maxtimelimit = ldapServerMaxTimeLimit;
    }

    
    /**
     * @return the ldapServerSaslHost
     */
    public String getLdapServerSaslHost()
    {
        return saslhost;
    }

    
    /**
     * @param ldapServerSaslHost the ldapServerSaslHost to set
     */
    public void setLdapServerSaslHost( String ldapServerSaslHost )
    {
        this.saslhost = ldapServerSaslHost;
    }

    
    /**
     * @return the ldapServerSaslPrincipal
     */
    public String getLdapServerSaslPrincipal()
    {
        return saslprincipal;
    }

    
    /**
     * @param ldapServerSaslPrincipal the ldapServerSaslPrincipal to set
     */
    public void setLdapServerSaslPrincipal( String ldapServerSaslPrincipal )
    {
        this.saslprincipal = ldapServerSaslPrincipal;
    }

    
    /**
     * @return the ldapServerSaslRealms
     */
    public Set<String> getLdapServerSaslRealms()
    {
        return saslrealms;
    }

    
    /**
     * @param ldapServerSaslRealms the ldapServerSaslRealms to set
     */
    public void setLdapServerSaslRealms( Set<String> ldapServerSaslRealms )
    {
        this.saslrealms = ldapServerSaslRealms;
    }

    
    /**
     * @param ldapServerSaslRealms the ldapServerSaslRealms to add
     */
    public void addsaslrealms( String... ldapServerSaslRealms )
    {
        for ( String saslRealm : ldapServerSaslRealms )
        {
            this.saslrealms.add( saslRealm );
        }
    }

    
    /**
     * @return the ldapServerKeystoreFile
     */
    public String getLdapServerKeystoreFile()
    {
        return keystorefile;
    }

    
    /**
     * @param ldapServerKeystoreFile the ldapServerKeystoreFile to set
     */
    public void setLdapServerKeystoreFile( String ldapServerKeystoreFile )
    {
        this.keystorefile = ldapServerKeystoreFile;
    }

    
    /**
     * @return the ldapServerCertificatePassword
     */
    public String getLdapServerCertificatePassword()
    {
        return certificatepassword;
    }

    
    /**
     * @param ldapServerCertificatePassword the ldapServerCertificatePassword to set
     */
    public void setLdapServerCertificatePassword( String ldapServerCertificatePassword )
    {
        this.certificatepassword = ldapServerCertificatePassword;
    }

    
    /**
     * @return the replProviderImpl
     *
    public ReplicationProviderBean getReplProviderImpl()
    {
        return replProviderImpl;
    }

    
    /**
     * @param replProviderImpl the replProviderImpl to set
     *
    public void setReplProviderImpl( ReplicationProviderBean replProviderImpl )
    {
        this.replProviderImpl = replProviderImpl;
    }

    
    /**
     * @return the enableReplProvider
     */
    public boolean isEnableReplProvider()
    {
        return enablereplprovider;
    }

    
    /**
     * @param enableReplProvider the enableReplProvider to set
     */
    public void setEnableReplProvider( boolean enableReplProvider )
    {
        this.enablereplprovider = enableReplProvider;
    }

    
    /**
     * @return the saslMechHandlers
     */
    public Set<SaslMechanisHandlerBean> getSaslMechHandlers()
    {
        return saslmechhandlers;
    }

    
    /**
     * @param saslMechHandlers the saslMechHandlers to set
     */
    public void setSaslMechHandlers( Set<SaslMechanisHandlerBean> saslMechHandlers )
    {
        this.saslmechhandlers = saslMechHandlers;
    }

    
    /**
     * @param saslMechHandlers the saslMechHandlers to add
     */
    public void setSaslMechHandlers( SaslMechanisHandlerBean... saslMechHandlers )
    {
        for ( SaslMechanisHandlerBean saslMechHandler : saslMechHandlers )
        {
            this.saslmechhandlers.add( saslMechHandler );
        }
    }

    
    /**
     * @return the extendedOps
     */
    public List<ExtendedOpHandlerBean> getExtendedOps()
    {
        return extendedophandlers;
    }

    
    /**
     * @param extendedOps the extendedOps to set
     */
    public void setExtendedOps( List<ExtendedOpHandlerBean> extendedOps )
    {
        this.extendedophandlers = extendedOps;
    }

    
    /**
     * @param extendedOps the extendedOps to add
     */
    public void addExtendedOps( ExtendedOpHandlerBean... extendedOps )
    {
        for ( ExtendedOpHandlerBean extendedOp : extendedOps )
        {   
            this.extendedophandlers.add( extendedOp );
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( tabs ).append( "LdapServer :\n" );
        sb.append( super.toString( tabs + "  " ) );
        sb.append( tabs ).append( "  max size limit : " ).append( maxsizelimit ).append( '\n' );
        sb.append( tabs ).append( "  max time limit : " ).append( maxtimelimit ).append( '\n' );
        sb.append( toString( tabs, "  certificate password", certificatepassword ) );
        sb.append( toString( tabs, "  keystore file", keystorefile ) );
        sb.append( toString( tabs, "  sasl principal", saslprincipal ) );
        sb.append( tabs ).append( "  sasl host : " ).append( saslhost ).append( '\n' );
        sb.append( toStringBoolean( tabs, "  confidentiality required", confidentialityrequired ) );
        sb.append( toStringBoolean( tabs, "  enable replication provider", enablereplprovider ) );
        
        if ( ( extendedophandlers != null ) && ( extendedophandlers.size() > 0 ) )
        {
            sb.append( tabs ).append( "  extended operation handlers :\n" );
            
            for ( ExtendedOpHandlerBean extendedOpHandler : extendedophandlers )
            {
                sb.append( extendedOpHandler.toString( tabs + "    " ) );
            }
        }
        
        if ( saslmechhandlers != null )
        {
            sb.append( tabs ).append( "  SASL mechanism handlers :\n" );
            
            for ( SaslMechanisHandlerBean saslMechHandler : saslmechhandlers )
            {
                sb.append( saslMechHandler.toString( tabs + "    " ) );
            }
        }
        
        if ( ( saslrealms != null ) && ( saslrealms.size() > 0 ) )
        {
            sb.append( tabs ).append( "  SASL realms :\n" );
            
            for ( String saslRealm : saslrealms )
            {
                sb.append( tabs ).append( "    " ).append( saslRealm ).append( "\n" );
            }
        }

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
