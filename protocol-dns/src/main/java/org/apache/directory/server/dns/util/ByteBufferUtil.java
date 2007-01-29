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

package org.apache.directory.server.dns.util;


import java.nio.ByteBuffer;


public class ByteBufferUtil
{

    private ByteBufferUtil()
    {
    }


    public static short getUnsignedByte( ByteBuffer byteBuffer )
    {
        return ( ( short ) ( byteBuffer.get() & 0xff ) );
    }


    public static short getUnsignedByte( ByteBuffer byteBuffer, int position )
    {
        return ( ( short ) ( byteBuffer.get( position ) & ( short ) 0xff ) );
    }


    public static int getUnsignedShort( ByteBuffer byteBuffer )
    {
        return ( byteBuffer.getShort() & 0xffff );
    }


    public static int getUnsignedShort( ByteBuffer byteBuffer, int position )
    {
        return ( byteBuffer.getShort( position ) & 0xffff );
    }


    public static long getUnsignedInt( ByteBuffer byteBuffer )
    {
        return ( byteBuffer.getInt() & 0xffffffffL );
    }


    public static long getUnsignedInt( ByteBuffer byteBuffer, int position )
    {
        return ( byteBuffer.getInt( position ) & 0xffffffffL );
    }


    public static void putUnsignedByte( ByteBuffer byteBuffer, int value )
    {
        byteBuffer.put( ( byte ) ( value & 0xff ) );
    }


    public static void putUnsignedByte( ByteBuffer byteBuffer, int position, int value )
    {
        byteBuffer.put( position, ( byte ) ( value & 0xff ) );
    }


    public static void putUnsignedShort( ByteBuffer byteBuffer, int value )
    {
        byteBuffer.putShort( ( short ) ( value & 0xffff ) );
    }


    public static void putUnsignedShort( ByteBuffer byteBuffer, int position, int value )
    {
        byteBuffer.putShort( position, ( short ) ( value & 0xffff ) );
    }


    public static void putUnsignedInt( ByteBuffer byteBuffer, long value )
    {
        byteBuffer.putInt( ( int ) ( value & 0xffffffffL ) );
    }


    public static void putUnsignedInt( ByteBuffer byteBuffer, int position, long value )
    {
        byteBuffer.putInt( position, ( int ) ( value & 0xffffffffL ) );
    }
}
