/*
 *   Copyright 2004 The Apache Software Foundation
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
package org.apache.directory.shared.ldap.codec;


import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.asn1.codec.stateful.EncoderCallback;
import org.apache.directory.shared.asn1.codec.stateful.EncoderMonitor;
import org.apache.directory.shared.asn1.codec.stateful.StatefulEncoder;
import org.apache.directory.shared.ldap.message.spi.Provider;
import org.apache.directory.shared.ldap.message.spi.ProviderEncoder;
import org.apache.directory.shared.ldap.message.spi.ProviderException;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;


/**
 * Twix LDAP BER provider's encoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class TwixEncoder implements ProviderEncoder
{
    /** The logger */
    private static Logger log = LoggerFactory.getLogger( TwixEncoder.class );

    /** A speedup for logger */
    private static final boolean IS_DEBUG = log.isDebugEnabled();
    
    /** The associated Provider */
    private final Provider provider;

    /** The callback to call when the encoding is done */
    private EncoderCallback encodeCallback;


    /**
     * Creates an instance of a Twix Encoder implementation.
     * 
     * @param provider
     *            The associated Provider
     */
    public TwixEncoder(Provider provider)
    {
        this.provider = provider;
        encodeCallback = new OutputCallback();
    }


    /**
     * Encodes a LdapMessage, and calls the callback.
     * 
     * @param lock
     *            Not used...
     * @param out
     *            Not used ...
     * @param obj
     *            The LdapMessage to encode
     * @throws ProviderException
     *             If anything went wrong
     */
    public void encodeBlocking( Object lock, OutputStream out, Object obj ) throws ProviderException
    {
        try
        {
            if ( IS_DEBUG )
            {
                log.debug( "Encoding this LdapMessage : " + obj );
            }

            encodeCallback.encodeOccurred( null, ( ( LdapMessage ) obj ).encode( null ) );
        }
        catch ( EncoderException e )
        {
            log.error( "Twix encoder failed to encode object: " + obj + ", error : " + e.getMessage() );
            ProviderException pe = new ProviderException( provider, "Twix encoder failed to encode object: " + obj
                + ", error : " + e.getMessage() );
            throw pe;
        }
    }


    /**
     * Encodes a LdapMessage, and return a ByteBuffer containing the resulting
     * PDU
     * 
     * @param obj
     *            The LdapMessage to encode
     * @return The ByteBuffer containing the PDU
     * @throws ProviderException
     *             If anything went wrong
     */
    public ByteBuffer encodeBlocking( Object obj ) throws ProviderException
    {
        try
        {
            if ( IS_DEBUG )
            {
                log.debug( "Encoding this LdapMessage : " + obj );
            }

            ByteBuffer pdu = ( ( LdapMessage ) obj ).encode( null );

            if ( IS_DEBUG )
            {
                log.debug( "Encoded PDU : " + StringTools.dumpBytes( pdu.array() ) );
            }

            pdu.flip();
            return pdu;
        }
        catch ( EncoderException e )
        {
            log.error( "Twix encoder failed to encode object: " + obj + ", error : " + e.getMessage() );
            ProviderException pe = new ProviderException( provider, "Twix encoder failed to encode object: " + obj
                + ", error : " + e.getMessage() );
            throw pe;
        }
    }


    /**
     * Encodes a LdapMessage, and return a byte array containing the resulting
     * PDU
     * 
     * @param obj
     *            The LdapMessage to encode
     * @return The byte[] containing the PDU
     * @throws ProviderException
     *             If anything went wrong
     */
    public byte[] encodeToArray( Object obj ) throws ProviderException
    {
        try
        {
            if ( IS_DEBUG )
            {
                log.debug( "Encoding this LdapMessage : " + obj );
            }

            byte[] pdu = ( ( LdapMessage ) obj ).encode( null ).array();

            if ( IS_DEBUG )
            {
                log.debug( "Encoded PDU : " + StringTools.dumpBytes( pdu ) );
            }

            return pdu;
        }
        catch ( EncoderException e )
        {
            log.error( "Twix encoder failed to encode object: " + obj + ", error : " + e.getMessage() );
            ProviderException pe = new ProviderException( provider, "Twix encoder failed to encode object: " + obj
                + ", error : " + e.getMessage() );
            throw pe;
        }
    }


    /**
     * Gets the Provider associated with this SPI implementation object.
     * 
     * @return Provider The provider
     */
    public Provider getProvider()
    {
        return provider;
    }


    /**
     * Encodes a LdapMessage, and calls the callback
     * 
     * @param obj
     *            The LdapMessage to encode
     * @throws EncoderException
     *             If anything went wrong
     */
    public void encode( Object obj ) throws EncoderException
    {
        ByteBuffer encoded = encodeBlocking( obj );
        encodeCallback.encodeOccurred( null, encoded );
    }


    /**
     * Set the callback called when the encoding is done.
     * 
     * @param cb
     *            The callback.
     */
    public void setCallback( EncoderCallback cb )
    {
        encodeCallback = cb;
    }


    /**
     * Not used ...
     * 
     * @deprecated
     */
    public void setEncoderMonitor( EncoderMonitor monitor )
    {
    }

    /**
     * The inner class used to write the PDU to a channel.
     */
    class OutputCallback implements EncoderCallback
    {
        /** The channel in which the PDU will be written */
        private WritableByteChannel channel = null;


        /**
         * Callback to deliver a fully encoded object.
         * 
         * @param encoder
         *            the stateful encoder driving the callback
         * @param encoded
         *            the object that was encoded
         */
        public void encodeOccurred( StatefulEncoder encoder, Object encoded )
        {
            try
            {
                channel.write( ( ByteBuffer ) encoded );
            }
            catch ( IOException e )
            {
                ProviderException pe = new ProviderException( provider,
                    "Twix encoder failed to encode object, error : " + e.getMessage() );
                throw pe;
            }
        }


        /**
         * Associate a channel to the callback
         * 
         * @param channel
         *            The channel to use to write a PDU
         */
        void attach( WritableByteChannel channel )
        {
            this.channel = channel;
        }


        /**
         * Associate a OutputStream to the callback. A channel will be created.
         * 
         * @param out
         *            The OutputStream to use
         */
        void attach( OutputStream out )
        {
            this.channel = Channels.newChannel( out );
        }
    }
}
