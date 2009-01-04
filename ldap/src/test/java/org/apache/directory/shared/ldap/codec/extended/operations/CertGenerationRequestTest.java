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


import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.codec.LdapDecoder;
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
            e.printStackTrace();
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
    public void testCertGenrationDecodeWithoutTargetDN()
    {
        Asn1Decoder decoder = new LdapDecoder();

        ByteBuffer bb = ByteBuffer.allocate( 5 );

        bb.put( new byte[]
            { 0x30, 0x03, // CertGenerateObject ::= SEQUENCE {
              0x04, 0x01, ' ' } ); // empty targetDN value

        String decodedPdu = StringTools.dumpBytes( bb.array() );
        bb.flip();

        CertGenerationContainer container = new CertGenerationContainer();

        try
        {
            decoder.decode( bb, container );
            fail( "shouldn't accept the empty targetDN" );
        }
        catch ( DecoderException e )
        {
            e.printStackTrace();
            assertTrue( true );
        }
                
    }
    
    
    @Test
    public void testNullvalues()
    {
        Asn1Decoder decoder = new LdapDecoder();
        
        ByteBuffer bb = ByteBuffer.allocate( 5 );

        bb.put( new byte[]
            { 0x30, 0x03, // CertGenerateObject ::= SEQUENCE {
              0x04, 0x01, 'x' } ); // non empty DN string

        CertGenerationContainer container = new CertGenerationContainer();
        bb.flip();

        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException e )
        {
            e.printStackTrace();
            fail( e.getMessage() );
        }

        CertGenerationObject certGenObj = container.getCertGenerationObject();
        
        assertEquals( "x", certGenObj.getTargetDN() );
        assertNull( certGenObj.getIssuerDN() );
        assertNull( certGenObj.getSubjectDN() );
        assertNull( certGenObj.getKeyAlgorithm() );
    }
}
