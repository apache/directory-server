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


import java.util.Map;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NoPermissionException;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
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
import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.jndi.ServerContext;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.shared.ldap.exception.LdapNoPermissionException;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.name.LdapDN;


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
    /**
     * the administrator's distinguished {@link Name}
     */
    private static LdapDN ADMIN_DN;

    /**
     * the base distinguished {@link Name} for all users
     */
    private static LdapDN USER_BASE_DN;
    private static LdapDN USER_BASE_DN_NORMALIZED;

    /**
     * the base distinguished {@link Name} for all groups
     */
    private static LdapDN GROUP_BASE_DN;
    private static LdapDN GROUP_BASE_DN_NORMALIZED;

    /**
     * the name parser used by this service
     */
    private boolean enabled = true;
    
    private Map oidsMap;
    


    /**
     * Creates a new instance.
     */
    public DefaultAuthorizationService()
    {
    }


    public void init( DirectoryServiceConfiguration factoryCfg, InterceptorConfiguration cfg ) throws NamingException
    {
        oidsMap = factoryCfg.getGlobalRegistries().getAttributeTypeRegistry().getNormalizerMapping();
        //dnParser = new DnParser( new ConcreteNameComponentNormalizer( atr ) );

        // disable this static module if basic access control mechanisms are enabled
        enabled = !factoryCfg.getStartupConfiguration().isAccessControlEnabled();
        ADMIN_DN = PartitionNexus.getAdminName(); 
        
        USER_BASE_DN = PartitionNexus.getUsersBaseName();
        USER_BASE_DN_NORMALIZED = LdapDN.normalize( USER_BASE_DN, oidsMap );
        
        GROUP_BASE_DN = PartitionNexus.getGroupsBaseName();
        GROUP_BASE_DN_NORMALIZED = LdapDN.normalize( GROUP_BASE_DN, oidsMap );
    }


    // Note:
    //    Lookup, search and list operations need to be handled using a filter
    // and so we need access to the filter service.

    public void delete( NextInterceptor nextInterceptor, LdapDN name ) throws NamingException
    {
        if ( !enabled )
        {
            nextInterceptor.delete( name );
            return;
        }

        Name principalDn = getPrincipal().getJndiName();

        if ( name.toString().equals( "" ) )
        {
            String msg = "The rootDSE cannot be deleted!";
            throw new LdapNoPermissionException( msg );
        }

        if ( name == ADMIN_DN || name.equals( ADMIN_DN ) )
        {
            String msg = "User " + principalDn;
            msg += " does not have permission to delete the admin account.";
            msg += " No one not even the admin can delete this account!";
            throw new LdapNoPermissionException( msg );
        }

        if ( name.size() > 2 && name.startsWith( USER_BASE_DN ) && !principalDn.equals( ADMIN_DN ) )
        {
            String msg = "User " + principalDn;
            msg += " does not have permission to delete the user account: ";
            msg += name + ". Only the admin can delete user accounts.";
            throw new LdapNoPermissionException( msg );
        }

        if ( name.size() > 2 && name.startsWith( GROUP_BASE_DN ) && !principalDn.equals( ADMIN_DN ) )
        {
            String msg = "User " + principalDn;
            msg += " does not have permission to delete the group entry: ";
            msg += name + ". Only the admin can delete groups.";
            throw new LdapNoPermissionException( msg );
        }

        nextInterceptor.delete( name );
    }


    /**
     * Note that we do nothing here. First because this is not an externally
     * exposed function via the JNDI interfaces.  It is used internally by
     * the provider for optimization purposes so there is no reason for us to
     * start to constrain it.
     */
    public boolean hasEntry( NextInterceptor nextInterceptor, LdapDN name ) throws NamingException
    {
        return super.hasEntry( nextInterceptor, name );
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
    public void modify( NextInterceptor nextInterceptor, LdapDN name, int modOp, Attributes attrs )
        throws NamingException
    {
        if ( enabled )
        {
            protectModifyAlterations( name );
        }

        nextInterceptor.modify( name, modOp, attrs );
    }


    /**
     * This policy needs to be really tight too because some attributes may take part
     * in giving the user permissions to protected resources.  We do not want users to
     * self access these resources.  As far as we're concerned no one but the admin
     * needs access.
     */
    public void modify( NextInterceptor nextInterceptor, LdapDN name, ModificationItem[] items ) throws NamingException
    {
        if ( enabled )
        {
            protectModifyAlterations( name );
        }
        nextInterceptor.modify( name, items );
    }


    private void protectModifyAlterations( LdapDN dn ) throws NamingException
    {
        LdapDN principalDn = getPrincipal().getJndiName();

        if ( dn.size() == 0 )
        {
            String msg = "The rootDSE cannot be modified!";
            throw new LdapNoPermissionException( msg );
        }

        if ( !principalDn.toNormName().equals( PartitionNexus.ADMIN_PRINCIPAL_NORMALIZED ) )
        {
            if ( dn.toNormName().equals( PartitionNexus.ADMIN_PRINCIPAL_NORMALIZED ) )
            {
                String msg = "User " + principalDn;
                msg += " does not have permission to modify the account of the";
                msg += " admin user.";
                throw new LdapNoPermissionException( msg );
            }

            if ( dn.size() > 2 && dn.startsWith( USER_BASE_DN ) )
            {
                String msg = "User " + principalDn;
                msg += " does not have permission to modify the account of the";
                msg += " user " + dn + ".\nEven the owner of an account cannot";
                msg += " modify it.\nUser accounts can only be modified by the";
                msg += " administrator.";
                throw new LdapNoPermissionException( msg );
            }

            if ( dn.size() > 2 && dn.startsWith( GROUP_BASE_DN ) )
            {
                String msg = "User " + principalDn;
                msg += " does not have permission to modify the group entry ";
                msg += dn.getUpName() + ".\nGroups can only be modified by the admin.";
                throw new LdapNoPermissionException( msg );
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

    public void modifyRn( NextInterceptor nextInterceptor, LdapDN name, String newRn, boolean deleteOldRn )
        throws NamingException
    {
        if ( enabled )
        {
            protectDnAlterations( name );
        }
        nextInterceptor.modifyRn( name, newRn, deleteOldRn );
    }


    public void move( NextInterceptor nextInterceptor, LdapDN oriChildName, LdapDN newParentName ) throws NamingException
    {
        if ( enabled )
        {
            protectDnAlterations( oriChildName );
        }
        nextInterceptor.move( oriChildName, newParentName );
    }


    public void move( NextInterceptor nextInterceptor, LdapDN oriChildName, LdapDN newParentName, String newRn,
                      boolean deleteOldRn ) throws NamingException
    {
        if ( enabled )
        {
            protectDnAlterations( oriChildName );
        }
        nextInterceptor.move( oriChildName, newParentName, newRn, deleteOldRn );
    }


    private void protectDnAlterations( Name dn ) throws LdapNoPermissionException
    {
        LdapDN principalDn = getPrincipal().getJndiName();

        if ( dn.toString().equals( "" ) )
        {
            String msg = "The rootDSE cannot be moved or renamed!";
            throw new LdapNoPermissionException( msg );
        }

        if ( dn == ADMIN_DN || dn.equals( ADMIN_DN ) )
        {
            String msg = "User '" + principalDn.getUpName();
            msg += "' does not have permission to move or rename the admin";
            msg += " account.  No one not even the admin can move or";
            msg += " rename " + dn + "!";
            throw new LdapNoPermissionException( msg );
        }

        if ( dn.size() > 2 && dn.startsWith( USER_BASE_DN ) && !principalDn.equals( ADMIN_DN ) )
        {
            String msg = "User '" + principalDn;
            msg += "' does not have permission to move or rename the user";
            msg += " account: " + dn + ". Only the admin can move or";
            msg += " rename user accounts.";
            throw new LdapNoPermissionException( msg );
        }

        if ( dn.size() > 2 && dn.startsWith( GROUP_BASE_DN ) && !principalDn.equals( ADMIN_DN ) )
        {
            String msg = "User " + principalDn;
            msg += " does not have permission to move or rename the group entry ";
            msg += dn + ".\nGroups can only be moved or renamed by the admin.";
            throw new LdapNoPermissionException( msg );
        }
    }


    public Attributes lookup( NextInterceptor nextInterceptor, LdapDN name ) throws NamingException
    {
        Attributes attributes = nextInterceptor.lookup( name );
        if ( !enabled || attributes == null )
        {
            return attributes;
        }

        protectLookUp( name );
        return attributes;
    }


    public Attributes lookup( NextInterceptor nextInterceptor, LdapDN name, String[] attrIds ) throws NamingException
    {
        Attributes attributes = nextInterceptor.lookup( name, attrIds );
        if ( !enabled || attributes == null )
        {
            return attributes;
        }

        protectLookUp( name );
        return attributes;
    }


    private void protectLookUp( LdapDN normalizedDn ) throws NamingException
    {
        LdapContext ctx = ( LdapContext ) InvocationStack.getInstance().peek().getCaller();
        LdapDN principalDn = ( ( ServerContext ) ctx ).getPrincipal().getJndiName();
        if ( !principalDn.equals( ADMIN_DN ) )
        {
            if ( normalizedDn.size() > 2 && normalizedDn.startsWith( USER_BASE_DN ) )
            {
                // allow for self reads
                if ( normalizedDn.getNormName().equals( principalDn.getNormName() ) )
                {
                    return;
                }

                String msg = "Access to user account '" + normalizedDn + "' not permitted";
                msg += " for user '" + principalDn + "'.  Only the admin can";
                msg += " access user account information";
                throw new LdapNoPermissionException( msg );
            }

            if ( normalizedDn.size() > 2 && normalizedDn.startsWith( GROUP_BASE_DN ) )
            {
                // allow for self reads
                if ( normalizedDn.getNormName().equals( principalDn.getNormName() ) )
                {
                    return;
                }

                String msg = "Access to group '" + normalizedDn + "' not permitted";
                msg += " for user '" + principalDn + "'.  Only the admin can";
                msg += " access group information";
                throw new LdapNoPermissionException( msg );
            }

            if ( normalizedDn.equals( ADMIN_DN ) )
            {
                // allow for self reads
                if ( normalizedDn.getNormName().equals( principalDn.getNormName() ) )
                {
                    return;
                }

                String msg = "Access to admin account not permitted for user '";
                msg += principalDn + "'.  Only the admin can";
                msg += " access admin account information";
                throw new LdapNoPermissionException( msg );
            }
        }
    }


    public NamingEnumeration search( NextInterceptor nextInterceptor, LdapDN base, Map env, ExprNode filter,
                                     SearchControls searchCtls ) throws NamingException
    {
        NamingEnumeration e = nextInterceptor.search( base, env, filter, searchCtls );
        if ( !enabled )
        {
            return e;
        }
        //if ( searchCtls.getReturningAttributes() != null )
        //{
        //    return null;
        //}

        Invocation invocation = InvocationStack.getInstance().peek();
        return new SearchResultFilteringEnumeration( e, searchCtls, invocation, new SearchResultFilter()
        {
            public boolean accept( Invocation invocation, SearchResult result, SearchControls controls )
                throws NamingException
            {
                return DefaultAuthorizationService.this.isSearchable( invocation, result );
            }
        } );
    }


    public NamingEnumeration list( NextInterceptor nextInterceptor, LdapDN base ) throws NamingException
    {
        NamingEnumeration e = nextInterceptor.list( base );
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
        LdapDN dn;
        dn = new LdapDN( result.getName() );
        dn.normalize( oidsMap );

        boolean isAdmin = principalDn.toNormName().equals( PartitionNexus.ADMIN_PRINCIPAL_NORMALIZED );
        
        // Admin user gets full access to all entries
        if ( isAdmin )
        {
            return true;
        }
        
        // Users reading their own entries should be allowed to see all
        boolean isSelfRead = dn.toNormName().equals( principalDn.toNormName() );
        if ( isSelfRead )
        {
            return true;
        }
        
        // Block off reads to anything under ou=users and ou=groups if not a self read
        if ( dn.size() > 2 )
        {
            // stuff this if in here instead of up in outer if to prevent 
            // constant needless reexecution for all entries in other depths
            
            if ( dn.toNormName().endsWith( USER_BASE_DN_NORMALIZED.toNormName() ) 
                || dn.toNormName().endsWith( GROUP_BASE_DN_NORMALIZED.toNormName() ) )
            {
                return false;
            }
        }
        
        // Non-admin users cannot read the admin entry
        if ( dn.toNormName().equals( PartitionNexus.ADMIN_PRINCIPAL_NORMALIZED ) )
        {
            return false;
        }
        
        return true;
    }
}
