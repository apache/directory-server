/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.shared.ldap.exception;


import javax.naming.OperationNotSupportedException;

import org.apache.directory.shared.ldap.message.ResultCodeEnum;


/**
 * An LDAPException that extends the OperationNotSupportedException 
 * carrying with it the corresponding result codes for this condition.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class LdapOperationNotSupportedException extends OperationNotSupportedException implements LdapException
{
    private static final long serialVersionUID = 1L;
    private final ResultCodeEnum resultCode;
    
    
    public LdapOperationNotSupportedException( String description, ResultCodeEnum resultCode )
    {
        super( description );
        checkResultCode( resultCode );
        this.resultCode = resultCode;
    }
    

    public LdapOperationNotSupportedException( ResultCodeEnum resultCode )
    {
        checkResultCode( resultCode );
        this.resultCode = resultCode;
    }
    
    
    private void checkResultCode( ResultCodeEnum resultCode )
    {
        if ( ! resultCode.equals( ResultCodeEnum.UNWILLING_TO_PERFORM ) && 
            ! resultCode.equals( ResultCodeEnum.UNAVAILABLE_CRITICAL_EXTENSION ) )
        {
            throw new IllegalArgumentException( "Only UNWILLING_TO_PERFORM and UNAVAILABLE_CRITICAL_EXTENSION "
                + "result codes are allowed to be used with this exception" );
        }
    }
    

    public ResultCodeEnum getResultCode()
    {
        return resultCode;
    }

}
