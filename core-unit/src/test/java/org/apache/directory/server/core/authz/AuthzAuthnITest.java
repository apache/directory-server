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


import javax.naming.NamingException;

import org.apache.directory.shared.ldap.exception.LdapNoPermissionException;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * Tests whether or not authentication with authorization works properly.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AuthzAuthnITest extends AbstractAuthorizationITest
{
    /**
     * Checks to make sure a user can authenticate with RootDSE as the
     * provider URL without need of any access control permissions.
     *
     * @throws javax.naming.NamingException if the test encounters an error
     */
    public void testAuthnWithRootDSE() throws NamingException
    {
        createUser( "billyd", "billyd" );

        LdapDN userName = new LdapDN( "uid=billyd,ou=users,ou=system" ); 
        try
        {
            // Authenticate to RootDSE
            getContextAs( userName, "billyd", "" );
        }
        catch ( LdapNoPermissionException e )
        {
            fail( "Authentication should not have failed." );
        }
    }
    
    
    /**
     * Checks to make sure a user cannot authenticate with a naming context
     * as the provider URL if it does not have appropriate Browse permissions.
     *
     * @throws javax.naming.NamingException if the test encounters an error
     */
    public void testAuthnFailsWithSystemPartition() throws NamingException
    {
        createUser( "billyd", "billyd" );
        
        LdapDN userName = new LdapDN( "uid=billyd,ou=users,ou=system" ); 
        try
        {
            // Authenticate to "ou=system"
            getContextAs( userName, "billyd" );
            fail( "Authentication should have failed." );
        }
        catch ( LdapNoPermissionException e )
        {
            
        }
    }
    
    
    /**
     * Checks to make sure a user can authenticate with a naming context
     * as the provider URL if it has appropriate Browse permissions.
     *
     * @throws javax.naming.NamingException if the test encounters an error
     */
    public void testAuthnPassesWithSystemPartition() throws NamingException
    {
        createUser( "billyd", "billyd" );
        
        // Create ACI with minimum level of required privileges:
        // Only for user "uid=billyd,ou=users,ou=system"
        // Only to The entry "ou=system"
        // Only Browse permission
        // Note: In order to read contents of the bound context
        //       user will need appropriate Read permissions.
        createAccessControlSubentry(
            "grantBrowseForTheWholeNamingContext",
            "{ maximum 0 }", // !!!!! Replace this with "{ minimum 1 }" for practicing !
            "{ " + "identificationTag \"browseACI\", "
            + "precedence 14, " + "authenticationLevel none, " + "itemOrUserFirst userFirst: { "
            + "userClasses { name { \"uid=billyd,ou=users,ou=system\" } }, " + "userPermissions { { "
            + "protectedItems { entry }, "
            + "grantsAndDenials { grantBrowse } } } } }" );
        
        LdapDN userName = new LdapDN( "uid=billyd,ou=users,ou=system" ); 
        try
        {
            // Authenticate to "ou=system"
            getContextAs( userName, "billyd" );
        }
        catch ( LdapNoPermissionException e )
        {
            fail( "Authentication should not have failed." );
        }
    }
}
