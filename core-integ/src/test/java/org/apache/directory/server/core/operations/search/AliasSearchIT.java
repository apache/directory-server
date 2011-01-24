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


import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.model.exception.LdapNoSuchObjectException;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.filter.FilterParser;
import org.apache.directory.shared.ldap.model.filter.SearchScope;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.name.Dn;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tests to make sure the server is operating correctly when handling aliases.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "AliasSearchDS", partitions =
    { @CreatePartition(name = "example", suffix = "dc=example,dc=com"),
        @CreatePartition(name = "acme", suffix = "o=acme") })
@ApplyLdifFiles(
    { "AliasSearchIT.ldif" })
public class AliasSearchIT extends AbstractLdapTestUnit
{

    Logger LOG = LoggerFactory.getLogger( AliasSearchIT.class );


    private void dump() throws Exception
    {
        List<String> results1 = search( "dc=example,dc=com", SearchScope.SUBTREE, "(objectClass=*)",
            AliasDerefMode.NEVER_DEREF_ALIASES );
        for ( String dn : results1 )
        {
            System.out.println( dn );
        }
        //List<String> results2 = search( "o=acme", SearchScope.SUBTREE, "(objectClass=*)",
        //    AliasDerefMode.NEVER_DEREF_ALIASES );
        //for ( String dn : results2 )
        //{
        //    System.out.println( dn );
        //}
    }


    @Test(expected = LdapNoSuchObjectException.class)
    public void testNonexistingPartition() throws Exception
    {
        search( "dc=x", SearchScope.SUBTREE, "(objectClass=*)", AliasDerefMode.NEVER_DEREF_ALIASES );
    }


    /**
     * Search from context entry.
     */
    @Test
    public void testContextEntry() throws Exception
    {
        // use filter
        verifyCounts( "dc=example,dc=com", SearchScope.OBJECT, "(uid=user1)", 0, 0, 0, 0 );
        verifyCounts( "dc=example,dc=com", SearchScope.ONELEVEL, "(uid=user1)", 0, 0, 0, 0 );
        verifyCounts( "dc=example,dc=com", SearchScope.SUBTREE, "(uid=user1)", 3, 3, 1, 1 );

        // check some important case: no deref -> aliases and non-alias must be contained in result
        verifyResults( "dc=example,dc=com", SearchScope.SUBTREE, "(uid=user1)", AliasDerefMode.NEVER_DEREF_ALIASES,
            "uid=user1,ou=managers,dc=example,dc=com", "uid=user1,ou=engineering,ou=users,dc=example,dc=com",
            "uid=user1,ou=sales,ou=users,dc=example,dc=com" );

        // check some important case: deref -> aliases must not be contained in result
        verifyResults( "dc=example,dc=com", SearchScope.SUBTREE, "(uid=user1)", AliasDerefMode.DEREF_ALWAYS,
            "uid=user1,ou=sales,ou=users,dc=example,dc=com" );

        // use true filter
        verifyCounts( "dc=example,dc=com", SearchScope.OBJECT, "(objectClass=*)", 1, 1, 1, 1 );
        verifyCounts( "dc=example,dc=com", SearchScope.ONELEVEL, "(objectClass=*)", 4, 4, 3, 3 );
        // 23 entries below dc=example,dc=com, 9 alias entries
        verifyCounts( "dc=example,dc=com", SearchScope.SUBTREE, "(objectClass=*)", 23, 23, 23 - 9, 23 - 9 );

        // check an important case: no deref -> ou=people must be contained in result 
        verifyResults( "dc=example,dc=com", SearchScope.ONELEVEL, "(objectClass=*)",
            AliasDerefMode.NEVER_DEREF_ALIASES, "ou=users,dc=example,dc=com", "ou=people,dc=example,dc=com",
            "ou=managers,dc=example,dc=com", "ou=newsfeeds,dc=example,dc=com" );

        // check an important case: deref -> ou=people is alias to ou=users must not be contained in result 
        verifyResults( "dc=example,dc=com", SearchScope.ONELEVEL, "(objectClass=*)", AliasDerefMode.DEREF_ALWAYS,
            "ou=users,dc=example,dc=com", "ou=managers,dc=example,dc=com", "ou=newsfeeds,dc=example,dc=com" );
    }


