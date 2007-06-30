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

package org.apache.directory.server.dhcp.messages;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public final class MessageType implements Comparable
{
    /**
     * Constant for the "Null" message type.
     */
    public static final MessageType NULL = new MessageType( 0, "Null" );

    /**
     * Constant for the "DHCP Discover" message type.
     */
    public static final MessageType DHCPDISCOVER = new MessageType( 1, "DHCP Discover" );

    /**
     * Constant for the "DHCP Offer" message type.
     */
    public static final MessageType DHCPOFFER = new MessageType( 2, "DHCP Offer" );

    /**
     * Constant for the "DHCP Request" message type.
     */
    public static final MessageType DHCPREQUEST = new MessageType( 3, "DHCP Request" );

    /**
     * Constant for the "DHCP Decline" message type.
     */
    public static final MessageType DHCPDECLINE = new MessageType( 4, "DHCP Decline" );

    /**
     * Constant for the "DHCP Acknowledge" message type.
     */
    public static final MessageType DHCPACK = new MessageType( 5, "DHCP Acknowledge" );

    /**
     * Constant for the "DHCP Not Acknowledge" message type.
     */
    public static final MessageType DHCPNAK = new MessageType( 6, "DHCP Not Acknowledge" );

    /**
     * Constant for the "DHCP Release" message type.
     */
    public static final MessageType DHCPRELEASE = new MessageType( 7, "DHCP Release" );

    /**
     * Constant for the "DHCP Inform" message type.
     */
    public static final MessageType DHCPINFORM = new MessageType( 8, "DHCP Inform" );

    /**
     * Array for building a List of VALUES.
     */
    private static final MessageType[] values =
        { NULL, DHCPDISCOVER, DHCPOFFER, DHCPREQUEST, DHCPDECLINE, DHCPACK, DHCPNAK, DHCPRELEASE, DHCPINFORM };

    /**
     * A list of all the message type constants.
     */
    public static final List VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    /**
     * The name of the message type.
     */
    private final String name;

    /**
     * The value/code for the message type.
     */
    private final int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private MessageType( int ordinal, String name )
    {
        this.ordinal = ordinal;
        this.name = name;
    }


    /**
     * Returns the message type when specified by its ordinal.
     *
     * @param type
     * @return The message type.
     */
    public static MessageType getTypeByOrdinal( int type )
    {
        for ( int ii = 0; ii < values.length; ii++ )
            if ( values[ii].ordinal == type )
                return values[ii];
        return NULL;
    }


    /**
     * Returns the number associated with this message type.
     *
     * @return The message type ordinal.
     */
    public int getOrdinal()
    {
        return ordinal;
    }


    public int compareTo( Object that )
    {
        return ordinal - ( ( MessageType ) that ).ordinal;
    }


    public String toString()
    {
        return name;
    }
}
