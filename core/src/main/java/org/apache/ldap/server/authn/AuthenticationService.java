/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.ldap.server.authn;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingException;

import org.apache.ldap.common.exception.LdapAuthenticationException;
import org.apache.ldap.common.exception.LdapAuthenticationNotSupportedException;
import org.apache.ldap.common.message.ResultCodeEnum;
import org.apache.ldap.common.util.StringTools;
import org.apache.ldap.server.configuration.AuthenticatorConfiguration;
import org.apache.ldap.server.interceptor.Interceptor;
import org.apache.ldap.server.interceptor.InterceptorContext;
import org.apache.ldap.server.interceptor.NextInterceptor;
import org.apache.ldap.server.invocation.Invocation;
import org.apache.ldap.server.jndi.ServerContext;
import org.apache.ldap.server.jndi.ServerLdapContext;


/**
 * An {@link Interceptor} that authenticates users.
 *
 * @author Apache Directory Project (dev@directory.apache.org)
 * @author Alex Karasulu (akarasulu@apache.org)
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class AuthenticationService implements Interceptor
{
    /** short for Context.SECURITY_AUTHENTICATION */
    private static final String AUTH_TYPE = Context.SECURITY_AUTHENTICATION;

    /** short for Context.SECURITY_CREDENTIALS */
    private static final String CREDS = Context.SECURITY_CREDENTIALS;

    /** authenticators **/
    public Map authenticators = new HashMap();


    /**
     * Creates an authentication service interceptor.
     */
    public AuthenticationService()
    {
    }

    public void init( InterceptorContext ctx ) throws NamingException
    {
        // Register all authenticators
        Iterator i = ctx.getConfiguration().getAuthenticatorConfigurations().iterator();
        while( i.hasNext() )
        {
            try {
                AuthenticatorConfiguration cfg = ( AuthenticatorConfiguration ) i.next();
                
                // Create context
                AuthenticatorContext authenticatorContext =
                    new GenericAuthenticatorContext( ctx.getConfiguration(), cfg, ctx.getRootNexus() );
    
                cfg.getAuthenticator().init( authenticatorContext );
                
                this.register( cfg.getAuthenticator() );
            }
            catch ( Exception e )
            {
                throw ( NamingException ) new NamingException(
                        "Failed to register authenticator." ).initCause( e );
            }
        }
    }
    
    public void destroy()
    {
        authenticators.clear();
    }

    /**
     * Registers an AuthenticationService with the AuthenticationService.  Called by each
     * AuthenticationService implementation after it has started to register for
     * authentication operation calls.
     *
     * @param authenticator AuthenticationService component to register with this
     * AuthenticatorService.
     */
    public void register( Authenticator authenticator )
    {
        Collection authenticatorList = getAuthenticators( authenticator.getAuthenticatorType() );

        if ( authenticatorList == null )
        {
            authenticatorList = new ArrayList();

            authenticators.put( authenticator.getAuthenticatorType(), authenticatorList );
        }

        authenticatorList.add( authenticator );
    }

    /**
     * Unregisters an AuthenticationService with the AuthenticationService.  Called for each
     * registered AuthenticationService right before it is to be stopped.  This prevents
     * protocol server calls from reaching the Backend and effectively puts
     * the ContextPartition's naming context offline.
     *
     * @param authenticator AuthenticationService component to unregister with this
     * AuthenticationService.
     */
    public void unregister( org.apache.ldap.server.authn.Authenticator authenticator )
    {
        Collection authenticatorList = getAuthenticators( authenticator.getAuthenticatorType() );

        if ( authenticatorList == null )
        {
            return;
        }

        authenticatorList.remove( authenticator );
    }

    /**
     * Gets the authenticators with a specific type.
     *
     * @param type the authentication type
     * @return the authenticators with the specified type
     */
    public Collection getAuthenticators( String type )
    {
        return ( Collection ) authenticators.get( type );
    }
    
    public void process( NextInterceptor nextProcessor, Invocation call ) throws NamingException
    {
        // check if we are already authenticated and if so we return making
        // sure first that the credentials are not exposed within context
        ServerContext ctx = ( ServerLdapContext ) call.getContextStack().peek();

        if ( ctx.getPrincipal() != null )
        {
            if ( ctx.getEnvironment().containsKey( CREDS ) )
            {
                ctx.removeFromEnvironment( CREDS );
            }

            nextProcessor.process(call);

            return;
        }

        String authList = ( String ) ctx.getEnvironment().get( AUTH_TYPE );

        if ( authList == null )
        {
            if ( ctx.getEnvironment().containsKey( CREDS ) )
            {
                // authentication type is simple here

                authList = "simple";
            }
            else
            {
                // authentication type is anonymous

                authList = "none";
            }

        }

        authList = StringTools.deepTrim( authList );

        String[] auth = authList.split( " " );

        Collection authenticators = null;

        // pick the first matching authenticator type

        for ( int i=0; i<auth.length; i++)
        {
            authenticators = getAuthenticators( auth[i] );

            if ( authenticators != null )
            {
                break;
            }
        }

        if ( authenticators == null )
        {
            ctx.getEnvironment(); // shut's up idea's yellow light

            ResultCodeEnum rc = ResultCodeEnum.AUTHMETHODNOTSUPPORTED;

            throw new LdapAuthenticationNotSupportedException( rc );
        }

        // try each authenticators
        for ( Iterator i = authenticators.iterator(); i.hasNext(); )
        {
            try
            {
                Authenticator authenticator = ( Authenticator ) i.next();

                // perform the authentication

                LdapPrincipal authorizationId = authenticator.authenticate( ctx );

                // authentication was successful

                ctx.setPrincipal( new TrustedPrincipalWrapper( authorizationId ) );

                // remove creds so there is no security risk

                ctx.removeFromEnvironment( CREDS );

                nextProcessor.process(call);

                return;
            }
            catch ( LdapAuthenticationException e )
            {
                // authentication failed, try the next authenticator
            }
        }

        throw new LdapAuthenticationException();
    }


    /**
     * Created this wrapper to pass to ctx.setPrincipal() which is public for added
     * security.  This adds more security because an instance of this class is not
     * easily accessible whereas LdapPrincipals can be accessed easily from a context
     * althought they cannot be instantiated outside of the authn package.  Malicious
     * code may not be able to set the principal to what they would like but they
     * could switch existing principals using the now public ServerContext.setPrincipal()
     * method.  To avoid this we make sure that this metho takes a TrustedPrincipalWrapper
     * as opposed to the LdapPrincipal.  Only this service can create and call setPrincipal
     * with a TrustedPrincipalWrapper.
     */
    public final class TrustedPrincipalWrapper
    {
        /** the wrapped ldap principal */
        private final LdapPrincipal principal;


        /**
         * Creates a TrustedPrincipalWrapper around an LdapPrincipal.
         *
         * @param principal the LdapPrincipal to wrap
         */
        private TrustedPrincipalWrapper( LdapPrincipal principal )
        {
            this.principal = principal;
        }


        /**
         * Gets the LdapPrincipal this TrustedPrincipalWrapper wraps.
         *
         * @return the wrapped LdapPrincipal
         */
        public LdapPrincipal getPrincipal()
        {
            return principal;
        }
    }
}
