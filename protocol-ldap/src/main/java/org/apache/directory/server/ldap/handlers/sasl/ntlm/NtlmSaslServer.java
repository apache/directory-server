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
package org.apache.directory.server.ldap.handlers.sasl.ntlm;


import javax.naming.Context;
import javax.security.sasl.SaslException;

import org.apache.directory.api.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.apache.directory.api.ldap.model.message.BindRequest;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.interceptor.context.BindOperationContext;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.server.ldap.handlers.sasl.AbstractSaslServer;
import org.apache.directory.server.ldap.handlers.sasl.SaslConstants;


/**
 * A SaslServer implementation for NTLM based SASL mechanism.  This is
 * required unfortunately because the JDK's SASL provider does not support
 * this mechanism.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NtlmSaslServer extends AbstractSaslServer
{
    /** The different states during a NTLM negotiation */
    enum NegotiationState
    {
        INITIALIZED, TYPE_1_RECEIVED, TYPE_2_SENT, TYPE_3_RECEIVED, COMPLETED
    }

    /** The current state */
    private NegotiationState state = NegotiationState.INITIALIZED;
    private final NtlmProvider provider;


    public NtlmSaslServer( NtlmProvider provider, BindRequest bindRequest, LdapSession ldapSession,
        CoreSession adminSession )
    {
        super( ldapSession, adminSession, bindRequest );
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
                throw new IllegalStateException( I18n.err( I18n.ERR_38012_CANNOT_RECEIVE_NTLM_MESSAGE_BEFORE_TYPE_2_CHALLENGE ) );

            case TYPE_2_SENT:
                state = NegotiationState.TYPE_3_RECEIVED;
                break;

            case TYPE_3_RECEIVED:
                throw new IllegalStateException( I18n.err( I18n.ERR_38013_CANNOT_RECEIVE_NTLM_MESSAGE_AFTER_TYPE_3_RECEIVED ) );

            case COMPLETED:
                throw new IllegalStateException( I18n.err( I18n.ERR_38014_SASL_CHALLENGE_RESPONSE_COMPLETED ) );

            default:
                throw new IllegalStateException( "Unexpected negotiation state " + state );
        }
    }


    protected void responseSent()
    {
        switch ( state )
        {
            case INITIALIZED:
                throw new IllegalStateException( I18n.err( I18n.ERR_38015_CANNOT_SEND_TYPE_2_CHALLENGE_BEFORE_TYPE_1_RESPONSE ) );

            case TYPE_1_RECEIVED:
                state = NegotiationState.TYPE_2_SENT;
                break;

            case TYPE_2_SENT:
                throw new IllegalStateException( I18n.err( I18n.ERR_38016_CANNOT_RESEND_TYPE_2 ) );

            case TYPE_3_RECEIVED:
                state = NegotiationState.COMPLETED;
                break;

            case COMPLETED:
                throw new IllegalStateException( I18n.err( I18n.ERR_38014_SASL_CHALLENGE_RESPONSE_COMPLETED ) );

            default:
                throw new IllegalStateException( "Unexpected negotiation state " + state );
        }
    }


    /**
     * {@inheritDoc}
     */
    public byte[] evaluateResponse( byte[] response ) throws SaslException
    {
        if ( response == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_38017_RESPONSE_WAS_NULL ) );
        }

        if ( response.length == 0 )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_38018_RESPONSE_WITH_ZERO_BYTES ) );
        }

        responseRecieved();
        byte[] retval = null;

        switch ( state )
        {
            case TYPE_1_RECEIVED:
                try
                {
                    retval = provider.generateChallenge( response );
                }
                catch ( Exception e )
                {
                    throw new SaslException( I18n.err( I18n.ERR_38019_FAILURE_DURING_TYPE_1_MESSAGE ), e );
                }

                break;

            case TYPE_3_RECEIVED:
                boolean result;
                try
                {
                    result = provider.authenticate( response );
                    Dn dn = getBindRequest().getDn();
                    
                    if ( dn == null )
                    {
                        dn = new Dn( getLdapSession().getLdapServer().getDirectoryService().getSchemaManager() );
                    }
                    else if ( !dn.isSchemaAware() ) 
                    {
                        dn = new Dn( getLdapSession().getLdapServer().getDirectoryService().getSchemaManager(), dn );
                    }
                    
                    LdapPrincipal ldapPrincipal = new LdapPrincipal( getAdminSession().getDirectoryService()
                        .getSchemaManager(),
                        dn, AuthenticationLevel.STRONG );
                    getLdapSession().putSaslProperty( SaslConstants.SASL_AUTHENT_USER, ldapPrincipal );
                    getLdapSession()
                        .putSaslProperty( Context.SECURITY_PRINCIPAL, getBindRequest().getName() );
                }
                catch ( Exception e )
                {
                    throw new SaslException( I18n.err( I18n.ERR_38020_FAILURE_DURING_TYPE_3_MESSAGE ), e );
                }

                if ( !result )
                {
                    throw new SaslException( I18n.err( I18n.ERR_38021_AUTHENTICATE_FAILED_CREDENTIALS_INVALID ) );
                }

                break;

            case INITIALIZED:
            case TYPE_2_SENT:
            case COMPLETED:
            default:
                throw new IllegalStateException( "Unexpected negotiation state " + state );
        }

        responseSent();
        return retval;
    }


    /**
     * Try to authenticate the usr against the underlying LDAP server.
     */
    private CoreSession authenticate( String user, String password ) throws Exception
    {
        BindOperationContext bindContext = new BindOperationContext( getLdapSession().getCoreSession() );
        bindContext.setDn( new Dn( user ) );
        bindContext.setCredentials( Strings.getBytesUtf8( password ) );

        getAdminSession().getDirectoryService().getOperationManager().bind( bindContext );

        return bindContext.getSession();
    }


    /**
     * {@inheritDoc}
     */
    public boolean isComplete()
    {
        return state == NegotiationState.COMPLETED;
    }
}
