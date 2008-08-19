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
package org.apache.directory.server.core.entry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import javax.naming.NamingException;

import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.LdapDNSerializer;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jdbm.helper.Serializer;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ServerEntrySerializer implements Serializer
{
    private static final long serialVersionUID = 1L;

    /** the logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( ServerEntrySerializer.class );

    /**
     * Speedup for logs
     */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The registries reference */
    private transient Registries registries;

    /** Flag used for ServerStringValue */
    private static final transient boolean HR_VALUE = true;

    /** Flag used for streamed values */
    private static final transient boolean STREAMED_VALUE = true;


    /**
     * Creates a new instance of ServerEntrySerializer.
     *
     * @param registries The reference to the global registries
     */
    public ServerEntrySerializer( Registries registries )
    {
        this.registries = registries;
    }


    /**
     * @see Externalizable#writeExternal(ObjectOutput)
     * <p>
     * 
     * This is the place where we serialize entries, and all theirs
     * elements. the reason why we don't call the underlying methods
     * (<code>ServerAttribute.write(), Value.write()</code>) is that we need
     * access to the registries to read back the values.
     * <p>
     * The structure used to store the entry is the following :
     * <li><b>[DN length]</b> : can be -1 if we don't have a DN, 0 if the 
     * DN is empty, otherwise contains the DN's length.<p> 
     * <b>NOTE :</b>This should be unnecessary, as the DN should always exists
     * <p>
     * </li>
     * <li>
     * <b>DN</b> : The entry's DN. Can be empty (rootDSE=<p>
     * </li>
     * We have to store the UPid, and all the values, if any.
     */
    public byte[] serialize( Object object ) throws IOException
    {
        ServerEntry entry = ( ServerEntry ) object;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        // First, the DN
        if ( entry.getDn() == null )
        {
            // Write an empty DN
            LdapDNSerializer.serialize( LdapDN.EMPTY_LDAPDN, out );
        }
        else
        {
            // Write the DN
            LdapDNSerializer.serialize( entry.getDn(), out );
        }

        // Then the attributes.
        out.writeInt( entry.size() );

        // Iterate through the attributes. We store the Attribute
        // here, to be able to restore it in the readExternal :
        // we need access to the registries, which are not available
        // in the ServerAttribute class.
        for ( EntryAttribute attribute : entry )
        {
            // We store the OID, as the AttributeType might have no name
            out.writeUTF( ( ( ServerAttribute ) attribute ).getAttributeType().getOid() );

            // And store the attribute.
            // Store the UP id
            out.writeUTF( attribute.getUpId() );

            // The number of values
            out.writeInt( attribute.size() );

            for ( Value<?> value : attribute )
            {
                try
                {
                    serializeValue( value, out );
                }
                catch ( NamingException ne )
                {
                    // TODO Handle this exception
                }
            }
        }

        // Note : we don't store the ObjectClassAttribute. I has already
        // been stored as an attribute.

        out.flush();

        if ( IS_DEBUG )
        {
            LOG.debug( ">------------------------------------------------" );
            LOG.debug( "Serialize " + entry );
        }

        return baos.toByteArray();
    }


    /**
     * We will write the value and the normalized value, only
     * if the normalized value is different.
     * 
     * The data will be stored following this structure :
     *
     *  [is valid]
     *  [HR flag]
     *  [Streamed flag]
     *  [UP value]
     *  [Norm value] (will be null if normValue == upValue)
     */
    private void serializeValue( Value<?> value, ObjectOutput out ) throws IOException, NamingException
    {
        out.writeBoolean( value.isValid() );

        if ( value instanceof ServerStringValue )
        {
            out.writeBoolean( HR_VALUE );
            out.writeBoolean( !STREAMED_VALUE );
            ServerStringValue ssv = ( ServerStringValue ) value;

            if ( ssv.get() == null )
            {
                // Write two empty string for UP and normalized
                out.writeUTF( "" );
                out.writeUTF( "" );
            }
            else
            {
                // Save the UP value and the normalized value
                out.writeUTF( ssv.get() );
                ssv.normalize();
                out.writeUTF( ssv.getNormalizedValue() );
            }
        }
        else if ( value instanceof ServerBinaryValue )
        {
            out.writeBoolean( !HR_VALUE );
            out.writeBoolean( !STREAMED_VALUE );
            ServerBinaryValue sbv = ( ServerBinaryValue ) value;

            if ( sbv.get() == null )
            {
                out.writeInt( 0 );
                out.writeInt( 0 );
            }
            else
            {
                // Save the UP value and the normalized value if !=
                out.writeInt( sbv.get().length );
                out.write( sbv.get() );

                out.writeBoolean( sbv.isSame() );

                if ( !sbv.isSame() )
                {
                    sbv.normalize();

                    out.writeInt( sbv.getNormalizedValueReference().length );
                    out.write( sbv.getNormalizedValueReference() );
                }
            }
        }

        out.flush();
    }


    /**
     * We will write the value and the normalized value, only
     * if the normalized value is different.
     * 
     * The data will be stored following this structure :
     *
     *  [is valid]
     *  [HR flag]
     *  [Streamed flag]
     *  [UP value]
     *  [Norm value] (will be null if normValue == upValue)
     */
    private Value<?> deserializeValue( ObjectInput in, AttributeType attributeType ) throws IOException
    {
        boolean isValid = in.readBoolean();
        boolean isHR = in.readBoolean();
        boolean isStreamed = in.readBoolean();

        if ( isHR )
        {
            if ( !isStreamed )
            {
                String value = in.readUTF();

                if ( value.length() == 0 )
                {
                    value = null;
                }

                String normalized = in.readUTF();

                if ( normalized.length() == 0 )
                {
                    normalized = null;
                }

                Value<?> ssv = new ServerStringValue( attributeType, value, normalized, isValid );

                return ssv;
            }
            else
            {
                return null;
            }
        }
        else
        {
            if ( !isStreamed )
            {
                int length = in.readInt();

                byte[] value = new byte[length];

                if ( length != 0 )
                {
                    in.read( value );
                }

                byte[] normalized = null;
                boolean same = in.readBoolean();

                // Now, if the normalized value is different from the wrapped value,
                // read the normalized value.
                if ( !same )
                {
                    length = in.readInt();

                    normalized = new byte[length];
                    if ( length != 0 )
                    {
                        in.read( normalized );
                    }
                }
                else
                {
                    normalized = value;
                }

                Value<?> sbv = new ServerBinaryValue( attributeType, value, normalized, same, isValid );

                return sbv;
            }
            else
            {
                return null;
            }
        }
    }


    /**
     *  Deserialize a ServerEntry
     */
    public Object deserialize( byte[] bytes ) throws IOException
    {
        ObjectInputStream in = new ObjectInputStream( new ByteArrayInputStream( bytes ) );

        try
        {
            // First, read the DN
            LdapDN dn = LdapDNSerializer.deserialize( in );

            // Read the number of attributes
            int nbAttrs = in.readInt();

            ServerEntry serverEntry = new DefaultServerEntry( registries, dn );

            // Read all the attributes
            for ( int i = 0; i < nbAttrs; i++ )
            {
                // The oid
                String oid = in.readUTF();

                AttributeType attributeType = registries.getAttributeTypeRegistry().lookup( oid );

                // The UP id
                String upId = in.readUTF();

                // The number of values
                int nbValues = in.readInt();

                ServerAttribute serverAttribute = new DefaultServerAttribute( upId, attributeType );

                for ( int j = 0; j < nbValues; j++ )
                {
                    Value<?> value = deserializeValue( in, attributeType );
                    serverAttribute.add( value );
                }

                serverEntry.put( serverAttribute );
            }

            if ( IS_DEBUG )
            {
                LOG.debug( "<------------------------------------------------" );
                LOG.debug( "Deserialize " + serverEntry );
            }

            return serverEntry;
        }
        catch ( NamingException ne )
        {
            // TODO Handle this exception
            return null;
        }
    }
}
