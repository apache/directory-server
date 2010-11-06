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
package org.apache.directory.shared.kerberos.codec;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;

import org.apache.directory.junit.tools.Concurrent;
import org.apache.directory.junit.tools.ConcurrentJunitRunner;
import org.apache.directory.shared.asn1.ber.Asn1Container;
import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.kerberos.codec.principalName.PrincipalNameContainer;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.apache.directory.shared.kerberos.components.PrincipalNameType;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test the PrincipalName decoder
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrent()
public class PrincipalNameDecoderTest
{
    /** The encoder instance */
    //LdapEncoder encoder = new LdapEncoder();

    /**
     * Test the decoding of a PrincipalName
     */
    @Test
    public void testPrincipalName()
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x29 );
        
        stream.put( new byte[]
            { 0x30, 0x27,
                (byte)0xA0, 0x03,                 // name-type
                  0x02, 0x01, 0x01,               // NT-PRINCIPAL
                (byte)0xA1, 0x20,                 // name-string
                  0x30, 0x1E,
                    0x1B, 0x08, 'h', 'n', 'e', 'l', 's', 'o', 'n', '1',
                    0x1B, 0x08, 'h', 'n', 'e', 'l', 's', 'o', 'n', '2',
                    0x1B, 0x08, 'h', 'n', 'e', 'l', 's', 'o', 'n', '3',
            } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a PrincipalName Container
        Asn1Container principalNameContainer = new PrincipalNameContainer();

        // Decode the PrincipalName PDU
        try
        {
            kerberosDecoder.decode( stream, principalNameContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        // Check the decoded BindRequest
        PrincipalName principalName = ( ( PrincipalNameContainer ) principalNameContainer ).getPrincipalName();

        assertEquals( PrincipalNameType.KRB_NT_PRINCIPAL, principalName.getNameType() );
        assertTrue( principalName.getNames().contains( "hnelson1" ) );
        assertTrue( principalName.getNames().contains( "hnelson2" ) );
        assertTrue( principalName.getNames().contains( "hnelson3" ) );

        /*
        // Check the encoding
        try
        {
            ByteBuffer bb = encoder.encodeMessage( bindRequest );

            // Check the length
            assertEquals( 0x35, bb.limit() );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
        */
    }
}
