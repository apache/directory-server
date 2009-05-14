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
import org.apache.directory.shared.ldap.codec.extended.operations.cancel.Cancel;
import org.apache.directory.shared.ldap.codec.extended.operations.cancel.CancelContainer;
import org.apache.directory.shared.ldap.codec.extended.operations.cancel.CancelDecoder;
import org.apache.directory.shared.ldap.util.StringTools;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/*
 * TestCase for a Cancel Extended Operation
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class CancelRequestTest
{
    /**
     * Test the normal Cancel message
     */
    @Test
    public void testDecodeCancel()
    {
        Asn1Decoder cancelDecoder = new CancelDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x05 );

        stream.put( new byte[]
            {
                0x30, 0x03,
                    0x02, 0x01, 0x01
            } ).flip();

        String decodedPdu = StringTools.dumpBytes( stream.array() );

        // Allocate a Cancel Container
        IAsn1Container cancelContainer = new CancelContainer();

        // Decode a Cancel message
        try
        {
            cancelDecoder.decode( stream, cancelContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        Cancel cancel = ( ( CancelContainer ) cancelContainer ).getCancel();

        assertEquals( 1, cancel.getCancelId() );

        // Check the encoding
        try
        {
            ByteBuffer bb = cancel.encode( null );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }

    
    /**
     * Test a Cancel message with no cancelId
     */
    @Test
    public void testDecodeCancelNoCancelId()
    {
        Asn1Decoder cancelDecoder = new CancelDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x02 );

        stream.put( new byte[]
            {
                0x30, 0x00
            } ).flip();

        // Allocate a Cancel Container
        IAsn1Container cancelContainer = new CancelContainer();
        
        // Decode a Cancel message
        try
        {
            cancelDecoder.decode( stream, cancelContainer );
            fail( "CancelID expected" );
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
        }
    }

    
    /**
     * Test a Cancel message with an empty cancelId
     */
    @Test
    public void testDecodeCancelEmptyCancelId()
    {
        Asn1Decoder cancelDecoder = new CancelDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x04 );

        stream.put( new byte[]
            {
                0x30, 0x02,
                  0x02, 0x00
            } ).flip();

        // Allocate a Cancel Container
        IAsn1Container cancelContainer = new CancelContainer();

        // Decode a Cancel message
        try
        {
            cancelDecoder.decode( stream, cancelContainer );
            fail( "CancelID expected" );
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
        }
    }

    
    /**
     * Test a Cancel message with a bad cancelId
     */
    @Test
    public void testDecodeCancelBadCancelId()
    {
        Asn1Decoder cancelDecoder = new CancelDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x08 );

        stream.put( new byte[]
            {
                0x30, 0x06,
                  0x02, 0x04, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF
            } ).flip();

        // Allocate a Cancel Container
        IAsn1Container cancelContainer = new CancelContainer();

        // Decode a Cancel message
        try
        {
            cancelDecoder.decode( stream, cancelContainer );
            fail( "CancelID expected" );
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
        }
    }

    
    /**
     * Test a Cancel message with more than one cancelId
     */
    @Test
    public void testDecodeCancelMoreThanOneCancelId()
    {
        Asn1Decoder cancelDecoder = new CancelDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x08 );

        stream.put( new byte[]
            {
                0x30, 0x06,
                  0x02, 0x01, 0x01,
                  0x02, 0x01, 0x02
            } ).flip();

        // Allocate a Cancel Container
        IAsn1Container cancelContainer = new CancelContainer();

        // Decode a Cancel message
        try
        {
            cancelDecoder.decode( stream, cancelContainer );
            fail( "CancelID expected" );
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
        }
    }
}
