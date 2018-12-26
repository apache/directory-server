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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.naming.directory.SearchControls;

import org.ehcache.Cache;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapOperationException;
import org.apache.directory.api.ldap.model.filter.BranchNode;
import org.apache.directory.api.ldap.model.filter.EqualityNode;
import org.apache.directory.api.ldap.model.filter.OrNode;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.api.CacheService;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.DnFactory;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.PartitionNexus;
import org.apache.directory.server.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A cache for tracking static group membership.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class GroupCache
{
    /** the logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( GroupCache.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** a handle on the partition nexus */
    private final PartitionNexus nexus;

    /** the directory service */
    private final DirectoryService directoryService;

    /**
     * the schema manager
     */
    private SchemaManager schemaManager;

    /** the Dn factory */
    private DnFactory dnFactory;

    /** the normalized dn of the administrators group */
    private Dn administratorsGroupDn;
    
    /** The Admin user DN */
    private Dn adminSystemDn;

    private static final Set<String> EMPTY_GROUPS = new HashSet<>();

    /** String key for the Dn of a group to a Set (HashSet) for the Strings of member DNs */
    @SuppressWarnings("rawtypes")
    private Cache< String, Set > groupCache;



    /**
     * Creates a static group cache.
     *
     * @param dirService the directory service core
     * @throws LdapException if there are failures on initialization
     */
    public GroupCache( DirectoryService dirService ) throws LdapException
    {
        this.directoryService = dirService;
        schemaManager = dirService.getSchemaManager();
        dnFactory = dirService.getDnFactory();
        nexus = dirService.getPartitionNexus();

        // stuff for dealing with the admin group
        administratorsGroupDn = parseNormalized( ServerDNConstants.ADMINISTRATORS_GROUP_DN );

        groupCache = dirService.getCacheService().getCache( "groupCache", String.class, Set.class );

        initialize( dirService.getAdminSession() );
    }


    private Dn parseNormalized( String name ) throws LdapException
    {
        return dnFactory.create( name );
    }


    private void initialize( CoreSession session ) throws LdapException
    {
        // search all naming contexts for static groups and generate
        // normalized sets of members to cache within the map

        Set<String> suffixes = nexus.listSuffixes();

        for ( String suffix : suffixes )
        {
            // moving the filter creation to inside loop to fix DIRSERVER-1121
            // didn't use clone() cause it is creating List objects, which IMO is not worth calling
            // in this initialization phase
            BranchNode filter = new OrNode();
            AttributeType ocAt = directoryService.getAtProvider().getObjectClass();

            filter.addNode( new EqualityNode<String>( ocAt, new Value( ocAt, SchemaConstants.GROUP_OF_NAMES_OC ) ) );
            filter.addNode( new EqualityNode<String>( ocAt,
                new Value( ocAt, SchemaConstants.GROUP_OF_UNIQUE_NAMES_OC ) ) );

            Dn baseDn = dnFactory.create( suffix );
            SearchControls ctls = new SearchControls();
            ctls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            ctls.setReturningAttributes( new String[]
                { SchemaConstants.ALL_USER_ATTRIBUTES, SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES } );
            
            Partition partition = nexus.getPartition( baseDn );

            SearchOperationContext searchOperationContext = new SearchOperationContext( session,
                baseDn, filter, ctls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.DEREF_ALWAYS );
            searchOperationContext.setPartition( partition );
            searchOperationContext.setTransaction( partition.beginReadTransaction() );
            EntryFilteringCursor results = nexus.search( searchOperationContext );

            try
            {
                while ( results.next() )
                {
                    Entry result = results.get();
                    Dn groupDn = result.getDn();
                    
                    if ( !groupDn.isSchemaAware() )
                    {
                        groupDn = new Dn( schemaManager, groupDn );
                    }
                    
                    Attribute members = getMemberAttribute( result );

                    if ( members != null )
                    {
                        Set<String> memberSet = new HashSet<>( members.size() );
                        addMembers( memberSet, members );

                        groupCache.put( groupDn.getNormName(), memberSet );
                    }
                    else
                    {
                        LOG.warn( "Found group '{}' without any member or uniqueMember attributes", groupDn.getName() );
                    }
                }

                results.close();
            }
            catch ( Exception e )
            {
                LOG.error( "Exception while initializing the groupCache:  {}", e.getCause() );
                throw new LdapOperationException( e.getMessage(), e );
            }
        }
        
        adminSystemDn = new Dn( schemaManager, ServerDNConstants.ADMIN_SYSTEM_DN_NORMALIZED );

        if ( IS_DEBUG )
        {
            LOG.debug( "group cache contents on startup:\n {}", CacheService.dumpCacheContentsToString( groupCache ) );
        }
    }


    /**
     * Gets the member attribute regardless of whether groupOfNames or
     * groupOfUniqueNames is used.
     *
     * @param entry the entry inspected for member attributes
     * @return the member attribute
     */
    private Attribute getMemberAttribute( Entry entry )
    {
        Attribute member = entry.get( directoryService.getAtProvider().getMember() );

        if ( member != null )
        {
            return member;
        }

        Attribute uniqueMember = entry.get( directoryService.getAtProvider().getUniqueMember() );

        if ( uniqueMember != null )
        {
            return uniqueMember;
        }

        return null;
    }


    /**
     * Adds normalized member DNs to the set of normalized member names.
     *
     * @param memberSet the set of member Dns (Strings)
     * @param members the member attribute values being added
     * @throws LdapException if there are problems accessing the attr values
     */
    private void addMembers( Set<String> memberSet, Attribute members ) throws LdapException
    {
        for ( Value value : members )
        {

            // get and normalize the Dn of the member
            String member = value.getValue();
            Dn memberDn = null;

            try
            {
                memberDn = parseNormalized( member );
            }
            catch ( LdapException e )
            {
                LOG.warn( "Malformed member Dn in groupOf[Unique]Names entry.  Member not added to GroupCache.", e );
                continue;
            }

            memberSet.add( memberDn.getNormName() );
        }
    }


    /**
     * Removes a set of member names from an existing set.
     *
     * @param memberSet the set of normalized member DNs
     * @param members the set of member values
     * @throws LdapException if there are problems accessing the attr values
     */
    private void removeMembers( Set<String> memberSet, Attribute members ) throws LdapException
    {
        for ( Value value : members )
        {
            // get and normalize the Dn of the member
            String member = value.getValue();
            Dn memberDn = null;

            try
            {
                memberDn = parseNormalized( member );
            }
            catch ( LdapException e )
            {
                LOG.warn( "Malformed member Dn in groupOf[Unique]Names entry.  Member not removed from GroupCache.", e );
                continue;
            }

            memberSet.remove( memberDn.getNormName() );
        }
    }


    /**
     * Adds a groups members to the cache.  Called by interceptor to account for new
     * group additions.
     *
     * @param name the user provided name for the group entry
     * @param entry the group entry's attributes
     * @throws LdapException if there are problems accessing the attr values
     */
    public void groupAdded( String name, Entry entry ) throws LdapException
    {
        Attribute members = getMemberAttribute( entry );

        if ( members == null )
        {
            return;
        }

        Set<String> memberSet = new HashSet<>( members.size() );
        addMembers( memberSet, members );

        groupCache.put( name, memberSet );

        if ( IS_DEBUG )
        {
            LOG.debug( "group cache contents after adding '{}' :\n {}", name,
                CacheService.dumpCacheContentsToString( groupCache ) );
        }
    }


    /**
     * Deletes a group's members from the cache.  Called by interceptor to account for
     * the deletion of groups.
     *
     * @param name the normalized Dn of the group entry
     * @param entry the attributes of entry being deleted
     * @throws LdapException If we wasn't able to delete the entry from the cache
     */
    public void groupDeleted( Dn name, Entry entry ) throws LdapException
    {
        Attribute members = getMemberAttribute( entry );

        if ( members == null )
        {
            return;
        }

        groupCache.remove( name.getNormName() );

        if ( IS_DEBUG )
        {
            LOG.debug( "group cache contents after deleting '{}' :\n {}", name.getName(),
                CacheService.dumpCacheContentsToString( groupCache ) );
        }
    }


    /**
     * Utility method to modify a set of member names based on a modify operation
     * that changes the members of a group.
     *
     * @param memberSet the set of members to be altered
     * @param modOp the type of modify operation being performed
     * @param members the members being added, removed or replaced
     * @throws LdapException if there are problems accessing attribute values
     */
    private void modify( Set<String> memberSet, ModificationOperation modOp, Attribute members )
        throws LdapException
    {

        switch ( modOp )
        {
            case ADD_ATTRIBUTE:
                addMembers( memberSet, members );
                break;

            case REPLACE_ATTRIBUTE:
                if ( members.size() > 0 )
                {
                    memberSet.clear();
                    addMembers( memberSet, members );
                }

                break;

            case REMOVE_ATTRIBUTE:
                removeMembers( memberSet, members );
                break;

            default:
                throw new InternalError( I18n.err( I18n.ERR_235, modOp ) );
        }
    }


    /**
     * Modifies the cache to reflect changes via modify operations to the group entries.
     * Called by the interceptor to account for modify ops on groups.
     *
     * @param name the normalized name of the group entry modified
     * @param mods the modification operations being performed
     * @param entry the group entry being modified
     * @param schemaManager The SchemaManager instance
     * @throws LdapException if there are problems accessing attribute  values
     */
    public void groupModified( Dn name, List<Modification> mods, Entry entry, SchemaManager schemaManager )
        throws LdapException
    {
        Attribute members = null;
        AttributeType memberAttr = null;
        Attribute oc = entry.get( directoryService.getAtProvider().getObjectClass() );

        if ( oc.contains( SchemaConstants.GROUP_OF_NAMES_OC ) )
        {
            memberAttr = directoryService.getAtProvider().getMember();
            members = entry.get( memberAttr );
        }

        if ( oc.contains( SchemaConstants.GROUP_OF_UNIQUE_NAMES_OC ) )
        {
            memberAttr = directoryService.getAtProvider().getUniqueMember();
            members = entry.get( memberAttr );
        }

        if ( members == null )
        {
            return;
        }

        for ( Modification modification : mods )
        {
            if ( memberAttr.getOid() == modification.getAttribute().getId() )
            {
                @SuppressWarnings("unchecked")
                Set<String> memberSet = groupCache.get( name.getNormName() );

                if ( memberSet != null )
                {
                    modify( memberSet, modification.getOperation(), modification.getAttribute() );
                }

                break;
            }
        }

        if ( IS_DEBUG )
        {
            LOG.debug( "group cache contents after modifying '{}' :\n {}", name.getName(),
                CacheService.dumpCacheContentsToString( groupCache ) );
        }
    }


    /**
     * Modifies the cache to reflect changes via modify operations to the group entries.
     * Called by the interceptor to account for modify ops on groups.
     *
     * @param name the normalized name of the group entry modified
     * @param modOp the modify operation being performed
     * @param mods the modifications being performed
     * @throws LdapException if there are problems accessing attribute  values
     */
    public void groupModified( Dn name, ModificationOperation modOp, Entry mods ) throws LdapException
    {
        Attribute members = getMemberAttribute( mods );

        if ( members == null )
        {
            return;
        }

        @SuppressWarnings("unchecked")
        Set<String> memberSet = groupCache.get( name.getNormName() );

        if ( memberSet != null )
        {
            modify( memberSet, modOp, members );
        }

        if ( IS_DEBUG )
        {
            LOG.debug( "group cache contents after modifying '{}' :\n {}", name.getName(),
                CacheService.dumpCacheContentsToString( groupCache ) );
        }
    }


    /**
     * An optimization.  By having this method here we can directly access the group
     * membership information and lookup to see if the principalDn is contained within.
     *
     * @param principalDn the normalized Dn of the user to check if they are an admin
     * @return true if the principal is an admin or the admin
     */
    public final boolean isPrincipalAnAdministrator( String principalDn )
    {
        if ( principalDn.equals( adminSystemDn.getNormName() ) )
        {
            return true;
        }

        @SuppressWarnings("unchecked")
        Set<String> members = groupCache.get( administratorsGroupDn.getNormName() );

        if ( members == null )
        {
            LOG.warn( "What do you mean there is no administrators group? This is bad news." );
            return false;
        }
        else
        {
            return members.contains( principalDn );
        }
    }


    /**
     * Gets the set of groups a user is a member of.  The groups are returned
     * as normalized Name objects within the set.
     *
     * @param memberDn the member (user) to get the groups for
     * @return a Set of Name objects representing the groups
     * @throws LdapException if there are problems accessing attribute  values
     */
    public Set<String> getGroups( String memberDn ) throws LdapException
    {
        Set<String> memberGroups = null;

        @SuppressWarnings("rawtypes")
        Iterator<Cache.Entry<String, Set>> iterator = groupCache.iterator();
        while ( iterator.hasNext() )
        {
            @SuppressWarnings("rawtypes")
            Cache.Entry<String, Set> next = iterator.next();
            String group = next.getKey();
            @SuppressWarnings("unchecked")
            Set<String> members = next.getValue();

            if ( members == null )
            {
                continue;
            }

            if ( members.contains( memberDn ) )
            {
                if ( memberGroups == null )
                {
                    memberGroups = new HashSet<>();
                }

                memberGroups.add( group );
            }
        }

        if ( memberGroups == null )
        {
            return EMPTY_GROUPS;
        }

        return memberGroups;
    }


    public boolean groupRenamed( Dn oldName, Dn newName )
    {
        @SuppressWarnings("unchecked")
        Set<String> members = groupCache.get( oldName.getNormName() );

        if ( members != null )
        {
            groupCache.remove( oldName.getNormName() );

            groupCache.put( newName.getNormName(), members );

            if ( IS_DEBUG )
            {
                LOG.debug( "group cache contents after renaming '{}' :\n{}", oldName.getName(),
                    CacheService.dumpCacheContentsToString( groupCache ) );
            }

            return true;
        }

        return false;
    }
}
