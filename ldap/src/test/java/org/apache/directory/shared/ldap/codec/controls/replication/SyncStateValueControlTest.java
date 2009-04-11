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
package org.apache.directory.shared.ldap.codec.controls.replication;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.codec.controls.replication.syncStateValue.SyncStateValueControlCodec;
import org.apache.directory.shared.ldap.codec.controls.replication.syncStateValue.SyncStateValueControlContainer;
import org.apache.directory.shared.ldap.codec.controls.replication.syncStateValue.SyncStateValueControlDecoder;
import org.apache.directory.shared.ldap.message.control.replication.SyncStateTypeEnum;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.Test;


/**
 * Test the SyncStateControlValue codec
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SyncStateValueControlTest
{
    /**
     * Test the decoding of a SyncStateValue control with a refreshOnly mode
     */
    @Test
    public void testDecodeSyncStateValueControlWithStateType()
    {
        Asn1Decoder decoder = new SyncStateValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 16 );
        bb.put( new byte[]
            { 0x30, ( byte ) 14,               // SyncStateValue ::= SEQUENCE {
                0x0A, 0x01, 0x00,              //     state ENUMERATED {
                                               //         present (0)
                                               //     }
                0x04, 0x03, 'a', 'b', 'c',     //     entryUUID syncUUID OPTIONAL,
                0x04, 0x04, 'x', 'k', 'c', 'd' //     cookie syncCookie OPTIONAL,
            } );
        bb.flip();

        SyncStateValueControlContainer container = new SyncStateValueControlContainer();

        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        SyncStateValueControlCodec SyncStateValue = container.getSyncStateValueControl();
        assertEquals( SyncStateTypeEnum.PRESENT, SyncStateValue.getSyncStateType() );
        assertEquals( "abc", StringTools.utf8ToString( SyncStateValue.getEntryUUID() ) );
        assertEquals( "xkcd", StringTools.utf8ToString( SyncStateValue.getCookie() ) );

        // Check the encoding
        try
        {
            ByteBuffer encoded = SyncStateValue.encode( null );
            encoded.flip();
            bb.flip();
            assertTrue( Arrays.equals( bb.array(), encoded.array() ) );
        }
        catch ( EncoderException ee )
        {
            fail( ee.getMessage() );
        }
    }


    /**
     * Test the decoding of a SyncStateValue control with no cookie
     */
    @Test
    public void testDecodeSyncStateValueControlNoCookie()
    {
        Asn1Decoder decoder = new SyncStateValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 10 );
        bb.put( new byte[]
            { 0x30, 0x08,                 // SyncStateValue ::= SEQUENCE {
                0x0A, 0x01, 0x01,         //     state ENUMERATED {
                                          //         add (1)
                                          //     }
                0x04, 0x03, 'a', 'b', 'c' //     entryUUID syncUUID OPTIONAL
            } );
        bb.flip();

        SyncStateValueControlContainer container = new SyncStateValueControlContainer();

        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            fail( de.getMessage() );
        }

        SyncStateValueControlCodec SyncStateValue = container.getSyncStateValueControl();
        assertEquals( SyncStateTypeEnum.ADD, SyncStateValue.getSyncStateType() );
        assertEquals( "abc", StringTools.utf8ToString( SyncStateValue.getEntryUUID() ) );
        assertNull( SyncStateValue.getCookie() );

        // Check the encoding
        try
        {
            ByteBuffer encoded = SyncStateValue.encode( null );
            encoded.flip();
            bb.flip();
            assertTrue( Arrays.equals( bb.array(), encoded.array() ) );
        }
        catch ( EncoderException ee )
        {
            fail( ee.getMessage() );
        }
    }


    /**
     * Test the decoding of a SyncStateValue control with an empty cookie
     */
    @Test
    public void testDecodeSyncStateValueControlEmptyCookie()
    {
        Asn1Decoder decoder = new SyncStateValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x12 );
        bb.put( new byte[]
            { 0x30, 0x10,                  // SyncStateValue ::= SEQUENCE {
                0x0A, 0x01, 0x02,          //     state ENUMERATED {
                                           //         modify (2)
                                           //     }
                0x04, 0x03, 'a', 'b', 'c', //     entryUUID syncUUID OPTIONAL
                0x04, 0x00                 //     cookie syncCookie OPTIONAL,
            } );
        bb.flip();

        SyncStateValueControlContainer container = new SyncStateValueControlContainer();
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        SyncStateValueControlCodec syncStateValue = container.getSyncStateValueControl();
        assertEquals( SyncStateTypeEnum.MODIFY, syncStateValue.getSyncStateType() );
        assertEquals( "abc", StringTools.utf8ToString( syncStateValue.getEntryUUID() ) );
        assertEquals( "", StringTools.utf8ToString( syncStateValue.getCookie() ) );

        // Check the encoding
        try
        {
            ByteBuffer encoded = syncStateValue.encode( null );
            encoded.flip();
            bb.flip();
            SyncStateValueControlCodec redecoded = container.getSyncStateValueControl();

            assertEquals( syncStateValue.getSyncStateType(), redecoded.getSyncStateType() );
            assertTrue( Arrays.equals( syncStateValue.getCookie(), redecoded.getCookie() ) );
            assertEquals( syncStateValue.getEntryUUID(), redecoded.getEntryUUID() );
        }
        catch ( EncoderException ee )
        {
            fail( ee.getMessage() );
        }
    }


    /**
     * Test the decoding of a SyncStateValue control with an empty sequence
     */
    @Test
    public void testDecodeSyncStateValueControlEmptySequence()
    {
        Asn1Decoder decoder = new SyncStateValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x02 );
        bb.put( new byte[]
            { 0x30, 0x00 // SyncStateValue ::= SEQUENCE {
            } );
        bb.flip();

        SyncStateValueControlContainer container = new SyncStateValueControlContainer();

        try
        {
            decoder.decode( bb, container );
            fail( "we should not get there" );
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
        }
    }


    /**
     * Test the decoding of a SyncStateValue control with no syncState
     */
    @Test
    public void testDecodeSyncStateValueControlNoSyancState()
    {
        Asn1Decoder decoder = new SyncStateValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x07 );
        bb.put( new byte[]
            { 0x30, 0x05,                 // SyncStateValue ::= SEQUENCE {
                0x04, 0x03, 'a', 'b', 'c' //     cookie syncCookie OPTIONAL,
            } );
        bb.flip();

        SyncStateValueControlContainer container = new SyncStateValueControlContainer();

        try
        {
            decoder.decode( bb, container );
            fail( "we should not get there" );
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
        }
    }


    /**
     * Test the decoding of a SyncStateValue control with no syncUUID
     */
    @Test
    public void testDecodeSyncStateValueControlNoSyncUUID()
    {
        Asn1Decoder decoder = new SyncStateValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x05 );
        bb.put( new byte[]
            { 0x30, 0x03,                  // SyncStateValue ::= SEQUENCE {
                0x0A, 0x01, 0x02,          //     state ENUMERATED {
                                           //         modify (2)
                                           //     }
            } );
        bb.flip();

        SyncStateValueControlContainer container = new SyncStateValueControlContainer();

        try
        {
            decoder.decode( bb, container );
            fail( "we should not get there" );
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
        }
    }
}
