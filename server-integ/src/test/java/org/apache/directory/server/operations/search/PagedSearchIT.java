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

 
import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.SizeLimitExceededException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsResponseControl;

import org.apache.directory.server.core.integ.annotations.ApplyLdifs;
import org.apache.directory.server.integ.SiRunner;

import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContext;

import org.apache.directory.server.ldap.LdapService;
import org.apache.directory.shared.ldap.message.control.PagedSearchControl;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


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

    /**
     * Create the searchControls with a paged size
     */
    private SearchControls createSearchControls( DirContext ctx, int sizeLimit, int pagedSize ) 
        throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setCountLimit( sizeLimit );
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        PagedSearchControl pagedSearchControl = new PagedSearchControl();
        pagedSearchControl.setSize( 5 );
        
        ((LdapContext)ctx).setRequestControls( new Control[] {pagedSearchControl} );
        
        return controls;
    }
    
    
    /**
     * Create the searchControls with a paged size
     */
    private void createNextSearchControls( DirContext ctx, byte[] cookie, int pagedSize ) 
        throws NamingException
    {
        PagedSearchControl pagedSearchControl = new PagedSearchControl();
        pagedSearchControl.setCookie( cookie );
        pagedSearchControl.setSize( 5 );
        ((LdapContext)ctx).setRequestControls( new Control[] {pagedSearchControl} );
    }
    
    
    /**
     * Do a pagedSearch with a paged size of 5, and no limit otherwise
     * @throws Exception
     */
    @Test
    public void testSearchPagedSearch5Entries() throws Exception
    {
        DirContext ctx = getWiredContext( ldapService );
        SearchControls controls = createSearchControls( ctx, LdapService.NO_SIZE_LIMIT, 5 );
        
        // Search the 5 first elements
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
        assertEquals( 0, responseControl.getResultSize() );
        
        // Prepare the next iteration
        createNextSearchControls( ctx, responseControl.getCookie(), 5 );
        
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
    
    
    /**
     * Do a pagedSearch with a paged size of 5, and no limit otherwise
     * @throws Exception
     */
    @Test
    public void testSearchPagedSearch5EntriesSizeLimit9() throws Exception
    {
        DirContext ctx = getWiredContext( ldapService );
        SearchControls controls = createSearchControls( ctx, 9, 5 );
        
        // Search the 5 first elements
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
        assertEquals( 0, responseControl.getResultSize() );

        // Prepare the next iteration
        createNextSearchControls( ctx, responseControl.getCookie(), 5 );
        
        list = ctx.search( "dc=users,ou=system", "(cn=*)", controls );
        
        results = new ArrayList<SearchResult>();
        
        boolean hasSizeLimitException = false;
        
        try
        {
            while ( list.hasMore() )
            {
                SearchResult result = list.next();
                results.add( result );
            }
        }
        catch ( SizeLimitExceededException slee )
        {
            hasSizeLimitException = true;
        }
        
        // We must have had a sizeLimoit exception
        assertTrue( hasSizeLimitException );
        
        assertEquals( 4, results.size() );
        
        // check that we have correctly read the 5 first entries
        for ( int i = 5; i < 9; i++ )
        {
            SearchResult entry = results.get( i-5 );
            assertEquals( "user" + i, entry.getAttributes().get( "cn" ).get() );
        }
        
    }
}