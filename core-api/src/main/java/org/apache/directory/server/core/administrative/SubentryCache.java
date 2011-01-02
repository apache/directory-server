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


import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * A cache for subtree specifications. It associates a Subentry with its entryUUID<br>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SubentryCache implements Iterable<String>
{
    /** The Subentry cache */
    private final Map<String, Subentry> cache;
    
    /**
     * Creates a new instance of SubentryCache with a default maximum size.
     */
    public SubentryCache()
    {
        cache = new ConcurrentHashMap<String, Subentry>();
    }
    
    
    /**
     * Creates a new instance of SubentryCache with a specific maximum size.
     */
    public SubentryCache( int maxSize )
    {
        cache = new ConcurrentHashMap<String, Subentry>();
    }
    
    
    /**
     * Retrieve a Subentry given an UUID. If there is none, null will be returned.
     *
     * @param uuid The UUID we want to get the Subentry for 
     * @return The found Subentry, or null
     */
    public final Subentry getSubentry( String uuid )
    {
        return cache.get( uuid );
    }
    
    
    /**
     * Remove a Subentry for a given UUID 
     *
     * @param uuid The UUID for which we want to remove the 
     * associated Subentry
     * @return The removed Subentry, if any
     */
    public final Subentry removeSubentry( String uuid )
    {
        Subentry oldSubentry = cache.remove( uuid );
        
        return oldSubentry;
    }
    
    
    /**
     * Stores a new Subentry into the cache, associated with a DN
     *
     * @param subentry The Subentry
     * @return The old Subentry, if any
     */
    public Subentry addSubentry( Subentry subentry )
    {
        Subentry oldSubentry = cache.put( subentry.getUuid(), subentry );
        
        return oldSubentry;
    }
    
    
    /**
     * Tells if there is a Subentry associated with an UUID
     * @param uuid The UUID
     * @return True if a Subentry is found
     */
    public boolean hasSubentry( String uuid )
    {
        return cache.containsKey( uuid );
    }
    
    
    /**
     * @return An Iterator over the Subentry's UUIDs 
     */
    public Iterator<String> iterator()
    {
        return cache.keySet().iterator();
    }
}
