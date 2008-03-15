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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;

import org.apache.directory.server.schema.bootstrap.ApacheSchema;
import org.apache.directory.server.schema.bootstrap.ApachemetaSchema;
import org.apache.directory.server.schema.bootstrap.BootstrapSchemaLoader;
import org.apache.directory.server.schema.bootstrap.CoreSchema;
import org.apache.directory.server.schema.bootstrap.CosineSchema;
import org.apache.directory.server.schema.bootstrap.InetorgpersonSchema;
import org.apache.directory.server.schema.bootstrap.Schema;
import org.apache.directory.server.schema.bootstrap.SystemSchema;
import org.apache.directory.server.schema.registries.DefaultOidRegistry;
import org.apache.directory.server.schema.registries.DefaultRegistries;
import org.apache.directory.server.schema.registries.OidRegistry;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;


/**
 * Test the DefaultServerEntry class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DefaultServerEntryTest
{
    private static BootstrapSchemaLoader loader;
    private static Registries registries;
    private static OidRegistry oidRegistry;
    
    /**
     * Initialize the registries once for the whole test suite
     */
    @BeforeClass
    public static void setup() throws NamingException
    {
        loader = new BootstrapSchemaLoader();
        oidRegistry = new DefaultOidRegistry();
        registries = new DefaultRegistries( "bootstrap", loader, oidRegistry );
        
        // load essential bootstrap schemas 
        Set<Schema> bootstrapSchemas = new HashSet<Schema>();
        bootstrapSchemas.add( new ApachemetaSchema() );
        bootstrapSchemas.add( new ApacheSchema() );
        bootstrapSchemas.add( new CoreSchema() );
        bootstrapSchemas.add( new SystemSchema() );
        bootstrapSchemas.add( new InetorgpersonSchema() );
        bootstrapSchemas.add( new CosineSchema() );
        loader.loadWithDependencies( bootstrapSchemas, registries );
    }


    /**
     * Test a conversion from a ServerEntry to an AttributesImpl
     */
    @Test public void testToAttributesImpl() throws InvalidNameException, NamingException
    {
        LdapDN dn = new LdapDN( "cn=test" );
        DefaultServerEntry entry = new DefaultServerEntry( registries, dn );
        
        AttributeType OBJECT_CLASS_AT = registries.getAttributeTypeRegistry().lookup( SchemaConstants.OBJECT_CLASS_AT );
        
        entry.put( "objectClass", OBJECT_CLASS_AT, "top", "person", "inetOrgPerson", "organizationalPerson" );
        entry.put( "cn", registries.getAttributeTypeRegistry().lookup( "cn" ), "test" );
        
        Attributes attributes = ServerEntryUtils.toAttributesImpl( entry );
        
        assertNotNull( attributes );
        assertTrue( attributes instanceof AttributesImpl );
        
        Set<String> expected = new HashSet<String>();
        expected.add( "objectClass" );
        expected.add( "cn" );
     
        for ( NamingEnumeration<String> ids = attributes.getIDs(); ids.hasMoreElements();)
        {
            String id = ids.nextElement();
            
            assertTrue( expected.contains( id ) );
            expected.remove( id );
            
        }

        // It should be empty
        assertEquals( 0, expected.size() );
    }


    /**
     * Test a conversion from a ServerEntry to an BasicAttributes
     */
    @Test public void testToBasicAttributes() throws InvalidNameException, NamingException
    {
        LdapDN dn = new LdapDN( "cn=test" );
        DefaultServerEntry entry = new DefaultServerEntry( registries,dn );
        
        AttributeType OBJECT_CLASS_AT = registries.getAttributeTypeRegistry().lookup( SchemaConstants.OBJECT_CLASS_AT );
        
        entry.put( "objectClass", OBJECT_CLASS_AT, "top", "person", "inetOrgPerson", "organizationalPerson" );
        
        Attributes attributes = ServerEntryUtils.toBasicAttributes( entry );
        
        assertNotNull( attributes );
        assertTrue( attributes instanceof BasicAttributes );
        
        Set<String> expected = new HashSet<String>();
        expected.add( "objectClass" );
        expected.add( "cn" );
     
        for ( NamingEnumeration<String> ids = attributes.getIDs(); ids.hasMoreElements();)
        {
            String id = ids.nextElement();
            
            assertTrue( expected.contains( id ) );
            expected.remove( id );
            
        }

        // We should still have the ObjectClass Attribute
        assertEquals( 1, expected.size() );
    }
    
    //-------------------------------------------------------------------------
    // Test the put methods
    //-------------------------------------------------------------------------
    /**
     * Test the set(AT...) method
     */
    @Test public void testSetATElipsis() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test" );
        DefaultServerEntry entry = new DefaultServerEntry( registries, dn );
        
        List<ServerAttribute> result = null;
        
        // First check that this method fails if we pass an empty list of ATs
        try
        {
            result = entry.set( (AttributeType)null);
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }
        
        // Now, check what we get when adding one existing AT
        AttributeType atSN = registries.getAttributeTypeRegistry().lookup( "sn" );
        result = entry.set( atSN );
        
        assertNotNull( result );
        assertEquals( 0, result.size() );
        ServerAttribute sa = entry.get( "sn" );
        assertNotNull( sa );
        assertEquals( atSN, sa.getAttributeType() );
        assertEquals( "sn", sa.getAttributeType().getName() );
        
        // Add two AT now
        AttributeType atL = registries.getAttributeTypeRegistry().lookup( "localityName" );
        AttributeType atC = registries.getAttributeTypeRegistry().lookup( "countryName" );
        AttributeType atGN = registries.getAttributeTypeRegistry().lookup( "givenname" );
        AttributeType atStreet = registries.getAttributeTypeRegistry().lookup( "2.5.4.9" );
        result = entry.set( atL, atC, atGN, atStreet );
        
        assertNotNull( result );
        
        assertEquals( 0, result.size() );
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
        assertEquals( "test", entry.get( "sn" ).get().get() ); 
        
        // Now add a new SN empty AT : it should replace the existing one.
        AttributeType atSNEmpty = registries.getAttributeTypeRegistry().lookup( "sn" );
        sa = entry.set( atSNEmpty ).get( 0 );
        assertEquals( "test", sa.get().get() ); 
        assertNotNull( entry.get(  "sn" ) );
        assertNull( entry.get(  "sn" ).get() );
        
        // Last, not least, put an ObjectClass AT
        AttributeType OBJECT_CLASS_AT = registries.getAttributeTypeRegistry().lookup( SchemaConstants.OBJECT_CLASS_AT );
        
        entry.set( OBJECT_CLASS_AT );
        
        assertNotNull( entry.get( "objectClass" ) );

        ServerAttribute oc = entry.get( "objectClass" );
        
        assertEquals( OBJECT_CLASS_AT, oc.getAttributeType() );
        assertNull( oc.get() );
    }

    /**
     * Test the set( upId ) method
     */
    @Test public void tesSetUpID() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test" );
        DefaultServerEntry entry = new DefaultServerEntry( registries, dn );
        
        // First check that this method fails if we pass a null or empty ID
        try
        {
            entry.set( (String)null );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }
        
        try
        {
            entry.set( "  " );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }
        
        // Now check that we can't put invalid IDs
        try
        {
            entry.set( "ThisIsNotAnAttributeType" );
            fail();
        }
        catch ( NamingException ne )
        {
            assertTrue( true );
        }
        
        // Now, check what we get when adding one existing AT
        List<ServerAttribute> result = entry.set( "sn" );
        
        assertNull( result );

        ServerAttribute sa = entry.get( "sn" );
        assertNotNull( sa );
        assertEquals( "sn", sa.getAttributeType().getName() );
        
        // Add different upIds now
        AttributeType atL = registries.getAttributeTypeRegistry().lookup( "localityName" );
        AttributeType atC = registries.getAttributeTypeRegistry().lookup( "countryName" );
        AttributeType atGN = registries.getAttributeTypeRegistry().lookup( "givenname" );
        AttributeType atStreet = registries.getAttributeTypeRegistry().lookup( "2.5.4.9" );
        
        entry.set( "L" );
        entry.set( "CountryName" );
        entry.set( "gn" );
        entry.set( "2.5.4.9" );
        

        sa = entry.get( "l" );
        assertNotNull( sa );
        assertEquals( atL, sa.getAttributeType() );
        assertEquals( "l", sa.getAttributeType().getName() );
        assertEquals( "L", sa.getUpId() );

        sa = entry.get( "c" );
        assertNotNull( sa );
        assertEquals( atC, sa.getAttributeType() );
        assertEquals( "c", sa.getAttributeType().getName() );
        assertEquals( "CountryName", sa.getUpId() );

        sa = entry.get( "2.5.4.9" );
        assertNotNull( sa );
        assertEquals( atStreet, sa.getAttributeType() );
        assertEquals( "street", sa.getAttributeType().getName() );
        assertEquals( "2.5.4.9", sa.getUpId() );

        sa = entry.get( "givenName" );
        assertNotNull( sa );
        assertEquals( atGN, sa.getAttributeType() );
        assertEquals( "givenName", sa.getAttributeType().getName() );
        assertEquals( "gn", sa.getUpId() );
        
        // Now try to add existing ATs
        // First, set some value to the modified AT
        sa = entry.get( "sn" );
        sa.add( "test" );
        
        // Check that the value has been added to the entry
        assertEquals( "test", entry.get( "sn" ).get().get() ); 
        
        // Now add a new SN empty AT : it should replace the existing one.
        AttributeType atSNEmpty = registries.getAttributeTypeRegistry().lookup( "sn" );
        sa = entry.set( atSNEmpty ).get( 0 );
        assertEquals( "test", sa.get().get() ); 
        assertNotNull( entry.get(  "sn" ) );
        assertNull( entry.get(  "sn" ).get() );
    }

    
    /**
     * Test the put( SA... ) method
     */
    @Test public void tesPutServerAttributeElipsis() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test" );
        DefaultServerEntry entry = new DefaultServerEntry( registries, dn );

        // first test a null SA addition. It should be allowed.
        try
        {
            entry.put( (ServerAttribute)null );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }
        
        // Ajout de serverAttributes
        AttributeType atL = registries.getAttributeTypeRegistry().lookup( "localityName" );
        AttributeType atC = registries.getAttributeTypeRegistry().lookup( "countryName" );
        AttributeType atGN = registries.getAttributeTypeRegistry().lookup( "givenname" );
        AttributeType atStreet = registries.getAttributeTypeRegistry().lookup( "2.5.4.9" );

        ServerAttribute sa = new DefaultServerAttribute( atL, "france" );
        entry.put( sa );
        
        assertEquals( 1, entry.size() );
        assertNotNull( entry.get( "l" ) );
        assertEquals( "france", entry.get( "l" ).get().get() );
        
        ServerAttribute sb = new DefaultServerAttribute( atC, "countryTest" );
        ServerAttribute sc = new DefaultServerAttribute( atGN, "test" );
        ServerAttribute sd = new DefaultServerAttribute( atStreet, "testStreet" );
        entry.put( sb, sc, sd );

        assertEquals( 4, entry.size() );
        assertNotNull( entry.get( atC ) );
        assertEquals( "countryTest", entry.get( atC ).get().get() );
        assertNotNull( entry.get( atGN ) );
        assertEquals( "test", entry.get( atGN ).get().get() );
        assertNotNull( entry.get( atStreet) );
        assertEquals( "testStreet", entry.get( atStreet ).get().get() );
        
        // Test a replacement
        ServerAttribute sbb = new DefaultServerAttribute( atC, "countryTestTest" );
        ServerAttribute scc = new DefaultServerAttribute( atGN, "testtest" );
        List<ServerAttribute> result = entry.put( sbb, scc );
        
        assertEquals( 2, result.size() );
        assertEquals( "countryTest", result.get(0).get().get() );
        assertEquals( "test", result.get(1).get().get() );
        assertEquals( 4, entry.size() );
        assertNotNull( entry.get( atC ) );
        assertEquals( "countryTestTest", entry.get( atC ).get().get() );
        assertNotNull( entry.get( atGN ) );
        assertEquals( "testtest", entry.get( atGN ).get().get() );
        assertNotNull( entry.get( atStreet) );
        assertEquals( "testStreet", entry.get( atStreet ).get().get() );
        
        // test an ObjectClass replacement
        AttributeType OBJECT_CLASS_AT = registries.getAttributeTypeRegistry().lookup( SchemaConstants.OBJECT_CLASS_AT );
        ServerAttribute oc = new DefaultServerAttribute( "OBJECTCLASS", OBJECT_CLASS_AT, "person", "inetorgperson" );
        List<ServerAttribute> oldOc = entry.put( oc );
        
        assertNotNull( oldOc );
        assertEquals( 0, oldOc.size() );
        
        assertNotNull( entry.get( "objectClass" ) );

        ServerAttribute newOc = entry.get( "objectClass" );
        
        assertNotNull( newOc );
        assertEquals( OBJECT_CLASS_AT, newOc.getAttributeType() );
        assertEquals( 2, newOc.size() );
        assertEquals( "OBJECTCLASS", newOc.getUpId() );
        assertTrue( newOc.contains( "person", "inetOrgPerson" ) );
    }

    
    /**
     * Test the put( AT, String... ) method
     */
    @Test public void tesPutAtStringElipsis() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test" );
        DefaultServerEntry entry = new DefaultServerEntry( registries, dn );
        
        AttributeType atCN = registries.getAttributeTypeRegistry().lookup( "cn" );

        // Test an empty AT
        entry.put( atCN, (String)null );
        assertEquals( 1, entry.size() );
        assertEquals( "cn", entry.get( atCN ).getUpId() );
        assertNull( entry.get( atCN ).get().get() );
        
        // Check that we can't use invalid arguments
        try
        {
            entry.put( (AttributeType)null, (String)null );
            fail();
        }
        catch( IllegalArgumentException iae )
        {
            assertTrue( true );
        }
        
        // Add a single value
        atCN = registries.getAttributeTypeRegistry().lookup( "cn" );
        entry.put( atCN, "test" );
        
        assertEquals( 1, entry.size() );
        assertEquals( "cn", entry.get( atCN ).getUpId() );
        assertEquals( 1, entry.get( atCN ).size() );
        assertEquals( "test", entry.get( atCN ).get().get() );
        
        // Add more than one value
        entry.put( atCN, "test1", "test2", "test3" );
        
        assertEquals( 1, entry.size() );
        assertEquals( "cn", entry.get( atCN ).getUpId() );
        assertEquals( 3, entry.get( atCN ).size() );
        assertTrue( entry.contains( "cn", "test1" ) );
        assertTrue( entry.contains( "cn", "test2" ) );
        assertTrue( entry.contains( "cn", "test3" ) );
        
        // Add twice the same value
        ServerAttribute sa = entry.put( atCN, "test1", "test2", "test1" );
        
        assertEquals( 3, sa.size() );
        assertTrue( sa.contains( "test1", "test2", "test3" ) );
        assertEquals( 1, entry.size() );
        assertEquals( "cn", entry.get( atCN ).getUpId() );
        assertEquals( 2, entry.get( atCN ).size() );
        assertTrue( entry.contains( "cn", "test1" ) );
        assertTrue( entry.contains( "cn", "test2" ) );
    }
    

    /**
     * Test the put( AT, Byte[]... ) method
     */
    @Test public void tesPutAtByteElipsis() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test" );
        DefaultServerEntry entry = new DefaultServerEntry( registries, dn );
        
        AttributeType atPwd = registries.getAttributeTypeRegistry().lookup( "userPassword" );

        // Test an empty AT
        entry.put( atPwd, (byte[])null );
        assertEquals( 1, entry.size() );
        assertEquals( "userPassword", entry.get( atPwd ).getUpId() );
        assertNull( entry.get( atPwd ).get().get() );
        
        // Check that we can't use invalid arguments
        try
        {
            entry.put( (AttributeType)null, (byte[])null );
            fail();
        }
        catch( IllegalArgumentException iae )
        {
            assertTrue( true );
        }
        
        byte[] password = StringTools.getBytesUtf8( "test" );
        byte[] test1 = StringTools.getBytesUtf8( "test1" );
        byte[] test2 = StringTools.getBytesUtf8( "test2" );
        byte[] test3 = StringTools.getBytesUtf8( "test3" );
        
        // Add a single value
        atPwd = registries.getAttributeTypeRegistry().lookup( "userPassword" );
        entry.put( atPwd, password );
        
        assertEquals( 1, entry.size() );
        assertEquals( "userPassword", entry.get( atPwd ).getUpId() );
        assertEquals( 1, entry.get( atPwd ).size() );
        assertTrue( Arrays.equals( password, (byte[])entry.get( atPwd ).get().get() ) );
        
        // Add more than one value
        entry.put( atPwd, test1, test2, test3 );
        
        assertEquals( 1, entry.size() );
        assertEquals( "userPassword", entry.get( atPwd ).getUpId() );
        assertEquals( 3, entry.get( atPwd ).size() );
        assertTrue( entry.contains( "userpassword", test1 ) );
        assertTrue( entry.contains( "userpassword", test2 ) );
        assertTrue( entry.contains( "userpassword", test3 ) );
        
        // Add twice the same value
        ServerAttribute sa = entry.put( atPwd, test1, test2, test1 );
        
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
    @Test public void tesPutAtSVs() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test" );
        DefaultServerEntry entry = new DefaultServerEntry( registries, dn );
        
        // Adding a null value to an attribute
        AttributeType atCN = registries.getAttributeTypeRegistry().lookup( "cn" );
        entry.put( atCN, (Value<?>)null );
        
        assertEquals( 1, entry.size() );
        assertEquals( "cn", entry.get( atCN ).getUpId() );
        
        // Check that we can't use invalid arguments
        try
        {
            entry.put( (AttributeType)null, (Value<?>)null );
            fail();
        }
        catch( IllegalArgumentException iae )
        {
            assertTrue( true );
        }
        
        // Add a single value
        atCN = registries.getAttributeTypeRegistry().lookup( "cn" );
        Value<?> ssv = new ServerStringValue( atCN, "test" );
        entry.put( atCN, ssv );
        
        assertEquals( 1, entry.size() );
        assertEquals( "cn", entry.get( atCN ).getUpId() );
        assertEquals( 1, entry.get( atCN ).size() );
        assertEquals( "test", entry.get( atCN ).get().get() );
        
        // Add more than one value
        entry.put( atCN, new ServerStringValue( atCN, "test1" ),
                         new ServerStringValue( atCN, "test2" ), 
                         new ServerStringValue( atCN, "test3" ));
        
        assertEquals( 1, entry.size() );
        assertEquals( "cn", entry.get( atCN ).getUpId() );
        assertEquals( 3, entry.get( atCN ).size() );
        assertTrue( entry.contains( "cn", "test1" ) );
        assertTrue( entry.contains( "cn", "test2" ) );
        assertTrue( entry.contains( "cn", "test3" ) );
        
        // Add twice the same value
        ServerAttribute sa = entry.put( atCN, new ServerStringValue( atCN, "test1" ),
                         new ServerStringValue( atCN, "test2" ), 
                         new ServerStringValue( atCN, "test1" ));
        
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
    @Test public void tesPutUpIdStringElipsis() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test" );
        DefaultServerEntry entry = new DefaultServerEntry( registries, dn );
        
        AttributeType atCN = registries.getAttributeTypeRegistry().lookup( "cn" );
        
        // Adding a null value should be possible
        entry.put( "cn", (String)null );
        
        assertEquals( 1, entry.size() );
        assertEquals( "cn", entry.get( atCN ).getUpId() );
        assertNull( entry.get( atCN ).get().get() );
        
        // Check that we can't use invalid arguments
        try
        {
            entry.put( (String)null, (String)null );
            fail();
        }
        catch( IllegalArgumentException iae )
        {
            assertTrue( true );
        }
        
        // Add a single value
        atCN = registries.getAttributeTypeRegistry().lookup( "cn" );
        entry.put( "cn", "test" );
        
        assertEquals( 1, entry.size() );
        assertEquals( "cn", entry.get( atCN ).getUpId() );
        assertEquals( 1, entry.get( atCN ).size() );
        assertEquals( "test", entry.get( atCN ).get().get() );
        
        // Add more than one value
        entry.put( "cn", "test1", "test2", "test3" );
        
        assertEquals( 1, entry.size() );
        assertEquals( "cn", entry.get( atCN ).getUpId() );
        assertEquals( 3, entry.get( atCN ).size() );
        assertTrue( entry.contains( "cn", "test1" ) );
        assertTrue( entry.contains( "cn", "test2" ) );
        assertTrue( entry.contains( "cn", "test3" ) );
        
        // Add twice the same value
        ServerAttribute sa = entry.put( "cn", "test1", "test2", "test1" );
        
        assertEquals( 3, sa.size() );
        assertTrue( sa.contains( "test1", "test2", "test3" ) );
        assertEquals( 1, entry.size() );
        assertEquals( "cn", entry.get( atCN ).getUpId() );
        assertEquals( 2, entry.get( atCN ).size() );
        assertTrue( entry.contains( "cn", "test1" ) );
        assertTrue( entry.contains( "cn", "test2" ) );
        
        // Check the UpId
        entry.put( "CN", "test4" );
        assertEquals( "CN", entry.get( atCN ).getUpId() );
    }
    

    /**
     * Test the put( upId, byte[]... ) method
     */
    @Test public void tesPutUpIdBytesElipsis() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test" );
        DefaultServerEntry entry = new DefaultServerEntry( registries, dn );
        
        AttributeType atPassword = registries.getAttributeTypeRegistry().lookup( "userPassword" );
        
        // Adding a null value should be possible
        entry.put( "userPassword", (byte[])null );
        assertEquals( 1, entry.size() );
        assertEquals( "userPassword", entry.get( atPassword ).getUpId() );
        assertNull( entry.get( atPassword ).get().get() );
        
        // Check that we can't use invalid arguments
        try
        {
            entry.put( (String)null, (String)null );
            fail();
        }
        catch( IllegalArgumentException iae )
        {
            assertTrue( true );
        }
        
        // Add a single value
        byte[] test = StringTools.getBytesUtf8( "test" );
        byte[] test1 = StringTools.getBytesUtf8( "test1" );
        byte[] test2 = StringTools.getBytesUtf8( "test2" );
        byte[] test3 = StringTools.getBytesUtf8( "test3" );
        
        entry.put( "userPassword", test );
        
        assertEquals( 1, entry.size() );
        assertEquals( "userPassword", entry.get( atPassword ).getUpId() );
        assertEquals( 1, entry.get( atPassword ).size() );
        assertTrue( Arrays.equals( test, (byte[])entry.get( atPassword ).get().get() ) );
        
        // Add more than one value
        entry.put( "userPassword", test1, test2, test3 );
        
        assertEquals( 1, entry.size() );
        assertEquals( "userPassword", entry.get( atPassword ).getUpId() );
        assertEquals( 3, entry.get( atPassword ).size() );
        assertTrue( entry.contains( "userPassword", test1 ) );
        assertTrue( entry.contains( "userPassword", test2 ) );
        assertTrue( entry.contains( "userPassword", test3 ) );
        
        // Add twice the same value
        ServerAttribute sa = entry.put( "userPassword", test1, test2, test1 );
        
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
    @Test public void tesPutUpIDAtStringElipsis() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test" );
        DefaultServerEntry entry = new DefaultServerEntry( registries, dn );
        
        AttributeType atCN = registries.getAttributeTypeRegistry().lookup( "cn" );
        
        // Test that we get an error when the ID or AT are null
        try
        {
            entry.put( null, (AttributeType)null, (String)null );
            fail();
        }
        catch( IllegalArgumentException iae )
        {
            assertTrue( true );
        }
        
        // Test an empty AT
        entry.put( "commonName", atCN, (String)null );
        assertEquals( 1, entry.size() );
        assertEquals( "commonName", entry.get( atCN ).getUpId() );
        assertEquals( "cn", entry.get( atCN ).getAttributeType().getName() );
        assertNull( entry.get( atCN ).get().get() );
        
        // Check that we can use a null AttributeType
        entry.put( "commonName", (AttributeType)null, (String)null );
        assertEquals( 1, entry.size() );
        assertEquals( "commonName", entry.get( atCN ).getUpId() );
        assertEquals( "cn", entry.get( atCN ).getAttributeType().getName() );
        assertNull( entry.get( atCN ).get().get() );
        
        // Test that we can use a null upId
        entry.put( null, atCN, (String)null );
        assertEquals( 1, entry.size() );
        assertEquals( "cn", entry.get( atCN ).getUpId() );
        assertEquals( "cn", entry.get( atCN ).getAttributeType().getName() );
        assertNull( entry.get( atCN ).get().get() );
        
        // Test that if we use an upId which is not compatible
        // with the AT, it is changed to the AT default name
        entry.put( "sn", atCN, (String)null );
        assertEquals( "cn", entry.get( atCN ).getId() );
        
        // Test that we can add some new attributes with values
        ServerAttribute result = entry.put( "CN", atCN, "test1", "test2", "test3" );
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
     * Test the put( upId, AT, byte[]... ) method
     */
    @Test public void tesPutUpIDAtBytesElipsis() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test" );
        DefaultServerEntry entry = new DefaultServerEntry( registries, dn );
        
        AttributeType atPassword = registries.getAttributeTypeRegistry().lookup( "userPassword" );
        
        // Test that we get an error when the ID or AT are null
        try
        {
            entry.put( null, (AttributeType)null, (String)null );
            fail();
        }
        catch( IllegalArgumentException iae )
        {
            assertTrue( true );
        }
        
        // Test an empty AT
        entry.put( "userPassword", atPassword, (byte[])null );
        assertEquals( 1, entry.size() );
        assertEquals( "userPassword", entry.get( atPassword ).getUpId() );
        assertEquals( "userPassword", entry.get( atPassword ).getAttributeType().getName() );
        assertNull( entry.get( atPassword ).get().get() );
        
        // Check that we can use a null AttributeType
        try
        {
            entry.put( "userPassword", (AttributeType)null, (byte[])null );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }
        
        assertEquals( 1, entry.size() );
        assertEquals( "userPassword", entry.get( atPassword ).getUpId() );
        assertEquals( "userPassword", entry.get( atPassword ).getAttributeType().getName() );
        assertNull( entry.get( atPassword ).get().get() );
        
        // Test that we can use a null upId
        entry.put( null, atPassword, (byte[])null );
        assertEquals( 1, entry.size() );
        assertEquals( "userPassword", entry.get( atPassword ).getUpId() );
        assertEquals( "userPassword", entry.get( atPassword ).getAttributeType().getName() );
        assertNull( entry.get( atPassword ).get().get() );
        
        // Test that if we use an upId which is not compatible
        // with the AT, it is changed to the AT default name
        entry.put( "sn", atPassword, (byte[])null );
        assertEquals( "userpassword", entry.get( atPassword ).getId() );
        
        // Test that we can add some new attributes with values
        byte[] test1 = StringTools.getBytesUtf8( "test1" );
        byte[] test2 = StringTools.getBytesUtf8( "test2" );
        byte[] test3 = StringTools.getBytesUtf8( "test3" );

        ServerAttribute result = entry.put( "UserPassword", atPassword, test1, test2, test3 );
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
    @Test public void tesPutUpIDAtSVElipsis() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test" );
        DefaultServerEntry entry = new DefaultServerEntry( registries, dn );
        
        AttributeType atCN = registries.getAttributeTypeRegistry().lookup( "cn" );
        
        // Test that we get an error when the ID or AT are null
        try
        {
            entry.put( null, (AttributeType)null, (Value<?>)null );
            fail();
        }
        catch( IllegalArgumentException iae )
        {
            assertTrue( true );
        }
        
        // Test an empty AT
        entry.put( "commonName", atCN, (Value<?>)null );
        assertEquals( 1, entry.size() );
        assertEquals( "commonName", entry.get( atCN ).getUpId() );
        assertEquals( "cn", entry.get( atCN ).getAttributeType().getName() );
        assertNull( entry.get( atCN ).get().get() );
        
        // Check that we can use a null AttributeType
        entry.put( "commonName", (AttributeType)null, (Value<?>)null );
        assertEquals( 1, entry.size() );
        assertEquals( "commonName", entry.get( atCN ).getUpId() );
        assertEquals( "cn", entry.get( atCN ).getAttributeType().getName() );
        assertNull( entry.get( atCN ).get().get() );
        
        // Test that we can use a null upId
        entry.put( null, atCN, (Value<?>)null );
        assertEquals( 1, entry.size() );
        assertEquals( "cn", entry.get( atCN ).getUpId() );
        assertEquals( "cn", entry.get( atCN ).getAttributeType().getName() );
        assertNull( entry.get( atCN ).get().get() );
        
        // Test that we can't use an upId which is not compatible
        // with the AT
        try
        {
            entry.put( "sn", atCN, (Value<?>)null );
            fail();
        }
        catch( IllegalArgumentException iae )
        {
            assertTrue( true );
        }
        
        // Test that we can add some new attributes with values
        Value<String> test1 = new ServerStringValue( atCN, "test1" );
        Value<String> test2 = new ServerStringValue( atCN, "test2" );
        Value<String> test3 = new ServerStringValue( atCN, "test3" );

        ServerAttribute result = entry.put( "CN", atCN, test1, test2, test3 );
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
    @Test public void tesPutUpIDSVElipsis() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test" );
        DefaultServerEntry entry = new DefaultServerEntry( registries, dn );
        
        AttributeType atCN = registries.getAttributeTypeRegistry().lookup( "cn" );
        
        // Test that we get an error when the ID or AT are null
        try
        {
            entry.put( (String)null, (Value<?>)null );
            fail();
        }
        catch( IllegalArgumentException iae )
        {
            assertTrue( true );
        }
        
        // Test an null valued AT
        entry.put( "commonName", (Value<?>)null );
        assertEquals( 1, entry.size() );
        assertEquals( "commonName", entry.get( atCN ).getUpId() );
        assertEquals( "cn", entry.get( atCN ).getAttributeType().getName() );
        assertNull( entry.get( atCN ).get().get() );

        // Test that we can add some new attributes with values
        Value<String> test1 = new ServerStringValue( atCN, "test1" );
        Value<String> test2 = new ServerStringValue( atCN, "test2" );
        Value<String> test3 = new ServerStringValue( atCN, "test3" );

        ServerAttribute result = entry.put( "CN", test1, test2, test3 );
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
    // Test the Add methods
    //-------------------------------------------------------------------------

    /**
     * Test the add( AT, String... ) method
     */
    @Test public void testAddAtStringElipsis() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test" );
        DefaultServerEntry entry = new DefaultServerEntry( registries, dn );
        
        AttributeType atCN = registries.getAttributeTypeRegistry().lookup( "cn" );
        
        // Test a simple addition
        entry.add( atCN, "test1" );
        assertNotNull( entry.get( atCN ) );
        assertEquals( 1, entry.get( atCN ).size() );
        assertEquals( "test1", entry.get( atCN ).get().get() );
        
        // Test some more addition
        entry.add( atCN, "test2", "test3" );
        assertNotNull( entry.get( atCN ) );
        assertEquals( 3, entry.get( atCN ).size() );
        assertTrue( entry.contains( atCN, "test1" ) );
        assertTrue( entry.contains( atCN, "test2" ) );
        assertTrue( entry.contains( atCN, "test3" ) );
        
        // Test some addition of existing values
        entry.add( atCN, "test2" );
        assertNotNull( entry.get( atCN ) );
        assertEquals( 3, entry.get( atCN ).size() );
        assertTrue( entry.contains( atCN, "test1" ) );
        assertTrue( entry.contains( atCN, "test2" ) );
        assertTrue( entry.contains( atCN, "test3" ) );
        
        // Test the addition of a null value
        entry.add( atCN, (String)null );
        assertNotNull( entry.get( atCN ) );
        assertEquals( 4, entry.get( atCN ).size() );
        assertTrue( entry.contains( atCN, "test1" ) );
        assertTrue( entry.contains( atCN, "test2" ) );
        assertTrue( entry.contains( atCN, "test3" ) );
        assertTrue( entry.contains( atCN, (String )null ) ); 
        
        entry.clear();
        
        // Test the addition of a binary value
        byte[] test4 = StringTools.getBytesUtf8( "test4" );
        
        entry.add( atCN, test4 );
        assertFalse( entry.get( atCN ).contains( test4 ) );
    }


    /**
     * Test the add( AT, byte[]... ) method
     */
    @Test public void testAddAtBytesElipsis() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test" );
        DefaultServerEntry entry = new DefaultServerEntry( registries, dn );
        
        AttributeType atPassword = registries.getAttributeTypeRegistry().lookup( "userPassword" );
        
        byte[] test1 = StringTools.getBytesUtf8( "test1" );
        byte[] test2 = StringTools.getBytesUtf8( "test2" );
        byte[] test3 = StringTools.getBytesUtf8( "test3" );
        
        // Test a simple addition
        entry.add( atPassword, test1 );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 1, entry.get( atPassword ).size() );
        assertTrue( Arrays.equals( test1, (byte[])entry.get( atPassword ).get().get() ) );
        
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
        entry.add( atPassword, (byte[])null );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 4, entry.get( atPassword ).size() );
        assertTrue( entry.contains( atPassword, test1 ) );
        assertTrue( entry.contains( atPassword, test2 ) );
        assertTrue( entry.contains( atPassword, test3 ) );
        assertTrue( entry.contains( atPassword, (byte[] )null ) ); 
        
        entry.clear();
        
        // Test the addition of a String value. It should be converted to a byte array
        byte[] test4 = StringTools.getBytesUtf8( "test4" );

        entry.add( atPassword, "test4" );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 0, entry.get( atPassword ).size() );
        assertFalse( entry.contains( atPassword, test4 ) );
    }


    /**
     * Test the add( AT, SV... ) method
     */
    @Test public void testAddAtServerValueElipsis() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test" );
        DefaultServerEntry entry = new DefaultServerEntry( registries, dn );
        
        AttributeType atCN = registries.getAttributeTypeRegistry().lookup( "cn" );
        AttributeType atPassword = registries.getAttributeTypeRegistry().lookup( "userPassword" );
        
        byte[] b1 = StringTools.getBytesUtf8( "test1" );
        byte[] b2 = StringTools.getBytesUtf8( "test2" );
        byte[] b3 = StringTools.getBytesUtf8( "test3" );

        Value<String> test1 = new ServerStringValue( atCN, "test1" );
        Value<String> test2 = new ServerStringValue( atCN, "test2" );
        Value<String> test3 = new ServerStringValue( atCN, "test3" );
        
        Value<byte[]> testB1 = new ServerBinaryValue( atPassword, b1 );
        Value<byte[]> testB2 = new ServerBinaryValue( atPassword, b2 );
        Value<byte[]> testB3 = new ServerBinaryValue( atPassword, b3 );
        
        // Test a simple addition in atCN
        entry.add( atCN, test1 );
        assertNotNull( entry.get( atCN ) );
        assertEquals( 1, entry.get( atCN ).size() );
        assertEquals( "test1", entry.get( atCN ).get().get() );
        
        // Test some more addition
        entry.add( atCN, test2, test3 );
        assertNotNull( entry.get( atCN ) );
        assertEquals( 3, entry.get( atCN ).size() );
        assertTrue( entry.contains( atCN, "test1" ) );
        assertTrue( entry.contains( atCN, "test2" ) );
        assertTrue( entry.contains( atCN, "test3" ) );
        
        // Test some addition of existing values
        entry.add( atCN, test2 );
        assertNotNull( entry.get( atCN ) );
        assertEquals( 3, entry.get( atCN ).size() );
        assertTrue( entry.contains( atCN, "test1" ) );
        assertTrue( entry.contains( atCN, "test2" ) );
        assertTrue( entry.contains( atCN, "test3" ) );
        
        // Test the addition of a null value
        entry.add( atCN, (String)null );
        assertNotNull( entry.get( atCN ) );
        assertEquals( 4, entry.get( atCN ).size() );
        assertTrue( entry.contains( atCN, "test1" ) );
        assertTrue( entry.contains( atCN, "test2" ) );
        assertTrue( entry.contains( atCN, "test3" ) );
        assertTrue( entry.contains( atCN, (String )null ) ); 
        
        entry.clear();
        
        // Test the addition of a String value. It should be converted to a byte array
        byte[] test4 = StringTools.getBytesUtf8( "test4" );

        entry.add( atCN, test4 );
        assertFalse( entry.contains( atCN, test4 ) );

        // Now, work with a binary attribute
        // Test a simple addition
        entry.add( atPassword, testB1 );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 1, entry.get( atPassword ).size() );
        assertTrue( Arrays.equals( b1, (byte[])entry.get( atPassword ).get().get() ) );
        
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
        entry.add( atPassword, (byte[])null );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 4, entry.get( atPassword ).size() );
        assertTrue( entry.contains( atPassword, b1 ) );
        assertTrue( entry.contains( atPassword, b2 ) );
        assertTrue( entry.contains( atPassword, b3 ) );
        assertTrue( entry.contains( atPassword, (byte[] )null ) ); 
        
        entry.clear();
        
        // Test the addition of a String value. It should be converted to a byte array
        byte[] b4 = StringTools.getBytesUtf8( "test4" );

        entry.add( atPassword, "test4" );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 0, entry.get( atPassword ).size() );
        assertFalse( entry.contains( atPassword, b4 ) );
    }


    /**
     * Test the add( upId, String... ) method
     */
    @Test public void testAddUpIdStringElipsis() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test" );
        DefaultServerEntry entry = new DefaultServerEntry( registries, dn );
        
        AttributeType atCN = registries.getAttributeTypeRegistry().lookup( "cn" );
        
        // Test a simple addition
        entry.add( "CN", "test1" );
        assertNotNull( entry.get( atCN ) );
        assertEquals( atCN, entry.get( atCN ).getAttributeType() );
        assertEquals( "cn", entry.get( atCN ).getAttributeType().getName() );
        assertEquals( "CN", entry.get( atCN ).getUpId() );
        assertEquals( 1, entry.get( atCN ).size() );
        assertEquals( "test1", entry.get( atCN ).get().get() );
        
        // Test some more addition
        entry.add( "CN", "test2", "test3" );
        assertNotNull( entry.get( atCN ) );
        assertEquals( 3, entry.get( atCN ).size() );
        assertTrue( entry.contains( atCN, "test1" ) );
        assertTrue( entry.contains( atCN, "test2" ) );
        assertTrue( entry.contains( atCN, "test3" ) );
        
        // Test some addition of existing values
        entry.add( "CN", "test2" );
        assertNotNull( entry.get( atCN ) );
        assertEquals( 3, entry.get( atCN ).size() );
        assertTrue( entry.contains( atCN, "test1" ) );
        assertTrue( entry.contains( atCN, "test2" ) );
        assertTrue( entry.contains( atCN, "test3" ) );
        
        // Test the addition of a null value
        entry.add( "CN", (String)null );
        assertNotNull( entry.get( atCN ) );
        assertEquals( 4, entry.get( atCN ).size() );
        assertTrue( entry.contains( atCN, "test1" ) );
        assertTrue( entry.contains( atCN, "test2" ) );
        assertTrue( entry.contains( atCN, "test3" ) );
        assertTrue( entry.contains( atCN, (String )null ) ); 
        
        entry.clear();
        
        // Test the addition of a binary value
        byte[] test4 = StringTools.getBytesUtf8( "test4" );
        
        entry.add( "CN", test4 );
        assertFalse( entry.contains(  "CN", test4 ) );
    }


    /**
     * Test the add( upId, byte[]... ) method
     */
    @Test public void testAddUpIdBytesElipsis() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test" );
        DefaultServerEntry entry = new DefaultServerEntry( registries, dn );
        
        AttributeType atPassword = registries.getAttributeTypeRegistry().lookup( "userPassword" );
        
        byte[] test1 = StringTools.getBytesUtf8( "test1" );
        byte[] test2 = StringTools.getBytesUtf8( "test2" );
        byte[] test3 = StringTools.getBytesUtf8( "test3" );
        
        // Test a simple addition
        entry.add( "userPassword", test1 );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 1, entry.get( atPassword ).size() );
        assertTrue( Arrays.equals( test1, (byte[])entry.get( atPassword ).get().get() ) );
        
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
        entry.add( "userPassword", (byte[])null );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 4, entry.get( atPassword ).size() );
        assertTrue( entry.contains( atPassword, test1 ) );
        assertTrue( entry.contains( atPassword, test2 ) );
        assertTrue( entry.contains( atPassword, test3 ) );
        assertTrue( entry.contains( atPassword, (byte[] )null ) ); 
        
        entry.clear();
        
        // Test the addition of a String value. It should be converted to a byte array
        byte[] test4 = StringTools.getBytesUtf8( "test4" );

        entry.add( "userPassword", "test4" );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 0, entry.get( atPassword ).size() );
        assertFalse( entry.contains( atPassword, test4 ) );
    }


    /**
     * Test the add( upId, SV... ) method
     */
    @Test public void testAddUpIdServerValueElipsis() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test" );
        DefaultServerEntry entry = new DefaultServerEntry( registries, dn );
        
        AttributeType atCN = registries.getAttributeTypeRegistry().lookup( "cn" );
        AttributeType atPassword = registries.getAttributeTypeRegistry().lookup( "userPassword" );
        
        byte[] b1 = StringTools.getBytesUtf8( "test1" );
        byte[] b2 = StringTools.getBytesUtf8( "test2" );
        byte[] b3 = StringTools.getBytesUtf8( "test3" );

        Value<String> test1 = new ServerStringValue( atCN, "test1" );
        Value<String> test2 = new ServerStringValue( atCN, "test2" );
        Value<String> test3 = new ServerStringValue( atCN, "test3" );
        
        Value<byte[]> testB1 = new ServerBinaryValue( atPassword, b1 );
        Value<byte[]> testB2 = new ServerBinaryValue( atPassword, b2 );
        Value<byte[]> testB3 = new ServerBinaryValue( atPassword, b3 );
        
        // Test a simple addition in atCN
        entry.add( "cN", test1 );
        assertNotNull( entry.get( atCN ) );
        assertEquals( 1, entry.get( atCN ).size() );
        assertEquals( "test1", entry.get( atCN ).get().get() );
        assertEquals( atCN, entry.get( atCN ).getAttributeType() );
        assertEquals( "cN", entry.get( atCN ).getUpId() );
        
        // Test some more addition
        entry.add( "cN", test2, test3 );
        assertNotNull( entry.get( atCN ) );
        assertEquals( 3, entry.get( atCN ).size() );
        assertTrue( entry.contains( atCN, "test1" ) );
        assertTrue( entry.contains( atCN, "test2" ) );
        assertTrue( entry.contains( atCN, "test3" ) );
        assertEquals( atCN, entry.get( atCN ).getAttributeType() );
        assertEquals( "cN", entry.get( atCN ).getUpId() );
        
        // Test some addition of existing values
        entry.add( "cN", test2 );
        assertNotNull( entry.get( atCN ) );
        assertEquals( 3, entry.get( atCN ).size() );
        assertTrue( entry.contains( atCN, "test1" ) );
        assertTrue( entry.contains( atCN, "test2" ) );
        assertTrue( entry.contains( atCN, "test3" ) );
        
        // Test the addition of a null value
        entry.add( "cN", (String)null );
        assertNotNull( entry.get( atCN ) );
        assertEquals( 4, entry.get( atCN ).size() );
        assertTrue( entry.contains( atCN, "test1" ) );
        assertTrue( entry.contains( atCN, "test2" ) );
        assertTrue( entry.contains( atCN, "test3" ) );
        assertTrue( entry.contains( atCN, (String )null ) ); 
        
        entry.clear();
        
        // Test the addition of a String value. It should be converted to a byte array
        byte[] test4 = StringTools.getBytesUtf8( "test4" );

        entry.add( "cN", test4 );
        assertFalse( entry.contains( "cN", test4 ) );

        // Now, work with a binary attribute
        // Test a simple addition
        entry.add( "userPASSWORD", testB1 );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 1, entry.get( atPassword ).size() );
        assertTrue( Arrays.equals( b1, (byte[])entry.get( atPassword ).get().get() ) );
        assertEquals( atPassword, entry.get( atPassword ).getAttributeType() );
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
        entry.add( "userPASSWORD", (byte[])null );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 4, entry.get( atPassword ).size() );
        assertTrue( entry.contains( atPassword, b1 ) );
        assertTrue( entry.contains( atPassword, b2 ) );
        assertTrue( entry.contains( atPassword, b3 ) );
        assertTrue( entry.contains( atPassword, (byte[] )null ) ); 
        
        entry.clear();
        
        // Test the addition of a String value. It should be converted to a byte array
        byte[] b4 = StringTools.getBytesUtf8( "test4" );

        entry.add( "userPASSWORD", "test4" );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 0, entry.get( atPassword ).size() );
        assertFalse( entry.contains( atPassword, b4 ) );
    }


    /**
     * Test the add( UpId, AT, String... ) method
     */
    @Test public void testAddUpIdAtStringElipsis() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test" );
        DefaultServerEntry entry = new DefaultServerEntry( registries, dn );
        
        AttributeType atCN = registries.getAttributeTypeRegistry().lookup( "cn" );
        
        // Test a simple addition
        entry.add( "cn", atCN, "test1" );
        assertNotNull( entry.get( atCN ) );
        assertEquals( 1, entry.get( atCN ).size() );
        assertEquals( "test1", entry.get( atCN ).get().get() );
        
        // Test some more addition
        entry.add( "CN", atCN, "test2", "test3" );
        assertNotNull( entry.get( atCN ) );
        assertEquals( 3, entry.get( atCN ).size() );
        assertTrue( entry.contains( atCN, "test1" ) );
        assertTrue( entry.contains( atCN, "test2" ) );
        assertTrue( entry.contains( atCN, "test3" ) );
        
        // Test some addition of existing values
        entry.add( "commonName", atCN, "test2" );
        assertNotNull( entry.get( atCN ) );
        assertEquals( 3, entry.get( atCN ).size() );
        assertTrue( entry.contains( atCN, "test1" ) );
        assertTrue( entry.contains( atCN, "test2" ) );
        assertTrue( entry.contains( atCN, "test3" ) );
        
        // Test the addition of a null value
        entry.add( "COMMONname", atCN, (String)null );
        assertNotNull( entry.get( atCN ) );
        assertEquals( 4, entry.get( atCN ).size() );
        assertTrue( entry.contains( atCN, "test1" ) );
        assertTrue( entry.contains( atCN, "test2" ) );
        assertTrue( entry.contains( atCN, "test3" ) );
        assertTrue( entry.contains( atCN, (String )null ) ); 
        
        entry.clear();
        
        // Test the addition of a binary value
        byte[] test4 = StringTools.getBytesUtf8( "test4" );
        
        entry.add( "cn", atCN, test4 );
        assertFalse( entry.contains( "cn", test4 ) );
    }


    /**
     * Test the add( upId, AT, byte[]... ) method
     */
    @Test public void testAddUpIdAtBytesElipsis() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test" );
        DefaultServerEntry entry = new DefaultServerEntry( registries, dn );
        
        AttributeType atPassword = registries.getAttributeTypeRegistry().lookup( "userPassword" );
        
        byte[] test1 = StringTools.getBytesUtf8( "test1" );
        byte[] test2 = StringTools.getBytesUtf8( "test2" );
        byte[] test3 = StringTools.getBytesUtf8( "test3" );
        
        // Test a simple addition
        entry.add( "userPassword", atPassword, test1 );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 1, entry.get( atPassword ).size() );
        assertTrue( Arrays.equals( test1, (byte[])entry.get( atPassword ).get().get() ) );
        
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
        entry.add( "userPassword", atPassword, (byte[])null );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 4, entry.get( atPassword ).size() );
        assertTrue( entry.contains( atPassword, test1 ) );
        assertTrue( entry.contains( atPassword, test2 ) );
        assertTrue( entry.contains( atPassword, test3 ) );
        assertTrue( entry.contains( atPassword, (byte[] )null ) ); 
        
        entry.clear();
        
        // Test the addition of a String value. It should be converted to a byte array
        byte[] test4 = StringTools.getBytesUtf8( "test4" );

        entry.add( "userPassword", atPassword, "test4" );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 0, entry.get( atPassword ).size() );
        assertFalse( entry.contains( atPassword, test4 ) );
    }


    /**
     * Test the add( upId, AT, SV... ) method
     */
    @Test public void testAddUpIdAtServerValueElipsis() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test" );
        DefaultServerEntry entry = new DefaultServerEntry( registries, dn );
        
        AttributeType atCN = registries.getAttributeTypeRegistry().lookup( "cn" );
        AttributeType atPassword = registries.getAttributeTypeRegistry().lookup( "userPassword" );
        
        byte[] b1 = StringTools.getBytesUtf8( "test1" );
        byte[] b2 = StringTools.getBytesUtf8( "test2" );
        byte[] b3 = StringTools.getBytesUtf8( "test3" );

        Value<String> test1 = new ServerStringValue( atCN, "test1" );
        Value<String> test2 = new ServerStringValue( atCN, "test2" );
        Value<String> test3 = new ServerStringValue( atCN, "test3" );
        
        Value<byte[]> testB1 = new ServerBinaryValue( atPassword, b1 );
        Value<byte[]> testB2 = new ServerBinaryValue( atPassword, b2 );
        Value<byte[]> testB3 = new ServerBinaryValue( atPassword, b3 );
        
        // Test a simple addition in atCN
        entry.add( "cN", atCN, test1 );
        assertNotNull( entry.get( atCN ) );
        assertEquals( 1, entry.get( atCN ).size() );
        assertEquals( "test1", entry.get( atCN ).get().get() );
        assertEquals( atCN, entry.get( atCN ).getAttributeType() );
        assertEquals( "cN", entry.get( atCN ).getUpId() );
        
        // Test some more addition
        entry.add( "cN", atCN, test2, test3 );
        assertNotNull( entry.get( atCN ) );
        assertEquals( 3, entry.get( atCN ).size() );
        assertTrue( entry.contains( atCN, "test1" ) );
        assertTrue( entry.contains( atCN, "test2" ) );
        assertTrue( entry.contains( atCN, "test3" ) );
        assertEquals( atCN, entry.get( atCN ).getAttributeType() );
        assertEquals( "cN", entry.get( atCN ).getUpId() );
        
        // Test some addition of existing values
        entry.add( "cN", atCN, test2 );
        assertNotNull( entry.get( atCN ) );
        assertEquals( 3, entry.get( atCN ).size() );
        assertTrue( entry.contains( atCN, "test1" ) );
        assertTrue( entry.contains( atCN, "test2" ) );
        assertTrue( entry.contains( atCN, "test3" ) );
        
        // Test the addition of a null value
        entry.add( "cN", atCN, (String)null );
        assertNotNull( entry.get( atCN ) );
        assertEquals( 4, entry.get( atCN ).size() );
        assertTrue( entry.contains( atCN, "test1" ) );
        assertTrue( entry.contains( atCN, "test2" ) );
        assertTrue( entry.contains( atCN, "test3" ) );
        assertTrue( entry.contains( atCN, (String )null ) ); 
        
        entry.clear();
        
        // Test the addition of a String value. It should be converted to a byte array
        byte[] test4 = StringTools.getBytesUtf8( "test4" );

        entry.add( "cN", atCN, test4 );
        assertFalse( entry.contains( "cN", test4 ) );

        // Now, work with a binary attribute
        // Test a simple addition
        entry.add( "userPASSWORD", atPassword, testB1 );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 1, entry.get( atPassword ).size() );
        assertTrue( Arrays.equals( b1, (byte[])entry.get( atPassword ).get().get() ) );
        assertEquals( atPassword, entry.get( atPassword ).getAttributeType() );
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
        entry.add( "userPASSWORD", atPassword, (byte[])null );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 4, entry.get( atPassword ).size() );
        assertTrue( entry.contains( atPassword, b1 ) );
        assertTrue( entry.contains( atPassword, b2 ) );
        assertTrue( entry.contains( atPassword, b3 ) );
        assertTrue( entry.contains( atPassword, (byte[] )null ) ); 
        
        entry.clear();
        
        // Test the addition of a String value. It should be converted to a byte array
        byte[] b4 = StringTools.getBytesUtf8( "test4" );

        entry.add( "userPASSWORD", atPassword, "test4" );
        assertNotNull( entry.get( atPassword ) );
        assertEquals( 0, entry.get( atPassword ).size() );
        assertFalse( entry.contains( atPassword, b4 ) );
    }

    //-------------------------------------------------------------------------
    // Test the remove method
    //-------------------------------------------------------------------------

    /**
     * Test the remove( AT...) method
     */
    @Test public void testRemoveAtElipsis()
    {
        
    }

    /**
     * Test the remove( upId...) method
     */
    @Test public void testRemoveUpIdElipsis() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test" );
        DefaultServerEntry entry = new DefaultServerEntry( registries, dn );
        
        AttributeType atCN = registries.getAttributeTypeRegistry().lookup( "cn" );
        AttributeType atPassword = registries.getAttributeTypeRegistry().lookup( "userPassword" );
        
        byte[] b1 = StringTools.getBytesUtf8( "test1" );
        byte[] b2 = StringTools.getBytesUtf8( "test2" );

        Value<String> test1 = new ServerStringValue( atCN, "test1" );
        Value<String> test2 = new ServerStringValue( atCN, "test2" );
        
        Value<byte[]> testB1 = new ServerBinaryValue( atPassword, b1 );
        Value<byte[]> testB2 = new ServerBinaryValue( atPassword, b2 );
        
        // test a removal of an non existing attribute
        List<ServerAttribute> removed = entry.remove( atCN );
        assertEquals( 0, removed.size() );
        
        // Test a simple removal
        entry.add( "cN", atCN, test1 );
        assertEquals( 1, entry.size() );
        assertNotNull( entry.get( atCN ) );
        entry.remove( "CN" );
        assertEquals( 0, entry.size() );
        assertNull( entry.get( atCN ) );
        
        // Test a removal of many elements
        entry.put( "CN", test1, test2 );
        entry.put( "userPassword", testB1, testB2 );
        assertEquals( 2, entry.size() );
        assertNotNull( entry.get( atCN ) );
        assertNotNull( entry.get( atPassword ) );
        
        AttributeType OBJECT_CLASS_AT = registries.getAttributeTypeRegistry().lookup( SchemaConstants.OBJECT_CLASS_AT );
        
        entry.remove( "cN", "UsErPaSsWoRd" );
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
        
        try
        {
            entry.remove( "badAttribute" );
            fail();
        }
        catch ( NamingException ne )
        {
            assertTrue( true );
        }
    }

    /**
     * Test the remove( SA...) method
     */
    @Test public void testRemoveSaElipsis()
    {
        
    }
}

