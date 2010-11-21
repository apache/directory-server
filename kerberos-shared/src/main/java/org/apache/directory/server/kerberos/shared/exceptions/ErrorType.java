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
package org.apache.directory.server.kerberos.shared.exceptions;




/**
 * A type-safe enumeration of Kerberos error types.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum ErrorType
{
 
    // TODO Add i18n. Don't no if these error messages are also a response to the client.
    // If so shall they really be i18n?
    
    /**
     * No error.
     */
    KDC_ERR_NONE ( 0, "No error" ),

    /**
     * Client's entry in database has expired.
     */
    KDC_ERR_NAME_EXP ( 1, "Client's entry in database has expired" ),

    /**
     * Server's entry in database has expired.
     */
    KDC_ERR_SERVICE_EXP ( 2, "Server's entry in database has expired" ),

    /**
     * Requested protocol version number not supported.
     */
    KDC_ERR_BAD_PVNO ( 3,
        "Requested protocol version number not supported" ),

    /**
     * Client's key encrypted in old master key.
     */
    KDC_ERR_C_OLD_MAST_KVNO ( 4,
        "Client's key encrypted in old master key" ),

    /**
     * Server's key encrypted in old master key.
     */
    KDC_ERR_S_OLD_MAST_KVNO ( 5,
        "Server's key encrypted in old master key" ),

    /**
     * Client not found in Kerberos database.
     */
    KDC_ERR_C_PRINCIPAL_UNKNOWN ( 6,
        "Client not found in Kerberos database" ),

    /**
     * Server not found in Kerberos database.
     */
    KDC_ERR_S_PRINCIPAL_UNKNOWN ( 7,
        "Server not found in Kerberos database" ),

    /**
     * Multiple principal entries in database.
     */
    KDC_ERR_PRINCIPAL_NOT_UNIQUE ( 8,
        "Multiple principal entries in database" ),

    /**
     * The client or server has a null key.
     */
    KDC_ERR_NULL_KEY ( 9, "The client or server has a null key" ),

    /**
     * Ticket not eligible for postdating.
     */
    KDC_ERR_CANNOT_POSTDATE ( 10, "Ticket not eligible for postdating" ),

    /**
     * Requested start time is later than end time.
     */
    KDC_ERR_NEVER_VALID ( 11,
        "Requested start time is later than end time" ),

    /**
     * KDC policy rejects request.
     */
    KDC_ERR_POLICY ( 12, "KDC policy rejects request" ),

    /**
     * KDC cannot accommodate requested option.
     */
    KDC_ERR_BADOPTION ( 13, "KDC cannot accommodate requested option" ),

    /**
     * KDC has no support for encryption type.
     */
    KDC_ERR_ETYPE_NOSUPP ( 14, "KDC has no support for encryption type" ),

    /**
     * KDC has no support for checksum type.
     */
    KDC_ERR_SUMTYPE_NOSUPP ( 15, "KDC has no support for checksum type" ),

    /**
     * KDC has no support for padata type.
     */
    KDC_ERR_PADATA_TYPE_NOSUPP ( 16, "KDC has no support for padata type" ),

    /**
     * KDC has no support for transited type.
     */
    KDC_ERR_TRTYPE_NOSUPP ( 17, "KDC has no support for transited type" ),

    /**
     * Clients credentials have been revoked.
     */
    KDC_ERR_CLIENT_REVOKED ( 18, "Clients credentials have been revoked" ),

    /**
     * Credentials for server have been revoked.
     */
    KDC_ERR_SERVICE_REVOKED ( 19,
        "Credentials for server have been revoked" ),

    /**
     * TGT has been revoked.
     */
    KDC_ERR_TGT_REVOKED ( 20, "TGT has been revoked" ),

    /**
     * Client not yet valid; try again later.
     */
    KDC_ERR_CLIENT_NOTYET ( 21, "Client not yet valid; try again later" ),

    /**
     * Server not yet valid; try again later.
     */
    KDC_ERR_SERVICE_NOTYET ( 22, "Server not yet valid; try again later" ),

    /**
     * Password has expired, change password to reset.
     */
    KDC_ERR_KEY_EXPIRED ( 23,
        "Password has expired; change password to reset" ),

    /**
     * Pre-authentication information was invalid.
     */
    KDC_ERR_PREAUTH_FAILED ( 24,
        "Pre-authentication information was invalid" ),

    /**
     * Additional pre-authentication required.
     */
    KDC_ERR_PREAUTH_REQUIRED ( 25,
        "Additional pre-authentication required" ),

    /**
     * Requested server and ticket don't match.
     */
    KDC_ERR_SERVER_NOMATCH ( 26, "Requested server and ticket don't match" ),

    /**
     * Server valid for user2user only.
     */
    KDC_ERR_MUST_USE_USER2USER ( 27, "Server valid for user2user only" ),

    /**
     * KDC Policy rejects transited path.
     */
    KDC_ERR_PATH_NOT_ACCEPTED ( 28, "KDC Policy rejects transited path" ),

    /**
     * A service is not available.
     */
    KDC_ERR_SVC_UNAVAILABLE ( 29, "A service is not available" ),

    /**
     * Integrity check on decrypted field failed.
     */
    KRB_AP_ERR_BAD_INTEGRITY ( 31,
        "Integrity check on decrypted field failed" ),

    /**
     * Ticket expired.
     */
    KRB_AP_ERR_TKT_EXPIRED ( 32, "Ticket expired" ),

    /**
     * Ticket not yet valid.
     */
    KRB_AP_ERR_TKT_NYV ( 33, "Ticket not yet valid" ),

    /**
     * Request is a replay.
     */
    KRB_AP_ERR_REPEAT ( 34, "Request is a replay" ),

    /**
     * The ticket isn't for us.
     */
    KRB_AP_ERR_NOT_US ( 35, "The ticket isn't for us" ),

    /**
     * Ticket and authenticator don't match.
     */
    KRB_AP_ERR_BADMATCH ( 36, "Ticket and authenticator don't match" ),

    /**
     * Clock skew too great.
     */
    KRB_AP_ERR_SKEW ( 37, "Clock skew too great" ),

    /**
     * Incorrect net address.
     */
    KRB_AP_ERR_BADADDR ( 38, "Incorrect net address" ),

    /**
     * Protocol version mismatch.
     */
    KRB_AP_ERR_BADVERSION ( 39, "Protocol version mismatch" ),

    /**
     * Invalid msg type.
     */
    KRB_AP_ERR_MSG_TYPE ( 40, "Invalid msg type" ),

    /**
     * Message stream modified.
     */
    KRB_AP_ERR_MODIFIED ( 41, "Message stream modified" ),

    /**
     * Message out of order.
     */
    KRB_AP_ERR_BADORDER ( 42, "Message out of order" ),

    /**
     * Specified version of key is not available.
     */
    KRB_AP_ERR_BADKEYVER ( 44, "Specified version of key is not available" ),

    /**
     * Service key not available.
     */
    KRB_AP_ERR_NOKEY ( 45, "Service key not available" ),

    /**
     * Mutual authentication failed.
     */
    KRB_AP_ERR_MUT_FAIL ( 46, "Mutual authentication failed" ),

    /**
     * Incorrect message direction.
     */
    KRB_AP_ERR_BADDIRECTION ( 47, "Incorrect message direction" ),

    /**
     * Alternative authentication method required.
     */
    KRB_AP_ERR_METHOD ( 48, "Alternative authentication method required" ),

    /**
     * Incorrect sequence number in message.
     */
    KRB_AP_ERR_BADSEQ ( 49, "Incorrect sequence number in message" ),

    /**
     * Inappropriate type of checksum in message.
     */
    KRB_AP_ERR_INAPP_CKSUM ( 50,
        "Inappropriate type of checksum in message" ),

    /**
     * Policy rejects transited path.
     */
    KRB_AP_PATH_NOT_ACCEPTED ( 51, "Policy rejects transited path" ),

    /**
     * Response too big for UDP; retry with TCP.
     */
    KRB_ERR_RESPONSE_TOO_BIG ( 52,
        "Response too big for UDP; retry with TCP" ),

    /**
     * Generic error (description in e-text).
     */
    KRB_ERR_GENERIC ( 60, "Generic error (description in e-text)" ),

    /**
     * Field is too long for this implementation.
     */
    KRB_ERR_FIELD_TOOLONG ( 61,
        "Field is too long for this implementation" ),

    /**
     * Client is not trusted.
     */
    KDC_ERR_CLIENT_NOT_TRUSTED ( 62, "Client is not trusted" ),

    /**
     * KDC is not trusted.
     */
    KRB_ERR_KDC_NOT_TRUSTED ( 63, "KDC is not trusted" ),

    /**
     * Signature is invalid.
     */
    KDC_ERR_INVALID_SIG ( 64, "Signature is invalid" ),

    /**
     * Diffie-Hellman (DH) key parameters not accepted.
     */
    KDC_ERR_DH_KEY_PARAMETERS_NOT_ACCEPTED ( 65,
        "Diffie-Hellman (DH) key parameters not accepted." ),

    /**
     * Certificates do not match.
     */
    KRB_ERR_CERTIFICATE_MISMATCH ( 66, "Certificates do not match" ),

    /**
     * No TGT available to validate USER-TO-USER.
     */
    KRB_AP_ERR_NO_TGT ( 67, "No TGT available to validate USER-TO-USER" ),

    /**
     * Wrong realm.
     */
    KRB_ERR_WRONG_REALM ( 68, "Wrong realm" ),

    /**
     * Ticket must be for USER-TO-USER.
     */
    KRB_AP_ERR_USER_TO_USER_REQUIRED ( 69,
        "Ticket must be for USER-TO-USER" ),

    /**
     * Can't verify certificate.
     */
    KDC_ERR_CANT_VERIFY_CERTIFICATE ( 70, "Can't verify certificate" ),

    /**
     * Invalid certificate.
     */
    KDC_ERR_INVALID_CERTIFICATE ( 71, "Invalid certificate" ),

    /**
     * Revoked certificate.
     */
    KDC_ERR_REVOKED_CERTIFICATE ( 72, "Revoked certificate" ),

    /**
     * Revocation status unknown.
     */
    KDC_ERR_REVOCATION_STATUS_UNKNOWN ( 73, "Revocation status unknown" ),

    /**
     * Revocation status unavailable.
     */
    KRB_ERR_REVOCATION_STATUS_UNAVAILABLE ( 74,
        "Revocation status unavailable" ),

    /**
     * Client names do not match.
     */
    KDC_ERR_CLIENT_NAME_MISMATCH ( 75, "Client names do not match" ),

    /**
     * KDC names do not match.
     */
    KRB_ERR_KDC_NAME_MISMATCH ( 76, "KDC names do not match" ),

    /**
     * Inconsistent key purpose.
     */
    KDC_ERR_INCONSISTENT_KEY_PURPOSE ( 77, "Inconsistent key purpose" ),

    /**
     * Digest in certificate not accepted.
     */
    KDC_ERR_DIGEST_IN_CERT_NOT_ACCEPTED ( 78,
        "Digest in certificate not accepted" ),

    /**
     * PA checksum must be included.
     */
    KDC_ERR_PA_CHECKSUM_MUST_BE_INCLUDED ( 79,
        "PA checksum must be included" ),

    /**
     * Digest in signed data not accepted.
     */
    KDC_ERR_DIGEST_IN_SIGNED_DATA_NOT_ACCEPTED ( 80,
        "Digest in signed data not accepted" ),

    /**
     * Public key encryption not supported.
     */
    KDC_ERR_PUBLIC_KEY_ENCRYPTION_NOT_SUPPORTED ( 81,
        "Public key encryption not supported" );

    /**
     * The name of the error type.
     */
    private String name;

    /**
     * The value/code for the error type.
     */
    private int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private ErrorType( int ordinal, String name )
    {
        this.ordinal = ordinal;
        this.name = name;
    }


    /**
     * Returns the message for this Kerberos error.
     *
     * @return the message for this Kerberos error.
     */
    public String getMessage()
    {
        return name;
    }


    /**
     * Returns the message for this Kerberos error.
     *
     * @return the message for this Kerberos error.
     */
    public String toString()
    {
        return name;
    }


    /**
     * Gets the ordinal by its ordinal value.
     *
     * @param ordinal the ordinal value of the ordinal
     * @return the type corresponding to the ordinal value
     */
    public static ErrorType getTypeByOrdinal( int ordinal )
    {
        for ( ErrorType et : ErrorType.values() ) 
        {
            if ( ordinal == et.getOrdinal() )
            {
                return et;
            }
        }

        return KRB_ERR_GENERIC;
    }


    /**
     * Gets the ordinal value associated with this Kerberos error.
     *
     * @return the ordinal value associated with this Kerberos error
     */
    public int getOrdinal()
    {
        return ordinal;
    }
}
