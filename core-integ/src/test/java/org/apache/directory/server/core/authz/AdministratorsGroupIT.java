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


import static org.apache.directory.server.core.authz.AutzIntegUtils.addUserToGroup;
import static org.apache.directory.server.core.authz.AutzIntegUtils.createUser;
import static org.apache.directory.server.core.authz.AutzIntegUtils.getConnectionAs;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Some tests to make sure users in the cn=Administrators,ou=groups,ou=system 
 * group behave as admin like users will full access rights.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( ApacheDSTestExtension.class )
@CreateDS(name = "AdministratorsGroupIT")
public class AdministratorsGroupIT extends AbstractLdapTestUnit
{

    @BeforeEach
    public void setService()
    {
        AutzIntegUtils.service = getService();
    }


    @AfterEach
    public void closeConnections()
    {
        IntegrationUtils.closeConnections();
    }


    boolean canReadAdministrators( LdapConnection connection ) throws Exception
    {
        Entry res = connection.lookup( "cn=Administrators,ou=groups,ou=system" );

        if ( res == null )
        {
            return false;
        }

        return true;
    }


    /**
     * Checks to make sure a non-admin user which is not in the Administrators 
     * group cannot access entries under ou=groups,ou=system.  Also check that 
     * after adding that user to the group they see those groups.  This test 
     * does NOT use the DefaultAuthorizationInterceptor but uses the one based on
     * ACI.
     * 
     * @throws Exception on failures
     */
    @Test
    @CreateDS(enableAccessControl = true, name = "testNonAdminReadAccessToGroups-method")
    public void testNonAdminReadAccessToGroups() throws Exception
    {
        Dn billydDn = createUser( "billyd", "s3kr3t" );

        // this should fail with a no permission exception because we
        // are not allowed to browse ou=system without an ACI 
        LdapConnection connection = getConnectionAs( billydDn, "s3kr3t" );
        assertTrue( connection.isAuthenticated() );
        assertFalse( canReadAdministrators( connection ) );

        // add billyd to administrators and try again
        addUserToGroup( "billyd", "Administrators" );

        // billyd should now be able to read ou=system and the admin group
        connection = getConnectionAs( billydDn, "s3kr3t" );
        assertTrue( canReadAdministrators( connection ) );
    }


    /**
     * Checks to make sure a non-admin user which is not in the Administrators
     * group cannot access entries under ou=groups,ou=system.  Also check that
     * after adding that user to the group they see those groups.
     *
     * @throws Exception on failure
     */
    @Test
    @CreateDS(name = "testDefaultNonAdminReadAccessToGroups-method")
    public void testDefaultNonAdminReadAccessToGroups() throws Exception
    {
        Dn billydDn = createUser( "billyd", "s3kr3t" );
        assertFalse( getService().isAccessControlEnabled() );
        LdapConnection connection = getConnectionAs( billydDn, "s3kr3t" );

        // billyd should not be able to read the admin group
        assertFalse( canReadAdministrators( connection ) );

        // add billyd to administrators and try again
        addUserToGroup( "billyd", "Administrators" );

        // billyd should now be able to read the admin group
        assertTrue( canReadAdministrators( connection ) );
    }
}
