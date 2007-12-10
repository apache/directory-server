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
 * Mode: This is a three-bit integer indicating the mode, with values
 * defined as follows:
 *
 *    Mode     Meaning
 *    ------------------------------------
 *    0        reserved
 *    1        symmetric active
 *    2        symmetric passive
 *    3        client
 *    4        server
 *    5        broadcast
 *    6        reserved for NTP control message
 *    7        reserved for private use
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public final class ModeType implements Comparable
{
    /**
     * Constant for the "Reserved mode" mode type.
     */
    public static final ModeType RESERVED = new ModeType( 0, "Reserved mode." );

    /**
     * Constant for the "Symmetric active mode" mode type.
     */
    public static final ModeType SYMMETRIC_ACTIVE = new ModeType( 1, "Symmetric active mode." );

    /**
     * Constant for the "Symmetric passive mode" mode type.
     */
    public static final ModeType RESERVED_PASSIVE = new ModeType( 2, "Symmetric passive mode." );

    /**
     * Constant for the "Client mode" mode type.
     */
    public static final ModeType CLIENT = new ModeType( 3, "Client mode." );

    /**
     * Constant for the "Server mode" mode type.
     */
    public static final ModeType SERVER = new ModeType( 4, "Server mode." );

    /**
     * Constant for the "Broadcast mode" mode type.
     */
    public static final ModeType BROADCAST = new ModeType( 5, "Broadcast mode." );

    /**
     * Constant for the "Reserved for NTP control message" mode type.
     */
    public static final ModeType RESERVED_FOR_NTP_CONTROL = new ModeType( 6, "Reserved for NTP control message." );

    /**
     * Constant for the "Reserved for private use" mode type.
     */
    public static final ModeType RESERVED_FOR_PRIVATE_USE = new ModeType( 7, "Reserved for private use." );

    /**
     * Array for building a List of VALUES.
     */
    private static final ModeType[] values =
        { RESERVED, SYMMETRIC_ACTIVE, RESERVED_PASSIVE, CLIENT, SERVER, BROADCAST, RESERVED_FOR_NTP_CONTROL,
            RESERVED_FOR_PRIVATE_USE };

    /**
     * A list of all the mode type constants.
     */
    public static final List VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    /**
     * The name of the mode type.
     */
    private final String name;

    /**
     * The value/code for the mode type.
     */
    private final int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private ModeType( int ordinal, String name )
    {
        this.ordinal = ordinal;
        this.name = name;
    }


    /**
     * Returns the mode type when specified by its ordinal.
     *
     * @param type
     * @return The mode type.
     */
    public static ModeType getTypeByOrdinal( int type )
    {
        for ( int ii = 0; ii < values.length; ii++ )
        {
            if ( values[ii].ordinal == type )
            {
                return values[ii];
            }
        }
        return SERVER;
    }


    /**
     * Returns the number associated with this mode type.
     *
     * @return The mode type ordinal.
     */
    public int getOrdinal()
    {
        return ordinal;
    }


    public int compareTo( Object that )
    {
        return ordinal - ( ( ModeType ) that ).ordinal;
    }


    public String toString()
    {
        return name;
    }
}
