/*
 *   Copyright 2004 The Apache Software Foundation
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
package org.apache.directory.shared.ldap.message ;


import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Iterator;
import javax.naming.*;
import javax.naming.directory.*;

import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.util.ValuedEnum;


/**
 * Type safe LDAP message envelope result code enumeration.  The resultCode is
 * a parameter of the LDAPResult which is the construct used in this protocol
 * to return success or failure indications from servers to clients.  In
 * response to various requests servers will return responses containing
 * fields of type LDAPResult to indicate the final status of a protocol
 * operation request. This enumeration represents the various status codes
 * associated with an LDAPResult, hence it is called the ResultCodeEnum. Here
 * are the definitions and values for error codes from section 4.1.10 of <a
 * href="http://www.faqs.org/rfcs/rfc2251.html">RFC 2251</a>:
 * <pre><code>
 *    resultCode
 *       ENUMERATED {
 *          success                      (0),
 *          operationsError              (1),
 *          protocolError                (2),
 *          timeLimitExceeded            (3),
 *          sizeLimitExceeded            (4),
 *          compareFalse                 (5),
 *          compareTrue                  (6),
 *          authMethodNotSupported       (7),
 *          strongAuthRequired           (8),
 *          partialResults               (9),   -- new
 *          referral                     (10),  -- new
 *          adminLimitExceeded           (11),  -- new
 *          unavailableCriticalExtension (12),  -- new
 *          confidentialityRequired      (13),  -- new
 *          saslBindInProgress           (14),  -- new
 *          noSuchAttribute              (16),
 *          undefinedAttributeType       (17),
 *          inappropriateMatching        (18),
 *          constraintViolation          (19),
 *          attributeOrValueExists       (20),
 *          invalidAttributeSyntax       (21),
 *          -- 22-31 unused --
 *          noSuchObject                 (32),
 *          aliasProblem                 (33),
 *          invalidDNSyntax              (34),
 *          -- 35 reserved for undefined isLeaf --
 *          aliasDereferencingProblem    (36),
 *          -- 37-47 unused --
 *          inappropriateAuthentication  (48),
 *          invalidCredentials           (49),
 *          insufficientAccessRights     (50),
 *          busy                         (51),
 *          unavailable                  (52),
 *          unwillingToPerform           (53),
 *          loopDetect                   (54),
 *          -- 55-63 unused --
 *          namingViolation              (64),
 *          objectClassViolation         (65),
 *          notAllowedOnNonLeaf          (66),
 *          notAllowedOnRDN              (67),
 *          entryAlreadyExists           (68),
 *          objectClassModsProhibited    (69),
 *          -- 70 reserved for CLDAP --
 *          affectsMultipleDSAs          (71), -- new
 *          -- 72-79 unused --
 *          other                        (80) },
 *          -- 81-90 reserved for APIs --
 *  </code></pre>
 * All the result codes with the exception of success, compareFalse and
 * compareTrue are to be treated as meaning the operation could not be
 * completed in its entirety. Most of the result codes are based on problem
 * indications from X.511 error data types.  Result codes from 16 to 21
 * indicate an AttributeProblem, codes 32, 33, 34 and 36 indicate a
 * NameProblem, codes 48, 49 and 50 indicate a SecurityProblem, codes 51 to 54
 * indicate a ServiceProblem, and codes 64 to 69 and 71 indicates an
 * UpdateProblem. If a client receives a result code which is not listed
 * above, it is to be treated as an unknown error condition. The majority of
 * this javadoc was pasted in from RFC 2251.  There's and expired draft out
 * there on error codes which makes alot of sense: <a
 * href="http://www.alternic.org/drafts/drafts-j-k/draft-just-ldapv3-rescodes-
 * 02.html"> ietf (expired) draft</a> on error codes (read at your discretion).
 * Result codes have been identified and split into categories:
 * 
 * <ul>
 * <li>
 * Non-Erroneous: Five result codes that may be returned in LDAPResult are not
 * used to indicate an error.
 * </li>
 * <li>
 * General: returned only when no suitable specific error exists.
 * </li>
 * <li>
 * Specific: Specific errors are used to indicate that a particular type of
 * error has occurred.  These error types are:
 * 
 * <ul>
 * <li>
 * Name,
 * </li>
 * <li>
 * Update,
 * </li>
 * <li>
 * Attribute
 * </li>
 * <li>
 * Security, and
 * </li>
 * <li>
 * Service
 * </li>
 * </ul>
 * </li>
 * </ul>
 * 
 * The result codes are also grouped according to the following LDAP operations
 * which return responses:
 * 
 * <ul>
 * <li>
 * bind
 * </li>
 * <li>
 * search
 * </li>
 * <li>
 * modify
 * </li>
 * <li>
 * modifyDn
 * </li>
 * <li>
 * add
 * </li>
 * <li>
 * delete
 * </li>
 * <li>
 * compare
 * </li>
 * <li>
 * extended
 * </li>
 * </ul>
 * 
 *
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision$
 */
public class ResultCodeEnum extends ValuedEnum
{
    static final long serialVersionUID = -6813787847504596968L;
    public static final int SUCCESS_VAL = 0;
    public static final int OPERATIONSERROR_VAL = 1;
    public static final int PROTOCOLERROR_VAL = 2;
    public static final int TIMELIMITEXCEEDED_VAL = 3;
    public static final int SIZELIMITEXCEEDED_VAL = 4;
    public static final int COMPAREFALSE_VAL = 5;
    public static final int COMPARETRUE_VAL = 6;
    public static final int AUTHMETHODNOTSUPPORTED_VAL = 7;
    public static final int STRONGAUTHREQUIRED_VAL = 8;
    public static final int PARTIALRESULTS_VAL = 9;
    public static final int REFERRAL_VAL = 10;
    public static final int ADMINLIMITEXCEEDED_VAL = 11;
    public static final int UNAVAILABLECRITICALEXTENSION_VAL = 12;
    public static final int CONFIDENTIALITYREQUIRED_VAL = 13;
    public static final int SASLBINDINPROGRESS_VAL = 14;

    // -- 15 unused --

    public static final int NOSUCHATTRIBUTE_VAL = 16;
    public static final int UNDEFINEDATTRIBUTETYPE_VAL = 17;
    public static final int INAPPROPRIATEMATCHING_VAL = 18;
    public static final int CONSTRAINTVIOLATION_VAL = 19;
    public static final int ATTRIBUTEORVALUEEXISTS_VAL = 20;
    public static final int INVALIDATTRIBUTESYNTAX_VAL = 21;

    // -- 22-31 unused --

    public static final int NOSUCHOBJECT_VAL = 32;
    public static final int ALIASPROBLEM_VAL = 33;
    public static final int INVALIDDNSYNTAX_VAL = 34;

    // -- 35 reserved for undefined isLeaf --

    public static final int ALIASDEREFERENCINGPROBLEM_VAL = 36;

    // -- 37-47 unused --

    public static final int INAPPROPRIATEAUTHENTICATION_VAL = 48;
    public static final int INVALIDCREDENTIALS_VAL = 49;
    public static final int INSUFFICIENTACCESSRIGHTS_VAL = 50;
    public static final int BUSY_VAL = 51;
    public static final int UNAVAILABLE_VAL = 52;
    public static final int UNWILLINGTOPERFORM_VAL = 53;
    public static final int LOOPDETECT_VAL = 54;

    // -- 55-63 unused --

    public static final int NAMINGVIOLATION_VAL = 64;
    public static final int NOTALLOWEDONNONLEAF_VAL = 66;
    public static final int OBJECTCLASSVIOLATION_VAL = 65;
    public static final int NOTALLOWEDONRDN_VAL = 67;
    public static final int ENTRYALREADYEXISTS_VAL = 68;
    public static final int OBJECTCLASSMODSPROHIBITED_VAL = 69;

    // -- 70 reserved for CLDAP --

    public static final int AFFECTSMULTIPLEDSAS_VAL = 71;

    // -- 72-79 unused --

    public static final int OTHER_VAL = 80;

    // -- 81-90 reserved for APIs --


    // ------------------------------------------------------------------------
    // Public Static Constants: Enumeration values and names.
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Non Erroneous Codes:
    //
    // Five result codes that may be returned in LDAPResult are not used to
    // indicate an error.  These result codes are listed below.  The first
    // three codes, indicate to the client that no further action is required
    // in order to satisfy their request.  In contrast, the last two errors
    // require further action by the client in order to complete their original
    // operation request.
    // ------------------------------------------------------------------------


    /**
     * Servers sends this result code to LDAP v2 clients to refer them to
     * another LDAP server. When sending this code to a client, the server
     * includes a newline-delimited list of LDAP URLs that identify another
     * LDAP server.  If the client identifies itself as an LDAP v3 client in
     * the request, servers send an REFERRAL result code instead of this
     * result code.
     */
    public static final ResultCodeEnum PARTIALRESULTS =
            new ResultCodeEnum( "PARTIALRESULTS", PARTIALRESULTS_VAL ) ;

    /**
     * It is returned when the client operation completed successfully without
     * errors.   This code is one of 5 result codes that may be returned in
     * the LDAPResult which are not used to indicate an error. Applicable
     * operations: all except for Compare. Result code type: Non-Erroneous
     */
    public static final ResultCodeEnum SUCCESS =
            new ResultCodeEnum( "SUCCESS", SUCCESS_VAL ) ;

    /**
     * It is used to indicate that the result of a Compare operation is FALSE
     * and does not indicate an error.  1 of 5 codes that do not indicate an
     * error condition. Applicable operations: Compare. Result code type:
     * Non-Erroneous
     */
    public static final ResultCodeEnum COMPAREFALSE = 
        new ResultCodeEnum( "COMPAREFALSE", COMPAREFALSE_VAL ) ;

    /**
     * It is used to indicate that the result of a Compare operation is TRUE
     * and does not indicate an error.  1 of 5 codes that do not indicate an
     * error condition. Applicable operations: Compare. Result code type:
     * Non-Erroneous
     */
    public static final ResultCodeEnum COMPARETRUE = 
        new ResultCodeEnum( "COMPARETRUE", COMPARETRUE_VAL ) ;

    /**
     * Rather than indicating an error, this result code is used to indicate
     * that the server does not hold the target entry of the request but is
     * able to provide alternative servers that may.  A set of server(s) URLs
     * may be returned in the referral field, which the client may
     * subsequently query to attempt to complete their operation.  1 of 5
     * codes that do not indicate an error condition yet requires further
     * action on behalf of the client to complete the request.  This result
     * code is new in LDAPv3. Applicable operations: all. Result code type:
     * Non-Erroneous
     */
    public static final ResultCodeEnum REFERRAL = 
        new ResultCodeEnum( "REFERRAL", REFERRAL_VAL ) ;

    /**
     * This result code is not an error response from the server, but rather,
     * is a request for bind continuation.  The server requires the client to
     * send a new bind request, with the same SASL mechanism, to continue the
     * authentication process [RFC2251, Section 4.2.3].  This result code is
     * new in LDAPv3. Applicable operations: Bind. Result code type:
     * Non-Erroneous
     */
    public static final ResultCodeEnum SASLBINDINPROGRESS = 
        new ResultCodeEnum( "SASLBINDINPROGRESS", SASLBINDINPROGRESS_VAL ) ;

    // ------------------------------------------------------------------------
    // Problem Specific Error Codes:
    //
    // Specific errors are used to indicate that a particular type of error
    // has occurred.  These error types are Name, Update, Attribute, Security,
    // and Service.
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Security Problem Specific Error Codes:
    //
    // A security error reports a problem in carrying out an operation for
    // security reasons [X511, Section 12.7].
    // ------------------------------------------------------------------------

    /**
     * This error code should be returned if the client requests, in a Bind
     * request, an authentication method which is not supported or recognized
     * by the server. Applicable operations: Bind. Result code type: Specific
     * (Security)
     */
    public static final ResultCodeEnum AUTHMETHODNOTSUPPORTED = 
        new ResultCodeEnum( "AUTHMETHODNOTSUPPORTED", AUTHMETHODNOTSUPPORTED_VAL ) ;

    /**
     * This error may be returned on a bind request if the server only accepts
     * strong authentication or it may be returned when a client attempts an
     * operation which requires the client to be strongly authenticated - for
     * example Delete. This result code may also be returned in an unsolicited
     * notice of disconnection if the server detects that an established
     * underlying security association protecting communication between the
     * client and server has unexpectedly failed or been compromised.
     * [RFC2251, Section 4.4.1] Applicable operations: all. Result code type:
     * Specific (Security)
     */
    public static final ResultCodeEnum STRONGAUTHREQUIRED = 
        new ResultCodeEnum( "STRONGAUTHREQUIRED", STRONGAUTHREQUIRED_VAL ) ;

    /**
     * This error code may be returned if the session is not protected by a
     * protocol which provides session confidentiality. For example, if the
     * client did not establish a TLS connection using a cipher suite which
     * provides confidentiality of the session before sending any other
     * requests, and the server requires session confidentiality then the
     * server may reject that request with a result code of
     * confidentialityRequired.  This error code is new in LDAPv3. Applicable
     * operations: all. Result code type: Specific (Security)
     */
    public static final ResultCodeEnum CONFIDENTIALITYREQUIRED = 
        new ResultCodeEnum( "CONFIDENTIALITYREQUIRED", CONFIDENTIALITYREQUIRED_VAL ) ;

    /**
     * An alias was encountered in a situation where it was not allowed or
     * where access was denied [X511, Section 12.5]. For example, if the
     * client does not have read permission for the aliasedObjectName
     * attribute and its value then the error aliasDereferencingProblem should
     * be returned. [X511, Section 7.11.1.1] Notice that this error has
     * similar meaning to INSUFFICIENTACCESSRIGHTS (50), but is specific to
     * Searching on an alias. Applicable operations: Search. Result code type:
     * Specific (Security)
     */
    public static final ResultCodeEnum ALIASDEREFERENCINGPROBLEM = 
        new ResultCodeEnum( "ALIASDEREFERENCINGPROBLEM", ALIASDEREFERENCINGPROBLEM_VAL ) ;

