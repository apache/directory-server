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

 
import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContextThrowOnRefferal;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.StringReader;

import javax.naming.NamingEnumeration;
import javax.naming.ReferralException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.ManageReferralControl;

import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.ldif.LdifEntry;
import org.apache.directory.shared.ldap.model.ldif.LdifReader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests to make sure the server is operating correctly when handling referrals.
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
    // Add new ref for ou=RemoteUsers
    "dn: ou=RemoteUsers,ou=system",
    "objectClass: top",
    "objectClass: referral",
    "objectClass: extensibleObject",
    "ou: RemoteUsers",
    "ref: ldap://fermi:10389/ou=users,ou=system",
    "ref: ldap://hertz:10389/ou=users,dc=example,dc=com",
    "ref: ldap://maxwell:10389/ou=users,ou=system",
    
    "dn: c=France,ou=system",
    "objectClass: top",
    "objectClass: country",
    "c: France",
    
    "dn: c=USA,ou=system",
    "objectClass: top",
    "objectClass: country",
    "c: USA",
    
    "dn: l=Paris,c=france,ou=system",
    "objectClass: top",
    "objectClass: locality",
    "l: Paris",
    
    "dn: l=Jacksonville,c=usa,ou=system",
    "objectClass: top",
    "objectClass: locality",
    "l: Jacksonville",
    
    "dn: cn=emmanuel lecharny,l=paris,c=france,ou=system",
    "objectClass: top",
    "objectClass: person",
    "objectClass: residentialPerson",
    "cn: emmanuel lecharny",
    "sn: elecharny",
    "l: Paris",
    
    "dn: cn=alex karasulu,l=jacksonville,c=usa,ou=system",
    "objectClass: top",
    "objectClass: person",
    "objectClass: residentialPerson",
    "cn: alex karasulu",
    "sn: karasulu",
    "l: Jacksonville",
    
    "dn: ou=Countries,ou=system",
    "objectClass: top",
    "objectClass: organizationalUnit",
    "ou: Countries"
    }
)
public class ReferralSearchMoveAndRenameIT extends AbstractLdapTestUnit
{    
    @Before
    public void setupReferrals() throws Exception
    {
        ldapServer.getDirectoryService().getChangeLog().setEnabled( false );
        
        String ldif =
        "dn: c=europ,ou=Countries,ou=system\n" +
        "objectClass: top\n" +
        "objectClass: referral\n" +
        "objectClass: extensibleObject\n" +
        "c: europ\n" +
        "ref: ldap://localhost:" + ldapServer.getPort() + "/c=france,ou=system\n\n" +

        "dn: c=america,ou=Countries,ou=system\n" +
        "objectClass: top\n" +
        "objectClass: referral\n" +
        "objectClass: extensibleObject\n" +
        "c: america\n" +
        "ref: ldap://localhost:" + ldapServer.getPort() + "/c=usa,ou=system\n\n";

        LdifReader reader = new LdifReader( new StringReader( ldif ) );
        
        while ( reader.hasNext() )
        {
            LdifEntry entry = reader.next();
            ldapServer.getDirectoryService().getAdminSession().add( 
                new DefaultEntry( ldapServer.getDirectoryService().getSchemaManager(), entry.getEntry() ) ); 
        }
    }
    
    

    /**
     * Test of an search operation with a referral after the entry
     * has been moved.
     *
     * search for "cn=alex karasulu" on "c=usa, ou=system"
     * we should get a referral URL thrown, which point to
     * "c=usa, ou=system", and ask for a subtree search
     */
    @Test
    public void testSearchBaseWithReferralThrowAfterMoveAndRename() throws Exception
    {
        DirContext ctx = getWiredContextThrowOnRefferal( ldapServer );

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );

        try
        {
            ctx.search( "c=america,ou=Countries,ou=system", "(cn=alex karasulu)", controls );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch ( ReferralException re )
        {
            String referral = (String)re.getReferralInfo();
            assertEquals( "ldap://localhost:" + ldapServer.getPort() + "/c=usa,ou=system??base", referral );
        }
        
        ((LdapContext)ctx).setRequestControls( new javax.naming.ldap.Control[]{new ManageReferralControl()} );

        // Now let's move the entry
        ctx.rename( "c=america,ou=Countries,ou=system", "c=us,ou=system" );

        controls.setSearchScope( SearchControls.OBJECT_SCOPE );

        ((LdapContext)ctx).setRequestControls( new javax.naming.ldap.Control[]{} );

        try
        {
            NamingEnumeration<SearchResult> results = ctx.search( "c=us,ou=system", "(cn=alex karasulu)", controls );
            
            results.next();
            fail( "Should fail here throwing a ReferralException" );
        }
        catch ( ReferralException re )
        {
            String referral = (String)re.getReferralInfo();
            assertEquals( "ldap://localhost:" + ldapServer.getPort() + "/c=usa,ou=system??base", referral );
        }
    }
}