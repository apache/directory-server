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
import java.util.List;
import java.util.Set;

import javax.naming.directory.SearchControls;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DnFactory;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.EntryAttribute;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.entry.StringValue;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapOperationException;
import org.apache.directory.shared.ldap.model.filter.BranchNode;
import org.apache.directory.shared.ldap.model.filter.EqualityNode;
import org.apache.directory.shared.ldap.model.filter.OrNode;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.MutableAttributeTypeImpl;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
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

    /** A storage for the ObjectClass attributeType */
    private MutableAttributeTypeImpl OBJECT_CLASS_AT;

    /** A storage for the member attributeType */
    private MutableAttributeTypeImpl MEMBER_AT;

    /** A storage for the uniqueMember attributeType */
    private MutableAttributeTypeImpl UNIQUE_MEMBER_AT;

    /**
     * the schema manager
     */
    private SchemaManager schemaManager;
    
    /** the Dn factory */
    private DnFactory dnFactory;

    /** the normalized dn of the administrators group */
    private Dn administratorsGroupDn;

    private static final Set<Dn> EMPTY_GROUPS = new HashSet<Dn>();

    /** String key for the Dn of a group to a Set (HashSet) for the Strings of member DNs */
    private Cache ehCache;

    /**
     * Creates a static group cache.
     *
     * @param dirService the directory service core
     * @throws LdapException if there are failures on initialization
     */
    public GroupCache( DirectoryService dirService ) throws LdapException
    {
        schemaManager = dirService.getSchemaManager();
        dnFactory = dirService.getDnFactory();
        nexus = dirService.getPartitionNexus();
        OBJECT_CLASS_AT = schemaManager.getAttributeType( SchemaConstants.OBJECT_CLASS_AT );
        MEMBER_AT = schemaManager.getAttributeType( SchemaConstants.MEMBER_AT );
        UNIQUE_MEMBER_AT = schemaManager.getAttributeType( SchemaConstants.UNIQUE_MEMBER_AT );

        // stuff for dealing with the admin group
        administratorsGroupDn = parseNormalized( ServerDNConstants.ADMINISTRATORS_GROUP_DN );

        this.ehCache = dirService.getCacheService().getCache( "groupCache" );
        
        initialize( dirService.getAdminSession() );
    }


    private Dn parseNormalized( String name ) throws LdapException
    {
        Dn dn = dnFactory.create( name );
        return dn;
    }


    private void initialize( CoreSession session ) throws LdapException
    {
        // search all naming contexts for static groups and generate
        // normalized sets of members to cache within the map

        Set<String> suffixes = nexus.listSuffixes();

        for ( String suffix:suffixes )
        {
            // moving the filter creation to inside loop to fix DIRSERVER-1121
            // didn't use clone() cause it is creating List objects, which IMO is not worth calling
            // in this initialization phase
            BranchNode filter = new OrNode();
            filter.addNode( new EqualityNode<String>( OBJECT_CLASS_AT, new StringValue(
                SchemaConstants.GROUP_OF_NAMES_OC ) ) );
            filter.addNode( new EqualityNode<String>( OBJECT_CLASS_AT, new StringValue(
                SchemaConstants.GROUP_OF_UNIQUE_NAMES_OC ) ) );

            Dn baseDn = dnFactory.create( suffix );
            SearchControls ctls = new SearchControls();
            ctls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            
            SearchOperationContext searchOperationContext = new SearchOperationContext( session,
                baseDn, filter, ctls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.DEREF_ALWAYS );
            EntryFilteringCursor results = nexus.search( searchOperationContext );

            try
            {
                while ( results.next() )
                {
                    Entry result = results.get();
                    Dn groupDn = result.getDn().normalize( schemaManager );
                    EntryAttribute members = getMemberAttribute( result );
    
                    if ( members != null )
                    {
                        Set<String> memberSet = new HashSet<String>( members.size() );
                        addMembers( memberSet, members );
                        
                        Element cacheElement = new Element( groupDn.getNormName(), memberSet );
                        ehCache.put( cacheElement );
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
                LdapOperationException le = new LdapOperationException( e.getMessage() );
                le.initCause( e );
                throw le;
            }
        }

        if ( IS_DEBUG )
        {
            LOG.debug( "group cache contents on startup:\n {}", ehCache.getAllWithLoader( ehCache.getKeys(), null ) );
        }
    }


    /**
     * Gets the member attribute regardless of whether groupOfNames or
     * groupOfUniqueNames is used.
     *
     * @param entry the entry inspected for member attributes
     * @return the member attribute
     */
    private EntryAttribute getMemberAttribute( Entry entry ) throws LdapException
    {
        EntryAttribute member = entry.get( MEMBER_AT );

        if ( member != null )
        {
            return member;
        }

        EntryAttribute uniqueMember = entry.get( UNIQUE_MEMBER_AT );

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
    private void addMembers( Set<String> memberSet, EntryAttribute members ) throws LdapException
    {
        for ( Value<?> value : members )
        {

            // get and normalize the Dn of the member
            String memberDn = value.getString();

            try
            {
                memberDn = parseNormalized( memberDn ).getNormName();
            }
            catch ( LdapException e )
            {
                LOG.warn( "Malformed member Dn in groupOf[Unique]Names entry.  Member not added to GroupCache.", e );
            }

            memberSet.add( memberDn );
        }
    }


    /**
     * Removes a set of member names from an existing set.
     *
     * @param memberSet the set of normalized member DNs
     * @param members the set of member values
     * @throws LdapException if there are problems accessing the attr values
     */
    private void removeMembers( Set<String> memberSet, EntryAttribute members ) throws LdapException
    {
        for ( Value<?> value : members )
        {
            // get and normalize the Dn of the member
            String memberDn = value.getString();

            try
            {
                memberDn = parseNormalized( memberDn ).getNormName();
            }
            catch ( LdapException e )
            {
                LOG.warn( "Malformed member Dn in groupOf[Unique]Names entry.  Member not removed from GroupCache.", e );
            }

            memberSet.remove( memberDn );
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
    public void groupAdded( Dn name, Entry entry ) throws LdapException
    {
        EntryAttribute members = getMemberAttribute( entry );

        if ( members == null )
        {
            return;
        }

        Set<String> memberSet = new HashSet<String>( members.size() );
        addMembers( memberSet, members );
        
        Element cacheElement = new Element( name.getNormName(), memberSet );
        ehCache.put( cacheElement );

        if ( IS_DEBUG )
        {
            LOG.debug( "group cache contents after adding '{}' :\n {}", name.getName(), ehCache.getAllWithLoader( ehCache.getKeys(), null ) );
        }
    }


    /**
     * Deletes a group's members from the cache.  Called by interceptor to account for
     * the deletion of groups.
     *
     * @param name the normalized Dn of the group entry
     * @param entry the attributes of entry being deleted
     */
    public void groupDeleted( Dn name, Entry entry ) throws LdapException
    {
        EntryAttribute members = getMemberAttribute( entry );

        if ( members == null )
        {
            return;
        }

        ehCache.remove( name.getNormName() );

        if ( IS_DEBUG )
        {
            LOG.debug( "group cache contents after deleting '{}' :\n {}", name.getName(), ehCache.getAllWithLoader( ehCache.getKeys(), null ) );
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
    private void modify( Set<String> memberSet, ModificationOperation modOp, EntryAttribute members )
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
     * @throws LdapException if there are problems accessing attribute  values
     */
    public void groupModified( Dn name, List<Modification> mods, Entry entry, SchemaManager schemaManager )
        throws LdapException
    {
        EntryAttribute members = null;
        String memberAttrId = null;
        EntryAttribute oc = entry.get( OBJECT_CLASS_AT );

        if ( oc.contains( SchemaConstants.GROUP_OF_NAMES_OC ) )
        {
            members = entry.get( MEMBER_AT );
            memberAttrId = SchemaConstants.MEMBER_AT;
        }

        if ( oc.contains( SchemaConstants.GROUP_OF_UNIQUE_NAMES_OC ) )
        {
            members = entry.get( UNIQUE_MEMBER_AT );
            memberAttrId = SchemaConstants.UNIQUE_MEMBER_AT;
        }

        if ( members == null )
        {
            return;
        }

        for ( Modification modification : mods )
        {
            if ( memberAttrId.equalsIgnoreCase( modification.getAttribute().getId() ) )
            {
                Element memSetElement = ehCache.get( name.getNormName() );
                
                if ( memSetElement != null )
                {
                    Set<String> memberSet = ( Set<String> ) memSetElement.getValue();
                    modify( memberSet, modification.getOperation(), modification.getAttribute() );
                }

                break;
            }
        }

        if ( IS_DEBUG )
        {
            LOG.debug( "group cache contents after modifying '{}' :\n {}", name.getName(), ehCache.getAllWithLoader( ehCache.getKeys(), null ) );
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
        EntryAttribute members = getMemberAttribute( mods );

        if ( members == null )
        {
            return;
        }

        Element memSetElement = ehCache.get( name.getNormName() );

        if ( memSetElement != null )
        {
            Set<String> memberSet = ( Set<String> ) memSetElement.getValue();
            modify( memberSet, modOp, members );
        }

        if ( IS_DEBUG )
        {
            LOG.debug( "group cache contents after modifying '{}' :\n {}", name.getName(), ehCache.getAllWithLoader( ehCache.getKeys(), null ) );
        }
    }


    /**
     * An optimization.  By having this method here we can directly access the group
     * membership information and lookup to see if the principalDn is contained within.
     *
     * @param principalDn the normalized Dn of the user to check if they are an admin
     * @return true if the principal is an admin or the admin
     */
    public final boolean isPrincipalAnAdministrator( Dn principalDn )
    {
        if ( principalDn.getNormName().equals( ServerDNConstants.ADMIN_SYSTEM_DN_NORMALIZED ) )
        {
            return true;
        }

        Element cacheElement = ehCache.get( administratorsGroupDn.getNormName() );
        
        if ( cacheElement == null )
        {
            LOG.warn( "What do you mean there is no administrators group? This is bad news." );
            return false;
        }
        else
        {
            Set<String> members = ( Set<String> ) cacheElement.getValue();            
            return members.contains( principalDn.getNormName() );
        }
    }


    /**
     * Gets the set of groups a user is a member of.  The groups are returned
     * as normalized Name objects within the set.
     *
     * @param member the member (user) to get the groups for
     * @return a Set of Name objects representing the groups
     * @throws LdapException if there are problems accessing attribute  values
     */
    public Set<Dn> getGroups( String member ) throws LdapException
    {
        Dn normMember;

        try
        {
            normMember = parseNormalized( member );
        }
        catch ( LdapException e )
        {
            LOG
                .warn(
                    "Malformed member Dn.  Could not find groups for member '{}' in GroupCache. Returning empty set for groups!",
                    member, e );
            return EMPTY_GROUPS;
        }

        Set<Dn> memberGroups = null;

        for ( Object obj : ehCache.getKeys() )
        {
            String group = ( String ) obj;
            Set<String> members = ( Set<String> ) ehCache.get( group ).getValue();

            if ( members == null )
            {
                continue;
            }

            if ( members.contains( normMember.getNormName() ) )
            {
                if ( memberGroups == null )
                {
                    memberGroups = new HashSet<Dn>();
                }

                memberGroups.add( parseNormalized( group ) );
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
        Element membersElement = ehCache.get( oldName.getNormName() );
        
        if ( membersElement != null )
        {
            Set<String> members = ( Set<String> ) membersElement.getValue();
            
            ehCache.remove( oldName.getNormName() );
            
            Element cacheElement = new Element( newName.getNormName(), members );
            ehCache.put( cacheElement );

            if ( IS_DEBUG )
            {
                LOG.debug( "group cache contents after renaming '{}' :\n{}", oldName.getName(), ehCache.getAllWithLoader( ehCache.getKeys(), null ) );
            }

            return true;
        }

        return false;
    }
}
