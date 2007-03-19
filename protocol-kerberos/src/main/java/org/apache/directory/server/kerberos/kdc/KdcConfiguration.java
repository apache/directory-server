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


import java.util.ArrayList;
import java.util.List;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.protocol.shared.ServiceConfiguration;


/**
 * Contains the configuration parameters for the Kerberos protocol provider.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class KdcConfiguration extends ServiceConfiguration
{
    private static final long serialVersionUID = 522567370475574165L;

    /** The default kdc port */
    private static final int DEFAULT_IP_PORT = 88;

    /** The default kdc search base DN */
    public static final String DEFAULT_SEARCH_BASEDN = "ou=users,dc=example,dc=com";

    /** The default kdc service pid */
    private static final String DEFAULT_PID = "org.apache.kerberos";

    /** The default kdc service name */
    private static final String DEFAULT_NAME = "Apache Kerberos Service";

    /** The default kdc service principal */
    private static final String DEFAULT_PRINCIPAL = "krbtgt/EXAMPLE.COM@EXAMPLE.COM";

    /** The default kdc realm */
    private static final String DEFAULT_REALM = "EXAMPLE.COM";

    /** The default allowable clockskew */
    private static final long DEFAULT_ALLOWABLE_CLOCKSKEW = 5 * MINUTE;

    /** The default encryption types */
    private static final String[] DEFAULT_ENCRYPTION_TYPES = new String[]
        { "des-cbc-md5" };

    /** The default for allowing empty addresses */
    private static final boolean DEFAULT_EMPTY_ADDRESSES_ALLOWED = true;

    /** The default for requiring encrypted timestamps */
    private static final boolean DEFAULT_PA_ENC_TIMESTAMP_REQUIRED = true;

    /** The default for the maximum ticket lifetime */
    private static final int DEFAULT_TGS_MAXIMUM_TICKET_LIFETIME = MINUTE * 1440;

    /** The default for the maximum renewable lifetime */
    private static final int DEFAULT_TGS_MAXIMUM_RENEWABLE_LIFETIME = MINUTE * 10080;

    /** The default for allowing forwardable tickets */
    private static final boolean DEFAULT_TGS_FORWARDABLE_ALLOWED = true;

    /** The default for allowing proxiable tickets */
    private static final boolean DEFAULT_TGS_PROXIABLE_ALLOWED = true;

    /** The default for allowing postdatable tickets */
    private static final boolean DEFAULT_TGS_POSTDATE_ALLOWED = true;

    /** The default for allowing renewable tickets */
    private static final boolean DEFAULT_TGS_RENEWABLE_ALLOWED = true;

    /** The encryption types. */
    private EncryptionType[] encryptionTypes;

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

    /** Whether postdating is allowed. */
    private boolean isPostdateAllowed = DEFAULT_TGS_POSTDATE_ALLOWED;

    /** Whether renewable tickets are allowed. */
    private boolean isRenewableAllowed = DEFAULT_TGS_RENEWABLE_ALLOWED;


    /**
     * Creates a new instance of KdcConfiguration.
     */
    public KdcConfiguration()
    {
        super.setServiceName( DEFAULT_NAME );
        super.setIpPort( DEFAULT_IP_PORT );
        super.setServicePid( DEFAULT_PID );
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
     * @return the isPostdateAllowed
     */
    public boolean isPostdateAllowed()
    {
        return isPostdateAllowed;
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
     * @param encryptionTypes the encryptionTypes to set
     */
    public void setEncryptionTypes( EncryptionType[] encryptionTypes )
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
     * @param isPostdateAllowed the isPostdateAllowed to set
     */
    public void setPostdateAllowed( boolean isPostdateAllowed )
    {
        this.isPostdateAllowed = isPostdateAllowed;
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
    public EncryptionType[] getEncryptionTypes()
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


    private void prepareEncryptionTypes()
    {
        String[] encryptionTypeStrings = DEFAULT_ENCRYPTION_TYPES;

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

        encryptionTypes = ( EncryptionType[] ) encTypes.toArray( new EncryptionType[encTypes.size()] );
    }
}
