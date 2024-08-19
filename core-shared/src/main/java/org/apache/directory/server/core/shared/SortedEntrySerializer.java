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
package org.apache.directory.server.core.shared;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import jdbm.helper.Serializer;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Serialize and deserialize a ServerEntry. 
 * 
 * WARNING: This serializer stores the complete DN as well (unlike other entry 
 *          serializers which store only RDN).
 * 
 * <b>This class must *not* be used anywhere else other than for storing sorted entries in server.</b>
 *  
 *  Note: this was initially used by Mavibot tree, but changed to use in JDBM later.
 *        This will again be ported to Mavibot as soon as it gets ready.
 *        
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SortedEntrySerializer implements Serializer
{
    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** the logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( SortedEntrySerializer.class );

    /**
     * Speedup for logs
     */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The schemaManager reference */
    private static SchemaManager schemaManager;


    /**
     * Creates a new instance of ServerEntrySerializer.
     * The schemaManager MUST be set explicitly set using the static {@link #setSchemaManager(SchemaManager)}
     */
    public SortedEntrySerializer()
    {
    }


    @Override
    public byte[] serialize( Object obj ) throws IOException
    {
        return serialize( ( Entry ) obj );
    }



    @Override
    public Object deserialize( byte[] serialized ) throws IOException
    {
        ObjectInputStream in = new ObjectInputStream( new ByteArrayInputStream( serialized ) );

        try
        {
            Entry entry = new DefaultEntry( schemaManager );

            Dn dn = new Dn( schemaManager );
            dn.readExternal( in );
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
     * <p>
     * 
     * This is the place where we serialize entries, and all theirs
     * elements. the reason why we don't call the underlying methods
     * (<code>ServerAttribute.write(), Value.write()</code>) is that we need
     * access to the registries to read back the values.
     * <p>
     * The structure used to store the entry is the following :
     * <ul>
     *   <li><b>[Dn]</b> : The entry's Rdn.</li>
     *   <li><b>[numberAttr]</b> : the bumber of attributes. Can be 0</li>
     *   <li>For each Attribute :
     *     <ul>
     *       <li><b>[attribute's oid]</b> : The attribute's OID to get back
     *       the attributeType on deserialization</li>
     *       <li><b>[Attribute]</b> The attribute</li>
     *     </ul>
     *   </li>
     * </ul>
     * 
     * @param entry The entry to serialize
     * @return The byte[] containing the serialized entry
     */
    public byte[] serialize( Entry entry )
    {
        try
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            ObjectOutput out = new ObjectOutputStream( baos );

            // First, the Dn
            Dn dn = entry.getDn();

            // Write the Dn
            dn.writeExternal( out );

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


    public static void setSchemaManager( SchemaManager schemaManager )
    {
        SortedEntrySerializer.schemaManager = schemaManager;
    }

}
