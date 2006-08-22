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
package org.apache.directory.shared.ldap.codec.del;


import javax.naming.InvalidNameException;

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
import org.apache.directory.shared.ldap.message.DeleteResponseImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.codec.LdapStatesEnum;
import org.apache.directory.shared.ldap.codec.ResponseCarryingException;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the DelRequest LDAP message. All the actions are
 * declared in this class. As it is a singleton, these declaration are only done
 * once. If an action is to be added or modified, this is where the work is to
 * be done !
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DelRequestGrammar extends AbstractGrammar implements IGrammar
{
    // ~ Static fields/initializers
    // -----------------------------------------------------------------

    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( DelRequestGrammar.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /** The instance of grammar. DelRequestGrammar is a singleton */
    private static IGrammar instance = new DelRequestGrammar();


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new DelRequestGrammar object.
     */
    private DelRequestGrammar()
    {
        name = DelRequestGrammar.class.getName();
        statesEnum = LdapStatesEnum.getInstance();

        // Create the transitions table
        super.transitions = new GrammarTransition[LdapStatesEnum.LAST_DEL_REQUEST_STATE][256];

        // ============================================================================================
        // DelRequest
        // ============================================================================================
        // DelRequestGrammar ::= [APPLICATION 10] LDAPDN { (Tag)
        // Nothing to do
        super.transitions[LdapStatesEnum.DEL_REQUEST_TAG][LdapConstants.DEL_REQUEST_TAG] = new GrammarTransition(
            LdapStatesEnum.DEL_REQUEST_TAG, LdapStatesEnum.DEL_REQUEST_VALUE, null );

        // DelRequestGrammar ::= [APPLICATION 10] LDAPDN { (Value)
        // Initialise the del request pojo and store the DN to be deleted
        super.transitions[LdapStatesEnum.DEL_REQUEST_VALUE][LdapConstants.DEL_REQUEST_TAG] = new GrammarTransition(
            LdapStatesEnum.DEL_REQUEST_VALUE, LdapStatesEnum.END_STATE, new GrammarAction( "Init del Request" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {

                    LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
                    LdapMessage ldapMessage = ldapMessageContainer.getLdapMessage();

                    // We can allocate the DelRequest Object
                    DelRequest delRequest = new DelRequest();

                    // And store the DN into it
                    // Get the Value and store it in the DelRequest
                    TLV tlv = ldapMessageContainer.getCurrentTLV();

                    // We have to handle the special case of a 0 length matched
                    // DN
                    LdapDN entry = null;

                    if ( tlv.getLength().getLength() == 0 )
                    {
                        throw new DecoderException( "The entry must not be null" );
                    }
                    else
                    {
                        try
                        {
                            entry = new LdapDN( tlv.getValue().getData() );
                        }
                        catch ( InvalidNameException ine )
                        {
                            String msg = "The DN to delete :" + StringTools.utf8ToString( tlv.getValue().getData() )+ " (" + StringTools.dumpBytes( tlv.getValue().getData() )
                                + ") is invalid";
                            log.error( "{} : {}", msg, ine.getMessage() );
                            
                            DeleteResponseImpl message = new DeleteResponseImpl( ldapMessage.getMessageId() );
                            message.getLdapResult().setErrorMessage( msg );
                            message.getLdapResult().setResultCode( ResultCodeEnum.INVALIDDNSYNTAX );
                            message.getLdapResult().setMatchedDn( LdapDN.EMPTY_LDAPDN );
                            
                            ResponseCarryingException exception = new ResponseCarryingException( msg, ine );
                            
                            exception.setResponse( message );
                            
                            throw exception;
                        }
                        delRequest.setEntry( entry );
                    }

                    // then we associate it to the ldapMessage Object
                    ldapMessage.setProtocolOP( delRequest );

                    // We can have an END transition
                    ldapMessageContainer.grammarEndAllowed( true );

                    // We can have an Pop transition
                    ldapMessageContainer.grammarPopAllowed( true );

                    if ( IS_DEBUG )
                    {
                        log.debug( "Deleting DN {}", entry );
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
