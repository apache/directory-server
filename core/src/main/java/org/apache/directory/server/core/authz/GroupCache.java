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


import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.constants.ServerDNConstants;
import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.filter.OrNode;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.OidNormalizer;
import org.apache.directory.shared.ldap.util.AttributeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;


/**
 * A cache for tracking static group membership.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class GroupCache
{
    /** the logger for this class */
    private static final Logger log = LoggerFactory.getLogger( GroupCache.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /** String key for the DN of a group to a Set (HashSet) for the Strings of member DNs */
    private final Map<String, Set<String>> groups = new HashMap<String, Set<String>>();
    
    /** a handle on the partition nexus */
    private final PartitionNexus nexus;
    
    /** the env to use for searching */
    private final Map<?, ?> env;

    /** Stores a reference to the AttributeType registry */ 
    private AttributeTypeRegistry attributeTypeRegistry;
    
    /** A storage for the member attributeType */
    private AttributeType memberAT;

    /** A storage for the uniqueMember attributeType */
    private AttributeType uniqueMemberAT;

    /**
     * The OIDs normalizer map
     */
    private Map<String, OidNormalizer> normalizerMap;
    
    /** the normalized dn of the administrators group */
    private LdapDN administratorsGroupDn;
    
    private static final Set<Name> EMPTY_GROUPS = new HashSet<Name>();
    
    /**
     * Creates a static group cache.
     *
     * @param factoryCfg the context factory configuration for the server
     */
    public GroupCache( DirectoryServiceConfiguration factoryCfg ) throws NamingException
    {
    	normalizerMap = factoryCfg.getRegistries().getAttributeTypeRegistry().getNormalizerMapping();
        nexus = factoryCfg.getPartitionNexus();
        env = ( Map<?, ?> ) factoryCfg.getEnvironment().clone();
        attributeTypeRegistry = factoryCfg.getRegistries().getAttributeTypeRegistry();
        
        memberAT = attributeTypeRegistry.lookup( SchemaConstants.MEMBER_AT_OID ); 
        uniqueMemberAT = attributeTypeRegistry.lookup( SchemaConstants.UNIQUE_MEMBER_AT_OID );

        // stuff for dealing with the admin group
        administratorsGroupDn = parseNormalized( ServerDNConstants.ADMINISTRATORS_GROUP_DN );

        initialize();
    }


    private LdapDN parseNormalized( String name ) throws NamingException
    {
        LdapDN dn = new LdapDN( name );
        dn.normalize( normalizerMap );
        return dn;
    }


    private void initialize() throws NamingException
    {
        // search all naming contexts for static groups and generate
        // normalized sets of members to cache within the map

        BranchNode filter = new OrNode();
        filter.addNode( new EqualityNode( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.GROUP_OF_NAMES_OC ) );
        filter.addNode( new EqualityNode( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.GROUP_OF_UNIQUE_NAMES_OC) );

        Iterator<String> suffixes = nexus.listSuffixes( null );
        
        while ( suffixes.hasNext() )
        {
            String suffix = suffixes.next();
            LdapDN baseDn = new LdapDN( suffix );
            SearchControls ctls = new SearchControls();
            ctls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            NamingEnumeration<SearchResult> results = 
                nexus.search( new SearchOperationContext( baseDn, env, filter, ctls ) );

            while ( results.hasMore() )
            {
                SearchResult result = results.next();
                LdapDN groupDn = parseNormalized( result.getName() );
                Attribute members = getMemberAttribute( result.getAttributes() );

                if ( members != null )
                {
                    Set<String> memberSet = new HashSet<String>( members.size() );
                    addMembers( memberSet, members );
                    groups.put( groupDn.getNormName(), memberSet );
                }
                else
                {
                    log.warn( "Found group '{}' without any member or uniqueMember attributes", groupDn.getUpName() );
                }
            }
            
            results.close();
        }

        if ( IS_DEBUG )
        {
            log.debug( "group cache contents on startup:\n {}", groups );
        }
    }


    /**
     * Gets the member attribute regardless of whether groupOfNames or
     * groupOfUniqueNames is used.
     *
     * @param entry the entry inspected for member attributes
     * @return the member attribute
     */
    private Attribute getMemberAttribute( Attributes entry )
    {
        Attribute oc = entry.get( SchemaConstants.OBJECT_CLASS_AT );

        if ( oc == null )
        {
        	Attribute member = AttributeUtils.getAttribute( entry, memberAT );
        	
            if ( member != null )
            {
                return member;
            }

            Attribute uniqueMember = AttributeUtils.getAttribute(entry, uniqueMemberAT );
            
            if ( uniqueMember != null )
            {
                return uniqueMember;
            }

            return null;
        }

        if ( AttributeUtils.containsValueCaseIgnore( oc, SchemaConstants.GROUP_OF_NAMES_OC ) ||
        		AttributeUtils.containsValueCaseIgnore( oc, SchemaConstants.GROUP_OF_NAMES_OC_OID )	)
        {
            return AttributeUtils.getAttribute( entry, memberAT );
        }

        if ( AttributeUtils.containsValueCaseIgnore( oc, SchemaConstants.GROUP_OF_UNIQUE_NAMES_OC ) || 
        		AttributeUtils.containsValueCaseIgnore( oc, SchemaConstants.GROUP_OF_UNIQUE_NAMES_OC_OID ))
        {
            return AttributeUtils.getAttribute(entry, uniqueMemberAT );
        }

        return null;
    }


    /**
     * Adds normalized member DNs to the set of normalized member names.
     *
     * @param memberSet the set of member Dns (Strings)
     * @param members the member attribute values being added
     * @throws NamingException if there are problems accessing the attr values
     */
    private void addMembers( Set<String> memberSet, Attribute members ) throws NamingException
    {
        for ( int ii = 0; ii < members.size(); ii++ )
        {
            // get and normalize the DN of the member
            String memberDn = ( String ) members.get( ii );

            try
            {
                memberDn = parseNormalized( memberDn ).toString();
            }
            catch ( NamingException e )
            {
                log.warn( "Malformed member DN in groupOf[Unique]Names entry.  Member not added to GroupCache.", e );
            }

            memberSet.add( memberDn );
        }
    }


    /**
     * Removes a set of member names from an existing set.
     *
     * @param memberSet the set of normalized member DNs
     * @param members the set of member values
     * @throws NamingException if there are problems accessing the attr values
     */
    private void removeMembers( Set<String> memberSet, Attribute members ) throws NamingException
    {
        for ( int ii = 0; ii < members.size(); ii++ )
        {
            // get and normalize the DN of the member
            String memberDn = ( String ) members.get( ii );

            try
            {
                memberDn = parseNormalized( memberDn ).toString();
            }
            catch ( NamingException e )
            {
                log.warn( "Malformed member DN in groupOf[Unique]Names entry.  Member not removed from GroupCache.", e );
            }

            memberSet.remove( memberDn );
        }
    }


    /**
     * Adds a groups members to the cache.  Called by interceptor to account for new
     * group additions.
     *
     * @param upName the user provided name for the group entry
     * @param normName the normalized name for the group entry
     * @param entry the group entry's attributes
     * @throws NamingException if there are problems accessing the attr values
     */
    public void groupAdded( LdapDN name, Attributes entry ) throws NamingException
    {
        Attribute members = getMemberAttribute( entry );

        if ( members == null )
        {
            return;
        }

        Set<String> memberSet = new HashSet<String>( members.size() );
        addMembers( memberSet, members );
        groups.put( name.getNormName(), memberSet );
        
        if ( IS_DEBUG )
        {
            log.debug( "group cache contents after adding '{}' :\n {}", name.getUpName(), groups );
        }
    }


    /**
     * Deletes a group's members from the cache.  Called by interceptor to account for
     * the deletion of groups.
     *
     * @param name the normalized DN of the group entry
     * @param entry the attributes of entry being deleted
     */
    public void groupDeleted( LdapDN name, Attributes entry )
    {
        Attribute members = getMemberAttribute( entry );

        if ( members == null )
        {
            return;
        }

        groups.remove( name.getNormName() );
        
        if ( IS_DEBUG )
        {
            log.debug( "group cache contents after deleting '{}' :\n {}", name.getUpName(), groups );
        }
    }


    /**
     * Utility method to modify a set of member names based on a modify operation
     * that changes the members of a group.
     *
     * @param memberSet the set of members to be altered
     * @param modOp the type of modify operation being performed
     * @param members the members being added, removed or replaced
     * @throws NamingException if there are problems accessing attribute values
     */
    private void modify( Set<String> memberSet, int modOp, Attribute members ) throws NamingException
    {

        switch ( modOp )
        {
            case ( DirContext.ADD_ATTRIBUTE  ):
                addMembers( memberSet, members );
                break;
                
            case ( DirContext.REPLACE_ATTRIBUTE  ):
                if ( members.size() > 0 )
                {
                    memberSet.clear();
                    addMembers( memberSet, members );
                }
            
                break;
                
            case ( DirContext.REMOVE_ATTRIBUTE  ):
                removeMembers( memberSet, members );
                break;
                
            default:
                throw new InternalError( "Undefined modify operation value of " + modOp );
        }
    }


    /**
     * Modifies the cache to reflect changes via modify operations to the group entries.
     * Called by the interceptor to account for modify ops on groups.
     *
     * @param name the normalized name of the group entry modified
     * @param mods the modification operations being performed
     * @param entry the group entry being modified
     * @throws NamingException if there are problems accessing attribute  values
     */
    public void groupModified( LdapDN name, List<ModificationItem> mods, Attributes entry ) throws NamingException
    {
        Attribute members = null;
        String memberAttrId = null;
        Attribute oc = entry.get( SchemaConstants.OBJECT_CLASS_AT );

        if ( AttributeUtils.containsValueCaseIgnore( oc, SchemaConstants.GROUP_OF_NAMES_OC ) ||
        		AttributeUtils.containsValueCaseIgnore( oc, SchemaConstants.GROUP_OF_NAMES_OC_OID ))
        {
            members = AttributeUtils.getAttribute( entry, memberAT );
            memberAttrId = SchemaConstants.MEMBER_AT;
        }

        if ( AttributeUtils.containsValueCaseIgnore( oc, SchemaConstants.GROUP_OF_UNIQUE_NAMES_OC ) ||
        		AttributeUtils.containsValueCaseIgnore( oc, SchemaConstants.GROUP_OF_UNIQUE_NAMES_OC_OID ) )
        {
            members = AttributeUtils.getAttribute(entry, uniqueMemberAT );
            memberAttrId = SchemaConstants.UNIQUE_MEMBER_AT;
        }

        if ( members == null )
        {
            return;
        }

        for ( ModificationItem modification:mods )
        {
            if ( memberAttrId.equalsIgnoreCase( modification.getAttribute().getID() ) )
            {
                Set<String> memberSet = groups.get( name.getNormName() );
                
                if ( memberSet != null )
                {
                    modify( memberSet, modification.getModificationOp(), modification.getAttribute() );
                }
                
                break;
            }
        }
        
        if ( IS_DEBUG )
        {
            log.debug( "group cache contents after modifying '{}' :\n {}", name.getUpName(), groups );
        }
    }


    /**
     * Modifies the cache to reflect changes via modify operations to the group entries.
     * Called by the interceptor to account for modify ops on groups.
     *
     * @param name the normalized name of the group entry modified
     * @param modOp the modify operation being performed
     * @param mods the modifications being performed
     * @param entry the entry being modified
     * @throws NamingException if there are problems accessing attribute  values
     */
    public void groupModified( LdapDN name, int modOp, Attributes mods, Attributes entry ) throws NamingException
    {
        Attribute members = getMemberAttribute( mods );

        if ( members == null )
        {
            return;
        }

        Set<String> memberSet = groups.get( name.getNormName() );
        
        if ( memberSet != null )
        {
            modify( memberSet, modOp, members );
        }
        
        if ( IS_DEBUG )
        {
            log.debug( "group cache contents after modifying '{}' :\n {}", name.getUpName(), groups );
        }
    }

    
    /**
     * An optimization.  By having this method here we can directly access the group
     * membership information and lookup to see if the principalDn is contained within.
     * 
     * @param principalDn the normalized DN of the user to check if they are an admin
     * @return true if the principal is an admin or the admin
     */
    public final boolean isPrincipalAnAdministrator( LdapDN principalDn )
    {
        if ( principalDn.getNormName().equals( PartitionNexus.ADMIN_PRINCIPAL_NORMALIZED ) )
        {
            return true;
        }
        
        Set<String> members = groups.get( administratorsGroupDn.getNormName() );
        
        if ( members == null )
        {
            log.warn( "What do you mean there is no administrators group? This is bad news." );
            return false;
        }
        
        return members.contains( principalDn.toNormName() );
    }
    

    /**
     * Gets the set of groups a user is a member of.  The groups are returned
     * as normalized Name objects within the set.
     *
     * @param member the member (user) to get the groups for
     * @return a Set of Name objects representing the groups
     * @throws NamingException if there are problems accessing attribute  values
     */
    public Set<Name> getGroups( String member ) throws NamingException
    {
    	LdapDN normMember = null;
    	
        try
        {
        	normMember = parseNormalized( member );
        }
        catch ( NamingException e )
        {
            log.warn( "Malformed member DN.  Could not find groups for member '{}' in GroupCache. Returning empty set for groups!", member, e );
            return EMPTY_GROUPS;
        }

        Set<Name> memberGroups = null;

        for ( String group:groups.keySet() )
        {
            Set<String> members = groups.get( group );

            if ( members == null )
            {
                continue;
            }

            if ( members.contains( normMember.getNormName() ) )
            {
                if ( memberGroups == null )
                {
                    memberGroups = new HashSet<Name>();
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


    public boolean groupRenamed( LdapDN oldName, LdapDN newName )
    {
        Set<String> members = groups.remove( oldName.getNormName() );

        if ( members != null )
        {
            groups.put( newName.getNormName(), members );
            
            if ( IS_DEBUG )
            {
                log.debug( "group cache contents after renaming '{}' :\n{}", oldName.getUpName(), groups );
            }
            
            return true;
        }
        
        return false;
    }
}
