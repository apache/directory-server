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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.directory.api.ldap.extras.extended.whoAmI.WhoAmIRequest;
import org.apache.directory.api.ldap.extras.extended.whoAmI.WhoAmIRequestImpl;
import org.apache.directory.api.ldap.extras.extended.whoAmI.WhoAmIResponse;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.hash.Sha512PasswordHashingInterceptor;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.ldap.handlers.extended.WhoAmIHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Test the RbacCreateSession extended operation
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( { ApacheDSTestExtension.class } )
@CreateLdapServer(
    transports =
        { @CreateTransport(protocol = "LDAP") },
    extendedOpHandlers =
        { WhoAmIHandler.class },
    allowAnonymousAccess = true)
//disable changelog, for more info see DIRSERVER-1528
@CreateDS(enableChangeLog = false, name = "RbacCreateSessionTest", additionalInterceptors =
    { Sha512PasswordHashingInterceptor.class })
public class WhoAmIIT extends AbstractLdapTestUnit
{
    /**
     * Test that the WhoAmI extended operation is handled correctly
     */
    @Test
    public void testRbacCreateSessionExtendedOperation() throws Exception
    {
        LdapConnection adminConnection = getAdminNetworkConnection( getLdapServer() );

        // Create a new RBAC session
        WhoAmIRequest whoAmIRequest = new WhoAmIRequestImpl();

        // Send the request
        WhoAmIResponse whoAmIResponse = ( WhoAmIResponse ) adminConnection.extended( whoAmIRequest );

        assertEquals( ResultCodeEnum.SUCCESS, whoAmIResponse.getLdapResult().getResultCode() );
        assertTrue( whoAmIResponse.isDnAuthzId() );
        assertEquals( "uid=admin,ou=system", whoAmIResponse.getDn().toString() );
        adminConnection.close();
    }
}
