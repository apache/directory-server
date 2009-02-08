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
import java.util.Arrays;

import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.codec.controls.replication.syncInfoValue.SyncInfoValueControlCodec;
import org.apache.directory.shared.ldap.codec.controls.replication.syncInfoValue.SyncInfoValueControlContainer;
import org.apache.directory.shared.ldap.codec.controls.replication.syncInfoValue.SyncInfoValueControlDecoder;
import org.apache.directory.shared.ldap.message.control.replication.SynchronizationInfoEnum;
import org.apache.directory.shared.ldap.util.StringTools;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test the SyncInfoControlValue codec
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SyncInfoValueControlTest
{
    //--------------------------------------------------------------------------------
    // NewCookie choice tests
    //--------------------------------------------------------------------------------
    /**
     * Test the decoding of a SyncInfoValue control, newCookie choice
     */
    @Test
    public void testDecodeSyncInfoValueControlNewCookie()
    {
        Asn1Decoder decoder = new SyncInfoValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x05 );
        bb.put( new byte[]
            { 
            (byte)0x80, 0x03,               // syncInfoValue ::= CHOICE {
              'a', 'b', 'c'                 //     newCookie [0] syncCookie
            } );
        bb.flip();

        SyncInfoValueControlContainer container = new SyncInfoValueControlContainer();
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        SyncInfoValueControlCodec syncInfoValue = container.getSyncInfoValueControl();
        assertEquals( SynchronizationInfoEnum.NEW_COOKIE, syncInfoValue.getType() );
        assertEquals( "abc", StringTools.utf8ToString( syncInfoValue.getCookie() ) );
        
        // Check the encoding
        try
        {
            ByteBuffer encoded = syncInfoValue.encode( null );
            encoded.flip();
            bb.flip();
            assertTrue( Arrays.equals( bb.array(), encoded.array() ) );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }
    
    
    /**
     * Test the decoding of a SyncInfoValue control, empty newCookie choice
     */
    @Test
    public void testDecodeSyncInfoValueControlEmptyNewCookie()
    {
        Asn1Decoder decoder = new SyncInfoValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x02 );
        bb.put( new byte[]
            { 
            (byte)0x80, 0x00,               // syncInfoValue ::= CHOICE {
                                            //     newCookie [0] syncCookie
            } );
        bb.flip();

        SyncInfoValueControlContainer container = new SyncInfoValueControlContainer();
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        SyncInfoValueControlCodec syncInfoValue = container.getSyncInfoValueControl();
        assertEquals( SynchronizationInfoEnum.NEW_COOKIE, syncInfoValue.getType() );
        assertEquals( "", StringTools.utf8ToString( syncInfoValue.getCookie() ) );
        
        // Check the encoding
        try
        {
            ByteBuffer encoded = syncInfoValue.encode( null );
            encoded.flip();
            bb.flip();
            assertTrue( Arrays.equals( bb.array(), encoded.array() ) );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }
    
    
    //--------------------------------------------------------------------------------
    // RefreshDelete choice tests
    //--------------------------------------------------------------------------------
    /**
     * Test the decoding of a SyncInfoValue control, refreshDelete choice,
     * refreshDone = true
     */
    @Test
    public void testDecodeSyncInfoValueControlRefreshDelete()
    {
        Asn1Decoder decoder = new SyncInfoValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x0A );
        bb.put( new byte[]
            { 
            (byte)0xA1, 0x08,               // syncInfoValue ::= CHOICE {
                                            //     refreshDelete [1] SEQUENCE {
              0x04, 0x03, 'a', 'b', 'c',    //         cookie       syncCookie OPTIONAL,
              0x01, 0x01, (byte)0xFF        //         refreshDone  BOOLEAN DEFAULT TRUE
            } );
        bb.flip();

        SyncInfoValueControlContainer container = new SyncInfoValueControlContainer();
        
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        SyncInfoValueControlCodec syncInfoValue = container.getSyncInfoValueControl();
        assertEquals( SynchronizationInfoEnum.REFRESH_DELETE, syncInfoValue.getType() );
        assertEquals( "abc", StringTools.utf8ToString( syncInfoValue.getCookie() ) );
        assertTrue( syncInfoValue.isRefreshDone() );
        
        // Check the encoding
        try
        {
            ByteBuffer encoded = syncInfoValue.encode( null );
            encoded.flip();
            bb.flip();
            assertTrue( Arrays.equals( bb.array(), encoded.array() ) );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }
    
    
    /**
     * Test the decoding of a SyncInfoValue control, refreshDelete choice,
     * refreshDone = false
     */
    @Test
    public void testDecodeSyncInfoValueControlRefreshDeleteRefreshDoneFalse()
    {
        Asn1Decoder decoder = new SyncInfoValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x0A );
        bb.put( new byte[]
            { 
            (byte)0xA1, 0x08,               // syncInfoValue ::= CHOICE {
                                            //     refreshDelete [1] SEQUENCE {
              0x04, 0x03, 'a', 'b', 'c',    //         cookie       syncCookie OPTIONAL,
              0x01, 0x01, (byte)0x00        //         refreshDone  BOOLEAN DEFAULT TRUE
            } );
        bb.flip();

        SyncInfoValueControlContainer container = new SyncInfoValueControlContainer();
        
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        SyncInfoValueControlCodec syncInfoValue = container.getSyncInfoValueControl();
        assertEquals( SynchronizationInfoEnum.REFRESH_DELETE, syncInfoValue.getType() );
        assertEquals( "abc", StringTools.utf8ToString( syncInfoValue.getCookie() ) );
        assertFalse( syncInfoValue.isRefreshDone() );
        
        // Check the encoding
        try
        {
            ByteBuffer encoded = syncInfoValue.encode( null );
            encoded.flip();
            bb.flip();
            assertTrue( Arrays.equals( bb.array(), encoded.array() ) );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }
    
    
    /**
     * Test the decoding of a SyncInfoValue control, refreshDelete choice,
     * no refreshDone
     */
    @Test
    public void testDecodeSyncInfoValueControlRefreshDeleteNoRefreshDone()
    {
        Asn1Decoder decoder = new SyncInfoValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x07 );
        bb.put( new byte[]
            { 
            (byte)0xA1, 0x05,               // syncInfoValue ::= CHOICE {
                                            //     refreshDelete [1] SEQUENCE {
              0x04, 0x03, 'a', 'b', 'c'     //         cookie       syncCookie OPTIONAL,
            } );
        bb.flip();

        SyncInfoValueControlContainer container = new SyncInfoValueControlContainer();
        
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        SyncInfoValueControlCodec syncInfoValue = container.getSyncInfoValueControl();
        assertEquals( SynchronizationInfoEnum.REFRESH_DELETE, syncInfoValue.getType() );
        assertEquals( "abc", StringTools.utf8ToString( syncInfoValue.getCookie() ) );
        assertTrue( syncInfoValue.isRefreshDone() );
        
        // Check the encoding
        try
        {
            ByteBuffer encoded = syncInfoValue.encode( null );
            encoded.flip();
            bb.flip();
            
            container.clean();
            
            try
            {
                decoder.decode( encoded, container );
            }
            catch ( DecoderException de )
            {
                de.printStackTrace();
                fail( de.getMessage() );
            }
            
            SyncInfoValueControlCodec redecoded = container.getSyncInfoValueControl();
            
            assertTrue( Arrays.equals( syncInfoValue.getCookie(), redecoded.getCookie() ) );
            assertEquals( syncInfoValue.isRefreshDone(), redecoded.isRefreshDone() );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }
    
    
    /**
     * Test the decoding of a SyncInfoValue control, refreshDelete choice,
     * no cookie
     */
    @Test
    public void testDecodeSyncInfoValueControlRefreshDeleteNoCookie()
    {
        Asn1Decoder decoder = new SyncInfoValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x05 );
        bb.put( new byte[]
            { 
            (byte)0xA1, 0x03,               // syncInfoValue ::= CHOICE {
                                            //     refreshDelete [1] SEQUENCE {
              0x01, 0x01, 0x00              //        refreshDone  BOOLEAN DEFAULT TRUE
            } );
        bb.flip();

        SyncInfoValueControlContainer container = new SyncInfoValueControlContainer();
        
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        SyncInfoValueControlCodec syncInfoValue = container.getSyncInfoValueControl();
        assertEquals( SynchronizationInfoEnum.REFRESH_DELETE, syncInfoValue.getType() );
        assertEquals( "", StringTools.utf8ToString( syncInfoValue.getCookie() ) );
        assertFalse( syncInfoValue.isRefreshDone() );
        
        // Check the encoding
        try
        {
            ByteBuffer encoded = syncInfoValue.encode( null );
            encoded.flip();
            bb.flip();
            
            container.clean();
            
            try
            {
                decoder.decode( encoded, container );
            }
            catch ( DecoderException de )
            {
                de.printStackTrace();
                fail( de.getMessage() );
            }
            
            SyncInfoValueControlCodec redecoded = container.getSyncInfoValueControl();
            
            assertTrue( Arrays.equals( syncInfoValue.getCookie(), redecoded.getCookie() ) );
            assertEquals( syncInfoValue.isRefreshDone(), redecoded.isRefreshDone() );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }
    
    
    /**
     * Test the decoding of a SyncInfoValue control, refreshDelete choice,
     * no cookie, no refreshDone
     */
    @Test
    public void testDecodeSyncInfoValueControlRefreshDeleteNoCookieNoRefreshDone()
    {
        Asn1Decoder decoder = new SyncInfoValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x02 );
        bb.put( new byte[]
            { 
            (byte)0xA1, 0x00                // syncInfoValue ::= CHOICE {
            } );
        bb.flip();

        SyncInfoValueControlContainer container = new SyncInfoValueControlContainer();
        
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        SyncInfoValueControlCodec syncInfoValue = container.getSyncInfoValueControl();
        assertEquals( SynchronizationInfoEnum.REFRESH_DELETE, syncInfoValue.getType() );
        assertEquals( "", StringTools.utf8ToString( syncInfoValue.getCookie() ) );
        assertTrue( syncInfoValue.isRefreshDone() );
        
        // Check the encoding
        try
        {
            ByteBuffer encoded = syncInfoValue.encode( null );
            encoded.flip();
            bb.flip();
            
            container.clean();
            
            try
            {
                decoder.decode( encoded, container );
            }
            catch ( DecoderException de )
            {
                de.printStackTrace();
                fail( de.getMessage() );
            }
            
            SyncInfoValueControlCodec redecoded = container.getSyncInfoValueControl();
            
            assertTrue( Arrays.equals( syncInfoValue.getCookie(), redecoded.getCookie() ) );
            assertEquals( syncInfoValue.isRefreshDone(), redecoded.isRefreshDone() );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }
    
    
    //--------------------------------------------------------------------------------
    // RefreshPresent choice tests
    //--------------------------------------------------------------------------------
    /**
     * Test the decoding of a SyncInfoValue control, refreshPresent choice,
     * refreshDone = true
     */
    @Test
    public void testDecodeSyncInfoValueControlRefreshPresent()
    {
        Asn1Decoder decoder = new SyncInfoValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x0A );
        bb.put( new byte[]
            { 
            (byte)0xA2, 0x08,               // syncInfoValue ::= CHOICE {
                                            //     refreshPresent [2] SEQUENCE {
              0x04, 0x03, 'a', 'b', 'c',    //         cookie       syncCookie OPTIONAL,
              0x01, 0x01, (byte)0xFF        //         refreshDone  BOOLEAN DEFAULT TRUE
            } );
        bb.flip();

        SyncInfoValueControlContainer container = new SyncInfoValueControlContainer();
        
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        SyncInfoValueControlCodec syncInfoValue = container.getSyncInfoValueControl();
        assertEquals( SynchronizationInfoEnum.REFRESH_PRESENT, syncInfoValue.getType() );
        assertEquals( "abc", StringTools.utf8ToString( syncInfoValue.getCookie() ) );
        assertTrue( syncInfoValue.isRefreshDone() );
        
        // Check the encoding
        try
        {
            ByteBuffer encoded = syncInfoValue.encode( null );
            encoded.flip();
            bb.flip();
            assertTrue( Arrays.equals( bb.array(), encoded.array() ) );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }
    
    
    /**
     * Test the decoding of a SyncInfoValue control, refreshPresent choice,
     * refreshDone = false
     */
    @Test
    public void testDecodeSyncInfoValueControlRefreshPresentRefreshDoneFalse()
    {
        Asn1Decoder decoder = new SyncInfoValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x0A );
        bb.put( new byte[]
            { 
            (byte)0xA2, 0x08,               // syncInfoValue ::= CHOICE {
                                            //     refreshPresent [2] SEQUENCE {
              0x04, 0x03, 'a', 'b', 'c',    //         cookie       syncCookie OPTIONAL,
              0x01, 0x01, (byte)0x00        //         refreshDone  BOOLEAN DEFAULT TRUE
            } );
        bb.flip();

        SyncInfoValueControlContainer container = new SyncInfoValueControlContainer();
        
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        SyncInfoValueControlCodec syncInfoValue = container.getSyncInfoValueControl();
        assertEquals( SynchronizationInfoEnum.REFRESH_PRESENT, syncInfoValue.getType() );
        assertEquals( "abc", StringTools.utf8ToString( syncInfoValue.getCookie() ) );
        assertFalse( syncInfoValue.isRefreshDone() );
        
        // Check the encoding
        try
        {
            ByteBuffer encoded = syncInfoValue.encode( null );
            encoded.flip();
            bb.flip();
            assertTrue( Arrays.equals( bb.array(), encoded.array() ) );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }
    
    
    /**
     * Test the decoding of a SyncInfoValue control, refreshPresent choice,
     * no refreshDone
     */
    @Test
    public void testDecodeSyncInfoValueControlRefreshPresentNoRefreshDone()
    {
        Asn1Decoder decoder = new SyncInfoValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x07 );
        bb.put( new byte[]
            { 
            (byte)0xA2, 0x05,               // syncInfoValue ::= CHOICE {
                                            //     refreshPresent [2] SEQUENCE {
              0x04, 0x03, 'a', 'b', 'c'     //         cookie       syncCookie OPTIONAL,
            } );
        bb.flip();

        SyncInfoValueControlContainer container = new SyncInfoValueControlContainer();
        
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        SyncInfoValueControlCodec syncInfoValue = container.getSyncInfoValueControl();
        assertEquals( SynchronizationInfoEnum.REFRESH_PRESENT, syncInfoValue.getType() );
        assertEquals( "abc", StringTools.utf8ToString( syncInfoValue.getCookie() ) );
        assertTrue( syncInfoValue.isRefreshDone() );
        
        // Check the encoding
        try
        {
            ByteBuffer encoded = syncInfoValue.encode( null );
            encoded.flip();
            bb.flip();
            
            container.clean();
            
            try
            {
                decoder.decode( encoded, container );
            }
            catch ( DecoderException de )
            {
                de.printStackTrace();
                fail( de.getMessage() );
            }
            
            SyncInfoValueControlCodec redecoded = container.getSyncInfoValueControl();
            
            assertTrue( Arrays.equals( syncInfoValue.getCookie(), redecoded.getCookie() ) );
            assertEquals( syncInfoValue.isRefreshDone(), redecoded.isRefreshDone() );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }
    
    
    /**
     * Test the decoding of a SyncInfoValue control, refreshPresent choice,
     * no cookie
     */
    @Test
    public void testDecodeSyncInfoValueControlRefreshPresentNoCookie()
    {
        Asn1Decoder decoder = new SyncInfoValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x05 );
        bb.put( new byte[]
            { 
            (byte)0xA2, 0x03,               // syncInfoValue ::= CHOICE {
                                            //     refreshPresent [2] SEQUENCE {
              0x01, 0x01, 0x00              //        refreshDone  BOOLEAN DEFAULT TRUE
            } );
        bb.flip();

        SyncInfoValueControlContainer container = new SyncInfoValueControlContainer();
        
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        SyncInfoValueControlCodec syncInfoValue = container.getSyncInfoValueControl();
        assertEquals( SynchronizationInfoEnum.REFRESH_PRESENT, syncInfoValue.getType() );
        assertEquals( "", StringTools.utf8ToString( syncInfoValue.getCookie() ) );
        assertFalse( syncInfoValue.isRefreshDone() );
        
        // Check the encoding
        try
        {
            ByteBuffer encoded = syncInfoValue.encode( null );
            encoded.flip();
            bb.flip();
            
            container.clean();
            
            try
            {
                decoder.decode( encoded, container );
            }
            catch ( DecoderException de )
            {
                de.printStackTrace();
                fail( de.getMessage() );
            }
            
            SyncInfoValueControlCodec redecoded = container.getSyncInfoValueControl();
            
            assertTrue( Arrays.equals( syncInfoValue.getCookie(), redecoded.getCookie() ) );
            assertEquals( syncInfoValue.isRefreshDone(), redecoded.isRefreshDone() );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }
    
    
    /**
     * Test the decoding of a SyncInfoValue control, refreshPresent choice,
     * no cookie, no refreshDone
     */
    @Test
    public void testDecodeSyncInfoValueControlRefreshPresentNoCookieNoRefreshDone()
    {
        Asn1Decoder decoder = new SyncInfoValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x02 );
        bb.put( new byte[]
            { 
            (byte)0xA2, 0x00                // syncInfoValue ::= CHOICE {
            } );
        bb.flip();

        SyncInfoValueControlContainer container = new SyncInfoValueControlContainer();
        
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        SyncInfoValueControlCodec syncInfoValue = container.getSyncInfoValueControl();
        assertEquals( SynchronizationInfoEnum.REFRESH_PRESENT, syncInfoValue.getType() );
        assertEquals( "", StringTools.utf8ToString( syncInfoValue.getCookie() ) );
        assertTrue( syncInfoValue.isRefreshDone() );
        
        // Check the encoding
        try
        {
            ByteBuffer encoded = syncInfoValue.encode( null );
            encoded.flip();
            bb.flip();
            
            container.clean();
            
            try
            {
                decoder.decode( encoded, container );
            }
            catch ( DecoderException de )
            {
                de.printStackTrace();
                fail( de.getMessage() );
            }
            
            SyncInfoValueControlCodec redecoded = container.getSyncInfoValueControl();
            
            assertTrue( Arrays.equals( syncInfoValue.getCookie(), redecoded.getCookie() ) );
            assertEquals( syncInfoValue.isRefreshDone(), redecoded.isRefreshDone() );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }
    
    
    //--------------------------------------------------------------------------------
    // syncIdSet choice tests
    //--------------------------------------------------------------------------------
    /**
     * Test the decoding of a SyncInfoValue control, syncIdSet choice, empty
     */
    @Test
    public void testDecodeSyncInfoValueControlSyncIdSetEmpty()
    {
        Asn1Decoder decoder = new SyncInfoValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x02 );
        bb.put( new byte[]
            { 
            (byte)0xA3, 0x00,               // syncInfoValue ::= CHOICE {
                                            //     syncIdSet [3] SEQUENCE {
            } );
        bb.flip();

        SyncInfoValueControlContainer container = new SyncInfoValueControlContainer();
        
        try
        {
            decoder.decode( bb, container );
            fail( "Should not get there" );
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
        }
    }
    
    
    /**
     * Test the decoding of a SyncInfoValue control, syncIdSet choice, cookie
     * but no UUID set
     */
    @Test
    public void testDecodeSyncInfoValueControlSyncIdSetCookieNoSet()
    {
        Asn1Decoder decoder = new SyncInfoValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x07 );
        bb.put( new byte[]
            { 
            (byte)0xA3, 0x05,               // syncInfoValue ::= CHOICE {
                                            //     syncIdSet [3] SEQUENCE {
              0x04, 0x03, 'a', 'b', 'c',    //         cookie       syncCookie OPTIONAL,
            } );
        bb.flip();

        SyncInfoValueControlContainer container = new SyncInfoValueControlContainer();
        
        try
        {
            decoder.decode( bb, container );
            fail( "Should not get there" );
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
        }
    }
    
    
    /**
     * Test the decoding of a SyncInfoValue control, syncIdSet choice, no cookie
     * a refreshDeletes flag, but no UUID set
     */
    @Test
    public void testDecodeSyncInfoValueControlSyncIdSetNoCookieRefreshDeletesNoSet()
    {
        Asn1Decoder decoder = new SyncInfoValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x05 );
        bb.put( new byte[]
            { 
            (byte)0xA3, 0x03,               // syncInfoValue ::= CHOICE {
                                            //     syncIdSet [3] SEQUENCE {
              0x01, 0x01, 0x00,             //         refreshDeletes BOOLEAN DEFAULT FALSE,
            } );
        bb.flip();

        SyncInfoValueControlContainer container = new SyncInfoValueControlContainer();
        
        try
        {
            decoder.decode( bb, container );
            fail( "Should not get there" );
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
        }
    }
    
    
    /**
     * Test the decoding of a SyncInfoValue control, syncIdSet choice, a cookie
     * a refreshDeletes flag, but no UUID set
     */
    @Test
    public void testDecodeSyncInfoValueControlSyncIdSetCookieRefreshDeletesNoSet()
    {
        Asn1Decoder decoder = new SyncInfoValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x0A );
        bb.put( new byte[]
            { 
            (byte)0xA3, 0x08,               // syncInfoValue ::= CHOICE {
                                            //     syncIdSet [3] SEQUENCE {
              0x04, 0x03, 'a', 'b', 'c',    //         cookie         syncCookie OPTIONAL,
              0x01, 0x01, 0x00,             //         refreshDeletes BOOLEAN DEFAULT FALSE,
            } );
        bb.flip();

        SyncInfoValueControlContainer container = new SyncInfoValueControlContainer();
        
        try
        {
            decoder.decode( bb, container );
            fail( "Should not get there" );
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
        }
    }

    
    /**
     * Test the decoding of a SyncInfoValue control, syncIdSet choice, no cookie
     * no refreshDeletes flag, an empty UUID set
     */
    @Test
    public void testDecodeSyncInfoValueControlSyncIdSetNoCookieNoRefreshDeletesEmptySet()
    {
        Asn1Decoder decoder = new SyncInfoValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x04 );
        bb.put( new byte[]
            { 
            (byte)0xA3, 0x02,               // syncInfoValue ::= CHOICE {
                                            //     syncIdSet [3] SEQUENCE {
              0x31, 0x00,                   //         syncUUIDs SET OF syncUUID
            } );
        bb.flip();

        SyncInfoValueControlContainer container = new SyncInfoValueControlContainer();
        
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }
        SyncInfoValueControlCodec syncInfoValue = container.getSyncInfoValueControl();
        assertEquals( SynchronizationInfoEnum.SYNC_ID_SET, syncInfoValue.getType() );
        assertEquals( "", StringTools.utf8ToString( syncInfoValue.getCookie() ) );
        assertFalse( syncInfoValue.isRefreshDeletes() );
        assertEquals( 0, syncInfoValue.getSyncUUIDs().size() );
        
        // Check the encoding
        try
        {
            ByteBuffer encoded = syncInfoValue.encode( null );
            encoded.flip();
            bb.flip();
            
            container.clean();
            
            try
            {
                decoder.decode( encoded, container );
            }
            catch ( DecoderException de )
            {
                de.printStackTrace();
                fail( de.getMessage() );
            }
            
            SyncInfoValueControlCodec redecoded = container.getSyncInfoValueControl();
            
            assertTrue( Arrays.equals( syncInfoValue.getCookie(), redecoded.getCookie() ) );
            assertEquals( syncInfoValue.isRefreshDeletes(), redecoded.isRefreshDeletes() );
            assertEquals( 0, redecoded.getSyncUUIDs().size() );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }

    
    /**
     * Test the decoding of a SyncInfoValue control, syncIdSet choice, no cookie
     * no refreshDeletes flag, a UUID set with some values
     */
    @Test
    public void testDecodeSyncInfoValueControlSyncIdSetNoCookieNoRefreshDeletesUUIDsSet()
    {
        Asn1Decoder decoder = new SyncInfoValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x3A );
        bb.put( new byte[]
            { 
            (byte)0xA3, 0x38,               // syncInfoValue ::= CHOICE {
                                            //     syncIdSet [3] SEQUENCE {
              0x31, 0x36,                   //         syncUUIDs SET OF syncUUID
                0x04, 0x10,                 // syncUUID
                  0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                  0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                0x04, 0x10,                 // syncUUID
                  0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02,
                  0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02,
                0x04, 0x10,                 // syncUUID
                  0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03,
                  0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03
            } );
        bb.flip();

        SyncInfoValueControlContainer container = new SyncInfoValueControlContainer();
        
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }
        SyncInfoValueControlCodec syncInfoValue = container.getSyncInfoValueControl();
        assertEquals( SynchronizationInfoEnum.SYNC_ID_SET, syncInfoValue.getType() );
        assertEquals( "", StringTools.utf8ToString( syncInfoValue.getCookie() ) );
        assertFalse( syncInfoValue.isRefreshDeletes() );
        assertEquals( 3, syncInfoValue.getSyncUUIDs().size() );
        
        for ( int i = 0; i < 3; i++ )
        {
            byte[] uuid = syncInfoValue.getSyncUUIDs().get( i );
            
            for ( int j = 0; j < 16; j++ )
            {
                assertEquals( i + 1, uuid[j] );
            }
        }
        
        // Check the encoding
        try
        {
            ByteBuffer encoded = syncInfoValue.encode( null );
            encoded.flip();
            bb.flip();
            
            container.clean();
            
            try
            {
                decoder.decode( encoded, container );
            }
            catch ( DecoderException de )
            {
                de.printStackTrace();
                fail( de.getMessage() );
            }
            
            SyncInfoValueControlCodec redecoded = container.getSyncInfoValueControl();
            
            assertTrue( Arrays.equals( syncInfoValue.getCookie(), redecoded.getCookie() ) );
            assertEquals( syncInfoValue.isRefreshDeletes(), redecoded.isRefreshDeletes() );
            assertEquals( 3, redecoded.getSyncUUIDs().size() );
            
            for ( int i = 0; i < 3; i++ )
            {
                byte[] uuid = redecoded.getSyncUUIDs().get( i );
                
                for ( int j = 0; j < 16; j++ )
                {
                    assertEquals( i + 1, uuid[j] );
                }
            }
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }

    
    /**
     * Test the decoding of a SyncInfoValue control, syncIdSet choice, A cookie
     * no refreshDeletes flag, an empty UUID set
     */
    @Test
    public void testDecodeSyncInfoValueControlSyncIdSetCookieNoRefreshDeletesEmptySet()
    {
        Asn1Decoder decoder = new SyncInfoValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x09 );
        bb.put( new byte[]
            { 
            (byte)0xA3, 0x07,               // syncInfoValue ::= CHOICE {
                                            //     syncIdSet [3] SEQUENCE {
              0X04, 0X03, 'a', 'b', 'c',    //         cookie         syncCookie OPTIONAL,
              0x31, 0x00,                   //         syncUUIDs SET OF syncUUID
            } );
        bb.flip();

        SyncInfoValueControlContainer container = new SyncInfoValueControlContainer();
        
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }
        SyncInfoValueControlCodec syncInfoValue = container.getSyncInfoValueControl();
        assertEquals( SynchronizationInfoEnum.SYNC_ID_SET, syncInfoValue.getType() );
        assertEquals( "abc", StringTools.utf8ToString( syncInfoValue.getCookie() ) );
        assertFalse( syncInfoValue.isRefreshDeletes() );
        assertEquals( 0, syncInfoValue.getSyncUUIDs().size() );
        
        // Check the encoding
        try
        {
            ByteBuffer encoded = syncInfoValue.encode( null );
            encoded.flip();
            bb.flip();
            
            container.clean();
            
            try
            {
                decoder.decode( encoded, container );
            }
            catch ( DecoderException de )
            {
                de.printStackTrace();
                fail( de.getMessage() );
            }
            
            SyncInfoValueControlCodec redecoded = container.getSyncInfoValueControl();
            
            assertTrue( Arrays.equals( syncInfoValue.getCookie(), redecoded.getCookie() ) );
            assertEquals( syncInfoValue.isRefreshDeletes(), redecoded.isRefreshDeletes() );
            assertEquals( 0, redecoded.getSyncUUIDs().size() );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }

    
    /**
     * Test the decoding of a SyncInfoValue control, syncIdSet choice, a cookie
     * no refreshDeletes flag, a UUID set with some values
     */
    @Test
    public void testDecodeSyncInfoValueControlSyncIdSetCookieNoRefreshDeletesUUIDsSet()
    {
        Asn1Decoder decoder = new SyncInfoValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x3F );
        bb.put( new byte[]
            { 
            (byte)0xA3, 0x3D,               // syncInfoValue ::= CHOICE {
                                            //     syncIdSet [3] SEQUENCE {
              0X04, 0X03, 'a', 'b', 'c',    //         cookie         syncCookie OPTIONAL,
              0x31, 0x36,                   //         syncUUIDs SET OF syncUUID
                0x04, 0x10,                 // syncUUID
                  0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                  0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                0x04, 0x10,                 // syncUUID
                  0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02,
                  0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02,
                0x04, 0x10,                 // syncUUID
                  0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03,
                  0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03
            } );
        bb.flip();

        SyncInfoValueControlContainer container = new SyncInfoValueControlContainer();
        
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }
        SyncInfoValueControlCodec syncInfoValue = container.getSyncInfoValueControl();
        assertEquals( SynchronizationInfoEnum.SYNC_ID_SET, syncInfoValue.getType() );
        assertEquals( "abc", StringTools.utf8ToString( syncInfoValue.getCookie() ) );
        assertFalse( syncInfoValue.isRefreshDeletes() );
        assertEquals( 3, syncInfoValue.getSyncUUIDs().size() );
        
        for ( int i = 0; i < 3; i++ )
        {
            byte[] uuid = syncInfoValue.getSyncUUIDs().get( i );
            
            for ( int j = 0; j < 16; j++ )
            {
                assertEquals( i + 1, uuid[j] );
            }
        }
        
        // Check the encoding
        try
        {
            ByteBuffer encoded = syncInfoValue.encode( null );
            encoded.flip();
            bb.flip();
            
            container.clean();
            
            try
            {
                decoder.decode( encoded, container );
            }
            catch ( DecoderException de )
            {
                de.printStackTrace();
                fail( de.getMessage() );
            }
            
            SyncInfoValueControlCodec redecoded = container.getSyncInfoValueControl();
            
            assertTrue( Arrays.equals( syncInfoValue.getCookie(), redecoded.getCookie() ) );
            assertEquals( syncInfoValue.isRefreshDeletes(), redecoded.isRefreshDeletes() );
            assertEquals( 3, redecoded.getSyncUUIDs().size() );
            
            for ( int i = 0; i < 3; i++ )
            {
                byte[] uuid = redecoded.getSyncUUIDs().get( i );
                
                for ( int j = 0; j < 16; j++ )
                {
                    assertEquals( i + 1, uuid[j] );
                }
            }
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }

    
    /**
     * Test the decoding of a SyncInfoValue control, syncIdSet choice, no cookie
     * a refreshDeletes flag, an empty UUID set
     */
    @Test
    public void testDecodeSyncInfoValueControlSyncIdSetNoCookieRefreshDeletesEmptySet()
    {
        Asn1Decoder decoder = new SyncInfoValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x07 );
        bb.put( new byte[]
            { 
            (byte)0xA3, 0x05,               // syncInfoValue ::= CHOICE {
                                            //     syncIdSet [3] SEQUENCE {
              0X01, 0X01, 0x10,             //         refreshDeletes BOOLEAN DEFAULT FALSE,
              0x31, 0x00,                   //         syncUUIDs SET OF syncUUID
            } );
        bb.flip();

        SyncInfoValueControlContainer container = new SyncInfoValueControlContainer();
        
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }
        SyncInfoValueControlCodec syncInfoValue = container.getSyncInfoValueControl();
        assertEquals( SynchronizationInfoEnum.SYNC_ID_SET, syncInfoValue.getType() );
        assertEquals( "", StringTools.utf8ToString( syncInfoValue.getCookie() ) );
        assertTrue( syncInfoValue.isRefreshDeletes() );
        assertEquals( 0, syncInfoValue.getSyncUUIDs().size() );
        
        // Check the encoding
        try
        {
            ByteBuffer encoded = syncInfoValue.encode( null );
            encoded.flip();
            bb.flip();
            
            container.clean();
            
            try
            {
                decoder.decode( encoded, container );
            }
            catch ( DecoderException de )
            {
                de.printStackTrace();
                fail( de.getMessage() );
            }
            
            SyncInfoValueControlCodec redecoded = container.getSyncInfoValueControl();
            
            assertTrue( Arrays.equals( syncInfoValue.getCookie(), redecoded.getCookie() ) );
            assertEquals( syncInfoValue.isRefreshDeletes(), redecoded.isRefreshDeletes() );
            assertEquals( 0, redecoded.getSyncUUIDs().size() );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }

    
    /**
     * Test the decoding of a SyncInfoValue control, syncIdSet choice, a cookie
     * no refreshDeletes flag, a UUID set with some values
     */
    @Test
    public void testDecodeSyncInfoValueControlSyncIdSetNoCookieRefreshDeletesUUIDsSet()
    {
        Asn1Decoder decoder = new SyncInfoValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x3D );
        bb.put( new byte[]
            { 
            (byte)0xA3, 0x3B,               // syncInfoValue ::= CHOICE {
                                            //     syncIdSet [3] SEQUENCE {
              0X01, 0X01, 0x10,             //         refreshDeletes BOOLEAN DEFAULT FALSE,
              0x31, 0x36,                   //         syncUUIDs SET OF syncUUID
                0x04, 0x10,                 // syncUUID
                  0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                  0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                0x04, 0x10,                 // syncUUID
                  0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02,
                  0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02,
                0x04, 0x10,                 // syncUUID
                  0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03,
                  0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03
            } );
        bb.flip();

        SyncInfoValueControlContainer container = new SyncInfoValueControlContainer();
        
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }
        SyncInfoValueControlCodec syncInfoValue = container.getSyncInfoValueControl();
        assertEquals( SynchronizationInfoEnum.SYNC_ID_SET, syncInfoValue.getType() );
        assertEquals( "", StringTools.utf8ToString( syncInfoValue.getCookie() ) );
        assertTrue( syncInfoValue.isRefreshDeletes() );
        assertEquals( 3, syncInfoValue.getSyncUUIDs().size() );
        
        for ( int i = 0; i < 3; i++ )
        {
            byte[] uuid = syncInfoValue.getSyncUUIDs().get( i );
            
            for ( int j = 0; j < 16; j++ )
            {
                assertEquals( i + 1, uuid[j] );
            }
        }
        
        // Check the encoding
        try
        {
            ByteBuffer encoded = syncInfoValue.encode( null );
            encoded.flip();
            bb.flip();
            
            container.clean();
            
            try
            {
                decoder.decode( encoded, container );
            }
            catch ( DecoderException de )
            {
                de.printStackTrace();
                fail( de.getMessage() );
            }
            
            SyncInfoValueControlCodec redecoded = container.getSyncInfoValueControl();
            
            assertTrue( Arrays.equals( syncInfoValue.getCookie(), redecoded.getCookie() ) );
            assertEquals( syncInfoValue.isRefreshDeletes(), redecoded.isRefreshDeletes() );
            assertEquals( 3, redecoded.getSyncUUIDs().size() );
            
            for ( int i = 0; i < 3; i++ )
            {
                byte[] uuid = redecoded.getSyncUUIDs().get( i );
                
                for ( int j = 0; j < 16; j++ )
                {
                    assertEquals( i + 1, uuid[j] );
                }
            }
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }

    
    /**
     * Test the decoding of a SyncInfoValue control, syncIdSet choice, a cookie
     * a refreshDeletes flag, an empty UUID set
     */
    @Test
    public void testDecodeSyncInfoValueControlSyncIdSetCookieRefreshDeletesEmptySet()
    {
        Asn1Decoder decoder = new SyncInfoValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x0C );
        bb.put( new byte[]
            { 
            (byte)0xA3, 0x0A,               // syncInfoValue ::= CHOICE {
                                            //     syncIdSet [3] SEQUENCE {
              0X04, 0X03, 'a', 'b', 'c',    //         cookie         syncCookie OPTIONAL,
              0x01, 0x01, 0x10,             //         refreshDeletes BOOLEAN DEFAULT FALSE,
              0x31, 0x00,                   //         syncUUIDs SET OF syncUUID
            } );
        bb.flip();

        SyncInfoValueControlContainer container = new SyncInfoValueControlContainer();
        
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }
        SyncInfoValueControlCodec syncInfoValue = container.getSyncInfoValueControl();
        assertEquals( SynchronizationInfoEnum.SYNC_ID_SET, syncInfoValue.getType() );
        assertEquals( "abc", StringTools.utf8ToString( syncInfoValue.getCookie() ) );
        assertTrue( syncInfoValue.isRefreshDeletes() );
        assertEquals( 0, syncInfoValue.getSyncUUIDs().size() );
        
        // Check the encoding
        try
        {
            ByteBuffer encoded = syncInfoValue.encode( null );
            encoded.flip();
            bb.flip();
            
            container.clean();
            
            try
            {
                decoder.decode( encoded, container );
            }
            catch ( DecoderException de )
            {
                de.printStackTrace();
                fail( de.getMessage() );
            }
            
            SyncInfoValueControlCodec redecoded = container.getSyncInfoValueControl();
            
            assertTrue( Arrays.equals( syncInfoValue.getCookie(), redecoded.getCookie() ) );
            assertEquals( syncInfoValue.isRefreshDeletes(), redecoded.isRefreshDeletes() );
            assertEquals( 0, redecoded.getSyncUUIDs().size() );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }

    
    /**
     * Test the decoding of a SyncInfoValue control, syncIdSet choice, a cookie
     * a refreshDeletes flag, a UUID set with some values
     */
    @Test
    public void testDecodeSyncInfoValueControlSyncIdSetCookieRefreshDeletesUUIDsSet()
    {
        Asn1Decoder decoder = new SyncInfoValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x42 );
        bb.put( new byte[]
            { 
            (byte)0xA3, 0x40,               // syncInfoValue ::= CHOICE {
                                            //     syncIdSet [3] SEQUENCE {
              0X04, 0X03, 'a', 'b', 'c',    //         cookie         syncCookie OPTIONAL,
              0x01, 0x01, 0x10,             //         refreshDeletes BOOLEAN DEFAULT FALSE,
              0x31, 0x36,                   //         syncUUIDs SET OF syncUUID
                0x04, 0x10,                 // syncUUID
                  0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                  0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                0x04, 0x10,                 // syncUUID
                  0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02,
                  0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02,
                0x04, 0x10,                 // syncUUID
                  0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03,
                  0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03
            } );
        bb.flip();

        SyncInfoValueControlContainer container = new SyncInfoValueControlContainer();
        
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }
        SyncInfoValueControlCodec syncInfoValue = container.getSyncInfoValueControl();
        assertEquals( SynchronizationInfoEnum.SYNC_ID_SET, syncInfoValue.getType() );
        assertEquals( "abc", StringTools.utf8ToString( syncInfoValue.getCookie() ) );
        assertTrue( syncInfoValue.isRefreshDeletes() );
        assertEquals( 3, syncInfoValue.getSyncUUIDs().size() );
        
        for ( int i = 0; i < 3; i++ )
        {
            byte[] uuid = syncInfoValue.getSyncUUIDs().get( i );
            
            for ( int j = 0; j < 16; j++ )
            {
                assertEquals( i + 1, uuid[j] );
            }
        }
        
        // Check the encoding
        try
        {
            ByteBuffer encoded = syncInfoValue.encode( null );
            encoded.flip();
            bb.flip();
            
            container.clean();
            
            try
            {
                decoder.decode( encoded, container );
            }
            catch ( DecoderException de )
            {
                de.printStackTrace();
                fail( de.getMessage() );
            }
            
            SyncInfoValueControlCodec redecoded = container.getSyncInfoValueControl();
            
            assertTrue( Arrays.equals( syncInfoValue.getCookie(), redecoded.getCookie() ) );
            assertEquals( syncInfoValue.isRefreshDeletes(), redecoded.isRefreshDeletes() );
            assertEquals( 3, redecoded.getSyncUUIDs().size() );
            
            for ( int i = 0; i < 3; i++ )
            {
                byte[] uuid = redecoded.getSyncUUIDs().get( i );
                
                for ( int j = 0; j < 16; j++ )
                {
                    assertEquals( i + 1, uuid[j] );
                }
            }
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }

    
    /**
     * Test the decoding of a SyncInfoValue control, syncIdSet choice, with some
     * invalid UUID
     */
    @Test
    public void testDecodeSyncInfoValueControlSyncIdSetTooSmallUUID()
    {
        Asn1Decoder decoder = new SyncInfoValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x1D );
        bb.put( new byte[]
            { 
            (byte)0xA3, 0x1B,               // syncInfoValue ::= CHOICE {
                                            //     syncIdSet [3] SEQUENCE {
              0X04, 0X03, 'a', 'b', 'c',    //         cookie         syncCookie OPTIONAL,
              0x01, 0x01, 0x10,             //         refreshDeletes BOOLEAN DEFAULT FALSE,
              0x31, 0x11,                   //         syncUUIDs SET OF syncUUID
                0x04, 0x0F,                 // syncUUID
                  0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                  0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01
            } );
        bb.flip();

        SyncInfoValueControlContainer container = new SyncInfoValueControlContainer();
        
        try
        {
            decoder.decode( bb, container );
            fail( "Should not be there" );
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
        }
    }

    
    /**
     * Test the decoding of a SyncInfoValue control, syncIdSet choice, with some
     * invalid UUID
     */
    @Test
    public void testDecodeSyncInfoValueControlSyncIdSetTooLongUUID()
    {
        Asn1Decoder decoder = new SyncInfoValueControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x20 );
        bb.put( new byte[]
            { 
            (byte)0xA3, 0x1E,               // syncInfoValue ::= CHOICE {
                                            //     syncIdSet [3] SEQUENCE {
              0X04, 0X03, 'a', 'b', 'c',    //         cookie         syncCookie OPTIONAL,
              0x01, 0x01, 0x10,             //         refreshDeletes BOOLEAN DEFAULT FALSE,
              0x31, 0x13,                   //         syncUUIDs SET OF syncUUID
                0x04, 0x11,                 // syncUUID
                  0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                  0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 
                  0x01
            } );
        bb.flip();

        SyncInfoValueControlContainer container = new SyncInfoValueControlContainer();
        
        try
        {
            decoder.decode( bb, container );
            fail( "Should not be there" );
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
        }
    }
}