    /**
     * This error should be returned by the server when the client has tried to
     * use a method of authentication that is inappropriate, that is a method
     * of authentication which the client is unable to use correctly. In other
     * words, the level of security associated with the requestor's
     * credentials is inconsistent with the level of protection requested,
     * e.g. simple credentials were supplied while strong credentials were
     * required [X511, Section 12.7]. Applicable operations: Bind. Result code
     * type: Specific (Security)
     */
    public static final ResultCodeEnum INAPPROPRIATEAUTHENTICATION = 
        new ResultCodeEnum( "INAPPROPRIATEAUTHENTICATION", INAPPROPRIATEAUTHENTICATION_VAL ) ;

    /**
     * This error code is returned if the DN or password used in a simple bind
     * operation is incorrect, or if the DN or password is incorrect for some
     * other reason, e.g. the password has expired.  This result code only
     * applies to Bind operations -- it should not be returned for other
     * operations if the client does not have sufficient permission to perform
     * the requested operation - in this case the return code should be
     * insufficientAccessRights. Applicable operations: Bind. Result code
     * type: Specific (Security)
     */
    public static final ResultCodeEnum INVALIDCREDENTIALS = 
        new ResultCodeEnum( "INVALIDCREDENTIALS", INVALIDCREDENTIALS_VAL ) ;

    /**
     * The requestor does not have the right to carry out the requested
     * operation [X511, Section 12.7]. Note that the more specific
     * aliasDereferencingProblem is returned in case of a Search on an alias
     * where the requestor has insufficientAccessRights. Applicable
     * operations: all except for Bind. Result code type: Specific (Security)
     */
    public static final ResultCodeEnum INSUFFICIENTACCESSRIGHTS = 
            new ResultCodeEnum( "INSUFFICIENTACCESSRIGHTS", INSUFFICIENTACCESSRIGHTS_VAL ) ;

    // ------------------------------------------------------------------------
    // Service Problem Specific Error Codes:
    //
    // A service error reports a problem related to the provision of the
    // service [X511, Section 12.8].
    // ------------------------------------------------------------------------

    /**
     * If the server requires that the client bind before browsing or modifying
     * the directory, the server MAY reject a request other than binding,
     * unbinding or an extended request with the "operationsError" result.
     * [RFC2251, Section 4.2.1] Applicable operations: all except Bind. Result
     * code type: Specific (Service)
     */
    public static final ResultCodeEnum OPERATIONSERROR = 
        new ResultCodeEnum( "OPERATIONSERROR", OPERATIONSERROR_VAL ) ;

    /**
     * A protocol error should be returned by the server when an invalid or
     * malformed request is received from the client. This may be a request
     * that is not recognized as an LDAP request, for example, if a
     * nonexistent operation were specified in LDAPMessage.  As well, it may
     * be the result of a request that is missing a required parameter, such
     * as a search filter in a search request. If the server can return an
     * error, which is more specific than protocolError, then this error
     * should be returned instead. For example if the server does not
     * recognize the authentication method requested by the client then the
     * error authMethodNotSupported should be returned instead of
     * protocolError. The server may return details of the error in the error
     * string. Applicable operations: all. Result code type: Specific
     * (Service)
     */
    public static final ResultCodeEnum PROTOCOLERROR = 
        new ResultCodeEnum( "PROTOCOLERROR", PROTOCOLERROR_VAL ) ;

    /**
     * This error should be returned when the time to perform an operation has
     * exceeded either the time limit specified by the client (which may only
     * be set by the client in a search operation) or the limit specified by
     * the server.  If the time limit is exceeded on a search operation then
     * the result is an arbitrary selection of the accumulated results [X511,
     * Section 7.5].  Note that an arbitrary selection of results may mean
     * that no results are returned to the client. If the LDAP server is a
     * front end for an X.500 server, any operation that is chained may exceed
     * the timelimit, therefore clients can expect to receive
     * timelimitExceeded for all operations. For stand alone LDAP- Servers
     * that do not implement chaining it is unlikely that operations other
     * than search operations will exceed the defined timelimit.  Applicable
     * operations: all. Result code type: Specific (Service)
     */
    public static final ResultCodeEnum TIMELIMITEXCEEDED = 
        new ResultCodeEnum( "TIMELIMITEXCEEDED", TIMELIMITEXCEEDED_VAL ) ;

    /**
     * This error should be returned when the number of results generated by a
     * search exceeds the maximum number of results specified by either the
     * client or the server. If the size limit is exceeded then the results of
     * a search operation will be an arbitrary selection of the accumulated
     * results, equal in number to the size limit [X511, Section 7.5].
     * Applicable operations: Search. Result code type: Specific (Service)
     */
    public static final ResultCodeEnum SIZELIMITEXCEEDED = 
        new ResultCodeEnum( "SIZELIMITEXCEEDED", SIZELIMITEXCEEDED_VAL ) ;

    /**
     * The server has reached some limit set by an administrative authority,
     * and no partial results are available to return to the user [X511,
     * Section 12.8].  For example, there may be an administrative limit to
     * the number of entries a server will check when gathering potential
     * search result candidates [Net].  This error code is new in LDAPv3.
     * Applicable operations: all. Result code type: Specific (Service)
     */
    public static final ResultCodeEnum ADMINLIMITEXCEEDED = 
        new ResultCodeEnum( "ADMINLIMITEXCEEDED", ADMINLIMITEXCEEDED_VAL ) ;

    /**
     * The server was unable to satisfy the request because one or more
     * critical extensions were not available [X511, Section 12.8]. This error
     * is returned, for example, when a control submitted with a request is
     * marked critical but is not recognized by a server or when such a
     * control is not appropriate for the operation type. [RFC2251 section
     * 4.1.12].  This error code is new in LDAPv3. Applicable operations: all.
     * Result code type: Specific (Service)
     */
    public static final ResultCodeEnum UNAVAILABLECRITICALEXTENSION = 
        new ResultCodeEnum( "UNAVAILABLECRITICALEXTENSION", UNAVAILABLECRITICALEXTENSION_VAL ) ;

    /**
     * This error code may be returned if the server is unable to process the
     * client's request at this time. This implies that if the client retries
     * the request shortly the server will be able to process it then.
     * Applicable operations: all. Result code type: Specific (Service)
     */
    public static final ResultCodeEnum BUSY = new ResultCodeEnum( "BUSY", BUSY_VAL ) ;

    /**
     * This error code is returned when the server is unavailable to process
     * the client's request. This usually means that the LDAP server is
     * shutting down [RFC2251, Section 4.2.3]. Applicable operations: all.
     * Result code type: Specific (Service)
     */
    public static final ResultCodeEnum UNAVAILABLE = 
        new ResultCodeEnum( "UNAVAILABLE", UNAVAILABLE_VAL ) ;

    /**
     * This error code should be returned by the server when a client request
     * is properly formed but which the server is unable to complete due to
     * server-defined restrictions.  For example, the server, or some part of
     * it, is not prepared to execute this request, e.g. because it would lead
     * to excessive consumption of resources or violates the policy of an
     * Administrative Authority involved [X511, Section 12.8]. If the server
     * is able to return a more specific error code such as adminLimitExceeded
     * it should. This error may also be returned if the client attempts to
     * modify attributes which can not be modified by users, e.g., operational
     * attributes such as creatorsName or createTimestamp [X511, Section
     * 7.12]. If appropriate, details of the error should be provided in the
     * error message. Applicable operations: all. Result code type: Specific
     * (Service)
     */
    public static final ResultCodeEnum UNWILLINGTOPERFORM = 
        new ResultCodeEnum( "UNWILLINGTOPERFORM", UNWILLINGTOPERFORM_VAL ) ;

    /**
     * This error may be returned by the server if it detects an alias or
     * referral loop, and is unable to satisfy the client's request.
     * Applicable operations: all. Result code type: Specific (Service)
     */
    public static final ResultCodeEnum LOOPDETECT = 
        new ResultCodeEnum( "LOOPDETECT", LOOPDETECT_VAL ) ;

    // ------------------------------------------------------------------------
    // Attribute Problem Specific Error Codes:
    //
    // An attribute error reports a problem related to an attribute specified
    // by the client in their request message.
    // ------------------------------------------------------------------------

    /**
     * This error may be returned if the attribute specified as an argument of
     * the operation does not exist in the entry. Applicable operations:
     * Modify, Compare. Result code type: Specific (Attribute)
     */
    public static final ResultCodeEnum NOSUCHATTRIBUTE = 
        new ResultCodeEnum( "NOSUCHATTRIBUTE", NOSUCHATTRIBUTE_VAL ) ;

    /**
     * This error may be returned if the specified attribute is unrecognized by
     * the server, since it is not present in the server's defined schema. If
     * the server doesn't recognize an attribute specified in a search request
     * as the attribute to be returned the server should not return an error
     * in this case - it should just return values for the requested
     * attributes it does recognize. Note that this result code only applies
     * to the Add and Modify operations [X.511, Section 12.4]. Applicable
     * operations: Modify, Add. Result code type: Specific (Attribute)
     */
    public static final ResultCodeEnum UNDEFINEDATTRIBUTETYPE = 
        new ResultCodeEnum( "UNDEFINEDATTRIBUTETYPE", UNDEFINEDATTRIBUTETYPE_VAL ) ;

    /**
     * An attempt was made, e.g., in a filter, to use a matching rule not
     * defined for the attribute type concerned [X511, Section 12.4].
     * Applicable operations: Search. Result code type: Specific (Attribute)
     */
    public static final ResultCodeEnum INAPPROPRIATEMATCHING = 
        new ResultCodeEnum( "INAPPROPRIATEMATCHING", INAPPROPRIATEMATCHING_VAL ) ;

    /**
     * This error should be returned by the server if an attribute value
     * specified by the client violates the constraints placed on the
     * attribute as it was defined in the DSA - this may be a size constraint
     * or a constraint on the content. Applicable operations: Modify, Add,
     * ModifyDN. Result code type: Specific (Attribute)
     */
    public static final ResultCodeEnum CONSTRAINTVIOLATION = 
        new ResultCodeEnum( "CONSTRAINTVIOLATION", CONSTRAINTVIOLATION_VAL ) ;

    /**
     * This error should be returned by the server if the value specified by
     * the client already exists within the attribute. Applicable operations:
     * Modify, Add. Result code type: Specific (Attribute)
     */
    public static final ResultCodeEnum ATTRIBUTEORVALUEEXISTS = 
        new ResultCodeEnum( "ATTRIBUTEORVALUEEXISTS", ATTRIBUTEORVALUEEXISTS_VAL ) ;

    /**
     * This error should be returned by the server if the attribute syntax for
     * the attribute value, specified as an argument of the operation, is
     * unrecognized or invalid. Applicable operations: Modify, Add. Result
     * code type: Specific (Attribute)
     */
    public static final ResultCodeEnum INVALIDATTRIBUTESYNTAX = 
        new ResultCodeEnum( "INVALIDATTRIBUTESYNTAX", INVALIDATTRIBUTESYNTAX_VAL ) ;

    // ------------------------------------------------------------------------
    // Name Problem Specific Error Codes:
    //
    // A name error reports a problem related to the distinguished name
    // provided as an argument to an operation [X511, Section 12.5].
    //
    // For result codes of noSuchObject, aliasProblem, invalidDNSyntax and
    // aliasDereferencingProblem (see Section 5.2.2.3.7), the matchedDN
    // field is set to the name of the lowest entry (object or alias) in the
    // directory that was matched.  If no aliases were dereferenced while
    // attempting to locate the entry, this will be a truncated form of the
    // name provided, or if aliases were dereferenced, of the resulting
    // name, as defined in section 12.5 of X.511 [X511]. The matchedDN field
    // is to be set to a zero length string with all other result codes
    // [RFC2251, Section 4.1.10].
    // ------------------------------------------------------------------------

    /**
     * This error should only be returned if the target object cannot be found.
     * For example, in a search operation if the search base can not be
     * located in the DSA the server should return noSuchObject. If, however,
     * the search base is found but does not match the search filter, success,
     * with no resultant objects, should be returned instead of noSuchObject.
     * If the LDAP server is a front end for an X.500 DSA then noSuchObject
     * may also be returned if discloseOnError is not granted for an entry and
     * the client does not have permission to view or modify the entry.
     * Applicable operations: all except for Bind. Result code type: Specific
     * (Name)
     */
    public static final ResultCodeEnum NOSUCHOBJECT = 
        new ResultCodeEnum( "NOSUCHOBJECT", NOSUCHOBJECT_VAL ) ;

    /**
     * An alias has been dereferenced which names no object [X511, Section
     * 12.5] Applicable operations: Search. Result code type: Specific (Name)
     */
    public static final ResultCodeEnum ALIASPROBLEM = 
        new ResultCodeEnum( "ALIASPROBLEM", ALIASPROBLEM_VAL ) ;

    /**
     * This error should be returned by the server if the DN syntax is
     * incorrect. It should not be returned if the DN is correctly formed but
     * represents an entry which is not permitted by the structure rules at
     * the DSA ; in this case namingViolation should be returned instead.
     * Applicable operations: all. Result code type: Specific (Name)
     */
    public static final ResultCodeEnum INVALIDDNSYNTAX = 
        new ResultCodeEnum( "INVALIDDNSYNTAX", INVALIDDNSYNTAX_VAL ) ;

    // ------------------------------------------------------------------------
    // Update Problem Specific Error Codes:
    //
    // An update error reports problems related to attempts to add, delete, or
    // modify information in the DIB [X511, Section 12.9].
    // ------------------------------------------------------------------------

