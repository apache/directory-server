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


import static org.apache.directory.server.integ.ServerIntegrationUtils.getAdminConnection;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredConnection;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContext;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContextThrowOnRefferal;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.PartialResultException;
import javax.naming.ReferralException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InvalidAttributeValueException;
import javax.naming.directory.SchemaViolationException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;

import netscape.ldap.LDAPException;

import org.apache.directory.api.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.csn.Csn;
import org.apache.directory.api.ldap.model.csn.CsnFactory;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.exception.LdapNoSuchAttributeException;
import org.apache.directory.api.ldap.model.exception.LdapOperationException;
import org.apache.directory.api.ldap.model.ldif.LdifUtils;
import org.apache.directory.api.ldap.model.message.AddRequest;
import org.apache.directory.api.ldap.model.message.AddRequestImpl;
import org.apache.directory.api.ldap.model.message.AddResponse;
import org.apache.directory.api.ldap.model.message.Control;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.controls.ManageDsaIT;
import org.apache.directory.api.ldap.model.message.controls.ManageDsaITImpl;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.Strings;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.jndi.ServerLdapContext;
import org.apache.directory.server.integ.ServerIntegrationUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Various add scenario tests.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(allowAnonAccess = true, name = "AddIT-class", partitions =
    {
        @CreatePartition(
            name = "example",
            suffix = "dc=example,dc=com",
            contextEntry = @ContextEntry(
                entryLdif = "dn: dc=example,dc=com\n" +
                    "dc: example\n" +
                    "objectClass: top\n" +
                    "objectClass: domain\n\n"),
            indexes =
                {
                    @CreateIndex(attribute = "objectClass"),
                    @CreateIndex(attribute = "dc"),
                    @CreateIndex(attribute = "ou")
            }),

        @CreatePartition(
            name = "directory",
            suffix = "dc=directory,dc=apache,dc=org",
            contextEntry = @ContextEntry(
                entryLdif = "dn: dc=directory,dc=apache,dc=org\n" +
                    "dc: directory\n" +
                    "objectClass: top\n" +
                    "objectClass: domain\n\n"),
            indexes =
                {
                    @CreateIndex(attribute = "objectClass"),
                    @CreateIndex(attribute = "dc"),
                    @CreateIndex(attribute = "ou")
            }) })
@CreateLdapServer(name = "ADDIT", transports =
    { @CreateTransport(protocol = "LDAP", port = -1) })
@ApplyLdifs(
    {
        // Entry # 0
        "dn: cn=The Person,ou=system",
        "objectClass: person",
        "objectClass: top",
        "cn: The Person",
        "description: this is a person",
        "sn: Person",

        // Entry # 1
        "dn: uid=akarasulu,ou=users,ou=system",
        "objectClass: uidObject",
        "objectClass: person",
        "objectClass: top",
        "uid: akarasulu",
        "cn: Alex Karasulu",
        "sn: karasulu",

        // Entry # 2
        "dn: ou=Computers,uid=akarasulu,ou=users,ou=system",
        "objectClass: organizationalUnit",
        "objectClass: top",
        "ou: computers",
        "description: Computers for Alex",
        "seeAlso: ou=Machines,uid=akarasulu,ou=users,ou=system",

        // Entry # 3
        "dn: uid=akarasuluref,ou=users,ou=system",
        "objectClass: uidObject",
        "objectClass: referral",
        "objectClass: top",
        "uid: akarasuluref",
        "ref: ldap://localhost:10389/uid=akarasulu,ou=users,ou=system",
        "ref: ldap://foo:10389/uid=akarasulu,ou=users,ou=system",
        "ref: ldap://bar:10389/uid=akarasulu,ou=users,ou=system" })
public class AddIT extends AbstractLdapTestUnit
{
    private static final Logger LOG = LoggerFactory.getLogger( AddIT.class );
    private static final String RDN = "cn=The Person";

    private static final String BASE = "ou=system";
    private static final String BASE_EXAMPLE_COM = "dc=example,dc=com";
    private static final String BASE_DIRECTORY_APACHE_ORG = "dc=directory,dc=apache,dc=org";


