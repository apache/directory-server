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


import org.junit.Test;
import org.junit.Before;
import org.apache.directory.shared.ldap.util.ArrayUtils;

import static junit.framework.Assert.*;

import java.util.Random;
import java.io.IOException;


/**
 * Test case for the BTreeRedirect serialization code.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BTreeRedirectMarshallerTest
{
    byte[] bites = new byte[BTreeRedirectMarshaller.SIZE];
    BTreeRedirectMarshaller marshaller = new BTreeRedirectMarshaller();


    @Before
    public void setup()
    {
        bites[0] = 'r';
        bites[1] = 'e';
        bites[2] = 'd';
        bites[3] = 'i';
        bites[4] = 'r';
        bites[5] = 'e';
        bites[6] = 'c';
        bites[7] = 't';

        for ( int ii = 8; ii < BTreeRedirectMarshaller.SIZE; ii++ )
        {
            bites[ii] = 0;
        }
    }


    @Test
    public void testZero() throws IOException
    {
        assertEquals( 0, marshaller.deserialize( bites ).getRecId() );
        assertTrue( ArrayUtils.isEquals( bites, marshaller.serialize( new BTreeRedirect( 0 ) ) ) );
    }


    @Test
    public void testOne() throws IOException
    {
        bites[15] = 1;
        assertEquals( 1, marshaller.deserialize( bites ).getRecId() );
        assertTrue( ArrayUtils.isEquals( bites, marshaller.serialize( new BTreeRedirect( 1 ) ) ) );
    }


    @Test
    public void testNegativeOne() throws IOException
    {
        for ( int ii = 8; ii < BTreeRedirectMarshaller.SIZE; ii++ )
        {
            bites[ii] =  ( byte ) 0xFF;
        }

        assertEquals( -1, marshaller.deserialize( bites ).getRecId() );
        assertTrue( ArrayUtils.isEquals( bites, marshaller.serialize( new BTreeRedirect( -1 ) ) ) );
    }


    @Test
    public void testLongMinValue() throws IOException
    {
        bites[8] = ( byte ) 0x80;
        assertEquals( Long.MIN_VALUE, marshaller.deserialize( bites ).getRecId() );
        assertTrue( ArrayUtils.isEquals( bites, marshaller.serialize( new BTreeRedirect( Long.MIN_VALUE ) ) ) );
    }


    @Test
    public void testLongMaxValue() throws IOException
    {
        bites[8] = ( byte ) 0x7F;

        for ( int ii = 9; ii < BTreeRedirectMarshaller.SIZE; ii++ )
        {
            bites[ii] =  ( byte ) 0xFF;
        }

        assertEquals( Long.MAX_VALUE, marshaller.deserialize( bites ).getRecId() );
        assertTrue( ArrayUtils.isEquals( bites, marshaller.serialize( new BTreeRedirect( Long.MAX_VALUE ) ) ) );
    }


    @Test
    public void testRoundTripTests() throws IOException
    {
        Random random = new Random();
        for ( int ii = 0; ii < 100; ii++ )
        {
            long orig = random.nextLong();
            bites = marshaller.serialize( new BTreeRedirect( orig ) );
            assertEquals( orig, marshaller.deserialize( bites ).getRecId() );
        }
    }
}
