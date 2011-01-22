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
package org.apache.directory.server.ldap.handlers.bind;

import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;

import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.shared.ldap.message.BindRequest;
import org.apache.directory.shared.util.StringConstants;


/**
 * An abstract class containing common parts for the SaslServer local 
 * implementation, like the BindRequest;
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractSaslServer implements SaslServer 
{
    /** The associated BindRequest */
    private final BindRequest bindRequest;
    
    /** The associated LdapSession instance */
    private final LdapSession ldapSession;
    
    /** The admin session, used to authenticate users against the LDAP server */ 
    private CoreSession adminSession;
    
    
    public AbstractSaslServer( LdapSession ldapSession, CoreSession adminSession, BindRequest bindRequest )
    {
        this.bindRequest = bindRequest;
        this.ldapSession = ldapSession;
        this.adminSession = adminSession;
    }

    
    /**
     * {@inheritDoc}
     * 
     * NOT IMPLEMENTED
     */
    public byte[] unwrap( byte[] incoming, int offset, int len ) throws SaslException
    {
        return StringConstants.EMPTY_BYTES;
    }


    /**
     * {@inheritDoc}
     * 
     * NOT IMPLEMENTED
     */
    public byte[] wrap( byte[] outgoing, int offset, int len ) throws SaslException
    {
        return new byte[0];
    }

    
    /**
     *  @return the associated BindRequest object
     */
    public BindRequest getBindRequest()
    {
        return bindRequest;
    }

    
    /**
     *  @return the associated ioSession
     */
    public LdapSession getLdapSession()
    {
        return ldapSession;
    }


    /**
     *  @return the admin Session
     */
    public CoreSession getAdminSession()
    {
        return adminSession;
    }


    /**
     * {@inheritDoc}
     */
    public String getAuthorizationID()
    {
        return "";
    }


    /**
     * {@inheritDoc}
     */
    public Object getNegotiatedProperty( String propName )
    {
        return "";
    }


    /**
     * {@inheritDoc}
     */
    public void dispose() throws SaslException
    {
    }
}
