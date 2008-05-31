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


import java.util.Iterator;

import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.interceptor.context.GetMatchedNameOperationContext;
import org.apache.directory.server.core.interceptor.context.GetRootDSEOperationContext;
import org.apache.directory.server.core.interceptor.context.GetSuffixOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.ListSuffixOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.interceptor.context.UnbindOperationContext;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * The default implementation of an OperationManager.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DefaultOperationManager implements OperationManager
{
    private final DirectoryService directoryService;


    public DefaultOperationManager( DirectoryService directoryService )
    {
        this.directoryService = directoryService;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.OperationManager#add(org.apache.directory.server.core.interceptor.context.AddOperationContext)
     */
    public void add( AddOperationContext opContext ) throws Exception
    {
        directoryService.getInterceptorChain().add( opContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.OperationManager#bind(org.apache.directory.server.core.interceptor.context.BindOperationContext)
     */
    public void bind( BindOperationContext opContext ) throws Exception
    {
        directoryService.getInterceptorChain().bind( opContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.OperationManager#compare(org.apache.directory.server.core.interceptor.context.CompareOperationContext)
     */
    public boolean compare( CompareOperationContext opContext ) throws Exception
    {
        return directoryService.getInterceptorChain().compare( opContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.OperationManager#delete(org.apache.directory.server.core.interceptor.context.DeleteOperationContext)
     */
    public void delete( DeleteOperationContext opContext ) throws Exception
    {
        directoryService.getInterceptorChain().delete( opContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.OperationManager#getMatchedName(org.apache.directory.server.core.interceptor.context.GetMatchedNameOperationContext)
     */
    public LdapDN getMatchedName( GetMatchedNameOperationContext opContext ) throws Exception
    {
        return directoryService.getInterceptorChain().getMatchedName( opContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.OperationManager#getRootDSE(org.apache.directory.server.core.interceptor.context.GetRootDSEOperationContext)
     */
    public ClonedServerEntry getRootDSE( GetRootDSEOperationContext opContext ) throws Exception
    {
        return directoryService.getInterceptorChain().getRootDSE( opContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.OperationManager#getSuffix(org.apache.directory.server.core.interceptor.context.GetSuffixOperationContext)
     */
    public LdapDN getSuffix( GetSuffixOperationContext opContext ) throws Exception
    {
        return directoryService.getInterceptorChain().getSuffix( opContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.OperationManager#hasEntry(org.apache.directory.server.core.interceptor.context.EntryOperationContext)
     */
    public boolean hasEntry( EntryOperationContext opContext ) throws Exception
    {
        return directoryService.getInterceptorChain().hasEntry( opContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.OperationManager#list(org.apache.directory.server.core.interceptor.context.ListOperationContext)
     */
    public EntryFilteringCursor list( ListOperationContext opContext ) throws Exception
    {
        return directoryService.getInterceptorChain().list( opContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.OperationManager#listSuffixes(org.apache.directory.server.core.interceptor.context.ListSuffixOperationContext)
     */
    public Iterator<String> listSuffixes( ListSuffixOperationContext opContext ) throws Exception
    {
        return directoryService.getInterceptorChain().listSuffixes( opContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.OperationManager#lookup(org.apache.directory.server.core.interceptor.context.LookupOperationContext)
     */
    public ClonedServerEntry lookup( LookupOperationContext opContext ) throws Exception
    {
        return directoryService.getInterceptorChain().lookup( opContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.OperationManager#modify(org.apache.directory.server.core.interceptor.context.ModifyOperationContext)
     */
    public void modify( ModifyOperationContext opContext ) throws Exception
    {
        directoryService.getInterceptorChain().modify( opContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.OperationManager#move(org.apache.directory.server.core.interceptor.context.MoveOperationContext)
     */
    public void move( MoveOperationContext opContext ) throws Exception
    {
        directoryService.getInterceptorChain().move( opContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.OperationManager#moveAndRename(org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext)
     */
    public void moveAndRename( MoveAndRenameOperationContext opContext ) throws Exception
    {
        directoryService.getInterceptorChain().moveAndRename( opContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.OperationManager#rename(org.apache.directory.server.core.interceptor.context.RenameOperationContext)
     */
    public void rename( RenameOperationContext opContext ) throws Exception
    {
        directoryService.getInterceptorChain().rename( opContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.OperationManager#search(org.apache.directory.server.core.interceptor.context.SearchOperationContext)
     */
    public EntryFilteringCursor search( SearchOperationContext opContext ) throws Exception
    {
        return directoryService.getInterceptorChain().search( opContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.OperationManager#unbind(org.apache.directory.server.core.interceptor.context.UnbindOperationContext)
     */
    public void unbind( UnbindOperationContext opContext ) throws Exception
    {
        directoryService.getInterceptorChain().unbind( opContext );
    }
}
