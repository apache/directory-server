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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;

import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.DeepTrimToLowerNormalizer;
import org.apache.directory.shared.ldap.schema.OidNormalizer;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A test class for the DefaultClientEntry class
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DefaultClientEntryTest
{
    private static LdapDN EXAMPLE_DN;
    private static final byte[] BYTES1 = new byte[]{ 'a', 'b' };
    private static final byte[] BYTES2 = new byte[]{ 'b' };
    private static final byte[] BYTES3 = new byte[]{ 'c' };
    private static Map<String, OidNormalizer> oids;
    
    
    /**
     * Helper method which creates an entry with 4 attributes.
     */
    private Entry createEntry()
    {
        try
        {
            Entry entry = new DefaultClientEntry( EXAMPLE_DN );
            
            EntryAttribute attrOC = new DefaultClientAttribute( "objectClass", "top", "person" );
            EntryAttribute attrCN = new DefaultClientAttribute( "cn", "test1", "test2" );
            EntryAttribute attrSN = new DefaultClientAttribute( "sn", "Test1", "Test2" );
            EntryAttribute attrPWD = new DefaultClientAttribute( "userPassword", BYTES1, BYTES2 );
            
            entry.put( attrOC, attrCN, attrSN, attrPWD );
            
            return entry;
        }
        catch ( NamingException ne )
        {
            // Do nothing
            return null;
        }
    }


    /**
     * Serialize a ClientEntry
     */
    private ByteArrayOutputStream serializeValue( ClientEntry value ) throws IOException
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
     * Deserialize a ClientEntry
     */
    private ClientEntry deserializeValue( ByteArrayOutputStream out ) throws IOException, ClassNotFoundException
    {
        ObjectInputStream oIn = null;
        ByteArrayInputStream in = new ByteArrayInputStream( out.toByteArray() );

        try
        {
            oIn = new ObjectInputStream( in );

            ClientEntry value = ( ClientEntry ) oIn.readObject();

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
        EXAMPLE_DN = new LdapDN( "dc=example,dc=com" );

        oids = new HashMap<String, OidNormalizer>();

        oids.put( "dc", new OidNormalizer( "dc", new DeepTrimToLowerNormalizer() ) );
        oids.put( "domaincomponent", new OidNormalizer( "dc", new DeepTrimToLowerNormalizer() ) );
        oids.put( "0.9.2342.19200300.100.1.25", new OidNormalizer( "dc", new DeepTrimToLowerNormalizer() ) );

        oids.put( "ou", new OidNormalizer( "ou", new DeepTrimToLowerNormalizer() ) );
        oids.put( "organizationalUnitName", new OidNormalizer( "ou", new DeepTrimToLowerNormalizer() ) );
        oids.put( "2.5.4.11", new OidNormalizer( "ou", new DeepTrimToLowerNormalizer() ) );
    }


    /**
     * Test method for DefaultClientEntry()
     */
    @Test
    public void testDefaultClientEntry()
    {
        Entry entry = new DefaultClientEntry();
        
        assertNotNull( entry );
        assertEquals( LdapDN.EMPTY_LDAPDN, entry.getDn() );
        assertEquals( 0, entry.size() );
    }


    /**
     * Test method for DefaultClientEntry( LdapDN )
     */
    @Test
    public void testDefaultClientEntryLdapDN()
    {
        Entry entry = new DefaultClientEntry( EXAMPLE_DN );
        
        assertNotNull( entry );
        assertNotNull( entry.getDn() );
        assertEquals( EXAMPLE_DN, entry.getDn() );
        assertEquals( 0, entry.size() );
    }


    /**
     * Test method for DefaultClientEntry( LdapDN, String... )
     */
    @Test
    public void testDefaultClientEntryLdapDNStringArray()
    {
        Entry entry = new DefaultClientEntry( EXAMPLE_DN, "ObjectClass", "cn", "sn" );
        
        assertNotNull( entry );
        assertNotNull( entry.getDn() );
        assertEquals( EXAMPLE_DN, entry.getDn() );
        assertEquals( 3, entry.size() );
        assertTrue( entry.containsAttribute( "OBJECTCLASS" ) );
        assertTrue( entry.containsAttribute( "CN" ) );
        assertTrue( entry.containsAttribute( "SN" ) );

        try
        {
            new DefaultClientEntry( EXAMPLE_DN, "ObjectClass", (String)null, "sn" );
            fail();
        }
        catch( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        try
        {
            new DefaultClientEntry( EXAMPLE_DN, "ObjectClass", " ", "sn" );
            fail();
        }
        catch( IllegalArgumentException iae )
        {
            assertTrue( true );
        }
    }


    /**
     * Test method for DefaultClientEntry( LdapDN, EntryAttribute... )
     */
    @Test
    public void testDefaultClientEntryLdapDNEntryAttributeArray()
    {
        EntryAttribute attrOC = new DefaultClientAttribute( "objectClass", "top", "person" );
        EntryAttribute attrCN = new DefaultClientAttribute( "cn", "test1", "test2" );
        EntryAttribute attrPWD = new DefaultClientAttribute( "userPassword", BYTES1, BYTES2 );
        
        Entry entry = new DefaultClientEntry( EXAMPLE_DN, attrOC, attrCN, attrPWD );
        
        assertNotNull( entry );
        assertNotNull( entry.getDn() );
        assertEquals( EXAMPLE_DN, entry.getDn() );
        assertEquals( 3, entry.size() );
        assertTrue( entry.containsAttribute( "OBJECTCLASS" ) );
        assertTrue( entry.containsAttribute( "CN" ) );
        assertTrue( entry.containsAttribute( "userPassword" ) );
        
        entry = new DefaultClientEntry( EXAMPLE_DN, attrOC, attrCN, attrOC );
        assertNotNull( entry );
        assertNotNull( entry.getDn() );
        assertEquals( EXAMPLE_DN, entry.getDn() );
        assertEquals( 2, entry.size() );
        assertTrue( entry.containsAttribute( "OBJECTCLASS" ) );
        assertTrue( entry.containsAttribute( "CN" ) );
        
        entry = new DefaultClientEntry( EXAMPLE_DN, attrOC, (EntryAttribute)null );
        assertNotNull( entry );
        assertNotNull( entry.getDn() );
        assertEquals( EXAMPLE_DN, entry.getDn() );
        assertEquals( 1, entry.size() );
        assertTrue( entry.containsAttribute( "OBJECTCLASS" ) );
    }


    /**
     * Test method for add( EntryAttribute... )
     */
    @Test
    public void testAddEntryAttributeArray() throws NamingException
    {
        Entry entry = createEntry();
        
        assertEquals( 4, entry.size() );
        assertTrue( entry.containsAttribute( "ObjectClass" ) );
        assertTrue( entry.containsAttribute( "CN" ) );
        assertTrue( entry.containsAttribute( "  sn  " ) );
        assertTrue( entry.containsAttribute( "userPassword" ) );
    
        EntryAttribute attr = entry.get( "objectclass" );
        assertEquals( 2, attr.size() );
        
        EntryAttribute attrCN2 = new DefaultClientAttribute( "cn", "test1", "test3" );
        entry.add( attrCN2 );
        assertEquals( 4, entry.size() );
        attr = entry.get( "cn" );
        assertEquals( 3, attr.size() );
        assertTrue( attr.contains( "test1", "test2", "test3" ) );
        
        // Check adding some byte[] values (they will be transformed to Strings)
        attrCN2.put( BYTES1, BYTES2 );
        entry.add( attrCN2 );
        assertEquals( 4, entry.size() );
        attr = entry.get( "cn" );
        assertEquals( 5, attr.size() );
        assertTrue( attr.contains( "test1", "test2", "test3", "ab", "b" ) );
    }


    /**
     * Test method for add( String, byte[]... )
     */
    @Test
    public void testAddStringByteArrayArray() throws NamingException
    {
        Entry entry = new DefaultClientEntry();
        
        entry.add( "userPassword", (byte[])null );
        assertEquals( 1, entry.size() );
        EntryAttribute attributePWD = entry.get( "userPassword" );
        assertEquals( 1, attributePWD.size() );
        assertNotNull( attributePWD.get() );
        assertNull( attributePWD.get().get() );
        
        entry.add( "jpegPhoto", BYTES1, BYTES1, BYTES2 );
        assertEquals( 2, entry.size() );
        EntryAttribute attributeJPG = entry.get( "jpegPhoto" );
        assertEquals( 2, attributeJPG.size() );
        assertNotNull( attributeJPG.get() );
        assertTrue( attributeJPG.contains( BYTES1 ) );
        assertTrue( attributeJPG.contains( BYTES2 ) );
    }


    /**
     * Test method for add( String, String... )
     */
    @Test
    public void testAddStringStringArray() throws NamingException
    {
        Entry entry = new DefaultClientEntry();
        
        entry.add( "cn", (String)null );
        assertEquals( 1, entry.size() );
        EntryAttribute attributeCN = entry.get( "cn" );
        assertEquals( 1, attributeCN.size() );
        assertNotNull( attributeCN.get() );
        assertNull( attributeCN.get().get() );
        
        entry.add( "sn", "test", "test", "TEST" );
        assertEquals( 2, entry.size() );
        EntryAttribute attributeSN = entry.get( "sn" );
        assertEquals( 2, attributeSN.size() );
        assertNotNull( attributeSN.get() );
        assertTrue( attributeSN.contains( "test" ) );
        assertTrue( attributeSN.contains( "TEST" ) );
    }


    /**
     * Test method for add( String, Value<?>... )
     */
    @Test
    public void testAddStringValueArray() throws NamingException
    {
        Entry entry = new DefaultClientEntry();
        
        Value<String> value = new ClientStringValue( (String)null );
        
        entry.add( "cn", value );
        assertEquals( 1, entry.size() );
        EntryAttribute attributeCN = entry.get( "cn" );
        assertEquals( 1, attributeCN.size() );
        assertNotNull( attributeCN.get() );
        assertNull( attributeCN.get().get() );
        
        Value<String> value1 = new ClientStringValue( "test1" );
        Value<String> value2 = new ClientStringValue( "test2" );
        Value<String> value3 = new ClientStringValue( "test1" );

        entry.add( "sn", value1, value2, value3 );
        assertEquals( 2, entry.size() );
        EntryAttribute attributeSN = entry.get( "sn" );
        assertEquals( 2, attributeSN.size() );
        assertNotNull( attributeSN.get() );
        assertTrue( attributeSN.contains( value1 ) );
        assertTrue( attributeSN.contains( value2 ) );
        
        Value<byte[]> value4 = new ClientBinaryValue( BYTES1 );
        entry.add( "l", value1, value4 );
        assertEquals( 3, entry.size() );
        EntryAttribute attributeL = entry.get( "l" );
        assertEquals( 2, attributeL.size() );
        assertNotNull( attributeL.get() );
        assertTrue( attributeL.contains( value1 ) );
        
        // The byte[] value must have been transformed to a String
        assertTrue( attributeL.contains( "ab" ) );
    }




    /**
     * Test method for clear()
     */
    @Test
    public void testClear() throws NamingException
    {
        Entry entry = new DefaultClientEntry( EXAMPLE_DN );
        
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
    public void testClone() throws NamingException
    {
        Entry entry1 = new DefaultClientEntry();
        
        Entry entry2 = entry1.clone();
        
        assertEquals( entry1, entry2 );
        entry2.setDn( EXAMPLE_DN );
        
        assertEquals( LdapDN.EMPTY_LDAPDN, entry1.getDn() );
        
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
        
        entry1.add( "sn", (String)null );
        assertFalse( entry2.containsAttribute( "sn" ) );
    }

    
    /**
     * Test method for contains( EntryAttribute... )
     */
    @Test
    public void testContainsEntryAttributeArray() throws NamingException
    {
        Entry entry = new DefaultClientEntry( EXAMPLE_DN );
        
        EntryAttribute attrOC = new DefaultClientAttribute( "objectClass", "top", "person" );
        EntryAttribute attrCN = new DefaultClientAttribute( "cn", "test1", "test2" );
        EntryAttribute attrSN = new DefaultClientAttribute( "sn", "Test1", "Test2" );
        EntryAttribute attrPWD = new DefaultClientAttribute( "userPassword", BYTES1, BYTES2 );

        assertFalse( entry.contains( attrOC, attrCN ) );
        
        entry.add( attrOC, attrCN );

        assertTrue( entry.contains( attrOC, attrCN ) );
        assertFalse( entry.contains( attrOC, attrCN, attrSN ) );
        
        entry.add( attrSN, attrPWD );

        assertTrue( entry.contains( attrSN, attrPWD ) );
    }


    /**
     * Test method for contains( String, byte[]... )
     */
    @Test
    public void testContainsStringByteArray() throws NamingException
    {
        Entry entry = new DefaultClientEntry( EXAMPLE_DN );
        
        assertFalse( entry.containsAttribute( "objectClass" ) );
        
        EntryAttribute attrPWD = new DefaultClientAttribute( "userPassword", BYTES1, (byte[])null, BYTES2 );

        entry.add( attrPWD );
        
        assertTrue( entry.contains( "  userPASSWORD  ", BYTES1, BYTES2 ) );
        assertTrue( entry.contains( "  userPASSWORD  ", (byte[])null ) );
        
        // We can search for byte[] using Strings. the strings will be converted to byte[]
        assertTrue( entry.contains( "  userPASSWORD  ", "ab", "b" ) );

        assertFalse( entry.contains( "  userPASSWORD  ", "ab", "b", "d" ) );
    }


    /**
     * Test method for contains( String, String... )
     */
    @Test
    public void testContainsStringStringArray() throws NamingException
    {
        Entry entry = new DefaultClientEntry( EXAMPLE_DN );
        
        assertFalse( entry.containsAttribute( "objectClass" ) );
        
        EntryAttribute attrOC = new DefaultClientAttribute( "objectClass", "top", "person" );
        EntryAttribute attrCN = new DefaultClientAttribute( "cn", "test1", "test2" );
        EntryAttribute attrSN = new DefaultClientAttribute( "sn", "Test1", "Test2", (String)null );
        EntryAttribute attrPWD = new DefaultClientAttribute( "userPassword", BYTES1, BYTES2 );

        entry.add( attrOC, attrCN, attrSN, attrPWD );
        
        assertTrue( entry.contains( "OBJECTCLASS", "top", "person" ) );
        assertTrue( entry.contains( " cn ", "test1", "test2" ) );
        assertTrue( entry.contains( "Sn", "Test1", "Test2", (String)null ) );
        assertTrue( entry.contains( "  userPASSWORD  ", "ab", "b" ) );
        
        assertFalse( entry.contains( "OBJECTCLASS", "PERSON" ) );
        assertFalse( entry.contains( " cn ", "test1", "test3" ) );
        assertFalse( entry.contains( "Sn", "Test" ) );
        assertFalse( entry.contains( "  userPASSWORD  ", (String)null ) );
    }


    /**
     * Test method for contains( Sring, Value<?>... )
     */
    @Test
    public void testContainsStringValueArray() throws NamingException
    {
        Entry entry = new DefaultClientEntry( EXAMPLE_DN );
        
        assertFalse( entry.containsAttribute( "objectClass" ) );
        
        EntryAttribute attrCN = new DefaultClientAttribute( "cn", "test1", "test2", (String)null );
        EntryAttribute attrPWD = new DefaultClientAttribute( "userPassword", BYTES1, BYTES2, (byte[])null );

        entry.add( attrCN, attrPWD );
        
        Value<String> strValue1 = new ClientStringValue( "test1" );
        Value<String> strValue2 = new ClientStringValue( "test2" );
        Value<String> strValue3 = new ClientStringValue( "test3" );
        Value<String> strNullValue = new ClientStringValue( null);

        Value<byte[]> binValue1 = new ClientBinaryValue( BYTES1 );
        Value<byte[]> binValue2 = new ClientBinaryValue( BYTES2 );
        Value<byte[]> binValue3 = new ClientBinaryValue( BYTES3 );
        Value<byte[]> binNullValue = new ClientBinaryValue( null );

        assertTrue( entry.contains( "CN", strValue1, strValue2, strNullValue ) );
        assertTrue( entry.contains( "userpassword", binValue1, binValue2, binNullValue ) );
        
        assertFalse( entry.contains( "cn", strValue3 ) );
        assertFalse( entry.contains( "UserPassword", binValue3 ) );
    }


    /**
     * Test method for containsAttribute( String )
     */
    @Test
    public void testContainsAttribute() throws NamingException
    {
        Entry entry = new DefaultClientEntry( EXAMPLE_DN );
        
        assertFalse( entry.containsAttribute( "objectClass" ) );
        
        EntryAttribute attrOC = new DefaultClientAttribute( "objectClass", "top", "person" );
        EntryAttribute attrCN = new DefaultClientAttribute( "cn", "test1", "test2" );
        EntryAttribute attrSN = new DefaultClientAttribute( "sn", "Test1", "Test2" );
        EntryAttribute attrPWD = new DefaultClientAttribute( "userPassword", BYTES1, BYTES2 );

        entry.add( attrOC, attrCN, attrSN, attrPWD );
        
        assertTrue( entry.containsAttribute( "OBJECTCLASS" ) );
        assertTrue( entry.containsAttribute( " cn " ) );
        assertTrue( entry.containsAttribute( "Sn" ) );
        assertTrue( entry.containsAttribute( "  userPASSWORD  " ) );
        
        entry.clear();

        assertFalse( entry.containsAttribute( "OBJECTCLASS" ) );
        assertFalse( entry.containsAttribute( " cn " ) );
        assertFalse( entry.containsAttribute( "Sn" ) );
        assertFalse( entry.containsAttribute( "  userPASSWORD  " ) );
    }


    /**
     * Test method for equals()
     */
    @Test
    public void testEqualsObject() throws NamingException
    {
        Entry entry1 = new DefaultClientEntry();
        Entry entry2 = new DefaultClientEntry();
        
        assertEquals( entry1, entry2 );
        
        entry1.setDn( EXAMPLE_DN );
        assertNotSame( entry1, entry2 );
        
        entry2.setDn( EXAMPLE_DN );
        assertEquals( entry1, entry2 );

        EntryAttribute attrOC = new DefaultClientAttribute( "objectClass", "top", "person" );
        EntryAttribute attrCN = new DefaultClientAttribute( "cn", "test1", "test2" );
        EntryAttribute attrSN = new DefaultClientAttribute( "sn", "Test1", "Test2" );
        EntryAttribute attrPWD = new DefaultClientAttribute( "userPassword", BYTES1, BYTES2 );
        
        entry1.put( attrOC, attrCN, attrSN, attrPWD );
        entry2.put( attrOC, attrCN, attrSN );
        assertNotSame( entry1, entry2 );
        
        entry2.put( attrPWD );
        assertEquals( entry1, entry2 );
        
        EntryAttribute attrL1 = new DefaultClientAttribute( "l", "Paris", "New-York" );
        EntryAttribute attrL2 = new DefaultClientAttribute( "l", "Paris", "Tokyo" );
        
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
     * Test method for get( String )
     */
    @Test
    public void testGet() throws NamingException
    {
        Entry entry = new DefaultClientEntry( EXAMPLE_DN );
        
        assertNull( entry.get( "objectClass" ) );
        
        EntryAttribute attrOC = new DefaultClientAttribute( "objectClass", "top", "person" );
        EntryAttribute attrCN = new DefaultClientAttribute( "cn", "test1", "test2" );
        EntryAttribute attrSN = new DefaultClientAttribute( "sn", "Test1", "Test2" );
        EntryAttribute attrPWD = new DefaultClientAttribute( "userPassword", BYTES1, BYTES2 );

        entry.add( attrOC, attrCN, attrSN, attrPWD );
        
        assertNotNull( entry.get( "  CN  " ) );
        EntryAttribute attribute = entry.get( "cN" );
        
        assertEquals( attribute, attrCN );
        
        assertNull( entry.get( null ) );
        assertNull( entry.get( "  " ) );
        assertNull( entry.get( "l" ) );
    }


    /**
     * Test method for getDN()
     */
    @Test
    public void testGetDn() throws InvalidNameException 
    {
        Entry entry = new DefaultClientEntry( EXAMPLE_DN );
        
        assertEquals( EXAMPLE_DN, entry.getDn() );
        
        LdapDN testDn = new LdapDN( "cn=test" );
        entry.setDn( testDn );
        
        assertEquals( testDn, entry.getDn() );
    }


    /**
     * Test method for hashcode()
     */
    @Test
    public void testHashCode() throws InvalidNameException, NamingException
    {
        Entry entry1 = new DefaultClientEntry( EXAMPLE_DN );
        Entry entry2 = new DefaultClientEntry( EXAMPLE_DN );
        
        assertEquals( entry1.hashCode(), entry2.hashCode() );
        
        entry2.setDn( new LdapDN( "ou=system,dc=com" ) );
        assertNotSame( entry1.hashCode(), entry2.hashCode() );
        
        entry2.setDn( EXAMPLE_DN );
        assertEquals( entry1.hashCode(), entry2.hashCode() );
        
        
        EntryAttribute attrOC = new DefaultClientAttribute( "objectClass", "top", "person" );
        EntryAttribute attrCN = new DefaultClientAttribute( "cn", "test1", "test2" );
        EntryAttribute attrSN = new DefaultClientAttribute( "sn", "Test1", "Test2" );
        EntryAttribute attrPWD = new DefaultClientAttribute( "userPassword", BYTES1, BYTES2 );

        entry1.add( attrOC, attrCN, attrSN, attrPWD );
        entry2.add( attrOC, attrCN, attrSN, attrPWD );

        assertEquals( entry1.hashCode(), entry2.hashCode() );
        
        Entry entry3 = new DefaultClientEntry( EXAMPLE_DN );
        entry3.add( attrOC, attrSN, attrCN, attrPWD );

        assertEquals( entry1.hashCode(), entry3.hashCode() );
    }

    
    /**
     * Test method for hasObjectClass( String )
     */
    @Test
    public void testHasObjectClass() throws NamingException
    {
        Entry entry = new DefaultClientEntry( EXAMPLE_DN );
        
        assertFalse( entry.containsAttribute( "objectClass" ) );
        assertFalse( entry.hasObjectClass( "top" ) );
        
        entry.add( new DefaultClientAttribute( "objectClass", "top", "person" ) );
        
        assertTrue( entry.hasObjectClass( "top" ) );
        assertTrue( entry.hasObjectClass( "person" ) );
        assertFalse( entry.hasObjectClass( "inetorgperson" ) );
        assertFalse( entry.hasObjectClass( null ) );
        assertFalse( entry.hasObjectClass( "" ) );
    }

    
    /**
     * Test method for Iterator()
     */
    @Test
    public void testIterator() throws NamingException
    {
        Entry entry = createEntry();
        
        Iterator<EntryAttribute> iterator = entry.iterator();
        
        assertTrue( iterator.hasNext() );
        
        Set<String> expectedIds = new HashSet<String>();
        expectedIds.add( "objectclass" );
        expectedIds.add( "cn" );
        expectedIds.add( "sn" );
        expectedIds.add( "userpassword" );
        
        while ( iterator.hasNext() )
        {
            EntryAttribute attribute = iterator.next();
            
            String id = attribute.getId();
            assertTrue( expectedIds.contains( id ) );
            expectedIds.remove( id );
        }
        
        assertEquals( 0, expectedIds.size() );
    }

    
    /**
     * Test method for put( EntryAttribute... )
     */
    @Test
    public void testPutEntryAttributeArray() throws NamingException
    {
        Entry entry = new DefaultClientEntry( EXAMPLE_DN );
        
        EntryAttribute attrOC = new DefaultClientAttribute( "objectClass", "top", "person" );
        EntryAttribute attrCN = new DefaultClientAttribute( "cn", "test1", "test2" );
        EntryAttribute attrSN = new DefaultClientAttribute( "sn", "Test1", "Test2" );
        EntryAttribute attrPWD = new DefaultClientAttribute( "userPassword", BYTES1, BYTES2 );
        
        List<EntryAttribute> removed = entry.put( attrOC, attrCN, attrSN, attrPWD );
        
        assertEquals( 4, entry.size() );
        assertEquals( 0, removed.size() );
        assertTrue( entry.containsAttribute( "ObjectClass" ) );
        assertTrue( entry.containsAttribute( "CN" ) );
        assertTrue( entry.containsAttribute( "  sn  " ) );
        assertTrue( entry.containsAttribute( "userPassword" ) );
        
        EntryAttribute attrCN2 = new DefaultClientAttribute( "cn", "test3", "test4" );
        removed = entry.put( attrCN2 );
        assertEquals( 4, entry.size() );
        assertEquals( 1, removed.size() );
        assertTrue( entry.containsAttribute( "CN" ) );
        assertTrue( entry.contains( "cn", "test3", "test4" ) );
    }


    /**
     * Test method for put( String, byte[]... )
     */
    @Test
    public void testPutStringByteArrayArray()
    {
        Entry entry = new DefaultClientEntry( EXAMPLE_DN );
        
        try
        {
            entry.put( (String)null, BYTES1 );
            fail();
        }
        catch ( IllegalArgumentException iae)
        {
            assertTrue( true );
        }
        
        try
        {
            entry.put( "   ", BYTES1 );
            fail();
        }
        catch ( IllegalArgumentException iae)
        {
            assertTrue( true );
        }
        
        entry.put( "userPassword", (byte[])null );
        assertEquals( 1, entry.size() );
        assertNotNull( entry.get( "userPassword" ) );
        assertEquals( 1, entry.get( "userPassword" ).size() );
        assertNull( entry.get( "userPassword" ).get().get() );
        
        entry.put(  "jpegPhoto", BYTES1, BYTES2, BYTES1 );
        assertEquals( 2, entry.size() );
        assertNotNull( entry.get( "jpegPhoto" ) );
        assertEquals( 2, entry.get( "JPEGPhoto" ).size() );
        EntryAttribute attribute = entry.get( "jpegPhoto" );
        assertTrue( attribute.contains( BYTES1 ) );
        assertTrue( attribute.contains( BYTES2 ) );
        assertEquals( "jpegphoto", attribute.getId() );
        assertEquals( "jpegPhoto", attribute.getUpId() );
    }


    /**
     * Test method for put( String, String... )
     */
    @Test
    public void testPutStringStringArray()
    {
        Entry entry = new DefaultClientEntry( EXAMPLE_DN );
        
        try
        {
            entry.put( (String)null, "a" );
            fail();
        }
        catch ( IllegalArgumentException iae)
        {
            assertTrue( true );
        }
        
        try
        {
            entry.put( "   ", "a" );
            fail();
        }
        catch ( IllegalArgumentException iae)
        {
            assertTrue( true );
        }
        
        entry.put( "sn", (String)null );
        assertEquals( 1, entry.size() );
        assertNotNull( "sn", entry.get( "sn" ) );
        assertEquals( 1, entry.get( "sn" ).size() );
        assertNull( entry.get( "sn" ).get().get() );
        
        entry.put(  "ObjectClass", "top", "person", "top" );
        assertEquals( 2, entry.size() );
        assertNotNull( "objectclass", entry.get( "sn" ) );
        assertEquals( 2, entry.get( "OBJECTCLASS" ).size() );
        EntryAttribute attribute = entry.get( "objectClass" );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "person" ) );
        assertEquals( "objectclass", attribute.getId() );
        assertEquals( "ObjectClass", attribute.getUpId() );
    }


    /**
     * Test method for pu( String, Value<?>... )
     */
    @Test
    public void testPutStringValueArray()
    {
        Entry entry = new DefaultClientEntry( EXAMPLE_DN );
        
        Value<String> strValueTop = new ClientStringValue( "top" );
        Value<String> strValuePerson = new ClientStringValue( "person" );
        Value<String> strValueTop2 = new ClientStringValue( "top" );
        Value<String> strNullValue = new ClientStringValue( null);

        Value<byte[]> binValue1 = new ClientBinaryValue( BYTES1 );
        Value<byte[]> binValue2 = new ClientBinaryValue( BYTES2 );
        Value<byte[]> binValue3 = new ClientBinaryValue( BYTES1 );
        Value<byte[]> binNullValue = new ClientBinaryValue( null );

        try
        {
            entry.put( (String)null, strValueTop );
            fail();
        }
        catch ( IllegalArgumentException iae)
        {
            assertTrue( true );
        }
        
        try
        {
            entry.put( "   ", strValueTop );
            fail();
        }
        catch ( IllegalArgumentException iae)
        {
            assertTrue( true );
        }
        
        entry.put( "sn", strNullValue );
        assertEquals( 1, entry.size() );
        assertNotNull( "sn", entry.get( "sn" ) );
        assertEquals( 1, entry.get( "sn" ).size() );
        assertNull( entry.get( "sn" ).get().get() );
        
        entry.clear();
        
        entry.put(  "ObjectClass", strValueTop, strValuePerson, strValueTop2, strNullValue );
        assertEquals( 1, entry.size() );
        assertNotNull( "objectclass", entry.get( "objectclass" ) );
        assertEquals( 3, entry.get( "OBJECTCLASS" ).size() );
        EntryAttribute attribute = entry.get( "objectClass" );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "person" ) );
        assertTrue( attribute.contains( (String)null ) );
        assertEquals( "objectclass", attribute.getId() );
        assertEquals( "ObjectClass", attribute.getUpId() );

        entry.clear();
        
        entry.put( "userpassword", strNullValue );
        assertEquals( 1, entry.size() );
        assertNotNull( "userpassword", entry.get( "userpassword" ) );
        assertEquals( 1, entry.get( "userpassword" ).size() );
        assertNull( entry.get( "userpassword" ).get().get() );
        
        entry.clear();
        
        entry.put(  "userPassword", binValue1, binValue2, binValue3, binNullValue );
        assertEquals( 1, entry.size() );
        assertNotNull( "userpassword", entry.get( "userpassword" ) );
        assertEquals( 3, entry.get( "userpassword" ).size() );
        attribute = entry.get( "userpassword" );
        assertTrue( attribute.contains( BYTES1 ) );
        assertTrue( attribute.contains( BYTES2 ) );
        assertTrue( attribute.contains( (byte[])null ) );
        assertEquals( "userpassword", attribute.getId() );
        assertEquals( "userPassword", attribute.getUpId() );
    }


    /**
     * Test method for removeAttributes( String... )
     */
    @Test
    public void testRemoveAttributesStringArray() throws NamingException
    {
        Entry entry = new DefaultClientEntry( EXAMPLE_DN );

        EntryAttribute attrOC = new DefaultClientAttribute( "objectClass", "top", "person" );
        EntryAttribute attrCN = new DefaultClientAttribute( "cn", "test1", "test2" );
        EntryAttribute attrSN = new DefaultClientAttribute( "sn", "Test1", "Test2" );
        EntryAttribute attrPWD = new DefaultClientAttribute( "userPassword", BYTES1, BYTES2 );
        
        entry.put( attrOC, attrCN, attrSN, attrPWD );
        
        entry.removeAttributes( "CN", "SN" );
        
        assertFalse( entry.containsAttribute( "cn", "sn" ) );
        assertTrue( entry.containsAttribute( "objectclass", "userpassword" ) );
        
        List<EntryAttribute> removed = entry.removeAttributes( "badId" );
        assertNull( removed );
        
        removed = entry.removeAttributes( (String )null );
        assertNull( removed );
    }
    
    
    /**
     * Test method for remove( EntryAttribute... )
     */
    @Test
    public void testRemoveEntryAttributeArray() throws NamingException
    {
        Entry entry = new DefaultClientEntry( EXAMPLE_DN );
        
        EntryAttribute attrOC = new DefaultClientAttribute( "objectClass", "top", "person" );
        EntryAttribute attrCN = new DefaultClientAttribute( "cn", "test1", "test2" );
        EntryAttribute attrSN = new DefaultClientAttribute( "sn", "Test1", "Test2" );
        EntryAttribute attrPWD = new DefaultClientAttribute( "userPassword", BYTES1, BYTES2 );
        
        entry.put( attrOC, attrCN, attrSN, attrPWD );
        
        List<EntryAttribute> removed = entry.remove( attrSN, attrPWD );
        
        assertEquals( 2, removed.size() ); 
        assertEquals( 2, entry.size() );
        assertTrue( removed.contains( attrSN ) );
        assertTrue( removed.contains( attrPWD ) );
        assertTrue( entry.contains( "objectClass", "top", "person" ) );
        assertTrue( entry.contains( "cn", "test1", "test2" ) );
        assertFalse( entry.containsAttribute( "sn" ) );
        assertFalse( entry.containsAttribute( "userPassword" ) );

        removed = entry.remove( attrSN, attrPWD );
        
        assertEquals( 0, removed.size() );
    }


    /**
     * Test method for remove(String, byte[]... )
     */
    @Test
    public void testRemoveStringByteArrayArray() throws NamingException
    {
        Entry entry = new DefaultClientEntry( EXAMPLE_DN );
        
        EntryAttribute attrPWD = new DefaultClientAttribute( "userPassword", BYTES1, (byte[])null, BYTES2 );

        entry.put( attrPWD );
        assertTrue( entry.remove( "userPassword", (byte[])null ) );
        assertTrue( entry.remove( "userPassword", BYTES1, BYTES2 ) );
        assertFalse( entry.containsAttribute( "userPassword" ) );
        
        entry.add( "userPassword", BYTES1, (byte[])null, BYTES2 );
        assertTrue( entry.remove( "userPassword", (byte[])null ) );
        assertEquals( 2, entry.get( "userPassword" ).size() );
        assertTrue( entry.remove( "userPassword", BYTES1, BYTES3 ) );
        assertEquals( 1, entry.get( "userPassword" ).size() );
        assertTrue( Arrays.equals( BYTES2, entry.get( "userPassword" ).getBytes() ) );
        
        assertFalse( entry.remove( "userPassword", BYTES3 ) );
        assertFalse( entry.remove( "void", "whatever" ) );
    }


    /**
     * Test method for remove( String, String... )
     */
    @Test
    public void testRemoveStringStringArray() throws NamingException
    {
        Entry entry = createEntry();
        
        assertTrue( entry.remove( "cn", "test1" ) );
        assertTrue( entry.remove( "cn", "test2" ) );
        assertFalse( entry.containsAttribute( "cn" ) );
        
        entry.add( "cn", "test1", (String)null, "test2" );
        assertTrue( entry.remove( "cn", (String)null ) );
        assertEquals( 2, entry.get( "cn" ).size() );
        assertTrue( entry.remove( "cn", "test1", "test3" ) );
        assertEquals( 1, entry.get( "cn" ).size() );
        assertEquals( "test2", entry.get( "cn" ).get().get() );
        
        assertFalse( entry.remove( "cn", "test3" ) );
        assertFalse( entry.remove( "void", "whatever" ) );
    }


    /**
     * Test method for remove(String, Value<?>... )
     */
    @Test
    public void testRemoveStringValueArray() throws NamingException
    {
        Entry entry = new DefaultClientEntry( EXAMPLE_DN );

        EntryAttribute attrCN = new DefaultClientAttribute( "cn", "test1", "test2", (String)null );
        EntryAttribute attrPWD = new DefaultClientAttribute( "userPassword", BYTES1, BYTES2, (byte[])null );

        entry.add( attrCN, attrPWD );
        
        Value<String> strValue1 = new ClientStringValue( "test1" );
        Value<String> strValue2 = new ClientStringValue( "test2" );
        Value<String> strValue3 = new ClientStringValue( "test3" );
        Value<String> strNullValue = new ClientStringValue( null);

        Value<byte[]> binValue1 = new ClientBinaryValue( BYTES1 );
        Value<byte[]> binValue2 = new ClientBinaryValue( BYTES2 );
        Value<byte[]> binValue3 = new ClientBinaryValue( BYTES3 );
        Value<byte[]> binNullValue = new ClientBinaryValue( null );
        
        assertTrue( entry.remove( "cn", strValue1, strNullValue ) );
        assertTrue( entry.contains( "cn", strValue2 ) );
        assertFalse( entry.remove( "cn", strValue3 ) );
        assertTrue( entry.remove( "cn", strValue2 ) );
        assertFalse( entry.containsAttribute( "cn" ) );

        entry.add( attrCN, attrPWD );

        assertTrue( entry.remove( "userpassword", binValue1, binNullValue ) );
        assertTrue( entry.contains( "userpassword", binValue2 ) );
        assertFalse( entry.remove( "userpassword", binValue3 ) );
        assertTrue( entry.remove( "userpassword", binValue2 ) );
        assertFalse( entry.containsAttribute( "userpassword" ) );
    }
    
    
    /**
     * Test method for set( String... )
     */
    @Test
    public void testSet() throws NamingException
    {
        Entry entry = new DefaultClientEntry( EXAMPLE_DN );

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
     * Test method for setDN( LdapDN )
     */
    @Test
    public void testSetDn()
    {
        Entry entry = new DefaultClientEntry();
        
        assertEquals( LdapDN.EMPTY_LDAPDN, entry.getDn() );
        
        entry.setDn( EXAMPLE_DN );
        assertEquals( EXAMPLE_DN, entry.getDn() );
    }
    
    
    /**
     * Test method for size()
     */
    @Test
    public void testSize() throws NamingException
    {
        Entry entry = new DefaultClientEntry( EXAMPLE_DN );
        
        assertEquals( 0, entry.size() );
        entry.add( "ObjectClass", "top", "person" );
        entry.add( "cn", "test" );
        entry.add( "sn", "Test" );
        
        assertEquals( 3, entry.size() );
       
        entry.clear();
        assertEquals( 0, entry.size() );
    }

    
    /**
     * Test method for for {@link org.apache.directory.shared.ldap.entry.client.DefaultClientEntry#toString()}.
     */
    @Test
    public void testToString()
    {
        Entry entry = new DefaultClientEntry( EXAMPLE_DN );
        
        assertEquals( "ClientEntry\n    dn: dc=example,dc=com\n", entry.toString() );
        
        Value<String> strValueTop = new ClientStringValue( "top" );
        Value<String> strValuePerson = new ClientStringValue( "person" );
        Value<String> strNullValue = new ClientStringValue( null);

        Value<byte[]> binValue1 = new ClientBinaryValue( BYTES1 );
        Value<byte[]> binValue2 = new ClientBinaryValue( BYTES2 );
        Value<byte[]> binNullValue = new ClientBinaryValue( null );
        
        entry.put( "ObjectClass", strValueTop, strValuePerson, strNullValue );
        entry.put( "UserPassword", binValue1, binValue2, binNullValue );

        String expected = 
            "ClientEntry\n" +
            "    dn: dc=example,dc=com\n" +
            "    ObjectClass: top\n" +
            "    ObjectClass: person\n" +
            "    ObjectClass: ''\n" +
            "    UserPassword: '0x61 0x62 '\n" +
            "    UserPassword: '0x62 '\n" +
            "    UserPassword: ''\n";

        assertEquals( expected, entry.toString() );
    }
    
    
    /**
     * Test the serialization of a complete entry
     */
    @Test
    public void testSerializeCompleteEntry() throws NamingException, IOException, ClassNotFoundException
    {
        LdapDN dn = new LdapDN( "ou=system" );
        
        dn.normalize( oids );
        
        byte[] password = StringTools.getBytesUtf8( "secret" );
        ClientEntry entry = new DefaultClientEntry( dn);
        entry.add( "ObjectClass", "top", "person" );
        entry.add( "cn", "test1" );
        entry.add( "userPassword", password );

        ClientEntry entrySer = deserializeValue( serializeValue( entry ) );
        
        assertEquals( entry, entrySer );
    }
    
    
    /**
     * Test the serialization of an entry with no DN
     */
    @Test
    public void testSerializeEntryWithNoDN() throws NamingException, IOException, ClassNotFoundException
    {
        byte[] password = StringTools.getBytesUtf8( "secret" );
        ClientEntry entry = new DefaultClientEntry();
        entry.add( "ObjectClass", "top", "person" );
        entry.add( "cn", "test1" );
        entry.add( "userPassword", password );

        ClientEntry entrySer = deserializeValue( serializeValue( entry ) );
        
        assertEquals( entry, entrySer );
    }
    
    
    /**
     * Test the serialization of an entry with no attribute and no DN
     */
    @Test
    public void testSerializeEntryWithNoDNNoAttribute() throws NamingException, IOException, ClassNotFoundException
    {
        ClientEntry entry = new DefaultClientEntry();

        ClientEntry entrySer = deserializeValue( serializeValue( entry ) );
        
        assertEquals( entry, entrySer );
    }
    
    
    /**
     * Test the serialization of an entry with no attribute
     */
    @Test
    public void testSerializeEntryWithNoAttribute() throws NamingException, IOException, ClassNotFoundException
    {
        LdapDN dn = new LdapDN( "ou=system" );
        
        dn.normalize( oids );
        
        ClientEntry entry = new DefaultClientEntry( dn );

        ClientEntry entrySer = deserializeValue( serializeValue( entry ) );
        
        assertEquals( entry, entrySer );
    }
    
    
}
