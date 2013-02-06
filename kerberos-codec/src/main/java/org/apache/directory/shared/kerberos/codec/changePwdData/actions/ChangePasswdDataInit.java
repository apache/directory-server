package org.apache.directory.shared.kerberos.codec.changePwdData.actions;


import org.apache.directory.server.i18n.I18n;
import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.ber.grammar.GrammarAction;
import org.apache.directory.api.asn1.ber.tlv.TLV;
import org.apache.directory.shared.kerberos.codec.changePwdData.ChangePasswdDataContainer;
import org.apache.directory.shared.kerberos.messages.ChangePasswdData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to initialize the Ticket object
 */
public class ChangePasswdDataInit extends GrammarAction<ChangePasswdDataContainer>
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( ChangePasswdDataInit.class );

    /**
     * Instantiates a new TicketInit action.
     */
    public ChangePasswdDataInit()
    {
        super( "Ticket initialization" );
    }


    /**
     * {@inheritDoc}
     */
    public void action( ChangePasswdDataContainer chngPwdDataContainer ) throws DecoderException
    {
        TLV tlv = chngPwdDataContainer.getCurrentTLV();

        // The Length should not be null
        if ( tlv.getLength() == 0 )
        {
            LOG.error( I18n.err( I18n.ERR_744_NULL_PDU_LENGTH ) );

            // This will generate a PROTOCOL_ERROR
            throw new DecoderException( I18n.err( I18n.ERR_744_NULL_PDU_LENGTH ) );
        }

        // Create the Ticket now
        ChangePasswdData chngPwdData = new ChangePasswdData();

        chngPwdDataContainer.setChngPwdData( chngPwdData );
    }
}
