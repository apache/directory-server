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
import java.util.Iterator;
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
import org.apache.directory.shared.ldap.exception.LdapNoSuchAttributeException;
import org.apache.directory.shared.ldap.exception.LdapOperationErrorException;
import org.apache.directory.shared.ldap.exception.LdapOperationException;
import org.apache.directory.shared.ldap.exception.LdapOtherException;
import org.apache.directory.shared.ldap.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecification;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecificationParser;
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
    /** the subentry control OID */
    private static final String SUBENTRY_CONTROL = SubentriesControl.CONTROL_OID;

    public static final String AC_AREA = "accessControlSpecificArea";
    public static final String AC_INNERAREA = "accessControlInnerArea";

    public static final String SCHEMA_AREA = "subschemaAdminSpecificArea";

    public static final String COLLECTIVE_AREA = "collectiveAttributeSpecificArea";
    public static final String COLLECTIVE_INNERAREA = "collectiveAttributeInnerArea";

    public static final String TRIGGER_AREA = "triggerExecutionSpecificArea";
    public static final String TRIGGER_INNERAREA = "triggerExecutionInnerArea";

    public static final String[] SUBENTRY_OPATTRS =
        { SchemaConstants.ACCESS_CONTROL_SUBENTRIES_AT, SchemaConstants.SUBSCHEMA_SUBENTRY_AT,
            SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT, SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT };

    private static final Logger LOG = LoggerFactory.getLogger( SubentryInterceptor.class );

    /** the hash mapping the DN of a subentry to its SubtreeSpecification/types */
    private final SubentryCache subentryCache = new SubentryCache();

    private SubtreeSpecificationParser ssParser;
    private SubtreeEvaluator evaluator;
    private PartitionNexus nexus;

    /** The global registries */
    private SchemaManager schemaManager;

    private AttributeType objectClassType;


    public void init( DirectoryService directoryService ) throws LdapException
    {
        super.init( directoryService );
        nexus = directoryService.getPartitionNexus();
        schemaManager = directoryService.getSchemaManager();

        // setup various attribute type values
        objectClassType = schemaManager.lookupAttributeTypeRegistry( schemaManager.getAttributeTypeRegistry()
            .getOidByName( SchemaConstants.OBJECT_CLASS_AT ) );

        ssParser = new SubtreeSpecificationParser( schemaManager );
        evaluator = new SubtreeEvaluator( schemaManager );

        // prepare to find all subentries in all namingContexts
        Set<String> suffixes = nexus.listSuffixes();
        ExprNode filter = new EqualityNode<String>( SchemaConstants.OBJECT_CLASS_AT, new StringValue(
            SchemaConstants.SUBENTRY_OC ) );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setReturningAttributes( new String[]
            { SchemaConstants.SUBTREE_SPECIFICATION_AT, SchemaConstants.OBJECT_CLASS_AT } );

        // search each namingContext for subentries
        for ( String suffix : suffixes )
        {
            DN suffixDn = new DN( suffix );
            suffixDn.normalize( schemaManager.getNormalizerMapping() );

            DN adminDn = new DN( ServerDNConstants.ADMIN_SYSTEM_DN_NORMALIZED );
            adminDn.normalize( schemaManager.getNormalizerMapping() );
            CoreSession adminSession = new DefaultCoreSession(
                new LdapPrincipal( adminDn, AuthenticationLevel.STRONG ), directoryService );

            SearchOperationContext searchOperationContext = new SearchOperationContext( adminSession, suffixDn, filter,
                controls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.NEVER_DEREF_ALIASES );

            EntryFilteringCursor subentries = nexus.search( searchOperationContext );

            try
            {
                while ( subentries.next() )
                {
                    Entry subentry = subentries.get();
                    DN dnName = subentry.getDn();
    
                    String subtree = subentry.get( SchemaConstants.SUBTREE_SPECIFICATION_AT ).getString();
                    SubtreeSpecification ss;
    
                    try
                    {
                        ss = ssParser.parse( subtree );
                    }
                    catch ( Exception e )
                    {
                        LOG.warn( "Failed while parsing subtreeSpecification for " + dnName );
                        continue;
                    }
    
                    dnName.normalize( schemaManager.getNormalizerMapping() );
                    subentryCache.setSubentry( dnName.getNormName(), ss, getSubentryAdminRoles( subentry ) );
                }
                
                subentries.close();
            }
            catch ( Exception e )
            {
                throw new LdapOperationException( e.getMessage() );
            }
        }
    }


    private Set<AdministrativeRole> getSubentryAdminRoles( Entry subentry ) throws LdapException
    {
        Set<AdministrativeRole> adminRoles = new HashSet<AdministrativeRole>();

        EntryAttribute oc = subentry.get( SchemaConstants.OBJECT_CLASS_AT );

        if ( oc == null )
        {
            throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_VIOLATION, I18n.err( I18n.ERR_305 ) );
        }

        if ( oc.contains( SchemaConstants.ACCESS_CONTROL_SUBENTRY_OC ) )
        {
            adminRoles.add( AdministrativeRole.ACCESS_CONTROL_ADMIN_ROLE );
        }

        if ( oc.contains( SchemaConstants.SUBSCHEMA_OC ) )
        {
            adminRoles.add( AdministrativeRole.SUB_SCHEMA_ADMIN_ROLE );
        }

        if ( oc.contains( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRY_OC ) )
        {
            adminRoles.add( AdministrativeRole.COLLECTIVE_ADMIN_ROLE );
        }

        if ( oc.contains( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRY_OC ) )
        {
            adminRoles.add( AdministrativeRole.TRIGGERS_ADMIN_ROLE );
        }

        return adminRoles;
    }


    // -----------------------------------------------------------------------
    // Methods/Code dealing with Subentry Visibility
    // -----------------------------------------------------------------------

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


    /**
     * Checks to see if subentries for the search and list operations should be
     * made visible based on the availability of the search request control
     *
     * @param invocation the invocation object to use for determining subentry visibility
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
            SubentriesControl subentriesControl = ( SubentriesControl ) opContext.getRequestControl( SUBENTRY_CONTROL );
            return subentriesControl.isVisible();
        }

        return false;
    }


    // -----------------------------------------------------------------------
    // Methods dealing with entry and subentry addition
    // -----------------------------------------------------------------------

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
        Iterator<String> list = subentryCache.nameIterator();

        while ( list.hasNext() )
        {
            String subentryDnStr = list.next();
            DN subentryDn = new DN( subentryDnStr );
            DN apDn = subentryDn.getParent();
            Subentry subentry = subentryCache.getSubentry( subentryDnStr );
            SubtreeSpecification ss = subentry.getSubtreeSpecification();

            if ( evaluator.evaluate( ss, apDn, dn, entryAttrs ) )
            {
                EntryAttribute operational;

                if ( subentry.isAccessControlAdminRole() )
                {
                    operational = subentryAttrs.get( SchemaConstants.ACCESS_CONTROL_SUBENTRIES_AT );

                    if ( operational == null )
                    {
                        operational = new DefaultEntryAttribute( SchemaConstants.ACCESS_CONTROL_SUBENTRIES_AT,
                            schemaManager.lookupAttributeTypeRegistry( SchemaConstants.ACCESS_CONTROL_SUBENTRIES_AT ) );
                        subentryAttrs.put( operational );
                    }

                    operational.add( subentryDn.getNormName() );
                }
                
                if ( subentry.isSchemaAdminRole() )
                {
                    operational = subentryAttrs.get( SchemaConstants.SUBSCHEMA_SUBENTRY_AT );

                    if ( operational == null )
                    {
                        operational = new DefaultEntryAttribute( SchemaConstants.SUBSCHEMA_SUBENTRY_AT, schemaManager
                            .lookupAttributeTypeRegistry( SchemaConstants.SUBSCHEMA_SUBENTRY_AT ) );
                        subentryAttrs.put( operational );
                    }

                    operational.add( subentryDn.getNormName() );
                }
                
                if ( subentry.isCollectiveAdminRole() )
                {
                    operational = subentryAttrs.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );

                    if ( operational == null )
                    {
                        operational = new DefaultEntryAttribute( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT,
                            schemaManager
                                .lookupAttributeTypeRegistry( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );
                        subentryAttrs.put( operational );
                    }

                    operational.add( subentryDn.getNormName() );
                }
                
                if ( subentry.isTriggersAdminRole() )
                {
                    operational = subentryAttrs.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );

                    if ( operational == null )
                    {
                        operational = new DefaultEntryAttribute( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT,
                            schemaManager.lookupAttributeTypeRegistry( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ) );
                        subentryAttrs.put( operational );
                    }

                    operational.add( subentryDn.getNormName() );
                }
            }
        }

        return subentryAttrs;
    }


    public void add( NextInterceptor next, AddOperationContext addContext ) throws LdapException
    {
        DN name = addContext.getDn();
        ClonedServerEntry entry = addContext.getEntry();

        EntryAttribute objectClasses = entry.get( SchemaConstants.OBJECT_CLASS_AT );

        if ( objectClasses.contains( SchemaConstants.SUBENTRY_OC ) )
        {
            // get the name of the administrative point and its administrativeRole attributes
            DN apName = name.getParent();
            Entry ap = addContext.lookup( apName, ByPassConstants.LOOKUP_BYPASS );
            
            // The administrativeRole AT must exist and not be null
            EntryAttribute administrativeRole = ap.get( SchemaConstants.ADMINISTRATIVE_ROLE_AT );

            // check that administrativeRole has something valid in it for us
            if ( ( administrativeRole == null ) || ( administrativeRole.size() <= 0 ) )
            {
                throw new LdapNoSuchAttributeException( I18n.err( I18n.ERR_306, apName ) );
            }

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
            Entry operational = getSubentryOperationalAttributes( name, subentry );

            /* ----------------------------------------------------------------
             * Parse the subtreeSpecification of the subentry and add it to the
             * SubtreeSpecification cache.  If the parse succeeds we continue
             * to add the entry to the DIT.  Thereafter we search out entries
             * to modify the subentry operational attributes of.
             * ----------------------------------------------------------------
             */
            String subtree = entry.get( SchemaConstants.SUBTREE_SPECIFICATION_AT ).getString();
            SubtreeSpecification ss;

            try
            {
                ss = ssParser.parse( subtree );
            }
            catch ( Exception e )
            {
                String msg = I18n.err( I18n.ERR_307, name.getName() );
                LOG.warn( msg );
                throw new LdapInvalidAttributeValueException( ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX, msg );
            }

            subentryCache.setSubentry( name.getNormName(), ss, getSubentryAdminRoles( entry ) );

            next.add( addContext );

            /* ----------------------------------------------------------------
             * Find the baseDn for the subentry and use that to search the tree
             * while testing each entry returned for inclusion within the
             * subtree of the subentry's subtreeSpecification.  All included
             * entries will have their operational attributes merged with the
             * operational attributes calculated above.
             * ----------------------------------------------------------------
             */
            DN baseDn = ( DN ) apName.clone();
            baseDn.addAll( ss.getBase() );

            ExprNode filter = new PresenceNode( SchemaConstants.OBJECT_CLASS_AT_OID ); // (objectClass=*)
            SearchControls controls = new SearchControls();
            controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            controls.setReturningAttributes( new String[]
                { SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES, SchemaConstants.ALL_USER_ATTRIBUTES } );

            SearchOperationContext searchOperationContext = new SearchOperationContext( addContext.getSession(),
                baseDn, filter, controls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.NEVER_DEREF_ALIASES );

            EntryFilteringCursor subentries = nexus.search( searchOperationContext );

            try
            {
                while ( subentries.next() )
                {
                    Entry candidate = subentries.get();
                    DN dn = candidate.getDn();
                    dn.normalize( schemaManager.getNormalizerMapping() );
    
                    if ( evaluator.evaluate( ss, apName, dn, candidate ) )
                    {
                        nexus.modify( new ModifyOperationContext( addContext.getSession(), dn, getOperationalModsForAdd(
                            candidate, operational ) ) );
                    }
                }
            }
            catch ( Exception e )
            {
                throw new LdapOtherException( e.getMessage() );
            }

            // TODO why are we doing this here if we got the entry from the 
            // opContext in the first place - got to look into this 
            addContext.setEntry( entry );
        }
        else
        {
            Iterator<String> list = subentryCache.nameIterator();

            while ( list.hasNext() )
            {
                String subentryDnStr = list.next();
                DN subentryDn = new DN( subentryDnStr );
                DN apDn = ( DN ) subentryDn.clone();
                apDn.remove( apDn.size() - 1 );
                Subentry subentry = subentryCache.getSubentry( subentryDnStr );
                SubtreeSpecification ss = subentry.getSubtreeSpecification();

                if ( evaluator.evaluate( ss, apDn, name, entry ) )
                {
                    EntryAttribute operational;

                    if ( subentry.isAccessControlAdminRole() )
                    {
                        operational = entry.get( SchemaConstants.ACCESS_CONTROL_SUBENTRIES_AT );

                        if ( operational == null )
                        {
                            operational = new DefaultEntryAttribute( schemaManager
                                .lookupAttributeTypeRegistry( SchemaConstants.ACCESS_CONTROL_SUBENTRIES_AT ) );
                            entry.put( operational );
                        }

                        operational.add( subentryDn.getNormName() );
                    }

                    if ( subentry.isSchemaAdminRole() )
                    {
                        operational = entry.get( SchemaConstants.SUBSCHEMA_SUBENTRY_AT );

                        if ( operational == null )
                        {
                            operational = new DefaultEntryAttribute( schemaManager
                                .lookupAttributeTypeRegistry( SchemaConstants.SUBSCHEMA_SUBENTRY_AT ) );
                            entry.put( operational );
                        }

                        operational.add( subentryDn.getNormName() );
                    }

                    if ( subentry.isCollectiveAdminRole() )
                    {
                        operational = entry.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );

                        if ( operational == null )
                        {
                            operational = new DefaultEntryAttribute( schemaManager
                                .lookupAttributeTypeRegistry( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );
                            entry.put( operational );
                        }

                        operational.add( subentryDn.getNormName() );
                    }

                    if ( subentry.isTriggersAdminRole() )
                    {
                        operational = entry.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );

                        if ( operational == null )
                        {
                            operational = new DefaultEntryAttribute( schemaManager
                                .lookupAttributeTypeRegistry( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ) );
                            entry.put( operational );
                        }

                        operational.add( subentryDn.getNormName() );
                    }
                }
            }

            // TODO why are we doing this here if we got the entry from the 
            // opContext in the first place - got to look into this 
            addContext.setEntry( entry );

            next.add( addContext );
        }
    }


    // -----------------------------------------------------------------------
    // Methods dealing subentry deletion
    // -----------------------------------------------------------------------

    public void delete( NextInterceptor next, DeleteOperationContext deleteContext ) throws LdapException
    {
        DN name = deleteContext.getDn();
        Entry entry = deleteContext.getEntry();
        EntryAttribute objectClasses = entry.get( objectClassType );

        if ( objectClasses.contains( SchemaConstants.SUBENTRY_OC ) )
        {
            next.delete( deleteContext );

            SubtreeSpecification ss = subentryCache.removeSubentry( name.getNormName() ).getSubtreeSpecification();

            /* ----------------------------------------------------------------
             * Find the baseDn for the subentry and use that to search the tree
             * for all entries included by the subtreeSpecification.  Then we
             * check the entry for subentry operational attribute that contain
             * the DN of the subentry.  These are the subentry operational
             * attributes we remove from the entry in a modify operation.
             * ----------------------------------------------------------------
             */
            DN apName = ( DN ) name.clone();
            apName.remove( name.size() - 1 );
            DN baseDn = ( DN ) apName.clone();
            baseDn.addAll( ss.getBase() );

            ExprNode filter = new PresenceNode( schemaManager.getAttributeTypeRegistry().getOidByName(
                SchemaConstants.OBJECT_CLASS_AT ) );
            SearchControls controls = new SearchControls();
            controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            controls.setReturningAttributes( new String[]
                { SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES, SchemaConstants.ALL_USER_ATTRIBUTES } );

            SearchOperationContext searchOperationContext = new SearchOperationContext( deleteContext.getSession(), baseDn,
                filter, controls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.NEVER_DEREF_ALIASES );

            EntryFilteringCursor subentries = nexus.search( searchOperationContext );

            try
            {
                while ( subentries.next() )
                {
                    Entry candidate = subentries.get();
                    DN dn = new DN( candidate.getDn() );
                    dn.normalize( schemaManager.getNormalizerMapping() );
    
                    if ( evaluator.evaluate( ss, apName, dn, candidate ) )
                    {
                        nexus.modify( new ModifyOperationContext( deleteContext.getSession(), dn, getOperationalModsForRemove(
                            name, candidate ) ) );
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
            next.delete( deleteContext );
        }
    }


    // -----------------------------------------------------------------------
    // Methods dealing subentry name changes
    // -----------------------------------------------------------------------

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
        ExprNode filter = new PresenceNode( SchemaConstants.ADMINISTRATIVE_ROLE_AT );
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


    private List<Modification> getModsOnEntryRdnChange( DN oldName, DN newName, Entry entry ) throws LdapException
    {
        List<Modification> modList = new ArrayList<Modification>();

        /*
         * There are two different situations warranting action.  Firt if
         * an ss evalutating to true with the old name no longer evalutates
         * to true with the new name.  This would be caused by specific chop
         * exclusions that effect the new name but did not effect the old
         * name. In this case we must remove subentry operational attribute
         * values associated with the dn of that subentry.
         *
         * In the second case an ss selects the entry with the new name when
         * it did not previously with the old name.  Again this situation
         * would be caused by chop exclusions. In this case we must add subentry
         * operational attribute values with the dn of this subentry.
         */
        Iterator<String> subentries = subentryCache.nameIterator();

        while ( subentries.hasNext() )
        {
            String subentryDn = subentries.next();
            DN apDn = new DN( subentryDn );
            apDn.remove( apDn.size() - 1 );
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
                for ( String aSUBENTRY_OPATTRS : SUBENTRY_OPATTRS )
                {
                    ModificationOperation op = ModificationOperation.REPLACE_ATTRIBUTE;
                    EntryAttribute opAttr = entry.get( aSUBENTRY_OPATTRS );

                    if ( opAttr != null )
                    {
                        opAttr = opAttr.clone();
                        opAttr.remove( subentryDn );

                        if ( opAttr.size() < 1 )
                        {
                            op = ModificationOperation.REMOVE_ATTRIBUTE;
                        }

                        modList.add( new DefaultModification( op, opAttr ) );
                    }
                }
            }
            // need to add references to the subentry
            else if ( isNewNameSelected && !isOldNameSelected )
            {
                for ( String aSUBENTRY_OPATTRS : SUBENTRY_OPATTRS )
                {
                    ModificationOperation op = ModificationOperation.ADD_ATTRIBUTE;
                    EntryAttribute opAttr = new DefaultEntryAttribute( aSUBENTRY_OPATTRS, schemaManager
                        .lookupAttributeTypeRegistry( aSUBENTRY_OPATTRS ) );
                    opAttr.add( subentryDn );
                    modList.add( new DefaultModification( op, opAttr ) );
                }
            }
        }

        return modList;
    }


    public void rename( NextInterceptor next, RenameOperationContext renameContext ) throws LdapException
    {
        DN oldDn = renameContext.getDn();

        Entry entry = renameContext.getEntry().getClonedEntry();

        EntryAttribute objectClasses = entry.get( objectClassType );

        if ( objectClasses.contains( SchemaConstants.SUBENTRY_OC ) )
        {
            // @Todo To be reviewed !!!
            Subentry subentry = subentryCache.getSubentry( oldDn.getNormName() );
            SubtreeSpecification ss = subentry.getSubtreeSpecification();
            DN apName = ( DN ) oldDn.clone();
            apName.remove( apName.size() - 1 );
            DN baseDn = ( DN ) apName.clone();
            baseDn.addAll( ss.getBase() );
            DN newName = ( DN ) oldDn.clone();
            newName.remove( newName.size() - 1 );

            newName.add( renameContext.getNewRdn() );

            String newNormName = newName.getNormName();
            subentryCache.setSubentry( newNormName, ss, subentry.getAdministrativeRoles() );
            next.rename( renameContext );

            subentry = subentryCache.getSubentry( newNormName );
            ExprNode filter = new PresenceNode( schemaManager.getAttributeTypeRegistry().getOidByName(
                SchemaConstants.OBJECT_CLASS_AT ) );
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
                    dn.normalize( schemaManager.getNormalizerMapping() );
    
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
    }


    public void moveAndRename( NextInterceptor next, MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        DN oldDn = moveAndRenameContext.getDn();
        DN newSuperiorDn = moveAndRenameContext.getNewSuperiorDn();

        Entry entry = moveAndRenameContext.getOriginalEntry();

        EntryAttribute objectClasses = entry.get( objectClassType );

        if ( objectClasses.contains( SchemaConstants.SUBENTRY_OC ) )
        {
            Subentry subentry = subentryCache.getSubentry( oldDn.getNormName() );
            SubtreeSpecification ss = subentry.getSubtreeSpecification();
            DN apName = ( DN ) oldDn.clone();
            apName.remove( apName.size() - 1 );
            DN baseDn = ( DN ) apName.clone();
            baseDn.addAll( ss.getBase() );
            DN newName = ( DN ) newSuperiorDn.clone();
            newName.remove( newName.size() - 1 );

            newName.add( moveAndRenameContext.getNewRdn() );

            String newNormName = newName.getNormName();
            subentryCache.setSubentry( newNormName, ss, subentry.getAdministrativeRoles() );
            next.moveAndRename( moveAndRenameContext );

            subentry = subentryCache.getSubentry( newNormName );

            ExprNode filter = new PresenceNode( schemaManager.getAttributeTypeRegistry().getOidByName(
                SchemaConstants.OBJECT_CLASS_AT ) );
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
                    dn.normalize( schemaManager.getNormalizerMapping() );
    
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
    public void move( NextInterceptor next, MoveOperationContext moveContext ) throws LdapException
    {
        DN oldDn = moveContext.getDn();
        DN newSuperiorDn = moveContext.getNewSuperior();

        Entry entry = moveContext.getOriginalEntry();

        EntryAttribute objectClasses = entry.get( SchemaConstants.OBJECT_CLASS_AT );

        if ( objectClasses.contains( SchemaConstants.SUBENTRY_OC ) )
        {
            Subentry subentry = subentryCache.getSubentry( oldDn.getNormName() );
            SubtreeSpecification ss = subentry.getSubtreeSpecification();
            DN apName = ( DN ) oldDn.clone();
            apName.remove( apName.size() - 1 );
            DN baseDn = ( DN ) apName.clone();
            baseDn.addAll( ss.getBase() );
            DN newName = ( DN ) newSuperiorDn.clone();
            newName.remove( newName.size() - 1 );
            newName.add( newSuperiorDn.get( newSuperiorDn.size() - 1 ) );

            String newNormName = newName.getNormName();
            subentryCache.setSubentry( newNormName, ss, subentry.getAdministrativeRoles() );
            next.move( moveContext );

            subentry = subentryCache.getSubentry( newNormName );

            ExprNode filter = new PresenceNode( SchemaConstants.OBJECT_CLASS_AT );
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
                while ( subentries.next() )
                {
                    Entry candidate = subentries.get();
                    DN dn = candidate.getDn();
                    dn.normalize( schemaManager.getNormalizerMapping() );
    
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
            if ( hasAdministrativeDescendant( moveContext, oldDn ) )
            {
                String msg = I18n.err( I18n.ERR_308 );
                LOG.warn( msg );
                throw new LdapSchemaViolationException( ResultCodeEnum.NOT_ALLOWED_ON_RDN, msg );
            }

            next.move( moveContext );

            // calculate the new DN now for use below to modify subentry operational
            // attributes contained within this regular entry with name changes
            DN newName = moveContext.getNewDn();
            List<Modification> mods = getModsOnEntryRdnChange( oldDn, newName, entry );

            if ( mods.size() > 0 )
            {
                nexus.modify( new ModifyOperationContext( moveContext.getSession(), newName, mods ) );
            }
        }
    }


    // -----------------------------------------------------------------------
    // Methods dealing subentry modification
    // -----------------------------------------------------------------------

    private Set<AdministrativeRole> getSubentryTypes( Entry entry, List<Modification> mods ) throws LdapException
    {
        EntryAttribute ocFinalState = entry.get( SchemaConstants.OBJECT_CLASS_AT ).clone();

        for ( Modification mod : mods )
        {
            if ( mod.getAttribute().getId().equalsIgnoreCase( SchemaConstants.OBJECT_CLASS_AT ) )
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
     * {@inheritDoc}
     */
    public void modify( NextInterceptor next, ModifyOperationContext modifyContext ) throws LdapException
    {
        DN dn = modifyContext.getDn();
        List<Modification> mods = modifyContext.getModItems();

        Entry entry = modifyContext.getEntry();

        EntryAttribute objectClasses = entry.get( objectClassType );
        boolean isSubtreeSpecificationModification = false;
        Modification subtreeMod = null;

        // Find the subtreeSpecification
        for ( Modification mod : mods )
        {
            if ( SchemaConstants.SUBTREE_SPECIFICATION_AT.equalsIgnoreCase( mod.getAttribute().getId() ) )
            {
                isSubtreeSpecificationModification = true;
                subtreeMod = mod;
                break;
            }
        }

        if ( objectClasses.contains( SchemaConstants.SUBENTRY_OC ) && isSubtreeSpecificationModification )
        {
            SubtreeSpecification ssOld = subentryCache.removeSubentry( dn.getNormName() ).getSubtreeSpecification();
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

            subentryCache.setSubentry( dn.getNormName(), ssNew, getSubentryTypes( entry, mods ) );
            next.modify( modifyContext );

            // search for all entries selected by the old SS and remove references to subentry
            DN apName = ( DN ) dn.clone();
            apName.remove( apName.size() - 1 );
            DN oldBaseDn = ( DN ) apName.clone();
            oldBaseDn.addAll( ssOld.getBase() );
            ExprNode filter = new PresenceNode( schemaManager.getAttributeTypeRegistry().getOidByName(
                SchemaConstants.OBJECT_CLASS_AT ) );
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
                    candidateDn.normalize( schemaManager.getNormalizerMapping() );
    
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
            Subentry subentry = subentryCache.getSubentry( dn.getNormName() );
            Entry operational = getSubentryOperationalAttributes( dn, subentry );
            DN newBaseDn = ( DN ) apName.clone();
            newBaseDn.addAll( ssNew.getBase() );

            searchOperationContext = new SearchOperationContext( modifyContext.getSession(), newBaseDn, filter, controls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.NEVER_DEREF_ALIASES );

            subentries = nexus.search( searchOperationContext );

            try
            {
                while ( subentries.next() )
                {
                    Entry candidate = subentries.get();
                    DN candidateDn = candidate.getDn();
                    candidateDn.normalize( schemaManager.getNormalizerMapping() );
    
                    if ( evaluator.evaluate( ssNew, apName, candidateDn, candidate ) )
                    {
                        nexus.modify( new ModifyOperationContext( modifyContext.getSession(), candidateDn,
                            getOperationalModsForAdd( candidate, operational ) ) );
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

            if ( !objectClasses.contains( SchemaConstants.SUBENTRY_OC ) )
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


    // -----------------------------------------------------------------------
    // Utility Methods
    // -----------------------------------------------------------------------

    private List<Modification> getOperationalModsForReplace( DN oldName, DN newName, Subentry subentry, Entry entry )
        throws Exception
    {
        List<Modification> modList = new ArrayList<Modification>();

        EntryAttribute operational;

        if ( subentry.isAccessControlAdminRole() )
        {
            operational = entry.get( SchemaConstants.ACCESS_CONTROL_SUBENTRIES_AT ).clone();

            if ( operational == null )
            {
                operational = new DefaultEntryAttribute( SchemaConstants.ACCESS_CONTROL_SUBENTRIES_AT, schemaManager
                    .lookupAttributeTypeRegistry( SchemaConstants.ACCESS_CONTROL_SUBENTRIES_AT ) );
                operational.add( newName.toString() );
            }
            else
            {
                operational.remove( oldName.toString() );
                operational.add( newName.toString() );
            }

            modList.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, operational ) );
        }

        if ( subentry.isSchemaAdminRole() )
        {
            operational = entry.get( SchemaConstants.SUBSCHEMA_SUBENTRY_AT ).clone();

            if ( operational == null )
            {
                operational = new DefaultEntryAttribute( SchemaConstants.SUBSCHEMA_SUBENTRY_AT, schemaManager
                    .lookupAttributeTypeRegistry( SchemaConstants.SUBSCHEMA_SUBENTRY_AT ) );
                operational.add( newName.toString() );
            }
            else
            {
                operational.remove( oldName.toString() );
                operational.add( newName.toString() );
            }

            modList.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, operational ) );
        }

        if ( subentry.isCollectiveAdminRole() )
        {
            operational = entry.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ).clone();

            if ( operational == null )
            {
                operational = new DefaultEntryAttribute( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT,
                    schemaManager.lookupAttributeTypeRegistry( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );
                operational.add( newName.toString() );
            }
            else
            {
                operational.remove( oldName.toString() );
                operational.add( newName.toString() );
            }

            modList.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, operational ) );
        }

        if ( subentry.isTriggersAdminRole() )
        {
            operational = entry.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ).clone();

            if ( operational == null )
            {
                operational = new DefaultEntryAttribute( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT, schemaManager
                    .lookupAttributeTypeRegistry( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ) );
                operational.add( newName.toString() );
            }
            else
            {
                operational.remove( oldName.toString() );
                operational.add( newName.toString() );
            }

            modList.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, operational ) );
        }

        return modList;
    }


    /**
     * Gets the subschema operational attributes to be added to or removed from
     * an entry selected by a subentry's subtreeSpecification.
     *
     * @param name the normalized distinguished name of the subentry (the value of op attrs)
     * @param subentry the subentry to get attributes from
     * @return the set of attributes to be added or removed from entries
     */
    private Entry getSubentryOperationalAttributes( DN name, Subentry subentry ) throws LdapException
    {
        Entry operational = new DefaultEntry( schemaManager, name );

        if ( subentry.isAccessControlAdminRole() )
        {
            if ( operational.get( SchemaConstants.ACCESS_CONTROL_SUBENTRIES_AT ) == null )
            {
                operational.put( SchemaConstants.ACCESS_CONTROL_SUBENTRIES_AT, name.getNormName() );
            }
            else
            {
                operational.get( SchemaConstants.ACCESS_CONTROL_SUBENTRIES_AT ).add( name.getNormName() );
            }
        }
        
        if ( subentry.isSchemaAdminRole() )
        {
            if ( operational.get( SchemaConstants.SUBSCHEMA_SUBENTRY_AT ) == null )
            {
                operational.put( SchemaConstants.SUBSCHEMA_SUBENTRY_AT, name.getNormName() );
            }
            else
            {
                operational.get( SchemaConstants.SUBSCHEMA_SUBENTRY_AT ).add( name.getNormName() );
            }
        }
        
        if ( subentry.isCollectiveAdminRole() )
        {
            if ( operational.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) == null )
            {
                operational.put( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT, name.getNormName() );
            }
            else
            {
                operational.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ).add( name.getNormName() );
            }
        }
        
        if ( subentry.isTriggersAdminRole() )
        {
            if ( operational.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ) == null )
            {
                operational.put( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT, name.getNormName() );
            }
            else
            {
                operational.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ).add( name.getNormName() );
            }
        }

        return operational;
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
        List<Modification> modList = new ArrayList<Modification>();
        String dn = subentryDn.getNormName();

        for ( String opAttrId : SUBENTRY_OPATTRS )
        {
            EntryAttribute opAttr = candidate.get( opAttrId );

            if ( ( opAttr != null ) && opAttr.contains( dn ) )
            {
                AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( opAttrId );
                EntryAttribute attr = new DefaultEntryAttribute( opAttrId, attributeType, dn );
                modList.add( new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, attr ) );
            }
        }

        return modList;
    }


    /**
     * Calculates the subentry operational attributes to add or replace from
     * a candidate entry selected by a subtree specification.  When a subentry
     * is added or it's specification is modified some entries must have new
     * operational attributes added to it to point back to the associated
     * subentry.  To do so a modify operation must be performed on entries
     * selected by the subtree specification.  This method calculates the
     * modify operation to be performed on the entry.
     *
     * @param entry the entry being modified
     * @param operational the set of operational attributes supported by the AP
     * of the subentry
     * @return the set of modifications needed to update the entry
     * @throws Exception if there are probelms accessing modification items
     */
    public List<Modification> getOperationalModsForAdd( Entry entry, Entry operational ) throws LdapException
    {
        List<Modification> modList = new ArrayList<Modification>();

        for ( AttributeType attributeType : operational.getAttributeTypes() )
        {
            ModificationOperation op = ModificationOperation.REPLACE_ATTRIBUTE;
            EntryAttribute result = new DefaultEntryAttribute( attributeType );
            EntryAttribute opAttrAdditions = operational.get( attributeType );
            EntryAttribute opAttrInEntry = entry.get( attributeType );

            for ( Value<?> value : opAttrAdditions )
            {
                result.add( value );
            }

            if ( opAttrInEntry != null && opAttrInEntry.size() > 0 )
            {
                for ( Value<?> value : opAttrInEntry )
                {
                    result.add( value );
                }
            }
            else
            {
                op = ModificationOperation.ADD_ATTRIBUTE;
            }

            modList.add( new DefaultModification( op, result ) );
        }

        return modList;
    }

    /**
     * SearchResultFilter used to filter out subentries based on objectClass values.
     */
    public class HideSubentriesFilter implements EntryFilter
    {
        public boolean accept( SearchingOperationContext searchContext, ClonedServerEntry entry ) throws Exception
        {
            String dn = entry.getDn().getNormName();

            // see if we can get a match without normalization
            if ( subentryCache.hasSubentry( dn ) )
            {
                return false;
            }

            // see if we can use objectclass if present
            EntryAttribute objectClasses = entry.get( SchemaConstants.OBJECT_CLASS_AT );

            if ( objectClasses != null )
            {
                return !objectClasses.contains( SchemaConstants.SUBENTRY_OC );
            }

            DN ndn = new DN( dn );
            ndn.normalize( schemaManager.getNormalizerMapping() );
            String normalizedDn = ndn.getNormName();
            return !subentryCache.hasSubentry( normalizedDn );
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
            String dn = entry.getDn().getNormName();

            // see if we can get a match without normalization
            if ( subentryCache.hasSubentry( dn ) )
            {
                return true;
            }

            // see if we can use objectclass if present
            EntryAttribute objectClasses = entry.get( SchemaConstants.OBJECT_CLASS_AT );

            if ( objectClasses != null )
            {
                return objectClasses.contains( SchemaConstants.SUBENTRY_OC );
            }

            DN ndn = new DN( dn );
            ndn.normalize( schemaManager.getNormalizerMapping() );
            return subentryCache.hasSubentry( ndn.getNormName() );
        }
    }


    private List<Modification> getModsOnEntryModification( DN name, Entry oldEntry, Entry newEntry ) throws LdapException
    {
        List<Modification> modList = new ArrayList<Modification>();

        Iterator<String> subentries = subentryCache.nameIterator();

        while ( subentries.hasNext() )
        {
            String subentryDn = subentries.next();
            DN apDn = new DN( subentryDn );
            apDn.remove( apDn.size() - 1 );
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
                for ( String aSUBENTRY_OPATTRS : SUBENTRY_OPATTRS )
                {
                    ModificationOperation op = ModificationOperation.REPLACE_ATTRIBUTE;
                    EntryAttribute opAttr = oldEntry.get( aSUBENTRY_OPATTRS );

                    if ( opAttr != null )
                    {
                        opAttr = opAttr.clone();
                        opAttr.remove( subentryDn );

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
                for ( String attribute : SUBENTRY_OPATTRS )
                {
                    ModificationOperation op = ModificationOperation.ADD_ATTRIBUTE;
                    AttributeType type = schemaManager.lookupAttributeTypeRegistry( attribute );
                    EntryAttribute opAttr = new DefaultEntryAttribute( attribute, type );
                    opAttr.add( subentryDn );
                    modList.add( new DefaultModification( op, opAttr ) );
                }
            }
        }

        return modList;
    }
}