    /**
     * Search from ou=managers. 
     * There is one child entry that is an alias:
     * uid=user1,ou=managers,dc=example,dc=com -> 
     * uid=user1,ou=sales,ou=users,dc=example,dc=com
     *
     * @throws Exception
     */
    @Test
    public void testOuManagers() throws Exception
    {
        verifyCounts( "ou=managers,dc=example,dc=com", SearchScope.OBJECT, "(uid=user1)", 0, 0, 0, 0 );
        verifyCounts( "ou=managers,dc=example,dc=com", SearchScope.ONELEVEL, "(uid=user1)", 1, 1, 1, 1 );
        verifyCounts( "ou=managers,dc=example,dc=com", SearchScope.SUBTREE, "(uid=user1)", 1, 1, 1, 1 );

        // get the alias (not dereferenced)
        verifyResults( "ou=managers,dc=example,dc=com", SearchScope.ONELEVEL, "(uid=user1)",
            AliasDerefMode.NEVER_DEREF_ALIASES, "uid=user1,ou=managers,dc=example,dc=com" );

        // get the alias (not dereferenced)
        verifyResults( "ou=managers,dc=example,dc=com", SearchScope.ONELEVEL, "(uid=user1)",
            AliasDerefMode.DEREF_FINDING_BASE_OBJ, "uid=user1,ou=managers,dc=example,dc=com" );

        // get the target (dereferenced)
        verifyResults( "ou=managers,dc=example,dc=com", SearchScope.ONELEVEL, "(uid=user1)",
            AliasDerefMode.DEREF_IN_SEARCHING, "uid=user1,ou=sales,ou=users,dc=example,dc=com" );

        // get the target (dereferenced)
        verifyResults( "ou=managers,dc=example,dc=com", SearchScope.ONELEVEL, "(uid=user1)",
            AliasDerefMode.DEREF_ALWAYS, "uid=user1,ou=sales,ou=users,dc=example,dc=com" );
    }


    /**
     * Search from ou=users.
     *
     * @throws Exception
     */
    @Test
    public void testOuUsers() throws Exception
    {
        // use filter
        verifyCounts( "ou=users,dc=example,dc=com", SearchScope.OBJECT, "(uid=user1)", 0, 0, 0, 0 );
        verifyCounts( "ou=users,dc=example,dc=com", SearchScope.ONELEVEL, "(uid=user1)", 0, 0, 0, 0 );
        verifyCounts( "ou=users,dc=example,dc=com", SearchScope.SUBTREE, "(uid=user1)", 2, 2, 1, 1 );

        // finding: uid=user1,ou=engineering is not dereferenced
        verifyResults( "ou=users,dc=example,dc=com", SearchScope.SUBTREE, "(uid=user1)",
            AliasDerefMode.DEREF_FINDING_BASE_OBJ, "uid=user1,ou=sales,ou=users,dc=example,dc=com",
            "uid=user1,ou=engineering,ou=users,dc=example,dc=com" );

        // searching: uid=user1,ou=engineering is dereferenced to uid=user1,ou=sales, no duplicates.
        verifyResults( "ou=users,dc=example,dc=com", SearchScope.SUBTREE, "(uid=user1)",
            AliasDerefMode.DEREF_IN_SEARCHING, "uid=user1,ou=sales,ou=users,dc=example,dc=com" );
    }


    /**
     * Test dereferencing to branch sibling. Search from ou=people, that is an alias to ou=users.
     *
     * @throws Exception
     */
    @Test
    public void testOuPeople() throws Exception
    {
        verifyCounts( "ou=people,dc=example,dc=com", SearchScope.OBJECT, "(uid=user1)", 0, 0, 0, 0 );
        verifyCounts( "ou=people,dc=example,dc=com", SearchScope.ONELEVEL, "(uid=user1)", 0, 0, 0, 0 );
        verifyCounts( "ou=people,dc=example,dc=com", SearchScope.SUBTREE, "(uid=user1)", 0, 2, 0, 1 );

        // finding: ou=people is dereferenced to ou=users, but uid=user1,ou=engineering is not dereferenced
        verifyResults( "ou=people,dc=example,dc=com", SearchScope.SUBTREE, "(uid=user1)",
            AliasDerefMode.DEREF_FINDING_BASE_OBJ, "uid=user1,ou=sales,ou=users,dc=example,dc=com",
            "uid=user1,ou=engineering,ou=users,dc=example,dc=com" );

        // always: ou=people is dereferenced to ou=users and uid=user1,ou=engineering is 
        // dereferenced to uid=user1,ou=sales.
        verifyResults( "ou=people,dc=example,dc=com", SearchScope.SUBTREE, "(uid=user1)", AliasDerefMode.DEREF_ALWAYS,
            "uid=user1,ou=sales,ou=users,dc=example,dc=com" );

        verifyCounts( "ou=people,dc=example,dc=com", SearchScope.OBJECT, "(uid=*)", 0, 0, 0, 0 );
        verifyCounts( "ou=people,dc=example,dc=com", SearchScope.ONELEVEL, "(uid=*)", 0, 0, 0, 0 );
        verifyCounts( "ou=people,dc=example,dc=com", SearchScope.SUBTREE, "(uid=*)", 0, 5, 0, 3 );
    }


