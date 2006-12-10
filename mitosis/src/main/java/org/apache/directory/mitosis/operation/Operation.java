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


import java.io.Serializable;

import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;

import org.apache.directory.mitosis.common.CSN;
import org.apache.directory.mitosis.common.Constants;
import org.apache.directory.mitosis.store.ReplicationStore;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.core.schema.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * Represents an operation performed on one or more entries in replicated
 * {@link Partition}.  Each {@link Operation} has its own {@link CSN} which
 * identifies itself.
 * <p>
 * An {@link Operation} is usually created by calling factory methods in
 * {@link OperationFactory}, which produces a {@link CompositeOperation} of
 * smaller multiple operation.  For example,
 * {@link OperationFactory#newDelete(LdapDN)} returns a
 * {@link CompositeOperation} which consists of two
 * {@link ReplaceAttributeOperation}s; one updates {@link Constants#ENTRY_CSN}
 * attribute and the other updates {@link Constants#ENTRY_DELETED}.  Refer
 * to {@link OperationFactory} to find out what LDAP/JNDI operation is 
 * translated into what {@link Operation} instance.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class Operation implements Serializable
{
    /**
     * Declares the Serial Version Uid.
     *
     * @see <a
     *      href="http://c2.com/cgi/wiki?AlwaysDeclareSerialVersionUid">Always
     *      Declare Serial Version Uid</a>
     */
    private static final long serialVersionUID = 1L;

    /** The entry CSN */
    private CSN csn;


    /**
     * Creates a new instance of Operation, for the entry which
     * csn is given as a parameter.
     *
     * @param csn The entry's csn.
     */
    public Operation( CSN csn )
    {
        assert csn != null;
        this.csn = csn;
    }


    /**
     * @return Returns {@link CSN} for this operation.
     */
    public CSN getCSN()
    {
        return csn;
    }


    /**
     * @return the CSN for this operation
     */
    public String toString()
    {
        return csn.toString();
    }


    /**
     * Exeutes this operation on the specified nexus.
     */
    public final void execute( PartitionNexus nexus, ReplicationStore store, AttributeTypeRegistry registry ) 
        throws NamingException
    {
        synchronized ( nexus )
        {
            execute0( nexus, store, registry );
            store.putLog( this );
        }
    }

    @SuppressWarnings("unused")
    protected void execute0( PartitionNexus nexus, ReplicationStore store, AttributeTypeRegistry registry ) 
        throws NamingException
    {
        throw new OperationNotSupportedException( nexus.getSuffix().toString() );
    }
}
