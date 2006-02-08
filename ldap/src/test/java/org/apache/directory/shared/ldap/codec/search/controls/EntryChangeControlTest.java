/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.shared.ldap.codec.search.controls;


import java.nio.ByteBuffer;

import javax.naming.NamingException;

import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.ldap.codec.search.controls.ChangeType;
import org.apache.directory.shared.ldap.codec.search.controls.EntryChangeControl;
import org.apache.directory.shared.ldap.codec.search.controls.EntryChangeControlContainer;
import org.apache.directory.shared.ldap.codec.search.controls.EntryChangeControlDecoder;
import org.apache.directory.shared.ldap.util.StringTools;

import junit.framework.Assert;
import junit.framework.TestCase;


/**
 * Test the EntryChangeControlTest codec
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EntryChangeControlTest extends TestCase
{
    /**
     * Test the decoding of a EntryChangeControl
     */
    public void testDecodeEntryChangeControlSuccess() throws NamingException
    {
        Asn1Decoder decoder = new EntryChangeControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x0D );
        bb.put( new byte[]
            { 0x30, 0x0B, // EntryChangeNotification ::= SEQUENCE {
                0x0A, 0x01, 0x08, // changeType ENUMERATED {
                // modDN (8)
                // }
                0x04, 0x03, 'a', '=', 'b', // previousDN LDAPDN OPTIONAL, --
                                            // modifyDN ops. only
                0x02, 0x01, 0x10 // changeNumber INTEGER OPTIONAL -- if
                                    // supported
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
            Assert.fail( de.getMessage() );
        }

        EntryChangeControl entryChange = container.getEntryChangeControl();
        assertEquals( ChangeType.MODDN, entryChange.getChangeType() );
        assertEquals( "a=b", entryChange.getPreviousDn() );
        assertEquals( 16, entryChange.getChangeNumber() );
    }


    /**
     * Test the decoding of a EntryChangeControl with a add and a change number
     */
    public void testDecodeEntryChangeControlWithADDAndChangeNumber() throws NamingException
    {
        Asn1Decoder decoder = new EntryChangeControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x08 );
        bb.put( new byte[]
            { 0x30, 0x06, // EntryChangeNotification ::= SEQUENCE {
                0x0A, 0x01, 0x01, // changeType ENUMERATED {
                // Add (1)
                // }
                0x02, 0x01, 0x10 // changeNumber INTEGER OPTIONAL -- if
                                    // supported
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
            Assert.fail( de.getMessage() );
        }

        EntryChangeControl entryChange = container.getEntryChangeControl();
        assertEquals( ChangeType.ADD, entryChange.getChangeType() );
        assertEquals( "", entryChange.getPreviousDn() );
        assertEquals( 16, entryChange.getChangeNumber() );
    }


    /**
     * Test the decoding of a EntryChangeControl with a add so we should not
     * have a PreviousDN
     */
    public void testDecodeEntryChangeControlWithADDAndPreviousDNBad() throws NamingException
    {
        Asn1Decoder decoder = new EntryChangeControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x0D );
        bb.put( new byte[]
            { 0x30, 0x0B, // EntryChangeNotification ::= SEQUENCE {
                0x0A, 0x01, 0x01, // changeType ENUMERATED {
                // ADD (1)
                // }
                0x04, 0x03, 'a', '=', 'b', // previousDN LDAPDN OPTIONAL, --
                                            // modifyDN ops. only
                0x02, 0x01, 0x10 // changeNumber INTEGER OPTIONAL -- if
                                    // supported
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

        Assert.fail( "A ADD operation should not have a PreviousDN" );
    }


    /**
     * Test the decoding of a EntryChangeControl with a add and nothing else
     */
    public void testDecodeEntryChangeControlWithADD() throws NamingException
    {
        Asn1Decoder decoder = new EntryChangeControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x05 );
        bb.put( new byte[]
            { 0x30, 0x03, // EntryChangeNotification ::= SEQUENCE {
                0x0A, 0x01, 0x01, // changeType ENUMERATED {
            // ADD (1)
            // }
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
            Assert.fail( de.getMessage() );
        }

        EntryChangeControl entryChange = container.getEntryChangeControl();
        assertEquals( ChangeType.ADD, entryChange.getChangeType() );
        assertEquals( "", entryChange.getPreviousDn() );
        assertEquals( EntryChangeControl.UNDEFINED_CHANGE_NUMBER, entryChange.getChangeNumber() );
    }


    /**
     * Test the decoding of a EntryChangeControl with a worng changeType and
     * nothing else
     */
    public void testDecodeEntryChangeControlWithWrongChangeType() throws NamingException
    {
        Asn1Decoder decoder = new EntryChangeControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x05 );
        bb.put( new byte[]
            { 0x30, 0x03, // EntryChangeNotification ::= SEQUENCE {
                0x0A, 0x01, 0x03, // changeType ENUMERATED {
            // BAD Change Type
            // }
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

        Assert.fail( "The changeType is unknown" );
    }


    /**
     * Test encoding of a EntryChangeControl.
     */
    public void testEncodeEntryChangeControl() throws Exception
    {
        ByteBuffer bb = ByteBuffer.allocate( 0x0D );
        bb.put( new byte[]
            { 0x30, 0x0B, // EntryChangeNotification ::= SEQUENCE {
                0x0A, 0x01, 0x08, // changeType ENUMERATED {
                // modDN (8)
                // }
                0x04, 0x03, 'a', '=', 'b', // previousDN LDAPDN OPTIONAL, --
                                            // modifyDN ops. only
                0x02, 0x01, 0x10 // changeNumber INTEGER OPTIONAL -- if
                                    // supported
            } );

        String expected = StringTools.dumpBytes( bb.array() );
        bb.flip();

        EntryChangeControl entry = new EntryChangeControl();
        entry.setChangeType( ChangeType.MODDN );
        entry.setChangeNumber( 16 );
        entry.setPreviousDn( "a=b" );
        bb = entry.encode( null );
        String decoded = StringTools.dumpBytes( bb.array() );
        assertEquals( expected, decoded );
    }
}
