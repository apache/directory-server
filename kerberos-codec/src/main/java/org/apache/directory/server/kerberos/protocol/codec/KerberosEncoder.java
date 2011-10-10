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
package org.apache.directory.server.kerberos.protocol.codec;


import java.io.IOException;

import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.EncoderException;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KerberosEncoder extends ProtocolEncoderAdapter
{
    public void encode( IoSession session, Object message, ProtocolEncoderOutput out ) throws IOException
    {
        AbstractAsn1Object asn1Obj = ( AbstractAsn1Object ) message;
        boolean isTcp = !session.getTransportMetadata().isConnectionless();
        IoBuffer response = null;
        IoBuffer kerberosMessage = null;
        
        int responseLength = asn1Obj.computeLength();
        kerberosMessage = IoBuffer.allocate( responseLength );
        
        if ( isTcp )
        {
            response = IoBuffer.allocate( responseLength + 4 );
        }
        else
        {
            response = kerberosMessage;
        }

        try
        {
            asn1Obj.encode( kerberosMessage.buf() );

            if ( isTcp )
            { 
                response.putInt( responseLength );
                response.put( kerberosMessage.buf().array() );
            }

            response.flip();

            out.write( response );
        }
        catch( EncoderException e )
        {
            throw new IOException(e.getMessage());
        }
    }
}
