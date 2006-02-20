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
package org.apache.directory.server.kerberos.protocol;


import java.io.IOException;

import org.apache.directory.server.kerberos.shared.io.decoder.KdcRequestDecoder;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;


public class KerberosDecoder implements ProtocolDecoder
{
    private KdcRequestDecoder decoder = new KdcRequestDecoder();


    public void decode( IoSession session, ByteBuffer in, ProtocolDecoderOutput out ) throws IOException
    {
        out.write( decoder.decode( in.buf() ) );
    }


    public void dispose( IoSession session ) throws Exception
    {
    }
}
