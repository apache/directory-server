/*
 *   Copyright 2006 The Apache Software Foundation
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
package org.apache.ldap.common.codec.extended.operations;

import java.nio.ByteBuffer;

import javax.naming.NamingException;

import org.apache.asn1.codec.DecoderException;
import org.apache.asn1.ber.Asn1Decoder;
import org.apache.ldap.common.codec.LdapDecoder;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Test the GracefulShutdownTest codec
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class GracefulShutdownTest extends TestCase {
    /**
     * Test the decoding of a GracefulShutdown
     */
    public void testDecodeGracefulShutdownSuccess() throws NamingException
    {
        Asn1Decoder decoder = new LdapDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x0b );
        bb.put( new byte[]
            {
                0x30, 0x06, 		        // GracefulShutdown ::= SEQUENCE {
				  0x02, 0x01, 0x01,         //     timeOffline INTEGER (0..720) DEFAULT 0,
				  (byte)0x80, 0x01, 0x01	//     delay INTEGER (0..86400) DEFAULT 0
                                            // }
            } );
        bb.flip();

        GracefulShutdownContainer container = new GracefulShutdownContainer();
        
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            Assert.fail( de.getMessage() );
        }
        
        GracefulShutdown gracefulShutdown = container.getGracefulShutdown();
        assertEquals( 1, gracefulShutdown.getTimeOffline() );
        assertEquals( 1, gracefulShutdown.getDelay() );
    }

    /**
     * Test the decoding of a GracefulShutdown with a timeOffline only
     */
    public void testDecodeGracefulShutdownTimeOffline() throws NamingException
    {
        Asn1Decoder decoder = new LdapDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x0b );
        bb.put( new byte[]
            {
                0x30, 0x03,                 // GracefulShutdown ::= SEQUENCE {
                  0x02, 0x01, 0x01          //     timeOffline INTEGER (0..720) DEFAULT 0,
            } );
        bb.flip();

        GracefulShutdownContainer container = new GracefulShutdownContainer();
        
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            Assert.fail( de.getMessage() );
        }
        
        GracefulShutdown gracefulShutdown = container.getGracefulShutdown();
        assertEquals( 1, gracefulShutdown.getTimeOffline() );
        assertEquals( 0, gracefulShutdown.getDelay() );
    }

    /**
     * Test the decoding of a GracefulShutdown with a delay only
     */
    public void testDecodeGracefulShutdownDelay() throws NamingException
    {
        Asn1Decoder decoder = new LdapDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x0b );
        bb.put( new byte[]
            {
                0x30, 0x03,                 // GracefulShutdown ::= SEQUENCE {
                  (byte)0x80, 0x01, 0x01          //     delay INTEGER (0..86400) DEFAULT 0
            } );
        bb.flip();

        GracefulShutdownContainer container = new GracefulShutdownContainer();
        
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            Assert.fail( de.getMessage() );
        }
        
        GracefulShutdown gracefulShutdown = container.getGracefulShutdown();
        assertEquals( 0, gracefulShutdown.getTimeOffline() );
        assertEquals( 1, gracefulShutdown.getDelay() );
    }

    /**
     * Test the decoding of a empty GracefulShutdown
     */
    public void testDecodeGracefulShutdownEmpty() throws NamingException
    {
        Asn1Decoder decoder = new LdapDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x0b );
        bb.put( new byte[]
            {
                0x30, 0x00                 // GracefulShutdown ::= SEQUENCE {
            } );
        bb.flip();

        GracefulShutdownContainer container = new GracefulShutdownContainer();
        
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            Assert.fail( de.getMessage() );
        }
        
        GracefulShutdown gracefulShutdown = container.getGracefulShutdown();
        assertEquals( 0, gracefulShutdown.getTimeOffline() );
        assertEquals( 0, gracefulShutdown.getDelay() );
    }
    
    // Defensive tests

    /**
     * Test the decoding of a GracefulShutdown with a timeOffline off limit
     */
    public void testDecodeGracefulShutdownTimeOfflineOffLimit() throws NamingException
    {
        Asn1Decoder decoder = new LdapDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x0b );
        bb.put( new byte[]
            {
                0x30, 0x04,                     // GracefulShutdown ::= SEQUENCE {
                  0x02, 0x02, 0x03, (byte)0xE8  //     timeOffline INTEGER (0..720) DEFAULT 0,
            } );
        bb.flip();

        GracefulShutdownContainer container = new GracefulShutdownContainer();
        
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            System.out.println( de.getMessage() );
            assertTrue( true );
            return;
        }
        
        fail( "We should not reach this point" );
    }

    /**
     * Test the decoding of a GracefulShutdown with a delay off limit
     */
    public void testDecodeGracefulShutdownDelayOffLimit() throws NamingException
    {
        Asn1Decoder decoder = new LdapDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x0b );
        bb.put( new byte[]
            {
                0x30, 0x05,                     // GracefulShutdown ::= SEQUENCE {
                  (byte)0x80, 0x03, 0x01, (byte)0x86, (byte)0xA0  //     delay INTEGER (0..86400) DEFAULT 0
            } );
        bb.flip();

        GracefulShutdownContainer container = new GracefulShutdownContainer();
        
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            System.out.println( de.getMessage() );
            assertTrue( true );
            return;
        }
        
        fail( "We should not reach this point" );
    }

    /**
     * Test the decoding of a GracefulShutdown with an empty TimeOffline
     */
    public void testDecodeGracefulShutdownTimeOfflineEmpty() throws NamingException
    {
        Asn1Decoder decoder = new LdapDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x0b );
        bb.put( new byte[]
            {
                0x30, 0x02,   // GracefulShutdown ::= SEQUENCE {
                  0x02, 0x00  //     timeOffline INTEGER (0..720) DEFAULT 0,
            } );
        bb.flip();

        GracefulShutdownContainer container = new GracefulShutdownContainer();
        
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            System.out.println( de.getMessage() );
            assertTrue( true );
            return;
        }
        
        fail( "We should not reach this point" );
    }

    /**
     * Test the decoding of a GracefulShutdown with an empty delay
     */
    public void testDecodeGracefulShutdownDelayEmpty() throws NamingException
    {
        Asn1Decoder decoder = new LdapDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x0b );
        bb.put( new byte[]
            {
                0x30, 0x02,         // GracefulShutdown ::= SEQUENCE {
                  (byte)0x80, 0x00  //     delay INTEGER (0..86400) DEFAULT 0
            } );
        bb.flip();

        GracefulShutdownContainer container = new GracefulShutdownContainer();
        
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            System.out.println( de.getMessage() );
            assertTrue( true );
            return;
        }
        
        fail( "We should not reach this point" );
    }
}
