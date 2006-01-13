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
package org.apache.kerberos.messages.value;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class HostAddressType implements Comparable
{
    /**
     * Enumeration elements are constructed once upon class loading.
     * Order of appearance here determines the order of compareTo.
     */
    public static final HostAddressType NULL               = new HostAddressType(0, "null");
    public static final HostAddressType ADDRTYPE_UNIX      = new HostAddressType(1, "Unix");
    public static final HostAddressType ADDRTYPE_INET      = new HostAddressType(2, "Internet");
    public static final HostAddressType ADDRTYPE_IMPLINK   = new HostAddressType(3, "Arpanet");
    public static final HostAddressType ADDRTYPE_PUP       = new HostAddressType(4, "PUP");
    public static final HostAddressType ADDRTYPE_CHAOS     = new HostAddressType(5, "CHAOS");
    public static final HostAddressType ADDRTYPE_XNS       = new HostAddressType(6, "XEROX Network Services");
    public static final HostAddressType ADDRTYPE_IPX       = new HostAddressType(6, "IPX");
    public static final HostAddressType ADDRTYPE_OSI       = new HostAddressType(7, "OSI");
    public static final HostAddressType ADDRTYPE_ECMA      = new HostAddressType(8, "European Computer Manufacturers");
    public static final HostAddressType ADDRTYPE_DATAKIT   = new HostAddressType(9, "Datakit");
    public static final HostAddressType ADDRTYPE_CCITT     = new HostAddressType(10, "CCITT");
    public static final HostAddressType ADDRTYPE_SNA       = new HostAddressType(11, "SNA");
    public static final HostAddressType ADDRTYPE_DECNET    = new HostAddressType(12, "DECnet");
    public static final HostAddressType ADDRTYPE_DLI       = new HostAddressType(13, "Direct Data Link Interface");
    public static final HostAddressType ADDRTYPE_LAT       = new HostAddressType(14, "LAT");
    public static final HostAddressType ADDRTYPE_HYLINK    = new HostAddressType(15, "NSC Hyperchannel");
    public static final HostAddressType ADDRTYPE_APPLETALK = new HostAddressType(16, "AppleTalk");
    public static final HostAddressType ADDRTYPE_NETBIOS   = new HostAddressType(17, "NetBios");
    public static final HostAddressType ADDRTYPE_VOICEVIEW = new HostAddressType(18, "VoiceView");
    public static final HostAddressType ADDRTYPE_FIREFOX   = new HostAddressType(19, "Firefox");
    public static final HostAddressType ADDRTYPE_BAN       = new HostAddressType(21, "Banyan");
    public static final HostAddressType ADDRTYPE_ATM       = new HostAddressType(22, "ATM");
    public static final HostAddressType ADDRTYPE_INET6     = new HostAddressType(23, "Internet Protocol V6");

    /**
     * These two lines are all that's necessary to export a List of VALUES.
     */
    private static final HostAddressType[] values = { NULL, ADDRTYPE_UNIX, ADDRTYPE_INET,
            ADDRTYPE_IMPLINK, ADDRTYPE_PUP, ADDRTYPE_CHAOS, ADDRTYPE_XNS, ADDRTYPE_IPX,
            ADDRTYPE_OSI, ADDRTYPE_ECMA, ADDRTYPE_DATAKIT, ADDRTYPE_CCITT, ADDRTYPE_SNA,
            ADDRTYPE_DECNET, ADDRTYPE_DLI, ADDRTYPE_LAT, ADDRTYPE_HYLINK, ADDRTYPE_APPLETALK,
            ADDRTYPE_NETBIOS, ADDRTYPE_VOICEVIEW, ADDRTYPE_FIREFOX, ADDRTYPE_BAN, ADDRTYPE_ATM,
            ADDRTYPE_INET6 };

    public static final List VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    private final String name;
    private final int ordinal;

    /**
     * Private constructor prevents construction outside of this class.
     */
    private HostAddressType( int ordinal, String name )
    {
        this.ordinal = ordinal;
        this.name = name;
    }

    public String toString()
    {
        return name + " (" + ordinal + ")";
    }

    public int compareTo( Object that )
    {
        return ordinal - ( (HostAddressType) that ).ordinal;
    }

    public static HostAddressType getTypeByOrdinal( int type )
    {
        for ( int ii = 0; ii < values.length; ii++ )
        {
            if ( values[ ii ].ordinal == type )
            {
                return values[ ii ];
            }
        }

        return NULL;
    }

    public int getOrdinal()
    {
        return ordinal;
    }
}
