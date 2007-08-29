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
package org.apache.directory.server.core.authn;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.configuration.AuthenticatorConfiguration;
import org.apache.directory.server.core.configuration.InterceptorConfiguration;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.interceptor.context.GetMatchedNameOperationContext;
import org.apache.directory.server.core.interceptor.context.GetRootDSEOperationContext;
import org.apache.directory.server.core.interceptor.context.GetSuffixOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.ListSuffixOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.jndi.LdapJndiProperties;
import org.apache.directory.server.core.jndi.ServerContext;
import org.apache.directory.shared.ldap.exception.LdapAuthenticationException;
import org.apache.directory.shared.ldap.exception.LdapConfigurationException;
import org.apache.directory.shared.ldap.message.MessageTypeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.AttributeUtils;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An {@link Interceptor} that authenticates users.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AuthenticationService extends BaseInterceptor
{
    private static final Logger log = LoggerFactory.getLogger( AuthenticationService.class );
    
    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /** authenticators **/
    public Map<String, Collection<Authenticator>> authenticators = new HashMap<String, Collection<Authenticator>>();

    private DirectoryServiceConfiguration factoryCfg;

    /**
     * Creates an authentication service interceptor.
     */
    public AuthenticationService()
    {
    }

    /**
     * Registers and initializes all {@link Authenticator}s to this service.
     */
    public void init( DirectoryServiceConfiguration factoryCfg, InterceptorConfiguration cfg ) throws NamingException
    {
        this.factoryCfg = factoryCfg;

        // Register all authenticators
        for ( AuthenticatorConfiguration config:factoryCfg.getStartupConfiguration().getAuthenticatorConfigurations() )
        {
            try
            {
                this.register( config );
            }
            catch ( Exception e )
            {
                destroy();
                throw ( NamingException ) new NamingException( "Failed to register authenticator." ).initCause( e );
            }
        }
    }


    /**
     * Deinitializes and deregisters all {@link Authenticator}s from this service.
     */
    public void destroy()
    {
        Set<Collection<Authenticator>> clonedAuthenticatorCollections = new HashSet<Collection<Authenticator>>();
        clonedAuthenticatorCollections.addAll( authenticators.values() );
        
        for ( Collection<Authenticator> collection:clonedAuthenticatorCollections )
        {
            Set <Authenticator> clonedAuthenticators = new HashSet<Authenticator>();
            clonedAuthenticators.addAll( collection );
            
            for ( Authenticator authenticator:clonedAuthenticators )
            {
                unregister( authenticator );
            }
        }

        authenticators.clear();
    }

    
    private Authenticator instantiateAuthenticator( AuthenticatorConfiguration cfg ) throws NamingException
    {
        if ( cfg == null )
        {
            throw new IllegalStateException( "Cannot get instance of authenticator without a proper " +
                    "configuration." );
        }
        
        Class<?> authenticatorClass;
        try
        {
            authenticatorClass = Class.forName( cfg.getAuthenticatorClassName() );
        }
        catch ( ClassNotFoundException e )
        {
            String msg = "Could not load authenticator implementation class '" 
                + cfg.getAuthenticatorClassName() + "' for authenticator with name " + cfg.getName();
            log.error( msg );
            throw new LdapConfigurationException( msg, e );
        }
        
        Authenticator authenticator = null;
        try
        {
            authenticator = ( Authenticator ) authenticatorClass.newInstance();
        }
        catch ( InstantiationException e )
        {
            String msg = "No default constructor in authenticator implementation class '" 
                + cfg.getAuthenticatorClassName() + "' for authenticator with name " + cfg.getName();
            log.error( msg );
            throw new LdapConfigurationException( msg, e );
        }
        catch ( IllegalAccessException e )
        {
            String msg = "Default constructor for authenticator implementation class '" 
                + cfg.getAuthenticatorClassName() + "' for authenticator with name " 
                + cfg.getName() + " is not publicly accessible.";
            log.error( msg );
            throw new LdapConfigurationException( msg, e );
        }
        
        return authenticator;
    }
    

    /**
     * Initializes the specified {@link Authenticator} and registers it to
     * this service.
     */
    private void register( AuthenticatorConfiguration cfg ) throws NamingException
    {
        Authenticator authenticator = instantiateAuthenticator( cfg );
        authenticator.init( factoryCfg, cfg );

        Collection<Authenticator> authenticatorList = getAuthenticators( authenticator.getAuthenticatorType() );
        
        if ( authenticatorList == null )
        {
            authenticatorList = new ArrayList<Authenticator>();
            authenticators.put( authenticator.getAuthenticatorType(), authenticatorList );
        }

        authenticatorList.add( authenticator );
    }


    /**
     * Deinitializes the specified {@link Authenticator} and deregisters it from
     * this service.
     */
    private void unregister( Authenticator authenticator )
    {
        Collection<Authenticator> authenticatorList = getAuthenticators( authenticator.getAuthenticatorType() );

        if ( authenticatorList == null )
        {
            return;
        }

        authenticatorList.remove( authenticator );

        try
        {
            authenticator.destroy();
        }
        catch ( Throwable t )
        {
            log.warn( "Failed to destroy an authenticator.", t );
        }
    }


    /**
     * Returns the list of {@link Authenticator}s with the specified type.
     * 
     * @return <tt>null</tt> if no authenticator is found.
     */
    private Collection<Authenticator> getAuthenticators( String type )
    {
        Collection<Authenticator> result = authenticators.get( type );
        
        if ( ( result != null ) && ( result.size() > 0 ) )
        {
            return result;
        }
        else
        {
            return null;
        }
    }


    public void add( NextInterceptor next, AddOperationContext opContext ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            log.debug( "Adding the entry " + 
            		AttributeUtils.toString( opContext.getEntry() ) + 
            		" for DN = '" + opContext.getDn().getUpName() + "'" );
        }

        checkAuthenticated( MessageTypeEnum.ADD_REQUEST );
        next.add( opContext );
    }


    public void delete( NextInterceptor next, DeleteOperationContext opContext ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            log.debug( "Deleting name = '" + opContext.getDn().getUpName() + "'" );
        }

        checkAuthenticated( MessageTypeEnum.DEL_REQUEST );
        next.delete( opContext );
        invalidateAuthenticatorCaches( opContext.getDn() );
    }


    public LdapDN getMatchedName ( NextInterceptor next, GetMatchedNameOperationContext opContext ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            log.debug( "Matching name = '" + opContext.getDn().getUpName() + "'" );
        }

        checkAuthenticated();
        return next.getMatchedName( opContext );
    }


    public Attributes getRootDSE( NextInterceptor next, GetRootDSEOperationContext opContext ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            log.debug( "Getting root DSE" );
        }

        checkAuthenticated();
        return next.getRootDSE( opContext );
    }


    public LdapDN getSuffix ( NextInterceptor next, GetSuffixOperationContext opContext ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            log.debug( "Getting suffix for name = '" + opContext.getDn().getUpName() + "'" );
        }

        checkAuthenticated();
        return next.getSuffix( opContext );
    }


    public boolean hasEntry( NextInterceptor next, EntryOperationContext opContext ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            log.debug( "Testing if entry name = '" + opContext.getDn().getUpName() + "' exists" );
        }

        checkAuthenticated();
        return next.hasEntry( opContext );
    }


    public NamingEnumeration<SearchResult> list( NextInterceptor next, ListOperationContext opContext ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            log.debug( "Listing base = '" + opContext.getDn().getUpName() + "'" );
        }

        checkAuthenticated();
        return next.list( opContext );
    }


    public Iterator<String> listSuffixes ( NextInterceptor next, ListSuffixOperationContext opContext ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            log.debug( "Listing suffixes" );
        }

        checkAuthenticated();
        return next.listSuffixes( opContext );
    }


    public Attributes lookup( NextInterceptor next, LookupOperationContext opContext ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            List<String> attrIds = opContext.getAttrsId();
            
            if ( ( attrIds != null ) && ( attrIds.size() != 0 ) )
            {
                String attrs = StringTools.listToString( attrIds );
                log.debug( "Lookup name = '" + opContext.getDn().getUpName() + "', attributes = " + attrs );
            }
            else
            {
                log.debug( "Lookup name = '" + opContext.getDn().getUpName() + "', no attributes " );
            }
        }

        checkAuthenticated();
        return next.lookup( opContext );
    }

    private void invalidateAuthenticatorCaches( LdapDN principalDn )
    {
        for ( String authMech:authenticators.keySet() )
        {
            Collection<Authenticator> authenticators = getAuthenticators( authMech );
            
            // try each authenticator
            for ( Authenticator authenticator:authenticators )
            {
                authenticator.invalidateCache( principalDn );
            }
        }
    }
    
    
    public void modify( NextInterceptor next, ModifyOperationContext opContext ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            log.debug( opContext.toString() );
        }

        checkAuthenticated( MessageTypeEnum.MODIFY_REQUEST );
        next.modify( opContext );
        invalidateAuthenticatorCaches( opContext.getDn() );
    }

    
    public void rename( NextInterceptor next, RenameOperationContext opContext ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            log.debug( "Modifying name = '" + opContext.getDn().getUpName() + "', new RDN = '" + 
                opContext.getNewRdn() + "', " +
                "oldRDN = '" + opContext.getDelOldDn() + "'" );
        }

        checkAuthenticated( MessageTypeEnum.MOD_DN_REQUEST );
        next.rename( opContext );
        invalidateAuthenticatorCaches( opContext.getDn() );
    }


    public void moveAndRename( NextInterceptor next, MoveAndRenameOperationContext opContext )
        throws NamingException
    {
        if ( IS_DEBUG )
        {
            log.debug( "Moving name = '" + opContext.getDn().getUpName() + "' to name = '" + 
                opContext.getParent() + "', new RDN = '" + 
                opContext.getNewRdn() + "', oldRDN = '" + 
                opContext.getDelOldDn() + "'" );
        }

        checkAuthenticated( MessageTypeEnum.MOD_DN_REQUEST );
        next.moveAndRename( opContext );
        invalidateAuthenticatorCaches( opContext.getDn() );
    }


    public void move( NextInterceptor next, MoveOperationContext opContext ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            log.debug( "Moving name = '" + opContext.getDn().getUpName() + " to name = '" + 
                opContext.getParent().getUpName() + "'" );
        }

        checkAuthenticated( MessageTypeEnum.MOD_DN_REQUEST );
        next.move( opContext );
        invalidateAuthenticatorCaches( opContext.getDn() );
    }


    public NamingEnumeration<SearchResult> search( NextInterceptor next, SearchOperationContext opContext ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            log.debug( "Search for base = '" + opContext.getDn().getUpName() + "'" );
        }

        checkAuthenticated( MessageTypeEnum.SEARCH_REQUEST );
        return next.search( opContext );
    }


    private void checkAuthenticated( MessageTypeEnum operation ) throws NamingException
    {
        try
        {
            checkAuthenticated();
        }
        catch( IllegalStateException ise )
        {
            log.error( "Attempted operation {} by unauthenticated caller.", operation.name() );

            throw new IllegalStateException( "Attempted operation by unauthenticated caller." );
        }
    }

    private void checkAuthenticated() throws NamingException
    {
        ServerContext ctx = ( ServerContext ) InvocationStack.getInstance().peek().getCaller();

        if ( ctx.getPrincipal() != null )
        {
            if ( ctx.getEnvironment().containsKey( Context.SECURITY_CREDENTIALS ) )
            {
                ctx.removeFromEnvironment( Context.SECURITY_CREDENTIALS );
            }
            
            return;
        }

        throw new IllegalStateException( "Attempted operation by unauthenticated caller." );
    }


    public void bind( NextInterceptor next, BindOperationContext opContext )
    throws NamingException
    {   
        // The DN is always normalized here
        LdapDN normBindDn = opContext.getDn();
        String bindUpDn = normBindDn.getUpName();
        
        if ( IS_DEBUG )
        {
            log.debug( "Bind operation. bindDn: " + bindUpDn );
        }
        
        // check if we are already authenticated and if so we return making
        // sure first that the credentials are not exposed within context
        ServerContext ctx = ( ServerContext ) InvocationStack.getInstance().peek().getCaller();
    
        if ( IS_DEBUG )
        {
            log.debug( "bind: principal: " + ctx.getPrincipal() );
        }
        
        if ( ctx.getPrincipal() != null )
        {
            if ( ctx.getEnvironment().containsKey( Context.SECURITY_CREDENTIALS ) )
            {
                ctx.removeFromEnvironment( Context.SECURITY_CREDENTIALS );
            }
            
            return;
        }
    
        // pick the first matching authenticator type
        Collection<Authenticator> authenticators = null;
        
        for ( String mechanism:opContext.getMechanisms() )
        {
            authenticators = getAuthenticators( mechanism );
    
            if ( authenticators != null )
            {
                break;
            }
        }
    
        if ( authenticators == null )
        {
            log.debug( "No authenticators found, delegating bind to the nexus." );
            
            // as a last resort try binding via the nexus
            next.bind( opContext );
            
            log.debug( "Nexus succeeded on bind operation." );
            
            // bind succeeded if we got this far 
            ctx.setPrincipal( new TrustedPrincipalWrapper( new LdapPrincipal( normBindDn, LdapJndiProperties
                .getAuthenticationLevel( ctx.getEnvironment() ) ) ) );
            
            // remove creds so there is no security risk
            ctx.removeFromEnvironment( Context.SECURITY_CREDENTIALS );
            return;
        }
    
        // TODO : we should refactor that.
        // try each authenticators
        for ( Authenticator authenticator:authenticators )
        {
            try
            {
                // perform the authentication
                LdapPrincipal authorizationId = authenticator.authenticate( normBindDn, ctx );
                
                // authentication was successful
                ctx.setPrincipal( new TrustedPrincipalWrapper( authorizationId ) );
                
                // remove creds so there is no security risk
                ctx.removeFromEnvironment( Context.SECURITY_CREDENTIALS );
                
                return;
            }
            catch ( LdapAuthenticationException e )
            {
                // authentication failed, try the next authenticator
                if ( log.isInfoEnabled() )
                {
                    log.info( "Authenticator " + authenticator.getClass() + " failed to authenticate " + bindUpDn );
                }
            }
            catch ( Exception e )
            {
                // Log other exceptions than LdapAuthenticationException
                if ( log.isWarnEnabled() )
                {
                    log.warn( "Unexpected exception from " + authenticator.getClass() + " for principal " + bindUpDn, e );
                }
            }
        }
    
        if ( log.isInfoEnabled() )
        {
            log.info( "Cannot bind to the server " );
        }
        
        throw new LdapAuthenticationException();
    }

    /**
     * FIXME This doesn't secure anything actually.
     * 
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
        private TrustedPrincipalWrapper(LdapPrincipal principal)
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
