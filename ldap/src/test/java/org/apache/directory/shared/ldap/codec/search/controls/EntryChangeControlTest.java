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
package org.apache.directory.shared.ldap.codec.search.controls;


import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.ldap.codec.search.controls.ChangeType;
import org.apache.directory.shared.ldap.codec.search.controls.entryChange.EntryChangeControlCodec;
import org.apache.directory.shared.ldap.codec.search.controls.entryChange.EntryChangeControlContainer;
import org.apache.directory.shared.ldap.codec.search.controls.entryChange.EntryChangeControlDecoder;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertNull;


/**
 * Test the EntryChangeControlTest codec
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EntryChangeControlTest
{
    /**
     * Test the decoding of a EntryChangeControl
     */
    @Test
    public void testDecodeEntryChangeControlSuccess()
    {
        Asn1Decoder decoder = new EntryChangeControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x0D );
        bb.put( new byte[]
            { 
            0x30, 0x0B,                     // EntryChangeNotification ::= SEQUENCE {
              0x0A, 0x01, 0x08,             //     changeType ENUMERATED {
                                            //         modDN (8)
                                            //     }
              0x04, 0x03, 'a', '=', 'b',    //     previousDN LDAPDN OPTIONAL, -- modifyDN ops. only
              0x02, 0x01, 0x10              //     changeNumber INTEGER OPTIONAL } -- if supported
            } );
        bb.flip();

        EntryChangeControlContainer container = new EntryChangeControlContainer();
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        EntryChangeControlCodec entryChange = container.getEntryChangeControl();
        assertEquals( ChangeType.MODDN, entryChange.getChangeType() );
        assertEquals( "a=b", entryChange.getPreviousDn().toString() );
        assertEquals( 16, entryChange.getChangeNumber() );
    }


    /**
     * Test the decoding of a EntryChangeControl
     */
    @Test
    public void testDecodeEntryChangeControlSuccessLongChangeNumber()
    {
        Asn1Decoder decoder = new EntryChangeControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x13 );
        bb.put( new byte[]
            { 
            0x30, 0x11,                     // EntryChangeNotification ::= SEQUENCE {
              0x0A, 0x01, 0x08,             //     changeType ENUMERATED {
                                            //         modDN (8)
                                            //     }
              0x04, 0x03, 'a', '=', 'b',    //     previousDN LDAPDN OPTIONAL, -- modifyDN ops. only
              0x02, 0x07,                   //     changeNumber INTEGER OPTIONAL } -- if supported
                0x12, 0x34, 0x56, 0x78, (byte)0x9A, (byte)0xBC, (byte)0xDE
            } );
        bb.flip();

        EntryChangeControlContainer container = new EntryChangeControlContainer();
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        EntryChangeControlCodec entryChange = container.getEntryChangeControl();
        assertEquals( ChangeType.MODDN, entryChange.getChangeType() );
        assertEquals( "a=b", entryChange.getPreviousDn().toString() );
        assertEquals( 5124095576030430L, entryChange.getChangeNumber() );
    }


    /**
     * Test the decoding of a EntryChangeControl with a add and a change number
     */
    @Test
    public void testDecodeEntryChangeControlWithADDAndChangeNumber()
    {
        Asn1Decoder decoder = new EntryChangeControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x08 );
        bb.put( new byte[]
            { 
            0x30, 0x06,             // EntryChangeNotification ::= SEQUENCE {
              0x0A, 0x01, 0x01,     //     changeType ENUMERATED {
                                    //         Add (1)
                                    //     }
              0x02, 0x01, 0x10      //     changeNumber INTEGER OPTIONAL -- if supported
                                    // }
            } );
        bb.flip();

        EntryChangeControlContainer container = new EntryChangeControlContainer();
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        EntryChangeControlCodec entryChange = container.getEntryChangeControl();
        assertEquals( ChangeType.ADD, entryChange.getChangeType() );
        assertNull( entryChange.getPreviousDn() );
        assertEquals( 16, entryChange.getChangeNumber() );
    }


    /**
     * Test the decoding of a EntryChangeControl with a add so we should not
     * have a PreviousDN
     */
    @Test
    public void testDecodeEntryChangeControlWithADDAndPreviousDNBad()
    {
        Asn1Decoder decoder = new EntryChangeControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x0D );
        bb.put( new byte[]
            { 
            0x30, 0x0B,                     // EntryChangeNotification ::= SEQUENCE {
              0x0A, 0x01, 0x01,             //     changeType ENUMERATED {
                                            //         ADD (1)
                                            //     }
              0x04, 0x03, 'a', '=', 'b',    //     previousDN LDAPDN OPTIONAL, --
                                            //     modifyDN ops. only
              0x02, 0x01, 0x10              //     changeNumber INTEGER OPTIONAL -- if supported
                                            // }
            } );
        bb.flip();

        EntryChangeControlContainer container = new EntryChangeControlContainer();

        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            // We should fail, because we have a previousDN with a ADD
            assertTrue( true );
            return;
        }

        fail( "A ADD operation should not have a PreviousDN" );
    }


    /**
     * Test the decoding of a EntryChangeControl with a add and nothing else
     */
    @Test
    public void testDecodeEntryChangeControlWithADD()
    {
        Asn1Decoder decoder = new EntryChangeControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x05 );
        bb.put( new byte[]
            { 
            0x30, 0x03,                 // EntryChangeNotification ::= SEQUENCE {
              0x0A, 0x01, 0x01,         //     changeType ENUMERATED {
                                        //         ADD (1)
                                        //     }
                                        // }
            } );
        bb.flip();

        EntryChangeControlContainer container = new EntryChangeControlContainer();

        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        EntryChangeControlCodec entryChange = container.getEntryChangeControl();
        assertEquals( ChangeType.ADD, entryChange.getChangeType() );
        assertNull( entryChange.getPreviousDn() );
        assertEquals( EntryChangeControlCodec.UNDEFINED_CHANGE_NUMBER, entryChange.getChangeNumber() );
    }


    /**
     * Test the decoding of a EntryChangeControl with a wrong changeType and
     * nothing else
     */
    @Test
    public void testDecodeEntryChangeControlWithWrongChangeType()
    {
        Asn1Decoder decoder = new EntryChangeControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x05 );
        bb.put( new byte[]
            { 
            0x30, 0x03,                 // EntryChangeNotification ::= SEQUENCE {
              0x0A, 0x01, 0x03,         //     changeType ENUMERATED {
                                        //         BAD Change Type
                                        //     }
                                        // }
            } );
        bb.flip();

        EntryChangeControlContainer container = new EntryChangeControlContainer();

        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            // We should fail because the ChangeType is not known
            assertTrue( true );
            return;
        }

        fail( "The changeType is unknown" );
    }


    /**
     * Test the decoding of a EntryChangeControl with a wrong changeNumber
     */
    @Test
    public void testDecodeEntryChangeControlWithWrongChangeNumber()
    {
        Asn1Decoder decoder = new EntryChangeControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x1C );
        bb.put( new byte[]
            { 
            0x30, 0x1A,                     // EntryChangeNotification ::= SEQUENCE {
              0x0A, 0x01, 0x08,             //     changeType ENUMERATED {
                                            //         modDN (8)
                                            //     }
              0x04, 0x03, 'a', '=', 'b',    //     previousDN LDAPDN OPTIONAL, -- modifyDN ops. only
              0x02, 0x10,                   //     changeNumber INTEGER OPTIONAL -- if supported
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
            } );
        bb.flip();

        EntryChangeControlContainer container = new EntryChangeControlContainer();

        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            // We should fail because the ChangeType is not known
            assertTrue( true );
            return;
        }

        fail( "The changeNumber is incorrect" );
    }


    /**
     * Test encoding of a EntryChangeControl.
     */
    @Test
    public void testEncodeEntryChangeControl() throws Exception
    {
        ByteBuffer bb = ByteBuffer.allocate( 0x0D );
        bb.put( new byte[]
            { 
            0x30, 0x0B,                     // EntryChangeNotification ::= SEQUENCE {
              0x0A, 0x01, 0x08,             //     changeType ENUMERATED {
                                            //         modDN (8)
                                            //     }
              0x04, 0x03, 'a', '=', 'b',    //     previousDN LDAPDN OPTIONAL, -- modifyDN ops. only
              0x02, 0x01, 0x10              //     changeNumber INTEGER OPTIONAL -- if supported
            } );

        String expected = StringTools.dumpBytes( bb.array() );
        bb.flip();

        EntryChangeControlCodec entry = new EntryChangeControlCodec();
        entry.setChangeType( ChangeType.MODDN );
        entry.setChangeNumber( 16 );
        entry.setPreviousDn( new LdapDN( "a=b" ) );
        bb = entry.encode( null );
        String decoded = StringTools.dumpBytes( bb.array() );
        assertEquals( expected, decoded );
    }


    /**
     * Test encoding of a EntryChangeControl with a long changeNumber.
     */
    @Test
    public void testEncodeEntryChangeControlLong() throws Exception
    {
        ByteBuffer bb = ByteBuffer.allocate( 0x13 );
        bb.put( new byte[]
            { 
            0x30, 0x11,                     // EntryChangeNotification ::= SEQUENCE {
              0x0A, 0x01, 0x08,             //     changeType ENUMERATED {
                                            //         modDN (8)
                                            //     }
              0x04, 0x03, 'a', '=', 'b',    //     previousDN LDAPDN OPTIONAL, -- modifyDN ops. only
              0x02, 0x07,                   //     changeNumber INTEGER OPTIONAL -- if supported
                0x12, 0x34, 0x56, 0x78, (byte)0x9a, (byte)0xbc, (byte)0xde
            } );

        String expected = StringTools.dumpBytes( bb.array() );
        bb.flip();

        EntryChangeControlCodec entry = new EntryChangeControlCodec();
        entry.setChangeType( ChangeType.MODDN );
        entry.setChangeNumber( 5124095576030430L );
        entry.setPreviousDn( new LdapDN( "a=b" ) );
        bb = entry.encode( null );
        String decoded = StringTools.dumpBytes( bb.array() );
        assertEquals( expected, decoded );
    }
}
