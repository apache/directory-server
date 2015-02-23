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


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import org.apache.directory.server.core.avltree.ArrayTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jdbm.btree.BTree;
import jdbm.helper.Serializer;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class IndexValueSerializer implements Serializer
{
    private static final long serialVersionUID = 1L;

    /** the flag for a Long value*/
    private static byte LONG_VALUE = 0;

    /** the flag for a AvlTree value*/
    private static byte AVL_TREE_VALUE = 0;

    /** the flag for a BTree value*/
    private static byte BTREE_VALUE = 0;

    /** the logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( IndexValueSerializer.class );


    public Object deserialize( byte[] serialized ) throws IOException
    {
        return null;
    }


    /**
     * Serialize the object. It can be a long, a BTree or an AvlTree
     * 
     * @param obj The object to serialize
     * @return a byte[] containing the serialized value
     * @throws IOException If the serialization failed
     */
    public byte[] serialize( Object obj ) throws IOException
    {
        if ( obj instanceof ArrayTree )
        {
            LOG.debug( "Serializing an AvlTree" );
            return serialize( ( ArrayTree<?> ) obj );
        }
        else if ( obj instanceof BTree )
        {
            LOG.debug( "Serializing a BTree" );
            return serialize( ( BTree ) obj );
        }
        else
        {
            LOG.debug( "Serializing a long [{}]", obj );
            return serialize( ( Long ) obj );
        }
    }


    /**
     * Serialize a Long value
     */
    private byte[] serialize( Long value ) throws IOException
    {
        try ( ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream( baos ) )
        {

            // First, write the type
            out.write( LONG_VALUE );

            // Now, flush the Long 
            out.writeLong( value );

            // And return the result
            out.flush();

            if ( LOG.isDebugEnabled() )
            {
                LOG.debug( ">------------------------------------------------" );
                LOG.debug( "Serializes a LONG value" );
            }

            return baos.toByteArray();
        }
    }


    /**
     * Serialize a BTree value
     */
    private byte[] serialize( BTree bTree ) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        // First, write the type
        out.write( BTREE_VALUE );

        // Marshal the BTree here. 
        // TODO : add the code

        out.flush();

        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( ">------------------------------------------------" );
            LOG.debug( "Serializes an BTree" );
        }

        return baos.toByteArray();
    }


    /**
     * Serialize a AvlTree value
     */
    private byte[] serialize( ArrayTree<?> arrayTree ) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        // First, write the type
        out.write( AVL_TREE_VALUE );

        // Marshal the AvlTree here. 
        // TODO : add the code

        out.flush();

        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( ">------------------------------------------------" );
            LOG.debug( "Serializes an AVL tree" );
        }

        return baos.toByteArray();
    }
}
