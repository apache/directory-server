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
package org.apache.directory.shared.ldap.codec.extended;

import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.grammar.IGrammar;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapMessage;
import org.apache.directory.shared.ldap.codec.LdapMessageContainer;
import org.apache.directory.shared.ldap.codec.LdapStatesEnum;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * This class implements the ExtendedRequest LDAP message. All the actions are declared in this
 * class. As it is a singleton, these declaration are only done once.
 * 
 * If an action is to be added or modified, this is where the work is to be done !
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ExtendedRequestGrammar extends AbstractGrammar implements IGrammar
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( ExtendedRequestGrammar.class );

    /** The instance of grammar. ExtendedRequest is a singleton */
    private static IGrammar instance = new ExtendedRequestGrammar();

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new ExtendedRequest object.
     */
    private ExtendedRequestGrammar()
    {
        name              = ExtendedRequestGrammar.class.getName();
        statesEnum        = LdapStatesEnum.getInstance();

        // Create the transitions table
        super.transitions = new GrammarTransition[LdapStatesEnum.LAST_EXTENDED_REQUEST_STATE][256];

        //============================================================================================
        // ExtendedRequest
        //============================================================================================
        // ExtendedRequest ::= [APPLICATION 23] SEQUENCE { (Tag)
        // Nothing to do
        super.transitions[LdapStatesEnum.EXTENDED_REQUEST_TAG][LdapConstants.EXTENDED_REQUEST_TAG] = new GrammarTransition(
                LdapStatesEnum.EXTENDED_REQUEST_TAG,
                LdapStatesEnum.EXTENDED_REQUEST_VALUE, null );

        // ExtendedRequest ::= [APPLICATION 23] SEQUENCE { (Value)
        // Initialize the compare request pojo
        super.transitions[LdapStatesEnum.EXTENDED_REQUEST_VALUE][LdapConstants.EXTENDED_REQUEST_TAG] = new GrammarTransition(
                LdapStatesEnum.EXTENDED_REQUEST_VALUE, LdapStatesEnum.EXTENDED_REQUEST_NAME_TAG,
                new GrammarAction( "Init Extended Request" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {

                        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
                            container;
                        LdapMessage          ldapMessage          =
                            ldapMessageContainer.getLdapMessage();

                        // We can allocate the ExtendedRequest Object
                        ldapMessage.setProtocolOP( new ExtendedRequest() );
                    }
                } );

        // ExtendedRequest ::= [APPLICATION 23] SEQUENCE { 
        //     requestName      [0] LDAPOID, (Tag)
        //     ...
        // Nothing to do
        super.transitions[LdapStatesEnum.EXTENDED_REQUEST_NAME_TAG][LdapConstants.EXTENDED_REQUEST_NAME_TAG] = new GrammarTransition(
                LdapStatesEnum.EXTENDED_REQUEST_NAME_TAG,
                LdapStatesEnum.EXTENDED_REQUEST_NAME_VALUE, null );

        // ExtendedRequest ::= [APPLICATION 23] SEQUENCE { 
        //     requestName      [0] LDAPOID, (Value)
        //     ...
        // Store the DN to be compared
        super.transitions[LdapStatesEnum.EXTENDED_REQUEST_NAME_VALUE][LdapConstants.EXTENDED_REQUEST_NAME_TAG] = new GrammarTransition(
                LdapStatesEnum.EXTENDED_REQUEST_NAME_VALUE, LdapStatesEnum.EXTENDED_REQUEST_VALUE_TAG,
                new GrammarAction( "Store name" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {

                        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
                            container;
                        LdapMessage          ldapMessage          =
                            ldapMessageContainer.getLdapMessage();

                        // We can allocate the ExtendedRequest Object
                        ExtendedRequest extendedRequest = ldapMessage.getExtendedRequest();

                        // Get the Value and store it in the ExtendedRequest
                        TLV tlv = ldapMessageContainer.getCurrentTLV();

                        // We have to handle the special case of a 0 length matched OID
                        if ( tlv.getLength().getLength() == 0 )
                        {
                            log.error( "The name must not be null");
                            throw new DecoderException( "The name must not be null" );
                        }
                        else
                        {
                            extendedRequest.setRequestName( new OID( StringTools.utf8ToString( tlv.getValue().getData() ) ) );
                        }

                        // We can have an END transition
                        ldapMessageContainer.grammarEndAllowed( true );

                        // We can have an Pop transition
                        ldapMessageContainer.grammarPopAllowed( true );

                        if ( log.isDebugEnabled() )
                        {
                            log.debug( "OID read : {}", extendedRequest.getRequestName() );
                        }
                    }
                } );

        // ExtendedRequest ::= [APPLICATION 23] SEQUENCE { 
        //     ...
        //     requestValue     [1] OCTET STRING OPTIONAL } (Tag)
        // Nothing to do
        super.transitions[LdapStatesEnum.EXTENDED_REQUEST_VALUE_TAG][LdapConstants.EXTENDED_REQUEST_VALUE_TAG] = new GrammarTransition(
                LdapStatesEnum.EXTENDED_REQUEST_VALUE_TAG,
                LdapStatesEnum.EXTENDED_REQUEST_VALUE_VALUE, null );

        // ExtendedRequest ::= [APPLICATION 23] SEQUENCE { 
        //     ...
        //     requestValue     [1] OCTET STRING OPTIONAL } (Value)
        // Store the DN to be compared
        super.transitions[LdapStatesEnum.EXTENDED_REQUEST_VALUE_VALUE][LdapConstants.EXTENDED_REQUEST_VALUE_TAG] = new GrammarTransition(
                LdapStatesEnum.EXTENDED_REQUEST_VALUE_VALUE, LdapStatesEnum.END_STATE,
                new GrammarAction( "Store value" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {

                        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
                            container;
                        LdapMessage          ldapMessage          =
                            ldapMessageContainer.getLdapMessage();

                        // We can allocate the ExtendedRequest Object
                        ExtendedRequest extendedRequest = ldapMessage.getExtendedRequest();

                        // Get the Value and store it in the ExtendedRequest
                        TLV tlv = ldapMessageContainer.getCurrentTLV();

                        // We have to handle the special case of a 0 length matched value
                        if ( tlv.getLength().getLength() == 0 )
                        {
                            extendedRequest.setRequestValue( new byte[]{} );
                        }
                        else
                        {
                            extendedRequest.setRequestValue(tlv.getValue().getData() );
                        }

                        // We can have an END transition
                        ldapMessageContainer.grammarEndAllowed( true );

                        // We can have an Pop transition
                        ldapMessageContainer.grammarPopAllowed( true );

                        if ( log.isDebugEnabled() )
                        {
                            log.debug( "Extended value : {}", extendedRequest.getRequestValue() );
                        }
                    }
                } );
    }

    //~ Methods ------------------------------------------------------------------------------------

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
