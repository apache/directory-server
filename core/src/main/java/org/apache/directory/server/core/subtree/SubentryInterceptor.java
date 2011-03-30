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


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.naming.directory.SearchControls;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DefaultCoreSession;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.LdapPrincipal;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.filtering.EntryFilter;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchingOperationContext;
import org.apache.directory.server.core.partition.ByPassConstants;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.codec.controls.search.subentries.SubentriesDecorator;
import org.apache.directory.shared.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.DefaultEntryAttribute;
import org.apache.directory.shared.ldap.model.entry.DefaultModification;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.EntryAttribute;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.entry.StringValue;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.shared.ldap.model.exception.LdapNoSuchAttributeException;
import org.apache.directory.shared.ldap.model.exception.LdapOperationErrorException;
import org.apache.directory.shared.ldap.model.exception.LdapOperationException;
import org.apache.directory.shared.ldap.model.exception.LdapOtherException;
import org.apache.directory.shared.ldap.model.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.model.filter.EqualityNode;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.filter.PresenceNode;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.message.controls.Subentries;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.subtree.AdministrativeRole;
import org.apache.directory.shared.ldap.model.subtree.SubtreeSpecification;
import org.apache.directory.shared.ldap.model.subtree.SubtreeSpecificationParser;
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
    private static final String SUBENTRY_CONTROL = Subentries.OID;

    /** The set of Subentry operational attributes */
    public static AttributeType[] SUBENTRY_OPATTRS;

    /** the hash mapping the Dn of a subentry to its SubtreeSpecification/types */
    private final SubentryCache subentryCache = new SubentryCache();

    /** The SubTree specification parser instance */
    private SubtreeSpecificationParser ssParser;

    /** The Subtree evaluator instance */
    private SubtreeEvaluator evaluator;

    /** A reference to the nexus for direct backend operations */
    private PartitionNexus nexus;

    /** An enum used for the entries update */
    private enum OperationEnum
    {
        ADD,
        REMOVE,
        REPLACE
    }


    //-------------------------------------------------------------------------------------------
    // Search filter methods
    //-------------------------------------------------------------------------------------------
    /**
     * SearchResultFilter used to filter out subentries based on objectClass values.
     */
    public class HideSubentriesFilter implements EntryFilter
    {
        public boolean accept( SearchingOperationContext searchContext, ClonedServerEntry entry ) throws Exception
        {
            // See if the requested entry is a subentry
            if ( subentryCache.hasSubentry( entry.getDn() ) )
            {
                return false;
            }

            // see if we can use objectclass if present
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
            if ( subentryCache.hasSubentry( entry.getDn() ) )
            {
                return true;
            }

            // see if we can use objectclass if present
            return entry.contains( OBJECT_CLASS_AT, SchemaConstants.SUBENTRY_OC );
        }
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

        nexus = directoryService.getPartitionNexus();

        SUBENTRY_OPATTRS = new AttributeType[]
            {
                ACCESS_CONTROL_SUBENTRIES_AT,
                SUBSCHEMA_SUBENTRY_AT,
                COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT,
                TRIGGER_EXECUTION_SUBENTRIES_AT
            };

        ssParser = new SubtreeSpecificationParser( schemaManager );
        evaluator = new SubtreeEvaluator( schemaManager );

        // prepare to find all subentries in all namingContexts
        Set<String> suffixes = nexus.listSuffixes();
        ExprNode filter = new EqualityNode<String>( OBJECT_CLASS_AT, new StringValue(
            SchemaConstants.SUBENTRY_OC ) );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setReturningAttributes( new String[]
            { SchemaConstants.SUBTREE_SPECIFICATION_AT, SchemaConstants.OBJECT_CLASS_AT } );

        Dn adminDn = directoryService.getDnFactory().create( ServerDNConstants.ADMIN_SYSTEM_DN );

        // search each namingContext for subentries
        for ( String suffix : suffixes )
        {
            Dn suffixDn = directoryService.getDnFactory().create( suffix );

            CoreSession adminSession = new DefaultCoreSession(
                new LdapPrincipal( schemaManager, adminDn, AuthenticationLevel.STRONG ), directoryService );

            SearchOperationContext searchOperationContext = new SearchOperationContext( adminSession, suffixDn, filter,
                controls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.NEVER_DEREF_ALIASES );

            EntryFilteringCursor subentries = nexus.search( searchOperationContext );

            // Loop on all the found Subentries, parse the SubtreeSpecification
            // and store the subentry in the subrentry cache
            try
            {
                while ( subentries.next() )
                {
                    Entry subentry = subentries.get();
                    Dn subentryDn = subentry.getDn();

                    String subtree = subentry.get( SUBTREE_SPECIFICATION_AT ).getString();
                    SubtreeSpecification ss;

                    try
                    {
                        ss = ssParser.parse( subtree );
                    }
                    catch ( Exception e )
                    {
                        LOG.warn( "Failed while parsing subtreeSpecification for " + subentryDn );
                        continue;
                    }

                    Subentry newSubentry = new Subentry();

                    newSubentry.setAdministrativeRoles( getSubentryAdminRoles( subentry ) );
                    newSubentry.setSubtreeSpecification( ss );

                    subentryCache.addSubentry( subentryDn, newSubentry );
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
    private Set<AdministrativeRole> getSubentryAdminRoles( Entry subentry ) throws LdapException
    {
        Set<AdministrativeRole> adminRoles = new HashSet<AdministrativeRole>();

        EntryAttribute oc = subentry.get( OBJECT_CLASS_AT );

        if ( oc == null )
        {
            throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_VIOLATION, I18n.err( I18n.ERR_305 ) );
        }

        if ( oc.contains( SchemaConstants.ACCESS_CONTROL_SUBENTRY_OC ) )
        {
            adminRoles.add( AdministrativeRole.AccessControlInnerArea );
        }

        if ( oc.contains( SchemaConstants.SUBSCHEMA_OC ) )
        {
            adminRoles.add( AdministrativeRole.SubSchemaSpecificArea );
        }

        if ( oc.contains( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRY_OC ) )
        {
            adminRoles.add( AdministrativeRole.CollectiveAttributeSpecificArea );
        }

        if ( oc.contains( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRY_OC ) )
        {
            adminRoles.add( AdministrativeRole.TriggerExecutionInnerArea );
        }

        return adminRoles;
    }


    /**
     * Checks to see if subentries for the search and list operations should be
     * made visible based on the availability of the search request control
     *
     * @param opContext the invocation object to use for determining subentry visibility
     * @return true if subentries should be visible, false otherwise
     * @throws Exception if there are problems accessing request controls
     */
    private boolean isSubentryVisible( OperationContext opContext ) throws LdapException
    {
        if ( !opContext.hasRequestControls() )
        {
            return false;
        }

        // found the subentry request control so we return its value
        if ( opContext.hasRequestControl( SUBENTRY_CONTROL ) )
        {
            SubentriesDecorator subentriesDecorator = ( SubentriesDecorator ) opContext.getRequestControl( SUBENTRY_CONTROL );
            return ( ( Subentries ) subentriesDecorator.getDecorated() ).isVisible();
        }

        return false;
    }

    /**
     * Update all the entries under an AP adding the
     */
    private void updateEntries( OperationEnum operation, CoreSession session, Dn subentryDn, Dn apDn, SubtreeSpecification ss, Dn baseDn, List<EntryAttribute> operationalAttributes  ) throws LdapException
    {
        ExprNode filter = new PresenceNode( OBJECT_CLASS_AT ); // (objectClass=*)
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setReturningAttributes( new String[]
            { SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES, SchemaConstants.ALL_USER_ATTRIBUTES } );

        SearchOperationContext searchOperationContext = new SearchOperationContext( session,
            baseDn, filter, controls );
        searchOperationContext.setAliasDerefMode( AliasDerefMode.NEVER_DEREF_ALIASES );

        EntryFilteringCursor subentries = nexus.search( searchOperationContext );

        try
        {
            while ( subentries.next() )
            {
                Entry candidate = subentries.get();
                Dn candidateDn = candidate.getDn();

                if ( evaluator.evaluate( ss, apDn, candidateDn, candidate ) )
                {
                    List<Modification> modifications = null;

                    switch ( operation )
                    {
                        case ADD :
                            modifications = getOperationalModsForAdd( candidate, operationalAttributes );
                            break;

                        case REMOVE :
                            modifications = getOperationalModsForRemove( subentryDn, candidate );
                            break;

                            /*
                        case REPLACE :
                            modifications = getOperationalModsForReplace( subentryDn, candidate );
                            break;
                            */
                    }

                    LOG.debug( "The entry {} has been evaluated to true for subentry {}", candidate.getDn(), subentryDn );
                    nexus.modify( new ModifyOperationContext( session, candidateDn, modifications ) );
                }
            }
        }
        catch ( Exception e )
        {
            throw new LdapOtherException( e.getMessage() );
        }
    }


    /**
     * Checks if the given Dn is a namingContext
     */
    private boolean isNamingContext( Dn dn ) throws LdapException
    {
        Dn namingContext = nexus.findSuffix( dn );

        return dn.equals( namingContext );
    }


    /**
     * Get the administrativePoint role
     */
    private void checkAdministrativeRole( OperationContext opContext, Dn apDn ) throws LdapException
    {
        Entry administrationPoint = opContext.lookup( apDn, ByPassConstants.LOOKUP_BYPASS );

        // The administrativeRole AT must exist and not be null
        EntryAttribute administrativeRole = administrationPoint.get( ADMINISTRATIVE_ROLE_AT );

        // check that administrativeRole has something valid in it for us
        if ( ( administrativeRole == null ) || ( administrativeRole.size() <= 0 ) )
        {
            LOG.error( "The entry on {} is not an AdministrativePoint", apDn );
            throw new LdapNoSuchAttributeException( I18n.err( I18n.ERR_306, apDn ) );
        }
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
    private boolean hasAdministrativeDescendant( OperationContext opContext, Dn name ) throws LdapException
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


    private List<Modification> getModsOnEntryRdnChange( Dn oldName, Dn newName, Entry entry ) throws LdapException
    {
        List<Modification> modifications = new ArrayList<Modification>();

        /*
         * There are two different situations warranting action.  First if
         * an ss evalutating to true with the old name no longer evalutates
         * to true with the new name.  This would be caused by specific chop
         * exclusions that effect the new name but did not effect the old
         * name. In this case we must remove subentry operational attribute
         * values associated with the dn of that subentry.
         *
         * In the second case an ss selects the entry with the new name when
         * it did not previously with the old name. Again this situation
         * would be caused by chop exclusions. In this case we must add subentry
         * operational attribute values with the dn of this subentry.
         */
        for ( Dn subentryDn : subentryCache )
        {
            Dn apDn = subentryDn.getParent();
            SubtreeSpecification ss = subentryCache.getSubentry( subentryDn ).getSubtreeSpecification();
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


    // -----------------------------------------------------------------------
    // Methods dealing with subentry modification
    // -----------------------------------------------------------------------

    private Set<AdministrativeRole> getSubentryTypes( Entry entry, List<Modification> mods ) throws LdapException
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

        Entry attrs = new DefaultEntry( schemaManager, Dn.ROOT_DSE );
        attrs.put( ocFinalState );
        return getSubentryAdminRoles( attrs );
    }


    /**
     * Update the list of modifications with a modification associated with a specific
     * role, if it's requested.
     */
    private void getOperationalModForReplace( boolean hasRole, AttributeType attributeType, Entry entry, Dn oldDn, Dn newDn, List<Modification> modifications ) 
        throws LdapInvalidAttributeValueException
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
    private List<Modification> getOperationalModsForReplace( Dn oldDn, Dn newDn, Subentry subentry, Entry entry )
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
    private List<EntryAttribute> getSubentryOperationalAttributes( Dn dn, Subentry subentry ) throws LdapException
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
            EntryAttribute tiggerExecutionSubentries = new DefaultEntryAttribute( TRIGGER_EXECUTION_SUBENTRIES_AT, dn.getNormName() );
            attributes.add( tiggerExecutionSubentries );
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
    private List<Modification> getOperationalModsForRemove( Dn subentryDn, Entry candidate ) throws LdapException
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
     */
    private List<Modification> getModsOnEntryModification( Dn name, Entry oldEntry, Entry newEntry ) throws LdapException
    {
        List<Modification> modList = new ArrayList<Modification>();

        for ( Dn subentryDn : subentryCache )
        {
            Dn apDn = subentryDn.getParent();
            SubtreeSpecification ss = subentryCache.getSubentry( subentryDn ).getSubtreeSpecification();
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
     * Update the Operational Attribute with the reference to the subentry
     */
    private void setOperationalAttribute( Entry entry, Dn subentryDn, AttributeType opAttr) throws LdapException
    {
        EntryAttribute operational = entry.get( opAttr );

        if ( operational == null )
        {
            operational = new DefaultEntryAttribute( opAttr );
            entry.put( operational );
        }

        operational.add( subentryDn.getNormName() );
    }


    //-------------------------------------------------------------------------------------------
    // Interceptor API methods
    //-------------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public void add( NextInterceptor next, AddOperationContext addContext ) throws LdapException
    {
        Dn dn = addContext.getDn();
        ClonedServerEntry entry = addContext.getEntry();

        // Check if the added entry is a subentry
        if ( entry.contains( OBJECT_CLASS_AT, SchemaConstants.SUBENTRY_OC ) )
        {
            // get the name of the administrative point and its administrativeRole attributes
            // The AP must be the parent Dn, but we also have to check that the given Dn
            // is not the rootDSE or a NamingContext
            if ( dn.isRootDSE() || isNamingContext( dn ) )
            {
                // Not allowed : we can't get a parent in those cases
                throw new LdapOtherException( "Cannot find an AdministrativePoint for " + dn );
            }

            // Get the administrativePoint role : we must have one immediately
            // upper
            Dn apDn = dn.getParent();
            checkAdministrativeRole( addContext, apDn );

            /* ----------------------------------------------------------------
             * Build the set of operational attributes to be injected into
             * entries that are contained within the subtree represented by this
             * new subentry.  In the process we make sure the proper roles are
             * supported by the administrative point to allow the addition of
             * this new subentry.
             * ----------------------------------------------------------------
             */
            Subentry subentry = new Subentry();
            subentry.setAdministrativeRoles( getSubentryAdminRoles( entry ) );
            List<EntryAttribute> operationalAttributes = getSubentryOperationalAttributes( dn, subentry );

            /* ----------------------------------------------------------------
             * Parse the subtreeSpecification of the subentry and add it to the
             * SubtreeSpecification cache.  If the parse succeeds we continue
             * to add the entry to the DIT.  Thereafter we search out entries
             * to modify the subentry operational attributes of.
             * ----------------------------------------------------------------
             */
            setSubtreeSpecification( subentry, entry );
            subentryCache.addSubentry( dn, subentry );

            // Now inject the subentry into the backend
            next.add( addContext );

            /* ----------------------------------------------------------------
             * Find the baseDn for the subentry and use that to search the tree
             * while testing each entry returned for inclusion within the
             * subtree of the subentry's subtreeSpecification.  All included
             * entries will have their operational attributes merged with the
             * operational attributes calculated above.
             * ----------------------------------------------------------------
             */
            Dn baseDn = apDn;
            baseDn = baseDn.add( subentry.getSubtreeSpecification().getBase() );

            updateEntries( OperationEnum.ADD, addContext.getSession(), dn, apDn, subentry.getSubtreeSpecification(), baseDn, operationalAttributes );

            // Store the newly modified entry into the context for later use in interceptor
            // just in case
            addContext.setEntry( entry );
        }
        else
        {
            // The added entry is not a Subentry.
            // Nevertheless, we have to check if the entry is added into an AdministrativePoint
            // and is associated with some SubtreeSpecification
            // We brutally check *all* the subentries, as we don't hold a hierarchy
            // of AP
            // TODO : add a hierarchy of subentries
            for ( Dn subentryDn : subentryCache )
            {
                Dn apDn = subentryDn.getParent();

                // No need to evaluate the entry if it's not below an AP.
                if ( dn.isDescendantOf( apDn ) )
                {
                    Subentry subentry = subentryCache.getSubentry( subentryDn );
                    SubtreeSpecification ss = subentry.getSubtreeSpecification();

                    // Now, evaluate the entry wrt the subentry ss
                    // and inject a ref to the subentry if it evaluates to true
                    if ( evaluator.evaluate( ss, apDn, dn, entry ) )
                    {

                        if ( subentry.isAccessControlAdminRole() )
                        {
                            setOperationalAttribute( entry, subentryDn, ACCESS_CONTROL_SUBENTRIES_AT );
                        }

                        if ( subentry.isSchemaAdminRole() )
                        {
                            setOperationalAttribute( entry, subentryDn, SUBSCHEMA_SUBENTRY_AT );
                        }

                        if ( subentry.isCollectiveAdminRole() )
                        {
                            setOperationalAttribute( entry, subentryDn, COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
                        }

                        if ( subentry.isTriggersAdminRole() )
                        {
                            setOperationalAttribute( entry, subentryDn, TRIGGER_EXECUTION_SUBENTRIES_AT );
                        }
                    }
                }
            }

            // Now that the entry has been updated with the operational attributes,
            // we can update it into the add context
            addContext.setEntry( entry );

            // Propagate the addition down to the backend.
            next.add( addContext );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void delete( NextInterceptor next, DeleteOperationContext deleteContext ) throws LdapException
    {
        Dn dn = deleteContext.getDn();
        Entry entry = deleteContext.getEntry();

        // If the entry has a "subentry" Objectclass, we can process the entry.
        // We first remove the re
        if ( entry.contains( OBJECT_CLASS_AT, SchemaConstants.SUBENTRY_OC ) )
        {
            Subentry removedSubentry = subentryCache.getSubentry( dn );

            /* ----------------------------------------------------------------
             * Find the baseDn for the subentry and use that to search the tree
             * for all entries included by the subtreeSpecification.  Then we
             * check the entry for subentry operational attribute that contain
             * the Dn of the subentry.  These are the subentry operational
             * attributes we remove from the entry in a modify operation.
             * ----------------------------------------------------------------
             */
            Dn apDn = dn.getParent();
            Dn baseDn = apDn;
            baseDn = baseDn.add( removedSubentry.getSubtreeSpecification().getBase() );

            // Remove all the references to this removed subentry from all the selected entries
            updateEntries( OperationEnum.REMOVE, deleteContext.getSession(), dn, apDn, removedSubentry.getSubtreeSpecification(), baseDn, null );

            // Update the cache
            subentryCache.removeSubentry( dn );

            // Now delete the subentry itself
            next.delete( deleteContext );
        }
        else
        {
            // TODO : deal with AP removal.
            next.delete( deleteContext );
        }
    }


    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor list( NextInterceptor nextInterceptor, ListOperationContext listContext )
        throws LdapException
    {
        EntryFilteringCursor cursor = nextInterceptor.list( listContext );

        if ( !isSubentryVisible( listContext ) )
        {
            cursor.addEntryFilter( new HideSubentriesFilter() );
        }

        return cursor;
    }


    /**
     * {@inheritDoc}
     */
    public void modify( NextInterceptor next, ModifyOperationContext modifyContext ) throws LdapException
    {
        Dn dn = modifyContext.getDn();
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
            Subentry subentry = subentryCache.removeSubentry( dn );
            SubtreeSpecification ssOld = subentry.getSubtreeSpecification();
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

            subentry.setSubtreeSpecification( ssNew );
            subentry.setAdministrativeRoles( getSubentryTypes( entry, modifications ) );
            subentryCache.addSubentry( dn, subentry );

            next.modify( modifyContext );

            // search for all entries selected by the old SS and remove references to subentry
            Dn apName = dn.getParent();
            Dn oldBaseDn = apName;
            oldBaseDn = oldBaseDn.add( ssOld.getBase() );

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
                    Dn candidateDn = candidate.getDn();

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
            subentry = subentryCache.getSubentry( dn );
            List<EntryAttribute> operationalAttributes = getSubentryOperationalAttributes( dn, subentry );
            Dn newBaseDn = apName;
            newBaseDn = newBaseDn.add( ssNew.getBase() );

            searchOperationContext = new SearchOperationContext( modifyContext.getSession(), newBaseDn, filter, controls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.NEVER_DEREF_ALIASES );

            subentries = nexus.search( searchOperationContext );

            try
            {
                while ( subentries.next() )
                {
                    Entry candidate = subentries.get();
                    Dn candidateDn = candidate.getDn();

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
     */
    public void move( NextInterceptor next, MoveOperationContext moveContext ) throws LdapException
    {
        Dn oldDn = moveContext.getDn();
        Dn newSuperiorDn = moveContext.getNewSuperior();

        Entry entry = moveContext.getOriginalEntry();

        if ( entry.contains( OBJECT_CLASS_AT, SchemaConstants.SUBENTRY_OC ) )
        {
            // This is a subentry. Moving a subentry means we have to:
            // o Check that there is a new AP where we move the subentry
            // o Remove the op Attr from all the entry selected by the subentry
            // o Add the op Attr in all the selected entry by the subentry

            // If we move it, we have to check that
            // the new parent is an AP
            checkAdministrativeRole( moveContext, newSuperiorDn );

            Subentry subentry = subentryCache.removeSubentry( oldDn );
            SubtreeSpecification ss = subentry.getSubtreeSpecification();
            Dn apName = oldDn.getParent();
            Dn baseDn = apName;
            baseDn = baseDn.add( ss.getBase() );
            Dn newName = newSuperiorDn;
            newName = newName.add( oldDn.getRdn() );
            newName.apply( schemaManager );

            subentryCache.addSubentry( newName, subentry );

            next.move( moveContext );

            subentry = subentryCache.getSubentry( newName );

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
                    Dn dn = candidate.getDn();
                    dn.apply( schemaManager );

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

            // calculate the new Dn now for use below to modify subentry operational
            // attributes contained within this regular entry with name changes
            Dn newDn = moveContext.getNewDn();
            List<Modification> mods = getModsOnEntryRdnChange( oldDn, newDn, entry );

            // Update the entry operational attributes
            if ( mods.size() > 0 )
            {
                nexus.modify( new ModifyOperationContext( moveContext.getSession(), newDn, mods ) );
            }
        }
    }


    public void moveAndRename( NextInterceptor next, MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        Dn oldDn = moveAndRenameContext.getDn();
        Dn newSuperiorDn = moveAndRenameContext.getNewSuperiorDn();

        Entry entry = moveAndRenameContext.getOriginalEntry();

        if ( entry.contains( OBJECT_CLASS_AT, SchemaConstants.SUBENTRY_OC ) )
        {
            Subentry subentry = subentryCache.removeSubentry( oldDn );
            SubtreeSpecification ss = subentry.getSubtreeSpecification();
            Dn apName = oldDn.getParent();
            Dn baseDn = apName;
            baseDn = baseDn.add( ss.getBase() );
            Dn newName = newSuperiorDn.getParent();

            newName = newName.add( moveAndRenameContext.getNewRdn() );
            newName.apply( schemaManager );

            subentryCache.addSubentry( newName, subentry );

            next.moveAndRename( moveAndRenameContext );

            subentry = subentryCache.getSubentry( newName );

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
                    Dn dn = candidate.getDn();
                    dn.apply( schemaManager );

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

            // calculate the new Dn now for use below to modify subentry operational
            // attributes contained within this regular entry with name changes
            Dn newDn = moveAndRenameContext.getNewDn();
            List<Modification> mods = getModsOnEntryRdnChange( oldDn, newDn, entry );

            if ( mods.size() > 0 )
            {
                nexus.modify( new ModifyOperationContext( moveAndRenameContext.getSession(), newDn, mods ) );
            }
        }
    }


    public void rename( NextInterceptor next, RenameOperationContext renameContext ) throws LdapException
    {
        Dn oldDn = renameContext.getDn();

        Entry entry = renameContext.getEntry().getClonedEntry();

        if ( entry.contains( OBJECT_CLASS_AT, SchemaConstants.SUBENTRY_OC ) )
        {
            // @Todo To be reviewed !!!
            Subentry subentry = subentryCache.removeSubentry( oldDn );
            SubtreeSpecification ss = subentry.getSubtreeSpecification();
            Dn apName = oldDn.getParent();
            Dn baseDn = apName;
            baseDn = baseDn.add( ss.getBase() );
            Dn newName = oldDn.getParent();

            newName = newName.add( renameContext.getNewRdn() );
            newName.apply( schemaManager );

            subentryCache.addSubentry( newName, subentry );
            next.rename( renameContext );

            subentry = subentryCache.getSubentry( newName );
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
                    Dn dn = candidate.getDn();
                    dn.apply( schemaManager );

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

            // calculate the new Dn now for use below to modify subentry operational
            // attributes contained within this regular entry with name changes
            Dn newName = renameContext.getNewDn();

            List<Modification> mods = getModsOnEntryRdnChange( oldDn, newName, entry );

            if ( mods.size() > 0 )
            {
                nexus.modify( new ModifyOperationContext( renameContext.getSession(), newName, mods ) );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor search( NextInterceptor nextInterceptor, SearchOperationContext searchContext )
        throws LdapException
    {
        EntryFilteringCursor cursor = nextInterceptor.search( searchContext );

        // object scope searches by default return subentries
        if ( searchContext.getScope() == SearchScope.OBJECT )
        {
            return cursor;
        }

        // for subtree and one level scope we filter
        if ( !isSubentryVisible( searchContext ) )
        {
            cursor.addEntryFilter( new HideSubentriesFilter() );
        }
        else
        {
            cursor.addEntryFilter( new HideEntriesFilter() );
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
    public Entry getSubentryAttributes( Dn dn, Entry entryAttrs ) throws LdapException
    {
        Entry subentryAttrs = new DefaultEntry( schemaManager, dn );

        for ( Dn subentryDn : subentryCache )
        {
            Dn apDn = subentryDn.getParent();
            Subentry subentry = subentryCache.getSubentry( subentryDn );
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
