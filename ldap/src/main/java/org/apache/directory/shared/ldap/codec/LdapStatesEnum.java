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
package org.apache.directory.shared.ldap.codec;


import org.apache.directory.shared.asn1.ber.grammar.IGrammar;
import org.apache.directory.shared.asn1.ber.grammar.IStates;
import org.apache.directory.shared.ldap.codec.abandon.AbandonRequestGrammar;
import org.apache.directory.shared.ldap.codec.add.AddRequestGrammar;
import org.apache.directory.shared.ldap.codec.add.AddResponseGrammar;
import org.apache.directory.shared.ldap.codec.bind.BindRequestGrammar;
import org.apache.directory.shared.ldap.codec.bind.BindResponseGrammar;
import org.apache.directory.shared.ldap.codec.compare.CompareRequestGrammar;
import org.apache.directory.shared.ldap.codec.compare.CompareResponseGrammar;
import org.apache.directory.shared.ldap.codec.del.DelRequestGrammar;
import org.apache.directory.shared.ldap.codec.del.DelResponseGrammar;
import org.apache.directory.shared.ldap.codec.extended.ExtendedRequestGrammar;
import org.apache.directory.shared.ldap.codec.extended.ExtendedResponseGrammar;
import org.apache.directory.shared.ldap.codec.modify.ModifyRequestGrammar;
import org.apache.directory.shared.ldap.codec.modify.ModifyResponseGrammar;
import org.apache.directory.shared.ldap.codec.modifyDn.ModifyDNRequestGrammar;
import org.apache.directory.shared.ldap.codec.modifyDn.ModifyDNResponseGrammar;
import org.apache.directory.shared.ldap.codec.search.FilterGrammar;
import org.apache.directory.shared.ldap.codec.search.MatchingRuleAssertionGrammar;
import org.apache.directory.shared.ldap.codec.search.SearchRequestGrammar;
import org.apache.directory.shared.ldap.codec.search.SearchResultDoneGrammar;
import org.apache.directory.shared.ldap.codec.search.SearchResultEntryGrammar;
import org.apache.directory.shared.ldap.codec.search.SearchResultReferenceGrammar;
import org.apache.directory.shared.ldap.codec.search.SubstringFilterGrammar;
import org.apache.directory.shared.ldap.codec.unbind.UnBindRequestGrammar;


