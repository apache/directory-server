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

package org.apache.directory.kerberos.client;

/**
 * The class to hold the result of change password operation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ChangePasswordResult
{
    /** the result code */
    private ChangePasswordResultCode code;
    
    /** result message */
    private String message;
    
    
    public ChangePasswordResult( byte[] userData )
    {
        // first 2 bytes contain the result code ( from 0-7 )
        int r = ( userData[0] & 0xFFFF << 8 ) + ( userData[1] & 0xFFFF );
        
        code = ChangePasswordResultCode.getByValue( r );
        
        message = new String( userData, 2, userData.length - 2 );
    }


    public ChangePasswordResultCode getCode()
    {
        return code;
    }


    public String getMessage()
    {
        return message;
    }


    @Override
    public String toString()
    {
        return "ChangePasswordResult [result=" + code + ", message=" + message + "]";
    }
    
}
