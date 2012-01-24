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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Random;

import org.apache.commons.lang.ArrayUtils;
import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test case for the BTreeRedirect serialization code.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class BTreeRedirectMarshallerTest
{
    BTreeRedirectMarshaller marshaller = new BTreeRedirectMarshaller();


    private byte[] createBites()
    {
        byte[] bites = new byte[BTreeRedirectMarshaller.SIZE];
        bites[0] = 1;

        for ( int ii = 8; ii < BTreeRedirectMarshaller.SIZE; ii++ )
        {
            bites[ii] = 0;
        }
        return bites;
    }


    @Test
    public void testZero() throws IOException
    {
        byte[] bites = createBites();
        assertEquals( 0, marshaller.deserialize( bites ).getRecId() );
        assertTrue( ArrayUtils.isEquals( bites, marshaller.serialize( new BTreeRedirect( 0 ) ) ) );
    }


    @Test
    public void testOne() throws IOException
    {
        byte[] bites = createBites();
        bites[8] = 1;
        assertEquals( 1, marshaller.deserialize( bites ).getRecId() );
        assertTrue( ArrayUtils.isEquals( bites, marshaller.serialize( new BTreeRedirect( 1 ) ) ) );
    }


    @Test
    public void testNegativeOne() throws IOException
    {
        byte[] bites = createBites();
        for ( int ii = 1; ii < BTreeRedirectMarshaller.SIZE; ii++ )
        {
            bites[ii] = ( byte ) 0xFF;
        }

        assertEquals( -1, marshaller.deserialize( bites ).getRecId() );
        assertTrue( ArrayUtils.isEquals( bites, marshaller.serialize( new BTreeRedirect( -1 ) ) ) );
    }


    @Test
    public void testLongMinValue() throws IOException
    {
        byte[] bites = createBites();
        bites[1] = ( byte ) 0x80;
        assertEquals( Long.MIN_VALUE, marshaller.deserialize( bites ).getRecId() );
        assertTrue( ArrayUtils.isEquals( bites, marshaller.serialize( new BTreeRedirect( Long.MIN_VALUE ) ) ) );
    }


    @Test
    public void testLongMaxValue() throws IOException
    {
        byte[] bites = createBites();
        bites[1] = ( byte ) 0x7F;

        for ( int ii = 2; ii < BTreeRedirectMarshaller.SIZE; ii++ )
        {
            bites[ii] = ( byte ) 0xFF;
        }

        assertEquals( Long.MAX_VALUE, marshaller.deserialize( bites ).getRecId() );
        assertTrue( ArrayUtils.isEquals( bites, marshaller.serialize( new BTreeRedirect( Long.MAX_VALUE ) ) ) );
    }


    @Test
    public void testRoundTripTests() throws IOException
    {
        byte[] bites = createBites();
        Random random = new Random();
        for ( int ii = 0; ii < 100; ii++ )
        {
            long orig = random.nextLong();
            bites = marshaller.serialize( new BTreeRedirect( orig ) );
            assertEquals( orig, marshaller.deserialize( bites ).getRecId() );
        }
    }


    @Test
    public void testMiscellaneous()
    {
        assertNotNull( new BTreeRedirect( 1 ).toString() );
        assertFalse( BTreeRedirectMarshaller.isRedirect( null ) );

        try
        {
            marshaller.deserialize( null );
            fail( "Should not get here." );
        }
        catch ( IOException e )
        {
        }

        try
        {
            marshaller.deserialize( "bogus".getBytes() );
            fail( "Should not get here." );
        }
        catch ( IOException e )
        {
        }
    }
}
