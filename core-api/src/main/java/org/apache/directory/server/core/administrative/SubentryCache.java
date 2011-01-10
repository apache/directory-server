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
package org.apache.directory.server.core.administrative;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.util.tree.DnNode;


/**
 * A cache for subentries. It associates a set of Subentry with its entryUUID or
 * with its DN<br>
 * This cache is thread safe.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SubentryCache implements Iterable<String>
{
    /** The Subentry UUID cache */
    private final Map<String, Subentry[]> uuidCache;
    
    /** The Subentry DN cache */
    private final DnNode<Subentry[]> dnCache;
    
    /**
     * Creates a new instance of SubentryCache with a default maximum size.
     */
    public SubentryCache()
    {
        uuidCache = new HashMap<String, Subentry[]>();
        dnCache = new DnNode<Subentry[]>();
    }
    
    
    /**
     * Retrieve a Subentry given an UUID and its role. If there is none, null will be returned.
     *
     * @param uuid The UUID we want to get the Subentry for 
     * @param role The subentry Role
     * @return The found Subentry, or null
     */
    public final Subentry getSubentry( String uuid, AdministrativeRoleEnum role )
    {
        Subentry[] subentries = uuidCache.get( uuid );
        Subentry subentry = subentries[role.getValue()];
        
        return subentry;
    }
    
    
    /**
     * Retrieve the Subentries for given an UUID
     *
     * @param uuid The UUID we want to get the Subentries for 
     * @return The found Subentries, or null
     */
    public final Subentry[] getSubentries( String uuid )
    {
        Subentry[] subentries = uuidCache.get( uuid );
        
        return subentries;
    }
    
    
    /**
     * Retrieve a Subentry given an DN and its role. If there is none, null will be returned.
     *
     * @param dn The DN we want to get the Subentry for 
     * @param role The subentry Role
     * @return The found Subentry, or null
     */
    public final Subentry getSubentry( DN dn, AdministrativeRoleEnum role )
    {
        Subentry[] subentries = dnCache.getElement( dn );
        Subentry subentry = subentries[role.getValue()];
        
        return subentry;
    }
    
    
    /**
     * Retrieve the Subentries for given a DN
     *
     * @param dn The DN we want to get the Subentries for 
     * @return The found Subentries, or null
     */
    public final Subentry[] getSubentries( DN dn )
    {
        Subentry[] subentries = dnCache.getElement( dn );
        
        return subentries;
    }
    
    
    /**
     * Remove the Subentries for a given UUID and role
     *
     * @param uuid The UUID for which we want to remove the 
     * associated Subentries
     * @return The removed Subentries, if any
     */
    public final Subentry[] removeSubentry( String uuid )
    {
        Subentry[] oldSubentry = uuidCache.remove( uuid );
        
        return oldSubentry;
    }
    
    
    /**
     * Remove the Subentries for a given UUID and role
     *
     * @param uuid The DN for which we want to remove the 
     * associated Subentries
     * @return The removed Subentries, if any
     */
    public final Subentry[] removeSubentry( DN dn ) throws LdapException
    {
        Subentry[] oldSubentry = dnCache.getElement( dn );
        dnCache.remove( dn );
        
        // Update the UUID cache
        if ( oldSubentry != null )
        { 
            for ( Subentry subentry : oldSubentry )
            {
                if ( subentry != null )
                {
                    uuidCache.remove( subentry.getUuid() );
                }
            }
        }
        
        return oldSubentry;
    }
    
    
    /**
     * Stores a new Subentry into the cache, associated with a DN
     *
     * @param dn, the subentry DN
     * @param subentry The Subentry
     * @return The old Subentries, if any
     */
    public Subentry[] addSubentry( DN dn, Subentry subentry ) throws LdapException
    {
        Subentry[] subentries = uuidCache.get( subentry.getUuid() );
        
        if ( subentries == null )
        {
            subentries = new Subentry[4];
        }
        
        subentries[subentry.getAdministrativeRole().getValue()] = subentry;
        Subentry[] oldSubentry = uuidCache.put( subentry.getUuid(), subentries );
        
        Subentry[] dnSubentries = dnCache.getElement( dn );
        
        if ( dnSubentries != null )
        {
            dnSubentries[subentry.getAdministrativeRole().getValue()] = subentry;
        }
        else
        {
            dnCache.add( dn, subentries );
        }
        
        return oldSubentry;
    }
    
    
    /**
     * Tells if there is a Subentry associated with an UUID
     * @param uuid The UUID
     * @return True if a Subentry is found
     */
    public boolean hasSubentry( String uuid )
    {
        return uuidCache.containsKey( uuid );
    }
    
    
    /**
     * Tells if there is a Subentry associated with a DN
     * @param dn The DN
     * @return True if a Subentry is found
     */
    public boolean hasSubentry( DN dn )
    {
        return dnCache.hasElement( dn );
    }
    
    
    /**
     * Tells if there is a Subentry associated with an UUID and a role
     * 
     * @param uuid The UUID
     * @param role The role
     * @return True if a Subentry is found
     */
    public boolean hasSubentry( String uuid, AdministrativeRoleEnum role )
    {
        Subentry[] subentries = uuidCache.get( uuid );
        
        if ( subentries == null )
        {
            return false;
        }
        else
        {
            return subentries[ role.getValue() ] != null;
        }
    }
    
    
    /**
     * Tells if there is a Subentry associated with a DN and a role
     * @param dn The DN
     * @param role The role
     * @return True if a Subentry is found
     */
    public boolean hasSubentry( DN dn, AdministrativeRoleEnum role )
    {
        Subentry[] subentries = dnCache.getElement( dn );
        
        if ( subentries == null )
        {
            return false;
        }
        else
        {
            return subentries[ role.getValue() ] != null;
        }
    }
    
    
    /**
     * @return An Iterator over the Subentry's UUIDs 
     */
    public Iterator<String> iterator()
    {
        return uuidCache.keySet().iterator();
    }
}
