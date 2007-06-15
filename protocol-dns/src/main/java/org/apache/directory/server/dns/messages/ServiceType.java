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
public enum ServiceType implements EnumConverter<Byte>
{
    /** Null */
    NULL(0),

    /** Remote job entry */
    RJE(5),

    /** Echo */
    ECHO(7),

    /** Discard */
    DISCARD(9),

    /** Active users */
    USERS(11),

    /** Daytime */
    DAYTIME(13),

    /** Quote of the day */
    QUOTE(17),

    /** Character generator */
    CHARGEN(19),

    /** File Transfer [Default Data] */
    FTP_DATA(20),

    /** File Transfer [Control] */
    FTP(21),

    /** Telnet */
    TELNET(23),

    /** Simple Mail Transfer */
    SMTP(25),

    /** NSW User System FE */
    NSW_FE(27),

    /** MSG ICP */
    MSG_ICP(29),

    /** MSG Authentication */
    MSG_AUTH(31),

    /** Display Support Protocol */
    DSP(33),

    /** Time */
    TIME(37),

    /** Resource Location Protocol */
    RLP(39),

    /** Graphics */
    GRAPHICS(41),

    /** Host Name Server */
    NAMESERVER(42),

    /** Who Is */
    NICKNAME(43),

    /** MPM FLAGS Protocol */
    MPM_FLAGS(44),

    /** Message Processing Module [recv] */
    MPM(45),

    /** MPM [default send] */
    MPM_SND(46),

    /** NI FTP */
    NI_FTP(47),

    /** Login Host Protocol */
    LOGIN(49),

    /** IMP Logical Address Maintenance */
    LA_MAINT(51),

    /** Domain Name Server */
    DOMAIN(53),

    /** ISI Graphics Language */
    ISI_GL(55),

    /** NI MAIL */
    NI_MAIL(61),

    /** VIA Systems - FTP */
    VIA_FTP(63),

    /** TACACS-Database Service */
    TACACS_DS(65),

    /** Bootstrap Protocol Server */
    BOOTPS(67),

    /** Bootstrap Protocol Client */
    BOOTPC(68),

    /** Trivial File Transfer */
    TFTP(69),

    /** Remote Job Service */
    NETRJS_1(71),

    /** Remote Job Service */
    NETRJS_2(72),

    /** Remote Job Service */
    NETRJS_3(73),

    /** Remote Job Service */
    NETRJS_4(74),

    /** Finger */
    FINGER(79),

    /** HOSTS2 Name Server */
    HOSTS2_NS(81),

    /** SU/MIT Telnet Gateway */
    SU_MIT_TG(89),

    /** MIT Dover Spooler */
    MIT_DOV(91),

    /** Device Control Protocol */
    DCP(93),

    /** SUPDUP */
    SUPDUP(95),

    /** Swift Remote Virtual File Protocol */
    SWIFT_RVF(97),

    /** TAC News */
    TACNEWS(98),

    /** Metagram Relay */
    METAGRAM(99),

    /** NIC Host Name Server */
    HOSTNAME(101),

    /** ISO-TSAP */
    ISO_TSAP(102),

    /** X400 */
    X400(103),

    /** X400-SND */
    X400_SND(104),

    /** Mailbox Name Nameserver */
    CSNET_NS(105),

    /** Remote Telnet Service */
    RTELNET(107),

    /** Post Office Protocol - Version 2 */
    POP_2(109),

    /** SUN Remote Procedure Call */
    SUNRPC(111),

    /** Authentication Service */
    AUTH(113),

    /** Simple File Transfer Protocol */
    SFTP(115),

    /** UUCP Path Service */
    UUCP_PATH(117),

    /** Network News Transfer Protocol */
    NNTP(119),

    /** HYDRA Expedited Remote Procedure */
    ERPC(121),

    /** Network Time Protocol */
    NTP(123),

    /** Locus PC-Interface Net Map Server */
    LOCUS_MAP(125),

    /** Locus PC-Interface Conn Server */
    LOCUS_CON(127),

    /** Password Generator Protocol */
    PWDGEN(129),

    /** CISCO FNATIVE */
    CISCO_FNA(130),

    /** CISCO TNATIVE */
    CISCO_TNA(131),

    /** CISCO SYSMAINT */
    CISCO_SYS(132),

    /** Statistics Service */
    STATSRV(133),

    /** INGRES-NET Service */
    INGRES_NET(134),

    /** Location Service */
    LOC_SRV(135),

    /** PROFILE Naming System */
    PROFILE(136),

    /** NETBIOS Name Service */
    NETBIOS_NS(137),

    /** NETBIOS Datagram Service */
    NETBIOS_DGM(138),

    /** NETBIOS Session Service */
    NETBIOS_SSN(139),

    /** EMFIS Data Service */
    EMFIS_DATA(140),

    /** EMFIS Control Service */
    EMFIS_CNTL(141),

    /** Britton-Lee IDM */
    BL_IDM(142),

    /** Survey Measurement */
    SUR_MEAS(243),

    /** LINK */
    LINK(245);

    private static ReverseEnumMap<Byte, ServiceType> map = new ReverseEnumMap<Byte, ServiceType>( ServiceType.class );

    private final byte value;


    private ServiceType( int value )
    {
        this.value = ( byte ) value;
    }


    public Byte convert()
    {
        return this.value;
    }


    /**
     * Converts an ordinal value into a {@link ServiceType}.
     *
     * @param value
     * @return The {@link ServiceType}.
     */
    public static ServiceType convert( byte value )
    {
        return map.get( value );
    }
}
