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
import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.ldap.codec.search.controls.SubEntryControl;
import org.apache.directory.shared.ldap.codec.search.controls.SubEntryControlContainer;
import org.apache.directory.shared.ldap.codec.search.controls.SubEntryControlDecoder;

import junit.framework.Assert;
import junit.framework.TestCase;


/**
 * Test the SubEntryControlTest codec
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SubEntryControlTest extends TestCase
{
    /**
     * Test the decoding of a SubEntryControl with a true visibility
     */
    public void testDecodeSubEntryVisibilityTrue() throws NamingException
    {
        Asn1Decoder decoder = new SubEntryControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x03 );
        bb.put( new byte[]
            { 0x01, 0x01, ( byte ) 0xFF // Visibility ::= BOOLEAN
            } );
        bb.flip();

        SubEntryControlContainer container = new SubEntryControlContainer();
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            Assert.fail( de.getMessage() );
        }

        SubEntryControl control = container.getSubEntryControl();
        assertTrue( control.isVisible() );
    }


    /**
     * Test the decoding of a SubEntryControl with a false visibility
     */
    public void testDecodeSubEntryVisibilityFalse() throws NamingException
    {
        Asn1Decoder decoder = new SubEntryControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x03 );
        bb.put( new byte[]
            { 0x01, 0x01, 0x00 // Visibility ::= BOOLEAN
            } );
        bb.flip();

        SubEntryControlContainer container = new SubEntryControlContainer();
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            Assert.fail( de.getMessage() );
        }

        SubEntryControl control = container.getSubEntryControl();
        assertFalse( control.isVisible() );
    }


    /**
     * Test the decoding of a SubEntryControl with an empty visibility
     */
    public void testDecodeSubEntryEmptyVisibility() throws NamingException
    {
        Asn1Decoder decoder = new SubEntryControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x02 );

        bb.put( new byte[]
            { 0x01, 0x00 // Visibility ::= BOOLEAN
            } );

        bb.flip();

        // Allocate a LdapMessage Container
        IAsn1Container container = new SubEntryControlContainer();

        // Decode a SubEntryControl PDU
        try
        {
            decoder.decode( bb, container );
            fail( "We should never reach this point !!!" );
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
        }
    }


    /**
     * Test the decoding of a bad SubEntryControl
     */
    public void testDecodeSubEntryBad() throws NamingException
    {
        Asn1Decoder decoder = new SubEntryControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x03 );

        bb.put( new byte[]
            { 0x02, 0x01, 0x01 // Visibility ::= BOOLEAN
            } );

        bb.flip();

        // Allocate a LdapMessage Container
        IAsn1Container container = new SubEntryControlContainer();

        // Decode a SubEntryControl PDU
        try
        {
            decoder.decode( bb, container );
            fail( "We should never reach this point !!!" );
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
        }
    }
}
