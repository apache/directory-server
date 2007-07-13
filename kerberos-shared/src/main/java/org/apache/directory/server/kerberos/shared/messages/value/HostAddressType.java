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
 * Type-safe enumerator for RFC 4120 section 7.5.3 "Address Types."
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public final class HostAddressType implements Comparable
{
    /**
     * Constant for the "IPv4" host address type.
     */
    public static final HostAddressType ADDRTYPE_IPV4 = new HostAddressType( 2, "IPv4" );

    /**
     * Constant for the "Directional" host address type.
     */
    public static final HostAddressType ADDRTYPE_DIRECTIONAL = new HostAddressType( 3, "Directional" );

    /**
     * Constant for the "ChaosNet" host address type.
     */
    public static final HostAddressType ADDRTYPE_CHAOSNET = new HostAddressType( 5, "ChaosNet" );

    /**
     * Constant for the "XEROX Network Services (XNS)" host address type.
     */
    public static final HostAddressType ADDRTYPE_XNS = new HostAddressType( 6, "XEROX Network Services (XNS)" );

    /**
     * Constant for the "ISO" host address type.
     */
    public static final HostAddressType ADDRTYPE_ISO = new HostAddressType( 7, "ISO" );

    /**
     * Constant for the "DECNET Phase IV" host address type.
     */
    public static final HostAddressType ADDRTYPE_DECNET = new HostAddressType( 12, "DECNET Phase IV" );

    /**
     * Constant for the "AppleTalk DDP" host address type.
     */
    public static final HostAddressType ADDRTYPE_APPLETALK = new HostAddressType( 16, "AppleTalk DDP" );

    /**
     * Constant for the "NetBios" host address type.
     */
    public static final HostAddressType ADDRTYPE_NETBIOS = new HostAddressType( 20, "NetBios" );

    /**
     * Constant for the "IPv6" host address type.
     */
    public static final HostAddressType ADDRTYPE_IPV6 = new HostAddressType( 24, "IPv6" );

    /**
     * Array for building a List of VALUES.
     */
    private static final HostAddressType[] values =
        { ADDRTYPE_IPV4, ADDRTYPE_DIRECTIONAL, ADDRTYPE_CHAOSNET, ADDRTYPE_XNS, ADDRTYPE_ISO, ADDRTYPE_DECNET,
            ADDRTYPE_APPLETALK, ADDRTYPE_NETBIOS, ADDRTYPE_IPV6 };

    /**
     * A List of all the host address type constants.
     */
    public static final List<HostAddressType> VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

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

        return ADDRTYPE_IPV4;
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
