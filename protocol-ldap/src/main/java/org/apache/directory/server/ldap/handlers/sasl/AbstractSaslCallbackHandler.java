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
package org.apache.directory.server.ldap.handlers.sasl;


import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.directory.api.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.exception.LdapOperationException;
import org.apache.directory.api.ldap.model.message.BindRequest;
import org.apache.directory.api.ldap.model.message.Control;
import org.apache.directory.api.ldap.model.message.LdapResult;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.util.JndiUtils;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Base class for all SASL {@link CallbackHandler}s.  Implementations of SASL mechanisms
 * selectively override the methods relevant to their mechanism.
 *
 * @see javax.security.auth.callback.CallbackHandler
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractSaslCallbackHandler implements CallbackHandler
{
    /** The logger instance */
    private static final Logger LOG = LoggerFactory.getLogger( AbstractSaslCallbackHandler.class );

    /** An empty control array */
    private static final Control[] EMPTY = new Control[0];

    private String username;
    private String realm;

    /** The reference on the user ldap session */
    protected LdapSession ldapSession;

    /** The admin core session */
    protected CoreSession adminSession;

    /** A reference on the DirectoryService instance */
    protected final DirectoryService directoryService;

    /** The associated BindRequest */
    protected final BindRequest bindRequest;


    /**
     * Creates a new instance of AbstractSaslCallbackHandler.
     *
     * @param directoryService The DirectoryService instance
     * @param bindRequest The Bind request
     */
    protected AbstractSaslCallbackHandler( DirectoryService directoryService, BindRequest bindRequest )
    {
        this.directoryService = directoryService;
        this.bindRequest = bindRequest;
    }


    /**
     * Implementors use this method to access the username resulting from a callback.
     * Callback default name will be username, eg 'hnelson', for CRAM-MD5 and DIGEST-MD5.
     * The {@link NameCallback} is not used by GSSAPI.
     *
     * @return The user name
     */
    protected String getUsername()
    {
        return username;
    }


    /**
     * Implementors use this method to access the realm resulting from a callback.
     * Callback default text will be realm name, eg 'example.com', for DIGEST-MD5.
     * The {@link RealmCallback} is not used by GSSAPI nor by CRAM-MD5.
     *
     * @return The realm
     */
    protected String getRealm()
    {
        return realm;
    }


    /**
     * Implementors set the password based on a lookup, using the username and
     * realm as keys.
     * <ul>
     * <li>For DIGEST-MD5, lookup password based on username and realm.
     * <li>For CRAM-MD5, lookup password based on username.
     * <li>For GSSAPI, this callback is unused.
     * </ul>
     * @param username The username.
     * @param realm The realm.
     * @return The Password entry attribute resulting from the lookup. It may contain more than one password
     */
    protected abstract Attribute lookupPassword( String username, String realm );


    /**
     * Final check to authorize user.  Used by all SASL mechanisms.  This
     * is the only callback used by GSSAPI.
     *
     * Implementors use setAuthorizedID() to set the base Dn after canonicalization.
     * Implementors must setAuthorized() to <code>true</code> if authentication was successful.
     *
     * @param callback An {@link AuthorizeCallback}.
     * @throws Exception If the authorization failed
     */
    protected abstract void authorize( AuthorizeCallback callback ) throws Exception;


    /**
     * SaslServer will use this method to call various callbacks, depending on the SASL
     * mechanism in use for a session.
     *
     * @param callbacks An array of one or more callbacks.
     */
    @Override
    public void handle( Callback[] callbacks )
    {
        for ( int i = 0; i < callbacks.length; i++ )
        {
            Callback callback = callbacks[i];

            if ( LOG.isDebugEnabled() )
            {
                LOG.debug( "Processing callback {} of {}: {}", callback.getClass(), ( i + 1 ), callbacks.length );
            }

            if ( callback instanceof NameCallback )
            {
                NameCallback nameCB = ( NameCallback ) callback;
                LOG.debug( "NameCallback default name:  {}", nameCB.getDefaultName() );

                username = nameCB.getDefaultName();
            }
            else if ( callback instanceof RealmCallback )
            {
                RealmCallback realmCB = ( RealmCallback ) callback;
                LOG.debug( "RealmCallback default text:  {}", realmCB.getDefaultText() );

                realm = realmCB.getDefaultText();
            }
            else if ( callback instanceof PasswordCallback )
            {
                PasswordCallback passwordCB = ( PasswordCallback ) callback;
                Attribute userPassword = lookupPassword( getUsername(), getRealm() );

                if ( userPassword != null )
                {
                    // We assume that we have only one password available
                    byte[] password = userPassword.get().getBytes();

                    String strPassword = Strings.utf8ToString( password );
                    passwordCB.setPassword( strPassword.toCharArray() );
                }
            }
            else if ( callback instanceof AuthorizeCallback )
            {
                AuthorizeCallback authorizeCB = ( AuthorizeCallback ) callback;

                // hnelson (CRAM-MD5, DIGEST-MD5)
                // hnelson@EXAMPLE.COM (GSSAPI)
                LOG.debug( "AuthorizeCallback authnID:  {}", authorizeCB.getAuthenticationID() );

                // hnelson (CRAM-MD5, DIGEST-MD5)
                // hnelson@EXAMPLE.COM (GSSAPI)
                LOG.debug( "AuthorizeCallback authzID:  {}", authorizeCB.getAuthorizationID() );

                // null (CRAM-MD5, DIGEST-MD5, GSSAPI)
                LOG.debug( "AuthorizeCallback authorizedID:  {}", authorizeCB.getAuthorizedID() );

                // false (CRAM-MD5, DIGEST-MD5, GSSAPI)
                LOG.debug( "AuthorizeCallback isAuthorized:  {}", authorizeCB.isAuthorized() );

                try
                {
                    authorize( authorizeCB );
                }
                catch ( Exception e )
                {
                    // TODO - figure out how to handle this properly.
                    throw new RuntimeException( I18n.err( I18n.ERR_677 ), e );
                }
            }
        }
    }


    /**
     * Convenience method for acquiring an {@link LdapContext} for the client to use for the
     * duration of a session.
     *
     * @param session The current session.
     * @param bindRequest The current BindRequest.
     * @param env An environment to be used to acquire an {@link LdapContext}.
     * @return An {@link LdapContext} for the client.
     */
    protected LdapContext getContext( IoSession session, BindRequest bindRequest, Hashtable<String, Object> env )
    {
        LdapResult result = bindRequest.getResultResponse().getLdapResult();

        LdapContext ctx = null;

        try
        {
            Control[] connCtls = bindRequest.getControls().values().toArray( EMPTY );
            env.put( DirectoryService.JNDI_KEY, directoryService );
            ctx = new InitialLdapContext( env, JndiUtils.toJndiControls( directoryService.getLdapCodecService(),
                connCtls ) );
        }
        catch ( Exception e )
        {
            ResultCodeEnum code;
            Dn dn = null;

            if ( e instanceof LdapOperationException )
            {
                code = ( ( LdapOperationException ) e ).getResultCode();
                result.setResultCode( code );
                dn = ( ( LdapOperationException ) e ).getResolvedDn();
            }
            else
            {
                code = ResultCodeEnum.getBestEstimate( e, bindRequest.getType() );
                result.setResultCode( code );
            }

            String msg = "Bind failed: " + e.getLocalizedMessage();

            if ( LOG.isDebugEnabled() )
            {
                msg += ":\n" + ExceptionUtils.getStackTrace( e );
                msg += "\n\nBindRequest = \n" + bindRequest.toString();
            }

            if ( ( dn != null )
                && ( ( code == ResultCodeEnum.NO_SUCH_OBJECT ) || ( code == ResultCodeEnum.ALIAS_PROBLEM )
                    || ( code == ResultCodeEnum.INVALID_DN_SYNTAX ) || ( code == ResultCodeEnum.ALIAS_DEREFERENCING_PROBLEM ) ) )
            {
                result.setMatchedDn( dn );
            }

            result.setDiagnosticMessage( msg );
            session.write( bindRequest.getResultResponse() );
            ctx = null;
        }

        return ctx;
    }


    /**
     * Convenience method for getting an environment suitable for acquiring
     * an {@link LdapContext} for the client.
     *
     * @param session The current session.
     * @return An environment suitable for acquiring an {@link LdapContext} for the client.
     */
    protected Hashtable<String, Object> getEnvironment( IoSession session )
    {
        Hashtable<String, Object> env = new Hashtable<>();
        env.put( Context.PROVIDER_URL, session.getAttribute( "baseDn" ) );
        env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.directory.server.core.jndi.CoreContextFactory" );
        env.put( Context.SECURITY_PRINCIPAL, ServerDNConstants.ADMIN_SYSTEM_DN );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );
        env.put( Context.SECURITY_AUTHENTICATION, AuthenticationLevel.SIMPLE.toString() );

        return env;
    }
}
