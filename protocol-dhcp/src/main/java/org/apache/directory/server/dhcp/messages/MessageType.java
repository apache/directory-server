/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
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
 */
public final class MessageType implements Comparable
{
    // FIXME: does this class make a lot of sense in absence of real (1.5)
    // enums?
    // The DOCPDISCOVER et. al. constants can't be used conveniently in
    // switches,
    // therefore the byte constants fpr the opcodes are duplicated here.
    public static final byte CODE_DHCPINFORM = 8;

    public static final byte CODE_DHCPRELEASE = 7;

    public static final byte CODE_DHCPNAK = 6;

    public static final byte CODE_DHCPACK = 5;

    public static final byte CODE_DHCPDECLINE = 4;

    public static final byte CODE_DHCPREQUEST = 3;

    public static final byte CODE_DHCPOFFER = 2;

    public static final byte CODE_DHCPDISCOVER = 1;

    /**
     * Enumeration elements are constructed once upon class loading. Order of
     * appearance here determines the order of compareTo.
     */
    public static final MessageType DHCPDISCOVER = new MessageType( CODE_DHCPDISCOVER, "DHCP Discover" );

    public static final MessageType DHCPOFFER = new MessageType( CODE_DHCPOFFER, "DHCP Offer" );

    public static final MessageType DHCPREQUEST = new MessageType( CODE_DHCPREQUEST, "DHCP Request" );

    public static final MessageType DHCPDECLINE = new MessageType( CODE_DHCPDECLINE, "DHCP Decline" );

    public static final MessageType DHCPACK = new MessageType( CODE_DHCPACK, "DHCP Acknowledge" );

    public static final MessageType DHCPNAK = new MessageType( CODE_DHCPNAK, "DHCP Not Acknowledge" );

    public static final MessageType DHCPRELEASE = new MessageType( CODE_DHCPRELEASE, "DHCP Release" );

    public static final MessageType DHCPINFORM = new MessageType( CODE_DHCPINFORM, "DHCP Inform" );


    public String toString()
    {
        return name;
    }


    public int compareTo( Object that )
    {
        return ordinal - ( ( MessageType ) that ).ordinal;
    }


    public static MessageType getTypeByCode( byte type )
    {
        for ( int ii = 0; ii < values.length; ii++ )
            if ( values[ii].ordinal == type )
                return values[ii];
        return new MessageType( type, "Unrecognized" );
    }


    public byte getCode()
    {
        return ordinal;
    }

    // / PRIVATE /////
    private final String name;

    private final byte ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private MessageType(byte ordinal, String name)
    {
        this.ordinal = ordinal;
        this.name = name;
    }

    /**
     * These two lines are all that's necessary to export a List of VALUES.
     */
    private static final MessageType[] values =
        { DHCPDISCOVER, DHCPOFFER, DHCPREQUEST, DHCPDECLINE, DHCPACK, DHCPNAK, DHCPRELEASE, DHCPINFORM };

    // VALUES needs to be located here, otherwise illegal forward reference
    public static final List VALUES = Collections.unmodifiableList( Arrays.asList( values ) );
}
