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


import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.NamingException;

import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.registries.ObjectClassRegistry;
import org.apache.directory.shared.ldap.schema.registries.OidRegistry;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A plain old java object implementation of an ObjectClassRegistry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultObjectClassRegistry implements ObjectClassRegistry
{
    /** static class logger */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultObjectClassRegistry.class );
    
    /** Speedup for DEBUG mode */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();
    
    /** maps an OID to an ObjectClass */
    private final Map<String,ObjectClass> byOidOC;
    
    /** the registry used to resolve names to OIDs */
    private final OidRegistry oidRegistry;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    /**
     * Creates an empty DefaultObjectClassRegistry.
     *
     * @param oidRegistry used by this registry for OID to name resolution of
     * dependencies and to automatically register and unregister it's aliases and OIDs
     */
    public DefaultObjectClassRegistry( OidRegistry oidRegistry )
    {
        byOidOC = new ConcurrentHashMap<String,ObjectClass>();
        this.oidRegistry = oidRegistry;
    }

    
    // ------------------------------------------------------------------------
    // Service Methods
    // ------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public void register( ObjectClass objectClass ) throws NamingException
    {
        String oid = objectClass.getOid();
        
        if ( byOidOC.containsKey( oid ) )
        {
            String msg = "objectClass " + objectClass.getName() + " w/ OID " + oid
                + " has already been registered!";
            LOG.warn( msg );
            throw new NamingException( msg );
        }

        // Register the name/oid relation
        String name = objectClass.getName();
        
        if ( !StringTools.isEmpty( name ) )
        {
            oidRegistry.register( name, oid );
        }
        
        // Also register the oid/oid relation
        oidRegistry.register( oid, oid );
        
        // Stores the OC in the internal map
        byOidOC.put( oid, objectClass );
        
        if ( IS_DEBUG )
        {
            LOG.debug( "registered objectClass: {}", objectClass );
        }
    }


    /**
     * {@inheritDoc}
     */
    public ObjectClass lookup( String id ) throws NamingException
    {
        String ocId = StringTools.trim( id ).toLowerCase();
        
        if ( StringTools.isEmpty( ocId ) )
        {
            String msg = "Lookup in the OC registry : name should not be empty";
            LOG.error( msg );
            throw new NamingException( msg );
        }
        
        String oid = oidRegistry.getOid( ocId );

        ObjectClass objectClass = byOidOC.get( oid );

        if ( objectClass == null )
        {
            String msg = "objectClass " + id + " w/ OID " + oid + " not registered!";
            LOG.warn( msg );
            throw new NamingException( msg );
        }
        
        if ( IS_DEBUG )
        {
            LOG.debug( "looked objectClass with OID '{}' and got back {}", oid, objectClass );
        }
        
        return objectClass;
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasObjectClass( String id )
    {
        try
        {
            String oid = oidRegistry.getOid( id );
            
            if ( oid == null )
            {
                return false;
            }
            
            return byOidOC.containsKey( oid );
        }
        catch ( NamingException e )
        {
            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    public String getSchemaName( String id ) throws NamingException
    {
        String ocOid = oidRegistry.getOid( id );
        
        if ( ocOid == null )
        {
            String msg = "Element " + id + " not found in the OID registry !";
            LOG.warn( msg );
            throw new NamingException( msg );
        }

        ObjectClass oc = byOidOC.get( ocOid );
        
        if ( oc != null )
        {
            return oc.getSchema();
        }

        String msg = "OID " + id + " not found in oid to " + "ObjectClass map!";
        LOG.warn( msg );
        throw new NamingException( msg );
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<ObjectClass> iterator()
    {
        return byOidOC.values().iterator();
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void unregister( String numericOid ) throws NamingException
    {
        if ( ! OID.isOID( numericOid ) )
        {
            String msg = "Looks like the arg " + numericOid + " is not a numeric OID";
            LOG.warn( msg );
            throw new NamingException( msg );
        }

        byOidOC.remove( numericOid );
    }
}