    /**
     * Search from ou=sales,ou=users. Ensure that no duplicate entries are returned due to 
     * alias of cn=deputy.
     *
     * @throws Exception
     */
    @Test
    public void testOuSales() throws Exception
    {
        verifyCounts( "ou=sales,ou=users,dc=example,dc=com", SearchScope.OBJECT, "(uid=user1)", 0, 0, 0, 0 );
        verifyCounts( "ou=sales,ou=users,dc=example,dc=com", SearchScope.ONELEVEL, "(uid=user1)", 1, 1, 1, 1 );
        verifyCounts( "ou=sales,ou=users,dc=example,dc=com", SearchScope.SUBTREE, "(uid=user1)", 1, 1, 1, 1 );

        verifyCounts( "ou=sales,ou=users,dc=example,dc=com", SearchScope.OBJECT, "(uid=*)", 0, 0, 0, 0 );
        verifyCounts( "ou=sales,ou=users,dc=example,dc=com", SearchScope.ONELEVEL, "(uid=*)", 2, 2, 2, 2 );
        verifyCounts( "ou=sales,ou=users,dc=example,dc=com", SearchScope.SUBTREE, "(uid=*)", 2, 2, 2, 2 );

        verifyResults( "ou=sales,ou=users,dc=example,dc=com", SearchScope.SUBTREE, "(uid=*)",
            AliasDerefMode.DEREF_IN_SEARCHING, "uid=user1,ou=sales,ou=users,dc=example,dc=com",
            "uid=user2,ou=sales,ou=users,dc=example,dc=com" );
    }


    /**
     * Test proper dereferencing of descendant of aliases.
     *
     * @throws Exception
     */
    @Ignore("fixme")
    @Test
    public void testDerefDescendantOfAlias() throws Exception
    {
        verifyResults( "ou=sales,ou=people,dc=example,dc=com", SearchScope.OBJECT, "(objectClass=*)",
            AliasDerefMode.DEREF_FINDING_BASE_OBJ, "ou=sales,ou=users,dc=example,dc=com" );

        verifyResults( "cn=deputy,uid=user1,ou=managers,dc=example,dc=com", SearchScope.OBJECT,
            "(objectClass=inetOrgPerson)", AliasDerefMode.DEREF_FINDING_BASE_OBJ,
            "uid=user2,ou=sales,ou=users,dc=example,dc=com" );

        // test to avoid loops
        try
        {
            verifyResults( "cn=nonexisting,uid=user1,ou=managers,dc=example,dc=com", SearchScope.OBJECT,
                "(objectClass=inetOrgPerson)", AliasDerefMode.DEREF_FINDING_BASE_OBJ );
            fail();
        }
        catch ( LdapNoSuchObjectException e )
        {
            // expected
        }
    }


