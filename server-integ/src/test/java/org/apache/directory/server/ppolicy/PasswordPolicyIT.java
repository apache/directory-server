/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.ppolicy;


import static org.apache.directory.api.ldap.extras.controls.ppolicy.PasswordPolicyErrorEnum.INSUFFICIENT_PASSWORD_QUALITY;
import static org.apache.directory.api.ldap.extras.controls.ppolicy.PasswordPolicyErrorEnum.PASSWORD_EXPIRED;
import static org.apache.directory.api.ldap.model.constants.PasswordPolicySchemaConstants.PWD_ACCOUNT_LOCKED_TIME_AT;
import static org.apache.directory.api.ldap.model.constants.PasswordPolicySchemaConstants.PWD_CHANGED_TIME_AT;
import static org.apache.directory.api.ldap.model.constants.PasswordPolicySchemaConstants.PWD_FAILURE_TIME_AT;
import static org.apache.directory.api.ldap.model.constants.PasswordPolicySchemaConstants.PWD_GRACE_USE_TIME_AT;
import static org.apache.directory.api.ldap.model.constants.PasswordPolicySchemaConstants.PWD_HISTORY_AT;
import static org.apache.directory.server.core.integ.IntegrationUtils.getAdminNetworkConnection;
import static org.apache.directory.server.core.integ.IntegrationUtils.getNetworkConnectionAs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.directory.api.ldap.codec.api.LdapApiService;
import org.apache.directory.api.ldap.codec.api.LdapApiServiceFactory;
import org.apache.directory.api.ldap.extras.controls.ppolicy.PasswordPolicy;
import org.apache.directory.api.ldap.extras.controls.ppolicy.PasswordPolicyImpl;
import org.apache.directory.api.ldap.extras.controls.ppolicy_impl.PasswordPolicyDecorator;
import org.apache.directory.api.ldap.model.constants.LdapSecurityConstants;
import org.apache.directory.api.ldap.model.constants.PasswordPolicySchemaConstants;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapAuthenticationException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.message.AddRequest;
import org.apache.directory.api.ldap.model.message.AddRequestImpl;
import org.apache.directory.api.ldap.model.message.AddResponse;
import org.apache.directory.api.ldap.model.message.BindRequest;
import org.apache.directory.api.ldap.model.message.BindRequestImpl;
import org.apache.directory.api.ldap.model.message.BindResponse;
import org.apache.directory.api.ldap.model.message.Control;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;
import org.apache.directory.api.ldap.model.message.ModifyResponse;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.password.PasswordUtil;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.api.InterceptorEnum;
import org.apache.directory.server.core.api.authn.ppolicy.CheckQualityEnum;
import org.apache.directory.server.core.api.authn.ppolicy.PasswordPolicyConfiguration;
import org.apache.directory.server.core.authn.AuthenticationInterceptor;
import org.apache.directory.server.core.authn.ppolicy.PpolicyConfigContainer;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test cases for testing PasswordPolicy implementation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports =
    {
        @CreateTransport(protocol = "LDAP"),
        @CreateTransport(protocol = "LDAPS") })
// disable changelog, for more info see DIRSERVER-1528
@CreateDS(enableChangeLog = false, name = "PasswordPolicyTest")
public class PasswordPolicyIT extends AbstractLdapTestUnit
{
    private PasswordPolicyConfiguration policyConfig;

    private static final LdapApiService codec = LdapApiServiceFactory.getSingleton();

    private static final PasswordPolicyDecorator PP_REQ_CTRL =
        new PasswordPolicyDecorator( codec, new PasswordPolicyImpl() );


    /**
     * Set a default PaswordPolicy configuration
     */
    @Before
    public void setPwdPolicy() throws LdapException
    {
        policyConfig = new PasswordPolicyConfiguration();

        policyConfig.setPwdMaxAge( 110 );
        policyConfig.setPwdFailureCountInterval( 30 );
        policyConfig.setPwdMaxFailure( 2 );
        policyConfig.setPwdLockout( true );
        policyConfig.setPwdLockoutDuration( 0 );
        policyConfig.setPwdMinLength( 5 );
        policyConfig.setPwdInHistory( 5 );
        policyConfig.setPwdExpireWarning( 600 );
        policyConfig.setPwdGraceAuthNLimit( 5 );
        policyConfig.setPwdCheckQuality( CheckQualityEnum.CHECK_REJECT ); // DO NOT allow the password if its quality can't be checked

        PpolicyConfigContainer policyContainer = new PpolicyConfigContainer();
        policyContainer.setDefaultPolicy( policyConfig );
        AuthenticationInterceptor authenticationInterceptor = ( AuthenticationInterceptor ) getService()
            .getInterceptor( InterceptorEnum.AUTHENTICATION_INTERCEPTOR.getName() );

        authenticationInterceptor.setPwdPolicies( policyContainer );
    }


    @After
    public void closeConnections()
    {
        IntegrationUtils.closeConnections();
    }


    /**
     * Get the PasswordPolicy control from a response
     */
    private PasswordPolicy getPwdRespCtrl( Response resp ) throws Exception
    {
        Control control = resp.getControls().get( PP_REQ_CTRL.getOid() );

        if ( control == null )
        {
            return null;
        }

        return ( ( PasswordPolicyDecorator ) control ).getDecorated();
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
        PasswordPolicy respCtrl = getPwdRespCtrl( addResp );
        assertNull( respCtrl );
    }


