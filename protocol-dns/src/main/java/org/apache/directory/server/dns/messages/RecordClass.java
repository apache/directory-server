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

public final class RecordClass implements Comparable
{
    /**
     * Enumeration elements are constructed once upon class loading.
     * Order of appearance here determines the order of compareTo.
     */
    public static final RecordClass IN = new RecordClass( 1, "IN", "Internet" );
    public static final RecordClass CS = new RecordClass( 2, "CS", "CSNET class" );
    public static final RecordClass CH = new RecordClass( 3, "CH", "CHAOS class" );
    public static final RecordClass HS = new RecordClass( 4, "HS", "Hesiod [Dyer 87]" );
    public static final RecordClass NONE = new RecordClass( 254, "NONE", "Special value used in dynamic update messages" );
    public static final RecordClass ANY = new RecordClass( 255, "*", "Any class" );

    /**
     * These two lines are all that's necessary to export a List of VALUES.
     */
    private static final RecordClass[] values = { IN, CS, CH, HS, NONE, ANY };

    public static final List VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    private final String name;
    private final String code;
    private final int ordinal;

    /**
     * Private constructor prevents construction outside of this class.
     */
    private RecordClass( int ordinal, String code, String name )
    {
        this.ordinal = ordinal;
        this.code = code;
        this.name = name;
    }

    public String toString()
    {
        return name;
    }

    public int compareTo( Object that )
    {
        return ordinal - ( (RecordClass) that ).ordinal;
    }

    public static RecordClass getTypeByOrdinal( int type )
    {
        for ( int ii = 0; ii < values.length; ii++ )
        {
            if ( values[ ii ].ordinal == type )
            {
                return values[ ii ];
            }
        }

        return IN;
    }

    public static RecordClass getTypeByName( String type )
    {
        for ( int ii = 0; ii < values.length; ii++ )
        {
            if ( values[ ii ].code.equalsIgnoreCase( type ) )
            {
                return values[ ii ];
            }
        }

        return IN;
    }

    public int getOrdinal()
    {
        return ordinal;
    }

    public String getCode()
    {
        return code;
    }
}
