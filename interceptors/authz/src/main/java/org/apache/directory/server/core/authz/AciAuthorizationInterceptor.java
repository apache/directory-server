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
package org.apache.directory.server.core.authz;


import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.naming.directory.SearchControls;

import org.apache.directory.api.ldap.aci.ACIItem;
import org.apache.directory.api.ldap.aci.ACIItemParser;
import org.apache.directory.api.ldap.aci.ACITuple;
import org.apache.directory.api.ldap.aci.MicroOperation;
import org.apache.directory.api.ldap.model.constants.Loggers;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.StringValue;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapNoPermissionException;
import org.apache.directory.api.ldap.model.exception.LdapOperationErrorException;
import org.apache.directory.api.ldap.model.exception.LdapOperationException;
import org.apache.directory.api.ldap.model.filter.EqualityNode;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.OrNode;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.normalizers.ConcreteNameComponentNormalizer;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InterceptorEnum;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.core.api.entry.ServerEntryUtils;
import org.apache.directory.server.core.api.filtering.EntryFilter;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.HasEntryOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.OperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.partition.PartitionNexus;
import org.apache.directory.server.core.api.subtree.SubentryUtils;
import org.apache.directory.server.core.authz.support.ACDFEngine;
import org.apache.directory.server.core.authz.support.AciContext;
import org.apache.directory.server.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An ACI based authorization service.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AciAuthorizationInterceptor extends BaseInterceptor
{
    /** the logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( AciAuthorizationInterceptor.class );

    /** the dedicated logger for ACI */
    private static final Logger ACI_LOG = LoggerFactory.getLogger( Loggers.ACI_LOG.getName() );

    private static final Collection<MicroOperation> ADD_PERMS;
    private static final Collection<MicroOperation> READ_PERMS;
    private static final Collection<MicroOperation> COMPARE_PERMS;
    private static final Collection<MicroOperation> SEARCH_ENTRY_PERMS;
    private static final Collection<MicroOperation> SEARCH_ATTRVAL_PERMS;
    private static final Collection<MicroOperation> REMOVE_PERMS;
    private static final Collection<MicroOperation> BROWSE_PERMS;
    private static final Collection<MicroOperation> LOOKUP_PERMS;
    private static final Collection<MicroOperation> REPLACE_PERMS;
    private static final Collection<MicroOperation> RENAME_PERMS;
    private static final Collection<MicroOperation> EXPORT_PERMS;
    private static final Collection<MicroOperation> IMPORT_PERMS;
    private static final Collection<MicroOperation> MOVERENAME_PERMS;

    static
    {
        Set<MicroOperation> set = new HashSet<MicroOperation>( 2 );
        set.add( MicroOperation.BROWSE );
        set.add( MicroOperation.RETURN_DN );
        SEARCH_ENTRY_PERMS = Collections.unmodifiableCollection( set );

        set = new HashSet<MicroOperation>( 2 );
        set.add( MicroOperation.READ );
        set.add( MicroOperation.BROWSE );
        LOOKUP_PERMS = Collections.unmodifiableCollection( set );

        set = new HashSet<MicroOperation>( 2 );
        set.add( MicroOperation.ADD );
        set.add( MicroOperation.REMOVE );
        REPLACE_PERMS = Collections.unmodifiableCollection( set );

        set = new HashSet<MicroOperation>( 2 );
        set.add( MicroOperation.EXPORT );
        set.add( MicroOperation.RENAME );
        MOVERENAME_PERMS = Collections.unmodifiableCollection( set );

        SEARCH_ATTRVAL_PERMS = Collections.singleton( MicroOperation.READ );
        ADD_PERMS = Collections.singleton( MicroOperation.ADD );
        READ_PERMS = Collections.singleton( MicroOperation.READ );
        COMPARE_PERMS = Collections.singleton( MicroOperation.COMPARE );
        REMOVE_PERMS = Collections.singleton( MicroOperation.REMOVE );
        BROWSE_PERMS = Collections.singleton( MicroOperation.BROWSE );
        RENAME_PERMS = Collections.singleton( MicroOperation.RENAME );
        EXPORT_PERMS = Collections.singleton( MicroOperation.EXPORT );
        IMPORT_PERMS = Collections.singleton( MicroOperation.IMPORT );
    }

    /** a tupleCache that responds to add, delete, and modify attempts */
    private TupleCache tupleCache;

    /** a groupCache that responds to add, delete, and modify attempts */
    private GroupCache groupCache;

    /** a normalizing ACIItem parser */
    private ACIItemParser aciParser;

    /** use and instance of the ACDF engine */
    private ACDFEngine engine;

    /** the system wide subschemaSubentryDn */
    private String subschemaSubentryDn;

    /** A reference to the nexus for direct backend operations */
    private PartitionNexus nexus;

    public static final SearchControls DEFAULT_SEARCH_CONTROLS = new SearchControls();

    /** The SubentryUtils instance */
    private static SubentryUtils subentryUtils;


    /**
     * Create a AciAuthorizationInterceptor instance
     */
    public AciAuthorizationInterceptor()
    {
        super( InterceptorEnum.ACI_AUTHORIZATION_INTERCEPTOR );
    }


    /**
     * Load the Tuples into the cache
     */
    private void initTupleCache() throws LdapException
    {
        // Load all the prescriptiveACI : they are stored in AccessControlSubentry entries
        Dn adminDn = new Dn( schemaManager, ServerDNConstants.ADMIN_SYSTEM_DN );

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setReturningAttributes( new String[]
            { SchemaConstants.PRESCRIPTIVE_ACI_AT } );

        ExprNode filter =
            new EqualityNode<String>( OBJECT_CLASS_AT, new StringValue( SchemaConstants.ACCESS_CONTROL_SUBENTRY_OC ) );

        CoreSession adminSession = directoryService.getAdminSession();

        SearchOperationContext searchOperationContext = new SearchOperationContext( adminSession, Dn.ROOT_DSE, filter,
            controls );

        searchOperationContext.setAliasDerefMode( AliasDerefMode.NEVER_DEREF_ALIASES );

        EntryFilteringCursor results = nexus.search( searchOperationContext );

        try
        {
            while ( results.next() )
            {
                Entry entry = results.get();

                tupleCache.subentryAdded( entry.getDn(), entry );
            }

            results.close();
        }
        catch ( Exception e )
        {
            throw new LdapOperationException( e.getMessage(), e );
        }
    }


    /**
     * Load the Groups into the cache
     */
    private void initGroupCache() throws LdapException
    {
        // Load all the member/uniqueMember : they are stored in groupOfNames/groupOfUniqueName
        Dn adminDn = new Dn( schemaManager, ServerDNConstants.ADMIN_SYSTEM_DN );

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setReturningAttributes( new String[]
            { SchemaConstants.MEMBER_AT, SchemaConstants.UNIQUE_MEMBER_AT } );

        ExprNode filter =
            new OrNode(
                new EqualityNode<String>( OBJECT_CLASS_AT, new StringValue( SchemaConstants.GROUP_OF_NAMES_OC ) ),
                new EqualityNode<String>( OBJECT_CLASS_AT, new StringValue( SchemaConstants.GROUP_OF_UNIQUE_NAMES_OC ) ) );

        CoreSession adminSession = directoryService.getAdminSession();

        SearchOperationContext searchOperationContext = new SearchOperationContext( adminSession, Dn.ROOT_DSE, filter,
            controls );

        searchOperationContext.setAliasDerefMode( AliasDerefMode.NEVER_DEREF_ALIASES );

        EntryFilteringCursor results = nexus.search( searchOperationContext );

        try
        {
            while ( results.next() )
            {
                Entry entry = results.get();

                groupCache.groupAdded( entry.getDn(), entry );
            }

            results.close();
        }
        catch ( Exception e )
        {
            throw new LdapOperationException( e.getMessage(), e );
        }
    }


    /**
     * Initializes this interceptor based service by getting a handle on the nexus, setting up
     * the tuple and group membership caches, the ACIItem parser and the ACDF engine.
     *
     * @param directoryService the directory service core
     * @throws Exception if there are problems during initialization
     */
    public void init( DirectoryService directoryService ) throws LdapException
    {
        LOG.debug( "Initializing the AciAuthorizationInterceptor" );

        super.init( directoryService );

        nexus = directoryService.getPartitionNexus();

        CoreSession adminSession = directoryService.getAdminSession();

        // Create the caches
        tupleCache = new TupleCache( adminSession );
        groupCache = new GroupCache( directoryService );

        // Iitialize the ACI PARSER and ACDF engine
        aciParser = new ACIItemParser( new ConcreteNameComponentNormalizer( schemaManager ), schemaManager );
        engine = new ACDFEngine( schemaManager );

        // stuff for dealing with subentries (garbage for now)
        Value<?> subschemaSubentry = directoryService.getPartitionNexus().getRootDseValue( SUBSCHEMA_SUBENTRY_AT );
        Dn subschemaSubentryDnName = dnFactory.create( subschemaSubentry.getString() );
        subschemaSubentryDn = subschemaSubentryDnName.getNormName();

        // Init the caches now
        initTupleCache();
        initGroupCache();

        // Init the SubentryUtils instance
        subentryUtils = new SubentryUtils( directoryService );
    }


    private void protectCriticalEntries( OperationContext opCtx, Dn dn ) throws LdapException
    {
        Dn principalDn = getPrincipal( opCtx ).getDn();

        if ( dn.isEmpty() )
        {
            String msg = I18n.err( I18n.ERR_8 );
            LOG.error( msg );
            throw new LdapNoPermissionException( msg );
        }

        if ( isTheAdministrator( dn ) )
        {
            String msg = I18n.err( I18n.ERR_9, principalDn.getName(), dn.getName() );
            LOG.error( msg );
            throw new LdapNoPermissionException( msg );
        }
    }


    /**
     * Adds perscriptiveACI tuples to a collection of tuples by accessing the
     * tupleCache.  The tuple cache is accessed for each A/C subentry
     * associated with the protected entry.  Note that subentries are handled
     * differently: their parent, the administrative entry is accessed to
     * determine the perscriptiveACIs effecting the AP and hence the subentry
     * which is considered to be in the same context.
     *
     * @param tuples the collection of tuples to add to
     * @param dn the normalized distinguished name of the protected entry
     * @param entry the target entry that access to is being controled
     * @throws Exception if there are problems accessing attribute values
     * @param proxy the partition nexus proxy object
     */
    private void addPerscriptiveAciTuples( OperationContext opContext, Collection<ACITuple> tuples, Dn dn, Entry entry )
        throws LdapException
    {
        Entry originalEntry = null;

        if ( entry instanceof ClonedServerEntry )
        {
            originalEntry = ( ( ClonedServerEntry ) entry ).getOriginalEntry();
        }
        else
        {
            originalEntry = entry;
        }

        Attribute oc = originalEntry.get( OBJECT_CLASS_AT );

        /*
         * If the protected entry is a subentry, then the entry being evaluated
         * for perscriptiveACIs is in fact the administrative entry.  By
         * substituting the administrative entry for the actual subentry the
         * code below this "if" statement correctly evaluates the effects of
         * perscriptiveACI on the subentry.  Basically subentries are considered
         * to be in the same naming context as their access point so the subentries
         * effecting their parent entry applies to them as well.
         */
        if ( oc.contains( SchemaConstants.SUBENTRY_OC ) )
        {
            Dn parentDn = dn.getParent();
            CoreSession session = opContext.getSession();
            LookupOperationContext lookupContext = new LookupOperationContext( session, parentDn,
                SchemaConstants.ALL_ATTRIBUTES_ARRAY );

            originalEntry = directoryService.getPartitionNexus().lookup( lookupContext );
        }

        Attribute subentries = originalEntry.get( ACCESS_CONTROL_SUBENTRIES_AT );

        if ( subentries == null )
        {
            return;
        }

        for ( Value<?> value : subentries )
        {
            String subentryDn = value.getString();
            tuples.addAll( tupleCache.getACITuples( subentryDn ) );
        }
    }


    /**
     * Adds the set of entryACI tuples to a collection of tuples.  The entryACI
     * is parsed and tuples are generated on they fly then added to the collection.
     *
     * @param tuples the collection of tuples to add to
     * @param entry the target entry that access to is being regulated
     * @throws Exception if there are problems accessing attribute values
     */
    private void addEntryAciTuples( Collection<ACITuple> tuples, Entry entry ) throws LdapException
    {
        Attribute entryAci = entry.get( ENTRY_ACI_AT );

        if ( entryAci == null )
        {
            return;
        }

        for ( Value<?> value : entryAci )
        {
            String aciString = value.getString();
            ACIItem item;

            try
            {
                item = aciParser.parse( aciString );
            }
            catch ( ParseException e )
            {
                String msg = I18n.err( I18n.ERR_10, aciString );
                LOG.error( msg, e );
                throw new LdapOperationErrorException( msg );
            }

            tuples.addAll( item.toTuples() );
        }
    }


    /**
     * Adds the set of subentryACI tuples to a collection of tuples.  The subentryACI
     * is parsed and tuples are generated on the fly then added to the collection.
     *
     * @param tuples the collection of tuples to add to
     * @param dn the normalized distinguished name of the protected entry
     * @param entry the target entry that access to is being regulated
     * @throws Exception if there are problems accessing attribute values
     * @param proxy the partition nexus proxy object
     */
    private void addSubentryAciTuples( OperationContext opContext, Collection<ACITuple> tuples, Dn dn, Entry entry )
        throws LdapException
    {
        // only perform this for subentries
        if ( !entry.contains( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.SUBENTRY_OC ) )
        {
            return;
        }

        // get the parent or administrative entry for this subentry since it
        // will contain the subentryACI attributes that effect subentries
        Dn parentDn = dn.getParent();

        CoreSession session = opContext.getSession();
        LookupOperationContext lookupContext = new LookupOperationContext( session, parentDn,
            SchemaConstants.ALL_ATTRIBUTES_ARRAY );

        Entry administrativeEntry = ( ( ClonedServerEntry ) directoryService.getPartitionNexus().lookup( lookupContext ) )
            .getOriginalEntry();

        Attribute subentryAci = administrativeEntry.get( SUBENTRY_ACI_AT );

        if ( subentryAci == null )
        {
            return;
        }

        for ( Value<?> value : subentryAci )
        {
            String aciString = value.getString();
            ACIItem item;

            try
            {
                item = aciParser.parse( aciString );
            }
            catch ( ParseException e )
            {
                String msg = I18n.err( I18n.ERR_11, aciString );
                LOG.error( msg, e );
                throw new LdapOperationErrorException( msg );
            }

            tuples.addAll( item.toTuples() );
        }
    }


    /* -------------------------------------------------------------------------------
     * Within every access controled interceptor method we must retrieve the ACITuple
     * set for all the perscriptiveACIs that apply to the candidate, the target entry
     * operated upon.  This ACITuple set is gotten from the TupleCache by looking up
     * the subentries referenced by the accessControlSubentries operational attribute
     * within the target entry.
     *
     * Then the entry is inspected for an entryACI.  This is not done for the add op
     * since it could introduce a security breech.  So for non-add ops if present a
     * set of ACITuples are generated for all the entryACIs within the entry.  This
     * set is combined with the ACITuples cached for the perscriptiveACI affecting
     * the target entry.  If the entry is a subentry the ACIs are also processed for
     * the subentry to generate more ACITuples.  This subentry TupleACI set is joined
     * with the entry and perscriptive ACI.
     *
     * The union of ACITuples are fed into the engine along with other parameters
     * to decide whether a permission is granted or rejected for the specific
     * operation.
     * -------------------------------------------------------------------------------
     */
    /**
     * {@inheritDoc}
     */
    public void add( AddOperationContext addContext ) throws LdapException
    {
        // bypass authz code if it was disabled
        if ( !directoryService.isAccessControlEnabled() )
        {
            ACI_LOG.debug( "ACI interceptor disabled" );
            next( addContext );
            return;
        }

        ACI_LOG.debug( "Adding the entry {}", addContext.getEntry() );

        // Access the principal requesting the operation, and bypass checks if it is the admin
        LdapPrincipal principal = addContext.getSession().getEffectivePrincipal();
        Dn principalDn = principal.getDn();

        Entry serverEntry = addContext.getEntry();

        Dn dn = addContext.getDn();

        // bypass authz code but manage caches if operation is performed by the admin
        if ( isPrincipalAnAdministrator( principalDn ) )
        {
            ACI_LOG.debug( "Addition done by the administartor : no check" );

            next( addContext );
            tupleCache.subentryAdded( dn, serverEntry );
            groupCache.groupAdded( dn, serverEntry );
            return;
        }

        // perform checks below here for all non-admin users
        Entry subentry = subentryUtils.getSubentryAttributes( dn, serverEntry );

        for ( Attribute attribute : serverEntry )
        {
            subentry.put( attribute );
        }

        // Assemble all the information required to make an access control decision
        Set<Dn> userGroups = groupCache.getGroups( principalDn.getNormName() );
        Collection<ACITuple> tuples = new HashSet<ACITuple>();

        // Build the total collection of tuples to be considered for add rights
        // NOTE: entryACI are NOT considered in adds (it would be a security breech)
        addPerscriptiveAciTuples( addContext, tuples, dn, subentry );
        addSubentryAciTuples( addContext, tuples, dn, subentry );

        // check if entry scope permission is granted
        AciContext entryAciCtx = new AciContext( schemaManager, addContext );
        entryAciCtx.setUserGroupNames( userGroups );
        entryAciCtx.setUserDn( principalDn );
        entryAciCtx.setAuthenticationLevel( principal.getAuthenticationLevel() );
        entryAciCtx.setEntryDn( dn );
        entryAciCtx.setMicroOperations( ADD_PERMS );
        entryAciCtx.setAciTuples( tuples );
        entryAciCtx.setEntry( subentry );

        engine.checkPermission( entryAciCtx );

        // now we must check if attribute type and value scope permission is granted
        for ( Attribute attribute : serverEntry )
        {
            for ( Value<?> value : attribute )
            {
                AciContext attrAciContext = new AciContext( schemaManager, addContext );
                attrAciContext.setUserGroupNames( userGroups );
                attrAciContext.setUserDn( principalDn );
                attrAciContext.setAuthenticationLevel( principal.getAuthenticationLevel() );
                attrAciContext.setEntryDn( dn );
                attrAciContext.setAttributeType( attribute.getAttributeType() );
                attrAciContext.setAttrValue( value );
                attrAciContext.setMicroOperations( ADD_PERMS );
                attrAciContext.setAciTuples( tuples );
                attrAciContext.setEntry( serverEntry );

                engine.checkPermission( attrAciContext );
            }
        }

        // if we've gotten this far then access has been granted
        next( addContext );

        // if the entry added is a subentry or a groupOf[Unique]Names we must
        // update the ACITuple cache and the groups cache to keep them in sync
        tupleCache.subentryAdded( dn, serverEntry );
        groupCache.groupAdded( dn, serverEntry );
    }


    /**
     * {@inheritDoc}
     */
    public boolean compare( CompareOperationContext compareContext ) throws LdapException
    {
        CoreSession session = compareContext.getSession();
        Dn dn = compareContext.getDn();
        String oid = compareContext.getOid();

        Entry entry = compareContext.getOriginalEntry();

        LdapPrincipal principal = session.getEffectivePrincipal();
        Dn principalDn = principal.getDn();

        if ( isPrincipalAnAdministrator( principalDn ) || !directoryService.isAccessControlEnabled() )
        {
            return next( compareContext );
        }

        Set<Dn> userGroups = groupCache.getGroups( principalDn.getNormName() );
        Collection<ACITuple> tuples = new HashSet<ACITuple>();
        addPerscriptiveAciTuples( compareContext, tuples, dn, entry );
        addEntryAciTuples( tuples, entry );
        addSubentryAciTuples( compareContext, tuples, dn, entry );

        AciContext aciContext = new AciContext( schemaManager, compareContext );
        aciContext.setUserGroupNames( userGroups );
        aciContext.setUserDn( principalDn );
        aciContext.setAuthenticationLevel( principal.getAuthenticationLevel() );
        aciContext.setEntryDn( dn );
        aciContext.setMicroOperations( READ_PERMS );
        aciContext.setAciTuples( tuples );
        aciContext.setEntry( entry );

        engine.checkPermission( aciContext );

        AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( oid );

        aciContext = new AciContext( schemaManager, compareContext );
        aciContext.setUserGroupNames( userGroups );
        aciContext.setUserDn( principalDn );
        aciContext.setAuthenticationLevel( principal.getAuthenticationLevel() );
        aciContext.setEntryDn( dn );
        aciContext.setAttributeType( attributeType );
        aciContext.setMicroOperations( COMPARE_PERMS );
        aciContext.setAciTuples( tuples );
        aciContext.setEntry( entry );

        engine.checkPermission( aciContext );

        return next( compareContext );
    }


    /**
     * {@inheritDoc}
     */
    public void delete( DeleteOperationContext deleteContext ) throws LdapException
    {
        CoreSession session = deleteContext.getSession();

        // bypass authz code if we are disabled
        if ( !directoryService.isAccessControlEnabled() )
        {
            next( deleteContext );
            return;
        }

        Dn dn = deleteContext.getDn();
        LdapPrincipal principal = session.getEffectivePrincipal();
        Dn principalDn = principal.getDn();

        Entry entry = deleteContext.getEntry();

        protectCriticalEntries( deleteContext, dn );

        // bypass authz code but manage caches if operation is performed by the admin
        if ( isPrincipalAnAdministrator( principalDn ) )
        {
            next( deleteContext );

            tupleCache.subentryDeleted( dn, entry );
            groupCache.groupDeleted( dn, entry );

            return;
        }

        Set<Dn> userGroups = groupCache.getGroups( principalDn.getNormName() );
        Collection<ACITuple> tuples = new HashSet<ACITuple>();
        addPerscriptiveAciTuples( deleteContext, tuples, dn, entry );
        addEntryAciTuples( tuples, entry );
        addSubentryAciTuples( deleteContext, tuples, dn, entry );

        AciContext aciContext = new AciContext( schemaManager, deleteContext );
        aciContext.setUserGroupNames( userGroups );
        aciContext.setUserDn( principalDn );
        aciContext.setAuthenticationLevel( principal.getAuthenticationLevel() );
        aciContext.setEntryDn( dn );
        aciContext.setMicroOperations( REMOVE_PERMS );
        aciContext.setAciTuples( tuples );
        aciContext.setEntry( entry );

        engine.checkPermission( aciContext );

        next( deleteContext );

        tupleCache.subentryDeleted( dn, entry );
        groupCache.groupDeleted( dn, entry );
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasEntry( HasEntryOperationContext hasEntryContext ) throws LdapException
    {
        Dn dn = hasEntryContext.getDn();

        if ( !directoryService.isAccessControlEnabled() )
        {
            return ( dn.isRootDse() || next( hasEntryContext ) );
        }

        boolean answer = next( hasEntryContext );

        // no checks on the RootDSE
        if ( dn.isRootDse() )
        {
            // No need to go down to the stack, if the dn is empty
            // It's the rootDSE, and it exists !
            return answer;
        }

        CoreSession session = hasEntryContext.getSession();

        // TODO - eventually replace this with a check on session.isAnAdministrator()
        LdapPrincipal principal = session.getEffectivePrincipal();
        Dn principalDn = principal.getDn();

        if ( isPrincipalAnAdministrator( principalDn ) )
        {
            return answer;
        }

        LookupOperationContext lookupContext = new LookupOperationContext( session, dn,
            SchemaConstants.ALL_ATTRIBUTES_ARRAY );
        Entry entry = directoryService.getPartitionNexus().lookup( lookupContext );

        Set<Dn> userGroups = groupCache.getGroups( principalDn.getNormName() );
        Collection<ACITuple> tuples = new HashSet<ACITuple>();
        addPerscriptiveAciTuples( hasEntryContext, tuples, dn, entry );
        addEntryAciTuples( tuples, ( ( ClonedServerEntry ) entry ).getOriginalEntry() );
        addSubentryAciTuples( hasEntryContext, tuples, dn, ( ( ClonedServerEntry ) entry ).getOriginalEntry() );

        // check that we have browse access to the entry
        AciContext aciContext = new AciContext( schemaManager, hasEntryContext );
        aciContext.setUserGroupNames( userGroups );
        aciContext.setUserDn( principalDn );
        aciContext.setAuthenticationLevel( principal.getAuthenticationLevel() );
        aciContext.setEntryDn( dn );
        aciContext.setMicroOperations( BROWSE_PERMS );
        aciContext.setAciTuples( tuples );
        aciContext.setEntry( ( ( ClonedServerEntry ) entry ).getOriginalEntry() );

        engine.checkPermission( aciContext );

        return next( hasEntryContext );
    }


    /**
     * {@inheritDoc}
     */
    public Entry lookup( LookupOperationContext lookupContext ) throws LdapException
    {
        CoreSession session = lookupContext.getSession();

        Entry entry = next( lookupContext );

        LdapPrincipal principal = session.getEffectivePrincipal();
        Dn principalDn = principal.getDn();

        principalDn.apply( schemaManager );

        // Bypass this interceptor if we disabled the AC subsystem or if the principal is the admin
        if ( isPrincipalAnAdministrator( principalDn ) || !directoryService.isAccessControlEnabled() )
        {
            return entry;
        }

        checkLookupAccess( lookupContext, entry );

        return entry;
    }


    /**
     * {@inheritDoc}
     */
    public void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
        Dn dn = modifyContext.getDn();

        // Access the principal requesting the operation, and bypass checks if it is the admin
        Entry entry = modifyContext.getEntry();

        LdapPrincipal principal = modifyContext.getSession().getEffectivePrincipal();
        Dn principalDn = principal.getDn();

        // bypass authz code if we are disabled
        if ( !directoryService.isAccessControlEnabled() )
        {
            next( modifyContext );
            return;
        }

        List<Modification> mods = modifyContext.getModItems();

        // bypass authz code but manage caches if operation is performed by the admin
        if ( isPrincipalAnAdministrator( principalDn ) )
        {
            next( modifyContext );

            Entry modifiedEntry = modifyContext.getAlteredEntry();
            tupleCache.subentryModified( dn, mods, modifiedEntry );
            groupCache.groupModified( dn, mods, entry, schemaManager );

            return;
        }

        Set<Dn> userGroups = groupCache.getGroups( principalDn.getName() );
        Collection<ACITuple> tuples = new HashSet<ACITuple>();
        addPerscriptiveAciTuples( modifyContext, tuples, dn, entry );
        addEntryAciTuples( tuples, entry );
        addSubentryAciTuples( modifyContext, tuples, dn, entry );

        AciContext entryAciContext = new AciContext( schemaManager, modifyContext );
        entryAciContext.setUserGroupNames( userGroups );
        entryAciContext.setUserDn( principalDn );
        entryAciContext.setAuthenticationLevel( principal.getAuthenticationLevel() );
        entryAciContext.setEntryDn( dn );
        entryAciContext.setMicroOperations( Collections.singleton( MicroOperation.MODIFY ) );
        entryAciContext.setAciTuples( tuples );
        entryAciContext.setEntry( entry );

        engine.checkPermission( entryAciContext );

        Collection<MicroOperation> perms = null;
        Entry entryView = entry.clone();

        for ( Modification mod : mods )
        {
            Attribute attr = mod.getAttribute();

            switch ( mod.getOperation() )
            {
                case ADD_ATTRIBUTE:
                    perms = ADD_PERMS;

                    // If the attribute is being created with an initial value ...
                    if ( entry.get( attr.getId() ) == null )
                    {
                        AciContext attrAciContext = new AciContext( schemaManager, modifyContext );
                        attrAciContext.setUserGroupNames( userGroups );
                        attrAciContext.setUserDn( principalDn );
                        attrAciContext.setAuthenticationLevel( principal.getAuthenticationLevel() );
                        attrAciContext.setEntryDn( dn );
                        attrAciContext.setAttributeType( attr.getAttributeType() );
                        attrAciContext.setMicroOperations( perms );
                        attrAciContext.setAciTuples( tuples );
                        attrAciContext.setEntry( entry );

                        // ... we also need to check if adding the attribute is permitted
                        engine.checkPermission( attrAciContext );
                    }

                    break;

                case REMOVE_ATTRIBUTE:
                    perms = REMOVE_PERMS;
                    Attribute entryAttr = entry.get( attr.getId() );

                    if ( entryAttr != null )
                    {
                        // If there is only one value remaining in the attribute ...
                        if ( entryAttr.size() == 1 )
                        {
                            // ... we also need to check if removing the attribute at all is permitted
                            AciContext aciContext = new AciContext( schemaManager, modifyContext );
                            aciContext.setUserGroupNames( userGroups );
                            aciContext.setUserDn( principalDn );
                            aciContext.setAuthenticationLevel( principal.getAuthenticationLevel() );
                            aciContext.setEntryDn( dn );
                            aciContext.setAttributeType( attr.getAttributeType() );
                            aciContext.setMicroOperations( perms );
                            aciContext.setAciTuples( tuples );
                            aciContext.setEntry( entry );

                            engine.checkPermission( aciContext );
                        }
                    }

                    break;

                case REPLACE_ATTRIBUTE:
                    perms = REPLACE_PERMS;
                    break;
            }

            /**
             * Update the entry view as the current modification is applied to the original entry.
             * This is especially required for handling the MaxValueCount protected item. Number of
             * values for an attribute after a modification should be known in advance in order to
             * check permissions for MaxValueCount protected item. So during addition of the first
             * value of an attribute it can be rejected if the permission denied due the the
             * MaxValueCount protected item. This is not the perfect implementation as required by
             * the specification because the system should reject the addition exactly on the right
             * value of the attribute. However as we do not have that much granularity in our
             * implementation (we consider an Attribute Addition itself a Micro Operation,
             * not the individual Value Additions) we just handle this when the first value of an
             * attribute is being checked for relevant permissions below.
             */
            entryView = ServerEntryUtils.getTargetEntry( mod, entryView, schemaManager );

            for ( Value<?> value : attr )
            {
                AciContext aciContext = new AciContext( schemaManager, modifyContext );
                aciContext.setUserGroupNames( userGroups );
                aciContext.setUserDn( principalDn );
                aciContext.setAuthenticationLevel( principal.getAuthenticationLevel() );
                aciContext.setEntryDn( dn );
                aciContext.setAttributeType( attr.getAttributeType() );
                aciContext.setAttrValue( value );
                aciContext.setMicroOperations( perms );
                aciContext.setAciTuples( tuples );
                aciContext.setEntry( entry );
                aciContext.setEntryView( entryView );

                engine.checkPermission( aciContext );
            }
        }

        next( modifyContext );

        Entry modifiedEntry = modifyContext.getAlteredEntry();
        tupleCache.subentryModified( dn, mods, modifiedEntry );
        groupCache.groupModified( dn, mods, entry, schemaManager );
    }


    /**
     * {@inheritDoc}
     */
    public void move( MoveOperationContext moveContext ) throws LdapException
    {
        Dn oriChildName = moveContext.getDn();

        // Access the principal requesting the operation, and bypass checks if it is the admin
        Entry entry = moveContext.getOriginalEntry();
        CoreSession session = moveContext.getSession();

        Dn newDn = moveContext.getNewDn();

        LdapPrincipal principal = session.getEffectivePrincipal();
        Dn principalDn = principal.getDn();

        // bypass authz code if we are disabled
        if ( !directoryService.isAccessControlEnabled() )
        {
            next( moveContext );
            return;
        }

        protectCriticalEntries( moveContext, oriChildName );

        // bypass authz code but manage caches if operation is performed by the admin
        if ( isPrincipalAnAdministrator( principalDn ) )
        {
            next( moveContext );
            tupleCache.subentryRenamed( oriChildName, newDn );
            groupCache.groupRenamed( oriChildName, newDn );
            return;
        }

        Set<Dn> userGroups = groupCache.getGroups( principalDn.getNormName() );
        Collection<ACITuple> tuples = new HashSet<ACITuple>();
        addPerscriptiveAciTuples( moveContext, tuples, oriChildName, entry );
        addEntryAciTuples( tuples, entry );
        addSubentryAciTuples( moveContext, tuples, oriChildName, entry );

        AciContext aciContext = new AciContext( schemaManager, moveContext );
        aciContext.setUserGroupNames( userGroups );
        aciContext.setUserDn( principalDn );
        aciContext.setAuthenticationLevel( principal.getAuthenticationLevel() );
        aciContext.setEntryDn( oriChildName );
        aciContext.setMicroOperations( EXPORT_PERMS );
        aciContext.setAciTuples( tuples );
        aciContext.setEntry( entry );

        engine.checkPermission( aciContext );

        // Get the entry again without operational attributes
        // because access control subentry operational attributes
        // will not be valid at the new location.
        // This will certainly be fixed by the SubentryInterceptor,
        // but after this service.
        LookupOperationContext lookupContext = new LookupOperationContext( session, oriChildName,
            SchemaConstants.ALL_USER_ATTRIBUTES_ARRAY );
        Entry importedEntry = directoryService.getPartitionNexus().lookup( lookupContext );

        // As the target entry does not exist yet and so
        // its subentry operational attributes are not there,
        // we need to construct an entry to represent it
        // at least with minimal requirements which are object class
        // and access control subentry operational attributes.
        Entry subentryAttrs = subentryUtils.getSubentryAttributes( newDn, importedEntry );

        for ( Attribute attribute : importedEntry )
        {
            subentryAttrs.put( attribute );
        }

        Collection<ACITuple> destTuples = new HashSet<ACITuple>();
        // Import permission is only valid for prescriptive ACIs
        addPerscriptiveAciTuples( moveContext, destTuples, newDn, subentryAttrs );

        // Evaluate the target context to see whether it
        // allows an entry named newName to be imported as a subordinate.
        aciContext = new AciContext( schemaManager, moveContext );
        aciContext.setUserGroupNames( userGroups );
        aciContext.setUserDn( principalDn );
        aciContext.setAuthenticationLevel( principal.getAuthenticationLevel() );
        aciContext.setEntryDn( newDn );
        aciContext.setMicroOperations( IMPORT_PERMS );
        aciContext.setAciTuples( destTuples );
        aciContext.setEntry( subentryAttrs );

        engine.checkPermission( aciContext );

        next( moveContext );
        tupleCache.subentryRenamed( oriChildName, newDn );
        groupCache.groupRenamed( oriChildName, newDn );
    }


    /**
     * {@inheritDoc}
     */
    public void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        Dn oldDn = moveAndRenameContext.getDn();
        CoreSession session = moveAndRenameContext.getSession();

        Entry entry = moveAndRenameContext.getOriginalEntry();

        LdapPrincipal principal = session.getEffectivePrincipal();
        Dn principalDn = principal.getDn();
        Dn newDn = moveAndRenameContext.getNewDn();

        // bypass authz code if we are disabled
        if ( !directoryService.isAccessControlEnabled() )
        {
            next( moveAndRenameContext );

            return;
        }

        protectCriticalEntries( moveAndRenameContext, oldDn );

        // bypass authz code but manage caches if operation is performed by the admin
        if ( isPrincipalAnAdministrator( principalDn ) )
        {
            next( moveAndRenameContext );
            tupleCache.subentryRenamed( oldDn, newDn );
            groupCache.groupRenamed( oldDn, newDn );

            return;
        }

        Set<Dn> userGroups = groupCache.getGroups( principalDn.getNormName() );
        Collection<ACITuple> tuples = new HashSet<ACITuple>();
        addPerscriptiveAciTuples( moveAndRenameContext, tuples, oldDn, entry );
        addEntryAciTuples( tuples, entry );
        addSubentryAciTuples( moveAndRenameContext, tuples, oldDn, entry );

        AciContext aciContext = new AciContext( schemaManager, moveAndRenameContext );
        aciContext.setUserGroupNames( userGroups );
        aciContext.setUserDn( principalDn );
        aciContext.setAuthenticationLevel( principal.getAuthenticationLevel() );
        aciContext.setEntryDn( oldDn );
        aciContext.setMicroOperations( MOVERENAME_PERMS );
        aciContext.setAciTuples( tuples );
        aciContext.setEntry( entry );

        engine.checkPermission( aciContext );

        // Get the entry again without operational attributes
        // because access control subentry operational attributes
        // will not be valid at the new location.
        // This will certainly be fixed by the SubentryInterceptor,
        // but after this service.

        LookupOperationContext lookupContext = new LookupOperationContext( session, oldDn,
            SchemaConstants.ALL_USER_ATTRIBUTES_ARRAY );
        Entry importedEntry = directoryService.getPartitionNexus().lookup( lookupContext );

        // As the target entry does not exist yet and so
        // its subentry operational attributes are not there,
        // we need to construct an entry to represent it
        // at least with minimal requirements which are object class
        // and access control subentry operational attributes.
        Entry subentryAttrs = subentryUtils.getSubentryAttributes( newDn, importedEntry );

        for ( Attribute attribute : importedEntry )
        {
            subentryAttrs.put( attribute );
        }

        Collection<ACITuple> destTuples = new HashSet<ACITuple>();
        // Import permission is only valid for prescriptive ACIs
        addPerscriptiveAciTuples( moveAndRenameContext, destTuples, newDn, subentryAttrs );

        // Evaluate the target context to see whether it
        // allows an entry named newName to be imported as a subordinate.
        aciContext = new AciContext( schemaManager, moveAndRenameContext );
        aciContext.setUserGroupNames( userGroups );
        aciContext.setUserDn( principalDn );
        aciContext.setAuthenticationLevel( principal.getAuthenticationLevel() );
        aciContext.setEntryDn( newDn );
        aciContext.setMicroOperations( IMPORT_PERMS );
        aciContext.setAciTuples( destTuples );
        aciContext.setEntry( subentryAttrs );

        engine.checkPermission( aciContext );

        next( moveAndRenameContext );
        tupleCache.subentryRenamed( oldDn, newDn );
        groupCache.groupRenamed( oldDn, newDn );
    }


    /**
     * {@inheritDoc}
     */
    public void rename( RenameOperationContext renameContext ) throws LdapException
    {
        Dn oldName = renameContext.getDn();
        Entry originalEntry = null;

        if ( renameContext.getEntry() != null )
        {
            originalEntry = ( ( ClonedServerEntry ) renameContext.getEntry() ).getOriginalEntry();
        }

        LdapPrincipal principal = renameContext.getSession().getEffectivePrincipal();
        Dn principalDn = principal.getDn();
        Dn newName = renameContext.getNewDn();

        // bypass authz code if we are disabled
        if ( !directoryService.isAccessControlEnabled() )
        {
            next( renameContext );
            return;
        }

        protectCriticalEntries( renameContext, oldName );

        // bypass authz code but manage caches if operation is performed by the admin
        if ( isPrincipalAnAdministrator( principalDn ) )
        {
            next( renameContext );
            tupleCache.subentryRenamed( oldName, newName );

            // TODO : this method returns a boolean : what should we do with the result ?
            groupCache.groupRenamed( oldName, newName );

            return;
        }

        Set<Dn> userGroups = groupCache.getGroups( principalDn.getNormName() );
        Collection<ACITuple> tuples = new HashSet<ACITuple>();
        addPerscriptiveAciTuples( renameContext, tuples, oldName, originalEntry );
        addEntryAciTuples( tuples, originalEntry );
        addSubentryAciTuples( renameContext, tuples, oldName, originalEntry );

        AciContext aciContext = new AciContext( schemaManager, renameContext );
        aciContext.setUserGroupNames( userGroups );
        aciContext.setUserDn( principalDn );
        aciContext.setAuthenticationLevel( principal.getAuthenticationLevel() );
        aciContext.setEntryDn( oldName );
        aciContext.setMicroOperations( RENAME_PERMS );
        aciContext.setAciTuples( tuples );
        aciContext.setEntry( originalEntry );

        engine.checkPermission( aciContext );

        next( renameContext );
        tupleCache.subentryRenamed( oldName, newName );
        groupCache.groupRenamed( oldName, newName );
    }


    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException
    {
        LdapPrincipal user = searchContext.getSession().getEffectivePrincipal();
        Dn principalDn = user.getDn();
        EntryFilteringCursor cursor = next( searchContext );

        boolean isSubschemaSubentryLookup = subschemaSubentryDn.equals( searchContext.getDn().getNormName() );

        boolean isRootDseLookup = ( searchContext.getDn().size() == 0 )
            && ( searchContext.getScope() == SearchScope.OBJECT );

        if ( isPrincipalAnAdministrator( principalDn )
            || !directoryService.isAccessControlEnabled() || isRootDseLookup
            || isSubschemaSubentryLookup )
        {
            return cursor;
        }

        cursor.addEntryFilter( new AuthorizationFilter() );
        return cursor;
    }


    /**
     * Checks if the READ permissions exist to the entry and to each attribute type and
     * value.
     *
     * @todo not sure if we should hide attribute types/values or throw an exception
     * instead.  I think we're going to have to use a filter to restrict the return
     * of attribute types and values instead of throwing an exception.  Lack of read
     * perms to attributes and their values results in their removal when returning
     * the entry.
     *
     * @param principal the user associated with the call
     * @param dn the name of the entry being looked up
     * @param entry the raw entry pulled from the nexus
     * @throws Exception if undlying access to the DIT fails
     */
    private void checkLookupAccess( LookupOperationContext lookupContext, Entry entry ) throws LdapException
    {
        Dn dn = lookupContext.getDn();

        // no permissions checks on the RootDSE
        if ( dn.isRootDse() )
        {
            return;
        }

        LdapPrincipal principal = lookupContext.getSession().getEffectivePrincipal();
        Dn userName = principal.getDn();
        Set<Dn> userGroups = groupCache.getGroups( userName.getNormName() );
        Collection<ACITuple> tuples = new HashSet<ACITuple>();
        addPerscriptiveAciTuples( lookupContext, tuples, dn, entry );
        addEntryAciTuples( tuples, entry );
        addSubentryAciTuples( lookupContext, tuples, dn, entry );

        // check that we have read access to the entry
        AciContext aciContext = new AciContext( schemaManager, lookupContext );
        aciContext.setUserGroupNames( userGroups );
        aciContext.setUserDn( userName );
        aciContext.setAuthenticationLevel( principal.getAuthenticationLevel() );
        aciContext.setEntryDn( dn );
        aciContext.setMicroOperations( LOOKUP_PERMS );
        aciContext.setAciTuples( tuples );
        aciContext.setEntry( entry );

        engine.checkPermission( aciContext );

        // check that we have read access to every attribute type and value
        for ( Attribute attribute : entry )
        {

            for ( Value<?> value : attribute )
            {
                AciContext valueAciContext = new AciContext( schemaManager, lookupContext );
                valueAciContext.setUserGroupNames( userGroups );
                valueAciContext.setUserDn( userName );
                valueAciContext.setAuthenticationLevel( principal.getAuthenticationLevel() );
                valueAciContext.setEntryDn( dn );
                valueAciContext.setAttributeType( attribute.getAttributeType() );
                valueAciContext.setAttrValue( value );
                valueAciContext.setMicroOperations( READ_PERMS );
                valueAciContext.setAciTuples( tuples );
                valueAciContext.setEntry( entry );

                engine.checkPermission( valueAciContext );
            }
        }
    }


    public final boolean isPrincipalAnAdministrator( Dn principalDn )
    {
        return groupCache.isPrincipalAnAdministrator( principalDn );
    }


    public void cacheNewGroup( Dn name, Entry entry ) throws Exception
    {
        groupCache.groupAdded( name, entry );
    }


    private boolean filter( OperationContext opContext, Dn normName, Entry clonedEntry ) throws LdapException
    {
        /*
         * First call hasPermission() for entry level "Browse" and "ReturnDN" perm
         * tests.  If we hasPermission() returns false we immediately short the
         * process and return false.
         */

        LdapPrincipal principal = opContext.getSession().getEffectivePrincipal();
        Dn userDn = principal.getDn();
        Set<Dn> userGroups = groupCache.getGroups( userDn.getNormName() );
        Collection<ACITuple> tuples = new HashSet<ACITuple>();
        addPerscriptiveAciTuples( opContext, tuples, normName, clonedEntry );
        addEntryAciTuples( tuples, ( ( ClonedServerEntry ) clonedEntry ).getOriginalEntry() );
        addSubentryAciTuples( opContext, tuples, normName, ( ( ClonedServerEntry ) clonedEntry ).getOriginalEntry() );

        AciContext aciContext = new AciContext( schemaManager, opContext );
        aciContext.setUserGroupNames( userGroups );
        aciContext.setUserDn( userDn );
        aciContext.setAuthenticationLevel( principal.getAuthenticationLevel() );
        aciContext.setEntryDn( normName );
        aciContext.setMicroOperations( SEARCH_ENTRY_PERMS );
        aciContext.setAciTuples( tuples );
        aciContext.setEntry( ( ( ClonedServerEntry ) clonedEntry ).getOriginalEntry() );

        if ( !engine.hasPermission( aciContext ) )
        {
            return false;
        }

        /*
         * For each attribute type we check if access is allowed to the type.  If not
         * the attribute is yanked out of the entry to be returned.  If permission is
         * allowed we move on to check if the values are allowed.  Values that are
         * not allowed are removed from the attribute.  If the attribute has no more
         * values remaining then the entire attribute is removed.
         */
        List<AttributeType> attributeToRemove = new ArrayList<AttributeType>();

        for ( Attribute attribute : clonedEntry.getAttributes() )
        {
            // if attribute type scope access is not allowed then remove the attribute and continue
            AttributeType attributeType = attribute.getAttributeType();
            Attribute attr = clonedEntry.get( attributeType );

            aciContext = new AciContext( schemaManager, opContext );
            aciContext.setUserGroupNames( userGroups );
            aciContext.setUserDn( userDn );
            aciContext.setAuthenticationLevel( principal.getAuthenticationLevel() );
            aciContext.setEntryDn( normName );
            aciContext.setAttributeType( attributeType );
            aciContext.setMicroOperations( SEARCH_ATTRVAL_PERMS );
            aciContext.setAciTuples( tuples );
            aciContext.setEntry( clonedEntry );

            if ( !engine.hasPermission( aciContext ) )
            {
                attributeToRemove.add( attributeType );

                continue;
            }

            List<Value<?>> valueToRemove = new ArrayList<Value<?>>();

            // attribute type scope is ok now let's determine value level scope
            for ( Value<?> value : attr )
            {
                aciContext = new AciContext( schemaManager, opContext );
                aciContext.setUserGroupNames( userGroups );
                aciContext.setUserDn( userDn );
                aciContext.setAuthenticationLevel( principal.getAuthenticationLevel() );
                aciContext.setEntryDn( normName );
                aciContext.setAttributeType( attr.getAttributeType() );
                aciContext.setAttrValue( value );
                aciContext.setMicroOperations( SEARCH_ATTRVAL_PERMS );
                aciContext.setAciTuples( tuples );
                aciContext.setEntry( clonedEntry );

                if ( !engine.hasPermission( aciContext ) )
                {
                    valueToRemove.add( value );
                }
            }

            for ( Value<?> value : valueToRemove )
            {
                attr.remove( value );
            }

            if ( attr.size() == 0 )
            {
                attributeToRemove.add( attributeType );
            }
        }

        for ( AttributeType attributeType : attributeToRemove )
        {
            clonedEntry.removeAttributes( attributeType );
        }

        return true;
    }

    /**
     * WARNING: create one of these filters fresh every time for each new search.
     */
    private class AuthorizationFilter implements EntryFilter
    {
        /**
         * {@inheritDoc}
         */
        public boolean accept( SearchOperationContext searchContext, Entry entry ) throws LdapException
        {
            Dn normName = entry.getDn().apply( schemaManager );

            return filter( searchContext, normName, entry );
        }


        /**
         * {@inheritDoc}
         */
        public String toString( String tabs )
        {
            return tabs + "AuthorizationFilter";
        }
    }


    private boolean isTheAdministrator( Dn normalizedDn )
    {
        return normalizedDn.getNormName().equals( ServerDNConstants.ADMIN_SYSTEM_DN_NORMALIZED );
    }
}
