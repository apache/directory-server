
package org.apache.directory.server.kerberos.protocol.codec;

import java.nio.ByteBuffer;

import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.kerberos.codec.KerberosMessageContainer;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderAdapter;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

public class MinaKerberosDecoder extends ProtocolDecoderAdapter
{
    /** the key used while storing message container in the session */
    private static final String KERBEROS_MESSAGE_CONTAINER = "kerberosMessageContainer";

    /** The ASN 1 decoder instance */
    private Asn1Decoder asn1Decoder = new Asn1Decoder();

    @Override
    public void decode( IoSession session, IoBuffer in, ProtocolDecoderOutput out ) throws Exception
    {
        ByteBuffer buf = in.buf();
        
        KerberosMessageContainer kerberosMessageContainer = ( KerberosMessageContainer ) session.getAttribute( KERBEROS_MESSAGE_CONTAINER );
        
        if ( kerberosMessageContainer == null )
        {
            kerberosMessageContainer = new KerberosMessageContainer();
            session.setAttribute( KERBEROS_MESSAGE_CONTAINER, kerberosMessageContainer );
            kerberosMessageContainer.setStream( buf );
            kerberosMessageContainer.setGathering( true );
            kerberosMessageContainer.setTCP( !session.getTransportMetadata().isConnectionless() );
        }

        try
        {
            Object obj = KerberosDecoder.decode( kerberosMessageContainer, asn1Decoder );
            out.write( obj );
        }
        finally
        {
            session.removeAttribute( KERBEROS_MESSAGE_CONTAINER );
        }
    }

}
