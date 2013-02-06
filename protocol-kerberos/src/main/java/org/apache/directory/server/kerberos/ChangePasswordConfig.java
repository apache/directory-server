
package org.apache.directory.server.kerberos;

public class ChangePasswordConfig extends KerberosConfig
{
    /** The default change password principal name. */
    private static final String SERVICE_PRINCIPAL_DEFAULT = "kadmin/changepw@EXAMPLE.COM";

    public ChangePasswordConfig()
    {
        setServicePrincipal( SERVICE_PRINCIPAL_DEFAULT );
    }

    public ChangePasswordConfig( KerberosConfig kdcConfig )
    {
        setServicePrincipal( "kadmin/changepw@" + kdcConfig.getPrimaryRealm() );

        // copy the relevant kdc config parameters
        this.setAllowableClockSkew( kdcConfig.getAllowableClockSkew() );
        this.setBodyChecksumVerified( kdcConfig.isBodyChecksumVerified() );
        this.setEmptyAddressesAllowed( kdcConfig.isEmptyAddressesAllowed() );
        this.setEncryptionTypes( kdcConfig.getEncryptionTypes() );
        this.setForwardableAllowed( kdcConfig.isForwardableAllowed() );
        this.setMaximumRenewableLifetime( kdcConfig.getMaximumRenewableLifetime() );
        this.setMaximumTicketLifetime( kdcConfig.getMaximumTicketLifetime() );
        this.setPaEncTimestampRequired( kdcConfig.isPaEncTimestampRequired() );
        this.setSearchBaseDn( kdcConfig.getSearchBaseDn() );
    }
}