    /**
     * Test dereferencing to branch sibling. Search ou=engineering which is an alias to ou=sales.
     *
     * @throws Exception
     */
    @Test
    public void testOuEngineering() throws Exception
    {
        verifyCounts( "ou=engineering,ou=users,dc=example,dc=com", SearchScope.OBJECT, "(uid=user1)", 0, 0, 0, 0 );
        verifyCounts( "ou=engineering,ou=users,dc=example,dc=com", SearchScope.ONELEVEL, "(uid=user1)", 1, 1, 1, 1 );
        verifyCounts( "ou=engineering,ou=users,dc=example,dc=com", SearchScope.SUBTREE, "(uid=user1)", 1, 1, 1, 1 );

        // not dereferenced -> ou=engineering
        verifyResults( "ou=engineering,ou=users,dc=example,dc=com", SearchScope.ONELEVEL, "(uid=user1)",
            AliasDerefMode.NEVER_DEREF_ALIASES, "uid=user1,ou=engineering,ou=users,dc=example,dc=com" );
        verifyResults( "ou=engineering,ou=users,dc=example,dc=com", SearchScope.SUBTREE, "(uid=user1)",
            AliasDerefMode.NEVER_DEREF_ALIASES, "uid=user1,ou=engineering,ou=users,dc=example,dc=com" );
        // not dereferenced -> ou=engineering
        verifyResults( "ou=engineering,ou=users,dc=example,dc=com", SearchScope.ONELEVEL, "(uid=user1)",
            AliasDerefMode.DEREF_FINDING_BASE_OBJ, "uid=user1,ou=engineering,ou=users,dc=example,dc=com" );
        verifyResults( "ou=engineering,ou=users,dc=example,dc=com", SearchScope.SUBTREE, "(uid=user1)",
            AliasDerefMode.DEREF_FINDING_BASE_OBJ, "uid=user1,ou=engineering,ou=users,dc=example,dc=com" );
        // dereferenced -> ou=sales
        verifyResults( "ou=engineering,ou=users,dc=example,dc=com", SearchScope.ONELEVEL, "(uid=user1)",
            AliasDerefMode.DEREF_IN_SEARCHING, "uid=user1,ou=sales,ou=users,dc=example,dc=com" );
        verifyResults( "ou=engineering,ou=users,dc=example,dc=com", SearchScope.SUBTREE, "(uid=user1)",
            AliasDerefMode.DEREF_IN_SEARCHING, "uid=user1,ou=sales,ou=users,dc=example,dc=com" );
        // dereferenced -> ou=sales
        verifyResults( "ou=engineering,ou=users,dc=example,dc=com", SearchScope.ONELEVEL, "(uid=user1)",
            AliasDerefMode.DEREF_ALWAYS, "uid=user1,ou=sales,ou=users,dc=example,dc=com" );
        verifyResults( "ou=engineering,ou=users,dc=example,dc=com", SearchScope.SUBTREE, "(uid=user1)",
            AliasDerefMode.DEREF_ALWAYS, "uid=user1,ou=sales,ou=users,dc=example,dc=com" );
    }


    /**
     * Test dereferencing to another partition.
     *
     * @throws Exception
     */
    @Ignore("not implemented")
    @Test
    public void testOuHr() throws Exception
    {
        verifyCounts( "ou=hr,ou=users,dc=example,dc=com", SearchScope.OBJECT, "(uid=*)", 0, 0, 0, 0 );
        verifyCounts( "ou=hr,ou=users,dc=example,dc=com", SearchScope.ONELEVEL, "(uid=*)", 0, 2, 0, 2 );
        verifyCounts( "ou=hr,ou=users,dc=example,dc=com", SearchScope.SUBTREE, "(uid=*)", 0, 2, 0, 2 );

        verifyResults( "ou=hr,ou=users,dc=example,dc=com", SearchScope.SUBTREE, "(uid=*)", AliasDerefMode.DEREF_ALWAYS,
            "uid=userA,ou=human resources,ou=users,o=acme", "uid=userB,ou=human resources,ou=users,o=acme" );
    }


    /**
     * Test dereferencing to leaf sibling.
     *
     * @throws Exception
     */
    @Ignore("not implemented")
    @Test
    public void testUidFoobar() throws Exception
    {
        verifyCounts( "uid=foobar,ou=engineering,ou=users,dc=example,dc=com", SearchScope.OBJECT,
            "(objectClass=person)", 0, 1, 0, 1 );
        verifyCounts( "uid=foobar,ou=engineering,ou=users,dc=example,dc=com", SearchScope.ONELEVEL,
            "(objectClass=person)", 0, 0, 0, 1 );
        verifyCounts( "uid=foobar,ou=engineering,ou=users,dc=example,dc=com", SearchScope.SUBTREE,
            "(objectClass=person)", 0, 1, 0, 1 );

        // cn=deputy,uid=user3 -> uid=user3
        verifyResults( "uid=foobar,ou=engineering,ou=users,dc=example,dc=com", SearchScope.ONELEVEL,
            "(objectClass=person)", AliasDerefMode.DEREF_ALWAYS, "uid=user3,ou=engineering,ou=users,dc=example,dc=com" );
    }


