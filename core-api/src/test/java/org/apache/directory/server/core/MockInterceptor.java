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


import java.util.List;
import java.util.Set;

import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
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
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.name.DN;


public class MockInterceptor implements Interceptor
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


    public ClonedServerEntry getRootDSE( NextInterceptor next, GetRootDSEOperationContext opContext )
        throws LdapException
    {
        interceptors.add( this );
        return next.getRootDSE( opContext );
    }


    public DN getSuffix( NextInterceptor next, GetSuffixOperationContext opContext ) throws LdapException
    {
        interceptors.add( this );
        return next.getSuffix( opContext );
    }


    public Set<String> listSuffixes( NextInterceptor next, ListSuffixOperationContext opContext ) throws LdapException
    {
        interceptors.add( this );
        return next.listSuffixes( opContext );
    }


    public boolean compare( NextInterceptor next, CompareOperationContext opContext ) throws LdapException
    {
        interceptors.add( this );
        return next.compare( opContext );
    }


    public void delete( NextInterceptor next, DeleteOperationContext opContext ) throws LdapException
    {
        interceptors.add( this );
        next.delete( opContext );
    }


    public void add( NextInterceptor next, AddOperationContext opContext ) throws LdapException
    {
        interceptors.add( this );
        next.add( opContext );
    }


    public void modify( NextInterceptor next, ModifyOperationContext opContext ) throws LdapException
    {
        interceptors.add( this );
        next.modify( opContext );
    }


    public EntryFilteringCursor list( NextInterceptor next, ListOperationContext opContext ) throws LdapException
    {
        interceptors.add( this );
        return next.list( opContext );
    }


    public EntryFilteringCursor search( NextInterceptor next, SearchOperationContext opContext ) throws LdapException
    {
        interceptors.add( this );
        return next.search( opContext );
    }


    public Entry lookup( NextInterceptor next, LookupOperationContext opContext ) throws LdapException
    {
        interceptors.add( this );
        return next.lookup( opContext );
    }


    public boolean hasEntry( NextInterceptor next, EntryOperationContext opContext ) throws LdapException
    {
        interceptors.add( this );
        return next.hasEntry( opContext );
    }


    public void rename( NextInterceptor next, RenameOperationContext opContext ) throws LdapException
    {
        interceptors.add( this );
        next.rename( opContext );
    }


    public void move( NextInterceptor next, MoveOperationContext opContext ) throws LdapException
    {
        interceptors.add( this );
        next.move( opContext );
    }


    public void moveAndRename( NextInterceptor next, MoveAndRenameOperationContext opContext ) throws LdapException
    {
        interceptors.add( this );
        next.moveAndRename( opContext );
    }


    public void bind( NextInterceptor next, BindOperationContext opContext ) throws LdapException
    {
        interceptors.add( this );
        next.bind( opContext );
    }


    public void unbind( NextInterceptor next, UnbindOperationContext opContext ) throws LdapException
    {
        interceptors.add( this );
        next.unbind( opContext );
    }


    public String toString()
    {
        return name;
    }
}