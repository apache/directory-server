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
package org.apache.ldap.server.authz;


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

import org.apache.ldap.common.exception.LdapNoPermissionException;
import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.name.DnParser;
import org.apache.ldap.server.DirectoryServiceConfiguration;
import org.apache.ldap.server.configuration.InterceptorConfiguration;
import org.apache.ldap.server.enumeration.SearchResultFilteringEnumeration;
import org.apache.ldap.server.enumeration.SearchResultFilter;
import org.apache.ldap.server.interceptor.BaseInterceptor;
import org.apache.ldap.server.interceptor.Interceptor;
import org.apache.ldap.server.interceptor.NextInterceptor;
import org.apache.ldap.server.invocation.InvocationStack;
import org.apache.ldap.server.invocation.Invocation;
import org.apache.ldap.server.jndi.ServerContext;
import org.apache.ldap.server.partition.DirectoryPartitionNexus;
import org.apache.ldap.server.schema.AttributeTypeRegistry;
import org.apache.ldap.server.schema.ConcreteNameComponentNormalizer;


/**
 * An {@link Interceptor} that controls access to {@link DirectoryPartitionNexus}.
 * If a user tries to perform any operations that requires
 * permission he or she doesn't have, {@link NoPermissionException} will be
 * thrown and therefore the current invocation chain will terminate.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 201550 $, $Date: 2005-06-23 23:08:31 -0400 (Thu, 23 Jun 2005) $
 */
public class OldAuthorizationService extends BaseInterceptor
{
    /**
     * the administrator's distinguished {@link Name}
     */
    private static final Name ADMIN_DN = DirectoryPartitionNexus.getAdminName();

    /**
     * the base distinguished {@link Name} for all users
     */
    private static final Name USER_BASE_DN = DirectoryPartitionNexus.getUsersBaseName();

    /**
     * the base distinguished {@link Name} for all groups
     */
    private static final Name GROUP_BASE_DN = DirectoryPartitionNexus.getGroupsBaseName();

    /**
     * the name parser used by this service
     */
    private DnParser dnParser;
    private boolean enabled = true;



    /**
     * Creates a new instance.
     */
    public OldAuthorizationService()
    {
    }


    public void init( DirectoryServiceConfiguration factoryCfg, InterceptorConfiguration cfg ) throws NamingException
    {
        AttributeTypeRegistry atr = factoryCfg.getGlobalRegistries().getAttributeTypeRegistry();
        dnParser = new DnParser( new ConcreteNameComponentNormalizer( atr ) );

        // disable this static module if basic access control mechanisms are enabled
        enabled = ! factoryCfg.getStartupConfiguration().isAccessControlEnabled();
    }


    // Note:
    //    Lookup, search and list operations need to be handled using a filter
    // and so we need access to the filter service.

    public void delete( NextInterceptor nextInterceptor, Name name ) throws NamingException
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

        if ( name.size() > 2 && name.startsWith( USER_BASE_DN )
                && !principalDn.equals( ADMIN_DN ) )
        {
            String msg = "User " + principalDn;
            msg += " does not have permission to delete the user account: ";
            msg += name + ". Only the admin can delete user accounts.";
            throw new LdapNoPermissionException( msg );
        }

        if ( name.size() > 2 && name.startsWith( GROUP_BASE_DN )
                && !principalDn.equals( ADMIN_DN ) )
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
    public boolean hasEntry( NextInterceptor nextInterceptor, Name name ) throws NamingException
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
    public void modify( NextInterceptor nextInterceptor, Name name, int modOp, Attributes attrs ) throws NamingException
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
    public void modify( NextInterceptor nextInterceptor, Name name, ModificationItem[] items ) throws NamingException
    {
        if ( enabled )
        {
            protectModifyAlterations( name );
        }
        nextInterceptor.modify( name, items );
    }


