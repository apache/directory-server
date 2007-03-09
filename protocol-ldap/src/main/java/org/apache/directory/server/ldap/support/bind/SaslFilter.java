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
package org.apache.directory.server.ldap.support.bind;


import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An {@link IoFilterAdapter} that handles privacy and confidentiality protection
 * for a SASL bound session.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SaslFilter extends IoFilterAdapter
{
    private static final Logger log = LoggerFactory.getLogger( SaslFilter.class );

    private static final String SASL_CONTEXT = "saslContext";
    private static final String SASL_STATE = "saslState";


    public void messageReceived( NextFilter nextFilter, IoSession session, Object message ) throws SaslException
    {
        log.debug( "Message received:  " + message );

        /*
         * Guard clause:  check if in SASL bound mode.
         */
        Boolean useSasl = ( Boolean ) session.getAttribute( SASL_STATE );

        if ( useSasl == null || !useSasl.booleanValue() )
        {
            log.debug( "Will not use SASL on received message." );
            nextFilter.messageReceived( session, message );
            return;
        }

        /*
         * Unwrap the data for mechanisms that support QoP (DIGEST-MD5, GSSAPI).
         */
        SaslServer context = getContext( session );
        String qop = ( String ) context.getNegotiatedProperty( Sasl.QOP );
        boolean hasSecurityLayer = ( qop != null && ( qop.equals( "auth-int" ) || qop.equals( "auth-conf" ) ) );

        if ( hasSecurityLayer )
        {
            /*
             * Get the buffer as bytes.  First 4 bytes are length as int.
             */
            ByteBuffer buf = ( ByteBuffer ) message;
            int bufferLength = buf.getInt();
            byte[] bufferBytes = new byte[bufferLength];
            buf.get( bufferBytes );

            log.debug( "Will use SASL to unwrap received message of length:  " + bufferLength );
            byte[] token = context.unwrap( bufferBytes, 0, bufferBytes.length );
            nextFilter.messageReceived( session, ByteBuffer.wrap( token ) );
        }
        else
        {
            log.debug( "Will not use SASL on received message." );
            nextFilter.messageReceived( session, message );
        }
    }


    public void filterWrite( NextFilter nextFilter, IoSession session, WriteRequest writeRequest ) throws SaslException
    {
        log.debug( "Filtering write request:  " + writeRequest );

        /*
         * Guard clause:  check if in SASL bound mode.
         */
        Boolean useSasl = ( Boolean ) session.getAttribute( SASL_STATE );

        if ( useSasl == null || !useSasl.booleanValue() )
        {
            log.debug( "Will not use SASL on write request." );
            nextFilter.filterWrite( session, writeRequest );
            return;
        }

        /*
         * Wrap the data for mechanisms that support QoP (DIGEST-MD5, GSSAPI).
         */
        SaslServer context = getContext( session );
        String qop = ( String ) context.getNegotiatedProperty( Sasl.QOP );
        boolean hasSecurityLayer = ( qop != null && ( qop.equals( "auth-int" ) || qop.equals( "auth-conf" ) ) );

        ByteBuffer saslLayerBuffer = null;

        if ( hasSecurityLayer )
        {
            /*
             * Get the buffer as bytes.
             */
            ByteBuffer buf = ( ByteBuffer ) writeRequest.getMessage();
            int bufferLength = buf.remaining();
            byte[] bufferBytes = new byte[bufferLength];
            buf.get( bufferBytes );

            log.debug( "Will use SASL to wrap message of length:  " + bufferLength );

            byte[] saslLayer = context.wrap( bufferBytes, 0, bufferBytes.length );

            /*
             * Prepend 4 byte length.
             */
            saslLayerBuffer = ByteBuffer.allocate( 4 + saslLayer.length );
            saslLayerBuffer.putInt( saslLayer.length );
            saslLayerBuffer.put( saslLayer );
            saslLayerBuffer.position( 0 );
            saslLayerBuffer.limit( 4 + saslLayer.length );

            log.debug( "Sending encrypted token of length " + saslLayerBuffer.limit() );
            nextFilter.filterWrite( session, new WriteRequest( saslLayerBuffer, writeRequest.getFuture() ) );
        }
        else
        {
            log.debug( "Will not use SASL on write request." );
            nextFilter.filterWrite( session, writeRequest );
        }
    }


    /**
     * Helper method to get the {@link SaslServer} and perform basic checks.
     *  
     * @param session The {@link IoSession}
     * @return {@link SaslServer} The {@link SaslServer} stored in the session.
     */
    private SaslServer getContext( IoSession session )
    {
        SaslServer context = null;

        if ( session.containsAttribute( SASL_CONTEXT ) )
        {
            context = ( SaslServer ) session.getAttribute( SASL_CONTEXT );
        }

        if ( context == null )
        {
            throw new IllegalStateException();
        }

        return context;
    }
}
