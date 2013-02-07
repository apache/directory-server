/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.kerberos.client;


import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.api.util.Strings;
import org.apache.directory.server.kerberos.protocol.codec.KerberosDecoder;
import org.apache.directory.server.kerberos.protocol.codec.KerberosProtocolCodecFactory;
import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KerberosKeyFactory;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KeyUsage;
import org.apache.directory.shared.kerberos.KerberosMessageType;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.options.KdcOptions;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.codec.types.PaDataType;
import org.apache.directory.shared.kerberos.codec.types.PrincipalNameType;
import org.apache.directory.shared.kerberos.components.EncryptedData;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.components.HostAddress;
import org.apache.directory.shared.kerberos.components.HostAddresses;
import org.apache.directory.shared.kerberos.components.KdcReqBody;
import org.apache.directory.shared.kerberos.components.PaData;
import org.apache.directory.shared.kerberos.components.PaEncTsEnc;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.apache.directory.shared.kerberos.exceptions.ErrorType;
import org.apache.directory.shared.kerberos.exceptions.KerberosException;
import org.apache.directory.shared.kerberos.messages.AsRep;
import org.apache.directory.shared.kerberos.messages.AsReq;
import org.apache.directory.shared.kerberos.messages.EncAsRepPart;
import org.apache.directory.shared.kerberos.messages.KerberosMessage;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A client to connect to Kerberos server and retrieve TGTs
 * 
 * WARN: still experimental, no doco and code is still convoluted a bit
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KerberosConnection extends IoHandlerAdapter
{
    /** logger for reporting errors that might not be handled properly upstream */
    private static final Logger LOG = LoggerFactory.getLogger( KerberosConnection.class );

    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    private IoConnector connector;

    private IoSession kerberosSession;

    private IoFilter filter = new ProtocolCodecFilter( KerberosProtocolCodecFactory.getInstance() );

    private String hostName;

    private int port = 88; // default value

    private CipherTextHandler lockBox = new CipherTextHandler();

    private SecureRandom random;

    private Map<Integer, ReplyFuture> futureMap;

    private EncryptionKey key;

    /** The timeout used for response we are waiting for */
    private long timeout = 30000L;


    public KerberosConnection( String hostName )
    {
        this.hostName = hostName;
    }


    public KerberosConnection( String hostName, int port )
    {
        this.hostName = hostName;
        this.port = port;
    }


    public boolean connect()
    {
        if ( connector != null )
        {
            return true;
        }

        random = new SecureRandom();
        futureMap = new HashMap<Integer, ReplyFuture>();

        connector = new NioSocketConnector();
        connector.getFilterChain().addLast( "kerberoscodec", filter );
        connector.setHandler( this );

        SocketAddress address = new InetSocketAddress( hostName, port );

        LOG.debug( "trying to establish connection to the kerberso server {} running at port {}", hostName, port );
        ConnectFuture connectFuture = connector.connect( address );

        connectFuture.awaitUninterruptibly();

        if ( !connectFuture.isConnected() )
        {
            close();
            return false;
        }

        kerberosSession = connectFuture.getSession();

        return true;
    }


    public void close()
    {
        if ( connector == null )
        {
            return;
        }

        connector.dispose();
        connector = null;
    }


    public void getTicketGrantingTicket( KerberosPrincipal principal, KerberosPrincipal targetPrincipal,
        String password, ClientRequestOptions clientOptions ) throws KerberosException
    {
        ReplyFuture future = getTicketGrantingTicketAsync( principal, targetPrincipal, password, clientOptions );

        try
        {
            KerberosMessage msg = future.get( timeout, TimeUnit.MILLISECONDS );

            if ( IS_DEBUG )
            {
                LOG.debug( "received TGT {}", msg );
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }


    public ReplyFuture getTicketGrantingTicketAsync( KerberosPrincipal principal, KerberosPrincipal targetPrincipal,
        String password, ClientRequestOptions clientOptions ) throws KerberosException
    {
        try
        {

            KdcReqBody reqBody = new KdcReqBody();

            KdcOptions kdcOptions = new KdcOptions();
            reqBody.setKdcOptions( kdcOptions );

            reqBody.setCName( new PrincipalName( principal ) );
            reqBody.setRealm( principal.getRealm() );
            PrincipalName srvPrincipal = new PrincipalName( targetPrincipal );
            srvPrincipal.setNameType( PrincipalNameType.KRB_NT_SRV_INST );
            reqBody.setSName( srvPrincipal );

            Date prefStartTime = clientOptions.getStartTime();
            if ( prefStartTime != null )
            {
                reqBody.setFrom( new KerberosTime( prefStartTime ) );
            }

            long currentTime = System.currentTimeMillis();
            KerberosTime lifeTime = new KerberosTime( clientOptions.getLifeTime() + currentTime );
            reqBody.setTill( lifeTime );

            if ( clientOptions.getRenewableLifetime() > 0 )
            {
                reqBody.setRtime( new KerberosTime( clientOptions.getRenewableLifetime() + currentTime ) );
                kdcOptions.setFlag( KdcOptions.RENEWABLE );
            }

            int nonce = random.nextInt();
            reqBody.setNonce( nonce );

            Set<EncryptionType> ciphers = clientOptions.getEncryptionTypes();

            reqBody.setEType( ciphers );

            if ( clientOptions.getClientAddresses() != null )
            {
                HostAddresses addresses = new HostAddresses();
                for ( InetAddress ia : clientOptions.getClientAddresses() )
                {
                    addresses.addHostAddress( new HostAddress( ia ) );
                }

                reqBody.setAddresses( addresses );
            }

            if ( clientOptions.isAllowPostdate() )
            {
                kdcOptions.setFlag( KdcOptions.ALLOW_POSTDATE );
            }

            if ( clientOptions.isProxiable() )
            {
                kdcOptions.setFlag( KdcOptions.PROXIABLE );
            }

            if ( clientOptions.isForwardable() )
            {
                kdcOptions.setFlag( KdcOptions.FORWARDABLE );
            }

            Map<EncryptionType, EncryptionKey> keys = KerberosKeyFactory.getKerberosKeys( principal.getName(),
                password, ciphers );

            /** The client's encryption key. */
            key = keys.get( ciphers.iterator().next() ); // FIXME this is always taking first cipher, not good

            PaData paData = new PaData();

            if ( clientOptions.isUsePaEncTimestamp() )
            {

                PaEncTsEnc paEncTimeStamp = new PaEncTsEnc( new KerberosTime(), 0 );

                EncryptedData encryptedData = null;

                try
                {
                    encryptedData = lockBox.seal( key, paEncTimeStamp, KeyUsage.AS_REQ_PA_ENC_TIMESTAMP_WITH_CKEY );
                }
                catch ( KerberosException ke )
                {
                    LOG.error( "Unexpected exception encrypting timestamp.", ke );
                }

                ByteBuffer buf = ByteBuffer.allocate( encryptedData.computeLength() );
                byte[] encodedEncryptedData = encryptedData.encode( buf ).array();
                paData.setPaDataType( PaDataType.PA_ENC_TIMESTAMP );

                paData.setPaDataValue( encodedEncryptedData );
            }

            AsReq request = new AsReq();
            request.setKdcReqBody( reqBody );
            request.addPaData( paData );

            ReplyFuture repFuture = new ReplyFuture();

            futureMap.put( nonce, repFuture );

            // Send the request to the server
            WriteFuture writeFuture = kerberosSession.write( request );

            // Wait for the message to be sent to the server
            if ( !writeFuture.awaitUninterruptibly( timeout ) )
            {
                // We didn't received anything : this is an error
                LOG.error( "Search failed : timeout occured" );

                throw new KerberosException( ErrorType.KRB_ERR_GENERIC, "operation timed out" );
            }

            return repFuture;
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            throw new KerberosException( ErrorType.KRB_ERR_GENERIC, e );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void exceptionCaught( IoSession session, Throwable cause ) throws Exception
    {
        LOG.warn( "", cause );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void messageReceived( IoSession session, Object message ) throws Exception
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Received reply:  {}", message );
        }

        KerberosMessage krbMessage = ( KerberosMessage ) message;

        KerberosMessageType messageType = krbMessage.getMessageType();

        try
        {
            switch ( messageType )
            {
                case AS_REP:

                    AsRep asrep = ( AsRep ) krbMessage;

                    byte[] encAsRepPartData = lockBox.decrypt( key, asrep.getEncPart(),
                        KeyUsage.AS_REP_ENC_PART_WITH_CKEY );
                    System.out.println( Strings.dumpBytes( encAsRepPartData ) );
                    EncAsRepPart encAsRepPart = KerberosDecoder.decodeEncAsRepPart( encAsRepPartData );
                    asrep.setEncKdcRepPart( encAsRepPart.getEncKdcRepPart() );

                    ReplyFuture future = futureMap.remove( asrep.getNonce() );
                    future.set( krbMessage );
                    break;

                case TGS_REP:
                    break;

                case KRB_ERROR:
                    break;
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }
}
