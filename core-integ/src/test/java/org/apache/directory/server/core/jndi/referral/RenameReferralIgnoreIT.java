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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.PartialResultException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapEntryAlreadyExistsException;
import org.apache.directory.shared.ldap.model.exception.LdapNoSuchObjectException;
import org.apache.directory.shared.ldap.model.exception.LdapPartialResultException;
import org.apache.directory.shared.ldap.model.name.Rdn;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests the referral handling functionality for the Modify operation 
 * within the server's core.
 * 
 * The Move operation is a ModifyDN where the Rdn is changed, not the superior.
 * 
 * All the tests are described on this page :
 * http://cwiki.apache.org/confluence/display/DIRxSRVx11/Referral+Handling+Changes
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith ( FrameworkRunner.class )
@CreateDS(name = "RenameReferralIgnoreIT")
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
    
    // Another Sub-root
    "dn: o=PNN,c=WW,ou=system",
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
    "sn: akarasulu",
    
    // Entry # 2
    "dn: cn=Alex,o=MNN,c=WW,ou=system",
    "objectClass: person",
    "objectClass: top",
    "cn: Alex",
    "sn: akarasulu"
    }
)
public class RenameReferralIgnoreIT extends AbstractLdapTestUnit
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
     * Test a rename of a non existing entry (not a referral), with a referral 
     * in its ancestor, using JNDI ignore.
     */
    @Test
    public void testRenameNotExistingSuperiorReferralAncestorJNDIIgnore() throws Exception
    {
        try
        {
            MNNCtx.addToEnvironment( DirContext.REFERRAL, "ignore" );
            MNNCtx.rename( "cn=Emmanuel Lecharny,ou=apache,ou=roles", "cn=Alex Karasulu,ou=apache,ou=roles" );
            fail();
        }
        catch ( PartialResultException pre )
        {
            assertTrue( true );
        }
    }


    /**
     * Test a rename of a non existing entry (not a referral), with a referral 
     * in its ancestor, using the Core API with the ManageDsaIT flag.
     */
    @Test
    public void testRenameNotExistingSuperiorReferralAncestorCoreAPIWithManageDsaIt() throws Exception
    {
        CoreSession session = service.getAdminSession();
        try
        {
            Dn dn = new Dn( "cn=Emmanuel Lecharny,ou=apache,ou=roles,o=MNN,c=WW,ou=system" );
            Rdn newRdn = new Rdn( "cn=Alex Karasulu" );
            session.rename( dn, newRdn, false, true );
            fail();
        }
        catch ( LdapPartialResultException lpre )
        {
            assertTrue( true );
        }
    }


    /**
     * Test a rename of an existing referral, using JNDI ignore. 
     */
    @Test
    public void testRenameExistingReferralJNDIIgnore() throws Exception
    {
        MNNCtx.addToEnvironment( DirContext.REFERRAL, "ignore" );

        // First check that the object exists
        Object renamed = MNNCtx.lookup( "ou=Roles" );
        assertNotNull( renamed );

        // Also check that the new entry does not exist
        try
        {
            renamed = MNNCtx.lookup( "ou=Groups" );
            fail();
        }
        catch ( NameNotFoundException nnfe )
        {
            assertTrue( true );
        }
        
        // Now renames the referral
        MNNCtx.rename( "ou=roles", "ou=groups" );

        // It should not be there anymore
        try
        {
            renamed = MNNCtx.lookup( "ou=Roles" );
            fail();
        }
        catch ( NameNotFoundException nnfe )
        {
            assertTrue( true );
        }

        // But the new one should be there
        renamed = MNNCtx.lookup( "ou=groups" );
        assertNotNull( renamed );
    }


    /**
     * Test a rename of an existing referral,  using the Core API with
     * the ManageDsaIt flag.  
     */
    @Test
    public void testRenameExistingReferralCoreAPIWithManageDsaIt() throws Exception
    {
        CoreSession session = service.getAdminSession();
        Dn dnRoles = new Dn( "ou=Roles,o=MNN,c=WW,ou=system" );
        Dn dnGroups = new Dn( "ou=Groups,o=MNN,c=WW,ou=system" );
        Rdn newRdn = new Rdn( "ou=Groups" );

        // First check that the object exists
        Entry renamed = session.lookup( dnRoles );
        assertNotNull( renamed );

        // Also check that the new entry does not exist
        try
        {
            renamed = session.lookup( dnGroups );
            fail();
        }
        catch ( LdapNoSuchObjectException lnsoe )
        {
            assertTrue( true );
        }
        
        // Now renames the referral
        session.rename( dnRoles, newRdn, false, true );

        // It should not be there anymore
        try
        {
            renamed = session.lookup( dnRoles );
            fail();
        }
        catch ( LdapNoSuchObjectException lnsoe )
        {
            assertTrue( true );
        }

        // But the new one should be there
        renamed = session.lookup( dnGroups );
        assertNotNull( renamed );
    }


    /**
     * Test a rename a referral using an already existing Rdn (the new entry already exists and is a referral),
     * using JNDI ignore
     */
    @Test
    public void testRenameRdnExistIsReferralJNDIIgnore() throws Exception
    {
        try
        {
            MNNCtx.addToEnvironment( DirContext.REFERRAL, "ignore" );
            MNNCtx.rename( "ou=Roles", "ou=People" );
            fail();
        }
        catch ( NameAlreadyBoundException nabe )
        {
            assertTrue( true );
        }
    }


    /**
     * Test a rename a referral using an already existing Rdn (the new entry already exists and is a referral),
     * using the Core API, with the ManageDsaIt flag
     */
    @Test
    public void testRenameRdnExistIsReferralCoreAPIWithManageDsaIt() throws Exception
    {
        CoreSession session = service.getAdminSession();
        Dn dn = new Dn( "ou=Roles,o=MNN,c=WW,ou=system" );
        Rdn newRdn = new Rdn( "ou=People" );

        try
        {
            session.rename( dn, newRdn, false, true );
            fail();
        }
        catch ( LdapEntryAlreadyExistsException leaee )
        {
            assertTrue( true );
        }
    }
}
