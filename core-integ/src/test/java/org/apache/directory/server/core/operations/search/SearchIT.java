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
package org.apache.directory.server.core.operations.search;


import static org.apache.directory.server.core.integ.IntegrationUtils.getRootContext;
import static org.apache.directory.server.core.integ.IntegrationUtils.getSchemaContext;
import static org.apache.directory.server.core.integ.IntegrationUtils.getSystemContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InvalidSearchFilterException;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.shared.ldap.constants.JndiPropertyConstants;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.entry.DefaultEntry;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.StringValue;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.GreaterEqNode;
import org.apache.directory.shared.ldap.filter.LessEqNode;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.ldif.LdifUtils;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.message.Response;
import org.apache.directory.shared.ldap.model.message.SearchResultEntry;
import org.apache.directory.shared.ldap.name.Dn;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests the search() methods of the provider.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "SearchDS")
@ApplyLdifs(
    { "dn: m-oid=2.2.0, ou=attributeTypes, cn=apachemeta, ou=schema", "objectclass: metaAttributeType",
        "objectclass: metaTop", "objectclass: top", "m-oid: 2.2.0", "m-name: integerAttribute",
        "m-description: the precursor for all integer attributes", "m-equality: integerMatch",
        "m-ordering: integerOrderingMatch", "m-syntax: 1.3.6.1.4.1.1466.115.121.1.27", "m-length: 0", "",
        "dn: ou=testing00,ou=system", "objectClass: top", "objectClass: organizationalUnit",
        "objectClass: extensibleObject", "ou: testing00", "integerAttribute: 0", "", "dn: ou=testing01,ou=system",
        "objectClass: top", "objectClass: organizationalUnit", "objectClass: extensibleObject", "ou: testing01",
        "integerAttribute: 1", "", "dn: ou=testing02,ou=system", "objectClass: top", "objectClass: organizationalUnit",
        "objectClass: extensibleObject", "ou: testing02", "integerAttribute: 2", "", "dn: ou=testing03,ou=system",
        "objectClass: top", "objectClass: organizationalUnit", "objectClass: extensibleObject", "ou: testing03",
        "integerAttribute: 3", "", "dn: ou=testing04,ou=system", "objectClass: top", "objectClass: organizationalUnit",
        "objectClass: extensibleObject", "ou: testing04", "integerAttribute: 4", "", "dn: ou=testing05,ou=system",
        "objectClass: top", "objectClass: organizationalUnit", "objectClass: extensibleObject", "ou: testing05",
        "integerAttribute: 5", "", "dn: ou=subtest,ou=testing01,ou=system", "objectClass: top",
        "objectClass: organizationalUnit", "ou: subtest", "", "dn: cn=Heather Nova, ou=system", "objectClass: top",
        "objectClass: person", "cn: Heather Nova", "sn: Nova", "telephoneNumber: 1 801 555 1212 ", "",
        "dn: cn=with-dn, ou=system", "objectClass: top", "objectClass: person", "objectClass: organizationalPerson",
        "objectClass: inetorgPerson", "cn: singer", "sn: manager", "telephoneNumber: 1 801 555 1212 ",
        "manager: cn=Heather Nova, ou=system" })
public class SearchIT extends AbstractLdapTestUnit
{
    private static final String RDN = "cn=Heather Nova";
    private static final String FILTER = "(objectclass=*)";

    public static LdapContext sysRoot;


