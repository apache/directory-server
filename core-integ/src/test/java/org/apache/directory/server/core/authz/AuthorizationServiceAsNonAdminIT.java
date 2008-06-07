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


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.integ.CiRunner;
import static org.apache.directory.server.core.integ.IntegrationUtils.getRootContext;
import static org.apache.directory.server.core.integ.IntegrationUtils.getContext;
import static org.apache.directory.server.core.integ.IntegrationUtils.getUserAddLdif;
import org.apache.directory.server.core.integ.annotations.Factory;
import org.apache.directory.shared.ldap.exception.LdapNoPermissionException;
import org.apache.directory.shared.ldap.ldif.LdifEntry;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.ldap.LdapContext;


/**
 * Tests the Authorization service to make sure it is enforcing policies
 * correctly.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith ( CiRunner.class )
@Factory ( AutzIntegUtils.ServiceFactory.class )
public class AuthorizationServiceAsNonAdminIT 
{
    public static DirectoryService service;


    /**
     * Makes sure a non-admin user cannot delete the admin account.
     *
     * @throws NamingException if there are problems
     */
    @Test
    public void testNoDeleteOnAdminByNonAdmin() throws Exception
    {
        LdifEntry akarasulu = getUserAddLdif();
        getRootContext( service ).createSubcontext( akarasulu.getDn(), akarasulu.getAttributes() );

        try
        {
            getContext( akarasulu.getDn(), service, "ou=system" ).destroySubcontext( "uid=admin" );
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
     * @throws NamingException if there are problems
     */
    @Test
    public void testNoRdnChangesOnAdminByNonAdmin() throws Exception
    {
        LdifEntry akarasulu = getUserAddLdif();
        getRootContext( service ).createSubcontext( akarasulu.getDn(), akarasulu.getAttributes() );
        LdapContext rootDSE = getContext( akarasulu.getDn(), service, "" );

        try
        {
            rootDSE.rename( "uid=admin,ou=system", "uid=alex,ou=system" );
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
     * @throws NamingException on error
     */
    @Test
    public void testModifyOnAdminByNonAdmin() throws Exception
    {
        LdifEntry akarasulu = getUserAddLdif();
        getRootContext( service ).createSubcontext( akarasulu.getDn(), akarasulu.getAttributes() );
        LdapContext rootDSE = getContext( akarasulu.getDn(), service, "" );

        Attributes attributes = new AttributesImpl();
        attributes.put( "userPassword", "replaced" );

        //noinspection EmptyCatchBlock
        try
        {
            rootDSE.modifyAttributes( "uid=admin,ou=system", DirContext.REPLACE_ATTRIBUTE, attributes );
            fail( "User 'uid=admin,ou=system' should not be able to modify attributes on admin" );
        }
        catch ( Exception e )
        {
        }
    }


    /**
     * Makes sure non-admin cannot search under ou=system.
     *
     * @throws NamingException if there are problems
     */
    @Test
    public void testNoSearchByNonAdmin() throws Exception
    {
        LdifEntry akarasulu = getUserAddLdif();
        getRootContext( service ).createSubcontext( akarasulu.getDn(), akarasulu.getAttributes() );
        LdapContext rootDSE = getContext( akarasulu.getDn(), service, "" );

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        
        try
        {
            rootDSE.search( "ou=system", "(objectClass=*)", controls );
        }
        catch ( LdapNoPermissionException e )
        {
            assertNotNull( e );
        }
    }
}
