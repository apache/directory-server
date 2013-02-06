
package org.apache.directory.server.kerberos.protocol.codec;

import java.nio.ByteBuffer;

import org.apache.directory.api.asn1.AbstractAsn1Object;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

public class MinaKerberosEncoder extends ProtocolEncoderAdapter
{

    @Override
    public void encode( IoSession session, Object message, ProtocolEncoderOutput out ) throws Exception
    {
        AbstractAsn1Object asn1Obj = ( AbstractAsn1Object ) message;
        boolean isTcp = !session.getTransportMetadata().isConnectionless();

        ByteBuffer encodedByteBuf = KerberosEncoder.encode( asn1Obj, isTcp );
        IoBuffer buf = IoBuffer.allocate( encodedByteBuf.remaining() );
        buf.put( encodedByteBuf.array() );
        buf.flip();
        out.write( buf );
    }
}
