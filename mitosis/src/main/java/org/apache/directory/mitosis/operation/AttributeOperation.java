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


import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.mitosis.common.CSN;
import org.apache.directory.mitosis.operation.support.EntryUtil;
import org.apache.directory.mitosis.store.ReplicationStore;


/**
 * An {@link Operation} that adds an attribute to an entry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AttributeOperation extends Operation
{
    /** The attribute's DN */
    protected LdapDN dn;
    
    /** The attribute */
    protected EntryAttribute attribute;


    /**
     * Create a new instance of AttributeOperation. This constructor should not
     * be visible out of this package, as it's only used for deserialization.
     * 
     * @param registries The server registries
     * @param operationType The operation type 
     */
    /* No qualifier*/ AttributeOperation( Registries registries, OperationType operationType )
    {
        super( registries, operationType );
    }
    
    
    /**
     * Create a new operation that affects an entry with the specified name.
     * 
     * @param registries the server registries
     * @param operationType The operation's type
     * @param csn the operation's CSN
     * @param dn the normalized name of an entry 
     * @param attribute an attribute to modify
     */
    public AttributeOperation( Registries registries, OperationType operationType, CSN csn, 
        LdapDN dn, EntryAttribute attribute )
    {
        super( registries, operationType, csn );

        assert dn != null;
        assert attribute != null;

        this.dn = dn;
        this.attribute = attribute.clone();
    }


    /**
     * @return the name of an entry this operation will affect.
     */
    public LdapDN getDn()
    {
        return ( LdapDN ) dn.clone();
    }


    /**
     * Check that we can apply the modification, and create the associated entry, if
     * it does not exists locally.
     * 
     * @param nexus The partition to update
     * @param store the replication storage
     * @param coreSession the current session
     */
    protected final void execute0( PartitionNexus nexus, ReplicationStore store, CoreSession coreSession ) 
        throws Exception
    {
        if ( ! EntryUtil.isEntryUpdatable( coreSession, dn, getCSN() ) )
        {
            return;
        }
        
        EntryUtil.createGlueEntries( coreSession, dn, true );

        execute1( nexus, coreSession );
    }


    /**
     * Apply the requested modification locally
     *
     * @param nexus The partition on which the operation is applied
     * @param coreSession the current session
     * @throws Exception 
     */
    protected abstract void execute1( PartitionNexus nexus, CoreSession coreSession ) throws Exception;


    /**
     * @return Returns the attribute to modify
     */
    public EntryAttribute getAttribute()
    {
        return attribute;
    }

    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return super.toString() + ": [" + dn + ']';
    }
}
