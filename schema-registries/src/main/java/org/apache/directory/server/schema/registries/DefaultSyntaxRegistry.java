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
import org.apache.directory.shared.ldap.schema.Syntax;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A SyntaxRegistry service available during server startup when other resources
 * like a syntax backing store is unavailable.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultSyntaxRegistry implements SyntaxRegistry
{
    /** static class logger */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultSyntaxRegistry.class );
    
    /** Speedup for DEBUG mode */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();
    
    /** a map of entries using an OID for the key and a Syntax for the value */
    private final Map<String,Syntax> byOidSyntax;
    
    /** the OID oidRegistry this oidRegistry uses to register new syntax OIDs */
    private final OidRegistry oidRegistry;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    /**
     * Creates a DefaultSyntaxRegistry.
     *
     * @param registry used by this registry for OID to name resolution of
     * dependencies and to automatically register and unregister it's aliases and OIDs
     */
    public DefaultSyntaxRegistry( OidRegistry registry )
    {
        this.oidRegistry = registry;
        byOidSyntax = new ConcurrentHashMap<String,Syntax>();
    }


    // ------------------------------------------------------------------------
    // SyntaxRegistry interface methods
    // ------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public Syntax lookup( String id ) throws NamingException
    {
        id = oidRegistry.getOid( id );
        Syntax syntax = byOidSyntax.get( id );

        if ( syntax != null )
        {
            if ( IS_DEBUG )
            {
                LOG.debug( "looked up using id '{}' : {}", id, syntax );
            }
            
            return syntax;
        }

        String msg = "Unknown syntax OID " + id;
        LOG.error( msg );
        throw new NamingException( msg );
    }


    /**
     * {@inheritDoc}
     */
    public void register( Syntax syntax ) throws NamingException
    {
        String oid = syntax.getOid();
        
        if ( byOidSyntax.containsKey( oid ) )
        {
            String msg = "syntax " + syntax + " w/ OID " + oid
                + " has already been registered!";
            LOG.warn( msg );
            throw new NamingException( msg );
        }

        // Register the new Syntax's oid/name relation into the global oidRegistry
        if ( syntax.getName() != null )
        {
            oidRegistry.register( syntax.getName(), oid );
        }

        // Also register the oid/oid relation
        oidRegistry.register( oid, oid );
        byOidSyntax.put( oid, syntax );
        
        if ( IS_DEBUG )
        {
            LOG.debug( "registered syntax: {}", syntax );
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasSyntax( String id )
    {
        try
        {
            String oid = oidRegistry.getOid( id );
            
            if ( oid != null )
            {
                return byOidSyntax.containsKey( oid );
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
        if ( ! OID.isOID( id ) )
        {
            String msg = "Looks like the arg is not a numeric OID";
            LOG.warn( msg );
            throw new NamingException( msg );
        }

        String oid = oidRegistry.getOid( id );
        Syntax syntax = byOidSyntax.get( oid );
       
        if ( syntax != null )
        {
            return syntax.getSchema();
        }

        String msg = "OID " + oid + " not found in oid to Syntax map!";
        LOG.error( msg );
        throw new NamingException( msg );
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<Syntax> iterator()
    {
        return byOidSyntax.values().iterator();
    }


    /**
     * {@inheritDoc}
     */
    public void unregister( String numericOid ) throws NamingException
    {
        if ( !OID.isOID(numericOid ) )
        {
            String msg = "Looks like the arg " + numericOid + " is not a numeric OID";
            LOG.error( msg );
            throw new NamingException( msg );
        }

        byOidSyntax.remove( numericOid );
    }
}
