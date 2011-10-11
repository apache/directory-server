/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.core;


import java.net.SocketAddress;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.OperationManager;
import org.apache.directory.server.core.api.changelog.LogChange;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.context.AbstractOperationContext;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.OperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.model.entry.BinaryValue;
import org.apache.directory.shared.ldap.model.entry.DefaultModification;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.StringValue;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidSearchFilterException;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.filter.FilterParser;
import org.apache.directory.shared.ldap.model.message.AddRequest;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.message.CompareRequest;
import org.apache.directory.shared.ldap.model.message.Control;
import org.apache.directory.shared.ldap.model.message.DeleteRequest;
import org.apache.directory.shared.ldap.model.message.ModifyDnRequest;
import org.apache.directory.shared.ldap.model.message.ModifyRequest;
import org.apache.directory.shared.ldap.model.message.SearchRequest;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.message.UnbindRequest;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.name.Rdn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.AttributeTypeOptions;
import org.apache.directory.shared.util.Strings;


/**
 * The default CoreSession implementation.
 * 
 * TODO - has not been completed yet
 * TODO - need to supply controls and other parameters to setup opContexts
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DefaultCoreSession implements CoreSession
{
    private final DirectoryService directoryService;
    private final LdapPrincipal authenticatedPrincipal;
    private final LdapPrincipal anonymousPrincipal;
    private LdapPrincipal authorizedPrincipal;


    public DefaultCoreSession( LdapPrincipal principal, DirectoryService directoryService )
    {
        this.directoryService = directoryService;
        this.authenticatedPrincipal = principal;
        this.anonymousPrincipal = new LdapPrincipal( directoryService.getSchemaManager() );
    }


    /**
     * Set the ignoreRefferal flag for the current operationContext.
     *
     * @param opContext The current operationContext
     * @param ignoreReferral The flag 
     */
    private void setReferralHandling( AbstractOperationContext opContext, boolean ignoreReferral )
    {
        if ( ignoreReferral )
        {
            opContext.ignoreReferral();
        }
        else
        {
            opContext.throwReferral();
        }
    }


    /**
     * {@inheritDoc} 
     */
    public void add( Entry entry ) throws LdapException
    {
        add( entry, LogChange.TRUE );
    }


    /**
     * {@inheritDoc} 
     */
    public void add( Entry entry, boolean ignoreReferral ) throws LdapException
    {
        add( entry, ignoreReferral, LogChange.TRUE );
    }


    /**
     * {@inheritDoc} 
     */
    public void add( Entry entry, LogChange log ) throws LdapException
    {
        AddOperationContext addContext = new AddOperationContext( this, entry );

        addContext.setLogChange( log );

        OperationManager operationManager = directoryService.getOperationManager();
        operationManager.add( addContext );
    }


    /**
     * {@inheritDoc} 
     */
    public void add( Entry entry, boolean ignoreReferral, LogChange log ) throws LdapException
    {
        AddOperationContext addContext = new AddOperationContext( this, entry );

        addContext.setLogChange( log );
        setReferralHandling( addContext, ignoreReferral );

        OperationManager operationManager = directoryService.getOperationManager();
        operationManager.add( addContext );
    }


    /**
     * {@inheritDoc} 
     */
    public void add( AddRequest addRequest ) throws LdapException
    {
        add( addRequest, LogChange.TRUE );
    }


    /**
     * {@inheritDoc} 
     */
    public void add( AddRequest addRequest, LogChange log ) throws LdapException
    {
        AddOperationContext addContext = new AddOperationContext( this, addRequest );

        addContext.setLogChange( log );

        OperationManager operationManager = directoryService.getOperationManager();
        try
        {
            operationManager.add( addContext );
        }
        catch ( LdapException e )
        {
            addRequest.getResultResponse().addAllControls( addContext.getResponseControls() );
            throw e;
        }
        addRequest.getResultResponse().addAllControls( addContext.getResponseControls() );
    }


    private Value<?> convertToValue( String oid, Object value ) throws LdapException
    {
        Value<?> val = null;

        AttributeType attributeType = directoryService.getSchemaManager().lookupAttributeTypeRegistry( oid );

        // make sure we add the request controls to operation
        if ( attributeType.getSyntax().isHumanReadable() )
        {
            if ( value instanceof String )
            {
                val = new StringValue( attributeType, ( String ) value );
            }
            else if ( value instanceof byte[] )
            {
                val = new StringValue( attributeType, Strings.utf8ToString((byte[]) value) );
            }
            else
            {
                throw new LdapException( I18n.err( I18n.ERR_309, oid ) );
            }
        }
        else
        {
            if ( value instanceof String )
            {
                val = new BinaryValue( attributeType, Strings.getBytesUtf8((String) value) );
            }
            else if ( value instanceof byte[] )
            {
                val = new BinaryValue( attributeType, ( byte[] ) value );
            }
            else
            {
                throw new LdapException( I18n.err( I18n.ERR_309, oid ) );
            }
        }

        return val;
    }


    /**
     * {@inheritDoc}
     */
    public boolean compare( Dn dn, String oid, Object value ) throws LdapException
    {
        OperationManager operationManager = directoryService.getOperationManager();

        return operationManager.compare( new CompareOperationContext( this, dn, oid, convertToValue( oid, value ) ) );
    }


    /**
     * {@inheritDoc}
     */
    public boolean compare( Dn dn, String oid, Object value, boolean ignoreReferral ) throws LdapException
    {
        CompareOperationContext compareContext = new CompareOperationContext( this, dn, oid,
            convertToValue( oid, value ) );

        setReferralHandling( compareContext, ignoreReferral );

        OperationManager operationManager = directoryService.getOperationManager();
        return operationManager.compare( compareContext );
    }


    /**
     * {@inheritDoc}
     */
    public void delete( Dn dn ) throws LdapException
    {
        delete( dn, LogChange.TRUE );
    }


    /**
     * {@inheritDoc}
     */
    public void delete( Dn dn, LogChange log ) throws LdapException
    {
        DeleteOperationContext deleteContext = new DeleteOperationContext( this, dn );

        deleteContext.setLogChange( log );

        OperationManager operationManager = directoryService.getOperationManager();
        operationManager.delete( deleteContext );
    }


    /**
     * {@inheritDoc}
     */
    public void delete( Dn dn, boolean ignoreReferral ) throws LdapException
    {
        delete( dn, ignoreReferral, LogChange.TRUE );
    }


    /**
     * {@inheritDoc}
     */
    public void delete( Dn dn, boolean ignoreReferral, LogChange log ) throws LdapException
    {
        DeleteOperationContext deleteContext = new DeleteOperationContext( this, dn );

        deleteContext.setLogChange( log );
        setReferralHandling( deleteContext, ignoreReferral );

        OperationManager operationManager = directoryService.getOperationManager();
        operationManager.delete( deleteContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.CoreSession#getAuthenticatedPrincipal()
     */
    public LdapPrincipal getAnonymousPrincipal()
    {
        return anonymousPrincipal;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.CoreSession#getAuthenticatedPrincipal()
     */
    public LdapPrincipal getAuthenticatedPrincipal()
    {
        return authenticatedPrincipal;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.CoreSession#getAuthenticationLevel()
     */
    public AuthenticationLevel getAuthenticationLevel()
    {
        return getEffectivePrincipal().getAuthenticationLevel();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.CoreSession#getClientAddress()
     */
    public SocketAddress getClientAddress()
    {
        // TODO Auto-generated method stub
        return null;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.CoreSession#getControls()
     */
    public Set<Control> getControls()
    {
        // TODO Auto-generated method stub
        return null;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.CoreSession#getDirectoryService()
     */
    public DirectoryService getDirectoryService()
    {
        return directoryService;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.CoreSession#getEffectivePrincipal()
     */
    public LdapPrincipal getEffectivePrincipal()
    {
        if ( authorizedPrincipal == null )
        {
            return authenticatedPrincipal;
        }

        return authorizedPrincipal;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.CoreSession#getOutstandingOperations()
     */
    public Set<OperationContext> getOutstandingOperations()
    {
        // TODO Auto-generated method stub
        return null;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.CoreSession#getServiceAddress()
     */
    public SocketAddress getServiceAddress()
    {
        // TODO Auto-generated method stub
        return null;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.CoreSession#isConfidential()
     */
    public boolean isConfidential()
    {
        // TODO Auto-generated method stub
        return false;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.CoreSession#isVirtual()
     */
    public boolean isVirtual()
    {
        // TODO Auto-generated method stub
        return true;
    }


    /**
     * TODO - perhaps we should just use a flag that is calculated on creation
     * of this session
     *  
     * @see org.apache.directory.server.core.api.CoreSession#isAdministrator()
     */
    public boolean isAdministrator()
    {
        String normName = getEffectivePrincipal().getName();
        
        return normName.equals( ServerDNConstants.ADMIN_SYSTEM_DN_NORMALIZED );
    }


    /**
     * TODO - this method impl does not check to see if the principal is in 
     * the administrators group - it only returns true of the principal is
     * the actual admin user.  need to make it check groups.
     * 
     * TODO - perhaps we should just use a flag that is calculated on creation
     * of this session
     *  
     * @see org.apache.directory.server.core.api.CoreSession#isAnAdministrator()
     */
    public boolean isAnAdministrator()
    {
        if ( isAdministrator() )
        {
            return true;
        }

        // TODO fix this so it checks groups
        return false;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.CoreSession#list(org.apache.directory.shared.ldap.model.name.Dn, org.apache.directory.shared.ldap.model.message.AliasDerefMode, java.util.Set)
     */
    public EntryFilteringCursor list( Dn dn, AliasDerefMode aliasDerefMode,
        Set<AttributeTypeOptions> returningAttributes ) throws LdapException
    {
        OperationManager operationManager = directoryService.getOperationManager();

        ListOperationContext listContext = new ListOperationContext( this, dn, returningAttributes );
        listContext.setAliasDerefMode( aliasDerefMode );

        return operationManager.list( listContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.CoreSession#list(org.apache.directory.shared.ldap.model.name.Dn, org.apache.directory.shared.ldap.message.AliasDerefMode, java.util.Set, int, int)
     */
    public EntryFilteringCursor list( Dn dn, AliasDerefMode aliasDerefMode,
        Set<AttributeTypeOptions> returningAttributes, long sizeLimit, int timeLimit ) throws LdapException
    {
        OperationManager operationManager = directoryService.getOperationManager();

        ListOperationContext listContext = new ListOperationContext( this, dn, returningAttributes );
        listContext.setSizeLimit( sizeLimit );
        listContext.setTimeLimit( timeLimit );
        listContext.setAliasDerefMode( aliasDerefMode );

        return operationManager.list( listContext );
    }


    /**
     * {@inheritDoc} 
     */
    public Entry lookup( Dn dn ) throws LdapException
    {
        OperationManager operationManager = directoryService.getOperationManager();
        return operationManager.lookup( new LookupOperationContext( this, dn ) );
    }


    /**
     * {@inheritDoc}
     */
    public Entry lookup( Dn dn, String... attrIds ) throws LdapException
    {
        OperationManager operationManager = directoryService.getOperationManager();
        LookupOperationContext lookupContext = new LookupOperationContext( this, dn, attrIds );

        Entry entry = operationManager.lookup( lookupContext );

        return entry;
    }


    /**
     * {@inheritDoc}
     */
    public Entry lookup( Dn dn, Control[] controls, String... attrIds ) throws LdapException
    {
        OperationManager operationManager = directoryService.getOperationManager();
        LookupOperationContext lookupContext = new LookupOperationContext( this, dn, attrIds );
        
        if ( controls != null )
        {
            lookupContext.addRequestControls( controls );
        }

        Entry entry = operationManager.lookup( lookupContext );

        return entry;
    }


    /**
     * {@inheritDoc}
     */
    public void modify( Dn dn, Modification... mods ) throws LdapException
    {
        modify( dn, Arrays.asList( mods ), LogChange.TRUE );
    }


    /**
     * {@inheritDoc}
     */
    public void modify( Dn dn, List<Modification> mods ) throws LdapException
    {
        modify( dn, mods, LogChange.TRUE );
    }


    /**
     * {@inheritDoc}
     */
    public void modify( Dn dn, List<Modification> mods, LogChange log ) throws LdapException
    {
        if ( mods == null )
        {
            return;
        }

        List<Modification> serverModifications = new ArrayList<Modification>( mods.size() );

        for ( Modification mod : mods )
        {
            serverModifications.add( new DefaultModification( directoryService.getSchemaManager(), mod ) );
        }

        ModifyOperationContext modifyContext = new ModifyOperationContext( this, dn, serverModifications );

        modifyContext.setLogChange( log );

        OperationManager operationManager = directoryService.getOperationManager();
        operationManager.modify( modifyContext );
    }


    /**
     * {@inheritDoc}
     */
    public void modify( Dn dn, List<Modification> mods, boolean ignoreReferral ) throws LdapException
    {
        modify( dn, mods, ignoreReferral, LogChange.TRUE );
    }


    /**
     * {@inheritDoc}
     */
    public void modify( Dn dn, List<Modification> mods, boolean ignoreReferral, LogChange log ) throws LdapException
    {
        if ( mods == null )
        {
            return;
        }

        List<Modification> serverModifications = new ArrayList<Modification>( mods.size() );

        for ( Modification mod : mods )
        {
            serverModifications.add( new DefaultModification( directoryService.getSchemaManager(), mod ) );
        }

        ModifyOperationContext modifyContext = new ModifyOperationContext( this, dn, serverModifications );

        setReferralHandling( modifyContext, ignoreReferral );
        modifyContext.setLogChange( log );

        OperationManager operationManager = directoryService.getOperationManager();
        operationManager.modify( modifyContext );
    }


    /**
     * {@inheritDoc} 
     */
    public void move( Dn dn, Dn newParent ) throws LdapException
    {
        move( dn, newParent, LogChange.TRUE );
    }


    /**
     * {@inheritDoc} 
     */
    public void move( Dn dn, Dn newParent, LogChange log ) throws LdapException
    {
        MoveOperationContext moveContext = new MoveOperationContext( this, dn, newParent );
        moveContext.setLogChange( log );

        OperationManager operationManager = directoryService.getOperationManager();
        operationManager.move( moveContext );
    }


    /**
     * {@inheritDoc} 
     */
    public void move( Dn dn, Dn newParent, boolean ignoreReferral ) throws Exception
    {
        move( dn, newParent, ignoreReferral, LogChange.TRUE );
    }


    /**
     * {@inheritDoc} 
     */
    public void move( Dn dn, Dn newParent, boolean ignoreReferral, LogChange log ) throws LdapException
    {
        OperationManager operationManager = directoryService.getOperationManager();
        MoveOperationContext moveContext = new MoveOperationContext( this, dn, newParent );

        setReferralHandling( moveContext, ignoreReferral );
        moveContext.setLogChange( log );

        operationManager.move( moveContext );
    }


    /**
     * {@inheritDoc} 
     */
    public void moveAndRename( Dn dn, Dn newParent, Rdn newRdn, boolean deleteOldRdn ) throws LdapException
    {
        moveAndRename( dn, newParent, newRdn, deleteOldRdn, LogChange.TRUE );
    }


    /**
     * {@inheritDoc} 
     */
    public void moveAndRename( Dn dn, Dn newSuperiorDn, Rdn newRdn, boolean deleteOldRdn, LogChange log )
        throws LdapException
    {
        MoveAndRenameOperationContext moveAndRenameContext = new MoveAndRenameOperationContext( this, dn,
            newSuperiorDn, newRdn, deleteOldRdn );

        moveAndRenameContext.setLogChange( log );

        OperationManager operationManager = directoryService.getOperationManager();
        operationManager.moveAndRename( moveAndRenameContext );
    }


    /**
     * {@inheritDoc} 
     */
    public void moveAndRename( Dn dn, Dn newParent, Rdn newRdn, boolean deleteOldRdn, boolean ignoreReferral )
        throws LdapException
    {
        moveAndRename( dn, newParent, newRdn, deleteOldRdn, ignoreReferral, LogChange.TRUE );
    }


    /**
     * {@inheritDoc} 
     */
    public void moveAndRename( Dn dn, Dn newParent, Rdn newRdn, boolean deleteOldRdn, boolean ignoreReferral,
        LogChange log ) throws LdapException
    {
        OperationManager operationManager = directoryService.getOperationManager();
        MoveAndRenameOperationContext moveAndRenameContext = new MoveAndRenameOperationContext( this, dn, newParent,
            newRdn, deleteOldRdn );

        moveAndRenameContext.setLogChange( log );
        setReferralHandling( moveAndRenameContext, ignoreReferral );

        operationManager.moveAndRename( moveAndRenameContext );
    }


    /**
     * {@inheritDoc}
     */
    public void rename( Dn dn, Rdn newRdn, boolean deleteOldRdn ) throws LdapException
    {
        rename( dn, newRdn, deleteOldRdn, LogChange.TRUE );
    }


    /**
     * {@inheritDoc}
     */
    public void rename( Dn dn, Rdn newRdn, boolean deleteOldRdn, LogChange log ) throws LdapException
    {
        RenameOperationContext renameContext = new RenameOperationContext( this, dn, newRdn, deleteOldRdn );

        renameContext.setLogChange( log );

        OperationManager operationManager = directoryService.getOperationManager();

        operationManager.rename( renameContext );
    }


    /**
     * {@inheritDoc}
     */
    public void rename( Dn dn, Rdn newRdn, boolean deleteOldRdn, boolean ignoreReferral ) throws LdapException
    {
        rename( dn, newRdn, deleteOldRdn, ignoreReferral, LogChange.TRUE );
    }


    /**
     * {@inheritDoc}
     */
    public void rename( Dn dn, Rdn newRdn, boolean deleteOldRdn, boolean ignoreReferral, LogChange log )
        throws LdapException
    {
        OperationManager operationManager = directoryService.getOperationManager();
        RenameOperationContext renameContext = new RenameOperationContext( this, dn, newRdn, deleteOldRdn );

        renameContext.setLogChange( log );
        setReferralHandling( renameContext, ignoreReferral );

        operationManager.rename( renameContext );
    }


    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor search( Dn dn, String filter ) throws LdapException
    {
        return search( dn, filter, true );
    }


    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor search( Dn dn, String filter, boolean ignoreReferrals ) throws LdapException
    {
        OperationManager operationManager = directoryService.getOperationManager();
        ExprNode filterNode = null;

        try
        {
            filterNode = ( ExprNode ) FilterParser.parse( directoryService.getSchemaManager(), filter );
        }
        catch ( ParseException pe )
        {
            throw new LdapInvalidSearchFilterException( pe.getMessage() );
        }

        SearchOperationContext searchContext = new SearchOperationContext( this, dn, SearchScope.OBJECT, filterNode,
            null );
        searchContext.setAliasDerefMode( AliasDerefMode.DEREF_ALWAYS );
        setReferralHandling( searchContext, ignoreReferrals );

        return operationManager.search( searchContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.CoreSession#search(org.apache.directory.shared.ldap.model.name.Dn, org.apache.directory.shared.ldap.model.filter.SearchScope, org.apache.directory.shared.ldap.model.filter.ExprNode, org.apache.directory.shared.ldap.message.AliasDerefMode, java.util.Set)
     */
    public EntryFilteringCursor search( Dn dn, SearchScope scope, ExprNode filter, AliasDerefMode aliasDerefMode,
        Set<AttributeTypeOptions> returningAttributes ) throws LdapException
    {
        OperationManager operationManager = directoryService.getOperationManager();

        SearchOperationContext searchContext = new SearchOperationContext( this, dn, scope, filter, returningAttributes );
        searchContext.setAliasDerefMode( aliasDerefMode );

        return operationManager.search( searchContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.CoreSession#search(org.apache.directory.shared.ldap.model.name.Dn, org.apache.directory.shared.ldap.model.filter.SearchScope, org.apache.directory.shared.ldap.model.filter.ExprNode, org.apache.directory.shared.ldap.model.message.AliasDerefMode, java.util.Set, int, int)
     */
    public EntryFilteringCursor search( Dn dn, SearchScope scope, ExprNode filter, AliasDerefMode aliasDerefMode,
        Set<AttributeTypeOptions> returningAttributes, long sizeLimit, int timeLimit ) throws LdapException
    {
        OperationManager operationManager = directoryService.getOperationManager();

        SearchOperationContext searchContext = new SearchOperationContext( this, dn, scope, filter, returningAttributes );
        searchContext.setAliasDerefMode( aliasDerefMode );
        searchContext.setSizeLimit( sizeLimit );
        searchContext.setTimeLimit( timeLimit );

        return operationManager.search( searchContext );
    }


    public boolean isAnonymous()
    {
        return getEffectivePrincipal().getDn().isEmpty();
    }


    /**
     * {@inheritDoc}
     */
    public boolean compare( CompareRequest compareRequest ) throws LdapException
    {
        CompareOperationContext compareContext = new CompareOperationContext( this, compareRequest );
        OperationManager operationManager = directoryService.getOperationManager();
        boolean result = false;
        try
        {
            result = operationManager.compare( compareContext );
        }
        catch ( LdapException e )
        {
            compareRequest.getResultResponse().addAllControls( compareContext.getResponseControls() );
            throw e;
        }

        compareRequest.getResultResponse().addAllControls( compareContext.getResponseControls() );
        return result;
    }


    /**
     * {@inheritDoc}
     */
    public void delete( DeleteRequest deleteRequest ) throws LdapException
    {
        delete( deleteRequest, LogChange.TRUE );
    }


    /**
     * {@inheritDoc}
     */
    public void delete( DeleteRequest deleteRequest, LogChange log ) throws LdapException
    {
        DeleteOperationContext deleteContext = new DeleteOperationContext( this, deleteRequest );

        deleteContext.setLogChange( log );

        OperationManager operationManager = directoryService.getOperationManager();
        try
        {
            operationManager.delete( deleteContext );
        }
        catch ( LdapException e )
        {
            deleteRequest.getResultResponse().addAllControls( deleteContext.getResponseControls() );
            throw e;
        }

        deleteRequest.getResultResponse().addAllControls( deleteContext.getResponseControls() );
    }
    
    
    /**
     * {@inheritDoc} 
     */
    public boolean exists( String dn ) throws LdapException
    {
        return exists( new Dn( dn ) );
    }


    /**
     * {@inheritDoc} 
     */
    public boolean exists( Dn dn ) throws LdapException
    {
        EntryOperationContext hasEntryContext = new EntryOperationContext( this, dn );
        OperationManager operationManager = directoryService.getOperationManager();
        return operationManager.hasEntry( hasEntryContext );
    }


    /**
     * {@inheritDoc}
     */
    public void modify( ModifyRequest modifyRequest ) throws LdapException
    {
        modify( modifyRequest, LogChange.TRUE );
    }


    /**
     * {@inheritDoc}
     */
    public void modify( ModifyRequest modifyRequest, LogChange log ) throws LdapException
    {
        ModifyOperationContext modifyContext = new ModifyOperationContext( this, modifyRequest );

        modifyContext.setLogChange( log );

        OperationManager operationManager = directoryService.getOperationManager();

        try
        {
            operationManager.modify( modifyContext );
        }
        catch ( LdapException e )
        {
            modifyRequest.getResultResponse().addAllControls( modifyContext.getResponseControls() );
            throw e;
        }

        modifyRequest.getResultResponse().addAllControls( modifyContext.getResponseControls() );
    }


    /**
     * {@inheritDoc} 
     */
    public void move( ModifyDnRequest modifyDnRequest ) throws LdapException
    {
        move( modifyDnRequest, LogChange.TRUE );
    }


    /**
     * {@inheritDoc} 
     */
    public void move( ModifyDnRequest modifyDnRequest, LogChange log ) throws LdapException
    {
        MoveOperationContext moveContext = new MoveOperationContext( this, modifyDnRequest );

        moveContext.setLogChange( log );

        OperationManager operationManager = directoryService.getOperationManager();

        try
        {
            operationManager.move( moveContext );
        }
        catch ( LdapException e )
        {
            modifyDnRequest.getResultResponse().addAllControls( moveContext.getResponseControls() );
            throw e;
        }

        modifyDnRequest.getResultResponse().addAllControls( moveContext.getResponseControls() );
    }


    /**
     * {@inheritDoc} 
     */
    public void moveAndRename( ModifyDnRequest modifyDnRequest ) throws LdapException
    {
        moveAndRename( modifyDnRequest, LogChange.TRUE );
    }


    /**
     * {@inheritDoc} 
     */
    public void moveAndRename( ModifyDnRequest modifyDnRequest, LogChange log ) throws LdapException
    {
        MoveAndRenameOperationContext moveAndRenameContext = new MoveAndRenameOperationContext( this, modifyDnRequest );

        moveAndRenameContext.setLogChange( log );

        OperationManager operationManager = directoryService.getOperationManager();

        try
        {
            operationManager.moveAndRename( moveAndRenameContext );
        }
        catch ( LdapException e )
        {
            modifyDnRequest.getResultResponse().addAllControls( moveAndRenameContext.getResponseControls() );
            throw e;
        }

        modifyDnRequest.getResultResponse().addAllControls( moveAndRenameContext.getResponseControls() );
    }


    /**
     * {@inheritDoc}
     */
    public void rename( ModifyDnRequest modifyDnRequest ) throws LdapException
    {
        rename( modifyDnRequest, LogChange.TRUE );
    }


    /**
     * {@inheritDoc}
     */
    public void rename( ModifyDnRequest modifyDnRequest, LogChange log ) throws LdapException
    {
        RenameOperationContext renameContext = new RenameOperationContext( this, modifyDnRequest );

        renameContext.setLogChange( log );

        OperationManager operationManager = directoryService.getOperationManager();

        try
        {
            operationManager.rename( renameContext );
        }
        catch ( LdapException e )
        {
            modifyDnRequest.getResultResponse().addAllControls( renameContext.getResponseControls() );
            throw e;
        }

        modifyDnRequest.getResultResponse().addAllControls( renameContext.getResponseControls() );
    }


    public EntryFilteringCursor search( SearchRequest searchRequest ) throws LdapException
    {
        SearchOperationContext searchContext = new SearchOperationContext( this, searchRequest );
        OperationManager operationManager = directoryService.getOperationManager();

        EntryFilteringCursor cursor = null;

        try
        {
            cursor = operationManager.search( searchContext );
        }
        catch ( LdapException e )
        {
            searchRequest.getResultResponse().addAllControls( searchContext.getResponseControls() );
            throw e;
        }

        searchRequest.getResultResponse().addAllControls( searchContext.getResponseControls() );

        return cursor;
    }


    public void unbind() throws LdapException
    {
        OperationManager operationManager = directoryService.getOperationManager();
        operationManager.unbind( new UnbindOperationContext( this ) );
    }


    public void unbind( UnbindRequest unbindRequest )
    {
        // TODO Auto-generated method stub
    }
}
