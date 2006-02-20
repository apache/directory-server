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


public final class OpCode implements Comparable
{
    /**
     * Enumeration elements are constructed once upon class loading.
     * Order of appearance here determines the order of compareTo.
     */
    public static final OpCode QUERY = new OpCode( 0, "Standard query" );
    public static final OpCode IQUERY = new OpCode( 1, "Inverse query" );
    public static final OpCode STATUS = new OpCode( 2, "Server status request" );
    public static final OpCode NOTIFY = new OpCode( 4, "Zone transfer notification" );
    public static final OpCode UPDATE = new OpCode( 5, "Dynamic update message" );

    /**
     * These two lines are all that's necessary to export a List of VALUES.
     */
    private static final OpCode[] values =
        { QUERY, IQUERY, STATUS, NOTIFY, UPDATE };

    public static final List VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    private final String name;
    private final int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private OpCode(int ordinal, String name)
    {
        this.ordinal = ordinal;
        this.name = name;
    }


    public String toString()
    {
        return name;
    }


    public int compareTo( Object that )
    {
        return ordinal - ( ( OpCode ) that ).ordinal;
    }


    public static OpCode getTypeByOrdinal( int type )
    {
        for ( int ii = 0; ii < values.length; ii++ )
        {
            if ( values[ii].ordinal == type )
            {
                return values[ii];
            }
        }

        return QUERY;
    }


    public int getOrdinal()
    {
        return ordinal;
    }
}