    /**
     * The attempted addition or modification would violate the structure rules
     * of the DIT as defined in the directory schema and X.501.  That is, it
     * would place an entry as the subordinate of an alias entry, or in a
     * region of the DIT not permitted to a member of its object class, or
     * would define an RDN for an entry to include a forbidden attribute type
     * [X511, Section 12.9]. Applicable operations: Add, ModifyDN. Result code
     * type: Specific (Update)
     */
    public static final ResultCodeEnum NAMINGVIOLATION = 
        new ResultCodeEnum( "NAMINGVIOLATION", NAMINGVIOLATION_VAL ) ;

    /**
     * This error should be returned if the operation requested by the user
     * would violate the objectClass requirements for the entry if carried
     * out. On an add or modify operation this would result from trying to add
     * an object class without a required attribute, or by trying to add an
     * attribute which is not permitted by the current object class set in the
     * entry. On a modify operation this may result from trying to remove a
     * required attribute without removing the associated auxiliary object
     * class, or by attempting to remove an object class while the attributes
     * it permits are still present. Applicable operations: Add, Modify,
     * ModifyDN. Result code type: Specific (Update)
     */
    public static final ResultCodeEnum OBJECTCLASSVIOLATION = 
        new ResultCodeEnum( "OBJECTCLASSVIOLATION", OBJECTCLASSVIOLATION_VAL ) ;

    /**
     * This error should be returned if the client attempts to perform an
     * operation which is permitted only on leaf entries - e.g., if the client
     * attempts to delete a non-leaf entry.  If the directory does not permit
     * ModifyDN for non-leaf entries then this error may be returned if the
     * client attempts to change the DN of a non-leaf entry. (Note that 1988
     * edition X.500 servers only permitted change of the RDN of an entry's DN
     * [X.511, Section 11.4.1]). Applicable operations: Delete, ModifyDN.
     * Result code type: Specific (Update)
     */
    public static final ResultCodeEnum NOTALLOWEDONNONLEAF = 
        new ResultCodeEnum( "NOTALLOWEDONNONLEAF", NOTALLOWEDONNONLEAF_VAL ) ;

    /**
     * The attempted operation would affect the RDN (e.g., removal of an
     * attribute which is a part of the RDN) [X511, Section 12.9].  If the
     * client attempts to remove from an entry any of its distinguished
     * values, those values which form the entry's relative distinguished name
     * the server should return the error notAllowedOnRDN. [RFC2251, Section
     * 4.6] Applicable operations: Modify. Result code type: Specific (Update)
     */
    public static final ResultCodeEnum NOTALLOWEDONRDN = 
        new ResultCodeEnum( "NOTALLOWEDONRDN", NOTALLOWEDONRDN_VAL ) ;

    /**
     * This error should be returned by the server when the client attempts to
     * add an entry which already exists, or if the client attempts to rename
     * an entry with the name of an entry which exists. Applicable operations:
     * Add, ModifyDN. Result code type: Specific (Update)
     */
    public static final ResultCodeEnum ENTRYALREADYEXISTS = 
        new ResultCodeEnum( "ENTRYALREADYEXISTS", ENTRYALREADYEXISTS_VAL ) ;

    /**
     * An operation attempted to modify an object class that should not be
     * modified, e.g., the structural object class of an entry.  Some servers
     * may not permit object class modifications, especially modifications to
     * the structural object class since this may change the entry entirely,
     * name forms, structure rules etc. [X.511, Section 12.9]. Applicable
     * operations: Modify. Result code type: Specific (Update)
     */
    public static final ResultCodeEnum OBJECTCLASSMODSPROHIBITED = 
        new ResultCodeEnum( "OBJECTCLASSMODSPROHIBITE", OBJECTCLASSMODSPROHIBITED_VAL ) ;

    /**
     * This error code should be returned to indicate that the operation could
     * not be performed since it affects more than one DSA. This error code is
     * new for LDAPv3. X.500 restricts the ModifyDN operation to only affect
     * entries that are contained within a single server. If the LDAP server
     * is mapped onto DAP, then this restriction will apply, and the
     * resultCode affectsMultipleDSAs will be returned if this error occurred.
     * In general clients MUST NOT expect to be able to perform arbitrary
     * movements of entries and subtrees between servers [RFC2251, Section
     * 4.9]. Applicable operations: ModifyDN. Result code type: Specific
     * (Update)
     */
    public static final ResultCodeEnum AFFECTSMULTIPLEDSAS = 
        new ResultCodeEnum( "AFFECTSMULTIPLEDSAS", AFFECTSMULTIPLEDSAS_VAL ) ;

    // ------------------------------------------------------------------------
    // General Error Codes:
    //
    // A general error code typically specifies an error condition for which
    // there is no suitable specific error code. If the server can return an
    // error, which is more specific than the following general errors, then
    // the specific error should be returned instead.
    // ------------------------------------------------------------------------

    /**
     * This error code should be returned only if no other error code is
     * suitable.  Use of this error code should be avoided if possible.
     * Details of the error should be provided in the error message.
     * Applicable operations: all. Result code type: General
     */
    public static final ResultCodeEnum OTHER = 
        new ResultCodeEnum( "OTHER", OTHER_VAL ) ;

    // ------------------------------------------------------------------------
    // Error Codes Grouped Into Categories & Static Accessors
    // ------------------------------------------------------------------------

    /**
     * This array holds the set of general error codes.  General error codes
     * are typically returned only when no suitable specific error exists.
     * Specific error codes are meant to capture situations that are specific
     * to the requested operation.  A general error code typically specifies
     * an error condition for which there is no suitable specific error code.
     * If the server can return an error, which is more specific than the
     * following general errors, then the specific error should be returned
     * instead.  This array only contains the OTHER error code at the present
     * time. The set contains:
     * <ul>
     *   <li><a href="OTHER">OTHER</a></li>
     * </ul>
     */
    public static final Set GENERAL_CODES = Collections.singleton( OTHER ) ;

    /**
     * Five result codes that may be returned in LDAPResult are not used to
     * indicate an error.  The first three codes, indicate to the client that
     * no further action is required in order to satisfy their request.  In
     * contrast, the last two errors require further action by the client in
     * order to complete their original operation request. The set contains:
     * <ul>
     *   <li><a href="#SUCCESS">SUCCESS</a></li>
     *   <li><a href="#COMPARETRUE">COMPARETRUE</a></li>
     *   <li><a href="#COMPAREFALSE">COMPAREFALSE</a></li>
     *   <li><a href="#REFERRAL">REFERRAL</a></li>
     *   <li><a href="#SASLBINDINPROGRESS">SASLBINDINPROGRESS</a></li>
     * </ul>
     */
    public static final Set NON_ERRONEOUS_CODES;
    static
    {
        HashSet set = new HashSet();
        set.add( SUCCESS );
        set.add( COMPARETRUE );
        set.add( COMPAREFALSE );
        set.add( REFERRAL );
        set.add( SASLBINDINPROGRESS );
        NON_ERRONEOUS_CODES = Collections.unmodifiableSet( set );
    }

    /**
     * Contains the set of error codes associated with attribute problems.  An
     * attribute error reports a problem related to an attribute specified by
     * the client in their request message. The set contains:
     * <ul>
     *   <li><a href="#NOSUCHATTRIBUTE">NOSUCHATTRIBUTE</a></li>
     *   <li><a href="#UNDEFINEDATTRIBUTETYPE">UNDEFINEDATTRIBUTETYPE</a></li>
     *   <li><a href="#INAPPROPRIATEMATCHING">INAPPROPRIATEMATCHING</a></li>
     *   <li><a href="#CONSTRAINTVIOLATION">CONSTRAINTVIOLATION</a></li>
     *   <li><a href="#ATTRIBUTEORVALUEEXISTS">ATTRIBUTEORVALUEEXISTS</a></li>
     *   <li><a href="#INVALIDATTRIBUTESYNTAX">INVALIDATTRIBUTESYNTAX</a></li>
     * </ul>
     */
    public static final Set ATTRIBUTE_CODES;
    static
    {
        HashSet set = new HashSet();
        set.add( NOSUCHATTRIBUTE );
        set.add( UNDEFINEDATTRIBUTETYPE );
        set.add( INAPPROPRIATEMATCHING );
        set.add( CONSTRAINTVIOLATION );
        set.add( ATTRIBUTEORVALUEEXISTS );
        set.add( INVALIDATTRIBUTESYNTAX );
        ATTRIBUTE_CODES = Collections.unmodifiableSet( set );
    }

    /**
     * Stores the set of error codes associated with name problems.  A name
     * error reports a problem related to the distinguished name provided as
     * an argument to an operation [X511, Section 12.5]. For result codes of
     * noSuchObject, aliasProblem, invalidDNSyntax and
     * aliasDereferencingProblem, the matchedDN field is set to the name of
     * the lowest entry (object or alias) in the directory that was matched.
     * If no aliases were dereferenced while attempting to locate the entry,
     * this will be a truncated form of the name provided, or if aliases were
     * dereferenced of the resulting name, as defined in section 12.5 of X.511
     * [X511]. The matchedDN field is to be set to a zero length string with
     * all other result codes [RFC2251, Section 4.1.10].
     * The set contains:
     * <ul>
     *   <li><a href="#NOSUCHOBJECT">NOSUCHOBJECT</a></li>
     *   <li><a href="#ALIASPROBLEM">ALIASPROBLEM</a></li>
     *   <li><a href="#INVALIDDNSYNTAX">INVALIDDNSYNTAX</a></li>
     * </ul>
     */
    public static final Set NAME_CODES;
    static
    {
        HashSet set = new HashSet();
        set.add( NOSUCHOBJECT );
        set.add( ALIASPROBLEM );
        set.add( INVALIDDNSYNTAX );
        NAME_CODES = Collections.unmodifiableSet( set );
    }

    /**
     * Stores all the result codes associated with security related problems. A
     * security error reports a problem in carrying out an operation for
     * security reasons [X511, Section 12.7]. The set contains:
     * <ul>
     *   <li><a href="#INVALIDCREDENTIALS">INVALIDCREDENTIALS</a></li>
     *   <li><a href="#STRONGAUTHREQUIRED">STRONGAUTHREQUIRED</a></li>
     *   <li><a href="#AUTHMETHODNOTSUPPORTED">AUTHMETHODNOTSUPPORTED</a></li>
     *   <li><a href="#CONFIDENTIALITYREQUIRED">CONFIDENTIALITYREQUIRED</a></li>
     *   <li><a href="#INSUFFICIENTACCESSRIGHTS">INSUFFICIENTACCESSRIGHTS</a></li>
     *   <li><a href="#ALIASDEREFERENCINGPROBLEM">ALIASDEREFERENCINGPROBLEM</a></li>
     *   <li><a href="#INAPPROPRIATEAUTHENTICATION">INAPPROPRIATEAUTHENTICATION</a></li>
     * </ul>
     */
    public static final Set SECURITY_CODES;
    static
    {
        HashSet set = new HashSet();
        set.add( INVALIDCREDENTIALS );
        set.add( STRONGAUTHREQUIRED );
        set.add( AUTHMETHODNOTSUPPORTED );
        set.add( CONFIDENTIALITYREQUIRED );
        set.add( INSUFFICIENTACCESSRIGHTS );
        set.add( ALIASDEREFERENCINGPROBLEM );
        set.add( INAPPROPRIATEAUTHENTICATION );
        SECURITY_CODES = Collections.unmodifiableSet( set );
    }

    /**
     * A service error reports a problem related to the provision of the
     * service [X511, Section 12.8].  This set stores all error codes
     * related to service problems.
     * The set contains:
     * <ul>
     *   <li><a href="#BUSY">BUSY</a></li>
     *   <li><a href="#LOOPDETECT">LOOPDETECT</a></li>
     *   <li><a href="#UNAVAILABLE">UNAVAILABLE</a></li>
     *   <li><a href="#PROTOCOLERROR">PROTOCOLERROR</a></li>
     *   <li><a href="#OPERATIONSERROR">OPERATIONSERROR</a></li>
     *   <li><a href="#TIMELIMITEXCEEDED">TIMELIMITEXCEEDED</a></li>
     *   <li><a href="#SIZELIMITEXCEEDED">SIZELIMITEXCEEDED</a></li>
     *   <li><a href="#ADMINLIMITEXCEEDED">ADMINLIMITEXCEEDED</a></li>
     *   <li><a href="#UNWILLINGTOPERFORM">UNWILLINGTOPERFORM</a></li>
     *   <li><a href="#UNAVAILABLECRITICALEXTENSION">UNAVAILABLECRITICALEXTENSION</a></li>
     * </ul>
     */
    public static final Set SERVICE_CODES;
    static
    {
        HashSet set = new HashSet();
        set.add( BUSY );
        set.add( LOOPDETECT );
        set.add( UNAVAILABLE );
        set.add( PROTOCOLERROR );
        set.add( OPERATIONSERROR );
        set.add( TIMELIMITEXCEEDED );
        set.add( SIZELIMITEXCEEDED );
        set.add( ADMINLIMITEXCEEDED );
        set.add( UNWILLINGTOPERFORM );
        set.add( UNAVAILABLECRITICALEXTENSION );
        SERVICE_CODES = Collections.unmodifiableSet( set );
    }

    /**
     * An update error reports problems related to attempts to add, delete, or
     * modify information in the DIB [X511, Section 12.9].  This set
     * contains the category of update errors.
     * <ul>
     *   <li><a href="#NAMINGVIOLATION">NAMINGVIOLATION</a></li>
     *   <li><a href="#OBJECTCLASSVIOLATION">OBJECTCLASSVIOLATION</a></li>
     *   <li><a href="#NOTALLOWEDONNONLEAF">NOTALLOWEDONNONLEAF</a></li>
     *   <li><a href="#NOTALLOWEDONRDN">NOTALLOWEDONRDN</a></li>
     *   <li><a href="#ENTRYALREADYEXISTS">ENTRYALREADYEXISTS</a></li>
     *   <li><a href="#OBJECTCLASSMODSPROHIBITED">OBJECTCLASSMODSPROHIBITED</a></li>
     *   <li><a href="#AFFECTSMULTIPLEDSAS">AFFECTSMULTIPLEDSAS</a></li>
     * </ul>
     */
    public static final Set UPDATE_CODES;
    static
    {
        HashSet set = new HashSet();
        set.add( NAMINGVIOLATION );
        set.add( OBJECTCLASSVIOLATION );
        set.add( NOTALLOWEDONNONLEAF );
        set.add( NOTALLOWEDONRDN );
        set.add( ENTRYALREADYEXISTS );
        set.add( OBJECTCLASSMODSPROHIBITED );
        set.add( AFFECTSMULTIPLEDSAS );
        UPDATE_CODES = Collections.unmodifiableSet( set );
    }

