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
import java.io.ObjectOutputStream;

import jdbm.helper.Serializer;

import org.apache.directory.server.core.event.EventType;
import org.apache.directory.server.core.partition.impl.btree.jdbm.ServerEntrySerializer;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;

/**
 * A Modification serializer/deserializer.
 * A modification is serialized following this format : <br/>
 * <ul>
 * <li>byte : EventType</li>
 * <li>int : entry's length in bytes</li>
 * <li>byte[] : the serialized entry</li>
 * </ul>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ModificationSerializer implements Serializer
{
    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** The internal entry serializer */
    private transient ServerEntrySerializer entrySerializer;

    /**
     * Creates a new instance of ServerEntrySerializer.
     *
     * @param schemaManager The reference to the global schemaManager
     */
    public ModificationSerializer( SchemaManager schemaManager )
    {
        entrySerializer = new ServerEntrySerializer( schemaManager );
    }


    /**
     * {@inheritDoc}
     */
    public byte[] serialize( Object object ) throws IOException
    {
        Modification modification = (Modification)object;
    
        Entry entry = modification.getEntry();
        EventType type = modification.getEventType();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        // The event type
        out.writeByte( type.getMask() );
        
        // The entry
        byte[] data = entrySerializer.serialize( entry );

        // Entry's length
        out.writeInt( data.length );
        
        // Entry's data
        out.write( data );
        
        out.flush();

        return baos.toByteArray();
    }

    
    /**
     *  Deserialize a Modification.
     *  
     *  @param bytes the byte array containing the serialized modification
     *  @return An instance of a Modification object 
     *  @throws IOException if we can't deserialize the Modification
     */
    public Object deserialize( byte[] bytes ) throws IOException
    {
        ObjectInputStream in = new ObjectInputStream( new ByteArrayInputStream( bytes ) );

        // Read the eventType
        byte type = in.readByte();
        
        // The Entry's length
        int length = in.readInt();
        byte[] data = new byte[length];
        
        // The entry itself
        in.read( data );
        Entry entry = (Entry)entrySerializer.deserialize( data );
        
        // And create a modification
        Modification modification = new Modification( EventType.getType( type ), entry );
        
        return modification;
    }
}
