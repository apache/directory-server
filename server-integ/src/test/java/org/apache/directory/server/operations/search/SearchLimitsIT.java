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


import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.SizeLimitExceededException;
import javax.naming.TimeLimitExceededException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.api.filtering.EntryFilter;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.core.api.interceptor.NextInterceptor;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchingOperationContext;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * A set of tests to make sure the negation operator is working 
 * properly when included in search filters on indexed attributes.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith ( FrameworkRunner.class ) 
@CreateLdapServer ( 
    transports = 
    {
        @CreateTransport( protocol = "LDAP" )
    })
@ApplyLdifs( {
    "dn: ou=actors,ou=system",
    "objectClass: top",
    "objectClass: organizationalUnit",
    "ou: actors",

    "dn: uid=jblack,ou=actors,ou=system",
    "objectClass: top",
    "objectClass: person",
    "objectClass: organizationalPerson",
    "objectClass: uidObject",
    "uid: jblack",
    "ou: comedy",
    "ou: adventure",
    "cn: Jack Black",
    "userPassword: secret",
    "sn: Black",

    "dn: uid=bpitt,ou=actors,ou=system",
    "objectClass: top",
    "objectClass: person",
    "objectClass: organizationalPerson",
    "objectClass: uidObject",
    "uid: bpitt",
    "ou: drama",
    "ou: adventure",
    "userPassword: secret",
    "cn: Brad Pitt",
    "sn: Pitt",

    "dn: uid=gcloony,ou=actors,ou=system",
    "objectClass: top",
    "objectClass: person",
    "objectClass: organizationalPerson",
    "objectClass: uidObject",
    "uid: gcloony",
    "ou: drama",
    "userPassword: secret",
    "cn: Goerge Cloony",
    "sn: Cloony",

    "dn: uid=jnewbie,ou=actors,ou=system",
    "objectClass: top",
    "objectClass: person",
    "objectClass: organizationalPerson",
    "objectClass: uidObject",
    "uid: jnewbie",
    "userPassword: secret",
    "cn: Joe Newbie",
    "sn: Newbie" 
    }
)
public class SearchLimitsIT extends AbstractLdapTestUnit 
{

