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
package org.apache.directory.mitosis.operation;


import javax.naming.directory.Attributes;

import org.apache.directory.mitosis.common.CSN;
import org.apache.directory.mitosis.operation.support.EntryUtil;
import org.apache.directory.mitosis.store.ReplicationStore;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.entry.ServerEntryUtils;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * An {@link Operation} that adds a new entry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AddEntryOperation extends Operation
{
    private static final long serialVersionUID = 2294492811671880570L;

    /** The entry to add */
    private final Attributes entry;
    
    /** The entry's dn */
    private final LdapDN dn;


    /**
     * Creates a new instance.
     * 
     * @param entry an entry
     */
    public AddEntryOperation( CSN csn, ServerEntry entry )
    {
        super( csn );

        assert entry != null;

        this.entry = ServerEntryUtils.toAttributesImpl( entry );
        this.dn = entry.getDn();
    }


    protected void execute0( PartitionNexus nexus, ReplicationStore store, Registries registries )
        throws Exception
    {
        if ( !EntryUtil.isEntryUpdatable( registries, nexus, dn, getCSN() ) )
        {
            return;
        }
        
        EntryUtil.createGlueEntries( registries, nexus, dn, false );

        // Replace the entry if an entry with the same name exists.
        if ( nexus.lookup( new LookupOperationContext( registries, dn ) ) != null )
        {
            recursiveDelete( nexus, dn, registries );
        }

        nexus.add( new AddOperationContext( registries, ServerEntryUtils.toServerEntry( entry, dn, registries ) ) );
    }


    @SuppressWarnings("unchecked")
    private void recursiveDelete( PartitionNexus nexus, LdapDN normalizedName, Registries registries )
        throws Exception
    {
        EntryFilteringCursor cursor = nexus.list( new ListOperationContext( registries, normalizedName ) );
        
        if ( !cursor.available() )
        {
            nexus.delete( new DeleteOperationContext( registries, normalizedName ) );
            return;
        }

        while ( cursor.next() )
        {
            ClonedServerEntry sr = cursor.get();
            LdapDN dn = sr.getDn();
            dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
            recursiveDelete( nexus, dn, registries );
        }
        
        nexus.delete( new DeleteOperationContext( registries, normalizedName ) );
    }


    public String toString()
    {
        return super.toString() + ": [" + dn + "].new( " + entry + " )";
    }
}
