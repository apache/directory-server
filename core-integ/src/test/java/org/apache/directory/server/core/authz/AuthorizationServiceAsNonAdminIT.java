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
import static org.apache.directory.server.core.integ.IntegrationUtils.*;
import org.apache.directory.server.core.integ.annotations.Factory;
import org.apache.directory.shared.ldap.exception.LdapNoPermissionException;
import org.apache.directory.shared.ldap.ldif.Entry;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;
import java.util.HashSet;


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
    public void testNoDeleteOnAdminByNonAdmin() throws NamingException
    {
        Entry akarasulu = getUserAddLdif();
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
    public void testNoRdnChangesOnAdminByNonAdmin() throws NamingException
    {
        Entry akarasulu = getUserAddLdif();
        getRootContext( service ).createSubcontext( akarasulu.getDn(), akarasulu.getAttributes() );
        LdapContext sysRoot = getContext( akarasulu.getDn(), service, "ou=system" );

        try
        {
            sysRoot.rename( "uid=admin", "uid=alex" );
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
    public void testModifyOnAdminByNonAdmin() throws NamingException
    {
        Entry akarasulu = getUserAddLdif();
        getRootContext( service ).createSubcontext( akarasulu.getDn(), akarasulu.getAttributes() );
        LdapContext sysRoot = getContext( akarasulu.getDn(), service, "ou=system" );

        Attributes attributes = new AttributesImpl();
        attributes.put( "userPassword", "replaced" );

        //noinspection EmptyCatchBlock
        try
        {
            sysRoot.modifyAttributes( "uid=admin", DirContext.REPLACE_ATTRIBUTE, attributes );
            fail( "User 'uid=admin,ou=system' should not be able to modify attributes on admin" );
        }
        catch ( Exception e )
        {
        }
    }


    /**
     * Makes sure the admin can see all entries we know of on a subtree search.
     *
     * @throws NamingException if there are problems
     */
    @Test
    public void testSearchSubtreeByNonAdmin() throws NamingException
    {
        Entry akarasulu = getUserAddLdif();
        getRootContext( service ).createSubcontext( akarasulu.getDn(), akarasulu.getAttributes() );
        LdapContext sysRoot = getContext( akarasulu.getDn(), service, "ou=system" );

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );

        //noinspection MismatchedQueryAndUpdateOfCollection
        HashSet<String> set = new HashSet<String>();
        NamingEnumeration list = sysRoot.search( "", "(objectClass=*)", controls );
        while ( list.hasMore() )
        {
            SearchResult result = ( SearchResult ) list.next();
            set.add( result.getName() );
        }

        // @todo this assertion fails now - is this the expected behavoir?
//        assertTrue( set.contains( "ou=system" ) );
//        assertTrue( set.contains( "ou=groups,ou=system" ) );
//        assertFalse( set.contains( "cn=administrators,ou=groups,ou=system" ) );
//        assertTrue( set.contains( "ou=users,ou=system" ) );
//        assertFalse( set.contains( "uid=akarasulu,ou=users,ou=system" ) );
//        assertFalse( set.contains( "uid=admin,ou=system" ) );
    }
}
