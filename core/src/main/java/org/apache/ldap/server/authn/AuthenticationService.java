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
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;

import org.apache.ldap.common.exception.LdapAuthenticationException;
import org.apache.ldap.common.exception.LdapAuthenticationNotSupportedException;
import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.message.ResultCodeEnum;
import org.apache.ldap.common.util.StringTools;
import org.apache.ldap.server.configuration.AuthenticatorConfiguration;
import org.apache.ldap.server.configuration.InterceptorConfiguration;
import org.apache.ldap.server.interceptor.Interceptor;
import org.apache.ldap.server.interceptor.NextInterceptor;
import org.apache.ldap.server.invocation.InvocationStack;
import org.apache.ldap.server.jndi.ContextFactoryConfiguration;
import org.apache.ldap.server.jndi.ServerContext;


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

    private ContextFactoryConfiguration factoryCfg;

    /**
     * Creates an authentication service interceptor.
     */
    public AuthenticationService()
    {
    }

    public void init( ContextFactoryConfiguration factoryCfg, InterceptorConfiguration cfg ) throws NamingException
    {
        this.factoryCfg = factoryCfg;

        // Register all authenticators
        Iterator i = factoryCfg.getConfiguration().getAuthenticatorConfigurations().iterator();
        while( i.hasNext() )
        {
            try
            {
                this.register( ( AuthenticatorConfiguration ) i.next() );
            }
            catch ( Exception e )
            {
                destroy();
                throw ( NamingException ) new NamingException(
                        "Failed to register authenticator." ).initCause( e );
            }
        }
    }
    
    public void destroy()
    {
        Iterator i = new ArrayList( authenticators.values() ).iterator();
        while( i.hasNext() )
        {
            Iterator j = new ArrayList( ( Collection ) i.next() ).iterator();
            while( j.hasNext() )
            {
                unregister( ( Authenticator ) j.next() );
            }
        }
        
        authenticators.clear();
    }

    /**
     * Registers an AuthenticationService with the AuthenticationService.  Called by each
     * AuthenticationService implementation after it has started to register for
     * authentication operation calls.
     */
    private void register( AuthenticatorConfiguration cfg ) throws NamingException
    {
        cfg.getAuthenticator().init( factoryCfg, cfg );

        Collection authenticatorList = getAuthenticators( cfg.getAuthenticator().getAuthenticatorType() );
        if ( authenticatorList == null )
        {
            authenticatorList = new ArrayList();
            authenticators.put( cfg.getAuthenticator().getAuthenticatorType(), authenticatorList );
        }

        authenticatorList.add( cfg.getAuthenticator() );
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
    private void unregister( Authenticator authenticator )
    {
        Collection authenticatorList = getAuthenticators( authenticator.getAuthenticatorType() );

        if ( authenticatorList == null )
        {
            return;
        }

        authenticatorList.remove( authenticator );
        
        try
        {
            authenticator.destroy();
        }
        catch( Throwable t )
        {
            t.printStackTrace();
        }
    }

    /**
     * Gets the authenticators with a specific type.
     *
     * @param type the authentication type
     * @return the authenticators with the specified type
     */
    private Collection getAuthenticators( String type )
    {
        return ( Collection ) authenticators.get( type );
    }
    

    public void add( NextInterceptor next, String upName, Name normName, Attributes entry ) throws NamingException
    {
        authenticate();
        next.add( upName, normName, entry );
    }


    public void delete( NextInterceptor next, Name name ) throws NamingException
    {
        authenticate();
        next.delete( name );
    }


    public Name getMatchedDn( NextInterceptor next, Name dn, boolean normalized ) throws NamingException
    {
        authenticate();
        return next.getMatchedDn( dn, normalized );
    }


    public Attributes getRootDSE( NextInterceptor next ) throws NamingException
    {
        authenticate();
        return next.getRootDSE();
    }


    public Name getSuffix( NextInterceptor next, Name dn, boolean normalized ) throws NamingException
    {
        authenticate();
        return next.getSuffix( dn, normalized );
    }


    public boolean hasEntry( NextInterceptor next, Name name ) throws NamingException
    {
        authenticate();
        return next.hasEntry( name );
    }


    public boolean isSuffix( NextInterceptor next, Name name ) throws NamingException
    {
        authenticate();
        return next.isSuffix( name );
    }


    public NamingEnumeration list( NextInterceptor next, Name base ) throws NamingException
    {
        authenticate();
        return next.list( base );
    }


    public Iterator listSuffixes( NextInterceptor next, boolean normalized ) throws NamingException
    {
        authenticate();
        return next.listSuffixes( normalized );
    }


    public Attributes lookup( NextInterceptor next, Name dn, String[] attrIds ) throws NamingException
    {
        authenticate();
        return next.lookup( dn, attrIds );
    }


    public Attributes lookup( NextInterceptor next, Name name ) throws NamingException
    {
        authenticate();
        return next.lookup( name );
    }


    public void modify( NextInterceptor next, Name name, int modOp, Attributes mods ) throws NamingException
    {
        authenticate();
        next.modify( name, modOp, mods );
    }


    public void modify( NextInterceptor next, Name name, ModificationItem[] mods ) throws NamingException
    {
        authenticate();
        next.modify( name, mods );
    }


    public void modifyRn( NextInterceptor next, Name name, String newRn, boolean deleteOldRn ) throws NamingException
    {
        authenticate();
        next.modifyRn( name, newRn, deleteOldRn );
    }


    public void move( NextInterceptor next, Name oriChildName, Name newParentName, String newRn, boolean deleteOldRn ) throws NamingException
    {
        authenticate();
        next.move( oriChildName, newParentName, newRn, deleteOldRn );
    }


    public void move( NextInterceptor next, Name oriChildName, Name newParentName ) throws NamingException
    {
        authenticate();
        next.move( oriChildName, newParentName );
    }


    public NamingEnumeration search( NextInterceptor next, Name base, Map env, ExprNode filter, SearchControls searchCtls ) throws NamingException
    {
        authenticate();
        return next.search( base, env, filter, searchCtls );
    }


    private void authenticate() throws NamingException
    {
        // check if we are already authenticated and if so we return making
        // sure first that the credentials are not exposed within context
        ServerContext ctx =
            ( ServerContext ) InvocationStack.getInstance().peek().getTarget();

        if ( ctx.getPrincipal() != null )
        {
            if ( ctx.getEnvironment().containsKey( CREDS ) )
            {
                ctx.removeFromEnvironment( CREDS );
            }
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
