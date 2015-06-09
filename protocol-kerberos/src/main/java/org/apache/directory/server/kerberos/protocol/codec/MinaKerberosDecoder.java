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

import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.api.asn1.ber.tlv.TLVStateEnum;
import org.apache.directory.api.ldap.model.constants.Loggers;
import org.apache.directory.api.util.Strings;
import org.apache.directory.shared.kerberos.codec.KerberosMessageContainer;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MinaKerberosDecoder extends CumulativeProtocolDecoder
{
    /** the key used while storing message container in the session */
    private static final String KERBEROS_MESSAGE_CONTAINER = "kerberosMessageContainer";

    /** The ASN 1 decoder instance */
    private Asn1Decoder asn1Decoder = new Asn1Decoder();

    private static final int MAX_PDU_SIZE = 1024 * 7; // 7KB
    
    private static final Logger LOG_KRB = LoggerFactory.getLogger( Loggers.KERBEROS_LOG.getName() );
    
    /** A speedup for logger */
    private static final boolean IS_DEBUG = LOG_KRB.isDebugEnabled();

    @Override
    public boolean doDecode( IoSession session, IoBuffer in, ProtocolDecoderOutput out ) throws Exception
    {
        ByteBuffer buf = in.buf();

        KerberosMessageContainer kerberosMessageContainer = ( KerberosMessageContainer ) session
            .getAttribute( KERBEROS_MESSAGE_CONTAINER );

        if ( kerberosMessageContainer == null )
        {
            kerberosMessageContainer = new KerberosMessageContainer();
            kerberosMessageContainer.setMaxPDUSize( MAX_PDU_SIZE );
            session.setAttribute( KERBEROS_MESSAGE_CONTAINER, kerberosMessageContainer );
            kerberosMessageContainer.setGathering( true );
            
            boolean tcp = !session.getTransportMetadata().isConnectionless();
            kerberosMessageContainer.setTCP( tcp );
            
            if ( tcp )
            {
                if ( buf.remaining() > 4 )
                {
                    kerberosMessageContainer.setTcpLength( buf.getInt() );
                    buf.mark();
                }
                else
                {
                    String err = "Could not determine the length of TCP buffer";
                    LOG_KRB.warn( "{} {}", err, Strings.dumpBytes( buf.array() ) );
                    throw new IllegalStateException( err );
                }
            }
        }

        kerberosMessageContainer.setStream( buf );

        while ( buf.hasRemaining() )
        {
            try
            {
                asn1Decoder.decode( buf, kerberosMessageContainer );
                
                if ( kerberosMessageContainer.getState() == TLVStateEnum.PDU_DECODED )
                {
                    if ( IS_DEBUG )
                    {
                        LOG_KRB.debug( "Decoded KerberosMessage : " + kerberosMessageContainer.getMessage() );
                        buf.mark();
                    }
                    
                    out.write( kerberosMessageContainer.getMessage() );
                    
                    return true;
                }
            }
            catch ( DecoderException de )
            {
                LOG_KRB.warn( "Error while decoding kerberos message", de );
                buf.clear();
                kerberosMessageContainer.clean();
                throw de;
            }
            finally
            {
                session.removeAttribute( KERBEROS_MESSAGE_CONTAINER );
            }
       }
        
       return false;
   }
}
