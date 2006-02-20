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

package org.apache.directory.server.ntp.messages;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * Leap Indicator (LI): This is a two-bit code warning of an impending
 * leap second to be inserted/deleted in the last minute of the current
 * day, with bit 0 and bit 1, respectively, coded as follows:
 *
 *    LI       Value     Meaning
 *    -------------------------------------------------------
 *    00       0         no warning
 *    01       1         last minute has 61 seconds
 *    10       2         last minute has 59 seconds)
 *    11       3         alarm condition (clock not synchronized)
 */
public final class LeapIndicatorType implements Comparable
{
    /**
     * Enumeration elements are constructed once upon class loading.
     * Order of appearance here determines the order of compareTo.
     */
    public static final LeapIndicatorType NO_WARNING = new LeapIndicatorType( 0, "No leap second warning." );
    public static final LeapIndicatorType POSITIVE_LEAP_SECOND = new LeapIndicatorType( 1,
        "Last minute has 61 seconds." );
    public static final LeapIndicatorType NEGATIVE_LEAP_SECOND = new LeapIndicatorType( 2,
        "Last minute has 59 seconds." );
    public static final LeapIndicatorType ALARM_CONDITION = new LeapIndicatorType( 3,
        "Alarm condition (clock not synchronized)." );

    /**
     * These two lines are all that's necessary to export a List of VALUES.
     */
    private static final LeapIndicatorType[] values =
        { NO_WARNING, POSITIVE_LEAP_SECOND, NEGATIVE_LEAP_SECOND, ALARM_CONDITION };
    // VALUES needs to be located here, otherwise illegal forward reference
    public static final List VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    private final String name;
    private final int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private LeapIndicatorType(int ordinal, String name)
    {
        this.ordinal = ordinal;
        this.name = name;
    }


    public static LeapIndicatorType getTypeByOrdinal( int type )
    {
        for ( int ii = 0; ii < values.length; ii++ )
        {
            if ( values[ii].ordinal == type )
            {
                return values[ii];
            }
        }

        return NO_WARNING;
    }


    public int getOrdinal()
    {
        return ordinal;
    }


    public int compareTo( Object that )
    {
        return ordinal - ( ( LeapIndicatorType ) that ).ordinal;
    }


    public String toString()
    {
        return name;
    }
}
