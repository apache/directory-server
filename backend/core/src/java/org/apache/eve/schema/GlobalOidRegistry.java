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
package org.apache.eve.schema;


import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Collections;

import javax.naming.NamingException;

import org.apache.eve.SystemPartition;
import org.apache.eve.schema.bootstrap.BootstrapOidRegistry;


/**
 * Default OID registry implementation used to resolve a schema object OID 
 * to a name and vice-versa.
 * 
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev: 55327 $
 */
public class GlobalOidRegistry implements OidRegistry
{ 
    /** Maps OID to a name or a list of names if more than one name exists */
    private Hashtable byOid = new Hashtable();
    /** Maps several names to an OID */
    private Hashtable byName = new Hashtable();
    /** Default OidRegistryMonitor */
    private OidRegistryMonitor monitor = new OidRegistryMonitorAdapter();
    /** the underlying bootstrap registry to delegate on misses to */
    private BootstrapOidRegistry bootstrap;
    /** the system partition where we keep attributeType updates */
    private SystemPartition systemPartition;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * Creates a default OidRegistry by initializing the map and the montior.
     */
    public GlobalOidRegistry( SystemPartition systemPartition,
            BootstrapOidRegistry bootstrap )
    {
        this.bootstrap = bootstrap;
        if ( this.bootstrap == null )
        {
            throw new NullPointerException( "the bootstrap registry cannot be null" ) ;
        }

        this.systemPartition = systemPartition;
        if ( this.systemPartition == null )
        {
            throw new NullPointerException( "the system partition cannot be null" ) ;
        }
    }




    /**
     * Gets the monitor.
     *
     * @return the monitor
     */
    OidRegistryMonitor getMonitor()
    {
        return monitor;
    }


    /**
     * Sets the monitor.
     *
     * @param monitor monitor to set.
     */
    void setMonitor( OidRegistryMonitor monitor )
    {
        this.monitor = monitor;
    }


    // ------------------------------------------------------------------------
    // Service Methods
    // ------------------------------------------------------------------------


    /**
     * @see OidRegistry#getOid(String)
     */
    public String getOid( String name ) throws NamingException
    {
        if ( name == null )
        {
            throw new NamingException( "name should not be null" );
        }

        /* If name is an OID than we return it back since inherently the
         * OID is another name for the object referred to by OID and the
         * caller does not know that the argument is an OID String.
         */
        if ( Character.isDigit( name.charAt( 0 ) ) )
        {
            monitor.getOidWithOid( name );
            return name;
        }

        // If name is mapped to a OID already return OID
        if ( byName.containsKey( name ) )
        {
            String oid = ( String ) byName.get( name );
            monitor.oidResolved( name, oid );
            return oid;
        }

        if ( bootstrap.hasOid( name ) )
        {
            String oid = bootstrap.getOid( name );
            monitor.oidResolved( name, oid );
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
        if ( ! name.equals( lowerCase )
        && byName.containsKey( lowerCase ) )
        {
            String oid = ( String ) byName.get( lowerCase );
            monitor.oidResolved( name, lowerCase, oid );

            // We expect to see this version of the key again so we add it
            byName.put( name, oid );
            return oid;
        }

        NamingException fault = new NamingException ( "OID for name '"
                + name + "' was not " + "found within the OID registry" );
        monitor.oidResolutionFailed( name, fault );
        throw fault;
    }


    /**
     * @see OidRegistry#hasOid(String)
     */
    public boolean hasOid( String name )
    {
        return this.byName.containsKey( name ) ||
               this.byOid.containsKey( name )  ||
               this.bootstrap.hasOid( name );
    }


    /**
     * @see OidRegistry#getPrimaryName(String)
     */
    public String getPrimaryName( String oid ) throws NamingException
    {
        Object value = byOid.get( oid );
        
        if ( null == value )
        {
            NamingException fault = new NamingException ( "OID '" + oid
                    + "' was not found within the OID registry" );
            monitor.oidDoesNotExist( oid, fault );
            throw fault;
        }
        
        if ( value instanceof String )
        {
            monitor.nameResolved( oid, ( String ) value );
            return ( String ) value;
        }
        
        String name = ( String ) ( ( List ) value ).get( 0 );
        monitor.nameResolved( oid, name );
        return name;
    }


    /**
     * @see OidRegistry#getNameSet(String)
     */
    public List getNameSet( String oid ) throws NamingException
    {
        Object value = byOid.get( oid );
        
        if ( null == value )
        {
            NamingException fault = new NamingException ( "OID '" + oid
                    + "' was not found within the OID registry" );
            monitor.oidDoesNotExist( oid, fault );
            throw fault;
        }
        
        if ( value instanceof String )
        {
            List list = Collections.singletonList( value );
            monitor.namesResolved( oid, list );
            return list;
        }
        
        monitor.namesResolved( oid, ( List ) value );
        return ( List ) value;
    }


    /**
     * @see OidRegistry#list()
     */
    public Iterator list()
    {
        return Collections.unmodifiableSet( byOid.keySet() ).iterator();
    }


    /**
     * @see OidRegistry#register(String, String)
     */
    public void register( String name, String oid )
    {
        if ( ! Character.isDigit( oid.charAt( 0 ) ) )
        {
            throw new RuntimeException( "Swap the parameter order: the oid " +
                "does not start with a digit!" );
        }

        /*
         * Add the entry for the given name as is and its lowercased version if
         * the lower cased name is different from the given name name.  
         */
        String lowerCase = name.toLowerCase();
        if ( ! lowerCase.equals( name ) )
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
        Object value = null;
        if ( ! byOid.containsKey( oid ) )
        {
            value = name;
        }
        else 
        {
            ArrayList list = null;
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
                list.add( value );
                value = list;
            }
            else if ( value instanceof ArrayList )
            {
                list = ( ArrayList ) value;
                
                for ( int ii = 0; ii < list.size(); ii++ )
                {
                    // One form or another of the name already exists in list
                    if ( ! name.equalsIgnoreCase( ( String ) list.get( ii ) ) )
                    {
                        return;
                    }
                }
                
                list.add( name );
            }
        }

        byOid.put( oid, value );
        monitor.registered( name, oid );
    }
}

