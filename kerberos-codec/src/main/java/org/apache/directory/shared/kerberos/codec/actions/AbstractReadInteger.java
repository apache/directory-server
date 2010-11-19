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
package org.apache.directory.shared.kerberos.codec.actions;


import org.apache.directory.shared.asn1.ber.Asn1Container;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.util.IntegerDecoder;
import org.apache.directory.shared.asn1.util.IntegerDecoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to read an integer value
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractReadInteger extends GrammarAction
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( AbstractReadInteger.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** the acceptable minimum value for the expected value to be parsed */
    private int minValue = 0;

    /** the acceptable maximum value for the expected value to be parsed */
    private int maxValue = Integer.MAX_VALUE;


    /**
     * Instantiates a new AbstractReadInteger action.
     */
    public AbstractReadInteger( String name )
    {
        super( name );
    }


    /**
     * 
     * Creates a new instance of AbstractReadInteger.
     *
     * @param name the action's name
     * @param minValue the acceptable minimum value for the expected value to be read
     * @param maxValue the acceptable maximum value for the value to be read
     */
    public AbstractReadInteger( String name, int minValue, int maxValue )
    {
        super( name );

        this.minValue = minValue;
        this.maxValue = maxValue;
    }


    /**
     * 
     * set the integer value to the appropriate field of ASN.1 object present in the container
     * 
     * @param value the integer value
     * @param container the ASN.1 object's container
     */
    protected abstract void setIntegerValue( int value, Asn1Container container );


    /**
     * {@inheritDoc}
     */
    public final void action( Asn1Container container ) throws DecoderException
    {
        TLV tlv = container.getCurrentTLV();

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
            int number = IntegerDecoder.parse( value, minValue, maxValue );

            if ( IS_DEBUG )
            {
                LOG.debug( "read integer value : {}", number );
            }
            
            setIntegerValue( number, container );
        }
        catch ( IntegerDecoderException ide )
        {
            LOG.error( I18n.err( I18n.ERR_04070, StringTools.dumpBytes( value.getData() ), ide
                .getLocalizedMessage() ) );

            // This will generate a PROTOCOL_ERROR
            throw new DecoderException( ide.getMessage() );
        }
    }
}
