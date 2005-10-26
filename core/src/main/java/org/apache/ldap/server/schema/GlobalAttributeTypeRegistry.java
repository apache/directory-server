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


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.ldap.common.schema.AttributeType;
import org.apache.ldap.common.util.JoinIterator;
import org.apache.ldap.server.schema.bootstrap.BootstrapAttributeTypeRegistry;


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
    /** monitor notified via callback events */
    private AttributeTypeRegistryMonitor monitor;
    /** the underlying bootstrap registry to delegate on misses to */
    private BootstrapAttributeTypeRegistry bootstrap;


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
    public GlobalAttributeTypeRegistry( BootstrapAttributeTypeRegistry bootstrap, OidRegistry oidRegistry )
    {
        this.byOid = new HashMap();
        this.oidToSchema = new HashMap();
        this.monitor = new AttributeTypeRegistryMonitorAdapter();

        this.oidRegistry = oidRegistry;
        if ( this.oidRegistry == null )
        {
            throw new NullPointerException( "the OID registry cannot be null" ) ;
        }

        this.bootstrap = bootstrap;
        if ( this.bootstrap == null )
        {
            throw new NullPointerException( "the bootstrap registry cannot be null" ) ;
        }
    }


    /**
     * Sets the monitor that is to be notified via callback events.
     *
     * @param monitor the new monitor to notify of notable events
     */
    public void setMonitor( AttributeTypeRegistryMonitor monitor )
    {
        this.monitor = monitor;
    }


    // ------------------------------------------------------------------------
    // Service Methods
    // ------------------------------------------------------------------------


    public void register( String schema, AttributeType attributeType ) throws NamingException
    {
        if ( byOid.containsKey( attributeType.getOid() ) ||
             bootstrap.hasAttributeType( attributeType.getOid() ) )
        {
            NamingException e = new NamingException( "attributeType w/ OID " +
                attributeType.getOid() + " has already been registered!" );
            monitor.registerFailed( attributeType, e );
            throw e;
        }

        String[] names = attributeType.getNames();
        for ( int ii = 0; ii < names.length; ii++ )
        {
            oidRegistry.register( names[ii], attributeType.getOid() );
        }

        oidToSchema.put( attributeType.getOid(), schema );
        byOid.put( attributeType.getOid(), attributeType );
        monitor.registered( attributeType );
    }


    public AttributeType lookup( String id ) throws NamingException
    {
        id = oidRegistry.getOid( id );

        if ( ! ( byOid.containsKey( id ) || bootstrap.hasAttributeType( id ) ) )
        {
            NamingException e = new NamingException( "attributeType w/ OID "
                + id + " not registered!" );
            monitor.lookupFailed( id, e );
            throw e;
        }

        AttributeType attributeType = ( AttributeType ) byOid.get( id );

        if ( attributeType == null )
        {
            attributeType = bootstrap.lookup( id );
        }

        monitor.lookedUp( attributeType );
        return attributeType;
    }


    public boolean hasAttributeType( String id )
    {
        try
        {
            if ( oidRegistry.hasOid( id ) )
            {
                return byOid.containsKey( oidRegistry.getOid( id ) ) ||
                       bootstrap.hasAttributeType( id );
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

        throw new NamingException( "OID " + id + " not found in oid to " +
            "schema name map!" );
    }


    public Iterator list()
    {
        return new JoinIterator( new Iterator[]
            { byOid.values().iterator(),bootstrap.list() } );
    }
}
