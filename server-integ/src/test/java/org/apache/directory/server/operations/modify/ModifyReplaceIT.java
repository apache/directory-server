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
package org.apache.directory.server.operations.modify;


import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InvalidAttributeIdentifierException;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SchemaViolationException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Test case for all modify replace operations.
 * 
 * Demonstrates DIRSERVER-646 ("Replacing an unknown attribute with
 * no values (deletion) causes an error").
 */
@ExtendWith( { ApacheDSTestExtension.class } )
@CreateDS(enableChangeLog = true, name = "ModifyReplaceIT-class")
@CreateLdapServer(
    transports =
        {
            @CreateTransport(protocol = "LDAP"),
            @CreateTransport(protocol = "LDAPS")
    })
@ApplyLdifs(
    {
        // Entry # 1
        "dn: cn=Kate Bush,ou=system",
        "objectClass: top",
        "objectClass: person",
        "sn: Bush",
        "cn: Kate Bush",

        // Entry # 2
        "dn: cn=Kim Wilde,ou=system",
        "objectClass: top",
        "objectClass: person",
        "objectClass: organizationalPerson ",
        "objectClass: inetOrgPerson ",
        "sn: Wilde",
        "cn: Kim Wilde" })
public class ModifyReplaceIT extends AbstractLdapTestUnit
{
    private static final String BASE = "ou=system";


    /**
     * Create a person entry and try to remove a not present attribute
     */
    @Test
    public void testReplaceToRemoveNotPresentAttribute() throws Exception
    {
        DirContext sysRoot = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );

        String rdn = "cn=Kate Bush";

        Attribute attr = new BasicAttribute( "description" );
        ModificationItem item = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );

        sysRoot.modifyAttributes( rdn, new ModificationItem[]
            { item } );

        SearchControls sctls = new SearchControls();
        sctls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        String filter = "(sn=Bush)";
        String base = "";

        NamingEnumeration<SearchResult> enm = sysRoot.search( base, filter, sctls );

        while ( enm.hasMore() )
        {
            SearchResult sr = enm.next();
            Attribute cn = sr.getAttributes().get( "cn" );
            assertNotNull( cn );
            assertTrue( cn.contains( "Kate Bush" ) );
            Attribute desc = sr.getAttributes().get( "description" );
            assertNull( desc );
        }
    }


    /**
     * Create a person entry and try to add a not present attribute via a REPLACE
     */
    @Test
    public void testReplaceToAddNotPresentAttribute() throws Exception
    {
        DirContext sysRoot = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );

        String rdn = "cn=Kate Bush";

        Attribute attr = new BasicAttribute( "description", "added description" );
        ModificationItem item = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );

        sysRoot.modifyAttributes( rdn, new ModificationItem[]
            { item } );

        SearchControls sctls = new SearchControls();
        sctls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        String filter = "(sn=Bush)";
        String base = "";

        NamingEnumeration<SearchResult> enm = sysRoot.search( base, filter, sctls );

        while ( enm.hasMore() )
        {
            SearchResult sr = enm.next();
            Attribute cn = sr.getAttributes().get( "cn" );
            assertNotNull( cn );
            assertTrue( cn.contains( "Kate Bush" ) );
            Attribute desc = sr.getAttributes().get( "description" );
            assertNotNull( desc );
            assertTrue( desc.contains( "added description" ) );
            assertEquals( 1, desc.size() );
        }
    }


    /**
     * Create a person entry and try to remove a non existing attribute
     */
    @Test
    public void testReplaceNonExistingAttribute() throws Exception
    {
        DirContext sysRoot = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );

        String rdn = "cn=Kate Bush";

        Attribute attr = new BasicAttribute( "numberOfOctaves" );
        ModificationItem item = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );

        try
        {
            sysRoot.modifyAttributes( rdn, new ModificationItem[]
                { item } );
            fail();
        }
        catch ( InvalidAttributeIdentifierException iaie )
        {
            assertTrue( true );
        }

        SearchControls sctls = new SearchControls();
        sctls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        String filter = "(sn=Bush)";
        String base = "";

        NamingEnumeration<SearchResult> enm = sysRoot.search( base, filter, sctls );

        while ( enm.hasMore() )
        {
            SearchResult sr = enm.next();
            Attribute cn = sr.getAttributes().get( "cn" );
            assertNotNull( cn );
            assertTrue( cn.contains( "Kate Bush" ) );
        }
    }


    /**
     * Create a person entry and try to remove a non existing attribute
     */
    @Test
    public void testReplaceNonExistingAttributeManyMods() throws Exception
    {
        DirContext sysRoot = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );

        String rdn = "cn=Kate Bush";

        Attribute attr = new BasicAttribute( "numberOfOctaves" );
        ModificationItem item = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );
        Attribute attr2 = new BasicAttribute( "description", "blah blah blah" );
        ModificationItem item2 = new ModificationItem( DirContext.ADD_ATTRIBUTE, attr2 );

        try
        {
            sysRoot.modifyAttributes( rdn, new ModificationItem[]
                { item, item2 } );
            fail();
        }
        catch ( InvalidAttributeIdentifierException iaie )
        {
            assertTrue( true );
        }

        SearchControls sctls = new SearchControls();
        sctls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        String filter = "(sn=Bush)";
        String base = "";

        NamingEnumeration<SearchResult> enm = sysRoot.search( base, filter, sctls );
        while ( enm.hasMore() )
        {
            SearchResult sr = enm.next();
            Attribute cn = sr.getAttributes().get( "cn" );
            assertNotNull( cn );
            assertTrue( cn.contains( "Kate Bush" ) );
        }
    }


    /**
     * Create a person entry and try to replace a non existing indexed attribute
     */
    @Test
    public void testReplaceNonExistingIndexedAttribute() throws Exception
    {
        DirContext sysRoot = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );

        String rdn = "cn=Kim Wilde";

        Attribute attr = new BasicAttribute( "ou", "test" );
        ModificationItem item = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );

        sysRoot.modifyAttributes( rdn, new ModificationItem[]
            { item } );

        SearchControls sctls = new SearchControls();
        sctls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        String filter = "(sn=Wilde)";
        String base = "";

        NamingEnumeration<SearchResult> enm = sysRoot.search( base, filter, sctls );

        while ( enm.hasMore() )
        {
            SearchResult sr = enm.next();
            Attribute ou = sr.getAttributes().get( "ou" );
            assertNotNull( ou );
            assertTrue( ou.contains( "test" ) );
        }
    }


    /**
     * Create a person entry, replace telephoneNumber, verify the
     * case of the attribute description attribute.
     */
    @Test
    public void testReplaceCaseOfAttributeDescription() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );
        String rdn = "cn=Kate Bush";

        // Replace telephoneNumber
        String newValue = "2345678901";
        Attributes attrs = new BasicAttributes( "telephoneNumber", newValue, false );
        ctx.modifyAttributes( rdn, DirContext.REPLACE_ATTRIBUTE, attrs );

        // Verify, that
        // - case of attribute description is correct
        // - attribute value is added
        attrs = ctx.getAttributes( rdn );
        Attribute attr = attrs.get( "telephoneNumber" );
        assertNotNull( attr );
        assertEquals( "telephoneNumber", attr.getID() );
        assertTrue( attr.contains( newValue ) );
        assertEquals( 1, attr.size() );
    }


    /**
     * Create a person entry, replace an attribute not present in the ObjectClasses
     */
    @Test
    public void testReplaceAttributeNotInOC() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );
        String rdn = "cn=Kate Bush";

        // Replace ou
        String newValue = "Test";
        Attributes attrs = new BasicAttributes( "ou", newValue, false );

        try
        {
            ctx.modifyAttributes( rdn, DirContext.REPLACE_ATTRIBUTE, attrs );
            fail( "Should get a SchemaViolationException" );
        }
        catch ( SchemaViolationException sve )
        {
            assertTrue( true );
        }
    }


    /**
     * Create a person entry, replace an attribute not present in the ObjectClasses
     */
    @Test
    public void testReplaceAttributeValueWithNonAsciiChars() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );
        String rdn = "cn=Kate Bush";

        // Replace sn
        String newValue = "test \u00DF test";
        Attributes attrs = new BasicAttributes( "sn", newValue, false );

        ctx.modifyAttributes( rdn, DirContext.REPLACE_ATTRIBUTE, attrs );

        attrs = ctx.getAttributes( rdn );
        Attribute attr = attrs.get( "sn" );
        assertNotNull( attr );
        assertEquals( "sn", attr.getID() );
        assertTrue( attr.contains( newValue ) );
        assertEquals( 1, attr.size() );
    }
}
