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
package org.apache.directory.server.core.entry;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.naming.directory.InvalidAttributeValueException;

import org.apache.commons.io.FileUtils;
import org.apache.directory.shared.ldap.model.entry.BinaryValue;
import org.apache.directory.shared.ldap.model.entry.DefaultAttribute;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.StringValue;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.shared.util.StringConstants;
import org.apache.directory.shared.util.Strings;
import org.apache.directory.shared.util.exception.Exceptions;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;


/**
 * Tests for the DefaultEntryAttribute class
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class SchemaAwareEntryAttributeTest
{
    private static LdifSchemaLoader loader;

    private static AttributeType atCN;
    private static AttributeType atDC;
    private static AttributeType atSN;
    private static AttributeType atName;

    // A SINGLE-VALUE attribute
    private static AttributeType atC;

    // A Binary attribute
    private static AttributeType atPwd;

    // A String attribute which allows null value
    private static AttributeType atEMail;

    private static final Value<String> NULL_STRING_VALUE = new StringValue( ( String ) null );
    private static final Value<byte[]> NULL_BINARY_VALUE = new BinaryValue( ( byte[] ) null );
    private static final byte[] BYTES1 = new byte[]
        { 'a', 'b' };
    private static final byte[] BYTES2 = new byte[]
        { 'b' };
    private static final byte[] BYTES3 = new byte[]
        { 'c' };
    private static final byte[] BYTES4 = new byte[]
        { 'd' };

    private static final StringValue STR_VALUE1 = new StringValue( "a" );
    private static final StringValue STR_VALUE2 = new StringValue( "b" );
    private static final StringValue STR_VALUE3 = new StringValue( "c" );
    private static final StringValue STR_VALUE4 = new StringValue( "d" );

    private static final BinaryValue BIN_VALUE1 = new BinaryValue( BYTES1 );
    private static final BinaryValue BIN_VALUE2 = new BinaryValue( BYTES2 );
    private static final BinaryValue BIN_VALUE3 = new BinaryValue( BYTES3 );
    private static final BinaryValue BIN_VALUE4 = new BinaryValue( BYTES4 );


    /**
     * Initialize the registries once for the whole test suite
     */
    @BeforeClass
    public static void setup() throws Exception
    {
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = SchemaAwareEntryAttributeTest.class.getResource( "" ).getPath();
            int targetPos = path.indexOf( "target" );
            workingDirectory = path.substring( 0, targetPos + 6 );
        }

        // Cleanup the target directory
        File schemaRepository = new File( workingDirectory, "schema" );
        FileUtils.deleteDirectory( schemaRepository );

        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy( true );
        loader = new LdifSchemaLoader( schemaRepository );
        SchemaManager schemaManager = new DefaultSchemaManager( loader );

        schemaManager.loadAllEnabled();

        List<Throwable> errors = schemaManager.getErrors();

        if ( errors.size() != 0 )
        {
            // We have inconsistencies : log them and exit.
            throw new RuntimeException( "Inconsistent schemas : " + Exceptions.printErrors(errors) );
        }

        atCN = schemaManager.lookupAttributeTypeRegistry( "cn" );
        atDC = schemaManager.lookupAttributeTypeRegistry( "dc" );
        atC = schemaManager.lookupAttributeTypeRegistry( "c" );
        atSN = schemaManager.lookupAttributeTypeRegistry( "sn" );
        atPwd = schemaManager.lookupAttributeTypeRegistry( "userpassword" );
        atEMail = schemaManager.lookupAttributeTypeRegistry( "email" );
        atName = schemaManager.lookupAttributeTypeRegistry( "name" );
    }


    /**
     * Serialize a DefaultEntryAttribute
     */
    private ByteArrayOutputStream serializeValue( DefaultAttribute value ) throws IOException
    {
        ObjectOutputStream oOut = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try
        {
            oOut = new ObjectOutputStream( out );
            value.writeExternal( oOut );
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
     * Deserialize a DefaultEntryAttribute
     */
    private DefaultAttribute deserializeValue( ByteArrayOutputStream out, AttributeType at ) throws IOException,
        ClassNotFoundException
    {
        ObjectInputStream oIn = null;
        ByteArrayInputStream in = new ByteArrayInputStream( out.toByteArray() );

        try
        {
            oIn = new ObjectInputStream( in );

            DefaultAttribute value = new DefaultAttribute( at );
            value.readExternal( oIn );

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


    @Test
    public void testAddOneValue() throws Exception
    {
        AttributeType at = TestServerEntryUtils.getIA5StringAttributeType();

        DefaultAttribute attr = new DefaultAttribute( at );

        // Add a String value
        attr.add( "test" );

        assertEquals( 1, attr.size() );

        assertTrue( attr.getAttributeType().getSyntax().isHumanReadable() );

        Value<?> value = attr.get();

        assertTrue( value instanceof StringValue );
        assertEquals( "test", ( ( StringValue ) value ).getString() );

        // Add a binary value
        assertEquals( 0, attr.add( new byte[]
            { 0x01 } ) );

        // Add a Value
        Value<?> ssv = new StringValue( at, "test2" );

        attr.add( ssv );

        assertEquals( 2, attr.size() );

        Set<String> expected = new HashSet<String>();
        expected.add( "test" );
        expected.add( "test2" );

        for ( Value<?> val : attr )
        {
            if ( expected.contains( val.getValue() ) )
            {
                expected.remove( val.getValue() );
            }
            else
            {
                fail();
            }
        }

        assertEquals( 0, expected.size() );
    }


    @Test
    public void testAddTwoValue() throws Exception
    {
        AttributeType at = TestServerEntryUtils.getIA5StringAttributeType();

        DefaultAttribute attr = new DefaultAttribute( at );

        // Add String values
        attr.add( "test" );
        attr.add( "test2" );

        assertEquals( 2, attr.size() );

        assertTrue( attr.getAttributeType().getSyntax().isHumanReadable() );

        Set<String> expected = new HashSet<String>();
        expected.add( "test" );
        expected.add( "test2" );

        for ( Value<?> val : attr )
        {
            if ( expected.contains( val.getValue() ) )
            {
                expected.remove( val.getValue() );
            }
            else
            {
                fail();
            }
        }

        assertEquals( 0, expected.size() );
    }


    @Test
    public void testAddNullValue() throws Exception
    {
        AttributeType at = TestServerEntryUtils.getIA5StringAttributeType();

        DefaultAttribute attr = new DefaultAttribute( at );

        // Add a null value
        attr.add( new StringValue( at, null ) );

        assertEquals( 1, attr.size() );

        assertTrue( attr.getAttributeType().getSyntax().isHumanReadable() );

        Value<?> value = attr.get();

        assertTrue( value instanceof StringValue );
        assertNull( ( ( StringValue ) value ).getValue() );
    }


    @Test
    public void testGetAttribute() throws Exception
    {
        AttributeType at = TestServerEntryUtils.getIA5StringAttributeType();

        DefaultAttribute attr = new DefaultAttribute( at );

        attr.add( "Test1" );
        attr.add( "Test2" );
        attr.add( "Test3" );

        assertEquals( "1.1", attr.getId() );
        assertEquals( 3, attr.size() );
        assertTrue( attr.contains( "Test1" ) );
        assertTrue( attr.contains( "Test2" ) );
        assertTrue( attr.contains( "Test3" ) );
    }


    /**
     * Test the contains() method
     */
    @Test
    public void testContains() throws Exception
    {
        AttributeType at = TestServerEntryUtils.getIA5StringAttributeType();

        DefaultAttribute attr = new DefaultAttribute( at );

        attr.add( "Test  1" );
        attr.add( "Test  2" );
        attr.add( "Test  3" );

        assertTrue( attr.contains( "test 1" ) );
        assertTrue( attr.contains( "Test 2" ) );
        assertTrue( attr.contains( "TEST     3" ) );
    }


    /**
     * Test method getBytes()
     */
    @Test
    public void testGetBytes() throws LdapInvalidAttributeValueException
    {
        Attribute attr1 = new DefaultAttribute( atPwd );

        attr1.add( ( byte[] ) null );
        assertNull( attr1.getBytes() );

        Attribute attr2 = new DefaultAttribute( atPwd );

        attr2.add( BYTES1, BYTES2 );
        assertTrue( Arrays.equals( BYTES1, attr2.getBytes() ) );

        Attribute attr3 = new DefaultAttribute( atCN );

        attr3.add( "a", "b" );

        try
        {
            attr3.getBytes();
            fail();
        }
        catch ( LdapInvalidAttributeValueException ivae )
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
        Attribute attr = new DefaultAttribute( atCN );

        assertEquals( "2.5.4.3", attr.getId() );

        attr.setUpId( "  CN  " );
        assertEquals( "2.5.4.3", attr.getId() );
        assertEquals( "  CN  ", attr.getUpId() );

        attr.setUpId( "  CommonName  " );
        assertEquals( "2.5.4.3", attr.getId() );
        assertEquals( "  CommonName  ", attr.getUpId() );

        attr.setUpId( "  2.5.4.3  " );
        assertEquals( "2.5.4.3", attr.getId() );
    }


    /**
     * Test method getString()
     */
    @Test
    public void testGetString() throws LdapInvalidAttributeValueException
    {
        Attribute attr1 = new DefaultAttribute( atDC );

        assertEquals( 1, attr1.add( ( String ) null ) );

        Attribute attr2 = new DefaultAttribute( atDC );

        attr2.add( "a" );
        assertEquals( "a", attr2.getString() );

        Attribute attr3 = new DefaultAttribute( atPwd );

        attr3.add( BYTES1, BYTES2 );

        try
        {
            attr3.getString();
            fail();
        }
        catch ( LdapInvalidAttributeValueException ivae )
        {
            assertTrue( true );
        }
    }


    /**
     * Test method getUpId
     */
    @Test
    public void testGetUpId()
    {
        Attribute attr = new DefaultAttribute( atCN );

        assertNotNull( attr.getUpId() );
        assertEquals( "cn", attr.getUpId() );

        attr.setUpId( "CN" );
        assertEquals( "CN", attr.getUpId() );

        attr.setUpId( "  Cn  " );
        assertEquals( "  Cn  ", attr.getUpId() );

        attr.setUpId( "  2.5.4.3  " );
        assertEquals( "  2.5.4.3  ", attr.getUpId() );
    }


    /**
     * Test method hashCode()
     */
    @Test
    public void testHashCode() throws LdapException
    {
        Attribute attr1 = new DefaultAttribute( atDC );
        Attribute attr2 = new DefaultAttribute( atSN );
        assertNotSame( attr1.hashCode(), attr2.hashCode() );

        attr2.apply( atDC );
        assertEquals( attr1.hashCode(), attr2.hashCode() );

        attr1.add( ( String ) null );
        assertNotSame( attr1.hashCode(), attr2.hashCode() );

        attr1.clear();
        assertEquals( attr1.hashCode(), attr2.hashCode() );

        attr1.add( "a", "b" );
        assertNotSame( attr1.hashCode(), attr2.hashCode() );

        attr2.add( "a", "b" );
        assertEquals( attr1.hashCode(), attr2.hashCode() );

        // Order matters
        attr2.clear();
        attr2.add( "b", "a" );
        assertNotSame( attr1.hashCode(), attr2.hashCode() );

        Attribute attr3 = new DefaultAttribute( atPwd );
        Attribute attr4 = new DefaultAttribute( atPwd );
        assertNotSame( attr3.hashCode(), attr4.hashCode() );

        attr3.add( ( byte[] ) null );
        assertNotSame( attr3.hashCode(), attr4.hashCode() );

        attr3.clear();
        assertEquals( attr3.hashCode(), attr4.hashCode() );

        attr3.add( new byte[]
            { 0x01, 0x02 }, new byte[]
            { 0x03, 0x04 } );
        assertNotSame( attr1.hashCode(), attr2.hashCode() );

        attr4.add( new byte[]
            { 0x01, 0x02 }, new byte[]
            { 0x03, 0x04 } );
        assertNotSame( attr1.hashCode(), attr2.hashCode() );

        // Order matters
        attr4.clear();
        attr4.add( new byte[]
            { 0x03, 0x04 }, new byte[]
            { 0x01, 0x02 } );
        assertNotSame( attr1.hashCode(), attr2.hashCode() );
    }


    /**
     * Test method SetId(String)
     */
    @Test
    public void testSetId()
    {
        Attribute attr = new DefaultAttribute( atCN );

        attr.setUpId( "Cn" );
        assertEquals( "2.5.4.3", attr.getId() );
        assertEquals( "Cn", attr.getUpId() );

        attr.setUpId( " CN " );
        assertEquals( "2.5.4.3", attr.getId() );
        assertEquals( " CN ", attr.getUpId() );

        attr.setUpId( " 2.5.4.3 " );
        assertEquals( " 2.5.4.3 ", attr.getUpId() );
        assertEquals( "2.5.4.3", attr.getId() );

        attr.setUpId( " commonName " );
        assertEquals( "2.5.4.3", attr.getId() );
        assertEquals( " commonName ", attr.getUpId() );

        attr.setUpId( null );
        assertEquals( "2.5.4.3", attr.getId() );
        assertEquals( "cn", attr.getUpId() );

        attr.setUpId( "" );
        assertEquals( "2.5.4.3", attr.getId() );
        assertEquals( "cn", attr.getUpId() );

        attr.setUpId( "  " );
        assertEquals( "2.5.4.3", attr.getId() );
        assertEquals( "cn", attr.getUpId() );

        try
        {
            attr.setUpId( " SN " );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }
    }


    /**
     * Test method isValid()
     */
    @Test
    public void testIsValid() throws Exception
    {
        Attribute attr = new DefaultAttribute( atCN );

        // No value, this should not be valid
        assertFalse( attr.isValid( atCN.getEquality().getSyntax().getSyntaxChecker() ) );

        attr.add( "test", "test2", "A123\\;" );
        assertTrue( attr.isValid( atCN.getEquality().getSyntax().getSyntaxChecker() ) );

        // If we try to add a wrong value, it will not be added. The
        // attribute remains valid.
        assertEquals( 0, attr.add( new byte[]
            { 0x01 } ) );
        assertTrue( attr.isValid( atCN.getEquality().getSyntax().getSyntaxChecker() ) );

        // test a SINGLE-VALUE attribute. CountryName is SINGLE-VALUE
        attr.clear();
        attr.apply( atC );
        attr.add( "FR" );
        assertTrue( attr.isValid( atC.getEquality().getSyntax().getSyntaxChecker() ) );
        assertEquals( 0, attr.add( "US" ) );
        assertFalse( attr.contains( "US" ) );
        assertTrue( attr.isValid( atC.getEquality().getSyntax().getSyntaxChecker() ) );
    }


    /**
     * Test method add( Value... )
     */
    @Test
    public void testAddValueArray() throws LdapException
    {
        Attribute attr1 = new DefaultAttribute( atDC );

        int nbAdded = attr1.add( (String)null );
        assertEquals( 1, nbAdded );
        assertTrue( attr1.isHumanReadable() );
        assertEquals( NULL_STRING_VALUE, attr1.get() );

        Attribute attr2 = new DefaultAttribute( atPwd );

        nbAdded = attr2.add( new BinaryValue( atPwd, null ) );
        assertEquals( 1, nbAdded );
        assertFalse( attr2.isHumanReadable() );
        assertEquals( NULL_BINARY_VALUE, attr2.get() );

        Attribute attr3 = new DefaultAttribute( atCN );

        nbAdded = attr3.add( new StringValue( atCN, "a" ), new StringValue( atCN, "b" ) );
        assertEquals( 2, nbAdded );
        assertTrue( attr3.isHumanReadable() );
        assertTrue( attr3.contains( "a" ) );
        assertTrue( attr3.contains( "b" ) );

        Attribute attr4 = new DefaultAttribute( atCN );

        nbAdded = attr4.add( new BinaryValue( atPwd, BYTES1 ), new BinaryValue( atPwd, BYTES2 ) );
        assertEquals( 0, nbAdded );
        assertTrue( attr4.isHumanReadable() );
        assertFalse( attr4.contains( BYTES1 ) );
        assertFalse( attr4.contains( BYTES2 ) );

        Attribute attr5 = new DefaultAttribute( atCN );

        nbAdded = attr5.add( new StringValue( atCN, "c" ), new BinaryValue( atPwd, BYTES1 ) );
        assertEquals( 1, nbAdded );
        assertTrue( attr5.isHumanReadable() );
        assertFalse( attr5.contains( "ab" ) );
        assertTrue( attr5.contains( "c" ) );

        Attribute attr6 = new DefaultAttribute( atPwd );

        nbAdded = attr6.add( new BinaryValue( atPwd, BYTES1 ), new StringValue( atCN, "c" ) );
        assertEquals( 1, nbAdded );
        assertFalse( attr6.isHumanReadable() );
        assertTrue( attr6.contains( BYTES1 ) );
        assertFalse( attr6.contains( BYTES3 ) );

        Attribute attr7 = new DefaultAttribute( atPwd );

        nbAdded = attr7.add( new BinaryValue( atPwd, null ), new StringValue( atCN, "c" ) );
        assertEquals( 1, nbAdded );
        assertFalse( attr7.isHumanReadable() );
        assertTrue( attr7.contains( NULL_BINARY_VALUE ) );
        assertFalse( attr7.contains( BYTES3 ) );

        Attribute attr8 = new DefaultAttribute( atDC );

        nbAdded = attr8.add( new StringValue( atDC, null ), new BinaryValue( atPwd, BYTES1 ) );
        assertEquals( 1, nbAdded );
        assertTrue( attr8.isHumanReadable() );
        assertTrue( attr8.contains( NULL_STRING_VALUE ) );
        assertFalse( attr8.contains( "ab" ) );

        Attribute attr9 = new DefaultAttribute( atDC );

        nbAdded = attr9.add( new StringValue( ( String ) null ), new StringValue( "ab" ) );
        assertEquals( 2, nbAdded );
        assertTrue( attr9.isHumanReadable() );
        assertTrue( attr9.contains( NULL_STRING_VALUE ) );
        assertTrue( attr9.contains( "ab" ) );

        Attribute attr10 = new DefaultAttribute( atPwd );

        nbAdded = attr10.add( new BinaryValue( ( byte[] ) null ), new BinaryValue( BYTES1 ) );
        assertEquals( 2, nbAdded );
        assertFalse( attr10.isHumanReadable() );
        assertTrue( attr10.contains( NULL_BINARY_VALUE ) );
        assertTrue( attr10.contains( BYTES1 ) );
    }


    /**
     * Test method add( String... )
     */
    @Test
    public void testAddStringArray() throws LdapInvalidAttributeValueException
    {
        Attribute attr1 = new DefaultAttribute( atDC );

        int nbAdded = attr1.add( ( String ) null );
        assertEquals( 1, nbAdded );
        assertTrue( attr1.isHumanReadable() );
        assertEquals( NULL_STRING_VALUE, attr1.get() );

        Attribute attr2 = new DefaultAttribute( atDC );

        nbAdded = attr2.add( "" );
        assertEquals( 1, nbAdded );
        assertTrue( attr2.isHumanReadable() );
        assertEquals( "", attr2.getString() );

        Attribute attr3 = new DefaultAttribute( atCN );

        nbAdded = attr3.add( "t" );
        assertEquals( 1, nbAdded );
        assertTrue( attr3.isHumanReadable() );
        assertEquals( "t", attr3.getString() );

        Attribute attr4 = new DefaultAttribute( atCN );

        nbAdded = attr4.add( "a", "b", "c", "d" );
        assertEquals( 4, nbAdded );
        assertTrue( attr4.isHumanReadable() );
        assertEquals( "a", attr4.getString() );
        assertTrue( attr4.contains( "a" ) );
        assertTrue( attr4.contains( "b" ) );
        assertTrue( attr4.contains( "c" ) );
        assertTrue( attr4.contains( "d" ) );

        nbAdded = attr4.add( "e" );
        assertEquals( 1, nbAdded );
        assertTrue( attr4.isHumanReadable() );
        assertEquals( "a", attr4.getString() );
        assertTrue( attr4.contains( "a" ) );
        assertTrue( attr4.contains( "b" ) );
        assertTrue( attr4.contains( "c" ) );
        assertTrue( attr4.contains( "d" ) );
        assertTrue( attr4.contains( "e" ) );

        nbAdded = attr4.add( BYTES1 );
        assertEquals( 0, nbAdded );
        assertTrue( attr4.isHumanReadable() );
        assertEquals( "a", attr4.getString() );
        assertTrue( attr4.contains( "a" ) );
        assertTrue( attr4.contains( "b" ) );
        assertTrue( attr4.contains( "c" ) );
        assertTrue( attr4.contains( "d" ) );
        assertTrue( attr4.contains( "e" ) );
        assertFalse( attr4.contains( "ab" ) );

        Attribute attr5 = new DefaultAttribute( atEMail );

        nbAdded = attr5.add( "a", "b", ( String ) null, "d" );
        assertEquals( 4, nbAdded );
        assertTrue( attr5.isHumanReadable() );
        assertTrue( attr5.contains( "a" ) );
        assertTrue( attr5.contains( "b" ) );
        assertTrue( attr5.contains( NULL_STRING_VALUE ) );
        assertTrue( attr5.contains( "d" ) );

        Attribute attr6 = new DefaultAttribute( atPwd );

        nbAdded = attr6.add( "a", ( String ) null );
        assertEquals( 2, nbAdded );
        assertFalse( attr6.isHumanReadable() );
    }


    /**
     * Test method add( byte[]... )
     */
    @Test
    public void testAddByteArray() throws LdapInvalidAttributeValueException
    {
        Attribute attr1 = new DefaultAttribute( atPwd );

        int nbAdded = attr1.add( ( byte[] ) null );
        assertEquals( 1, nbAdded );
        assertFalse( attr1.isHumanReadable() );
        assertTrue( Arrays.equals( NULL_BINARY_VALUE.getBytes(), attr1.getBytes() ) );

        Attribute attr2 = new DefaultAttribute( atPwd );

        nbAdded = attr2.add( StringConstants.EMPTY_BYTES );
        assertEquals( 1, nbAdded );
        assertFalse( attr2.isHumanReadable() );
        assertTrue( Arrays.equals( StringConstants.EMPTY_BYTES, attr2.getBytes() ) );

        Attribute attr3 = new DefaultAttribute( atPwd );

        nbAdded = attr3.add( BYTES1 );
        assertEquals( 1, nbAdded );
        assertFalse( attr3.isHumanReadable() );
        assertTrue( Arrays.equals( BYTES1, attr3.getBytes() ) );

        Attribute attr4 = new DefaultAttribute( atPwd );

        nbAdded = attr4.add( BYTES1, BYTES2, BYTES3, BYTES4 );
        assertEquals( 4, nbAdded );
        assertFalse( attr4.isHumanReadable() );
        assertTrue( attr4.contains( BYTES1 ) );
        assertTrue( attr4.contains( BYTES2 ) );
        assertTrue( attr4.contains( BYTES3 ) );
        assertTrue( attr4.contains( BYTES4 ) );

        Attribute attr5 = new DefaultAttribute( atPwd );

        nbAdded = attr5.add( BYTES1, BYTES2, ( byte[] ) null, BYTES3 );
        assertEquals( 4, nbAdded );
        assertFalse( attr5.isHumanReadable() );
        assertTrue( attr5.contains( BYTES1 ) );
        assertTrue( attr5.contains( BYTES2 ) );
        assertTrue( attr5.contains( ( byte[] ) null ) );
        assertTrue( attr5.contains( BYTES3 ) );

        Attribute attr6 = new DefaultAttribute( atPwd );

        nbAdded = attr6.add( "ab", ( String ) null );
        assertEquals( 2, nbAdded );
        assertFalse( attr6.isHumanReadable() );
    }


    /**
     * Test method clear()
     */
    @Test
    public void testClear() throws LdapException
    {
        Attribute attr = new DefaultAttribute( "email", atEMail );

        assertEquals( 0, attr.size() );

        attr.add( ( String ) null, "a", "b" );
        assertEquals( 3, attr.size() );

        attr.clear();
        assertTrue( attr.isHumanReadable() );
        assertEquals( 0, attr.size() );
        assertEquals( atEMail, attr.getAttributeType() );
    }


    /**
     * Test method contains( Value... ) throws LdapException
     */
    @Test
    public void testContainsValueArray() throws LdapException
    {
        Attribute attr1 = new DefaultAttribute( atEMail );

        assertEquals( 0, attr1.size() );
        assertFalse( attr1.contains( STR_VALUE1 ) );
        assertFalse( attr1.contains( NULL_STRING_VALUE ) );

        attr1.add( ( String ) null );
        assertEquals( 1, attr1.size() );
        assertTrue( attr1.contains( NULL_STRING_VALUE ) );

        attr1.remove( ( String ) null );
        assertFalse( attr1.contains( NULL_STRING_VALUE ) );
        assertEquals( 0, attr1.size() );

        attr1.add( "a", "b", "c" );
        assertEquals( 3, attr1.size() );
        assertTrue( attr1.contains( STR_VALUE1 ) );
        assertTrue( attr1.contains( STR_VALUE2 ) );
        assertTrue( attr1.contains( STR_VALUE3 ) );
        assertTrue( attr1.contains( STR_VALUE1, STR_VALUE3 ) );
        assertFalse( attr1.contains( STR_VALUE4 ) );
        assertFalse( attr1.contains( NULL_STRING_VALUE ) );

        Attribute attr2 = new DefaultAttribute( atPwd );
        assertEquals( 0, attr2.size() );
        assertFalse( attr2.contains( BYTES1 ) );
        assertFalse( attr2.contains( NULL_BINARY_VALUE ) );

        attr2.add( ( byte[] ) null );
        assertEquals( 1, attr2.size() );
        assertTrue( attr2.contains( NULL_BINARY_VALUE ) );

        attr2.remove( ( byte[] ) null );
        assertFalse( attr2.contains( NULL_BINARY_VALUE ) );
        assertEquals( 0, attr2.size() );

        attr2.add( BYTES1, BYTES2, BYTES3 );
        assertEquals( 3, attr2.size() );
        assertTrue( attr2.contains( BIN_VALUE1 ) );
        assertTrue( attr2.contains( BIN_VALUE2 ) );
        assertTrue( attr2.contains( BIN_VALUE3 ) );
        assertFalse( attr2.contains( NULL_BINARY_VALUE ) );
    }


    /**
     * Test method contains( String... )
     */
    @Test
    public void testContainsStringArray() throws LdapException
    {
        Attribute attr1 = new DefaultAttribute( atEMail );

        assertEquals( 0, attr1.size() );
        assertFalse( attr1.contains( "a" ) );
        assertFalse( attr1.contains( ( String ) null ) );

        attr1.add( ( String ) null );
        assertEquals( 1, attr1.size() );
        assertTrue( attr1.contains( ( String ) null ) );

        attr1.remove( ( String ) null );
        assertFalse( attr1.contains( ( String ) null ) );
        assertEquals( 0, attr1.size() );

        attr1.add( "a", "b", "c" );
        assertEquals( 3, attr1.size() );
        assertTrue( attr1.contains( "a" ) );
        assertTrue( attr1.contains( "b" ) );
        assertTrue( attr1.contains( "c" ) );
        assertFalse( attr1.contains( "e" ) );
        assertFalse( attr1.contains( ( String ) null ) );
    }


    /**
     * Test method contains( byte[]... )
     */
    @Test
    public void testContainsByteArray() throws LdapException
    {
        Attribute attr1 = new DefaultAttribute( atPwd );

        assertEquals( 0, attr1.size() );
        assertFalse( attr1.contains( BYTES1 ) );
        assertFalse( attr1.contains( ( byte[] ) null ) );

        attr1.add( ( byte[] ) null );
        assertEquals( 1, attr1.size() );
        assertTrue( attr1.contains( ( byte[] ) null ) );

        attr1.remove( ( byte[] ) null );
        assertFalse( attr1.contains( ( byte[] ) null ) );
        assertEquals( 0, attr1.size() );

        attr1.add( BYTES1, BYTES2, BYTES3 );
        assertEquals( 3, attr1.size() );
        assertTrue( attr1.contains( BYTES1 ) );
        assertTrue( attr1.contains( BYTES2 ) );
        assertTrue( attr1.contains( BYTES3 ) );
        assertFalse( attr1.contains( BYTES4 ) );
        assertFalse( attr1.contains( ( byte[] ) null ) );
    }


    /**
     * Test method testEquals()
     */
    @Test
    public void testEquals() throws LdapException
    {
        Attribute attr1 = new DefaultAttribute( atCN );

        assertFalse( attr1.equals( null ) );

        Attribute attr2 = new DefaultAttribute( atCN );

        assertTrue( attr1.equals( attr2 ) );

        attr2.setUpId( "CN" );
        assertTrue( attr1.equals( attr2 ) );

        attr1.setUpId( "CommonName" );
        assertTrue( attr1.equals( attr2 ) );

        attr1.setUpId( "CN" );
        assertTrue( attr1.equals( attr2 ) );

        attr1.add( "a", "b", "c" );
        attr2.add( "c", "b", "a" );
        assertTrue( attr1.equals( attr2 ) );

        assertTrue( attr1.equals( attr2 ) );

        Attribute attr3 = new DefaultAttribute( atPwd );
        Attribute attr4 = new DefaultAttribute( atPwd );

        attr3.add( NULL_BINARY_VALUE );
        attr4.add( NULL_BINARY_VALUE );
        assertTrue( attr3.equals( attr4 ) );

        Attribute attr5 = new DefaultAttribute( atPwd );
        Attribute attr6 = new DefaultAttribute( atDC );
        assertFalse( attr5.equals( attr6 ) );

        attr5.add( NULL_BINARY_VALUE );
        attr6.add( NULL_STRING_VALUE );
        assertFalse( attr5.equals( attr6 ) );

        Attribute attr7 = new DefaultAttribute( atCN );
        Attribute attr8 = new DefaultAttribute( atPwd );

        attr7.add( "a" );
        attr8.add( BYTES2 );
        assertFalse( attr7.equals( attr8 ) );

        Attribute attr9 = new DefaultAttribute( atCN );
        Attribute attr10 = new DefaultAttribute( atPwd );

        attr9.add( "a" );
        attr9.add( BYTES2 );
        attr10.add( "a", "b" );
        assertFalse( attr9.equals( attr10 ) );
    }


    /**
     * Test method get()
     */
    @Test
    public void testGet() throws LdapException
    {
        Attribute attr1 = new DefaultAttribute( "dc", atDC );

        attr1.add( ( String ) null );
        assertEquals( NULL_STRING_VALUE, attr1.get() );

        Attribute attr2 = new DefaultAttribute( "email", atEMail );

        attr2.add( "a", "b", "c" );
        assertEquals( "a", attr2.get().getString() );

        attr2.remove( "a" );
        assertEquals( "b", attr2.get().getString() );

        attr2.remove( "b" );
        assertEquals( "c", attr2.get().getString() );

        attr2.remove( "c" );
        assertNull( attr2.get() );

        Attribute attr3 = new DefaultAttribute( "userPassword", atPwd );

        attr3.add( BYTES1, BYTES2, BYTES3 );
        assertTrue( Arrays.equals( BYTES1, attr3.get().getBytes() ) );

        attr3.remove( BYTES1 );
        assertTrue( Arrays.equals( BYTES2, attr3.get().getBytes() ) );

        attr3.remove( BYTES2 );
        assertTrue( Arrays.equals( BYTES3, attr3.get().getBytes() ) );

        attr3.remove( BYTES3 );
        assertNull( attr2.get() );
    }


    /**
     * Test method getAll()
     */
    @Test
    public void testGetAll() throws LdapException
    {
        Attribute attr = new DefaultAttribute( atEMail );

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

        attr.add( "a", "b", "c" );
        iterator = attr.getAll();
        assertTrue( iterator.hasNext() );
        assertEquals( "a", iterator.next().getString() );
        assertEquals( "b", iterator.next().getString() );
        assertEquals( "c", iterator.next().getString() );
        assertFalse( iterator.hasNext() );
    }


    /**
     * Test method size()
     */
    @Test
    public void testSize() throws Exception
    {
        Attribute attr1 = new DefaultAttribute( atDC );

        assertEquals( 0, attr1.size() );

        attr1.add( ( String ) null );
        assertEquals( 1, attr1.size() );

        Attribute attr2 = new DefaultAttribute( atCN );

        attr2.add( "a", "b" );
        assertEquals( 2, attr2.size() );

        attr2.clear();
        assertEquals( 0, attr2.size() );

        Attribute attr3 = new DefaultAttribute( atC );

        attr3.add( "US" );
        assertEquals( 1, attr3.size() );

        // TODO : forbid addition of more than 1 value for SINGLE-VALUE attributes
        attr3.add( "FR" );
        assertEquals( 1, attr3.size() );
    }


    /**
     * Test method put( byte[]... )
     */
    @Test
    public void testPutByteArray() throws InvalidAttributeValueException, Exception
    {
        Attribute attr1 = new DefaultAttribute( atPwd );

        int nbAdded = attr1.add( ( byte[] ) null );
        assertEquals( 1, nbAdded );
        assertFalse( attr1.isHumanReadable() );
        assertTrue( Arrays.equals( NULL_BINARY_VALUE.getBytes(), attr1.getBytes() ) );

        Attribute attr2 = new DefaultAttribute( atPwd );

        nbAdded = attr2.add( StringConstants.EMPTY_BYTES );
        assertEquals( 1, nbAdded );
        assertFalse( attr2.isHumanReadable() );
        assertTrue( Arrays.equals( StringConstants.EMPTY_BYTES, attr2.getBytes() ) );

        Attribute attr3 = new DefaultAttribute( atPwd );

        nbAdded = attr3.add( BYTES1 );
        assertEquals( 1, nbAdded );
        assertFalse( attr3.isHumanReadable() );
        assertTrue( Arrays.equals( BYTES1, attr3.getBytes() ) );

        Attribute attr4 = new DefaultAttribute( atPwd );

        nbAdded = attr4.add( BYTES1, BYTES2 );
        assertEquals( 2, nbAdded );
        assertFalse( attr4.isHumanReadable() );
        assertTrue( attr4.contains( BYTES1 ) );
        assertTrue( attr4.contains( BYTES2 ) );

        attr4.clear();
        nbAdded = attr4.add( BYTES3, BYTES4 );
        assertEquals( 2, nbAdded );
        assertFalse( attr4.isHumanReadable() );
        assertTrue( attr4.contains( BYTES3 ) );
        assertTrue( attr4.contains( BYTES4 ) );

        Attribute attr5 = new DefaultAttribute( atPwd );

        nbAdded = attr5.add( BYTES1, BYTES2, ( byte[] ) null, BYTES3 );
        assertEquals( 4, nbAdded );
        assertFalse( attr5.isHumanReadable() );
        assertTrue( attr5.contains( BYTES1 ) );
        assertTrue( attr5.contains( BYTES2 ) );
        assertTrue( attr5.contains( ( byte[] ) null ) );
        assertTrue( attr5.contains( BYTES3 ) );

        Attribute attr6 = new DefaultAttribute( atPwd );

        assertFalse( attr6.isHumanReadable() );
        nbAdded = attr6.add( BYTES1, ( byte[] ) null );
        assertEquals( 2, nbAdded );
        assertTrue( attr6.contains( BYTES1 ) );
        assertTrue( attr6.contains( ( byte[] ) null ) );
    }


    /**
     * Test method put( String... )
     */
    @Test
    public void testPutStringArray() throws LdapInvalidAttributeValueException
    {
        Attribute attr1 = new DefaultAttribute( atDC );

        int nbAdded = attr1.add( ( String ) null );
        assertEquals( 1, nbAdded );
        assertTrue( attr1.isHumanReadable() );
        assertEquals( NULL_STRING_VALUE, attr1.get() );

        Attribute attr2 = new DefaultAttribute( atDC );

        nbAdded = attr2.add( "" );
        assertEquals( 1, nbAdded );
        assertTrue( attr2.isHumanReadable() );
        assertEquals( "", attr2.getString() );

        Attribute attr3 = new DefaultAttribute( atDC );

        nbAdded = attr3.add( "t" );
        assertEquals( 1, nbAdded );
        assertTrue( attr3.isHumanReadable() );
        assertEquals( "t", attr3.getString() );

        Attribute attr4 = new DefaultAttribute( atEMail );

        nbAdded = attr4.add( "a", "b", "c", "d" );
        assertEquals( 4, nbAdded );
        assertTrue( attr4.isHumanReadable() );
        assertEquals( "a", attr4.getString() );
        assertTrue( attr4.contains( "a" ) );
        assertTrue( attr4.contains( "b" ) );
        assertTrue( attr4.contains( "c" ) );
        assertTrue( attr4.contains( "d" ) );

        attr4.clear();
        nbAdded = attr4.add( "e" );
        assertEquals( 1, nbAdded );
        assertTrue( attr4.isHumanReadable() );
        assertEquals( "e", attr4.getString() );
        assertFalse( attr4.contains( "a" ) );
        assertFalse( attr4.contains( "b" ) );
        assertFalse( attr4.contains( "c" ) );
        assertFalse( attr4.contains( "d" ) );
        assertTrue( attr4.contains( "e" ) );

        attr4.clear();
        nbAdded = attr4.add( BYTES1 );
        assertEquals( 0, nbAdded );
        assertTrue( attr4.isHumanReadable() );

        Attribute attr5 = new DefaultAttribute( atEMail );

        nbAdded = attr5.add( "a", "b", ( String ) null, "d" );
        assertEquals( 4, nbAdded );
        assertTrue( attr5.isHumanReadable() );
        assertTrue( attr5.contains( "a" ) );
        assertTrue( attr5.contains( "b" ) );
        assertTrue( attr5.contains( NULL_STRING_VALUE ) );
        assertTrue( attr5.contains( "d" ) );

        Attribute attr6 = new DefaultAttribute( atPwd );

        nbAdded = attr6.add( "a", ( String ) null );
        assertEquals( 2, nbAdded );
        assertFalse( attr6.isHumanReadable() );
    }


    /**
     * Test method put( Value... )
     */
    @Test
    public void testPutValueArray() throws Exception
    {
        Attribute attr1 = new DefaultAttribute( atDC );

        assertEquals( 0, attr1.size() );

        attr1.add( NULL_STRING_VALUE );
        assertEquals( 1, attr1.size() );
        assertTrue( attr1.contains( NULL_STRING_VALUE ) );

        attr1.clear();
        attr1.add( STR_VALUE1, STR_VALUE2, STR_VALUE3 );
        assertEquals( 3, attr1.size() );
        assertTrue( attr1.contains( STR_VALUE1 ) );
        assertTrue( attr1.contains( STR_VALUE2 ) );
        assertTrue( attr1.contains( STR_VALUE3 ) );

        attr1.clear();
        attr1.add( STR_VALUE1, NULL_STRING_VALUE, STR_VALUE3 );
        assertEquals( 3, attr1.size() );
        assertTrue( attr1.contains( STR_VALUE1 ) );
        assertTrue( attr1.contains( NULL_STRING_VALUE ) );
        assertTrue( attr1.contains( STR_VALUE3 ) );

        attr1.clear();
        attr1.add( STR_VALUE1, NULL_STRING_VALUE, BIN_VALUE3 );
        assertEquals( 2, attr1.size() );
        assertTrue( attr1.contains( STR_VALUE1 ) );
        assertTrue( attr1.contains( NULL_STRING_VALUE ) );
        assertFalse( attr1.contains( STR_VALUE3 ) );

        Attribute attr2 = new DefaultAttribute( atPwd );
        assertEquals( 0, attr2.size() );

        attr2.add( NULL_BINARY_VALUE );
        assertEquals( 1, attr2.size() );
        assertTrue( attr2.contains( NULL_BINARY_VALUE ) );

        attr2.clear();
        attr2.add( BIN_VALUE1, BIN_VALUE2, BIN_VALUE3 );
        assertEquals( 3, attr2.size() );
        assertTrue( attr2.contains( BIN_VALUE1 ) );
        assertTrue( attr2.contains( BIN_VALUE2 ) );
        assertTrue( attr2.contains( BIN_VALUE3 ) );

        attr2.clear();
        attr2.add( BIN_VALUE1, NULL_BINARY_VALUE, STR_VALUE3 );
        assertEquals( 2, attr2.size() );
        assertTrue( attr2.contains( BIN_VALUE1 ) );
        assertTrue( attr2.contains( NULL_BINARY_VALUE ) );
        assertFalse( attr2.contains( BIN_VALUE3 ) );
    }


    /**
     * Test method remove( Value... )
     */
    @Test
    public void testRemoveValueArray() throws Exception
    {
        Attribute attr1 = new DefaultAttribute( atEMail );

        assertFalse( attr1.remove( STR_VALUE1 ) );

        attr1.add( "a", "b", "c" );
        assertTrue( attr1.remove( STR_VALUE1 ) );
        assertEquals( 2, attr1.size() );

        assertTrue( attr1.remove( STR_VALUE2, STR_VALUE3 ) );
        assertEquals( 0, attr1.size() );

        assertFalse( attr1.remove( STR_VALUE4 ) );

        attr1.clear();
        attr1.add( "a", "b", "c" );
        assertFalse( attr1.remove( STR_VALUE2, STR_VALUE4 ) );
        assertEquals( 2, attr1.size() );

        attr1.clear();
        attr1.add( "a", ( String ) null, "b" );
        assertTrue( attr1.remove( NULL_STRING_VALUE, STR_VALUE1 ) );
        assertEquals( 1, attr1.size() );

        attr1.clear();
        attr1.add( "a", ( String ) null, "b" );
        attr1.add( BYTES3 );
        assertFalse( attr1.remove( NULL_STRING_VALUE, STR_VALUE1, BIN_VALUE3 ) );
        assertEquals( 1, attr1.size() );

        Attribute attr2 = new DefaultAttribute( atPwd );

        assertFalse( attr2.remove( BIN_VALUE1 ) );

        attr2.add( BYTES1, BYTES2, BYTES3 );
        assertTrue( attr2.remove( BIN_VALUE1 ) );
        assertEquals( 2, attr2.size() );

        assertTrue( attr2.remove( BIN_VALUE2, BIN_VALUE3 ) );
        assertEquals( 0, attr2.size() );

        assertFalse( attr2.remove( BIN_VALUE4 ) );

        attr2.clear();
        attr2.add( BYTES1, BYTES2, BYTES3 );
        assertFalse( attr2.remove( BIN_VALUE2, STR_VALUE4 ) );
        assertEquals( 2, attr2.size() );

        attr2.clear();
        attr2.add( BYTES1, ( byte[] ) null, BYTES3 );
        assertFalse( attr2.remove( NULL_STRING_VALUE, BIN_VALUE1 ) );
        assertEquals( 2, attr2.size() );

        attr2.clear();
        attr2.add( BYTES1, ( byte[] ) null, BYTES2 );
        attr2.add( "c" );
        assertEquals( 4, attr2.size() );
        assertFalse( attr2.remove( NULL_STRING_VALUE, BIN_VALUE1, STR_VALUE3 ) );
        assertEquals( 3, attr2.size() );
    }


    /**
     * Test method remove( byte... )
     */
    @Test
    public void testRemoveByteArray() throws Exception
    {
        Attribute attr1 = new DefaultAttribute( atPwd );

        assertFalse( attr1.remove( BYTES1 ) );

        attr1.add( BYTES1, BYTES2, BYTES3 );
        assertTrue( attr1.remove( BYTES1 ) );
        assertEquals( 2, attr1.size() );

        assertTrue( attr1.remove( BYTES2, BYTES3 ) );
        assertEquals( 0, attr1.size() );

        assertFalse( attr1.remove( BYTES4 ) );

        attr1.clear();
        attr1.add( BYTES1, BYTES2, BYTES3 );
        assertFalse( attr1.remove( BYTES3, BYTES4 ) );
        assertEquals( 2, attr1.size() );

        attr1.clear();
        attr1.add( BYTES1, ( byte[] ) null, BYTES2 );
        assertTrue( attr1.remove( ( byte[] ) null, BYTES1 ) );
        assertEquals( 1, attr1.size() );
    }


    /**
     * Test method remove( String... )
     */
    @Test
    public void testRemoveStringArray() throws Exception
    {
        Attribute attr1 = new DefaultAttribute( atEMail );

        assertFalse( attr1.remove( "a" ) );

        attr1.add( "a", "b", "c" );
        assertTrue( attr1.remove( "a" ) );
        assertEquals( 2, attr1.size() );

        assertTrue( attr1.remove( "b", "c" ) );
        assertEquals( 0, attr1.size() );

        assertFalse( attr1.remove( "d" ) );

        attr1.clear();
        attr1.add( "a", "b", "c" );
        assertFalse( attr1.remove( "b", "e" ) );
        assertEquals( 2, attr1.size() );

        attr1.clear();
        attr1.add( "a", ( String ) null, "b" );
        assertTrue( attr1.remove( ( String ) null, "a" ) );
        assertEquals( 1, attr1.size() );

        Attribute attr2 = new DefaultAttribute( "test" );

        attr2.add( BYTES1, BYTES2, BYTES3 );

        assertFalse( attr2.remove( ( String ) null ) );
        assertTrue( attr2.remove( "ab", "c" ) );
        assertFalse( attr2.remove( "d" ) );
    }


    /**
     * Test method iterator()
     */
    @Test
    public void testIterator() throws LdapException
    {
        Attribute attr1 = new DefaultAttribute( atCN );
        attr1.add( "a", "b", "c" );

        Iterator<Value<?>> iter = attr1.iterator();

        assertTrue( iter.hasNext() );

        String[] values = new String[]
            { "a", "b", "c" };
        int pos = 0;

        for ( Value<?> val : attr1 )
        {
            assertTrue( val instanceof StringValue );
            assertEquals( values[pos++], val.getString() );
        }
    }


    /**
     * Test method toString
     */
    @Test
    public void testToString() throws LdapException
    {
        Attribute attr = new DefaultAttribute( atEMail );

        assertEquals( "    email: (null)\n", attr.toString() );

        attr.setUpId( "EMail" );
        assertEquals( "    EMail: (null)\n", attr.toString() );

        attr.add( ( String ) null );
        assertEquals( "    EMail: ''\n", attr.toString() );

        attr.clear();
        attr.add( "a", "b" );
        assertEquals( "    EMail: a\n    EMail: b\n", attr.toString() );
    }


    /**
     * Test method instanceOf()
     */
    @Test
    public void testInstanceOf() throws Exception
    {
        Attribute attr = new DefaultAttribute( atCN );

        assertTrue( attr.isInstanceOf( atCN ) );
        assertTrue( attr.isInstanceOf( atName) );
        assertFalse( attr.isInstanceOf( atSN ) );
    }


    /**
     * Test method setUpId( String, AttributeType )
     */
    @Test
    public void testSetUpIdStringAttributeType() throws Exception
    {
        Attribute attr = new DefaultAttribute( atSN );

        attr.setUpId( null, atCN );
        assertEquals( "2.5.4.3", attr.getId() );
        assertEquals( "cn", attr.getUpId() );
        assertEquals( atCN, attr.getAttributeType() );

        attr.setUpId( "  ", atCN );
        assertEquals( "2.5.4.3", attr.getId() );
        assertEquals( "cn", attr.getUpId() );
        assertEquals( atCN, attr.getAttributeType() );

        attr.setUpId( "  CN  ", atCN );
        assertEquals( "2.5.4.3", attr.getId() );
        assertEquals( "  CN  ", attr.getUpId() );
        assertEquals( atCN, attr.getAttributeType() );

        attr.setUpId( "  CommonName  ", atCN );
        assertEquals( "2.5.4.3", attr.getId() );
        assertEquals( "  CommonName  ", attr.getUpId() );
        assertEquals( atCN, attr.getAttributeType() );

        attr.setUpId( "  2.5.4.3  ", atCN );
        assertEquals( "2.5.4.3", attr.getId() );
        assertEquals( "  2.5.4.3  ", attr.getUpId() );
        assertEquals( atCN, attr.getAttributeType() );

        // Check with wrong IDs
        try
        {
            attr.setUpId( "sn", atCN );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        try
        {
            attr.setUpId( "  2.5.4.4  ", atCN );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }
    }


    /**
     * Test method setUpId( String ) inherited from ClientAttribute
     */
    @Test
    public void testSetUpIdString() throws Exception
    {
        Attribute attr = new DefaultAttribute( atCN );

        attr.setUpId( "cn" );
        assertEquals( "2.5.4.3", attr.getId() );
        assertEquals( "cn", attr.getUpId() );
        assertEquals( atCN, attr.getAttributeType() );

        attr.setUpId( "  CN  " );
        assertEquals( "2.5.4.3", attr.getId() );
        assertEquals( "  CN  ", attr.getUpId() );
        assertEquals( atCN, attr.getAttributeType() );

        attr.setUpId( "  CommonName  " );
        assertEquals( "2.5.4.3", attr.getId() );
        assertEquals( "  CommonName  ", attr.getUpId() );
        assertEquals( atCN, attr.getAttributeType() );

        attr.setUpId( "  2.5.4.3  " );
        assertEquals( "  2.5.4.3  ", attr.getUpId() );
        assertEquals( "2.5.4.3", attr.getId() );
        assertEquals( atCN, attr.getAttributeType() );

        // Now check wrong IDs
        attr = new DefaultAttribute( atCN );
        
        try
        {
            attr.setUpId( "sn" );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            // Expected
        }
        
        assertEquals( "2.5.4.3", attr.getId() );
        assertEquals( "cn", attr.getUpId() );
        assertEquals( atCN, attr.getAttributeType() );

        try
        {
            attr.setUpId( "  SN  " );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            // Expected
        }
    
        assertEquals( "2.5.4.3", attr.getId() );
        assertEquals( "cn", attr.getUpId() );
        assertEquals( atCN, attr.getAttributeType() );

        try
        {
            attr.setUpId( "  surname  " );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            // Expected
        }
        
        assertEquals( "2.5.4.3", attr.getId() );
        assertEquals( "cn", attr.getUpId() );
        assertEquals( atCN, attr.getAttributeType() );

        try
        {
            attr.setUpId( "  2.5.4.4  " );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            // Expected
        }

        assertEquals( "2.5.4.3", attr.getId() );
        assertEquals( "cn", attr.getUpId() );
        assertEquals( atCN, attr.getAttributeType() );
    }


    /**
     * Test method setAttributeType( AttributeType )
     */
    @Test
    public void testSetAttributeType() throws Exception
    {
        Attribute attr = new DefaultAttribute( atCN );

        try
        {
            attr.apply( null );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        attr.apply( atSN );

        assertTrue( attr.isInstanceOf( atSN ) );
        assertEquals( "2.5.4.4", attr.getId() );
        assertEquals( "sn", attr.getUpId() );
    }


    /**
     * Test method getAttributeType()
     */
    @Test
    public void testGetAttributeType() throws Exception
    {
        Attribute attr = new DefaultAttribute( atSN );
        assertEquals( atSN, attr.getAttributeType() );
    }


    /**
     * Test constructor DefaultEntryAttribute( AttributeType )
     */
    @Test
    public void testDefaultServerAttributeAttributeType()
    {
        Attribute attr = new DefaultAttribute( atCN );

        assertTrue( attr.isHumanReadable() );
        assertEquals( 0, attr.size() );
        assertEquals( "2.5.4.3", attr.getId() );
        assertEquals( "cn", attr.getUpId() );
        assertEquals( atCN, attr.getAttributeType() );
    }


    /**
     * Test constructor DefaultEntryAttribute( String, AttributeType )
     */
    @Test
    public void testDefaultServerAttributeStringAttributeType()
    {
        Attribute attr1 = new DefaultAttribute( "cn", atCN );

        assertTrue( attr1.isHumanReadable() );
        assertEquals( 0, attr1.size() );
        assertEquals( "2.5.4.3", attr1.getId() );
        assertEquals( "cn", attr1.getUpId() );
        assertEquals( atCN, attr1.getAttributeType() );

        Attribute attr2 = new DefaultAttribute( "  CommonName  ", atCN );

        assertTrue( attr2.isHumanReadable() );
        assertEquals( 0, attr2.size() );
        assertEquals( "2.5.4.3", attr2.getId() );
        assertEquals( "  CommonName  ", attr2.getUpId() );
        assertEquals( atCN, attr2.getAttributeType() );

        Attribute attr3 = new DefaultAttribute( "  ", atCN );

        assertTrue( attr3.isHumanReadable() );
        assertEquals( 0, attr3.size() );
        assertEquals( "2.5.4.3", attr3.getId() );
        assertEquals( "cn", attr3.getUpId() );
        assertEquals( atCN, attr3.getAttributeType() );
    }


    /**
     * Test constructor DefaultEntryAttribute( AttributeType, Value... )
     */
    @Test
    public void testDefaultServerAttributeAttributeTypeValueArray() throws Exception
    {
        Attribute attr1 = new DefaultAttribute( atDC, STR_VALUE1, STR_VALUE2, NULL_STRING_VALUE );

        assertTrue( attr1.isHumanReadable() );
        assertEquals( 3, attr1.size() );
        assertEquals( "0.9.2342.19200300.100.1.25", attr1.getId() );
        assertEquals( "dc", attr1.getUpId() );
        assertEquals( atDC, attr1.getAttributeType() );
        assertTrue( attr1.contains( "a", "b" ) );
        assertTrue( attr1.contains( NULL_STRING_VALUE ) );

        Attribute attr2 = new DefaultAttribute( atDC, STR_VALUE1, BIN_VALUE2, NULL_STRING_VALUE );

        assertTrue( attr2.isHumanReadable() );
        assertEquals( 2, attr2.size() );
        assertEquals( "0.9.2342.19200300.100.1.25", attr2.getId() );
        assertEquals( "dc", attr2.getUpId() );
        assertEquals( atDC, attr2.getAttributeType() );
        assertTrue( attr2.contains( "a" ) );
        assertTrue( attr2.contains( NULL_STRING_VALUE ) );
    }


    /**
     * Test constructor DefaultEntryAttribute( String, AttributeType, Value... )
     */
    @Test
    public void testDefaultServerAttributeStringAttributeTypeValueArray() throws LdapException
    {
        Attribute attr1 = new DefaultAttribute( "dc", atDC, STR_VALUE1, STR_VALUE2, NULL_STRING_VALUE );

        assertTrue( attr1.isHumanReadable() );
        assertEquals( 3, attr1.size() );
        assertEquals( "0.9.2342.19200300.100.1.25", attr1.getId() );
        assertEquals( "dc", attr1.getUpId() );
        assertEquals( atDC, attr1.getAttributeType() );
        assertTrue( attr1.contains( "a", "b" ) );
        assertTrue( attr1.contains( NULL_STRING_VALUE ) );

        Attribute attr2 = new DefaultAttribute( atDC, STR_VALUE1, BIN_VALUE2, NULL_STRING_VALUE );

        assertTrue( attr2.isHumanReadable() );
        assertEquals( 2, attr2.size() );
        assertEquals( "0.9.2342.19200300.100.1.25", attr2.getId() );
        assertEquals( "dc", attr2.getUpId() );
        assertEquals( atDC, attr2.getAttributeType() );
        assertTrue( attr2.contains( "a" ) );
        assertTrue( attr2.contains( NULL_STRING_VALUE ) );

        Attribute attr3 = new DefaultAttribute( "DomainComponent", atDC, STR_VALUE1, STR_VALUE2,
            NULL_STRING_VALUE );

        assertTrue( attr3.isHumanReadable() );
        assertEquals( 3, attr3.size() );
        assertEquals( "0.9.2342.19200300.100.1.25", attr3.getId() );
        assertEquals( "DomainComponent", attr3.getUpId() );
        assertEquals( atDC, attr3.getAttributeType() );
        assertTrue( attr3.contains( "a", "b" ) );
        assertTrue( attr3.contains( NULL_STRING_VALUE ) );

        Attribute attr4 = new DefaultAttribute( " 0.9.2342.19200300.100.1.25 ", atDC, STR_VALUE1, STR_VALUE2,
            NULL_STRING_VALUE );

        assertTrue( attr4.isHumanReadable() );
        assertEquals( 3, attr4.size() );
        assertEquals( "0.9.2342.19200300.100.1.25", attr4.getId() );
        assertEquals( " 0.9.2342.19200300.100.1.25 ", attr4.getUpId() );
        assertEquals( atDC, attr4.getAttributeType() );
        assertTrue( attr4.contains( "a", "b" ) );
        assertTrue( attr4.contains( NULL_STRING_VALUE ) );
    }


    /**
     * Test constructor DefaultEntryAttribute( AttributeType, String... ) 
     */
    @Test
    public void testDefaultServerAttributeAttributeTypeStringArray() throws LdapException
    {
        Attribute attr1 = new DefaultAttribute( atEMail, "a", "b", ( String ) null );

        assertTrue( attr1.isHumanReadable() );
        assertEquals( 3, attr1.size() );
        assertEquals( "1.2.840.113549.1.9.1", attr1.getId() );
        assertEquals( "email", attr1.getUpId() );
        assertEquals( atEMail, attr1.getAttributeType() );
        assertTrue( attr1.contains( "a", "b" ) );
        assertTrue( attr1.contains( NULL_STRING_VALUE ) );

        Attribute attr2 = new DefaultAttribute( atEMail, STR_VALUE1, BIN_VALUE2, NULL_STRING_VALUE );

        assertTrue( attr2.isHumanReadable() );
        assertEquals( 2, attr2.size() );
        assertEquals( "1.2.840.113549.1.9.1", attr2.getId() );
        assertEquals( "email", attr2.getUpId() );
        assertEquals( atEMail, attr2.getAttributeType() );
        assertTrue( attr2.contains( "a" ) );
        assertTrue( attr2.contains( NULL_STRING_VALUE ) );
    }


    /**
     * Test constructor DefaultEntryAttribute( String, AttributeType, String... )
     */
    @Test
    public void testDefaultServerAttributeStringAttributeTypeStringArray() throws LdapException
    {
        Attribute attr1 = new DefaultAttribute( "email", atEMail, "a", "b", ( String ) null );

        assertTrue( attr1.isHumanReadable() );
        assertEquals( 3, attr1.size() );
        assertEquals( "1.2.840.113549.1.9.1", attr1.getId() );
        assertEquals( "email", attr1.getUpId() );
        assertEquals( atEMail, attr1.getAttributeType() );
        assertTrue( attr1.contains( "a", "b" ) );
        assertTrue( attr1.contains( NULL_STRING_VALUE ) );

        Attribute attr2 = new DefaultAttribute( "EMail", atEMail, "a", "b", ( String ) null );

        assertTrue( attr2.isHumanReadable() );
        assertEquals( 3, attr2.size() );
        assertEquals( "1.2.840.113549.1.9.1", attr2.getId() );
        assertEquals( "EMail", attr2.getUpId() );
        assertEquals( atEMail, attr2.getAttributeType() );
        assertTrue( attr2.contains( "a", "b" ) );
        assertTrue( attr2.contains( NULL_STRING_VALUE ) );

        Attribute attr3 = new DefaultAttribute( " 1.2.840.113549.1.9.1 ", atEMail, "a", "b",
            ( String ) null );

        assertTrue( attr3.isHumanReadable() );
        assertEquals( 3, attr3.size() );
        assertEquals( "1.2.840.113549.1.9.1", attr3.getId() );
        assertEquals( " 1.2.840.113549.1.9.1 ", attr3.getUpId() );
        assertEquals( atEMail, attr3.getAttributeType() );
        assertTrue( attr3.contains( "a", "b" ) );
        assertTrue( attr3.contains( NULL_STRING_VALUE ) );
    }


    /**
     * Test method DefaultEntryAttribute( AttributeType, byte[]... ) 
     */
    @Test
    public void testDefaultServerAttributeAttributeTypeByteArray() throws LdapException
    {
        Attribute attr1 = new DefaultAttribute( atPwd, BYTES1, BYTES2, ( byte[] ) null );

        assertFalse( attr1.isHumanReadable() );
        assertEquals( 3, attr1.size() );
        assertEquals( "2.5.4.35", attr1.getId() );
        assertEquals( "userPassword", attr1.getUpId() );
        assertEquals( atPwd, attr1.getAttributeType() );
        assertTrue( attr1.contains( BYTES1, BYTES2 ) );
        assertTrue( attr1.contains( NULL_BINARY_VALUE ) );

        Attribute attr2 = new DefaultAttribute( atPwd, STR_VALUE1, BIN_VALUE2, NULL_BINARY_VALUE );

        assertFalse( attr2.isHumanReadable() );
        assertEquals( 2, attr2.size() );
        assertEquals( "2.5.4.35", attr2.getId() );
        assertEquals( "userPassword", attr2.getUpId() );
        assertEquals( atPwd, attr2.getAttributeType() );
        assertTrue( attr2.contains( BYTES2 ) );
        assertTrue( attr2.contains( NULL_BINARY_VALUE ) );
    }


    /**
     * Test method DefaultEntryAttribute( String, AttributeType, byte[]... )
     */
    @Test
    public void testDefaultServerAttributeStringAttributeTypeByteArray() throws LdapException
    {
        Attribute attr1 = new DefaultAttribute( "userPassword", atPwd, BYTES1, BYTES2, ( byte[] ) null );

        assertFalse( attr1.isHumanReadable() );
        assertEquals( 3, attr1.size() );
        assertEquals( "2.5.4.35", attr1.getId() );
        assertEquals( "userPassword", attr1.getUpId() );
        assertEquals( atPwd, attr1.getAttributeType() );
        assertTrue( attr1.contains( BYTES1, BYTES2 ) );
        assertTrue( attr1.contains( NULL_BINARY_VALUE ) );

        Attribute attr2 = new DefaultAttribute( "2.5.4.35", atPwd, STR_VALUE1, BIN_VALUE2, NULL_BINARY_VALUE );

        assertFalse( attr2.isHumanReadable() );
        assertEquals( 2, attr2.size() );
        assertEquals( "2.5.4.35", attr2.getId() );
        assertEquals( "2.5.4.35", attr2.getUpId() );
        assertEquals( atPwd, attr2.getAttributeType() );
        assertTrue( attr2.contains( BYTES2 ) );
        assertTrue( attr2.contains( NULL_BINARY_VALUE ) );
    }


    /**
     * Test method testClone()
     */
    @Test
    public void testClone() throws LdapException
    {
        Attribute attr = new DefaultAttribute( atDC );

        Attribute clone = attr.clone();

        assertEquals( attr, clone );
        attr.setUpId( "DomainComponent" );
        assertEquals( "0.9.2342.19200300.100.1.25", clone.getId() );

        attr.add( "a", ( String ) null, "b" );
        clone = attr.clone();
        assertEquals( attr, clone );

        attr.remove( "a" );
        assertNotSame( attr, clone );

        clone = attr.clone();
        assertEquals( attr, clone );
    }


    /**
     * Test the copy constructor of a EntryAttribute
     */
    @Test
    public void testCopyConstructorServerAttribute() throws LdapException
    {
        Attribute attribute = new DefaultAttribute( atCN );

        Attribute copy = new DefaultAttribute( atCN, attribute );

        assertEquals( copy, attribute );

        Attribute attribute2 = new DefaultAttribute( atCN, "test" );

        Attribute copy2 = new DefaultAttribute( atCN, attribute2 );

        assertEquals( copy2, attribute2 );
        attribute2.add( "test2" );
        assertNotSame( copy2, attribute2 );
        assertEquals( "test", copy2.getString() );
    }


    /**
     * Test the copy constructor of a ClientAttribute
     */
    @Test
    public void testCopyConstructorClientAttribute() throws LdapException
    {
        Attribute attribute = new DefaultAttribute( "commonName" );
        attribute.add( "test" );

        Attribute copy = new DefaultAttribute( atCN, attribute );

        assertEquals( atCN, copy.getAttributeType() );
        assertEquals( "test", copy.getString() );
        assertTrue( copy.isHumanReadable() );

        attribute.add( "test2" );
        assertFalse( copy.contains( "test2" ) );
    }


    /**
     * Test the conversion method 
     */
    @Test
    public void testToClientAttribute() throws LdapException
    {
        Attribute attribute = new DefaultAttribute( atCN, "test", "test2" );

        Attribute clientAttribute = attribute.clone();

        assertTrue( clientAttribute instanceof Attribute );

        assertTrue( clientAttribute.contains( "test", "test2" ) );
        assertEquals( "2.5.4.3", clientAttribute.getId() );

        attribute.remove( "test", "test2" );
        assertTrue( clientAttribute.contains( "test", "test2" ) );
    }


    /**
     * Test the serialization of a complete server attribute
     */
    @Test
    public void testSerializeCompleteAttribute() throws LdapException, IOException, ClassNotFoundException
    {
        DefaultAttribute dsa = new DefaultAttribute( atCN );
        dsa.setUpId( "CommonName" );
        dsa.add( "test1", "test2" );

        DefaultAttribute dsaSer = deserializeValue( serializeValue( dsa ), atCN );
        assertEquals( dsa.toString(), dsaSer.toString() );
        assertEquals( "2.5.4.3", dsaSer.getId() );
        assertEquals( "CommonName", dsaSer.getUpId() );
        assertEquals( "test1", dsaSer.getString() );
        assertTrue( dsaSer.contains( "test2", "test1" ) );
        assertTrue( dsaSer.isHumanReadable() );
    }


    /**
     * Test the serialization of a server attribute with no value
     */
    @Test
    public void testSerializeAttributeWithNoValue() throws LdapException, IOException, ClassNotFoundException
    {
        DefaultAttribute dsa = new DefaultAttribute( atCN );
        dsa.setUpId( "cn" );

        DefaultAttribute dsaSer = deserializeValue( serializeValue( dsa ), atCN );
        assertEquals( dsa.toString(), dsaSer.toString() );
        assertEquals( "2.5.4.3", dsaSer.getId() );
        assertEquals( "cn", dsaSer.getUpId() );
        assertEquals( 0, dsaSer.size() );
        assertTrue( dsaSer.isHumanReadable() );
    }


    /**
     * Test the serialization of a server attribute with a null value
     */
    @Test
    public void testSerializeAttributeNullValue() throws LdapException, IOException, ClassNotFoundException
    {
        DefaultAttribute dsa = new DefaultAttribute( atDC );
        dsa.setUpId( "DomainComponent" );
        dsa.add( ( String ) null );

        DefaultAttribute dsaSer = deserializeValue( serializeValue( dsa ), atDC );
        assertEquals( dsa.toString(), dsaSer.toString() );
        assertEquals( "0.9.2342.19200300.100.1.25", dsaSer.getId() );
        assertEquals( "DomainComponent", dsaSer.getUpId() );
        assertEquals( "", dsaSer.getString() );
        assertEquals( 1, dsaSer.size() );
        assertTrue( dsaSer.contains( ( String ) null ) );
        assertTrue( dsaSer.isHumanReadable() );
    }


    /**
     * Test the serialization of a server attribute with a binary value
     */
    @Test
    public void testSerializeAttributeBinaryValue() throws LdapException, IOException, ClassNotFoundException
    {
        DefaultAttribute dsa = new DefaultAttribute( atPwd );
        byte[] password = Strings.getBytesUtf8("secret");
        dsa.add( password );

        DefaultAttribute dsaSer = deserializeValue( serializeValue( dsa ), atPwd );
        assertEquals( dsa.toString(), dsaSer.toString() );
        assertEquals( "2.5.4.35", dsaSer.getId() );
        assertEquals( "userPassword", dsaSer.getUpId() );
        assertTrue( Arrays.equals( dsa.getBytes(), dsaSer.getBytes() ) );
        assertEquals( 1, dsaSer.size() );
        assertTrue( dsaSer.contains( password ) );
        assertFalse( dsaSer.isHumanReadable() );
    }
}
