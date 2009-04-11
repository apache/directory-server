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


import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.codec.LdapDecoder;
import org.apache.directory.shared.ldap.codec.extended.operations.certGeneration.CertGenerationContainer;
import org.apache.directory.shared.ldap.codec.extended.operations.certGeneration.CertGenerationObject;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.Test;


/**
 * 
 * Test case for CertGenerate extended operation request.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class CertGenerationRequestTest
{

    /**
     * test the decode operation
     */
    @Test
    public void testCertGenrationDecode()
    {
        String dn = "uid=admin,ou=system";
        String keyAlgo = "RSA";

        Asn1Decoder decoder = new LdapDecoder();

        int dnLen = dn.length();

        // start Tag + L is 2 bytes
        // the same value of DN is used for all target,issuer and subject DNs so
        // it is ( ( OCTET_STRING Tag + Len ) + dnLen ) * 3 
        // finally for keyAlgo ( OCTET_STRING Tag + Len ) + keyAlgoLen

        int bufLen = 2 + ( ( 2 + dnLen ) * 3 ) + ( keyAlgo.length() + 2 );

        ByteBuffer bb = ByteBuffer.allocate( bufLen );

        bb.put( new byte[]
            { 0x30, ( byte ) ( bufLen - 2 ) } ); // CertGenerateObject ::= SEQUENCE {

        /*  targetDN IA5String,
        *   issuerDN IA5String,
        *   subjectDN IA5String,
        *   keyAlgorithm IA5String
        */
        for ( int i = 0; i < 3; i++ )
        {
            bb.put( new byte[]
                { 0x04, ( byte ) dnLen } );
            for ( char c : dn.toCharArray() )
            {
                bb.put( ( byte ) c );
            }
        }
        bb.put( new byte[]
            { 0x04, 0x03, 'R', 'S', 'A' } );

        String decodedPdu = StringTools.dumpBytes( bb.array() );
        bb.flip();

        CertGenerationContainer container = new CertGenerationContainer();
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException e )
        {
            fail( e.getMessage() );
        }

        CertGenerationObject certGenObj = container.getCertGenerationObject();
        assertEquals( dn, certGenObj.getTargetDN() );
        assertEquals( dn, certGenObj.getIssuerDN() );
        assertEquals( dn, certGenObj.getSubjectDN() );
        assertEquals( keyAlgo, certGenObj.getKeyAlgorithm() );

        assertEquals( bufLen, certGenObj.computeLength() );

        try
        {
            ByteBuffer encodedBuf = certGenObj.encode( null );
            String encodedPdu = StringTools.dumpBytes( encodedBuf.array() );

            assertEquals( decodedPdu, encodedPdu );
        }
        catch ( EncoderException e )
        {
            e.getMessage();
            fail( e.getMessage() );
        }

    }


    @Test
    public void testCertGenerationDecodeTargetDN()
    {
        Asn1Decoder decoder = new LdapDecoder();

        ByteBuffer bb = ByteBuffer.allocate( 5 );

        bb.put( new byte[]
            { 0x30, 0x03, // CertGenerateObject ::= SEQUENCE {
              0x04, 0x01, ' ' } ); // empty targetDN value

        bb.flip();

        CertGenerationContainer container = new CertGenerationContainer();

        try
        {
            decoder.decode( bb, container );
            fail( "shouldn't accept the empty targetDN" );
        }
        catch ( DecoderException e )
        {
            assertTrue( true );
        }
                
        String dn = "=sys";

        bb = ByteBuffer.allocate( dn.length() + 2 + 2 );

        bb.put( new byte[]
            { 0x30, ( byte )( dn.length() + 2 ), // CertGenerateObject ::= SEQUENCE {
              0x04, ( byte ) dn.length(), '=', 's', 'y', 's' } ); // empty targetDN value

        bb.flip();

        try
        {
            decoder.decode( bb, container );
            fail( "shouldn't accept the invalid targetDN" );
        }
        catch ( DecoderException e )
        {
            assertTrue( true );
        }

    }
    
    
    @Test
    public void testCertGenerationDecodeIssuerDN()
    {
        Asn1Decoder decoder = new LdapDecoder();
        
        ByteBuffer bb = ByteBuffer.allocate( 11 );

        bb.put( new byte[]
            { 0x30, 0x09, // CertGenerateObject ::= SEQUENCE {
              0x04, 0x04, 'c', 'n', '=', 'x', // target DN string 
              0x04, 0x01, ' '} ); // empty issuer DN

        CertGenerationContainer container = new CertGenerationContainer();
        bb.flip();

        try
        {
            decoder.decode( bb, container );
            fail();
        }
        catch ( DecoderException e )
        {
            assertTrue( true );
        }


        bb = ByteBuffer.allocate( 12 );

        bb.put( new byte[]
                         { 0x30, 0x10, // CertGenerateObject ::= SEQUENCE {
                           0x04, 0x04, 'c', 'n', '=', 'x', // target DN string 
                           0x04, 0x02, '=', 'x' } ); // empty issuer DN

        bb.flip();

        try
        {
            decoder.decode( bb, container );
            fail( "shouldn't accept the invalid issuerDN" );
        }
        catch ( DecoderException e )
        {
            assertTrue( true );
        }
    }


    @Test
    public void testCertGenerationDecodeWithoutSubjectDN()
    {
        Asn1Decoder decoder = new LdapDecoder();
        
        ByteBuffer bb = ByteBuffer.allocate( 17 );

        bb.put( new byte[]
            { 0x30, 0x15, // CertGenerateObject ::= SEQUENCE {
              0x04, 0x04, 'c', 'n', '=', 'x', // target DN string 
              0x04, 0x04, 'c', 'n', '=', 'x', // issuer DN
              0x04, 0x01, ' ' } ); // empty subject DN

        CertGenerationContainer container = new CertGenerationContainer();
        bb.flip();

        try
        {
            decoder.decode( bb, container );
            fail();
        }
        catch ( DecoderException e )
        {
            assertTrue( true );
        }


        bb = ByteBuffer.allocate( 18 );

        bb.put( new byte[]
                         { 0x30, 0x16, // CertGenerateObject ::= SEQUENCE {
                           0x04, 0x04, 'c', 'n', '=', 'x', // target DN string 
                           0x04, 0x04, 'c', 'n', '=', 'x', // issuer DN
                           0x04, 0x02, '=', 'x' } ); // invalid subject DN

        bb.flip();

        try
        {
            decoder.decode( bb, container );
            fail( "shouldn't accept the invalid subject DN" );
        }
        catch ( DecoderException e )
        {
            assertTrue( true );
        }
    }
    
    
    @Test
    public void testDecodeEmptySequence()
    {
        Asn1Decoder decoder = new LdapDecoder();
        
        ByteBuffer bb = ByteBuffer.allocate( 2 );

        bb.put( new byte[]
            { 0x30, 0x00 }); // CertGenerateObject ::= SEQUENCE {

        CertGenerationContainer container = new CertGenerationContainer();
        bb.flip();

        try
        {
            decoder.decode( bb, container );
            // The PDU with an empty sequence is not allowed
            fail();
        }
        catch ( DecoderException e )
        {
            assertTrue( true );
        }
    }
}
