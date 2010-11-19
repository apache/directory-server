/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.shared.kerberos.codec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.kerberos.codec.krbError.KrbErrorContainer;
import org.apache.directory.shared.kerberos.messages.KrbError;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.Test;

/**
 * Test cases for KrbError codec
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KrbErrorDecoderTest
{
    @Test
    public void testDecodeKrbError()
    {
        Asn1Decoder decoder = new Asn1Decoder();
        
        int streamLen = 0xCE;
        ByteBuffer stream = ByteBuffer.allocate( streamLen );
        stream.put( new byte[]
        {
                0x7E, (byte)0x81, (byte)0xCB,
                  0x30, (byte)0x81, (byte)0xC8,
                    (byte)0xA0, 0x03,           // pvno
                           0x02, 0x01, 0x05,    
                    (byte)0xA1, 0x03,           // msg-type
                           0x02, 0x01, 0x1E,   
                    (byte)0xA2, 0x11,           // ctime
                           0x18, 0xF, '2', '0', '1', '0', '1', '1', '1', '9', '0', '8', '0', '0', '4', '3', 'Z',
                    (byte)0xA3, 0x03,           // cusec
                           0x02, 0x01, 0x01,
                    (byte)0xA4, 0x11,           // stime
                           0x18, 0xF, '2', '0', '1', '0', '1', '1', '1', '9', '0', '8', '0', '0', '4', '3', 'Z',
                    (byte)0xA5, 0x03,           // susec
                           0x02, 0x01, 0x02,
                    (byte)0xA6, 0x03,           // error-code
                           0x02, 0x01, 0x00,
                    (byte)0xA7, 0x8,            // crealm
                           0x1B, 0x06, 'c', 'r', 'e', 'a', 'l', 'm',
                    (byte)0xA8, 0x12,           // cname
                           0x30, 0x10, 
                           // FIXME here it fails with ERR_00001_BAD_TRANSITION_FROM_STATE Bad transition from state START_STATE, tag 0xA0
                           (byte)0xA0, 0x03,
                             0x02, 0x01, 0x00, 
                             (byte)0xA1, 0x09, 
                             0x30, 0x07, 
                              0x1B, 0x05, 'c', 'n', 'a', 'm', 'e',
                    (byte)0xA9, 0x07,           // realm
                           0x1B, 0x05, 'r', 'e', 'a', 'l', 'm',
                    (byte)0xAA, 0x12,           // sname
                           0x30, 0x10, 
                           (byte)0xA0, 0x03,
                             0x02, 0x01, 0x00, 
                             (byte)0xA1, 0x09, 
                             0x30, 0x07, 
                              0x1B, 0x05, 's', 'n', 'a', 'm', 'e',
                    (byte)0xAB, 0x07,           // e-text
                           0x1B, 0x5, 'e', 't', 'e', 'x', 't',
                    (byte)0xAC, 0x04,           // e-data
                           0x04, 0x02, 0x00, 0x01
        } );
        
        String decoded = StringTools.utf8ToString( stream.array() );
        stream.flip();
        
        KrbErrorContainer container = new KrbErrorContainer();
        container.setStream( stream );
        
        try
        {
            decoder.decode( stream, container );
        }
        catch( DecoderException e )
        {
            e.printStackTrace();
            fail();
        }
        
        KrbError krbError = container.getKrbError();
        
        int encodedLen = krbError.computeLength();
        
        assertEquals( streamLen, encodedLen );
        
        ByteBuffer buffer = ByteBuffer.allocate( streamLen );
        try
        {
            buffer = krbError.encode( buffer );
            assertEquals( decoded, StringTools.utf8ToString( buffer.array() ) );
        }
        catch( EncoderException e )
        {
            fail();
        }
    }
}