    // ------------------------------------------------------------------------
    // Result Codes Categorized by Request Type
    // ------------------------------------------------------------------------

    /**
     * A set of result code enumerations common to all operations.
     * The set contains:
     * <ul>
     *   <li><a href="#BUSY">BUSY</a></li>
     *   <li><a href="#OTHER">OTHER</a></li>
     *   <li><a href="#REFERRAL">REFERRAL</a></li>
     *   <li><a href="#LOOPDETECT">LOOPDETECT</a></li>
     *   <li><a href="#UNAVAILABLE">UNAVAILABLE</a></li>
     *   <li><a href="#PROTOCOLERROR">PROTOCOLERROR</a></li>
     *   <li><a href="#TIMELIMITEXCEEDED">TIMELIMITEXCEEDED</a></li>
     *   <li><a href="#ADMINLIMITEXCEEDED">ADMINLIMITEXCEEDED</a></li>
     *   <li><a href="#STRONGAUTHREQUIRED">STRONGAUTHREQUIRED</a></li>
     *   <li><a href="#UNWILLINGTOPERFORM">UNWILLINGTOPERFORM</a></li>
     *   <li><a href="#CONFIDENTIALITYREQUIRED">CONFIDENTIALITYREQUIRED</a></li>
     *   <li><a href="#UNAVAILABLECRITICALEXTENSION">UNAVAILABLECRITICALEXTENSION</a></li>
     * </ul>
     */
    public static final Set COMMON_CODES;
    static
    {
        HashSet set = new HashSet();
        set.add( BUSY );
        set.add( OTHER );
        set.add( REFERRAL );
        set.add( LOOPDETECT );
        set.add( UNAVAILABLE );
        set.add( PROTOCOLERROR );
        set.add( TIMELIMITEXCEEDED );
        set.add( ADMINLIMITEXCEEDED );
        set.add( STRONGAUTHREQUIRED );
        set.add( UNWILLINGTOPERFORM );
        set.add( CONFIDENTIALITYREQUIRED );
        set.add( UNAVAILABLECRITICALEXTENSION );
        COMMON_CODES = Collections.unmodifiableSet( set );
    }

    /**
     * A set of result code enumerations that may result from bind operations.
     * The set contains:
     * <ul>
     *   <li><a href="#BUSY">BUSY</a></li>
     *   <li><a href="#OTHER">OTHER</a></li>
     *   <li><a href="#SUCCESS">SUCCESS</a></li>
     *   <li><a href="#REFERRAL">REFERRAL</a></li>
     *   <li><a href="#LOOPDETECT">LOOPDETECT</a></li>
     *   <li><a href="#UNAVAILABLE">UNAVAILABLE</a></li>
     *   <li><a href="#PROTOCOLERROR">PROTOCOLERROR</a></li>
     *   <li><a href="#INVALIDDNSYNTAX">INVALIDDNSYNTAX</a></li>
     *   <li><a href="#TIMELIMITEXCEEDED">TIMELIMITEXCEEDED</a></li>
     *   <li><a href="#ADMINLIMITEXCEEDED">ADMINLIMITEXCEEDED</a></li>
     *   <li><a href="#UNWILLINGTOPERFORM">UNWILLINGTOPERFORM</a></li>
     *   <li><a href="#SASLBINDINPROGRESS">SASLBINDINPROGRESS</a></li>
     *   <li><a href="#STRONGAUTHREQUIRED">STRONGAUTHREQUIRED</a></li>
     *   <li><a href="#INVALIDCREDENTIALS">INVALIDCREDENTIALS</a></li>
     *   <li><a href="#AUTHMETHODNOTSUPPORTED">AUTHMETHODNOTSUPPORTED</a></li>
     *   <li><a href="#CONFIDENTIALITYREQUIRED">CONFIDENTIALITYREQUIRED</a></li>
     *   <li><a href="#INAPPROPRIATEAUTHENTICATION">INAPPROPRIATEAUTHENTICATION</a></li>
     *   <li><a href="#UNAVAILABLECRITICALEXTENSION">UNAVAILABLECRITICALEXTENSION</a></li>
     * </ul>
     */
    public static final Set BIND_CODES;
    static
    {
        HashSet set = new HashSet();
        set.add( BUSY );
        set.add( OTHER );
        set.add( SUCCESS );
        set.add( REFERRAL );
        set.add( LOOPDETECT );
        set.add( UNAVAILABLE );
        set.add( PROTOCOLERROR );
        set.add( INVALIDDNSYNTAX );
        set.add( TIMELIMITEXCEEDED );
        set.add( ADMINLIMITEXCEEDED );
        set.add( UNWILLINGTOPERFORM );
        set.add( SASLBINDINPROGRESS );
        set.add( STRONGAUTHREQUIRED );
        set.add( INVALIDCREDENTIALS );
        set.add( AUTHMETHODNOTSUPPORTED );
        set.add( CONFIDENTIALITYREQUIRED );
        set.add( INAPPROPRIATEAUTHENTICATION );
        set.add( UNAVAILABLECRITICALEXTENSION );
        BIND_CODES = Collections.unmodifiableSet( set );
    }

    /**
     * A set of result code enumerations that may result from search operations.
     * The set contains:
     * <ul>
     *   <li><a href="#BUSY">BUSY</a></li>
     *   <li><a href="#OTHER">OTHER</a></li>
     *   <li><a href="#SUCCESS">SUCCESS</a></li>
     *   <li><a href="#REFERRAL">REFERRAL</a></li>
     *   <li><a href="#LOOPDETECT">LOOPDETECT</a></li>
     *   <li><a href="#UNAVAILABLE">UNAVAILABLE</a></li>
     *   <li><a href="#NOSUCHOBJECT">NOSUCHOBJECT</a></li>
     *   <li><a href="#ALIASPROBLEM">ALIASPROBLEM</a></li>
     *   <li><a href="#PROTOCOLERROR">PROTOCOLERROR</a></li>
     *   <li><a href="#INVALIDDNSYNTAX">INVALIDDNSYNTAX</a></li>
     *   <li><a href="#SIZELIMITEXCEEDED">SIZELIMITEXCEEDED</a></li>
     *   <li><a href="#TIMELIMITEXCEEDED">TIMELIMITEXCEEDED</a></li>
     *   <li><a href="#ADMINLIMITEXCEEDED">ADMINLIMITEXCEEDED</a></li>
     *   <li><a href="#STRONGAUTHREQUIRED">STRONGAUTHREQUIRED</a></li>
     *   <li><a href="#UNWILLINGTOPERFORM">UNWILLINGTOPERFORM</a></li>
     *   <li><a href="#INAPPROPRIATEMATCHING">INAPPROPRIATEMATCHING</a></li>
     *   <li><a href="#CONFIDENTIALITYREQUIRED">CONFIDENTIALITYREQUIRED</a></li>
     *   <li><a href="#INSUFFICIENTACCESSRIGHTS">INSUFFICIENTACCESSRIGHTS</a></li>
     *   <li><a href="#ALIASDEREFERENCINGPROBLEM">ALIASDEREFERENCINGPROBLEM</a></li>
     *   <li><a href="#UNAVAILABLECRITICALEXTENSION">UNAVAILABLECRITICALEXTENSION</a></li>
     * </ul>
     */
    public static final Set SEARCH_CODES;
    static
    {
        HashSet set = new HashSet();
        set.add( BUSY );
        set.add( OTHER );
        set.add( SUCCESS );
        set.add( REFERRAL );
        set.add( LOOPDETECT );
        set.add( UNAVAILABLE );
        set.add( NOSUCHOBJECT );
        set.add( ALIASPROBLEM );
        set.add( PROTOCOLERROR );
        set.add( INVALIDDNSYNTAX );
        set.add( SIZELIMITEXCEEDED );
        set.add( TIMELIMITEXCEEDED );
        set.add( ADMINLIMITEXCEEDED );
        set.add( STRONGAUTHREQUIRED );
        set.add( UNWILLINGTOPERFORM );
        set.add( INAPPROPRIATEMATCHING );
        set.add( CONFIDENTIALITYREQUIRED );
        set.add( INSUFFICIENTACCESSRIGHTS );
        set.add( ALIASDEREFERENCINGPROBLEM );
        set.add( UNAVAILABLECRITICALEXTENSION );
        SEARCH_CODES = Collections.unmodifiableSet( set );
    }

    /**
     * A set of result code enumerations that may result from modify operations.
     * The set contains:
     * <ul>
     *   <li><a href="#BUSY">BUSY</a></li>
     *   <li><a href="#OTHER">OTHER</a></li>
     *   <li><a href="#SUCCESS">SUCCESS</a></li>
     *   <li><a href="#REFERRAL">REFERRAL</a></li>
     *   <li><a href="#LOOPDETECT">LOOPDETECT</a></li>
     *   <li><a href="#UNAVAILABLE">UNAVAILABLE</a></li>
     *   <li><a href="#NOSUCHOBJECT">NOSUCHOBJECT</a></li>
     *   <li><a href="#PROTOCOLERROR">PROTOCOLERROR</a></li>
     *   <li><a href="#INVALIDDNSYNTAX">INVALIDDNSYNTAX</a></li>
     *   <li><a href="#NOTALLOWEDONRDN">NOTALLOWEDONRDN</a></li>
     *   <li><a href="#NOSUCHATTRIBUTE">NOSUCHATTRIBUTE</a></li>
     *   <li><a href="#TIMELIMITEXCEEDED">TIMELIMITEXCEEDED</a></li>
     *   <li><a href="#ADMINLIMITEXCEEDED">ADMINLIMITEXCEEDED</a></li>
     *   <li><a href="#STRONGAUTHREQUIRED">STRONGAUTHREQUIRED</a></li>
     *   <li><a href="#UNWILLINGTOPERFORM">UNWILLINGTOPERFORM</a></li>
     *   <li><a href="#CONSTRAINTVIOLATION">CONSTRAINTVIOLATION</a></li>
     *   <li><a href="#OBJECTCLASSVIOLATION">OBJECTCLASSVIOLATION</a></li>
     *   <li><a href="#INVALIDATTRIBUTESYNTAX">INVALIDATTRIBUTESYNTAX</a></li>
     *   <li><a href="#UNDEFINEDATTRIBUTETYPE">UNDEFINEDATTRIBUTETYPE</a></li>
     *   <li><a href="#ATTRIBUTEORVALUEEXISTS">ATTRIBUTEORVALUEEXISTS</a></li>
     *   <li><a href="#CONFIDENTIALITYREQUIRED">CONFIDENTIALITYREQUIRED</a></li>
     *   <li><a href="#INSUFFICIENTACCESSRIGHTS">INSUFFICIENTACCESSRIGHTS</a></li>
     *   <li><a href="#OBJECTCLASSMODSPROHIBITED">OBJECTCLASSMODSPROHIBITED</a></li>
     *   <li><a href="#UNAVAILABLECRITICALEXTENSION">UNAVAILABLECRITICALEXTENSION</a></li>
     * </ul>
     */
    public static final Set MODIFY_CODES;
    static
    {
        HashSet set = new HashSet();
        set.add( BUSY );
        set.add( OTHER );
        set.add( SUCCESS );
        set.add( REFERRAL );
        set.add( LOOPDETECT );
        set.add( UNAVAILABLE );
        set.add( NOSUCHOBJECT );
        set.add( PROTOCOLERROR );
        set.add( INVALIDDNSYNTAX );
        set.add( NOTALLOWEDONRDN );
        set.add( NOSUCHATTRIBUTE );
        set.add( TIMELIMITEXCEEDED );
        set.add( ADMINLIMITEXCEEDED );
        set.add( STRONGAUTHREQUIRED );
        set.add( UNWILLINGTOPERFORM );
        set.add( CONSTRAINTVIOLATION );
        set.add( OBJECTCLASSVIOLATION );
        set.add( INVALIDATTRIBUTESYNTAX );
        set.add( UNDEFINEDATTRIBUTETYPE );
        set.add( ATTRIBUTEORVALUEEXISTS );
        set.add( CONFIDENTIALITYREQUIRED );
        set.add( INSUFFICIENTACCESSRIGHTS );
        set.add( OBJECTCLASSMODSPROHIBITED );
        set.add( UNAVAILABLECRITICALEXTENSION );
        MODIFY_CODES = Collections.unmodifiableSet( set );
    }

