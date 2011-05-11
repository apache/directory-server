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


import static org.apache.directory.server.core.authz.AutzIntegUtils.getAdminConnection;
import static org.apache.directory.server.core.authz.AutzIntegUtils.getConnectionAs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;

import org.apache.commons.lang.ArrayUtils;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.cursor.EntryCursor;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapNoPermissionException;
import org.apache.directory.shared.ldap.model.message.ModifyRequest;
import org.apache.directory.shared.ldap.model.message.ModifyRequestImpl;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.name.Rdn;
import org.apache.directory.shared.util.Strings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests the Authorization service to make sure it is enforcing policies
 * correctly.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "AuthorizationServiceAsAdminIT")
public class AuthorizationServiceAsAdminIT extends AbstractLdapTestUnit
{

    @Before
    public void setService()
    {
        AutzIntegUtils.service = getService();
    }


    @After
    public void closeConnections()
    {
        IntegrationUtils.closeConnections();
    }


    /**
     * Makes sure the admin cannot delete the admin account.
     *
     * @throws Exception if there are problems
     */
    @Test( expected=LdapNoPermissionException.class )
    public void testNoDeleteOnAdminByAdmin() throws Exception
    {
        getAdminConnection().delete( "uid=admin,ou=system" );
    }


    /**
     * Makes sure the admin cannot rename the admin account.
     *
     * @throws Exception if there are problems
     */
    @Test( expected=LdapNoPermissionException.class )
    public void testNoRdnChangesOnAdminByAdmin() throws Exception
    {
        getAdminConnection().rename( new Dn( "uid=admin,ou=system" ), new Rdn( "uid=alex" ) );
    }


    /**
     * Makes sure the admin can update the admin account password.
     *
     * @throws Exception if there are problems
     */
    @Test
    public void testModifyOnAdminByAdmin() throws Exception
    {
        LdapConnection connection = getAdminConnection();
        Dn adminDn = new Dn( "uid=admin,ou=system" );
        ModifyRequest modReq = new ModifyRequestImpl();
        modReq.setName( adminDn );
        String newPwd = "replaced";
        modReq.replace( SchemaConstants.USER_PASSWORD_AT, newPwd );
        connection.modify( modReq );
        connection.close();

        connection = getConnectionAs( adminDn, newPwd );
        Entry entry = connection.lookup( adminDn.getName() );
        assertTrue( ArrayUtils.isEquals( Strings.getBytesUtf8(newPwd), entry.get( "userPassword" ).get()
            .getBytes() ) );
    }


    /**
     * Makes sure the admin can see all entries we know of on a subtree search.
     *
     * @throws Exception if there are problems
     */
    @Test
    public void testSearchSubtreeByAdmin() throws Exception
    {
        LdapConnection connection = getAdminConnection();

        HashSet<String> set = new HashSet<String>();

        EntryCursor cursor = connection.search( "ou=system", "(objectClass=*)", SearchScope.SUBTREE, "*" );

        while ( cursor.next() )
        {
            Entry result = cursor.get();
            set.add( result.getDn().getName() );
        }

        cursor.close();

        assertEquals( 10, set.size() );
        assertTrue( set.contains( "ou=system" ) );
        assertTrue( set.contains( "ou=configuration,ou=system" ) );
        assertTrue( set.contains( "ou=interceptors,ou=configuration,ou=system" ) );
        assertTrue( set.contains( "ou=partitions,ou=configuration,ou=system" ) );
        assertTrue( set.contains( "ou=services,ou=configuration,ou=system" ) );
        assertTrue( set.contains( "ou=groups,ou=system" ) );
        assertTrue( set.contains( "cn=Administrators,ou=groups,ou=system" ) );
        assertTrue( set.contains( "ou=users,ou=system" ) );
        assertTrue( set.contains( "prefNodeName=sysPrefRoot,ou=system" ) );
        assertTrue( set.contains( "uid=admin,ou=system" ) );
    }
}
