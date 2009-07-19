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

package org.apache.directory.shared.ldap.client.api;

import java.security.SecureRandom;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;


/**
 * A class to hold the configuration for creating an LdapConnection.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class LdapConnectionConfig
{

    /** Define the default ports for LDAP and LDAPS */
    public static final int DEFAULT_LDAP_PORT = 389;

    public static final int DEFAULT_LDAPS_PORT = 636;

    /** The default host : localhost */
    public static final String DEFAULT_LDAP_HOST = "127.0.0.1";

    /** The LDAP version */
    public static int LDAP_V3 = 3;

    /** The default timeout for operation : 30 seconds */
    public static final long DEFAULT_TIMEOUT = 30000L;

    /** the default protocol used for creating SSL context */
    public static final String DEFAULT_SSL_PROTOCOL = "TLS";
    
    // --- private members ----
    
    /** A flag indicating if we are using SSL or not, default value is false */
    private boolean useSsl = false;

    /** The selected LDAP port */
    private int ldapPort;

    /** the remote LDAP host */
    private String ldapHost;

    /** a valid DN to authenticate the user */
    private String name;

    /** user's credentials ( current implementation supports password only); it must be a non-null value */
    private byte[] credentials;

    /** an array of key managers, if set, will be used while initializing the SSL context */
    private KeyManager[] keyManagers;
    
    /** an instance of SecureRandom, if set, will be used while initializing the SSL context */
    private SecureRandom secureRandom;
    
    /** an array of certificate trust managers, if set, will be used while initializing the SSL context */
    private TrustManager[] trustManagers;

    /** name of the protocol used for creating SSL context, default value is "TLS" */
    private String sslProtocol = DEFAULT_SSL_PROTOCOL;
    
    public boolean isUseSsl()
    {
        return useSsl;
    }


    public void setUseSsl( boolean useSsl )
    {
        this.useSsl = useSsl;
    }


    public int getLdapPort()
    {
        return ldapPort;
    }


    public void setLdapPort( int ldapPort )
    {
        this.ldapPort = ldapPort;
    }


    public String getLdapHost()
    {
        return ldapHost;
    }


    public void setLdapHost( String ldapHost )
    {
        this.ldapHost = ldapHost;
    }


    public String getName()
    {
        return name;
    }


    public void setName( String name )
    {
        this.name = name;
    }


    public byte[] getCredentials()
    {
        return credentials;
    }


    public void setCredentials( byte[] credentials )
    {
        this.credentials = credentials;
    }


    public int getDefaultLdapPort()
    {
        return DEFAULT_LDAP_PORT;
    }


    public int getDefaultLdapsPort()
    {
        return DEFAULT_LDAPS_PORT;
    }


    public String getDefaultLdapHost()
    {
        return DEFAULT_LDAP_HOST;
    }


    public long getDefaultTimeout()
    {
        return DEFAULT_TIMEOUT;
    }


    public int getSupportedLdapVersion()
    {
        return LDAP_V3;
    }


    public TrustManager[] getTrustManagers()
    {
        return trustManagers;
    }


    public void setTrustManagers( TrustManager[] trustManagers )
    {
        this.trustManagers = trustManagers;
    }


    public String getSslProtocol()
    {
        return sslProtocol;
    }


    public void setSslProtocol( String sslProtocol )
    {
        this.sslProtocol = sslProtocol;
    }


    public KeyManager[] getKeyManagers()
    {
        return keyManagers;
    }


    public void setKeyManagers( KeyManager[] keyManagers )
    {
        this.keyManagers = keyManagers;
    }


    public SecureRandom getSecureRandom()
    {
        return secureRandom;
    }


    public void setSecureRandom( SecureRandom secureRandom )
    {
        this.secureRandom = secureRandom;
    }
    
}
