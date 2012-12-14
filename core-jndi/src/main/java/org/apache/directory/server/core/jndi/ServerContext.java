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
package org.apache.directory.server.core.jndi;


import java.io.Serializable;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NoPermissionException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.directory.DirContext;
import javax.naming.directory.SchemaViolationException;
import javax.naming.directory.SearchControls;
import javax.naming.event.EventContext;
import javax.naming.event.NamingListener;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapName;
import javax.naming.spi.DirStateFactory;
import javax.naming.spi.DirectoryManager;

import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.OperationEnum;
import org.apache.directory.server.core.api.OperationManager;
import org.apache.directory.server.core.api.entry.ServerEntryUtils;
import org.apache.directory.server.core.api.event.DirectoryListener;
import org.apache.directory.server.core.api.event.NotificationCriteria;
import org.apache.directory.server.core.api.filtering.BaseEntryFilteringCursor;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.api.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.GetRootDseOperationContext;
import org.apache.directory.server.core.api.interceptor.context.HasEntryOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.OperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.shared.DefaultCoreSession;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.asn1.DecoderException;
import org.apache.directory.shared.ldap.codec.api.CodecControl;
import org.apache.directory.shared.ldap.codec.controls.cascade.CascadeDecorator;
import org.apache.directory.shared.ldap.codec.controls.manageDsaIT.ManageDsaITDecorator;
import org.apache.directory.shared.ldap.codec.controls.search.entryChange.EntryChangeDecorator;
import org.apache.directory.shared.ldap.codec.controls.search.pagedSearch.PagedResultsDecorator;
import org.apache.directory.shared.ldap.codec.controls.search.persistentSearch.PersistentSearchDecorator;
import org.apache.directory.shared.ldap.codec.controls.search.subentries.SubentriesDecorator;
import org.apache.directory.shared.ldap.extras.controls.SyncDoneValue;
import org.apache.directory.shared.ldap.extras.controls.SyncInfoValue;
import org.apache.directory.shared.ldap.extras.controls.SyncRequestValue;
import org.apache.directory.shared.ldap.extras.controls.SyncStateValue;
import org.apache.directory.shared.ldap.extras.controls.ppolicy.PasswordPolicy;
import org.apache.directory.shared.ldap.extras.controls.ppolicy.PasswordPolicyImpl;
import org.apache.directory.shared.ldap.extras.controls.ppolicy.PasswordPolicyResponseImpl;
import org.apache.directory.shared.ldap.extras.controls.ppolicy_impl.PasswordPolicyDecorator;
import org.apache.directory.shared.ldap.extras.controls.syncrepl_impl.SyncDoneValueDecorator;
import org.apache.directory.shared.ldap.extras.controls.syncrepl_impl.SyncInfoValueDecorator;
import org.apache.directory.shared.ldap.extras.controls.syncrepl_impl.SyncRequestValueDecorator;
import org.apache.directory.shared.ldap.extras.controls.syncrepl_impl.SyncStateValueDecorator;
import org.apache.directory.shared.ldap.model.constants.JndiPropertyConstants;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.cursor.EmptyCursor;
import org.apache.directory.shared.ldap.model.cursor.SingletonCursor;
import org.apache.directory.shared.ldap.model.entry.AttributeUtils;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidAttributeTypeException;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.model.filter.EqualityNode;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.filter.PresenceNode;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.message.controls.Cascade;
import org.apache.directory.shared.ldap.model.message.controls.CascadeImpl;
import org.apache.directory.shared.ldap.model.message.controls.EntryChange;
import org.apache.directory.shared.ldap.model.message.controls.ManageDsaIT;
import org.apache.directory.shared.ldap.model.message.controls.ManageDsaITImpl;
import org.apache.directory.shared.ldap.model.message.controls.PagedResults;
import org.apache.directory.shared.ldap.model.message.controls.PersistentSearch;
import org.apache.directory.shared.ldap.model.message.controls.Subentries;
import org.apache.directory.shared.ldap.model.name.Ava;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.name.Rdn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.util.JndiUtils;
import org.apache.directory.shared.util.Strings;


