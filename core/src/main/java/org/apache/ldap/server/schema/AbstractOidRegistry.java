/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.ldap.server.schema;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;

import org.apache.asn1.codec.util.StringUtils;
import org.apache.asn1new.primitives.OID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Object identifier registry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 355412 $
 */
public abstract class AbstractOidRegistry implements OidRegistry
{
    /** The LoggerFactory used by this Interceptor */
    private static Logger log = LoggerFactory.getLogger( AbstractOidRegistry.class );

    /** Maps OID to a name or a list of names if more than one name exists */
    protected Map byOid = new HashMap();
    
    /** Maps several names to an OID */
    protected Map byName = new HashMap();

    /**
     * Gets the object identifier for a common name or returns the argument
     * as-is if it is an object identifier.
     * 
     * @param name the name to lookup an OID for
     * @return the OID string associated with a name
     * @throws NamingException if name does not map to an OID
     */
    //public String getOid( String name ) throws NamingException;

    /**
     * Gets the primary name associated with an OID.  The primary name is the
     * first name specified for the OID.
     * 
     * @param oid the object identifier
     * @return the primary name
     * @throws NamingException if oid does not exist
     */
    public String getPrimaryName( String oid ) throws NamingException
    {
        Object value = byOid.get( oid );
        
        if ( null == value )
        {
            NamingException fault = new NamingException ( "OID '" + oid
                    + "' was not found within the OID registry" );
            throw fault;
        }
        
        if ( value instanceof String )
        {
            return ( String ) value;
        }
        
        return ( String ) ( ( List ) value ).get( 0 );
    }

    /**
     * Gets the names associated with an OID.  An OID is unique however it may 
     * have many names used to refer to it.  A good example is the cn and
     * commonName attribute names for OID 2.5.4.3.  Within a server one name 
     * within the set must be chosen as the primary name.  This is used to
     * name certain things within the serimport org.apache.commons.lang.StringUtils;
ver internally.  If there is more than
     * one name then the first name is taken to be the primary.
     * 
     * @param oid the OID for which we return the set of common names
     * @return a sorted set of names
     * @throws NamingException if oid does not exist
     */
    public List getNameSet( String oid ) throws NamingException
    {
    	Object value = byOid.get( oid );
        
        if ( null == value )
        {
            return null;
        }

        if ( value instanceof String )
        {
            List list = Collections.singletonList( value );

            return list;
        }
        
        return ( List ) value;
    }
    
    /**
     * Lists all the OIDs within the registry.  This may be a really big list.
     * 
     * @return all the OIDs registered
     */
    public Iterator list()
    {

        return Collections.unmodifiableSet( byOid.keySet() ).iterator();
    }
    
    /**
     * Adds an OID name pair to the registry.
     * 
     * @param name the name to associate with the OID
     * @param oid the OID to add or associate a new name with
     */
    /**
     * @see org.apache.ldap.server.schema.OidRegistry#register(String, String)
     */
    public void register( String name, String oid )
    {
    	// Defending the method against bad parameters
    	if ( StringUtils.isEmpty( name ) )
    	{
    		log.error( "Cannot register an empty name in the OID registry." );
            throw new RuntimeException( "The name can't be null or empty !" );
    	}
    	
    	if ( StringUtils.isEmpty( oid ) )
    	{
    		log.error( "Cannot register an empty oid in the OID registry." );
            throw new RuntimeException( "The oid can't be null or empty !" );
    	}
    	
    	// The oid must be valid
    	if ( OID.isOID( oid ) == false )
    	{
    		log.error( "The oid " + oid + " is invalid" );
    		throw new RuntimeException( "Invalid oid : " + oid );
    	}
    	
        /*
         * Add the entry for the lowercased version of the name   
         */
        String nameLower = name.toLowerCase();
        
        byName.put( nameLower, oid );
        
        if ( byName.containsKey( oid ) == false )
        {
        	// We only add the oid as a key if it does not exists.
            byName.put( oid, oid );
        }
        
        /*
         * Update OID Map. An oid can be mapped to more than one name.
         * 
         * 1). Check if we already have a value[s] stored
         *      1a). Value is a single value and is a String
         *          Replace value with list containing old and new values
         *      1b). More than one value stored in a list
         *          Add new value to the list
         * 2). If we do not have a value then we just add it as a String
         */
        Object value;
        
        if ( ! byOid.containsKey( oid ) )
        {
            value = nameLower;
        }
        else 
        {
            List list;
            value = byOid.get( oid );
            
            if ( value instanceof String )
            {
            	// We have only one value : a String. 
            	// We have to transform it to an set
                String existingName = ( String ) value;
                
                // if the existing name is already there we don't read it
                if ( existingName.equals( nameLower ) )
                {
                    return;
                }
                
                list = new ArrayList();
                
                // Store the previous name
                list.add( existingName );
                
                // Add the new one
                list.add( nameLower );
                
                value = list;
            }
            else if ( value instanceof Set )
            {
                list = ( ArrayList ) value;
                
                Iterator names = list.iterator();
                
                while ( names.hasNext() )
                {
                	String currentName = (String)names.next();
                	
                	if ( currentName.compareToIgnoreCase( nameLower ) == 0 )
                	{
                		// The name is already present
                		return;
                	}
                }
                
                list.add( nameLower );
            }
        }

        byOid.put( oid, value );
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
     * A String representation of the class
     */
    public String toString( String tabs )
    {
    	StringBuffer sb = new StringBuffer();

    	sb.append( tabs ).append( "by name :\n" );
    	sb.append( StringUtils.mapToString( byName, tabs + "  " ) );
    	sb.append( '\n' );
    	
    	sb.append( tabs ).append( "by OID :\n" );
    	sb.append( StringUtils.mapToString( byOid, tabs + "  " ) );

    	sb.append( '\n' );
    	return sb.toString();
    }
    
    /**
     * A String representation of the class
     */
    public String toString()
    {
    	return toString( "" );
    }
}
