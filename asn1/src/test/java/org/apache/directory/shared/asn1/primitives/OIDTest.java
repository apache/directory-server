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
package org.apache.directory.shared.asn1.primitives;


import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.primitives.OID;


/**
 * Test the OID primitive
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class OIDTest extends TestCase
{
    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Test a null OID
     */
    public void testOidNull()
    {

        OID oid = new OID();

        try
        {
            oid.setOID( ( byte[] ) null );
            Assert.fail( "Should not reach this point ..." );
        }
        catch ( DecoderException de )
        {
            Assert.assertTrue( true );
        }
    }


    /**
     * Test an empty OID
     */
    public void testOidEmpty()
    {

        OID oid = new OID();

        try
        {
            oid.setOID( new byte[]
                {} );
            Assert.fail( "Should not reach this point ..." );
        }
        catch ( DecoderException de )
        {
            Assert.assertTrue( true );
        }
    }


    /**
     * Test itu-t OID tree
     */
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
                Assert.assertEquals( "0.0." + i, oid.toString() );
            }

            // itu-t(0), question(1)
            oid.setOID( new byte[]
                { 0x01 } );
            Assert.assertEquals( "0.1", oid.toString() );

            // itu-t(0), administration(2), country(202 .. 748)
            for ( int i = 202; i < 748; i++ )
            {
                oid.setOID( new byte[]
                    { 0x02, ( byte ) ( ( i / 128 ) | 0x0080 ), ( byte ) ( i % 128 ) } );
                Assert.assertEquals( "0.2." + i, oid.toString() );
            }

            // itu-t(0), network-operator(3), operator(2023 .. 41363)
            for ( int i = 2023; i < 41363; i++ )
            {

                if ( i < ( 128 * 128 ) )
                {
                    oid.setOID( new byte[]
                        { 0x03, ( byte ) ( ( i / 128 ) | 0x0080 ), ( byte ) ( i % 128 ) } );
                    Assert.assertEquals( "0.3." + i, oid.toString() );
                }
                else
                {
                    oid.setOID( new byte[]
                        { 0x03, ( byte ) ( ( i / ( 128 * 128 ) ) | 0x0080 ),
                            ( byte ) ( ( ( i / 128 ) % 128 ) | 0x0080 ), ( byte ) ( i % 128 ) } );
                    Assert.assertEquals( "0.3." + i, oid.toString() );

                }
            }
        }
        catch ( DecoderException de )
        {
            Assert.fail();
        }
    }


    /**
     * Test iso OID tree
     */
    public void testOidIso()
    {

        OID oid = new OID();

        try
        {

            // iso(1), standard(0)
            oid.setOID( new byte[]
                { 40 + 0 } );
            Assert.assertEquals( "1.0", oid.toString() );

            // iso(1), registration-authority(1)
            oid.setOID( new byte[]
                { 40 + 1 } );
            Assert.assertEquals( "1.1", oid.toString() );

            // iso(1), member-body(2)
            oid.setOID( new byte[]
                { 40 + 2 } );
            Assert.assertEquals( "1.2", oid.toString() );

            // iso(1), identified-organization(3) | org(3) | organization(3)
            oid.setOID( new byte[]
                { 40 + 3 } );
            Assert.assertEquals( "1.3", oid.toString() );
        }
        catch ( DecoderException de )
        {
            Assert.fail();
        }
    }


    /**
     * Test joint-iso-itu-t OID tree
     */
    public void testOidJointIsoItuT()
    {

        OID oid = new OID();

        try
        {

            // joint-iso-itu-t(2), presentation(0)
            oid.setOID( new byte[]
                { 80 + 0 } );
            Assert.assertEquals( "2.0", oid.toString() );

            // joint-iso-itu-t(2), asn1(1)
            oid.setOID( new byte[]
                { 80 + 1 } );
            Assert.assertEquals( "2.1", oid.toString() );

            // joint-iso-itu-t(2), association-control(2)
            oid.setOID( new byte[]
                { 80 + 2 } );
            Assert.assertEquals( "2.2", oid.toString() );

            // joint-iso-itu-t(2), reliable-transfer(3)
            oid.setOID( new byte[]
                { 80 + 3 } );
            Assert.assertEquals( "2.3", oid.toString() );

            // ...
            // joint-iso-itu-t(2), upu(40)
            oid.setOID( new byte[]
                { 80 + 40 } );
            Assert.assertEquals( "2.40", oid.toString() );

            // ...
            // joint-iso-itu-t(2), xxx(100)
            oid.setOID( new byte[]
                { ( byte ) ( 0x81 ), 0x34 } );
            Assert.assertEquals( "2.100", oid.toString() );
        }
        catch ( DecoderException de )
        {
            Assert.fail();
        }
    }


    /**
     * Test valid String OIDs
     */
    public void testOidStringGood()
    {

        OID oid = new OID();

        try
        {
            oid.setOID( "0.0" );
            Assert.assertEquals( "0.0", oid.toString() );

            oid.setOID( "0.0.0.0.0" );
            Assert.assertEquals( "0.0.0.0.0", oid.toString() );

            oid.setOID( "0.1.2.3.4" );
            Assert.assertEquals( "0.1.2.3.4", oid.toString() );

            oid.setOID( "2.123456" );
            Assert.assertEquals( "2.123456", oid.toString() );

            oid.setOID( "1.2.840.113554.1.2.2" );
            Assert.assertEquals( "1.2.840.113554.1.2.2", oid.toString() );
        }
        catch ( DecoderException de )
        {
            Assert.fail();
        }
    }


    /**
     * Test invalid String OIDs
     */
    public void testOidStringBad()
    {

        OID oid = new OID();

        try
        {
            oid.setOID( "0" );
        }
        catch ( DecoderException de )
        {
            Assert.assertTrue( true );
        }

        try
        {
            oid.setOID( "0." );
        }
        catch ( DecoderException de )
        {
            Assert.assertTrue( true );
        }

        try
        {
            oid.setOID( "." );
        }
        catch ( DecoderException de )
        {
            Assert.assertTrue( true );
        }

        try
        {
            oid.setOID( "0.1.2." );
        }
        catch ( DecoderException de )
        {
            Assert.assertTrue( true );
        }

        try
        {
            oid.setOID( "3.1" );
        }
        catch ( DecoderException de )
        {
            Assert.assertTrue( true );
        }

        try
        {
            oid.setOID( "0..1" );
        }
        catch ( DecoderException de )
        {
            Assert.assertTrue( true );
        }

        try
        {
            oid.setOID( "0..12" );
        }
        catch ( DecoderException de )
        {
            Assert.assertTrue( true );
        }

        try
        {
            oid.setOID( "0.a.2" );
        }
        catch ( DecoderException de )
        {
            Assert.assertTrue( true );
        }

        try
        {
            oid.setOID( "0.123456" );
        }
        catch ( DecoderException de )
        {
            Assert.assertTrue( true );
        }

        try
        {
            oid.setOID( "1.123456" );
        }
        catch ( DecoderException de )
        {
            Assert.assertTrue( true );
        }

    }


    /**
     * Test Spnego OID
     */
    public void testOidSpnego()
    {

        OID oid = new OID();

        try
        {
            oid.setOID( new byte[]
                { 0x2b, 0x06, 0x01, 0x05, 0x05, 0x02 } );

            Assert.assertEquals( "1.3.6.1.5.5.2", oid.toString() );
        }
        catch ( DecoderException de )
        {
            Assert.fail();
        }
    }


    /**
     * Test Kerberos V5 OID
     */
    public void testOidKerberosV5()
    {

        OID oid = new OID();

        try
        {
            oid.setOID( new byte[]
                { 0x2a, ( byte ) 0x86, 0x48, ( byte ) 0x86, ( byte ) 0xf7, 0x12, 0x01, 0x02, 0x02 } );

            Assert.assertEquals( "1.2.840.113554.1.2.2", oid.toString() );
        }
        catch ( DecoderException de )
        {
            Assert.fail();
        }
    }


    /**
     * Test OIDs bytes
     */
    public void testOidBytes()
    {
        OID oid = new OID();
        OID oid2 = new OID();

        try
        {
            oid.setOID( "0.0" );
            oid2.setOID( oid.getOID() );
            Assert.assertEquals( oid.toString(), oid2.toString() );

            oid.setOID( "0.0.0.0.0" );
            oid2.setOID( oid.getOID() );
            Assert.assertEquals( oid.toString(), oid2.toString() );

            oid.setOID( "0.1.2.3.4" );
            oid2.setOID( oid.getOID() );
            Assert.assertEquals( oid.toString(), oid2.toString() );

            oid.setOID( "2.123456" );
            oid2.setOID( oid.getOID() );
            Assert.assertEquals( oid.toString(), oid2.toString() );

            oid.setOID( "1.2.840.113554.1.2.2" );
            oid2.setOID( oid.getOID() );
            Assert.assertEquals( oid.toString(), oid2.toString() );
        }
        catch ( DecoderException de )
        {
            Assert.fail();
        }
    }
}
