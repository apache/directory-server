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
package org.apache.directory.server.core.partition.impl.btree.jdbm;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import jdbm.helper.Serializer;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.ParentIdAndRdn;
import org.apache.directory.shared.ldap.model.name.Rdn;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Serialize and deserialize a ParentidAndRdn.
 * </br></br>
 * <b>This class must *not* be used outside of the server.</b>
 *  
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ParentIdAndRdnSerializer implements Serializer
{
    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** the logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( ParentIdAndRdnSerializer.class );

    /**
     * Speedup for logs
     */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The schemaManager reference */
    private transient SchemaManager schemaManager;


    /**
     * Creates a new instance of ParentIdAndRdnSerializer.
     * 
     * @param schemaManager The reference to the global schemaManager
     */
    public ParentIdAndRdnSerializer( SchemaManager schemaManager )
    {
        this.schemaManager = schemaManager;
    }


    /**
     * <p>
     * 
     * This is the place where we serialize ParentIdAndRdn
     * <p>
     */
    public byte[] serialize( Object object ) throws IOException
    {
        ParentIdAndRdn parentIdAndRdn = ( ParentIdAndRdn ) object;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
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

        return baos.toByteArray();
    }


    /**
     *  Deserialize a ParentIdAndRdn.
     *  
     *  @param bytes the byte array containing the serialized ParentIdAndRdn
     *  @return An instance of a ParentIdAndRdn object 
     *  @throws IOException if we can't deserialize the ParentIdAndRdn
     */
    public Object deserialize( byte[] bytes ) throws IOException
    {
        ObjectInputStream in = new ObjectInputStream( new ByteArrayInputStream( bytes ) );

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

            return parentIdAndRdn;
        }
        catch ( ClassNotFoundException cnfe )
        {
            LOG.error( I18n.err( I18n.ERR_134, cnfe.getLocalizedMessage() ) );
            throw new IOException( cnfe.getLocalizedMessage() );
        }
    }
}
