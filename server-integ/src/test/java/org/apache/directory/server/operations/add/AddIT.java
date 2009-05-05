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


import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredConnection;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContext;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContextThrowOnRefferal;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.PartialResultException;
import javax.naming.ReferralException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
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

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.authn.LdapPrincipal;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.core.integ.annotations.Factory;
import org.apache.directory.server.core.jndi.ServerLdapContext;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.integ.LdapServerFactory;
import org.apache.directory.server.integ.SiRunner;
import org.apache.directory.server.ldap.LdapService;
import org.apache.directory.server.ldap.handlers.extended.StoredProcedureExtendedOperationHandler;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.StringTools;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Various add scenario tests.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 674593 $
 */
@RunWith ( SiRunner.class ) 
@CleanupLevel ( Level.CLASS )
@Factory ( AddIT.Factory.class )
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
    "ref: ldap://bar:10389/uid=akarasulu,ou=users,ou=system\n\n" +
    
    // Entry example.com
    "dn: dc=example,dc=com\n" +
    "dc: example\n" +
    "objectClass: top\n" +
    "objectClass: domain\n\n" +
    
    // Entry directory.apache.org
    "dn: dc=directory,dc=apache,dc=org\n" +
    "dc: directory\n" +
    "objectClass: top\n" +
    "objectClass: domain\n\n"
    }
)
public class AddIT
{
    private static final Logger LOG = LoggerFactory.getLogger( AddIT.class );
    private static final String RDN = "cn=The Person";

    private static final String BASE = "ou=system";
    private static final String BASE_EXAMPLE_COM = "dc=example,dc=com";
    private static final String BASE_DIRECTORY_APACHE_ORG = "dc=directory,dc=apache,dc=org";


