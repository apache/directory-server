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

import org.apache.directory.server.core.api.OperationManager;
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
import org.apache.directory.server.core.filtering.BaseEntryFilteringCursor;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;

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

    public Entry getRootDSE( GetRootDSEOperationContext getRootDseContext ) throws LdapException
    {
        return null;
    }

    public boolean hasEntry( EntryOperationContext hasEntryContext ) throws LdapException
    {
        return false;
    }

    public EntryFilteringCursor list( ListOperationContext listContext ) throws LdapException
    {
        return null;
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
        return new BaseEntryFilteringCursor( cursor, searchContext );
    }


    public void unbind( UnbindOperationContext unbindContext ) throws LdapException
    {
    }
}
