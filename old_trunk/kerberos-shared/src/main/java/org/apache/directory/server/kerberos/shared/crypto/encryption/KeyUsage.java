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
package org.apache.directory.server.kerberos.shared.crypto.encryption;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * From RFC 4120, "The Kerberos Network Authentication Service (V5)":
 * 
 * 7.5.1.  Key Usage Numbers
 * 
 * The encryption and checksum specifications in [RFC3961] require as
 * input a "key usage number", to alter the encryption key used in any
 * specific message in order to make certain types of cryptographic
 * attack more difficult.  These are the key usage values assigned in
 * [RFC 4120]:
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public final class KeyUsage implements Comparable<KeyUsage>
{
    /**
     * AS-REQ PA-ENC-TIMESTAMP padata timestamp, encrypted with the client key (Section 5.2.7.2)
     */
    public static final KeyUsage NUMBER1 = new KeyUsage( 1,
        "AS-REQ PA-ENC-TIMESTAMP padata timestamp, encrypted with the client key" );

    /**
     * AS-REP Ticket and TGS-REP Ticket (includes TGS session key or application session key), encrypted with the service key (Section 5.3)
     */
    public static final KeyUsage NUMBER2 = new KeyUsage(
        2,
        "AS-REP Ticket and TGS-REP Ticket (includes TGS session key or application session key), encrypted with the service key" );

    /**
     * AS-REP encrypted part (includes TGS session key or application session key), encrypted with the client key (Section 5.4.2)
     */
    public static final KeyUsage NUMBER3 = new KeyUsage( 3,
        "AS-REP encrypted part (includes TGS session key or application session key), encrypted with the client key" );

    /**
     * TGS-REQ KDC-REQ-BODY AuthorizationData, encrypted with the TGS session key (Section 5.4.1)
     */
    public static final KeyUsage NUMBER4 = new KeyUsage( 4,
        "TGS-REQ KDC-REQ-BODY AuthorizationData, encrypted with the TGS session key" );

    /**
     * TGS-REQ KDC-REQ-BODY AuthorizationData, encrypted with the TGS authenticator subkey (Section 5.4.1)
     */
    public static final KeyUsage NUMBER5 = new KeyUsage( 5,
        "TGS-REQ KDC-REQ-BODY AuthorizationData, encrypted with the TGS authenticator subkey" );

    /**
     * TGS-REQ PA-TGS-REQ padata AP-REQ Authenticator cksum, keyed with the TGS session key (Section 5.5.1)
     */
    public static final KeyUsage NUMBER6 = new KeyUsage( 6,
        "TGS-REQ PA-TGS-REQ padata AP-REQ Authenticator cksum, keyed with the TGS session key" );

    /**
     * TGS-REQ PA-TGS-REQ padata AP-REQ Authenticator (includes TGS authenticator subkey), encrypted with the TGS session key (Section 5.5.1)
     */
    public static final KeyUsage NUMBER7 = new KeyUsage(
        7,
        "TGS-REQ PA-TGS-REQ padata AP-REQ Authenticator (includes TGS authenticator subkey), encrypted with the TGS session key" );

    /**
     * TGS-REP encrypted part (includes application session key), encrypted with the TGS session key (Section 5.4.2)
     */
    public static final KeyUsage NUMBER8 = new KeyUsage( 8,
        "TGS-REP encrypted part (includes application session key), encrypted with the TGS session key" );

    /**
     * TGS-REP encrypted part (includes application session key), encrypted with the TGS authenticator subkey (Section 5.4.2)
     */
    public static final KeyUsage NUMBER9 = new KeyUsage( 9,
        "TGS-REP encrypted part (includes application session key), encrypted with the TGS authenticator subkey" );

    /**
     * AP-REQ Authenticator cksum, keyed with the application session key (Section 5.5.1)
     */
    public static final KeyUsage NUMBER10 = new KeyUsage( 10,
        "AP-REQ Authenticator cksum, keyed with the application session key" );

    /**
     * AP-REQ Authenticator (includes application authenticator subkey), encrypted with the application session key (Section 5.5.1)
     */
    public static final KeyUsage NUMBER11 = new KeyUsage( 11,
        "AP-REQ Authenticator (includes application authenticator subkey), encrypted with the application session key" );

    /**
     * AP-REP encrypted part (includes application session subkey), encrypted with the application session key (Section 5.5.2)
     */
    public static final KeyUsage NUMBER12 = new KeyUsage( 12,
        "AP-REP encrypted part (includes application session subkey), encrypted with the application session key" );

    /**
     * KRB-PRIV encrypted part, encrypted with a key chosen by the application (Section 5.7.1)
     */
    public static final KeyUsage NUMBER13 = new KeyUsage( 13,
        "KRB-PRIV encrypted part, encrypted with a key chosen by the application" );

    /**
     * These two lines are all that's necessary to export a List of VALUES.
     */
    private static final KeyUsage[] values =
        { NUMBER1, NUMBER2, NUMBER3, NUMBER4, NUMBER5, NUMBER6, NUMBER7, NUMBER8, NUMBER9, NUMBER10, NUMBER11,
            NUMBER12, NUMBER13 };

    /**
     * VALUES needs to be located here, otherwise illegal forward reference.
     */
    public static final List<KeyUsage> VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    private final int ordinal;
    private final String name;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private KeyUsage( int ordinal, String name )
    {
        this.ordinal = ordinal;
        this.name = name;
    }


    /**
     * Returns the key usage number type when specified by its ordinal.
     *
     * @param type
     * @return The key usage number type.
     */
    public static KeyUsage getTypeByOrdinal( int type )
    {
        for ( int ii = 0; ii < values.length; ii++ )
        {
            if ( values[ii].ordinal == type )
            {
                return values[ii];
            }
        }

        return NUMBER1;
    }


    /**
     * Returns the number associated with this key usage number.
     *
     * @return The key usage number
     */
    public int getOrdinal()
    {
        return ordinal;
    }


    public int compareTo( KeyUsage that )
    {
        return ordinal - that.ordinal;
    }


    public String toString()
    {
        return name + " (" + ordinal + ")";
    }
}
