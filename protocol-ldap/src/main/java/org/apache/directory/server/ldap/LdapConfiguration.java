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
package org.apache.directory.server.ldap;


import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.directory.server.protocol.shared.ServiceConfiguration;
import org.apache.directory.server.protocol.shared.ServiceConfigurationException;


/**
 * Contains the configuration parameters for the LDAP protocol provider.
 *
 * @org.apache.xbean.XBean
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class LdapConfiguration extends ServiceConfiguration
{
    private static final long serialVersionUID = 3757127143811666817L;

    /** The default maximum size limit. */
    private static final int MAX_SIZE_LIMIT_DEFAULT = 100;

    /** The default maximum time limit. */
    private static final int MAX_TIME_LIMIT_DEFAULT = 10000;

    /** The default IP port. */
    private static final int IP_PORT_DEFAULT = 389;

    /** Whether to allow anonymous access. */
    private boolean allowAnonymousAccess = true; // allow by default

    /** The maximum size limit. */
    private int maxSizeLimit = MAX_SIZE_LIMIT_DEFAULT; // set to default value

    /** The maximum time limit. */
    private int maxTimeLimit = MAX_TIME_LIMIT_DEFAULT; // set to default value (milliseconds)

    /** Whether LDAPS is enabled. */
    private boolean enableLdaps;

    /** The path to the certificate file. */
    private File ldapsCertificateFile = new File( "server-work" + File.separator + "certificates" + File.separator
        + "server.cert" );

    /** The certificate password. */
    private String ldapsCertificatePassword = "changeit";

    /** The extended operation handlers. */
    private final Collection<ExtendedOperationHandler> extendedOperationHandlers = new ArrayList<ExtendedOperationHandler>();

    /** The supported authentication mechanisms. */
    private Set<String> supportedMechanisms;

    /** The name of this host, validated during SASL negotiation. */
    private String saslHost = "ldap.example.com";

    /** The service principal, used by GSSAPI. */
    private String saslPrincipal = "ldap/ldap.example.com@EXAMPLE.COM";

    /** The quality of protection (QoP), used by DIGEST-MD5 and GSSAPI. */
    private List<String> saslQop;

    /** The list of realms serviced by this host. */
    private List<String> saslRealms;


    /**
     * Creates a new instance of LdapConfiguration.
     */
    public LdapConfiguration()
    {
        super.setIpPort( IP_PORT_DEFAULT );
        super.setEnabled( true );

        supportedMechanisms = new HashSet<String>();
        supportedMechanisms.add( "SIMPLE" );
        supportedMechanisms.add( "CRAM-MD5" );
        supportedMechanisms.add( "DIGEST-MD5" );
        supportedMechanisms.add( "GSSAPI" );

        saslQop = new ArrayList<String>();
        saslQop.add( "auth" );
        saslQop.add( "auth-int" );
        saslQop.add( "auth-conf" );

        saslRealms = new ArrayList<String>();
        saslRealms.add( "example.com" );
    }


    /**
     * Returns <tt>true</tt> if LDAPS is enabled.
     * 
     * @return True if LDAPS is enabled.
     */
    public boolean isEnableLdaps()
    {
        return enableLdaps;
    }


    /**
     * Sets if LDAPS is enabled or not.
     * 
     * @param enableLdaps Whether LDAPS is enabled.
     */
    public void setEnableLdaps( boolean enableLdaps )
    {
        this.enableLdaps = enableLdaps;
    }


    /**
     * Returns the path of the X509 (or JKS) certificate file for LDAPS.
     * The default value is <tt>"&lt;WORKDIR&gt;/certificates/server.cert"</tt>.
     *  
     * @return The LDAPS certificate file.
     */
    public File getLdapsCertificateFile()
    {
        return ldapsCertificateFile;
    }


    /**
     * Sets the path of the SunX509 certificate file (either PKCS12 or JKS format)
     * for LDAPS.
     * 
     * @param ldapsCertificateFile The path to the SunX509 certificate.
     */
    public void setLdapsCertificateFile( File ldapsCertificateFile )
    {
        if ( ldapsCertificateFile == null )
        {
            throw new ServiceConfigurationException( "LdapsCertificateFile cannot be null." );
        }
        this.ldapsCertificateFile = ldapsCertificateFile;
    }


    /**
     * Returns the password which is used to load the the SunX509 certificate file
     * (either PKCS12 or JKS format).
     * The default value is <tt>"changeit"</tt>.  This is the same value with what
     * <a href="http://jakarta.apache.org/tomcat/">Apache Jakarta Tomcat</a> uses by
     * default.
     * 
     * @return The LDAPS certificate password.
     */
    public String getLdapsCertificatePassword()
    {
        return ldapsCertificatePassword;
    }


    /**
     * Sets the password which is used to load the LDAPS certificate file.
     * 
     * @param ldapsCertificatePassword The certificate password. 
     */
    public void setLdapsCertificatePassword( String ldapsCertificatePassword )
    {
        if ( ldapsCertificatePassword == null )
        {
            throw new ServiceConfigurationException( "LdapsCertificatePassword cannot be null." );
        }
        this.ldapsCertificatePassword = ldapsCertificatePassword;
    }


    /**
     * Returns <code>true</code> if anonymous access is allowed.
     * 
     * @return True if anonymous access is allowed.
     */
    public boolean isAllowAnonymousAccess()
    {
        return allowAnonymousAccess;
    }


    /**
     * Sets whether to allow anonymous access or not.
     * 
     * @param enableAnonymousAccess Set <code>true</code> to allow anonymous access.
     */
    public void setAllowAnonymousAccess( boolean enableAnonymousAccess )
    {
        this.allowAnonymousAccess = enableAnonymousAccess;
    }


    /**
     * Sets the maximum size limit.
     *
     * @param maxSizeLimit
     */
    public void setMaxSizeLimit( int maxSizeLimit )
    {
        this.maxSizeLimit = maxSizeLimit;
    }


    /**
     * Returns the maximum size limit.
     *
     * @return The maximum size limit.
     */
    public int getMaxSizeLimit()
    {
        return maxSizeLimit;
    }


    /**
     * Sets the maximum time limit.
     *
     * @param maxTimeLimit
     */
    public void setMaxTimeLimit( int maxTimeLimit )
    {
        this.maxTimeLimit = maxTimeLimit;
    }


    /**
     * Returns the maximum time limit.
     *
     * @return The maximum time limit.
     */
    public int getMaxTimeLimit()
    {
        return maxTimeLimit;
    }


    /**
     * Gets the {@link ExtendedOperationHandler}s.
     *
     * @return A collection of {@link ExtendedOperationHandler}s.
     */
    public Collection<ExtendedOperationHandler> getExtendedOperationHandlers()
    {
        return new ArrayList<ExtendedOperationHandler>( extendedOperationHandlers );
    }


    /**
     * Sets the {@link ExtendedOperationHandler}s.
     *
     * @org.apache.xbean.Property nestedType="org.apache.directory.server.ldap.ExtendedOperationHandler"
     *
     * @param handlers A collection of {@link ExtendedOperationHandler}s.
     */
    public void setExtendedOperationHandlers( Collection<ExtendedOperationHandler> handlers )
    {
        for ( Iterator i = handlers.iterator(); i.hasNext(); )
        {
            if ( !( i.next() instanceof ExtendedOperationHandler ) )
            {
                throw new IllegalArgumentException(
                    "The specified handler collection contains an element which is not an ExtendedOperationHandler." );
            }
        }

        this.extendedOperationHandlers.clear();
        this.extendedOperationHandlers.addAll( handlers );
    }


    /**
     * Returns the FQDN of this SASL host, validated during SASL negotiation.
     * 
     * @return The FQDN of this SASL host, validated during SASL negotiation.
     */
    public String getSaslHost()
    {
        return saslHost;
    }


    /**
     * Sets the FQDN of this SASL host, validated during SASL negotiation.
     * 
     * @param saslHost The FQDN of this SASL host, validated during SASL negotiation.
     */
    public void setSaslHost( String saslHost )
    {
        this.saslHost = saslHost;
    }


    /**
     * Returns the Kerberos principal name for this LDAP service, used by GSSAPI.
     * 
     * @return The Kerberos principal name for this LDAP service, used by GSSAPI.
     */
    public String getSaslPrincipal()
    {
        return saslPrincipal;
    }


    /**
     * Sets the Kerberos principal name for this LDAP service, used by GSSAPI.
     * 
     * @param saslPrincipal The Kerberos principal name for this LDAP service, used by GSSAPI.
     */
    public void setSaslPrincipal( String saslPrincipal )
    {
        this.saslPrincipal = saslPrincipal;
    }


    /**
     * Returns the desired quality-of-protection, used by DIGEST-MD5 and GSSAPI.
     * 
     * @return The desired quality-of-protection, used by DIGEST-MD5 and GSSAPI.
     */
    public List<String> getSaslQop()
    {
        return saslQop;
    }


    /**
     * Sets the desired quality-of-protection, used by DIGEST-MD5 and GSSAPI.
     * 
     * @org.apache.xbean.Property nestedType="java.lang.String"
     *
     * @param saslQop The desired quality-of-protection, used by DIGEST-MD5 and GSSAPI.
     */
    public void setSaslQop( List<String> saslQop )
    {
        this.saslQop = saslQop;
    }


    /**
     * Returns the realms serviced by this SASL host, used by DIGEST-MD5 and GSSAPI.
     * 
     * @return The realms serviced by this SASL host, used by DIGEST-MD5 and GSSAPI.
     */
    public List getSaslRealms()
    {
        return saslRealms;
    }


    /**
     * Sets the realms serviced by this SASL host, used by DIGEST-MD5 and GSSAPI.
     * 
     * @org.apache.xbean.Property nestedType="java.lang.String"
     *
     * @param saslRealms The realms serviced by this SASL host, used by DIGEST-MD5 and GSSAPI.
     */
    public void setSaslRealms( List<String> saslRealms )
    {
        this.saslRealms = saslRealms;
    }


    /**
     * Returns the list of supported authentication mechanisms.
     * 
     * @return The list of supported authentication mechanisms.
     */
    public Set<String> getSupportedMechanisms()
    {
        return supportedMechanisms;
    }


    /**
     * Sets the list of supported authentication mechanisms.
     * 
     * @org.apache.xbean.Property propertyEditor="ListEditor" nestedType="java.lang.String"
     *
     * @param supportedMechanisms The list of supported authentication mechanisms.
     */
    public void setSupportedMechanisms( Set<String> supportedMechanisms )
    {
        this.supportedMechanisms = supportedMechanisms;
    }
}
