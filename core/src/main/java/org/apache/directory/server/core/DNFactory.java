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


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    /** a map of <DN-upName, DN> tuples */
    // NOTE: currently a map is used, will eventually be replaced with ehCache if thie experiment succeeds
    private static final Map<String, DN> DN_CACHE = new ConcurrentHashMap<String, DN>();

    private static final Logger LOG = LoggerFactory.getLogger( DNFactory.class );

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
        if ( dn == null || dn.trim().length() == 0 )
        {
            return null;
        }

        DN cachedDN = DN_CACHE.get( dn );

        if ( cachedDN == null )
        {
            LOG.debug( "DN {} not found in the cache, creating", dn );

            cachedDN = new DN( dn, schemaManager );

            DN_CACHE.put( dn, cachedDN );
            missCount++;
        }
        else
        {
            if ( !cachedDN.isNormalized() && ( schemaManager != null ) )
            {
                cachedDN.normalize( schemaManager.getNormalizerMapping() );
            }

            hitCount++;
        }

        LOG.debug( "DN {} found in the cache", dn );
        System.out.println( "DN cache hit - " + hitCount + ", miss - " + missCount + " and is normalized = "
            + cachedDN.isNormalized() );
        return cachedDN;
    }


    public static DN create( String... upRdns ) throws LdapInvalidDnException
    {
        return create( null, upRdns );
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
        return create( dn.getName(), null );
    }


    public static DN create( DN dn, SchemaManager schemaManager ) throws LdapInvalidDnException
    {
        return create( dn.getName(), schemaManager );
    }


    public static DN create( String dn ) throws LdapInvalidDnException
    {
        return create( dn, null );
    }

}
