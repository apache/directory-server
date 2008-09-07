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


import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.filtering.EntryFilter;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchingOperationContext;
import org.apache.directory.server.integ.SiRunner;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContext;
import static org.junit.Assert.*;

import org.apache.directory.server.ldap.LdapService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.NamingEnumeration;
import javax.naming.SizeLimitExceededException;
import javax.naming.TimeLimitExceededException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import java.util.HashSet;
import java.util.Set;


/**
 * A set of tests to make sure the negation operator is working 
 * properly when included in search filters on indexed attributes.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
@RunWith ( SiRunner.class ) 
@CleanupLevel ( Level.SUITE )
@ApplyLdifs( {
    "dn: ou=actors,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: organizationalUnit\n" +
    "ou: actors\n\n" +

    "dn: uid=jblack,ou=actors,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: person\n" +
    "objectClass: organizationalPerson\n" +
    "objectClass: uidObject\n" +
    "uid: jblack\n" +
    "ou: comedy\n" +
    "ou: adventure\n" +
    "cn: Jack Black\n" +
    "userPassword: secret\n" +
    "sn: Black\n\n" +

    "dn: uid=bpitt,ou=actors,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: person\n" +
    "objectClass: organizationalPerson\n" +
    "objectClass: uidObject\n" +
    "uid: bpitt\n" +
    "ou: drama\n" +
    "ou: adventure\n" +
    "userPassword: secret\n" +
    "cn: Brad Pitt\n" +
    "sn: Pitt\n\n" +

    "dn: uid=gcloony,ou=actors,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: person\n" +
    "objectClass: organizationalPerson\n" +
    "objectClass: uidObject\n" +
    "uid: gcloony\n" +
    "ou: drama\n" +
    "userPassword: secret\n" +
    "cn: Goerge Cloony\n" +
    "sn: Cloony\n\n" +

    "dn: uid=jnewbie,ou=actors,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: person\n" +
    "objectClass: organizationalPerson\n" +
    "objectClass: uidObject\n" +
    "uid: jnewbie\n" +
    "userPassword: secret\n" +
    "cn: Joe Newbie\n" +
    "sn: Newbie\n\n" 
    }
)
public class SearchLimitsIT 
{
    public static LdapService ldapService;

    
    /**
     * An {@link Interceptor} that fakes a specified amount of delay to each 
     * search iteration so we can make sure search time limits are adhered to.
     *
     * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
     * @version $Rev$, $Date$
     */
    class DelayInducingInterceptor extends BaseInterceptor
    {
        private Long delayMillis;

        
        public EntryFilteringCursor search( NextInterceptor next, SearchOperationContext opContext ) throws Exception
        {
            EntryFilteringCursor cursor = next.search( opContext );
            cursor.addEntryFilter( new EntryFilter() {
                public boolean accept( SearchingOperationContext operation, ClonedServerEntry result ) throws Exception
                {
                    if ( delayMillis != null )
                    {
                        Thread.sleep( delayMillis );
                    }

                    return true;
                }
            });
            return cursor;
        }
        
        
        public void setDelayMillis( long delayMillis )
        {
            if ( delayMillis <= 0 )
            {
                this.delayMillis = null;
            }
            
            this.delayMillis = delayMillis;
        }
    }

    
    private int oldMaxTimeLimit;
    private int oldMaxSizeLimit;
    private DelayInducingInterceptor delayInterceptor;

    
    @Before
    public void setUp() throws Exception
    {
        oldMaxTimeLimit = ldapService.getMaxTimeLimit();
        oldMaxSizeLimit = ldapService.getMaxSizeLimit();
        delayInterceptor = new DelayInducingInterceptor();
        ldapService.getDirectoryService().getInterceptorChain().addFirst( delayInterceptor );
    }
    
    
    @After
    public void tearDown() throws Exception
    {
        ldapService.setMaxTimeLimit( oldMaxTimeLimit );
        ldapService.setMaxSizeLimit( oldMaxSizeLimit );
        ldapService.getDirectoryService().getInterceptorChain().remove( DelayInducingInterceptor.class.getName() );
    }
    

    // -----------------------------------------------------------------------
    // Time Limit Tests
    // -----------------------------------------------------------------------
    
    
    /**
     * Sets up the server with unlimited search time limit but constrains time
     * by request time limit value to cause a time limit exceeded exception on
     * the client.
     */
    @Test ( expected = TimeLimitExceededException.class )
    public void testRequestConstrainedUnlimitByConfiguration() throws Exception
    {
        ldapService.setMaxTimeLimit( LdapService.NO_TIME_LIMIT );
        delayInterceptor.setDelayMillis( 500 );
        
        getActorsWithLimit( "(objectClass=*)", 499, 0 );
    }
    

    /**
     * Sets up the server with longer search time limit than the request's 
     * which constrains time by request time limit value to cause a time limit 
     * exceeded exception on the client.
     */
    @Test ( expected = TimeLimitExceededException.class )
    public void testRequestConstrainedLessThanConfiguration() throws Exception
    {
        ldapService.setMaxTimeLimit( 10000 ); // this is in seconds
        delayInterceptor.setDelayMillis( 500 );
        
        getActorsWithLimit( "(objectClass=*)", 499, 0 );
    }

    
    /**
     * Sets up the server with shorter search time limit than the request's 
     * which constrains time by using server max limit value to cause a time 
     * limit exceeded exception on the client.
     */
    @Test ( expected = TimeLimitExceededException.class )
    public void testRequestConstrainedGreaterThanConfiguration() throws Exception
    {
        ldapService.setMaxTimeLimit( 1 ); // this is in seconds
        delayInterceptor.setDelayMillis( 1100 );
        
        getActorsWithLimit( "(objectClass=*)", 100000, 0 );
    }

    
    /**
     * Sets up the server with limited search time with unlimited request
     * time limit.  Should work just fine for the administrative user.
     */
    @Test 
    public void testRequestUnlimitedConfigurationLimited() throws Exception
    {
        ldapService.setMaxTimeLimit( 1 ); // this is in seconds
        delayInterceptor.setDelayMillis( 500 );
        
        getActorsWithLimit( "(objectClass=*)", 0, 0 );
    }

    
    /**
     * Sets up the server with limited search time with unlimited request
     * time limit.  Should not work for non administrative users.
     */
    @Test ( expected = TimeLimitExceededException.class ) 
    public void testNonAdminRequestUnlimitedConfigurationLimited() throws Exception
    {
        ldapService.setMaxTimeLimit( 1 ); // this is in seconds
        delayInterceptor.setDelayMillis( 500 );
        
        getActorsWithLimitNonAdmin( "(objectClass=*)", 0, 0 );
    }
    
    
    // -----------------------------------------------------------------------
    // Size Limit Tests
    // -----------------------------------------------------------------------
    
    
    /**
     * Sets up the server with unlimited search size limit but constrains size
     * by request size limit value to cause a size limit exceeded exception on
     * the client.
     */
    @Test ( expected = SizeLimitExceededException.class )
    public void testRequestConstrainedUnlimitByConfigurationSize() throws Exception
    {
        ldapService.setMaxSizeLimit( LdapService.NO_SIZE_LIMIT );
        getActorsWithLimit( "(objectClass=*)", 0,  1 );
    }
    

    /**
     * Sets up the server with longer search size limit than the request's 
     * which constrains size by request size limit value to cause a size limit 
     * exceeded exception on the client.
     */
    @Test ( expected = SizeLimitExceededException.class )
    public void testRequestConstrainedLessThanConfigurationSize() throws Exception
    {
        ldapService.setMaxSizeLimit( 10000 ); 
        getActorsWithLimit( "(objectClass=*)", 0, 1 );
    }


    /**
     * Sets up the server with shorter search size limit than the request's 
     * which constrains size by using server max limit value. Should work 
     * just fine for the administrative user.
     */
    @Test
    public void testRequestConstrainedGreaterThanConfigurationSize() throws Exception
    {
        ldapService.setMaxSizeLimit( 1 ); 
        Set<String> set = getActorsWithLimit( "(objectClass=*)", 0, 100000 );
        assertEquals( 4, set.size() );
    }


    /**
     * Sets up the server with shorter search size limit than the request's 
     * which constrains size by using server max limit value to cause a size 
     * limit exceeded exception on the client.
     */
    @Test ( expected = SizeLimitExceededException.class )
    public void testNonAdminRequestConstrainedGreaterThanConfigurationSize() throws Exception
    {
        ldapService.setMaxSizeLimit( 1 ); 
        getActorsWithLimitNonAdmin( "(objectClass=*)", 0, 100000 );
    }

    
    /**
     * Sets up the server with limited search size with unlimited request
     * size limit.  Should work just fine for the administrative user.
     */
    @Test 
    public void testRequestUnlimitedConfigurationLimitedSize() throws Exception
    {
        ldapService.setMaxSizeLimit( 1 ); 
        Set<String> set = getActorsWithLimit( "(objectClass=*)", 0, 0 );
        assertEquals( 4, set.size() );
    }

    
    /**
     * Sets up the server with limited search size with unlimited request
     * size limit.  Should not work for non administrative users.
     */
    @Test ( expected = SizeLimitExceededException.class ) 
    public void testNonAdminRequestUnlimitedConfigurationLimitedSize() throws Exception
    {
        ldapService.setMaxSizeLimit( 1 ); // this is in seconds
        getActorsWithLimitNonAdmin( "(objectClass=*)", 0, 0 );
    }


    /**
     * Test for DIRSERVER-1235.
     * Sets up the server with unlimited search size limit but constrains size
     * by request size limit value. The request size limit is less than the
     * expected number of result entries, so exception expected.
     */
    @Test(expected = SizeLimitExceededException.class)
    public void testRequestConstraintedLessThanExpectedSize() throws Exception
    {
        ldapService.setMaxSizeLimit( LdapService.NO_SIZE_LIMIT );
        getActorsWithLimit( "(objectClass=*)", 0, 3 );
    }


    /**
     * Test for DIRSERVER-1235.
     * Sets up the server with unlimited search size limit but constrains size
     * by request size limit value. The request size limit is equal to the
     * expected number of result entries so no exception expected.
     */
    @Test
    public void testRequestConstraintedEqualToExpectedSize() throws Exception
    {
        ldapService.setMaxSizeLimit( LdapService.NO_SIZE_LIMIT );
        Set<String> set = getActorsWithLimit( "(objectClass=*)", 0, 4 );
        assertEquals( 4, set.size() );
    }


    /**
     * Test for DIRSERVER-1235.
     * Sets up the server with unlimited search size limit but constrains size
     * by request size limit value. The request size limit is greater than the
     * expected number of result entries so no exception expected.
     */
    @Test
    public void testRequestConstraintedGreaterThanExpectedSize() throws Exception
    {
        ldapService.setMaxSizeLimit( LdapService.NO_SIZE_LIMIT );
        Set<String> set = getActorsWithLimit( "(objectClass=*)", 0, 5 );
        assertEquals( 4, set.size() );
    }


    /**
     * Test for DIRSERVER-1235.
     * Reads an entry using object scope and size limit 1, no exception
     * expected.
     */
    @Test
    public void testRequestObjectScopeAndSizeLimit() throws Exception
    {
        ldapService.setMaxSizeLimit( LdapService.NO_SIZE_LIMIT );

        DirContext ctx = getWiredContext( ldapService );
        String filter = "(objectClass=*)";
        SearchControls controls = new SearchControls();
        controls.setTimeLimit( 0 );
        controls.setCountLimit( 1 );
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );

        NamingEnumeration<SearchResult> namingEnumeration = ctx.search( "ou=actors,ou=system", filter, controls );
        assertTrue( namingEnumeration.hasMore() );
        namingEnumeration.next();
        assertFalse( namingEnumeration.hasMore() );
    }


    // -----------------------------------------------------------------------
    // Utility Methods
    // -----------------------------------------------------------------------
    
    
    private Set<String> getActorsWithLimit( String filter, int timeLimitMillis, int sizeLimit ) throws Exception
    {
        DirContext ctx = getWiredContext( ldapService );
        Set<String> results = new HashSet<String>();
        SearchControls controls = new SearchControls();
        controls.setTimeLimit( timeLimitMillis );
        controls.setCountLimit( sizeLimit );
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        
        NamingEnumeration<SearchResult> namingEnumeration = ctx.search( "ou=actors,ou=system", filter, controls );
        while( namingEnumeration.hasMore() )
        {
            results.add( namingEnumeration.next().getNameInNamespace() );
        }
        
        return results;
    }

    
    private Set<String> getActorsWithLimitNonAdmin( String filter, int timeLimitMillis, int sizeLimit ) 
        throws Exception
    {
        DirContext ctx = getWiredContext( ldapService, "uid=jblack,ou=actors,ou=system", "secret" );
        Set<String> results = new HashSet<String>();
        SearchControls controls = new SearchControls();
        controls.setTimeLimit( timeLimitMillis );
        controls.setCountLimit( sizeLimit );
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        
        NamingEnumeration<SearchResult> namingEnumeration = ctx.search( "ou=actors,ou=system", filter, controls );
        while( namingEnumeration.hasMore() )
        {
            results.add( namingEnumeration.next().getNameInNamespace() );
        }
        
        return results;
    }
}
