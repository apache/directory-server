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


public class IntegerKeyMarshaller implements Marshaller<Integer>
{

    public byte[] serialize( Integer i )
    {
        int y = i.intValue();
        byte[] data = new byte[4];
        data[0] = ( byte ) ( ( y & 0xFF ) >>> 24 );
        data[1] = ( byte ) ( ( y & 0xFF ) >>> 16 );
        data[2] = ( byte ) ( ( y & 0xFF ) >>> 8 );
        data[3] = ( byte ) ( y & 0xFF );

        return data;
    }


    public Integer deserialize( byte[] data )
    {
        if ( data == null || data.length == 0 )
        {
            return null;
        }

        return ( ( data[0] & 0xFF ) << 24 )
            | ( ( data[1] & 0xFF ) << 16 )
            | ( ( data[2] & 0xFF ) << 8 )
            | ( data[3] & 0xFF );
    }
}
