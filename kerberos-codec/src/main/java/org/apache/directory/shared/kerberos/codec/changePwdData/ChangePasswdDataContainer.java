package org.apache.directory.shared.kerberos.codec.changePwdData;

import java.nio.ByteBuffer;

import org.apache.directory.api.asn1.ber.AbstractContainer;
import org.apache.directory.shared.kerberos.messages.ChangePasswdData;


/**
 * The ChangePasswdData container.
 * 
 */
public class ChangePasswdDataContainer extends AbstractContainer
{
    /** An ChangePasswdData container */
    private ChangePasswdData chngPwdData;

    /**
     * Creates a new ChangePasswdDataContainer object.
     * @param stream The stream containing the data to decode
     */
    public ChangePasswdDataContainer( ByteBuffer stream )
    {
        super( stream );
        this.stateStack = new int[1];
        this.grammar = ChangePasswdDataGrammar.getInstance();
        setTransition( ChangePasswdDataStatesEnum.START_STATE );
    }


    /**
     * @return Returns the ApRep.
     */
    public ChangePasswdData getChngPwdData()
    {
        return chngPwdData;
    }

    
    /**
     * Set an ChangePasswdData Object into the container. It will be completed by the
     * KerberosDecoder.
     * 
     * @param chngPwdData The ChangePasswdData to set.
     */
    public void setChngPwdData( ChangePasswdData chngPwdData )
    {
        this.chngPwdData = chngPwdData;
    }
}
