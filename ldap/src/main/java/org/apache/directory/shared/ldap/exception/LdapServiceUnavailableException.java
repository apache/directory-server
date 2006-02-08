/*
 *   Copyright 2004 The Apache Software Foundation
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
package org.apache.directory.shared.ldap.exception;


import javax.naming.ServiceUnavailableException;

import org.apache.directory.shared.ldap.message.ResultCodeEnum;


/**
 * LDAP specific ServiceUnavailableException that preserves resultCode
 * resolution.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class LdapServiceUnavailableException extends ServiceUnavailableException implements LdapException
{
    static final long serialVersionUID = -5058439476235675179L;

    /** the LDAP result code to be checked */
    private final ResultCodeEnum resultCode;


    /**
     * Creates an LDAP specific ServiceUnavailableException that preserves
     * resultCode resolution.
     * 
     * @param resultCode
     *            the LDAP result code to be checked
     * @throws IllegalArgumentException
     *             if the result code is not within the set
     *             {@link ResultCodeEnum#SERVICEUNAVAILABLE_CODES}.
     */
    public LdapServiceUnavailableException(ResultCodeEnum resultCode)
    {
        super();
        checkResultCode( resultCode );
        this.resultCode = resultCode;
    }


    /**
     * Creates an LDAP specific ServiceUnavailableException that preserves
     * resultCode resolution.
     * 
     * @param explanation
     *            the reason for the exception to pass to super
     * @param resultCode
     *            the LDAP result code to be checked
     * @throws IllegalArgumentException
     *             if the result code is not within the set
     *             {@link ResultCodeEnum#SERVICEUNAVAILABLE_CODES}.
     */
    public LdapServiceUnavailableException(String explanation, ResultCodeEnum resultCode)
    {
        super( explanation );
        checkResultCode( resultCode );
        this.resultCode = resultCode;
    }


    /**
     * Checks to see if the LDAP result code is valid for this exception.
     * 
     * @param resultCode
     *            the LDAP result code to be checked
     * @throws IllegalArgumentException
     *             if the result code is not within the set
     *             {@link ResultCodeEnum#SERVICEUNAVAILABLE_CODES}.
     */
    private void checkResultCode( ResultCodeEnum resultCode )
    {
        if ( !ResultCodeEnum.getServiceCodes().contains( resultCode ) )
        {
            String msg = "Only the following LDAP result codes can be used: " + ResultCodeEnum.getSearchCodes();
            throw new IllegalArgumentException( msg );
        }
    }


    /**
     * Returns one of the resultCodes within the set {@link
     * ResultCodeEnum#SERVICEUNAVAILABLE_CODES}.
     * 
     * @see LdapException#getResultCode()
     */
    public final ResultCodeEnum getResultCode()
    {
        return resultCode;
    }
}
