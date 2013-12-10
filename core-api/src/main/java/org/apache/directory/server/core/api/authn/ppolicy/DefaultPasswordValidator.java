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

package org.apache.directory.server.core.api.authn.ppolicy;

import org.apache.directory.api.ldap.model.entry.Entry;


/**
 * The default password validator.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DefaultPasswordValidator implements PasswordValidator
{

    /** the default validator's instance */
    public static final DefaultPasswordValidator INSTANCE = new DefaultPasswordValidator();


    /**
     * Creates a new instance of DefaultPasswordValidator.
     */
    public DefaultPasswordValidator()
    {
    }


    /**
     * {@inheritDoc}
     */
    public void validate( String password, Entry entry ) throws PasswordPolicyException
    {
        checkUsernameSubstring( password, entry );
        //TODO add more checks
    }


    /**
     * The password does not contain three letter (or more) tokens from the user's account name.
     *
     * If the account name is less than three characters long, this check is not performed
     * because the rate at which passwords would be rejected is too high. For each token that is
     * three or more characters long, that token is searched for in the password; if it is present,
     * the password change is rejected. For example, the name "First M. Last" would be split into
     * three tokens: "First", "M", and "Last". Because the second token is only one character long,
     * it would be ignored. Therefore, this user could not have a password that included either
     * "first" or "last" as a substring anywhere in the password. All of these checks are
     * case-insensitive.
     */
    private void checkUsernameSubstring( String password, Entry entry ) throws PasswordPolicyException
    {
        String username = entry.getDn().getRdn().getValue().getString();
        
        if ( username == null || username.trim().length() == 0 )
        {
            return;
        }

        String[] tokens = username.split( "[^a-zA-Z]" );

        for ( String token : tokens )
        {
            if ( ( token == null ) || ( token.length() < 4 ) )
            {
                // Two short : continue with the next token
                continue;
            }

            if ( password.matches( "(?i).*" + token + ".*" ) )
            {
                throw new PasswordPolicyException( "Password shouldn't contain parts of the username", 5 );// 5 == PasswordPolicyErrorEnum.INSUFFICIENT_PASSWORD_QUALITY
            }
        }
    }
}
