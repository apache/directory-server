/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.kerberos.protocol;

import java.io.IOException;

import org.apache.kerberos.io.encoder.ErrorMessageEncoder;
import org.apache.kerberos.io.encoder.KdcReplyEncoder;
import org.apache.kerberos.messages.ErrorMessage;
import org.apache.kerberos.messages.KdcReply;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

public class KerberosEncoder implements ProtocolEncoder
{
    private KdcReplyEncoder replyEncoder = new KdcReplyEncoder();
    private ErrorMessageEncoder errorEncoder = new ErrorMessageEncoder();

    public void encode( IoSession session, Object message, ProtocolEncoderOutput out ) throws IOException
    {
        ByteBuffer buf = ByteBuffer.allocate( 1024 );

        if ( message instanceof KdcReply )
        {
            replyEncoder.encode( (KdcReply) message, buf.buf() );
        }
        else
        {
            if ( message instanceof ErrorMessage )
            {
                errorEncoder.encode( (ErrorMessage) message, buf.buf() );
            }
        }

        buf.flip();

        out.write( buf );
    }

    public void dispose( IoSession arg0 ) throws Exception
    {
    }
}
