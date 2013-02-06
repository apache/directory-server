package org.apache.directory.server.kerberos;


import java.util.HashSet;
import java.util.Set;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.shared.kerberos.KerberosUtils;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.codec.types.PrincipalNameType;


public class KerberosConfig
{

    /** The default kdc service principal */
    public static final String DEFAULT_PRINCIPAL = "krbtgt/EXAMPLE.COM@EXAMPLE.COM";
    
    /** The default kdc realm */
    public static final String DEFAULT_REALM = "EXAMPLE.COM";
    
    /** The default allowable clockskew */
    public static final long DEFAULT_ALLOWABLE_CLOCKSKEW = 5 * 60000;
    
    /** The default for allowing empty addresses */
    public static final boolean DEFAULT_EMPTY_ADDRESSES_ALLOWED = true;
    
    /** The default for requiring encrypted timestamps */
    public static final boolean DEFAULT_PA_ENC_TIMESTAMP_REQUIRED = true;
    
    /** The default for the maximum ticket lifetime */
    public static final int DEFAULT_TGS_MAXIMUM_TICKET_LIFETIME = 60000 * 1440;
    
    /** The default for the minimum ticket lifetime, 4 minutes */
    public static final int DEFAULT_TGS_MINIMUM_TICKET_LIFETIME = 60000 * 4;
    
    /** The default for the maximum renewable lifetime */
    public static final int DEFAULT_TGS_MAXIMUM_RENEWABLE_LIFETIME = 60000 * 10080;
    
    /** The default for allowing forwardable tickets */
    public static final boolean DEFAULT_TGS_FORWARDABLE_ALLOWED = true;
    
    /** The default for allowing proxiable tickets */
    public static final boolean DEFAULT_TGS_PROXIABLE_ALLOWED = true;
    
    /** The default for allowing postdated tickets */
    public static final boolean DEFAULT_TGS_POSTDATED_ALLOWED = true;
    
    /** The default for allowing renewable tickets */
    public static final boolean DEFAULT_TGS_RENEWABLE_ALLOWED = true;
    
    /** The default for verifying the body checksum */
    public static final boolean DEFAULT_VERIFY_BODY_CHECKSUM = true;
    
    /** The default encryption types */
    public static final String[] DEFAULT_ENCRYPTION_TYPES = new String[]
        { "aes128-cts-hmac-sha1-96", "des-cbc-md5", "des3-cbc-sha1-kd" };
    
    /** The primary realm */
    private String primaryRealm = KerberosConfig.DEFAULT_REALM;

    /** The service principal name. */
    private String servicePrincipal = KerberosConfig.DEFAULT_PRINCIPAL;

    /** The allowable clock skew. */
    private long allowableClockSkew = KerberosConfig.DEFAULT_ALLOWABLE_CLOCKSKEW;

    /** Whether pre-authentication by encrypted timestamp is required. */
    private boolean isPaEncTimestampRequired = KerberosConfig.DEFAULT_PA_ENC_TIMESTAMP_REQUIRED;

    /** The maximum ticket lifetime. */
    private long maximumTicketLifetime = KerberosConfig.DEFAULT_TGS_MAXIMUM_TICKET_LIFETIME;

    /** The minimum ticket lifetime. */
    private long minimumTicketLifetime = KerberosConfig.DEFAULT_TGS_MINIMUM_TICKET_LIFETIME;

    /** The maximum renewable lifetime. */
    private long maximumRenewableLifetime = KerberosConfig.DEFAULT_TGS_MAXIMUM_RENEWABLE_LIFETIME;

    /** Whether empty addresses are allowed. */
    private boolean isEmptyAddressesAllowed = KerberosConfig.DEFAULT_EMPTY_ADDRESSES_ALLOWED;

    /** Whether forwardable addresses are allowed. */
    private boolean isForwardableAllowed = KerberosConfig.DEFAULT_TGS_FORWARDABLE_ALLOWED;

    /** Whether proxiable addresses are allowed. */
    private boolean isProxiableAllowed = KerberosConfig.DEFAULT_TGS_PROXIABLE_ALLOWED;

    /** Whether postdated tickets are allowed. */
    private boolean isPostdatedAllowed = KerberosConfig.DEFAULT_TGS_POSTDATED_ALLOWED;

    /** Whether renewable tickets are allowed. */
    private boolean isRenewableAllowed = KerberosConfig.DEFAULT_TGS_RENEWABLE_ALLOWED;

    /** Whether to verify the body checksum. */
    private boolean isBodyChecksumVerified = KerberosConfig.DEFAULT_VERIFY_BODY_CHECKSUM;

    /** The encryption types. */
    private Set<EncryptionType> encryptionTypes;

    /* cached kerberos/changepassword service principal */
    private KerberosPrincipal srvPrincipal;
    
    private String searchBaseDn;
    
    public KerberosConfig()
    {
        setSearchBaseDn( ServerDNConstants.USER_EXAMPLE_COM_DN );
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

            for ( EncryptionType encryptionType : encryptionTypes )
            {
                this.encryptionTypes.add( encryptionType );
            }
        }
        
        this.encryptionTypes = KerberosUtils.orderEtypesByStrength( this.encryptionTypes );
    }


    /**
     * Initialize the encryptionTypes set
     * 
     * @param encryptionTypes the encryptionTypes to set
     */
    public void setEncryptionTypes( Set<EncryptionType> encryptionTypes )
    {
        this.encryptionTypes = KerberosUtils.orderEtypesByStrength( encryptionTypes );
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
    public void setServicePrincipal( String kdcPrincipal )
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
     * Returns the service principal for this KDC/changepwd service.
     *
     * @return The service principal for this KDC/changepwd service.
     */
    public KerberosPrincipal getServicePrincipal()
    {
        if( srvPrincipal == null )
        {
            srvPrincipal = new KerberosPrincipal( servicePrincipal, PrincipalNameType.KRB_NT_SRV_INST.getValue() );
        }
        
        return srvPrincipal;
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

    
    public String getSearchBaseDn()
    {
        return searchBaseDn;
    }


    public void setSearchBaseDn( String searchBaseDn )
    {
        this.searchBaseDn = searchBaseDn;
    }

    
    public long getMinimumTicketLifetime()
    {
        return minimumTicketLifetime;
    }


    public void setMinimumTicketLifetime( long minimumTicketLifetime )
    {
        this.minimumTicketLifetime = minimumTicketLifetime;
    }


    /**
     * Construct an HashSet containing the default encryption types
     */
    private void prepareEncryptionTypes()
    {
        String[] encryptionTypeStrings = KerberosConfig.DEFAULT_ENCRYPTION_TYPES;

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
        
        encryptionTypes = KerberosUtils.orderEtypesByStrength( encryptionTypes );
    }
}
