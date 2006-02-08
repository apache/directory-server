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
package org.apache.directory.shared.ldap.codec.unbind;

import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.grammar.IGrammar;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapMessage;
import org.apache.directory.shared.ldap.codec.LdapMessageContainer;
import org.apache.directory.shared.ldap.codec.LdapStatesEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the UnBindRequest LDAP message. All the actions are declared in this
 * class. As it is a singleton, these declaration are only done once.
 * 
 * If an action is to be added or modified, this is where the work is to be done !
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class UnBindRequestGrammar extends AbstractGrammar implements IGrammar
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( UnBindRequestGrammar.class );

    /** The instance of grammar. UnBindRequestGrammar is a singleton */
    private static IGrammar instance = new UnBindRequestGrammar();

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Get the instance of this grammar
     *
     * @return An instance on the UnBindRequest Grammar
     */
    public static IGrammar getInstance()
    {
        return instance;
    }

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new UnBindRequestGrammar object.
     */
    private UnBindRequestGrammar()
    {

        name = UnBindRequestGrammar.class.getName();
        
        statesEnum = LdapStatesEnum.getInstance();

        // Create the transitions table
        super.transitions = new GrammarTransition[LdapStatesEnum.LAST_UNBIND_REQUEST_STATE][256];

        //============================================================================================
        // protocolOp : UnBind Request
        //============================================================================================
        // LdapMessage ::= ... UnBindRequest ...
        // UnbindRequest ::= [APPLICATION 2] NULL (Length)
        super.transitions[LdapStatesEnum.UNBIND_REQUEST_TAG][LdapConstants.UNBIND_REQUEST_TAG]    = new GrammarTransition(
                LdapStatesEnum.UNBIND_REQUEST_TAG, LdapStatesEnum.UNBIND_REQUEST_VALUE, null );

        // LdapMessage ::= ... UnBindRequest ...
        // UnbindRequest ::= [APPLICATION 2] NULL (Value)
        // We have to check that the length is null (the Value is empty). This is the end of this grammar.
        // We also have to allocate a UnBindRequest
        super.transitions[LdapStatesEnum.UNBIND_REQUEST_VALUE][LdapConstants.UNBIND_REQUEST_TAG] = new GrammarTransition(
                LdapStatesEnum.UNBIND_REQUEST_VALUE, LdapStatesEnum.GRAMMAR_END,
                new GrammarAction( "Init UnBindRequest" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {

                        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
                            container;
                        LdapMessage      ldapMessage          =
                            ldapMessageContainer.getLdapMessage();

                        // Now, we can allocate the UnBindRequest Object
                        UnBindRequest unBindRequest = new UnBindRequest();

                        // As this is a new Constructed object, we have to init its length
                        TLV tlv            = ldapMessageContainer.getCurrentTLV();
                        int expectedLength = tlv.getLength().getLength();
                        
                        // If the length is not null, this is an error.
                        if ( expectedLength != 0 )
                        {
                            log.error( "The length of a UnBindRequest must be null, the actual value is {}", new Integer( expectedLength ) );
                            throw new DecoderException( "The length of a UnBindRequest must be null" );
                        }

                        unBindRequest.setParent( ldapMessage );

                        // We can have an END transition
                        ldapMessageContainer.grammarEndAllowed( true );
                        
                        // We can have a Pop transition
                        ldapMessageContainer.grammarPopAllowed( true );
                        
                        // And we associate it to the ldapMessage Object
                        ldapMessage.setProtocolOP( unBindRequest );
                    }
                } );
    }
}
