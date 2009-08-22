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
package org.apache.directory.server.schema.registries;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.NamingException;

import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.ldap.schema.LdapComparator;
import org.apache.directory.shared.ldap.schema.parsers.LdapComparatorDescription;
import org.apache.directory.shared.ldap.schema.registries.ComparatorRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A simple POJO implementation of the ComparatorRegistry service interface.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultComparatorRegistry implements ComparatorRegistry
{
    /** static class logger */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultComparatorRegistry.class );
    
    /** A speedup for debug */
    private static final boolean DEBUG = LOG.isDebugEnabled();

    /** the comparators in this registry */
    private final Map<String, LdapComparator<?>> byOidComparator;
    
    /** maps oids to a comparator description */
    private final Map<String, LdapComparatorDescription> oidToDescription;

    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * Creates a DefaultComparatorRegistry by initializing the maps
     */
    public DefaultComparatorRegistry()
    {
        byOidComparator = new ConcurrentHashMap<String, LdapComparator<?>>();
        oidToDescription = new ConcurrentHashMap<String, LdapComparatorDescription>();
    }


    // ------------------------------------------------------------------------
    // Service Methods
    // ------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public void register( LdapComparatorDescription description, LdapComparator<?> comparator ) throws NamingException
    {
        String oid = description.getOid();
        
        if ( byOidComparator.containsKey( oid ) )
        {
            String msg = "Comparator '" + description + "' with OID " + oid + " already registered!";
            LOG.warn( msg );
            throw new NamingException( msg );
        }

        oidToDescription.put( oid, description );
        byOidComparator.put( oid, comparator );
        
        if ( DEBUG )
        {
            LOG.debug( "registed comparator with OID: {}", oid );
        }
    }

    
    /**
     * {@inheritDoc}
     */
    public void register( LdapComparator<?> comparator ) throws NamingException
    {
        String oid = comparator.getOid();
        
        if ( byOidComparator.containsKey( oid ) )
        {
            String msg = "Comparator '" + comparator + "' with OID " + oid + " already registered!";
            LOG.warn( msg );
            throw new NamingException( msg );
        }

        byOidComparator.put( oid, comparator );
        
        if ( DEBUG )
        {
            LOG.debug( "registed comparator with OID: {}", oid );
        }
    }

    
    /**
     * Return the schema, contained in the first position of the extensions
     */
    private static String getSchema( LdapComparatorDescription desc )
    {
        List<String> values = desc.getExtensions().get( "X-SCHEMA" );
        
        if ( ( values == null ) || ( values.size() == 0 ) )
        {
            return "other";
        }
        
        return values.get( 0 );
    }
    

    /**
     * {@inheritDoc}
     */
    public LdapComparator<?> lookup( String oid ) throws NamingException
    {
        LdapComparator<?> c = byOidComparator.get( oid );
        
        if ( c == null )
        {
            String msg = "Comparator not found for OID: " + oid;
            LOG.error( msg );
            throw new NamingException( msg );
        }
        
        if ( DEBUG )
        {
            LOG.debug( "looked up comparator with OID: {}", oid );
        }
        
        return c;
    }


    /**
     * {@inheritDoc}
     */
    public boolean contains( String oid )
    {
        return byOidComparator.containsKey( oid );
    }


    /**
     * {@inheritDoc}
     */
    public String getSchemaName( String oid ) throws NamingException
    {
        if ( ! OID.isOID( oid ) )
        {
            String msg = "OID " + oid + " is not a numeric OID";
            LOG.error( msg );
            throw new NamingException( msg );
        }

        LdapComparatorDescription description = oidToDescription.get( oid );
        
        if ( description != null )
        {
            return getSchema( description );
        }

        String msg = "OID " + oid + " not found in oid to description map!";
        LOG.error( msg );
        throw new NamingException( msg );
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<String> oidsIterator()
    {
        return byOidComparator.keySet().iterator();
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<LdapComparator<?>> iterator()
    {
        return byOidComparator.values().iterator();
    }


    /**
     * {@inheritDoc}
     */
    public void unregister( String oid ) throws NamingException
    {
        if ( ! OID.isOID( oid ) )
        {
            String msg = "OID " + oid + " is not a numeric OID";
            LOG.error( msg );
            throw new NamingException( msg );
        }

        byOidComparator.remove( oid );
        oidToDescription.remove( oid );
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void unregisterSchemaElements( String schemaName )
    {
        List<String> oids = new ArrayList<String>( byOidComparator.keySet() );
        
        for ( String oid : oids )
        {
        	LdapComparatorDescription description = oidToDescription.get( oid );
            String schemaNameForOid = getSchema( description );
            
            if ( schemaNameForOid.equalsIgnoreCase( schemaName ) )
            {
                byOidComparator.remove( oid );
                oidToDescription.remove( oid );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public void renameSchema( String originalSchemaName, String newSchemaName )
    {
        List<String> oids = new ArrayList<String>( byOidComparator.keySet() );
        
        for ( String oid : oids )
        {
        	LdapComparatorDescription description = oidToDescription.get( oid );
            String schemaNameForOid = getSchema( description );
            
            if ( schemaNameForOid.equalsIgnoreCase( originalSchemaName ) )
            {
                List<String> schemaExt = description.getExtensions().get( "X-SCHEMA" );
                schemaExt.clear();
                schemaExt.add( newSchemaName );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<LdapComparatorDescription> ldapComparatorDescriptionIterator()
    {
        return oidToDescription.values().iterator();
    }
}
