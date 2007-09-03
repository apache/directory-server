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

package org.apache.directory.server.core.trigger;


import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.filter.AssertionEnum;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.SimpleNode;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.NormalizerMappingResolver;
import org.apache.directory.shared.ldap.trigger.TriggerSpecification;
import org.apache.directory.shared.ldap.trigger.TriggerSpecificationParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A cache for Trigger Specifications which responds to specific events to
 * perform cache house keeping as trigger subentries are added, deleted
 * and modified.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$
 */
public class TriggerSpecCache
{
    /** the attribute id for prescriptive trigger: prescriptiveTrigger */
    private static final String PRESCRIPTIVE_TRIGGER_ATTR = "prescriptiveTriggerSpecification";

    /** the logger for this class */
    private static final Logger log = LoggerFactory.getLogger( TriggerSpecCache.class );

    /** cloned startup environment properties we use for subentry searching */
    private final Hashtable env;
    /** a map of strings to TriggerSpecification collections */
    private final Map<String, List<TriggerSpecification>> triggerSpecs = new HashMap<String, List<TriggerSpecification>>();
    /** a handle on the partition nexus */
    private final PartitionNexus nexus;
    /** a normalizing TriggerSpecification parser */
    private final TriggerSpecificationParser triggerSpecParser;
    private AttributeTypeRegistry attrRegistry;


    /**
     * Creates a TriggerSpecification cache.
     *
     * @param dirServCfg the context factory configuration for the server
     */
    public TriggerSpecCache( DirectoryServiceConfiguration dirServCfg ) throws NamingException
    {
        this.nexus = dirServCfg.getPartitionNexus();
        attrRegistry = dirServCfg.getRegistries().getAttributeTypeRegistry();
        final AttributeTypeRegistry registry = dirServCfg.getRegistries().getAttributeTypeRegistry();
        triggerSpecParser = new TriggerSpecificationParser( new NormalizerMappingResolver()
            {
                public Map getNormalizerMapping() throws NamingException
                {
                    return registry.getNormalizerMapping();
                }
            });
        env = ( Hashtable ) dirServCfg.getEnvironment().clone();
        initialize();
    }


    private void initialize() throws NamingException
    {
        // search all naming contexts for trigger subentenries
        // generate TriggerSpecification arrays for each subentry
        // add that subentry to the hash
        Iterator suffixes = nexus.listSuffixes( null );
        
        while ( suffixes.hasNext() )
        {
            String suffix = ( String ) suffixes.next();
            LdapDN baseDn = new LdapDN( suffix );
            ExprNode filter = new SimpleNode( SchemaConstants.OBJECT_CLASS_AT, ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRY_OC, AssertionEnum.EQUALITY );
            SearchControls ctls = new SearchControls();
            ctls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            NamingEnumeration results = 
                nexus.search( 
                    new SearchOperationContext( baseDn, env, filter, ctls ) );
            
            while ( results.hasMore() )
            {
                SearchResult result = ( SearchResult ) results.next();
                String subentryDn = result.getName();
                Attribute triggerSpec = result.getAttributes().get( PRESCRIPTIVE_TRIGGER_ATTR );
                
                if ( triggerSpec == null )
                {
                    log.warn( "Found triggerExecutionSubentry '" + subentryDn + "' without any " + PRESCRIPTIVE_TRIGGER_ATTR );
                    continue;
                }

                LdapDN normSubentryName = new LdapDN( subentryDn );
                normSubentryName.normalize( attrRegistry.getNormalizerMapping() );
                subentryAdded( normSubentryName, result.getAttributes() );
            }
            
            results.close();
        }
    }


    private boolean hasPrescriptiveTrigger( Attributes entry ) throws NamingException
    {
        // only do something if the entry contains prescriptiveTrigger
        Attribute triggerSpec = entry.get( PRESCRIPTIVE_TRIGGER_ATTR );        
        if ( triggerSpec == null )
        {
            return false;
        }
        return true;
    }


    public void subentryAdded( LdapDN normName, Attributes entry ) throws NamingException
    {
        // only do something if the entry contains prescriptiveTrigger
        Attribute triggerSpec = entry.get( PRESCRIPTIVE_TRIGGER_ATTR );
        
        if ( triggerSpec == null )
        {
            return;
        }
        
        List<TriggerSpecification> subentryTriggerSpecs = new ArrayList<TriggerSpecification>();
        
        for ( int ii = 0; ii < triggerSpec.size(); ii++ )
        {
            TriggerSpecification item = null;

            try
            {
                item = triggerSpecParser.parse( ( String ) triggerSpec.get( ii ) );
                subentryTriggerSpecs.add( item );
            }
            catch ( ParseException e )
            {
                String msg = "TriggerSpecification parser failure on '" + item + "'. Cannnot add Trigger Specificaitons to TriggerSpecCache.";
                log.error( msg, e );
            }
            
        }
        
        triggerSpecs.put( normName.toString(), subentryTriggerSpecs );
    }


    public void subentryDeleted( LdapDN normName, Attributes entry ) throws NamingException
    {
        if ( !hasPrescriptiveTrigger( entry ) )
        {
            return;
        }

        triggerSpecs.remove( normName.toString() );
    }


    public void subentryModified( ModifyOperationContext opContext, Attributes entry ) throws NamingException
    {
        if ( !hasPrescriptiveTrigger( entry ) )
        {
            return;
        }

        LdapDN normName = opContext.getDn();
        ModificationItemImpl[] mods = opContext.getModItems();

        boolean isTriggerSpecModified = false;
        
        for ( int ii = 0; ii < mods.length; ii++ )
        {
            isTriggerSpecModified |= mods[ii].getAttribute().contains( PRESCRIPTIVE_TRIGGER_ATTR );
        }
        
        if ( isTriggerSpecModified )
        {
            subentryDeleted( normName, entry );
            subentryAdded( normName, entry );
        }
    }


    public List<TriggerSpecification> getSubentryTriggerSpecs( String subentryDn )
    {
        List<TriggerSpecification> subentryTriggerSpecs = triggerSpecs.get( subentryDn );
        if ( subentryTriggerSpecs == null )
        {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList( subentryTriggerSpecs );
    }


    public void subentryRenamed( LdapDN oldName, LdapDN newName )
    {
        triggerSpecs.put( newName.toString(), triggerSpecs.remove( oldName.toString() ) );
    }
}
