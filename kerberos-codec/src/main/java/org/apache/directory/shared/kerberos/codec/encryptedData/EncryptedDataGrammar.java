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
package org.apache.directory.shared.kerberos.codec.encryptedData;


import org.apache.directory.shared.asn1.ber.Asn1Container;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.util.IntegerDecoder;
import org.apache.directory.shared.asn1.util.IntegerDecoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.actions.CheckNotNullLength;
import org.apache.directory.shared.kerberos.components.EncryptedData;
import org.apache.directory.shared.kerberos.components.EncryptionType;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the EncryptedData structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class EncryptedDataGrammar extends AbstractGrammar
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( EncryptedDataGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. EncryptedDataGrammar is a singleton */
    private static Grammar instance = new EncryptedDataGrammar();


    /**
     * Creates a new PrincipalNameGrammar object.
     */
    private EncryptedDataGrammar()
    {
        setName( EncryptedDataGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[EncryptedDataStatesEnum.LAST_ENCRYPTED_DATA_STATE.ordinal()][256];

        // ============================================================================================
        // EncryptedData 
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from EncryptedData init to EncryptedData SEQ
        // --------------------------------------------------------------------------------------------
        // EncryptedData   ::= SEQUENCE
        super.transitions[EncryptedDataStatesEnum.START_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] = new GrammarTransition(
            EncryptedDataStatesEnum.START_STATE, EncryptedDataStatesEnum.ENCRYPTED_DATA_SEQ_STATE, UniversalTag.SEQUENCE.getValue(),
            new GrammarAction( "EncryptedData SEQUENCE" )
            {
                public void action( Asn1Container container ) throws DecoderException
                {
                    EncryptedDataContainer encryptedDataContainer = ( EncryptedDataContainer ) container;

                    TLV tlv = encryptedDataContainer.getCurrentTLV();

                    // The Length should not be null
                    if ( tlv.getLength() == 0 )
                    {
                        LOG.error( I18n.err( I18n.ERR_04066 ) );

                        // This will generate a PROTOCOL_ERROR
                        throw new DecoderException( I18n.err( I18n.ERR_04067 ) );
                    }
                    
                    EncryptedData encryptedData = new EncryptedData();
                    encryptedDataContainer.setEncryptedData( encryptedData );
                    
                    if ( IS_DEBUG )
                    {
                        LOG.debug( "EncryptedData created" );
                    }
                }
            } );
        
        // --------------------------------------------------------------------------------------------
        // Transition from EncryptedData SEQ to etype tag
        // --------------------------------------------------------------------------------------------
        // EncryptedData   ::= SEQUENCE {
        //         etype       [0]
        super.transitions[EncryptedDataStatesEnum.ENCRYPTED_DATA_SEQ_STATE.ordinal()][KerberosConstants.ENCRYPTED_DATA_ETYPE_TAG] = new GrammarTransition(
            EncryptedDataStatesEnum.ENCRYPTED_DATA_SEQ_STATE, EncryptedDataStatesEnum.ENCRYPTED_DATA_ETYPE_TAG_STATE, KerberosConstants.ENCRYPTED_DATA_ETYPE_TAG,
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from etype tag to etype value
        // --------------------------------------------------------------------------------------------
        // EncryptedData   ::= SEQUENCE {
        //         etype       [0] Int32,
        super.transitions[EncryptedDataStatesEnum.ENCRYPTED_DATA_ETYPE_TAG_STATE.ordinal()][UniversalTag.INTEGER.getValue()] = new GrammarTransition(
            EncryptedDataStatesEnum.ENCRYPTED_DATA_ETYPE_TAG_STATE, EncryptedDataStatesEnum.ENCRYPTED_DATA_ETYPE_STATE, UniversalTag.INTEGER.getValue(),
            new GrammarAction( "EncryptedData SEQUENCE" )
            {
                public void action( Asn1Container container ) throws DecoderException
                {
                    EncryptedDataContainer encryptedDataContainer = ( EncryptedDataContainer ) container;

                    TLV tlv = encryptedDataContainer.getCurrentTLV();

                    // The Length should not be null
                    if ( tlv.getLength() == 0 )
                    {
                        LOG.error( I18n.err( I18n.ERR_04066 ) );

                        // This will generate a PROTOCOL_ERROR
                        throw new DecoderException( I18n.err( I18n.ERR_04067 ) );
                    }
                    
                    // The encyptionType is an integer
                    Value value = tlv.getValue();
                    
                    EncryptionType encryptionType = null;
                    EncryptedData encryptedData = encryptedDataContainer.getEncryptedData();
                    
                    try
                    {
                        int eType = IntegerDecoder.parse( value );
                        encryptionType = EncryptionType.getTypeByOrdinal( eType );

                        encryptedData.setEType( encryptionType );

                        if ( IS_DEBUG )
                        {
                            LOG.debug( "etype : " + encryptionType );
                        }
                    }
                    catch ( IntegerDecoderException ide )
                    {
                        LOG.error( I18n.err( I18n.ERR_04070, StringTools.dumpBytes( value.getData() ), ide
                            .getLocalizedMessage() ) );

                        // This will generate a PROTOCOL_ERROR
                        throw new DecoderException( ide.getMessage() );
                    }
                    
                    if ( IS_DEBUG )
                    {
                        LOG.debug( "EncryptionType : {}", encryptionType );
                    }
                }
            } );
        
        // --------------------------------------------------------------------------------------------
        // Transition from etype value to kvno tag
        // --------------------------------------------------------------------------------------------
        // EncryptedData   ::= SEQUENCE {
        //         ...
        //         kvno     [1]
        super.transitions[EncryptedDataStatesEnum.ENCRYPTED_DATA_ETYPE_STATE.ordinal()][KerberosConstants.ENCRYPTED_DATA_KVNO_TAG] = new GrammarTransition(
            EncryptedDataStatesEnum.ENCRYPTED_DATA_ETYPE_STATE, EncryptedDataStatesEnum.ENCRYPTED_DATA_KVNO_TAG_STATE, KerberosConstants.ENCRYPTED_DATA_KVNO_TAG,
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from etype value to cipher tag (kvno is missing)
        // --------------------------------------------------------------------------------------------
        // EncryptedData   ::= SEQUENCE {
        //         ...
        //         cipher     [2]
        super.transitions[EncryptedDataStatesEnum.ENCRYPTED_DATA_ETYPE_STATE.ordinal()][KerberosConstants.ENCRYPTED_DATA_CIPHER_TAG] = new GrammarTransition(
            EncryptedDataStatesEnum.ENCRYPTED_DATA_ETYPE_STATE, EncryptedDataStatesEnum.ENCRYPTED_DATA_CIPHER_TAG_STATE, KerberosConstants.ENCRYPTED_DATA_CIPHER_TAG,
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from kvno tag to kvno value
        // --------------------------------------------------------------------------------------------
        // EncryptedData   ::= SEQUENCE {
        //         ...
        //         kvno     [1] UInt32
        super.transitions[EncryptedDataStatesEnum.ENCRYPTED_DATA_KVNO_TAG_STATE.ordinal()][UniversalTag.INTEGER.getValue()] = new GrammarTransition(
            EncryptedDataStatesEnum.ENCRYPTED_DATA_KVNO_TAG_STATE, EncryptedDataStatesEnum.ENCRYPTED_DATA_KVNO_STATE, UniversalTag.INTEGER.getValue(),
            new GrammarAction( "EncryptedData kvno" )
            {
                public void action( Asn1Container container ) throws DecoderException
                {
                    EncryptedDataContainer encryptedDataContainer = ( EncryptedDataContainer ) container;

                    TLV tlv = encryptedDataContainer.getCurrentTLV();

                    // The Length should not be null
                    if ( tlv.getLength() == 0 )
                    {
                        LOG.error( I18n.err( I18n.ERR_04066 ) );

                        // This will generate a PROTOCOL_ERROR
                        throw new DecoderException( I18n.err( I18n.ERR_04067 ) );
                    }
                    
                    Value value = tlv.getValue();
                    
                    try
                    {
                        int kvno = IntegerDecoder.parse( value, 0, Integer.MAX_VALUE );

                        EncryptedData encryptedData = encryptedDataContainer.getEncryptedData();
                        encryptedData.setKvno( kvno );

                        if ( IS_DEBUG )
                        {
                            LOG.debug( "kvno : {}", kvno );
                        }
                    }
                    catch ( IntegerDecoderException ide )
                    {
                        LOG.error( I18n.err( I18n.ERR_04070, StringTools.dumpBytes( value.getData() ), ide
                            .getLocalizedMessage() ) );

                        // This will generate a PROTOCOL_ERROR
                        throw new DecoderException( ide.getMessage() );
                    }
                }
            });
        
        // --------------------------------------------------------------------------------------------
        // Transition from kvno value value to cipher tag
        // --------------------------------------------------------------------------------------------
        // EncryptedData   ::= SEQUENCE {
        //         ...
        //         cipher     [2]
        super.transitions[EncryptedDataStatesEnum.ENCRYPTED_DATA_KVNO_STATE.ordinal()][KerberosConstants.ENCRYPTED_DATA_CIPHER_TAG] = new GrammarTransition(
            EncryptedDataStatesEnum.ENCRYPTED_DATA_KVNO_STATE, EncryptedDataStatesEnum.ENCRYPTED_DATA_CIPHER_TAG_STATE, KerberosConstants.ENCRYPTED_DATA_CIPHER_TAG,
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from cipher tag to cipher value
        // --------------------------------------------------------------------------------------------
        // EncryptedData   ::= SEQUENCE {
        //         ...
        //         cipher     [2] OCTET STRING
        super.transitions[EncryptedDataStatesEnum.ENCRYPTED_DATA_CIPHER_TAG_STATE.ordinal()][UniversalTag.OCTET_STRING.getValue()] = new GrammarTransition(
            EncryptedDataStatesEnum.ENCRYPTED_DATA_CIPHER_TAG_STATE, EncryptedDataStatesEnum.ENCRYPTED_DATA_CIPHER_STATE, UniversalTag.OCTET_STRING.getValue(),
            new GrammarAction( "EncryptedData SEQUENCE" )
            {
                public void action( Asn1Container container ) throws DecoderException
                {
                    EncryptedDataContainer encryptedDataContainer = ( EncryptedDataContainer ) container;

                    TLV tlv = encryptedDataContainer.getCurrentTLV();

                    // The Length should not be null
                    if ( tlv.getLength() == 0 ) 
                    {
                        LOG.error( I18n.err( I18n.ERR_04066 ) );

                        // This will generate a PROTOCOL_ERROR
                        throw new DecoderException( I18n.err( I18n.ERR_04067 ) );
                    }
                    
                    Value value = tlv.getValue();
                    
                    // The encrypted data should not be null
                    if ( value.getData() == null ) 
                    {
                        LOG.error( I18n.err( I18n.ERR_04066 ) );

                        // This will generate a PROTOCOL_ERROR
                        throw new DecoderException( I18n.err( I18n.ERR_04067 ) );
                    }
                    
                    EncryptedData encryptedData = encryptedDataContainer.getEncryptedData();
                    encryptedData.setCipher( value.getData() );
                    
                    if ( IS_DEBUG )
                    {
                        LOG.debug( "cipher : {}", StringTools.dumpBytes( value.getData() ) );
                    }
                    
                    encryptedDataContainer.setGrammarEndAllowed( true );
                }
            } );
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Get the instance of this grammar
     * 
     * @return An instance on the PrincipalName Grammar
     */
    public static Grammar getInstance()
    {
        return instance;
    }
}
