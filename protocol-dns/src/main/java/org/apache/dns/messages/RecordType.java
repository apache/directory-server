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

package org.apache.dns.messages;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RecordType implements Comparable
{
    /**
     * Enumeration elements are constructed once upon class loading.
     * Order of appearance here determines the order of compareTo.
     */
    public static final RecordType A = new RecordType( 1, "A", "Host address" );
    public static final RecordType NS = new RecordType( 2, "NS", "Authoritative name server" );
    public static final RecordType MD = new RecordType( 3, "MD", "Mail destination" );
    public static final RecordType MF = new RecordType( 4, "MF", "Mail forwarder" );
    public static final RecordType CNAME = new RecordType( 5, "CNAME", "Canonical name for an alias" );
    public static final RecordType SOA = new RecordType( 6, "SOA", "Start of a zone of authority" );
    public static final RecordType MB = new RecordType( 7, "MB", "Mailbox domain name" );
    public static final RecordType MG = new RecordType( 8, "MG", "Mail group member" );
    public static final RecordType MR = new RecordType( 9, "MR", "Mail rename domain name" );
    public static final RecordType NULL = new RecordType( 10, "NULL", "Null resource record" );
    public static final RecordType WKS = new RecordType( 11, "WKS", "Well known service description" );
    public static final RecordType PTR = new RecordType( 12, "PTR", "Domain name pointer" );
    public static final RecordType HINFO = new RecordType( 13, "HINFO", "Host information" );
    public static final RecordType MINFO = new RecordType( 14, "MINFO", "Mailbox or mail list information" );
    public static final RecordType MX = new RecordType( 15, "MX", "Mail exchange" );
    public static final RecordType TXT = new RecordType( 16, "TXT", "Text strings" );
    public static final RecordType RP = new RecordType( 17, "RP", "Responsible person" );
    public static final RecordType AFSDB = new RecordType( 18, "AFSDB", "AFS cell database" );
    public static final RecordType X25 = new RecordType( 19, "X25", "X.25 calling address" );
    public static final RecordType ISDN = new RecordType( 20, "ISDN", "ISDN calling address" );
    public static final RecordType RT = new RecordType( 21, "RT", "Router" );
    public static final RecordType NSAP = new RecordType( 22, "NSAP", "NSAP address" );
    public static final RecordType NSAP_PTR = new RecordType( 23, "NSAP_PTR", "Reverse NSAP address (deprecated)" );
    public static final RecordType SIG = new RecordType( 24, "SIG", "Signature" );
    public static final RecordType KEY = new RecordType( 25, "KEY", "Key" );
    public static final RecordType PX = new RecordType( 26, "PX", "X.400 mail mapping" );
    public static final RecordType GPOS = new RecordType( 27, "GPOS", "Geographical position (withdrawn)" );
    public static final RecordType AAAA = new RecordType( 28, "AAAA", "IPv6 address" );
    public static final RecordType LOC = new RecordType( 29, "LOC", "Location" );
    public static final RecordType NXT = new RecordType( 30, "NXT", "Next valid name in zone" );
    public static final RecordType EID = new RecordType( 31, "EID", "Endpoint identifier" );
    public static final RecordType NIMLOC = new RecordType( 32, "NIMLOC", "Nimrod locator" );
    public static final RecordType SRV = new RecordType( 33, "SRV", "Server selection" );
    public static final RecordType ATMA = new RecordType( 34, "ATMA", "ATM address" );
    public static final RecordType NAPTR = new RecordType( 35, "NAPTR", "Naming authority pointer" );
    public static final RecordType KX = new RecordType( 36, "KX", "Key exchange" );
    public static final RecordType CERT = new RecordType( 34, "CERT", "Certificate" );
    public static final RecordType A6 = new RecordType( 38, "A6", "IPv6 address (experimental)" );
    public static final RecordType DNAME = new RecordType( 39, "DNAME", "Non-terminal name redirection" );
    public static final RecordType OPT = new RecordType( 41, "OPT", "Options - contains EDNS metadata" );
    public static final RecordType APL = new RecordType( 42, "APL", "Address Prefix List" );
    public static final RecordType DS = new RecordType( 43, "DS", "Delegation Signer" );
    public static final RecordType SSHFP = new RecordType( 44, "SSHFP", "SSH Key Fingerprint" );
    public static final RecordType RRSIG = new RecordType( 46, "RRSIG", "Resource Record Signature" );
    public static final RecordType NSEC = new RecordType( 47, "NSEC", "Next Secure Name" );
    public static final RecordType DNSKEY = new RecordType( 48, "DNSKEY", "DNSSEC Key" );
    public static final RecordType TKEY = new RecordType( 249, "TKEY",
            "Transaction key - used to compute a shared secret or exchange a key" );
    public static final RecordType TSIG = new RecordType( 250, "TSIG", "Transaction signature" );
    public static final RecordType IXFR = new RecordType( 251, "IXFR", "Incremental zone transfer" );
    public static final RecordType AXFR = new RecordType( 252, "AXFR", "Request for transfer of an entire zone" );
    public static final RecordType MAILB = new RecordType( 253, "MAILB", "Request for mailbox-related records" );
    public static final RecordType MAILA = new RecordType( 254, "MAILA", "Request for mail agent resource records" );
    public static final RecordType ANY = new RecordType( 255, "ANY", "Request for all records" );

    /**
     * These two lines are all that's necessary to export a List of VALUES.
     */
    private static final RecordType[] values = { A, NS, MD, MF, CNAME, SOA, MB, MG, MR, NULL, WKS, PTR, HINFO, MINFO,
            MX, TXT, RP, AFSDB, X25, ISDN, RT, NSAP, NSAP_PTR, SIG, KEY, PX, GPOS, AAAA, LOC, NXT, EID, NIMLOC, SRV,
            ATMA, NAPTR, KX, CERT, A6, DNAME, OPT, APL, DS, SSHFP, RRSIG, NSEC, DNSKEY, TKEY, TSIG, IXFR, AXFR, MAILB,
            MAILA, ANY };

    public static final List VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    private final String name;
    private final String code;
    private final int ordinal;

    /**
     * Private constructor prevents construction outside of this class.
     */
    private RecordType( int ordinal, String code, String name )
    {
        this.ordinal = ordinal;
        this.code = code;
        this.name = name;
    }

    public String toString()
    {
        return name;
    }

    public int compareTo( Object that )
    {
        return ordinal - ( (RecordType) that ).ordinal;
    }

    public static RecordType getTypeByOrdinal( int type )
    {
        for ( int ii = 0; ii < values.length; ii++ )
        {
            if ( values[ ii ].ordinal == type )
            {
                return values[ ii ];
            }
        }

        return A;
    }

    public static RecordType getTypeByName( String type )
    {
        for ( int ii = 0; ii < values.length; ii++ )
        {
            if ( values[ ii ].code.equalsIgnoreCase( type ) )
            {
                return values[ ii ];
            }
        }

        return A;
    }

    public int getOrdinal()
    {
        return ordinal;
    }

    public String getCode()
    {
        return code;
    }

    public static boolean isResourceRecord( RecordType resourceType )
    {
        int type = resourceType.getOrdinal();

        switch ( type )
        {
            case 41:
            case 249:
            case 250:
            case 251:
            case 252:
            case 253:
            case 254:
            case 255:
                return false;
            default:
                return true;
        }
    }
}
