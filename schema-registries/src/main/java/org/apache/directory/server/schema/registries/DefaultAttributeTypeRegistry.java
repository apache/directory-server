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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.NoOpNormalizer;
import org.apache.directory.shared.ldap.schema.OidNormalizer;
import org.apache.directory.shared.ldap.util.StringTools;

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
    private final Map<String,AttributeType> byOid;
    /** maps OIDs to a Set of descendants for that OID */
    private final Map<String,Set<AttributeType>> oidToDescendantSet;
    /** the registry used to resolve names to OIDs */
    private final OidRegistry oidRegistry;
    /** cached normalizer mapping */
    private transient Map<String, OidNormalizer> mapping;
    

    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    /**
     * Creates an empty BootstrapAttributeTypeRegistry.
     * @param oidRegistry a numeric object identifier registry
     */
    public DefaultAttributeTypeRegistry( OidRegistry oidRegistry )
    {
        this.byOid = new HashMap<String,AttributeType>();
        this.oidToDescendantSet= new HashMap<String,Set<AttributeType>>();
        this.oidRegistry = oidRegistry;
    }


    // ------------------------------------------------------------------------
    // Service Methods
    // ------------------------------------------------------------------------

    
    public void register( AttributeType attributeType ) throws NamingException
    {
        if ( byOid.containsKey( attributeType.getOid() ) )
        {
            throw new NamingException( "attributeType w/ OID " + attributeType.getOid()
                + " has already been registered!" );
        }

        String[] names = attributeType.getNames();
        for ( String name : names )
        {
            oidRegistry.register( name, attributeType.getOid() );
        }
        oidRegistry.register( attributeType.getOid(), attributeType.getOid() );

        registerDescendants( attributeType );
        byOid.put( attributeType.getOid(), attributeType );
        
        if ( IS_DEBUG )
        {
            LOG.debug( "registed attributeType: " + attributeType );
        }
    }


    public Set<String> getBinaryAttributes() throws NamingException
    {
        Set<String> binaries = new HashSet<String>();
        Iterator<AttributeType> list = iterator();
        while ( list.hasNext() )
        {
            AttributeType type = list.next();

            if ( ! type.getSyntax().isHumanReadable() )
            {
                // add the OID for the attributeType
                binaries.add( type.getOid() );

                // add the lowercased name for the names for the attributeType
                String[] names = type.getNames();

                for ( String name : names )
                {
                    // @TODO do we really need to lowercase strings here?
                    binaries.add( StringTools.lowerCaseAscii( StringTools.trim( name ) ) );
                }
            }
        }

        return binaries;
    }


    public void registerDescendants( AttributeType attributeType ) throws NamingException
    {
        // add/create the descendent set for this attribute
        oidToDescendantSet.put( attributeType.getOid(), new HashSet<AttributeType>( 5 ) );
        
        // add this attribute to descendant list of other attributes in superior chain
        onRegisterAddToAncestorDescendants( attributeType, attributeType.getSuperior() );
    }
    
    
    /**
     * Recursively adds a new attributeType to the descendant's list of all ancestors
     * until top is reached.  Top will not have the new type added.
     * 
     * @param newType the new attributeType being added
     * @param ancestor some anscestor from superior up to and including top
     * @throws NamingException if there are resolution failures
     */
    protected void onRegisterAddToAncestorDescendants( AttributeType newType, AttributeType ancestor ) 
        throws NamingException
    {
        if ( ancestor == null )
        {
            return;
        }
        
        if ( ancestor.getName() != null && ancestor.getName().equals( SchemaConstants.TOP_OC ) )
        {
            return;
        }
        
        Set<AttributeType> descendants = oidToDescendantSet.get( ancestor.getOid() );
        if ( descendants == null )
        {
            descendants = new HashSet<AttributeType>( 5 );
            oidToDescendantSet.put( ancestor.getOid(), descendants );
        }
        descendants.add( newType );
        onRegisterAddToAncestorDescendants( newType, ancestor.getSuperior() );
    }
    

    public AttributeType lookup( String id ) throws NamingException
    {
        id = oidRegistry.getOid( id );

        if ( !byOid.containsKey( id ) )
        {
            throw new NamingException( "attributeType w/ OID " + id + " not registered!" );
        }

        AttributeType attributeType = byOid.get( id );
        
        if ( IS_DEBUG )
        {
            LOG.debug( "lookup with id" + id + "' of attributeType: " + attributeType );
        }
        
        return attributeType;
    }


    public boolean hasAttributeType( String id )
    {
        if ( oidRegistry.hasOid( id ) )
        {
            try
            {
                return byOid.containsKey( oidRegistry.getOid( id ) );
            }
            catch ( NamingException e )
            {
                return false;
            }
        }

        return false;
    }


    public String getSchemaName( String id ) throws NamingException
    {
        id = oidRegistry.getOid( id );
        AttributeType at = byOid.get( id );
        
        if ( at != null )
        {
            return at.getSchema();
        }

        throw new NamingException( "OID " + id + " not found in oid to " + "AttributeType map!" );
    }


    public Iterator list()
    {
        return byOid.values().iterator();
    }
    
    
    public Map<String,OidNormalizer> getNormalizerMapping() throws NamingException
    {
        if ( mapping == null )
        {
            mapping = new HashMap<String,OidNormalizer>( byOid.size() << 1 );
            for ( AttributeType type : byOid.values() )
            {
                MatchingRule matchingRule = type.getEquality();
                OidNormalizer oidNormalizer;

                if ( matchingRule == null )
                {
                    LOG.debug( "Attribute " + type.getName() + " does not have normalizer : using NoopNormalizer" );
                    oidNormalizer = new OidNormalizer( type.getOid(), new NoOpNormalizer() );
                }
                else
                {
                    oidNormalizer = new OidNormalizer( type.getOid(), matchingRule.getNormalizer() );
                }

                mapping.put( type.getOid(), oidNormalizer );
                String[] aliases = type.getNames();
                for ( String aliase : aliases )
                {
                    mapping.put( aliase, oidNormalizer );
                    mapping.put( aliase.toLowerCase(), oidNormalizer );
                }
            }
        }
        
        return Collections.unmodifiableMap( mapping );
    }


    public Iterator<AttributeType> descendants( String ancestorId ) throws NamingException
    {
        String oid = oidRegistry.getOid( ancestorId );
        Set<AttributeType> descendants = oidToDescendantSet.get( oid );
        if ( descendants == null )
        {
            //noinspection unchecked
            return Collections.EMPTY_SET.iterator();
        }
        return descendants.iterator();
    }


    public boolean hasDescendants( String ancestorId ) throws NamingException
    {
        String oid = oidRegistry.getOid( ancestorId );
        Set descendants = oidToDescendantSet.get( oid );
        return descendants != null && !descendants.isEmpty();
    }


    public Iterator<AttributeType> iterator()
    {
        return byOid.values().iterator();
    }
    
    
    public void unregister( String numericOid ) throws NamingException
    {
        if ( ! Character.isDigit( numericOid.charAt( 0 ) ) )
        {
            throw new NamingException( "Looks like the arg is not a numeric OID" );
        }

        byOid.remove( numericOid );
        oidToDescendantSet.remove( numericOid );
    }
}