    /**
     * A set of result code enumerations that may result from add operations.
     * The set contains:
     * <ul>
     *   <li><a href="#BUSY">BUSY</a></li>
     *   <li><a href="#OTHER">OTHER</a></li>
     *   <li><a href="#SUCCESS">SUCCESS</a></li>
     *   <li><a href="#REFERRAL">REFERRAL</a></li>
     *   <li><a href="#LOOPDETECT">LOOPDETECT</a></li>
     *   <li><a href="#UNAVAILABLE">UNAVAILABLE</a></li>
     *   <li><a href="#NOSUCHOBJECT">NOSUCHOBJECT</a></li>
     *   <li><a href="#PROTOCOLERROR">PROTOCOLERROR</a></li>
     *   <li><a href="#NAMINGVIOLATION">NAMINGVIOLATION</a></li>
     *   <li><a href="#INVALIDDNSYNTAX">INVALIDDNSYNTAX</a></li>
     *   <li><a href="#TIMELIMITEXCEEDED">TIMELIMITEXCEEDED</a></li>
     *   <li><a href="#ADMINLIMITEXCEEDED">ADMINLIMITEXCEEDED</a></li>
     *   <li><a href="#STRONGAUTHREQUIRED">STRONGAUTHREQUIRED</a></li>
     *   <li><a href="#UNWILLINGTOPERFORM">UNWILLINGTOPERFORM</a></li>
     *   <li><a href="#ENTRYALREADYEXISTS">ENTRYALREADYEXISTS</a></li>
     *   <li><a href="#CONSTRAINTVIOLATION">CONSTRAINTVIOLATION</a></li>
     *   <li><a href="#OBJECTCLASSVIOLATION">OBJECTCLASSVIOLATION</a></li>
     *   <li><a href="#INVALIDATTRIBUTESYNTAX">INVALIDATTRIBUTESYNTAX</a></li>
     *   <li><a href="#ATTRIBUTEORVALUEEXISTS">ATTRIBUTEORVALUEEXISTS</a></li>
     *   <li><a href="#UNDEFINEDATTRIBUTETYPE">UNDEFINEDATTRIBUTETYPE</a></li>
     *   <li><a href="#CONFIDENTIALITYREQUIRED">CONFIDENTIALITYREQUIRED</a></li>
     *   <li><a href="#INSUFFICIENTACCESSRIGHTS">INSUFFICIENTACCESSRIGHTS</a></li>
     *   <li><a href="#UNAVAILABLECRITICALEXTENSION">UNAVAILABLECRITICALEXTENSION</a></li>
     * </ul>
     */
    public static final Set ADD_CODES;
    static
    {
        HashSet set = new HashSet();
        set.add( BUSY ); 
        set.add( OTHER ); 
        set.add( SUCCESS ); 
        set.add( REFERRAL ); 
        set.add( LOOPDETECT ); 
        set.add( UNAVAILABLE ); 
        set.add( NOSUCHOBJECT );
        set.add( PROTOCOLERROR ); 
        set.add( NAMINGVIOLATION ); 
        set.add( INVALIDDNSYNTAX ); 
        set.add( TIMELIMITEXCEEDED );
        set.add( ADMINLIMITEXCEEDED ); 
        set.add( STRONGAUTHREQUIRED ); 
        set.add( UNWILLINGTOPERFORM );
        set.add( ENTRYALREADYEXISTS ); 
        set.add( CONSTRAINTVIOLATION ); 
        set.add( OBJECTCLASSVIOLATION );
        set.add( INVALIDATTRIBUTESYNTAX ); 
        set.add( ATTRIBUTEORVALUEEXISTS ); 
        set.add( UNDEFINEDATTRIBUTETYPE );
        set.add( CONFIDENTIALITYREQUIRED ); 
        set.add( INSUFFICIENTACCESSRIGHTS );
        set.add( UNAVAILABLECRITICALEXTENSION );
        ADD_CODES = Collections.unmodifiableSet( set );
    } ;

    /**
     * A set of result code enumerations that may result from delete operations.
     * The set may contain:
     * <ul>
     *   <li><a href="#BUSY">BUSY</a></li>
     *   <li><a href="#OTHER">OTHER</a></li>
     *   <li><a href="#SUCCESS">SUCCESS</a></li>
     *   <li><a href="#REFERRAL">REFERRAL</a></li>
     *   <li><a href="#LOOPDETECT">LOOPDETECT</a></li>
     *   <li><a href="#UNAVAILABLE">UNAVAILABLE</a></li>
     *   <li><a href="#NOSUCHOBJECT">NOSUCHOBJECT</a></li>
     *   <li><a href="#PROTOCOLERROR">PROTOCOLERROR</a></li>
     *   <li><a href="#INVALIDDNSYNTAX">INVALIDDNSYNTAX</a></li>
     *   <li><a href="#TIMELIMITEXCEEDED">TIMELIMITEXCEEDED</a></li>
     *   <li><a href="#ADMINLIMITEXCEEDED">ADMINLIMITEXCEEDED</a></li>
     *   <li><a href="#STRONGAUTHREQUIRED">STRONGAUTHREQUIRED</a></li>
     *   <li><a href="#UNWILLINGTOPERFORM">UNWILLINGTOPERFORM</a></li>
     *   <li><a href="#NOTALLOWEDONNONLEAF">NOTALLOWEDONNONLEAF</a></li>
     *   <li><a href="#CONFIDENTIALITYREQUIRED">CONFIDENTIALITYREQUIRED</a></li>
     *   <li><a href="#INSUFFICIENTACCESSRIGHTS">INSUFFICIENTACCESSRIGHTS</a></li>
     *   <li><a href="#UNAVAILABLECRITICALEXTENSION">UNAVAILABLECRITICALEXTENSION</a></li>
     * </ul>
     */
    public static final Set DELETE_CODES;
    static
    {
        HashSet set = new HashSet();
        set.add( BUSY ); 
        set.add( OTHER ); 
        set.add( SUCCESS ); 
        set.add( REFERRAL ); 
        set.add( LOOPDETECT ); 
        set.add( UNAVAILABLE ); 
        set.add( NOSUCHOBJECT );
        set.add( PROTOCOLERROR ); 
        set.add( INVALIDDNSYNTAX ); 
        set.add( TIMELIMITEXCEEDED ); 
        set.add( ADMINLIMITEXCEEDED );
        set.add( STRONGAUTHREQUIRED ); 
        set.add( UNWILLINGTOPERFORM ); 
        set.add( NOTALLOWEDONNONLEAF );
        set.add( CONFIDENTIALITYREQUIRED ); 
        set.add( INSUFFICIENTACCESSRIGHTS );
        set.add( UNAVAILABLECRITICALEXTENSION );
        DELETE_CODES = Collections.unmodifiableSet( set );
    } ;

    /**
     * A set of result code enumerations resulting from modifyDn operations.
     * The set contains:
     * <ul>
     *   <li><a href="#BUSY">BUSY</a></li>
     *   <li><a href="#OTHER">OTHER</a></li>
     *   <li><a href="#SUCCESS">SUCCESS</a></li>
     *   <li><a href="#REFERRAL">REFERRAL</a></li>
     *   <li><a href="#LOOPDETECT">LOOPDETECT</a></li>
     *   <li><a href="#UNAVAILABLE">UNAVAILABLE</a></li>
     *   <li><a href="#NOSUCHOBJECT">NOSUCHOBJECT</a></li>
     *   <li><a href="#PROTOCOLERROR">PROTOCOLERROR</a></li>
     *   <li><a href="#INVALIDDNSYNTAX">INVALIDDNSYNTAX</a></li>
     *   <li><a href="#NAMINGVIOLATION">NAMINGVIOLATION</a></li>
     *   <li><a href="#TIMELIMITEXCEEDED">TIMELIMITEXCEEDED</a></li>
     *   <li><a href="#ENTRYALREADYEXISTS">ENTRYALREADYEXISTS</a></li>
     *   <li><a href="#ADMINLIMITEXCEEDED">ADMINLIMITEXCEEDED</a></li>
     *   <li><a href="#STRONGAUTHREQUIRED">STRONGAUTHREQUIRED</a></li>
     *   <li><a href="#UNWILLINGTOPERFORM">UNWILLINGTOPERFORM</a></li>
     *   <li><a href="#NOTALLOWEDONNONLEAF">NOTALLOWEDONNONLEAF</a></li>
     *   <li><a href="#AFFECTSMULTIPLEDSAS">AFFECTSMULTIPLEDSAS</a></li>
     *   <li><a href="#CONSTRAINTVIOLATION">CONSTRAINTVIOLATION</a></li>
     *   <li><a href="#OBJECTCLASSVIOLATION">OBJECTCLASSVIOLATION</a></li>
     *   <li><a href="#CONFIDENTIALITYREQUIRED">CONFIDENTIALITYREQUIRED</a></li>
     *   <li><a href="#INSUFFICIENTACCESSRIGHTS">INSUFFICIENTACCESSRIGHTS</a></li>
     *   <li><a href="#UNAVAILABLECRITICALEXTENSION">UNAVAILABLECRITICALEXTENSION</a></li>
     * </ul>
     */
    public static final Set MODIFYDN_CODES;
    static
    {
        HashSet set = new HashSet();
        set.add( BUSY );
        set.add( OTHER );
        set.add( SUCCESS );
        set.add( REFERRAL );
        set.add( LOOPDETECT );
        set.add( UNAVAILABLE );
        set.add( NOSUCHOBJECT );
        set.add( PROTOCOLERROR );
        set.add( INVALIDDNSYNTAX );
        set.add( NAMINGVIOLATION );
        set.add( TIMELIMITEXCEEDED );
        set.add( ENTRYALREADYEXISTS );
        set.add( ADMINLIMITEXCEEDED );
        set.add( STRONGAUTHREQUIRED );
        set.add( UNWILLINGTOPERFORM );
        set.add( NOTALLOWEDONNONLEAF );
        set.add( AFFECTSMULTIPLEDSAS );
        set.add( CONSTRAINTVIOLATION );
        set.add( OBJECTCLASSVIOLATION );
        set.add( CONFIDENTIALITYREQUIRED );
        set.add( INSUFFICIENTACCESSRIGHTS );
        set.add( UNAVAILABLECRITICALEXTENSION );
        MODIFYDN_CODES = Collections.unmodifiableSet( set );
    } 

    /**
     * A set of result code enumerations that may result from compare
     * operations.  The set contains:
     * <ul>
     *   <li><a href="#OPERATIONSERROR">OPERATIONSERROR</a></li>
     *   <li><a href="#PROTOCOLERROR">PROTOCOLERROR</a></li>
     *   <li><a href="#TIMELIMITEXCEEDED">TIMELIMITEXCEEDED</a></li>
     *   <li><a href="#COMPAREFALSE">COMPAREFALSE</a></li>
     *   <li><a href="#COMPARETRUE">COMPARETRUE</a></li>
     *   <li><a href="#STRONGAUTHREQUIRED">STRONGAUTHREQUIRED</a></li>
     *   <li><a href="#ADMINLIMITEXCEEDED">ADMINLIMITEXCEEDED</a></li>
     *   <li><a href="#UNAVAILABLECRITICALEXTENSION">UNAVAILABLECRITICALEXTENSION</a></li>
     *   <li><a href="#CONFIDENTIALITYREQUIRED">CONFIDENTIALITYREQUIRED</a></li>
     *   <li><a href="#NOSUCHATTRIBUTE">NOSUCHATTRIBUTE</a></li>
     *   <li><a href="#INVALIDATTRIBUTESYNTAX">INVALIDATTRIBUTESYNTAX</a></li>
     *   <li><a href="#NOSUCHOBJECT">NOSUCHOBJECT</a></li>
     *   <li><a href="#INVALIDDNSYNTAX">INVALIDDNSYNTAX</a></li>
     *   <li><a href="#INSUFFICIENTACCESSRIGHTS">INSUFFICIENTACCESSRIGHTS</a></li>
     *   <li><a href="#BUSY">BUSY</a></li>
     *   <li><a href="#UNAVAILABLE">UNAVAILABLE</a></li>
     *   <li><a href="#UNWILLINGTOPERFORM">UNWILLINGTOPERFORM</a></li>
     *   <li><a href="#LOOPDETECT">LOOPDETECT</a></li>
     *   <li><a href="#REFERRAL">REFERRAL</a></li>
     *   <li><a href="#OTHER">OTHER</a></li>
     * </ul>
     */
    public static final Set COMPARE_CODES;
    static
    {
        HashSet set = new HashSet();
        set.add( OPERATIONSERROR );
        set.add( PROTOCOLERROR );
        set.add( TIMELIMITEXCEEDED );
        set.add( COMPAREFALSE );
        set.add( COMPARETRUE );
        set.add( STRONGAUTHREQUIRED );
        set.add( ADMINLIMITEXCEEDED );
        set.add( UNAVAILABLECRITICALEXTENSION );
        set.add( CONFIDENTIALITYREQUIRED );
        set.add( NOSUCHATTRIBUTE );
        set.add( INVALIDATTRIBUTESYNTAX );
        set.add( NOSUCHOBJECT );
        set.add( INVALIDDNSYNTAX );
        set.add( INSUFFICIENTACCESSRIGHTS );
        set.add( BUSY );
        set.add( UNAVAILABLE );
        set.add( UNWILLINGTOPERFORM );
        set.add( LOOPDETECT );
        set.add( REFERRAL );
        set.add( OTHER );
        COMPARE_CODES = Collections.unmodifiableSet( set );
    }

