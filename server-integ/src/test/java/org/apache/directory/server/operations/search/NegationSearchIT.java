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


import java.util.HashSet;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.core.integ.annotations.Factory;
import org.apache.directory.server.integ.SiRunner;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContext;

import org.apache.directory.server.newldap.LdapServer;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.directory.DirContext;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;


/**
 * A set of tests to make sure the negation operator is working 
 * properly when included in search filters. Created in response
 * to JIRA issue 
 * <a href="https://issues.apache.org/jira/browse/DIRSERVER-951">DIRSERVER-951</a>.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 519077 $
 */
@RunWith ( SiRunner.class ) 
@CleanupLevel ( Level.SUITE )
@Factory ( IndexedNegationSearchIT.Factory.class )
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
    "sn: Black\n\n" +

    "dn: uid=bpitt,ou=actors,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: person\n" +
    "objectClass: organizationalPerson\n" +
    "objectClass: uidObject\n" +
    "uid: bpitt\n" +
    "ou: drama\n" +
    "ou: adventure\n" +
    "cn: Brad Pitt\n" +
    "sn: Pitt\n\n" +

    "dn: uid=gcloony,ou=actors,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: person\n" +
    "objectClass: organizationalPerson\n" +
    "objectClass: uidObject\n" +
    "uid: gcloony\n" +
    "ou: drama\n" +
    "cn: Goerge Cloony\n" +
    "sn: Cloony\n\n" +

    "dn: uid=jnewbie,ou=actors,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: person\n" +
    "objectClass: organizationalPerson\n" +
    "objectClass: uidObject\n" +
    "uid: jnewbie\n" +
    "cn: Joe Newbie\n" +
    "sn: Newbie\n\n" 

    }
)
public class NegationSearchIT 
{
    public static LdapServer ldapServer;

    
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
        DirContext ctx = getWiredContext( ldapServer );
        Set<SearchResult> results = new HashSet<SearchResult>();
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        NamingEnumeration<SearchResult> namingEnumeration = ctx.search( "ou=actors,ou=system", filter, controls );
        while( namingEnumeration.hasMore() )
        {
            results.add( namingEnumeration.next() );
        }
        
        return results;
    }
}
