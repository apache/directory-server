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

import org.apache.directory.shared.kerberos.codec.types.AuthorizationType;
import org.apache.directory.shared.kerberos.components.AuthorizationData;
import org.apache.directory.shared.kerberos.components.AuthorizationDataEntry;
import org.junit.jupiter.api.Test;


/**
 * Test the AuthorizationData encoding and decoding
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AuthorizationDataTest
{
    @Test
    public void testAuthorizationDataOneAD() throws Exception
    {
        AuthorizationData ad = new AuthorizationData();
        ad.addEntry( new AuthorizationDataEntry( AuthorizationType.AD_KDC_ISSUED, new byte[]
            { 0x01, 0x02, 0x03, 0x04 } ) );

        ByteBuffer encoded = ByteBuffer.allocate( ad.computeLength() );

        ad.encode( encoded );

        byte[] expectedResult = new byte[]
            {
                0x30, 0x0F,
                0x30, 0x0d,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x04,
                ( byte ) 0xA1, 0x06,
                0x04, 0x04, 0x01, 0x02, 0x03, 0x04
        };

        assertTrue( Arrays.equals( expectedResult, encoded.array() ) );
    }


    @Test
    public void testAuthorizationDataThreeAD() throws Exception
    {
        AuthorizationData ad = new AuthorizationData();
        ad.addEntry( new AuthorizationDataEntry( AuthorizationType.AD_KDC_ISSUED, new byte[]
            { 0x01, 0x02, 0x03, 0x04 } ) );
        ad.addEntry( new AuthorizationDataEntry( AuthorizationType.AD_IF_RELEVANT, new byte[]
            { 0x05, 0x06, 0x07, 0x08 } ) );
        ad.addEntry( new AuthorizationDataEntry( AuthorizationType.AD_MANDATORY_TICKET_EXTENSIONS, new byte[]
            { 0x09, 0x0A, 0x0B, 0x0C } ) );

        ByteBuffer encoded = ByteBuffer.allocate( ad.computeLength() );

        ad.encode( encoded );

        byte[] expectedResult = new byte[]
            {
                0x30, 0x2D,
                0x30, 0x0d,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x04,
                ( byte ) 0xA1, 0x06,
                0x04, 0x04,
                0x01, 0x02, 0x03, 0x04,
                0x30, 0x0d,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x01,
                ( byte ) 0xA1, 0x06,
                0x04, 0x04,
                0x05, 0x06, 0x07, 0x08,
                0x30, 0x0d,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x06,
                ( byte ) 0xA1, 0x06,
                0x04, 0x04,
                0x09, 0x0A, 0x0B, 0x0C
        };

        assertTrue( Arrays.equals( expectedResult, encoded.array() ) );
    }
}
