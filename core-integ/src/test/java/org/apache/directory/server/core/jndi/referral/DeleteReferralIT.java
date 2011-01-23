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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.ReferralException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.entry.DefaultEntry;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapNoSuchObjectException;
import org.apache.directory.shared.ldap.model.exception.LdapReferralException;
import org.apache.directory.shared.ldap.name.Dn;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests the referral handling functionality for the Delete operation 
 * within the server's core.
 * 
 * All the tests are described on this page :
 * http://cwiki.apache.org/confluence/display/DIRxSRVx11/Referral+Handling+Changes
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith ( FrameworkRunner.class )
@CreateDS(name = "DeleteReferralIT")
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
public class DeleteReferralIT extends AbstractLdapTestUnit
{

    /** The Context we are using to inject entries with JNDI */
    LdapContext MNNCtx;
    
    /** The entries we are using to do the tests */
    Attributes userEntry;
    Entry serverEntry;
    
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
    }

    
    /**
     * Test deletion of a non existing entry (not a referral), with no referral 
     * in its ancestor, using JNDI.
     */
    @Test
    public void testDeleteNonExistingEntry() throws Exception
    {
        try
        {
            MNNCtx.destroySubcontext( "cn=Emmanuel Lecharny" );
            fail();
        }
        catch ( NameNotFoundException nnfe )
        {
            assertTrue( true );
        }
    }


    /**
     * Test deletion of an entry with an ancestor referral, using JNDI.
     */
    @Test
    public void testDeleteEntryWithAncestorJNDI() throws Exception
    {
        try
        {
            MNNCtx.destroySubcontext( "cn=Emmanuel Lecharny,ou=Roles" );
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
     * Test deletion of an entry with an ancestor referral, using the core api.
     */
    @Test
    public void testDeleteEntryWithAncestorCoreAPI() throws Exception
    {
        CoreSession session = service.getAdminSession();
        
        try
        {
            session.delete( new Dn( "cn=Emmanuel Lecharny,ou=Roles" ) );
            fail();
        }
        catch ( LdapNoSuchObjectException lnsoe )
        {
            assertTrue( true );
        }
    }


    /**
     * Test deletion of an existing entry with no ancestor referral, using the core api.
     */
    @Test
    public void testDeleteExistingEntryNotReferral() throws Exception
    {
        CoreSession session = service.getAdminSession();
        Dn dn = new Dn( "cn=Alex Karasulu,o=MNN,c=WW,ou=system" );
        
        session.delete( dn );
        
        try
        {
            session.lookup( dn, new String[]{} );
            fail();
        }
        catch ( LdapNoSuchObjectException lnsoe )
        {
            assertTrue( true );
        }
    }


    /**
     * Test deletion of an entry which is a referral an ancestor referral, 
     * using JNDI, with 'throw'.
     */
    @Test
    public void testDeleteExistingEntryReferralJNDIThrow() throws Exception
    {
        try
        {
            // Set to 'throw'
            MNNCtx.addToEnvironment( Context.REFERRAL, "throw" );

            MNNCtx.destroySubcontext( "ou=Roles" );
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
     * Test deletion of an entry which is a referral an ancestor referral, 
     * using JNDI, with 'ignore'.
     */
    @Test
    public void testDeleteExistingEntryReferralJNDIIgnore() throws Exception
    {
        CoreSession session = service.getAdminSession();

        // Set to 'throw'
        MNNCtx.addToEnvironment( Context.REFERRAL, "ignore" );

        MNNCtx.destroySubcontext( "ou=Roles" );

        Dn dn = new Dn( "ou=Roles,o=MNN,c=WW,ou=system" );
        
        // We should not find the entry
        try
        {
            session.lookup( dn, new String[]{} );
            fail();
        }
        catch ( LdapNoSuchObjectException lnsoe )
        {
            assertTrue( true );
        }
    }


    /**
     * Test deletion of an entry which is a referral an ancestor referral, 
     * using the CoreAPI, without the ManageDsaIT control.
     */
    @Test
    public void testDeleteExistingEntryReferralCoreAPINoManageDSAIt() throws Exception
    {
        CoreSession session = service.getAdminSession();
        Dn dn = new Dn( "ou=Roles,o=MNN,c=WW,ou=system" );

        try
        {
            session.delete( dn, false );
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
     * Test deletion of an entry which is a referral an ancestor referral, 
     * using the CoreAPI, with the ManageDsaIT control.
     */
    @Test
    public void testDeleteExistingEntryReferralCoreAPIManageDSAIT() throws Exception
    {
        CoreSession session = service.getAdminSession();
        Dn dn = new Dn( "ou=Roles,o=MNN,c=WW,ou=system" );

        session.delete( dn, true );

        // We should not find the entry
        try
        {
            session.lookup( dn, new String[]{} );
            fail();
        }
        catch ( LdapNoSuchObjectException lnsoe )
        {
            assertTrue( true );
        }
    }
}
