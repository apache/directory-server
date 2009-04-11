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
package org.apache.directory.shared.asn1.primitives;


import org.apache.directory.shared.asn1.codec.DecoderException;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;


/**
 * Test the OID primitive
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class OIDTest
{
    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Test a null OID
     */
    @Test
    public void testOidNull()
    {

        OID oid = new OID();

        try
        {
            oid.setOID( ( byte[] ) null );
            fail( "Should not reach this point ..." );
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
        }
    }


    /**
     * Test an empty OID
     */
    @Test
    public void testOidEmpty()
    {

        OID oid = new OID();

        try
        {
            oid.setOID( new byte[]
                {} );
            fail( "Should not reach this point ..." );
        }
        catch ( DecoderException de )
        {
            assertTrue( true );
        }
    }


    /**
     * Test itu-t OID tree
     */
    @Test
    public void testOidItuT()
    {

        OID oid = new OID();

        try
        {

            // itu-t(0), recommendation(0), series a-z (0..26)
            for ( int i = 1; i < 27; i++ )
            {
                oid.setOID( new byte[]
                    { 0x00, ( byte ) i } );
                assertEquals( "0.0." + i, oid.toString() );
            }

            // itu-t(0), question(1)
            oid.setOID( new byte[]
                { 0x01 } );
            assertEquals( "0.1", oid.toString() );

            // itu-t(0), administration(2), country(202 .. 748)
            for ( int i = 202; i < 748; i++ )
            {
                oid.setOID( new byte[]
                    { 0x02, ( byte ) ( ( i / 128 ) | 0x0080 ), ( byte ) ( i % 128 ) } );
                assertEquals( "0.2." + i, oid.toString() );
            }

            // itu-t(0), network-operator(3), operator(2023 .. 41363)
            for ( int i = 2023; i < 41363; i++ )
            {

                if ( i < ( 128 * 128 ) )
                {
                    oid.setOID( new byte[]
                        { 0x03, ( byte ) ( ( i / 128 ) | 0x0080 ), ( byte ) ( i % 128 ) } );
                    assertEquals( "0.3." + i, oid.toString() );
                }
                else
                {
                    oid.setOID( new byte[]
                        { 0x03, ( byte ) ( ( i / ( 128 * 128 ) ) | 0x0080 ),
                            ( byte ) ( ( ( i / 128 ) % 128 ) | 0x0080 ), ( byte ) ( i % 128 ) } );
                    assertEquals( "0.3." + i, oid.toString() );

                }
            }
        }
        catch ( DecoderException de )
        {
            fail();
        }
    }


    /**
     * Test iso OID tree
     */
    @Test
    public void testOidIso()
    {

        OID oid = new OID();

        try
        {

            // iso(1), standard(0)
            oid.setOID( new byte[]
                { 40 + 0 } );
            assertEquals( "1.0", oid.toString() );

            // iso(1), registration-authority(1)
            oid.setOID( new byte[]
                { 40 + 1 } );
            assertEquals( "1.1", oid.toString() );

            // iso(1), member-body(2)
            oid.setOID( new byte[]
                { 40 + 2 } );
            assertEquals( "1.2", oid.toString() );

            // iso(1), identified-organization(3) | org(3) | organization(3)
            oid.setOID( new byte[]
                { 40 + 3 } );
            assertEquals( "1.3", oid.toString() );
        }
        catch ( DecoderException de )
        {
            fail();
        }
    }


    /**
     * Test joint-iso-itu-t OID tree
     */
    @Test
    public void testOidJointIsoItuT()
    {

        OID oid = new OID();

        try
        {

            // joint-iso-itu-t(2), presentation(0)
            oid.setOID( new byte[]
                { 80 + 0 } );
            assertEquals( "2.0", oid.toString() );

            // joint-iso-itu-t(2), asn1(1)
            oid.setOID( new byte[]
                { 80 + 1 } );
            assertEquals( "2.1", oid.toString() );

            // joint-iso-itu-t(2), association-control(2)
            oid.setOID( new byte[]
                { 80 + 2 } );
            assertEquals( "2.2", oid.toString() );

            // joint-iso-itu-t(2), reliable-transfer(3)
            oid.setOID( new byte[]
                { 80 + 3 } );
            assertEquals( "2.3", oid.toString() );

            // ...
            // joint-iso-itu-t(2), upu(40)
            oid.setOID( new byte[]
                { 80 + 40 } );
            assertEquals( "2.40", oid.toString() );

            // ...
            // joint-iso-itu-t(2), xxx(100)
            oid.setOID( new byte[]
                { ( byte ) ( 0x81 ), 0x34 } );
            assertEquals( "2.100", oid.toString() );
        }
        catch ( DecoderException de )
        {
            fail();
        }
    }


    /**
     * Test valid String OIDs
     */
    @Test
    public void testOidStringGood()
    {

        OID oid = new OID();

        try
        {
            oid.setOID( "0.0" );
            assertEquals( "0.0", oid.toString() );

            oid.setOID( "0.0.0.0.0" );
            assertEquals( "0.0.0.0.0", oid.toString() );

            oid.setOID( "0.1.2.3.4" );
            assertEquals( "0.1.2.3.4", oid.toString() );

            oid.setOID( "2.123456" );
            assertEquals( "2.123456", oid.toString() );

            oid.setOID( "1.2.840.113554.1.2.2" );
            assertEquals( "1.2.840.113554.1.2.2", oid.toString() );
        }
        catch ( DecoderException de )
        {
            fail();
        }
    }


    /**
     * Test invalid String OIDs
     */
    @Test
    public void testOidStringBad()
    {
        assertFalse( OID.isOID( "0" ) );
        assertFalse( OID.isOID( "0." ) );
        assertFalse( OID.isOID( "." ) );
        assertFalse( OID.isOID( "0.1.2." ) );
        assertFalse( OID.isOID( "3.1" ) );
        assertFalse( OID.isOID( "0..1" ) );
        assertFalse( OID.isOID( "0..12" ) );
        assertFalse( OID.isOID( "0.a.2" ) );
        assertTrue( OID.isOID( "0.123456" ) );
        assertTrue( OID.isOID( "1.123456" ) );
    }


    /**
     * Test Spnego OID
     */
    @Test
    public void testOidSpnego()
    {

        OID oid = new OID();

        try
        {
            oid.setOID( new byte[]
                { 0x2b, 0x06, 0x01, 0x05, 0x05, 0x02 } );

            assertEquals( "1.3.6.1.5.5.2", oid.toString() );
        }
        catch ( DecoderException de )
        {
            fail();
        }
    }


    /**
     * Test Kerberos V5 OID
     */
    @Test
    public void testOidKerberosV5()
    {

        OID oid = new OID();

        try
        {
            oid.setOID( new byte[]
                { 0x2a, ( byte ) 0x86, 0x48, ( byte ) 0x86, ( byte ) 0xf7, 0x12, 0x01, 0x02, 0x02 } );

            assertEquals( "1.2.840.113554.1.2.2", oid.toString() );
        }
        catch ( DecoderException de )
        {
            fail();
        }
    }


    /**
     * Test OIDs bytes
     */
    @Test
    public void testOidBytes()
    {
        OID oid = new OID();
        OID oid2 = new OID();

        try
        {
            oid.setOID( "0.0" );
            oid2.setOID( oid.getOID() );
            assertEquals( oid.toString(), oid2.toString() );

            oid.setOID( "0.0.0.0.0" );
            oid2.setOID( oid.getOID() );
            assertEquals( oid.toString(), oid2.toString() );

            oid.setOID( "0.1.2.3.4" );
            oid2.setOID( oid.getOID() );
            assertEquals( oid.toString(), oid2.toString() );

            oid.setOID( "2.123456" );
            oid2.setOID( oid.getOID() );
            assertEquals( oid.toString(), oid2.toString() );

            oid.setOID( "1.2.840.113554.1.2.2" );
            oid2.setOID( oid.getOID() );
            assertEquals( oid.toString(), oid2.toString() );
        }
        catch ( DecoderException de )
        {
            fail();
        }
    }

    /**
     * Test OID Equals
     */
    @Test
    public void testOidEquals() throws DecoderException
    {
        OID oid1 = new OID();
        OID oid2 = new OID();
        OID oid3 = new OID( "1.1" );

        assertTrue( oid1.equals( oid2 ) );
        assertFalse( oid1.equals( oid3 ) );
        assertFalse( oid2.equals( oid3 ) );
    }

    /**
     * Test OID Equals
     */
    @Test
    public void testOidEqualsPerf() throws DecoderException
    {
        String s1 = "1.2.840.113554.1.2.2.1.2.840.113554.1.2.2.1.2.840.113554.1.2.2";
        String s2 = "1.2.840.113554.1.2.2.1.2.840.113554.1.2.2.1.2.840.113554.1.2.2";
        String s3 = "1.3.6.1.5.5.2";
        
        OID oid1 = new OID( s1 );
        OID oid2 = new OID( s2 );
        OID oid3 = new OID( s3 );
        
        assertTrue( oid1.equals( oid2 ) );
        assertFalse( oid1.equals( oid3 ) );
        assertFalse( oid2.equals( oid3 ) );
    }
}