    /**
     * Test dereferencing of chained alias
     * uid=user1,ou=managers,dc=example,dc=com -> 
     * uid=user1,ou=engineering,ou=users,dc=example,dc=com ->
     * uid=user1,ou=sales,ou=users,dc=example,dc=com
     *
     * @throws Exception
     */
    @Ignore("not implemented")
    @Test
    public void testUidUser1OuManager() throws Exception
    {
        verifyCounts( "uid=user1,ou=managers,dc=example,dc=com", SearchScope.OBJECT, "(uid=user1)", 1, 1, 0, 1 );
        verifyCounts( "uid=user1,ou=managers,dc=example,dc=com", SearchScope.ONELEVEL, "(uid=user1)", 0, 0, 0, 0 );
        verifyCounts( "uid=user1,ou=managers,dc=example,dc=com", SearchScope.SUBTREE, "(uid=user1)", 1, 1, 0, 1 );

        verifyResults( "uid=user1,ou=managers,dc=example,dc=com", SearchScope.OBJECT, "(uid=user1)",
            AliasDerefMode.NEVER_DEREF_ALIASES, "uid=user1,ou=managers,dc=example,dc=com" );
        verifyResults( "uid=user1,ou=managers,dc=example,dc=com", SearchScope.OBJECT, "(uid=user1)",
            AliasDerefMode.DEREF_FINDING_BASE_OBJ, "uid=user1,ou=sales,ou=users,dc=example,dc=com" );
        verifyResults( "uid=user1,ou=managers,dc=example,dc=com", SearchScope.OBJECT, "(uid=user1)",
            AliasDerefMode.DEREF_IN_SEARCHING );
        verifyResults( "uid=user1,ou=managers,dc=example,dc=com", SearchScope.OBJECT, "(uid=user1)",
            AliasDerefMode.DEREF_ALWAYS, "uid=user1,ou=sales,ou=users,dc=example,dc=com" );

        verifyResults( "uid=user1,ou=managers,dc=example,dc=com", SearchScope.SUBTREE, "(uid=user1)",
            AliasDerefMode.NEVER_DEREF_ALIASES, "uid=user1,ou=managers,dc=example,dc=com" );
        verifyResults( "uid=user1,ou=managers,dc=example,dc=com", SearchScope.SUBTREE, "(uid=user1)",
            AliasDerefMode.DEREF_FINDING_BASE_OBJ, "uid=user1,ou=sales,ou=users,dc=example,dc=com" );
        verifyResults( "uid=user1,ou=managers,dc=example,dc=com", SearchScope.SUBTREE, "(uid=user1)",
            AliasDerefMode.DEREF_IN_SEARCHING );
        verifyResults( "uid=user1,ou=managers,dc=example,dc=com", SearchScope.SUBTREE, "(uid=user1)",
            AliasDerefMode.DEREF_ALWAYS, "uid=user1,ou=sales,ou=users,dc=example,dc=com" );
    }


    @Test
    public void testCnDeputy() throws Exception
    {
        verifyCounts( "cn=deputy,uid=user2,ou=sales,ou=users,dc=example,dc=com", SearchScope.OBJECT, "(objectClass=*)",
            1, 1, 1, 1 );
        verifyResults( "cn=deputy,uid=user2,ou=sales,ou=users,dc=example,dc=com", SearchScope.OBJECT,
            "(objectClass=*)", AliasDerefMode.NEVER_DEREF_ALIASES,
            "cn=deputy,uid=user2,ou=sales,ou=users,dc=example,dc=com" );
        verifyResults( "cn=deputy,uid=user2,ou=sales,ou=users,dc=example,dc=com", SearchScope.OBJECT,
            "(objectClass=*)", AliasDerefMode.DEREF_FINDING_BASE_OBJ, "uid=user1,ou=sales,ou=users,dc=example,dc=com" );
        verifyResults( "cn=deputy,uid=user2,ou=sales,ou=users,dc=example,dc=com", SearchScope.OBJECT,
            "(objectClass=*)", AliasDerefMode.DEREF_IN_SEARCHING,
            "cn=deputy,uid=user2,ou=sales,ou=users,dc=example,dc=com" );
        verifyResults( "cn=deputy,uid=user2,ou=sales,ou=users,dc=example,dc=com", SearchScope.OBJECT,
            "(objectClass=*)", AliasDerefMode.DEREF_ALWAYS, "uid=user1,ou=sales,ou=users,dc=example,dc=com" );

        // should not cause infinite loops
        verifyCounts( "cn=deputy,uid=user2,ou=sales,ou=users,dc=example,dc=com", SearchScope.ONELEVEL,
            "(objectClass=*)", 0, 2, 0, 1 );
        verifyCounts( "cn=deputy,uid=user2,ou=sales,ou=users,dc=example,dc=com", SearchScope.SUBTREE,
            "(objectClass=*)", 1, 4, 0, 4 );
    }


