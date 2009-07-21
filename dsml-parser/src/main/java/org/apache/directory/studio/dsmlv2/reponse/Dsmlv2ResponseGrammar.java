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


import java.io.IOException;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.naming.InvalidNameException;

import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.ldap.codec.ControlCodec;
import org.apache.directory.shared.ldap.codec.LdapMessageCodec;
import org.apache.directory.shared.ldap.codec.LdapResponseCodec;
import org.apache.directory.shared.ldap.codec.LdapResultCodec;
import org.apache.directory.shared.ldap.codec.add.AddResponseCodec;
import org.apache.directory.shared.ldap.codec.bind.BindResponseCodec;
import org.apache.directory.shared.ldap.codec.compare.CompareResponseCodec;
import org.apache.directory.shared.ldap.codec.del.DelResponseCodec;
import org.apache.directory.shared.ldap.codec.extended.ExtendedResponseCodec;
import org.apache.directory.shared.ldap.codec.modify.ModifyResponseCodec;
import org.apache.directory.shared.ldap.codec.modifyDn.ModifyDNResponseCodec;
import org.apache.directory.shared.ldap.codec.search.SearchResultDoneCodec;
import org.apache.directory.shared.ldap.codec.search.SearchResultEntryCodec;
import org.apache.directory.shared.ldap.codec.search.SearchResultReferenceCodec;
import org.apache.directory.shared.ldap.util.LdapURL;
import org.apache.directory.shared.ldap.codec.util.LdapURLEncodingException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.Base64;
import org.apache.directory.studio.dsmlv2.AbstractGrammar;
import org.apache.directory.studio.dsmlv2.Dsmlv2Container;
import org.apache.directory.studio.dsmlv2.Dsmlv2StatesEnum;
import org.apache.directory.studio.dsmlv2.GrammarAction;
import org.apache.directory.studio.dsmlv2.GrammarTransition;
import org.apache.directory.studio.dsmlv2.IGrammar;
import org.apache.directory.studio.dsmlv2.ParserUtils;
import org.apache.directory.studio.dsmlv2.Tag;
import org.apache.directory.studio.dsmlv2.reponse.ErrorResponse.ErrorResponseType;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;