    /**
     * This is the original defect as in JIRA DIREVE-216.
     * 
     * @throws NamingException if we cannot connect and perform add operations
     */
    @Test
    public void testAddObjectClasses() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );

        // modify object classes, add two more
        Attributes attributes = LdifUtils.createJndiAttributes( "objectClass: organizationalPerson",
            "objectClass: inetOrgPerson" );

        DirContext person = ( DirContext ) ctx.lookup( RDN );
        person.modifyAttributes( "", DirContext.ADD_ATTRIBUTE, attributes );

        // Read again from directory
        person = ( DirContext ) ctx.lookup( RDN );
        attributes = person.getAttributes( "" );
        javax.naming.directory.Attribute newOcls = attributes.get( "objectClass" );

        String[] expectedOcls =
            { "top", "person", "organizationalPerson", "inetOrgPerson" };

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
        DirContext ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );

        String newDescription = "More info on the user ...";

        // modify object classes, add two more
        Attributes attributes = new BasicAttributes( true );
        javax.naming.directory.Attribute desc = new BasicAttribute( "description", newDescription );
        attributes.put( desc );

        DirContext person = ( DirContext ) ctx.lookup( RDN );
        person.modifyAttributes( "", DirContext.REPLACE_ATTRIBUTE, attributes );

        // Read again from directory
        person = ( DirContext ) ctx.lookup( RDN );
        attributes = person.getAttributes( "" );
        javax.naming.directory.Attribute newDesc = attributes.get( "description" );

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
        DirContext ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );

        // person without sn
        Attributes attrs = new BasicAttributes( true );
        javax.naming.directory.Attribute ocls = new BasicAttribute( "objectClass" );
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
        LdapConnection con = getAdminConnection( getLdapServer() );

        String dn = "cn=Kate Bush," + BASE;
        Entry kate = new DefaultEntry( dn );

        kate.add( "objectclass", "top", "person" );
        kate.add( "sn", "Bush" );
        kate.add( "cn", "Kate Bush" );

        String descr[] =
            { "a British singer-songwriter with an expressive four-octave voice",
                "one of the most influential female artists of the twentieth century" };

        kate.add( "description", descr );

        con.add( kate );

        // Analyze entry and description attribute
        Entry kateReloaded = con.lookup( dn );
        assertNotNull( kateReloaded );
        Attribute attr = kateReloaded.get( "description" );
        assertNotNull( attr );
        assertEquals( 2, attr.size() );

        // Remove entry
        con.delete( dn );
        con.unBind();
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
        LdapConnection con = getAdminConnection( getLdapServer() );

        String dn = "cn=Kate Bush," + BASE;
        Entry kate = new DefaultEntry( dn );
        kate.add( "objectclass", "top", "person" );
        kate.add( "sn", "Bush" );
        kate.add( "cn", "Kate Bush" );

        String descr[] =
            { "a British singer-songwriter with an expressive four-octave voice",
                "one of the most influential female artists of the twentieth century" };

        kate.add( "description", descr[0] );
        kate.add( "description", descr[1] );

        con.add( kate );

        // Analyze entry and description attribute
        Entry kateReloaded = con.lookup( dn );
        assertNotNull( kateReloaded );
        Attribute attr = kateReloaded.get( "description" );
        assertNotNull( attr );
        assertEquals( 2, attr.size() );

        // Remove entry
        con.delete( dn );
        con.unBind();
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
        LdapConnection con = getAdminConnection( getLdapServer() );

        String dn = "cn=Kate Bush," + BASE;
        Entry kate = new DefaultEntry( dn );

        kate.add( "objectclass", "top", "person" );
        kate.add( "sn", "Bush" );

        String descr[] =
            { "a British singer-songwriter with an expressive four-octave voice",
                "one of the most influential female artists of the twentieth century" };

        kate.add( "description", descr[0] );
        kate.add( "cn", "Kate Bush" );
        kate.add( "description", descr[1] );

        con.add( kate );

        // Analyze entry and description attribute
        Entry kateReloaded = con.lookup( dn );
        assertNotNull( kateReloaded );
        Attribute attr = kateReloaded.get( "description" );
        assertNotNull( attr );
        assertEquals( 2, attr.size() );

        // Remove entry
        con.delete( dn );
        con.unBind();
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
        DirContext ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );

        // add inetOrgPerson with two displayNames
        Attributes attrs = new BasicAttributes( true );
        javax.naming.directory.Attribute ocls = new BasicAttribute( "objectClass" );
        ocls.add( "top" );
        ocls.add( "inetOrgPerson" );
        attrs.put( ocls );
        attrs.put( "cn", "Fiona Apple" );
        attrs.put( "sn", "Apple" );
        javax.naming.directory.Attribute displayName = new BasicAttribute( "displayName" );
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
        DirContext ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );

        // Create entry
        Attributes entry = new BasicAttributes( true );
        javax.naming.directory.Attribute entryOcls = new BasicAttribute( SchemaConstants.OBJECT_CLASS_AT );
        entryOcls.add( SchemaConstants.TOP_OC );
        entryOcls.add( SchemaConstants.ORGANIZATIONAL_UNIT_OC );
        entry.put( entryOcls );
        entry.put( SchemaConstants.OU_AT, "favorite" );
        String entryRdn = "ou=favorite";
        ctx.createSubcontext( entryRdn, entry );

        // Create Alias
        String aliasedObjectName = entryRdn + "," + ctx.getNameInNamespace();
        Attributes alias = new BasicAttributes( true );
        javax.naming.directory.Attribute aliasOcls = new BasicAttribute( SchemaConstants.OBJECT_CLASS_AT );
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
        DirContext ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );

        // Create container
        Attributes container = new BasicAttributes( true );
        javax.naming.directory.Attribute containerOcls = new BasicAttribute( "ObjectClass" );
        containerOcls.add( "top" );
        containerOcls.add( "organizationalUnit" );
        container.put( containerOcls );
        container.put( "ou", "Fruits" );
        String containerRdn = "ou=Fruits";
        DirContext containerCtx = ctx.createSubcontext( containerRdn, container );

        // Create entry
        Attributes entry = new BasicAttributes( true );
        javax.naming.directory.Attribute entryOcls = new BasicAttribute( "ObjectClass" );
        entryOcls.add( "top" );
        entryOcls.add( "organizationalUnit" );
        entry.put( entryOcls );
        entry.put( "ou", "favorite" );
        String entryRdn = "ou=favorite";
        containerCtx.createSubcontext( entryRdn, entry );

        // Create alias ou=bestFruit,ou=Fruits to entry ou=favorite,ou=Fruits
        String aliasedObjectName = entryRdn + "," + containerCtx.getNameInNamespace();
        Attributes alias = new BasicAttributes( true );
        javax.naming.directory.Attribute aliasOcls = new BasicAttribute( "ObjectClass" );
        aliasOcls.add( "top" );
        aliasOcls.add( "extensibleObject" );
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
        Set<String> names = new HashSet<String>();
        NamingEnumeration<SearchResult> ne = containerCtx.search( "", "(objectClass=*)", controls );
        assertTrue( ne.hasMore() );
        SearchResult sr = ne.next();
        names.add( sr.getName() );
        assertTrue( ne.hasMore() );
        sr = ne.next();
        names.add( sr.getName() );
        assertFalse( ne.hasMore() );
        assertEquals( 2, names.size() );
        assertTrue( names.contains( "ou=favorite" ) );
        assertTrue( names.contains( "ou=bestFruit" ) );

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
        assertEquals( "ldap://" + InetAddress.getLocalHost().getHostName() + ":" + getLdapServer().getPort()
            + "/ou=favorite,ou=Fruits,ou=system", sr.getName() );
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
        DirContext ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );

        // Create entry ou=favorite,ou=system
        Attributes entry = new BasicAttributes( true );
        javax.naming.directory.Attribute entryOcls = new BasicAttribute( SchemaConstants.OBJECT_CLASS_AT );
        entryOcls.add( SchemaConstants.TOP_OC );
        entryOcls.add( SchemaConstants.ORGANIZATIONAL_UNIT_OC );
        entry.put( entryOcls );
        entry.put( SchemaConstants.OU_AT, "favorite" );
        String entryRdn = "ou=favorite";
        ctx.createSubcontext( entryRdn, entry );

        // Create Alias ou=bestFruit,ou=system to ou=favorite
        String aliasedObjectName = entryRdn + "," + ctx.getNameInNamespace();
        Attributes alias = new BasicAttributes( true );
        javax.naming.directory.Attribute aliasOcls = new BasicAttribute( SchemaConstants.OBJECT_CLASS_AT );
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
     * Test for DIRSERVER-1352:  Infinite Loop when deleting an alias with suffix size > 1
     * Test for DIRSERVER-1157:  Deleting Alias entry failure
     * 
     * @see https://issues.apache.org/jira/browse/DIRSERVER-1352
     * @see https://issues.apache.org/jira/browse/DIRSERVER-1157
     * @throws Exception
     */
    @Test
    public void testAddDeleteAlias2() throws Exception
    {
        // use a partition with suffix size 2
        DirContext ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE_EXAMPLE_COM );

        // Create entry ou=favorite,dc=example,dc=com
        Attributes entry = new BasicAttributes( true );
        javax.naming.directory.Attribute entryOcls = new BasicAttribute( SchemaConstants.OBJECT_CLASS_AT );
        entryOcls.add( SchemaConstants.TOP_OC );
        entryOcls.add( SchemaConstants.ORGANIZATIONAL_UNIT_OC );
        entry.put( entryOcls );
        entry.put( SchemaConstants.OU_AT, "favorite" );
        String entryRdn = "ou=favorite";
        ctx.createSubcontext( entryRdn, entry );

        // Create Alias ou=bestFruit,dc=example,dc=com to ou=favorite
        String aliasedObjectName = entryRdn + "," + ctx.getNameInNamespace();
        Attributes alias = new BasicAttributes( true );
        javax.naming.directory.Attribute aliasOcls = new BasicAttribute( SchemaConstants.OBJECT_CLASS_AT );
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
     * Test for DIRSERVER-1352:  Infinite Loop when deleting an alias with suffix size > 1
     * Test for DIRSERVER-1157:  Deleting Alias entry failure
     * 
     * @see https://issues.apache.org/jira/browse/DIRSERVER-1352
     * @see https://issues.apache.org/jira/browse/DIRSERVER-1157
     * @throws Exception
     */
    @Test
    public void testAddDeleteAlias3() throws Exception
    {
        // use a partition with suffix size 3
        DirContext ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE_DIRECTORY_APACHE_ORG );

        // Create entry ou=favorite,dc=directory,dc=apache,dc=org
        Attributes entry = new BasicAttributes( true );
        javax.naming.directory.Attribute entryOcls = new BasicAttribute( SchemaConstants.OBJECT_CLASS_AT );
        entryOcls.add( SchemaConstants.TOP_OC );
        entryOcls.add( SchemaConstants.ORGANIZATIONAL_UNIT_OC );
        entry.put( entryOcls );
        entry.put( SchemaConstants.OU_AT, "favorite" );
        String entryRdn = "ou=favorite";
        ctx.createSubcontext( entryRdn, entry );

        // Create Alias ou=bestFruit,dc=directory,dc=apache,dc=org to ou=favorite
        String aliasedObjectName = entryRdn + "," + ctx.getNameInNamespace();
        Attributes alias = new BasicAttributes( true );
        javax.naming.directory.Attribute aliasOcls = new BasicAttribute( SchemaConstants.OBJECT_CLASS_AT );
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
        LdapConnection conn = getWiredConnection( getLdapServer() );

        AddRequest addRequest = new AddRequestImpl();
        ManageDsaIT manageDSAIT = new ManageDsaITImpl();
        manageDSAIT.setCritical( true );
        addRequest.addControl( manageDSAIT );

        // add success
        Entry entry = new DefaultEntry( "ou=UnderReferral,uid=akarasuluref,ou=users,ou=system",
            "objectClass", "organizationalUnit",
            "ou", "UnderReferral" );

        addRequest.setEntry( entry );

        AddResponse addResponse = conn.add( addRequest );
        assertEquals( ResultCodeEnum.REFERRAL, addResponse.getLdapResult().getResultCode() );

        assertNull( conn.lookup( "ou=UnderReferral,uid=akarasuluref,ou=users,ou=system", new Control[]
            { manageDSAIT } ) );

        conn.close();
    }


    public static LdapContext getContext( String principalDn, DirectoryService service, String dn ) throws Exception
    {
        if ( principalDn == null )
        {
            principalDn = "";
        }

        Dn userDn = new Dn( service.getSchemaManager(), principalDn );
        LdapPrincipal principal = new LdapPrincipal( service.getSchemaManager(), userDn, AuthenticationLevel.SIMPLE );

        if ( dn == null )
        {
            dn = "";
        }

        CoreSession session = service.getSession( principal );
        LdapContext ctx = new ServerLdapContext( service, session, new LdapName( dn ) );
        return ctx;
    }


    /**
     * Tests add operation on referral entry with the ManageDsaIT control.
     */
    @Test
    public void testOnReferralWitJNDIIgnore() throws Exception
    {
        LdapContext MNNCtx = getContext( ServerDNConstants.ADMIN_SYSTEM_DN, getLdapServer().getDirectoryService(),
            "uid=akarasuluref,ou=users,ou=system" );

        // Set to 'ignore'
        MNNCtx.addToEnvironment( Context.REFERRAL, "ignore" );

        try
        {
            // JNDI entry
            Attributes userEntry = new BasicAttributes( "objectClass", "top", true );
            userEntry.get( "objectClass" ).add( "person" );
            userEntry.put( "sn", "elecharny" );
            userEntry.put( "cn", "Emmanuel Lecharny" );

            MNNCtx.createSubcontext( "cn=Emmanuel Lecharny, ou=apache, ou=people", userEntry );
            fail();
        }
        catch ( PartialResultException pre )
        {
            assertTrue( true );
        }
    }


    /**
     * Tests referral handling when an ancestor is a referral.
     */
    @Test
    public void testAncestorReferral() throws Exception
    {
        LOG.debug( "" );

        LdapConnection conn = getWiredConnection( getLdapServer() );

        AddRequest addRequest = new AddRequestImpl();

        // referrals failure
        Entry entry = new DefaultEntry(
            "ou=UnderReferral,ou=Computers,uid=akarasuluref,ou=users,ou=system",
            "objectClass", "organizationalUnit",
            "ou", "UnderReferral" );
        addRequest.setEntry( entry );

        AddResponse addResponse = conn.add( addRequest );

        assertEquals( ResultCodeEnum.REFERRAL, addResponse.getLdapResult().getResultCode() );

        assertTrue( addResponse.getLdapResult().getReferral().getLdapUrls()
            .contains( "ldap://localhost:10389/ou=UnderReferral,ou=Computers,uid=akarasulu,ou=users,ou=system" ) );
        assertTrue( addResponse.getLdapResult().getReferral().getLdapUrls()
            .contains( "ldap://foo:10389/ou=UnderReferral,ou=Computers,uid=akarasulu,ou=users,ou=system" ) );
        assertTrue( addResponse.getLdapResult().getReferral().getLdapUrls()
            .contains( "ldap://bar:10389/ou=UnderReferral,ou=Computers,uid=akarasulu,ou=users,ou=system" ) );

        conn.close();
    }


    /**
     * Tests add operation on normal and referral entries without the
     * ManageDsaIT control. Referrals are sent back to the client with a
     * non-success result code.
     */
    @Test
    public void testOnReferral() throws Exception
    {
        LdapConnection conn = getWiredConnection( getLdapServer() );

        AddRequest addRequest = new AddRequestImpl();
        ManageDsaIT manageDSAIT = new ManageDsaITImpl();
        manageDSAIT.setCritical( true );
        //addRequest.addControl( manageDSAIT );

        // referrals failure
        Entry entry = new DefaultEntry(
            "ou=UnderReferral,uid=akarasuluref,ou=users,ou=system",
            "objectClass", "organizationalUnit",
            "ou", "UnderReferral" );

        addRequest.setEntry( entry );

        AddResponse addResponse = conn.add( addRequest );

        assertEquals( ResultCodeEnum.REFERRAL, addResponse.getLdapResult().getResultCode() );

        assertTrue( addResponse.getLdapResult().getReferral().getLdapUrls()
            .contains( "ldap://localhost:10389/ou=UnderReferral,uid=akarasulu,ou=users,ou=system" ) );
        assertTrue( addResponse.getLdapResult().getReferral().getLdapUrls()
            .contains( "ldap://foo:10389/ou=UnderReferral,uid=akarasulu,ou=users,ou=system" ) );
        assertTrue( addResponse.getLdapResult().getReferral().getLdapUrls()
            .contains( "ldap://bar:10389/ou=UnderReferral,uid=akarasulu,ou=users,ou=system" ) );

        conn.close();
    }


    /**
     * Tests add operation on normal and referral entries without the
     * ManageDsaIT control using JNDI instead of the Netscape API. Referrals
     * are sent back to the client with a non-success result code.
     */
    @Test
    public void testThrowOnReferralWithJndi() throws Exception
    {
        LdapContext ctx = getWiredContextThrowOnRefferal( getLdapServer() );
        SearchControls controls = new SearchControls();
        controls.setReturningAttributes( new String[0] );
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );

        // add failure
        Attributes attrs = new BasicAttributes( "objectClass", "organizationalUnit", true );
        attrs.put( "ou", "UnderReferral" );

        try
        {
            ctx.createSubcontext( "ou=UnderReferral,uid=akarasuluref,ou=users,ou=system", attrs );
            fail( "Should never get here: add should fail with ReferralExcpetion" );
        }
        catch ( ReferralException e )
        {
            assertEquals( "ldap://localhost:10389/ou=UnderReferral,uid=akarasulu,ou=users,ou=system", e
                .getReferralInfo() );
        }

        ctx.close();
    }


    /**
     * Test for DIRSERVER-1183.
     * 
     * @see https://issues.apache.org/jira/browse/DIRSERVER-1183
     * @throws Exception
     */
    @Test
    public void testDIRSERVER_1183() throws Exception
    {
        LdapContext ctx = ( LdapContext ) getWiredContext( getLdapServer() ).lookup( BASE );
        Attributes attrs = new BasicAttributes( "objectClass", "inetOrgPerson", true );
        attrs.get( "objectClass" ).add( "organizationalPerson" );
        attrs.get( "objectClass" ).add( "person" );
        attrs.put( "givenName", "Jim" );
        attrs.put( "sn", "Bean" );
        attrs.put( "cn", "Jim, Bean" );

        DirContext jimBeanCtx = ctx.createSubcontext( "cn=\"Jim, Bean\"", attrs );

        assertNotNull( jimBeanCtx );
    }


    /**
     * Create an entry a Rdn which is not present in the entry
     */
    @Test
    public void testAddEntryNoRDNInEntry() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );

        // Create a person
        Attributes person = new BasicAttributes( "objectClass", "inetOrgPerson", true );
        person.get( "objectClass" ).add( "top" );
        person.get( "objectClass" ).add( "person" );
        person.get( "objectClass" ).add( "organizationalPerson" );
        person.put( "sn", "Michael Jackson" );
        person.put( "cn", "Jackson" );

        DirContext michaelCtx = ctx.createSubcontext( "givenname=Michael", person );

        assertNotNull( michaelCtx );

        DirContext jackson = ( DirContext ) ctx.lookup( "givenname=Michael" );
        person = jackson.getAttributes( "" );
        javax.naming.directory.Attribute newOcls = person.get( "objectClass" );

        String[] expectedOcls =
            { "top", "person", "organizationalPerson", "inetOrgPerson" };

        for ( String name : expectedOcls )
        {
            assertTrue( "object class " + name + " is present", newOcls.contains( name ) );
        }

        javax.naming.directory.Attribute givenName = person.get( "givenname" );

        assertEquals( "Michael", givenName.get() );
    }


    /**
     * Create an entry a Rdn which is not present in the entry, but
     * with another attribute's value
     */
    @Test
    public void testAddEntryDifferentRDNInEntry() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );

        // Create a person
        Attributes person = new BasicAttributes( "objectClass", "inetOrgPerson", true );
        person.get( "objectClass" ).add( "top" );
        person.get( "objectClass" ).add( "person" );
        person.get( "objectClass" ).add( "organizationalPerson" );
        person.put( "givenName", "Michael" );
        person.put( "sn", "Michael Jackson" );
        person.put( "cn", "Jackson" );

        DirContext michaelCtx = ctx.createSubcontext( "cn=Michael", person );

        assertNotNull( michaelCtx );

        DirContext jackson = ( DirContext ) ctx.lookup( "cn=Michael" );
        person = jackson.getAttributes( "" );
        javax.naming.directory.Attribute newOcls = person.get( "objectClass" );

        String[] expectedOcls =
            { "top", "person", "organizationalPerson", "inetOrgPerson" };

        for ( String name : expectedOcls )
        {
            assertTrue( "object class " + name + " is present", newOcls.contains( name ) );
        }

        javax.naming.directory.Attribute cn = person.get( "cn" );

        assertEquals( 2, cn.size() );
        String[] expectedCns =
            { "Jackson", "Michael" };

        for ( String name : expectedCns )
        {
            assertTrue( "CN " + name + " is present", cn.contains( name ) );
        }
    }


    /**
     * Create an entry a Rdn which is not present in the entry,
     * with another attribute's value, and on a SingleValued attribute
     */
    @Test
    public void testAddEntryDifferentRDNSingleValuedInEntry() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );

        // Create a person
        Attributes person = new BasicAttributes( "objectClass", "inetOrgPerson", true );
        person.get( "objectClass" ).add( "top" );
        person.get( "objectClass" ).add( "person" );
        person.get( "objectClass" ).add( "organizationalPerson" );
        person.put( "displayName", "Michael" );
        person.put( "sn", "Michael Jackson" );
        person.put( "cn", "Jackson" );

        DirContext michaelCtx = ctx.createSubcontext( "displayName=test", person );

        assertNotNull( michaelCtx );

        DirContext jackson = ( DirContext ) ctx.lookup( "displayName=test" );
        person = jackson.getAttributes( "" );
        javax.naming.directory.Attribute newOcls = person.get( "objectClass" );

        String[] expectedOcls =
            { "top", "person", "organizationalPerson", "inetOrgPerson" };

        for ( String name : expectedOcls )
        {
            assertTrue( "object class " + name + " is present", newOcls.contains( name ) );
        }

        // Check that the displayName attribute has been replaced
        javax.naming.directory.Attribute displayName = person.get( "displayName" );

        assertEquals( 1, displayName.size() );
        assertTrue( displayName.contains( "test" ) );
    }


    /**
     * Create an entry a composed Rdn which is not present in the entry,
     * with another attribute's value, and on a SingleValued attribute
     */
    @Test
    public void testAddEntryComposedRDN() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );

        // Create a person
        Attributes person = new BasicAttributes( "objectClass", "inetOrgPerson", true );
        person.get( "objectClass" ).add( "top" );
        person.get( "objectClass" ).add( "person" );
        person.get( "objectClass" ).add( "organizationalPerson" );
        person.put( "sn", "Michael Jackson" );
        person.put( "cn", "Jackson" );

        DirContext michaelCtx = ctx.createSubcontext( "displayName=test+cn=Michael", person );

        assertNotNull( michaelCtx );

        DirContext jackson = ( DirContext ) ctx.lookup( "displayName=test+cn=Michael" );
        person = jackson.getAttributes( "" );
        javax.naming.directory.Attribute newOcls = person.get( "objectClass" );

        String[] expectedOcls =
            { "top", "person", "organizationalPerson", "inetOrgPerson" };

        for ( String name : expectedOcls )
        {
            assertTrue( "object class " + name + " is present", newOcls.contains( name ) );
        }

        // Check that the DIsplayName attribute has been added
        javax.naming.directory.Attribute displayName = person.get( "displayName" );

        assertEquals( 1, displayName.size() );
        assertTrue( displayName.contains( "test" ) );

        // Check that the cn attribute value has been added
        javax.naming.directory.Attribute cn = person.get( "cn" );

        assertEquals( 2, cn.size() );
        assertTrue( cn.contains( "Jackson" ) );
        assertTrue( cn.contains( "Michael" ) );
    }


    /**
     * Test that if we inject a PDU above the max allowed size,
     * the connection is closed.
     * 
     * @throws NamingException
     */
    @Test
    public void testAddPDUExceedingMaxSizeJNDI() throws Exception
    {
        // Limit the PDU size to 1024
        getLdapServer().getDirectoryService().setMaxPDUSize( 1024 );
        DirContext ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );

        // modify object classes, add two more
        Attributes attributes = new BasicAttributes( true );
        javax.naming.directory.Attribute ocls = new BasicAttribute( "description" );

        // Inject a 1024 bytes long description
        StringBuilder sb = new StringBuilder();

        for ( int i = 0; i < 128; i++ )
        {
            sb.append( "0123456789ABCDEF" );
        }

        ocls.add( sb.toString() );
        attributes.put( ocls );

        DirContext person = ( DirContext ) ctx.lookup( RDN );

        try
        {
            person.modifyAttributes( "", DirContext.ADD_ATTRIBUTE, attributes );
            fail();
        }
        catch ( Exception e )
        {
            // We are expecting the session to be close here.
        }

        // Test again with a bigger size
        // Limit the PDU size to 1024
        getLdapServer().getDirectoryService().setMaxPDUSize( 4096 );

        ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );
        person = ( DirContext ) ctx.lookup( RDN );

        try
        {
            person.modifyAttributes( "", DirContext.ADD_ATTRIBUTE, attributes );
        }
        catch ( Exception e )
        {
            // We should not go there
            fail();
        }

        // Read again from directory
        ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );
        person = ( DirContext ) ctx.lookup( RDN );

        assertNotNull( person );
        attributes = person.getAttributes( "" );
        javax.naming.directory.Attribute newOcls = attributes.get( "objectClass" );

        assertNotNull( newOcls );
    }


    /**
     * Test that if we inject a PDU above the max allowed size,
     * the connection is closed.
     * 
     * @throws NamingException
     */
    @Test
    public void testAddPDUExceedingMaxSizeLdapApi() throws Exception
    {
        // Limit the PDU size to 1024
        getLdapServer().getDirectoryService().setMaxPDUSize( 1024 );
        LdapConnection connection = new LdapNetworkConnection( InetAddress.getLocalHost().getHostName(), getLdapServer().getPort() );
        connection.bind( "uid=admin,ou=system", "secret" );

        // Inject a 1024 bytes long description
        StringBuilder sb = new StringBuilder();

        for ( int i = 0; i < 128; i++ )
        {
            sb.append( "0123456789ABCDEF" );
        }

        Attribute description = new DefaultAttribute( "description", sb.toString() );

        try
        {
            Modification modification = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, description );
            connection.modify( "cn=the person, ou=system", modification );
            fail();
        }
        catch ( Exception e )
        {
            // We are expecting the session to be close here.
            if ( connection.isConnected() )
            {
                // Race condition:
                // Upon NoticeOfDisconnection the API sends an abandon request but does not immediately close the connection.
                // So at this point it is not guaranteed that the connnection is already closed.
                // TODO: This is just a workaround, better check the connection for any outstanding abandon requests
                Thread.sleep( 1000 );
            }
            assertFalse( connection.isConnected() );
        }
    }


    /**
     * Test for DIRSERVER-1311: If the Rdn attribute+value is not present
     * in the entry the server should implicit add this attribute+value to
     * the entry. Additionally, if the Rdn value is escaped or a hexstring
     * the server must add the unescaped string or binary value to the entry.
     */
    @Test
    public void testAddUnescapedRdnValue_DIRSERVER_1311() throws Exception
    {
        LdapContext ctx = ( LdapContext ) getWiredContext( getLdapServer() ).lookup( BASE );

        Attributes tori = new BasicAttributes( true );
        javax.naming.directory.Attribute toriOC = new BasicAttribute( "objectClass" );
        toriOC.add( "top" );
        toriOC.add( "person" );
        tori.put( toriOC );
        //tori.put( "cn", "Amos,Tori" );
        tori.put( "cn", "Tori,Amos" );
        tori.put( "sn", "Amos" );
        /*
         * Note that the Rdn attribute is different to the cn specified in the entry.
         * This creates a second cn attribute "cn:Amos,Tori". This is a JNDI hack:
         * If no other cn is available in the entry, JNDI adds the Rdn
         * attribute to the entry before sending the request to the server.
         */
        ctx.createSubcontext( " cn = Amos\\,Tori ", tori );

        Attributes binary = new BasicAttributes( true );
        javax.naming.directory.Attribute binaryOC = new BasicAttribute( "objectClass" );
        binaryOC.add( "top" );
        binaryOC.add( "person" );
        binary.put( binaryOC );
        binary.put( "cn", "Binary" );
        binary.put( "sn", "Binary" );
        binary.put( "userPassword", "test" );
        /*
         * Note that the Rdn attribute is different to the userPassword specified
         * in the entry. This creates a second cn attribute "userPassword:#414243".
         * This is a JNDI hack:
         * If no other userPassword is available in the entry, JNDI adds the Rdn
         * attribute to the entry before sending the request to the server.
         */
        ctx.createSubcontext( " userPassword = #414243 ", binary );

        SearchControls controls = new SearchControls();
        NamingEnumeration<SearchResult> res;

        // search for the implicit added cn
        res = ctx.search( "", "(cn=Amos,Tori)", controls );
        assertTrue( res.hasMore() );
        javax.naming.directory.Attribute cnAttribute = res.next().getAttributes().get( "cn" );
        assertEquals( 2, cnAttribute.size() );
        assertTrue( cnAttribute.contains( "Tori,Amos" ) );
        assertTrue( cnAttribute.contains( "Amos,Tori" ) );
        assertFalse( res.hasMore() );

        // search for the implicit added userPassword
        res = ctx.search( "", "(userPassword=\\41\\42\\43)", controls );
        assertTrue( res.hasMore() );
        javax.naming.directory.Attribute userPasswordAttribute = res.next().getAttributes().get( "userPassword" );
        assertEquals( 2, userPasswordAttribute.size() );
        assertTrue( userPasswordAttribute.contains( Strings.getBytesUtf8( "test" ) ) );
        assertTrue( userPasswordAttribute.contains( Strings.getBytesUtf8( "ABC" ) ) );
        assertFalse( res.hasMore() );
    }


    @Test
    public void testAddEntryUUIDAndCSNAttributes() throws Exception
    {
        LdapConnection con = getAdminConnection( getLdapServer() );

        String dn = "cn=Kate Bush," + BASE;
        Entry entry = new DefaultEntry( dn );
        entry.add( "objectclass", "top", "person" );
        entry.add( "sn", "Bush" );
        entry.add( "cn", "Kate Bush" );

        String descr = "a British singer-songwriter with an expressive four-octave voice";
        entry.add( "description", descr );

        UUID uuid = UUID.randomUUID();
        entry.add( SchemaConstants.ENTRY_UUID_AT, uuid.toString() );

        CsnFactory csnFac = new CsnFactory( 0 );
        Csn csn = csnFac.newInstance();
        entry.add( SchemaConstants.ENTRY_CSN_AT, csn.toString() );

        con.add( entry );

        // Analyze entry and description attribute
        Entry addedEntry = con.lookup( dn, "*", "+" );
        assertNotNull( addedEntry );

        Attribute attr = addedEntry.get( SchemaConstants.ENTRY_UUID_AT );
        assertNotNull( attr );

        assertEquals( uuid.toString(), attr.getString() );

        attr = addedEntry.get( SchemaConstants.ENTRY_CSN_AT );
        assertNotNull( attr );
        assertEquals( csn.toString(), attr.getString() );

        // Remove entry
        con.delete( dn );
        con.unBind();
    }


    protected Attributes getPersonAttributes( String sn, String cn )
    {
        Attributes attrs = new BasicAttributes( true );
        javax.naming.directory.Attribute ocls = new BasicAttribute( "objectClass" );
        ocls.add( "top" );
        ocls.add( "person" );
        attrs.put( ocls );
        attrs.put( "cn", cn );
        attrs.put( "sn", sn );

        return attrs;
    }


    protected Attributes getOrgUnitAttributes( String ou )
    {
        Attributes attrs = new BasicAttributes( true );
        javax.naming.directory.Attribute ocls = new BasicAttribute( "objectClass" );
        ocls.add( "top" );
        ocls.add( "organizationalUnit" );
        attrs.put( ocls );
        attrs.put( "ou", ou );

        return attrs;
    }


    /**
     * <pre>
     * ou=system
     *   |--ou=sales
     *   |    |--cn=real  <--real entry
     *   |--ou=engineering
     *        |--cn=alias  <--alias, pointing to the real entry
     * </pre>
     * 
     * @throws NamingException
     */
    @Test
    @CreateDS(
        enableChangeLog = false,
        name = "DSAlias")
    @CreateLdapServer(name = "DSAlias", transports =
        { @CreateTransport(protocol = "LDAP", port = -1) })
    public void test_DIRSERVER_1357() throws Exception
    {
        DirContext ctx = ( DirContext ) ServerIntegrationUtils.getWiredContext( getLdapServer() ).lookup( "ou=system" );

        Attributes salesAttrs = getOrgUnitAttributes( "sales" );
        ctx.createSubcontext( "ou=sales", salesAttrs );

        Attributes engAttrs = getOrgUnitAttributes( "engineering" );
        ctx.createSubcontext( "ou=engineering", engAttrs );

        // The real entry under ou=sales
        Attributes fooAttrs = getPersonAttributes( "real", "real" );
        ctx.createSubcontext( "cn=real,ou=sales", fooAttrs );

        // The alias under ou=engineering, pointing to the real entry
        Attributes aliasAttrs = new BasicAttributes( true );
        javax.naming.directory.Attribute aliasOC = new BasicAttribute( "objectClass" );
        aliasOC.add( "top" );
        aliasOC.add( "alias" );
        aliasOC.add( "extensibleObject" );
        aliasAttrs.put( aliasOC );
        aliasAttrs.put( "cn", "alias" );
        aliasAttrs.put( "aliasedObjectName", "cn=real,ou=sales,ou=system" );
        ctx.createSubcontext( "cn=alias,ou=engineering", aliasAttrs );

        // Delete the real entry first
        ctx.destroySubcontext( "cn=real,ou=sales" );

        // Now the alias entry still exists, but points to nowhere.
        // When trying to delete the alias entry an exception occurs.
        ctx.destroySubcontext( "cn=alias,ou=engineering" );
    }


    /**
     * Adding an entry with a non existing attribute type.
     * 
     * @throws Exception
     */
    @Test
    public void testAddEntryNonExistingAT() throws Exception
    {
        LdapConnection connection = getAdminConnection( getLdapServer() );

        Dn dn = new Dn( "cn=Kate Bush," + BASE );

        Entry personEntry = new DefaultEntry();
        personEntry.add( SchemaConstants.OBJECT_CLASS_AT, "person" );
        personEntry.add( SchemaConstants.CN_AT, "Kate Bush" );
        personEntry.add( SchemaConstants.SN_AT, "Bush" );
        personEntry.add( "nonExistingAttribute", "value" );
        personEntry.setDn( dn );

        try
        {
            connection.add( personEntry );
            fail( "should throw LdapNoSuchAttributeException" );
        }
        catch ( LdapNoSuchAttributeException e )
        {
            //expected exception
        }

        Entry entry = connection.lookup( dn );
        assertNull( entry );

        connection.close();
    }


    /**
     * Adding an entry with a non existing attribute type.
     * 
     * @throws Exception
     */
    @Test(expected = LdapOperationException.class)
    public void testAddEntryNonExistingOC() throws Exception
    {
        LdapConnection connection = getAdminConnection( getLdapServer() );

        Dn dn = new Dn( "cn=Kate Bush," + BASE );

        Entry personEntry = new DefaultEntry();
        personEntry.add( SchemaConstants.OBJECT_CLASS_AT, "nonexistingOC" );
        personEntry.add( SchemaConstants.CN_AT, "Kate Bush" );
        personEntry.add( SchemaConstants.SN_AT, "Bush" );
        personEntry.setDn( dn );

        connection.add( personEntry );
    }


    /**
     * Adding an entry with a 100K attribute's value.
     * 
     * @throws Exception
     */
    @Test(expected = LdapException.class)
    public void testAddEntry100KData() throws Exception
    {
        LdapConnection connection = getAdminConnection( getLdapServer() );

        int size = 100 * 1024;
        byte[] dataBytes = new byte[size];

        for ( int i = 0; i < size; i++ )
        {
            dataBytes[i] = 'A';
        }

        String data = Strings.utf8ToString( dataBytes );

        Dn dn = new Dn( "cn=Kate Bush," + BASE );

        Entry personEntry = new DefaultEntry( "cn=Kate Bush," + BASE,
            "objectClass: top",
            "objectClass: person",
            "cn: Kate Bush",
            "sn: Bush",
            "description", data );

        connection.add( personEntry );

        // Check that the entry has been stored
        Entry entry = connection.lookup( dn, "description", "cn", "sn" );

        String description = entry.get( "description" ).getString();

        assertNotNull( description );
        assertTrue( description.startsWith( "AAA" ) );
        assertEquals( size, description.length() );

        for ( int i = 0; i < size; i++ )
        {
            assertEquals( 'A', description.charAt( i ) );
        }
    }


    /**
     * Test case to demonstrate DIRSERVER-1681
     * Create an entry with a certificate;binary value
     * 
     * @throws LDAPException if we fail to connect and add entries
     */
    @Test
    public void testAddWithCertificateBinary() throws Exception
    {
        LdapConnection con = getAdminConnection( getLdapServer() );
        con.loadSchema();

        String dn = "cn=Kate Bush," + BASE;
        Entry kate = new DefaultEntry( dn,
            "objectclass: top",
            "objectclass: person",
            "objectclass: inetOrgPerson",
            "userCertificate;binary:: PEhlbGxvIHdvcmxkICE+", // This is "<hello world !>"
            "sn: Bush",
            "cn: Kate Bush" );

        con.add( kate );

        // Analyze entry and description attribute
        Entry kateReloaded = con.lookup( dn );
        assertNotNull( kateReloaded );
        Attribute certificate = kateReloaded.get( "userCertificate;binary" );
        assertNotNull( certificate );
        assertEquals( 1, certificate.size() );
        assertTrue( certificate.contains( Strings.getBytesUtf8( "<Hello world !>" ) ) );

        // Same check without the ";binary"
        certificate = kateReloaded.get( "userCertificate" );
        assertNotNull( certificate );
        assertEquals( 1, certificate.size() );
        assertTrue( certificate.contains( Strings.getBytesUtf8( "<Hello world !>" ) ) );

        // Remove entry
        con.delete( dn );
        con.unBind();
    }


    @Test
    public void testAddNullValue() throws LdapException, IOException
    {
        LdapConnection connection = new LdapNetworkConnection( InetAddress.getLocalHost().getHostName(), getLdapServer().getPort() );
        connection.setTimeOut( 0L );

        // Use the client API
        connection.bind( "uid=admin,ou=system", "secret" );

        // Add a new entry with some null values
        Entry entry = new DefaultEntry( "cn=test,ou=system",
            "ObjectClass: top",
            "ObjectClass: person",
            "ObjectClass: person",
            "ObjectClass: OrganizationalPerson",
            "ObjectClass: inetOrgPerson",
            "cn: test",
            "sn: Test",
            "userPassword:",
            "mail:" );

        connection.add( entry );

        // Now fetch the entry
        Entry found = connection.lookup( "cn=test,ou=system" );

        assertNotNull( found );
        assertNotNull( found.get( "userPassword" ) );
        assertNotNull( found.get( "mail" ) );
        String userPassword = found.get( "userPassword" ).getString();
        String mail = found.get( "mail" ).getString();

        assertTrue( Strings.isEmpty( userPassword ) );
        assertTrue( Strings.isEmpty( mail ) );

        connection.close();
    }


    @Test
    public void testAddNullValueDirectoryString() throws LdapException, IOException
    {
        LdapConnection connection = new LdapNetworkConnection( InetAddress.getLocalHost().getHostName(), getLdapServer().getPort() );
        connection.setTimeOut( 0L );

        // Use the client API
        connection.bind( "uid=admin,ou=system", "secret" );

        // Add a new entry with some null values
        Entry entry = new DefaultEntry( "cn=test,ou=system",
            "ObjectClass: top",
            "ObjectClass: person",
            "ObjectClass: person",
            "ObjectClass: OrganizationalPerson",
            "ObjectClass: inetOrgPerson",
            "cn: test",
            "sn: Test",
            "displayName:" ); // The DisplayName must contain a value

        try
        {
            connection.add( entry );
            fail();
        }
        catch ( LdapInvalidAttributeValueException liave )
        {
            // Expected
        }

        connection.close();
    }
}