    /**
     * A set of result code enumerations that could result from extended
     * operations.  The set contains:
     * <ul>
     *   <li></li>
     *   <li><a href="#SUCCESS">SUCCESS</a></li>
     *   <li><a href="#OPERATIONSERROR">OPERATIONSERROR</a></li>
     *   <li><a href="#PROTOCOLERROR">PROTOCOLERROR</a></li>
     *   <li><a href="#TIMELIMITEXCEEDED">TIMELIMITEXCEEDED</a></li>
     *   <li><a href="#SIZELIMITEXCEEDED">SIZELIMITEXCEEDED</a></li>
     *   <li><a href="#COMPAREFALSE">COMPAREFALSE</a></li>
     *   <li><a href="#COMPARETRUE">COMPARETRUE</a></li>
     *   <li><a href="#AUTHMETHODNOTSUPPORTED">AUTHMETHODNOTSUPPORTED</a></li>
     *   <li><a href="#STRONGAUTHREQUIRED">STRONGAUTHREQUIRED</a></li>
     *   <li><a href="#REFERRAL">REFERRAL</a></li>
     *   <li><a href="#ADMINLIMITEXCEEDED">ADMINLIMITEXCEEDED</a></li>
     *   <li><a href="#UNAVAILABLECRITICALEXTENSION">UNAVAILABLECRITICALEXTENSION</a></li>
     *   <li><a href="#CONFIDENTIALITYREQUIRED">CONFIDENTIALITYREQUIRED</a></li>
     *   <li><a href="#SASLBINDINPROGRESS">SASLBINDINPROGRESS</a></li>
     *   <li><a href="#NOSUCHATTRIBUTE">NOSUCHATTRIBUTE</a></li>
     *   <li><a href="#UNDEFINEDATTRIBUTETYPE">UNDEFINEDATTRIBUTETYPE</a></li>
     *   <li><a href="#INAPPROPRIATEMATCHING">INAPPROPRIATEMATCHING</a></li>
     *   <li><a href="#CONSTRAINTVIOLATION">CONSTRAINTVIOLATION</a></li>
     *   <li><a href="#ATTRIBUTEORVALUEEXISTS">ATTRIBUTEORVALUEEXISTS</a></li>
     *   <li><a href="#INVALIDATTRIBUTESYNTAX">INVALIDATTRIBUTESYNTAX</a></li>
     *   <li><a href="#NOSUCHOBJECT">NOSUCHOBJECT</a></li>
     *   <li><a href="#ALIASPROBLEM">ALIASPROBLEM</a></li>
     *   <li><a href="#INVALIDDNSYNTAX">INVALIDDNSYNTAX</a></li>
     *   <li><a href="#ALIASDEREFERENCINGPROBLEM">ALIASDEREFERENCINGPROBLEM</a></li>
     *   <li><a href="#INAPPROPRIATEAUTHENTICATION">INAPPROPRIATEAUTHENTICATION</a></li>
     *   <li><a href="#INVALIDCREDENTIALS">INVALIDCREDENTIALS</a></li>
     *   <li><a href="#INSUFFICIENTACCESSRIGHTS">INSUFFICIENTACCESSRIGHTS</a></li>
     *   <li><a href="#BUSY">BUSY</a></li>
     *   <li><a href="#UNAVAILABLE">UNAVAILABLE</a></li>
     *   <li><a href="#UNWILLINGTOPERFORM">UNWILLINGTOPERFORM</a></li>
     *   <li><a href="#LOOPDETECT">LOOPDETECT</a></li>
     *   <li><a href="#NAMINGVIOLATION">NAMINGVIOLATION</a></li>
     *   <li><a href="#OBJECTCLASSVIOLATION">OBJECTCLASSVIOLATION</a></li>
     *   <li><a href="#NOTALLOWEDONNONLEAF">NOTALLOWEDONNONLEAF</a></li>
     *   <li><a href="#NOTALLOWEDONRDN">NOTALLOWEDONRDN</a></li>
     *   <li><a href="#ENTRYALREADYEXISTS">ENTRYALREADYEXISTS</a></li>
     *   <li><a href="#OBJECTCLASSMODSPROHIBITED">OBJECTCLASSMODSPROHIBITED</a></li>
     *   <li><a href="#AFFECTSMULTIPLEDSAS">AFFECTSMULTIPLEDSAS</a></li>
     *   <li><a href="#OTHER">OTHER</a></li>
     * </ul>
     */
    public static final Set EXTENDED_CODES;
    static 
    {
        HashSet set = new HashSet();
        set.add( SUCCESS );
        set.add( OPERATIONSERROR );
        set.add( PROTOCOLERROR );
        set.add( TIMELIMITEXCEEDED );
        set.add( SIZELIMITEXCEEDED );
        set.add( COMPAREFALSE );
        set.add( COMPARETRUE );
        set.add( AUTHMETHODNOTSUPPORTED );
        set.add( STRONGAUTHREQUIRED );
        set.add( REFERRAL );
        set.add( ADMINLIMITEXCEEDED );
        set.add( UNAVAILABLECRITICALEXTENSION );
        set.add( CONFIDENTIALITYREQUIRED );
        set.add( SASLBINDINPROGRESS );
        set.add( NOSUCHATTRIBUTE );
        set.add( UNDEFINEDATTRIBUTETYPE );
        set.add( INAPPROPRIATEMATCHING );
        set.add( CONSTRAINTVIOLATION );
        set.add( ATTRIBUTEORVALUEEXISTS );
        set.add( INVALIDATTRIBUTESYNTAX );
        set.add( NOSUCHOBJECT );
        set.add( ALIASPROBLEM );
        set.add( INVALIDDNSYNTAX );
        set.add( ALIASDEREFERENCINGPROBLEM );
        set.add( INAPPROPRIATEAUTHENTICATION );
        set.add( INVALIDCREDENTIALS );
        set.add( INSUFFICIENTACCESSRIGHTS );
        set.add( BUSY );
        set.add( UNAVAILABLE );
        set.add( UNWILLINGTOPERFORM );
        set.add( LOOPDETECT );
        set.add( NAMINGVIOLATION );
        set.add( OBJECTCLASSVIOLATION );
        set.add( NOTALLOWEDONNONLEAF );
        set.add( NOTALLOWEDONRDN );
        set.add( ENTRYALREADYEXISTS );
        set.add( OBJECTCLASSMODSPROHIBITED );
        set.add( AFFECTSMULTIPLEDSAS );
        set.add( OTHER );
        EXTENDED_CODES = Collections.unmodifiableSet( set );
    }

    // ------------------------------------------------------------------------
    // All Result Codes
    // ------------------------------------------------------------------------

    /**
     * Set of all result code enumerations.  The set contains:
     * <ul>
     *   <li><a href="#SUCCESS">SUCCESS</a></li>
     *   <li><a href="#OPERATIONSERROR">OPERATIONSERROR</a></li>
     *   <li><a href="#PROTOCOLERROR">PROTOCOLERROR</a></li>
     *   <li><a href="#TIMELIMITEXCEEDED">TIMELIMITEXCEEDED</a></li>
     *   <li><a href="#SIZELIMITEXCEEDED">SIZELIMITEXCEEDED</a></li>
     *   <li><a href="#COMPAREFALSE">COMPAREFALSE</a></li>
     *   <li><a href="#COMPARETRUE">COMPARETRUE</a></li>
     *   <li><a href="#AUTHMETHODNOTSUPPORTED">AUTHMETHODNOTSUPPORTED</a></li>
     *   <li><a href="#STRONGAUTHREQUIRED">STRONGAUTHREQUIRED</a></li>
     *   <li><a href="#PARTIALRESULTS">PARTIALRESULTS</a></li>
     *   <li><a href="#REFERRAL">REFERRAL</a></li>
     *   <li><a href="#ADMINLIMITEXCEEDED">ADMINLIMITEXCEEDED</a></li>
     *   <li><a href="#UNAVAILABLECRITICALEXTENSION">UNAVAILABLECRITICALEXTENSION</a></li>
     *   <li><a href="#CONFIDENTIALITYREQUIRED">CONFIDENTIALITYREQUIRED</a></li>
     *   <li><a href="#SASLBINDINPROGRESS">SASLBINDINPROGRESS</a></li>
     *   <li><a href="#NOSUCHATTRIBUTE">NOSUCHATTRIBUTE</a></li>
     *   <li><a href="#UNDEFINEDATTRIBUTETYPE">UNDEFINEDATTRIBUTETYPE</a></li>
     *   <li><a href="#INAPPROPRIATEMATCHING">INAPPROPRIATEMATCHING</a></li>
     *   <li><a href="#CONSTRAINTVIOLATION">CONSTRAINTVIOLATION</a></li>
     *   <li><a href="#ATTRIBUTEORVALUEEXISTS">ATTRIBUTEORVALUEEXISTS</a></li>
     *   <li><a href="#INVALIDATTRIBUTESYNTAX">INVALIDATTRIBUTESYNTAX</a></li>
     *   <li><a href="#NOSUCHOBJECT">NOSUCHOBJECT</a></li>
     *   <li><a href="#ALIASPROBLEM">ALIASPROBLEM</a></li>
     *   <li><a href="#INVALIDDNSYNTAX">INVALIDDNSYNTAX</a></li>
     *   <li><a href="#ALIASDEREFERENCINGPROBLEM">ALIASDEREFERENCINGPROBLEM</a></li>
     *   <li><a href="#INAPPROPRIATEAUTHENTICATION">INAPPROPRIATEAUTHENTICATION</a></li>
     *   <li><a href="#INVALIDCREDENTIALS">INVALIDCREDENTIALS</a></li>
     *   <li><a href="#INSUFFICIENTACCESSRIGHTS">INSUFFICIENTACCESSRIGHTS</a></li>
     *   <li><a href="#BUSY">BUSY</a></li>
     *   <li><a href="#UNAVAILABLE">UNAVAILABLE</a></li>
     *   <li><a href="#UNWILLINGTOPERFORM">UNWILLINGTOPERFORM</a></li>
     *   <li><a href="#LOOPDETECT">LOOPDETECT</a></li>
     *   <li><a href="#NAMINGVIOLATION">NAMINGVIOLATION</a></li>
     *   <li><a href="#OBJECTCLASSVIOLATION">OBJECTCLASSVIOLATION</a></li>
     *   <li><a href="#NOTALLOWEDONNONLEAF">NOTALLOWEDONNONLEAF</a></li>
     *   <li><a href="#NOTALLOWEDONRDN">NOTALLOWEDONRDN</a></li>
     *   <li><a href="#ENTRYALREADYEXISTS">ENTRYALREADYEXISTS</a></li>
     *   <li><a href="#OBJECTCLASSMODSPROHIBITED">OBJECTCLASSMODSPROHIBITED</a></li>
     *   <li><a href="#AFFECTSMULTIPLEDSAS">AFFECTSMULTIPLEDSAS</a></li>
     *   <li><a href="#OTHER">OTHER</a></li>
     * </ul>
     */
    public static final Set ALL_CODES;
    static
    {
        HashSet set = new HashSet();
        set.add( SUCCESS );
        set.add( OPERATIONSERROR );
        set.add( PROTOCOLERROR );
        set.add( TIMELIMITEXCEEDED );
        set.add( SIZELIMITEXCEEDED );
        set.add( COMPAREFALSE );
        set.add( COMPARETRUE );
        set.add( AUTHMETHODNOTSUPPORTED );
        set.add( STRONGAUTHREQUIRED );
        set.add( PARTIALRESULTS );
        set.add( REFERRAL );
        set.add( ADMINLIMITEXCEEDED );
        set.add( UNAVAILABLECRITICALEXTENSION );
        set.add( CONFIDENTIALITYREQUIRED );
        set.add( SASLBINDINPROGRESS );
        set.add( NOSUCHATTRIBUTE );
        set.add( UNDEFINEDATTRIBUTETYPE );
        set.add( INAPPROPRIATEMATCHING );
        set.add( CONSTRAINTVIOLATION );
        set.add( ATTRIBUTEORVALUEEXISTS );
        set.add( INVALIDATTRIBUTESYNTAX );
        set.add( NOSUCHOBJECT );
        set.add( ALIASPROBLEM );
        set.add( INVALIDDNSYNTAX );
        set.add( ALIASDEREFERENCINGPROBLEM );
        set.add( INAPPROPRIATEAUTHENTICATION );
        set.add( INVALIDCREDENTIALS );
        set.add( INSUFFICIENTACCESSRIGHTS );
        set.add( BUSY );
        set.add( UNAVAILABLE );
        set.add( UNWILLINGTOPERFORM );
        set.add( LOOPDETECT );
        set.add( NAMINGVIOLATION );
        set.add( OBJECTCLASSVIOLATION );
        set.add( NOTALLOWEDONNONLEAF );
        set.add( NOTALLOWEDONRDN );
        set.add( ENTRYALREADYEXISTS );
        set.add( OBJECTCLASSMODSPROHIBITED );
        set.add( AFFECTSMULTIPLEDSAS );
        set.add( OTHER );
        ALL_CODES = Collections.unmodifiableSet( set );
    }

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Private construct so no other instances can be created other than the
     * public static constants in this class.
     *
     * @param a_name a string name for the enumeration value.
     * @param a_value the integer value of the enumeration.
     */
    private ResultCodeEnum( final String a_name, final int a_value )
    {
        super( a_name, a_value ) ;
    }
    
    
    /**
     * Gets the set of general error codes.
     *
     * @return array of result codes enumerations
     *
     * @see #GENERAL_CODES
     */
    public static Set getGeneralCodes()
    {
        // Must clone to prevent array content alterations
        return GENERAL_CODES;
    }


    /**
     * Gets the set of result code enumerations that do not represent
     * operational failures.
     *
     * @return array of result codes enumerations
     *
     * @see #NON_ERRONEOUS_CODES
     */
    public static Set getNonErroneousCodes()
    {
        // Must clone to prevent array content alterations
        return NON_ERRONEOUS_CODES;
    }


    /**
     * Gets an array of result code enumerations that report a problem related
     * to an attribute specified by the client in their request message..
     *
     * @return array of result codes enumerations
     *
     * @see #ATTRIBUTE_CODES
     */
    public static Set getAttributeCodes()
    {
        // Must clone to prevent array content alterations
        return ATTRIBUTE_CODES;
    }


    /**
     * Gets an array of result code enumerations that report a problem related
     * to a distinguished name provided as an argument to a request message.
     *
     * @return array of result codes enumerations
     *
     * @see #NAME_CODES
     */
    public static Set getNameCodes()
    {
        // Must clone to prevent array content alterations
        return NAME_CODES;
    }


    /**
     * Gets an array of result code enumerations that report a problem related
     * to a problem in carrying out an operation for security reasons.
     *
     * @return array of result codes enumerations
     *
     * @see #SECURITY_CODES
     */
    public static Set getSecurityCodes()
    {
        // Must clone to prevent array content alterations
        return SECURITY_CODES;
    }


    /**
     * Gets an array of result code enumerations that report a problem related
     * to the provision of the service.
     *
     * @return array of result codes enumerations
     *
     * @see #SERVICE_CODES
     */
    public static Set getServiceCodes()
    {
        // Must clone to prevent array content alterations
        return SERVICE_CODES;
    }


