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


import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.shared.ldap.filter.AssertionEnum;
import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.SimpleNode;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.*;
import java.util.*;


/**
 * A cache for tracking static group membership.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class GroupCache
{
    /** the attribute id for an object class: objectClass */
    private static final String OC_ATTR = "objectClass";
    /** the member attribute for a groupOfNames: member */
    private static final String MEMBER_ATTR = "member";
    /** the member attribute for a groupOfUniqueNames: uniqueMember */
    private static final String UNIQUEMEMBER_ATTR = "uniqueMember";
    /** the groupOfNames objectClass: groupOfNames */
    private static final String GROUPOFNAMES_OC = "groupOfNames";
    /** the groupOfUniqueNames objectClass: groupOfUniqueNames */
    private static final String GROUPOFUNIQUENAMES_OC = "groupOfUniqueNames";
    /** the logger for this class */
    private static final Logger log = LoggerFactory.getLogger( GroupCache.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /** String key for the DN of a group to a Set (HashSet) for the Strings of member DNs */
    private final Map groups = new HashMap();
    /** a handle on the partition nexus */
    private final PartitionNexus nexus;
    /** the env to use for searching */
    private final Hashtable env;

    /**
     * The OIDs normalizer map
     */
    private Map normalizerMap;
    
    /** the normalized dn of the administrators group */
    LdapDN administratorsGroupDn;
    
    /**
     * Creates a static group cache.
     *
     * @param factoryCfg the context factory configuration for the server
     */
    public GroupCache( DirectoryServiceConfiguration factoryCfg ) throws NamingException
    {
    	normalizerMap = factoryCfg.getGlobalRegistries().getAttributeTypeRegistry().getNormalizerMapping();
        this.nexus = factoryCfg.getPartitionNexus();
        this.env = ( Hashtable ) factoryCfg.getEnvironment().clone();
        
        // stuff for dealing with the admin group
        administratorsGroupDn = new LdapDN( "cn=Administrators,ou=groups,ou=system" );
        administratorsGroupDn.normalize( normalizerMap );

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

        BranchNode filter = new BranchNode( AssertionEnum.OR );
        filter.addNode( new SimpleNode( OC_ATTR, GROUPOFNAMES_OC, AssertionEnum.EQUALITY ) );
        filter.addNode( new SimpleNode( OC_ATTR, GROUPOFUNIQUENAMES_OC, AssertionEnum.EQUALITY ) );

        Iterator suffixes = nexus.listSuffixes();
        while ( suffixes.hasNext() )
        {
            String suffix = ( String ) suffixes.next();
            LdapDN baseDn = new LdapDN( suffix );
            SearchControls ctls = new SearchControls();
            ctls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            NamingEnumeration results = nexus.search( baseDn, env, filter, ctls );

            while ( results.hasMore() )
            {
                SearchResult result = ( SearchResult ) results.next();
                String groupDn = result.getName();
                groupDn = parseNormalized( groupDn ).toString();
                Attribute members = getMemberAttribute( result.getAttributes() );

                if ( members != null )
                {
                    Set memberSet = new HashSet( members.size() );
                    addMembers( memberSet, members );
                    groups.put( groupDn, memberSet );
                }
                else
                {
                    log.warn( "Found group '" + groupDn + "' without any member or uniqueMember attributes" );
                }
            }
            results.close();
        }

        if ( IS_DEBUG )
        {
            log.debug( "group cache contents on startup:\n" + groups );
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
        Attribute oc = entry.get( OC_ATTR );

        if ( oc == null )
        {
            if ( entry.get( MEMBER_ATTR ) != null )
            {
                return entry.get( MEMBER_ATTR );
            }

            if ( entry.get( UNIQUEMEMBER_ATTR ) != null )
            {
                return entry.get( UNIQUEMEMBER_ATTR );
            }

            return null;
        }

        if ( oc.contains( GROUPOFNAMES_OC ) )
        {
            return entry.get( MEMBER_ATTR );
        }

        if ( oc.contains( GROUPOFUNIQUENAMES_OC ) )
        {
            return entry.get( UNIQUEMEMBER_ATTR );
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
    private void addMembers( Set memberSet, Attribute members ) throws NamingException
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
    private void removeMembers( Set memberSet, Attribute members ) throws NamingException
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
    public void groupAdded( String upName, Name normName, Attributes entry ) throws NamingException
    {
        Attribute members = getMemberAttribute( entry );

        if ( members == null )
        {
            return;
        }

        Set memberSet = new HashSet( members.size() );
        addMembers( memberSet, members );
        groups.put( normName.toString(), memberSet );
        
        if ( IS_DEBUG )
        {
            log.debug( "group cache contents after adding " + normName.toString() + ":\n" + groups );
        }
    }


    /**
     * Deletes a group's members from the cache.  Called by interceptor to account for
     * the deletion of groups.
     *
     * @param name the normalized DN of the group entry
     * @param entry the attributes of entry being deleted
     */
    public void groupDeleted( Name name, Attributes entry )
    {
        Attribute members = getMemberAttribute( entry );

        if ( members == null )
        {
            return;
        }

        groups.remove( name.toString() );
        
        if ( IS_DEBUG )
        {
            log.debug( "group cache contents after deleting " + name.toString() + ":\n" + groups );
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
    private void modify( Set memberSet, int modOp, Attribute members ) throws NamingException
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
    public void groupModified( Name name, ModificationItem[] mods, Attributes entry ) throws NamingException
    {
        Attribute members = null;
        String memberAttrId = null;
        Attribute oc = entry.get( OC_ATTR );

        if ( oc.contains( GROUPOFNAMES_OC ) )
        {
            members = entry.get( MEMBER_ATTR );
            memberAttrId = MEMBER_ATTR;
        }

        if ( oc.contains( GROUPOFUNIQUENAMES_OC ) )
        {
            members = entry.get( UNIQUEMEMBER_ATTR );
            memberAttrId = UNIQUEMEMBER_ATTR;
        }

        if ( members == null )
        {
            return;
        }

        for ( int ii = 0; ii < mods.length; ii++ )
        {
            if ( memberAttrId.equalsIgnoreCase( mods[ii].getAttribute().getID() ) )
            {
                Set memberSet = ( Set ) groups.get( name.toString() );
                if ( memberSet != null )
                {
                    modify( memberSet, mods[ii].getModificationOp(), mods[ii].getAttribute() );
                }
                break;
            }
        }
        
        if ( IS_DEBUG )
        {
            log.debug( "group cache contents after modifying " + name.toString() + ":\n" + groups );
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
    public void groupModified( Name name, int modOp, Attributes mods, Attributes entry ) throws NamingException
    {
        Attribute members = getMemberAttribute( mods );

        if ( members == null )
        {
            return;
        }

        Set memberSet = ( Set ) groups.get( name.toString() );
        if ( memberSet != null )
        {
            modify( memberSet, modOp, members );
        }
        
        if ( IS_DEBUG )
        {
            log.debug( "group cache contents after modifying " + name.toString() + ":\n" + groups );
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
        if ( principalDn.toNormName().equals( PartitionNexus.ADMIN_PRINCIPAL_NORMALIZED ) )
        {
            return true;
        }
        
        Set members = ( Set ) groups.get( administratorsGroupDn.toNormName() );
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
    public Set getGroups( String member ) throws NamingException
    {
        try
        {
            member = parseNormalized( member ).toString();
        }
        catch ( NamingException e )
        {
            log
                .warn(
                    "Malformed member DN.  Could not find groups for member in GroupCache. Returning empty set for groups!",
                    e );
            return Collections.EMPTY_SET;
        }

        Set memberGroups = null;

        Iterator list = groups.keySet().iterator();
        while ( list.hasNext() )
        {
            String group = ( String ) list.next();
            Set members = ( Set ) groups.get( group );

            if ( members == null )
            {
                continue;
            }

            if ( members.contains( member ) )
            {
                if ( memberGroups == null )
                {
                    memberGroups = new HashSet();
                }

                memberGroups.add( new LdapDN( group ) );
            }
        }

        if ( memberGroups == null )
        {
            return Collections.EMPTY_SET;
        }

        return memberGroups;
    }


    public boolean groupRenamed( Name oldName, Name newName )
    {
        Object members = groups.remove( oldName.toString() );

        if ( members != null )
        {
            groups.put( newName.toString(), members );
            
            if ( IS_DEBUG )
            {
                log.debug( "group cache contents after renaming " + oldName.toString() + ":\n" + groups );
            }
            return true;
        }
        return false;
    }
}
