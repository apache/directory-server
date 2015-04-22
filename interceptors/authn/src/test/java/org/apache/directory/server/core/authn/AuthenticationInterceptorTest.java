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
package org.apache.directory.server.core.authn;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AuthenticationInterceptorTest
{

    @Test
    public void testSetAuthenticatorsWithDefaultAuthenticators()
    {
        Authenticator[] authenticators = new Authenticator[]
            { new AnonymousAuthenticator(), new SimpleAuthenticator(), new StrongAuthenticator() };

        setAuthenticatorsAndAssertAllAreRegistered( authenticators );
    }


    @Test
    public void testSetAuthenticatorsWithDelegatingAuthenticator()
    {
        Authenticator[] authenticators = new Authenticator[]
            { new AnonymousAuthenticator(), new SimpleAuthenticator(), new StrongAuthenticator(),
                new DelegatingAuthenticator() };

        setAuthenticatorsAndAssertAllAreRegistered( authenticators );
    }


    @Test
    public void testSetAuthenticatorsWithDelegatingAuthenticatorShuffled()
    {
        Authenticator[] authenticators = new Authenticator[]
            { new AnonymousAuthenticator(), new SimpleAuthenticator(), new StrongAuthenticator(),
                new DelegatingAuthenticator(), };

        for ( int i = 0; i < 42; i++ )
        {
            Collections.shuffle( Arrays.asList( authenticators ) );

            setAuthenticatorsAndAssertAllAreRegistered( authenticators );

        }
    }


    private void setAuthenticatorsAndAssertAllAreRegistered( Authenticator[] authenticators )
    {
        AuthenticationInterceptor ai = new AuthenticationInterceptor();
        ai.setAuthenticators( authenticators );

        // assert all authenticators are registered
        assertEquals( authenticators.length, ai.getAuthenticators().size() );
        assertTrue( ai.getAuthenticators().containsAll( Arrays.asList( authenticators ) ) );
    }

}
