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
package org.apache.directory.server.core.subtree;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecification;


/**
 * A cache for subtree specifications. It associates a Subentry with a DN,
 * representing its position in the DIT.<br>
 * This cache has a size limit set to 1000 at the moment. We should add a configuration
 * parameter to manage its size.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SubentryCache
{
    /** The default cache size limit */
    private static final int DEFAULT_CACHE_MAX_SIZE = 1000;
    
    /** The cache size limit */
    private int cacheMaxSize = DEFAULT_CACHE_MAX_SIZE;
    
    /** The current cache size */
    private AtomicInteger cacheSize;
    
    /** The Subentry cache */
    private final Map<String, Subentry> cache;
    
    /**
     * Creates a new instance of SubentryCache with a default maximum size.
     */
    public SubentryCache()
    {
        cache = new HashMap<String, Subentry>();
        cacheSize = new AtomicInteger( 0 );
    }
    
    
    /**
     * Creates a new instance of SubentryCache with a specific maximum size.
     */
    public SubentryCache( int maxSize )
    {
        cache = new HashMap<String, Subentry>();
        cacheSize = new AtomicInteger( 0 );
        cacheMaxSize = maxSize;
    }
    
    
    /**
     * Retrieve a Subentry given a AP DN. If there is none, null will be returned.
     *
     * @param apDn The AdministrativePoint we want to get the Subentry for 
     * @return The found Subentry, or null
     */
    final Subentry getSubentry( DN apDn )
    {
        return cache.get( apDn.getNormName() );
    }
    
    
    /**
     * Remove a Subentry for a given AdministrativePoint 
     *
     * @param apDn The administrativePoint for which we want to remove the 
     * associated Subentry
     * @return The removed Subentry, if any
     */
    final Subentry removeSubentry( DN apDn )
    {
        return  cache.remove( apDn.getNormName() );
    }
    
    
    /**
     * Stores a new Subentry into the cache, associated with an AdministrativePoint
     *
     * @param apDn The administrativePoint DN
     * @param ss The SubtreeSpecification
     * @param adminRoles The administrative roles for this Subentry
     * @return The old Subentry, if any
     */
    final Subentry addSubentry( DN apDn, SubtreeSpecification ss, Set<AdministrativeRole> adminRoles )
    {
        Subentry oldSubentry = cache.get( apDn.getNormName() );
        Subentry subentry = new Subentry();
        subentry.setSubtreeSpecification( ss );
        subentry.setAdministrativeRoles( adminRoles );
        cache.put( apDn.getNormName(), subentry );
        
        return oldSubentry;
    }
    
    
    /**
     * Tells if there is a Subentry associated with an administrativePoint
     * @param apDn The administrativePoint DN
     * @return True if a Subentry is found
     */
    final boolean hasSubentry( DN apDn )
    {
        return cache.containsKey( apDn.getNormName() );
    }
    
    
    /**
     * @return An Iterator over the AdministartivePoints normalized DNs 
     */
    final Iterator<String> nameIterator()
    {
        return cache.keySet().iterator();
    }
}
