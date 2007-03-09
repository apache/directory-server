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
package org.apache.directory.server.ldap.support.bind;


import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.spi.InitialContextFactory;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;

import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.message.BindRequest;
import org.apache.directory.shared.ldap.message.Control;
import org.apache.directory.shared.ldap.message.LdapResult;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.ExceptionUtils;
import org.apache.mina.common.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Base class for all SASL {@link CallbackHandler}s.  Implementations of SASL mechanisms
 * selectively override the methods relevant to their mechanism.
 * 
 * @see javax.security.auth.callback.CallbackHandler
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class AbstractSaslCallbackHandler implements CallbackHandler
{
    private static final Logger log = LoggerFactory.getLogger( AbstractSaslCallbackHandler.class );

    private static final Control[] EMPTY = new Control[0];

    private String username;
    private String realm;


    /**
     * Implementors use this method to access the username resulting from a callback.
     * Callback default name will be username, eg 'hnelson', for CRAM-MD5 and DIGEST-MD5.
     * The {@link NameCallback} is not used by GSSAPI.
     */
    protected String getUsername()
    {
        return username;
    }


    /**
     * Implementors use this method to access the realm resulting from a callback.
     * Callback default text will be realm name, eg 'example.com', for DIGEST-MD5.
     * The {@link RealmCallback} is not used by GSSAPI nor by CRAM-MD5.
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
     * @return The password resulting from the lookup.
     */
    protected abstract String lookupPassword( String username, String realm );


    /**
     * Final check to authorize user.  Used by all SASL mechanisms.  This
     * is the only callback used by GSSAPI.
     * 
     * Implementors use setAuthorizedID() to set the base DN after canonicalization.
     * Implementors must setAuthorized() to <code>true</code> if authentication was successful.
     * 
     * @param callback An {@link AuthorizeCallback}.
     */
    protected abstract void authorize( AuthorizeCallback callback );


    /**
     * SaslServer will use this method to call various callbacks, depending on the SASL
     * mechanism in use for a session.
     * 
     * @param callbacks An array of one or more callbacks.
     */
    public void handle( Callback[] callbacks )
    {
        for ( int i = 0; i < callbacks.length; i++ )
        {
            Callback callback = callbacks[i];

            log.debug( "Processing callback " + ( i + 1 ) + " of " + callbacks.length + ":  "
                + callback.getClass().toString() );

            if ( callback instanceof NameCallback )
            {
                NameCallback nameCB = ( NameCallback ) callback;
                log.debug( "NameCallback default name   " + nameCB.getDefaultName() );

                username = nameCB.getDefaultName();
            }
            else if ( callback instanceof RealmCallback )
            {
                RealmCallback realmCB = ( RealmCallback ) callback;
                log.debug( "RealmCallback default text  " + realmCB.getDefaultText() );

                realm = realmCB.getDefaultText();
            }
            else if ( callback instanceof PasswordCallback )
            {
                PasswordCallback passwordCB = ( PasswordCallback ) callback;
                String userPassword = lookupPassword( getUsername(), getRealm() );

                if ( userPassword != null )
                {
                    passwordCB.setPassword( userPassword.toCharArray() );
                }
            }
            else if ( callback instanceof AuthorizeCallback )
            {
                AuthorizeCallback authorizeCB = ( AuthorizeCallback ) callback;

                // hnelson (CRAM-MD5, DIGEST-MD5)
                // hnelson@EXAMPLE.COM (GSSAPI)
                log.debug( "AuthorizeCallback authnID      " + authorizeCB.getAuthenticationID() );

                // hnelson (CRAM-MD5, DIGEST-MD5)
                // hnelson@EXAMPLE.COM (GSSAPI)
                log.debug( "AuthorizeCallback authzID      " + authorizeCB.getAuthorizationID() );

                // null (CRAM-MD5, DIGEST-MD5, GSSAPI)
                log.debug( "AuthorizeCallback authorizedID " + authorizeCB.getAuthorizedID() );

                // false (CRAM-MD5, DIGEST-MD5, GSSAPI)
                log.debug( "AuthorizeCallback isAuthorized " + authorizeCB.isAuthorized() );

                authorize( authorizeCB );
            }
        }
    }


    /**
     * Convenience method for acquiring an {@link LdapContext} for the client to use for the
     * duration of a session.
     * 
     * @param session The current session.
     * @param message The current message.
     * @param env An environment to be used to acquire an {@link LdapContext}.
     * @return An {@link LdapContext} for the client.
     */
    protected LdapContext getContext( IoSession session, Object message, Hashtable env )
    {
        BindRequest request = ( BindRequest ) message;
        LdapResult result = request.getResultResponse().getLdapResult();

        LdapContext ctx = null;

        try
        {
            if ( env.containsKey( "server.use.factory.instance" ) )
            {
                InitialContextFactory factory = ( InitialContextFactory ) env.get( "server.use.factory.instance" );

                if ( factory == null )
                {
                    throw new NullPointerException( "server.use.factory.instance was set in env but was null" );
                }

                // Bind is a special case where we have to use the referral property to deal
                ctx = ( LdapContext ) factory.getInitialContext( env );
            }
            else
            {
                Control[] connCtls = ( Control[] ) request.getControls().values().toArray( EMPTY );
                ctx = new InitialLdapContext( env, connCtls );
            }
        }
        catch ( NamingException e )
        {
            ResultCodeEnum code;

            if ( e instanceof LdapException )
            {
                code = ( ( LdapException ) e ).getResultCode();
                result.setResultCode( code );
            }
            else
            {
                code = ResultCodeEnum.getBestEstimate( e, request.getType() );
                result.setResultCode( code );
            }

            String msg = "Bind failed: " + e.getMessage();

            if ( log.isDebugEnabled() )
            {
                msg += ":\n" + ExceptionUtils.getStackTrace( e );
                msg += "\n\nBindRequest = \n" + request.toString();
            }

            if ( ( e.getResolvedName() != null )
                && ( ( code == ResultCodeEnum.NO_SUCH_OBJECT ) || ( code == ResultCodeEnum.ALIAS_PROBLEM )
                    || ( code == ResultCodeEnum.INVALID_DN_SYNTAX ) || ( code == ResultCodeEnum.ALIAS_DEREFERENCING_PROBLEM ) ) )
            {
                result.setMatchedDn( ( LdapDN ) e.getResolvedName() );
            }

            result.setErrorMessage( msg );
            session.write( request.getResultResponse() );
            ctx = null;
        }

        return ctx;
    }


    /**
     * Convenience method for getting an environment suitable for acquiring
     * an {@link LdapContext} for the client.
     * 
     * @param session The current session.
     * @param message The current message.
     * @return An environment suitable for acquiring an {@link LdapContext} for the client.
     */
    protected Hashtable getEnvironment( IoSession session, Object message )
    {
        Hashtable env = new Hashtable();
        env.put( Context.PROVIDER_URL, ( String ) session.getAttribute( "baseDn" ) );
        env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.directory.server.core.jndi.CoreContextFactory" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );

        return env;
    }
}