    /**
     * Gets an array of result code enumerations that reports problems related
     * to attempts to add, delete, or modify information in the DIB.
     *
     * @return array of result codes enumerations
     *
     * @see #UPDATE_CODES
     */
    public static Set getUpdateCodes()
    {
        // Must clone to prevent array content alterations
        return UPDATE_CODES;
    }


    /**
     * Gets an array of result code enumerations common to all operations.
     *
     * @return an array of common operation ResultCodeEnum's
     *
     * @see #COMMON_CODES
     */
    public static Set getCommonCodes()
    {
        return COMMON_CODES;
    }


    /**
     * Gets an array of result code enumerations resulting from bind
     * operations.
     *
     * @return an array of bind operation ResultCodeEnum's
     *
     * @see #BIND_CODES
     */
    public static Set getBindCodes()
    {
        return BIND_CODES;
    }


    /**
     * Gets an array of result code enumerations resulting from search
     * operations.
     *
     * @return an array of search operation ResultCodeEnum's
     *
     * @see #SEARCH_CODES
     */
    public static Set getSearchCodes()
    {
        return SEARCH_CODES;
    }


    /**
     * Gets an array of result code enumerations resulting from modify
     * operations.
     *
     * @return an array of modify operation ResultCodeEnum's
     *
     * @see #MODIFY_CODES
     */
    public static Set getModifyCodes()
    {
        return MODIFY_CODES;
    }


    /**
     * Gets an array of result code enumerations resulting from add operations.
     *
     * @return an array of add operation ResultCodeEnum's
     *
     * @see #ADD_CODES
     */
    public static Set getAddCodes()
    {
        return ADD_CODES;
    }


    /**
     * Gets an array of result code enumerations resulting from delete
     * operations.
     *
     * @return an array of delete operation ResultCodeEnum's
     *
     * @see #DELETE_CODES
     */
    public static Set getDeleteCodes()
    {
        return DELETE_CODES;
    }


    /**
     * Gets an array of result code enumerations resulting from modifyDn
     * operations.
     *
     * @return an array of modifyDn operation ResultCodeEnum's
     *
     * @see #MODIFYDN_CODES
     */
    public static Set getModifyDnCodes()
    {
        return MODIFYDN_CODES;
    }


    /**
     * Gets an array of result code enumerations resulting from compare
     * operations.
     *
     * @return an array of compare operation ResultCodeEnum's
     *
     * @see #COMPARE_CODES
     */
    public static Set getCompareCodes()
    {
        return COMPARE_CODES;
    }


    /**
     * Gets an array of result code enumerations resulting from extended
     * operations.
     *
     * @return an array of extended operation ResultCodeEnum's
     *
     * @see #EXTENDED_CODES
     */
    public static Set getExtendedCodes()
    {
        return EXTENDED_CODES;
    }


    /**
     * Gets all of the result code enumerations defined.
     *
     * @return an array of all defined result codes
     *
     * @see #ALL_CODES
     */
    public static Set getAllCodes()
    {
        // Must clone to prevent array content tampering.
        return ALL_CODES;
    }


    // ------------------------------------------------------------------------
    // Getting Result Code Enumeration Object Using Integer Values
    // ------------------------------------------------------------------------

    /**
     * Gets the result code enumeration object associated with a result code
     * value.
     *
     * @param value the result code constant.
     * @return the result code with a_value, or null if no enumeration exists
     *         for an (undefined) value.
     */
    public static ResultCodeEnum getResultCodeEnum( int value )
    {
        switch ( value )
        {
        case ( SUCCESS_VAL ):
            return SUCCESS ;

        case ( OPERATIONSERROR_VAL ):
            return OPERATIONSERROR ;

        case ( PROTOCOLERROR_VAL ):
            return PROTOCOLERROR ;

        case ( TIMELIMITEXCEEDED_VAL ):
            return TIMELIMITEXCEEDED ;

        case ( SIZELIMITEXCEEDED_VAL ):
            return SIZELIMITEXCEEDED ;

        case ( COMPAREFALSE_VAL ):
            return COMPAREFALSE ;

        case ( COMPARETRUE_VAL ):
            return COMPARETRUE ;

        case ( AUTHMETHODNOTSUPPORTED_VAL ):
            return AUTHMETHODNOTSUPPORTED ;

        case ( STRONGAUTHREQUIRED_VAL ):
            return STRONGAUTHREQUIRED ;

        case ( PARTIALRESULTS_VAL ):
             return PARTIALRESULTS ;

        case ( REFERRAL_VAL ):
            return REFERRAL ;

        case ( ADMINLIMITEXCEEDED_VAL ):
            return ADMINLIMITEXCEEDED ;

        case ( UNAVAILABLECRITICALEXTENSION_VAL ):
            return UNAVAILABLECRITICALEXTENSION ;

        case ( CONFIDENTIALITYREQUIRED_VAL ):
            return CONFIDENTIALITYREQUIRED ;

        case ( SASLBINDINPROGRESS_VAL ):
            return SASLBINDINPROGRESS ;

        // -- 15 unused --

        case ( NOSUCHATTRIBUTE_VAL ):
            return NOSUCHATTRIBUTE ;

        case ( UNDEFINEDATTRIBUTETYPE_VAL ):
            return UNDEFINEDATTRIBUTETYPE ;

        case ( INAPPROPRIATEMATCHING_VAL ):
            return INAPPROPRIATEMATCHING ;

        case ( CONSTRAINTVIOLATION_VAL ):
            return CONSTRAINTVIOLATION ;

        case ( ATTRIBUTEORVALUEEXISTS_VAL ):
            return ATTRIBUTEORVALUEEXISTS ;

        case ( INVALIDATTRIBUTESYNTAX_VAL ):
            return INVALIDATTRIBUTESYNTAX ;

        // -- 22-31 unused --

        case ( NOSUCHOBJECT_VAL ):
            return NOSUCHOBJECT ;

        case ( ALIASPROBLEM_VAL ):
            return ALIASPROBLEM ;

        case ( INVALIDDNSYNTAX_VAL ):
            return INVALIDDNSYNTAX ;

        // -- 35 reserved for undefined isLeaf --

        case ( ALIASDEREFERENCINGPROBLEM_VAL ):
            return ALIASDEREFERENCINGPROBLEM ;

        // -- 37-47 unused --

        case ( INAPPROPRIATEAUTHENTICATION_VAL ):
            return INAPPROPRIATEAUTHENTICATION ;

        case ( INVALIDCREDENTIALS_VAL ):
            return INVALIDCREDENTIALS ;

        case ( INSUFFICIENTACCESSRIGHTS_VAL ):
            return INSUFFICIENTACCESSRIGHTS ;

        case ( BUSY_VAL ):
            return BUSY ;

        case ( UNAVAILABLE_VAL ):
            return UNAVAILABLE ;

        case ( UNWILLINGTOPERFORM_VAL ):
            return UNWILLINGTOPERFORM ;

        case ( LOOPDETECT_VAL ):
            return LOOPDETECT ;

        // -- 55-63 unused --

        case ( NAMINGVIOLATION_VAL ):
            return NAMINGVIOLATION ;

        case ( OBJECTCLASSVIOLATION_VAL ):
            return OBJECTCLASSVIOLATION ;

        case ( NOTALLOWEDONNONLEAF_VAL ):
            return NOTALLOWEDONNONLEAF ;

        case ( NOTALLOWEDONRDN_VAL ):
            return NOTALLOWEDONRDN ;

        case ( ENTRYALREADYEXISTS_VAL ):
            return ENTRYALREADYEXISTS ;

        case ( OBJECTCLASSMODSPROHIBITED_VAL ):
            return OBJECTCLASSMODSPROHIBITED ;

        // -- 70 reserved for CLDAP --

        case ( AFFECTSMULTIPLEDSAS_VAL ):
            return AFFECTSMULTIPLEDSAS ;

        // -- 72-79 unused --

        case ( OTHER_VAL ):
            return OTHER ;

        // -- 81-90 reserved for APIs --

        default:
            return null ;
        }
    }


    // ------------------------------------------------------------------------
    // JNDI Exception to ResultCodeEnum Mappings
    // ------------------------------------------------------------------------

    /**
     * A set of ResultCodes containing those that may correspond to a
     * {@link NamingException}.
     *
     * <ul>
     *   <li><a href="#OPERATIONSERROR">operationsError(1)</a></li>
     *   <li><a href="#ALIASPROBLEM">aliasProblem(33)</a></li>
     *   <li><a href="#ALIASDEREFERENCINGPROBLEM">aliasDereferencingProblem(36)</a></li>
     *   <li><a href="#LOOPDETECT">loopDetect(54)</a></li>
     *   <li><a href="#AFFECTSMULTIPLEDSAS">affectsMultipleDSAs(71)</a></li>
     *   <li><a href="#OTHER">other(80)</a></li>
     * </ul>
     */
    public static final Set NAMINGEXCEPTION_CODES;
    static
    {
        HashSet set = new HashSet();
        set.add( ResultCodeEnum.OPERATIONSERROR );
        set.add( ResultCodeEnum.ALIASPROBLEM );
        set.add( ResultCodeEnum.ALIASDEREFERENCINGPROBLEM );
        set.add( ResultCodeEnum.LOOPDETECT );
        set.add( ResultCodeEnum.AFFECTSMULTIPLEDSAS );
        set.add( ResultCodeEnum.OTHER );
        NAMINGEXCEPTION_CODES = Collections.unmodifiableSet( set );
    }

    /**
     * A set of ResultCodes containing those that may correspond to a
     * {@link Exception}.
     *
     * <ul>
     *   <li><a href="#AUTHMETHODNOTSUPPORTED">authMethodNotSupported(7)</a></li>
     *   <li><a href="#STRONGAUTHREQUIRED">strongAuthRequired(8)</a></li>
     *   <li><a href="#CONFIDENTIALITYREQUIRED">confidentialityRequired(13)</a></li>
     *   <li><a href="#INAPPROPRIATEAUTHENTICATION">inappropriateAuthentication(48)</a></li>
     * </ul>
     */
    public static final Set AUTHENTICATIONNOTSUPPOERTEDEXCEPTION_CODES;
    static
    {
        HashSet set = new HashSet();
        set.add( ResultCodeEnum.AUTHMETHODNOTSUPPORTED );
        set.add( ResultCodeEnum.STRONGAUTHREQUIRED );
        set.add( ResultCodeEnum.CONFIDENTIALITYREQUIRED );
        set.add( ResultCodeEnum.INAPPROPRIATEAUTHENTICATION );
        AUTHENTICATIONNOTSUPPOERTEDEXCEPTION_CODES = Collections.unmodifiableSet( set );
    }

    /**
     * A set of ResultCodes containing those that may correspond to a
     * {@link Exception}.
     *
     * <ul>
     *   <li><a href="#BUSY">busy(51)</a></li>
     *   <li><a href="#UNAVAILABLE">unavailable(52)</a></li>
     * </ul>
     */
    public static final Set SERVICEUNAVAILABLE_CODES;
    static
    {
        HashSet set = new HashSet();
        set.add( ResultCodeEnum.BUSY );
        set.add( ResultCodeEnum.UNAVAILABLE );
        SERVICEUNAVAILABLE_CODES = Collections.unmodifiableSet( set );
    }

    /**
     * A set of ResultCodes containing those that may correspond to a
     * {@link Exception}.
     *
     * <ul>
     *   <li><a href="#CONSTRAINTVIOLATION">constraintViolation(19)</a></li>
     *   <li><a href="#INVALIDATTRIBUTESYNTAX">invalidAttributeSyntax(21)</a></li>
     * </ul>
     */
    public static final Set INVALIDATTRIBUTEVALUEEXCEPTION_CODES;
    static
    {
        HashSet set = new HashSet();
        set.add( ResultCodeEnum.CONSTRAINTVIOLATION );
        set.add( ResultCodeEnum.INVALIDATTRIBUTESYNTAX );
        INVALIDATTRIBUTEVALUEEXCEPTION_CODES = Collections.unmodifiableSet( set );
    }

    /**
     * A set of ResultCodes containing those that may correspond to a
     * {@link Exception}.
     *
     * <ul>
     *   <li><a href="#PARTIALRESULTS">partialResults(9)</a></li>
     *   <li><a href="#REFERRAL">referral(10)</a></li>
     * </ul>
     */
    public static final Set PARTIALRESULTSEXCEPTION_CODES;
    static
    {
        HashSet set = new HashSet();
        set.add( ResultCodeEnum.PARTIALRESULTS );
        set.add( ResultCodeEnum.REFERRAL );
        PARTIALRESULTSEXCEPTION_CODES = Collections.unmodifiableSet( set );
    }

    /**
     * A set of ResultCodes containing those that may correspond to a
     * {@link Exception}.
     *
     * <ul>
     *   <li><a href="#REFERRAL">referal(9)</a></li>
     *   <li><a href="#ADMINLIMITEXCEEDED">adminLimitExceeded(11)</a></li>
     * </ul>
     */
    public static final Set LIMITEXCEEDEDEXCEPTION_CODES;
    static
    {
        HashSet set = new HashSet();
        set.add( ResultCodeEnum.REFERRAL );
        set.add( ResultCodeEnum.ADMINLIMITEXCEEDED );
        LIMITEXCEEDEDEXCEPTION_CODES = Collections.unmodifiableSet( set );
    }

    /**
     * A set of ResultCodes containing those that may correspond to a
     * {@link Exception}.
     *
     * <ul>
     *   <li><a href="#UNAVAILABLECRITICALEXTENTION">unavailableCriticalExtention(12)</a></li>
     *   <li><a href="#UNWILLINGTOPERFORM">unwillingToPerform(53)</a></li>
     * </ul>
     */
    public static final Set OPERATIONNOTSUPPOERTEXCEPTION_CODES;
    static
    {
        HashSet set = new HashSet();
        set.add( ResultCodeEnum.UNAVAILABLECRITICALEXTENSION );
        set.add( ResultCodeEnum.UNWILLINGTOPERFORM );
        OPERATIONNOTSUPPOERTEXCEPTION_CODES = Collections.unmodifiableSet( set );
    }

