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


import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public final class ServiceType implements Comparable
{
    /**
     * Enumeration elements are constructed once upon class loading.
     * Order of appearance here determines the order of compareTo.
     */
    public static final ServiceType NULL = new ServiceType( 0, "Null" );
    public static final ServiceType RJE = new ServiceType( 5, "Remote job entry." );
    public static final ServiceType ECHO = new ServiceType( 7, "Echo" );
    public static final ServiceType DISCARD = new ServiceType( 9, "Discard" );
    public static final ServiceType USERS = new ServiceType( 11, "Active users." );
    public static final ServiceType DAYTIME = new ServiceType( 13, "Daytime" );
    public static final ServiceType QUOTE = new ServiceType( 17, "Quote of the day." );
    public static final ServiceType CHARGEN = new ServiceType( 19, "Character generator." );
    public static final ServiceType FTP_DATA = new ServiceType( 20, "File Transfer [Default Data]" );
    public static final ServiceType FTP = new ServiceType( 21, "File Transfer [Control]" );
    public static final ServiceType TELNET = new ServiceType( 23, "Telnet" );
    public static final ServiceType SMTP = new ServiceType( 25, "Simple Mail Transfer" );
    public static final ServiceType NSW_FE = new ServiceType( 27, "NSW User System FE" );
    public static final ServiceType MSG_ICP = new ServiceType( 29, "MSG ICP" );
    public static final ServiceType MSG_AUTH = new ServiceType( 31, "MSG Authentication" );
    public static final ServiceType DSP = new ServiceType( 33, "Display Support Protocol" );
    public static final ServiceType TIME = new ServiceType( 37, "Time" );
    public static final ServiceType RLP = new ServiceType( 39, "Resource Location Protocol" );
    public static final ServiceType GRAPHICS = new ServiceType( 41, "Graphics" );
    public static final ServiceType NAMESERVER = new ServiceType( 42, "Host Name Server" );
    public static final ServiceType NICKNAME = new ServiceType( 43, "Who Is" );
    public static final ServiceType MPM_FLAGS = new ServiceType( 44, "MPM FLAGS Protocol" );
    public static final ServiceType MPM = new ServiceType( 45, "Message Processing Module [recv]" );
    public static final ServiceType MPM_SND = new ServiceType( 46, "MPM [default send]" );
    public static final ServiceType NI_FTP = new ServiceType( 47, "NI FTP" );
    public static final ServiceType LOGIN = new ServiceType( 49, "Login Host Protocol" );
    public static final ServiceType LA_MAINT = new ServiceType( 51, "IMP Logical Address Maintenance" );
    public static final ServiceType DOMAIN = new ServiceType( 53, "Domain Name Server" );
    public static final ServiceType ISI_GL = new ServiceType( 55, "ISI Graphics Language" );
    public static final ServiceType NI_MAIL = new ServiceType( 61, "NI MAIL" );
    public static final ServiceType VIA_FTP = new ServiceType( 63, "VIA Systems - FTP" );
    public static final ServiceType TACACS_DS = new ServiceType( 65, "TACACS-Database Service" );
    public static final ServiceType BOOTPS = new ServiceType( 67, "Bootstrap Protocol Server" );
    public static final ServiceType BOOTPC = new ServiceType( 68, "Bootstrap Protocol Client" );
    public static final ServiceType TFTP = new ServiceType( 69, "Trivial File Transfer" );
    public static final ServiceType NETRJS_1 = new ServiceType( 71, "Remote Job Service" );
    public static final ServiceType NETRJS_2 = new ServiceType( 72, "Remote Job Service" );
    public static final ServiceType NETRJS_3 = new ServiceType( 73, "Remote Job Service" );
    public static final ServiceType NETRJS_4 = new ServiceType( 74, "Remote Job Service" );
    public static final ServiceType FINGER = new ServiceType( 79, "Finger" );
    public static final ServiceType HOSTS2_NS = new ServiceType( 81, "HOSTS2 Name Server" );
    public static final ServiceType SU_MIT_TG = new ServiceType( 89, "SU/MIT Telnet Gateway" );
    public static final ServiceType MIT_DOV = new ServiceType( 91, "MIT Dover Spooler" );
    public static final ServiceType DCP = new ServiceType( 93, "Device Control Protocol" );
    public static final ServiceType SUPDUP = new ServiceType( 95, "SUPDUP" );
    public static final ServiceType SWIFT_RVF = new ServiceType( 97, "Swift Remote Virtual File Protocol" );
    public static final ServiceType TACNEWS = new ServiceType( 98, "TAC News" );
    public static final ServiceType METAGRAM = new ServiceType( 99, "Metagram Relay" );
    public static final ServiceType HOSTNAME = new ServiceType( 101, "NIC Host Name Server" );
    public static final ServiceType ISO_TSAP = new ServiceType( 102, "ISO-TSAP" );
    public static final ServiceType X400 = new ServiceType( 103, "X400" );
    public static final ServiceType X400_SND = new ServiceType( 104, "X400-SND" );
    public static final ServiceType CSNET_NS = new ServiceType( 105, "Mailbox Name Nameserver" );
    public static final ServiceType RTELNET = new ServiceType( 107, "Remote Telnet Service" );
    public static final ServiceType POP_2 = new ServiceType( 109, "Post Office Protocol - Version 2" );
    public static final ServiceType SUNRPC = new ServiceType( 111, "SUN Remote Procedure Call" );
    public static final ServiceType AUTH = new ServiceType( 113, "Authentication Service" );
    public static final ServiceType SFTP = new ServiceType( 115, "Simple File Transfer Protocol" );
    public static final ServiceType UUCP_PATH = new ServiceType( 117, "UUCP Path Service" );
    public static final ServiceType NNTP = new ServiceType( 119, "Network News Transfer Protocol" );
    public static final ServiceType ERPC = new ServiceType( 121, "HYDRA Expedited Remote Procedure" );
    public static final ServiceType NTP = new ServiceType( 123, "Network Time Protocol" );
    public static final ServiceType LOCUS_MAP = new ServiceType( 125, "Locus PC-Interface Net Map Server" );
    public static final ServiceType LOCUS_CON = new ServiceType( 127, "Locus PC-Interface Conn Server" );
    public static final ServiceType PWDGEN = new ServiceType( 129, "Password Generator Protocol" );
    public static final ServiceType CISCO_FNA = new ServiceType( 130, "CISCO FNATIVE" );
    public static final ServiceType CISCO_TNA = new ServiceType( 131, "CISCO TNATIVE" );
    public static final ServiceType CISCO_SYS = new ServiceType( 132, "CISCO SYSMAINT" );
    public static final ServiceType STATSRV = new ServiceType( 133, "Statistics Service" );
    public static final ServiceType INGRES_NET = new ServiceType( 134, "INGRES-NET Service" );
    public static final ServiceType LOC_SRV = new ServiceType( 135, "Location Service" );
    public static final ServiceType PROFILE = new ServiceType( 136, "PROFILE Naming System" );
    public static final ServiceType NETBIOS_NS = new ServiceType( 137, "NETBIOS Name Service" );
    public static final ServiceType NETBIOS_DGM = new ServiceType( 138, "NETBIOS Datagram Service" );
    public static final ServiceType NETBIOS_SSN = new ServiceType( 139, "NETBIOS Session Service" );
    public static final ServiceType EMFIS_DATA = new ServiceType( 140, "EMFIS Data Service" );
    public static final ServiceType EMFIS_CNTL = new ServiceType( 141, "EMFIS Control Service" );
    public static final ServiceType BL_IDM = new ServiceType( 142, "Britton-Lee IDM" );
    public static final ServiceType SUR_MEAS = new ServiceType( 243, "Survey Measurement" );
    public static final ServiceType LINK = new ServiceType( 245, "LINK" );

    /**
     * These two lines are all that's necessary to export a List of VALUES.
     */
    private static final ServiceType[] values =
        { NULL, RJE, ECHO, DISCARD, USERS, DAYTIME, QUOTE, CHARGEN, FTP_DATA, FTP, TELNET, SMTP, NSW_FE, MSG_ICP,
            MSG_AUTH, DSP, TIME, RLP, GRAPHICS, NAMESERVER, NICKNAME, MPM_FLAGS, MPM, MPM_SND, NI_FTP, LOGIN, LA_MAINT,
            DOMAIN, ISI_GL, NI_MAIL, VIA_FTP, TACACS_DS, BOOTPS, BOOTPC, TFTP, NETRJS_1, NETRJS_2, NETRJS_3, NETRJS_4,
            FINGER, HOSTS2_NS, SU_MIT_TG, MIT_DOV, DCP, SUPDUP, SWIFT_RVF, TACNEWS, METAGRAM, HOSTNAME, ISO_TSAP, X400,
            X400_SND, CSNET_NS, RTELNET, POP_2, SUNRPC, AUTH, SFTP, UUCP_PATH, NNTP, ERPC, NTP, LOCUS_MAP, LOCUS_CON,
            PWDGEN, CISCO_FNA, CISCO_TNA, CISCO_SYS, STATSRV, INGRES_NET, LOC_SRV, PROFILE, NETBIOS_NS, NETBIOS_DGM,
            NETBIOS_SSN, EMFIS_DATA, EMFIS_CNTL, BL_IDM, SUR_MEAS, LINK };

    public static final List VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    private final String name;
    private final int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private ServiceType(int ordinal, String name)
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
        return ordinal - ( ( ServiceType ) that ).ordinal;
    }


    public static ServiceType getTypeByOrdinal( int type )
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
