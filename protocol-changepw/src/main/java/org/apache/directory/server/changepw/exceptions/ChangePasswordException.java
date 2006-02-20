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

package org.apache.directory.server.changepw.exceptions;


import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;


/**
 * The root of the Change Password exception hierarchy.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ChangePasswordException extends KerberosException
{
    private static final long serialVersionUID = 4880242751298831543L;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    /**
     * Creates a ChangePasswordException with an error code and a message.
     *
     * @param errorCode the error code associated with this ChangePasswordException
     * @param msg the standard Change Password error message for this ChangePasswordException
     */
    public ChangePasswordException(int errorCode, String msg)
    {
        super( errorCode, msg );
    }


    /**
     * Creates a ChangePasswordException with an error code, a message and an
     * underlying throwable that caused this fault.
     *
     * @param errorCode the error code associated with this ChangePasswordException
     * @param msg the standard Change Password error message for this ChangePasswordException
     * @param cause the underlying failure, if any
     */
    public ChangePasswordException(int errorCode, String msg, Throwable cause)
    {
        super( errorCode, msg, cause );
    }


    /**
     * Creates a ChangePasswordException with an error code and a message.
     *
     * @param errorType the error type associated with this ChangePasswordException
     */
    public ChangePasswordException(ErrorType errorType)
    {
        super( errorType.getOrdinal(), errorType.getMessage() );
    }


    /**
     * Creates a ChangePasswordException with an error code, a message, and
     * data helping to explain what caused this fault.
     *
     * @param errorType the error type associated with this ChangePasswordException
     * @param explanatoryData data helping to explain this fault, if any
     */
    public ChangePasswordException(ErrorType errorType, byte[] explanatoryData)
    {
        super( errorType.getOrdinal(), errorType.getMessage(), explanatoryData );
    }
}
