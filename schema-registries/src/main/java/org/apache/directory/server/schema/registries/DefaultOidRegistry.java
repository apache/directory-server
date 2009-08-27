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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.NamingException;
import javax.naming.directory.NoSuchAttributeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.ldap.schema.registries.OidRegistry;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * Default OID registry implementation used to resolve a schema object OID 
 * to a name and vice-versa. 
 * <br/>
 * We are storing the schema elements in two data structures :
 * <li>an oid to names map</li>
 * <li>a name to oid map</li>
 * <br/>
 * The first data structure contains a list of names associated with the given
 * oid. The oid itself is not necessarily stored in this list, unless the schema
 * object does not have any name.<br/>
 * The second data structure contains all the names with the associated OID. We 
 * also store the oid -> oid relation, to allow us to look for a registered 
 * schema object using its oid.<br/>
 * <br/>
 * For instance, if we have registered the C AttributeType, which OID is
 * 2.5.4.6 and has another name, 'country', the data structure will contain :<br>
 * 
 * <ul>
 * <li>
 * byOid
 *   <ul>
 *     <li>2.5.4.6 -> {'c', 'country'}</li>
 *   </ul>
 * </li>
 * <li>
 * byName
 *   <ul>
 *     <li>'c' -> 2.5.4.6</li>
 *     <li>'country' -> 2.5.4.6</li>
 *     <li>'2.5.4.6' -> 2.5.4.6</li>
 *   </ul>
 * </li>
 * </ul>
 * 
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultOidRegistry implements OidRegistry
{
    /** static class logger */
    private static final Logger LOG = LoggerFactory.getLogger( OidRegistry.class );

    /** Speedup for DEBUG mode */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();
    
    /** Maps OID to a name or a list of names if more than one name exists */
    private Map<String, List<String>> byOid = new ConcurrentHashMap<String, List<String>>();
    
    /** Maps several names to an OID */
    private Map<String,String> byName = new ConcurrentHashMap<String,String>();


    /**
     * {@inheritDoc}
     */
    public String getOid( String name ) throws NamingException
    {
        if ( StringTools.isEmpty( name ) )
        {
            throw new NamingException( "name should not be empty" );
        }
        
        /* If name is an OID then we return it back since inherently the
         * OID is another name for the object referred to by OID and the
         * caller does not know that the argument is an OID String.
         */
        if ( StringTools.isDigit( name.charAt( 0 ) ) )
        {
            return name;
        }

        // If name is mapped to a OID already return OID
        if ( byName.containsKey( name ) )
        {
            String oid = byName.get( name );
            
            if ( IS_DEBUG )
            {
                LOG.debug( "looked up OID '" + oid + "' with id '" + name + "'" );
            }
            
            return oid;
        }

        /*
         * As a last resort we check if name is not normalized and if the
         * normalized version used as a key returns an OID.  If the normalized
         * name works add the normalized name as a key with its OID to the
         * byName lookup.  BTW these normalized versions of the key are not
         * returned on a getNameSet.
         */
        String lowerCase = name.trim().toLowerCase();
        
        String oid = byName.get( lowerCase );
        
        if ( oid != null )
        {
            if ( IS_DEBUG )
            {
                LOG.debug( "looked up OID '{}' with id '{}'", oid, name );
            }

            return oid;
        }

        NamingException fault = new NoSuchAttributeException( "OID for name '" + name + "' was not "
            + "found within the OID registry" );
        LOG.warn( fault.getMessage() );
        throw fault;
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasOid( String name )
    {
        if ( StringTools.isEmpty( name ) )
        {
            return false;
        }
        
        String normalized = name.trim().toLowerCase();
        
        return byName.containsKey( normalized );
    }


    /**
     * {@inheritDoc}
     */
    public String getPrimaryName( String oid ) throws NamingException
    {
        List<String> value = byOid.get( oid );

        if ( null == value )
        {
            String msg = "OID '" + oid + "' was not found within the OID registry";
            LOG.error( msg );
            throw new NamingException( msg );
        }

        String name = value.get( 0 );
        
        if ( IS_DEBUG )
        {
            LOG.debug( "looked up primary name '{}' with OID '{}'", name, oid );
        }
        
        return name;
    }


    /**
     * {@inheritDoc}
     */
    public List<String> getNameSet( String oid ) throws NamingException
    {
        List<String> value = byOid.get( oid );

        if ( null == value )
        {
            String msg = "OID '" + oid + "' was not found within the OID registry";
            LOG.error( msg );
            throw new NamingException( msg );
        }

        if ( IS_DEBUG )
        {
            LOG.debug( "looked up names '{}' for OID '{}'", value, oid );
        }
        
        return value;
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<String> list()
    {
        return Collections.unmodifiableSet( byOid.keySet() ).iterator();
    }


    /**
     * {@inheritDoc}
     */
    public Map<String, String> getOidByName()
    {
        return byName;
    }


    /**
     * {@inheritDoc}
     */
    public Map<String, List<String>> getNameByOid()
    {
        return byOid;
    }


    /**
     * {@inheritDoc}
     */
    public void register( String name, String oid ) throws NamingException
    {
        if ( !OID.isOID( oid ) )
        {
            String message = "Swap the parameter order: the oid " + 
            "does not start with a digit, or is not an OID!";
            
            LOG.debug( message );
            throw new NamingException( message );
        }
        
        if ( StringTools.isEmpty( name ) )
        {
            String message = "The name is empty for OID " + oid;
            LOG.error( message );
            throw new NamingException( message );
        }

        /*
         * Add the entry for the given name as is and its lowercased version if
         * the lower cased name is different from the given name name.  
         */
        String lowerCase = name.trim().toLowerCase();

        // Put both the name and the oid as names
        byName.put( lowerCase, oid );
        byName.put( oid, oid );

        /*
         * Update OID Map
         * 
         * 1). Check if we already have a value[s] stored
         *      1a). Value is a single value and is a String
         *          Replace value with list containing old and new values
         *      1b). More than one value stored in a list
         *          Add new value to the list
         * 2). If we do not have a value then we just add it as a String
         */
        List<String> value = byOid.get( oid );
        
        if ( value == null )
        {
            value = new ArrayList<String>( 1 );
            value.add( lowerCase );
        }
        else
        {
            if ( value.contains( lowerCase ) )
            {
                return;
            }

            value.add( lowerCase );
        }

        byOid.put( oid, value );
        
        if ( IS_DEBUG )
        {
            LOG.debug( "registed name '" + name + "' with OID: " + oid );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void unregister( String numericOid ) throws NamingException
    {
        // First, remove the <OID, names> from the byOID map
        List<String> names = byOid.remove( numericOid );
        
        // Then remove all the <name, OID> from the byName map
        if ( names != null )
        {
            for ( String name:names )
            {
                byName.remove( name );
            }
        }

        // Last, remove the <OID, OID> from the byName map
        byName.remove( numericOid );
        
        if ( IS_DEBUG )
        {
            LOG.debug( "Unregisted name '{}' with OID: {}", StringTools.listToString( names ), numericOid );
        }
    }
}
