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
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
    
    /** A lock to guarantee the Subentry cache consistency */
    private ReentrantReadWriteLock mutex = new ReentrantReadWriteLock();

    /**
     * Get a read-lock on the Subentry cache.
     * No read operation can be done on the AP cache if this
     * method is not called before.
     */
    public void lockRead()
    {
        mutex.readLock().lock();
    }


    /**
     * Get a write-lock on the Subentry cache.
     * No write operation can be done on the apCache if this
     * method is not called before.
     */
    public void lockWrite()
    {
        mutex.writeLock().lock();
    }


    /**
     * Release the read-write lock on the AP cache.
     * This method must be called after having read or modified the
     * AP cache
     */
    public void unlock()
    {
        if ( mutex.isWriteLockedByCurrentThread() )
        {
            mutex.writeLock().unlock();
        }
        else
        {
            mutex.readLock().unlock();
        }
    }

    
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
        try
        {
            lockRead();
            Subentry[] subentries = uuidCache.get( uuid );
            Subentry subentry = subentries[role.getValue()];
            
            return subentry;
        }
        finally 
        {
            unlock();
        }
    }
    
    
    /**
     * Retrieve the Subentries for given an UUID
     *
     * @param uuid The UUID we want to get the Subentries for 
     * @return The found Subentries, or null
     */
    public final Subentry[] getSubentries( String uuid )
    {
        try
        {
            lockRead();
            Subentry[] subentries = uuidCache.get( uuid );
            
            return subentries;
        }
        finally
        {
            unlock();
        }
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
        try
        {
            lockRead();
            Subentry[] subentries = dnCache.getElement( dn );
            Subentry subentry = subentries[role.getValue()];
            
            return subentry;
        }
        finally
        {
            unlock();
        }
    }
    
    
    /**
     * Retrieve the Subentries for given a DN
     *
     * @param dn The DN we want to get the Subentries for 
     * @return The found Subentries, or null
     */
    public final Subentry[] getSubentries( DN dn )
    {
        try
        {
            lockRead();
            Subentry[] subentries = dnCache.getElement( dn );
            
            return subentries;
        }
        finally
        {
            unlock();
        }
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
        try
        {
            lockWrite();
            Subentry[] oldSubentry = uuidCache.remove( uuid );
            
            return oldSubentry;
        }
        finally
        {
            unlock();
        }
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
        try
        {
            lockWrite();
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
        finally
        {
            unlock();
        }
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
        try
        {
            lockWrite();
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
        finally
        {
            unlock();
        }
    }
    
    
    /**
     * Tells if there is a Subentry associated with an UUID
     * @param uuid The UUID
     * @return True if a Subentry is found
     */
    public boolean hasSubentry( String uuid )
    {
        try
        {
            lockRead();
            return uuidCache.containsKey( uuid );
        }
        finally
        {
            unlock();
        }
    }
    
    
    /**
     * Tells if there is a Subentry associated with a DN
     * @param dn The DN
     * @return True if a Subentry is found
     */
    public boolean hasSubentry( DN dn )
    {
        try
        {
            lockRead();
            return dnCache.hasElement( dn );
        }
        finally
        {
            unlock();
        }
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
        try
        {
            lockRead();
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
        finally
        {
            unlock();
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
        try
        {
            lockRead();
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
        finally
        {
            unlock();
        }
    }
    
    
    /**
     * @return An Iterator over the Subentry's UUIDs 
     */
    public Iterator<String> iterator()
    {
        try
        {
            lockRead();
            return uuidCache.keySet().iterator();
        }
        finally
        {
            unlock();
        }
    }
    
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( "Subentry cache :\n" );
        
        lockRead();
        sb.append( "  DN cache :\n" ).append( dnCache ).append( "\n" );
        
        sb.append( "  UUID cache :\n" );
        
        for ( String uuid : uuidCache.keySet() )
        {
            sb.append( uuid ).append( " -> " );
            
            for ( Subentry subentry : uuidCache.get( uuid ) )
            {
                if ( subentry != null )
                {
                    sb.append( subentry );
                }
            }
        }
        
        unlock();
        
        return sb.toString();
    }
}
