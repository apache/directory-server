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
import java.util.Comparator;
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
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.Syntax;
import org.apache.directory.shared.ldap.schema.UsageEnum;
import org.apache.directory.shared.ldap.schema.syntax.SyntaxChecker;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
    private static AttributeType AT;
    
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
        
        AT = new AttributeType()
        {
            private static final long serialVersionUID = 1L;

            public boolean isSingleValue()
            {
                return false;
            }


            public boolean isCanUserModify()
            {
                return true;
            }


            public boolean isCollective()
            {
                return false;
            }


            public UsageEnum getUsage()
            {
                return null;
            }


            public AttributeType getSuperior() throws NamingException
            {
                return null;
            }


            public Syntax getSyntax() throws NamingException
            {
                return new Syntax()
                {

                    private static final long serialVersionUID = 1L;

                    public boolean isHumanReadable()
                    {
                        return true;
                    }

                    public SyntaxChecker getSyntaxChecker() throws NamingException
                    {
                        return null;
                    }

                    public boolean isObsolete()
                    {
                        return false;
                    }

                    public String getOid()
                    {
                        return null;
                    }

                    public String[] getNames()
                    {
                        return null;
                    }

                    public String getName()
                    {
                        return null;
                    }

                    public String getDescription()
                    {
                        return null;
                    }

                    public String getSchema()
                    {
                        return null;
                    }

                    public void setSchema( String schemaName )
                    {
                    }
                };
            }


            public int getLength()
            {
                return 0;
            }


            public MatchingRule getEquality() throws NamingException
            {
                return new MatchingRule()
                {
                    private static final long serialVersionUID = 1L;

                    public Syntax getSyntax() throws NamingException
                    {
                        return new Syntax()
                        {
                            private static final long serialVersionUID = 1L;


                            public boolean isHumanReadable()
                            {
                                return true;
                            }

                            public SyntaxChecker getSyntaxChecker() throws NamingException
                            {
                                return null;
                            }

                            public boolean isObsolete()
                            {
                                return false;
                            }

                            public String getOid()
                            {
                                return null;
                            }

                            public String[] getNames()
                            {
                                return null;
                            }

                            public String getName()
                            {
                                return null;
                            }

                            public String getDescription()
                            {
                                return null;
                            }

                            public String getSchema()
                            {
                                return null;
                            }

                            public void setSchema( String schemaName )
                            {
                            }
                        };
                    }

                    public Comparator getComparator() throws NamingException
                    {
                        return null;
                    }

                    public Normalizer getNormalizer() throws NamingException
                    {
                        return new Normalizer()
                        {
                            private static final long serialVersionUID = 1L;

                            public Object normalize( Object value ) throws NamingException
                            {
                                return StringTools.deepTrimToLower( value.toString() );
                            }
                        };
                    }

                    public boolean isObsolete()
                    {
                        return false;
                    }

                    public String getOid()
                    {
                        return null;
                    }

                    public String[] getNames()
                    {
                        return null;
                    }

                    public String getName()
                    {
                        return null;
                    }

                    public String getDescription()
                    {
                        return null;
                    }

                    public String getSchema()
                    {
                        return null;
                    }

                    public void setSchema( String schemaName )
                    {
                    }
                };
            }


            public MatchingRule getOrdering() throws NamingException
            {
                return null;
            }


            public MatchingRule getSubstr() throws NamingException
            {
                return null;
            }


            public boolean isAncestorOf( AttributeType descendant ) throws NamingException
            {
                return false;
            }


            public boolean isDescentantOf( AttributeType ancestor ) throws NamingException
            {
                return false;
            }


            public boolean isObsolete()
            {
                return false;
            }


            public String getOid()
            {
                return "1.2.3";
            }


            public String[] getNames()
            {
                return new String[]
                    { "test" };
            }


            public String getName()
            {
                return "test";
            }


            public String getDescription()
            {
                return "test";
            }


            public String getSchema()
            {
                return null;
            }


            public void setSchema( String schemaName )
            {
            }
        };
    }


    //-------------------------------------------------------------------------
    // Test the constructors
    //-------------------------------------------------------------------------
    
    /**
     * Test a conversion from a ServerEntry to an AttributesImpl
     */
    @Test public void testToAttributesImpl() throws InvalidNameException, NamingException
    {
        LdapDN dn = new LdapDN( "cn=test" );
        DefaultServerEntry entry = new DefaultServerEntry( registries, dn );
        
        ObjectClassAttribute oc = new ObjectClassAttribute( registries );
        oc.add( "top", "person", "inetOrgPerson" );
        
        entry.addObjectClass( oc );
        //entry.put( "cn", registries.getAttributeTypeRegistry().lookup( "cn" ), "test" );
        
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

        // We should still have the ObjectClass Attribute
        assertEquals( 1, expected.size() );
    }


    /**
     * Test a conversion from a ServerEntry to an BasicAttributes
     */
    @Test public void testToBasicAttributes() throws InvalidNameException, NamingException
    {
        LdapDN dn = new LdapDN( "cn=test" );
        DefaultServerEntry entry = new DefaultServerEntry( registries,dn );
        
        ObjectClassAttribute oc = new ObjectClassAttribute( registries );
        oc.add( "top", "person", "inetOrgPerson" );
        
        entry.addObjectClass( oc );
        //entry.put( "cn", registries.getAttributeTypeRegistry().lookup( "cn" ), "test" );
        
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
    @Test public void tesSetATElipsis() throws NamingException
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
        assertEquals( atSN, sa.getType() );
        assertEquals( "sn", sa.getType().getName() );
        
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
        assertEquals( atL, sa.getType() );
        assertEquals( "l", sa.getType().getName() );

        sa = entry.get( "c" );
        assertNotNull( sa );
        assertEquals( atC, sa.getType() );
        assertEquals( "c", sa.getType().getName() );

        sa = entry.get( "2.5.4.9" );
        assertNotNull( sa );
        assertEquals( atStreet, sa.getType() );
        assertEquals( "street", sa.getType().getName() );

        sa = entry.get( "givenName" );
        assertNotNull( sa );
        assertEquals( atGN, sa.getType() );
        assertEquals( "givenName", sa.getType().getName() );
        
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
        
        assertEquals( OBJECT_CLASS_AT, oc.getType() );
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
        assertEquals( "sn", sa.getType().getName() );
        
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
        assertEquals( atL, sa.getType() );
        assertEquals( "l", sa.getType().getName() );
        assertEquals( "L", sa.getUpId() );

        sa = entry.get( "c" );
        assertNotNull( sa );
        assertEquals( atC, sa.getType() );
        assertEquals( "c", sa.getType().getName() );
        assertEquals( "CountryName", sa.getUpId() );

        sa = entry.get( "2.5.4.9" );
        assertNotNull( sa );
        assertEquals( atStreet, sa.getType() );
        assertEquals( "street", sa.getType().getName() );
        assertEquals( "2.5.4.9", sa.getUpId() );

        sa = entry.get( "givenName" );
        assertNotNull( sa );
        assertEquals( atGN, sa.getType() );
        assertEquals( "givenName", sa.getType().getName() );
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
        
        assertEquals( 2, entry.size() );
        assertNotNull( entry.get( "l" ) );
        assertEquals( "france", entry.get( "l" ).get().get() );
        
        ServerAttribute sb = new DefaultServerAttribute( atC, "countryTest" );
        ServerAttribute sc = new DefaultServerAttribute( atGN, "test" );
        ServerAttribute sd = new DefaultServerAttribute( atStreet, "testStreet" );
        entry.put( sb, sc, sd );

        assertEquals( 5, entry.size() );
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
        assertEquals( 5, entry.size() );
        assertNotNull( entry.get( atC ) );
        assertEquals( "countryTestTest", entry.get( atC ).get().get() );
        assertNotNull( entry.get( atGN ) );
        assertEquals( "testtest", entry.get( atGN ).get().get() );
        assertNotNull( entry.get( atStreet) );
        assertEquals( "testStreet", entry.get( atStreet ).get().get() );
        
        // test an ObjectClass replacement
        AttributeType OBJECT_CLASS_AT = registries.getAttributeTypeRegistry().lookup( SchemaConstants.OBJECT_CLASS_AT );
        ServerAttribute oc = new ObjectClassAttribute( registries, "OBJECTCLASS", "person", "inetorgperson" );
        List<ServerAttribute> oldOc = entry.put( oc );
        
        assertNotNull( oldOc );
        assertEquals( 1, oldOc.size() );
        assertEquals( "top", oldOc.get( 0 ).get().get() );
        
        assertNotNull( entry.get( "objectClass" ) );

        ServerAttribute newOc = entry.get( "objectClass" );
        
        assertNotNull( newOc );
        assertEquals( OBJECT_CLASS_AT, newOc.getType() );
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
        assertEquals( 2, entry.size() );
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
        
        assertEquals( 2, entry.size() );
        assertEquals( "cn", entry.get( atCN ).getUpId() );
        assertEquals( 1, entry.get( atCN ).size() );
        assertEquals( "test", entry.get( atCN ).get().get() );
        
        // Add more than one value
        entry.put( atCN, "test1", "test2", "test3" );
        
        assertEquals( 2, entry.size() );
        assertEquals( "cn", entry.get( atCN ).getUpId() );
        assertEquals( 3, entry.get( atCN ).size() );
        assertTrue( entry.contains( "cn", "test1" ) );
        assertTrue( entry.contains( "cn", "test2" ) );
        assertTrue( entry.contains( "cn", "test3" ) );
        
        // Add twice the same value
        ServerAttribute sa = entry.put( atCN, "test1", "test2", "test1" );
        
        assertEquals( 3, sa.size() );
        assertTrue( sa.contains( "test1", "test2", "test3" ) );
        assertEquals( 2, entry.size() );
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
        assertEquals( 2, entry.size() );
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
        
        assertEquals( 2, entry.size() );
        assertEquals( "userPassword", entry.get( atPwd ).getUpId() );
        assertEquals( 1, entry.get( atPwd ).size() );
        assertTrue( Arrays.equals( password, (byte[])entry.get( atPwd ).get().get() ) );
        
        // Add more than one value
        entry.put( atPwd, test1, test2, test3 );
        
        assertEquals( 2, entry.size() );
        assertEquals( "userPassword", entry.get( atPwd ).getUpId() );
        assertEquals( 3, entry.get( atPwd ).size() );
        assertTrue( entry.contains( "userpassword", test1 ) );
        assertTrue( entry.contains( "userpassword", test2 ) );
        assertTrue( entry.contains( "userpassword", test3 ) );
        
        // Add twice the same value
        ServerAttribute sa = entry.put( atPwd, test1, test2, test1 );
        
        assertEquals( 3, sa.size() );
        assertTrue( sa.contains( test1, test2, test3 ) );
        assertEquals( 2, entry.size() );
        assertEquals( "userPassword", entry.get( atPwd ).getUpId() );
        assertEquals( 2, entry.get( atPwd ).size() );
        assertTrue( entry.contains( "userpassword", test1 ) );
        assertTrue( entry.contains( "userpassword", test2 ) );
    }
    

    /**
     * Test the put( AT, ServerValue... ) method
     */
    @Test public void tesPutAtSVs() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test" );
        DefaultServerEntry entry = new DefaultServerEntry( registries, dn );
        
        // Adding a null value to an attribute
        AttributeType atCN = registries.getAttributeTypeRegistry().lookup( "cn" );
        entry.put( atCN, (ServerValue<?>)null );
        
        assertEquals( 2, entry.size() );
        assertEquals( "cn", entry.get( atCN ).getUpId() );
        
        // Check that we can't use invalid arguments
        try
        {
            entry.put( (AttributeType)null, (ServerValue<?>)null );
            fail();
        }
        catch( IllegalArgumentException iae )
        {
            assertTrue( true );
        }
        
        // Add a single value
        atCN = registries.getAttributeTypeRegistry().lookup( "cn" );
        ServerValue<?> ssv = new ServerStringValue( atCN, "test" );
        entry.put( atCN, ssv );
        
        assertEquals( 2, entry.size() );
        assertEquals( "cn", entry.get( atCN ).getUpId() );
        assertEquals( 1, entry.get( atCN ).size() );
        assertEquals( "test", entry.get( atCN ).get().get() );
        
        // Add more than one value
        entry.put( atCN, new ServerStringValue( atCN, "test1" ),
                         new ServerStringValue( atCN, "test2" ), 
                         new ServerStringValue( atCN, "test3" ));
        
        assertEquals( 2, entry.size() );
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
        assertEquals( 2, entry.size() );
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
        
        assertEquals( 2, entry.size() );
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
        
        assertEquals( 2, entry.size() );
        assertEquals( "cn", entry.get( atCN ).getUpId() );
        assertEquals( 1, entry.get( atCN ).size() );
        assertEquals( "test", entry.get( atCN ).get().get() );
        
        // Add more than one value
        entry.put( "cn", "test1", "test2", "test3" );
        
        assertEquals( 2, entry.size() );
        assertEquals( "cn", entry.get( atCN ).getUpId() );
        assertEquals( 3, entry.get( atCN ).size() );
        assertTrue( entry.contains( "cn", "test1" ) );
        assertTrue( entry.contains( "cn", "test2" ) );
        assertTrue( entry.contains( "cn", "test3" ) );
        
        // Add twice the same value
        ServerAttribute sa = entry.put( "cn", "test1", "test2", "test1" );
        
        assertEquals( 3, sa.size() );
        assertTrue( sa.contains( "test1", "test2", "test3" ) );
        assertEquals( 2, entry.size() );
        assertEquals( "cn", entry.get( atCN ).getUpId() );
        assertEquals( 2, entry.get( atCN ).size() );
        assertTrue( entry.contains( "cn", "test1" ) );
        assertTrue( entry.contains( "cn", "test2" ) );
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
        assertEquals( 2, entry.size() );
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
        
        assertEquals( 2, entry.size() );
        assertEquals( "userPassword", entry.get( atPassword ).getUpId() );
        assertEquals( 1, entry.get( atPassword ).size() );
        assertTrue( Arrays.equals( test, (byte[])entry.get( atPassword ).get().get() ) );
        
        // Add more than one value
        entry.put( "userPassword", test1, test2, test3 );
        
        assertEquals( 2, entry.size() );
        assertEquals( "userPassword", entry.get( atPassword ).getUpId() );
        assertEquals( 3, entry.get( atPassword ).size() );
        assertTrue( entry.contains( "userPassword", test1 ) );
        assertTrue( entry.contains( "userPassword", test2 ) );
        assertTrue( entry.contains( "userPassword", test3 ) );
        
        // Add twice the same value
        ServerAttribute sa = entry.put( "userPassword", test1, test2, test1 );
        
        assertEquals( 3, sa.size() );
        assertTrue( sa.contains( test1, test2, test3 ) );
        assertEquals( 2, entry.size() );
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
        assertEquals( 2, entry.size() );
        assertEquals( "commonName", entry.get( atCN ).getUpId() );
        assertEquals( "cn", entry.get( atCN ).getType().getName() );
        assertNull( entry.get( atCN ).get().get() );
        
        // Check that we can use a null AttributeType
        entry.put( "commonName", (AttributeType)null, (String)null );
        assertEquals( 2, entry.size() );
        assertEquals( "commonName", entry.get( atCN ).getUpId() );
        assertEquals( "cn", entry.get( atCN ).getType().getName() );
        assertNull( entry.get( atCN ).get().get() );
        
        // Test that we can use a null upId
        entry.put( null, atCN, (String)null );
        assertEquals( 2, entry.size() );
        assertEquals( "cn", entry.get( atCN ).getUpId() );
        assertEquals( "cn", entry.get( atCN ).getType().getName() );
        assertNull( entry.get( atCN ).get().get() );
        
        // Test that we can't use an upId which is not compatible
        // with the AT
        try
        {
            entry.put( "sn", atCN, (String)null );
            fail();
        }
        catch( IllegalArgumentException iae )
        {
            assertTrue( true );
        }
        
        // Test that we can add some new attributes with values
        ServerAttribute result = entry.put( "CN", atCN, "test1", "test2", "test3" );
        assertNotNull( result );
        assertEquals( "cn", result.getUpId() );
        assertEquals( 2, entry.size() );
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
        assertEquals( 2, entry.size() );
        assertEquals( "userPassword", entry.get( atPassword ).getUpId() );
        assertEquals( "userPassword", entry.get( atPassword ).getType().getName() );
        assertNull( entry.get( atPassword ).get().get() );
        
        // Check that we can use a null AttributeType
        entry.put( "userPassword", (AttributeType)null, (byte[])null );
        assertEquals( 2, entry.size() );
        assertEquals( "userPassword", entry.get( atPassword ).getUpId() );
        assertEquals( "userPassword", entry.get( atPassword ).getType().getName() );
        assertNull( entry.get( atPassword ).get().get() );
        
        // Test that we can use a null upId
        entry.put( null, atPassword, (byte[])null );
        assertEquals( 2, entry.size() );
        assertEquals( "userPassword", entry.get( atPassword ).getUpId() );
        assertEquals( "userPassword", entry.get( atPassword ).getType().getName() );
        assertNull( entry.get( atPassword ).get().get() );
        
        // Test that we can't use an upId which is not compatible
        // with the AT
        try
        {
            entry.put( "sn", atPassword, (byte[])null );
            fail();
        }
        catch( IllegalArgumentException iae )
        {
            assertTrue( true );
        }
        
        // Test that we can add some new attributes with values
        byte[] test1 = StringTools.getBytesUtf8( "test1" );
        byte[] test2 = StringTools.getBytesUtf8( "test2" );
        byte[] test3 = StringTools.getBytesUtf8( "test3" );

        ServerAttribute result = entry.put( "UserPassword", atPassword, test1, test2, test3 );
        assertNotNull( result );
        assertEquals( "userPassword", result.getUpId() );
        assertEquals( 2, entry.size() );
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
            entry.put( null, (AttributeType)null, (ServerValue<?>)null );
            fail();
        }
        catch( IllegalArgumentException iae )
        {
            assertTrue( true );
        }
        
        // Test an empty AT
        entry.put( "commonName", atCN, (ServerValue<?>)null );
        assertEquals( 2, entry.size() );
        assertEquals( "commonName", entry.get( atCN ).getUpId() );
        assertEquals( "cn", entry.get( atCN ).getType().getName() );
        assertNull( entry.get( atCN ).get().get() );
        
        // Check that we can use a null AttributeType
        entry.put( "commonName", (AttributeType)null, (ServerValue<?>)null );
        assertEquals( 2, entry.size() );
        assertEquals( "commonName", entry.get( atCN ).getUpId() );
        assertEquals( "cn", entry.get( atCN ).getType().getName() );
        assertNull( entry.get( atCN ).get().get() );
        
        // Test that we can use a null upId
        entry.put( null, atCN, (ServerValue<?>)null );
        assertEquals( 2, entry.size() );
        assertEquals( "cn", entry.get( atCN ).getUpId() );
        assertEquals( "cn", entry.get( atCN ).getType().getName() );
        assertNull( entry.get( atCN ).get().get() );
        
        // Test that we can't use an upId which is not compatible
        // with the AT
        try
        {
            entry.put( "sn", atCN, (ServerValue<?>)null );
            fail();
        }
        catch( IllegalArgumentException iae )
        {
            assertTrue( true );
        }
        
        // Test that we can add some new attributes with values
        ServerValue<String> test1 = new ServerStringValue( atCN, "test1" );
        ServerValue<String> test2 = new ServerStringValue( atCN, "test2" );
        ServerValue<String> test3 = new ServerStringValue( atCN, "test3" );

        ServerAttribute result = entry.put( "CN", atCN, test1, test2, test3 );
        assertNotNull( result );
        assertEquals( "cn", result.getUpId() );
        assertEquals( 2, entry.size() );
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
            entry.put( (String)null, (ServerValue<?>)null );
            fail();
        }
        catch( IllegalArgumentException iae )
        {
            assertTrue( true );
        }
        
        // Test an null valued AT
        entry.put( "commonName", (ServerValue<?>)null );
        assertEquals( 2, entry.size() );
        assertEquals( "commonName", entry.get( atCN ).getUpId() );
        assertEquals( "cn", entry.get( atCN ).getType().getName() );
        assertNull( entry.get( atCN ).get().get() );

        // Test that we can add some new attributes with values
        ServerValue<String> test1 = new ServerStringValue( atCN, "test1" );
        ServerValue<String> test2 = new ServerStringValue( atCN, "test2" );
        ServerValue<String> test3 = new ServerStringValue( atCN, "test3" );

        ServerAttribute result = entry.put( "CN", test1, test2, test3 );
        assertNotNull( result );
        assertEquals( "commonName", result.getUpId() );
        assertEquals( 2, entry.size() );
        assertEquals( "CN", entry.get( atCN ).getUpId() );
        assertNotNull( entry.get( atCN ).get() );
        assertTrue( entry.contains( "cn", "test1" ) );
        assertTrue( entry.contains( "CN", "test2" ) );
        assertTrue( entry.contains( "commonName", "test3" ) );
    }
}
