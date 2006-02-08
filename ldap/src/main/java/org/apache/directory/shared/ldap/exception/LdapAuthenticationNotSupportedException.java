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


import javax.naming.AuthenticationNotSupportedException;

import org.apache.directory.shared.ldap.message.ResultCodeEnum;


/**
 * A subclass of the {@link AuthenticationNotSupportedException} carrying along
 * an unequivocal ResultCodeEnum value.
 * 
 * @see <a
 *      href="http://java.sun.com/j2se/1.4.2/docs/guide/jndi/jndi-ldap-gl.html#EXCEPT">
 *      LDAP ResultCode to JNDI Exception Mappings</a>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class LdapAuthenticationNotSupportedException extends AuthenticationNotSupportedException implements
    LdapException
{
    static final long serialVersionUID = 2532733848470791678L;

    /** the result code type safe enumeration */
    private final ResultCodeEnum resultCode;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    /**
     * Creates an LdapException with the resultCode.
     * 
     * @see AuthenticationNotSupportedException#AuthenticationNotSupportedException()
     * @param resultCode
     *            the resultCode enumeration
     * @throws IllegalArgumentException
     *             if the resultCode is not one of the codes corresponding to an
     *             AuthenticationNotSupportedException
     */
    public LdapAuthenticationNotSupportedException(ResultCodeEnum resultCode)
    {
        super();
        this.resultCode = resultCode;
        checkResultCode();
    }


    /**
     * Sets the resultCode associated with this LdapException.
     * 
     * @see AuthenticationNotSupportedException#AuthenticationNotSupportedException(String)
     * @param resultCode
     *            the resultCode enumeration
     * @throws IllegalArgumentException
     *             if the resultCode is not one of the codes corresponding to an
     *             AuthenticationNotSupportedException
     */
    public LdapAuthenticationNotSupportedException(String explanation, ResultCodeEnum resultCode)
    {
        super( explanation );
        this.resultCode = resultCode;
        checkResultCode();
    }


    // ------------------------------------------------------------------------
    // LdapException methods
    // ------------------------------------------------------------------------

    /**
     * @see LdapException#getResultCode()
     */
    public ResultCodeEnum getResultCode()
    {
        return resultCode;
    }


    // ------------------------------------------------------------------------
    // P R I V A T E M E T H O D S
    // ------------------------------------------------------------------------

    /**
     * Checks to make sure the resultCode value is right for this exception
     * type.
     * 
     * @throws IllegalArgumentException
     *             if the result code is not one of
     *             {@link ResultCodeEnum#INAPPROPRIATEAUTHENTICATION},
     *             {@link ResultCodeEnum#AUTHMETHODNOTSUPPORTED},
     *             {@link ResultCodeEnum#CONFIDENTIALITYREQUIRED}.
     */
    private void checkResultCode()
    {
        switch ( resultCode.getValue() )
        {
            case ( ResultCodeEnum.INAPPROPRIATEAUTHENTICATION_VAL  ):
                break;
            case ( ResultCodeEnum.CONFIDENTIALITYREQUIRED_VAL  ):
                break;
            case ( ResultCodeEnum.AUTHMETHODNOTSUPPORTED_VAL  ):
                break;
            default:
                throw new IllegalArgumentException( "Unexceptable result code " + "for this exception type: "
                    + resultCode.getName() );
        }
    }
}