/**
 * This Class represents the DSMLv2 Response Grammar
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class Dsmlv2ResponseGrammar extends AbstractGrammar implements IGrammar
{
    /** The instance of grammar. Dsmlv2ResponseGrammar is a singleton */
    private static Dsmlv2ResponseGrammar instance = new Dsmlv2ResponseGrammar();

    // Initializing DESCR_TAGS
    private static Set<String> DSMLV2_DESCR_TAGS = null;
    static
    {
        DSMLV2_DESCR_TAGS = new HashSet<String>();
        DSMLV2_DESCR_TAGS.add( "success" );
        DSMLV2_DESCR_TAGS.add( "operationsError" );
        DSMLV2_DESCR_TAGS.add( "protocolError" );
        DSMLV2_DESCR_TAGS.add( "timeLimitExceeded" );
        DSMLV2_DESCR_TAGS.add( "sizeLimitExceeded" );
        DSMLV2_DESCR_TAGS.add( "compareFalse" );
        DSMLV2_DESCR_TAGS.add( "compareTrue" );
        DSMLV2_DESCR_TAGS.add( "authMethodNotSupported" );
        DSMLV2_DESCR_TAGS.add( "strongAuthRequired" );
        DSMLV2_DESCR_TAGS.add( "referral" );
        DSMLV2_DESCR_TAGS.add( "adminLimitExceeded" );
        DSMLV2_DESCR_TAGS.add( "unavailableCriticalExtension" );
        DSMLV2_DESCR_TAGS.add( "confidentialityRequired" );
        DSMLV2_DESCR_TAGS.add( "saslBindInProgress" );
        DSMLV2_DESCR_TAGS.add( "noSuchAttribute" );
        DSMLV2_DESCR_TAGS.add( "undefinedAttributeType" );
        DSMLV2_DESCR_TAGS.add( "inappropriateMatching" );
        DSMLV2_DESCR_TAGS.add( "constraintViolation" );
        DSMLV2_DESCR_TAGS.add( "attributeOrValueExists" );
        DSMLV2_DESCR_TAGS.add( "invalidAttributeSyntax" );
        DSMLV2_DESCR_TAGS.add( "noSuchObject" );
        DSMLV2_DESCR_TAGS.add( "aliasProblem" );
        DSMLV2_DESCR_TAGS.add( "invalidDNSyntax" );
        DSMLV2_DESCR_TAGS.add( "aliasDereferencingProblem" );
        DSMLV2_DESCR_TAGS.add( "inappropriateAuthentication" );
        DSMLV2_DESCR_TAGS.add( "invalidCredentials" );
        DSMLV2_DESCR_TAGS.add( "insufficientAccessRights" );
        DSMLV2_DESCR_TAGS.add( "busy" );
        DSMLV2_DESCR_TAGS.add( "unavailable" );
        DSMLV2_DESCR_TAGS.add( "unwillingToPerform" );
        DSMLV2_DESCR_TAGS.add( "loopDetect" );
        DSMLV2_DESCR_TAGS.add( "namingViolation" );
        DSMLV2_DESCR_TAGS.add( "objectClassViolation" );
        DSMLV2_DESCR_TAGS.add( "notAllowedOnNonLeaf" );
        DSMLV2_DESCR_TAGS.add( "notAllowedOnRDN" );
        DSMLV2_DESCR_TAGS.add( "entryAlreadyExists" );
        DSMLV2_DESCR_TAGS.add( "objectClassModsProhibited" );
        DSMLV2_DESCR_TAGS.add( "affectMultipleDSAs" );
        DSMLV2_DESCR_TAGS.add( "other" );
    }


    @SuppressWarnings("unchecked")
    private Dsmlv2ResponseGrammar()
    {
        name = Dsmlv2ResponseGrammar.class.getName();
        statesEnum = Dsmlv2StatesEnum.getInstance();

        // Create the transitions table
        super.transitions = ( HashMap<Tag, GrammarTransition>[] ) Array.newInstance( HashMap.class, 300 );; // TODO Change this value

        //====================================================
        //  Transitions concerning : BATCH RESPONSE
        //====================================================
        super.transitions[Dsmlv2StatesEnum.INIT_GRAMMAR_STATE] = new HashMap<Tag, GrammarTransition>();

        // ** OPEN BATCH Reponse **
        // State: [INIT_GRAMMAR_STATE] - Tag: <batchResponse>
        super.transitions[Dsmlv2StatesEnum.INIT_GRAMMAR_STATE].put( new Tag( "batchResponse", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.INIT_GRAMMAR_STATE, Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP,
                batchResponseCreation ) );

        //====================================================
        //  Transitions concerning : BATCH RESPONSE LOOP
        //====================================================
        super.transitions[Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP] = new HashMap<Tag, GrammarTransition>();

        // State: [BATCH_RESPONSE_LOOP] - Tag: <addResponse>
        super.transitions[Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP].put( new Tag( "addResponse", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP, Dsmlv2StatesEnum.LDAP_RESULT,
                addResponseCreation ) );

        // State: [BATCH_RESPONSE_LOOP] - Tag: <authResponse>
        super.transitions[Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP].put( new Tag( "authResponse", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP, Dsmlv2StatesEnum.LDAP_RESULT,
                authResponseCreation ) );

        // State: [BATCH_RESPONSE_LOOP] - Tag: <compareResponse>
        super.transitions[Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP].put( new Tag( "compareResponse", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP, Dsmlv2StatesEnum.LDAP_RESULT,
                compareResponseCreation ) );

        // State: [BATCH_RESPONSE_LOOP] - Tag: <delResponse>
        super.transitions[Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP].put( new Tag( "delResponse", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP, Dsmlv2StatesEnum.LDAP_RESULT,
                delResponseCreation ) );

        // State: [BATCH_RESPONSE_LOOP] - Tag: <modifyResponse>
        super.transitions[Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP].put( new Tag( "modifyResponse", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP, Dsmlv2StatesEnum.LDAP_RESULT,
                modifyResponseCreation ) );

        // State: [BATCH_RESPONSE_LOOP] - Tag: <modDNResponse>
        super.transitions[Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP].put( new Tag( "modDNResponse", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP, Dsmlv2StatesEnum.LDAP_RESULT,
                modDNResponseCreation ) );

        // State: [BATCH_RESPONSE_LOOP] - Tag: <extendedResponse>
        super.transitions[Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP].put( new Tag( "extendedResponse", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP, Dsmlv2StatesEnum.EXTENDED_RESPONSE,
                extendedResponseCreation ) );

        // State: [BATCH_RESPONSE_LOOP] - Tag: <errorResponse>
        super.transitions[Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP].put( new Tag( "errorResponse", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP, Dsmlv2StatesEnum.ERROR_RESPONSE,
                errorResponseCreation ) );

        // State: [BATCH_RESPONSE_LOOP] - Tag: <searchReponse>
        super.transitions[Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP].put( new Tag( "searchResponse", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP, Dsmlv2StatesEnum.SEARCH_RESPONSE,
                searchResponseCreation ) );

        // State: [BATCH_RESPONSE_LOOP] - Tag: </batchResponse>
        super.transitions[Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP].put( new Tag( "batchResponse", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP, Dsmlv2StatesEnum.GRAMMAR_END, null ) );

        //====================================================
        //  Transitions concerning : ERROR RESPONSE
        //====================================================
        super.transitions[Dsmlv2StatesEnum.ERROR_RESPONSE] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.MESSAGE_END] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.DETAIL_START] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.DETAIL_END] = new HashMap<Tag, GrammarTransition>();

        // State: [ERROR_RESPONSE] - Tag: <message>
        super.transitions[Dsmlv2StatesEnum.ERROR_RESPONSE].put( new Tag( "message", Tag.START ), new GrammarTransition(
            Dsmlv2StatesEnum.ERROR_RESPONSE, Dsmlv2StatesEnum.MESSAGE_END, errorResponseAddMessage ) );

        // State: [ERROR_RESPONSE] - Tag: <detail>
        super.transitions[Dsmlv2StatesEnum.ERROR_RESPONSE].put( new Tag( "detail", Tag.START ), new GrammarTransition(
            Dsmlv2StatesEnum.ERROR_RESPONSE, Dsmlv2StatesEnum.DETAIL_START, errorResponseAddDetail ) );

        // State: [MESSAGE_END] - Tag: </errorResponse>
        super.transitions[Dsmlv2StatesEnum.MESSAGE_END].put( new Tag( "errorResponse", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.MESSAGE_END, Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP, null ) );

        // State: [MESSAGE_END] - Tag: <detail>
        super.transitions[Dsmlv2StatesEnum.MESSAGE_END].put( new Tag( "detail", Tag.START ), new GrammarTransition(
            Dsmlv2StatesEnum.MESSAGE_END, Dsmlv2StatesEnum.DETAIL_START, errorResponseAddDetail ) );

        // State: [DETAIL_START] - Tag: </detail>
        super.transitions[Dsmlv2StatesEnum.DETAIL_START].put( new Tag( "detail", Tag.END ), new GrammarTransition(
            Dsmlv2StatesEnum.DETAIL_START, Dsmlv2StatesEnum.DETAIL_END, null ) );

        // State: [DETAIL_END] - Tag: <detail>
        super.transitions[Dsmlv2StatesEnum.DETAIL_END].put( new Tag( "detail", Tag.END ), new GrammarTransition(
            Dsmlv2StatesEnum.DETAIL_END, Dsmlv2StatesEnum.DETAIL_END, errorResponseAddDetail ) );

        // State: [ERROR_RESPONSE] - Tag: </errorResponse>
        super.transitions[Dsmlv2StatesEnum.ERROR_RESPONSE].put( new Tag( "errorResponse", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.ERROR_RESPONSE, Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP, null ) );

        //====================================================
        //  Transitions concerning : EXTENDED RESPONSE
        //====================================================
        super.transitions[Dsmlv2StatesEnum.EXTENDED_RESPONSE] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.EXTENDED_RESPONSE_CONTROL_START] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.EXTENDED_RESPONSE_CONTROL_END] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.EXTENDED_RESPONSE_CONTROL_VALUE_END] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.EXTENDED_RESPONSE_RESULT_CODE_START] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.EXTENDED_RESPONSE_RESULT_CODE_END] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.EXTENDED_RESPONSE_ERROR_MESSAGE_END] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.EXTENDED_RESPONSE_REFERRAL_END] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.RESPONSE_NAME_END] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.RESPONSE_END] = new HashMap<Tag, GrammarTransition>();

        // State: [EXTENDED_RESPONSE] - Tag: <control>
        super.transitions[Dsmlv2StatesEnum.EXTENDED_RESPONSE].put( new Tag( "control", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.EXTENDED_RESPONSE,
                Dsmlv2StatesEnum.EXTENDED_RESPONSE_CONTROL_START, ldapResultControlCreation ) );

        // State: [EXTENDED_RESPONSE_CONTROL_START] - Tag: <controlValue>
        super.transitions[Dsmlv2StatesEnum.EXTENDED_RESPONSE_CONTROL_START].put( new Tag( "controlValue", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.EXTENDED_RESPONSE_CONTROL_START,
                Dsmlv2StatesEnum.EXTENDED_RESPONSE_CONTROL_VALUE_END, ldapResultControlValueCreation ) );

        // State: [EXTENDED_RESPONSE_CONTROL_VALUE_END] - Tag: </control>
        super.transitions[Dsmlv2StatesEnum.EXTENDED_RESPONSE_CONTROL_VALUE_END].put( new Tag( "control", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.EXTENDED_RESPONSE_CONTROL_VALUE_END,
                Dsmlv2StatesEnum.EXTENDED_RESPONSE_CONTROL_END, null ) );

        // State: [EXTENDED_RESPONSE_CONTROL_START] - Tag: </control>
        super.transitions[Dsmlv2StatesEnum.EXTENDED_RESPONSE_CONTROL_START].put( new Tag( "control", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.EXTENDED_RESPONSE_CONTROL_START,
                Dsmlv2StatesEnum.EXTENDED_RESPONSE_CONTROL_END, null ) );

        // State: [EXTENDED_RESPONSE_CONTROL_END] - Tag: <control>
        super.transitions[Dsmlv2StatesEnum.EXTENDED_RESPONSE_CONTROL_END].put( new Tag( "control", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.EXTENDED_RESPONSE_CONTROL_END,
                Dsmlv2StatesEnum.EXTENDED_RESPONSE_CONTROL_START, ldapResultControlCreation ) );

        // State: [EXTENDED_RESPONSE_CONTROL_END] - Tag: <resultCode>
        super.transitions[Dsmlv2StatesEnum.EXTENDED_RESPONSE_CONTROL_END].put( new Tag( "resultCode", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.EXTENDED_RESPONSE_CONTROL_END,
                Dsmlv2StatesEnum.EXTENDED_RESPONSE_RESULT_CODE_START, extendedResponseAddResultCode ) );

        // State: [EXTENDED_RESPONSE] - Tag: <resultCode>
        super.transitions[Dsmlv2StatesEnum.EXTENDED_RESPONSE].put( new Tag( "resultCode", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.EXTENDED_RESPONSE,
                Dsmlv2StatesEnum.EXTENDED_RESPONSE_RESULT_CODE_START, extendedResponseAddResultCode ) );

        // State: [EXTENDED_RESPONSE_RESULT_CODE_START] - Tag: </resultCode>
        super.transitions[Dsmlv2StatesEnum.EXTENDED_RESPONSE_RESULT_CODE_START].put( new Tag( "resultCode", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.EXTENDED_RESPONSE_RESULT_CODE_START,
                Dsmlv2StatesEnum.EXTENDED_RESPONSE_RESULT_CODE_END, null ) );

        // State: [EXTENDED_RESPONSE_RESULT_CODE_END] - Tag: <errorMessage>
        super.transitions[Dsmlv2StatesEnum.EXTENDED_RESPONSE_RESULT_CODE_END].put(
            new Tag( "errorMessage", Tag.START ), new GrammarTransition(
                Dsmlv2StatesEnum.EXTENDED_RESPONSE_RESULT_CODE_END,
                Dsmlv2StatesEnum.EXTENDED_RESPONSE_ERROR_MESSAGE_END, extendedResponseAddErrorMessage ) );

        // State: [EXTENDED_RESPONSE_RESULT_CODE_END] - Tag: <referral>
        super.transitions[Dsmlv2StatesEnum.EXTENDED_RESPONSE_RESULT_CODE_END].put( new Tag( "referral", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.EXTENDED_RESPONSE_RESULT_CODE_END,
                Dsmlv2StatesEnum.EXTENDED_RESPONSE_REFERRAL_END, extendedResponseAddReferral ) );

        // State: [EXTENDED_RESPONSE_RESULT_CODE_END] - Tag: <responseName>
        super.transitions[Dsmlv2StatesEnum.EXTENDED_RESPONSE_RESULT_CODE_END].put(
            new Tag( "responseName", Tag.START ), new GrammarTransition(
                Dsmlv2StatesEnum.EXTENDED_RESPONSE_RESULT_CODE_END, Dsmlv2StatesEnum.RESPONSE_NAME_END,
                extendedResponseAddResponseName ) );

        // State: [EXTENDED_RESPONSE_RESULT_CODE_END] - Tag: <response>
        super.transitions[Dsmlv2StatesEnum.EXTENDED_RESPONSE_RESULT_CODE_END].put( new Tag( "response", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.EXTENDED_RESPONSE_RESULT_CODE_END, Dsmlv2StatesEnum.RESPONSE_END,
                extendedResponseAddResponse ) );

        // State: [EXTENDED_RESPONSE_RESULT_CODE_END] - Tag: </extendedResponse>
        super.transitions[Dsmlv2StatesEnum.EXTENDED_RESPONSE_RESULT_CODE_END].put(
            new Tag( "extendedResponse", Tag.END ), new GrammarTransition(
                Dsmlv2StatesEnum.EXTENDED_RESPONSE_RESULT_CODE_END, Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP, null ) );

        // State: [EXTENDED_RESPONSE_ERROR_MESSAGE_END] - Tag: <referral>
        super.transitions[Dsmlv2StatesEnum.EXTENDED_RESPONSE_ERROR_MESSAGE_END].put( new Tag( "referral", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.EXTENDED_RESPONSE_ERROR_MESSAGE_END,
                Dsmlv2StatesEnum.EXTENDED_RESPONSE_REFERRAL_END, extendedResponseAddReferral ) );

        // State: [EXTENDED_RESPONSE_ERROR_MESSAGE_END] - Tag: <responseName>
        super.transitions[Dsmlv2StatesEnum.EXTENDED_RESPONSE_ERROR_MESSAGE_END].put(
            new Tag( "responseName", Tag.START ), new GrammarTransition(
                Dsmlv2StatesEnum.EXTENDED_RESPONSE_ERROR_MESSAGE_END, Dsmlv2StatesEnum.RESPONSE_NAME_END,
                extendedResponseAddResponseName ) );

        // State: [EXTENDED_RESPONSE_ERROR_MESSAGE_END] - Tag: <response>
        super.transitions[Dsmlv2StatesEnum.EXTENDED_RESPONSE_ERROR_MESSAGE_END].put( new Tag( "response", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.EXTENDED_RESPONSE_ERROR_MESSAGE_END,
                Dsmlv2StatesEnum.RESPONSE_END, extendedResponseAddResponse ) );

        // State: [EXTENDED_RESPONSE_ERROR_MESSAGE_END] - Tag: </extendedResponse>
        super.transitions[Dsmlv2StatesEnum.EXTENDED_RESPONSE_ERROR_MESSAGE_END].put( new Tag( "extendedResponse",
            Tag.END ), new GrammarTransition( Dsmlv2StatesEnum.EXTENDED_RESPONSE_ERROR_MESSAGE_END,
            Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP, null ) );

        // State: [EXTENDED_RESPONSE_REFERRAL_END] - Tag: <referral>
        super.transitions[Dsmlv2StatesEnum.EXTENDED_RESPONSE_REFERRAL_END].put( new Tag( "referral", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.EXTENDED_RESPONSE_REFERRAL_END,
                Dsmlv2StatesEnum.EXTENDED_RESPONSE_REFERRAL_END, extendedResponseAddReferral ) );

        // State: [EXTENDED_RESPONSE_REFERRAL_END] - Tag: <responseName>
        super.transitions[Dsmlv2StatesEnum.EXTENDED_RESPONSE_REFERRAL_END].put( new Tag( "responseName", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.EXTENDED_RESPONSE_REFERRAL_END,
                Dsmlv2StatesEnum.RESPONSE_NAME_END, extendedResponseAddResponseName ) );

        // State: [EXTENDED_RESPONSE_REFERRAL_END] - Tag: <reponse>
        super.transitions[Dsmlv2StatesEnum.EXTENDED_RESPONSE_REFERRAL_END].put( new Tag( "reponse", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.EXTENDED_RESPONSE_REFERRAL_END, Dsmlv2StatesEnum.RESPONSE_END,
                extendedResponseAddResponse ) );

        // State: [EXTENDED_RESPONSE_REFERRAL_END] - Tag: </extendedResponse>
        super.transitions[Dsmlv2StatesEnum.EXTENDED_RESPONSE_REFERRAL_END].put( new Tag( "extendedResponse", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.EXTENDED_RESPONSE_REFERRAL_END,
                Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP, null ) );

        // State: [RESPONSE_NAME_END] - Tag: <response>
        super.transitions[Dsmlv2StatesEnum.RESPONSE_NAME_END].put( new Tag( "response", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.RESPONSE_NAME_END, Dsmlv2StatesEnum.RESPONSE_END,
                extendedResponseAddResponse ) );

        // State: [RESPONSE_NAME_END] - Tag: </extendedResponse>
        super.transitions[Dsmlv2StatesEnum.RESPONSE_NAME_END].put( new Tag( "extendedResponse", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.RESPONSE_NAME_END, Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP, null ) );

        // State: [RESPONSE_END] - Tag: </extendedResponse>
        super.transitions[Dsmlv2StatesEnum.RESPONSE_END].put( new Tag( "extendedResponse", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.RESPONSE_END, Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP, null ) );

        //====================================================
        //  Transitions concerning : LDAP RESULT
        //====================================================
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_CONTROL_START] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_CONTROL_END] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_CONTROL_VALUE_END] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_RESULT_CODE_START] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_RESULT_CODE_END] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_ERROR_MESSAGE_END] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_REFERRAL_END] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_DONE_END] = new HashMap<Tag, GrammarTransition>();

        // State: [LDAP_RESULT] - Tag: <control>
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT].put( new Tag( "control", Tag.START ), new GrammarTransition(
            Dsmlv2StatesEnum.LDAP_RESULT, Dsmlv2StatesEnum.LDAP_RESULT_CONTROL_START, ldapResultControlCreation ) );

        // State: [LDAP_RESULT] - Tag: <resultCode>
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT].put( new Tag( "resultCode", Tag.START ), new GrammarTransition(
            Dsmlv2StatesEnum.LDAP_RESULT, Dsmlv2StatesEnum.LDAP_RESULT_RESULT_CODE_START, ldapResultAddResultCode ) );

        // State: [LDAP_RESULT_CONTROL_START] - Tag: <controlValue>
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_CONTROL_START].put( new Tag( "controlValue", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.LDAP_RESULT_CONTROL_START,
                Dsmlv2StatesEnum.LDAP_RESULT_CONTROL_VALUE_END, ldapResultControlValueCreation ) );

        // State: [LDAP_RESULT_CONTROL_VALUE_END] - Tag: </control>
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_CONTROL_VALUE_END].put( new Tag( "control", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.LDAP_RESULT_CONTROL_VALUE_END,
                Dsmlv2StatesEnum.LDAP_RESULT_CONTROL_END, null ) );

        // State: [LDAP_RESULT_CONTROL_START] - Tag: </control>
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_CONTROL_START].put( new Tag( "control", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.LDAP_RESULT_CONTROL_START,
                Dsmlv2StatesEnum.LDAP_RESULT_CONTROL_END, null ) );

        // State: [LDAP_RESULT_CONTROL_END] - Tag: <control>
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_CONTROL_END].put( new Tag( "control", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.LDAP_RESULT_CONTROL_END,
                Dsmlv2StatesEnum.LDAP_RESULT_CONTROL_START, ldapResultControlCreation ) );

        // State: [LDAP_RESULT_CONTROL_END] - Tag: <resultCode>
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_CONTROL_END].put( new Tag( "resultCode", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.LDAP_RESULT_CONTROL_END,
                Dsmlv2StatesEnum.LDAP_RESULT_RESULT_CODE_START, ldapResultAddResultCode ) );

        // State: [LDAP_RESULT_RESULT_CODE_START] - Tag: </resultCode>
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_RESULT_CODE_START].put( new Tag( "resultCode", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.LDAP_RESULT_RESULT_CODE_START,
                Dsmlv2StatesEnum.LDAP_RESULT_RESULT_CODE_END, null ) );

        // State: [LDAP_RESULT_RESULT_CODE_END] - Tag: <errorMessage>
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_RESULT_CODE_END].put( new Tag( "errorMessage", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.LDAP_RESULT_RESULT_CODE_END,
                Dsmlv2StatesEnum.LDAP_RESULT_ERROR_MESSAGE_END, ldapResultAddErrorMessage ) );

        // State: [LDAP_RESULT_RESULT_CODE_END] - Tag: <referral>
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_RESULT_CODE_END].put( new Tag( "referral", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.LDAP_RESULT_RESULT_CODE_END,
                Dsmlv2StatesEnum.LDAP_RESULT_REFERRAL_END, ldapResultAddReferral ) );

        // State: [LDAP_RESULT_RESULT_CODE_END] - Tag: </addResponse>
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_RESULT_CODE_END].put( new Tag( "addResponse", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.LDAP_RESULT_RESULT_CODE_END, Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP,
                null ) );

        // State: [LDAP_RESULT_RESULT_CODE_END] - Tag: </authResponse>
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_RESULT_CODE_END].put( new Tag( "authResponse", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.LDAP_RESULT_RESULT_CODE_END, Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP,
                null ) );

        // State: [LDAP_RESULT_RESULT_CODE_END] - Tag: </compareResponse>
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_RESULT_CODE_END].put( new Tag( "compareResponse", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.LDAP_RESULT_RESULT_CODE_END, Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP,
                null ) );

        // State: [LDAP_RESULT_RESULT_CODE_END] - Tag: </delResponse>
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_RESULT_CODE_END].put( new Tag( "delResponse", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.LDAP_RESULT_RESULT_CODE_END, Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP,
                null ) );

        // State: [LDAP_RESULT_RESULT_CODE_END] - Tag: </modifyResponse>
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_RESULT_CODE_END].put( new Tag( "modifyResponse", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.LDAP_RESULT_RESULT_CODE_END, Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP,
                null ) );

        // State: [LDAP_RESULT_RESULT_CODE_END] - Tag: </modDNResponse>
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_RESULT_CODE_END].put( new Tag( "modDNResponse", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.LDAP_RESULT_RESULT_CODE_END, Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP,
                null ) );

        // State: [LDAP_RESULT_RESULT_CODE_END] - Tag: </searchResultDone>
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_RESULT_CODE_END].put( new Tag( "searchResultDone", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.LDAP_RESULT_RESULT_CODE_END,
                Dsmlv2StatesEnum.SEARCH_RESULT_DONE_END, null ) );

        // State: [SEARCH_RESULT_DONE_END] - Tag: </searchResponse>
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_DONE_END]
            .put( new Tag( "searchResponse", Tag.END ), new GrammarTransition( Dsmlv2StatesEnum.SEARCH_RESULT_DONE_END,
                Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP, null ) );

        // State: [LDAP_RESULT_ERROR_MESSAGE_END] - Tag: <referral>
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_ERROR_MESSAGE_END].put( new Tag( "referral", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.LDAP_RESULT_ERROR_MESSAGE_END,
                Dsmlv2StatesEnum.LDAP_RESULT_REFERRAL_END, ldapResultAddReferral ) );

        // State: [LDAP_RESULT_ERROR_MESSAGE_END] - Tag: </addResponse>
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_ERROR_MESSAGE_END].put( new Tag( "addResponse", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.LDAP_RESULT_ERROR_MESSAGE_END,
                Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP, null ) );

        // State: [LDAP_RESULT_ERROR_MESSAGE_END] - Tag: </authResponse>
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_ERROR_MESSAGE_END].put( new Tag( "authResponse", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.LDAP_RESULT_ERROR_MESSAGE_END,
                Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP, null ) );

        // State: [LDAP_RESULT_ERROR_MESSAGE_END] - Tag: </compareResponse>
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_ERROR_MESSAGE_END].put( new Tag( "compareResponse", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.LDAP_RESULT_ERROR_MESSAGE_END,
                Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP, null ) );

        // State: [LDAP_RESULT_ERROR_MESSAGE_END] - Tag: </delResponse>
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_ERROR_MESSAGE_END].put( new Tag( "delResponse", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.LDAP_RESULT_ERROR_MESSAGE_END,
                Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP, null ) );

        // State: [LDAP_RESULT_ERROR_MESSAGE_END] - Tag: </modifyResponse>
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_ERROR_MESSAGE_END].put( new Tag( "modifyResponse", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.LDAP_RESULT_ERROR_MESSAGE_END,
                Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP, null ) );

        // State: [LDAP_RESULT_ERROR_MESSAGE_END] - Tag: </modDNResponse>
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_ERROR_MESSAGE_END].put( new Tag( "modDNResponse", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.LDAP_RESULT_ERROR_MESSAGE_END,
                Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP, null ) );

        // State: [LDAP_RESULT_ERROR_MESSAGE_END] - Tag: </searchResultDone>
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_ERROR_MESSAGE_END].put( new Tag( "searchResultDone", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.LDAP_RESULT_ERROR_MESSAGE_END,
                Dsmlv2StatesEnum.SEARCH_RESULT_DONE_END, null ) );

        // State: [LDAP_RESULT_REFERRAL_END] - Tag: <referral>
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_REFERRAL_END].put( new Tag( "referral", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.LDAP_RESULT_REFERRAL_END,
                Dsmlv2StatesEnum.LDAP_RESULT_REFERRAL_END, ldapResultAddReferral ) );

        // State: [LDAP_RESULT_REFERRAL_END] - Tag: </addResponse>
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_REFERRAL_END].put( new Tag( "addResponse", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.LDAP_RESULT_REFERRAL_END, Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP,
                null ) );

        // State: [LDAP_RESULT_REFERRAL_END] - Tag: </authResponse>
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_REFERRAL_END].put( new Tag( "authResponse", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.LDAP_RESULT_REFERRAL_END, Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP,
                null ) );

        // State: [LDAP_RESULT_REFERRAL_END] - Tag: </compareResponse>
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_REFERRAL_END].put( new Tag( "compareResponse", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.LDAP_RESULT_REFERRAL_END, Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP,
                null ) );

        // State: [LDAP_RESULT_REFERRAL_END] - Tag: </delResponse>
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_REFERRAL_END].put( new Tag( "delResponse", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.LDAP_RESULT_REFERRAL_END, Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP,
                null ) );

        // State: [LDAP_RESULT_REFERRAL_END] - Tag: </modifyResponse>
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_REFERRAL_END].put( new Tag( "modifyResponse", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.LDAP_RESULT_REFERRAL_END, Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP,
                null ) );

        // State: [LDAP_RESULT_REFERRAL_END] - Tag: </modDNResponse>
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_REFERRAL_END].put( new Tag( "modDNResponse", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.LDAP_RESULT_REFERRAL_END, Dsmlv2StatesEnum.BATCH_RESPONSE_LOOP,
                null ) );

        // State: [LDAP_RESULT_REFERRAL_END] - Tag: </searchResultDone>
        super.transitions[Dsmlv2StatesEnum.LDAP_RESULT_REFERRAL_END].put( new Tag( "searchResultDone", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.LDAP_RESULT_REFERRAL_END, Dsmlv2StatesEnum.SEARCH_RESULT_DONE_END,
                null ) );

        //====================================================
        //  Transitions concerning : SEARCH RESPONSE
        //====================================================
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESPONSE] = new HashMap<Tag, GrammarTransition>();

        // State: [SEARCH_REPONSE] - Tag: <searchResultEntry>
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESPONSE].put( new Tag( "searchResultEntry", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_RESPONSE, Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY,
                searchResultEntryCreation ) );

        // State: [SEARCH_REPONSE] - Tag: <searchResultReference>
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESPONSE].put( new Tag( "searchResultReference", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_RESPONSE, Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE,
                searchResultReferenceCreation ) );

        // State: [SEARCH_REPONSE] - Tag: <searchResultDone>
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESPONSE].put( new Tag( "searchResultDone", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_RESPONSE, Dsmlv2StatesEnum.LDAP_RESULT,
                searchResultDoneCreation ) );

        //====================================================
        //  Transitions concerning : SEARCH RESULT ENTRY
        //====================================================
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_CONTROL_START] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_CONTROL_END] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_CONTROL_VALUE_END] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_ATTR_START] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_ATTR_END] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_VALUE_END] = new HashMap<Tag, GrammarTransition>();

        // State: [SEARCH_RESULT_ENTRY] - Tag: <control>
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY].put( new Tag( "control", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY,
                Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_CONTROL_START, searchResultEntryControlCreation ) );

        // State: [SEARCH_RESULT_ENTRY] - Tag: <attr>
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY].put( new Tag( "attr", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY,
                Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_ATTR_START, searchResultEntryAddAttr ) );

        // State: [SEARCH_RESULT_ENTRY] - Tag: </searchResultEntry>
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY].put( new Tag( "searchResultEntry", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY, Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_LOOP,
                null ) );

        // State: [SEARCH_RESULT_ENTRY_CONTROL_START] - Tag: <controlValue>
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_CONTROL_START].put(
            new Tag( "controlValue", Tag.START ), new GrammarTransition(
                Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_CONTROL_START,
                Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_CONTROL_VALUE_END, searchResultEntryControlValueCreation ) );

        // State: [SEARCH_RESULT_ENTRY_CONTROL_VALUE_END] - Tag: </control>
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_CONTROL_VALUE_END].put( new Tag( "control", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_CONTROL_VALUE_END,
                Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_CONTROL_END, null ) );

        // State: [SEARCH_RESULT_ENTRY_CONTROL_START] - Tag: </control>
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_CONTROL_START].put( new Tag( "control", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_CONTROL_START,
                Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_CONTROL_END, null ) );

        // State: [SEARCH_RESULT_ENTRY_CONTROL_END] - Tag: <control>
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_CONTROL_END].put( new Tag( "control", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_CONTROL_END,
                Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_CONTROL_START, searchResultEntryControlCreation ) );

        // State: [SEARCH_RESULT_ENTRY_CONTROL_END] - Tag: </searchResultEntry>
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_CONTROL_END].put(
            new Tag( "searchResultEntry", Tag.END ), new GrammarTransition(
                Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_CONTROL_END, Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_LOOP, null ) );

        // State: [SEARCH_RESULT_ENTRY_CONTROL_END] - Tag: <attr>
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_CONTROL_END].put( new Tag( "attr", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_CONTROL_END,
                Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_ATTR_START, null ) );

        // State: [SEARCH_RESULT_ENTRY_ATTR_START] - Tag: </attr>
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_ATTR_START].put( new Tag( "attr", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_ATTR_START,
                Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_ATTR_END, null ) );

        // State: [SEARCH_RESULT_ENTRY_ATTR_START] - Tag: <value>
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_ATTR_START].put( new Tag( "value", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_ATTR_START,
                Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_VALUE_END, searchResultEntryAddValue ) );

        // State: [SEARCH_RESULT_ENTRY_ATTR_END] - Tag: <attr>
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_ATTR_END].put( new Tag( "attr", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_ATTR_END,
                Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_ATTR_START, searchResultEntryAddAttr ) );

        // State: [SEARCH_RESULT_ENTRY_ATTR_END] - Tag: </searchResultEntry>
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_ATTR_END].put( new Tag( "searchResultEntry", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_ATTR_END,
                Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_LOOP, null ) );

        // State: [SEARCH_RESULT_ENTRY_VALUE_END] - Tag: <value>
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_VALUE_END].put( new Tag( "value", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_VALUE_END,
                Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_VALUE_END, searchResultEntryAddValue ) );

        // State: [SEARCH_RESULT_ENTRY_VALUE_END] - Tag: </attr>
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_VALUE_END].put( new Tag( "attr", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_VALUE_END,
                Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_ATTR_END, null ) );

        //====================================================
        //  Transitions concerning : SEARCH RESULT ENTRY LOOP
        //====================================================
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_LOOP] = new HashMap<Tag, GrammarTransition>();

        // State: [SEARCH_RESULT_ENTRY_LOOP] - Tag: <searchResultEntry>
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_LOOP].put( new Tag( "searchResultEntry", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_LOOP, Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY,
                searchResultEntryCreation ) );

        // State: [SEARCH_RESULT_ENTRY_LOOP] - Tag: <searchResultReference>
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_LOOP].put(
            new Tag( "searchResultReference", Tag.START ), new GrammarTransition(
                Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_LOOP, Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE,
                searchResultReferenceCreation ) );

        // State: [SEARCH_RESULT_ENTRY_LOOP] - Tag: <searchResultDone>
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_LOOP].put( new Tag( "searchResultDone", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_RESULT_ENTRY_LOOP, Dsmlv2StatesEnum.LDAP_RESULT,
                searchResultDoneCreation ) );

        //====================================================
        //  Transitions concerning : SEARCH RESULT REFERENCE
        //====================================================
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE_CONTROL_START] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE_CONTROL_END] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE_CONTROL_VALUE_END] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE_REF_END] = new HashMap<Tag, GrammarTransition>();

        // State: [SEARCH_RESULT_REFERENCE] - Tag: <control>
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE].put( new Tag( "control", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE,
                Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE_CONTROL_START, searchResultReferenceControlCreation ) );

        // State: [SEARCH_RESULT_REFERENCE] - Tag: <ref>
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE].put( new Tag( "ref", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE,
                Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE_REF_END, searchResultReferenceAddRef ) );

        // State: [SEARCH_RESULT_REFERENCE_CONTROL_START] - Tag: <controlValue>
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE_CONTROL_START].put( new Tag( "controlValue",
            Tag.START ), new GrammarTransition( Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE_CONTROL_START,
            Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE_CONTROL_VALUE_END, searchResultReferenceControlValueCreation ) );

        // State: [sEARCH_RESULT_REFERENCE_CONTROL_VALUE_END] - Tag: </control>
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE_CONTROL_VALUE_END].put(
            new Tag( "control", Tag.END ), new GrammarTransition(
                Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE_CONTROL_VALUE_END,
                Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE_CONTROL_END, null ) );

        // State: [SEARCH_RESULT_REFERENCE_CONTROL_START] - Tag: </control>
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE_CONTROL_START].put( new Tag( "control", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE_CONTROL_START,
                Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE_CONTROL_END, null ) );

        // State: [SEARCH_RESULT_REFERENCE_CONTROL_END] - Tag: <control>
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE_CONTROL_END].put( new Tag( "control", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE_CONTROL_END,
                Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE_CONTROL_START, searchResultReferenceControlCreation ) );

        // State: [SEARCH_RESULT_REFERENCE_CONTROL_END] - Tag: <ref>
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE_CONTROL_END].put( new Tag( "ref", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE_CONTROL_END,
                Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE_REF_END, searchResultReferenceAddRef ) );

        // State: [SEARCH_RESULT_REFERENCE_REF_END] - Tag: <ref>
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE_REF_END].put( new Tag( "ref", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE_REF_END,
                Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE_REF_END, searchResultReferenceAddRef ) );

        // State: [SEARCH_RESULT_REFERENCE_REF_END] - Tag: </searchResultReference>
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE_REF_END].put( new Tag( "searchResultReference",
            Tag.END ), new GrammarTransition( Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE_REF_END,
            Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE_LOOP, null ) );

        //==========================================================
        //  Transitions concerning : SEARCH RESULT REFERENCE LOOP
        //==========================================================
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE_LOOP] = new HashMap<Tag, GrammarTransition>();

        // State: [SEARCH_RESULT_REFERENCE_LOOP] - Tag: <searchResultReference>
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE_LOOP].put( new Tag( "searchResultReference",
            Tag.START ), new GrammarTransition( Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE_LOOP,
            Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE, searchResultReferenceCreation ) );

        // State: [SEARCH_RESULT_REFERENCE_LOOP] - Tag: <searchResultDone>
        super.transitions[Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE_LOOP].put( new Tag( "searchResultDone", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_RESULT_REFERENCE_LOOP, Dsmlv2StatesEnum.LDAP_RESULT,
                searchResultDoneCreation ) );
    }

    /**
     * GrammarAction that creates the Batch Response
     */
    private final GrammarAction batchResponseCreation = new GrammarAction( "Create Batch Response" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            BatchResponse batchResponse = new BatchResponse();

            container.setBatchResponse( batchResponse );

            XmlPullParser xpp = container.getParser();

            // Checking and adding the batchRequest's attributes
            String attributeValue;
            // requestID
            attributeValue = xpp.getAttributeValue( "", "requestID" );
            if ( attributeValue != null )
            {
            	batchResponse.setRequestID( ParserUtils.parseAndVerifyRequestID( attributeValue, xpp ) );
            }
        }
    };

    /**
     * GrammarAction that creates the Add Response
     */
    private final GrammarAction addResponseCreation = new GrammarAction( "Create Add Response" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            AddResponseCodec addResponse = new AddResponseCodec();

            container.getBatchResponse().addResponse( addResponse );

            LdapResultCodec ldapResult = new LdapResultCodec();

            addResponse.setLdapResult( ldapResult );

            XmlPullParser xpp = container.getParser();

            // Checking and adding the batchRequest's attributes
            String attributeValue;
            // requestID
            attributeValue = xpp.getAttributeValue( "", "requestID" );
            if ( attributeValue != null )
            {
                addResponse.setMessageId( ParserUtils.parseAndVerifyRequestID( attributeValue, xpp ) );
            }
            // MatchedDN
            attributeValue = xpp.getAttributeValue( "", "matchedDN" );
            if ( attributeValue != null )
            {
                try
                {
                    ldapResult.setMatchedDN( new LdapDN( attributeValue ) );
                }
                catch ( InvalidNameException e )
                {
                    throw new XmlPullParserException( "" + e.getMessage(), xpp, null );
                }
            }
        }
    };

    /**
     * GrammarAction that creates the Auth Response
     */
    private final GrammarAction authResponseCreation = new GrammarAction( "Create Auth Response" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            BindResponseCodec bindResponse = new BindResponseCodec();

            container.getBatchResponse().addResponse( bindResponse );

            LdapResultCodec ldapResult = new LdapResultCodec();

            bindResponse.setLdapResult( ldapResult );

            XmlPullParser xpp = container.getParser();

            // Checking and adding the batchRequest's attributes
            String attributeValue;
            // requestID
            attributeValue = xpp.getAttributeValue( "", "requestID" );
            if ( attributeValue != null )
            {
            	bindResponse.setMessageId( ParserUtils.parseAndVerifyRequestID( attributeValue, xpp ) );
               
            }
            // MatchedDN
            attributeValue = xpp.getAttributeValue( "", "matchedDN" );
            if ( attributeValue != null )
            {
                try
                {
                    ldapResult.setMatchedDN( new LdapDN( attributeValue ) );
                }
                catch ( InvalidNameException e )
                {
                    throw new XmlPullParserException( "" + e.getMessage(), xpp, null );
                }
            }
        }
    };

    /**
     * GrammarAction that creates the Compare Response
     */
    private final GrammarAction compareResponseCreation = new GrammarAction( "Create Compare Response" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            CompareResponseCodec compareResponse = new CompareResponseCodec();

            container.getBatchResponse().addResponse( compareResponse );

            LdapResultCodec ldapResult = new LdapResultCodec();

            compareResponse.setLdapResult( ldapResult );

            XmlPullParser xpp = container.getParser();

            // Checking and adding the batchRequest's attributes
            String attributeValue;
            // requestID
            attributeValue = xpp.getAttributeValue( "", "requestID" );
            if ( attributeValue != null )
            {
            	compareResponse.setMessageId( ParserUtils.parseAndVerifyRequestID( attributeValue, xpp ) );
            }
            // MatchedDN
            attributeValue = xpp.getAttributeValue( "", "matchedDN" );
            if ( attributeValue != null )
            {
                try
                {
                    ldapResult.setMatchedDN( new LdapDN( attributeValue ) );
                }
                catch ( InvalidNameException e )
                {
                    throw new XmlPullParserException( "" + e.getMessage(), xpp, null );
                }
            }
        }
    };

    /**
     * GrammarAction that creates the Del Response
     */
    private final GrammarAction delResponseCreation = new GrammarAction( "Create Del Response" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            DelResponseCodec delResponse = new DelResponseCodec();

            container.getBatchResponse().addResponse( delResponse );

            LdapResultCodec ldapResult = new LdapResultCodec();

            delResponse.setLdapResult( ldapResult );

            XmlPullParser xpp = container.getParser();

            // Checking and adding the batchRequest's attributes
            String attributeValue;
            // requestID
            attributeValue = xpp.getAttributeValue( "", "requestID" );
            if ( attributeValue != null )
            {
            	delResponse.setMessageId( ParserUtils.parseAndVerifyRequestID( attributeValue, xpp ) );        
            }
            // MatchedDN
            attributeValue = xpp.getAttributeValue( "", "matchedDN" );
            if ( attributeValue != null )
            {
                try
                {
                    ldapResult.setMatchedDN( new LdapDN( attributeValue ) );
                }
                catch ( InvalidNameException e )
                {
                    throw new XmlPullParserException( "" + e.getMessage(), xpp, null );
                }
            }
        }
    };

    /**
     * GrammarAction that creates the Modify Response
     */
    private final GrammarAction modifyResponseCreation = new GrammarAction( "Create Modify Response" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            ModifyResponseCodec modifyResponse = new ModifyResponseCodec();

            container.getBatchResponse().addResponse( modifyResponse );

            LdapResultCodec ldapResult = new LdapResultCodec();

            modifyResponse.setLdapResult( ldapResult );

            XmlPullParser xpp = container.getParser();

            // Checking and adding the batchRequest's attributes
            String attributeValue;
            // requestID
            attributeValue = xpp.getAttributeValue( "", "requestID" );
            if ( attributeValue != null )
            {
            	modifyResponse.setMessageId( ParserUtils.parseAndVerifyRequestID( attributeValue, xpp ) );
            }
            // MatchedDN
            attributeValue = xpp.getAttributeValue( "", "matchedDN" );
            if ( attributeValue != null )
            {
                try
                {
                    ldapResult.setMatchedDN( new LdapDN( attributeValue ) );
                }
                catch ( InvalidNameException e )
                {
                    throw new XmlPullParserException( "" + e.getMessage(), xpp, null );
                }
            }
        }
    };

    /**
     * GrammarAction that creates the Mod DN Response
     */
    private final GrammarAction modDNResponseCreation = new GrammarAction( "Create Mod DN Response" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            ModifyDNResponseCodec modifyDNResponse = new ModifyDNResponseCodec();

            container.getBatchResponse().addResponse( modifyDNResponse );

            LdapResultCodec ldapResult = new LdapResultCodec();

            modifyDNResponse.setLdapResult( ldapResult );

            XmlPullParser xpp = container.getParser();

            // Checking and adding the batchRequest's attributes
            String attributeValue;
            // requestID
            attributeValue = xpp.getAttributeValue( "", "requestID" );
            if ( attributeValue != null )
            {
            	modifyDNResponse.setMessageId( ParserUtils.parseAndVerifyRequestID( attributeValue, xpp ) );  
            }
            // MatchedDN
            attributeValue = xpp.getAttributeValue( "", "matchedDN" );
            if ( attributeValue != null )
            {
                try
                {
                    ldapResult.setMatchedDN( new LdapDN( attributeValue ) );
                }
                catch ( InvalidNameException e )
                {
                    throw new XmlPullParserException( "" + e.getMessage(), xpp, null );
                }
            }
        }
    };

    /**
     * GrammarAction that creates the Extended Response
     */
    private final GrammarAction extendedResponseCreation = new GrammarAction( "Create Extended Response" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            ExtendedResponseCodec extendedResponse = new ExtendedResponseCodec();

            container.getBatchResponse().addResponse( extendedResponse );

            LdapResultCodec ldapResult = new LdapResultCodec();

            extendedResponse.setLdapResult( ldapResult );

            XmlPullParser xpp = container.getParser();

            // Checking and adding the batchRequest's attributes
            String attributeValue;
            // requestID
            attributeValue = xpp.getAttributeValue( "", "requestID" );
            if ( attributeValue != null )
            {
            	extendedResponse.setMessageId( ParserUtils.parseAndVerifyRequestID( attributeValue, xpp ) );
            }
            // MatchedDN
            attributeValue = xpp.getAttributeValue( "", "matchedDN" );
            if ( attributeValue != null )
            {
                try
                {
                    ldapResult.setMatchedDN( new LdapDN( attributeValue ) );
                }
                catch ( InvalidNameException e )
                {
                    throw new XmlPullParserException( "" + e.getMessage(), xpp, null );
                }
            }
        }
    };

    /**
     * GrammarAction that creates the Error Response
     */
    private final GrammarAction errorResponseCreation = new GrammarAction( "Create Error Response" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            ErrorResponse errorResponse = new ErrorResponse();

            container.getBatchResponse().addResponse( errorResponse );

            XmlPullParser xpp = container.getParser();

            // Checking and adding the batchRequest's attributes
            String attributeValue;
            // requestID
            attributeValue = xpp.getAttributeValue( "", "requestID" );
            if ( attributeValue != null )
            {
            	errorResponse.setMessageId( ParserUtils.parseAndVerifyRequestID( attributeValue, xpp ) );
            }
            // type
            attributeValue = xpp.getAttributeValue( "", "type" );
            if ( attributeValue != null )
            {
                if ( attributeValue.equals( errorResponse.getTypeDescr( ErrorResponseType.NOT_ATTEMPTED ) ) )
                {
                    errorResponse.setType( ErrorResponseType.NOT_ATTEMPTED );
                }
                else if ( attributeValue.equals( errorResponse.getTypeDescr( ErrorResponseType.COULD_NOT_CONNECT ) ) )
                {
                    errorResponse.setType( ErrorResponseType.COULD_NOT_CONNECT );
                }
                else if ( attributeValue.equals( errorResponse.getTypeDescr( ErrorResponseType.CONNECTION_CLOSED ) ) )
                {
                    errorResponse.setType( ErrorResponseType.CONNECTION_CLOSED );
                }
                else if ( attributeValue.equals( errorResponse.getTypeDescr( ErrorResponseType.MALFORMED_REQUEST ) ) )
                {
                    errorResponse.setType( ErrorResponseType.MALFORMED_REQUEST );
                }
                else if ( attributeValue
                    .equals( errorResponse.getTypeDescr( ErrorResponseType.GATEWAY_INTERNAL_ERROR ) ) )
                {
                    errorResponse.setType( ErrorResponseType.GATEWAY_INTERNAL_ERROR );
                }
                else if ( attributeValue.equals( errorResponse.getTypeDescr( ErrorResponseType.AUTHENTICATION_FAILED ) ) )
                {
                    errorResponse.setType( ErrorResponseType.AUTHENTICATION_FAILED );
                }
                else if ( attributeValue.equals( errorResponse.getTypeDescr( ErrorResponseType.UNRESOLVABLE_URI ) ) )
                {
                    errorResponse.setType( ErrorResponseType.UNRESOLVABLE_URI );
                }
                else if ( attributeValue.equals( errorResponse.getTypeDescr( ErrorResponseType.OTHER ) ) )
                {
                    errorResponse.setType( ErrorResponseType.OTHER );
                }
                else
                {
                    throw new XmlPullParserException( "Unknown type", xpp, null );
                }
            }
            else
            {
                throw new XmlPullParserException( "type attribute is required", xpp, null );
            }
        }
    };

    /**
     * GrammarAction that adds Message to an Error Response
     */
    private final GrammarAction errorResponseAddMessage = new GrammarAction( "Add Message to Error Response" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            ErrorResponse errorResponse = ( ErrorResponse ) container.getBatchResponse().getCurrentResponse();
            
            XmlPullParser xpp = container.getParser();
            try
            {
                String nextText = xpp.nextText();
                if ( !nextText.equals( "" ) )
                {
                    errorResponse.setMessage( nextText.trim() );
                }
            }
            catch ( IOException e )
            {
                throw new XmlPullParserException( e.getMessage(), xpp, null );
            }
        }
    };

    /**
     * GrammarAction that adds Detail to an Error Response
     */
    private final GrammarAction errorResponseAddDetail = null; // TODO Look for documentation about this Detail element (the DSML documentation doesn't give enough information)


    /**
     * Creates a Control parsing the current node and adds it to the given parent 
     * @param container the DSMLv2Container
     * @param parent the parent 
     * @throws XmlPullParserException
     */
    private void createAndAddControl( Dsmlv2Container container, LdapMessageCodec parent ) throws XmlPullParserException
    {
        ControlCodec control = new ControlCodec();

        parent.addControl( control );

        XmlPullParser xpp = container.getParser();

        // Checking and adding the Control's attributes
        String attributeValue;
        // TYPE
        attributeValue = xpp.getAttributeValue( "", "type" );
        if ( attributeValue != null )
        {
            if ( !OID.isOID( attributeValue ) )
            {
                throw new XmlPullParserException( "Incorrect value for 'type' attribute. This is not an OID.", xpp, null );
            }
            control.setControlType( attributeValue );
        }
        else
        {
            throw new XmlPullParserException( "type attribute is required", xpp, null );
        }
        // CRITICALITY
        attributeValue = xpp.getAttributeValue( "", "criticality" );
        if ( attributeValue != null )
        {
            if ( attributeValue.equals( "true" ) )
            {
                control.setCriticality( true );
            }
            else if ( attributeValue.equals( "false" ) )
            {
                control.setCriticality( false );
            }
            else
            {
                throw new XmlPullParserException( "Incorrect value for 'criticality' attribute", xpp, null );
            }
        }
    }

    /**
     * GrammarAction that creates a Control for LDAP Result
     */
    private final GrammarAction ldapResultControlCreation = new GrammarAction( "Create Control for LDAP Result" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            LdapResponseCodec ldapResponse = container.getBatchResponse().getCurrentResponse();
            // Search Response is a special case
            if ( ldapResponse instanceof SearchResponse )
            {
                ldapResponse = ( ( SearchResponse ) ldapResponse ).getSearchResultDone();
            }

            createAndAddControl( container, ldapResponse );
        }
    };

    /**
     * GrammarAction that creates a Control for Search Result Entry
     */
    private final GrammarAction searchResultEntryControlCreation = new GrammarAction(
        "Create Control for Search Result Entry" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            LdapMessageCodec ldapMessage = ( ( SearchResponse ) container.getBatchResponse().getCurrentResponse() )
                .getCurrentSearchResultEntry();
            createAndAddControl( container, ldapMessage );
        }
    };

    /**
     * GrammarAction that creates a Control for Search Result Entry
     */
    private final GrammarAction searchResultReferenceControlCreation = new GrammarAction(
        "Create Control for Search Result Reference" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            LdapMessageCodec ldapMessage = ( ( SearchResponse ) container.getBatchResponse().getCurrentResponse() )
                .getCurrentSearchResultReference();
            createAndAddControl( container, ldapMessage );
        }
    };


    /**
     * Creates a Control Value parsing the current node and adds it to the given parent 
     * @param container the DSMLv2Container
     * @param parent the parent 
     * @throws XmlPullParserException
     */
    private void createAndAddControlValue( Dsmlv2Container container, LdapMessageCodec parent )
        throws XmlPullParserException
    {
        ControlCodec control = parent.getCurrentControl();

        XmlPullParser xpp = container.getParser();
        try
        {
            // We have to catch the type Attribute Value before going to the next Text node
            String typeValue = ParserUtils.getXsiTypeAttributeValue( xpp );
            
            // Getting the value
            String nextText = xpp.nextText();
            if ( !nextText.equals( "" ) )
            {
                if ( ParserUtils.isBase64BinaryValue( xpp, typeValue ) )
                {
                    control.setControlValue( Base64.decode( nextText.trim().toCharArray() ) );
                }
                else
                {
                    control.setControlValue( nextText.trim() );
                }
            }
        }
        catch ( IOException e )
        {
            throw new XmlPullParserException( "An unexpected error ocurred : " + e.getMessage(), xpp, null );
        }
    }

    /**
     * GrammarAction that creates a Control Value for LDAP Result
     */
    private final GrammarAction ldapResultControlValueCreation = new GrammarAction(
        "Add ControlValue to Control for LDAP Result" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            LdapResponseCodec ldapResponse = container.getBatchResponse().getCurrentResponse();
            // Search Response is a special case
            if ( ldapResponse instanceof SearchResponse )
            {
                ldapResponse = ( ( SearchResponse ) ldapResponse ).getSearchResultDone();
            }

            createAndAddControlValue( container, ldapResponse );
        }
    };

    /**
     * GrammarAction that creates a Control Value for Search Result Entry
     */
    private final GrammarAction searchResultEntryControlValueCreation = new GrammarAction(
        "Add ControlValue to Control for Search Result Entry" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            LdapMessageCodec ldapMessage = ( ( SearchResponse ) container.getBatchResponse().getCurrentResponse() )
                .getCurrentSearchResultEntry();
            createAndAddControlValue( container, ldapMessage );
        }
    };

    /**
     * GrammarAction that creates a Control Value for Search Result Reference
     */
    private final GrammarAction searchResultReferenceControlValueCreation = new GrammarAction(
        "Add ControlValue to Control for Search Result Entry" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            LdapMessageCodec ldapMessage = ( ( SearchResponse ) container.getBatchResponse().getCurrentResponse() )
                .getCurrentSearchResultReference();
            createAndAddControlValue( container, ldapMessage );
        }
    };

    /**
     * GrammarAction that adds a Result Code to a LDAP Result
     */
    private final GrammarAction ldapResultAddResultCode = new GrammarAction( "Add ResultCode to LDAP Result" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            LdapResponseCodec ldapResponse = container.getBatchResponse().getCurrentResponse();

            LdapResultCodec ldapResult = null;

            // Search Response is a special case
            // ResultCode can only occur in a case of Search Result Done in a Search Response
            if ( ldapResponse instanceof SearchResponse )
            {
                SearchResponse searchResponse = ( SearchResponse ) ldapResponse;
                ldapResult = searchResponse.getSearchResultDone().getLdapResult();
            }
            else
            {
                ldapResult = ldapResponse.getLdapResult();
            }

            XmlPullParser xpp = container.getParser();

            // Checking and adding the request's attributes
            String attributeValue;
            // code
            attributeValue = xpp.getAttributeValue( "", "code" );
            if ( attributeValue != null )
            {
                try
                {
                    ldapResult.setResultCode( ResultCodeEnum.getResultCode( Integer.parseInt( attributeValue ) ) );
                }
                catch ( NumberFormatException e )
                {
                    throw new XmlPullParserException( "the given resultCode is not an integer", xpp, null );
                }
            }
            else
            {
                throw new XmlPullParserException( "code attribute is required", xpp, null );
            }
            // descr
            attributeValue = xpp.getAttributeValue( "", "descr" );
            if ( attributeValue != null )
            {
                if ( DSMLV2_DESCR_TAGS.contains( attributeValue ) == false )
                {
                    throw new XmlPullParserException( "descr ('" + attributeValue
                        + "') doesn't match with the possible values", xpp, null );
                }

            }
        }
    };

    /**
     * GrammarAction that adds a Error Message to a LDAP Result
     */
    private final GrammarAction ldapResultAddErrorMessage = new GrammarAction( "Add Error Message to LDAP Result" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            LdapResponseCodec ldapResponse = container.getBatchResponse().getCurrentResponse();

            LdapResultCodec ldapResult = null;

            // Search Response is a special case
            // ResultCode can only occur in a case of Search Result Done in a Search Response
            if ( ldapResponse instanceof SearchResponse )
            {
                SearchResponse searchResponse = ( SearchResponse ) ldapResponse;
                ldapResult = searchResponse.getSearchResultDone().getLdapResult();
            }
            else
            {
                ldapResult = ldapResponse.getLdapResult();
            }

            XmlPullParser xpp = container.getParser();
            try
            {
                String nextText = xpp.nextText();
                if ( !nextText.equals( "" ) )
                {
                    ldapResult.setErrorMessage( nextText.trim() );
                }
            }
            catch ( IOException e )
            {
                throw new XmlPullParserException( "An unexpected error ocurred : " + e.getMessage(), xpp, null );
            }
        }
    };

    /**
     * GrammarAction that adds a Referral to a LDAP Result
     */
    private final GrammarAction ldapResultAddReferral = new GrammarAction( "Add Referral to LDAP Result" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            LdapResponseCodec ldapResponse = container.getBatchResponse().getCurrentResponse();

            LdapResultCodec ldapResult = null;

            // Search Response is a special case
            // ResultCode can only occur in a case of Search Result Done in a Search Response
            if ( ldapResponse instanceof SearchResponse )
            {
                SearchResponse searchResponse = ( SearchResponse ) ldapResponse;
                ldapResult = searchResponse.getSearchResultDone().getLdapResult();
            }
            else
            {
                ldapResult = ldapResponse.getLdapResult();
            }

            // Initialization of the Referrals if needed
            if ( ldapResult.getReferrals() == null )
            {
                ldapResult.initReferrals();
            }

            XmlPullParser xpp = container.getParser();
            try
            {
                String nextText = xpp.nextText();
                if ( !nextText.equals( "" ) )
                {
                    try
                    {
                        ldapResult.addReferral( new LdapURL( nextText.trim() ) );
                    }
                    catch ( LdapURLEncodingException e )
                    {
                        throw new XmlPullParserException( e.getMessage(), xpp, null );
                    }
                }
            }
            catch ( IOException e )
            {
                throw new XmlPullParserException( "An unexpected error ocurred : " + e.getMessage(), xpp, null );
            }
        }
    };

    /**
     * GrammarAction that creates the Search Response
     */
    private final GrammarAction searchResponseCreation = new GrammarAction( "Create Search Response" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            SearchResponse searchResponse = new SearchResponse();

            container.getBatchResponse().addResponse( searchResponse );

            XmlPullParser xpp = container.getParser();

            // Checking and adding the batchRequest's attributes
            String attributeValue;
            // requestID
            attributeValue = xpp.getAttributeValue( "", "requestID" );
            if ( attributeValue != null )
            {
            	searchResponse.setMessageId( ParserUtils.parseAndVerifyRequestID( attributeValue, xpp ) );
            }
        }
    };

    /**
     * GrammarAction that creates a Search Result Entry
     */
    private final GrammarAction searchResultEntryCreation = new GrammarAction(
        "Add Search Result Entry to Search Response" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            SearchResultEntryCodec searchResultEntry = new SearchResultEntryCodec();

            SearchResponse searchResponse = ( SearchResponse ) container.getBatchResponse().getCurrentResponse();

            searchResponse.addSearchResultEntry( searchResultEntry );

            XmlPullParser xpp = container.getParser();

            // Checking and adding the request's attributes
            String attributeValue;
            // requestID
            attributeValue = xpp.getAttributeValue( "", "requestID" );
            if ( attributeValue != null )
            {
            	searchResultEntry.setMessageId( ParserUtils.parseAndVerifyRequestID( attributeValue, xpp ) );
            }
            // dn
            attributeValue = xpp.getAttributeValue( "", "dn" );
            if ( attributeValue != null )
            {
                try
                {
                    searchResultEntry.setObjectName( new LdapDN( attributeValue ) );
                }
                catch ( InvalidNameException e )
                {
                    throw new XmlPullParserException( e.getMessage(), xpp, null );
                }
            }
            else
            {
                throw new XmlPullParserException( "dn attribute is required", xpp, null );
            }
        }
    };

    /**
     * GrammarAction that creates a Search Result Reference
     */
    private final GrammarAction searchResultReferenceCreation = new GrammarAction(
        "Add Search Result Reference to Search Response" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            SearchResultReferenceCodec searchResultReference = new SearchResultReferenceCodec();

            SearchResponse searchResponse = ( SearchResponse ) container.getBatchResponse().getCurrentResponse();

            searchResponse.addSearchResultReference( searchResultReference );

            XmlPullParser xpp = container.getParser();

            // Checking and adding the request's attributes
            String attributeValue;
            // requestID
            attributeValue = xpp.getAttributeValue( "", "requestID" );
            if ( attributeValue != null )
            {
            	searchResultReference.setMessageId( ParserUtils.parseAndVerifyRequestID( attributeValue, xpp ) );
            }
        }
    };

    /**
     * GrammarAction that creates a Search Result Done
     */
    private final GrammarAction searchResultDoneCreation = new GrammarAction(
        "Add Search Result Done to Search Response" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            SearchResultDoneCodec searchResultDone = new SearchResultDoneCodec();

            searchResultDone.setLdapResult( new LdapResultCodec() );

            SearchResponse searchResponse = ( SearchResponse ) container.getBatchResponse().getCurrentResponse();

            searchResponse.setSearchResultDone( searchResultDone );

            XmlPullParser xpp = container.getParser();

            // Checking and adding the batchRequest's attributes
            String attributeValue;
            // requestID
            attributeValue = xpp.getAttributeValue( "", "requestID" );
            if ( attributeValue != null )
            {
            	searchResultDone.setMessageId( ParserUtils.parseAndVerifyRequestID( attributeValue, xpp ) );
            }
            // MatchedDN
            attributeValue = xpp.getAttributeValue( "", "matchedDN" );
            if ( attributeValue != null )
            {
                try
                {
                    searchResultDone.getLdapResult().setMatchedDN( new LdapDN( attributeValue ) );
                }
                catch ( InvalidNameException e )
                {
                    throw new XmlPullParserException( "" + e.getMessage(), xpp, null );
                }
            }
        }
    };

    /**
     * GrammarAction that adds an Attr to a Search Result Entry
     */
    private final GrammarAction searchResultEntryAddAttr = new GrammarAction( "Add Attr to Search Result Entry" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            SearchResponse searchResponse = ( SearchResponse ) container.getBatchResponse().getCurrentResponse();

            SearchResultEntryCodec searchResultEntry = searchResponse.getCurrentSearchResultEntry();

            XmlPullParser xpp = container.getParser();

            // Checking and adding the request's attributes
            String attributeValue;
            // name
            attributeValue = xpp.getAttributeValue( "", "name" );
            if ( attributeValue != null )
            {
                searchResultEntry.addAttributeValues( attributeValue );
            }
            else
            {
                throw new XmlPullParserException( "name attribute is required", xpp, null );
            }
        }
    };

    /**
     * GrammarAction that adds a Value to an Attr of a Search Result Entry
     */
    private final GrammarAction searchResultEntryAddValue = new GrammarAction(
        "Add a Value to an Attr of a Search Result Entry" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            SearchResponse searchResponse = ( SearchResponse ) container.getBatchResponse().getCurrentResponse();
            SearchResultEntryCodec searchResultEntry = searchResponse.getCurrentSearchResultEntry();

            XmlPullParser xpp = container.getParser();
            try
            {
                // We have to catch the type Attribute Value before going to the next Text node
                String typeValue = ParserUtils.getXsiTypeAttributeValue( xpp );
                
                // Getting the value
                String nextText = xpp.nextText();
                if ( ParserUtils.isBase64BinaryValue( xpp, typeValue ) )
                {
                    searchResultEntry.addAttributeValue( Base64.decode( nextText.toCharArray() ) );
                }
                else
                {
                    searchResultEntry.addAttributeValue( nextText );
                }
            }
            catch ( IOException e )
            {
                throw new XmlPullParserException( "An unexpected error ocurred : " + e.getMessage(), xpp, null );
            }
        }
    };

    /**
     * GrammarAction that adds a Ref to a Search Result Reference
     */
    private final GrammarAction searchResultReferenceAddRef = new GrammarAction(
        "Add a Ref to a Search Result Reference" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            SearchResponse searchResponse = ( SearchResponse ) container.getBatchResponse().getCurrentResponse();
            SearchResultReferenceCodec searchResultReference = searchResponse.getCurrentSearchResultReference();

            XmlPullParser xpp = container.getParser();
            try
            {
                String nextText = xpp.nextText();
                if ( !nextText.equals( "" ) )
                {
                    searchResultReference.addSearchResultReference( new LdapURL( nextText ) );
                }
            }
            catch ( IOException e )
            {
                throw new XmlPullParserException( "An unexpected error ocurred : " + e.getMessage(), xpp, null );
            }
            catch ( LdapURLEncodingException e )
            {
                throw new XmlPullParserException( e.getMessage(), xpp, null );
            }
        }
    };

    /**
     * GrammarAction that adds Result Code to an Extended Response
     */
    private final GrammarAction extendedResponseAddResultCode = ldapResultAddResultCode;

    /**
     * GrammarAction that creates the Search Response
     */
    private final GrammarAction extendedResponseAddErrorMessage = ldapResultAddErrorMessage;

    /**
     * GrammarAction that adds a Referral to an Extended Response
     */
    private final GrammarAction extendedResponseAddReferral = ldapResultAddReferral;

    /**
     * GrammarAction that adds a Response Name to an Extended Response
     */
    private final GrammarAction extendedResponseAddResponseName = new GrammarAction(
        "Add Response Name to Extended Response" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            ExtendedResponseCodec extendedResponse = ( ExtendedResponseCodec ) container.getBatchResponse().getCurrentResponse();

            XmlPullParser xpp = container.getParser();
            try
            {
                String nextText = xpp.nextText();
                if ( !nextText.equals( "" ) )
                {
                    extendedResponse.setResponseName( new OID( nextText.trim() ) );
                }
                
            }
            catch ( IOException e )
            {
                throw new XmlPullParserException( "An unexpected error ocurred : " + e.getMessage(), xpp, null );
            }
            catch ( DecoderException e )
            {
                throw new XmlPullParserException( e.getMessage(), xpp, null );
            }
        }
    };

    /**
     * GrammarAction that adds a Response to an Extended Response
     */
    private final GrammarAction extendedResponseAddResponse = new GrammarAction( "Add Response to Extended Response" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            ExtendedResponseCodec extendedResponse = ( ExtendedResponseCodec ) container.getBatchResponse().getCurrentResponse();

            XmlPullParser xpp = container.getParser();
            try
            {
                // We have to catch the type Attribute Value before going to the next Text node
                String typeValue = ParserUtils.getXsiTypeAttributeValue( xpp );
                
                // Getting the value
                String nextText = xpp.nextText();
                if ( ParserUtils.isBase64BinaryValue( xpp, typeValue ) )
                {
                    extendedResponse.setResponse( Base64.decode( nextText.trim().toCharArray() ) );
                }
                else
                {
                    extendedResponse.setResponse( nextText.trim() );
                }
            }
            catch ( IOException e )
            {
                throw new XmlPullParserException( "An unexpected error ocurred : " + e.getMessage(), xpp, null );
            }
        }
    };


    /**
     * Get the instance of this grammar
     * 
     * @return
     *      an instance on this grammar
     */
    public static Dsmlv2ResponseGrammar getInstance()
    {
        return instance;
    }
}
