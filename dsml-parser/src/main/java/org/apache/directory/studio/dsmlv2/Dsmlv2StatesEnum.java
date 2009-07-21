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

package org.apache.directory.studio.dsmlv2;


/**
 * This class store the Dsml grammar's constants. It is also used for debugging
 * purpose
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class Dsmlv2StatesEnum implements IStates
{
    //====================================================
    //  <batchRequest> ... </batchRequest>
    //====================================================
    /** The &lt;batchRequest&gt; tag */
    public static final int BATCHREQUEST_START_TAG = 104;

    public static final int BATCHREQUEST_LOOP = 105;

    /** The &lt;/batchRequest&gt; tag */
    public static final int BATCHREQUEST_END_TAG = 1;

    //====================================================
    //  <abandonRequest> ... </abandonRequest>
    //====================================================
    /** The &lt;abandonRequest&gt; tag */
    public static final int ABANDON_REQUEST_START_TAG = 2;

    /** The &lt;control&gt; tag */
    public static final int ABANDON_REQUEST_CONTROL_START_TAG = 4;

    /** The &lt;/control&gt; tag */
    public static final int ABANDON_REQUEST_CONTROL_END_TAG = 5;

    /** The &lt;controlValue&gt; tag */
    public static final int ABANDON_REQUEST_CONTROLVALUE_START_TAG = 6;

    /** The &lt;/controlValue&gt; tag */
    public static final int ABANDON_REQUEST_CONTROLVALUE_END_TAG = 7;

    //====================================================
    //  <addRequest> ... </addRequest>
    //====================================================
    /** The &lt;addRequest&gt; tag */
    public static final int ADD_REQUEST_START_TAG = 8;

    /** The &lt;control&gt; tag */
    public static final int ADD_REQUEST_CONTROL_START_TAG = 10;

    /** The &lt;/control&gt; tag */
    public static final int ADD_REQUEST_CONTROL_END_TAG = 11;

    /** The &lt;controlValue&gt; tag */
    public static final int ADD_REQUEST_CONTROLVALUE_START_TAG = 12;

    /** The &lt;/controlValue&gt; tag */
    public static final int ADD_REQUEST_CONTROLVALUE_END_TAG = 13;

    /** The &lt;attr&gt; tag */
    public static final int ADD_REQUEST_ATTR_START_TAG = 14;

    /** The &lt;/attr&gt; tag */
    public static final int ADD_REQUEST_ATTR_END_TAG = 15;

    /** The &lt;value&gt; tag */
    public static final int ADD_REQUEST_VALUE_START_TAG = 16;

    /** The &lt;/value&gt; tag */
    public static final int ADD_REQUEST_VALUE_END_TAG = 17;

    //====================================================
    //  <authRequest> ... </authRequest>
    //====================================================
    /** The &lt;authRequest&gt; tag */
    public static final int AUTH_REQUEST_START_TAG = 18;

    /** The &lt;control&gt; tag */
    public static final int AUTH_REQUEST_CONTROL_START_TAG = 20;

    /** The &lt;/control&gt; tag */
    public static final int AUTH_REQUEST_CONTROL_END_TAG = 21;

    /** The &lt;controlValue&gt; tag */
    public static final int AUTH_REQUEST_CONTROLVALUE_START_TAG = 22;

    /** The &lt;/controlValue&gt; tag */
    public static final int AUTH_REQUEST_CONTROLVALUE_END_TAG = 23;

    //====================================================
    //  <compareRequest> ... </compareRequest>
    //====================================================
    /** The &lt;compareRequest&gt; tag */
    public static final int COMPARE_REQUEST_START_TAG = 24;

    /** The &lt;control&gt; tag */
    public static final int COMPARE_REQUEST_CONTROL_START_TAG = 26;

    /** The &lt;/control&gt; tag */
    public static final int COMPARE_REQUEST_CONTROL_END_TAG = 27;

    /** The &lt;controlValue&gt; tag */
    public static final int COMPARE_REQUEST_CONTROLVALUE_START_TAG = 28;

    /** The &lt;/controlValue&gt; tag */
    public static final int COMPARE_REQUEST_CONTROLVALUE_END_TAG = 29;

    /** The &lt;assertion&gt; tag */
    public static final int COMPARE_REQUEST_ASSERTION_START_TAG = 30;

    /** The &lt;/assertion&gt; tag */
    public static final int COMPARE_REQUEST_ASSERTION_END_TAG = 31;

    /** The &lt;value&gt; tag */
    public static final int COMPARE_REQUEST_VALUE_START_TAG = 32;

    /** The &lt;/value&gt; tag */
    public static final int COMPARE_REQUEST_VALUE_END_TAG = 33;

    //====================================================
    //  <delRequest> ... </delRequest>
    //====================================================
    /** The &lt;delRequest&gt; tag */
    public static final int DEL_REQUEST_START_TAG = 34;

    /** The &lt;control&gt; tag */
    public static final int DEL_REQUEST_CONTROL_START_TAG = 36;

    /** The &lt;/control&gt; tag */
    public static final int DEL_REQUEST_CONTROL_END_TAG = 37;

    /** The &lt;controlValue&gt; tag */
    public static final int DEL_REQUEST_CONTROLVALUE_START_TAG = 38;

    /** The &lt;/controlValue&gt; tag */
    public static final int DEL_REQUEST_CONTROLVALUE_END_TAG = 39;

    //====================================================
    //  <extendedRequest> ... </extendedRequest>
    //====================================================
    /** The &lt;extendedRequest&gt; tag */
    public static final int EXTENDED_REQUEST_START_TAG = 40;

    /** The &lt;control&gt; tag */
    public static final int EXTENDED_REQUEST_CONTROL_START_TAG = 42;

    /** The &lt;/control&gt; tag */
    public static final int EXTENDED_REQUEST_CONTROL_END_TAG = 43;

    /** The &lt;controlValue&gt; tag */
    public static final int EXTENDED_REQUEST_CONTROLVALUE_START_TAG = 44;

    /** The &lt;/controlValue&gt; tag */
    public static final int EXTENDED_REQUEST_CONTROLVALUE_END_TAG = 45;

    /** The &lt;requestName&gt; tag */
    public static final int EXTENDED_REQUEST_REQUESTNAME_START_TAG = 46;

    /** The &lt;/requestName&gt; tag */
    public static final int EXTENDED_REQUEST_REQUESTNAME_END_TAG = 47;

    /** The &lt;requestValue&gt; tag */
    public static final int EXTENDED_REQUEST_REQUESTVALUE_START_TAG = 48;

    /** The &lt;/requestValue&gt; tag */
    public static final int EXTENDED_REQUEST_REQUESTVALUE_END_TAG = 49;

    //====================================================
    //  <modDNRequest> ... </modDNRequest>
    //====================================================
    /** The &lt;modDNRequest&gt; tag */
    public static final int MODIFY_DN_REQUEST_START_TAG = 50;

    /** The &lt;control&gt; tag */
    public static final int MODIFY_DN_REQUEST_CONTROL_START_TAG = 52;

    /** The &lt;/control&gt; tag */
    public static final int MODIFY_DN_REQUEST_CONTROL_END_TAG = 53;

    /** The &lt;controlValue&gt; tag */
    public static final int MODIFY_DN_REQUEST_CONTROLVALUE_START_TAG = 54;

    /** The &lt;/controlValue&gt; tag */
    public static final int MODIFY_DN_REQUEST_CONTROLVALUE_END_TAG = 55;

    //====================================================
    //  <modifyRequest> ... </modifyRequest>
    //====================================================
    /** The &lt;modifyRequest&gt; tag */
    public static final int MODIFY_REQUEST_START_TAG = 56;

    /** The &lt;control&gt; tag */
    public static final int MODIFY_REQUEST_CONTROL_START_TAG = 58;

    /** The &lt;/control&gt; tag */
    public static final int MODIFY_REQUEST_CONTROL_END_TAG = 59;

    /** The &lt;controlValue&gt; tag */
    public static final int MODIFY_REQUEST_CONTROLVALUE_START_TAG = 60;

    /** The &lt;/controlValue&gt; tag */
    public static final int MODIFY_REQUEST_CONTROLVALUE_END_TAG = 61;

    /** The &lt;modification&gt; tag */
    public static final int MODIFY_REQUEST_MODIFICATION_START_TAG = 62;

    /** The &lt;/modification&gt; tag */
    public static final int MODIFY_REQUEST_MODIFICATION_END_TAG = 63;

    /** The &lt;value&gt; tag */
    public static final int MODIFY_REQUEST_VALUE_START_TAG = 64;

    /** The &lt;/value&gt; tag */
    public static final int MODIFY_REQUEST_VALUE_END_TAG = 65;

    //====================================================
    //  <searchRequest> ... </searchRequest>
    //====================================================
    /** The &lt;searchRequest&gt; tag */
    public static final int SEARCH_REQUEST_START_TAG = 66;

    /** The &lt;control&gt; tag */
    public static final int SEARCH_REQUEST_CONTROL_START_TAG = 68;

    /** The &lt;/control&gt; tag */
    public static final int SEARCH_REQUEST_CONTROL_END_TAG = 69;

    /** The &lt;controlValue&gt; tag */
    public static final int SEARCH_REQUEST_CONTROLVALUE_START_TAG = 70;

    /** The &lt;/controlValue&gt; tag */
    public static final int SEARCH_REQUEST_CONTROLVALUE_END_TAG = 71;

    /** The &lt;filter&gt; tag */
    public static final int SEARCH_REQUEST_FILTER_START_TAG = 72;

    /** The &lt;/filter&gt; tag */
    public static final int SEARCH_REQUEST_FILTER_END_TAG = 73;

    /** The &lt;attributes&gt; tag */
    public static final int SEARCH_REQUEST_ATTRIBUTES_START_TAG = 74;

    /** The &lt;/attributes&gt; tag */
    public static final int SEARCH_REQUEST_ATTRIBUTES_END_TAG = 75;

    /** The &lt;attribute&gt; tag */
    public static final int SEARCH_REQUEST_ATTRIBUTE_START_TAG = 76;

    /** The &lt;/attribute&gt; tag */
    public static final int SEARCH_REQUEST_ATTRIBUTE_END_TAG = 77;

    /** The &lt;equalityMatch&gt; tag */
    public static final int SEARCH_REQUEST_EQUALITYMATCH_START_TAG = 84;

    /** The &lt;subStrings&gt; tag */
    public static final int SEARCH_REQUEST_SUBSTRINGS_START_TAG = 86;

    /** The &lt;/subStrings&gt; tag */
    public static final int SEARCH_REQUEST_SUBSTRINGS_END_TAG = 87;

    /** The &lt;greaterOrEqual&gt; tag */
    public static final int SEARCH_REQUEST_GREATEROREQUAL_START_TAG = 88;

    /** The &lt;lessOrEqual&gt; tag */
    public static final int SEARCH_REQUEST_LESSOREQUAL_START_TAG = 90;

    /** The &lt;present&gt; tag */
    public static final int SEARCH_REQUEST_PRESENT_START_TAG = 92;

    /** The &lt;approxMatch&gt; tag */
    public static final int SEARCH_REQUEST_APPROXMATCH_START_TAG = 94;

    /** The &lt;extensibleMatch&gt; tag */
    public static final int SEARCH_REQUEST_EXTENSIBLEMATCH_START_TAG = 96;

    /** The &lt;value&gt; tag */
    public static final int SEARCH_REQUEST_EXTENSIBLEMATCH_VALUE_START_TAG = 109;

    /** The &lt;/value&gt; tag */
    public static final int SEARCH_REQUEST_EXTENSIBLEMATCH_VALUE_END_TAG = 110;

    /** The &lt;initial&gt; tag */
    public static final int SEARCH_REQUEST_INITIAL_START_TAG = 98;

    /** The &lt;/initial&gt; tag */
    public static final int SEARCH_REQUEST_INITIAL_END_TAG = 99;

    /** The &lt;any&gt; tag */
    public static final int SEARCH_REQUEST_ANY_START_TAG = 100;

    /** The &lt;/any&gt; tag */
    public static final int SEARCH_REQUEST_ANY_END_TAG = 101;

    /** The &lt;final&gt; tag */
    public static final int SEARCH_REQUEST_FINAL_START_TAG = 102;

    /** The &lt;/final&gt; tag */
    public static final int SEARCH_REQUEST_FINAL_END_TAG = 103;

    /** The &lt;value&gt; tag */
    public static final int SEARCH_REQUEST_VALUE_START_TAG = 107;

    /** The &lt;/value&gt; tag */
    public static final int SEARCH_REQUEST_VALUE_END_TAG = 108;

    /** The Filter Loop state */
    public static final int SEARCH_REQUEST_FILTER_LOOP = 106;

    //****************
    // DSML Response 
    //****************

    /** The Batch Response Loop state */
    public static final int BATCH_RESPONSE_LOOP = 200;

    /** The Error Response Loop state */
    public static final int ERROR_RESPONSE = 201;

    /** The Message Start state */
    public static final int MESSAGE_START = 202;

    /** The Message End state */
    public static final int MESSAGE_END = 203;

    /** The Detail Start state */
    public static final int DETAIL_START = 204;

    /** The Detail End state */
    public static final int DETAIL_END = 205;

    /** The Extended Response state */
    public static final int EXTENDED_RESPONSE = 206;

    /** The Extended Response Control Start state */
    public static final int EXTENDED_RESPONSE_CONTROL_START = 207;

    /** The Extended Response Control End state */
    public static final int EXTENDED_RESPONSE_CONTROL_END = 208;

    /** The Extended Response Control Value Start state */
    public static final int EXTENDED_RESPONSE_CONTROL_VALUE_START = 245;

    /** The Extended Response Control Value End state */
    public static final int EXTENDED_RESPONSE_CONTROL_VALUE_END = 246;

    /** The Extended Response Result Code Start state */
    public static final int EXTENDED_RESPONSE_RESULT_CODE_START = 209;

    /** The Extended Response Result Code End state */
    public static final int EXTENDED_RESPONSE_RESULT_CODE_END = 210;

    /** The Extended Response Error Message Start state */
    public static final int EXTENDED_RESPONSE_ERROR_MESSAGE_START = 211;

    /** The Extended Response Error Message End state */
    public static final int EXTENDED_RESPONSE_ERROR_MESSAGE_END = 212;

    /** The Extended Response Referral Start state */
    public static final int EXTENDED_RESPONSE_REFERRAL_START = 213;

    /** The Extended Response Referral End state */
    public static final int EXTENDED_RESPONSE_REFERRAL_END = 214;

    /** The Response Name Start state */
    public static final int RESPONSE_NAME_START = 215;

    /** The Response Name End state */
    public static final int RESPONSE_NAME_END = 216;

    /** The Response Start state */
    public static final int RESPONSE_START = 217;

    /** The Response End state */
    public static final int RESPONSE_END = 218;

    /** The LDAP Result state */
    public static final int LDAP_RESULT = 219;

    /** The LDAP Result Control Start state */
    public static final int LDAP_RESULT_CONTROL_START = 220;

    /** The LDAP Result Control End state */
    public static final int LDAP_RESULT_CONTROL_END = 221;

    /** The LDAP Result Control Value Start state */
    public static final int LDAP_RESULT_CONTROL_VALUE_START = 247;

    /** The LDAP Result Control Value End state */
    public static final int LDAP_RESULT_CONTROL_VALUE_END = 248;

    /** The LDAP Result Result Code Start state */
    public static final int LDAP_RESULT_RESULT_CODE_START = 222;

    /** The LDAP Result Result Code End state */
    public static final int LDAP_RESULT_RESULT_CODE_END = 223;

    /** The LDAP Result Error Message Start state */
    public static final int LDAP_RESULT_ERROR_MESSAGE_START = 224;

    /** The LDAP Result Error Message End state */
    public static final int LDAP_RESULT_ERROR_MESSAGE_END = 225;

    /** The LDAP Result Referral Start state */
    public static final int LDAP_RESULT_REFERRAL_START = 226;

    /** The LDAP Result Referral End state */
    public static final int LDAP_RESULT_REFERRAL_END = 227;

    /** The LDAP Result End state */
    public static final int LDAP_RESULT_END = 228;

    /** The Search Response state */
    public static final int SEARCH_RESPONSE = 229;

    /** The Search Result Entry state */
    public static final int SEARCH_RESULT_ENTRY = 230;

    /** The Search Result Entry Control Start state */
    public static final int SEARCH_RESULT_ENTRY_CONTROL_START = 231;

    /** The Search Result Entry Control End state */
    public static final int SEARCH_RESULT_ENTRY_CONTROL_END = 232;

    /** The Search Result Entry Control Value Start state */
    public static final int SEARCH_RESULT_ENTRY_CONTROL_VALUE_START = 249;

    /** The Search Result Entry Control Value End state */
    public static final int SEARCH_RESULT_ENTRY_CONTROL_VALUE_END = 250;

    /** The Search Result Entry Attr Start state */
    public static final int SEARCH_RESULT_ENTRY_ATTR_START = 233;

    /** The Search Result Entry Attr End state */
    public static final int SEARCH_RESULT_ENTRY_ATTR_END = 234;

    /** The Search Result Entry Value Start state */
    public static final int SEARCH_RESULT_ENTRY_VALUE_START = 235;

    /** The Search Result Entry Value End state */
    public static final int SEARCH_RESULT_ENTRY_VALUE_END = 236;

    /** The Search Result Entry Loop state */
    public static final int SEARCH_RESULT_ENTRY_LOOP = 237;

    /** The Search Result Reference state */
    public static final int SEARCH_RESULT_REFERENCE = 238;

    /** The Search Result Reference Control Start state */
    public static final int SEARCH_RESULT_REFERENCE_CONTROL_START = 239;

    /** The Search Result Reference Control End state */
    public static final int SEARCH_RESULT_REFERENCE_CONTROL_END = 240;

    /** The Search Result Reference Control Value Start state */
    public static final int SEARCH_RESULT_REFERENCE_CONTROL_VALUE_START = 251;

    /** The Search Result Reference Control Value End state */
    public static final int SEARCH_RESULT_REFERENCE_CONTROL_VALUE_END = 252;

    /** The Search Result Reference Ref Start state */
    public static final int SEARCH_RESULT_REFERENCE_REF_START = 241;

    /** The Search Result Reference Ref End state */
    public static final int SEARCH_RESULT_REFERENCE_REF_END = 242;

    /** The Search Result Reference Loop state */
    public static final int SEARCH_RESULT_REFERENCE_LOOP = 243;

    /** The Search Result Done End state */
    public static final int SEARCH_RESULT_DONE_END = 244;

    /** The instance */
    private static Dsmlv2StatesEnum instance = new Dsmlv2StatesEnum();


    private Dsmlv2StatesEnum()
    {
    }


    /**
     * Get an instance of this class
     * 
     * @return An instance on this class
     */
    public static Dsmlv2StatesEnum getInstance()
    {
        return instance;
    }


    /** Get the current state for a specified grammar */
    public String getState( int state )
    {
        switch ( state )
        {
            case BATCHREQUEST_START_TAG:
                return "BATCHREQUEST_START_TAG";
            case BATCHREQUEST_LOOP:
                return "BATCHREQUEST_LOOP";
            case BATCHREQUEST_END_TAG:
                return "BATCHREQUEST_END_TAG";
            case ABANDON_REQUEST_START_TAG:
                return "ABANDON_REQUEST_START_TAG";
            case ABANDON_REQUEST_CONTROL_START_TAG:
                return "ABANDON_REQUEST_CONTROL_START_TAG";
            case ABANDON_REQUEST_CONTROL_END_TAG:
                return "ABANDON_REQUEST_CONTROL_END_TAG";
            case ABANDON_REQUEST_CONTROLVALUE_START_TAG:
                return "ABANDON_REQUEST_CONTROLVALUE_START_TAG";
            case ABANDON_REQUEST_CONTROLVALUE_END_TAG:
                return "ABANDON_REQUEST_CONTROLVALUE_END_TAG";
            case ADD_REQUEST_START_TAG:
                return "ADD_REQUEST_START_TAG";
            case ADD_REQUEST_CONTROL_START_TAG:
                return "ADD_REQUEST_CONTROL_START_TAG";
            case ADD_REQUEST_CONTROL_END_TAG:
                return "ADD_REQUEST_CONTROL_END_TAG";
            case ADD_REQUEST_CONTROLVALUE_START_TAG:
                return "ADD_REQUEST_CONTROLVALUE_START_TAG";
            case ADD_REQUEST_CONTROLVALUE_END_TAG:
                return "ADD_REQUEST_CONTROLVALUE_END_TAG";
            case ADD_REQUEST_ATTR_START_TAG:
                return "ADD_REQUEST_ATTR_START_TAG";
            case ADD_REQUEST_ATTR_END_TAG:
                return "ADD_REQUEST_ATTR_END_TAG";
            case ADD_REQUEST_VALUE_START_TAG:
                return "ADD_REQUEST_VALUE_START_TAG";
            case ADD_REQUEST_VALUE_END_TAG:
                return "ADD_REQUEST_VALUE_END_TAG";
            case AUTH_REQUEST_START_TAG:
                return "AUTH_REQUEST_START_TAG";
            case AUTH_REQUEST_CONTROL_START_TAG:
                return "AUTH_REQUEST_CONTROL_START_TAG";
            case AUTH_REQUEST_CONTROL_END_TAG:
                return "AUTH_REQUEST_CONTROL_END_TAG";
            case AUTH_REQUEST_CONTROLVALUE_START_TAG:
                return "AUTH_REQUEST_CONTROLVALUE_START_TAG";
            case AUTH_REQUEST_CONTROLVALUE_END_TAG:
                return "AUTH_REQUEST_CONTROLVALUE_END_TAG";
            case COMPARE_REQUEST_START_TAG:
                return "COMPARE_REQUEST_START_TAG";
            case COMPARE_REQUEST_CONTROL_START_TAG:
                return "COMPARE_REQUEST_CONTROL_START_TAG";
            case COMPARE_REQUEST_CONTROL_END_TAG:
                return "COMPARE_REQUEST_CONTROL_END_TAG";
            case COMPARE_REQUEST_CONTROLVALUE_START_TAG:
                return "COMPARE_REQUEST_CONTROLVALUE_START_TAG";
            case COMPARE_REQUEST_CONTROLVALUE_END_TAG:
                return "COMPARE_REQUEST_CONTROLVALUE_END_TAG";
            case COMPARE_REQUEST_ASSERTION_START_TAG:
                return "COMPARE_REQUEST_ASSERTION_START_TAG";
            case COMPARE_REQUEST_ASSERTION_END_TAG:
                return "COMPARE_REQUEST_ASSERTION_END_TAG";
            case COMPARE_REQUEST_VALUE_START_TAG:
                return "COMPARE_REQUEST_VALUE_START_TAG";
            case COMPARE_REQUEST_VALUE_END_TAG:
                return "COMPARE_REQUEST_VALUE_END_TAG";
            case DEL_REQUEST_START_TAG:
                return "DEL_REQUEST_START_TAG";
            case DEL_REQUEST_CONTROL_START_TAG:
                return "DEL_REQUEST_CONTROL_START_TAG";
            case DEL_REQUEST_CONTROL_END_TAG:
                return "DEL_REQUEST_CONTROL_END_TAG";
            case DEL_REQUEST_CONTROLVALUE_START_TAG:
                return "DEL_REQUEST_CONTROLVALUE_START_TAG";
            case DEL_REQUEST_CONTROLVALUE_END_TAG:
                return "DEL_REQUEST_CONTROLVALUE_END_TAG";
            case EXTENDED_REQUEST_START_TAG:
                return "EXTENDED_REQUEST_START_TAG";
            case EXTENDED_REQUEST_CONTROL_START_TAG:
                return "EXTENDED_REQUEST_CONTROL_START_TAG";
            case EXTENDED_REQUEST_CONTROL_END_TAG:
                return "EXTENDED_REQUEST_CONTROL_END_TAG";
            case EXTENDED_REQUEST_CONTROLVALUE_START_TAG:
                return "EXTENDED_REQUEST_CONTROLVALUE_START_TAG";
            case EXTENDED_REQUEST_CONTROLVALUE_END_TAG:
                return "EXTENDED_REQUEST_CONTROLVALUE_END_TAG";
            case EXTENDED_REQUEST_REQUESTNAME_START_TAG:
                return "EXTENDED_REQUEST_REQUESTNAME_START_TAG";
            case EXTENDED_REQUEST_REQUESTNAME_END_TAG:
                return "EXTENDED_REQUEST_REQUESTNAME_END_TAG";
            case EXTENDED_REQUEST_REQUESTVALUE_START_TAG:
                return "EXTENDED_REQUEST_REQUESTVALUE_START_TAG";
            case EXTENDED_REQUEST_REQUESTVALUE_END_TAG:
                return "EXTENDED_REQUEST_REQUESTVALUE_END_TAG";
            case MODIFY_DN_REQUEST_START_TAG:
                return "MODIFY_DN_REQUEST_START_TAG";
            case MODIFY_DN_REQUEST_CONTROL_START_TAG:
                return "MODIFY_DN_REQUEST_CONTROL_START_TAG";
            case MODIFY_DN_REQUEST_CONTROL_END_TAG:
                return "MODIFY_DN_REQUEST_CONTROL_END_TAG";
            case MODIFY_DN_REQUEST_CONTROLVALUE_START_TAG:
                return "MODIFY_DN_REQUEST_CONTROLVALUE_START_TAG";
            case MODIFY_DN_REQUEST_CONTROLVALUE_END_TAG:
                return "MODIFY_DN_REQUEST_CONTROLVALUE_END_TAG";
            case MODIFY_REQUEST_START_TAG:
                return "MODIFY_REQUEST_START_TAG";
            case MODIFY_REQUEST_CONTROL_START_TAG:
                return "MODIFY_REQUEST_CONTROL_START_TAG";
            case MODIFY_REQUEST_CONTROL_END_TAG:
                return "MODIFY_REQUEST_CONTROL_END_TAG";
            case MODIFY_REQUEST_CONTROLVALUE_START_TAG:
                return "MODIFY_REQUEST_CONTROLVALUE_START_TAG";
            case MODIFY_REQUEST_CONTROLVALUE_END_TAG:
                return "MODIFY_REQUEST_CONTROLVALUE_END_TAG";
            case MODIFY_REQUEST_MODIFICATION_START_TAG:
                return "MODIFY_REQUEST_MODIFICATION_START_TAG";
            case MODIFY_REQUEST_MODIFICATION_END_TAG:
                return "MODIFY_REQUEST_MODIFICATION_END_TAG";
            case MODIFY_REQUEST_VALUE_START_TAG:
                return "MODIFY_REQUEST_VALUE_START_TAG";
            case MODIFY_REQUEST_VALUE_END_TAG:
                return "MODIFY_REQUEST_VALUE_END_TAG";
            case SEARCH_REQUEST_START_TAG:
                return "SEARCH_REQUEST_START_TAG";
            case SEARCH_REQUEST_CONTROL_START_TAG:
                return "SEARCH_REQUEST_CONTROL_START_TAG";
            case SEARCH_REQUEST_CONTROL_END_TAG:
                return "SEARCH_REQUEST_CONTROL_END_TAG";
            case SEARCH_REQUEST_CONTROLVALUE_START_TAG:
                return "SEARCH_REQUEST_CONTROLVALUE_START_TAG";
            case SEARCH_REQUEST_CONTROLVALUE_END_TAG:
                return "SEARCH_REQUEST_CONTROLVALUE_END_TAG";
            case SEARCH_REQUEST_FILTER_START_TAG:
                return "SEARCH_REQUEST_FILTER_START_TAG";
            case SEARCH_REQUEST_FILTER_END_TAG:
                return "SEARCH_REQUEST_FILTER_END_TAG";
            case SEARCH_REQUEST_ATTRIBUTES_START_TAG:
                return "SEARCH_REQUEST_ATTRIBUTES_START_TAG";
            case SEARCH_REQUEST_ATTRIBUTES_END_TAG:
                return "SEARCH_REQUEST_ATTRIBUTES_END_TAG";
            case SEARCH_REQUEST_ATTRIBUTE_START_TAG:
                return "SEARCH_REQUEST_ATTRIBUTE_START_TAG";
            case SEARCH_REQUEST_ATTRIBUTE_END_TAG:
                return "SEARCH_REQUEST_ATTRIBUTE_END_TAG";
            case SEARCH_REQUEST_EQUALITYMATCH_START_TAG:
                return "SEARCH_REQUEST_EQUALITYMATCH_START_TAG";
            case SEARCH_REQUEST_SUBSTRINGS_START_TAG:
                return "SEARCH_REQUEST_SUBSTRINGS_START_TAG";
            case SEARCH_REQUEST_SUBSTRINGS_END_TAG:
                return "SEARCH_REQUEST_SUBSTRINGS_END_TAG";
            case SEARCH_REQUEST_GREATEROREQUAL_START_TAG:
                return "SEARCH_REQUEST_GREATEROREQUAL_START_TAG";
            case SEARCH_REQUEST_LESSOREQUAL_START_TAG:
                return "SEARCH_REQUEST_LESSOREQUAL_START_TAG";
            case SEARCH_REQUEST_PRESENT_START_TAG:
                return "SEARCH_REQUEST_PRESENT_START_TAG";
            case SEARCH_REQUEST_APPROXMATCH_START_TAG:
                return "SEARCH_REQUEST_APPROXMATCH_START_TAG";
            case SEARCH_REQUEST_EXTENSIBLEMATCH_START_TAG:
                return "SEARCH_REQUEST_EXTENSIBLEMATCH_START_TAG";
            case SEARCH_REQUEST_EXTENSIBLEMATCH_VALUE_START_TAG:
                return "SEARCH_REQUEST_EXTENSIBLEMATCH_VALUE_START_TAG";
            case SEARCH_REQUEST_EXTENSIBLEMATCH_VALUE_END_TAG:
                return "SEARCH_REQUEST_EXTENSIBLEMATCH_VALUE_END_TAG";
            case SEARCH_REQUEST_INITIAL_START_TAG:
                return "SEARCH_REQUEST_INITIAL_START_TAG";
            case SEARCH_REQUEST_INITIAL_END_TAG:
                return "SEARCH_REQUEST_INITIAL_END_TAG";
            case SEARCH_REQUEST_ANY_START_TAG:
                return "SEARCH_REQUEST_ANY_START_TAG";
            case SEARCH_REQUEST_ANY_END_TAG:
                return "SEARCH_REQUEST_ANY_END_TAG";
            case SEARCH_REQUEST_FINAL_START_TAG:
                return "SEARCH_REQUEST_FINAL_START_TAG";
            case SEARCH_REQUEST_FINAL_END_TAG:
                return "SEARCH_REQUEST_FINAL_END_TAG";
            case SEARCH_REQUEST_VALUE_START_TAG:
                return "SEARCH_REQUEST_VALUE_START_TAG";
            case SEARCH_REQUEST_VALUE_END_TAG:
                return "SEARCH_REQUEST_VALUE_END_TAG";
            case SEARCH_REQUEST_FILTER_LOOP:
                return "SEARCH_REQUEST_FILTER_LOOP";

            case BATCH_RESPONSE_LOOP:
                return "BATCH_RESPONSE_LOOP";
            case ERROR_RESPONSE:
                return "ERROR_RESPONSE";
            case MESSAGE_START:
                return "MESSAGE_START";
            case MESSAGE_END:
                return "MESSAGE_END";
            case DETAIL_START:
                return "DETAIL_START";
            case DETAIL_END:
                return "DETAIL_END";
            case EXTENDED_RESPONSE:
                return "EXTENDED_RESPONSE";
            case EXTENDED_RESPONSE_CONTROL_START:
                return "EXTENDED_RESPONSE_CONTROL_START";
            case EXTENDED_RESPONSE_CONTROL_END:
                return "EXTENDED_RESPONSE_CONTROL_END";
            case EXTENDED_RESPONSE_CONTROL_VALUE_START:
                return "EXTENDED_RESPONSE_CONTROL_VALUE_START";
            case EXTENDED_RESPONSE_CONTROL_VALUE_END:
                return "EXTENDED_RESPONSE_CONTROL_VALUE_END";
            case EXTENDED_RESPONSE_RESULT_CODE_START:
                return "EXTENDED_RESPONSE_RESULT_CODE_START";
            case EXTENDED_RESPONSE_RESULT_CODE_END:
                return "EXTENDED_RESPONSE_RESULT_CODE_END";
            case EXTENDED_RESPONSE_ERROR_MESSAGE_START:
                return "EXTENDED_RESPONSE_ERROR_MESSAGE_START";
            case EXTENDED_RESPONSE_ERROR_MESSAGE_END:
                return "EXTENDED_RESPONSE_ERROR_MESSAGE_END";
            case EXTENDED_RESPONSE_REFERRAL_START:
                return "EXTENDED_RESPONSE_REFERRAL_START";
            case EXTENDED_RESPONSE_REFERRAL_END:
                return "EXTENDED_RESPONSE_REFERRAL_END";
            case RESPONSE_NAME_START:
                return "RESPONSE_NAME_START";
            case RESPONSE_NAME_END:
                return "RESPONSE_NAME_END";
            case RESPONSE_START:
                return "RESPONSE_START";
            case RESPONSE_END:
                return "RESPONSE_END";
            case LDAP_RESULT:
                return "LDAP_RESULT";
            case LDAP_RESULT_CONTROL_START:
                return "LDAP_RESULT_CONTROL_START";
            case LDAP_RESULT_CONTROL_END:
                return "LDAP_RESULT_CONTROL_END";
            case LDAP_RESULT_CONTROL_VALUE_START:
                return "LDAP_RESULT_CONTROL_VALUE_START";
            case LDAP_RESULT_CONTROL_VALUE_END:
                return "LDAP_RESULT_CONTROL_VALUE_END";
            case LDAP_RESULT_RESULT_CODE_START:
                return "LDAP_RESULT_RESULT_CODE_START";
            case LDAP_RESULT_RESULT_CODE_END:
                return "LDAP_RESULT_RESULT_CODE_END";
            case LDAP_RESULT_ERROR_MESSAGE_START:
                return "LDAP_RESULT_ERROR_MESSAGE_START";
            case LDAP_RESULT_ERROR_MESSAGE_END:
                return "LDAP_RESULT_ERROR_MESSAGE_END";
            case LDAP_RESULT_REFERRAL_START:
                return "LDAP_RESULT_REFERRAL_START";
            case LDAP_RESULT_REFERRAL_END:
                return "LDAP_RESULT_REFERRAL_END";
            case LDAP_RESULT_END:
                return "LDAP_RESULT_END";
            case SEARCH_RESPONSE:
                return "SEARCH_RESPONSE";
            case SEARCH_RESULT_ENTRY:
                return "SEARCH_RESULT_ENTRY";
            case SEARCH_RESULT_ENTRY_CONTROL_START:
                return "SEARCH_RESULT_ENTRY_CONTROL_START";
            case SEARCH_RESULT_ENTRY_CONTROL_END:
                return "SEARCH_RESULT_ENTRY_CONTROL_END";
            case SEARCH_RESULT_ENTRY_CONTROL_VALUE_START:
                return "SEARCH_RESULT_ENTRY_CONTROL_VALUE_START";
            case SEARCH_RESULT_ENTRY_CONTROL_VALUE_END:
                return "SEARCH_RESULT_ENTRY_CONTROL_VALUE_END";
            case SEARCH_RESULT_ENTRY_ATTR_START:
                return "SEARCH_RESULT_ENTRY_ATTR_START";
            case SEARCH_RESULT_ENTRY_ATTR_END:
                return "SEARCH_RESULT_ENTRY_ATTR_END";
            case SEARCH_RESULT_ENTRY_VALUE_START:
                return "SEARCH_RESULT_ENTRY_VALUE_START";
            case SEARCH_RESULT_ENTRY_VALUE_END:
                return "SEARCH_RESULT_ENTRY_VALUE_END";
            case SEARCH_RESULT_ENTRY_LOOP:
                return "SEARCH_RESULT_ENTRY_LOOP";
            case SEARCH_RESULT_REFERENCE:
                return "SEARCH_RESULT_REFERENCE";
            case SEARCH_RESULT_REFERENCE_CONTROL_START:
                return "SEARCH_RESULT_REFERENCE_CONTROL_START";
            case SEARCH_RESULT_REFERENCE_CONTROL_END:
                return "SEARCH_RESULT_REFERENCE_CONTROL_END";
            case SEARCH_RESULT_REFERENCE_CONTROL_VALUE_START:
                return "SEARCH_RESULT_REFERENCE_CONTROL_VALUE_START";
            case SEARCH_RESULT_REFERENCE_CONTROL_VALUE_END:
                return "SEARCH_RESULT_REFERENCE_CONTROL_VALUE_END";
            case SEARCH_RESULT_REFERENCE_REF_START:
                return "SEARCH_RESULT_REFERENCE_REF_START";
            case SEARCH_RESULT_REFERENCE_REF_END:
                return "SEARCH_RESULT_REFERENCE_REF_END";
            case SEARCH_RESULT_REFERENCE_LOOP:
                return "SEARCH_RESULT_REFERENCE_LOOP";
            case SEARCH_RESULT_DONE_END:
                return "SEARCH_RESULT_DONE_END";

            default:
                return "UNKNOWN";
        }
    }
}
