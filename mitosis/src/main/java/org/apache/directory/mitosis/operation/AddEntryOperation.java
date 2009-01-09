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


import org.apache.directory.mitosis.common.CSN;
import org.apache.directory.mitosis.common.CSNFactory;
import org.apache.directory.mitosis.operation.support.EntryUtil;
import org.apache.directory.mitosis.store.ReplicationStore;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * An {@link Operation} that adds a new entry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AddEntryOperation extends Operation
{
    /**
     * Declares the Serial Version UID.
     *
     * @see <a
     *      href="http://c2.com/cgi/wiki?AlwaysDeclareSerialVersionUid">Always
     *      Declare Serial Version UID</a>
     */
    private static final long serialVersionUID = 2294492811671880570L;

    /** The entry to add */
    private Entry entry;


    /**
     * Creates a new operation that adds the specified entry. This 
     * constructor will not be visible out of this package, as it is 
     * only used for the de-serialization process.
     * 
     * @param registries the registries instance
     */
    public AddEntryOperation( Registries registries )
    {
        super( registries, OperationType.ADD_ENTRY );
    }
    
    
    /**
     * Creates a new instance.
     * 
     * @param entry an entry
     */
    public AddEntryOperation( Registries registries, CSN csn, ServerEntry entry )
    {
        super( registries, OperationType.ADD_ENTRY, csn );

        assert entry != null;

        this.entry = entry;
    }


    /**
     * Inject the entry into the local server
     * 
     * @param nexus the local partition to update
     * @param store not used... Just for inheritance sake.
     * @param coreSession the current session
     */
    protected void applyOperation( PartitionNexus nexus, ReplicationStore store, CoreSession coreSession, CSNFactory csnFactory )
        throws Exception
    {
        if ( ! EntryUtil.isEntryUpdatable( csnFactory, coreSession, entry.getDn(), getCSN() ) )
        {
            return;
        }
        
        EntryUtil.createGlueEntries( coreSession, entry.getDn(), false );

        // Replace the entry if an entry with the same name exists.
        if ( nexus.lookup( new LookupOperationContext( coreSession, entry.getDn() ) ) != null )
        {
            recursiveDelete( nexus, entry.getDn(), coreSession );
        }

        nexus.add( new AddOperationContext( coreSession, (ServerEntry)entry ) );
    }


    /**
     * 
     * TODO recursiveDelete.
     *
     * @param nexus
     * @param normalizedName
     * @param coreSession
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private void recursiveDelete( PartitionNexus nexus, LdapDN normalizedName, CoreSession coreSession )
        throws Exception
    {
        EntryFilteringCursor cursor = nexus.list( new ListOperationContext( coreSession, normalizedName ) );
        
        if ( !cursor.available() )
        {
            nexus.delete( new DeleteOperationContext( coreSession, normalizedName ) );
            return;
        }

        Registries registries = coreSession.getDirectoryService().getRegistries();
        while ( cursor.next() )
        {
            ClonedServerEntry sr = cursor.get();
            LdapDN dn = sr.getDn();
            dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
            recursiveDelete( nexus, dn, coreSession );
        }
        
        nexus.delete( new DeleteOperationContext( coreSession, normalizedName ) );
    }

    
    /**
     * Set the Entry to add into this AddEntry instance.
     *
     * @param entry the entry to add
     */
    public void setEntry( Entry entry )
    {
        this.entry = entry;
    }


    /**
     * @return the operation's entry
     */
    public Entry getEntry()
    {
        return entry;
    }

    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return super.toString() + ": [" + entry.getDn() + "].new( " + entry + " )";
    }
}
