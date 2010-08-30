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

import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A DN factory.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DNFactory
{

    /** a cache for DNs */
    // this cache instance will be initialized only when DNFactory gets initialized by
    // directory service. Other classes(like tests) can still use this factory but no cache is maintained
    // FIXME this decision needs to be evaluated
    private static Cache DN_CACHE;

    private static final Logger LOG = LoggerFactory.getLogger( DNFactory.class );

    private static SchemaManager schemaManager;

    private static boolean enableStats = false;

    // stat counters
    private static int hitCount = 0;
    private static int missCount = 0;


    /**
     * searches the cache first for a possible DN value based on the given 'upName' match.
     * If a DN is present in the cache will return it (after normalizing if required)
     * otherwise will create a new DN instance and stores in the cache before returning it
     *
     * Note that the DN cache is maintained by using user provided DN name as key
     *
     * @param dn the upName of the DN
     * @param schemaManager the schema manager (optional)
     * @return a DN
     * @throws LdapInvalidDnException
     */
    public static DN create( String dn, SchemaManager schemaManager ) throws LdapInvalidDnException
    {
        if ( dn == null )
        {
            return null;
        }

        if ( dn.trim().length() == 0 )
        {
            return DN.EMPTY_DN;
        }

        DN cachedDN = null;

        // read the explanation at the above DN_CACHE variable declaration
        // for the reason for performing this check
        if( DN_CACHE != null )
        {
            Element dnCacheEntry = DN_CACHE.get( dn );
            
            if ( dnCacheEntry != null )
            {
                cachedDN = ( DN ) dnCacheEntry.getValue();
            }
        }

        if ( cachedDN == null )
        {
            LOG.debug( "DN {} not found in the cache, creating", dn );

            cachedDN = new DN( dn, schemaManager );

            if( DN_CACHE != null )
            {
                DN_CACHE.put( new Element( dn, cachedDN ) );
            }

            if ( enableStats )
            {
                missCount++;
            }
        }
        else
        {
            if ( !cachedDN.isNormalized() && ( schemaManager != null ) )
            {
                cachedDN.normalize( schemaManager );
            }

            if ( enableStats )
            {
                hitCount++;
            }
        }

        LOG.debug( "DN {} found in the cache", dn );
        
        if ( enableStats )
        {
            //System.out.println( "DN '" + cachedDN + "' found in the cache and isNormalized " + cachedDN.isNormalized() );
            System.out.println( "DN cache hit - " + hitCount + ", miss - " + missCount + " and is normalized = "
                + cachedDN.isNormalized() );
        }
        
        return cachedDN;
    }


    public static DN create( String... upRdns ) throws LdapInvalidDnException
    {
        return create( schemaManager, upRdns );
    }


    public static DN create( SchemaManager schemaManager, String... upRdns ) throws LdapInvalidDnException
    {
        StringBuilder sb = new StringBuilder();
        for ( String s : upRdns )
        {
            sb.append( s ).append( ',' );
        }

        String dn = sb.toString();
        dn = dn.substring( 0, dn.length() - 1 );
        return create( dn, schemaManager );
    }


    public static DN create( DN dn ) throws LdapInvalidDnException
    {
        return create( dn.getName(), schemaManager );
    }


    public static DN create( DN dn, SchemaManager schemaManager ) throws LdapInvalidDnException
    {
        return create( dn.getName(), schemaManager );
    }


    public static DN create( String dn ) throws LdapInvalidDnException
    {
        return create( dn, schemaManager );
    }


    public static void setSchemaManager( SchemaManager schemaManager )
    {
        DNFactory.schemaManager = schemaManager;
    }

    
    /**
     * this method will be called from the DefaultDirectoryService during startup
     */
    protected static void initialize( DirectoryService dirService )
    {
        schemaManager = dirService.getSchemaManager();
        DN_CACHE = dirService.getCacheService().getCache( "dnCache" );
    }
}