    /**
     * Search uid=user3,ou=engineering,ou=users,dc=example,dc=com.
     *
     * @throws Exception
     */
    @Ignore("fixme")
    @Test
    public void testOuUser3_Loop() throws Exception
    {
        verifyCounts( "uid=user3,ou=engineering,ou=users,dc=example,dc=com", SearchScope.OBJECT, "(uid=user3)", 1, 1,
            1, 1 );
        verifyCounts( "uid=user3,ou=engineering,ou=users,dc=example,dc=com", SearchScope.ONELEVEL, "(uid=user3)", 0, 0,
            1, 1 );
        verifyCounts( "uid=user3,ou=engineering,ou=users,dc=example,dc=com", SearchScope.SUBTREE, "(uid=user3)", 1, 1,
            1, 1 );

        verifyResults( "uid=user3,ou=engineering,ou=users,dc=example,dc=com", SearchScope.ONELEVEL, "(uid=user3)",
            AliasDerefMode.DEREF_IN_SEARCHING, "uid=user3,ou=engineering,ou=users,dc=example,dc=com" );
    }


    @Ignore("fixme")
    @Test
    public void testCnDeputyOuUser3_Loop() throws Exception
    {
        verifyCounts( "cn=deputy,uid=user3,ou=engineering,ou=users,dc=example,dc=com", SearchScope.OBJECT,
            "(uid=user3)", 0, 1, 0, 1 );
        verifyCounts( "cn=deputy,uid=user3,ou=engineering,ou=users,dc=example,dc=com", SearchScope.ONELEVEL,
            "(uid=user3)", 0, 0, 0, 1 );
        verifyCounts( "cn=deputy,uid=user3,ou=engineering,ou=users,dc=example,dc=com", SearchScope.SUBTREE,
            "(uid=user3)", 0, 1, 0, 1 );
    }


    @Ignore
    @Test
    public void testCursorNextPrevWithReset() throws Exception
    {
        try
        {
            Dn base = new Dn( "dc=example,dc=com" );
            SearchScope scope = SearchScope.SUBTREE;
            ExprNode exprNode = FilterParser.parse( service.getSchemaManager(), "(objectClass=*)" );
            AliasDerefMode aliasDerefMode = AliasDerefMode.DEREF_ALWAYS;
            EntryFilteringCursor cursor = service.getAdminSession()
                .search( base, scope, exprNode, aliasDerefMode, null );

            // advancing the cursor forward and backward must give the same result
            for ( int count = 1; count < 20; count++ )
            {
                cursor.beforeFirst();

                List<String> nextResults = new ArrayList<String>();
                while ( nextResults.size() < count && cursor.next() )
                {
                    nextResults.add( cursor.get().getDn().getName() );
                }

                cursor.next();

                List<String> prevResults = new ArrayList<String>();
                while ( cursor.previous() )
                {
                    prevResults.add( 0, cursor.get().getDn().getName() );
                }

                assertEquals( nextResults.size(), prevResults.size() );
                assertEquals( nextResults, prevResults );
            }
        }
        catch ( UnsupportedOperationException e )
        {
            LOG.warn( "Partition doesn't support next/previous test" );
        }
    }


