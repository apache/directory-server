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
import javax.naming.directory.SearchControls;

import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.configuration.AuthenticatorConfiguration;
import org.apache.directory.server.core.configuration.InterceptorConfiguration;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.AddServiceContext;
import org.apache.directory.server.core.interceptor.context.BindServiceContext;
import org.apache.directory.server.core.interceptor.context.LookupServiceContext;
import org.apache.directory.server.core.interceptor.context.ModifyDNServiceContext;
import org.apache.directory.server.core.interceptor.context.ModifyServiceContext;
import org.apache.directory.server.core.interceptor.context.ReplaceServiceContext;
import org.apache.directory.server.core.interceptor.context.ServiceContext;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.jndi.LdapJndiProperties;
import org.apache.directory.server.core.jndi.ServerContext;
import org.apache.directory.shared.ldap.exception.LdapAuthenticationException;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.util.AttributeUtils;
import org.apache.directory.shared.ldap.util.StringTools;
import org.apache.directory.shared.ldap.message.MessageTypeEnum;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.name.LdapDN;

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
    @SuppressWarnings("unchecked")
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


    /**
     * Initializes the specified {@link Authenticator} and registers it to
     * this service.
     */
    private void register( AuthenticatorConfiguration cfg ) throws NamingException
    {
        cfg.getAuthenticator().init( factoryCfg, cfg );

        Collection<Authenticator> authenticatorList = getAuthenticators( cfg.getAuthenticator().getAuthenticatorType() );
        
        if ( authenticatorList == null )
        {
            authenticatorList = new ArrayList<Authenticator>();
            authenticators.put( cfg.getAuthenticator().getAuthenticatorType(), authenticatorList );
        }

        authenticatorList.add( cfg.getAuthenticator() );
    }


    /**
     * Deinitializes the specified {@link Authenticator} and deregisters it from
     * this service.
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


    public void add( NextInterceptor next, ServiceContext addContext ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            log.debug( "Adding the entry " + 
            		AttributeUtils.toString( ((AddServiceContext)addContext).getEntry() ) + 
            		" for DN = '" + addContext.getDn().getUpName() + "'" );
        }

        checkAuthenticated( MessageTypeEnum.ADD_REQUEST );
        next.add( addContext );
    }


    public void delete( NextInterceptor next, ServiceContext deleteContext ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            log.debug( "Deleting name = '" + deleteContext.getDn().getUpName() + "'" );
        }

        checkAuthenticated( MessageTypeEnum.DEL_REQUEST );
        next.delete( deleteContext );
        invalidateAuthenticatorCaches( deleteContext.getDn() );
    }


    public LdapDN getMatchedName ( NextInterceptor next, LdapDN dn ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            log.debug( "Matching name = '" + dn.toString() + "'" );
        }

        checkAuthenticated();
        return next.getMatchedName( dn );
    }


    public Attributes getRootDSE( NextInterceptor next ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            log.debug( "Getting root DSE" );
        }

        checkAuthenticated();
        return next.getRootDSE();
    }


    public LdapDN getSuffix ( NextInterceptor next, LdapDN dn ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            log.debug( "Getting suffix for name = '" + dn.toString() + "'" );
        }

        checkAuthenticated();
        return next.getSuffix( dn );
    }


    public boolean hasEntry( NextInterceptor next, ServiceContext entryContext ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            log.debug( "Testing if entry name = '" + entryContext.getDn().getUpName() + "' exists" );
        }

        checkAuthenticated();
        return next.hasEntry( entryContext );
    }


    public NamingEnumeration list( NextInterceptor next, LdapDN base ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            log.debug( "Listing base = '" + base.toString() + "'" );
        }

        checkAuthenticated();
        return next.list( base );
    }


    public Iterator listSuffixes ( NextInterceptor next ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            log.debug( "Listing suffixes" );
        }

        checkAuthenticated();
        return next.listSuffixes();
    }


    public Attributes lookup( NextInterceptor next, ServiceContext lookupContext ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            LookupServiceContext ctx = (LookupServiceContext)lookupContext;
            
            List<String> attrIds = ctx.getAttrsId();
            
            if ( ( attrIds != null ) && ( attrIds.size() != 0 ) )
            {
                String attrs = StringTools.listToString( attrIds );
                log.debug( "Lookup name = '" + ctx.getDn().getUpName() + "', attributes = " + attrs );
            }
            else
            {
                log.debug( "Lookup name = '" + ctx.getDn().getUpName() + "', no attributes " );
            }
        }

        checkAuthenticated();
        return next.lookup( lookupContext );
    }

    private void invalidateAuthenticatorCaches( LdapDN principalDn )
    {
        for ( String authMech:authenticators.keySet() )
        {
            Collection<Authenticator> authenticators = getAuthenticators( authMech );
            
            // try each authenticator
            for ( Authenticator authenticator:authenticators )
            {
                authenticator.invalidateCache( getPrincipal().getJndiName() );
            }
        }
    }
    
    
    public void modify( NextInterceptor next, ServiceContext modifyContext ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            log.debug( "Modifying name = '" + modifyContext.getDn().getUpName() + 
            		"', modifs = " + AttributeUtils.toString( 
            				((ModifyServiceContext)modifyContext).getMods() ) );
        }

        checkAuthenticated( MessageTypeEnum.MODIFY_REQUEST );
        next.modify( modifyContext );
        invalidateAuthenticatorCaches( modifyContext.getDn() );
    }

    
    public void modify( NextInterceptor next, LdapDN name, ModificationItemImpl[] mods ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            log.debug( "Modifying name = '" + name.toString() + "'" );
        }

        checkAuthenticated( MessageTypeEnum.MODIFY_REQUEST );
        next.modify( name, mods );
        invalidateAuthenticatorCaches( name );
    }


    public void modifyRn( NextInterceptor next, ServiceContext modifyDnContext ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            log.debug( "Modifying name = '" + modifyDnContext.getDn().getUpName() + "', new RDN = '" + 
                ((ModifyDNServiceContext)modifyDnContext).getNewDn() + "', " +
                "oldRDN = '" + ((ModifyDNServiceContext)modifyDnContext).getDelOldDn() + "'" );
        }

        checkAuthenticated( MessageTypeEnum.MOD_DN_REQUEST );
        next.modifyRn( modifyDnContext );
        invalidateAuthenticatorCaches( modifyDnContext.getDn() );
    }


    public void move( NextInterceptor next, LdapDN oriChildName, LdapDN newParentName, String newRn, boolean deleteOldRn )
        throws NamingException
    {
        if ( IS_DEBUG )
        {
            log.debug( "Moving name = '" + oriChildName.toString() + "' to name = '" + newParentName + "', new RDN = '"
                + newRn + "', oldRDN = '" + deleteOldRn + "'" );
        }

        checkAuthenticated( MessageTypeEnum.MOD_DN_REQUEST );
        next.move( oriChildName, newParentName, newRn, deleteOldRn );
        invalidateAuthenticatorCaches( oriChildName );
    }


    public void replace( NextInterceptor next, ServiceContext replaceContext ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            log.debug( "Moving name = '" + replaceContext.getDn().getUpName() + " to name = '" + 
                ((ReplaceServiceContext)replaceContext).getParent().getUpName() + "'" );
        }

        checkAuthenticated( MessageTypeEnum.MOD_DN_REQUEST );
        next.replace( replaceContext );
        invalidateAuthenticatorCaches( replaceContext.getDn() );
    }


    public NamingEnumeration search( NextInterceptor next, LdapDN base, Map env, ExprNode filter,
        SearchControls searchCtls ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            log.debug( "Search for base = '" + base.toString() + "'" );
        }

        checkAuthenticated( MessageTypeEnum.SEARCH_REQUEST );
        return next.search( base, env, filter, searchCtls );
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


    public void bind( NextInterceptor next, ServiceContext bindContext )
    throws NamingException
    {   
        // The DN is always normalized here
        LdapDN normBindDn = bindContext.getDn();
        String bindUpDn = bindContext.getDn().getUpName();
        
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
        
        for ( String mechanism:((BindServiceContext)bindContext).getMechanisms() )
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
            next.bind( bindContext );
            
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
