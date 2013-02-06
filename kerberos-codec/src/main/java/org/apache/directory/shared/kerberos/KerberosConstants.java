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
    public static final int KERBEROS_V5 = 5;

    //-------------------------------------------------------------------------
    // Messages
    //-------------------------------------------------------------------------
    /** Ticket message's tags */
    public static final int TICKET_TAG = 0x61;
    public static final int TICKET_TKT_VNO_TAG = 0xA0;
    public static final int TICKET_REALM_TAG = 0xA1;
    public static final int TICKET_SNAME_TAG = 0xA2;
    public static final int TICKET_ENC_PART_TAG = 0xA3;

    /** Authenticator tags */
    public static final int AUTHENTICATOR_TAG = 0x62;
    public static final int AUTHENTICATOR_AUTHENTICATOR_VNO_TAG = 0xA0;
    public static final int AUTHENTICATOR_CREALM_TAG = 0xA1;
    public static final int AUTHENTICATOR_CNAME_TAG = 0xA2;
    public static final int AUTHENTICATOR_CKSUM_TAG = 0xA3;
    public static final int AUTHENTICATOR_CUSEC_TAG = 0xA4;
    public static final int AUTHENTICATOR_CTIME_TAG = 0xA5;
    public static final int AUTHENTICATOR_SUBKEY_TAG = 0xA6;
    public static final int AUTHENTICATOR_SEQ_NUMBER_TAG = 0xA7;
    public static final int AUTHENTICATOR_AUTHORIZATION_DATA_TAG = 0xA8;

    /** AS-REQ's tags */
    public static final int AS_REQ_TAG = 0x6A;

    /** AS-REP's tags */
    public static final int AS_REP_TAG = 0x6B;

    /** TGS-REQ's tags */
    public static final int TGS_REQ_TAG = 0x6C;

    /** TGS-REP's tags */
    public static final int TGS_REP_TAG = 0x6D;

    /** AP-REQ tags */
    public static final int AP_REQ_TAG = 0x6E;
    public static final int AP_REQ_PVNO_TAG = 0xA0;
    public static final int AP_REQ_MSG_TYPE_TAG = 0xA1;
    public static final int AP_REQ_AP_OPTIONS_TAG = 0xA2;
    public static final int AP_REQ_TICKET_TAG = 0xA3;
    public static final int AP_REQ_AUTHENTICATOR_TAG = 0xA4;

    /** AP-REP tags */
    public static final int AP_REP_TAG = 0x6F;
    public static final int AP_REP_PVNO_TAG = 0xA0;
    public static final int AP_REP_MSG_TYPE_TAG = 0xA1;
    public static final int AP_REP_ENC_PART_TAG = 0xA2;

    /** KrbSafe tags */
    public static final int KRB_SAFE_TAG = 0x74;
    public static final int KRB_SAFE_PVNO_TAG = 0xA0;
    public static final int KRB_SAFE_MSGTYPE_TAG = 0xA1;
    public static final int KRB_SAFE_SAFE_BODY_TAG = 0xA2;
    public static final int KRB_SAFE_CKSUM_TAG = 0xA3;

    /** KrbPriv */
    public static final int KRB_PRIV_TAG = 0x75;
    public static final int KRB_PRIV_PVNO_TAG = 0xA0;
    public static final int KRB_PRIV_MSGTYPE_TAG = 0xA1;
    public static final int KRB_PRIV_ENC_PART_TAG = 0xA3;

    /** EncAsRepPart's tags */
    public static final int ENC_AS_REP_PART_TAG = 0x79;

    /** EncTgsRepPart's tags */
    public static final int ENC_TGS_REP_PART_TAG = 0x7A;

    /** EncAPRepPart's tags */
    public static final int ENC_AP_REP_PART_TAG = 0x7B;
    public static final int ENC_AP_REP_PART_CTIME_TAG = 0xA0;
    public static final int ENC_AP_REP_PART_CUSEC_TAG = 0xA1;
    public static final int ENC_AP_REP_PART_SUB_KEY_TAG = 0xA2;
    public static final int ENC_AP_REP_PART_SEQ_NUMBER_TAG = 0xA3;

    /** EncKrbPrivPart */
    public static final int ENC_KRB_PRIV_PART_TAG = 0x7C;
    public static final int ENC_KRB_PRIV_PART_USER_DATA_TAG = 0xA0;
    public static final int ENC_KRB_PRIV_PART_TIMESTAMP_TAG = 0xA1;
    public static final int ENC_KRB_PRIV_PART_USEC_TAG = 0xA2;
    public static final int ENC_KRB_PRIV_PART_SEQ_NUMBER_TAG = 0xA3;
    public static final int ENC_KRB_PRIV_PART_SENDER_ADDRESS_TAG = 0xA4;
    public static final int ENC_KRB_PRIV_PART_RECIPIENT_ADDRESS_TAG = 0xA5;

    /** KRB-ERROR tags */
    public static final int KRB_ERROR_TAG = 0x7E;
    public static final int KRB_ERROR_PVNO_TAG = 0xA0;
    public static final int KRB_ERROR_MSGTYPE_TAG = 0xA1;
    public static final int KRB_ERROR_CTIME_TAG = 0xA2;
    public static final int KRB_ERROR_CUSEC_TAG = 0xA3;
    public static final int KRB_ERROR_STIME_TAG = 0xA4;
    public static final int KRB_ERROR_SUSEC_TAG = 0xA5;
    public static final int KRB_ERROR_ERROR_CODE_TAG = 0xA6;
    public static final int KRB_ERROR_CREALM_TAG = 0xA7;
    public static final int KRB_ERROR_CNAME_TAG = 0xA8;
    public static final int KRB_ERROR_REALM_TAG = 0xA9;
    public static final int KRB_ERROR_SNAME_TAG = 0xAA;
    public static final int KRB_ERROR_ETEXT_TAG = 0xAB;
    public static final int KRB_ERROR_EDATA_TAG = 0xAC;

    /** KRB-CRED tags */
    public static final int KRB_CRED_TAG = 0x76;
    public static final int KRB_CRED_PVNO_TAG = 0xA0;
    public static final int KRB_CRED_MSGTYPE_TAG = 0xA1;
    public static final int KRB_CRED_TICKETS_TAG = 0xA2;
    public static final int KRB_CRED_ENCPART_TAG = 0xA3;

    //-------------------------------------------------------------------------
    // Components
    //-------------------------------------------------------------------------
    /** AD-AND-OR */
    public static final int AD_AND_OR_CONDITION_COUNT_TAG = 0xA0;
    public static final int AD_AND_OR_ELEMENTS_TAG = 0xA1;

    /** AD-KDCIssued */
    public static final int AD_KDC_ISSUED_AD_CHECKSUM_TAG = 0xA0;
    public static final int AD_KDC_ISSUED_I_REALM_TAG = 0xA1;
    public static final int AD_KDC_ISSUED_I_SNAME_TAG = 0xA2;
    public static final int AD_KDC_ISSUED_ELEMENTS_TAG = 0xA3;

    /** AuthorizationData tags */
    public static final int AUTHORIZATION_DATA_ADTYPE_TAG = 0xA0;
    public static final int AUTHORIZATION_DATA_ADDATA_TAG = 0xA1;

    /** Checksum tags */
    public static final int CHECKSUM_TYPE_TAG = 0xA0;
    public static final int CHECKSUM_CHECKSUM_TAG = 0xA1;

    /** EncKdcRepPart tags */
    public static final int ENC_KDC_REP_PART_KEY_TAG = 0xA0;
    public static final int ENC_KDC_REP_PART_LAST_REQ_TAG = 0xA1;
    public static final int ENC_KDC_REP_PART_NONCE_TAG = 0xA2;
    public static final int ENC_KDC_REP_PART_KEY_EXPIRATION_TAG = 0xA3;
    public static final int ENC_KDC_REP_PART_FLAGS_TAG = 0xA4;
    public static final int ENC_KDC_REP_PART_AUTH_TIME_TAG = 0xA5;
    public static final int ENC_KDC_REP_PART_START_TIME_TAG = 0xA6;
    public static final int ENC_KDC_REP_PART_END_TIME_TAG = 0xA7;
    public static final int ENC_KDC_REP_PART_RENEW_TILL_TAG = 0xA8;
    public static final int ENC_KDC_REP_PART_SREALM_TAG = 0xA9;
    public static final int ENC_KDC_REP_PART_SNAME_TAG = 0xAA;
    public static final int ENC_KDC_REP_PART_CADDR_TAG = 0xAB;

    /** EncKrbCredPart tags */
    public static final int ENC_KRB_CRED_PART_TAG = 0x7D;
    public static final int ENC_KRB_CRED_TICKET_INFO_TAG = 0xA0;
    public static final int ENC_KRB_CRED_PART_NONCE_TAG = 0xA1;
    public static final int ENC_KRB_CRED_PART_TIMESTAMP_TAG = 0xA2;
    public static final int ENC_KRB_CRED_PART_USEC_TAG = 0xA3;
    public static final int ENC_KRB_CRED_PART_SENDER_ADDRESS_TAG = 0xA4;
    public static final int ENC_KRB_CRED_PART_RECIPIENT_ADDRESS_TAG = 0xA5;

    /** Encrypteddata's tags */
    public static final int ENCRYPTED_DATA_ETYPE_TAG = 0xA0;
    public static final int ENCRYPTED_DATA_KVNO_TAG = 0xA1;
    public static final int ENCRYPTED_DATA_CIPHER_TAG = 0xA2;

    /** EncryptionKey tags */
    public static final int ENCRYPTION_KEY_TYPE_TAG = 0xA0;
    public static final int ENCRYPTION_KEY_VALUE_TAG = 0xA1;

    /** EncTicketPart tags */
    public static final int ENC_TICKET_PART_TAG = 0x63;
    public static final int ENC_TICKET_PART_FLAGS_TAG = 0xA0;
    public static final int ENC_TICKET_PART_KEY_TAG = 0xA1;
    public static final int ENC_TICKET_PART_CREALM_TAG = 0xA2;
    public static final int ENC_TICKET_PART_CNAME_TAG = 0xA3;
    public static final int ENC_TICKET_PART_TRANSITED_TAG = 0xA4;
    public static final int ENC_TICKET_PART_AUTHTIME_TAG = 0xA5;
    public static final int ENC_TICKET_PART_STARTTIME_TAG = 0xA6;
    public static final int ENC_TICKET_PART_ENDTIME_TAG = 0xA7;
    public static final int ENC_TICKET_PART_RENEWTILL_TAG = 0xA8;
    public static final int ENC_TICKET_PART_CADDR_TAG = 0xA9;
    public static final int ENC_TICKET_PART_AUTHORIZATION_DATA_TAG = 0xAA;

    /** ETYPE-INFO-ENTRY tags */
    public static final int ETYPE_INFO_ENTRY_ETYPE_TAG = 0xA0;
    public static final int ETYPE_INFO_ENTRY_SALT_TAG = 0xA1;

    /** ETYPE-INFO2-ENTRY tags */
    public static final int ETYPE_INFO2_ENTRY_ETYPE_TAG = 0xA0;
    public static final int ETYPE_INFO2_ENTRY_SALT_TAG = 0xA1;
    public static final int ETYPE_INFO2_ENTRY_S2KPARAMS_TAG = 0xA2;

    /** HostAddress' tags */
    public static final int HOST_ADDRESS_ADDR_TYPE_TAG = 0xA0;
    public static final int HOST_ADDRESS_ADDRESS_TAG = 0xA1;

    /** KrbCredInfo tags */
    public static final int KRB_CRED_INFO_KEY_TAG = 0xA0;
    public static final int KRB_CRED_INFO_PREALM_TAG = 0xA1;
    public static final int KRB_CRED_INFO_PNAME_TAG = 0xA2;
    public static final int KRB_CRED_INFO_FLAGS_TAG = 0xA3;
    public static final int KRB_CRED_INFO_AUTHTIME_TAG = 0xA4;
    public static final int KRB_CRED_INFO_STARTTIME_TAG = 0xA5;
    public static final int KRB_CRED_INFO_ENDTIME_TAG = 0xA6;
    public static final int KRB_CRED_INFO_RENEWTILL_TAG = 0xA7;
    public static final int KRB_CRED_INFO_SREALM_TAG = 0xA8;
    public static final int KRB_CRED_INFO_SNAME_TAG = 0xA9;
    public static final int KRB_CRED_INFO_CADDR_TAG = 0xAA;

    /** KRB-REP's tags */
    public static final int KDC_REP_PVNO_TAG = 0xA0;
    public static final int KDC_REP_MSG_TYPE_TAG = 0xA1;
    public static final int KDC_REP_PA_DATA_TAG = 0xA2;
    public static final int KDC_REP_CREALM_TAG = 0xA3;
    public static final int KDC_REP_CNAME_TAG = 0xA4;
    public static final int KDC_REP_TICKET_TAG = 0xA5;
    public static final int KDC_REP_ENC_PART_TAG = 0xA6;

    /** KRB-REQ's tags */
    public static final int KDC_REQ_PVNO_TAG = 0xA1;
    public static final int KDC_REQ_MSG_TYPE_TAG = 0xA2;
    public static final int KDC_REQ_PA_DATA_TAG = 0xA3;
    public static final int KDC_REQ_KDC_REQ_BODY_TAG = 0xA4;

    /** KRB-REQ-BODY's tags */
    public static final int KDC_REQ_BODY_KDC_OPTIONS_TAG = 0xA0;
    public static final int KDC_REQ_BODY_CNAME_TAG = 0xA1;
    public static final int KDC_REQ_BODY_REALM_TAG = 0xA2;
    public static final int KDC_REQ_BODY_SNAME_TAG = 0xA3;
    public static final int KDC_REQ_BODY_FROM_TAG = 0xA4;
    public static final int KDC_REQ_BODY_TILL_TAG = 0xA5;
    public static final int KDC_REQ_BODY_RTIME_TAG = 0xA6;
    public static final int KDC_REQ_BODY_NONCE_TAG = 0xA7;
    public static final int KDC_REQ_BODY_ETYPE_TAG = 0xA8;
    public static final int KDC_REQ_BODY_ADDRESSES_TAG = 0xA9;
    public static final int KDC_REQ_BODY_ENC_AUTHZ_DATA_TAG = 0xAA;
    public static final int KDC_REQ_BODY_ADDITIONAL_TICKETS_TAG = 0xAB;

    /** KrbSafeBody tags */
    public static final int KRB_SAFE_BODY_USER_DATA_TAG = 0xA0;
    public static final int KRB_SAFE_BODY_TIMESTAMP_TAG = 0xA1;
    public static final int KRB_SAFE_BODY_USEC_TAG = 0xA2;
    public static final int KRB_SAFE_BODY_SEQ_NUMBER_TAG = 0xA3;
    public static final int KRB_SAFE_BODY_SENDER_ADDRESS_TAG = 0xA4;
    public static final int KRB_SAFE_BODY_RECIPIENT_ADDRESS_TAG = 0xA5;

    /** LastRequest tags */
    public static final int LAST_REQ_LR_TYPE_TAG = 0xA0;
    public static final int LAST_REQ_LR_VALUE_TAG = 0xA1;

    /** PaData tags */
    public static final int PADATA_TYPE_TAG = 0xA1;
    public static final int PADATA_VALUE_TAG = 0xA2;

    /** PA-ENC-TS-ENC tags */
    public static final int PA_ENC_TS_ENC_PA_TIMESTAMP_TAG = 0xA0;
    public static final int PA_ENC_TS_ENC_PA_USEC_TAG = 0xA1;

    /** PrincipalName's tags */
    public static final int PRINCIPAL_NAME_NAME_TYPE_TAG = 0xA0;
    public static final int PRINCIPAL_NAME_NAME_STRING_TAG = 0xA1;

    /** TransitedEncoding tags */
    public static final int TRANSITED_ENCODING_TR_TYPE_TAG = 0xA0;
    public static final int TRANSITED_ENCODING_CONTENTS_TAG = 0xA1;

    /** TypedData tags */
    public static final int TYPED_DATA_TDTYPE_TAG = 0xA0;
    public static final int TYPED_DATA_TDDATA_TAG = 0xA1;
    
    /** CHangePasswdData tags */
    public static final int CHNGPWD_NEWPWD_TAG = 0xA0;
    public static final int CHNGPWD_TARGNAME_TAG = 0xA1;
    public static final int CHNGPWD_TARGREALM_TAG = 0xA2;
}
