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
public class KdcServerBean extends DSBasedServerBean
{
    /** The default allowable clockskew */
    private static final long DEFAULT_ALLOWABLE_CLOCKSKEW = 5 * 60000;

    /** The default for allowing empty addresses */
    private static final boolean DEFAULT_EMPTY_ADDRESSES_ALLOWED = true;

    /** The allowable clock skew. */
    private long krballowableclockskew = DEFAULT_ALLOWABLE_CLOCKSKEW;

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

    /** Whether empty addresses are allowed. */
    private boolean krbemptyaddressesallowed = DEFAULT_EMPTY_ADDRESSES_ALLOWED;

    /** Whether forwardable addresses are allowed. */
    private boolean krbforwardableallowed = DEFAULT_TGS_FORWARDABLE_ALLOWED;

    /** Whether pre-authentication by encrypted timestamp is required. */
    private boolean krbpaenctimestamprequired = DEFAULT_PA_ENC_TIMESTAMP_REQUIRED;

    /** Whether postdated tickets are allowed. */
    private boolean krbpostdatedallowed = DEFAULT_TGS_POSTDATED_ALLOWED;

    /** Whether proxiable addresses are allowed. */
    private boolean krbproxiableallowed = DEFAULT_TGS_PROXIABLE_ALLOWED;

    /** Whether renewable tickets are allowed. */
    private boolean krbrenewableallowed = DEFAULT_TGS_RENEWABLE_ALLOWED;

    /** The maximum renewable lifetime. */
    private long krbmaximumrenewablelifetime = DEFAULT_TGS_MAXIMUM_RENEWABLE_LIFETIME;

    /** The maximum ticket lifetime. */
    private long krbmaximumticketlifetime = DEFAULT_TGS_MAXIMUM_TICKET_LIFETIME;

    /** The primary realm */
    private String krbprimaryrealm = DEFAULT_REALM;

    /** Whether to verify the body checksum. */
    private boolean krbbodychecksumverified = DEFAULT_VERIFY_BODY_CHECKSUM;

    /** The encryption types. */
    private Set<EncryptionType> krbencryptiontypes;

    /** The service principal name. */
    private String krbkdcprincipal = DEFAULT_PRINCIPAL;

    /**
     * Create a new KdcServerBean instance
     */
    public KdcServerBean()
    {
        super();
        
        // Enabled by default
        setEnabled( true );
    }
    
    
    /**
     * Returns the allowable clock skew.
     *
     * @return The allowable clock skew.
     */
    public long getKrbAllowableClockSkew()
    {
        return krballowableclockskew;
    }


    /**
     * @param krbAllowableClockSkew the allowableClockSkew to set
     */
    public void setKrbAllowableClockSkew( long krbAllowableClockSkew )
    {
        this.krballowableclockskew = krbAllowableClockSkew;
    }


    /**
     * Returns the encryption types.
     *
     * @return The encryption types.
     */
    public Set<EncryptionType> getKrbEncryptionTypes()
    {
        return krbencryptiontypes;
    }


    /**
     * Initialize the encryptionTypes set
     * 
     * @param krbEncryptionTypes the encryptionTypes to set
     */
    public void setKrbEncryptionTypes( EncryptionType... krbEncryptionTypes )
    {
        if ( krbEncryptionTypes != null )
        {
            this.krbencryptiontypes.clear();
            
            for ( EncryptionType encryptionType:krbEncryptionTypes )
            {
                this.krbencryptiontypes.add( encryptionType );
            }
        }
    }


    /**
     * @return the isEmptyAddressesAllowed
     */
    public boolean isKrbEmptyAddressesAllowed()
    {
        return krbemptyaddressesallowed;
    }


    /**
     * @param krbEmptyAddressesAllowed the krbEmptyAddressesAllowed to set
     */
    public void setKrbEmptyAddressesAllowed( boolean krbEmptyAddressesAllowed )
    {
        this.krbemptyaddressesallowed = krbEmptyAddressesAllowed;
    }


    /**
     * @return the krbForwardableAllowed
     */
    public boolean isKrbForwardableAllowed()
    {
        return krbforwardableallowed;
    }


