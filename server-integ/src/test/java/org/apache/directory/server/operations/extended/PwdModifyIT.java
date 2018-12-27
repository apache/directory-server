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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.apache.directory.api.ldap.codec.api.LdapApiServiceFactory;
import org.apache.directory.api.ldap.extras.controls.ppolicy.PasswordPolicyErrorEnum;
import org.apache.directory.api.ldap.extras.controls.ppolicy.PasswordPolicyResponse;
import org.apache.directory.api.ldap.extras.controls.ppolicy.PasswordPolicyResponseImpl;
import org.apache.directory.api.ldap.extras.extended.pwdModify.PasswordModifyRequest;
import org.apache.directory.api.ldap.extras.extended.pwdModify.PasswordModifyRequestImpl;
import org.apache.directory.api.ldap.extras.extended.pwdModify.PasswordModifyResponse;
import org.apache.directory.api.ldap.model.constants.LdapSecurityConstants;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapAuthenticationException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.AddRequest;
import org.apache.directory.api.ldap.model.message.AddRequestImpl;
import org.apache.directory.api.ldap.model.message.AddResponse;
import org.apache.directory.api.ldap.model.message.Control;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.password.PasswordUtil;
import org.apache.directory.api.util.Strings;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.api.InterceptorEnum;
import org.apache.directory.server.core.api.authn.ppolicy.CheckQualityEnum;
import org.apache.directory.server.core.api.authn.ppolicy.PasswordPolicyConfiguration;
import org.apache.directory.server.core.authn.AuthenticationInterceptor;
import org.apache.directory.server.core.authn.ppolicy.PpolicyConfigContainer;
import org.apache.directory.server.core.hash.Sha512PasswordHashingInterceptor;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.ldap.handlers.extended.PwdModifyHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test the PwdModify extended operation
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
//disable changelog, for more info see DIRSERVER-1528
@CreateDS(enableChangeLog = false, name = "PasswordPolicyTest", additionalInterceptors =
    { Sha512PasswordHashingInterceptor.class })
public class PwdModifyIT extends AbstractLdapTestUnit
{
    private static final PasswordPolicyResponse PP_REQ_CTRL = new PasswordPolicyResponseImpl();

    /** The passwordPolicy configuration */
    private PasswordPolicyConfiguration policyConfig;


    /**
     * Get the PasswordPolicy control from a response
     */
    private PasswordPolicyResponse getPwdRespCtrl( Response resp ) throws Exception
    {
        Control control = resp.getControls().get( PP_REQ_CTRL.getOid() );

        if ( control == null )
        {
            return null;
        }

        return ( PasswordPolicyResponse ) control;
    }


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
        addRequest.addControl( PP_REQ_CTRL );

