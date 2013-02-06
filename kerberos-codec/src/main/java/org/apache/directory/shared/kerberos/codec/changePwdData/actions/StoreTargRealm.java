package org.apache.directory.shared.kerberos.codec.changePwdData.actions;


import org.apache.directory.shared.kerberos.codec.actions.AbstractReadRealm;
import org.apache.directory.shared.kerberos.codec.changePwdData.ChangePasswdDataContainer;


/**
 * The action used to set the target realm of the ChangePasswdData
 */
public class StoreTargRealm extends AbstractReadRealm<ChangePasswdDataContainer>
{
    /**
     * Instantiates a new StoreRealm action.
     */
    public StoreTargRealm()
    {
        super( "Kerberos changepassword target realm value" );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void setRealm( String realm, ChangePasswdDataContainer container )
    {
        container.getChngPwdData().setTargRealm( realm );
        container.setGrammarEndAllowed( true );
    }
}
