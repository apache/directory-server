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


import javax.naming.ContextNotEmptyException;

import org.apache.directory.shared.ldap.message.ResultCodeEnum;


/**
 * A ContextNotEmptyException which contains an LDAP result code.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class LdapContextNotEmptyException extends ContextNotEmptyException implements LdapException
{
    static final long serialVersionUID = -2320797162018226278L;


    public LdapContextNotEmptyException()
    {
        super();
    }


    public LdapContextNotEmptyException(String explanation)
    {
        super( explanation );
    }


    /**
     * Gets the LDAP ResultCode for this exception type.
     * 
     * @return {@link ResultCodeEnum#NOTALLOWEDONNONLEAF} always
     */
    public ResultCodeEnum getResultCode()
    {
        return ResultCodeEnum.NOTALLOWEDONNONLEAF;
    }
}
