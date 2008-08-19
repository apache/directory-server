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
package org.apache.directory.shared.ldap.name;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;

import org.apache.directory.shared.ldap.util.StringTools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;


/**
 * Test the class AttributeTypeAndValue
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$, 
 */
public class AttributeTypeAndValueTest
{
    // ~ Methods
    // ------------------------------------------------------------------------------------
    /**
     * Test a null AttributeTypeAndValue
     */
    @Test
    public void testAttributeTypeAndValueNull()
    {
        AttributeTypeAndValue atav = new AttributeTypeAndValue();
        assertEquals( "", atav.toString() );
        assertEquals( "", atav.getUpName() );
        assertEquals( -1, atav.getStart() );
        assertEquals( 0, atav.getLength() );
    }


    /**
     * Test a null type for an AttributeTypeAndValue
     */
    @Test
    public void testAttributeTypeAndValueNullType() throws InvalidNameException
    {
        try
        {
            new AttributeTypeAndValue( null, null, null, null );
            fail();
        }
        catch ( InvalidNameException ine )
        {
            assertTrue( true );
        }

    }

    /**
     * Test an invalid type for an AttributeTypeAndValue
     */
    @Test
    public void testAttributeTypeAndValueInvalidType() throws InvalidNameException
    {
        try
        {
            new AttributeTypeAndValue( "  ", " ", null, null );
            fail();
        }
        catch ( InvalidNameException ine )
        {
            assertTrue( true );
        }
    }


    /**
     * Test a valid type for an AttributeTypeAndValue
     */
    @Test
    public void testAttributeTypeAndValueValidType() throws InvalidNameException
    {
        AttributeTypeAndValue atav = new AttributeTypeAndValue( "A", "a", null, null );
        assertEquals( "a=", atav.toString() );
        assertEquals( "A=", atav.getUpName() );
        
        atav = new AttributeTypeAndValue( "  A  ", "a", null, null );
        assertEquals( "a=", atav.toString() );
        assertEquals( "  A  =", atav.getUpName() );
        
        atav = new AttributeTypeAndValue( "  A  ", null, null, null );
        assertEquals( "a=", atav.toString() );
        assertEquals( "  A  =", atav.getUpName() );
        
        atav = new AttributeTypeAndValue( null, "a", null, null );
        assertEquals( "a=", atav.toString() );
        assertEquals( "a=", atav.getUpName() );
        
    }

    /**
     * test an empty AttributeTypeAndValue
     */
    @Test
    public void testLdapRDNEmpty()
    {
        try
        {
            new AttributeTypeAndValue( "", "", "", "" );
            fail( "Should not occurs ... " );
        }
        catch ( InvalidNameException ine )
        {
            assertTrue( true );
        }
    }


    /**
     * test a simple AttributeTypeAndValue : a = b
     */
    @Test
    public void testLdapRDNSimple() throws InvalidNameException
    {
        AttributeTypeAndValue atav = new AttributeTypeAndValue( "a", "a", "b", "b" );
        assertEquals( "a=b", atav.toString() );
        assertEquals( "a=b", atav.getUpName() );
        assertEquals( 0, atav.getStart() );
        assertEquals( 3, atav.getLength() );
    }


    /**
     * Compares two equals atavs
     */
    @Test
    public void testCompareToEquals() throws InvalidNameException
    {
        AttributeTypeAndValue atav1 = new AttributeTypeAndValue( "a", "a", "b", "b" );
        AttributeTypeAndValue atav2 = new AttributeTypeAndValue( "a", "a", "b", "b" );

        assertEquals( 0, atav1.compareTo( atav2 ) );
    }


    /**
     * Compares two equals atavs but with a type in different case
     */
    @Test
    public void testCompareToEqualsCase() throws InvalidNameException
    {
        AttributeTypeAndValue atav1 = new AttributeTypeAndValue( "a", "a", "b", "b" );
        AttributeTypeAndValue atav2 = new AttributeTypeAndValue( "A", "A", "b", "b" );

        assertEquals( 0, atav1.compareTo( atav2 ) );
    }