    /**
     * @param krbForwardableAllowed the krbForwardableAllowed to set
     */
    public void setKrbForwardableAllowed( boolean krbForwardableAllowed )
    {
        this.krbforwardableallowed = krbForwardableAllowed;
    }


    /**
     * Returns whether pre-authentication by encrypted timestamp is required.
     *
     * @return Whether pre-authentication by encrypted timestamp is required.
     */
    public boolean isKrbPaEncTimestampRequired()
    {
        return krbpaenctimestamprequired;
    }


    /**
     * @param krbPaEncTimestampRequired the krbPaEncTimestampRequired to set
     */
    public void setKrbPaEncTimestampRequired( boolean krbPaEncTimestampRequired )
    {
        this.krbpaenctimestamprequired = krbPaEncTimestampRequired;
    }


    /**
     * @return the krbPostdatedAllowed
     */
    public boolean isKrbPostdatedAllowed()
    {
        return krbpostdatedallowed;
    }


    /**
     * @param krbPostdatedAllowed the krbPostdatedAllowed to set
     */
    public void setKrbPostdatedAllowed( boolean krbPostdatedAllowed )
    {
        this.krbpostdatedallowed = krbPostdatedAllowed;
    }


    /**
     * @return the krbProxiableAllowed
     */
    public boolean isKrbProxiableAllowed()
    {
        return krbproxiableallowed;
    }


    /**
     * @param krbProxiableAllowed the krbProxiableAllowed to set
     */
    public void setKrbProxiableAllowed( boolean krbProxiableAllowed )
    {
        this.krbproxiableallowed = krbProxiableAllowed;
    }


    /**
     * @return the krbRenewableAllowed
     */
    public boolean isKrbRenewableAllowed()
    {
        return krbrenewableallowed;
    }


    /**
     * @param krbRenewableAllowed the krbRenewableAllowed to set
     */
    public void setKrbRenewableAllowed( boolean krbRenewableAllowed )
    {
        this.krbrenewableallowed = krbRenewableAllowed;
    }


    /**
     * @return the krbMaximumRenewableLifetime
     */
    public long getKrbMaximumRenewableLifetime()
    {
        return krbmaximumrenewablelifetime;
    }


    /**
     * @param krbMaximumRenewableLifetime the krbMaximumRenewableLifetime to set
     */
    public void setKrbMaximumRenewableLifetime( long krbMaximumRenewableLifetime )
    {
        this.krbmaximumrenewablelifetime = krbMaximumRenewableLifetime;
    }


    /**
     * @return the krbMaximumTicketLifetime
     */
    public long getKrbMaximumTicketLifetime()
    {
        return krbmaximumticketlifetime;
    }


    /**
     * @param krbMaximumTicketLifetime the krbMaximumTicketLifetime to set
     */
    public void setKrbMaximumTicketLifetime( long krbMaximumTicketLifetime )
    {
        this.krbmaximumticketlifetime = krbMaximumTicketLifetime;
    }


    /**
     * Returns the primary realm.
     *
     * @return The primary realm.
     */
    public String getKrbPrimaryRealm()
    {
        return krbprimaryrealm;
    }


    /**
     * @param krbPrimaryRealm the krbPrimaryRealm to set
     */
    public void setKrbPrimaryRealm( String krbPrimaryRealm )
    {
        this.krbprimaryrealm = krbPrimaryRealm;
    }


    /**
     * @return the krbBodyChecksumVerified
     */
    public boolean isKrbBodyChecksumVerified()
    {
        return krbbodychecksumverified;
    }


    /**
     * @param krbBodyChecksumVerified the krbBodyChecksumVerified to set
     */
    public void setKrbBodyChecksumVerified( boolean krbBodyChecksumVerified )
    {
        this.krbbodychecksumverified = krbBodyChecksumVerified;
    }


    /**
     * Returns the service principal for this KDC service.
     *
     * @return The service principal for this KDC service.
     */
    public KerberosPrincipal getKrbKdcPrincipal()
    {
        return new KerberosPrincipal( krbkdcprincipal );
    }


    /**
     * @param krbKdcPrincipal the krbKdcPrincipal to set
     */
    public void setKrbKdcPrincipal( String krbKdcPrincipal )
    {
        this.krbkdcprincipal = krbKdcPrincipal;
    }
}
