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

import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.ldap.codec.LdapDecoder;
import org.apache.directory.shared.ldap.codec.LdapMessage;
import org.apache.directory.shared.ldap.codec.LdapMessageContainer;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

/**
 * 
 * A LDAP message decoder. It is based on shared-ldap decoder.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class LdapProtocolDecoder implements ProtocolDecoder
{
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

        // Decode a LDAP PDU
        try
        {
            ldapDecoder.decode( buffer.buf(), ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
        }
        
        // get back the decoded message
        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        
        // Clean the container for the next decoding
        ( ( LdapMessageContainer ) ldapMessageContainer).clean();
        
        // Send back the message
        out.write( message );
        
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
