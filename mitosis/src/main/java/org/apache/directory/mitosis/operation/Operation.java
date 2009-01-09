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


import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;

import org.apache.directory.mitosis.common.CSN;
import org.apache.directory.mitosis.common.CSNFactory;
import org.apache.directory.mitosis.common.Constants;
import org.apache.directory.mitosis.store.ReplicationStore;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.entry.DefaultServerAttribute;
import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.LdapDNSerializer;
import org.apache.directory.shared.ldap.schema.AttributeType;


/**
 * Represents an operation performed on one or more entries in replicated
 * {@link Partition}. Each {@link Operation} has its own {@link CSN} which
 * identifies itself.
 * <p>
 * An {@link Operation} is usually created by calling factory methods in
 * {@link OperationFactory}, which produces a {@link CompositeOperation} of
 * smaller multiple operations.  For example,
 * {@link OperationFactory#newDelete(LdapDN)} returns a
 * {@link CompositeOperation} which consists of two
 * {@link ReplaceAttributeOperation}s; one updates {@link Constants#ENTRY_CSN}
 * attribute and the other updates {@link Constants#ENTRY_DELETED}.  Refer
 * to {@link OperationFactory} to find out what LDAP/JNDI operation is 
 * translated into what {@link Operation} instance.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class Operation implements Externalizable
{
    /**
     * Declares the Serial Version UID.
     *
     * @see <a
     *      href="http://c2.com/cgi/wiki?AlwaysDeclareSerialVersionUid">Always
     *      Declare Serial Version UID</a>
     */
    private static final long serialVersionUID = 1L;

    /** The entry CSN */
    protected CSN csn;

    /** The operation type */
    protected OperationType operationType;

    /** A reference on the server registries */
    protected Registries registries;


    /**
     * Creates a new instance of Operation, for the entry which
     * CSN is given as a parameter. This constructor is not visible
     * out of this package, as it's only used for the de-serialization 
     * process.
     *
     * @param registries the server registries
     * @param operationType the operation type
     */
    /* no qualifier */Operation( Registries registries, OperationType operationType )
    {
        this.registries = registries;
        this.operationType = operationType;
    }


    /**
     * Creates a new instance of Operation, for the entry which
     * csn is given as a parameter.
     *
     * @param registries the server registries
     * @param operationType the operation type
     * @param csn The entry's CSN.
     */
    protected Operation( Registries registries, OperationType operationType, CSN csn )
    {
        assert csn != null;
        this.registries = registries;
        this.csn = csn;
        this.operationType = operationType;
    }


    /**
     * @return Returns {@link CSN} for this operation.
     */
    public CSN getCSN()
    {
        return csn;
    }


    /**
     * Replicates this operation on the specified nexus.
     * 
     * @param nexus the partition nexus
     * @param store the replication store
     * @param coreSession the current session
     * @param csnFactory The CSN Factory
     */
    public final void execute( PartitionNexus nexus, ReplicationStore store, CoreSession coreSession, CSNFactory csnFactory ) throws Exception
    {
        synchronized ( nexus )
        {
            applyOperation( nexus, store, coreSession, csnFactory );
            store.putLog( this );
        }
    }


    /**
     * Not supported. We should never call this method directly.
     * 
     * @param nexus the partition nexus
     * @param store the replication store
     * @param coreSession the current session
     * @param csnFactory The CSN Factory
     * @throws Exception
     */
    protected void applyOperation( PartitionNexus nexus, ReplicationStore store, CoreSession coreSession,
        CSNFactory csnFactory ) throws Exception
    {
        throw new OperationNotSupportedException( nexus.getSuffixDn().toString() );
    }


    /**
     * De-serialize an Attribute Operation
     *
     * @param in the stream from which we will read an AttributeOperation
     * @param registries the server registries
     * @param operation the operation we will feed
     * @return an AttributeOperation
     * @throws ClassNotFoundException 
     * @throws IOException
     */
    private static Operation readAttributeOperation( ObjectInput in, Registries registries, Operation operation )
        throws ClassNotFoundException, IOException
    {
        AttributeOperation attributeOperation = ( AttributeOperation ) operation;
        // Read the DN
        LdapDN dn = LdapDNSerializer.deserialize( in );

        // Read the Attribute ID
        String id = in.readUTF();

        try
        {
            // Get the AttributeType
            AttributeType at = registries.getAttributeTypeRegistry().lookup( id );

            // De-serialize the attribute
            DefaultServerAttribute attribute = new DefaultServerAttribute( id, at );
            attribute.deserialize( in );

            // Store the read data into the operation 
            attributeOperation.dn = dn;
            attributeOperation.attribute = attribute;

            return operation;
        }
        catch ( NamingException ne )
        {
            throw new IOException( "Cannot find the '" + id + "' attributeType" );
        }
    }


    /**
     * De-serialize an operation. This is a recursive method, as we may have 
     * composite operations.
     *
     * @param registries The server registries
     * @param in the stream from which we will read an operation
     * @return an operation
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public static Operation deserialize( Registries registries, ObjectInput in ) throws ClassNotFoundException,
        IOException
    {
        // Read the operation type
        int opTypeValue = in.readInt();
        OperationType opType = OperationType.get( opTypeValue );

        // Read the CSN
        CSN csn = ( CSN ) in.readObject();

        Operation operation = null;

        switch ( opType )
        {
            case ADD_ATTRIBUTE:
                // Create a new AddAttribute operation
                operation = new AddAttributeOperation( registries );

                // Set the CSN
                operation.csn = csn;

                // Read it
                readAttributeOperation( in, registries, operation );

                return operation;

            case DELETE_ATTRIBUTE:
                // Create a new DeleteAttribute operation
                operation = new DeleteAttributeOperation( registries );

                // Set the CSN
                operation.csn = csn;

                // Read it
                readAttributeOperation( in, registries, operation );

                return operation;

            case REPLACE_ATTRIBUTE:
                // Create a new ReplaceAttribute operation
                operation = new ReplaceAttributeOperation( registries );

                // Set the CSN
                operation.csn = csn;

                // Read it
                readAttributeOperation( in, registries, operation );

                return operation;

            case ADD_ENTRY:
                // Create a new AddEntry operation
                operation = new AddEntryOperation( registries );

                // Set the CSN
                operation.csn = csn;

                DefaultServerEntry entry = new DefaultServerEntry( registries );
                entry.deserialize( in );
                ( ( AddEntryOperation ) operation ).setEntry( entry );

                return operation;

            case COMPOSITE_OPERATION:
                // Create a new Composite operation
                operation = new CompositeOperation( registries );

                // Set the CSN
                operation.csn = csn;

                // Read the number of operations to deserialize
                int nbOperations = in.readInt();

                for ( int i = 0; i < nbOperations; i++ )
                {
                    Operation child = deserialize( registries, in );
                    child.csn = csn;
                    ( ( CompositeOperation ) operation ).add( child );
                }

                return operation;

            default:
                throw new IOException( "Cannot read the unkown operation" );
        }
    }


    /**
     * Serialize an operation. This is a recursive method, as an operation
     * can be composite.
     *
     * @param operation the operation to serialize
     * @param out the stream into which the resulting serialized operation will be stored
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public static void serialize( Operation operation, ObjectOutput out ) throws ClassNotFoundException, IOException
    {
        OperationType opType = operation.operationType;

        // Write the operation type
        out.writeInt( opType.ordinal() );

        // Write the CSN
        out.writeObject( operation.csn );

        switch ( opType )
        {
            case REPLACE_ATTRIBUTE:
            case DELETE_ATTRIBUTE:
            case ADD_ATTRIBUTE:
                AttributeOperation attrOp = ( AttributeOperation ) operation;

                // Write the DN
                LdapDNSerializer.serialize( attrOp.dn, out );

                // Write the attribute ID
                out.writeUTF( ( ( AttributeOperation ) operation ).attribute.getId() );

                // Write the attribute
                DefaultServerAttribute attr = ( DefaultServerAttribute ) ( attrOp.attribute );
                attr.serialize( out );
                return;

            case ADD_ENTRY:
                ( ( DefaultServerEntry ) ( ( AddEntryOperation ) operation ).getEntry() ).serialize( out );
                return;

            case COMPOSITE_OPERATION:
                out.writeInt( ( ( CompositeOperation ) operation ).size() );

                // Loop on all the operations
                for ( Operation child : ( ( CompositeOperation ) operation ).getChildren() )
                {
                    serialize( child, out );
                }

                return;
        }
    }


    /**
     * Read the CSN from an input stream
     * 
     * @param in the input stream
     * @throws ClassNotFoundException if the read object is not a CSN
     * @throws IOException if we can't read from the input stream
     */
    public void readExternal( ObjectInput in ) throws ClassNotFoundException, IOException
    {
        csn = ( CSN ) in.readObject();
    }


    /**
     * Write the CSN to an output stream
     * 
     * @param out the output stream in which the CSN is written
     * @throws IOException if we can't write to the stream
     */
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        out.writeObject( csn );
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return csn.toString();
    }
}
