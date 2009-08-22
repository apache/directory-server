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
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.registries.MatchingRuleRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A MatchingRuleRegistry service used to lookup matching rules by OID.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultMatchingRuleRegistry implements MatchingRuleRegistry
{
    /** static class logger */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultMatchingRuleRegistry.class );

    /** a map using an OID for the key and a MatchingRule for the value */
    private final Map<String,MatchingRule> byOidMatchingRule;
    
    /** the registry used to resolve names to OIDs */
    private final OidRegistry oidRegistry;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------
    /**
     * Creates a DefaultMatchingRuleRegistry using existing MatchingRulees
     * for lookups.
     *
     * @param oidRegistry used by this registry for OID to name resolution of
     * dependencies and to automatically register and unregister it's aliases and OIDs
     */
    public DefaultMatchingRuleRegistry( OidRegistry oidRegistry )
    {
        this.oidRegistry = oidRegistry;
        byOidMatchingRule = new ConcurrentHashMap<String,MatchingRule>();
    }


    // ------------------------------------------------------------------------
    // MatchingRuleRegistry interface methods
    // ------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public MatchingRule lookup( String id ) throws NamingException
    {
        String oid = oidRegistry.getOid( id );

        MatchingRule matchingRule = byOidMatchingRule.get( oid );
        
        if ( matchingRule != null )
        {
            if ( LOG.isDebugEnabled() )
            {
                LOG.debug( "lookup with id '{}' of matchingRule: {}", oid, matchingRule );
            }

            return matchingRule;
        }

        String msg = "Unknown MatchingRule OID " + oid;
        LOG.error( msg );
        throw new NamingException( msg );
    }


    /**
     * {@inheritDoc}
     */
    public void register( MatchingRule matchingRule ) throws NamingException
    {
        String oid = matchingRule.getOid();
        
        if ( byOidMatchingRule.containsKey( oid ) )
        {
            String msg = "matchingRule w/ OID " + oid
                + " has already been registered!";
            LOG.warn( msg );
            throw new NamingException( msg );
        }

        String[] names = matchingRule.getNamesRef();
        
        for ( String name : names )
        {
            oidRegistry.register( name, oid );
        }
        
        oidRegistry.register( oid, oid );
        byOidMatchingRule.put( oid, matchingRule );
        
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "registed matchingRule: {}", matchingRule);
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasMatchingRule( String id )
    {
        try
        {
            String oid = oidRegistry.getOid( id );
            
            if ( oid != null )
            {
                return byOidMatchingRule.containsKey( oid );
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
        MatchingRule mr = byOidMatchingRule.get( oid );
       
        if ( mr != null )
        {
            return mr.getSchema();
        }

        String msg = "OID " + oid + " not found in oid to " + "MatchingRule name map!";
        LOG.warn( msg );
        throw new NamingException( msg );
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<MatchingRule> iterator()
    {
        return byOidMatchingRule.values().iterator();
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

        byOidMatchingRule.remove( numericOid );
    }
}
