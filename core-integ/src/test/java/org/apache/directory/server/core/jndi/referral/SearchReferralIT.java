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
package org.apache.directory.server.core.jndi.referral;


import static org.apache.directory.server.core.integ.IntegrationUtils.getContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;

import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.PartialResultException;
import javax.naming.ReferralException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapPartialResultException;
import org.apache.directory.shared.ldap.model.exception.LdapReferralException;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests the referral handling functionality for the Search operation 
 * within the server's core.
 * 
 * All the tests are described on this page :
 * http://cwiki.apache.org/confluence/display/DIRxSRVx11/Referral+Handling+Changes
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "SearchReferralIT")
@ApplyLdifs(
    {
        // Root
        "dn: c=WW,ou=system",
        "objectClass: country",
        "objectClass: top",
        "c: WW",

        // Sub-root
        "dn: o=MNN,c=WW,ou=system",
        "objectClass: organization",
        "objectClass: top",
        "o: MNN",

        // Another Sub-root
        "dn: o=PNN,c=WW,ou=system",
        "objectClass: organization",
        "objectClass: top",
        "o: MNN",

        // Referral #1
        "dn: ou=Roles,o=MNN,c=WW,ou=system",
        "objectClass: extensibleObject",
        "objectClass: referral",
        "objectClass: top",
        "ou: Roles",
        "ref: ldap://hostd/ou=Roles,dc=apache,dc=org",

        // Referral #2
        "dn: ou=People,o=MNN,c=WW,ou=system",
        "objectClass: extensibleObject",
        "objectClass: referral",
        "objectClass: top",
        "ou: People",
        "ref: ldap://hostb/OU=People,DC=example,DC=com",
        "ref: ldap://hostc/OU=People,O=MNN,C=WW",

        // Entry # 1
        "dn: cn=Alex Karasulu,o=MNN,c=WW,ou=system",
        "objectClass: person",
        "objectClass: top",
        "cn: Alex Karasulu",
        "sn: akarasulu",

        // Entry # 2
        "dn: cn=Alex,o=MNN,c=WW,ou=system",
        "objectClass: person",
        "objectClass: top",
        "cn: Alex",
        "sn: akarasulu"
})
public class SearchReferralIT extends AbstractLdapTestUnit
{

/** The Context we are using to inject entries with JNDI */
LdapContext MNNCtx;

/** The entries we are using to do the tests */
Attributes userEntry;
Entry serverEntry;


@Before
public void setUp() throws Exception
{
    MNNCtx = getContext( ServerDNConstants.ADMIN_SYSTEM_DN, getService(), "o=MNN,c=WW,ou=system" );

    // JNDI entry
    userEntry = new BasicAttributes( "objectClass", "top", true );
    userEntry.get( "objectClass" ).add( "person" );
    userEntry.put( "sn", "elecharny" );
    userEntry.put( "cn", "Emmanuel Lecharny" );

    // Core API entry
    Dn dn = new Dn( "cn=Emmanuel Lecharny, ou=apache, ou=people, o=MNN, c=WW, ou=system" );
    serverEntry = new DefaultEntry( getService().getSchemaManager(), dn );

    serverEntry.put( "ObjectClass", "top", "person" );
    serverEntry.put( "sn", "elecharny" );
    serverEntry.put( "cn", "Emmanuel Lecharny" );
}


/**
 * Test a search of a non existing entry (not a referral), with no referral 
 * in its ancestor.
 */
@Test
public void testSearchNotExistingSuperiorNoReferralAncestor() throws Exception
{
    Attributes attrs = new BasicAttributes( "ObjectClass", "top", true );

    try
    {
        MNNCtx.search( "cn=nobody", attrs );
        fail();
    }
    catch ( NameNotFoundException nnfe )
    {
        assertTrue( true );
    }
}


/**
 * Test a search of a non existing entry (not a referral), with a referral 
 * in its ancestor, using JNDI throw.
 */
@Test
public void testSearchWithReferralAncestorJNDIThrow() throws Exception
{
    Attributes attrs = new BasicAttributes( "ObjectClass", "top", true );

    try
    {
        MNNCtx.addToEnvironment( DirContext.REFERRAL, "throw" );
        MNNCtx.search( "ou=nobody,ou=apache,ou=roles", attrs );
        fail();
    }
    catch ( ReferralException re )
    {
        int nbRefs = 0;
        Set<String> expectedRefs = new HashSet<String>();
        expectedRefs.add( "ldap://hostd/ou=nobody,ou=apache,ou=Roles,dc=apache,dc=org??one" );

        do
        {
            String ref = ( String ) re.getReferralInfo();

            assertTrue( expectedRefs.contains( ref ) );
            nbRefs++;
        }
        while ( re.skipReferral() );

        assertEquals( 1, nbRefs );
    }
}


/**
 * Test a search of a non existing entry (not a referral), with a referral 
 * in its ancestor, using JNDI ignore.
 */
@Test
public void testSearchWithReferralAncestorJNDIIgnore() throws Exception
{
    Attributes attrs = new BasicAttributes( "ObjectClass", "top", true );

    try
    {
        MNNCtx.addToEnvironment( DirContext.REFERRAL, "ignore" );
        MNNCtx.search( "ou=nobody,ou=apache,ou=roles", attrs );
        fail();
    }
    catch ( PartialResultException pre )
    {
        assertTrue( true );
    }
}


/**
 * Test a search of a non existing entry (not a referral), with a referral 
 * in its ancestor, using the Core API without the ManageDsaIt flag.
 */
@Test
public void testSearchWithReferralAncestorCoreAPIWithoutManageDSAIt() throws Exception
{
    CoreSession coreSession = getService().getAdminSession();
    Dn dn = new Dn( "ou=nobody,ou=apache,ou=roles,o=Mnn,c=WW,ou=system" );

    try
    {
        coreSession.search( dn, "(ObjectClass=*)", false );
        fail();
    }
    catch ( LdapReferralException re )
    {
        int nbRefs = 0;
        Set<String> expectedRefs = new HashSet<String>();
        expectedRefs.add( "ldap://hostd/ou=nobody,ou=apache,ou=Roles,dc=apache,dc=org??base" );

        do
        {
            String ref = ( String ) re.getReferralInfo();

            assertTrue( expectedRefs.contains( ref ) );
            nbRefs++;
        }
        while ( re.skipReferral() );

        assertEquals( 1, nbRefs );
    }
}


/**
 * Test a search of a non existing entry (not a referral), with a referral 
 * in its ancestor, using the Core API with the ManageDsaIt flag.
 */
@Test
public void testSearchWithReferralAncestorCoreAPIWithManageDSAIt() throws Exception
{
    CoreSession coreSession = getService().getAdminSession();
    Dn dn = new Dn( "ou=nobody,ou=apache,ou=roles,o=Mnn,c=WW,ou=system" );

    try
    {
        coreSession.search( dn, "(ObjectClass=*)", true );
        fail();
    }
    catch ( LdapPartialResultException lpre )
    {
        assertTrue( true );
    }
}


/**
 * Test a search of an existing entry (not a referral).
 */
@Test
public void testSearchExistingNoReferral() throws Exception
{
    SearchControls sCtrls = new SearchControls();
    sCtrls.setReturningAttributes( new String[]
        { "*" } );
    sCtrls.setSearchScope( SearchControls.OBJECT_SCOPE );

    NamingEnumeration<SearchResult> result = MNNCtx.search( "cn=Alex Karasulu", "(ObjectClass=top)", sCtrls );

    assertNotNull( result );
    int nbRes = 0;

    while ( result.hasMoreElements() )
    {
        SearchResult entry = result.nextElement();
        assertNotNull( entry );
        assertNotNull( entry.getAttributes() );
        assertNotNull( entry.getAttributes().get( "cn" ) );
        assertEquals( "Alex Karasulu", entry.getAttributes().get( "cn" ).get() );
        nbRes++;
    }

    assertEquals( 1, nbRes );
}


/**
 * Test a search of an existing referral, using JNDI throw.
 */
@Test
public void testSearchExistingReferralJNDIThrow() throws Exception
{
    SearchControls sCtrls = new SearchControls();
    sCtrls.setReturningAttributes( new String[]
        { "*" } );
    sCtrls.setSearchScope( SearchControls.OBJECT_SCOPE );

    MNNCtx.addToEnvironment( DirContext.REFERRAL, "throw" );

    try
    {
        MNNCtx.search( "ou=Roles", "(ObjectClass=top)", sCtrls );
        fail();
    }
    catch ( ReferralException re )
    {
        int nbRefs = 0;
        Set<String> expectedRefs = new HashSet<String>();
        expectedRefs.add( "ldap://hostd/ou=Roles,dc=apache,dc=org??base" );

        do
        {
            String ref = ( String ) re.getReferralInfo();

            assertTrue( expectedRefs.contains( ref ) );
            nbRefs++;
        }
        while ( re.skipReferral() );

        assertEquals( 1, nbRefs );
    }
}


/**
 * Test a search of an existing referral, using JNDI ignore.
 */
@Test
public void testSearchExistingReferralJNDIIgnore() throws Exception
{
    SearchControls sCtrls = new SearchControls();
    sCtrls.setReturningAttributes( new String[]
        { "*" } );
    sCtrls.setSearchScope( SearchControls.OBJECT_SCOPE );

    MNNCtx.addToEnvironment( DirContext.REFERRAL, "ignore" );

    NamingEnumeration<SearchResult> result = MNNCtx.search( "ou=Roles", "(ObjectClass=top)", sCtrls );

    assertNotNull( result );
    int nbRes = 0;

    while ( result.hasMoreElements() )
    {
        SearchResult entry = result.nextElement();
        assertNotNull( entry );
        assertNotNull( entry.getAttributes() );
        assertNotNull( entry.getAttributes().get( "ou" ) );
        assertEquals( "Roles", entry.getAttributes().get( "ou" ).get() );
        nbRes++;
    }

    result.close();

    assertEquals( 1, nbRes );
}


/**
 * Test a search of an existing referral, using the Core API without the ManageDsaIt flag.
 */
@Test
public void testSearchExistingReferralCoreAPIWithoutManageDsaIT() throws Exception
{
    CoreSession coreSession = getService().getAdminSession();
    Dn dn = new Dn( "ou=roles,o=Mnn,c=WW,ou=system" );

    try
    {
        coreSession.search( dn, "(ObjectClass=*)", false );
        fail();
    }
    catch ( LdapReferralException re )
    {
        int nbRefs = 0;
        Set<String> expectedRefs = new HashSet<String>();
        expectedRefs.add( "ldap://hostd/ou=Roles,dc=apache,dc=org??base" );

        do
        {
            String ref = ( String ) re.getReferralInfo();

            assertTrue( expectedRefs.contains( ref ) );
            nbRefs++;
        }
        while ( re.skipReferral() );

        assertEquals( 1, nbRefs );
    }
}


/**
 * Test a search of an existing referral, using the Core API with the ManageDsaIt flag.
 */
@Test
public void testSearchExistingReferralCoreAPIWithManageDsaIT() throws Exception
{
    CoreSession coreSession = getService().getAdminSession();
    Dn dn = new Dn( "ou=roles,o=Mnn,c=WW,ou=system" );

    EntryFilteringCursor cursor = coreSession.search( dn, "(ObjectClass=*)", true );

    assertNotNull( cursor );

    cursor.beforeFirst();
    int nbRes = 0;

    while ( cursor.next() )
    {
        Entry entry = cursor.get();
        assertNotNull( entry );
        assertNotNull( entry.get( "ou" ) );
        assertEquals( "Roles", entry.get( "ou" ).getString() );
        nbRes++;
    }

    cursor.close();

    assertEquals( 1, nbRes );
}
}