        AddResponse addResp = adminConnection.add( addRequest );
        assertEquals( ResultCodeEnum.SUCCESS, addResp.getLdapResult().getResultCode() );
        PasswordPolicyResponse respCtrl = getPwdRespCtrl( addResp );
        assertNull( respCtrl );
    }


    /**
     * Check that we can bind N times with a user/password
     */
    private void checkBind( LdapConnection connection, Dn userDn, String password, int nbIterations,
        String expectedMessage ) throws Exception
    {
        for ( int i = 0; i < nbIterations; i++ )
        {
            try
            {
                connection.bind( userDn, password );
            }
            catch ( LdapAuthenticationException le )
            {
                assertEquals( expectedMessage, le.getMessage() );
            }
        }
    }


    private void safeCloseConnections( LdapConnection... connections )
    {
        for ( LdapConnection connection : connections )
        {
            if ( connection != null )
            {
                try
                {
                    connection.close();
                }
                catch ( Exception e )
                {
                    // may wanna log a message or something
                }
            }
        }
    }


    /**
     * Set a default PaswordPolicy configuration
     */
    @Before
    public void setPwdPolicy() throws LdapException
    {
        policyConfig = new PasswordPolicyConfiguration();

        policyConfig.setPwdMaxAge( 110 );
        policyConfig.setPwdFailureCountInterval( 30 );
        policyConfig.setPwdMaxFailure( 3 );
        policyConfig.setPwdLockout( true );
        policyConfig.setPwdLockoutDuration( 0 );
        policyConfig.setPwdMinLength( 5 );
        policyConfig.setPwdInHistory( 5 );
        policyConfig.setPwdExpireWarning( 600 );
        policyConfig.setPwdGraceAuthNLimit( 5 );
        policyConfig.setPwdCheckQuality( CheckQualityEnum.CHECK_REJECT ); // DO NOT allow the password if its quality can't be checked

        PpolicyConfigContainer policyContainer = new PpolicyConfigContainer();
        Dn defaultPolicyDn = new Dn( ldapServer.getDirectoryService().getSchemaManager(), "cn=default" );
        policyContainer.addPolicy( defaultPolicyDn, policyConfig );
        policyContainer.setDefaultPolicyDn( defaultPolicyDn );
        
        AuthenticationInterceptor authenticationInterceptor = ( AuthenticationInterceptor ) getService()
            .getInterceptor( InterceptorEnum.AUTHENTICATION_INTERCEPTOR.getName() );

        authenticationInterceptor.setPwdPolicies( policyContainer );
    }


    /**
     * Modify an existing user password while the user is connected
     */
    @Test
    public void testModifyOwnPasswordConnected() throws Exception
    {
        LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() );

        addUser( adminConnection, "User1", "secret1" );

        // Bind as the user
        LdapConnection userConnection = getNetworkConnectionAs( getLdapServer(), "cn=user1,ou=system", "secret1" );
        userConnection.setTimeOut( 0L );

        // Now change the password
        PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
        pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "secret1Bis" ) );

        // Send the request
        PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) userConnection.extended( pwdModifyRequest );

        assertEquals( ResultCodeEnum.SUCCESS, pwdModifyResponse.getLdapResult().getResultCode() );

        // Now try to bind with the new password
        userConnection = getNetworkConnectionAs( ldapServer, "cn=User1,ou=system", "secret1Bis" );

        Entry entry = userConnection.lookup( "cn=User1,ou=system" );

        assertNotNull( entry );

        userConnection.close();
        adminConnection.close();
    }


    /**
     * Modify an existing user password while the user is not connected
     */
    @Test
    public void testModifyUserPasswordAnonymous() throws Exception
    {
        LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() );

        addUser( adminConnection, "User2", "secret2" );

        LdapConnection userConnection = getNetworkConnectionAs( ldapServer, "cn=User2,ou=system", "secret2" );

        Entry entry = userConnection.lookup( "cn=User2,ou=system" );

        assertNotNull( entry );

        userConnection.close();

        // Bind as the user
        LdapConnection anonymousConnection = getAnonymousNetworkConnection( getLdapServer() );
        anonymousConnection.setTimeOut( 0L );

        // Now change the password
        PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
        pwdModifyRequest.setUserIdentity( Strings.getBytesUtf8( "cn=User2,ou=system" ) );
        pwdModifyRequest.setOldPassword( Strings.getBytesUtf8( "secret2" ) );
        pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "secret2Bis" ) );

        // Send the request
        PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) anonymousConnection.extended( pwdModifyRequest );

        assertEquals( ResultCodeEnum.SUCCESS, pwdModifyResponse.getLdapResult().getResultCode() );

        // Check that we can now bind using the new credentials
        userConnection = getNetworkConnectionAs( ldapServer, "cn=User2,ou=system", "secret2Bis" );

        entry = userConnection.lookup( "cn=User2,ou=system" );

        assertNotNull( entry );

        userConnection.close();
        anonymousConnection.close();
        adminConnection.close();
    }


    /**
     * Modify an existing user password while the user is not connected, when
     * the PasswordPolicy is activated
     */
    @Test
    public void testModifyUserPasswordAnonymousPPActivated() throws Exception
    {
        policyConfig.setPwdCheckQuality( CheckQualityEnum.CHECK_ACCEPT ); // allow the password if its quality can't be checked
        LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() );

        addUser( adminConnection, "User3", "secret3" );
        Dn userDn = new Dn( "cn=User3,ou=system" );

        LdapConnection userConnection = getNetworkConnectionAs( ldapServer, "cn=User3,ou=system", "secret3" );

        Entry entry = userConnection.lookup( "cn=User3,ou=system" );

        assertNotNull( entry );

        userConnection.close();

        // almost lock the user now
        checkBind( userConnection, userDn, "badPassword", 2,
            "INVALID_CREDENTIALS: Bind failed: ERR_229 Cannot authenticate user cn=User3,ou=system" );

        // Bind as the user
        LdapConnection anonymousConnection = getAnonymousNetworkConnection( getLdapServer() );
        anonymousConnection.setTimeOut( 0L );

        // Now change the password
        PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
        pwdModifyRequest.setUserIdentity( Strings.getBytesUtf8( "cn=User3,ou=system" ) );
        pwdModifyRequest.setOldPassword( Strings.getBytesUtf8( "secret3" ) );
        pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "secret3Bis" ) );

        // Send the request
        PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) anonymousConnection.extended( pwdModifyRequest );

        assertEquals( ResultCodeEnum.SUCCESS, pwdModifyResponse.getLdapResult().getResultCode() );

        // Check that we can now bind using the new credentials
        userConnection = getNetworkConnectionAs( ldapServer, "cn=User3,ou=system", "secret3Bis" );

        entry = userConnection.lookup( "cn=User3,ou=system" );

        assertNotNull( entry );

        // almost lock the user now, the count should be reset
        checkBind( userConnection, userDn, "badPassword", 2,
            "INVALID_CREDENTIALS: Bind failed: ERR_229 Cannot authenticate user cn=User3,ou=system" );

        userConnection.close();
        anonymousConnection.close();
        adminConnection.close();
    }


    /**
     * Modify an existing user password with an admin account
     */
    @Test
    public void testAdminModifyPassword() throws Exception
    {
        LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() );

        addUser( adminConnection, "User4", "secret4" );

        // Modify the user with the admin account

        // Now change the password
        PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
        pwdModifyRequest.setUserIdentity( Strings.getBytesUtf8( "cn=User4,ou=system" ) );
        pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "secret4Bis" ) );

        // Send the request
        PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) adminConnection.extended( pwdModifyRequest );

        assertEquals( ResultCodeEnum.SUCCESS, pwdModifyResponse.getLdapResult().getResultCode() );

        // Now try to bind with the new password
        LdapConnection userConnection = getNetworkConnectionAs( ldapServer, "cn=User4,ou=system", "secret4Bis" );

        Entry entry = userConnection.lookup( "cn=User4,ou=system" );

        assertNotNull( entry );

        userConnection.close();
        adminConnection.close();
    }


    /**
     * Modify an existing user password with a bad account
     */
    @Test
    public void testAdminModifyPasswordBadUser() throws Exception
    {
        LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() );

        addUser( adminConnection, "User5", "secret5" );

        // Modify the user with the admin account

        // Now change the password
        PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
        pwdModifyRequest.setUserIdentity( Strings.getBytesUtf8( "cn=baduser,ou=system" ) );
        pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "secret5Bis" ) );

        // Send the request
        PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) adminConnection.extended( pwdModifyRequest );

        assertEquals( ResultCodeEnum.NO_SUCH_OBJECT, pwdModifyResponse.getLdapResult().getResultCode() );
        assertEquals( "Cannot find an entry for DN cn=baduser,ou=system", pwdModifyResponse.getLdapResult()
            .getDiagnosticMessage() );

        adminConnection.close();
    }


    /**
     * Test that the server generates a new password when required
     */
    @Test
    public void testAdminGenPassword() throws Exception
    {
        LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() );

        addUser( adminConnection, "User6", "secret6" );

        // Modify the user with the admin account

        // Now request a new password
        PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
        pwdModifyRequest.setUserIdentity( Strings.getBytesUtf8( "cn=User6,ou=system" ) );

        // Send the request
        PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) adminConnection.extended( pwdModifyRequest );

        // We should not be allowed to do that, as the operation is not yet implemented
        assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, pwdModifyResponse.getLdapResult().getResultCode() );

        adminConnection.close();
    }


    /**
     * Modify an existing user password with an admin account
     */
    @Test
    public void testModifyPasswordTooSoon() throws Exception
    {
        LdapConnection adminConnection = null;
        LdapConnection userConnection = null;
        int minAge = policyConfig.getPwdMinAge();
        try
        {
            policyConfig.setPwdMinAge( 1000 );

            adminConnection = getAdminNetworkConnection( getLdapServer() );
            addUser( adminConnection, "User7", "secret7" );
            Entry userEntry = adminConnection.lookup( "cn=User7,ou=system", "*", "+" );
            Attribute attribute = userEntry.get( "pwdHistory" );
            assertEquals( 1, attribute.size() );

            PasswordModifyRequest pwdModifyRequest = null;
            PasswordModifyResponse pwdModifyResponse = null;

            // Fail modify user with user account
            try
            {
                userConnection = getNetworkConnectionAs( getLdapServer(), "cn=User7,ou=system", "secret7" );
                pwdModifyRequest = new PasswordModifyRequestImpl();
                pwdModifyRequest.setUserIdentity( Strings.getBytesUtf8( "cn=User7,ou=system" ) );
                pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "secret4Bis" ) );
                pwdModifyResponse = ( PasswordModifyResponse ) userConnection.extended( pwdModifyRequest );
                assertNotEquals( ResultCodeEnum.SUCCESS, pwdModifyResponse.getLdapResult().getResultCode() );
            }
            finally
            {
                safeCloseConnections( userConnection );
            }

            // Modify the user with the admin account
            pwdModifyRequest = new PasswordModifyRequestImpl();
            pwdModifyRequest.setUserIdentity( Strings.getBytesUtf8( "cn=User7,ou=system" ) );
            pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "secret4Bis" ) );
            pwdModifyResponse = ( PasswordModifyResponse ) adminConnection.extended( pwdModifyRequest );
            assertEquals( ResultCodeEnum.SUCCESS, pwdModifyResponse.getLdapResult().getResultCode() );
            userEntry = adminConnection.lookup( "cn=User7,ou=system", "*", "+" );
            attribute = userEntry.get( "pwdHistory" );
            assertEquals( 2, attribute.size() );

            // Now try to bind with the new password
            userConnection = getNetworkConnectionAs( ldapServer, "cn=User7,ou=system", "secret4Bis" );
            Entry entry = userConnection.lookup( "cn=User7,ou=system" );

            assertNotNull( entry );
        }
        finally
        {
            policyConfig.setPwdMinAge( minAge );
            safeCloseConnections( userConnection, adminConnection );
        }
    }


    /**
     * Attempt to modify an existing user password and fail.  Then process the
     * password policy control from the response.
     */
    @Test
    public void testModifyUserPasswordWithPasswordPolicyControl() throws Exception
    {
        policyConfig.setPwdCheckQuality( CheckQualityEnum.CHECK_ACCEPT ); // allow the password if its quality can't be checked
        policyConfig.setPwdMinLength( 5 );
        policyConfig.setPwdMinAge( 5 );

        LdapConnection adminConnection = null;
        LdapConnection userConnection = null;
        try
        {
            adminConnection = getAdminNetworkConnection( getLdapServer() );
            adminConnection.setTimeOut( Long.MAX_VALUE );
            addUser( adminConnection, "UserXY", "secret3" );
            Dn userDn = new Dn( "cn=UserXY,ou=system" );

            userConnection = getNetworkConnectionAs( ldapServer, userDn.toString(), "secret3" );
            PasswordPolicyResponse passwordPolicyRequestControl =
                new PasswordPolicyResponseImpl();
            PasswordModifyRequest selfPwdModifyRequest = new PasswordModifyRequestImpl();
            selfPwdModifyRequest.setUserIdentity( Strings.getBytesUtf8( userDn.getNormName() ) );
            selfPwdModifyRequest.setOldPassword( Strings.getBytesUtf8( "secret3" ) );
            selfPwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "1234567" ) );
            selfPwdModifyRequest.addControl( passwordPolicyRequestControl );

            // Send the request to update own password
            PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) userConnection.extended( selfPwdModifyRequest );
            // passwordTooShort is a constraint violation
            assertEquals( ResultCodeEnum.CONSTRAINT_VIOLATION, pwdModifyResponse.getLdapResult().getResultCode() );
            Control passwordPolicyResponseControl = pwdModifyResponse
                .getControl( passwordPolicyRequestControl.getOid() );
            assertNotNull( passwordPolicyResponseControl );
            assertEquals( PasswordPolicyErrorEnum.PASSWORD_TOO_YOUNG,
                ( ( PasswordPolicyResponse ) passwordPolicyResponseControl )
                    .getPasswordPolicyError() );

            addUser( adminConnection, "UserZZ", "secret4" );
            Dn otherUserDn = new Dn( "cn=UserZZ,ou=system" );

            PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
            pwdModifyRequest.setUserIdentity( Strings.getBytesUtf8( otherUserDn.getNormName() ) );
            pwdModifyRequest.setOldPassword( Strings.getBytesUtf8( "secret4" ) );
            pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "1234567" ) );
            pwdModifyResponse = ( PasswordModifyResponse ) userConnection.extended( pwdModifyRequest );
            assertEquals( ResultCodeEnum.INSUFFICIENT_ACCESS_RIGHTS, pwdModifyResponse.getLdapResult().getResultCode() );
        }
        finally
        {
            adminConnection.close();
            userConnection.close();
        }
    }


    /**
     * Modify an existing user password while the user is connected and the password is stored as a hash
     */
    @Test
    public void testModifyPasswordStoredAsHash() throws Exception
    {
        LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() );

        byte[] password = "secret1".getBytes();
        byte[] credHash = PasswordUtil.createStoragePassword( password, LdapSecurityConstants.HASH_METHOD_SHA256 );
        addUser( adminConnection, "User11", credHash );

        // Bind as the user
        LdapConnection userConnection = getNetworkConnectionAs( getLdapServer(), "cn=user11,ou=system", "secret1" );
        userConnection.setTimeOut( 0L );

        // Now change the password
        PasswordModifyRequest pwdModifyRequest = new PasswordModifyRequestImpl();
        pwdModifyRequest.setOldPassword( password );
        pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "secret1Bis" ) );

        // Send the request
        PasswordModifyResponse pwdModifyResponse = ( PasswordModifyResponse ) userConnection.extended( pwdModifyRequest );

        assertEquals( ResultCodeEnum.SUCCESS, pwdModifyResponse.getLdapResult().getResultCode() );

        // Now try to bind with the new password
        userConnection = getNetworkConnectionAs( ldapServer, "cn=User11,ou=system", "secret1Bis" );
        userConnection.loadSchema();

        Entry entry = userConnection.lookup( "cn=User11,ou=system", SchemaConstants.ALL_ATTRIBUTES_ARRAY );

        assertNotNull( entry );

        Attribute at = entry.get( SchemaConstants.USER_PASSWORD_AT );
        assertEquals( LdapSecurityConstants.HASH_METHOD_SHA512, PasswordUtil.findAlgorithm( at.getBytes() ) );

        userConnection.close();
        adminConnection.close();
    }
}
