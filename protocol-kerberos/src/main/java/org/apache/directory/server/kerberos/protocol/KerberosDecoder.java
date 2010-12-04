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
package org.apache.directory.server.kerberos.protocol;


import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.shared.asn1.ber.Asn1Container;
import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.TLVStateEnum;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.kerberos.codec.KerberosMessageContainer;
import org.apache.directory.shared.kerberos.codec.encryptedData.EncryptedDataContainer;
import org.apache.directory.shared.kerberos.codec.paEncTsEnc.PaEncTsEncContainer;
import org.apache.directory.shared.kerberos.components.EncryptedData;
import org.apache.directory.shared.kerberos.components.PaEncTsEnc;
import org.apache.directory.shared.kerberos.exceptions.ErrorType;
import org.apache.directory.shared.ldap.codec.LdapDecoder;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderAdapter;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KerberosDecoder extends ProtocolDecoderAdapter
{

    /** The logger */
    private static Logger LOG = LoggerFactory.getLogger( LdapDecoder.class );

    /** A speedup for logger */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The ASN 1 decoder instance */
    private Asn1Decoder asn1Decoder = new Asn1Decoder();

    /** the key used while storing message container in the session */
    private static final String KERBEROS_MESSAGE_CONTAINER = "kerberosMessageContainer";
    
    public void decode( IoSession session, IoBuffer in, ProtocolDecoderOutput out ) throws IOException
    {
        ByteBuffer buf = in.buf();
        KerberosMessageContainer kerberosMessageContainer = ( KerberosMessageContainer ) session.getAttribute( KERBEROS_MESSAGE_CONTAINER );

        //System.out.println( "IN : " + StringTools.dumpBytes( buf.array() ) );
        
        if ( kerberosMessageContainer == null )
        {
            kerberosMessageContainer = new KerberosMessageContainer();
            session.setAttribute( KERBEROS_MESSAGE_CONTAINER, kerberosMessageContainer );
            kerberosMessageContainer.setStream( buf );
            kerberosMessageContainer.setGathering( true );
        }
        
        while ( buf.hasRemaining() )
        {
            try
            {
                asn1Decoder.decode( buf, kerberosMessageContainer );
                
                TLV tlv = kerberosMessageContainer.getCurrentTLV();
                Value value = tlv.getValue();

                if ( kerberosMessageContainer.getState() == TLVStateEnum.PDU_DECODED )
                {
                    if ( IS_DEBUG )
                    {
                        LOG.debug( "Decoded KerberosMessage : " + kerberosMessageContainer.getMessage() );
                        buf.mark();
                    }
        
                    out.write( kerberosMessageContainer.getMessage() );
        
                    kerberosMessageContainer.clean();
                }
            }
            catch ( DecoderException de )
            {
                buf.clear();
                kerberosMessageContainer.clean();
            }
            catch ( Exception e )
            {
                LOG.warn( "error while decoding", e );
            }
        }
    }
    
    
    /**
     * Decode an EncrytedData structure
     * 
     * @param data The byte array containing the data structure to decode
     * @return An instance of EncryptedData
     * @throws KerberosException If the decoding fails
     */
    public static EncryptedData decodeEncryptedData( byte[] data ) throws KerberosException
    {
        ByteBuffer stream = ByteBuffer.allocate( data.length );
        stream.put( data );
        stream.flip();
        
        // Allocate a EncryptedData Container
        Asn1Container encryptedDataContainer = new EncryptedDataContainer();

        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        // Decode the EncryptedData PDU
        try
        {
            kerberosDecoder.decode( stream, encryptedDataContainer );
        }
        catch ( DecoderException de )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BAD_INTEGRITY, de );
        }

        // get the decoded EncryptedData
        EncryptedData encryptedData = ( ( EncryptedDataContainer ) encryptedDataContainer ).getEncryptedData();

        return encryptedData;
    }
    
    
    /**
     * Decode an PaEncTsEnc structure
     * 
     * @param data The byte array containing the data structure to decode
     * @return An instance of PaEncTsEnc
     * @throws KerberosException If the decoding fails
     */
    public static PaEncTsEnc decodePaEncTsEnc( byte[] data ) throws KerberosException
    {
        ByteBuffer stream = ByteBuffer.allocate( data.length );
        stream.put( data );
        stream.flip();
        
        // Allocate a PaEncTsEnc Container
        Asn1Container paEncTsEncContainer = new PaEncTsEncContainer();

        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        // Decode the PaEncTsEnc PDU
        try
        {
            kerberosDecoder.decode( stream, paEncTsEncContainer );
        }
        catch ( DecoderException de )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BAD_INTEGRITY, de );
        }

        // get the decoded EncryptedData
        PaEncTsEnc paEncTsEnc = ( ( PaEncTsEncContainer ) paEncTsEncContainer ).getPaEncTsEnc();

        return paEncTsEnc;
    }
}
