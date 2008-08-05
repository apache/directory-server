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
package org.apache.directory.server.operations.add;


import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.ReferralException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InvalidAttributeValueException;
import javax.naming.directory.SchemaViolationException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import netscape.ldap.LDAPAttribute;
import netscape.ldap.LDAPAttributeSet;
import netscape.ldap.LDAPConnection;
import netscape.ldap.LDAPConstraints;
import netscape.ldap.LDAPControl;
import netscape.ldap.LDAPEntry;
import netscape.ldap.LDAPException;
import netscape.ldap.LDAPResponse;
import netscape.ldap.LDAPResponseListener;
import netscape.ldap.LDAPSearchConstraints;

import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;

import org.apache.directory.server.integ.SiRunner;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredConnection;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContext;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContextThrowOnRefferal;

import org.apache.directory.server.newldap.LdapServer;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * Various add scenario tests.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 674593 $
 */
@RunWith ( SiRunner.class ) 
@CleanupLevel ( Level.SUITE )
@ApplyLdifs( {
    // Entry # 0
    "dn: cn=The Person,ou=system\n" +
    "objectClass: person\n" +
    "objectClass: top\n" +
    "cn: The Person\n" +
    "description: this is a person\n" +
    "sn: Person\n\n" + 
    // Entry # 1
    "dn: uid=akarasulu,ou=users,ou=system\n" +
    "objectClass: uidObject\n" +
    "objectClass: person\n" +
    "objectClass: top\n" +
    "uid: akarasulu\n" +
    "cn: Alex Karasulu\n" +
    "sn: karasulu\n\n" + 
    // Entry # 2
    "dn: ou=Computers,uid=akarasulu,ou=users,ou=system\n" +
    "objectClass: organizationalUnit\n" +
    "objectClass: top\n" +
    "ou: computers\n" +
    "description: Computers for Alex\n" +
    "seeAlso: ou=Machines,uid=akarasulu,ou=users,ou=system\n\n" + 
    // Entry # 3
    "dn: uid=akarasuluref,ou=users,ou=system\n" +
    "objectClass: uidObject\n" +
    "objectClass: referral\n" +
    "objectClass: top\n" +
    "uid: akarasuluref\n" +
    "ref: ldap://localhost:10389/uid=akarasulu,ou=users,ou=system\n" + 
    "ref: ldap://foo:10389/uid=akarasulu,ou=users,ou=system\n" +
    "ref: ldap://bar:10389/uid=akarasulu,ou=users,ou=system\n\n"
    }
)
public class AddIT
{
    private static final Logger LOG = LoggerFactory.getLogger( AddIT.class );
    private static final String RDN = "cn=The Person";

    private static final String BASE = "ou=system";


    public static LdapServer ldapServer;


