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
package org.apache.directory.server.core.shared.partition;


import javax.naming.InvalidNameException;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.HasEntryOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.core.api.partition.AbstractPartition;
import org.apache.directory.server.core.api.partition.PartitionReadTxn;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.core.api.partition.PartitionWriteTxn;
import org.apache.directory.server.core.api.partition.Subordinates;

/**
 * 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class RootPartition extends AbstractPartition
{

    protected RootPartition( SchemaManager schemaManager )
    {
        // TODO Auto-generated constructor stub
    }

    
    @Override
    public PartitionReadTxn beginReadTransaction()
    {
        return new PartitionReadTxn();
    }

    
    @Override
    public PartitionWriteTxn beginWriteTransaction()
    {
        return new PartitionWriteTxn();
    }

    @Override
    protected void doRepair() throws LdapException
    {
    }


    @Override
    public Entry delete( DeleteOperationContext deleteContext ) throws LdapException
    {
        // TODO Auto-generated method stub
        return null;
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
    public void saveContextCsn( PartitionTxn partitionTxn ) throws LdapException
    {
        // TODO Auto-generated method stub
        
    }


    @Override
    public Subordinates getSubordinates( PartitionTxn partitionTxn, Entry entry ) throws LdapException
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    protected void doDestroy( PartitionTxn partitionTxn ) throws LdapException
    {
        // TODO Auto-generated method stub
        
    }


    @Override
    protected void doInit() throws InvalidNameException, LdapException
    {
        // TODO Auto-generated method stub
        
    }
}
