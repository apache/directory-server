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
package org.apache.directory.server.ldap.handlers.bind.ntlm;


/**
 * The results of an NTLM authentication attempt.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $$Rev$$
 */
public class NtlmAuthenticationResult
{
    private final boolean success;
    private final byte[] response;


    public NtlmAuthenticationResult( byte[] response, boolean success )
    {
        this.response = response;
        this.success = success;
    }


    /**
     * Gets whether or not authentication was a success.
     *
     * @return true if authentication succeeded, or false if it failed
     */
    public boolean isSuccess()
    {
        return success;
    }


    /**
     * Gets a copy of the response to return so it cannot be altered.
     *
     * @return a copy of the authentication response
     */
    public byte[] getResponse()
    {
        if ( response == null )
        {
            return null;
        }
        byte[] copy = new byte[response.length];
        System.arraycopy( response, 0, copy, 0, response.length );
        return copy;
    }
}
