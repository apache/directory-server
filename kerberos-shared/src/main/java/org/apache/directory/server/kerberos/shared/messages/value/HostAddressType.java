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
package org.apache.directory.server.kerberos.shared.messages.value;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public final class HostAddressType implements Comparable
{
    /**
     * Constant for the "null" host address type.
     */
    public static final HostAddressType NULL = new HostAddressType( 0, "null" );

    /**
     * Constant for the "Unix" host address type.
     */
    public static final HostAddressType ADDRTYPE_UNIX = new HostAddressType( 1, "Unix" );

    /**
     * Constant for the "Internet" host address type.
     */
    public static final HostAddressType ADDRTYPE_INET = new HostAddressType( 2, "Internet" );

    /**
     * Constant for the "Arpanet" host address type.
     */
    public static final HostAddressType ADDRTYPE_IMPLINK = new HostAddressType( 3, "Arpanet" );

    /**
     * Constant for the "PUP" host address type.
     */
    public static final HostAddressType ADDRTYPE_PUP = new HostAddressType( 4, "PUP" );

    /**
     * Constant for the "CHAOS" host address type.
     */
    public static final HostAddressType ADDRTYPE_CHAOS = new HostAddressType( 5, "CHAOS" );

    /**
     * Constant for the "XEROX Network Services" host address type.
     */
    public static final HostAddressType ADDRTYPE_XNS = new HostAddressType( 6, "XEROX Network Services" );

    /**
     * Constant for the "IPX" host address type.
     */
    public static final HostAddressType ADDRTYPE_IPX = new HostAddressType( 6, "IPX" );

    /**
     * Constant for the "OSI" host address type.
     */
    public static final HostAddressType ADDRTYPE_OSI = new HostAddressType( 7, "OSI" );

    /**
     * Constant for the "European Computer Manufacturers" host address type.
     */
    public static final HostAddressType ADDRTYPE_ECMA = new HostAddressType( 8, "European Computer Manufacturers" );

    /**
     * Constant for the "Datakit" host address type.
     */
    public static final HostAddressType ADDRTYPE_DATAKIT = new HostAddressType( 9, "Datakit" );

    /**
     * Constant for the "CCITT" host address type.
     */
    public static final HostAddressType ADDRTYPE_CCITT = new HostAddressType( 10, "CCITT" );

    /**
     * Constant for the "SNA" host address type.
     */
    public static final HostAddressType ADDRTYPE_SNA = new HostAddressType( 11, "SNA" );

    /**
     * Constant for the "DECnet" host address type.
     */
    public static final HostAddressType ADDRTYPE_DECNET = new HostAddressType( 12, "DECnet" );

    /**
     * Constant for the "Direct Data Link Interface" host address type.
     */
    public static final HostAddressType ADDRTYPE_DLI = new HostAddressType( 13, "Direct Data Link Interface" );

    /**
     * Constant for the "LAT" host address type.
     */
    public static final HostAddressType ADDRTYPE_LAT = new HostAddressType( 14, "LAT" );

    /**
     * Constant for the "NSC Hyperchannel" host address type.
     */
    public static final HostAddressType ADDRTYPE_HYLINK = new HostAddressType( 15, "NSC Hyperchannel" );

    /**
     * Constant for the "AppleTalk" host address type.
     */
    public static final HostAddressType ADDRTYPE_APPLETALK = new HostAddressType( 16, "AppleTalk" );

    /**
     * Constant for the "NetBios" host address type.
     */
    public static final HostAddressType ADDRTYPE_NETBIOS = new HostAddressType( 17, "NetBios" );

    /**
     * Constant for the "VoiceView" host address type.
     */
    public static final HostAddressType ADDRTYPE_VOICEVIEW = new HostAddressType( 18, "VoiceView" );

    /**
     * Constant for the "Firefox" host address type.
     */
    public static final HostAddressType ADDRTYPE_FIREFOX = new HostAddressType( 19, "Firefox" );

    /**
     * Constant for the "Banyan" host address type.
     */
    public static final HostAddressType ADDRTYPE_BAN = new HostAddressType( 21, "Banyan" );

    /**
     * Constant for the "ATM" host address type.
     */
    public static final HostAddressType ADDRTYPE_ATM = new HostAddressType( 22, "ATM" );

    /**
     * Constant for the "Internet Protocol V6" host address type.
     */
    public static final HostAddressType ADDRTYPE_INET6 = new HostAddressType( 23, "Internet Protocol V6" );

    /**
     * Array for building a List of VALUES.
     */
    private static final HostAddressType[] values =
        { NULL, ADDRTYPE_UNIX, ADDRTYPE_INET, ADDRTYPE_IMPLINK, ADDRTYPE_PUP, ADDRTYPE_CHAOS, ADDRTYPE_XNS,
            ADDRTYPE_IPX, ADDRTYPE_OSI, ADDRTYPE_ECMA, ADDRTYPE_DATAKIT, ADDRTYPE_CCITT, ADDRTYPE_SNA, ADDRTYPE_DECNET,
            ADDRTYPE_DLI, ADDRTYPE_LAT, ADDRTYPE_HYLINK, ADDRTYPE_APPLETALK, ADDRTYPE_NETBIOS, ADDRTYPE_VOICEVIEW,
            ADDRTYPE_FIREFOX, ADDRTYPE_BAN, ADDRTYPE_ATM, ADDRTYPE_INET6 };

    /**
     * A List of all the host address type constants.
     */
    public static final List VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    /**
     * The name of the host address type.
     */
    private final String name;

    /**
     * The value/code for the host address type.
     */
    private final int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private HostAddressType( int ordinal, String name )
    {
        this.ordinal = ordinal;
        this.name = name;
    }


    /**
     * Returns the host address type when specified by its ordinal.
     *
     * @param type
     * @return The host address type.
     */
    public static HostAddressType getTypeByOrdinal( int type )
    {
        for ( int ii = 0; ii < values.length; ii++ )
        {
            if ( values[ii].ordinal == type )
            {
                return values[ii];
            }
        }

        return NULL;
    }


    /**
     * Returns the number associated with this host address type.
     *
     * @return The host address type ordinal.
     */
    public int getOrdinal()
    {
        return ordinal;
    }


    public int compareTo( Object that )
    {
        return ordinal - ( ( HostAddressType ) that ).ordinal;
    }


    public String toString()
    {
        return name + " (" + ordinal + ")";
    }
}
