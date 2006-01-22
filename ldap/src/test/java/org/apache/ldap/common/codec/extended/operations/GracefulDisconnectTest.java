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
package org.apache.ldap.common.codec.extended.operations;

import java.nio.ByteBuffer;

import javax.naming.NamingException;

import org.apache.asn1.codec.DecoderException;
import org.apache.asn1.ber.Asn1Decoder;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Test the GracefulDisconnectTest codec
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class GracefulDisconnectTest extends TestCase {
    /**
     * Test the decoding of a GracefulDisconnect
     */
    public void testDecodeGracefulDisconnectSuccess() throws NamingException
    {
        Asn1Decoder decoder = new GracefulDisconnectDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x70 );
        bb.put( new byte[]
            {
                0x30, 0x6E, 		        // GracefulDisconnec ::= SEQUENCE {
				  0x02, 0x01, 0x01,         //     timeOffline INTEGER (0..720) DEFAULT 0,
				  (byte)0x80, 0x01, 0x01,	//     delay INTEGER (0..86400) DEFAULT 0
                                            //     replicatedContexts Referral OPTIONAL
                  0x30, 0x66, 
                    0x04, 0x1F, 
                      'l', 'd', 'a', 'p', ':', '/', '/', 'd', 
                      'i', 'r', 'e', 'c', 't', 'o', 'r', 'y', 
                      '.', 'a', 'p', 'a', 'c', 'h', 'e', '.', 
                      'o', 'r', 'g', ':', '8', '0', '/',
                    0x04, 0x43,
                      'l', 'd', 'a', 'p', ':', '/', '/', 'l', 
                      'd', 'a', 'p', '.', 'n', 'e', 't', 's', 
                      'c', 'a', 'p', 'e', '.', 'c', 'o', 'm', 
                      '/', 'o', '=', 'B', 'a', 'b', 's', 'c', 
                      'o', ',', 'c', '=', 'U', 'S', '?', '?', 
                      '?', '(', 'i', 'n', 't', '=', '%', '5', 
                      'c', '0', '0', '%', '5', 'c', '0', '0', 
                      '%', '5', 'c', '0', '0', '%', '5', 'c', 
                      '0', '4', ')'
                                            // }
            } );
        bb.flip();

        GracefulDisconnectContainer container = new GracefulDisconnectContainer();
        
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            Assert.fail( de.getMessage() );
        }
        
        GracefulDisconnect gracefulDisconnect = container.getGracefulDisconnect();
        assertEquals( 1, gracefulDisconnect.getTimeOffline() );
        assertEquals( 1, gracefulDisconnect.getDelay() );
        assertEquals( 2, gracefulDisconnect.getReplicatedContexts().size() );
        assertEquals( "ldap://directory.apache.org:80/", gracefulDisconnect.getReplicatedContexts().get( 0 ).toString() );
        assertEquals( "ldap://ldap.netscape.com/o=Babsco,c=US???(int=%5c00%5c00%5c00%5c04)", gracefulDisconnect.getReplicatedContexts().get( 1 ).toString() );
    }

    /**
     * Test the decoding of a GracefulDisconnect with a timeOffline only
     */
    public void testDecodeGracefulDisconnectTimeOffline() throws NamingException
    {
        Asn1Decoder decoder = new GracefulDisconnectDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x0b );
        bb.put( new byte[]
            {
                0x30, 0x03,                 // GracefulDisconnect ::= SEQUENCE {
                  0x02, 0x01, 0x01          //     timeOffline INTEGER (0..720) DEFAULT 0,
            } );
        bb.flip();

        GracefulDisconnectContainer container = new GracefulDisconnectContainer();
        
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            Assert.fail( de.getMessage() );
        }
        
        GracefulDisconnect gracefulDisconnect = container.getGracefulDisconnect();
        assertEquals( 1, gracefulDisconnect.getTimeOffline() );
        assertEquals( 0, gracefulDisconnect.getDelay() );
    }

    /**
     * Test the decoding of a GracefulDisconnect with a delay only
     */
    public void testDecodeGracefulDisconnectDelay() throws NamingException
    {
        Asn1Decoder decoder = new GracefulDisconnectDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x0b );
        bb.put( new byte[]
            {
                0x30, 0x03,                 // GracefulDisconnect ::= SEQUENCE {
                  (byte)0x80, 0x01, 0x01          //     delay INTEGER (0..86400) DEFAULT 0
            } );
        bb.flip();

        GracefulDisconnectContainer container = new GracefulDisconnectContainer();
        
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            Assert.fail( de.getMessage() );
        }
        
        GracefulDisconnect gracefulDisconnect = container.getGracefulDisconnect();
        assertEquals( 0, gracefulDisconnect.getTimeOffline() );
        assertEquals( 1, gracefulDisconnect.getDelay() );
    }

    /**
     * Test the decoding of a empty GracefulDisconnect
     */
    public void testDecodeGracefulDisconnectEmpty() throws NamingException
    {
        Asn1Decoder decoder = new GracefulDisconnectDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x0b );
        bb.put( new byte[]
            {
                0x30, 0x00                 // GracefulDisconnect ::= SEQUENCE {
            } );
        bb.flip();

        GracefulDisconnectContainer container = new GracefulDisconnectContainer();
        
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            Assert.fail( de.getMessage() );
        }
        
        GracefulDisconnect gracefulDisconnect = container.getGracefulDisconnect();
        assertEquals( 0, gracefulDisconnect.getTimeOffline() );
        assertEquals( 0, gracefulDisconnect.getDelay() );
    }
    
    // Defensive tests

    /**
     * Test the decoding of a GracefulDisconnect with a timeOffline off limit
     */
    public void testDecodeGracefulDisconnectTimeOfflineOffLimit() throws NamingException
    {
        Asn1Decoder decoder = new GracefulDisconnectDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x0b );
        bb.put( new byte[]
            {
                0x30, 0x04,                     // GracefulDisconnect ::= SEQUENCE {
                  0x02, 0x02, 0x03, (byte)0xE8  //     timeOffline INTEGER (0..720) DEFAULT 0,
            } );
        bb.flip();

        GracefulDisconnectContainer container = new GracefulDisconnectContainer();
        
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
     * Test the decoding of a GracefulDisconnect with a delay off limit
     */
    public void testDecodeGracefulDisconnectDelayOffLimit() throws NamingException
    {
        Asn1Decoder decoder = new GracefulDisconnectDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x0b );
        bb.put( new byte[]
            {
                0x30, 0x05,                     // GracefulDisconnect ::= SEQUENCE {
                  (byte)0x80, 0x03, 0x01, (byte)0x86, (byte)0xA0  //     delay INTEGER (0..86400) DEFAULT 0
            } );
        bb.flip();

        GracefulDisconnectContainer container = new GracefulDisconnectContainer();
        
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
     * Test the decoding of a GracefulDisconnect with an empty TimeOffline
     */
    public void testDecodeGracefulDisconnectTimeOfflineEmpty() throws NamingException
    {
        Asn1Decoder decoder = new GracefulDisconnectDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x0b );
        bb.put( new byte[]
            {
                0x30, 0x02,   // GracefulDisconnect ::= SEQUENCE {
                  0x02, 0x00  //     timeOffline INTEGER (0..720) DEFAULT 0,
            } );
        bb.flip();

        GracefulDisconnectContainer container = new GracefulDisconnectContainer();
        
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
     * Test the decoding of a GracefulDisconnect with an empty delay
     */
    public void testDecodeGracefulDisconnectDelayEmpty() throws NamingException
    {
        Asn1Decoder decoder = new GracefulDisconnectDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x0b );
        bb.put( new byte[]
            {
                0x30, 0x02,         // GracefulDisconnect ::= SEQUENCE {
                  (byte)0x80, 0x00  //     delay INTEGER (0..86400) DEFAULT 0
            } );
        bb.flip();

        GracefulDisconnectContainer container = new GracefulDisconnectContainer();
        
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
