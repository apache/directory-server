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
package org.apache.directory.server.core.authz;


import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NoPermissionException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.configuration.InterceptorConfiguration;
import org.apache.directory.server.core.enumeration.SearchResultFilter;
import org.apache.directory.server.core.enumeration.SearchResultFilteringEnumeration;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.jndi.ServerContext;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.constants.ServerDNConstants;
import org.apache.directory.shared.ldap.exception.LdapNoPermissionException;
import org.apache.directory.shared.ldap.message.ServerSearchResult;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.OidNormalizer;
import org.apache.directory.shared.ldap.util.AttributeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An {@link Interceptor} that controls access to {@link PartitionNexus}.
 * If a user tries to perform any operations that requires
 * permission he or she doesn't have, {@link NoPermissionException} will be
 * thrown and therefore the current invocation chain will terminate.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DefaultAuthorizationService extends BaseInterceptor
{
    /** the logger for this class */
    private static final Logger log = LoggerFactory.getLogger( DefaultAuthorizationService.class );
    
    /** The service name */
    public static final String NAME = "defaultAuthorizationService";

    /**
     * the base distinguished {@link Name} for all users
     */
    private static LdapDN USER_BASE_DN;

    /**
     * the base distinguished {@link Name} for all groups
     */
    private static LdapDN GROUP_BASE_DN;

    /**
     * the distinguished {@link Name} for the administrator group
     */
    private static LdapDN ADMIN_GROUP_DN;

    /**
     * the name parser used by this service
     */
    private boolean enabled = true;
    
    private Set administrators = new HashSet(2);
    
    /** The normalizer mapping containing a relation between an OID and a normalizer */
    private Map<String, OidNormalizer> normalizerMapping;
    
    private PartitionNexus nexus;
    
    /** attribute type registry */
    private AttributeTypeRegistry attrRegistry;

    /** A starage for the uniqueMember attributeType */
    private AttributeType uniqueMemberAT;


    /**
     * Creates a new instance.
     */
    public DefaultAuthorizationService()
    {
    }


    public void init( DirectoryServiceConfiguration factoryCfg, InterceptorConfiguration cfg ) throws NamingException
    {
        nexus = factoryCfg.getPartitionNexus();
        normalizerMapping = factoryCfg.getRegistries().getAttributeTypeRegistry().getNormalizerMapping();

        // disable this static module if basic access control mechanisms are enabled
        enabled = !factoryCfg.getStartupConfiguration().isAccessControlEnabled();
        
        USER_BASE_DN = PartitionNexus.getUsersBaseName();
        USER_BASE_DN.normalize( normalizerMapping );
        
        GROUP_BASE_DN = PartitionNexus.getGroupsBaseName();
        GROUP_BASE_DN.normalize( normalizerMapping );
     
        ADMIN_GROUP_DN = new LdapDN( ServerDNConstants.ADMINISTRATORS_GROUP_DN );
        ADMIN_GROUP_DN.normalize( normalizerMapping );
        
        attrRegistry = factoryCfg.getRegistries().getAttributeTypeRegistry();
        
        uniqueMemberAT = attrRegistry.lookup( SchemaConstants.UNIQUE_MEMBER_AT_OID );
        
        loadAdministrators();
    }
    
    
    private void loadAdministrators() throws NamingException
    {
        // read in the administrators and cache their normalized names
        Set<String> newAdministrators = new HashSet<String>( 2 );
        Attributes adminGroup = nexus.lookup( new LookupOperationContext( ADMIN_GROUP_DN ) );
        
        if ( adminGroup == null )
        {
            return;
        }
        
        Attribute uniqueMember = AttributeUtils.getAttribute( adminGroup, uniqueMemberAT );
        
        for ( int ii = 0; ii < uniqueMember.size(); ii++ )
        {
            LdapDN memberDn = new LdapDN( ( String ) uniqueMember.get( ii ) );
            memberDn.normalize( normalizerMapping );
            newAdministrators.add( memberDn.getNormName() );
        }
        
        administrators = newAdministrators;
    }

    
    // Note:
    //    Lookup, search and list operations need to be handled using a filter
    // and so we need access to the filter service.

    public void delete( NextInterceptor nextInterceptor, OperationContext opContext ) throws NamingException
    {
    	LdapDN name = opContext.getDn();
    	
        if ( !enabled )
        {
            nextInterceptor.delete( opContext );
            return;
        }

        LdapDN principalDn = getPrincipal().getJndiName();

        if ( name.isEmpty() )
        {
            String msg = "The rootDSE cannot be deleted!";
            log.error( msg );
            throw new LdapNoPermissionException( msg );
        }

        if ( name.getNormName().equals( ADMIN_GROUP_DN.getNormName() ) )
        {
            String msg = "The Administrators group cannot be deleted!";
            log.error( msg );
            throw new LdapNoPermissionException( msg );
        }

        if ( isTheAdministrator( name ) )
        {
            String msg = "User " + principalDn.getUpName();
            msg += " does not have permission to delete the admin account.";
            msg += " No one not even the admin can delete this account!";
            log.error( msg );
            throw new LdapNoPermissionException( msg );
        }

        if ( name.size() > 2 )
        {
            if ( !isAnAdministrator( principalDn ) )
            {
                if ( name.startsWith( USER_BASE_DN ) )
                {
                    String msg = "User " + principalDn.getUpName();
                    msg += " does not have permission to delete the user account: ";
                    msg += name.getUpName() + ". Only the admin can delete user accounts.";
                    log.error( msg );
                    throw new LdapNoPermissionException( msg );
                }
        
                if ( name.startsWith( GROUP_BASE_DN ) )
                {
                    String msg = "User " + principalDn.getUpName();
                    msg += " does not have permission to delete the group entry: ";
                    msg += name.getUpName() + ". Only the admin can delete groups.";
                    log.error( msg );
                    throw new LdapNoPermissionException( msg );
                }
            }
        }

        nextInterceptor.delete( opContext );
    }

    
    private final boolean isTheAdministrator( LdapDN normalizedDn )
    {
        return normalizedDn.getNormName().equals( PartitionNexus.ADMIN_PRINCIPAL_NORMALIZED );
    }
    
    
    private final boolean isAnAdministrator( LdapDN normalizedDn )
    {
        if ( isTheAdministrator( normalizedDn ) )
        {
            return true;
        }
        
        return administrators.contains( normalizedDn.getNormName() );
    }
    

    // ------------------------------------------------------------------------
    // Entry Modification Operations
    // ------------------------------------------------------------------------

    /**
     * This policy needs to be really tight too because some attributes may take
     * part in giving the user permissions to protected resources.  We do not want
     * users to self access these resources.  As far as we're concerned no one but
     * the admin needs access.
     */
    public void modify( NextInterceptor nextInterceptor, OperationContext opContext )
        throws NamingException
    {
        if ( enabled )
        {
            LdapDN dn = opContext.getDn();
            
            protectModifyAlterations( dn );
            nextInterceptor.modify( opContext );

            // update administrators if we change administrators group
            if ( dn.getNormName().equals( ADMIN_GROUP_DN.getNormName() ) )
            {
                loadAdministrators();
            }
        }
        else
        {
            nextInterceptor.modify( opContext );
        }
    }


    private void protectModifyAlterations( LdapDN dn ) throws NamingException
    {
        LdapDN principalDn = getPrincipal().getJndiName();

        if ( dn.isEmpty() )
        {
            String msg = "The rootDSE cannot be modified!";
            log.error( msg );
            throw new LdapNoPermissionException( msg );
        }

        if ( ! isAnAdministrator( principalDn ) )
        {
            // allow self modifications 
            if ( dn.getNormName().equals( getPrincipal().getJndiName().getNormName() ) )
            {
                return;
            }
            
            if ( dn.getNormName().equals( PartitionNexus.ADMIN_PRINCIPAL_NORMALIZED ) )
            {
                String msg = "User " + principalDn.getUpName();
                msg += " does not have permission to modify the account of the";
                msg += " admin user.";
                log.error( msg );
                throw new LdapNoPermissionException( msg );
            }

            if ( dn.size() > 2 ) 
                {
                if ( dn.startsWith( USER_BASE_DN ) )
                {
                    String msg = "User " + principalDn.getUpName();
                    msg += " does not have permission to modify the account of the";
                    msg += " user " + dn.getUpName() + ".\nEven the owner of an account cannot";
                    msg += " modify it.\nUser accounts can only be modified by the";
                    msg += " administrator.";
                    log.error( msg );
                    throw new LdapNoPermissionException( msg );
                }
    
                if ( dn.startsWith( GROUP_BASE_DN ) )
                {
                    String msg = "User " + principalDn.getUpName();
                    msg += " does not have permission to modify the group entry ";
                    msg += dn.getUpName() + ".\nGroups can only be modified by the admin.";
                    log.error( msg );
                    throw new LdapNoPermissionException( msg );
                }
            }
        }
    }
    
    
    // ------------------------------------------------------------------------
    // DN altering operations are a no no for any user entry.  Basically here
    // are the rules of conduct to follow:
    //
    //  o No user should have the ability to move or rename their entry
    //  o Only the administrator can move or rename non-admin user entries
    //  o The administrator entry cannot be moved or renamed by anyone
    // ------------------------------------------------------------------------

    public void rename( NextInterceptor nextInterceptor, OperationContext opContext )
        throws NamingException
    {
        if ( enabled )
        {
            protectDnAlterations( opContext.getDn() );
        }
        
        nextInterceptor.rename( opContext );
    }


    public void move( NextInterceptor nextInterceptor, OperationContext opContext ) throws NamingException
    {
        if ( enabled )
        {
            protectDnAlterations( opContext.getDn() );
        }
        
        nextInterceptor.move( opContext );
    }


    public void moveAndRename( NextInterceptor nextInterceptor, OperationContext opContext ) throws NamingException
    {
        if ( enabled )
        {
            protectDnAlterations( opContext.getDn() );
        }
        
        nextInterceptor.moveAndRename( opContext );
    }


    private void protectDnAlterations( LdapDN dn ) throws NamingException
    {
        LdapDN principalDn = getPrincipal().getJndiName();

        if ( dn.isEmpty() )
        {
            String msg = "The rootDSE cannot be moved or renamed!";
            log.error( msg );
            throw new LdapNoPermissionException( msg );
        }

        if ( dn.getNormName().equals( ADMIN_GROUP_DN.getNormName() ) )
        {
            String msg = "The Administrators group cannot be moved or renamed!";
            log.error( msg );
            throw new LdapNoPermissionException( msg );
        }
        
        if ( isTheAdministrator( dn ) )
        {
            String msg = "User '" + principalDn.getUpName();
            msg += "' does not have permission to move or rename the admin";
            msg += " account.  No one not even the admin can move or";
            msg += " rename " + dn.getUpName() + "!";
            log.error( msg );
            throw new LdapNoPermissionException( msg );
        }

        if ( dn.size() > 2 && dn.startsWith( USER_BASE_DN ) && !isAnAdministrator( principalDn ) )
        {
            String msg = "User '" + principalDn.getUpName();
            msg += "' does not have permission to move or rename the user";
            msg += " account: " + dn.getUpName() + ". Only the admin can move or";
            msg += " rename user accounts.";
            log.error( msg );
            throw new LdapNoPermissionException( msg );
        }

        if ( dn.size() > 2 && dn.startsWith( GROUP_BASE_DN ) && !isAnAdministrator( principalDn ) )
        {
            String msg = "User " + principalDn.getUpName();
            msg += " does not have permission to move or rename the group entry ";
            msg += dn.getUpName() + ".\nGroups can only be moved or renamed by the admin.";
            throw new LdapNoPermissionException( msg );
        }
    }


    public Attributes lookup( NextInterceptor nextInterceptor, OperationContext opContext ) throws NamingException
    {
        Attributes attributes = nextInterceptor.lookup( opContext );
        
        if ( !enabled || ( attributes == null ) )
        {
            return attributes;
        }

        protectLookUp( ((LookupOperationContext)opContext).getDn() );
        return attributes;
    }


    private void protectLookUp( LdapDN normalizedDn ) throws NamingException
    {
        LdapContext ctx = ( LdapContext ) InvocationStack.getInstance().peek().getCaller();
        LdapDN principalDn = ( ( ServerContext ) ctx ).getPrincipal().getJndiName();
        
        if ( !isAnAdministrator( principalDn ) )
        {
            if ( normalizedDn.size() > 2 )
            {
                if( normalizedDn.startsWith( USER_BASE_DN ) )
                {
                    // allow for self reads
                    if ( normalizedDn.getNormName().equals( principalDn.getNormName() ) )
                    {
                        return;
                    }
    
                    String msg = "Access to user account '" + normalizedDn.getUpName() + "' not permitted";
                    msg += " for user '" + principalDn.getUpName() + "'.  Only the admin can";
                    msg += " access user account information";
                    log.error( msg );
                    throw new LdapNoPermissionException( msg );
                }

                if ( normalizedDn.startsWith( GROUP_BASE_DN ) )
                {
                    // allow for self reads
                    if ( normalizedDn.getNormName().equals( principalDn.getNormName() ) )
                    {
                        return;
                    }
    
                    String msg = "Access to group '" + normalizedDn.getUpName() + "' not permitted";
                    msg += " for user '" + principalDn.getUpName() + "'.  Only the admin can";
                    msg += " access group information";
                    log.error( msg );
                    throw new LdapNoPermissionException( msg );
                }
            }

            if ( isTheAdministrator( normalizedDn ) )
            {
                // allow for self reads
                if ( normalizedDn.getNormName().equals( principalDn.getNormName() ) )
                {
                    return;
                }

                String msg = "Access to admin account not permitted for user '";
                msg += principalDn.getUpName() + "'.  Only the admin can";
                msg += " access admin account information";
                log.error( msg );
                throw new LdapNoPermissionException( msg );
            }
        }
    }


    public NamingEnumeration<SearchResult> search( NextInterceptor nextInterceptor, OperationContext opContext ) throws NamingException
    {
        NamingEnumeration<SearchResult> e = nextInterceptor.search( opContext );

        if ( !enabled )
        {
            return e;
        }

        Invocation invocation = InvocationStack.getInstance().peek();
        return new SearchResultFilteringEnumeration( e, ((SearchOperationContext)opContext).getSearchControls(), invocation, 
            new SearchResultFilter()
        {
            public boolean accept( Invocation invocation, SearchResult result, SearchControls controls )
                throws NamingException
            {
                return DefaultAuthorizationService.this.isSearchable( invocation, result );
            }
        } );
    }


    public NamingEnumeration list( NextInterceptor nextInterceptor, OperationContext opContext ) throws NamingException
    {
        NamingEnumeration e = nextInterceptor.list( opContext );
        
        if ( !enabled )
        {
            return e;
        }

        Invocation invocation = InvocationStack.getInstance().peek();
        
        return new SearchResultFilteringEnumeration( e, null, invocation, new SearchResultFilter()
        {
            public boolean accept( Invocation invocation, SearchResult result, SearchControls controls )
                throws NamingException
            {
                return DefaultAuthorizationService.this.isSearchable( invocation, result );
            }
        } );
    }


    private boolean isSearchable( Invocation invocation, SearchResult result ) throws NamingException
    {
        LdapDN principalDn = ( ( ServerContext ) invocation.getCaller() ).getPrincipal().getJndiName();
        LdapDN dn = ((ServerSearchResult)result).getDn();
        
        if ( !dn.isNormalized() )
        {
        	dn.normalize( normalizerMapping );
        }

        // Admin users gets full access to all entries
        if ( isAnAdministrator( principalDn ) )
        {
            return true;
        }
        
        // Users reading their own entries should be allowed to see all
        boolean isSelfRead = dn.getNormName().equals( principalDn.getNormName() );
        
        if ( isSelfRead )
        {
            return true;
        }
        
        // Block off reads to anything under ou=users and ou=groups if not a self read
        if ( dn.size() > 2 )
        {
            // stuff this if in here instead of up in outer if to prevent 
            // constant needless reexecution for all entries in other depths
            
            if ( dn.getNormName().endsWith( USER_BASE_DN.getNormName() ) 
                || dn.getNormName().endsWith( GROUP_BASE_DN.getNormName() ) )
            {
                return false;
            }
        }
        
        // Non-admin users cannot read the admin entry
        if ( isTheAdministrator( dn ) )
        {
            return false;
        }
        
        return true;
    }
}
