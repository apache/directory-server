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
 * The Kerberos Error codes
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 540371 $, $Date: 2007-05-22 02:00:43 +0200 (Tue, 22 May 2007) $
 */
public enum KerberosErrorType
{
    /** No error */
    KDC_ERR_NONE(0),
    
    /** Client's entry in database has expired */
    KDC_ERR_NAME_EXP(1),

    /** Server's entry in database has expired */
    KDC_ERR_SERVICE_EXP(2),  
                          
    /** Requested protocol version number not supported */
    KDC_ERR_BAD_PVNO(3),  
    
    /** Client's key encrypted in old master key */ 
    KDC_ERR_C_OLD_MAST_KVNO(4),
    
    /** Server's key encrypted in old master key */ 
    KDC_ERR_S_OLD_MAST_KVNO(5),
    
    /** Client not found in Kerberos database */ 
    KDC_ERR_C_PRINCIPAL_UNKNOWN(6),
    
    /** Server not found in Kerberos database */ 
    KDC_ERR_S_PRINCIPAL_UNKNOWN(7),
    
    /** Multiple principal entries in database */ 
    KDC_ERR_PRINCIPAL_NOT_UNIQUE(8),
    
    /** The client or server has a null key */ 
    KDC_ERR_NULL_KEY(9),
    
    /** Ticket not eligible for postdating */ 
    KDC_ERR_CANNOT_POSTDATE(10),
    
    /** Requested starttime is later than end time */ 
    KDC_ERR_NEVER_VALID(11),
    
    /** KDC policy rejects request */ 
    KDC_ERR_POLICY(12),
    
    /** KDC cannot accommodate requested option */ 
    KDC_ERR_BADOPTION(13),
    
    /** KDC has no support for encryption type */ 
    KDC_ERR_ETYPE_NOSUPP(14),
    
    /** KDC has no support for checksum type */ 
    KDC_ERR_SUMTYPE_NOSUPP(15),
    
    /** KDC has no support for padata type */ 
    KDC_ERR_PADATA_TYPE_NOSUPP(16),
    
    /** KDC has no support for transited type */ 
    KDC_ERR_TRTYPE_NOSUPP(17),
    
    /** Clients credentials have been revoked */ 
    KDC_ERR_CLIENT_REVOKED(18),
    
    /** Credentials for server have been revoked */ 
    KDC_ERR_SERVICE_REVOKED(19),
    
    /** TGT has been revoked */ 
    KDC_ERR_TGT_REVOKED(20),
    
    /** Client not yet valid; try again later */ 
    KDC_ERR_CLIENT_NOTYET(21),
    
    /** Server not yet valid; try again later */ 
    KDC_ERR_SERVICE_NOTYET(22),
    
    /** Password has expired; change password to reset */ 
    KDC_ERR_KEY_EXPIRED(23),
    
    /** Pre-authentication information was invalid */ 
    KDC_ERR_PREAUTH_FAILED(24),
    
    /** Additional pre-authentication required */ 
    KDC_ERR_PREAUTH_REQUIRED(25),
    
    /** Requested server and ticket don't match */ 
    KDC_ERR_SERVER_NOMATCH(26),
    
    /** Server principal valid for user2user only */
    
    KDC_ERR_MUST_USE_USER2USER(27),  
    
    /** KDC Policy rejects transited path */ 
    KDC_ERR_PATH_NOT_ACCEPTED(28),
    
    /** A service is not available */ 
    KDC_ERR_SVC_UNAVAILABLE(29),
    
    /** Integrity check on decrypted field failed */ 
    KRB_AP_ERR_BAD_INTEGRITY(31),
    
    /** Ticket expired */ 
    KRB_AP_ERR_TKT_EXPIRED(32),
    
    /** Ticket not yet valid */ 
    KRB_AP_ERR_TKT_NYV(33),
    
    /** Request is a replay */ 
    KRB_AP_ERR_REPEAT(34),
    
    /** The ticket isn't for us */ 
    KRB_AP_ERR_NOT_US(35),
    
    /** Ticket and authenticator don't match */ 
    KRB_AP_ERR_BADMATCH(36),
    
    /** Clock skew too great */ 
    KRB_AP_ERR_SKEW(37),
    
    /** Incorrect net address */ 
    KRB_AP_ERR_BADADDR(38),
    
    /** Protocol version mismatch */ 
    KRB_AP_ERR_BADVERSION(39),
    
    /** Invalid msg type */ 
    KRB_AP_ERR_MSG_TYPE(40),
    
