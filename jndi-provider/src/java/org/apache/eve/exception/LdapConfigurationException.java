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


import javax.naming.ConfigurationException;

import org.apache.ldap.common.message.ResultCodeEnum;


/**
 * A ConfigurationException which associates a resultCode namely the
 * {@link ResultCodeEnum#OTHER} resultCode with the exception.
 *
 * @see LdapException
 * @see javax.naming.ConfigurationException
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class LdapConfigurationException extends ConfigurationException
        implements LdapException
{
    /**
     * @see javax.naming.NoPermissionException#NoPermissionException()
     */
    public LdapConfigurationException()
    {
        super();
    }


    /**
     * @see javax.naming.NoPermissionException#NoPermissionException(String)
     */
    public LdapConfigurationException( String explanation )
    {
        super( explanation );
    }


    /**
     * Always returns {@link org.apache.ldap.common.message.ResultCodeEnum#OTHER}
     *
     * @see LdapException#getResultCode()
     */
    public ResultCodeEnum getResultCode()
    {
        return ResultCodeEnum.OTHER;
    }
}
