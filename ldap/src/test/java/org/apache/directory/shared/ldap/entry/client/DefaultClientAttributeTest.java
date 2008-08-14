/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.shared.ldap.entry.client;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.InvalidAttributeValueException;

import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.schema.syntax.Ia5StringSyntaxChecker;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test the DefaultClientAttribute class
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DefaultClientAttributeTest
{
    private static final Value<String> NULL_STRING_VALUE = new ClientStringValue( null );
    private static final Value<byte[]> NULL_BINARY_VALUE = new ClientBinaryValue( null );
    private static final byte[] BYTES1 = new byte[]{ 'a', 'b' };
    private static final byte[] BYTES2 = new byte[]{ 'b' };
    private static final byte[] BYTES3 = new byte[]{ 'c' };
    private static final byte[] BYTES4 = new byte[]{ 'd' };
    
    private static final ClientStringValue STR_VALUE1 = new ClientStringValue( "a" );
    private static final ClientStringValue STR_VALUE2 = new ClientStringValue( "b" );
    private static final ClientStringValue STR_VALUE3 = new ClientStringValue( "c" );
    private static final ClientStringValue STR_VALUE4 = new ClientStringValue( "d" );

    private static final ClientBinaryValue BIN_VALUE1 = new ClientBinaryValue( BYTES1 );
    private static final ClientBinaryValue BIN_VALUE2 = new ClientBinaryValue( BYTES2 );
    private static final ClientBinaryValue BIN_VALUE3 = new ClientBinaryValue( BYTES3 );
    private static final ClientBinaryValue BIN_VALUE4 = new ClientBinaryValue( BYTES4 );

    
    
    /**
     * Serialize a DefaultClientAttribute
     */
    private ByteArrayOutputStream serializeValue( DefaultClientAttribute value ) throws IOException
    {
        ObjectOutputStream oOut = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try
        {
            oOut = new ObjectOutputStream( out );
            oOut.writeObject( value );
        }
        catch ( IOException ioe )
        {
            throw ioe;
        }
        finally
        {
            try
            {
                if ( oOut != null )
                {
                    oOut.flush();
                    oOut.close();
                }
            }
            catch ( IOException ioe )
            {
                throw ioe;
            }
        }
        
        return out;
    }
    
    
    /**
     * Deserialize a DefaultClientAttribute
     */
    private DefaultClientAttribute deserializeValue( ByteArrayOutputStream out ) throws IOException, ClassNotFoundException
    {
        ObjectInputStream oIn = null;
        ByteArrayInputStream in = new ByteArrayInputStream( out.toByteArray() );

        try
        {
            oIn = new ObjectInputStream( in );

            DefaultClientAttribute value = ( DefaultClientAttribute ) oIn.readObject();

            return value;
        }
        catch ( IOException ioe )
        {
            throw ioe;
        }
        finally
        {
            try
            {
                if ( oIn != null )
                {
                    oIn.close();
                }
            }
            catch ( IOException ioe )
            {
                throw ioe;
            }
        }
    }

    
    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception
    {
    }


    /**
     * Test method new DefaultClientAttribute()
     */
    @Test
    public void testDefaultClientAttribute()
    {
        EntryAttribute attr = new DefaultClientAttribute();
        
        assertFalse( attr.isHR() );
        assertEquals( 0, attr.size() );
        assertNull( attr.getId() );
        assertNull( attr.getUpId() );
    }


    /**
     * Test method new DefaultClientAttribute( String )
     */
    @Test
    public void testDefaultClientAttributeString()
    {
        EntryAttribute attr = new DefaultClientAttribute( "TEST" );
        
        assertFalse( attr.isHR() );
        assertEquals( 0, attr.size() );
        assertEquals( "test", attr.getId() );
        assertEquals( "TEST", attr.getUpId() );
    }


    /**
     * Test method new DefaultClientAttribute( String, Value... )
     */
    @Test
    public void testDefaultClientAttributeStringValueArray()
    {
        EntryAttribute attr = new DefaultClientAttribute( "Test", STR_VALUE1, STR_VALUE2 );
        
        assertTrue( attr.isHR() );
        assertEquals( 2, attr.size() );
        assertTrue( attr.contains( "a" ) );
        assertTrue( attr.contains( "b" ) );
        assertEquals( "test", attr.getId() );
        assertEquals( "Test", attr.getUpId() );
    }


    /**
     * Test method 
     */
    @Test
    public void testDefaultClientAttributeStringStringArray()
    {
        EntryAttribute attr = new DefaultClientAttribute( "Test", "a", "b" );
        
        assertTrue( attr.isHR() );
        assertEquals( 2, attr.size() );
        assertTrue( attr.contains( "a" ) );
        assertTrue( attr.contains( "b" ) );
        assertEquals( "test", attr.getId() );
        assertEquals( "Test", attr.getUpId() );
    }


    /**
     * Test method 
     */
    @Test
    public void testDefaultClientAttributeStringBytesArray()
    {
        EntryAttribute attr = new DefaultClientAttribute( "Test", BYTES1, BYTES2 );
        
        assertFalse( attr.isHR() );
        assertEquals( 2, attr.size() );
        assertTrue( attr.contains( BYTES1 ) );
        assertTrue( attr.contains( BYTES2 ) );
        assertEquals( "test", attr.getId() );
        assertEquals( "Test", attr.getUpId() );
    }


    /**
     * Test method getBytes()
     */
    @Test
    public void testGetBytes() throws InvalidAttributeValueException, NamingException
    {
        EntryAttribute attr1 = new DefaultClientAttribute( "test" );
        
        attr1.add( (byte[])null );
        assertNull( attr1.getBytes() );

        EntryAttribute attr2 = new DefaultClientAttribute( "test" );
        
        attr2.add( BYTES1, BYTES2 );
        assertTrue( Arrays.equals( BYTES1, attr2.getBytes() ) );
        
        EntryAttribute attr3 = new DefaultClientAttribute( "test" );
        
        attr3.add( "a", "b" );
        
        try
        {
            attr3.getBytes();
            fail();
        }
        catch ( InvalidAttributeValueException ivae )
        {
            assertTrue( true );
        }
    }


    /**
     * Test method getString()
     */
    @Test
    public void testGetString() throws InvalidAttributeValueException, NamingException
    {
        EntryAttribute attr1 = new DefaultClientAttribute( "test" );
        
        attr1.add( (String)null );
        assertNull( attr1.getString() );

        EntryAttribute attr2 = new DefaultClientAttribute( "test" );
        
        attr2.add( "a", "b" );
        assertEquals( "a", attr2.getString() );
        
        EntryAttribute attr3 = new DefaultClientAttribute( "test" );
        
        attr3.add( BYTES1, BYTES2 );
        
        try
        {
            attr3.getString();
            fail();
        }
        catch ( InvalidAttributeValueException ivae )
        {
            assertTrue( true );
        }
    }


    /**
     * Test method getId()
     */
    @Test
    public void testGetId()
    {
        EntryAttribute attr = new DefaultClientAttribute();

        assertNull( attr.getId() );
        
        attr.setId( "test" );
        assertEquals( "test", attr.getId() );
        
        attr.setId(  "  TEST  " );
        assertEquals( "test", attr.getId() );
    }


    /**
     * Test method SetId(String)
     */
    @Test
    public void testSetId()
    {
        EntryAttribute attr = new DefaultClientAttribute();

        try
        {
            attr.setId( null );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }
        
        try
        {
            attr.setId( "" );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }
        
        try
        {
            attr.setId( "  " );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }
        
        attr.setId( "Test" );
        assertEquals( "test", attr.getId() );
        
        attr.setId( " Test " );
        assertEquals( "test", attr.getId() );
    }


    /**
     * Test method getUpId()
     */
    @Test
    public void testGetUpId()
    {
        EntryAttribute attr = new DefaultClientAttribute();

        assertNull( attr.getUpId() );
        
        attr.setUpId( "test" );
        assertEquals( "test", attr.getUpId() );
        
        attr.setUpId(  "  TEST  " );
        assertEquals( "TEST", attr.getUpId() );
    }


    /**
     * Test method setUpId(String)
     */
    @Test
    public void testSetUpId()
    {
        EntryAttribute attr = new DefaultClientAttribute();

        try
        {
            attr.setUpId( null );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }
        
        try
        {
            attr.setUpId( "" );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }
        
        try
        {
            attr.setUpId( "  " );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }
        
        attr.setUpId( "Test" );
        assertEquals( "Test", attr.getUpId() );
        assertEquals( "test", attr.getId() );
        
        attr.setUpId( " Test " );
        assertEquals( "Test", attr.getUpId() );
        assertEquals( "test", attr.getId() );
    }


    /**
     * Test method isValid( SyntaxChecker ) 
     */
    @Test
    public void testIsValidSyntaxChecker() throws NamingException
    {
        ClientAttribute attr = new DefaultClientAttribute( "test" );
        
        attr.add( "test", "another test" );
        
        assertTrue( attr.isValid( new Ia5StringSyntaxChecker() ) );
        
        attr.add( "é" );
        assertFalse( attr.isValid( new Ia5StringSyntaxChecker() ) );
    }


    /**
     * Test method iterator()
     */
    @Test
    public void testIterator()
    {
        EntryAttribute attr = new DefaultClientAttribute();
        attr.add(  "a", "b", "c" );
        
        Iterator<Value<?>> iter = attr.iterator();
        
        assertTrue( iter.hasNext() );
        
        String[] values = new String[]{ "a", "b", "c" };
        int pos = 0;
        
        for ( Value<?> val:attr )
        {
            assertTrue( val instanceof ClientStringValue );
            assertEquals( values[pos++], val.get() );
        }
    }


    /**
     * Test method add(Value...)
     */
    @Test
    public void testAddValueArray() throws InvalidAttributeValueException, NamingException
    {
        EntryAttribute attr1 = new DefaultClientAttribute( "test" );
        
        int nbAdded = attr1.add( new ClientStringValue( null ) );
        assertEquals( 1, nbAdded );
        assertTrue( attr1.isHR() );
        assertEquals( NULL_STRING_VALUE, attr1.get() );
        
        EntryAttribute attr2 = new DefaultClientAttribute( "test" );
        
        nbAdded = attr2.add( new ClientBinaryValue( null ) );
        assertEquals( 1, nbAdded );
        assertFalse( attr2.isHR() );
        assertEquals( NULL_BINARY_VALUE, attr2.get() );
        
        EntryAttribute attr3 = new DefaultClientAttribute( "test" );
        
        nbAdded = attr3.add( new ClientStringValue( "a" ), new ClientStringValue( "b" ) );
        assertEquals( 2, nbAdded );
        assertTrue( attr3.isHR() );
        assertTrue( attr3.contains( "a" ) );
        assertTrue( attr3.contains( "b" ) );
        
        EntryAttribute attr4 = new DefaultClientAttribute( "test" );
        
        nbAdded = attr4.add( new ClientBinaryValue( BYTES1 ), new ClientBinaryValue( BYTES2 ) );
        assertEquals( 2, nbAdded );
        assertFalse( attr4.isHR() );
        assertTrue( attr4.contains( BYTES1 ) );
        assertTrue( attr4.contains( BYTES2 ) );
        
        EntryAttribute attr5 = new DefaultClientAttribute( "test" );
        
        nbAdded = attr5.add( new ClientStringValue( "c" ), new ClientBinaryValue( BYTES1 ) );
        assertEquals( 2, nbAdded );
        assertTrue( attr5.isHR() );
        assertTrue( attr5.contains( "ab" ) );
        assertTrue( attr5.contains( "c" ) );

        EntryAttribute attr6 = new DefaultClientAttribute( "test" );
        
        nbAdded = attr6.add( new ClientBinaryValue( BYTES1 ), new ClientStringValue( "c" ) );
        assertEquals( 2, nbAdded );
        assertFalse( attr6.isHR() );
        assertTrue( attr6.contains( BYTES1 ) );
        assertTrue( attr6.contains( BYTES3 ) );

        EntryAttribute attr7 = new DefaultClientAttribute( "test" );
        
        nbAdded = attr7.add( new ClientBinaryValue( null ), new ClientStringValue( "c" ) );
        assertEquals( 2, nbAdded );
        assertFalse( attr7.isHR() );
        assertTrue( attr7.contains( NULL_BINARY_VALUE ) );
        assertTrue( attr7.contains( BYTES3 ) );

        EntryAttribute attr8 = new DefaultClientAttribute( "test" );
        
        nbAdded = attr8.add( new ClientStringValue( null ), new ClientBinaryValue( BYTES1 ) );
        assertEquals( 2, nbAdded );
        assertTrue( attr8.isHR() );
        assertTrue( attr8.contains( NULL_STRING_VALUE ) );
        assertTrue( attr8.contains( "ab" ) );
    }


    /**
     * Test method add( String... )
     */
    @Test
    public void testAddStringArray() throws InvalidAttributeValueException, NamingException
    {
        EntryAttribute attr1 = new DefaultClientAttribute( "test" );
        
        int nbAdded = attr1.add( (String)null );
        assertEquals( 1, nbAdded );
        assertTrue( attr1.isHR() );
        assertEquals( NULL_STRING_VALUE, attr1.get() );
        
        EntryAttribute attr2 = new DefaultClientAttribute( "test" );
        
        nbAdded = attr2.add( "" );
        assertEquals( 1, nbAdded );
        assertTrue( attr2.isHR() );
        assertEquals( "", attr2.getString() );
        
        EntryAttribute attr3 = new DefaultClientAttribute( "test" );
        
        nbAdded = attr3.add( "t" );
        assertEquals( 1, nbAdded );
        assertTrue( attr3.isHR() );
        assertEquals( "t", attr3.getString() );
        
        EntryAttribute attr4 = new DefaultClientAttribute( "test" );
        
        nbAdded = attr4.add( "a", "b", "c", "d" );
        assertEquals( 4, nbAdded );
        assertTrue( attr4.isHR() );
        assertEquals( "a", attr4.getString() );
        assertTrue( attr4.contains( "a" ) );
        assertTrue( attr4.contains( "b" ) );
        assertTrue( attr4.contains( "c" ) );
        assertTrue( attr4.contains( "d" ) );
        
        nbAdded = attr4.add( "e" );
        assertEquals( 1, nbAdded );
        assertTrue( attr4.isHR() );
        assertEquals( "a", attr4.getString() );
        assertTrue( attr4.contains( "a" ) );
        assertTrue( attr4.contains( "b" ) );
        assertTrue( attr4.contains( "c" ) );
        assertTrue( attr4.contains( "d" ) );
        assertTrue( attr4.contains( "e" ) );
        
        nbAdded = attr4.add( BYTES1 );
        assertEquals( 1, nbAdded );
        assertTrue( attr4.isHR() );
        assertEquals( "a", attr4.getString() );
        assertTrue( attr4.contains( "a" ) );
        assertTrue( attr4.contains( "b" ) );
        assertTrue( attr4.contains( "c" ) );
        assertTrue( attr4.contains( "d" ) );
        assertTrue( attr4.contains( "e" ) );
        assertTrue( attr4.contains( "ab" ) );
        
        EntryAttribute attr5 = new DefaultClientAttribute( "test" );
        
        nbAdded = attr5.add( "a", "b", (String)null, "d" );
        assertEquals( 4, nbAdded );
        assertTrue( attr5.isHR() );
        assertTrue( attr5.contains( "a" ) );
        assertTrue( attr5.contains( "b" ) );
        assertTrue( attr5.contains( (String)null ) );
        assertTrue( attr5.contains( "d" ) );

        EntryAttribute attr6 = new DefaultClientAttribute( "test" );
        
        attr6.setHR( false );
        nbAdded = attr6.add( "a", (String)null );
        assertEquals( 2, nbAdded );
        assertFalse( attr6.isHR() );
        assertTrue( attr6.contains( new byte[]{'a'} ) );
        assertTrue( attr6.contains( (byte[])null ) );
        
        EntryAttribute attr7 = new DefaultClientAttribute( "test" );
        
        attr7.add( "a", "b" );
        assertEquals( 2, attr7.size() );
        
        assertEquals( 1, attr7.add( "b", "c" ) );
        assertEquals( 3, attr7.size() );
        assertTrue( attr7.contains( "a", "b", "c" ) );
    }


    /**
     * Test method add( byte[]... )
     */
    @Test
    public void testAddByteArray() throws InvalidAttributeValueException, NamingException
    {
        EntryAttribute attr1 = new DefaultClientAttribute( "test" );
        
        int nbAdded = attr1.add( (byte[])null );
        assertEquals( 1, nbAdded );
        assertFalse( attr1.isHR() );
        assertTrue( Arrays.equals( NULL_BINARY_VALUE.get(), attr1.getBytes() ) );
        
        EntryAttribute attr2 = new DefaultClientAttribute( "test" );
        
        nbAdded = attr2.add( StringTools.EMPTY_BYTES );
        assertEquals( 1, nbAdded );
        assertFalse( attr2.isHR() );
        assertTrue( Arrays.equals( StringTools.EMPTY_BYTES, attr2.getBytes() ) );
        
        EntryAttribute attr3 = new DefaultClientAttribute( "test" );
        
        nbAdded = attr3.add( BYTES1 );
        assertEquals( 1, nbAdded );
        assertFalse( attr3.isHR() );
        assertTrue( Arrays.equals( BYTES1, attr3.getBytes() ) );
        
        EntryAttribute attr4 = new DefaultClientAttribute( "test" );
        
        nbAdded = attr4.add( BYTES1, BYTES2, BYTES3, BYTES4 );
        assertEquals( 4, nbAdded );
        assertFalse( attr4.isHR() );
        assertTrue( attr4.contains( BYTES1 ) );
        assertTrue( attr4.contains( BYTES2 ) );
        assertTrue( attr4.contains( BYTES3 ) );
        assertTrue( attr4.contains( BYTES4 ) );
        
        EntryAttribute attr5 = new DefaultClientAttribute( "test" );
        
        nbAdded = attr5.add( BYTES1, BYTES2, (byte[])null, BYTES3 );
        assertEquals( 4, nbAdded );
        assertFalse( attr5.isHR() );
        assertTrue( attr5.contains( BYTES1 ) );
        assertTrue( attr5.contains( BYTES2 ) );
        assertTrue( attr5.contains( (byte[])null ) );
        assertTrue( attr5.contains( BYTES3 ) );

        EntryAttribute attr6 = new DefaultClientAttribute( "test" );
        
        attr6.setHR( true );
        nbAdded = attr6.add( BYTES1, (byte[])null );
        assertEquals( 2, nbAdded );
        assertTrue( attr6.isHR() );
        assertTrue( attr6.contains( "ab" ) );
        assertTrue( attr6.contains( (String)null ) );
    }


    /**
     * Test method clear()
     */
    @Test
    public void testClear() throws InvalidAttributeValueException, NamingException
    {
        EntryAttribute attr1 = new DefaultClientAttribute( "test" );
        
        assertEquals( 0, attr1.size() );
        
        attr1.add( (String)null );
        assertEquals( 1, attr1.size() );
        assertTrue( attr1.isHR() );
        attr1.clear();
        assertTrue( attr1.isHR() );
        assertEquals( 0, attr1.size() );

        EntryAttribute attr2 = new DefaultClientAttribute( "test" );
        attr2.add( BYTES1, BYTES2 );
        assertEquals( 2, attr2.size() );
        assertFalse( attr2.isHR() );
        attr2.clear();
        assertFalse( attr2.isHR() );
        assertEquals( 0, attr2.size() );
    }


    /**
     * Test method contains( Value... )
     */
    @Test
    public void testContainsValueArray() throws InvalidAttributeValueException, NamingException
    {
        EntryAttribute attr1 = new DefaultClientAttribute( "test" );
        
        assertEquals( 0, attr1.size() );
        assertFalse( attr1.contains( STR_VALUE1 ) );
        assertFalse( attr1.contains( NULL_STRING_VALUE ) );
        
        attr1.add( (String)null );
        assertEquals( 1, attr1.size() );
        assertTrue( attr1.contains( NULL_STRING_VALUE ) );
        
        attr1.remove( (String)null );
        assertFalse( attr1.contains( NULL_STRING_VALUE ) );
        assertEquals( 0, attr1.size() );
        
        attr1.add(  "a", "b", "c" );
        assertEquals( 3, attr1.size() );
        assertTrue( attr1.contains( STR_VALUE1 ) );
        assertTrue( attr1.contains( STR_VALUE2 ) );
        assertTrue( attr1.contains( STR_VALUE3 ) );
        assertTrue( attr1.contains( STR_VALUE1, STR_VALUE3 ) );
        assertFalse( attr1.contains( STR_VALUE4 ) );
        assertFalse( attr1.contains( NULL_STRING_VALUE ) );
        assertTrue( attr1.contains( STR_VALUE1, BIN_VALUE2 ) );

        EntryAttribute attr2 = new DefaultClientAttribute( "test" );
        assertEquals( 0, attr2.size() );
        assertFalse( attr2.contains( BYTES1 ) );
        assertFalse( attr2.contains( NULL_BINARY_VALUE ) );
        
        attr2.add( (byte[])null );
        assertEquals( 1, attr2.size() );
        assertTrue( attr2.contains( NULL_BINARY_VALUE ) );
        
        attr2.remove( (byte[])null );
        assertFalse( attr2.contains( NULL_BINARY_VALUE ) );
        assertEquals( 0, attr2.size() );
        
        attr2.add( BYTES1, BYTES2, BYTES3 );
        assertEquals( 3, attr2.size() );
        assertTrue( attr2.contains( BIN_VALUE1 ) );
        assertTrue( attr2.contains( BIN_VALUE2 ) );
        assertTrue( attr2.contains( BIN_VALUE3 ) );
        assertFalse( attr2.contains( NULL_BINARY_VALUE ) );
        assertTrue( attr2.contains( STR_VALUE2, BIN_VALUE1 ) );
    }


    /**
     * Test method contains( String... )
     */
    @Test
    public void testContainsStringArray() throws InvalidAttributeValueException, NamingException
    {
        EntryAttribute attr1 = new DefaultClientAttribute( "test" );
        
        assertEquals( 0, attr1.size() );
        assertFalse( attr1.contains( "a" ) );
        assertFalse( attr1.contains( (String)null ) );
        
        attr1.add( (String)null );
        assertEquals( 1, attr1.size() );
        assertTrue( attr1.contains( (String)null ) );
        
        attr1.remove( (String)null );
        assertFalse( attr1.contains( (String)null ) );
        assertEquals( 0, attr1.size() );
        
        attr1.add(  "a", "b", "c" );
        assertEquals( 3, attr1.size() );
        assertTrue( attr1.contains( "a" ) );
        assertTrue( attr1.contains( "b" ) );
        assertTrue( attr1.contains( "c" ) );
        assertFalse( attr1.contains( "e" ) );
        assertFalse( attr1.contains( (String)null ) );

        EntryAttribute attr2 = new DefaultClientAttribute( "test" );
        assertEquals( 0, attr2.size() );
        assertFalse( attr2.contains( BYTES1 ) );
        assertFalse( attr2.contains( (byte[])null ) );
        
        attr2.add( (byte[])null );
        assertEquals( 1, attr2.size() );
        assertTrue( attr2.contains( (byte[])null ) );
        
        attr2.remove( (byte[])null );
        assertFalse( attr2.contains( (byte[])null ) );
        assertEquals( 0, attr2.size() );
        
        attr2.add( BYTES1, BYTES2, BYTES3 );
        assertEquals( 3, attr2.size() );
        assertTrue( attr2.contains( "ab" ) );
        assertTrue( attr2.contains( "b" ) );
        assertTrue( attr2.contains( "c" ) );
        assertFalse( attr2.contains( (String)null ) );
    }


    /**
     * Test method contains( byte... )
     */
    @Test
    public void testContainsByteArray() throws InvalidAttributeValueException, NamingException
    {
        EntryAttribute attr1 = new DefaultClientAttribute( "test" );
        
        assertEquals( 0, attr1.size() );
        assertFalse( attr1.contains( BYTES1 ) );
        assertFalse( attr1.contains( (byte[])null ) );
        
        attr1.add( (byte[])null );
        assertEquals( 1, attr1.size() );
        assertTrue( attr1.contains( (byte[])null ) );
        
        attr1.remove( (byte[])null );
        assertFalse( attr1.contains( (byte[])null ) );
        assertEquals( 0, attr1.size() );
        
        attr1.add(  BYTES1, BYTES2, BYTES3 );
        assertEquals( 3, attr1.size() );
        assertTrue( attr1.contains( BYTES1 ) );
        assertTrue( attr1.contains( BYTES2 ) );
        assertTrue( attr1.contains( BYTES3 ) );
        assertFalse( attr1.contains( BYTES4 ) );
        assertFalse( attr1.contains( (byte[])null ) );

        EntryAttribute attr2 = new DefaultClientAttribute( "test" );
        assertEquals( 0, attr2.size() );
        assertFalse( attr2.contains( "a" ) );
        assertFalse( attr2.contains( (String)null ) );
        
        attr2.add( (String)null );
        assertEquals( 1, attr2.size() );
        assertTrue( attr2.contains( (String)null ) );
        
        attr2.remove( (String)null );
        assertFalse( attr2.contains( (String)null ) );
        assertEquals( 0, attr2.size() );
        
        attr2.add( "ab", "b", "c" );
        assertEquals( 3, attr2.size() );
        assertTrue( attr2.contains( BYTES1 ) );
        assertTrue( attr2.contains( BYTES2 ) );
        assertTrue( attr2.contains( BYTES3 ) );
        assertFalse( attr2.contains( (byte[])null ) );
    }


    /**
     * Test method get()
     */
    @Test
    public void testGet() throws InvalidAttributeValueException, NamingException
    {
        EntryAttribute attr1 = new DefaultClientAttribute( "test" );
        
        attr1.add( (String)null );
        assertEquals( NULL_STRING_VALUE,attr1.get() );

        EntryAttribute attr2 = new DefaultClientAttribute( "test" );
        
        attr2.add( "a", "b", "c" );
        assertEquals( "a", attr2.get().get() );
        
        attr2.remove( "a" );
        assertEquals( "b", attr2.get().get() );

        attr2.remove( "b" );
        assertEquals( "c", attr2.get().get() );

        attr2.remove( "c" );
        assertNull( attr2.get() );

        EntryAttribute attr3 = new DefaultClientAttribute( "test" );
        
        attr3.add( BYTES1, BYTES2, BYTES3 );
        assertTrue( Arrays.equals( BYTES1, (byte[])attr3.get().get() ) );
        
        attr3.remove( BYTES1 );
        assertTrue( Arrays.equals( BYTES2, (byte[])attr3.get().get() ) );

        attr3.remove( BYTES2 );
        assertTrue( Arrays.equals( BYTES3, (byte[])attr3.get().get() ) );

        attr3.remove( BYTES3 );
        assertNull( attr2.get() );
    }


    /**
     * Test method getAll()
     */
    @Test
    public void testGetAll()
    {
        EntryAttribute attr = new DefaultClientAttribute( "test" );
        
        Iterator<Value<?>> iterator = attr.getAll(); 
        assertFalse( iterator.hasNext() );
        
        attr.add( NULL_STRING_VALUE );
        iterator = attr.getAll(); 
        assertTrue( iterator.hasNext() );
        
        Value<?> value = iterator.next();
        assertEquals( NULL_STRING_VALUE, value );
        
        attr.clear();
        iterator = attr.getAll(); 
        assertFalse( iterator.hasNext() );
        
        attr.add(  "a", "b", "c" );
        iterator = attr.getAll(); 
        assertTrue( iterator.hasNext() );
        assertEquals( "a", iterator.next().get() );
        assertEquals( "b", iterator.next().get() );
        assertEquals( "c", iterator.next().get() );
        assertFalse( iterator.hasNext() );
    }


    /**
     * Test method size()
     */
    @Test
    public void testSize() throws InvalidAttributeValueException, NamingException
    {
        EntryAttribute attr1 = new DefaultClientAttribute( "test" );

        assertEquals( 0, attr1.size() );
        
        attr1.add( (String)null );
        assertEquals( 1, attr1.size() );

        EntryAttribute attr2 = new DefaultClientAttribute( "test" );
        
        attr2.add( "a", "b" );
        assertEquals( 2, attr2.size() );
        
        attr2.clear();
        assertEquals( 0, attr2.size() );
    }


    /**
     * Test method remove( Value... )
     */
    @Test
    public void testRemoveValueArray() throws InvalidAttributeValueException, NamingException
    {
        EntryAttribute attr1 = new DefaultClientAttribute( "test" );

        assertFalse( attr1.remove( STR_VALUE1 ) );

        attr1.setHR( true );
        assertFalse( attr1.remove( STR_VALUE1 ) );
        
        attr1.put( "a", "b", "c" );
        assertTrue( attr1.remove( STR_VALUE1 ) );
        assertEquals( 2, attr1.size() );
        
        assertTrue( attr1.remove( STR_VALUE2, STR_VALUE3 ) );
        assertEquals( 0, attr1.size() );
        
        assertFalse( attr1.remove( STR_VALUE4 ) );
        
        attr1.put( "a", "b", "c" );
        assertFalse( attr1.remove( STR_VALUE2, STR_VALUE4 ) );
        assertEquals( 2, attr1.size() );
        
        attr1.clear();
        attr1.put( "a", (String)null, "b" );
        assertTrue( attr1.remove( NULL_STRING_VALUE, STR_VALUE1 ) );
        assertEquals( 1, attr1.size() );
        
        attr1.clear();
        attr1.put( "a", (String)null, "b" );
        attr1.add( BYTES3 );
        assertTrue( attr1.remove( NULL_STRING_VALUE, STR_VALUE1, BIN_VALUE3 ) );
        assertEquals( 1, attr1.size() );
        
        EntryAttribute attr2 = new DefaultClientAttribute( "test" );

        assertFalse( attr2.remove( BIN_VALUE1 ) );

        attr2.setHR( true );
        assertFalse( attr2.remove( BIN_VALUE1 ) );
        
        attr2.put( BYTES1, BYTES2, BYTES3 );
        assertTrue( attr2.remove( BIN_VALUE1 ) );
        assertEquals( 2, attr2.size() );
        
        assertTrue( attr2.remove( BIN_VALUE2, BIN_VALUE3 ) );
        assertEquals( 0, attr2.size() );
        
        assertFalse( attr2.remove( BIN_VALUE4 ) );
        
        attr2.put( BYTES1, BYTES2, BYTES3 );
        assertFalse( attr2.remove( BIN_VALUE2, STR_VALUE4 ) );
        assertEquals( 2, attr2.size() );
        
        attr2.clear();
        attr2.put( BYTES1, (byte[])null, BYTES3 );
        assertTrue( attr2.remove( NULL_STRING_VALUE, BIN_VALUE1 ) );
        assertEquals( 1, attr2.size() );
        
        attr2.clear();
        attr2.put( BYTES1, (byte[])null, BYTES2 );
        attr2.add( "c" );
        assertTrue( attr2.remove( NULL_STRING_VALUE, BIN_VALUE1, STR_VALUE3 ) );
        assertEquals( 1, attr2.size() );
    }


    /**
     * Test method remove( byte... )
     */
    @Test
    public void testRemoveByteArray() throws InvalidAttributeValueException, NamingException
    {
        EntryAttribute attr1 = new DefaultClientAttribute( "test" );

        assertFalse( attr1.remove( BYTES1 ) );

        attr1.setHR( false );
        assertFalse( attr1.remove( BYTES1 ) );
        
        attr1.put( BYTES1, BYTES2, BYTES3 );
        assertTrue( attr1.remove( BYTES1 ) );
        assertEquals( 2, attr1.size() );
        
        assertTrue( attr1.remove( BYTES2, BYTES3 ) );
        assertEquals( 0, attr1.size() );
        
        assertFalse( attr1.remove( BYTES4 ) );
        
        attr1.put( BYTES1, BYTES2, BYTES3 );
        assertFalse( attr1.remove( BYTES3, BYTES4 ) );
        assertEquals( 2, attr1.size() );
        
        attr1.clear();
        attr1.put( BYTES1, (byte[])null, BYTES2 ) ;
        assertTrue( attr1.remove( (byte[])null, BYTES1 ) );
        assertEquals( 1, attr1.size() );
        
        EntryAttribute attr2 = new DefaultClientAttribute( "test" );
        
        attr2.put( "ab", "b", "c" );
        
        assertFalse( attr2.remove( (byte[])null ) );
        assertTrue( attr2.remove( BYTES1, BYTES2 ) );
        assertFalse( attr2.remove( BYTES4 ) );
    }


    /**
     * Test method remove( String... )
     */
    @Test
    public void testRemoveStringArray() throws InvalidAttributeValueException, NamingException
    {
        EntryAttribute attr1 = new DefaultClientAttribute( "test" );

        assertFalse( attr1.remove( "a" ) );

        attr1.setHR( true );
        assertFalse( attr1.remove( "a" ) );
        
        attr1.put( "a", "b", "c" );
        assertTrue( attr1.remove( "a" ) );
        assertEquals( 2, attr1.size() );
        
        assertTrue( attr1.remove( "b", "c" ) );
        assertEquals( 0, attr1.size() );
        
        assertFalse( attr1.remove( "d" ) );
        
        attr1.put( "a", "b", "c" );
        assertFalse( attr1.remove( "b", "e" ) );
        assertEquals( 2, attr1.size() );
        
        attr1.clear();
        attr1.put( "a", (String)null, "b" );
        assertTrue( attr1.remove( (String )null, "a" ) );
        assertEquals( 1, attr1.size() );
        
        EntryAttribute attr2 = new DefaultClientAttribute( "test" );
        
        attr2.put( BYTES1, BYTES2, BYTES3 );
        
        assertFalse( attr2.remove( (String)null ) );
        assertTrue( attr2.remove( "ab", "c" ) );
        assertFalse( attr2.remove( "d" ) );
    }


    /**
     * Test method put( String... )
     */
    @Test
    public void testPutStringArray() throws InvalidAttributeValueException, NamingException
    {
        EntryAttribute attr1 = new DefaultClientAttribute( "test" );
        
        int nbAdded = attr1.put( (String)null );
        assertEquals( 1, nbAdded );
        assertTrue( attr1.isHR() );
        assertEquals( NULL_STRING_VALUE, attr1.get() );
        
        EntryAttribute attr2 = new DefaultClientAttribute( "test" );
        
        nbAdded = attr2.put( "" );
        assertEquals( 1, nbAdded );
        assertTrue( attr2.isHR() );
        assertEquals( "", attr2.getString() );
        
        EntryAttribute attr3 = new DefaultClientAttribute( "test" );
        
        nbAdded = attr3.put( "t" );
        assertEquals( 1, nbAdded );
        assertTrue( attr3.isHR() );
        assertEquals( "t", attr3.getString() );
        
        EntryAttribute attr4 = new DefaultClientAttribute( "test" );
        
        nbAdded = attr4.put( "a", "b", "c", "d" );
        assertEquals( 4, nbAdded );
        assertTrue( attr4.isHR() );
        assertTrue( attr4.contains( "a" ) );
        assertTrue( attr4.contains( "b" ) );
        assertTrue( attr4.contains( "c" ) );
        assertTrue( attr4.contains( "d" ) );
        
        EntryAttribute attr5 = new DefaultClientAttribute( "test" );
        
        nbAdded = attr5.put( "a", "b", (String)null, "d" );
        assertEquals( 4, nbAdded );
        assertTrue( attr5.isHR() );
        assertTrue( attr5.contains( "a" ) );
        assertTrue( attr5.contains( "b" ) );
        assertTrue( attr5.contains( (String)null ) );
        assertTrue( attr5.contains( "d" ) );

        EntryAttribute attr6 = new DefaultClientAttribute( "test" );
        
        attr6.setHR( false );
        nbAdded = attr6.put( "a", (String)null );
        assertEquals( 2, nbAdded );
        assertFalse( attr6.isHR() );
        assertTrue( attr6.contains( new byte[]{'a'} ) );
        assertTrue( attr6.contains( (byte[])null ) );
    }


    /**
     * Test method put( byte[]... )
     */
    @Test
    public void testPutByteArray() throws InvalidAttributeValueException, NamingException
    {
        EntryAttribute attr1 = new DefaultClientAttribute( "test" );
        
        int nbAdded = attr1.put( (byte[])null );
        assertEquals( 1, nbAdded );
        assertFalse( attr1.isHR() );
        assertTrue( Arrays.equals( NULL_BINARY_VALUE.get(), attr1.getBytes() ) );
        
        EntryAttribute attr2 = new DefaultClientAttribute( "test" );
        
        nbAdded = attr2.put( StringTools.EMPTY_BYTES );
        assertEquals( 1, nbAdded );
        assertFalse( attr2.isHR() );
        assertTrue( Arrays.equals( StringTools.EMPTY_BYTES, attr2.getBytes() ) );
        
        EntryAttribute attr3 = new DefaultClientAttribute( "test" );
        
        nbAdded = attr3.put( BYTES1 );
        assertEquals( 1, nbAdded );
        assertFalse( attr3.isHR() );
        assertTrue( Arrays.equals( BYTES1, attr3.getBytes() ) );
        
        EntryAttribute attr4 = new DefaultClientAttribute( "test" );
        
        nbAdded = attr4.put( BYTES1, BYTES2 );
        assertEquals( 2, nbAdded );
        assertFalse( attr4.isHR() );
        assertTrue( attr4.contains( BYTES1 ) );
        assertTrue( attr4.contains( BYTES2 ) );
        
        nbAdded = attr4.put( BYTES3, BYTES4 );
        assertEquals( 2, nbAdded );
        assertFalse( attr4.isHR() );
        assertTrue( attr4.contains( BYTES3 ) );
        assertTrue( attr4.contains( BYTES4 ) );
        
        EntryAttribute attr5 = new DefaultClientAttribute( "test" );
        
        nbAdded = attr5.put( BYTES1, BYTES2, (byte[])null, BYTES3 );
        assertEquals( 4, nbAdded );
        assertFalse( attr5.isHR() );
        assertTrue( attr5.contains( BYTES1 ) );
        assertTrue( attr5.contains( BYTES2 ) );
        assertTrue( attr5.contains( (byte[])null ) );
        assertTrue( attr5.contains( BYTES3 ) );

        EntryAttribute attr6 = new DefaultClientAttribute( "test" );
        
        attr6.setHR( true );
        nbAdded = attr6.put( BYTES1, (byte[])null );
        assertEquals( 2, nbAdded );
        assertTrue( attr6.isHR() );
        assertTrue( attr6.contains( "ab" ) );
        assertTrue( attr6.contains( (String)null ) );
    }


    /**
     * Test method put( Value... )
     */
    @Test
    public void testPutValueArray() throws InvalidAttributeValueException, NamingException
    {
        EntryAttribute attr1 = new DefaultClientAttribute( "test" );
        
        assertEquals( 0, attr1.size() );
        
        attr1.put( NULL_STRING_VALUE );
        assertEquals( 1, attr1.size() );
        assertTrue( attr1.contains( NULL_STRING_VALUE ) );
        
        attr1.put( STR_VALUE1, STR_VALUE2, STR_VALUE3 );
        assertEquals( 3, attr1.size() );
        assertTrue( attr1.contains( STR_VALUE1 ) );
        assertTrue( attr1.contains( STR_VALUE2 ) );
        assertTrue( attr1.contains( STR_VALUE3 ) );

        attr1.put( STR_VALUE1, NULL_STRING_VALUE, STR_VALUE3 );
        assertEquals( 3, attr1.size() );
        assertTrue( attr1.contains( STR_VALUE1 ) );
        assertTrue( attr1.contains( NULL_STRING_VALUE ) );
        assertTrue( attr1.contains( STR_VALUE3 ) );
        
        attr1.put( STR_VALUE1, NULL_STRING_VALUE, BIN_VALUE3 );
        assertEquals( 3, attr1.size() );
        assertTrue( attr1.contains( STR_VALUE1 ) );
        assertTrue( attr1.contains( NULL_STRING_VALUE ) );
        assertTrue( attr1.contains( STR_VALUE3 ) );
        

        EntryAttribute attr2 = new DefaultClientAttribute( "test" );
        assertEquals( 0, attr2.size() );
        
        attr2.put( NULL_BINARY_VALUE );
        assertEquals( 1, attr2.size() );
        assertTrue( attr2.contains( NULL_BINARY_VALUE ) );
        
        attr2.put( BIN_VALUE1, BIN_VALUE2, BIN_VALUE3 );
        assertEquals( 3, attr2.size() );
        assertTrue( attr2.contains( BIN_VALUE1 ) );
        assertTrue( attr2.contains( BIN_VALUE2 ) );
        assertTrue( attr2.contains( BIN_VALUE3 ) );
        
        attr2.put( BIN_VALUE1, NULL_BINARY_VALUE, STR_VALUE3 );
        assertEquals( 3, attr2.size() );
        assertTrue( attr2.contains( BIN_VALUE1 ) );
        assertTrue( attr2.contains( NULL_BINARY_VALUE ) );
        assertTrue( attr2.contains( BIN_VALUE3 ) );
    }


    /**
     * Test method put( List&lt;Value&gt; )
     */
    @Test
    public void testPutListOfValues()
    {
        EntryAttribute attr1 = new DefaultClientAttribute( "test" );
        
        assertEquals( 0, attr1.size() );
        
        List<Value<?>> list = new ArrayList<Value<?>>();
        list.add( NULL_STRING_VALUE );
        
        attr1.put( list );
        assertEquals( 1, attr1.size() );
        assertTrue( attr1.contains( NULL_STRING_VALUE ) );
        
        list.clear();
        list.add( STR_VALUE1 );
        list.add( STR_VALUE2 );
        list.add( STR_VALUE3 );
        attr1.put( list );
        assertEquals( 3, attr1.size() );
        assertTrue( attr1.contains( STR_VALUE1 ) );
        assertTrue( attr1.contains( STR_VALUE2 ) );
        assertTrue( attr1.contains( STR_VALUE3 ) );

        list.clear();
        list.add( STR_VALUE1 );
        list.add( NULL_STRING_VALUE );
        list.add( STR_VALUE3 );
        attr1.put( list );
        assertEquals( 3, attr1.size() );
        assertTrue( attr1.contains( STR_VALUE1 ) );
        assertTrue( attr1.contains( NULL_STRING_VALUE ) );
        assertTrue( attr1.contains( STR_VALUE3 ) );
        
        list.clear();
        list.add( STR_VALUE1 );
        list.add( NULL_STRING_VALUE );
        list.add( BIN_VALUE3 );
        attr1.put( list );
        assertEquals( 3, attr1.size() );
        assertTrue( attr1.contains( STR_VALUE1 ) );
        assertTrue( attr1.contains( NULL_STRING_VALUE ) );
        assertTrue( attr1.contains( STR_VALUE3 ) );
        

        EntryAttribute attr2 = new DefaultClientAttribute( "test" );
        assertEquals( 0, attr2.size() );
        
        list.clear();
        list.add( NULL_BINARY_VALUE );
        attr2.put( list );
        assertEquals( 1, attr2.size() );
        assertTrue( attr2.contains( NULL_BINARY_VALUE ) );
        
        list.clear();
        list.add( BIN_VALUE1 );
        list.add( BIN_VALUE2 );
        list.add( BIN_VALUE3 );
        attr2.put( list );
        assertEquals( 3, attr2.size() );
        assertTrue( attr2.contains( BIN_VALUE1 ) );
        assertTrue( attr2.contains( BIN_VALUE2 ) );
        assertTrue( attr2.contains( BIN_VALUE3 ) );
        
        list.clear();
        list.add( BIN_VALUE1 );
        list.add( NULL_BINARY_VALUE );
        list.add( STR_VALUE3 );
        attr2.put( list );
        assertEquals( 3, attr2.size() );
        assertTrue( attr2.contains( BIN_VALUE1 ) );
        assertTrue( attr2.contains( NULL_BINARY_VALUE ) );
        assertTrue( attr2.contains( BIN_VALUE3 ) );
    }


    /**
     * Test method toString()
     */
    @Test
    public void testToString() throws InvalidAttributeValueException, NamingException
    {
        EntryAttribute attr1 = new DefaultClientAttribute( "test" );
        
        assertEquals( "    test: (null)\n", attr1.toString() );
        
        attr1.add( "a" );
        assertEquals( "    test: a\n", attr1.toString() );
        
        attr1.add( "b" );
        assertEquals( "    test: a\n    test: b\n", attr1.toString() );

        EntryAttribute attr2 = new DefaultClientAttribute( "test" );

        attr2.add( BYTES1 );
        assertEquals( "    test: '0x61 0x62 '\n", attr2.toString() );

        attr2.add( BYTES3 );
        assertEquals( "    test: '0x61 0x62 '\n    test: '0x63 '\n", attr2.toString() );
    }


    /**
     * Test method hashCode()
     */
    @Test
    public void testHashCode() throws InvalidAttributeValueException, NamingException
    {
        EntryAttribute attr = new DefaultClientAttribute();
        assertEquals( 37, attr.hashCode() );
        
        attr.setHR( true );
        assertEquals( 37*17 + 1231, attr.hashCode() );
        
        attr.setHR(  false );
        assertEquals( 37*17 + 1237, attr.hashCode() );

        EntryAttribute attr1 = new DefaultClientAttribute( "test" );
        EntryAttribute attr2 = new DefaultClientAttribute( "test" );
        
        assertEquals( attr1.hashCode(), attr2.hashCode() );
        
        attr1.put( "a", "b", "c" );
        attr2.put( "a", "b", "c" );
        assertEquals( attr1.hashCode(), attr2.hashCode() );
        
        attr1.add( "d" );
        attr2.add( "d" );
        assertEquals( attr1.hashCode(), attr2.hashCode() );

        attr1.add( NULL_STRING_VALUE );
        attr2.add(  NULL_STRING_VALUE );
        assertEquals( attr1.hashCode(), attr2.hashCode() );

        // Order mess up the hashCode
        attr1.put( "a", "b", "c" );
        attr2.put( "c", "b", "a" );
        assertNotSame( attr1.hashCode(), attr2.hashCode() );
        
        EntryAttribute attr3 = new DefaultClientAttribute( "test" );
        EntryAttribute attr4 = new DefaultClientAttribute( "test" );
        
        attr3.put( BYTES1, BYTES2 );
        attr4.put( BYTES1, BYTES2 );
        assertEquals( attr3.hashCode(), attr4.hashCode() );
        
        attr3.add( BYTES3 );
        attr4.add( BYTES3 );
        assertEquals( attr3.hashCode(), attr4.hashCode() );
        
        attr3.add( NULL_BINARY_VALUE );
        attr4.add(  NULL_BINARY_VALUE );
        assertEquals( attr3.hashCode(), attr4.hashCode() );

        // Order mess up the hashCode
        attr3.put( BYTES1, BYTES2 );
        attr4.put( BYTES2, BYTES1 );
        assertNotSame( attr3.hashCode(), attr4.hashCode() );
    }


    /**
     * Test method testEquals()
     */
    @Test
    public void testEquals()
    {
        EntryAttribute attr1 = new DefaultClientAttribute( "test" );
        
        assertFalse( attr1.equals( null ) );
        
        EntryAttribute attr2 = new DefaultClientAttribute( "test" );
        
        assertTrue( attr1.equals( attr2 ) );
        
        attr2.setId( "TEST" );
        assertTrue( attr1.equals( attr2 ) );

        attr1.setId( "tset" );
        assertFalse( attr1.equals( attr2 ) );
        
        attr1.setUpId( "TEST" );
        assertTrue( attr1.equals( attr2 ) );
        
        attr1.add( "a", "b", "c" );
        attr2.add( "c", "b", "a" );
        assertTrue( attr1.equals( attr2 ) );
        
        attr1.setHR( true );
        attr2.setHR( false );
        assertFalse( attr1.equals( attr2 ) );
        
        EntryAttribute attr3 = new DefaultClientAttribute( "test" );
        EntryAttribute attr4 = new DefaultClientAttribute( "test" );
        
        attr3.put( NULL_BINARY_VALUE );
        attr4.put( NULL_BINARY_VALUE );
        assertTrue( attr3.equals( attr4 ) );
        
        EntryAttribute attr5 = new DefaultClientAttribute( "test" );
        EntryAttribute attr6 = new DefaultClientAttribute( "test" );
        
        attr5.put( NULL_BINARY_VALUE );
        attr6.put( NULL_STRING_VALUE );
        assertFalse( attr5.equals( attr6 ) );

        EntryAttribute attr7 = new DefaultClientAttribute( "test" );
        EntryAttribute attr8 = new DefaultClientAttribute( "test" );
        
        attr7.put( "a" );
        attr8.put( BYTES2 );
        assertFalse( attr7.equals( attr8 ) );

        EntryAttribute attr9 = new DefaultClientAttribute( "test" );
        EntryAttribute attr10 = new DefaultClientAttribute( "test" );
        
        attr7.put( "a" );
        attr7.add( BYTES2 );
        attr8.put( "a", "b" );
        assertTrue( attr9.equals( attr10 ) );
    }


    /**
     * Test method testClone()
     */
    @Test
    public void testClone()
    {
        EntryAttribute attr = new DefaultClientAttribute( "test" );
        
        EntryAttribute clone = attr.clone();
        
        assertEquals( attr, clone );
        attr.setId( "new" );
        assertEquals( "test", clone.getId() );
        
        attr.add( "a", (String)null, "b" );
        clone = attr.clone();
        assertEquals( attr, clone );
        
        attr.remove( "a" );
        assertNotSame( attr, clone );
        
        clone = attr.clone();
        assertEquals( attr, clone );

        attr.setHR( false );
        assertNotSame( attr, clone );
    }
    
    
    /**
     * Test the serialization of a complete client attribute
     */
    @Test
    public void testSerializeCompleteAttribute() throws NamingException, IOException, ClassNotFoundException
    {
        DefaultClientAttribute dca = new DefaultClientAttribute( "CommonName" );
        dca.setHR( true );
        dca.setId( "cn" );
        dca.add( "test1", "test2" );

        DefaultClientAttribute dcaSer = deserializeValue( serializeValue( dca ) );
        assertEquals( dca.toString(), dcaSer.toString() );
        assertEquals( "commonname", dcaSer.getId() );
        assertEquals( "CommonName", dcaSer.getUpId() );
        assertEquals( "test1", dcaSer.getString() );
        assertTrue( dcaSer.contains( "test2", "test1" ) );
        assertTrue( dcaSer.isHR() );
        assertFalse( dcaSer.isValid() );
    }
    
    
    /**
     * Test the serialization of a client attribute with no value
     */
    @Test
    public void testSerializeAttributeWithNoValue() throws NamingException, IOException, ClassNotFoundException
    {
        DefaultClientAttribute dca = new DefaultClientAttribute( "CommonName" );
        dca.setHR( true );
        dca.setId( "cn" );

        DefaultClientAttribute dcaSer = deserializeValue( serializeValue( dca ) );
        assertEquals( dca.toString(), dcaSer.toString() );
        assertEquals( "commonname", dcaSer.getId() );
        assertEquals( "CommonName", dcaSer.getUpId() );
        assertEquals( 0, dcaSer.size() );
        assertTrue( dcaSer.isHR() );
        assertTrue( dcaSer.isValid() );
    }
    
    
    /**
     * Test the serialization of a client attribute with a null value
     */
    @Test
    public void testSerializeAttributeNullValue() throws NamingException, IOException, ClassNotFoundException
    {
        DefaultClientAttribute dca = new DefaultClientAttribute( "CommonName" );
        dca.setHR( true );
        dca.setId( "cn" );
        dca.add( (String)null );

        DefaultClientAttribute dcaSer = deserializeValue( serializeValue( dca ) );
        assertEquals( dca.toString(), dcaSer.toString() );
        assertEquals( "commonname", dcaSer.getId() );
        assertEquals( "CommonName", dcaSer.getUpId() );
        assertNull( dcaSer.getString() );
        assertEquals( 1, dcaSer.size() );
        assertTrue( dcaSer.contains( (String)null ) );
        assertTrue( dcaSer.isHR() );
        assertFalse( dcaSer.isValid() );
    }
    
    
    /**
     * Test the serialization of a client attribute with a binary value
     */
    @Test
    public void testSerializeAttributeBinaryValue() throws NamingException, IOException, ClassNotFoundException
    {
        DefaultClientAttribute dca = new DefaultClientAttribute( "UserPassword" );
        dca.setHR( false );
        byte[] password = StringTools.getBytesUtf8( "secret" );
        dca.add( password );

        DefaultClientAttribute dcaSer = deserializeValue( serializeValue( dca ) );
        assertEquals( dca.toString(), dcaSer.toString() );
        assertEquals( "userpassword", dcaSer.getId() );
        assertEquals( "UserPassword", dcaSer.getUpId() );
        assertTrue( Arrays.equals( dca.getBytes(), dcaSer.getBytes() ) );
        assertEquals( 1, dcaSer.size() );
        assertTrue( dcaSer.contains( password ) );
        assertFalse( dcaSer.isHR() );
        assertFalse( dcaSer.isValid() );
    }
}
