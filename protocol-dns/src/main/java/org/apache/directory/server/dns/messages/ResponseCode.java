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

package org.apache.directory.server.dns.messages;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public final class ResponseCode implements Comparable
{
    /**
     * Enumeration elements are constructed once upon class loading.
     * Order of appearance here determines the order of compareTo.
     */
    public static final ResponseCode NO_ERROR = new ResponseCode( 0, "No error condition." );
    public static final ResponseCode FORMAT_ERROR = new ResponseCode( 1,
        "The name server was unable to interpret the query." );
    public static final ResponseCode SERVER_FAILURE = new ResponseCode( 2,
        "The name server was unable to process this query due to a problem with the name server." );
    public static final ResponseCode NAME_ERROR = new ResponseCode( 3,
        "The domain name referenced in the query does not exist." );
    public static final ResponseCode NOT_IMPLEMENTED = new ResponseCode( 4,
        "The name server does not support the requested kind of query." );
    public static final ResponseCode REFUSED = new ResponseCode( 5,
        "The name server refuses to perform the specified operation for policy reasons." );

    /**
     * These two lines are all that's necessary to export a List of VALUES.
     */
    private static final ResponseCode[] values =
        { NO_ERROR, FORMAT_ERROR, SERVER_FAILURE, NAME_ERROR, NOT_IMPLEMENTED, REFUSED };

    public static final List VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    private final String name;
    private final int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private ResponseCode(int ordinal, String name)
    {
        this.ordinal = ordinal;
        this.name = name;
    }


    public String getMessage()
    {
        return name;
    }


    public String toString()
    {
        return name;
    }


    public int compareTo( Object that )
    {
        return ordinal - ( ( ResponseCode ) that ).ordinal;
    }


    public static ResponseCode getTypeByOrdinal( int type )
    {
        for ( int ii = 0; ii < values.length; ii++ )
        {
            if ( values[ii].ordinal == type )
            {
                return values[ii];
            }
        }

        return NO_ERROR;
    }


    public int getOrdinal()
    {
        return ordinal;
    }
}
