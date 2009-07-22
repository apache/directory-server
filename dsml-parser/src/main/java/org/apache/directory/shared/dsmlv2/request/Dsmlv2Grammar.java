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

package org.apache.directory.shared.dsmlv2.request;


import java.io.IOException;
import java.lang.reflect.Array;
import java.util.HashMap;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;

import org.apache.directory.shared.asn1.Asn1Object;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.dsmlv2.AbstractGrammar;
import org.apache.directory.shared.dsmlv2.Dsmlv2Container;
import org.apache.directory.shared.dsmlv2.Dsmlv2StatesEnum;
import org.apache.directory.shared.dsmlv2.GrammarAction;
import org.apache.directory.shared.dsmlv2.GrammarTransition;
import org.apache.directory.shared.dsmlv2.IGrammar;
import org.apache.directory.shared.dsmlv2.ParserUtils;
import org.apache.directory.shared.dsmlv2.Tag;
import org.apache.directory.shared.dsmlv2.request.BatchRequest.OnError;
import org.apache.directory.shared.dsmlv2.request.BatchRequest.Processing;
import org.apache.directory.shared.dsmlv2.request.BatchRequest.ResponseOrder;
import org.apache.directory.shared.ldap.codec.AttributeValueAssertion;
import org.apache.directory.shared.ldap.codec.ControlCodec;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.abandon.AbandonRequestCodec;
import org.apache.directory.shared.ldap.codec.add.AddRequestCodec;
import org.apache.directory.shared.ldap.codec.bind.BindRequestCodec;
import org.apache.directory.shared.ldap.codec.bind.SimpleAuthentication;
import org.apache.directory.shared.ldap.codec.compare.CompareRequestCodec;
import org.apache.directory.shared.ldap.codec.del.DelRequestCodec;
import org.apache.directory.shared.ldap.codec.extended.ExtendedRequestCodec;
import org.apache.directory.shared.ldap.codec.modify.ModifyRequestCodec;
import org.apache.directory.shared.ldap.codec.modifyDn.ModifyDNRequestCodec;
import org.apache.directory.shared.ldap.codec.search.AndFilter;
import org.apache.directory.shared.ldap.codec.search.AttributeValueAssertionFilter;
import org.apache.directory.shared.ldap.codec.search.ExtensibleMatchFilter;
import org.apache.directory.shared.ldap.codec.search.Filter;
import org.apache.directory.shared.ldap.codec.search.NotFilter;
import org.apache.directory.shared.ldap.codec.search.OrFilter;
import org.apache.directory.shared.ldap.codec.search.PresentFilter;
import org.apache.directory.shared.ldap.codec.search.SearchRequestCodec;
import org.apache.directory.shared.ldap.codec.search.SubstringFilter;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.entry.client.ClientBinaryValue;
import org.apache.directory.shared.ldap.entry.client.ClientStringValue;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.util.Base64;
import org.apache.directory.shared.ldap.util.StringTools;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;


