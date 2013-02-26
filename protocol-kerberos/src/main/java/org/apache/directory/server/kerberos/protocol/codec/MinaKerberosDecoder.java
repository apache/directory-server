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


import java.nio.ByteBuffer;

import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.kerberos.codec.KerberosMessageContainer;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderAdapter;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
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

        KerberosMessageContainer kerberosMessageContainer = ( KerberosMessageContainer ) session
            .getAttribute( KERBEROS_MESSAGE_CONTAINER );

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
