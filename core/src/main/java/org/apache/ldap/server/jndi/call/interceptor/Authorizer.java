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
package org.apache.ldap.server.jndi.call.interceptor;


import java.util.Properties;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.apache.ldap.common.exception.LdapNoPermissionException;
import org.apache.ldap.common.name.DnParser;
import org.apache.ldap.common.name.NameComponentNormalizer;
import org.apache.ldap.server.SystemPartition;
import org.apache.ldap.server.db.ResultFilteringEnumeration;
import org.apache.ldap.server.db.SearchResultFilter;
import org.apache.ldap.server.jndi.ServerContext;
import org.apache.ldap.server.jndi.call.Call;
import org.apache.ldap.server.jndi.call.Delete;
import org.apache.ldap.server.jndi.call.HasEntry;
import org.apache.ldap.server.jndi.call.Lookup;
import org.apache.ldap.server.jndi.call.LookupWithAttrIds;
import org.apache.ldap.server.jndi.call.Modify;
import org.apache.ldap.server.jndi.call.ModifyMany;
import org.apache.ldap.server.jndi.call.ModifyRN;
import org.apache.ldap.server.jndi.call.Move;
import org.apache.ldap.server.jndi.call.MoveAndModifyRN;
import org.apache.ldap.server.jndi.call.Search;


