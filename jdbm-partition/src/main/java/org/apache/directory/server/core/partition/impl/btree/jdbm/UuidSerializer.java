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
import java.util.UUID;

import jdbm.helper.Serializer;


/**
 * A {@link Serializer} for Longs
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class UuidSerializer implements Serializer
{
    private static final long serialVersionUID = 237756689544852128L;
    public static final UuidSerializer INSTANCE = new UuidSerializer();


    public byte[] serialize( Object o ) throws IOException
    {
        UUID uuid = ( UUID ) o;
        byte[] bites = new byte[16];

        long id = uuid.getMostSignificantBits();

        bites[0] = ( byte ) ( id >> 56 );
        bites[1] = ( byte ) ( id >> 48 );
        bites[2] = ( byte ) ( id >> 40 );
        bites[3] = ( byte ) ( id >> 32 );
        bites[4] = ( byte ) ( id >> 24 );
        bites[5] = ( byte ) ( id >> 16 );
        bites[6] = ( byte ) ( id >> 8 );
        bites[7] = ( byte ) id;

        id = uuid.getLeastSignificantBits();

        bites[8] = ( byte ) ( id >> 56 );
        bites[9] = ( byte ) ( id >> 48 );
        bites[10] = ( byte ) ( id >> 40 );
        bites[11] = ( byte ) ( id >> 32 );
        bites[12] = ( byte ) ( id >> 24 );
        bites[13] = ( byte ) ( id >> 16 );
        bites[14] = ( byte ) ( id >> 8 );
        bites[15] = ( byte ) id;

        return bites;
    }


    public Object deserialize( byte[] bites ) throws IOException
    {
        long msb;
        msb = bites[0] + ( ( bites[0] < 0 ) ? 256 : 0 );
        msb <<= 8;
        msb += bites[1] + ( ( bites[1] < 0 ) ? 256 : 0 );
        msb <<= 8;
        msb += bites[2] + ( ( bites[2] < 0 ) ? 256 : 0 );
        msb <<= 8;
        msb += bites[3] + ( ( bites[3] < 0 ) ? 256 : 0 );
        msb <<= 8;
        msb += bites[4] + ( ( bites[4] < 0 ) ? 256 : 0 );
        msb <<= 8;
        msb += bites[5] + ( ( bites[5] < 0 ) ? 256 : 0 );
        msb <<= 8;
        msb += bites[6] + ( ( bites[6] < 0 ) ? 256 : 0 );
        msb <<= 8;
        msb += bites[7] + ( ( bites[7] < 0 ) ? 256 : 0 );

        long lsb;
        lsb = bites[8] + ( ( bites[8] < 0 ) ? 256 : 0 );
        lsb <<= 8;
        lsb += bites[9] + ( ( bites[9] < 0 ) ? 256 : 0 );
        lsb <<= 8;
        lsb += bites[10] + ( ( bites[10] < 0 ) ? 256 : 0 );
        lsb <<= 8;
        lsb += bites[11] + ( ( bites[11] < 0 ) ? 256 : 0 );
        lsb <<= 8;
        lsb += bites[12] + ( ( bites[12] < 0 ) ? 256 : 0 );
        lsb <<= 8;
        lsb += bites[13] + ( ( bites[13] < 0 ) ? 256 : 0 );
        lsb <<= 8;
        lsb += bites[14] + ( ( bites[14] < 0 ) ? 256 : 0 );
        lsb <<= 8;
        lsb += bites[15] + ( ( bites[15] < 0 ) ? 256 : 0 );

        return new UUID( msb, lsb );
    }
}
