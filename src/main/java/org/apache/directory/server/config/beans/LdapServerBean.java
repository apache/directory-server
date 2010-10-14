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

import java.util.HashSet;
import java.util.Set;


/**
 * A class used to store the LdapServer configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapServerBean extends CatalogBasedServerBean
{
    /** */
    private boolean ldapServerConfidentialityRequired;
    
    /** The maximum number of entries returned by the server */
    private int ldapServerMaxSizeLimit;
    
    /** The maximum time to execute a request on the server */
    private int ldapServerMaxTimeLimit;
    
    /** The SASL host */
    private String ldapServerSaslHost;
    
    /** The SASL  principal */
    private String ldapServerSaslPrincipal;
    
    /** The SASL realms */
    private Set<String> ldapServerSaslRealms = new HashSet<String>();;
    
    /** The keystore file */
    private String ldapServerKeystoreFile;
    
    /** The certificate password */
    private String ldapServerCertificatePassword;
    
    /** The ReplicationProvider configuration */
    //private ReplicationProviderBean replProviderImpl;
    
    /** tells if the replication is enabled */
    private boolean enableReplProvider; 
    
    /** The list of supported mechanisms */
    private Set<LdapServerSaslMechanisHandlerBean> saslMechHandlers = new HashSet<LdapServerSaslMechanisHandlerBean>();
    
    /** The list of supported extedned operations */
    private Set<LdapServerExtendedOpHandlerBean> extendedOps = new HashSet<LdapServerExtendedOpHandlerBean>();

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
        return ldapServerConfidentialityRequired;
    }

    
    /**
     * @param ldapServerConfidentialityRequired the ldapServerConfidentialityRequired to set
     */
    public void setLdapServerConfidentialityRequired( boolean ldapServerConfidentialityRequired )
    {
        this.ldapServerConfidentialityRequired = ldapServerConfidentialityRequired;
    }

    
    /**
     * @return the ldapServerMaxSizeLimit
     */
    public int getLdapServerMaxSizeLimit()
    {
        return ldapServerMaxSizeLimit;
    }

    
    /**
     * @param ldapServerMaxSizeLimit the ldapServerMaxSizeLimit to set
     */
    public void setLdapServerMaxSizeLimit( int ldapServerMaxSizeLimit )
    {
        this.ldapServerMaxSizeLimit = ldapServerMaxSizeLimit;
    }

    
    /**
     * @return the ldapServerMaxTimeLimit
     */
    public int getLdapServerMaxTimeLimit()
    {
        return ldapServerMaxTimeLimit;
    }

    
    /**
     * @param ldapServerMaxTimeLimit the ldapServerMaxTimeLimit to set
     */
    public void setLdapServerMaxTimeLimit( int ldapServerMaxTimeLimit )
    {
        this.ldapServerMaxTimeLimit = ldapServerMaxTimeLimit;
    }

    
    /**
     * @return the ldapServerSaslHost
     */
    public String getLdapServerSaslHost()
    {
        return ldapServerSaslHost;
    }

    
    /**
     * @param ldapServerSaslHost the ldapServerSaslHost to set
     */
    public void setLdapServerSaslHost( String ldapServerSaslHost )
    {
        this.ldapServerSaslHost = ldapServerSaslHost;
    }

    
    /**
     * @return the ldapServerSaslPrincipal
     */
    public String getLdapServerSaslPrincipal()
    {
        return ldapServerSaslPrincipal;
    }

    
    /**
     * @param ldapServerSaslPrincipal the ldapServerSaslPrincipal to set
     */
    public void setLdapServerSaslPrincipal( String ldapServerSaslPrincipal )
    {
        this.ldapServerSaslPrincipal = ldapServerSaslPrincipal;
    }

    
    /**
     * @return the ldapServerSaslRealms
     */
    public Set<String> getLdapServerSaslRealms()
    {
        return ldapServerSaslRealms;
    }

    
    /**
     * @param ldapServerSaslRealms the ldapServerSaslRealms to set
     */
    public void setLdapServerSaslRealms( Set<String> ldapServerSaslRealms )
    {
        this.ldapServerSaslRealms = ldapServerSaslRealms;
    }

    
    /**
     * @param ldapServerSaslRealms the ldapServerSaslRealms to add
     */
    public void addLdapServerSaslRealms( String... ldapServerSaslRealms )
    {
        for ( String saslRealm : ldapServerSaslRealms )
        {
            this.ldapServerSaslRealms.add( saslRealm );
        }
    }

    
    /**
     * @return the ldapServerKeystoreFile
     */
    public String getLdapServerKeystoreFile()
    {
        return ldapServerKeystoreFile;
    }

    
    /**
     * @param ldapServerKeystoreFile the ldapServerKeystoreFile to set
     */
    public void setLdapServerKeystoreFile( String ldapServerKeystoreFile )
    {
        this.ldapServerKeystoreFile = ldapServerKeystoreFile;
    }

    
    /**
     * @return the ldapServerCertificatePassword
     */
    public String getLdapServerCertificatePassword()
    {
        return ldapServerCertificatePassword;
    }

    
    /**
     * @param ldapServerCertificatePassword the ldapServerCertificatePassword to set
     */
    public void setLdapServerCertificatePassword( String ldapServerCertificatePassword )
    {
        this.ldapServerCertificatePassword = ldapServerCertificatePassword;
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
        return enableReplProvider;
    }

    
    /**
     * @param enableReplProvider the enableReplProvider to set
     */
    public void setEnableReplProvider( boolean enableReplProvider )
    {
        this.enableReplProvider = enableReplProvider;
    }

    
    /**
     * @return the saslMechHandlers
     */
    public Set<LdapServerSaslMechanisHandlerBean> getSaslMechHandlers()
    {
        return saslMechHandlers;
    }

    
    /**
     * @param saslMechHandlers the saslMechHandlers to set
     */
    public void setSaslMechHandlers( Set<LdapServerSaslMechanisHandlerBean> saslMechHandlers )
    {
        this.saslMechHandlers = saslMechHandlers;
    }

    
    /**
     * @param saslMechHandlers the saslMechHandlers to add
     */
    public void setSaslMechHandlers( LdapServerSaslMechanisHandlerBean... saslMechHandlers )
    {
        for ( LdapServerSaslMechanisHandlerBean saslMechHandler : saslMechHandlers )
        {
            this.saslMechHandlers.add( saslMechHandler );
        }
    }

    
    /**
     * @return the extendedOps
     */
    public Set<LdapServerExtendedOpHandlerBean> getExtendedOps()
    {
        return extendedOps;
    }

    
    /**
     * @param extendedOps the extendedOps to set
     */
    public void setExtendedOps( Set<LdapServerExtendedOpHandlerBean> extendedOps )
    {
        this.extendedOps = extendedOps;
    }

    
    /**
     * @param extendedOps the extendedOps to add
     */
    public void addExtendedOps( LdapServerExtendedOpHandlerBean... extendedOps )
    {
        for ( LdapServerExtendedOpHandlerBean extendedOp : extendedOps )
        {   
            this.extendedOps.add( extendedOp );
        }
    }
}
