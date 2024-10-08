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
package org.apache.directory.server.core.partition.impl.btree.mavibot;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.Comparator;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.mavibot.btree.serializer.AbstractElementSerializer;
import org.apache.directory.mavibot.btree.serializer.BufferHandler;
import org.apache.directory.server.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Serialize and deserialize a ServerEntry. There is a big difference with the standard
 * Entry serialization : we don't serialize the entry's Dn, we just serialize it's Rdn.
 * <br><br>
 * <b>This class must *not* be used outside of the server.</b>
 *  
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MavibotEntrySerializer extends AbstractElementSerializer<Entry>
{
    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** the logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( MavibotEntrySerializer.class );

    /**
     * Speedup for logs
     */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The schemaManager reference */
    private static SchemaManager schemaManager;

    private static final class EntryComparator implements Comparator<Entry>
    {

        @Override
        public int compare( Entry entry1, Entry entry2 )
        {
            return entry1.getDn().getName().compareTo( entry1.getDn().getName() );
        }

    }

    private static Comparator<Entry> comparator = new EntryComparator();


    /**
     * Creates a new instance of ServerEntrySerializer.
     * The schemaManager MUST be set explicitly using the static {@link #setSchemaManager(SchemaManager)}
     */
    public MavibotEntrySerializer()
    {
        super( comparator );
    }


    @Override
    public Comparator<Entry> getComparator()
    {
        return comparator;
    }


    /**
     * <p>
     * 
     * This is the place where we serialize entries, and all theirs
     * elements. the reason why we don't call the underlying methods
     * (<code>ServerAttribute.write(), Value.write()</code>) is that we need
     * access to the registries to read back the values.
     * <p>
     * The structure used to store the entry is the following :
     * <ul>
     *   <li><b>[a byte]</b> : if the Dn is empty 0 will be written else 1</li>
     *   <li><b>[Rdn]</b> : The entry's Rdn.</li>
     *   <li><b>[numberAttr]</b> : the bumber of attributes. Can be 0</li>
     *   <li>For each Attribute :
     *     <ul>
     *       <li><b>[attribute's oid]</b> : The attribute's OID to get back
     *       the attributeType on deserialization</li>
     *       <li><b>[Attribute]</b> The attribute</li>
     *     </ul>
     *   </li>
     * </ul>
     */
    public byte[] serialize( Entry entry )
    {
        try
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            ObjectOutput out = new ObjectOutputStream( baos );

            // First, the Dn
            Dn dn = entry.getDn();

            // Write the Rdn of the Dn
            if ( dn.isEmpty() )
            {
                out.writeByte( 0 );
            }
            else
            {
                out.writeByte( 1 );
                Rdn rdn = dn.getRdn();
                rdn.writeExternal( out );
            }

            // Then the attributes.
            out.writeInt( entry.getAttributes().size() );

            // Iterate through the keys. We store the Attribute
            // here, to be able to restore it in the readExternal :
            // we need access to the registries, which are not available
            // in the ServerAttribute class.
            for ( Attribute attribute : entry.getAttributes() )
            {
                AttributeType attributeType = attribute.getAttributeType();

                // Write the oid to be able to restore the AttributeType when deserializing
                // the attribute
                String oid = attributeType.getOid();

                out.writeUTF( oid );

                // Write the attribute
                attribute.writeExternal( out );
            }

            out.flush();

            // Note : we don't store the ObjectClassAttribute. It has already
            // been stored as an attribute.

            if ( IS_DEBUG )
            {
                LOG.debug( ">------------------------------------------------" );
                LOG.debug( "Serialize {}", entry );
            }

            return baos.toByteArray();
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }
    }


    /**
     *  Deserialize a Entry.
     *  
     *  @param buffer The buffer containing the serialized entry
     *  @return An instance of a Entry object 
     *  @throws IOException if we can't deserialize the Entry
     */
    public Entry deserialize( ByteBuffer buffer ) throws IOException
    {
        // read the length
        int len = buffer.limit();

        ObjectInputStream in = new ObjectInputStream( new ByteArrayInputStream( buffer.array(), buffer.position(), len ) );

        try
        {
            Entry entry = new DefaultEntry( schemaManager );

            // Read the Dn, if any
            byte hasDn = in.readByte();

            if ( hasDn == 1 )
            {
                Rdn rdn = new Rdn( schemaManager );
                rdn.readExternal( in );

                try
                {
                    entry.setDn( new Dn( schemaManager, rdn ) );
                }
                catch ( LdapInvalidDnException lide )
                {
                    IOException ioe = new IOException( lide.getMessage() );
                    ioe.initCause( lide );
                    throw ioe;
                }
            }
            else
            {
                entry.setDn( Dn.EMPTY_DN );
            }

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
                    Attribute attribute = new DefaultAttribute( attributeType );

                    // Read the attribute
                    attribute.readExternal( in );

                    entry.add( attribute );
                }
                catch ( LdapException ne )
                {
                    // We weren't able to find the OID. The attribute will not be added
                    throw new ClassNotFoundException( ne.getMessage(), ne );
                }
            }

            buffer.position( buffer.position() + len ); // previous position + length

            return entry;
        }
        catch ( ClassNotFoundException cnfe )
        {
            LOG.error( I18n.err( I18n.ERR_07000_CANNOT_DESERIALIZE_ENTRY, cnfe.getLocalizedMessage() ) );
            throw new IOException( cnfe.getLocalizedMessage() );
        }
    }


    @Override
    public Entry deserialize( BufferHandler bufferHandler ) throws IOException
    {
        return deserialize( ByteBuffer.wrap( bufferHandler.getBuffer() ) );
    }


    public static void setSchemaManager( SchemaManager schemaManager )
    {
        MavibotEntrySerializer.schemaManager = schemaManager;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Entry fromBytes( byte[] buffer ) throws IOException
    {
        return fromBytes( buffer, 0 );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Entry fromBytes( byte[] buffer, int pos ) throws IOException
    {
        // read the length
        int len = buffer.length - pos;

        ObjectInputStream in = new ObjectInputStream( new ByteArrayInputStream( buffer, pos, len ) );

        try
        {
            Entry entry = new DefaultEntry( schemaManager );

            // Read the Dn, if any
            byte hasDn = in.readByte();

            if ( hasDn == 1 )
            {
                Rdn rdn = new Rdn( schemaManager );
                rdn.readExternal( in );

                try
                {
                    entry.setDn( new Dn( schemaManager, rdn ) );
                }
                catch ( LdapInvalidDnException lide )
                {
                    IOException ioe = new IOException( lide.getMessage() );
                    ioe.initCause( lide );
                    throw ioe;
                }
            }
            else
            {
                entry.setDn( Dn.EMPTY_DN );
            }

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
                    Attribute attribute = new DefaultAttribute( attributeType );

                    // Read the attribute
                    attribute.readExternal( in );

                    entry.add( attribute );
                }
                catch ( LdapException ne )
                {
                    // We weren't able to find the OID. The attribute will not be added
                    throw new ClassNotFoundException( ne.getMessage(), ne );
                }
            }

            return entry;
        }
        catch ( ClassNotFoundException cnfe )
        {
            LOG.error( I18n.err( I18n.ERR_07000_CANNOT_DESERIALIZE_ENTRY, cnfe.getLocalizedMessage() ) );
            throw new IOException( cnfe.getLocalizedMessage() );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getType()
    {
        return Entry.class;
    }
}
