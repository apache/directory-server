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


import static org.apache.directory.server.core.integ.IntegrationUtils.getSchemaContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.LdapContext;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.cursor.EntryCursor;
import org.apache.directory.shared.ldap.model.cursor.SearchCursor;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.StringValue;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.filter.GreaterEqNode;
import org.apache.directory.shared.ldap.model.filter.LessEqNode;
import org.apache.directory.shared.ldap.model.message.SearchRequest;
import org.apache.directory.shared.ldap.model.message.SearchRequestImpl;
import org.apache.directory.shared.ldap.model.message.SearchResultEntry;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
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
    {
        "dn: m-oid=2.2.0, ou=attributeTypes, cn=apachemeta, ou=schema",
        "objectclass: metaAttributeType",
        "objectclass: metaTop",
        "objectclass: top",
        "m-oid: 2.2.0",
        "m-name: integerAttribute",
        "m-description: the precursor for all integer attributes",
        "m-equality: integerMatch",
        "m-ordering: integerOrderingMatch",
        "m-syntax: 1.3.6.1.4.1.1466.115.121.1.27",
        "m-length: 0",
        "",
        "dn: ou=testing00,ou=system",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "objectClass: extensibleObject",
        "ou: testing00",
        "integerAttribute: 0",
        "",
        "dn: ou=testing01,ou=system",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "objectClass: extensibleObject",
        "ou: testing01",
        "integerAttribute: 1",
        "",
        "dn: ou=testing02,ou=system",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "objectClass: extensibleObject",
        "ou: testing02",
        "integerAttribute: 2",
        "",
        "dn: ou=testing03,ou=system",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "objectClass: extensibleObject",
        "ou: testing03",
        "integerAttribute: 3",
        "",
        "dn: ou=testing04,ou=system",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "objectClass: extensibleObject",
        "ou: testing04",
        "integerAttribute: 4",
        "",
        "dn: ou=testing05,ou=system",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "objectClass: extensibleObject",
        "ou: testing05",
        "integerAttribute: 5",
        "",
        "dn: ou=subtest,ou=testing01,ou=system",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "ou: subtest",
        "",
        "dn: cn=Heather Nova, ou=system",
        "objectClass: top",
        "objectClass: person",
        "cn: Heather Nova",
        "sn: Nova",
        "telephoneNumber: 1 801 555 1212 ",
        "",
        "dn: cn=with-dn, ou=system",
        "objectClass: top",
        "objectClass: person",
        "objectClass: organizationalPerson",
        "objectClass: inetorgPerson",
        "cn: singer",
        "sn: manager",
        "telephoneNumber: 1 801 555 1212 ",
        "manager: cn=Heather Nova, ou=system"
})
public class SearchIT extends AbstractLdapTestUnit
{
private static final String RDN = "cn=Heather Nova,ou=system";
private static final String FILTER = "(objectclass=*)";

//public static LdapContext sysRoot;
public LdapConnection sysRoot;


/**
 * @param sysRoot the system root to add entries to
 * @throws NamingException on errors
 */
@Before
public void createData() throws Exception
{
    getService().getSchemaManager().enable( "nis" );

    sysRoot = IntegrationUtils.getAdminConnection( getService() );

    /*
     * Check ou=testing00,ou=system
     */
    Entry entry = sysRoot.lookup( "ou=testing00,ou=system" );
    assertNotNull( entry );

    assertNotNull( entry.getAttributes() );
    assertTrue( entry.contains( "ou", "testing00" ) );
    assertTrue( entry.contains( "objectClass", "top", "organizationalUnit" ) );

    /*
     * check ou=testing01,ou=system
     */
    entry = sysRoot.lookup( "ou=testing01,ou=system" );
    assertNotNull( entry );
    assertNotNull( entry.getAttributes() );
    assertTrue( entry.contains( "ou", "testing01" ) );
    assertTrue( entry.contains( "objectClass", "top", "organizationalUnit" ) );

    /*
     * Check ou=testing02,ou=system
     */
    entry = sysRoot.lookup( "ou=testing02,ou=system" );
    assertNotNull( entry );
    assertNotNull( entry.getAttributes() );
    assertTrue( entry.contains( "ou", "testing02" ) );
    assertTrue( entry.contains( "objectClass", "top", "organizationalUnit" ) );

    /*
     * Check ou=subtest,ou=testing01,ou=system
     */
    entry = sysRoot.lookup( "ou=subtest,ou=testing01,ou=system" );
    assertNotNull( entry );
    assertNotNull( entry.getAttributes() );
    assertTrue( entry.contains( "ou", "subtest" ) );
    assertTrue( entry.contains( "objectClass", "top", "organizationalUnit" ) );

    /*
     *  Check entry cn=Heather Nova, ou=system
     */
    entry = sysRoot.lookup( RDN );
    assertNotNull( entry );

    // -------------------------------------------------------------------
    // Enable the nis schema
    // -------------------------------------------------------------------

    // check if nis is disabled
    LdapContext schemaRoot = getSchemaContext( getService() );
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
private void addNisPosixGroup( String name, int gid ) throws Exception
{
    Entry entry = new DefaultEntry(
        "cn=" + name + ",ou=groups,ou=system",
        "objectClass: top",
        "objectClass: posixGroup",
        "cn", name,
        "gidNumber", String.valueOf( gid ) );

    sysRoot.add( entry );
}


@Test
public void testSearchOneLevel() throws Exception
{
    Set<String> set = new HashSet<String>();

    EntryCursor cursor = sysRoot.search( "ou=system", "(ou=*)", SearchScope.ONELEVEL, "*" );

    while ( cursor.next() )
    {
        Entry result = cursor.get();
        set.add( result.getDn().getName() );
    }

    cursor.close();

    assertEquals( "Expected number of results returned was incorrect!", 9, set.size() );
    assertTrue( set.contains( "ou=testing00,ou=system" ) );
    assertTrue( set.contains( "ou=testing01,ou=system" ) );
    assertTrue( set.contains( "ou=testing02,ou=system" ) );
}


@Test
public void testSearchSubTreeLevel() throws Exception
{
    Set<String> set = new HashSet<String>();

    EntryCursor cursor = sysRoot.search( "ou=system", "(ou=*)", SearchScope.SUBTREE, "*" );

    while ( cursor.next() )
    {
        Entry result = cursor.get();
        set.add( result.getDn().getName() );
    }

    cursor.close();

    assertEquals( "Expected number of results returned was incorrect", 14, set.size() );
    assertTrue( set.contains( "ou=system" ) );
    assertTrue( set.contains( "ou=testing00,ou=system" ) );
    assertTrue( set.contains( "ou=testing01,ou=system" ) );
    assertTrue( set.contains( "ou=testing02,ou=system" ) );
    assertTrue( set.contains( "ou=subtest,ou=testing01,ou=system" ) );
}


@Test
public void testSearchSubTreeLevelNoAttributes() throws Exception
{
    Map<String, Entry> map = new HashMap<String, Entry>();

    EntryCursor cursor = sysRoot.search( "ou=system", "(ou=testing02)", SearchScope.SUBTREE, "1.1" );

    while ( cursor.next() )
    {
        Entry result = cursor.get();
        map.put( result.getDn().getName(), result );
    }

    cursor.close();

    assertEquals( "Expected number of results returned was incorrect", 1, map.size() );
    assertTrue( map.containsKey( "ou=testing02,ou=system" ) );
    Entry entry = map.get( "ou=testing02,ou=system" );
    assertEquals( 0, entry.size() );
}


@Test
public void testSearchSubstringSubTreeLevel() throws Exception
{
    Set<String> set = new HashSet<String>();

    EntryCursor cursor = sysRoot.search( "ou=system", "(objectClass=organ*)", SearchScope.SUBTREE, "*" );

    while ( cursor.next() )
    {
        Entry result = cursor.get();
        set.add( result.getDn().getName() );
    }

    cursor.close();

    // 17 because it also matches organizationalPerson which the admin is
    assertEquals( "Expected number of results returned was incorrect", 17, set.size() );
    assertTrue( set.contains( "ou=system" ) );
    assertTrue( set.contains( "ou=configuration,ou=system" ) );
    assertTrue( set.contains( "ou=interceptors,ou=configuration,ou=system" ) );
    assertTrue( set.contains( "ou=partitions,ou=configuration,ou=system" ) );
    assertTrue( set.contains( "ou=services,ou=configuration,ou=system" ) );
    assertTrue( set.contains( "ou=groups,ou=system" ) );
    assertTrue( set.contains( "ou=testing00,ou=system" ) );
    assertTrue( set.contains( "ou=testing01,ou=system" ) );
    assertTrue( set.contains( "ou=subtest,ou=testing01,ou=system" ) );
    assertTrue( set.contains( "ou=testing02,ou=system" ) );
    assertTrue( set.contains( "ou=users,ou=system" ) );
    assertTrue( set.contains( "prefNodeName=sysPrefRoot,ou=system" ) );
    assertTrue( set.contains( "uid=admin,ou=system" ) );
}


/**
 * Tests to make sure undefined attributes in filter assertions are pruned and do not
 * result in exceptions.
 */
@Test
public void testBogusAttributeInSearchFilter() throws Exception
{
    boolean oldSetAllowAnnonymousAccess = getService().isAllowAnonymousAccess();
    getService().setAllowAnonymousAccess( true );

    EntryCursor cursor = sysRoot.search( "ou=system", "(bogusAttribute=abc123)", SearchScope.SUBTREE, "*" );

    assertNotNull( cursor );
    cursor.close();

    cursor = sysRoot.search( "ou=system", "(!(bogusAttribute=abc123))", SearchScope.SUBTREE, "*" );
    assertNotNull( cursor );
    assertFalse( cursor.next() );
    cursor.close();

    cursor = sysRoot
        .search( "ou=system", "(|(bogusAttribute=abc123)(bogusAttribute=abc123))", SearchScope.SUBTREE, "*" );
    assertNotNull( cursor );
    assertFalse( cursor.next() );
    cursor.close();

    cursor = sysRoot.search( "ou=system", "(|(bogusAttribute=abc123)(ou=abc123))", SearchScope.SUBTREE, "*" );
    assertNotNull( cursor );
    assertFalse( cursor.next() );
    cursor.close();

    cursor = sysRoot.search( "ou=system", "(OBJECTclass=*)", SearchScope.SUBTREE, "*" );
    assertNotNull( cursor );
    assertTrue( cursor.next() );
    cursor.close();

    cursor = sysRoot.search( "ou=system", "(objectclass=*)", SearchScope.SUBTREE, "*" );
    assertNotNull( cursor );
    cursor.close();

    getService().setAllowAnonymousAccess( oldSetAllowAnnonymousAccess );
}


@Test
public void testSearchFilterArgs() throws Exception
{
    Set<String> set = new HashSet<String>();

    EntryCursor cursor = sysRoot.search( "ou=system", "(|(ou=testing00)(ou=testing01))", SearchScope.ONELEVEL, "*" );

    while ( cursor.next() )
    {
        Entry result = cursor.get();
        set.add( result.getDn().getName() );
    }

    cursor.close();

    assertEquals( "Expected number of results returned was incorrect!", 2, set.size() );
    assertTrue( set.contains( "ou=testing00,ou=system" ) );
    assertTrue( set.contains( "ou=testing01,ou=system" ) );
}


@Test
public void testFilterExpansion0() throws Exception
{
    Map<String, Entry> map = new HashMap<String, Entry>();
    EntryCursor cursor = sysRoot.search( "ou=system", "(name=testing00)", SearchScope.SUBTREE, "*" );

    while ( cursor.next() )
    {
        Entry result = cursor.get();
        map.put( result.getDn().getName(), result );
    }

    cursor.close();

    assertEquals( "size of results", 1, map.size() );
    assertTrue( "contains ou=testing00,ou=system", map.containsKey( "ou=testing00,ou=system" ) );
}


@Test
public void testFilterExpansion1() throws Exception
{
    Map<String, Entry> map = new HashMap<String, Entry>();
    EntryCursor cursor = sysRoot.search( "ou=system", "(name=*)", SearchScope.SUBTREE, "*" );

    while ( cursor.next() )
    {
        Entry result = cursor.get();
        map.put( result.getDn().getName(), result );
    }

    cursor.close();

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
    Map<String, Entry> map = new HashMap<String, Entry>();
    EntryCursor cursor = sysRoot.search( "ou=system", "(|(name=testing00)(name=testing01))", SearchScope.SUBTREE, "*" );

    while ( cursor.next() )
    {
        Entry result = cursor.get();
        map.put( result.getDn().getName(), result );
    }

    cursor.close();

    assertEquals( "size of results", 2, map.size() );
    assertTrue( "contains ou=testing00,ou=system", map.containsKey( "ou=testing00,ou=system" ) );
    assertTrue( "contains ou=testing01,ou=system", map.containsKey( "ou=testing01,ou=system" ) );
}


@Test
public void testFilterExpansion4() throws Exception
{
    Map<String, Entry> map = new HashMap<String, Entry>();
    EntryCursor cursor = sysRoot.search( "ou=system", "(name=testing*)", SearchScope.SUBTREE, "*" );

    while ( cursor.next() )
    {
        Entry result = cursor.get();
        map.put( result.getDn().getName(), result );
    }

    cursor.close();

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
    String filter = "(|(2.5.4.11.1=testing*)(2.5.4.54=testing*)(2.5.4.10=testing*)"
        + "(2.5.4.6=testing*)(2.5.4.43=testing*)(2.5.4.7.1=testing*)(2.5.4.10.1=testing*)"
        + "(2.5.4.44=testing*)(2.5.4.11=testing*)(2.5.4.4=testing*)(2.5.4.8.1=testing*)"
        + "(2.5.4.12=testing*)(1.3.6.1.4.1.18060.0.4.1.2.3=testing*)"
        + "(2.5.4.7=testing*)(2.5.4.3=testing*)(2.5.4.8=testing*)(2.5.4.42=testing*))";

    Map<String, Entry> map = new HashMap<String, Entry>();
    EntryCursor cursor = sysRoot.search( "ou=system", filter, SearchScope.SUBTREE, "*" );

    while ( cursor.next() )
    {
        Entry result = cursor.get();
        map.put( result.getDn().getName(), result );
    }

    cursor.close();

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
    Map<String, Entry> map = new HashMap<String, Entry>();
    EntryCursor cursor = sysRoot.search( "ou=system", "(ou=testing00)", SearchScope.ONELEVEL, "creatorsName" );

    while ( cursor.next() )
    {
        Entry result = cursor.get();
        map.put( result.getDn().getName(), result );
    }

    cursor.close();

    assertEquals( "Expected number of results returned was incorrect!", 1, map.size() );
    assertTrue( map.containsKey( "ou=testing00,ou=system" ) );
    Entry entry = map.get( "ou=testing00,ou=system" );
    assertEquals( "normalized creator's name", "0.9.2342.19200300.100.1.1=admin,2.5.4.11=system", entry.get(
        "creatorsName" ).getString() );
}


@Test
public void testOpAttrDenormalizationOn() throws Exception
{
    getService().setDenormalizeOpAttrsEnabled( true );

    Map<String, Entry> map = new HashMap<String, Entry>();
    EntryCursor cursor = sysRoot.search( "ou=system", "(ou=testing00)", SearchScope.ONELEVEL, "creatorsName" );

    while ( cursor.next() )
    {
        Entry result = cursor.get();
        map.put( result.getDn().getName(), result );
    }

    cursor.close();

    assertEquals( "Expected number of results returned was incorrect!", 1, map.size() );
    assertTrue( map.containsKey( "ou=testing00,ou=system" ) );
    Entry entry = map.get( "ou=testing00,ou=system" );
    assertEquals( "normalized creator's name", "uid=admin,ou=system", entry.get( "creatorsName" ).getString() );
}


/**
 * Creation of required attributes of a person entry.
 *
 * @param cn the commonName of the person
 * @param sn the surName of the person
 * @return the attributes of a new person entry
 */
protected Entry getPersonEntry( String dn, String sn, String cn ) throws LdapException
{
    Entry entry = new DefaultEntry(
        dn,
        "objectClass: top",
        "objectClass: person",
        "cn", cn,
        "sn", sn );

    return entry;
}


@Test
public void testBinaryAttributesInFilter() throws Exception
{
    byte[] certData = new byte[]
        { 0x34, 0x56, 0x4e, 0x5f };

    // First let's add a some binary data representing a userCertificate
    Entry entry = getPersonEntry( "cn=Kate Bush,ou=system", "Bush", "Kate Bush" );
    entry.put( "userCertificate", certData );
    entry.add( "objectClass", "strongAuthenticationUser" );

    sysRoot.add( entry );

    // Search for kate by cn first
    EntryCursor cursor = sysRoot.search( "ou=system", "(cn=Kate Bush)", SearchScope.ONELEVEL, "*" );
    assertTrue( cursor.next() );
    entry = cursor.get();
    assertNotNull( entry );
    assertFalse( cursor.next() );
    assertEquals( "cn=Kate Bush,ou=system", entry.getDn().getName() );

    cursor.close();

    cursor = sysRoot.search( "ou=system", "(userCertificate=\\34\\56\\4E\\5F)", SearchScope.ONELEVEL, "*" );
    assertTrue( cursor.next() );
    entry = cursor.get();
    assertNotNull( entry );
    assertFalse( cursor.next() );
    assertEquals( "cn=Kate Bush,ou=system", entry.getDn().getName() );

    cursor.close();
}


@Test
public void testSearchOperationalAttr() throws Exception
{
    Map<String, Entry> map = new HashMap<String, Entry>();
    EntryCursor cursor = sysRoot.search( "ou=system", "(ou=testing01)", SearchScope.ONELEVEL, "+" );

    while ( cursor.next() )
    {
        Entry result = cursor.get();
        map.put( result.getDn().getName(), result );
    }

    cursor.close();

    assertEquals( "Expected number of results returned was incorrect!", 1, map.size() );

    Entry entry = map.get( "ou=testing01,ou=system" );

    assertNotNull( entry.get( "createTimestamp" ) );
    assertNotNull( entry.get( "creatorsName" ) );
    assertNull( entry.get( "objectClass" ) );
    assertNull( entry.get( "ou" ) );
}


@Test
public void testSearchUserAttr() throws Exception
{
    Map<String, Entry> map = new HashMap<String, Entry>();
    EntryCursor cursor = sysRoot.search( "ou=system", "(ou=testing01)", SearchScope.ONELEVEL, "*" );

    while ( cursor.next() )
    {
        Entry result = cursor.get();
        map.put( result.getDn().getName(), result );
    }

    cursor.close();

    assertEquals( "Expected number of results returned was incorrect!", 1, map.size() );

    Entry entry = map.get( "ou=testing01,ou=system" );

    assertNotNull( entry.get( "objectClass" ) );
    assertNotNull( entry.get( "ou" ) );
    assertNull( entry.get( "createTimestamp" ) );
    assertNull( entry.get( "creatorsName" ) );
}


@Test
public void testSearchUserAttrAndOpAttr() throws Exception
{
    Map<String, Entry> map = new HashMap<String, Entry>();
    EntryCursor cursor = sysRoot.search( "ou=system", "(ou=testing01)", SearchScope.ONELEVEL, "*", "creatorsName" );

    while ( cursor.next() )
    {
        Entry result = cursor.get();
        map.put( result.getDn().getName(), result );
    }

    cursor.close();

    assertEquals( "Expected number of results returned was incorrect!", 1, map.size() );

    Entry entry = map.get( "ou=testing01,ou=system" );

    assertNotNull( entry.get( "objectClass" ) );
    assertNotNull( entry.get( "ou" ) );
    assertNotNull( entry.get( "creatorsName" ) );
    assertNull( entry.get( "createTimestamp" ) );
}


@Test
public void testSearchUserAttrAndNoAttr() throws Exception
{
    Map<String, Entry> map = new HashMap<String, Entry>();
    EntryCursor cursor = sysRoot.search( "ou=system", "(ou=testing01)", SearchScope.ONELEVEL, "1.1", "ou" );

    while ( cursor.next() )
    {
        Entry result = cursor.get();
        map.put( result.getDn().getName(), result );
    }

    cursor.close();

    assertEquals( "Expected number of results returned was incorrect!", 1, map.size() );

    Entry entry = map.get( "ou=testing01,ou=system" );

    assertNull( entry.get( "objectClass" ) );
    assertNotNull( entry.get( "ou" ) );
    assertNull( entry.get( "creatorsName" ) );
    assertNull( entry.get( "createTimestamp" ) );
}


@Test
public void testSearchNoAttr() throws Exception
{
    Map<String, Entry> map = new HashMap<String, Entry>();
    EntryCursor cursor = sysRoot.search( "ou=system", "(ou=testing01)", SearchScope.ONELEVEL, "1.1" );

    while ( cursor.next() )
    {
        Entry result = cursor.get();
        map.put( result.getDn().getName(), result );
    }

    cursor.close();

    assertEquals( "Expected number of results returned was incorrect!", 1, map.size() );

    Entry entry = map.get( "ou=testing01,ou=system" );

    assertNull( entry.get( "objectClass" ) );
    assertNull( entry.get( "ou" ) );
    assertNull( entry.get( "creatorsName" ) );
    assertNull( entry.get( "createTimestamp" ) );
}


@Test
public void testSearchAllAttr() throws Exception
{
    Map<String, Entry> map = new HashMap<String, Entry>();
    EntryCursor cursor = sysRoot.search( "ou=system", "(ou=testing01)", SearchScope.ONELEVEL, "+", "*" );

    while ( cursor.next() )
    {
        Entry result = cursor.get();
        map.put( result.getDn().getName(), result );
    }

    cursor.close();

    assertEquals( "Expected number of results returned was incorrect!", 1, map.size() );

    Entry entry = map.get( "ou=testing01,ou=system" );

    assertNotNull( entry.get( "createTimestamp" ) );
    assertNotNull( entry.get( "creatorsName" ) );
    assertNotNull( entry.get( "objectClass" ) );
    assertNotNull( entry.get( "ou" ) );
}


/**
 * Search an entry and fetch an attribute with unknown option
 * @throws NamingException if there are errors
 */
@Test
public void testSearchFetchNonExistingAttributeOption() throws Exception
{
    EntryCursor cursor = sysRoot.search( RDN, FILTER, SearchScope.OBJECT, "cn", "sn;unknownOption", "badAttr" );

    if ( cursor.next() )
    {
        Entry entry = cursor.get();

        assertTrue( entry.contains( "cn", "Heather Nova" ) );
        assertFalse( entry.containsAttribute( "sn" ) );
    }
    else
    {
        fail( "entry " + RDN + " not found" );
    }

    cursor.close();
}


/**
 * Search an entry and fetch an attribute and all its subtypes
 * @throws NamingException if there are errors
 */
@Test
public void testSearchFetchAttributeAndSubTypes() throws Exception
{
    EntryCursor cursor = sysRoot.search( RDN, FILTER, SearchScope.OBJECT, "name" );

    if ( cursor.next() )
    {
        Entry entry = cursor.get();

        // We should have get cn and sn only
        assertEquals( 2, entry.size() );

        // Check CN
        assertTrue( entry.contains( "cn", "Heather Nova" ) );

        // Assert SN
        assertTrue( entry.contains( "sn", "Nova" ) );
    }
    else
    {
        fail( "entry " + RDN + " not found" );
    }

    cursor.close();
}


/**
 * Search an entry and fetch an attribute with twice the same attributeType
 * @throws NamingException if there are errors
 */
@Test
public void testSearchFetchTwiceSameAttribute() throws Exception
{
    EntryCursor cursor = sysRoot.search( RDN, FILTER, SearchScope.OBJECT, "cn", "cn" );

    if ( cursor.next() )
    {
        Entry entry = cursor.get();
        assertTrue( entry.contains( "cn", "Heather Nova" ) );
    }
    else
    {
        fail( "entry " + RDN + " not found" );
    }

    cursor.close();
}


// this one is failing because it returns the admin user twice: count = 15
@Test
public void testFilterExpansion3() throws Exception
{
    EntryCursor cursor = sysRoot.search( "ou=system", "(name=*)", SearchScope.SUBTREE, "*" );
    Set<String> set = new HashSet<String>();

    while ( cursor.next() )
    {
        Entry result = cursor.get();
        set.add( result.getDn().getName() );
    }

    cursor.close();

    assertEquals( "size of results", 23, set.size() );
    assertTrue( set.contains( "ou=system" ) );
    assertTrue( set.contains( "ou=groups,ou=system" ) );
    assertTrue( set.contains( "cn=Administrators,ou=groups,ou=system" ) );
    assertTrue( set.contains( "cn=testGroup0,ou=groups,ou=system" ) );
    assertTrue( set.contains( "cn=testGroup1,ou=groups,ou=system" ) );
    assertTrue( set.contains( "cn=testGroup2,ou=groups,ou=system" ) );
    assertTrue( set.contains( "cn=testGroup4,ou=groups,ou=system" ) );
    assertTrue( set.contains( "cn=testGroup5,ou=groups,ou=system" ) );
    assertTrue( set.contains( "ou=users,ou=system" ) );
    assertTrue( set.contains( "uid=admin,ou=system" ) );
    assertTrue( set.contains( "cn=Heather Nova,ou=system" ) );
    assertTrue( set.contains( "ou=configuration,ou=system" ) );
    assertTrue( set.contains( "ou=interceptors,ou=configuration,ou=system" ) );
    assertTrue( set.contains( "ou=partitions,ou=configuration,ou=system" ) );
    assertTrue( set.contains( "ou=services,ou=configuration,ou=system" ) );
    assertTrue( set.contains( "ou=testing00,ou=system" ) );
    assertTrue( set.contains( "ou=testing01,ou=system" ) );
    assertTrue( set.contains( "ou=subtest,ou=testing01,ou=system" ) );
    assertTrue( set.contains( "ou=testing02,ou=system" ) );
    assertTrue( set.contains( "ou=testing03,ou=system" ) );
    assertTrue( set.contains( "ou=testing04,ou=system" ) );
    assertTrue( set.contains( "ou=testing05,ou=system" ) );
    assertTrue( set.contains( "cn=with-dn,ou=system" ) );
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
public Set<String> searchGroups( String filter, SearchScope scope ) throws Exception
{
    Set<String> results = new HashSet<String>();

    EntryCursor cursor = sysRoot.search( "ou=groups,ou=system", filter, scope, "*" );

    while ( cursor.next() )
    {
        Entry result = cursor.get();
        results.add( result.getDn().getName() );
    }

    cursor.close();

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
private Set<String> searchUnits( String filter, SearchScope scope ) throws Exception
{
    Set<String> results = new HashSet<String>();
    EntryCursor cursor = sysRoot.search( "ou=system", filter, scope, "*" );

    while ( cursor.next() )
    {
        Entry result = cursor.get();
        results.add( result.getDn().getName() );
    }

    cursor.close();

    return results;
}


@Test
public void testSetup() throws Exception
{
    Set<String> results = searchGroups( "(objectClass=posixGroup)", SearchScope.ONELEVEL );
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
    Set<String> results = searchGroups( "(gidNumber<=5)", SearchScope.ONELEVEL );
    assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
    assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
    assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
    assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
    assertTrue( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
    assertTrue( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

    results = searchGroups( "(gidNumber<=4)", SearchScope.ONELEVEL );
    assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
    assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
    assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
    assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
    assertTrue( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
    assertFalse( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

    results = searchGroups( "(gidNumber<=3)", SearchScope.ONELEVEL );
    assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
    assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
    assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
    assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
    assertFalse( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
    assertFalse( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

    results = searchGroups( "(gidNumber<=0)", SearchScope.ONELEVEL );
    assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
    assertFalse( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
    assertFalse( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
    assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
    assertFalse( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
    assertFalse( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

    results = searchGroups( "(gidNumber<=-1)", SearchScope.ONELEVEL );
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
    Set<String> results = searchGroups( "(gidNumber>=0)", SearchScope.ONELEVEL );
    assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
    assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
    assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
    assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
    assertTrue( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
    assertTrue( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

    results = searchGroups( "(gidNumber>=1)", SearchScope.ONELEVEL );
    assertFalse( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
    assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
    assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
    assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
    assertTrue( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
    assertTrue( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

    results = searchGroups( "(gidNumber>=3)", SearchScope.ONELEVEL );
    assertFalse( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
    assertFalse( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
    assertFalse( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
    assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
    assertTrue( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
    assertTrue( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

    results = searchGroups( "(gidNumber>=6)", SearchScope.ONELEVEL );
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
    Set<String> results = searchGroups( "(!(gidNumber=4))", SearchScope.ONELEVEL );
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
    Set<String> results = searchGroups( "(!(gidNumber=4))", SearchScope.SUBTREE );
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
    Entry vicious = new DefaultEntry(
        "cn=Sid Vicious,ou=system",
        "objectClass: top",
        "objectClass: person",
        "cn: Sid Vicious",
        "sn: Vicious",
        "description: (sex*pis\\tols)" );

    sysRoot.add( vicious );

    assertTrue( sysRoot.exists( "cn=Sid Vicious,ou=system" ) );

    Entry entry = sysRoot.lookup( "cn=Sid Vicious,ou=system" );

    assertNotNull( entry );

    assertTrue( entry.contains( "description", "(sex*pis\\tols)" ) );

    // Now, search for the description
    EntryCursor cursor = sysRoot.search( "ou=system", "(description=\\28sex\\2Apis\\5Ctols\\29)", SearchScope.SUBTREE,
        "*" );
    Map<String, Entry> map = new HashMap<String, Entry>();

    while ( cursor.next() )
    {
        Entry result = cursor.get();
        map.put( result.getDn().getName(), result );
    }

    cursor.close();

    assertEquals( "Expected number of results returned was incorrect!", 1, map.size() );

    entry = map.get( "cn=Sid Vicious,ou=system" );

    assertNotNull( entry.get( "objectClass" ) );
    assertNotNull( entry.get( "cn" ) );
}


@Test
public void testSubstringSearchWithEscapedCharsInFilter() throws Exception
{
    // Create entry cn=Sid Vicious, ou=system
    Entry vicious = new DefaultEntry(
        "cn=Sid Vicious,ou=system",
        "objectClass: top",
        "objectClass: person",
        "cn: Sid Vicious",
        "sn: Vicious",
        "description: (sex*pis\\tols)" );

    sysRoot.add( vicious );

    assertTrue( sysRoot.exists( "cn=Sid Vicious,ou=system" ) );

    Entry entry = sysRoot.lookup( "cn=Sid Vicious,ou=system" );

    assertTrue( entry.contains( "description", "(sex*pis\\tols)" ) );

    // Now, search for the description
    String[] filters = new String[]
        { "(description=*\\28*)", "(description=*\\29*)", "(description=*\\2A*)", "(description=*\\5C*)" };

    for ( String filter : filters )
    {
        Map<String, Entry> map = new HashMap<String, Entry>();
        EntryCursor cursor = sysRoot.search( "ou=system", filter, SearchScope.ONELEVEL, "*" );

        while ( cursor.next() )
        {
            Entry result = cursor.get();
            map.put( result.getDn().getName(), result );
        }

        cursor.close();

        assertEquals( "Expected number of results returned was incorrect!", 1, map.size() );

        entry = map.get( "cn=Sid Vicious,ou=system" );

        assertNotNull( entry.get( "objectClass" ) );
        assertNotNull( entry.get( "cn" ) );
    }
}


@Test
public void testSubstringSearchWithEscapedAsterisksInFilter_DIRSERVER_1181() throws Exception
{
    Entry vicious = new DefaultEntry(
        "cn=x*y*z*,ou=system",
        "objectClass: top",
        "objectClass: person",
        "cn: x*y*z*",
        "sn: x*y*z*",
        "description: (sex*pis\\tols)" );

    sysRoot.add( vicious );

    EntryCursor cursor = sysRoot.search( "ou=system", "(cn=*x\\2Ay\\2Az\\2A*)", SearchScope.ONELEVEL, "cn" );

    assertTrue( cursor.next() );
    assertTrue( cursor.get().contains( "cn", "x*y*z*" ) );
    assertFalse( cursor.next() );

    cursor.close();

    cursor = sysRoot.search( "ou=system", "(cn=x*y*z*)", SearchScope.ONELEVEL, "cn" );
    assertTrue( cursor.next() );
    assertTrue( cursor.get().contains( "cn", "x*y*z*" ) );
    assertFalse( cursor.next() );

    cursor.close();
}


/**
 * Test a search with a bad filter : there is a missing closing ')'
 */
@Test
public void testBadFilter() throws Exception
{
    try
    {
        sysRoot.search( "ou=system", "(|(name=testing00)(name=testing01)", SearchScope.SUBTREE,
            "*" );
        fail();
    }
    catch ( LdapException le )
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
    LdapContext sysRoot = getSystemContext( getService() );
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
    Set<String> results = searchUnits( "(&(objectClass=organizationalUnit)(integerAttribute<=2))", SearchScope.ONELEVEL );
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
    Set<String> results = searchUnits( "(&(objectClass=organizationalUnit)(integerAttribute>=3))", SearchScope.ONELEVEL );
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
    Set<String> results = searchUnits( "(&(objectClass=organizationalUnit)(integerAttribute<=42))",
        SearchScope.ONELEVEL );
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
        "(&(objectClass=organizationalUnit)(|(integerAttribute<=1)(integerAttribute>=5)))", SearchScope.ONELEVEL );
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
    Map<String, Entry> map = new HashMap<String, Entry>();

    EntryCursor cursor = sysRoot.search( "ou=system", "(telephoneNumber=18015551212)", SearchScope.ONELEVEL,
        "*" );

    while ( cursor.next() )
    {
        Entry result = cursor.get();
        map.put( result.getDn().getName(), result );
    }

    cursor.close();

    assertEquals( "Expected number of results returned was incorrect!", 2, map.size() );
    assertTrue( map.containsKey( "cn=Heather Nova, ou=system" ) || map.containsKey( "cn=Heather Nova,ou=system" ) );
}


@Test
public void testSearchDN() throws Exception
{
    Map<String, Entry> map = new HashMap<String, Entry>();

    EntryCursor cursor = sysRoot.search( "ou=system", "(manager=cn=Heather Nova, ou=system)", SearchScope.SUBTREE,
        "*" );

    while ( cursor.next() )
    {
        Entry result = cursor.get();
        map.put( result.getDn().getName(), result );
    }

    cursor.close();

    assertEquals( "Expected number of results returned was incorrect", 1, map.size() );
    assertTrue( map.containsKey( "cn=with-dn, ou=system" ) || map.containsKey( "cn=with-dn,ou=system" ) );
}


@Test
public void testComplexFilter() throws Exception
{
    // Create an entry which does not match
    Entry entry = new DefaultEntry(
        "cn=testGroup3,ou=groups,ou=system",
        "objectClass: top",
        "objectClass: groupOfUniqueNames",
        "cn: testGroup3",
        "uniqueMember: uid=admin,ou=system" );

    sysRoot.add( entry );

    Map<String, Entry> map = new HashMap<String, Entry>();
    String filter = "(|(&(|(2.5.4.0=posixgroup)(2.5.4.0=groupofuniquenames)(2.5.4.0=groupofnames)(2.5.4.0=group))(!(|(2.5.4.50=uid=admin,ou=system)(2.5.4.31=0.9.2342.19200300.100.1.1=admin,2.5.4.11=system))))(objectClass=referral))";

    EntryCursor cursor = sysRoot.search( "ou=groups,ou=system", filter, SearchScope.ONELEVEL, "*" );

    while ( cursor.next() )
    {
        Entry result = cursor.get();
        map.put( result.getDn().getName(), result );
    }

    cursor.close();

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
    Map<String, Entry> map = new HashMap<String, Entry>();

    SearchRequest searchRequest = new SearchRequestImpl();
    searchRequest.setBase( new Dn( "ou=system" ) );
    searchRequest.setFilter( "(ou=testing01)" );
    searchRequest.setTypesOnly( true );
    searchRequest.setScope( SearchScope.ONELEVEL );
    searchRequest.addAttributes( "1.1" );

    SearchCursor cursor = sysRoot.search( searchRequest );

    while ( cursor.next() )
    {
        SearchResultEntry result = ( SearchResultEntry ) cursor.get();
        map.put( result.getEntry().getDn().getName(), result.getEntry() );
    }

    cursor.close();

    assertEquals( "Expected number of results returned was incorrect!", 1, map.size() );

    Entry entry = map.get( "ou=testing01,ou=system" );

    assertEquals( 0, entry.size() );
}


/**
 * operational attributes with no values must be returned
 */
@Test
public void testSearchTypesOnlyWithNoAttrAndOperationalAttr() throws Exception
{
    Map<String, Entry> map = new HashMap<String, Entry>();

    SearchRequest searchRequest = new SearchRequestImpl();
    searchRequest.setBase( new Dn( "ou=system" ) );
    searchRequest.setFilter( "(ou=testing01)" );
    searchRequest.setTypesOnly( true );
    searchRequest.setScope( SearchScope.ONELEVEL );
    searchRequest.addAttributes( "1.1", "+" );

    SearchCursor cursor = sysRoot.search( searchRequest );

    while ( cursor.next() )
    {
        SearchResultEntry result = ( SearchResultEntry ) cursor.get();
        map.put( result.getEntry().getDn().getName(), result.getEntry() );
    }

    cursor.close();

    assertEquals( "Expected number of results returned was incorrect!", 1, map.size() );

    Entry entry = map.get( "ou=testing01,ou=system" );

    assertNotNull( entry.get( "entryUuid" ) );
    assertNotNull( entry.get( "creatorsName" ) );

    assertEquals( 0, entry.get( "entryUuid" ).size() );
    assertEquals( 0, entry.get( "creatorsName" ).size() );
}


/**
 * all user attributes with no values must be returned
 */
@Test
public void testSearchTypesOnlyWithNullReturnAttr() throws Exception
{
    Map<String, Entry> map = new HashMap<String, Entry>();

    SearchRequest searchRequest = new SearchRequestImpl();
    searchRequest.setBase( new Dn( "ou=system" ) );
    searchRequest.setFilter( "(ou=testing01)" );
    searchRequest.setTypesOnly( true );
    searchRequest.setScope( SearchScope.ONELEVEL );

    SearchCursor cursor = sysRoot.search( searchRequest );

    while ( cursor.next() )
    {
        SearchResultEntry result = ( SearchResultEntry ) cursor.get();
        map.put( result.getEntry().getDn().getName(), result.getEntry() );
    }

    cursor.close();

    assertEquals( "Expected number of results returned was incorrect!", 1, map.size() );

    Entry entry = map.get( "ou=testing01,ou=system" );

    assertNotNull( entry.get( "ou" ) );
    assertNotNull( entry.get( "integerAttribute" ) );

    assertEquals( 0, entry.get( "ou" ).size() );
    assertEquals( 0, entry.get( "integerAttribute" ).size() );

    assertNull( entry.get( "entryUuid" ) );
    assertNull( entry.get( "creatorsName" ) );
}


@Test
public void testSearchEmptyDNWithOneLevelScope() throws Exception
{
    Map<String, Entry> map = new HashMap<String, Entry>();
    EntryCursor cursor = sysRoot.search( "", "(objectClass=*)", SearchScope.ONELEVEL, "*" );

    while ( cursor.next() )
    {
        Entry result = cursor.get();
        map.put( result.getDn().getName(), result );
    }

    cursor.close();

    assertEquals( 2, map.size() );

    assertTrue( map.containsKey( "ou=system" ) );
    assertTrue( map.containsKey( "ou=schema" ) );
}


@Test
public void testSearchEmptyDNWithSubLevelScope() throws Exception
{
    Map<String, Entry> map = new HashMap<String, Entry>();
    EntryCursor cursor = sysRoot.search( "", "(objectClass=organizationalUnit)", SearchScope.SUBTREE, "*" );

    while ( cursor.next() )
    {
        Entry result = cursor.get();
        map.put( result.getDn().getName(), result );
    }

    cursor.close();

    assertTrue( map.size() > 2 );

    assertTrue( map.containsKey( "ou=system" ) );
    assertTrue( map.containsKey( "ou=schema" ) );
}


@Test
public void testSearchEmptyDNWithObjectScopeAndNoObjectClassPresenceFilter() throws Exception
{
    Map<String, Entry> map = new HashMap<String, Entry>();
    EntryCursor cursor = sysRoot.search( "", "(objectClass=domain)", SearchScope.OBJECT, "*" );

    while ( cursor.next() )
    {
        Entry result = cursor.get();
        map.put( result.getDn().getName(), result );
    }

    cursor.close();

    assertEquals( 0, map.size() );

    assertFalse( map.containsKey( "ou=system" ) );
    assertFalse( map.containsKey( "ou=schema" ) );
}


@Test
public void testSearchEmptyDNWithOneLevelScopeAndNoObjectClassPresenceFilter() throws Exception
{
    Map<String, Entry> map = new HashMap<String, Entry>();
    EntryCursor cursor = sysRoot.search( "", "(cn=*)", SearchScope.ONELEVEL, "*" );

    while ( cursor.next() )
    {
        Entry result = cursor.get();
        map.put( result.getDn().getName(), result );
    }

    cursor.close();

    assertEquals( 0, map.size() );

    assertFalse( map.containsKey( "ou=system" ) );
    assertFalse( map.containsKey( "ou=schema" ) );
}


@Test
public void testCsnLessEqualitySearch() throws Exception
{
    LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

    Dn dn = new Dn( "cn=testLowerCsnAdd,ou=system" );
    Entry entry = new DefaultEntry( dn,
        "objectClass: person",
        "cn: testLowerCsnAdd_cn",
        "sn: testLowerCsnAdd_sn" );

    connection.add( entry );

    // add an entry to have a entry with higher CSN value
    Dn dn2 = new Dn( "cn=testHigherCsnAdd,ou=system" );
    Entry entry2 = new DefaultEntry( dn2,
        "objectClass: person",
        "cn: testHigherCsnAdd_cn",
        "sn: testHigherCsnAdd_sn" );

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
    AttributeType entryCsnAt = getService().getSchemaManager().getAttributeType( SchemaConstants.ENTRY_CSN_AT );
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
    EntryCursor cursor = connection.search( "ou=system", filter.toString(), SearchScope.ONELEVEL, "*", "+" );

    while ( cursor.next() )
    {
        loadedEntry = cursor.get();
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
    Map<String, Entry> map = new HashMap<String, Entry>();
    EntryCursor cursor = sysRoot
        .search( "ou=system", "(|(badAttr=testing00)(ou=testing01))", SearchScope.ONELEVEL, "*" );

    while ( cursor.next() )
    {
        Entry result = cursor.get();
        map.put( result.getDn().getName(), result );
    }

    cursor.close();

    assertEquals( "Expected number of results returned was incorrect!", 1, map.size() );
    assertTrue( map.containsKey( "ou=testing01,ou=system" ) );
}


@Test
public void testSearchFilterBadAttributeType() throws Exception
{
    Map<String, Entry> map = new HashMap<String, Entry>();
    EntryCursor cursor = sysRoot
        .search( "ou=system", "(badAttr=*)", SearchScope.ONELEVEL, "*" );

    while ( cursor.next() )
    {
        Entry result = cursor.get();
        map.put( result.getDn().getName(), result );
    }

    cursor.close();

    assertEquals( "Expected number of results returned was incorrect!", 0, map.size() );
}
}
