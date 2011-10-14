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
package org.apache.directory.server.core.authn.ppolicy;


import org.apache.directory.shared.ldap.model.exception.LdapException;


/**
 * A exception class defined for PasswordPolicy related errors.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PasswordPolicyException extends LdapException
{
    private static final long serialVersionUID = -9158126177779964262L;

    /** password policy error code */
    private int errorCode;

    /** the array of valid error codes representing password policy errors */
    private static final int[] VALID_CODES = {0, 1, 2, 3, 4, 5, 6, 7, 8};

    public PasswordPolicyException( Throwable cause )
    {
        super( cause );
    }


    public PasswordPolicyException( String message )
    {
        super( message );
    }


    public PasswordPolicyException( String message, int errorCode )
    {
        super( message );
        validateErrorCode( errorCode );
        this.errorCode = errorCode;
    }


    public PasswordPolicyException( int errorCode )
    {
        validateErrorCode( errorCode );
        this.errorCode = errorCode;
    }


    public int getErrorCode()
    {
        return errorCode;
    }
    
    
    /**
     * this method checks if the given error code is valid or not.
     * This method was created cause using PasswordPolicyErrorEnum class creates some 
     * unwanted dependency issues on core-api
     * 
     * @param errorCode the error code of password policy
     */
    private void validateErrorCode( int errorCode )
    {
        for ( int i : VALID_CODES )
        {
            if ( i == errorCode )
            {
                return;
            }
        }
        
        throw new IllegalArgumentException( "Unknown password policy response error code " + errorCode );
    }
}
