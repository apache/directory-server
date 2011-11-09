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
package org.apache.directory.server.core.api;


import java.util.List;

import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.NextInterceptor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.api.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.api.interceptor.context.GetRootDSEOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.interceptor.context.UnbindOperationContext;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;


public class MockInterceptor extends BaseInterceptor
{
    final String name;
    final List<MockInterceptor> interceptors;


    public MockInterceptor( String name, List<MockInterceptor> interceptors )
    {
        this.name = name;
        this.interceptors = interceptors;
    }


    public String getName()
    {
        return this.name;
    }


    public List<MockInterceptor> getInterceptors()
    {
        return interceptors;
    }


    public void init( DirectoryService directoryService ) throws LdapException
    {
    }


    public void destroy()
    {
    }


    /**
     * {@inheritDoc}
     */
    public void add( NextInterceptor next, AddOperationContext addContext ) throws LdapException
    {
        interceptors.add( this );
        next.add( addContext );
    }


    /**
     * {@inheritDoc}
     */
    public void bind( BindOperationContext bindContext ) throws LdapException
    {
        interceptors.add( this );
        next( bindContext );
    }


    /**
     * {@inheritDoc}
     */
    public boolean compare( CompareOperationContext compareContext ) throws LdapException
    {
        interceptors.add( this );
        return next( compareContext );
    }


    /**
     * {@inheritDoc}
     */
    public void delete( DeleteOperationContext deleteContext ) throws LdapException
    {
        interceptors.add( this );
        next( deleteContext );
    }


    /**
     * {@inheritDoc}
     */
    public Entry getRootDSE( GetRootDSEOperationContext getRootDseContext )
        throws LdapException
        {
        interceptors.add( this );
        return next( getRootDseContext );
        }


    /**
     * {@inheritDoc}
     */
    public boolean hasEntry( EntryOperationContext hasEntryContext ) throws LdapException
    {
        interceptors.add( this );
        return next( hasEntryContext );
    }


    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor list( NextInterceptor next, ListOperationContext listContext ) throws LdapException
    {
        interceptors.add( this );
        return next.list( listContext );
    }


    /**
     * {@inheritDoc}
     */
    public Entry lookup( NextInterceptor next, LookupOperationContext lookupContext ) throws LdapException
    {
        interceptors.add( this );
        return next.lookup( lookupContext );
    }


    /**
     * {@inheritDoc}
     */
    public void modify( NextInterceptor next, ModifyOperationContext modifyContext ) throws LdapException
    {
        interceptors.add( this );
        next.modify( modifyContext );
    }


    /**
     * {@inheritDoc}
     */
    public void move( NextInterceptor next, MoveOperationContext moveContext ) throws LdapException
    {
        interceptors.add( this );
        next.move( moveContext );
    }


    /**
     * {@inheritDoc}
     */
    public void moveAndRename( NextInterceptor next, MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        interceptors.add( this );
        next.moveAndRename( moveAndRenameContext );
    }


    /**
     * {@inheritDoc}
     */
    public void rename( NextInterceptor next, RenameOperationContext renameContext ) throws LdapException
    {
        interceptors.add( this );
        next.rename( renameContext );
    }


    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor search( NextInterceptor next, SearchOperationContext searchContext ) throws LdapException
    {
        interceptors.add( this );
        return next.search( searchContext );
    }


    /**
     * {@inheritDoc}
     */
    public void unbind( UnbindOperationContext unbindContext ) throws LdapException
    {
        interceptors.add( this );
        next( unbindContext );
    }


    public String toString()
    {
        return name;
    }
}