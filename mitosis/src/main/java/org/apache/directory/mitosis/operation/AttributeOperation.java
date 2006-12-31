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


import javax.naming.NamingException;
import javax.naming.directory.Attribute;

import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
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
    private final LdapDN name;
    private final Attribute attribute;


    /**
     * Create a new operation that affects an entry with the specified name.
     * 
     * @param name the normalized name of an entry 
     * @param attribute an attribute to modify
     */
    public AttributeOperation( CSN csn, LdapDN name, Attribute attribute )
    {
        super( csn );

        assert name != null;
        assert attribute != null;

        this.name = name;
        this.attribute = ( Attribute ) attribute.clone();
    }


    /**
     * Returns the attribute to modify.
     */
    public Attribute getAttribute()
    {
        return ( Attribute ) attribute.clone();
    }


    /**
     * Returns the name of an entry this operation will affect.
     */
    public LdapDN getName()
    {
        return ( LdapDN ) name.clone();
    }


    protected final void execute0( PartitionNexus nexus, ReplicationStore store, AttributeTypeRegistry registry ) 
        throws NamingException
    {
        if ( !EntryUtil.isEntryUpdatable( nexus, name, getCSN() ) )
        {
            return;
        }
        EntryUtil.createGlueEntries( nexus, name, true );

        execute1( nexus );
    }


    protected abstract void execute1( PartitionNexus nexus ) throws NamingException;


    /**
     * Returns string representation of this operation.
     */
    public String toString()
    {
        return super.toString() + ": [" + name.toString() + ']';
    }
}