/**
 * This Class represents the DSMLv2 Request Grammar
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class Dsmlv2Grammar extends AbstractGrammar implements IGrammar
{
    /** The instance of grammar. Dsmlv2Grammar is a singleton */
    private static Dsmlv2Grammar instance = new Dsmlv2Grammar();


    /**
     * Creates a new instance of Dsmlv2Grammar.
     */
    @SuppressWarnings("unchecked")
    private Dsmlv2Grammar()
    {
        name = Dsmlv2Grammar.class.getName();
        statesEnum = Dsmlv2StatesEnum.getInstance();

        // Create the transitions table
        super.transitions = ( HashMap<Tag, GrammarTransition>[] ) Array.newInstance( HashMap.class, 200 );; // TODO Change this value

        //====================================================
        //  Transitions concerning : BATCH REQUEST
        //====================================================
        super.transitions[Dsmlv2StatesEnum.INIT_GRAMMAR_STATE] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.BATCHREQUEST_START_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.BATCHREQUEST_LOOP] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.BATCHREQUEST_END_TAG] = new HashMap<Tag, GrammarTransition>();

        // ** OPEN BATCH REQUEST **
        // State: [INIT_GRAMMAR_STATE] - Tag: <batchRequest>
        super.transitions[Dsmlv2StatesEnum.INIT_GRAMMAR_STATE].put( new Tag( "batchRequest", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.INIT_GRAMMAR_STATE, Dsmlv2StatesEnum.BATCHREQUEST_START_TAG,
                batchRequestCreation ) );

        // ** CLOSE BATCH REQUEST **
        // state: [BATCHREQUEST_START_TAG] - Tag: </batchRequest>
        super.transitions[Dsmlv2StatesEnum.BATCHREQUEST_START_TAG]
            .put( new Tag( "batchRequest", Tag.END ), new GrammarTransition( Dsmlv2StatesEnum.BATCHREQUEST_START_TAG,
                Dsmlv2StatesEnum.BATCHREQUEST_END_TAG, null ) );
        //state: [BATCHREQUEST_LOOP] - Tag: </batchRequest>
        super.transitions[Dsmlv2StatesEnum.BATCHREQUEST_LOOP].put( new Tag( "batchRequest", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.BATCHREQUEST_LOOP, Dsmlv2StatesEnum.END_STATE, null ) );

        // ** ABANDON REQUEST **
        // State: [BATCHREQUEST_START_TAG] - Tag: <abandonRequest>
        super.transitions[Dsmlv2StatesEnum.BATCHREQUEST_START_TAG].put( new Tag( "abandonRequest", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.BATCHREQUEST_START_TAG, Dsmlv2StatesEnum.ABANDON_REQUEST_START_TAG,
                abandonRequestCreation ) );
        // state: [BATCHREQUEST_LOOP] - Tag: <abandonRequest>
        super.transitions[Dsmlv2StatesEnum.BATCHREQUEST_LOOP].put( new Tag( "abandonRequest", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.BATCHREQUEST_LOOP, Dsmlv2StatesEnum.ABANDON_REQUEST_START_TAG,
                abandonRequestCreation ) );

        // ** ADD REQUEST **
        // state: [BATCHREQUEST_START_TAG] - Tag: <addRequest>
        super.transitions[Dsmlv2StatesEnum.BATCHREQUEST_START_TAG].put( new Tag( "addRequest", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.BATCHREQUEST_START_TAG, Dsmlv2StatesEnum.ADD_REQUEST_START_TAG,
                addRequestCreation ) );
        // state: [BATCHREQUEST_LOOP] - Tag: <addRequest>
        super.transitions[Dsmlv2StatesEnum.BATCHREQUEST_LOOP].put( new Tag( "addRequest", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.BATCHREQUEST_LOOP, Dsmlv2StatesEnum.ADD_REQUEST_START_TAG,
                addRequestCreation ) );

        // ** AUTH REQUEST **
        // state: [BATCHREQUEST_START_TAG] - Tag: <authRequest>
        super.transitions[Dsmlv2StatesEnum.BATCHREQUEST_START_TAG].put( new Tag( "authRequest", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.BATCHREQUEST_START_TAG, Dsmlv2StatesEnum.AUTH_REQUEST_START_TAG,
                authRequestCreation ) );

        // ** COMPARE REQUEST **
        // state: [BATCHREQUEST_START_TAG] - Tag: <compareRequest>
        super.transitions[Dsmlv2StatesEnum.BATCHREQUEST_START_TAG].put( new Tag( "compareRequest", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.BATCHREQUEST_START_TAG, Dsmlv2StatesEnum.COMPARE_REQUEST_START_TAG,
                compareRequestCreation ) );
        // state: [BATCHREQUEST_LOOP] - Tag: <compareRequest>
        super.transitions[Dsmlv2StatesEnum.BATCHREQUEST_LOOP].put( new Tag( "compareRequest", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.BATCHREQUEST_LOOP, Dsmlv2StatesEnum.COMPARE_REQUEST_START_TAG,
                compareRequestCreation ) );

        // ** DEL REQUEST **
        // state: [BATCHREQUEST_START_TAG] - Tag: <delRequest>
        super.transitions[Dsmlv2StatesEnum.BATCHREQUEST_START_TAG].put( new Tag( "delRequest", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.BATCHREQUEST_START_TAG, Dsmlv2StatesEnum.DEL_REQUEST_START_TAG,
                delRequestCreation ) );
        // state: [BATCHREQUEST_LOOP] - Tag: <delRequest>
        super.transitions[Dsmlv2StatesEnum.BATCHREQUEST_LOOP].put( new Tag( "delRequest", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.BATCHREQUEST_LOOP, Dsmlv2StatesEnum.DEL_REQUEST_START_TAG,
                delRequestCreation ) );

        // ** EXTENDED REQUEST **
        // state: [BATCHREQUEST_START_TAG] - Tag: <extendedRequest>
        super.transitions[Dsmlv2StatesEnum.BATCHREQUEST_START_TAG].put( new Tag( "extendedRequest", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.BATCHREQUEST_START_TAG,
                Dsmlv2StatesEnum.EXTENDED_REQUEST_START_TAG, extendedRequestCreation ) );
        // state: [BATCHREQUEST_LOOP] - Tag: <extendedRequest>
        super.transitions[Dsmlv2StatesEnum.BATCHREQUEST_LOOP].put( new Tag( "extendedRequest", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.BATCHREQUEST_LOOP, Dsmlv2StatesEnum.EXTENDED_REQUEST_START_TAG,
                extendedRequestCreation ) );

        // ** MOD DN REQUEST **
        // state: [BATCHREQUEST_START_TAG] - Tag: <modDNRequest>
        super.transitions[Dsmlv2StatesEnum.BATCHREQUEST_START_TAG].put( new Tag( "modDNRequest", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.BATCHREQUEST_START_TAG,
                Dsmlv2StatesEnum.MODIFY_DN_REQUEST_START_TAG, modDNRequestCreation ) );
        // state: [BATCHREQUEST_LOOP] - Tag: <modDNRequest>
        super.transitions[Dsmlv2StatesEnum.BATCHREQUEST_LOOP].put( new Tag( "modDNRequest", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.BATCHREQUEST_LOOP, Dsmlv2StatesEnum.MODIFY_DN_REQUEST_START_TAG,
                modDNRequestCreation ) );

        // ** MODIFY REQUEST **
        // state: [BATCHREQUEST_START_TAG] - Tag: <modifyRequest>
        super.transitions[Dsmlv2StatesEnum.BATCHREQUEST_START_TAG].put( new Tag( "modifyRequest", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.BATCHREQUEST_START_TAG, Dsmlv2StatesEnum.MODIFY_REQUEST_START_TAG,
                modifyRequestCreation ) );
        // state: [BATCHREQUEST_LOOP] - Tag: <modifyRequest>
        super.transitions[Dsmlv2StatesEnum.BATCHREQUEST_LOOP].put( new Tag( "modifyRequest", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.BATCHREQUEST_LOOP, Dsmlv2StatesEnum.MODIFY_REQUEST_START_TAG,
                modifyRequestCreation ) );

        // ** SEARCH REQUEST **
        // state: [BATCHREQUEST_START_TAG] - Tag: <searchRequest>
        super.transitions[Dsmlv2StatesEnum.BATCHREQUEST_START_TAG].put( new Tag( "searchRequest", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.BATCHREQUEST_START_TAG, Dsmlv2StatesEnum.SEARCH_REQUEST_START_TAG,
                searchRequestCreation ) );
        // state: [BATCHREQUEST_LOOP] - Tag: <searchRequest>
        super.transitions[Dsmlv2StatesEnum.BATCHREQUEST_LOOP].put( new Tag( "searchRequest", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.BATCHREQUEST_LOOP, Dsmlv2StatesEnum.SEARCH_REQUEST_START_TAG,
                searchRequestCreation ) );

        //====================================================
        //  Transitions concerning : ABANDON REQUEST
        //====================================================
        super.transitions[Dsmlv2StatesEnum.ABANDON_REQUEST_START_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.ABANDON_REQUEST_CONTROL_START_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.ABANDON_REQUEST_CONTROL_END_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.ABANDON_REQUEST_CONTROLVALUE_END_TAG] = new HashMap<Tag, GrammarTransition>();

        // State: [ABANDON_REQUEST_START_TAG] - Tag: </abandonRequest>
        super.transitions[Dsmlv2StatesEnum.ABANDON_REQUEST_START_TAG]
            .put( new Tag( "abandonRequest", Tag.END ), new GrammarTransition(
                Dsmlv2StatesEnum.ABANDON_REQUEST_START_TAG, Dsmlv2StatesEnum.BATCHREQUEST_LOOP, null ) );

        // State: [ABANDON_REQUEST_START_TAG] - Tag: <control>
        super.transitions[Dsmlv2StatesEnum.ABANDON_REQUEST_START_TAG].put( new Tag( "control", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.ABANDON_REQUEST_START_TAG,
                Dsmlv2StatesEnum.ABANDON_REQUEST_CONTROL_START_TAG, controlCreation ) );

        // State: [ABANDON_REQUEST_CONTROL_START_TAG] - Tag: <controlValue>
        super.transitions[Dsmlv2StatesEnum.ABANDON_REQUEST_CONTROL_START_TAG].put(
            new Tag( "controlValue", Tag.START ), new GrammarTransition(
                Dsmlv2StatesEnum.ABANDON_REQUEST_CONTROL_START_TAG,
                Dsmlv2StatesEnum.ABANDON_REQUEST_CONTROLVALUE_END_TAG, controlValueCreation ) );

        // State: [ABANDON_REQUEST_CONTROLVALUE_END_TAG] - Tag: </control>
        super.transitions[Dsmlv2StatesEnum.ABANDON_REQUEST_CONTROLVALUE_END_TAG].put( new Tag( "control", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.ABANDON_REQUEST_CONTROLVALUE_END_TAG,
                Dsmlv2StatesEnum.ABANDON_REQUEST_CONTROL_END_TAG, null ) );

        // State: [ABANDON_REQUEST_CONTROL_START_TAG] - Tag: </control>
        super.transitions[Dsmlv2StatesEnum.ABANDON_REQUEST_CONTROL_START_TAG].put( new Tag( "control", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.ABANDON_REQUEST_CONTROL_START_TAG,
                Dsmlv2StatesEnum.ABANDON_REQUEST_CONTROL_END_TAG, null ) );

        // State: [ABANDON_REQUEST_CONTROL_END_TAG] - Tag: <control>
        super.transitions[Dsmlv2StatesEnum.ABANDON_REQUEST_CONTROL_END_TAG].put( new Tag( "control", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.ABANDON_REQUEST_CONTROL_END_TAG,
                Dsmlv2StatesEnum.ABANDON_REQUEST_CONTROL_START_TAG, controlCreation ) );

        // State: [ABANDON_REQUEST_CONTROL_END_TAG] - Tag: </abandonRequest>
        super.transitions[Dsmlv2StatesEnum.ABANDON_REQUEST_CONTROL_END_TAG].put( new Tag( "abandonRequest", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.ABANDON_REQUEST_CONTROL_END_TAG,
                Dsmlv2StatesEnum.BATCHREQUEST_LOOP, null ) );

        //====================================================
        //  Transitions concerning : ADD REQUEST
        //====================================================
        super.transitions[Dsmlv2StatesEnum.ADD_REQUEST_START_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.ADD_REQUEST_CONTROL_START_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.ADD_REQUEST_CONTROL_END_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.ADD_REQUEST_CONTROLVALUE_END_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.ADD_REQUEST_ATTR_START_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.ADD_REQUEST_ATTR_END_TAG] = new HashMap<Tag, GrammarTransition>();

        // state: [ADD_REQUEST_START_TAG] -> Tag: </addRequest>
        super.transitions[Dsmlv2StatesEnum.ADD_REQUEST_START_TAG].put( new Tag( "addRequest", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.ADD_REQUEST_START_TAG, Dsmlv2StatesEnum.BATCHREQUEST_LOOP, null ) );

        // State: [ADD_REQUEST_START_TAG] - Tag: <control>
        super.transitions[Dsmlv2StatesEnum.ADD_REQUEST_START_TAG].put( new Tag( "control", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.ADD_REQUEST_START_TAG,
                Dsmlv2StatesEnum.ADD_REQUEST_CONTROL_START_TAG, controlCreation ) );

        // State: [ADD_REQUEST_CONTROL_START_TAG] - Tag: <controlValue>
        super.transitions[Dsmlv2StatesEnum.ADD_REQUEST_CONTROL_START_TAG].put( new Tag( "controlValue", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.ADD_REQUEST_CONTROL_START_TAG,
                Dsmlv2StatesEnum.ADD_REQUEST_CONTROLVALUE_END_TAG, controlValueCreation ) );

        // State: [ADD_REQUEST_CONTROLVALUE_END_TAG] - Tag: </control>
        super.transitions[Dsmlv2StatesEnum.ADD_REQUEST_CONTROLVALUE_END_TAG].put( new Tag( "control", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.ADD_REQUEST_CONTROLVALUE_END_TAG,
                Dsmlv2StatesEnum.ADD_REQUEST_CONTROL_END_TAG, null ) );

        // State: [ADD_REQUEST_CONTROL_START_TAG] - Tag: </control>
        super.transitions[Dsmlv2StatesEnum.ADD_REQUEST_CONTROL_START_TAG].put( new Tag( "control", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.ADD_REQUEST_CONTROL_START_TAG,
                Dsmlv2StatesEnum.ADD_REQUEST_CONTROL_END_TAG, null ) );

        // State: [ADD_REQUEST_CONTROL_END_TAG] - Tag: <control>
        super.transitions[Dsmlv2StatesEnum.ADD_REQUEST_CONTROL_END_TAG].put( new Tag( "control", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.ADD_REQUEST_CONTROL_END_TAG,
                Dsmlv2StatesEnum.ADD_REQUEST_CONTROL_START_TAG, controlCreation ) );

        // State: [ADD_REQUEST_CONTROL_END_TAG] - Tag: </addRequest>
        super.transitions[Dsmlv2StatesEnum.ADD_REQUEST_CONTROL_END_TAG].put( new Tag( "addRequest", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.ADD_REQUEST_CONTROL_END_TAG, Dsmlv2StatesEnum.BATCHREQUEST_LOOP,
                null ) );

        // State: [ADD_REQUEST_START_TAG] - Tag: <attr>
        super.transitions[Dsmlv2StatesEnum.ADD_REQUEST_START_TAG].put( new Tag( "attr", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.ADD_REQUEST_START_TAG, Dsmlv2StatesEnum.ADD_REQUEST_ATTR_START_TAG,
                addRequestAddAttribute ) );

        // State: [ADD_REQUEST_CONTROL_END_TAG] - Tag: <attr>
        super.transitions[Dsmlv2StatesEnum.ADD_REQUEST_CONTROL_END_TAG].put( new Tag( "attr", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.ADD_REQUEST_CONTROL_END_TAG,
                Dsmlv2StatesEnum.ADD_REQUEST_ATTR_START_TAG, addRequestAddAttribute ) );

        // State: [ADD_REQUEST_ATTR_END_TAG] - Tag: <attr>
        super.transitions[Dsmlv2StatesEnum.ADD_REQUEST_ATTR_END_TAG].put( new Tag( "attr", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.ADD_REQUEST_ATTR_END_TAG,
                Dsmlv2StatesEnum.ADD_REQUEST_ATTR_START_TAG, addRequestAddAttribute ) );

        // State: [ADD_REQUEST_ATTR_START_TAG] - Tag: </attr>
        super.transitions[Dsmlv2StatesEnum.ADD_REQUEST_ATTR_START_TAG].put( new Tag( "attr", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.ADD_REQUEST_ATTR_START_TAG,
                Dsmlv2StatesEnum.ADD_REQUEST_ATTR_END_TAG, null ) );

        // State: [ADD_REQUEST_ATTR_START_TAG] - Tag: <value>
        super.transitions[Dsmlv2StatesEnum.ADD_REQUEST_ATTR_START_TAG].put( new Tag( "value", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.ADD_REQUEST_ATTR_START_TAG,
                Dsmlv2StatesEnum.ADD_REQUEST_ATTR_START_TAG, addRequestAddValue ) );

        // State: [ADD_REQUEST_ATTR_END_TAG] - Tag: </addRequest>
        super.transitions[Dsmlv2StatesEnum.ADD_REQUEST_ATTR_END_TAG]
            .put( new Tag( "addRequest", Tag.END ), new GrammarTransition( Dsmlv2StatesEnum.ADD_REQUEST_ATTR_END_TAG,
                Dsmlv2StatesEnum.BATCHREQUEST_LOOP, null ) );

        //====================================================
        //  Transitions concerning : AUTH REQUEST
        //====================================================
        super.transitions[Dsmlv2StatesEnum.AUTH_REQUEST_START_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.AUTH_REQUEST_CONTROL_START_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.AUTH_REQUEST_CONTROL_END_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.AUTH_REQUEST_CONTROLVALUE_END_TAG] = new HashMap<Tag, GrammarTransition>();

        // state: [AUTH_REQUEST_START_TAG] -> Tag: </authRequest>
        super.transitions[Dsmlv2StatesEnum.AUTH_REQUEST_START_TAG].put( new Tag( "authRequest", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.AUTH_REQUEST_START_TAG, Dsmlv2StatesEnum.BATCHREQUEST_LOOP, null ) );

        // State: [AUTH_REQUEST_START_TAG] - Tag: <control>
        super.transitions[Dsmlv2StatesEnum.AUTH_REQUEST_START_TAG].put( new Tag( "control", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.AUTH_REQUEST_START_TAG,
                Dsmlv2StatesEnum.AUTH_REQUEST_CONTROL_START_TAG, controlCreation ) );

        // State: [AUTH_REQUEST_CONTROL_START_TAG] - Tag: <controlValue>
        super.transitions[Dsmlv2StatesEnum.AUTH_REQUEST_CONTROL_START_TAG].put( new Tag( "controlValue", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.AUTH_REQUEST_CONTROL_START_TAG,
                Dsmlv2StatesEnum.AUTH_REQUEST_CONTROLVALUE_END_TAG, controlValueCreation ) );

        // State: [AUTH_REQUEST_CONTROLVALUE_END_TAG] - Tag: </control>
        super.transitions[Dsmlv2StatesEnum.AUTH_REQUEST_CONTROLVALUE_END_TAG].put( new Tag( "control", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.AUTH_REQUEST_CONTROLVALUE_END_TAG,
                Dsmlv2StatesEnum.AUTH_REQUEST_CONTROL_END_TAG, null ) );

        // State: [AUTH_REQUEST_CONTROL_START_TAG] - Tag: </control>
        super.transitions[Dsmlv2StatesEnum.AUTH_REQUEST_CONTROL_START_TAG].put( new Tag( "control", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.AUTH_REQUEST_CONTROL_START_TAG,
                Dsmlv2StatesEnum.AUTH_REQUEST_CONTROL_END_TAG, null ) );

        // State: [AUTH_REQUEST_CONTROL_END_TAG] - Tag: <control>
        super.transitions[Dsmlv2StatesEnum.AUTH_REQUEST_CONTROL_END_TAG].put( new Tag( "control", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.AUTH_REQUEST_CONTROL_END_TAG,
                Dsmlv2StatesEnum.AUTH_REQUEST_CONTROL_START_TAG, controlCreation ) );

        // State: [AUTH_REQUEST_CONTROL_END_TAG] - Tag: </authRequest>
        super.transitions[Dsmlv2StatesEnum.AUTH_REQUEST_CONTROL_END_TAG].put( new Tag( "authRequest", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.AUTH_REQUEST_CONTROL_END_TAG, Dsmlv2StatesEnum.BATCHREQUEST_LOOP,
                null ) );

        //====================================================
        //  Transitions concerning : COMPARE REQUEST
        //====================================================
        super.transitions[Dsmlv2StatesEnum.COMPARE_REQUEST_START_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.COMPARE_REQUEST_CONTROL_START_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.COMPARE_REQUEST_CONTROL_END_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.COMPARE_REQUEST_CONTROLVALUE_END_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.COMPARE_REQUEST_ASSERTION_START_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.COMPARE_REQUEST_ASSERTION_END_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.COMPARE_REQUEST_VALUE_END_TAG] = new HashMap<Tag, GrammarTransition>();

        // State: [COMPARE_REQUEST_START_TAG] - Tag: <control>
        super.transitions[Dsmlv2StatesEnum.COMPARE_REQUEST_START_TAG].put( new Tag( "control", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.COMPARE_REQUEST_START_TAG,
                Dsmlv2StatesEnum.COMPARE_REQUEST_CONTROL_START_TAG, controlCreation ) );

        // State: [COMPARE_REQUEST_CONTROL_START_TAG] - Tag: <controlValue>
        super.transitions[Dsmlv2StatesEnum.COMPARE_REQUEST_CONTROL_START_TAG].put(
            new Tag( "controlValue", Tag.START ), new GrammarTransition(
                Dsmlv2StatesEnum.COMPARE_REQUEST_CONTROL_START_TAG,
                Dsmlv2StatesEnum.COMPARE_REQUEST_CONTROLVALUE_END_TAG, controlValueCreation ) );

        // State: [COMPARE_REQUEST_CONTROLVALUE_END_TAG] - Tag: </control>
        super.transitions[Dsmlv2StatesEnum.COMPARE_REQUEST_CONTROLVALUE_END_TAG].put( new Tag( "control", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.COMPARE_REQUEST_CONTROLVALUE_END_TAG,
                Dsmlv2StatesEnum.COMPARE_REQUEST_CONTROL_END_TAG, null ) );

        // State: [COMPARE_REQUEST_CONTROL_START_TAG] - Tag: </control>
        super.transitions[Dsmlv2StatesEnum.COMPARE_REQUEST_CONTROL_START_TAG].put( new Tag( "control", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.COMPARE_REQUEST_CONTROL_START_TAG,
                Dsmlv2StatesEnum.COMPARE_REQUEST_CONTROL_END_TAG, null ) );

        // State: [COMPARE_REQUEST_CONTROL_END_TAG] - Tag: <control>
        super.transitions[Dsmlv2StatesEnum.COMPARE_REQUEST_CONTROL_END_TAG].put( new Tag( "control", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.COMPARE_REQUEST_CONTROL_END_TAG,
                Dsmlv2StatesEnum.COMPARE_REQUEST_CONTROL_START_TAG, controlCreation ) );

        // State: [COMPARE_REQUEST_CONTROL_END_TAG] - Tag: </compareRequest>
        super.transitions[Dsmlv2StatesEnum.COMPARE_REQUEST_CONTROL_END_TAG].put( new Tag( "compareRequest", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.COMPARE_REQUEST_CONTROL_END_TAG,
                Dsmlv2StatesEnum.BATCHREQUEST_LOOP, null ) );

        // State: [COMPARE_REQUEST_START_TAG] - Tag: <assertion>
        super.transitions[Dsmlv2StatesEnum.COMPARE_REQUEST_START_TAG].put( new Tag( "assertion", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.COMPARE_REQUEST_CONTROL_START_TAG,
                Dsmlv2StatesEnum.COMPARE_REQUEST_ASSERTION_START_TAG, compareRequestAddAssertion ) );

        // State: [COMPARE_REQUEST_CONTROL_END_TAG] - Tag: <assertion>
        super.transitions[Dsmlv2StatesEnum.COMPARE_REQUEST_CONTROL_END_TAG].put( new Tag( "assertion", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.COMPARE_REQUEST_CONTROL_END_TAG,
                Dsmlv2StatesEnum.COMPARE_REQUEST_ASSERTION_START_TAG, compareRequestAddAssertion ) );

        // State: [COMPARE_REQUEST_ASSERTION_START_TAG] - Tag: <value>
        super.transitions[Dsmlv2StatesEnum.COMPARE_REQUEST_ASSERTION_START_TAG].put( new Tag( "value", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.COMPARE_REQUEST_ASSERTION_START_TAG,
                Dsmlv2StatesEnum.COMPARE_REQUEST_VALUE_END_TAG, compareRequestAddValue ) );

        //State: [COMPARE_REQUEST_VALUE_END_TAG] - Tag: </assertion>
        super.transitions[Dsmlv2StatesEnum.COMPARE_REQUEST_VALUE_END_TAG].put( new Tag( "assertion", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.COMPARE_REQUEST_VALUE_END_TAG,
                Dsmlv2StatesEnum.COMPARE_REQUEST_ASSERTION_END_TAG, null ) );

        // State: [COMPARE_REQUEST_ASSERTION_END_TAG] - Tag: </compareRequest>
        super.transitions[Dsmlv2StatesEnum.COMPARE_REQUEST_ASSERTION_END_TAG].put(
            new Tag( "compareRequest", Tag.END ), new GrammarTransition(
                Dsmlv2StatesEnum.COMPARE_REQUEST_ASSERTION_END_TAG, Dsmlv2StatesEnum.BATCHREQUEST_LOOP, null ) );

        //====================================================
        //  Transitions concerning : DEL REQUEST
        //====================================================
        super.transitions[Dsmlv2StatesEnum.DEL_REQUEST_START_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.DEL_REQUEST_CONTROL_START_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.DEL_REQUEST_CONTROL_END_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.DEL_REQUEST_CONTROLVALUE_END_TAG] = new HashMap<Tag, GrammarTransition>();

        // State: [DEL_REQUEST_START_TAG] - Tag: </delRequest>
        super.transitions[Dsmlv2StatesEnum.DEL_REQUEST_START_TAG].put( new Tag( "delRequest", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.DEL_REQUEST_START_TAG, Dsmlv2StatesEnum.BATCHREQUEST_LOOP, null ) );

        // State: [DEL_REQUEST_START_TAG] - Tag: <control>
        super.transitions[Dsmlv2StatesEnum.DEL_REQUEST_START_TAG].put( new Tag( "control", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.DEL_REQUEST_START_TAG,
                Dsmlv2StatesEnum.DEL_REQUEST_CONTROL_START_TAG, controlCreation ) );

        // State: [DEL_REQUEST_CONTROL_START_TAG] - Tag: <controlValue>
        super.transitions[Dsmlv2StatesEnum.DEL_REQUEST_CONTROL_START_TAG].put( new Tag( "controlValue", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.DEL_REQUEST_CONTROL_START_TAG,
                Dsmlv2StatesEnum.DEL_REQUEST_CONTROLVALUE_END_TAG, controlValueCreation ) );

        // State: [DEL_REQUEST_CONTROLVALUE_END_TAG] - Tag: </control>
        super.transitions[Dsmlv2StatesEnum.DEL_REQUEST_CONTROLVALUE_END_TAG].put( new Tag( "control", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.DEL_REQUEST_CONTROLVALUE_END_TAG,
                Dsmlv2StatesEnum.DEL_REQUEST_CONTROL_END_TAG, null ) );

        // State: [DEL_REQUEST_CONTROL_START_TAG] - Tag: </control>
        super.transitions[Dsmlv2StatesEnum.DEL_REQUEST_CONTROL_START_TAG].put( new Tag( "control", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.DEL_REQUEST_CONTROL_START_TAG,
                Dsmlv2StatesEnum.DEL_REQUEST_CONTROL_END_TAG, null ) );

        // State: [DEL_REQUEST_CONTROL_END_TAG] - Tag: <control>
        super.transitions[Dsmlv2StatesEnum.DEL_REQUEST_CONTROL_END_TAG].put( new Tag( "control", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.DEL_REQUEST_CONTROL_END_TAG,
                Dsmlv2StatesEnum.DEL_REQUEST_CONTROL_START_TAG, controlCreation ) );

        // State: [DEL_REQUEST_CONTROL_END_TAG] - Tag: </delRequest>
        super.transitions[Dsmlv2StatesEnum.DEL_REQUEST_CONTROL_END_TAG].put( new Tag( "delRequest", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.DEL_REQUEST_CONTROL_END_TAG, Dsmlv2StatesEnum.BATCHREQUEST_LOOP,
                null ) );

        //====================================================
        //  Transitions concerning : EXTENDED REQUEST
        //====================================================
        super.transitions[Dsmlv2StatesEnum.EXTENDED_REQUEST_START_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.EXTENDED_REQUEST_CONTROL_START_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.EXTENDED_REQUEST_CONTROL_END_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.EXTENDED_REQUEST_CONTROLVALUE_END_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.EXTENDED_REQUEST_REQUESTNAME_END_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.EXTENDED_REQUEST_REQUESTVALUE_END_TAG] = new HashMap<Tag, GrammarTransition>();

        // State: [EXTENDED_REQUEST_START_TAG] - Tag: <control>
        super.transitions[Dsmlv2StatesEnum.EXTENDED_REQUEST_START_TAG].put( new Tag( "control", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.EXTENDED_REQUEST_START_TAG,
                Dsmlv2StatesEnum.EXTENDED_REQUEST_CONTROL_START_TAG, controlCreation ) );

        // State: [EXTENDED_REQUEST_CONTROL_START_TAG] - Tag: <controlValue>
        super.transitions[Dsmlv2StatesEnum.EXTENDED_REQUEST_CONTROL_START_TAG].put(
            new Tag( "controlValue", Tag.START ), new GrammarTransition(
                Dsmlv2StatesEnum.EXTENDED_REQUEST_CONTROL_START_TAG,
                Dsmlv2StatesEnum.EXTENDED_REQUEST_CONTROLVALUE_END_TAG, controlValueCreation ) );

        // State: [EXTENDED_REQUEST_CONTROLVALUE_END_TAG] - Tag: </control>
        super.transitions[Dsmlv2StatesEnum.EXTENDED_REQUEST_CONTROLVALUE_END_TAG].put( new Tag( "control", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.EXTENDED_REQUEST_CONTROLVALUE_END_TAG,
                Dsmlv2StatesEnum.EXTENDED_REQUEST_CONTROL_END_TAG, null ) );

        // State: [EXTENDED_REQUEST_CONTROL_START_TAG] - Tag: </control>
        super.transitions[Dsmlv2StatesEnum.EXTENDED_REQUEST_CONTROL_START_TAG].put( new Tag( "control", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.EXTENDED_REQUEST_CONTROL_START_TAG,
                Dsmlv2StatesEnum.EXTENDED_REQUEST_CONTROL_END_TAG, null ) );

        // State: [EXTENDED_REQUEST_CONTROL_END_TAG] - Tag: <control>
        super.transitions[Dsmlv2StatesEnum.EXTENDED_REQUEST_CONTROL_END_TAG].put( new Tag( "control", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.EXTENDED_REQUEST_CONTROL_END_TAG,
                Dsmlv2StatesEnum.EXTENDED_REQUEST_CONTROL_START_TAG, controlCreation ) );

        // State: [EXTENDED_REQUEST_CONTROL_END_TAG] - Tag: </extendedRequest>
        super.transitions[Dsmlv2StatesEnum.EXTENDED_REQUEST_CONTROL_END_TAG].put(
            new Tag( "extendedRequest", Tag.END ), new GrammarTransition(
                Dsmlv2StatesEnum.EXTENDED_REQUEST_CONTROL_END_TAG, Dsmlv2StatesEnum.BATCHREQUEST_LOOP, null ) );

        // State: [EXTENDED_REQUEST_START_TAG] - Tag: <requestName>
        super.transitions[Dsmlv2StatesEnum.EXTENDED_REQUEST_START_TAG].put( new Tag( "requestName", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.EXTENDED_REQUEST_START_TAG,
                Dsmlv2StatesEnum.EXTENDED_REQUEST_REQUESTNAME_END_TAG, extendedRequestAddName ) );

        // State: [EXTENDED_REQUEST_CONTROL_END_TAG] - Tag: <requestName>
        super.transitions[Dsmlv2StatesEnum.EXTENDED_REQUEST_CONTROL_END_TAG].put( new Tag( "requestName", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.EXTENDED_REQUEST_CONTROL_END_TAG,
                Dsmlv2StatesEnum.EXTENDED_REQUEST_REQUESTNAME_END_TAG, extendedRequestAddName ) );

        // State: [EXTENDED_REQUEST_REQUESTNAME_END_TAG] - Tag: </extendedRequest>
        super.transitions[Dsmlv2StatesEnum.EXTENDED_REQUEST_REQUESTNAME_END_TAG].put( new Tag( "extendedRequest",
            Tag.END ), new GrammarTransition( Dsmlv2StatesEnum.EXTENDED_REQUEST_REQUESTNAME_END_TAG,
            Dsmlv2StatesEnum.BATCHREQUEST_LOOP, null ) );

        // State: [EXTENDED_REQUEST_REQUESTNAME_END_TAG] - Tag: <requestValue>
        super.transitions[Dsmlv2StatesEnum.EXTENDED_REQUEST_REQUESTNAME_END_TAG].put( new Tag( "requestValue",
            Tag.START ), new GrammarTransition( Dsmlv2StatesEnum.EXTENDED_REQUEST_REQUESTNAME_END_TAG,
            Dsmlv2StatesEnum.EXTENDED_REQUEST_REQUESTVALUE_END_TAG, extendedRequestAddValue ) );

        // State: [EXTENDED_REQUEST_REQUESTVALUE_END_TAG] - Tag: </requestRequest>
        super.transitions[Dsmlv2StatesEnum.EXTENDED_REQUEST_REQUESTVALUE_END_TAG].put( new Tag( "extendedRequest",
            Tag.END ), new GrammarTransition( Dsmlv2StatesEnum.EXTENDED_REQUEST_REQUESTVALUE_END_TAG,
            Dsmlv2StatesEnum.BATCHREQUEST_LOOP, null ) );

        //====================================================
        //  Transitions concerning : MODIFY DN REQUEST
        //====================================================
        super.transitions[Dsmlv2StatesEnum.MODIFY_DN_REQUEST_START_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.MODIFY_DN_REQUEST_CONTROL_START_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.MODIFY_DN_REQUEST_CONTROL_END_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.MODIFY_DN_REQUEST_CONTROLVALUE_END_TAG] = new HashMap<Tag, GrammarTransition>();

        // State: [MODIFY_DN_REQUEST_START_TAG] - Tag: </modDNRequest>
        super.transitions[Dsmlv2StatesEnum.MODIFY_DN_REQUEST_START_TAG].put( new Tag( "modDNRequest", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.MODIFY_DN_REQUEST_START_TAG, Dsmlv2StatesEnum.BATCHREQUEST_LOOP,
                null ) );

        // State: [MODIFY_DN_REQUEST_START_TAG] - Tag: <control>
        super.transitions[Dsmlv2StatesEnum.MODIFY_DN_REQUEST_START_TAG].put( new Tag( "control", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.MODIFY_DN_REQUEST_START_TAG,
                Dsmlv2StatesEnum.MODIFY_DN_REQUEST_CONTROL_START_TAG, controlCreation ) );

        // State: [MODIFY_DN_REQUEST_CONTROL_START_TAG] - Tag: <controlValue>
        super.transitions[Dsmlv2StatesEnum.MODIFY_DN_REQUEST_CONTROL_START_TAG].put(
            new Tag( "controlValue", Tag.START ), new GrammarTransition(
                Dsmlv2StatesEnum.MODIFY_DN_REQUEST_CONTROL_START_TAG,
                Dsmlv2StatesEnum.MODIFY_DN_REQUEST_CONTROLVALUE_END_TAG, controlValueCreation ) );

        // State: [MODIFY_DN_REQUEST_CONTROLVALUE_END_TAG] - Tag: </control>
        super.transitions[Dsmlv2StatesEnum.MODIFY_DN_REQUEST_CONTROLVALUE_END_TAG].put( new Tag( "control", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.MODIFY_DN_REQUEST_CONTROLVALUE_END_TAG,
                Dsmlv2StatesEnum.MODIFY_DN_REQUEST_CONTROL_END_TAG, null ) );

        // State: [MODIFY_DN_REQUEST_CONTROL_START_TAG] - Tag: </control>
        super.transitions[Dsmlv2StatesEnum.MODIFY_DN_REQUEST_CONTROL_START_TAG].put( new Tag( "control", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.MODIFY_DN_REQUEST_CONTROL_START_TAG,
                Dsmlv2StatesEnum.MODIFY_DN_REQUEST_CONTROL_END_TAG, null ) );

        // State: [MODIFY_DN_REQUEST_CONTROL_END_TAG] - Tag: <control>
        super.transitions[Dsmlv2StatesEnum.MODIFY_DN_REQUEST_CONTROL_END_TAG].put( new Tag( "control", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.MODIFY_DN_REQUEST_CONTROL_END_TAG,
                Dsmlv2StatesEnum.MODIFY_DN_REQUEST_CONTROL_START_TAG, controlCreation ) );

        // State: [MODIFY_DN_REQUEST_CONTROL_END_TAG] - Tag: </modDNRequest>
        super.transitions[Dsmlv2StatesEnum.MODIFY_DN_REQUEST_CONTROL_END_TAG].put( new Tag( "modDNRequest", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.MODIFY_DN_REQUEST_CONTROL_END_TAG,
                Dsmlv2StatesEnum.BATCHREQUEST_LOOP, null ) );

        //====================================================
        //  Transitions concerning : MODIFY REQUEST
        //====================================================
        super.transitions[Dsmlv2StatesEnum.MODIFY_REQUEST_START_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.MODIFY_REQUEST_CONTROL_START_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.MODIFY_REQUEST_CONTROL_END_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.MODIFY_REQUEST_CONTROLVALUE_END_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.MODIFY_REQUEST_MODIFICATION_START_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.MODIFY_REQUEST_MODIFICATION_END_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.MODIFY_REQUEST_VALUE_END_TAG] = new HashMap<Tag, GrammarTransition>();

        // State: [MODIFY_REQUEST_START_TAG] - Tag: </modifyRequest>
        super.transitions[Dsmlv2StatesEnum.MODIFY_REQUEST_START_TAG]
            .put( new Tag( "modifyRequest", Tag.END ), new GrammarTransition(
                Dsmlv2StatesEnum.MODIFY_REQUEST_START_TAG, Dsmlv2StatesEnum.BATCHREQUEST_LOOP, null ) );

        // State: [MODIFY_REQUEST_START_TAG] - Tag: <control>
        super.transitions[Dsmlv2StatesEnum.MODIFY_REQUEST_START_TAG].put( new Tag( "control", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.MODIFY_REQUEST_START_TAG,
                Dsmlv2StatesEnum.MODIFY_REQUEST_CONTROL_START_TAG, controlCreation ) );

        // State: [MODIFY_REQUEST_CONTROL_START_TAG] - Tag: <controlValue>
        super.transitions[Dsmlv2StatesEnum.MODIFY_REQUEST_CONTROL_START_TAG].put( new Tag( "controlValue", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.MODIFY_REQUEST_CONTROL_START_TAG,
                Dsmlv2StatesEnum.MODIFY_REQUEST_CONTROLVALUE_END_TAG, controlValueCreation ) );

        // State: [MODIFY_REQUEST_CONTROLVALUE_END_TAG] - Tag: </control>
        super.transitions[Dsmlv2StatesEnum.MODIFY_REQUEST_CONTROLVALUE_END_TAG].put( new Tag( "control", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.MODIFY_REQUEST_CONTROLVALUE_END_TAG,
                Dsmlv2StatesEnum.MODIFY_REQUEST_CONTROL_END_TAG, null ) );

        // State: [MODIFY_REQUEST_CONTROL_START_TAG] - Tag: </control>
        super.transitions[Dsmlv2StatesEnum.MODIFY_REQUEST_CONTROL_START_TAG].put( new Tag( "control", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.MODIFY_REQUEST_CONTROL_START_TAG,
                Dsmlv2StatesEnum.MODIFY_REQUEST_CONTROL_END_TAG, null ) );

        // State: [MODIFY_REQUEST_CONTROL_END_TAG] - Tag: <control>
        super.transitions[Dsmlv2StatesEnum.MODIFY_REQUEST_CONTROL_END_TAG].put( new Tag( "control", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.MODIFY_REQUEST_CONTROL_END_TAG,
                Dsmlv2StatesEnum.MODIFY_REQUEST_CONTROL_START_TAG, controlCreation ) );

        // State: [MODIFY_REQUEST_CONTROL_END_TAG] - Tag: </modifyRequest>
        super.transitions[Dsmlv2StatesEnum.MODIFY_REQUEST_CONTROL_END_TAG].put( new Tag( "modifyRequest", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.MODIFY_REQUEST_CONTROL_END_TAG, Dsmlv2StatesEnum.BATCHREQUEST_LOOP,
                null ) );

        // State: [MODIFY_REQUEST_CONTROL_END_TAG] - Tag: <modification>
        super.transitions[Dsmlv2StatesEnum.MODIFY_REQUEST_CONTROL_END_TAG].put( new Tag( "modification", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.MODIFY_REQUEST_CONTROL_END_TAG,
                Dsmlv2StatesEnum.MODIFY_REQUEST_MODIFICATION_START_TAG, modifyRequestAddModification ) );

        // State: [MODIFY_REQUEST_START_TAG] - Tag: <modification>
        super.transitions[Dsmlv2StatesEnum.MODIFY_REQUEST_START_TAG].put( new Tag( "modification", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.MODIFY_REQUEST_START_TAG,
                Dsmlv2StatesEnum.MODIFY_REQUEST_MODIFICATION_START_TAG, modifyRequestAddModification ) );

        // State: [MODIFY_REQUEST_MODIFICATION_END_TAG] - Tag: <modification>
        super.transitions[Dsmlv2StatesEnum.MODIFY_REQUEST_MODIFICATION_END_TAG].put(
            new Tag( "modification", Tag.START ), new GrammarTransition(
                Dsmlv2StatesEnum.MODIFY_REQUEST_MODIFICATION_END_TAG,
                Dsmlv2StatesEnum.MODIFY_REQUEST_MODIFICATION_START_TAG, modifyRequestAddModification ) );

        // State: [MODIFY_REQUEST_MODIFICATION_START_TAG] - Tag: </modification>
        super.transitions[Dsmlv2StatesEnum.MODIFY_REQUEST_MODIFICATION_START_TAG].put(
            new Tag( "modification", Tag.END ), new GrammarTransition(
                Dsmlv2StatesEnum.MODIFY_REQUEST_MODIFICATION_START_TAG,
                Dsmlv2StatesEnum.MODIFY_REQUEST_MODIFICATION_END_TAG, null ) );

        // State: [MODIFY_REQUEST_MODIFICATION_START_TAG] - Tag: <value>
        super.transitions[Dsmlv2StatesEnum.MODIFY_REQUEST_MODIFICATION_START_TAG].put( new Tag( "value", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.MODIFY_REQUEST_MODIFICATION_START_TAG,
                Dsmlv2StatesEnum.MODIFY_REQUEST_VALUE_END_TAG, modifyRequestAddValue ) );

        // State: [MODIFY_REQUEST_VALUE_END_TAG] - Tag: <value>
        super.transitions[Dsmlv2StatesEnum.MODIFY_REQUEST_VALUE_END_TAG].put( new Tag( "value", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.MODIFY_REQUEST_VALUE_END_TAG,
                Dsmlv2StatesEnum.MODIFY_REQUEST_VALUE_END_TAG, modifyRequestAddValue ) );

        // State: [MODIFY_REQUEST_VALUE_END_TAG] - Tag: </modification>
        super.transitions[Dsmlv2StatesEnum.MODIFY_REQUEST_VALUE_END_TAG].put( new Tag( "modification", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.MODIFY_REQUEST_VALUE_END_TAG,
                Dsmlv2StatesEnum.MODIFY_REQUEST_MODIFICATION_END_TAG, null ) );

        // State: [MODIFY_REQUEST_MODIFICATION_END_TAG] - Tag: </modifyRequest>
        super.transitions[Dsmlv2StatesEnum.MODIFY_REQUEST_MODIFICATION_END_TAG].put(
            new Tag( "modifyRequest", Tag.END ), new GrammarTransition(
                Dsmlv2StatesEnum.MODIFY_REQUEST_MODIFICATION_END_TAG, Dsmlv2StatesEnum.BATCHREQUEST_LOOP, null ) );

        //====================================================
        //  Transitions concerning : SEARCH REQUEST
        //====================================================
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_START_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_CONTROL_START_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_CONTROL_END_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_CONTROLVALUE_END_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_ATTRIBUTES_START_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_ATTRIBUTES_END_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_ATTRIBUTE_START_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_ATTRIBUTE_END_TAG] = new HashMap<Tag, GrammarTransition>();

        // State: [SEARCH_REQUEST_START_TAG] - Tag: <control>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_START_TAG].put( new Tag( "control", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_START_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_CONTROL_START_TAG, controlCreation ) );

        // State: [SEARCH_REQUEST_CONTROL_START_TAG] - Tag: <controlValue>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_CONTROL_START_TAG].put( new Tag( "controlValue", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_CONTROL_START_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_CONTROLVALUE_END_TAG, controlValueCreation ) );

        // State: [SEARCH_REQUEST_CONTROLVALUE_END_TAG] - Tag: </control>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_CONTROLVALUE_END_TAG].put( new Tag( "control", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_CONTROLVALUE_END_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_CONTROL_END_TAG, null ) );

        // State: [SEARCH_REQUEST_CONTROL_START_TAG] - Tag: </control>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_CONTROL_START_TAG].put( new Tag( "control", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_CONTROL_START_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_CONTROL_END_TAG, null ) );

        // State: [SEARCH_REQUEST_CONTROL_END_TAG] - Tag: <control>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_CONTROL_END_TAG].put( new Tag( "control", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_CONTROL_END_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_CONTROL_START_TAG, controlCreation ) );

        // State: [SEARCH_REQUEST_ATTRIBUTES_START_TAG] - Tag: </attributes>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_ATTRIBUTES_START_TAG].put( new Tag( "attributes", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_ATTRIBUTES_START_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_ATTRIBUTES_END_TAG, null ) );

        // State: [SEARCH_REQUEST_ATTRIBUTES_START_TAG] - Tag: <attribute>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_ATTRIBUTES_START_TAG].put( new Tag( "attribute", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_ATTRIBUTES_START_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_ATTRIBUTE_START_TAG, searchRequestAddAttribute ) );

        // State: [SEARCH_REQUEST_ATTRIBUTE_START_TAG] - Tag: </attribute>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_ATTRIBUTE_START_TAG].put( new Tag( "attribute", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_ATTRIBUTE_START_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_ATTRIBUTE_END_TAG, null ) );

        // State: [SEARCH_REQUEST_ATTRIBUTE_END_TAG] - Tag: <attribute>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_ATTRIBUTE_END_TAG].put( new Tag( "attribute", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_ATTRIBUTE_END_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_ATTRIBUTE_START_TAG, searchRequestAddAttribute ) );

        // State: [SEARCH_REQUEST_ATTRIBUTE_END_TAG] - Tag: </attributes>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_ATTRIBUTE_END_TAG].put( new Tag( "attributes", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_ATTRIBUTE_END_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_ATTRIBUTES_END_TAG, null ) );

        // State: [SEARCH_REQUEST_ATTRIBUTES_END_TAG] - Tag: </searchRequest>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_ATTRIBUTES_END_TAG].put( new Tag( "searchRequest", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_ATTRIBUTES_END_TAG,
                Dsmlv2StatesEnum.BATCHREQUEST_LOOP, null ) );

        //====================================================
        //  Transitions concerning : FILTER
        //====================================================
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_START_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_END_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_EQUALITYMATCH_START_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_GREATEROREQUAL_START_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_LESSOREQUAL_START_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_APPROXMATCH_START_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_PRESENT_START_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_EXTENSIBLEMATCH_START_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_EXTENSIBLEMATCH_VALUE_END_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_VALUE_END_TAG] = new HashMap<Tag, GrammarTransition>();

        // State: [SEARCH_REQUEST_START_TAG] - Tag: <filter>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_START_TAG].put( new Tag( "filter", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_START_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_START_TAG, null ) );

        // State: [SEARCH_REQUEST_CONTROL_END_TAG] - Tag: <filter>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_CONTROL_END_TAG].put( new Tag( "filter", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_CONTROL_END_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_START_TAG, null ) );

        //*** AND ***
        // State: [SEARCH_REQUEST_FILTER_START_TAG] - Tag: <and>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_START_TAG].put( new Tag( "and", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_START_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP, andFilterCreation ) );

        // State: [SEARCH_REQUEST_FILTER_LOOP] - Tag: <and>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP].put( new Tag( "and", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP,
                Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP, andFilterCreation ) );

        // State: [SEARCH_REQUEST_FILTER_LOOP] - Tag: </and>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP].put( new Tag( "and", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP,
                Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP, connectorFilterClose ) );

        //*** OR ***
        // State: [SEARCH_REQUEST_FILTER_START_TAG] - Tag: <or>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_START_TAG].put( new Tag( "or", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_START_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP, orFilterCreation ) );

        // State: [SEARCH_REQUEST_FILTER_LOOP] - Tag: <or>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP].put( new Tag( "or", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP,
                Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP, orFilterCreation ) );

        // State: [SEARCH_REQUEST_FILTER_LOOP] - Tag: </or>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP].put( new Tag( "or", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP,
                Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP, connectorFilterClose ) );

        //*** NOT ***
        // State: [SEARCH_REQUEST_FILTER_START_TAG] - Tag: <not>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_START_TAG].put( new Tag( "not", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_START_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP, notFilterCreation ) );

        // State: [SEARCH_REQUEST_FILTER_LOOP] - Tag: <not>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP].put( new Tag( "not", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP,
                Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP, notFilterCreation ) );

        // State: [SEARCH_REQUEST_FILTER_LOOP] - Tag: </not>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP].put( new Tag( "not", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP,
                Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP, connectorFilterClose ) );

        //*** SUBSTRINGS ***
        // State: [SEARCH_REQUEST_FILTER_START_TAG] - Tag: <substrings>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_START_TAG].put( new Tag( "substrings", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_START_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_SUBSTRINGS_START_TAG, substringsFilterCreation ) );

        // State: [SEARCH_REQUEST_FILTER_LOOP] - Tag: <substrings>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP].put( new Tag( "substrings", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP,
                Dsmlv2StatesEnum.SEARCH_REQUEST_SUBSTRINGS_START_TAG, substringsFilterCreation ) );

        //*** EQUALITY MATCH ***
        // State: [SEARCH_REQUEST_FILTER_START_TAG] - Tag: <equalityMatch>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_START_TAG].put( new Tag( "equalityMatch", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_START_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_EQUALITYMATCH_START_TAG, equalityMatchFilterCreation ) );

        // State: [SEARCH_REQUEST_FILTER_LOOP] - Tag: <equalityMatch>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP].put( new Tag( "equalityMatch", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP,
                Dsmlv2StatesEnum.SEARCH_REQUEST_EQUALITYMATCH_START_TAG, equalityMatchFilterCreation ) );

        // State: [SEARCH_REQUEST_EQUALITYMATCH_START_TAG] - Tag: <value>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_EQUALITYMATCH_START_TAG].put( new Tag( "value", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_EQUALITYMATCH_START_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_VALUE_END_TAG, filterAddValue ) );

        // State: [SEARCH_REQUEST_VALUE_END_TAG] - Tag: </equalityMatch>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_VALUE_END_TAG].put( new Tag( "equalityMatch", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_VALUE_END_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP, null ) );

        //*** GREATER OR EQUAL ***
        // State: [SEARCH_REQUEST_FILTER_START_TAG] - Tag: <greaterOrEqual>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_START_TAG].put(
            new Tag( "greaterOrEqual", Tag.START ), new GrammarTransition(
                Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_START_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_GREATEROREQUAL_START_TAG, greaterOrEqualFilterCreation ) );

        // State: [SEARCH_REQUEST_FILTER_LOOP] - Tag: <greaterOrEqual>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP].put( new Tag( "greaterOrEqual", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP,
                Dsmlv2StatesEnum.SEARCH_REQUEST_GREATEROREQUAL_START_TAG, greaterOrEqualFilterCreation ) );

        // State: [SEARCH_REQUEST_GREATEROREQUAL_START_TAG] - Tag: <value>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_GREATEROREQUAL_START_TAG].put( new Tag( "value", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_GREATEROREQUAL_START_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_VALUE_END_TAG, filterAddValue ) );

        // State: [SEARCH_REQUEST_VALUE_END_TAG] - Tag: </greaterOrEqual>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_VALUE_END_TAG].put( new Tag( "greaterOrEqual", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_VALUE_END_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP, null ) );

        //*** LESS OR EQUAL ***
        // State: [SEARCH_REQUEST_FILTER_START_TAG] - Tag: <lessOrEqual>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_START_TAG].put( new Tag( "lessOrEqual", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_START_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_LESSOREQUAL_START_TAG, lessOrEqualFilterCreation ) );

        // State: [SEARCH_REQUEST_FILTER_LOOP] - Tag: <lessOrEqual>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP].put( new Tag( "lessOrEqual", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP,
                Dsmlv2StatesEnum.SEARCH_REQUEST_LESSOREQUAL_START_TAG, lessOrEqualFilterCreation ) );

        // State: [SEARCH_REQUEST_LESSOREQUAL_START_TAG] - Tag: <value>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_LESSOREQUAL_START_TAG].put( new Tag( "value", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_LESSOREQUAL_START_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_VALUE_END_TAG, filterAddValue ) );

        // State: [SEARCH_REQUEST_VALUE_END_TAG] - Tag: </lessOrEqual>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_VALUE_END_TAG].put( new Tag( "lessOrEqual", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_VALUE_END_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP, null ) );

        //*** LESS OR EQUAL ***
        // State: [SEARCH_REQUEST_FILTER_START_TAG] - Tag: <approxMatch>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_START_TAG].put( new Tag( "approxMatch", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_START_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_APPROXMATCH_START_TAG, approxMatchFilterCreation ) );

        // State: [SEARCH_REQUEST_FILTER_LOOP] - Tag: <approxMatch>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP].put( new Tag( "approxMatch", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP,
                Dsmlv2StatesEnum.SEARCH_REQUEST_APPROXMATCH_START_TAG, approxMatchFilterCreation ) );

        // State: [SEARCH_REQUEST_APPROXMATCH_START_TAG] - Tag: <value>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_APPROXMATCH_START_TAG].put( new Tag( "value", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_APPROXMATCH_START_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_VALUE_END_TAG, filterAddValue ) );

        // State: [SEARCH_REQUEST_VALUE_END_TAG] - Tag: </approxMatch>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_VALUE_END_TAG].put( new Tag( "approxMatch", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_VALUE_END_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP, null ) );

        //*** PRESENT ***
        // State: [SEARCH_REQUEST_FILTER_START_TAG] - Tag: <present>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_START_TAG].put( new Tag( "present", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_START_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_PRESENT_START_TAG, presentFilterCreation ) );

        // State: [SEARCH_REQUEST_FILTER_LOOP] - Tag: <present>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP].put( new Tag( "present", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP,
                Dsmlv2StatesEnum.SEARCH_REQUEST_PRESENT_START_TAG, presentFilterCreation ) );

        // State: [SEARCH_REQUEST_PRESENT_START_TAG] - Tag: </present>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_PRESENT_START_TAG].put( new Tag( "present", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_PRESENT_START_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP, null ) );

        //*** EXTENSIBLE MATCH ***
        // State: [SEARCH_REQUEST_FILTER_START_TAG] - Tag: <extensibleMatch>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_START_TAG].put(
            new Tag( "extensibleMatch", Tag.START ), new GrammarTransition(
                Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_START_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_EXTENSIBLEMATCH_START_TAG, extensibleMatchFilterCreation ) );

        // State: [SEARCH_REQUEST_FILTER_LOOP] - Tag: <extensibleMatch>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP].put( new Tag( "extensibleMatch", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP,
                Dsmlv2StatesEnum.SEARCH_REQUEST_EXTENSIBLEMATCH_START_TAG, extensibleMatchFilterCreation ) );

        // State: [SEARCH_REQUEST_EXTENSIBLEMATCH_START_TAG] - Tag: <value>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_EXTENSIBLEMATCH_START_TAG].put(
            new Tag( "value", Tag.START ), new GrammarTransition(
                Dsmlv2StatesEnum.SEARCH_REQUEST_EXTENSIBLEMATCH_START_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_EXTENSIBLEMATCH_VALUE_END_TAG, extensibleMatchAddValue ) );

        // State: [SEARCH_REQUEST_EXTENSIBLEMATCH_VALUE_END_TAG] - Tag: </extensibleMatch>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_EXTENSIBLEMATCH_VALUE_END_TAG].put( new Tag(
            "extensibleMatch", Tag.END ), new GrammarTransition(
            Dsmlv2StatesEnum.SEARCH_REQUEST_EXTENSIBLEMATCH_VALUE_END_TAG, Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP,
            null ) );

        //*** Filter (end) ***
        // State: [SEARCH_REQUEST_FILTER_LOOP] - Tag: </filter>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP].put( new Tag( "filter", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP,
                Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_END_TAG, null ) );

        // State: [SEARCH_REQUEST_FILTER_END_TAG] - Tag: <attributes>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_END_TAG].put( new Tag( "attributes", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_END_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_ATTRIBUTES_START_TAG, null ) );

        // State: [SEARCH_REQUEST_FILTER_END_TAG] - Tag: </searchRequest>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_END_TAG].put( new Tag( "searchRequest", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_END_TAG, Dsmlv2StatesEnum.BATCHREQUEST_LOOP,
                null ) );

        //====================================================
        //  Transitions concerning : SUBSTRING FILTER
        //====================================================
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_SUBSTRINGS_START_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_INITIAL_END_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_ANY_END_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_FINAL_END_TAG] = new HashMap<Tag, GrammarTransition>();
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_SUBSTRINGS_END_TAG] = new HashMap<Tag, GrammarTransition>();

        // State: [SEARCH_REQUEST_SUBSTRINGS_START_TAG] - Tag: </substrings>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_SUBSTRINGS_START_TAG].put( new Tag( "substrings", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_SUBSTRINGS_START_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP, null ) );

        // State: [SEARCH_REQUEST_SUBSTRINGS_START_TAG] - Tag: <initial>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_SUBSTRINGS_START_TAG].put( new Tag( "initial", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_SUBSTRINGS_START_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_INITIAL_END_TAG, substringsFilterSetInitial ) );

        // State: [SEARCH_REQUEST_INITIAL_END_TAG] - Tag: <any>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_INITIAL_END_TAG].put( new Tag( "any", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_INITIAL_END_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_ANY_END_TAG, substringsFilterAddAny ) );

        // State: [SEARCH_REQUEST_INITIAL_END_TAG] - Tag: <final>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_INITIAL_END_TAG].put( new Tag( "final", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_INITIAL_END_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_FINAL_END_TAG, substringsFilterSetFinal ) );

        // State: [SEARCH_REQUEST_INITIAL_END_TAG] - Tag: </substrings>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_INITIAL_END_TAG].put( new Tag( "substrings", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_INITIAL_END_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP, substringsFilterClose ) );

        // State: [SEARCH_REQUEST_SUBSTRINGS_START_TAG] - Tag: <any>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_SUBSTRINGS_START_TAG].put( new Tag( "any", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_SUBSTRINGS_START_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_ANY_END_TAG, substringsFilterAddAny ) );

        // State: [SEARCH_REQUEST_ANY_END_TAG] - Tag: </any>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_ANY_END_TAG].put( new Tag( "any", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_ANY_END_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_ANY_END_TAG, substringsFilterAddAny ) );

        // State: [SEARCH_REQUEST_ANY_END_TAG] - Tag: <final>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_ANY_END_TAG].put( new Tag( "final", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_ANY_END_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_FINAL_END_TAG, substringsFilterSetFinal ) );

        // State: [SEARCH_REQUEST_ANY_END_TAG] - Tag: </substrings>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_ANY_END_TAG].put( new Tag( "substrings", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_ANY_END_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP, substringsFilterClose ) );

        // State: [SEARCH_REQUEST_SUBSTRINGS_START_TAG] - Tag: <final>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_SUBSTRINGS_START_TAG].put( new Tag( "final", Tag.START ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_SUBSTRINGS_START_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_FINAL_END_TAG, substringsFilterSetFinal ) );

        // State: [SEARCH_REQUEST_FINAL_END_TAG] - Tag: </substrings>
        super.transitions[Dsmlv2StatesEnum.SEARCH_REQUEST_FINAL_END_TAG].put( new Tag( "substrings", Tag.END ),
            new GrammarTransition( Dsmlv2StatesEnum.SEARCH_REQUEST_FINAL_END_TAG,
                Dsmlv2StatesEnum.SEARCH_REQUEST_FILTER_LOOP, substringsFilterClose ) );

    } // End of the constructor

    //*************************
    //*    GRAMMAR ACTIONS    *
    //*************************

    /**
     * GrammarAction that creates an Abandon Request
     */
    private final GrammarAction batchRequestCreation = new GrammarAction( "Create Batch Request" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            BatchRequest batchRequest = new BatchRequest();

            container.setBatchRequest( batchRequest );

            XmlPullParser xpp = container.getParser();

            // Checking and adding the batchRequest's attributes
            String attributeValue;
            // requestID
            attributeValue = xpp.getAttributeValue( "", "requestID" );
            if ( attributeValue != null )
            {
                batchRequest.setRequestID( ParserUtils.parseAndVerifyRequestID( attributeValue, xpp ) );
            }
            // processing
            attributeValue = xpp.getAttributeValue( "", "processing" );
            if ( attributeValue != null )
            {
                if ( "sequential".equals( attributeValue ) )
                {
                    batchRequest.setProcessing( Processing.SEQUENTIAL );
                }
                else if ( "parallel".equals( attributeValue ) )
                {
                    batchRequest.setProcessing( Processing.PARALLEL );
                }
                else
                {
                    throw new XmlPullParserException( "Unknown value for 'processing' attribute.", xpp, null );
                }
            }
            else
            {
                batchRequest.setProcessing( Processing.SEQUENTIAL );
            }
            // onError
            attributeValue = xpp.getAttributeValue( "", "onError" );
            if ( attributeValue != null )
            {
                if ( "resume".equals( attributeValue ) )
                {
                    batchRequest.setOnError( OnError.RESUME );
                }
                else if ( "exit".equals( attributeValue ) )
                {
                    batchRequest.setOnError( OnError.EXIT );
                }
                else
                {
                    throw new XmlPullParserException( "Unknown value for 'onError' attribute.", xpp, null );
                }
            }
            else
            {
                batchRequest.setOnError( OnError.EXIT );
            }
            // responseOrder
            attributeValue = xpp.getAttributeValue( "", "responseOrder" );
            if ( attributeValue != null )
            {
                if ( "sequential".equals( attributeValue ) )
                {
                    batchRequest.setResponseOrder( ResponseOrder.SEQUENTIAL );
                }
                else if ( "unordered".equals( attributeValue ) )
                {
                    batchRequest.setResponseOrder( ResponseOrder.UNORDERED );
                }
                else
                {
                    throw new XmlPullParserException( "Unknown value for 'responseOrder' attribute.", xpp, null );
                }
            }
            else
            {
                batchRequest.setResponseOrder( ResponseOrder.SEQUENTIAL );
            }
        }
    };

    /**
     * GrammarAction that creates an Abandon Request
     */
    private final GrammarAction abandonRequestCreation = new GrammarAction( "Create Abandon Request" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            AbandonRequestCodec abandonRequest = new AbandonRequestCodec();
            container.getBatchRequest().addRequest( abandonRequest );

            XmlPullParser xpp = container.getParser();

            // Checking and adding the request's attributes
            String attributeValue;
            // requestID
            attributeValue = xpp.getAttributeValue( "", "requestID" );
            if ( attributeValue != null )
            {
                abandonRequest.setMessageId( ParserUtils.parseAndVerifyRequestID( attributeValue, xpp ) );
            }
            else
            {
                if ( ParserUtils.isRequestIdNeeded( container ) )
                {
                    throw new XmlPullParserException( "requestID attribute is required", xpp, null );
                }
            }
            // abandonID
            attributeValue = xpp.getAttributeValue( "", "abandonID" );
            if ( attributeValue != null )
            {
                try
                {
                    abandonRequest.setAbandonedMessageId( Integer.parseInt( attributeValue ) );
                }
                catch ( NumberFormatException e )
                {
                    throw new XmlPullParserException( "the given abandonID is not an integer", xpp, null );
                }
            }
            else
            {
                throw new XmlPullParserException( "abandonID attribute is required", xpp, null );
            }
        }
    };

    /**
     * GrammarAction that creates an Add Request
     */
    private final GrammarAction addRequestCreation = new GrammarAction( "Create Add Request" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            AddRequestCodec addRequest = new AddRequestCodec();
            container.getBatchRequest().addRequest( addRequest );
            addRequest.initEntry(); // TODO maybe delay that to the first attribute discovery

            XmlPullParser xpp = container.getParser();

            // Checking and adding the request's attributes
            String attributeValue;
            // requestID
            attributeValue = xpp.getAttributeValue( "", "requestID" );
            if ( attributeValue != null )
            {
                addRequest.setMessageId( ParserUtils.parseAndVerifyRequestID( attributeValue, xpp ) );
            }
            else
            {
                if ( ParserUtils.isRequestIdNeeded( container ) )
                {
                    throw new XmlPullParserException( "requestID attribute is required", xpp, null );
                }
            }
            // dn
            attributeValue = xpp.getAttributeValue( "", "dn" );
            if ( attributeValue != null )
            {
                try
                {
                    addRequest.setEntryDn( new LdapDN( attributeValue ) );
                }
                catch ( InvalidNameException e )
                {
                    throw new XmlPullParserException( "" + e.getMessage(), xpp, null );
                }
            }
            else
            {
                throw new XmlPullParserException( "dn attribute is required", xpp, null );
            }
        }
    };

    /**
     * GrammarAction that adds an attribute to an Add Request
     */
    private final GrammarAction addRequestAddAttribute = new GrammarAction( "Add Attribute to Add Request" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            AddRequestCodec addRequest = ( AddRequestCodec ) container.getBatchRequest().getCurrentRequest();

            XmlPullParser xpp = container.getParser();

            // Checking and adding the request's attributes
            String attributeValue;
            // name
            attributeValue = xpp.getAttributeValue( "", "name" );
            if ( attributeValue != null )
            {
                try
                {
                    addRequest.addAttributeType( attributeValue );
                }
                catch ( NamingException e )
                {
                    throw new XmlPullParserException( "can not add attribute value", xpp, e );
                }
            }
            else
            {
                throw new XmlPullParserException( "name attribute is required", xpp, null );
            }
        }
    };

    /**
     * GrammarAction that adds a Value to an Attribute of an Add Request
     */
    private final GrammarAction addRequestAddValue = new GrammarAction( "Add Value to Attribute" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            AddRequestCodec addRequest = ( AddRequestCodec ) container.getBatchRequest().getCurrentRequest();

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
                        addRequest.addAttributeValue( Base64.decode( nextText.trim().toCharArray() ) );
                    }
                    else
                    {
                        addRequest.addAttributeValue( nextText.trim() );
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
     * GrammarAction that creates an Auth Request
     */
    private final GrammarAction authRequestCreation = new GrammarAction( "Create Auth Request" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            BindRequestCodec authRequest = new BindRequestCodec();
            container.getBatchRequest().addRequest( authRequest );

            SimpleAuthentication simpleAuthentication = new SimpleAuthentication();
            simpleAuthentication.setSimple( StringTools.EMPTY_BYTES );
            authRequest.setAuthentication( simpleAuthentication );
            authRequest.setVersion( 3 );

            XmlPullParser xpp = container.getParser();

            // Checking and adding the request's attributes
            String attributeValue;
            // requestID
            attributeValue = xpp.getAttributeValue( "", "requestID" );
            if ( attributeValue != null )
            {
                authRequest.setMessageId( ParserUtils.parseAndVerifyRequestID( attributeValue, xpp ) );
            }
            else
            {
                if ( ParserUtils.isRequestIdNeeded( container ) )
                {
                    throw new XmlPullParserException( "requestID attribute is required", xpp, null );
                }
            }
            // principal
            attributeValue = xpp.getAttributeValue( "", "principal" );
            if ( attributeValue != null )
            {
                try
                {
                    authRequest.setName( new LdapDN( attributeValue ) );
                }
                catch ( InvalidNameException e )
                {
                    throw new XmlPullParserException( "" + e.getMessage(), xpp, null );
                }
            }
            else
            {
                throw new XmlPullParserException( "principal attribute is required", xpp, null );
            }
        }
    };

    /**
     * GrammarAction that creates an Compare Request
     */
    private final GrammarAction compareRequestCreation = new GrammarAction( "Create Compare Request" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            CompareRequestCodec compareRequest = new CompareRequestCodec();
            container.getBatchRequest().addRequest( compareRequest );

            XmlPullParser xpp = container.getParser();

            // Checking and adding the request's attributes
            String attributeValue;
            // requestID
            attributeValue = xpp.getAttributeValue( "", "requestID" );
            if ( attributeValue != null )
            {
                compareRequest.setMessageId( ParserUtils.parseAndVerifyRequestID( attributeValue, xpp ) );
            }
            else
            {
                if ( ParserUtils.isRequestIdNeeded( container ) )
                {
                    throw new XmlPullParserException( "requestID attribute is required", xpp, null );
                }
            }
            // dn
            attributeValue = xpp.getAttributeValue( "", "dn" );
            if ( attributeValue != null )
            {
                try
                {
                    compareRequest.setEntry( new LdapDN( attributeValue ) );
                }
                catch ( InvalidNameException e )
                {
                    throw new XmlPullParserException( "" + e.getMessage(), xpp, null );
                }
            }
            else
            {
                throw new XmlPullParserException( "dn attribute is required", xpp, null );
            }
        }
    };

    /**
     * GrammarAction that adds an Assertion to a Compare Request
     */
    private final GrammarAction compareRequestAddAssertion = new GrammarAction( "Add Assertion to Compare Request" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            CompareRequestCodec compareRequest = ( CompareRequestCodec ) container.getBatchRequest().getCurrentRequest();

            XmlPullParser xpp = container.getParser();

            // Checking and adding the request's attributes
            String attributeValue;
            // name
            attributeValue = xpp.getAttributeValue( "", "name" );
            if ( attributeValue != null )
            {
                compareRequest.setAttributeDesc( attributeValue );

            }
            else
            {
                throw new XmlPullParserException( "name attribute is required", xpp, null );
            }
        }
    };

    /**
     * GrammarAction that adds a Value to a Compare Request
     */
    private final GrammarAction compareRequestAddValue = new GrammarAction( "Add Value to Compare Request" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            CompareRequestCodec compareRequest = ( CompareRequestCodec ) container.getBatchRequest().getCurrentRequest();

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
                        compareRequest.setAssertionValue( Base64.decode( nextText.trim().toCharArray() ) );
                    }
                    else
                    {

                        compareRequest.setAssertionValue( nextText.trim() );
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
     * GrammarAction that creates a Del Request
     */
    private final GrammarAction delRequestCreation = new GrammarAction( "Create Del Request" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            DelRequestCodec delRequest = new DelRequestCodec();
            container.getBatchRequest().addRequest( delRequest );

            XmlPullParser xpp = container.getParser();

            // Checking and adding the request's attributes
            String attributeValue;
            // requestID
            attributeValue = xpp.getAttributeValue( "", "requestID" );
            if ( attributeValue != null )
            {
                delRequest.setMessageId( ParserUtils.parseAndVerifyRequestID( attributeValue, xpp ) );
            }
            else
            {
                if ( ParserUtils.isRequestIdNeeded( container ) )
                {
                    throw new XmlPullParserException( "requestID attribute is required", xpp, null );
                }
            }
            // dn
            attributeValue = xpp.getAttributeValue( "", "dn" );
            if ( attributeValue != null )
            {
                try
                {
                    delRequest.setEntry( new LdapDN( attributeValue ) );
                }
                catch ( InvalidNameException e )
                {
                    throw new XmlPullParserException( "" + e.getMessage(), xpp, null );
                }
            }
            else
            {
                throw new XmlPullParserException( "dn attribute is required", xpp, null );
            }
        }
    };

    /**
     * GrammarAction that creates an Extended Request
     */
    private final GrammarAction extendedRequestCreation = new GrammarAction( "Create Extended Request" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            ExtendedRequestCodec extendedRequest = new ExtendedRequestCodec();
            container.getBatchRequest().addRequest( extendedRequest );

            XmlPullParser xpp = container.getParser();

            // Checking and adding the request's attributes
            String attributeValue;
            // requestID
            attributeValue = xpp.getAttributeValue( "", "requestID" );
            if ( attributeValue != null )
            {
                extendedRequest.setMessageId( ParserUtils.parseAndVerifyRequestID( attributeValue, xpp ) );
            }
            else
            {
                if ( ParserUtils.isRequestIdNeeded( container ) )
                {
                    throw new XmlPullParserException( "requestID attribute is required", xpp, null );
                }
            }
        }
    };

    /**
     * GrammarAction that adds a Name to an Extended Request
     */
    private final GrammarAction extendedRequestAddName = new GrammarAction( "Add Name to Extended Request" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            ExtendedRequestCodec extendedRequest = ( ExtendedRequestCodec ) container.getBatchRequest().getCurrentRequest();

            XmlPullParser xpp = container.getParser();
            try
            {
                String nextText = xpp.nextText();
                if ( nextText.equals( "" ) )
                {
                    throw new XmlPullParserException( "The request name can't be null", xpp, null );
                }
                else
                {
                    extendedRequest.setRequestName( new OID( nextText.trim() ) );
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
     * GrammarAction that adds a Value to an Extended Request
     */
    private final GrammarAction extendedRequestAddValue = new GrammarAction( "Add Value to Extended Request" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            ExtendedRequestCodec extendedRequest = ( ExtendedRequestCodec ) container.getBatchRequest().getCurrentRequest();

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
                        extendedRequest.setRequestValue( Base64.decode( nextText.trim().toCharArray() ) );
                    }
                    else
                    {
                        extendedRequest.setRequestValue( nextText.trim().getBytes() );
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
     * GrammarAction that creates a Modify DN Request
     */
    private final GrammarAction modDNRequestCreation = new GrammarAction( "Create Modify DN Request" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            ModifyDNRequestCodec modifyDNRequest = new ModifyDNRequestCodec();
            container.getBatchRequest().addRequest( modifyDNRequest );

            XmlPullParser xpp = container.getParser();

            // Checking and adding the request's attributes
            String attributeValue;
            // requestID
            attributeValue = xpp.getAttributeValue( "", "requestID" );
            if ( attributeValue != null )
            {
                modifyDNRequest.setMessageId( ParserUtils.parseAndVerifyRequestID( attributeValue, xpp ) );
            }
            else
            {
                if ( ParserUtils.isRequestIdNeeded( container ) )
                {
                    throw new XmlPullParserException( "requestID attribute is required", xpp, null );
                }
            }
            // dn
            attributeValue = xpp.getAttributeValue( "", "dn" );
            if ( attributeValue != null )
            {
                try
                {
                    modifyDNRequest.setEntry( new LdapDN( attributeValue ) );
                }
                catch ( InvalidNameException e )
                {
                    throw new XmlPullParserException( "" + e.getMessage(), xpp, null );
                }
            }
            else
            {
                throw new XmlPullParserException( "dn attribute is required", xpp, null );
            }
            // newrdn
            attributeValue = xpp.getAttributeValue( "", "newrdn" );
            if ( attributeValue != null )
            {
                try
                {
                    modifyDNRequest.setNewRDN( new Rdn( attributeValue ) );
                }
                catch ( InvalidNameException e )
                {
                    throw new XmlPullParserException( "" + e.getMessage(), xpp, null );
                }
            }
            else
            {
                throw new XmlPullParserException( "newrdn attribute is required", xpp, null );
            }
            // deleteoldrdn
            attributeValue = xpp.getAttributeValue( "", "deleteoldrdn" );
            if ( attributeValue != null )
            {
                if ( ( attributeValue.equals( "true" ) ) || ( attributeValue.equals( "1" ) ) )
                {
                    modifyDNRequest.setDeleteOldRDN( true );
                }
                else if ( ( attributeValue.equals( "false" ) ) || ( attributeValue.equals( "0" ) ) )
                {
                    modifyDNRequest.setDeleteOldRDN( false );
                }
                else
                {
                    throw new XmlPullParserException( "Incorrect value for 'deleteoldrdn' attribute", xpp, null );
                }
            }
            else
            {
                modifyDNRequest.setDeleteOldRDN( true );
            }
            // newsuperior
            attributeValue = xpp.getAttributeValue( "", "newSuperior" );
            if ( attributeValue != null )
            {
                try
                {
                    modifyDNRequest.setNewSuperior( new LdapDN( attributeValue ) );
                }
                catch ( InvalidNameException e )
                {
                    throw new XmlPullParserException( "" + e.getMessage(), xpp, null );
                }
            }
        }
    };

    /**
     * GrammarAction that creates a Modify Request
     */
    private final GrammarAction modifyRequestCreation = new GrammarAction( "Create Modify Request" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            ModifyRequestCodec modifyRequest = new ModifyRequestCodec();
            container.getBatchRequest().addRequest( modifyRequest );

            modifyRequest.initModifications();

            XmlPullParser xpp = container.getParser();

            // Checking and adding the request's attributes
            String attributeValue;
            // requestID
            attributeValue = xpp.getAttributeValue( "", "requestID" );
            if ( attributeValue != null )
            {
                modifyRequest.setMessageId( ParserUtils.parseAndVerifyRequestID( attributeValue, xpp ) );
            }
            else
            {
                if ( ParserUtils.isRequestIdNeeded( container ) )
                {
                    throw new XmlPullParserException( "requestID attribute is required", xpp, null );
                }
            }
            // dn
            attributeValue = xpp.getAttributeValue( "", "dn" );
            if ( attributeValue != null )
            {
                try
                {
                    modifyRequest.setObject( new LdapDN( attributeValue ) );
                }
                catch ( InvalidNameException e )
                {
                    throw new XmlPullParserException( "" + e.getMessage(), xpp, null );
                }
            }
            else
            {
                throw new XmlPullParserException( "dn attribute is required", xpp, null );
            }
        }
    };

    /**
     * GrammarAction that adds a Modification to a Modify Request
     */
    private final GrammarAction modifyRequestAddModification = new GrammarAction( "Adds Modification to Modify Request" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            ModifyRequestCodec modifyRequest = ( ModifyRequestCodec ) container.getBatchRequest().getCurrentRequest();

            XmlPullParser xpp = container.getParser();

            // Checking and adding the request's attributes
            String attributeValue;
            // operation
            attributeValue = xpp.getAttributeValue( "", "operation" );
            if ( attributeValue != null )
            {
                if ( "add".equals( attributeValue ) )
                {
                    modifyRequest.setCurrentOperation( LdapConstants.OPERATION_ADD );
                }
                else if ( "delete".equals( attributeValue ) )
                {
                    modifyRequest.setCurrentOperation( LdapConstants.OPERATION_DELETE );
                }
                else if ( "replace".equals( attributeValue ) )
                {
                    modifyRequest.setCurrentOperation( LdapConstants.OPERATION_REPLACE );
                }
                else
                {
                    throw new XmlPullParserException(
                        "unknown operation. Operation can be 'add', 'delete' or 'replace'.", xpp, null );
                }
            }
            else
            {
                throw new XmlPullParserException( "operation attribute is required", xpp, null );
            }
            // name
            attributeValue = xpp.getAttributeValue( "", "name" );
            if ( attributeValue != null )
            {
                modifyRequest.addAttributeTypeAndValues( attributeValue );
            }
            else
            {
                throw new XmlPullParserException( "name attribute is required", xpp, null );
            }
        }
    };

    /**
     * GrammarAction that adds a Value to a Modification of a Modify Request
     */
    private final GrammarAction modifyRequestAddValue = new GrammarAction(
        "Add Value to Modification of Modify Request" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            ModifyRequestCodec modifyRequest = ( ModifyRequestCodec ) container.getBatchRequest().getCurrentRequest();

            XmlPullParser xpp = container.getParser();
            try
            {
                // We have to catch the type Attribute Value before going to the next Text node
                String typeValue = ParserUtils.getXsiTypeAttributeValue( xpp );

                // Getting the value
                String nextText = xpp.nextText();
                // We are testing if nextText equals "" since a modification can be "".

                if ( ParserUtils.isBase64BinaryValue( xpp, typeValue ) )
                {
                    modifyRequest.addAttributeValue( Base64.decode( nextText.trim().toCharArray() ) );
                }
                else
                {
                    modifyRequest.addAttributeValue( nextText.trim() );
                }
            }
            catch ( IOException e )
            {
                throw new XmlPullParserException( "An unexpected error ocurred : " + e.getMessage(), xpp, null );
            }
        }
    };

    /**
     * GrammarAction that creates a Search Request
     */
    private final GrammarAction searchRequestCreation = new GrammarAction( "Create Search Request" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            SearchRequestCodec searchRequest = new SearchRequestCodec();
            container.getBatchRequest().addRequest( searchRequest );

            XmlPullParser xpp = container.getParser();

            // Checking and adding the request's attributes
            String attributeValue;
            // requestID
            attributeValue = xpp.getAttributeValue( "", "requestID" );
            if ( attributeValue != null )
            {
                searchRequest.setMessageId( ParserUtils.parseAndVerifyRequestID( attributeValue, xpp ) );
            }
            else
            {
                if ( ParserUtils.isRequestIdNeeded( container ) )
                {
                    throw new XmlPullParserException( "requestID attribute is required", xpp, null );
                }
            }
            // dn
            attributeValue = xpp.getAttributeValue( "", "dn" );
            if ( attributeValue != null )
            {
                try
                {
                    searchRequest.setBaseObject( new LdapDN( attributeValue ) );
                }
                catch ( InvalidNameException e )
                {
                    throw new XmlPullParserException( "" + e.getMessage(), xpp, null );
                }
            }
            else
            {
                throw new XmlPullParserException( "dn attribute is required", xpp, null );
            }
            // scope
            attributeValue = xpp.getAttributeValue( "", "scope" );
            if ( attributeValue != null )
            {
                if ( "baseObject".equals( attributeValue ) )
                {
                    searchRequest.setScope( SearchScope.OBJECT );
                }
                else if ( "singleLevel".equals( attributeValue ) )
                {
                    searchRequest.setScope( SearchScope.ONELEVEL );
                }
                else if ( "wholeSubtree".equals( attributeValue ) )
                {
                    searchRequest.setScope( SearchScope.SUBTREE );
                }
                else
                {
                    throw new XmlPullParserException(
                        "unknown scope. Scope must be 'baseObject', 'singleLevel' or 'wholeSubtree'.", xpp, null );
                }
            }
            else
            {
                throw new XmlPullParserException( "scope attribute is required", xpp, null );
            }
            // derefAliases
            attributeValue = xpp.getAttributeValue( "", "derefAliases" );
            if ( attributeValue != null )
            {
                if ( "neverDerefAliases".equals( attributeValue ) )
                {
                    searchRequest.setDerefAliases( LdapConstants.NEVER_DEREF_ALIASES );
                }
                else if ( "derefInSearching".equals( attributeValue ) )
                {
                    searchRequest.setDerefAliases( LdapConstants.DEREF_IN_SEARCHING );
                }
                else if ( "derefFindingBaseObj".equals( attributeValue ) )
                {
                    searchRequest.setDerefAliases( LdapConstants.DEREF_FINDING_BASE_OBJ );
                }
                else if ( "derefAlways".equals( attributeValue ) )
                {
                    searchRequest.setDerefAliases( LdapConstants.DEREF_ALWAYS );
                }
                else
                {
                    throw new XmlPullParserException(
                        "unknown derefAliases value. derefAliases must be 'neverDerefAliases', 'derefInSearching', 'derefFindingBaseObj' or 'derefAlways'.",
                        xpp, null );
                }
            }
            else
            {
                throw new XmlPullParserException( "derefAliases attribute is required", xpp, null );
            }
            // sizeLimit
            attributeValue = xpp.getAttributeValue( "", "sizeLimit" );
            if ( attributeValue != null )
            {
                try
                {
                    searchRequest.setSizeLimit( Integer.parseInt( attributeValue ) );
                }
                catch ( NumberFormatException e )
                {
                    throw new XmlPullParserException( "the given sizeLimit is not an integer", xpp, null );
                }
            }
            else
            {
                searchRequest.setSizeLimit( 0 );
            }
            // timeLimit
            attributeValue = xpp.getAttributeValue( "", "timeLimit" );
            if ( attributeValue != null )
            {
                try
                {
                    searchRequest.setTimeLimit( Integer.parseInt( attributeValue ) );
                }
                catch ( NumberFormatException e )
                {
                    throw new XmlPullParserException( "the given timeLimit is not an integer", xpp, null );
                }
            }
            else
            {
                searchRequest.setTimeLimit( 0 );
            }
            // typesOnly
            attributeValue = xpp.getAttributeValue( "", "typesOnly" );
            if ( attributeValue != null )
            {
                if ( ( attributeValue.equals( "true" ) ) || ( attributeValue.equals( "1" ) ) )
                {
                    searchRequest.setTypesOnly( true );
                }
                else if ( ( attributeValue.equals( "false" ) ) || ( attributeValue.equals( "0" ) ) )
                {
                    searchRequest.setTypesOnly( false );
                }
                else
                {
                    throw new XmlPullParserException( "typesOnly must be a boolean ('true' or 'false').", xpp, null );
                }
            }
            else
            {
                searchRequest.setTypesOnly( false );
            }
        }
    };

    /**
     * GrammarAction that adds an Attribute to a Search Request
     */
    private final GrammarAction searchRequestAddAttribute = new GrammarAction(
        "Add Value to Modification of Modify Request" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            SearchRequestCodec searchRequest = ( SearchRequestCodec ) container.getBatchRequest().getCurrentRequest();

            XmlPullParser xpp = container.getParser();

            // Checking and adding the request's attributes
            String attributeValue;
            // name
            attributeValue = xpp.getAttributeValue( "", "name" );
            if ( attributeValue != null )
            {
                searchRequest.addAttribute( attributeValue );
            }
            else
            {
                throw new XmlPullParserException( "name attribute is required", xpp, null );
            }
        }
    };

    /**
     * GrammarAction that create a Substring Filter
     */
    private final GrammarAction substringsFilterCreation = new GrammarAction( "Create Substring Filter" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            SearchRequestCodec searchRequest = ( SearchRequestCodec ) container.getBatchRequest().getCurrentRequest();

            XmlPullParser xpp = container.getParser();

            SubstringFilter filter = new SubstringFilter();

            // Adding the filter to the Search Filter
            try
            {
                searchRequest.addCurrentFilter( filter );
            }
            catch ( DecoderException e )
            {
                throw new XmlPullParserException( e.getMessage(), xpp, null );
            }
            searchRequest.setTerminalFilter( filter );

            // Checking and adding the filter's attributes
            String attributeValue;
            // name
            attributeValue = xpp.getAttributeValue( "", "name" );
            if ( attributeValue != null )
            {
                filter.setType( attributeValue );
            }
            else
            {
                throw new XmlPullParserException( "name attribute is required", xpp, null );
            }
        }
    };

    /**
     * GrammarAction that sets the Initial value to a Substring Filter
     */
    private final GrammarAction substringsFilterSetInitial = new GrammarAction( "Set Initial value to Substring Filter" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            SearchRequestCodec searchRequest = ( SearchRequestCodec ) container.getBatchRequest().getCurrentRequest();
            SubstringFilter substringFilter = ( SubstringFilter ) searchRequest.getTerminalFilter();

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
                        substringFilter
                            .setInitialSubstrings( new String( Base64.decode( nextText.trim().toCharArray() ) ) );
                    }
                    else
                    {
                        substringFilter.setInitialSubstrings( nextText.trim() );
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
     * GrammarAction that adds a Any value to a Substring Filter
     */
    private final GrammarAction substringsFilterAddAny = new GrammarAction( "Add Any value to Substring Filter" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            SearchRequestCodec searchRequest = ( SearchRequestCodec ) container.getBatchRequest().getCurrentRequest();
            SubstringFilter substringFilter = ( SubstringFilter ) searchRequest.getTerminalFilter();

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
                        substringFilter.addAnySubstrings( new String( Base64.decode( nextText.trim().toCharArray() ) ) );
                    }
                    else
                    {
                        substringFilter.addAnySubstrings( nextText.trim() );
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
     * GrammarAction that sets the Final value to a Substring Filter
     */
    private final GrammarAction substringsFilterSetFinal = new GrammarAction( "Set Final value to Substring Filter" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            SearchRequestCodec searchRequest = ( SearchRequestCodec ) container.getBatchRequest().getCurrentRequest();
            SubstringFilter substringFilter = ( SubstringFilter ) searchRequest.getTerminalFilter();

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
                        substringFilter
                            .setFinalSubstrings( new String( Base64.decode( nextText.trim().toCharArray() ) ) );
                    }
                    else
                    {
                        substringFilter.setFinalSubstrings( nextText.trim() );
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
     * GrammarAction that closes a Substring Filter
     */
    private final GrammarAction substringsFilterClose = new GrammarAction( "Close Substring Filter" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            SearchRequestCodec searchRequest = ( SearchRequestCodec ) container.getBatchRequest().getCurrentRequest();

            searchRequest.setTerminalFilter( null );
        }
    };

    /**
     * GrammarAction that create a And Filter
     */
    private final GrammarAction andFilterCreation = new GrammarAction( "Create And Filter" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            SearchRequestCodec searchRequest = ( SearchRequestCodec ) container.getBatchRequest().getCurrentRequest();

            XmlPullParser xpp = container.getParser();

            AndFilter filter = new AndFilter();

            // Adding the filter to the Search Filter
            try
            {
                searchRequest.addCurrentFilter( filter );
            }
            catch ( DecoderException e )
            {
                throw new XmlPullParserException( e.getMessage(), xpp, null );
            }
        }
    };

    /**
     * GrammarAction that closes a Connector Filter (And, Or, Not)
     */
    private final GrammarAction connectorFilterClose = new GrammarAction( "Close Connector Filter" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            SearchRequestCodec searchRequest = ( SearchRequestCodec ) container.getBatchRequest().getCurrentRequest();

            Asn1Object parent = searchRequest.getCurrentFilter().getParent();

            if ( parent instanceof Filter )
            {
                Filter filter = ( Filter ) parent;

                searchRequest.setCurrentFilter( filter );
            }
            else
            {
                searchRequest.setCurrentFilter( null );
            }

        }
    };

    /**
     * GrammarAction that create a Or Filter
     */
    private final GrammarAction orFilterCreation = new GrammarAction( "Create Or Filter" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            SearchRequestCodec searchRequest = ( SearchRequestCodec ) container.getBatchRequest().getCurrentRequest();

            XmlPullParser xpp = container.getParser();

            OrFilter filter = new OrFilter();

            // Adding the filter to the Search Filter
            try
            {
                searchRequest.addCurrentFilter( filter );
            }
            catch ( DecoderException e )
            {
                throw new XmlPullParserException( e.getMessage(), xpp, null );
            }
        }
    };

    /**
     * GrammarAction that create a Not Filter
     */
    private final GrammarAction notFilterCreation = new GrammarAction( "Create Not Filter" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            SearchRequestCodec searchRequest = ( SearchRequestCodec ) container.getBatchRequest().getCurrentRequest();

            XmlPullParser xpp = container.getParser();

            NotFilter filter = new NotFilter();

            // Adding the filter to the Search Filter
            try
            {
                searchRequest.addCurrentFilter( filter );
            }
            catch ( DecoderException e )
            {
                throw new XmlPullParserException( e.getMessage(), xpp, null );
            }
        }
    };

    /**
     * GrammarAction that create a Equality Match Filter
     */
    private final GrammarAction equalityMatchFilterCreation = new GrammarAction( "Create Equality Match Filter" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            SearchRequestCodec searchRequest = ( SearchRequestCodec ) container.getBatchRequest().getCurrentRequest();

            XmlPullParser xpp = container.getParser();

            AttributeValueAssertion assertion = new AttributeValueAssertion();

            // Checking and adding the filter's attributes
            String attributeValue;
            // name
            attributeValue = xpp.getAttributeValue( "", "name" );
            if ( attributeValue != null )
            {
                assertion.setAttributeDesc( new String( attributeValue.getBytes() ) );
            }
            else
            {
                throw new XmlPullParserException( "name attribute is required", xpp, null );
            }

            AttributeValueAssertionFilter filter = new AttributeValueAssertionFilter(
                LdapConstants.EQUALITY_MATCH_FILTER );

            filter.setAssertion( assertion );

            // Adding the filter to the Search Filter
            try
            {
                searchRequest.addCurrentFilter( filter );
            }
            catch ( DecoderException e )
            {
                throw new XmlPullParserException( e.getMessage(), xpp, null );
            }
            searchRequest.setTerminalFilter( filter );
        }
    };

    /**
     * GrammarAction that create a Greater Or Equal Filter
     */
    private final GrammarAction greaterOrEqualFilterCreation = new GrammarAction( "Create Greater Or Equal Filter" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            SearchRequestCodec searchRequest = ( SearchRequestCodec ) container.getBatchRequest().getCurrentRequest();

            XmlPullParser xpp = container.getParser();

            AttributeValueAssertion assertion = new AttributeValueAssertion();

            // Checking and adding the filter's attributes
            String attributeValue;
            // name
            attributeValue = xpp.getAttributeValue( "", "name" );
            if ( attributeValue != null )
            {
                assertion.setAttributeDesc( new String( attributeValue.getBytes() ) );
            }
            else
            {
                throw new XmlPullParserException( "name attribute is required", xpp, null );
            }

            AttributeValueAssertionFilter filter = new AttributeValueAssertionFilter(
                LdapConstants.GREATER_OR_EQUAL_FILTER );

            filter.setAssertion( assertion );

            // Adding the filter to the Search Filter
            try
            {
                searchRequest.addCurrentFilter( filter );
            }
            catch ( DecoderException e )
            {
                throw new XmlPullParserException( e.getMessage(), xpp, null );
            }
            searchRequest.setTerminalFilter( filter );
        }
    };

    /**
     * GrammarAction that create a Less Or Equal Filter
     */
    private final GrammarAction lessOrEqualFilterCreation = new GrammarAction( "Create Less Or Equal Filter" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            SearchRequestCodec searchRequest = ( SearchRequestCodec ) container.getBatchRequest().getCurrentRequest();

            XmlPullParser xpp = container.getParser();

            AttributeValueAssertion assertion = new AttributeValueAssertion();

            // Checking and adding the filter's attributes
            String attributeValue;
            // name
            attributeValue = xpp.getAttributeValue( "", "name" );
            if ( attributeValue != null )
            {
                assertion.setAttributeDesc( new String( attributeValue.getBytes() ) );
            }
            else
            {
                throw new XmlPullParserException( "name attribute is required", xpp, null );
            }

            AttributeValueAssertionFilter filter = new AttributeValueAssertionFilter(
                LdapConstants.LESS_OR_EQUAL_FILTER );

            filter.setAssertion( assertion );

            // Adding the filter to the Search Filter
            try
            {
                searchRequest.addCurrentFilter( filter );
            }
            catch ( DecoderException e )
            {
                throw new XmlPullParserException( e.getMessage(), xpp, null );
            }
            searchRequest.setTerminalFilter( filter );
        }
    };

    /**
     * GrammarAction that create an Approx Match Filter
     */
    private final GrammarAction approxMatchFilterCreation = new GrammarAction( "Create Approx Match Filter" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            SearchRequestCodec searchRequest = ( SearchRequestCodec ) container.getBatchRequest().getCurrentRequest();

            XmlPullParser xpp = container.getParser();

            AttributeValueAssertion assertion = new AttributeValueAssertion();

            // Checking and adding the filter's attributes
            String attributeValue;
            // name
            attributeValue = xpp.getAttributeValue( "", "name" );
            if ( attributeValue != null )
            {
                assertion.setAttributeDesc( new String( attributeValue.getBytes() ) );
            }
            else
            {
                throw new XmlPullParserException( "name attribute is required", xpp, null );
            }

            AttributeValueAssertionFilter filter = new AttributeValueAssertionFilter( LdapConstants.APPROX_MATCH_FILTER );

            filter.setAssertion( assertion );

            // Adding the filter to the Search Filter
            try
            {
                searchRequest.addCurrentFilter( filter );
            }
            catch ( DecoderException e )
            {
                throw new XmlPullParserException( e.getMessage(), xpp, null );
            }

            searchRequest.setTerminalFilter( filter );
        }
    };

    /**
     * GrammarAction that adds a Value to a Filter
     */
    private final GrammarAction filterAddValue = new GrammarAction( "Adds Value to Filter" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            SearchRequestCodec searchRequest = ( SearchRequestCodec ) container.getBatchRequest().getCurrentRequest();
            AttributeValueAssertionFilter filter = ( AttributeValueAssertionFilter ) searchRequest.getTerminalFilter();
            AttributeValueAssertion assertion = filter.getAssertion();

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
                        Value<byte[]> value = new ClientBinaryValue( Base64.decode( nextText.trim().toCharArray() ) );
                        assertion.setAssertionValue( value );
                    }
                    else
                    {
                        Value<String> value = new ClientStringValue( nextText.trim() );
                        assertion.setAssertionValue( value );
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
     * GrammarAction that creates a Present Filter
     */
    private final GrammarAction presentFilterCreation = new GrammarAction( "Create Present Filter" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            PresentFilter presentFilter = new PresentFilter();

            XmlPullParser xpp = container.getParser();

            // Adding the filter to the Search Filter
            SearchRequestCodec searchRequest = ( SearchRequestCodec ) container.getBatchRequest().getCurrentRequest();
            try
            {
                searchRequest.addCurrentFilter( presentFilter );
            }
            catch ( DecoderException e )
            {
                throw new XmlPullParserException( e.getMessage(), xpp, null );
            }

            // Checking and adding the filter's attributes
            String attributeValue;
            // name
            attributeValue = xpp.getAttributeValue( "", "name" );
            if ( attributeValue != null )
            {
                presentFilter.setAttributeDescription( new String( attributeValue.getBytes() ) );
            }
            else
            {
                throw new XmlPullParserException( "name attribute is required", xpp, null );
            }
        }
    };

    /**
     * GrammarAction that creates an Extensible Match Filter
     */
    private final GrammarAction extensibleMatchFilterCreation = new GrammarAction( "Create Extensible Match Filter" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            ExtensibleMatchFilter extensibleMatchFilter = new ExtensibleMatchFilter();

            XmlPullParser xpp = container.getParser();

            // Adding the filter to the Search Filter
            SearchRequestCodec searchRequest = ( SearchRequestCodec ) container.getBatchRequest().getCurrentRequest();
            try
            {
                searchRequest.addCurrentFilter( extensibleMatchFilter );
            }
            catch ( DecoderException e )
            {
                throw new XmlPullParserException( "name attribute is required", xpp, null );
            }
            searchRequest.setTerminalFilter( extensibleMatchFilter );

            // Checking and adding the filter's attributes
            String attributeValue;
            // dnAttributes
            attributeValue = xpp.getAttributeValue( "", "dnAttributes" );
            if ( attributeValue != null )
            {
                if ( ( attributeValue.equals( "true" ) ) || ( attributeValue.equals( "1" ) ) )
                {
                    extensibleMatchFilter.setDnAttributes( true );
                }
                else if ( ( attributeValue.equals( "false" ) ) || ( attributeValue.equals( "0" ) ) )
                {
                    extensibleMatchFilter.setDnAttributes( false );
                }
                else
                {
                    throw new XmlPullParserException( "dnAttributes must be a boolean ('true' or 'false').", xpp, null );
                }
            }
            else
            {
                extensibleMatchFilter.setDnAttributes( false );
            }
            // matchingRule
            attributeValue = xpp.getAttributeValue( "", "matchingRule" );
            if ( attributeValue != null )
            {
                extensibleMatchFilter.setMatchingRule( attributeValue );
            }
            // name
            attributeValue = xpp.getAttributeValue( "", "name" );
            if ( attributeValue != null )
            {
                extensibleMatchFilter.setType( attributeValue );
            }
        }
    };

    /**
     * GrammarAction that adds a Value to an Extensible Match Filter
     */
    private final GrammarAction extensibleMatchAddValue = new GrammarAction( "Adds Value to Extensible MatchFilter" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            SearchRequestCodec searchRequest = ( SearchRequestCodec ) container.getBatchRequest().getCurrentRequest();
            ExtensibleMatchFilter filter = ( ExtensibleMatchFilter ) searchRequest.getTerminalFilter();

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
                        filter.setMatchValue( Base64.decode( nextText.trim().toCharArray() ) );
                    }
                    else
                    {
                        filter.setMatchValue( nextText.trim() );
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
     * GrammarAction that creates a Control
     */
    private final GrammarAction controlCreation = new GrammarAction( "Create Control" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            ControlCodec control = new ControlCodec();
            container.getBatchRequest().getCurrentRequest().addControl( control );

            XmlPullParser xpp = container.getParser();

            // Checking and adding the Control's attributes
            String attributeValue;
            // TYPE
            attributeValue = xpp.getAttributeValue( "", "type" );
            if ( attributeValue != null )
            {
                if ( !OID.isOID( attributeValue ) )
                {
                    throw new XmlPullParserException( "Incorrect value for 'type' attribute. This is not an OID.", xpp,
                        null );
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
    };

    /**
     * GrammarAction that adds a Value to a Control
     */
    private final GrammarAction controlValueCreation = new GrammarAction( "Add ControlValue to Control" )
    {
        public void action( Dsmlv2Container container ) throws XmlPullParserException
        {
            ControlCodec control = container.getBatchRequest().getCurrentRequest().getCurrentControl();

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
    };


    /**
     * Gets an instance of this grammar
     * 
     * @return
     *      an instance of this grammar
     */
    public static Dsmlv2Grammar getInstance()
    {
        return instance;
    }
}