    /**
     * Compare two atavs : the first one is superior because its type is
     * superior
     */
    @Test
    public void testCompareAtav1TypeSuperior() throws InvalidNameException
    {
        AttributeTypeAndValue atav1 = new AttributeTypeAndValue( "b", "b", "b", "b" );
        AttributeTypeAndValue atav2 = new AttributeTypeAndValue( "a", "a", "b", "b" );

        assertEquals( 1, atav1.compareTo( atav2 ) );
    }


    /**
     * Compare two atavs : the second one is superior because its type is
     * superior
     */
    @Test
    public void testCompareAtav2TypeSuperior() throws InvalidNameException
    {
        AttributeTypeAndValue atav1 = new AttributeTypeAndValue( "a", "a", "b", "b" );
        AttributeTypeAndValue atav2 = new AttributeTypeAndValue( "b", "b", "b", "b" );

        assertEquals( -1, atav1.compareTo( atav2 ) );
    }


    /**
     * Compare two atavs : the first one is superior because its type is
     * superior
     */
    @Test
    public void testCompareAtav1ValueSuperior() throws InvalidNameException
    {
        AttributeTypeAndValue atav1 = new AttributeTypeAndValue( "a", "a", "b", "b" );
        AttributeTypeAndValue atav2 = new AttributeTypeAndValue( "a", "a", "a", "a" );

        assertEquals( 1, atav1.compareTo( atav2 ) );
    }


    /**
     * Compare two atavs : the second one is superior because its type is
     * superior
     */
    @Test
    public void testCompareAtav2ValueSuperior() throws InvalidNameException
    {
        AttributeTypeAndValue atav1 = new AttributeTypeAndValue( "a", "a", "a", "a" );
        AttributeTypeAndValue atav2 = new AttributeTypeAndValue( "a", "a", "b", "b" );

        assertEquals( -1, atav1.compareTo( atav2 ) );
    }


    @Test
    public void testNormalize() throws InvalidNameException
    {
        AttributeTypeAndValue atav = new AttributeTypeAndValue( " A ", " A ", "a", "a" );

        assertEquals( "a=a", atav.normalize() );

    }


    /** Serialization tests ------------------------------------------------- */

    /**
     * Test serialization of a simple ATAV
     */
    @Test
    public void testStringAtavSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        AttributeTypeAndValue atav = new AttributeTypeAndValue( "cn", "CN", "test", "Test" );

