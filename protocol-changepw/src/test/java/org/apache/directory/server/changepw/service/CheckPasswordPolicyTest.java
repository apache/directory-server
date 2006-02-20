/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.changepw.service;


import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.changepw.service.CheckPasswordPolicy;

import junit.framework.TestCase;


/**
 * Tests {@link CheckPasswordPolicy}.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class CheckPasswordPolicyTest extends TestCase
{
    private int passwordLength = 6;
    private int categoryCount = 3;
    private int tokenSize = 3;

    private CheckPasswordPolicy policy = new CheckPasswordPolicy();


    public void testGoodPassword()
    {
        String username = "Enrique Rodriguez";
        String password = "d1r3ct0rY";
        assertTrue( policy.isValidPasswordLength( password, passwordLength ) );
        assertTrue( policy.isValidCategoryCount( password, categoryCount ) );
        assertTrue( policy.isValidUsernameSubstring( username, password, tokenSize ) );
        assertTrue( policy.isValid( username, password, passwordLength, categoryCount, tokenSize ) );
    }


    public void testBadPassword()
    {
        String username = "Erin Randall";
        String password = "erin1";
        assertFalse( policy.isValidPasswordLength( password, passwordLength ) );
        assertFalse( policy.isValidCategoryCount( password, categoryCount ) );
        assertFalse( policy.isValidUsernameSubstring( username, password, tokenSize ) );
        assertFalse( policy.isValid( username, password, passwordLength, categoryCount, tokenSize ) );
    }


    public void testPrincipalAsUsername()
    {
        String username = new KerberosPrincipal( "erodriguez@EXAMPLE.COM" ).getName();
        String password1 = "d1r3ct0rY";
        String password2 = "ERodriguez@d1r3ct0rY";
        String password3 = "Example@d1r3ct0rY";

        assertTrue( policy.isValidUsernameSubstring( username, password1, tokenSize ) );

        assertFalse( policy.isValidUsernameSubstring( username, password2, tokenSize ) );
        assertFalse( policy.isValidUsernameSubstring( username, password3, tokenSize ) );
    }
}
