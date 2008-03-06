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
package org.apache.directory.server.core.entry;


import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.ByteArrayComparator;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.syntax.AcceptAllSyntaxChecker;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.Before;
import org.junit.Test;

import javax.naming.NamingException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;


/**
 * Tests that the ServerBinaryValue class works properly as expected.
 *
 * Some notes while conducting tests:
 *
 * <ul>
 *   <li>comparing values with different types - how does this behave</li>
 *   <li>exposing access to at from value or to a comparator?</li>
 * </ul>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ServerBinaryValueTest
{
    static private TestServerEntryUtils.S s;
    static private TestServerEntryUtils.AT at;
    static private TestServerEntryUtils.MR mr;
    
    private static final byte[] BYTES1 = new byte[]{0x01, 0x02, 0x03, 0x04};
    private static final byte[] BYTES2 = new byte[]{(byte)0x81, (byte)0x82, (byte)0x83, (byte)0x84};

    /**
     * Initialize an AttributeType and the associated MatchingRule 
     * and Syntax
     */
    @Before public void initAT()
    {
        s = new TestServerEntryUtils.S( "1.1.1.1", false );
        s.setSyntaxChecker( new AcceptAllSyntaxChecker( "1.1.1.1" ) );
        mr = new TestServerEntryUtils.MR( "1.1.2.1" );
        mr.syntax = s;
        mr.comparator = new ByteArrayComparator();
        mr.normalizer = new Normalizer()
        {
            public static final long serialVersionUID = 1L;
            
            public Object normalize( Object value ) throws NamingException
            {
                if ( value instanceof byte[] )
                {
                    byte[] val = (byte[])value;
                    // each byte will be changed to be > 0, and spaces will be trimmed
                    byte[] newVal = new byte[ val.length ];
                    int i = 0;
                    
                    for ( byte b:val )
                    {
                        newVal[i++] = (byte)(b & 0x007F); 
                    }
                    
                    return StringTools.trim( newVal );
                }

                throw new IllegalStateException( "expected byte[] to normalize" );
            }
        };
        at = new TestServerEntryUtils.AT( "1.1.3.1" );
        at.setEquality( mr );
        at.setOrdering( mr );
        at.setSubstr( mr );
        at.setSyntax( s );
    }
    

    /**
     * Test the constructor with bad AttributeType
     */
    @Test public void testBadConstructor()
    {
        try
        {
            new ServerBinaryValue( null );
            fail();
        }
        catch ( AssertionError ae )
        {
            // Expected...
        }
        
        // create a AT without any syntax
        AttributeType at = new TestServerEntryUtils.AT( "1.1.3.1" );
        
        try
        {
            new ServerBinaryValue( at );
            fail();
        }
        catch ( AssertionError ae )
        {
            // Expected...
        }
    }


    /**
     * Test the constructor with a null value
     */
    @Test public void testServerBinaryValueNullValue()
    {
        AttributeType at = TestServerEntryUtils.getBytesAttributeType();
        
        ServerBinaryValue value = new ServerBinaryValue( at, null );
        
        assertNull( value.getReference() );
        assertTrue( value.isNull() );
    }
    
    
    /**
     * Test the constructor with an empty value
     */
    @Test public void testServerBinaryValueEmptyValue()
    {
        AttributeType at = TestServerEntryUtils.getBytesAttributeType();
        
        ServerBinaryValue value = new ServerBinaryValue( at, StringTools.EMPTY_BYTES );
        
        assertEquals( StringTools.EMPTY_BYTES, value.getReference() );
        assertFalse( value.isNull() );
    }
    
    
    /**
     * Test the constructor with a value
     */
    @Test public void testServerBinaryValueNoValue()
    {
        AttributeType at = TestServerEntryUtils.getBytesAttributeType();
        byte[] val = new byte[]{0x01};
        ServerBinaryValue value = new ServerBinaryValue( at );
        
        value.set( val );
        assertTrue( Arrays.equals( val, value.getReference() ) );
        assertFalse( value.isNull() );
        assertTrue( Arrays.equals( val, value.getCopy() ) );
    }
    
    
    /**
     * Test the constructor with a value
     */
    @Test public void testServerBinaryValue()
    {
        AttributeType at = TestServerEntryUtils.getBytesAttributeType();
        byte[] val = new byte[]{0x01};
        ServerBinaryValue value = new ServerBinaryValue( at, val );
        
        assertTrue( Arrays.equals( val, value.getReference() ) );
        assertFalse( value.isNull() );
        assertTrue( Arrays.equals( val, value.getCopy() ) );
    }
    
    
    /**
     * Test the clone method
     */
    @Test
    public void testClone() throws NamingException
    {
        AttributeType at1 = TestServerEntryUtils.getBytesAttributeType();
        ServerBinaryValue sbv = new ServerBinaryValue( at1, null );
        
        ServerBinaryValue sbv1 = (ServerBinaryValue)sbv.clone();
        
        assertEquals( sbv, sbv1 );
        
        sbv.set( StringTools.EMPTY_BYTES );
        
        assertNotSame( sbv, sbv1 );
        assertTrue( Arrays.equals( StringTools.EMPTY_BYTES, sbv.get() ) );
        
        sbv.set(  BYTES2 );
        sbv1 = (ServerBinaryValue)sbv.clone();
        
        assertEquals( sbv, sbv1 );
        
        sbv.normalize();
        
        // Even if we didn't normalized sbv2, it should be equal to sbv,
        // as if they have the same AT, and the same value, they are equal.
        assertEquals( sbv, sbv1 );
    }
    

    /**
     * Test the equals method
     */
    @Test public void testEquals()
    {
        AttributeType at1 = TestServerEntryUtils.getBytesAttributeType();
        
        ServerBinaryValue value1 = new ServerBinaryValue( at1, new byte[]{0x01, (byte)0x02} );
        ServerBinaryValue value2 = new ServerBinaryValue( at1, new byte[]{0x01, (byte)0x02} );
        ServerBinaryValue value3 = new ServerBinaryValue( at1, new byte[]{0x01, (byte)0x82} );
        ServerBinaryValue value4 = new ServerBinaryValue( at1, new byte[]{0x01} );
        ServerBinaryValue value5 = new ServerBinaryValue( at1, null );
        ServerBinaryValue value6 = new ServerBinaryValue( at, new byte[]{0x01, 0x02} );
        ServerStringValue value7 = new ServerStringValue( TestServerEntryUtils.getIA5StringAttributeType(), 
            "test" );
        
        assertTrue( value1.equals( value1 ) );
        assertTrue( value1.equals( value2 ) );
        assertTrue( value1.equals( value3 ) );
        assertFalse( value1.equals( value4 ) );
        assertFalse( value1.equals( value5 ) );
        assertFalse( value1.equals( "test" ) );
        assertFalse( value1.equals( null ) );
        
        assertFalse( value1.equals( value6 ) );
        assertFalse( value1.equals( value7 ) );
    }

    
    /**
     * Test the getNormalizedValue method
     */
    @Test public void testGetNormalizedValue() throws NamingException
    {
        AttributeType at = TestServerEntryUtils.getBytesAttributeType();
        
        ServerBinaryValue value = new ServerBinaryValue( at, null );
        assertNull( value.getNormalizedValue() );

        value = new ServerBinaryValue( at, StringTools.EMPTY_BYTES );
        assertTrue( Arrays.equals(  StringTools.EMPTY_BYTES, value.getNormalizedValue() ) );

        value = new ServerBinaryValue( at, BYTES2 );
        assertTrue( Arrays.equals( BYTES1, value.getNormalizedValue() ) );
    }
    
    
    /**
     * Test the getNormalizedValue method
     */
    @Test public void testGetNormalizedValueCopy() throws NamingException
    {
        AttributeType at = TestServerEntryUtils.getBytesAttributeType();
        
        ServerBinaryValue value = new ServerBinaryValue( at, null );
        assertNull( value.getNormalizedValueCopy() );

        value = new ServerBinaryValue( at, StringTools.EMPTY_BYTES );
        assertTrue( Arrays.equals(  StringTools.EMPTY_BYTES, value.getNormalizedValueCopy() ) );

        value = new ServerBinaryValue( at, BYTES2 );
        assertTrue( Arrays.equals( BYTES1, value.getNormalizedValueCopy() ) );
    }
    
    
    /**
     * Test the getNormalizedValue method
     */
    @Test public void testGetNormalizedValueReference() throws NamingException
    {
        AttributeType at = TestServerEntryUtils.getBytesAttributeType();
        
        ServerBinaryValue value = new ServerBinaryValue( at, null );
        assertNull( value.getNormalizedValueReference() );

        value = new ServerBinaryValue( at, StringTools.EMPTY_BYTES );
        assertTrue( Arrays.equals(  StringTools.EMPTY_BYTES, value.getNormalizedValueReference() ) );

        value = new ServerBinaryValue( at, BYTES2 );
        assertTrue( Arrays.equals( BYTES1, value.getNormalizedValueReference() ) );
    }
    
    
    /**
     * Test the getAttributeType method
     */
    @Test
    public void testgetAttributeType() throws NamingException
    {
        AttributeType at = TestServerEntryUtils.getBytesAttributeType();
        ServerBinaryValue sbv = new ServerBinaryValue( at );
        
        assertEquals( at, sbv.getAttributeType() );
    }    

    
    /**
     * Test the isValid method
     * 
     * The SyntaxChecker does not accept values longer than 5 chars.
     */
    @Test public void testIsValid() throws NamingException
    {
        AttributeType at = TestServerEntryUtils.getBytesAttributeType();
        
        ServerBinaryValue value = new ServerBinaryValue( at, null );
        assertTrue( value.isValid() );
        
        value = new ServerBinaryValue( at, StringTools.EMPTY_BYTES );
        assertTrue( value.isValid() );

        value = new ServerBinaryValue( at, new byte[]{0x01, 0x02} );
        assertTrue( value.isValid() );

        value = new ServerBinaryValue( at, new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06} );
        assertFalse( value.isValid() );
    }
    
    
    /**
     * Tests to make sure the hashCode method is working properly.
     * @throws Exception on errors
     */
    @Test public void testHashCode() throws Exception
    {
        AttributeType at = TestServerEntryUtils.getBytesAttributeType();
        ServerBinaryValue v0 = new ServerBinaryValue( at, new byte[]{0x01, 0x02} );
        ServerBinaryValue v1 = new ServerBinaryValue( at, new byte[]{(byte)0x81, (byte)0x82} );
        ServerBinaryValue v2 = new ServerBinaryValue( at, new byte[]{0x01, 0x02} );
        assertEquals( v0.hashCode(), v1.hashCode() );
        assertEquals( v1.hashCode(), v2.hashCode() );
        assertEquals( v0.hashCode(), v2.hashCode() );
        assertEquals( v0, v1 );
        assertEquals( v0, v2 );
        assertEquals( v1, v2 );
        assertTrue( v0.isValid() );
        assertTrue( v1.isValid() );
        assertTrue( v2.isValid() );

        ServerBinaryValue v3 = new ServerBinaryValue( at, new byte[]{0x01, 0x03} );
        assertFalse( v3.equals( v0 ) );
        assertFalse( v3.equals( v1 ) );
        assertFalse( v3.equals( v2 ) );
        assertTrue( v3.isValid() );
    }


    /**
     * Test the same method
     */
    @Test
    public void testSame() throws NamingException
    {
        AttributeType at = TestServerEntryUtils.getBytesAttributeType();
        ServerBinaryValue sbv = new ServerBinaryValue( at );

        sbv.normalize();
        assertTrue( sbv.isSame() );
        
        sbv.set( StringTools.EMPTY_BYTES );
        sbv.normalize();
        assertTrue( sbv.isSame() );

        sbv.set( BYTES1 );
        sbv.normalize();
        assertTrue( sbv.isSame() );

        sbv.set( BYTES2 );
        sbv.normalize();
        assertFalse( sbv.isSame() );
    }
    
    
    /**
     * Test the instanceOf method
     */
    @Test
    public void testInstanceOf() throws NamingException
    {
        AttributeType at = TestServerEntryUtils.getBytesAttributeType();
        ServerBinaryValue sbv = new ServerBinaryValue( at );
        
        assertTrue( sbv.instanceOf( at ) );
        
        at = TestServerEntryUtils.getIA5StringAttributeType();
        
        assertFalse( sbv.instanceOf( at ) );
    }    
    

    /**
     * Test the normalize method
     */
    @Test
    public void testNormalize() throws NamingException
    {
        AttributeType at = TestServerEntryUtils.getBytesAttributeType();
        ServerBinaryValue sbv = new ServerBinaryValue( at );

        sbv.normalize();
        assertEquals( null, sbv.getNormalizedValue() );
        
        sbv.set( StringTools.EMPTY_BYTES );
        sbv.normalize();
        assertTrue( Arrays.equals( StringTools.EMPTY_BYTES, sbv.getNormalizedValue() ) );

        sbv.set( BYTES2 );
        sbv.normalize();
        assertTrue( Arrays.equals( BYTES1, sbv.getNormalizedValue() ) );
    }
    

    /**
     * Test the compareTo method
     */
    @Test
    public void testCompareTo()
    {
        AttributeType at1 = TestServerEntryUtils.getBytesAttributeType();
        ServerBinaryValue v0 = new ServerBinaryValue( at1, BYTES1 );
        ServerBinaryValue v1 = new ServerBinaryValue( at1, BYTES2 );
        
        assertEquals( 0, v0.compareTo( v1 ) );
        assertEquals( 0, v1.compareTo( v0 ) );

        ServerBinaryValue v2 = new ServerBinaryValue( at1, null );
        
        assertEquals( 1, v0.compareTo( v2 ) );
        assertEquals( -1, v2.compareTo( v0 ) );
    }


    /**
     * Test serialization of a BinaryValue which has a normalized value
     */
    @Test public void testNormalizedBinaryValueSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        byte[] v1 = StringTools.getBytesUtf8( "  Test   Test  " );
        byte[] v1Norm = StringTools.getBytesUtf8( "Test   Test" );
        
        // First check with a value which will be normalized
        ServerBinaryValue sv = new ServerBinaryValue( at, v1 );
        
        sv.normalize();
        byte[] normalized = sv.getNormalizedValueReference();
        
        assertTrue( Arrays.equals( v1Norm, normalized ) );
        assertTrue( Arrays.equals( v1, sv.getReference() ) );
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );
        
        sv.writeExternal( out );
        
        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );
        
        ServerBinaryValue sv2 = new ServerBinaryValue( at );
        sv2.readExternal( in );
        
        assertEquals( sv, sv2 );
    }


    /**
     * Test serialization of a BinaryValue which does not have a normalized value
     */
    @Test public void testNoNormalizedBinaryValueSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        byte[] v1 = StringTools.getBytesUtf8( "test" );
        byte[] v1Norm = StringTools.getBytesUtf8( "test" );

        // First check with a value which will be normalized
        ServerBinaryValue sv = new ServerBinaryValue( at, v1 );
        
        sv.normalize();
        byte[] normalized = sv.getNormalizedValueReference();
        
        assertTrue( Arrays.equals( v1Norm, normalized ) );
        assertTrue( Arrays.equals( v1, sv.get() ) );
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );
        
        sv.writeExternal( out );
        
        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );
        
        ServerBinaryValue sv2 = new ServerBinaryValue( at );
        sv2.readExternal( in );
        
        assertEquals( sv, sv2 );
   }


    /**
     * Test serialization of a null BinaryValue
     */
    @Test public void testNullBinaryValueSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        // First check with a value which will be normalized
        ServerBinaryValue sv = new ServerBinaryValue( at );
        
        sv.normalize();
        byte[] normalized = sv.getNormalizedValueReference();
        
        assertEquals( null, normalized );
        assertEquals( null, sv.get() );
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );
        
        sv.writeExternal( out );
        
        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );
        
        ServerBinaryValue sv2 = new ServerBinaryValue( at );
        sv2.readExternal( in );
        
        assertEquals( sv, sv2 );
   }


    /**
     * Test serialization of an empty BinaryValue
     */
    @Test public void testEmptyBinaryValueSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        // First check with a value which will be normalized
        ServerBinaryValue sv = new ServerBinaryValue( at, StringTools.EMPTY_BYTES );
        
        sv.normalize();
        byte[] normalized = sv.getNormalizedValueReference();
        
        assertTrue( Arrays.equals( StringTools.EMPTY_BYTES, normalized ) );
        assertTrue( Arrays.equals( StringTools.EMPTY_BYTES, sv.get() ) );
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );
        
        sv.writeExternal( out );
        
        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );
        
        ServerBinaryValue sv2 = new ServerBinaryValue( at );
        sv2.readExternal( in );
        
        assertEquals( sv, sv2 );
   }
}