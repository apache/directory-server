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

import java.util.Set;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;

/**
 * A class used to store the KdcServer configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KdcServerBean extends DirectoryBackedServiceBean
{
    /** The default allowable clockskew */
    private static final long DEFAULT_ALLOWABLE_CLOCKSKEW = 5 * 60000;

    /** The default for allowing empty addresses */
    private static final boolean DEFAULT_EMPTY_ADDRESSES_ALLOWED = true;

    /** The allowable clock skew. */
    private long allowableClockSkew = DEFAULT_ALLOWABLE_CLOCKSKEW;

    /** The default for allowing forwardable tickets */
    private static final boolean DEFAULT_TGS_FORWARDABLE_ALLOWED = true;

    /** The default for requiring encrypted timestamps */
    private static final boolean DEFAULT_PA_ENC_TIMESTAMP_REQUIRED = true;

    /** The default for allowing postdated tickets */
    private static final boolean DEFAULT_TGS_POSTDATED_ALLOWED = true;

    /** The default for allowing proxiable tickets */
    private static final boolean DEFAULT_TGS_PROXIABLE_ALLOWED = true;

    /** The default for allowing renewable tickets */
    private static final boolean DEFAULT_TGS_RENEWABLE_ALLOWED = true;

    /** The default for the maximum renewable lifetime */
    private static final int DEFAULT_TGS_MAXIMUM_RENEWABLE_LIFETIME = 60000 * 10080;

    /** The default for the maximum ticket lifetime */
    private static final int DEFAULT_TGS_MAXIMUM_TICKET_LIFETIME = 60000 * 1440;

    /** The default kdc realm */
    private static final String DEFAULT_REALM = "EXAMPLE.COM";

    /** The default for verifying the body checksum */
    private static final boolean DEFAULT_VERIFY_BODY_CHECKSUM = true;

    /** The default kdc service principal */
    private static final String DEFAULT_PRINCIPAL = "krbtgt/EXAMPLE.COM@EXAMPLE.COM";

    /** Tells if the KdcServer is enabled */
    private boolean enabled;

    /** Whether empty addresses are allowed. */
    private boolean isEmptyAddressesAllowed = DEFAULT_EMPTY_ADDRESSES_ALLOWED;

    /** Whether forwardable addresses are allowed. */
    private boolean isForwardableAllowed = DEFAULT_TGS_FORWARDABLE_ALLOWED;

    /** Whether pre-authentication by encrypted timestamp is required. */
    private boolean isPaEncTimestampRequired = DEFAULT_PA_ENC_TIMESTAMP_REQUIRED;

    /** Whether postdated tickets are allowed. */
    private boolean isPostdatedAllowed = DEFAULT_TGS_POSTDATED_ALLOWED;

    /** Whether proxiable addresses are allowed. */
    private boolean isProxiableAllowed = DEFAULT_TGS_PROXIABLE_ALLOWED;

    /** Whether renewable tickets are allowed. */
    private boolean isRenewableAllowed = DEFAULT_TGS_RENEWABLE_ALLOWED;

    /** The maximum renewable lifetime. */
    private long maximumRenewableLifetime = DEFAULT_TGS_MAXIMUM_RENEWABLE_LIFETIME;

    /** The maximum ticket lifetime. */
    private long maximumTicketLifetime = DEFAULT_TGS_MAXIMUM_TICKET_LIFETIME;

    /** The primary realm */
    private String primaryRealm = DEFAULT_REALM;

    /** Whether to verify the body checksum. */
    private boolean isBodyChecksumVerified = DEFAULT_VERIFY_BODY_CHECKSUM;

    /** The encryption types. */
    private Set<EncryptionType> encryptionTypes;

    /** The service principal name. */
    private String servicePrincipal = DEFAULT_PRINCIPAL;

    /**
     * Create a new KdcServerBean instance
     */
    public KdcServerBean()
    {
        // Enabled by default
        enabled = true;
    }
    
    
    /**
     * @return <code>true</code> if the Journal is enabled
     */
    public boolean isEnabled() 
    {
        return enabled;
    }

    
    /**
     * @param enabled Set the enabled flag
     */
    public void setEnabled( boolean enabled ) 
    {
        this.enabled = enabled;
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
     * @param allowableClockSkew the allowableClockSkew to set
     */
    public void setAllowableClockSkew( long allowableClockSkew )
    {
        this.allowableClockSkew = allowableClockSkew;
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
     * Initialize the encryptionTypes set
     * 
     * @param encryptionTypes the encryptionTypes to set
     */
    public void setEncryptionTypes( EncryptionType... encryptionTypes )
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
     * @return the isEmptyAddressesAllowed
     */
    public boolean isEmptyAddressesAllowed()
    {
        return isEmptyAddressesAllowed;
    }


    /**
     * @param isEmptyAddressesAllowed the isEmptyAddressesAllowed to set
     */
    public void setEmptyAddressesAllowed( boolean isEmptyAddressesAllowed )
    {
        this.isEmptyAddressesAllowed = isEmptyAddressesAllowed;
    }


    /**
     * @return the isForwardableAllowed
     */
    public boolean isForwardableAllowed()
    {
        return isForwardableAllowed;
    }


    /**
     * @param isForwardableAllowed the isForwardableAllowed to set
     */
    public void setForwardableAllowed( boolean isForwardableAllowed )
    {
        this.isForwardableAllowed = isForwardableAllowed;
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
     * @param isPaEncTimestampRequired the isPaEncTimestampRequired to set
     */
    public void setPaEncTimestampRequired( boolean isPaEncTimestampRequired )
    {
        this.isPaEncTimestampRequired = isPaEncTimestampRequired;
    }


    /**
     * @return the isPostdatedAllowed
     */
    public boolean isPostdatedAllowed()
    {
        return isPostdatedAllowed;
    }


    /**
     * @param isPostdatedAllowed the isPostdatedAllowed to set
     */
    public void setPostdatedAllowed( boolean isPostdatedAllowed )
    {
        this.isPostdatedAllowed = isPostdatedAllowed;
    }


    /**
     * @return the isProxiableAllowed
     */
    public boolean isProxiableAllowed()
    {
        return isProxiableAllowed;
    }


    /**
     * @param isProxiableAllowed the isProxiableAllowed to set
     */
    public void setProxiableAllowed( boolean isProxiableAllowed )
    {
        this.isProxiableAllowed = isProxiableAllowed;
    }


    /**
     * @return the isRenewableAllowed
     */
    public boolean isRenewableAllowed()
    {
        return isRenewableAllowed;
    }


    /**
     * @param isRenewableAllowed the isRenewableAllowed to set
     */
    public void setRenewableAllowed( boolean isRenewableAllowed )
    {
        this.isRenewableAllowed = isRenewableAllowed;
    }


    /**
     * @return the maximumRenewableLifetime
     */
    public long getMaximumRenewableLifetime()
    {
        return maximumRenewableLifetime;
    }


    /**
     * @param maximumRenewableLifetime the maximumRenewableLifetime to set
     */
    public void setMaximumRenewableLifetime( long maximumRenewableLifetime )
    {
        this.maximumRenewableLifetime = maximumRenewableLifetime;
    }


    /**
     * @return the maximumTicketLifetime
     */
    public long getMaximumTicketLifetime()
    {
        return maximumTicketLifetime;
    }


    /**
     * @param maximumTicketLifetime the maximumTicketLifetime to set
     */
    public void setMaximumTicketLifetime( long maximumTicketLifetime )
    {
        this.maximumTicketLifetime = maximumTicketLifetime;
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
     * @param primaryRealm the primaryRealm to set
     */
    public void setPrimaryRealm( String primaryRealm )
    {
        this.primaryRealm = primaryRealm;
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
     * Returns the service principal for this KDC service.
     *
     * @return The service principal for this KDC service.
     */
    public KerberosPrincipal getServicePrincipal()
    {
        return new KerberosPrincipal( servicePrincipal );
    }


    /**
     * @param kdcPrincipal the kdcPrincipal to set
     */
    public void setKdcPrincipal( String kdcPrincipal )
    {
        this.servicePrincipal = kdcPrincipal;
    }
}
