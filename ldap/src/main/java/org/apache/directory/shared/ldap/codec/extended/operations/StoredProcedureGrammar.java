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

package org.apache.directory.shared.ldap.codec.extended.operations;


import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.grammar.IGrammar;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.ldap.util.StringTools;
import org.apache.log4j.Logger;
import org.apache.directory.shared.ldap.codec.extended.operations.StoredProcedure.StoredProcedureParameter;


/**
 * ASN.1 BER Grammar for Stored Procedure Extended Operation
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoredProcedureGrammar extends AbstractGrammar implements IGrammar
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** The logger */
    //private static final Logger log = LoggerFactory.getLogger( StoredProcedureGrammar.class );
    private static final Logger log = Logger.getLogger( StoredProcedureGrammar.class );

    /** The instance of grammar. StoredProcedureGrammar is a singleton. */
    private static IGrammar instance = new StoredProcedureGrammar();


    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new StoredProcedureGrammar object.
     */
    private StoredProcedureGrammar()
    {
        name = StoredProcedureGrammar.class.getName();
        statesEnum = StoredProcedureStatesEnum.getInstance();

        // Create the transitions table
        super.transitions = new GrammarTransition[StoredProcedureStatesEnum.LAST_STORED_PROCEDURE_STATE][256];

        //============================================================================================
        // StoredProcedure Message
        //============================================================================================
        // StoredProcedure ::= SEQUENCE { (Tag)
        //   ...
        // Nothing to do.
        super.transitions[StoredProcedureStatesEnum.STORED_PROCEDURE_TAG][UniversalTag.SEQUENCE_TAG] = new GrammarTransition(
            StoredProcedureStatesEnum.STORED_PROCEDURE_TAG, StoredProcedureStatesEnum.STORED_PROCEDURE_VALUE, null );

        // StoredProcedure ::= SEQUENCE { (Value)
        //   ...
        // Nothing to do.
        super.transitions[StoredProcedureStatesEnum.STORED_PROCEDURE_VALUE][UniversalTag.SEQUENCE_TAG] = new GrammarTransition(
            StoredProcedureStatesEnum.STORED_PROCEDURE_VALUE, StoredProcedureStatesEnum.LANGUAGE_TAG, null );

        //    language OCTETSTRING, (Tag)
        //    ...
        // Nothing to do
        super.transitions[StoredProcedureStatesEnum.LANGUAGE_TAG][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            StoredProcedureStatesEnum.LANGUAGE_TAG, StoredProcedureStatesEnum.LANGUAGE_VALUE, null );

        //    language OCTETSTRING, (Value)
        //    ...
        // Store the language.
        super.transitions[StoredProcedureStatesEnum.LANGUAGE_VALUE][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            StoredProcedureStatesEnum.LANGUAGE_VALUE, StoredProcedureStatesEnum.PROCEDURE_TAG, new GrammarAction(
                "Stores the language" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {

                    StoredProcedureContainer storedProcedureContainer = ( StoredProcedureContainer ) container;

                    TLV tlv = storedProcedureContainer.getCurrentTLV();

                    StoredProcedure storedProcedure = null;

                    // Store the value.
                    if ( tlv.getLength().getLength() == 0 )
                    {
                        // We can't have a void language !
                        log.error( "The stored procedure language is null" );
                        throw new DecoderException( "The stored procedure language cannot be null" );
                    }
                    else
                    {
                        // Only this field's type is String by default
                        String language = StringTools.utf8ToString( tlv.getValue().getData() );

                        if ( log.isDebugEnabled() )
                        {
                            log.debug( "SP language found: " + language );
                        }

                        storedProcedure = new StoredProcedure();
                        storedProcedure.setLanguage( language );
                        storedProcedureContainer.setStoredProcedure( storedProcedure );
                    }
                }
            } );

        //    procedure OCTETSTRING, (Tag)
        //    ...
        // Nothing to do
        super.transitions[StoredProcedureStatesEnum.PROCEDURE_TAG][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            StoredProcedureStatesEnum.PROCEDURE_TAG, StoredProcedureStatesEnum.PROCEDURE_VALUE, null );

        //    procedure OCTETSTRING, (Value)
        //    ...
        // Store the procedure.
        super.transitions[StoredProcedureStatesEnum.PROCEDURE_VALUE][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            StoredProcedureStatesEnum.PROCEDURE_VALUE, StoredProcedureStatesEnum.PARAMETERS_TAG, new GrammarAction(
                "Stores the procedure" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {

                    StoredProcedureContainer storedProcedureContainer = ( StoredProcedureContainer ) container;

                    TLV tlv = storedProcedureContainer.getCurrentTLV();

                    StoredProcedure storedProcedure = storedProcedureContainer.getStoredProcedure();

                    // Store the value.
                    if ( tlv.getLength().getLength() == 0 )
                    {
                        // We can't have a void procedure !
                        log.error( "The procedure can't be null" );
                        throw new DecoderException( "The procedure can't be null" );
                    }
                    else
                    {
                        byte[] procedure = tlv.getValue().getData();

                        storedProcedure.setProcedure( procedure );
                    }

                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "Procedure found : " + storedProcedure.getProcedure() );
                    }
                }
            } );

        // parameters SEQUENCE OF Parameter { (Tag)
        //    ...
        // Nothing to do
        super.transitions[StoredProcedureStatesEnum.PARAMETERS_TAG][UniversalTag.SEQUENCE_TAG] = new GrammarTransition(
            StoredProcedureStatesEnum.PARAMETERS_TAG, StoredProcedureStatesEnum.PARAMETERS_VALUE, null );

        // parameters SEQUENCE OF Parameter { (Value)
        //    ...
        // Nothing to do. The list of parameters will be created with the first parameter.
        super.transitions[StoredProcedureStatesEnum.PARAMETERS_VALUE][UniversalTag.SEQUENCE_TAG] = new GrammarTransition(
            StoredProcedureStatesEnum.PARAMETERS_VALUE, StoredProcedureStatesEnum.PARAMETER_TAG, new GrammarAction(
                "Stores the parameters" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    StoredProcedureContainer storedProcedureContainer = ( StoredProcedureContainer ) container;
                    storedProcedureContainer.grammarEndAllowed( true );
                }
            } );
        
        

        // parameter SEQUENCE OF  { (Tag)
        //    ...
        // Nothing to do
        super.transitions[StoredProcedureStatesEnum.PARAMETER_TAG][UniversalTag.SEQUENCE_TAG] = new GrammarTransition(
            StoredProcedureStatesEnum.PARAMETER_TAG, StoredProcedureStatesEnum.PARAMETER_VALUE, null );

        // parameter SEQUENCE OF { (Value)
        //    ...
        // Nothing to do. 
        super.transitions[StoredProcedureStatesEnum.PARAMETER_VALUE][UniversalTag.SEQUENCE_TAG] = new GrammarTransition(
            StoredProcedureStatesEnum.PARAMETER_VALUE, StoredProcedureStatesEnum.PARAMETER_TYPE_TAG, null );

        // Parameter ::= {
        //    type OCTETSTRING, (Tag)
        //    ...
        // Nothing to do
        super.transitions[StoredProcedureStatesEnum.PARAMETER_TYPE_TAG][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            StoredProcedureStatesEnum.PARAMETER_TYPE_TAG, StoredProcedureStatesEnum.PARAMETER_TYPE_VALUE, null );

        // Parameter ::= {
        //    type OCTETSTRING, (Value)
        //    ...
        // We can create a parameter, and store its type
        super.transitions[StoredProcedureStatesEnum.PARAMETER_TYPE_VALUE][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            StoredProcedureStatesEnum.PARAMETER_TYPE_VALUE, StoredProcedureStatesEnum.PARAMETER_VALUE_TAG, new GrammarAction(
                "Store parameter type" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    StoredProcedureContainer storedProcedureContainer = ( StoredProcedureContainer ) container;

                    TLV tlv = storedProcedureContainer.getCurrentTLV();
                    StoredProcedure storedProcedure = storedProcedureContainer.getStoredProcedure();

                    // Store the value.
                    if ( tlv.getLength().getLength() == 0 )
                    {
                        // We can't have a void parameter type !
                        log.error( "The parameter type can't be null" );
                        throw new DecoderException( "The parameter type can't be null" );
                    }
                    else
                    {
                        StoredProcedureParameter parameter = new StoredProcedureParameter();

                        byte[] parameterType = tlv.getValue().getData();

                        parameter.setType( parameterType );

                        // We store the type in the current parameter.
                        storedProcedure.setCurrentParameter( parameter );

                        if ( log.isDebugEnabled() )
                        {
                            log.debug( "Parameter type found : " + StringTools.dumpBytes( parameterType ) );
                        }

                    }
                }
            } );

        // Parameter ::= {
        //    ...
        //    value OCTETSTRING (Tag)
        // }
        // Nothing to do
        super.transitions[StoredProcedureStatesEnum.PARAMETER_VALUE_TAG][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            StoredProcedureStatesEnum.PARAMETER_VALUE_TAG, StoredProcedureStatesEnum.PARAMETER_VALUE_VALUE, null );

        // Parameter ::= {
        //    ...
        //    value OCTETSTRING (Tag)
        // }
        // Store the parameter value
        super.transitions[StoredProcedureStatesEnum.PARAMETER_VALUE_VALUE][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            StoredProcedureStatesEnum.PARAMETER_VALUE_VALUE, StoredProcedureStatesEnum.PARAMETER_TAG, new GrammarAction(
                "Store parameter value" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    StoredProcedureContainer storedProcedureContainer = ( StoredProcedureContainer ) container;

                    TLV tlv = storedProcedureContainer.getCurrentTLV();
                    StoredProcedure storedProcedure = storedProcedureContainer.getStoredProcedure();

                    // Store the value.
                    if ( tlv.getLength().getLength() == 0 )
                    {
                        // We can't have a void parameter value !
                        log.error( "The parameter value can't be null" );
                        throw new DecoderException( "The parameter value can't be null" );
                    }
                    else
                    {
                        byte[] parameterValue = tlv.getValue().getData();

                        if ( parameterValue.length != 0 )
                        {
                            StoredProcedureParameter parameter = storedProcedure.getCurrentParameter();
                            parameter.setValue( parameterValue );

                            // We can now add a new Parameter to the procedure
                            storedProcedure.addParameter( parameter );

                            if ( log.isDebugEnabled() )
                            {
                                log.debug( "Parameter value found : " + StringTools.dumpBytes( parameterValue ) );
                            }
                        }
                        else
                        {
                            log.error( "The parameter value is empty. This is not allowed." );
                            throw new DecoderException( "The parameter value is empty. This is not allowed." );
                        }
                    }

                    // The only possible END state for the grammar is here
                    container.grammarEndAllowed( true );
                }
            } );
    }


    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Get the instance of this grammar
     *
     * @return An instance on the StoredProcedure Grammar
     */
    public static IGrammar getInstance()
    {
        return instance;
    }
}