    /**
     * @param sysRoot the system root to add entries to
     * @throws NamingException on errors
     */
    @Before
    public void createData() throws Exception
    {
        service.getSchemaManager().enable( "nis" );

        sysRoot = getSystemContext( service );

        /*
         * Check ou=testing00,ou=system
         */
        DirContext ctx = ( DirContext ) sysRoot.lookup( "ou=testing00" );
        assertNotNull( ctx );
        Attributes attributes = ctx.getAttributes( "" );
        assertNotNull( attributes );
        assertEquals( "testing00", attributes.get( "ou" ).get() );
        Attribute attribute = attributes.get( "objectClass" );
        assertNotNull( attribute );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "organizationalUnit" ) );

        /*
         * check ou=testing01,ou=system
         */
        ctx = ( DirContext ) sysRoot.lookup( "ou=testing01" );
        assertNotNull( ctx );
        attributes = ctx.getAttributes( "" );
        assertNotNull( attributes );
        assertEquals( "testing01", attributes.get( "ou" ).get() );
        attribute = attributes.get( "objectClass" );
        assertNotNull( attribute );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "organizationalUnit" ) );

        /*
         * Check ou=testing02,ou=system
         */
        ctx = ( DirContext ) sysRoot.lookup( "ou=testing02" );
        assertNotNull( ctx );

        attributes = ctx.getAttributes( "" );
        assertNotNull( attributes );
        assertEquals( "testing02", attributes.get( "ou" ).get() );

        attribute = attributes.get( "objectClass" );
        assertNotNull( attribute );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "organizationalUnit" ) );

        /*
         * Check ou=subtest,ou=testing01,ou=system
         */
        ctx = ( DirContext ) sysRoot.lookup( "ou=subtest,ou=testing01" );
        assertNotNull( ctx );

        attributes = ctx.getAttributes( "" );
        assertNotNull( attributes );
        assertEquals( "subtest", attributes.get( "ou" ).get() );

        attribute = attributes.get( "objectClass" );
        assertNotNull( attribute );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "organizationalUnit" ) );

        /*
         *  Check entry cn=Heather Nova, ou=system
         */
        ctx = ( DirContext ) sysRoot.lookup( RDN );
        assertNotNull( ctx );

        // -------------------------------------------------------------------
        // Enable the nis schema
        // -------------------------------------------------------------------

        // check if nis is disabled
        LdapContext schemaRoot = getSchemaContext( service );
        Attributes nisAttrs = schemaRoot.getAttributes( "cn=nis" );
        boolean isNisDisabled = false;

        if ( nisAttrs.get( "m-disabled" ) != null )
        {
            isNisDisabled = ( ( String ) nisAttrs.get( "m-disabled" ).get() ).equalsIgnoreCase( "TRUE" );
        }

        // if nis is disabled then enable it
        if ( isNisDisabled )
        {
            Attribute disabled = new BasicAttribute( "m-disabled" );
            ModificationItem[] mods = new ModificationItem[]
                { new ModificationItem( DirContext.REMOVE_ATTRIBUTE, disabled ) };
            schemaRoot.modifyAttributes( "cn=nis", mods );
        }

        // -------------------------------------------------------------------
        // Add a bunch of nis groups
        // -------------------------------------------------------------------
        addNisPosixGroup( "testGroup0", 0 );
        addNisPosixGroup( "testGroup1", 1 );
        addNisPosixGroup( "testGroup2", 2 );
        addNisPosixGroup( "testGroup4", 4 );
        addNisPosixGroup( "testGroup5", 5 );
    }


    /**
     * Create a NIS group
     */
    private static DirContext addNisPosixGroup( String name, int gid ) throws Exception
    {
        Attributes attrs = LdifUtils.createAttributes( "objectClass: top", "objectClass: posixGroup", "cn", name,
            "gidNumber", String.valueOf( gid ) );

        return getSystemContext( service ).createSubcontext( "cn=" + name + ",ou=groups", attrs );
    }


    @Test
    public void testSearchOneLevel() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        controls.setDerefLinkFlag( false );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, AliasDerefMode.NEVER_DEREF_ALIASES
            .getJndiValue() );
        HashMap<String, Attributes> map = new HashMap<String, Attributes>();

        NamingEnumeration<SearchResult> list = sysRoot.search( "", "(ou=*)", controls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            map.put( result.getName(), result.getAttributes() );
        }

        assertEquals( "Expected number of results returned was incorrect!", 9, map.size() );
        assertTrue( map.containsKey( "ou=testing00,ou=system" ) );
        assertTrue( map.containsKey( "ou=testing01,ou=system" ) );
        assertTrue( map.containsKey( "ou=testing02,ou=system" ) );
    }


    @Test
    public void testSearchSubTreeLevel() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setDerefLinkFlag( false );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, AliasDerefMode.NEVER_DEREF_ALIASES
            .getJndiValue() );

        HashMap<String, Attributes> map = new HashMap<String, Attributes>();
        NamingEnumeration<SearchResult> list = sysRoot.search( "", "(ou=*)", controls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            map.put( result.getName(), result.getAttributes() );
        }

        assertEquals( "Expected number of results returned was incorrect", 14, map.size() );
        assertTrue( map.containsKey( "ou=system" ) );
        assertTrue( map.containsKey( "ou=testing00,ou=system" ) );
        assertTrue( map.containsKey( "ou=testing01,ou=system" ) );
        assertTrue( map.containsKey( "ou=testing02,ou=system" ) );
        assertTrue( map.containsKey( "ou=subtest,ou=testing01,ou=system" ) );
    }


    @Test
    public void testSearchSubTreeLevelNoAttributes() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setDerefLinkFlag( false );
        controls.setReturningAttributes( new String[]
            { "1.1" } );

        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, AliasDerefMode.NEVER_DEREF_ALIASES
            .getJndiValue() );

        HashMap<String, Attributes> map = new HashMap<String, Attributes>();
        NamingEnumeration<SearchResult> list = sysRoot.search( "", "(ou=testing02)", controls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            map.put( result.getName(), result.getAttributes() );
        }

        assertEquals( "Expected number of results returned was incorrect", 1, map.size() );
        assertTrue( map.containsKey( "ou=testing02,ou=system" ) );
        Attributes attrs = map.get( "ou=testing02,ou=system" );
        assertEquals( 0, attrs.size() );
    }


    @Test
    public void testSearchSubstringSubTreeLevel() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setDerefLinkFlag( false );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, AliasDerefMode.NEVER_DEREF_ALIASES
            .getJndiValue() );

        HashMap<String, Attributes> map = new HashMap<String, Attributes>();
        NamingEnumeration<SearchResult> list = sysRoot.search( "", "(objectClass=organ*)", controls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            map.put( result.getName(), result.getAttributes() );
        }

        // 16 because it also matches organizationalPerson which the admin is
        assertEquals( "Expected number of results returned was incorrect", 17, map.size() );
        assertTrue( map.containsKey( "ou=system" ) );
        assertTrue( map.containsKey( "ou=configuration,ou=system" ) );
        assertTrue( map.containsKey( "ou=interceptors,ou=configuration,ou=system" ) );
        assertTrue( map.containsKey( "ou=partitions,ou=configuration,ou=system" ) );
        assertTrue( map.containsKey( "ou=services,ou=configuration,ou=system" ) );
        assertTrue( map.containsKey( "ou=groups,ou=system" ) );
        assertTrue( map.containsKey( "ou=testing00,ou=system" ) );
        assertTrue( map.containsKey( "ou=testing01,ou=system" ) );
        assertTrue( map.containsKey( "ou=subtest,ou=testing01,ou=system" ) );
        assertTrue( map.containsKey( "ou=testing02,ou=system" ) );
        assertTrue( map.containsKey( "ou=users,ou=system" ) );
        assertTrue( map.containsKey( "prefNodeName=sysPrefRoot,ou=system" ) );
        assertTrue( map.containsKey( "uid=admin,ou=system" ) );
    }


    /**
     * Tests to make sure undefined attributes in filter assertions are pruned and do not
     * result in exceptions.
     */
    @Test
    public void testBogusAttributeInSearchFilter() throws Exception
    {
        boolean oldSetAllowAnnonymousAccess = service.isAllowAnonymousAccess();
        service.setAllowAnonymousAccess( true );

        SearchControls cons = new SearchControls();
        NamingEnumeration<SearchResult> e = sysRoot.search( "", "(bogusAttribute=abc123)", cons );
        assertNotNull( e );

        e = sysRoot.search( "", "(!(bogusAttribute=abc123))", cons );
        assertNotNull( e );
        assertFalse( e.hasMore() );

        e = sysRoot.search( "", "(|(bogusAttribute=abc123)(bogusAttribute=abc123))", cons );
        assertNotNull( e );
        assertFalse( e.hasMore() );

        e = sysRoot.search( "", "(|(bogusAttribute=abc123)(ou=abc123))", cons );
        assertNotNull( e );
        assertFalse( e.hasMore() );

        e = sysRoot.search( "", "(OBJECTclass=*)", cons );
        assertNotNull( e );
        assertTrue( e.hasMore() );

        e = sysRoot.search( "", "(objectclass=*)", cons );
        assertNotNull( e );

        service.setAllowAnonymousAccess( oldSetAllowAnnonymousAccess );
    }


    @Test
    public void testSearchFilterArgs() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        controls.setDerefLinkFlag( false );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, AliasDerefMode.NEVER_DEREF_ALIASES
            .getJndiValue() );
        HashMap<String, Attributes> map = new HashMap<String, Attributes>();

        NamingEnumeration<SearchResult> list = sysRoot.search( "", "(|(ou={0})(ou={1}))", new Object[]
            { "testing00", "testing01" }, controls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            map.put( result.getName(), result.getAttributes() );
        }

        assertEquals( "Expected number of results returned was incorrect!", 2, map.size() );
        assertTrue( map.containsKey( "ou=testing00,ou=system" ) );
        assertTrue( map.containsKey( "ou=testing01,ou=system" ) );
    }


    @Test
    public void testFilterExpansion0() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setDerefLinkFlag( false );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, AliasDerefMode.NEVER_DEREF_ALIASES
            .getJndiValue() );

        HashMap<String, Attributes> map = new HashMap<String, Attributes>();
        NamingEnumeration<SearchResult> list = sysRoot.search( "", "(name=testing00)", controls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            map.put( result.getName(), result.getAttributes() );
        }

        assertEquals( "size of results", 1, map.size() );
        assertTrue( "contains ou=testing00,ou=system", map.containsKey( "ou=testing00,ou=system" ) );
    }


    @Test
    public void testFilterExpansion1() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setDerefLinkFlag( false );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, AliasDerefMode.NEVER_DEREF_ALIASES
            .getJndiValue() );

        HashMap<String, Attributes> map = new HashMap<String, Attributes>();
        NamingEnumeration<SearchResult> list = sysRoot.search( "", "(name=*)", controls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            map.put( result.getName(), result.getAttributes() );
        }

        assertEquals( "size of results", 23, map.size() );
        assertTrue( "contains ou=testing00,ou=system", map.containsKey( "ou=testing00,ou=system" ) );
        assertTrue( "contains ou=testing01,ou=system", map.containsKey( "ou=testing01,ou=system" ) );
        assertTrue( "contains ou=testing02,ou=system", map.containsKey( "ou=testing01,ou=system" ) );
        assertTrue( "contains ou=configuration,ou=system", map.containsKey( "ou=configuration,ou=system" ) );
        assertTrue( "contains ou=groups,ou=system", map.containsKey( "ou=groups,ou=system" ) );
        assertTrue( "contains ou=interceptors,ou=configuration,ou=system", map
            .containsKey( "ou=interceptors,ou=configuration,ou=system" ) );
        assertTrue( "contains ou=partitions,ou=configuration,ou=system", map
            .containsKey( "ou=partitions,ou=configuration,ou=system" ) );
        assertTrue( "contains ou=services,ou=configuration,ou=system", map
            .containsKey( "ou=services,ou=configuration,ou=system" ) );
        assertTrue( "contains ou=subtest,ou=testing01,ou=system", map.containsKey( "ou=subtest,ou=testing01,ou=system" ) );
        assertTrue( "contains ou=system", map.containsKey( "ou=system" ) );
        assertTrue( "contains ou=users,ou=system", map.containsKey( "ou=users,ou=system" ) );
        assertTrue( "contains uid=admin,ou=system", map.containsKey( "uid=admin,ou=system" ) );
        assertTrue( "contains cn=administrators,ou=groups,ou=system", map
            .containsKey( "cn=Administrators,ou=groups,ou=system" ) );
    }


    @Test
    public void testFilterExpansion2() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setDerefLinkFlag( false );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, AliasDerefMode.NEVER_DEREF_ALIASES
            .getJndiValue() );

        HashMap<String, Attributes> map = new HashMap<String, Attributes>();
        NamingEnumeration<SearchResult> list = sysRoot.search( "", "(|(name=testing00)(name=testing01))", controls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            map.put( result.getName(), result.getAttributes() );
        }

        assertEquals( "size of results", 2, map.size() );
        assertTrue( "contains ou=testing00,ou=system", map.containsKey( "ou=testing00,ou=system" ) );
        assertTrue( "contains ou=testing01,ou=system", map.containsKey( "ou=testing01,ou=system" ) );
    }


    @Test
    public void testFilterExpansion4() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setDerefLinkFlag( false );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, AliasDerefMode.NEVER_DEREF_ALIASES
            .getJndiValue() );

        HashMap<String, Attributes> map = new HashMap<String, Attributes>();
        NamingEnumeration<SearchResult> list = sysRoot.search( "", "(name=testing*)", controls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            map.put( result.getName(), result.getAttributes() );
        }

        assertEquals( "size of results", 6, map.size() );
        assertTrue( "contains ou=testing00,ou=system", map.containsKey( "ou=testing00,ou=system" ) );
        assertTrue( "contains ou=testing01,ou=system", map.containsKey( "ou=testing01,ou=system" ) );
        assertTrue( "contains ou=testing02,ou=system", map.containsKey( "ou=testing02,ou=system" ) );
        assertTrue( "contains ou=testing03,ou=system", map.containsKey( "ou=testing03,ou=system" ) );
        assertTrue( "contains ou=testing04,ou=system", map.containsKey( "ou=testing04,ou=system" ) );
        assertTrue( "contains ou=testing05,ou=system", map.containsKey( "ou=testing05,ou=system" ) );
    }


    @Test
    public void testFilterExpansion5() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setDerefLinkFlag( false );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, AliasDerefMode.NEVER_DEREF_ALIASES
            .getJndiValue() );

        HashMap<String, Attributes> map = new HashMap<String, Attributes>();
        String filter = "(|(2.5.4.11.1=testing*)(2.5.4.54=testing*)(2.5.4.10=testing*)"
            + "(2.5.4.6=testing*)(2.5.4.43=testing*)(2.5.4.7.1=testing*)(2.5.4.10.1=testing*)"
            + "(2.5.4.44=testing*)(2.5.4.11=testing*)(2.5.4.4=testing*)(2.5.4.8.1=testing*)"
            + "(2.5.4.12=testing*)(1.3.6.1.4.1.18060.0.4.1.2.3=testing*)"
            + "(2.5.4.7=testing*)(2.5.4.3=testing*)(2.5.4.8=testing*)(2.5.4.42=testing*))";
        NamingEnumeration<SearchResult> list = sysRoot.search( "", filter, controls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            map.put( result.getName(), result.getAttributes() );
        }

        assertEquals( "size of results", 6, map.size() );
        assertTrue( "contains ou=testing00,ou=system", map.containsKey( "ou=testing00,ou=system" ) );
        assertTrue( "contains ou=testing01,ou=system", map.containsKey( "ou=testing01,ou=system" ) );
        assertTrue( "contains ou=testing02,ou=system", map.containsKey( "ou=testing02,ou=system" ) );
        assertTrue( "contains ou=testing03,ou=system", map.containsKey( "ou=testing03,ou=system" ) );
        assertTrue( "contains ou=testing04,ou=system", map.containsKey( "ou=testing04,ou=system" ) );
        assertTrue( "contains ou=testing05,ou=system", map.containsKey( "ou=testing05,ou=system" ) );
    }


    @Test
    public void testOpAttrDenormalizationOff() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        controls.setDerefLinkFlag( false );
        controls.setReturningAttributes( new String[]
            { "creatorsName" } );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, AliasDerefMode.NEVER_DEREF_ALIASES
            .getJndiValue() );
        HashMap<String, Attributes> map = new HashMap<String, Attributes>();

        NamingEnumeration<SearchResult> list = sysRoot.search( "", "(ou=testing00)", controls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            map.put( result.getName(), result.getAttributes() );
        }

        assertEquals( "Expected number of results returned was incorrect!", 1, map.size() );
        assertTrue( map.containsKey( "ou=testing00,ou=system" ) );
        Attributes attrs = map.get( "ou=testing00,ou=system" );
        assertEquals( "normalized creator's name", "0.9.2342.19200300.100.1.1=admin,2.5.4.11=system", attrs.get(
            "creatorsName" ).get() );
    }


    @Test
    public void testOpAttrDenormalizationOn() throws Exception
    {
        service.setDenormalizeOpAttrsEnabled( true );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        controls.setDerefLinkFlag( false );
        controls.setReturningAttributes( new String[]
            { "creatorsName" } );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, AliasDerefMode.NEVER_DEREF_ALIASES
            .getJndiValue() );
        HashMap<String, Attributes> map = new HashMap<String, Attributes>();

        NamingEnumeration<SearchResult> list = sysRoot.search( "", "(ou=testing00)", controls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            map.put( result.getName(), result.getAttributes() );
        }

        assertEquals( "Expected number of results returned was incorrect!", 1, map.size() );
        assertTrue( map.containsKey( "ou=testing00,ou=system" ) );
        Attributes attrs = map.get( "ou=testing00,ou=system" );
        assertEquals( "normalized creator's name", "uid=admin,ou=system", attrs.get( "creatorsName" ).get() );
    }


    /**
     * Creation of required attributes of a person entry.
     *
     * @param cn the commonName of the person
     * @param sn the surName of the person
     * @return the attributes of a new person entry
     */
    protected Attributes getPersonAttributes( String sn, String cn ) throws LdapException
    {
        Attributes attributes = LdifUtils.createAttributes( "objectClass: top", "objectClass: top",
            "objectClass: person", "cn", cn, "sn", sn );

        return attributes;
    }


    @Test
    public void testBinaryAttributesInFilter() throws Exception
    {
        byte[] certData = new byte[]
            { 0x34, 0x56, 0x4e, 0x5f };

        // First let's add a some binary data representing a userCertificate
        Attributes attrs = getPersonAttributes( "Bush", "Kate Bush" );
        attrs.put( "userCertificate", certData );

        Attribute objectClasses = attrs.get( "objectClass" );
        objectClasses.add( "strongAuthenticationUser" );

        sysRoot.createSubcontext( "cn=Kate Bush", attrs );

        // Search for kate by cn first
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        NamingEnumeration<SearchResult> enm = sysRoot.search( "", "(cn=Kate Bush)", controls );
        assertTrue( enm.hasMore() );
        SearchResult sr = enm.next();
        assertNotNull( sr );
        assertFalse( enm.hasMore() );
        assertEquals( "cn=Kate Bush,ou=system", sr.getName() );

        enm = sysRoot.search( "", "(userCertificate=\\34\\56\\4E\\5F)", controls );
        assertTrue( enm.hasMore() );
        sr = enm.next();
        assertNotNull( sr );
        assertFalse( enm.hasMore() );
        assertEquals( "cn=Kate Bush,ou=system", sr.getName() );
    }


    @Test
    public void testSearchOperationalAttr() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        controls.setDerefLinkFlag( false );
        controls.setReturningAttributes( new String[]
            { "+" } );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, AliasDerefMode.NEVER_DEREF_ALIASES
            .getJndiValue() );
        HashMap<String, Attributes> map = new HashMap<String, Attributes>();

        NamingEnumeration<SearchResult> list = sysRoot.search( "", "(ou=testing01)", controls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            map.put( result.getName(), result.getAttributes() );
        }

        assertEquals( "Expected number of results returned was incorrect!", 1, map.size() );

        Attributes attrs = map.get( "ou=testing01,ou=system" );

        assertNotNull( attrs.get( "createTimestamp" ) );
        assertNotNull( attrs.get( "creatorsName" ) );
        assertNull( attrs.get( "objectClass" ) );
        assertNull( attrs.get( "ou" ) );
    }


    @Test
    public void testSearchUserAttr() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        controls.setDerefLinkFlag( false );
        controls.setReturningAttributes( new String[]
            { "*" } );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, AliasDerefMode.NEVER_DEREF_ALIASES
            .getJndiValue() );
        HashMap<String, Attributes> map = new HashMap<String, Attributes>();

        NamingEnumeration<SearchResult> list = sysRoot.search( "", "(ou=testing01)", controls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            map.put( result.getName(), result.getAttributes() );
        }

        assertEquals( "Expected number of results returned was incorrect!", 1, map.size() );

        Attributes attrs = map.get( "ou=testing01,ou=system" );

        assertNotNull( attrs.get( "objectClass" ) );
        assertNotNull( attrs.get( "ou" ) );
        assertNull( attrs.get( "createTimestamp" ) );
        assertNull( attrs.get( "creatorsName" ) );
    }


    @Test
    public void testSearchUserAttrAndOpAttr() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        controls.setDerefLinkFlag( false );
        controls.setReturningAttributes( new String[]
            { "*", "creatorsName" } );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, AliasDerefMode.NEVER_DEREF_ALIASES
            .getJndiValue() );
        HashMap<String, Attributes> map = new HashMap<String, Attributes>();

        NamingEnumeration<SearchResult> list = sysRoot.search( "", "(ou=testing01)", controls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            map.put( result.getName(), result.getAttributes() );
        }

        assertEquals( "Expected number of results returned was incorrect!", 1, map.size() );

        Attributes attrs = map.get( "ou=testing01,ou=system" );

        assertNotNull( attrs.get( "objectClass" ) );
        assertNotNull( attrs.get( "ou" ) );
        assertNotNull( attrs.get( "creatorsName" ) );
        assertNull( attrs.get( "createTimestamp" ) );
    }


    @Test
    public void testSearchUserAttrAndNoAttr() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        controls.setDerefLinkFlag( false );
        controls.setReturningAttributes( new String[]
            { "1.1", "ou" } );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, AliasDerefMode.NEVER_DEREF_ALIASES
            .getJndiValue() );
        HashMap<String, Attributes> map = new HashMap<String, Attributes>();

        NamingEnumeration<SearchResult> list = sysRoot.search( "", "(ou=testing01)", controls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            map.put( result.getName(), result.getAttributes() );
        }

        assertEquals( "Expected number of results returned was incorrect!", 1, map.size() );

        Attributes attrs = map.get( "ou=testing01,ou=system" );

        assertNull( attrs.get( "objectClass" ) );
        assertNotNull( attrs.get( "ou" ) );
        assertNull( attrs.get( "creatorsName" ) );
        assertNull( attrs.get( "createTimestamp" ) );
    }


    @Test
    public void testSearchNoAttr() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        controls.setDerefLinkFlag( false );
        controls.setReturningAttributes( new String[]
            { "1.1" } );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, AliasDerefMode.NEVER_DEREF_ALIASES
            .getJndiValue() );
        HashMap<String, Attributes> map = new HashMap<String, Attributes>();

        NamingEnumeration<SearchResult> list = sysRoot.search( "", "(ou=testing01)", controls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            map.put( result.getName(), result.getAttributes() );
        }

        assertEquals( "Expected number of results returned was incorrect!", 1, map.size() );

        Attributes attrs = map.get( "ou=testing01,ou=system" );

        assertNull( attrs.get( "objectClass" ) );
        assertNull( attrs.get( "ou" ) );
        assertNull( attrs.get( "creatorsName" ) );
        assertNull( attrs.get( "createTimestamp" ) );
    }


    @Test
    public void testSearchAllAttr() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        controls.setDerefLinkFlag( false );
        controls.setReturningAttributes( new String[]
            { "+", "*" } );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, AliasDerefMode.NEVER_DEREF_ALIASES
            .getJndiValue() );
        HashMap<String, Attributes> map = new HashMap<String, Attributes>();

        NamingEnumeration<SearchResult> list = sysRoot.search( "", "(ou=testing01)", controls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            map.put( result.getName(), result.getAttributes() );
        }

        assertEquals( "Expected number of results returned was incorrect!", 1, map.size() );

        Attributes attrs = map.get( "ou=testing01,ou=system" );

        assertNotNull( attrs.get( "createTimestamp" ) );
        assertNotNull( attrs.get( "creatorsName" ) );
        assertNotNull( attrs.get( "objectClass" ) );
        assertNotNull( attrs.get( "ou" ) );
    }


    /**
     * Search an entry and fetch an attribute with unknown option
     * @throws NamingException if there are errors
     */
    @Test
    public void testSearchFetchNonExistingAttributeOption() throws Exception
    {
        SearchControls ctls = new SearchControls();
        ctls.setSearchScope( SearchControls.OBJECT_SCOPE );
        ctls.setReturningAttributes( new String[]
            { "cn", "sn;unknownOption", "badAttr" } );

        NamingEnumeration<SearchResult> result = sysRoot.search( RDN, FILTER, ctls );

        if ( result.hasMore() )
        {
            SearchResult entry = result.next();
            Attributes attrs = entry.getAttributes();
            Attribute cn = attrs.get( "cn" );

            assertNotNull( cn );
            assertEquals( "Heather Nova", cn.get().toString() );

            Attribute sn = attrs.get( "sn" );
            assertNull( sn );
        }
        else
        {
            fail( "entry " + RDN + " not found" );
        }

        result.close();
    }


    /**
     * Search an entry and fetch an attribute and all its subtypes
     * @throws NamingException if there are errors
     */
    @Test
    public void testSearchFetchAttributeAndSubTypes() throws Exception
    {
        SearchControls ctls = new SearchControls();
        ctls.setSearchScope( SearchControls.OBJECT_SCOPE );
        ctls.setReturningAttributes( new String[]
            { "name" } );

        NamingEnumeration<SearchResult> result = sysRoot.search( RDN, FILTER, ctls );

        if ( result.hasMore() )
        {
            SearchResult entry = result.next();
            Attributes attrs = entry.getAttributes();

            // We should have get cn and sn only
            assertEquals( 2, attrs.size() );

            // Check CN
            Attribute cn = attrs.get( "cn" );

            assertNotNull( cn );
            assertEquals( "Heather Nova", cn.get().toString() );

            // Assert SN
            Attribute sn = attrs.get( "sn" );
            assertNotNull( sn );
            assertEquals( "Nova", sn.get().toString() );
        }
        else
        {
            fail( "entry " + RDN + " not found" );
        }

        result.close();
    }


    /**
     * Search an entry and fetch an attribute with twice the same attributeType
     * @throws NamingException if there are errors
     */
    @Test
    public void testSearchFetchTwiceSameAttribute() throws Exception
    {
        SearchControls ctls = new SearchControls();
        ctls.setSearchScope( SearchControls.OBJECT_SCOPE );
        ctls.setReturningAttributes( new String[]
            { "cn", "cn" } );

        NamingEnumeration<SearchResult> result = sysRoot.search( RDN, FILTER, ctls );

        if ( result.hasMore() )
        {
            SearchResult entry = result.next();
            Attributes attrs = entry.getAttributes();
            Attribute cn = attrs.get( "cn" );

            assertNotNull( cn );
            assertEquals( "Heather Nova", cn.get().toString() );
        }
        else
        {
            fail( "entry " + RDN + " not found" );
        }

        result.close();
    }


    // this one is failing because it returns the admin user twice: count = 15
    //    public void testFilterExpansion3() throws Exception
    //    {
    //        SearchControls controls = new SearchControls();
    //        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
    //        controls.setDerefLinkFlag( false );
    //        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, AliasDerefMode.NEVER_DEREF_ALIASES );
    //
    //        List map = new ArrayList();
    //        NamingEnumeration list = sysRoot.search( "", "(name=*)", controls );
    //        while ( list.hasMore() )
    //        {
    //            SearchResult result = ( SearchResult ) list.next();
    //            map.add( result.getName() );
    //        }
    //        assertEquals( "size of results", 14, map.size() );
    //        assertTrue( "contains ou=testing00,ou=system", map.contains( "ou=testing00,ou=system" ) );
    //        assertTrue( "contains ou=testing01,ou=system", map.contains( "ou=testing01,ou=system" ) );
    //        assertTrue( "contains ou=testing02,ou=system", map.contains( "ou=testing01,ou=system" ) );
    //        assertTrue( "contains uid=akarasulu,ou=users,ou=system", map.contains( "uid=akarasulu,ou=users,ou=system" ) );
    //        assertTrue( "contains ou=configuration,ou=system", map.contains( "ou=configuration,ou=system" ) );
    //        assertTrue( "contains ou=groups,ou=system", map.contains( "ou=groups,ou=system" ) );
    //        assertTrue( "contains ou=interceptors,ou=configuration,ou=system", map.contains( "ou=interceptors,ou=configuration,ou=system" ) );
    //        assertTrue( "contains ou=partitions,ou=configuration,ou=system", map.contains( "ou=partitions,ou=configuration,ou=system" ) );
    //        assertTrue( "contains ou=services,ou=configuration,ou=system", map.contains( "ou=services,ou=configuration,ou=system" ) );
    //        assertTrue( "contains ou=subtest,ou=testing01,ou=system", map.contains( "ou=subtest,ou=testing01,ou=system" ) );
    //        assertTrue( "contains ou=system", map.contains( "ou=system" ) );
    //        assertTrue( "contains ou=users,ou=system", map.contains( "ou=users,ou=system" ) );
    //        assertTrue( "contains uid=admin,ou=system", map.contains( "uid=admin,ou=system" ) );
    //        assertTrue( "contains cn=administrators,ou=groups,ou=system", map.contains( "cn=administrators,ou=groups,ou=system" ) );
    //    }

    /**
     *  Convenience method that performs a one level search using the
     *  specified filter returning their DNs as Strings in a set.
     *
     * @param controls the search controls
     * @param filter the filter expression
     * @return the set of groups
     * @throws NamingException if there are problems conducting the search
     */
    public Set<String> searchGroups( String filter, SearchControls controls ) throws Exception
    {
        if ( controls == null )
        {
            controls = new SearchControls();
        }

        Set<String> results = new HashSet<String>();
        NamingEnumeration<SearchResult> list = getSystemContext( service ).search( "ou=groups", filter, controls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            results.add( result.getName() );
        }

        return results;
    }


    /**
     *  Convenience method that performs a one level search using the
     *  specified filter returning their DNs as Strings in a set.
     *
     * @param filter the filter expression
     * @return the set of group names
     * @throws NamingException if there are problems conducting the search
     */
    public Set<String> searchGroups( String filter ) throws Exception
    {
        return searchGroups( filter, null );
    }


    /**
     *  Convenience method that performs a one level search using the
     *  specified filter returning their DNs as Strings in a set.
     *
     * @param controls the search controls
     * @return the set of groups
     * @throws NamingException if there are problems conducting the search
     */
    public Set<String> searchRevisions( String filter ) throws Exception
    {
        SearchControls controls = new SearchControls();

        Set<String> results = new HashSet<String>();
        NamingEnumeration<SearchResult> list = getSystemContext( service ).search( "", filter, controls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            results.add( result.getName() );
        }

        return results;
    }


    /**
     *  Convenience method that performs a one level search using the
     *  specified filter returning their DNs as Strings in a set.
     *
     * @param controls the search controls
     * @param filter the filter expression
     * @return the set of groups
     * @throws NamingException if there are problems conducting the search
     */
    private Set<String> searchUnits( String filter, SearchControls controls ) throws Exception
    {
        if ( controls == null )
        {
            controls = new SearchControls();
        }

        Set<String> results = new HashSet<String>();
        NamingEnumeration<SearchResult> list = getSystemContext( service ).search( "", filter, controls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            results.add( result.getName() );
        }

        return results;
    }


    @Test
    public void testSetup() throws Exception
    {
        Set<String> results = searchGroups( "(objectClass=posixGroup)" );
        assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );
    }


    @Test
    public void testLessThanSearch() throws Exception
    {
        Set<String> results = searchGroups( "(gidNumber<=5)" );
        assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(gidNumber<=4)" );
        assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(gidNumber<=3)" );
        assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(gidNumber<=0)" );
        assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(gidNumber<=-1)" );
        assertFalse( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );
    }


    @Test
    public void testGreaterThanSearch() throws Exception
    {
        Set<String> results = searchGroups( "(gidNumber>=0)" );
        assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(gidNumber>=1)" );
        assertFalse( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(gidNumber>=3)" );
        assertFalse( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(gidNumber>=6)" );
        assertFalse( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );
    }


    @Test
    public void testNotOperator() throws Exception
    {
        Set<String> results = searchGroups( "(!(gidNumber=4))" );
        assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );
    }


    @Test
    public void testNotOperatorSubtree() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );

        Set<String> results = searchGroups( "(!(gidNumber=4))", controls );
        assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );
    }


    @Test
    public void testSearchWithEscapedCharsInFilter() throws Exception
    {
        // Create entry cn=Sid Vicious, ou=system
        Attributes vicious = LdifUtils.createAttributes( "objectClass: top", "objectClass: person", "cn: Sid Vicious",
            "sn: Vicious", "description: (sex*pis\\tols)" );

        DirContext ctx = sysRoot.createSubcontext( "cn=Sid Vicious", vicious );
        assertNotNull( ctx );

        ctx = ( DirContext ) sysRoot.lookup( "cn=Sid Vicious" );
        assertNotNull( ctx );

        Attributes attributes = ctx.getAttributes( "" );

        assertEquals( "(sex*pis\\tols)", attributes.get( "description" ).get() );

        // Now, search for the description
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setDerefLinkFlag( false );

        controls.setReturningAttributes( new String[]
            { "*" } );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, AliasDerefMode.NEVER_DEREF_ALIASES
            .getJndiValue() );
        HashMap<String, Attributes> map = new HashMap<String, Attributes>();

        NamingEnumeration<SearchResult> list = sysRoot
            .search( "", "(description=\\28sex\\2Apis\\5Ctols\\29)", controls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            map.put( result.getName(), result.getAttributes() );
        }

        assertEquals( "Expected number of results returned was incorrect!", 1, map.size() );

        Attributes attrs = map.get( "cn=Sid Vicious,ou=system" );

        assertNotNull( attrs.get( "objectClass" ) );
        assertNotNull( attrs.get( "cn" ) );
    }


    @Test
    public void testSubstringSearchWithEscapedCharsInFilter() throws Exception
    {
        // Create entry cn=Sid Vicious, ou=system
        Attributes vicious = LdifUtils.createAttributes( "objectClass: top", "objectClass: person", "cn: Sid Vicious",
            "sn: Vicious", "description: (sex*pis\\tols)" );

        DirContext ctx = sysRoot.createSubcontext( "cn=Sid Vicious", vicious );
        assertNotNull( ctx );

        ctx = ( DirContext ) sysRoot.lookup( "cn=Sid Vicious" );
        assertNotNull( ctx );

        Attributes attributes = ctx.getAttributes( "" );

        assertEquals( "(sex*pis\\tols)", attributes.get( "description" ).get() );

        // Now, search for the description
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setDerefLinkFlag( false );
        controls.setReturningAttributes( new String[]
            { "*" } );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, AliasDerefMode.NEVER_DEREF_ALIASES
            .getJndiValue() );

        String[] filters = new String[]
            { "(description=*\\28*)", "(description=*\\29*)", "(description=*\\2A*)", "(description=*\\5C*)" };
        for ( String filter : filters )
        {
            HashMap<String, Attributes> map = new HashMap<String, Attributes>();
            NamingEnumeration<SearchResult> list = sysRoot.search( "", filter, controls );

            while ( list.hasMore() )
            {
                SearchResult result = list.next();
                map.put( result.getName(), result.getAttributes() );
            }

            assertEquals( "Expected number of results returned was incorrect!", 1, map.size() );

            Attributes attrs = map.get( "cn=Sid Vicious,ou=system" );

            assertNotNull( attrs.get( "objectClass" ) );
            assertNotNull( attrs.get( "cn" ) );
        }
    }


    @Test
    public void testSubstringSearchWithEscapedAsterisksInFilter_DIRSERVER_1181() throws Exception
    {
        Attributes vicious = LdifUtils.createAttributes( "objectClass: top", "objectClass: person", "cn: x*y*z*",
            "sn: x*y*z*", "description: (sex*pis\\tols)" );

        sysRoot.createSubcontext( "cn=x*y*z*", vicious );

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        controls.setReturningAttributes( new String[]
            { "cn" } );
        NamingEnumeration<SearchResult> res;

        res = sysRoot.search( "", "(cn=*x\\2Ay\\2Az\\2A*)", controls );
        assertTrue( res.hasMore() );
        assertEquals( "x*y*z*", res.next().getAttributes().get( "cn" ).get() );
        assertFalse( res.hasMore() );

        res = sysRoot.search( "", "(cn=*{0}*)", new String[]
            { "x*y*z*" }, controls );
        assertTrue( res.hasMore() );
        assertEquals( "x*y*z*", res.next().getAttributes().get( "cn" ).get() );
        assertFalse( res.hasMore() );
    }


    /**
     * Test a search with a bad filter : there is a missing closing ')'
     */
    @Test
    public void testBadFilter() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setDerefLinkFlag( false );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, AliasDerefMode.NEVER_DEREF_ALIASES
            .getJndiValue() );

        try
        {
            sysRoot.search( "", "(|(name=testing00)(name=testing01)", controls );
            fail();
        }
        catch ( InvalidSearchFilterException isfe )
        {
            assertTrue( true );
        }
    }


    /**
     * Search operation with a base Dn with quotes
     * Commented as it's not valid by RFC 5514
    @Test
    public void testSearchWithQuotesInBase() throws NamingException
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        SearchControls ctls = new SearchControls();
        ctls.setSearchScope( SearchControls.OBJECT_SCOPE );
        String filter = "(cn=Tori Amos)";
        ctls.setReturningAttributes( new String[]
            { "cn", "cn" } );

        // Search for cn="Tori Amos" (with quotes)
        String base = "cn=\"Tori Amos\"";

        try {
            // Check entry
            NamingEnumeration<SearchResult> result = sysRoot.search( base, filter, ctls );
            assertTrue( result.hasMore() );

            while ( result.hasMore() )
            {
                SearchResult sr = result.next();
                Attributes attrs = sr.getAttributes();
                Attribute sn = attrs.get( "cn" );
                assertNotNull(sn);
                assertTrue( sn.contains( "Amos" ) );
            }
        } catch (Exception e)
        {
            fail( e.getMessage() );
        }
    }
    */

    /**
    * Added to test correct comparison of integer attribute types when searching.
    * testGreaterThanSearch and testLesserThanSearch fail to detect the problem using values less than 10.
    * Ref. DIRSERVER-1296
    *
    * @throws Exception
    */
    @Test
    public void testIntegerComparison() throws Exception
    {
        Set<String> results = searchUnits( "(&(objectClass=organizationalUnit)(integerAttribute<=2))", null );
        assertTrue( results.contains( "ou=testing00,ou=system" ) );
        assertTrue( results.contains( "ou=testing01,ou=system" ) );
        assertTrue( results.contains( "ou=testing02,ou=system" ) );
        assertFalse( results.contains( "ou=testing03,ou=system" ) );
        assertFalse( results.contains( "ou=testing04,ou=system" ) );
        assertFalse( results.contains( "ou=testing05,ou=system" ) );
    }


    /**
     * Added to test correct comparison of integer attribute types when searching.
     * testGreaterThanSearch and testLesserThanSearch fail to detect the problem using values less than 10.
     * Ref. DIRSERVER-1296
     *
     * @throws Exception
     */
    @Test
    public void testIntegerComparison2() throws Exception
    {
        Set<String> results = searchUnits( "(&(objectClass=organizationalUnit)(integerAttribute>=3))", null );
        assertFalse( results.contains( "ou=testing00,ou=system" ) );
        assertFalse( results.contains( "ou=testing01,ou=system" ) );
        assertFalse( results.contains( "ou=testing02,ou=system" ) );
        assertTrue( results.contains( "ou=testing03,ou=system" ) );
        assertTrue( results.contains( "ou=testing04,ou=system" ) );
        assertTrue( results.contains( "ou=testing05,ou=system" ) );
    }


    /**
     * Added to test correct comparison of integer attribute types when searching.
     * testGreaterThanSearch and testLesserThanSearch fail to detect the problem using values less than 10.
     * Ref. DIRSERVER-1296
     *
     * @throws Exception
     */
    @Test
    public void testIntegerComparison3() throws Exception
    {
        Set<String> results = searchUnits( "(&(objectClass=organizationalUnit)(integerAttribute<=42))", null );
        assertTrue( results.contains( "ou=testing00,ou=system" ) );
        assertTrue( results.contains( "ou=testing01,ou=system" ) );
        assertTrue( results.contains( "ou=testing02,ou=system" ) );
        assertTrue( results.contains( "ou=testing03,ou=system" ) );
        assertTrue( results.contains( "ou=testing04,ou=system" ) );
        assertTrue( results.contains( "ou=testing05,ou=system" ) );
    }


    /**
     * Added to test correct comparison of integer attribute types when searching.
     * testGreaterThanSearch and testLesserThanSearch fail to detect the problem using values less than 10.
     * Ref. DIRSERVER-1296
     *
     * @throws Exception
     */
    @Test
    public void testIntegerComparison4() throws Exception
    {
        Set<String> results = searchUnits(
            "(&(objectClass=organizationalUnit)(|(integerAttribute<=1)(integerAttribute>=5)))", null );
        assertTrue( results.contains( "ou=testing00,ou=system" ) );
        assertTrue( results.contains( "ou=testing01,ou=system" ) );
        assertFalse( results.contains( "ou=testing02,ou=system" ) );
        assertFalse( results.contains( "ou=testing03,ou=system" ) );
        assertFalse( results.contains( "ou=testing04,ou=system" ) );
        assertTrue( results.contains( "ou=testing05,ou=system" ) );
    }


    @Test
    public void testSearchTelephoneNumber() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );

        HashMap<String, Attributes> map = new HashMap<String, Attributes>();

        NamingEnumeration<SearchResult> list = sysRoot.search( "", "(telephoneNumber=18015551212)", controls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            map.put( result.getName(), result.getAttributes() );
        }

        assertEquals( "Expected number of results returned was incorrect!", 2, map.size() );
        assertTrue( map.containsKey( "cn=Heather Nova, ou=system" ) || map.containsKey( "cn=Heather Nova,ou=system" ) );
    }


    @Test
    public void testSearchDN() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setDerefLinkFlag( false );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, AliasDerefMode.NEVER_DEREF_ALIASES
            .getJndiValue() );

        HashMap<String, Attributes> map = new HashMap<String, Attributes>();

        NamingEnumeration<SearchResult> list = sysRoot.search( "", "(manager=cn=Heather Nova, ou=system)", controls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            map.put( result.getName(), result.getAttributes() );
        }

        assertEquals( "Expected number of results returned was incorrect", 1, map.size() );
        assertTrue( map.containsKey( "cn=with-dn, ou=system" ) || map.containsKey( "cn=with-dn,ou=system" ) );
    }


    @Test
    public void testComplexFilter() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setDerefLinkFlag( false );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, AliasDerefMode.NEVER_DEREF_ALIASES
            .getJndiValue() );

        // Create an entry which does not match
        Attributes attrs = LdifUtils.createAttributes( "objectClass: top", "objectClass: groupOfUniqueNames",
            "cn: testGroup3", "uniqueMember: uid=admin,ou=system" );

        getSystemContext( service ).createSubcontext( "cn=testGroup3,ou=groups", attrs );

        HashMap<String, Attributes> map = new HashMap<String, Attributes>();
        String filter = "(|(&(|(2.5.4.0=posixgroup)(2.5.4.0=groupofuniquenames)(2.5.4.0=groupofnames)(2.5.4.0=group))(!(|(2.5.4.50=uid=admin,ou=system)(2.5.4.31=0.9.2342.19200300.100.1.1=admin,2.5.4.11=system))))(objectClass=referral))";
        NamingEnumeration<SearchResult> list = sysRoot.search( "", filter, controls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            map.put( result.getName(), result.getAttributes() );
        }

        assertEquals( "size of results", 5, map.size() );
        assertTrue( map.containsKey( "cn=testGroup0,ou=groups,ou=system" ) );
        assertTrue( map.containsKey( "cn=testGroup1,ou=groups,ou=system" ) );
        assertTrue( map.containsKey( "cn=testGroup2,ou=groups,ou=system" ) );
        assertTrue( map.containsKey( "cn=testGroup4,ou=groups,ou=system" ) );
        assertTrue( map.containsKey( "cn=testGroup5,ou=groups,ou=system" ) );
        assertFalse( map.containsKey( "cn=testGroup3,ou=groups,ou=system" ) );
    }


    /**
     *  NO attributes should be returned
     */
    @Test
    public void testSearchTypesOnlyAndNoAttr() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        controls.setDerefLinkFlag( false );
        controls.setReturningAttributes( new String[]
            { "1.1" } );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, AliasDerefMode.NEVER_DEREF_ALIASES
            .getJndiValue() );
        sysRoot.addToEnvironment( "java.naming.ldap.typesOnly", "true" );

        HashMap<String, Attributes> map = new HashMap<String, Attributes>();

        NamingEnumeration<SearchResult> list = sysRoot.search( "", "(ou=testing01)", controls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            map.put( result.getName(), result.getAttributes() );
        }

        assertEquals( "Expected number of results returned was incorrect!", 1, map.size() );

        Attributes attrs = map.get( "ou=testing01,ou=system" );

        assertEquals( 0, attrs.size() );
    }


    /**
     * operational attributes with no values must be returned
     */
    @Test
    public void testSearchTypesOnlyWithNoAttrAndOperationalAttr() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        controls.setDerefLinkFlag( false );
        controls.setReturningAttributes( new String[]
            { "1.1", "+" } );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, AliasDerefMode.NEVER_DEREF_ALIASES
            .getJndiValue() );
        sysRoot.addToEnvironment( "java.naming.ldap.typesOnly", "true" );

        HashMap<String, Attributes> map = new HashMap<String, Attributes>();

        NamingEnumeration<SearchResult> list = sysRoot.search( "", "(ou=testing01)", controls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            map.put( result.getName(), result.getAttributes() );
        }

        assertEquals( "Expected number of results returned was incorrect!", 1, map.size() );

        Attributes attrs = map.get( "ou=testing01,ou=system" );

        assertNotNull( attrs.get( SchemaConstants.ENTRY_UUID_AT ) );
        assertNotNull( attrs.get( SchemaConstants.CREATORS_NAME_AT ) );

        assertEquals( 0, attrs.get( SchemaConstants.ENTRY_UUID_AT ).size() );
        assertEquals( 0, attrs.get( SchemaConstants.CREATORS_NAME_AT ).size() );
    }


    /**
     * all user attributes with no values must be returned
     */
    @Test
    public void testSearchTypesOnlyWithNullReturnAttr() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        controls.setDerefLinkFlag( false );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, AliasDerefMode.NEVER_DEREF_ALIASES
            .getJndiValue() );
        sysRoot.addToEnvironment( "java.naming.ldap.typesOnly", "true" );

        HashMap<String, Attributes> map = new HashMap<String, Attributes>();

        NamingEnumeration<SearchResult> list = sysRoot.search( "", "(ou=testing01)", controls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            map.put( result.getName(), result.getAttributes() );
        }

        assertEquals( "Expected number of results returned was incorrect!", 1, map.size() );

        Attributes attrs = map.get( "ou=testing01,ou=system" );

        assertNotNull( attrs.get( SchemaConstants.OU_AT ) );
        assertNotNull( attrs.get( "integerAttribute" ) );

        assertEquals( 0, attrs.get( SchemaConstants.OU_AT ).size() );
        assertEquals( 0, attrs.get( "integerAttribute" ).size() );

        assertNull( attrs.get( SchemaConstants.ENTRY_UUID_AT ) );
        assertNull( attrs.get( SchemaConstants.CREATORS_NAME_AT ) );
    }


    @Test
    public void testSearchEmptyDNWithOneLevelScope() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        controls.setDerefLinkFlag( false );

        LdapContext nullRootCtx = getRootContext( service );

        NamingEnumeration<SearchResult> list = nullRootCtx.search( "", "(objectClass=*)", controls );
        HashMap<String, Attributes> map = new HashMap<String, Attributes>();

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            map.put( result.getName(), result.getAttributes() );
        }

        assertEquals( 2, map.size() );

        assertTrue( map.containsKey( "ou=system" ) );
        assertTrue( map.containsKey( "ou=schema" ) );
    }


    @Test
    public void testSearchEmptyDNWithSubLevelScope() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setDerefLinkFlag( false );

        LdapContext nullRootCtx = getRootContext( service );

        NamingEnumeration<SearchResult> list = nullRootCtx.search( "", "(objectClass=organizationalUnit)", controls );
        HashMap<String, Attributes> map = new HashMap<String, Attributes>();

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            map.put( result.getName(), result.getAttributes() );
        }

        assertTrue( map.size() > 2 );

        assertTrue( map.containsKey( "ou=system" ) );
        assertTrue( map.containsKey( "ou=schema" ) );
    }


    @Test
    public void testSearchEmptyDNWithObjectScopeAndNoObjectClassPresenceFilter() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        controls.setDerefLinkFlag( false );

        LdapContext nullRootCtx = getRootContext( service );

        NamingEnumeration<SearchResult> list = nullRootCtx.search( "", "(objectClass=domain)", controls );
        HashMap<String, Attributes> map = new HashMap<String, Attributes>();

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            map.put( result.getName(), result.getAttributes() );
        }

        assertEquals( 0, map.size() );

        assertFalse( map.containsKey( "ou=system" ) );
        assertFalse( map.containsKey( "ou=schema" ) );
    }


    @Test
    public void testSearchEmptyDNWithOneLevelScopeAndNoObjectClassPresenceFilter() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        controls.setDerefLinkFlag( false );

        LdapContext nullRootCtx = getRootContext( service );

        NamingEnumeration<SearchResult> list = nullRootCtx.search( "", "(cn=*)", controls );
        HashMap<String, Attributes> map = new HashMap<String, Attributes>();

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            map.put( result.getName(), result.getAttributes() );
        }

        assertEquals( 0, map.size() );

        assertFalse( map.containsKey( "ou=system" ) );
        assertFalse( map.containsKey( "ou=schema" ) );
    }


    @Test
    public void testCsnLessEqualitySearch() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( service );

        Dn dn = new Dn( "cn=testLowerCsnAdd,ou=system" );
        Entry entry = new DefaultEntry( dn );
        entry.add( "objectClass", SchemaConstants.PERSON_OC );
        entry.add( "cn", "testLowerCsnAdd_cn" );
        entry.add( "sn", "testLowerCsnAdd_sn" );

        connection.add( entry );

        // add an entry to have a entry with higher CSN value
        Dn dn2 = new Dn( "cn=testHigherCsnAdd,ou=system" );
        Entry entry2 = new DefaultEntry( dn2 );
        entry2.add( "objectClass", SchemaConstants.PERSON_OC );
        entry2.add( "cn", "testHigherCsnAdd_cn" );
        entry2.add( "sn", "testHigherCsnAdd_sn" );

        connection.add( entry2 );

        entry = connection.lookup( dn.getName(), "+" );
        entry2 = connection.lookup( dn2.getName(), "+" );

        String lowerCsn = entry.get( "entryCsn" ).getString();
        String higherCsn = entry2.get( "entryCsn" ).getString();

        // usecases
        // 1.1 Less than or Equal ( with the lower csn value)
        testUseCases( lowerCsn, new String[]
            { lowerCsn }, connection, 1 );

        // 1.2 Less than or equals with a highest csn value
        testUseCases( higherCsn, new String[]
            { higherCsn, lowerCsn }, connection, 1 );

        // 2.1 Greater than or Equal ( with the highest csn value )
        testUseCases( higherCsn, new String[]
            { higherCsn }, connection, 2 );

        // 2.2 Greater than or Equal ( with lower csn value )
        testUseCases( lowerCsn, new String[]
            { higherCsn, lowerCsn }, connection, 2 );
    }


    private void testUseCases( String filterCsnVal, String[] expectedCsns, LdapConnection connection, int useCaseNum )
        throws Exception
    {
        Value<String> val = new StringValue( filterCsnVal );
        AttributeType entryCsnAt = service.getSchemaManager().getAttributeType( SchemaConstants.ENTRY_CSN_AT );
        ExprNode filter = null;

        if ( useCaseNum == 1 )
        {
            filter = new LessEqNode( entryCsnAt, val );
        }
        else if ( useCaseNum == 2 )
        {
            filter = new GreaterEqNode( entryCsnAt, val );
        }

        Entry loadedEntry = null;

        Set<String> csnSet = new HashSet<String>( expectedCsns.length );
        Cursor<Response> cursor = connection.search( "ou=system", filter.toString(), SearchScope.ONELEVEL, "*", "+" );
        while ( cursor.next() )
        {
            loadedEntry = ( ( SearchResultEntry ) cursor.get() ).getEntry();
            csnSet.add( loadedEntry.get( SchemaConstants.ENTRY_CSN_AT ).getString() );
        }
        cursor.close();

        assertTrue( csnSet.size() >= expectedCsns.length );

        for ( String csn : expectedCsns )
        {
            assertTrue( csnSet.contains( csn ) );
        }
    }


    @Test
    public void testSearchFilterWithBadAttributeType() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        controls.setDerefLinkFlag( false );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, AliasDerefMode.NEVER_DEREF_ALIASES
            .getJndiValue() );
        HashMap<String, Attributes> map = new HashMap<String, Attributes>();

        NamingEnumeration<SearchResult> list = sysRoot.search( "", "(|(badAttr={0})(ou={1}))", new Object[]
            { "testing00", "testing01" }, controls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            map.put( result.getName(), result.getAttributes() );
        }

        assertEquals( "Expected number of results returned was incorrect!", 1, map.size() );
        assertTrue( map.containsKey( "ou=testing01,ou=system" ) );
    }


    @Test
    public void testSearchFilterBadAttributeType() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        controls.setDerefLinkFlag( false );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, AliasDerefMode.NEVER_DEREF_ALIASES
            .getJndiValue() );
        HashMap<String, Attributes> map = new HashMap<String, Attributes>();

        NamingEnumeration<SearchResult> list = sysRoot.search( "", "(badAttr=*)", controls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            map.put( result.getName(), result.getAttributes() );
        }

        assertEquals( "Expected number of results returned was incorrect!", 0, map.size() );
    }
}
