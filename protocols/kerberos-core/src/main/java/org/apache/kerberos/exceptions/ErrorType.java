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
package org.apache.kerberos.exceptions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Type safe enumeration of Kerberos error types
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public final class ErrorType implements Comparable
{
	/*
	 * Enumeration elements are constructed once upon class loading.
	 * Order of appearance here determines the order of compareTo.
	 */
    public static final ErrorType KDC_ERR_NONE = new ErrorType(0,
    	"No error");
    public static final ErrorType KDC_ERR_NAME_EXP = new ErrorType(1,
    	"Client's entry in database expired");
    public static final ErrorType KDC_ERR_SERVICE_EXP = new ErrorType(2,
    	"Server's entry in database has expired");
    public static final ErrorType KDC_ERR_BAD_PVNO = new ErrorType(3,
    	"Requested protocol version number not supported");
    public static final ErrorType KDC_ERR_C_OLD_MAST_KVNO = new ErrorType(4,
    	"Client's key encrypted in old master key");
    public static final ErrorType KDC_ERR_S_OLD_MAST_KVNO = new ErrorType(5,
    	"Server's key encrypted in old master key");
    public static final ErrorType KDC_ERR_C_PRINCIPAL_UNKNOWN = new ErrorType(6,
    	"Client not found in Kerberos database");
    public static final ErrorType KDC_ERR_S_PRINCIPAL_UNKNOWN = new ErrorType(7,
    	"Server not found in Kerberos database");
    public static final ErrorType KDC_ERR_PRINCIPAL_NOT_UNIQUE = new ErrorType(8,
    	"Multiple principal entries in database");
    public static final ErrorType KDC_ERR_NULL_KEY = new ErrorType(9,
    	"The client or server has a null key");
    public static final ErrorType KDC_ERR_CANNOT_POSTDATE = new ErrorType(10,
    	"Ticket not eligible for postdating");
    public static final ErrorType KDC_ERR_NEVER_VALID = new ErrorType(11,
    	"Requested start time is later than end time");
    public static final ErrorType KDC_ERR_POLICY = new ErrorType(12,
    	"KDC policy rejects request");
    public static final ErrorType KDC_ERR_BADOPTION = new ErrorType(13,
    	"KDC cannot accommodate requested option");
    public static final ErrorType KDC_ERR_ETYPE_NOSUPP = new ErrorType(14,
    	"KDC has no support for encryption type");
    public static final ErrorType KDC_ERR_SUMTYPE_NOSUPP = new ErrorType(15,
    	"KDC has no support for checksum type");
    public static final ErrorType KDC_ERR_PADATA_TYPE_NOSUPP = new ErrorType(16,
    	"KDC has no support for padata type");
    public static final ErrorType KDC_ERR_TRTYPE_NOSUPP = new ErrorType(17,
    	"KDC has no support for transitedEncoding type");
    public static final ErrorType KDC_ERR_CLIENT_REVOKED = new ErrorType(18,
    	"Clients credentials have been revoked");
    public static final ErrorType KDC_ERR_SERVICE_REVOKED = new ErrorType(19,
    	"Credentials for server have been revoked");
    public static final ErrorType KDC_ERR_TGT_REVOKED = new ErrorType(20,
    	"TGT has been revoked");
    public static final ErrorType KDC_ERR_CLIENT_NOTYET = new ErrorType(21,
    	"Client not yet valid - try again later");
    public static final ErrorType KDC_ERR_SERVICE_NOTYET = new ErrorType(22,
    	"Server not yet valid - try again later");
    public static final ErrorType KDC_ERR_KEY_EXPIRED = new ErrorType(23,
    	"Password has expired - change password to reset");
    public static final ErrorType KDC_ERR_PREAUTH_FAILED = new ErrorType(24,
    	"Pre-authentication information was invalid");
    public static final ErrorType KDC_ERR_PREAUTH_REQUIRED = new ErrorType(25,
    	"Additional pre-authentication required");
    public static final ErrorType KDC_ERR_SERVER_NOMATCH = new ErrorType(26,
    	"Requested server and ticket don't match");
    public static final ErrorType KDC_ERR_MUST_USE_USER2USER = new ErrorType(27,
    	"Server valid for user2user only");
    public static final ErrorType KDC_ERR_PATH_NOT_ACCEPTED = new ErrorType(28,
    	"KDC Policy rejects transitedEncoding path");
    public static final ErrorType KDC_ERR_SVC_UNAVAILABLE = new ErrorType(29,
    	"A service is not available");
    public static final ErrorType KRB_AP_ERR_BAD_INTEGRITY = new ErrorType(31,
    	"Integrity check on decrypted field failed");
    public static final ErrorType KRB_AP_ERR_TKT_EXPIRED = new ErrorType(32,
    	"Ticket expired");
    public static final ErrorType KRB_AP_ERR_TKT_NYV = new ErrorType(33,
    	"Ticket not yet valid");
    public static final ErrorType KRB_AP_ERR_REPEAT = new ErrorType(34,
    	"Request is a replay");
    public static final ErrorType KRB_AP_ERR_NOT_US = new ErrorType(35,
    	"The ticket isn't for us");
    public static final ErrorType KRB_AP_ERR_BADMATCH = new ErrorType(36,
    	"Ticket and authenticator don't match");
    public static final ErrorType KRB_AP_ERR_SKEW = new ErrorType(37,
    	"Clock skew too great");
    public static final ErrorType KRB_AP_ERR_BADADDR = new ErrorType(38,
    	"Incorrect net address");
    public static final ErrorType KRB_AP_ERR_BADVERSION = new ErrorType(39,
    	"Protocol version mismatch");
    public static final ErrorType KRB_AP_ERR_MSG_TYPE = new ErrorType(40,
    	"Invalid msg type");
    public static final ErrorType KRB_AP_ERR_MODIFIED = new ErrorType(41,
    	"Message stream modified");
    public static final ErrorType KRB_AP_ERR_BADORDER = new ErrorType(42,
    	"Message out of order");
    public static final ErrorType KRB_AP_ERR_BADKEYVER = new ErrorType(44,
    	"Specified version of key is not available");
    public static final ErrorType KRB_AP_ERR_NOKEY = new ErrorType(45,
    	"Service key not available");
    public static final ErrorType KRB_AP_ERR_MUT_FAIL = new ErrorType(46,
    	"Mutual authentication failed");
    public static final ErrorType KRB_AP_ERR_BADDIRECTION = new ErrorType(47,
    	"Incorrect message direction");
    public static final ErrorType KRB_AP_ERR_METHOD = new ErrorType(48,
    	"Alternative authentication method required");
    public static final ErrorType KRB_AP_ERR_BADSEQ = new ErrorType(49,
    	"Incorrect sequence number in message");
    public static final ErrorType KRB_AP_ERR_INAPP_CKSUM = new ErrorType(50,
    	"Inappropriate type of checksum in message");
    public static final ErrorType KRB_ERR_GENERIC = new ErrorType(60,
    	"Generic error (description in e-text)");
    public static final ErrorType KRB_ERR_FIELD_TOOLONG = new ErrorType(61,
    	"Field is too long for this implementation");
    public static final ErrorType KRB_ERR_CLIENT_NOT_TRUSTED = new ErrorType(62,
    	"Client is not trusted");
    public static final ErrorType KRB_ERR_KDC_NOT_TRUSTED = new ErrorType(63,
    	"KDC is not trusted");
    public static final ErrorType KRB_ERR_INVALID_SIG = new ErrorType(64,
    	"Signature is invalid");
    public static final ErrorType KRB_ERR_KEY_TOO_WEAK = new ErrorType(65,
    	"Key too weak");
    public static final ErrorType KRB_ERR_CERTIFICATE_MISMATCH = new ErrorType(66,
    	"Certificates do not match");
    public static final ErrorType KRB_AP_ERR_NO_TGT = new ErrorType(67,
    	"No tgt for user-to-user authentication");
    public static final ErrorType KRB_ERR_WRONG_REALM = new ErrorType(68,
    	"Wrong realm");
    public static final ErrorType KRB_AP_ERR_USER_TO_USER_REQUIRED = new ErrorType(
    	69, "User-to-user authentication required");
    public static final ErrorType KRB_ERR_CANT_VERIFY_CERTIFICATE = new ErrorType(
    	70, "Can't verify certificate");
    public static final ErrorType KRB_ERR_INVALID_CERTIFICATE = new ErrorType(71,
    	"Invalid certificate");
    public static final ErrorType KRB_ERR_REVOKED_CERTIFICATE = new ErrorType(72,
    	"Revoked certificate");
    public static final ErrorType KRB_ERR_REVOCATION_STATUS_UNKNOWN = new ErrorType(
    	73, "Revocation status unknown");
    public static final ErrorType KRB_ERR_REVOCATION_STATUS_UNAVAILABLE = new ErrorType(
    	74, "Revocation status unavailable");
    public static final ErrorType KRB_ERR_CLIENT_NAME_MISMATCH = new ErrorType(75,
    	"Client names do not match");
    public static final ErrorType KRB_ERR_KDC_NAME_MISMATCH = new ErrorType(76,
    	"KDC names do not match");

    /** Array for building a List of VALUES. */
    private static final ErrorType[] values = {
            KDC_ERR_NONE,
            KDC_ERR_NAME_EXP,
            KDC_ERR_SERVICE_EXP,
            KDC_ERR_BAD_PVNO,
            KDC_ERR_C_OLD_MAST_KVNO,
            KDC_ERR_S_OLD_MAST_KVNO,
            KDC_ERR_C_PRINCIPAL_UNKNOWN,
            KDC_ERR_S_PRINCIPAL_UNKNOWN,
            KDC_ERR_PRINCIPAL_NOT_UNIQUE,
            KDC_ERR_NULL_KEY,
            KDC_ERR_CANNOT_POSTDATE,
            KDC_ERR_NEVER_VALID,
            KDC_ERR_POLICY,
            KDC_ERR_BADOPTION,
            KDC_ERR_ETYPE_NOSUPP,
            KDC_ERR_SUMTYPE_NOSUPP,
            KDC_ERR_PADATA_TYPE_NOSUPP,
            KDC_ERR_TRTYPE_NOSUPP,
            KDC_ERR_CLIENT_REVOKED,
            KDC_ERR_SERVICE_REVOKED,
            KDC_ERR_TGT_REVOKED,
            KDC_ERR_CLIENT_NOTYET,
            KDC_ERR_SERVICE_NOTYET,
            KDC_ERR_KEY_EXPIRED,
            KDC_ERR_PREAUTH_FAILED,
            KDC_ERR_PREAUTH_REQUIRED,
            KDC_ERR_SERVER_NOMATCH,
            KDC_ERR_MUST_USE_USER2USER,
            KDC_ERR_PATH_NOT_ACCEPTED,
            KDC_ERR_SVC_UNAVAILABLE,
            KRB_AP_ERR_BAD_INTEGRITY,
            KRB_AP_ERR_TKT_EXPIRED,
            KRB_AP_ERR_TKT_NYV,
            KRB_AP_ERR_REPEAT,
            KRB_AP_ERR_NOT_US,
            KRB_AP_ERR_BADMATCH,
            KRB_AP_ERR_SKEW,
            KRB_AP_ERR_BADADDR,
            KRB_AP_ERR_BADVERSION,
            KRB_AP_ERR_MSG_TYPE,
            KRB_AP_ERR_MODIFIED,
            KRB_AP_ERR_BADORDER,
            KRB_AP_ERR_BADKEYVER,
            KRB_AP_ERR_NOKEY,
            KRB_AP_ERR_MUT_FAIL,
            KRB_AP_ERR_BADDIRECTION,
            KRB_AP_ERR_METHOD,
            KRB_AP_ERR_BADSEQ,
            KRB_AP_ERR_INAPP_CKSUM,
            KRB_ERR_GENERIC,
            KRB_ERR_FIELD_TOOLONG,
            KRB_ERR_CLIENT_NOT_TRUSTED,
            KRB_ERR_KDC_NOT_TRUSTED,
            KRB_ERR_INVALID_SIG,
            KRB_ERR_KEY_TOO_WEAK,
            KRB_ERR_CERTIFICATE_MISMATCH,
            KRB_AP_ERR_NO_TGT,
            KRB_ERR_WRONG_REALM,
            KRB_AP_ERR_USER_TO_USER_REQUIRED,
            KRB_ERR_CANT_VERIFY_CERTIFICATE,
            KRB_ERR_INVALID_CERTIFICATE,
            KRB_ERR_REVOKED_CERTIFICATE,
            KRB_ERR_REVOCATION_STATUS_UNKNOWN,
            KRB_ERR_REVOCATION_STATUS_UNAVAILABLE,
            KRB_ERR_CLIENT_NAME_MISMATCH,
            KRB_ERR_KDC_NAME_MISMATCH
    };

    /** a list of all the error type constants */
    public static final List VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    /** the name of the error type */
    private final String name;

    /** the value/code for the error type */
    private final int ordinal;

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
     * Compares this type to another object hopefully one that is of the same
     * type.
     *
     * @param that the object to compare this KerberosError to
     * @return ordinal - ( ( KerberosError ) that ).ordinal;
     */
    public int compareTo( Object that )
    {
        return ordinal - ( (ErrorType) that ).ordinal;
    }

    /**
     * Gets the ordinal by its ordinal value.
     *
     * @param ordinal the ordinal value of the ordinal
     * @return the type corresponding to the ordinal value
     */
    public static ErrorType getTypeByOrdinal( int ordinal )
    {
        for ( int ii = 0; ii < values.length; ii++ )
        {
            if ( values[ ii ].ordinal == ordinal )
            {
                return values[ ii ];
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
