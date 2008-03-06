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


import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.entry.client.ClientStringValue;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.DeepTrimToLowerNormalizer;
import org.apache.directory.shared.ldap.schema.NoOpNormalizer;
import org.apache.directory.shared.ldap.schema.syntax.AcceptAllSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.SyntaxChecker;
import org.junit.Before;
import org.junit.Test;

import javax.naming.NamingException;
import javax.naming.directory.InvalidAttributeValueException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import jdbm.helper.StringComparator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;


/**
 * Tests that the ServerStringValue class works properly as expected.
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
public class ServerStringValueTest
{
    static private TestServerEntryUtils.S s;
    static private TestServerEntryUtils.AT at;
    static private TestServerEntryUtils.MR mr;

    
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
        mr.comparator = new StringComparator();
        mr.normalizer = new DeepTrimToLowerNormalizer();
        at = new TestServerEntryUtils.AT( "1.1.3.1" );
        at.setEquality( mr );
        at.setOrdering( mr );
        at.setSubstr( mr );
        at.setSyntax( s );
    }
    
    /**
     * Test the constructor with a null value
     */
    @Test 
    public void testServerStringValueNullValue()
    {
        AttributeType at = TestServerEntryUtils.getIA5StringAttributeType();
        
        ServerStringValue value = new ServerStringValue( at, null );
        
        assertNull( value.get() );
        assertTrue( value.isNull() );
    }
    
    
    /**
     * Test the getNormalizedValue method
     */
    @Test public void testGetNormalizedValue() throws NamingException
    {
        AttributeType at = TestServerEntryUtils.getIA5StringAttributeType();
        
        ServerStringValue value = new ServerStringValue( at, null );
        
        assertFalse( value.isNormalized() );
        assertNull( value.getNormalizedValue() );
        assertTrue( value.isNormalized() );

        value.set( "" );
        assertFalse( value.isNormalized() );
        assertEquals( "", value.getNormalizedValue() );
        assertTrue( value.isNormalized() );

        value.set( "TEST" );
        assertFalse( value.isNormalized() );
        assertEquals( "test", value.getNormalizedValue() );
        assertTrue( value.isNormalized() );
    }
    

    /**
     * Test the isValid method
     * 
     * The SyntaxChecker does not accept values longer than 5 chars.
     */
    @Test public void testIsValid() throws NamingException
    {
        AttributeType at = TestServerEntryUtils.getIA5StringAttributeType();
        
        ServerStringValue value = new ServerStringValue( at, null );
        assertTrue( value.isValid() );

        value.set( "" );
        assertTrue( value.isValid() );

        value.set( "TEST" );
        assertTrue( value.isValid() );

        value.set( "testlong" );
        assertFalse( value.isValid() );
    }
    
    
    /**
     * Test the normalize method
     */
    @Test
    public void testNormalize() throws NamingException
    {
        AttributeType at = TestServerEntryUtils.getIA5StringAttributeType();
        ServerStringValue ssv = new ServerStringValue( at );

        ssv.normalize();
        assertEquals( null, ssv.getNormalizedValue() );
        
        ssv.set( "" );
        ssv.normalize();
        assertEquals( "", ssv.getNormalizedValue() );

        ssv.set(  "  This is    a   TEST  " );
        ssv.normalize();
        assertEquals( "this is a test", ssv.getNormalizedValue() );
    }
    

    /**
     * Test the instanceOf method
     */
    @Test
    public void testInstanceOf() throws NamingException
    {
        AttributeType at = TestServerEntryUtils.getIA5StringAttributeType();
        ServerStringValue ssv = new ServerStringValue( at );
        
        assertTrue( ssv.instanceOf( at ) );
        
        at = TestServerEntryUtils.getBytesAttributeType();
        
        assertFalse( ssv.instanceOf( at ) );
    }    
    

    /**
     * Test the getAttributeType method
     */
    @Test
    public void testgetAttributeType() throws NamingException
    {
        AttributeType at = TestServerEntryUtils.getIA5StringAttributeType();
        ServerStringValue ssv = new ServerStringValue( at );
        
        assertEquals( at, ssv.getAttributeType() );
    }    

    
    /**
     * Test the equals method
     */
    @Test public void testEquals()
    {
        AttributeType at1 = TestServerEntryUtils.getIA5StringAttributeType();
        AttributeType at2 = TestServerEntryUtils.getBytesAttributeType();
        
        ServerStringValue value1 = new ServerStringValue( at1, "test" );
        ServerStringValue value2 = new ServerStringValue( at1, "test" );
        ServerStringValue value3 = new ServerStringValue( at1, "TEST" );
        ServerStringValue value4 = new ServerStringValue( at1, "tes" );
        ServerStringValue value5 = new ServerStringValue( at1, null );
        ServerBinaryValue valueBytes = new ServerBinaryValue( at2, new byte[]{0x01} );
        ServerStringValue valueString = new ServerStringValue( at, "test" );
        
        assertTrue( value1.equals( value1 ) );
        assertTrue( value1.equals( value2 ) );
        assertTrue( value1.equals( value3 ) );
        assertFalse( value1.equals( value4 ) );
        assertFalse( value1.equals( value5 ) );
        assertFalse( value1.equals( "test" ) );
        assertFalse( value1.equals( null ) );
        
        assertFalse( value1.equals( valueString ) );
        assertFalse( value1.equals( valueBytes ) );
    }

    
    
    
    
    
    
    
    
    
    /**
     * Test the constructor with bad AttributeType
     */
    @Test public void testBadConstructor()
    {
        try
        {
            new ServerStringValue( null );
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
            new ServerStringValue( at );
            fail();
        }
        catch ( AssertionError ae )
        {
            // Expected...
        }
    }


    /**
     * Tests to make sure the hashCode method is working properly.
     * @throws Exception on errors
     */
    @Test public void testHashCode() throws Exception
    {
        AttributeType at1 = TestServerEntryUtils.getCaseIgnoringAttributeNoNumbersType();
        ServerStringValue v0 = new ServerStringValue( at1, "Alex" );
        ServerStringValue v1 = new ServerStringValue( at1, "ALEX" );
        ServerStringValue v2 = new ServerStringValue( at1, "alex" );
        
        assertEquals( v0.hashCode(), v1.hashCode() );
        assertEquals( v0.hashCode(), v2.hashCode() );
        assertEquals( v1.hashCode(), v2.hashCode() );
        
        assertEquals( v0, v1 );
        assertEquals( v0, v2 );
        assertEquals( v1, v2 );
        
        assertTrue( v0.isValid() );
        assertTrue( v1.isValid() );
        assertTrue( v2.isValid() );

        ServerStringValue v3 = new ServerStringValue( at1, "Timber" );
        
        assertTrue( v3.isValid() );
        assertNotSame( v0.hashCode(), v3.hashCode() );

        ServerStringValue v4 = new ServerStringValue( at, "Alex" );
        
        assertNotSame( v0.hashCode(), v4.hashCode() );
        
    }
    
    
    /**
     * Test the compareTo method
     */
    @Test
    public void testCompareTo()
    {
        AttributeType at1 = TestServerEntryUtils.getCaseIgnoringAttributeNoNumbersType();
        ServerStringValue v0 = new ServerStringValue( at1, "Alex" );
        ServerStringValue v1 = new ServerStringValue( at1, "ALEX" );
        
        assertEquals( 0, v0.compareTo( v1 ) );
        assertEquals( 0, v1.compareTo( v0 ) );

        ServerStringValue v2 = new ServerStringValue( at1, null );
        
        assertEquals( 1, v0.compareTo( v2 ) );
        assertEquals( -1, v2.compareTo( v0 ) );
    }


    /**
     * Test the clone method
     */
    @Test
    public void testClone() throws NamingException
    {
        AttributeType at1 = TestServerEntryUtils.getCaseIgnoringAttributeNoNumbersType();
        ServerStringValue ssv = new ServerStringValue( at1, "Test" );
        
        ServerStringValue ssv1 = (ServerStringValue)ssv.clone();
        
        assertEquals( ssv, ssv1 );
        
        ssv.set( "" );
        
        assertNotSame( ssv, ssv1 );
        assertEquals( "", ssv.get() );
        
        ssv.set(  "  This is    a   TEST  " );
        ssv1 = (ServerStringValue)ssv.clone();
        
        assertEquals( ssv, ssv1 );
        
        ssv.normalize();
        
        assertNotSame( ssv, ssv1 );
    }
    


    /**
     * Presumes an attribute which constrains it's values to some constant
     * strings: LOW, MEDIUM, HIGH.  Normalization does nothing. MatchingRules
     * are exact case matching.
     *
     * @throws Exception on errors
     */
    @Test public void testConstrainedString() throws Exception
    {
        s.setSyntaxChecker( new SyntaxChecker() {
            public String getSyntaxOid() { return "1.1.1.1"; }
            public boolean isValidSyntax( Object value )
            {
                if ( value instanceof String )
                {
                    String strval = ( String ) value;
                    return strval.equals( "HIGH" ) || strval.equals( "LOW" ) || strval.equals( "MEDIUM" );
                }
                return false;
            }
            public void assertSyntax( Object value ) throws NamingException
            { if ( ! isValidSyntax( value ) ) throw new InvalidAttributeValueException(); }
        });

        mr.syntax = s;
        mr.comparator = new Comparator<String>()
        {
            public int compare( String o1, String o2 )
            {
                if ( o1 == null )
                {
                    if ( o2 == null )
                    {
                        return 0;
                    }
                    else
                    {
                        return -1;
                    }
                }
                else if ( o2 == null )
                {
                    return 1;
                }

                int i1 = getValue( o1 );
                int i2 = getValue( o2 );

                if ( i1 == i2 ) return 0;
                if ( i1 > i2 ) return 1;
                if ( i1 < i2 ) return -1;

                throw new IllegalStateException( "should not get here at all" );
            }

            int getValue( String val )
            {
                if ( val.equals( "LOW" ) ) return 0;
                if ( val.equals( "MEDIUM" ) ) return 1;
                if ( val.equals( "HIGH" ) ) return 2;
                throw new IllegalArgumentException( "Not a valid value" );
            }
        };
        mr.normalizer = new NoOpNormalizer();
        at.setEquality( mr );
        at.setSyntax( s );

        // check that normalization and syntax checks work as expected
        ServerStringValue value = new ServerStringValue( at, "HIGH" );
        assertEquals( value.get(), value.get() );
        assertTrue( value.isValid() );
        value = new ServerStringValue( at, "high" );
        assertFalse( value.isValid() );

        // create a bunch to best tested for equals and in containers
        ServerStringValue v0 = new ServerStringValue( at, "LOW" );
        assertTrue( v0.isValid() );
        ServerStringValue v1 = new ServerStringValue( at, "LOW" );
        assertTrue( v1.isValid() );
        ServerStringValue v2 = new ServerStringValue( at, "MEDIUM" );
        assertTrue( v2.isValid() );
        ServerStringValue v3 = new ServerStringValue( at, "HIGH" );
        assertTrue( v3.isValid() );
        ServerStringValue v4 = new ServerStringValue( at );
        assertFalse( v4.isValid() );
        ServerStringValue v5 = new ServerStringValue( at );
        assertFalse( v5.isValid() );

        // check equals
        assertTrue( v0.equals( v1 ) );
        assertTrue( v1.equals( v0 ) );
        assertEquals( 0, v0.compareTo( v1 ) );

        assertTrue( v4.equals( v5 ) );
        assertTrue( v5.equals( v4 ) );
        assertEquals( 0, v4.compareTo( v5 ) );

        assertFalse( v2.equals( v3 ) );
        assertFalse( v3.equals( v2 ) );
        assertTrue( v2.compareTo( v3 ) < 0 );
        assertTrue( v3.compareTo( v2 ) > 0 );

        // add all except v1 and v5 to a set
        HashSet<ServerStringValue> set = new HashSet<ServerStringValue>();
        set.add( v0 );
        set.add( v2 );
        set.add( v3 );
        set.add( v4 );

        // check contains method
        assertTrue( "since v1.equals( v0 ) and v0 was added then this should be true", set.contains( v1 ) );
        assertTrue( "since v4.equals( v5 ) and v4 was added then this should be true", set.contains( v5 ) );

        // check ordering based on the comparator
        List<Value<String>> list = new ArrayList<Value<String>>();
        list.add( v1 );
        list.add( v3 );
        list.add( v5 );
        list.add( v0 );
        list.add( v2 );
        list.add( v4 );

        Collections.sort( list );

        // null ones are at first 2 indices
        assertTrue( "since v4 equals v5 and has no value either could be at index 0 & 1", list.get( 0 ).equals( v4 ) );
        assertTrue( "since v4 equals v5 and has no value either could be at index 0 & 1", list.get( 0 ).equals( v5 ) );
        assertTrue( "since v4 equals v5 and has no value either could be at index 0 & 1", list.get( 1 ).equals( v4 ) );
        assertTrue( "since v4 equals v5 and has no value either could be at index 0 & 1", list.get( 1 ).equals( v5 ) );

        // low ones are at the 3rd and 4th indices
        assertTrue( "since v0 equals v1 either could be at index 2 & 3", list.get( 2 ).equals( v0 ) );
        assertTrue( "since v0 equals v1 either could be at index 2 & 3", list.get( 2 ).equals( v1 ) );
        assertTrue( "since v0 equals v1 either could be at index 2 & 3", list.get( 3 ).equals( v0 ) );
        assertTrue( "since v0 equals v1 either could be at index 2 & 3", list.get( 3 ).equals( v1 ) );

        // medium then high next
        assertTrue( "since v2 \"MEDIUM\" should be at index 4", list.get( 4 ).equals( v2 ) );
        assertTrue( "since v3 \"HIGH\" should be at index 5", list.get( 5 ).equals( v3 ) );

        assertEquals( 6, list.size() );
    }


    /**
     * Creates a string value with an attribute type that is of a syntax
     * which accepts anything.  Also there is no normalization since the
     * value is the same as the normalized value.  This makes the at technically
     * a binary value however it can be dealt with as a string so this test
     * is still OK.
     * @throws Exception on errors
     */
    @Test public void testAcceptAllNoNormalization() throws Exception
    {
        // check that normalization and syntax checks work as expected
        ServerStringValue value = new ServerStringValue( at, "hello" );
        assertEquals( value.get(), value.get() );
        assertTrue( value.isValid() );

        // create a bunch to best tested for equals and in containers
        ServerStringValue v0 = new ServerStringValue( at, "hello" );
        ServerStringValue v1 = new ServerStringValue( at, "hello" );
        ServerStringValue v2 = new ServerStringValue( at, "next0" );
        ServerStringValue v3 = new ServerStringValue( at, "next1" );
        ServerStringValue v4 = new ServerStringValue( at );
        ServerStringValue v5 = new ServerStringValue( at );

        // check equals
        assertTrue( v0.equals( v1 ) );
        assertTrue( v1.equals( v0 ) );
        assertTrue( v4.equals( v5 ) );
        assertTrue( v5.equals( v4 ) );
        assertFalse( v2.equals( v3 ) );
        assertFalse( v3.equals( v2 ) );

        // add all except v1 and v5 to a set
        HashSet<ServerStringValue> set = new HashSet<ServerStringValue>();
        set.add( v0 );
        set.add( v2 );
        set.add( v3 );
        set.add( v4 );

        // check contains method
        assertTrue( "since v1.equals( v0 ) and v0 was added then this should be true", set.contains( v1 ) );
        assertTrue( "since v4.equals( v5 ) and v4 was added then this should be true", set.contains( v5 ) );

        // check ordering based on the comparator
        ArrayList<ServerStringValue> list = new ArrayList<ServerStringValue>();
        list.add( v1 );
        list.add( v3 );
        list.add( v5 );
        list.add( v0 );
        list.add( v2 );
        list.add( v4 );

        Comparator<ServerStringValue> c = new Comparator<ServerStringValue>()
        {
            public int compare( ServerStringValue o1, ServerStringValue o2 )
            {
                String n1 = null;
                String n2 = null;
                
                if ( o1 != null )
                {
                    n1 = o1.get();
                }

                if ( o2 != null )
                {
                    n2 = o2.get();
                }

                if ( n1 == null )
                {
                    return ( n2 == null ) ? 0 : -1;
                }
                else if ( n2 == null )
                {
                    return 1;
                }

                try
                {
                    return mr.getComparator().compare( n1, n2 );
                }
                catch ( Exception e )
                {
                    throw new IllegalStateException( "Normalization and comparison should succeed!", e );
                }
            }
        };

        Collections.sort( list, c );

        assertTrue( "since v4 equals v5 and has no value either could be at index 0 & 1", list.get( 0 ).equals( v4 ) );
        assertTrue( "since v4 equals v5 and has no value either could be at index 0 & 1", list.get( 0 ).equals( v5 ) );
        assertTrue( "since v4 equals v5 and has no value either could be at index 0 & 1", list.get( 1 ).equals( v4 ) );
        assertTrue( "since v4 equals v5 and has no value either could be at index 0 & 1", list.get( 1 ).equals( v5 ) );

        assertTrue( "since v0 equals v1 either could be at index 2 & 3", list.get( 2 ).equals( v0 ) );
        assertTrue( "since v0 equals v1 either could be at index 2 & 3", list.get( 2 ).equals( v1 ) );
        assertTrue( "since v0 equals v1 either could be at index 2 & 3", list.get( 3 ).equals( v0 ) );
        assertTrue( "since v0 equals v1 either could be at index 2 & 3", list.get( 3 ).equals( v1 ) );

        assertTrue( "since v2 \"next0\" should be at index 4", list.get( 4 ).equals( v2 ) );
        assertTrue( "since v3 \"next1\" should be at index 5", list.get( 5 ).equals( v3 ) );

        assertEquals( 6, list.size() );
    }

    
    /**
     * Test serialization of a StringValue which has a normalized value
     */
    @Test public void testNormalizedStringValueSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        // First check with a value which will be normalized
        ServerStringValue sv = new ServerStringValue( at, "  Test   Test  " );
        
        sv.normalize();
        String normalized = sv.getNormalizedValue();
        
        assertEquals( "test test", normalized );
        assertEquals( "  Test   Test  ", sv.get() );
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );
        
        sv.writeExternal( out );
        
        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );
        
        ServerStringValue sv2 = new ServerStringValue( at );
        sv2.readExternal( in );
        
        assertEquals( sv, sv2 );
   }


    /**
     * Test serialization of a StringValue which does not have a normalized value
     */
    @Test public void testNoNormalizedStringValueSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        // First check with a value which will be normalized
        ServerStringValue sv = new ServerStringValue( at, "test" );
        
        sv.normalize();
        String normalized = sv.getNormalizedValue();
        
        assertEquals( "test", normalized );
        assertEquals( "test", sv.get() );
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );
        
        sv.writeExternal( out );
        
        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );
        
        ServerStringValue sv2 = new ServerStringValue( at );
        sv2.readExternal( in );
        
        assertEquals( sv, sv2 );
   }


    /**
     * Test serialization of a null StringValue
     */
    @Test public void testNullStringValueSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        // First check with a value which will be normalized
        ServerStringValue sv = new ServerStringValue( at );
        
        sv.normalize();
        String normalized = sv.getNormalizedValue();
        
        assertEquals( null, normalized );
        assertEquals( null, sv.get() );
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );
        
        sv.writeExternal( out );
        
        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );
        
        ServerStringValue sv2 = new ServerStringValue( at );
        sv2.readExternal( in );
        
        assertEquals( sv, sv2 );
   }


    /**
     * Test serialization of an empty StringValue
     */
    @Test public void testEmptyStringValueSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        // First check with a value which will be normalized
        ServerStringValue sv = new ServerStringValue( at, "" );
        
        sv.normalize();
        String normalized = sv.getNormalizedValue();
        
        assertEquals( "", normalized );
        assertEquals( "", sv.get() );
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );
        
        sv.writeExternal( out );
        
        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );
        
        ServerStringValue sv2 = new ServerStringValue( at );
        sv2.readExternal( in );
        
        assertEquals( sv, sv2 );
   }
}