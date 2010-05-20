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
import java.util.Set;

import javax.naming.NoPermissionException;

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DefaultCoreSession;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.LdapPrincipal;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.filtering.EntryFilter;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchingOperationContext;
import org.apache.directory.server.core.partition.DefaultPartitionNexus;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapNoPermissionException;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An {@link Interceptor} that controls access to {@link DefaultPartitionNexus}.
 * If a user tries to perform any operations that requires
 * permission he or she doesn't have, {@link NoPermissionException} will be
 * thrown and therefore the current invocation chain will terminate.
 *
 * @org.apache.xbean.XBean
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DefaultAuthorizationInterceptor extends BaseInterceptor
{
    /** the logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultAuthorizationInterceptor.class );

    /** the base distinguished {@link Name} for the admin system */
    private static DN ADMIN_SYSTEM_DN;

    /** the base distinguished {@link Name} for all groups */
    private static DN GROUP_BASE_DN;

    /** the distinguished {@link Name} for the administrator group */
    private static DN ADMIN_GROUP_DN;

    private Set<String> administrators = new HashSet<String>(2);
    
    private PartitionNexus nexus;

    /** A starage for the uniqueMember attributeType */
    private AttributeType uniqueMemberAT;


    /**
     * Creates a new instance.
     */
    public DefaultAuthorizationInterceptor()
    {
        // Nothing to do
    }


    public void init( DirectoryService directoryService ) throws Exception
    {
        nexus = directoryService.getPartitionNexus();
        SchemaManager schemaManager = directoryService.getSchemaManager();

        ADMIN_SYSTEM_DN = new DN( ServerDNConstants.ADMIN_SYSTEM_DN );
        ADMIN_SYSTEM_DN.normalize( schemaManager.getNormalizerMapping() );
        
        GROUP_BASE_DN = new DN( ServerDNConstants.GROUPS_SYSTEM_DN );
        GROUP_BASE_DN.normalize( schemaManager.getNormalizerMapping() );
     
        ADMIN_GROUP_DN = new DN( ServerDNConstants.ADMINISTRATORS_GROUP_DN );
        ADMIN_GROUP_DN.normalize( schemaManager.getNormalizerMapping() );

        uniqueMemberAT = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.UNIQUE_MEMBER_AT_OID );
        
        loadAdministrators( directoryService );
    }
    
    
    private void loadAdministrators( DirectoryService directoryService ) throws Exception
    {
        // read in the administrators and cache their normalized names
        Set<String> newAdministrators = new HashSet<String>( 2 );
        DN adminDn = new DN( ServerDNConstants.ADMIN_SYSTEM_DN_NORMALIZED );
        adminDn.normalize( directoryService.getSchemaManager().getNormalizerMapping() );
        CoreSession adminSession = new DefaultCoreSession( 
            new LdapPrincipal( adminDn, AuthenticationLevel.STRONG ), directoryService );

        Entry adminGroup = nexus.lookup( new LookupOperationContext( adminSession, ADMIN_GROUP_DN ) );
        
        if ( adminGroup == null )
        {
            return;
        }
        
        EntryAttribute uniqueMember = adminGroup.get( uniqueMemberAT );
        
        for ( Value<?> value:uniqueMember )
        {
            DN memberDn = new DN( value.getString() );
            memberDn.normalize( directoryService.getSchemaManager().getNormalizerMapping() );
            newAdministrators.add( memberDn.getNormName() );
        }
        
        administrators = newAdministrators;
    }

    
    // Note:
    //    Lookup, search and list operations need to be handled using a filter
    // and so we need access to the filter service.

    public void delete( NextInterceptor nextInterceptor, DeleteOperationContext opContext ) throws Exception
    {
        if ( opContext.getSession().getDirectoryService().isAccessControlEnabled() )
        {
            nextInterceptor.delete( opContext );
            return;
        }

        DN dn = opContext.getDn();

        if ( dn.isEmpty() )
        {
            String msg = I18n.err( I18n.ERR_12 );
            LOG.error( msg );
            throw new LdapNoPermissionException( msg );
        }

        if ( dn.equals( ADMIN_GROUP_DN ) )
        {
            String msg = I18n.err( I18n.ERR_13 );
            LOG.error( msg );
            throw new LdapNoPermissionException( msg );
        }
        
        DN principalDN = getPrincipal().getDNRef();

        if ( dn.equals( ADMIN_SYSTEM_DN ) )
        {
            String msg = I18n.err( I18n.ERR_14, principalDN.getName() );
            LOG.error( msg );
            throw new LdapNoPermissionException( msg );
        }

        if ( dn.size() > 2 )
        {
            if ( !isAnAdministrator( principalDN ) )
            {
                if ( dn.isChildOf( ADMIN_SYSTEM_DN ) )
                {
                    String msg = I18n.err( I18n.ERR_15, principalDN.getName(), dn.getName() );
                    LOG.error( msg );
                    throw new LdapNoPermissionException( msg );
                }
        
                if ( dn.isChildOf( GROUP_BASE_DN ) )
                {
                    String msg = I18n.err( I18n.ERR_16, principalDN.getName(), dn.getName() );
                    LOG.error( msg );
                    throw new LdapNoPermissionException( msg );
                }
            }
        }

        nextInterceptor.delete( opContext );
    }

    
    private boolean isTheAdministrator( DN normalizedDn )
    {
        return normalizedDn.equals( ADMIN_SYSTEM_DN );
    }
    
    
    private boolean isAnAdministrator( DN dn )
    {
        return isTheAdministrator( dn ) || administrators.contains( dn.getNormName() );
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
    public void modify( NextInterceptor nextInterceptor, ModifyOperationContext opContext )
        throws Exception
    {
        if ( !opContext.getSession().getDirectoryService().isAccessControlEnabled() )
        {
            DN dn = opContext.getDn();
            
            protectModifyAlterations( dn );
            nextInterceptor.modify( opContext );

            // update administrators if we change administrators group
            if ( dn.getNormName().equals( ADMIN_GROUP_DN.getNormName() ) )
            {
                loadAdministrators( opContext.getSession().getDirectoryService() );
            }
        }
        else
        {
            nextInterceptor.modify( opContext );
        }
    }


    private void protectModifyAlterations( DN dn ) throws Exception
    {
        DN principalDn = getPrincipal().getDN();

        if ( dn.isEmpty() )
        {
            String msg = I18n.err( I18n.ERR_17 );
            LOG.error( msg );
            throw new LdapNoPermissionException( msg );
        }

        if ( ! isAnAdministrator( principalDn ) )
        {
            // allow self modifications 
            if ( dn.getNormName().equals( getPrincipal().getName() ) )
            {
                return;
            }
            
            if ( dn.getNormName().equals( ServerDNConstants.ADMIN_SYSTEM_DN_NORMALIZED ) )
            {
                String msg = I18n.err( I18n.ERR_18, principalDn.getName() );
                LOG.error( msg );
                throw new LdapNoPermissionException( msg );
            }

            if ( dn.size() > 2 ) 
                {
                if ( dn.isChildOf( ADMIN_SYSTEM_DN ) )
                {
                    String msg = I18n.err( I18n.ERR_19, principalDn.getName(),  dn.getName() );
                    LOG.error( msg );
                    throw new LdapNoPermissionException( msg );
                }
    
                if ( dn.isChildOf( GROUP_BASE_DN ) )
                {
                    String msg = I18n.err( I18n.ERR_20, principalDn.getName(), dn.getName() );
                    LOG.error( msg );
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

    public void rename( NextInterceptor nextInterceptor, RenameOperationContext opContext )
        throws Exception
    {
        if ( !opContext.getSession().getDirectoryService().isAccessControlEnabled() )
        {
            protectDnAlterations( opContext.getDn() );
        }
        
        nextInterceptor.rename( opContext );
    }


    public void move( NextInterceptor nextInterceptor, MoveOperationContext opContext ) throws Exception
    {
        if ( !opContext.getSession().getDirectoryService().isAccessControlEnabled() )
        {
            protectDnAlterations( opContext.getDn() );
        }
        
        nextInterceptor.move( opContext );
    }


    public void moveAndRename( NextInterceptor nextInterceptor, MoveAndRenameOperationContext opContext ) throws Exception
    {
        if ( !opContext.getSession().getDirectoryService().isAccessControlEnabled() )
        {
            protectDnAlterations( opContext.getDn() );
        }
        
        nextInterceptor.moveAndRename( opContext );
    }


    private void protectDnAlterations( DN dn ) throws Exception
    {
        DN principalDn = getPrincipal().getDN();

        if ( dn.isEmpty() )
        {
            String msg = I18n.err( I18n.ERR_234 );
            LOG.error( msg );
            throw new LdapNoPermissionException( msg );
        }

        if ( dn.getNormName().equals( ADMIN_GROUP_DN.getNormName() ) )
        {
            String msg = I18n.err( I18n.ERR_21 );
            LOG.error( msg );
            throw new LdapNoPermissionException( msg );
        }
        
        if ( isTheAdministrator( dn ) )
        {
            String msg = I18n.err( I18n.ERR_22, principalDn.getName(), dn.getName() );
            LOG.error( msg );
            throw new LdapNoPermissionException( msg );
        }

        if ( dn.size() > 2 && dn.isChildOf( ADMIN_SYSTEM_DN ) && !isAnAdministrator( principalDn ) )
        {
            String msg = I18n.err( I18n.ERR_23, principalDn.getName(), dn.getName() );
            LOG.error( msg );
            throw new LdapNoPermissionException( msg );
        }

        if ( dn.size() > 2 && dn.isChildOf( GROUP_BASE_DN ) && !isAnAdministrator( principalDn ) )
        {
            String msg = I18n.err( I18n.ERR_24, principalDn.getName(), dn.getName() );
            LOG.error( msg );
            throw new LdapNoPermissionException( msg );
        }
    }


    public Entry lookup( NextInterceptor nextInterceptor, LookupOperationContext opContext ) throws Exception
    {
        CoreSession session = opContext.getSession();
        Entry entry = nextInterceptor.lookup( opContext );
        
        if ( session.getDirectoryService().isAccessControlEnabled() || ( entry == null ) )
        {
            return entry;
        }

        protectLookUp( session.getEffectivePrincipal().getDN(), opContext.getDn() );
        
        return entry;
    }


    private void protectLookUp( DN principalDn, DN normalizedDn ) throws Exception
    {
        if ( !isAnAdministrator( principalDn ) )
        {
            if ( normalizedDn.size() > 2 )
            {
                if( normalizedDn.isChildOf( ADMIN_SYSTEM_DN ) )
                {
                    // allow for self reads
                    if ( normalizedDn.getNormName().equals( principalDn.getNormName() ) )
                    {
                        return;
                    }
    
                    String msg = I18n.err( I18n.ERR_25, normalizedDn.getName(), principalDn.getName() );
                    LOG.error( msg );
                    throw new LdapNoPermissionException( msg );
                }

                if ( normalizedDn.isChildOf( GROUP_BASE_DN ) )
                {
                    // allow for self reads
                    if ( normalizedDn.getNormName().equals( principalDn.getNormName() ) )
                    {
                        return;
                    }
    
                    String msg = I18n.err( I18n.ERR_26, normalizedDn.getName(), principalDn.getName() );
                    LOG.error( msg );
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

                String msg = I18n.err( I18n.ERR_27,  principalDn.getName() );
                LOG.error( msg );
                throw new LdapNoPermissionException( msg );
            }
        }
    }


    public EntryFilteringCursor search( NextInterceptor nextInterceptor, SearchOperationContext opContext ) throws Exception
    {
        EntryFilteringCursor cursor = nextInterceptor.search( opContext );

        if ( opContext.getSession().getDirectoryService().isAccessControlEnabled() )
        {
            return cursor;
        }

        cursor.addEntryFilter( new EntryFilter() {
            public boolean accept( SearchingOperationContext operation, ClonedServerEntry result ) throws Exception
            {
                return DefaultAuthorizationInterceptor.this.isSearchable( operation, result );
            }
        } );
        return cursor;
    }


    public EntryFilteringCursor list( NextInterceptor nextInterceptor, ListOperationContext opContext ) throws Exception
    {
        EntryFilteringCursor cursor = nextInterceptor.list( opContext );
        
        if ( opContext.getSession().getDirectoryService().isAccessControlEnabled() )
        {
            return cursor;
        }

        cursor.addEntryFilter( new EntryFilter()
        {
            public boolean accept( SearchingOperationContext operation, ClonedServerEntry entry ) throws Exception
            {
                return DefaultAuthorizationInterceptor.this.isSearchable( operation, entry );
            }
        } );
        return cursor;
    }


    private boolean isSearchable( OperationContext opContext, ClonedServerEntry result ) throws Exception
    {
        DN principalDn = opContext.getSession().getEffectivePrincipal().getDN();
        DN dn = result.getDn();
        
        if ( !dn.isNormalized() )
        {
            dn.normalize( opContext.getSession().getDirectoryService().getSchemaManager().getNormalizerMapping() );
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
            
            if ( dn.getNormName().endsWith( ADMIN_SYSTEM_DN.getNormName() ) 
                || dn.getNormName().endsWith( GROUP_BASE_DN.getNormName() ) )
            {
                return false;
            }
        }
        
        // Non-admin users cannot read the admin entry
        return ! isTheAdministrator( dn );

    }
}
