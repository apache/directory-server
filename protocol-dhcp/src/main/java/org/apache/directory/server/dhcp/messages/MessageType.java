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

package org.apache.directory.server.dhcp.messages;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public final class MessageType implements Comparable
{
    /**
     * Enumeration elements are constructed once upon class loading.
     * Order of appearance here determines the order of compareTo.
     */
    public static final MessageType NULL = new MessageType( 0, "Null" );
    public static final MessageType DHCPDISCOVER = new MessageType( 1, "DHCP Discover" );
    public static final MessageType DHCPOFFER = new MessageType( 2, "DHCP Offer" );
    public static final MessageType DHCPREQUEST = new MessageType( 3, "DHCP Request" );
    public static final MessageType DHCPDECLINE = new MessageType( 4, "DHCP Decline" );
    public static final MessageType DHCPACK = new MessageType( 5, "DHCP Acknowledge" );
    public static final MessageType DHCPNAK = new MessageType( 6, "DHCP Not Acknowledge" );
    public static final MessageType DHCPRELEASE = new MessageType( 7, "DHCP Release" );
    public static final MessageType DHCPINFORM = new MessageType( 8, "DHCP Inform" );


    public String toString()
    {
        return name;
    }


    public int compareTo( Object that )
    {
        return ordinal - ( ( MessageType ) that ).ordinal;
    }


    public static MessageType getTypeByOrdinal( int type )
    {
        for ( int ii = 0; ii < values.length; ii++ )
            if ( values[ii].ordinal == type )
                return values[ii];
        return NULL;
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
    private MessageType(int ordinal, String name)
    {
        this.ordinal = ordinal;
        this.name = name;
    }

    /**
     * These two lines are all that's necessary to export a List of VALUES.
     */
    private static final MessageType[] values =
        { NULL, DHCPDISCOVER, DHCPOFFER, DHCPREQUEST, DHCPDECLINE, DHCPACK, DHCPNAK, DHCPRELEASE, DHCPINFORM };
    // VALUES needs to be located here, otherwise illegal forward reference
    public static final List VALUES = Collections.unmodifiableList( Arrays.asList( values ) );
}
