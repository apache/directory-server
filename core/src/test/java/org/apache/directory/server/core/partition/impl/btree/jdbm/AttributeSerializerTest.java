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

import javax.naming.NamingException;
import javax.naming.directory.Attribute;

import org.apache.directory.shared.ldap.message.LockableAttributeImpl;
import org.apache.directory.shared.ldap.util.ArrayUtils;

import junit.framework.TestCase;


/**
 * Tests the {@link AttributeSerializer}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AttributeSerializerTest extends TestCase
{
    public void testGetLengthBytes()
    {
        // test first 10 values
        for ( int ii = 0; ii < 10; ii++ )
        {
            byte[] bites = AttributeSerializer.getLengthBytes( ii );
            int deserialized = AttributeSerializer.getLength( bites );
            assertEquals( ii, deserialized );
        }

        // test first byte boundry
        for ( int ii = 250; ii < 260; ii++ )
        {
            byte[] bites = AttributeSerializer.getLengthBytes( ii );
            int deserialized = AttributeSerializer.getLength( bites );
            assertEquals( ii, deserialized );
        }

        // test 2nd byte boundry
        for ( int ii = 65530; ii < 65540; ii++ )
        {
            byte[] bites = AttributeSerializer.getLengthBytes( ii );
            int deserialized = AttributeSerializer.getLength( bites );
            assertEquals( ii, deserialized );
        }

        // test 3rd byte boundry
        for ( int ii = 16777210; ii < 16777220; ii++ )
        {
            byte[] bites = AttributeSerializer.getLengthBytes( ii );
            int deserialized = AttributeSerializer.getLength( bites );
            assertEquals( ii, deserialized );
        }
    }
    
    
    public void testWriteLengthBytes0()
    {
        byte[] buf = new byte[7];
        
        int pos = AttributeSerializer.writeLengthBytes( buf, 23 );
        assertEquals( 4, pos );
        assertEquals( 0, buf[0] );
        assertEquals( 0, buf[1] );
        assertEquals( 0, buf[2] );
        assertEquals( 23, buf[3] );
        assertEquals( 0, buf[4] );
        assertEquals( 0, buf[5] );
        assertEquals( 0, buf[6] );
        
        pos = AttributeSerializer.writeValueBytes( buf, "a", pos );
        assertEquals( 6, pos );
        assertEquals( 0, buf[4] );
        assertEquals( 97, buf[5] );
        assertEquals( 0, buf[6] );
    }


    public void testWriteValueBytes0()
    {
        byte[] buf = new byte[20];
        
        int pos = AttributeSerializer.writeLengthBytes( buf, 23 );
        assertEquals( 4, pos );
        assertEquals( 0, buf[0] );
        assertEquals( 0, buf[1] );
        assertEquals( 0, buf[2] );
        assertEquals( 23, buf[3] );
        assertEquals( 0, buf[4] );
        assertEquals( 0, buf[5] );
        assertEquals( 0, buf[6] );
        
        pos = AttributeSerializer.writeValueBytes( buf, "abc", pos );
        assertEquals( 10,  pos );
        assertEquals( 0,  buf[4] );
        assertEquals( 97, buf[5] );
        assertEquals( 0,  buf[6] );
        assertEquals( 98, buf[7] );
        assertEquals( 0,  buf[8] );
        assertEquals( 99, buf[9] );
        assertEquals( 0,  buf[10] ); // here now
        assertEquals( 0,  buf[11] );
        assertEquals( 0,  buf[12] );
        
        pos = AttributeSerializer.write( buf, "def", pos );
        assertEquals( 20, pos );
        assertEquals( 0, buf[10] );
        assertEquals( 0, buf[11] );
        assertEquals( 0, buf[12] );
        assertEquals( 6, buf[13] );
        
        assertEquals( 0,   buf[14] );
        assertEquals( 100, buf[15] );
        assertEquals( 0,   buf[16] );
        assertEquals( 101, buf[17] );
        assertEquals( 0,   buf[18] );
        assertEquals( 102, buf[19] );
    }
    
    
    public void testReadString()
    {
        byte[] buf = new byte[26];
        
        // let's write the length so we can read it
        int pos = AttributeSerializer.writeLengthBytes( buf, 6 );
        assertEquals( 4, pos );
        assertEquals( 0, buf[0] );
        assertEquals( 0, buf[1] );
        assertEquals( 0, buf[2] );
        assertEquals( 6, buf[3] );
        
        // let's write the value so we can read it
        pos = AttributeSerializer.writeValueBytes( buf, "abc", pos );
        assertEquals( 10,  pos );
        assertEquals( 0,  buf[4] );
        assertEquals( 97, buf[5] );
        assertEquals( 0,  buf[6] );
        assertEquals( 98, buf[7] );
        assertEquals( 0,  buf[8] );
        assertEquals( 99, buf[9] );

        // let's write another string as well
        pos = AttributeSerializer.write( buf, "defgh", pos );
        assertEquals( 24, pos );
        assertEquals( 0, buf[10] );
        assertEquals( 0, buf[11] );
        assertEquals( 0, buf[12] );
        assertEquals( 10, buf[13] );
        
        assertEquals( 0,   buf[14] );
        assertEquals( 100, buf[15] );
        assertEquals( 0,   buf[16] );
        assertEquals( 101, buf[17] );
        assertEquals( 0,   buf[18] );
        assertEquals( 102, buf[19] );
        assertEquals( 0,   buf[20] );
        assertEquals( 103, buf[21] );
        assertEquals( 0,   buf[22] );
        assertEquals( 104, buf[23] );
        assertEquals( 0,   buf[24] );
        assertEquals( 0,   buf[25] );
        
        // now let's read "abc"
        String s1 = AttributeSerializer.readString( buf );
        assertEquals( "abc", s1 );
    }
    
    
    public void testFullCycleNonBinaryAttribute() throws IOException
    {
        LockableAttributeImpl attr = new LockableAttributeImpl( "testing" );
        AttributeSerializer serializer = new AttributeSerializer();
        attr.add( "value0" );
        attr.add( "val1" );
        attr.add( "anything over here!" );
        
        byte[] serialized = serializer.serialize( attr );
        Attribute deserialized = ( Attribute ) serializer.deserialize( serialized );
        assertEquals( attr, deserialized );
    }

    
    public void testFullCycleBinaryAttribute() throws IOException, NamingException
    {
        LockableAttributeImpl attr = new LockableAttributeImpl( "testing" );
        AttributeSerializer serializer = new AttributeSerializer();
        byte[] ba0 = new byte[2];
        ba0[0] = 7;
        ba0[1] = 23;
        attr.add( ba0 );
        byte[] ba1 = new byte[3];
        ba1[0] = 34;
        ba1[1] = 111;
        ba1[2] = 67;
        attr.add( ba1 );
        
        byte[] serialized = serializer.serialize( attr );
        Attribute deserialized = ( Attribute ) serializer.deserialize( serialized );
        ArrayUtils.isEquals( ba0, ( byte[] ) deserialized.get() );
        ArrayUtils.isEquals( ba1, ( byte[] ) deserialized.get( 1 ) );
    }
    
    
    public void doSerializerSpeedTest() throws IOException
    {
        final int limit = 1000000;
        long start = System.currentTimeMillis();
        for ( int ii = 0; ii < limit; ii++ )
        {
            LockableAttributeImpl attr = new LockableAttributeImpl( "testing" );
            AttributeSerializer serializer = new AttributeSerializer();
            attr.add( "value0" );
            attr.add( "val1" );
            attr.add( "anything over here!" );
            
            byte[] serialized = serializer.serialize( attr );
            serializer.deserialize( serialized );
        }
        
        System.out.println( limit + " attributes with 3 values each were serialized and deserialized in " 
            + ( System.currentTimeMillis() - start ) + " (ms)" );
    }
}
