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
package org.apache.directory.shared.kerberos;

/**
 * An cass to define Kerberos constants
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KerberosConstants
{
    /** The Kerberos version 5 */
    public static final byte KERBEROS_V5 = 5;

    //-------------------------------------------------------------------------
    // Messages
    //-------------------------------------------------------------------------
    /** Ticket message's tags */
    public static final byte TICKET_TAG = 0x61;
    public static final byte TICKET_TKT_VNO_TAG = (byte)0xA0;
    public static final byte TICKET_REALM_TAG = (byte)0xA1;
    public static final byte TICKET_SNAME_TAG = (byte)0xA2;
    public static final byte TICKET_ENC_PART_TAG = (byte)0xA3;
    
    /** Authenticator tags */
    public static final byte AUTHENTICATOR_TAG = 0x62;
    public static final byte AUTHENTICATOR_AUTHENTICATOR_VNO_TAG = (byte)0xA0;
    public static final byte AUTHENTICATOR_CREALM_TAG = (byte)0xA1;
    public static final byte AUTHENTICATOR_CNAME_TAG = (byte)0xA2;
    public static final byte AUTHENTICATOR_CKSUM_TAG = (byte)0xA3;
    public static final byte AUTHENTICATOR_CUSEC_TAG = (byte)0xA4;
    public static final byte AUTHENTICATOR_CTIME_TAG = (byte)0xA5;
    public static final byte AUTHENTICATOR_SUBKEY_TAG = (byte)0xA6;
    public static final byte AUTHENTICATOR_SEQ_NUMBER_TAG = (byte)0xA7;
    public static final byte AUTHENTICATOR_AUTHORIZATION_DATA_TAG = (byte)0xA8;
    
    /** AS-REQ's tags */
    public static final byte AS_REQ_TAG = 0x6A;
    
    /** AS-REP's tags */
    public static final byte AS_REP_TAG = 0x6B;
    
    /** TGS-REQ's tags */
    public static final byte TGS_REQ_TAG = 0x6C;
    
    /** TGS-REP's tags */
    public static final byte TGS_REP_TAG = 0x6D;
    
    /** AP-REQ tags */
    public static final byte AP_REQ_TAG = 0x6E;
    public static final byte AP_REQ_PVNO_TAG = (byte)0xA0;
    public static final byte AP_REQ_MSG_TYPE_TAG = (byte)0xA1;
    public static final byte AP_REQ_AP_OPTIONS_TAG = (byte)0xA2;
    public static final byte AP_REQ_TICKET_TAG = (byte)0xA3;
    public static final byte AP_REQ_AUTHENTICATOR_TAG = (byte)0xA4;
    
    /** AP-REP tags */
    public static final byte AP_REP_TAG = 0x6F;
    public static final byte AP_REP_PVNO_TAG = (byte)0xA0;
    public static final byte AP_REP_MSG_TYPE_TAG = (byte)0xA1;
    public static final byte AP_REP_ENC_PART_TAG = (byte)0xA2;
    
    /** KrbSafe tags */
    public static final byte KRB_SAFE_TAG = 0x74;
    public static final byte KRB_SAFE_PVNO_TAG = (byte)0xA0;
    public static final byte KRB_SAFE_MSGTYPE_TAG = (byte)0xA1;
    public static final byte KRB_SAFE_SAFE_BODY_TAG = (byte)0xA2;
    public static final byte KRB_SAFE_CKSUM_TAG = (byte)0xA3;

    /** KrbPriv */
    public static final byte KRB_PRIV_TAG = 0x75;
    public static final byte KRB_PRIV_PVNO_TAG = (byte)0xA0;
    public static final byte KRB_PRIV_MSGTYPE_TAG = (byte)0xA1;
    public static final byte KRB_PRIV_ENC_PART_TAG = (byte)0xA3;

    /** EncKrbPrivPart */
    public static final byte ENC_KRB_PRIV_PART_TAG = 0x7C;
    public static final byte ENC_KRB_PRIV_PART_USER_DATA_TAG = (byte)0xA0;
    public static final byte ENC_KRB_PRIV_PART_TIMESTAMP_TAG = (byte)0xA1;
    public static final byte ENC_KRB_PRIV_PART_USEC_TAG = (byte)0xA2;
    public static final byte ENC_KRB_PRIV_PART_SEQ_NUMBER_TAG = (byte)0xA3;
    public static final byte ENC_KRB_PRIV_PART_SENDER_ADDRESS_TAG = (byte)0xA4;
    public static final byte ENC_KRB_PRIV_PART_RECIPIENT_ADDRESS_TAG = (byte)0xA5;

    /** KRB-ERROR tags */
    public static final byte KRB_ERR_TAG = 0x7E;
    public static final byte KRB_ERR_PVNO_TAG = (byte)0xA0;
    public static final byte KRB_ERR_MSGTYPE_TAG = (byte)0xA1;
    public static final byte KRB_ERR_CTIME_TAG = (byte)0xA2;
    public static final byte KRB_ERR_CUSEC_TAG = (byte)0xA3;
    public static final byte KRB_ERR_STIME_TAG = (byte)0xA4;
    public static final byte KRB_ERR_SUSEC_TAG = (byte)0xA5;
    public static final byte KRB_ERR_ERROR_CODE_TAG = (byte)0xA6;
    public static final byte KRB_ERR_CREALM_TAG = (byte)0xA7;
    public static final byte KRB_ERR_CNAME_TAG = (byte)0xA8;
    public static final byte KRB_ERR_REALM_TAG = (byte)0xA9;
    public static final byte KRB_ERR_SNAME_TAG = (byte)0xAA;
    public static final byte KRB_ERR_ETEXT_TAG = (byte)0xAB;
    public static final byte KRB_ERR_EDATA_TAG = (byte)0xAC;

    //-------------------------------------------------------------------------
    // Components
    //-------------------------------------------------------------------------
    /** AuthorizationData tags */
    public static final byte AUTHORIZATION_DATA_ADTYPE_TAG = (byte)0xA0;
    public static final byte AUTHORIZATION_DATA_ADDATA_TAG = (byte)0xA1;

    /** Checksum tags */
    public static final byte CHECKSUM_TYPE_TAG = (byte)0xA0;
    public static final byte CHECKSUM_CHECKSUM_TAG = (byte)0xA1;

    /** Encrypteddata's tags */
    public static final byte ENCRYPTED_DATA_ETYPE_TAG = (byte)0xA0;
    public static final byte ENCRYPTED_DATA_KVNO_TAG = (byte)0xA1;
    public static final byte ENCRYPTED_DATA_CIPHER_TAG = (byte)0xA2;
    
    /** EncryptionKey tags */
    public static final byte ENCRYPTION_KEY_TYPE_TAG = (byte)0xA0;
    public static final byte ENCRYPTION_KEY_VALUE_TAG = (byte)0xA1;
    
    /** ETYPE-INFO-ENTRY tags */
    public static final byte ETYPE_INFO_ENTRY_ETYPE_TAG = (byte)0xA0;
    public static final byte ETYPE_INFO_ENTRY_SALT_TAG = (byte)0xA1;
    
    /** ETYPE-INFO2-ENTRY tags */
    public static final byte ETYPE_INFO2_ENTRY_ETYPE_TAG = (byte)0xA0;
    public static final byte ETYPE_INFO2_ENTRY_SALT_TAG = (byte)0xA1;
    public static final byte ETYPE_INFO2_ENTRY_S2KPARAMS_TAG = (byte)0xA2;
    
    /** HostAddress' tags */
    public static final byte HOST_ADDRESS_ADDR_TYPE_TAG = (byte)0xA0;
    public static final byte HOST_ADDRESS_ADDRESS_TAG = (byte)0xA1;
    
    
    /** KrbCredInfo tags */
    public static final byte KRB_CRED_INFO_KEY_TAG = (byte)0xA0;
    public static final byte KRB_CRED_INFO_PREALM_TAG = (byte)0xA1;
    public static final byte KRB_CRED_INFO_PNAME_TAG = (byte)0xA2;
    public static final byte KRB_CRED_INFO_FLAGS_TAG = (byte)0xA3;
    public static final byte KRB_CRED_INFO_AUTHTIME_TAG = (byte)0xA4;
    public static final byte KRB_CRED_INFO_STARTTIME_TAG = (byte)0xA5;
    public static final byte KRB_CRED_INFO_ENDTIME_TAG = (byte)0xA6;
    public static final byte KRB_CRED_INFO_RENEWTILL_TAG = (byte)0xA7;
    public static final byte KRB_CRED_INFO_SREALM_TAG = (byte)0xA8;
    public static final byte KRB_CRED_INFO_SNAME_TAG = (byte)0xA9;
    public static final byte KRB_CRED_INFO_CADDR_TAG = (byte)0xAA;

    /** KRB-REP's tags */
    public static final byte KDC_REP_PVNO_TAG = (byte)0xA0;
    public static final byte KDC_REP_MSG_TYPE_TAG = (byte)0xA1;
    public static final byte KDC_REP_PA_DATA_TAG = (byte)0xA2;
    public static final byte KDC_REP_CREALM_TAG = (byte)0xA3;
    public static final byte KDC_REP_CNAME_TAG = (byte)0xA4;
    public static final byte KDC_REP_TICKET_TAG = (byte)0xA5;
    public static final byte KDC_REP_ENC_PART_TAG = (byte)0xA6;
    
    /** KRB-REQ's tags */
    public static final byte KDC_REQ_PVNO_TAG = (byte)0xA1;
    public static final byte KDC_REQ_MSG_TYPE_TAG = (byte)0xA2;
    public static final byte KDC_REQ_PA_DATA_TAG = (byte)0xA3;
    public static final byte KDC_REQ_KDC_REQ_BODY_TAG = (byte)0xA4;

    /** KRB-REQ-BODY's tags */
    public static final byte KDC_REQ_BODY_KDC_OPTIONS_TAG = (byte)0xA0;
    public static final byte KDC_REQ_BODY_CNAME_TAG = (byte)0xA1;
    public static final byte KDC_REQ_BODY_REALM_TAG = (byte)0xA2;
    public static final byte KDC_REQ_BODY_SNAME_TAG = (byte)0xA3;
    public static final byte KDC_REQ_BODY_FROM_TAG = (byte)0xA4;
    public static final byte KDC_REQ_BODY_TILL_TAG = (byte)0xA5;
    public static final byte KDC_REQ_BODY_RTIME_TAG = (byte)0xA6;
    public static final byte KDC_REQ_BODY_NONCE_TAG = (byte)0xA7;
    public static final byte KDC_REQ_BODY_ETYPE_TAG = (byte)0xA8;
    public static final byte KDC_REQ_BODY_ADDRESSES_TAG = (byte)0xA9;
    public static final byte KDC_REQ_BODY_ENC_AUTHZ_DATA_TAG = (byte)0xAA;
    public static final byte KDC_REQ_BODY_ADDITIONAL_TICKETS_TAG = (byte)0xAB;
    
    /** KrbSafeBody tags */
    public static final byte KRB_SAFE_BODY_USER_DATA_TAG = (byte)0xA0;
    public static final byte KRB_SAFE_BODY_TIMESTAMP_TAG = (byte)0xA1;
    public static final byte KRB_SAFE_BODY_USEC_TAG = (byte)0xA2;
    public static final byte KRB_SAFE_BODY_SEQ_NUMBER_TAG = (byte)0xA3;
    public static final byte KRB_SAFE_BODY_SENDER_ADDRESS_TAG = (byte)0xA4;
    public static final byte KRB_SAFE_BODY_RECIPIENT_ADDRESS_TAG = (byte)0xA5;

    /** PaData tags */
    public static final byte PADATA_TYPE_TAG = (byte)0xA1;
    public static final byte PADATA_VALUE_TAG = (byte)0xA2;

    /** PrincipalName's tags */
    public static final byte PRINCIPAL_NAME_NAME_TYPE_TAG = (byte)0xA0;
    public static final byte PRINCIPAL_NAME_NAME_STRING_TAG = (byte)0xA1;

    /** TransitedEncoding tags */
    public static final byte TRANSITED_ENCODING_TR_TYPE_TAG = (byte)0xA0;
    public static final byte TRANSITED_ENCODING_CONTENTS_TAG = (byte)0xA1;
}
