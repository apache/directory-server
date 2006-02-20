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

package org.apache.directory.server.dns.messages;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public final class ProtocolType implements Comparable
{
    /**
     * Enumeration elements are constructed once upon class loading.
     * Order of appearance here determines the order of compareTo.
     */
    public static final ProtocolType NULL = new ProtocolType( 0, "Null" );
    public static final ProtocolType ICMP = new ProtocolType( 1, "Internet Control Message" );
    public static final ProtocolType IGMP = new ProtocolType( 2, "Internet Group Management" );
    public static final ProtocolType GGP = new ProtocolType( 3, "Gateway-to-Gateway" );
    public static final ProtocolType ST = new ProtocolType( 5, "Stream" );
    public static final ProtocolType TCP = new ProtocolType( 6, "Transmission Control" );
    public static final ProtocolType UCL = new ProtocolType( 7, "UCL" );
    public static final ProtocolType EGP = new ProtocolType( 8, "Exterior Gateway Protocol" );
    public static final ProtocolType IGP = new ProtocolType( 9, "any private interior gateway" );
    public static final ProtocolType BBN_RCC_MON = new ProtocolType( 10, "BBN RCC Monitoring" );
    public static final ProtocolType NVP_II = new ProtocolType( 11, "Network Voice Protocol" );
    public static final ProtocolType PUP = new ProtocolType( 12, "PUP" );
    public static final ProtocolType ARGUS = new ProtocolType( 13, "ARGUS" );
    public static final ProtocolType EMCON = new ProtocolType( 14, "EMCON" );
    public static final ProtocolType XNET = new ProtocolType( 15, "Cross Net Debugger" );
    public static final ProtocolType CHAOS = new ProtocolType( 16, "Chaos" );
    public static final ProtocolType UDP = new ProtocolType( 17, "User Datagram" );
    public static final ProtocolType MUX = new ProtocolType( 18, "Multiplexing" );
    public static final ProtocolType DCN_MEAS = new ProtocolType( 19, "DCN Measurement Subsystems" );
    public static final ProtocolType HMP = new ProtocolType( 20, "Host Monitoring" );
    public static final ProtocolType PRM = new ProtocolType( 21, "Packet Radio Measurement" );
    public static final ProtocolType XNS_IDP = new ProtocolType( 22, "XEROX NS IDP" );
    public static final ProtocolType TRUNK_1 = new ProtocolType( 23, "Trunk-1" );
    public static final ProtocolType TRUNK_2 = new ProtocolType( 24, "Trunk-2" );
    public static final ProtocolType LEAF_1 = new ProtocolType( 25, "Leaf-1" );
    public static final ProtocolType LEAF_2 = new ProtocolType( 26, "Leaf-2" );
    public static final ProtocolType RDP = new ProtocolType( 27, "Reliable Data Protocol" );
    public static final ProtocolType IRTP = new ProtocolType( 28, "Internet Reliable Transaction" );
    public static final ProtocolType ISO_TP4 = new ProtocolType( 29, "ISO Transport Protocol Class 4" );
    public static final ProtocolType NETBLT = new ProtocolType( 30, "Bulk Data Transfer Protocol" );
    public static final ProtocolType MFE_NSP = new ProtocolType( 31, "MFE Network Services Protocol" );
    public static final ProtocolType MERIT_INP = new ProtocolType( 32, "MERIT Internodal Protocol" );
    public static final ProtocolType SEP = new ProtocolType( 33, "Sequential Exchange Protocol" );
    public static final ProtocolType CFTP = new ProtocolType( 62, "CFTP" );
    public static final ProtocolType SAT_EXPAK = new ProtocolType( 64, "SATNET and Backroom EXPAK" );
    public static final ProtocolType MIT_SUBNET = new ProtocolType( 65, "MIT Subnet Support" );
    public static final ProtocolType RVD = new ProtocolType( 66, "MIT Remote Virtual Disk Protocol" );
    public static final ProtocolType IPPC = new ProtocolType( 67, "Internet Pluribus Packet Core" );
    public static final ProtocolType SAT_MON = new ProtocolType( 69, "SATNET Monitoring" );
    public static final ProtocolType IPCV = new ProtocolType( 71, "Internet Packet Core Utility" );
    public static final ProtocolType BR_SAT_MON = new ProtocolType( 76, "Backroom SATNET Monitoring" );
    public static final ProtocolType WB_MON = new ProtocolType( 78, "WIDEBAND Monitoring" );
    public static final ProtocolType WB_EXPAK = new ProtocolType( 79, "WIDEBAND EXPAK" );

    /**
     * These two lines are all that's necessary to export a List of VALUES.
     */
    private static final ProtocolType[] values =
        { NULL, ICMP, IGMP, GGP, ST, TCP, UCL, EGP, IGP, BBN_RCC_MON, NVP_II, PUP, ARGUS, EMCON, XNET, CHAOS, UDP, MUX,
            DCN_MEAS, HMP, PRM, XNS_IDP, TRUNK_1, TRUNK_2, LEAF_1, LEAF_2, RDP, IRTP, ISO_TP4, NETBLT, MFE_NSP,
            MERIT_INP, SEP, CFTP, SAT_EXPAK, MIT_SUBNET, RVD, IPPC, SAT_MON, IPCV, BR_SAT_MON, WB_MON, WB_EXPAK };

    public static final List VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    private final String name;
    private final int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private ProtocolType(int ordinal, String name)
    {
        this.ordinal = ordinal;
        this.name = name;
    }


    public String toString()
    {
        return name;
    }


    public int compareTo( Object that )
    {
        return ordinal - ( ( ProtocolType ) that ).ordinal;
    }


    public static ProtocolType getTypeByOrdinal( int type )
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


    public int getOrdinal()
    {
        return ordinal;
    }
}
