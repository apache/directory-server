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
package org.apache.directory.server.ldap;


import org.apache.directory.ldap.client.api.protocol.LdapProtocolEncoder;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.codec.stateful.DecoderCallback;
import org.apache.directory.shared.asn1.codec.stateful.StatefulDecoder;
import org.apache.directory.shared.ldap.message.MessageDecoder;
import org.apache.directory.shared.ldap.message.spi.BinaryAttributeDetector;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.util.StringTools;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderAdapter;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoder;


/**
 * An LDAP BER Decoder/Encoder factory implementing {@link ProtocolCodecFactory}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
final class LdapProtocolCodecFactory implements ProtocolCodecFactory
{
    private class Asn1CodecDecoder extends ProtocolDecoderAdapter
    {
        /** The stateful decoder */
        private final StatefulDecoder decoder;

        /** The Output queue */
        private ProtocolDecoderOutput decOut;


        /**
         * Creates a new instance of Asn1CodecDecoder.
         * 
         * @param decoder The associated decoder
         */
        public Asn1CodecDecoder( StatefulDecoder decoder )
        {
            this.decoder = decoder;
            this.decoder.setCallback( new DecoderCallback()
            {
                public void decodeOccurred( StatefulDecoder decoder, Object decoded )
                {
                    decOut.write( decoded );
                }
            } );
        }


        /**
         * {@inheritDoc}
         */
        public void decode( IoSession session, IoBuffer in, ProtocolDecoderOutput out ) throws DecoderException
        {
            decOut = out;
            decoder.decode( in.buf() );
        }
    }


    /** the directory service for which this factor generates codecs */
    final private DirectoryService directoryService;

    /** The tag stored into the session if we want to set a max PDU size */
    public final static String MAX_PDU_SIZE = "MAX_PDU_SIZE";


    /**
     * Creates a new instance of LdapProtocolCodecFactory.
     *
     * @param directoryService the {@link DirectoryService} for which this 
     * factory generates codecs.
     */
    public LdapProtocolCodecFactory( DirectoryService directoryService )
    {
        this.directoryService = directoryService;
    }


    /**
     * {@inheritDoc}
     */
    public ProtocolEncoder getEncoder( IoSession session )
    {
        return new LdapProtocolEncoder();
    }


    /**
     * {@inheritDoc}
     */
    public ProtocolDecoder getDecoder( IoSession session )
    {
        return new Asn1CodecDecoder( new MessageDecoder( new BinaryAttributeDetector()
        {
            public boolean isBinary( String id )
            {
                SchemaManager schemaManager = directoryService.getSchemaManager();

                try
                {
                    AttributeType type = schemaManager.lookupAttributeTypeRegistry( id );
                    return !type.getSyntax().isHumanReadable();
                }
                catch ( Exception e )
                {
                    if ( StringTools.isEmpty( id ) )
                    {
                        return false;
                    }

                    return id.endsWith( ";binary" );
                }
            }
        }, directoryService.getMaxPDUSize() ) );
    }
}