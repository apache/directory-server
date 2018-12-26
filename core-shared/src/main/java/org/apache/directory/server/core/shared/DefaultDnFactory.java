/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.server.core.shared;


import org.ehcache.Cache;

import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.core.api.DnFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The default Dn factory implementation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DefaultDnFactory implements DnFactory
{
    private static final Logger LOG = LoggerFactory.getLogger( DefaultDnFactory.class );

    /** The cache for DNs */
    private Cache<String, Dn> dnCache;

    /** The schema manager */
    private SchemaManager schemaManager;

    /** Flag to enable stats */
    private boolean enableStats = false;

    // stat counters
    private int hitCount = 0;
    private int missCount = 0;


    /**
     * Instantiates a new default Dn factory.
     *
     * @param schemaManager The SchemaManager instance
     * @param dnCache The cache used to store DNs
     */
    public DefaultDnFactory( SchemaManager schemaManager, Cache< String, Dn > dnCache )
    {
        this.schemaManager = schemaManager;
        this.dnCache = dnCache;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Dn create( String dn ) throws LdapInvalidDnException
    {
        if ( dn == null )
        {
            return null;
        }

        if ( dn.trim().length() == 0 )
        {
            return Dn.ROOT_DSE;
        }

        Dn cachedDn = null;

        // read the explanation at the above DN_CACHE variable declaration
        // for the reason for performing this check
        if ( dnCache != null )
        {
            cachedDn = dnCache.get( dn );
        }

        if ( cachedDn == null )
        {
            LOG.debug( "Dn {} not found in the cache, creating", dn );

            cachedDn = new Dn( schemaManager, dn );

            if ( dnCache != null )
            {
                dnCache.put( dn, cachedDn );
            }

            if ( enableStats )
            {
                missCount++;
            }
        }
        else
        {
            if ( enableStats )
            {
                hitCount++;
            }
        }

        LOG.debug( "Dn {} found in the cache", dn );

        if ( enableStats )
        {
            LOG.debug( "Dn cache hit - {} , miss - {} and is normalized = {}", hitCount, missCount, cachedDn.isSchemaAware() );
        }

        return cachedDn;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Dn create( String... upRdns ) throws LdapInvalidDnException
    {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        
        for ( String s : upRdns )
        {
            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                sb.append( ',' );
            }
            
            sb.append( s );
        }

        String dn = sb.toString();
        
        return create( dn );
    }

}
