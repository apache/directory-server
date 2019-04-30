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
package org.apache.directory.server.core.avltree;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Comparator;

import org.apache.directory.server.i18n.I18n;


/**
 * Class to serialize the AvlTree node data.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@SuppressWarnings("unchecked")
public class AvlTreeMarshaller<E> implements Marshaller<AvlTree<E>>
{
    /** used for serialized form of an empty AvlTree */
    private static final byte[] EMPTY_TREE = new byte[1];

    /** marshaller to be used for marshalling the keys */
    private Marshaller<E> keyMarshaller;

    /** key Comparator for the AvlTree */
    private Comparator<E> comparator;


    /**
     * Creates a new instance of AvlTreeMarshaller with a custom key
     * Marshaller.
     *
     * @param comparator Comparator to be used for key comparision
     * @param keyMarshaller marshaller for keys
     */
    public AvlTreeMarshaller( Comparator<E> comparator, Marshaller<E> keyMarshaller )
    {
        this.comparator = comparator;
        this.keyMarshaller = keyMarshaller;
    }


    /**
     * Creates a new instance of AvlTreeMarshaller with the default key
     * Marshaller which uses Java Serialization.
     *
     * @param comparator Comparator to be used for key comparision
     */
    public AvlTreeMarshaller( Comparator<E> comparator )
    {
        this.comparator = comparator;
        this.keyMarshaller = ( Marshaller<E> ) DefaultMarshaller.INSTANCE;
    }


    /**
     * Marshals the given tree to bytes
     * @param tree the tree to be marshalled
     */
    public byte[] serialize( AvlTree<E> tree )
    {
        if ( tree.isEmpty() )
        {
            return EMPTY_TREE;
        }

        LinkedAvlNode<E> x = tree.getFirst().next;

        while ( x != null )
        {
            x.setIndex( x.previous.getIndex() + 1 );
            x = x.next;
        }

        byte[] data = null;

        try ( ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream( byteStream ) )
        {
            out.writeByte( 0 ); // represents the start of AvlTree byte stream
            out.writeInt( tree.getSize() );
            writeTree( tree.getRoot(), out );
            out.flush();
            data = byteStream.toByteArray();
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }

        return data;
    }


    /**
     * writes the content of the AVLTree to an output stream.
     * The current format is 
     *  
     *  AvlTree = [0(zero-byte-value)][node] // the '0' (zero) is to distinguish AvlTree from BTreeRedirect which starts with 1 (one)
     *   node = [size] [data-length] [data] [index] [child-marker] [node] [child-marker] [node]
     *
     * @param node the node to be marshalled to bytes
     * @param out OutputStream
     * @throws IOException on write failures of serialized tree to stream
     */
    private void writeTree( LinkedAvlNode<E> node, DataOutputStream out ) throws IOException
    {
        byte[] data = keyMarshaller.serialize( node.getKey() );

        out.writeInt( data.length ); // data-length
        out.write( data ); // data
        out.writeInt( node.getIndex() ); // index

        if ( node.getLeft() != null )
        {
            out.writeInt( 2 ); // left
            writeTree( node.getLeft(), out );
        }
        else
        {
            out.writeInt( 0 );
        }

        if ( node.getRight() != null )
        {
            out.writeInt( 4 ); // right
            writeTree( node.getRight(), out );
        }
        else
        {
            out.writeInt( 0 );
        }

    }


    /**
     * Creates an AVLTree from given bytes of data.
     * 
     * @param data byte array to be converted into AVLTree  
     */
    public AvlTree<E> deserialize( byte[] data ) throws IOException
    {
        if ( data == null || data.length == 0 )
        {
            throw new IOException( I18n.err( I18n.ERR_439 ) );
        }

        if ( data.length == 1 && data[0] == 0 )
        {
            return new AvlTreeImpl<>( comparator );
        }

        ByteArrayInputStream bin = new ByteArrayInputStream( data );
        DataInputStream din = new DataInputStream( bin );

        byte startByte = din.readByte();

        if ( startByte != 0 )
        {
            throw new IOException( I18n.err( I18n.ERR_443 ) );
        }

        int size = din.readInt();

        LinkedAvlNode[] nodes = new LinkedAvlNode[size];
        LinkedAvlNode<E> root = readTree( din, nodes );

        AvlTreeImpl<E> tree = new AvlTreeImpl<>( comparator );

        tree.setRoot( root );

        tree.setFirst( nodes[0] );

        // Update the size
        tree.setSize( size );

        if ( nodes.length >= 1 )
        {
            tree.setLast( nodes[nodes.length - 1] );
        }

        for ( int i = 0; i < nodes.length - 1; i++ )
        {
            nodes[i].setNext( nodes[i + 1] );
            nodes[i + 1].setPrevious( nodes[i] );
        }

        return tree;
    }


    /**
     * Reads the data from given InputStream and creates the LinkedAvlNodes to
     * form the tree node = [size] [data-length] [data] [index] [child-marker]
     * [node] [child-marker] [node].
     *
     * @param in the input stream to deserialize from
     * @param nodes the deserialized nodes
     * @return the deserialized AvlTree node
     * @throws IOException on failures to deserialize or read from the stream
     */
    public LinkedAvlNode<E> readTree( DataInputStream in, LinkedAvlNode[] nodes )
        throws IOException
    {
        int dLen = in.readInt();

        byte[] data = new byte[dLen];

        //noinspection ResultOfMethodCallIgnored
        in.readFully( data );

        E key = keyMarshaller.deserialize( data );
        LinkedAvlNode<E> node = new LinkedAvlNode<>( key );

        int index = in.readInt();
        nodes[index] = node;

        int childMarker = in.readInt();

        if ( childMarker == 2 )
        {
            node.setLeft( readTree( in, nodes ) );
        }

        childMarker = in.readInt();

        if ( childMarker == 4 )
        {
            node.setRight( readTree( in, nodes ) );
        }

        return node;
    }
}
