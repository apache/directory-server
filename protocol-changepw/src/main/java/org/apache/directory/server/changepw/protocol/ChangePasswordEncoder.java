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

package org.apache.directory.server.changepw.protocol;


import java.io.IOException;

import org.apache.directory.server.changepw.io.ChangePasswordErrorEncoder;
import org.apache.directory.server.changepw.io.ChangePasswordReplyEncoder;
import org.apache.directory.server.changepw.messages.ChangePasswordError;
import org.apache.directory.server.changepw.messages.ChangePasswordReply;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;


public class ChangePasswordEncoder implements ProtocolEncoder
{
    public void encode( IoSession session, Object message, ProtocolEncoderOutput out ) throws IOException
    {
        ByteBuffer buf = ByteBuffer.allocate( 512 );

        if ( message instanceof ChangePasswordReply )
        {
            encodeReply( ( ChangePasswordReply ) message, buf );
        }
        else
        {
            if ( message instanceof ChangePasswordError )
            {
                encodeError( ( ChangePasswordError ) message, buf );
            }
        }

        buf.flip();

        out.write( buf );
    }


    private void encodeReply( ChangePasswordReply reply, ByteBuffer buf ) throws IOException
    {
        ChangePasswordReplyEncoder encoder = new ChangePasswordReplyEncoder();

        encoder.encode( buf.buf(), reply );
    }


    private void encodeError( ChangePasswordError error, ByteBuffer buf ) throws IOException
    {
        ChangePasswordErrorEncoder encoder = new ChangePasswordErrorEncoder();

        encoder.encode( buf.buf(), error );
    }


    public void dispose( IoSession arg0 ) throws Exception
    {
    }
}
