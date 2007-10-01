/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.shared.ldap.common;


import java.util.Arrays;
import java.util.Iterator;

import javax.naming.NamingException;

import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;


/**
 * TODO ServerAttributeImplTest.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ServerAttributeImplTest
{
    
    /**
     * Test that we can't create an attribute with a null OID or ID
     */
    @Test public void testNullAttribute()
    {
        try
        {
            new ServerAttributeImpl( (OID)null );
            fail();
        } catch ( NamingException ne ) {}

        try
        {
            new ServerAttributeImpl( (OID)null, "test" );
            fail();
        } catch ( NamingException ne ) {}

        try
        {
            new ServerAttributeImpl( (OID)null, StringTools.EMPTY_BYTES );
            fail();
        } catch ( NamingException ne ) {}

        try
        {
            new ServerAttributeImpl( (OID)null, new StringValue() );
            fail();
        } catch ( NamingException ne ) {}

        try
        {
            new ServerAttributeImpl( (String)null );
            fail();
        } catch ( NamingException ne ) {}

        try
        {
            new ServerAttributeImpl( (String)null, "test" );
            fail();
        } catch ( NamingException ne ) {}

        try
        {
            new ServerAttributeImpl( (String)null, StringTools.EMPTY_BYTES );
            fail();
        } catch ( NamingException ne ) {}

        try 
        { 
            new ServerAttributeImpl( (String)null, new StringValue() );
            fail();
        } catch ( NamingException ne ) {}
    }
    
    /**
     * Test a String attribute
     */
    @Test public void testStringAttr() throws NamingException
    {
        ServerAttribute attr = new ServerAttributeImpl( "test", "test1" );
        
        attr.add( "test2" );
        
        assertEquals( 2, attr.size() );
        assertEquals( "test1", attr.get().getValue() );
        
        Iterator<Value> vals = attr.getAll();
        int i = 1;
        
        while ( vals.hasNext() )
        {
            assertEquals( "test" + i, vals.next().getValue() );
            i++;
        }
    }

    /**
     * Test a Binary attribute
     */
    @Test public void testBinaryAttr() throws NamingException
    {
        byte[] b1 = StringTools.getBytesUtf8( "test1" );
        byte[] b2 = StringTools.getBytesUtf8( "test2" );
        
        ServerAttribute attr = new ServerAttributeImpl( "test", b1 );
        
        attr.add( b2 );
        
        assertEquals( 2, attr.size() );
        assertTrue( Arrays.equals( b1, (byte[])attr.get().getValue() ) );
        
        Iterator<Value> vals = attr.getAll();
        int i = 1;
        
        while ( vals.hasNext() )
        {
            b1[4] = (byte)(i + '0');
            assertTrue( Arrays.equals( b1, (byte[])vals.next().getValue() ) );
            i++;
        }
    }

    /**
     * Test a String attribute with two values
     */
    @Test public void testStringAttrTwice() throws NamingException
    {
        ServerAttribute attr = new ServerAttributeImpl( "test", "test1" );
        
        attr.add( "test1" );
        
        assertEquals( 1, attr.size() );
        assertEquals( "test1", attr.get().getValue() );
    }

    /**
     * Test a Binary attribute with 2 values
     */
    @Test public void testBinaryAttrTwice() throws NamingException
    {
        byte[] b1 = StringTools.getBytesUtf8( "test1" );
        byte[] b2 = StringTools.getBytesUtf8( "test1" );
        
        ServerAttribute attr = new ServerAttributeImpl( "test", b1 );
        
        attr.add( b2 );
        
        assertEquals( 1, attr.size() );
        assertTrue( Arrays.equals( b1, (byte[])attr.get().getValue() ) );
    }
    
    /**
     * Test an attribute with a null value
     */
    @Test public void testAttributeWithNullValue() throws NamingException
    {
        ServerAttribute attr = new ServerAttributeImpl( "test", (String)null );
        
        assertNull( attr.get().getValue() );
    }

    /**
     * Test a String attribute with a value and a null
     */
    @Test public void testAttributeWithNullAndStringValues() throws NamingException
    {
        ServerAttribute attr = new ServerAttributeImpl( "test", (String)null );
        attr.add( "test" );
        
        assertEquals( 2, attr.size() );
        
        String[] expected = new String[]{ null, "test" }; 
        
        for ( String v:expected )
        {
            assertTrue( attr.contains( v ) );
        }

        ServerAttribute attr2 = new ServerAttributeImpl( "test", "test" );
        attr2.add( (String)null );
        
        assertEquals( 2, attr2.size() );
        
        for ( String v:expected )
        {
            assertTrue( attr2.contains( v ) );
        }
    }

    /**
     * Test an attribute with mixed values
     */
    @Test public void testAttributeWithMixedValues() throws NamingException
    {
        ServerAttribute attr = new ServerAttributeImpl( "test", (String)null );
        attr.add( "test" );
        byte[] b1 = StringTools.getBytesUtf8( "test1" );
        attr.add( b1 );
        
        assertEquals( 2, attr.size() );
        
        String[] expected = new String[]{ null, "test" }; 
        
        for ( String v:expected )
        {
            assertTrue( attr.contains( v ) );
        }

        ServerAttribute attr2 = new ServerAttributeImpl( "test", (byte[])null );
        attr2.add( b1 );
        attr2.add( "test" );
        
        assertEquals( 2, attr2.size() );
        
        byte[][] expected2 = new byte[][]{ null, b1 }; 
        
        for ( byte[] v:expected2 )
        {
            assertTrue( attr2.contains( v ) );
        }
    }
    
    
    /**
     * Test the clear method
     */
    @Test public void testClear() throws NamingException
    {
        ServerAttribute attr1 = new ServerAttributeImpl( "test", "test" );
        
        attr1.clear();
        assertEquals( 0, attr1.size() );
        assertNull( attr1.get() );
        
        ServerAttribute attr2 = new ServerAttributeImpl( "test", "test" );
        attr2.add( "test2" );

        attr2.clear();
        assertEquals( 0, attr1.size() );
        assertNull( attr1.get() );
        
    }
    

    /**
     * Test the get method
     */
    @Test public void testGet() throws NamingException
    {
        ServerAttribute attr1 = new ServerAttributeImpl( "test", "test1" );
        
        Value v = attr1.get();
        assertEquals( "test1", v.getValue() );

        attr1.add( "test2" );

        v = attr1.get();
        assertEquals( "test1", v.getValue() );
        
        attr1.remove( new StringValue( "test1" ) );
        
        v = attr1.get();
        assertEquals( "test2", v.getValue() );
    }


    /**
     * Test the getId
     */
    @Test public void testGetID() throws NamingException
    {
        ServerAttribute attr = new ServerAttributeImpl( "test", "test1" );

        assertEquals( "test", attr.getID() );
    }


    /**
     * Test the getOid
     */
    @Test public void testGetOid() throws NamingException, DecoderException
    {
        OID oid = new OID( "0.1.2.3.4" );
        
        ServerAttribute attr = new ServerAttributeImpl( new OID( "0.1.2.3.4" ), "test1" );

        assertEquals( oid, attr.getOid() );
    }
    
    
    /**
     * Test the normalize method with null value
     */
    @Test public void testNormalizerNullValue() throws NamingException, DecoderException
    {
        OID oid = new OID( "0.1.2.3.4" );
        
        ServerAttribute attr = new ServerAttributeImpl( "test", (String)null );

        attr.normalize( oid, new Normalizer<String>()
            {
                public static final long serialVersionUID = 1L;
                
                public String normalize( String value )
                {
                    return value == null ? null : StringTools.toLowerCase( value );
                }
            });
        
        assertEquals( null, attr.get().getNormValue() );
        assertEquals( null, attr.get().getValue() );
        assertEquals( "test", attr.getID() );
        assertEquals( oid, attr.getOid() );
    }
    
    
    /**
     * Test the normalize method
     */
    @Test public void testNormalizerMultipleValue() throws NamingException, DecoderException
    {
        OID oid = new OID( "0.1.2.3.4" );
        
        ServerAttribute attr = new ServerAttributeImpl( "test", "TEST1" );
        attr.add( "TEST2" );
        attr.add( (String)null );

        attr.normalize( oid, new Normalizer<String>()
            {
                public static final long serialVersionUID = 1L;
                
                public String normalize( String value )
                {
                    return value == null ? null : StringTools.toLowerCase( value );
                }
            });
        
        String[] expectedLC = new String[]{ "test1", "test2", null };
        String[] expectedUC = new String[]{ "TEST1", "TEST2", null };
        
        Iterator<Value> iter = attr.getAll();
        int i = 0;
        
        while ( iter.hasNext() )
        {
            Value v = iter.next();
            assertEquals( expectedLC[i], v.getNormValue() );
            assertEquals( expectedUC[i], v.getValue() );
            
            i++;
        }
        
        assertEquals( "test1", attr.get().getNormValue() );
        assertEquals( "TEST1", attr.get().getValue() );
        assertEquals( "test", attr.getID() );
        assertEquals( oid, attr.getOid() );
    }


    /**
     * Test the remove method
     */
    @Test public void testRemove() throws NamingException
    {
        ServerAttribute attr = new ServerAttributeImpl( "test", "TEST1" );
        attr.add( "TEST2" );
        attr.add( (String)null );
        
        assertTrue( attr.remove( (String)null ) );
        assertEquals( 2, attr.size() );
        
        assertFalse( attr.remove( "Not existent" ) );
        
        assertTrue( attr.remove( "TEST1" ) );
        assertEquals( 1, attr.size() );
        
        assertFalse( attr.remove( "TEST1" ) );
        
        assertTrue( attr.remove( new StringValue( "TEST2" ) ) );
        assertEquals( 0, attr.size() );
    }


    /**
     * Test the contains method
     */
    @Test public void testContains() throws NamingException
    {
        ServerAttribute attr = new ServerAttributeImpl( "string", "test1" );
        attr.add(  "test2" );
        attr.add( (String)null );
        
        assertTrue( attr.contains( "test1" ) );
        assertTrue( attr.contains( "test2" ) );
        assertTrue( attr.contains( (String)null ) );
        assertTrue( attr.contains( new StringValue( "test2" ) ) );
        assertFalse( attr.contains( "test3" ) );
        
        
        byte[] b1 = StringTools.getBytesUtf8( "test1" );
        byte[] b2 = StringTools.getBytesUtf8( "test2" );
        
        ServerAttribute attr2 = new ServerAttributeImpl( "binary", b1 );
        attr2.add( b2 ); 
        attr2.add( (byte[])null );

        assertTrue( attr2.contains( (byte[])null ) );
        assertTrue( attr2.contains( b1 ) );
        assertTrue( attr2.contains( b2 ) );
        assertTrue( attr2.contains( new BinaryValue( b1 ) ) );
        assertFalse( attr2.contains( StringTools.getBytesUtf8( "test3" ) ) );
    }
    
    /**
     * Test the clone method
     */
    @Test public void testClone() throws NamingException, DecoderException
    {
        ServerAttribute attr = new ServerAttributeImpl( "string", "TEST1" );
        attr.add(  "TEST2" );
        attr.add( (String)null );
        
        ServerAttribute clone = (ServerAttribute)attr.clone();
        
        assertEquals( clone, attr );
        
        OID oid = new OID( "0.1.2.3.4" );

        attr.normalize( oid, new Normalizer<String>()
            {
                public static final long serialVersionUID = 1L;
                
                public String normalize( String value )
                {
                    return value == null ? null : StringTools.toLowerCase( value );
                }
            });

        assertNotSame( clone, attr );
        
        clone.normalize( oid, new Normalizer<String>()
            {
                public static final long serialVersionUID = 1L;
                
                public String normalize( String value )
                {
                    return value == null ? null : StringTools.toLowerCase( value );
                }
            });
        
        assertEquals( clone, attr );
        
        attr.clear();
        
        assertNotSame( clone, attr );
        assertEquals( 3, clone.size() );
        Value v = clone.get();
        assertEquals( "TEST1", v.getValue() );
        assertEquals( "test1", v.getNormValue() );
        assertEquals( oid, clone.getOid() );
    }


    /**
     * Test the getAll method
     */
    @Test public void testGetAll() throws NamingException
    {
        ServerAttribute attr = new ServerAttributeImpl( "string", "test1" );
        attr.add(  "test2" );
        attr.add( (String)null );
        
        Iterator<Value> iter = attr.getAll();
        assertNotNull( iter );
        
        int i = 0;
        
        while ( iter.hasNext() )
        {
            Value v = iter.next();
            
            assertNotNull( v );
            i++;
        }
        
        assertEquals( i, attr.size() );
    }
}
