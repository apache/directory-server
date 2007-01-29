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
public enum RecordType implements EnumConverter<Short>
{
    /** Host address */
    A(1),

    /** Authoritative name server */
    NS(2),

    /** Mail destination */
    MD(3),

    /** Mail forwarder */
    MF(4),

    /** Canonical name for an alias */
    CNAME(5),

    /** Start of a zone of authority */
    SOA(6),

    /** Mailbox domain name */
    MB(7),

    /** Mail group member */
    MG(8),

    /** Mail rename domain name */
    MR(9),

    /** Null resource record */
    NULL(10),

    /** Well know service description */
    WKS(11),

    /** Domain name pointer */
    PTR(12),

    /** Host information */
    HINFO(13),

    /** Mailbox or mail list information */
    MINFO(14),

    /** Mail exchange */
    MX(15),

    /** Text strings */
    TXT(16),

    /** Responsible person */
    RP(17),

    /** AFS cell database */
    AFSDB(18),

    /** X.25 calling address */
    X25(19),

    /** ISDN calling address */
    ISDN(20),

    /** Router */
    RT(21),

    /** NSAP address */
    NSAP(22),

    /** Reverse NSAP address (deprecated) */
    NSAP_PTR(23),

    /** Signature */
    SIG(24),

    /** Key */
    KEY(25),

    /** X.400 mail mapping */
    PX(26),

    /** Geographical position (withdrawn) */
    GPOS(27),

    /** IPv6 address */
    AAAA(28),

    /** Location */
    LOC(29),

    /** Next valid name in zone */
    NXT(30),

    /** Endpoint identifier */
    EID(31),

    /** Nimrod locator */
    NIMLOC(32),

    /** Server selection */
    SRV(33),

    /** ATM address */
    ATMA(34),

    /** Naming authority pointer */
    NAPTR(35),

    /** Key exchange */
    KX(36),

    /** Certificate */
    CERT(34),

    /** IPv6 address (experimental) */
    A6(38),

    /** Non-terminal name redirection */
    DNAME(39),

    /** Options - contains EDNS metadata */
    OPT(41),

    /** Address Prefix List */
    APL(42),

    /** Delegation Signer */
    DS(43),

    /** SSH Key Fingerprint */
    SSHFP(44),

    /** Resource Record Signature */
    RRSIG(46),

    /** Next Secure Name */
    NSEC(47),

    /** DNSSEC Key */
    DNSKEY(48),

    /** Transaction key - used to compute a shared secret or exchange a key */
    TKEY(249),

    /** Transaction signature */
    TSIG(250),

    /** Incremental zone transfer */
    IXFR(251),

    /** Request for transfer of an entire zone */
    AXFR(252),

    /** Request for mailbox-related records */
    MAILB(253),

    /** Request for mail agent resource records */
    MAILA(254),

    /** Request for all records */
    ANY(255);

    private static ReverseEnumMap<Short, RecordType> map = new ReverseEnumMap<Short, RecordType>( RecordType.class );

    private final short value;


    private RecordType( int value )
    {
        this.value = ( short ) value;
    }


    public Short convert()
    {
        return this.value;
    }


    public static RecordType convert( short value )
    {
        return map.get( value );
    }


    public static boolean isResourceRecord( RecordType resourceType )
    {
        switch ( resourceType )
        {
            case OPT:
            case TKEY:
            case TSIG:
            case IXFR:
            case AXFR:
            case MAILB:
            case MAILA:
            case ANY:
                return false;
            default:
                return true;
        }
    }
}
