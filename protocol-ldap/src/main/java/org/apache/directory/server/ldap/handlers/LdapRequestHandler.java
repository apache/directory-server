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
package org.apache.directory.server.ldap.handlers;


import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.directory.api.ldap.model.exception.LdapOperationException;
import org.apache.directory.api.ldap.model.exception.LdapReferralException;
import org.apache.directory.api.ldap.model.message.AbandonRequest;
import org.apache.directory.api.ldap.model.message.BindRequest;
import org.apache.directory.api.ldap.model.message.BindResponse;
import org.apache.directory.api.ldap.model.message.BindResponseImpl;
import org.apache.directory.api.ldap.model.message.ExtendedRequest;
import org.apache.directory.api.ldap.model.message.LdapResult;
import org.apache.directory.api.ldap.model.message.Referral;
import org.apache.directory.api.ldap.model.message.ReferralImpl;
import org.apache.directory.api.ldap.model.message.Request;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.ResultResponse;
import org.apache.directory.api.ldap.model.message.ResultResponseRequest;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.shared.DefaultCoreSession;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.server.ldap.handlers.extended.StartTlsHandler;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.handler.demux.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A base class for all LDAP request handlers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class LdapRequestHandler<T extends Request> implements MessageHandler<T>
{
    /** The logger for this class */
    protected static final Logger LOG = LoggerFactory.getLogger( LdapRequestHandler.class );

    /** The reference on the Ldap server instance */
    protected LdapServer ldapServer;


    /**
     * @return The associated ldap server instance
     */
    public final LdapServer getLdapServer()
    {
        return ldapServer;
    }


    /**
     * Associates a Ldap server instance to the message handler
     * @param ldapServer the associated ldap server instance
     */
    public final void setLdapServer( LdapServer ldapServer )
    {
        this.ldapServer = ldapServer;
    }


    /**
     * Checks to see if confidentiality requirements are met.  If the 
     * LdapServer requires confidentiality and the SSLFilter is engaged
     * this will return true.  If confidentiality is not required this 
     * will return true.  If confidentially is required and the SSLFilter
     * is not engaged in the IoFilterChain this will return false.
     * 
     * This method is used by handlers to determine whether to send back
     * {@link ResultCodeEnum#CONFIDENTIALITY_REQUIRED} error responses back
     * to clients.
     * 
     * @param session the MINA IoSession to check for TLS security
     * @return true if confidentiality requirement is met, false otherwise
     */
    public final boolean isConfidentialityRequirementSatisfied( IoSession session )
    {

        if ( !ldapServer.isConfidentialityRequired() )
        {
            return true;
        }

        IoFilterChain chain = session.getFilterChain();
        return chain.contains( "sslFilter" );
    }


    public void rejectWithoutConfidentiality( IoSession session, ResultResponse resp )
    {
        LdapResult result = resp.getLdapResult();
        result.setResultCode( ResultCodeEnum.CONFIDENTIALITY_REQUIRED );
        result.setDiagnosticMessage( "Confidentiality (TLS secured connection) is required." );
        session.write( resp );
    }


    /**
     *{@inheritDoc} 
     */
    public final void handleMessage( IoSession session, T message ) throws Exception
    {
        LdapSession ldapSession = ldapServer.getLdapSessionManager().getLdapSession( session );

        if ( ldapSession == null )
        {
            // in some cases the session is becoming null though the client is sending the UnbindRequest
            // before closing
            LOG.info( "ignoring the message {} received from null session", message );
            return;
        }

        // First check that the client hasn't issued a previous BindRequest, unless it
        // was a SASL BindRequest
        if ( ldapSession.isAuthPending() )
        {
            // Only SASL BinRequest are allowed if we already are handling a 
            // SASL BindRequest
            if ( !( message instanceof BindRequest ) || ( ( BindRequest ) message ).isSimple()
                || ldapSession.isSimpleAuthPending() )
            {
                LOG.error( I18n.err( I18n.ERR_732 ) );
                BindResponse bindResponse = new BindResponseImpl( message.getMessageId() );
                LdapResult bindResult = bindResponse.getLdapResult();
                bindResult.setResultCode( ResultCodeEnum.UNWILLING_TO_PERFORM );
                bindResult.setDiagnosticMessage( I18n.err( I18n.ERR_732 ) );
                ldapSession.getIoSession().write( bindResponse );
                return;
            }
        }

        // TODO - session you get from LdapServer should have the ldapServer 
        // member already set no?  Should remove these lines where ever they
        // may be if that's the case.
        ldapSession.setLdapServer( ldapServer );

        // protect against insecure conns when confidentiality is required 
        if ( !isConfidentialityRequirementSatisfied( session ) )
        {
            if ( message instanceof ExtendedRequest )
            {
                // Reject all extended operations except StartTls  
                ExtendedRequest req = ( ExtendedRequest ) message;

                if ( !req.getRequestName().equals( StartTlsHandler.EXTENSION_OID ) )
                {
                    rejectWithoutConfidentiality( session, req.getResultResponse() );
                    return;
                }

                // Allow StartTls extended operations to go through
            }
            else if ( message instanceof ResultResponseRequest )
            {
                // Reject all other operations that have a result response  
                rejectWithoutConfidentiality( session, ( ( ResultResponseRequest ) message )
                    .getResultResponse() );
                return;
            }
            else
            // Just return from unbind, and abandon immediately
            {
                return;
            }
        }

        // We should check that the server allows anonymous requests
        // only if it's not a BindRequest
        if ( message instanceof BindRequest )
        {
            handle( ldapSession, message );
        }
        else
        {
            CoreSession coreSession = null;

            /*
             * All requests except bind automatically presume the authentication 
             * is anonymous if the session has not been authenticated.  Hence a
             * default bind is presumed as the anonymous identity.
             */
            if ( ldapSession.isAuthenticated() )
            {
                coreSession = ldapSession.getCoreSession();
                handle( ldapSession, message );
                return;
            }

            coreSession = getLdapServer().getDirectoryService().getSession();
            ldapSession.setCoreSession( coreSession );

            // Store the IoSession in the coreSession
            ( ( DefaultCoreSession ) coreSession ).setIoSession( ldapSession.getIoSession() );

            if ( message instanceof AbandonRequest )
            {
                return;
            }

            handle( ldapSession, message );
        }
    }


    /**
     * Handle a Ldap message associated with a session
     * 
     * @param session The associated session
     * @param message The message we have to handle
     * @throws Exception If there is an error during the processing of this message
     */
    public abstract void handle( LdapSession session, T message ) throws Exception;


    /**
     * Handles processing with referrals without ManageDsaIT decorator.
     * 
     * @param session The associated session
     * @param req The response
     * @param e The associated exception 
     */
    public void handleException( LdapSession session, ResultResponseRequest req, Exception e )
    {
        LdapResult result = req.getResultResponse().getLdapResult();

        /*
         * Set the result code or guess the best option.
         */
        ResultCodeEnum code;

        if ( e instanceof LdapOperationException )
        {
            code = ( ( LdapOperationException ) e ).getResultCode();
        }
        else
        {
            code = ResultCodeEnum.getBestEstimate( e, req.getType() );
        }

        result.setResultCode( code );

        /*
         * Setup the error message to put into the request and put entire
         * exception into the message if we are in debug mode.  Note we 
         * embed the result code name into the message.
         */
        String msg = code.toString() + ": failed for " + req + ": " + e.getLocalizedMessage();

        LOG.debug( msg, e );

        if ( LOG.isDebugEnabled() || ( code == ResultCodeEnum.OTHER ) )
        {
            msg += ":\n" + ExceptionUtils.getStackTrace( e );
        }

        result.setDiagnosticMessage( msg );

        if ( e instanceof LdapOperationException )
        {
            LdapOperationException ne = ( LdapOperationException ) e;

            // Add the matchedDN if necessary
            boolean setMatchedDn = code == ResultCodeEnum.NO_SUCH_OBJECT || code == ResultCodeEnum.ALIAS_PROBLEM
                || code == ResultCodeEnum.INVALID_DN_SYNTAX || code == ResultCodeEnum.ALIAS_DEREFERENCING_PROBLEM;

            if ( ( ne.getResolvedDn() != null ) && setMatchedDn )
            {
                result.setMatchedDn( ne.getResolvedDn() );
            }

            // Add the referrals if necessary
            if ( e instanceof LdapReferralException )
            {
                Referral referrals = new ReferralImpl();

                do
                {
                    String ref = ( ( LdapReferralException ) e ).getReferralInfo();
                    referrals.addLdapUrl( ref );
                }
                while ( ( ( LdapReferralException ) e ).skipReferral() );

                result.setReferral( referrals );
            }
        }

        session.getIoSession().write( req.getResultResponse() );
    }
}
