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

package org.apache.directory.studio.dsmlv2.reponse;

import org.apache.directory.shared.ldap.message.ResultCodeEnum;


/**
 * This Class helps to get resultCodeDesc for a ResultCode of a LdapResult.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class LdapResultEnum
{
    /**
     * Gets the String description of a given result code 
     * 
     * @param resultCode 
     *      a result code
     * @return 
     *      the String description corresponding to the result code
     */
    public static String getResultCodeDescr( ResultCodeEnum resultCode )
    {
        switch ( resultCode )
        {
            case SUCCESS:
                return "success";
            case OPERATIONS_ERROR:
                return "operationsError";
            case PROTOCOL_ERROR:
                return "protocolError";
            case TIME_LIMIT_EXCEEDED:
                return "timeLimitExceeded";
            case SIZE_LIMIT_EXCEEDED:
                return "sizeLimitExceeded";
            case COMPARE_FALSE:
                return "compareFalse";
            case COMPARE_TRUE:
                return "compareTrue";
            case AUTH_METHOD_NOT_SUPPORTED:
                return "authMethodNotSupported";
            case STRONG_AUTH_REQUIRED:
                return "strongAuthRequired";
            case PARTIAL_RESULTS:
                return "partialResults";
            case REFERRAL:
                return "referral";
            case ADMIN_LIMIT_EXCEEDED:
                return "adminLimitExceeded";
            case UNAVAILABLE_CRITICAL_EXTENSION:
                return "unavailableCriticalExtension";
            case CONFIDENTIALITY_REQUIRED:
                return "confidentialityRequired";
            case SASL_BIND_IN_PROGRESS:
                return "saslBindInProgress";
            case NO_SUCH_ATTRIBUTE:
                return "noSuchAttribute";
            case UNDEFINED_ATTRIBUTE_TYPE:
                return "undefinedAttributeType";
            case INAPPROPRIATE_MATCHING:
                return "inappropriateMatching";
            case CONSTRAINT_VIOLATION:
                return "constraintViolation";
            case ATTRIBUTE_OR_VALUE_EXISTS:
                return "attributeOrValueExists";
            case INVALID_ATTRIBUTE_SYNTAX:
                return "invalidAttributeSyntax";
            case NO_SUCH_OBJECT:
                return "NO_SUCH_OBJECT";
            case ALIAS_PROBLEM:
                return "aliasProblem";
            case INVALID_DN_SYNTAX:
                return "invalidDNSyntax";
            case ALIAS_DEREFERENCING_PROBLEM:
                return "aliasDereferencingProblem";
            case INAPPROPRIATE_AUTHENTICATION:
                return "inappropriateAuthentication";
            case INVALID_CREDENTIALS:
                return "invalidCredentials";
            case INSUFFICIENT_ACCESS_RIGHTS:
                return "insufficientAccessRights";
            case BUSY:
                return "busy";
            case UNAVAILABLE:
                return "unavailable";
            case UNWILLING_TO_PERFORM:
                return "unwillingToPerform";
            case LOOP_DETECT:
                return "loopDetect";
            case NAMING_VIOLATION:
                return "namingViolation";
            case OBJECT_CLASS_VIOLATION:
                return "objectClassViolation";
            case NOT_ALLOWED_ON_NON_LEAF:
                return "notAllowedOnNonLeaf";
            case NOT_ALLOWED_ON_RDN:
                return "notAllowedOnRDN";
            case ENTRY_ALREADY_EXISTS:
                return "entryAlreadyExists";
            case OBJECT_CLASS_MODS_PROHIBITED:
                return "objectClassModsProhibited";
            case AFFECTS_MULTIPLE_DSAS:
                return "affectsMultipleDSAs";
            case OTHER:
                return "other";
            case UNKNOWN:
                return "unknown";
                
            default:
                return "unknoxn";
        }
    }
}