    public static LdapService ldapService;

    
    /**
     * The factory
     *
     */
    public static class Factory implements LdapServerFactory
    {
        public LdapService newInstance() throws Exception
        {
            DirectoryService service = new DefaultDirectoryService();
            IntegrationUtils.doDelete( service.getWorkingDirectory() );
            service.getChangeLog().setEnabled( true );
            service.setAllowAnonymousAccess( true );
            service.setShutdownHookEnabled( false );

            JdbmPartition example = new JdbmPartition();
            example.setCacheSize( 500 );
            example.setSuffix( BASE_EXAMPLE_COM );
            example.setId( "example" );
            Set<Index<?, ServerEntry>> indexedAttrs = new HashSet<Index<?, ServerEntry>>();
            indexedAttrs.add( new JdbmIndex<String, ServerEntry>( "ou" ) );
            indexedAttrs.add( new JdbmIndex<String, ServerEntry>( "dc" ) );
            indexedAttrs.add( new JdbmIndex<String, ServerEntry>( "objectClass" ) );
            example.setIndexedAttributes( indexedAttrs );

            service.addPartition( example );

            JdbmPartition directory = new JdbmPartition();
            directory.setCacheSize( 500 );
            directory.setSuffix( BASE_DIRECTORY_APACHE_ORG );
            directory.setId( "directory" );
            Set<Index<?, ServerEntry>> indexedAttrs2 = new HashSet<Index<?, ServerEntry>>();
            indexedAttrs2.add( new JdbmIndex<String, ServerEntry>( "ou" ) );
            indexedAttrs2.add( new JdbmIndex<String, ServerEntry>( "dc" ) );
            indexedAttrs2.add( new JdbmIndex<String, ServerEntry>( "objectClass" ) );
            directory.setIndexedAttributes( indexedAttrs2 );
            
            service.addPartition( directory );
            
            // change the working directory to something that is unique
            // on the system and somewhere either under target directory
            // or somewhere in a temp area of the machine.

            LdapService ldapService = new LdapService();
            ldapService.setDirectoryService( service );
            int port = AvailablePortFinder.getNextAvailable( 1024 );
            ldapService.setTcpTransport( new TcpTransport( port ) );
            ldapService.setAllowAnonymousAccess( true );
            ldapService.addExtendedOperationHandler( new StoredProcedureExtendedOperationHandler() );

            return ldapService;
        }
    }

    
    /**
     * This is the original defect as in JIRA DIREVE-216.
     * 
     * @throws NamingException if we cannot connect and perform add operations
     */
    @Test
    public void testAddObjectClasses() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapService ).lookup( BASE );

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
        DirContext ctx = ( DirContext ) getWiredContext( ldapService ).lookup( BASE );

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
     * 
     * @throws NamingException if we fail to connect
     */
    @Test
    public void testAddWithMissingRequiredAttributes() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapService ).lookup( BASE );

        // person without sn
        Attributes attrs = new BasicAttributes( true );
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
        LDAPConnection con = getWiredConnection( ldapService );
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
        LDAPConnection con = getWiredConnection( ldapService );
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
        LDAPConnection con = getWiredConnection( ldapService );
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
        DirContext ctx = ( DirContext ) getWiredContext( ldapService ).lookup( BASE );
        
        // add inetOrgPerson with two displayNames
        Attributes attrs = new BasicAttributes( true );
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
     * 
     * @throws NamingException if we fail to connect and add entries
     */
    @Test
    public void testAddAlias() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapService ).lookup( BASE );

        // Create entry
        Attributes entry = new BasicAttributes( true );
        Attribute entryOcls = new BasicAttribute( SchemaConstants.OBJECT_CLASS_AT );
        entryOcls.add( SchemaConstants.TOP_OC );
        entryOcls.add( SchemaConstants.ORGANIZATIONAL_UNIT_OC );
        entry.put( entryOcls );
        entry.put( SchemaConstants.OU_AT, "favorite" );
        String entryRdn = "ou=favorite";
        ctx.createSubcontext( entryRdn, entry );

        // Create Alias
        String aliasedObjectName = entryRdn + "," + ctx.getNameInNamespace();
        Attributes alias = new BasicAttributes( true );
        Attribute aliasOcls = new BasicAttribute( SchemaConstants.OBJECT_CLASS_AT );
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
        DirContext ctx = ( DirContext ) getWiredContext( ldapService ).lookup( BASE );

        // Create container
        Attributes container = new BasicAttributes( true );
        Attribute containerOcls = new BasicAttribute( SchemaConstants.OBJECT_CLASS_AT );
        containerOcls.add( SchemaConstants.TOP_OC );
        containerOcls.add( SchemaConstants.ORGANIZATIONAL_UNIT_OC );
        container.put( containerOcls );
        container.put( SchemaConstants.OU_AT, "Fruits" );
        String containerRdn = "ou=Fruits";
        DirContext containerCtx = ctx.createSubcontext( containerRdn, container );

        // Create entry
        Attributes entry = new BasicAttributes( true );
        Attribute entryOcls = new BasicAttribute( SchemaConstants.OBJECT_CLASS_AT );
        entryOcls.add( SchemaConstants.TOP_OC );
        entryOcls.add( SchemaConstants.ORGANIZATIONAL_UNIT_OC );
        entry.put( entryOcls );
        entry.put( SchemaConstants.OU_AT, "favorite" );
        String entryRdn = "ou=favorite";
        containerCtx.createSubcontext( entryRdn, entry );

        // Create alias ou=bestFruit,ou=Fruits to entry ou=favorite,ou=Fruits
        String aliasedObjectName = entryRdn + "," + containerCtx.getNameInNamespace();
        Attributes alias = new BasicAttributes( true );
        Attribute aliasOcls = new BasicAttribute( SchemaConstants.OBJECT_CLASS_AT );
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
        assertEquals( "ldap://localhost:"+ ldapService.getPort() +"/ou=favorite,ou=Fruits,ou=system", sr.getName() );
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
        DirContext ctx = ( DirContext ) getWiredContext( ldapService ).lookup( BASE );

        // Create entry ou=favorite,ou=system
        Attributes entry = new BasicAttributes( true );
        Attribute entryOcls = new BasicAttribute( SchemaConstants.OBJECT_CLASS_AT );
        entryOcls.add( SchemaConstants.TOP_OC );
        entryOcls.add( SchemaConstants.ORGANIZATIONAL_UNIT_OC );
        entry.put( entryOcls );
        entry.put( SchemaConstants.OU_AT, "favorite" );
        String entryRdn = "ou=favorite";
        ctx.createSubcontext( entryRdn, entry );

        // Create Alias ou=bestFruit,ou=system to ou=favorite
        String aliasedObjectName = entryRdn + "," + ctx.getNameInNamespace();
        Attributes alias = new BasicAttributes( true );
        Attribute aliasOcls = new BasicAttribute( SchemaConstants.OBJECT_CLASS_AT );
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
        DirContext ctx = ( DirContext ) getWiredContext( ldapService ).lookup( BASE_EXAMPLE_COM );

        // Create entry ou=favorite,dc=example,dc=com
        Attributes entry = new BasicAttributes( true );
        Attribute entryOcls = new BasicAttribute( SchemaConstants.OBJECT_CLASS_AT );
        entryOcls.add( SchemaConstants.TOP_OC );
        entryOcls.add( SchemaConstants.ORGANIZATIONAL_UNIT_OC );
        entry.put( entryOcls );
        entry.put( SchemaConstants.OU_AT, "favorite" );
        String entryRdn = "ou=favorite";
        ctx.createSubcontext( entryRdn, entry );

        // Create Alias ou=bestFruit,dc=example,dc=com to ou=favorite
        String aliasedObjectName = entryRdn + "," + ctx.getNameInNamespace();
        Attributes alias = new BasicAttributes( true );
        Attribute aliasOcls = new BasicAttribute( SchemaConstants.OBJECT_CLASS_AT );
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
        DirContext ctx = ( DirContext ) getWiredContext( ldapService ).lookup( BASE_DIRECTORY_APACHE_ORG );

        // Create entry ou=favorite,dc=directory,dc=apache,dc=org
        Attributes entry = new BasicAttributes( true );
        Attribute entryOcls = new BasicAttribute( SchemaConstants.OBJECT_CLASS_AT );
        entryOcls.add( SchemaConstants.TOP_OC );
        entryOcls.add( SchemaConstants.ORGANIZATIONAL_UNIT_OC );
        entry.put( entryOcls );
        entry.put( SchemaConstants.OU_AT, "favorite" );
        String entryRdn = "ou=favorite";
        ctx.createSubcontext( entryRdn, entry );

        // Create Alias ou=bestFruit,dc=directory,dc=apache,dc=org to ou=favorite
        String aliasedObjectName = entryRdn + "," + ctx.getNameInNamespace();
        Attributes alias = new BasicAttributes( true );
        Attribute aliasOcls = new BasicAttribute( SchemaConstants.OBJECT_CLASS_AT );
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
        LDAPConnection conn = getWiredConnection( ldapService );
        LDAPConstraints constraints = new LDAPSearchConstraints();
        constraints.setClientControls( new LDAPControl( LDAPControl.MANAGEDSAIT, true, new byte[0] ) );
        constraints.setServerControls( new LDAPControl( LDAPControl.MANAGEDSAIT, true, new byte[0] ) );
        conn.setConstraints( constraints );
        
        // add success
        LDAPAttributeSet attrSet = new LDAPAttributeSet();
        attrSet.add( new LDAPAttribute( "objectClass", "organizationalUnit" ) );
        attrSet.add( new LDAPAttribute( "ou", "UnderReferral" ) );
        LDAPEntry entry = new LDAPEntry( "ou=UnderReferral,uid=akarasuluref,ou=users,ou=system", attrSet );
        
        try
        {
            conn.add( entry, constraints );
            fail();
        }
        catch ( LDAPException le )
        {
            assertEquals( 80, le.getLDAPResultCode() );
        }
        
        try
        {
            conn.read( "ou=UnderReferral,uid=akarasuluref,ou=users,ou=system", 
                ( LDAPSearchConstraints ) constraints );
            fail();
        }
        catch ( LDAPException le )
        {
            
        }
        
        conn.disconnect();
    }
    
    
    public static LdapContext getContext( String principalDn, DirectoryService service, String dn )
    throws Exception
    {
        if ( principalDn == null )
        {
            principalDn = "";
        }
        
        LdapDN userDn = new LdapDN( principalDn );
        userDn.normalize( service.getRegistries().getAttributeTypeRegistry().getNormalizerMapping() );
        LdapPrincipal principal = new LdapPrincipal( userDn, AuthenticationLevel.SIMPLE );
        
        if ( dn == null )
        {
            dn = "";
        }
        
        CoreSession session = service.getSession( principal );
        LdapContext ctx = new ServerLdapContext( service, session, new LdapDN( dn ) );
        return ctx;
    }
    
    
    /**
     * Tests add operation on referral entry with the ManageDsaIT control.
     */
    @Test
    public void testOnReferralWitJNDIIgnore() throws Exception
    {
        LdapContext MNNCtx = getContext( ServerDNConstants.ADMIN_SYSTEM_DN, ldapService.getDirectoryService(), "uid=akarasuluref,ou=users,ou=system" );

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

        LDAPConnection conn = getWiredConnection( ldapService );
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
        LDAPConnection conn = getWiredConnection( ldapService );
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
        LdapContext ctx = getWiredContextThrowOnRefferal( ldapService );
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
        catch( ReferralException e )
        {
            assertEquals( "ldap://localhost:10389/ou=UnderReferral,uid=akarasulu,ou=users,ou=system", e.getReferralInfo() );
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
        LdapContext ctx = ( LdapContext ) getWiredContext( ldapService ).lookup( BASE );
        Attributes attrs = new BasicAttributes( "objectClass", "inetOrgPerson", true );
        attrs.get( "objectClass" ).add( "organizationalPerson" );
        attrs.get( "objectClass" ).add( "person" );
        attrs.put( "givenName", "Jim" );
        attrs.put( "sn", "Bean" );
        attrs.put( "cn", "\"Jim, Bean\"" );
        
        ctx.createSubcontext( "cn=\"Jim, Bean\"", attrs );
    }


    /**
     * Create an entry a RDN which is not present in the entry
     */
    @Test
    public void testAddEntryNoRDNInEntry() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapService ).lookup( BASE );
        
        // Create a person
        Attributes person = new BasicAttributes( "objectClass", "inetOrgPerson", true );
        person.get( "objectClass" ).add( "top" );
        person.get( "objectClass" ).add( "person" );
        person.get( "objectClass" ).add( "organizationalperson" );
        person.put( "sn", "Michael Jackson" );
        person.put( "cn", "Jackson" );

        DirContext michaelCtx = ctx.createSubcontext( "givenname=Michael", person );
        
        assertNotNull( michaelCtx );
        
        DirContext jackson = ( DirContext ) ctx.lookup( "givenname=Michael" );
        person = jackson.getAttributes( "" );
        Attribute newOcls = person.get( "objectClass" );

        String[] expectedOcls = { "top", "person", "organizationalPerson", "inetOrgPerson" };

        for ( String name : expectedOcls )
        {
            assertTrue( "object class " + name + " is present", newOcls.contains( name ) );
        }
        
        Attribute givenName = person.get( "givenname" );
        
        assertEquals( "Michael", givenName.get() );
    }


    /**
     * Create an entry a RDN which is not present in the entry, but
     * with another attribute's value
     */
    @Test
    public void testAddEntryDifferentRDNInEntry() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapService ).lookup( BASE );
        
        // Create a person
        Attributes person = new BasicAttributes( "objectClass", "inetOrgPerson", true );
        person.get( "objectClass" ).add( "top" );
        person.get( "objectClass" ).add( "person" );
        person.get( "objectClass" ).add( "organizationalperson" );
        person.put( "givenName", "Michael" );
        person.put( "sn", "Michael Jackson" );
        person.put( "cn", "Jackson" );

        DirContext michaelCtx = ctx.createSubcontext( "cn=Michael", person );
        
        assertNotNull( michaelCtx );
        
        DirContext jackson = ( DirContext ) ctx.lookup( "cn=Michael" );
        person = jackson.getAttributes( "" );
        Attribute newOcls = person.get( "objectClass" );

        String[] expectedOcls = { "top", "person", "organizationalPerson", "inetOrgPerson" };

        for ( String name : expectedOcls )
        {
            assertTrue( "object class " + name + " is present", newOcls.contains( name ) );
        }
        
        Attribute cn = person.get( "cn" );
        
        assertEquals( 2, cn.size() );
        String[] expectedCns = { "Jackson", "Michael" };

        for ( String name : expectedCns )
        {
            assertTrue( "CN " + name + " is present", cn.contains( name ) );
        }
    }


    /**
     * Create an entry a RDN which is not present in the entry, 
     * with another attribute's value, and on a SingleValued attribute
     */
    @Test
    public void testAddEntryDifferentRDNSingleValuedInEntry() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapService ).lookup( BASE );
        
        // Create a person
        Attributes person = new BasicAttributes( "objectClass", "inetOrgPerson", true );
        person.get( "objectClass" ).add( "top" );
        person.get( "objectClass" ).add( "person" );
        person.get( "objectClass" ).add( "organizationalperson" );
        person.put( "displayName", "Michael" );
        person.put( "sn", "Michael Jackson" );
        person.put( "cn", "Jackson" );

        DirContext michaelCtx = ctx.createSubcontext( "displayName=test", person );
        
        assertNotNull( michaelCtx );
        
        DirContext jackson = ( DirContext ) ctx.lookup( "displayName=test" );
        person = jackson.getAttributes( "" );
        Attribute newOcls = person.get( "objectClass" );

        String[] expectedOcls = { "top", "person", "organizationalPerson", "inetOrgPerson" };

        for ( String name : expectedOcls )
        {
            assertTrue( "object class " + name + " is present", newOcls.contains( name ) );
        }
        
        // Check that the displayName attribute has been replaced
        Attribute displayName = person.get( "displayName" );
        
        assertEquals( 1, displayName.size() );
        assertTrue( displayName.contains( "test" ) );
    }


    /**
     * Create an entry a composed RDN which is not present in the entry, 
     * with another attribute's value, and on a SingleValued attribute
     */
    @Test
    public void testAddEntryComposedRDN() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapService ).lookup( BASE );
        
        // Create a person
        Attributes person = new BasicAttributes( "objectClass", "inetOrgPerson", true );
        person.get( "objectClass" ).add( "top" );
        person.get( "objectClass" ).add( "person" );
        person.get( "objectClass" ).add( "organizationalperson" );
        person.put( "sn", "Michael Jackson" );
        person.put( "cn", "Jackson" );

        DirContext michaelCtx = ctx.createSubcontext( "displayName=test+cn=Michael", person );
        
        assertNotNull( michaelCtx );
        
        DirContext jackson = ( DirContext ) ctx.lookup( "displayName=test+cn=Michael" );
        person = jackson.getAttributes( "" );
        Attribute newOcls = person.get( "objectClass" );

        String[] expectedOcls = { "top", "person", "organizationalPerson", "inetOrgPerson" };

        for ( String name : expectedOcls )
        {
            assertTrue( "object class " + name + " is present", newOcls.contains( name ) );
        }
        
        // Check that the DIsplayName attribute has been added
        Attribute displayName = person.get( "displayName" );
        
        assertEquals( 1, displayName.size() );
        assertTrue( displayName.contains( "test" ) );

        // Check that the cn attribute value has been added
        Attribute cn = person.get( "cn" );
        
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
    public void testAddPDUExceedingMaxSize() throws Exception
    {
        // Limit the PDU size to 1024
        ldapService.getDirectoryService().setMaxPDUSize( 1024 );
        DirContext ctx = ( DirContext ) getWiredContext( ldapService ).lookup( BASE );

        // modify object classes, add two more
        Attributes attributes = new BasicAttributes( true );
        Attribute ocls = new BasicAttribute( "description" );
        
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
        ldapService.getDirectoryService().setMaxPDUSize( 4096 );
        
        ctx = ( DirContext ) getWiredContext( ldapService ).lookup( BASE );
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
        ctx = ( DirContext ) getWiredContext( ldapService ).lookup( BASE );
        person = ( DirContext ) ctx.lookup( RDN );
        
        assertNotNull( person );
        attributes = person.getAttributes( "" );
        Attribute newOcls = attributes.get( "objectClass" );

        assertNotNull( newOcls );
    }


    /**
     * Test for DIRSERVER-1311: If the RDN attribute+value is not present
     * in the entry the server should implicit add this attribute+value to
     * the entry. Additionally, if the RDN value is escaped or a hexstring
     * the server must add the unescaped string or binary value to the entry.
     */
    @Test
    public void testAddUnescapedRdnValue_DIRSERVER_1311() throws Exception
    {
        LdapContext ctx = ( LdapContext ) getWiredContext( ldapService ).lookup( BASE );

        Attributes tori = new BasicAttributes( true );
        Attribute toriOC = new BasicAttribute( "objectClass" );
        toriOC.add( "top" );
        toriOC.add( "person" );
        tori.put( toriOC );
        tori.put( "cn", "Tori Amos" );
        tori.put( "sn", "Amos" );
        /*
         * Note that the RDN attribute is different to the cn specified in the entry.
         * This creates a second cn attribute "cn:Amos,Tori". This is a JNDI hack:
         * If no other cn is available in the entry, JNDI adds the RDN 
         * attribute to the entry before sending the request to the server.
         */
        ctx.createSubcontext( " cn = Amos\\,Tori ", tori );

        Attributes binary = new BasicAttributes( true );
        Attribute binaryOC = new BasicAttribute( "objectClass" );
        binaryOC.add( "top" );
        binaryOC.add( "person" );
        binary.put( binaryOC );
        binary.put( "cn", "Binary" );
        binary.put( "sn", "Binary" );
        binary.put( "userPassword", "test" );
        /*
         * Note that the RDN attribute is different to the userPassword specified 
         * in the entry. This creates a second cn attribute "userPassword:#414243". 
         * This is a JNDI hack:
         * If no other userPassword is available in the entry, JNDI adds the RDN 
         * attribute to the entry before sending the request to the server.
         */
        ctx.createSubcontext( " userPassword = #414243 ", binary );

        SearchControls controls = new SearchControls();
        NamingEnumeration<SearchResult> res;

        // search for the implicit added cn
        res = ctx.search( "", "(cn=Amos,Tori)", controls );
        assertTrue( res.hasMore() );
        Attribute cnAttribute = res.next().getAttributes().get( "cn" );
        assertEquals( 2, cnAttribute.size() );
        assertTrue( cnAttribute.contains( "Tori Amos" ) );
        assertTrue( cnAttribute.contains( "Amos,Tori" ) );
        assertFalse( res.hasMore() );

        // search for the implicit added userPassword
        res = ctx.search( "", "(userPassword=\\41\\42\\43)", controls );
        assertTrue( res.hasMore() );
        Attribute userPasswordAttribute = res.next().getAttributes().get( "userPassword" );
        assertEquals( 2, userPasswordAttribute.size() );
        assertTrue( userPasswordAttribute.contains( StringTools.getBytesUtf8( "test" ) ) );
        assertTrue( userPasswordAttribute.contains( StringTools.getBytesUtf8( "ABC" ) ) );
        assertFalse( res.hasMore() );
    }

}
