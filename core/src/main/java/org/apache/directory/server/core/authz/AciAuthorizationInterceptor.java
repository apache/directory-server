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

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DefaultCoreSession;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.LdapPrincipal;
import org.apache.directory.server.core.authz.support.ACDFEngine;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.ServerEntryUtils;
import org.apache.directory.server.core.filtering.EntryFilter;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.InterceptorChain;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.interceptor.context.GetMatchedNameOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchingOperationContext;
import org.apache.directory.server.core.partition.ByPassConstants;
import org.apache.directory.server.core.subtree.SubentryInterceptor;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.aci.ACIItem;
import org.apache.directory.shared.ldap.aci.ACIItemParser;
import org.apache.directory.shared.ldap.aci.ACITuple;
import org.apache.directory.shared.ldap.aci.MicroOperation;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapNoPermissionException;
import org.apache.directory.shared.ldap.exception.LdapOperationErrorException;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.normalizers.ConcreteNameComponentNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An ACI based authorization service.
 *
 * @org.apache.xbean.XBean
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AciAuthorizationInterceptor extends BaseInterceptor
{
    /** the logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( AciAuthorizationInterceptor.class );

    /**
     * the multivalued op attr used to track the prescriptive access control
     * subentries that apply to an entry.
     */
    private static final String AC_SUBENTRY_ATTR = "accessControlSubentries";

    private static final Collection<MicroOperation> ADD_PERMS;
    private static final Collection<MicroOperation> READ_PERMS;
    private static final Collection<MicroOperation> COMPARE_PERMS;
    private static final Collection<MicroOperation> SEARCH_ENTRY_PERMS;
    private static final Collection<MicroOperation> SEARCH_ATTRVAL_PERMS;
    private static final Collection<MicroOperation> REMOVE_PERMS;
    private static final Collection<MicroOperation> MATCHEDNAME_PERMS;
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
        MATCHEDNAME_PERMS = Collections.singleton( MicroOperation.DISCLOSE_ON_ERROR );
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
    
    /** interceptor chain */
    private InterceptorChain chain;
    
    /** Global registries */
    private SchemaManager schemaManager;
    
    /** the system wide subschemaSubentryDn */
    private String subschemaSubentryDn;

    private AttributeType objectClassType;
    private AttributeType acSubentryType;

    private String subentryOid;

    /** A storage for the entryACI attributeType */
    private AttributeType entryAciType;

    /** the subentry ACI attribute type */
    private AttributeType subentryAciType;
    
    public static final SearchControls DEFAULT_SEARCH_CONTROLS = new SearchControls();

    /**
     * Initializes this interceptor based service by getting a handle on the nexus, setting up
     * the tupe and group membership caches and the ACIItem parser and the ACDF engine.
     *
     * @param directoryService the directory service core
     * @throws Exception if there are problems during initialization
     */
    public void init( DirectoryService directoryService ) throws Exception
    {
        super.init( directoryService );

        DN adminDn = new DN( ServerDNConstants.ADMIN_SYSTEM_DN_NORMALIZED );
        adminDn.normalize( directoryService.getSchemaManager().getNormalizerMapping() );
        CoreSession adminSession = new DefaultCoreSession( 
            new LdapPrincipal( adminDn, AuthenticationLevel.STRONG ), directoryService );

        tupleCache = new TupleCache( adminSession );
        groupCache = new GroupCache( adminSession );
        schemaManager = directoryService.getSchemaManager();
        //ocRegistry = registries.getObjectClassRegistry();
        
        // look up some constant information
        String objectClassOid = schemaManager.getAttributeTypeRegistry().getOidByName( SchemaConstants.OBJECT_CLASS_AT );
        subentryOid = schemaManager.getObjectClassRegistry().getOidByName( SchemaConstants.SUBENTRY_OC );
        String acSubentryOid = schemaManager.getAttributeTypeRegistry().getOidByName( AC_SUBENTRY_ATTR );
        objectClassType = schemaManager.lookupAttributeTypeRegistry( objectClassOid );
        acSubentryType = schemaManager.lookupAttributeTypeRegistry( acSubentryOid );
        entryAciType = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.ENTRY_ACI_AT_OID ); 
        subentryAciType = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.SUBENTRY_ACI_AT_OID );
        
        aciParser = new ACIItemParser( new ConcreteNameComponentNormalizer( schemaManager ), schemaManager.getNormalizerMapping() );
        engine = new ACDFEngine( schemaManager.getGlobalOidRegistry(), schemaManager );
        chain = directoryService.getInterceptorChain();

        // stuff for dealing with subentries (garbage for now)
        Value<?> subschemaSubentry = 
            directoryService.getPartitionNexus().getRootDSE( null ).
                get( SchemaConstants.SUBSCHEMA_SUBENTRY_AT ).get();
        DN subschemaSubentryDnName = new DN( subschemaSubentry.getString() );
        subschemaSubentryDnName.normalize( schemaManager.getNormalizerMapping() );
        subschemaSubentryDn = subschemaSubentryDnName.getNormName();
    }


    private void protectCriticalEntries( DN dn ) throws Exception
    {
        DN principalDn = getPrincipal().getDNRef();

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
    private void addPerscriptiveAciTuples( OperationContext opContext, Collection<ACITuple> tuples, DN dn,
        Entry entry ) throws Exception
    {
        EntryAttribute oc = null;
        
        if ( entry instanceof ClonedServerEntry )
        {
            oc = ((ClonedServerEntry)entry).getOriginalEntry().get( objectClassType );
        }
        else
        {
            oc = entry.get( objectClassType );
        }
        
        /*
         * If the protected entry is a subentry, then the entry being evaluated
         * for perscriptiveACIs is in fact the administrative entry.  By
         * substituting the administrative entry for the actual subentry the
         * code below this "if" statement correctly evaluates the effects of
         * perscriptiveACI on the subentry.  Basically subentries are considered
         * to be in the same naming context as their access point so the subentries
         * effecting their parent entry applies to them as well.
         */
        if ( oc.contains( SchemaConstants.SUBENTRY_OC ) || oc.contains( subentryOid ) )
        {
            DN parentDn = ( DN ) dn.clone();
            parentDn.remove( dn.size() - 1 );
            entry = opContext.lookup( parentDn, ByPassConstants.LOOKUP_BYPASS );
        }

        EntryAttribute subentries = entry.get( acSubentryType );
        
        if ( subentries == null )
        {
            return;
        }
        
        for ( Value<?> value:subentries )
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
    private void addEntryAciTuples( Collection<ACITuple> tuples, Entry entry ) throws Exception
    {
        EntryAttribute entryAci = entry.get( entryAciType );
        
        if ( entryAci == null )
        {
            return;
        }

        for ( Value<?> value:entryAci )
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
    private void addSubentryAciTuples( OperationContext opContext, Collection<ACITuple> tuples, DN dn, Entry entry )
        throws Exception
    {
        // only perform this for subentries
        if ( !entry.contains( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.SUBENTRY_OC ) )
        {
            return;
        }

        // get the parent or administrative entry for this subentry since it
        // will contain the subentryACI attributes that effect subentries
        DN parentDn = ( DN ) dn.clone();
        parentDn.remove( dn.size() - 1 );
        Entry administrativeEntry = ((ClonedServerEntry)opContext.lookup( parentDn, ByPassConstants.LOOKUP_BYPASS )).getOriginalEntry();
        
        EntryAttribute subentryAci = administrativeEntry.get( subentryAciType );

        if ( subentryAci == null )
        {
            return;
        }

        for ( Value<?> value:subentryAci )
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

    public void add( NextInterceptor next, AddOperationContext addContext ) throws Exception
    {
        // Access the principal requesting the operation, and bypass checks if it is the admin
        LdapPrincipal principal = addContext.getSession().getEffectivePrincipal();
        DN principalDn = principal.getDN();
        
        Entry serverEntry = addContext.getEntry(); 
        //Attributes entry = ServerEntryUtils.toAttributesImpl( serverEntry );

        DN name = addContext.getDn();

        // bypass authz code if we are disabled
        if ( !addContext.getSession().getDirectoryService().isAccessControlEnabled() )
        {
            next.add( addContext );
            return;
        }

        // bypass authz code but manage caches if operation is performed by the admin
        if ( isPrincipalAnAdministrator( principalDn ) )
        {
            next.add( addContext );
            tupleCache.subentryAdded( name, serverEntry );
            groupCache.groupAdded( name, serverEntry );
            return;
        }

        // perform checks below here for all non-admin users
        SubentryInterceptor subentryInterceptor = ( SubentryInterceptor ) chain.get( SubentryInterceptor.class.getName() );
        Entry subentryAttrs = subentryInterceptor.getSubentryAttributes( name, serverEntry );
        
        for ( EntryAttribute attribute:serverEntry )
        {
            subentryAttrs.put( attribute );
        }

        // Assemble all the information required to make an access control decision
        Set<DN> userGroups = groupCache.getGroups( principalDn.getNormName() );
        Collection<ACITuple> tuples = new HashSet<ACITuple>();

        // Build the total collection of tuples to be considered for add rights
        // NOTE: entryACI are NOT considered in adds (it would be a security breech)
        addPerscriptiveAciTuples( addContext, tuples, name, subentryAttrs );
        addSubentryAciTuples( addContext, tuples, name, subentryAttrs );

        // check if entry scope permission is granted
        engine.checkPermission( schemaManager, addContext, userGroups, principalDn, principal.getAuthenticationLevel(), name, null, null,
            ADD_PERMS, tuples, subentryAttrs, null );

        // now we must check if attribute type and value scope permission is granted
        for ( EntryAttribute attribute:serverEntry )
        {
            for ( Value<?> value:attribute )
            {
                engine.checkPermission( schemaManager, addContext, userGroups, principalDn, 
                    principal.getAuthenticationLevel(), name, attribute.getUpId(), value, 
                    ADD_PERMS, tuples, serverEntry, null );
            }
        }

        // if we've gotten this far then access has been granted
        next.add( addContext );

        // if the entry added is a subentry or a groupOf[Unique]Names we must
        // update the ACITuple cache and the groups cache to keep them in sync
        tupleCache.subentryAdded( name, serverEntry );
        groupCache.groupAdded( name, serverEntry );
    }


    private boolean isTheAdministrator( DN normalizedDn )
    {
        return normalizedDn.getNormName().equals( ServerDNConstants.ADMIN_SYSTEM_DN_NORMALIZED );
    }


    public void delete( NextInterceptor next, DeleteOperationContext deleteContext ) throws Exception
    {
        CoreSession session = deleteContext.getSession();
        
        // bypass authz code if we are disabled
        if ( ! session.getDirectoryService().isAccessControlEnabled() )
        {
            next.delete( deleteContext );
            return;
        }

        DN dn = deleteContext.getDn();
        LdapPrincipal principal = session.getEffectivePrincipal();
        DN principalDn = principal.getDN();

        Entry entry = deleteContext.getEntry();

        protectCriticalEntries( dn );

        // bypass authz code but manage caches if operation is performed by the admin
        if ( isPrincipalAnAdministrator( principalDn ) )
        {
            next.delete( deleteContext );
            
            tupleCache.subentryDeleted( dn, entry );
            groupCache.groupDeleted( dn, entry );
            
            return;
        }

        Set<DN> userGroups = groupCache.getGroups( principalDn.getNormName() );
        Collection<ACITuple> tuples = new HashSet<ACITuple>();
        addPerscriptiveAciTuples( deleteContext, tuples, dn, ((ClonedServerEntry)entry).getOriginalEntry() );
        addEntryAciTuples( tuples, entry );
        addSubentryAciTuples( deleteContext, tuples, dn, entry );

        engine.checkPermission( schemaManager, deleteContext, userGroups, principalDn, 
            principal.getAuthenticationLevel(), dn, null, null, REMOVE_PERMS, tuples, entry, null );

        next.delete( deleteContext );
        
        tupleCache.subentryDeleted( dn, entry );
        groupCache.groupDeleted( dn, entry );
    }


    // False positive, we want to keep the comment
    @SuppressWarnings("PMD.CollapsibleIfStatements")
    public void modify( NextInterceptor next, ModifyOperationContext opContext ) throws Exception
    {
        DN name = opContext.getDn();

        // Access the principal requesting the operation, and bypass checks if it is the admin
        Entry entry = opContext.lookup( name, ByPassConstants.LOOKUP_BYPASS );
        
        LdapPrincipal principal = opContext.getSession().getEffectivePrincipal();
        DN principalDn = principal.getDN();

        // bypass authz code if we are disabled
        if ( !opContext.getSession().getDirectoryService().isAccessControlEnabled() )
        {
            next.modify( opContext );
            return;
        }

        List<Modification> mods = opContext.getModItems();

        // bypass authz code but manage caches if operation is performed by the admin
        if ( isPrincipalAnAdministrator( principalDn ) )
        {
            next.modify( opContext );
            /**
             * @TODO: A virtual entry can be created here for not hitting the backend again.
             */
            Entry modifiedEntry = opContext.lookup( name, ByPassConstants.LOOKUP_BYPASS );
            tupleCache.subentryModified( name, mods, modifiedEntry );
            groupCache.groupModified( name, mods, entry, schemaManager );
            return;
        }

        Set<DN> userGroups = groupCache.getGroups( principalDn.getName() );
        Collection<ACITuple> tuples = new HashSet<ACITuple>();
        addPerscriptiveAciTuples( opContext, tuples, name, ((ClonedServerEntry)entry).getOriginalEntry() );
        addEntryAciTuples( tuples, entry );
        addSubentryAciTuples( opContext, tuples, name, entry );

        engine.checkPermission( schemaManager, opContext, userGroups, principalDn, 
            principal.getAuthenticationLevel(), name, null, null, 
            Collections.singleton( MicroOperation.MODIFY ), tuples, entry, null );

        Collection<MicroOperation> perms = null;
        Entry entryView = ( Entry ) entry.clone();
        
        for ( Modification mod : mods )
        {
            EntryAttribute attr = mod.getAttribute();

            switch ( mod.getOperation() )
            {
                case ADD_ATTRIBUTE :
                    perms = ADD_PERMS;
                
                    // If the attribute is being created with an initial value ...
                    if ( entry.get( attr.getId() ) == null )
                    {
                        // ... we also need to check if adding the attribute is permitted
                        engine.checkPermission( schemaManager, opContext, userGroups, principalDn, principal.getAuthenticationLevel(), name,
                                attr.getId(), null, perms, tuples, entry, null );
                    }
                    
                    break;

                case REMOVE_ATTRIBUTE :
                    perms = REMOVE_PERMS;
                    EntryAttribute entryAttr = entry.get( attr.getId() );

                    if ( entryAttr != null )
                    {
                        // If there is only one value remaining in the attribute ...
                        if ( entryAttr.size() == 1 )
                        {
                            // ... we also need to check if removing the attribute at all is permitted
                            engine.checkPermission( schemaManager, opContext, userGroups, principalDn, 
                                principal.getAuthenticationLevel(), name, attr.getId(), 
                                null, perms, tuples, entry, null );
                        }
                    }
                    
                    break;

                case REPLACE_ATTRIBUTE :
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
            
            for ( Value<?> value:attr )
            {                
                engine.checkPermission( schemaManager, opContext, userGroups, principalDn, 
                    principal.getAuthenticationLevel(), name, attr.getId(), value, 
                    perms, tuples, entry, entryView );
            }
        }

        

        next.modify( opContext );
        /**
         * @TODO: A virtual entry can be created here for not hitting the backend again.
         */
        Entry modifiedEntry = opContext.lookup( name, ByPassConstants.LOOKUP_BYPASS );
        tupleCache.subentryModified( name, mods, modifiedEntry );
        groupCache.groupModified( name, mods, entry, schemaManager );
    }

    
    public boolean hasEntry( NextInterceptor next, EntryOperationContext entryContext ) throws Exception
    {
        DN name = entryContext.getDn();
        
        if ( ! entryContext.getSession().getDirectoryService().isAccessControlEnabled() )
        {
            return name.size() == 0 || next.hasEntry( entryContext );
        }
        
        boolean answer = next.hasEntry( entryContext );

        // no checks on the RootDSE
        if ( name.size() == 0 )
        {
            // No need to go down to the stack, if the dn is empty 
            // It's the rootDSE, and it exists ! 
            return answer;
        }
        
        // TODO - eventually replace this with a check on session.isAnAdministrator()
        LdapPrincipal principal = entryContext.getSession().getEffectivePrincipal();
        DN principalDn = principal.getDN();
        if ( isPrincipalAnAdministrator( principalDn ) )
        {
            return answer;
        }

        Entry entry = entryContext.lookup( name, ByPassConstants.HAS_ENTRY_BYPASS );
        Set<DN> userGroups = groupCache.getGroups( principalDn.getNormName() );
        Collection<ACITuple> tuples = new HashSet<ACITuple>();
        addPerscriptiveAciTuples( entryContext, tuples, name, ((ClonedServerEntry)entry).getOriginalEntry() );
        addEntryAciTuples( tuples, ((ClonedServerEntry)entry).getOriginalEntry() );
        addSubentryAciTuples( entryContext, tuples, name, ((ClonedServerEntry)entry).getOriginalEntry() );

        // check that we have browse access to the entry
        engine.checkPermission( schemaManager, entryContext, userGroups, principalDn, 
            principal.getAuthenticationLevel(), name, null, null,
            BROWSE_PERMS, tuples, ((ClonedServerEntry)entry).getOriginalEntry(), null );

        return next.hasEntry( entryContext );
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
    private void checkLookupAccess( LookupOperationContext lookupContext, Entry entry ) throws Exception
    {
        // no permissions checks on the RootDSE
        if ( lookupContext.getDn().getNormName().trim().equals( "" ) )
        {
            return;
        }

        LdapPrincipal principal = lookupContext.getSession().getEffectivePrincipal();
        DN userName = principal.getDN();
        Set<DN> userGroups = groupCache.getGroups( userName.getNormName() );
        Collection<ACITuple> tuples = new HashSet<ACITuple>();
        addPerscriptiveAciTuples( lookupContext, tuples, lookupContext.getDn(), entry );
        addEntryAciTuples( tuples, entry );
        addSubentryAciTuples( lookupContext, tuples, lookupContext.getDn(), entry );

        // check that we have read access to the entry
        engine.checkPermission( schemaManager, lookupContext, userGroups, userName, principal.getAuthenticationLevel(), 
            lookupContext.getDn(), null, null,
            LOOKUP_PERMS, tuples, entry, null );

        // check that we have read access to every attribute type and value
        for ( EntryAttribute attribute:entry )
        {
            
            for ( Value<?> value:attribute )
            {
                engine.checkPermission( 
                    schemaManager, 
                    lookupContext, 
                    userGroups, 
                    userName, 
                    principal.getAuthenticationLevel(), 
                    lookupContext.getDn(), 
                    attribute.getUpId(), 
                    value, 
                    READ_PERMS, 
                    tuples, 
                    entry, 
                    null );
            }
        }
    }


    public Entry lookup( NextInterceptor next, LookupOperationContext lookupContext ) throws Exception
    {
        CoreSession session = lookupContext.getSession();
        DirectoryService directoryService = session.getDirectoryService();
        
        LdapPrincipal principal = session.getEffectivePrincipal();
        DN principalDn = principal.getDN();
        
        if ( !principalDn.isNormalized() )
        {
            principalDn.normalize( schemaManager.getNormalizerMapping() );
        }
        
        if ( isPrincipalAnAdministrator( principalDn ) || !directoryService.isAccessControlEnabled() )
        {
            return next.lookup( lookupContext );
        }

        lookupContext.setByPassed( ByPassConstants.LOOKUP_BYPASS );
        Entry entry = directoryService.getOperationManager().lookup( lookupContext );

        checkLookupAccess( lookupContext, entry );
        
        return next.lookup( lookupContext );
    }

    
    public void rename( NextInterceptor next, RenameOperationContext renameContext ) throws Exception
    {
        DN oldName = renameContext.getDn();
        Entry originalEntry = null;
        
        if ( renameContext.getEntry() != null )
        {
            originalEntry = renameContext.getEntry().getOriginalEntry();
        }
        
        LdapPrincipal principal = renameContext.getSession().getEffectivePrincipal();
        DN principalDn = principal.getDN();
        DN newName = renameContext.getNewDn();

        // bypass authz code if we are disabled
        if ( !renameContext.getSession().getDirectoryService().isAccessControlEnabled() )
        {
            next.rename( renameContext );
            return;
        }

        protectCriticalEntries( oldName );

        // bypass authz code but manage caches if operation is performed by the admin
        if ( isPrincipalAnAdministrator( principalDn ) )
        {
            next.rename( renameContext );
            tupleCache.subentryRenamed( oldName, newName );
            
            // TODO : this method returns a boolean : what should we do with the result ?
            groupCache.groupRenamed( oldName, newName );

            return;
        }

        Set<DN> userGroups = groupCache.getGroups( principalDn.getNormName() );
        Collection<ACITuple> tuples = new HashSet<ACITuple>();
        addPerscriptiveAciTuples( renameContext, tuples, oldName, originalEntry );
        addEntryAciTuples( tuples, originalEntry );
        addSubentryAciTuples( renameContext, tuples, oldName, originalEntry );

        engine.checkPermission( schemaManager, renameContext, userGroups, principalDn, 
            principal.getAuthenticationLevel(), oldName, null, null,
            RENAME_PERMS, tuples, originalEntry, null );

        next.rename( renameContext );
        tupleCache.subentryRenamed( oldName, newName );
        groupCache.groupRenamed( oldName, newName );
    }


    public void moveAndRename( NextInterceptor next, MoveAndRenameOperationContext moveAndRenameContext )
        throws Exception
    {
        DN oriChildName = moveAndRenameContext.getDn();
        DN newParentName = moveAndRenameContext.getParent();

        Entry entry = moveAndRenameContext.lookup( oriChildName, ByPassConstants.LOOKUP_BYPASS );
        
        LdapPrincipal principal = moveAndRenameContext.getSession().getEffectivePrincipal();
        DN principalDn = principal.getDN();
        DN newName = ( DN ) newParentName.clone();
        newName.add( moveAndRenameContext.getNewRdn().getName() );

        // bypass authz code if we are disabled
        if ( !moveAndRenameContext.getSession().getDirectoryService().isAccessControlEnabled() )
        {
            next.moveAndRename( moveAndRenameContext );
            return;
        }

        protectCriticalEntries( oriChildName );

        // bypass authz code but manage caches if operation is performed by the admin
        if ( isPrincipalAnAdministrator( principalDn ) )
        {
            next.moveAndRename( moveAndRenameContext );
            tupleCache.subentryRenamed( oriChildName, newName );
            groupCache.groupRenamed( oriChildName, newName );
            return;
        }

        Set<DN> userGroups = groupCache.getGroups( principalDn.getNormName() );
        Collection<ACITuple> tuples = new HashSet<ACITuple>();
        addPerscriptiveAciTuples( moveAndRenameContext, tuples, oriChildName, ((ClonedServerEntry)entry).getOriginalEntry() );
        addEntryAciTuples( tuples, entry );
        addSubentryAciTuples( moveAndRenameContext, tuples, oriChildName, entry );

        engine.checkPermission( schemaManager, moveAndRenameContext, userGroups, 
            principalDn, principal.getAuthenticationLevel(), oriChildName, null,
            null, MOVERENAME_PERMS, tuples, entry, null );

        // Get the entry again without operational attributes
        // because access control subentry operational attributes
        // will not be valid at the new location.
        // This will certainly be fixed by the SubentryInterceptor,
        // but after this service.
        
        Entry importedEntry = moveAndRenameContext.lookup( oriChildName, 
            ByPassConstants.LOOKUP_EXCLUDING_OPR_ATTRS_BYPASS );
        
        // As the target entry does not exist yet and so
        // its subentry operational attributes are not there,
        // we need to construct an entry to represent it
        // at least with minimal requirements which are object class
        // and access control subentry operational attributes.
        SubentryInterceptor subentryInterceptor = ( SubentryInterceptor ) chain.get( SubentryInterceptor.class.getName() );
        Entry subentryAttrs = subentryInterceptor.getSubentryAttributes( newName, importedEntry );
        
        for ( EntryAttribute attribute:importedEntry )
        {
            subentryAttrs.put( attribute );
        }
        
        Collection<ACITuple> destTuples = new HashSet<ACITuple>();
        // Import permission is only valid for prescriptive ACIs
        addPerscriptiveAciTuples( moveAndRenameContext, destTuples, newName, subentryAttrs );
        // Evaluate the target context to see whether it
        // allows an entry named newName to be imported as a subordinate.
        engine.checkPermission( schemaManager, moveAndRenameContext, userGroups, principalDn, 
            principal.getAuthenticationLevel(), newName, null,
            null, IMPORT_PERMS, destTuples, subentryAttrs, null );


        next.moveAndRename( moveAndRenameContext );
        tupleCache.subentryRenamed( oriChildName, newName );
        groupCache.groupRenamed( oriChildName, newName );
    }


    public void move( NextInterceptor next, MoveOperationContext moveContext ) throws Exception
    {
        DN oriChildName = moveContext.getDn();
        DN newParentName = moveContext.getParent();
        
        // Access the principal requesting the operation, and bypass checks if it is the admin
        Entry entry = moveContext.lookup( oriChildName, ByPassConstants.LOOKUP_BYPASS );
       
        DN newName = ( DN ) newParentName.clone();
        newName.add( oriChildName.get( oriChildName.size() - 1 ) );
        LdapPrincipal principal = moveContext.getSession().getEffectivePrincipal();
        DN principalDn = principal.getDN();

        // bypass authz code if we are disabled
        if ( !moveContext.getSession().getDirectoryService().isAccessControlEnabled() )
        {
            next.move( moveContext );
            return;
        }

        protectCriticalEntries( oriChildName);

        // bypass authz code but manage caches if operation is performed by the admin
        if ( isPrincipalAnAdministrator( principalDn ) )
        {
            next.move( moveContext );
            tupleCache.subentryRenamed( oriChildName, newName );
            groupCache.groupRenamed( oriChildName, newName );
            return;
        }

        Set<DN> userGroups = groupCache.getGroups( principalDn.getNormName() );
        Collection<ACITuple> tuples = new HashSet<ACITuple>();
        addPerscriptiveAciTuples( moveContext, tuples, oriChildName, ((ClonedServerEntry)entry).getOriginalEntry() );
        addEntryAciTuples( tuples, entry );
        addSubentryAciTuples( moveContext, tuples, oriChildName, entry );

        engine.checkPermission( schemaManager, moveContext, userGroups, principalDn, 
            principal.getAuthenticationLevel(), oriChildName, null,
            null, EXPORT_PERMS, tuples, entry, null );
        
        // Get the entry again without operational attributes
        // because access control subentry operational attributes
        // will not be valid at the new location.
        // This will certainly be fixed by the SubentryInterceptor,
        // but after this service.
        Entry importedEntry = moveContext.lookup( oriChildName, 
            ByPassConstants.LOOKUP_EXCLUDING_OPR_ATTRS_BYPASS );
            
        // As the target entry does not exist yet and so
        // its subentry operational attributes are not there,
        // we need to construct an entry to represent it
        // at least with minimal requirements which are object class
        // and access control subentry operational attributes.
        SubentryInterceptor subentryInterceptor = ( SubentryInterceptor ) 
            chain.get( SubentryInterceptor.class.getName() );
        Entry subentryAttrs = subentryInterceptor.getSubentryAttributes( newName, importedEntry );
        
        for ( EntryAttribute attribute:importedEntry )
        {
            subentryAttrs.put( attribute );
        }
        
        Collection<ACITuple> destTuples = new HashSet<ACITuple>();
        // Import permission is only valid for prescriptive ACIs
        addPerscriptiveAciTuples( moveContext, destTuples, newName, subentryAttrs );
        // Evaluate the target context to see whether it
        // allows an entry named newName to be imported as a subordinate.
        engine.checkPermission( schemaManager, moveContext, userGroups, principalDn, 
            principal.getAuthenticationLevel(), newName, null,
            null, IMPORT_PERMS, destTuples, subentryAttrs, null );

        next.move( moveContext );
        tupleCache.subentryRenamed( oriChildName, newName );
        groupCache.groupRenamed( oriChildName, newName );
    }

    
    public EntryFilteringCursor list( NextInterceptor next, ListOperationContext opContext ) throws Exception
    {
        LdapPrincipal user = opContext.getSession().getEffectivePrincipal();
        EntryFilteringCursor cursor = next.list( opContext );
        
        if ( isPrincipalAnAdministrator( user.getDNRef() ) || !opContext.getSession().getDirectoryService().isAccessControlEnabled() )
        {
            return cursor;
        }
        
        AuthorizationFilter authzFilter = new AuthorizationFilter();
        cursor.addEntryFilter( authzFilter );
        return cursor;
    }


    public EntryFilteringCursor search( NextInterceptor next, SearchOperationContext opContext ) throws Exception
    {
        LdapPrincipal user = opContext.getSession().getEffectivePrincipal();
        DN principalDn = user.getDN();
        EntryFilteringCursor cursor = next.search( opContext );

        boolean isSubschemaSubentryLookup = subschemaSubentryDn.equals( opContext.getDn().getNormName() );
        SearchControls searchCtls = opContext.getSearchControls();
        boolean isRootDSELookup = opContext.getDn().size() == 0 && searchCtls.getSearchScope() == SearchControls.OBJECT_SCOPE;

        if ( isPrincipalAnAdministrator( principalDn ) || !opContext.getSession().getDirectoryService().isAccessControlEnabled() || isRootDSELookup || isSubschemaSubentryLookup )
        {
            return cursor;
        }
        
        cursor.addEntryFilter( new AuthorizationFilter() );
        return cursor;
    }

    
    public final boolean isPrincipalAnAdministrator( DN principalDn )
    {
        return groupCache.isPrincipalAnAdministrator( principalDn );
    }
    

    public boolean compare( NextInterceptor next, CompareOperationContext opContext ) throws Exception
    {
        DN name = opContext.getDn();
        String oid = opContext.getOid();
        Value<?> value = ( Value<?> ) opContext.getValue();

        Entry entry = opContext.lookup( name, ByPassConstants.LOOKUP_BYPASS );

        LdapPrincipal principal = opContext.getSession().getEffectivePrincipal();
        DN principalDn = principal.getDN();

        if ( isPrincipalAnAdministrator( principalDn ) || !opContext.getSession().getDirectoryService().isAccessControlEnabled() )
        {
            return next.compare( opContext );
        }

        Set<DN> userGroups = groupCache.getGroups( principalDn.getNormName() );
        Collection<ACITuple> tuples = new HashSet<ACITuple>();
        addPerscriptiveAciTuples( opContext, tuples, name, ((ClonedServerEntry)entry).getOriginalEntry() );
        addEntryAciTuples( tuples, entry );
        addSubentryAciTuples( opContext, tuples, name, entry );

        engine.checkPermission( schemaManager, opContext, userGroups, principalDn, 
            principal.getAuthenticationLevel(), name, null, null,
            READ_PERMS, tuples, entry, null );
        engine.checkPermission( schemaManager, opContext, userGroups, principalDn, 
            principal.getAuthenticationLevel(), name, oid, value,
            COMPARE_PERMS, tuples, entry, null );

        return next.compare( opContext );
    }


    public DN getMatchedName ( NextInterceptor next, GetMatchedNameOperationContext opContext ) throws Exception
    {
        // Access the principal requesting the operation, and bypass checks if it is the admin
        LdapPrincipal principal = opContext.getSession().getEffectivePrincipal();
        DN principalDn = principal.getDN();
        
        if ( isPrincipalAnAdministrator( principalDn ) || !opContext.getSession().getDirectoryService().isAccessControlEnabled() )
        {
            return next.getMatchedName( opContext );
        }

        // get the present matched name
        Entry entry;
        DN matched = next.getMatchedName( opContext );

        // check if we have disclose on error permission for the entry at the matched dn
        // if not remove rdn and check that until nothing is left in the name and return
        // that but if permission is granted then short the process and return the dn
        while ( matched.size() > 0 )
        {
            entry = opContext.lookup( matched, ByPassConstants.GETMATCHEDDN_BYPASS );
            
            Set<DN> userGroups = groupCache.getGroups( principalDn.getNormName() );
            Collection<ACITuple> tuples = new HashSet<ACITuple>();
            addPerscriptiveAciTuples( opContext, tuples, matched, ((ClonedServerEntry)entry).getOriginalEntry() );
            addEntryAciTuples( tuples, entry );
            addSubentryAciTuples( opContext, tuples, matched, entry );

            if ( engine.hasPermission( schemaManager, opContext, userGroups, principalDn, 
                principal.getAuthenticationLevel(), matched, null,
                null, MATCHEDNAME_PERMS, tuples, entry, null ) )
            {
                return matched;
            }

            matched.remove( matched.size() - 1 );
        }

        return matched;
    }


    public void cacheNewGroup( DN name, Entry entry ) throws Exception
    {
        groupCache.groupAdded( name, entry );
    }


    private boolean filter( OperationContext opContext, DN normName, ClonedServerEntry clonedEntry ) 
        throws Exception
    {
        /*
         * First call hasPermission() for entry level "Browse" and "ReturnDN" perm
         * tests.  If we hasPermission() returns false we immediately short the
         * process and return false.
         */
        
        LdapPrincipal principal = opContext.getSession().getEffectivePrincipal();
        DN userDn = principal.getDN();
        Set<DN> userGroups = groupCache.getGroups( userDn.getNormName() );
        Collection<ACITuple> tuples = new HashSet<ACITuple>();
        addPerscriptiveAciTuples( opContext, tuples, normName, clonedEntry.getOriginalEntry() );
        addEntryAciTuples( tuples, clonedEntry.getOriginalEntry() );
        addSubentryAciTuples( opContext, tuples, normName, clonedEntry.getOriginalEntry() );

        if ( !engine.hasPermission( 
                        schemaManager, 
                        opContext, 
                        userGroups, 
                        userDn, 
                        principal.getAuthenticationLevel(), 
                        normName, 
                        null, 
                        null, 
                        SEARCH_ENTRY_PERMS, 
                        tuples, 
                        clonedEntry.getOriginalEntry(), 
                        null ) )
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
        
        for ( AttributeType attributeType:clonedEntry.getAttributeTypes() )
        {
            // if attribute type scope access is not allowed then remove the attribute and continue
            String id = attributeType.getName();
            EntryAttribute attr = clonedEntry.get( attributeType );
        
            if ( !engine.hasPermission( 
                        schemaManager, 
                        opContext, 
                        userGroups, 
                        userDn,
                        principal.getAuthenticationLevel(), 
                        normName, 
                        id, 
                        null, 
                        SEARCH_ATTRVAL_PERMS, 
                        tuples, 
                        clonedEntry, 
                        null ) )
            {
                attributeToRemove.add( attributeType );
                
                continue;
            }

            List<Value<?>> valueToRemove = new ArrayList<Value<?>>();
            
            // attribute type scope is ok now let's determine value level scope
            for ( Value<?> value:attr )
            {
                if ( !engine.hasPermission( 
                        schemaManager, 
                        opContext, 
                        userGroups, 
                        userDn, 
                        principal.getAuthenticationLevel(), 
                        normName, 
                        attr.getUpId(), 
                        value, 
                        SEARCH_ATTRVAL_PERMS, 
                        tuples,
                        clonedEntry, 
                        null ) )
                {
                    valueToRemove.add( value );
                }
            }
            
            for ( Value<?> value:valueToRemove )
            {
                attr.remove( value );
            }
            
            if ( attr.size() == 0 )
            {
                attributeToRemove.add( attributeType );
            }
        }
        
        for ( AttributeType attributeType:attributeToRemove )
        {
            clonedEntry.removeAttributes( attributeType );
        }

        return true;
    }


    /**
     * WARNING: create one of these filters fresh every time for each new search.
     */
    class AuthorizationFilter implements EntryFilter
    {
        public boolean accept( SearchingOperationContext operationContext, ClonedServerEntry entry ) 
            throws Exception
        {
            DN normName = entry.getDn().normalize( schemaManager.getNormalizerMapping() );
            return filter( operationContext, normName, entry );
        }
    }
}