    /**
     * This is the original defect as in JIRA DIREVE-216.
     * 
     * @throws NamingException if we cannot connect and perform add operations
     */
    @Test
    public void testAddObjectClasses() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );

        // modify object classes, add two more
        Attributes attributes = new AttributesImpl( true );
        Attribute ocls = new AttributeImpl( "objectClass" );
        ocls.add( "organizationalPerson" );
        ocls.add( "inetOrgPerson" );
        attributes.put( ocls );

        DirContext person = ( DirContext ) ctx.lookup( RDN );
        person.modifyAttributes( "", DirContext.ADD_ATTRIBUTE, attributes );

        // Read again from directory
        person = ( DirContext ) ctx.lookup( RDN );
        attributes = person.getAttributes( "" );
        Attribute newOcls = attributes.get( "objectClass" );

        String[] expectedOcls = { "top", "person", "organizationalPerson", "inetOrgPerson" };
        for ( String name : expectedOcls )
        {
            assertTrue( "object class " + name + " is present", newOcls.contains( name ) );
        }
    }


    /**
     * This changes a single attribute value. Just as a reference.
     * 
     * @throws NamingException if we cannot connect and modify the description
     */
    @Test
    public void testModifyDescription() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );

        String newDescription = "More info on the user ...";

        // modify object classes, add two more
        Attributes attributes = new AttributesImpl( true );
        Attribute desc = new AttributeImpl( "description", newDescription );
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
     * 
     * @throws NamingException if we fail to connect
     */
    @Test
    public void testAddWithMissingRequiredAttributes() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );

        // person without sn
        Attributes attrs = new AttributesImpl();
        Attribute ocls = new AttributeImpl( "objectClass" );
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
    
    
    /**
     * Test case to demonstrate DIRSERVER-643 ("Netscape SDK: Adding an entry with
     * two description attributes does not combine values."). Uses Sun ONE Directory
     * SDK for Java 4.1 , or comparable (Netscape, Mozilla).
     * 
     * @throws LDAPException if we fail to connect and add entries
     */
    @Test
    public void testAddEntryWithTwoDescriptions() throws Exception
    {
        LDAPConnection con = getWiredConnection( ldapServer );
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
     * 
     * @throws LDAPException if we fail to connect and add entries
     */
    @Test
    public void testAddEntryWithTwoDescriptionsVariant() throws Exception
    {
        LDAPConnection con = getWiredConnection( ldapServer );
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
     * 
     * @throws LDAPException if we fail to connect and add entries
     */
    @Test
    public void testAddEntryWithTwoDescriptionsSecondVariant() throws Exception
    {
        LDAPConnection con = getWiredConnection( ldapServer );
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
     * Try to add entry with invalid number of values for a single-valued attribute
     * 
     * @throws NamingException if we fail to connect and add entries
     * @see <a href="http://issues.apache.org/jira/browse/DIRSERVER-614">DIRSERVER-614</a>
     */
    @Test
    public void testAddWithInvalidNumberOfAttributeValues() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );
        
        // add inetOrgPerson with two displayNames
        Attributes attrs = new AttributesImpl();
        Attribute ocls = new AttributeImpl( "objectClass" );
        ocls.add( "top" );
        ocls.add( "inetOrgPerson" );
        attrs.put( ocls );
        attrs.put( "cn", "Fiona Apple" );
        attrs.put( "sn", "Apple" );
        Attribute displayName = new AttributeImpl( "displayName" );
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
     * 
     * @throws NamingException if we fail to connect and add entries
     */
    @Test
    public void testAddAlias() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );

        // Create entry
        Attributes entry = new AttributesImpl();
        Attribute entryOcls = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT );
        entryOcls.add( SchemaConstants.TOP_OC );
        entryOcls.add( SchemaConstants.ORGANIZATIONAL_UNIT_OC );
        entry.put( entryOcls );
        entry.put( SchemaConstants.OU_AT, "favorite" );
        String entryRdn = "ou=favorite";
        ctx.createSubcontext( entryRdn, entry );

        // Create Alias
        String aliasedObjectName = entryRdn + "," + ctx.getNameInNamespace();
        Attributes alias = new AttributesImpl();
        Attribute aliasOcls = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT );
        aliasOcls.add( SchemaConstants.TOP_OC );
        aliasOcls.add( SchemaConstants.EXTENSIBLE_OBJECT_OC );
        aliasOcls.add( SchemaConstants.ALIAS_OC );
        alias.put( aliasOcls );
        alias.put( SchemaConstants.OU_AT, "bestFruit" );
        alias.put( SchemaConstants.ALIASED_OBJECT_NAME_AT, aliasedObjectName );
        String rdnAlias = "ou=bestFruit";
        ctx.createSubcontext( rdnAlias, alias );

        // Remove alias and entry
        ctx.destroySubcontext( rdnAlias );
        ctx.destroySubcontext( entryRdn );
    }


    /**
     * Try to add entry and an alias to it. Afterwards, remove it. This version
     * cretes a container entry before the operations.
     * 
     * @throws NamingException if we fail to connect and add entries
     */
    @Test
    public void testAddAliasInContainer() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );

        // Create container
        Attributes container = new AttributesImpl();
        Attribute containerOcls = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT );
        containerOcls.add( SchemaConstants.TOP_OC );
        containerOcls.add( SchemaConstants.ORGANIZATIONAL_UNIT_OC );
        container.put( containerOcls );
        container.put( SchemaConstants.OU_AT, "Fruits" );
        String containerRdn = "ou=Fruits";
        DirContext containerCtx = ctx.createSubcontext( containerRdn, container );

        // Create entry
        Attributes entry = new AttributesImpl();
        Attribute entryOcls = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT );
        entryOcls.add( SchemaConstants.TOP_OC );
        entryOcls.add( SchemaConstants.ORGANIZATIONAL_UNIT_OC );
        entry.put( entryOcls );
        entry.put( SchemaConstants.OU_AT, "favorite" );
        String entryRdn = "ou=favorite";
        containerCtx.createSubcontext( entryRdn, entry );

        // Create alias ou=bestFruit,ou=Fruits to entry ou=favorite,ou=Fruits
        String aliasedObjectName = entryRdn + "," + containerCtx.getNameInNamespace();
        Attributes alias = new AttributesImpl();
        Attribute aliasOcls = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT );
        aliasOcls.add( SchemaConstants.TOP_OC );
        aliasOcls.add( SchemaConstants.EXTENSIBLE_OBJECT_OC );
        aliasOcls.add( SchemaConstants.ALIAS_OC );
        alias.put( aliasOcls );
        alias.put( SchemaConstants.OU_AT, "bestFruit" );
        alias.put( SchemaConstants.ALIASED_OBJECT_NAME_AT, aliasedObjectName );
        String rdnAlias = "ou=bestFruit";
        containerCtx.createSubcontext( rdnAlias, alias );

        // search one level scope for alias 
        SearchControls controls = new SearchControls();
        controls.setDerefLinkFlag( true );
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        containerCtx.addToEnvironment( "java.naming.ldap.derefAliases", "never" );
        NamingEnumeration<SearchResult> ne = containerCtx.search( "", "(objectClass=*)", controls );
        assertTrue( ne.hasMore() );
        SearchResult sr = ne.next();
        assertEquals( "ou=favorite", sr.getName() );
        assertTrue( ne.hasMore() );
        sr = ne.next();
        assertEquals( "ou=bestFruit", sr.getName() );
        
        // search one level with dereferencing turned on
        controls = new SearchControls();
        controls.setDerefLinkFlag( true );
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        containerCtx.addToEnvironment( "java.naming.ldap.derefAliases", "always" );
        ne = containerCtx.search( "", "(objectClass=*)", controls );
        assertTrue( ne.hasMore() );
        sr = ne.next();
        assertEquals( "ou=favorite", sr.getName() );
        assertFalse( ne.hasMore() );
        
        // search with base set to alias and dereferencing turned on
        controls = new SearchControls();
        controls.setDerefLinkFlag( false );
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        containerCtx.addToEnvironment( "java.naming.ldap.derefAliases", "always" );
        ne = containerCtx.search( "ou=bestFruit", "(objectClass=*)", controls );
        assertTrue( ne.hasMore() );
        sr = ne.next();
        assertEquals( "ldap://localhost:"+ ldapServer.getIpPort() +"/ou=favorite,ou=Fruits,ou=system", sr.getName() );
        assertFalse( ne.hasMore() );
        
        // Remove alias and entry
        containerCtx.destroySubcontext( rdnAlias );
        containerCtx.destroySubcontext( entryRdn );

        // Remove container
        ctx.destroySubcontext( containerRdn );
    }
    
    
    /**
     * Try to add entry and an alias to it. Afterwards, remove it.  Taken from
     * DIRSERVER-1157 test contribution.
     * 
     * @see https://issues.apache.org/jira/browse/DIRSERVER-1157
     * @throws Exception
     */
    @Test
    public void testAddDeleteAlias() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );

        // Create entry ou=favorite,dc=example,dc=com
        Attributes entry = new AttributesImpl();
        Attribute entryOcls = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT );
        entryOcls.add( SchemaConstants.TOP_OC );
        entryOcls.add( SchemaConstants.ORGANIZATIONAL_UNIT_OC );
        entry.put( entryOcls );
        entry.put( SchemaConstants.OU_AT, "favorite" );
        String entryRdn = "ou=favorite";
        ctx.createSubcontext( entryRdn, entry );

        // Create Alias ou=bestFruit,dc=example,dc=com to ou=favorite
        String aliasedObjectName = entryRdn + "," + ctx.getNameInNamespace();
        Attributes alias = new AttributesImpl();
        Attribute aliasOcls = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT );
        aliasOcls.add( SchemaConstants.TOP_OC );
        aliasOcls.add( SchemaConstants.EXTENSIBLE_OBJECT_OC );
        aliasOcls.add( SchemaConstants.ALIAS_OC );
        alias.put( aliasOcls );
        alias.put( SchemaConstants.OU_AT, "bestFruit" );
        alias.put( SchemaConstants.ALIASED_OBJECT_NAME_AT, aliasedObjectName );
        String rdnAlias = "ou=bestFruit";
        ctx.createSubcontext( rdnAlias, alias );

        // Remove alias and entry
        ctx.destroySubcontext( rdnAlias ); //Waiting for Connection.reply()
        ctx.destroySubcontext( entryRdn );
    }
    
    
    /**
     * Tests add operation on referral entry with the ManageDsaIT control.
     */
    @Test
    public void testOnReferralWithManageDsaITControl() throws Exception
    {
        LDAPConnection conn = getWiredConnection( ldapServer );
        LDAPConstraints constraints = new LDAPSearchConstraints();
        constraints.setClientControls( new LDAPControl( LDAPControl.MANAGEDSAIT, true, new byte[0] ) );
        constraints.setServerControls( new LDAPControl( LDAPControl.MANAGEDSAIT, true, new byte[0] ) );
        conn.setConstraints( constraints );
        
        // add success
        LDAPAttributeSet attrSet = new LDAPAttributeSet();
        attrSet.add( new LDAPAttribute( "objectClass", "organizationalUnit" ) );
        attrSet.add( new LDAPAttribute( "ou", "UnderReferral" ) );
        LDAPEntry entry = new LDAPEntry( "ou=UnderReferral,uid=akarasuluref,ou=users,ou=system", attrSet );
        
        conn.add( entry, constraints );
        
        LDAPEntry reread = conn.read( "ou=UnderReferral,uid=akarasuluref,ou=users,ou=system", 
            ( LDAPSearchConstraints ) constraints );
        assertEquals( "ou=UnderReferral,uid=akarasuluref,ou=users,ou=system", reread.getDN() );
        
        conn.disconnect();
    }
    
    
    /**
     * Tests referral handling when an ancestor is a referral.
     */
    @Test 
    public void testAncestorReferral() throws Exception
    {
        LOG.debug( "" );

        LDAPConnection conn = getWiredConnection( ldapServer );
        LDAPConstraints constraints = new LDAPConstraints();
        conn.setConstraints( constraints );

        // referrals failure
        LDAPAttributeSet attrSet = new LDAPAttributeSet();
        attrSet.add( new LDAPAttribute( "objectClass", "organizationalUnit" ) );
        attrSet.add( new LDAPAttribute( "ou", "UnderReferral" ) );
        LDAPEntry entry = new LDAPEntry( "ou=UnderReferral,ou=Computers,uid=akarasuluref,ou=users,ou=system", attrSet );
        
        LDAPResponseListener listener = conn.add( entry, null, constraints );
        LDAPResponse response = listener.getResponse();
        assertEquals( ResultCodeEnum.REFERRAL.getValue(), response.getResultCode() );

        assertEquals( "ldap://localhost:10389/ou=UnderReferral,ou=Computers,uid=akarasulu,ou=users,ou=system", 
            response.getReferrals()[0] );
        assertEquals( "ldap://foo:10389/ou=UnderReferral,ou=Computers,uid=akarasulu,ou=users,ou=system", 
            response.getReferrals()[1] );
        assertEquals( "ldap://bar:10389/ou=UnderReferral,ou=Computers,uid=akarasulu,ou=users,ou=system", 
            response.getReferrals()[2] );

        conn.disconnect();
    }

    
    /**
     * Tests add operation on normal and referral entries without the 
     * ManageDsaIT control. Referrals are sent back to the client with a
     * non-success result code.
     */
    @Test
    public void testOnReferral() throws Exception
    {
        LDAPConnection conn = getWiredConnection( ldapServer );
        LDAPConstraints constraints = new LDAPConstraints();
        constraints.setReferrals( false );
        conn.setConstraints( constraints );
        
        // referrals failure

        LDAPAttributeSet attrSet = new LDAPAttributeSet();
        attrSet.add( new LDAPAttribute( "objectClass", "organizationalUnit" ) );
        attrSet.add( new LDAPAttribute( "ou", "UnderReferral" ) );
        LDAPEntry entry = new LDAPEntry( "ou=UnderReferral,uid=akarasuluref,ou=users,ou=system", attrSet );
        
        LDAPResponseListener listener = null;
        LDAPResponse response = null;
        listener = conn.add( entry, null, constraints );
        response = listener.getResponse();

        assertEquals( ResultCodeEnum.REFERRAL.getValue(), response.getResultCode() );

        assertEquals( "ldap://localhost:10389/ou=UnderReferral,uid=akarasulu,ou=users,ou=system", response.getReferrals()[0] );
        assertEquals( "ldap://foo:10389/ou=UnderReferral,uid=akarasulu,ou=users,ou=system", response.getReferrals()[1] );
        assertEquals( "ldap://bar:10389/ou=UnderReferral,uid=akarasulu,ou=users,ou=system", response.getReferrals()[2] );

        conn.disconnect();
    }
    
    
    /**
     * Tests add operation on normal and referral entries without the 
     * ManageDsaIT control using JNDI instead of the Netscape API. Referrals 
     * are sent back to the client with a non-success result code.
     */
    @Test
    public void testThrowOnReferralWithJndi() throws Exception
    {
        LdapContext ctx = getWiredContextThrowOnRefferal( ldapServer );
        SearchControls controls = new SearchControls();
        controls.setReturningAttributes( new String[0] );
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        
        // add failure
        Attributes attrs = new AttributesImpl( "objectClass", "organizationalUnit" );
        attrs.put( "ou", "UnderReferral" );
        
        try
        {
            ctx.createSubcontext( "ou=UnderReferral,uid=akarasuluref,ou=users,ou=system", attrs );
            fail( "Should never get here: add should fail with ReferralExcpetion" );
        }
        catch( ReferralException e )
        {
            assertEquals( "ldap://localhost:10389/ou=UnderReferral,uid=akarasulu,ou=users,ou=system", e.getReferralInfo() );
        }

        ctx.close();
    }
}
