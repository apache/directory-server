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

package org.apache.dhcp.protocol;

import org.apache.dhcp.io.DhcpMessageEncoder;
import org.apache.dhcp.messages.DhcpMessage;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;


public class DhcpEncoder implements ProtocolEncoder
{
    public void encode( IoSession session, Object message, ProtocolEncoderOutput out )
    {
        DhcpMessageEncoder encoder = new DhcpMessageEncoder();
        
        ByteBuffer buf = ByteBuffer.allocate( 1024 );
        encoder.encode( buf.buf(), (DhcpMessage)message );
        
        buf.flip();
        
        out.write( buf );
    }

    public void dispose( IoSession arg0 ) throws Exception
    {
    }
}