    /**
     * Check we can bind a user with a given password
     */
    private void checkBindSuccess( Dn userDn, String password ) throws Exception
    {
        LdapConnection userConnection = getNetworkConnectionAs( getLdapServer(), userDn.getName(), password );
        assertNotNull( userConnection );
        assertTrue( userConnection.isAuthenticated() );

        userConnection.close();
    }


    /**
     * Check we cannot bind a user with a given password
     */
    private void checkBindFailure( Dn userDn, String password ) throws Exception
    {
        try
        {
            LdapConnection userConnection = getNetworkConnectionAs( getLdapServer(), userDn.getName(), password );
            assertNull( userConnection );
            assertFalse( userConnection.isAuthenticated() );

            userConnection.close();
        }
        catch ( LdapException le )
        {
            // Expected
        }
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


    /**
     * Test that we can't inject a hashed password when the ChekcQuality is CHECK_REJECT,
     * and that we can when the CheckQuality is CHECK_ACCEPT
     */
    @Test
    public void testAddUserWithHashedPwd() throws Exception
    {
        LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() );

        byte[] password = PasswordUtil.createStoragePassword( "12345", LdapSecurityConstants.HASH_METHOD_CRYPT );

        Entry userEntry = new DefaultEntry(
            "cn=hashedpwd,ou=system",
            "ObjectClass: top",
            "ObjectClass: person",
            "cn: hashedpwd",
            "sn: hashedpwd_sn",
            "userPassword", password );

        AddRequest addRequest = new AddRequestImpl();
        addRequest.setEntry( userEntry );
        addRequest.addControl( PP_REQ_CTRL );

        // We should get a failure
        AddResponse addResp = adminConnection.add( addRequest );
        assertEquals( ResultCodeEnum.CONSTRAINT_VIOLATION, addResp.getLdapResult().getResultCode() );

        PasswordPolicy respCtrl = getPwdRespCtrl( addResp );
        assertNotNull( respCtrl );
        assertEquals( INSUFFICIENT_PASSWORD_QUALITY, respCtrl.getResponse().getPasswordPolicyError() );

        // Relax the Check Quality to CHECK_ACCEPT
        policyConfig.setPwdCheckQuality( CheckQualityEnum.CHECK_ACCEPT ); // allow the password if its quality can't be checked
        Attribute pwdAt = userEntry.get( SchemaConstants.USER_PASSWORD_AT );
        pwdAt.clear();
        pwdAt.add( password );

        addResp = adminConnection.add( addRequest );
        assertEquals( ResultCodeEnum.SUCCESS, addResp.getLdapResult().getResultCode() );
        respCtrl = getPwdRespCtrl( addResp );
        assertNull( respCtrl );

        LdapConnection userConnection = getNetworkConnectionAs( getLdapServer(), "cn=hashedpwd,ou=system", "12345" );
        assertNotNull( userConnection );
        assertTrue( userConnection.isAuthenticated() );
        adminConnection.close();
    }


    @Test
    public void testPwdLockoutForever() throws Exception
    {
        policyConfig.setPwdMaxFailure( 2 );
        policyConfig.setPwdLockout( true );
        policyConfig.setPwdLockoutDuration( 0 );
        policyConfig.setPwdGraceAuthNLimit( 2 );
        policyConfig.setPwdFailureCountInterval( 30 );

        LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() );

        Dn userDn = new Dn( "cn=user2,ou=system" );
        Entry userEntry = new DefaultEntry(
            userDn.toString(),
            "ObjectClass: top",
            "ObjectClass: person",
            "cn: user2",
            "sn: user_sn",
            "userPassword: 12345" );

        AddRequest addRequest = new AddRequestImpl();
        addRequest.setEntry( userEntry );
        addRequest.addControl( PP_REQ_CTRL );

        AddResponse addResp = adminConnection.add( addRequest );
        assertEquals( ResultCodeEnum.SUCCESS, addResp.getLdapResult().getResultCode() );
        PasswordPolicy respCtrl = getPwdRespCtrl( addResp );
        assertNull( respCtrl );

        BindRequest bindReq = new BindRequestImpl();
        bindReq.setDn( userDn );
        bindReq.setCredentials( "1234" ); // wrong password
        bindReq.addControl( PP_REQ_CTRL );

        LdapConnection userConnection = new LdapNetworkConnection( "localhost", ldapServer.getPort() );

        for ( int i = 0; i < 3; i++ )
        {
            userConnection.bind( bindReq );
            assertFalse( userConnection.isAuthenticated() );
        }

        // Added an extra wait (for Windows)
        Thread.sleep( 2000 );

        userEntry = adminConnection.lookup( userDn, SchemaConstants.ALL_ATTRIBUTES_ARRAY );
        Attribute pwdAccountLockedTime = userEntry.get( PasswordPolicySchemaConstants.PWD_ACCOUNT_LOCKED_TIME_AT );
        assertNotNull( pwdAccountLockedTime );
        assertEquals( "000001010000Z", pwdAccountLockedTime.getString() );

        bindReq = new BindRequestImpl();
        bindReq.setDn( userDn );
        bindReq.setCredentials( "12345" ); // correct password
        bindReq.addControl( PP_REQ_CTRL );
        userConnection.bind( bindReq );
        assertFalse( userConnection.isAuthenticated() ); // but still fails cause account is locked

        userConnection.close();

        // test deleting the password, it should clear all the ppolicy related attributes except the ppolicy subentry

        ModifyRequest modReq = new ModifyRequestImpl();
        modReq.setName( userDn );
        modReq.addControl( PP_REQ_CTRL );
        modReq.remove( userEntry.get( SchemaConstants.USER_PASSWORD_AT ) );

