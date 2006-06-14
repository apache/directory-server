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
package org.apache.directory.server.core.schema.global;


import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.directory.server.core.schema.AttributeTypeRegistry;
import org.apache.directory.server.core.schema.OidRegistry;
import org.apache.directory.server.core.schema.bootstrap.BootstrapAttributeTypeRegistry;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.OidNormalizer;
import org.apache.directory.shared.ldap.util.JoinIterator;


/**
 * A plain old java object implementation of an AttributeTypeRegistry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class GlobalAttributeTypeRegistry implements AttributeTypeRegistry
{
    /** maps an OID to an AttributeType */
    private final Map byOid;
    /** maps an OID to a schema name*/
    private final Map oidToSchema;
    /** the registry used to resolve names to OIDs */
    private final OidRegistry oidRegistry;
    /** the underlying bootstrap registry to delegate on misses to */
    private BootstrapAttributeTypeRegistry bootstrap;
    /** cached normalizer mapping */
    private transient Map mapping;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    /**
     * Creates a GlobalAttributeTypeRegistry which accesses data stored within
     * the system partition and within the bootstrapping registry to service
     * AttributeType lookup requests.
     *
     * @param bootstrap the bootstrapping registry to delegate to
     */
    public GlobalAttributeTypeRegistry(BootstrapAttributeTypeRegistry bootstrap, OidRegistry oidRegistry)
    {
        this.byOid = new HashMap();
        this.oidToSchema = new HashMap();

        this.oidRegistry = oidRegistry;
        if ( this.oidRegistry == null )
        {
            throw new NullPointerException( "the OID registry cannot be null" );
        }

        this.bootstrap = bootstrap;
        if ( this.bootstrap == null )
        {
            throw new NullPointerException( "the bootstrap registry cannot be null" );
        }
    }


    // ------------------------------------------------------------------------
    // Service Methods
    // ------------------------------------------------------------------------

    public void register( String schema, AttributeType attributeType ) throws NamingException
    {
        if ( byOid.containsKey( attributeType.getOid() ) || bootstrap.hasAttributeType( attributeType.getOid() ) )
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

        oidToSchema.put( attributeType.getOid(), schema );
        byOid.put( attributeType.getOid(), attributeType );
        
        // update the cached normalizer mapping
        if ( mapping == null )
        {
            Map bootstrapMapping = bootstrap.getNormalizerMapping();
            mapping = new HashMap( bootstrapMapping.size() );
            mapping.putAll( bootstrapMapping );
        }
        
        Normalizer normalizer = attributeType.getEquality().getNormalizer();
        OidNormalizer oidNormalizer = new OidNormalizer( attributeType.getOid(), normalizer );
        mapping.put( attributeType.getOid(), oidNormalizer );
        String[] aliases = attributeType.getNames();
        for ( int jj = 0; jj < aliases.length; jj++ )
        {
            mapping.put( aliases[jj], oidNormalizer );
            mapping.put( aliases[jj].toLowerCase(), oidNormalizer );
        }
    }


    public AttributeType lookup( String id ) throws NamingException
    {
        id = oidRegistry.getOid( id );

        if ( !( byOid.containsKey( id ) || bootstrap.hasAttributeType( id ) ) )
        {
            NamingException e = new NamingException( "attributeType w/ OID " + id + " not registered!" );
            throw e;
        }

        AttributeType attributeType = ( AttributeType ) byOid.get( id );

        if ( attributeType == null )
        {
            attributeType = bootstrap.lookup( id );
        }

        return attributeType;
    }


    public boolean hasAttributeType( String id )
    {
        try
        {
            if ( oidRegistry.hasOid( id ) )
            {
                return byOid.containsKey( oidRegistry.getOid( id ) ) || bootstrap.hasAttributeType( id );
            }
        }
        catch ( NamingException e )
        {
            return false;
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

        if ( bootstrap.getSchemaName( id ) != null )
        {
            return bootstrap.getSchemaName( id );
        }

        throw new NamingException( "OID " + id + " not found in oid to " + "schema name map!" );
    }


    public Iterator list()
    {
        return new JoinIterator( new Iterator[]
            { byOid.values().iterator(), bootstrap.list() } );
    }
    
    
    public Map getNormalizerMapping() throws NamingException
    {
        if ( mapping == null )
        {
            Map bootstrapMapping = bootstrap.getNormalizerMapping();
            mapping = new HashMap( ( byOid.size() << 1 ) + bootstrapMapping.size() );
            mapping.putAll( bootstrapMapping );
            for ( Iterator ii = byOid.values().iterator(); ii.hasNext(); /**/ )
            {
                AttributeType type = ( AttributeType ) ii.next();
                OidNormalizer oidNormalizer = new OidNormalizer( type.getOid(), type.getEquality().getNormalizer() );
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
}