    private void protectModifyAlterations( Name dn ) throws LdapNoPermissionException
    {
        Name principalDn = getPrincipal().getJndiName();

        if ( dn.toString().equals( "" ) )
        {
            String msg = "The rootDSE cannot be modified!";
            throw new LdapNoPermissionException( msg );
        }

        if ( !principalDn.equals( ADMIN_DN ) )
        {
            if ( dn.equals( DirectoryPartitionNexus.ADMIN_PRINCIPAL ) )
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
                msg += dn + ".\nGroups can only be modified by the admin.";
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


    public void modifyRn( NextInterceptor nextInterceptor, Name name, String newRn, boolean deleteOldRn ) throws NamingException
    {
        if ( enabled )
        {
            protectDnAlterations( name );
        }
        nextInterceptor.modifyRn( name, newRn, deleteOldRn );
    }


    public void move( NextInterceptor nextInterceptor, Name oriChildName, Name newParentName ) throws NamingException
    {
        if ( enabled )
        {
            protectDnAlterations( oriChildName );
        }
        nextInterceptor.move( oriChildName, newParentName );
    }


    public void move( NextInterceptor nextInterceptor,
            Name oriChildName, Name newParentName, String newRn,
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
        Name principalDn = getPrincipal().getJndiName();

        if ( dn.toString().equals( "" ) )
        {
            String msg = "The rootDSE cannot be moved or renamed!";
            throw new LdapNoPermissionException( msg );
        }

        if ( dn == ADMIN_DN || dn.equals( ADMIN_DN ) )
        {
            String msg = "User '" + principalDn;
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


    public Attributes lookup( NextInterceptor nextInterceptor, Name name ) throws NamingException
    {
        Attributes attributes = nextInterceptor.lookup( name );
        if ( ! enabled || attributes == null )
        {
            return attributes;
        }

        protectLookUp( name );
        return attributes;
    }


    public Attributes lookup( NextInterceptor nextInterceptor, Name name, String[] attrIds ) throws NamingException
    {
        Attributes attributes = nextInterceptor.lookup( name, attrIds );
        if ( ! enabled || attributes == null )
        {
            return attributes;
        }

        protectLookUp( name );
        return attributes;
    }


    private void protectLookUp( Name dn ) throws NamingException
    {
        LdapContext ctx =
            ( LdapContext ) InvocationStack.getInstance().peek().getCaller();
        Name principalDn = ( ( ServerContext ) ctx ).getPrincipal().getJndiName();

        if ( !principalDn.equals( ADMIN_DN ) )
        {
            if ( dn.size() > 2 && dn.startsWith( USER_BASE_DN ) )
            {
                // allow for self reads
                if ( dn.toString().equals( principalDn.toString() ) )
                {
                    return;
                }

                String msg = "Access to user account '" + dn + "' not permitted";
                msg += " for user '" + principalDn + "'.  Only the admin can";
                msg += " access user account information";
                throw new LdapNoPermissionException( msg );
            }

            if ( dn.size() > 2 && dn.startsWith( GROUP_BASE_DN ) )
            {
                // allow for self reads
                if ( dn.toString().equals( principalDn.toString() ) )
                {
                    return;
                }

                String msg = "Access to group '" + dn + "' not permitted";
                msg += " for user '" + principalDn + "'.  Only the admin can";
                msg += " access group information";
                throw new LdapNoPermissionException( msg );
            }

            if ( dn.equals( ADMIN_DN ) )
            {
                // allow for self reads
                if ( dn.toString().equals( principalDn.toString() ) )
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


    public NamingEnumeration search( NextInterceptor nextInterceptor,
            Name base, Map env, ExprNode filter,
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
        return new SearchResultFilteringEnumeration( e, searchCtls, invocation,
            new SearchResultFilter()
            {
                public boolean accept( Invocation invocation, SearchResult result, SearchControls controls )
                        throws NamingException
                {
                    return OldAuthorizationService.this.isSearchable( invocation, result );
                }
            });
    }


    public NamingEnumeration list( NextInterceptor nextInterceptor, Name base ) throws NamingException
    {
        NamingEnumeration e = nextInterceptor.list( base );
        if ( !enabled )
        {
            return e;
        }

        Invocation invocation = InvocationStack.getInstance().peek();
        return new SearchResultFilteringEnumeration( e, null, invocation,
            new SearchResultFilter()
            {
                public boolean accept( Invocation invocation, SearchResult result, SearchControls controls )
                        throws NamingException
                {
                    return OldAuthorizationService.this.isSearchable( invocation, result );
                }
            } );
    }


    private boolean isSearchable( Invocation invocataion, SearchResult result )
            throws NamingException
    {
        Name dn;

        synchronized ( dnParser )
        {
            dn = dnParser.parse( result.getName() );
        }

        Name principalDn = ( ( ServerContext ) invocataion.getCaller() ).getPrincipal().getJndiName();
        if ( !principalDn.equals( ADMIN_DN ) )
        {
            if ( dn.size() > 2 )
            {
                if ( dn.startsWith( USER_BASE_DN ) || dn.startsWith( GROUP_BASE_DN ) )
                {
                    return false;
                }
            }

            if ( dn.equals( ADMIN_DN ) )
            {
                return false;
            }

        }

        return true;
    }
}
