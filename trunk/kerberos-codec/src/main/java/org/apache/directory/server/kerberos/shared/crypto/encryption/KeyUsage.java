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

import org.apache.directory.server.i18n.I18n;


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
 */
public final class KeyUsage implements Comparable<KeyUsage>
{
    /**
     * AS-REQ PA-ENC-TIMESTAMP padata timestamp, encrypted with the client key (Section 5.2.7.2)
     */
    public static final KeyUsage AS_REQ_PA_ENC_TIMESTAMP_WITH_CKEY = new KeyUsage( 1, I18n.err( I18n.ERR_603 ) );

    /**
     * AS-REP Ticket and TGS-REP Ticket (includes TGS session key or application session key), encrypted with the service key (Section 5.3)
     */
    public static final KeyUsage AS_OR_TGS_REP_TICKET_WITH_SRVKEY = new KeyUsage( 2, I18n.err( I18n.ERR_604 ) );

    /**
     * AS-REP encrypted part (includes TGS session key or application session key), encrypted with the client key (Section 5.4.2)
     */
    public static final KeyUsage AS_REP_ENC_PART_WITH_CKEY = new KeyUsage( 3, I18n.err( I18n.ERR_605 ) );

    /**
     * TGS-REQ KDC-REQ-BODY AuthorizationData, encrypted with the TGS session key (Section 5.4.1)
     */
    public static final KeyUsage TGS_REQ_KDC_REQ_BODY_AUTHZ_DATA_ENC_WITH_TGS_SESS_KEY = new KeyUsage( 4,
        I18n.err( I18n.ERR_606 ) );

    /**
     * TGS-REQ KDC-REQ-BODY AuthorizationData, encrypted with the TGS authenticator subkey (Section 5.4.1)
     */
    public static final KeyUsage TGS_REQ_KDC_REQ_BODY_AUTHZ_DATA_ENC_WITH_AUTHNT_SUB_KEY = new KeyUsage( 5,
        I18n.err( I18n.ERR_607 ) );

    /**
     * TGS-REQ PA-TGS-REQ padata AP-REQ Authenticator cksum, keyed with the TGS session key (Section 5.5.1)
     */
    public static final KeyUsage TGS_REQ_PA_TGS_REQ_PADATA_AP_REQ_AUTHNT_CKSUM_TGS_SESS_KEY = new KeyUsage( 6,
        I18n.err( I18n.ERR_608 ) );

    /**
     * TGS-REQ PA-TGS-REQ padata AP-REQ Authenticator (includes TGS authenticator subkey), encrypted with the TGS session key (Section 5.5.1)
     */
    public static final KeyUsage TGS_REQ_PA_TGS_REQ_PADATA_AP_REQ_TGS_SESS_KEY = new KeyUsage( 7,
        I18n.err( I18n.ERR_609 ) );

    /**
     * TGS-REP encrypted part (includes application session key), encrypted with the TGS session key (Section 5.4.2)
     */
    public static final KeyUsage TGS_REP_ENC_PART_TGS_SESS_KEY = new KeyUsage( 8, I18n.err( I18n.ERR_610 ) );

    /**
     * TGS-REP encrypted part (includes application session key), encrypted with the TGS authenticator subkey (Section 5.4.2)
     */
    public static final KeyUsage TGS_REP_ENC_PART_TGS_AUTHNT_SUB_KEY = new KeyUsage( 9, I18n.err( I18n.ERR_610 ) );

    /**
     * AP-REQ Authenticator cksum, keyed with the application session key (Section 5.5.1)
     */
    public static final KeyUsage AP_REQ_AUTHNT_CKSUM_SESS_KEY = new KeyUsage( 10, I18n.err( I18n.ERR_612 ) );

    /**
     * AP-REQ Authenticator (includes application authenticator subkey), encrypted with the application session key (Section 5.5.1)
     */
    public static final KeyUsage AP_REQ_AUTHNT_SESS_KEY = new KeyUsage( 11, I18n.err( I18n.ERR_613 ) );

    /**
     * AP-REP encrypted part (includes application session subkey), encrypted with the application session key (Section 5.5.2)
     */
    public static final KeyUsage AP_REP_ENC_PART_SESS_KEY = new KeyUsage( 12, I18n.err( I18n.ERR_614 ) );

    /**
     * KRB-PRIV encrypted part, encrypted with a key chosen by the application (Section 5.7.1)
     */
    public static final KeyUsage KRB_PRIV_ENC_PART_CHOSEN_KEY = new KeyUsage( 13, I18n.err( I18n.ERR_615 ) );

    /**
     * These two lines are all that's necessary to export a List of VALUES.
     */
    private static final KeyUsage[] values =
        {
            AS_REQ_PA_ENC_TIMESTAMP_WITH_CKEY,
            AS_OR_TGS_REP_TICKET_WITH_SRVKEY,
            AS_REP_ENC_PART_WITH_CKEY,
            TGS_REQ_KDC_REQ_BODY_AUTHZ_DATA_ENC_WITH_TGS_SESS_KEY,
            TGS_REQ_KDC_REQ_BODY_AUTHZ_DATA_ENC_WITH_AUTHNT_SUB_KEY,
            TGS_REQ_PA_TGS_REQ_PADATA_AP_REQ_AUTHNT_CKSUM_TGS_SESS_KEY,
            TGS_REQ_PA_TGS_REQ_PADATA_AP_REQ_TGS_SESS_KEY,
            TGS_REP_ENC_PART_TGS_SESS_KEY,
            TGS_REP_ENC_PART_TGS_AUTHNT_SUB_KEY,
            AP_REQ_AUTHNT_CKSUM_SESS_KEY,
            AP_REQ_AUTHNT_SESS_KEY,
            AP_REP_ENC_PART_SESS_KEY,
            KRB_PRIV_ENC_PART_CHOSEN_KEY };

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

        return AS_REQ_PA_ENC_TIMESTAMP_WITH_CKEY;
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
