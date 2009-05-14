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

package org.apache.directory.shared.ldap.codec.extended.operations;


import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.codec.extended.operations.storedProcedure.StoredProcedure;
import org.apache.directory.shared.ldap.codec.extended.operations.storedProcedure.StoredProcedureContainer;
import org.apache.directory.shared.ldap.codec.extended.operations.storedProcedure.StoredProcedureDecoder;
import org.apache.directory.shared.ldap.codec.extended.operations.storedProcedure.StoredProcedure.StoredProcedureParameter;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


/*
 * TestCase for a Stored Procedure Extended Operation ASN.1 codec
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoredProcedureTest
{
    @Test
    public void testDecodeStoredProcedureNParams()
    {
        Asn1Decoder storedProcedureDecoder = new StoredProcedureDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x44 );

        stream.put( new byte[]
            {
                0x30, 0x42,
                    0x04, 0x04, 'J', 'a', 'v', 'a',
                    0x04, 0x07, 'e', 'x', 'e', 'c', 'u', 't', 'e',
                    0x30, 0x31,
                      0x30, 0x08,
                        0x04, 0x03, 'i', 'n', 't', 
                        0x04, 0x01, 0x01,
                      0x30, 0x0F,
                        0x04, 0x07, 'b', 'o', 'o', 'l', 'e', 'a', 'n', 
                        0x04, 0x04, 't', 'r', 'u', 'e',
                      0x30, 0x14,
                        0x04, 0x06, 'S', 't', 'r', 'i', 'n', 'g', 
                        0x04, 0x0A, 'p', 'a', 'r', 'a', 'm', 'e', 't', 'e', 'r', '3' 
            } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a StoredProcedure Container
        IAsn1Container storedProcedureContainer = new StoredProcedureContainer();

        // Decode a StoredProcedure message
        try
        {
            storedProcedureDecoder.decode( stream, storedProcedureContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        StoredProcedure storedProcedure = ( ( StoredProcedureContainer ) storedProcedureContainer ).getStoredProcedure();

        assertEquals("Java", storedProcedure.getLanguage());
        
        assertEquals( "execute", StringTools.utf8ToString( storedProcedure.getProcedure() ) );

        assertEquals( 3, storedProcedure.getParameters().size() );

        StoredProcedureParameter param = storedProcedure.getParameters().get( 0 );

        assertEquals( "int", StringTools.utf8ToString( param.getType() ) );
        assertEquals( 1, param.getValue()[0] );

        param = storedProcedure.getParameters().get( 1 );

        assertEquals( "boolean", StringTools.utf8ToString( param.getType() ) );
        assertEquals( "true", StringTools.utf8ToString( param.getValue() ) );

        param = storedProcedure.getParameters().get( 2 );

        assertEquals( "String", StringTools.utf8ToString( param.getType() ) );
        assertEquals( "parameter3", StringTools.utf8ToString( param.getValue() ) );

        // Check the encoding
        try
        {
            ByteBuffer bb = storedProcedure.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }

    @Test
    public void testDecodeStoredProcedureNoParam()
    {
        Asn1Decoder storedProcedureDecoder = new StoredProcedureDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x13 );

        stream.put( new byte[]
            {
                0x30, 0x11,
                    0x04, 0x04, 'J', 'a', 'v', 'a',
                    0x04, 0x07, 'e', 'x', 'e', 'c', 'u', 't', 'e',
                    0x30, 0x00
            } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a StoredProcedure Container
        IAsn1Container storedProcedureContainer = new StoredProcedureContainer();

        // Decode a StoredProcedure message
        try
        {
            storedProcedureDecoder.decode( stream, storedProcedureContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        StoredProcedure storedProcedure = ( ( StoredProcedureContainer ) storedProcedureContainer ).getStoredProcedure();

        assertEquals("Java", storedProcedure.getLanguage());
        
        assertEquals( "execute", StringTools.utf8ToString( storedProcedure.getProcedure() ) );

        assertEquals( 0, storedProcedure.getParameters().size() );
        
        // Check the encoding
        try
        {
            ByteBuffer bb = storedProcedure.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }

    
    @Test
    public void testDecodeStoredProcedureOneParam()
    {
        Asn1Decoder storedProcedureDecoder = new StoredProcedureDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x1D );

        stream.put( new byte[]
            {
                0x30, 0x1B,
                  0x04, 0x04, 'J', 'a', 'v', 'a',
                  0x04, 0x07, 'e', 'x', 'e', 'c', 'u', 't', 'e',
                  0x30, 0x0A,
                      0x30, 0x08,
                        0x04, 0x03, 'i', 'n', 't', 
                        0x04, 0x01, 0x01,
            } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a StoredProcedure Container
        IAsn1Container storedProcedureContainer = new StoredProcedureContainer();

        // Decode a StoredProcedure message
        try
        {
            storedProcedureDecoder.decode( stream, storedProcedureContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        StoredProcedure storedProcedure = ( ( StoredProcedureContainer ) storedProcedureContainer ).getStoredProcedure();

        assertEquals("Java", storedProcedure.getLanguage());
        
        assertEquals( "execute", StringTools.utf8ToString( storedProcedure.getProcedure() ) );

        assertEquals( 1, storedProcedure.getParameters().size() );

        StoredProcedureParameter param = storedProcedure.getParameters().get( 0 );

        assertEquals( "int", StringTools.utf8ToString( param.getType() ) );
        assertEquals( 1, param.getValue()[0] );

        // Check the encoding
        try
        {
            ByteBuffer bb = storedProcedure.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }
}
