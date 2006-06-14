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
package org.apache.directory.shared.ldap.codec.bind;


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
 * This class implements the BindRequest LDAP message. All the actions are
 * declared in this class. As it is a singleton, these declaration are only done
 * once. If an action is to be added or modified, this is where the work is to
 * be done !
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class BindRequestGrammar extends AbstractGrammar implements IGrammar
{
    // ~ Static fields/initializers
    // -----------------------------------------------------------------

    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( BindRequestGrammar.class );

    /** The instance of grammar. BindRequestGrammar is a singleton */
    private static IGrammar instance = new BindRequestGrammar();


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Get the instance of this grammar
     * 
     * @return An instance on the BindRequest Grammar
     */
    public static IGrammar getInstance()
    {
        return instance;
    }


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new LdapMessageGrammar object.
     */
    private BindRequestGrammar()
    {

        name = BindRequestGrammar.class.getName();

        statesEnum = LdapStatesEnum.getInstance();

        // Create the transitions table
        super.transitions = new GrammarTransition[LdapStatesEnum.LAST_BIND_REQUEST_STATE][256];

        // ============================================================================================
        // protocolOp : Bind Request
        // ============================================================================================
        // We have to allocate a BindRequest
        // LdapMessage ::= ... BindRequest ...
        // BindRequest ::= [APPLICATION 0] SEQUENCE { ... (Length)
        super.transitions[LdapStatesEnum.BIND_REQUEST_TAG][LdapConstants.BIND_REQUEST_TAG] = new GrammarTransition(
            LdapStatesEnum.BIND_REQUEST_TAG, LdapStatesEnum.BIND_REQUEST_VALUE, null );

        // LdapMessage ::= ... BindRequest ...
        // BindRequest ::= [APPLICATION 0] SEQUENCE { ... (Value)
        // Nothing to do, the Value is empty, this is a constructed TLV. We now
        // can swith to the BindRequest Grammar
        super.transitions[LdapStatesEnum.BIND_REQUEST_VALUE][LdapConstants.BIND_REQUEST_TAG] = new GrammarTransition(
            LdapStatesEnum.BIND_REQUEST_VALUE, LdapStatesEnum.BIND_REQUEST_VERSION_TAG, new GrammarAction(
                "Init BindRequest" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
                    LdapMessage ldapMessage = ldapMessageContainer.getLdapMessage();

                    // We will check that the request is not null
                    TLV tlv = ldapMessageContainer.getCurrentTLV();

                    if ( tlv.getLength().getLength() == 0 )
                    {
                        String msg = "The BindRequest must not be null";
                        log.error( msg );
                        throw new DecoderException( msg );
                    }

                    // Now, we can allocate the BindRequest Object
                    ldapMessage.setProtocolOP( new BindRequest() );
                }
            } );

        // BindRequest ::= ... version INTEGER (1 .. 127 ), ... (Tag)
        // Nothing to do.
        super.transitions[LdapStatesEnum.BIND_REQUEST_VERSION_TAG][UniversalTag.INTEGER_TAG] = new GrammarTransition(
            LdapStatesEnum.BIND_REQUEST_VERSION_TAG, LdapStatesEnum.BIND_REQUEST_VERSION_VALUE, null );

        // BindRequest ::= ... version INTEGER (1 .. 127 ), ... (value)
        // Checks that the Version is in [1, 127]
        super.transitions[LdapStatesEnum.BIND_REQUEST_VERSION_VALUE][UniversalTag.INTEGER_TAG] = new GrammarTransition(
            LdapStatesEnum.BIND_REQUEST_VERSION_VALUE, LdapStatesEnum.BIND_REQUEST_NAME_TAG, new GrammarAction(
                "Store version" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {

                    LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
                    BindRequest bindRequestMessage = ldapMessageContainer.getLdapMessage().getBindRequest();

                    // The current TLV should be a integer between 1 and 127
                    // We get it and store it in Version
                    TLV tlv = ldapMessageContainer.getCurrentTLV();

                    Value value = tlv.getValue();

                    try
                    {
                        int version = IntegerDecoder.parse( value, 1, 127 );

                        if ( version != 3 )
                        {
                            log.error( "The version {} is invalid : it must be 3", new Integer( version ) );

                            throw new DecoderException( "Ldap Version " + version + " is not supported" );
                        }

                        if ( log.isDebugEnabled() )
                        {
                            log.debug( "Ldap version ", new Integer( version ) );
                        }

                        bindRequestMessage.setVersion( version );
                    }
                    catch ( IntegerDecoderException ide )
                    {
                        log.error( "The version {} is invalid : {}. The version must be between (0 .. 127)",
                            StringTools.dumpBytes( value.getData() ), ide.getMessage() );

                        throw new DecoderException( ide.getMessage() );
                    }

                    return;
                }
            } );

        // BindRequest ::= ... name LDAPDN, ... (Tag)
        // Nothing to do. The tag is supposed to be 0x04
        super.transitions[LdapStatesEnum.BIND_REQUEST_NAME_TAG][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            LdapStatesEnum.BIND_REQUEST_NAME_TAG, LdapStatesEnum.BIND_REQUEST_NAME_VALUE, null );

        // BindRequest ::= ... name LDAPDN, ... (Value)
        // We have to store the name
        // The name may be null, for anonymous binds or for SASL
        // authentication
        super.transitions[LdapStatesEnum.BIND_REQUEST_NAME_VALUE][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            LdapStatesEnum.BIND_REQUEST_NAME_VALUE, LdapStatesEnum.BIND_REQUEST_AUTHENTICATION_CHOICE_TAG,
            new GrammarAction( "Store Bind Name value" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {

                    LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
                    BindRequest bindRequestMessage = ldapMessageContainer.getLdapMessage().getBindRequest();

                    // Get the Value and store it in the BindRequest
                    TLV tlv = ldapMessageContainer.getCurrentTLV();

                    // We have to handle the special case of a 0 length name
                    if ( tlv.getLength().getLength() == 0 )
                    {
                        bindRequestMessage.setName( LdapDN.EMPTY_LDAPDN );
                    }
                    else
                    {
                        LdapDN name = LdapDN.EMPTY_LDAPDN;

                        try
                        {
                            name = new LdapDN( tlv.getValue().getData() );
                        }
                        catch ( InvalidNameException ine )
                        {
                            String msg = "Incorrect DN given : " + StringTools.dumpBytes( tlv.getValue().getData() )
                                + " : " + ine.getMessage();
                            log.error( "{} : {}", msg, ine.getMessage() );
                            throw new DecoderException( msg, ine );
                        }

                        bindRequestMessage.setName( name );
                    }

                    if ( log.isDebugEnabled() )
                    {
                        log.debug( " The Bind name is {}", bindRequestMessage.getName() );
                    }

                    return;
                }
            } );

        // BindRequest ::= ... authentication AuthenticationChoice }
        // AuthenticationChoice ::= CHOICE {
        // The tag might be either 0x80 (SimpleAuthentication) or 0x83
        // (SaslCredentials)
        // --------------------------------------------------------------------------------------------
        // If it's 0x80, it is a SimpleAuthentication.
        // --------------------------------------------------------------------------------------------
        // Nothing to do.
        super.transitions[LdapStatesEnum.BIND_REQUEST_AUTHENTICATION_CHOICE_TAG][LdapConstants.BIND_REQUEST_SIMPLE_TAG] = new GrammarTransition(
            LdapStatesEnum.BIND_REQUEST_AUTHENTICATION_CHOICE_TAG,
            LdapStatesEnum.BIND_REQUEST_AUTHENTICATION_SIMPLE_VALUE, null );

        // AuthenticationChoice ::= CHOICE {
        // simple [0] OCTET STRING, (Value)
        // We have to create an Authentication Object to store the credentials.
        // The nextState is GRAMMAR_END
        super.transitions[LdapStatesEnum.BIND_REQUEST_AUTHENTICATION_SIMPLE_VALUE][LdapConstants.BIND_REQUEST_SIMPLE_TAG] = new GrammarTransition(
            LdapStatesEnum.BIND_REQUEST_AUTHENTICATION_SIMPLE_VALUE, LdapStatesEnum.GRAMMAR_END, new GrammarAction(
                "Store Bind Simple Authentication value" )
            {
                public void action( IAsn1Container container )
                {

                    LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;

                    BindRequest bindRequestMessage = ldapMessageContainer.getLdapMessage().getBindRequest();
                    TLV tlv = ldapMessageContainer.getCurrentTLV();

                    // Allocate the Authentication Object
                    SimpleAuthentication authentication = null;

                    authentication = new SimpleAuthentication();

                    authentication.setParent( bindRequestMessage );

                    bindRequestMessage.setAuthentication( authentication );

                    // We have to handle the special case of a 0 length simple
                    if ( tlv.getLength().getLength() == 0 )
                    {
                        authentication.setSimple( new byte[]
                            {} );
                    }
                    else
                    {
                        authentication.setSimple( tlv.getValue().getData() );
                    }

                    // We can have an END transition
                    ldapMessageContainer.grammarEndAllowed( true );

                    // We can have an pop transition
                    ldapMessageContainer.grammarPopAllowed( true );

                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "The simple authentication is : {}", authentication.getSimple() );
                    }
                }
            } );

        // --------------------------------------------------------------------------------------------
        // If it's 0xA3, it is a Sasl Credentials.
        // AuthenticationChoice ::= CHOICE {
        // ...
        // sasl [3] SaslCredentials }
        //
        // SaslCredentials ::= SEQUENCE {
        // mechanism LDAPSTRING,
        // credentials OCTET STRING OPTIONNAL }
        // --------------------------------------------------------------------------------------------
        // AuthenticationChoice ::= CHOICE {
        // sasl [3] saslCredentials, (Tag)
        // Nothing to do. In fact, 0xA3 is the mechanism tag.
        super.transitions[LdapStatesEnum.BIND_REQUEST_AUTHENTICATION_CHOICE_TAG][LdapConstants.BIND_REQUEST_SASL_TAG] = new GrammarTransition(
            LdapStatesEnum.BIND_REQUEST_AUTHENTICATION_CHOICE_TAG,
            LdapStatesEnum.BIND_REQUEST_AUTHENTICATION_SASL_VALUE, null );

        // --------------------------------------------------------------------------------------------
        // If it's 0x83, it is a Sasl Credentials.
        // AuthenticationChoice ::= CHOICE {
        // ...
        // sasl [3] SaslCredentials }
        //
        // SaslCredentials ::= SEQUENCE {
        // mechanism LDAPSTRING,
        // credentials OCTET STRING OPTIONNAL }
        // --------------------------------------------------------------------------------------------
        // AuthenticationChoice ::= CHOICE {
        // sasl [3] saslCredentials, (Value)
        // We will just check that the structure is not null, and create the
        // structure
        super.transitions[LdapStatesEnum.BIND_REQUEST_AUTHENTICATION_SASL_VALUE][LdapConstants.BIND_REQUEST_SASL_TAG] = new GrammarTransition(
            LdapStatesEnum.BIND_REQUEST_AUTHENTICATION_CHOICE_TAG,
            LdapStatesEnum.BIND_REQUEST_AUTHENTICATION_MECHANISM_TAG, new GrammarAction(
                "Create Bind sasl Authentication Object" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {

                    LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
                    BindRequest bindRequestMessage = ldapMessageContainer.getLdapMessage().getBindRequest();
                    TLV tlv = ldapMessageContainer.getCurrentTLV();

                    // We will check that the sasl is not null
                    if ( tlv.getLength().getLength() == 0 )
                    {
                        String msg = "The SaslCredential must not be null";
                        log.error( msg );
                        throw new DecoderException( msg );
                    }

                    // Create the SaslCredentials Object
                    SaslCredentials authentication = new SaslCredentials();

                    authentication.setParent( bindRequestMessage );

                    bindRequestMessage.setAuthentication( authentication );

                    log.debug( "The SaslCredential has been created" );

                    return;
                }
            } );

        // --------------------------------------------------------------------------------------------
        // AuthenticationChoice ::= CHOICE {
        // sasl [3] saslCredentials, (Tag)
        //
        // SaslCredentials ::= SEQUENCE {
        // mechanism LDAPSTRING, (Value)
        // ...
        // Nothing to do.
        super.transitions[LdapStatesEnum.BIND_REQUEST_AUTHENTICATION_MECHANISM_TAG][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            LdapStatesEnum.BIND_REQUEST_AUTHENTICATION_MECHANISM_TAG,
            LdapStatesEnum.BIND_REQUEST_AUTHENTICATION_MECHANISM_VALUE, null );

        // AuthenticationChoice ::= CHOICE {
        // sasl [3] saslCredentials }
        //
        // SaslCredentials ::= SEQUENCE {
        // mechanism LDAPSTRING, (Value)
        // ...
        // We have to store the mechanism.
        super.transitions[LdapStatesEnum.BIND_REQUEST_AUTHENTICATION_MECHANISM_VALUE][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            LdapStatesEnum.BIND_REQUEST_AUTHENTICATION_MECHANISM_VALUE,
            LdapStatesEnum.BIND_REQUEST_AUTHENTICATION_CREDENTIALS_TAG, new GrammarAction(
                "Create Bind sasl Authentication Object" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {

                    LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
                    BindRequest bindRequestMessage = ldapMessageContainer.getLdapMessage().getBindRequest();
                    TLV tlv = ldapMessageContainer.getCurrentTLV();

                    // Get the SaslCredentials Object
                    SaslCredentials authentication = bindRequestMessage.getSaslAuthentication();

                    // We have to handle the special case of a 0 length
                    // mechanism
                    if ( tlv.getLength().getLength() == 0 )
                    {
                        authentication.setMechanism( LdapString.EMPTY_STRING );
                    }
                    else
                    {
                        try
                        {
                            authentication.setMechanism( new LdapString( tlv.getValue().getData() ) );
                        }
                        catch ( LdapStringEncodingException lsee )
                        {
                            log.error( "Invalid mechanism : {} : {}",
                                StringTools.dumpBytes( tlv.getValue().getData() ), lsee.getMessage() );
                            throw new DecoderException( lsee.getMessage() );
                        }
                    }

                    // We can have an END transition
                    ldapMessageContainer.grammarEndAllowed( true );

                    // We can have an Pop transition
                    ldapMessageContainer.grammarPopAllowed( true );

                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "The mechanism is : {}", authentication.getMechanism() );
                    }

                    return;
                }
            } );

        // --------------------------------------------------------------------------------------------
        // SaslCredentials ::= SEQUENCE {
        // ...
        // credentials OCTET STRING OPTIONAL } (Tag)
        // --------------------------------------------------------------------------------------------
        // We may have a credential, or nothing, as it's an optional element.
        // The tag will have one of those values :
        // - 0x04 if it's a credentials
        // - 0x90 if it's a control
        // - any other value is an error.
        //
        // It's a credential if it's 0x04
        super.transitions[LdapStatesEnum.BIND_REQUEST_AUTHENTICATION_CREDENTIALS_TAG][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            LdapStatesEnum.BIND_REQUEST_AUTHENTICATION_CREDENTIALS_TAG,
            LdapStatesEnum.BIND_REQUEST_AUTHENTICATION_CREDENTIALS_VALUE, null );

        // It's a control if it's 0x90
        // super.transitions[LdapStatesEnum.BIND_REQUEST_AUTHENTICATION_CREDENTIALS_TAG][0x90]
        // =
        // new GrammarTransition(
        // LdapStatesEnum.BIND_REQUEST_AUTHENTICATION_CREDENTIALS_TAG,
        // LdapStatesEnum.GRAMMAR_END, null );

        // SaslCredentials ::= SEQUENCE {
        // ...
        // credentials OCTET STRING OPTIONAL } (Value)
        //
        // We have to get the Credentials and store it in the SaslCredentials.
        // Two different following states are possible :
        // - a Control tag (0x90)
        // - or nothing at all (end of the BindRequest).
        // We just have to transit to the first case, which will accept or not
        // the transition.
        // This is done by transiting to the GRAMMAR_END state
        super.transitions[LdapStatesEnum.BIND_REQUEST_AUTHENTICATION_CREDENTIALS_VALUE][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            LdapStatesEnum.BIND_REQUEST_AUTHENTICATION_CREDENTIALS_VALUE, LdapStatesEnum.GRAMMAR_END,
            new GrammarAction( "Store Bind sasl Authentication credentials value" )
            {
                public void action( IAsn1Container container )
                {

                    LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;

                    BindRequest bindRequestMessage = ldapMessageContainer.getLdapMessage().getBindRequest();

                    // Get the Value and store it in the BindRequest
                    TLV tlv = ldapMessageContainer.getCurrentTLV();

                    SaslCredentials credentials = bindRequestMessage.getSaslAuthentication();

                    // We have to handle the special case of a 0 length
                    // credentials
                    if ( tlv.getLength().getLength() == 0 )
                    {
                        credentials.setCredentials( new byte[]
                            {} );
                    }
                    else
                    {
                        credentials.setCredentials( tlv.getValue().getData() );
                    }

                    // We can have an END transition
                    ldapMessageContainer.grammarEndAllowed( true );

                    // We can have an Pop transition
                    ldapMessageContainer.grammarPopAllowed( true );

                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "The credentials are : {}", credentials.getCredentials() );
                    }

                    return;
                }
            } );
    }
}