    /** Message stream modified */ 
    KRB_AP_ERR_MODIFIED(41),
    
    /** Message out of order */ 
    KRB_AP_ERR_BADORDER(42),
    
    /** Specified version of key is not available */ 
    KRB_AP_ERR_BADKEYVER(44),
    
    /** Service key not available */ 
    KRB_AP_ERR_NOKEY(45),
    
    /** Mutual authentication failed */ 
    KRB_AP_ERR_MUT_FAIL(46),
    
    /** Incorrect message direction */ 
    KRB_AP_ERR_BADDIRECTION(47),
    
    /** Alternative authentication method required */ 
    KRB_AP_ERR_METHOD(48),
    
    /** Incorrect sequence number in message */ 
    KRB_AP_ERR_BADSEQ(49),
    
    /** Inappropriate type of checksum in message */ 
    KRB_AP_ERR_INAPP_CKSUM(50),
    
    /** Policy rejects transited path */ 
    KRB_AP_PATH_NOT_ACCEPTED(51),
    
    /** Response too big for UDP; retry with TCP */ 
    KRB_ERR_RESPONSE_TOO_BIG(52),
    
    /** Generic error (description in e-text) */ 
    KRB_ERR_GENERIC(60),
    
    /** Field is too long for this implementation */ 
    KRB_ERR_FIELD_TOOLONG(61),
    
    /** Reserved for PKINIT */ 
    KDC_ERROR_CLIENT_NOT_TRUSTED(62),
    
    /** Reserved for PKINIT */ 
    KDC_ERROR_KDC_NOT_TRUSTED(63),
    
    /** Reserved for PKINIT */ 
    KDC_ERROR_INVALID_SIG(64),
    
    /** Reserved for PKINIT */ 
    KDC_ERR_KEY_TOO_WEAK(65),
    
    /** Reserved for PKINIT */ 
    KDC_ERR_CERTIFICATE_MISMATCH(66),
    
    /** No TGT available to validate USER-TO-USER */ 
    KRB_AP_ERR_NO_TGT(67),
    
    /** Reserved for future use */ 
    KDC_ERR_WRONG_REALM(68),
    
    /** Ticket must be for USER-TO-USER */ 
    KRB_AP_ERR_USER_TO_USER_REQUIRED(69),
    
    /** Reserved for PKINIT */ 
    KDC_ERR_CANT_VERIFY_CERTIFICATE(70),
    
    /** Reserved for PKINIT */ 
    KDC_ERR_INVALID_CERTIFICATE(71),
    
    /** Reserved for PKINIT */ 
    KDC_ERR_REVOKED_CERTIFICATE(72),
    
    /** Reserved for PKINIT */ 
    KDC_ERR_REVOCATION_STATUS_UNKNOWN(73),
    
    /** Reserved for PKINIT */ 
    KDC_ERR_REVOCATION_STATUS_UNAVAILABLE(74),
    
    /** Reserved for PKINIT */ 
    KDC_ERR_CLIENT_NAME_MISMATCH(75),
    
    /** Reserved for PKINIT */ 
    KDC_ERR_KDC_NAME_MISMATCH(76),
    
    /** No error */
    NULL(-1);
    
    

    /**
     * The value/code for the authorization type.
     */
    private final int ordinal;

    /**
     * Private constructor prevents construction outside of this class.
     */
    private KerberosErrorType( int ordinal )
    {
        this.ordinal = ordinal;
    }


