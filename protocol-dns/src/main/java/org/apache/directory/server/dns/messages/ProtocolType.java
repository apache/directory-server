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

package org.apache.directory.server.dns.messages;


import org.apache.directory.server.dns.util.EnumConverter;
import org.apache.directory.server.dns.util.ReverseEnumMap;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public enum ProtocolType implements EnumConverter<Byte>
{
    /** Null */
    NULL(0),

    /** Internet Conrol Message */
    ICMP(1),

    /** Internet Group Management */
    IGMP(2),

    /** Gateway-to-Gateway */
    GGP(3),

    /** Stream */
    ST(5),

    /** Transmission control */
    TCP(6),

    /** UCL */
    UCL(7),

    /** Exterior Gateway Protocol */
    EGP(8),

    /** any private interior gateway */
    IGP(9),

    /** BBN RCC Monitoring */
    BBN_RCC_MON(10),

    /** Network Voice Protocol */
    NVP_II(11),

    /** PUP */
    PUP(12),

    /** ARGUS */
    ARGUS(13),

    /** EMCON */
    EMCON(14),

    /** Cross Net Debugger */
    XNET(15),

    /** Chaos */
    CHAOS(16),

    /** User Datagram */
    UDP(17),

    /** Multiplexing */
    MUX(18),

    /** DCN Measurement Subsystems */
    DCN_MEAS(19),

    /** Host Monitoring */
    HMP(20),

    /** Packet Radio Measurement */
    PRM(21),

    /** XEROX NS IDP */
    XNS_IDP(22),

    /** Trunk-1 */
    TRUNK_1(23),

    /** Trunk-2 */
    TRUNK_2(24),

    /** Leaf-1 */
    LEAF_1(25),

    /** Leaf-2 */
    LEAF_2(26),

    /** Reliable Data Protocol */
    RDP(27),

    /** Internet Reliable Transaction */
    IRTP(28),

    /** ISO Transport Protocol Class 4 */
    ISO_TP4(29),

    /** Bulk Data Transfer Protocol */
    NETBLT(30),

    /** MFE Network Services Protocol */
    MFE_NSP(31),

    /** MERIT Internodal Protocol */
    MERIT_INP(32),

    /** Sequential Exchange Protocol */
    SEP(33),

    /** CFTP */
    CFTP(62),

    /** SATNET and Backroom EXPAK */
    SAT_EXPAK(64),

    /** MIT Subnet Support */
    MIT_SUBNET(65),

    /** MIT Remote Virtual Disk Protocol */
    RVD(66),

    /** Internet Pluribus Packet Core */
    IPPC(67),

    /** SATNET Monitoring */
    SAT_MON(69),

    /** Internet Packet Core Utility */
    IPCV(71),

    /** Backroom SETNET Monitoring */
    BR_SAT_MON(76),

    /** WIDEBAND Monitoring */
    WB_MON(78),

    /** WIDEBAND EXPAK */
    WB_EXPAK(79);

    private static ReverseEnumMap<Byte, ProtocolType> map = new ReverseEnumMap<Byte, ProtocolType>( ProtocolType.class );

    private final byte value;


    private ProtocolType( int value )
    {
        this.value = ( byte ) value;
    }


    public Byte convert()
    {
        return this.value;
    }


    public static ProtocolType convert( byte value )
    {
        return map.get( value );
    }
}
