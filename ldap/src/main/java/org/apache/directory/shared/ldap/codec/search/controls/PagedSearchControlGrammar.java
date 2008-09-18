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


import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.grammar.IGrammar;
import org.apache.directory.shared.asn1.ber.grammar.IStates;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.util.IntegerDecoder;
import org.apache.directory.shared.asn1.util.IntegerDecoderException;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the PagedSearchControl. All the actions are declared in
 * this class. As it is a singleton, these declaration are only done once.
 * 
 * The decoded grammar is the following :
 * 
 * realSearchControlValue ::= SEQUENCE {
 *     size   INTEGER,
 *     cookie OCTET STRING,
 * }
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 664290 $, $Date: 2008-06-07 08:28:06 +0200 (Sat, 07 Jun 2008) $, 
 */
public class PagedSearchControlGrammar extends AbstractGrammar
{
    /** The logger */
    static final Logger log = LoggerFactory.getLogger( PagedSearchControlGrammar.class );

    /** Speedup for logs */
    static final boolean IS_DEBUG = log.isDebugEnabled();

    /** The instance of grammar. PagedSearchControlGrammar is a singleton */
    private static IGrammar instance = new PagedSearchControlGrammar();


    /**
     * Creates a new PagedSearchControlGrammar object.
     */
    private PagedSearchControlGrammar()
    {
        name = PagedSearchControlGrammar.class.getName();
        statesEnum = PagedSearchControlStatesEnum.getInstance();

        // Create the transitions table
        super.transitions = new GrammarTransition[PagedSearchControlStatesEnum.LAST_PAGED_SEARCH_STATE][256];

        /** 
         * Transition from initial state to PagedSearch sequence
         * realSearchControlValue ::= SEQUENCE OF {
         *     ...
         *     
         * Initialize the persistence search object
         */
        super.transitions[IStates.INIT_GRAMMAR_STATE][UniversalTag.SEQUENCE_TAG] = 
            new GrammarTransition( IStates.INIT_GRAMMAR_STATE, 
                                    PagedSearchControlStatesEnum.PAGED_SEARCH_SEQUENCE_STATE, 
                                    UniversalTag.SEQUENCE_TAG, 
                new GrammarAction( "Init PagedSearchControl" )
            {
                public void action( IAsn1Container container )
                {
                    PagedSearchControlContainer pagedSearchContainer = ( PagedSearchControlContainer ) container;
                    PagedSearchControlCodec control = new PagedSearchControlCodec();
                    pagedSearchContainer.setPagedSearchControl( control );
                }
            } );


        /** 
         * Transition from PagedSearch sequence to size
         * 
         * realSearchControlValue ::= SEQUENCE OF {
         *     size  INTEGER,  -- INTEGER (0..maxInt),
         *     ...
         *     
         * Stores the size value
         */
        super.transitions[PagedSearchControlStatesEnum.PAGED_SEARCH_SEQUENCE_STATE][UniversalTag.INTEGER_TAG] = 
            new GrammarTransition( PagedSearchControlStatesEnum.PAGED_SEARCH_SEQUENCE_STATE, 
                PagedSearchControlStatesEnum.SIZE_STATE, 
                UniversalTag.INTEGER_TAG,
                new GrammarAction( "Set PagedSearchControl size" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    PagedSearchControlContainer pagedSearchContainer = ( PagedSearchControlContainer ) container;
                    Value value = pagedSearchContainer.getCurrentTLV().getValue();

                    try
                    {
                        // Check that the value is into the allowed interval
                        int size = IntegerDecoder.parse( value, 0, Integer.MAX_VALUE );
                        
                        if ( IS_DEBUG )
                        {
                            log.debug( "size = " + size );
                        }

                        pagedSearchContainer.getPagedSearchControl().setSize( size );
                    }
                    catch ( IntegerDecoderException e )
                    {
                        String msg = "failed to decode the size for PagedSearchControl";
                        log.error( msg, e );
                        throw new DecoderException( msg );
                    }
                }
            } );

        /** 
         * Transition from size to cookie
         * realSearchControlValue ::= SEQUENCE OF {
         *     ...
         *     cookie   OCTET STRING
         * }
         *     
         * Stores the cookie flag
         */
        super.transitions[PagedSearchControlStatesEnum.SIZE_STATE][UniversalTag.OCTET_STRING_TAG] = 
            new GrammarTransition( PagedSearchControlStatesEnum.SIZE_STATE,
                                    PagedSearchControlStatesEnum.COOKIE_STATE, UniversalTag.OCTET_STRING_TAG,
                new GrammarAction( "Set PagedSearchControl cookie" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    PagedSearchControlContainer pagedSearchContainer = ( PagedSearchControlContainer ) container;
                    Value value = pagedSearchContainer.getCurrentTLV().getValue();

                    if ( pagedSearchContainer.getCurrentTLV().getLength() == 0 )
                    {
                        pagedSearchContainer.getPagedSearchControl().setCookie( StringTools.EMPTY_BYTES );
                    }
                    else
                    {
                        pagedSearchContainer.getPagedSearchControl().setCookie( value.getData() );
                    }

                    // We can have an END transition
                    pagedSearchContainer.grammarEndAllowed( true );
                }
            } );
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
