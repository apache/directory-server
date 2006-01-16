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
package org.apache.ldap.common.codec.add;

import org.apache.asn1.ber.grammar.IGrammar;
import org.apache.asn1.ber.grammar.AbstractGrammar;
import org.apache.asn1.ber.grammar.GrammarTransition;
import org.apache.asn1.ber.grammar.GrammarAction;
import org.apache.asn1.ber.IAsn1Container;
import org.apache.asn1.ber.tlv.TLV;
import org.apache.asn1.ber.tlv.UniversalTag;
import org.apache.asn1.codec.DecoderException;
import org.apache.ldap.common.codec.LdapConstants;
import org.apache.ldap.common.codec.LdapMessage;
import org.apache.ldap.common.codec.LdapMessageContainer;
import org.apache.ldap.common.codec.LdapStatesEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the AddResponse LDAP message. All the actions are declared in this
 * class. As it is a singleton, these declaration are only done once.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AddResponseGrammar extends AbstractGrammar implements IGrammar
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( AddResponseGrammar.class );

    /** The instance of grammar. AddResponseGrammar is a singleton */
    private static IGrammar instance = new AddResponseGrammar();

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new AddResponseGrammar object.
     */
    private AddResponseGrammar()
    {
        name = AddResponseGrammar.class.getName();
        statesEnum = LdapStatesEnum.getInstance();

        // Create the transitions table
        super.transitions = new GrammarTransition[LdapStatesEnum.LAST_ADD_RESPONSE_STATE][256];

        //============================================================================================
        // AddResponse Message
        //============================================================================================
        // LdapMessage ::= ... AddResponse ...
        // AddResponse ::= [APPLICATION 9] LDAPResult (Tag)
        // Nothing to do.
        super.transitions[LdapStatesEnum.ADD_RESPONSE_TAG][LdapConstants.ADD_RESPONSE_TAG] = new GrammarTransition(
                LdapStatesEnum.ADD_RESPONSE_TAG, LdapStatesEnum.ADD_RESPONSE_VALUE, null );

        // LdapMessage ::= ... AddResponse ...
        // AddResponse ::= [APPLICATION 9] LDAPResult (Value)
        // The next Tag will be the LDAPResult Tag (0x0A).
        // We will switch the grammar then.
        super.transitions[LdapStatesEnum.ADD_RESPONSE_VALUE][LdapConstants.ADD_RESPONSE_TAG] = new GrammarTransition(
                LdapStatesEnum.ADD_RESPONSE_VALUE, LdapStatesEnum.ADD_RESPONSE_LDAP_RESULT, 
                new GrammarAction( "Init AddResponse" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {

                        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
                            container;
                        LdapMessage      ldapMessage          =
                            ldapMessageContainer.getLdapMessage();

                        // We will check that the request is not null
                        TLV   tlv       = ldapMessageContainer.getCurrentTLV();

                        if ( tlv.getLength().getLength() == 0 )
                        {
                        	String msg = "The AddResponse must not be null";
                        	log.error( msg );
                        	throw new DecoderException( msg );
                        }
                        
                        // Now, we can allocate the AddRequest Object
                        AddResponse addResponse = new AddResponse();

                        // As this is a new Constructed object, we have to init its length
                        int expectedLength = tlv.getLength().getLength();
                        addResponse.setExpectedLength( expectedLength );
                        addResponse.setCurrentLength( 0 );
                        addResponse.setParent( ldapMessage );

                        // And we associate it to the ldapMessage Object
                        ldapMessage.setProtocolOP( addResponse );
                        
                        log.debug( "Add Response" );
                    }
                } );

        // LdapMessage ::= ... AddResponse ...
        // AddResponse ::= [APPLICATION 9] LDAPResult (Value)
        // Ok, we have a LDAPResult Tag (0x0A). So we have to switch the grammar.
        super.transitions[LdapStatesEnum.ADD_RESPONSE_LDAP_RESULT][UniversalTag.ENUMERATED_TAG] = new GrammarTransition(
                LdapStatesEnum.ADD_RESPONSE_LDAP_RESULT, LdapStatesEnum.LDAP_RESULT_GRAMMAR_SWITCH, null );
    }

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Get the instance of this grammar
     *
     * @return An instance on the LdapMessage Grammar
     */
    public static IGrammar getInstance()
    {
        return instance;
    }
}