        ModifyResponse modResp = adminConnection.modify( modReq );
        assertEquals( ResultCodeEnum.SUCCESS, modResp.getLdapResult().getResultCode() );

        userEntry = adminConnection.lookup( userDn, "+" );
        assertNull( userEntry.get( PWD_FAILURE_TIME_AT ) );
        assertNull( userEntry.get( PWD_GRACE_USE_TIME_AT ) );
        assertNull( userEntry.get( PWD_HISTORY_AT ) );
        assertNull( userEntry.get( PWD_CHANGED_TIME_AT ) );
        assertNull( userEntry.get( PWD_ACCOUNT_LOCKED_TIME_AT ) );
        adminConnection.close();
    }


    /**
     * Test that we can't change the password before a given delay
     */
    @Test
    public void testPwdMinAge() throws Exception
    {
        policyConfig.setPwdMinAge( 5 );

        LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() );

        Dn userDn = new Dn( "cn=userMinAge,ou=system" );
        Entry userEntry = new DefaultEntry(
            userDn.toString(),
            "ObjectClass: top",
            "ObjectClass: person",
            "cn: userMinAge",
            "sn: userMinAge_sn",
            "userPassword: 12345" );

        adminConnection.add( userEntry );
        Modification modification = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, "userPassword",
            "123456" );

        // We should not be able to modify the password : it's too recent
        try
        {
            adminConnection.modify( userDn, modification );
            fail();
        }
        catch ( LdapException LdapInvalidAttributeValueException )
        {
            // Expected
        }

        // Wait for the pwdMinAge delay to be over
        Thread.sleep( 5000 );

        // Now, we should be able to modify the password
        adminConnection.modify( userDn, modification );

        LdapConnection userConnection = getNetworkConnectionAs( getLdapServer(), userDn.getName(), "123456" );
        assertNotNull( userConnection );
        assertTrue( userConnection.isAuthenticated() );
        userConnection.close();
        adminConnection.close();
    }


    /**
     * Test the number of password we keep in history
     */
    @Test
    public void testPwdInHistory() throws Exception
    {
        policyConfig.setPwdInHistory( 2 );

        LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() );

        Dn userDn = new Dn( "cn=userPwdHist,ou=system" );

        addUser( adminConnection, "userPwdHist", "12345" );

        checkBindSuccess( userDn, "12345" );

        Entry entry = adminConnection.lookup( userDn, "*", "+" );

        Attribute pwdHistAt = entry.get( PasswordPolicySchemaConstants.PWD_HISTORY_AT );
        assertNotNull( pwdHistAt );
        assertEquals( 1, pwdHistAt.size() );

        Thread.sleep( 1000 );// to avoid creating a history value with the same timestamp

        Modification modification = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, "userPassword",
            "67891" );

        adminConnection.modify( userDn, modification );
        checkBindSuccess( userDn, "67891" );

        entry = adminConnection.lookup( userDn, "*", "+" );

        pwdHistAt = entry.get( PasswordPolicySchemaConstants.PWD_HISTORY_AT );
        assertNotNull( pwdHistAt );
        assertEquals( 2, pwdHistAt.size() );

        Thread.sleep( 1000 );// to avoid creating a history value with the same timestamp

        modification = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, "userPassword",
            "abcde" );

        adminConnection.modify( userDn, modification );
        checkBindSuccess( userDn, "abcde" );

        entry = adminConnection.lookup( userDn, "*", "+" );
        pwdHistAt = entry.get( PasswordPolicySchemaConstants.PWD_HISTORY_AT );
        assertNotNull( pwdHistAt );

        // it should still hold only 2 values
        assertEquals( 2, pwdHistAt.size() );

        // try to reuse the password, should fail
        try
        {
            adminConnection.modify( userDn, modification );
            fail();
        }
        catch ( LdapInvalidAttributeValueException liave )
        {
            // Expected
        }

        // Try to reuse the very first password, should succeed
        modification = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, "userPassword",
            "12345" );

        adminConnection.modify( userDn, modification );
        checkBindSuccess( userDn, "12345" );

        adminConnection.close();
    }


    @Test
    public void testPwdLength() throws Exception
    {
        policyConfig.setPwdMinLength( 5 );
        policyConfig.setPwdMaxLength( 7 );
        policyConfig.setPwdCheckQuality( CheckQualityEnum.CHECK_REJECT );

        LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() );

        Dn userDn = new Dn( "cn=userLen,ou=system" );
        Entry userEntry = new DefaultEntry(
            userDn.toString(),
            "ObjectClass: top",
            "ObjectClass: person",
            "cn: userLen",
            "sn: userLen_sn",
            "userPassword: 1234" );

        // Try with a password below the minLength
        try
        {
            adminConnection.add( userEntry );
            fail();
        }
        catch ( LdapInvalidAttributeValueException liave )
        {
            // Expected
        }

        checkBindFailure( userDn, "1234" );

        // Try with a password above the maxLength
        userEntry.put( "userPassword", "12345678" );

        try
        {
            adminConnection.add( userEntry );
            fail();
        }
        catch ( LdapInvalidAttributeValueException liave )
        {
            // Expected
        }

        checkBindFailure( userDn, "12345678" );

        // And try with a correct password
        userEntry.put( "userPassword", "123456" );

        adminConnection.add( userEntry );
        checkBindSuccess( userDn, "123456" );

        adminConnection.close();
    }


    /**
     * Test the pwdMaxAge configuration. The password should expire when the pwdMaxAge is reached, and
     * the password should be locked as we don't have a grace limit. 
     */
    @Test
    public void testPwdMaxAgeNoGraceAuthNLimit() throws Exception
    {
        policyConfig.setPwdMaxAge( 5 );
        policyConfig.setPwdGraceAuthNLimit( 0 );

        LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() );

        Dn userDn = new Dn( "cn=userMaxAgeNoGraceAuthNLimit,ou=system" );
        String password = "12345";

        addUser( adminConnection, "userMaxAgeNoGraceAuthNLimit", password );

        BindRequest bindReq = new BindRequestImpl();
        bindReq.setDn( userDn );
        bindReq.setCredentials( password.getBytes() );
        bindReq.addControl( PP_REQ_CTRL );

        LdapConnection userCon = new LdapNetworkConnection( "localhost", ldapServer.getPort() );
        userCon.setTimeOut( 0 );

        Thread.sleep( 1000 ); // sleep for one second so that the password expire warning will be sent

        BindResponse bindResp = userCon.bind( bindReq );
        assertEquals( ResultCodeEnum.SUCCESS, bindResp.getLdapResult().getResultCode() );

        PasswordPolicy respCtrl = getPwdRespCtrl( bindResp );
        assertNotNull( respCtrl );
        assertTrue( respCtrl.getResponse().getTimeBeforeExpiration() > 0 );

        Thread.sleep( 4500 ); // sleep for four seconds and a half so that the password expires

        // this time it should fail
        bindResp = userCon.bind( bindReq );
        assertEquals( ResultCodeEnum.INVALID_CREDENTIALS, bindResp.getLdapResult().getResultCode() );

        respCtrl = getPwdRespCtrl( bindResp );
        assertEquals( PASSWORD_EXPIRED, respCtrl.getResponse().getPasswordPolicyError() );
        adminConnection.close();
    }


    /**
     * Test the pwdMaxAge configuration with a grace limit. We should be able to bind two
     * times when the password has expired. 
     */
    @Test
    public void testPwdMaxAgeWithGraceAuthNLimit() throws Exception
    {
        policyConfig.setPwdMaxAge( 5 );
        policyConfig.setPwdGraceAuthNLimit( 2 );

        LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() );

        Dn userDn = new Dn( "cn=userMaxAgeWithGraceAuthNLimit,ou=system" );
        String password = "12345";

        addUser( adminConnection, "userMaxAgeWithGraceAuthNLimit", password );

        BindRequest bindReq = new BindRequestImpl();
        bindReq.setDn( userDn );
        bindReq.setCredentials( password.getBytes() );
        bindReq.addControl( PP_REQ_CTRL );

        LdapConnection userCon = new LdapNetworkConnection( "localhost", ldapServer.getPort() );
        userCon.setTimeOut( 0 );

        Thread.sleep( 1000 ); // sleep for one second so that the password expire warning will be sent

        BindResponse bindResp = userCon.bind( bindReq );
        assertEquals( ResultCodeEnum.SUCCESS, bindResp.getLdapResult().getResultCode() );

        PasswordPolicy respCtrl = getPwdRespCtrl( bindResp );
        assertNotNull( respCtrl );
        assertTrue( respCtrl.getResponse().getTimeBeforeExpiration() > 0 );

        Thread.sleep( 4500 ); // sleep for four seconds and a half so that the password expires

        // bind for one more time, should succeed
        bindResp = userCon.bind( bindReq );
        assertEquals( ResultCodeEnum.SUCCESS, bindResp.getLdapResult().getResultCode() );
        respCtrl = getPwdRespCtrl( bindResp );
        assertNotNull( respCtrl );
        assertEquals( 1, respCtrl.getResponse().getGraceAuthNRemaining() );

        // bind for one last time, should succeed
        bindResp = userCon.bind( bindReq );
        assertEquals( ResultCodeEnum.SUCCESS, bindResp.getLdapResult().getResultCode() );
        respCtrl = getPwdRespCtrl( bindResp );
        assertNotNull( respCtrl );
        assertEquals( 0, respCtrl.getResponse().getGraceAuthNRemaining() );

        // this time it should fail
        bindResp = userCon.bind( bindReq );
        assertEquals( ResultCodeEnum.INVALID_CREDENTIALS, bindResp.getLdapResult().getResultCode() );

        respCtrl = getPwdRespCtrl( bindResp );
        assertEquals( PASSWORD_EXPIRED, respCtrl.getResponse().getPasswordPolicyError() );
        adminConnection.close();
    }


    /**
     * Test the pwdMaxAge configuration with a grace period. We should be able to bind for a 
     * period of time when the password has expired. 
     */
    @Test
    public void testPwdMaxAgeWithGraceExpire() throws Exception
    {
        policyConfig.setPwdMaxAge( 5 );
        policyConfig.setPwdGraceExpire( 2 );
        policyConfig.setPwdGraceAuthNLimit( 2 );

        LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() );

        Dn userDn = new Dn( "cn=userMaxAgeWithGraceExpire,ou=system" );
        String password = "12345";

        addUser( adminConnection, "userMaxAgeWithGraceExpire", password );

        BindRequest bindReq = new BindRequestImpl();
        bindReq.setDn( userDn );
        bindReq.setCredentials( password.getBytes() );
        bindReq.addControl( PP_REQ_CTRL );

        LdapConnection userCon = new LdapNetworkConnection( "localhost", ldapServer.getPort() );
        userCon.setTimeOut( 0 );

        Thread.sleep( 1000 ); // sleep for one second so that the password expire warning will be sent

        BindResponse bindResp = userCon.bind( bindReq );
        assertEquals( ResultCodeEnum.SUCCESS, bindResp.getLdapResult().getResultCode() );

        PasswordPolicy respCtrl = getPwdRespCtrl( bindResp );
        assertNotNull( respCtrl );
        assertTrue( respCtrl.getResponse().getTimeBeforeExpiration() > 0 );

        Thread.sleep( 4500 ); // sleep for four seconds and a half so that the password expires

        // bind for one more time, should succeed
        bindResp = userCon.bind( bindReq );
        assertEquals( ResultCodeEnum.SUCCESS, bindResp.getLdapResult().getResultCode() );
        respCtrl = getPwdRespCtrl( bindResp );
        assertNotNull( respCtrl );
        assertEquals( 1, respCtrl.getResponse().getGraceAuthNRemaining() );

        // Wait 1 second, we should still be able to bind
        // bind for one last time, should succeed
        bindResp = userCon.bind( bindReq );
        assertEquals( ResultCodeEnum.SUCCESS, bindResp.getLdapResult().getResultCode() );
        respCtrl = getPwdRespCtrl( bindResp );
        assertNotNull( respCtrl );
        assertEquals( 0, respCtrl.getResponse().getGraceAuthNRemaining() );

        // this time it should fail
        bindResp = userCon.bind( bindReq );
        assertEquals( ResultCodeEnum.INVALID_CREDENTIALS, bindResp.getLdapResult().getResultCode() );

        respCtrl = getPwdRespCtrl( bindResp );
        assertEquals( PASSWORD_EXPIRED, respCtrl.getResponse().getPasswordPolicyError() );
        adminConnection.close();
    }


    /**
     * Test the pwdMaxAge configuration. The password should expire when the pwdMaxAge is reached.
     * @throws Exception
     */
    @Test
    public void testPwdMaxAgeAndGraceAuth() throws Exception
    {
        policyConfig.setPwdMaxAge( 5 );
        policyConfig.setPwdExpireWarning( 4 );
        policyConfig.setPwdGraceAuthNLimit( 2 );

        LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() );

        Dn userDn = new Dn( "cn=userMaxAgeAndGraceAuth,ou=system" );
        String password = "12345";

        addUser( adminConnection, "userMaxAgeAndGraceAuth", password );

        BindRequest bindReq = new BindRequestImpl();
        bindReq.setDn( userDn );
        bindReq.setCredentials( password.getBytes() );
        bindReq.addControl( PP_REQ_CTRL );

        LdapConnection userCon = new LdapNetworkConnection( "localhost", ldapServer.getPort() );
        userCon.setTimeOut( 0 );

        Thread.sleep( 1000 ); // sleep for one second so that the password expire warning will be sent

        BindResponse bindResp = userCon.bind( bindReq );
        assertEquals( ResultCodeEnum.SUCCESS, bindResp.getLdapResult().getResultCode() );

        PasswordPolicy respCtrl = getPwdRespCtrl( bindResp );
        assertNotNull( respCtrl );
        assertTrue( respCtrl.getResponse().getTimeBeforeExpiration() > 0 );

        Thread.sleep( 4000 ); // sleep for four seconds so that the password expires

        // bind for two more times, should succeed
        bindResp = userCon.bind( bindReq );
        assertEquals( ResultCodeEnum.SUCCESS, bindResp.getLdapResult().getResultCode() );
        respCtrl = getPwdRespCtrl( bindResp );
        assertNotNull( respCtrl );
        assertEquals( 1, respCtrl.getResponse().getGraceAuthNRemaining() );

        // this extra second sleep is necessary to modify pwdGraceUseTime attribute with a different timestamp
        Thread.sleep( 1000 );
        bindResp = userCon.bind( bindReq );
        assertEquals( ResultCodeEnum.SUCCESS, bindResp.getLdapResult().getResultCode() );
        respCtrl = getPwdRespCtrl( bindResp );
        assertEquals( 0, respCtrl.getResponse().getGraceAuthNRemaining() );

        // this time it should fail
        bindResp = userCon.bind( bindReq );
        assertEquals( ResultCodeEnum.INVALID_CREDENTIALS, bindResp.getLdapResult().getResultCode() );

        respCtrl = getPwdRespCtrl( bindResp );
        assertEquals( PASSWORD_EXPIRED, respCtrl.getResponse().getPasswordPolicyError() );
        adminConnection.close();
    }


    @Test
    public void testModifyPwdSubentry() throws Exception
    {
        LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() );

        Dn userDn = new Dn( "cn=ppolicySubentry,ou=system" );
        String password = "12345";
        Entry userEntry = new DefaultEntry(
            userDn.toString(),
            "ObjectClass: top",
            "ObjectClass: person",
            "cn: ppolicySubentry",
            "sn: ppolicySubentry_sn",
            "userPassword: " + password,
            "pwdPolicySubEntry:" + userDn.getName() );

        AddRequest addRequest = new AddRequestImpl();
        addRequest.setEntry( userEntry );
        addRequest.addControl( PP_REQ_CTRL );

        AddResponse addResp = adminConnection.add( addRequest );
        assertEquals( ResultCodeEnum.SUCCESS, addResp.getLdapResult().getResultCode() );

        userEntry = adminConnection.lookup( userDn, "*", "+" );
        assertEquals( userDn.getName(), userEntry.get( "pwdPolicySubEntry" ).getString() );

        ModifyRequest modReq = new ModifyRequestImpl();
        modReq.setName( userDn );
        String modSubEntryDn = "cn=policy,ou=system";
        modReq.replace( "pwdPolicySubEntry", modSubEntryDn );
        ModifyResponse modResp = adminConnection.modify( modReq );
        assertEquals( ResultCodeEnum.SUCCESS, modResp.getLdapResult().getResultCode() );

        userEntry = adminConnection.lookup( userDn, "*", "+" );
        assertEquals( modSubEntryDn, userEntry.get( "pwdPolicySubEntry" ).getString() );

        // try to modify the subentry as a non-admin
        adminConnection = new LdapNetworkConnection( "localhost", getLdapServer().getPort() );
        adminConnection.bind( userDn.getName(), password );

        modResp = adminConnection.modify( modReq );
        modReq.replace( "pwdPolicySubEntry", userDn.getName() );
        assertEquals( ResultCodeEnum.INSUFFICIENT_ACCESS_RIGHTS, modResp.getLdapResult().getResultCode() );
        adminConnection.close();
    }


    @Test
    public void testGraceAuth() throws Exception
    {
        policyConfig.setPwdMaxFailure( 2 );
        policyConfig.setPwdLockout( true );
        policyConfig.setPwdLockoutDuration( 0 );
        policyConfig.setPwdGraceAuthNLimit( 2 );
        policyConfig.setPwdFailureCountInterval( 60 );
        policyConfig.setPwdMaxAge( 1 );

        LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() );

        Dn userDn = new Dn( "cn=userGrace,ou=system" );

        addUser( adminConnection, "userGrace", "12345" );

        BindRequest bindReq = new BindRequestImpl();
        bindReq.setDn( userDn );
        bindReq.setCredentials( "12345" ); // grace login
        bindReq.addControl( PP_REQ_CTRL );

        LdapConnection userConnection = new LdapNetworkConnection( "localhost", ldapServer.getPort() );

        Thread.sleep( 2000 ); // let the password expire
        BindResponse bindResp = userConnection.bind( bindReq );
        assertTrue( userConnection.isAuthenticated() );
        PasswordPolicy ppolicy = getPwdRespCtrl( bindResp );
        assertEquals( 1, ppolicy.getResponse().getGraceAuthNRemaining() );

        Entry userEntry = adminConnection.lookup( userDn, "+" );
        Attribute pwdGraceAuthUseTime = userEntry.get( PasswordPolicySchemaConstants.PWD_GRACE_USE_TIME_AT );
        assertNotNull( pwdGraceAuthUseTime );

        Attribute pwdChangedTime = userEntry.get( PasswordPolicySchemaConstants.PWD_CHANGED_TIME_AT );

        ModifyRequest modReq = new ModifyRequestImpl();
        modReq.setName( userDn );
        modReq.replace( SchemaConstants.USER_PASSWORD_AT, "secret1" );
        ModifyResponse modResp = userConnection.modify( modReq );
        assertEquals( ResultCodeEnum.SUCCESS, modResp.getLdapResult().getResultCode() );

        userEntry = adminConnection.lookup( userDn, "+" );
        pwdGraceAuthUseTime = userEntry.get( PasswordPolicySchemaConstants.PWD_GRACE_USE_TIME_AT );
        assertNull( pwdGraceAuthUseTime );

        Attribute latestPwdChangedTime = userEntry.get( PasswordPolicySchemaConstants.PWD_CHANGED_TIME_AT );
        assertNotSame( pwdChangedTime.getString(), latestPwdChangedTime.getString() );

        userConnection.close();
        adminConnection.close();
    }


    @Test
    public void testPwdLockoutWithDuration() throws Exception
    {
        policyConfig.setPwdMaxFailure( 2 );
        policyConfig.setPwdLockout( true );
        policyConfig.setPwdLockoutDuration( 5 ); //5 sec
        policyConfig.setPwdGraceAuthNLimit( 2 );
        policyConfig.setPwdFailureCountInterval( 0 );

        LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() );

        Dn userDn = new Dn( "cn=userLockoutWithDuration,ou=system" );

        addUser( adminConnection, "userLockoutWithDuration", "12345" );

        BindRequest bindReq = new BindRequestImpl();
        bindReq.setDn( userDn );
        bindReq.setCredentials( "1234" ); // wrong password
        bindReq.addControl( PP_REQ_CTRL );

        LdapConnection userConnection = new LdapNetworkConnection( "localhost", ldapServer.getPort() );

        for ( int i = 0; i < 4; i++ )
        {
            userConnection.bind( bindReq );
            assertFalse( userConnection.isAuthenticated() );
        }

        Entry userEntry = adminConnection.lookup( userDn, "+" );
        Attribute pwdAccountLockedTime = userEntry.get( PasswordPolicySchemaConstants.PWD_ACCOUNT_LOCKED_TIME_AT );
        assertNotNull( pwdAccountLockedTime );

        Thread.sleep( 10000 );
        bindReq = new BindRequestImpl();
        bindReq.setDn( userDn );
        bindReq.setCredentials( "12345" ); // correct password
        bindReq.addControl( PP_REQ_CTRL );
        userConnection.setTimeOut( Long.MAX_VALUE );
        userConnection.bind( bindReq );
        assertTrue( userConnection.isAuthenticated() );

        userConnection.close();
        adminConnection.close();
    }


    /**
     * Check that we can't try more than N times to login with a wrong password before
     * being locked.
     * @throws Exception
     */
    @Test
    public void testPwdLockoutWithNAttempts() throws Exception
    {
        policyConfig.setPwdMaxFailure( 3 );
        policyConfig.setPwdLockout( true );

        Dn userDn = new Dn( "cn=userLockout,ou=system" );
        LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() );

        addUser( adminConnection, "userLockout", "12345" );

        LdapConnection userConnection = new LdapNetworkConnection( "localhost", ldapServer.getPort() );
        userConnection.setTimeOut( 0L );

        checkBind( userConnection, userDn, "badPassword", 3,
            "INVALID_CREDENTIALS: Bind failed: ERR_229 Cannot authenticate user cn=userLockout,ou=system" );

        checkBind( userConnection, userDn, "badPassword", 1,
            "INVALID_CREDENTIALS: Bind failed: account was permanently locked" );

        userConnection.close();

        Entry userEntry = adminConnection.lookup( userDn, "+" );
        Attribute pwdAccountLockedTime = userEntry.get( PasswordPolicySchemaConstants.PWD_ACCOUNT_LOCKED_TIME_AT );
        assertNotNull( pwdAccountLockedTime );
        adminConnection.close();
    }


    /**
     * Check that we can't try more than N times to login with a wrong password before
     * being locked. Also check that we have a delay before we can log again.
     */
    @Test
    public void testPwdLockoutWithNAttemptsAndLockoutDelay() throws Exception
    {
        policyConfig.setPwdLockout( true );
        policyConfig.setPwdMaxFailure( 3 );
        policyConfig.setPwdLockoutDuration( 5 );

        Dn userDn = new Dn( "cn=userLockout2,ou=system" );
        LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() );

        addUser( adminConnection, "userLockout2", "12345" );

        LdapConnection userConnection = new LdapNetworkConnection( "localhost", ldapServer.getPort() );
        userConnection.setTimeOut( 0L );

        checkBind( userConnection, userDn, "badPassword", 3,
            "INVALID_CREDENTIALS: Bind failed: ERR_229 Cannot authenticate user cn=userLockout2,ou=system" );

        // Now, try to login until the delay is elapsed
        boolean success = false;
        int t = 0;

        for ( t = 0; t < 10; t++ )
        {
            try
            {
                userConnection.bind( userDn, "12345" );
                //System.out.println( "Attempt success " + ( t + 1 ) + " at " + new Date( System.currentTimeMillis() ) );
                success = true;
                break;
            }
            catch ( LdapException le )
            {
                //System.out.println( "Attempt failure " + ( t + 1 ) + " at " + new Date( System.currentTimeMillis() ) );
                Entry userEntry = adminConnection.lookup( userDn, "+" );
                Attribute pwdAccountLockedTime = userEntry
                    .get( PasswordPolicySchemaConstants.PWD_ACCOUNT_LOCKED_TIME_AT );
                assertNotNull( pwdAccountLockedTime );

                // Expected : wait 1 second before retrying
                Thread.sleep( 1000 );
            }
        }

        assertTrue( success );
        assertTrue( t >= 5 );
        userConnection.close();

        Entry userEntry = adminConnection.lookup( userDn, "+" );
        Attribute pwdAccountLockedTime = userEntry.get( PasswordPolicySchemaConstants.PWD_ACCOUNT_LOCKED_TIME_AT );
        assertNull( pwdAccountLockedTime );
        adminConnection.close();
    }


    /**
     * Check that the failure attempts are removed from the entry when the 
     * pwdFailureCountInterval attribute is set.
     */
    @Test
    public void testPwdLockoutFailureCountInterval() throws Exception
    {
        policyConfig.setPwdLockout( true );
        policyConfig.setPwdMaxFailure( 5 );
        policyConfig.setPwdFailureCountInterval( 2 );

        Dn userDn = new Dn( "cn=userLockout3,ou=system" );
        LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() );

        addUser( adminConnection, "userLockout3", "12345" );

        LdapConnection userConnection = new LdapNetworkConnection( "localhost", ldapServer.getPort() );
        userConnection.setTimeOut( 0L );

        // First attempt
        checkBind( userConnection, userDn, "badPassword", 1,
            "INVALID_CREDENTIALS: Bind failed: ERR_229 Cannot authenticate user cn=userLockout3,ou=system" );

        Entry userEntry = adminConnection.lookup( userDn, "+" );
        Attribute pwdFailureTime = userEntry
            .get( PasswordPolicySchemaConstants.PWD_FAILURE_TIME_AT );
        assertNotNull( pwdFailureTime );
        assertEquals( 1, pwdFailureTime.size() );

        Thread.sleep( 1000 );

        // Second attempt
        checkBind( userConnection, userDn, "badPassword", 1,
            "INVALID_CREDENTIALS: Bind failed: ERR_229 Cannot authenticate user cn=userLockout3,ou=system" );

        userEntry = adminConnection.lookup( userDn, "+" );
        pwdFailureTime = userEntry
            .get( PasswordPolicySchemaConstants.PWD_FAILURE_TIME_AT );
        assertNotNull( pwdFailureTime );
        assertEquals( 2, pwdFailureTime.size() );

        Thread.sleep( 1000 );

        // Third attempt
        checkBind( userConnection, userDn, "badPassword", 1,
            "INVALID_CREDENTIALS: Bind failed: ERR_229 Cannot authenticate user cn=userLockout3,ou=system" );

        userEntry = adminConnection.lookup( userDn, "+" );
        pwdFailureTime = userEntry
            .get( PasswordPolicySchemaConstants.PWD_FAILURE_TIME_AT );
        assertNotNull( pwdFailureTime );
        assertEquals( 2, pwdFailureTime.size() );

        Thread.sleep( 1000 );

        // Forth attempt
        checkBind( userConnection, userDn, "badPassword", 1,
            "INVALID_CREDENTIALS: Bind failed: ERR_229 Cannot authenticate user cn=userLockout3,ou=system" );

        userEntry = adminConnection.lookup( userDn, "+" );
        pwdFailureTime = userEntry
            .get( PasswordPolicySchemaConstants.PWD_FAILURE_TIME_AT );
        assertNotNull( pwdFailureTime );

        // We should not have more than 2 attempts stored
        assertEquals( 2, pwdFailureTime.size() );

        userConnection.close();
        adminConnection.close();
    }


    /**
     * Check that we are delayed between each attempt
     * @throws Exception
     */
    @Test
    @Ignore
    public void testPwdAttempsDelayed() throws Exception
    {
        policyConfig.setPwdMinDelay( 200 );
        policyConfig.setPwdMaxDelay( 400 );
        policyConfig.setPwdLockout( true );

        Dn userDn = new Dn( "cn=userLockout,ou=system" );
        LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() );

        addUser( adminConnection, "userLockout", "12345" );

        LdapConnection userConnection = new LdapNetworkConnection( "localhost", ldapServer.getPort() );

        // Do two attempts 
        checkBind( userConnection, userDn, "badPassword", 1,
            "INVALID_CREDENTIALS: Bind failed: ERR_229 Cannot authenticate user cn=userLockout,ou=system" );

        // Wait 1 second
        Thread.sleep( 1000L );

        // Retry with the correct password : we should get rejected because it's too early
        checkBind( userConnection, userDn, "12345", 1,
            "INVALID_CREDENTIALS: Bind failed: ERR_229 Cannot authenticate user cn=userLockout,ou=system" );

        // Wait 1 second and a bit more
        Thread.sleep( 1200L );

        // Retry : it should work
        userConnection.bind( userDn, "12345" );
        userConnection.close();
        adminConnection.close();
    }


    /**
     * Check the maxIdle : if the user does not bind for more than this delay,
     * the password is locked.
     */
    @Test
    public void testPwMaxIdle() throws Exception
    {
        policyConfig.setPwdMaxIdle( 5 );

        Dn userDn = new Dn( "cn=userLockout4,ou=system" );
        LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() );

        addUser( adminConnection, "userLockout4", "12345" );

        // We should succeed
        checkBindSuccess( userDn, "12345" );

        // Wait 5 seconds now
        Thread.sleep( 5000 );

        // We shpuld not be able to succeed now
        checkBindFailure( userDn, "12345" );

        adminConnection.close();
    }


    /**
     * Check the pwdAllowUserChange
     */
    @Test
    public void testPwdAllowUserChange() throws Exception
    {
        policyConfig.setPwdAllowUserChange( false );

        Dn userDn = new Dn( "cn=userAllowUserChange,ou=system" );
        LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() );

        addUser( adminConnection, "userAllowUserChange", "12345" );

        LdapConnection userConnection = new LdapNetworkConnection( "localhost", ldapServer.getPort() );
        userConnection.setTimeOut( 0L );

        // We should be able to bind
        checkBindSuccess( userDn, "12345" );

        // Now, try to change the password
        ModifyRequest modReq = new ModifyRequestImpl();
        modReq.setName( userDn );
        modReq.addControl( PP_REQ_CTRL );
        modReq.replace( "userPassword", "67890" );

        userConnection = getNetworkConnectionAs( getLdapServer(), userDn.getName(), "12345" );
        userConnection.setTimeOut( 0L );

        ModifyResponse modifyResponse = userConnection.modify( modReq );

        assertEquals( ResultCodeEnum.INSUFFICIENT_ACCESS_RIGHTS, modifyResponse.getLdapResult().getResultCode() );

        // Now, allow the user to change his password
        policyConfig.setPwdAllowUserChange( true );

        modifyResponse = userConnection.modify( modReq );

        assertEquals( ResultCodeEnum.SUCCESS, modifyResponse.getLdapResult().getResultCode() );

        userConnection.close();

        checkBindSuccess( userDn, "67890" );
        adminConnection.close();
    }


    /**
     * Check the pwdExpireWarning
     */
    @Test
    public void testPwdExpireWarning() throws Exception
    {
        // The password will expire in 5 seconds
        policyConfig.setPwdMaxAge( 5 );
        policyConfig.setPwdGraceAuthNLimit( 0 );
        // Send a warning 3 seconds before the expiration
        policyConfig.setPwdExpireWarning( 3 );

        Dn userDn = new Dn( "cn=userExpireWarning,ou=system" );
        LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() );

        addUser( adminConnection, "userExpireWarning", "12345" );

        LdapConnection userConnection = new LdapNetworkConnection( "localhost", ldapServer.getPort() );
        userConnection.setTimeOut( 0L );

        BindRequest bindReq = new BindRequestImpl();
        bindReq.setDn( userDn );
        bindReq.setCredentials( "12345" );
        bindReq.addControl( PP_REQ_CTRL );

        for ( int i = 0; i < 5; i++ )
        {
            BindResponse bindResponse = userConnection.bind( bindReq );
            assertEquals( ResultCodeEnum.SUCCESS, bindResponse.getLdapResult().getResultCode() );

            PasswordPolicy respCtrl = getPwdRespCtrl( bindResponse );
            assertNotNull( respCtrl );

            if ( i < 2 )
            {
                assertNull( respCtrl.getResponse() );
            }
            else
            {
                assertEquals( 5 - i, respCtrl.getResponse().getTimeBeforeExpiration() );
            }

            // Added an one second wait
            Thread.sleep( 1000 );
        }

        // Added an one second wait
        Thread.sleep( 1000 );

        // We should not be able to login
        checkBindFailure( userDn, "12345" );

        userConnection.close();
        adminConnection.close();
    }
}
