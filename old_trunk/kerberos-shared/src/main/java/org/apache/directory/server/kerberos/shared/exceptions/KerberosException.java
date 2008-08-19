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
package org.apache.directory.server.kerberos.shared.exceptions;


/**
 * The root of the Kerberos exception hierarchy.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class KerberosException extends Exception
{
    private static final long serialVersionUID = 2968072183596955597L;

    /**
     * The Kerberos error code associated with this exception.
     */
    private final int errorCode;

    /**
     * Additional data about the error for use by the application
     * to help it recover from or handle the error.
     */
    private byte[] explanatoryData;


    /**
     * Creates a KerberosException with an {@link ErrorType}.
     *
     * @param errorType The error type associated with this KerberosException.
     */
    public KerberosException( ErrorType errorType )
    {
        super( errorType.getMessage() );

        this.errorCode = errorType.getOrdinal();
    }


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

        this.errorCode = errorType.getOrdinal();
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

        this.errorCode = errorType.getOrdinal();
    }


    /**
     * Creates a KerberosException with an {@link ErrorType}, a custom error message, and an
     * underlying {@link Throwable} that caused this fault.
     *
     * @param errorType The error type associated with this KerberosException.
     * @param msg A custom error message for this KerberosException.
     * @param cause The underlying failure, if any.
     */
    public KerberosException( ErrorType errorType, String msg, Throwable cause )
    {
        super( msg, cause );

        this.errorCode = errorType.getOrdinal();
    }


    /**
     * Creates a KerberosException with an {@link ErrorType} and data helping to
     * explain what caused this fault.
     *
     * @param errorType The error type associated with this KerberosException.
     * @param explanatoryData Data helping to explain this fault, if any.
     */
    public KerberosException( ErrorType errorType, byte[] explanatoryData )
    {
        super( errorType.getMessage() );

        this.errorCode = errorType.getOrdinal();
        this.explanatoryData = explanatoryData;
    }


    /**
     * Creates a KerberosException with an {@link ErrorType}, data helping to
     * explain what caused this fault, and an underlying {@link Throwable} that caused this fault.
     *
     * @param errorType The error type associated with this KerberosException.
     * @param explanatoryData Data helping to explain this fault, if any.
     * @param cause The underlying failure, if any.
     */
    public KerberosException( ErrorType errorType, byte[] explanatoryData, Throwable cause )
    {
        super( errorType.getMessage(), cause );

        this.errorCode = errorType.getOrdinal();
        this.explanatoryData = explanatoryData;
    }


    /**
     * Gets the protocol error code associated with this KerberosException.
     *
     * @return The error code associated with this KerberosException.
     */
    public int getErrorCode()
    {
        return this.errorCode;
    }


    /**
     * Gets the explanatory data associated with this KerberosException.
     *
     * @return The explanatory data associated with this KerberosException.
     */
    public byte[] getExplanatoryData()
    {
        return explanatoryData;
    }


    /**
     * Creates a KerberosException with an error code and a message.
     *
     * @param errorCode The error code associated with this KerberosException.
     * @param msg The standard Kerberos error message for this KerberosException.
     */
    protected KerberosException( int errorCode, String msg )
    {
        super( msg );

        this.errorCode = errorCode;
    }


    /**
     * Creates a KerberosException with an error code, a message and an
     * underlying {@link Throwable} that caused this fault.
     *
     * @param errorCode The error code associated with this KerberosException.
     * @param msg The standard Kerberos error message for this KerberosException.
     * @param cause The underlying failure, if any.
     */
    protected KerberosException( int errorCode, String msg, Throwable cause )
    {
        super( msg, cause );

        this.errorCode = errorCode;
    }


    /**
     * Creates a KerberosException with an error code, a message, and data
     * helping to explain what caused this fault.
     *
     * @param errorCode The error code associated with this KerberosException.
     * @param msg The standard Kerberos error message for this KerberosException.
     * @param explanatoryData Data helping to explain this fault, if any.
     */
    protected KerberosException( int errorCode, String msg, byte[] explanatoryData )
    {
        super( msg );

        this.errorCode = errorCode;
        this.explanatoryData = explanatoryData;
    }


    /**
     * Creates a KerberosException with an error code, a message, and data
     * helping to explain what caused this fault.
     *
     * @param errorCode The error code associated with this KerberosException.
     * @param msg The standard Kerberos error message for this KerberosException.
     * @param explanatoryData Data helping to explain this fault, if any.
     * @param cause The underlying failure, if any.
     */
    protected KerberosException( int errorCode, String msg, byte[] explanatoryData, Throwable cause )
    {
        super( msg, cause );

        this.errorCode = errorCode;
        this.explanatoryData = explanatoryData;
    }
}
