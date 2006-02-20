/*
 *   Copyright 2004 The Apache Software Foundation
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
package org.apache.directory.server.kerberos.shared.messages.value;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public final class LastRequestType implements Comparable
{
    /**
     * Enumeration elements are constructed once upon class loading.
     * Order of appearance here determines the order of compareTo.
     */
    public static final LastRequestType NONE = new LastRequestType( 0, "none" );
    public static final LastRequestType TIME_OF_INITIAL_TGT = new LastRequestType( 1, "time of initial ticket" );
    public static final LastRequestType TIME_OF_INITIAL_REQ = new LastRequestType( 2, "time of initial request" );
    public static final LastRequestType TIME_OF_NEWEST_TGT = new LastRequestType( 3, "time of newest ticket" );
    public static final LastRequestType TIME_OF_LAST_RENEWAL = new LastRequestType( 4, "time of last renewal" );
    public static final LastRequestType TIME_OF_LAST_REQ = new LastRequestType( 5, "time of last request" );
    public static final LastRequestType TIME_OF_PASSWORD_EXP = new LastRequestType( 6, "time of password expiration" );


    public String toString()
    {
        return name + " (" + ordinal + ")";
    }


    public int compareTo( Object that )
    {
        return ordinal - ( ( LastRequestType ) that ).ordinal;
    }


    public static LastRequestType getTypeByOrdinal( int type )
    {
        for ( int ii = 0; ii < values.length; ii++ )
        {
            if ( values[ii].ordinal == type )
            {
                return values[ii];
            }
        }

        return NONE;
    }


    public int getOrdinal()
    {
        return ordinal;
    }

    /// PRIVATE /////
    private final String name;
    private final int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private LastRequestType(int ordinal, String name)
    {
        this.ordinal = ordinal;
        this.name = name;
    }

    /**
     * These two lines are all that's necessary to export a List of VALUES.
     */
    private static final LastRequestType[] values =
        { NONE, TIME_OF_INITIAL_TGT, TIME_OF_INITIAL_REQ, TIME_OF_NEWEST_TGT, TIME_OF_LAST_RENEWAL, TIME_OF_LAST_REQ,
            TIME_OF_PASSWORD_EXP };
    // VALUES needs to be located here, otherwise illegal forward reference
    public static final List VALUES = Collections.unmodifiableList( Arrays.asList( values ) );
}
