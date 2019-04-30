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

import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.comparators.DnComparator;
import org.apache.directory.mavibot.btree.serializer.AbstractElementSerializer;
import org.apache.directory.mavibot.btree.serializer.BufferHandler;
import org.apache.directory.server.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Serialize and deserialize a Dn.
 * <br><br>
 * <b>This class must *not* be used outside of the server.</b>
 *  
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DnSerializer extends AbstractElementSerializer<Dn>
{
    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** the logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( DnSerializer.class );

    /**
     * Speedup for logs
     */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    private static Comparator<Dn> comp = new Comparator<Dn>()
    {
        DnComparator comparator = new DnComparator( null );
        
        @Override
        public int compare( Dn dn1, Dn dn2 )
        {
            return comparator.compare( dn1,  dn2 );
        }
    };


    /**
     * Creates a new instance of DnSerializer.
     */
    public DnSerializer()
    {
        super( comp );
    }


    /**
     * This is the place where we serialize Dn
     * 
     * @param dn The Dn to serialize
     * @return The byte[] containing the serialized Dn
     */
    public byte[] serialize( Dn dn )
    {
        try
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream( baos );

            // First, the Dn
            dn.writeExternal( out );

            out.flush();

            if ( IS_DEBUG )
            {
                LOG.debug( ">------------------------------------------------" );
                LOG.debug( "Serialized {}", dn );
            }

            return baos.toByteArray();
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }


    /**
     *  Deserialize a Dn.
     *  
     *  @param buffer the buffer containing the serialized Dn
     *  @return An instance of a Dn object 
     *  @throws IOException if we can't deserialize the Dn
     */
    @Override
    public Dn deserialize( ByteBuffer buffer ) throws IOException
    {
        return deserialize( new BufferHandler( buffer.array() ) );
    }


    @Override
    public Dn deserialize( BufferHandler bufferHandler ) throws IOException
    {
        ObjectInputStream in = new ObjectInputStream( new ByteArrayInputStream( bufferHandler.getBuffer() ) );

        try
        {
            Dn dn = new Dn();

            dn.readExternal( in );

            return dn;
        }
        catch ( ClassNotFoundException cnfe )
        {
            LOG.error( I18n.err( I18n.ERR_134, cnfe.getLocalizedMessage() ) );
            throw new IOException( cnfe.getLocalizedMessage() );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Dn fromBytes( byte[] buffer ) throws IOException
    {
        return fromBytes( buffer, 0 );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Dn fromBytes( byte[] buffer, int pos ) throws IOException
    {
        int length = buffer.length - pos;
        ObjectInputStream in = new ObjectInputStream( new ByteArrayInputStream( buffer, pos, length ) );

        try
        {
            Dn dn = new Dn();

            dn.readExternal( in );

            return dn;
        }
        catch ( ClassNotFoundException cnfe )
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
        return Dn.class;
    }
}
