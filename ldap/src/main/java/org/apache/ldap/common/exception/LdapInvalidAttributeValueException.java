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
package org.apache.ldap.common.exception;


import javax.naming.directory.InvalidAttributeValueException;

import org.apache.ldap.common.message.ResultCodeEnum;


/**
 * Makes a InvalidAttributeValueException unambiguous with respect to the result code it corresponds to
 * by associating an LDAP specific result code with it.
 *
 * @see <a href="http://java.sun.com/j2se/1.4.2/docs/guide/jndi/jndi-ldap-gl.html#EXCEPT">
 * LDAP ResultCode to JNDI Exception Mappings</a>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class LdapInvalidAttributeValueException extends InvalidAttributeValueException implements LdapException
{
    static final long serialVersionUID = 5763624876999168014L;
    /** the LDAP resultCode this exception is associated with */
    private final ResultCodeEnum resultCode;


    /**
     * Creates an Ldap InvalidAttributeValueException using a result code.
     *
     * @param resultCode the LDAP resultCode this exception is associated with
     * @throws IllegalArgumentException if the resultCode argument is not ResultCodeEnum.CONSTRAINTVIOLATION, or
     * ResultCodeEnum.INVALIDATTRIBUTESYNTAX
     */
    public LdapInvalidAttributeValueException( ResultCodeEnum resultCode )
    {
        super();

        switch( resultCode.getValue() )
        {
            case( ResultCodeEnum.CONSTRAINTVIOLATION_VAL ):
                break;
            case( ResultCodeEnum.INVALIDATTRIBUTESYNTAX_VAL ):
                break;
            default:

                throw new IllegalArgumentException( resultCode.getName() + " is not an acceptable result code." );
        }

        this.resultCode = resultCode;
    }


    /**
     * Creates an Ldap InvalidAttributeValueException using a result code and a specific message.
     *
     * @param explanation an explanation for the failure
     * @param resultCode the LDAP resultCode this exception is associated with
     */
    public LdapInvalidAttributeValueException( String explanation, ResultCodeEnum resultCode )
    {
        super( explanation );

        this.resultCode = resultCode;
    }


    /**
     * Gets the LDAP resultCode this exception is associated with.
     *
     * @return the LDAP resultCode this exception is associated with
     */
    public ResultCodeEnum getResultCode()
    {
        return this.resultCode;
    }
}
