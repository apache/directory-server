/*
 *   Copyright 2006 The Apache Software Foundation
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
package org.apache.directory.shared.ldap.codec.search;


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
 * This class implements the SubstringFilter grammar. All the actions are
 * declared in this class. As it is a singleton, these declaration are only done
 * once. If an action is to be added or modified, this is where the work is to
 * be done !
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SubstringFilterGrammar extends AbstractGrammar implements IGrammar
{
    // ~ Static fields/initializers
    // -----------------------------------------------------------------

    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( SubstringFilterGrammar.class );

    /** The instance of grammar. SubstringFilterGrammar is a singleton */
    private static IGrammar instance = new SubstringFilterGrammar();


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new SubstringFilterGrammar object.
     */
    private SubstringFilterGrammar()
    {
        name = SubstringFilterGrammar.class.getName();
        statesEnum = LdapStatesEnum.getInstance();

        // Create the transitions table
        super.transitions = new GrammarTransition[LdapStatesEnum.LAST_SUBSTRING_FILTER_STATE][256];

        // SubstringFilter ::= [4] SEQUENCE { (Tag)
        // Nothing to do
        super.transitions[LdapStatesEnum.SUBSTRINGS_FILTER_TAG][LdapConstants.SUBSTRINGS_FILTER_TAG] = new GrammarTransition(
            LdapStatesEnum.SUBSTRINGS_FILTER_TAG, LdapStatesEnum.SUBSTRINGS_FILTER_VALUE, null );

        // Here we are dealing with substrings. LDAP grammar is not very
        // explicit about
        // what is allowed (-- at least one must be present !!!), while RFC 2254
        // is
        // really clear. But LDAP grammar is the one to follow...
        //
        // substring ::= attr "=" [AttributeValue] any [AttributeValue]
        // any ::= "*" *(AttributeValue "*")
        //
        // SubstringFilter ::= [4] SEQUENCE { (Value)
        // Nothing to do
        // ...
        // Store the substring
        super.transitions[LdapStatesEnum.SUBSTRINGS_FILTER_VALUE][LdapConstants.SUBSTRINGS_FILTER_TAG] = new GrammarTransition(
            LdapStatesEnum.SUBSTRINGS_FILTER_VALUE, LdapStatesEnum.SUBSTRINGS_FILTER_TYPE_TAG, new GrammarAction(
                "Init Substring Filter" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
                    LdapMessage ldapMessage = ldapMessageContainer.getLdapMessage();
                    SearchRequest searchRequest = ldapMessage.getSearchRequest();

                    TLV tlv = ldapMessageContainer.getCurrentTLV();

                    if ( tlv.getLength().getLength() == 0 )
                    {
                        log.error( "The Substring filter PDU must not be empty" );
                        throw new DecoderException( "The Substring filter PDU must not be empty" );
                    }

                    // We can allocate the SearchRequest
                    Filter substringFilter = new SubstringFilter();

                    // Get the parent, if any
                    Filter currentFilter = searchRequest.getCurrentFilter();

                    if ( currentFilter != null )
                    {
                        // Ok, we have a parent. The new Filter will be added to
                        // this parent, then.
                        ( ( ConnectorFilter ) currentFilter ).addFilter( substringFilter );
                        substringFilter.setParent( currentFilter );
                    }
                    else
                    {
                        // No parent. This Filter will become the root.

                        searchRequest.setFilter( substringFilter );
                        substringFilter.setParent( searchRequest );
                    }

                    searchRequest.setCurrentFilter( substringFilter );

                    // As this is a new Constructed object, we have to init its
                    // length
                    int expectedLength = tlv.getLength().getLength();

                    substringFilter.setExpectedLength( expectedLength );
                    substringFilter.setCurrentLength( 0 );
                }
            } );

        // SubstringFilter ::= SEQUENCE {
        // type AttributeDescription, (Tag)
        // ...
        // Nothing to do
        super.transitions[LdapStatesEnum.SUBSTRINGS_FILTER_TYPE_TAG][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            LdapStatesEnum.SUBSTRINGS_FILTER_TYPE_TAG, LdapStatesEnum.SUBSTRINGS_FILTER_TYPE_VALUE, null );

        // SubstringFilter ::= SEQUENCE {
        // type AttributeDescription, (Value)
        // ...
        // 
        super.transitions[LdapStatesEnum.SUBSTRINGS_FILTER_TYPE_VALUE][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            LdapStatesEnum.SUBSTRINGS_FILTER_TYPE_VALUE, LdapStatesEnum.SUBSTRINGS_FILTER_SUBSTRINGS_SEQ_TAG,
            new GrammarAction( "Store substring filter Value" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
                    LdapMessage ldapMessage = ldapMessageContainer.getLdapMessage();
                    SearchRequest searchRequest = ldapMessage.getSearchRequest();

                    TLV tlv = ldapMessageContainer.getCurrentTLV();

                    // Store the value.
                    SubstringFilter substringFilter = ( SubstringFilter ) searchRequest.getCurrentFilter();

                    if ( tlv.getLength().getLength() == 0 )
                    {
                        log.error( "The attribute description should not be null" );
                        throw new DecoderException( "The attribute description should not be null" );
                    }
                    else
                    {
                        try
                        {
                            LdapString type = LdapDN.normalizeAttribute( tlv.getValue().getData() );
                            substringFilter.setType( type );
                        }
                        catch ( LdapStringEncodingException lsee )
                        {
                            String msg = StringTools.dumpBytes( tlv.getValue().getData() );
                            log.error( "The substring filter type ({}) is invalid", msg );
                            throw new DecoderException( "Invalid substring filter type " + msg + ", : "
                                + lsee.getMessage() );
                        }

                        // We now have to get back to the nearest filter which
                        // is not terminal.
                        unstackFilters( container );
                    }
                }
            } );

        // SubstringFilter ::= SEQUENCE {
        // ...
        // -- at least one must be present
        // substrings SEQUENCE OF CHOICE { (Tag)
        // ...
        // Nothing to do.
        super.transitions[LdapStatesEnum.SUBSTRINGS_FILTER_SUBSTRINGS_SEQ_TAG][UniversalTag.SEQUENCE_TAG] = new GrammarTransition(
            LdapStatesEnum.SUBSTRINGS_FILTER_SUBSTRINGS_SEQ_TAG, LdapStatesEnum.SUBSTRINGS_FILTER_SUBSTRINGS_SEQ_VALUE,
            null );

        // SubstringFilter ::= SEQUENCE {
        // ...
        // -- at least one must be present
        // substrings SEQUENCE OF CHOICE { (Value)
        // ...
        // Here, we may have three possibilities. We may have an "initial"
        // value,
        // or an "any" value, or a "final" value. Any other option is an error.
        // We must have at least one of those three.
        super.transitions[LdapStatesEnum.SUBSTRINGS_FILTER_SUBSTRINGS_SEQ_VALUE][UniversalTag.SEQUENCE_TAG] = new GrammarTransition(
            LdapStatesEnum.SUBSTRINGS_FILTER_SUBSTRINGS_SEQ_VALUE,
            LdapStatesEnum.SUBSTRINGS_FILTER_SUBSTRINGS_INITIAL_OR_ANY_OR_FINAL_TAG, new GrammarAction(
                "Substring Filter substringsSequence " )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;

                    TLV tlv = ldapMessageContainer.getCurrentTLV();

                    if ( tlv.getLength().getLength() == 0 )
                    {
                        log.error( "The substrings sequence is empty" );
                        throw new DecoderException( "The substring sequence is empty" );
                    }
                }
            } );

        // SubstringFilter ::= SEQUENCE {
        // ...
        // -- at least one must be present
        // substrings SEQUENCE OF CHOICE {
        // initial [0] LDAPString, (Tag)
        // ...
        // We have an "initial" value. Nothing to do.
        super.transitions[LdapStatesEnum.SUBSTRINGS_FILTER_SUBSTRINGS_INITIAL_OR_ANY_OR_FINAL_TAG][LdapConstants.SUBSTRINGS_FILTER_INITIAL_TAG] = new GrammarTransition(
            LdapStatesEnum.SUBSTRINGS_FILTER_SUBSTRINGS_INITIAL_OR_ANY_OR_FINAL_TAG,
            LdapStatesEnum.SUBSTRINGS_FILTER_SUBSTRINGS_INITIAL_VALUE, null );

        // SubstringFilter ::= SEQUENCE {
        // ...
        // -- at least one must be present
        // substrings SEQUENCE OF CHOICE {
        // initial [0] LDAPString, (Value)
        // ...
        // Store the initial value.
        super.transitions[LdapStatesEnum.SUBSTRINGS_FILTER_SUBSTRINGS_INITIAL_VALUE][LdapConstants.SUBSTRINGS_FILTER_INITIAL_TAG] = new GrammarTransition(
            LdapStatesEnum.SUBSTRINGS_FILTER_SUBSTRINGS_INITIAL_VALUE,
            LdapStatesEnum.SUBSTRINGS_FILTER_SUBSTRINGS_ANY_OR_FINAL_TAG, new GrammarAction(
                "Store substring filter initial Value" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
                    LdapMessage ldapMessage = ldapMessageContainer.getLdapMessage();
                    SearchRequest searchRequest = ldapMessage.getSearchRequest();

                    TLV tlv = ldapMessageContainer.getCurrentTLV();

                    // Store the value.
                    SubstringFilter substringFilter = ( SubstringFilter ) searchRequest.getCurrentFilter();

                    if ( tlv.getLength().getLength() == 0 )
                    {
                        log.error( "The substring initial filter is empty" );
                        throw new DecoderException( "The substring initial filter is empty" );
                    }

                    try
                    {
                        substringFilter.setInitialSubstrings( new LdapString( tlv.getValue().getData() ) );
                    }
                    catch ( LdapStringEncodingException lsee )
                    {
                        String msg = StringTools.dumpBytes( tlv.getValue().getData() );
                        log.error( "The substring filter initial ({}) is invalid" );
                        throw new DecoderException( "Invalid substring filter initial " + msg + ", : "
                            + lsee.getMessage() );
                    }

                    // We now have to get back to the nearest filter which is
                    // not terminal.
                    unstackFilters( container );

                    // We can have a pop transition
                    container.grammarPopAllowed( true );
                }
            } );

        // SubstringFilter ::= SEQUENCE {
        // ...
        // -- at least one must be present
        // substrings SEQUENCE OF CHOICE {
        // (void)
        // any [1] LDAPString, (Tag)
        // ...
        // We have an "any" value. Nothing to do.
        super.transitions[LdapStatesEnum.SUBSTRINGS_FILTER_SUBSTRINGS_INITIAL_OR_ANY_OR_FINAL_TAG][LdapConstants.SUBSTRINGS_FILTER_ANY_TAG] = new GrammarTransition(
            LdapStatesEnum.SUBSTRINGS_FILTER_SUBSTRINGS_INITIAL_OR_ANY_OR_FINAL_TAG,
            LdapStatesEnum.SUBSTRINGS_FILTER_SUBSTRINGS_ANY_VALUE, null );

        // SubstringFilter ::= SEQUENCE {
        // ...
        // -- at least one must be present
        // substrings SEQUENCE OF CHOICE {
        // ...
        // any [1] LDAPString, (Tag)
        // ...
        // We have an "any" value after a "initial" value. Nothing to do.
        super.transitions[LdapStatesEnum.SUBSTRINGS_FILTER_SUBSTRINGS_ANY_OR_FINAL_TAG][LdapConstants.SUBSTRINGS_FILTER_ANY_TAG] = new GrammarTransition(
            LdapStatesEnum.SUBSTRINGS_FILTER_SUBSTRINGS_ANY_OR_FINAL_TAG,
            LdapStatesEnum.SUBSTRINGS_FILTER_SUBSTRINGS_ANY_VALUE, null );

        // SubstringFilter ::= SEQUENCE {
        // ...
        // -- at least one must be present
        // substrings SEQUENCE OF CHOICE {
        // ...
        // any [1] LDAPString, (Value)
        // ...
        // Store the 'any' value.
        super.transitions[LdapStatesEnum.SUBSTRINGS_FILTER_SUBSTRINGS_ANY_VALUE][LdapConstants.SUBSTRINGS_FILTER_ANY_TAG] = new GrammarTransition(
            LdapStatesEnum.SUBSTRINGS_FILTER_SUBSTRINGS_ANY_VALUE,
            LdapStatesEnum.SUBSTRINGS_FILTER_SUBSTRINGS_ANY_OR_FINAL_TAG, new GrammarAction(
                "Store substring filter any Value" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
                    LdapMessage ldapMessage = ldapMessageContainer.getLdapMessage();
                    SearchRequest searchRequest = ldapMessage.getSearchRequest();

                    TLV tlv = ldapMessageContainer.getCurrentTLV();

                    // Store the value.
                    SubstringFilter substringFilter = ( SubstringFilter ) searchRequest.getCurrentFilter();

                    if ( tlv.getLength().getLength() == 0 )
                    {
                        log.error( "The substring any filter is empty" );
                        throw new DecoderException( "The substring any filter is empty" );
                    }

                    try
                    {
                        substringFilter.addAnySubstrings( new LdapString( tlv.getValue().getData() ) );
                    }
                    catch ( LdapStringEncodingException lsee )
                    {
                        String msg = StringTools.dumpBytes( tlv.getValue().getData() );
                        log.error( "The substring any filter ({}) is invalid", msg );
                        throw new DecoderException( "Invalid substring any filter " + msg + ", : " + lsee.getMessage() );
                    }

                    // We now have to get back to the nearest filter which is
                    // not terminal.
                    unstackFilters( container );

                    // We can have a pop transition
                    container.grammarPopAllowed( true );
                }
            } );

        // SubstringFilter ::= SEQUENCE {
        // ...
        // -- at least one must be present
        // substrings SEQUENCE OF CHOICE {
        // ...
        // any [1] LDAPString,
        // final [2] LDAPString, (Tag)
        // }
        //
        // We have an 'final' value after an 'any' value. Nothing to do.
        super.transitions[LdapStatesEnum.SUBSTRINGS_FILTER_SUBSTRINGS_ANY_OR_FINAL_TAG][LdapConstants.SUBSTRINGS_FILTER_FINAL_TAG] = new GrammarTransition(
            LdapStatesEnum.SUBSTRINGS_FILTER_SUBSTRINGS_ANY_OR_FINAL_TAG,
            LdapStatesEnum.SUBSTRINGS_FILTER_SUBSTRINGS_FINAL_VALUE, null );

        // SubstringFilter ::= SEQUENCE {
        // ...
        // -- at least one must be present
        // substrings SEQUENCE OF CHOICE {
        // (void),
        // (void),
        // final [2] LDAPString, (Tag)
        // }
        //
        // We have only a final falue. Nothing to do.
        super.transitions[LdapStatesEnum.SUBSTRINGS_FILTER_SUBSTRINGS_INITIAL_OR_ANY_OR_FINAL_TAG][LdapConstants.SUBSTRINGS_FILTER_FINAL_TAG] = new GrammarTransition(
            LdapStatesEnum.SUBSTRINGS_FILTER_SUBSTRINGS_INITIAL_OR_ANY_OR_FINAL_TAG,
            LdapStatesEnum.SUBSTRINGS_FILTER_SUBSTRINGS_FINAL_VALUE, null );

        // SubstringFilter ::= SEQUENCE {
        // ...
        // -- at least one must be present
        // substrings SEQUENCE OF CHOICE {
        // ...
        // final [2] LDAPString, (Value)
        // Store the initial value.
        super.transitions[LdapStatesEnum.SUBSTRINGS_FILTER_SUBSTRINGS_FINAL_VALUE][LdapConstants.SUBSTRINGS_FILTER_FINAL_TAG] = new GrammarTransition(
            LdapStatesEnum.SUBSTRINGS_FILTER_SUBSTRINGS_FINAL_VALUE, LdapStatesEnum.END_STATE, new GrammarAction(
                "Store substring filter final Value" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
                    LdapMessage ldapMessage = ldapMessageContainer.getLdapMessage();
                    SearchRequest searchRequest = ldapMessage.getSearchRequest();

                    TLV tlv = ldapMessageContainer.getCurrentTLV();

                    // Store the value.
                    SubstringFilter substringFilter = ( SubstringFilter ) searchRequest.getCurrentFilter();

                    if ( tlv.getLength().getLength() == 0 )
                    {
                        log.error( "The substring final filter is empty" );
                        throw new DecoderException( "The substring final filter is empty" );
                    }

                    try
                    {
                        substringFilter.setFinalSubstrings( new LdapString( tlv.getValue().getData() ) );
                    }
                    catch ( LdapStringEncodingException lsee )
                    {
                        String msg = StringTools.dumpBytes( tlv.getValue().getData() );
                        log.error( "The substring final filter ({}) is invalid", msg );
                        throw new DecoderException( "Invalid substring final filter " + msg + ", : "
                            + lsee.getMessage() );
                    }

                    // We now have to get back to the nearest filter which is
                    // not terminal.
                    unstackFilters( container );
                    // We can have a pop transition
                    container.grammarPopAllowed( true );
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


    /**
     * This method is used to clear the filter's stack for terminated elements.
     * An element is considered as terminated either if : - it's a final element
     * (ie an element which cannot contains a Filter) - its current length
     * equals its expected length.
     * 
     * @param container
     *            The container being decoded
     */
    private void unstackFilters( IAsn1Container container )
    {
        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
        LdapMessage ldapMessage = ldapMessageContainer.getLdapMessage();
        SearchRequest searchRequest = ldapMessage.getSearchRequest();

        TLV tlv = ldapMessageContainer.getCurrentTLV();

        // Get the parent, if any
        Filter currentFilter = searchRequest.getCurrentFilter();

        // We know have to check if the parent has been completed
        if ( tlv.getParent().getExpectedLength() == 0 )
        {
            TLV parent = tlv.getParent();

            // The parent has been completed, we have to switch it
            while ( ( parent != null ) && ( parent.getExpectedLength() == 0 ) )
            {
                parent = parent.getParent();

                if ( ( currentFilter != null ) && ( currentFilter.getParent() instanceof Filter ) )
                {
                    currentFilter = ( Filter ) currentFilter.getParent();
                }
                else
                {
                    currentFilter = null;
                    break;
                }
            }

            searchRequest.setCurrentFilter( currentFilter );
        }
    }
}