/**
 * A non-federated abstract Context implementation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class ServerContext implements EventContext
{
    /** property key used for deleting the old Rdn on a rename */
    public static final String DELETE_OLD_RDN_PROP = JndiPropertyConstants.JNDI_LDAP_DELETE_RDN;

    /** Empty array of controls for use in dealing with them */
    protected static final Control[] EMPTY_CONTROLS = new Control[0];

    /** The directory service which owns this context **/
    private final DirectoryService service;

    /** The SchemManager instance */
    protected SchemaManager schemaManager;

    /** A reference to the ObjectClass AT */
    protected AttributeType OBJECT_CLASS_AT;

    /** The cloned environment used by this Context */
    private final Hashtable<String, Object> env;

    /** The distinguished name of this Context */
    private final Dn dn;

    /** The set of registered NamingListeners */
    private final Map<NamingListener, DirectoryListener> listeners =
        new HashMap<NamingListener, DirectoryListener>();

    /** The request controls to set on operations before performing them */
    protected Control[] requestControls = EMPTY_CONTROLS;

    /** The response controls to set after performing operations */
    protected Control[] responseControls = EMPTY_CONTROLS;

    /** Connection level controls associated with the session */
    protected Control[] connectControls = EMPTY_CONTROLS;

    /** The session */
    private final CoreSession session;

    private static final Map<String, ControlEnum> ADS_CONTROLS = new HashMap<String, ControlEnum>();

    static
    {
        ADS_CONTROLS.put( Cascade.OID, ControlEnum.CASCADE_CONTROL );
        ADS_CONTROLS.put( EntryChange.OID, ControlEnum.ENTRY_CHANGE_CONTROL );
        ADS_CONTROLS.put( ManageDsaIT.OID, ControlEnum.MANAGE_DSA_IT_CONTROL );
        ADS_CONTROLS.put( PagedResults.OID, ControlEnum.PAGED_RESULTS_CONTROL );
        ADS_CONTROLS.put( PasswordPolicy.OID, ControlEnum.PASSWORD_POLICY_REQUEST_CONTROL );
        ADS_CONTROLS.put( PersistentSearch.OID, ControlEnum.PERSISTENT_SEARCH_CONTROL );
        ADS_CONTROLS.put( Subentries.OID, ControlEnum.SUBENTRIES_CONTROL );
        ADS_CONTROLS.put( SyncDoneValue.OID, ControlEnum.SYNC_DONE_VALUE_CONTROL );
        ADS_CONTROLS.put( SyncInfoValue.OID, ControlEnum.SYNC_INFO_VALUE_CONTROL );
        ADS_CONTROLS.put( SyncRequestValue.OID, ControlEnum.SYNC_REQUEST_VALUE_CONTROL );
        ADS_CONTROLS.put( SyncStateValue.OID, ControlEnum.SYNC_STATE_VALUE_CONTROL );
    }


    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------
    /**
     * Must be called by all subclasses to initialize the nexus proxy and the
     * environment settings to be used by this Context implementation.  This
     * specific constructor relies on the presence of the {@link
     * Context#PROVIDER_URL} key and value to determine the distinguished name
     * of the newly created context.  It also checks to make sure the
     * referenced name actually exists within the system.  This constructor
     * is used for all InitialContext requests.
     *
     * @param service the parent service that manages this context
     * @param env the environment properties used by this context.
     * @throws NamingException if the environment parameters are not set
     * correctly.
     */
    protected ServerContext( DirectoryService service, Hashtable<String, Object> env ) throws Exception
    {
        this.service = service;
        this.env = env;

        LdapJndiProperties props = LdapJndiProperties.getLdapJndiProperties( this.env );
        dn = props.getProviderDn();

        /*
         * Need do bind operation here, and bindContext returned contains the
         * newly created session.
         */
        BindOperationContext bindContext = doBindOperation( props.getBindDn(), props.getCredentials(),
            props.getSaslMechanism(), props.getSaslAuthId() );

        session = bindContext.getSession();
        OperationManager operationManager = service.getOperationManager();

        HasEntryOperationContext hasEntryContext = new HasEntryOperationContext( session, dn );

        if ( !operationManager.hasEntry( hasEntryContext ) )
        {
            throw new NameNotFoundException( I18n.err( I18n.ERR_490, dn ) );
        }

        schemaManager = service.getSchemaManager();

        // setup attribute type value
        OBJECT_CLASS_AT = schemaManager.getAttributeType( SchemaConstants.OBJECT_CLASS_AT );
    }


    /**
     * Must be called by all subclasses to initialize the nexus proxy and the
     * environment settings to be used by this Context implementation.  This
     * constructor is used to propagate new contexts from existing contexts.
     *
     * @param service the directory service core
     * @param principal the directory user principal that is propagated
     * @param name the distinguished name of this context
     * @throws NamingException if there is a problem creating the new context
     */
    public ServerContext( DirectoryService service, LdapPrincipal principal, Name name ) throws Exception
    {
        this.service = service;
        this.dn = JndiUtils.fromName( name );

        this.env = new Hashtable<String, Object>();
        this.env.put( PROVIDER_URL, dn.toString() );
        this.env.put( DirectoryService.JNDI_KEY, service );
        session = new DefaultCoreSession( principal, service );
        OperationManager operationManager = service.getOperationManager();

        HasEntryOperationContext hasEntryContext = new HasEntryOperationContext( session, dn );

        if ( !operationManager.hasEntry( hasEntryContext ) )
        {
            throw new NameNotFoundException( I18n.err( I18n.ERR_490, dn ) );
        }

        schemaManager = service.getSchemaManager();

        // setup attribute type value
        OBJECT_CLASS_AT = schemaManager.getAttributeType( SchemaConstants.OBJECT_CLASS_AT );
    }


    public ServerContext( DirectoryService service, CoreSession session, Name name ) throws Exception
    {
        this.service = service;
        this.dn = JndiUtils.fromName( name );
        this.env = new Hashtable<String, Object>();
        this.env.put( PROVIDER_URL, dn.toString() );
        this.env.put( DirectoryService.JNDI_KEY, service );
        this.session = session;
        OperationManager operationManager = service.getOperationManager();

        HasEntryOperationContext hasEntryContext = new HasEntryOperationContext( session, dn );

        if ( !operationManager.hasEntry( hasEntryContext ) )
        {
            throw new NameNotFoundException( I18n.err( I18n.ERR_490, dn ) );
        }

        schemaManager = service.getSchemaManager();

        // setup attribute type value
        OBJECT_CLASS_AT = schemaManager.getAttributeType( SchemaConstants.OBJECT_CLASS_AT );
    }


    /**
     * Set the referral handling flag into the operation context using
     * the JNDI value stored into the environment.
     */
    protected void injectReferralControl( OperationContext opCtx )
    {
        if ( "ignore".equalsIgnoreCase( ( String ) env.get( Context.REFERRAL ) ) )
        {
            opCtx.ignoreReferral();
        }
        else if ( "throw".equalsIgnoreCase( ( String ) env.get( Context.REFERRAL ) ) )
        {
            opCtx.throwReferral();
        }
        else
        {
            // TODO : handle the 'follow' referral option
            opCtx.throwReferral();
        }
    }


    // ------------------------------------------------------------------------
    // Protected Methods for Operations
    // ------------------------------------------------------------------------
    // Use these methods instead of manually calling the nexusProxy so we can
    // add request controls to operation contexts before the call and extract
    // response controls from the contexts after the call.  NOTE that the
    // convertControls( requestControls ) must be cleared after each operation.  This makes a
    // context not thread safe.
    // ------------------------------------------------------------------------

    /**
     * Used to encapsulate [de]marshalling of controls before and after add operations.
     * @param entry
     * @param target
     */
    protected void doAddOperation( Dn target, Entry entry ) throws Exception
    {
        // setup the op context and populate with request controls
        AddOperationContext opCtx = new AddOperationContext( session, entry );

        opCtx.addRequestControls( convertControls( true, requestControls ) );

        // Inject the referral handling into the operation context
        injectReferralControl( opCtx );

        // execute add operation
        OperationManager operationManager = service.getOperationManager();
        operationManager.add( opCtx );

        // clear the request controls and set the response controls
        requestControls = EMPTY_CONTROLS;
        responseControls = JndiUtils.toJndiControls( getDirectoryService().getLdapCodecService(),
            opCtx.getResponseControls() );
    }


    /**
     * Used to encapsulate [de]marshalling of controls before and after delete operations.
     * @param target
     */
    protected void doDeleteOperation( Dn target ) throws Exception
    {
        // setup the op context and populate with request controls
        DeleteOperationContext deleteContext = new DeleteOperationContext( session, target );

        deleteContext.addRequestControls( convertControls( true, requestControls ) );

        // Inject the referral handling into the operation context
        injectReferralControl( deleteContext );

        // execute delete operation
        OperationManager operationManager = service.getOperationManager();
        operationManager.delete( deleteContext );

        // clear the request controls and set the response controls
        requestControls = EMPTY_CONTROLS;
        responseControls = JndiUtils.toJndiControls( getDirectoryService().getLdapCodecService(),
            deleteContext.getResponseControls() );
    }


    private org.apache.directory.shared.ldap.model.message.Control convertControl( boolean isRequest,
        Control jndiControl ) throws DecoderException
    {
        String controlIDStr = jndiControl.getID();
        CodecControl<? extends org.apache.directory.shared.ldap.model.message.Control> control = null;

        ControlEnum controlId = ADS_CONTROLS.get( controlIDStr );

        switch ( controlId )
        {
            case CASCADE_CONTROL:
                control = new CascadeDecorator( getDirectoryService().getLdapCodecService(), new CascadeImpl() );

                break;

            case ENTRY_CHANGE_CONTROL:
                control = new EntryChangeDecorator( getDirectoryService().getLdapCodecService() );

                break;

            case MANAGE_DSA_IT_CONTROL:
                control = new ManageDsaITDecorator( getDirectoryService().getLdapCodecService(), new ManageDsaITImpl() );

                break;

            case PAGED_RESULTS_CONTROL:
                control = new PagedResultsDecorator( getDirectoryService().getLdapCodecService() );

                break;

            case PASSWORD_POLICY_REQUEST_CONTROL:
                if ( isRequest )
                {
                    control = new PasswordPolicyDecorator( getDirectoryService().getLdapCodecService(),
                        new PasswordPolicyImpl() );
                }
                else
                {
                    control = new PasswordPolicyDecorator( getDirectoryService().getLdapCodecService(),
                        new PasswordPolicyImpl( new PasswordPolicyResponseImpl() ) );
                }

                break;

            case PERSISTENT_SEARCH_CONTROL:
                control = new PersistentSearchDecorator( getDirectoryService().getLdapCodecService() );

                break;

            case SUBENTRIES_CONTROL:
                control = new SubentriesDecorator( getDirectoryService().getLdapCodecService() );

                break;

            case SYNC_DONE_VALUE_CONTROL:
                control = new SyncDoneValueDecorator( getDirectoryService().getLdapCodecService() );

                break;

            case SYNC_INFO_VALUE_CONTROL:
                control = new SyncInfoValueDecorator( getDirectoryService().getLdapCodecService() );

                break;

            case SYNC_REQUEST_VALUE_CONTROL:
                control = new SyncRequestValueDecorator( getDirectoryService().getLdapCodecService() );

                break;

            case SYNC_STATE_VALUE_CONTROL:
                control = new SyncStateValueDecorator( getDirectoryService().getLdapCodecService() );

                break;
        }

        control.setCritical( jndiControl.isCritical() );
        control.setValue( jndiControl.getEncodedValue() );

        byte[] value = jndiControl.getEncodedValue();

        if ( !Strings.isEmpty( value ) )
        {
            control.decode( value );
        }

        return control;
    }


    /**
     * Convert the JNDI controls to ADS controls
     * TODO convertControls.
     */
    private org.apache.directory.shared.ldap.model.message.Control[] convertControls( boolean isRequest,
        Control[] jndiControls ) throws DecoderException
    {
        if ( jndiControls != null )
        {
            org.apache.directory.shared.ldap.model.message.Control[] controls =
                new org.apache.directory.shared.ldap.model.message.Control[jndiControls.length];
            int i = 0;

            for ( javax.naming.ldap.Control jndiControl : jndiControls )
            {
                controls[i++] = convertControl( isRequest, jndiControl );
            }

            return controls;
        }
        else
        {
            return null;
        }

    }


    /**
     * Used to encapsulate [de]marshalling of controls before and after list operations.
     * @param dn
     * @param aliasDerefMode
     * @param filter
     * @param searchControls
     * @return NamingEnumeration
     */
    protected EntryFilteringCursor doSearchOperation( Dn dn, AliasDerefMode aliasDerefMode,
        ExprNode filter, SearchControls searchControls ) throws Exception
    {
        OperationManager operationManager = service.getOperationManager();
        EntryFilteringCursor results = null;

        Object typesOnlyObj = getEnvironment().get( "java.naming.ldap.typesOnly" );
        boolean typesOnly = false;

        if ( typesOnlyObj != null )
        {
            typesOnly = Boolean.parseBoolean( typesOnlyObj.toString() );
        }

        SearchOperationContext searchContext = null;

        // We have to check if it's a compare operation or a search.
        // A compare operation has a OBJECT scope search, the filter must
        // be of the form (object=value) (no wildcards), and no attributes
        // should be asked to be returned.
        if ( ( searchControls.getSearchScope() == SearchControls.OBJECT_SCOPE )
            && ( ( searchControls.getReturningAttributes() != null )
            && ( searchControls.getReturningAttributes().length == 0 ) )
            && ( filter instanceof EqualityNode ) )
        {
            CompareOperationContext compareContext = new CompareOperationContext( session, dn,
                ( ( EqualityNode<?> ) filter ).getAttribute(), ( ( EqualityNode ) filter ).getValue() );

            // Inject the referral handling into the operation context
            injectReferralControl( compareContext );

            // Call the operation
            boolean result = operationManager.compare( compareContext );

            // setup the op context and populate with request controls
            searchContext = new SearchOperationContext( session, dn, filter,
                searchControls );
            searchContext.setAliasDerefMode( aliasDerefMode );
            searchContext.addRequestControls( convertControls( true, requestControls ) );

            searchContext.setTypesOnly( typesOnly );

            if ( result )
            {
                Entry emptyEntry = new DefaultEntry( service.getSchemaManager(), Dn.EMPTY_DN );
                return new BaseEntryFilteringCursor( new SingletonCursor<Entry>( emptyEntry ), searchContext );
            }
            else
            {
                return new BaseEntryFilteringCursor( new EmptyCursor<Entry>(), searchContext );
            }
        }
        else
        {
            // It's a Search

            // setup the op context and populate with request controls
            searchContext = new SearchOperationContext( session, dn, filter, searchControls );
            searchContext.setAliasDerefMode( aliasDerefMode );
            searchContext.addRequestControls( convertControls( true, requestControls ) );
            searchContext.setTypesOnly( typesOnly );

            // Inject the referral handling into the operation context
            injectReferralControl( searchContext );

            // execute search operation
            results = operationManager.search( searchContext );
        }

        // clear the request controls and set the response controls
        requestControls = EMPTY_CONTROLS;
        responseControls = JndiUtils.toJndiControls( getDirectoryService().getLdapCodecService(),
            searchContext.getResponseControls() );

        return results;
    }


    /**
     * Used to encapsulate [de]marshalling of controls before and after list operations.
     */
    protected EntryFilteringCursor doListOperation( Dn target ) throws Exception
    {
        // setup the op context and populate with request controls
        PresenceNode filter = new PresenceNode( OBJECT_CLASS_AT );
        SearchOperationContext searchContext = new SearchOperationContext( session, target, SearchScope.ONELEVEL, filter, SchemaConstants.ALL_USER_ATTRIBUTES_ARRAY );
        searchContext.addRequestControls( convertControls( true, requestControls ) );

        // execute search operation
        OperationManager operationManager = service.getOperationManager();
        EntryFilteringCursor results = operationManager.search( searchContext );

        // clear the request controls and set the response controls
        requestControls = EMPTY_CONTROLS;
        responseControls = JndiUtils.toJndiControls( getDirectoryService().getLdapCodecService(),
            searchContext.getResponseControls() );

        return results;
    }


    protected Entry doGetRootDseOperation( Dn target ) throws Exception
    {
        GetRootDseOperationContext getRootDseContext = new GetRootDseOperationContext( session, target );
        getRootDseContext.addRequestControls( convertControls( true, requestControls ) );

        // do not reset request controls since this is not an external
        // operation and not do bother setting the response controls either
        OperationManager operationManager = service.getOperationManager();

        return operationManager.getRootDse( getRootDseContext );
    }


    /**
     * Used to encapsulate [de]marshalling of controls before and after lookup operations.
     */
    protected Entry doLookupOperation( Dn target ) throws Exception
    {
        // setup the op context and populate with request controls
        // execute lookup/getRootDSE operation
        LookupOperationContext lookupContext = new LookupOperationContext( session, target );
        lookupContext.addRequestControls( convertControls( true, requestControls ) );
        OperationManager operationManager = service.getOperationManager();
        Entry serverEntry = operationManager.lookup( lookupContext );

        // clear the request controls and set the response controls
        requestControls = EMPTY_CONTROLS;
        responseControls = JndiUtils.toJndiControls( getDirectoryService().getLdapCodecService(),
            lookupContext.getResponseControls() );
        return serverEntry;
    }


    /**
     * Used to encapsulate [de]marshalling of controls before and after lookup operations.
     */
    protected Entry doLookupOperation( Dn target, String[] attrIds ) throws Exception
    {
        // setup the op context and populate with request controls
        // execute lookup/getRootDSE operation
        LookupOperationContext lookupContext = new LookupOperationContext( session, target, attrIds );
        lookupContext.addRequestControls( convertControls( true, requestControls ) );
        OperationManager operationManager = service.getOperationManager();
        Entry serverEntry = operationManager.lookup( lookupContext );

        // clear the request controls and set the response controls
        requestControls = EMPTY_CONTROLS;
        responseControls = JndiUtils.toJndiControls(
            getDirectoryService().getLdapCodecService(),
            lookupContext.getResponseControls() );

        // Now remove the ObjectClass attribute if it has not been requested
        if ( ( lookupContext.getReturningAttributes() != null ) && ( lookupContext.getReturningAttributes().size() != 0 ) &&
            ( ( serverEntry.get( SchemaConstants.OBJECT_CLASS_AT ) != null )
            && ( serverEntry.get( SchemaConstants.OBJECT_CLASS_AT ).size() == 0 ) ) )
        {
            serverEntry.removeAttributes( SchemaConstants.OBJECT_CLASS_AT );
        }

        return serverEntry;
    }


    /**
     * Used to encapsulate [de]marshalling of controls before and after bind operations.
     */
    protected BindOperationContext doBindOperation( Dn bindDn, byte[] credentials, String saslMechanism,
        String saslAuthId ) throws Exception
    {
        // setup the op context and populate with request controls
        BindOperationContext bindContext = new BindOperationContext( null );
        bindContext.setDn( bindDn );
        bindContext.setCredentials( credentials );
        bindContext.setSaslMechanism( saslMechanism );
        bindContext.setSaslAuthId( saslAuthId );
        bindContext.addRequestControls( convertControls( true, requestControls ) );
        bindContext.setInterceptors( getDirectoryService().getInterceptors( OperationEnum.BIND ) );

        // execute bind operation
        OperationManager operationManager = service.getOperationManager();
        operationManager.bind( bindContext );

        // clear the request controls and set the response controls
        requestControls = EMPTY_CONTROLS;
        responseControls = JndiUtils.toJndiControls( getDirectoryService().getLdapCodecService(),
            bindContext.getResponseControls() );
        return bindContext;
    }


    /**
     * Used to encapsulate [de]marshalling of controls before and after moveAndRename operations.
     */
    protected void doMoveAndRenameOperation( Dn oldDn, Dn parent, Rdn newRdn, boolean delOldDn )
        throws Exception
    {
        // setup the op context and populate with request controls
        MoveAndRenameOperationContext moveAndRenameContext = new MoveAndRenameOperationContext( session, oldDn, parent,
            new Rdn(
                newRdn ), delOldDn );
        moveAndRenameContext.addRequestControls( convertControls( true, requestControls ) );

        // Inject the referral handling into the operation context
        injectReferralControl( moveAndRenameContext );

        // execute moveAndRename operation
        OperationManager operationManager = service.getOperationManager();
        operationManager.moveAndRename( moveAndRenameContext );

        // clear the request controls and set the response controls
        requestControls = EMPTY_CONTROLS;
        responseControls = JndiUtils.toJndiControls( getDirectoryService().getLdapCodecService(),
            moveAndRenameContext.getResponseControls() );
    }


    /**
     * Used to encapsulate [de]marshalling of controls before and after modify operations.
     */
    protected void doModifyOperation( Dn dn, List<Modification> modifications ) throws Exception
    {
        // setup the op context and populate with request controls
        ModifyOperationContext modifyContext = new ModifyOperationContext( session, dn, modifications );
        modifyContext.addRequestControls( convertControls( true, requestControls ) );

        // Inject the referral handling into the operation context
        injectReferralControl( modifyContext );

        // execute modify operation
        OperationManager operationManager = service.getOperationManager();
        operationManager.modify( modifyContext );

        // clear the request controls and set the response controls
        requestControls = EMPTY_CONTROLS;
        responseControls = JndiUtils.toJndiControls( getDirectoryService().getLdapCodecService(),
            modifyContext.getResponseControls() );
    }


    /**
     * Used to encapsulate [de]marshalling of controls before and after moveAndRename operations.
     */
    protected void doMove( Dn oldDn, Dn target ) throws Exception
    {
        // setup the op context and populate with request controls
        MoveOperationContext moveContext = new MoveOperationContext( session, oldDn, target );
        moveContext.addRequestControls( convertControls( true, requestControls ) );

        // Inject the referral handling into the operation context
        injectReferralControl( moveContext );

        // execute move operation
        OperationManager operationManager = service.getOperationManager();
        operationManager.move( moveContext );

        // clear the request controls and set the response controls
        requestControls = EMPTY_CONTROLS;
        responseControls = JndiUtils.toJndiControls( getDirectoryService().getLdapCodecService(),
            moveContext.getResponseControls() );
    }


    /**
     * Used to encapsulate [de]marshalling of controls before and after rename operations.
     */
    protected void doRename( Dn oldDn, Rdn newRdn, boolean delOldRdn ) throws Exception
    {
        // setup the op context and populate with request controls
        RenameOperationContext renameContext = new RenameOperationContext( session, oldDn, newRdn, delOldRdn );
        renameContext.addRequestControls( convertControls( true, requestControls ) );

        // Inject the referral handling into the operation context
        injectReferralControl( renameContext );

        // execute rename operation
        OperationManager operationManager = service.getOperationManager();
        operationManager.rename( renameContext );

        // clear the request controls and set the response controls
        requestControls = EMPTY_CONTROLS;
        responseControls = JndiUtils.toJndiControls( getDirectoryService().getLdapCodecService(),
            renameContext.getResponseControls() );
    }


    public CoreSession getSession()
    {
        return session;
    }


    public DirectoryService getDirectoryService()
    {
        return service;
    }


    // ------------------------------------------------------------------------
    // New Impl Specific Public Methods
    // ------------------------------------------------------------------------

    /**
     * Gets a handle on the root context of the DIT.  The RootDSE as the present user.
     *
     * @return the rootDSE context
     * @throws NamingException if this fails
     */
    public abstract ServerContext getRootContext() throws NamingException;


    /**
     * Gets the {@link DirectoryService} associated with this context.
     *
     * @return the directory service associated with this context
     */
    public DirectoryService getService()
    {
        return service;
    }


    // ------------------------------------------------------------------------
    // Protected Accessor Methods
    // ------------------------------------------------------------------------

    /**
     * Gets the distinguished name of the entry associated with this Context.
     *
     * @return the distinguished name of this Context's entry.
     */
    protected Dn getDn()
    {
        return dn;
    }


    // ------------------------------------------------------------------------
    // JNDI Context Interface Methods
    // ------------------------------------------------------------------------

    /**
     * @see javax.naming.Context#close()
     */
    public void close() throws NamingException
    {
        for ( DirectoryListener listener : listeners.values() )
        {
            try
            {
                service.getEventService().removeListener( listener );
            }
            catch ( Exception e )
            {
                JndiUtils.wrap( e );
            }
        }

        listeners.clear();
    }


    /**
     * @see javax.naming.Context#getNameInNamespace()
     */
    public String getNameInNamespace() throws NamingException
    {
        return dn.getName();
    }


    /**
     * @see javax.naming.Context#getEnvironment()
     */
    public Hashtable<String, Object> getEnvironment()
    {
        return env;
    }


    /**
     * @see javax.naming.Context#addToEnvironment(java.lang.String,
     * java.lang.Object)
     */
    public Object addToEnvironment( String propName, Object propVal ) throws NamingException
    {
        return env.put( propName, propVal );
    }


    /**
     * @see javax.naming.Context#removeFromEnvironment(java.lang.String)
     */
    public Object removeFromEnvironment( String propName ) throws NamingException
    {
        return env.remove( propName );
    }


    /**
     * @see javax.naming.Context#createSubcontext(java.lang.String)
     */
    public Context createSubcontext( String name ) throws NamingException
    {
        return createSubcontext( new LdapName( name ) );
    }


    /**
     * @see javax.naming.Context#createSubcontext(javax.naming.Name)
     */
    public Context createSubcontext( Name name ) throws NamingException
    {
        Dn target = buildTarget( JndiUtils.fromName( name ) );
        Entry serverEntry = null;

        try
        {
            serverEntry = service.newEntry( target );
        }
        catch ( LdapException le )
        {
            throw new NamingException( le.getMessage() );
        }

        try
        {
            serverEntry.add( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.TOP_OC, JavaLdapSupport.JCONTAINER_ATTR );
        }
        catch ( LdapException le )
        {
            throw new SchemaViolationException( I18n.err( I18n.ERR_491, name ) );
        }

        // Now add the CN attribute, which is mandatory
        Rdn rdn = target.getRdn();

        if ( rdn != null )
        {
            if ( SchemaConstants.CN_AT_OID.equals( rdn.getNormType() ) )
            {
                serverEntry.put( rdn.getType(), rdn.getValue() );
            }
            else
            {
                // No CN in the rdn, this is an error
                throw new SchemaViolationException( I18n.err( I18n.ERR_491, name ) );
            }
        }
        else
        {
            // No CN in the rdn, this is an error
            throw new SchemaViolationException( I18n.err( I18n.ERR_491, name ) );
        }

        /*
         * Add the new context to the server which as a side effect adds
         * operational attributes to the serverEntry refering instance which
         * can them be used to initialize a new ServerLdapContext.  Remember
         * we need to copy over the controls as well to propagate the complete
         * environment besides what's in the hashtable for env.
         */
        try
        {
            doAddOperation( target, serverEntry );
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
        }

        ServerLdapContext ctx = null;

        try
        {
            ctx = new ServerLdapContext( service, session.getEffectivePrincipal(), JndiUtils.toName( target ) );
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
        }

        return ctx;
    }


    /**
     * @see javax.naming.Context#destroySubcontext(java.lang.String)
     */
    public void destroySubcontext( String name ) throws NamingException
    {
        destroySubcontext( new LdapName( name ) );
    }


    /**
     * @see javax.naming.Context#destroySubcontext(javax.naming.Name)
     */
    public void destroySubcontext( Name name ) throws NamingException
    {
        Dn target = buildTarget( JndiUtils.fromName( name ) );

        if ( target.size() == 0 )
        {
            throw new NoPermissionException( I18n.err( I18n.ERR_492 ) );
        }

        try
        {
            doDeleteOperation( target );
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
        }
    }


    /**
     * @see javax.naming.Context#bind(java.lang.String, java.lang.Object)
     */
    public void bind( String name, Object obj ) throws NamingException
    {
        bind( new LdapName( name ), obj );
    }


    private void injectRdnAttributeValues( Dn target, Entry serverEntry ) throws NamingException
    {
        // Add all the Rdn attributes and their values to this entry
        Rdn rdn = target.getRdn();

        if ( rdn.size() == 1 )
        {
            serverEntry.put( rdn.getType(), rdn.getValue() );
        }
        else
        {
            for ( Ava atav : rdn )
            {
                serverEntry.put( atav.getType(), atav.getValue() );
            }
        }
    }


    /**
     * @see javax.naming.Context#bind(javax.naming.Name, java.lang.Object)
     */
    public void bind( Name name, Object obj ) throws NamingException
    {
        // First, use state factories to do a transformation
        DirStateFactory.Result res = DirectoryManager.getStateToBind( obj, name, this, env, null );

        Dn target = buildTarget( JndiUtils.fromName( name ) );

        // let's be sure that the Attributes is case insensitive
        Entry outServerEntry = null;

        try
        {
            outServerEntry = ServerEntryUtils.toServerEntry( AttributeUtils.toCaseInsensitive( res
                .getAttributes() ), target, service.getSchemaManager() );
        }
        catch ( LdapInvalidAttributeTypeException liate )
        {
            throw new NamingException( I18n.err( I18n.ERR_495, obj ) );
        }

        if ( outServerEntry != null )
        {
            try
            {
                // Remember : a JNDI Bind is a LDAP Add ! Silly, insn't it ?
                doAddOperation( target, outServerEntry );
            }
            catch ( Exception e )
            {
                JndiUtils.wrap( e );
            }
            
            return;
        }

        if ( obj instanceof Entry )
        {
            try
            {
                doAddOperation( target, ( Entry ) obj );
            }
            catch ( Exception e )
            {
                JndiUtils.wrap( e );
            }
        }
        // Check for Referenceable
        else if ( obj instanceof Referenceable )
        {
            throw new NamingException( I18n.err( I18n.ERR_493 ) );
        }
        // Store different formats
        else if ( obj instanceof Reference )
        {
            // Store as ref and add outAttrs
            throw new NamingException( I18n.err( I18n.ERR_494 ) );
        }
        else if ( obj instanceof Serializable )
        {
            // Serialize and add outAttrs
            Entry serverEntry = null;

            try
            {
                serverEntry = service.newEntry( target );
            }
            catch ( LdapException le )
            {
                throw new NamingException( I18n.err( I18n.ERR_495, obj ) );
            }

            // Get target and inject all rdn attributes into entry
            injectRdnAttributeValues( target, serverEntry );

            // Serialize object into entry attributes and add it.
            try
            {
                JavaLdapSupport.serialize( serverEntry, obj, service.getSchemaManager() );
            }
            catch ( LdapException le )
            {
                throw new NamingException( I18n.err( I18n.ERR_495, obj ) );
            }

            try
            {
                doAddOperation( target, serverEntry );
            }
            catch ( Exception e )
            {
                JndiUtils.wrap( e );
            }
        }
        else if ( obj instanceof DirContext )
        {
            // Grab attributes and merge with outAttrs
            Entry serverEntry = null;

            try
            {
                serverEntry = ServerEntryUtils.toServerEntry( ( ( DirContext ) obj ).getAttributes( "" ),
                    target, service.getSchemaManager() );
            }
            catch ( LdapInvalidAttributeTypeException liate )
            {
                throw new NamingException( I18n.err( I18n.ERR_495, obj ) );
            }

            injectRdnAttributeValues( target, serverEntry );
            
            try
            {
                doAddOperation( target, serverEntry );
            }
            catch ( Exception e )
            {
                JndiUtils.wrap( e );
            }
        }
        else
        {
            throw new NamingException( I18n.err( I18n.ERR_495, obj ) );
        }
    }


    /**
     * @see javax.naming.Context#rename(java.lang.String, java.lang.String)
     */
    public void rename( String oldName, String newName ) throws NamingException
    {
        rename( new LdapName( oldName ), new LdapName( newName ) );
    }


    /**
     * @see javax.naming.Context#rename(javax.naming.Name, javax.naming.Name)
     */
    public void rename( Name oldName, Name newName ) throws NamingException
    {
        Dn oldDn = buildTarget( JndiUtils.fromName( oldName ) );
        Dn newDn = buildTarget( JndiUtils.fromName( newName ) );

        if ( oldDn.size() == 0 )
        {
            throw new NoPermissionException( I18n.err( I18n.ERR_312 ) );
        }

        // calculate parents
        Dn oldParent = oldDn;

        oldParent = oldParent.getParent();

        Dn newParent = newDn;

        newParent = newParent.getParent();

        Rdn oldRdn = oldDn.getRdn();
        Rdn newRdn = newDn.getRdn();
        boolean delOldRdn = true;

        /*
         * Attempt to use the java.naming.ldap.deleteRDN environment property
         * to get an override for the deleteOldRdn option to modifyRdn.
         */
        if ( null != env.get( DELETE_OLD_RDN_PROP ) )
        {
            String delOldRdnStr = ( String ) env.get( DELETE_OLD_RDN_PROP );
            delOldRdn = !delOldRdnStr.equalsIgnoreCase( "false" ) && !delOldRdnStr.equalsIgnoreCase( "no" )
                && !delOldRdnStr.equals( "0" );
        }

        /*
         * We need to determine if this rename operation corresponds to a simple
         * Rdn name change or a move operation.  If the two names are the same
         * except for the Rdn then it is a simple modifyRdn operation.  If the
         * names differ in size or have a different baseDN then the operation is
         * a move operation.  Furthermore if the Rdn in the move operation
         * changes it is both an Rdn change and a move operation.
         */
        if ( oldParent.equals( newParent ) )
        {
            try
            {
                doRename( oldDn, newRdn, delOldRdn );
            }
            catch ( Exception e )
            {
                JndiUtils.wrap( e );
            }
        }
        else
        {
            if ( newRdn.equals( oldRdn ) )
            {
                try
                {
                    doMove( oldDn, newParent );
                }
                catch ( Exception e )
                {
                    JndiUtils.wrap( e );
                }
            }
            else
            {
                try
                {
                    doMoveAndRenameOperation( oldDn, newParent, newRdn, delOldRdn );
                }
                catch ( Exception e )
                {
                    JndiUtils.wrap( e );
                }
            }
        }
    }


    /**
     * @see javax.naming.Context#rebind(java.lang.String, java.lang.Object)
     */
    public void rebind( String name, Object obj ) throws NamingException
    {
        rebind( new LdapName( name ), obj );
    }


    /**
     * @see javax.naming.Context#rebind(javax.naming.Name, java.lang.Object)
     */
    public void rebind( Name name, Object obj ) throws NamingException
    {
        Dn target = buildTarget( JndiUtils.fromName( name ) );
        OperationManager operationManager = service.getOperationManager();

        try
        {
            HasEntryOperationContext hasEntryContext = new HasEntryOperationContext( session, target );

            if ( operationManager.hasEntry( hasEntryContext ) )
            {
                doDeleteOperation( target );
            }
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
        }

        bind( name, obj );
    }


    /**
     * @see javax.naming.Context#unbind(java.lang.String)
     */
    public void unbind( String name ) throws NamingException
    {
        unbind( new LdapName( name ) );
    }


    /**
     * @see javax.naming.Context#unbind(javax.naming.Name)
     */
    public void unbind( Name name ) throws NamingException
    {
        try
        {
            doDeleteOperation( buildTarget( JndiUtils.fromName( name ) ) );
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
        }
    }


    /**
     * @see javax.naming.Context#lookup(java.lang.String)
     */
    public Object lookup( String name ) throws NamingException
    {
        if ( Strings.isEmpty( name ) )
        {
            return lookup( new LdapName( "" ) );
        }
        else
        {
            return lookup( new LdapName( name ) );
        }
    }


    /**
     * @see javax.naming.Context#lookup(javax.naming.Name)
     */
    public Object lookup( Name name ) throws NamingException
    {
        Object obj;
        Dn target = buildTarget( JndiUtils.fromName( name ) );

        Entry serverEntry = null;

        try
        {
            if ( name.size() == 0 )
            {
                serverEntry = doGetRootDseOperation( target );
            }
            else
            {
                serverEntry = doLookupOperation( target );
            }
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
        }

        try
        {
            obj = DirectoryManager.getObjectInstance( null, name, this, env,
                ServerEntryUtils.toBasicAttributes( serverEntry ) );
        }
        catch ( Exception e )
        {
            String msg = I18n.err( I18n.ERR_497, target );
            NamingException ne = new NamingException( msg );
            ne.setRootCause( e );
            throw ne;
        }

        if ( obj != null )
        {
            return obj;
        }

        // First lets test and see if the entry is a serialized java object
        if ( serverEntry.get( JavaLdapSupport.JCLASSNAME_ATTR ) != null )
        {
            // Give back serialized object and not a context
            return JavaLdapSupport.deserialize( serverEntry );
        }

        // Initialize and return a context since the entry is not a java object
        ServerLdapContext ctx = null;

        try
        {
            ctx = new ServerLdapContext( service, session.getEffectivePrincipal(), JndiUtils.toName( target ) );
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
        }

        return ctx;
    }


    /**
     * @see javax.naming.Context#lookupLink(java.lang.String)
     */
    public Object lookupLink( String name ) throws NamingException
    {
        throw new UnsupportedOperationException();
    }


    /**
     * @see javax.naming.Context#lookupLink(javax.naming.Name)
     */
    public Object lookupLink( Name name ) throws NamingException
    {
        throw new UnsupportedOperationException();
    }


    /**
     * Non-federated implementation presuming the name argument is not a
     * composite name spanning multiple namespaces but a compound name in
     * the same LDAP namespace.  Hence the parser returned is always the
     * same as calling this method with the empty String.
     *
     * @see javax.naming.Context#getNameParser(java.lang.String)
     */
    public NameParser getNameParser( String name ) throws NamingException
    {
        return new NameParser()
        {
            public Name parse( String name ) throws NamingException
            {
                try
                {
                    return JndiUtils.toName( new Dn( name ) );
                }
                catch ( LdapInvalidDnException lide )
                {
                    throw new InvalidNameException( lide.getMessage() );
                }
            }
        };
    }


    /**
     * Non-federated implementation presuming the name argument is not a
     * composite name spanning multiple namespaces but a compound name in
     * the same LDAP namespace.  Hence the parser returned is always the
     * same as calling this method with the empty String Name.
     *
     * @see javax.naming.Context#getNameParser(javax.naming.Name)
     */
    public NameParser getNameParser( final Name name ) throws NamingException
    {
        return new NameParser()
        {
            public Name parse( String n ) throws NamingException
            {
                try
                {
                    return JndiUtils.toName( new Dn( name.toString() ) );
                }
                catch ( LdapInvalidDnException lide )
                {
                    throw new InvalidNameException( lide.getMessage() );
                }
            }
        };
    }


    /**
     * @see javax.naming.Context#list(java.lang.String)
     */
    @SuppressWarnings(value =
        { "unchecked" })
    public NamingEnumeration list( String name ) throws NamingException
    {
        return list( new LdapName( name ) );
    }


    /**
     * @see javax.naming.Context#list(javax.naming.Name)
     */
    @SuppressWarnings(value =
        { "unchecked" })
    public NamingEnumeration list( Name name ) throws NamingException
    {
        try
        {
            return new NamingEnumerationAdapter( doListOperation( buildTarget( JndiUtils.fromName( name ) ) ) );
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
            return null; // shut up compiler
        }
    }


    /**
     * @see javax.naming.Context#listBindings(java.lang.String)
     */
    @SuppressWarnings(value =
        { "unchecked" })
    public NamingEnumeration listBindings( String name ) throws NamingException
    {
        return listBindings( new LdapName( name ) );
    }


    /**
     * @see javax.naming.Context#listBindings(javax.naming.Name)
     */
    @SuppressWarnings(value =
        { "unchecked" })
    public NamingEnumeration listBindings( Name name ) throws NamingException
    {
        // Conduct a special one level search at base for all objects
        Dn base = buildTarget( JndiUtils.fromName( name ) );
        PresenceNode filter = new PresenceNode( OBJECT_CLASS_AT );
        SearchControls ctls = new SearchControls();
        ctls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        AliasDerefMode aliasDerefMode = AliasDerefMode.getEnum( getEnvironment() );
        try
        {
            return new NamingEnumerationAdapter( doSearchOperation( base, aliasDerefMode, filter, ctls ) );
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
            return null; // shutup compiler
        }
    }


    /**
     * @see javax.naming.Context#composeName(java.lang.String, java.lang.String)
     */
    public String composeName( String name, String prefix ) throws NamingException
    {
        return composeName( new LdapName( name ), new LdapName( prefix ) ).toString();
    }


    /**
     * @see javax.naming.Context#composeName(javax.naming.Name,
     * javax.naming.Name)
     */
    public Name composeName( Name name, Name prefix ) throws NamingException
    {
        // No prefix reduces to name, or the name relative to this context
        if ( ( prefix == null ) || ( prefix.size() == 0 ) )
        {
            return name;
        }

        /*
         * Example: This context is ou=people and say name is the relative
         * name of uid=jwalker and the prefix is dc=domain.  Then we must
         * compose the name relative to prefix which would be:
         *
         * uid=jwalker,ou=people,dc=domain.
         *
         * The following general algorithm generates the right name:
         *      1). Find the Dn for name and walk it from the head to tail
         *          trying to match for the head of prefix.
         *      2). Remove name components from the Dn until a match for the
         *          head of the prefix is found.
         *      3). Return the remainder of the fqn or Dn after chewing off some
         */

        // 1). Find the Dn for name and walk it from the head to tail
        Dn fqn = buildTarget( JndiUtils.fromName( name ) );

        try
        {
            return JndiUtils.toName( JndiUtils.fromName( prefix ).add( fqn ) );
        }
        catch ( LdapInvalidDnException lide )
        {
            throw new InvalidNameException( lide.getMessage() );
        }
    }


    // ------------------------------------------------------------------------
    // EventContext implementations
    // ------------------------------------------------------------------------

    public void addNamingListener( Name name, int scope, NamingListener namingListener ) throws NamingException
    {
        ExprNode filter = new PresenceNode( OBJECT_CLASS_AT );

        try
        {
            DirectoryListener listener = new EventListenerAdapter( ( ServerLdapContext ) this, namingListener );
            NotificationCriteria criteria = new NotificationCriteria();
            criteria.setFilter( filter );
            criteria.setScope( SearchScope.getSearchScope( scope ) );
            criteria.setAliasDerefMode( AliasDerefMode.getEnum( env ) );
            criteria.setBase( buildTarget( JndiUtils.fromName( name ) ) );

            service.getEventService().addListener( listener, criteria );
            listeners.put( namingListener, listener );
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
        }
    }


    public void addNamingListener( String name, int scope, NamingListener namingListener ) throws NamingException
    {
        addNamingListener( new LdapName( name ), scope, namingListener );
    }


    public void removeNamingListener( NamingListener namingListener ) throws NamingException
    {
        try
        {
            DirectoryListener listener = listeners.remove( namingListener );

            if ( listener != null )
            {
                service.getEventService().removeListener( listener );
            }
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
        }
    }


    public boolean targetMustExist() throws NamingException
    {
        return false;
    }


    /**
     * Allows subclasses to register and unregister listeners.
     *
     * @return the set of listeners used for tracking registered name listeners.
     */
    protected Map<NamingListener, DirectoryListener> getListeners()
    {
        return listeners;
    }


    // ------------------------------------------------------------------------
    // Utility Methods to Reduce Code
    // ------------------------------------------------------------------------

    /**
     * Clones this context's Dn and adds the components of the name relative to
     * this context to the left hand side of this context's cloned Dn.
     *
     * @param relativeName a name relative to this context.
     * @return the name of the target
     * @throws InvalidNameException if relativeName is not a valid name in
     *      the LDAP namespace.
     */
    Dn buildTarget( Dn relativeName ) throws NamingException
    {
        Dn target = dn;

        // Add to left hand side of cloned Dn the relative name arg
        try
        {
            target = target.add( relativeName );
            target.apply( schemaManager );
        }
        catch ( LdapInvalidDnException lide )
        {
            throw new InvalidNameException( lide.getMessage() );
        }

        return target;
    }
}
