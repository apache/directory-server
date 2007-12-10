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


import java.util.HashSet;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.core.unit.AbstractAdminTestCase;
import org.apache.directory.shared.ldap.exception.LdapNoPermissionException;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.util.ArrayUtils;


/**
 * Tests the Authorization service to make sure it is enforcing policies
 * correctly.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AuthorizationServiceAsAdminITest extends AbstractAdminTestCase
{
    /**
     * Makes sure the admin cannot delete the admin account.
     *
     * @throws NamingException if there are problems
     */
    public void testNoDeleteOnAdminByAdmin() throws NamingException
    {
        try
        {
            sysRoot.destroySubcontext( "uid=admin" );
            fail( "admin should not be able to delete his account" );
        }
        catch ( LdapNoPermissionException e )
        {
            assertNotNull( e );
        }
    }


    /**
     * Makes sure the admin cannot rename the admin account.
     *
     * @throws NamingException if there are problems
     */
    public void testNoRdnChangesOnAdminByAdmin() throws NamingException
    {
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
     * Makes sure the admin cannot rename the admin account.
     *
     * @throws NamingException if there are problems
     */
    public void testModifyOnAdminByAdmin() throws NamingException
    {
        Attributes attributes = new AttributesImpl();
        attributes.put( "userPassword", "replaced" );
        sysRoot.modifyAttributes( "uid=admin", DirContext.REPLACE_ATTRIBUTE, attributes );
        Attributes newAttrs = sysRoot.getAttributes( "uid=admin" );
        assertTrue( ArrayUtils.isEquals( "replaced".getBytes(), newAttrs.get( "userPassword" ).get() ) );
    }


    /**
     * Makes sure the admin can see all entries we know of on a subtree search.
     *
     * @throws NamingException if there are problems
     */
    public void testSearchSubtreeByAdmin() throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        HashSet<String> set = new HashSet<String>();
        NamingEnumeration list = sysRoot.search( "", "(objectClass=*)", controls );

        while ( list.hasMore() )
        {
            SearchResult result = ( SearchResult ) list.next();
            set.add( result.getName() );
        }

        assertTrue( set.contains( "ou=system" ) );
        assertTrue( set.contains( "ou=groups,ou=system" ) );
        assertTrue( set.contains( "ou=users,ou=system" ) );
        assertTrue( set.contains( "uid=akarasulu,ou=users,ou=system" ) );
        assertTrue( set.contains( "uid=admin,ou=system" ) );
    }
}
