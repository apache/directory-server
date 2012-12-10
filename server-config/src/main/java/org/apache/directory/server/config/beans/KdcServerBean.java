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

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.config.ConfigurationElement;


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

    /** The allowable clock skew. */
    @ConfigurationElement(attributeType = "ads-krbAllowableClockSkew", isOptional = true)
    private long krbAllowableClockSkew = DEFAULT_ALLOWABLE_CLOCKSKEW;

    /** Whether empty addresses are allowed. */
    @ConfigurationElement(attributeType = "ads-krbEmptyAddressesAllowed", isOptional = true)
    private boolean krbEmptyAddressesAllowed = DEFAULT_EMPTY_ADDRESSES_ALLOWED;

    /** Whether forwardable addresses are allowed. */
    @ConfigurationElement(attributeType = "ads-krbForwardableAllowed", isOptional = true)
    private boolean krbForwardableAllowed = DEFAULT_TGS_FORWARDABLE_ALLOWED;

    /** Whether pre-authentication by encrypted timestamp is required. */
    @ConfigurationElement(attributeType = "ads-krbPAEncTimestampRequired", isOptional = true)
    private boolean krbPAEncTimestampRequired = DEFAULT_PA_ENC_TIMESTAMP_REQUIRED;

    /** Whether postdated tickets are allowed. */
    @ConfigurationElement(attributeType = "ads-krbPostdatedAllowed", isOptional = true)
    private boolean krbPostdatedAllowed = DEFAULT_TGS_POSTDATED_ALLOWED;

    /** Whether proxiable addresses are allowed. */
    @ConfigurationElement(attributeType = "ads-krbProxiableAllowed", isOptional = true)
    private boolean krbProxiableAllowed = DEFAULT_TGS_PROXIABLE_ALLOWED;

    /** Whether renewable tickets are allowed. */
    @ConfigurationElement(attributeType = "ads-krbRenewableAllowed", isOptional = true)
    private boolean krbRenewableAllowed = DEFAULT_TGS_RENEWABLE_ALLOWED;

    /** The maximum renewable lifetime. */
    @ConfigurationElement(attributeType = "ads-krbMaximumRenewableLifetime", isOptional = true)
    private long krbMaximumRenewableLifetime = DEFAULT_TGS_MAXIMUM_RENEWABLE_LIFETIME;

    /** The maximum ticket lifetime. */
    @ConfigurationElement(attributeType = "ads-krbMaximumTicketLifetime", isOptional = true)
    private long krbMaximumTicketLifetime = DEFAULT_TGS_MAXIMUM_TICKET_LIFETIME;

    /** The primary realm */
    @ConfigurationElement(attributeType = "ads-krbPrimaryRealm", isOptional = true)
    private String krbPrimaryRealm = DEFAULT_REALM;

    /** Whether to verify the body checksum. */
    @ConfigurationElement(attributeType = "ads-krbBodyChecksumVerified", isOptional = true)
    private boolean krbBodyChecksumVerified = DEFAULT_VERIFY_BODY_CHECKSUM;

    /** The encryption types. */
    @ConfigurationElement(attributeType = "ads-krbEncryptionTypes", isOptional = true)
    private List<String> krbEncryptionTypes = new ArrayList<String>();

    /** The service principal name. */
    @ConfigurationElement(attributeType = "ads-krbKdcPrincipal", isOptional = true)
    private String krbKdcPrincipal = DEFAULT_PRINCIPAL;


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
        return krbAllowableClockSkew;
    }


    /**
     * @param krbAllowableClockSkew the allowableClockSkew to set
     */
    public void setKrbAllowableClockSkew( long krbAllowableClockSkew )
    {
        this.krbAllowableClockSkew = krbAllowableClockSkew;
    }


    /**
     * Returns the encryption types.
     *
     * @return The encryption types.
     */
    public List<String> getKrbEncryptionTypes()
    {
        return krbEncryptionTypes;
    }


    /**
     * Initialize the encryptionTypes set
     * 
     * @param krbEncryptionTypes the encryptionTypes to set
     */
    public void addKrbEncryptionTypes( String... krbEncryptionTypes )
    {
        for ( String encryptionType : krbEncryptionTypes )
        {
            this.krbEncryptionTypes.add( encryptionType );
        }
    }


    /**
     * @return the isEmptyAddressesAllowed
     */
    public boolean isKrbEmptyAddressesAllowed()
    {
        return krbEmptyAddressesAllowed;
    }


    /**
     * @param krbEmptyAddressesAllowed the krbEmptyAddressesAllowed to set
     */
    public void setKrbEmptyAddressesAllowed( boolean krbEmptyAddressesAllowed )
    {
        this.krbEmptyAddressesAllowed = krbEmptyAddressesAllowed;
    }


    /**
     * @return the krbForwardableAllowed
     */
    public boolean isKrbForwardableAllowed()
    {
        return krbForwardableAllowed;
    }


    /**
     * @param krbForwardableAllowed the krbForwardableAllowed to set
     */
    public void setKrbForwardableAllowed( boolean krbForwardableAllowed )
    {
        this.krbForwardableAllowed = krbForwardableAllowed;
    }


    /**
     * Returns whether pre-authentication by encrypted timestamp is required.
     *
     * @return Whether pre-authentication by encrypted timestamp is required.
     */
    public boolean isKrbPaEncTimestampRequired()
    {
        return krbPAEncTimestampRequired;
    }


    /**
     * @param krbPaEncTimestampRequired the krbPaEncTimestampRequired to set
     */
    public void setKrbPaEncTimestampRequired( boolean krbPaEncTimestampRequired )
    {
        this.krbPAEncTimestampRequired = krbPaEncTimestampRequired;
    }


    /**
     * @return the krbPostdatedAllowed
     */
    public boolean isKrbPostdatedAllowed()
    {
        return krbPostdatedAllowed;
    }


    /**
     * @param krbPostdatedAllowed the krbPostdatedAllowed to set
     */
    public void setKrbPostdatedAllowed( boolean krbPostdatedAllowed )
    {
        this.krbPostdatedAllowed = krbPostdatedAllowed;
    }


    /**
     * @return the krbProxiableAllowed
     */
    public boolean isKrbProxiableAllowed()
    {
        return krbProxiableAllowed;
    }


    /**
     * @param krbProxiableAllowed the krbProxiableAllowed to set
     */
    public void setKrbProxiableAllowed( boolean krbProxiableAllowed )
    {
        this.krbProxiableAllowed = krbProxiableAllowed;
    }


    /**
     * @return the krbRenewableAllowed
     */
    public boolean isKrbRenewableAllowed()
    {
        return krbRenewableAllowed;
    }


    /**
     * @param krbRenewableAllowed the krbRenewableAllowed to set
     */
    public void setKrbRenewableAllowed( boolean krbRenewableAllowed )
    {
        this.krbRenewableAllowed = krbRenewableAllowed;
    }


    /**
     * @return the krbMaximumRenewableLifetime
     */
    public long getKrbMaximumRenewableLifetime()
    {
        return krbMaximumRenewableLifetime;
    }


    /**
     * @param krbMaximumRenewableLifetime the krbMaximumRenewableLifetime to set
     */
    public void setKrbMaximumRenewableLifetime( long krbMaximumRenewableLifetime )
    {
        this.krbMaximumRenewableLifetime = krbMaximumRenewableLifetime;
    }


    /**
     * @return the krbMaximumTicketLifetime
     */
    public long getKrbMaximumTicketLifetime()
    {
        return krbMaximumTicketLifetime;
    }


    /**
     * @param krbMaximumTicketLifetime the krbMaximumTicketLifetime to set
     */
    public void setKrbMaximumTicketLifetime( long krbMaximumTicketLifetime )
    {
        this.krbMaximumTicketLifetime = krbMaximumTicketLifetime;
    }


    /**
     * Returns the primary realm.
     *
     * @return The primary realm.
     */
    public String getKrbPrimaryRealm()
    {
        return krbPrimaryRealm;
    }


    /**
     * @param krbPrimaryRealm the krbPrimaryRealm to set
     */
    public void setKrbPrimaryRealm( String krbPrimaryRealm )
    {
        this.krbPrimaryRealm = krbPrimaryRealm;
    }


    /**
     * @return the krbBodyChecksumVerified
     */
    public boolean isKrbBodyChecksumVerified()
    {
        return krbBodyChecksumVerified;
    }


    /**
     * @param krbBodyChecksumVerified the krbBodyChecksumVerified to set
     */
    public void setKrbBodyChecksumVerified( boolean krbBodyChecksumVerified )
    {
        this.krbBodyChecksumVerified = krbBodyChecksumVerified;
    }


    /**
     * Returns the service principal for this KDC service.
     *
     * @return The service principal for this KDC service.
     */
    public KerberosPrincipal getKrbKdcPrincipal()
    {
        return new KerberosPrincipal( krbKdcPrincipal );
    }


    /**
     * @param krbKdcPrincipal the krbKdcPrincipal to set
     */
    public void setKrbKdcPrincipal( String krbKdcPrincipal )
    {
        this.krbKdcPrincipal = krbKdcPrincipal;
    }


    /**
     * {@inheritDoc}
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "KDCServer :\n" );
        sb.append( super.toString( tabs + "  " ) );
        sb.append( toString( tabs, "  body checksum verified", krbBodyChecksumVerified ) );
        sb.append( toString( tabs, "  empty address alowed", krbEmptyAddressesAllowed ) );
        sb.append( toString( tabs, "  forwardable allowed", krbForwardableAllowed ) );
        sb.append( toString( tabs, "  PA encode timestamp required", krbPAEncTimestampRequired ) );
        sb.append( toString( tabs, "  postdated allowed", krbPostdatedAllowed ) );
        sb.append( toString( tabs, "  proxiable allowed", krbProxiableAllowed ) );
        sb.append( toString( tabs, "  renew allowed", krbRenewableAllowed ) );
        sb.append( toString( tabs, "  allowable clock skew", krbAllowableClockSkew ) );
        sb.append( toString( tabs, "  KDC principal", krbKdcPrincipal ) );
        sb.append( toString( tabs, "  maximum renewable lifetime", krbMaximumRenewableLifetime ) );
        sb.append( toString( tabs, "  maximum ticket lifetime", krbMaximumTicketLifetime ) );
        sb.append( toString( tabs, "  primary realm", krbPrimaryRealm ) );

        if ( ( krbEncryptionTypes != null ) && ( krbEncryptionTypes.size() > 0 ) )
        {
            sb.append( tabs ).append( "  encryption types :\n" );

            for ( String encryptionType : krbEncryptionTypes )
            {
                sb.append( toString( tabs, "    encryption type", encryptionType ) );
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
