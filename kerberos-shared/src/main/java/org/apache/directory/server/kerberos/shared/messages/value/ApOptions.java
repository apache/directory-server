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
package org.apache.directory.server.kerberos.shared.messages.value;


public class ApOptions extends Options
{
    // AP Request option - reserved
    public static final int RESERVED = 0;
    // AP Request option - use session key
    public static final int USE_SESSION_KEY = 1;
    // AP Request option - mutual authentication required
    public static final int MUTUAL_REQUIRED = 2;

    // AP Request option - maximum value
    public static final int MAX_VALUE = 32;


    /**
     * Class constructors
     */
    public ApOptions()
    {
        super( MAX_VALUE );
    }


    public ApOptions(byte[] options)
    {
        super( MAX_VALUE );
        setBytes( options );
    }


    /**
     * Converts the object to a printable string
     */
    public String toString()
    {
        StringBuffer result = new StringBuffer();

        if ( get( MUTUAL_REQUIRED ) )
        {
            result.append( "MUTUAL_REQUIRED " );
        }

        if ( get( RESERVED ) )
        {
            result.append( "RESERVED " );
        }

        if ( get( USE_SESSION_KEY ) )
        {
            result.append( "USE_SESSION_KEY " );
        }

        return result.toString().trim();
    }
}
