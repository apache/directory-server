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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.changelog.LogChange;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.context.AbstractOperationContext;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.entry.BinaryValue;
import org.apache.directory.shared.ldap.entry.StringValue;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.entry.ServerModification;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.FilterParser;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.message.control.Control;
import org.apache.directory.shared.ldap.message.internal.InternalAddRequest;
import org.apache.directory.shared.ldap.message.internal.InternalCompareRequest;
import org.apache.directory.shared.ldap.message.internal.InternalDeleteRequest;
import org.apache.directory.shared.ldap.message.internal.InternalModifyDnRequest;
import org.apache.directory.shared.ldap.message.internal.InternalModifyRequest;
import org.apache.directory.shared.ldap.message.internal.InternalSearchRequest;
import org.apache.directory.shared.ldap.message.internal.InternalUnbindRequest;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.name.RDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.AttributeTypeOptions;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * The default CoreSession implementation.
 * 
 * TODO - has not been completed yet
 * TODO - need to supply controls and other parameters to setup opContexts
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DefaultCoreSession implements CoreSession
{
    private final DirectoryService directoryService;
    private final LdapPrincipal authenticatedPrincipal;
    private LdapPrincipal authorizedPrincipal;
    
    
    public DefaultCoreSession( LdapPrincipal principal, DirectoryService directoryService )
    {
        this.directoryService = directoryService;
        this.authenticatedPrincipal = principal;
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
    public void add( ServerEntry entry ) throws Exception
    {
        add( entry, LogChange.TRUE );
    }


    /**
     * {@inheritDoc} 
     */
    public void add( ServerEntry entry, boolean ignoreReferral ) throws Exception
    {
        add( entry, ignoreReferral, LogChange.TRUE );
    }


    /**
     * {@inheritDoc} 
     */
    public void add( ServerEntry entry, LogChange log ) throws Exception
    {
        AddOperationContext opContext = new AddOperationContext( this, entry );

        opContext.setLogChange( log );
        
        OperationManager operationManager = directoryService.getOperationManager();
        operationManager.add( opContext );
    }


    /**
     * {@inheritDoc} 
     */
    public void add( ServerEntry entry, boolean ignoreReferral, LogChange log ) throws Exception
    {
        AddOperationContext opContext = new AddOperationContext( this, entry );

        opContext.setLogChange( log );
        setReferralHandling( opContext, ignoreReferral );
        
        OperationManager operationManager = directoryService.getOperationManager();
        operationManager.add( opContext );
    }


    /**
     * {@inheritDoc} 
     */
    public void add( InternalAddRequest addRequest ) throws Exception
    {
        add( addRequest, LogChange.TRUE );
    }

    
    /**
     * {@inheritDoc} 
     */
    public void add( InternalAddRequest addRequest, LogChange log ) throws Exception
    {
        AddOperationContext opContext = new AddOperationContext( this, addRequest );

        opContext.setLogChange( log );
        
        OperationManager operationManager = directoryService.getOperationManager();
        operationManager.add( opContext );
        addRequest.getResultResponse().addAll( opContext.getResponseControls() );
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
                val = new StringValue( attributeType, (String)value );
            }
            else if ( value instanceof byte[] )
            {
                val = new StringValue( attributeType, StringTools.utf8ToString( (byte[])value ) );
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
                val = new BinaryValue( attributeType, StringTools.getBytesUtf8( (String)value ) );
            }
            else if ( value instanceof byte[] )
            {
                val = new BinaryValue( attributeType, (byte[])value );
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
    public boolean compare( DN dn, String oid, Object value ) throws Exception
    {
        OperationManager operationManager = directoryService.getOperationManager();
        
        return operationManager.compare( 
            new CompareOperationContext( this, dn, oid, 
                convertToValue( oid, value ) ) );
    }


    /**
     * {@inheritDoc}
     */
    public boolean compare( DN dn, String oid, Object value, boolean ignoreReferral ) throws Exception
    {
        CompareOperationContext opContext =  
                new CompareOperationContext( this, dn, oid, 
                    convertToValue( oid, value ) );
        
        setReferralHandling( opContext, ignoreReferral );
        
        OperationManager operationManager = directoryService.getOperationManager();
        return operationManager.compare( opContext );
    }


    /**
     * {@inheritDoc}
     */
    public void delete( DN dn ) throws Exception
    {
        delete( dn, LogChange.TRUE );
    }


    /**
     * {@inheritDoc}
     */
    public void delete( DN dn, LogChange log ) throws Exception
    {
        DeleteOperationContext opContext = new DeleteOperationContext( this, dn );

        opContext.setLogChange( log );

        OperationManager operationManager = directoryService.getOperationManager();
        operationManager.delete( opContext );
    }


    /**
     * {@inheritDoc}
     */
    public void delete( DN dn, boolean ignoreReferral  ) throws Exception
    {
        delete( dn, ignoreReferral, LogChange.TRUE );
    }


    /**
     * {@inheritDoc}
     */
    public void delete( DN dn, boolean ignoreReferral, LogChange log ) throws Exception
    {
        DeleteOperationContext opContext = new DeleteOperationContext( this, dn );
        
        opContext.setLogChange( log );
        setReferralHandling( opContext, ignoreReferral );

        OperationManager operationManager = directoryService.getOperationManager();
        operationManager.delete( opContext );
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
     * @see org.apache.directory.server.core.CoreSession#isAdministrator()
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
     * @see org.apache.directory.server.core.CoreSession#isAnAdministrator()
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
     * @see org.apache.directory.server.core.CoreSession#list(org.apache.directory.shared.ldap.name.DN, org.apache.directory.shared.ldap.message.AliasDerefMode, java.util.Set)
     */
    public EntryFilteringCursor list( DN dn, AliasDerefMode aliasDerefMode,
        Set<AttributeTypeOptions> returningAttributes ) throws Exception
    {
        OperationManager operationManager = directoryService.getOperationManager();

        ListOperationContext listOperationContext = new ListOperationContext( this, dn, returningAttributes );
        listOperationContext.setAliasDerefMode( aliasDerefMode );
        
        return operationManager.list( listOperationContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.CoreSession#list(org.apache.directory.shared.ldap.name.DN, org.apache.directory.shared.ldap.message.AliasDerefMode, java.util.Set, int, int)
     */
    public EntryFilteringCursor list( DN dn, AliasDerefMode aliasDerefMode,
        Set<AttributeTypeOptions> returningAttributes, long sizeLimit, int timeLimit ) throws Exception
    {
        OperationManager operationManager = directoryService.getOperationManager();

        ListOperationContext listOperationContext = new ListOperationContext( this, dn, returningAttributes );
        listOperationContext.setSizeLimit( sizeLimit );
        listOperationContext.setTimeLimit( timeLimit );
        listOperationContext.setAliasDerefMode( aliasDerefMode );
        
        return operationManager.list( listOperationContext );
    }


    /**
     * {@inheritDoc} 
     */
    public ClonedServerEntry lookup( DN dn ) throws Exception
    {
        OperationManager operationManager = directoryService.getOperationManager();
        return operationManager.lookup( new LookupOperationContext( this, dn ) );
    }


    /**
     * {@inheritDoc}
     */
    public ClonedServerEntry lookup( DN dn, String[] attrId ) throws Exception
    {
        OperationManager operationManager = directoryService.getOperationManager();
        return operationManager.lookup( 
            new LookupOperationContext( this, dn, attrId ) );
    }


    /**
     * {@inheritDoc}
     */
    public void modify( DN dn, List<Modification> mods ) throws Exception
    {
        modify( dn, mods, LogChange.TRUE );
    }


    /**
     * {@inheritDoc}
     */
    public void modify( DN dn, List<Modification> mods, LogChange log ) throws Exception
    {
        if ( mods == null )
        {
            return;
        }
        
        List<Modification> serverModifications = new ArrayList<Modification>( mods.size() );
        
        for ( Modification mod:mods )
        {
            serverModifications.add( new ServerModification( directoryService.getSchemaManager(), mod ) );
        }
        
        ModifyOperationContext opContext = new ModifyOperationContext( this, dn, serverModifications );

        opContext.setLogChange( log );

        OperationManager operationManager = directoryService.getOperationManager();
        operationManager.modify( opContext );
    }


    /**
     * {@inheritDoc}
     */
    public void modify( DN dn, List<Modification> mods, boolean ignoreReferral ) throws Exception
    {
        modify( dn, mods, ignoreReferral, LogChange.TRUE );
    }


    /**
     * {@inheritDoc}
     */
    public void modify( DN dn, List<Modification> mods, boolean ignoreReferral, LogChange log ) throws Exception
    {
        if ( mods == null )
        {
            return;
        }
        
        List<Modification> serverModifications = new ArrayList<Modification>( mods.size() );
        
        for ( Modification mod:mods )
        {
            serverModifications.add( new ServerModification( directoryService.getSchemaManager(), mod ) );
        }

        ModifyOperationContext opContext = new ModifyOperationContext( this, dn, serverModifications );
        
        setReferralHandling( opContext, ignoreReferral );
        opContext.setLogChange( log );

        OperationManager operationManager = directoryService.getOperationManager();
        operationManager.modify( opContext );
    }


    /**
     * {@inheritDoc} 
     */
    public void move( DN dn, DN newParent ) throws Exception
    {
        move( dn, newParent, LogChange.TRUE );
    }


    /**
     * {@inheritDoc} 
     */
    public void move( DN dn, DN newParent, LogChange log ) throws Exception
    {
        MoveOperationContext opContext = new MoveOperationContext( this, dn, newParent );
        
        opContext.setLogChange( log );

        OperationManager operationManager = directoryService.getOperationManager();
        operationManager.move( opContext );
    }


    /**
     * {@inheritDoc} 
     */
    public void move( DN dn, DN newParent, boolean ignoreReferral ) throws Exception
    {
        move( dn, newParent, ignoreReferral, LogChange.TRUE );
    }


    /**
     * {@inheritDoc} 
     */
    public void move( DN dn, DN newParent, boolean ignoreReferral, LogChange log ) throws Exception
    {
        OperationManager operationManager = directoryService.getOperationManager();
        MoveOperationContext opContext = new MoveOperationContext( this, dn, newParent );
        
        setReferralHandling( opContext, ignoreReferral );
        opContext.setLogChange( log );

        operationManager.move( opContext );
    }


    /**
     * {@inheritDoc} 
     */
    public void moveAndRename( DN dn, DN newParent, RDN newRdn, boolean deleteOldRdn ) throws Exception
    {
        moveAndRename( dn, newParent, newRdn, deleteOldRdn, LogChange.TRUE );
    }


    /**
     * {@inheritDoc} 
     */
    public void moveAndRename( DN dn, DN newParent, RDN newRdn, boolean deleteOldRdn, LogChange log ) throws Exception
    {
        MoveAndRenameOperationContext opContext = 
            new MoveAndRenameOperationContext( this, dn, newParent, newRdn, deleteOldRdn );
        
        opContext.setLogChange( log );

        OperationManager operationManager = directoryService.getOperationManager();
        operationManager.moveAndRename( opContext );
    }


    /**
     * {@inheritDoc} 
     */
    public void moveAndRename( DN dn, DN newParent, RDN newRdn, boolean deleteOldRdn, boolean ignoreReferral ) throws Exception
    {
        moveAndRename( dn, newParent, newRdn, deleteOldRdn, ignoreReferral, LogChange.TRUE );
    }


    /**
     * {@inheritDoc} 
     */
    public void moveAndRename( DN dn, DN newParent, RDN newRdn, boolean deleteOldRdn, boolean ignoreReferral, LogChange log ) throws Exception
    {
        OperationManager operationManager = directoryService.getOperationManager();
        MoveAndRenameOperationContext opContext = new MoveAndRenameOperationContext( this, dn, newParent, newRdn, deleteOldRdn );
        
        opContext.setLogChange( log );
        setReferralHandling( opContext, ignoreReferral );

        operationManager.moveAndRename( opContext );
    }


    /**
     * {@inheritDoc}
     */
    public void rename( DN dn, RDN newRdn, boolean deleteOldRdn ) throws Exception
    {
        rename( dn, newRdn, deleteOldRdn, LogChange.TRUE );
    }


    /**
     * {@inheritDoc}
     */
    public void rename( DN dn, RDN newRdn, boolean deleteOldRdn, LogChange log ) throws Exception
    {
        RenameOperationContext opContext = new RenameOperationContext( this, dn, newRdn, deleteOldRdn );
        
        opContext.setLogChange( log );

        OperationManager operationManager = directoryService.getOperationManager();
        
        operationManager.rename( opContext );
    }


    /**
     * {@inheritDoc}
     */
    public void rename( DN dn, RDN newRdn, boolean deleteOldRdn, boolean ignoreReferral ) throws Exception
    {
        rename( dn, newRdn, deleteOldRdn, ignoreReferral, LogChange.TRUE );
    }


    /**
     * {@inheritDoc}
     */
    public void rename( DN dn, RDN newRdn, boolean deleteOldRdn, boolean ignoreReferral, LogChange log ) throws Exception
    {
        OperationManager operationManager = directoryService.getOperationManager();
        RenameOperationContext opContext = new RenameOperationContext( this, dn, newRdn, deleteOldRdn );
        
        opContext.setLogChange( log );
        setReferralHandling( opContext, ignoreReferral );

        operationManager.rename( opContext );
    }


    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor search( DN dn, String filter ) throws Exception
    {
        return search( dn, filter, true );
    }


    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor search( DN dn, String filter, boolean ignoreReferrals ) throws Exception
    {
        OperationManager operationManager = directoryService.getOperationManager();
        ExprNode filterNode = FilterParser.parse( filter ); 
        
        SearchOperationContext searchOperationContext = new SearchOperationContext( this, dn, SearchScope.OBJECT, 
            filterNode, null );
        searchOperationContext.setAliasDerefMode( AliasDerefMode.DEREF_ALWAYS ); 
        setReferralHandling( searchOperationContext, ignoreReferrals );

        return operationManager.search( searchOperationContext );
    }
    

    /* (non-Javadoc)
     * @see org.apache.directory.server.core.CoreSession#search(org.apache.directory.shared.ldap.name.DN, org.apache.directory.shared.ldap.filter.SearchScope, org.apache.directory.shared.ldap.filter.ExprNode, org.apache.directory.shared.ldap.message.AliasDerefMode, java.util.Set)
     */
    public EntryFilteringCursor search( DN dn, SearchScope scope, ExprNode filter, AliasDerefMode aliasDerefMode,
        Set<AttributeTypeOptions> returningAttributes ) throws Exception
    {
        OperationManager operationManager = directoryService.getOperationManager();

        SearchOperationContext searchOperationContext = new SearchOperationContext( this, dn, scope, filter, 
            returningAttributes );
        searchOperationContext.setAliasDerefMode( aliasDerefMode );

        return operationManager.search( searchOperationContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.CoreSession#search(org.apache.directory.shared.ldap.name.DN, org.apache.directory.shared.ldap.filter.SearchScope, org.apache.directory.shared.ldap.filter.ExprNode, org.apache.directory.shared.ldap.message.AliasDerefMode, java.util.Set, int, int)
     */
    public EntryFilteringCursor search( DN dn, SearchScope scope, ExprNode filter, AliasDerefMode aliasDerefMode,
        Set<AttributeTypeOptions> returningAttributes, long sizeLimit, int timeLimit ) throws Exception
    {
        OperationManager operationManager = directoryService.getOperationManager();

        SearchOperationContext searchOperationContext = new SearchOperationContext( this, dn, scope, filter, 
            returningAttributes );
        searchOperationContext.setAliasDerefMode( aliasDerefMode );
        searchOperationContext.setSizeLimit( sizeLimit );
        searchOperationContext.setTimeLimit( timeLimit );
        
        return operationManager.search( searchOperationContext );
    }


    public boolean isAnonymous()
    {
        return getEffectivePrincipal().getClonedName().isEmpty();
    }


    /**
     * {@inheritDoc}
     */
    public boolean compare( InternalCompareRequest compareRequest ) throws Exception
    {
        CompareOperationContext opContext = new CompareOperationContext( this, compareRequest );
        OperationManager operationManager = directoryService.getOperationManager();
        boolean result = operationManager.compare( opContext );
        compareRequest.getResultResponse().addAll( opContext.getResponseControls() );
        return result;
    }


    /**
     * {@inheritDoc}
     */
    public void delete( InternalDeleteRequest deleteRequest ) throws Exception
    {
        delete( deleteRequest, LogChange.TRUE );
    }


    /**
     * {@inheritDoc}
     */
    public void delete( InternalDeleteRequest deleteRequest, LogChange log ) throws Exception
    {
        DeleteOperationContext opContext = new DeleteOperationContext( this, deleteRequest );
        
        opContext.setLogChange( log );

        OperationManager operationManager = directoryService.getOperationManager();
        operationManager.delete( opContext );
        deleteRequest.getResultResponse().addAll( opContext.getResponseControls() );
    }


    public boolean exists( DN dn ) throws Exception
    {
        EntryOperationContext opContext = new EntryOperationContext( this, dn );
        OperationManager operationManager = directoryService.getOperationManager();
        return operationManager.hasEntry( opContext );
    }


    /**
     * {@inheritDoc}
     */
    public void modify( InternalModifyRequest modifyRequest ) throws Exception
    {
        modify( modifyRequest, LogChange.TRUE );
    }


    /**
     * {@inheritDoc}
     */
    public void modify( InternalModifyRequest modifyRequest, LogChange log ) throws Exception
    {
        ModifyOperationContext opContext = new ModifyOperationContext( this, modifyRequest );

        opContext.setLogChange( log );

        OperationManager operationManager = directoryService.getOperationManager();
        operationManager.modify( opContext );
        modifyRequest.getResultResponse().addAll( opContext.getResponseControls() );
    }


    /**
     * {@inheritDoc} 
     */
    public void move( InternalModifyDnRequest modifyDnRequest ) throws Exception
    {
        move( modifyDnRequest, LogChange.TRUE );
    }


    /**
     * {@inheritDoc} 
     */
    public void move( InternalModifyDnRequest modifyDnRequest, LogChange log ) throws Exception
    {
        MoveOperationContext opContext = new MoveOperationContext( this, modifyDnRequest );
        
        opContext.setLogChange( log );

        OperationManager operationManager = directoryService.getOperationManager();
        operationManager.move( opContext );
        modifyDnRequest.getResultResponse().addAll( opContext.getResponseControls() );
    }


    /**
     * {@inheritDoc} 
     */
    public void moveAndRename( InternalModifyDnRequest modifyDnRequest ) throws Exception
    {
        moveAndRename( modifyDnRequest, LogChange.TRUE );
    }


    /**
     * {@inheritDoc} 
     */
    public void moveAndRename( InternalModifyDnRequest modifyDnRequest, LogChange log ) throws Exception
    {
        MoveAndRenameOperationContext opContext = new MoveAndRenameOperationContext( this, modifyDnRequest );

        opContext.setLogChange( log );

        OperationManager operationManager = directoryService.getOperationManager();
        operationManager.moveAndRename( opContext );
        modifyDnRequest.getResultResponse().addAll( opContext.getResponseControls() );
    }


    /**
     * {@inheritDoc}
     */
    public void rename( InternalModifyDnRequest modifyDnRequest ) throws Exception
    {
        rename( modifyDnRequest, LogChange.TRUE );
    }


    /**
     * {@inheritDoc}
     */
    public void rename( InternalModifyDnRequest modifyDnRequest, LogChange log ) throws Exception
    {
        RenameOperationContext opContext = new RenameOperationContext( this, modifyDnRequest );

        opContext.setLogChange( log );

        OperationManager operationManager = directoryService.getOperationManager();
        operationManager.rename( opContext );
        modifyDnRequest.getResultResponse().addAll( opContext.getResponseControls() );
    }


    public EntryFilteringCursor search( InternalSearchRequest searchRequest ) throws Exception
    {
        SearchOperationContext opContext = new SearchOperationContext( this, searchRequest );
        OperationManager operationManager = directoryService.getOperationManager();
        EntryFilteringCursor cursor = operationManager.search( opContext );
        searchRequest.getResultResponse().addAll( opContext.getResponseControls() );
        
        return cursor;
    }


    public void unbind() throws Exception
    {
        OperationManager operationManager = directoryService.getOperationManager();
        operationManager.unbind( new UnbindOperationContext( this ) );
    }


    public void unbind( InternalUnbindRequest unbindRequest )
    {
        // TODO Auto-generated method stub
        
    }
}
