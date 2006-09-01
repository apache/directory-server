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


import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.NoPermissionException;
import javax.naming.directory.DirContext;


/**
 * Some tests to make sure users in the cn=Administrators,ou=groups,ou=system 
 * group behave as admin like users will full access rights.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AdministratorsGroupTest extends AbstractAuthorizationITest
{
    public void setUp() throws Exception
    {
        if ( getName().equals( "testDefaultNonAdminReadAccessToGroups" ) )
        {
            configuration.setAccessControlEnabled( false );
        }
        
        super.setUp();
    }
    
    
    boolean canReadAdministrators( DirContext ctx ) throws NamingException
    {
        try
        {
            ctx.getAttributes( "cn=Administrators,ou=groups" );
            return true;
        }
        catch ( NoPermissionException e )
        {
            return false;
        }
    }
    
    
    /**
     * Checks to make sure a non-admin user which is not in the Administrators 
     * group cannot access entries under ou=groups,ou=system.  Also check that 
     * after adding that user to the group they see those groups.  This test 
     * does NOT use the DefaultAuthorizationService but uses the one based on
     * ACI.
     */
    public void testNonAdminReadAccessToGroups() throws Exception
    {
        Name billydDn = super.createUser( "billyd", "s3kr3t" );
        
        // this should fail with a no permission exception because we
        // are not allowed to browse ou=system without an ACI 
        try
        {
            super.getContextAs( billydDn, "s3kr3t" );
            fail( "Should not get here since we cannot browse ou=system" );
        }
        catch( NoPermissionException e )
        {
        }
        
        // add billyd to administrators and try again
        super.addUserToGroup( "billyd", "Administrators" );

        // billyd should now be able to read ou=system and the admin group
        DirContext ctx = super.getContextAs( billydDn, "s3kr3t" );
        assertTrue( canReadAdministrators( ctx ) );
    }
    
    
    /**
     * Checks to make sure a non-admin user which is not in the Administrators 
     * group cannot access entries under ou=groups,ou=system.  Also check that 
     * after adding that user to the group they see those groups.
     */
    public void testDefaultNonAdminReadAccessToGroups() throws Exception
    {
        Name billydDn = super.createUser( "billyd", "s3kr3t" );
        DirContext ctx = super.getContextAs( billydDn, "s3kr3t" );
        
        // billyd should not be able to read the admin group
        assertFalse( canReadAdministrators( ctx ) );
        
        // add billyd to administrators and try again
        super.addUserToGroup( "billyd", "Administrators" );

        // billyd should now be able to read the admin group
        assertTrue( canReadAdministrators( ctx ) );
    }
}
