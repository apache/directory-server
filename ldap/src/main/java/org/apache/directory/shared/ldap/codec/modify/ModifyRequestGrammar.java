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
package org.apache.directory.shared.ldap.codec.modify;


import javax.naming.InvalidNameException;

import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.grammar.IGrammar;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.util.IntegerDecoder;
import org.apache.directory.shared.asn1.util.IntegerDecoderException;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapMessage;
import org.apache.directory.shared.ldap.codec.LdapMessageContainer;
import org.apache.directory.shared.ldap.codec.LdapStatesEnum;
import org.apache.directory.shared.ldap.codec.ResponseCarryingException;
import org.apache.directory.shared.ldap.codec.util.LdapString;
import org.apache.directory.shared.ldap.codec.util.LdapStringEncodingException;
import org.apache.directory.shared.ldap.message.ModifyResponseImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the ModifyRequest LDAP message. All the actions are
 * declared in this class. As it is a singleton, these declaration are only done
 * once.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ModifyRequestGrammar extends AbstractGrammar implements IGrammar
{
    // ~ Static fields/initializers
    // -----------------------------------------------------------------

    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( ModifyRequestGrammar.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /** The instance of grammar. ModifyRequestGrammar is a singleton */
    private static IGrammar instance = new ModifyRequestGrammar();


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new ModifyRequestGrammar object.
     */
    private ModifyRequestGrammar()
    {
        name = ModifyRequestGrammar.class.getName();
        statesEnum = LdapStatesEnum.getInstance();

        // Create the transitions table
        super.transitions = new GrammarTransition[LdapStatesEnum.LAST_MODIFY_REQUEST_STATE][256];

        // ============================================================================================
        // ModifyRequest Message
        // ============================================================================================
        // LdapMessage ::= ... ModifyRequest ...
        // ModifyRequest ::= [APPLICATION 6] SEQUENCE { (Tag)
        // Nothing to do.
        super.transitions[LdapStatesEnum.MODIFY_REQUEST_TAG][LdapConstants.MODIFY_REQUEST_TAG] = new GrammarTransition(
            LdapStatesEnum.MODIFY_REQUEST_TAG, LdapStatesEnum.MODIFY_REQUEST_VALUE, null );

        // LdapMessage ::= ... ModifyRequest ...
        // ModifyRequest ::= [APPLICATION 6] SEQUENCE { (Value)
        // Create the structure
        super.transitions[LdapStatesEnum.MODIFY_REQUEST_VALUE][LdapConstants.MODIFY_REQUEST_TAG] = new GrammarTransition(
            LdapStatesEnum.MODIFY_REQUEST_VALUE, LdapStatesEnum.MODIFY_REQUEST_OBJECT_TAG, new GrammarAction(
                "Init ModifyRequest" )
            {
                public void action( IAsn1Container container )
                {

                    LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
                    LdapMessage ldapMessage = ldapMessageContainer.getLdapMessage();

                    // Now, we can allocate the ModifyRequest Object
                    // And we associate it to the ldapMessage Object
                    ldapMessage.setProtocolOP( new ModifyRequest() );
                }
            } );

        // LdapMessage ::= ... ModifyRequest ...
        // ModifyRequest ::= [APPLICATION 6] SEQUENCE {
        // object LDAPDN, (Tag)
        // ...
        // Nothing to do
        super.transitions[LdapStatesEnum.MODIFY_REQUEST_OBJECT_TAG][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            LdapStatesEnum.MODIFY_REQUEST_OBJECT_TAG, LdapStatesEnum.MODIFY_REQUEST_OBJECT_VALUE, null );

        // LdapMessage ::= ... ModifyRequest ...
        // ModifyRequest ::= [APPLICATION 6] SEQUENCE {
        // object LDAPDN, (Value)
        // ...
        // Store the object name.
        super.transitions[LdapStatesEnum.MODIFY_REQUEST_OBJECT_VALUE][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            LdapStatesEnum.MODIFY_REQUEST_OBJECT_VALUE, LdapStatesEnum.MODIFY_REQUEST_MODIFICATIONS_TAG,
            new GrammarAction( "Store Modify request object Value" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {

                    LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
                    LdapMessage ldapMessage = ldapMessageContainer.getLdapMessage();
                    ModifyRequest modifyRequest = ldapMessage.getModifyRequest();

                    TLV tlv = ldapMessageContainer.getCurrentTLV();

                    LdapDN object = LdapDN.EMPTY_LDAPDN;

                    // Store the value.
                    if ( tlv.getLength().getLength() == 0 )
                    {
                        modifyRequest.setObject( object );
                    }
                    else
                    {
                        byte[] dnBytes = tlv.getValue().getData();

                        try
                        {
                            object = new LdapDN( dnBytes );
                        }
                        catch ( InvalidNameException ine )
                        {
                            String msg = "Invalid DN given : " + StringTools.utf8ToString( dnBytes ) + 
                                " (" + StringTools.dumpBytes( dnBytes ) + 
                                ") is invalid";
                            log.error( "{} : {}", msg, ine.getMessage() );
                    
                            ModifyResponseImpl response = new ModifyResponseImpl( ldapMessage.getMessageId() );
                            throw new ResponseCarryingException( msg, response, ResultCodeEnum.INVALIDDNSYNTAX, LdapDN.EMPTY_LDAPDN, ine );
                        }

                        modifyRequest.setObject( object );
                    }

                    if ( IS_DEBUG )
                    {
                        log.debug( "Modification of DN {}", modifyRequest.getObject() );
                    }
                }
            } );

        // LdapMessage ::= ... ModifyRequest ...
        // ModifyRequest ::= [APPLICATION 6] SEQUENCE {
        // ...
        // modification *SEQUENCE* OF SEQUENCE { (Tag)
        // Nothing to do
        super.transitions[LdapStatesEnum.MODIFY_REQUEST_MODIFICATIONS_TAG][UniversalTag.SEQUENCE_TAG] = new GrammarTransition(
            LdapStatesEnum.MODIFY_REQUEST_MODIFICATIONS_TAG, LdapStatesEnum.MODIFY_REQUEST_MODIFICATIONS_VALUE, null );

        // LdapMessage ::= ... ModifyRequest ...
        // ModifyRequest ::= [APPLICATION 6] SEQUENCE {
        // ...
        // modification *SEQUENCE* OF SEQUENCE { (Value)
        // Allocates the array list
        super.transitions[LdapStatesEnum.MODIFY_REQUEST_MODIFICATIONS_VALUE][UniversalTag.SEQUENCE_TAG] = new GrammarTransition(
            LdapStatesEnum.MODIFY_REQUEST_MODIFICATIONS_VALUE, LdapStatesEnum.MODIFY_REQUEST_MODIFICATION_SEQUENCE_TAG,
            new GrammarAction( "Init modifications array list" )
            {
                public void action( IAsn1Container container )
                {

                    LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
                    LdapMessage ldapMessage = ldapMessageContainer.getLdapMessage();
                    ModifyRequest modifyRequest = ldapMessage.getModifyRequest();

                    modifyRequest.initModifications();
                }
            } );

        // LdapMessage ::= ... ModifyRequest ...
        // ModifyRequest ::= [APPLICATION 6] SEQUENCE {
        // ...
        // modification SEQUENCE OF *SEQUENCE* { (Tag)
        // Nothing to do
        super.transitions[LdapStatesEnum.MODIFY_REQUEST_MODIFICATION_SEQUENCE_TAG][UniversalTag.SEQUENCE_TAG] = new GrammarTransition(
            LdapStatesEnum.MODIFY_REQUEST_MODIFICATION_SEQUENCE_TAG,
            LdapStatesEnum.MODIFY_REQUEST_MODIFICATION_SEQUENCE_VALUE, null );

        // LdapMessage ::= ... ModifyRequest ...
        // ModifyRequest ::= [APPLICATION 6] SEQUENCE {
        // ...
        // modification SEQUENCE OF *SEQUENCE* { (Tag)
        // Nothing to do
        // This is a loop, when dealing with more than one modification
        super.transitions[LdapStatesEnum.MODIFY_REQUEST_ATTRIBUTE_VALUE_OR_MODIFICATION_TAG][UniversalTag.SEQUENCE_TAG] = new GrammarTransition(
            LdapStatesEnum.MODIFY_REQUEST_ATTRIBUTE_VALUE_OR_MODIFICATION_TAG,
            LdapStatesEnum.MODIFY_REQUEST_MODIFICATION_SEQUENCE_VALUE, null );

        // LdapMessage ::= ... ModifyRequest ...
        // ModifyRequest ::= [APPLICATION 6] SEQUENCE {
        // ...
        // modification SEQUENCE OF *SEQUENCE* { (Value)
        // Nothing to do
        super.transitions[LdapStatesEnum.MODIFY_REQUEST_MODIFICATION_SEQUENCE_VALUE][UniversalTag.SEQUENCE_TAG] = new GrammarTransition(
            LdapStatesEnum.MODIFY_REQUEST_MODIFICATION_SEQUENCE_VALUE, LdapStatesEnum.MODIFY_REQUEST_OPERATION_TAG,
            null );

        // ...
        // modification SEQUENCE OF SEQUENCE {
        // operation ENUMERATED { (Tag)
        // add (0),
        // delete (1),
        // replace (2) },
        // ...
        // Nothing to do
        super.transitions[LdapStatesEnum.MODIFY_REQUEST_OPERATION_TAG][UniversalTag.ENUMERATED_TAG] = new GrammarTransition(
            LdapStatesEnum.MODIFY_REQUEST_OPERATION_TAG, LdapStatesEnum.MODIFY_REQUEST_OPERATION_VALUE, null );

        // ...
        // modification SEQUENCE OF SEQUENCE {
        // operation ENUMERATED { (Value)
        // add (0),
        // delete (1),
        // replace (2) },
        // ...
        // Store the operation type. We put it in a temporary storage,
        // because we can't allocate a ModificationItem before knowing
        // the attributes'name.
        super.transitions[LdapStatesEnum.MODIFY_REQUEST_OPERATION_VALUE][UniversalTag.ENUMERATED_TAG] = new GrammarTransition(
            LdapStatesEnum.MODIFY_REQUEST_OPERATION_VALUE, LdapStatesEnum.MODIFY_REQUEST_MODIFICATION_TAG,
            new GrammarAction( "Store operation type" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {

                    LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
                    LdapMessage ldapMessage = ldapMessageContainer.getLdapMessage();
                    ModifyRequest modifyRequest = ldapMessage.getModifyRequest();

                    TLV tlv = ldapMessageContainer.getCurrentTLV();

                    // Decode the operation type
                    int operation = 0;

                    try
                    {
                        operation = IntegerDecoder.parse( tlv.getValue(), 0, 2 );
                    }
                    catch ( IntegerDecoderException ide )
                    {
                        String msg = "Invalid operation ( " + StringTools.dumpBytes( tlv.getValue().getData() )
                            + "), it should be 0, 1 or 2";
                        log.error( msg );
                        
                        // This will generate a PROTOCOL_ERROR
                        throw new DecoderException( msg );
                    }

                    // Store the current operation.
                    modifyRequest.setCurrentOperation( operation );

                    if ( IS_DEBUG )
                    {
                        switch ( operation )
                        {
                            case LdapConstants.OPERATION_ADD:
                                log.debug( "Modification operation : ADD" );
                                break;

                            case LdapConstants.OPERATION_DELETE:
                                log.debug( "Modification operation : DELETE" );
                                break;

                            case LdapConstants.OPERATION_REPLACE:
                                log.debug( "Modification operation : REPLACE" );
                                break;
                        }
                    }

                }
            } );

        // ...
        // modification SEQUENCE OF SEQUENCE {
        // modification AttributeTypeAndValues } }
        // AttributeTypeAndValues ::= SEQUENCE { (Tag)
        // Nothing to do
        super.transitions[LdapStatesEnum.MODIFY_REQUEST_MODIFICATION_TAG][UniversalTag.SEQUENCE_TAG] = new GrammarTransition(
            LdapStatesEnum.MODIFY_REQUEST_MODIFICATION_TAG, LdapStatesEnum.MODIFY_REQUEST_MODIFICATION_VALUE, null );

        // ...
        // modification SEQUENCE OF SEQUENCE {
        // modification AttributeTypeAndValues } }
        // AttributeTypeAndValues ::= SEQUENCE { (Value)
        // Nothing to do
        super.transitions[LdapStatesEnum.MODIFY_REQUEST_MODIFICATION_VALUE][UniversalTag.SEQUENCE_TAG] = new GrammarTransition(
            LdapStatesEnum.MODIFY_REQUEST_MODIFICATION_VALUE, LdapStatesEnum.MODIFY_REQUEST_TYPE_TAG, null );

        // AttributeTypeAndValues ::= SEQUENCE {
        // type AttributeDescription, (Tag)
        // ...
        // Nothing to do
        super.transitions[LdapStatesEnum.MODIFY_REQUEST_TYPE_TAG][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            LdapStatesEnum.MODIFY_REQUEST_TYPE_TAG, LdapStatesEnum.MODIFY_REQUEST_TYPE_VALUE, null );

        // AttributeTypeAndValues ::= SEQUENCE {
        // type AttributeDescription, (Value)
        // ...
        // Store a new attribute type and values.
        super.transitions[LdapStatesEnum.MODIFY_REQUEST_TYPE_VALUE][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            LdapStatesEnum.MODIFY_REQUEST_TYPE_VALUE, LdapStatesEnum.MODIFY_REQUEST_VALS_TAG, new GrammarAction(
                "Store type" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {

                    LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
                    LdapMessage ldapMessage = ldapMessageContainer.getLdapMessage();
                    ModifyRequest modifyRequest = ldapMessage.getModifyRequest();

                    TLV tlv = ldapMessageContainer.getCurrentTLV();

                    // Store the value. It can't be null
                    LdapString type = null;

                    if ( tlv.getLength().getLength() == 0 )
                    {
                        String msg = "The type can't be null";
                        log.error( msg );
                        
                        ModifyResponseImpl response = new ModifyResponseImpl( ldapMessage.getMessageId() );
                        throw new ResponseCarryingException( msg, response, ResultCodeEnum.INVALIDATTRIBUTESYNTAX, modifyRequest.getObject(), null );
                    }
                    else
                    {
                        try
                        {
                            type = new LdapString( tlv.getValue().getData() );
                            modifyRequest.addAttributeTypeAndValues( type );
                        }
                        catch ( LdapStringEncodingException lsee )
                        {
                            String msg = "Invalid type : " + StringTools.dumpBytes( tlv.getValue().getData() );
                            log.error( msg );

                            ModifyResponseImpl response = new ModifyResponseImpl( ldapMessage.getMessageId() );
                            throw new ResponseCarryingException( msg, response, ResultCodeEnum.INVALIDATTRIBUTESYNTAX, modifyRequest.getObject(), lsee );
                        }
                    }

                    if ( IS_DEBUG )
                    {
                        log.debug( "Modifying type : {}", type );
                    }
                }
            } );

        // AttributeTypeAndValues ::= SEQUENCE {
        // ...
        // vals SET OF AttributeValue } (Tag)
        // Nothing to do
        super.transitions[LdapStatesEnum.MODIFY_REQUEST_VALS_TAG][UniversalTag.SET_TAG] = new GrammarTransition(
            LdapStatesEnum.MODIFY_REQUEST_VALS_TAG, LdapStatesEnum.MODIFY_REQUEST_VALS_VALUE, null );

        // AttributeTypeAndValues ::= SEQUENCE {
        // ...
        // vals SET OF AttributeValue } (Value)
        // It can be null.
        super.transitions[LdapStatesEnum.MODIFY_REQUEST_VALS_VALUE][UniversalTag.SET_TAG] = new GrammarTransition(
            LdapStatesEnum.MODIFY_REQUEST_VALS_VALUE,
            LdapStatesEnum.MODIFY_REQUEST_ATTRIBUTE_VALUE_OR_MODIFICATION_TAG, new GrammarAction( "Attribute vals" )
            {
                public void action( IAsn1Container container )
                {

                    LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;

                    TLV tlv = ldapMessageContainer.getCurrentTLV();

                    // If the length is null, we store an empty value
                    if ( tlv.getLength().getLength() == 0 )
                    {
                        log.debug( "No vals for this attribute" );
                    }

                    // We can have an END transition
                    ldapMessageContainer.grammarEndAllowed( true );

                    // We can have a Pop transition
                    ldapMessageContainer.grammarPopAllowed( true );

                    log.debug( "Some vals are to be decoded" );
                }
            } );

        // AttributeTypeAndValues ::= SEQUENCE {
        // ...
        // vals SET OF AttributeValue } (Value)
        // Loop if the value is null.
        super.transitions[LdapStatesEnum.MODIFY_REQUEST_ATTRIBUTE_VALUE_TAG][UniversalTag.SEQUENCE_TAG] = new GrammarTransition(
            LdapStatesEnum.MODIFY_REQUEST_VALS_VALUE,
            LdapStatesEnum.MODIFY_REQUEST_ATTRIBUTE_VALUE_OR_MODIFICATION_TAG, null );

        // AttributeTypeAndValues ::= SEQUENCE {
        // ...
        // vals SET OF AttributeValue }
        // AttributeValue ::= OCTET STRING (Tag)
        // Nothing to do
        super.transitions[LdapStatesEnum.MODIFY_REQUEST_ATTRIBUTE_VALUE_TAG][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            LdapStatesEnum.MODIFY_REQUEST_ATTRIBUTE_VALUE_TAG, LdapStatesEnum.MODIFY_REQUEST_ATTRIBUTE_VALUE_VALUE,
            null );

        // AttributeTypeAndValues ::= SEQUENCE {
        // ...
        // vals SET OF AttributeValue }
        // AttributeValue ::= OCTET STRING (Tag)
        // This is a loop, when dealing with multi-valued attributes
        super.transitions[LdapStatesEnum.MODIFY_REQUEST_ATTRIBUTE_VALUE_OR_MODIFICATION_TAG][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            LdapStatesEnum.MODIFY_REQUEST_ATTRIBUTE_VALUE_OR_MODIFICATION_TAG,
            LdapStatesEnum.MODIFY_REQUEST_ATTRIBUTE_VALUE_VALUE, null );

        // AttributeTypeAndValues ::= SEQUENCE {
        // ...
        // vals SET OF AttributeValue }
        // AttributeValue ::= OCTET STRING (Value)
        // Store a new attribute value.
        super.transitions[LdapStatesEnum.MODIFY_REQUEST_ATTRIBUTE_VALUE_VALUE][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            LdapStatesEnum.MODIFY_REQUEST_ATTRIBUTE_VALUE_VALUE,
            LdapStatesEnum.MODIFY_REQUEST_ATTRIBUTE_VALUE_OR_MODIFICATION_TAG, new GrammarAction( "Store value" )
            {
                public void action( IAsn1Container container )
                {

                    LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
                    LdapMessage ldapMessage = ldapMessageContainer.getLdapMessage();
                    ModifyRequest modifyRequest = ldapMessage.getModifyRequest();

                    TLV tlv = ldapMessageContainer.getCurrentTLV();

                    // Store the value. It can't be null
                    Object value = StringTools.EMPTY_BYTES;

                    if ( tlv.getLength().getLength() == 0 )
                    {
                        modifyRequest.addAttributeValue( "" );
                    }
                    else
                    {
                        value = tlv.getValue().getData();

                        if ( ldapMessageContainer.isBinary( modifyRequest.getCurrentAttributeType() ) )
                        {
                            modifyRequest.addAttributeValue( value );
                        }
                        else
                        {
                            modifyRequest.addAttributeValue( StringTools.utf8ToString( ( byte[] ) value ) );
                        }
                    }

                    // We can have an END transition
                    ldapMessageContainer.grammarEndAllowed( true );

                    // We can have an Pop transition
                    ldapMessageContainer.grammarPopAllowed( true );

                    log.debug( "Value modified : {}", value );
                }
            } );

    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Get the instance of this grammar
     * 
     * @return An instance on the ModifyRequest Grammar
     */
    public static IGrammar getInstance()
    {
        return instance;
    }
}