/**
 * A service used for applying access controls to backing store operations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class Authorizer extends BaseInterceptor
{
    /** the administrator's distinguished {@link Name} */
    private static final Name ADMIN_DN = SystemPartition.getAdminDn();
    /** the base distinguished {@link Name} for all users */
    private static final Name USER_BASE_DN = SystemPartition.getUsersBaseDn();
    /** the base distinguished {@link Name} for all groups */
    private static final Name GROUP_BASE_DN = SystemPartition.getGroupsBaseDn();

    /** the name parser used by this service */
    private final DnParser dnParser;


    /**
     * Creates an authorization service interceptor.
     *
     * @param normalizer a schema enabled name component normalizer
     * @param filterService a {@link FilterService} to register filters with
     */
    public Authorizer( NameComponentNormalizer normalizer )
            throws NamingException
    {
        this.dnParser = new DnParser( normalizer );
    }
    
    public void init( Properties config )
    {
    }
    
    public void destroy()
    {
    }

    // Note:
    //    Lookup, search and list operations need to be handled using a filter
    // and so we need access to the filter service.

    protected void process( NextInterceptor nextProcessor, Delete call ) throws NamingException
    {
        Name name = call.getName();
        Name principalDn = getPrincipal( call ).getDn();

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
                && ! principalDn.equals( ADMIN_DN ) )
        {
            String msg = "User " + principalDn;
            msg += " does not have permission to delete the user account: ";
            msg += name + ". Only the admin can delete user accounts.";
            throw new LdapNoPermissionException( msg );
        }

        if ( name.size() > 2 && name.startsWith( GROUP_BASE_DN )
                && ! principalDn.equals( ADMIN_DN ) )
        {
            String msg = "User " + principalDn;
            msg += " does not have permission to delete the group entry: ";
            msg += name + ". Only the admin can delete groups.";
            throw new LdapNoPermissionException( msg );
        }
        
        nextProcessor.process( call );
    }


    /**
     * Note that we do nothing here. First because this is not an externally
     * exposed function via the JNDI interfaces.  It is used internally by the
     * provider for optimization purposes so there is no reason for us to start
     * to constrain it.
     *
     * @see org.apache.ldap.server.jndi.BaseInterceptor#hasEntry(Name)
     */
    protected void process( NextInterceptor nextProcessor, HasEntry call ) throws NamingException
    {
        super.process( nextProcessor, call );
    }


    // ------------------------------------------------------------------------
    // Entry Modification Operations
    // ------------------------------------------------------------------------


    /**
     * This policy needs to be really tight too because some attributes may
     * take part in giving the user permissions to protected resources.  We
     * do not want users to self access these resources.  As far as we're
     * concerned no one but the admin needs access.
     *
     * @see BaseInterceptor#modify(Name, int, Attributes)
     */
    protected void process( NextInterceptor nextProcessor, Modify call ) throws NamingException
    {
        protectModifyAlterations( call, call.getName() );
        nextProcessor.process( call );
    }


    /**
     * This policy needs to be really tight too because some attributes may
     * take part in giving the user permissions to protected resources.  We
     * do not want users to self access these resources.  As far as we're
     * concerned no one but the admin needs access.
     *
     * @see BaseInterceptor#modify(Name, ModificationItem[])
     */
    protected void process( NextInterceptor nextProcessor, ModifyMany call ) throws NamingException
    {
        protectModifyAlterations( call, call.getName() );
        nextProcessor.process( call );
    }


    private void protectModifyAlterations( Call call, Name dn ) throws LdapNoPermissionException
    {
        Name principalDn = getPrincipal( call ).getDn();

        if ( dn.toString().equals( "" ) )
        {
            String msg = "The rootDSE cannot be modified!";
            throw new LdapNoPermissionException( msg );
        }

        if ( ! principalDn.equals( ADMIN_DN ) )
        {
            if ( dn == ADMIN_DN || dn.equals( ADMIN_DN ) )
            {
                String msg = "User " + principalDn;
                msg += " does not have permission to modify the admin account.";
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


    protected void process( NextInterceptor nextProcessor, ModifyRN call ) throws NamingException
    {
        protectDnAlterations( call, call.getName() );
        nextProcessor.process( call );
    }


    protected void process( NextInterceptor nextProcessor, Move call ) throws NamingException
    {
        protectDnAlterations( call, call.getName() );
        nextProcessor.process( call );
    }


    protected void process( NextInterceptor nextProcessor, MoveAndModifyRN call ) throws NamingException
    {
        protectDnAlterations( call, call.getName() );
        nextProcessor.process( call );
    }


    private void protectDnAlterations( Call call, Name dn ) throws LdapNoPermissionException
    {
        Name principalDn = getPrincipal( call ).getDn();

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

        if ( dn.size() > 2 && dn.startsWith( USER_BASE_DN ) && ! principalDn.equals( ADMIN_DN ) )
        {
            String msg = "User '" + principalDn;
            msg += "' does not have permission to move or rename the user";
            msg += " account: " + dn + ". Only the admin can move or";
            msg += " rename user accounts.";
            throw new LdapNoPermissionException( msg );
        }

        if ( dn.size() > 2 && dn.startsWith( GROUP_BASE_DN ) && ! principalDn.equals( ADMIN_DN ) )
        {
            String msg = "User " + principalDn;
            msg += " does not have permission to move or rename the group entry ";
            msg += dn + ".\nGroups can only be moved or renamed by the admin.";
            throw new LdapNoPermissionException( msg );
        }
    }
    
    protected void process(NextInterceptor nextProcessor, Lookup call) throws NamingException {
        super.process(nextProcessor, call);
        
        Attributes attributes = ( Attributes ) call.getResponse();
        if( attributes == null )
        {
            return;
        }

        Attributes retval = ( Attributes ) attributes.clone();
        LdapContext ctx = ( LdapContext ) call.getContextStack().peek();
        protectLookUp( ctx, call.getName() );
        call.setResponse( retval );
    }

    protected void process(NextInterceptor nextProcessor, LookupWithAttrIds call) throws NamingException {
        super.process(nextProcessor, call);
        
        Attributes attributes = ( Attributes ) call.getResponse();
        if( attributes == null )
        {
            return;
        }

        Attributes retval = ( Attributes ) attributes.clone();
        LdapContext ctx = ( LdapContext ) call.getContextStack().peek();
        protectLookUp( ctx, call.getName() );
        call.setResponse( retval );
    }
    
    private void protectLookUp( LdapContext ctx, Name dn ) throws NamingException
    {
        Name principalDn = ( ( ServerContext ) ctx ).getPrincipal().getDn();

        if ( ! principalDn.equals( ADMIN_DN ) )
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
    
    protected void process(NextInterceptor nextProcessor, Search call) throws NamingException {
        super.process(nextProcessor, call);
        
        SearchControls searchControls = call.getControls();
        if ( searchControls.getReturningAttributes() != null )
        {
            return;
        }

        NamingEnumeration e ;
        ResultFilteringEnumeration retval;
        LdapContext ctx = ( LdapContext ) call.getContextStack().peek();
        e = ( NamingEnumeration ) call.getResponse();
        retval = new ResultFilteringEnumeration( e, searchControls, ctx,
            new SearchResultFilter()
            {
                public boolean accept( LdapContext ctx, SearchResult result,
                                       SearchControls controls )
                        throws NamingException
                {
                    return Authorizer.this.isSearchable( ctx, result );
                }
            } );

        call.setResponse( retval );
    }

    private boolean isSearchable( LdapContext ctx, SearchResult result )
            throws NamingException
    {
        Name dn;

        synchronized( dnParser )
        {
            dn = dnParser.parse( result.getName() );
        }
        
        Name principalDn = ( ( ServerContext ) ctx ).getPrincipal().getDn();
        if ( ! principalDn.equals( ADMIN_DN ) )
        {
            if ( dn.size() > 2  )
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
