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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * Default OID registry implementation used to resolve a schema object OID 
 * to a name and vice-versa.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultOidRegistry implements OidRegistry
{
    /** static class logger */
    private final static Logger log = LoggerFactory.getLogger( DefaultOidRegistry.class );
    /** Maps OID to a name or a list of names if more than one name exists */
    private Map byOid = new HashMap();
    /** Maps several names to an OID */
    private Map<String,String> byName = new HashMap<String,String>();


    /**
     * @see org.apache.directory.server.schema.registries.OidRegistry#getOid(java.lang.String)
     */
    public String getOid( String name ) throws NamingException
    {
        if ( StringTools.isEmpty( name ) )
        {
            throw new NamingException( "name should not be empty" );
        }
        /* If name is an OID than we return it back since inherently the
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
            String oid = ( String ) byName.get( name );
            if ( log.isDebugEnabled() )
            {
                log.debug( "looked up OID '" + oid + "' with id '" + name + "'" );
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
        if ( !name.equals( lowerCase ) && byName.containsKey( lowerCase ) )
        {
            String oid = ( String ) byName.get( lowerCase );
            if ( log.isDebugEnabled() )
            {
                log.debug( "looked up OID '" + oid + "' with id '" + name + "'" );
            }

            // We expect to see this version of the key again so we add it
            byName.put( name, oid );
            return oid;
        }

        NamingException fault = new NamingException( "OID for name '" + name + "' was not "
            + "found within the OID registry" );
        throw fault;
    }


    /**
     * @see org.apache.directory.server.schema.registries.OidRegistry#hasOid(java.lang.String)
     */
    public boolean hasOid( String name )
    {
        if ( this.byName.containsKey( name ) || this.byOid.containsKey( name ) )
        {
            return true;
        }

        String normalized = name.toLowerCase();
        return this.byName.containsKey( normalized ) || this.byOid.containsKey( normalized );
    }


    /**
     * @see org.apache.directory.server.schema.registries.OidRegistry#getPrimaryName(java.lang.String)
     */
    public String getPrimaryName( String oid ) throws NamingException
    {
        Object value = byOid.get( oid );

        if ( null == value )
        {
            NamingException fault = new NamingException( "OID '" + oid + "' was not found within the OID registry" );
            throw fault;
        }

        if ( value instanceof String )
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "looked up primary name '" + value + "' with OID '" + oid + "'" );
            }
            return ( String ) value;
        }

        String name = ( String ) ( ( List ) value ).get( 0 );
        if ( log.isDebugEnabled() )
        {
            log.debug( "looked up primary name '" + name + "' with OID '" + oid + "'" );
        }
        return name;
    }


    /**
     * @see org.apache.directory.server.schema.registries.OidRegistry#getNameSet(java.lang.String)
     */
    public List getNameSet( String oid ) throws NamingException
    {
        Object value = byOid.get( oid );

        if ( null == value )
        {
            NamingException fault = new NamingException( "OID '" + oid + "' was not found within the OID registry" );
            throw fault;
        }

        if ( value instanceof String )
        {
            List list = Collections.singletonList( value );
            if ( log.isDebugEnabled() )
            {
                log.debug( "looked up names '" + list + "' for OID '" + oid + "'" );
            }
            return list;
        }

        if ( log.isDebugEnabled() )
        {
            log.debug( "looked up names '" + value + "' for OID '" + oid + "'" );
        }
        return ( List ) value;
    }


    /**
     * @see org.apache.directory.server.schema.registries.OidRegistry#list()
     */
    @SuppressWarnings("unchecked")
    public Iterator list()
    {
        return Collections.unmodifiableSet( byOid.keySet() ).iterator();
    }


    /**
     * Get the map of all the oids by their name
     * @return The Map that contains all the oids
     */
    public Map getOidByName()
    {
        return byName;
    }


    /**
     * Get the map of all the oids by their name
     * @return The Map that contains all the oids
     */
    public Map getNameByOid()
    {
        return byOid;
    }


    /**
     * @see org.apache.directory.server.schema.registries.OidRegistry#register(String, String)
     */
    @SuppressWarnings("unchecked")
    public void register( String name, String oid )
    {
        if ( !Character.isDigit( oid.charAt( 0 ) ) )
        {
            throw new RuntimeException( "Swap the parameter order: the oid " + "does not start with a digit!" );
        }

        /*
         * Add the entry for the given name as is and its lowercased version if
         * the lower cased name is different from the given name name.  
         */
        String lowerCase = name.toLowerCase();
        if ( !lowerCase.equals( name ) )
        {
            byName.put( lowerCase, oid );
        }

        // Put both the name and the oid as names
        byName.put( name, oid );
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
        Object value;
        if ( !byOid.containsKey( oid ) )
        {
            value = name;
        }
        else
        {
            ArrayList list;
            value = byOid.get( oid );

            if ( value instanceof String )
            {
                String existingName = ( String ) value;

                // if the existing name is already there we don't readd it
                if ( existingName.equalsIgnoreCase( name ) )
                {
                    return;
                }

                list = new ArrayList();
                list.add( name );
                list.add( value );
                value = list;
            }
            else if ( value instanceof ArrayList )
            {
                list = ( ArrayList ) value;

                for ( int ii = 0; ii < list.size(); ii++ )
                {
                    // One form or another of the name already exists in list
                    if ( !name.equalsIgnoreCase( ( String ) list.get( ii ) ) )
                    {
                        return;
                    }
                }

                list.add( name );
            }
        }

        byOid.put( oid, value );
        if ( log.isDebugEnabled() )
        {
            log.debug( "registed name '" + name + "' with OID: " + oid );
        }
    }


    public void unregister( String numericOid ) throws NamingException
    {
        byOid.remove( numericOid );
        Iterator<String> names = byName.keySet().iterator();
        List<String> namesToRemove = new ArrayList<String>(); 
        while ( names.hasNext() )
        {
            String name = names.next();
            if ( numericOid.equals( byName.get( name ) ) )
            {
                namesToRemove.add( name );
            }
        }
        
        for ( String name : namesToRemove )
        {
            byName.remove( name );
        }
    }
}
