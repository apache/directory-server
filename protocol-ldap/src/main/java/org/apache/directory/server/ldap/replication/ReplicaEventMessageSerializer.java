/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.ldap.replication;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import jdbm.helper.Serializer;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.message.controls.ChangeType;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;


/**
 * A ReplicaEventMessage serializer/deserializer.
 * 
 * A modification is serialized following this format : <br/>
 * <ul>
 * <li>byte : EventType</li>
 * <li>byte[] : the serialized DN</li>
 * <li>byte[] : the serialized entry</li>
 * </ul>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ReplicaEventMessageSerializer implements Serializer
{
    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** The schemaManager */
    private transient SchemaManager schemaManager;


    /**
     * Creates a new instance of ReplicaEventMessageSerializer.
     *
     * @param schemaManager The reference to the global schemaManager
     */
    public ReplicaEventMessageSerializer( SchemaManager schemaManager )
    {
        this.schemaManager = schemaManager;
    }


    /**
     * {@inheritDoc}
     */
    public byte[] serialize( Object object ) throws IOException
    {
        ReplicaEventMessage replicaEventMessage = ( ReplicaEventMessage ) object;

        Entry entry = replicaEventMessage.getEntry();
        ChangeType changeType = replicaEventMessage.getChangeType();

        try ( ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream( baos ) )
        {

            // The change type first
            out.writeByte( changeType.getValue() );

            // The entry DN
            entry.getDn().writeExternal( out );

            // The entry
            entry.writeExternal( out );

            out.flush();

            return baos.toByteArray();
        }
    }


    /**
     *  Deserialize a ReplicaEventMessage.
     *  
     *  @param bytes the byte array containing the serialized ReplicaEventMessage
     *  @return An instance of a ReplicaEventMessage object 
     *  @throws IOException if we can't deserialize the ReplicaEventMessage
     */
    public Object deserialize( byte[] bytes ) throws IOException
    {
        ObjectInputStream in = new ObjectInputStream( new ByteArrayInputStream( bytes ) );

        ReplicaEventMessage replicaEventMessage = null;

        try
        {
            // The changeType
            byte type = in.readByte();
            ChangeType changeType = ChangeType.getChangeType( type );

            // The Entry's DN
            Dn entryDn = new Dn( schemaManager );
            entryDn.readExternal( in );

            // The Entry
            Entry entry = new DefaultEntry( schemaManager );
            entry.readExternal( in );
            entry.setDn( entryDn );

            // And create a ReplicaEventMessage
            replicaEventMessage = new ReplicaEventMessage( changeType, entry );
        }
        catch ( ClassNotFoundException cnfe )
        {
            // there is nothing we can do here...
        }

        return replicaEventMessage;
    }
}
