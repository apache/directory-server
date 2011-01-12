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
package org.apache.directory.server.core.subtree;


import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.naming.directory.SearchControls;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DefaultCoreSession;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.LdapPrincipal;
import org.apache.directory.server.core.admin.AdministrativePointInterceptor;
import org.apache.directory.server.core.administrative.AccessControlAAP;
import org.apache.directory.server.core.administrative.AccessControlAdministrativePoint;
import org.apache.directory.server.core.administrative.AccessControlIAP;
import org.apache.directory.server.core.administrative.AccessControlSAP;
import org.apache.directory.server.core.administrative.AccessControlSubentry;
import org.apache.directory.server.core.administrative.AdministrativePoint;
import org.apache.directory.server.core.administrative.AdministrativeRoleEnum;
import org.apache.directory.server.core.administrative.CollectiveAttributeAAP;
import org.apache.directory.server.core.administrative.CollectiveAttributeAdministrativePoint;
import org.apache.directory.server.core.administrative.CollectiveAttributeIAP;
import org.apache.directory.server.core.administrative.CollectiveAttributeSAP;
import org.apache.directory.server.core.administrative.CollectiveAttributeSubentry;
import org.apache.directory.server.core.administrative.SubSchemaSubentry;
import org.apache.directory.server.core.administrative.Subentry;
import org.apache.directory.server.core.administrative.SubentryCache;
import org.apache.directory.server.core.administrative.SubschemaAAP;
import org.apache.directory.server.core.administrative.SubschemaAdministrativePoint;
import org.apache.directory.server.core.administrative.SubschemaSAP;
import org.apache.directory.server.core.administrative.TriggerExecutionAAP;
import org.apache.directory.server.core.administrative.TriggerExecutionAdministrativePoint;
import org.apache.directory.server.core.administrative.TriggerExecutionIAP;
import org.apache.directory.server.core.administrative.TriggerExecutionSAP;
import org.apache.directory.server.core.administrative.TriggerExecutionSubentry;
import org.apache.directory.server.core.authn.AuthenticationInterceptor;
import org.apache.directory.server.core.authz.AciAuthorizationInterceptor;
import org.apache.directory.server.core.authz.DefaultAuthorizationInterceptor;
import org.apache.directory.server.core.collective.CollectiveAttributeInterceptor;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.event.EventInterceptor;
import org.apache.directory.server.core.exception.ExceptionInterceptor;
import org.apache.directory.server.core.filtering.EntryFilter;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchingOperationContext;
import org.apache.directory.server.core.normalization.NormalizationInterceptor;
import org.apache.directory.server.core.operational.OperationalAttributeInterceptor;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.core.schema.SchemaInterceptor;
import org.apache.directory.server.core.trigger.TriggerInterceptor;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.aci.ACIItem;
import org.apache.directory.shared.ldap.aci.ACIItemParser;
import org.apache.directory.shared.ldap.codec.search.controls.subentries.SubentriesControl;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.DefaultEntry;
import org.apache.directory.shared.ldap.entry.DefaultEntryAttribute;
import org.apache.directory.shared.ldap.entry.DefaultModification;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.StringValue;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapInvalidAttributeValueException;
import org.apache.directory.shared.ldap.exception.LdapOperationException;
import org.apache.directory.shared.ldap.exception.LdapUnwillingToPerformException;
import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.name.RDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.NormalizerMappingResolver;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.normalizers.ConcreteNameComponentNormalizer;
import org.apache.directory.shared.ldap.schema.normalizers.OidNormalizer;
import org.apache.directory.shared.ldap.subtree.AdministrativeRole;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecification;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecificationParser;
import org.apache.directory.shared.ldap.trigger.TriggerSpecification;
import org.apache.directory.shared.ldap.trigger.TriggerSpecificationParser;
import org.apache.directory.shared.ldap.util.StringTools;
import org.apache.directory.shared.ldap.util.tree.DnNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The Subentry interceptor service which is responsible for filtering
 * out subentries on search operations and injecting operational attributes
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SubentryInterceptor extends BaseInterceptor
{
    /** The logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( SubentryInterceptor.class );

    /** the subentry control OID */
    private static final String SUBENTRY_CONTROL = SubentriesControl.CONTROL_OID;

    /** The set of Subentry operational attributes */
    public static AttributeType[] SUBENTRY_OPATTRS;

    /** the hash mapping the DN of a subentry to its SubtreeSpecification/types */
    private final SubentryCache subentryCache = new SubentryCache();

    /** The SubTree specification parser instance */
    private SubtreeSpecificationParser ssParser;

    /** The Subtree evaluator instance */
    private SubtreeEvaluator evaluator;

    /** a normalizing Trigger Specification parser */
    private TriggerSpecificationParser triggerParser;

    /** a normalizing ACIItem parser */
    private ACIItemParser aciParser;

    /** A reference to the nexus for direct backend operations */
    private PartitionNexus nexus;

    /** The SchemManager instance */
    private SchemaManager schemaManager;

    /** A reference to the ObjectClass AT */
    private static AttributeType OBJECT_CLASS_AT;
    
    /** A reference to the CN AT */
    private static AttributeType CN_AT;
    
    /** A reference to the EntryUUID AT */
    private static AttributeType ENTRY_UUID_AT;

    /** A reference to the AdministrativeRole AT */
    private static AttributeType ADMINISTRATIVE_ROLE_AT;

    /** A reference to the SubtreeSpecification AT */
    private static AttributeType SUBTREE_SPECIFICATION_AT;

    /** A reference to the AccessControl dedicated AT */
    private static AttributeType ACCESS_CONTROL_SUBENTRIES_AT;
    private static AttributeType ACCESS_CONTROL_SEQ_NUMBER_AT;
    private static AttributeType ACCESS_CONTROL_SUBENTRIES_UUID_AT;

    /** A reference to the CollectiveAttribute dedicated AT */
    private static AttributeType COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT;
    private static AttributeType COLLECTIVE_ATTRIBUTE_SEQ_NUMBER_AT;
    private static AttributeType COLLECTIVE_ATTRIBUTE_SUBENTRIES_UUID_AT;

    /** A reference to the Subschema dedicated AT */
    private static AttributeType SUBSCHEMA_SUBENTRY_AT;
    private static AttributeType SUBSCHEMA_SUBENTRY_UUID_AT;
    private static AttributeType SUB_SCHEMA_SEQ_NUMBER_AT;

    /** A reference to the TriggerExecution dedicated AT */
    private static AttributeType TRIGGER_EXECUTION_SUBENTRIES_AT;
    private static AttributeType TRIGGER_EXECUTION_SEQ_NUMBER_AT;
    private static AttributeType TRIGGER_EXECUTION_SUBENTRIES_UUID_AT;

    /** An enum used for the entries update */
    private enum OperationEnum
    {
        ADD,
        REMOVE,
        REPLACE
    }
    
    /** The possible roles */
    private static final Set<String> ROLES = new HashSet<String>();

    // Initialize the ROLES field
    static
    {
        ROLES.add( SchemaConstants.AUTONOMOUS_AREA.toLowerCase() );
        ROLES.add( SchemaConstants.AUTONOMOUS_AREA_OID );
        ROLES.add( SchemaConstants.ACCESS_CONTROL_SPECIFIC_AREA.toLowerCase() );
        ROLES.add( SchemaConstants.ACCESS_CONTROL_SPECIFIC_AREA_OID );
        ROLES.add( SchemaConstants.ACCESS_CONTROL_INNER_AREA.toLowerCase() );
        ROLES.add( SchemaConstants.ACCESS_CONTROL_INNER_AREA_OID );
        ROLES.add( SchemaConstants.COLLECTIVE_ATTRIBUTE_SPECIFIC_AREA.toLowerCase() );
        ROLES.add( SchemaConstants.COLLECTIVE_ATTRIBUTE_SPECIFIC_AREA_OID );
        ROLES.add( SchemaConstants.COLLECTIVE_ATTRIBUTE_INNER_AREA.toLowerCase() );
        ROLES.add( SchemaConstants.COLLECTIVE_ATTRIBUTE_INNER_AREA_OID );
        ROLES.add( SchemaConstants.SUB_SCHEMA_ADMIN_SPECIFIC_AREA.toLowerCase() );
        ROLES.add( SchemaConstants.SUB_SCHEMA_ADMIN_SPECIFIC_AREA_OID );
        ROLES.add( SchemaConstants.TRIGGER_EXECUTION_SPECIFIC_AREA.toLowerCase() );
        ROLES.add( SchemaConstants.TRIGGER_EXECUTION_SPECIFIC_AREA_OID );
        ROLES.add( SchemaConstants.TRIGGER_EXECUTION_INNER_AREA.toLowerCase() );
        ROLES.add( SchemaConstants.TRIGGER_EXECUTION_INNER_AREA_OID );
    }

    /** A Map to associate a role with it's OID */
    private static final Map<String, String> ROLES_OID = new HashMap<String, String>();

    // Initialize the roles/oid map
    static
    {
        ROLES_OID.put( SchemaConstants.AUTONOMOUS_AREA.toLowerCase(), SchemaConstants.AUTONOMOUS_AREA_OID );
        ROLES_OID.put( SchemaConstants.ACCESS_CONTROL_SPECIFIC_AREA.toLowerCase(),
            SchemaConstants.ACCESS_CONTROL_SPECIFIC_AREA_OID );
        ROLES_OID.put( SchemaConstants.ACCESS_CONTROL_INNER_AREA.toLowerCase(),
            SchemaConstants.ACCESS_CONTROL_INNER_AREA_OID );
        ROLES_OID.put( SchemaConstants.COLLECTIVE_ATTRIBUTE_SPECIFIC_AREA.toLowerCase(),
            SchemaConstants.COLLECTIVE_ATTRIBUTE_SPECIFIC_AREA_OID );
        ROLES_OID.put( SchemaConstants.COLLECTIVE_ATTRIBUTE_INNER_AREA.toLowerCase(),
            SchemaConstants.COLLECTIVE_ATTRIBUTE_INNER_AREA_OID );
        ROLES_OID.put( SchemaConstants.SUB_SCHEMA_ADMIN_SPECIFIC_AREA.toLowerCase(),
            SchemaConstants.SUB_SCHEMA_ADMIN_SPECIFIC_AREA_OID );
        ROLES_OID.put( SchemaConstants.TRIGGER_EXECUTION_SPECIFIC_AREA.toLowerCase(),
            SchemaConstants.TRIGGER_EXECUTION_SPECIFIC_AREA_OID );
        ROLES_OID.put( SchemaConstants.TRIGGER_EXECUTION_INNER_AREA.toLowerCase(),
            SchemaConstants.TRIGGER_EXECUTION_INNER_AREA_OID );
    }

    /** The possible inner area roles */
    private static final Set<String> INNER_AREA_ROLES = new HashSet<String>();

    static
    {
        INNER_AREA_ROLES.add( SchemaConstants.ACCESS_CONTROL_INNER_AREA.toLowerCase() );
        INNER_AREA_ROLES.add( SchemaConstants.ACCESS_CONTROL_INNER_AREA_OID );
        INNER_AREA_ROLES.add( SchemaConstants.COLLECTIVE_ATTRIBUTE_INNER_AREA.toLowerCase() );
        INNER_AREA_ROLES.add( SchemaConstants.COLLECTIVE_ATTRIBUTE_INNER_AREA_OID );
        INNER_AREA_ROLES.add( SchemaConstants.TRIGGER_EXECUTION_INNER_AREA.toLowerCase() );
        INNER_AREA_ROLES.add( SchemaConstants.TRIGGER_EXECUTION_INNER_AREA_OID );
    }

    /** The possible specific area roles */
    private static final Set<String> SPECIFIC_AREA_ROLES = new HashSet<String>();

    static
    {
        SPECIFIC_AREA_ROLES.add( SchemaConstants.ACCESS_CONTROL_SPECIFIC_AREA.toLowerCase() );
        SPECIFIC_AREA_ROLES.add( SchemaConstants.ACCESS_CONTROL_SPECIFIC_AREA_OID );
        SPECIFIC_AREA_ROLES.add( SchemaConstants.COLLECTIVE_ATTRIBUTE_SPECIFIC_AREA.toLowerCase() );
        SPECIFIC_AREA_ROLES.add( SchemaConstants.COLLECTIVE_ATTRIBUTE_SPECIFIC_AREA_OID );
        SPECIFIC_AREA_ROLES.add( SchemaConstants.SUB_SCHEMA_ADMIN_SPECIFIC_AREA.toLowerCase() );
        SPECIFIC_AREA_ROLES.add( SchemaConstants.SUB_SCHEMA_ADMIN_SPECIFIC_AREA_OID );
        SPECIFIC_AREA_ROLES.add( SchemaConstants.TRIGGER_EXECUTION_SPECIFIC_AREA.toLowerCase() );
        SPECIFIC_AREA_ROLES.add( SchemaConstants.TRIGGER_EXECUTION_SPECIFIC_AREA_OID );
    }

    /** A lock to guarantee the AP cache consistency */
    private ReentrantReadWriteLock mutex = new ReentrantReadWriteLock();

    /**
     * the set of interceptors we should *not* go through updating some operational attributes
     */
    private static final Collection<String> BYPASS_INTERCEPTORS;

    static
    {
        Set<String> c = new HashSet<String>();
        c.add( NormalizationInterceptor.class.getName() );
        c.add( AuthenticationInterceptor.class.getName() );
        c.add( AciAuthorizationInterceptor.class.getName() );
        c.add( AdministrativePointInterceptor.class.getName() );
        c.add( DefaultAuthorizationInterceptor.class.getName() );
        c.add( ExceptionInterceptor.class.getName() );
        c.add( OperationalAttributeInterceptor.class.getName() );
        c.add( SchemaInterceptor.class.getName() );
        c.add( CollectiveAttributeInterceptor.class.getName() );
        c.add( SubentryInterceptor.class.getName() );
        c.add( EventInterceptor.class.getName() );
        c.add( TriggerInterceptor.class.getName() );
        BYPASS_INTERCEPTORS = Collections.unmodifiableCollection( c );
    }
    
    //-------------------------------------------------------------------------------------------
    // Search filter methods
    //-------------------------------------------------------------------------------------------
    /**
     * SearchResultFilter used to filter out subentries based on objectClass values.
     * A subentry won't be returned if the request is done with a ONE_LEVEL or SUB_LEVEL
     * scope.
     */
    public class HideSubentriesFilter implements EntryFilter
    {
        public boolean accept( SearchingOperationContext searchContext, ClonedServerEntry entry ) throws Exception
        {
            // See if the requested entry is a subentry
            return !entry.contains( OBJECT_CLASS_AT, SchemaConstants.SUBENTRY_OC );
        }
    }


    /**
     * SearchResultFilter used to filter out normal entries but shows subentries based on
     * objectClass values.
     */
    public class HideEntriesFilter implements EntryFilter
    {
        public boolean accept( SearchingOperationContext searchContext, ClonedServerEntry entry ) throws Exception
        {
            // See if the requested entry is a subentry
            return entry.contains( OBJECT_CLASS_AT, SchemaConstants.SUBENTRY_OC );
        }
    }

    
    /**
     * Filter the returned entries, checking if they depend on some APs, and if so,
     * update their SeqNumber if it's not uptodate.
     */
    public class SeqNumberUpdateFilter implements EntryFilter
    {
        public boolean accept( SearchingOperationContext searchContext, ClonedServerEntry entry ) throws Exception
        {
            if ( entry.getOriginalEntry().containsAttribute( ADMINISTRATIVE_ROLE_AT ) ||
                entry.getOriginalEntry().contains( OBJECT_CLASS_AT, SchemaConstants.SUBENTRY_OC ) )
            {
                // No need to update anything
                return true;
            }

            List<Modification> modifications = updateEntry( entry );
            
            if ( modifications != null )
            {
                // The entry has been updated, we have to apply the modifications in the entry
                applyModifications( entry, modifications );
            }

            return true;
        }
    }
    
    
    /**
     * Apply the modifications done on an entry. We should just have REPLACE operations
     * @param entry
     * @param modifications
     * @throws LdapException
     */
    private void applyModifications( Entry entry, List<Modification> modifications ) throws LdapException
    {
        for ( Modification modification : modifications )
        {
            switch ( modification.getOperation() )
            {
                case ADD_ATTRIBUTE :
                    entry.add( modification.getAttribute() );
                    break;
                    
                case REMOVE_ATTRIBUTE :
                    entry.remove( modification.getAttribute() );
                    break;
                    
                case REPLACE_ATTRIBUTE :
                    entry.remove( modification.getAttribute() );
                    entry.add( modification.getAttribute() );
                    
                    break;
            }
        }
    }
    
    
    /**
     * Creates a specific Subentry instances using the Auxiliary ObjectClasses stored into 
     * the Subentry Entry. We may have more than one instance created, if the entry is managing
     * more than one role.
     */
    private Subentry[] createSubentry( Entry subentry ) throws LdapException
    {
        DN subentryDn = subentry.getDn();

        String subtree = subentry.get( SUBTREE_SPECIFICATION_AT ).getString();
        
        EntryAttribute cn = subentry.get( SchemaConstants.CN_AT );
        String uuid = subentry.get( SchemaConstants.ENTRY_UUID_AT).getString();
        SubtreeSpecification ss;

        // Process the SubtreeSpecification
        try
        {
            ss = ssParser.parse( subtree );
        }
        catch ( Exception e )
        {
            String message = "Failed while parsing subtreeSpecification for " + subentryDn;
            LOG.error( message );
            
            throw new LdapUnwillingToPerformException( message );
        }

        // Now, creates all the subentries
        List<Subentry> subentries = new ArrayList<Subentry>();
        
        // CollectiveAttribute Subentry
        if ( subentry.contains( OBJECT_CLASS_AT, SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRY_OC ) )
        {
            // It's a CA subentry. Collect the list of COLLECTIVE attributes
            List<EntryAttribute> collectiveAttributes = new ArrayList<EntryAttribute>();
            
            for ( EntryAttribute attribute : subentry )
            {
                if ( attribute.getAttributeType().isCollective() )
                {
                    collectiveAttributes.add( attribute );
                }
            }
            
            Subentry newSubentry = new CollectiveAttributeSubentry( cn, ss, uuid, collectiveAttributes );
            
            subentries.add( newSubentry );
        }

        // AccessControl Subentry
        if ( subentry.contains( OBJECT_CLASS_AT, SchemaConstants.ACCESS_CONTROL_SUBENTRY_OC ) )
        {
            // It's a AC subentry. Collect the list of ACIItem attributes
            EntryAttribute prescriptiveACI = subentry.get( SchemaConstants.PRESCRIPTIVE_ACI_AT );
            
            AccessControlSubentry newSubentry = new AccessControlSubentry( cn, ss, uuid );

            for ( Value<?> value:prescriptiveACI )
            {
                ACIItem aciItem = null;
                String aciItemStr = value.getString();

                try
                {
                    aciItem = aciParser.parse( aciItemStr );
                    newSubentry.addAciItem( aciItem );
                }
                catch ( ParseException e )
                {
                    String msg = I18n.err( I18n.ERR_73, aciItemStr );
                    LOG.error( msg, e );
                }

            }
            
            subentries.add( newSubentry );
        }

        // SubSchema Subentry
        if ( subentry.contains( OBJECT_CLASS_AT, SchemaConstants.SUB_SCHEMA_SUBENTRY_AT ) )
        {
            SubSchemaSubentry newSubentry = new SubSchemaSubentry( cn, ss, uuid );
            subentries.add( newSubentry );
        }

        // TrggerExecution Subentry
        if ( subentry.contains( OBJECT_CLASS_AT, SchemaConstants.TRIGGER_EXECUTION_SUBENTRY_OC ) )
        {
            EntryAttribute triggerSpecificationAttr = subentry.get( SchemaConstants.PRESCRIPTIVE_TRIGGER_SPECIFICATION );
            TriggerExecutionSubentry newSubentry = new TriggerExecutionSubentry( cn, ss, uuid );

            for ( Value<?> value:triggerSpecificationAttr )
            {
                try
                {
                    TriggerSpecification triggerSpecification = triggerParser.parse( value.getString() );
                    newSubentry.addTriggerSpecification( triggerSpecification );
                }
                catch ( ParseException e )
                {
                    String msg = I18n.err( I18n.ERR_73, value );
                    LOG.error( msg, e );
                }
            }
        }
        
        return subentries.toArray( new Subentry[]{});
    }

    //-------------------------------------------------------------------------------------------
    // Interceptor initialization
    //-------------------------------------------------------------------------------------------
    /**
     * Initialize the Subentry Interceptor
     *
     * @param directoryService The DirectoryService instance
     */
    public void init( DirectoryService directoryService ) throws LdapException
    {
        super.init( directoryService );

        this.directoryService = directoryService;
        nexus = directoryService.getPartitionNexus();
        schemaManager = directoryService.getSchemaManager();

        // setup various attribute type values
        OBJECT_CLASS_AT = schemaManager.getAttributeType( SchemaConstants.OBJECT_CLASS_AT );
        ADMINISTRATIVE_ROLE_AT = schemaManager.getAttributeType( SchemaConstants.ADMINISTRATIVE_ROLE_AT );
        SUBTREE_SPECIFICATION_AT = schemaManager.getAttributeType( SchemaConstants.SUBTREE_SPECIFICATION_AT );
        ENTRY_UUID_AT = schemaManager.getAttributeType( SchemaConstants.ENTRY_UUID_AT );
        CN_AT = schemaManager.getAttributeType( SchemaConstants.CN_AT );
        
        ACCESS_CONTROL_SEQ_NUMBER_AT = schemaManager.getAttributeType( ApacheSchemaConstants.ACCESS_CONTROL_SEQ_NUMBER_AT );
        ACCESS_CONTROL_SUBENTRIES_AT = schemaManager.getAttributeType( SchemaConstants.ACCESS_CONTROL_SUBENTRIES_AT );
        ACCESS_CONTROL_SUBENTRIES_UUID_AT = schemaManager.getAttributeType( ApacheSchemaConstants.ACCESS_CONTROL_SUBENTRIES_UUID_AT );

        COLLECTIVE_ATTRIBUTE_SEQ_NUMBER_AT = schemaManager.getAttributeType( ApacheSchemaConstants.COLLECTIVE_ATTRIBUTE_SEQ_NUMBER_AT );
        COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT = schemaManager.getAttributeType( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        COLLECTIVE_ATTRIBUTE_SUBENTRIES_UUID_AT = schemaManager.getAttributeType( ApacheSchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_UUID_AT );
        
        SUB_SCHEMA_SEQ_NUMBER_AT = schemaManager.getAttributeType( ApacheSchemaConstants.SUB_SCHEMA_SEQ_NUMBER_AT );
        SUBSCHEMA_SUBENTRY_AT = schemaManager.getAttributeType( SchemaConstants.SUB_SCHEMA_SUBENTRY_AT );
        SUBSCHEMA_SUBENTRY_UUID_AT = schemaManager.getAttributeType( ApacheSchemaConstants.SUB_SCHEMA_SUBENTRY_UUID_AT );
        
        TRIGGER_EXECUTION_SEQ_NUMBER_AT = schemaManager.getAttributeType( ApacheSchemaConstants.TRIGGER_EXECUTION_SEQ_NUMBER_AT );
        TRIGGER_EXECUTION_SUBENTRIES_AT = schemaManager.getAttributeType( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
        TRIGGER_EXECUTION_SUBENTRIES_UUID_AT = schemaManager.getAttributeType( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_UUID_AT );

        SUBENTRY_OPATTRS = new AttributeType[]
            {
                ACCESS_CONTROL_SUBENTRIES_AT,
                SUBSCHEMA_SUBENTRY_AT,
                COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT,
                TRIGGER_EXECUTION_SUBENTRIES_AT
            };

        // Initialize the various parsers we use for SubtreeSpecification, ACIItem and TriggerSpecification
        ssParser = new SubtreeSpecificationParser( schemaManager );
        evaluator = new SubtreeEvaluator( schemaManager );

        triggerParser = new TriggerSpecificationParser( new NormalizerMappingResolver()
        {
            public Map<String, OidNormalizer> getNormalizerMapping() throws Exception
            {
                return schemaManager.getNormalizerMapping();
            }
        } );

        aciParser = new ACIItemParser( new ConcreteNameComponentNormalizer( schemaManager ), schemaManager );

        // prepare to find all subentries in all namingContexts
        Set<String> suffixes = nexus.listSuffixes();
        ExprNode filter = new EqualityNode<String>( OBJECT_CLASS_AT, new StringValue(
            SchemaConstants.SUBENTRY_OC ) );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setReturningAttributes( new String[]
            { SchemaConstants.SUBTREE_SPECIFICATION_AT, SchemaConstants.OBJECT_CLASS_AT } );

        DN adminDn = directoryService.getDNFactory().create( ServerDNConstants.ADMIN_SYSTEM_DN );

        // search each namingContext for subentries
        for ( String suffix : suffixes )
        {
            DN suffixDn = directoryService.getDNFactory().create( suffix );

            CoreSession adminSession = new DefaultCoreSession(
                new LdapPrincipal( adminDn, AuthenticationLevel.STRONG ), directoryService );

            SearchOperationContext searchOperationContext = new SearchOperationContext( adminSession, suffixDn, filter,
                controls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.NEVER_DEREF_ALIASES );

            EntryFilteringCursor subentries = nexus.search( searchOperationContext );

            // Loop on all the found Subentries, parse the SubtreeSpecification
            // and store the subentry in the subrentry caches (UUID and DN)
            try
            {
                while ( subentries.next() )
                {
                    Entry subentry = subentries.get();

                    Subentry[] subentryInstances = createSubentry( subentry );
                    
                    for ( Subentry newSubentry : subentryInstances )
                    {
                        directoryService.getSubentryCache().addSubentry( subentry.getDn(), newSubentry );
                    }
                }

                subentries.close();
            }
            catch ( Exception e )
            {
                throw new LdapOperationException( e.getMessage() );
            }
        }
    }


    //-------------------------------------------------------------------------------------------
    // Helper methods
    //-------------------------------------------------------------------------------------------
    /**
     * Return the list of AdministrativeRole for a subentry
     */
    private Set<AdministrativeRoleEnum> getSubentryAdminRoles( Entry subentry ) throws LdapException
    {
        Set<AdministrativeRoleEnum> adminRoles = new HashSet<AdministrativeRoleEnum>();

        if ( subentry.hasObjectClass( SchemaConstants.ACCESS_CONTROL_SUBENTRY_OC ) )
        {
            adminRoles.add( AdministrativeRoleEnum.AccessControl );
        }

        if ( subentry.hasObjectClass( SchemaConstants.SUBSCHEMA_OC ) )
        {
            adminRoles.add( AdministrativeRoleEnum.SubSchema );
        }

        if ( subentry.hasObjectClass( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRY_OC ) )
        {
            adminRoles.add( AdministrativeRoleEnum.CollectiveAttribute );
        }

        if ( subentry.hasObjectClass( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRY_OC ) )
        {
            adminRoles.add( AdministrativeRoleEnum.TriggerExecution );
        }

        return adminRoles;
    }


    /**
     * Checks to see if subentries for the search and list operations should be
     * made visible based on the availability of the search request control
     *
     * @param invocation the invocation object to use for determining subentry visibility
     * @return true if subentries should be visible, false otherwise
     * @throws Exception if there are problems accessing request controls
     */
    private boolean isSubentriesControlPresent( OperationContext opContext ) throws LdapException
    {
        if ( !opContext.hasRequestControls() )
        {
            return false;
        }

        // found the subentry request control so we return its value
        if ( opContext.hasRequestControl( SUBENTRY_CONTROL ) )
        {
            SubentriesControl subentriesControl = ( SubentriesControl ) opContext.getRequestControl( SUBENTRY_CONTROL );

            return subentriesControl.isVisible();
        }

        return false;
    }
    
    
    /**
     * Update the seqNumber for each kind of role. The entry will be updated in the backend only
     * if its seqNumbers are not up to date.
     */
    private List<Modification> updateEntry( Entry entry ) throws LdapException
    {
        List<Modification> modificationAcs = updateEntry( entry, directoryService.getAccessControlAPCache(), ACCESS_CONTROL_SEQ_NUMBER_AT, ACCESS_CONTROL_SUBENTRIES_UUID_AT );
        List<Modification> modificationCas = updateEntry( entry, directoryService.getCollectiveAttributeAPCache(), COLLECTIVE_ATTRIBUTE_SEQ_NUMBER_AT, COLLECTIVE_ATTRIBUTE_SUBENTRIES_UUID_AT );
        List<Modification> modificationSss = updateEntry( entry, directoryService.getSubschemaAPCache(), SUB_SCHEMA_SEQ_NUMBER_AT, SUBSCHEMA_SUBENTRY_UUID_AT );
        List<Modification> modificationTes = updateEntry( entry, directoryService.getTriggerExecutionAPCache(), TRIGGER_EXECUTION_SEQ_NUMBER_AT, TRIGGER_EXECUTION_SUBENTRIES_UUID_AT );
        List<Modification> modifications = null;
        
        if ( ( modificationAcs != null ) || ( modificationCas != null ) || ( modificationSss != null ) || ( modificationTes != null ) )
        {
            modifications = new ArrayList<Modification>();
            
            if ( modificationAcs != null )
            {
                modifications.addAll( modificationAcs );
            }
            
            if ( modificationCas != null )
            {
                modifications.addAll( modificationCas );
            }
            
            if ( modificationSss != null )
            {
                modifications.addAll( modificationSss );
            }
            
            if ( modificationTes != null )
            {
                modifications.addAll( modificationTes );
            }
            
            ModifyOperationContext modCtx = new ModifyOperationContext( directoryService.getAdminSession() );
            modCtx.setByPassed( BYPASS_INTERCEPTORS );
            modCtx.setDn( entry.getDn() );
            modCtx.setModItems( modifications );
            modCtx.setEntry( (ClonedServerEntry)entry );

            directoryService.getOperationManager().modify( modCtx );
        }
        
        return modifications;
    }

    
    /**
     * Update the CollectiveAttribute seqNumber for each kind of role. The entry will be updated in the backend only
     * if its seqNumbers are not up to date.
     */
    private List<Modification> updateEntry( Entry entry, DnNode<AdministrativePoint> apCache, AttributeType seqNumberAT,
                AttributeType subentryUuidAT ) throws LdapException
    {
        DN entryDn = entry.getDn();
        List<Modification> modifications = null;
        
        DnNode<AdministrativePoint> apNode = apCache.getParentWithElement( entryDn );
        
        if ( apNode != null )
        {
            // We have an AdministrativePoint for this entry, get its SeqNumber
            AdministrativePoint adminPoint = apNode.getElement();
            EntryAttribute seqNumberAttribute = entry.get( seqNumberAT );

            // We have to recurse : starting from the IAP, we go up the AP tree
            // until we find the SAP. For each AP we find, we compare the AP's seqNumber
            // and if it's above the current SeqNumber (initialized to the entry value),
            // we select this SeqNumber. When we are done, if the selected SeqNumber is
            // greater than the initial entry seqNumber, then we update the entry
            
            // First, init the entry seqNumber. If we have no AT, then we initialize it to -1
            long entrySeqNumber = Long.MIN_VALUE;
            
            if ( seqNumberAttribute != null )
            {
                entrySeqNumber = Long.parseLong( seqNumberAttribute.getString() );
            }
            
            boolean sapFound = false;
            boolean seqNumberUpdated = false;
            List<String> subentryUuids = null;
            long initialSeqNumber = entrySeqNumber;

            do
            {
                if ( adminPoint.isSpecific() )
                {
                    sapFound = true;
                }
                
                // We update the seqNumber only if it's below the AdminPoint seqNumber
                // We update the UUID ref only if the initial seqNumber is below the AdminPoint
                if ( entrySeqNumber < adminPoint.getSeqNumber() )
                {
                    seqNumberUpdated = true;
                    subentryUuids = new ArrayList<String>();
                    entrySeqNumber = adminPoint.getSeqNumber();
                }

                if ( initialSeqNumber < adminPoint.getSeqNumber() )
                {
                    // Evaluate the current AP on the entry for each subentry
                    for ( Subentry subentry : adminPoint.getSubentries() )
                    {
                        if ( evaluator.evaluate( subentry.getSubtreeSpecification(), apNode.getDn(), entryDn, entry ) )
                        {
                            subentryUuids.add( subentry.getUuid() ); 
                        }
                    }
                }
                
                // Go down one level
                apNode = apNode.getParentWithElement();
                adminPoint = apNode.getElement();
            } while ( !sapFound );
            
            // If we have updated the entry, create the list of modifications to apply
            if ( seqNumberUpdated )
            {
                // Create the list of modifications : we will inject REPLACE operations. 
                modifications = new ArrayList<Modification>();
                
                // The seqNubmer
                EntryAttribute newSeqNumberAT = new DefaultEntryAttribute( seqNumberAT, Long.toString( entrySeqNumber ) );
                Modification seqNumberModification = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, newSeqNumberAT );
                
                modifications.add( seqNumberModification );
                
                // The subentry UUID, if any
                if ( ( subentryUuids != null ) && ( subentryUuids.size() != 0 ) )
                {
                    EntryAttribute newSubentryUuidAT = new DefaultEntryAttribute( subentryUuidAT, subentryUuids.toArray( new String[]{} ) );
                    Modification subentryUuiMod = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, newSubentryUuidAT );

                    modifications.add( subentryUuiMod );
                }
                else
                {
                    // We may have to remove UUID refs from the entry
                    if ( entry.containsAttribute( subentryUuidAT ) )
                    {
                        EntryAttribute newSubentryUuidAT = new DefaultEntryAttribute( subentryUuidAT );
                        Modification subentryUuiMod = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, newSubentryUuidAT );

                        modifications.add( subentryUuiMod );
                    }
                }
            }
        }

        return modifications;
    }


    /**
     * Checks if the given DN is a namingContext
     */
    private boolean isNamingContext( DN dn ) throws LdapException
    {
        DN namingContext = nexus.findSuffix( dn );

        return dn.equals( namingContext );
    }


    /**
     * Get the SubtreeSpecification, parse it and stores it into the subentry
     */
    private void setSubtreeSpecification( Subentry subentry, Entry entry ) throws LdapException
    {
        String subtree = entry.get( SUBTREE_SPECIFICATION_AT ).getString();
        SubtreeSpecification ss;

        try
        {
            ss = ssParser.parse( subtree );
        }
        catch ( Exception e )
        {
            String msg = I18n.err( I18n.ERR_307, entry.getDn() );
            LOG.warn( msg );
            throw new LdapInvalidAttributeValueException( ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX, msg );
        }

        subentry.setSubtreeSpecification( ss );
    }


    /**
     * Checks to see if an entry being renamed has a descendant that is an
     * administrative point.
     *
     * @param name the name of the entry which is used as the search base
     * @return true if name is an administrative point or one of its descendants
     * are, false otherwise
     * @throws Exception if there are errors while searching the directory
     */
    private boolean hasAdministrativeDescendant( OperationContext opContext, DN name ) throws LdapException
    {
        ExprNode filter = new PresenceNode( ADMINISTRATIVE_ROLE_AT );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );

        SearchOperationContext searchOperationContext = new SearchOperationContext( opContext.getSession(), name,
            filter, controls );
        searchOperationContext.setAliasDerefMode( AliasDerefMode.NEVER_DEREF_ALIASES );

        EntryFilteringCursor aps = nexus.search( searchOperationContext );

        try
        {
            if ( aps.next() )
            {
                aps.close();
                return true;
            }
        }
        catch ( Exception e )
        {
            throw new LdapOperationException( e.getMessage() );
        }


        return false;
    }


    /**
     * {@inheritDoc}
     *
    private List<Modification> getModsOnEntryRdnChange( DN oldName, DN newName, Entry entry ) throws LdapException
    {
        List<Modification> modifications = new ArrayList<Modification>();

        // There are two different situations warranting action.  First if
        // an ss evalutating to true with the old name no longer evalutates
        // to true with the new name.  This would be caused by specific chop
        // exclusions that effect the new name but did not effect the old
        // name. In this case we must remove subentry operational attribute
        // values associated with the dn of that subentry.
        //
        // In the second case an ss selects the entry with the new name when
        // it did not previously with the old name. Again this situation
        // would be caused by chop exclusions. In this case we must add subentry
        // operational attribute values with the dn of this subentry.
        //
        for ( String uuid : subentryCache )
        {
            DN apDn = null; //subentryDn.getParent();
            DN subentryDn = null;
            
            SubtreeSpecification ss = directoryService.getSubentryCache().getSubentry( subentryDn ).getSubtreeSpecification();
            boolean isOldNameSelected = evaluator.evaluate( ss, apDn, oldName, entry );
            boolean isNewNameSelected = evaluator.evaluate( ss, apDn, newName, entry );

            if ( isOldNameSelected == isNewNameSelected )
            {
                continue;
            }

            // need to remove references to the subentry
            if ( isOldNameSelected && !isNewNameSelected )
            {
                for ( AttributeType operationalAttribute : SUBENTRY_OPATTRS )
                {
                    ModificationOperation op = ModificationOperation.REPLACE_ATTRIBUTE;
                    EntryAttribute opAttr = entry.get( operationalAttribute );

                    if ( opAttr != null )
                    {
                        opAttr = opAttr.clone();
                        opAttr.remove( subentryDn.getNormName() );

                        if ( opAttr.size() < 1 )
                        {
                            op = ModificationOperation.REMOVE_ATTRIBUTE;
                        }

                        modifications.add( new DefaultModification( op, opAttr ) );
                    }
                }
            }
            // need to add references to the subentry
            else if ( isNewNameSelected && !isOldNameSelected )
            {
                for ( AttributeType operationalAttribute : SUBENTRY_OPATTRS )
                {
                    ModificationOperation op = ModificationOperation.ADD_ATTRIBUTE;
                    EntryAttribute opAttr = new DefaultEntryAttribute( operationalAttribute );
                    opAttr.add( subentryDn.getNormName() );
                    modifications.add( new DefaultModification( op, opAttr ) );
                }
            }
        }

        return modifications;
    }
    
    
    /**
     * Check that the AT contains the AccessControl SAP role
     */
    private boolean hasAccessControlSpecificRole( EntryAttribute adminPoint )
    {
        return adminPoint.contains( SchemaConstants.ACCESS_CONTROL_SPECIFIC_AREA ) ||
               adminPoint.contains( SchemaConstants.ACCESS_CONTROL_SPECIFIC_AREA_OID );
    }


    /**
     * Check that the AT contains the CollectiveAttribute SAP role
     */
    private boolean hasCollectiveAttributeSpecificRole( EntryAttribute adminPoint )
    {
        return adminPoint.contains( SchemaConstants.COLLECTIVE_ATTRIBUTE_SPECIFIC_AREA ) ||
               adminPoint.contains( SchemaConstants.COLLECTIVE_ATTRIBUTE_SPECIFIC_AREA_OID );
    }


    /**
     * Check that the AT contains the TriggerExecution SAP role
     */
    private boolean hasTriggerExecutionSpecificRole( EntryAttribute adminPoint )
    {
        return adminPoint.contains( SchemaConstants.TRIGGER_EXECUTION_SPECIFIC_AREA ) ||
               adminPoint.contains( SchemaConstants.TRIGGER_EXECUTION_SPECIFIC_AREA_OID );
    }


    /**
     * Check that the AT contains the SubSchema SAP role
     */
    private boolean hasSubSchemaSpecificRole( EntryAttribute adminPoint )
    {
        return adminPoint.contains( SchemaConstants.SUB_SCHEMA_ADMIN_SPECIFIC_AREA ) ||
               adminPoint.contains( SchemaConstants.SUB_SCHEMA_ADMIN_SPECIFIC_AREA_OID );
    }


    /**
     * Tells if the role is an AC IAP
     */
    private boolean isAccessControlInnerRole( String role )
    {
        return role.equalsIgnoreCase( SchemaConstants.ACCESS_CONTROL_INNER_AREA ) ||
               role.equals( SchemaConstants.ACCESS_CONTROL_INNER_AREA_OID );
    }


    /**
     * Tells if the role is an AC SAP
     */
    private boolean isAccessControlSpecificRole( String role )
    {
        return role.equalsIgnoreCase( SchemaConstants.ACCESS_CONTROL_SPECIFIC_AREA ) ||
               role.equals( SchemaConstants.ACCESS_CONTROL_SPECIFIC_AREA_OID );
    }


    /**
     * Tells if the role is a CA IAP
     */
    private boolean isCollectiveAttributeInnerRole( String role )
    {
        return role.equalsIgnoreCase( SchemaConstants.COLLECTIVE_ATTRIBUTE_INNER_AREA ) ||
               role.equals( SchemaConstants.COLLECTIVE_ATTRIBUTE_INNER_AREA_OID );
    }


    /**
     * Tells if the role is a CA SAP
     */
    private boolean isCollectiveAttributeSpecificRole( String role )
    {
        return role.equalsIgnoreCase( SchemaConstants.COLLECTIVE_ATTRIBUTE_SPECIFIC_AREA ) ||
               role.equals( SchemaConstants.COLLECTIVE_ATTRIBUTE_SPECIFIC_AREA_OID );
    }


    /**
     * Tells if the role is a TE IAP
     */
    private boolean isTriggerExecutionInnerRole( String role )
    {
        return role.equalsIgnoreCase( SchemaConstants.TRIGGER_EXECUTION_INNER_AREA ) ||
               role.equals( SchemaConstants.TRIGGER_EXECUTION_INNER_AREA_OID );
    }


    /**
     * Tells if the role is a TE SAP
     */
    private boolean isTriggerExecutionSpecificRole( String role )
    {
        return role.equalsIgnoreCase( SchemaConstants.TRIGGER_EXECUTION_SPECIFIC_AREA ) ||
               role.equals( SchemaConstants.TRIGGER_EXECUTION_SPECIFIC_AREA_OID );
    }


    /**
     * Tells if the role is a SS SAP
     */
    private boolean isSubschemaSpecificRole( String role )
    {
        return role.equalsIgnoreCase( SchemaConstants.SUB_SCHEMA_ADMIN_SPECIFIC_AREA ) ||
               role.equals( SchemaConstants.SUB_SCHEMA_ADMIN_SPECIFIC_AREA_OID );
    }


    /**
     * Tells if the role is an AAP
     */
    private boolean isAutonomousAreaRole( String role )
    {
        return role.equalsIgnoreCase( SchemaConstants.AUTONOMOUS_AREA ) ||
               role.equals( SchemaConstants.AUTONOMOUS_AREA_OID );
    }


    /**
     * Tells if the Administrative Point role is an AAP
     */
    private boolean isAAP( EntryAttribute adminPoint )
    {
        return ( adminPoint.contains( SchemaConstants.AUTONOMOUS_AREA ) || adminPoint
            .contains( SchemaConstants.AUTONOMOUS_AREA_OID ) );
    }


    /**
     * Tells if the Administrative Point role is an IAP
     */
    private boolean isIAP( EntryAttribute adminPoint )
    {
        return ( adminPoint.contains( SchemaConstants.ACCESS_CONTROL_INNER_AREA ) || 
            adminPoint.contains( SchemaConstants.ACCESS_CONTROL_INNER_AREA_OID ) ||
            adminPoint.contains( SchemaConstants.COLLECTIVE_ATTRIBUTE_INNER_AREA ) ||
            adminPoint.contains( SchemaConstants.COLLECTIVE_ATTRIBUTE_INNER_AREA_OID ) ||
            adminPoint.contains( SchemaConstants.TRIGGER_EXECUTION_INNER_AREA ) ||
            adminPoint.contains( SchemaConstants.TRIGGER_EXECUTION_INNER_AREA_OID ) );
    }


    /**
     * Check that we don't have an IAP and a SAP with the same family
     */
    private void checkInnerSpecificMix( String role, EntryAttribute adminPoint ) throws LdapUnwillingToPerformException
    {
        if ( isAccessControlInnerRole( role ) )
        {
            if ( hasAccessControlSpecificRole( adminPoint ) )
            {
                // This is inconsistent
                String message = "Cannot add a specific Administrative Point and the same"
                    + " inner Administrative point at the same time : " + adminPoint;
                LOG.error( message );
                throw new LdapUnwillingToPerformException( message );
            }
            else
            {
                return;
            }
        }

        if ( isCollectiveAttributeInnerRole( role ) )
        {
            if ( hasCollectiveAttributeSpecificRole( adminPoint ) )
            {
                // This is inconsistent
                String message = "Cannot add a specific Administrative Point and the same"
                    + " inner Administrative point at the same time : " + adminPoint;
                LOG.error( message );
                throw new LdapUnwillingToPerformException( message );
            }
            else
            {
                return;
            }
        }

        if ( isTriggerExecutionInnerRole( role ) )
        {
            if ( hasTriggerExecutionSpecificRole( adminPoint ) )
            {
                // This is inconsistent
                String message = "Cannot add a specific Administrative Point and the same"
                    + " inner Administrative point at the same time : " + adminPoint;
                LOG.error( message );
                throw new LdapUnwillingToPerformException( message );
            }
            else
            {
                return;
            }
        }
    }


    private boolean isIAP( String role )
    {
        return INNER_AREA_ROLES.contains( role );
    }


    /**
     * Check that the IAPs (if any) have a parent. We will check for each kind or role :
     * AC, CA and TE.
     */
    private void checkIAPHasParent( String role, EntryAttribute adminPoint, DN dn )
        throws LdapUnwillingToPerformException
    {
        // Check for the AC role
        if ( isAccessControlInnerRole( role ) )
        {
            DnNode<AdministrativePoint> acCache = directoryService.getAccessControlAPCache();
            
            DnNode<AdministrativePoint> parent =  acCache.getNode( dn );
            
            if ( parent == null )
            {
                // We don't have any AC administrativePoint in the tree, this is an error
                String message = "Cannot add an IAP with no parent : " + adminPoint;
                LOG.error( message );
                throw new LdapUnwillingToPerformException( message );
            }
        }
        else if ( isCollectiveAttributeInnerRole( role ) )
        {
            DnNode<AdministrativePoint> caCache = directoryService.getCollectiveAttributeAPCache();
            
            boolean hasAP = caCache.hasParentElement( dn );
            
            if ( !hasAP )
            {
                // We don't have any AC administrativePoint in the tree, this is an error
                String message = "Cannot add an IAP with no parent : " + adminPoint;
                LOG.error( message );
                throw new LdapUnwillingToPerformException( message );
            }
        }
        else if ( isTriggerExecutionInnerRole( role ) )
        {
            DnNode<AdministrativePoint> caCache = directoryService.getTriggerExecutionAPCache();
            
            DnNode<AdministrativePoint> parent =  caCache.getNode( dn );
            
            if ( parent == null )
            {
                // We don't have any AC administrativePoint in the tree, this is an error
                String message = "Cannot add an IAP with no parent : " + adminPoint;
                LOG.error( message );
                throw new LdapUnwillingToPerformException( message );
            }
        }
        else
        {
            // Wtf ? We *must* have an IAP here...
            String message = "This is not an IAP : " + role;
            LOG.error( message );
            throw new LdapUnwillingToPerformException( message );
        }
    }
    
    
    /**
     * Check if we can safely add a role. If it's an AAP, we have to be sure that
     * all the 4 SAPs are present.
     */
    private void checkAddRole( Value<?> role, EntryAttribute adminPoint, DN dn ) throws LdapException
    {
        String roleStr = StringTools.toLowerCase( StringTools.trim( role.getString() ) );

        // Check that the added AdministrativeRole is valid
        if ( !ROLES.contains( roleStr ) )
        {
            String message = "Cannot add the given role, it's not a valid one :" + role;
            LOG.error( message );
            throw new LdapUnwillingToPerformException( message );
        }

        // If we are trying to add an AAP, we have to check that 
        // all the SAP roles are present. If nit, we add them.
        int nbRoles = adminPoint.size();
        
        if ( isAutonomousAreaRole( roleStr ) )
        {
            nbRoles--;
            
            if ( !hasAccessControlSpecificRole( adminPoint ) )
            {
                adminPoint.add( SchemaConstants.ACCESS_CONTROL_SPECIFIC_AREA );
            }
            else
            {
                nbRoles--;
            }
            
            if ( !hasCollectiveAttributeSpecificRole( adminPoint ) )
            {
                adminPoint.add( SchemaConstants.COLLECTIVE_ATTRIBUTE_SPECIFIC_AREA );
            }
            else
            {
                nbRoles--;
            }
            
            if ( !hasTriggerExecutionSpecificRole( adminPoint ) )
            {
                adminPoint.add( SchemaConstants.TRIGGER_EXECUTION_SPECIFIC_AREA );
            }
            else
            {
                nbRoles--;
            }
            
            if ( !hasSubSchemaSpecificRole( adminPoint ) )
            {
                adminPoint.add( SchemaConstants.SUB_SCHEMA_ADMIN_SPECIFIC_AREA );
            }
            else
            {
                nbRoles--;
            }
            
            if ( nbRoles != 0 )
            {
                // Check that we don't have any other role : we should have only 5 roles max
                String message = "Cannot add an Autonomous Administrative Point if we have some IAP roles : "
                    + adminPoint;
                LOG.error( message );
                
                throw new LdapUnwillingToPerformException( message );
            }
                
            // Fine, we have an AAP and the 4 SAPs
            return;
        }
        
        // check that we can't mix Inner and Specific areas
        checkInnerSpecificMix( roleStr, adminPoint );

        // Check that we don't add an IAP with no parent. The IAP must be added under
        // either a AAP, or a SAP/IAP within the same family
        if ( isIAP( roleStr ) )
        {
            checkIAPHasParent( roleStr, adminPoint, dn );
        }
    }


    /**
     * Check if an AP being added is valid or not :
     * - it's not under a subentry
     * - it cannot be a subentry
     * - the roles must be consistent (ie, AAP is coming with the 4 SAPs, we can't have a SAP and a IAP for the same role)
     * - it can't be the rootDSE or a NamingContext
     */
    private boolean checkIsValidAP( Entry entry ) throws LdapException
    {
        DN dn = entry.getDn();
        
        // Not rootDSE nor a namingContext
        if ( dn.isRootDSE() || isNamingContext( dn ) )
        {
            return false;
        }
        
        // Not a subentry
        if ( entry.hasObjectClass( SchemaConstants.SUBENTRY_OC ) )
        {
            return false;
        }
        
        // Not under a subentry
        if ( directoryService.getSubentryCache().hasSubentry( dn.getParent() ) )
        {
            return false;
        }
        
        // Check the roles
        EntryAttribute adminPoint = entry.get( ADMINISTRATIVE_ROLE_AT );

        for ( Value<?> role : adminPoint )
        {
            checkAddRole( role, adminPoint, dn );
        }

        return true;
    }

    
    // -----------------------------------------------------------------------
    // Methods dealing with subentry modification
    // -----------------------------------------------------------------------

    private Set<AdministrativeRoleEnum> getSubentryTypes( Entry entry, List<Modification> mods ) throws LdapException
    {
        EntryAttribute ocFinalState = entry.get( OBJECT_CLASS_AT ).clone();

        for ( Modification mod : mods )
        {
            if ( mod.getAttribute().getId().equalsIgnoreCase( SchemaConstants.OBJECT_CLASS_AT ) ||
                 mod.getAttribute().getId().equalsIgnoreCase( SchemaConstants.OBJECT_CLASS_AT_OID ) )
            {
                switch ( mod.getOperation() )
                {
                    case ADD_ATTRIBUTE:
                        for ( Value<?> value : mod.getAttribute() )
                        {
                            ocFinalState.add( value.getString() );
                        }

                        break;

                    case REMOVE_ATTRIBUTE:
                        for ( Value<?> value : mod.getAttribute() )
                        {
                            ocFinalState.remove( value.getString() );
                        }

                        break;

                    case REPLACE_ATTRIBUTE:
                        ocFinalState = mod.getAttribute();
                        break;
                }
            }
        }

        Entry attrs = new DefaultEntry( schemaManager, DN.EMPTY_DN );
        attrs.put( ocFinalState );
        return getSubentryAdminRoles( attrs );
    }


    /**
     * Update the list of modifications with a modification associated with a specific
     * role, if it's requested.
     */
    private void getOperationalModForReplace( boolean hasRole, AttributeType attributeType, Entry entry, DN oldDn, DN newDn, List<Modification> modifications )
    {
        String oldDnStr = oldDn.getNormName();
        String newDnStr = newDn.getNormName();

        if ( hasRole )
        {
            EntryAttribute operational = entry.get( attributeType ).clone();

            if ( operational == null )
            {
                operational = new DefaultEntryAttribute( attributeType, newDnStr );
            }
            else
            {
                operational.remove( oldDnStr );
                operational.add( newDnStr );
            }

            modifications.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, operational ) );
        }
    }


    /**
     * Get the list of modifications to be applied on an entry to inject the operational attributes
     * associated with the administrative roles.
     */
    private List<Modification> getOperationalModsForReplace( DN oldDn, DN newDn, Subentry subentry, Entry entry )
        throws Exception
    {
        List<Modification> modifications = new ArrayList<Modification>();

        getOperationalModForReplace( subentry.isAccessControlAdminRole(), ACCESS_CONTROL_SUBENTRIES_AT, entry, oldDn, newDn, modifications );
        getOperationalModForReplace( subentry.isSchemaAdminRole(), SUBSCHEMA_SUBENTRY_AT, entry, oldDn, newDn, modifications );
        getOperationalModForReplace( subentry.isCollectiveAdminRole(), COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT, entry, oldDn, newDn, modifications );
        getOperationalModForReplace( subentry.isTriggersAdminRole(), TRIGGER_EXECUTION_SUBENTRIES_AT, entry, oldDn, newDn, modifications );

        return modifications;
    }


    /**
     * Gets the subschema operational attributes to be added to or removed from
     * an entry selected by a subentry's subtreeSpecification.
     */
    private List<EntryAttribute> getSubentryOperationalAttributes( DN dn, Subentry subentry ) throws LdapException
    {
        List<EntryAttribute> attributes = new ArrayList<EntryAttribute>();

        if ( subentry.isAccessControlAdminRole() )
        {
            EntryAttribute accessControlSubentries = new DefaultEntryAttribute( ACCESS_CONTROL_SUBENTRIES_AT, dn.getNormName() );
            attributes.add( accessControlSubentries );
        }

        if ( subentry.isSchemaAdminRole() )
        {
            EntryAttribute subschemaSubentry = new DefaultEntryAttribute( SUBSCHEMA_SUBENTRY_AT, dn.getNormName() );
            attributes.add( subschemaSubentry );
        }

        if ( subentry.isCollectiveAdminRole() )
        {
            EntryAttribute collectiveAttributeSubentries = new DefaultEntryAttribute( COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT, dn.getNormName() );
            attributes.add( collectiveAttributeSubentries );
        }

        if ( subentry.isTriggersAdminRole() )
        {
            EntryAttribute triggerExecutionSubentries = new DefaultEntryAttribute( TRIGGER_EXECUTION_SUBENTRIES_AT, dn.getNormName() );
            attributes.add( triggerExecutionSubentries );
        }

        return attributes;
    }


    /**
     * Calculates the subentry operational attributes to remove from a candidate
     * entry selected by a subtreeSpecification.  When we remove a subentry we
     * must remove the operational attributes in the entries that were once selected
     * by the subtree specification of that subentry.  To do so we must perform
     * a modify operation with the set of modifications to perform.  This method
     * calculates those modifications.
     *
     * @param subentryDn the distinguished name of the subentry
     * @param candidate the candidate entry to removed from the
     * @return the set of modifications required to remove an entry's reference to
     * a subentry
     */
    private List<Modification> getOperationalModsForRemove( DN subentryDn, Entry candidate ) throws LdapException
    {
        List<Modification> modifications = new ArrayList<Modification>();
        String dn = subentryDn.getNormName();

        for ( AttributeType operationalAttribute : SUBENTRY_OPATTRS )
        {
            EntryAttribute opAttr = candidate.get( operationalAttribute );

            if ( ( opAttr != null ) && opAttr.contains( dn ) )
            {
                EntryAttribute attr = new DefaultEntryAttribute( operationalAttribute, dn );
                modifications.add( new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, attr ) );
            }
        }

        return modifications;
    }


    /**
     * Calculates the subentry operational attributes to add or replace from
     * a candidate entry selected by a subtree specification.  When a subentry
     * is added or it's specification is modified some entries must have new
     * operational attributes added to it to point back to the associated
     * subentry.  To do so a modify operation must be performed on entries
     * selected by the subtree specification.  This method calculates the
     * modify operation to be performed on the entry.
     */
    private List<Modification> getOperationalModsForAdd( Entry entry, List<EntryAttribute> operationalAttributes ) throws LdapException
    {
        List<Modification> modifications = new ArrayList<Modification>();

        for ( EntryAttribute operationalAttribute : operationalAttributes )
        {
            EntryAttribute opAttrInEntry = entry.get( operationalAttribute.getAttributeType() );

            if ( ( opAttrInEntry != null ) && ( opAttrInEntry.size() > 0 ) )
            {
                EntryAttribute newOperationalAttribute = operationalAttribute.clone();

                for ( Value<?> value : opAttrInEntry )
                {
                    newOperationalAttribute.add( value );
                }

                modifications.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, newOperationalAttribute ) );
            }
            else
            {
                modifications.add( new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, operationalAttribute ) );
            }
        }

        return modifications;
    }


    /**
     * Get the list of modification to apply to all the entries
     *
    private List<Modification> getModsOnEntryModification( DN name, Entry oldEntry, Entry newEntry ) throws LdapException
    {
        List<Modification> modList = new ArrayList<Modification>();

        for ( String uuid : subentryCache )
        {
            DN apDn = null; //subentryDn.getParent();
            DN subentryDn = null;
            
            SubtreeSpecification ss = directoryService.getSubentryCache().getSubentry( uuid ).getSubtreeSpecification();
            boolean isOldEntrySelected = evaluator.evaluate( ss, apDn, name, oldEntry );
            boolean isNewEntrySelected = evaluator.evaluate( ss, apDn, name, newEntry );

            if ( isOldEntrySelected == isNewEntrySelected )
            {
                continue;
            }

            // need to remove references to the subentry
            if ( isOldEntrySelected && !isNewEntrySelected )
            {
                for ( AttributeType operationalAttribute : SUBENTRY_OPATTRS )
                {
                    ModificationOperation op = ModificationOperation.REPLACE_ATTRIBUTE;
                    EntryAttribute opAttr = oldEntry.get( operationalAttribute );

                    if ( opAttr != null )
                    {
                        opAttr = opAttr.clone();
                        opAttr.remove( subentryDn.getNormName() );

                        if ( opAttr.size() < 1 )
                        {
                            op = ModificationOperation.REMOVE_ATTRIBUTE;
                        }

                        modList.add( new DefaultModification( op, opAttr ) );
                    }
                }
            }
            // need to add references to the subentry
            else if ( isNewEntrySelected && !isOldEntrySelected )
            {
                for ( AttributeType operationalAttribute : SUBENTRY_OPATTRS )
                {
                    ModificationOperation op = ModificationOperation.ADD_ATTRIBUTE;
                    EntryAttribute opAttr = new DefaultEntryAttribute( operationalAttribute );
                    opAttr.add( subentryDn.getNormName() );
                    modList.add( new DefaultModification( op, opAttr ) );
                }
            }
        }

        return modList;
    }


    /**
     * Get a read-lock on the AP cache.
     * No read operation can be done on the AP cache if this
     * method is not called before.
     */
    public void lockRead()
    {
        mutex.readLock().lock();
    }


    /**
     * Get a write-lock on the AP cache.
     * No write operation can be done on the apCache if this
     * method is not called before.
     */
    public void lockWrite()
    {
        mutex.writeLock().lock();
    }


    /**
     * Release the read-write lock on the AP cache.
     * This method must be called after having read or modified the
     * AP cache
     */
    public void unlock()
    {
        if ( mutex.isWriteLockedByCurrentThread() )
        {
            mutex.writeLock().unlock();
        }
        else
        {
            mutex.readLock().unlock();
        }
    }

    
    /**
     * Create the list of AP for a given entry.
     */
    private void createAdministrativePoints( EntryAttribute adminPoint, DN dn, String uuid, long seqNumber ) throws LdapException
    {
        if ( isAAP( adminPoint ) )
        {
            // The AC AAP
            AccessControlAdministrativePoint acAap = new AccessControlAAP( uuid, seqNumber );
            directoryService.getAccessControlAPCache().add( dn, acAap );

            // The CA AAP
            CollectiveAttributeAdministrativePoint caAap = new CollectiveAttributeAAP( uuid, seqNumber );
            directoryService.getCollectiveAttributeAPCache().add( dn, caAap );

            // The TE AAP
            TriggerExecutionAdministrativePoint teAap = new TriggerExecutionAAP( uuid, seqNumber );
            directoryService.getTriggerExecutionAPCache().add( dn, teAap );

            // The SS AAP
            SubschemaAdministrativePoint ssAap = new SubschemaAAP( uuid, seqNumber );
            directoryService.getSubschemaAPCache().add( dn, ssAap );

            // If it's an AAP, we can get out immediately
            return;
        }

        // Not an AAP
        for ( Value<?> value : adminPoint )
        {
            String role = value.getString();

            // Deal with AccessControl AP
            if ( isAccessControlSpecificRole( role ) )
            {
                AccessControlAdministrativePoint sap = new AccessControlSAP( uuid, seqNumber );
                directoryService.getAccessControlAPCache().add( dn, sap );

                continue;
            }

            if ( isAccessControlInnerRole( role ) )
            {
                AccessControlAdministrativePoint iap = new AccessControlIAP( uuid, seqNumber );
                directoryService.getAccessControlAPCache().add( dn, iap );

                continue;
            }

            // Deal with CollectiveAttribute AP
            if ( isCollectiveAttributeSpecificRole( role ) )
            {
                CollectiveAttributeAdministrativePoint sap = new CollectiveAttributeSAP( uuid, seqNumber );
                directoryService.getCollectiveAttributeAPCache().add( dn, sap );

                continue;
            }

            if ( isCollectiveAttributeInnerRole( role ) )
            {
                CollectiveAttributeAdministrativePoint iap = new CollectiveAttributeIAP( uuid, seqNumber );
                directoryService.getCollectiveAttributeAPCache().add( dn, iap );

                continue;
            }

            // Deal with SubSchema AP
            if ( isSubschemaSpecificRole( role ) )
            {
                SubschemaAdministrativePoint sap = new SubschemaSAP( uuid, seqNumber );
                directoryService.getSubschemaAPCache().add( dn, sap );

                continue;
            }

            // Deal with TriggerExecution AP
            if ( isTriggerExecutionSpecificRole( role ) )
            {
                TriggerExecutionAdministrativePoint sap = new TriggerExecutionSAP( uuid, seqNumber );
                directoryService.getTriggerExecutionAPCache().add( dn, sap );

                continue;
            }

            if ( isTriggerExecutionInnerRole( role ) )
            {
                TriggerExecutionAdministrativePoint iap = new TriggerExecutionIAP( uuid, seqNumber );
                directoryService.getTriggerExecutionAPCache().add( dn, iap );

                continue;
            }
        }

        return;
    }
    
    
    /**
     * Delete the list of AP for a given entry. We can update the cache for each role,
     * as if the AP doe snot have such a role, it won't do anythig anyway
     */
    private void deleteAdministrativePoints( EntryAttribute adminPoint, DN dn ) throws LdapException
    {
        // The AC SAP
        directoryService.getAccessControlAPCache().remove( dn );

        // The CA SAP
        directoryService.getCollectiveAttributeAPCache().remove( dn );

        // The TE SAP
        directoryService.getTriggerExecutionAPCache().remove( dn );

        // The SS SAP
        directoryService.getSubschemaAPCache().remove( dn );
        // If it's an AAP, we can get out immediately
        return;
    }
    
    
    /**
     * Get the AdministrativePoint associated with a subentry
     * @param apDn
     * @return
     */
    private List<AdministrativePoint> getAdministrativePoints( DN apDn )
    {
        List<AdministrativePoint> administrativePoints = new ArrayList<AdministrativePoint>();
        
        AdministrativePoint administrativePoint = directoryService.getAccessControlAPCache().getElement( apDn );
        
        if ( administrativePoint != null )
        {
            administrativePoints.add( administrativePoint );
        }
        
        administrativePoint = directoryService.getCollectiveAttributeAPCache().getElement( apDn );

        if ( administrativePoint != null )
        {
            administrativePoints.add( administrativePoint );
        }
        
        administrativePoint = directoryService.getTriggerExecutionAPCache().getElement( apDn );

        if ( administrativePoint != null )
        {
            administrativePoints.add( administrativePoint );
        }
        
        administrativePoint = directoryService.getSubschemaAPCache().getElement( apDn );

        if ( administrativePoint != null )
        {
            administrativePoints.add( administrativePoint );
        }
        
        return administrativePoints;
    }
    
    
    /**
     * Tells if the subentry's roles point to an AP
     */
    private boolean isValidSubentry( Entry subentry, DN apDn )
    {
        boolean isValid = true;
        
        // Check that the ACAP exists if the AC subentry OC is present in the subentry
        if ( subentry.hasObjectClass( SchemaConstants.ACCESS_CONTROL_SUBENTRY_OC ) ||
             subentry.hasObjectClass( SchemaConstants.ACCESS_CONTROL_SUBENTRY_OC_OID ) )
        {
            isValid &= directoryService.getAccessControlAPCache().hasElement( apDn );
        }

        // Check that the CAAP exists if the CA subentry OC is present in the subentry
        if ( subentry.hasObjectClass( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRY_OC ) ||
             subentry.hasObjectClass( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRY_OC_OID ) )
        {
            isValid &= directoryService.getCollectiveAttributeAPCache().hasElement( apDn );
        }

        // Check that the TEAP exists if the TE subentry OC is present in the subentry
        if ( subentry.hasObjectClass( SchemaConstants.TRIGGER_EXECUTION_SUBENTRY_OC ) ||
             subentry.hasObjectClass( SchemaConstants.TRIGGER_EXECUTION_SUBENTRY_OC_OID ) )
        {
            isValid &= directoryService.getTriggerExecutionAPCache().hasElement( apDn );
        }
        

        // Check that the SSAP exists if the SS subentry OC is present in the subentry
        if ( subentry.hasObjectClass( SchemaConstants.SUBSCHEMA_OC ) ||
             subentry.hasObjectClass( SchemaConstants.SUBSCHEMA_OC_OID ) )
        {
            isValid &= directoryService.getSubschemaAPCache().hasElement( apDn );
        }
        
        return isValid;
    }
    
    
    /**
     * Inject the seqNumbers in an AP
     */
    private long updateAPSeqNumbers( OperationEnum operation, DN apDn, Entry entry, Subentry[] subentries ) throws LdapException
    {
        long seqNumber = directoryService.getNewApSeqNumber();
        String seqNumberStr = Long.toString( seqNumber );
        List<Modification> modifications = new ArrayList<Modification>();

        EntryAttribute newSeqNumber = null;
        
        for ( Subentry subentry : subentries )
        {
            if ( subentry == null )
            {
                continue;
            }
            
            AdministrativePoint adminPoint = null;
            
            switch ( subentry.getAdministrativeRole() )
            {
                case AccessControl :
                    newSeqNumber = new DefaultEntryAttribute( ACCESS_CONTROL_SEQ_NUMBER_AT, seqNumberStr );
                    adminPoint = directoryService.getAccessControlAPCache().getElement( apDn );
                    break;
    
                case CollectiveAttribute :
                    newSeqNumber = new DefaultEntryAttribute( COLLECTIVE_ATTRIBUTE_SEQ_NUMBER_AT, seqNumberStr );
                    adminPoint = directoryService.getCollectiveAttributeAPCache().getElement( apDn );
                    break;
    
                case SubSchema :
                    newSeqNumber = new DefaultEntryAttribute( SUB_SCHEMA_SEQ_NUMBER_AT, seqNumberStr );
                    adminPoint = directoryService.getSubschemaAPCache().getElement( apDn );
                    break;
    
                case TriggerExecution :
                    newSeqNumber = new DefaultEntryAttribute( TRIGGER_EXECUTION_SEQ_NUMBER_AT, seqNumberStr );
                    adminPoint = directoryService.getTriggerExecutionAPCache().getElement( apDn );
                    break;
    
            }
            
            Modification modification = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, newSeqNumber );
            modifications.add( modification );

            // Get back the subentry entryUUID and store it in the subentry if this is an addition
            switch ( operation )
            {
                case ADD :
                    String subentryUuid = entry.get( SchemaConstants.ENTRY_UUID_AT ).getString();
                    subentry.setUuid( subentryUuid );
                    adminPoint.addSubentry( subentry );
                    adminPoint.setSeqNumber( seqNumber );
                    break;
                    
                case REMOVE :
                    adminPoint.setSeqNumber( seqNumber );
                    break;
            }
        }
        
        // Inject the seqNumbers into the parent AP
        ModifyOperationContext modCtx = new ModifyOperationContext( directoryService.getAdminSession() );
        modCtx.setByPassed( BYPASS_INTERCEPTORS );
        modCtx.setDn( apDn );
        modCtx.setModItems( modifications );

        directoryService.getOperationManager().modify( modCtx );
        
        return seqNumber;
    }
    
    
    /**
     * Process the addition of a standard entry, adding the SeqNumber and references
     * to the subentries.
     */
    private void processAddEntry( Entry entry ) throws LdapException
    {
        List<Modification> modificationAcs = updateEntry( entry, directoryService.getAccessControlAPCache(), ACCESS_CONTROL_SEQ_NUMBER_AT, ACCESS_CONTROL_SUBENTRIES_UUID_AT );
        List<Modification> modificationCas = updateEntry( entry, directoryService.getCollectiveAttributeAPCache(), COLLECTIVE_ATTRIBUTE_SEQ_NUMBER_AT, COLLECTIVE_ATTRIBUTE_SUBENTRIES_UUID_AT );
        List<Modification> modificationSss = updateEntry( entry, directoryService.getSubschemaAPCache(), SUB_SCHEMA_SEQ_NUMBER_AT, SUBSCHEMA_SUBENTRY_UUID_AT );
        List<Modification> modificationTes = updateEntry( entry, directoryService.getTriggerExecutionAPCache(), TRIGGER_EXECUTION_SEQ_NUMBER_AT, TRIGGER_EXECUTION_SUBENTRIES_UUID_AT );
        
        if ( ( modificationAcs != null ) || ( modificationCas != null ) || ( modificationSss != null ) || (modificationTes != null ) )
        {
            List<Modification> modifications = new ArrayList<Modification>();
            
            if ( modificationAcs != null )
            {
                modifications.addAll( modificationAcs );
            }
            
            if ( modificationCas != null )
            {
                modifications.addAll( modificationCas );
            }
            
            if ( modificationSss != null )
            {
                modifications.addAll( modificationSss );
            }
            
            if ( modificationTes != null )
            {
                modifications.addAll( modificationTes );
            }
            
            for ( Modification modification : modifications )
            {
                entry.add( modification.getAttribute() );
            }
        }
    }
    
    
    /**
     * Add a reference to the added subentry in each of the AP cache for which the 
     * subentry has a role.
     */
    private void addSubentry( DN apDn, Entry entry, Subentry subentry, long seqNumber ) throws LdapException
    {
        for ( AdministrativeRoleEnum role : getSubentryAdminRoles( entry ) )
        {
            switch ( role )
            {
                case AccessControl :
                     AdministrativePoint apAC = directoryService.getAccessControlAPCache().getElement( apDn );
                     apAC.addSubentry( subentry );
                     apAC.setSeqNumber( seqNumber );
                     break;
                     
                case CollectiveAttribute :
                    AdministrativePoint apCA = directoryService.getCollectiveAttributeAPCache().getElement( apDn );
                    apCA.addSubentry( subentry );
                    apCA.setSeqNumber( seqNumber );
                    break;
                    
                case SubSchema :
                    AdministrativePoint apSS = directoryService.getSubschemaAPCache().getElement( apDn );
                    apSS.addSubentry( subentry );
                    apSS.setSeqNumber( seqNumber );
                    break;
                    
                case TriggerExecution :
                    AdministrativePoint apTE = directoryService.getTriggerExecutionAPCache().getElement( apDn );
                    apTE.addSubentry( subentry );
                    apTE.setSeqNumber( seqNumber );
                    break;
            }
        }
    }
    
    
    /**
     * Remove the reference to the deleted subentry in each of the AP cache for which the 
     * subentry has a role.
     */
    private void deleteSubentry( DN apDn, Entry entry, Subentry subentry ) throws LdapException
    {
        for ( AdministrativeRoleEnum role : getSubentryAdminRoles( entry ) )
        {
            switch ( role )
            {
                case AccessControl :
                     directoryService.getAccessControlAPCache().getElement( apDn ).deleteSubentry( subentry );
                     break;
                     
                case CollectiveAttribute :
                    directoryService.getCollectiveAttributeAPCache().getElement( apDn ).deleteSubentry( subentry );
                    break;
                    
                case SubSchema :
                    directoryService.getSubschemaAPCache().getElement( apDn ).deleteSubentry( subentry );
                    break;
                    
                case TriggerExecution :
                    directoryService.getTriggerExecutionAPCache().getElement( apDn ).deleteSubentry( subentry );
                    break;
                    
            }
        }
    }
    
    
    private void initSeqNumber( Entry entry, EntryAttribute adminPoint ) throws LdapException
    {
        String initSeqNumber = Long.toString( -1L );
        
        if ( adminPoint.contains( AdministrativeRole.AutonomousArea.getRole() ) )
        {
            entry.add( ApacheSchemaConstants.ACCESS_CONTROL_SEQ_NUMBER_AT, initSeqNumber);
            entry.add( ApacheSchemaConstants.COLLECTIVE_ATTRIBUTE_SEQ_NUMBER_AT, initSeqNumber);
            entry.add( ApacheSchemaConstants.SUB_SCHEMA_SEQ_NUMBER_AT, initSeqNumber);
            entry.add( ApacheSchemaConstants.TRIGGER_EXECUTION_SEQ_NUMBER_AT, initSeqNumber);
            
            return;
        }
        
        if ( adminPoint.contains( AdministrativeRole.AccessControlSpecificArea.getRole() ) || 
             adminPoint.contains( AdministrativeRole.AccessControlInnerArea.getRole() ) )
        {
            entry.add( ApacheSchemaConstants.ACCESS_CONTROL_SEQ_NUMBER_AT, initSeqNumber);
        }
        
        if ( adminPoint.contains( AdministrativeRole.CollectiveAttributeSpecificArea.getRole() ) || 
             adminPoint.contains( AdministrativeRole.CollectiveAttributeInnerArea.getRole() ) )
        {
            entry.add( ApacheSchemaConstants.COLLECTIVE_ATTRIBUTE_SEQ_NUMBER_AT, initSeqNumber);
        }
        
        if ( adminPoint.contains( AdministrativeRole.SubSchemaSpecificArea.getRole() )  )
        {
            entry.add( ApacheSchemaConstants.SUB_SCHEMA_SEQ_NUMBER_AT, initSeqNumber);
        }
        
        if ( adminPoint.contains( AdministrativeRole.TriggerExecutionSpecificArea.getRole() ) ||
             adminPoint.contains( AdministrativeRole.TriggerExecutionInnerArea.getRole() ))
        {
            entry.add( ApacheSchemaConstants.TRIGGER_EXECUTION_SEQ_NUMBER_AT, initSeqNumber);
        }
    }


    //-------------------------------------------------------------------------------------------
    // Interceptor API methods
    //-------------------------------------------------------------------------------------------
    /**
     * Add a new entry into the DIT. We deal with the Administrative aspects.
     * We have to manage the three kind of added element :
     * <ul>
     * <li>APs</li>
     * <li>SubEntries</li>
     * <li>Entries</li>
     * </ul>
     * 
     * @param next The next {@link Interceptor} in the chain
     * @param addContext The {@link AddOperationContext} instance
     * @throws LdapException If we had some error while processing the Add operation
     */
    public void add( NextInterceptor next, AddOperationContext addContext ) throws LdapException
    {
        LOG.debug( "Entering into the Subtree Interceptor, addRequest : {}", addContext );
        
        DN dn = addContext.getDn();
        Entry entry = addContext.getEntry();

        boolean isAdmin = addContext.getSession().getAuthenticatedPrincipal().getName().equals(
            ServerDNConstants.ADMIN_SYSTEM_DN_NORMALIZED );

        // Check if we are adding an Administrative Point
        EntryAttribute adminPointAT = entry.get( ADMINISTRATIVE_ROLE_AT );

        // First, deal with an AP addition
        if ( adminPointAT != null )
        {
            // Only admin can add an AP
            if ( !isAdmin )
            {
                String message = "Cannot add the given AdministrativePoint, user is not an Admin";
                LOG.error( message );
                
                throw new LdapUnwillingToPerformException( message );
            }

            LOG.debug( "Addition of an administrative point at {} for the roles {}", dn, adminPointAT );

            try
            {
                // Protect the AP caches against concurrent access
                lockWrite();

                // This is an AP, do the initial check
                if ( !checkIsValidAP( entry ) )
                {
                    String message = "Cannot add the given AP, it's not a valid one :" + entry;
                    LOG.error( message );
                    throw new LdapUnwillingToPerformException( message );
                }
                
                // Add a negative seqNumber for each role this AP manage
                initSeqNumber( entry, adminPointAT );
                
                // Ok, we are golden.
                next.add( addContext );
    
                String apUuid = entry.get( ENTRY_UUID_AT ).getString();
    
                // Now, update the AdminPoint cache
                createAdministrativePoints( adminPointAT, dn, apUuid, -1 );
            }
            finally
            {
                // Release the APCaches lock
                unlock();
            }
            
            LOG.debug( "Added an Administrative Point at {}", dn );
        }
        else if ( entry.contains( OBJECT_CLASS_AT, SchemaConstants.SUBENTRY_OC ) )
        {
            // It's a subentry
            if ( !isAdmin )
            {
                String message = "Cannot add the given Subentry, user is not an Admin";
                LOG.error( message );
                
                throw new LdapUnwillingToPerformException( message );
            }
            
            // Get the administrativePoint role : we must have one immediately
            // upper
            DN apDn = dn.getParent();
            
            try
            {
                // Protect the AP caches while checking the subentry
                lockRead();
    
                if ( !isValidSubentry( entry,  apDn ) )
                {
                    String message = "Cannot add the given Subentry, it does not have a parent AP";
                    LOG.error( message );
                    
                    throw new LdapUnwillingToPerformException( message );
                }
                
                // Create the Subentry
                Subentry[] subentries = createSubentry( entry );

                // Now inject the subentry into the backend
                next.add( addContext );
                    
                // Update the seqNumber and update the parent AP
                updateAPSeqNumbers( OperationEnum.ADD, apDn, entry, subentries );
                
                // Update the subentry cache
                for ( Subentry subentry : subentries )
                {
                    if ( subentry != null )
                    {
                        directoryService.getSubentryCache().addSubentry( dn, subentry );
                    }
                }
            }
            finally
            {
                // Release the APCaches lock
                unlock();
            }
            
            LOG.debug( "Added a subentry at {}", dn );
        }
        else
        {
            // The added entry is a normal entry
            // We have to process the addition for each role
            processAddEntry( entry );

            // Propagate the addition down to the backend.
            next.add( addContext );
        }
    }
    
    
    /**
     * Remove the subentry from the associated AP, and update the AP seqNumber
     */
    private void deleteAPSubentry(DnNode<AdministrativePoint> cache, DN apDn, Subentry subentry )
    {
        AdministrativePoint adminPoint = cache.getElement( apDn );

        Set<Subentry> apSubentries = adminPoint.getSubentries( subentry.getAdministrativeRole() );
        
        for ( Subentry apSubentry : apSubentries )
        {
            if ( apSubentry.equals( subentry ) )
            {
                adminPoint.deleteSubentry( subentry );
                break;
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public void delete( NextInterceptor next, DeleteOperationContext deleteContext ) throws LdapException
    {
        DN dn = deleteContext.getDn();
        Entry entry = deleteContext.getEntry();

        // Check if we are deleting an Administrative Point
        EntryAttribute adminPointAT = entry.get( ADMINISTRATIVE_ROLE_AT );

        boolean isAdmin = deleteContext.getSession().getAuthenticatedPrincipal().getName().equals(
            ServerDNConstants.ADMIN_SYSTEM_DN_NORMALIZED );

        // First, deal with an AP deletion
        if ( adminPointAT != null )
        {
            if ( !isAdmin )
            {
                String message = "Cannot delete the given AdministrativePoint, user is not an Admin";
                LOG.error( message );
                
                throw new LdapUnwillingToPerformException( message );
            }
            
            // It's an AP : we can delete the entry, and if done successfully,
            // we can update the APCache for each role
            next.delete( deleteContext );
            
            // Now, update the AP cache
            deleteAdministrativePoints( adminPointAT, dn );
        }
        else if ( entry.contains( OBJECT_CLASS_AT, SchemaConstants.SUBENTRY_OC ) )
        {
            // It's a subentry. We must be admin to remove it
            if ( !isAdmin )
            {
                String message = "Cannot delete the given Subentry, user is not an Admin";
                LOG.error( message );
                
                throw new LdapUnwillingToPerformException( message );
            }
            
            // Now delete the subentry itself
            next.delete( deleteContext );

            // Get the administrativePoint role : we must have one immediately
            // upper
            DN apDn = dn.getParent();

            // As the deleted entry subentry can handle more than one role, we have to get all
            // the subentries and delete their associated AP (those with the same role)
            Subentry[] subentries = directoryService.getSubentryCache().getSubentries( dn );
            
            for ( Subentry subentry : subentries )
            {
                if ( subentry == null )
                {
                    continue;
                }
                
                switch ( subentry.getAdministrativeRole() )
                {
                    case AccessControl :
                        deleteAPSubentry( directoryService.getAccessControlAPCache(), apDn, subentry );
                        break;
                        
                    case CollectiveAttribute :
                        deleteAPSubentry( directoryService.getCollectiveAttributeAPCache(), apDn, subentry );
                        break;
                        
                    case SubSchema :
                        deleteAPSubentry( directoryService.getSubschemaAPCache(), apDn, subentry );
                        break;
                        
                    case TriggerExecution :
                        deleteAPSubentry( directoryService.getTriggerExecutionAPCache(), apDn, subentry );
                        break;
                        
                }

                // Cleanup the subentry cache
                directoryService.getSubentryCache().removeSubentry( dn );
                
            }
            
            // And finally, update the parent AP SeqNumber for each role the subentry manage
            //Set<AdministrativeRoleEnum> subentryRoles = subentry.getAdministrativeRoles();
            updateAPSeqNumbers( OperationEnum.REMOVE, apDn, entry, subentries );
        }
        else
        {
            // This is a normal entry : propagate the deletion down to the backend
            next.delete( deleteContext );
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    public Entry lookup( NextInterceptor nextInterceptor, LookupOperationContext lookupContext )
        throws LdapException
    {
        Entry entry = nextInterceptor.lookup( lookupContext );

        // We have to update the entry now. At this point, we have no idea if the 
        // returned entry is an AP or a subentry. Check that now
        Entry originalEntry = ((ClonedServerEntry)entry).getOriginalEntry();
        
        if ( originalEntry.containsAttribute( ADMINISTRATIVE_ROLE_AT ) ||
             originalEntry.contains( OBJECT_CLASS_AT, SchemaConstants.SUBENTRY_OC ) )
        {
            // No need to update anything
            return entry;
        }
        
        // This is a normal entry, update its seqNumbers if needed
        List<Modification> modifications = updateEntry( entry );
        
        if ( modifications != null )
        {
            // update the entry
            applyModifications( entry, modifications );
        }

        return entry;
    }
    


    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor list( NextInterceptor nextInterceptor, ListOperationContext listContext )
        throws LdapException
    {
        EntryFilteringCursor cursor = nextInterceptor.list( listContext );

        if ( !isSubentriesControlPresent( listContext ) )
        {
            cursor.addEntryFilter( new HideSubentriesFilter() );
        }

        return cursor;
    }


    /**
     * {@inheritDoc}
     *
    public void modify( NextInterceptor next, ModifyOperationContext modifyContext ) throws LdapException
    {
        DN dn = modifyContext.getDn();
        List<Modification> modifications = modifyContext.getModItems();

        Entry entry = modifyContext.getEntry();

        // We have three types of modifications :
        // 1) A modification applied on a normal entry
        // 2) A modification done on a subentry (the entry will have a 'subentry' ObjectClass)
        // 3) A modification on a normal entry on whch we add a 'subentry' ObjectClass
        // The third case is a transformation of a normal entry to a subentry. Not sure if it's
        // legal ...

        boolean isSubtreeSpecificationModification = false;
        Modification subtreeMod = null;

        // Find the subtreeSpecification
        for ( Modification mod : modifications )
        {
            if ( mod.getAttribute().getAttributeType().equals( SUBTREE_SPECIFICATION_AT ) )
            {
                isSubtreeSpecificationModification = true;
                subtreeMod = mod;
                break;
            }
        }

        boolean containsSubentryOC = entry.contains( OBJECT_CLASS_AT, SchemaConstants.SUBENTRY_OC );

        // Check if we have a modified subentry attribute in a Subentry entry
        if ( containsSubentryOC && isSubtreeSpecificationModification )
        {
            Subentry[] subentry = directoryService.getSubentryCache().removeSubentry( dn );
            SubtreeSpecification ssOld = null; //subentry.getSubtreeSpecification();
            SubtreeSpecification ssNew;

            try
            {
                ssNew = ssParser.parse( subtreeMod.getAttribute().getString() );
            }
            catch ( Exception e )
            {
                String msg = I18n.err( I18n.ERR_71 );
                LOG.error( msg, e );
                throw new LdapInvalidAttributeValueException( ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX, msg );
            }

            subentry[0].setSubtreeSpecification( ssNew );
            directoryService.getSubentryCache().addSubentry( dn, subentry[0]);

            next.modify( modifyContext );

            // search for all entries selected by the old SS and remove references to subentry
            DN apName = dn.getParent();
            DN oldBaseDn = apName;
            oldBaseDn = oldBaseDn.addAll( ssOld.getBase() );

            ExprNode filter = new PresenceNode( OBJECT_CLASS_AT );
            SearchControls controls = new SearchControls();
            controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            controls.setReturningAttributes( new String[]
                { SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES, SchemaConstants.ALL_USER_ATTRIBUTES } );

            SearchOperationContext searchOperationContext = new SearchOperationContext( modifyContext.getSession(),
                oldBaseDn, filter, controls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.NEVER_DEREF_ALIASES );

            EntryFilteringCursor subentries = nexus.search( searchOperationContext );

            try
            {
                while ( subentries.next() )
                {
                    Entry candidate = subentries.get();
                    DN candidateDn = candidate.getDn();

                    if ( evaluator.evaluate( ssOld, apName, candidateDn, candidate ) )
                    {
                        nexus.modify( new ModifyOperationContext( modifyContext.getSession(), candidateDn,
                            getOperationalModsForRemove( dn, candidate ) ) );
                    }
                }
            }
            catch ( Exception e )
            {
                throw new LdapOperationErrorException( e.getMessage() );
            }

            // search for all selected entries by the new SS and add references to subentry
            subentry = directoryService.getSubentryUuidCache().getSubentry( dn.toString() );
            List<EntryAttribute> operationalAttributes = getSubentryOperationalAttributes( dn, subentry );
            DN newBaseDn = apName;
            newBaseDn = newBaseDn.addAll( ssNew.getBase() );

            searchOperationContext = new SearchOperationContext( modifyContext.getSession(), newBaseDn, filter, controls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.NEVER_DEREF_ALIASES );

            subentries = nexus.search( searchOperationContext );

            try
            {
                while ( subentries.next() )
                {
                    Entry candidate = subentries.get();
                    DN candidateDn = candidate.getDn();

                    if ( evaluator.evaluate( ssNew, apName, candidateDn, candidate ) )
                    {
                        nexus.modify( new ModifyOperationContext( modifyContext.getSession(), candidateDn,
                            getOperationalModsForAdd( candidate, operationalAttributes ) ) );
                    }
                }
            }
            catch ( Exception e )
            {
                throw new LdapOperationErrorException( e.getMessage() );
            }
        }
        else
        {
            next.modify( modifyContext );

            if ( !containsSubentryOC )
            {
                Entry newEntry = modifyContext.getAlteredEntry();

                List<Modification> subentriesOpAttrMods = getModsOnEntryModification( dn, entry, newEntry );

                if ( subentriesOpAttrMods.size() > 0 )
                {
                    nexus.modify( new ModifyOperationContext( modifyContext.getSession(), dn, subentriesOpAttrMods ) );
                }
            }
        }
    }


    /**
     * The Move operation for a Subentry will deal with different cases :
     * 1) we move a normal entry
     * 2) we move a subentry
     * 3) we move an administrationPoint
     * <p>
     * <u>Case 1 :</u><br>
     * A normal entry (ie, not a subentry or an AP) may be part of some administrative areas.
     * We have to remove the references to the associated areas if the entry gets out of them.<br>
     * This entry can also be moved to some other administrative area, and it should then be
     * updated to point to the associated subentries.
     * <br><br>
     * There is one preliminary condition : If the entry has a descendant which is an
     * Administrative Point, then the move cannot be done.
     * <br><br>
     * <u>Case 2 :</u><br>
     * The subentry has to be moved under a new AP, otherwise this is an error. Once moved,
     * we have to update all the entries selected by the old subtreeSpecification, removing
     * the references to the subentry from all the selected entry, and update the entries
     * selected by the new subtreeSpecification by adding a reference to the subentry into them.
     * <br><br>
     * <u>Case 3 :</u><br>
     *
     *
     * @param next The next interceptor in the chain
     * @param moveContext The context containing all the needed informations to proceed
     * @throws LdapException If the move failed
     *
    public void move( NextInterceptor next, MoveOperationContext moveContext ) throws LdapException
    {
        DN oldDn = moveContext.getDn();
        DN newSuperiorDn = moveContext.getNewSuperior();

        Entry entry = moveContext.getOriginalEntry();

        if ( entry.contains( OBJECT_CLASS_AT, SchemaConstants.SUBENTRY_OC ) )
        {
            // This is a subentry. Moving a subentry means we have to:
            // o Check that there is a new AP where we move the subentry
            // o Remove the op Attr from all the entry selected by the subentry
            // o Add the op Attr in all the selected entry by the subentry

            // If we move it, we have to check that
            // the new parent is an AP
            //checkAdministrativeRole( moveContext, newSuperiorDn );

            Subentry subentry = directoryService.getSubentryUuidCache().removeSubentry( oldDn.toString() );
            SubtreeSpecification ss = subentry.getSubtreeSpecification();
            DN apName = oldDn.getParent();
            DN baseDn = apName;
            baseDn = baseDn.addAll( ss.getBase() );
            DN newName = newSuperiorDn;
            newName = newName.add( oldDn.getRdn() );
            newName.normalize( schemaManager );

            directoryService.getSubentryUuidCache().addSubentry( subentry );

            next.move( moveContext );

            subentry = directoryService.getSubentryUuidCache().getSubentry( newName.toString() );

            ExprNode filter = new PresenceNode( OBJECT_CLASS_AT );
            SearchControls controls = new SearchControls();
            controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            controls.setReturningAttributes( new String[]
                { SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES, SchemaConstants.ALL_USER_ATTRIBUTES } );

            SearchOperationContext searchOperationContext = new SearchOperationContext( moveContext.getSession(), baseDn,
                filter, controls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.NEVER_DEREF_ALIASES );

            EntryFilteringCursor subentries = nexus.search( searchOperationContext );

            try
            {
                // Modify all the entries under this subentry
                while ( subentries.next() )
                {
                    Entry candidate = subentries.get();
                    DN dn = candidate.getDn();
                    dn.normalize( schemaManager );

                    if ( evaluator.evaluate( ss, apName, dn, candidate ) )
                    {
                        nexus.modify( new ModifyOperationContext( moveContext.getSession(), dn, getOperationalModsForReplace(
                            oldDn, newName, subentry, candidate ) ) );
                    }
                }

                subentries.close();
            }
            catch ( Exception e )
            {
                throw new LdapOperationException( e.getMessage() );
            }
        }
        else
        {
            // A normal entry. It may be part of a SubtreeSpecifciation. In this
            // case, we have to update the opAttrs (removing old ones and adding the
            // new ones)

            // First, an moved entry which has an AP in one of its descendant
            // can't be moved.
            if ( hasAdministrativeDescendant( moveContext, oldDn ) )
            {
                String msg = I18n.err( I18n.ERR_308 );
                LOG.warn( msg );
                throw new LdapSchemaViolationException( ResultCodeEnum.NOT_ALLOWED_ON_RDN, msg );
            }

            // Move the entry
            next.move( moveContext );

            // calculate the new DN now for use below to modify subentry operational
            // attributes contained within this regular entry with name changes
            DN newDn = moveContext.getNewDn();
            List<Modification> mods = getModsOnEntryRdnChange( oldDn, newDn, entry );

            // Update the entry operational attributes
            if ( mods.size() > 0 )
            {
                nexus.modify( new ModifyOperationContext( moveContext.getSession(), newDn, mods ) );
            }
        }
    }


    /**
     * {@inheritDoc}
     *
    public void moveAndRename( NextInterceptor next, MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        DN oldDn = moveAndRenameContext.getDn();
        DN newSuperiorDn = moveAndRenameContext.getNewSuperiorDn();

        Entry entry = moveAndRenameContext.getOriginalEntry();

        if ( entry.contains( OBJECT_CLASS_AT, SchemaConstants.SUBENTRY_OC ) )
        {
            Subentry subentry = directoryService.getSubentryUuidCache().removeSubentry( oldDn.toString() );
            SubtreeSpecification ss = subentry.getSubtreeSpecification();
            DN apName = oldDn.getParent();
            DN baseDn = apName;
            baseDn = baseDn.addAll( ss.getBase() );
            DN newName = newSuperiorDn.getParent();

            newName = newName.add( moveAndRenameContext.getNewRdn() );
            newName.normalize( schemaManager );

            directoryService.getSubentryUuidCache().addSubentry( subentry );

            next.moveAndRename( moveAndRenameContext );

            subentry = subentryCache.getSubentry( newName.toString() );

            ExprNode filter = new PresenceNode( OBJECT_CLASS_AT );
            SearchControls controls = new SearchControls();
            controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            controls.setReturningAttributes( new String[]
                { SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES, SchemaConstants.ALL_USER_ATTRIBUTES } );

            SearchOperationContext searchOperationContext = new SearchOperationContext( moveAndRenameContext.getSession(), baseDn,
                filter, controls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.NEVER_DEREF_ALIASES );

            EntryFilteringCursor subentries = nexus.search( searchOperationContext );

            try
            {
                while ( subentries.next() )
                {
                    Entry candidate = subentries.get();
                    DN dn = candidate.getDn();
                    dn.normalize( schemaManager );

                    if ( evaluator.evaluate( ss, apName, dn, candidate ) )
                    {
                        nexus.modify( new ModifyOperationContext( moveAndRenameContext.getSession(), dn, getOperationalModsForReplace(
                            oldDn, newName, subentry, candidate ) ) );
                    }
                }

                subentries.close();
            }
            catch ( Exception e )
            {
                throw new LdapOperationException( e.getMessage() );
            }
        }
        else
        {
            if ( hasAdministrativeDescendant( moveAndRenameContext, oldDn ) )
            {
                String msg = I18n.err( I18n.ERR_308 );
                LOG.warn( msg );
                throw new LdapSchemaViolationException( ResultCodeEnum.NOT_ALLOWED_ON_RDN, msg );
            }

            next.moveAndRename( moveAndRenameContext );

            // calculate the new DN now for use below to modify subentry operational
            // attributes contained within this regular entry with name changes
            DN newDn = moveAndRenameContext.getNewDn();
            List<Modification> mods = getModsOnEntryRdnChange( oldDn, newDn, entry );

            if ( mods.size() > 0 )
            {
                nexus.modify( new ModifyOperationContext( moveAndRenameContext.getSession(), newDn, mods ) );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public void rename( NextInterceptor next, RenameOperationContext renameContext ) throws LdapException
    {
        LOG.debug( "Entering into the Subtree Interceptor, renameRequest : {}", renameContext );
        DN oldDn = renameContext.getDn();
        RDN oldRdn = oldDn.getRdn();
        RDN newRdn = renameContext.getNewRdn();
        DN newDn = oldDn.getParent().add( newRdn );

        Entry entry = renameContext.getEntry().getClonedEntry();

        boolean isAdmin = renameContext.getSession().getAuthenticatedPrincipal().getName().equals(
            ServerDNConstants.ADMIN_SYSTEM_DN_NORMALIZED );

        // Check if we are adding an Administrative Point
        EntryAttribute adminPointAT = entry.get( ADMINISTRATIVE_ROLE_AT );

        // First, deal with an AP addition
        if ( adminPointAT != null )
        {
            // This is an AP. If it's a SAP, we have nothing to do, as a rename does not modify 
            // the subtree evaluations, nor does it impact any underlying entries. We just have to 
            // update the AP caches.
            // If the AP is an IAP, then as the underlying entries will be modified, then
            // we have to update the IAP seqNumber : the underlying entries might be impacted
            // as the parent's AP for the renamed IAP may have a base or some chopBefore/chopAfter
            // specificExclusion that depend on the old name.
            if ( isIAP( adminPointAT) )
            {
            }
            else
            {
                next.rename( renameContext );
                
                List<AdministrativePoint> adminpoints = getAdministrativePoints( oldDn );
                
                for ( AdministrativePoint adminPoint : adminpoints )
                {
                    switch ( adminPoint.getRole() )
                    {
                        case AccessControlSpecificArea :
                            directoryService.getAccessControlAPCache().remove( oldDn );
                            directoryService.getAccessControlAPCache().add( newDn, adminPoint );
                            break;
                            
                        case CollectiveAttributeSpecificArea :
                            directoryService.getCollectiveAttributeAPCache().remove( oldDn );
                            directoryService.getCollectiveAttributeAPCache().add( newDn, adminPoint );
                            break;
                            
                        case SubSchemaSpecificArea :
                            directoryService.getSubschemaAPCache().remove( oldDn );
                            directoryService.getSubschemaAPCache().add( newDn, adminPoint );
                            break;
                            
                        case TriggerExecutionSpecificArea :
                            directoryService.getTriggerExecutionAPCache().remove( oldDn );
                            directoryService.getTriggerExecutionAPCache().add( newDn, adminPoint );
                            break;
                    }
                }
            }
        }
        else if ( entry.contains( OBJECT_CLASS_AT, SchemaConstants.SUBENTRY_OC ) )
        {
            // First check that the rename is legal : the new RDN must be a valid CN
            AttributeType newAT = directoryService.getSchemaManager().getAttributeType( newRdn.getNormType() );
            
            if ( !CN_AT.equals( newAT ) )
            {
                String message = "Cannot rename a subentry using an AttributeType which is not CN : " + renameContext;
                LOG.error( message );
                throw new LdapUnwillingToPerformException( message );
            }

            // Get the new name
            EntryAttribute newCn = new DefaultEntryAttribute( CN_AT, newRdn.getUpValue() );

            // It's a subentry : we just have to update the subentryCache
            next.rename( renameContext );
            
            // We can update the Subentry cache, removing the old subentry and
            // adding the new subentry with the new CN
            Subentry[] subentries = directoryService.getSubentryCache().removeSubentry( oldDn );

            for ( Subentry subentry : subentries )
            {
                if ( subentry != null )
                {
                    subentry.setCn( newCn );
                    directoryService.getSubentryCache().addSubentry( newDn, subentry );
                }
            }
        }
        else
        {
            // A normal entry
            next.rename( renameContext );
        }
        /*
            Subentry subentry = directoryService.getSubentryUuidCache().removeSubentry( oldDn.toString() );
            SubtreeSpecification ss = subentry.getSubtreeSpecification();
            DN apName = oldDn.getParent();
            DN baseDn = apName;
            baseDn = baseDn.addAll( ss.getBase() );
            DN newName = oldDn.getParent();

            newName = newName.add( renameContext.getNewRdn() );
            newName.normalize( schemaManager );

            directoryService.getSubentryUuidCache().addSubentry( subentry );
            next.rename( renameContext );

            subentry = directoryService.getSubentryUuidCache().getSubentry( newName.toString() );
            ExprNode filter = new PresenceNode( OBJECT_CLASS_AT );
            SearchControls controls = new SearchControls();
            controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            controls.setReturningAttributes( new String[]
                { SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES, SchemaConstants.ALL_USER_ATTRIBUTES } );

            SearchOperationContext searchOperationContext = new SearchOperationContext( renameContext.getSession(), baseDn,
                filter, controls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.NEVER_DEREF_ALIASES );

            EntryFilteringCursor subentries = nexus.search( searchOperationContext );

            try
            {
                while ( subentries.next() )
                {
                    Entry candidate = subentries.get();
                    DN dn = candidate.getDn();
                    dn.normalize( schemaManager );

                    if ( evaluator.evaluate( ss, apName, dn, candidate ) )
                    {
                        nexus.modify( new ModifyOperationContext( renameContext.getSession(), dn, getOperationalModsForReplace(
                            oldDn, newName, subentry, candidate ) ) );
                    }
                }

                subentries.close();
            }
            catch ( Exception e )
            {
                throw new LdapOperationException( e.getMessage() );
            }
        }
        else
        {
            if ( hasAdministrativeDescendant( renameContext, oldDn ) )
            {
                String msg = I18n.err( I18n.ERR_308 );
                LOG.warn( msg );
                throw new LdapSchemaViolationException( ResultCodeEnum.NOT_ALLOWED_ON_RDN, msg );
            }

            next.rename( renameContext );

            // calculate the new DN now for use below to modify subentry operational
            // attributes contained within this regular entry with name changes
            DN newName = renameContext.getNewDn();

            List<Modification> mods = getModsOnEntryRdnChange( oldDn, newName, entry );

            if ( mods.size() > 0 )
            {
                nexus.modify( new ModifyOperationContext( renameContext.getSession(), newName, mods ) );
            }
        }
        */
    }


    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor search( NextInterceptor nextInterceptor, SearchOperationContext searchContext )
        throws LdapException
    {
        EntryFilteringCursor cursor = nextInterceptor.search( searchContext );

        // If the Subentries control is present, we return only the subentries
        if ( isSubentriesControlPresent( searchContext ) )
        {
            cursor.addEntryFilter( new HideEntriesFilter() );
        }
        else
        {
            // If the scope is not OBJECT, we don't return the subentries
            if ( searchContext.getScope() != SearchScope.OBJECT )
            {
                cursor.addEntryFilter( new HideSubentriesFilter() );
                cursor.addEntryFilter( new SeqNumberUpdateFilter() );
            }
            else
            {
                // Return everything
                cursor.addEntryFilter( new SeqNumberUpdateFilter() );
            }
        }

        return cursor;
    }


    //-------------------------------------------------------------------------------------------
    // Shared method
    //-------------------------------------------------------------------------------------------
    /**
     * Evaluates the set of subentry subtrees upon an entry and returns the
     * operational subentry attributes that will be added to the entry if
     * added at the dn specified.
     *
     * @param dn the normalized distinguished name of the entry
     * @param entryAttrs the entry attributes are generated for
     * @return the set of subentry op attrs for an entry
     * @throws Exception if there are problems accessing entry information
     */
    public Entry getSubentryAttributes( DN dn, Entry entryAttrs ) throws LdapException
    {
        Entry subentryAttrs = new DefaultEntry( schemaManager, dn );

        for ( String subentryUuid : subentryCache )
        {
            DN subentryDn = null;
            DN apDn = null; //subentryDn.getParent();
            Subentry subentry = null; //subentryCache.getSubentry( subentryDn );
            SubtreeSpecification ss = subentry.getSubtreeSpecification();

            if ( evaluator.evaluate( ss, apDn, dn, entryAttrs ) )
            {
                EntryAttribute operational;

                if ( subentry.isAccessControlAdminRole() )
                {
                    operational = subentryAttrs.get( ACCESS_CONTROL_SUBENTRIES_AT );

                    if ( operational == null )
                    {
                        operational = new DefaultEntryAttribute( ACCESS_CONTROL_SUBENTRIES_AT );
                        subentryAttrs.put( operational );
                    }

                    operational.add( subentryDn.getNormName() );
                }

                if ( subentry.isSchemaAdminRole() )
                {
                    operational = subentryAttrs.get( SUBSCHEMA_SUBENTRY_AT );

                    if ( operational == null )
                    {
                        operational = new DefaultEntryAttribute( SUBSCHEMA_SUBENTRY_AT );
                        subentryAttrs.put( operational );
                    }

                    operational.add( subentryDn.getNormName() );
                }

                if ( subentry.isCollectiveAdminRole() )
                {
                    operational = subentryAttrs.get( COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );

                    if ( operational == null )
                    {
                        operational = new DefaultEntryAttribute( COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
                        subentryAttrs.put( operational );
                    }

                    operational.add( subentryDn.getNormName() );
                }

                if ( subentry.isTriggersAdminRole() )
                {
                    operational = subentryAttrs.get( TRIGGER_EXECUTION_SUBENTRIES_AT );

                    if ( operational == null )
                    {
                        operational = new DefaultEntryAttribute( TRIGGER_EXECUTION_SUBENTRIES_AT );
                        subentryAttrs.put( operational );
                    }

                    operational.add( subentryDn.getNormName() );
                }
            }
        }

        return subentryAttrs;
    }
}
