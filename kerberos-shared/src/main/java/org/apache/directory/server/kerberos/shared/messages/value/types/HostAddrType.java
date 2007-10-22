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
package org.apache.directory.server.kerberos.shared.messages.value.types;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 540371 $, $Date: 2007-05-22 02:00:43 +0200 (Tue, 22 May 2007) $
 */
public enum HostAddrType
{
    /**
     * Constant for the "null" host address type.
     */
    NULL( 0 ),

    /**
     * Constant for the "Unix" host address type.
     * 
     * Not in RFC
     */
    // ADDRTYPE_UNIX( 1 ),

    /**
     * Constant for the "Internet" host address type.
     */
    ADDRTYPE_INET( 2 ),

    /**
     * Constant for the "Arpanet" host address type.
     */
    ADDRTYPE_IMPLINK( 3 ),

    /**
     * Constant for the "PUP" host address type.
     * 
     * Not in RFC
     */
    //ADDRTYPE_PUP( 4 ),

    /**
     * Constant for the "CHAOS" host address type.
     */
    ADDRTYPE_CHAOS( 5 ),

    /**
     * Constant for the "XEROX Network Services" host address type.
     */
    ADDRTYPE_XNS( 6 ),

    /**
     * Constant for the "IPX" host address type.
     * 
     * Not in RFC
     */
    // ADDRTYPE_IPX( 6 ),

    /**
     * Constant for the "OSI" host address type.
     */
    ADDRTYPE_OSI( 7 ),

    /**
     * Constant for the "European Computer Manufacturers" host address type.
     * 
     * Not in RFC
     */
    //ADDRTYPE_ECMA( 8 ),

    /**
     * Constant for the "Datakit" host address type.
     * 
     * Not in RFC
     */
    //ADDRTYPE_DATAKIT( 9 ),

    /**
     * Constant for the "CCITT" host address type.
     * 
     * Not in RFC
     */
    //pADDRTYPE_CCITT( 10 ),

    /**
     * Constant for the "SNA" host address type.
     * 
     * Not in RFC
     */
    //ADDRTYPE_SNA( 11 ),

    /**
     * Constant for the "DECnet" host address type.
     */
    ADDRTYPE_DECNET( 12 ),

    /**
     * Constant for the "Direct Data Link Interface" host address type.
     * 
     * Not in RFC
     */
    //ADDRTYPE_DLI( 13 ),

    /**
     * Constant for the "LAT" host address type.
     * 
     * Not in RFC
     */
    //ADDRTYPE_LAT( 14 ),

    /**
     * Constant for the "NSC Hyperchannel" host address type.
     * 
     * Not in RFC
     */
    //ADDRTYPE_HYLINK( 15 ),

    /**
     * Constant for the "AppleTalk" host address type.
     */
    ADDRTYPE_APPLETALK( 16 ),

    /**
     * Constant for the "VoiceView" host address type.
     * 
     * Not in RFC
     */
    //ADDRTYPE_VOICEVIEW( 18 ),

    /**
     * Constant for the "Firefox" host address type.
     * 
     * Not in RFC
     */
    //ADDRTYPE_FIREFOX( 19 ),

    /**
     * Constant for the "NetBios" host address type.
     * 
     * Not in RFC
     */
    ADDRTYPE_NETBIOS( 20 ),

    /**
     * Constant for the "Banyan" host address type.
     * 
     * Not in RFC
     */
    //ADDRTYPE_BAN( 21 ),

    /**
     * Constant for the "ATM" host address type.
     * 
     * Not in RFC
     */
    //ADDRTYPE_ATM( 22 ),

    /**
     * Constant for the "Internet Protocol V6" host address type.
     */
    ADDRTYPE_INET6( 24 );

    /**
     * The value/code for the host address type.
     */
    private final int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private HostAddrType( int ordinal )
    {
        this.ordinal = ordinal;
    }


    /**
     * Returns the host address type when specified by its ordinal.
     *
     * @param type
     * @return The host address type.
     */
    public static HostAddrType getTypeByOrdinal( int type )
    {
        switch ( type )
        {
            case 0 : return NULL;
            //case 1 : return ADDRTYPE_UNIX;
            case 2 : return ADDRTYPE_INET;
            case 3 : return ADDRTYPE_IMPLINK;
            //case 4 : return ADDRTYPE_PUP;
            case 5 : return ADDRTYPE_CHAOS;
            case 6 : return ADDRTYPE_XNS;
            case 7 : return ADDRTYPE_OSI;
            //case 8 : return ADDRTYPE_ECMA;
            //case 9 : return ADDRTYPE_DATAKIT;
            //case 10 : return pADDRTYPE_CCITT;
            //case 11 : return ADDRTYPE_SNA;
            case 12 : return ADDRTYPE_DECNET;
            //case 13 : return ADDRTYPE_DLI;
            //case 14 : return ADDRTYPE_LAT;
            //case 15 : return ADDRTYPE_HYLINK;
            case 16 : return ADDRTYPE_APPLETALK;
            //case 18 : return ADDRTYPE_VOICEVIEW;
            //case 19 : return ADDRTYPE_FIREFOX;
            case 20 : return ADDRTYPE_NETBIOS;
            //case 21 : return ADDRTYPE_BAN;
            //case 22 : return ADDRTYPE_ATM;
            case 24 : return ADDRTYPE_INET6;
            default : return NULL;
        }
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

    public String toString()
    {
        switch ( ordinal )
        {
            //case 1 : return "Unix" + "(" + ordinal + ")"  ;
            case 2 : return "Internet" + "(" + ordinal + ")"  ;
            case 3 : return "Arpanet" + "(" + ordinal + ")"  ;
            //case 4 : return "PUP" + "(" + ordinal + ")"  ;
            case 5 : return "CHAOS" + "(" + ordinal + ")"  ;
            case 6 : return "XEROX Network Services" + "(" + ordinal + ")"  ;
            case 7 : return "OSI" + "(" + ordinal + ")"  ;
            //case 8 : return "European Computer Manufacturers" + "(" + ordinal + ")"  ;
            //case 9 : return "Datakit" + "(" + ordinal + ")"  ;
            //case 10 : return "CCITT" + "(" + ordinal + ")"  ;
            //case 11 : return "SNA" + "(" + ordinal + ")"  ;
            case 12 : return "DECnet" + "(" + ordinal + ")"  ;
            //case 13 : return "Direct Data Link Interface" + "(" + ordinal + ")"  ;
            //case 14 : return "LAT" + "(" + ordinal + ")"  ;
            //case 15 : return "NSC Hyperchannel" + "(" + ordinal + ")"  ;
            //case 16 : return "AppleTalk" + "(" + ordinal + ")"  ;
            //case 18 : return "VoiceView" + "(" + ordinal + ")"  ;
            //case 19 : return "Firefox" + "(" + ordinal + ")"  ;
            case 20 : return "NetBios" + "(" + ordinal + ")"  ;
            //case 21 : return "Banyan" + "(" + ordinal + ")"  ;
            //case 22 : return "ATM" + "(" + ordinal + ")"  ;
            case 24 : return "Internet Protocol V6" + "(" + ordinal + ")" ;             
            default : return "null" + "(" + ordinal + ")" ;
        }
    }
}
