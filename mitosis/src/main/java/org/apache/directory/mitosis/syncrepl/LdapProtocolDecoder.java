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
package org.apache.directory.mitosis.syncrepl;

import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.ber.tlv.TLVStateEnum;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.ldap.codec.LdapDecoder;
import org.apache.directory.shared.ldap.codec.LdapMessage;
import org.apache.directory.shared.ldap.codec.LdapMessageContainer;
import org.apache.directory.shared.ldap.util.StringTools;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * A LDAP message decoder. It is based on shared-ldap decoder.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class LdapProtocolDecoder implements ProtocolDecoder
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( LdapProtocolDecoder.class );
    
    /** A speedup for logger */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /**
     * Decode a Ldap request and write it to the remote server.
     * 
     * @param session The session containing the LdapMessageContainer
     * @param buffer The ByteBuffer containing the incoming bytes to decode
     * to a LDAP message
     * @param out The callback we have to invoke when the message has been decoded 
     */
    public void decode( IoSession session, IoBuffer buffer, ProtocolDecoderOutput out ) throws Exception
    {
        // Allocate a LdapMessage Container
        Asn1Decoder ldapDecoder = new LdapDecoder();
        IAsn1Container ldapMessageContainer = (LdapMessageContainer)session.getAttribute( "LDAP-Container" );
    	ByteBuffer buf = buffer.buf();

        
        while ( buf.hasRemaining() )
        {
            try
            {
                ldapDecoder.decode( buf, ldapMessageContainer );
    
                if ( IS_DEBUG )
                {
                    LOG.debug( "Decoding the PDU : " );
                	int size = buf.capacity();
                	
                	byte[] b = new byte[size];
                	
                	System.arraycopy( buf.array(), 0, b, 0, size );
                	
                	System.out.println( "Received buffer : " + StringTools.dumpBytes( b ) );
                }
                
                if ( ldapMessageContainer.getState() == TLVStateEnum.PDU_DECODED )
                {
                    // get back the decoded message
                    LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
                    
                    if ( IS_DEBUG )
                    {
                        LOG.debug( "Decoded LdapMessage : " + 
                        		( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage() );
                        buf.mark();
                    }
    
                    // Clean the container for the next decoding
                    ( ( LdapMessageContainer ) ldapMessageContainer).clean();
                    
                    System.out.println( "Decoded message : " + message );
                    
                    // Send back the message
                    out.write( message );
                }
            }
            catch ( DecoderException de )
            {
                buf.clear();
                ( ( LdapMessageContainer ) ldapMessageContainer).clean();
                throw de;
            }
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void finishDecode( IoSession session, ProtocolDecoderOutput out ) throws Exception
    {
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void dispose( IoSession session ) throws Exception
    {
    }
}
