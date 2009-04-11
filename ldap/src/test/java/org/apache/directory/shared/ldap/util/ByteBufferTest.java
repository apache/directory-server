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
package org.apache.directory.shared.ldap.util;


import org.junit.Test;
import static org.junit.Assert.assertEquals;


/**
 * A test case for a dynamically growing byte[]. 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ByteBufferTest
{
    @Test
    public void testByteBufferGrowth()
    {
        ByteBuffer buf = new ByteBuffer();
        assertEquals( 10, buf.capacity() );
        
        for ( int ii = 0; ii < 20; ii++ )
        {
            buf.append( ii );
            assertEquals( ii, buf.get( ii ) );
            assertEquals( ii, buf.buffer()[ii] );
        }
        
        assertEquals( 20, buf.capacity() );
        buf.append( 20 );
        assertEquals( 30, buf.capacity() );

        // -------------------------------------------------------------------
        
        buf = new ByteBuffer( 5 );
        assertEquals( 5, buf.capacity() );
        
        for ( int ii = 0; ii < 5; ii++ )
        {
            buf.append( ii );
            assertEquals( ii, buf.get( ii ) );
            assertEquals( ii, buf.buffer()[ii] );
        }
        
        assertEquals( 5, buf.capacity() );
        buf.append( 5 );
        assertEquals( 10, buf.capacity() );
    }
    
    public void testCopyOfUsedBytes()
    {
        ByteBuffer buf = new ByteBuffer();
        byte[] bytes = buf.copyOfUsedBytes();
        assertEquals( 0, bytes.length );
        
        for ( int ii = 0; ii < 20; ii++ )
        {
            buf.append( ii );
            assertEquals( ii, buf.get( ii ) );
            assertEquals( ii, buf.buffer()[ii] );
            assertEquals( ii, buf.copyOfUsedBytes()[ii] );
        }
    }
    
    public void testAppendByteArray()
    {
        ByteBuffer buf = new ByteBuffer();
        buf.append( new byte[]{ 0, 1, 2, 3, 4 } );
        for ( int ii = 0; ii < 5; ii++ )
        {
            assertEquals( ii, buf.get( ii ) );
            assertEquals( ii, buf.buffer()[ii] );
            assertEquals( ii, buf.copyOfUsedBytes()[ii] );
        }
    }
}
