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


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Comparator;

import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.util.Serialize;
import org.apache.directory.mavibot.btree.serializer.AbstractElementSerializer;
import org.apache.directory.mavibot.btree.serializer.BufferHandler;
import org.apache.directory.mavibot.btree.util.Strings;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.ParentIdAndRdn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Serialize and deserialize a ParentidAndRdn.
 * <br><br>
 * <b>This class must *not* be used outside of the server.</b>
 *  
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MavibotParentIdAndRdnSerializer extends AbstractElementSerializer<ParentIdAndRdn>
{
    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** the logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( MavibotParentIdAndRdnSerializer.class );

    /**
     * Speedup for logs
     */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The schemaManager reference */
    private static SchemaManager schemaManager;

    private static Comparator<ParentIdAndRdn> comparator = new Comparator<ParentIdAndRdn>()
    {

        @Override
        public int compare( ParentIdAndRdn rdn1, ParentIdAndRdn rdn2 )
        {
            return rdn1.compareTo( rdn2 );
        }

    };


    /**
     * Creates a new instance of ParentIdAndRdnSerializer.
     * The schemaManager MUST be set explicitly using the static {@link #setSchemaManager(SchemaManager)}
     */
    public MavibotParentIdAndRdnSerializer()
    {
        super( comparator );
    }


    /**
     * This is the place where we serialize ParentIdAndRdn. The format is the following :<br>
     * <ul>
     * <li>length</li>
     * <li>the RDN</li>
     * <li>the parent ID</li>
     * <li>Number of children</li>
     * <li>Number of descendant</li>
     * <li></li>
     * <li></li>
     * <li></li>
     * </ul>
     */
    public byte[] serialize( ParentIdAndRdn parentIdAndRdn )
    {
        try
        {
            int bufferSize = 1024;

            while ( bufferSize < Integer.MAX_VALUE )
            {
                // allocate a big enough buffer for most of the cases
                byte[] buffer = new byte[bufferSize];

                try
                {
                    // The current position.
                    int pos = 0;

                    // First, the Dn
                    Rdn[] rdns;

                    try
                    {
                        rdns = parentIdAndRdn.getRdns();
                    }
                    catch ( NullPointerException npe )
                    {
                        throw npe;
                    }

                    // Write the Rdn of the Dn
                    // The number of RDN (we may have more than one)
                    if ( ( rdns == null ) || ( rdns.length == 0 ) )
                    {
                        pos = Serialize.serialize( 0, buffer, pos );
                    }
                    else
                    {
                        pos = Serialize.serialize( rdns.length, buffer, pos );

                        for ( Rdn rdn : rdns )
                        {
                            pos = rdn.serialize( buffer, pos );
                        }
                    }

                    // Then the parentId.
                    String parentId = parentIdAndRdn.getParentId();
                    byte[] parentIdBytes = Strings.getBytesUtf8( parentId );
                    pos = Serialize.serialize( parentIdBytes, buffer, pos );

                    // The number of children
                    pos = Serialize.serialize( parentIdAndRdn.getNbChildren(), buffer, pos );

                    // The number of descendants
                    pos = Serialize.serialize( parentIdAndRdn.getNbDescendants(), buffer, pos );

                    if ( IS_DEBUG )
                    {
                        LOG.debug( ">------------------------------------------------" );
                        LOG.debug( "Serialize " + parentIdAndRdn );
                    }


                    // Copy the serialized data
                    byte[] result = new byte[pos];
                    System.arraycopy( buffer, 0, result, 0, pos );

                    return result;
                }
                catch ( ArrayIndexOutOfBoundsException aioobe )
                {
                    // Bad luck, try with a bigger buffer
                    bufferSize += bufferSize;
                }
            }

            // No reason we should reach this point
            throw new RuntimeException();
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }
    }


    /**
     *  Deserialize a ParentIdAndRdn.
     *  
     *  @param bufferHandler The buffer containing the serialized ParentIdAndRdn
     *  @return An instance of a ParentIdAndRdn object 
     *  @throws IOException if we can't deserialize the ParentIdAndRdn
     */
    public ParentIdAndRdn deserialize( BufferHandler bufferHandler ) throws IOException
    {
        return deserialize( ByteBuffer.wrap( bufferHandler.getBuffer() ) );
    }


    @Override
    public ParentIdAndRdn deserialize( ByteBuffer buffer ) throws IOException
    {
        ParentIdAndRdn parentIdAndRdn = fromBytes( buffer.array(), buffer.position() );

        return parentIdAndRdn;
    }


    @Override
    public int compare( ParentIdAndRdn type1, ParentIdAndRdn type2 )
    {
        return type1.compareTo( type2 );
    }


    @Override
    public Comparator<ParentIdAndRdn> getComparator()
    {
        return comparator;
    }


    public static void setSchemaManager( SchemaManager schemaManager )
    {
        MavibotParentIdAndRdnSerializer.schemaManager = schemaManager;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ParentIdAndRdn fromBytes( byte[] buffer ) throws IOException
    {
        return fromBytes( buffer, 0 );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ParentIdAndRdn fromBytes( byte[] buffer, int pos ) throws IOException
    {
        try
        {
            ParentIdAndRdn parentIdAndRdn = new ParentIdAndRdn();

            // Read the number of rdns, if any
            int nbRdns = Serialize.deserializeInt( buffer, pos );
            pos += 4;

            if ( nbRdns == 0 )
            {
                parentIdAndRdn.setRdns( new Rdn[0] );
            }
            else
            {
                Rdn[] rdns = new Rdn[nbRdns];

                for ( int i = 0; i < nbRdns; i++ )
                {
                    Rdn rdn = new Rdn( schemaManager );
                    pos = rdn.deserialize( buffer, pos );
                    rdns[i] = rdn;
                }

                parentIdAndRdn.setRdns( rdns );
            }

            // Read the parent ID
            byte[] uuidBytes = Serialize.deserializeBytes( buffer, pos );
            pos += 4 + uuidBytes.length;
            String uuid = Strings.utf8ToString( uuidBytes );

            parentIdAndRdn.setParentId( uuid );

            // Read the number of children and descendants
            int nbChildren = Serialize.deserializeInt( buffer, pos );
            pos += 4;

            int nbDescendants = Serialize.deserializeInt( buffer, pos );
            pos += 4;

            parentIdAndRdn.setNbChildren( nbChildren );
            parentIdAndRdn.setNbDescendants( nbDescendants );

            return parentIdAndRdn;
        }
        catch ( LdapInvalidAttributeValueException cnfe )
        {
            LOG.error( I18n.err( I18n.ERR_134, cnfe.getLocalizedMessage() ) );
            throw new IOException( cnfe.getLocalizedMessage() );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getType()
    {
        return ParentIdAndRdn.class;
    }
}
