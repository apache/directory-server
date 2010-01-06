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
package org.apache.directory.shared.ldap.schema.registries;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.schema.DITStructureRule;
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.schema.SchemaObjectType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A DITStructureRule registry's service default implementation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 828111 $
 */
public class DefaultDITStructureRuleRegistry extends DefaultSchemaObjectRegistry<DITStructureRule>
    implements DITStructureRuleRegistry
{
    /** static class logger */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultDITStructureRuleRegistry.class );

    /** A speedup for debug */
    private static final boolean DEBUG = LOG.isDebugEnabled();
    
    /** a map of DITStructureRule looked up by RuleId */
    protected Map<Integer, DITStructureRule> byRuleId;
    
    /**
     * Creates a new default NormalizerRegistry instance.
     */
    public DefaultDITStructureRuleRegistry()
    {
        super( SchemaObjectType.DIT_STRUCTURE_RULE, new OidRegistry() );
        byRuleId = new HashMap<Integer, DITStructureRule>();
    }


    /**
     * {@inheritDoc}
     */
    public boolean contains( int ruleId )
    {
        return byRuleId.containsKey( ruleId );
    }

    
    /**
     * {@inheritDoc}
     */
    public Iterator<DITStructureRule> iterator()
    {
        return byRuleId.values().iterator();
    }
    
    
    /**
     * {@inheritDoc}
     */
    public Iterator<Integer> ruleIdIterator()
    {
        return byRuleId.keySet().iterator();
    }
    
    
    /**
     * {@inheritDoc}
     */
    public String getSchemaName( int ruleId ) throws NamingException
    {
        DITStructureRule ditStructureRule = byRuleId.get( ruleId );

        if ( ditStructureRule != null )
        {
            return ditStructureRule.getSchemaName();
        }
        
        String msg = "RuleId " + ruleId + " not found in ruleId to schema name map!";
        LOG.warn( msg );
        throw new NamingException( msg );
    }

    
    /**
     * {@inheritDoc}
     */
    public void register( DITStructureRule ditStructureRule ) throws NamingException
    {
        int ruleId = ditStructureRule.getRuleId();
        
        if ( byRuleId.containsKey( ruleId ) )
        {
            String msg = "DITStructureRule with RuleId " + ruleId + " already registered!";
            LOG.warn( msg );
            throw new NamingException( msg );
        }

        byRuleId.put( ruleId, ditStructureRule );
        
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "registered {} for OID {}", ditStructureRule, ruleId );
        }
    }

    
    /**
     * {@inheritDoc}
     */
    public DITStructureRule lookup( int ruleId ) throws NamingException
    {
        DITStructureRule ditStructureRule = byRuleId.get( ruleId );

        if ( ditStructureRule == null )
        {
            String msg = "DITStructureRule for ruleId " + ruleId + " does not exist!";
            LOG.debug( msg );
            throw new NamingException( msg );
        }

        if ( DEBUG )
        {
            LOG.debug( "Found {} with ruleId: {}", ditStructureRule, ruleId );
        }
        
        return ditStructureRule;
    }


    /**
     * {@inheritDoc}
     */
    public void unregister( int ruleId ) throws NamingException
    {
        DITStructureRule ditStructureRule = byRuleId.remove( ruleId );
        
        if ( DEBUG )
        {
            LOG.debug( "Removed {} with ruleId {} from the registry", ditStructureRule, ruleId );
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void unregisterSchemaElements( String schemaName )
    {
        if ( schemaName == null )
        {
            return;
        }
        
        // Loop on all the SchemaObjects stored and remove those associated
        // with the give schemaName
        for ( DITStructureRule ditStructureRule : this )
        {
            if ( schemaName.equalsIgnoreCase( ditStructureRule.getSchemaName() ) )
            {
                int ruleId = ditStructureRule.getRuleId();
                SchemaObject removed = byRuleId.remove( ruleId );
                
                if ( DEBUG )
                {
                    LOG.debug( "Removed {} with ruleId {} from the registry", removed, ruleId );
                }
            }
        }
    }

    
    /**
     * {@inheritDoc}
     */
    public void renameSchema( String originalSchemaName, String newSchemaName )
    {
        // Loop on all the SchemaObjects stored and remove those associated
        // with the give schemaName
        for ( DITStructureRule ditStructureRule : this )
        {
            if ( originalSchemaName.equalsIgnoreCase( ditStructureRule.getSchemaName() ) )
            {
                ditStructureRule.setSchemaName( newSchemaName );

                if ( DEBUG )
                {
                    LOG.debug( "Renamed {} schemaName to {}", ditStructureRule, newSchemaName );
                }
            }
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    public DefaultDITStructureRuleRegistry copy()
    {
        DefaultDITStructureRuleRegistry copy = new DefaultDITStructureRuleRegistry();
        
        // Copy the base data
        copy.copy( this );
        
        return copy;
    }
}
