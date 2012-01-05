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
package org.apache.directory.server.core.shared.txn.utils;


import java.util.HashMap;
import java.util.Map;

import javax.naming.InvalidNameException;

import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.HasEntryOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.core.api.partition.AbstractPartition;
import org.apache.directory.server.core.api.partition.index.Index;
import org.apache.directory.server.core.api.partition.index.IndexNotFoundException;
import org.apache.directory.server.core.api.partition.index.MasterTable;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.model.name.Dn;


/**
 * A mock partition
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MockPartition extends AbstractPartition
{
    /** The MockMatserTable */
    MasterTable master = new MockMasterTable();

    /** The Mock indices */
    private Map<String, Index<?>> indices = new HashMap<String, Index<?>>();


    public MockPartition( Dn dn ) throws LdapInvalidDnException
    {
        //try
        //{
        setSuffixDn( dn );
        //}
    }


    @Override
    public void sync() throws Exception
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void delete( DeleteOperationContext deleteContext ) throws LdapException
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void add( AddOperationContext addContext ) throws LdapException
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
        // TODO Auto-generated method stub

    }


    @Override
    public EntryFilteringCursor list( ListOperationContext listContext ) throws LdapException
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public Entry lookup( LookupOperationContext lookupContext ) throws LdapException
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public boolean hasEntry( HasEntryOperationContext hasEntryContext ) throws LdapException
    {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public void rename( RenameOperationContext renameContext ) throws LdapException
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void move( MoveOperationContext moveContext ) throws LdapException
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void unbind( UnbindOperationContext unbindContext ) throws LdapException
    {
        // TODO Auto-generated method stub

    }


    @Override
    protected void doDestroy() throws Exception
    {
        // TODO Auto-generated method stub

    }


    @Override
    protected void doInit() throws InvalidNameException, Exception
    {
        // TODO Auto-generated method stub

    }


    /**
     * {@inheritDoc}
     */
    public MasterTable getMasterTable() throws Exception
    {
        return master;
    }


    /**
     * {@inheritDoc}
     */
    public Index<?> getIndex( String oid ) throws IndexNotFoundException
    {
        if ( indices.containsKey( oid ) )
        {
            return indices.get( oid );
        }

        throw new IndexNotFoundException( I18n.err( I18n.ERR_3, oid, oid ) );
    }


    /**
     * {@inheritDoc}
     */
    public void addIndex( Index<?> index ) throws Exception
    {
        indices.put( index.getAttributeId(), index );
    }
}
