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

import javax.naming.ldap.Control;

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.authn.LdapPrincipal;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.entry.ServerModification;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
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
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.message.AddRequest;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.message.CompareRequest;
import org.apache.directory.shared.ldap.message.DeleteRequest;
import org.apache.directory.shared.ldap.message.ModifyDnRequest;
import org.apache.directory.shared.ldap.message.ModifyRequest;
import org.apache.directory.shared.ldap.message.SearchRequest;
import org.apache.directory.shared.ldap.message.UnbindRequest;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.AttributeTypeOptions;


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

    
    /* (non-Javadoc)
     * @see org.apache.directory.server.core.CoreSession#add(org.apache.directory.server.core.entry.ServerEntry)
     */
    public void add( ServerEntry entry ) throws Exception
    {
        directoryService.getOperationManager().add( new AddOperationContext( this, entry ) );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.CoreSession#compare(org.apache.directory.shared.ldap.name.LdapDN, java.lang.String, java.lang.Object)
     */
    public void compare( LdapDN dn, String oid, Object value ) throws Exception
    {
        directoryService.getOperationManager().compare( new CompareOperationContext( this, dn, oid, value ) );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.CoreSession#delete(org.apache.directory.shared.ldap.name.LdapDN)
     */
    public void delete( LdapDN dn ) throws Exception
    {
        directoryService.getOperationManager().delete( new DeleteOperationContext( this, dn ) );
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
        String normName = getEffectivePrincipal().getJndiName().toNormName(); 
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
     * @see org.apache.directory.server.core.CoreSession#list(org.apache.directory.shared.ldap.name.LdapDN, org.apache.directory.shared.ldap.message.AliasDerefMode, java.util.Set)
     */
    public EntryFilteringCursor list( LdapDN dn, AliasDerefMode aliasDerefMode,
        Set<AttributeTypeOptions> returningAttributes ) throws Exception
    {
        return directoryService.getOperationManager().list( 
            new ListOperationContext( this, dn, aliasDerefMode, returningAttributes ) );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.CoreSession#list(org.apache.directory.shared.ldap.name.LdapDN, org.apache.directory.shared.ldap.message.AliasDerefMode, java.util.Set, int, int)
     */
    public EntryFilteringCursor list( LdapDN dn, AliasDerefMode aliasDerefMode,
        Set<AttributeTypeOptions> returningAttributes, int sizeLimit, int timeLimit ) throws Exception
    {
        ListOperationContext opContext = new ListOperationContext( this, dn, aliasDerefMode, returningAttributes );
        opContext.setSizeLimit( sizeLimit );
        opContext.setTimeLimit( timeLimit );
        return directoryService.getOperationManager().list( opContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.CoreSession#lookup(org.apache.directory.shared.ldap.name.LdapDN)
     */
    public ClonedServerEntry lookup( LdapDN dn ) throws Exception
    {
        return directoryService.getOperationManager().lookup( new LookupOperationContext( this, dn ) );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.CoreSession#lookup(org.apache.directory.shared.ldap.name.LdapDN)
     */
    public ClonedServerEntry lookup( LdapDN dn, String[] attrId ) throws Exception
    {
        return directoryService.getOperationManager().lookup( 
            new LookupOperationContext( this, dn, attrId ) );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.CoreSession#modify(org.apache.directory.shared.ldap.name.LdapDN, java.util.List)
     */
    public void modify( LdapDN dn, List<Modification> mods ) throws Exception
    {
        if ( mods == null )
        {
            return;
        }
        
        List<Modification> serverModifications = new ArrayList<Modification>( mods.size() );
        
        for ( Modification mod:mods )
        {
            serverModifications.add( new ServerModification( directoryService.getRegistries(), mod ) );
        }
        
        directoryService.getOperationManager().modify( new ModifyOperationContext( this, dn, serverModifications ) );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.CoreSession#move(org.apache.directory.shared.ldap.name.LdapDN, org.apache.directory.shared.ldap.name.LdapDN)
     */
    public void move( LdapDN dn, LdapDN newParent ) throws Exception
    {
        directoryService.getOperationManager().move( new MoveOperationContext( this, dn, newParent ) );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.CoreSession#moveAndRename(org.apache.directory.shared.ldap.name.LdapDN, org.apache.directory.shared.ldap.name.LdapDN, org.apache.directory.shared.ldap.name.Rdn, boolean)
     */
    public void moveAndRename( LdapDN dn, LdapDN newParent, Rdn newRdn, boolean deleteOldRdn ) throws Exception
    {
        directoryService.getOperationManager().moveAndRename( 
            new MoveAndRenameOperationContext( this, dn, newParent, newRdn, deleteOldRdn ) );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.CoreSession#rename(org.apache.directory.shared.ldap.name.LdapDN, org.apache.directory.shared.ldap.name.Rdn, boolean)
     */
    public void rename( LdapDN dn, Rdn newRdn, boolean deleteOldRdn ) throws Exception
    {
        directoryService.getOperationManager().rename( new RenameOperationContext( this, dn, newRdn, deleteOldRdn ) );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.CoreSession#search(org.apache.directory.shared.ldap.name.LdapDN, org.apache.directory.shared.ldap.filter.SearchScope, org.apache.directory.shared.ldap.filter.ExprNode, org.apache.directory.shared.ldap.message.AliasDerefMode, java.util.Set)
     */
    public EntryFilteringCursor search( LdapDN dn, SearchScope scope, ExprNode filter, AliasDerefMode aliasDerefMode,
        Set<AttributeTypeOptions> returningAttributes ) throws Exception
    {
        return directoryService.getOperationManager().search( new SearchOperationContext( this, dn, scope, filter, 
            aliasDerefMode, returningAttributes ) );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.CoreSession#search(org.apache.directory.shared.ldap.name.LdapDN, org.apache.directory.shared.ldap.filter.SearchScope, org.apache.directory.shared.ldap.filter.ExprNode, org.apache.directory.shared.ldap.message.AliasDerefMode, java.util.Set, int, int)
     */
    public EntryFilteringCursor search( LdapDN dn, SearchScope scope, ExprNode filter, AliasDerefMode aliasDerefMode,
        Set<AttributeTypeOptions> returningAttributes, int sizeLimit, int timeLimit ) throws Exception
    {
        SearchOperationContext opContext = new SearchOperationContext( this, dn, scope, filter, 
            aliasDerefMode, returningAttributes );
        opContext.setSizeLimit( sizeLimit );
        opContext.setTimeLimit( timeLimit );
        return directoryService.getOperationManager().search( opContext );
    }


    public boolean isAnonymous()
    {
        return getEffectivePrincipal().getJndiName().isEmpty();
    }


    public void add( AddRequest addRequest ) throws Exception
    {
        AddOperationContext opContext = new AddOperationContext( this, addRequest );
        directoryService.getOperationManager().add( opContext );
        addRequest.getResultResponse().addAll( opContext.getResponseControls() );
    }


    public boolean compare( CompareRequest compareRequest ) throws Exception
    {
        CompareOperationContext opContext = new CompareOperationContext( this, compareRequest );
        boolean result = directoryService.getOperationManager().compare( opContext );
        compareRequest.getResultResponse().addAll( opContext.getResponseControls() );
        return result;
    }


    public void delete( DeleteRequest deleteRequest ) throws Exception
    {
        DeleteOperationContext opContext = new DeleteOperationContext( this, deleteRequest );
        directoryService.getOperationManager().delete( opContext );
        deleteRequest.getResultResponse().addAll( opContext.getResponseControls() );
    }


    public ClonedServerEntry lookup( LdapDN dn, Control[] requestControls, ReferralHandlingMode refMode,
        LdapDN authorized ) throws Exception
    {
        LookupOperationContext opContext = new LookupOperationContext( this, dn );
        opContext.addRequestControls( requestControls );
        return directoryService.getOperationManager().lookup( opContext );
    }


    public boolean exists( LdapDN dn ) throws Exception
    {
        EntryOperationContext opContext = new EntryOperationContext( this, dn );
        return directoryService.getOperationManager().hasEntry( opContext );
    }


    public void modify( ModifyRequest modifyRequest ) throws Exception
    {
        ModifyOperationContext opContext = new ModifyOperationContext( this, modifyRequest );
        directoryService.getOperationManager().modify( opContext );
        modifyRequest.getResultResponse().addAll( opContext.getResponseControls() );
    }


    public void move( ModifyDnRequest modifyDnRequest ) throws Exception
    {
        MoveOperationContext opContext = new MoveOperationContext( this, modifyDnRequest );
        directoryService.getOperationManager().move( opContext );
        modifyDnRequest.getResultResponse().addAll( opContext.getResponseControls() );
    }


    public void moveAndRename( ModifyDnRequest modifyDnRequest ) throws Exception
    {
        MoveAndRenameOperationContext opContext = new MoveAndRenameOperationContext( this, modifyDnRequest );
        directoryService.getOperationManager().moveAndRename( opContext );
        modifyDnRequest.getResultResponse().addAll( opContext.getResponseControls() );
    }


    public void rename( ModifyDnRequest modifyDnRequest ) throws Exception
    {
        RenameOperationContext opContext = new RenameOperationContext( this, modifyDnRequest );
        directoryService.getOperationManager().rename( opContext );
        modifyDnRequest.getResultResponse().addAll( opContext.getResponseControls() );
    }


    public EntryFilteringCursor search( SearchRequest searchRequest ) throws Exception
    {
        SearchOperationContext opContext = new SearchOperationContext( this, searchRequest );
        EntryFilteringCursor cursor = directoryService.getOperationManager().search( opContext );
        searchRequest.getResultResponse().addAll( opContext.getResponseControls() );
        return cursor;
    }


    public void unbind() throws Exception
    {
        directoryService.getOperationManager().unbind( new UnbindOperationContext( this ) );
    }


    public void unbind( UnbindRequest unbindRequest )
    {
        // TODO Auto-generated method stub
        
    }
}
