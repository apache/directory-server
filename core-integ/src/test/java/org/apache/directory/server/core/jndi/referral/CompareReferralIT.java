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

import javax.naming.Context;
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
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.entry.DefaultEntry;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.exception.LdapPartialResultException;
import org.apache.directory.shared.ldap.exception.LdapReferralException;
import org.apache.directory.shared.ldap.name.Dn;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests the referral handling functionality for the Compare operation 
 * within the server's core.
 * 
 * All the tests are described on this page :
 * http://cwiki.apache.org/confluence/display/DIRxSRVx11/Referral+Handling+Changes
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith ( FrameworkRunner.class )
@CreateDS(name = "CompareReferralIT")
@ApplyLdifs( {
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
    "sn: akarasulu"
    }
)
public class CompareReferralIT extends AbstractLdapTestUnit
{

    /** The Context we are using to inject entries with JNDI */
    LdapContext MNNCtx;
    
    /** The entries we are using to do the tests */
    Attributes userEntry;
    Entry serverEntry;
    
    /** The search controls used globally */
    SearchControls ctls;
    
    @Before
    public void setUp() throws Exception
    {
        MNNCtx = getContext( ServerDNConstants.ADMIN_SYSTEM_DN, service, "o=MNN,c=WW,ou=system" );

        // JNDI entry
        userEntry = new BasicAttributes( "objectClass", "top", true );
        userEntry.get( "objectClass" ).add( "person" );
        userEntry.put( "sn", "elecharny" );
        userEntry.put( "cn", "Emmanuel Lecharny" );
        
        // Core API entry
        Dn dn = new Dn( "cn=Emmanuel Lecharny, ou=apache, ou=people, o=MNN, c=WW, ou=system" );
        serverEntry = new DefaultEntry( service.getSchemaManager(), dn );

        serverEntry.put( "ObjectClass", "top", "person" );
        serverEntry.put( "sn", "elecharny" );
        serverEntry.put( "cn", "Emmanuel Lecharny" );
        
        // Set up the search controls
        ctls = new SearchControls();
        ctls.setReturningAttributes( new String[0] );       // Return no attrs
        ctls.setSearchScope( SearchControls.OBJECT_SCOPE ); // Search object only
    }

    
    /**
     * Test a compare on a non existing entry (not a referral), with no referral 
     * in its ancestor, using JNDI.
     */
    @Test
    public void testCompareNonExistingEntry() throws Exception
    {
        try
        {
            // This is a compare operation, not a search, thanks to JNDI !
            MNNCtx.search( "cn=Emmanuel Lecharny", "(cn=Emmanuel Lecharny)", ctls );
            fail();
        }
        catch ( NameNotFoundException nnfe )
        {
            assertTrue( true );
        }
    }


    /**
     * Test a Compare on an entry with an ancestor referral, using JNDI,
     * with 'throw'
     */
    @Test
    public void testCompareEntryWithAncestorJNDIThrow() throws Exception
    {
        try
        {
            // Set to 'throw'
            MNNCtx.addToEnvironment( Context.REFERRAL, "throw" );

            // This is a compare operation, not a search, thanks to JNDI !
            MNNCtx.search( "cn=Emmanuel Lecharny,ou=Roles", "(cn=Emmanuel Lecharny)", ctls );
            fail();
        }
        catch ( ReferralException re )
        {
            int nbRefs = 0;
            Set<String> expectedRefs = new HashSet<String>();
            expectedRefs.add( "ldap://hostd/cn=Emmanuel%20Lecharny,ou=Roles,dc=apache,dc=org" );
            
            do 
            {
                String ref = (String)re.getReferralInfo();
                
                assertTrue( expectedRefs.contains( ref ) );
                nbRefs ++;
            }
            while ( re.skipReferral() );
            
            assertEquals( 1, nbRefs );
        }
    }


    /**
     * Test a Compare on an entry with an ancestor referral, using JNDI,
     * with 'ignore'
     */
    @Test
    public void testCompareEntryWithAncestorJNDIIgnore() throws Exception
    {
        try
        {
            // Set to 'throw'
            MNNCtx.addToEnvironment( Context.REFERRAL, "ignore" );

            // This is a compare operation, not a search, thanks to JNDI !
            MNNCtx.search( "cn=Emmanuel Lecharny,ou=Roles", "(cn=Emmanuel Lecharny)", ctls );
            fail();
        }
        catch ( PartialResultException pre )
        {
            assertTrue( true );
        }
    }


    /**
     * Test a compare on an entry with an ancestor referral, using the core API,
     * without a ManageDsaIT.
     */
    @Test
    public void testCompareEntryWithAncestorCoreAPIWithoutManageDsaIt() throws Exception
    {
        CoreSession session = service.getAdminSession();
        
        try
        {
            session.compare( new Dn( "cn=Emmanuel Lecharny,ou=Roles,o=MNN,c=WW,ou=system" ), "cn", "Emmanuel Lecharny", false );
            fail();
        }
        catch ( LdapReferralException re )
        {
            int nbRefs = 0;
            Set<String> expectedRefs = new HashSet<String>();
            expectedRefs.add( "ldap://hostd/cn=Emmanuel%20Lecharny,ou=Roles,dc=apache,dc=org" );
            
            do 
            {
                String ref = (String)re.getReferralInfo();
                
                assertTrue( expectedRefs.contains( ref ) );
                nbRefs ++;
            }
            while ( re.skipReferral() );
            
            assertEquals( 1, nbRefs );
        }
    }


