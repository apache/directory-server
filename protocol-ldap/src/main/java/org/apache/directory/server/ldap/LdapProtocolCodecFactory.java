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
package org.apache.directory.server.ldap;


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.shared.ldap.codec.protocol.mina.LdapProtocolDecoder;
import org.apache.directory.shared.ldap.codec.protocol.mina.LdapProtocolEncoder;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;


/**
 * An LDAP BER Decoder/Encoder factory implementing {@link ProtocolCodecFactory}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
final class LdapProtocolCodecFactory implements ProtocolCodecFactory
{
    /** The tag stored into the session if we want to set a max PDU size */
    public final static String MAX_PDU_SIZE = "MAX_PDU_SIZE";

    private ProtocolDecoder decoder;
    private ProtocolEncoder encoder;


    /**
     * Creates a new instance of LdapProtocolCodecFactory.
     *
     * @param directoryService the {@link DirectoryService} for which this 
     * factory generates codecs.
     */
    public LdapProtocolCodecFactory( final DirectoryService directoryService )
    {
        encoder = new LdapProtocolEncoder( directoryService.getLdapCodecService() );
        decoder = new LdapProtocolDecoder( directoryService.getLdapCodecService() );
    }


    /**
     * {@inheritDoc}
     */
    public ProtocolEncoder getEncoder( IoSession session )
    {
        return encoder;
    }


    /**
     * {@inheritDoc}
     */
    public ProtocolDecoder getDecoder( IoSession session )
    {
        return decoder;
    }
}