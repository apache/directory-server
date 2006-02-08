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
package org.apache.directory.shared.ldap.codec.compare;


import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingException;

import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.grammar.IGrammar;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapMessage;
import org.apache.directory.shared.ldap.codec.LdapMessageContainer;
import org.apache.directory.shared.ldap.codec.LdapStatesEnum;
import org.apache.directory.shared.ldap.codec.util.LdapString;
import org.apache.directory.shared.ldap.codec.util.LdapStringEncodingException;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the CompareRequest LDAP message. All the actions are
 * declared in this class. As it is a singleton, these declaration are only done
 * once. If an action is to be added or modified, this is where the work is to
 * be done !
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class CompareRequestGrammar extends AbstractGrammar implements IGrammar
{
    // ~ Static fields/initializers
    // -----------------------------------------------------------------

    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( CompareRequestGrammar.class );

    /** The instance of grammar. CompareRequest is a singleton */
    private static IGrammar instance = new CompareRequestGrammar();


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new CompareRequest object.
     */
    private CompareRequestGrammar()
    {
        name = CompareRequestGrammar.class.getName();
        statesEnum = LdapStatesEnum.getInstance();

        // Create the transitions table
        super.transitions = new GrammarTransition[LdapStatesEnum.LAST_COMPARE_REQUEST_STATE][256];

        // ============================================================================================
        // CompareRequest
        // ============================================================================================
        // CompareRequest ::= [APPLICATION 14] SEQUENCE { (Tag)
        // Nothing to do
        super.transitions[LdapStatesEnum.COMPARE_REQUEST_TAG][LdapConstants.COMPARE_REQUEST_TAG] = new GrammarTransition(
            LdapStatesEnum.COMPARE_REQUEST_TAG, LdapStatesEnum.COMPARE_REQUEST_VALUE, null );

        // CompareRequest ::= [APPLICATION 14] SEQUENCE { (Value)
        // Initialize the compare request pojo
        super.transitions[LdapStatesEnum.COMPARE_REQUEST_VALUE][LdapConstants.COMPARE_REQUEST_TAG] = new GrammarTransition(
            LdapStatesEnum.COMPARE_REQUEST_VALUE, LdapStatesEnum.COMPARE_REQUEST_ENTRY_TAG, new GrammarAction(
                "Init Compare Request" )
            {
                public void action( IAsn1Container container )
                {

                    LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
                    LdapMessage ldapMessage = ldapMessageContainer.getLdapMessage();

                    // We can allocate the CompareRequest Object
                    ldapMessage.setProtocolOP( new CompareRequest() );
                }
            } );

        // CompareRequest ::= [APPLICATION 14] SEQUENCE {
        // entry LDAPDN, (Tag)
        // ...
        // Nothing to do
        super.transitions[LdapStatesEnum.COMPARE_REQUEST_ENTRY_TAG][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            LdapStatesEnum.COMPARE_REQUEST_ENTRY_TAG, LdapStatesEnum.COMPARE_REQUEST_ENTRY_VALUE, null );

        // CompareRequest ::= [APPLICATION 14] SEQUENCE {
        // entry LDAPDN, (Tag)
        // ...
        // Store the DN to be compared
        super.transitions[LdapStatesEnum.COMPARE_REQUEST_ENTRY_VALUE][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            LdapStatesEnum.COMPARE_REQUEST_ENTRY_VALUE, LdapStatesEnum.COMPARE_REQUEST_AVA_TAG, new GrammarAction(
                "Store entry" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {

                    LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
                    LdapMessage ldapMessage = ldapMessageContainer.getLdapMessage();

                    // We can allocate the CompareRequest Object
                    CompareRequest compareRequest = ldapMessage.getCompareRequest();

                    // Get the Value and store it in the CompareRequest
                    TLV tlv = ldapMessageContainer.getCurrentTLV();
                    Name entry = null;

                    // We have to handle the special case of a 0 length matched
                    // DN
                    if ( tlv.getLength().getLength() == 0 )
                    {
                        throw new DecoderException( "The entry must not be null" );
                    }
                    else
                    {
                        try
                        {
                            entry = new LdapDN( tlv.getValue().getData() );
                            entry = LdapDN.normalize( entry );
                        }
                        catch ( InvalidNameException ine )
                        {
                            String msg = "The DN to compare  (" + StringTools.dumpBytes( tlv.getValue().getData() )
                                + ") is invalid";
                            log.error( "{} : {}", msg, ine.getMessage() );
                            throw new DecoderException( msg, ine );
                        }
                        catch ( NamingException ne )
                        {
                            String msg = "The DN to compare  (" + StringTools.dumpBytes( tlv.getValue().getData() )
                                + ") is invalid";
                            log.error( "{} : {}", msg, ne.getMessage() );
                            throw new DecoderException( msg, ne );
                        }

                        compareRequest.setEntry( entry );
                    }

                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "Comparing DN {}", entry );
                    }
                }
            } );

        // CompareRequest ::= [APPLICATION 14] SEQUENCE {
        // ...
        // ava AttributeValueAssertion }
        // AttributeValueAssertion ::= SEQUENCE { (Tag)
        // Nothing to do
        super.transitions[LdapStatesEnum.COMPARE_REQUEST_AVA_TAG][UniversalTag.SEQUENCE_TAG] = new GrammarTransition(
            LdapStatesEnum.COMPARE_REQUEST_AVA_TAG, LdapStatesEnum.COMPARE_REQUEST_AVA_VALUE, null );

        // CompareRequest ::= [APPLICATION 14] SEQUENCE {
        // ...
        // ava AttributeValueAssertion }
        // AttributeValueAssertion ::= SEQUENCE { (Value)
        // Nothing to do
        super.transitions[LdapStatesEnum.COMPARE_REQUEST_AVA_VALUE][UniversalTag.SEQUENCE_TAG] = new GrammarTransition(
            LdapStatesEnum.COMPARE_REQUEST_AVA_VALUE, LdapStatesEnum.COMPARE_REQUEST_ATTRIBUTE_DESC_TAG, null );

        // AttributeValueAssertion ::= SEQUENCE {
        // attributeDesc AttributeDescription, (Tag)
        // ...
        // Nothing to do
        super.transitions[LdapStatesEnum.COMPARE_REQUEST_ATTRIBUTE_DESC_TAG][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            LdapStatesEnum.COMPARE_REQUEST_ATTRIBUTE_DESC_TAG, LdapStatesEnum.COMPARE_REQUEST_ATTRIBUTE_DESC_VALUE,
            null );

        // AttributeValueAssertion ::= SEQUENCE {
        // attributeDesc AttributeDescription, (Value)
        // ...
        // Nothing to do
        super.transitions[LdapStatesEnum.COMPARE_REQUEST_ATTRIBUTE_DESC_VALUE][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            LdapStatesEnum.COMPARE_REQUEST_ATTRIBUTE_DESC_VALUE, LdapStatesEnum.COMPARE_REQUEST_ASSERTION_VALUE_TAG,
            new GrammarAction( "Store attribute desc" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {

                    LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
                    LdapMessage ldapMessage = ldapMessageContainer.getLdapMessage();

                    // Get the CompareRequest Object
                    CompareRequest compareRequest = ldapMessage.getCompareRequest();

                    // Get the Value and store it in the CompareRequest
                    TLV tlv = ldapMessageContainer.getCurrentTLV();

                    // We have to handle the special case of a 0 length matched
                    // DN
                    if ( tlv.getLength().getLength() == 0 )
                    {
                        throw new DecoderException( "The attribute description must not be null" );
                    }
                    else
                    {
                        try
                        {
                            LdapString type = LdapDN.normalizeAttribute( tlv.getValue().getData() );
                            compareRequest.setAttributeDesc( type );
                        }
                        catch ( LdapStringEncodingException lsee )
                        {
                            log.error( "The attribute description ({}) is invalid.", StringTools.dumpBytes( tlv
                                .getValue().getData() ) );
                            throw new DecoderException( "Invalid attribute description "
                                + StringTools.dumpBytes( tlv.getValue().getData() ) + ", : " + lsee.getMessage() );

                        }
                    }

                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "Comparing attribute description {}", compareRequest.getAttributeDesc() );
                    }
                }
            } );

        // AttributeValueAssertion ::= SEQUENCE {
        // ...
        // assertionValue AssertionValue } (Tag)
        // Nothing to do
        super.transitions[LdapStatesEnum.COMPARE_REQUEST_ASSERTION_VALUE_TAG][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            LdapStatesEnum.COMPARE_REQUEST_ASSERTION_VALUE_TAG, LdapStatesEnum.COMPARE_REQUEST_ASSERTION_VALUE_VALUE,
            null );

        // AttributeValueAssertion ::= SEQUENCE {
        // ...
        // assertionValue AssertionValue } (Value)
        // Nothing to do
        super.transitions[LdapStatesEnum.COMPARE_REQUEST_ASSERTION_VALUE_VALUE][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            LdapStatesEnum.COMPARE_REQUEST_ASSERTION_VALUE_VALUE, LdapStatesEnum.END_STATE, new GrammarAction(
                "Store assertion value" )
            {
                public void action( IAsn1Container container )
                {

                    LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
                    LdapMessage ldapMessage = ldapMessageContainer.getLdapMessage();

                    // Get the CompareRequest Object
                    CompareRequest compareRequest = ldapMessage.getCompareRequest();

                    // Get the Value and store it in the CompareRequest
                    TLV tlv = ldapMessageContainer.getCurrentTLV();

                    // We have to handle the special case of a 0 length value
                    if ( tlv.getLength().getLength() == 0 )
                    {
                        compareRequest.setAssertionValue( "" );
                    }
                    else
                    {
                        if ( ldapMessageContainer.isBinary( compareRequest.getAttributeDesc() ) )
                        {
                            compareRequest.setAssertionValue( tlv.getValue().getData() );

                            if ( log.isDebugEnabled() )
                            {
                                log.debug( "Comparing attribute value {}", StringTools
                                    .dumpBytes( ( byte[] ) compareRequest.getAssertionValue() ) );
                            }
                        }
                        else
                        {
                            compareRequest.setAssertionValue( StringTools.utf8ToString( tlv.getValue().getData() ) );

                            if ( log.isDebugEnabled() )
                            {
                                log.debug( "Comparing attribute value {}", compareRequest.getAssertionValue() );
                            }
                        }
                    }

                    // We can have an END transition
                    ldapMessageContainer.grammarEndAllowed( true );

                    // We can have an POP transition
                    ldapMessageContainer.grammarPopAllowed( true );
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
