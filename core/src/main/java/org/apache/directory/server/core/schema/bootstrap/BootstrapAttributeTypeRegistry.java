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
package org.apache.directory.server.core.schema.bootstrap;


import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;

import org.apache.directory.server.core.schema.AttributeTypeRegistry;
import org.apache.directory.server.core.schema.OidRegistry;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.NoOpNormalizer;
import org.apache.directory.shared.ldap.schema.OidNormalizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A plain old java object implementation of an AttributeTypeRegistry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BootstrapAttributeTypeRegistry implements AttributeTypeRegistry
{
    /** static class logger */
    private final static Logger log = LoggerFactory.getLogger( BootstrapAttributeTypeRegistry.class );

    /** maps an OID to an AttributeType */
    private final Map byOid;
    /** maps an OID to a schema name*/
    private final Map oidToSchema;
    /** maps OIDs to a Set of descendants for that OID */
    private final Map oidToDescendantSet;
    /** the registry used to resolve names to OIDs */
    private final OidRegistry oidRegistry;
    /** cached normalizer mapping */
    private transient Map mapping;
    

    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    /**
     * Creates an empty BootstrapAttributeTypeRegistry.
     */
    public BootstrapAttributeTypeRegistry( OidRegistry oidRegistry )
    {
        this.byOid = new HashMap();
        this.oidToSchema = new HashMap();
        this.oidToDescendantSet= new HashMap();
        this.oidRegistry = oidRegistry;
    }


    // ------------------------------------------------------------------------
    // Service Methods
    // ------------------------------------------------------------------------

    
    public void register( String schema, AttributeType attributeType ) throws NamingException
    {
        if ( byOid.containsKey( attributeType.getOid() ) )
        {
            NamingException e = new NamingException( "attributeType w/ OID " + attributeType.getOid()
                + " has already been registered!" );
            throw e;
        }

        String[] names = attributeType.getNames();
        for ( int ii = 0; ii < names.length; ii++ )
        {
            oidRegistry.register( names[ii], attributeType.getOid() );
        }

        registerDescendants( attributeType );
        oidToSchema.put( attributeType.getOid(), schema );
        byOid.put( attributeType.getOid(), attributeType );
        if ( log.isDebugEnabled() )
        {
            log.debug( "registed attributeType: " + attributeType );
        }
    }

    
    public void registerDescendants( AttributeType attributeType ) throws NamingException
    {
        // add/create the descendent set for this attribute
        oidToDescendantSet.put( attributeType.getOid(), new HashSet( 5 ) );
        
        // add this attribute to descendant list of other attributes in superior chain
        onRegisterAddToAncestorDescendants( attributeType, attributeType.getSuperior() );
    }
    
    
    /**
     * Recursively adds a new attributeType to the descendant's list of all ancestors
     * until top is reached.  Top will not have the new type added.
     * 
     * @param newType the new attributeType being added
     * @param ancestor some anscestor from superior up to and including top
     * @throws NamingException
     */
    protected void onRegisterAddToAncestorDescendants( AttributeType newType, AttributeType ancestor ) 
        throws NamingException
    {
        if ( ancestor == null || ancestor.getName().equals( "top" ) )
        {
            return;
        }
        
        Set descendants = ( Set ) oidToDescendantSet.get( ancestor.getOid() );
        if ( descendants == null )
        {
            descendants = new HashSet( 5 );
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
            NamingException e = new NamingException( "attributeType w/ OID " + id + " not registered!" );
            throw e;
        }

        AttributeType attributeType = ( AttributeType ) byOid.get( id );
        if ( log.isDebugEnabled() )
        {
            log.debug( "lookup with id" + id + "' of attributeType: " + attributeType );
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
        if ( oidToSchema.containsKey( id ) )
        {
            return ( String ) oidToSchema.get( id );
        }

        throw new NamingException( "OID " + id + " not found in oid to " + "schema name map!" );
    }


    public Iterator list()
    {
        return byOid.values().iterator();
    }
    
    
    public Map getNormalizerMapping() throws NamingException
    {
        if ( mapping == null )
        {
            mapping = new HashMap( byOid.size() << 1 );
            for ( Iterator ii = byOid.values().iterator(); ii.hasNext(); /**/ )
            {
                AttributeType type = ( AttributeType ) ii.next();
                MatchingRule matchingRule = type.getEquality();
                OidNormalizer oidNormalizer = null;
                
                if ( matchingRule == null )
                {
                    log.warn( "Attribute " + type.getName() + " does not have normalizer : using NoopNormalizer" );
                    oidNormalizer = new OidNormalizer( type.getOid(), new NoOpNormalizer() );
                }
                else
                {
                    oidNormalizer = new OidNormalizer( type.getOid(), matchingRule.getNormalizer() );
                }
                
                mapping.put( type.getOid(), oidNormalizer );
                String[] aliases = type.getNames();
                for ( int jj = 0; jj < aliases.length; jj++ )
                {
                    mapping.put( aliases[jj], oidNormalizer );
                    mapping.put( aliases[jj].toLowerCase(), oidNormalizer );
                }
            }
        }
        
        return Collections.unmodifiableMap( mapping );
    }


    public Iterator descendants( String ancestorId ) throws NamingException
    {
        String oid = oidRegistry.getOid( ancestorId );
        Set descendants = ( Set ) oidToDescendantSet.get( oid );
        if ( descendants == null )
        {
            return Collections.EMPTY_SET.iterator();
        }
        return descendants.iterator();
    }
}
