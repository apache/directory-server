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
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;

import org.apache.directory.junit.tools.Concurrent;
import org.apache.directory.junit.tools.ConcurrentJunitRunner;
import org.apache.directory.shared.asn1.DecoderException;
import org.apache.directory.shared.asn1.EncoderException;
import org.apache.directory.shared.asn1.ber.Asn1Container;
import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.kerberos.codec.etypeInfo2.ETypeInfo2Container;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.ETypeInfo2;
import org.apache.directory.shared.kerberos.components.ETypeInfo2Entry;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test the ETYPE-INFO2 decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrent()
public class ETypeInfo2DecoderTest
{
    /**
     * Test the decoding of a ETYPE-INFO2
     */
    @Test
    public void testETypeInfo2()
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x20 );
        
        stream.put( new byte[]
            { 
              0x30, 0x1E,
                0x30, 0x0D,
                  (byte)0xA0, 0x03,                 // etype
                    0x02, 0x01, 0x05,
                  (byte)0xA1, 0x06,                 // salt
                    0x1B, 0x04, 0x31, 0x32, 0x33, 0x34,
                0x30, 0x0D,
                  (byte)0xA0, 0x03,                 // etype
                    0x02, 0x01, 0x05,
                  (byte)0xA1, 0x06,                 // salt
                    0x1B, 0x04, 0x35, 0x36, 0x37, 0x38
            } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a ETypeInfo2 Container
        Asn1Container etypeInfo2Container = new ETypeInfo2Container();
        etypeInfo2Container.setStream( stream );

        // Decode the ETypeInfo2 PDU
        try
        {
            kerberosDecoder.decode( stream, etypeInfo2Container );
        }
        catch ( DecoderException de )
        {
            fail( de.getMessage() );
        }

        // Check the decoded ETypeInfo2
        ETypeInfo2 etypeInfo2 = ( ( ETypeInfo2Container ) etypeInfo2Container ).getETypeInfo2();

        assertEquals( 2, etypeInfo2.getETypeInfo2Entries().length );
        
        String[] expected = new String[]{ "1234", "5678" };
        int i = 0;
        
        for ( ETypeInfo2Entry etypeInfo2Entry : etypeInfo2.getETypeInfo2Entries() )
        {
            assertEquals( EncryptionType.DES3_CBC_MD5, etypeInfo2Entry.getEType() );
            assertEquals( expected[i], etypeInfo2Entry.getSalt() );
            i++;
        }

        // Check the encoding
        ByteBuffer bb = ByteBuffer.allocate( etypeInfo2.computeLength() );
        
        try
        {
            bb = etypeInfo2.encode( bb );
    
            // Check the length
            assertEquals( 0x20, bb.limit() );
    
            String encodedPdu = StringTools.dumpBytes( bb.array() );
    
            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }
    
    
    /**
     * Test the decoding of a ETypeInfo2 with nothing in it
     */
    @Test( expected = DecoderException.class)
    public void testETypeInfo2Empty() throws DecoderException
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x02 );
        
        stream.put( new byte[]
            { 0x30, 0x00 } );

        stream.flip();

        // Allocate a ETypeInfo2 Container
        Asn1Container etypeInfo2Container = new ETypeInfo2Container();

        // Decode the ETypeInfo2 PDU
        kerberosDecoder.decode( stream, etypeInfo2Container );
        fail();
    }
    
    
    /**
     * Test the decoding of a ETypeInfo2 with empty ETypeInfo2Entry in it
     */
    @Test( expected = DecoderException.class)
    public void testETypeInfo2NoETypeInfo2Entry() throws DecoderException
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x04 );
        
        stream.put( new byte[]
            { 
              0x30, 0x02,
                (byte)0x30, 0x00                  // empty ETypeInfo2Entry
            } );

        stream.flip();

        // Allocate a ETypeInfo2 Container
        Asn1Container etypeInfo2Container = new ETypeInfo2Container();

        // Decode the ETypeInfo2 PDU
        kerberosDecoder.decode( stream, etypeInfo2Container );
        fail();
    }
}
