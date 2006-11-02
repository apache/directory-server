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
package org.apache.directory.server;


import javax.naming.directory.*;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import netscape.ldap.LDAPAttribute;
import netscape.ldap.LDAPAttributeSet;
import netscape.ldap.LDAPConnection;
import netscape.ldap.LDAPEntry;
import netscape.ldap.LDAPException;

import org.apache.directory.server.unit.AbstractServerTest;

import java.util.Hashtable;


/**
 * Various add scenario tests.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AddITest extends AbstractServerTest
{
    private static final String RDN = "cn=The Person";

    private DirContext ctx = null;


    /**
     * Create an entry for a person.
     */
    public void setUp() throws Exception
    {
        super.setUp();

        Hashtable<String, String> env = new Hashtable<String,String>();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://localhost:" + port + "/ou=system" );
        env.put( "java.naming.security.principal", "uid=admin,ou=system" );
        env.put( "java.naming.security.credentials", "secret" );
        env.put( "java.naming.security.authentication", "simple" );
        ctx = new InitialDirContext( env );

        // Create a person
        Attributes attributes = new BasicAttributes( true );
        Attribute attribute = new BasicAttribute( "objectClass" );
        attribute.add( "top" );
        attribute.add( "person" );
        attributes.put( attribute );
        attributes.put( "cn", "The Person" );
        attributes.put( "sn", "Person" );
        attributes.put( "description", "this is a person" );
        DirContext person = ctx.createSubcontext( RDN, attributes );

        assertNotNull( person );
    }


    /**
     * Remove the person.
     */
    public void tearDown() throws Exception
    {
        ctx.unbind( RDN );
        ctx.close();
        ctx = null;
        super.tearDown();
    }


    /**
     * Just a little test to check wether the person is created correctly after
     * setup.
     * 
     * @throws NamingException
     */
    public void testSetUpTearDown() throws NamingException
    {
        DirContext person = ( DirContext ) ctx.lookup( RDN );
        assertNotNull( person );

        // Check object classes

        Attributes attributes = person.getAttributes( "" );
        Attribute ocls = attributes.get( "objectClass" );

        String[] expectedOcls =
            { "top", "person" };
        for ( int i = 0; i < expectedOcls.length; i++ )
        {
            String name = expectedOcls[i];
            assertTrue( "object class " + name + " is NOT present when it should be!", ocls.contains( name ) );
        }
    }


    /**
     * This is the original defect as in JIRA DIREVE-216.
     * 
     * @throws NamingException
     */
    public void testAddObjectClasses() throws NamingException
    {

        // modify object classes, add two more
        Attributes attributes = new BasicAttributes( true );
        Attribute ocls = new BasicAttribute( "objectClass" );
        ocls.add( "organizationalPerson" );
        ocls.add( "inetOrgPerson" );
        attributes.put( ocls );

        DirContext person = ( DirContext ) ctx.lookup( RDN );
        person.modifyAttributes( "", DirContext.ADD_ATTRIBUTE, attributes );

        // Read again from directory
        person = ( DirContext ) ctx.lookup( RDN );
        attributes = person.getAttributes( "" );
        Attribute newOcls = attributes.get( "objectClass" );

        String[] expectedOcls =
            { "top", "person", "organizationalPerson", "inetOrgPerson" };
        for ( int i = 0; i < expectedOcls.length; i++ )
        {
            String name = expectedOcls[i];
            assertTrue( "object class " + name + " is present", newOcls.contains( name ) );
        }
    }


    /**
     * This changes a single attribute value. Just as a reference.
     * 
     * @throws NamingException
     */
    public void testModifyDescription() throws NamingException
    {
        String newDescription = "More info on the user ...";

        // modify object classes, add two more
        Attributes attributes = new BasicAttributes( true );
        Attribute desc = new BasicAttribute( "description", newDescription );
        attributes.put( desc );

        DirContext person = ( DirContext ) ctx.lookup( RDN );
        person.modifyAttributes( "", DirContext.REPLACE_ATTRIBUTE, attributes );

        // Read again from directory
        person = ( DirContext ) ctx.lookup( RDN );
        attributes = person.getAttributes( "" );
        Attribute newDesc = attributes.get( "description" );

        assertTrue( "new Description", newDesc.contains( newDescription ) );
    }


    /**
     * Try to add entry with required attribute missing.
     */
    public void testAddWithMissingRequiredAttributes() throws NamingException
    {
        // person without sn
        Attributes attrs = new BasicAttributes();
        Attribute ocls = new BasicAttribute( "objectClass" );
        ocls.add( "top" );
        ocls.add( "person" );
        attrs.put( ocls );
        attrs.put( "cn", "Fiona Apple" );

        try
        {
            ctx.createSubcontext( "cn=Fiona Apple", attrs );
            fail( "creation of entry should fail" );
        }
        catch ( SchemaViolationException e )
        {
            // expected
        }
    }
    
    

    static final String HOST = "localhost";
    static final String USER = "uid=admin,ou=system";
    static final String PASSWORD = "secret";
    static final String BASE = "ou=system";


    /**
     * Testcase to demonstrate DIRSERVER-643 ("Netscape SDK: Adding an entry with
     * two description attributes does not combine values."). Uses Sun ONE Directory
     * SDK for Java 4.1 , or comparable (Netscape, Mozilla).
     */
    public void testAddEntryWithTwoDescriptions() throws LDAPException
    {
        LDAPConnection con = new LDAPConnection();
        con.connect( 3, HOST, super.port, USER, PASSWORD );
        LDAPAttributeSet attrs = new LDAPAttributeSet();
        LDAPAttribute ocls = new LDAPAttribute( "objectclass", new String[]
            { "top", "person" } );
        attrs.add( ocls );
        attrs.add( new LDAPAttribute( "sn", "Bush" ) );
        attrs.add( new LDAPAttribute( "cn", "Kate Bush" ) );

        String descr[] =
            { "a British singer-songwriter with an expressive four-octave voice",
                "one of the most influential female artists of the twentieth century" };

        attrs.add( new LDAPAttribute( "description", descr ) );

        String dn = "cn=Kate Bush," + BASE;
        LDAPEntry kate = new LDAPEntry( dn, attrs );

        con.add( kate );

        // Analyze entry and description attribute
        LDAPEntry kateReloaded = con.read( dn );
        assertNotNull( kateReloaded );
        LDAPAttribute attr = kateReloaded.getAttribute( "description" );
        assertNotNull( attr );
        assertEquals( 2, attr.getStringValueArray().length );

        // Remove entry
        con.delete( dn );
        con.disconnect();
    }


    /**
     * Testcase to demonstrate DIRSERVER-643 ("Netscape SDK: Adding an entry with
     * two description attributes does not combine values."). Uses Sun ONE Directory
     * SDK for Java 4.1 , or comparable (Netscape, Mozilla).
     */
    public void testAddEntryWithTwoDescriptionsVariant() throws LDAPException
    {
        LDAPConnection con = new LDAPConnection();
        con.connect( 3, HOST, super.port, USER, PASSWORD );
        LDAPAttributeSet attrs = new LDAPAttributeSet();
        LDAPAttribute ocls = new LDAPAttribute( "objectclass", new String[]
            { "top", "person" } );
        attrs.add( ocls );
        attrs.add( new LDAPAttribute( "sn", "Bush" ) );
        attrs.add( new LDAPAttribute( "cn", "Kate Bush" ) );

        String descr[] =
            { "a British singer-songwriter with an expressive four-octave voice",
                "one of the most influential female artists of the twentieth century" };

        attrs.add( new LDAPAttribute( "description", descr[0] ) );
        attrs.add( new LDAPAttribute( "description", descr[1] ) );

        String dn = "cn=Kate Bush," + BASE;
        LDAPEntry kate = new LDAPEntry( dn, attrs );

        con.add( kate );

        // Analyze entry and description attribute
        LDAPEntry kateReloaded = con.read( dn );
        assertNotNull( kateReloaded );
        LDAPAttribute attr = kateReloaded.getAttribute( "description" );
        assertNotNull( attr );
        assertEquals( 2, attr.getStringValueArray().length );

        // Remove entry
        con.delete( dn );
        con.disconnect();
    }


    /**
     * Testcase to demonstrate DIRSERVER-643 ("Netscape SDK: Adding an entry with
     * two description attributes does not combine values."). Uses Sun ONE Directory
     * SDK for Java 4.1 , or comparable (Netscape, Mozilla).
     */
    public void testAddEntryWithTwoDescriptionsSecondVariant() throws LDAPException
    {
        LDAPConnection con = new LDAPConnection();
        con.connect( 3, HOST, super.port, USER, PASSWORD );
        LDAPAttributeSet attrs = new LDAPAttributeSet();
        LDAPAttribute ocls = new LDAPAttribute( "objectclass", new String[]
            { "top", "person" } );
        attrs.add( ocls );
        attrs.add( new LDAPAttribute( "sn", "Bush" ) );

        String descr[] =
            { "a British singer-songwriter with an expressive four-octave voice",
                "one of the most influential female artists of the twentieth century" };

        attrs.add( new LDAPAttribute( "description", descr[0] ) );
        attrs.add( new LDAPAttribute( "cn", "Kate Bush" ) );
        attrs.add( new LDAPAttribute( "description", descr[1] ) );

        String dn = "cn=Kate Bush," + BASE;
        LDAPEntry kate = new LDAPEntry( dn, attrs );

        con.add( kate );

        // Analyze entry and description attribute
        LDAPEntry kateReloaded = con.read( dn );
        assertNotNull( kateReloaded );
        LDAPAttribute attr = kateReloaded.getAttribute( "description" );
        assertNotNull( attr );
        assertEquals( 2, attr.getStringValueArray().length );

        // Remove entry
        con.delete( dn );
        con.disconnect();
    }
    
    /**
     * Try to add entry with invalid number of values for a single-valued atribute
     * @see http://issues.apache.org/jira/browse/DIRSERVER-614
     */
    public void testAddWithInvalidNumberOfAttributeValues() throws NamingException
    {
        // add inetOrgPerson with two displayNames
        Attributes attrs = new BasicAttributes();
        Attribute ocls = new BasicAttribute( "objectClass" );
        ocls.add( "top" );
        ocls.add( "inetOrgPerson" );
        attrs.put( ocls );
        attrs.put( "cn", "Fiona Apple" );
        attrs.put( "sn", "Apple" );
        Attribute displayName = new BasicAttribute( "displayName" );
        displayName.add( "Fiona" );
        displayName.add( "Fiona A." );
        attrs.put( displayName );

        try
        {
            ctx.createSubcontext( "cn=Fiona Apple", attrs );
            fail( "creation of entry should fail" );
        }
        catch ( InvalidAttributeValueException e )
        {
            
        }
    }


    /**
     * Try to add entry and an alias to it. Afterwards, remove it.
     */
    public void testAddAlias() throws NamingException
    {

        // Create entry
        Attributes entry = new BasicAttributes();
        Attribute entryOcls = new BasicAttribute( "objectclass" );
        entryOcls.add( "top" );
        entryOcls.add( "organizationalUnit" );
        entry.put( entryOcls );
        entry.put( "ou", "favorite" );
        String entryRdn = "ou=favorite";
        ctx.createSubcontext( entryRdn, entry );

        // Create Alias
        String aliasedObjectName = entryRdn + "," + ctx.getNameInNamespace();
        Attributes alias = new BasicAttributes();
        Attribute aliasOcls = new BasicAttribute( "objectclass" );
        aliasOcls.add( "top" );
        aliasOcls.add( "alias" );
        alias.put( aliasOcls );
        alias.put( "ou", "bestFruit" );
        alias.put( "aliasedObjectName", aliasedObjectName );
        String rdnAlias = "ou=bestFruit";
        ctx.createSubcontext( rdnAlias, alias );

        // Remove alias and entry
        ctx.destroySubcontext( rdnAlias );
        ctx.destroySubcontext( entryRdn );
    }


    /**
     * Try to add entry and an alias to it. Afterwards, remove it. This version
     * cretes a container entry before the operations.
     */
    public void testAddAliasInContainer() throws NamingException
    {

        // Create container
        Attributes container = new BasicAttributes();
        Attribute containerOcls = new BasicAttribute( "objectclass" );
        containerOcls.add( "top" );
        containerOcls.add( "organizationalUnit" );
        container.put( containerOcls );
        container.put( "ou", "Fruits" );
        String containerRdn = "ou=Fruits";
        DirContext containerCtx = ctx.createSubcontext( containerRdn, container );

        // Create entry
        Attributes entry = new BasicAttributes();
        Attribute entryOcls = new BasicAttribute( "objectclass" );
        entryOcls.add( "top" );
        entryOcls.add( "organizationalUnit" );
        entry.put( entryOcls );
        entry.put( "ou", "favorite" );
        String entryRdn = "ou=favorite";
        containerCtx.createSubcontext( entryRdn, entry );

        // Create alias ou=bestFruit,ou=Fruits to entry ou=favorite,ou=Fruits
        String aliasedObjectName = entryRdn + "," + containerCtx.getNameInNamespace();
        Attributes alias = new BasicAttributes();
        Attribute aliasOcls = new BasicAttribute( "objectclass" );
        aliasOcls.add( "top" );
        aliasOcls.add( "alias" );
        alias.put( aliasOcls );
        alias.put( "ou", "bestFruit" );
        alias.put( "aliasedObjectName", aliasedObjectName );
        String rdnAlias = "ou=bestFruit";
        containerCtx.createSubcontext( rdnAlias, alias );

        // search one level scope for alias 
        SearchControls controls = new SearchControls();
        controls.setDerefLinkFlag( true );
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        containerCtx.addToEnvironment( "java.naming.ldap.derefAliases", "never" );
        NamingEnumeration ne = containerCtx.search( "", "(objectClass=*)", controls );
        assertTrue( ne.hasMore() );
        SearchResult sr = ( SearchResult ) ne.next();
        assertEquals( "ou=favorite", sr.getName() );
        assertTrue( ne.hasMore() );
        sr = ( SearchResult ) ne.next();
        assertEquals( "ou=bestFruit", sr.getName() );
        
        // search one level with dereferencing turned on
        controls = new SearchControls();
        controls.setDerefLinkFlag( true );
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        containerCtx.addToEnvironment( "java.naming.ldap.derefAliases", "always" );
        ne = containerCtx.search( "", "(objectClass=*)", controls );
        assertTrue( ne.hasMore() );
        sr = ( SearchResult ) ne.next();
        assertEquals( "ou=favorite", sr.getName() );
        assertFalse( ne.hasMore() );
        
        // search with base set to alias and dereferencing turned on
        controls = new SearchControls();
        controls.setDerefLinkFlag( false );
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        containerCtx.addToEnvironment( "java.naming.ldap.derefAliases", "always" );
        ne = containerCtx.search( "ou=bestFruit", "(objectClass=*)", controls );
        assertTrue( ne.hasMore() );
        sr = ( SearchResult ) ne.next();
        assertEquals( "ldap://localhost:"+super.port+"/ou=favorite,ou=Fruits,ou=system", sr.getName() );
        assertFalse( ne.hasMore() );
        
        // Remove alias and entry
        containerCtx.destroySubcontext( rdnAlias );
        containerCtx.destroySubcontext( entryRdn );

        // Remove container
        ctx.destroySubcontext( containerRdn );
    }
}
