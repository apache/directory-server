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
package org.apache.directory.shared.ldap.codec.search.controls;


import javax.naming.InvalidNameException;

import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.grammar.IGrammar;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.util.IntegerDecoder;
import org.apache.directory.shared.asn1.util.IntegerDecoderException;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the EntryChangeControl. All the actions are declared in
 * this class. As it is a singleton, these declaration are only done once.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EntryChangeControlGrammar extends AbstractGrammar implements IGrammar
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( EntryChangeControlGrammar.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /** The instance of grammar. EntryChangeControlGrammar is a singleton */
    private static IGrammar instance = new EntryChangeControlGrammar();


    /**
     * Creates a new EntryChangeControlGrammar object.
     */
    private EntryChangeControlGrammar()
    {
        name = EntryChangeControlGrammar.class.getName();
        statesEnum = EntryChangeControlStatesEnum.getInstance();

        // Create the transitions table
        super.transitions = new GrammarTransition[EntryChangeControlStatesEnum.LAST_EC_STATE][256];

        // ============================================================================================
        // Entry Change Control
        // ============================================================================================
        // EntryChangeNotification ::= SEQUENCE { (Tag)
        // ...
        // Nothing to do
        super.transitions[EntryChangeControlStatesEnum.EC_SEQUENCE_TAG][UniversalTag.SEQUENCE_TAG] = new GrammarTransition(
            EntryChangeControlStatesEnum.EC_SEQUENCE_TAG, EntryChangeControlStatesEnum.EC_SEQUENCE_VALUE, null );

        // ============================================================================================
        // Entry Change Control
        // ============================================================================================
        // EntryChangeNotification ::= SEQUENCE { (Value)
        // ...
        // Initialization of the structure
        super.transitions[EntryChangeControlStatesEnum.EC_SEQUENCE_VALUE][UniversalTag.SEQUENCE_TAG] = new GrammarTransition(
            EntryChangeControlStatesEnum.EC_SEQUENCE_VALUE, EntryChangeControlStatesEnum.CHANGE_TYPE_TAG,
            new GrammarAction( "Init EntryChangeControl" )
            {
                public void action( IAsn1Container container )
                {
                    EntryChangeControlContainer entryChangeContainer = ( EntryChangeControlContainer ) container;
                    EntryChangeControl control = new EntryChangeControl();
                    entryChangeContainer.setEntryChangeControl( control );
                }
            } );

        // ============================================================================================
        // Change Type
        // ============================================================================================
        // EntryChangeNotification ::= SEQUENCE {
        // changeType ENUMERATED { (Tag) },
        // ...
        //
        // Nothing to do
        super.transitions[EntryChangeControlStatesEnum.CHANGE_TYPE_TAG][UniversalTag.ENUMERATED_TAG] = new GrammarTransition(
            EntryChangeControlStatesEnum.CHANGE_TYPE_TAG, EntryChangeControlStatesEnum.CHANGE_TYPE_VALUE, null );

        // ============================================================================================
        // Change Type
        // ============================================================================================
        // EntryChangeNotification ::= SEQUENCE {
        // changeType ENUMERATED { (Value) },
        // ...
        //
        // Evaluates the changeType

        // Action associated with the ChangeType transition
        GrammarAction setChangeTypeAction = new GrammarAction( "Set EntryChangeControl changeType" )
        {
            public void action( IAsn1Container container ) throws DecoderException
            {
                EntryChangeControlContainer entryChangeContainer = ( EntryChangeControlContainer ) container;
                Value value = entryChangeContainer.getCurrentTLV().getValue();

                try
                {
                    int change = IntegerDecoder.parse( value, 1, 8 );

                    switch ( change )
                    {
                        case ChangeType.ADD_VALUE:
                        case ChangeType.DELETE_VALUE:
                        case ChangeType.MODDN_VALUE:
                        case ChangeType.MODIFY_VALUE:
                            ChangeType changeType = ChangeType.getChangeType( change );

                            if ( IS_DEBUG )
                            {
                                log.debug( "changeType = " + changeType );
                            }

                            entryChangeContainer.getEntryChangeControl().setChangeType( changeType );
                            break;

                        default:
                            String msg = "failed to decode the changeType for EntryChangeControl";
                            log.error( msg );
                            throw new DecoderException( msg );
                    }

                    // We can have an END transition
                    entryChangeContainer.grammarEndAllowed( true );
                }
                catch ( IntegerDecoderException e )
                {
                    String msg = "failed to decode the changeType for EntryChangeControl";
                    log.error( msg, e );
                    throw new DecoderException( msg );
                }
            }
        };

        // ChangeType Transition
        super.transitions[EntryChangeControlStatesEnum.CHANGE_TYPE_VALUE][UniversalTag.ENUMERATED_TAG] = new GrammarTransition(
            EntryChangeControlStatesEnum.CHANGE_TYPE_VALUE,
            EntryChangeControlStatesEnum.CHANGE_NUMBER_OR_PREVIOUS_DN_TAG, setChangeTypeAction );

        // ============================================================================================
        // Previous DN (We have a OCTET_STRING Tag)
        // ============================================================================================
        // EntryChangeNotification ::= SEQUENCE {
        // ...
        // previousDN LDAPDN OPTIONAL, (Tag)
        // ...
        //
        // Nothing to do
        super.transitions[EntryChangeControlStatesEnum.CHANGE_NUMBER_OR_PREVIOUS_DN_TAG][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            EntryChangeControlStatesEnum.CHANGE_NUMBER_OR_PREVIOUS_DN_TAG,
            EntryChangeControlStatesEnum.PREVIOUS_DN_VALUE, null );

        // ============================================================================================
        // Previous DN
        // ============================================================================================
        // EntryChangeNotification ::= SEQUENCE {
        // ...
        // previousDN LDAPDN OPTIONAL, (Value)
        // ...
        //
        // Set the previousDN into the structure. We first check that it's a
        // valid DN

        // Action associated with the PreviousDN transition
        GrammarAction setPreviousDnAction = new GrammarAction( "Set EntryChangeControl previousDN" )
        {
            public void action( IAsn1Container container ) throws DecoderException
            {
                EntryChangeControlContainer entryChangeContainer = ( EntryChangeControlContainer ) container;

                ChangeType changeType = entryChangeContainer.getEntryChangeControl().getChangeType();

                if ( changeType != ChangeType.MODDN )
                {
                    log.error( "The previousDN field should not contain anything if the changeType is not MODDN" );
                    throw new DecoderException( "Previous DN is not allowed for this change type" );
                }
                else
                {
                    Value value = entryChangeContainer.getCurrentTLV().getValue();
                    LdapDN previousDn = null;

                    try
                    {
                        previousDn = new LdapDN( StringTools.utf8ToString( value.getData() ) );
                    }
                    catch ( InvalidNameException ine )
                    {
                        log.error( "Bad Previous DN : '" + StringTools.dumpBytes( value.getData() ) );
                        throw new DecoderException( "failed to decode the previous DN" );
                    }

                    if ( IS_DEBUG )
                    {
                        log.debug( "previousDN = " + previousDn );
                    }

                    entryChangeContainer.getEntryChangeControl().setPreviousDn( previousDn );

                    // We can have an END transition
                    entryChangeContainer.grammarEndAllowed( true );
                }
            }
        };

        // PreviousDN transition
        super.transitions[EntryChangeControlStatesEnum.PREVIOUS_DN_VALUE][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            EntryChangeControlStatesEnum.PREVIOUS_DN_VALUE, EntryChangeControlStatesEnum.CHANGE_NUMBER_TAG,
            setPreviousDnAction );

        // ============================================================================================
        // Change Number from Change Type
        // ============================================================================================
        // EntryChangeNotification ::= SEQUENCE {
        // ...
        // changeNumber INTEGER OPTIONAL (Tag)
        // }
        //
        // Nothing to do
        super.transitions[EntryChangeControlStatesEnum.CHANGE_NUMBER_OR_PREVIOUS_DN_TAG][UniversalTag.INTEGER_TAG] = new GrammarTransition(
            EntryChangeControlStatesEnum.CHANGE_NUMBER_OR_PREVIOUS_DN_TAG,
            EntryChangeControlStatesEnum.CHANGE_NUMBER_VALUE, null );

        // ============================================================================================
        // Change Number from PreviousDN
        // ============================================================================================
        // EntryChangeNotification ::= SEQUENCE {
        // ...
        // changeNumber INTEGER OPTIONAL (Tag)
        // }
        //
        // Nothing to do
        super.transitions[EntryChangeControlStatesEnum.CHANGE_NUMBER_TAG][UniversalTag.INTEGER_TAG] = new GrammarTransition(
            EntryChangeControlStatesEnum.CHANGE_NUMBER_TAG, EntryChangeControlStatesEnum.CHANGE_NUMBER_VALUE, null );

        // ============================================================================================
        // Change Number
        // ============================================================================================
        // EntryChangeNotification ::= SEQUENCE {
        // ...
        // changeNumber INTEGER OPTIONAL (Value)
        // }
        //
        // Set the changeNumber into the structure

        // Change Number action
        GrammarAction setChangeNumberAction = new GrammarAction( "Set EntryChangeControl changeNumber" )
        {
            public void action( IAsn1Container container ) throws DecoderException
            {
                EntryChangeControlContainer entryChangeContainer = ( EntryChangeControlContainer ) container;
                Value value = entryChangeContainer.getCurrentTLV().getValue();

                try
                {
                    int changeNumber = IntegerDecoder.parse( value );

                    if ( IS_DEBUG )
                    {
                        log.debug( "changeNumber = " + changeNumber );
                    }

                    entryChangeContainer.getEntryChangeControl().setChangeNumber( changeNumber );

                    // We can have an END transition
                    entryChangeContainer.grammarEndAllowed( true );
                }
                catch ( IntegerDecoderException e )
                {
                    String msg = "failed to decode the changeNumber for EntryChangeControl";
                    log.error( msg, e );
                    throw new DecoderException( msg );
                }
            }
        };

        // Transition
        super.transitions[EntryChangeControlStatesEnum.CHANGE_NUMBER_VALUE][UniversalTag.INTEGER_TAG] = new GrammarTransition(
            EntryChangeControlStatesEnum.CHANGE_NUMBER_VALUE, EntryChangeControlStatesEnum.GRAMMAR_END,
            setChangeNumberAction );
    }


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