    /**
     * Test a compare on an entry with an ancestor referral, using the core API,
     * with a ManageDsaIT.
     */
    @Test
    public void testCompareEntryWithAncestorCoreAPIWithManageDsaIt() throws Exception
    {
        CoreSession session = service.getAdminSession();
        
        try
        {
            session.compare( new Dn( "cn=Emmanuel Lecharny,ou=Roles,o=MNN,c=WW,ou=system" ), "cn", "Emmanuel Lecharny", true );
            fail();
        }
        catch ( LdapPartialResultException lpre )
        {
            assertTrue( true );
        }
    }

    
    /**
     * Test a compare on an existing entry (not a referral), with no referral 
     * in its ancestor, using JNDI.
     */
    @Test
    public void testCompareExistingEntryNoReferral() throws Exception
    {
        // This is a compare operation, not a search, thanks to JNDI !
        NamingEnumeration<SearchResult> result = MNNCtx.search( "cn=Alex Karasulu", "(sn=akarasulu)", ctls );
        
        assertNotNull( result );

        int nbResult = 0;
        
        while ( result.hasMore() )
        {
            SearchResult sr = ( SearchResult ) result.next();
            
            // The name should be empty
            assertEquals( "", sr.getName() );
            
            // It should not have any attribute
            assertEquals( 0, sr.getAttributes().size() );
            nbResult++;
        }
        
        // We should have only one result
        assertEquals( 1, nbResult );
    }

    
    /**
     * Test a compare on an existing referral entry, using JNDI "throw".
     */
    @Test
    public void testCompareExistingEntryReferralJNDIThrow() throws Exception
    {
        // Set to 'throw'
        MNNCtx.addToEnvironment( DirContext.REFERRAL, "throw" );

        try
        {
            // This is a compare operation, not a search, thanks to JNDI !
            MNNCtx.search( "ou=roles", "(ou=roles)", ctls );
            fail();
        }
        catch ( ReferralException re )
        {
            int nbRefs = 0;
            Set<String> expectedRefs = new HashSet<String>();
            expectedRefs.add( "ldap://hostd/ou=Roles,dc=apache,dc=org" );
            
            do 
            {
                String ref = (String)re.getReferralInfo();
                
                assertTrue( expectedRefs.contains( ref ) );
                nbRefs ++;
            }
            while ( re.skipReferral() );
            
            assertEquals( 1, nbRefs );
        }
    }

    
    /**
     * Test a compare on an existing referral entry, using JNDI "ignore".
     */
    @Test
    public void testCompareExistingEntryReferralJNDIIgnore() throws Exception
    {
        // Set to 'throw'
        MNNCtx.addToEnvironment( DirContext.REFERRAL, "ignore" );

        // This is a compare operation, not a search, thanks to JNDI !
        NamingEnumeration<SearchResult> result = MNNCtx.search( "ou=roles", "(ou=roles)", ctls );

        assertNotNull( result );

        int nbResult = 0;
        
        while ( result.hasMore() )
        {
            SearchResult sr = ( SearchResult ) result.next();
            
            // The name should be empty
            assertEquals( "", sr.getName() );
            
            // It should not have any attribute
            assertEquals( 0, sr.getAttributes().size() );
            nbResult++;
        }
        
        // We should have only one result
        assertEquals( 1, nbResult );
    }

    
    /**
     * Test a compare on an existing referral entry, using The core API
     * and no ManageDsaIt flag.
     */
    @Test
    public void testCompareExistingEntryReferralCoreAPIWithoutManageDsaIt() throws Exception
    {
        CoreSession session = service.getAdminSession();
        
        try
        {
            session.compare( new Dn( "ou=Roles,o=MNN,c=WW,ou=system" ), "ou", "roles", false );
            fail();
        }
        catch ( LdapReferralException re )
        {
            int nbRefs = 0;
            Set<String> expectedRefs = new HashSet<String>();
            expectedRefs.add( "ldap://hostd/ou=Roles,dc=apache,dc=org" );
            
            do 
            {
                String ref = (String)re.getReferralInfo();
                
                assertTrue( expectedRefs.contains( ref ) );
                nbRefs ++;
            }
            while ( re.skipReferral() );
            
            assertEquals( 1, nbRefs );
        }
    }

    
    /**
     * Test a compare on an existing referral entry, using The core API
     * with the ManageDsaIt flag.
     */
    @Test
    public void testCompareExistingEntryReferralCoreAPIWithManageDsaIt() throws Exception
    {
        CoreSession session = service.getAdminSession();
        
        assertTrue( session.compare( new Dn( "ou=Roles,o=MNN,c=WW,ou=system" ), "ou", "roles", true ) );
    }
}