/**
 * This class store the Ldap grammar's constants.
 * It is also used for debugging purpose
 * 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapStatesEnum implements IStates
{
    //~ Static fields/initializers -----------------------------------------------------------------

    //=========================================================================
    // LdapMessage
    //=========================================================================
    /** LDAPMessage Tag */
    public static int LDAP_MESSAGE_TAG = 0;

    /** LDAPMessage Value */
    public static int LDAP_MESSAGE_VALUE = 1;

    // Message ID -------------------------------------------------------------
    /** MessageID Tag */
    public static int LDAP_MESSAGE_ID_TAG = 2;

    /** MessageID Value */
    public static int LDAP_MESSAGE_ID_VALUE = 3;

    //=========================================================================
    // ProtocolOp
    //=========================================================================
    /** protocolOp CHOICE Tag */
    public static int PROTOCOL_OP_TAG = 4;

    /** protocolOp CHOICE Value */
    public static int PROTOCOL_OP_VALUE       = 5;

    /** The Ldap Message last state */
    public static int LAST_LDAP_MESSAGE_STATE = 6;

    //=========================================================================
    // BindRequest 
    //=========================================================================
    /** The BindRequest Tag */
    public static int BIND_REQUEST_TAG = 0;

    /** The BindRequest Value */
    public static int BIND_REQUEST_VALUE = 1;

    /** Version Tag */
    public static int BIND_REQUEST_VERSION_TAG = 2;

    /** Version Value */
    public static int BIND_REQUEST_VERSION_VALUE = 3;

    // Name -------------------------------------------------------------------
    /** Name Tag */
    public static int BIND_REQUEST_NAME_TAG = 4;

    /** Name Value */
    public static int BIND_REQUEST_NAME_VALUE = 5;

    // Authentication choice --------------------------------------------------
    /** Authentication choice Tag */
    public static int BIND_REQUEST_AUTHENTICATION_CHOICE_TAG = 6;

    // Authentication simple --------------------------------------------------
    /** Authentication Simple Value */
    public static int BIND_REQUEST_AUTHENTICATION_SIMPLE_VALUE = 7;

    // Authentication sasl ----------------------------------------------------
    /** Authentication Sasl Value */
    public static int BIND_REQUEST_AUTHENTICATION_SASL_VALUE = 8;

    // Authentication sasl mechanism ------------------------------------------
    /** Authentication Sasl mechanism Tag */
    public static int BIND_REQUEST_AUTHENTICATION_MECHANISM_TAG = 9;

    /** Authentication sasl mechanism Value */
    public static int BIND_REQUEST_AUTHENTICATION_MECHANISM_VALUE = 10;

    // Authentication sasl mechanism ------------------------------------------
    /** Authentication Sasl credentials Tag */
    public static int BIND_REQUEST_AUTHENTICATION_CREDENTIALS_TAG = 11;

    /** Authentication sasl credentials Value */
    public static int BIND_REQUEST_AUTHENTICATION_CREDENTIALS_VALUE = 12;

    /** The bind request last state */
    public static int LAST_BIND_REQUEST_STATE = 13;

    //=========================================================================
    // UnBindRequest 
    //=========================================================================
    /** The UnBindRequest Tag */
    public static int UNBIND_REQUEST_TAG = 0;

    /** The UnBindRequest Value */
    public static int UNBIND_REQUEST_VALUE = 1;

    /** The unbind request last state */
    public static int LAST_UNBIND_REQUEST_STATE = 2;

    //=========================================================================
    // AbandonRequest 
    //=========================================================================
    /** The abandon request Tag */
    public static int ABANDON_REQUEST_MESSAGE_ID_TAG = 0;

    /** The abandon request Value */
    public static int ABANDON_REQUEST_MESSAGE_ID_VALUE = 1;

    /** The abandon request last state */
    public static int LAST_ABANDON_REQUEST_STATE = 2;

    //=========================================================================
    // Control 
    //=========================================================================
    /** Control Tag */
    public static int CONTROLS_TAG = 0;

    /** Control Value */
    public static int CONTROLS_VALUE = 1;

    // Control ----------------------------------------------------------------
    /** Control Tag */
    public static int CONTROL_TAG = 2;

    /** Control Value */
    public static int CONTROL_VALUE = 3;

    // Control controltype ----------------------------------------------------
    /** Control type Tag */
    public static int CONTROL_TYPE_TAG = 4;

    /** Control type Value */
    public static int CONTROL_TYPE_VALUE = 5;

    // Control criticality ----------------------------------------------------
    /** Control criticality Tag */
    public static int CONTROL_LOOP_OR_CRITICAL_OR_VALUE_TAG = 6;

    /** Control criticality Value */
    public static int CONTROL_CRITICALITY_VALUE = 7;

    // Control controlvalue ---------------------------------------------------
    /** Control value Tag */
    public static int CONTROL_LOOP_OR_VALUE_TAG = 8;

    /** Control value Value */
    public static int CONTROL_VALUE_VALUE = 9;

    /** Another Control, or the end */
    public static int CONTROL_LOOP_OR_END_TAG = 10;

    /** Control last state */
    public static int LAST_CONTROL_STATE = 11;

    //=========================================================================
    // BindResponse
    //=========================================================================
    /**  Bind Response Tag */
    public static int BIND_RESPONSE_TAG = 0;

    /**  Bind Response Value */
    public static int BIND_RESPONSE_VALUE = 1;

    /**  Bind Response Ldap Result (we will switch the grammar here) */
    public static int BIND_RESPONSE_LDAP_RESULT = 2;

    /** serverSaslCreds Tag */
    public static int BIND_RESPONSE_SERVER_SASL_CREDS_TAG = 3;

    /** serverSaslCreds Value */
    public static int BIND_RESPONSE_SERVER_SASL_CREDS_VALUE = 4;

    /** Last state */
    public static int LAST_BIND_RESPONSE_STATE = 5;

    //=========================================================================
    // AddResponse 
    //=========================================================================
    /**  Add Response Tag */
    public static int ADD_RESPONSE_TAG = 0;

    /**  Add Response Value */
    public static int ADD_RESPONSE_VALUE = 1;

    /**  Add Response Ldap Result (we will switch the grammar here) */
    public static int ADD_RESPONSE_LDAP_RESULT = 2;

    /** Last state */
    public static int LAST_ADD_RESPONSE_STATE = 3;

    //=========================================================================
    // CompareResponse 
    //=========================================================================
    /**  Compare Response Tag */
    public static int COMPARE_RESPONSE_TAG = 0;

    /**  Compare Response Value */
    public static int COMPARE_RESPONSE_VALUE = 1;

    /**  Compare Response Ldap Result (we will switch the grammar here) */
    public static int COMPARE_RESPONSE_LDAP_RESULT = 2;

    /** Last state */
    public static int LAST_COMPARE_RESPONSE_STATE = 3;

    //=========================================================================
    // DelResponse 
    //=========================================================================
    /**  Del Response Tag */
    public static int DEL_RESPONSE_TAG = 0;

    /**  Del Response Value */
    public static int DEL_RESPONSE_VALUE = 1;

    /**  Del Response Ldap Result (we will switch the grammar here) */
    public static int DEL_RESPONSE_LDAP_RESULT = 2;

    /** Last state */
    public static int LAST_DEL_RESPONSE_STATE = 3;

    //=========================================================================
    // ModifyResponse 
    //=========================================================================
    /**  Modify Response Tag */
    public static int MODIFY_RESPONSE_TAG = 0;

    /**  Modify Response Value */
    public static int MODIFY_RESPONSE_VALUE = 1;

    /**  Modify Response Ldap Result (we will switch the grammar here) */
    public static int MODIFY_RESPONSE_LDAP_RESULT = 2;

    /** Last state */
    public static int LAST_MODIFY_RESPONSE_STATE = 3;

    //=========================================================================
    // ModifyDNResponse 
    //=========================================================================
    /**  Modify DN Response Tag */
    public static int MODIFY_DN_RESPONSE_TAG = 0;

    /**  Modify DN Response Value */
    public static int MODIFY_DN_RESPONSE_VALUE = 1;

    /**  Modify DN Response Ldap Result (we will switch the grammar here) */
    public static int MODIFY_DN_RESPONSE_LDAP_RESULT = 2;

    /** Last state */
    public static int LAST_MODIFY_DN_RESPONSE_STATE = 3;

    //=========================================================================
    // SearchResultDone
    //=========================================================================
    /**  SearchResultDone Tag */
    public static int SEARCH_RESULT_DONE_TAG = 0;

    /**  SearchResultDone Value */
    public static int SEARCH_RESULT_DONE_VALUE = 1;

    /**  SearchResultDone Ldap Result (we will switch the grammar here) */
    public static int SEARCH_RESULT_DONE_LDAP_RESULT = 2;

    /** Last state */
    public static int LAST_SEARCH_RESULT_DONE_STATE = 3;

    //=========================================================================
    // LdapResult grammar states 
    //=========================================================================
    /** LdapResult Code Tag */
    public static int LDAP_RESULT_CODE_TAG = 0;

    /** LdapResult Code Value */
    public static int LDAP_RESULT_CODE_VALUE = 1;

    // LdapResult Matched DN --------------------------------------------------
    /** LdapResult Matched DN Tag */
    public static int LDAP_RESULT_MATCHED_DN_TAG = 2;

    /** LdapResult Matched DN Value */
    public static int LDAP_RESULT_MATCHED_DN_VALUE = 3;

    // LdapResult error message -----------------------------------------------
    /** LdapResult error message Tag */
    public static int LDAP_RESULT_ERROR_MESSAGE_TAG = 4;

    /** LdapResult error message Value */
    public static int LDAP_RESULT_ERROR_MESSAGE_VALUE = 5;

    // LdapResult referral sequence -------------------------------------------
    /** LdapResult referral sequence Tag */
    public static int LDAP_RESULT_REFERRAL_SEQUENCE_TAG = 6;

    /** LdapResult referral sequence Value */
    public static int LDAP_RESULT_REFERRAL_SEQUENCE_VALUE = 7;

    // LdapResult referral ---------------------------------------------------
    /** LdapResult referral Tag */
    public static int LDAP_RESULT_REFERRAL_TAG = 8;

    /** LdapResult referral Value */
    public static int LDAP_RESULT_REFERRAL_VALUE = 9;

    /** LdapResult referral Tag loop */
    public static int LDAP_RESULT_REFERRAL_LOOP_TAG = 10;

    /** The last state */
    public static int LAST_LDAP_RESULT_STATE = 11;

    //=========================================================================
    // SearchRequest grammar states 
    //=========================================================================
    // SearchRequest ----------------------------------------------------------
    /** SearchRequest Tag */
    public static int SEARCH_REQUEST_TAG = 0;

    /** SearchRequest Value */
    public static int SEARCH_REQUEST_VALUE = 1;

    // SearchRequest base Object ----------------------------------------------
    /** SearchRequest BaseObject Tag */
    public static int SEARCH_REQUEST_BASE_OBJECT_TAG = 2;

    /** SearchRequest BaseObject Value */
    public static int SEARCH_REQUEST_BASE_OBJECT_VALUE = 3;

    // SearchRequest scope ----------------------------------------------------
    /** SearchRequest scope Tag */
    public static int SEARCH_REQUEST_SCOPE_TAG = 4;

    /** SearchRequest scope Value */
    public static int SEARCH_REQUEST_SCOPE_VALUE = 5;

    // SearchRequest derefAliases ---------------------------------------------
    /** SearchRequest derefAliases Tag */
    public static int SEARCH_REQUEST_DEREF_ALIASES_TAG = 6;

    /** SearchRequest derefAliases Value */
    public static int SEARCH_REQUEST_DEREF_ALIASES_VALUE = 7;

    // SearchRequest sizeLimit ------------------------------------------------
    /** SearchRequest sizeLimit Tag */
    public static int SEARCH_REQUEST_SIZE_LIMIT_TAG = 8;

    /** SearchRequest sizeLimit Value */
    public static int SEARCH_REQUEST_SIZE_LIMIT_VALUE = 9;

    // SearchRequest timeLimit ------------------------------------------------
    /** SearchRequest timeLimit Tag */
    public static int SEARCH_REQUEST_TIME_LIMIT_TAG = 10;

    /** SearchRequest timeLimit Value */
    public static int SEARCH_REQUEST_TIME_LIMIT_VALUE = 11;

    // SearchRequest typesOnly ------------------------------------------------
    /** SearchRequest typesOnly Tag */
    public static int SEARCH_REQUEST_TYPES_ONLY_TAG = 12;

    /** SearchRequest typesOnly Value */
    public static int SEARCH_REQUEST_TYPES_ONLY_VALUE = 13;

    // SearchRequest Filter ---------------------------------------------------
    /** SearchRequest Filter (we will switch the grammar here) */
    public static int SEARCH_REQUEST_FILTER = 14;

    // SearchRequest attribute description list -------------------------------
    /** SearchRequest attributes description list Tag */
    public static int SEARCH_REQUEST_ATTRIBUTE_DESCRIPTION_LIST_TAG = 15;

    /** serverSaslCreds attributes description list Value */
    public static int SEARCH_REQUEST_ATTRIBUTE_DESCRIPTION_LIST_VALUE = 16;

    // SearchRequest attribute description ------------------------------------
    /** SearchRequest attributes description Tag */
    public static int SEARCH_REQUEST_ATTRIBUTE_DESCRIPTION_TAG = 17;

    /** SearchRequest attributes description Value */
    public static int SEARCH_REQUEST_ATTRIBUTE_DESCRIPTION_VALUE = 18;

    /** SearchRequest attributes description Tag loop */
    public static int SEARCH_REQUEST_ATTRIBUTE_DESCRIPTION_LOOP_TAG = 19;

    /** The last state */
    public static int LAST_SEARCH_REQUEST_STATE = 20;

    //=========================================================================
    // Filter grammar states 
    //=========================================================================
    /** Filter Tag */
    public static int FILTER_TAG = 0;

    // Filter and -------------------------------------------------------------
    /** Filter And Tag */
    public static int FILTER_AND_TAG = 1;

    /** Filter And Value */
    public static int FILTER_AND_VALUE = 2;

    // Filter or --------------------------------------------------------------
    /** Filter Or Tag */
    public static int FILTER_OR_TAG = 3;

    /** Filter Or Value */
    public static int FILTER_OR_VALUE = 4;

    // Filter not -------------------------------------------------------------
    /** Filter Not Tag */
    public static int FILTER_NOT_TAG = 5;

    /** Filter Not Value */
    public static int FILTER_NOT_VALUE = 6;

    // Filter equalityMatch ---------------------------------------------------
    /** Filter equalityMatch Tag */
    public static int FILTER_EQUALITY_MATCH_TAG = 7;

    /** Filter equalityMatch Value */
    public static int FILTER_EQUALITY_MATCH_VALUE = 8;

    // Filter substrings ------------------------------------------------------
    /** Filter substrings Tag */
    public static int FILTER_SUBSTRINGS_TAG = 9;

    /** Filter substrings Value */
    public static int FILTER_SUBSTRINGS_VALUE = 10;

    // Filter greaterOrEqual --------------------------------------------------
    /** Filter greaterOrEqual Tag */
    public static int FILTER_GREATER_OR_EQUAL_TAG = 11;

    /** Filter greaterOrEqual Value */
    public static int FILTER_GREATER_OR_EQUAL_VALUE = 12;

    // Filter lessOrEqual -----------------------------------------------------
    /** Filter lessOrEqual Tag */
    public static int FILTER_LESS_OR_EQUAL_TAG = 13;

    /** Filter lessOrEqual Value */
    public static int FILTER_LESS_OR_EQUAL_VALUE = 14;

    // Filter present ---------------------------------------------------------
    /** Filter present Tag */
    public static int FILTER_PRESENT_TAG = 15;

    /** Filter present Value */
    public static int FILTER_PRESENT_VALUE = 16;

    // Filter approxMatch -----------------------------------------------------
    /** Filter approxMatch Tag */
    public static int FILTER_APPROX_MATCH_TAG = 17;

    /** Filter approxMatch Value */
    public static int FILTER_APPROX_MATCH_VALUE = 18;

    // Filter extensibleMatch -------------------------------------------------
    /** Filter extensibleMatch Tag */
    public static int FILTER_EXTENSIBLE_MATCH_TAG = 19;

    /** Filter extensibleMatch Value */
    public static int FILTER_EXTENSIBLE_MATCH_VALUE = 20;

    // Filter Loop ------------------------------------------------------------
    /** Filter Loop Tag */
    public static int FILTER_LOOP_TAG = 21;

    // Filter AttributeDesc ---------------------------------------------------
    /** Filter AttributeDesc Tag */
    public static int FILTER_ATTRIBUTE_DESC_TAG = 22;

    /** Filter Value */
    public static int FILTER_ATTRIBUTE_DESC_VALUE = 23;

    // Filter Assertion Value  ------------------------------------------------
    /** Filter Assertion Value Tag */
    public static int FILTER_ASSERTION_VALUE_TAG = 24;

    /** Filter Assertion Value Value */
    public static int FILTER_ASSERTION_VALUE_VALUE = 25;

    /** The last state */
    public static int LAST_FILTER_STATE = 26;

    //=========================================================================
    // SubSstrings Filter grammar states 
    //=========================================================================
    /** Substrings Filter Tag */
    public static int SUBSTRINGS_FILTER_TAG = 0;

    /** Substrings Filter Value */
    public static int SUBSTRINGS_FILTER_VALUE = 1;

    // Substrings Filter type -------------------------------------------------
    /** Substrings Filter Type Tag */
    public static int SUBSTRINGS_FILTER_TYPE_TAG = 2;

    /** Substrings Filter Type Value */
    public static int SUBSTRINGS_FILTER_TYPE_VALUE = 3;

    // Substrings Filter sequence Substrings -------------------------------------------
    /** Substrings Filter Substrings sequence Tag */
    public static int SUBSTRINGS_FILTER_SUBSTRINGS_SEQ_TAG = 4;

    /** Substrings Filter Substrings sequence Value */
    public static int SUBSTRINGS_FILTER_SUBSTRINGS_SEQ_VALUE = 5;

    // Substrings Filter Substrings Initial -----------------------------------
    /** Substrings Filter Substrings initial Tag */
    public static int SUBSTRINGS_FILTER_SUBSTRINGS_INITIAL_OR_ANY_OR_FINAL_TAG = 6;

    /** Substrings Filter Substrings Initial Value */
    public static int SUBSTRINGS_FILTER_SUBSTRINGS_INITIAL_VALUE = 7;

    // Substrings Filter Substrings Substrings Any ---------------------------------------
    /** Substrings Filter Substrings Substrings Any Tag */
    public static int SUBSTRINGS_FILTER_SUBSTRINGS_ANY_OR_FINAL_TAG = 8;

    /** Substrings Filter Substrings Substrings Any Value */
    public static int SUBSTRINGS_FILTER_SUBSTRINGS_ANY_VALUE = 9;

    // Substrings Filter Substrings Substrings Final -------------------------------------
    /** Substrings Filter Substrings Substrings Final Tag */
    public static int SUBSTRINGS_FILTER_SUBSTRINGS_FINAL_TAG = 10;

    /** Substrings Filter Substrings Substrings Final Value */
    public static int SUBSTRINGS_FILTER_SUBSTRINGS_FINAL_VALUE = 11;

    /** The last state */
    public static int LAST_SUBSTRING_FILTER_STATE = 12;

    //=========================================================================
    // Matching rule assertion Filter grammar states 
    //=========================================================================
    // Matching rule assertion -----------------------------------------
    /** Matching rule assertion Tag */
    public static int MATCHING_RULE_ASSERTION_TAG = 0;

    /** Matching rule assertion Value */
    public static int MATCHING_RULE_ASSERTION_VALUE = 1;

    // Matching rule assertion Matching rule ---------------------------------------------------
    /** Matching rule assertion matching rule assertion rule Tag */
    public static int MATCHING_RULE_ASSERTION_MATCHING_RULE_OR_TYPE_TAG = 2;

    /** Matching rule assertion matching rule Value */
    public static int MATCHING_RULE_ASSERTION_MATCHING_RULE_VALUE = 3;

    // Matching rule assertion type ---------------------------------------------------
    /** Matching rule assertion type Tag */
    public static int MATCHING_RULE_ASSERTION_TYPE_OR_MATCH_VALUE_TAG = 4;

    /** Matching rule assertion type Value */
    public static int MATCHING_RULE_ASSERTION_TYPE_VALUE = 5;

    // Matching rule assertion matchValue ---------------------------------------------
    /** Matching rule assertion matchValue Tag */
    public static int MATCHING_RULE_ASSERTION_MATCH_VALUE_TAG = 6;

    /** Matching rule assertion  matchValue Value */
    public static int MATCHING_RULE_ASSERTION_MATCH_VALUE_VALUE = 7;

    // Matching rule assertion  dnAttributes -------------------------------------------
    /** Matching rule assertion  dnAttributes Tag */
    public static int MATCHING_RULE_ASSERTION_DN_ATTRIBUTES_TAG = 8;

    /** Matching rule assertion  dnAttributes Value */
    public static int MATCHING_RULE_ASSERTION_DN_ATTRIBUTES_VALUE = 9;

    /** The last state */
    public static int LAST_MATCHING_RULE_ASSERTION_STATE = 10;

    //=========================================================================
    // Search Result Entry grammar states 
    //=========================================================================
    // Search Result Entry ----------------------------------------------------
    /** Search Result Entry Tag */
    public static int SEARCH_RESULT_ENTRY_TAG = 0;

    /** Search Result Entry Value */
    public static int SEARCH_RESULT_ENTRY_VALUE = 1;

    // Object Name ------------------------------------------------------------
    /** Object Name Tag */
    public static int SEARCH_RESULT_ENTRY_OBJECT_NAME_TAG = 2;

    /** Object Name Value */
    public static int SEARCH_RESULT_ENTRY_OBJECT_NAME_VALUE = 3;

    // Attributes -------------------------------------------------------------
    /** Attributes Tag */
    public static int SEARCH_RESULT_ENTRY_ATTRIBUTES_TAG = 4;

    /** Attributes Value */
    public static int SEARCH_RESULT_ENTRY_ATTRIBUTES_VALUE = 5;

    // Partial Attributes List ------------------------------------------------
    /** Partial Attributes List Tag */
    public static int SEARCH_RESULT_ENTRY_PARTIAL_ATTRIBUTE_LIST_TAG = 6;

    /** Partial Attributes List Value */
    public static int SEARCH_RESULT_ENTRY_PARTIAL_ATTRIBUTE_LIST_VALUE = 7;

    // Attribute type ---------------------------------------------------------
    /** Type Tag */
    public static int SEARCH_RESULT_ENTRY_TYPE_TAG = 8;

    /** Type Value */
    public static int SEARCH_RESULT_ENTRY_TYPE_VALUE = 9;

    // Vals  ------------------------------------------------------------------
    /** Attribute vals Tag */
    public static int SEARCH_RESULT_ENTRY_VALS_TAG = 10;

    /** Attribute vals Value */
    public static int SEARCH_RESULT_ENTRY_VALS_VALUE = 11;

    // Attribute value --------------------------------------------------------
    /** Attribute value Tag */
    public static int SEARCH_RESULT_ENTRY_ATTRIBUTE_VALUE_TAG = 12;

    /** Attribute value Value */
    public static int SEARCH_RESULT_ENTRY_ATTRIBUTE_VALUE_VALUE = 13;

    // Attribute value loop or next attribute ---------------------------------
    /** Attribute value or next attribute Tag */
    public static int SEARCH_RESULT_ENTRY_ATTRIBUTE_VALUE_OR_LIST_TAG = 14;

    /** The last state */
    public static int LAST_SEARCH_RESULT_ENTRY_STATE = 15;

    //=========================================================================
    // Modify Request grammar states 
    //=========================================================================
    // Modify Request ---------------------------------------------------------
    /** Modify Request Tag */
    public static int MODIFY_REQUEST_TAG = 0;

    /** Modify Request Value */
    public static int MODIFY_REQUEST_VALUE = 1;

    // object ------------------------------------------------------------------
    /** Object Tag */
    public static int MODIFY_REQUEST_OBJECT_TAG = 2;

    /** Object Value */
    public static int MODIFY_REQUEST_OBJECT_VALUE = 3;

    // Modifications ----------------------------------------------------------
    /** Modifications Tag */
    public static int MODIFY_REQUEST_MODIFICATIONS_TAG = 4;

    /** Modifications Value */
    public static int MODIFY_REQUEST_MODIFICATIONS_VALUE = 5;

    // Modification sequence --------------------------------------------------
    /** Modifications sequence Tag */
    public static int MODIFY_REQUEST_MODIFICATION_SEQUENCE_TAG = 6;

    /** Modifications sequence Value */
    public static int MODIFY_REQUEST_MODIFICATION_SEQUENCE_VALUE = 7;

    // Operation --------------------------------------------------------------
    /** Operation Tag */
    public static int MODIFY_REQUEST_OPERATION_TAG = 8;

    /** Operation Value */
    public static int MODIFY_REQUEST_OPERATION_VALUE = 9;

    // Modification  ----------------------------------------------------------
    /** Modification Tag */
    public static int MODIFY_REQUEST_MODIFICATION_TAG = 10;

    /** Modification Value */
    public static int MODIFY_REQUEST_MODIFICATION_VALUE = 11;

    // Type -------------------------------------------------------------------
    /** Type Tag */
    public static int MODIFY_REQUEST_TYPE_TAG = 12;

    /** Type Value */
    public static int MODIFY_REQUEST_TYPE_VALUE = 13;

    // Vals -------------------------------------------------------------------
    /** Vals Tag */
    public static int MODIFY_REQUEST_VALS_TAG = 14;

    /** Vals Value */
    public static int MODIFY_REQUEST_VALS_VALUE = 15;

    // Vals -------------------------------------------------------------------
    /** Attribute value Tag */
    public static int MODIFY_REQUEST_ATTRIBUTE_VALUE_TAG = 16;

    /** Attribute value Value*/
    public static int MODIFY_REQUEST_ATTRIBUTE_VALUE_VALUE = 17;

    /** The loop */
    public static int MODIFY_REQUEST_ATTRIBUTE_VALUE_OR_MODIFICATION_TAG = 18;

    /** The last state */
    public static int LAST_MODIFY_REQUEST_STATE = 19;

    //=========================================================================
    // Search Result Reference grammar states 
    //=========================================================================
    // Search Result Reference ------------------------------------------------
    /** Search Result Reference Tag */
    public static int SEARCH_RESULT_REFERENCE_TAG = 0;

    /** Search Result Reference Value */
    public static int SEARCH_RESULT_REFERENCE_VALUE = 1;

    // Search Result Reference Ldap URL ---------------------------------------
    /** Search Result Reference Ldap Url Tag */
    public static int SEARCH_RESULT_REFERENCE_LDAP_URL_TAG = 2;

    /** Search Result Reference Ldap Url Value */
    public static int SEARCH_RESULT_REFERENCE_LDAP_URL_VALUE = 3;

    /** Ldap Url Loop Tag */
    public static int SEARCH_RESULT_REFERENCE_LOOP_OR_END_TAG = 4;

    /** The last state */
    public static int LAST_SEARCH_RESULT_REFERENCE_STATE_STATE = 5;

    //=========================================================================
    // Add Request grammar states 
    //=========================================================================
    // Add Request ------------------------------------------------------------
    /** Add Request Tag */
    public static int ADD_REQUEST_TAG = 0;

    /** Add Request Value */
    public static int ADD_REQUEST_VALUE = 1;

    // entry ------------------------------------------------------------------
    /** Entry Tag */
    public static int ADD_REQUEST_ENTRY_TAG = 2;

    /** Entry Value */
    public static int ADD_REQUEST_ENTRY_VALUE = 3;

    // Attributes -------------------------------------------------------------
    /** Attribute list Tag */
    public static int ADD_REQUEST_ATTRIBUTE_LIST_TAG = 4;

    /** Attribute list Value */
    public static int ADD_REQUEST_ATTRIBUTE_LIST_VALUE = 5;

    // Attribute -------------------------------------------------------------è
    /** Attribute Tag */
    public static int ADD_REQUEST_ATTRIBUTE_TAG = 6;

    /** Attribute Value */
    public static int ADD_REQUEST_ATTRIBUTE_VALUE = 7;

    // Attribute type ---------------------------------------------------------
    /** Attribute type Tag */
    public static int ADD_REQUEST_ATTRIBUTE_TYPE_TAG = 8;

    /** Attribute type Value */
    public static int ADD_REQUEST_ATTRIBUTE_TYPE_VALUE = 9;

    // Attribute vals ---------------------------------------------------------
    /** Attribute vals Tag */
    public static int ADD_REQUEST_ATTRIBUTE_VALS_TAG = 10;

    /** Attribute vals Value */
    public static int ADD_REQUEST_ATTRIBUTE_VALS_VALUE = 11;

    // Attribute val ----------------------------------------------------------
    /** Attribute val Tag */
    public static int ADD_REQUEST_ATTRIBUTE_VAL_TAG = 10;

    /** Attribute val Value */
    public static int ADD_REQUEST_ATTRIBUTE_VAL_VALUE = 11;

    /** Attribute val Loop */
    public static int ADD_REQUEST_ATTRIBUTE_VAL_OR_ATTRIBUTE_OR_END = 12;

    /** The last state */
    public static int LAST_ADD_REQUEST_STATE = 13;

    //=========================================================================
    // Modify DN Request grammar states 
    //=========================================================================
    // Modify DN Request ------------------------------------------------------
    /** Modify DN Tag */
    public static int MODIFY_DN_REQUEST_TAG = 0;

    /** Modify DN Value */
    public static int MODIFY_DN_REQUEST_VALUE = 1;

    // entry ------------------------------------------------------------------
    /** Entry Tag */
    public static int MODIFY_DN_REQUEST_ENTRY_TAG = 2;

    /** Entry Value */
    public static int MODIFY_DN_REQUEST_ENTRY_VALUE = 3;

    // New RDN ----------------------------------------------------------------
    /** New RDN Tag */
    public static int MODIFY_DN_REQUEST_NEW_RDN_TAG = 4;

    /** Enw RDN Value */
    public static int MODIFY_DN_REQUEST_NEW_RDN_VALUE = 5;

    // Delete old RDN ---------------------------------------------------------
    /** Delete old RDN Tag */
    public static int MODIFY_DN_REQUEST_DELETE_OLD_RDN_TAG = 6;

    /** Delete old RDN Value */
    public static int MODIFY_DN_REQUEST_DELETE_OLD_RDN_VALUE = 7;

    // New superior -----------------------------------------------------------
    /** New superior Tag */
    public static int MODIFY_DN_REQUEST_NEW_SUPERIOR_TAG = 8;

    /** New superior Value */
    public static int MODIFY_DN_REQUEST_NEW_SUPERIOR_VALUE = 9;

    /** The last state */
    public static int LAST_MODIFY_DN_REQUEST_STATE = 10;

    //=========================================================================
    // Del Request grammar states 
    //=========================================================================
    // Del Request ------------------------------------------------------------
    /** Del Tag */
    public static int DEL_REQUEST_TAG = 0;

    /** Del Value */
    public static int DEL_REQUEST_VALUE = 1;

    /** The last state */
    public static int LAST_DEL_REQUEST_STATE = 2;

    //=========================================================================
    // Compare Request grammar states 
    //=========================================================================
    // Compare Request --------------------------------------------------------
    /** Compare Tag */
    public static int COMPARE_REQUEST_TAG = 0;

    /** Compare Value */
    public static int COMPARE_REQUEST_VALUE = 1;

    // Entry ------------------------------------------------------------------
    /** Entry Tag */
    public static int COMPARE_REQUEST_ENTRY_TAG = 2;

    /** Entry Value */
    public static int COMPARE_REQUEST_ENTRY_VALUE = 3;

    // AVA --------------------------------------------------------------------
    /** AVA Tag */
    public static int COMPARE_REQUEST_AVA_TAG = 4;

    /** AVA Value */
    public static int COMPARE_REQUEST_AVA_VALUE = 5;

    // Attribute desc ---------------------------------------------------------
    /** Attribute desc Tag */
    public static int COMPARE_REQUEST_ATTRIBUTE_DESC_TAG = 6;

    /** Attribute desc Value */
    public static int COMPARE_REQUEST_ATTRIBUTE_DESC_VALUE = 7;

    // Assertion value --------------------------------------------------------
    /** Assertion value Tag */
    public static int COMPARE_REQUEST_ASSERTION_VALUE_TAG = 8;

    /** Assertion value Value */
    public static int COMPARE_REQUEST_ASSERTION_VALUE_VALUE = 9;

    /** The last state */
    public static int LAST_COMPARE_REQUEST_STATE = 10;

    //=========================================================================
    // Extended Request grammar states 
    //=========================================================================
    // Extended Request -------------------------------------------------------
    /** Extended Tag */
    public static int EXTENDED_REQUEST_TAG = 0;

    /** Extended Value */
    public static int EXTENDED_REQUEST_VALUE = 1;

    // Name -------------------------------------------------------------------
    /** Name Tag */
    public static int EXTENDED_REQUEST_NAME_TAG = 2;

    /** Name Value */
    public static int EXTENDED_REQUEST_NAME_VALUE = 3;

    // Value ------------------------------------------------------------------
    /** Value Tag */
    public static int EXTENDED_REQUEST_VALUE_TAG = 4;

    /** Value Value */
    public static int EXTENDED_REQUEST_VALUE_VALUE = 5;

    /** The last state */
    public static int LAST_EXTENDED_REQUEST_STATE = 6;

    //=========================================================================
    // Extended Response grammar states 
    //=========================================================================
    // Extended Response ------------------------------------------------------
    /** Extended Response Tag */
    public static int EXTENDED_RESPONSE_TAG = 0;

    /** Extended Response Value */
    public static int EXTENDED_RESPONSE_VALUE = 1;

    /** Extended Response Ldap Result (we will switch the grammar here) */
    public static int EXTENDED_RESPONSE_LDAP_RESULT = 2;

    // Name -------------------------------------------------------------------
    /** Name Value */
    public static int EXTENDED_RESPONSE_NAME_VALUE = 4;

    // Response ------------------------------------------------------------------
    /** Response Tag */
    public static int EXTENDED_RESPONSE_RESPONSE_TAG = 5;

    /** Response Value */
    public static int EXTENDED_RESPONSE_RESPONSE_VALUE = 6;

    /** The last state */
    public static int LAST_EXTENDED_RESPONSE_STATE = 7;

    //=========================================================================
    // Grammars declaration.
    //=========================================================================
    /** Ldap Message Grammar */
    public static final int LDAP_MESSAGE_GRAMMAR_SWITCH = 0x0100;

    /** LdapMessage grammar number */
    public static final int LDAP_MESSAGE_GRAMMAR = 0;

    /** Ldap Result Grammar */
    public static final int LDAP_RESULT_GRAMMAR_SWITCH = 0x0200;

    /** LdapResult grammar number */
    public static final int LDAP_RESULT_GRAMMAR = 1;

    /** Ldap Control Grammar */
    public static final int LDAP_CONTROL_GRAMMAR_SWITCH = 0x0300;

    /** LdapControl grammar number */
    public static final int LDAP_CONTROL_GRAMMAR = 2;

    /** Bind Request  Grammar */
    public static final int BIND_REQUEST_GRAMMAR_SWITCH = 0x0400;

    /** BindRequest grammar number */
    public static final int BIND_REQUEST_GRAMMAR = 3;

    /** BindResponse Grammar */
    public static final int BIND_RESPONSE_GRAMMAR_SWITCH = 0x0500;

    /** BindResponse number */
    public static final int BIND_RESPONSE_GRAMMAR = 4;

    /** UnBindRequest Grammar */
    public static final int UNBIND_REQUEST_GRAMMAR_SWITCH = 0x0600;

    /** UnBindRequest number */
    public static final int UNBIND_REQUEST_GRAMMAR = 5;

    /** AbandonRequest Grammar */
    public static final int ABANDON_REQUEST_GRAMMAR_SWITCH = 0x0700;

    /** AbandonRequest number */
    public static final int ABANDON_REQUEST_GRAMMAR = 6;

    /** AddResponse Grammar */
    public static final int ADD_RESPONSE_GRAMMAR_SWITCH = 0x0800;

    /** AddResponse number */
    public static final int ADD_RESPONSE_GRAMMAR = 7;

    /** CompareResponse Grammar */
    public static final int COMPARE_RESPONSE_GRAMMAR_SWITCH = 0x0900;

    /** CompareResponse number */
    public static final int COMPARE_RESPONSE_GRAMMAR = 8;

    /** DelResponse Grammar */
    public static final int DEL_RESPONSE_GRAMMAR_SWITCH = 0x0A00;

    /** DelResponse number */
    public static final int DEL_RESPONSE_GRAMMAR = 9;

    /** ModifyResponse Grammar */
    public static final int MODIFY_RESPONSE_GRAMMAR_SWITCH = 0x0B00;

    /** ModifyResponse number */
    public static final int MODIFY_RESPONSE_GRAMMAR = 10;

    /** ModifyDNResponse Grammar */
    public static final int MODIFY_DN_RESPONSE_GRAMMAR_SWITCH = 0x0C00;

    /** ModifyDNResponse number */
    public static final int MODIFY_DN_RESPONSE_GRAMMAR = 11;

    /** SearchResultDone Grammar */
    public static final int SEARCH_RESULT_DONE_GRAMMAR_SWITCH = 0x0D00;

    /** SearchResultDone number */
    public static final int SEARCH_RESULT_DONE_GRAMMAR = 12;

    /** SearchRequest Grammar */
    public static final int SEARCH_REQUEST_GRAMMAR_SWITCH = 0x0E00;

    /** SearchRequest number */
    public static final int SEARCH_REQUEST_GRAMMAR = 13;

    /** Filter Grammar */
    public static final int FILTER_GRAMMAR_SWITCH = 0x0F00;

    /** Filter number */
    public static final int FILTER_GRAMMAR = 14;

    /** SearchResultEntry Grammar */
    public static final int SEARCH_RESULT_ENTRY_GRAMMAR_SWITCH = 0x1000;

    /** SearchResultEntry number */
    public static final int SEARCH_RESULT_ENTRY_GRAMMAR = 15;

    /** ModifyRequest Grammar */
    public static final int MODIFY_REQUEST_GRAMMAR_SWITCH = 0x1100;

    /** ModifyRequest number */
    public static final int MODIFY_REQUEST_GRAMMAR = 16;

    /** SearchResultReference Grammar */
    public static final int SEARCH_RESULT_REFERENCE_GRAMMAR_SWITCH = 0x1200;

    /** SearchResultReference number */
    public static final int SEARCH_RESULT_REFERENCE_GRAMMAR = 17;

    /** AddRequest Grammar */
    public static final int ADD_REQUEST_GRAMMAR_SWITCH = 0x1300;

    /** AddRequest number */
    public static final int ADD_REQUEST_GRAMMAR = 18;

    /** ModifyDNRequest Grammar */
    public static final int MODIFY_DN_REQUEST_GRAMMAR_SWITCH = 0x1400;

    /** ModifyDNRequest number */
    public static final int MODIFY_DN_REQUEST_GRAMMAR = 19;

    /** DelRequest Grammar */
    public static final int DEL_REQUEST_GRAMMAR_SWITCH = 0x1500;

    /** DelRequest number */
    public static final int DEL_REQUEST_GRAMMAR = 20;

    /** CompareRequest Grammar */
    public static final int COMPARE_REQUEST_GRAMMAR_SWITCH = 0x1600;

    /** CompareRequest number */
    public static final int COMPARE_REQUEST_GRAMMAR = 21;

    /** ExtendedRequest Grammar */
    public static final int EXTENDED_REQUEST_GRAMMAR_SWITCH = 0x1700;

    /** ExtendedRequest number */
    public static final int EXTENDED_REQUEST_GRAMMAR = 22;

    /** ExtendedResponse Grammar */
    public static final int EXTENDED_RESPONSE_GRAMMAR_SWITCH = 0x1800;

    /** ExtendedResponse number */
    public static final int EXTENDED_RESPONSE_GRAMMAR = 23;
    
    /** SubstringFilter grammar */
    public static final int SUBSTRING_FILTER_GRAMMAR_SWITCH = 0x1900;

    /** SubstringFilter number */
    public static final int SUBSTRING_FILTER_GRAMMAR = 24;
    
    /** MatchingRuleAssertion grammar */
    public static final int MATCHING_RULE_ASSERTION_GRAMMAR_SWITCH = 0x1A00;

    /** MatchingRuleAssertion number */
    public static final int MATCHING_RULE_ASSERTION_GRAMMAR = 25;
    
    /** The total number of grammars used */
    public static final int NB_GRAMMARS = 26;

    //=========================================================================
    // Grammar switches debug strings 
    //=========================================================================
    /** A string representation of grammars */
    private static String[] GrammarSwitchString =
        new String[]
        {
            "LDAP_MESSAGE_GRAMMAR_SWITCH",
            "LDAP_RESULT_GRAMMAR_SWITCH",
            "LDAP_CONTROL_GRAMMAR_SWITCH",
            "BIND_REQUEST_GRAMMAR_SWITCH",
            "BIND_RESPONSE_GRAMMAR_SWITCH",
            "UNBIND_REQUEST_GRAMMAR_SWITCH",
            "ABANDON_RESPONSE_GRAMMAR_SWITCH",
            "ADD_RESPONSE_GRAMMAR_SWITCH",
            "COMPARE_RESPONSE_GRAMMAR_SWITCH",
            "DEL_RESPONSE_GRAMMAR_SWITCH",
            "MODIFY_RESPONSE_GRAMMAR_SWITCH",
            "MODIFY_DN_RESPONSE_GRAMMAR_SWITCH",
            "SEARCH_RESULT_DONE_GRAMMAR_SWITCH",
            "SEARCH_REQUEST_GRAMMAR_SWITCH",
            "FILTER_GRAMMAR_SWITCH",
            "SEARCH_RESULT_ENTRY_GRAMMAR_SWITCH",
            "MODIFY_REQUEST_GRAMMAR_SWITCH",
            "SEARCH_RESULT_REFERENCE_GRAMMAR_SWITCH",
            "ADD_REQUEST_GRAMMAR_SWITCH",
            "MODIFY_DN_REQUEST_GRAMMAR_SWITCH",
            "DEL_REQUEST_GRAMMAR_SWITCH",
            "COMPARE_REQUEST_GRAMMAR_SWITCH",
            "EXTENDED_REQUEST_GRAMMAR_SWITCH",
            "EXTENDED_RESPONSE_GRAMMAR_SWITCH",
            "SUBSTRING_FILTER_GRAMMAR_SWITCH",
            "MATCHING_RULE_ASSERTION_GRAMMAR_SWITCH"
        };

    //=========================================================================
    // States debug strings 
    //=========================================================================
    /** A string representation of all the states */
    private static String[] LdapMessageString =
        new String[]
        {
            "LDAP_MESSAGE_TAG",
            "LDAP_MESSAGE_VALUE",
            "LDAP_MESSAGE_ID_TAG",
            "LDAP_MESSAGE_ID_VALUE",
            "PROTOCOL_OP_TAG",
            "PROTOCOL_OP_VALUE"
        };

    /** A string representation of all the LdapResult states */
    private static String[] LdapResultString =
        new String[]
        {
            "LDAP_RESULT_CODE_TAG",
            "LDAP_RESULT_CODE_VALUE",
            "LDAP_RESULT_MATCHED_DN_TAG",
            "LDAP_RESULT_MATCHED_DN_VALUE",
            "LDAP_RESULT_ERROR_MESSAGE_TAG",
            "LDAP_RESULT_ERROR_MESSAGE_VALUE",
            "LDAP_RESULT_REFERRAL_SEQUENCE_TAG",
            "LDAP_RESULT_REFERRAL_SEQUENCE_VALUE",
            "LDAP_RESULT_REFERRAL_TAG",
            "LDAP_RESULT_REFERRAL_VALUE",
            "LDAP_RESULT_REFERRAL_LOOP_TAG"
        };

    /** A string representation of all the controls states */
    private static String[] LdapControlString =
        new String[]
        {
            "CONTROLS_TAG",
            "CONTROLS_VALUE",
            "CONTROL_TAG",
            "CONTROL_VALUE",
            "CONTROL_TYPE_TAG",
            "CONTROL_TYPE_VALUE",
            "CONTROL_LOOP_OR_CRITICAL_OR_VALUE_TAG",
            "CONTROL_CRITICALITY_VALUE",
            "CONTROL_LOOP_OR_VALUE_TAG",
            "CONTROL_VALUE_VALUE",
            "CONTROL_LOOP_OR_END_TAG"
        };

    /** A string representation of all the Bind Request states */
    private static String[] BindRequestString =
        new String[]
        {
            "BIND_REQUEST_TAG",
            "BIND_REQUEST_VALUE",
            "BIND_REQUEST_VERSION_TAG",
            "BIND_REQUEST_VERSION_VALUE",
            "BIND_REQUEST_NAME_TAG",
            "BIND_REQUEST_NAME_VALUE",
            "BIND_REQUEST_AUTHENTICATION_CHOICE_TAG",
            "BIND_REQUEST_AUTHENTICATION_SIMPLE_VALUE",
            "BIND_REQUEST_AUTHENTICATION_SASL_VALUE",
            "BIND_REQUEST_AUTHENTICATION_MECHANISM_TAG",
            "BIND_REQUEST_AUTHENTICATION_MECHANISM_VALUE",
            "BIND_REQUEST_AUTHENTICATION_CREDENTIALS_TAG",
            "BIND_REQUEST_AUTHENTICATION_CREDENTIALS_VALUE",
        };

    /** A string representation of all the BindResponse states */
    private static String[] BindResponseString =
        new String[]
        {
            "BIND_RESPONSE_TAG",
            "BIND_RESPONSE_VALUE",
            "BIND_RESPONSE_LDAP_RESULT",
            "BIND_RESPONSE_SERVER_SASL_CREDS_TAG",
            "BIND_RESPONSE_SERVER_SASL_CREDS_VALUE"
        };

    /** A string representation of all the Unbind Request states */
    private static String[] UnBindRequestString =
        new String[]
        {
            "UNBIND_REQUEST_TAG",
            "UNBIND_REQUEST_VALUE"
        };

    /** A string representation of all the Abandon Request states */
    private static String[] AbandonRequestString =
        new String[]
        {
            "ABANDON_REQUEST_MESSAGE_ID_TAG",
            "ABANDON_REQUEST_MESSAGE_ID_VALUE"
        };

    /** A string representation of all the Add Response states */
    private static String[] AddResponseString =
        new String[]
        {
            "ADD_RESPONSE_TAG",
            "ADD_RESPONSE_VALUE",
            "ADD_RESPONSE_LDAP_RESULT"
        };

    /** A string representation of all the Compare Response states */
    private static String[] CompareResponseString =
        new String[]
        {
            "COMPARE_RESPONSE_TAG",
            "COMPARE_RESPONSE_VALUE",
            "COMPARE_RESPONSE_LDAP_RESULT"
        };

    /** A string representation of all the Del Response states */
    private static String[] DelResponseString =
        new String[]
        {
            "DEL_RESPONSE_TAG",
            "DEL_RESPONSE_VALUE",
            "DEL_RESPONSE_LDAP_RESULT"
        };

    /** A string representation of all the Modify Response states */
    private static String[] ModifyResponseString =
        new String[]
        {
            "MODIFY_RESPONSE_TAG",
            "MODIFY_RESPONSE_VALUE",
            "MODIFY_RESPONSE_LDAP_RESULT"
        };

    /** A string representation of all the Modify DN Response states */
    private static String[] ModifyDNResponseString =
        new String[]
        {
            "MODIFY_DN_RESPONSE_TAG",
            "MODIFY_DN_RESPONSE_VALUE",
            "MODIFY_DN_RESPONSE_LDAP_RESULT"
        };

    /** A string representation of all the Search Result Done states */
    private static String[] SearchResultDoneString =
        new String[]
        {
            "SEARCH_RESULT_DONE_TAG",
            "SEARCH_RESULT_DONE_VALUE",
            "SEARCH_RESULT_DONE_LDAP_RESULT"
        };

    /** A string representation of all the Search Request states */
    private static String[] SearchRequestString =
        new String[]
        {
            "SEARCH_REQUEST_TAG",
            "SEARCH_REQUEST_VALUE",
            "SEARCH_REQUEST_BASE_OBJECT_TAG",
            "SEARCH_REQUEST_BASE_OBJECT_VALUE",
            "SEARCH_REQUEST_SCOPE_TAG",
            "SEARCH_REQUEST_SCOPE_VALUE",
            "SEARCH_REQUEST_DEREF_ALIASES_TAG",
            "SEARCH_REQUEST_DEREF_ALIASES_VALUE",
            "SEARCH_REQUEST_SIZE_LIMIT_TAG",
            "SEARCH_REQUEST_SIZE_LIMIT_VALUE",
            "SEARCH_REQUEST_TIME_LIMIT_TAG",
            "SEARCH_REQUEST_TIME_LIMIT_VALUE",
            "SEARCH_REQUEST_TYPES_ONLY_TAG",
            "SEARCH_REQUEST_TYPES_ONLY_VALUE",
            "SEARCH_REQUEST_FILTER",
            "SEARCH_REQUEST_ATTRIBUTE_DESCRIPTION_LIST_TAG",
            "SEARCH_REQUEST_ATTRIBUTE_DESCRIPTION_LIST_VALUE",
            "SEARCH_REQUEST_ATTRIBUTE_DESCRIPTION_TAG",
            "SEARCH_REQUEST_ATTRIBUTE_DESCRIPTION_VALUE",
            "SEARCH_REQUEST_ATTRIBUTE_DESCRIPTION_LOOP_TAG"
        };

    /** A string representation of all the Filter states */
    private static String[] FilterString =
        new String[]
        {
            "FILTER_TAG",
            "FILTER_AND_TAG",
            "FILTER_AND_VALUE",
            "FILTER_OR_TAG",
            "FILTER_OR_VALUE",
            "FILTER_NOT_TAG",
            "FILTER_NOT_VALUE",
            "FILTER_EQUALITY_MATCH_TAG",
            "FILTER_EQUALITY_MATCH_VALUE",
            "FILTER_SUBSTRINGS_TAG",
            "FILTER_SUBSTRINGS_VALUE",
            "FILTER_GREATER_OR_EQUAL_TAG",
            "FILTER_GREATER_OR_EQUAL_VALUE",
            "FILTER_LESS_OR_EQUAL_TAG",
            "FILTER_LESS_OR_EQUAL_VALUE",
            "FILTER_PRESENT_TAG",
            "FILTER_PRESENT_VALUE",
            "FILTER_APPROX_MATCH_TAG",
            "FILTER_APPROX_MATCH_VALUE",
            "FILTER_EXTENSIBLE_MATCH_TAG",
            "FILTER_EXTENSIBLE_MATCH_VALUE",
            "FILTER_LOOP_TAG",
            "FILTER_ATTRIBUTE_DESC_TAG",
            "FILTER_ATTRIBUTE_DESC_VALUE",
            "FILTER_ASSERTION_VALUE_TAG",
            "FILTER_ASSERTION_VALUE_VALUE",
        };

    /** A string representation of all the search result entry states */
    private static String[] SearchResultEntryString =
        new String[]
        {
            "SEARCH_RESULT_ENTRY_TAG",
            "SEARCH_RESULT_ENTRY_VALUE",
            "SEARCH_RESULT_ENTRY_OBJECT_NAME_TAG",
            "SEARCH_RESULT_ENTRY_OBJECT_NAME_VALUE",
            "SEARCH_RESULT_ENTRY_ATTRIBUTES_TAG",
            "SEARCH_RESULT_ENTRY_ATTRIBUTES_VALUE",
            "SEARCH_RESULT_ENTRY_PARTIAL_ATTRIBUTE_LIST_TAG",
            "SEARCH_RESULT_ENTRY_PARTIAL_ATTRIBUTE_LIST_VALUE",
            "SEARCH_RESULT_ENTRY_TYPE_TAG",
            "SEARCH_RESULT_ENTRY_TYPE_VALUE",
            "SEARCH_RESULT_ENTRY_VALS_TAG",
            "SEARCH_RESULT_ENTRY_VALS_VALUE",
            "SEARCH_RESULT_ENTRY_ATTRIBUTE_VALUE_TAG",
            "SEARCH_RESULT_ENTRY_ATTRIBUTE_VALUE_VALUE",
            "SEARCH_RESULT_ENTRY_ATTRIBUTE_VALUE_OR_LIST_TAG"
        };

    /** A string representation of all the modify request states */
    private static String[] ModifyRequestString =
        new String[]
        {
            "MODIFY_REQUEST_TAG",
            "MODIFY_REQUEST_VALUE",
            "MODIFY_REQUEST_OBJECT_TAG",
            "MODIFY_REQUEST_OBJECT_VALUE",
            "MODIFY_REQUEST_MODIFICATIONS_TAG",
            "MODIFY_REQUEST_MODIFICATIONS_VALUE",
            "MODIFY_REQUEST_MODIFICATION_SEQUENCE_TAG",
            "MODIFY_REQUEST_MODIFICATION_SEQUENCE_VALUE",
            "MODIFY_REQUEST_OPERATION_TAG",
            "MODIFY_REQUEST_OPERATION_VALUE",
            "MODIFY_REQUEST_MODIFICATION_TAG",
            "MODIFY_REQUEST_MODIFICATION_VALUE",
            "MODIFY_REQUEST_TYPE_TAG",
            "MODIFY_REQUEST_TYPE_VALUE",
            "MODIFY_REQUEST_VALS_TAG",
            "MODIFY_REQUEST_VALS_VALUE",
            "MODIFY_REQUEST_ATTRIBUTE_VALUE_TAG",
            "MODIFY_REQUEST_ATTRIBUTE_VALUE_VALUE",
            "MODIFY_REQUEST_ATTRIBUTE_VALUE_OR_MODIFICATION_TAG"
        };

    /** A string representation of all the search result reference states */
    private static String[] SearchResultReferenceString =
        new String[]
        {
            "SEARCH_RESULT_REFERENCE_TAG",
            "SEARCH_RESULT_REFERENCE_VALUE",
            "SEARCH_RESULT_REFERENCE_LDAP_URL_TAG",
            "SEARCH_RESULT_REFERENCE_LDAP_URL_VALUE",
            "SEARCH_RESULT_REFERENCE_LOOP_OR_END_TAG"
        };

    /** A string representation of all the add request states */
    private static String[] AddRequestString =
        new String[]
        {
            "ADD_REQUEST_TAG",
            "ADD_REQUEST_VALUE",
            "ADD_REQUEST_ENTRY_TAG",
            "ADD_REQUEST_ENTRY_VALUE",
            "ADD_REQUEST_ATTRIBUTE_LIST_TAG",
            "ADD_REQUEST_ATTRIBUTE_LIST_VALUE",
            "ADD_REQUEST_ATTRIBUTE_TAG",
            "ADD_REQUEST_ATTRIBUTE_VALUE",
            "ADD_REQUEST_ATTRIBUTE_TYPE_TAG",
            "ADD_REQUEST_ATTRIBUTE_TYPE_VALUE",
            "ADD_REQUEST_ATTRIBUTE_VALS_TAG",
            "ADD_REQUEST_ATTRIBUTE_VALS_VALUE",
            "ADD_REQUEST_ATTRIBUTE_VAL_TAG",
            "ADD_REQUEST_ATTRIBUTE_VAL_VALUE",
            "ADD_REQUEST_ATTRIBUTE_VAL_OR_ATTRIBUTE_OR_END"
        };

    /** A string representation of all the ModifyDN request states */
    private static String[] ModifyDNRequestString =
        new String[]
        {
            "MODIFY_DN_REQUEST_TAG",
            "MODIFY_DN_REQUEST_VALUE",
            "MODIFY_DN_REQUEST_ENTRY_TAG",
            "MODIFY_DN_REQUEST_ENTRY_VALUE",
            "MODIFY_DN_REQUEST_NEW_RDN_TAG",
            "MODIFY_DN_REQUEST_NEW_RDN_VALUE",
            "MODIFY_DN_REQUEST_DELETE_OLD_RDN_TAG",
            "MODIFY_DN_REQUEST_DELETE_OLD_RDN_VALUE",
            "MODIFY_DN_REQUEST_NEW_SUPERIOR_TAG",
            "MODIFY_DN_REQUEST_NEW_SUPERIOR_VALUE"
        };

    /** A string representation of all the delete request states */
    private static String[] DelRequestString =
        new String[]
        {
            "DEL_REQUEST_TAG",
            "DEL_REQUEST_VALUE"
        };

    /** A string representation of all the compare request states */
    private static String[] CompareRequestString =
        new String[]
        {
            "COMPARE_REQUEST_TAG",
            "COMPARE_REQUEST_VALUE",
            "COMPARE_REQUEST_ENTRY_TAG",
            "COMPARE_REQUEST_ENTRY_VALUE",
            "COMPARE_REQUEST_AVA_TAG",
            "COMPARE_REQUEST_AVA_VALUE",
            "COMPARE_REQUEST_ATTRIBUTE_DESC_TAG",
            "COMPARE_REQUEST_ATTRIBUTE_DESC_VALUE",
            "COMPARE_REQUEST_ASSERTION_VALUE_TAG",
            "COMPARE_REQUEST_ASSERTION_VALUE_VALUE"
        };

    /** A string representation of all the extended request states */
    private static String[] ExtendedRequestString =
        new String[]
        {
            "EXTENDED_REQUEST_TAG",
            "EXTENDED_REQUEST_VALUE",
            "EXTENDED_REQUEST_NAME_TAG",
            "EXTENDED_REQUEST_NAME_VALUE",
            "EXTENDED_REQUEST_VALUE_TAG",
            "EXTENDED_REQUEST_VALUE_VALUE"
        };

    /** A string representation of all the extended response states */
    private static String[] ExtendedResponseString =
        new String[]
        {
            "EXTENDED_RESPONSE_TAG",
            "EXTENDED_RESPONSE_VALUE",
            "EXTENDED_RESPONSE_LDAP_RESULT",
            "EXTENDED_RESPONSE_NAME_TAG",
            "EXTENDED_RESPONSE_NAME_VALUE",
            "EXTENDED_RESPONSE_VALUE_TAG",
            "EXTENDED_RESPONSE_VALUE_VALUE"
        };

    /** A string representation of all the substring filter states */
    private static String[] SubstringFilterString =
        new String[]
        {
            "SUBSTRINGS_FILTER_TAG",
            "SUBSTRINGS_FILTER_VALUE",
            "SUBSTRINGS_FILTER_TYPE_TAG",
            "SUBSTRINGS_FILTER_TYPE_VALUE",
            "SUBSTRINGS_FILTER_SUBSTRINGS_SEQ_TAG",
            "SUBSTRINGS_FILTER_SUBSTRINGS_SEQ_VALUE",
            "SUBSTRINGS_FILTER_SUBSTRINGS_INITIAL_OR_ANY_OR_FINAL_TAG",
            "SUBSTRINGS_FILTER_SUBSTRINGS_INITIAL_VALUE",
            "SUBSTRINGS_FILTER_SUBSTRINGS_ANY_OR_FINAL_TAG",
            "SUBSTRINGS_FILTER_SUBSTRINGS_ANY_VALUE",
            "SUBSTRINGS_FILTER_SUBSTRINGS_FINAL_TAG",
            "SUBSTRINGS_FILTER_SUBSTRINGS_FINAL_VALUE"
        };

    /** A string representation of all the Matching Rule Assertion states */
    private static String[] MatchingRuleAssertionString =
        new String[]
        {
            "MATCHING_RULE_ASSERTION_TAG",
            "MATCHING_RULE_ASSERTION_VALUE",
            "MATCHING_RULE_ASSERTION_MATCHING_RULE_OR_TYPE_TAG",
            "MATCHING_RULE_ASSERTION_MATCHING_RULE_VALUE",
            "MATCHING_RULE_ASSERTION_TYPE_OR_MATCH_VALUE_TAG",
            "MATCHING_RULE_ASSERTION_TYPE_VALUE",
            "MATCHING_RULE_ASSERTION_MATCH_VALUE_TAG",
            "MATCHING_RULE_ASSERTION_MATCH_VALUE_VALUE",
            "MATCHING_RULE_ASSERTION_DN_ATTRIBUTES_TAG",
            "MATCHING_RULE_ASSERTION_DN_ATTRIBUTES_VALUE"
        };

    /** The instance */
    private static LdapStatesEnum instance = new LdapStatesEnum();

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * This is a private constructor. This class is a singleton
     *
     */
    private LdapStatesEnum()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Get an instance of this class
     * @return An instance on this class
     */
    public static IStates getInstance()
    {
        return instance;
    }

    /**
     * Get the grammar name
     * @param grammar The grammar code
     * @return The grammar name
     */
    public String getGrammarName(int grammar)
    {
        switch (grammar)
        {
            case LDAP_MESSAGE_GRAMMAR            : return "LDAP_MESSAGE_GRAMMAR";
            case LDAP_RESULT_GRAMMAR             : return "LDAP_RESULT_GRAMMAR";
            case LDAP_CONTROL_GRAMMAR            : return "LDAP_CONTROL_GRAMMAR";
            case BIND_REQUEST_GRAMMAR            : return "BIND_REQUEST_GRAMMAR";
            case BIND_RESPONSE_GRAMMAR           : return "BIND_RESPONSE_GRAMMAR";
            case UNBIND_REQUEST_GRAMMAR          : return "UNBIND_REQUEST_GRAMMAR";
            case ABANDON_REQUEST_GRAMMAR         : return "ABANDON_REQUEST_GRAMMAR";
            case ADD_RESPONSE_GRAMMAR            : return "ADD_RESPONSE_GRAMMAR";
            case COMPARE_RESPONSE_GRAMMAR        : return "COMPARE_RESPONSE_GRAMMAR";
            case DEL_RESPONSE_GRAMMAR            : return "DEL_RESPONSE_GRAMMAR";
            case MODIFY_RESPONSE_GRAMMAR         : return "MODIFY_RESPONSE_GRAMMAR";
            case MODIFY_DN_RESPONSE_GRAMMAR      : return "MODIFY_DN_RESPONSE_GRAMMAR";
            case SEARCH_RESULT_DONE_GRAMMAR      : return "SEARCH_RESULT_DONE_GRAMMAR";
            case SEARCH_REQUEST_GRAMMAR          : return "SEARCH_REQUEST_GRAMMAR";
            case FILTER_GRAMMAR                  : return "FILTER_GRAMMAR";
            case SEARCH_RESULT_ENTRY_GRAMMAR     : return "SEARCH_RESULT_ENTRY_GRAMMAR";
            case MODIFY_REQUEST_GRAMMAR          : return "MODIFY_REQUEST_GRAMMAR";
            case SEARCH_RESULT_REFERENCE_GRAMMAR : return "SEARCH_RESULT_REFERENCE_GRAMMAR";
            case ADD_REQUEST_GRAMMAR             : return "ADD_REQUEST_GRAMMAR";
            case MODIFY_DN_REQUEST_GRAMMAR       : return "MODIFY_DN_REQUEST_GRAMMAR";
            case DEL_REQUEST_GRAMMAR             : return "DEL_REQUEST_GRAMMAR";
            case COMPARE_REQUEST_GRAMMAR         : return "COMPARE_REQUEST_GRAMMAR";
            case EXTENDED_REQUEST_GRAMMAR        : return "EXTENDED_REQUEST_GRAMMAR";
            case EXTENDED_RESPONSE_GRAMMAR       : return "EXTENDED_RESPONSE_GRAMMAR";
            case SUBSTRING_FILTER_GRAMMAR        : return "SUBSTRING_FILTER_GRAMMAR";
            case MATCHING_RULE_ASSERTION_GRAMMAR : return "MATCHING_RULE_ASSERTION_GRAMMAR";

            default                              : return "UNKNOWN";
        }
    }

    /**
     * Get the grammar name
     * @param grammar The grammar class
     * @return The grammar name
     */
    public String getGrammarName( IGrammar grammar )
    {
        if ( grammar instanceof LdapMessageGrammar )
        {
            return "LDAP_MESSAGE_GRAMMAR";
        }
        else if ( grammar instanceof LdapResultGrammar )
        {
            return "LDAP_RESULT_GRAMMAR";
        }
        else if ( grammar instanceof LdapControlGrammar )
        {
            return "LDAP_CONTROL_GRAMMAR";
        }
        else if ( grammar instanceof BindRequestGrammar )
        {
            return "BIND_REQUEST_GRAMMAR";
        }
        else if ( grammar instanceof BindResponseGrammar )
        {
            return "BIND_RESPONSE_GRAMMAR";
        }
        else if ( grammar instanceof UnBindRequestGrammar )
        {
            return "UNBIND_REQUEST_GRAMMAR";
        }
        else if ( grammar instanceof AbandonRequestGrammar )
        {
            return "ABANDON_REQUEST_GRAMMAR";
        }
        else if ( grammar instanceof AddResponseGrammar )
        {
            return "ADD_RESPONSE_GRAMMAR";
        }
        else if ( grammar instanceof CompareResponseGrammar )
        {
            return "COMPARE_RESPONSE_GRAMMAR";
        }
        else if ( grammar instanceof DelResponseGrammar )
        {
            return "DEL_RESPONSE_GRAMMAR";
        }
        else if ( grammar instanceof ModifyResponseGrammar )
        {
            return "MODIFY_RESPONSE_GRAMMAR";
        }
        else if ( grammar instanceof ModifyDNResponseGrammar )
        {
            return "MODIFY_DN_RESPONSE_GRAMMAR";
        }
        else if ( grammar instanceof SearchResultDoneGrammar )
        {
            return "SEARCH_RESULT_DONE_GRAMMAR";
        }
        else if ( grammar instanceof SearchRequestGrammar )
        {
            return "SEARCH_REQUEST_GRAMMAR";
        }
        else if ( grammar instanceof FilterGrammar )
        {
            return "FILTER_GRAMMAR";
        }
        else if ( grammar instanceof SearchResultEntryGrammar )
        {
            return "SEARCH_RESULT_ENTRY_GRAMMAR";
        }
        else if ( grammar instanceof ModifyRequestGrammar )
        {
            return "MODIFY_REQUEST_GRAMMAR";
        }
        else if ( grammar instanceof SearchResultReferenceGrammar )
        {
            return "SEARCH_RESULT_REFERENCE_GRAMMAR";
        }
        else if ( grammar instanceof AddRequestGrammar )
        {
            return "ADD_REQUEST_GRAMMAR";
        }
        else if ( grammar instanceof ModifyDNRequestGrammar )
        {
            return "MODIFY_DN_REQUEST_GRAMMAR";
        }
        else if ( grammar instanceof DelRequestGrammar )
        {
            return "DEL_REQUEST_GRAMMAR";
        }
        else if ( grammar instanceof CompareRequestGrammar )
        {
            return "COMPARE_REQUEST_GRAMMAR";
        }
        else if ( grammar instanceof ExtendedRequestGrammar )
        {
            return "EXTENDED_REQUEST_GRAMMAR";
        }
        else if ( grammar instanceof ExtendedResponseGrammar )
        {
            return "EXTENDED_RESPONSE_GRAMMAR";
        }
        else if ( grammar instanceof SubstringFilterGrammar )
        {
            return "SUBSTRING_FILTER_GRAMMAR";
        }
        else if ( grammar instanceof MatchingRuleAssertionGrammar )
        {
            return "MATCHING_RULE_ASSERTION_GRAMMAR";
        }
        else 
        {
            return "UNKNOWN GRAMMAR";
        }
    }

    /**
     * Get the string representing the state
     * 
     * @param grammar The current grammar being used
     * @param state The state number
     * @return The String representing the state
     */
    public String getState( int grammar, int state )
    {

        if ( ( state & GRAMMAR_SWITCH_MASK ) != 0 )
        {
            return ( state == END_STATE ) ? "END_STATE"
                : GrammarSwitchString[( ( state & GRAMMAR_SWITCH_MASK ) >> 8 ) - 1];
        }
        else
        {

            switch ( grammar )
            {

                case LDAP_MESSAGE_GRAMMAR :
                    return ( ( state == GRAMMAR_END ) ? "LDAP_MESSAGE_END_STATE" : LdapMessageString[state] );

                case LDAP_RESULT_GRAMMAR :
                    return ( ( state == GRAMMAR_END ) ? "LDAP_RESULT_END_STATE" : LdapResultString[state] );

                case LDAP_CONTROL_GRAMMAR :
                    return ( ( state == GRAMMAR_END ) ? "LDAP_CONTROL_END_STATE" : LdapControlString[state] );

                case BIND_REQUEST_GRAMMAR :
                    return ( ( state == GRAMMAR_END ) ? "BIND_REQUEST_END_STATE" : BindRequestString[state] );

                case BIND_RESPONSE_GRAMMAR :
                    return ( ( state == GRAMMAR_END ) ? "IND_RESPONSE_END_STATE" : BindResponseString[state] );

                case UNBIND_REQUEST_GRAMMAR :
                    return ( ( state == GRAMMAR_END ) ? "UNBIND_REQUEST_END_STATE" : UnBindRequestString[state] );

                case ABANDON_REQUEST_GRAMMAR :
                    return ( ( state == GRAMMAR_END ) ? "ABANDON_REQUEST_END_STATE" : AbandonRequestString[state] );

                case ADD_RESPONSE_GRAMMAR :
                    return ( ( state == GRAMMAR_END ) ? "ADD_RESPONSE_END_STATE" : AddResponseString[state] );

                case COMPARE_RESPONSE_GRAMMAR :
                    return ( ( state == GRAMMAR_END ) ? "COMPARE_RESPONSE_END_STATE" : CompareResponseString[state] );

                case DEL_RESPONSE_GRAMMAR :
                    return ( ( state == GRAMMAR_END ) ? "DEL_RESPONSE_END_STATE" : DelResponseString[state] );

                case MODIFY_RESPONSE_GRAMMAR :
                    return ( ( state == GRAMMAR_END ) ? "MODIFY_RESPONSE_END_STATE" : ModifyResponseString[state] );

                case MODIFY_DN_RESPONSE_GRAMMAR :
                    return ( ( state == GRAMMAR_END ) ? "MODIFY_DN_RESPONSE_END_STATE" : ModifyDNResponseString[state] );

                case SEARCH_RESULT_DONE_GRAMMAR :
                    return ( ( state == GRAMMAR_END ) ? "SEARCH_RESULT_DONE_END_STATE" : SearchResultDoneString[state] );

                case SEARCH_REQUEST_GRAMMAR :
                    return ( ( state == GRAMMAR_END ) ? "SEARCH_REQUEST_END_STATE" : SearchRequestString[state] );

                case SEARCH_RESULT_REFERENCE_GRAMMAR :
                    return ( ( state == GRAMMAR_END ) ? "SEARCH_RESULT_REFERENCE_END_STATE" : SearchResultReferenceString[state] );

                case FILTER_GRAMMAR :
                    return ( ( state == GRAMMAR_END ) ? "FILTER_END_STATE" : FilterString[state] );

                case SEARCH_RESULT_ENTRY_GRAMMAR :
                    return ( ( state == GRAMMAR_END ) ? "SEARCH_RESULT_ENTRY_END_STATE" : SearchResultEntryString[state] );

                case MODIFY_REQUEST_GRAMMAR :
                    return ( ( state == GRAMMAR_END ) ? "MODIFY_REQUEST_END_STATE" : ModifyRequestString[state] );

                case ADD_REQUEST_GRAMMAR :
                    return ( ( state == GRAMMAR_END ) ? "ADD_REQUEST_END_STATE" : AddRequestString[state] );

                case MODIFY_DN_REQUEST_GRAMMAR :
                    return ( ( state == GRAMMAR_END ) ? "MODIFY_DN_REQUEST_END_STATE" : ModifyDNRequestString[state] );

                case DEL_REQUEST_GRAMMAR :
                    return ( ( state == GRAMMAR_END ) ? "DEL_REQUEST_END_STATE" : DelRequestString[state] );

                case COMPARE_REQUEST_GRAMMAR :
                    return ( ( state == GRAMMAR_END ) ? "COMPARE_REQUEST_END_STATE" : CompareRequestString[state] );

                case EXTENDED_REQUEST_GRAMMAR :
                    return ( ( state == GRAMMAR_END ) ? "EXTENDED_REQUEST_END_STATE" : ExtendedRequestString[state] );

                case EXTENDED_RESPONSE_GRAMMAR :
                    return ( ( state == GRAMMAR_END ) ? "EXTENDED_RESPONSE_END_STATE" : ExtendedResponseString[state] );

                case SUBSTRING_FILTER_GRAMMAR :
                    return ( ( state == GRAMMAR_END ) ? "SUBSTRING_FILTER_END_STATE" : SubstringFilterString[state] );

                case MATCHING_RULE_ASSERTION_GRAMMAR :
                    return ( ( state == GRAMMAR_END ) ? "MATCHING_RULE_ASSERTION_END_STATE" : MatchingRuleAssertionString[state] );

                default :
                    return "UNKNOWN";
            }
        }
    }
}
