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

package org.apache.directory.server.core.partition.impl.btree.je.serializers;


import java.io.IOException;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LongSerializer implements Serializer<Long>
{

    public static final LongSerializer INSTANCE = new LongSerializer();


    public byte[] serialize( Long obj ) throws IOException
    {
        long n = obj.longValue();

        n = ( n ^ 0x8000000000000000L ); // flip MSB because "long" is signed
        byte[] bites = new byte[8];

        bites[0] = ( byte ) ( n >> 56 );
        bites[1] = ( byte ) ( n >> 48 );
        bites[2] = ( byte ) ( n >> 40 );
        bites[3] = ( byte ) ( n >> 32 );
        bites[4] = ( byte ) ( n >> 24 );
        bites[5] = ( byte ) ( n >> 16 );
        bites[6] = ( byte ) ( n >> 8 );
        bites[7] = ( byte ) n;

        return bites;
    }


    public Long deserialize( byte[] bites ) throws IOException
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

        id = ( id ^ 0x8000000000000000L ); // flip MSB because "long" is signed

        return id;
    }

}
