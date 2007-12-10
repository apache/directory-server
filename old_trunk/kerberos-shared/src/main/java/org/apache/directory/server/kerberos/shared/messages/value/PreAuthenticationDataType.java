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
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class PreAuthenticationDataType implements Comparable<PreAuthenticationDataType>
{
    /**
     * Constant for the "null" pre-authentication data type.
     */
    public static final PreAuthenticationDataType NULL = new PreAuthenticationDataType( 0, "null" );

    /**
     * Constant for the "TGS request" pre-authentication data type.
     */
    public static final PreAuthenticationDataType PA_TGS_REQ = new PreAuthenticationDataType( 1, "TGS request." );

    /**
     * Constant for the "encrypted timestamp" pre-authentication data type.
     */
    public static final PreAuthenticationDataType PA_ENC_TIMESTAMP = new PreAuthenticationDataType( 2,
        "Encrypted timestamp." );

    /**
     * Constant for the "password salt" pre-authentication data type.
     */
    public static final PreAuthenticationDataType PA_PW_SALT = new PreAuthenticationDataType( 3, "password salt" );

    /**
     * Constant for the "enc unix time" pre-authentication data type.
     */
    public static final PreAuthenticationDataType PA_ENC_UNIX_TIME = new PreAuthenticationDataType( 5, "enc unix time" );

    /**
     * Constant for the "sandia secureid" pre-authentication data type.
     */
    public static final PreAuthenticationDataType PA_SANDIA_SECUREID = new PreAuthenticationDataType( 6,
        "sandia secureid" );

    /**
     * Constant for the "sesame" pre-authentication data type.
     */
    public static final PreAuthenticationDataType PA_SESAME = new PreAuthenticationDataType( 7, "sesame" );

    /**
     * Constant for the "OSF DCE" pre-authentication data type.
     */
    public static final PreAuthenticationDataType PA_OSF_DCE = new PreAuthenticationDataType( 8, "OSF DCE" );

    /**
     * Constant for the "cybersafe secureid" pre-authentication data type.
     */
    public static final PreAuthenticationDataType PA_CYBERSAFE_SECUREID = new PreAuthenticationDataType( 9,
        "cybersafe secureid" );

    /**
     * Constant for the "ASF3 salt" pre-authentication data type.
     */
    public static final PreAuthenticationDataType PA_ASF3_SALT = new PreAuthenticationDataType( 10, "ASF3 salt" );

    /**
     * Constant for the "encryption info" pre-authentication data type.
     */
    public static final PreAuthenticationDataType PA_ETYPE_INFO = new PreAuthenticationDataType( 11, "Encryption info." );

    /**
     * Constant for the "SAM challenge" pre-authentication data type.
     */
    public static final PreAuthenticationDataType PA_SAM_CHALLENGE = new PreAuthenticationDataType( 12,
        "SAM challenge." );

    /**
     * Constant for the "SAM response" pre-authentication data type.
     */
    public static final PreAuthenticationDataType PA_SAM_RESPONSE = new PreAuthenticationDataType( 13, "SAM response." );

    /**
     * Constant for the "Old PK AS request" pre-authentication data type.
     */
    public static final PreAuthenticationDataType PA_PK_AS_REQ_OLD = new PreAuthenticationDataType( 14,
        "Old PK AS request." );

    /**
     * Constant for the "Old PK AS reply" pre-authentication data type.
     */
    public static final PreAuthenticationDataType PA_PK_AS_REP_OLD = new PreAuthenticationDataType( 15,
        "Old PK AS reply." );

    /**
     * Constant for the "PK AS request" pre-authentication data type.
     */
    public static final PreAuthenticationDataType PA_PK_AS_REQ = new PreAuthenticationDataType( 16, "PK AS request." );

    /**
     * Constant for the "PK AS reply" pre-authentication data type.
     */
    public static final PreAuthenticationDataType PA_PK_AS_REP = new PreAuthenticationDataType( 17, "PK AS reply." );

    /**
     * Constant for the "Encryption info 2" pre-authentication data type.
     */
    public static final PreAuthenticationDataType PA_ETYPE_INFO2 = new PreAuthenticationDataType( 19,
        "Encryption info 2." );

    /**
     * Constant for the "use specified key version" pre-authentication data type.
     */
    public static final PreAuthenticationDataType PA_USE_SPECIFIED_KVNO = new PreAuthenticationDataType( 20,
        "use specified key version" );

    /**
     * Constant for the "SAM redirect" pre-authentication data type.
     */
    public static final PreAuthenticationDataType PA_SAM_REDIRECT = new PreAuthenticationDataType( 21, "SAM redirect." );

    /**
     * Constant for the "Embedded in typed data" pre-authentication data type.
     */
    public static final PreAuthenticationDataType PA_GET_FROM_TYPED_DATA = new PreAuthenticationDataType( 22,
        "Embedded in typed data." );

    /**
     * Constant for the "Embeds padata" pre-authentication data type.
     */
    public static final PreAuthenticationDataType TD_PADATA = new PreAuthenticationDataType( 22, "Embeds padata." );

    /**
     * Constant for the "SAM encryption info" pre-authentication data type.
     */
    public static final PreAuthenticationDataType PA_SAM_ETYPE_INFO = new PreAuthenticationDataType( 23,
        "SAM encryption info." );

    /**
     * Constant for the "Alternate principal" pre-authentication data type.
     */
    public static final PreAuthenticationDataType PA_ALT_PRINC = new PreAuthenticationDataType( 24,
        "Alternate principal." );

    /**
     * Constant for the "SAM challenge 2" pre-authentication data type.
     */
    public static final PreAuthenticationDataType PA_SAM_CHALLENGE2 = new PreAuthenticationDataType( 30,
        "SAM challenge 2." );

    /**
     * Constant for the "SAM response 2" pre-authentication data type.
     */
    public static final PreAuthenticationDataType PA_SAM_RESPONSE2 = new PreAuthenticationDataType( 31,
        "SAM response 2." );

    /**
     * Constant for the "Reserved extra TGT" pre-authentication data type.
     */
    public static final PreAuthenticationDataType PA_EXTRA_TGT = new PreAuthenticationDataType( 41,
        "Reserved extra TGT." );

    /**
     * Constant for the "CertificateSet from CMS" pre-authentication data type.
     */
    public static final PreAuthenticationDataType TD_PKINIT_CMS_CERTIFICATES = new PreAuthenticationDataType( 101,
        "CertificateSet from CMS." );

    /**
     * Constant for the "PrincipalName" pre-authentication data type.
     */
    public static final PreAuthenticationDataType TD_KRB_PRINCIPAL = new PreAuthenticationDataType( 102,
        "PrincipalName." );

    /**
     * Constant for the "Realm" pre-authentication data type.
     */
    public static final PreAuthenticationDataType TD_KRB_REALM = new PreAuthenticationDataType( 103, "Realm." );

    /**
     * Constant for the "Trusted certifiers" pre-authentication data type.
     */
    public static final PreAuthenticationDataType TD_TRUSTED_CERTIFIERS = new PreAuthenticationDataType( 104,
        "Trusted certifiers." );

    /**
     * Constant for the "Certificate index" pre-authentication data type.
     */
    public static final PreAuthenticationDataType TD_CERTIFICATE_INDEX = new PreAuthenticationDataType( 105,
        "Certificate index." );

    /**
     * Constant for the "application specific" pre-authentication data type.
     */
    public static final PreAuthenticationDataType TD_APP_DEFINED_ERROR = new PreAuthenticationDataType( 106,
        "application specific." );

    /**
     * Constant for the "Request nonce" pre-authentication data type.
     */
    public static final PreAuthenticationDataType TD_REQ_NONCE = new PreAuthenticationDataType( 107, "Request nonce." );

    /**
     * Constant for the "Request sequence number" pre-authentication data type.
     */
    public static final PreAuthenticationDataType TD_REQ_SEQ = new PreAuthenticationDataType( 108,
        "Request sequence number." );

    /**
     * Constant for the "PAC request" pre-authentication data type.
     */
    public static final PreAuthenticationDataType PA_PAC_REQUEST = new PreAuthenticationDataType( 128, "PAC request." );

    /**
     * Array for building a List of VALUES.
     */
    private static final PreAuthenticationDataType[] values =
        { NULL, PA_TGS_REQ, PA_ENC_TIMESTAMP, PA_PW_SALT, PA_ENC_UNIX_TIME, PA_SANDIA_SECUREID, PA_SESAME, PA_OSF_DCE,
            PA_CYBERSAFE_SECUREID, PA_ASF3_SALT, PA_ETYPE_INFO, PA_SAM_CHALLENGE, PA_SAM_RESPONSE, PA_PK_AS_REQ_OLD,
            PA_PK_AS_REP_OLD, PA_PK_AS_REQ, PA_PK_AS_REP, PA_ETYPE_INFO2, PA_USE_SPECIFIED_KVNO, PA_SAM_REDIRECT,
            PA_GET_FROM_TYPED_DATA, TD_PADATA, PA_SAM_ETYPE_INFO, PA_ALT_PRINC, PA_SAM_CHALLENGE2, PA_SAM_RESPONSE2,
            PA_EXTRA_TGT, TD_PKINIT_CMS_CERTIFICATES, TD_KRB_PRINCIPAL, TD_KRB_REALM, TD_TRUSTED_CERTIFIERS,
            TD_CERTIFICATE_INDEX, TD_APP_DEFINED_ERROR, TD_REQ_NONCE, TD_REQ_SEQ, PA_PAC_REQUEST };

    /**
     * A list of all the pre-authentication type constants.
     */
    public static final List<PreAuthenticationDataType> VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    /**
     * The name of the pre-authentication type.
     */
    private final String name;

    /**
     * The value/code for the pre-authentication type.
     */
    private final int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private PreAuthenticationDataType( int ordinal, String name )
    {
        this.ordinal = ordinal;
        this.name = name;
    }


    /**
     * Returns the pre-authentication type when specified by its ordinal.
     *
     * @param type
     * @return The pre-authentication type.
     */
    public static PreAuthenticationDataType getTypeByOrdinal( int type )
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


    /**
     * Returns the number associated with this pre-authentication type.
     *
     * @return The pre-authentication type ordinal.
     */
    public int getOrdinal()
    {
        return ordinal;
    }


    public int compareTo( PreAuthenticationDataType that )
    {
        return ordinal - that.ordinal;
    }


    public String toString()
    {
        return name + " (" + ordinal + ")";
    }
}
