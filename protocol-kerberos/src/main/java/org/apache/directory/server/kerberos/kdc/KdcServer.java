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
package org.apache.directory.server.kerberos.kdc;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.protocol.KerberosProtocolHandler;
import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.store.JndiPrincipalStoreImpl;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.directory.server.protocol.shared.DirectoryBackedService;
import org.apache.mina.transport.socket.nio.DatagramAcceptorConfig;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;


/**
 * Contains the configuration parameters for the Kerberos protocol provider.
 *
 * @org.apache.xbean.XBean
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class KdcServer extends DirectoryBackedService
{
    private static final long serialVersionUID = 522567370475574165L;

    /** The default kdc port */
    private static final int DEFAULT_IP_PORT = 88;

    /** The default kdc search base DN */
    public static final String DEFAULT_SEARCH_BASEDN = "ou=users,dc=example,dc=com";

    /** The default kdc service pid */
    private static final String DEFAULT_PID = "org.apache.directory.server.kerberos";

    /** The default kdc service name */
    private static final String DEFAULT_NAME = "ApacheDS Kerberos Service";

    /** The default kdc service principal */
    private static final String DEFAULT_PRINCIPAL = "krbtgt/EXAMPLE.COM@EXAMPLE.COM";

    /** The default kdc realm */
    private static final String DEFAULT_REALM = "EXAMPLE.COM";

    /** The default allowable clockskew */
    private static final long DEFAULT_ALLOWABLE_CLOCKSKEW = 5 * 60000;

    /** The default encryption types */
    private static final String[] DEFAULT_ENCRYPTION_TYPES = new String[]
        { "des-cbc-md5" };

    /** The default for allowing empty addresses */
    private static final boolean DEFAULT_EMPTY_ADDRESSES_ALLOWED = true;

    /** The default for requiring encrypted timestamps */
    private static final boolean DEFAULT_PA_ENC_TIMESTAMP_REQUIRED = true;

    /** The default for the maximum ticket lifetime */
    private static final int DEFAULT_TGS_MAXIMUM_TICKET_LIFETIME = 60000 * 1440;

    /** The default for the maximum renewable lifetime */
    private static final int DEFAULT_TGS_MAXIMUM_RENEWABLE_LIFETIME = 60000 * 10080;

    /** The default for allowing forwardable tickets */
    private static final boolean DEFAULT_TGS_FORWARDABLE_ALLOWED = true;

    /** The default for allowing proxiable tickets */
    private static final boolean DEFAULT_TGS_PROXIABLE_ALLOWED = true;

    /** The default for allowing postdated tickets */
    private static final boolean DEFAULT_TGS_POSTDATED_ALLOWED = true;

    /** The default for allowing renewable tickets */
    private static final boolean DEFAULT_TGS_RENEWABLE_ALLOWED = true;

    /** The default for verifying the body checksum */
    private static final boolean DEFAULT_VERIFY_BODY_CHECKSUM = true;

    /** The encryption types. */
    private Set<EncryptionType> encryptionTypes;

    /** The primary realm */
    private String primaryRealm = DEFAULT_REALM;

    /** The service principal name. */
    private String servicePrincipal = DEFAULT_PRINCIPAL;

    /** The allowable clock skew. */
    private long allowableClockSkew = DEFAULT_ALLOWABLE_CLOCKSKEW;

    /** Whether pre-authentication by encrypted timestamp is required. */
    private boolean isPaEncTimestampRequired = DEFAULT_PA_ENC_TIMESTAMP_REQUIRED;

    /** The maximum ticket lifetime. */
    private long maximumTicketLifetime = DEFAULT_TGS_MAXIMUM_TICKET_LIFETIME;

    /** The maximum renewable lifetime. */
    private long maximumRenewableLifetime = DEFAULT_TGS_MAXIMUM_RENEWABLE_LIFETIME;

    /** Whether empty addresses are allowed. */
    private boolean isEmptyAddressesAllowed = DEFAULT_EMPTY_ADDRESSES_ALLOWED;

    /** Whether forwardable addresses are allowed. */
    private boolean isForwardableAllowed = DEFAULT_TGS_FORWARDABLE_ALLOWED;

    /** Whether proxiable addresses are allowed. */
    private boolean isProxiableAllowed = DEFAULT_TGS_PROXIABLE_ALLOWED;

    /** Whether postdated tickets are allowed. */
    private boolean isPostdatedAllowed = DEFAULT_TGS_POSTDATED_ALLOWED;

    /** Whether renewable tickets are allowed. */
    private boolean isRenewableAllowed = DEFAULT_TGS_RENEWABLE_ALLOWED;

    /** Whether to verify the body checksum. */
    private boolean isBodyChecksumVerified = DEFAULT_VERIFY_BODY_CHECKSUM;


    /**
     * Creates a new instance of KdcConfiguration.
     */
    public KdcServer()
    {
        super.setServiceName( DEFAULT_NAME );
        super.setIpPort( DEFAULT_IP_PORT );
        super.setServiceId( DEFAULT_PID );
        super.setSearchBaseDn( DEFAULT_SEARCH_BASEDN );

        prepareEncryptionTypes();
    }


    /**
     * Returns the allowable clock skew.
     *
     * @return The allowable clock skew.
     */
    public long getAllowableClockSkew()
    {
        return allowableClockSkew;
    }


    /**
     * @return the isEmptyAddressesAllowed
     */
    public boolean isEmptyAddressesAllowed()
    {
        return isEmptyAddressesAllowed;
    }


    /**
     * @return the isForwardableAllowed
     */
    public boolean isForwardableAllowed()
    {
        return isForwardableAllowed;
    }


    /**
     * @return the isPostdatedAllowed
     */
    public boolean isPostdatedAllowed()
    {
        return isPostdatedAllowed;
    }


    /**
     * @return the isProxiableAllowed
     */
    public boolean isProxiableAllowed()
    {
        return isProxiableAllowed;
    }


    /**
     * @return the isRenewableAllowed
     */
    public boolean isRenewableAllowed()
    {
        return isRenewableAllowed;
    }


    /**
     * @return the maximumRenewableLifetime
     */
    public long getMaximumRenewableLifetime()
    {
        return maximumRenewableLifetime;
    }


    /**
     * @return the maximumTicketLifetime
     */
    public long getMaximumTicketLifetime()
    {
        return maximumTicketLifetime;
    }


    /**
     * @param allowableClockSkew the allowableClockSkew to set
     */
    public void setAllowableClockSkew( long allowableClockSkew )
    {
        this.allowableClockSkew = allowableClockSkew;
    }


    /**
     * Initialize the encryptionTypes set
     * 
     * @param encryptionTypes the encryptionTypes to set
     */
    public void setEncryptionTypes( EncryptionType[] encryptionTypes )
    {
        if ( encryptionTypes != null )
        {
            this.encryptionTypes.clear();
            
            for ( EncryptionType encryptionType:encryptionTypes )
            {
                this.encryptionTypes.add( encryptionType );
            }
        }
    }


    /**
     * Initialize the encryptionTypes set
     * 
     * @param encryptionTypes the encryptionTypes to set
     */
    public void setEncryptionTypes( Set<EncryptionType> encryptionTypes )
    {
        this.encryptionTypes = encryptionTypes;
    }


    /**
     * @param isEmptyAddressesAllowed the isEmptyAddressesAllowed to set
     */
    public void setEmptyAddressesAllowed( boolean isEmptyAddressesAllowed )
    {
        this.isEmptyAddressesAllowed = isEmptyAddressesAllowed;
    }


    /**
     * @param isForwardableAllowed the isForwardableAllowed to set
     */
    public void setForwardableAllowed( boolean isForwardableAllowed )
    {
        this.isForwardableAllowed = isForwardableAllowed;
    }


    /**
     * @param isPaEncTimestampRequired the isPaEncTimestampRequired to set
     */
    public void setPaEncTimestampRequired( boolean isPaEncTimestampRequired )
    {
        this.isPaEncTimestampRequired = isPaEncTimestampRequired;
    }


    /**
     * @param isPostdatedAllowed the isPostdatedAllowed to set
     */
    public void setPostdatedAllowed( boolean isPostdatedAllowed )
    {
        this.isPostdatedAllowed = isPostdatedAllowed;
    }


    /**
     * @param isProxiableAllowed the isProxiableAllowed to set
     */
    public void setProxiableAllowed( boolean isProxiableAllowed )
    {
        this.isProxiableAllowed = isProxiableAllowed;
    }


    /**
     * @param isRenewableAllowed the isRenewableAllowed to set
     */
    public void setRenewableAllowed( boolean isRenewableAllowed )
    {
        this.isRenewableAllowed = isRenewableAllowed;
    }


    /**
     * @param kdcPrincipal the kdcPrincipal to set
     */
    public void setKdcPrincipal( String kdcPrincipal )
    {
        this.servicePrincipal = kdcPrincipal;
    }


    /**
     * @param maximumRenewableLifetime the maximumRenewableLifetime to set
     */
    public void setMaximumRenewableLifetime( long maximumRenewableLifetime )
    {
        this.maximumRenewableLifetime = maximumRenewableLifetime;
    }


    /**
     * @param maximumTicketLifetime the maximumTicketLifetime to set
     */
    public void setMaximumTicketLifetime( long maximumTicketLifetime )
    {
        this.maximumTicketLifetime = maximumTicketLifetime;
    }


    /**
     * @param primaryRealm the primaryRealm to set
     */
    public void setPrimaryRealm( String primaryRealm )
    {
        this.primaryRealm = primaryRealm;
    }


    /**
     * Returns the primary realm.
     *
     * @return The primary realm.
     */
    public String getPrimaryRealm()
    {
        return primaryRealm;
    }


    /**
     * Returns the service principal for this KDC service.
     *
     * @return The service principal for this KDC service.
     */
    public KerberosPrincipal getServicePrincipal()
    {
        return new KerberosPrincipal( servicePrincipal );
    }


    /**
     * Returns the encryption types.
     *
     * @return The encryption types.
     */
    public Set<EncryptionType> getEncryptionTypes()
    {
        return encryptionTypes;
    }


    /**
     * Returns whether pre-authentication by encrypted timestamp is required.
     *
     * @return Whether pre-authentication by encrypted timestamp is required.
     */
    public boolean isPaEncTimestampRequired()
    {
        return isPaEncTimestampRequired;
    }


    /**
     * @return the isBodyChecksumVerified
     */
    public boolean isBodyChecksumVerified()
    {
        return isBodyChecksumVerified;
    }


    /**
     * @param isBodyChecksumVerified the isBodyChecksumVerified to set
     */
    public void setBodyChecksumVerified( boolean isBodyChecksumVerified )
    {
        this.isBodyChecksumVerified = isBodyChecksumVerified;
    }


    /**
     * @org.apache.xbean.InitMethod
     * @throws IOException if we cannot bind to the sockets
     */
    public void start() throws IOException
    {
        PrincipalStore store = new JndiPrincipalStoreImpl( getSearchBaseDn(),
                getSearchBaseDn(), getDirectoryService() );

        if ( getDatagramAcceptor() != null )
        {
            DatagramAcceptorConfig udpConfig = new DatagramAcceptorConfig();
            getDatagramAcceptor().bind( new InetSocketAddress( getIpPort() ), new KerberosProtocolHandler( this, store ), udpConfig );
        }

        if ( getSocketAcceptor() != null )
        {
            SocketAcceptorConfig tcpConfig = new SocketAcceptorConfig();
            tcpConfig.setDisconnectOnUnbind( false );
            tcpConfig.setReuseAddress( true );
            getSocketAcceptor().bind( new InetSocketAddress( getIpPort() ), new KerberosProtocolHandler( this, store ), tcpConfig );
        }
    }

    
    /**
     * @org.apache.xbean.DestroyMethod
     */
    public void stop()
    {
        if ( getDatagramAcceptor() != null )
        {
            getDatagramAcceptor().unbind( new InetSocketAddress( getIpPort() ));
        }
        if ( getSocketAcceptor() != null )
        {
            getSocketAcceptor().unbind( new InetSocketAddress( getIpPort() ));
        }
    }


    /**
     * Construct an HashSet containing the default encryption types
     */
    private void prepareEncryptionTypes()
    {
        String[] encryptionTypeStrings = DEFAULT_ENCRYPTION_TYPES;

        encryptionTypes = new HashSet<EncryptionType>();

        for ( String enc : encryptionTypeStrings )
        {
            for ( EncryptionType type : EncryptionType.getEncryptionTypes() )
            {
                if ( type.getName().equalsIgnoreCase( enc ) )
                {
                    encryptionTypes.add( type );
                }
            }
        }
    }
}
