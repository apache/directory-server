/*
 *   Copyright 2005 The Apache Software Foundation
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
public final class ErrorType implements Comparable
{
    /*
     * Enumeration elements are constructed once upon class loading.
     * Order of appearance here determines the order of compareTo.
     */
    public static final ErrorType KRB5_KPASSWD_MALFORMED = new ErrorType( 1, "Request failed due to being malformed." );
    public static final ErrorType KRB5_KPASSWD_HARDERROR = new ErrorType( 2,
        "Request failed due to a hard error in processing the request." );
    public static final ErrorType KRB5_KPASSWD_AUTHERROR = new ErrorType( 3,
        "Request failed due to an error in authentication processing." );
    public static final ErrorType KRB5_KPASSWD_SOFTERROR = new ErrorType( 4,
        "Request failed due to a soft error in processing the request." );
    public static final ErrorType KRB5_KPASSWD_ACCESSDENIED = new ErrorType( 5, "Requestor not authorized." );
    public static final ErrorType KRB5_KPASSWD_BAD_VERSION = new ErrorType( 6, "Protocol version unsupported." );
    public static final ErrorType KRB5_KPASSWD_INITIAL_FLAG_NEEDED = new ErrorType( 7, "Initial flag required." );
    public static final ErrorType KRB5_KPASSWD_UNKNOWN_ERROR = new ErrorType( 8,
        "Request failed for an unknown reason." );

    /** Array for building a List of VALUES. */
    private static final ErrorType[] values =
        { KRB5_KPASSWD_MALFORMED, KRB5_KPASSWD_HARDERROR, KRB5_KPASSWD_AUTHERROR, KRB5_KPASSWD_SOFTERROR,
            KRB5_KPASSWD_ACCESSDENIED, KRB5_KPASSWD_BAD_VERSION, KRB5_KPASSWD_INITIAL_FLAG_NEEDED,
            KRB5_KPASSWD_UNKNOWN_ERROR };

    /** a list of all the error type constants */
    public static final List VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    /** the name of the error type */
    private final String name;

    /** the value/code for the error type */
    private final int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private ErrorType(int ordinal, String name)
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
     * @return ordinal - ( ( ErrorType ) that ).ordinal;
     */
    public int compareTo( Object that )
    {
        return this.ordinal - ( ( ErrorType ) that ).ordinal;
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
