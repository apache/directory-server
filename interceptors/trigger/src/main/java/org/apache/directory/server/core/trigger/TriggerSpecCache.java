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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.directory.SearchControls;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapOperationException;
import org.apache.directory.api.ldap.model.filter.EqualityNode;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.NormalizerMappingResolver;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.normalizers.OidNormalizer;
import org.apache.directory.api.ldap.trigger.TriggerSpecification;
import org.apache.directory.api.ldap.trigger.TriggerSpecificationParser;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.PartitionNexus;
import org.apache.directory.server.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A cache for Trigger Specifications which responds to specific events to
 * perform cache house keeping as trigger subentries are added, deleted
 * and modified.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class TriggerSpecCache
{
    /** the attribute id for prescriptive trigger: prescriptiveTrigger */
    private static final String PRESCRIPTIVE_TRIGGER_ATTR = "prescriptiveTriggerSpecification";

    /** the logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( TriggerSpecCache.class );

    /** a map of strings to TriggerSpecification collections */
    private final Map<Dn, List<TriggerSpecification>> triggerSpecs = new HashMap<>();

    /** a handle on the partition nexus */
    private final PartitionNexus nexus;

    /** a normalizing TriggerSpecification parser */
    private final TriggerSpecificationParser triggerSpecParser;


    /**
     * Creates a TriggerSpecification cache.
     *
     * @param directoryService the directory service core
     * @throws LdapException with problems initializing cache
     */
    public TriggerSpecCache( DirectoryService directoryService ) throws LdapException
    {
        this.nexus = directoryService.getPartitionNexus();
        final SchemaManager schemaManager = directoryService.getSchemaManager();

        triggerSpecParser = new TriggerSpecificationParser( new NormalizerMappingResolver()
        {
            @Override
            public Map<String, OidNormalizer> getNormalizerMapping() throws Exception
            {
                return schemaManager.getNormalizerMapping();
            }
        } );

        initialize( directoryService );
    }


    private void initialize( DirectoryService directoryService ) throws LdapException
    {
        // search all naming contexts for trigger subentenries
        // generate TriggerSpecification arrays for each subentry
        // add that subentry to the hash
        Set<String> suffixes = nexus.listSuffixes();

        AttributeType objectClassAt = directoryService.getSchemaManager().
            getAttributeType( SchemaConstants.OBJECT_CLASS_AT );

        for ( String suffix : suffixes )
        {
            Dn baseDn = directoryService.getDnFactory().create( suffix );
            ExprNode filter = new EqualityNode<String>( objectClassAt,
                new Value( objectClassAt, ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRY_OC ) );
            SearchControls ctls = new SearchControls();
            ctls.setSearchScope( SearchControls.SUBTREE_SCOPE );

            CoreSession adminSession = directoryService.getAdminSession();

            Partition partition = nexus.getPartition( baseDn ); 
            SearchOperationContext searchOperationContext = new SearchOperationContext( adminSession, baseDn,
                filter, ctls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.DEREF_ALWAYS );
            searchOperationContext.setPartition( partition );
            searchOperationContext.setTransaction( partition.beginReadTransaction() );

            EntryFilteringCursor results = nexus.search( searchOperationContext );

            try
            {
                while ( results.next() )
                {
                    Entry resultEntry = results.get();
                    Dn subentryDn = resultEntry.getDn();
                    Attribute triggerSpec = resultEntry.get( PRESCRIPTIVE_TRIGGER_ATTR );

                    if ( triggerSpec == null )
                    {
                        LOG.warn( "Found triggerExecutionSubentry '{}' without any {}", subentryDn, PRESCRIPTIVE_TRIGGER_ATTR );
                        continue;
                    }

                    Dn normSubentryDn = subentryDn;
                    
                    if ( !subentryDn.isSchemaAware() )
                    {
                        normSubentryDn = new Dn( directoryService.getSchemaManager(), subentryDn );
                    }
                    
                    subentryAdded( normSubentryDn, resultEntry );
                }

                results.close();
            }
            catch ( Exception e )
            {
                throw new LdapOperationException( e.getMessage(), e );
            }
        }
    }


    private boolean hasPrescriptiveTrigger( Entry entry )
    {
        // only do something if the entry contains prescriptiveTrigger
        Attribute triggerSpec = entry.get( PRESCRIPTIVE_TRIGGER_ATTR );

        return triggerSpec != null;
    }


    public void subentryAdded( Dn normName, Entry entry )
    {
        // only do something if the entry contains prescriptiveTrigger
        Attribute triggerSpec = entry.get( PRESCRIPTIVE_TRIGGER_ATTR );

        if ( triggerSpec == null )
        {
            return;
        }

        List<TriggerSpecification> subentryTriggerSpecs = new ArrayList<>();

        for ( Value value : triggerSpec )
        {
            TriggerSpecification item = null;

            try
            {
                item = triggerSpecParser.parse( value.getValue() );
                subentryTriggerSpecs.add( item );
            }
            catch ( ParseException e )
            {
                String msg = I18n.err( I18n.ERR_73, item );
                LOG.error( msg, e );
            }

        }

        triggerSpecs.put( normName, subentryTriggerSpecs );
    }


    public void subentryDeleted( Dn normName, Entry entry ) throws LdapException
    {
        if ( !hasPrescriptiveTrigger( entry ) )
        {
            return;
        }

        triggerSpecs.remove( normName );
    }


    public void subentryModified( ModifyOperationContext opContext, Entry entry ) throws LdapException
    {
        if ( !hasPrescriptiveTrigger( entry ) )
        {
            return;
        }

        Dn normName = opContext.getDn();
        List<Modification> mods = opContext.getModItems();

        boolean isTriggerSpecModified = false;

        for ( Modification mod : mods )
        {
            isTriggerSpecModified |= mod.getAttribute().contains( PRESCRIPTIVE_TRIGGER_ATTR );
        }

        if ( isTriggerSpecModified )
        {
            subentryDeleted( normName, entry );
            subentryAdded( normName, entry );
        }
    }


    public List<TriggerSpecification> getSubentryTriggerSpecs( Dn subentryDn )
    {
        List<TriggerSpecification> subentryTriggerSpecs = triggerSpecs.get( subentryDn );
        
        if ( subentryTriggerSpecs == null )
        {
            return Collections.emptyList();
        }
        
        return Collections.unmodifiableList( subentryTriggerSpecs );
    }


    public void subentryRenamed( Dn oldName, Dn newName )
    {
        triggerSpecs.put( newName, triggerSpecs.remove( oldName ) );
    }
}
