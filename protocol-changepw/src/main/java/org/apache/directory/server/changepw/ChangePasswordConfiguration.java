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


import java.util.ArrayList;
import java.util.List;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.protocol.shared.ServiceConfiguration;


/**
 * Contains the configuration parameters for the Change Password protocol provider.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ChangePasswordConfiguration extends ServiceConfiguration
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
    private static final long DEFAULT_ALLOWABLE_CLOCKSKEW = 5 * MINUTE;

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
    public ChangePasswordConfiguration()
    {
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
     * Returns the encryption types.
     *
     * @return The encryption types.
     */
    public EncryptionType[] getEncryptionTypes()
    {
        return encryptionTypes;
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
     * Returns the Change Password service principal.
     *
     * @return The Change Password service principal.
     */
    public KerberosPrincipal getServicePrincipal()
    {
        return new KerberosPrincipal( servicePrincipal );
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
