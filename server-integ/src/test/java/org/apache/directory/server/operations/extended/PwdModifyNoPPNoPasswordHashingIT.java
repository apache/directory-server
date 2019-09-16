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
package org.apache.directory.server.operations.extended;


import static org.apache.directory.server.core.integ.IntegrationUtils.getAdminNetworkConnection;
import static org.apache.directory.server.core.integ.IntegrationUtils.getAnonymousNetworkConnection;
import static org.apache.directory.server.core.integ.IntegrationUtils.getNetworkConnectionAs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.directory.api.ldap.extras.extended.pwdModify.PasswordModifyRequest;
import org.apache.directory.api.ldap.extras.extended.pwdModify.PasswordModifyRequestImpl;
import org.apache.directory.api.ldap.extras.extended.pwdModify.PasswordModifyResponse;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapAuthenticationException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.AddRequest;
import org.apache.directory.api.ldap.model.message.AddRequestImpl;
import org.apache.directory.api.ldap.model.message.AddResponse;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.password.PasswordUtil;
import org.apache.directory.api.util.Strings;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.ldap.handlers.extended.PwdModifyHandler;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
<<<<<<< HEAD
 * Test the PwdModify extended operation
=======
 * Test the PwdModify extended operation, when no PasswordPolicy or PasswordHashing interceptor
 * are present.
>>>>>>> c42030f59692518133715f0a4753ce24bb63b2d8
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateLdapServer(
    transports =
        { @CreateTransport(protocol = "LDAP") },
    extendedOpHandlers =
        { PwdModifyHandler.class },
    allowAnonymousAccess = true)
@CreateDS(name = "PasswordPolicyTest")
public class PwdModifyNoPPNoPasswordHashingIT extends AbstractLdapTestUnit
{
    /**
     * Add a user with a password
     */
    private void addUser( LdapConnection adminConnection, String user, Object password ) throws Exception
    {
        Entry userEntry = new DefaultEntry(
            "cn=" + user + ",ou=system",
            "ObjectClass: top",
            "ObjectClass: person",
            "cn", user,
            "sn", user + "_sn",
            "userPassword", password );

        AddRequest addRequest = new AddRequestImpl();
        addRequest.setEntry( userEntry );

        AddResponse addResp = adminConnection.add( addRequest );
        assertEquals( ResultCodeEnum.SUCCESS, addResp.getLdapResult().getResultCode() );
    }
    
    
    /**
     * Add a user with no password
     */
    private void addUserNoPassword( LdapConnection adminConnection, String user ) throws Exception
    {
        Entry userEntry = new DefaultEntry(
            "cn=" + user + ",ou=system",
            "ObjectClass: top",
            "ObjectClass: person",
            "cn", user,
            "sn", user + "_sn" );

        AddRequest addRequest = new AddRequestImpl();
        addRequest.setEntry( userEntry );

        AddResponse addResp = adminConnection.add( addRequest );
        assertEquals( ResultCodeEnum.SUCCESS, addResp.getLdapResult().getResultCode() );
    }


    /**
     * Add a user with 2 passwords
     */
    private void addUser2Passwords( LdapConnection adminConnection, String user, Object password1, Object password2 ) throws Exception
    {
        Entry userEntry = new DefaultEntry(
            "cn=" + user + ",ou=system",
            "ObjectClass: top",
            "ObjectClass: person",
            "cn", user,
            "sn", user + "_sn",
            "userPassword", password1, 
            "userPassword", password2 );

        AddRequest addRequest = new AddRequestImpl();
        addRequest.setEntry( userEntry );

        AddResponse addResp = adminConnection.add( addRequest );
        assertEquals( ResultCodeEnum.SUCCESS, addResp.getLdapResult().getResultCode() );
    }


    //-----------------------------------------------------------------------------------
    // Self password modification  with one password
    //-----------------------------------------------------------------------------------
    /**
     * Modify an existing user password while the user is connected:
     * o the userIdentity is not provided
     * o the old password is not provided
     * o the new password is new
     * o the entry has a userPassword attribute
     * o the userPassword attribute contains only one value
     * 
     * At the same time, PP and passwordHashing interceptor are disabled
     */
    @Test
    public void testModifyOwnPasswordNoOldNew() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        {
            // Create a user
            addUser( adminConnection, "User1", "secret1" );
    
            // Bind as the user
            try ( LdapConnection userConnection = getNetworkConnectionAs( getLdapServer(), "cn=user1,ou=system", "secret1" ) )
            {
                Entry user = userConnection.lookup( "cn=user1,ou=system", "modifyTimestamp" );
                
                // Now change the password
                PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
                pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "secret1Bis" ) );
        
