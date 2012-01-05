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
package org.apache.directory.server.core.shared.txn;


import java.io.IOException;

import org.apache.directory.server.core.api.partition.index.Serializer;


/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LongSerializer implements Serializer
{
    public static final LongSerializer INSTANCE = new LongSerializer();


    public byte[] serialize( Object o ) throws IOException
    {
        long id = ( Long ) o;
        byte[] bites = new byte[8];

        bites[0] = ( byte ) ( id >> 56 );
        bites[1] = ( byte ) ( id >> 48 );
        bites[2] = ( byte ) ( id >> 40 );
        bites[3] = ( byte ) ( id >> 32 );
        bites[4] = ( byte ) ( id >> 24 );
        bites[5] = ( byte ) ( id >> 16 );
        bites[6] = ( byte ) ( id >> 8 );
        bites[7] = ( byte ) id;

        return bites;
    }


    public Object deserialize( byte[] bites ) throws IOException
    {
        long id;
        id = bites[0] + ( ( bites[0] < 0 ) ? 256 : 0 );
        id <<= 8;
        id += bites[1] + ( ( bites[1] < 0 ) ? 256 : 0 );
        id <<= 8;
        id += bites[2] + ( ( bites[2] < 0 ) ? 256 : 0 );
        id <<= 8;
        id += bites[3] + ( ( bites[3] < 0 ) ? 256 : 0 );
        id <<= 8;
        id += bites[4] + ( ( bites[4] < 0 ) ? 256 : 0 );
        id <<= 8;
        id += bites[5] + ( ( bites[5] < 0 ) ? 256 : 0 );
        id <<= 8;
        id += bites[6] + ( ( bites[6] < 0 ) ? 256 : 0 );
        id <<= 8;
        id += bites[7] + ( ( bites[7] < 0 ) ? 256 : 0 );
        return id;
    }
}
