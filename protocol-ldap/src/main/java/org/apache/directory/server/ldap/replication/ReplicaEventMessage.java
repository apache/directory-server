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


import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Iterator;

import org.apache.directory.server.core.event.EventType;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.codec.controls.replication.syncmodifydn.SyncModifyDnControl;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.EntryAttribute;
import org.apache.directory.shared.ldap.message.control.replication.SyncModifyDnType;
import org.apache.directory.shared.ldap.model.entry.DefaultEntryAttribute;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.name.Dn;
import org.apache.directory.shared.ldap.name.DnSerializer;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.util.Unicode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * TODO ReplicaEventMessage.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ReplicaEventMessage implements Externalizable
{
    private EventType eventType;
    private Entry entry;

    private SyncModifyDnControl modDnControl;
    
    private static final Logger LOG = LoggerFactory.getLogger( ReplicaEventMessage.class );

    private static SchemaManager schemaManager;


    public ReplicaEventMessage()
    {
        // used by deserializer
    }


    public ReplicaEventMessage( EventType eventType, Entry entry )
    {
        this.eventType = eventType;
        this.entry = entry;
    }


    public ReplicaEventMessage( SyncModifyDnControl modDnControl, Entry entry )
    {
        this.modDnControl = modDnControl;
        this.entry = entry;
    }

    
    public EventType getEventType()
    {
        return eventType;
    }


    public Entry getEntry()
    {
        return entry;
    }


    public SyncModifyDnControl getModDnControl()
    {
        return modDnControl;
    }

    
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {

        byte b = in.readByte();
        if( b == 0 ) // handle the SyncModDnControl
        {
            SyncModifyDnType modDnType = SyncModifyDnType.getModifyDnType( in.readShort() );
            
            modDnControl = new SyncModifyDnControl();
            modDnControl.setModDnType( modDnType );
            
            modDnControl.setEntryDn( Unicode.readUTF(in) );
            
            switch( modDnType )
            {
                case MOVE:
                    modDnControl.setNewSuperiorDn( Unicode.readUTF(in) );
                    break;
                   
                case RENAME:
                    modDnControl.setNewRdn( Unicode.readUTF(in) );
                    modDnControl.setDeleteOldRdn( in.readBoolean() );
                    break;
                    
                case MOVEANDRENAME:
                    modDnControl.setNewSuperiorDn( Unicode.readUTF(in) );
                    modDnControl.setNewRdn( Unicode.readUTF(in) );
                    modDnControl.setDeleteOldRdn( in.readBoolean() );
            }
        }
        else // read the event type
        {
            eventType = EventType.getType( in.readShort() );
        }

        // initialize the entry
        entry = new DefaultEntry( schemaManager );

        // Read the Dn
        Dn dn = DnSerializer.deserialize( in );
        entry.setDn( dn );

        // Read the number of attributes
        int nbAttributes = in.readInt();

        // Read the attributes
        for ( int i = 0; i < nbAttributes; i++ )
        {
            // Read the attribute's OID
            String oid = Unicode.readUTF(in);

            try
            {
                AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( oid );

                // Create the attribute we will read
                DefaultEntryAttribute attribute = new DefaultEntryAttribute( attributeType );

                // Read the attribute
                attribute.deserialize( in );

                entry.add( attribute );
            }
            catch ( Exception ne )
            {
                entry = null;
                // We weren't able to find the OID. The attribute will not be added
                LOG.warn( I18n.err( I18n.ERR_04470, oid ) );
            }
        }
    }


    public void writeExternal( ObjectOutput out ) throws IOException
    {
        if( eventType == null )
        {
            out.writeByte( 0 );
            
            SyncModifyDnType modDnType = modDnControl.getModDnType();
            out.writeShort( modDnType.getValue() );
            Unicode.writeUTF(out, modDnControl.getEntryDn());
            
            switch( modDnType )
            {
                case MOVE:
                    Unicode.writeUTF(out, modDnControl.getNewSuperiorDn());
                    break;
                   
                case RENAME:
                    Unicode.writeUTF(out, modDnControl.getNewRdn());
                    out.writeBoolean( modDnControl.isDeleteOldRdn() );
                    break;
                    
                case MOVEANDRENAME:
                    Unicode.writeUTF(out, modDnControl.getNewSuperiorDn());
                    Unicode.writeUTF(out, modDnControl.getNewRdn());
                    out.writeBoolean( modDnControl.isDeleteOldRdn() );
            }
        }
        else
        {
            out.writeByte( 1 );
            out.writeShort( eventType.getMask() );
        }

        // then Dn
        DnSerializer.serialize( entry.getDn(), out );

        // Then the attributes.
        out.writeInt( entry.size() );

        // Iterate through the keys. We store the Attribute
        // here, to be able to restore it in the readExternal :
        // we need access to the registries, which are not available
        // in the ServerAttribute class.
        Iterator<EntryAttribute> attrItr = entry.iterator();
        while ( attrItr.hasNext() )
        {
            DefaultEntryAttribute attribute = ( DefaultEntryAttribute ) attrItr.next();
            // Write the oid to be able to restore the AttributeType when deserializing
            // the attribute
            Unicode.writeUTF(out, attribute.getAttributeType().getOid());

            // Write the attribute
            attribute.serialize( out );
        }
    }


    public static void setSchemaManager( SchemaManager schemaManager )
    {
        ReplicaEventMessage.schemaManager = schemaManager;
    }

}