                // Send the request
                PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) userConnection.extended( pwdModifyRequest );
        
                assertEquals( ResultCodeEnum.SUCCESS, pwdModifyResponse.getLdapResult().getResultCode() );
        
                // Now try to bind with the new password
                try ( LdapConnection newUserConnection = getNetworkConnectionAs( ldapServer, "cn=User1,ou=system", "secret1Bis" ) )
                {
                    Entry entry = newUserConnection.lookup( "cn=User1,ou=system", "userPassword", "modifyTimestamp" );
            
                    assertNotNull( entry );
                    assertTrue( entry.containsAttribute( "userPassword" ) );
                    Attribute userPassword = entry.get( "userPassword" );
                    
                    assertNull( PasswordUtil.findAlgorithm( userPassword.getBytes() ) );
            
                    assertEquals( 1, userPassword.size() );
                    assertEquals( "secret1Bis", userPassword.getString() );
                    assertNull( user.get( "modifyTimestamp" ) );
                    assertNotNull( entry.get( "modifyTimestamp" ).getString() );
                }
            }
        }
    }


    /**
     * Modify an existing user password while the user is connected:
     * o the userIdentity is not provided
     * o the old password is not provided
     * o the new password exists
     * o the entry has a userPassword attribute
     * o the userPassword attribute contains only one value
     * 
     * At the same time, PP and passwordHashing interceptor are disabled
     */
    @Test
    public void testModifyOwnPasswordNoOldNewExists() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        {
            // Create a user
            addUser( adminConnection, "User1", "secret1" );
    
            // Bind as the user
            Entry user;
            
            try ( LdapConnection userConnection = getNetworkConnectionAs( getLdapServer(), "cn=user1,ou=system", "secret1" ) )
            {
                user = userConnection.lookup( "cn=user1,ou=system", "modifyTimestamp" );
                
                // Now change the password
                PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
                pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "secret1" ) );
        
                // Send the request
                PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) userConnection.extended( pwdModifyRequest );
        
                assertEquals( ResultCodeEnum.SUCCESS, pwdModifyResponse.getLdapResult().getResultCode() );
            }
    
            // Now try to bind with the new password
            try ( LdapConnection userConnection = getNetworkConnectionAs( ldapServer, "cn=User1,ou=system", "secret1" ) )
            {
                Entry entry = userConnection.lookup( "cn=User1,ou=system", "userPassword", "modifyTimestamp" );
        
                assertNotNull( entry );
                assertTrue( entry.containsAttribute( "userPassword" ) );
                Attribute userPassword = entry.get( "userPassword" );
                
                assertNull( PasswordUtil.findAlgorithm( userPassword.getBytes() ) );
        
                assertEquals( 1, userPassword.size() );
                assertEquals( "secret1", userPassword.getString() );
                assertNull( user.get( "modifyTimestamp" ) );
                assertNull( entry.get( "modifyTimestamp" ) );
            }
        }
    }


    /**
     * Modify an existing user password while the user is connected:
     * o the userIdentity is not provided
     * o the old password is provided
     * o the new password is new
     * o the entry has a userPassword attribute
     * o the userPassword attribute contains only one value
     * 
     * At the same time, PP and passwordHashing interceptor are disabled
     */
    @Test
    public void testModifyOwnPasswordOldNew() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        {
            // Create a user
            addUser( adminConnection, "User1", "secret1" );
        
            // Bind as the user
            try ( LdapConnection userConnection = getNetworkConnectionAs( getLdapServer(), "cn=user1,ou=system", "secret1" ) )
            {
                // Now change the password
                PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
                pwdModifyRequest.setOldPassword( Strings.getBytesUtf8( "secret1" ) );
                pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "secret1Bis" ) );
            
                // Send the request
                PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) userConnection.extended( pwdModifyRequest );
            
                assertEquals( ResultCodeEnum.SUCCESS, pwdModifyResponse.getLdapResult().getResultCode() );
            }
        
            // Now try to bind with the new password
            try ( LdapConnection userConnection = getNetworkConnectionAs( ldapServer, "cn=User1,ou=system", "secret1Bis" ) )
            { 
                Entry entry = userConnection.lookup( "cn=User1,ou=system" );
            
                assertNotNull( entry );
                assertTrue( entry.containsAttribute( "userPassword" ) );
                Attribute userPassword = entry.get( "userPassword" );
                
                assertNull( PasswordUtil.findAlgorithm( userPassword.getBytes() ) );
            
                assertEquals( 1, userPassword.size() );
                assertEquals( "secret1Bis", userPassword.getString() );
            }
        }
    }


    /**
     * Modify an existing user password while the user is connected:
     * o the userIdentity is not provided
     * o the old password is provided, but it's the wrong one
     * o the new password is new
     * o the entry has a userPassword attribute
     * o the userPassword attribute contains only one value
     * 
     * At the same time, PP and passwordHashing interceptor are disabled
     */
    @Test
    public void testModifyOwnPasswordInvalidOldNew() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        {
            // Create a user
            addUser( adminConnection, "User1", "secret1" );
    
            // Bind as the user
            try ( LdapConnection userConnection = getNetworkConnectionAs( getLdapServer(), 
                "cn=user1,ou=system", "secret1" ) )
            {            
                // Now change the password
                PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
                pwdModifyRequest.setOldPassword( Strings.getBytesUtf8( "secret2" ) );
                pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "secret1Bis" ) );
        
                // Send the request
                PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) userConnection.extended( pwdModifyRequest );
        
                assertEquals( ResultCodeEnum.INVALID_CREDENTIALS, pwdModifyResponse.getLdapResult().getResultCode() );
            }
    
            // Now try to bind with the new password. Should fail
            try ( LdapConnection userConnection = getNetworkConnectionAs( ldapServer, "cn=User1,ou=system", "secret1Bis" ) )
            {
                
                fail();
            }
            catch ( LdapAuthenticationException lae )
            {
                // We are fine
            }
    
            // Rebind with the original password
            try ( LdapConnection userConnection = getNetworkConnectionAs( ldapServer, "cn=User1,ou=system", "secret1" ) )
            {
                Entry entry = userConnection.lookup( "cn=User1,ou=system", "userPassword", "modifyTimestamp" );
        
                assertNotNull( entry );
                assertTrue( entry.containsAttribute( "userPassword" ) );
                Attribute userPassword = entry.get( "userPassword" );
                
                assertNull( PasswordUtil.findAlgorithm( userPassword.getBytes() ) );
        
                assertEquals( 1, userPassword.size() );
                assertEquals( "secret1", userPassword.getString() );
                
                // The entry should not have been modified
                assertFalse( entry.containsAttribute( "modifyTimestamp" ) );
            }
        }
    }


    /**
     * Modify an existing user password while the user is connected:
     * o the userIdentity is not provided
     * o the old password is provided
     * o the new password is not provided 
     * o the entry has a userPassword attribute
     * o the userPassword attribute contains only one value
     * 
     * At the same time, PP and passwordHashing interceptor are disabled
     */
    @Test
    public void testModifyOwnPasswordOldNoNew() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        {
            // Create a user
            addUser( adminConnection, "User1", "secret1" );
        
            // Bind as the user
            try ( LdapConnection userConnection = getNetworkConnectionAs( getLdapServer(), "cn=user1,ou=system", "secret1" ) )
            {
                // Now change the password
                PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
                pwdModifyRequest.setOldPassword( Strings.getBytesUtf8( "secret1" ) );
            
                // Send the request
                PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) userConnection.extended( pwdModifyRequest );
            
                assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, pwdModifyResponse.getLdapResult().getResultCode() );
            }
        
            // Rebind with the original password
            try ( LdapConnection  userConnection = getNetworkConnectionAs( ldapServer, "cn=User1,ou=system", "secret1" ) )
            {
                Entry entry = userConnection.lookup( "cn=User1,ou=system", "userPassword", "modifyTimestamp" );
            
                assertNotNull( entry );
                assertTrue( entry.containsAttribute( "userPassword" ) );
                Attribute userPassword = entry.get( "userPassword" );
                
                assertNull( PasswordUtil.findAlgorithm( userPassword.getBytes() ) );
            
                assertEquals( 1, userPassword.size() );
                assertEquals( "secret1", userPassword.getString() );
                assertFalse( entry.containsAttribute( "modifyTimestamp" ) );
            }
        }
    }


    /**
     * Modify an existing user password while the user is connected:
     * o the userIdentity is not provided
     * o the old password is provided
     * o the new password is already existing
     * o the entry has a userPassword attribute
     * o the userPassword attribute contains only one value
     * 
     * At the same time, PP and passwordHashing interceptor are disabled
     */
    @Test
    public void testModifyOwnPasswordOldNewExists() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        {
            // Create a user
            addUser( adminConnection, "User1", "secret1" );
    
            // Bind as the user
            try ( LdapConnection userConnection = getNetworkConnectionAs( getLdapServer(), "cn=user1,ou=system", "secret1" ) )
            {
                // Now change the password
                PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
                pwdModifyRequest.setOldPassword( Strings.getBytesUtf8( "secret1" ) );
                pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "secret1" ) );
        
                // Send the request
                PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) userConnection.extended( pwdModifyRequest );
        
                assertEquals( ResultCodeEnum.SUCCESS, pwdModifyResponse.getLdapResult().getResultCode() );
            }
    
            // Rebind with the original password
            try ( LdapConnection userConnection = getNetworkConnectionAs( ldapServer, "cn=User1,ou=system", "secret1" ) )
            {
                Entry entry = userConnection.lookup( "cn=User1,ou=system" );
        
                assertNotNull( entry );
                assertTrue( entry.containsAttribute( "userPassword" ) );
                Attribute userPassword = entry.get( "userPassword" );
                
                assertNull( PasswordUtil.findAlgorithm( userPassword.getBytes() ) );
        
                assertEquals( 1, userPassword.size() );
                assertEquals( "secret1", userPassword.getString() );
                assertFalse( entry.containsAttribute( "modifyTimestamp" ) );
            }
        }
    }


    /**
     * Modify an existing user password while the user is connected:
     * o the userIdentity is not provided
     * o the old password is not provided
     * o the new password is not provided
     * o the entry has a userPassword attribute
     * o the userPassword attribute contains only one value
     * 
     * At the same time, PP and passwordHashing interceptor are disabled
     */
    @Test
    public void testModifyOwnPasswordNoOldNoNew() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        {
            // Create a user
            addUser( adminConnection, "User1", "secret1" );

            // Bind as the user
            try ( LdapConnection userConnection = getNetworkConnectionAs( getLdapServer(), "cn=user1,ou=system", "secret1" ) )
            {
                // Now change the password
                PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
        
                // Send the request
                PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) userConnection.extended( pwdModifyRequest );
        
                assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, pwdModifyResponse.getLdapResult().getResultCode() );
            }
    
            // Rebind with the original password
            try ( LdapConnection userConnection = getNetworkConnectionAs( ldapServer, "cn=User1,ou=system", "secret1" ) )
            {
                Entry entry = userConnection.lookup( "cn=User1,ou=system" );
        
                assertNotNull( entry );
                assertTrue( entry.containsAttribute( "userPassword" ) );
                Attribute userPassword = entry.get( "userPassword" );
                
                assertNull( PasswordUtil.findAlgorithm( userPassword.getBytes() ) );
        
                assertEquals( 1, userPassword.size() );
                assertEquals( "secret1", userPassword.getString() );
            }
        }
    }


    //-----------------------------------------------------------------------------------
    // Self password modification with two passwords
    //-----------------------------------------------------------------------------------
    /**
     * Modify an existing user password while the user is connected:
     * o the userIdentity is not provided
     * o the old password is not provided
     * o the new password is not provided
     * o the entry has a userPassword attribute
     * o the userPassword attribute contains two values
     * 
     * At the same time, PP and passwordHashing interceptor are disabled
     */
    @Test
    public void testModifyOwn2PasswordsNoOldNoNew() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        {
            // Create a user
            addUser2Passwords( adminConnection, "User1", "secret1", "other" );

            // Bind as the user
            try ( LdapConnection userConnection = getNetworkConnectionAs( getLdapServer(), "cn=user1,ou=system", "secret1" ) )
            {
                // Now change the password
                PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
        
                // Send the request
                PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) userConnection.extended( pwdModifyRequest );
        
                assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, pwdModifyResponse.getLdapResult().getResultCode() );
            }
    
            // Rebind with the original password
            try ( LdapConnection userConnection = getNetworkConnectionAs( ldapServer, "cn=User1,ou=system", "secret1" ) )
            {
                Entry entry = userConnection.lookup( "cn=User1,ou=system" );
        
                assertNotNull( entry );
                assertTrue( entry.containsAttribute( "userPassword" ) );
                Attribute userPassword = entry.get( "userPassword" );
                
                assertNull( PasswordUtil.findAlgorithm( userPassword.getBytes() ) );
        
                assertEquals( 2, userPassword.size() );
                assertTrue( userPassword.contains( "secret1", "other" ) );
            }
        }
    }

    
    /**
     * Modify an existing user password while the user is connected:
     * o the userIdentity is not provided
     * o the old password is not provided
     * o the new password is new
     * o the entry has a userPassword attribute
     * o the userPassword attribute contains two values
     * 
     * At the end, we will have only one password remaining
     * 
     * At the same time, PP and passwordHashing interceptor are disabled
     */
    @Test
    public void testModifyOwn2PassworsdNoOldNew() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        {
            // Create a user
            addUser2Passwords( adminConnection, "User1", "secret1", "other" );

            // Bind as the user
            Entry user;
            
            try ( LdapConnection userConnection = getNetworkConnectionAs( getLdapServer(), "cn=user1,ou=system", "secret1" ) )
            {
                user = userConnection.lookup( "cn=user1,ou=system", "modifyTimestamp" );
                
                // Now change the password
                PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
                pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "secret1Bis" ) );
        
                // Send the request
                PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) userConnection.extended( pwdModifyRequest );
        
                assertEquals( ResultCodeEnum.SUCCESS, pwdModifyResponse.getLdapResult().getResultCode() );
            }
    
            // Now try to bind with the new password
            try ( LdapConnection userConnection = getNetworkConnectionAs( ldapServer, "cn=User1,ou=system", "secret1Bis" ) )
            {
                Entry entry = userConnection.lookup( "cn=User1,ou=system", "userPassword", "modifyTimestamp" );
        
                assertNotNull( entry );
                assertTrue( entry.containsAttribute( "userPassword" ) );
                Attribute userPassword = entry.get( "userPassword" );
                
                assertNull( PasswordUtil.findAlgorithm( userPassword.getBytes() ) );
        
                assertEquals( 1, userPassword.size() );
                assertEquals( "secret1Bis", userPassword.getString() );
                assertNull( user.get( "modifyTimestamp" ) );
                assertNotNull( entry.get( "modifyTimestamp" ).getString() );
            }
        }
    }


    /**
     * Modify an existing user password while the user is connected:
     * o the userIdentity is not provided
     * o the old password is not provided
     * o the new password exists
     * o the entry has a userPassword attribute
     * o the userPassword attribute contains two values
     * 
     * At the same time, PP and passwordHashing interceptor are disabled
     */
    @Test
    public void testModifyOwn2PasswordsNoOldNewExists() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        {
            // Create a user
            addUser2Passwords( adminConnection, "User1", "secret1", "other" );

            // Bind as the user
            Entry user;
            
            try ( LdapConnection userConnection = getNetworkConnectionAs( getLdapServer(), "cn=user1,ou=system", "secret1" ) )
            {
                user = userConnection.lookup( "cn=user1,ou=system", "modifyTimestamp" );
                
                // Now change the password
                PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
                pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "secret1" ) );
        
                // Send the request
                PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) userConnection.extended( pwdModifyRequest );
        
                assertEquals( ResultCodeEnum.SUCCESS, pwdModifyResponse.getLdapResult().getResultCode() );
            }
    
            // Now try to bind with the new password
            try ( LdapConnection userConnection = getNetworkConnectionAs( ldapServer, "cn=User1,ou=system", "secret1" ) )
            {
                Entry entry = userConnection.lookup( "cn=User1,ou=system", "userPassword", "modifyTimestamp" );
        
                assertNotNull( entry );
                assertTrue( entry.containsAttribute( "userPassword" ) );
                Attribute userPassword = entry.get( "userPassword" );
                
                assertNull( PasswordUtil.findAlgorithm( userPassword.getBytes() ) );
        
                assertEquals( 2, userPassword.size() );
                assertTrue( userPassword.contains( "secret1", "other" ) );
                assertNull( user.get( "modifyTimestamp" ) );
                assertNull( entry.get( "modifyTimestamp" ) );
            }
        }
    }


    /**
     * Modify an existing user password while the user is connected:
     * o the userIdentity is not provided
     * o the old password is provided
     * o the new password is new
     * o the entry has a userPassword attribute
     * o the userPassword attribute contains two values
     * 
     * At the same time, PP and passwordHashing interceptor are disabled
     */
    @Test
    public void testModifyOwn2PasswordsOldNew() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        {
            // Create a user
            addUser2Passwords( adminConnection, "User1", "secret1", "other" );

            // Bind as the user
            try ( LdapConnection userConnection = getNetworkConnectionAs( getLdapServer(), "cn=user1,ou=system", "secret1" ) )
            {
                // Now change the password
                PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
                pwdModifyRequest.setOldPassword( Strings.getBytesUtf8( "secret1" ) );
                pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "secret1Bis" ) );
        
                // Send the request
                PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) userConnection.extended( pwdModifyRequest );
        
                assertEquals( ResultCodeEnum.SUCCESS, pwdModifyResponse.getLdapResult().getResultCode() );
            }
    
            // Now try to bind with the new password
            try ( LdapConnection userConnection = getNetworkConnectionAs( ldapServer, "cn=User1,ou=system", "secret1Bis" ) )
            {
                Entry entry = userConnection.lookup( "cn=User1,ou=system" );
        
                assertNotNull( entry );
                assertTrue( entry.containsAttribute( "userPassword" ) );
                Attribute userPassword = entry.get( "userPassword" );
                
                assertNull( PasswordUtil.findAlgorithm( userPassword.getBytes() ) );
        
                assertEquals( 1, userPassword.size() );
                assertEquals( "secret1Bis", userPassword.getString() );
            }
        }
    }


    /**
     * Modify an existing user password while the user is connected:
     * o the userIdentity is not provided
     * o the old password is provided, but it's the wrong one
     * o the new password is new
     * o the entry has a userPassword attribute
     * o the userPassword attribute contains two values
     * 
     * At the same time, PP and passwordHashing interceptor are disabled
     */
    @Test
    public void testModifyOwn2PasswordsInvalidOldNew() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        {
            // Create a user
            addUser2Passwords( adminConnection, "User1", "secret1", "other" );

            // Bind as the user
            try ( LdapConnection userConnection = getNetworkConnectionAs( getLdapServer(), 
                "cn=user1,ou=system", "secret1" ) )
            {
                // Now change the password
                PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
                pwdModifyRequest.setOldPassword( Strings.getBytesUtf8( "secret2" ) );
                pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "secret1Bis" ) );
        
                // Send the request
                PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) userConnection.extended( pwdModifyRequest );
        
                assertEquals( ResultCodeEnum.INVALID_CREDENTIALS, pwdModifyResponse.getLdapResult().getResultCode() );
            }
    
            // Now try to bind with the new password. Should fail
            try ( LdapConnection userConnection = getNetworkConnectionAs( ldapServer, "cn=User1,ou=system", "secret1Bis" ) )
            {
                fail();
            }
            catch ( LdapAuthenticationException lae )
            {
                // We are fine
            }
    
            // Rebind with the original password
            try ( LdapConnection userConnection = getNetworkConnectionAs( ldapServer, "cn=User1,ou=system", "secret1" ) )
            { 
                Entry entry = userConnection.lookup( "cn=User1,ou=system", "userPassword", "modifyTimestamp" );
        
                assertNotNull( entry );
                assertTrue( entry.containsAttribute( "userPassword" ) );
                Attribute userPassword = entry.get( "userPassword" );
                
                assertNull( PasswordUtil.findAlgorithm( userPassword.getBytes() ) );
        
                assertEquals( 2, userPassword.size() );
                assertTrue( userPassword.contains( "secret1", "other" ) );
                
                // The entry should not have been modified
                assertFalse( entry.containsAttribute( "modifyTimestamp" ) );
            }
        }
    }


    /**
     * Modify an existing user password while the user is connected:
     * o the userIdentity is not provided
     * o the old password is provided
     * o the new password is not provided 
     * o the entry has a userPassword attribute
     * o the userPassword attribute contains two values
     * 
     * At the same time, PP and passwordHashing interceptor are disabled
     */
    @Test
    public void testModifyOwn2PasswordsOldNoNew() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        {
            // Create a user
            addUser2Passwords( adminConnection, "User1", "secret1", "other" );
    
            // Bind as the user
            try ( LdapConnection userConnection = getNetworkConnectionAs( getLdapServer(), "cn=user1,ou=system", "secret1" ) )
            {
                // Now change the password
                PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
                pwdModifyRequest.setOldPassword( Strings.getBytesUtf8( "secret1" ) );
        
                // Send the request
                PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) userConnection.extended( pwdModifyRequest );
        
                assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, pwdModifyResponse.getLdapResult().getResultCode() );
            }
    
            // Rebind with the original password
            try ( LdapConnection userConnection = getNetworkConnectionAs( ldapServer, "cn=User1,ou=system", "secret1" ) )
            { 
                Entry entry = userConnection.lookup( "cn=User1,ou=system", "userPassword", "modifyTimestamp" );
        
                assertNotNull( entry );
                assertTrue( entry.containsAttribute( "userPassword" ) );
                Attribute userPassword = entry.get( "userPassword" );
                
                assertNull( PasswordUtil.findAlgorithm( userPassword.getBytes() ) );
        
                assertEquals( 2, userPassword.size() );
                assertTrue( userPassword.contains( "secret1", "other" ) );
                assertFalse( entry.containsAttribute( "modifyTimestamp" ) );
            }
        }
    }


    /**
     * Modify an existing user password while the user is connected:
     * o the userIdentity is not provided
     * o the old password is provided
     * o the new password is already existing
     * o the entry has a userPassword attribute
     * o the userPassword attribute contains two values
     * 
     * At the same time, PP and passwordHashing interceptor are disabled
     */
    @Test
    public void testModifyOwn2PasswordsOldNewExists() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        {
            // Create a user
            addUser2Passwords( adminConnection, "User1", "secret1", "other" );
    
            // Bind as the user
            try ( LdapConnection userConnection = getNetworkConnectionAs( getLdapServer(), "cn=user1,ou=system", "secret1" ) )
            { 
                // Now change the password
                PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
                pwdModifyRequest.setOldPassword( Strings.getBytesUtf8( "secret1" ) );
                pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "secret1" ) );
        
                // Send the request
                PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) userConnection.extended( pwdModifyRequest );
        
                assertEquals( ResultCodeEnum.SUCCESS, pwdModifyResponse.getLdapResult().getResultCode() );
            }
    
            // Rebind with the original password
            try ( LdapConnection userConnection = getNetworkConnectionAs( ldapServer, "cn=User1,ou=system", "secret1" ) )
            {
                Entry entry = userConnection.lookup( "cn=User1,ou=system" );
        
                assertNotNull( entry );
                assertTrue( entry.containsAttribute( "userPassword" ) );
                Attribute userPassword = entry.get( "userPassword" );
                
                assertNull( PasswordUtil.findAlgorithm( userPassword.getBytes() ) );
        
                assertEquals( 2, userPassword.size() );
                assertTrue( userPassword.contains( "secret1", "other" ) );
                assertFalse( entry.containsAttribute( "modifyTimestamp" ) );
            }
        }
    }
    
    
    //-----------------------------------------------------------------------------------
    // Non connected user
    //-----------------------------------------------------------------------------------
    /**
     * Modify an existing user password while the user is not connected
     */
    @Test
    public void testModifyUserPasswordAnonymous() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        {
            // Create a user
            addUser( adminConnection, "User2", "secret2" );
    
            try ( LdapConnection userConnection = getNetworkConnectionAs( ldapServer, "cn=User2,ou=system", "secret2" ) )
            {
                Entry entry = userConnection.lookup( "cn=User2,ou=system" );
        
                assertNotNull( entry );
            }
    
            // Anonymous Bind
            try ( LdapConnection anonymousConnection = getAnonymousNetworkConnection( getLdapServer() ) )
            { 
                // Now change the password
                PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
                pwdModifyRequest.setUserIdentity( Strings.getBytesUtf8( "cn=User2,ou=system" ) );
                pwdModifyRequest.setOldPassword( Strings.getBytesUtf8( "secret2" ) );
                pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "secret2Bis" ) );
        
                // Send the request
                PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) anonymousConnection.extended( pwdModifyRequest );
        
                assertEquals( ResultCodeEnum.SUCCESS, pwdModifyResponse.getLdapResult().getResultCode() );
            }
    
            // Check that we can now bind using the new credentials
            try ( LdapConnection userConnection = getNetworkConnectionAs( ldapServer, "cn=User2,ou=system", "secret2Bis" ) )
            { 
                Entry entry = userConnection.lookup( "cn=User2,ou=system" );
        
                assertNotNull( entry );
            }
        }
    }
    
    
    /**
     * Test that the server refuse to generate a password
     */
    @Test
    public void testOwnGenPassword() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        {
            // Create a user
            addUser( adminConnection, "User6", "secret6" );
    
            // Modify the user with the user account
            try ( LdapConnection userConnection = getNetworkConnectionAs( ldapServer, "cn=User6,ou=system", "secret6" ) )
            {
                // Now request a new password
                PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
                pwdModifyRequest.setUserIdentity( Strings.getBytesUtf8( "cn=User6,ou=system" ) );
        
                // Send the request
                PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) userConnection.extended( pwdModifyRequest );
        
                // We should not be allowed to do that, as the operation is not yet implemented
                assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, pwdModifyResponse.getLdapResult().getResultCode() );
            }
        }
    }


    //-----------------------------------------------------------------------------------
    // With admin
    //-----------------------------------------------------------------------------------
    /**
     * Modify an existing user password with an admin account
     */
    @Test
    public void testAdminModifyPassword() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        {
            // Create a user
            addUserNoPassword( adminConnection, "User4" );
    
            // Modify the user with the admin account
            Entry entry = adminConnection.lookup( "cn=User4,ou=system" );
    
            assertNotNull( entry );
    
            // Now change the password
            PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
            pwdModifyRequest.setUserIdentity( Strings.getBytesUtf8( "cn=User4,ou=system" ) );
            pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "secret4" ) );
    
            // Send the request
            PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) adminConnection.extended( pwdModifyRequest );
    
            assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, pwdModifyResponse.getLdapResult().getResultCode() );
    
            // Now try to bind with the new password
            try( LdapConnection userConnection = getNetworkConnectionAs( ldapServer, "cn=User4,ou=system", "secret4" ))
            {
                fail();
            }
            catch ( LdapException le )
            {
                // expected
            }
    
            entry = adminConnection.lookup( "cn=User4,ou=system", SchemaConstants.ALL_ATTRIBUTES_ARRAY );
    
            assertNotNull( entry );
            assertFalse( entry.containsAttribute( "userPassword" ) );
            assertFalse( entry.containsAttribute( "modifyTimestamp" ) );
        }
    }


    /**
     * Modify an existing user password with admin
     * o the userIdentity is provided
     * o the old password is not provided
     * o the new password is not provided
     * o the entry has a userPassword attribute
     * o the userPassword attribute contains only one value
     * 
     * At the same time, PP and passwordHashing interceptor are disabled
     */
    @Test
    public void testAdminModifyPasswordNoOldNoNew() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        {
            // Create a user
            addUser( adminConnection, "User1", "secret1" );
    
            // Now change the password
            PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
            pwdModifyRequest.setUserIdentity( Strings.getBytesUtf8( "cn=User1,ou=system" ) );
    
            // Send the request
            PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) adminConnection.extended( pwdModifyRequest );
    
            assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, pwdModifyResponse.getLdapResult().getResultCode() );
    
            Entry entry = adminConnection.lookup( "cn=User1,ou=system", "userPassword", "modifyTimestamp" );
    
            assertNotNull( entry );
            assertTrue( entry.containsAttribute( "userPassword" ) );
            Attribute userPassword = entry.get( "userPassword" );
            
            assertNull( PasswordUtil.findAlgorithm( userPassword.getBytes() ) );
    
            assertEquals( 1, userPassword.size() );
            assertEquals( "secret1", userPassword.getString() );
            assertFalse( entry.containsAttribute( "modifyTimestamp" ) );
        }
    }


    /**
     * Modify an existing user password with admin
     * o the userIdentity is provided
     * o the old password is not provided
     * o the new password is provided
     * o the entry has a userPassword attribute
     * o the userPassword attribute contains only one value
     * 
     * At the same time, PP and passwordHashing interceptor are disabled
     */
    @Test
    public void testAdminModifyPasswordNoOldNew() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        {
            // Create a user
            addUser( adminConnection, "User1", "secret1" );
    
            // Now change the password
            PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
            pwdModifyRequest.setUserIdentity( Strings.getBytesUtf8( "cn=User1,ou=system" ) );
            pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "secret1Bis" ) );
    
            // Send the request
            PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) adminConnection.extended( pwdModifyRequest );
    
            assertEquals( ResultCodeEnum.SUCCESS, pwdModifyResponse.getLdapResult().getResultCode() );
    
            try ( LdapConnection userConnection = getNetworkConnectionAs( getLdapServer(), 
                "cn=user1,ou=system", "secret1Bis" ) )
            {
                Entry entry = userConnection.lookup( "cn=User1,ou=system", "userPassword", "modifyTimestamp" );
            
                assertNotNull( entry );
                assertTrue( entry.containsAttribute( "userPassword" ) );
                Attribute userPassword = entry.get( "userPassword" );
                
                assertNull( PasswordUtil.findAlgorithm( userPassword.getBytes() ) );
            
                assertEquals( 1, userPassword.size() );
                assertEquals( "secret1Bis", userPassword.getString() );
                assertTrue( entry.containsAttribute( "modifyTimestamp" ) );
            }
        }
    }


    /**
     * Modify an existing user with no password with admin
     * o the userIdentity is provided
     * o the old password is not provided
     * o the new password is provided
     * o the entry does not have a userPassword attribute
     * o the userPassword attribute contains only one value
     * 
     * At the same time, PP and passwordHashing interceptor are disabled
     */
    @Test
    public void testAdminAddPasswordNoOldNew() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        {
            // Create a user
            addUserNoPassword( adminConnection, "User1" );
    
            // Now change the password
            PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
            pwdModifyRequest.setUserIdentity( Strings.getBytesUtf8( "cn=User1,ou=system" ) );
            pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "secret1" ) );
    
            // Send the request
            PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) adminConnection.extended( pwdModifyRequest );
    
            assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, pwdModifyResponse.getLdapResult().getResultCode() );
    
            try ( LdapConnection userConnection = getNetworkConnectionAs( getLdapServer(), 
                "cn=user1,ou=system", "secret1" ) )
            {
                fail();
            }
            catch ( LdapException le )
            {
                // expected
            }

            Entry entry = adminConnection.lookup( "cn=User1,ou=system", "userPassword", "modifyTimestamp" );
        
            assertNotNull( entry );
            assertFalse( entry.containsAttribute( "userPassword" ) );
            assertFalse( entry.containsAttribute( "modifyTimestamp" ) );
        }
    }
    

    /**
     * Modify an existing user password with admin
     * o the userIdentity is provided
     * o the old password is provided but is invalid
     * o the new password is provided
     * o the entry has a userPassword attribute
     * o the userPassword attribute contains only one value
     * 
     * At the same time, PP and passwordHashing interceptor are disabled
     */
    @Test
    public void testAdminModifyPasswordInvalidOldNew() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        {
            // Create a user
            addUser( adminConnection, "User1", "secret1" );
        
            // Now change the password
            PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
            pwdModifyRequest.setUserIdentity( Strings.getBytesUtf8( "cn=User1,ou=system" ) );
            pwdModifyRequest.setOldPassword( Strings.getBytesUtf8( "secret2" ) );
            pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "secret2" ) ); 
        
            // Send the request
            PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) adminConnection.extended( pwdModifyRequest );
        
            assertEquals( ResultCodeEnum.INVALID_CREDENTIALS, pwdModifyResponse.getLdapResult().getResultCode() );
        
            Entry entry = adminConnection.lookup( "cn=User1,ou=system", "userPassword", "modifyTimestamp" );
        
            assertNotNull( entry );
            assertTrue( entry.containsAttribute( "userPassword" ) );
            Attribute userPassword = entry.get( "userPassword" );
            
            assertNull( PasswordUtil.findAlgorithm( userPassword.getBytes() ) );
        
            assertEquals( 1, userPassword.size() );
            assertEquals( "secret1", userPassword.getString() );
            assertFalse( entry.containsAttribute( "modifyTimestamp" ) );
        }
    }


    /**
     * Modify an existing user password with admin
     * o the userIdentity is provided
     * o the old password is not provided
     * o the new password is provided but already exist
     * o the entry has a userPassword attribute
     * o the userPassword attribute contains only one value
     * 
     * At the same time, PP and passwordHashing interceptor are disabled
     */
    @Test
    public void testAdminModifyPasswordNoOldNewExists() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        {
            // Create a user
            addUser( adminConnection, "User1", "secret1" );
    
            // Now change the password
            PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
            pwdModifyRequest.setUserIdentity( Strings.getBytesUtf8( "cn=User1,ou=system" ) );
            pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "secret1" ) );
    
            // Send the request
            PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) adminConnection.extended( pwdModifyRequest );
    
            assertEquals( ResultCodeEnum.SUCCESS, pwdModifyResponse.getLdapResult().getResultCode() );
    
            try ( LdapConnection userConnection = getNetworkConnectionAs( getLdapServer(), 
                "cn=user1,ou=system", "secret1" ) )
            {
                Entry entry = userConnection.lookup( "cn=User1,ou=system", "userPassword", "modifyTimestamp" );
            
                assertNotNull( entry );
                assertTrue( entry.containsAttribute( "userPassword" ) );
                Attribute userPassword = entry.get( "userPassword" );
                
                assertNull( PasswordUtil.findAlgorithm( userPassword.getBytes() ) );
            
                assertEquals( 1, userPassword.size() );
                assertEquals( "secret1", userPassword.getString() );
                assertFalse( entry.containsAttribute( "modifyTimestamp" ) );
            }
        }
    }


    /**
     * Modify an existing user password with admin
     * o the userIdentity is provided
     * o the old password is provided
     * o the new password is provided
     * o the entry has a userPassword attribute
     * o the userPassword attribute contains only one value
     * 
     * At the same time, PP and passwordHashing interceptor are disable
     */
    @Test
    public void testAdminModifyPasswordOldNew() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        {
            // Create a user
            addUser( adminConnection, "User1", "secret1" );
    
            // Now change the password
            PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
            pwdModifyRequest.setUserIdentity( Strings.getBytesUtf8( "cn=User1,ou=system" ) );
            pwdModifyRequest.setOldPassword( Strings.getBytesUtf8( "secret1" ) );
            pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "secret1Bis" ) );
    
            // Send the request
            PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) adminConnection.extended( pwdModifyRequest );
    
            assertEquals( ResultCodeEnum.SUCCESS, pwdModifyResponse.getLdapResult().getResultCode() );
    
            try ( LdapConnection userConnection = getNetworkConnectionAs( getLdapServer(), 
                "cn=user1,ou=system", "secret1Bis" ) )
            {
                Entry entry = userConnection.lookup( "cn=User1,ou=system", SchemaConstants.ALL_ATTRIBUTES_ARRAY );
            
                assertNotNull( entry );
                assertTrue( entry.containsAttribute( "userPassword" ) );
                Attribute userPassword = entry.get( "userPassword" );
                
                assertNull( PasswordUtil.findAlgorithm( userPassword.getBytes() ) );
            
                assertEquals( 1, userPassword.size() );
                assertEquals( "secret1Bis", userPassword.getString() );
                assertTrue( entry.containsAttribute( "modifyTimestamp" ) );
            }
        }
    }


    /**
     * Modify an existing user password with admin
     * o the userIdentity is provided
     * o the old password is provided
     * o the new password is not provided
     * o the entry has a userPassword attribute
     * o the userPassword attribute contains only one value
     * 
     * At the same time, PP and passwordHashing interceptor are disable
     */
    @Test
    public void testAdminModifyPasswordOldNoNew() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        {
            // Create a user
            addUser( adminConnection, "User1", "secret1" );
    
            // Now change the password
            PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
            pwdModifyRequest.setUserIdentity( Strings.getBytesUtf8( "cn=User1,ou=system" ) );
            pwdModifyRequest.setOldPassword( Strings.getBytesUtf8( "secret1" ) );
    
            // Send the request
            PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) adminConnection.extended( pwdModifyRequest );
    
            assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, pwdModifyResponse.getLdapResult().getResultCode() );
    
            try ( LdapConnection userConnection = getNetworkConnectionAs( getLdapServer(), 
                "cn=user1,ou=system", "secret1" ) )
            {
                Entry entry = userConnection.lookup( "cn=User1,ou=system", SchemaConstants.ALL_ATTRIBUTES_ARRAY );
            
                assertNotNull( entry );
                assertTrue( entry.containsAttribute( "userPassword" ) );
                Attribute userPassword = entry.get( "userPassword" );
                
                assertNull( PasswordUtil.findAlgorithm( userPassword.getBytes() ) );
            
                assertEquals( 1, userPassword.size() );
                assertEquals( "secret1", userPassword.getString() );
                assertFalse( entry.containsAttribute( "modifyTimestamp" ) );
            }
        }
    }


    /**
     * Modify an existing user password with admin
     * o the userIdentity is provided
     * o the old password is provided
     * o the new password is provided
     * o the entry has a userPassword attribute
     * o the userPassword attribute contains only one value
     * 
     * At the same time, PP and passwordHashing interceptor are disabled
     */
    @Test
    public void testAdminModifyPasswordOldNewExists() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        {
            // Create a user
            addUser( adminConnection, "User1", "secret1" );
    
            // Now change the password
            PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
            pwdModifyRequest.setUserIdentity( Strings.getBytesUtf8( "cn=User1,ou=system" ) );
            pwdModifyRequest.setOldPassword( Strings.getBytesUtf8( "secret1" ) );
            pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "secret1" ) );
    
            // Send the request
            PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) adminConnection.extended( pwdModifyRequest );
    
            assertEquals( ResultCodeEnum.SUCCESS, pwdModifyResponse.getLdapResult().getResultCode() );
    
            try ( LdapConnection userConnection = getNetworkConnectionAs( getLdapServer(), 
                "cn=user1,ou=system", "secret1" ) )
            {
                Entry entry = userConnection.lookup( "cn=User1,ou=system", "userPassword", "modifyTimestamp" );
            
                assertNotNull( entry );
                assertTrue( entry.containsAttribute( "userPassword" ) );
                Attribute userPassword = entry.get( "userPassword" );
                
                assertNull( PasswordUtil.findAlgorithm( userPassword.getBytes() ) );
            
                assertEquals( 1, userPassword.size() );
                assertEquals( "secret1", userPassword.getString() );
                assertFalse( entry.containsAttribute( "modifyTimestamp" ) );
            }
        }
    }
    
    
    //-----------------------------------------------------------------------------------
    // Admin password modification with two passwords
    //-----------------------------------------------------------------------------------
    /**
     * Modify an existing user password with an admin account:
     * o the userIdentity is provided
     * o the old password is not provided
     * o the new password is not provided
     * o the entry has a userPassword attribute
     * o the userPassword attribute contains two values
     * 
     * At the same time, PP and passwordHashing interceptor are disabled
     */
    @Test
    public void testAdminPasswordModify2PasswordsNoOldNoNew() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        {
            // Create a user
            addUser2Passwords( adminConnection, "User1", "secret1", "other" );
    
            // Now change the password
            PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
            pwdModifyRequest.setUserIdentity( Strings.getBytesUtf8( "cn=User1,ou=system" ) );
    
            // Send the request
            PasswordModifyResponse pwdModifyResponse = 
                    ( PasswordModifyResponse ) adminConnection.extended( pwdModifyRequest );
    
            assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, pwdModifyResponse.getLdapResult().getResultCode() );
    
            // Rebind with the original password
            try ( LdapConnection userConnection = getNetworkConnectionAs( ldapServer, "cn=User1,ou=system", "secret1" ) )
            {
                Entry entry = userConnection.lookup( "cn=User1,ou=system" );
        
                assertNotNull( entry );
                assertTrue( entry.containsAttribute( "userPassword" ) );
                Attribute userPassword = entry.get( "userPassword" );
                
                assertNull( PasswordUtil.findAlgorithm( userPassword.getBytes() ) );
        
                assertEquals( 2, userPassword.size() );
                assertTrue( userPassword.contains( "secret1", "other" ) );
            }
        }
    }

    
    /**
     * Modify an existing user password with an admin account:
     * o the userIdentity is provided
     * o the old password is not provided
     * o the new password is new
     * o the entry has a userPassword attribute
     * o the userPassword attribute contains two values
     * 
     * At the end, we will have only one password remaining
     * 
     * At the same time, PP and passwordHashing interceptor are disabled
     */
    @Test
    public void testAdminPasswordModify2PassworsdNoOldNew() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        {
            // Create a user
            addUser2Passwords( adminConnection, "User1", "secret1", "other" );

            Entry user = adminConnection.lookup( "cn=User1,ou=system", "userPassword", "modifyTimestamp" );

            // Now change the password
            PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
            pwdModifyRequest.setUserIdentity( Strings.getBytesUtf8( "cn=User1,ou=system" ) );
            pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "secret1Bis" ) );

            // Send the request
            PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) adminConnection.extended( pwdModifyRequest );
    
            assertEquals( ResultCodeEnum.SUCCESS, pwdModifyResponse.getLdapResult().getResultCode() );
    
            // Now try to bind with the new password
            try ( LdapConnection userConnection = getNetworkConnectionAs( ldapServer, "cn=User1,ou=system", "secret1Bis" ) )
            {
                Entry entry = userConnection.lookup( "cn=User1,ou=system", "userPassword", "modifyTimestamp" );
        
                assertNotNull( entry );
                assertTrue( entry.containsAttribute( "userPassword" ) );
                Attribute userPassword = entry.get( "userPassword" );
                
                assertNull( PasswordUtil.findAlgorithm( userPassword.getBytes() ) );
        
                assertEquals( 1, userPassword.size() );
                assertEquals( "secret1Bis", userPassword.getString() );
                assertFalse( user.containsAttribute( "modifyTimestamp" ) );
                assertNotNull( entry.get( "modifyTimestamp" ).getString() );
            } 
        }
    }


    /**
     * Modify an existing user password with an admin account:
     * o the userIdentity is provided
     * o the old password is not provided
     * o the new password exists
     * o the entry has a userPassword attribute
     * o the userPassword attribute contains two values
     * 
     * At the same time, PP and passwordHashing interceptor are disabled
     */
    @Test
    public void testAdminPasswordModify2PasswordsNoOldNewExists() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        {
            // Create a user
            addUser2Passwords( adminConnection, "User1", "secret1", "other" );
    
            Entry user = adminConnection.lookup( "cn=user1,ou=system", "modifyTimestamp" );
            
            // Now change the password
            PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
            pwdModifyRequest.setUserIdentity( Strings.getBytesUtf8( "cn=User1,ou=system" ) );
            pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "secret1" ) );
    
            // Send the request
            PasswordModifyResponse pwdModifyResponse = 
                    ( PasswordModifyResponse ) adminConnection.extended( pwdModifyRequest );
    
            assertEquals( ResultCodeEnum.SUCCESS, pwdModifyResponse.getLdapResult().getResultCode() );
    
            // Now try to bind with the new password
            try ( LdapConnection userConnection = getNetworkConnectionAs( ldapServer, "cn=User1,ou=system", "secret1" ) )
            {
                Entry entry = userConnection.lookup( "cn=User1,ou=system", "userPassword", "modifyTimestamp" );
        
                assertNotNull( entry );
                assertTrue( entry.containsAttribute( "userPassword" ) );
                Attribute userPassword = entry.get( "userPassword" );
                
                assertNull( PasswordUtil.findAlgorithm( userPassword.getBytes() ) );
        
                assertEquals( 2, userPassword.size() );
                assertTrue( userPassword.contains( "secret1", "other" ) );
                assertNull( user.get( "modifyTimestamp" ) );
                assertNull( entry.get( "modifyTimestamp" ) );
            }                
        }
    }


    /**
     * Modify an existing user password with an admin account:
     * o the userIdentity is provided
     * o the old password is provided
     * o the new password is new
     * o the entry has a userPassword attribute
     * o the userPassword attribute contains two values
     * 
     * At the same time, PP and passwordHashing interceptor are disabled
     */
    @Test
    public void testAdminPasswordModify2PasswordsOldNew() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        {
            // Create a user
            addUser2Passwords( adminConnection, "User1", "secret1", "other" );
    
            // Now change the password
            PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
            pwdModifyRequest.setUserIdentity( Strings.getBytesUtf8( "cn=User1,ou=system" ) );
            pwdModifyRequest.setOldPassword( Strings.getBytesUtf8( "secret1" ) );
            pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "secret1Bis" ) );
    
            // Send the request
            PasswordModifyResponse pwdModifyResponse = 
                    ( PasswordModifyResponse ) adminConnection.extended( pwdModifyRequest );

            assertEquals( ResultCodeEnum.SUCCESS, pwdModifyResponse.getLdapResult().getResultCode() );
    
            // Now try to bind with the new password
            try ( LdapConnection userConnection = getNetworkConnectionAs( ldapServer, "cn=User1,ou=system", "secret1Bis" ) )
            {
                Entry entry = userConnection.lookup( "cn=User1,ou=system", "userPassword", "modifyTimestamp" );
        
                assertNotNull( entry );
                assertTrue( entry.containsAttribute( "userPassword" ) );
                Attribute userPassword = entry.get( "userPassword" );
                
                assertNull( PasswordUtil.findAlgorithm( userPassword.getBytes() ) );
        
                assertEquals( 1, userPassword.size() );
                assertEquals( "secret1Bis", userPassword.getString() );
                assertTrue( entry.containsAttribute( "modifyTimestamp" ) );
            }
        }
    }


    /**
     * Modify an existing user password with an admin account:
     * o the userIdentity is provided
     * o the old password is provided, but it's the wrong one
     * o the new password is new
     * o the entry has a userPassword attribute
     * o the userPassword attribute contains two values
     * 
     * At the same time, PP and passwordHashing interceptor are disabled
     */
    @Test
    public void testAdminPasswordModify2PasswordsInvalidOldNew() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        {
            // Create a user
            addUser2Passwords( adminConnection, "User1", "secret1", "other" );
    
            // Now change the password
            PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
            pwdModifyRequest.setUserIdentity( Strings.getBytesUtf8( "cn=User1,ou=system" ) );
            pwdModifyRequest.setOldPassword( Strings.getBytesUtf8( "secret2" ) );
            pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "secret1Bis" ) );
    
            // Send the request
            PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) adminConnection.extended( pwdModifyRequest );
    
            assertEquals( ResultCodeEnum.INVALID_CREDENTIALS, pwdModifyResponse.getLdapResult().getResultCode() );
    
            // Now try to bind with the new password. Should fail
            try
            {
                try ( LdapConnection userConnection = getNetworkConnectionAs( ldapServer, "cn=User1,ou=system", "secret1Bis" ) )
                {
                    
                }
                fail();
            }
            catch ( LdapAuthenticationException lae )
            {
                // We are fine
            }
    
            // Rebind with the original password
            try ( LdapConnection userConnection = getNetworkConnectionAs( ldapServer, "cn=User1,ou=system", "secret1" ) )
            {
                Entry entry = userConnection.lookup( "cn=User1,ou=system", "userPassword", "modifyTimestamp" );
        
                assertNotNull( entry );
                assertTrue( entry.containsAttribute( "userPassword" ) );
                Attribute userPassword = entry.get( "userPassword" );
                
                assertNull( PasswordUtil.findAlgorithm( userPassword.getBytes() ) );
        
                assertEquals( 2, userPassword.size() );
                assertTrue( userPassword.contains( "secret1", "other" ) );
                
                // The entry should not have been modified
                assertFalse( entry.containsAttribute( "modifyTimestamp" ) );
            }
        }
    }


    /**
     * Modify an existing user password with an admin account:
     * o the userIdentity is provided
     * o the old password is provided
     * o the new password is not provided 
     * o the entry has a userPassword attribute
     * o the userPassword attribute contains two values
     * 
     * At the same time, PP and passwordHashing interceptor are disabled
     */
    @Test
    public void testAdminPasswordModify2PasswordsOldNoNew() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        {
            // Create a user
            addUser2Passwords( adminConnection, "User1", "secret1", "other" );
    
            // Now change the password
            PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
            pwdModifyRequest.setUserIdentity( Strings.getBytesUtf8( "cn=User1,ou=system" ) );
            pwdModifyRequest.setOldPassword( Strings.getBytesUtf8( "secret1" ) );
    
            // Send the request
            PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) adminConnection.extended( pwdModifyRequest );
    
            assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, pwdModifyResponse.getLdapResult().getResultCode() );
    
            // Rebind with the original password
            try ( LdapConnection userConnection = getNetworkConnectionAs( ldapServer, "cn=User1,ou=system", "secret1" ) )
            {
                Entry entry = userConnection.lookup( "cn=User1,ou=system", "userPassword", "modifyTimestamp" );
        
                assertNotNull( entry );
                assertTrue( entry.containsAttribute( "userPassword" ) );
                Attribute userPassword = entry.get( "userPassword" );
                
                assertNull( PasswordUtil.findAlgorithm( userPassword.getBytes() ) );
        
                assertEquals( 2, userPassword.size() );
                assertTrue( userPassword.contains( "secret1", "other" ) );
                assertFalse( entry.containsAttribute( "modifyTimestamp" ) );
            }        
        }
    }


    /**
     * Modify an existing user password with an admin account:
     * o the userIdentity is provided
     * o the old password is provided
     * o the new password is already existing
     * o the entry has a userPassword attribute
     * o the userPassword attribute contains two values
     * 
     * At the same time, PP and passwordHashing interceptor are disabled
     */
    @Test
    public void testAdminPasswordModify2PasswordsOldNewExists() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        {
            // Create a user
            addUser2Passwords( adminConnection, "User1", "secret1", "other" );
    
            // Now change the password
            PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
            pwdModifyRequest.setUserIdentity( Strings.getBytesUtf8( "cn=User1,ou=system" ) );
            pwdModifyRequest.setOldPassword( Strings.getBytesUtf8( "secret1" ) );
            pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "other" ) );
    
            // Send the request
            PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) adminConnection.extended( pwdModifyRequest );
    
            assertEquals( ResultCodeEnum.SUCCESS, pwdModifyResponse.getLdapResult().getResultCode() );
    
            // Rebind with the original password
            try ( LdapConnection userConnection = getNetworkConnectionAs( ldapServer, "cn=User1,ou=system", "secret1" ) )
            {
                Entry entry = userConnection.lookup( "cn=User1,ou=system" );
        
                assertNotNull( entry );
                assertTrue( entry.containsAttribute( "userPassword" ) );
                Attribute userPassword = entry.get( "userPassword" );
                
                assertNull( PasswordUtil.findAlgorithm( userPassword.getBytes() ) );
        
                assertEquals( 2, userPassword.size() );
                assertTrue( userPassword.contains( "secret1", "other" ) );
                assertFalse( entry.containsAttribute( "modifyTimestamp" ) );
            }
        }
    }


    /**
     * Modify an existing user password with an admin account
     */
    @Test
    public void testAdminModifyMultiplePassword() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        { 
            // Create a user
            addUser2Passwords( adminConnection, "User5", "secret51", "secret52" );
    
            // Modify the user with the admin account
            PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
            pwdModifyRequest.setUserIdentity( Strings.getBytesUtf8( "cn=User5,ou=system" ) );
            pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "secret5Bis" ) );
    
            // Send the request
            PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) adminConnection.extended( pwdModifyRequest );
    
            assertEquals( ResultCodeEnum.SUCCESS, pwdModifyResponse.getLdapResult().getResultCode() );
    
            // Now try to bind with the new password
            try ( LdapConnection userConnection = getNetworkConnectionAs( ldapServer, "cn=User5,ou=system", "secret5Bis" ) )
            {
                Entry entry = userConnection.lookup( "cn=User5,ou=system" );
        
                assertNotNull( entry );
        
                userConnection.close();
            }
        }
    }


    /**
     * Modify an existing user password with a bad account
     */
    @Test
    public void testAdminModifyPasswordBadUser() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        {        
            addUser( adminConnection, "User5", "secret5" );
    
            // Modify the user with the admin account
    
            // Now change the password
            PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
            pwdModifyRequest.setUserIdentity( Strings.getBytesUtf8( "cn=baduser,ou=system" ) );
            pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "secret5Bis" ) );
    
            // Send the request
            PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) adminConnection.extended( pwdModifyRequest );
    
            assertEquals( ResultCodeEnum.NO_SUCH_OBJECT, pwdModifyResponse.getLdapResult().getResultCode() );
        }
    }


    /**
     * Test that the server refuse to generate a password
     */
    @Test
    public void testAdminGenPassword() throws Exception
    {
        try ( LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() ) )
        {
            addUser( adminConnection, "User6", "secret6" );
    
            // Modify the user with the admin account
    
            // Now request a new password
            PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
            pwdModifyRequest.setUserIdentity( Strings.getBytesUtf8( "cn=User6,ou=system" ) );
    
            // Send the request
            PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) adminConnection.extended( pwdModifyRequest );
    
            // We should not be allowed to do that, as the operation is not yet implemented
            assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, pwdModifyResponse.getLdapResult().getResultCode() );
        }
    }
}
