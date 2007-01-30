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

package org.apache.directory.server.dns.protocol;


import org.apache.directory.server.dns.io.encoder.DnsMessageEncoder;
import org.apache.directory.server.dns.messages.DnsMessage;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;


/**
 * A ProtocolEncoder for use in the MINA framework that uses the 
 * DnsMessageEncoder to encode DnsMessages.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DnsEncoder implements ProtocolEncoder
{
    private DnsMessageEncoder encoder = new DnsMessageEncoder();


    public void encode( IoSession session, Object message, ProtocolEncoderOutput out )
    {
        ByteBuffer buf = ByteBuffer.allocate( 1024 );
        encoder.encode( buf, ( DnsMessage ) message );

        buf.flip();

        out.write( buf );
    }


    public void dispose( IoSession arg0 ) throws Exception
    {
    }
}
