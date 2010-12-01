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
package org.apache.directory.server.kerberos.protocol;


import java.nio.ByteBuffer;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.kerberos.shared.io.decoder.KdcRequestDecoder;
import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.ber.tlv.TLVStateEnum;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.ldap.codec.LdapDecoder;
import org.apache.directory.shared.ldap.codec.LdapMessageContainer;
import org.apache.directory.shared.ldap.util.StringTools;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderAdapter;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A {@link CumulativeProtocolDecoder} which supports Kerberos operation over TCP,
 * by reassembling split packets prior to ASN.1 DER decoding.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KerberosTcpDecoder extends ProtocolDecoderAdapter
{
    /** The logger */
    private static Logger log = LoggerFactory.getLogger( LdapDecoder.class );

    /** A speedup for logger */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /** The message container for this instance */
    private LdapMessageContainer ldapMessageContainer;

    /** The ASN 1 deocder instance */
    private Asn1Decoder asn1Decoder;

    private KdcRequestDecoder decoder = new KdcRequestDecoder();

    private int maxObjectSize = 16384; // 16KB


    /**
     * Returns the allowed maximum size of the object to be decoded.
     * 
     * @return The max object size.
     */
    public int getMaxObjectSize()
    {
        return maxObjectSize;
    }


    /**
     * Sets the allowed maximum size of the object to be decoded.
     * If the size of the object to be decoded exceeds this value, this
     * decoder will throw a {@link IllegalArgumentException}.  The default
     * value is <tt>16384</tt> (16KB).
     * 
     * @param maxObjectSize 
     */
    public void setMaxObjectSize( int maxObjectSize )
    {
        if ( maxObjectSize <= 0 )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_634, maxObjectSize ) );
        }

        this.maxObjectSize = maxObjectSize;
    }


    /**
     * {@inheritDoc}
     */
    public void decode( IoSession session, IoBuffer in, ProtocolDecoderOutput out ) throws Exception
    {
        ByteBuffer buf = in.buf();
        int position = 0;
        
        LdapMessageContainer messageContainer = ( LdapMessageContainer ) session
            .getAttribute( "messageContainer" );
        
        while ( buf.hasRemaining() )
        {
            try
            {
                asn1Decoder.decode( buf, messageContainer );

                if ( IS_DEBUG )
                {
                    log.debug( "Decoding the PDU : " );

                    int size = buf.position();
                    buf.flip();

                    byte[] array = new byte[size - position];

                    for ( int i = position; i < size; i++ )
                    {
                        array[i] = buf.get();
                    }

                    position = size;

                    if ( array.length == 0 )
                    {
                        log.debug( "NULL buffer, what the HELL ???" );
                    }
                    else
                    {
                        log.debug( StringTools.dumpBytes( array ) );
                    }
                }

                if ( messageContainer.getState() == TLVStateEnum.PDU_DECODED )
                {
                    if ( IS_DEBUG )
                    {
                        log.debug( "Decoded LdapMessage : " + messageContainer.getMessage() );
                        buf.mark();
                    }

                    out.write( messageContainer.getMessage() );

                    messageContainer.clean();
                }
            }
            catch ( DecoderException de )
            {
                buf.clear();
                messageContainer.clean();

                throw de;
            }
        }
    }
}
