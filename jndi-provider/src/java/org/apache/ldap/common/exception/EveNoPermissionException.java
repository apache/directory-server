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
package org.apache.eve.exception;


import javax.naming.NoPermissionException;

import org.apache.ldap.common.message.ResultCodeEnum;


/**
 * A NoPermissionException which associates a resultCode namely the
 * {@link ResultCodeEnum#INSUFFICIENTACCESSRIGHTS} resultCode with the exception.
 *
 * @see EveException
 * @see NoPermissionException
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class EveNoPermissionException extends NoPermissionException
        implements EveException
{
    /**
     * @see NoPermissionException#NoPermissionException()
     */
    public EveNoPermissionException()
    {
        super();
    }


    /**
     * @see NoPermissionException#NoPermissionException(String)
     */
    public EveNoPermissionException( String explanation )
    {
        super( explanation );
    }


    /**
     * Always returns {@link ResultCodeEnum#INSUFFICIENTACCESSRIGHTS}
     *
     * @see EveException#getResultCode()
     */
    public ResultCodeEnum getResultCode()
    {
        return ResultCodeEnum.INSUFFICIENTACCESSRIGHTS;
    }
}
