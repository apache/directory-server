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


/**
 * Type safe enumeration of Change Password error types
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum ErrorType
{
    // TODO Add i18n. Don't no if these error messages are also a response to the client.
    // If so shall they really be i18n?

    /**
     * Constant for the "Request failed due to being malformed" error type.
     */
    KRB5_KPASSWD_MALFORMED(1, "Request failed due to being malformed."),

    /**
     * Constant for the "Request failed due to a hard error in processing the request" error type.
     */
    KRB5_KPASSWD_HARDERROR(2, "Request failed due to a hard error in processing the request."),

    /**
     * Constant for the "Request failed due to an error in authentication processing" error type.
     */
    KRB5_KPASSWD_AUTHERROR(3, "Request failed due to an error in authentication processing."),

    /**
     * Constant for the "Request failed due to a soft error in processing the request" error type.
     */
    KRB5_KPASSWD_SOFTERROR(4, "Request failed due to a soft error in processing the request."),

    /**
     * Constant for the "Requestor not authorized" error type.
     */
    KRB5_KPASSWD_ACCESSDENIED(5, "Requestor not authorized."),

    /**
     * Constant for the "Protocol version unsupported" error type.
     */
    KRB5_KPASSWD_BAD_VERSION(6, "Protocol version unsupported."),

    /**
     * Constant for the "Initial flag required" error type.
     */
    KRB5_KPASSWD_INITIAL_FLAG_NEEDED(7, "Initial flag required."),

    /**
     * Constant for the "Request failed for an unknown reason" error type.
     */
    KRB5_KPASSWD_UNKNOWN_ERROR(8, "Request failed for an unknown reason.");

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
     * Gets the ordinal by its ordinal value.
     *
     * @param ordinal the ordinal value of the ordinal
     * @return the type corresponding to the ordinal value
     */
    public static ErrorType getTypeByOrdinal( int ordinal )
    {
        for ( ErrorType et : ErrorType.values() )
        {
            if ( ordinal == et.getOrdinal() )
            {
                return et;
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
