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


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.schema.DITStructureRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A plain old java object implementation of an DITStructureRuleRegistry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultDitStructureRuleRegistry implements DITStructureRuleRegistry
{
    /** static class logger */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultDitStructureRuleRegistry.class );
    /** maps an OID to an DITStructureRule */
    private final Map<String,DITStructureRule> byOid;
    /** maps an OID to an DITStructureRule */
    private final Map<Integer,DITStructureRule> byRuleId;
    /** the registry used to resolve names to OIDs */
    private final OidRegistry oidRegistry;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    
    /**
     * Creates an empty DefaultDitStructureRuleRegistry.
     *
     * @param oidRegistry used by this registry for OID to name resolution of
     * dependencies and to automatically register and unregister it's aliases and OIDs
     */
    public DefaultDitStructureRuleRegistry( OidRegistry oidRegistry )
    {
        this.byRuleId = new HashMap<Integer,DITStructureRule>();
        this.byOid = new HashMap<String,DITStructureRule>();
        this.oidRegistry = oidRegistry;
    }


    // ------------------------------------------------------------------------
    // Service Methods
    // ------------------------------------------------------------------------

    public void register( DITStructureRule dITStructureRule ) throws NamingException
    {
        if ( byOid.containsKey( dITStructureRule.getOid() ) )
        {
            throw new NamingException( "dITStructureRule w/ OID " + dITStructureRule.getOid()
                + " has already been registered!" );
        }

        oidRegistry.register( dITStructureRule.getName(), dITStructureRule.getOid() );
        byOid.put( dITStructureRule.getOid(), dITStructureRule );
        byRuleId.put( dITStructureRule.getRuleId(), dITStructureRule );
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "registered dITStructureRule: " + dITStructureRule );
        }
    }


    public DITStructureRule lookup( String id ) throws NamingException
    {
        id = oidRegistry.getOid( id );

        if ( !byOid.containsKey( id ) )
        {
            throw new NamingException( "dITStructureRule w/ OID " + id + " not registered!" );
        }

        DITStructureRule dITStructureRule = byOid.get( id );
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "lookup with id '" + id + "' for dITStructureRule: " + dITStructureRule );
        }
        return dITStructureRule;
    }


    public boolean hasDITStructureRule( String id )
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
        DITStructureRule dsr = byOid.get( id );
        if ( dsr != null )
        {
            return dsr.getSchema();
        }

        throw new NamingException( "OID " + id + " not found in oid to " + "DITStructureRule map!" );
    }


    public String getSchemaName( Integer ruleId ) throws NamingException
    {
        DITStructureRule dsr = byRuleId.get( ruleId );
        if ( dsr != null )
        {
            return dsr.getSchema();
        }

        throw new NamingException( "A DitStructureRule with ruleId " + ruleId 
            + " not found in the DITStructureRule map!" );
    }


    public Iterator<DITStructureRule> iterator()
    {
        return byOid.values().iterator();
    }

    
    public void unregister( Integer ruleId ) throws NamingException
    {
        DITStructureRule dsr = byRuleId.remove( ruleId );
        
        if ( dsr == null )
        {
            if ( dsr == null )
            {
                throw new LdapNamingException(
                    "No such DITStructureRule for rule identifier: " + ruleId,
                    ResultCodeEnum.OTHER );
            }
        }
        
        byOid.remove( dsr.getOid() );
    }
    
    
    public void unregister( String numericOid ) throws NamingException
    {
        if ( ! Character.isDigit( numericOid.charAt( 0 ) ) )
        {
            throw new NamingException( "Looks like the arg is not a numeric OID" );
        }

        DITStructureRule dsr = byOid.remove( numericOid );
        byRuleId.remove( dsr.getRuleId() );
    }


    public boolean hasDITStructureRule( Integer ruleId )
    {
        DITStructureRule dsr = byRuleId.get( ruleId );
        return dsr != null;
    }


    public DITStructureRule lookup( Integer ruleId ) throws NamingException
    {
        DITStructureRule dsr = byRuleId.get( ruleId );
        
        if ( dsr == null )
        {
            throw new LdapNamingException(
                "No such DITStructureRule for rule identifier: " + ruleId,
                ResultCodeEnum.OTHER );
        }

        return dsr;
    }
}
