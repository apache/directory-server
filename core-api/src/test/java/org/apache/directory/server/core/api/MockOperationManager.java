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
package org.apache.directory.server.core.api;


import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
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
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.interceptor.context.UnbindOperationContext;


public class MockOperationManager implements OperationManager
{
    int count;


    public MockOperationManager( int count )
    {
        this.count = count;
    }


    public void add( AddOperationContext addContext ) throws LdapException
    {
    }


    public void bind( BindOperationContext bindContext ) throws LdapException
    {
    }


    public boolean compare( CompareOperationContext compareContext ) throws LdapException
    {
        return false;
    }


    public void delete( DeleteOperationContext deleteContext ) throws LdapException
    {
    }


    public Entry getRootDse( GetRootDseOperationContext getRootDseContext ) throws LdapException
    {
        return null;
    }


    public boolean hasEntry( HasEntryOperationContext hasEntryContext ) throws LdapException
    {
        return false;
    }


    public Entry lookup( LookupOperationContext lookupContext ) throws LdapException
    {
        return null;
    }


    public void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
    }


    public void move( MoveOperationContext moveContext ) throws LdapException
    {
    }


    public void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
    }


    public void rename( RenameOperationContext renameContext ) throws LdapException
    {
    }


    public EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException
    {
        MockCursor cursor = new MockCursor( count );
        cursor.setSchemaManager( searchContext.getSession().getDirectoryService().getSchemaManager() );
        return new BaseEntryFilteringCursor( cursor, searchContext, cursor.schemaManager );
    }


    public void unbind( UnbindOperationContext unbindContext ) throws LdapException
    {
    }


    public void lockWrite()
    {
    }


    public void unlockWrite()
    {
    }


    @Override
    public void lockRead()
    {
    }


    @Override
    public void unlockRead()
    {
    }


    /**
     * {@inheritDoc}
     */
    public ReadWriteLock getRWLock()
    {
        return new ReentrantReadWriteLock();
    }
}
