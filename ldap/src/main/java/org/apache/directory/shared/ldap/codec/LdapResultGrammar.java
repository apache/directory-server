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
package org.apache.directory.shared.ldap.codec;


import java.util.Iterator;

import javax.naming.InvalidNameException;

import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.grammar.IGrammar;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.util.IntegerDecoder;
import org.apache.directory.shared.asn1.util.IntegerDecoderException;
import org.apache.directory.shared.ldap.codec.util.LdapResultEnum;
import org.apache.directory.shared.ldap.codec.util.LdapString;
import org.apache.directory.shared.ldap.codec.util.LdapStringEncodingException;
import org.apache.directory.shared.ldap.codec.util.LdapURL;
import org.apache.directory.shared.ldap.codec.util.LdapURLEncodingException;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the LdapResult LDAP message. All the actions are
 * declared in this class. As it is a singleton, these declaration are only done
 * once. If an action is to be added or modified, this is where the work is to
 * be done !
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapResultGrammar extends AbstractGrammar implements IGrammar
{
    // ~ Static fields/initializers
    // -----------------------------------------------------------------

    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( LdapResultGrammar.class );

    /** A speedup for logger */
    private static final boolean IS_DEBUG = log.isDebugEnabled();
    
    /** The instance of grammar. LdapResultGrammar is a singleton */
    private static IGrammar instance = new LdapResultGrammar();


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new LdapResultGrammar object.
     */
    private LdapResultGrammar()
    {
        name = LdapResultGrammar.class.getName();
        statesEnum = LdapStatesEnum.getInstance();

        // Create the transitions table
        super.transitions = new GrammarTransition[LdapStatesEnum.LAST_LDAP_RESULT_STATE][256];

        // ============================================================================================
        // LdapResult
        // ============================================================================================
        // LDAPResult --> SEQUENCE {
        // resultCode ENUMERATED { ... (Tag)
        // We have a LDAPResult, and the tag may be 0x0A
        // Nothing to do
        super.transitions[LdapStatesEnum.LDAP_RESULT_CODE_TAG][UniversalTag.ENUMERATED_TAG] = new GrammarTransition(
            LdapStatesEnum.LDAP_RESULT_CODE_TAG, LdapStatesEnum.LDAP_RESULT_CODE_VALUE, null );

        // LDAPResult --> SEQUENCE {
        // resultCode ENUMERATED { ... (Value)
        // The result code. The different values are :
        // success (0),
        // operationsError (1),
        // protocolError (2),
        // timeLimitExceeded (3),
        // sizeLimitExceeded (4),
        // compareFalse (5),
        // compareTrue (6),
        // authMethodNotSupported (7),
        // strongAuthRequired (8),
        // -- 9 reserved --
        // referral (10), -- new
        // adminLimitExceeded (11), -- new
        // unavailableCriticalExtension (12), -- new
        // confidentialityRequired (13), -- new
        // saslBindInProgress (14), -- new
        // noSuchAttribute (16),
        // undefinedAttributeType (17),
        // inappropriateMatching (18),
        // constraintViolation (19),
        // attributeOrValueExists (20),
        // invalidAttributeSyntax (21),
        // -- 22-31 unused --
        // noSuchObject (32),
        // aliasProblem (33),
        // invalidDNSyntax (34),
        // -- 35 reserved for undefined isLeaf --
        // aliasDereferencingProblem (36),
        // -- 37-47 unused --
        // inappropriateAuthentication (48),
        // invalidCredentials (49),
        // insufficientAccessRights (50),
        // busy (51),
        // unavailable (52),
        // unwillingToPerform (53),
        // loopDetect (54),
        // -- 55-63 unused --
        // namingViolation (64),
        // objectClassViolation (65),
        // notAllowedOnNonLeaf (66),
        // notAllowedOnRDN (67),
        // entryAlreadyExists (68),
        // objectClassModsProhibited (69),
        // -- 70 reserved for CLDAP --
        // affectsMultipleDSAs (71), -- new
        // -- 72-79 unused --
        // other (80) },
        // -- 81-90 reserved for APIs --
        //
        super.transitions[LdapStatesEnum.LDAP_RESULT_CODE_VALUE][UniversalTag.ENUMERATED_TAG] = new GrammarTransition(
            LdapStatesEnum.LDAP_RESULT_CODE_VALUE, LdapStatesEnum.LDAP_RESULT_MATCHED_DN_TAG, new GrammarAction(
                "Store resultCode" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {

                    LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
                    LdapMessage ldapMessage = ldapMessageContainer.getLdapMessage();
                    LdapResponse response = ldapMessage.getLdapResponse();
                    LdapResult ldapResult = new LdapResult();
                    response.setLdapResult( ldapResult );

                    // We don't have to allocate a LdapResult first.

                    // The current TLV should be a integer
                    // We get it and store it in MessageId
                    TLV tlv = ldapMessageContainer.getCurrentTLV();

                    Value value = tlv.getValue();
                    int resultCode = 0;

                    try
                    {
                        resultCode = IntegerDecoder.parse( value, 0, 90 );
                    }
                    catch ( IntegerDecoderException ide )
                    {
                        log.error( "The result code " + StringTools.dumpBytes( value.getData() ) + " is invalid : "
                            + ide.getMessage() + ". The result code must be between (0 .. 90)" );

                        throw new DecoderException( ide.getMessage() );
                    }

                    // Treat the 'normal' cases !
                    switch ( resultCode )
                    {
                        case LdapResultEnum.SUCCESS:
                        case LdapResultEnum.OPERATIONS_ERROR:
                        case LdapResultEnum.PROTOCOL_ERROR:
                        case LdapResultEnum.TIME_LIMIT_EXCEEDED:
                        case LdapResultEnum.SIZE_LIMIT_EXCEEDED:
                        case LdapResultEnum.COMPARE_FALSE:
                        case LdapResultEnum.COMPARE_TRUE:
                        case LdapResultEnum.AUTH_METHOD_NOT_SUPPORTED:
                        case LdapResultEnum.STRONG_AUTH_REQUIRED:
                        case LdapResultEnum.REFERRAL:
                        case LdapResultEnum.ADMIN_LIMIT_EXCEEDED:
                        case LdapResultEnum.UNAVAILABLE_CRITICAL_EXTENSION:
                        case LdapResultEnum.CONFIDENTIALITY_REQUIRED:
                        case LdapResultEnum.SASL_BIND_IN_PROGRESS:
                        case LdapResultEnum.NO_SUCH_ATTRIBUTE:
                        case LdapResultEnum.UNDEFINED_ATTRIBUTE_TYPE:
                        case LdapResultEnum.INAPPROPRIATE_MATCHING:
                        case LdapResultEnum.CONSTRAINT_VIOLATION:
                        case LdapResultEnum.ATTRIBUTE_OR_VALUE_EXISTS:
                        case LdapResultEnum.INVALID_ATTRIBUTE_SYNTAX:
                        case LdapResultEnum.NO_SUCH_OBJECT:
                        case LdapResultEnum.ALIAS_PROBLEM:
                        case LdapResultEnum.INVALID_DN_SYNTAX:
                        case LdapResultEnum.ALIAS_DEREFERENCING_PROBLEM:
                        case LdapResultEnum.INAPPROPRIATE_AUTHENTICATION:
                        case LdapResultEnum.INVALID_CREDENTIALS:
                        case LdapResultEnum.INSUFFICIENT_ACCESS_RIGHTS:
                        case LdapResultEnum.BUSY:
                        case LdapResultEnum.UNAVAILABLE:
                        case LdapResultEnum.UNWILLING_TO_PERFORM:
                        case LdapResultEnum.LOOP_DETECT:
                        case LdapResultEnum.NAMING_VIOLATION:
                        case LdapResultEnum.OBJECT_CLASS_VIOLATION:
                        case LdapResultEnum.NOT_ALLOWED_ON_NON_LEAF:
                        case LdapResultEnum.NOT_ALLOWED_ON_RDN:
                        case LdapResultEnum.ENTRY_ALREADY_EXISTS:
                        case LdapResultEnum.AFFECTS_MULTIPLE_DSAS:
                            ldapResult.setResultCode( resultCode );
                            break;

                        default:
                            log.warn( "The resultCode " + resultCode + " is unknown." );
                            ldapResult.setResultCode( LdapResultEnum.OTHER );
                    }

                    if ( IS_DEBUG )
                    {
                        log.debug( "The result code is set to " + LdapResultEnum.errorCode( resultCode ) );
                    }
                }
            } );

        // LDAPResult --> SEQUENCE {
        // ...
        // matchedDN LDAPDN, (Tag)
        // The tag is 0x04. Nothing to do
        super.transitions[LdapStatesEnum.LDAP_RESULT_MATCHED_DN_TAG][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            LdapStatesEnum.LDAP_RESULT_MATCHED_DN_TAG, LdapStatesEnum.LDAP_RESULT_MATCHED_DN_VALUE, null );

        // LDAPResult --> SEQUENCE {
        // ...
        // matchedDN LDAPDN, (Value)
        // We store the LDAPDN after having checked that it is valid.
        super.transitions[LdapStatesEnum.LDAP_RESULT_MATCHED_DN_VALUE][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            LdapStatesEnum.LDAP_RESULT_MATCHED_DN_VALUE, LdapStatesEnum.LDAP_RESULT_ERROR_MESSAGE_TAG,
            new GrammarAction( "Store Ldap Result matched DN" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {

                    LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
                    LdapMessage ldapMessage = ldapMessageContainer.getLdapMessage();
                    LdapResponse response = ldapMessage.getLdapResponse();
                    LdapResult ldapResult = response.getLdapResult();

                    // Get the Value and store it in the BindResponse
                    TLV tlv = ldapMessageContainer.getCurrentTLV();

                    // We have to handle the special case of a 0 length matched
                    // DN
                    if ( tlv.getLength().getLength() == 0 )
                    {
                        ldapResult.setMatchedDN( LdapDN.EMPTY_LDAPDN );
                    }
                    else
                    {
                        // A not null matchedDN is valid for resultCodes
                        // NoSuchObject, AliasProblem, InvalidDNSyntax and
                        // AliasDreferencingProblem.

                        if ( ( ldapResult.getResultCode() == LdapResultEnum.NO_SUCH_OBJECT )
                            || ( ldapResult.getResultCode() == LdapResultEnum.ALIAS_PROBLEM )
                            || ( ldapResult.getResultCode() == LdapResultEnum.INVALID_DN_SYNTAX )
                            || ( ldapResult.getResultCode() == LdapResultEnum.ALIAS_DEREFERENCING_PROBLEM ) )
                        {
                            byte[] dnBytes = tlv.getValue().getData();
                            
                            try
                            {
                                ldapResult.setMatchedDN( new LdapDN( dnBytes ) );
                            }
                            catch ( InvalidNameException ine )
                            {
                                // This is for the client side. We will never decode LdapResult on the server
                                String msg = "Incorrect DN given : " + StringTools.utf8ToString( dnBytes ) + 
                                    " (" + StringTools.dumpBytes( dnBytes )
                                    + ") is invalid";
                                log.error( "{} : {}", msg, ine.getMessage() );
                            
                                throw new DecoderException( "Incorrect DN given : " + ine.getMessage() );
                            }
                        }
                        else
                        {
                            log
                                .warn( "The matched DN should not be set when the result code is one of NoSuchObject, AliasProblem, InvalidDNSyntax or AliasDreferencingProblem" );
                            ldapResult.setMatchedDN( LdapDN.EMPTY_LDAPDN );
                        }
                    }

                    if ( IS_DEBUG )
                    {
                        log.debug( "The matchedDN is " + ldapResult.getMatchedDN() );
                    }

                    return;
                }
            } );

        // LDAPResult --> SEQUENCE {
        // ...
        // errorMessage LDAPString, (Tag)
        // The tag is 0x04. Nothing to do
        super.transitions[LdapStatesEnum.LDAP_RESULT_ERROR_MESSAGE_TAG][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            LdapStatesEnum.LDAP_RESULT_ERROR_MESSAGE_TAG, LdapStatesEnum.LDAP_RESULT_ERROR_MESSAGE_VALUE, null );

        // LDAPResult --> SEQUENCE {
        // ...
        // errorMessage LDAPString, (Value)
        // Stores the value.
        super.transitions[LdapStatesEnum.LDAP_RESULT_ERROR_MESSAGE_VALUE][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            LdapStatesEnum.LDAP_RESULT_ERROR_MESSAGE_VALUE, LdapStatesEnum.LDAP_RESULT_REFERRAL_SEQUENCE_TAG,
            new GrammarAction( "Store Ldap Result error message" )
            {
                public void action( IAsn1Container container )
                {

                    LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
                    LdapMessage ldapMessage = ldapMessageContainer.getLdapMessage();
                    LdapResponse response = ldapMessage.getLdapResponse();
                    LdapResult ldapResult = response.getLdapResult();

                    // Get the Value and store it in the BindResponse
                    TLV tlv = ldapMessageContainer.getCurrentTLV();

                    // We have to handle the special case of a 0 length error
                    // message
                    if ( tlv.getLength().getLength() == 0 )
                    {
                        ldapResult.setErrorMessage( LdapString.EMPTY_STRING );
                    }
                    else
                    {
                        try
                        {
                            ldapResult.setErrorMessage( new LdapString( tlv.getValue().getData() ) );
                        }
                        catch ( LdapStringEncodingException lsee )
                        {
                            log.error( "The Error Message is invalid : "
                                + StringTools.dumpBytes( tlv.getValue().getData() ) );
                            ldapResult.setErrorMessage( LdapString.EMPTY_STRING );
                        }
                    }

                    // We can have an END transition
                    ldapMessageContainer.grammarEndAllowed( true );

                    // We can pop this grammar
                    container.grammarPopAllowed( true );

                    if ( IS_DEBUG )
                    {
                        log.debug( "The error message is : " + ldapResult.getErrorMessage() );
                    }

                    return;
                }
            } );

        // LDAPResult --> SEQUENCE {
        // ...
        // referral [3] Referral OPTIONAL } (Tag)
        //
        // The next state could be one of the following :
        // - 0x83 : a referral, in a LdapResult.
        // - 0x8A : an extended response
        // - GRAMMAR_END : this is implicitly deducted by the fact that we don't
        // have anymore byte...
        //
        // We don't deal with all of this values, we just have to treat the
        // 0x83, because
        // referral is a part of the LdapResult grammar. In all other cases, we
        // just quit
        // the grammar.
        // As referral is optionnal, we may transit from a error message state

        // This is a referral sequence.
        super.transitions[LdapStatesEnum.LDAP_RESULT_REFERRAL_SEQUENCE_TAG][LdapConstants.LDAP_RESULT_REFERRAL_SEQUENCE_TAG] = new GrammarTransition(
            LdapStatesEnum.LDAP_RESULT_REFERRAL_SEQUENCE_TAG, LdapStatesEnum.LDAP_RESULT_REFERRAL_SEQUENCE_VALUE, null );

        // Nothing to do with the value, except that we can't pop the gramar
        // here.
        super.transitions[LdapStatesEnum.LDAP_RESULT_REFERRAL_SEQUENCE_VALUE][LdapConstants.LDAP_RESULT_REFERRAL_SEQUENCE_TAG] = new GrammarTransition(
            LdapStatesEnum.LDAP_RESULT_REFERRAL_SEQUENCE_VALUE, LdapStatesEnum.LDAP_RESULT_REFERRAL_TAG,
            new GrammarAction( "Pop not allowed" )
            {
                public void action( IAsn1Container container )
                {
                    container.grammarPopAllowed( false );
                }
            } );

        // Referral ::= SEQUENCE OF LDAPURL (Tag)
        // This is a SEQUENCE, we will have at least one referral, but may be
        // many.
        // As this is the tag, we don't have anything to do.
        super.transitions[LdapStatesEnum.LDAP_RESULT_REFERRAL_TAG][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            LdapStatesEnum.LDAP_RESULT_REFERRAL_TAG, LdapStatesEnum.LDAP_RESULT_REFERRAL_VALUE, null );

        // Referral ::= SEQUENCE OF LDAPURL (Length)
        // We may have other referrals, but we may also have finished to read
        // the LdapResult.
        // To handle those different cases, we have to transit to a special
        // state, which
        // will do this brancing.
        //
        // The referral message exists only if the resultCode is REFERRAL.
        //
        // Here, we store the referral in the ldapResult.
        super.transitions[LdapStatesEnum.LDAP_RESULT_REFERRAL_VALUE][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            LdapStatesEnum.LDAP_RESULT_REFERRAL_VALUE, LdapStatesEnum.LDAP_RESULT_REFERRAL_TAG, new GrammarAction(
                "Store Ldap Result referral" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {

                    LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
                    LdapMessage ldapMessage = ldapMessageContainer.getLdapMessage();
                    LdapResponse response = ldapMessage.getLdapResponse();
                    LdapResult ldapResult = response.getLdapResult();

                    TLV tlv = ldapMessageContainer.getCurrentTLV();

                    if ( tlv.getLength().getLength() == 0 )
                    {
                        ldapResult.addReferral( LdapURL.EMPTY_URL );
                    }
                    else
                    {
                        if ( ldapResult.getResultCode() == LdapResultEnum.REFERRAL )
                        {
                            try
                            {
                                ldapResult.addReferral( new LdapURL( tlv.getValue().getData() ) );
                            }
                            catch ( LdapURLEncodingException luee )
                            {
                                String badUrl = new String( tlv.getValue().getData() );
                                log.error( "The URL " + badUrl + " is not valid : " + luee.getMessage() );
                                throw new DecoderException( "Invalid URL : " + luee.getMessage() );
                            }
                        }
                        else
                        {
                            log
                                .warn( "The Referral error message is not allowed when havind an error code no equals to REFERRAL" );
                            ldapResult.addReferral( LdapURL.EMPTY_URL );
                        }
                    }

                    // We can have an END transition
                    ldapMessageContainer.grammarEndAllowed( true );

                    // We can have a Pop transition
                    ldapMessageContainer.grammarPopAllowed( true );

                    if ( IS_DEBUG )
                    {
                        Iterator urls = ldapResult.getReferrals().iterator();

                        StringBuffer sb = new StringBuffer();
                        boolean isFirst = true;

                        while ( urls.hasNext() )
                        {
                            if ( isFirst )
                            {
                                isFirst = false;
                            }
                            else
                            {
                                sb.append( ", " );
                            }

                            sb.append( urls.next() );
                        }

                        log.debug( "The referral error message is set to " + sb.toString() );
                    }
                }
            } );

    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * This class is a singleton.
     * 
     * @return An instance on this grammar
     */
    public static IGrammar getInstance()
    {
        return instance;
    }
}
