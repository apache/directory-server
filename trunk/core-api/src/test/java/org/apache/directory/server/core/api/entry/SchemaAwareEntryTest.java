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
package org.apache.directory.server.core.api.entry;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.BinaryValue;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.StringValue;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.exception.LdapNoSuchAttributeException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.Strings;
import org.apache.directory.api.util.exception.Exceptions;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Test the DefaultEntry class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SchemaAwareEntryTest
{
    private final byte[] BYTES1 = new byte[]
        { 'a', 'b' };
    private final byte[] BYTES2 = new byte[]
        { 'b' };
    private final byte[] BYTES3 = new byte[]
        { 'c' };

    private static LdifSchemaLoader loader;
    private static SchemaManager schemaManager;

    private AttributeType atObjectClass;
    private AttributeType atCN;
    private AttributeType atDC;
    private AttributeType atSN;
    private AttributeType atC;
    private AttributeType atEMail;
    private AttributeType atL;
    private AttributeType atOC;

    // A Binary attribute
    private static AttributeType atPwd;

    private static Dn EXAMPLE_DN;


    /**
     * Initialize the registries once for the whole test suite
     */
    @BeforeClass
    public static void setup() throws Exception
    {
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = SchemaAwareEntryTest.class.getResource( "" ).getPath();
            int targetPos = path.indexOf( "target" );
            workingDirectory = path.substring( 0, targetPos + 6 );
        }

        File schemaRepository = new File( workingDirectory, "schema" );
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy( true );
        loader = new LdifSchemaLoader( schemaRepository );

        schemaManager = new DefaultSchemaManager( loader );
        schemaManager.loadAllEnabled();

        List<Throwable> errors = schemaManager.getErrors();

        if ( errors.size() != 0 )
        {
            fail( "Schema load failed : " + Exceptions.printErrors( errors ) );
        }
    }


    @Before
    public void init() throws Exception
    {
        atObjectClass = schemaManager.lookupAttributeTypeRegistry( "objectClass" );
        atCN = schemaManager.lookupAttributeTypeRegistry( "cn" );
        atDC = schemaManager.lookupAttributeTypeRegistry( "dc" );
        atC = schemaManager.lookupAttributeTypeRegistry( "c" );
        atL = schemaManager.lookupAttributeTypeRegistry( "l" );
        atOC = schemaManager.lookupAttributeTypeRegistry( "objectClass" );
        atSN = schemaManager.lookupAttributeTypeRegistry( "sn" );
        atPwd = schemaManager.lookupAttributeTypeRegistry( "userpassword" );
        atEMail = schemaManager.lookupAttributeTypeRegistry( "eMail" );

        EXAMPLE_DN = new Dn( schemaManager, "dc=example,dc=com" );
    }


    //-------------------------------------------------------------------------
    // Test the Constructors
    //-------------------------------------------------------------------------
    /**
     * Test for method DefaultEntry()
     */
    @Test
    public void testDefaultClientEntry() throws Exception
    {
        Entry entry = new DefaultEntry();
        assertNotNull( entry );
        assertEquals( Dn.EMPTY_DN, entry.getDn() );
        assertEquals( 0, entry.size() );
    }


    /**
     * Test for method DefaultEntry( registries )
     */
    @Test
    public void testDefaultClientEntryRegistries() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager );
        assertNotNull( entry );
        assertEquals( Dn.EMPTY_DN, entry.getDn() );
        assertEquals( 0, entry.size() );
    }


    /**
     * Test for method DefaultEntry( registries, Dn )
     */
    @Test
    public void testDefaultClientEntryRegistriesDN() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );
        assertNotNull( entry );
        assertEquals( EXAMPLE_DN, entry.getDn() );
        assertEquals( 0, entry.size() );
    }


    //-------------------------------------------------------------------------
    // Test the Add methods
    //-------------------------------------------------------------------------
    /**
     * Test for method add( EntryAttribute...)
     */
    @Test
    public void testAddEntryAttribute() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        Attribute oc = new DefaultAttribute( atObjectClass, "top", "person" );
        Attribute cn = new DefaultAttribute( atCN, "test1", "test2" );
        Attribute sn = new DefaultAttribute( atSN, "Test1", "Test2" );
        Attribute up = new DefaultAttribute( atPwd, BYTES1, BYTES2 );
        Attribute email = new DefaultAttribute( atEMail, "FR", "US" );

        entry.add( oc, cn, sn, email );

        assertEquals( 4, entry.size() );
        assertTrue( entry.containsAttribute( "ObjectClass" ) );
        assertTrue( entry.containsAttribute( "CN" ) );
        assertTrue( entry.containsAttribute( "  sn  " ) );
        assertTrue( entry.containsAttribute( " email  " ) );

        Attribute attr = entry.get( "objectclass" );
        assertEquals( 2, attr.size() );

        Attribute email2 = new DefaultAttribute( atEMail, "UK", "DE" );
        entry.add( email2, up );
        assertEquals( 5, entry.size() );

        assertTrue( entry.containsAttribute( "userPassword" ) );
        assertTrue( entry.containsAttribute( " email " ) );

        Attribute attrC = entry.get( "email" );
        assertEquals( 4, attrC.size() );

        entry.clear();
    }


    /**
     * Test for method add( String, byte[]...)
     */
    @Test
    public void testAddStringByteArrayArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        entry.add( "userPassword", ( byte[] ) null );
        assertEquals( 1, entry.size() );
        Attribute attributePWD = entry.get( "userPassword" );
        assertEquals( 1, attributePWD.size() );
        assertNotNull( attributePWD.get() );
        assertNull( attributePWD.get().getValue() );

        entry.clear();

        entry.add( "userPassword", BYTES1, BYTES1, BYTES2 );
        assertEquals( 1, entry.size() );
        Attribute attributeJPG = entry.get( "userPassword" );
        assertEquals( 2, attributeJPG.size() );
        assertNotNull( attributeJPG.get() );
        assertTrue( attributeJPG.contains( BYTES1 ) );
        assertTrue( attributeJPG.contains( BYTES2 ) );

        entry.clear();

        try
        {
            // Cannot add an attribute which does not exist
            entry.add( "wrongAT", BYTES1, BYTES2 );
            fail();
        }
        catch ( LdapNoSuchAttributeException nsae )
        {
            assertTrue( true );
        }

        // Cannot add String values into a binary attribute
        entry.add( "userPassword", "test", "test2" );
        assertEquals( 2, entry.get( "userPassword" ).size() );
    }


    /**
     * Test for method add( String, String...)
     */
    @Test
    public void testAddStringStringArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        entry.add( "dc", ( String ) null );
        assertEquals( 1, entry.size() );
        Attribute attributeDC = entry.get( "dc" );

        assertEquals( 1, attributeDC.size() );
        assertNotNull( attributeDC.get() );

        entry.add( "sn", "test", "test", "TEST" );
        assertEquals( 2, entry.size() );
        Attribute attributeSN = entry.get( "sn" );

        // 'TEST' and 'test' are the same value for 'sn' (this is a case insensitive attributeType)
        assertEquals( 1, attributeSN.size() );
        assertNotNull( attributeSN.get() );
        assertTrue( attributeSN.contains( "test" ) );
        assertTrue( attributeSN.contains( "TEST" ) );

        entry.clear();

        try
        {
            // Cannot add an attribute which does not exist
            entry.add( "wrongAT", "wrong", "wrong" );
            fail();
        }
        catch ( LdapNoSuchAttributeException nsae )
        {
            assertTrue( true );
        }

        // Cannot add binary values into a String attribute
        entry.add( "sn", BYTES1, BYTES2 );
        assertEquals( 0, entry.get( "sn" ).size() );
    }


    /**
     * Test for method add( String, Value<?>...)
     */
    @Test
    public void testAddStringValueArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );
        Value<String> value = new StringValue( atDC, ( String ) null );

        entry.add( "dc", value );
        assertEquals( 1, entry.size() );
        Attribute attributeCN = entry.get( "dc" );
        assertEquals( 1, attributeCN.size() );
        assertNotNull( attributeCN.get() );
        assertNull( attributeCN.get().getValue() );

        Value<String> value1 = new StringValue( atCN, "test1" );
        Value<String> value2 = new StringValue( atCN, "test2" );
        Value<String> value3 = new StringValue( atCN, "test1" );

        entry.add( "sn", value1, value2, value3 );
        assertEquals( 2, entry.size() );
        Attribute attributeSN = entry.get( "sn" );
        assertEquals( 2, attributeSN.size() );
        assertNotNull( attributeSN.get() );
        assertTrue( attributeSN.contains( value1 ) );
        assertTrue( attributeSN.contains( value2 ) );

        Value<byte[]> value4 = new BinaryValue( atPwd, BYTES1 );
        entry.add( "l", value1, value4 );
        assertEquals( 3, entry.size() );
        Attribute attributeL = entry.get( "l" );

        // Cannot store a binary value in a String attribute
        assertEquals( 1, attributeL.size() );
        assertNotNull( attributeL.get() );
        assertTrue( attributeL.contains( value1 ) );

        entry.clear();

        try
        {
            // Cannot add an attribute which does not exist
            entry.add( "wrongAT", value1, value2 );
            fail();
        }
        catch ( LdapNoSuchAttributeException nsae )
        {
            assertTrue( true );
        }
    }


    /**
     * Test method for add( AttributeType, byte[]... )
     */
    @Test
    public void testAddAttributeTypeByteArrayArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        entry.add( atPwd, BYTES1, BYTES2 );
        assertEquals( 1, entry.size() );
        assertTrue( entry.contains( atPwd, BYTES1, BYTES2 ) );

        entry.add( atPwd, ( byte[] ) null, BYTES1 );
        assertEquals( 1, entry.size() );

        Attribute attribute = entry.get( atPwd );
        assertEquals( 3, attribute.size() );
        assertTrue( attribute.contains( BYTES1 ) );
        assertTrue( attribute.contains( BYTES2 ) );
        assertTrue( attribute.contains( ( byte[] ) null ) );
    }


    /**
     * Test method for add( AttributeType, String... )
     */
    @Test
    public void testAddAttributeTypeStringArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        entry.add( atC, "us", "fr" );
        assertEquals( 1, entry.size() );
        assertFalse( entry.contains( atC, "fr", "us" ) );

        entry.add( atC, "de", "fr" );
        assertEquals( 1, entry.size() );

        Attribute attribute = entry.get( atC );
        assertEquals( 0, attribute.size() );
        assertFalse( attribute.contains( "de" ) );
        assertFalse( attribute.contains( "fr" ) );
        assertFalse( attribute.contains( "us" ) );

        entry.clear();

        assertEquals( 0, entry.size() );
    }


    /**
     * Test method for add( AttributeType, Value<?>... )
     */
    @Test
    public void testAddAttributeTypeValueArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        Value<String> strValue1 = new StringValue( atDC, "test1" );
        Value<String> strValue2 = new StringValue( atDC, "test2" );
        Value<String> strValue3 = new StringValue( atDC, "test3" );
        Value<String> strNullValue = new StringValue( atDC, null );

        Value<byte[]> binValue1 = new BinaryValue( atPwd, BYTES1 );
        Value<byte[]> binValue2 = new BinaryValue( atPwd, BYTES2 );
        Value<byte[]> binValue3 = new BinaryValue( atPwd, BYTES3 );

        try
        {
            entry.add( ( AttributeType ) null, strValue1 );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        entry.add( atDC, strValue1, strValue2, strValue1 );
        entry.add( atPwd, binValue1, binValue2, binValue1 );

        assertEquals( 2, entry.size() );
        assertTrue( entry.contains( atDC, "test1", "test2" ) );
        assertTrue( entry.contains( atPwd, BYTES1, BYTES2 ) );

        entry.add( atDC, strValue3, strNullValue );

        assertEquals( 4, entry.get( atDC ).size() );
        assertTrue( entry.contains( atDC, strNullValue ) );

        entry.add( atCN, binValue3 );
        assertFalse( entry.contains( atCN, binValue3 ) );
    }


    /**
     * Test method for add( String, AttributeType, byte[]... )
     */
    @Test
    public void testAddStringAttributeTypeByteArrayArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        entry.add( "UserPassword", atPwd, BYTES1, BYTES2 );
        assertEquals( 1, entry.size() );
        assertTrue( entry.contains( atPwd, BYTES1, BYTES2 ) );
        assertEquals( "UserPassword", entry.get( atPwd ).getUpId() );
        assertEquals( "2.5.4.35", entry.get( atPwd ).getId() );

        entry.add( "  UserPassword  ", atPwd, ( byte[] ) null, BYTES1 );
        assertEquals( 1, entry.size() );

        Attribute attribute = entry.get( atPwd );
        assertEquals( 3, attribute.size() );
        assertTrue( attribute.contains( BYTES1 ) );
        assertTrue( attribute.contains( BYTES2 ) );
        assertTrue( attribute.contains( ( byte[] ) null ) );
        assertEquals( "  UserPassword  ", attribute.getUpId() );
        assertEquals( "2.5.4.35", attribute.getId() );

        try
        {
            entry.add( "  ObjectClass  ", atOC, BYTES1 );
            fail();
        }
        catch ( UnsupportedOperationException uoe )
        {
            assertTrue( true );
        }
    }


    /**
     * Test method for add( String, AttributeType, String... )
     */
    @Test
    public void testAddStringAttributeTypeStringArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        entry.add( "EMail", atEMail, "test1", "test2" );
        assertEquals( 1, entry.size() );
        assertTrue( entry.contains( atEMail, "test1", "test2" ) );
        assertEquals( "EMail", entry.get( atEMail ).getUpId() );
        assertEquals( "1.2.840.113549.1.9.1", entry.get( atEMail ).getId() );

        entry.add( "  EMAIL  ", atEMail, ( String ) null, "test1" );
        assertEquals( 1, entry.size() );

        Attribute attribute = entry.get( atEMail );
        assertEquals( 3, attribute.size() );
        assertTrue( attribute.contains( "test1" ) );
        assertTrue( attribute.contains( ( String ) null ) );
        assertTrue( attribute.contains( "test2" ) );
        assertEquals( "  EMAIL  ", attribute.getUpId() );
        assertEquals( "1.2.840.113549.1.9.1", attribute.getId() );

        entry.clear();

        // Binary values are not allowed
        entry.add( "  EMail  ", atEMail, BYTES1 );
        assertEquals( 1, entry.size() );
        assertNotNull( entry.get( atEMail ) );
        assertEquals( 0, entry.get( atEMail ).size() );
    }


    /**
     * Test method for add( String, AttributeType, Value<?>... )
     */
    @Test
    public void testAddStringAttributeTypeValueArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        Value<String> strValue1 = new StringValue( atDC, "test1" );
        Value<String> strValue2 = new StringValue( atDC, "test2" );
        Value<String> strValue3 = new StringValue( atDC, "test3" );
        Value<String> strNullValue = new StringValue( atDC, null );

        Value<byte[]> binValue1 = new BinaryValue( atPwd, BYTES1 );
        Value<byte[]> binValue2 = new BinaryValue( atPwd, BYTES2 );
        Value<byte[]> binValue3 = new BinaryValue( atPwd, BYTES3 );

        try
        {
            entry.add( "cn", ( AttributeType ) null, strValue1 );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        entry.add( "DC", atDC, strValue1, strValue2, strValue1 );
        entry.add( "UserPassword", atPwd, binValue1, binValue2, binValue1 );

        assertEquals( 2, entry.size() );
        assertTrue( entry.contains( atDC, "test1", "test2" ) );
        assertTrue( entry.contains( atPwd, BYTES1, BYTES2 ) );
        assertEquals( "DC", entry.get( atDC ).getUpId() );
        assertEquals( "0.9.2342.19200300.100.1.25", entry.get( atDC ).getId() );
        assertEquals( "UserPassword", entry.get( atPwd ).getUpId() );
        assertEquals( "2.5.4.35", entry.get( atPwd ).getId() );

        entry.add( "DC", atDC, strValue3, strNullValue );

        assertEquals( 4, entry.get( atDC ).size() );
        assertTrue( entry.contains( atDC, strNullValue ) );

        entry.add( atDC, binValue3 );
        assertFalse( entry.contains( atDC, binValue3 ) );

        try
        {
            entry.add( "SN", atDC, "test" );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }
    }


    /**
     * Test the add( AT, String... ) method
     */
    @Test
    public void testAddAtStringElipsis() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=test" );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );

        // Test that we can't inject a null AT
        try
        {
            entry.add( ( AttributeType ) null, "test" );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            // Expected
        }

        // Test a simple addition
        entry.add( atEMail, "test1" );
        assertNotNull( entry.get( atEMail ) );
        assertEquals( 1, entry.get( atEMail ).size() );
        assertEquals( "test1", entry.get( atEMail ).get().getString() );

        // Test some more addition
        entry.add( atEMail, "test2", "test3" );
        assertNotNull( entry.get( atEMail ) );
        assertEquals( 3, entry.get( atEMail ).size() );
        assertTrue( entry.contains( atEMail, "test1" ) );
        assertTrue( entry.contains( atEMail, "test2" ) );
        assertTrue( entry.contains( atEMail, "test3" ) );

        // Test some addition of existing values
        entry.add( atEMail, "test2" );
        assertNotNull( entry.get( atEMail ) );
        assertEquals( 3, entry.get( atEMail ).size() );
        assertTrue( entry.contains( atEMail, "test1" ) );
        assertTrue( entry.contains( atEMail, "test2" ) );
        assertTrue( entry.contains( atEMail, "test3" ) );

        // Test the addition of a null value
        entry.add( atEMail, ( String ) null );
        assertNotNull( entry.get( atEMail ) );
        assertEquals( 4, entry.get( atEMail ).size() );
        assertTrue( entry.contains( atEMail, "test1" ) );
        assertTrue( entry.contains( atEMail, "test2" ) );
        assertTrue( entry.contains( atEMail, "test3" ) );
        assertTrue( entry.contains( atEMail, ( String ) null ) );

        entry.clear();

        // Test the addition of a binary value
        byte[] test4 = Strings.getBytesUtf8( "test4" );

        entry.add( atCN, test4 );
        assertFalse( entry.get( atCN ).contains( test4 ) );
    }


    /**
     * Test the add( AT, byte[]... ) method
     */
    @Test
    public void testAddAtBytesElipsis() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=test" );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );

        AttributeType atPassword = schemaManager.lookupAttributeTypeRegistry( "userPassword" );
        AttributeType atJpegPhoto = schemaManager.lookupAttributeTypeRegistry( "jpegPhoto" );

        byte[] test1 = Strings.getBytesUtf8( "test1" );
        byte[] test2 = Strings.getBytesUtf8( "test2" );
        byte[] test3 = Strings.getBytesUtf8( "test3" );

        // Test that we can't inject a null AT
        try
        {
            entry.add( ( AttributeType ) null, test1 );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            // Expected
        }

        // Test that we cannot inject a null value in an AT that does not allow it
        try
        {
            entry.add( atJpegPhoto, ( byte[] ) null );
            fail();
        }
        catch ( LdapInvalidAttributeValueException liave )
        {
            // Expected
        }

        // Test a simple addition
        entry.add( atPassword, test1 );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 1, entry.get( atPassword ).size() );
        assertTrue( Arrays.equals( test1, entry.get( atPassword ).get().getBytes() ) );

        // Test some more addition
        entry.add( atPassword, test2, test3 );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 3, entry.get( atPassword ).size() );
        assertTrue( entry.contains( atPassword, test1 ) );
        assertTrue( entry.contains( atPassword, test2 ) );
        assertTrue( entry.contains( atPassword, test3 ) );

        // Test some addition of existing values
        entry.add( atPassword, test2 );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 3, entry.get( atPassword ).size() );
        assertTrue( entry.contains( atPassword, test1 ) );
        assertTrue( entry.contains( atPassword, test2 ) );
        assertTrue( entry.contains( atPassword, test3 ) );

        // Test the addition of a null value
        entry.add( atPassword, ( byte[] ) null );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 4, entry.get( atPassword ).size() );
        assertTrue( entry.contains( atPassword, test1 ) );
        assertTrue( entry.contains( atPassword, test2 ) );
        assertTrue( entry.contains( atPassword, test3 ) );
        assertTrue( entry.contains( atPassword, ( byte[] ) null ) );

        entry.clear();

        // Test the addition of a String value. It should be converted to a byte array
        byte[] test4 = Strings.getBytesUtf8( "test4" );

        entry.add( atPassword, "test4" );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 1, entry.get( atPassword ).size() );
        assertTrue( entry.contains( atPassword, test4 ) );
    }


    /**
     * Test the add( AT, SV... ) method
     */
    @Test
    public void testAddAtServerValueElipsis() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=test" );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );

        AttributeType atPassword = schemaManager.lookupAttributeTypeRegistry( "userPassword" );

        byte[] b1 = Strings.getBytesUtf8( "test1" );
        byte[] b2 = Strings.getBytesUtf8( "test2" );
        byte[] b3 = Strings.getBytesUtf8( "test3" );

        Value<String> test1 = new StringValue( atDC, "test1" );

        Value<String> testEMail1 = new StringValue( atEMail, "test1" );
        Value<String> testEMail2 = new StringValue( atEMail, "test2" );
        Value<String> testEMail3 = new StringValue( atEMail, "test3" );

        Value<byte[]> testB1 = new BinaryValue( atPassword, b1 );
        Value<byte[]> testB2 = new BinaryValue( atPassword, b2 );
        Value<byte[]> testB3 = new BinaryValue( atPassword, b3 );

        // Test that we can't inject a null AT
        try
        {
            entry.add( ( AttributeType ) null, test1 );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            // Expected
        }

        // Test a simple addition in atEMail
        entry.add( atEMail, testEMail1 );
        assertNotNull( entry.get( atEMail ) );
        assertEquals( 1, entry.get( atEMail ).size() );
        assertEquals( "test1", entry.get( atEMail ).get().getString() );

        // Test some more addition
        entry.add( atEMail, testEMail2, testEMail3 );
        assertNotNull( entry.get( atEMail ) );
        assertEquals( 3, entry.get( atEMail ).size() );
        assertTrue( entry.contains( atEMail, "test1" ) );
        assertTrue( entry.contains( atEMail, "test2" ) );
        assertTrue( entry.contains( atEMail, "test3" ) );

        // Test some addition of existing values
        entry.add( atEMail, testEMail2 );
        assertNotNull( entry.get( atEMail ) );
        assertEquals( 3, entry.get( atEMail ).size() );
        assertTrue( entry.contains( atEMail, "test1" ) );
        assertTrue( entry.contains( atEMail, "test2" ) );
        assertTrue( entry.contains( atEMail, "test3" ) );

        // Test the addition of a null value
        entry.add( atEMail, ( String ) null );
        assertNotNull( entry.get( atEMail ) );
        assertEquals( 4, entry.get( atEMail ).size() );
        assertTrue( entry.contains( atEMail, "test1" ) );
        assertTrue( entry.contains( atEMail, "test2" ) );
        assertTrue( entry.contains( atEMail, "test3" ) );
        assertTrue( entry.contains( atEMail, ( String ) null ) );

        entry.clear();

        // Test the addition of a String value. It should be converted to a byte array
        byte[] test4 = Strings.getBytesUtf8( "test4" );

        entry.add( atDC, test4 );
        assertFalse( entry.contains( atDC, test4 ) );

        // Now, work with a binary attribute
        // Test a simple addition
        entry.add( atPassword, testB1 );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 1, entry.get( atPassword ).size() );
        assertTrue( Arrays.equals( b1, entry.get( atPassword ).get().getBytes() ) );

        // Test some more addition
        entry.add( atPassword, testB2, testB3 );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 3, entry.get( atPassword ).size() );
        assertTrue( entry.contains( atPassword, b1 ) );
        assertTrue( entry.contains( atPassword, b2 ) );
        assertTrue( entry.contains( atPassword, b3 ) );

        // Test some addition of existing values
        entry.add( atPassword, testB2 );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 3, entry.get( atPassword ).size() );
        assertTrue( entry.contains( atPassword, b1 ) );
        assertTrue( entry.contains( atPassword, b2 ) );
        assertTrue( entry.contains( atPassword, b3 ) );

        // Test the addition of a null value
        entry.add( atPassword, ( byte[] ) null );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 4, entry.get( atPassword ).size() );
        assertTrue( entry.contains( atPassword, b1 ) );
        assertTrue( entry.contains( atPassword, b2 ) );
        assertTrue( entry.contains( atPassword, b3 ) );
        assertTrue( entry.contains( atPassword, ( byte[] ) null ) );

        entry.clear();

        // Test the addition of a String value. It should be converted to a byte array
        byte[] b4 = Strings.getBytesUtf8( "test4" );

        entry.add( atPassword, "test4" );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 1, entry.get( atPassword ).size() );
        assertTrue( entry.contains( atPassword, b4 ) );
    }


    /**
     * Test the add( upId, String... ) method
     */
    @Test
    public void testAddUpIdStringElipsis() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=test" );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );

        // Test a simple addition
        entry.add( "EMail", "test1" );
        assertNotNull( entry.get( atEMail ) );
        assertTrue( entry.containsAttribute( atEMail ) );
        assertEquals( "1.2.840.113549.1.9.1", entry.get( atEMail ).getId() );
        assertEquals( "EMail", entry.get( atEMail ).getUpId() );
        assertEquals( 1, entry.get( atEMail ).size() );
        assertEquals( "test1", entry.get( atEMail ).get().getString() );

        // Test some more addition
        entry.add( "EMail", "test2", "test3" );
        assertNotNull( entry.get( atEMail ) );
        assertEquals( 3, entry.get( atEMail ).size() );
        assertTrue( entry.contains( atEMail, "test1" ) );
        assertTrue( entry.contains( atEMail, "test2" ) );
        assertTrue( entry.contains( atEMail, "test3" ) );

        // Test some addition of existing values
        entry.add( "EMail", "test2" );
        assertNotNull( entry.get( atEMail ) );
        assertEquals( 3, entry.get( atEMail ).size() );
        assertTrue( entry.contains( atEMail, "test1" ) );
        assertTrue( entry.contains( atEMail, "test2" ) );
        assertTrue( entry.contains( atEMail, "test3" ) );

        // Test the addition of a null value
        entry.add( "EMail", ( String ) null );
        assertNotNull( entry.get( atEMail ) );
        assertEquals( 4, entry.get( atEMail ).size() );
        assertTrue( entry.contains( atEMail, "test1" ) );
        assertTrue( entry.contains( atEMail, "test2" ) );
        assertTrue( entry.contains( atEMail, "test3" ) );
        assertTrue( entry.contains( atEMail, ( String ) null ) );

        entry.clear();

        // Test the addition of a binary value
        byte[] test4 = Strings.getBytesUtf8( "test4" );

        entry.add( "DC", test4 );
        assertFalse( entry.contains( "DC", test4 ) );
    }


    /**
     * Test the add( upId, byte[]... ) method
     */
    @Test
    public void testAddUpIdBytesElipsis() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=test" );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );

        AttributeType atPassword = schemaManager.lookupAttributeTypeRegistry( "userPassword" );

        byte[] test1 = Strings.getBytesUtf8( "test1" );
        byte[] test2 = Strings.getBytesUtf8( "test2" );
        byte[] test3 = Strings.getBytesUtf8( "test3" );

        // Test a simple addition
        entry.add( "userPassword", test1 );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 1, entry.get( atPassword ).size() );
        assertTrue( Arrays.equals( test1, entry.get( atPassword ).get().getBytes() ) );

        // Test some more addition
        entry.add( "userPassword", test2, test3 );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 3, entry.get( atPassword ).size() );
        assertTrue( entry.contains( atPassword, test1 ) );
        assertTrue( entry.contains( atPassword, test2 ) );
        assertTrue( entry.contains( atPassword, test3 ) );

        // Test some addition of existing values
        entry.add( "userPassword", test2 );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 3, entry.get( atPassword ).size() );
        assertTrue( entry.contains( atPassword, test1 ) );
        assertTrue( entry.contains( atPassword, test2 ) );
        assertTrue( entry.contains( atPassword, test3 ) );

        // Test the addition of a null value
        entry.add( "userPassword", ( byte[] ) null );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 4, entry.get( atPassword ).size() );
        assertTrue( entry.contains( atPassword, test1 ) );
        assertTrue( entry.contains( atPassword, test2 ) );
        assertTrue( entry.contains( atPassword, test3 ) );
        assertTrue( entry.contains( atPassword, ( byte[] ) null ) );

        entry.clear();

        // Test the addition of a String value. It should be converted to a byte array
        byte[] test4 = Strings.getBytesUtf8( "test4" );

        entry.add( "userPassword", "test4" );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 1, entry.get( atPassword ).size() );
        assertTrue( entry.contains( atPassword, test4 ) );
    }


    /**
     * Test the add( upId, SV... ) method
     */
    @Test
    public void testAddUpIdServerValueElipsis() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=test" );
        Entry entry = new DefaultEntry( schemaManager, dn );

        AttributeType atPassword = schemaManager.lookupAttributeTypeRegistry( "userPassword" );

        byte[] b1 = Strings.getBytesUtf8( "test1" );
        byte[] b2 = Strings.getBytesUtf8( "test2" );
        byte[] b3 = Strings.getBytesUtf8( "test3" );

        Value<String> test1 = new StringValue( atEMail, "test1" );
        Value<String> test2 = new StringValue( atEMail, "test2" );
        Value<String> test3 = new StringValue( atEMail, "test3" );

        Value<byte[]> testB1 = new BinaryValue( atPassword, b1 );
        Value<byte[]> testB2 = new BinaryValue( atPassword, b2 );
        Value<byte[]> testB3 = new BinaryValue( atPassword, b3 );

        // Test a simple addition in atDC
        entry.add( "eMail", test1 );
        assertNotNull( entry.get( atEMail ) );
        assertEquals( 1, entry.get( atEMail ).size() );
        assertEquals( "test1", entry.get( atEMail ).get().getString() );
        assertTrue( entry.containsAttribute( atEMail ) );
        assertEquals( "eMail", entry.get( atEMail ).getUpId() );

        // Test some more addition
        entry.add( "eMail", test2, test3 );
        assertNotNull( entry.get( atEMail ) );
        assertEquals( 3, entry.get( atEMail ).size() );
        assertTrue( entry.contains( atEMail, "test1" ) );
        assertTrue( entry.contains( atEMail, "test2" ) );
        assertTrue( entry.contains( atEMail, "test3" ) );
        assertTrue( entry.containsAttribute( atEMail ) );
        assertEquals( "eMail", entry.get( atEMail ).getUpId() );

        // Test some addition of existing values
        entry.add( "eMail", test2 );
        assertNotNull( entry.get( atEMail ) );
        assertEquals( 3, entry.get( atEMail ).size() );
        assertTrue( entry.contains( atEMail, "test1" ) );
        assertTrue( entry.contains( atEMail, "test2" ) );
        assertTrue( entry.contains( atEMail, "test3" ) );

        // Test the addition of a null value
        entry.add( "eMail", ( String ) null );
        assertNotNull( entry.get( atEMail ) );
        assertEquals( 4, entry.get( atEMail ).size() );
        assertTrue( entry.contains( atEMail, "test1" ) );
        assertTrue( entry.contains( atEMail, "test2" ) );
        assertTrue( entry.contains( atEMail, "test3" ) );
        assertTrue( entry.contains( atEMail, ( String ) null ) );

        entry.clear();

        // Test the addition of a String value. It should be converted to a byte array
        byte[] test4 = Strings.getBytesUtf8( "test4" );

        entry.add( "eMail", test4 );
        assertFalse( entry.contains( "cN", test4 ) );

        // Now, work with a binary attribute
        // Test a simple addition
        entry.add( "userPASSWORD", testB1 );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 1, entry.get( atPassword ).size() );
        assertTrue( Arrays.equals( b1, entry.get( atPassword ).get().getBytes() ) );
        assertTrue( entry.containsAttribute( atPassword ) );
        assertEquals( "userPASSWORD", entry.get( atPassword ).getUpId() );

        // Test some more addition
        entry.add( "userPASSWORD", testB2, testB3 );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 3, entry.get( atPassword ).size() );
        assertTrue( entry.contains( atPassword, b1 ) );
        assertTrue( entry.contains( atPassword, b2 ) );
        assertTrue( entry.contains( atPassword, b3 ) );

        // Test some addition of existing values
        entry.add( "userPASSWORD", testB2 );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 3, entry.get( atPassword ).size() );
        assertTrue( entry.contains( atPassword, b1 ) );
        assertTrue( entry.contains( atPassword, b2 ) );
        assertTrue( entry.contains( atPassword, b3 ) );

        // Test the addition of a null value
        entry.add( "userPASSWORD", ( byte[] ) null );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 4, entry.get( atPassword ).size() );
        assertTrue( entry.contains( atPassword, b1 ) );
        assertTrue( entry.contains( atPassword, b2 ) );
        assertTrue( entry.contains( atPassword, b3 ) );
        assertTrue( entry.contains( atPassword, ( byte[] ) null ) );

        entry.clear();

        // Test the addition of a String value. It should be converted to a byte array
        byte[] b4 = Strings.getBytesUtf8( "test4" );

        entry.add( "userPASSWORD", "test4" );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 1, entry.get( atPassword ).size() );
        assertTrue( entry.contains( atPassword, b4 ) );
    }


    /**
     * Test the add( UpId, AT, String... ) method
     */
    @Test
    public void testAddUpIdAtStringElipsis() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=test" );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );

        // Test a simple addition
        entry.add( "email", atEMail, "test1" );
        assertNotNull( entry.get( atEMail ) );
        assertEquals( 1, entry.get( atEMail ).size() );
        assertEquals( "test1", entry.get( atEMail ).get().getString() );

        // Test some more addition
        entry.add( "EMAIL", atEMail, "test2", "test3" );
        assertNotNull( entry.get( atEMail ) );
        assertEquals( 3, entry.get( atEMail ).size() );
        assertTrue( entry.contains( atEMail, "test1" ) );
        assertTrue( entry.contains( atEMail, "test2" ) );
        assertTrue( entry.contains( atEMail, "test3" ) );

        // Test some addition of existing values
        entry.add( "EMail", atEMail, "test2" );
        assertNotNull( entry.get( atEMail ) );
        assertEquals( 3, entry.get( atEMail ).size() );
        assertTrue( entry.contains( atEMail, "test1" ) );
        assertTrue( entry.contains( atEMail, "test2" ) );
        assertTrue( entry.contains( atEMail, "test3" ) );

        // Test the addition of a null value
        entry.add( "EMAIL", atEMail, ( String ) null );
        assertNotNull( entry.get( atEMail ) );
        assertEquals( 4, entry.get( atEMail ).size() );
        assertTrue( entry.contains( atEMail, "test1" ) );
        assertTrue( entry.contains( atEMail, "test2" ) );
        assertTrue( entry.contains( atEMail, "test3" ) );
        assertTrue( entry.contains( atEMail, ( String ) null ) );

        entry.clear();

        // Test the addition of a binary value
        byte[] test4 = Strings.getBytesUtf8( "test4" );

        entry.add( "email", atEMail, test4 );
        assertFalse( entry.contains( "email", test4 ) );
    }


    /**
     * Test the add( upId, AT, byte[]... ) method
     */
    @Test
    public void testAddUpIdAtBytesElipsis() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=test" );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );

        AttributeType atPassword = schemaManager.lookupAttributeTypeRegistry( "userPassword" );

        byte[] test1 = Strings.getBytesUtf8( "test1" );
        byte[] test2 = Strings.getBytesUtf8( "test2" );
        byte[] test3 = Strings.getBytesUtf8( "test3" );

        // Test a simple addition
        entry.add( "userPassword", atPassword, test1 );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 1, entry.get( atPassword ).size() );
        assertTrue( Arrays.equals( test1, entry.get( atPassword ).get().getBytes() ) );

        // Test some more addition
        entry.add( "userPassword", atPassword, test2, test3 );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 3, entry.get( atPassword ).size() );
        assertTrue( entry.contains( atPassword, test1 ) );
        assertTrue( entry.contains( atPassword, test2 ) );
        assertTrue( entry.contains( atPassword, test3 ) );

        // Test some addition of existing values
        entry.add( "userPassword", atPassword, test2 );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 3, entry.get( atPassword ).size() );
        assertTrue( entry.contains( atPassword, test1 ) );
        assertTrue( entry.contains( atPassword, test2 ) );
        assertTrue( entry.contains( atPassword, test3 ) );

        // Test the addition of a null value
        entry.add( "userPassword", atPassword, ( byte[] ) null );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 4, entry.get( atPassword ).size() );
        assertTrue( entry.contains( atPassword, test1 ) );
        assertTrue( entry.contains( atPassword, test2 ) );
        assertTrue( entry.contains( atPassword, test3 ) );
        assertTrue( entry.contains( atPassword, ( byte[] ) null ) );

        entry.clear();

        // Test the addition of a String value. It should be converted to a byte array
        byte[] test4 = Strings.getBytesUtf8( "test4" );

        entry.add( "userPassword", atPassword, "test4" );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 1, entry.get( atPassword ).size() );
        assertTrue( entry.contains( atPassword, test4 ) );
    }


    /**
     * Test the add( upId, AT, SV... ) method
     */
    @Test
    public void testAddUpIdAtServerValueElipsis() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=test" );
        Entry entry = new DefaultEntry( schemaManager, dn );

        AttributeType atPassword = schemaManager.lookupAttributeTypeRegistry( "userPassword" );

        byte[] b1 = Strings.getBytesUtf8( "test1" );
        byte[] b2 = Strings.getBytesUtf8( "test2" );
        byte[] b3 = Strings.getBytesUtf8( "test3" );

        Value<String> test1 = new StringValue( atEMail, "test1" );
        Value<String> test2 = new StringValue( atEMail, "test2" );
        Value<String> test3 = new StringValue( atEMail, "test3" );

        Value<byte[]> testB1 = new BinaryValue( atPassword, b1 );
        Value<byte[]> testB2 = new BinaryValue( atPassword, b2 );
        Value<byte[]> testB3 = new BinaryValue( atPassword, b3 );

        // Test a simple addition in atCN
        entry.add( "eMail", atEMail, test1 );
        assertNotNull( entry.get( atEMail ) );
        assertEquals( 1, entry.get( atEMail ).size() );
        assertEquals( "test1", entry.get( atEMail ).get().getString() );
        assertTrue( entry.containsAttribute( atEMail ) );
        assertEquals( "eMail", entry.get( atEMail ).getUpId() );

        // Test some more addition
        entry.add( "eMail", atEMail, test2, test3 );
        assertNotNull( entry.get( atEMail ) );
        assertEquals( 3, entry.get( atEMail ).size() );
        assertTrue( entry.contains( atEMail, "test1" ) );
        assertTrue( entry.contains( atEMail, "test2" ) );
        assertTrue( entry.contains( atEMail, "test3" ) );
        assertTrue( entry.containsAttribute( atEMail ) );
        assertEquals( "eMail", entry.get( atEMail ).getUpId() );

        // Test some addition of existing values
        entry.add( "eMail", atEMail, test2 );
        assertNotNull( entry.get( atEMail ) );
        assertEquals( 3, entry.get( atEMail ).size() );
        assertTrue( entry.contains( atEMail, "test1" ) );
        assertTrue( entry.contains( atEMail, "test2" ) );
        assertTrue( entry.contains( atEMail, "test3" ) );

        // Test the addition of a null value
        entry.add( "eMail", atEMail, ( String ) null );
        assertNotNull( entry.get( atEMail ) );
        assertEquals( 4, entry.get( atEMail ).size() );
        assertTrue( entry.contains( atEMail, "test1" ) );
        assertTrue( entry.contains( atEMail, "test2" ) );
        assertTrue( entry.contains( atEMail, "test3" ) );
        assertTrue( entry.contains( atEMail, ( String ) null ) );

        entry.clear();

        // Test the addition of a String value. It should be converted to a byte array
        byte[] test4 = Strings.getBytesUtf8( "test4" );

        entry.add( "eMail", atEMail, test4 );
        assertFalse( entry.contains( "cN", test4 ) );

        // Now, work with a binary attribute
        // Test a simple addition
        entry.add( "userPASSWORD", atPassword, testB1 );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 1, entry.get( atPassword ).size() );
        assertTrue( Arrays.equals( b1, entry.get( atPassword ).get().getBytes() ) );
        assertTrue( entry.containsAttribute( atPassword ) );
        assertEquals( "userPASSWORD", entry.get( atPassword ).getUpId() );

        // Test some more addition
        entry.add( "userPASSWORD", atPassword, testB2, testB3 );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 3, entry.get( atPassword ).size() );
        assertTrue( entry.contains( atPassword, b1 ) );
        assertTrue( entry.contains( atPassword, b2 ) );
        assertTrue( entry.contains( atPassword, b3 ) );

        // Test some addition of existing values
        entry.add( "userPASSWORD", atPassword, testB2 );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 3, entry.get( atPassword ).size() );
        assertTrue( entry.contains( atPassword, b1 ) );
        assertTrue( entry.contains( atPassword, b2 ) );
        assertTrue( entry.contains( atPassword, b3 ) );

        // Test the addition of a null value
        entry.add( "userPASSWORD", atPassword, ( byte[] ) null );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 4, entry.get( atPassword ).size() );
        assertTrue( entry.contains( atPassword, b1 ) );
        assertTrue( entry.contains( atPassword, b2 ) );
        assertTrue( entry.contains( atPassword, b3 ) );
        assertTrue( entry.contains( atPassword, ( byte[] ) null ) );

        entry.clear();

        // Test the addition of a String value. It should be converted to a byte array
        byte[] b4 = Strings.getBytesUtf8( "test4" );

        entry.add( "userPASSWORD", atPassword, "test4" );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 1, entry.get( atPassword ).size() );
        assertTrue( entry.contains( atPassword, b4 ) );
    }


    /**
     * Test method for clear()
     */
    @Test
    public void testClear() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        assertEquals( 0, entry.size() );
        assertNull( entry.get( "ObjectClass" ) );
        entry.clear();
        assertEquals( 0, entry.size() );
        assertNull( entry.get( "ObjectClass" ) );

        entry.add( "ObjectClass", "top", "person" );
        assertEquals( 1, entry.size() );
        assertNotNull( entry.get( "ObjectClass" ) );

        entry.clear();
        assertEquals( 0, entry.size() );
        assertNull( entry.get( "ObjectClass" ) );
    }


    /**
     * Test method for clone()
     */
    @Test
    public void testClone() throws Exception
    {
        Entry entry1 = new DefaultEntry( schemaManager );

        Entry entry2 = entry1.clone();

        assertEquals( entry1, entry2 );
        entry2.setDn( EXAMPLE_DN );

        assertEquals( Dn.EMPTY_DN, entry1.getDn() );

        entry1.setDn( EXAMPLE_DN );
        entry2 = entry1.clone();
        assertEquals( entry1, entry2 );

        entry1.add( "objectClass", "top", "person" );
        entry1.add( "cn", "test1", "test2" );

        entry2 = entry1.clone();
        assertEquals( entry1, entry2 );

        entry1.add( "cn", "test3" );
        assertEquals( 2, entry2.get( "cn" ).size() );
        assertFalse( entry2.contains( "cn", "test3" ) );

        entry1.add( "dc", ( String ) null );
        assertFalse( entry2.containsAttribute( "dc" ) );
    }


    //-------------------------------------------------------------------------
    // Test the Contains methods
    //-------------------------------------------------------------------------
    /**
     * Test for method contains( AttributeType, byte[]... )
     */
    @Test
    public void testContainsAttributeTypeByteArrayArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        assertFalse( entry.contains( ( AttributeType ) null, BYTES1 ) );
        assertFalse( entry.contains( atPwd, BYTES1 ) );

        Attribute attrPWD = new DefaultAttribute( atPwd, BYTES1, BYTES2 );

        assertFalse( entry.contains( attrPWD ) );

        entry.add( attrPWD );

        assertTrue( entry.contains( atPwd, BYTES1, BYTES2 ) );
        assertFalse( entry.contains( atPwd, BYTES1, BYTES2, BYTES3 ) );
        assertFalse( entry.contains( atPwd, "ab" ) );
    }


    /**
     * Test for method contains( AttributeType, String... )
     */
    @Test
    public void testContainsAttributeTypeStringArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        assertFalse( entry.contains( ( AttributeType ) null, "test" ) );
        assertFalse( entry.contains( atCN, "test" ) );

        Attribute attrCN = new DefaultAttribute( atCN, "test1", "test2" );

        assertFalse( entry.contains( attrCN ) );

        entry.add( attrCN );

        assertTrue( entry.contains( atCN, "test1", "test2" ) );
        assertFalse( entry.contains( atCN, "test1", "test2", "test3" ) );
        assertFalse( entry.contains( atCN, BYTES1 ) );
    }


    /**
     * Test for method contains( AttributeType, Value<?>... )
     */
    @Test
    public void testContainsAttributeTypeValuesArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        Value<String> strValue1 = new StringValue( atDC, "test1" );
        Value<String> strValue2 = new StringValue( atDC, "test2" );
        Value<String> strValue3 = new StringValue( atDC, "test3" );
        Value<String> strNullValue = new StringValue( atDC, null );

        Value<byte[]> binValue1 = new BinaryValue( atPwd, BYTES1 );
        Value<byte[]> binValue2 = new BinaryValue( atPwd, BYTES2 );
        Value<byte[]> binValue3 = new BinaryValue( atPwd, BYTES3 );
        Value<byte[]> binNullValue = new BinaryValue( atPwd, null );

        assertFalse( entry.contains( ( String ) null, strValue1 ) );
        assertFalse( entry.contains( atDC, binValue1 ) );

        Attribute attrCN = new DefaultAttribute( atDC, strValue1, strValue2 );
        Attribute attrPWD = new DefaultAttribute( atPwd, binValue1, binValue2, binNullValue );

        entry.add( attrCN, attrPWD );

        assertTrue( entry.contains( atDC, strValue1, strValue2 ) );
        assertTrue( entry.contains( atPwd, binValue1, binValue2, binNullValue ) );

        assertFalse( entry.contains( atDC, strValue3 ) );
        assertFalse( entry.contains( atDC, strNullValue ) );
        assertFalse( entry.contains( atPwd, binValue3 ) );
    }


    /**
     * Test for method contains( EntryAttribute... )
     */
    @Test
    public void testContainsEntryAttributeArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        Attribute attrOC = new DefaultAttribute( atOC, "top", "person" );
        Attribute attrCN = new DefaultAttribute( atCN, "test1", "test2" );
        Attribute attrSN = new DefaultAttribute( atSN, "Test1", "Test2" );
        Attribute attrPWD = new DefaultAttribute( atPwd, BYTES1, BYTES2 );

        assertFalse( entry.contains( attrOC, attrCN ) );

        entry.add( attrOC, attrCN );

        assertTrue( entry.contains( attrOC, attrCN ) );
        assertFalse( entry.contains( attrOC, attrCN, attrSN ) );

        entry.add( attrSN, attrPWD );

        assertTrue( entry.contains( attrSN, attrPWD ) );

        assertFalse( entry.contains( ( Attribute ) null ) );
        entry.clear();
        assertTrue( entry.contains( ( Attribute ) null ) );
    }


    /**
     * Test for method contains( String, byte[]... )
     */
    @Test
    public void testContainsStringByteArrayArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        assertFalse( entry.contains( ( String ) null, BYTES3 ) );
        assertFalse( entry.containsAttribute( "objectClass" ) );

        Attribute attrPWD = new DefaultAttribute( atPwd, BYTES1, ( byte[] ) null, BYTES2 );

        entry.add( attrPWD );

        assertTrue( entry.contains( "  userPASSWORD  ", BYTES1, BYTES2 ) );
        assertTrue( entry.contains( "  userPASSWORD  ", ( byte[] ) null ) );

        assertFalse( entry.contains( "  userPASSWORD  ", "ab", "b" ) );
        assertFalse( entry.contains( "  userPASSWORD  ", BYTES3 ) );
        assertFalse( entry.contains( "  userASSWORD  ", BYTES3 ) );
    }


    /**
     * Test for method contains( String, String... )
     */
    @Test
    public void testContainsStringStringArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        assertFalse( entry.contains( ( String ) null, "test" ) );
        assertFalse( entry.containsAttribute( "objectClass" ) );

        Attribute attrEMail = new DefaultAttribute( atEMail, "test1", ( String ) null, "test2" );

        entry.add( attrEMail );

        assertTrue( entry.contains( "  EMAIL  ", "test1", "test2" ) );

        assertTrue( entry.contains( "  EMAIL  ", ( String ) null ) );
        assertFalse( entry.contains( "  EMAIL  ", BYTES1, BYTES2 ) );
        assertFalse( entry.contains( "  EMAIL  ", "test3" ) );
        assertFalse( entry.contains( "  EMMAIL  ", "test3" ) );
    }


    /**
     * Test for method contains( String, Value<?>... )
     */
    @Test
    public void testContainsStringValueArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        assertFalse( entry.contains( ( String ) null, "test" ) );
        assertFalse( entry.containsAttribute( "objectClass" ) );

        Attribute attrEMail = new DefaultAttribute( atEMail, "test1", "test2", ( String ) null );
        Attribute attrPWD = new DefaultAttribute( atPwd, BYTES1, BYTES2, ( byte[] ) null );

        entry.add( attrEMail, attrPWD );

        Value<String> strValue1 = new StringValue( atEMail, "test1" );
        Value<String> strValue2 = new StringValue( atEMail, "test2" );
        Value<String> strValue3 = new StringValue( atEMail, "test3" );
        Value<String> strNullValue = new StringValue( atEMail, null );

        Value<byte[]> binValue1 = new BinaryValue( atPwd, BYTES1 );
        Value<byte[]> binValue2 = new BinaryValue( atPwd, BYTES2 );
        Value<byte[]> binValue3 = new BinaryValue( atPwd, BYTES3 );
        Value<byte[]> binNullValue = new BinaryValue( atPwd, null );

        assertTrue( entry.contains( "EMail", strValue1, strValue2 ) );
        assertTrue( entry.contains( "userpassword", binValue1, binValue2, binNullValue ) );

        assertFalse( entry.contains( "email", strValue3 ) );
        assertTrue( entry.contains( "email", strNullValue ) );
        assertFalse( entry.contains( "UserPassword", binValue3 ) );
    }


    /**
     * Test method for containsAttribute( AttributeType )
     */
    @Test
    public void testContainsAttributeAttributeType() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        assertFalse( entry.containsAttribute( atOC ) );

        Attribute attrOC = new DefaultAttribute( atOC, "top", "person" );
        Attribute attrCN = new DefaultAttribute( atCN, "test1", "test2" );
        Attribute attrSN = new DefaultAttribute( atSN, "Test1", "Test2" );
        Attribute attrPWD = new DefaultAttribute( atPwd, BYTES1, BYTES2 );

        entry.add( attrOC, attrCN, attrSN, attrPWD );

        assertTrue( entry.containsAttribute( atOC ) );
        assertTrue( entry.containsAttribute( atCN ) );
        assertTrue( entry.containsAttribute( atSN ) );
        assertTrue( entry.containsAttribute( atPwd ) );

        entry.clear();

        assertFalse( entry.containsAttribute( atOC ) );
        assertFalse( entry.containsAttribute( atCN ) );
        assertFalse( entry.containsAttribute( atSN ) );
        assertFalse( entry.containsAttribute( atPwd ) );
    }


    /**
     * Test method for containsAttribute( String )
     */
    @Test
    public void testContainsAttributeString() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        assertFalse( entry.containsAttribute( "objectClass" ) );

        Attribute attrOC = new DefaultAttribute( atOC, "top", "person" );
        Attribute attrCN = new DefaultAttribute( atCN, "test1", "test2" );
        Attribute attrSN = new DefaultAttribute( atSN, "Test1", "Test2" );
        Attribute attrPWD = new DefaultAttribute( atPwd, BYTES1, BYTES2 );

        entry.add( attrOC, attrCN, attrSN, attrPWD );

        assertTrue( entry.containsAttribute( "OBJECTCLASS", " cn ", "Sn", "  userPASSWORD  " ) );

        entry.clear();

        assertFalse( entry.containsAttribute( "OBJECTCLASS" ) );
        assertFalse( entry.containsAttribute( " cn " ) );
        assertFalse( entry.containsAttribute( "Sn" ) );
        assertFalse( entry.containsAttribute( "  userPASSWORD  " ) );
        assertFalse( entry.containsAttribute( "  userASSWORD  " ) );
    }


    /**
     * Test method for equals()
     */
    @Test
    public void testEqualsObject() throws Exception
    {
        Entry entry1 = new DefaultEntry( schemaManager );
        Entry entry2 = new DefaultEntry( schemaManager );

        assertEquals( entry1, entry2 );

        entry1.setDn( EXAMPLE_DN );
        assertNotSame( entry1, entry2 );

        entry2.setDn( EXAMPLE_DN );
        assertEquals( entry1, entry2 );

        Attribute attrOC = new DefaultAttribute( "objectClass", atOC, "top", "person" );
        Attribute attrCN = new DefaultAttribute( "cn", atCN, "test1", "test2" );
        Attribute attrSN = new DefaultAttribute( "sn", atSN, "Test1", "Test2" );
        Attribute attrPWD = new DefaultAttribute( "userPassword", atPwd, BYTES1, BYTES2 );

        entry1.put( attrOC, attrCN, attrSN, attrPWD );
        entry2.put( attrOC, attrCN, attrSN );
        assertNotSame( entry1, entry2 );

        entry2.put( attrPWD );
        assertEquals( entry1, entry2 );

        Attribute attrL1 = new DefaultAttribute( "l", atL, "Paris", "New-York" );
        Attribute attrL2 = new DefaultAttribute( "l", atL, "Paris", "Tokyo" );

        entry1.put( attrL1 );
        entry2.put( attrL1 );
        assertEquals( entry1, entry2 );

        entry1.add( "l", "London" );
        assertNotSame( entry1, entry2 );

        entry2.add( attrL2 );
        assertNotSame( entry1, entry2 );

        entry1.clear();
        entry2.clear();
        assertEquals( entry1, entry2 );
    }


    /**
     * Test method for getAttributeTypes()
     */
    @Test
    public void testGetAttributeTypes() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        assertEquals( 0, entry.getAttributes().size() );

        Attribute attrOC = new DefaultAttribute( atOC, "top", "person" );
        Attribute attrCN = new DefaultAttribute( atCN, "test1", "test2" );
        Attribute attrSN = new DefaultAttribute( atSN, "Test1", "Test2" );
        Attribute attrPWD = new DefaultAttribute( atPwd, BYTES1, BYTES2 );

        entry.add( attrOC, attrCN, attrSN, attrPWD );

        Collection<Attribute> attributes = entry.getAttributes();

        assertEquals( 4, attributes.size() );
        Set<AttributeType> expected = new HashSet<AttributeType>();
        expected.add( atOC );
        expected.add( atCN );
        expected.add( atSN );
        expected.add( atPwd );
        expected.add( atC );

        for ( Attribute attribute : attributes )
        {
            AttributeType attributeType = attribute.getAttributeType();

            assertTrue( expected.contains( attributeType ) );
        }
    }


    /**
     * Test method for get( AttributeType )
     */
    @Test
    public void testGetAttributeType() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        assertNull( entry.get( atCN ) );
        assertNull( entry.get( ( AttributeType ) null ) );

        Attribute attrOC = new DefaultAttribute( atOC, "top", "person" );
        Attribute attrCN = new DefaultAttribute( atCN, "test1", "test2" );
        Attribute attrSN = new DefaultAttribute( atSN, "Test1", "Test2" );
        Attribute attrPWD = new DefaultAttribute( atPwd, BYTES1, BYTES2 );

        entry.add( attrOC, attrCN, attrSN, attrPWD );

        assertNotNull( entry.get( atCN ) );

        assertEquals( attrCN, entry.get( atCN ) );
        assertEquals( attrOC, entry.get( atOC ) );
        assertEquals( attrSN, entry.get( atSN ) );
        assertEquals( attrPWD, entry.get( atPwd ) );
    }


    /**
     * Test method for get( String )
     */
    @Test
    public void testGetString() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        assertNull( entry.get( "cn" ) );
        assertNull( entry.get( "badId" ) );

        Attribute attrOC = new DefaultAttribute( atOC, "top", "person" );
        Attribute attrCN = new DefaultAttribute( atCN, "test1", "test2" );
        Attribute attrSN = new DefaultAttribute( atSN, "Test1", "Test2" );
        Attribute attrPWD = new DefaultAttribute( atPwd, BYTES1, BYTES2 );

        entry.add( attrOC, attrCN, attrSN, attrPWD );

        assertNotNull( entry.get( "CN" ) );
        assertNotNull( entry.get( " commonName " ) );
        assertNotNull( entry.get( "2.5.4.3" ) );

        assertEquals( attrCN, entry.get( "2.5.4.3" ) );
        assertEquals( attrOC, entry.get( " OBJECTCLASS" ) );
        assertEquals( attrSN, entry.get( "sn" ) );
        assertEquals( attrPWD, entry.get( "  userPassword  " ) );
    }


    /**
     * Test method for getDN()
     */
    @Test
    public void testGetDn() throws LdapInvalidDnException
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        assertEquals( EXAMPLE_DN, entry.getDn() );

        Dn testDn = new Dn( schemaManager, "cn=test" );
        entry.setDn( testDn );

        assertEquals( testDn, entry.getDn() );
    }


    /**
     * Test method for hashcode()
     */
    @Test
    public void testHashCode() throws InvalidNameException, Exception
    {
        Entry entry1 = new DefaultEntry( schemaManager, EXAMPLE_DN );
        Entry entry2 = new DefaultEntry( schemaManager, EXAMPLE_DN );

        assertEquals( entry1.hashCode(), entry2.hashCode() );

        entry2.setDn( new Dn( schemaManager, "ou=system,dc=com" ) );
        assertNotSame( entry1.hashCode(), entry2.hashCode() );

        entry2.setDn( EXAMPLE_DN );
        assertEquals( entry1.hashCode(), entry2.hashCode() );

        Attribute attrOC = new DefaultAttribute( "objectClass", atOC, "top", "person" );
        Attribute attrCN = new DefaultAttribute( "cn", atCN, "test1", "test2" );
        Attribute attrSN = new DefaultAttribute( "sn", atSN, "Test1", "Test2" );
        Attribute attrPWD = new DefaultAttribute( "userPassword", atPwd, BYTES1, BYTES2 );

        entry1.add( attrOC, attrCN, attrSN, attrPWD );
        entry2.add( attrOC, attrCN, attrSN, attrPWD );

        assertEquals( entry1.hashCode(), entry2.hashCode() );

        Entry entry3 = new DefaultEntry( schemaManager, EXAMPLE_DN );
        entry3.add( attrOC, attrSN, attrCN, attrPWD );

        assertEquals( entry1.hashCode(), entry3.hashCode() );
    }


    /**
     * Test method for hasObjectClass( EntryAttribute )
     */
    @Test
    public void testHasObjectClassEntryAttribute() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        Attribute attrOC = new DefaultAttribute( atOC, "top", "person" );

        assertFalse( entry.contains( attrOC ) );
        assertFalse( entry.hasObjectClass( attrOC ) );

        entry.add( attrOC );

        assertTrue( entry.hasObjectClass( attrOC ) );

        Attribute attrOC2 = new DefaultAttribute( atOC, "person" );
        assertTrue( entry.hasObjectClass( attrOC2 ) );

        Attribute attrOC3 = new DefaultAttribute( atOC, "inetOrgPerson" );
        assertFalse( entry.hasObjectClass( attrOC3 ) );
        assertFalse( entry.hasObjectClass( ( Attribute ) null ) );

        Attribute attrCN = new DefaultAttribute( atCN, "top" );
        assertFalse( entry.hasObjectClass( attrCN ) );
    }


    /**
     * Test method for hasObjectClass( String )
     */
    @Test
    public void testHasObjectClassString() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        assertFalse( entry.containsAttribute( "objectClass" ) );
        assertFalse( entry.hasObjectClass( "top" ) );

        entry.add( new DefaultAttribute( atOC, "top", "person" ) );

        assertTrue( entry.hasObjectClass( "top" ) );
        assertTrue( entry.hasObjectClass( "person" ) );
        assertFalse( entry.hasObjectClass( "inetorgperson" ) );
        assertFalse( entry.hasObjectClass( ( String ) null ) );
        assertFalse( entry.hasObjectClass( "" ) );
    }


    /**
     * Test method for isValid()
     */
    @Test
    public void testIsValid()
    {
        // @TODO Implement me !
        assertTrue( true );
    }


    /**
     * Test method for isValid( AttributeType )
     */
    @Test
    public void testIsValidAttributeType()
    {
        // @TODO Implement me !
        assertTrue( true );
    }


    /**
     * Test method for isValid( String )
     */
    @Test
    public void testIsValidString()
    {
        // @TODO Implement me !
        assertTrue( true );
    }


    /**
     * Test method for Iterator()
     */
    @Test
    public void testIterator() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        Attribute attrOC = new DefaultAttribute( atOC, "top", "person" );
        Attribute attrCN = new DefaultAttribute( atCN, "test1", "test2" );
        Attribute attrSN = new DefaultAttribute( atSN, "Test1", "Test2" );
        Attribute attrPWD = new DefaultAttribute( atPwd, BYTES1, BYTES2 );

        entry.put( attrOC, attrCN, attrSN, attrPWD );

        Iterator<Attribute> iterator = entry.iterator();

        assertTrue( iterator.hasNext() );

        Set<AttributeType> expectedIds = new HashSet<AttributeType>();
        expectedIds.add( atOC );
        expectedIds.add( atCN );
        expectedIds.add( atSN );
        expectedIds.add( atPwd );

        while ( iterator.hasNext() )
        {
            Attribute attribute = iterator.next();

            AttributeType attributeType = attribute.getAttributeType();
            assertTrue( expectedIds.contains( attributeType ) );
            expectedIds.remove( attributeType );
        }

        assertEquals( 0, expectedIds.size() );
    }


    //-------------------------------------------------------------------------
    // Test the Put methods
    //-------------------------------------------------------------------------
    /**
     * Test for method put( AttributeType, byte[]... )
     */
    @Test
    public void testPutAttributeTypeByteArrayArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        try
        {
            entry.put( ( AttributeType ) null, BYTES1 );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        entry.put( atPwd, ( byte[] ) null );
        assertEquals( 1, entry.size() );
        assertTrue( entry.containsAttribute( atPwd ) );
        assertTrue( entry.contains( atPwd, ( byte[] ) null ) );

        Attribute replaced = entry.put( atPwd, BYTES1, BYTES2, BYTES1 );
        assertNotNull( replaced );
        assertEquals( atPwd, replaced.getAttributeType() );
        assertTrue( replaced.contains( ( byte[] ) null ) );
        assertEquals( 1, entry.size() );
        assertTrue( entry.contains( atPwd, BYTES1, BYTES2 ) );
        assertFalse( entry.contains( atPwd, BYTES3 ) );
        assertEquals( 2, entry.get( atPwd ).size() );

        replaced = entry.put( atPwd, "test" );
        assertNotNull( replaced );
        assertTrue( replaced.contains( BYTES1, BYTES2 ) );

        Attribute attribute = entry.get( atPwd );
        assertEquals( 1, attribute.size() );
        assertTrue( attribute.contains( "test".getBytes() ) );
    }


    /**
     * Test for method put( AttributeType, String... )
     */
    @Test
    public void testPutAttributeTypeStringArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        try
        {
            entry.put( ( AttributeType ) null, "test" );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        entry.put( atEMail, ( String ) null );
        assertEquals( 1, entry.size() );
        assertTrue( entry.containsAttribute( atEMail ) );
        assertTrue( entry.contains( atEMail, ( String ) null ) );

        Attribute replaced = entry.put( atEMail, "test1", "test2", "test1" );
        assertNotNull( replaced );
        assertEquals( atEMail, replaced.getAttributeType() );
        assertTrue( replaced.contains( ( String ) null ) );
        assertEquals( 1, entry.size() );
        assertTrue( entry.contains( atEMail, "test1", "test2" ) );
        assertFalse( entry.contains( atEMail, "test3" ) );
        assertEquals( 2, entry.get( atEMail ).size() );

        replaced = entry.put( atEMail, BYTES1 );
        assertNotNull( replaced );
        assertTrue( replaced.contains( "test1", "test2" ) );

        Attribute attribute = entry.get( atEMail );
        assertEquals( 0, attribute.size() );
    }


    /**
     * Test for method put( AttributeType, Value<?>... )
     */
    @Test
    public void testPutAttributeTypeValueArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        Value<String> strValue1 = new StringValue( atDC, "test1" );
        Value<String> strValue2 = new StringValue( atDC, "test2" );
        Value<String> strValue3 = new StringValue( atDC, "test3" );
        Value<String> strNullValue = new StringValue( atDC, null );

        Value<byte[]> binValue1 = new BinaryValue( atPwd, BYTES1 );

        try
        {
            entry.put( ( AttributeType ) null, strValue1 );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        entry.put( atDC, strNullValue );
        assertEquals( 1, entry.size() );
        assertTrue( entry.containsAttribute( atDC ) );
        assertTrue( entry.contains( atDC, ( String ) null ) );

        Attribute replaced = entry.put( atDC, strValue1, strValue2, strValue1 );
        assertNotNull( replaced );
        assertEquals( atDC, replaced.getAttributeType() );
        assertTrue( replaced.contains( ( String ) null ) );
        assertEquals( 1, entry.size() );
        assertTrue( entry.contains( atDC, strValue1, strValue2 ) );
        assertFalse( entry.contains( atDC, strValue3 ) );
        assertEquals( 2, entry.get( atDC ).size() );

        replaced = entry.put( atDC, binValue1 );
        assertNotNull( replaced );
        assertTrue( replaced.contains( strValue1, strValue2 ) );

        Attribute attribute = entry.get( atDC );
        assertEquals( 0, attribute.size() );
    }


    /**
     * Test for method put( EntryAttribute...)
     */
    @Test
    public void testPutEntryAttribute() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        Attribute oc = new DefaultAttribute( atObjectClass, "top", "person" );
        Attribute cn = new DefaultAttribute( atCN, "test1", "test2" );
        Attribute sn = new DefaultAttribute( atSN, "Test1", "Test2" );
        Attribute up = new DefaultAttribute( atPwd, BYTES1, BYTES2 );
        Attribute c = new DefaultAttribute( atEMail, "FR", "US" );

        List<Attribute> removed = entry.put( oc, cn, sn, c );

        assertEquals( 4, entry.size() );
        assertEquals( 0, removed.size() );
        assertTrue( entry.containsAttribute( "ObjectClass" ) );
        assertTrue( entry.containsAttribute( "CN" ) );
        assertTrue( entry.containsAttribute( "  sn  " ) );
        assertTrue( entry.containsAttribute( " email  " ) );

        Attribute attr = entry.get( "objectclass" );
        assertEquals( 2, attr.size() );

        Attribute c2 = new DefaultAttribute( atEMail, "UK", "DE" );
        removed = entry.put( c2, up );
        assertEquals( 1, removed.size() );
        assertEquals( c, removed.get( 0 ) );
        assertTrue( removed.get( 0 ).contains( "FR" ) );
        assertTrue( removed.get( 0 ).contains( "US" ) );

        assertEquals( 5, entry.size() );

        assertTrue( entry.containsAttribute( "userPassword" ) );
        assertTrue( entry.containsAttribute( " email " ) );

        Attribute attrC = entry.get( "email" );
        assertEquals( 2, attrC.size() );
        assertTrue( attrC.contains( "UK", "DE" ) );

        c2.clear();
        entry.put( c2 );
        assertEquals( 5, entry.size() );
        attrC = entry.get( "email" );
        assertEquals( 0, attrC.size() );
    }


    /**
     * Test for method put( String, AttributeType, byte[]... )
     */
    @Test
    public void testPutStringAttributeTypeByteArrayArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        try
        {
            entry.put( ( String ) null, ( AttributeType ) null, BYTES1 );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        try
        {
            entry.put( " ", ( AttributeType ) null, BYTES1 );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        try
        {
            entry.put( "badAttr", ( AttributeType ) null, BYTES1 );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        try
        {
            entry.put( "badAttr", atPwd, BYTES1 );
            fail();
        }
        catch ( LdapNoSuchAttributeException nsae )
        {
            assertTrue( true );
        }

        entry.put( "UserPassword", atPwd, ( byte[] ) null );
        assertEquals( 1, entry.size() );
        assertTrue( entry.containsAttribute( atPwd ) );
        assertTrue( entry.contains( atPwd, ( byte[] ) null ) );

        assertEquals( "UserPassword", entry.get( atPwd ).getUpId() );

        Attribute replaced = entry.put( "USERpassword ", atPwd, BYTES1, BYTES2, BYTES1 );
        assertNotNull( replaced );
        assertEquals( atPwd, replaced.getAttributeType() );
        assertTrue( replaced.contains( ( byte[] ) null ) );
        assertEquals( 1, entry.size() );
        assertTrue( entry.contains( atPwd, BYTES1, BYTES2 ) );
        assertFalse( entry.contains( atPwd, BYTES3 ) );
        assertEquals( 2, entry.get( atPwd ).size() );
        assertEquals( "USERpassword ", entry.get( atPwd ).getUpId() );

        replaced = entry.put( "userpassword", atPwd, "test" );
        assertNotNull( replaced );
        assertTrue( replaced.contains( BYTES1, BYTES2 ) );
        assertEquals( "userpassword", entry.get( atPwd ).getUpId() );

        Attribute attribute = entry.get( atPwd );
        assertEquals( 1, attribute.size() );
    }


    /**
     * Test for method put( String, AttributeType, String... )
     */
    @Test
    public void testPutStringAttributeTypeStringArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        try
        {
            entry.put( ( String ) null, ( AttributeType ) null, "test" );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        try
        {
            entry.put( " ", ( AttributeType ) null, "test" );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        try
        {
            entry.put( "badAttr", ( AttributeType ) null, "test" );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        try
        {
            entry.put( "badAttr", atCN, "test" );
            fail();
        }
        catch ( LdapNoSuchAttributeException nsae )
        {
            assertTrue( true );
        }

        entry.put( "EMail", atEMail, ( String ) null );
        assertEquals( 1, entry.size() );
        assertTrue( entry.containsAttribute( atEMail ) );
        assertTrue( entry.contains( atEMail, ( String ) null ) );
        assertEquals( "EMail", entry.get( atEMail ).getUpId() );

        Attribute replaced = entry.put( "eMail", atEMail, "test1", "test2", "test1" );
        assertNotNull( replaced );
        assertEquals( atEMail, replaced.getAttributeType() );
        assertEquals( "eMail", entry.get( atEMail ).getUpId() );
        assertTrue( replaced.contains( ( String ) null ) );
        assertEquals( 1, entry.size() );
        assertTrue( entry.contains( atEMail, "test1", "test2" ) );
        assertFalse( entry.contains( atEMail, "test3" ) );
        assertEquals( 2, entry.get( atEMail ).size() );

        replaced = entry.put( "1.2.840.113549.1.9.1", atEMail, BYTES1 );
        assertNotNull( replaced );
        assertTrue( replaced.contains( "test1", "test2" ) );
        assertEquals( "1.2.840.113549.1.9.1", entry.get( atEMail ).getUpId() );

        Attribute attribute = entry.get( atEMail );
        assertEquals( 0, attribute.size() );
    }


    /**
     * Test for method put( String, AttributeType, Value<?>... )
     */
    @Test
    public void testPutStringAttributeTypeValueArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        Value<String> strValue1 = new StringValue( atDC, "test1" );
        Value<String> strValue2 = new StringValue( atDC, "test2" );
        Value<String> strValue3 = new StringValue( atDC, "test3" );
        Value<String> strNullValue = new StringValue( atDC, null );

        Value<byte[]> binValue1 = new BinaryValue( atPwd, BYTES1 );

        try
        {
            entry.put( ( String ) null, ( AttributeType ) null, strValue1 );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        try
        {
            entry.put( " ", ( AttributeType ) null, strValue1 );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        try
        {
            entry.put( "badAttr", ( AttributeType ) null, strValue1 );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        try
        {
            entry.put( "badAttr", atDC, strValue1 );
            fail();
        }
        catch ( LdapNoSuchAttributeException nsae )
        {
            assertTrue( true );
        }

        entry.put( "Dc", atDC, strNullValue );
        assertEquals( 1, entry.size() );
        assertTrue( entry.containsAttribute( atDC ) );
        assertTrue( entry.contains( atDC, ( String ) null ) );
        assertEquals( "Dc", entry.get( atDC ).getUpId() );

        Attribute replaced = entry.put( "domainComponent", atDC, strValue1, strValue2, strValue1 );
        assertNotNull( replaced );
        assertEquals( atDC, replaced.getAttributeType() );
        assertTrue( replaced.contains( ( String ) null ) );
        assertEquals( 1, entry.size() );
        assertTrue( entry.contains( atDC, strValue1, strValue2 ) );
        assertFalse( entry.contains( atDC, strValue3 ) );
        assertEquals( 2, entry.get( atDC ).size() );
        assertEquals( "domainComponent", entry.get( atDC ).getUpId() );

        replaced = entry.put( "0.9.2342.19200300.100.1.25", atDC, binValue1 );
        assertNotNull( replaced );
        assertTrue( replaced.contains( strValue1, strValue2 ) );

        Attribute attribute = entry.get( atDC );
        assertEquals( 0, attribute.size() );
        assertEquals( "0.9.2342.19200300.100.1.25", entry.get( atDC ).getUpId() );
    }


    /**
     * Test method for put( String, byte[]... )
     */
    @Test
    public void testPutStringByteArrayArray()
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        try
        {
            entry.put( ( String ) null, BYTES1 );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        try
        {
            entry.put( "   ", BYTES1 );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        try
        {
            entry.put( "userAssword", BYTES1 );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        Attribute replaced = entry.put( "userPassword", ( byte[] ) null );
        assertNull( replaced );
        assertEquals( 1, entry.size() );
        assertNotNull( entry.get( "userPassword" ) );
        assertEquals( 1, entry.get( "userPassword" ).size() );
        assertNull( entry.get( "userPassword" ).get().getValue() );

        replaced = entry.put( "UserPassword", BYTES1 );
        assertNotNull( replaced );
        assertEquals( atPwd, replaced.getAttributeType() );
        assertTrue( replaced.contains( ( byte[] ) null ) );
        assertEquals( 1, entry.size() );
        assertNotNull( entry.get( "userPassword" ) );
        assertEquals( 1, entry.get( "userPassword" ).size() );
        assertNotNull( entry.get( "userPassword" ).get().getValue() );
        assertTrue( entry.get( "userPassword" ).contains( BYTES1 ) );

        replaced = entry.put( "userPassword", BYTES1, BYTES2, BYTES1 );
        assertNotNull( replaced );
        assertEquals( 1, entry.size() );
        assertNotNull( entry.get( "userPassword" ) );
        assertEquals( 2, entry.get( "USERPassword" ).size() );
        Attribute attribute = entry.get( "userPassword" );
        assertTrue( attribute.contains( BYTES1 ) );
        assertTrue( attribute.contains( BYTES2 ) );
        assertEquals( "2.5.4.35", attribute.getId() );
        assertEquals( "userPassword", attribute.getUpId() );
    }


    /**
     * Test method for put( String, String... )
     */
    @Test
    public void testPutStringStringArray()
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        try
        {
            entry.put( ( String ) null, "test" );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        try
        {
            entry.put( "   ", "test" );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        try
        {
            entry.put( "cnn", "test" );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        Attribute replaced = entry.put( "dc", ( String ) null );
        assertNull( replaced );
        assertEquals( 1, entry.size() );
        assertNotNull( entry.get( "dc" ) );
        assertEquals( 1, entry.get( "dc" ).size() );
        assertNotNull( entry.get( "dc" ).get() );

        replaced = entry.put( "CN", "test" );
        assertNull( replaced );
        assertEquals( 2, entry.size() );
        assertNotNull( entry.get( "cn" ) );
        assertEquals( 1, entry.get( "cn" ).size() );
        assertNotNull( entry.get( "cn" ).get().getValue() );
        assertTrue( entry.get( "cn" ).contains( "test" ) );

        replaced = entry.put( "cN", "test1", "test2", "test1" );
        assertNotNull( replaced );
        assertEquals( "test", replaced.get().getString() );

        assertEquals( 2, entry.size() );
        assertNotNull( entry.get( "cn" ) );
        assertEquals( 2, entry.get( "CN" ).size() );

        Attribute attribute = entry.get( "cn" );
        assertTrue( attribute.contains( "test1" ) );
        assertTrue( attribute.contains( "test2" ) );
        assertEquals( "2.5.4.3", attribute.getId() );
        assertEquals( "cN", attribute.getUpId() );
    }


    /**
     * Test method for put( String, Value<?>... )
     */
    @Test
    public void testPutStringValueArray() throws LdapException
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        Value<String> strValue1 = new StringValue( atDC, "test1" );
        Value<String> strValue2 = new StringValue( atDC, "test2" );
        Value<String> strValue3 = new StringValue( atDC, "test3" );
        Value<String> strNullValue = new StringValue( atDC, null );

        Value<byte[]> binValue1 = new BinaryValue( atPwd, BYTES1 );

        try
        {
            entry.put( ( String ) null, strValue1 );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        try
        {
            entry.put( "   ", strValue1 );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        try
        {
            entry.put( "cnn", strValue1 );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        Attribute replaced = entry.put( "domainComponent", strNullValue );
        assertNull( replaced );
        assertEquals( 1, entry.size() );
        assertNotNull( entry.get( "domainComponent" ) );
        assertEquals( 1, entry.get( "domainComponent" ).size() );
        assertNotNull( entry.get( "domainComponent" ).get() );
        assertNull( entry.get( "domainComponent" ).get().getValue() );
        entry.removeAttributes( "dc" );

        replaced = entry.put( "DC", strValue3 );
        assertNull( replaced );
        assertEquals( 1, entry.size() );
        assertNotNull( entry.get( "dc" ) );
        assertEquals( 1, entry.get( "dc" ).size() );
        assertNotNull( entry.get( "dc" ).get().getValue() );
        assertTrue( entry.get( "dc" ).contains( strValue3 ) );

        replaced = entry.put( "dC", strValue1, strValue2, strValue1 );
        assertNotNull( replaced );
        assertEquals( strValue3, replaced.get() );

        assertEquals( 1, entry.size() );
        assertNotNull( entry.get( "dc" ) );
        assertEquals( 2, entry.get( "DC" ).size() );

        Attribute attribute = entry.get( "dc" );
        assertTrue( attribute.contains( strValue1 ) );
        assertTrue( attribute.contains( strValue2 ) );
        assertEquals( "0.9.2342.19200300.100.1.25", attribute.getId() );
        assertEquals( "dC", attribute.getUpId() );

        // Bin values are not allowed, so the new CN will be empty
        entry.put( "dc", binValue1 );
        assertNull( entry.get( "dc" ).get() );
    }


    /**
     * Test the put( SA... ) method
     */
    @Test
    public void tesPutServerAttributeElipsis() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=test" );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );

        // first test a null SA addition. It should be allowed.
        try
        {
            entry.put( ( Attribute ) null );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        // Adding some serverAttributes
        //AttributeType atCo = registries.lookupAttributeTypeRegistry( "countryName" );
        AttributeType atGN = schemaManager.lookupAttributeTypeRegistry( "givenname" );
        AttributeType atStreet = schemaManager.lookupAttributeTypeRegistry( "2.5.4.9" );

        Attribute sa = new DefaultAttribute( atL, "france" );
        entry.put( sa );

        assertEquals( 1, entry.size() );
        assertNotNull( entry.get( "l" ) );
        assertEquals( "france", entry.get( "l" ).get().getString() );

        Attribute sb = new DefaultAttribute( atC, "countryTest" );
        Attribute sc = new DefaultAttribute( atGN, "test" );
        Attribute sd = new DefaultAttribute( atStreet, "testStreet" );
        entry.put( sb, sc, sd );

        assertEquals( 4, entry.size() );
        assertNotNull( entry.get( atC ) );
        assertEquals( "countryTest", entry.get( atC ).get().getString() );
        assertNotNull( entry.get( atGN ) );
        assertEquals( "test", entry.get( atGN ).get().getString() );
        assertNotNull( entry.get( atStreet ) );
        assertEquals( "testStreet", entry.get( atStreet ).get().getString() );

        // Test a replacement
        Attribute sbb = new DefaultAttribute( atC, "countryTestTest" );
        Attribute scc = new DefaultAttribute( atGN, "testtest" );
        List<Attribute> result = entry.put( sbb, scc );

        assertEquals( 2, result.size() );
        assertEquals( "countryTest", result.get( 0 ).get().getString() );
        assertEquals( "test", result.get( 1 ).get().getString() );
        assertEquals( 4, entry.size() );
        assertNotNull( entry.get( atC ) );
        assertEquals( "countryTestTest", entry.get( atC ).get().getString() );
        assertNotNull( entry.get( atGN ) );
        assertEquals( "testtest", entry.get( atGN ).get().getString() );
        assertNotNull( entry.get( atStreet ) );
        assertEquals( "testStreet", entry.get( atStreet ).get().getString() );

        // test an ObjectClass replacement
        AttributeType OBJECT_CLASS_AT = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.OBJECT_CLASS_AT );
        Attribute oc = new DefaultAttribute( "OBJECTCLASS", OBJECT_CLASS_AT, "person", "inetorgperson" );
        List<Attribute> oldOc = entry.put( oc );

        assertNotNull( oldOc );
        assertEquals( 0, oldOc.size() );

        assertNotNull( entry.get( "objectClass" ) );

        Attribute newOc = entry.get( "objectClass" );

        assertNotNull( newOc );
        assertEquals( OBJECT_CLASS_AT, newOc.getAttributeType() );
        assertEquals( 2, newOc.size() );
        assertEquals( "OBJECTCLASS", newOc.getUpId() );
        assertTrue( newOc.contains( "person", "inetOrgPerson" ) );
    }


    /**
     * Test the put( AT, String... ) method
     */
    @Test
    public void tesPutAtStringElipsis() throws Exception
    {
        Dn dn = new Dn( schemaManager, "dc=test" );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );

        // Test an empty AT
        entry.put( atEMail, ( String ) null );
        assertEquals( 1, entry.size() );
        assertEquals( "email", entry.get( atEMail ).getUpId() );
        assertNull( entry.get( atEMail ).get().getValue() );

        // Check that we can't use invalid arguments
        try
        {
            entry.put( ( AttributeType ) null, ( String ) null );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        // Add a single value
        entry.put( atEMail, "test" );

        assertEquals( 1, entry.size() );
        assertEquals( "email", entry.get( atEMail ).getUpId() );
        assertEquals( 1, entry.get( atEMail ).size() );
        assertEquals( "test", entry.get( atEMail ).get().getString() );

        // Add more than one value
        entry.put( atEMail, "test1", "test2", "test3" );

        assertEquals( 1, entry.size() );
        assertEquals( "email", entry.get( atEMail ).getUpId() );
        assertEquals( 3, entry.get( atEMail ).size() );
        assertTrue( entry.contains( "email", "test1" ) );
        assertTrue( entry.contains( "email", "test2" ) );
        assertTrue( entry.contains( "email", "test3" ) );

        // Add twice the same value
        Attribute sa = entry.put( atEMail, "test1", "test2", "test1" );

        assertEquals( 3, sa.size() );
        assertTrue( sa.contains( "test1", "test2", "test3" ) );
        assertEquals( 1, entry.size() );
        assertEquals( "email", entry.get( atEMail ).getUpId() );
        assertEquals( 2, entry.get( atEMail ).size() );
        assertTrue( entry.contains( "email", "test1" ) );
        assertTrue( entry.contains( "email", "test2" ) );
    }


    /**
     * Test the put( AT, Byte[]... ) method
     */
    @Test
    public void tesPutAtByteElipsis() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=test" );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );

        // Test an empty AT
        entry.put( atPwd, ( byte[] ) null );
        assertEquals( 1, entry.size() );
        assertEquals( "userPassword", entry.get( atPwd ).getUpId() );
        assertNull( entry.get( atPwd ).get().getValue() );

        // Check that we can't use invalid arguments
        try
        {
            entry.put( ( AttributeType ) null, ( byte[] ) null );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        byte[] password = Strings.getBytesUtf8( "test" );
        byte[] test1 = Strings.getBytesUtf8( "test1" );
        byte[] test2 = Strings.getBytesUtf8( "test2" );
        byte[] test3 = Strings.getBytesUtf8( "test3" );

        // Add a single value
        atPwd = schemaManager.lookupAttributeTypeRegistry( "userPassword" );
        entry.put( atPwd, password );

        assertEquals( 1, entry.size() );
        assertEquals( "userPassword", entry.get( atPwd ).getUpId() );
        assertEquals( 1, entry.get( atPwd ).size() );
        assertTrue( Arrays.equals( password, entry.get( atPwd ).get().getBytes() ) );

        // Add more than one value
        entry.put( atPwd, test1, test2, test3 );

        assertEquals( 1, entry.size() );
        assertEquals( "userPassword", entry.get( atPwd ).getUpId() );
        assertEquals( 3, entry.get( atPwd ).size() );
        assertTrue( entry.contains( "userpassword", test1 ) );
        assertTrue( entry.contains( "userpassword", test2 ) );
        assertTrue( entry.contains( "userpassword", test3 ) );

        // Add twice the same value
        Attribute sa = entry.put( atPwd, test1, test2, test1 );

        assertEquals( 3, sa.size() );
        assertTrue( sa.contains( test1, test2, test3 ) );
        assertEquals( 1, entry.size() );
        assertEquals( "userPassword", entry.get( atPwd ).getUpId() );
        assertEquals( 2, entry.get( atPwd ).size() );
        assertTrue( entry.contains( "userpassword", test1 ) );
        assertTrue( entry.contains( "userpassword", test2 ) );
    }


    /**
     * Test the put( AT, Value... ) method
     */
    @Test
    public void tesPutAtSVs() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=test" );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );

        // Adding a null value to an attribute
        entry.put( atDC, ( Value<?> ) null );

        assertEquals( 1, entry.size() );
        assertEquals( "dc", entry.get( atDC ).getUpId() );

        // Check that we can't use invalid arguments
        try
        {
            entry.put( ( AttributeType ) null, ( Value<?> ) null );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        // Add a single value
        atCN = schemaManager.lookupAttributeTypeRegistry( "cn" );
        Value<?> ssv = new StringValue( atCN, "test" );
        entry.put( atCN, ssv );

        assertEquals( 2, entry.size() );
        assertEquals( "cn", entry.get( atCN ).getUpId() );
        assertEquals( 1, entry.get( atCN ).size() );
        assertEquals( "test", entry.get( atCN ).get().getString() );

        // Add more than one value
        entry.put( atCN, new StringValue( atCN, "test1" ), new StringValue( atCN, "test2" ), new StringValue( atCN,
            "test3" ) );

        assertEquals( 2, entry.size() );
        assertEquals( "cn", entry.get( atCN ).getUpId() );
        assertEquals( 3, entry.get( atCN ).size() );
        assertTrue( entry.contains( "cn", "test1" ) );
        assertTrue( entry.contains( "cn", "test2" ) );
        assertTrue( entry.contains( "cn", "test3" ) );

        // Add twice the same value
        Attribute sa = entry.put( atCN, new StringValue( atCN, "test1" ), new StringValue( atCN, "test2" ),
            new StringValue( atCN, "test1" ) );

        assertEquals( 3, sa.size() );
        assertTrue( sa.contains( "test1", "test2", "test3" ) );
        assertEquals( 2, entry.size() );
        assertEquals( "cn", entry.get( atCN ).getUpId() );
        assertEquals( 2, entry.get( atCN ).size() );
        assertTrue( entry.contains( "cn", "test1" ) );
        assertTrue( entry.contains( "cn", "test2" ) );
    }


    /**
     * Test the put( upId, String... ) method
     */
    @Test
    public void tesPutUpIdStringElipsis() throws Exception
    {
        Dn dn = new Dn( schemaManager, "dc=test" );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );

        // Adding a null value should be possible
        entry.put( "email", ( String ) null );

        assertEquals( 1, entry.size() );
        assertEquals( "email", entry.get( atEMail ).getUpId() );
        assertNull( entry.get( atEMail ).get().getValue() );

        // Check that we can't use invalid arguments
        try
        {
            entry.put( ( String ) null, ( String ) null );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        // Add a single value
        entry.put( "email", "test" );

        assertEquals( 1, entry.size() );
        assertEquals( "email", entry.get( atEMail ).getUpId() );
        assertEquals( 1, entry.get( atEMail ).size() );
        assertEquals( "test", entry.get( atEMail ).get().getString() );

        // Add more than one value
        entry.put( "email", "test1", "test2", "test3" );

        assertEquals( 1, entry.size() );
        assertEquals( "email", entry.get( atEMail ).getUpId() );
        assertEquals( 3, entry.get( atEMail ).size() );
        assertTrue( entry.contains( "email", "test1" ) );
        assertTrue( entry.contains( "email", "test2" ) );
        assertTrue( entry.contains( "email", "test3" ) );

        // Add twice the same value
        Attribute sa = entry.put( "email", "test1", "test2", "test1" );

        assertEquals( 3, sa.size() );
        assertTrue( sa.contains( "test1", "test2", "test3" ) );
        assertEquals( 1, entry.size() );
        assertEquals( "email", entry.get( atEMail ).getUpId() );
        assertEquals( 2, entry.get( atEMail ).size() );
        assertTrue( entry.contains( "email", "test1" ) );
        assertTrue( entry.contains( "email", "test2" ) );

        // Check the UpId
        entry.put( "EMail", "test4" );
        assertEquals( "EMail", entry.get( atEMail ).getUpId() );
    }


    /**
     * Test the put( upId, byte[]... ) method
     */
    @Test
    public void tesPutUpIdBytesElipsis() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=test" );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );

        AttributeType atPassword = schemaManager.lookupAttributeTypeRegistry( "userPassword" );

        // Adding a null value should be possible
        entry.put( "userPassword", ( byte[] ) null );
        assertEquals( 1, entry.size() );
        assertEquals( "userPassword", entry.get( atPassword ).getUpId() );
        assertNull( entry.get( atPassword ).get().getValue() );

        // Check that we can't use invalid arguments
        try
        {
            entry.put( ( String ) null, ( String ) null );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        // Add a single value
        byte[] test = Strings.getBytesUtf8( "test" );
        byte[] test1 = Strings.getBytesUtf8( "test1" );
        byte[] test2 = Strings.getBytesUtf8( "test2" );
        byte[] test3 = Strings.getBytesUtf8( "test3" );

        entry.put( "userPassword", test );

        assertEquals( 1, entry.size() );
        assertEquals( "userPassword", entry.get( atPassword ).getUpId() );
        assertEquals( 1, entry.get( atPassword ).size() );
        assertTrue( Arrays.equals( test, entry.get( atPassword ).get().getBytes() ) );

        // Add more than one value
        entry.put( "userPassword", test1, test2, test3 );

        assertEquals( 1, entry.size() );
        assertEquals( "userPassword", entry.get( atPassword ).getUpId() );
        assertEquals( 3, entry.get( atPassword ).size() );
        assertTrue( entry.contains( "userPassword", test1 ) );
        assertTrue( entry.contains( "userPassword", test2 ) );
        assertTrue( entry.contains( "userPassword", test3 ) );

        // Add twice the same value
        Attribute sa = entry.put( "userPassword", test1, test2, test1 );

        assertEquals( 3, sa.size() );
        assertTrue( sa.contains( test1, test2, test3 ) );
        assertEquals( 1, entry.size() );
        assertEquals( "userPassword", entry.get( atPassword ).getUpId() );
        assertEquals( 2, entry.get( atPassword ).size() );
        assertTrue( entry.contains( "userPassword", test1 ) );
        assertTrue( entry.contains( "userPassword", test2 ) );
    }


    /**
     * Test the put( upId, AT, String... ) method
     */
    @Test
    public void tesPutUpIDAtStringElipsis() throws Exception
    {
        Dn dn = new Dn( schemaManager, "dc=test" );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );

        // Test that we get an error when the ID or AT are null
        try
        {
            entry.put( null, ( AttributeType ) null, ( String ) null );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        // Test an empty AT
        entry.put( "email", atEMail, ( String ) null );
        assertEquals( 1, entry.size() );
        assertEquals( "email", entry.get( atEMail ).getUpId() );
        assertTrue( entry.containsAttribute( "email" ) );
        assertNull( entry.get( atEMail ).get().getValue() );

        // Check that we can use a null AttributeType
        entry.put( "email", ( AttributeType ) null, ( String ) null );
        assertEquals( 1, entry.size() );
        assertEquals( "email", entry.get( atEMail ).getUpId() );
        assertTrue( entry.containsAttribute( "email" ) );
        assertNull( entry.get( atEMail ).get().getValue() );

        // Test that we can use a null upId
        entry.put( null, atEMail, ( String ) null );
        assertEquals( 1, entry.size() );
        assertEquals( "email", entry.get( atEMail ).getUpId() );
        assertTrue( entry.containsAttribute( "email" ) );
        assertNull( entry.get( atEMail ).get().getValue() );

        try
        {
            entry.put( "sn", atEMail, ( String ) null );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        // Test that we can add some new attributes with values
        Attribute result = entry.put( "EMail", atEMail, "test1", "test2", "test3" );
        assertNotNull( result );
        assertEquals( "email", result.getUpId() );
        assertEquals( 1, entry.size() );
        assertEquals( "EMail", entry.get( atEMail ).getUpId() );
        assertNotNull( entry.get( atEMail ).get() );
        assertTrue( entry.contains( "email", "test1" ) );
        assertTrue( entry.contains( "EMail", "test2" ) );
        assertTrue( entry.contains( "eMail", "test3" ) );
    }


    /**
     * Test the put( upId, AT, byte[]... ) method
     */
    @Test
    public void tesPutUpIDAtBytesElipsis() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=test" );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );

        AttributeType atPassword = schemaManager.lookupAttributeTypeRegistry( "userPassword" );

        // Test that we get an error when the ID or AT are null
        try
        {
            entry.put( null, ( AttributeType ) null, ( String ) null );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        // Test an empty AT
        entry.put( "userPassword", atPassword, ( byte[] ) null );
        assertEquals( 1, entry.size() );
        assertEquals( "userPassword", entry.get( atPassword ).getUpId() );
        assertTrue( entry.containsAttribute( "userPassword" ) );
        assertNull( entry.get( atPassword ).get().getValue() );

        // Check that we can use a null AttributeType
        entry.put( "userPassword", ( AttributeType ) null, ( byte[] ) null );
        assertEquals( 1, entry.size() );
        assertEquals( "userPassword", entry.get( atPassword ).getUpId() );
        assertTrue( entry.containsAttribute( "userPassword" ) );
        assertNull( entry.get( atPassword ).get().getValue() );

        // Test that we can use a null upId
        entry.put( null, atPassword, ( byte[] ) null );
        assertEquals( 1, entry.size() );
        assertEquals( "userPassword", entry.get( atPassword ).getUpId() );
        assertTrue( entry.containsAttribute( "userPassword" ) );
        assertNull( entry.get( atPassword ).get().getValue() );

        // Test that if we use an upId which is not compatible
        // with the AT, it is changed to the AT default name
        try
        {
            entry.put( "sn", atPassword, ( byte[] ) null );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        assertEquals( "2.5.4.35", entry.get( atPassword ).getId() );

        // Test that we can add some new attributes with values
        byte[] test1 = Strings.getBytesUtf8( "test1" );
        byte[] test2 = Strings.getBytesUtf8( "test2" );
        byte[] test3 = Strings.getBytesUtf8( "test3" );

        Attribute result = entry.put( "UserPassword", atPassword, test1, test2, test3 );
        assertNotNull( result );
        assertEquals( "userPassword", result.getUpId() );
        assertEquals( 1, entry.size() );
        assertEquals( "UserPassword", entry.get( atPassword ).getUpId() );
        assertNotNull( entry.get( atPassword ).get() );
        assertEquals( 3, entry.get( atPassword ).size() );
        assertTrue( entry.contains( "UserPassword", test1 ) );
        assertTrue( entry.contains( "userPassword", test2 ) );
        assertTrue( entry.contains( "2.5.4.35", test3 ) );
    }


    /**
     * Test the put( upId, AT, SV... ) method
     */
    @Test
    public void tesPutUpIDAtSVElipsis() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=test" );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );

        // Test that we get an error when the ID or AT are null
        try
        {
            entry.put( null, ( AttributeType ) null, ( Value<?> ) null );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        // Test an empty AT
        entry.put( "domainComponent", atDC, ( Value<?> ) null );
        assertEquals( 1, entry.size() );
        assertEquals( "domainComponent", entry.get( atDC ).getUpId() );
        assertTrue( entry.containsAttribute( "dc" ) );
        assertNull( entry.get( atDC ).get().getValue() );

        // Check that we can use a null AttributeType
        entry.put( "domainComponent", ( AttributeType ) null, ( Value<?> ) null );

        assertEquals( 1, entry.size() );
        assertEquals( "domainComponent", entry.get( atDC ).getUpId() );
        assertTrue( entry.containsAttribute( "dc" ) );
        assertNull( entry.get( atDC ).get().getValue() );

        // Test that we can use a null upId
        entry.put( null, atDC, ( Value<?> ) null );
        assertEquals( 1, entry.size() );
        assertEquals( "dc", entry.get( atDC ).getUpId() );
        assertTrue( entry.containsAttribute( "dc" ) );
        assertNull( entry.get( atDC ).get().getValue() );

        // Test that we can't use an upId which is not compatible
        // with the AT
        try
        {
            entry.put( "sn", atDC, ( Value<?> ) null );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        // Test that we can add some new attributes with values
        Value<String> test1 = new StringValue( atDC, "test1" );
        Value<String> test2 = new StringValue( atDC, "test2" );
        Value<String> test3 = new StringValue( atDC, "test3" );

        Attribute result = entry.put( "DC", atDC, test1, test2, test3 );
        assertNotNull( result );
        assertEquals( "dc", result.getUpId() );
        assertEquals( 1, entry.size() );
        assertEquals( "DC", entry.get( atDC ).getUpId() );
        assertNotNull( entry.get( atDC ).get() );
        assertTrue( entry.contains( "dc", "test1" ) );
        assertTrue( entry.contains( "DC", "test2" ) );
        assertTrue( entry.contains( "domainComponent", "test3" ) );
    }


    /**
     * Test the put( upId, SV... ) method
     */
    @Test
    public void tesPutUpIDSVElipsis() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=test" );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );

        // Test that we get an error when the ID or AT are null
        try
        {
            entry.put( ( String ) null, ( Value<?> ) null );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        // Test an null valued AT
        entry.put( "domainComponent", ( Value<?> ) null );
        assertEquals( 1, entry.size() );
        assertEquals( "domainComponent", entry.get( atDC ).getUpId() );
        assertTrue( entry.containsAttribute( "dc" ) );
        assertNull( entry.get( atDC ).get().getValue() );

        // Test that we can add some new attributes with values
        Value<String> test1 = new StringValue( atDC, "test1" );
        Value<String> test2 = new StringValue( atDC, "test2" );
        Value<String> test3 = new StringValue( atDC, "test3" );

        Attribute result = entry.put( "DC", test1, test2, test3 );
        assertNotNull( result );
        assertEquals( "domainComponent", result.getUpId() );
        assertEquals( 1, entry.size() );
        assertEquals( "DC", entry.get( atDC ).getUpId() );
        assertNotNull( entry.get( atDC ).get() );
        assertTrue( entry.contains( "dc", "test1" ) );
        assertTrue( entry.contains( "DC", "test2" ) );
        assertTrue( entry.contains( "domainComponent", "test3" ) );
    }


    //-------------------------------------------------------------------------
    // Test the Remove methods
    //-------------------------------------------------------------------------
    /**
     * Test method for remove( AttributeType, byte[]... )
     */
    @Test
    public void testRemoveAttributeTypeByteArrayArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        Attribute attrPWD = new DefaultAttribute( atPwd, BYTES1, ( byte[] ) null, BYTES2 );

        entry.put( attrPWD );
        assertTrue( entry.remove( atPwd, ( byte[] ) null ) );
        assertTrue( entry.remove( atPwd, BYTES1, BYTES2 ) );
        assertFalse( entry.containsAttribute( atPwd ) );

        entry.add( atPwd, BYTES1, ( byte[] ) null, BYTES2 );
        assertTrue( entry.remove( atPwd, ( byte[] ) null ) );
        assertEquals( 2, entry.get( atPwd ).size() );
        assertFalse( entry.contains( atPwd, ( byte[] ) null ) );
        assertTrue( entry.remove( atPwd, BYTES1, BYTES3 ) );
        assertEquals( 1, entry.get( atPwd ).size() );
        assertTrue( entry.contains( atPwd, BYTES2 ) );
        assertFalse( entry.contains( atPwd, BYTES1 ) );

        assertFalse( entry.remove( atPwd, BYTES3 ) );
        assertFalse( entry.remove( atPwd, new byte[]
            { 0x00 } ) );
    }


    /**
     * Test method for remove( AttributeType, String... )
     */
    @Test
    public void testRemoveAttributeTypeStringArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        Attribute attrCN = new DefaultAttribute( atEMail, "test1", ( String ) null, "test2" );

        entry.put( attrCN );
        assertTrue( entry.remove( atEMail, ( String ) null ) );
        assertTrue( entry.remove( atEMail, "test1", "test2" ) );
        assertFalse( entry.containsAttribute( atEMail ) );

        entry.add( atEMail, "test1", ( String ) null, "test2" );
        assertTrue( entry.remove( atEMail, ( String ) null ) );
        assertEquals( 2, entry.get( atEMail ).size() );
        assertFalse( entry.contains( atEMail, ( String ) null ) );
        assertTrue( entry.remove( atEMail, "test1", "test3" ) );
        assertEquals( 1, entry.get( atEMail ).size() );
        assertTrue( entry.contains( atEMail, "test2" ) );
        assertFalse( entry.contains( atEMail, "test1" ) );

        assertFalse( entry.remove( atEMail, "test3" ) );
        assertFalse( entry.remove( atEMail, "test" ) );
    }


    /**
     * Test method for remove( AttributeType, Value<?>... )
     */
    @Test
    public void testRemoveAttributeTypeValueArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        Value<String> strValue1 = new StringValue( atEMail, "test1" );
        Value<String> strValue2 = new StringValue( atEMail, "test2" );
        Value<String> strValue3 = new StringValue( atEMail, "test3" );
        Value<String> strNullValue = new StringValue( atEMail, null );

        Value<byte[]> binValue1 = new BinaryValue( atPwd, BYTES1 );

        Attribute attrPWD = new DefaultAttribute( atEMail, "test1", ( String ) null, "test2" );

        entry.put( attrPWD );
        assertTrue( entry.remove( atEMail, strNullValue ) );
        assertTrue( entry.remove( atEMail, strValue1, strValue2 ) );
        assertFalse( entry.containsAttribute( atEMail ) );

        entry.add( atEMail, strValue1, strNullValue, strValue2 );
        assertTrue( entry.remove( atEMail, strNullValue ) );
        assertEquals( 2, entry.get( atEMail ).size() );
        assertFalse( entry.contains( atEMail, strNullValue ) );
        assertTrue( entry.remove( atEMail, strValue1, strValue3 ) );
        assertEquals( 1, entry.get( atEMail ).size() );
        assertTrue( entry.contains( atEMail, strValue2 ) );
        assertFalse( entry.contains( atEMail, strValue1 ) );

        assertFalse( entry.remove( atEMail, strValue3 ) );
        assertFalse( entry.remove( atEMail, binValue1 ) );
    }


    /**
     * Test method for remove( EntryAttribute... )
     */
    @Test
    public void testRemoveEntryAttribute() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        Attribute attrOC = new DefaultAttribute( atOC, "top", "person" );
        Attribute attrCN = new DefaultAttribute( atCN, "test1", "test2" );
        Attribute attrSN = new DefaultAttribute( atSN, "Test1", "Test2" );
        Attribute attrPWD = new DefaultAttribute( atPwd, BYTES1, BYTES2 );

        entry.put( attrOC, attrCN, attrSN, attrPWD );

        List<Attribute> removed = entry.remove( attrSN, attrPWD );

        assertEquals( 2, removed.size() );
        assertEquals( 2, entry.size() );
        assertTrue( removed.contains( attrSN ) );
        assertTrue( removed.contains( attrPWD ) );
        assertTrue( entry.contains( "objectClass", "top", "person" ) );
        assertTrue( entry.contains( "cn", "test1", "test2" ) );
        assertFalse( entry.containsAttribute( atSN ) );
        assertFalse( entry.containsAttribute( "userPassword" ) );

        removed = entry.remove( attrSN, attrPWD );

        assertEquals( 0, removed.size() );
    }


    /**
     * Test method for removeAttributes( AttributeType... )
     */
    @Test
    public void testRemoveAttributesAttributeTypeArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        Attribute attrOC = new DefaultAttribute( atOC, "top", "person" );
        Attribute attrCN = new DefaultAttribute( atCN, "test1", "test2" );
        Attribute attrSN = new DefaultAttribute( atSN, "Test1", "Test2" );
        Attribute attrPWD = new DefaultAttribute( atPwd, BYTES1, BYTES2 );

        entry.put( attrOC, attrCN, attrSN, attrPWD );

        entry.removeAttributes( atCN, atSN );

        assertFalse( entry.containsAttribute( "cn", "sn" ) );
        assertTrue( entry.containsAttribute( "objectclass", "userpassword" ) );
    }


    /**
     * Test method for removeAttributes( String... )
     */
    @Test
    public void testRemoveAttributesStringArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        Attribute attrOC = new DefaultAttribute( atOC, "top", "person" );
        Attribute attrCN = new DefaultAttribute( atCN, "test1", "test2" );
        Attribute attrSN = new DefaultAttribute( atSN, "Test1", "Test2" );
        Attribute attrPWD = new DefaultAttribute( atPwd, BYTES1, BYTES2 );

        entry.put( attrOC, attrCN, attrSN, attrPWD );

        entry.removeAttributes( "CN", "SN" );

        assertFalse( entry.containsAttribute( "cn", "sn" ) );
        assertTrue( entry.containsAttribute( "objectclass", "userpassword" ) );

        entry.removeAttributes( "badId" );
        entry.removeAttributes( "l" );
        entry.removeAttributes( ( String ) null );
    }


    /**
     * Test method for remove( String, byte[]... )
     */
    @Test
    public void testRemoveStringByteArrayArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        Attribute attrPWD = new DefaultAttribute( atPwd, BYTES1, ( byte[] ) null, BYTES2 );

        assertFalse( entry.remove( ( String ) null, BYTES1 ) );
        assertFalse( entry.remove( " ", BYTES1 ) );
        assertFalse( entry.remove( "badId", BYTES1 ) );

        entry.put( attrPWD );
        assertTrue( entry.remove( "userPassword", ( byte[] ) null ) );
        assertTrue( entry.remove( "UserPassword", BYTES1, BYTES2 ) );
        assertFalse( entry.containsAttribute( atPwd ) );

        entry.add( atPwd, BYTES1, ( byte[] ) null, BYTES2 );
        assertTrue( entry.remove( "userPassword", ( byte[] ) null ) );
        assertEquals( 2, entry.get( atPwd ).size() );
        assertFalse( entry.contains( atPwd, ( byte[] ) null ) );
        assertTrue( entry.remove( "userPassword", BYTES1, BYTES3 ) );
        assertEquals( 1, entry.get( atPwd ).size() );
        assertTrue( entry.contains( atPwd, BYTES2 ) );
        assertFalse( entry.contains( atPwd, BYTES1 ) );

        assertFalse( entry.remove( "userPassword", BYTES3 ) );
        assertFalse( entry.remove( "userPassword", new byte[]
            { 0x00 } ) );
    }


    /**
     * Test method for remove( String, String... )
     */
    @Test
    public void testRemoveStringStringArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        Attribute attrCN = new DefaultAttribute( atEMail, "test1", ( String ) null, "test2" );

        assertFalse( entry.remove( ( String ) null, "test1" ) );
        assertFalse( entry.remove( " ", "test1" ) );
        assertFalse( entry.remove( "badId", "test1" ) );

        entry.put( attrCN );
        assertTrue( entry.remove( "email", ( String ) null ) );
        assertTrue( entry.remove( "eMail", "test1", "test2" ) );
        assertFalse( entry.containsAttribute( atEMail ) );

        entry.add( atEMail, "test1", ( String ) null, "test2" );
        assertTrue( entry.remove( "1.2.840.113549.1.9.1", ( String ) null ) );
        assertEquals( 2, entry.get( atEMail ).size() );
        assertFalse( entry.contains( atEMail, ( byte[] ) null ) );
        assertTrue( entry.remove( "EMAIL", "test1", "test3" ) );
        assertEquals( 1, entry.get( atEMail ).size() );
        assertTrue( entry.contains( atEMail, "test2" ) );
        assertFalse( entry.contains( atEMail, "test1" ) );

        assertFalse( entry.remove( "Email", "test3" ) );
        assertFalse( entry.remove( "eMail", "whatever" ) );
    }


    /**
     * Test method for remove( String, Value<?>... )
     */
    @Test
    public void testRemoveStringValueArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        Value<String> strValue1 = new StringValue( atEMail, "test1" );
        Value<String> strValue2 = new StringValue( atEMail, "test2" );
        Value<String> strValue3 = new StringValue( atEMail, "test3" );
        Value<String> strNullValue = new StringValue( atEMail, null );

        Value<byte[]> binValue1 = new BinaryValue( atPwd, BYTES1 );

        Attribute attrPWD = new DefaultAttribute( atEMail, "test1", ( String ) null, "test2" );

        entry.put( attrPWD );
        assertTrue( entry.remove( "EMail", strNullValue ) );
        assertTrue( entry.remove( "eMail", strValue1, strValue2 ) );
        assertFalse( entry.containsAttribute( atEMail ) );

        entry.add( atEMail, strValue1, strNullValue, strValue2 );
        assertTrue( entry.remove( "1.2.840.113549.1.9.1", strNullValue ) );
        assertEquals( 2, entry.get( atEMail ).size() );
        assertFalse( entry.contains( atEMail, strNullValue ) );
        assertTrue( entry.remove( "  email", strValue1, strValue3 ) );
        assertEquals( 1, entry.get( atEMail ).size() );
        assertTrue( entry.contains( atEMail, strValue2 ) );
        assertFalse( entry.contains( atEMail, strValue1 ) );

        assertFalse( entry.remove( " Email", strValue3 ) );
        assertFalse( entry.remove( "eMail ", binValue1 ) );
    }


    /**
     * Test the remove( upId...) method
     */
    @Test
    public void testRemoveUpIdElipsis() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=test" );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );

        AttributeType atPassword = schemaManager.lookupAttributeTypeRegistry( "userPassword" );

        byte[] b1 = Strings.getBytesUtf8( "test1" );
        byte[] b2 = Strings.getBytesUtf8( "test2" );

        Value<String> test1 = new StringValue( atCN, "test1" );
        Value<String> test2 = new StringValue( atCN, "test2" );

        Value<byte[]> testB1 = new BinaryValue( atPassword, b1 );
        Value<byte[]> testB2 = new BinaryValue( atPassword, b2 );

        // test a removal of an non existing attribute
        entry.removeAttributes( atCN );

        // Test a simple removal
        entry.add( "cN", atCN, test1 );
        assertEquals( 1, entry.size() );
        assertNotNull( entry.get( atCN ) );
        entry.removeAttributes( "CN" );
        assertEquals( 0, entry.size() );
        assertNull( entry.get( atCN ) );

        // Test a removal of many elements
        entry.put( "CN", test1, test2 );
        entry.put( "userPassword", testB1, testB2 );
        assertEquals( 2, entry.size() );
        assertNotNull( entry.get( atCN ) );
        assertNotNull( entry.get( atPassword ) );

        AttributeType OBJECT_CLASS_AT = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.OBJECT_CLASS_AT );

        entry.removeAttributes( "cN", "UsErPaSsWoRd" );
        assertEquals( 0, entry.size() );
        assertNull( entry.get( atCN ) );
        assertNull( entry.get( atPassword ) );
        assertFalse( entry.contains( OBJECT_CLASS_AT, "top" ) );

        // test the removal of a bad Attribute
        entry.put( "CN", test1, test2 );
        entry.put( "userPassword", testB1, testB2 );
        assertEquals( 2, entry.size() );
        assertNotNull( entry.get( atCN ) );
        assertNotNull( entry.get( atPassword ) );

        entry.removeAttributes( "badAttribute" );
    }


    /**
     * Test method for setDN( Dn )
     */
    @Test
    public void testSetDn()
    {
        Entry entry = new DefaultEntry( schemaManager );

        assertEquals( Dn.EMPTY_DN, entry.getDn() );

        entry.setDn( EXAMPLE_DN );
        assertEquals( EXAMPLE_DN, entry.getDn() );
    }


    /**
     * Test for method size()
     */
    @Test
    public void testSize() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        assertEquals( 0, entry.size() );
        entry.add( "ObjectClass", schemaManager.lookupAttributeTypeRegistry( "ObjectClass" ), "top", "person" );
        entry.add( "CN", schemaManager.lookupAttributeTypeRegistry( "Cn" ), "test" );
        entry.add( "SN", schemaManager.lookupAttributeTypeRegistry( "Sn" ), "Test" );

        assertEquals( 3, entry.size() );

        entry.clear();
        assertEquals( 0, entry.size() );
    }


    /**
     * Test a conversion from a Entry to an BasicAttributes
     */
    @Test
    public void testToBasicAttributes() throws InvalidNameException, Exception
    {
        Dn dn = new Dn( schemaManager, "cn=test" );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );

        AttributeType OBJECT_CLASS_AT = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.OBJECT_CLASS_AT );

        entry.put( "objectClass", OBJECT_CLASS_AT, "top", "person", "inetOrgPerson", "organizationalPerson" );
        entry.put( "cn", schemaManager.lookupAttributeTypeRegistry( "cn" ), "test" );

        Attributes attributes = ServerEntryUtils.toBasicAttributes( entry );

        assertNotNull( attributes );
        assertTrue( attributes instanceof BasicAttributes );

        Set<String> expected = new HashSet<String>();
        expected.add( "objectClass" );
        expected.add( "cn" );

        for ( NamingEnumeration<String> ids = attributes.getIDs(); ids.hasMoreElements(); )
        {
            String id = ids.nextElement();

            assertTrue( expected.contains( id ) );
            expected.remove( id );

        }

        // It should be empty
        assertEquals( 0, expected.size() );
    }


    /**
     * Test method for toString().
     */
    @Test
    public void testToString() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        assertEquals( "Entry\n    dn[n]: dc=example,dc=com\n\n", entry.toString() );

        Value<String> strValueTop = new StringValue( "top" );
        Value<String> strValuePerson = new StringValue( "person" );

        Value<byte[]> binValue1 = new BinaryValue( BYTES1 );
        Value<byte[]> binValue2 = new BinaryValue( BYTES2 );
        Value<byte[]> binNullValue = new BinaryValue( ( byte[] ) null );

        entry.put( "ObjectClass", atOC, strValueTop, strValuePerson );
        entry.put( "UserPassword", atPwd, binValue1, binValue2, binNullValue );

        String expected =
            "Entry\n" +
                "    dn[n]: dc=example,dc=com\n" +
                "    ObjectClass: top\n" +
                "    ObjectClass: person\n" +
                "    UserPassword: 0x61 0x62 \n" +
                "    UserPassword: 0x62 \n" +
                "    UserPassword: ''\n";

        assertEquals( expected, entry.toString() );
    }


    /**
     * Test the copy constructor of a Entry
     */
    @Test
    public void testCopyConstructorServerEntry() throws LdapException
    {
        Entry serverEntry = new DefaultEntry( schemaManager );
        serverEntry.add( "cn", "test1", "test2" );
        serverEntry.add( "objectClass", "top", "person" );

        Entry copyEntry = new DefaultEntry( schemaManager, serverEntry );

        assertEquals( copyEntry, serverEntry );
        assertTrue( copyEntry.contains( "objectClass", "top", "person" ) );
        assertTrue( copyEntry.contains( "cn", "test1", "test2" ) );

        serverEntry.removeAttributes( "cn" );

        assertNotSame( copyEntry, serverEntry );
        assertTrue( copyEntry.contains( "objectClass", "top", "person" ) );
        assertTrue( copyEntry.contains( "cn", "test1", "test2" ) );
    }


    /**
     * Test the copy constructor of a ClientEntry
     */
    @Test
    public void testCopyConstructorClientEntry() throws LdapException
    {
        Entry clientEntry = new DefaultEntry();
        clientEntry.setDn( new Dn( schemaManager, "ou=system" ) );
        clientEntry.add( "cn", "test1", "test2" );
        clientEntry.add( "objectClass", "top", "person" );

        Entry copyEntry = new DefaultEntry( schemaManager, clientEntry );

        assertTrue( copyEntry instanceof Entry );
        assertTrue( copyEntry.contains( "objectClass", "top", "person" ) );
        assertTrue( copyEntry.contains( "cn", "test1", "test2" ) );

        clientEntry.removeAttributes( "cn" );

        assertTrue( copyEntry.contains( "objectClass", "top", "person" ) );
        assertTrue( copyEntry.contains( "cn", "test1", "test2" ) );
    }
}
