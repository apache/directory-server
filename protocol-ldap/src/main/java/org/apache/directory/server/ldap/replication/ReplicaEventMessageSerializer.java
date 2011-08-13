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

import org.apache.directory.server.core.partition.impl.btree.jdbm.EntrySerializer;
import org.apache.directory.shared.ldap.codec.api.LdapApiService;
import org.apache.directory.shared.ldap.codec.api.LdapApiServiceFactory;
import org.apache.directory.shared.ldap.extras.controls.SyncModifyDn;
import org.apache.directory.shared.ldap.extras.controls.SyncModifyDnType;
import org.apache.directory.shared.ldap.extras.controls.syncrepl_impl.SyncModifyDnDecorator;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.message.controls.ChangeType;
import org.apache.directory.shared.ldap.model.name.Dn;
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
public class ReplicaEventMessageSerializer implements Serializer
{
    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** The internal entry serializer */
    private transient EntrySerializer entrySerializer;

    /** The LDAP codec used to serialize the entries */
    private transient LdapApiService codec = LdapApiServiceFactory.getSingleton();

    private transient SchemaManager schemaManager;
    
    /**
     * Creates a new instance of ReplicaEventMessageSerializer.
     *
     * @param schemaManager The reference to the global schemaManager
     */
    public ReplicaEventMessageSerializer( SchemaManager schemaManager )
    {
        this.schemaManager = schemaManager;
        entrySerializer = new EntrySerializer( schemaManager );
    }


    /**
     * {@inheritDoc}
     */
    public byte[] serialize( Object object ) throws IOException
    {
        ReplicaEventMessage replicaEventMessage = (ReplicaEventMessage)object;
    
        Entry entry = replicaEventMessage.getEntry();
        ChangeType changeType = replicaEventMessage.getChangeType();
        SyncModifyDn modDnControl = replicaEventMessage.getModDnControl();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        // The entry DN
        entry.getDn().writeExternal( out );
        
        // The entry
        byte[] data = entrySerializer.serialize( entry );

        // Entry's length
        out.writeInt( data.length );
        
        // Entry's data
        out.write( data );

        // The change type
        out.writeByte( changeType.getValue() );
        
        // The moddn control if any (only if it's a MODDN operation)
        if ( changeType == ChangeType.MODDN )
        {
            SyncModifyDnType modDnType = modDnControl.getModDnType();
            out.writeByte( modDnType.getValue() );

            switch ( modDnType )
            {
                case MOVE:
                    out.writeUTF( modDnControl.getNewSuperiorDn() );
                    break;
                   
                case MOVE_AND_RENAME:
                    out.writeUTF( modDnControl.getNewSuperiorDn() );
                    // Fall through

                case RENAME:
                    out.writeUTF( modDnControl.getNewRdn() );
                    out.writeBoolean( modDnControl.isDeleteOldRdn() );
                    break;
            }
        }

        out.flush();

        return baos.toByteArray();
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
            // The Entry's DN
            Dn entryDn = new Dn( schemaManager );
            entryDn.readExternal( in );

            // The Entry's length
            int length = in.readInt();
            byte[] data = new byte[length];
            
            // The entry itself
            in.read( data );
            Entry entry = ( Entry ) entrySerializer.deserialize( data );
            entry.setDn( entryDn );

            // Read the changeType
            byte type = in.readByte();
            ChangeType changeType = ChangeType.getChangeType( type );

            if ( changeType == ChangeType.MODDN )
            {
                type = in.readByte();
                SyncModifyDnType modDnType = SyncModifyDnType.getModifyDnType( type );
                SyncModifyDn modDnControl = new SyncModifyDnDecorator( codec );
                
                modDnControl.setModDnType( modDnType );
                
                switch ( modDnType )
                {
                    case MOVE :
                        modDnControl.setNewSuperiorDn( in.readUTF() );
                        break;

                    case MOVE_AND_RENAME :
                        modDnControl.setNewSuperiorDn( in.readUTF() );
                        // Fallthrough

                    case RENAME :
                        modDnControl.setNewRdn( in.readUTF() );
                        modDnControl.setDeleteOldRdn( in.readBoolean() );
                        break;
                }
                
                // And create a ReplicaEventMessage
                replicaEventMessage = new ReplicaEventMessage( modDnControl, entry );

            }
            else
            {
                // And create a ReplicaEventMessage
                replicaEventMessage = new ReplicaEventMessage( changeType, entry );
            }
        }
        catch ( ClassNotFoundException cnfe )
        {
            // there is nothing we can do here...
        }
        
        return replicaEventMessage;
    }
}
