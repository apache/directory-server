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
package org.apache.directory.shared.ldap.codec.bind;


import java.nio.ByteBuffer;
import java.util.List;

import javax.naming.NamingException;

import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.codec.Control;
import org.apache.directory.shared.ldap.codec.LdapDecoder;
import org.apache.directory.shared.ldap.codec.LdapMessage;
import org.apache.directory.shared.ldap.codec.LdapMessageContainer;
import org.apache.directory.shared.ldap.codec.bind.BindRequest;
import org.apache.directory.shared.ldap.codec.bind.SimpleAuthentication;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.StringTools;

import junit.framework.TestCase;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class BindRequestPerfTest extends TestCase
{
    /**
     * Test the decoding of a BindRequest with Simple authentication and no
     * controls
     */
    public void testDecodeBindRequestSimpleNoControlsPerf()
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x52 );
        stream.put( new byte[]
             { 
             0x30, 0x50,                 // LDAPMessage ::=SEQUENCE {
               0x02, 0x01, 0x01,         // messageID MessageID
               0x60, 0x2E,               // CHOICE { ..., bindRequest BindRequest, ...
                                         // BindRequest ::= APPLICATION[0] SEQUENCE {
                 0x02, 0x01, 0x03,       // version INTEGER (1..127),
                 0x04, 0x1F,             // name LDAPDN,
                 'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                 'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 
                 ( byte ) 0x80, 0x08,    // authentication AuthenticationChoice
                                         // AuthenticationChoice ::= CHOICE { simple [0] OCTET STRING,
                                         // ...
                   'p', 'a', 's', 's', 'w', 'o', 'r', 'd', 
               ( byte ) 0xA0, 0x1B, // A control
                 0x30, 0x19, 
                   0x04, 0x17, 
                     0x32, 0x2E, 0x31, 0x36, 0x2E, 0x38, 0x34, 0x30, 0x2E, 0x31, 0x2E, 0x31, 0x31, 0x33, 0x37, 0x33, 
                     0x30, 0x2E, 0x33, 0x2E, 0x34, 0x2E, 0x32 
             } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode the BindRequest PDU
        try
        {
            int nbLoops = 10;
            long t0 = System.currentTimeMillis();
            
            for ( int i = 0; i < nbLoops; i++ )
            {
                ldapDecoder.decode( stream, ldapMessageContainer );
                ( ( LdapMessageContainer ) ldapMessageContainer).clean();
                stream.flip();
            }
            
            long t1 = System.currentTimeMillis();
            System.out.println( "testDecodeBindRequestSimpleNoControlsPerf, " + nbLoops + " loops, Delta = " + ( t1 - t0 ) );
            
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }
        catch ( NamingException ne )
        {
            ne.printStackTrace();
            fail( ne.getMessage() );
        }

        // Check the decoded BindRequest
        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        BindRequest br = message.getBindRequest();

        assertEquals( 1, message.getMessageId() );
        assertEquals( 3, br.getVersion() );
        assertEquals( "uid=akarasulu,dc=example,dc=com", br.getName().toString() );
        assertEquals( true, ( br.getAuthentication() instanceof SimpleAuthentication ) );
        assertEquals( "password", StringTools.utf8ToString( ( ( SimpleAuthentication ) br.getAuthentication() )
            .getSimple() ) );

        // Check the Control
        List controls = message.getControls();

        assertEquals( 1, controls.size() );

        Control control = message.getControls( 0 );
        assertEquals( "2.16.840.1.113730.3.4.2", control.getControlType() );
        assertEquals( "", StringTools.dumpBytes( ( byte[] ) control.getControlValue() ) );

        // Check the length
        assertEquals( 0x52, message.computeLength() );

        // Check the encoding
        try
        {
            ByteBuffer bb = message.encode( null );

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
     * Test the decoding of a BindRequest with Simple authentication and no
     * controls
     */
    public void testEncodeBindRequestPerf() throws Exception
    {
        LdapDN name = new LdapDN( "uid=akarasulu,dc=example,dc=com" );
        int nbLoops = 10;
        long t0 = System.currentTimeMillis();
        ByteBuffer bb=null;
        
        for ( int i = 0; i< nbLoops; i++)
        {
            // Check the decoded BindRequest
            LdapMessage message = new LdapMessage();
            message.setMessageId( 1 );
            
            BindRequest br = new BindRequest();
            br.setMessageId( 1 );
            br.setName( name );
            
            Control control = new Control();
            control.setControlType( "2.16.840.1.113730.3.4.2" );

            LdapAuthentication authentication = new SimpleAuthentication();
            ((SimpleAuthentication)authentication).setSimple( StringTools.getBytesUtf8( "password" ) );

            br.addControl( control );
            br.setAuthentication( authentication );
            message.setProtocolOP( br );
    
            // Check the encoding
            try
            {
                bb = message.encode( null );
            }
            catch ( EncoderException ee )
            {
                ee.printStackTrace();
                fail( ee.getMessage() );
            }
        }

        long t1 = System.currentTimeMillis();
        System.out.println( "BindRequest testEncodeBindRequestPerf, " + nbLoops + " loops, Delta = " + (t1 - t0));
        System.out.println( StringTools.dumpBytes( bb.array() ));
    }
}
