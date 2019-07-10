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


import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;

import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.api.util.Strings;
import org.apache.directory.shared.kerberos.codec.changePwdData.ChangePasswdDataContainer;
import org.apache.directory.shared.kerberos.messages.ChangePasswdData;
import org.junit.Test;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ChangePasswdDataDecoderTest
{

    @Test
    public void testDecodeChangePasswdData() throws Exception
    {

        ByteBuffer buf = ByteBuffer.allocate( 0x30 );
        buf.put( new byte[]
            {
                0x30, 0x2E,
                ( byte ) 0xA0, 0x08, // newpasswd
                0x04,
                0x06,
                's',
                'e',
                'c',
                'r',
                'e',
                't',
                ( byte ) 0xA1,
                0x13, // targname
                0x30,
                0x11,
                ( byte ) 0xA0,
                0x03, // name-type
                0x02,
                0x01,
                0x01, // NT-PRINCIPAL
                ( byte ) 0xA1,
                0x0A, // name-string
                0x30,
                0x08,
                0x1B,
                0x06,
                'k',
                'r',
                'b',
                't',
                'g',
                't',
                ( byte ) 0xA2,
                0x0D,
                0x1B,
                0x0B,
                'E',
                'X',
                'A',
                'M',
                'P',
                'L',
                'E',
                '.',
                'C',
                'O',
                'M'

        } );

        String decodedPdu = Strings.dumpBytes( buf.array() );
        buf.flip();

        ChangePasswdDataContainer container = new ChangePasswdDataContainer( buf );

        Asn1Decoder.decode( buf, container );

        ChangePasswdData chngPwdData = container.getChngPwdData();

        assertArrayEquals( Strings.getBytesUtf8( "secret" ), chngPwdData.getNewPasswd() );
        assertEquals( "krbtgt", chngPwdData.getTargName().getNameString() );
        assertEquals( "EXAMPLE.COM", chngPwdData.getTargRealm() );

        String encodedPdu = Strings.dumpBytes( chngPwdData.encode( null ).array() );
        assertEquals( decodedPdu, encodedPdu );
    }


    @Test
    public void testDecodeChangePasswdDataWithoutTargName() throws Exception
    {

        ByteBuffer buf = ByteBuffer.allocate( 0x1B );
        buf.put( new byte[]
            {
                0x30, 0x19,
                ( byte ) 0xA0, 0x08, // newpasswd
                0x04,
                0x06,
                's',
                'e',
                'c',
                'r',
                'e',
                't',
                ( byte ) 0xA2,
                0x0D,
                0x1B,
                0x0B,
                'E',
                'X',
                'A',
                'M',
                'P',
                'L',
                'E',
                '.',
                'C',
                'O',
                'M'

        } );

        String decodedPdu = Strings.dumpBytes( buf.array() );
        buf.flip();

        ChangePasswdDataContainer container = new ChangePasswdDataContainer( buf );

        Asn1Decoder.decode( buf, container );

        ChangePasswdData chngPwdData = container.getChngPwdData();

        assertArrayEquals( Strings.getBytesUtf8( "secret" ), chngPwdData.getNewPasswd() );
        assertEquals( "EXAMPLE.COM", chngPwdData.getTargRealm() );

        String encodedPdu = Strings.dumpBytes( chngPwdData.encode( null ).array() );
        assertEquals( decodedPdu, encodedPdu );
    }


    @Test
    public void testDecodeChangePasswdDataWithoutTargRealm() throws Exception
    {

        ByteBuffer buf = ByteBuffer.allocate( 0x21 );
        buf.put( new byte[]
            {
                0x30, 0x1F,
                ( byte ) 0xA0, 0x08, // newpasswd
                0x04,
                0x06,
                's',
                'e',
                'c',
                'r',
                'e',
                't',
                ( byte ) 0xA1,
                0x13, // targname
                0x30,
                0x11,
                ( byte ) 0xA0,
                0x03, // name-type
                0x02,
                0x01,
                0x01, // NT-PRINCIPAL
                ( byte ) 0xA1,
                0x0A, // name-string
                0x30,
                0x08,
                0x1B,
                0x06,
                'k',
                'r',
                'b',
                't',
                'g',
                't'
        } );

        String decodedPdu = Strings.dumpBytes( buf.array() );
        buf.flip();

        ChangePasswdDataContainer container = new ChangePasswdDataContainer( buf );

        Asn1Decoder.decode( buf, container );

        ChangePasswdData chngPwdData = container.getChngPwdData();

        assertArrayEquals( Strings.getBytesUtf8( "secret" ), chngPwdData.getNewPasswd() );
        assertEquals( "krbtgt", chngPwdData.getTargName().getNameString() );

        String encodedPdu = Strings.dumpBytes( chngPwdData.encode( null ).array() );
        assertEquals( decodedPdu, encodedPdu );
    }


    @Test
    public void testDecodeChangePasswdDataWithoutTargNameAndRealm() throws Exception
    {

        ByteBuffer buf = ByteBuffer.allocate( 0x0C );
        buf.put( new byte[]
            {
                0x30, 0x0A,
                ( byte ) 0xA0, 0x08, // newpasswd
                0x04,
                0x06,
                's',
                'e',
                'c',
                'r',
                'e',
                't'
        } );

        String decodedPdu = Strings.dumpBytes( buf.array() );
        buf.flip();

        ChangePasswdDataContainer container = new ChangePasswdDataContainer( buf );

        Asn1Decoder.decode( buf, container );

        ChangePasswdData chngPwdData = container.getChngPwdData();

        assertArrayEquals( Strings.getBytesUtf8( "secret" ), chngPwdData.getNewPasswd() );

        String encodedPdu = Strings.dumpBytes( chngPwdData.encode( null ).array() );
        assertEquals( decodedPdu, encodedPdu );
    }
}
