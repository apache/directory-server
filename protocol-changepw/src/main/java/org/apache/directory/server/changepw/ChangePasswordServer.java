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
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.store.JndiPrincipalStoreImpl;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.directory.server.protocol.shared.DirectoryBackedService;
import org.apache.mina.transport.socket.nio.DatagramAcceptorConfig;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;


/**
 * Contains the configuration parameters for the Change Password protocol provider.
 *
 * @org.apache.xbean.XBean
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ChangePasswordServer extends DirectoryBackedService
{
    private static final long serialVersionUID = 3509208713288140629L;

    /** The default change password principal name. */
    private static final String SERVICE_PRINCIPAL_DEFAULT = "kadmin/changepw@EXAMPLE.COM";

    /** The default change password realm. */
    private static final String REALM_DEFAULT = "EXAMPLE.COM";

    /** The default change password port. */
    private static final int IP_PORT_DEFAULT = 464;

    /** The default encryption types. */
    public static final String[] ENCRYPTION_TYPES_DEFAULT = new String[]
        { "des-cbc-md5" };

    /** The default changepw buffer size. */
    private static final long DEFAULT_ALLOWABLE_CLOCKSKEW = 5 * 60000;

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


    /**
     * Creates a new instance of ChangePasswordConfiguration.
     */
    public ChangePasswordServer()
    {
        super.setServiceName( SERVICE_NAME_DEFAULT );
        super.setIpPort( IP_PORT_DEFAULT );
        super.setServiceId( SERVICE_PID_DEFAULT );
        super.setSearchBaseDn( ServerDNConstants.USER_EXAMPLE_COM_DN );

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
     * @throws IOException if we cannot bind to the specified ports
     */
    public void start() throws IOException
    {
        PrincipalStore store = new JndiPrincipalStoreImpl( getSearchBaseDn(),
                getSearchBaseDn(), getDirectoryService() );

        if ( getDatagramAcceptor() != null )
        {
            DatagramAcceptorConfig udpConfig = new DatagramAcceptorConfig();
            getDatagramAcceptor().bind( new InetSocketAddress( getIpPort() ),
                    new ChangePasswordProtocolHandler( this, store ), udpConfig );
        }

        if ( getSocketAcceptor() != null )
        {
            SocketAcceptorConfig tcpConfig = new SocketAcceptorConfig();
            tcpConfig.setDisconnectOnUnbind( false );
            tcpConfig.setReuseAddress( true );
            getSocketAcceptor().bind( new InetSocketAddress( getIpPort() ),
                    new ChangePasswordProtocolHandler( this, store ), tcpConfig );
        }
    }


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


    private void prepareEncryptionTypes()
    {
        String[] encryptionTypeStrings = ENCRYPTION_TYPES_DEFAULT;
        List<EncryptionType> encTypes = new ArrayList<EncryptionType>();

        for ( String enc : encryptionTypeStrings )
        {
            for ( EncryptionType type : EncryptionType.getEncryptionTypes() )
            {
                if ( type.toString().equalsIgnoreCase( enc ) )
                {
                    encTypes.add( type );
                }
            }
        }

        encryptionTypes = encTypes.toArray( new EncryptionType[encTypes.size()] );
    }


    /**
     * Sets the policy's minimum?? password length.
     *
     * @param policyPasswordLength the minimum password length requirement
     */
    public void setPolicyPasswordLength( int policyPasswordLength )
    {
        this.policyPasswordLength = policyPasswordLength;
    }


    /**
     * Sets the policy category count - what's this?
     *
     * @param policyCategoryCount the policy category count
     */
    public void setPolicyCategoryCount( int policyCategoryCount )
    {
        this.policyCategoryCount = policyCategoryCount;
    }


    /**
     * Sets the policy token size - what's this?
     *
     * @param policyTokenSize the policy token size
     */
    public void setPolicyTokenSize( int policyTokenSize )
    {
        this.policyTokenSize = policyTokenSize;
    }
}
