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

import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.ldap.codec.controls.replication.syncRequestValue.SyncRequestValueControlCodec;
import org.apache.directory.shared.ldap.codec.controls.replication.syncRequestValue.SyncRequestValueControlContainer;
import org.apache.directory.shared.ldap.codec.controls.replication.syncRequestValue.SyncRequestValueControlDecoder;
import org.apache.directory.shared.ldap.message.control.replication.SynchronizationModeEnum;
import org.apache.directory.shared.ldap.util.StringTools;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test the SyncRequestControlValue codec
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SyncRequestValueControlTest
{
    /**
     * Test the decoding of a SyncRequestValue control with a refreshOnly mode
     */
    @Test
    public void testDecodeSyncRequestValueControlRefreshOnlySuccess()
    {
        Asn1Decoder decoder = new SyncRequestValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x0D );
        bb.put( new byte[]
            { 
            0x30, 0x0B,                     // syncRequestValue ::= SEQUENCE {
              0x0A, 0x01, 0x01,             //     mode ENUMERATED {
                                            //         refreshOnly (1)
                                            //     }
              0x04, 0x03, 'a', 'b', 'c',    //     cookie syncCookie OPTIONAL,
              0x01, 0x01, 0x00              //     reloadHint BOOLEAN DEFAULT FALSE
            } );
        bb.flip();

        SyncRequestValueControlContainer container = new SyncRequestValueControlContainer();
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        SyncRequestValueControlCodec syncRequestValue = container.getSyncRequestValueControl();
        assertEquals( SynchronizationModeEnum.REFRESH_ONLY, syncRequestValue.getMode() );
        assertEquals( "abc", StringTools.utf8ToString( syncRequestValue.getCookie() ) );
        assertEquals( false, syncRequestValue.isReloadHint() );
    }


    /**
     * Test the decoding of a SyncRequestValue control with a refreshAndPersist mode
     */
    @Test
    public void testDecodeSyncRequestValueControlRefreshAndPersistSuccess()
    {
        Asn1Decoder decoder = new SyncRequestValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x0D );
        bb.put( new byte[]
            { 
            0x30, 0x0B,                     // syncRequestValue ::= SEQUENCE {
              0x0A, 0x01, 0x03,             //     mode ENUMERATED {
                                            //         refreshAndPersist (3)
                                            //     }
              0x04, 0x03, 'a', 'b', 'c',    //     cookie syncCookie OPTIONAL,
              0x01, 0x01, 0x00              //     reloadHint BOOLEAN DEFAULT FALSE
            } );
        bb.flip();

        SyncRequestValueControlContainer container = new SyncRequestValueControlContainer();
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        SyncRequestValueControlCodec syncRequestValue = container.getSyncRequestValueControl();
        assertEquals( SynchronizationModeEnum.REFRESH_AND_PERSIST, syncRequestValue.getMode() );
        assertEquals( "abc", StringTools.utf8ToString( syncRequestValue.getCookie() ) );
        assertEquals( false, syncRequestValue.isReloadHint() );
    }


    /**
     * Test the decoding of a SyncRequestValue control with no cookie
     */
    @Test
    public void testDecodeSyncRequestValueControlNoCookie()
    {
        Asn1Decoder decoder = new SyncRequestValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x08 );
        bb.put( new byte[]
            { 
            0x30, 0x06,                     // syncRequestValue ::= SEQUENCE {
              0x0A, 0x01, 0x03,             //     mode ENUMERATED {
                                            //         refreshAndPersist (3)
                                            //     }
              0x01, 0x01, 0x00              //     reloadHint BOOLEAN DEFAULT FALSE
            } );
        bb.flip();

        SyncRequestValueControlContainer container = new SyncRequestValueControlContainer();
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        SyncRequestValueControlCodec syncRequestValue = container.getSyncRequestValueControl();
        assertEquals( SynchronizationModeEnum.REFRESH_AND_PERSIST, syncRequestValue.getMode() );
        assertNull( syncRequestValue.getCookie() );
        assertEquals( false, syncRequestValue.isReloadHint() );
    }


    /**
     * Test the decoding of a SyncRequestValue control with no cookie, a true
     * reloadHint
     */
    @Test
    public void testDecodeSyncRequestValueControlNoCookieReloadHintTrue()
    {
        Asn1Decoder decoder = new SyncRequestValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x08 );
        bb.put( new byte[]
            { 
            0x30, 0x06,                     // syncRequestValue ::= SEQUENCE {
              0x0A, 0x01, 0x03,             //     mode ENUMERATED {
                                            //         refreshAndPersist (3)
                                            //     }
              0x01, 0x01, (byte)0xFF        //     reloadHint BOOLEAN DEFAULT FALSE
            } );
        bb.flip();

        SyncRequestValueControlContainer container = new SyncRequestValueControlContainer();
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        SyncRequestValueControlCodec syncRequestValue = container.getSyncRequestValueControl();
        assertEquals( SynchronizationModeEnum.REFRESH_AND_PERSIST, syncRequestValue.getMode() );
        assertNull( syncRequestValue.getCookie() );
        assertEquals( true, syncRequestValue.isReloadHint() );
    }


    /**
     * Test the decoding of a SyncRequestValue control with no cookie, no
     * reloadHint
     */
    @Test
    public void testDecodeSyncRequestValueControlNoCookieNoReloadHint()
    {
        Asn1Decoder decoder = new SyncRequestValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x05 );
        bb.put( new byte[]
            { 
            0x30, 0x03,                     // syncRequestValue ::= SEQUENCE {
              0x0A, 0x01, 0x03              //     mode ENUMERATED {
                                            //         refreshAndPersist (3)
                                            //     }
            } );
        bb.flip();

        SyncRequestValueControlContainer container = new SyncRequestValueControlContainer();
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        SyncRequestValueControlCodec syncRequestValue = container.getSyncRequestValueControl();
        assertEquals( SynchronizationModeEnum.REFRESH_AND_PERSIST, syncRequestValue.getMode() );
        assertNull( syncRequestValue.getCookie() );
        assertEquals( false, syncRequestValue.isReloadHint() );
    }


    /**
     * Test the decoding of a SyncRequestValue control with no reloadHint
     */
    @Test
    public void testDecodeSyncRequestValueControlNoReloadHintSuccess()
    {
        Asn1Decoder decoder = new SyncRequestValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x0D );
        bb.put( new byte[]
            { 
            0x30, 0x08,                     // syncRequestValue ::= SEQUENCE {
              0x0A, 0x01, 0x03,             //     mode ENUMERATED {
                                            //         refreshAndPersist (3)
                                            //     }
              0x04, 0x03, 'a', 'b', 'c'     //     cookie syncCookie OPTIONAL,
            } );
        bb.flip();

        SyncRequestValueControlContainer container = new SyncRequestValueControlContainer();
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        SyncRequestValueControlCodec syncRequestValue = container.getSyncRequestValueControl();
        assertEquals( SynchronizationModeEnum.REFRESH_AND_PERSIST, syncRequestValue.getMode() );
        assertEquals( "abc", StringTools.utf8ToString( syncRequestValue.getCookie() ) );
        assertEquals( false, syncRequestValue.isReloadHint() );
    }


    /**
     * Test the decoding of a SyncRequestValue control with an empty cookie
     */
    @Test
    public void testDecodeSyncRequestValueControlEmptyCookie()
    {
        Asn1Decoder decoder = new SyncRequestValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x07 );
        bb.put( new byte[]
            { 
            0x30, 0x05,                     // syncRequestValue ::= SEQUENCE {
              0x0A, 0x01, 0x03,             //     mode ENUMERATED {
                                            //         refreshAndPersist (3)
                                            //     }
              0x04, 0x00,                   //     cookie syncCookie OPTIONAL,
            } );
        bb.flip();

        SyncRequestValueControlContainer container = new SyncRequestValueControlContainer();
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        SyncRequestValueControlCodec syncRequestValue = container.getSyncRequestValueControl();
        assertEquals( SynchronizationModeEnum.REFRESH_AND_PERSIST, syncRequestValue.getMode() );
        assertEquals( "", StringTools.utf8ToString( syncRequestValue.getCookie() ) );
        assertEquals( false, syncRequestValue.isReloadHint() );
    }


    /**
     * Test the decoding of a SyncRequestValue control with an empty sequence
     */
    @Test
    public void testDecodeSyncRequestValueControlEmptySequence()
    {
        Asn1Decoder decoder = new SyncRequestValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x02 );
        bb.put( new byte[]
            { 
            0x30, 0x00                      // syncRequestValue ::= SEQUENCE {
            } );
        bb.flip();

        SyncRequestValueControlContainer container = new SyncRequestValueControlContainer();

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
     * Test the decoding of a SyncRequestValue control with no mode
     */
    @Test
    public void testDecodeSyncRequestValueControlNoMode()
    {
        Asn1Decoder decoder = new SyncRequestValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x07 );
        bb.put( new byte[]
            { 
            0x30, 0x05,                     // syncRequestValue ::= SEQUENCE {
              0x04, 0x03, 'a', 'b', 'c'     //     cookie syncCookie OPTIONAL,
            } );
        bb.flip();

        SyncRequestValueControlContainer container = new SyncRequestValueControlContainer();

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
