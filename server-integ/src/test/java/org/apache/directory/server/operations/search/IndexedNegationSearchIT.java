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
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.junit.tools.MultiThreadedMultiInvoker;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.annotations.SaslMechanism;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.ldap.handlers.bind.cramMD5.CramMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.digestMD5.DigestMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.gssapi.GssapiMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.plain.PlainMechanismHandler;
import org.apache.directory.server.ldap.handlers.extended.StartTlsHandler;
import org.apache.directory.server.ldap.handlers.extended.StoredProcedureExtendedOperationHandler;
import org.apache.directory.shared.ldap.model.constants.SupportedSaslMechanisms;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * A set of tests to make sure the negation operator is working 
 * properly when included in search filters on indexed attributes.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith ( FrameworkRunner.class ) 
@ApplyLdifs( {
    "dn: ou=test,ou=system", 
    "objectClass: top", 
    "objectClass: organizationalUnit", 
    "ou: test", 

    "dn: uid=test1,ou=test,ou=system", 
    "objectClass: top", 
    "objectClass: account", 
    "uid: test1", 
    "ou: test1", 

    "dn: uid=test2,ou=test,ou=system", 
    "objectClass: top", 
    "objectClass: account", 
    "uid: test2", 
    "ou: test2", 

    "dn: uid=testNoOU,ou=test,ou=system", 
    "objectClass: top", 
    "objectClass: account", 
    "uid: testNoOU", 
    
    "dn: ou=actors,ou=system", 
    "objectClass: top", 
    "objectClass: organizationalUnit", 
    "ou: actors\n", 

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
    "sn: Newbie" 

    }
)
@CreateLdapServer ( 
    transports = 
    {
        @CreateTransport( protocol = "LDAP" )
    },
    saslMechanisms = 
    {
        @SaslMechanism( name=SupportedSaslMechanisms.PLAIN, implClass=PlainMechanismHandler.class ),
        @SaslMechanism( name=SupportedSaslMechanisms.CRAM_MD5, implClass=CramMd5MechanismHandler.class),
        @SaslMechanism( name= SupportedSaslMechanisms.DIGEST_MD5, implClass=DigestMd5MechanismHandler.class),
        @SaslMechanism( name=SupportedSaslMechanisms.GSSAPI, implClass=GssapiMechanismHandler.class),
        @SaslMechanism( name=SupportedSaslMechanisms.NTLM, implClass=NtlmMechanismHandler.class),
        @SaslMechanism( name=SupportedSaslMechanisms.GSS_SPNEGO, implClass=NtlmMechanismHandler.class)
    },
    extendedOpHandlers = 
    {
        StartTlsHandler.class,
        StoredProcedureExtendedOperationHandler.class
    })
public class IndexedNegationSearchIT extends AbstractLdapTestUnit
{
    @Rule
    public MultiThreadedMultiInvoker i = new MultiThreadedMultiInvoker( MultiThreadedMultiInvoker.THREADSAFE );

    /**
     * Tests to make sure a negated search for OU of "test1" returns
     * those entries that do not have the OU attribute or do not have
     * a "test1" value for OU if the attribute exists.
     */
    @Test
    public void testSearchNotOUIndexed() throws Exception
    {
        Set<SearchResult> results = getResults( "(!(ou=test1))" );
        assertFalse( contains( "uid=test1,ou=test,ou=system", results ) );
        assertTrue( contains( "uid=test2,ou=test,ou=system", results ) );
        assertTrue( contains( "uid=testNoOU,ou=test,ou=system", results ) );
    }

    
    /**
     * Tests to make sure a negated search for actors without ou
     * with value 'drama' returns those that do not have the attribute
     * and do not have a 'drama' value for ou if the attribute still
     * exists.  This test DOES build an index on ou for the system
     * partition and should have failed if the bug in DIRSERVER-951
     * was present and reproducable.
     */
    @Test
    public void testSearchNotDramaIndexed() throws Exception
    {
        // jack black has ou but not drama, and joe newbie has no ou what so ever
        Set<SearchResult> results = getActorResults( "(!(ou=drama))" );
        assertTrue( contains( "uid=jblack,ou=actors,ou=system", results ) );
        assertTrue( contains( "uid=jnewbie,ou=actors,ou=system", results ) );
        assertEquals( 2, results.size() );
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
    
    
    /**
     * Tests to make sure a negated search for actors without ou
     * with value 'drama' returns those that do not have the attribute
     * and do not have a 'drama' value for ou if the attribute still
     * exists.  This test DOES build an index on ou for the system
     * partition and should have failed if the bug in DIRSERVER-951
     * was present and reproducable.
     */
    @Test
    public void testSearchNotDramaNotNewbieIndexed() throws Exception
    {
        // jack black has ou but not drama, and joe newbie has no ou what so ever
        Set<SearchResult> results = getActorResults( "(& (!(uid=jnewbie)) (!(ou=drama)) )" );
        assertTrue( contains( "uid=jblack,ou=actors,ou=system", results ) );
        assertFalse( contains( "uid=jnewbie,ou=actors,ou=system", results ) );
        assertEquals( 1, results.size() );
    }

    
    Set<SearchResult> getActorResults( String filter ) throws Exception
    {
        DirContext ctx = getWiredContext( getLdapServer() );
        Set<SearchResult> results = new HashSet<SearchResult>();
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        NamingEnumeration<SearchResult> namingEnumeration = ctx.search( "ou=actors,ou=system", filter, controls );
        while( namingEnumeration.hasMore() )
        {
            results.add( namingEnumeration.next() );
        }
        
        namingEnumeration.close();
        ctx.close();
        
        return results;
    }

    
    Set<SearchResult> getResults( String filter ) throws Exception
    {
        DirContext ctx = getWiredContext( getLdapServer() );
        Set<SearchResult> results = new HashSet<SearchResult>();
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        NamingEnumeration<SearchResult> namingEnumeration = ctx.search( "ou=system", filter, controls );
        while( namingEnumeration.hasMore() )
        {
            results.add( namingEnumeration.next() );
        }
        
        namingEnumeration.close();
        ctx.close();
        
        return results;
    }
}
