package org.apache.directory.shared.kerberos.codec.changePwdData.actions;


import org.apache.directory.shared.kerberos.codec.actions.AbstractReadPrincipalName;
import org.apache.directory.shared.kerberos.codec.changePwdData.ChangePasswdDataContainer;
import org.apache.directory.shared.kerberos.components.PrincipalName;


/**
 * The action used to set the targname of ChangePasswdData
 */
public class StoreTargName extends AbstractReadPrincipalName<ChangePasswdDataContainer>
{
    /**
     * Instantiates a new StoreTargName action.
     */
    public StoreTargName()
    {
        super( "Kerberos change password targetName" );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void setPrincipalName( PrincipalName principalName, ChangePasswdDataContainer ticketContainer )
    {
        ticketContainer.getChngPwdData().setTargName( principalName );
        ticketContainer.setGrammarEndAllowed( true );
    }
}
