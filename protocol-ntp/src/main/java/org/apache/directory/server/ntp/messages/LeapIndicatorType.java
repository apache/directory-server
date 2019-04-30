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
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum LeapIndicatorType
{
    /**
     * Constant for the "No leap second warning" leap indicator type.
     */
    NO_WARNING(0, "No leap second warning."),

    /**
     * Constant for the "Last minute has 61 seconds" leap indicator type.
     */
    POSITIVE_LEAP_SECOND(1, "Last minute has 61 seconds."),

    /**
     * Constant for the "Last minute has 59 seconds" leap indicator type.
     */
    NEGATIVE_LEAP_SECOND(2, "Last minute has 59 seconds."),

    /**
     * Constant for the "Alarm condition (clock not synchronized)" leap indicator type.
     */
    ALARM_CONDITION(3, "Alarm condition (clock not synchronized).");

    /**
     * The name of the leap indicator type.
     */
    private String name;

    /**
     * The value/code for the leap indicator type.
     */
    private int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    LeapIndicatorType( int ordinal, String name )
    {
        this.ordinal = ordinal;
        this.name = name;
    }


    /**
     * Returns the leap indicator type when specified by its ordinal.
     *
     * @param type
     * @return The leap indicator type.
     */
    public static LeapIndicatorType getTypeByOrdinal( int type )
    {
        for ( LeapIndicatorType lit : LeapIndicatorType.values() )
        {
            if ( type == lit.getOrdinal() )
            {
                return lit;
            }
        }

        return NO_WARNING;
    }


    /**
     * Returns the number associated with this leap indicator type.
     *
     * @return The leap indicator type ordinal.
     */
    public int getOrdinal()
    {
        return ordinal;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return name;
    }
}