    @Ignore
    @Test
    public void testCursorPrevNext() throws Exception
    {
        try
        {
            Dn base = new Dn( "dc=example,dc=com" );
            SearchScope scope = SearchScope.SUBTREE;
            ExprNode exprNode = FilterParser.parse( service.getSchemaManager(), "(objectClass=*)" );
            AliasDerefMode aliasDerefMode = AliasDerefMode.DEREF_ALWAYS;
            EntryFilteringCursor cursor = service.getAdminSession()
                .search( base, scope, exprNode, aliasDerefMode, null );

            // advancing the cursor backward and forward must give the same result
            for ( int count = 1; count < 20; count++ )
            {
                cursor.afterLast();

                List<String> prevResults = new ArrayList<String>();
                while ( prevResults.size() < count && cursor.previous() )
                {
                    prevResults.add( cursor.get().getDn().getName() );
                }

                cursor.previous();

                List<String> nextResults = new ArrayList<String>();
                while ( cursor.next() )
                {
                    nextResults.add( 0, cursor.get().getDn().getName() );
                }

                assertEquals( nextResults.size(), prevResults.size() );
                assertEquals( nextResults, prevResults );
            }
        }
        catch ( UnsupportedOperationException e )
        {
            LOG.warn( "Partition doesn't support previous/next test" );
        }
    }


    private void verifyResults( String base, SearchScope scope, String filter, AliasDerefMode aliasDerefMode,
        String... expectedResults ) throws Exception
    {
        List<String> result = search( base, scope, filter, aliasDerefMode );
        assertEquals( expectedResults.length, result.size() );
        for ( String expected : expectedResults )
        {
            assertTrue( result.contains( expected ) );
        }
    }


    /**
     * Performs a search for each alias dereferencing mode and checks the search result count. 
     *
     * @param base the search base to use
     * @param scope the search scope to use
     * @param filter the search filter to use
     * @param neverCount the expected result count for AliasDerefMode.NEVER_DEREF_ALIASES
     * @param findCount the expected result count for AliasDerefMode.DEREF_FINDING_BASE_OBJ
     * @param searchCount the expected result count for AliasDerefMode.DEREF_IN_SEARCHING
     * @param alwaysCount the expected result count for AliasDerefMode.DEREF_ALWAYS
     * @throws Exception
     */
    private void verifyCounts( String base, SearchScope scope, String filter, int neverCount, int findCount,
        int searchCount, int alwaysCount ) throws Exception
    {
        List<String> results1 = search( base, scope, filter, AliasDerefMode.NEVER_DEREF_ALIASES );
        assertEquals( neverCount, results1.size() );

        List<String> results2 = search( base, scope, filter, AliasDerefMode.DEREF_FINDING_BASE_OBJ );
        assertEquals( findCount, results2.size() );

        List<String> results3 = search( base, scope, filter, AliasDerefMode.DEREF_IN_SEARCHING );
        assertEquals( searchCount, results3.size() );

        List<String> results4 = search( base, scope, filter, AliasDerefMode.DEREF_ALWAYS );
        assertEquals( alwaysCount, results4.size() );
    }


    /**
     * Performs a search and returns the search results DNs.
     *
     * @param base the search base to use
     * @param scope the search scope to use
     * @param filter the search filter to use
     * @param aliasDerefMode the alias dereferencing mode to use
     * @return the list of search results DNs
     * @throws Exception
     */
    private List<String> search( String base, SearchScope scope, String filter, AliasDerefMode aliasDerefMode )
        throws Exception
    {
        List<String> nextResults = new ArrayList<String>();

        ExprNode exprNode = FilterParser.parse(service.getSchemaManager(), filter);
        EntryFilteringCursor cursor = service.getAdminSession().search( new Dn( base ), scope, exprNode,
            aliasDerefMode, null );
        cursor.beforeFirst();
        while ( cursor.next() )
        {
            nextResults.add( cursor.get().getDn().getName() );
        }

        try
        {
            List<String> prevResults = new ArrayList<String>();
            cursor.afterLast();
            while ( cursor.previous() )
            {
                prevResults.add( 0, cursor.get().getDn().getName() );
            }

            assertEquals( nextResults.size(), prevResults.size() );
            assertEquals( nextResults, prevResults );
        }
        catch ( UnsupportedOperationException e )
        {
            LOG.warn( "Partition doesn't support previous test" );
        }

        return nextResults;
    }

}