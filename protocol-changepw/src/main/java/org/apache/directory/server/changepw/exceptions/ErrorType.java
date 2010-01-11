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

package org.apache.directory.server.changepw.exceptions;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * Type safe enumeration of Change Password error types
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public final class ErrorType implements Comparable<ErrorType>
{
    /**
     * Constant for the "Request failed due to being malformed" error type.
     */
    public static final ErrorType KRB5_KPASSWD_MALFORMED = new ErrorType( 1, "Request failed due to being malformed." );

    /**
     * Constant for the "Request failed due to a hard error in processing the request" error type.
     */
    public static final ErrorType KRB5_KPASSWD_HARDERROR = new ErrorType( 2,
        "Request failed due to a hard error in processing the request." );

    /**
     * Constant for the "Request failed due to an error in authentication processing" error type.
     */
    public static final ErrorType KRB5_KPASSWD_AUTHERROR = new ErrorType( 3,
        "Request failed due to an error in authentication processing." );

    /**
     * Constant for the "Request failed due to a soft error in processing the request" error type.
     */
    public static final ErrorType KRB5_KPASSWD_SOFTERROR = new ErrorType( 4,
        "Request failed due to a soft error in processing the request." );

    /**
     * Constant for the "Requestor not authorized" error type.
     */
    public static final ErrorType KRB5_KPASSWD_ACCESSDENIED = new ErrorType( 5, "Requestor not authorized." );

    /**
     * Constant for the "Protocol version unsupported" error type.
     */
    public static final ErrorType KRB5_KPASSWD_BAD_VERSION = new ErrorType( 6, "Protocol version unsupported." );

    /**
     * Constant for the "Initial flag required" error type.
     */
    public static final ErrorType KRB5_KPASSWD_INITIAL_FLAG_NEEDED = new ErrorType( 7, "Initial flag required." );

    /**
     * Constant for the "Request failed for an unknown reason" error type.
     */
    public static final ErrorType KRB5_KPASSWD_UNKNOWN_ERROR = new ErrorType( 8,
        "Request failed for an unknown reason." );

    /**
     * Array for building a List of VALUES.
     */
    private static final ErrorType[] values =
        { KRB5_KPASSWD_MALFORMED, KRB5_KPASSWD_HARDERROR, KRB5_KPASSWD_AUTHERROR, KRB5_KPASSWD_SOFTERROR,
            KRB5_KPASSWD_ACCESSDENIED, KRB5_KPASSWD_BAD_VERSION, KRB5_KPASSWD_INITIAL_FLAG_NEEDED,
            KRB5_KPASSWD_UNKNOWN_ERROR };

    /**
     * A list of all the error type constants.
     */
    public static final List<ErrorType> VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    /**
     * The name of the error type.
     */
    private final String name;

    /**
     * The value/code for the error type.
     */
    private final int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private ErrorType( int ordinal, String name )
    {
        this.ordinal = ordinal;
        this.name = name;
    }


    /**
     * Returns the message for this Change Password error.
     *
     * @return the message for this Change Password error.
     */
    public String getMessage()
    {
        return name;
    }


    /**
     * Returns the message for this Change Password error.
     *
     * @return the message for this Change Password error.
     */
    public String toString()
    {
        return name;
    }


    /**
     * Compares this type to another object hopefully one that is of the same
     * type.
     *
     * @param that the object to compare this ErrorType to
     * @return ordinal - that.ordinal;
     */
    public int compareTo( ErrorType that )
    {
        return this.ordinal - that.ordinal;
    }


    /**
     * Gets the ordinal by its ordinal value.
     *
     * @param ordinal the ordinal value of the ordinal
     * @return the type corresponding to the ordinal value
     */
    public static ErrorType getTypeByOrdinal( int ordinal )
    {
        for ( int ii = 0; ii < values.length; ii++ )
        {
            if ( values[ii].ordinal == ordinal )
            {
                return values[ii];
            }
        }

        return KRB5_KPASSWD_UNKNOWN_ERROR;
    }


    /**
     * Gets the ordinal value associated with this Change Password error.
     *
     * @return the ordinal value associated with this Change Password error
     */
    public int getOrdinal()
    {
        return ordinal;
    }
}
