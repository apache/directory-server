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
import org.apache.directory.shared.ldap.schema.DITContentRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A plain old java object implementation of an DITContentRuleRegistry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultDitContentRuleRegistry implements DITContentRuleRegistry
{
    /** static class logger */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultDitContentRuleRegistry.class );
    
    /** maps an OID to an DITContentRule */
    private final Map<String,DITContentRule> byOidDitContentRule;
    
    /** the registry used to resolve names to OIDs */
    private final OidRegistry oidRegistry;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    /**
     * Creates an empty DefaultDitContentRuleRegistry.
     *
     * @param oidRegistry used by this registry for OID to name resolution of
     * dependencies and to automatically register and unregister it's aliases and OIDs
     */
    public DefaultDitContentRuleRegistry( OidRegistry oidRegistry )
    {
        byOidDitContentRule = new ConcurrentHashMap<String,DITContentRule>();
        this.oidRegistry = oidRegistry;
    }


    // ------------------------------------------------------------------------
    // Service Methods
    // ------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public void register( DITContentRule dITContentRule ) throws NamingException
    {
        String oid = dITContentRule.getOid();
        
        if ( byOidDitContentRule.containsKey( oid ) )
        {
            String msg = "dITContentRule w/ OID " + oid
                + " has already been registered!";
            LOG.warn( msg );
            throw new NamingException( msg );
        }

        oidRegistry.register( dITContentRule.getName(), oid );
        byOidDitContentRule.put( oid, dITContentRule );
        
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "registed dITContentRule: {}", dITContentRule );
        }
    }


    /**
     * {@inheritDoc}
     */
    public DITContentRule lookup( String id ) throws NamingException
    {
        String oid = oidRegistry.getOid( id );

        DITContentRule dITContentRule = byOidDitContentRule.get( oid );

        if ( dITContentRule == null )
        {
            String msg = "dITContentRule w/ OID " + oid + " not registered!";
            LOG.debug( msg );
            throw new NamingException( msg );
        }

        
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "lookup with id '{}' of dITContentRule: {}", oid, dITContentRule );
        }
        
        return dITContentRule;
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasDITContentRule( String id )
    {
        try
        {
            String oid = oidRegistry.getOid( id );
            
            if ( oid != null )
            {
                return byOidDitContentRule.containsKey( oid );
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
        String oid = oidRegistry.getOid( id );
        DITContentRule dcr = byOidDitContentRule.get( oid );
        
        if ( dcr != null )
        {
            return dcr.getSchema();
        }

        String msg = "OID " + oid + " not found in oid to " + "DITContentRule map!";
        LOG.error( msg );
        throw new NamingException( msg );
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<DITContentRule> iterator()
    {
        return byOidDitContentRule.values().iterator();
    }


    /**
     * {@inheritDoc}
     */
    public void unregister( String numericOid ) throws NamingException
    {
        if ( !OID.isOID( numericOid ) )
        {
            String msg = "OID " + numericOid + " is not a numeric OID";
            LOG.error( msg );
            throw new NamingException( msg );
        }

        byOidDitContentRule.remove( numericOid );
    }
}
