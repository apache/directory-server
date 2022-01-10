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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * A set of tests to make sure the negation operator is working
 * properly when included in search filters. Created in response
 * to JIRA issue
 * <a href="https://issues.apache.org/jira/browse/DIRSERVER-951">DIRSERVER-951</a>.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( { ApacheDSTestExtension.class } )
@CreateLdapServer(
    transports =
        {
            @CreateTransport(protocol = "LDAP")
    })
@ApplyLdifs(
    {
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
        "sn: Black",

        "dn: uid=bpitt,ou=actors,ou=system",
        "objectClass: top",
        "objectClass: person",
        "objectClass: organizationalPerson",
        "objectClass: uidObject",
        "uid: bpitt",
        "ou: drama",
        "ou: adventure",
        "cn: Brad Pitt",
        "sn: Pitt",

        "dn: uid=gcloony,ou=actors,ou=system",
        "objectClass: top",
        "objectClass: person",
        "objectClass: organizationalPerson",
        "objectClass: uidObject",
        "uid: gcloony",
        "ou: drama",
        "cn: Goerge Cloony",
        "sn: Cloony",

        "dn: uid=jnewbie,ou=actors,ou=system",
        "objectClass: top",
        "objectClass: person",
        "objectClass: organizationalPerson",
        "objectClass: uidObject",
        "uid: jnewbie",
        "cn: Joe Newbie",
        "sn: Newbie" })
public class NegationSearchIT extends AbstractLdapTestUnit
{
    /**
     * Tests to make sure a negated search for actors without ou
     * with value 'drama' returns those that do not have the attribute
     * and do not have a 'drama' value for ou if the attribute still
     * exists.  This test does not build an index on ou for the system
     * partition.
     */
    @Test
    public void testSearchNotDrama() throws Exception
    {
        // jack black has ou but not drama, and joe newbie has no ou what so ever
        Set<SearchResult> results = getResults( "(!(ou=drama))" );
        assertTrue( contains( "uid=jblack,ou=actors,ou=system", results ) );
        assertTrue( contains( "uid=jnewbie,ou=actors,ou=system", results ) );
        assertEquals( 2, results.size() );
    }


    /**
     * Tests to make sure a negated search for actors without ou
     * with value 'drama' returns those that do not have the attribute
     * and do not have a 'drama' value for ou if the attribute still
     * exists.  This test does not build an index on ou for the system
     * partition.
     */
    @Test
    public void testSearchNotDramaNotNewbie() throws Exception
    {
        // jack black has ou but not drama, and joe newbie has no ou what so ever
        Set<SearchResult> results = getResults( "(& (!(uid=jnewbie)) (!(ou=drama)) )" );
        assertTrue( contains( "uid=jblack,ou=actors,ou=system", results ) );
        assertFalse( contains( "uid=jnewbie,ou=actors,ou=system", results ) );
        assertEquals( 1, results.size() );
    }


    boolean contains( String dn, Set<SearchResult> results )
    {
        for ( SearchResult result : results )
        {
            if ( result.getNameInNamespace().equals( dn ) )
            {
                return true;
            }
        }

        return false;
    }


    Set<SearchResult> getResults( String filter ) throws Exception
    {
        DirContext ctx = getWiredContext( getLdapServer() );
        Set<SearchResult> results = new HashSet<SearchResult>();
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        NamingEnumeration<SearchResult> namingEnumeration = ctx.search( "ou=actors,ou=system", filter, controls );
        while ( namingEnumeration.hasMore() )
        {
            results.add( namingEnumeration.next() );
        }

        namingEnumeration.close();
        ctx.close();

        return results;
    }
}
