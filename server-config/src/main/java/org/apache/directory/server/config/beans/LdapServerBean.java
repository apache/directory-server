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


/**
 * A class used to store the LdapServer configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapServerBean extends DSBasedServerBean
{
    /** */
    @ConfigurationElement(attributeType = "ads-confidentialityRequired")
    private boolean confidentialityRequired;

    /** The maximum number of entries returned by the server */
    @ConfigurationElement(attributeType = "ads-maxSizeLimit")
    private int maxSizeLimit;

    /** The maximum time to execute a request on the server */
    @ConfigurationElement(attributeType = "ads-maxTimeLimit")
    private int maxTimeLimit;

    /** The SASL host */
    @ConfigurationElement(attributeType = "ads-saslHost")
    private String saslHost;

    /** The SASL  principal */
    @ConfigurationElement(attributeType = "ads-saslPrincipal")
    private String saslPrincipal;

    /** The SASL realms */
    @ConfigurationElement(attributeType = "ads-saslRealms")
    private List<String> saslRealms = new ArrayList<String>();

    /** The keystore file */
    @ConfigurationElement(attributeType = "ads-keystoreFile")
    private String keystoreFile;

    /** The certificate password */
    @ConfigurationElement(attributeType = "ads-certificatePassword")
    private String certificatePassword;

    /** the replication request handler, server will be in replication provider/master mode if a valid FQCN is given */
    @ConfigurationElement(attributeType = "ads-replReqHandler")
    private String replReqHandler;

    /** The replication consumer Bean */
    @ConfigurationElement(attributeType = "ads-replConsumers", container = "replConsumers")
    private List<ReplConsumerBean> replConsumers = new ArrayList<ReplConsumerBean>();

    /** The list of supported mechanisms */
    @ConfigurationElement(attributeType = "ads-saslMechHandlers", container = "saslMechHandlers")
    private List<SaslMechHandlerBean> saslMechHandlers = new ArrayList<SaslMechHandlerBean>();

    /** The list of supported extended operation handlers */
    @ConfigurationElement(attributeType = "ads-extendedOpHandlers", container = "extendedOpHandlers")
    private List<ExtendedOpHandlerBean> extendedOpHandlers = new ArrayList<ExtendedOpHandlerBean>();
    

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
        return confidentialityRequired;
    }


    /**
     * @param ldapServerConfidentialityRequired the ldapServerConfidentialityRequired to set
     */
    public void setLdapServerConfidentialityRequired( boolean ldapServerConfidentialityRequired )
    {
        this.confidentialityRequired = ldapServerConfidentialityRequired;
    }


    /**
     * @return the ldapServerMaxSizeLimit
     */
    public int getLdapServerMaxSizeLimit()
    {
        return maxSizeLimit;
    }


    /**
     * @param ldapServerMaxSizeLimit the ldapServerMaxSizeLimit to set
     */
    public void setLdapServerMaxSizeLimit( int ldapServerMaxSizeLimit )
    {
        this.maxSizeLimit = ldapServerMaxSizeLimit;
    }


    /**
     * @return the ldapServerMaxTimeLimit
     */
    public int getLdapServerMaxTimeLimit()
    {
        return maxTimeLimit;
    }


    /**
     * @param ldapServerMaxTimeLimit the ldapServerMaxTimeLimit to set
     */
    public void setLdapServerMaxTimeLimit( int ldapServerMaxTimeLimit )
    {
        this.maxTimeLimit = ldapServerMaxTimeLimit;
    }


    /**
     * @return the ldapServerSaslHost
     */
    public String getLdapServerSaslHost()
    {
        return saslHost;
    }


    /**
     * @param ldapServerSaslHost the ldapServerSaslHost to set
     */
    public void setLdapServerSaslHost( String ldapServerSaslHost )
    {
        this.saslHost = ldapServerSaslHost;
    }


    /**
     * @return the ldapServerSaslPrincipal
     */
    public String getLdapServerSaslPrincipal()
    {
        return saslPrincipal;
    }


    /**
     * @param ldapServerSaslPrincipal the ldapServerSaslPrincipal to set
     */
    public void setLdapServerSaslPrincipal( String ldapServerSaslPrincipal )
    {
        this.saslPrincipal = ldapServerSaslPrincipal;
    }


    /**
     * @return the ldapServerSaslRealms
     */
    public List<String> getLdapServerSaslRealms()
    {
        return saslRealms;
    }


    /**
     * @param ldapServerSaslRealms the ldapServerSaslRealms to set
     */
    public void setLdapServerSaslRealms( List<String> ldapServerSaslRealms )
    {
        this.saslRealms = ldapServerSaslRealms;
    }


    /**
     * @param ldapServerSaslRealms the ldapServerSaslRealms to add
     */
    public void addSaslRealms( String... ldapServerSaslRealms )
    {
        for ( String saslRealm : ldapServerSaslRealms )
        {
            this.saslRealms.add( saslRealm );
        }
    }


    /**
     * @return the ldapServerKeystoreFile
     */
    public String getLdapServerKeystoreFile()
    {
        return keystoreFile;
    }


    /**
     * @param ldapServerKeystoreFile the ldapServerKeystoreFile to set
     */
    public void setLdapServerKeystoreFile( String ldapServerKeystoreFile )
    {
        this.keystoreFile = ldapServerKeystoreFile;
    }


    /**
     * @return the ldapServerCertificatePassword
     */
    public String getLdapServerCertificatePassword()
    {
        return certificatePassword;
    }


    /**
     * @param ldapServerCertificatePassword the ldapServerCertificatePassword to set
     */
    public void setLdapServerCertificatePassword( String ldapServerCertificatePassword )
    {
        this.certificatePassword = ldapServerCertificatePassword;
    }


    /**
     * @return the replReqHandler
     */
    public String getReplReqHandler()
    {
        return replReqHandler;
    }


    /**
     * @param replReqHandler the replReqHandler to set
     */
    public void setReplReqHandler( String replReqHandler )
    {
        this.replReqHandler = replReqHandler;
    }


    /**
     * @return the saslMechHandlers
     */
    public List<SaslMechHandlerBean> getSaslMechHandlers()
    {
        return saslMechHandlers;
    }


    /**
     * @param saslMechHandlers the saslMechHandlers to set
     */
    public void setSaslMechHandlers( List<SaslMechHandlerBean> saslMechHandlers )
    {
        this.saslMechHandlers = saslMechHandlers;
    }


    /**
     * @param saslMechHandlers the saslMechHandlers to add
     */
    public void setSaslMechHandlers( SaslMechHandlerBean... saslMechHandlers )
    {
        for ( SaslMechHandlerBean saslMechHandler : saslMechHandlers )
        {
            this.saslMechHandlers.add( saslMechHandler );
        }
    }


    /**
     * @return the extendedOps
     */
    public List<ExtendedOpHandlerBean> getExtendedOps()
    {
        return extendedOpHandlers;
    }


    
    /**
     * @param extendedOps the extendedOps to set
     */
    public void setExtendedOps( List<ExtendedOpHandlerBean> extendedOps )
    {
        this.extendedOpHandlers = extendedOps;
    }


    /**
     * @param extendedOps the extendedOps to add
     */
    public void addExtendedOps( ExtendedOpHandlerBean... extendedOps )
    {
        for ( ExtendedOpHandlerBean extendedOp : extendedOps )
        {
            this.extendedOpHandlers.add( extendedOp );
        }
    }


    /**
     * @return the Replication Consumer Bean
     */
    public List<ReplConsumerBean> getReplConsumers()
    {
        return replConsumers;
    }


    /**
     * @param replConsumers the Replication Consumer Bean to set
     */
    public void setReplConsumer( List<ReplConsumerBean> replConsumers )
    {
        this.replConsumers = replConsumers;
    }


    /**
     * @param replConsumers the Replication Consumer Bean to set
     */
    public void addReplConsumers( ReplConsumerBean... replConsumers )
    {
        for( ReplConsumerBean bean : replConsumers )
        {
            this.replConsumers.add( bean );
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
        sb.append( tabs ).append( "  max size limit : " ).append( maxSizeLimit ).append( '\n' );
        sb.append( tabs ).append( "  max time limit : " ).append( maxTimeLimit ).append( '\n' );
        sb.append( toString( tabs, "  certificate password", certificatePassword ) );
        sb.append( toString( tabs, "  keystore file", keystoreFile ) );
        sb.append( toString( tabs, "  sasl principal", saslPrincipal ) );
        sb.append( tabs ).append( "  sasl host : " ).append( saslHost ).append( '\n' );
        sb.append( toString( tabs, "  confidentiality required", confidentialityRequired ) );
        sb.append( toString( tabs, "  enable replication provider", replReqHandler ) );

        if ( ( extendedOpHandlers != null ) && ( extendedOpHandlers.size() > 0 ) )
        {
            sb.append( tabs ).append( "  extended operation handlers :\n" );

            for ( ExtendedOpHandlerBean extendedOpHandler : extendedOpHandlers )
            {
                sb.append( extendedOpHandler.toString( tabs + "    " ) );
            }
        }

        if ( saslMechHandlers != null )
        {
            sb.append( tabs ).append( "  SASL mechanism handlers :\n" );

            for ( SaslMechHandlerBean saslMechHandler : saslMechHandlers )
            {
                sb.append( saslMechHandler.toString( tabs + "    " ) );
            }
        }

        if ( ( saslRealms != null ) && ( saslRealms.size() > 0 ) )
        {
            sb.append( tabs ).append( "  SASL realms :\n" );

            for ( String saslRealm : saslRealms )
            {
                sb.append( tabs ).append( "    " ).append( saslRealm ).append( "\n" );
            }
        }

        if ( ( replConsumers != null ) && ( replConsumers.size() > 0 ) )
        {
            sb.append( tabs ).append( "  replication consumers :\n" );

            for ( ReplConsumerBean replConsumer : replConsumers )
            {
                sb.append( replConsumer.toString( tabs + "    " ) );
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
