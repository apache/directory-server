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
package org.apache.directory.server.core;

import java.util.Set;

import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.filtering.BaseEntryFilteringCursor;
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
import org.apache.directory.shared.ldap.name.DN;

public class MockOperationManager implements OperationManager
{
    int count;
    
    public MockOperationManager( int count )
    {
        this.count = count;
    }
    
    public void add( AddOperationContext opContext ) throws Exception
    {
    }

    
    public void bind( BindOperationContext opContext ) throws Exception
    {
    }

    
    public boolean compare( CompareOperationContext opContext ) throws Exception
    {
        return false;
    }


    public void delete( DeleteOperationContext opContext ) throws Exception
    {
    }

    public DN getMatchedName( GetMatchedNameOperationContext opContext ) throws Exception
    {
        return null;
    }

    public ClonedServerEntry getRootDSE( GetRootDSEOperationContext opContext ) throws Exception
    {
        return null;
    }

    public DN getSuffix( GetSuffixOperationContext opContext ) throws Exception
    {
        return null;
    }

    public boolean hasEntry( EntryOperationContext opContext ) throws Exception
    {
        return false;
    }

    public EntryFilteringCursor list( ListOperationContext opContext ) throws Exception
    {
        return null;
    }

    public Set<String> listSuffixes( ListSuffixOperationContext opContext ) throws Exception
    {
        return null;
    }

    public ClonedServerEntry lookup( LookupOperationContext opContext ) throws Exception
    {
        return null;
    }

    public void modify( ModifyOperationContext opContext ) throws Exception
    {
    }

    public void move( MoveOperationContext opContext ) throws Exception
    {
    }

    public void moveAndRename( MoveAndRenameOperationContext opContext ) throws Exception
    {
    }

    public void rename( RenameOperationContext opContext ) throws Exception
    {
    }

    public EntryFilteringCursor search( SearchOperationContext opContext ) throws Exception
    {
        MockCursor cursor = new MockCursor( count );
        cursor.setSchemaManager( opContext.getSession().getDirectoryService().getSchemaManager() );
        return new BaseEntryFilteringCursor( cursor, opContext );
    }


    public void unbind( UnbindOperationContext opContext ) throws Exception
    {
    }
}
