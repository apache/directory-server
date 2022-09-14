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
package org.apache.directory.shared.kerberos.exceptions;




/**
 * The root of the Kerberos exception hierarchy.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KerberosException extends Exception
{
    private static final long serialVersionUID = 2968072183596955597L;

    /**
     * Creates a KerberosException with an {@link ErrorType} and an
     * underlying {@link Throwable} that caused this fault.
     *
     * @param errorType The error type associated with this KerberosException.
     * @param cause The underlying failure, if any.
     */
    public KerberosException( ErrorType errorType, Throwable cause )
    {
        super( errorType.getMessage(), cause );
    }


    /**
     * Creates a KerberosException with an {@link ErrorType} and a custom error message.
     *
     * @param errorType The {@link ErrorType} associated with this KerberosException.
     * @param msg A custom error message for this KerberosException.
     */
    public KerberosException( ErrorType errorType, String msg )
    {
        super( msg );
    }
}
