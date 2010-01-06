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
package org.apache.directory.shared.ldap.client.api.protocol;

import java.nio.ByteBuffer;

import org.apache.directory.shared.ldap.codec.LdapMessageCodec;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

/**
 * 
 * A LDAP encoder.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class LdapProtocolEncoder implements ProtocolEncoder
{
    /**
     * Encode a Ldap request and write it to the remote server.
     * 
     * @param session The session containing the LdapMessageContainer
     * @param request The LDAP message we have to encode to a Byte stream
     * @param out The callback we have to invoke when the message has been encoded 
     */
    public void encode( IoSession session, Object request, ProtocolEncoderOutput out ) throws Exception
    {
        if ( request instanceof LdapMessageCodec )
        {
            LdapMessageCodec ldapRequest = (LdapMessageCodec)request;
            ByteBuffer bb = ldapRequest.encode( null );
            bb.flip();
            
            IoBuffer buffer = IoBuffer.allocate( bb.limit(), false );
            buffer.setAutoExpand( false );
            buffer.put( bb );
            buffer.flip();
            
            out.write( buffer );
        }
        else
        {
            throw new Exception();
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void dispose( IoSession session ) throws Exception
    {
        // Nothing to dispose
    }
}