    /**
     * A set of ResultCodes containing those that may correspond to a
     * {@link Exception}.
     *
     * <ul>
     *   <li><a href="#INVALIDDNSYNTAX">invalidDNSyntax(34)</a></li>
     *   <li><a href="#NAMINGVIOLATION">namingViolation(64)</a></li>
     * </ul>
     */
    public static final Set INVALIDNAMEEXCEPTION_CODES;
    static
    {
        HashSet set = new HashSet();
        set.add( ResultCodeEnum.INVALIDDNSYNTAX );
        set.add( ResultCodeEnum.NAMINGVIOLATION );
        INVALIDNAMEEXCEPTION_CODES = Collections.unmodifiableSet( set );
    }

    /**
     * A set of ResultCodes containing those that may correspond to a
     * {@link javax.naming.directory.SchemaViolationException}.
     *
     * <ul>
     *   <li><a href="#OBJECTCLASSVIOLATION">objectClassViolation(65)</a></li>
     *   <li><a href="#NOTALLOWEDONRDN">notAllowedOnRDN(67)</a></li>
     *   <li><a href="#OBJECTCLASSMODSPROHIBITED">objectClassModsProhibited(69)</a></li>
     * </ul>
     */
    public static final Set SCHEMAVIOLATIONEXCEPTION_CODES;
    static
    {
        HashSet set = new HashSet();
        set.add( ResultCodeEnum.OBJECTCLASSVIOLATION );
        set.add( ResultCodeEnum.NOTALLOWEDONRDN );
        set.add( ResultCodeEnum.OBJECTCLASSMODSPROHIBITED );
        SCHEMAVIOLATIONEXCEPTION_CODES = Collections.unmodifiableSet( set );
    }


    /**
     * Takes a guess at the result code to use if it cannot figure it out from
     * known Throwable to result code mappings.  Some however are ambiguous
     * mapping the same Throwable to multiple codes.  If no code can be resolved
     * then {@link ResultCodeEnum#OTHER} is returned.
     *
     * @param t the throwable to estimate a result code for
     * @param type the type of operation being performed
     * @return the result code or a good estimate of one
     */
    public static ResultCodeEnum getBestEstimate( Throwable t, MessageTypeEnum type )
    {
        Set set = getResultCodes( t );

        if ( set.isEmpty() )
        {
            return ResultCodeEnum.OTHER;
        }

        if ( set.size() == 1 )
        {
            return ( ResultCodeEnum ) set.iterator().next();
        }

        if ( type == null )
        {
            HashSet tmp = new HashSet();
            tmp.addAll( set );
            tmp.removeAll( NON_ERRONEOUS_CODES );

            if ( tmp.isEmpty() )
            {
                return ResultCodeEnum.OTHER;
            }

            return ( ResultCodeEnum ) tmp.iterator().next();
        }

        Set candidates = Collections.EMPTY_SET;
        switch( type.getValue() )
        {
            case( MessageTypeEnum.ABANDONREQUEST_VAL ):
                return ( ResultCodeEnum ) set.iterator().next();
            case( MessageTypeEnum.ADDREQUEST_VAL ):
                candidates = intersection( set, ADD_CODES );
                break;
            case( MessageTypeEnum.ADDRESPONSE_VAL ):
                candidates = intersection( set, ADD_CODES );
                break;
            case( MessageTypeEnum.BINDREQUEST_VAL ):
                candidates = intersection( set, BIND_CODES );
                break;
            case( MessageTypeEnum.BINDRESPONSE_VAL ):
                candidates = intersection( set, BIND_CODES );
                break;
            case( MessageTypeEnum.COMPAREREQUEST_VAL ):
                candidates = intersection( set, COMPARE_CODES );
                break;
            case( MessageTypeEnum.COMPARERESPONSE_VAL ):
                candidates = intersection( set, COMPARE_CODES );
                break;
            case( MessageTypeEnum.DELREQUEST_VAL ):
                candidates = intersection( set, DELETE_CODES );
                break;
            case( MessageTypeEnum.DELRESPONSE_VAL ):
                candidates = intersection( set, DELETE_CODES );
                break;
            case( MessageTypeEnum.EXTENDEDREQ_VAL ):
                candidates = intersection( set, EXTENDED_CODES );
                break;
            case( MessageTypeEnum.EXTENDEDRESP_VAL ):
                candidates = intersection( set, EXTENDED_CODES );
                break;
            case( MessageTypeEnum.MODDNREQUEST_VAL ):
                candidates = intersection( set, MODIFYDN_CODES );
                break;
            case( MessageTypeEnum.MODDNRESPONSE_VAL ):
                candidates = intersection( set, MODIFYDN_CODES );
                break;
            case( MessageTypeEnum.MODIFYREQUEST_VAL ):
                candidates = intersection( set, MODIFY_CODES );
                break;
            case( MessageTypeEnum.MODIFYRESPONSE_VAL ):
                candidates = intersection( set, MODIFY_CODES );
                break;
            case( MessageTypeEnum.SEARCHREQUEST_VAL ):
                candidates = intersection( set, SEARCH_CODES );
                break;
            case( MessageTypeEnum.SEARCHRESDONE_VAL ):
                candidates = intersection( set, SEARCH_CODES );
                break;
            case( MessageTypeEnum.SEARCHRESENTRY_VAL ):
                candidates = intersection( set, SEARCH_CODES );
                break;
            case( MessageTypeEnum.SEARCHRESREF_VAL ):
                candidates = intersection( set, SEARCH_CODES );
                break;
            case( MessageTypeEnum.UNBINDREQUEST_VAL ):
                return ( ResultCodeEnum ) set.iterator().next();
        }

        // we don't want any codes that do not have anything to do w/ errors
        candidates.removeAll( NON_ERRONEOUS_CODES );

        if ( candidates.isEmpty() )
        {
            return ResultCodeEnum.OTHER;
        }

        return ( ResultCodeEnum ) candidates.iterator().next();
    }


    private static Set intersection( Set s1, Set s2 )
    {
        if ( s1.isEmpty() || s2.isEmpty() )
        {
            return Collections.EMPTY_SET;
        }

        Set intersection = new HashSet();
        if ( s1.size() <= s2.size() )
        {
            Iterator items = s1.iterator();
            while ( items.hasNext() )
            {
                Object item = items.next();
                if ( s2.contains( item ) )
                {
                    intersection.add( item );
                }
            }
        }
        else
        {
            Iterator items = s2.iterator();
            while ( items.hasNext() )
            {
                Object item = items.next();
                if ( s1.contains( item ) )
                {
                    intersection.add( item );
                }
            }
        }

        return intersection;
    }


    /**
     * Gets the set of result codes a Throwable may map to.  If the throwable
     * does not map to any result code at all an empty set is returned.  The
     * following Throwables and their subclasses map to result codes:
     *
     * <pre>
     *
     * Unambiguous Exceptions
     * ======================
     *
     * CommunicationException              ==> operationsError(1)
     * TimeLimitExceededException          ==> timeLimitExceeded(3)
     * SizeLimitExceededException          ==> sizeLimitExceeded(4)
     * AuthenticationException             ==> invalidCredentials(49)
     * NoPermissionException               ==> insufficientAccessRights(50)
     * NoSuchAttributeException            ==> noSuchAttribute(16)
     * InvalidAttributeIdentifierException ==> undefinedAttributeType(17)
     * InvalidSearchFilterException        ==> inappropriateMatching(18)
     * AttributeInUseException             ==> attributeOrValueExists(20)
     * NameNotFoundException               ==> noSuchObject(32)
     * NameAlreadyBoundException           ==> entryAlreadyExists(68)
     * ContextNotEmptyException            ==> notAllowedOnNonLeaf(66)
     *
     *
     * Ambiguous Exceptions
     * ====================
     *
     * NamingException
     * ---------------
     * operationsError(1)
     * aliasProblem(33)
     * aliasDereferencingProblem(36)
     * loopDetect(54)
     * affectsMultipleDSAs(71)
     * other(80)
     *
     * AuthenticationNotSupportedException
     * -----------------------------------
     * authMethodNotSupported (7)
     * strongAuthRequired (8)
     * confidentialityRequired (13)
     * inappropriateAuthentication(48)
     *
     * ServiceUnavailableException
     * ---------------------------
     * busy(51)
     * unavailable(52)
     *
     * InvalidAttributeValueException
     * ------------------------------
     * constraintViolation(19)
     * invalidAttributeSyntax(21)
     *
     * PartialResultException
     * ----------------------
     * partialResults(9)
     * referral(10)
     *
     * LimitExceededException
     * ----------------------
     * referal(9)
     * adminLimitExceeded(11)
     *
     * OperationNotSupportedException
     * ------------------------------
     * unavailableCriticalExtention(12)
     * unwillingToPerform(53)
     *
     * InvalidNameException
     * --------------------
     * invalidDNSyntax(34)
     * namingViolation(64)
     *
     * SchemaViolationException
     * ------------------------
     * objectClassViolation(65)
     * notAllowedOnRDN(67)
     * objectClassModsProhibited(69)
     *
     * </pre>
     *
     * @param t the Throwable to find the result code mappings for
     * @return the set of mapped result codes
     */
    public static Set getResultCodes( Throwable t )
    {
        ResultCodeEnum rc;
        if ( ( rc = getResultCode( t ) ) != null )
        {
            return Collections.singleton( rc );
        }

        if ( t instanceof SchemaViolationException )
        {
            return SCHEMAVIOLATIONEXCEPTION_CODES;
        }

        if ( t instanceof InvalidNameException )
        {
            return INVALIDNAMEEXCEPTION_CODES;
        }

        if ( t instanceof OperationNotSupportedException )
        {
            return OPERATIONNOTSUPPOERTEXCEPTION_CODES;
        }

        if ( t instanceof LimitExceededException )
        {
            return LIMITEXCEEDEDEXCEPTION_CODES;
        }

        if ( t instanceof PartialResultException )
        {
            return PARTIALRESULTSEXCEPTION_CODES;
        }

        if ( t instanceof InvalidAttributeValueException )
        {
            return INVALIDATTRIBUTEVALUEEXCEPTION_CODES;
        }

        if ( t instanceof ServiceUnavailableException )
        {
            return SERVICEUNAVAILABLE_CODES;
        }

        if ( t instanceof AuthenticationNotSupportedException )
        {
            return AUTHENTICATIONNOTSUPPOERTEDEXCEPTION_CODES;
        }

        // keep this last because others are subtypes and thier evaluation
        // may be shorted otherwise by this comparison here
        if ( t instanceof NamingException )
        {
            return NAMINGEXCEPTION_CODES;
        }

        return Collections.EMPTY_SET;
    }


    /**
     * Gets an LDAP result code from a Throwable if it can resolve it
     * unambiguously or returns null if it cannot resolve the exception to
     * a single ResultCode.  If the Throwable is an instance of LdapException
     * this is already done for us, otherwise we use the following mapping:
     * <pre>
     *
     * Unambiguous Exceptions
     * ======================
     *
     * CommunicationException              ==> operationsError(1)
     * TimeLimitExceededException          ==> timeLimitExceeded(3)
     * SizeLimitExceededException          ==> sizeLimitExceeded(4)
     * AuthenticationException             ==> invalidCredentials(49)
     * NoPermissionException               ==> insufficientAccessRights(50)
     * NoSuchAttributeException            ==> noSuchAttribute(16)
     * InvalidAttributeIdentifierException ==> undefinedAttributeType(17)
     * InvalidSearchFilterException        ==> inappropriateMatching(18)
     * AttributeInUseException             ==> attributeOrValueExists(20)
     * NameNotFoundException               ==> noSuchObject(32)
     * NameAlreadyBoundException           ==> entryAlreadyExists(68)
     * ContextNotEmptyException            ==> notAllowedOnNonLeaf(66)
     * </pre>
     *
     * If we cannot find a mapping then null is returned.
     *
     * @param t
     * @return
     */
    public static ResultCodeEnum getResultCode( Throwable t )
    {
        if ( t instanceof LdapException )
        {
            return ( ( LdapException ) t ).getResultCode();
        }

        if ( t instanceof CommunicationException )
        {
            return ResultCodeEnum.PROTOCOLERROR;
        }

        if ( t instanceof TimeLimitExceededException )
        {
            return ResultCodeEnum.TIMELIMITEXCEEDED;
        }

        if ( t instanceof SizeLimitExceededException )
        {
            return ResultCodeEnum.SIZELIMITEXCEEDED;
        }

        if ( t instanceof AuthenticationException )
        {
            return ResultCodeEnum.INVALIDCREDENTIALS;
        }

        if ( t instanceof NoPermissionException )
        {
            return ResultCodeEnum.INSUFFICIENTACCESSRIGHTS;
        }

        if ( t instanceof NoSuchAttributeException )
        {
            return ResultCodeEnum.NOSUCHATTRIBUTE;
        }

        if ( t instanceof InvalidAttributeIdentifierException )
        {
            return ResultCodeEnum.UNDEFINEDATTRIBUTETYPE;
        }

        if ( t instanceof InvalidSearchFilterException )
        {
            return ResultCodeEnum.INAPPROPRIATEMATCHING;
        }

        if ( t instanceof AttributeInUseException )
        {
            return ResultCodeEnum.ATTRIBUTEORVALUEEXISTS;
        }

        if ( t instanceof NameNotFoundException )
        {
            return ResultCodeEnum.NOSUCHOBJECT;
        }

        if ( t instanceof NameAlreadyBoundException )
        {
            return ResultCodeEnum.ENTRYALREADYEXISTS;
        }

        if ( t instanceof ContextNotEmptyException )
        {
            return ResultCodeEnum.NOTALLOWEDONNONLEAF;
        }

        return null;
    }
}
