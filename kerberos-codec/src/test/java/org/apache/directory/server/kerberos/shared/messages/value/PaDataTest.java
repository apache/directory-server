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
package org.apache.directory.server.kerberos.shared.messages.value;


import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;
import org.apache.directory.shared.kerberos.codec.types.PaDataType;
import org.apache.directory.shared.kerberos.components.PaData;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test the PaData encoding and decoding
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class PaDataTest
{
    @Test
    public void testEncodingPreAuthenticationData() throws Exception
    {
        PaData pad = new PaData( PaDataType.PA_ASF3_SALT, new byte[]
            { 0x01, 0x02, 0x03 } );

        ByteBuffer encoded = ByteBuffer.allocate( pad.computeLength() );

        pad.encode( encoded );

        byte[] expectedResult = new byte[]
            {
                0x30, 0x0c,
                ( byte ) 0xA1, 0x03,
                0x02, 0x01, 0x0A,
                ( byte ) 0xA2, 0x05,
                0x04, 0x03,
                0x01, 0x02, 0x03
        };

        assertTrue( Arrays.equals( expectedResult, encoded.array() ) );
    }


    @Test
    public void testEncodingNullPreAuthenticationData() throws Exception
    {
        PaData pad = new PaData( PaDataType.PA_ASF3_SALT, null );

        ByteBuffer encoded = ByteBuffer.allocate( pad.computeLength() );

        pad.encode( encoded );

        byte[] expectedResult = new byte[]
            {
                0x30, 0x09,
                ( byte ) 0xA1, 0x03,
                0x02, 0x01, 0x0A,
                ( byte ) 0xA2, 0x02,
                0x04, 0x00
        };

        assertTrue( Arrays.equals( expectedResult, encoded.array() ) );
    }
}
