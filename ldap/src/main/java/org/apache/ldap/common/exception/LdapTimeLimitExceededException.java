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


import javax.naming.TimeLimitExceededException;
import org.apache.ldap.common.message.ResultCodeEnum;


/**
 * A TiimeLimitExceededException which associates a resultCode namely the
 * {@link ResultCodeEnum#SIZELIMITEXCEEDED} resultCode with the exception.
 *
 * @see LdapException
 * @see SizeLimitExceededException
 * @see <a href="http://java.sun.com/j2se/1.4.2/docs/guide/jndi/jndi-ldap-gl.html#EXCEPT">
 * LDAP ResultCode to JNDI Exception Mappings</a>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class LdapTimeLimitExceededException extends TimeLimitExceededException implements LdapException
{
    static final long serialVersionUID = -8611970137960601723L;


    /**
     * @see TimeLimitExceededException#TimeLimitExceededException()
     */
    public LdapTimeLimitExceededException()
    {
        super();
    }


    /**
     * @see TimeLimitExceededException#TimeLimitExceededException(String)
     */
    public LdapTimeLimitExceededException( String explanation )
    {
        super( explanation );
    }


    /**
     * Always returns {@link ResultCodeEnum#TIMELIMITEXCEEDED}
     *
     * @see LdapException#getResultCode()
     */
    public ResultCodeEnum getResultCode()
    {
        return ResultCodeEnum.TIMELIMITEXCEEDED;
    }
}
