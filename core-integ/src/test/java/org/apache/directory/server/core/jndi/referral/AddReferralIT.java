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
import javax.naming.NameAlreadyBoundException;
import javax.naming.PartialResultException;
import javax.naming.ReferralException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapPartialResultException;
import org.apache.directory.shared.ldap.model.exception.LdapReferralException;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests the referral handling functionality for the Add operation 
 * within the server's core.
 * 
 * All the tests are described on this page :
 * http://cwiki.apache.org/confluence/display/DIRxSRVx11/Referral+Handling+Changes
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith ( FrameworkRunner.class )
@CreateDS(name = "AddReferralIT")
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
public class AddReferralIT extends AbstractLdapTestUnit
{

    /** The Context we are using to inject entries with JNDI */
    LdapContext MNNCtx;
    
    /** The entries we are using to do the tests */
    Attributes userEntry;
    Entry serverEntry;
    
    @Before
    public void setUp() throws Exception
    {
        MNNCtx = getContext( ServerDNConstants.ADMIN_SYSTEM_DN, getService(), "o=MNN,c=WW,ou=system" );

        // JNDI entry
        userEntry = new BasicAttributes( "objectClass", "top", true );
        userEntry.get( "objectClass" ).add( "person" );
        userEntry.put( "sn", "elecharny" );
        userEntry.put( "cn", "Emmanuel Lecharny" );
        
        // Core API entry
        Dn dn = new Dn( "cn=Emmanuel Lecharny, ou=apache, ou=people, o=MNN, c=WW, ou=system" );
        serverEntry = new DefaultEntry( getService().getSchemaManager(), dn );

        serverEntry.put( "ObjectClass", "top", "person" );
        serverEntry.put( "sn", "elecharny" );
        serverEntry.put( "cn", "Emmanuel Lecharny" );
    }

    
    /**
     * Test addition of a new entry (not a referral), with no referral 
     * in its ancestor, using JNDI.
     */
    @Test
    public void testAddNewEntryNoReferralAncestorJNDI() throws Exception
    {
        DirContext eleCtx = MNNCtx.createSubcontext( "cn=Emmanuel Lecharny", userEntry );
        
        assertNotNull( eleCtx );
        
        Attributes attrs = eleCtx.getAttributes( "" );
        assertNotNull( attrs );
        
        assertEquals( "Emmanuel Lecharny", attrs.get( "cn" ).get() );
        assertEquals( "elecharny", attrs.get( "sn" ).get() );
        Attribute attribute = attrs.get( "objectClass" );
        assertNotNull( attribute );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "person" ) );
    }


    /**
     * Test addition of a new entry (not a referral), with a referral 
     * in its ancestor, using JNDI 'ignore'.
     */
    @Test
    public void testAddNewEntryWithReferralAncestorJNDIIgnore() throws Exception
    {
        // Set to 'ignore'
        MNNCtx.addToEnvironment( Context.REFERRAL, "ignore" );
        
        try
        {
            MNNCtx.createSubcontext( "cn=Emmanuel Lecharny, ou=apache, ou=people", userEntry );
            fail();
        }
        catch ( PartialResultException pre )
        {
            assertTrue( true );
        }
    }


    /**
     * Test addition of a new entry (not a referral), with a referral 
     * in its ancestor, using the Core API with the ManageDsaIt flag set to true.
     *   
     * @throws Exception if something goes wrong.
     */
    @Test
    public void testAddNewEntryWithReferralAncestorCoreAPImanageDsaIT() throws Exception
    {
        CoreSession session = getService().getAdminSession();
        
        try
        {
            session.add( serverEntry, true );
            fail();
        }
        catch ( LdapPartialResultException lpre )
        {
            assertTrue( true );
        }
    }


    /**
     * Test addition of a new entry (not a referral), with a referral 
     * in its ancestor, using JNDI throw.
     */
    @Test
    public void testAddNewEntryWithReferralAncestorJNDIThrow() throws Exception
    {
        // Set to 'throw'
        MNNCtx.addToEnvironment( Context.REFERRAL, "throw" );
        
        try
        {
            MNNCtx.createSubcontext( "cn=Emmanuel Lecharny, ou=apache, ou=people", userEntry );
            fail();
        }
        catch ( ReferralException re )
        {
            assertTrue( true );
            
            int nbRefs = 0;
            Set<String> peopleRefs = new HashSet<String>();
            peopleRefs.add( "ldap://hostb/cn=Emmanuel%20Lecharny,%20ou=apache,OU=People,DC=example,DC=com" );
            peopleRefs.add( "ldap://hostc/cn=Emmanuel%20Lecharny,%20ou=apache,OU=People,O=MNN,C=WW" );
            
            do 
            {
                String ref = (String)re.getReferralInfo();
                
                assertTrue( peopleRefs.contains( ref ) );
                nbRefs ++;
            }
            while ( re.skipReferral() );
            
            assertEquals( 2, nbRefs );
        }
    }


    /**
     * Test addition of a new entry (not a referral), with a referral 
     * in its ancestor, without the ManageDsaIt flag.
     */
    @Test
    public void testAddNewEntryWithReferralAncestorCoreAPINoManageDsaIT() throws Exception
    {
        CoreSession session = getService().getAdminSession();

        try
        {
            session.add( serverEntry, false );
            fail();
        }
        catch ( LdapReferralException re )
        {
            assertTrue( true );
            
            int nbRefs = 0;
            Set<String> peopleRefs = new HashSet<String>();
            peopleRefs.add( "ldap://hostb/cn=Emmanuel%20Lecharny,%20ou=apache,OU=People,DC=example,DC=com" );
            peopleRefs.add( "ldap://hostc/cn=Emmanuel%20Lecharny,%20ou=apache,OU=People,O=MNN,C=WW" );
            
            do 
            {
                String ref = (String)re.getReferralInfo();
                
                assertTrue( peopleRefs.contains( ref ) );
                nbRefs ++;
            }
            while ( re.skipReferral() );
            
            assertEquals( 2, nbRefs );
        }
    }


    /**
     * Test addition of an existing entry (not a referral), with no referral 
     * in its ancestor, using JNDI.
     */
    @Test
    public void testAddExistingEntryNoReferralAncestorJNDI() throws Exception
    {
        Attributes userEntry = new BasicAttributes( "objectClass", "top", true );
        userEntry.get( "objectClass" ).add( "person" );
        userEntry.put( "sn", "elecharny" );
        userEntry.put( "cn", "Emmanuel Lecharny" );

        DirContext eleCtx = MNNCtx.createSubcontext( "cn=Emmanuel Lecharny", userEntry );
        
        assertNotNull( eleCtx );
        
        Attributes attributes = eleCtx.getAttributes( "" );
        assertNotNull( attributes );
        
        assertEquals( "Emmanuel Lecharny", attributes.get( "cn" ).get() );
        assertEquals( "elecharny", attributes.get( "sn" ).get() );
        Attribute attribute = attributes.get( "objectClass" );
        assertNotNull( attribute );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "person" ) );

        try
        {
            MNNCtx.createSubcontext( "cn=Emmanuel Lecharny", userEntry );
            fail();
        }
        catch ( NameAlreadyBoundException nabe )
        {
            assertTrue( true );
        }
    }
}
