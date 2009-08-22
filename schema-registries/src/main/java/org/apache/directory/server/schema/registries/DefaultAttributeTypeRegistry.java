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


import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.NamingException;
import javax.naming.directory.NoSuchAttributeException;

import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.normalizers.NoOpNormalizer;
import org.apache.directory.shared.ldap.schema.normalizers.OidNormalizer;
import org.apache.directory.shared.ldap.schema.registries.AttributeTypeRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A plain old java object implementation of an AttributeTypeRegistry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultAttributeTypeRegistry implements AttributeTypeRegistry
{
    /** static class logger */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultAttributeTypeRegistry.class );

    /** Speedup for DEBUG mode */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();
    
    /** maps an OID to an AttributeType */
    private final Map<String,AttributeType> byOidAT;
    
    /** maps OIDs to a Set of descendants for that OID */
    private final Map<String,Set<AttributeType>> oidToDescendantSet;
    
    /** the Registry used to resolve names to OIDs */
    private final OidRegistry oidRegistry;
    
    /** cached Oid/normalizer mapping */
    private transient Map<String, OidNormalizer> oidNormalizerMap;
    

    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------
    /**
     * Creates an empty DefaultAttributeTypeRegistry.
     *
     * @param oidRegistry used by this registry for OID to name resolution of
     * dependencies and to automatically register and unregister it's aliases and OIDs
     */
    public DefaultAttributeTypeRegistry( OidRegistry oidRegistry )
    {
        byOidAT = new ConcurrentHashMap<String,AttributeType>();
        oidToDescendantSet= new ConcurrentHashMap<String,Set<AttributeType>>();
        oidNormalizerMap = new ConcurrentHashMap<String, OidNormalizer>();
        this.oidRegistry = oidRegistry;
    }


    // ------------------------------------------------------------------------
    // Service Methods
    // ------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public void register( AttributeType attributeType ) throws NamingException
    {
        if ( byOidAT.containsKey( attributeType.getOid() ) )
        {
            String msg = "attributeType " + attributeType.getName() + " w/ OID " + attributeType.getOid()
            + " has already been registered!";
            LOG.error( msg );
            throw new NamingException( msg );
        }

        // First, register the AttributeType names and oid in the global
        // OidRegistry
        String[] names = attributeType.getNamesRef();
        String oid = attributeType.getOid();
        
        for ( String name : names )
        {
            oidRegistry.register( name, oid );
        }
        
        // Also register the oid/oid relation
        oidRegistry.register( oid, oid );

        // Inject the attributeType into the oid/normalizer map
        addMappingFor( attributeType );

        // Register this AttributeType into the Descendant map
        registerDescendants( attributeType );
        
        // Internally associate the OID to the registered AttributeType
        byOidAT.put( attributeType.getOid(), attributeType );
        
        if ( IS_DEBUG )
        {
            LOG.debug( "registred attributeType: {}", attributeType );
        }
    }


    /**
     * Store the AttributeType into a map associating an AttributeType to its
     * descendants.
     * 
     * @param attributeType The attributeType to register
     * @throws NamingException If something went wrong
     */
    public void registerDescendants( AttributeType attributeType ) throws NamingException
    {
        // add this attribute to descendant list of other attributes in superior chain
        onRegisterAddToAncestorDescendants( attributeType, attributeType.getSuperior() );
    }
    
    
    /**
     * Recursively adds a new attributeType to the descendant's list of all ancestors
     * until top is reached.  Top will not have the new type added.
     * 
     * @param newType the new attributeType being added
     * @param ancestor some ancestor from superior up to and including top
     * @throws NamingException if there are resolution failures
     */
    protected void onRegisterAddToAncestorDescendants( AttributeType newType, AttributeType ancestor ) 
        throws NamingException
    {
        if ( ancestor == null )
        {
            return;
        }
        
        // Get the ancestor's descendant, if any
        Set<AttributeType> descendants = oidToDescendantSet.get( ancestor.getOid() );

        // Initialize the descendant Set to store the descendants for the attributeType
        if ( descendants == null )
        {
            descendants = new HashSet<AttributeType>( 1 );
            oidToDescendantSet.put( ancestor.getOid(), descendants );
        }
        
        // Add the current type as a descendant
        descendants.add( newType );
        
        // And recurse until we reach the top of the hierarchy
        onRegisterAddToAncestorDescendants( newType, ancestor.getSuperior() );
    }
    

    /**
     * {@inheritDoc}
     */
    public AttributeType lookup( String id ) throws NamingException
    {
        String oid = oidRegistry.getOid( id );
        AttributeType attributeType = byOidAT.get( oid );

        if ( attributeType == null )
        {
            String msg = "attributeType w/ OID " + oid + " not registered!";
            LOG.error( msg );
            throw new NoSuchAttributeException( msg );
        }

        if ( IS_DEBUG )
        {
            LOG.debug( "lookup with id '{}' for attributeType: {}", oid, attributeType );
        }
        
        return attributeType;
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasAttributeType( String id )
    {
        try
        {
            String oid = oidRegistry.getOid( id );
            
            if ( oid != null )
            {
                return byOidAT.containsKey( oid );
            }
            
            return false;
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
        AttributeType at = byOidAT.get( oidRegistry.getOid( id ) );
        
        if ( at != null )
        {
            return at.getSchema();
        }

        String msg = "OID " + id + " not found in oid to " + "AttributeType map!";
        LOG.debug( msg );
        throw new NamingException( msg );
    }


    /**
     * Remove the AttributeType normalizer from the OidNormalizer map 
     */
    private void removeMappingFor( AttributeType type ) throws NamingException
    {
        if ( type == null )
        {
            return;
        }
        
        oidNormalizerMap.remove( type.getOid() );

        // We also have to remove all the short names for this attribute
        String[] aliases = type.getNamesRef();
        
        for ( String aliase : aliases )
        {
            oidNormalizerMap.remove( aliase );
            oidNormalizerMap.remove( aliase.toLowerCase() );
        }
    }


    /**
     * Add a new Oid/Normalizer couple in the OidNormalizer map
     */
    private void addMappingFor( AttributeType type ) throws NamingException
    {
        MatchingRule matchingRule = type.getEquality();
        OidNormalizer oidNormalizer;
        String oid = type.getOid();

        if ( matchingRule == null )
        {
            LOG.debug( "Attribute {} does not have normalizer : using NoopNormalizer", type.getName() );
            oidNormalizer = new OidNormalizer( oid, new NoOpNormalizer() );
        }
        else
        {
            oidNormalizer = new OidNormalizer( oid, matchingRule.getNormalizer() );
        }

        oidNormalizerMap.put( oid, oidNormalizer );
        
        // Also inject the attributeType's short nampes in the map
        String[] aliases = type.getNamesRef();
        
        for ( String aliase : aliases )
        {
            oidNormalizerMap.put( aliase, oidNormalizer );
            oidNormalizerMap.put( aliase.toLowerCase(), oidNormalizer );
        }
    }

    
    /**
     * {@inheritDoc}
     */
    public Map<String,OidNormalizer> getNormalizerMapping() throws NamingException
    {
        return Collections.unmodifiableMap( oidNormalizerMap );
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<AttributeType> descendants( String ancestorId ) throws NamingException
    {
        String oid = oidRegistry.getOid( ancestorId );
        Set<AttributeType> descendants = oidToDescendantSet.get( oid );
        
        if ( descendants == null )
        {
            return Collections.EMPTY_SET.iterator();
        }
        
        return descendants.iterator();
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasDescendants( String ancestorId ) throws NamingException
    {
        String oid = oidRegistry.getOid( ancestorId );
        Set<AttributeType> descendants = oidToDescendantSet.get( oid );
        return (descendants != null) && !descendants.isEmpty();
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<AttributeType> iterator()
    {
        return byOidAT.values().iterator();
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void unregister( String numericOid ) throws NamingException
    {
        if ( ! OID.isOID( numericOid ) )
        {
            String msg = "Looks like the arg " + numericOid + " is not a numeric OID";
            LOG.error(msg );
            throw new NamingException( msg );
        }

        removeMappingFor( byOidAT.get( numericOid ));
        byOidAT.remove( numericOid );
        oidToDescendantSet.remove( numericOid );
    }
    
    
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        for ( String value:byOidAT.keySet() )
        {
            sb.append( value ).append( ":" ).append( byOidAT.get( value ) ).append( '\n' );
        }
        
        return sb.toString();
    }
}
