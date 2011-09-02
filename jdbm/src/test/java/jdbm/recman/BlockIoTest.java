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
package jdbm.recman;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

public class BlockIoTest
{
    BlockIo blockIo;
    BlockIo blockIoData;
    
    @Before
    public void init()
    {
        blockIo = new BlockIo( 0L, new byte[1024] );
        
        // Init the blockIo with 1024 bytes from 0x00 to 0xFF, 4 times
        for ( int i = 0; i < 1024; i++ )
        {
            blockIo.writeByte( i, (byte)( i & 0x00fff ) );
        }
        
        blockIoData = new BlockIo( 0L, new byte[1024] );
        blockIoData.writeLong( 0, 0x8081828384858687L );
        blockIoData.writeInt( 8, 0x000000ff );
        
        for ( int i = 0; i < 256; i++ )
        {
            blockIoData.writeByte( 12+i, (byte)0x80 );
        }
    }

    
    @Test
    public void testReadByte()
    {
        assertEquals( (byte)0x00, blockIo.readByte( 0 ) );
        assertEquals( (byte)0xff, blockIo.readByte( 1023 ) );
        
        try
        {
            blockIo.readByte( -1 );
            fail();
        }
        catch ( ArrayIndexOutOfBoundsException aioobe )
        {
            // Expected
        }
        
        try
        {
            blockIo.readByte( 1024 );
            fail();
        }
        catch ( ArrayIndexOutOfBoundsException aioobe )
        {
            // Expected
        }
    }

    
    @Test
    public void testReadInt()
    {
        assertEquals( 0x00010203, blockIo.readInt( 0 ) );
        assertEquals( 0x7c7d7e7f, blockIo.readInt( 124 ) );
        assertEquals( 0x7d7e7f80, blockIo.readInt( 125 ) );
        assertEquals( 0x7e7f8081, blockIo.readInt( 126 ) );
        assertEquals( 0x7f808182, blockIo.readInt( 127 ) );
        assertEquals( 0x80818283, blockIo.readInt( 128 ) );
        assertEquals( 0xfbfcfdfe, blockIo.readInt( 1019 ) );
        assertEquals( 0xfcfdfeff, blockIo.readInt( 1020 ) );
        
        try
        {
            blockIo.readInt( -1 );
            fail();
        }
        catch ( ArrayIndexOutOfBoundsException aioobe )
        {
            // Expected
        }
        
        try
        {
            blockIo.readInt( 1021 );
            fail();
        }
        catch ( ArrayIndexOutOfBoundsException aioobe )
        {
            // Expected
        }
        
        try
        {
            blockIo.readInt( 1024 );
            fail();
        }
        catch ( ArrayIndexOutOfBoundsException aioobe )
        {
            // Expected
        }
    }

    
    @Test
    public void testReadShort()
    {
        assertEquals( 0x0001, blockIo.readShort( 0 ) );
        assertEquals( 0x7c7d, blockIo.readShort( 124 ) );
        assertEquals( 0x7d7e, blockIo.readShort( 125 ) );
        assertEquals( 0x7e7f, blockIo.readShort( 126 ) );
        assertEquals( 0x7f80, blockIo.readShort( 127 ) );
        assertEquals( (short)0x8081, blockIo.readShort( 128 ) );
        assertEquals( (short)0xfdfe, blockIo.readShort( 1021 ) );
        assertEquals( (short)0xfeff, blockIo.readShort( 1022 ) );
        
        try
        {
            blockIo.readShort( -1 );
            fail();
        }
        catch ( ArrayIndexOutOfBoundsException aioobe )
        {
            // Expected
        }
        
        try
        {
            blockIo.readShort( 1023 );
            fail();
        }
        catch ( ArrayIndexOutOfBoundsException aioobe )
        {
            // Expected
        }
        
        try
        {
            blockIo.readShort( 1024 );
            fail();
        }
        catch ( ArrayIndexOutOfBoundsException aioobe )
        {
            // Expected
        }
    }

    
    @Test
    public void testReadLong()
    {
        assertEquals( 0x0001020304050607L, blockIo.readLong( 0 ) );
        assertEquals( 0x78797a7b7c7d7e7fL, blockIo.readLong( 120 ) );
        assertEquals( 0x797a7b7c7d7e7f80L, blockIo.readLong( 121 ) );
        assertEquals( 0x7a7b7c7d7e7f8081L, blockIo.readLong( 122 ) );
        assertEquals( 0x7b7c7d7e7f808182L, blockIo.readLong( 123 ) );
        assertEquals( 0x7c7d7e7f80818283L, blockIo.readLong( 124 ) );
        assertEquals( 0x7d7e7F8081828384L, blockIo.readLong( 125 ) );
        assertEquals( 0x7e7f808182838485L, blockIo.readLong( 126 ) );
        assertEquals( 0x7f80818283848586L, blockIo.readLong( 127 ) );
        assertEquals( 0x8081828384858687L, blockIo.readLong( 128 ) );
        assertEquals( 0xf7f8f9fafbfcfdfeL, blockIo.readLong( 1015 ) );
        assertEquals( 0xf8f9fafbfcfdfeffL, blockIo.readLong( 1016 ) );
        
        try
        {
            blockIo.readLong( -1 );
            fail();
        }
        catch ( ArrayIndexOutOfBoundsException aioobe )
        {
            // Expected
        }
        
        try
        {
            blockIo.readLong( 1017 );
            fail();
        }
        catch ( ArrayIndexOutOfBoundsException aioobe )
        {
            // Expected
        }
        
        try
        {
            blockIo.readLong( 1024 );
            fail();
        }
        catch ( ArrayIndexOutOfBoundsException aioobe )
        {
            // Expected
        }
    }
}
