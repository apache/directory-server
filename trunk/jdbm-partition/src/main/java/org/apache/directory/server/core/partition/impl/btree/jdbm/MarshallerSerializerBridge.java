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


import java.io.IOException;

import jdbm.helper.Serializer;

import org.apache.directory.server.core.avltree.Marshaller;


/**
 * A Marshaller which adapts a JDBM Serializer.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MarshallerSerializerBridge<E> implements Marshaller<E>
{
    /** the wrapped serializer */
    private final Serializer serializer;


    /**
     *
     * @param serializer the JDBM Serializer to wrap
     */
    public MarshallerSerializerBridge( Serializer serializer )
    {
        if ( serializer == null )
        {
            throw new IllegalArgumentException( "serializer" );
        }
        this.serializer = serializer;
    }


    /**
     * @see Marshaller#serialize(Object)
     */
    public byte[] serialize( E object ) throws IOException
    {
        return serializer.serialize( object );
    }


    /**
     * @see Marshaller#deserialize(byte[])
     */
    @SuppressWarnings(
        { "unchecked" })
    public E deserialize( byte[] bytes ) throws IOException
    {
        return ( E ) serializer.deserialize( bytes );
    }
}
