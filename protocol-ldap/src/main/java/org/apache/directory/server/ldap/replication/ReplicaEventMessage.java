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
import org.apache.directory.shared.ldap.codec.api.LdapApiService;
import org.apache.directory.shared.ldap.codec.api.LdapApiServiceFactory;
import org.apache.directory.shared.ldap.extras.controls.SyncModifyDnType;
import org.apache.directory.shared.ldap.extras.controls.syncrepl_impl.SyncModifyDnDecorator;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.DefaultAttribute;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A place holder storing an Entry and the operation applied on it
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ReplicaEventMessage implements Externalizable
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( ReplicaEventMessage.class );

    /** The message event type */
    private EventType eventType;
    
    /** The entry */
    private Entry entry;

    /** The SchemaManager instance */
    private static SchemaManager schemaManager;

    /** The LDAP codec used to serialize the entries */
    private LdapApiService codec = LdapApiServiceFactory.getSingleton();

    /** The modifyDN control */
    private SyncModifyDnDecorator modDnControl;
    

    /**
     * Create a new ReplicaEvent instance for a Add/Delete+Modify operation
     * @param eventType The event type
     * @param entry The entry
     */
    public ReplicaEventMessage( EventType eventType, Entry entry )
    {
        this.eventType = eventType;
        this.entry = entry;
    }


    /**
     * Create a new ReplicaEvent instance for a ModDN operation
     * @param modDnControl The modDN control
     * @param entry The entry
     */
    public ReplicaEventMessage( SyncModifyDnDecorator modDnControl, Entry entry )
    {
        this.modDnControl = modDnControl;
        this.entry = entry;
    }

    
    /**
     * @return The eventType
     */
    public EventType getEventType()
    {
        return eventType;
    }


    /**
     * @return The stored Entry
     */
    public Entry getEntry()
    {
        return entry;
    }


    /**
     * @return The ModDN conrol
     */
    public SyncModifyDnDecorator getModDnControl()
    {
        return modDnControl;
    }

    
    /**
     * {@inheritDoc}
     */
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {

        byte b = in.readByte();
        
        if ( b == 0 ) // handle the SyncModDnControl
        {
            SyncModifyDnType modDnType = SyncModifyDnType.getModifyDnType( in.readShort() );
            
            modDnControl = new SyncModifyDnDecorator( codec );
            modDnControl.setModDnType( modDnType );
            
            modDnControl.setEntryDn( in.readUTF() );
            
            switch ( modDnType )
            {
                case MOVE:
                    modDnControl.setNewSuperiorDn( in.readUTF() );
                    break;
                   
                case RENAME:
                    modDnControl.setNewRdn( in.readUTF() );
                    modDnControl.setDeleteOldRdn( in.readBoolean() );
                    break;
                    
                case MOVEANDRENAME:
                    modDnControl.setNewSuperiorDn( in.readUTF() );
                    modDnControl.setNewRdn( in.readUTF() );
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
        Dn dn = null;
        
        try
        {
            dn = new Dn( schemaManager );
            dn.readExternal( in );
        }
        catch ( ClassNotFoundException cnfe )
        {
            IOException ioe = new IOException( cnfe.getMessage() );
            ioe.initCause( cnfe );
            throw ioe;
        }
        
        entry.setDn( dn );

        // Read the number of attributes
        int nbAttributes = in.readInt();

        // Read the attributes
        for ( int i = 0; i < nbAttributes; i++ )
        {
            // Read the attribute's OID
            String oid = in.readUTF();

            try
            {
                AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( oid );

                // Create the attribute we will read
                DefaultAttribute attribute = new DefaultAttribute( attributeType );

                // Read the attribute
                attribute.readExternal( in );

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


    /**
     * {@inheritDoc}
     */
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        if ( eventType == null )
        {
            out.writeByte( 0 );
            
            SyncModifyDnType modDnType = modDnControl.getModDnType();
            out.writeShort( modDnType.getValue() );
            out.writeUTF( modDnControl.getEntryDn() );
            
            switch ( modDnType )
            {
                case MOVE:
                    out.writeUTF( modDnControl.getNewSuperiorDn() );
                    break;
                   
                case RENAME:
                    out.writeUTF( modDnControl.getNewRdn() );
                    out.writeBoolean( modDnControl.isDeleteOldRdn() );
                    break;
                    
                case MOVEANDRENAME:
                    out.writeUTF( modDnControl.getNewSuperiorDn() );
                    out.writeUTF( modDnControl.getNewRdn() );
                    out.writeBoolean( modDnControl.isDeleteOldRdn() );
            }
        }
        else
        {
            out.writeByte( 1 );
            out.writeShort( eventType.getMask() );
        }

        // then Dn
        entry.getDn().writeExternal( out );

        // Then the attributes.
        out.writeInt( entry.size() );

        // Iterate through the keys. We store the Attribute
        // here, to be able to restore it in the readExternal :
        // we need access to the registries, which are not available
        // in the ServerAttribute class.
        Iterator<Attribute> attrItr = entry.iterator();
        
        while ( attrItr.hasNext() )
        {
            DefaultAttribute attribute = ( DefaultAttribute ) attrItr.next();
            // Write the oid to be able to restore the AttributeType when deserializing
            // the attribute
            out.writeUTF( attribute.getAttributeType().getOid() );

            // Write the attribute
            attribute.writeExternal( out );
        }
    }


    /**
     * Set the SchemaManager 
     * @param schemaManager The SchemaManager instance
     */
    public static void setSchemaManager( SchemaManager schemaManager )
    {
        ReplicaEventMessage.schemaManager = schemaManager;
    }
}
