/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
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
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 549315 $, $Date: 2007-06-20 18:13:53 -0700 (Wed, 20 Jun 2007) $
 */
public class ChangePasswordTcpEncoder extends ProtocolEncoderAdapter
{
    ChangePasswordReplyEncoder replyEncoder = new ChangePasswordReplyEncoder();
    ChangePasswordErrorEncoder errorEncoder = new ChangePasswordErrorEncoder();


    public void encode( IoSession session, Object message, ProtocolEncoderOutput out ) throws IOException
    {
        ByteBuffer buf = ByteBuffer.allocate( 512 );

        // make space for int length
        buf.putInt( 0 );

        if ( message instanceof ChangePasswordReply )
        {
            replyEncoder.encode( buf.buf(), ( ChangePasswordReply ) message );
        }
        else
        {
            if ( message instanceof ChangePasswordError )
            {
                errorEncoder.encode( buf.buf(), ( ChangePasswordError ) message );
            }
        }

        // mark position
        int pos = buf.position();

        // length is the data minus 4 bytes for the pre-pended length
        int recordLength = buf.position() - 4;

        // write the length
        buf.rewind();
        buf.putInt( recordLength );

        // set the position back before flipping the buffer
        buf.position( pos );
        buf.flip();

        out.write( buf );
    }
}
