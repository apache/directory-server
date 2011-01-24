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

package org.apache.directory.server.core;


import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.apache.directory.shared.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The default Dn factory implementation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DefaultDNFactory implements DNFactory
{
    private static final Logger LOG = LoggerFactory.getLogger( DefaultDNFactory.class );

    /** The cache for DNs */
    private Cache dnCache;

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
     * @param directoryService the directory service
     */
    public DefaultDNFactory( SchemaManager schemaManager, Cache dnCache )
    {
        this.schemaManager = schemaManager;
        this.dnCache = dnCache;
    }


    /**
     * {@inheritDoc}
     */
    public Dn create( String dn ) throws LdapInvalidDnException
    {
        if ( dn == null )
        {
            return null;
        }

        if ( dn.trim().length() == 0 )
        {
            return Dn.EMPTY_DN;
        }

        Dn cachedDn = null;

        // read the explanation at the above DN_CACHE variable declaration
        // for the reason for performing this check
        if ( dnCache != null )
        {
            Element dnCacheEntry = dnCache.get( dn );

            if ( dnCacheEntry != null )
            {
                cachedDn = (Dn) dnCacheEntry.getValue();
            }
        }

        if ( cachedDn == null )
        {
            LOG.debug( "Dn {} not found in the cache, creating", dn );

            cachedDn = new Dn( dn, schemaManager );

            if ( dnCache != null )
            {
                dnCache.put( new Element( dn, cachedDn) );
            }

            if ( enableStats )
            {
                missCount++;
            }
        }
        else
        {
            if ( !cachedDn.isNormalized() && ( schemaManager != null ) )
            {
                cachedDn.normalize( schemaManager );
            }

            if ( enableStats )
            {
                hitCount++;
            }
        }

        LOG.debug( "Dn {} found in the cache", dn );

        if ( enableStats )
        {
            //System.out.println( "Dn '" + cachedDn + "' found in the cache and isNormalized " + cachedDn.isNormalized() );
            System.out.println( "Dn cache hit - " + hitCount + ", miss - " + missCount + " and is normalized = "
                + cachedDn.isNormalized() );
        }

        return cachedDn;
    }


    /**
     * {@inheritDoc}
     */
    public Dn create( String... upRdns ) throws LdapInvalidDnException
    {
        StringBuilder sb = new StringBuilder();
        for ( String s : upRdns )
        {
            sb.append( s ).append( ',' );
        }

        String dn = sb.toString();
        dn = dn.substring( 0, dn.length() - 1 );
        return create( dn );
    }

}
