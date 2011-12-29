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


import static org.apache.directory.server.core.integ.IntegrationUtils.getUserAddLdif;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.DefaultAttribute;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.DefaultModification;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.exception.LdapNoPermissionException;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.filter.FilterParser;
import org.apache.directory.shared.ldap.model.ldif.LdifEntry;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.name.Rdn;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests the Authorization service to make sure it is enforcing policies
 * correctly.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "AuthorizationServiceAsNonAdminIT")
public class AuthorizationServiceAsNonAdminIT extends AbstractLdapTestUnit
{
    private CoreSession session;
    private CoreSession userSession;
    private SchemaManager schemaManager;


    @Before
    public void setup() throws Exception
    {
        LdifEntry akarasulu = getUserAddLdif();

        session = getService().getAdminSession();
        schemaManager = getService().getSchemaManager();

        session.add( new DefaultEntry( schemaManager, akarasulu.getEntry() ) );
        Dn userDn = new Dn( schemaManager, "uid=akarasulu,ou=users,ou=system" );
        LdapPrincipal principal = new LdapPrincipal( schemaManager, userDn,
            AuthenticationLevel.SIMPLE );
        userSession = getService().getSession( principal );
    }


    /**
     * Makes sure a non-admin user cannot delete the admin account.
     *
     * @throws Exception if there are problems
     */
    @Test
    public void testNoDeleteOnAdminByNonAdmin() throws Exception
    {
        try
        {
            userSession.delete( new Dn( "uid=admin,ou=system" ) );
            fail( "User 'admin' should not be able to delete his account" );
        }
        catch ( LdapNoPermissionException e )
        {
            assertNotNull( e );
        }
    }


    /**
     * Makes sure the admin user cannot delete the admin account.
     *
     * @throws Exception if there are problems
     */
    @Test
    public void testNoDeleteOnAdminByAdmin() throws Exception
    {
        try
        {
            session.delete( new Dn( "uid=admin,ou=system" ) );
            fail( "User 'admin' should not be able to delete his account" );
        }
        catch ( LdapNoPermissionException e )
        {
            assertNotNull( e );
        }
    }


    /**
     * Makes sure a non-admin user cannot rename the admin account.
     *
     * @throws Exception if there are problems
     */
    @Test
    public void testNoRdnChangesOnAdminByNonAdmin() throws Exception
    {
        try
        {
            userSession.rename(
                new Dn( "uid=admin,ou=system" ),
                new Rdn( "uid=alex" ),
                false );
            fail( "admin should not be able to rename his account" );
        }
        catch ( LdapNoPermissionException e )
        {
            assertNotNull( e );
        }
    }


    /**
     * Makes sure the a non-admin user cannot rename the admin account.
     *
     * @throws Exception on error
     */
    @Test
    public void testModifyOnAdminByNonAdmin() throws Exception
    {
        LdifEntry user = getUserAddLdif();

        // Read the entry we just created using the akarasuluSession
        Entry readEntry = session.lookup( user.getDn(), "userPassword" );

        assertTrue( Arrays.equals( user.get( "userPassword" ).getBytes(), readEntry.get( "userPassword" )
            .getBytes() ) );

        Attribute attribute = new DefaultAttribute( "userPassword", "replaced" );

        Modification mod = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, attribute );

        try
        {
            userSession.modify( new Dn( "uid=admin,ou=system" ), mod );
            fail( "User 'uid=admin,ou=system' should not be able to modify attributes on admin" );
        }
        catch ( Exception e )
        {
        }
    }


    /**
     * Makes sure non-admin cannot search under ou=system.
     *
     * @throws Exception if there are problems
     */
    @Test
    @Ignore
    // This test is blantantly wrong
    public void testNoSearchByNonAdmin() throws Exception
    {
        LdifEntry akarasulu = getUserAddLdif();

        session.add(
            new DefaultEntry( schemaManager, akarasulu.getEntry() ) );

        ExprNode filter = FilterParser.parse( schemaManager, "(objectClass=*)" );
        EntryFilteringCursor cursor = userSession.search( new Dn( "ou=system" ), SearchScope.OBJECT, filter,
            AliasDerefMode.DEREF_ALWAYS, null );
        cursor.next();

        try
        {
            cursor.get();
        }
        catch ( LdapNoPermissionException e )
        {
            assertNotNull( e );
            cursor.close();
        }
    }
}
