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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;

import org.apache.directory.junit.tools.Concurrent;
import org.apache.directory.junit.tools.ConcurrentJunitRunner;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.BinaryValue;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.DefaultEntryAttribute;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.EntryAttribute;
import org.apache.directory.shared.ldap.model.entry.StringValue;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.model.exception.LdapNoSuchAttributeException;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.shared.util.exception.Exceptions;
import org.apache.directory.shared.util.Strings;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test the DefaultEntry class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrent()
public class SchemaAwareEntryTest
{
    private static final byte[] BYTES1 = new byte[]
        { 'a', 'b' };
    private static final byte[] BYTES2 = new byte[]
        { 'b' };
    private static final byte[] BYTES3 = new byte[]
        { 'c' };

    private static LdifSchemaLoader loader;
    private static SchemaManager schemaManager;

    private static AttributeType atObjectClass;
    private static AttributeType atCN;
    private static AttributeType atDC;
    private static AttributeType atSN;
    private static AttributeType atC;
    private static AttributeType atL;
    private static AttributeType atOC;

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
            fail( "Schema load failed : " + Exceptions.printErrors(errors) );
        }

        atObjectClass = schemaManager.lookupAttributeTypeRegistry( "objectClass" );
        atCN = schemaManager.lookupAttributeTypeRegistry( "cn" );
        atDC = schemaManager.lookupAttributeTypeRegistry( "dc" );
        atC = schemaManager.lookupAttributeTypeRegistry( "c" );
        atL = schemaManager.lookupAttributeTypeRegistry( "l" );
        atOC = schemaManager.lookupAttributeTypeRegistry( "objectClass" );
        atSN = schemaManager.lookupAttributeTypeRegistry( "sn" );
        atPwd = schemaManager.lookupAttributeTypeRegistry( "userpassword" );

        EXAMPLE_DN = new Dn( "dc=example,dc=com" );
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


    /**
     * Test for method DefaultEntry( registries, Dn, AttributeType... )
     */
    @Test
    public void testDefaultClientEntryRegistriesDNAttributeTypeArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN, atOC, atPwd, atCN );
        assertNotNull( entry );
        assertEquals( EXAMPLE_DN, entry.getDn() );
        assertEquals( 3, entry.size() );
        assertTrue( entry.containsAttribute( atOC ) );
        assertTrue( entry.containsAttribute( atPwd ) );
        assertTrue( entry.containsAttribute( atCN ) );
    }


    /**
     * Test for method DefaultEntry( registries, Dn, AttributeType, upId )
     */
    @Test
    public void testDefaultClientEntryRegistriesDNAttributeTypeUpId() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN, atOC, "  OBJECTCLASS  " );
        assertNotNull( entry );
        assertEquals( EXAMPLE_DN, entry.getDn() );
        assertEquals( 1, entry.size() );
        assertTrue( entry.containsAttribute( atOC ) );
        assertEquals( "objectclass", entry.get( atOC ).getId() );
        assertEquals( "  OBJECTCLASS  ", entry.get( atOC ).getUpId() );
    }


    /**
     * Test for method DefaultEntry( registries, Dn, AttributeType, upId )
     */
    @Test
    public void testDefaultClientEntryRegistriesDNUpIdArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN, "  OBJECTCLASS  ", " Cn " );
        assertNotNull( entry );
        assertEquals( EXAMPLE_DN, entry.getDn() );
        assertEquals( 2, entry.size() );
        assertTrue( entry.containsAttribute( "objectClass" ) );
        assertEquals( "objectclass", entry.get( atOC ).getId() );
        assertEquals( "  OBJECTCLASS  ", entry.get( atOC ).getUpId() );
        assertTrue( entry.containsAttribute( "2.5.4.3" ) );
        assertEquals( "cn", entry.get( atCN ).getId() );
        assertEquals( " Cn ", entry.get( atCN ).getUpId() );
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

        EntryAttribute oc = new DefaultEntryAttribute( atObjectClass, "top", "person" );
        EntryAttribute cn = new DefaultEntryAttribute( atCN, "test1", "test2" );
        EntryAttribute sn = new DefaultEntryAttribute( atSN, "Test1", "Test2" );
        EntryAttribute up = new DefaultEntryAttribute( atPwd, BYTES1, BYTES2 );
        EntryAttribute c = new DefaultEntryAttribute( atC, "FR", "US" );

        entry.add( oc, cn, sn, c );

        assertEquals( 4, entry.size() );
        assertTrue( entry.containsAttribute( "ObjectClass" ) );
        assertTrue( entry.containsAttribute( "CN" ) );
        assertTrue( entry.containsAttribute( "  sn  " ) );
        assertTrue( entry.containsAttribute( " countryName  " ) );

        EntryAttribute attr = entry.get( "objectclass" );
        assertEquals( 2, attr.size() );

        EntryAttribute c2 = new DefaultEntryAttribute( atC, "UK", "DE" );
        entry.add( c2, up );
        assertEquals( 5, entry.size() );

        assertTrue( entry.containsAttribute( "userPassword" ) );
        assertTrue( entry.containsAttribute( " countryName " ) );

        EntryAttribute attrC = entry.get( "countryName" );
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
        EntryAttribute attributePWD = entry.get( "userPassword" );
        assertEquals( 1, attributePWD.size() );
        assertNotNull( attributePWD.get() );
        assertNull( attributePWD.get().get() );

        entry.clear();

        entry.add( "userPassword", BYTES1, BYTES1, BYTES2 );
        assertEquals( 1, entry.size() );
        EntryAttribute attributeJPG = entry.get( "userPassword" );
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

        entry.add( "cn", ( String ) null );
        assertEquals( 1, entry.size() );
        EntryAttribute attributeCN = entry.get( "cn" );

        assertEquals( 0, attributeCN.size() );
        assertNull( attributeCN.get() );

        entry.add( "sn", "test", "test", "TEST" );
        assertEquals( 2, entry.size() );
        EntryAttribute attributeSN = entry.get( "sn" );

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
        Value<String> value = new StringValue( atCN, ( String ) null );

        entry.add( "cn", value );
        assertEquals( 1, entry.size() );
        EntryAttribute attributeCN = entry.get( "cn" );
        assertEquals( 1, attributeCN.size() );
        assertNotNull( attributeCN.get() );
        assertNull( attributeCN.get().get() );

        Value<String> value1 = new StringValue( atCN, "test1" );
        Value<String> value2 = new StringValue( atCN, "test2" );
        Value<String> value3 = new StringValue( atCN, "test1" );

        entry.add( "sn", value1, value2, value3 );
        assertEquals( 2, entry.size() );
        EntryAttribute attributeSN = entry.get( "sn" );
        assertEquals( 2, attributeSN.size() );
        assertNotNull( attributeSN.get() );
        assertTrue( attributeSN.contains( value1 ) );
        assertTrue( attributeSN.contains( value2 ) );

        Value<byte[]> value4 = new BinaryValue( atPwd, BYTES1 );
        entry.add( "l", value1, value4 );
        assertEquals( 3, entry.size() );
        EntryAttribute attributeL = entry.get( "l" );

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

        EntryAttribute attribute = entry.get( atPwd );
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
        assertTrue( entry.contains( atC, "fr", "us" ) );

        entry.add( atC, ( String ) null, "de", "fr" );
        assertEquals( 1, entry.size() );

        EntryAttribute attribute = entry.get( atC );
        assertEquals( 3, attribute.size() );
        assertTrue( attribute.contains( "de" ) );
        assertTrue( attribute.contains( "fr" ) );
        assertFalse( attribute.contains( ( String ) null ) );
        assertTrue( attribute.contains( "us" ) );

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

        Value<String> strValue1 = new StringValue( atCN, "test1" );
        Value<String> strValue2 = new StringValue( atCN, "test2" );
        Value<String> strValue3 = new StringValue( atCN, "test3" );
        Value<String> strNullValue = new StringValue( atCN, null );

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

        entry.add( atCN, strValue1, strValue2, strValue1 );
        entry.add( atPwd, binValue1, binValue2, binValue1 );

        assertEquals( 2, entry.size() );
        assertTrue( entry.contains( atCN, "test1", "test2" ) );
        assertTrue( entry.contains( atPwd, BYTES1, BYTES2 ) );

        entry.add( atCN, strValue3, strNullValue );

        assertEquals( 4, entry.get( atCN ).size() );
        assertTrue( entry.contains( atCN, strNullValue ) );

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
        assertEquals( "userpassword", entry.get( atPwd ).getId() );

        entry.add( "  UserPassword  ", atPwd, ( byte[] ) null, BYTES1 );
        assertEquals( 1, entry.size() );

        EntryAttribute attribute = entry.get( atPwd );
        assertEquals( 3, attribute.size() );
        assertTrue( attribute.contains( BYTES1 ) );
        assertTrue( attribute.contains( BYTES2 ) );
        assertTrue( attribute.contains( ( byte[] ) null ) );
        assertEquals( "  UserPassword  ", attribute.getUpId() );
        assertEquals( "userpassword", attribute.getId() );

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

        entry.add( "DomainComponent", atDC, "test1", "test2" );
        assertEquals( 1, entry.size() );
        assertTrue( entry.contains( atDC, "test1", "test2" ) );
        assertEquals( "DomainComponent", entry.get( atDC ).getUpId() );
        assertEquals( "domaincomponent", entry.get( atDC ).getId() );

        entry.add( "  DC  ", atDC, ( String ) null, "test1" );
        assertEquals( 1, entry.size() );

        EntryAttribute attribute = entry.get( atDC );
        assertEquals( 3, attribute.size() );
        assertTrue( attribute.contains( "test1" ) );
        assertTrue( attribute.contains( ( String ) null ) );
        assertTrue( attribute.contains( "test2" ) );
        assertEquals( "  DC  ", attribute.getUpId() );
        assertEquals( "dc", attribute.getId() );

        entry.clear();

        // Binary values are not allowed
        entry.add( "  DC  ", atDC, BYTES1 );
        assertEquals( 1, entry.size() );
        assertNotNull( entry.get( atDC ) );
        assertEquals( 0, entry.get( atDC ).size() );
    }


    /**
     * Test method for add( String, AttributeType, Value<?>... )
     */
    @Test
    public void testAddStringAttributeTypeValueArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        Value<String> strValue1 = new StringValue( atCN, "test1" );
        Value<String> strValue2 = new StringValue( atCN, "test2" );
        Value<String> strValue3 = new StringValue( atCN, "test3" );
        Value<String> strNullValue = new StringValue( atCN, null );

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

        entry.add( "CN", atCN, strValue1, strValue2, strValue1 );
        entry.add( "UserPassword", atPwd, binValue1, binValue2, binValue1 );

        assertEquals( 2, entry.size() );
        assertTrue( entry.contains( atCN, "test1", "test2" ) );
        assertTrue( entry.contains( atPwd, BYTES1, BYTES2 ) );
        assertEquals( "CN", entry.get( atCN ).getUpId() );
        assertEquals( "cn", entry.get( atCN ).getId() );
        assertEquals( "UserPassword", entry.get( atPwd ).getUpId() );
        assertEquals( "userpassword", entry.get( atPwd ).getId() );

        entry.add( "CN", atCN, strValue3, strNullValue );

        assertEquals( 4, entry.get( atCN ).size() );
        assertTrue( entry.contains( atCN, strNullValue ) );

        entry.add( atCN, binValue3 );
        assertFalse( entry.contains( atCN, binValue3 ) );

        try
        {
            entry.add( "SN", atCN, "test" );
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
        Dn dn = new Dn( "cn=test" );
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
        entry.add( atDC, "test1" );
        assertNotNull( entry.get( atDC ) );
        assertEquals( 1, entry.get( atDC ).size() );
        assertEquals( "test1", entry.get( atDC ).get().getString() );

        // Test some more addition
        entry.add( atDC, "test2", "test3" );
        assertNotNull( entry.get( atDC ) );
        assertEquals( 3, entry.get( atDC ).size() );
        assertTrue( entry.contains( atDC, "test1" ) );
        assertTrue( entry.contains( atDC, "test2" ) );
        assertTrue( entry.contains( atDC, "test3" ) );

        // Test some addition of existing values
        entry.add( atDC, "test2" );
        assertNotNull( entry.get( atDC ) );
        assertEquals( 3, entry.get( atDC ).size() );
        assertTrue( entry.contains( atDC, "test1" ) );
        assertTrue( entry.contains( atDC, "test2" ) );
        assertTrue( entry.contains( atDC, "test3" ) );

        // Test the addition of a null value
        entry.add( atDC, ( String ) null );
        assertNotNull( entry.get( atDC ) );
        assertEquals( 4, entry.get( atDC ).size() );
        assertTrue( entry.contains( atDC, "test1" ) );
        assertTrue( entry.contains( atDC, "test2" ) );
        assertTrue( entry.contains( atDC, "test3" ) );
        assertTrue( entry.contains( atDC, ( String ) null ) );

        entry.clear();

        // Test the addition of a binary value
        byte[] test4 = Strings.getBytesUtf8("test4");

        entry.add( atCN, test4 );
        assertFalse( entry.get( atCN ).contains( test4 ) );
    }


    /**
     * Test the add( AT, byte[]... ) method
     */
    @Test
    public void testAddAtBytesElipsis() throws Exception
    {
        Dn dn = new Dn( "cn=test" );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );

        AttributeType atPassword = schemaManager.lookupAttributeTypeRegistry( "userPassword" );
        AttributeType atJpegPhoto = schemaManager.lookupAttributeTypeRegistry( "jpegPhoto" );

        byte[] test1 = Strings.getBytesUtf8("test1");
        byte[] test2 = Strings.getBytesUtf8("test2");
        byte[] test3 = Strings.getBytesUtf8("test3");

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
        catch ( IllegalArgumentException iae )
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
        byte[] test4 = Strings.getBytesUtf8("test4");

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
        Dn dn = new Dn( "cn=test" );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );

        AttributeType atPassword = schemaManager.lookupAttributeTypeRegistry( "userPassword" );

        byte[] b1 = Strings.getBytesUtf8("test1");
        byte[] b2 = Strings.getBytesUtf8("test2");
        byte[] b3 = Strings.getBytesUtf8("test3");

        Value<String> test1 = new StringValue( atDC, "test1" );
        Value<String> test2 = new StringValue( atDC, "test2" );
        Value<String> test3 = new StringValue( atDC, "test3" );

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

        // Test a simple addition in atDC
        entry.add( atDC, test1 );
        assertNotNull( entry.get( atDC ) );
        assertEquals( 1, entry.get( atDC ).size() );
        assertEquals( "test1", entry.get( atDC ).get().getString() );

        // Test some more addition
        entry.add( atDC, test2, test3 );
        assertNotNull( entry.get( atDC ) );
        assertEquals( 3, entry.get( atDC ).size() );
        assertTrue( entry.contains( atDC, "test1" ) );
        assertTrue( entry.contains( atDC, "test2" ) );
        assertTrue( entry.contains( atDC, "test3" ) );

        // Test some addition of existing values
        entry.add( atDC, test2 );
        assertNotNull( entry.get( atDC ) );
        assertEquals( 3, entry.get( atDC ).size() );
        assertTrue( entry.contains( atDC, "test1" ) );
        assertTrue( entry.contains( atDC, "test2" ) );
        assertTrue( entry.contains( atDC, "test3" ) );

        // Test the addition of a null value
        entry.add( atDC, ( String ) null );
        assertNotNull( entry.get( atDC ) );
        assertEquals( 4, entry.get( atDC ).size() );
        assertTrue( entry.contains( atDC, "test1" ) );
        assertTrue( entry.contains( atDC, "test2" ) );
        assertTrue( entry.contains( atDC, "test3" ) );
        assertTrue( entry.contains( atDC, ( String ) null ) );

        entry.clear();

        // Test the addition of a String value. It should be converted to a byte array
        byte[] test4 = Strings.getBytesUtf8("test4");

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
        byte[] b4 = Strings.getBytesUtf8("test4");

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
        Dn dn = new Dn( "cn=test" );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );

        // Test a simple addition
        entry.add( "DC", "test1" );
        assertNotNull( entry.get( atDC ) );
        assertTrue( entry.containsAttribute( atDC ) );
        assertEquals( "dc", entry.get( atDC ).getId() );
        assertEquals( "DC", entry.get( atDC ).getUpId() );
        assertEquals( 1, entry.get( atDC ).size() );
        assertEquals( "test1", entry.get( atDC ).get().getString() );

        // Test some more addition
        entry.add( "DC", "test2", "test3" );
        assertNotNull( entry.get( atDC ) );
        assertEquals( 3, entry.get( atDC ).size() );
        assertTrue( entry.contains( atDC, "test1" ) );
        assertTrue( entry.contains( atDC, "test2" ) );
        assertTrue( entry.contains( atDC, "test3" ) );

        // Test some addition of existing values
        entry.add( "DC", "test2" );
        assertNotNull( entry.get( atDC ) );
        assertEquals( 3, entry.get( atDC ).size() );
        assertTrue( entry.contains( atDC, "test1" ) );
        assertTrue( entry.contains( atDC, "test2" ) );
        assertTrue( entry.contains( atDC, "test3" ) );

        // Test the addition of a null value
        entry.add( "DC", ( String ) null );
        assertNotNull( entry.get( atDC ) );
        assertEquals( 4, entry.get( atDC ).size() );
        assertTrue( entry.contains( atDC, "test1" ) );
        assertTrue( entry.contains( atDC, "test2" ) );
        assertTrue( entry.contains( atDC, "test3" ) );
        assertTrue( entry.contains( atDC, ( String ) null ) );

        entry.clear();

        // Test the addition of a binary value
        byte[] test4 = Strings.getBytesUtf8("test4");

        entry.add( "DC", test4 );
        assertFalse( entry.contains( "DC", test4 ) );
    }


    /**
     * Test the add( upId, byte[]... ) method
     */
    @Test
    public void testAddUpIdBytesElipsis() throws Exception
    {
        Dn dn = new Dn( "cn=test" );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );

        AttributeType atPassword = schemaManager.lookupAttributeTypeRegistry( "userPassword" );

        byte[] test1 = Strings.getBytesUtf8("test1");
        byte[] test2 = Strings.getBytesUtf8("test2");
        byte[] test3 = Strings.getBytesUtf8("test3");

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
        byte[] test4 = Strings.getBytesUtf8("test4");

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
        Dn dn = new Dn( "cn=test" );
        Entry entry = new DefaultEntry( schemaManager, dn );

        AttributeType atPassword = schemaManager.lookupAttributeTypeRegistry( "userPassword" );

        byte[] b1 = Strings.getBytesUtf8("test1");
        byte[] b2 = Strings.getBytesUtf8("test2");
        byte[] b3 = Strings.getBytesUtf8("test3");

        Value<String> test1 = new StringValue( atDC, "test1" );
        Value<String> test2 = new StringValue( atDC, "test2" );
        Value<String> test3 = new StringValue( atDC, "test3" );

        Value<byte[]> testB1 = new BinaryValue( atPassword, b1 );
        Value<byte[]> testB2 = new BinaryValue( atPassword, b2 );
        Value<byte[]> testB3 = new BinaryValue( atPassword, b3 );

        // Test a simple addition in atDC
        entry.add( "dC", test1 );
        assertNotNull( entry.get( atDC ) );
        assertEquals( 1, entry.get( atDC ).size() );
        assertEquals( "test1", entry.get( atDC ).get().getString() );
        assertTrue( entry.containsAttribute( atDC ) );
        assertEquals( "dC", entry.get( atDC ).getUpId() );

        // Test some more addition
        entry.add( "dC", test2, test3 );
        assertNotNull( entry.get( atDC ) );
        assertEquals( 3, entry.get( atDC ).size() );
        assertTrue( entry.contains( atDC, "test1" ) );
        assertTrue( entry.contains( atDC, "test2" ) );
        assertTrue( entry.contains( atDC, "test3" ) );
        assertTrue( entry.containsAttribute( atDC ) );
        assertEquals( "dC", entry.get( atDC ).getUpId() );

        // Test some addition of existing values
        entry.add( "dC", test2 );
        assertNotNull( entry.get( atDC ) );
        assertEquals( 3, entry.get( atDC ).size() );
        assertTrue( entry.contains( atDC, "test1" ) );
        assertTrue( entry.contains( atDC, "test2" ) );
        assertTrue( entry.contains( atDC, "test3" ) );

        // Test the addition of a null value
        entry.add( "dC", ( String ) null );
        assertNotNull( entry.get( atDC ) );
        assertEquals( 4, entry.get( atDC ).size() );
        assertTrue( entry.contains( atDC, "test1" ) );
        assertTrue( entry.contains( atDC, "test2" ) );
        assertTrue( entry.contains( atDC, "test3" ) );
        assertTrue( entry.contains( atDC, ( String ) null ) );

        entry.clear();

        // Test the addition of a String value. It should be converted to a byte array
        byte[] test4 = Strings.getBytesUtf8("test4");

        entry.add( "dC", test4 );
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
        byte[] b4 = Strings.getBytesUtf8("test4");

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
        Dn dn = new Dn( "cn=test" );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );

        // Test a simple addition
        entry.add( "dc", atDC, "test1" );
        assertNotNull( entry.get( atDC ) );
        assertEquals( 1, entry.get( atDC ).size() );
        assertEquals( "test1", entry.get( atDC ).get().getString() );

        // Test some more addition
        entry.add( "DC", atDC, "test2", "test3" );
        assertNotNull( entry.get( atDC ) );
        assertEquals( 3, entry.get( atDC ).size() );
        assertTrue( entry.contains( atDC, "test1" ) );
        assertTrue( entry.contains( atDC, "test2" ) );
        assertTrue( entry.contains( atDC, "test3" ) );

        // Test some addition of existing values
        entry.add( "domainComponent", atDC, "test2" );
        assertNotNull( entry.get( atDC ) );
        assertEquals( 3, entry.get( atDC ).size() );
        assertTrue( entry.contains( atDC, "test1" ) );
        assertTrue( entry.contains( atDC, "test2" ) );
        assertTrue( entry.contains( atDC, "test3" ) );

        // Test the addition of a null value
        entry.add( "DOMAINComponent", atDC, ( String ) null );
        assertNotNull( entry.get( atDC ) );
        assertEquals( 4, entry.get( atDC ).size() );
        assertTrue( entry.contains( atDC, "test1" ) );
        assertTrue( entry.contains( atDC, "test2" ) );
        assertTrue( entry.contains( atDC, "test3" ) );
        assertTrue( entry.contains( atDC, ( String ) null ) );

        entry.clear();

        // Test the addition of a binary value
        byte[] test4 = Strings.getBytesUtf8("test4");

        entry.add( "dc", atDC, test4 );
        assertFalse( entry.contains( "dc", test4 ) );
    }


    /**
     * Test the add( upId, AT, byte[]... ) method
     */
    @Test
    public void testAddUpIdAtBytesElipsis() throws Exception
    {
        Dn dn = new Dn( "cn=test" );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );

        AttributeType atPassword = schemaManager.lookupAttributeTypeRegistry( "userPassword" );

        byte[] test1 = Strings.getBytesUtf8("test1");
        byte[] test2 = Strings.getBytesUtf8("test2");
        byte[] test3 = Strings.getBytesUtf8("test3");

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
        byte[] test4 = Strings.getBytesUtf8("test4");

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
        Dn dn = new Dn( "cn=test" );
        Entry entry = new DefaultEntry( schemaManager, dn );

        AttributeType atPassword = schemaManager.lookupAttributeTypeRegistry( "userPassword" );

        byte[] b1 = Strings.getBytesUtf8("test1");
        byte[] b2 = Strings.getBytesUtf8("test2");
        byte[] b3 = Strings.getBytesUtf8("test3");

        Value<String> test1 = new StringValue( atDC, "test1" );
        Value<String> test2 = new StringValue( atDC, "test2" );
        Value<String> test3 = new StringValue( atDC, "test3" );

        Value<byte[]> testB1 = new BinaryValue( atPassword, b1 );
        Value<byte[]> testB2 = new BinaryValue( atPassword, b2 );
        Value<byte[]> testB3 = new BinaryValue( atPassword, b3 );

        // Test a simple addition in atCN
        entry.add( "dC", atDC, test1 );
        assertNotNull( entry.get( atDC ) );
        assertEquals( 1, entry.get( atDC ).size() );
        assertEquals( "test1", entry.get( atDC ).get().getString() );
        assertTrue( entry.containsAttribute( atDC ) );
        assertEquals( "dC", entry.get( atDC ).getUpId() );

        // Test some more addition
        entry.add( "dC", atDC, test2, test3 );
        assertNotNull( entry.get( atDC ) );
        assertEquals( 3, entry.get( atDC ).size() );
        assertTrue( entry.contains( atDC, "test1" ) );
        assertTrue( entry.contains( atDC, "test2" ) );
        assertTrue( entry.contains( atDC, "test3" ) );
        assertTrue( entry.containsAttribute( atDC ) );
        assertEquals( "dC", entry.get( atDC ).getUpId() );

        // Test some addition of existing values
        entry.add( "dC", atDC, test2 );
        assertNotNull( entry.get( atDC ) );
        assertEquals( 3, entry.get( atDC ).size() );
        assertTrue( entry.contains( atDC, "test1" ) );
        assertTrue( entry.contains( atDC, "test2" ) );
        assertTrue( entry.contains( atDC, "test3" ) );

        // Test the addition of a null value
        entry.add( "dC", atDC, ( String ) null );
        assertNotNull( entry.get( atDC ) );
        assertEquals( 4, entry.get( atDC ).size() );
        assertTrue( entry.contains( atDC, "test1" ) );
        assertTrue( entry.contains( atDC, "test2" ) );
        assertTrue( entry.contains( atDC, "test3" ) );
        assertTrue( entry.contains( atDC, ( String ) null ) );

        entry.clear();

        // Test the addition of a String value. It should be converted to a byte array
        byte[] test4 = Strings.getBytesUtf8("test4");

        entry.add( "dC", atDC, test4 );
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
        byte[] b4 = Strings.getBytesUtf8("test4");

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

        entry1.add( "sn", ( String ) null );
        assertFalse( entry2.containsAttribute( "sn" ) );
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

        EntryAttribute attrPWD = new DefaultEntryAttribute( atPwd, BYTES1, BYTES2 );

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

        EntryAttribute attrCN = new DefaultEntryAttribute( atCN, "test1", "test2" );

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

        Value<String> strValue1 = new StringValue( atCN, "test1" );
        Value<String> strValue2 = new StringValue( atCN, "test2" );
        Value<String> strValue3 = new StringValue( atCN, "test3" );
        Value<String> strNullValue = new StringValue( atCN, null );

        Value<byte[]> binValue1 = new BinaryValue( atPwd, BYTES1 );
        Value<byte[]> binValue2 = new BinaryValue( atPwd, BYTES2 );
        Value<byte[]> binValue3 = new BinaryValue( atPwd, BYTES3 );
        Value<byte[]> binNullValue = new BinaryValue( atPwd, null );

        assertFalse( entry.contains( ( String ) null, strValue1 ) );
        assertFalse( entry.contains( atCN, binValue1 ) );

        EntryAttribute attrCN = new DefaultEntryAttribute( atCN, strValue1, strValue2 );
        EntryAttribute attrPWD = new DefaultEntryAttribute( atPwd, binValue1, binValue2, binNullValue );

        entry.add( attrCN, attrPWD );

        assertTrue( entry.contains( atCN, strValue1, strValue2 ) );
        assertTrue( entry.contains( atPwd, binValue1, binValue2, binNullValue ) );

        assertFalse( entry.contains( atCN, strValue3 ) );
        assertFalse( entry.contains( atCN, strNullValue ) );
        assertFalse( entry.contains( atPwd, binValue3 ) );
    }


    /**
     * Test for method contains( EntryAttribute... )
     */
    @Test
    public void testContainsEntryAttributeArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        EntryAttribute attrOC = new DefaultEntryAttribute( atOC, "top", "person" );
        EntryAttribute attrCN = new DefaultEntryAttribute( atCN, "test1", "test2" );
        EntryAttribute attrSN = new DefaultEntryAttribute( atSN, "Test1", "Test2" );
        EntryAttribute attrPWD = new DefaultEntryAttribute( atPwd, BYTES1, BYTES2 );

        assertFalse( entry.contains( attrOC, attrCN ) );

        entry.add( attrOC, attrCN );

        assertTrue( entry.contains( attrOC, attrCN ) );
        assertFalse( entry.contains( attrOC, attrCN, attrSN ) );

        entry.add( attrSN, attrPWD );

        assertTrue( entry.contains( attrSN, attrPWD ) );

        assertFalse( entry.contains( ( EntryAttribute ) null ) );
        entry.clear();
        assertTrue( entry.contains( ( EntryAttribute ) null ) );
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

        EntryAttribute attrPWD = new DefaultEntryAttribute( atPwd, BYTES1, ( byte[] ) null, BYTES2 );

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

        EntryAttribute attrDC = new DefaultEntryAttribute( atDC, "test1", ( String ) null, "test2" );

        entry.add( attrDC );

        assertTrue( entry.contains( "  DC  ", "test1", "test2" ) );

        assertTrue( entry.contains( "  DC  ", ( String ) null ) );
        assertFalse( entry.contains( "  DC  ", BYTES1, BYTES2 ) );
        assertFalse( entry.contains( "  DC  ", "test3" ) );
        assertFalse( entry.contains( "  DCC  ", "test3" ) );
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

        EntryAttribute attrDC = new DefaultEntryAttribute( atDC, "test1", "test2", ( String ) null );
        EntryAttribute attrPWD = new DefaultEntryAttribute( atPwd, BYTES1, BYTES2, ( byte[] ) null );

        entry.add( attrDC, attrPWD );

        Value<String> strValue1 = new StringValue( atDC, "test1" );
        Value<String> strValue2 = new StringValue( atDC, "test2" );
        Value<String> strValue3 = new StringValue( atDC, "test3" );
        Value<String> strNullValue = new StringValue( atDC, null );

        Value<byte[]> binValue1 = new BinaryValue( atPwd, BYTES1 );
        Value<byte[]> binValue2 = new BinaryValue( atPwd, BYTES2 );
        Value<byte[]> binValue3 = new BinaryValue( atPwd, BYTES3 );
        Value<byte[]> binNullValue = new BinaryValue( atPwd, null );

        assertTrue( entry.contains( "DC", strValue1, strValue2 ) );
        assertTrue( entry.contains( "userpassword", binValue1, binValue2, binNullValue ) );

        assertFalse( entry.contains( "dc", strValue3 ) );
        assertTrue( entry.contains( "dc", strNullValue ) );
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

        EntryAttribute attrOC = new DefaultEntryAttribute( atOC, "top", "person" );
        EntryAttribute attrCN = new DefaultEntryAttribute( atCN, "test1", "test2" );
        EntryAttribute attrSN = new DefaultEntryAttribute( atSN, "Test1", "Test2" );
        EntryAttribute attrPWD = new DefaultEntryAttribute( atPwd, BYTES1, BYTES2 );

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

        EntryAttribute attrOC = new DefaultEntryAttribute( atOC, "top", "person" );
        EntryAttribute attrCN = new DefaultEntryAttribute( atCN, "test1", "test2" );
        EntryAttribute attrSN = new DefaultEntryAttribute( atSN, "Test1", "Test2" );
        EntryAttribute attrPWD = new DefaultEntryAttribute( atPwd, BYTES1, BYTES2 );

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

        EntryAttribute attrOC = new DefaultEntryAttribute( "objectClass", atOC, "top", "person" );
        EntryAttribute attrCN = new DefaultEntryAttribute( "cn", atCN, "test1", "test2" );
        EntryAttribute attrSN = new DefaultEntryAttribute( "sn", atSN, "Test1", "Test2" );
        EntryAttribute attrPWD = new DefaultEntryAttribute( "userPassword", atPwd, BYTES1, BYTES2 );

        entry1.put( attrOC, attrCN, attrSN, attrPWD );
        entry2.put( attrOC, attrCN, attrSN );
        assertNotSame( entry1, entry2 );

        entry2.put( attrPWD );
        assertEquals( entry1, entry2 );

        EntryAttribute attrL1 = new DefaultEntryAttribute( "l", atL, "Paris", "New-York" );
        EntryAttribute attrL2 = new DefaultEntryAttribute( "l", atL, "Paris", "Tokyo" );

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

        assertEquals( 0, entry.getAttributeTypes().size() );

        EntryAttribute attrOC = new DefaultEntryAttribute( atOC, "top", "person" );
        EntryAttribute attrCN = new DefaultEntryAttribute( atCN, "test1", "test2" );
        EntryAttribute attrSN = new DefaultEntryAttribute( atSN, "Test1", "Test2" );
        EntryAttribute attrPWD = new DefaultEntryAttribute( atPwd, BYTES1, BYTES2 );

        entry.add( attrOC, attrCN, attrSN, attrPWD );

        Set<AttributeType> attributeTypes = entry.getAttributeTypes();

        assertEquals( 4, attributeTypes.size() );
        assertTrue( attributeTypes.contains( atOC ) );
        assertTrue( attributeTypes.contains( atCN ) );
        assertTrue( attributeTypes.contains( atSN ) );
        assertTrue( attributeTypes.contains( atPwd ) );
        assertFalse( attributeTypes.contains( atC ) );
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

        EntryAttribute attrOC = new DefaultEntryAttribute( atOC, "top", "person" );
        EntryAttribute attrCN = new DefaultEntryAttribute( atCN, "test1", "test2" );
        EntryAttribute attrSN = new DefaultEntryAttribute( atSN, "Test1", "Test2" );
        EntryAttribute attrPWD = new DefaultEntryAttribute( atPwd, BYTES1, BYTES2 );

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

        EntryAttribute attrOC = new DefaultEntryAttribute( atOC, "top", "person" );
        EntryAttribute attrCN = new DefaultEntryAttribute( atCN, "test1", "test2" );
        EntryAttribute attrSN = new DefaultEntryAttribute( atSN, "Test1", "Test2" );
        EntryAttribute attrPWD = new DefaultEntryAttribute( atPwd, BYTES1, BYTES2 );

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

        Dn testDn = new Dn( "cn=test" );
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

        entry2.setDn( new Dn( "ou=system,dc=com" ) );
        assertNotSame( entry1.hashCode(), entry2.hashCode() );

        entry2.setDn( EXAMPLE_DN );
        assertEquals( entry1.hashCode(), entry2.hashCode() );

        EntryAttribute attrOC = new DefaultEntryAttribute( "objectClass", atOC, "top", "person" );
        EntryAttribute attrCN = new DefaultEntryAttribute( "cn", atCN, "test1", "test2" );
        EntryAttribute attrSN = new DefaultEntryAttribute( "sn", atSN, "Test1", "Test2" );
        EntryAttribute attrPWD = new DefaultEntryAttribute( "userPassword", atPwd, BYTES1, BYTES2 );

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

        EntryAttribute attrOC = new DefaultEntryAttribute( atOC, "top", "person" );

        assertFalse( entry.contains( attrOC ) );
        assertFalse( entry.hasObjectClass( attrOC ) );

        entry.add( attrOC );

        assertTrue( entry.hasObjectClass( attrOC ) );

        EntryAttribute attrOC2 = new DefaultEntryAttribute( atOC, "person" );
        assertTrue( entry.hasObjectClass( attrOC2 ) );

        EntryAttribute attrOC3 = new DefaultEntryAttribute( atOC, "inetOrgPerson" );
        assertFalse( entry.hasObjectClass( attrOC3 ) );
        assertFalse( entry.hasObjectClass( ( EntryAttribute ) null ) );

        EntryAttribute attrCN = new DefaultEntryAttribute( atCN, "top" );
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

        entry.add( new DefaultEntryAttribute( atOC, "top", "person" ) );

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

        EntryAttribute attrOC = new DefaultEntryAttribute( atOC, "top", "person" );
        EntryAttribute attrCN = new DefaultEntryAttribute( atCN, "test1", "test2" );
        EntryAttribute attrSN = new DefaultEntryAttribute( atSN, "Test1", "Test2" );
        EntryAttribute attrPWD = new DefaultEntryAttribute( atPwd, BYTES1, BYTES2 );

        entry.put( attrOC, attrCN, attrSN, attrPWD );

        Iterator<EntryAttribute> iterator = entry.iterator();

        assertTrue( iterator.hasNext() );

        Set<AttributeType> expectedIds = new HashSet<AttributeType>();
        expectedIds.add( atOC );
        expectedIds.add( atCN );
        expectedIds.add( atSN );
        expectedIds.add( atPwd );

        while ( iterator.hasNext() )
        {
            EntryAttribute attribute = iterator.next();

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

        EntryAttribute replaced = entry.put( atPwd, BYTES1, BYTES2, BYTES1 );
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

        EntryAttribute attribute = entry.get( atPwd );
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

        entry.put( atDC, ( String ) null );
        assertEquals( 1, entry.size() );
        assertTrue( entry.containsAttribute( atDC ) );
        assertTrue( entry.contains( atDC, ( String ) null ) );

        EntryAttribute replaced = entry.put( atDC, "test1", "test2", "test1" );
        assertNotNull( replaced );
        assertEquals( atDC, replaced.getAttributeType() );
        assertTrue( replaced.contains( ( String ) null ) );
        assertEquals( 1, entry.size() );
        assertTrue( entry.contains( atDC, "test1", "test2" ) );
        assertFalse( entry.contains( atDC, "test3" ) );
        assertEquals( 2, entry.get( atDC ).size() );

        replaced = entry.put( atDC, BYTES1 );
        assertNotNull( replaced );
        assertTrue( replaced.contains( "test1", "test2" ) );

        EntryAttribute attribute = entry.get( atDC );
        assertEquals( 0, attribute.size() );
    }


    /**
     * Test for method put( AttributeType, Value<?>... )
     */
    @Test
    public void testPutAttributeTypeValueArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        Value<String> strValue1 = new StringValue( atCN, "test1" );
        Value<String> strValue2 = new StringValue( atCN, "test2" );
        Value<String> strValue3 = new StringValue( atCN, "test3" );
        Value<String> strNullValue = new StringValue( atCN, null );

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

        entry.put( atCN, strNullValue );
        assertEquals( 1, entry.size() );
        assertTrue( entry.containsAttribute( atCN ) );
        assertTrue( entry.contains( atCN, ( String ) null ) );

        EntryAttribute replaced = entry.put( atCN, strValue1, strValue2, strValue1 );
        assertNotNull( replaced );
        assertEquals( atCN, replaced.getAttributeType() );
        assertTrue( replaced.contains( ( String ) null ) );
        assertEquals( 1, entry.size() );
        assertTrue( entry.contains( atCN, strValue1, strValue2 ) );
        assertFalse( entry.contains( atCN, strValue3 ) );
        assertEquals( 2, entry.get( atCN ).size() );

        replaced = entry.put( atCN, binValue1 );
        assertNotNull( replaced );
        assertTrue( replaced.contains( strValue1, strValue2 ) );

        EntryAttribute attribute = entry.get( atCN );
        assertEquals( 0, attribute.size() );
    }


    /**
     * Test for method put( EntryAttribute...)
     */
    @Test
    public void testPutEntryAttribute() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        EntryAttribute oc = new DefaultEntryAttribute( atObjectClass, "top", "person" );
        EntryAttribute cn = new DefaultEntryAttribute( atCN, "test1", "test2" );
        EntryAttribute sn = new DefaultEntryAttribute( atSN, "Test1", "Test2" );
        EntryAttribute up = new DefaultEntryAttribute( atPwd, BYTES1, BYTES2 );
        EntryAttribute c = new DefaultEntryAttribute( atC, "FR", "US" );

        List<EntryAttribute> removed = entry.put( oc, cn, sn, c );

        assertEquals( 4, entry.size() );
        assertEquals( 0, removed.size() );
        assertTrue( entry.containsAttribute( "ObjectClass" ) );
        assertTrue( entry.containsAttribute( "CN" ) );
        assertTrue( entry.containsAttribute( "  sn  " ) );
        assertTrue( entry.containsAttribute( " countryName  " ) );

        EntryAttribute attr = entry.get( "objectclass" );
        assertEquals( 2, attr.size() );

        EntryAttribute c2 = new DefaultEntryAttribute( atC, "UK", "DE" );
        removed = entry.put( c2, up );
        assertEquals( 1, removed.size() );
        assertEquals( c, removed.get( 0 ) );
        assertTrue( removed.get( 0 ).contains( "FR" ) );
        assertTrue( removed.get( 0 ).contains( "US" ) );

        assertEquals( 5, entry.size() );

        assertTrue( entry.containsAttribute( "userPassword" ) );
        assertTrue( entry.containsAttribute( " countryName " ) );

        EntryAttribute attrC = entry.get( "countryName" );
        assertEquals( 2, attrC.size() );
        assertTrue( attrC.contains( "UK", "DE" ) );

        c2.clear();
        entry.put( c2 );
        assertEquals( 5, entry.size() );
        attrC = entry.get( "countryName" );
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

        EntryAttribute replaced = entry.put( "USERpassword ", atPwd, BYTES1, BYTES2, BYTES1 );
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

        EntryAttribute attribute = entry.get( atPwd );
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

        entry.put( "DC", atDC, ( String ) null );
        assertEquals( 1, entry.size() );
        assertTrue( entry.containsAttribute( atDC ) );
        assertTrue( entry.contains( atDC, ( String ) null ) );
        assertEquals( "DC", entry.get( atDC ).getUpId() );

        EntryAttribute replaced = entry.put( "domainComponent", atDC, "test1", "test2", "test1" );
        assertNotNull( replaced );
        assertEquals( atDC, replaced.getAttributeType() );
        assertEquals( "domainComponent", entry.get( atDC ).getUpId() );
        assertTrue( replaced.contains( ( String ) null ) );
        assertEquals( 1, entry.size() );
        assertTrue( entry.contains( atDC, "test1", "test2" ) );
        assertFalse( entry.contains( atDC, "test3" ) );
        assertEquals( 2, entry.get( atDC ).size() );

        replaced = entry.put( "0.9.2342.19200300.100.1.25", atDC, BYTES1 );
        assertNotNull( replaced );
        assertTrue( replaced.contains( "test1", "test2" ) );
        assertEquals( "0.9.2342.19200300.100.1.25", entry.get( atDC ).getUpId() );

        EntryAttribute attribute = entry.get( atDC );
        assertEquals( 0, attribute.size() );
    }


    /**
     * Test for method put( String, AttributeType, Value<?>... )
     */
    @Test
    public void testPutStringAttributeTypeValueArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        Value<String> strValue1 = new StringValue( atCN, "test1" );
        Value<String> strValue2 = new StringValue( atCN, "test2" );
        Value<String> strValue3 = new StringValue( atCN, "test3" );
        Value<String> strNullValue = new StringValue( atCN, null );

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
            entry.put( "badAttr", atCN, strValue1 );
            fail();
        }
        catch ( LdapNoSuchAttributeException nsae )
        {
            assertTrue( true );
        }

        entry.put( "Cn", atCN, strNullValue );
        assertEquals( 1, entry.size() );
        assertTrue( entry.containsAttribute( atCN ) );
        assertTrue( entry.contains( atCN, ( String ) null ) );
        assertEquals( "Cn", entry.get( atCN ).getUpId() );

        EntryAttribute replaced = entry.put( "commonName", atCN, strValue1, strValue2, strValue1 );
        assertNotNull( replaced );
        assertEquals( atCN, replaced.getAttributeType() );
        assertTrue( replaced.contains( ( String ) null ) );
        assertEquals( 1, entry.size() );
        assertTrue( entry.contains( atCN, strValue1, strValue2 ) );
        assertFalse( entry.contains( atCN, strValue3 ) );
        assertEquals( 2, entry.get( atCN ).size() );
        assertEquals( "commonName", entry.get( atCN ).getUpId() );

        replaced = entry.put( "2.5.4.3", atCN, binValue1 );
        assertNotNull( replaced );
        assertTrue( replaced.contains( strValue1, strValue2 ) );

        EntryAttribute attribute = entry.get( atCN );
        assertEquals( 0, attribute.size() );
        assertEquals( "2.5.4.3", entry.get( atCN ).getUpId() );
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

        EntryAttribute replaced = entry.put( "userPassword", ( byte[] ) null );
        assertNull( replaced );
        assertEquals( 1, entry.size() );
        assertNotNull( entry.get( "userPassword" ) );
        assertEquals( 1, entry.get( "userPassword" ).size() );
        assertNull( entry.get( "userPassword" ).get().get() );

        replaced = entry.put( "UserPassword", BYTES1 );
        assertNotNull( replaced );
        assertEquals( atPwd, replaced.getAttributeType() );
        assertTrue( replaced.contains( ( byte[] ) null ) );
        assertEquals( 1, entry.size() );
        assertNotNull( entry.get( "userPassword" ) );
        assertEquals( 1, entry.get( "userPassword" ).size() );
        assertNotNull( entry.get( "userPassword" ).get().get() );
        assertTrue( entry.get( "userPassword" ).contains( BYTES1 ) );

        replaced = entry.put( "userPassword", BYTES1, BYTES2, BYTES1 );
        assertNotNull( replaced );
        assertEquals( 1, entry.size() );
        assertNotNull( entry.get( "userPassword" ) );
        assertEquals( 2, entry.get( "USERPassword" ).size() );
        EntryAttribute attribute = entry.get( "userPassword" );
        assertTrue( attribute.contains( BYTES1 ) );
        assertTrue( attribute.contains( BYTES2 ) );
        assertEquals( "userpassword", attribute.getId() );
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

        EntryAttribute replaced = entry.put( "description", ( String ) null );
        assertNull( replaced );
        assertEquals( 1, entry.size() );
        assertNotNull( entry.get( "description" ) );
        assertEquals( 0, entry.get( "description" ).size() );
        assertNull( entry.get( "description" ).get() );

        replaced = entry.put( "CN", "test" );
        assertNull( replaced );
        assertEquals( 2, entry.size() );
        assertNotNull( entry.get( "cn" ) );
        assertEquals( 1, entry.get( "cn" ).size() );
        assertNotNull( entry.get( "cn" ).get().get() );
        assertTrue( entry.get( "cn" ).contains( "test" ) );

        replaced = entry.put( "cN", "test1", "test2", "test1" );
        assertNotNull( replaced );
        assertEquals( "test", replaced.get().getString() );

        assertEquals( 2, entry.size() );
        assertNotNull( entry.get( "cn" ) );
        assertEquals( 2, entry.get( "CN" ).size() );

        EntryAttribute attribute = entry.get( "cn" );
        assertTrue( attribute.contains( "test1" ) );
        assertTrue( attribute.contains( "test2" ) );
        assertEquals( "cn", attribute.getId() );
        assertEquals( "cN", attribute.getUpId() );
    }


    /**
     * Test method for put( String, Value<?>... )
     */
    @Test
    public void testPutStringValueArray()
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        Value<String> strValue1 = new StringValue( atCN, "test1" );
        Value<String> strValue2 = new StringValue( atCN, "test2" );
        Value<String> strValue3 = new StringValue( atCN, "test3" );
        Value<String> strNullValue = new StringValue( atCN, null );

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

        EntryAttribute replaced = entry.put( "description", strNullValue );
        assertNull( replaced );
        assertEquals( 1, entry.size() );
        assertNotNull( entry.get( "description" ) );
        assertEquals( 1, entry.get( "description" ).size() );
        assertNotNull( entry.get( "description" ).get() );
        assertNull( entry.get( "description" ).get().get() );

        replaced = entry.put( "CN", strValue3 );
        assertNull( replaced );
        assertEquals( 2, entry.size() );
        assertNotNull( entry.get( "cn" ) );
        assertEquals( 1, entry.get( "cn" ).size() );
        assertNotNull( entry.get( "cn" ).get().get() );
        assertTrue( entry.get( "cn" ).contains( strValue3 ) );

        replaced = entry.put( "cN", strValue1, strValue2, strValue1 );
        assertNotNull( replaced );
        assertEquals( strValue3, replaced.get() );

        assertEquals( 2, entry.size() );
        assertNotNull( entry.get( "cn" ) );
        assertEquals( 2, entry.get( "CN" ).size() );

        EntryAttribute attribute = entry.get( "cn" );
        assertTrue( attribute.contains( strValue1 ) );
        assertTrue( attribute.contains( strValue2 ) );
        assertEquals( "cn", attribute.getId() );
        assertEquals( "cN", attribute.getUpId() );

        // Bin values are not allowed, so the new CN will be empty
        entry.put( "cn", binValue1 );
        assertNull( entry.get( "cn" ).get() );
    }


    /**
     * Test the put( SA... ) method
     */
    @Test
    public void tesPutServerAttributeElipsis() throws Exception
    {
        Dn dn = new Dn( "cn=test" );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );

        // first test a null SA addition. It should be allowed.
        try
        {
            entry.put( ( EntryAttribute ) null );
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

        EntryAttribute sa = new DefaultEntryAttribute( atL, "france" );
        entry.put( sa );

        assertEquals( 1, entry.size() );
        assertNotNull( entry.get( "l" ) );
        assertEquals( "france", entry.get( "l" ).get().getString() );

        EntryAttribute sb = new DefaultEntryAttribute( atC, "countryTest" );
        EntryAttribute sc = new DefaultEntryAttribute( atGN, "test" );
        EntryAttribute sd = new DefaultEntryAttribute( atStreet, "testStreet" );
        entry.put( sb, sc, sd );

        assertEquals( 4, entry.size() );
        assertNotNull( entry.get( atC ) );
        assertEquals( "countryTest", entry.get( atC ).get().getString() );
        assertNotNull( entry.get( atGN ) );
        assertEquals( "test", entry.get( atGN ).get().getString() );
        assertNotNull( entry.get( atStreet ) );
        assertEquals( "testStreet", entry.get( atStreet ).get().getString() );

        // Test a replacement
        EntryAttribute sbb = new DefaultEntryAttribute( atC, "countryTestTest" );
        EntryAttribute scc = new DefaultEntryAttribute( atGN, "testtest" );
        List<EntryAttribute> result = entry.put( sbb, scc );

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
        EntryAttribute oc = new DefaultEntryAttribute( "OBJECTCLASS", OBJECT_CLASS_AT, "person", "inetorgperson" );
        List<EntryAttribute> oldOc = entry.put( oc );

        assertNotNull( oldOc );
        assertEquals( 0, oldOc.size() );

        assertNotNull( entry.get( "objectClass" ) );

        EntryAttribute newOc = entry.get( "objectClass" );

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
        Dn dn = new Dn( "dc=test" );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );

        // Test an empty AT
        entry.put( atDC, ( String ) null );
        assertEquals( 1, entry.size() );
        assertEquals( "dc", entry.get( atDC ).getUpId() );
        assertNull( entry.get( atDC ).get().get() );

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
        entry.put( atDC, "test" );

        assertEquals( 1, entry.size() );
        assertEquals( "dc", entry.get( atDC ).getUpId() );
        assertEquals( 1, entry.get( atDC ).size() );
        assertEquals( "test", entry.get( atDC ).get().getString() );

        // Add more than one value
        entry.put( atDC, "test1", "test2", "test3" );

        assertEquals( 1, entry.size() );
        assertEquals( "dc", entry.get( atDC ).getUpId() );
        assertEquals( 3, entry.get( atDC ).size() );
        assertTrue( entry.contains( "dc", "test1" ) );
        assertTrue( entry.contains( "dc", "test2" ) );
        assertTrue( entry.contains( "dc", "test3" ) );

        // Add twice the same value
        EntryAttribute sa = entry.put( atDC, "test1", "test2", "test1" );

        assertEquals( 3, sa.size() );
        assertTrue( sa.contains( "test1", "test2", "test3" ) );
        assertEquals( 1, entry.size() );
        assertEquals( "dc", entry.get( atDC ).getUpId() );
        assertEquals( 2, entry.get( atDC ).size() );
        assertTrue( entry.contains( "dc", "test1" ) );
        assertTrue( entry.contains( "dc", "test2" ) );
    }


    /**
     * Test the put( AT, Byte[]... ) method
     */
    @Test
    public void tesPutAtByteElipsis() throws Exception
    {
        Dn dn = new Dn( "cn=test" );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );

        // Test an empty AT
        entry.put( atPwd, ( byte[] ) null );
        assertEquals( 1, entry.size() );
        assertEquals( "userPassword", entry.get( atPwd ).getUpId() );
        assertNull( entry.get( atPwd ).get().get() );

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

        byte[] password = Strings.getBytesUtf8("test");
        byte[] test1 = Strings.getBytesUtf8("test1");
        byte[] test2 = Strings.getBytesUtf8("test2");
        byte[] test3 = Strings.getBytesUtf8("test3");

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
        EntryAttribute sa = entry.put( atPwd, test1, test2, test1 );

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
        Dn dn = new Dn( "cn=test" );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );

        // Adding a null value to an attribute
        entry.put( atCN, ( Value<?> ) null );

        assertEquals( 1, entry.size() );
        assertEquals( "cn", entry.get( atCN ).getUpId() );

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

        assertEquals( 1, entry.size() );
        assertEquals( "cn", entry.get( atCN ).getUpId() );
        assertEquals( 1, entry.get( atCN ).size() );
        assertEquals( "test", entry.get( atCN ).get().getString() );

        // Add more than one value
        entry.put( atCN, new StringValue( atCN, "test1" ), new StringValue( atCN, "test2" ), new StringValue( atCN,
            "test3" ) );

        assertEquals( 1, entry.size() );
        assertEquals( "cn", entry.get( atCN ).getUpId() );
        assertEquals( 3, entry.get( atCN ).size() );
        assertTrue( entry.contains( "cn", "test1" ) );
        assertTrue( entry.contains( "cn", "test2" ) );
        assertTrue( entry.contains( "cn", "test3" ) );

        // Add twice the same value
        EntryAttribute sa = entry.put( atCN, new StringValue( atCN, "test1" ), new StringValue( atCN, "test2" ),
            new StringValue( atCN, "test1" ) );

        assertEquals( 3, sa.size() );
        assertTrue( sa.contains( "test1", "test2", "test3" ) );
        assertEquals( 1, entry.size() );
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
        Dn dn = new Dn( "dc=test" );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );

        // Adding a null value should be possible
        entry.put( "dc", ( String ) null );

        assertEquals( 1, entry.size() );
        assertEquals( "dc", entry.get( atDC ).getUpId() );
        assertNull( entry.get( atDC ).get().get() );

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
        entry.put( "dc", "test" );

        assertEquals( 1, entry.size() );
        assertEquals( "dc", entry.get( atDC ).getUpId() );
        assertEquals( 1, entry.get( atDC ).size() );
        assertEquals( "test", entry.get( atDC ).get().getString() );

        // Add more than one value
        entry.put( "dc", "test1", "test2", "test3" );

        assertEquals( 1, entry.size() );
        assertEquals( "dc", entry.get( atDC ).getUpId() );
        assertEquals( 3, entry.get( atDC ).size() );
        assertTrue( entry.contains( "dc", "test1" ) );
        assertTrue( entry.contains( "dc", "test2" ) );
        assertTrue( entry.contains( "dc", "test3" ) );

        // Add twice the same value
        EntryAttribute sa = entry.put( "dc", "test1", "test2", "test1" );

        assertEquals( 3, sa.size() );
        assertTrue( sa.contains( "test1", "test2", "test3" ) );
        assertEquals( 1, entry.size() );
        assertEquals( "dc", entry.get( atDC ).getUpId() );
        assertEquals( 2, entry.get( atDC ).size() );
        assertTrue( entry.contains( "dc", "test1" ) );
        assertTrue( entry.contains( "dc", "test2" ) );

        // Check the UpId
        entry.put( "DC", "test4" );
        assertEquals( "DC", entry.get( atDC ).getUpId() );
    }


    /**
     * Test the put( upId, byte[]... ) method
     */
    @Test
    public void tesPutUpIdBytesElipsis() throws Exception
    {
        Dn dn = new Dn( "cn=test" );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );

        AttributeType atPassword = schemaManager.lookupAttributeTypeRegistry( "userPassword" );

        // Adding a null value should be possible
        entry.put( "userPassword", ( byte[] ) null );
        assertEquals( 1, entry.size() );
        assertEquals( "userPassword", entry.get( atPassword ).getUpId() );
        assertNull( entry.get( atPassword ).get().get() );

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
        byte[] test = Strings.getBytesUtf8("test");
        byte[] test1 = Strings.getBytesUtf8("test1");
        byte[] test2 = Strings.getBytesUtf8("test2");
        byte[] test3 = Strings.getBytesUtf8("test3");

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
        EntryAttribute sa = entry.put( "userPassword", test1, test2, test1 );

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
        Dn dn = new Dn( "dc=test" );
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
        entry.put( "domaincomponent", atDC, ( String ) null );
        assertEquals( 1, entry.size() );
        assertEquals( "domaincomponent", entry.get( atDC ).getUpId() );
        assertTrue( entry.containsAttribute( "dc" ) );
        assertNull( entry.get( atDC ).get().get() );

        // Check that we can use a null AttributeType
        entry.put( "domaincomponent", ( AttributeType ) null, ( String ) null );
        assertEquals( 1, entry.size() );
        assertEquals( "domaincomponent", entry.get( atDC ).getUpId() );
        assertTrue( entry.containsAttribute( "dc" ) );
        assertNull( entry.get( atDC ).get().get() );

        // Test that we can use a null upId
        entry.put( null, atDC, ( String ) null );
        assertEquals( 1, entry.size() );
        assertEquals( "dc", entry.get( atDC ).getUpId() );
        assertTrue( entry.containsAttribute( "dc" ) );
        assertNull( entry.get( atDC ).get().get() );

        try
        {
            entry.put( "sn", atDC, ( String ) null );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        // Test that we can add some new attributes with values
        EntryAttribute result = entry.put( "DC", atDC, "test1", "test2", "test3" );
        assertNotNull( result );
        assertEquals( "dc", result.getUpId() );
        assertEquals( 1, entry.size() );
        assertEquals( "DC", entry.get( atDC ).getUpId() );
        assertNotNull( entry.get( atDC ).get() );
        assertTrue( entry.contains( "dc", "test1" ) );
        assertTrue( entry.contains( "DC", "test2" ) );
        assertTrue( entry.contains( "DomainComponent", "test3" ) );
    }


    /**
     * Test the put( upId, AT, byte[]... ) method
     */
    @Test
    public void tesPutUpIDAtBytesElipsis() throws Exception
    {
        Dn dn = new Dn( "cn=test" );
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
        assertNull( entry.get( atPassword ).get().get() );

        // Check that we can use a null AttributeType
        entry.put( "userPassword", ( AttributeType ) null, ( byte[] ) null );
        assertEquals( 1, entry.size() );
        assertEquals( "userPassword", entry.get( atPassword ).getUpId() );
        assertTrue( entry.containsAttribute( "userPassword" ) );
        assertNull( entry.get( atPassword ).get().get() );

        // Test that we can use a null upId
        entry.put( null, atPassword, ( byte[] ) null );
        assertEquals( 1, entry.size() );
        assertEquals( "userPassword", entry.get( atPassword ).getUpId() );
        assertTrue( entry.containsAttribute( "userPassword" ) );
        assertNull( entry.get( atPassword ).get().get() );

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

        assertEquals( "userpassword", entry.get( atPassword ).getId() );

        // Test that we can add some new attributes with values
        byte[] test1 = Strings.getBytesUtf8("test1");
        byte[] test2 = Strings.getBytesUtf8("test2");
        byte[] test3 = Strings.getBytesUtf8("test3");

        EntryAttribute result = entry.put( "UserPassword", atPassword, test1, test2, test3 );
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
        Dn dn = new Dn( "cn=test" );
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
        entry.put( "commonName", atCN, ( Value<?> ) null );
        assertEquals( 1, entry.size() );
        assertEquals( "commonName", entry.get( atCN ).getUpId() );
        assertTrue( entry.containsAttribute( "cn" ) );
        assertNull( entry.get( atCN ).get().get() );

        // Check that we can use a null AttributeType
        entry.put( "commonName", ( AttributeType ) null, ( Value<?> ) null );

        assertEquals( 1, entry.size() );
        assertEquals( "commonName", entry.get( atCN ).getUpId() );
        assertTrue( entry.containsAttribute( "cn" ) );
        assertNull( entry.get( atCN ).get().get() );

        // Test that we can use a null upId
        entry.put( null, atCN, ( Value<?> ) null );
        assertEquals( 1, entry.size() );
        assertEquals( "cn", entry.get( atCN ).getUpId() );
        assertTrue( entry.containsAttribute( "cn" ) );
        assertNull( entry.get( atCN ).get().get() );

        // Test that we can't use an upId which is not compatible
        // with the AT
        try
        {
            entry.put( "sn", atCN, ( Value<?> ) null );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        // Test that we can add some new attributes with values
        Value<String> test1 = new StringValue( atCN, "test1" );
        Value<String> test2 = new StringValue( atCN, "test2" );
        Value<String> test3 = new StringValue( atCN, "test3" );

        EntryAttribute result = entry.put( "CN", atCN, test1, test2, test3 );
        assertNotNull( result );
        assertEquals( "cn", result.getUpId() );
        assertEquals( 1, entry.size() );
        assertEquals( "CN", entry.get( atCN ).getUpId() );
        assertNotNull( entry.get( atCN ).get() );
        assertTrue( entry.contains( "cn", "test1" ) );
        assertTrue( entry.contains( "CN", "test2" ) );
        assertTrue( entry.contains( "commonName", "test3" ) );
    }


    /**
     * Test the put( upId, SV... ) method
     */
    @Test
    public void tesPutUpIDSVElipsis() throws Exception
    {
        Dn dn = new Dn( "cn=test" );
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
        entry.put( "commonName", ( Value<?> ) null );
        assertEquals( 1, entry.size() );
        assertEquals( "commonName", entry.get( atCN ).getUpId() );
        assertTrue( entry.containsAttribute( "cn" ) );
        assertNull( entry.get( atCN ).get().get() );

        // Test that we can add some new attributes with values
        Value<String> test1 = new StringValue( atCN, "test1" );
        Value<String> test2 = new StringValue( atCN, "test2" );
        Value<String> test3 = new StringValue( atCN, "test3" );

        EntryAttribute result = entry.put( "CN", test1, test2, test3 );
        assertNotNull( result );
        assertEquals( "commonName", result.getUpId() );
        assertEquals( 1, entry.size() );
        assertEquals( "CN", entry.get( atCN ).getUpId() );
        assertNotNull( entry.get( atCN ).get() );
        assertTrue( entry.contains( "cn", "test1" ) );
        assertTrue( entry.contains( "CN", "test2" ) );
        assertTrue( entry.contains( "commonName", "test3" ) );
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

        EntryAttribute attrPWD = new DefaultEntryAttribute( atPwd, BYTES1, ( byte[] ) null, BYTES2 );

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

        EntryAttribute attrCN = new DefaultEntryAttribute( atDC, "test1", ( String ) null, "test2" );

        entry.put( attrCN );
        assertTrue( entry.remove( atDC, ( String ) null ) );
        assertTrue( entry.remove( atDC, "test1", "test2" ) );
        assertFalse( entry.containsAttribute( atDC ) );

        entry.add( atDC, "test1", ( String ) null, "test2" );
        assertTrue( entry.remove( atDC, ( String ) null ) );
        assertEquals( 2, entry.get( atDC ).size() );
        assertFalse( entry.contains( atDC, ( String ) null ) );
        assertTrue( entry.remove( atDC, "test1", "test3" ) );
        assertEquals( 1, entry.get( atDC ).size() );
        assertTrue( entry.contains( atDC, "test2" ) );
        assertFalse( entry.contains( atDC, "test1" ) );

        assertFalse( entry.remove( atDC, "test3" ) );
        assertFalse( entry.remove( atDC, "test" ) );
    }


    /**
     * Test method for remove( AttributeType, Value<?>... )
     */
    @Test
    public void testRemoveAttributeTypeValueArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        Value<String> strValue1 = new StringValue( atDC, "test1" );
        Value<String> strValue2 = new StringValue( atDC, "test2" );
        Value<String> strValue3 = new StringValue( atDC, "test3" );
        Value<String> strNullValue = new StringValue( atDC, null );

        Value<byte[]> binValue1 = new BinaryValue( atPwd, BYTES1 );

        EntryAttribute attrPWD = new DefaultEntryAttribute( atDC, "test1", ( String ) null, "test2" );

        entry.put( attrPWD );
        assertTrue( entry.remove( atDC, strNullValue ) );
        assertTrue( entry.remove( atDC, strValue1, strValue2 ) );
        assertFalse( entry.containsAttribute( atDC ) );

        entry.add( atDC, strValue1, strNullValue, strValue2 );
        assertTrue( entry.remove( atDC, strNullValue ) );
        assertEquals( 2, entry.get( atDC ).size() );
        assertFalse( entry.contains( atDC, strNullValue ) );
        assertTrue( entry.remove( atDC, strValue1, strValue3 ) );
        assertEquals( 1, entry.get( atDC ).size() );
        assertTrue( entry.contains( atDC, strValue2 ) );
        assertFalse( entry.contains( atDC, strValue1 ) );

        assertFalse( entry.remove( atDC, strValue3 ) );
        assertFalse( entry.remove( atDC, binValue1 ) );
    }


    /**
     * Test method for remove( EntryAttribute... )
     */
    @Test
    public void testRemoveEntryAttribute() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        EntryAttribute attrOC = new DefaultEntryAttribute( atOC, "top", "person" );
        EntryAttribute attrCN = new DefaultEntryAttribute( atCN, "test1", "test2" );
        EntryAttribute attrSN = new DefaultEntryAttribute( atSN, "Test1", "Test2" );
        EntryAttribute attrPWD = new DefaultEntryAttribute( atPwd, BYTES1, BYTES2 );

        entry.put( attrOC, attrCN, attrSN, attrPWD );

        List<EntryAttribute> removed = entry.remove( attrSN, attrPWD );

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

        EntryAttribute attrOC = new DefaultEntryAttribute( atOC, "top", "person" );
        EntryAttribute attrCN = new DefaultEntryAttribute( atCN, "test1", "test2" );
        EntryAttribute attrSN = new DefaultEntryAttribute( atSN, "Test1", "Test2" );
        EntryAttribute attrPWD = new DefaultEntryAttribute( atPwd, BYTES1, BYTES2 );

        entry.put( attrOC, attrCN, attrSN, attrPWD );

        entry.removeAttributes( atCN, atSN );

        assertFalse( entry.containsAttribute( "cn", "sn" ) );
        assertTrue( entry.containsAttribute( "objectclass", "userpassword" ) );

        List<EntryAttribute> removed = entry.removeAttributes( ( AttributeType ) null );
        assertNull( removed );

        removed = entry.removeAttributes( atC );
        assertNull( removed );
    }


    /**
     * Test method for removeAttributes( String... )
     */
    @Test
    public void testRemoveAttributesStringArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        EntryAttribute attrOC = new DefaultEntryAttribute( atOC, "top", "person" );
        EntryAttribute attrCN = new DefaultEntryAttribute( atCN, "test1", "test2" );
        EntryAttribute attrSN = new DefaultEntryAttribute( atSN, "Test1", "Test2" );
        EntryAttribute attrPWD = new DefaultEntryAttribute( atPwd, BYTES1, BYTES2 );

        entry.put( attrOC, attrCN, attrSN, attrPWD );

        entry.removeAttributes( "CN", "SN" );

        assertFalse( entry.containsAttribute( "cn", "sn" ) );
        assertTrue( entry.containsAttribute( "objectclass", "userpassword" ) );

        List<EntryAttribute> removed = entry.removeAttributes( "badId" );
        assertNull( removed );

        removed = entry.removeAttributes( "l" );
        assertNull( removed );

        removed = entry.removeAttributes( ( String ) null );
        assertNull( removed );
    }


    /**
     * Test method for remove( String, byte[]... )
     */
    @Test
    public void testRemoveStringByteArrayArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        EntryAttribute attrPWD = new DefaultEntryAttribute( atPwd, BYTES1, ( byte[] ) null, BYTES2 );

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

        EntryAttribute attrCN = new DefaultEntryAttribute( atDC, "test1", ( String ) null, "test2" );

        assertFalse( entry.remove( ( String ) null, "test1" ) );
        assertFalse( entry.remove( " ", "test1" ) );
        assertFalse( entry.remove( "badId", "test1" ) );

        entry.put( attrCN );
        assertTrue( entry.remove( "dc", ( String ) null ) );
        assertTrue( entry.remove( "domainComponent", "test1", "test2" ) );
        assertFalse( entry.containsAttribute( atDC ) );

        entry.add( atDC, "test1", ( String ) null, "test2" );
        assertTrue( entry.remove( "0.9.2342.19200300.100.1.25", ( String ) null ) );
        assertEquals( 2, entry.get( atDC ).size() );
        assertFalse( entry.contains( atDC, ( byte[] ) null ) );
        assertTrue( entry.remove( "DOMAINCOMPONENT", "test1", "test3" ) );
        assertEquals( 1, entry.get( atDC ).size() );
        assertTrue( entry.contains( atDC, "test2" ) );
        assertFalse( entry.contains( atDC, "test1" ) );

        assertFalse( entry.remove( "Dc", "test3" ) );
        assertFalse( entry.remove( "dC", "whatever" ) );
    }


    /**
     * Test method for remove( String, Value<?>... )
     */
    @Test
    public void testRemoveStringValueArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        Value<String> strValue1 = new StringValue( atDC, "test1" );
        Value<String> strValue2 = new StringValue( atDC, "test2" );
        Value<String> strValue3 = new StringValue( atDC, "test3" );
        Value<String> strNullValue = new StringValue( atDC, null );

        Value<byte[]> binValue1 = new BinaryValue( atPwd, BYTES1 );

        EntryAttribute attrPWD = new DefaultEntryAttribute( atDC, "test1", ( String ) null, "test2" );

        entry.put( attrPWD );
        assertTrue( entry.remove( "DC", strNullValue ) );
        assertTrue( entry.remove( "DomainComponent", strValue1, strValue2 ) );
        assertFalse( entry.containsAttribute( atDC ) );

        entry.add( atDC, strValue1, strNullValue, strValue2 );
        assertTrue( entry.remove( "0.9.2342.19200300.100.1.25", strNullValue ) );
        assertEquals( 2, entry.get( atDC ).size() );
        assertFalse( entry.contains( atDC, strNullValue ) );
        assertTrue( entry.remove( "  dc", strValue1, strValue3 ) );
        assertEquals( 1, entry.get( atDC ).size() );
        assertTrue( entry.contains( atDC, strValue2 ) );
        assertFalse( entry.contains( atDC, strValue1 ) );

        assertFalse( entry.remove( " Dc", strValue3 ) );
        assertFalse( entry.remove( "dC ", binValue1 ) );
    }


    /**
     * Test the remove( upId...) method
     */
    @Test
    public void testRemoveUpIdElipsis() throws Exception
    {
        Dn dn = new Dn( "cn=test" );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );

        AttributeType atPassword = schemaManager.lookupAttributeTypeRegistry( "userPassword" );

        byte[] b1 = Strings.getBytesUtf8("test1");
        byte[] b2 = Strings.getBytesUtf8("test2");

        Value<String> test1 = new StringValue( atCN, "test1" );
        Value<String> test2 = new StringValue( atCN, "test2" );

        Value<byte[]> testB1 = new BinaryValue( atPassword, b1 );
        Value<byte[]> testB2 = new BinaryValue( atPassword, b2 );

        // test a removal of an non existing attribute
        List<EntryAttribute> removed = entry.removeAttributes( atCN );
        assertNull( removed );

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

        removed = entry.removeAttributes( "badAttribute" );

        assertNull( removed );
    }


    /**
     * Test the set(AT...) method
     */
    @Test
    public void testSetATElipsis() throws Exception
    {
        Dn dn = new Dn( "cn=test" );
        Entry entry = new DefaultEntry( schemaManager, dn );

        List<EntryAttribute> result = null;

        // First check that this method fails if we pass an empty list of ATs
        result = entry.set( ( AttributeType ) null );
        assertNull( result );

        // Now, check what we get when adding one existing AT
        result = entry.set( atSN );

        assertNull( result );
        EntryAttribute sa = entry.get( "sn" );
        assertNotNull( sa );
        assertEquals( atSN, sa.getAttributeType() );
        assertEquals( "sn", sa.getAttributeType().getName() );

        // Add two AT now
        AttributeType atGN = schemaManager.lookupAttributeTypeRegistry( "givenname" );
        AttributeType atStreet = schemaManager.lookupAttributeTypeRegistry( "2.5.4.9" );
        result = entry.set( atL, atC, atGN, atStreet );

        assertNull( result );

        sa = entry.get( "l" );
        assertNotNull( sa );
        assertEquals( atL, sa.getAttributeType() );
        assertEquals( "l", sa.getAttributeType().getName() );

        sa = entry.get( "c" );
        assertNotNull( sa );
        assertEquals( atC, sa.getAttributeType() );
        assertEquals( "c", sa.getAttributeType().getName() );

        sa = entry.get( "2.5.4.9" );
        assertNotNull( sa );
        assertEquals( atStreet, sa.getAttributeType() );
        assertEquals( "street", sa.getAttributeType().getName() );

        sa = entry.get( "givenName" );
        assertNotNull( sa );
        assertEquals( atGN, sa.getAttributeType() );
        assertEquals( "givenName", sa.getAttributeType().getName() );

        // Now try to add existing ATs
        // First, set some value to the modified AT
        sa = entry.get( "sn" );
        sa.add( "test" );

        // Check that the value has been added to the entry
        assertEquals( "test", entry.get( "sn" ).get().getString() );

        // Now add a new SN empty AT : it should replace the existing one.
        AttributeType atSNEmpty = schemaManager.lookupAttributeTypeRegistry( "sn" );
        sa = entry.set( atSNEmpty ).get( 0 );
        assertEquals( "test", sa.get().getString() );
        assertNotNull( entry.get( "sn" ) );
        assertNull( entry.get( "sn" ).get() );

        // Last, not least, put an ObjectClass AT
        AttributeType OBJECT_CLASS_AT = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.OBJECT_CLASS_AT );

        entry.set( OBJECT_CLASS_AT );

        assertNotNull( entry.get( "objectClass" ) );

        EntryAttribute oc = entry.get( "objectClass" );

        assertEquals( OBJECT_CLASS_AT, oc.getAttributeType() );
        assertNull( oc.get() );
    }


    /**
     * Test the set( upId ) method
     */
    @Test
    public void testSetUpID() throws Exception
    {
        Dn dn = new Dn( "cn=test" );
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );
        List<EntryAttribute> result = null;

        // First check that this method fails if we pass a null or empty ID
        try
        {
            result = entry.set( ( String ) null );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            // expected
        }

        result = entry.set( "  " );
        assertNull( result );

        // Now check that we can't put invalid IDs
        result = entry.set( "ThisIsNotAnAttributeType" );
        assertNull( result );

        // Now, check what we get when adding one existing AT
        result = entry.set( "sn" );

        assertNull( result );

        EntryAttribute sa = entry.get( "sn" );
        assertNotNull( sa );
        assertEquals( "sn", sa.getId() );

        // Add different upIds now
        AttributeType atGN = schemaManager.lookupAttributeTypeRegistry( "givenname" );
        AttributeType atStreet = schemaManager.lookupAttributeTypeRegistry( "2.5.4.9" );

        entry.set( "L" );
        entry.set( "CountryName" );
        entry.set( "gn" );
        entry.set( "2.5.4.9" );

        sa = entry.get( "l" );
        assertNotNull( sa );
        assertEquals( atL, sa.getAttributeType() );
        assertEquals( "l", sa.getId() );
        assertEquals( "L", sa.getUpId() );

        sa = entry.get( "c" );
        assertNotNull( sa );
        assertEquals( atC, sa.getAttributeType() );
        assertEquals( "countryname", sa.getId() );
        assertEquals( "CountryName", sa.getUpId() );

        sa = entry.get( "2.5.4.9" );
        assertNotNull( sa );
        assertEquals( atStreet, sa.getAttributeType() );
        assertEquals( "2.5.4.9", sa.getId() );
        assertEquals( "2.5.4.9", sa.getUpId() );

        sa = entry.get( "givenName" );
        assertNotNull( sa );
        assertEquals( atGN, sa.getAttributeType() );
        assertEquals( "gn", sa.getId() );
        assertEquals( "gn", sa.getUpId() );

        // Now try to add existing ATs
        // First, set some value to the modified AT
        sa = entry.get( "sn" );
        sa.add( "test" );

        // Check that the value has been added to the entry
        assertEquals( "test", entry.get( "sn" ).get().getString() );

        // Now add a new SN empty AT : it should replace the existing one.
        AttributeType atSNEmpty = schemaManager.lookupAttributeTypeRegistry( "sn" );
        sa = entry.set( atSNEmpty ).get( 0 );
        assertEquals( "test", sa.get().getString() );
        assertNotNull( entry.get( "sn" ) );
        assertNull( entry.get( "sn" ).get() );
    }


    /**
     * Test method for set( AttributeType... )
     */
    @Test
    public void testSetAttributeTypeArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        entry.add( "ObjectClass", "top", "person" );
        entry.add( "cn", "test1", "test2" );
        entry.add( "sn", "Test" );

        List<EntryAttribute> removed = entry.set( atOC, atCN, atPwd );

        assertEquals( 4, entry.size() );
        assertNotNull( entry.get( "objectclass" ) );
        assertNotNull( entry.get( "cn" ) );
        assertNotNull( entry.get( "userPassword" ) );
        assertNotNull( entry.get( "sn" ) );

        assertNull( entry.get( "objectclass" ).get() );
        assertNull( entry.get( "cn" ).get() );
        assertNull( entry.get( "userPassword" ).get() );
        assertNotNull( entry.get( "sn" ).get() );

        assertNotNull( removed );
        assertEquals( 2, removed.size() );
    }


    /**
     * Test method for set( String... )
     */
    @Test
    public void testSetStringArray() throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

        entry.add( "ObjectClass", "top", "person" );
        entry.add( "cn", "test1", "test2" );
        entry.add( "sn", "Test" );

        List<EntryAttribute> removed = entry.set( "objectClass", "CN", "givenName" );

        assertEquals( 4, entry.size() );
        assertNotNull( entry.get( "objectclass" ) );
        assertNotNull( entry.get( "cn" ) );
        assertNotNull( entry.get( "givenname" ) );
        assertNotNull( entry.get( "sn" ) );

        assertNull( entry.get( "objectclass" ).get() );
        assertNull( entry.get( "cn" ).get() );
        assertNull( entry.get( "givenname" ).get() );
        assertNotNull( entry.get( "sn" ).get() );

        assertNotNull( removed );
        assertEquals( 2, removed.size() );
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
        Dn dn = new Dn( "cn=test" );
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

        assertEquals( "Entry\n    dn[n]: dc=example,dc=com\n", entry.toString() );

        Value<String> strValueTop = new StringValue( "top" );
        Value<String> strValuePerson = new StringValue( "person" );
        Value<String> strNullValue = new StringValue( ( String ) null );

        Value<byte[]> binValue1 = new BinaryValue( BYTES1 );
        Value<byte[]> binValue2 = new BinaryValue( BYTES2 );
        Value<byte[]> binNullValue = new BinaryValue( ( byte[] ) null );

        entry.put( "ObjectClass", atOC, strValueTop, strValuePerson, strNullValue );
        entry.put( "UserPassword", atPwd, binValue1, binValue2, binNullValue );

        String expected = "Entry\n" + "    dn[n]: dc=example,dc=com\n" + "    ObjectClass: top\n"
            + "    ObjectClass: person\n" + "    ObjectClass: ''\n" + "    UserPassword: '0x61 0x62 '\n"
            + "    UserPassword: '0x62 '\n" + "    UserPassword: ''\n";

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
        clientEntry.setDn( new Dn( "ou=system" ) );
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
