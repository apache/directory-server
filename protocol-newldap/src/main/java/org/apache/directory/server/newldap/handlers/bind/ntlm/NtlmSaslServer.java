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
package org.apache.directory.server.newldap.handlers.bind.ntlm;


import org.apache.directory.server.newldap.LdapSession;
import org.apache.directory.server.newldap.handlers.bind.AbstractSaslServer;
import org.apache.directory.shared.ldap.constants.SupportedSaslMechanisms;
import org.apache.directory.shared.ldap.message.BindRequest;

import javax.naming.Context;
import javax.security.sasl.SaslException;


/**
 * A SaslServer implementation for NTLM based SASL mechanism.  This is
 * required unfortunately because the JDK's SASL provider does not support
 * this mechanism.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $$Rev$$
 */
public class NtlmSaslServer extends AbstractSaslServer
{
    /** The different states during a NTLM negotiation */ 
    enum NegotiationState { INITIALIZED, TYPE_1_RECEIVED, TYPE_2_SENT, TYPE_3_RECEIVED, COMPLETED }

    /** The current state */
    private NegotiationState state = NegotiationState.INITIALIZED;
    private final NtlmProvider provider;

    
    public NtlmSaslServer( NtlmProvider provider, BindRequest bindRequest, LdapSession ldapSession )
    {
        super( ldapSession, null, bindRequest );
        this.provider = provider;
    }


    /**
     * {@inheritDoc}
     */
    public String getMechanismName()
    {
        return SupportedSaslMechanisms.NTLM;
    }


    protected void responseRecieved()
    {
        switch ( state )
        {
            case INITIALIZED:
                state = NegotiationState.TYPE_1_RECEIVED;
                break;
                
            case TYPE_1_RECEIVED:
                throw new IllegalStateException( "Cannot receive NTLM message before sending Type 2 challenge." );
                
            case TYPE_2_SENT:
                state = NegotiationState.TYPE_3_RECEIVED;
                break;
                
            case TYPE_3_RECEIVED:
                throw new IllegalStateException( "Cannot receive NTLM message after Type 3 has been received." );
                
            case COMPLETED:
                throw new IllegalStateException( "Sasl challenge response already completed." );
        }
    }


    protected void responseSent()
    {
        switch ( state )
        {
            case INITIALIZED:
                throw new IllegalStateException( "Cannot send Type 2 challenge before Type 1 response." );
                
            case TYPE_1_RECEIVED:
                state = NegotiationState.TYPE_2_SENT;
                break;
                
            case TYPE_2_SENT:
                throw new IllegalStateException( "Cannot send Type 2 after it's already sent." );
                
            case TYPE_3_RECEIVED:
                state = NegotiationState.COMPLETED;
                break;
                
            case COMPLETED:
                throw new IllegalStateException( "Sasl challenge response already completed." );
        }
    }


    /**
     * {@inheritDoc}
     */
    public byte[] evaluateResponse( byte[] response ) throws SaslException
    {
        if ( response == null )
        {
            throw new NullPointerException( "response was null" );
        }

        if ( response.length == 0 )
        {
            throw new IllegalArgumentException( "response with zero bytes" );
        }

        responseRecieved();
        byte[] retval = null;

        switch ( state )
        {
            case TYPE_1_RECEIVED:
                try
                {
                    retval = provider.generateChallenge( getLdapSession().getIoSession(), response );
                }
                catch ( Exception e )
                {
                    throw new SaslException( "There was a failure during NTLM Type 1 message handling.", e );
                }
                
                break;
                
            case TYPE_3_RECEIVED:
                boolean result;
                try
                {
                    result = provider.authenticate( getLdapSession().getIoSession(), response );
                    getLdapSession().getIoSession().setAttribute( Context.SECURITY_PRINCIPAL, getBindRequest().getName().toString() );
                }
                catch ( Exception e )
                {
                    throw new SaslException( "There was a failure during NTLM Type 3 message handling.", e );
                }

                if ( ! result )
                {
                    throw new SaslException( "Authentication occurred but the credentials were invalid." );
                }
                
                break;
        }
        
        responseSent();
        return retval;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isComplete()
    {
        return state == NegotiationState.COMPLETED;
    }
}
