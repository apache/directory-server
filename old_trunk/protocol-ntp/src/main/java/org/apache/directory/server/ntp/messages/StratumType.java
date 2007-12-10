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

package org.apache.directory.server.ntp.messages;


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
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public final class StratumType implements Comparable
{
    /**
     * Constant for the "Unspecified or unavailable" stratum type.
     */
    public static final StratumType UNSPECIFIED = new StratumType( 0, "Unspecified or unavailable." );

    /**
     * Constant for the "Primary reference" stratum type.
     */
    public static final StratumType PRIMARY_REFERENCE = new StratumType( 1, "Primary reference." );

    /**
     * Constant for the "Secondary reference" stratum type.
     */
    public static final StratumType SECONDARY_REFERENCE = new StratumType( 2, "Secondary reference." );

    /**
     * Array for building a List of VALUES.
     */
    private static final StratumType[] values =
        { UNSPECIFIED, PRIMARY_REFERENCE, SECONDARY_REFERENCE };

    /**
     * A list of all the stratum type constants.
     */
    public static final List VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    /**
     * The name of the stratum type.
     */
    private final String name;

    /**
     * The value/code for the stratum type.
     */
    private final int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private StratumType( int ordinal, String name )
    {
        this.ordinal = ordinal;
        this.name = name;
    }


    /**
     * Returns the stratum type when specified by its ordinal.
     *
     * @param type
     * @return The stratum type.
     */
    public static StratumType getTypeByOrdinal( int type )
    {
        for ( int ii = 0; ii < values.length; ii++ )
        {
            if ( values[ii].ordinal == type )
            {
                return values[ii];
            }
        }

        return UNSPECIFIED;
    }


    /**
     * Returns the number associated with this stratum type.
     *
     * @return The stratum type ordinal.
     */
    public int getOrdinal()
    {
        return ordinal;
    }


    public int compareTo( Object that )
    {
        return ordinal - ( ( StratumType ) that ).ordinal;
    }


    public String toString()
    {
        return name;
    }
}
