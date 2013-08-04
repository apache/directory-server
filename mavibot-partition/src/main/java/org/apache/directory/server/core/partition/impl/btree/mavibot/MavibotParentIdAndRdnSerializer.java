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

import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.ParentIdAndRdn;
import org.apache.directory.mavibot.btree.serializer.BufferHandler;
import org.apache.directory.mavibot.btree.serializer.ElementSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Serialize and deserialize a ParentidAndRdn.
 * </br></br>
 * <b>This class must *not* be used outside of the server.</b>
 *  
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MavibotParentIdAndRdnSerializer implements ElementSerializer<ParentIdAndRdn>
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

    private Comparator<ParentIdAndRdn> comparator = new Comparator<ParentIdAndRdn>()
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
    }


    /**
     * <p>
     * 
     * This is the place where we serialize ParentIdAndRdn
     * <p>
     */
    public byte[] serialize( ParentIdAndRdn parentIdAndRdn )
    {
        try
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            int totalBytes = 0;
            // write dummy length for preserving the space for 'length' to be filled later
            baos.write( 0 );
            baos.write( 0 );
            baos.write( 0 );
            baos.write( 0 );
            
            ObjectOutput out = new ObjectOutputStream( baos );
            
            
            // First, the Dn
            Rdn[] rdns = parentIdAndRdn.getRdns();
            
            // Write the Rdn of the Dn
            if ( ( rdns == null ) || ( rdns.length == 0 ) )
            {
                out.writeByte( 0 );
            }
            else
            {
                out.writeByte( rdns.length );
                
                for ( Rdn rdn : rdns )
                {
                    rdn.writeExternal( out );
                }
            }
            
            // Then the parentId.
            out.writeUTF( parentIdAndRdn.getParentId() );
            
            // The number of children
            out.writeInt( parentIdAndRdn.getNbChildren() );
            
            // The number of descendants
            out.writeInt( parentIdAndRdn.getNbDescendants() );
            
            out.flush();
            
            if ( IS_DEBUG )
            {
                LOG.debug( ">------------------------------------------------" );
                LOG.debug( "Serialize " + parentIdAndRdn );
            }
            
            byte[] bytes = baos.toByteArray();
            
            totalBytes = bytes.length - 4; //subtract the first 4 dummy bytes
            
            // replace the dummy length with the actual length
            bytes[0] = ( byte ) ( totalBytes >>> 24 );
            bytes[1] = ( byte ) ( totalBytes >>> 16 );
            bytes[2] = ( byte ) ( totalBytes >>> 8 );
            bytes[3] = ( byte ) ( totalBytes );
            
            return bytes;
        }
        catch( Exception e )
        {
            throw new RuntimeException( e );
        }
    }


    /**
     *  Deserialize a ParentIdAndRdn.
     *  
     *  @param bytes the byte array containing the serialized ParentIdAndRdn
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
        int len = buffer.getInt();

        ObjectInputStream in = new ObjectInputStream( new ByteArrayInputStream( buffer.array(), buffer.position(), len ) );

        try
        {
            ParentIdAndRdn parentIdAndRdn = new ParentIdAndRdn();

            // Read the number of rdns, if any
            byte nbRdns = in.readByte();

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
                    rdn.readExternal( in );
                    rdns[i] = rdn;
                }

                parentIdAndRdn.setRdns( rdns );
            }

            // Read the parent ID
            String uuid = in.readUTF();

            parentIdAndRdn.setParentId( uuid );

            // Read the nulber of children and descendants
            int nbChildren = in.readInt();
            int nbDescendants = in.readInt();

            parentIdAndRdn.setNbChildren( nbChildren );
            parentIdAndRdn.setNbDescendants( nbDescendants );

            buffer.position( buffer.position() + len );
            
            return parentIdAndRdn;
        }
        catch ( ClassNotFoundException cnfe )
        {
            LOG.error( I18n.err( I18n.ERR_134, cnfe.getLocalizedMessage() ) );
            throw new IOException( cnfe.getLocalizedMessage() );
        }        
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

}
