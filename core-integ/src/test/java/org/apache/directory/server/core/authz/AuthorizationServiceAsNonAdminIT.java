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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.LdapPrincipal;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.DefaultEntryAttribute;
import org.apache.directory.shared.ldap.model.entry.DefaultModification;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.EntryAttribute;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.exception.LdapNoPermissionException;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.filter.FilterParser;
import org.apache.directory.shared.ldap.model.filter.SearchScope;
import org.apache.directory.shared.ldap.model.ldif.LdifEntry;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.name.Rdn;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests the Authorization service to make sure it is enforcing policies
 * correctly.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith ( FrameworkRunner.class )
@CreateDS(name = "AuthorizationServiceAsNonAdminIT")
public class AuthorizationServiceAsNonAdminIT extends AbstractLdapTestUnit 
{

    /**
     * Makes sure a non-admin user cannot delete the admin account.
     *
     * @throws Exception if there are problems
     */
    @Test
    public void testNoDeleteOnAdminByNonAdmin() throws Exception
    {
        LdifEntry akarasulu = getUserAddLdif();

        service.getAdminSession().add( 
            new DefaultEntry( service.getSchemaManager(), akarasulu.getEntry() ) ); 

        try
        {
            service.getAdminSession().delete( new Dn( "uid=admin,ou=system") );
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
        LdifEntry akarasulu = getUserAddLdif();

        service.getAdminSession().add( 
            new DefaultEntry( service.getSchemaManager(), akarasulu.getEntry() ) );

        try
        {
            service.getAdminSession().rename( 
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
        LdifEntry akarasulu = getUserAddLdif();
        
        service.getAdminSession().add( 
            new DefaultEntry( service.getSchemaManager(), akarasulu.getEntry() ) ); 
        
        // Read the entry we just created using the akarasuluSession
        Entry readEntry = service.getAdminSession().lookup( akarasulu.getDn(), new String[]{ "userPassword"} );
        
        assertTrue( Arrays.equals( akarasulu.get( "userPassword" ).getBytes(), readEntry.get( "userPassword" ).getBytes() ) );

        EntryAttribute attribute = new DefaultEntryAttribute( "userPassword", "replaced" );

        List<Modification> mods = new ArrayList<Modification>();
        
        Modification mod = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, attribute );
        mods.add( mod );
      
        Dn userDn = new Dn( service.getSchemaManager(), "uid=akarasulu,ou=users,ou=system" );
        LdapPrincipal principal = new LdapPrincipal( userDn, AuthenticationLevel.SIMPLE );
        CoreSession akarasuluSession = service.getSession( principal );

        try
        {
            akarasuluSession.modify( 
                new Dn( "uid=admin,ou=system" ), mods );
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
    public void testNoSearchByNonAdmin() throws Exception
    {
        LdifEntry akarasulu = getUserAddLdif();
        
        service.getAdminSession().add( 
            new DefaultEntry( service.getSchemaManager(), akarasulu.getEntry() ) ); 

        try
        {
            ExprNode filter = FilterParser.parse( service.getSchemaManager(), "(objectClass=*)" );
            service.getAdminSession().search( new Dn( "ou=system" ), SearchScope.SUBTREE, filter , AliasDerefMode.DEREF_ALWAYS, null );
        }
        catch ( LdapNoPermissionException e )
        {
            assertNotNull( e );
        }
    }
}
