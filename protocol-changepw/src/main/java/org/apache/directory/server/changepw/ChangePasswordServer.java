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
package org.apache.directory.server.changepw;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.changepw.protocol.ChangePasswordProtocolHandler;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.store.JndiPrincipalStoreImpl;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.directory.server.protocol.shared.ServiceConfiguration;
import org.apache.mina.transport.socket.nio.DatagramAcceptor;
import org.apache.mina.transport.socket.nio.DatagramAcceptorConfig;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;


/**
 * Contains the configuration parameters for the Change Password protocol provider.
 *
 * @org.apache.xbean.XBean
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ChangePasswordServer extends ServiceConfiguration
{
    private static final long serialVersionUID = 3509208713288140629L;

    /** The default change password principal name. */
    private static final String SERVICE_PRINCIPAL_DEFAULT = "kadmin/changepw@EXAMPLE.COM";

    /** The default change password base DN. */
    public static final String SEARCH_BASEDN_DEFAULT = "ou=users,dc=example,dc=com";

    /** The default change password realm. */
    private static final String REALM_DEFAULT = "EXAMPLE.COM";

    /** The default change password port. */
    private static final int IP_PORT_DEFAULT = 464;

    /** The default encryption types. */
    public static final String[] ENCRYPTION_TYPES_DEFAULT = new String[]
        { "des-cbc-md5" };

    /** The default changepw buffer size. */
    private static final long DEFAULT_ALLOWABLE_CLOCKSKEW = 5 * ServiceConfiguration.MINUTE;

    /** The default empty addresses. */
    private static final boolean DEFAULT_EMPTY_ADDRESSES_ALLOWED = true;

    /** The default change password password policy for password length. */
    public static final int DEFAULT_PASSWORD_LENGTH = 6;

    /** The default change password password policy for category count. */
    public static final int DEFAULT_CATEGORY_COUNT = 3;

    /** The default change password password policy for token size. */
    public static final int DEFAULT_TOKEN_SIZE = 3;

    /** The default service PID. */
    private static final String SERVICE_PID_DEFAULT = "org.apache.directory.server.changepw";

    /** The default service name. */
    private static final String SERVICE_NAME_DEFAULT = "ApacheDS Change Password Service";

    /** The encryption types. */
    private EncryptionType[] encryptionTypes;

    /** The primary realm. */
    private String primaryRealm = REALM_DEFAULT;

    /** The service principal name. */
    private String servicePrincipal = SERVICE_PRINCIPAL_DEFAULT;

    /** The allowable clock skew. */
    private long allowableClockSkew = DEFAULT_ALLOWABLE_CLOCKSKEW;

    /** Whether empty addresses are allowed. */
    private boolean isEmptyAddressesAllowed = DEFAULT_EMPTY_ADDRESSES_ALLOWED;

    /** The policy for password length. */
    private int policyPasswordLength;

    /** The policy for category count. */
    private int policyCategoryCount;

    /** The policy for token size. */
    private int policyTokenSize;

    private final DirectoryService directoryService;

    private final DatagramAcceptor datagramAcceptor;
    private final SocketAcceptor socketAcceptor;


    /**
     * Creates a new instance of ChangePasswordConfiguration.
     */
    public ChangePasswordServer( DatagramAcceptor datagramAcceptor, SocketAcceptor socketAcceptor, DirectoryService directoryService )
    {
        this.datagramAcceptor = datagramAcceptor;
        this.socketAcceptor = socketAcceptor;
        this.directoryService = directoryService;

        super.setServiceName( SERVICE_NAME_DEFAULT );
        super.setIpPort( IP_PORT_DEFAULT );
        super.setServicePid( SERVICE_PID_DEFAULT );
        super.setSearchBaseDn( SEARCH_BASEDN_DEFAULT );

        prepareEncryptionTypes();
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
     * @param primaryRealm The primaryRealm to set.
     */
    public void setPrimaryRealm( String primaryRealm )
    {
        this.primaryRealm = primaryRealm;
    }


    /**
     * Returns the encryption types.
     *
     * @return The encryption types.
     */
    public EncryptionType[] getEncryptionTypes()
    {
        return encryptionTypes;
    }


    /**
     * @param encryptionTypes The encryptionTypes to set.
     */
    public void setEncryptionTypes( EncryptionType[] encryptionTypes )
    {
        this.encryptionTypes = encryptionTypes;
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
     * @param allowableClockSkew The allowableClockSkew to set.
     */
    public void setAllowableClockSkew( long allowableClockSkew )
    {
        this.allowableClockSkew = allowableClockSkew;
    }


    /**
     * Returns the Change Password service principal.
     *
     * @return The Change Password service principal.
     */
    public KerberosPrincipal getServicePrincipal()
    {
        return new KerberosPrincipal( servicePrincipal );
    }


    /**
     * @param servicePrincipal The Change Password service principal to set.
     */
    public void setServicePrincipal( String servicePrincipal )
    {
        this.servicePrincipal = servicePrincipal;
    }


    /**
     * Returns whether empty addresses are allowed.
     *
     * @return Whether empty addresses are allowed.
     */
    public boolean isEmptyAddressesAllowed()
    {
        return isEmptyAddressesAllowed;
    }


    /**
     * @param isEmptyAddressesAllowed The isEmptyAddressesAllowed to set.
     */
    public void setEmptyAddressesAllowed( boolean isEmptyAddressesAllowed )
    {
        this.isEmptyAddressesAllowed = isEmptyAddressesAllowed;
    }


    /**
     * Returns the password length.
     *
     * @return The password length.
     */
    public int getPasswordLengthPolicy()
    {
        return policyPasswordLength;
    }


    /**
     * Returns the category count.
     *
     * @return The category count.
     */
    public int getCategoryCountPolicy()
    {
        return policyCategoryCount;
    }


    /**
     * Returns the token size.
     *
     * @return The token size.
     */
    public int getTokenSizePolicy()
    {
        return policyTokenSize;
    }


    /**
     * @org.apache.xbean.InitMethod
     */
    public void start() throws IOException
    {
        PrincipalStore store = new JndiPrincipalStoreImpl( getCatalogBaseDn(), getSearchBaseDn(), directoryService );

        if ( datagramAcceptor != null )
        {
            DatagramAcceptorConfig udpConfig = new DatagramAcceptorConfig();
            datagramAcceptor.bind( new InetSocketAddress( getIpPort() ), new ChangePasswordProtocolHandler( this, store ), udpConfig );
        }

        if ( socketAcceptor != null )
        {
            SocketAcceptorConfig tcpConfig = new SocketAcceptorConfig();
            tcpConfig.setDisconnectOnUnbind( false );
            tcpConfig.setReuseAddress( true );
            socketAcceptor.bind( new InetSocketAddress( getIpPort() ), new ChangePasswordProtocolHandler( this, store ), tcpConfig );
        }
    }

    /**
     * @org.apache.xbean.DestroyMethod
     */
    public void stop() {
        if ( datagramAcceptor != null )
        {
            datagramAcceptor.unbind( new InetSocketAddress( getIpPort() ));
        }
        if ( socketAcceptor != null )
        {
            socketAcceptor.unbind( new InetSocketAddress( getIpPort() ));
        }
    }

    private void prepareEncryptionTypes()
    {
        String[] encryptionTypeStrings = ENCRYPTION_TYPES_DEFAULT;

        List<EncryptionType> encTypes = new ArrayList<EncryptionType>();

        for ( String enc : encryptionTypeStrings )
        {
            for ( EncryptionType type : EncryptionType.VALUES )
            {
                if ( type.toString().equalsIgnoreCase( enc ) )
                {
                    encTypes.add( type );
                }
            }
        }

        encryptionTypes = encTypes.toArray( new EncryptionType[encTypes.size()] );
    }
}
