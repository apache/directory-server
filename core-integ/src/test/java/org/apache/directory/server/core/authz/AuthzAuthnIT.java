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
package org.apache.directory.server.core.authz;


import static org.apache.directory.server.core.authz.AutzIntegUtils.createAccessControlSubentry;
import static org.apache.directory.server.core.authz.AutzIntegUtils.createUser;
import static org.apache.directory.server.core.authz.AutzIntegUtils.getConnectionAs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests whether or not authentication with authorization works properly.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "AuthzAuthnIT")
public class AuthzAuthnIT extends AbstractLdapTestUnit
{

    @Before
    public void setService()
    {
        AutzIntegUtils.service = service;
        service.setAccessControlEnabled( true );
    }


    @After
    public void closeConnections()
    {
        IntegrationUtils.closeConnections();
    }


    /**
     * Checks to make sure a user can authenticate with RootDSE as the
     * provider URL without need of any access control permissions.
     *
     * @throws Exception if the test encounters an error
     */
    @Test
    public void testAuthnWithRootDSE() throws Exception
    {
        createUser( "billyd", "billyd" );

        Dn userName = new Dn( "uid=billyd,ou=users,ou=system" );
        // Authenticate to RootDSE
        LdapConnection connection = getConnectionAs( userName, "billyd" );
        Entry entry = connection.lookup( "" );
        assertNotNull( entry );
        assertEquals( 0, entry.getDn().size() );
    }


    /**
     * Checks to make sure a user cannot authenticate with a naming context
     * as the provider URL if it does not have appropriate Browse permissions.
     *
     * @throws Exception if the test encounters an error
     */
    @Test
    public void testAuthnFailsWithSystemPartition() throws Exception
    {
        createUser( "billyd", "billyd" );

        Dn userName = new Dn( "uid=billyd,ou=users,ou=system" );
        LdapConnection connection = getConnectionAs( userName, "billyd" );
        Entry entry = connection.lookup( "ou=system" );
        assertNull( entry );
    }


    /**
     * Checks to make sure a user can authenticate with a naming context
     * as the provider URL if it has appropriate Browse permissions.
     *
     * @throws Exception if the test encounters an error
     */
    @Ignore("This test is not failing but I want to make sure that this test case is equivalent to its prior JNDI based impl, so ignoring this to get attention")
    @Test
    public void testAuthnPassesWithSystemPartition() throws Exception
    {
        createUser( "billyd", "billyd" );

        // Create ACI with minimum level of required privileges:
        // Only for user "uid=billyd,ou=users,ou=system"
        // Only to The entry "ou=system"
        // Only Browse permission
        // Note: In order to read contents of the bound context
        //       user will need appropriate Read permissions.
        createAccessControlSubentry( "grantBrowseForTheWholeNamingContext", "{ maximum 0 }", // !!!!! Replace this with "{ minimum 1 }" for practicing !
            "{ " + 
            "  identificationTag \"browseACI\", " + 
            "  precedence 14, " + 
            "  authenticationLevel none, " +
            "  itemOrUserFirst userFirst: " + 
            "  { " +
            "    userClasses { name { \"uid=billyd,ou=users,ou=system\" } }, " + 
            "    userPermissions " +
            "    { " + 
            "      { " + 
            "        protectedItems { entry }, " +
            "        grantsAndDenials { grantBrowse } " + 
            "      } " + 
            "    } " + 
            "  } " + 
            "}" );

        Dn userName = new Dn( "uid=billyd,ou=users,ou=system" );

        LdapConnection connection = getConnectionAs( userName, "billyd" );
        Entry entry = connection.lookup( "ou=system" );
        assertNull( entry );
    }
}
