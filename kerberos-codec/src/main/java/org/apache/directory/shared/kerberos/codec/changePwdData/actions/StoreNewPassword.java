package org.apache.directory.shared.kerberos.codec.changePwdData.actions;


import org.apache.directory.api.asn1.actions.AbstractReadOctetString;
import org.apache.directory.shared.kerberos.codec.changePwdData.ChangePasswdDataContainer;


/**
 * The action used to set the new password
 */
public class StoreNewPassword extends AbstractReadOctetString<ChangePasswdDataContainer>
{
    /**
     * Instantiates a new StoreNewPassword action.
     */
    public StoreNewPassword()
    {
        super( "Kerberos changepassword's new password value" );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void setOctetString( byte[] newPasswd, ChangePasswdDataContainer container )
    {
        container.getChngPwdData().setNewPasswd( newPasswd );
        container.setGrammarEndAllowed( true );
    }
}