    /**
     * Returns the authorization type when specified by its ordinal.
     *
     * @param type
     * @return The authorization type.
     */
    public static KerberosErrorType getTypeByOrdinal( int type )
    {
    	switch ( type )
    	{
            case 0 : return KDC_ERR_NONE;
            case 1 : return KDC_ERR_NAME_EXP;
            case 2 : return KDC_ERR_SERVICE_EXP;
            case 3 : return KDC_ERR_BAD_PVNO;
            case 4 : return KDC_ERR_C_OLD_MAST_KVNO;
            case 5 : return KDC_ERR_S_OLD_MAST_KVNO;
            case 6 : return KDC_ERR_C_PRINCIPAL_UNKNOWN;
            case 7 : return KDC_ERR_S_PRINCIPAL_UNKNOWN;
            case 8 : return KDC_ERR_PRINCIPAL_NOT_UNIQUE;
            case 9 : return KDC_ERR_NULL_KEY;
            case 10 : return KDC_ERR_CANNOT_POSTDATE;
            case 11 : return KDC_ERR_NEVER_VALID;
            case 12 : return KDC_ERR_POLICY;
            case 13 : return KDC_ERR_BADOPTION;
            case 14 : return KDC_ERR_ETYPE_NOSUPP;
            case 15 : return KDC_ERR_SUMTYPE_NOSUPP;
            case 16 : return KDC_ERR_PADATA_TYPE_NOSUPP;
            case 17 : return KDC_ERR_TRTYPE_NOSUPP;
            case 18 : return KDC_ERR_CLIENT_REVOKED;
            case 19 : return KDC_ERR_SERVICE_REVOKED;
            case 20 : return KDC_ERR_TGT_REVOKED;
            case 21 : return KDC_ERR_CLIENT_NOTYET;
            case 22 : return KDC_ERR_SERVICE_NOTYET;
            case 23 : return KDC_ERR_KEY_EXPIRED;
            case 24 : return KDC_ERR_PREAUTH_FAILED;
            case 25 : return KDC_ERR_PREAUTH_REQUIRED;
            case 26 : return KDC_ERR_SERVER_NOMATCH;
            case 27 : return KDC_ERR_MUST_USE_USER2USER;
            case 28 : return KDC_ERR_PATH_NOT_ACCEPTED;
            case 29 : return KDC_ERR_SVC_UNAVAILABLE;
            case 31 : return KRB_AP_ERR_BAD_INTEGRITY;
            case 32 : return KRB_AP_ERR_TKT_EXPIRED;
            case 33 : return KRB_AP_ERR_TKT_NYV;
            case 34 : return KRB_AP_ERR_REPEAT;
            case 35 : return KRB_AP_ERR_NOT_US;
            case 36 : return KRB_AP_ERR_BADMATCH;
            case 37 : return KRB_AP_ERR_SKEW;
            case 38 : return KRB_AP_ERR_BADADDR;
            case 39 : return KRB_AP_ERR_BADVERSION;
            case 40 : return KRB_AP_ERR_MSG_TYPE;
            case 41 : return KRB_AP_ERR_MODIFIED;
            case 42 : return KRB_AP_ERR_BADORDER;
            case 44 : return KRB_AP_ERR_BADKEYVER;
            case 45 : return KRB_AP_ERR_NOKEY;
            case 46 : return KRB_AP_ERR_MUT_FAIL;
            case 47 : return KRB_AP_ERR_BADDIRECTION;
            case 48 : return KRB_AP_ERR_METHOD;
            case 49 : return KRB_AP_ERR_BADSEQ;
            case 50 : return KRB_AP_ERR_INAPP_CKSUM;
            case 51 : return KRB_AP_PATH_NOT_ACCEPTED;
            case 52 : return KRB_ERR_RESPONSE_TOO_BIG;
            case 60 : return KRB_ERR_GENERIC;
            case 61 : return KRB_ERR_FIELD_TOOLONG;
            case 62 : return KDC_ERROR_CLIENT_NOT_TRUSTED;
            case 63 : return KDC_ERROR_KDC_NOT_TRUSTED;
            case 64 : return KDC_ERROR_INVALID_SIG;
            case 65 : return KDC_ERR_KEY_TOO_WEAK;
            case 66 : return KDC_ERR_CERTIFICATE_MISMATCH;
            case 67 : return KRB_AP_ERR_NO_TGT;
            case 68 : return KDC_ERR_WRONG_REALM;
            case 69 : return KRB_AP_ERR_USER_TO_USER_REQUIRED;
            case 70 : return KDC_ERR_CANT_VERIFY_CERTIFICATE;
            case 71 : return KDC_ERR_INVALID_CERTIFICATE;
            case 72 : return KDC_ERR_REVOKED_CERTIFICATE;
            case 73 : return KDC_ERR_REVOCATION_STATUS_UNKNOWN;
            case 74 : return KDC_ERR_REVOCATION_STATUS_UNAVAILABLE;
            case 75 : return KDC_ERR_CLIENT_NAME_MISMATCH;
            case 76 : return KDC_ERR_KDC_NAME_MISMATCH;
	    	default : return NULL;
    	}
    }


