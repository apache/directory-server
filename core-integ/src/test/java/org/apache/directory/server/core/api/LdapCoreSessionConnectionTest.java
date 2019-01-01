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
package org.apache.directory.server.core.api;


import static org.junit.Assert.assertNotNull;

import org.apache.directory.api.ldap.extras.controls.ppolicy.PasswordPolicyResponse;
import org.apache.directory.api.ldap.extras.controls.ppolicy.PasswordPolicyResponseImpl;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.BindRequest;
import org.apache.directory.api.ldap.model.message.BindRequestImpl;
import org.apache.directory.api.ldap.model.message.BindResponse;
import org.apache.directory.api.ldap.model.message.Control;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.Network;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.api.authn.ppolicy.CheckQualityEnum;
import org.apache.directory.server.core.api.authn.ppolicy.PasswordPolicyConfiguration;
import org.apache.directory.server.core.authn.AuthenticationInterceptor;
import org.apache.directory.server.core.authn.ppolicy.PpolicyConfigContainer;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A test case to ensure that LdapCoresSessionConnection works correctly
 * with controls.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports =
    {
        @CreateTransport(protocol = "LDAP") })
// disable changelog, for more info see DIRSERVER-1528
@CreateDS(enableChangeLog = false, name = "LdapCoreSessionConnectionTest")
@ApplyLdifs(
    {
        // Add a non admin user
        "dn: cn=user,ou=system",
        "objectClass: top",
        "objectClass: person",
        "cn: user",
        "userPassword: secret",
        "sn: user" })
public class LdapCoreSessionConnectionTest extends AbstractLdapTestUnit
{
    private static Logger logger = LoggerFactory.getLogger( LdapCoreSessionConnection.class );
    private static final PasswordPolicyResponse passwordPolicyRequestControl =
        new PasswordPolicyResponseImpl();


    @Before
    public void setPwdPolicy() throws LdapException
    {
        PasswordPolicyConfiguration policyConfig = new PasswordPolicyConfiguration();
        policyConfig.setPwdCheckQuality( CheckQualityEnum.CHECK_REJECT ); // DO NOT allow the password if its quality can't be checked

        Dn policyDn = new Dn(
            "ads-pwdId=test,ou=passwordPolicies,ads-interceptorId=authenticationInterceptor,ou=interceptors,ads-directoryServiceId=default,ou=config" );
        PpolicyConfigContainer policyContainer = new PpolicyConfigContainer();
        policyContainer.addPolicy( policyDn, policyConfig );
        policyContainer.setDefaultPolicyDn( policyDn );

        AuthenticationInterceptor authenticationInterceptor = ( AuthenticationInterceptor ) getService()
            .getInterceptor( InterceptorEnum.AUTHENTICATION_INTERCEPTOR.getName() );
        authenticationInterceptor.setPwdPolicies( policyContainer );
    }


    @Test
    public void testBindWithLdapNetworkConnection() throws LdapException
    {
        LdapNetworkConnection connection = null;
        try
        {
            connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, getLdapServer().getPort() );

            BindRequest bindRequest = new BindRequestImpl();
            bindRequest.setDn( new Dn( "cn=user,ou=system" ) );
            bindRequest.setCredentials( "secret" );
            bindRequest.addControl( passwordPolicyRequestControl );

            BindResponse bindResponse = connection.bind( bindRequest );
            Control responseControl = bindResponse.getControls().get( passwordPolicyRequestControl.getOid() );
            assertNotNull( responseControl );
            PasswordPolicyResponse passwordPolicy = ( PasswordPolicyResponse ) responseControl;
            assertNotNull( passwordPolicy );
        }
        finally
        {
            safeClose( connection );
        }
    }


    //@Ignore
    @Test
    public void testBindWithLdapCoreSessionConnection() throws LdapException
    {
        LdapCoreSessionConnection connection = null;
        try
        {
            connection = new LdapCoreSessionConnection();
            connection.setDirectoryService( getService() );

            BindRequest bindRequest = new BindRequestImpl();
            bindRequest.setDn( new Dn( "cn=user,ou=system" ) );
            bindRequest.setCredentials( "secret" );
            bindRequest.addControl( passwordPolicyRequestControl );

            BindResponse bindResponse = connection.bind( bindRequest );
            Control responseControl = bindResponse.getControls().get( passwordPolicyRequestControl.getOid() );
            assertNotNull( responseControl );
            PasswordPolicyResponse passwordPolicy = ( PasswordPolicyResponse ) responseControl;
            assertNotNull( passwordPolicy );
        }
        finally
        {
            safeClose( connection );
        }
    }


    private static void safeClose( LdapConnection... connections )
    {
        for ( LdapConnection connection : connections )
        {
            try
            {
                connection.close();
            }
            catch ( Exception e )
            {
                logger.warn( "close failed, possible connection leak: {}", e.getMessage() );
                logger.debug( "close failed, possible connection leak: ", e );
            }
        }
    }
}
