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


import java.util.Map;

import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.LdapPrincipal;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.LdapProtocolUtils;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.server.ldap.handlers.bind.MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.SaslConstants;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapAuthenticationException;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.model.exception.LdapUnwillingToPerformException;
import org.apache.directory.shared.ldap.model.message.*;
import org.apache.directory.shared.ldap.model.message.BindResponse;
import org.apache.directory.shared.ldap.model.message.LdapResult;
import org.apache.directory.shared.ldap.model.message.BindRequest;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.util.StringConstants;
import org.apache.directory.shared.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A single reply handler for {@link BindRequest}s.
 *
 * Implements server-side of RFC 2222, sections 4.2 and 4.3.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class BindHandler extends LdapRequestHandler<BindRequest>
{
    private static final Logger LOG = LoggerFactory.getLogger( BindHandler.class );

    /** A Hashed Adapter mapping SASL mechanisms to their handlers. */
    private Map<String, MechanismHandler> handlers;


    /**
     * Set the mechanisms handler map.
     * 
     * @param handlers The associations btween a machanism and its handler
     */
    public void setSaslMechanismHandlers( Map<String, MechanismHandler> handlers )
    {
        this.handlers = handlers;
    }


    /**
     * Handle the Simple authentication.
     *
     * @param ldapSession The associated Session
     * @param bindRequest The BindRequest received
     * @throws Exception If the authentication cannot be done
     */
    // This will suppress PMD.EmptyCatchBlock warnings in this method
    @SuppressWarnings("PMD.EmptyCatchBlock")
    public void handleSimpleAuth( LdapSession ldapSession, BindRequest bindRequest ) throws Exception
    {
        // if the user is already bound, we have to unbind him
        if ( ldapSession.isAuthenticated() )
        {
            // We already have a bound session for this user. We have to
            // abandon it first.
            ldapSession.getCoreSession().unbind();
        }

        // Set the status to SimpleAuthPending
        ldapSession.setSimpleAuthPending();

        // Now, bind the user

        // create a new Bind context, with a null session, as we don't have 
        // any context yet.
        BindOperationContext bindContext = new BindOperationContext( null );

        // Stores the Dn of the user to check, and its password
        bindContext.setDn( bindRequest.getName() );
        bindContext.setCredentials( bindRequest.getCredentials() );

        // Stores the request controls into the operation context
        LdapProtocolUtils.setRequestControls( bindContext, bindRequest );

        try
        {
            /*
             * Referral handling as specified by RFC 3296 here:
             *    
             *      http://www.faqs.org/rfcs/rfc3296.html
             *      
             * See section 5.6.1 where if the bind principal Dn is a referral
             * we return an invalidCredentials result response.  Optionally we
             * could support delegated authentication in the future with this
             * potential.  See the following JIRA for more on this possibility:
             * 
             *      https://issues.apache.org/jira/browse/DIRSERVER-1217
             *      
             * NOTE: if this is done then this handler should extend the 
             * a modified form of the ReferralAwareRequestHandler so it can 
             * detect conditions where ancestors of the Dn are referrals
             * and delegate appropriately.
             */
            Entry principalEntry = null;

            try
            {
                principalEntry = getLdapServer().getDirectoryService().getAdminSession().lookup( bindRequest.getName() );
            }
            catch ( LdapException le )
            {
                // this is OK
            }

            if ( principalEntry == null )
            {
                LOG.info( "The {} principalDN cannot be found in the server : bind failure.", bindRequest.getName() );
            }
            else if ( ( ( ClonedServerEntry ) principalEntry ).getOriginalEntry().contains( SchemaConstants.OBJECT_CLASS_AT,
                SchemaConstants.REFERRAL_OC ) )
            {
                LOG.info( "Bind principalDn points to referral." );
                LdapResult result = bindRequest.getResultResponse().getLdapResult();
                result.setErrorMessage( "Bind principalDn points to referral." );
                result.setResultCode( ResultCodeEnum.INVALID_CREDENTIALS );
                ldapSession.getIoSession().write( bindRequest.getResultResponse() );
                return;
            }

            // TODO - might cause issues since lookups are not returning all 
            // attributes right now - this is an optimization that can be 
            // enabled later after determining whether or not this will cause
            // issues.
            // reuse the looked up entry so we don't incur another lookup
            // opContext.setEntry( principalEntry );

            // And call the OperationManager bind operation.
            getLdapServer().getDirectoryService().getOperationManager().bind( bindContext );

            // As a result, store the created session in the Core Session
            ldapSession.setCoreSession( bindContext.getSession() );

            // And set the current state accordingly
            if ( !ldapSession.getCoreSession().isAnonymous() )
            {
                ldapSession.setAuthenticated();
            }
            else
            {
                ldapSession.setAnonymous();
            }

            // Return the successful response
            bindRequest.getResultResponse().addAllControls( bindContext.getResponseControls() );
            sendBindSuccess( ldapSession, bindRequest, null );
        }
        catch ( Exception e )
        {
            // Something went wrong. Write back an error message
            // For BindRequest, it should be an InvalidCredentials, 
            // no matter what kind of exception we got.
            ResultCodeEnum code = null;
            LdapResult result = bindRequest.getResultResponse().getLdapResult();

            if ( e instanceof LdapUnwillingToPerformException )
            {
                code = ResultCodeEnum.UNWILLING_TO_PERFORM;
                result.setResultCode( code );
            }
            else if ( e instanceof LdapInvalidDnException)
            {
                code = ResultCodeEnum.INVALID_DN_SYNTAX;
                result.setResultCode( code );
            }
            else
            {
                code = ResultCodeEnum.INVALID_CREDENTIALS;
                result.setResultCode( code );
            }

            String msg = code.toString() + ": Bind failed: " + e.getLocalizedMessage();

            if ( LOG.isDebugEnabled() )
            {
                msg += ":\n" + ExceptionUtils.getStackTrace( e );
                msg += "\n\nBindRequest = \n" + bindRequest.toString();
            }

            Dn dn = null;

            if ( e instanceof LdapAuthenticationException)
            {
                dn = ( ( LdapAuthenticationException ) e ).getResolvedDn();
            }

            if ( ( dn != null )
                && ( ( code == ResultCodeEnum.NO_SUCH_OBJECT ) || ( code == ResultCodeEnum.ALIAS_PROBLEM )
                    || ( code == ResultCodeEnum.INVALID_DN_SYNTAX ) || ( code == ResultCodeEnum.ALIAS_DEREFERENCING_PROBLEM ) ) )
            {
                result.setMatchedDn( dn );
            }

            result.setErrorMessage( msg );
            bindRequest.getResultResponse().addAllControls( bindContext.getResponseControls() );
            ldapSession.getIoSession().write( bindRequest.getResultResponse() );
        }
        finally
        {
            // Reset LDAP session bind status to anonymous if authentication failed
            if ( !ldapSession.isAuthenticated() )
            {
                ldapSession.setAnonymous();
            }
        }
    }


    /**
     * Check if the mechanism exists.
     */
    private boolean checkMechanism( String saslMechanism ) throws Exception
    {
        // Guard clause:  Reject unsupported SASL mechanisms.
        if ( !ldapServer.getSupportedMechanisms().contains( saslMechanism ) )
        {
            LOG.error( I18n.err( I18n.ERR_160, saslMechanism ) );

            return false;
        }
        else
        {
            return true;
        }
    }


    /**
     * For challenge/response exchange, generate the challenge. 
     * If the exchange is complete then send bind success.
     *
     * @param ldapSession
     * @param ss
     * @param bindRequest
     */
    private void generateSaslChallengeOrComplete( LdapSession ldapSession, SaslServer ss,
        BindRequest bindRequest ) throws Exception
    {
        LdapResult result = bindRequest.getResultResponse().getLdapResult();

        // SaslServer will throw an exception if the credentials are null.
        if ( bindRequest.getCredentials() == null )
        {
            bindRequest.setCredentials( StringConstants.EMPTY_BYTES );
        }

        try
        {
            // Compute the challenge
            byte[] tokenBytes = ss.evaluateResponse( bindRequest.getCredentials() );

            if ( ss.isComplete() )
            {
                // This is the end of the C/R exchange
                if ( tokenBytes != null )
                {
                    /*
                     * There may be a token to return to the client.  We set it here
                     * so it will be returned in a SUCCESS message, after an LdapContext
                     * has been initialized for the client.
                     */
                    ldapSession.putSaslProperty( SaslConstants.SASL_CREDS, tokenBytes );
                }

                LdapPrincipal ldapPrincipal = ( LdapPrincipal ) ldapSession
                    .getSaslProperty( SaslConstants.SASL_AUTHENT_USER );

                if ( ldapPrincipal != null )
                {
                    DirectoryService ds = ldapSession.getLdapServer().getDirectoryService();
                    String saslMechanism = bindRequest.getSaslMechanism();
                    CoreSession userSession = ds.getSession( ldapPrincipal.getDN(), ldapPrincipal.getUserPassword(),
                        saslMechanism, null );

                    // Set the user session into the ldap session 
                    ldapSession.setCoreSession( userSession );
                }

                // Mark the user as authenticated
                ldapSession.setAuthenticated();

                // Call the cleanup method for the selected mechanism
                MechanismHandler handler = ( MechanismHandler ) ldapSession
                    .getSaslProperty( SaslConstants.SASL_MECH_HANDLER );
                handler.cleanup( ldapSession );

                // Return the successful response
                sendBindSuccess( ldapSession, bindRequest, tokenBytes );
            }
            else
            {
                // The SASL bind must continue, we are sending the computed challenge
                LOG.info( "Continuation token had length " + tokenBytes.length );

                // Build the response
                result.setResultCode( ResultCodeEnum.SASL_BIND_IN_PROGRESS );
                BindResponse resp = ( BindResponse ) bindRequest.getResultResponse();

                // Store the challenge
                resp.setServerSaslCreds( tokenBytes );

                // Switch to SASLAuthPending
                ldapSession.setSaslAuthPending();

                // And write back the response
                ldapSession.getIoSession().write( resp );

                LOG.debug( "Returning final authentication data to client to complete context." );
            }
        }
        catch ( SaslException se )
        {
            sendInvalidCredentials( ldapSession, bindRequest, se );
        }
    }


    /**
     * Send back an AUTH-METH-NOT-SUPPORTED error message to the client
     */
    private void sendAuthMethNotSupported( LdapSession ldapSession, BindRequest bindRequest )
    {
        // First, r-einit the state to Anonymous, and clear the
        // saslProperty map
        ldapSession.clearSaslProperties();
        ldapSession.setAnonymous();

        // And send the response to the client
        LdapResult bindResult = bindRequest.getResultResponse().getLdapResult();
        bindResult.setResultCode( ResultCodeEnum.AUTH_METHOD_NOT_SUPPORTED );
        bindResult.setErrorMessage( ResultCodeEnum.AUTH_METHOD_NOT_SUPPORTED.toString() + ": "
            + bindRequest.getSaslMechanism() + " is not a supported mechanism." );

        // Write back the error
        ldapSession.getIoSession().write( bindRequest.getResultResponse() );
    }


    /**
     * Send back an INVALID-CREDENTIAL error message to the user. If we have an exception
     * as a third argument, then send back the associated message to the client. 
     */
    private void sendInvalidCredentials( LdapSession ldapSession, BindRequest bindRequest, Exception e )
    {
        LdapResult result = bindRequest.getResultResponse().getLdapResult();

        String message = "";

        if ( e != null )
        {
            message = ResultCodeEnum.INVALID_CREDENTIALS + ": " + e.getLocalizedMessage();
        }
        else
        {
            message = ResultCodeEnum.INVALID_CREDENTIALS.toString();
        }

        LOG.error( message );
        result.setResultCode( ResultCodeEnum.INVALID_CREDENTIALS );
        result.setErrorMessage( message );

        // Reinitialize the state to Anonymous and clear the sasl properties
        ldapSession.clearSaslProperties();
        ldapSession.setAnonymous();

        // Write back the error response
        ldapSession.getIoSession().write( bindRequest.getResultResponse() );
    }


    /**
     * Send a SUCCESS message back to the client.
     */
    private void sendBindSuccess( LdapSession ldapSession, BindRequest bindRequest, byte[] tokenBytes )
    {
        // Return the successful response
        BindResponse response = ( BindResponse ) bindRequest.getResultResponse();
        response.getLdapResult().setResultCode( ResultCodeEnum.SUCCESS );
        response.setServerSaslCreds( tokenBytes );

        if ( !ldapSession.getCoreSession().isAnonymous() )
        {
            // If we have not been asked to authenticate as Anonymous, authenticate the user
            ldapSession.setAuthenticated();
        }
        else
        {
            // Otherwise, switch back to Anonymous
            ldapSession.setAnonymous();
        }

        // Clean the SaslProperties, we don't need them anymore
        MechanismHandler handler = ( MechanismHandler ) ldapSession.getSaslProperty( SaslConstants.SASL_MECH_HANDLER );

        if ( handler != null )
        {
            handler.cleanup( ldapSession );
        }

        ldapSession.getIoSession().write( response );

        LOG.debug( "Returned SUCCESS message: {}.", response );
    }


    private void handleSaslAuthPending( LdapSession ldapSession, BindRequest bindRequest ) throws Exception
    {
        // First, check that we have the same mechanism
        String saslMechanism = bindRequest.getSaslMechanism();

        // The empty mechanism is also a request for a new Bind session
        if ( Strings.isEmpty(saslMechanism)
            || !ldapSession.getSaslProperty( SaslConstants.SASL_MECH ).equals( saslMechanism ) )
        {
            sendAuthMethNotSupported( ldapSession, bindRequest );
            return;
        }

        // We have already received a first BindRequest, and sent back some challenge.
        // First, check if the mechanism is the same
        MechanismHandler mechanismHandler = handlers.get( saslMechanism );

        if ( mechanismHandler == null )
        {
            String message = I18n.err( I18n.ERR_161, saslMechanism );

            // Clear the saslProperties, and move to the anonymous state
            ldapSession.clearSaslProperties();
            ldapSession.setAnonymous();

            LOG.error( message );
            throw new IllegalArgumentException( message );
        }

        // Get the previously created SaslServer instance
        SaslServer ss = mechanismHandler.handleMechanism( ldapSession, bindRequest );

        generateSaslChallengeOrComplete( ldapSession, ss, bindRequest );
    }


    /**
     * Handle the SASL authentication. If the mechanism is known, we are
     * facing three cases :
     * <ul>
     * <li>The user does not has a session yet</li>
     * <li>The user already has a session</li>
     * <li>The user has started a SASL negotiation</li>
     * </lu><br/>
     * 
     * In the first case, we initiate a SaslBind session, which will be used all
     * along the negotiation.<br/>
     * In the second case, we first have to unbind the user, and initiate a new
     * SaslBind session.<br/>
     * In the third case, we have sub cases :
     * <ul>
     * <li>The mechanism is not provided : that means the user want to reset the
     * current negotiation. We move back to an Anonymous state</li>
     * <li>The mechanism is provided : the user is initializing a new negotiation
     * with another mechanism. The current SaslBind session is reinitialized</li>
     * <li></li>
     * </ul><br/>
     *
     * @param ldapSession The associated Session
     * @param bindRequest The BindRequest received
     * @throws Exception If the authentication cannot be done
     */
    public void handleSaslAuth( LdapSession ldapSession, BindRequest bindRequest ) throws Exception
    {
        String saslMechanism = bindRequest.getSaslMechanism();

        // Case #2 : the user does have a session. We have to unbind him
        if ( ldapSession.isAuthenticated() )
        {
            // We already have a bound session for this user. We have to
            // close the previous session first.
            ldapSession.getCoreSession().unbind();

            // Reset the status to Anonymous
            ldapSession.setAnonymous();

            // Clean the sasl properties
            ldapSession.clearSaslProperties();

            // Now we can continue as if the client was Anonymous from the beginning
        }

        // case #1 : The user does not have a session.
        if ( ldapSession.isAnonymous() )
        {
            // fist check that the mechanism exists
            if ( !checkMechanism( saslMechanism ) )
            {
                // get out !
                sendAuthMethNotSupported( ldapSession, bindRequest );
                
                return;
            }
            
            // Store the mechanism in the ldap session
            ldapSession.putSaslProperty( SaslConstants.SASL_MECH, saslMechanism );
            
            // Get the handler for this mechanism
            MechanismHandler mechanismHandler = handlers.get( saslMechanism );
            
            // Store the mechanism handler in the salsProperties
            ldapSession.putSaslProperty( SaslConstants.SASL_MECH_HANDLER, mechanismHandler );
            
            // Initialize the mechanism specific data
            mechanismHandler.init( ldapSession );
            
            // Get the SaslServer instance which manage the C/R exchange
            SaslServer ss = mechanismHandler.handleMechanism( ldapSession, bindRequest );
            
            // We have to generate a challenge
            generateSaslChallengeOrComplete( ldapSession, ss, bindRequest );
            
            // And get back
            return;
        }
        else if ( ldapSession.isAuthPending() )
        {
            try
            {
                handleSaslAuthPending( ldapSession, bindRequest );
            }
            catch ( SaslException se )
            {
                sendInvalidCredentials( ldapSession, bindRequest, se );
            }

            return;
        }
    }


    /**
     * Deal with a received BindRequest
     * 
     * @param ldapSession The current session
     * @param bindRequest The received BindRequest
     * @throws Exception If the authentication cannot be handled
     */
    @Override
    public void handle( LdapSession ldapSession, BindRequest bindRequest ) throws Exception
    {
        LOG.debug( "Received: {}", bindRequest );

        // Guard clause:  LDAP version 3
        if ( !bindRequest.getVersion3() )
        {
            LOG.error( I18n.err( I18n.ERR_162 ) );
            LdapResult bindResult = bindRequest.getResultResponse().getLdapResult();
            bindResult.setResultCode( ResultCodeEnum.PROTOCOL_ERROR );
            bindResult.setErrorMessage( I18n.err( I18n.ERR_163 ) );
            ldapSession.getIoSession().write( bindRequest.getResultResponse() );
            return;
        }

        // Deal with the two kinds of authentication : Simple and SASL
        if ( bindRequest.isSimple() )
        {
            handleSimpleAuth( ldapSession, bindRequest );
        }
        else
        {
            handleSaslAuth( ldapSession, bindRequest );
        }
    }
}
