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
import static org.junit.Assert.assertNull;

import org.apache.directory.api.ldap.codec.api.LdapApiService;
import org.apache.directory.api.ldap.codec.api.LdapApiServiceFactory;
import org.apache.directory.api.ldap.extras.controls.ppolicy.PasswordPolicy;
import org.apache.directory.api.ldap.extras.controls.ppolicy.PasswordPolicyImpl;
import org.apache.directory.api.ldap.extras.controls.ppolicy_impl.PasswordPolicyDecorator;
import org.apache.directory.api.ldap.extras.extended.PwdModifyRequestImpl;
import org.apache.directory.api.ldap.extras.extended.PwdModifyResponse;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.message.AddRequest;
import org.apache.directory.api.ldap.model.message.AddRequestImpl;
import org.apache.directory.api.ldap.model.message.AddResponse;
import org.apache.directory.api.ldap.model.message.Control;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.util.Strings;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.ldap.handlers.extended.PwdModifyHandler;
import org.junit.Ignore;
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
        { PwdModifyHandler.class })
public class PwdModifyIT extends AbstractLdapTestUnit
{
    private static final LdapApiService codec = LdapApiServiceFactory.getSingleton();

    private static final PasswordPolicyDecorator PP_REQ_CTRL =
        new PasswordPolicyDecorator( codec, new PasswordPolicyImpl() );


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
     * Modify an existing user password while the user is connected
     */
    @Test
    @Ignore
    public void testModifyOwnPasswordConnected() throws Exception
    {
        LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() );

        addUser( adminConnection, "User", "secret" );

        // Bind as the user
        LdapConnection userConnection = getNetworkConnectionAs( getLdapServer(), "cn=user,ou=system", "secret" );
        userConnection.setTimeOut( 0L );

        // Now change the password
        PwdModifyRequestImpl pwdModifyRequest = new PwdModifyRequestImpl();
        pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "secretBis" ) );

        // Send the request
        PwdModifyResponse pwdModifyResponse = ( PwdModifyResponse ) userConnection.extended( pwdModifyRequest );

        userConnection.close();
        adminConnection.close();
    }


    /**
     * Modify an existing user password while the user is not connected
     */
    @Test
    @Ignore
    public void testModifyUserPasswordAnonymous() throws Exception
    {
        LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() );

        addUser( adminConnection, "User", "secret" );

        // Bind as the user
        LdapConnection anonymousConnection = getAnonymousNetworkConnection( getLdapServer() );
        anonymousConnection.setTimeOut( 0L );

        // Now change the password
        PwdModifyRequestImpl pwdModifyRequest = new PwdModifyRequestImpl();
        pwdModifyRequest.setUserIdentity( Strings.getBytesUtf8( "cn=User,ou=system" ) );
        pwdModifyRequest.setOldPassword( Strings.getBytesUtf8( "secret" ) );
        pwdModifyRequest.setNewPassword( Strings.getBytesUtf8( "secretBis" ) );

        // Send the request
        PwdModifyResponse pwdModifyResponse = ( PwdModifyResponse ) anonymousConnection.extended( pwdModifyRequest );

        anonymousConnection.close();
        adminConnection.close();
    }


    /**
     * Modify an existing user password while the user is not connected
     */
    @Test
    @Ignore
    public void testSwitchFromAnonymousToBound() throws Exception
    {
        LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() );

        addUser( adminConnection, "User", "secret" );

        // Bind as the user
        LdapConnection anonymousConnection = getAnonymousNetworkConnection( getLdapServer() );
        anonymousConnection.setTimeOut( 0L );

        anonymousConnection.bind( "cn=user,ou=system", "secret" );

        anonymousConnection.close();
        adminConnection.close();
    }
}
