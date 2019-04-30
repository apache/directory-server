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


import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.ber.Asn1Container;
import org.apache.directory.api.asn1.ber.grammar.GrammarAction;
import org.apache.directory.api.asn1.ber.tlv.BerValue;
import org.apache.directory.api.asn1.ber.tlv.IntegerDecoder;
import org.apache.directory.api.asn1.ber.tlv.IntegerDecoderException;
import org.apache.directory.api.asn1.ber.tlv.TLV;
import org.apache.directory.api.i18n.I18n;
import org.apache.directory.api.util.Strings;
import org.apache.directory.shared.kerberos.KerberosMessageType;
import org.apache.directory.shared.kerberos.codec.kdcRep.KdcRepContainer;
import org.apache.directory.shared.kerberos.codec.kdcReq.KdcReqContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to read and validate the msg-type
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractReadMsgType<E extends Asn1Container> extends GrammarAction<E>
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( AbstractReadMsgType.class );

    /** The msgType to decode */
    private KerberosMessageType msgType = null;


    /**
     * Instantiates a new StoreMsgType action.
     */
    public AbstractReadMsgType( String name )
    {
        super( name );
    }


    /**
     * Instantiates a new StoreMsgType action.
     */
    public AbstractReadMsgType( String name, KerberosMessageType msgType )
    {
        super( name );
        this.msgType = msgType;
    }


    /**
     * {@inheritDoc}
     */
    public final void action( E container ) throws DecoderException
    {
        TLV tlv = container.getCurrentTLV();

        // The Length should not be null and should be 1
        if ( tlv.getLength() != 1 )
        {
            LOG.error( I18n.err( I18n.ERR_01308_ZERO_LENGTH_TLV ) );

            // This will generate a PROTOCOL_ERROR
            throw new DecoderException( I18n.err( I18n.ERR_01309_EMPTY_TLV ) );
        }

        BerValue value = tlv.getValue();

        try
        {
            int msgTypeValue = IntegerDecoder.parse( value );

            if ( msgType != null )
            {
                if ( msgType.getValue() == msgTypeValue )
                {
                    LOG.debug( "msg-type : {}", msgType );

                    return;
                }

                String message = I18n.err( I18n.ERR_05102_INVALID_MESSAGE_ID, Strings.dumpBytes( value.getData() ) );
                LOG.error( message );

                // This will generate a PROTOCOL_ERROR
                throw new DecoderException( message );
            }
            else
            {
                KerberosMessageType messageType = KerberosMessageType.getTypeByValue( msgTypeValue );

                if ( ( container instanceof KdcReqContainer )
                        &&  ( ( ( KdcReqContainer ) container ).getKdcReq().getMessageType() == messageType ) )
                {
                    return;
                }
                else if ( ( container instanceof KdcRepContainer )
                        && ( ( ( KdcRepContainer ) container ).getKdcRep().getMessageType() == messageType ) )
                {
                    return;
                }

                String message = I18n.err( I18n.ERR_05102_INVALID_MESSAGE_ID, Strings.dumpBytes( value.getData() ) );
                LOG.error( message );

                // This will generate a PROTOCOL_ERROR
                throw new DecoderException( message );
            }
        }
        catch ( IntegerDecoderException ide )
        {
            LOG.error( I18n.err( I18n.ERR_05102_INVALID_MESSAGE_ID, Strings.dumpBytes( value.getData() ), ide
                .getLocalizedMessage() ) );

            // This will generate a PROTOCOL_ERROR
            throw new DecoderException( ide.getMessage() );
        }
    }
}