    /**
     * Returns the number associated with this authorization type.
     *
     * @return The authorization type ordinal.
     */
    public int getOrdinal()
    {
        return ordinal;
    }
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
    	switch ( this )
    	{
            case KDC_ERR_NONE : 
                return "No error" + "(" + ordinal + ")";
            
            case KDC_ERR_NAME_EXP : 
                return "Client's entry in database has expired" + "(" + ordinal + ")";
            
            case KDC_ERR_SERVICE_EXP : 
                return "Server's entry in database has expired" + "(" + ordinal + ")";
            
            case KDC_ERR_BAD_PVNO : 
                return "Requested protocol version number not supported" + "(" + ordinal + ")";
            
            case KDC_ERR_C_OLD_MAST_KVNO : 
                return "Client's key encrypted in old master key" + "(" + ordinal + ")";
            
            case KDC_ERR_S_OLD_MAST_KVNO : 
                return "Server's key encrypted in old master key" + "(" + ordinal + ")";
            
            case KDC_ERR_C_PRINCIPAL_UNKNOWN : 
                return "Client not found in Kerberos database" + "(" + ordinal + ")";
            
            case KDC_ERR_S_PRINCIPAL_UNKNOWN : 
                return "Server not found in Kerberos database" + "(" + ordinal + ")";
            
            case KDC_ERR_PRINCIPAL_NOT_UNIQUE : 
                return "Multiple principal entries in database" + "(" + ordinal + ")";
            
            case KDC_ERR_NULL_KEY : 
                return "The client or server has a null key" + "(" + ordinal + ")";
            
            case KDC_ERR_CANNOT_POSTDATE : 
                return "Ticket not eligible for postdating" + "(" + ordinal + ")";
            
            case KDC_ERR_NEVER_VALID : 
                return "Requested starttime is later than end time" + "(" + ordinal + ")";
            
            case KDC_ERR_POLICY : 
                return "KDC policy rejects request" + "(" + ordinal + ")";
            
            case KDC_ERR_BADOPTION : 
                return "KDC cannot accommodate requested option" + "(" + ordinal + ")";
            
            case KDC_ERR_ETYPE_NOSUPP : 
                return "KDC has no support for encryption type" + "(" + ordinal + ")";
            
            case KDC_ERR_SUMTYPE_NOSUPP : 
                return "KDC has no support for checksum type" + "(" + ordinal + ")";
            
            case KDC_ERR_PADATA_TYPE_NOSUPP : 
                return "KDC has no support for padata type" + "(" + ordinal + ")";
            
            case KDC_ERR_TRTYPE_NOSUPP : 
                return "KDC has no support for transited type" + "(" + ordinal + ")";
            
            case KDC_ERR_CLIENT_REVOKED : 
                return "Clients credentials have been revoked" + "(" + ordinal + ")";
            
            case KDC_ERR_SERVICE_REVOKED : 
                return "Credentials for server have been revoked" + "(" + ordinal + ")";
            
            case KDC_ERR_TGT_REVOKED : 
                return "TGT has been revoked" + "(" + ordinal + ")";
            
            case KDC_ERR_CLIENT_NOTYET : 
                return "Client not yet valid; try again later" + "(" + ordinal + ")";
            
            case KDC_ERR_SERVICE_NOTYET : 
                return "Server not yet valid; try again later" + "(" + ordinal + ")";
            
            case KDC_ERR_KEY_EXPIRED : 
                return "Password has expired; change password to reset" + "(" + ordinal + ")";
            
            case KDC_ERR_PREAUTH_FAILED : 
                return "Pre-authentication information was invalid" + "(" + ordinal + ")";
            
            case KDC_ERR_PREAUTH_REQUIRED : 
                return "Additional pre-authentication required" + "(" + ordinal + ")";
            
            case KDC_ERR_SERVER_NOMATCH : 
                return "Requested server and ticket don't match" + "(" + ordinal + ")";
            
            case KDC_ERR_MUST_USE_USER2USER : 
                return "Server principal valid for user2user only" + "(" + ordinal + ")";
            
            case KDC_ERR_PATH_NOT_ACCEPTED : 
                return "KDC Policy rejects transited path" + "(" + ordinal + ")";
            
            case KDC_ERR_SVC_UNAVAILABLE : 
                return "A service is not available" + "(" + ordinal + ")";
            
            case KRB_AP_ERR_BAD_INTEGRITY : 
                return "Integrity check on decrypted field failed" + "(" + ordinal + ")";
            
            case KRB_AP_ERR_TKT_EXPIRED : 
                return "Ticket expired" + "(" + ordinal + ")";
            
            case KRB_AP_ERR_TKT_NYV : 
                return "Ticket not yet valid" + "(" + ordinal + ")";
            
            case KRB_AP_ERR_REPEAT : 
                return "Request is a replay" + "(" + ordinal + ")";
            
            case KRB_AP_ERR_NOT_US : 
                return "The ticket isn't for us" + "(" + ordinal + ")";
            
            case KRB_AP_ERR_BADMATCH : 
                return "Ticket and authenticator don't match" + "(" + ordinal + ")";
            
            case KRB_AP_ERR_SKEW : 
                return "Clock skew too great" + "(" + ordinal + ")";
            
            case KRB_AP_ERR_BADADDR : 
                return "Incorrect net address" + "(" + ordinal + ")";
            
            case KRB_AP_ERR_BADVERSION : 
                return "Protocol version mismatch" + "(" + ordinal + ")";
            
            case KRB_AP_ERR_MSG_TYPE : 
                return "Invalid msg type" + "(" + ordinal + ")";
            
            case KRB_AP_ERR_MODIFIED : 
                return "Message stream modified" + "(" + ordinal + ")";
            
            case KRB_AP_ERR_BADORDER : 
                return "Message out of order" + "(" + ordinal + ")";
            
            case KRB_AP_ERR_BADKEYVER : 
                return "Specified version of key is not available" + "(" + ordinal + ")";
            
            case KRB_AP_ERR_NOKEY : 
                return "Service key not available" + "(" + ordinal + ")";
            
            case KRB_AP_ERR_MUT_FAIL : 
                return "Mutual authentication failed" + "(" + ordinal + ")";
            
            case KRB_AP_ERR_BADDIRECTION : 
                return "Incorrect message direction" + "(" + ordinal + ")";
            
            case KRB_AP_ERR_METHOD : 
                return "Alternative authentication method required" + "(" + ordinal + ")";
            
            case KRB_AP_ERR_BADSEQ : 
                return "Incorrect sequence number in message" + "(" + ordinal + ")";
            
            case KRB_AP_ERR_INAPP_CKSUM : 
                return "Inappropriate type of checksum in message" + "(" + ordinal + ")";
            
            case KRB_AP_PATH_NOT_ACCEPTED : 
                return "Policy rejects transited path" + "(" + ordinal + ")";
            
            case KRB_ERR_RESPONSE_TOO_BIG : 
                return "Response too big for UDP; retry with TCP" + "(" + ordinal + ")";
            
            case KRB_ERR_GENERIC : 
                return "Generic error (description in e-text)" + "(" + ordinal + ")";
            
            case KRB_ERR_FIELD_TOOLONG : 
                return "Field is too long for this implementation" + "(" + ordinal + ")";
            
            case KDC_ERROR_CLIENT_NOT_TRUSTED : 
                return "Reserved for PKINIT" + "(" + ordinal + ")";
            
            case KDC_ERROR_KDC_NOT_TRUSTED : 
                return "Reserved for PKINIT" + "(" + ordinal + ")";
            
            case KDC_ERROR_INVALID_SIG : 
                return "Reserved for PKINIT" + "(" + ordinal + ")";
            
            case KDC_ERR_KEY_TOO_WEAK : 
                return "Reserved for PKINIT" + "(" + ordinal + ")";
            
            case KDC_ERR_CERTIFICATE_MISMATCH : 
                return "Reserved for PKINIT" + "(" + ordinal + ")";
            
            case KRB_AP_ERR_NO_TGT : 
                return "No TGT available to validate USER-TO-USER" + "(" + ordinal + ")";
            
            case KDC_ERR_WRONG_REALM : 
                return "Reserved for future use" + "(" + ordinal + ")";
            
            case KRB_AP_ERR_USER_TO_USER_REQUIRED : 
                return "Ticket must be for USER-TO-USER" + "(" + ordinal + ")";
            
            case KDC_ERR_CANT_VERIFY_CERTIFICATE : 
                return "Reserved for PKINIT" + "(" + ordinal + ")";
            
            case KDC_ERR_INVALID_CERTIFICATE :
                return "Reserved for PKINIT" + "(" + ordinal + ")";
            
            case KDC_ERR_REVOKED_CERTIFICATE : 
                return "Reserved for PKINIT" + "(" + ordinal + ")";
            
            case KDC_ERR_REVOCATION_STATUS_UNKNOWN : 
                return "Reserved for PKINIT" + "(" + ordinal + ")";
            
            case KDC_ERR_REVOCATION_STATUS_UNAVAILABLE : 
                return "Reserved for PKINIT" + "(" + ordinal + ")";
            
            case KDC_ERR_CLIENT_NAME_MISMATCH : 
                return "Reserved for PKINIT" + "(" + ordinal + ")";
            
            case KDC_ERR_KDC_NAME_MISMATCH : 
                return "Reserved for PKINIT" + "(" + ordinal + ")";

            default : 
    			return "null" + "(" + ordinal + ")";
    	}
    }
}
