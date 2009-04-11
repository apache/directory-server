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

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

/**
 * 
 * The factory used to create the LDAP encoder and decoder.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class LdapProtocolCodecFactory implements ProtocolCodecFactory
{
    /** The Ldap encoder */
    private ProtocolEncoder ldapEncoder;
    
    /** The Ldap decoder */
    private ProtocolDecoder ldapDecoder;

    
    /**
     * 
     * Creates a new instance of LdapProtocolCodecFactory. It
     * creates the encoded an decoder instances.
     *
     */
    public LdapProtocolCodecFactory()
    {
        // Create the encoder.
        ldapEncoder = new LdapProtocolEncoder();
        ldapDecoder = new LdapProtocolDecoder();
    }
    
    
    /**
     * Get the Ldap decoder. 
     */
    public ProtocolDecoder getDecoder( IoSession session ) throws Exception
    {
        return ldapDecoder;
    }
    

    /**
     * Get the Ldap encoder.
     */
    public ProtocolEncoder getEncoder( IoSession session ) throws Exception
    {
        return ldapEncoder;
    }    
}