    /**
     * An {@link Interceptor} that fakes a specified amount of delay to each 
     * search iteration so we can make sure search time limits are adhered to.
     *
     * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
         */
    class DelayInducingInterceptor extends BaseInterceptor
    {
        private Long delayMillis;

        
        public EntryFilteringCursor search( NextInterceptor next, SearchOperationContext searchContext ) throws LdapException
        {
            EntryFilteringCursor cursor = next.search( searchContext );
            cursor.addEntryFilter( new EntryFilter() {
                public boolean accept( SearchingOperationContext operation, Entry result ) throws Exception
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
    private long oldMaxSizeLimit;
    private DelayInducingInterceptor delayInterceptor;

    
    @Before
    public void setUp() throws Exception
    {
        oldMaxTimeLimit = getLdapServer().getMaxTimeLimit();
        oldMaxSizeLimit = getLdapServer().getMaxSizeLimit();
        delayInterceptor = new DelayInducingInterceptor();
        getLdapServer().getDirectoryService().getInterceptorChain().addFirst( delayInterceptor );
    }
    
    
    @After
    public void tearDown() throws Exception
    {
        getLdapServer().setMaxTimeLimit( oldMaxTimeLimit );
        getLdapServer().setMaxSizeLimit( oldMaxSizeLimit );
        getLdapServer().getDirectoryService().getInterceptorChain().remove( DelayInducingInterceptor.class.getSimpleName() );
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
        getLdapServer().setMaxTimeLimit( LdapServer.NO_TIME_LIMIT );
        delayInterceptor.setDelayMillis( 500 );
        
        getActorsWithLimit( "(objectClass=*)", 499, LdapServer.NO_SIZE_LIMIT );
    }
    

    /**
     * Sets up the server with longer search time limit than the request's 
     * which constrains time by request time limit value to cause a time limit 
     * exceeded exception on the client.
     */
    @Test ( expected = TimeLimitExceededException.class )
    public void testRequestConstrainedLessThanConfiguration() throws Exception
    {
        getLdapServer().setMaxTimeLimit( 10000 ); // this is in seconds
        delayInterceptor.setDelayMillis( 500 );
        
        getActorsWithLimit( "(objectClass=*)", 499, LdapServer.NO_SIZE_LIMIT );
    }

    
    /**
     * Sets up the server with shorter search time limit than the request's 
     * which constrains time by using server max limit value to cause a time 
     * limit exceeded exception on the client.
     */
    @Test ( expected = TimeLimitExceededException.class )
    public void testRequestConstrainedGreaterThanConfiguration() throws Exception
    {
        getLdapServer().setMaxTimeLimit( 1 ); // this is in seconds
        delayInterceptor.setDelayMillis( 1100 );
        
        getActorsWithLimit( "(objectClass=*)", 100000, LdapServer.NO_SIZE_LIMIT );
    }

    
    /**
     * Sets up the server with limited search time with unlimited request
     * time limit.  Should work just fine for the administrative user.
     */
    @Test 
    public void testRequestUnlimitedConfigurationLimited() throws Exception
    {
        getLdapServer().setMaxTimeLimit( 1 ); // this is in seconds
        delayInterceptor.setDelayMillis( 500 );
        
        getActorsWithLimit( "(objectClass=*)", 
            LdapServer.NO_TIME_LIMIT, LdapServer.NO_SIZE_LIMIT );
    }

    
    /**
     * Sets up the server with limited search time with unlimited request
     * time limit.  Should not work for non administrative users.
     */
    @Test ( expected = TimeLimitExceededException.class ) 
    public void testNonAdminRequestUnlimitedConfigurationLimited() throws Exception
    {
        getLdapServer().setMaxTimeLimit( 1 ); // this is in seconds
        delayInterceptor.setDelayMillis( 500 );
        
        getActorsWithLimitNonAdmin( "(objectClass=*)", 
            LdapServer.NO_TIME_LIMIT, LdapServer.NO_SIZE_LIMIT );
    }
    
    
    // -----------------------------------------------------------------------
    // Size Limit Tests
    // -----------------------------------------------------------------------
    
    
    /**
     * Sets up the server with unlimited search size limit but constrains size
     * by request size limit value to cause a size limit exceeded exception on
     * the client.
     */
    @Test (expected = SizeLimitExceededException.class)
    public void testRequestConstrainedUnlimitByConfigurationSize() throws Exception
    {
        getLdapServer().setMaxSizeLimit( LdapServer.NO_SIZE_LIMIT );
        
        getActorsWithLimit( "(objectClass=*)", LdapServer.NO_TIME_LIMIT, 1 );
    }
    

    /**
     * Sets up the server with longer search size limit than the request's 
     * which constrains size by request size limit value to cause a size limit 
     * exceeded exception on the client.
     */
    @Test ( expected = SizeLimitExceededException.class )
    public void testRequestConstrainedLessThanConfigurationSize() throws Exception
    {
        getLdapServer().setMaxSizeLimit( 10000 ); 

        getActorsWithLimit( "(objectClass=*)", LdapServer.NO_TIME_LIMIT, 1 );
    }


    /**
     * Sets up the server with shorter search size limit than the request's 
     * which constrains size by using server max limit value. Should work 
     * just fine for the administrative user.
     */
    @Test
    public void testRequestConstrainedGreaterThanConfigurationSize() throws Exception
    {
        getLdapServer().setMaxSizeLimit( 1 ); 
        Set<String> set = getActorsWithLimit( "(objectClass=*)", LdapServer.NO_TIME_LIMIT, 100000 );
        assertEquals( 4, set.size() );
    }


    /**
     * Sets up the server with shorter search size limit than the request's 
     * which constrains size by using server max limit value to cause a size 
     * limit exceeded exception on the client.
     */
    @Test (expected = SizeLimitExceededException.class ) 
    public void testNonAdminRequestConstrainedGreaterThanConfigurationSize() throws Exception
    {
        getLdapServer().setMaxSizeLimit( 1 ); 
        
        // We are not using the admin : it should fail
        getActorsWithLimitNonAdmin( "(objectClass=*)", LdapServer.NO_TIME_LIMIT, 100000 );
    }

    
    /**
     * Sets up the server with limited search size with unlimited request
     * size limit.  Should work just fine for the administrative user.
     */
    @Test 
    public void testRequestUnlimitedConfigurationLimitedSize() throws Exception
    {
        getLdapServer().setMaxSizeLimit( 1 ); 
        Set<String> set = getActorsWithLimit( "(objectClass=*)", 
            LdapServer.NO_TIME_LIMIT, LdapServer.NO_SIZE_LIMIT );
        
        assertEquals( 4, set.size() );
    }

    
    /**
     * Sets up the server with limited search size with unlimited request
     * size limit.  Should not work for non administrative users.
     */
    @Test ( expected = SizeLimitExceededException.class ) 
    public void testNonAdminRequestUnlimitedConfigurationLimitedSize() throws Exception
    {
        getLdapServer().setMaxSizeLimit( 1 );
        getActorsWithLimitNonAdmin( "(objectClass=*)", 
            LdapServer.NO_TIME_LIMIT, LdapServer.NO_SIZE_LIMIT );
    }


    /**
     * Test for DIRSERVER-1235.
     * Sets up the server with unlimited search size limit but constrains size
     * by request size limit value. The request size limit is less than the
     * expected number of result entries, so exception expected.
     * 
     * cf RFC 4511 :
     *  "sizeLimitExceeded (4)
     *   Indicates that the size limit specified by the client was
     *   exceeded before the operation could be completed."
     */
    @Test ( expected = SizeLimitExceededException.class )
    public void testRequestConstraintedLessThanExpectedSize() throws Exception
    {
        getLdapServer().setMaxSizeLimit( LdapServer.NO_SIZE_LIMIT );
        
        getActorsWithLimit( "(objectClass=*)", LdapServer.NO_TIME_LIMIT, 3 );
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
        getLdapServer().setMaxSizeLimit( LdapServer.NO_SIZE_LIMIT );
        Set<String> set = getActorsWithLimit( "(objectClass=*)", LdapServer.NO_TIME_LIMIT, 4 );
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
        getLdapServer().setMaxSizeLimit( LdapServer.NO_SIZE_LIMIT );
        Set<String> set = getActorsWithLimit( "(objectClass=*)", LdapServer.NO_TIME_LIMIT, 5 );
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
        getLdapServer().setMaxSizeLimit( LdapServer.NO_SIZE_LIMIT );

        DirContext ctx = getWiredContext( getLdapServer() );
        String filter = "(objectClass=*)";
        SearchControls controls = new SearchControls();
        controls.setTimeLimit( 0 );
        controls.setCountLimit( 1 );
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );

        NamingEnumeration<SearchResult> namingEnumeration = 
            ctx.search( "ou=actors,ou=system", filter, controls );
        assertTrue( namingEnumeration.hasMore() );
        namingEnumeration.next();
        assertFalse( namingEnumeration.hasMore() );
    }


    // -----------------------------------------------------------------------
    // Utility Methods
    // -----------------------------------------------------------------------
    
    
    /**
     * Do a search request from the ou=actors,ou=system base, with a principal
     * which is the administrator.
     */
    private Set<String> getActorsWithLimit( String filter, int timeLimitMillis, long sizeLimit ) throws Exception
    {
        DirContext ctx = getWiredContext( getLdapServer() );
        Set<String> results = new HashSet<String>();
        SearchControls controls = new SearchControls();
        controls.setTimeLimit( timeLimitMillis );
        controls.setCountLimit( sizeLimit );
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        
        NamingEnumeration<SearchResult> namingEnumeration = 
            ctx.search( "ou=actors,ou=system", filter, controls );
        
        while( namingEnumeration.hasMore() )
        {
            results.add( namingEnumeration.next().getNameInNamespace() );
        }
        
        return results;
    }

    /**
     * Do a search request from the ou=actors,ou=system base, with a principal
     * which is not the administrator.
     */
    private Set<String> getActorsWithLimitNonAdmin( String filter, int timeLimitMillis, long sizeLimit ) 
        throws Exception
    {
        DirContext ctx = getWiredContext( getLdapServer(), "uid=jblack,ou=actors,ou=system", "secret" );
        Set<String> results = new HashSet<String>();
        SearchControls controls = new SearchControls();
        controls.setTimeLimit( timeLimitMillis );
        controls.setCountLimit( sizeLimit );
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        
        NamingEnumeration<SearchResult> namingEnumeration = 
            ctx.search( "ou=actors,ou=system", filter, controls );
        
        while( namingEnumeration.hasMore() )
        {
            results.add( namingEnumeration.next().getNameInNamespace() );
        }
        
        return results;
    }
}
