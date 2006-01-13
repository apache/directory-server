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
package org.apache.changepw.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.changepw.ChangePasswordConfiguration;
import org.apache.changepw.exceptions.ChangePasswordException;
import org.apache.changepw.exceptions.ErrorType;
import org.apache.kerberos.messages.components.Authenticator;
import org.apache.protocol.common.chain.Context;
import org.apache.protocol.common.chain.impl.CommandBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A basic password policy check using well-established methods.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class CheckPasswordPolicy extends CommandBase
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( CheckPasswordPolicy.class );

    public boolean execute( Context context ) throws Exception
    {
        ChangePasswordContext changepwContext = (ChangePasswordContext) context;

        ChangePasswordConfiguration config = changepwContext.getConfig();
        Authenticator authenticator = changepwContext.getAuthenticator();
        KerberosPrincipal clientPrincipal = authenticator.getClientPrincipal();

        String password = changepwContext.getPassword();
        String username = clientPrincipal.getName();

        int passwordLength = config.getPasswordLengthPolicy();
        int categoryCount = config.getCategoryCountPolicy();
        int tokenSize = config.getTokenSizePolicy();

        if ( isValid( username, password, passwordLength, categoryCount, tokenSize ) )
        {
            return CONTINUE_CHAIN;
        }

        String explanation = buildErrorMessage( username, password, passwordLength, categoryCount, tokenSize );
        log.error( explanation );

        byte[] explanatoryData = explanation.getBytes( "UTF-8" );

        throw new ChangePasswordException( ErrorType.KRB5_KPASSWD_SOFTERROR, explanatoryData );
    }

    /**
     * Tests that:
     * The password is at least six characters long.
     * The password contains a mix of characters.
     * The password does not contain three letter (or more) tokens from the user's account name.
     */
    boolean isValid( String username, String password, int passwordLength, int categoryCount, int tokenSize )
    {
        return isValidPasswordLength( password, passwordLength ) && isValidCategoryCount( password, categoryCount )
                && isValidUsernameSubstring( username, password, tokenSize );
    }

    /**
     * The password is at least six characters long.
     */
    boolean isValidPasswordLength( String password, int passwordLength )
    {
        return password.length() >= passwordLength;
    }

    /**
     * The password contains characters from at least three of the following four categories:
     * English uppercase characters (A - Z)
     * English lowercase characters (a - z)
     * Base 10 digits (0 - 9)
     * Any non-alphanumeric character (for example: !, $, #, or %)
     */
    boolean isValidCategoryCount( String password, int categoryCount )
    {
        int uppercase = 0;
        int lowercase = 0;
        int digit = 0;
        int nonAlphaNumeric = 0;

        char[] characters = password.toCharArray();

        for ( int ii = 0; ii < characters.length; ii++ )
        {
            if ( Character.isLowerCase( characters[ ii ] ) )
            {
                lowercase = 1;
            }
            else
            {
                if ( Character.isUpperCase( characters[ ii ] ) )
                {
                    uppercase = 1;
                }
                else
                {
                    if ( Character.isDigit( characters[ ii ] ) )
                    {
                        digit = 1;
                    }
                    else
                    {
                        if ( !Character.isLetterOrDigit( characters[ ii ] ) )
                        {
                            nonAlphaNumeric = 1;
                        }
                    }
                }
            }
        }

        return ( uppercase + lowercase + digit + nonAlphaNumeric ) >= categoryCount;
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
    boolean isValidUsernameSubstring( String username, String password, int tokenSize )
    {
        String[] tokens = username.split( "[^a-zA-Z]" );

        for ( int ii = 0; ii < tokens.length; ii++ )
        {
            if ( tokens[ ii ].length() >= tokenSize )
            {
                if ( password.matches( "(?i).*" + tokens[ ii ] + ".*" ) )
                {
                    return false;
                }
            }
        }

        return true;
    }

    private String buildErrorMessage( String username, String password, int passwordLength, int categoryCount,
            int tokenSize )
    {
        List violations = new ArrayList();

        if ( !isValidPasswordLength( password, passwordLength ) )
        {
            violations.add( "length too short" );
        }

        if ( !isValidCategoryCount( password, categoryCount ) )
        {
            violations.add( "insufficient character mix" );
        }

        if ( !isValidUsernameSubstring( username, password, tokenSize ) )
        {
            violations.add( "contains portions of username" );
        }

        StringBuffer sb = new StringBuffer( "Password violates policy:  " );

        Iterator it = violations.iterator();

        while ( it.hasNext() )
        {
            sb.append( (String) it.next() );

            if ( it.hasNext() )
            {
                sb.append( ", " );
            }
        }

        return sb.toString();
    }
}
