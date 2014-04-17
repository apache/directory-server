/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.shared.client.api.operations.search;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.directory.api.ldap.model.cursor.CursorLdapReferralException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.client.api.LdapApiIntegrationUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * A class to test the search operation with referrals
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP"), @CreateTransport(protocol = "LDAPS") })
@ApplyLdifs(
    {
        // Add new ref for ou=RemoteUsers
        "dn: ou=RemoteUsers,ou=system",
        "objectClass: top",
        "objectClass: referral",
        "objectClass: extensibleObject",
        "ou: RemoteUsers",
        "ref: ldap://fermi:10389/ou=users,ou=system",
        "ref: ldap://hertz:10389/ou=users,dc=example,dc=com",
        "ref: ldap://maxwell:10389/ou=users,ou=system",

        "dn: c=France,ou=system",
        "objectClass: top",
        "objectClass: country",
        "c: France",

        "dn: c=USA,ou=system",
        "objectClass: top",
        "objectClass: country",
        "c: USA",

        "dn: l=Paris,c=france,ou=system",
        "objectClass: top",
        "objectClass: locality",
        "l: Paris",

        "dn: l=Jacksonville,c=usa,ou=system",
        "objectClass: top",
        "objectClass: locality",
        "l: Jacksonville",

        "dn: cn=emmanuel lecharny,l=paris,c=france,ou=system",
        "objectClass: top",
        "objectClass: person",
        "objectClass: residentialPerson",
        "cn: emmanuel lecharny",
        "sn: elecharny",
        "l: Paris",

        "dn: cn=alex karasulu,l=jacksonville,c=usa,ou=system",
        "objectClass: top",
        "objectClass: person",
        "objectClass: residentialPerson",
        "cn: alex karasulu",
        "sn: karasulu",
        "l: Jacksonville",

        "dn: ou=Countries,ou=system",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "ou: Countries"
})
public class SearchWithReferralsTest extends AbstractLdapTestUnit
{
    private LdapNetworkConnection connection;
    
    
    @Before
    public void setupReferrals() throws Exception
    {
        String ldif =
            "dn: c=europ,ou=Countries,ou=system\n" +
                "objectClass: top\n" +
                "objectClass: referral\n" +
                "objectClass: extensibleObject\n" +
                "c: europ\n" +
                "ref: ldap://localhost:52489/c=france,ou=system\n\n" +
    
                "dn: c=america,ou=Countries,ou=system\n" +
                "objectClass: top\n" +
                "objectClass: referral\n" +
                "objectClass: extensibleObject\n" +
                "c: america\n" +
                "ref: ldap://localhost:52489/c=usa,ou=system\n\n";
    
        LdifReader reader = new LdifReader( new StringReader( ldif ) );
    
        while ( reader.hasNext() )
        {
            LdifEntry entry = reader.next();
            getLdapServer().getDirectoryService().getAdminSession().add(
                new DefaultEntry( getLdapServer().getDirectoryService().getSchemaManager(), entry.getEntry() ) );
        }
    
        reader.close();
    
        connection = ( LdapNetworkConnection ) LdapApiIntegrationUtils.getPooledAdminConnection( getLdapServer() );
    }
    
    
    @After
    public void shutdown() throws Exception
    {
        LdapApiIntegrationUtils.releasePooledAdminConnection( connection, getLdapServer() );
    }
    
    
    /**
     * Test of an search operation with a referral
     *
     * search for "cn=alex karasulu" on "c=america, ou=system"
     * we should get a referral URL thrown, which point to
     * "c=usa, ou=system", and ask for a subtree search
     */
    @Test
    public void testSearchWithReferralThrow() throws Exception
    {
        EntryCursor cursor = connection.search( "ou=Countries,ou=system", "(objectClass=*)",
            SearchScope.SUBTREE, "*", "+" );
        int count = 0;
        Entry entry = null;
        List<String> refs = new ArrayList<String>();
    
        while ( cursor.next() )
        {
            try
            {
                entry = cursor.get();
    
                assertNotNull( entry );
                count++;
            }
            catch ( CursorLdapReferralException clre )
            {
                count++;
    
                do
                {
                    String ref = clre.getReferralInfo();
                    refs.add( ref );
                }
                while ( clre.skipReferral() );
            }
        }
    
        assertEquals( 3, count );
        assertEquals( 2, refs.size() );
        assertTrue( refs.contains( "ldap://localhost:52489/c=usa,ou=system??sub" ) );
        assertTrue( refs.contains( "ldap://localhost:52489/c=france,ou=system??sub" ) );
        cursor.close();
    }
    
    
    /**
     * Test of an search operation with a referral and a follow
     *
     * search for "cn=alex karasulu" on "c=america, ou=system"
     * we should get a referral URL thrown, which point to
     * "c=usa, ou=system", and ask for a subtree search
     */
    @Test
    public void testSearchWithReferralAsIs() throws Exception
    {
        // We will ask the referal to be returned as is
        SearchRequest searchRequest = new SearchRequestImpl();
        searchRequest.setBase( new Dn( "ou=Countries,ou=system" ) );
        searchRequest.setFilter( "(objectClass=*)" );
        searchRequest.setScope( SearchScope.SUBTREE );
        searchRequest.addAttributes( "*", "+" );
        
        searchRequest.ignoreReferrals();
        
        SearchCursor cursor = connection.search( searchRequest );
        int count = 0;
        Entry entry = null;
        List<String> refs = new ArrayList<String>();
    
        while ( cursor.next() )
        {
            entry = cursor.getEntry();

            assertNotNull( entry );
            count++;
        }
    
        assertEquals( 3, count );
        assertEquals( 0, refs.size() );
        cursor.close();
    }
}