        atav.normalize();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        out.writeObject( atav );

        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );

        AttributeTypeAndValue atav2 = (AttributeTypeAndValue)in.readObject();

        assertEquals( atav, atav2 );
    }


    @Test
    public void testBinaryAtavSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        byte[] upValue = StringTools.getBytesUtf8( "  Test  " );
        byte[] normValue = StringTools.getBytesUtf8( "Test" );

        AttributeTypeAndValue atav = new AttributeTypeAndValue( "cn", "CN", upValue, normValue );

        atav.normalize();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        out.writeObject( atav );

        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );

        AttributeTypeAndValue atav2 = (AttributeTypeAndValue)in.readObject();

        assertEquals( atav, atav2 );
    }


    /**
     * Test serialization of a simple ATAV
     */
    @Test
    public void testNullAtavSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        AttributeTypeAndValue atav = new AttributeTypeAndValue();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        try
        {
            out.writeObject( atav );
            fail();
        }
        catch ( IOException ioe )
        {
            assertTrue( true );
        }
    }


    @Test
    public void testNullNormValueSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        AttributeTypeAndValue atav = new AttributeTypeAndValue( "CN", "cn", "test", (String)null );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        try
        {
            out.writeObject( atav );
            fail();
        }
        catch ( IOException ioe )
        {
            String message = ioe.getMessage();
            assertEquals( "Cannot serialize an wrong ATAV, the value should not be null", message );
        }
    }


    @Test
    public void testNullUpValueSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        AttributeTypeAndValue atav = new AttributeTypeAndValue( "CN", "cn", null, "test" );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        try
        {
            out.writeObject( atav );
            fail();
        }
        catch ( IOException ioe )
        {
            String message = ioe.getMessage();
            assertEquals( "Cannot serialize an wrong ATAV, the upValue should not be null", message );
        }
    }


    @Test
    public void testEmptyNormValueSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        AttributeTypeAndValue atav = new AttributeTypeAndValue( "CN", "cn", "test", "" );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        out.writeObject( atav );

        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );

        AttributeTypeAndValue atav2 = (AttributeTypeAndValue)in.readObject();

        assertEquals( atav, atav2 );
    }


    @Test
    public void testEmptyUpValueSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        AttributeTypeAndValue atav = new AttributeTypeAndValue( "CN", "cn", "", "test" );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        out.writeObject( atav );

        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );

        AttributeTypeAndValue atav2 = (AttributeTypeAndValue)in.readObject();

        assertEquals( atav, atav2 );
    }


    /**
     * Test serialization of a simple ATAV
     */
    @Test
    public void testStringAtavStaticSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        AttributeTypeAndValue atav = new AttributeTypeAndValue( "cn", "CN", "test", "Test" );

        atav.normalize();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        AtavSerializer.serialize( atav, out );
        out.flush();

        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );

        AttributeTypeAndValue atav2 = AtavSerializer.deserialize( in );

        assertEquals( atav, atav2 );
    }


    @Test
    public void testBinaryAtavStaticSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        byte[] upValue = StringTools.getBytesUtf8( "  Test  " );
        byte[] normValue = StringTools.getBytesUtf8( "Test" );

        AttributeTypeAndValue atav = new AttributeTypeAndValue( "cn", "CN", upValue, normValue );

        atav.normalize();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        AtavSerializer.serialize( atav, out );
        out.flush();

        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );

        AttributeTypeAndValue atav2 = AtavSerializer.deserialize( in );

        assertEquals( atav, atav2 );
    }


    /**
     * Test static serialization of a simple ATAV
     */
    @Test
    public void testNullAtavStaticSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        AttributeTypeAndValue atav = new AttributeTypeAndValue();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        try
        {
            AtavSerializer.serialize( atav, out );
            fail();
        }
        catch ( IOException ioe )
        {
            assertTrue( true );
        }
    }


    @Test
    public void testNullNormValueStaticSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        AttributeTypeAndValue atav = new AttributeTypeAndValue( "CN", "cn", "test", (String)null );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        try
        {
            AtavSerializer.serialize( atav, out );
            fail();
        }
        catch ( IOException ioe )
        {
            String message = ioe.getMessage();
            assertEquals( "Cannot serialize an wrong ATAV, the value should not be null", message );
        }
    }


    @Test
    public void testNullUpValueStaticSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        AttributeTypeAndValue atav = new AttributeTypeAndValue( "CN", "cn", null, "test" );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        try
        {
            AtavSerializer.serialize( atav, out );
            fail();
        }
        catch ( IOException ioe )
        {
            String message = ioe.getMessage();
            assertEquals( "Cannot serialize an wrong ATAV, the upValue should not be null", message );
        }
    }


    @Test
    public void testEmptyNormValueStaticSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        AttributeTypeAndValue atav = new AttributeTypeAndValue( "CN", "cn", "test", "" );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        AtavSerializer.serialize( atav, out );
        out.flush();

        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );

        AttributeTypeAndValue atav2 = AtavSerializer.deserialize( in );

        assertEquals( atav, atav2 );
    }


    @Test
    public void testEmptyUpValueStaticSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        AttributeTypeAndValue atav = new AttributeTypeAndValue( "CN", "cn", "", "test" );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        AtavSerializer.serialize( atav, out );
        out.flush();

        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );

        AttributeTypeAndValue atav2 = AtavSerializer.deserialize( in );

        assertEquals( atav, atav2 );
    }
}
