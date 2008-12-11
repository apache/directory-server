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
package org.apache.directory.server.operations.search;

 
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.ReferralException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.ManageReferralControl;
import javax.naming.ldap.PagedResultsResponseControl;

import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.server.core.integ.annotations.ApplyLdifs;
import org.apache.directory.server.integ.SiRunner;

import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContextThrowOnRefferal;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContext;

import org.apache.directory.server.ldap.LdapService;
import org.apache.directory.shared.ldap.ldif.LdifEntry;
import org.apache.directory.shared.ldap.ldif.LdifReader;
import org.apache.directory.shared.ldap.message.control.PagedSearchControl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * Test the PagedSearchControl
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 545029 $
 */
@RunWith ( SiRunner.class )
@ApplyLdifs( {
    // Add 10 new entries
    "dn: dc=users,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: domain\n" +
    "dc: users\n" +
    "\n" +
    "dn: cn=user0,dc=users,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: person\n" +
    "cn: user0\n" +
    "sn: user 0\n" +
    "\n" +
    "dn: cn=user1,dc=users,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: person\n" +
    "cn: user1\n" +
    "sn: user 1\n" +
    "\n" +
    "dn: cn=user2,dc=users,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: person\n" +
    "cn: user2\n" +
    "sn: user 2\n" +
    "\n" +
    "dn: cn=user3,dc=users,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: person\n" +
    "cn: user3\n" +
    "sn: user 3\n" +
    "\n" +
    "dn: cn=user4,dc=users,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: person\n" +
    "cn: user4\n" +
    "sn: user 4\n" +
    "\n" +
    "dn: cn=user5,dc=users,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: person\n" +
    "cn: user5\n" +
    "sn: user 5\n" +
    "\n" +
    "dn: cn=user6,dc=users,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: person\n" +
    "cn: user6\n" +
    "sn: user 6\n" +
    "\n" +
    "dn: cn=user7,dc=users,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: person\n" +
    "cn: user7\n" +
    "sn: user 7\n" +
    "\n" +
    "dn: cn=user8,dc=users,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: person\n" +
    "cn: user8\n" +
    "sn: user 8\n" +
    "\n" +
    "dn: cn=user9,dc=users,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: person\n" +
    "cn: user9\n" +
    "sn: user 9\n" +
    "\n"
    }
)
public class PagedSearchIT
{
    public static LdapService ldapService;

    
    @Test
    public void testSearchPagedSearch5Entries() throws Exception
    {
        DirContext ctx = getWiredContext( ldapService );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        PagedSearchControl pagedSearchControl = new PagedSearchControl();
        pagedSearchControl.setSize( 5 );

        ((LdapContext)ctx).setRequestControls( new Control[] {pagedSearchControl} );
        
        NamingEnumeration<SearchResult> list = ctx.search( "dc=users,ou=system", "(cn=*)", controls );
        
        List<SearchResult> results = new ArrayList<SearchResult>();
        
        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            results.add( result );
        }
        
        assertEquals( 5, results.size() );
        
        // check that we have correctly read the 5 first entries
        for ( int i = 0; i < 5; i++ )
        {
            SearchResult entry = results.get( i );
            assertEquals( "user" + i, entry.getAttributes().get( "cn" ).get() );
        }
        
        // Now read the 5 next ones
        Control[] responseControls = ((LdapContext)ctx).getResponseControls();
        
        PagedResultsResponseControl responseControl = (PagedResultsResponseControl)responseControls[0];
        pagedSearchControl.setCookie( responseControl.getCookie() );
        ((LdapContext)ctx).setRequestControls( new Control[] {pagedSearchControl} );
        
        list = ctx.search( "dc=users,ou=system", "(cn=*)", controls );
        
        results = new ArrayList<SearchResult>();
        
        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            results.add( result );
        }
        
        assertEquals( 5, results.size() );
        
        // check that we have correctly read the 5 first entries
        for ( int i = 5; i < 10; i++ )
        {
            SearchResult entry = results.get( i-5 );
            assertEquals( "user" + i, entry.getAttributes().get( "cn" ).get() );
        }
        
    }
}