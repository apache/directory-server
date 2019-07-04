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
import java.util.Locale;

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

    private static final int DEFAULT_MAX_PDU_SIZE = 1024 * 7; // 7KB
    
    /** the maximum allowed PDU size for a Kerberos request */
    private int maxPduSize = DEFAULT_MAX_PDU_SIZE;
    
    private static final Logger LOG_KRB = LoggerFactory.getLogger( Loggers.KERBEROS_LOG.getName() );
    
    /** A speedup for logger */
    private static final boolean IS_DEBUG = LOG_KRB.isDebugEnabled();

    @Override
    public boolean doDecode( IoSession session, IoBuffer in, ProtocolDecoderOutput out ) throws Exception
    {
        ByteBuffer incomingBuf = in.buf();

        KerberosMessageContainer krbMsgContainer = ( KerberosMessageContainer ) session
            .getAttribute( KERBEROS_MESSAGE_CONTAINER );

        if ( krbMsgContainer == null )
        {
            krbMsgContainer = new KerberosMessageContainer();
            krbMsgContainer.setMaxPDUSize( maxPduSize );
            session.setAttribute( KERBEROS_MESSAGE_CONTAINER, krbMsgContainer );
            krbMsgContainer.setGathering( true );
            
            boolean tcp = !session.getTransportMetadata().isConnectionless();
            krbMsgContainer.setTCP( tcp );
            
            if ( tcp )
            {
                if ( incomingBuf.remaining() > 4 )
                {
                    int len = incomingBuf.getInt();
                    
                    if ( len > maxPduSize )
                    {
                        session.removeAttribute( KERBEROS_MESSAGE_CONTAINER );
                        
                        String err = "Request length %d exceeds allowed max PDU size %d";
                        err = String.format( Locale.ROOT, err, len, maxPduSize );
                        
                        throw new DecoderException( err );
                    }
                    
                    krbMsgContainer.setTcpLength( len );
                    incomingBuf.mark();
                    
                    ByteBuffer tmp = ByteBuffer.allocate( len );
                    tmp.put( incomingBuf );
                    
                    krbMsgContainer.setStream( tmp );
                }
                else
                {
                    String err = "Could not determine the length of TCP buffer";
                    LOG_KRB.warn( "{} {}", err, Strings.dumpBytes( incomingBuf.array() ) );
                    throw new IllegalStateException( err );
                }
            }
            else // UDP
            {
                krbMsgContainer.setStream( incomingBuf );
            }
        }
        else // must be a fragmented TCP stream, copy the incomingBuf into the existing buffer of the container
        {
            int totLen = incomingBuf.limit() + krbMsgContainer.getStream().position();
            if ( totLen > maxPduSize )
            {
                session.removeAttribute( KERBEROS_MESSAGE_CONTAINER );
                
                String err = "Total length of recieved bytes %d exceeds allowed max PDU size %d";
                err = String.format( Locale.ROOT, err, totLen, maxPduSize );
                
                throw new DecoderException( err );
            }
            
            krbMsgContainer.getStream().put( incomingBuf );
        }

        if ( krbMsgContainer.isTCP() )
        {
            int curLen = krbMsgContainer.getStream().position();
            if ( curLen < krbMsgContainer.getTcpLength() )
            {
                return false;
            }
        }

        try
        {
            ByteBuffer stream = krbMsgContainer.getStream();
            if ( stream.position() != 0 )
            {
                stream.flip();
            }
            
            Asn1Decoder.decode( stream, krbMsgContainer );
            
            if ( krbMsgContainer.getState() == TLVStateEnum.PDU_DECODED )
            {
                if ( IS_DEBUG )
                {
                    LOG_KRB.debug( "Decoded KerberosMessage : {}", krbMsgContainer.getMessage() );
                    incomingBuf.mark();
                }
                
                out.write( krbMsgContainer.getMessage() );
                
                return true;
            }
        }
        catch ( DecoderException de )
        {
            LOG_KRB.warn( "Error while decoding kerberos message", de );
            incomingBuf.clear();
            krbMsgContainer.clean();
            throw de;
        }
        finally
        {
            session.removeAttribute( KERBEROS_MESSAGE_CONTAINER );
        }
        
        throw new DecoderException( "Invalid buffer" );
   }

    
    /**
     * @return the maxPduSize
     */
    public int getMaxPduSize()
    {
        return maxPduSize;
    }

    
    /**
     * @param maxPduSize the maxPduSize to set
     */
    public void setMaxPduSize( int maxPduSize )
    {
        this.maxPduSize = maxPduSize;
    }
}
