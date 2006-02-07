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

package org.apache.ntp.messages;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Stratum: This is a eight-bit unsigned integer indicating the stratum
 * level of the local clock, with values defined as follows:
 *
 *    Stratum  Meaning
 *    ----------------------------------------------
 *    0        unspecified or unavailable
 *    1        primary reference (e.g., radio clock)
 *    2-15     secondary reference (via NTP or SNTP)
 *    16-255   reserved
 */
public final class StratumType implements Comparable
{
	/**
	 * Enumeration elements are constructed once upon class loading.
	 * Order of appearance here determines the order of compareTo.
	 */
	public static final StratumType UNSPECIFIED         = new StratumType(0, "Unspecified or unavailable.");
	public static final StratumType PRIMARY_REFERENCE   = new StratumType(1, "Primary reference.");
	public static final StratumType SECONDARY_REFERENCE = new StratumType(2, "Secondary reference.");

    /**
     * These two lines are all that's necessary to export a List of VALUES.
     */
    private static final StratumType[] values = { UNSPECIFIED, PRIMARY_REFERENCE, SECONDARY_REFERENCE };
    // VALUES needs to be located here, otherwise illegal forward reference
    public static final List VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    private final int ordinal;
    private final String name;

    /**
     * Private constructor prevents construction outside of this class.
     */
    private StratumType( int ordinal, String name )
    {
        this.ordinal = ordinal;
        this.name = name;
    }

    public static StratumType getTypeByOrdinal( int type )
    {
        for ( int ii = 0; ii < values.length; ii++ )
        {
            if ( values[ ii ].ordinal == type )
            {
                return values[ ii ];
            }
        }

        return UNSPECIFIED;
    }

    public int getOrdinal()
    {
        return ordinal;
    }

    public int compareTo( Object that )
    {
        return ordinal - ( (StratumType) that ).ordinal;
    }

    public String toString()
    {
        return name;
    }
}
