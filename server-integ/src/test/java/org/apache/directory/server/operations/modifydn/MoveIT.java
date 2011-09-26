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
package org.apache.directory.server.operations.modifydn;


import static org.apache.directory.server.integ.ServerIntegrationUtils.getAdminConnection;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.naming.NamingEnumeration;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.model.exception.LdapUnwillingToPerformException;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test case with different modify Dn operations which move the entry under a
 * new superior.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP") })
@ApplyLdifs(
    {
        // Entry # 1
        "dn: uid=akarasulu,ou=users,ou=system", "objectClass: uidObject", "objectClass: person", "objectClass: top",
        "uid: akarasulu", "cn: Alex Karasulu",
        "sn: karasulu",
        // Entry # 2
        "dn: ou=NewSuperior,ou=system", "objectClass: organizationalUnit", "objectClass: top", "ou: NewSuperior",

        "dn: ou=parent,ou=system", "changetype: add", "objectClass: organizationalUnit", "objectClass: top",
        "ou: parent",

        "dn: ou=child,ou=parent,ou=system", "changetype: add", "objectClass: organizationalUnit", "objectClass: top",
        "ou: child" })
public class MoveIT extends AbstractLdapTestUnit
{
    private static final String DN = "uid=akarasulu,ou=users,ou=system";
    private static final String NEW_DN = "uid=akarasulu,ou=NewSuperior,ou=system";
    private static final String NEW_DN2 = "uid=elecharny,ou=NewSuperior,ou=system";


    @Test
    public void testMoveNoRdnChange() throws Exception
    {
        LdapContext ctx = getWiredContext( getLdapServer() );
        ctx.rename( DN, NEW_DN );

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );

        NamingEnumeration<SearchResult> results = ctx.search( NEW_DN, "(objectClass=*)", controls );
        assertNotNull( results );
        assertTrue( "Could not find entry after move.", results.hasMore() );
        SearchResult result = results.next();
        assertNotNull( result );
        assertEquals( NEW_DN, result.getNameInNamespace() );

        results.close();
        ctx.close();
    }


    @Test
    public void testMoveAndRdnChange() throws Exception
    {
        LdapContext ctx = getWiredContext( getLdapServer() );
        ctx.rename( DN, NEW_DN2 );

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );

        NamingEnumeration<SearchResult> results = ctx.search( NEW_DN2, "(objectClass=*)", controls );
        assertNotNull( results );
        assertTrue( "Could not find entry after move.", results.hasMore() );
        SearchResult result = results.next();
        assertNotNull( result );
        assertEquals( NEW_DN2, result.getNameInNamespace() );

        results.close();
        ctx.close();
    }


    @Test
    public void testIllegalMove() throws Exception
    {

        LdapConnection con = getAdminConnection( getLdapServer() );

        //now do something bad: make the parent a child of its own child 
        try
        {
            con.move( "ou=parent,ou=system", "ou=child,ou=parent,ou=system" );
            fail();
        }
        catch ( LdapUnwillingToPerformException lutpe )
        {
            assertTrue( true );
        }
        
        con.close();
    }


    @Test
    public void testIllegalMoveToSameDN() throws Exception
    {
        LdapConnection con = getAdminConnection( getLdapServer() );

        //now do something bad: try to move the entry to the same Dn
        try
        {
            con.move( "ou=parent,ou=system", "ou=parent,ou=system" );
            fail();
        }
        catch ( LdapUnwillingToPerformException lutpe )
        {
            assertTrue( true );
        }
        con.close();
    }
}
