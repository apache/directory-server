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
package org.apache.directory.kerberos.client;


import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.AES128_CTS_HMAC_SHA1_96;
import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.DES3_CBC_SHA1_KD;
import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.DES_CBC_MD5;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.text.ParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.api.asn1.Asn1Object;
import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.kerberos.changepwd.exceptions.ChangePasswdErrorType;
import org.apache.directory.server.kerberos.changepwd.exceptions.ChangePasswordException;
import org.apache.directory.server.kerberos.changepwd.io.ChangePasswordDecoder;
import org.apache.directory.server.kerberos.changepwd.io.ChangePasswordEncoder;
import org.apache.directory.server.kerberos.changepwd.messages.AbstractPasswordMessage;
import org.apache.directory.server.kerberos.changepwd.messages.ChangePasswordError;
import org.apache.directory.server.kerberos.changepwd.messages.ChangePasswordRequest;
import org.apache.directory.server.kerberos.protocol.codec.KerberosDecoder;
import org.apache.directory.server.kerberos.protocol.codec.KerberosEncoder;
import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KerberosKeyFactory;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KeyUsage;
import org.apache.directory.server.kerberos.shared.crypto.encryption.RandomKeyFactory;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.KerberosUtils;
import org.apache.directory.shared.kerberos.codec.KerberosMessageContainer;
import org.apache.directory.shared.kerberos.codec.options.ApOptions;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.codec.types.PaDataType;
import org.apache.directory.shared.kerberos.codec.types.PrincipalNameType;
import org.apache.directory.shared.kerberos.components.EncKdcRepPart;
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
import org.apache.directory.shared.kerberos.messages.ApReq;
import org.apache.directory.shared.kerberos.messages.AsRep;
import org.apache.directory.shared.kerberos.messages.AsReq;
import org.apache.directory.shared.kerberos.messages.Authenticator;
import org.apache.directory.shared.kerberos.messages.ChangePasswdData;
import org.apache.directory.shared.kerberos.messages.EncAsRepPart;
import org.apache.directory.shared.kerberos.messages.EncTgsRepPart;
import org.apache.directory.shared.kerberos.messages.KerberosMessage;
import org.apache.directory.shared.kerberos.messages.KrbError;
import org.apache.directory.shared.kerberos.messages.KrbPriv;
import org.apache.directory.shared.kerberos.messages.TgsRep;
import org.apache.directory.shared.kerberos.messages.TgsReq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * A client to connect to kerberos servers using TCP or UDP transports.
 * 
 * @author Kiran Ayyagari
 */
public class KdcConnection
{

    private static final Logger LOG = LoggerFactory.getLogger( KdcConnection.class );
    
    /** host name of the Kerberos server */
    private String host;

    /** port on which the Kerberos server is listening */
    private int port;

    /** flag to indicate if the client should use UDP while connecting to Kerberos server */
    private boolean useUdp;

    /** the timeout of the connection to the Kerberos server */
    private int timeout = 60000; // default 1 min

    /** a secure random number generator used for creating nonces */
    private SecureRandom nonceGenerator;

    /** the set of encryption types that the client can support */
    private Set<EncryptionType> encryptionTypes;
    
    static final String TIME_OUT_ERROR = "TimeOut occured";
    
    /** the cipher text handler */
    private CipherTextHandler cipherTextHandler;

    /** underlying network channel handler */
    private KerberosChannel channel;
    
    /** the default encryption types, this includes <b>many</b> encryption types */
    private static Set<EncryptionType> DEFAULT_ENCRYPTION_TYPES;
    

    static
    {
        DEFAULT_ENCRYPTION_TYPES = new HashSet<EncryptionType>();
        
        DEFAULT_ENCRYPTION_TYPES.add( AES128_CTS_HMAC_SHA1_96 );
        DEFAULT_ENCRYPTION_TYPES.add( DES_CBC_MD5 );
        DEFAULT_ENCRYPTION_TYPES.add( DES3_CBC_SHA1_KD );
//        DEFAULT_ENCRYPTION_TYPES.add( RC4_HMAC );
//        DEFAULT_ENCRYPTION_TYPES.add( RC4_HMAC_EXP );
        
        DEFAULT_ENCRYPTION_TYPES = KerberosUtils.orderEtypesByStrength( DEFAULT_ENCRYPTION_TYPES );
    }
    
    
    /**
     * 
     * Creates a new instance of KdcConnection.
     *
     * @param host the host name of Kerberos server
     * @param port the port on which Kerberos server is listening
     * @param isUdp flag to indicate if UDP should be used instead of TCP
     */
    private KdcConnection( String host, int port, boolean isUdp )
    {
        this.host = host;
        this.port = port;
        this.useUdp = isUdp;
        
        nonceGenerator = new SecureRandom( String.valueOf( System.currentTimeMillis() ).getBytes() );
        cipherTextHandler = new CipherTextHandler();
        channel = new KerberosChannel();
        encryptionTypes = DEFAULT_ENCRYPTION_TYPES;
    }


    /**
     * created a UDP based Kerberos client connection
     * 
     * @param host the host name of Kerberos server
     * @param port the port on which Kerberos server is listening
     * @return
     * @throws Exception
     */
    public static KdcConnection createUdpConnection( String host, int port ) throws Exception
    {
        KdcConnection connection = new KdcConnection( host, port, true );
        
        return connection;
    }


    /**
     * created a TCP based Kerberos client connection
     * 
     * @param host the host name of Kerberos server
     * @param port the port on which Kerberos server is listening
     * @return
     * @throws Exception
     */
    public static KdcConnection createTcpConnection( String host, int port ) throws Exception
    {
        KdcConnection connection = new KdcConnection( host, port, false );
        
        return connection;
    }

    
    private void connect() throws IOException
    {
        channel.openConnection( host, port, timeout, !useUdp );
    }
    
    
    /**
     * Authenticates to the Kerberos server and gets the initial Ticket Granting Ticket
     * 
     * @param principal the client's principal 
     * @param password password of the client
     * @return
     * @throws Exception
     */
    public TgTicket getTgt( String principal, String password ) throws Exception
    {
        TgtRequest clientTgtReq = new TgtRequest();
        
        clientTgtReq.setClientPrincipal( principal );
        clientTgtReq.setPassword( password );
        
        return getTgt( clientTgtReq );
    }
    

    /**
     * Authenticates to the Kerberos server and gets a service ticket for the given server principal
     * 
     * @param principal the client's principal 
     * @param password password of the client
     * @param serverPrincipal the application server's principal
     * @return
     * @throws Exception
     */
    public ServiceTicket getServiceTicket( String clientPrincipal, String password, String serverPrincipal ) throws KerberosException
    {
        TgtRequest clientTgtReq = new TgtRequest();
        clientTgtReq.setClientPrincipal( clientPrincipal );
        clientTgtReq.setPassword( password );

        TgTicket tgt = getTgt( clientTgtReq );
        
        return getServiceTicket( new ServiceTicketRequest( tgt, serverPrincipal ) );
    }

    
    public TgTicket getTgt( TgtRequest clientTgtReq ) throws KerberosException
    {
        String realm = clientTgtReq.getRealm();
        
        if ( clientTgtReq.getServerPrincipal() == null )
        {
            String serverPrincipal = "krbtgt/" + realm + "@" + realm;
            clientTgtReq.setServerPrincipal( serverPrincipal );
        }

        KdcReqBody body = new KdcReqBody();
        
        body.setFrom( new KerberosTime( clientTgtReq.getStartTime() ) );
        
        PrincipalName cName = null;
        try
        {
            cName = new PrincipalName( clientTgtReq.getClientPrincipal(), PrincipalNameType.KRB_NT_PRINCIPAL );
            body.setCName( cName );
            body.setRealm( realm );
            PrincipalName sName = new PrincipalName( clientTgtReq.getServerPrincipal(), PrincipalNameType.KRB_NT_SRV_INST );
            body.setSName( sName );
        }
        catch( ParseException e )
        {
            throw new IllegalArgumentException( "Couldn't parse the given principals", e );
        }
        
        body.setTill( new KerberosTime( clientTgtReq.getExpiryTime() ) );
        int currentNonce = nonceGenerator.nextInt();
        body.setNonce( currentNonce );
        body.setEType( encryptionTypes );
        body.setKdcOptions( clientTgtReq.getOptions() );
        
        List<HostAddress> lstAddresses = clientTgtReq.getHostAddresses();
        if ( !lstAddresses.isEmpty() )
        {
            HostAddresses addresses = new HostAddresses();
            for( HostAddress h : lstAddresses )
            {
                addresses.addHostAddress( h );
            }
            
            body.setAddresses( addresses );
        }
        
        EncryptionType encryptionType = encryptionTypes.iterator().next();
        EncryptionKey clientKey = KerberosKeyFactory.string2Key( clientTgtReq.getClientPrincipal(), clientTgtReq.getPassword(), encryptionType );

        AsReq req = new AsReq();
        req.setKdcReqBody( body );

        if ( clientTgtReq.isPreAuthEnabled() )
        {
            PaEncTsEnc tmstmp = new PaEncTsEnc();
            tmstmp.setPaTimestamp( new KerberosTime() );
            
            EncryptedData paDataValue = cipherTextHandler.encrypt( clientKey, getEncoded( tmstmp ), KeyUsage.AS_REQ_PA_ENC_TIMESTAMP_WITH_CKEY );
            
            PaData paEncTstmp = new PaData();
            paEncTstmp.setPaDataType( PaDataType.PA_ENC_TIMESTAMP );
            paEncTstmp.setPaDataValue( getEncoded( paDataValue ) );
            
            req.addPaData( paEncTstmp );
        }
        
        // Get the result from the future
        try
        {
            connect();
            
            // Read the response, waiting for it if not available immediately
            // Get the response, blocking
            KerberosMessage kdcRep = sendAndReceiveKrbMsg( req );

            if ( kdcRep == null )
            {
                // We didn't received anything : this is an error
                LOG.error( "Authentication failed : timeout occured" );
                throw new KerberosException( ErrorType.KRB_ERR_GENERIC, TIME_OUT_ERROR );
            }

            if ( kdcRep instanceof KrbError )
            {
                // We have an error
                LOG.debug( "Authentication failed : {}", kdcRep );
                throw new KerberosException( ( KrbError ) kdcRep );
            }

            AsRep rep = ( AsRep ) kdcRep;
            
            if ( !cName.getNameString().equals( rep.getCName().getNameString() ) )
            {
                throw new KerberosException( ErrorType.KDC_ERR_CLIENT_NAME_MISMATCH );
            }
            
            if ( !realm.equals( rep.getCRealm() ) )
            {
                throw new KerberosException( ErrorType.KRB_ERR_WRONG_REALM );
            }
            
            byte[] decryptedEncAsRepPart = cipherTextHandler.decrypt( clientKey, rep.getEncPart(), KeyUsage.AS_REP_ENC_PART_WITH_CKEY );
            EncAsRepPart encAsRepPart = KerberosDecoder.decodeEncAsRepPart( decryptedEncAsRepPart );
            
            if ( currentNonce != encAsRepPart.getEncKdcRepPart().getNonce() )
            {
                throw new KerberosException( ErrorType.KRB_ERR_GENERIC, "received nonce didn't match with the nonce sent in the request" );
            }
            
            EncKdcRepPart encKdcRepPart = encAsRepPart.getEncKdcRepPart();
            
            if ( !encKdcRepPart.getSName().getNameString().equals( clientTgtReq.getServerPrincipal() ) )
            {
                throw new KerberosException( ErrorType.KDC_ERR_SERVER_NOMATCH );
            }
            
            if ( !encKdcRepPart.getSRealm().equals( clientTgtReq.getRealm() ) )
            {
                throw new KerberosException( ErrorType.KRB_ERR_GENERIC, "received server realm does not match with requested server realm" );
            }
            
            List<HostAddress> hosts = clientTgtReq.getHostAddresses();
            
            if( !hosts.isEmpty() )
            {
                HostAddresses addresses = encKdcRepPart.getClientAddresses();
                for( HostAddress h : hosts )
                {
                    if ( !addresses.contains( h ) )
                    {
                        throw new KerberosException( ErrorType.KRB_ERR_GENERIC, "requested client address" + h + " is not found in the ticket" );
                    }
                }
            }
            
            // Everything is fine, return the response
            LOG.debug( "Authentication successful : {}", kdcRep );
            
            TgTicket tgTicket = new TgTicket( rep.getTicket(), encKdcRepPart, rep.getCName().getNameString() );
            
            return tgTicket;
        }
        catch( KerberosException ke )
        {
            throw ke;
        }
        catch ( Exception e )
        {
            // We didn't received anything : this is an error
            LOG.error( "Authentication failed" );
            throw new KerberosException( ErrorType.KRB_ERR_GENERIC, TIME_OUT_ERROR );
        }
        finally
        {
            if ( channel != null )
            {
                try
                {
                    channel.close();
                }
                catch( IOException e )
                {
                    LOG.warn( "Failed to close the channel", e );
                }
            }
        }
    }

    
    private ServiceTicket getServiceTicket( ServiceTicketRequest srvTktReq ) throws KerberosException
    {
        String serverPrincipal = srvTktReq.getServerPrincipal();
        
        // session key
        EncryptionKey sessionKey = srvTktReq.getTgt().getSessionKey();
        
        Authenticator authenticator = new Authenticator();
        
        try
        {
            authenticator.setCName( new PrincipalName( srvTktReq.getTgt().getClientName(), PrincipalNameType.KRB_NT_PRINCIPAL ) );
        }
        catch( ParseException e )
        {
            throw new IllegalArgumentException( "Couldn't parse the given principal", e );
        }
        
        authenticator.setCRealm( srvTktReq.getTgt().getRealm() );
        authenticator.setCTime( new KerberosTime() );
        authenticator.setCusec( 0 );

        if( srvTktReq.getSubSessionKey() != null )
        {
            sessionKey = srvTktReq.getSubSessionKey();
            authenticator.setSubKey( sessionKey );
        }
        
        EncryptedData authnData = cipherTextHandler.encrypt( sessionKey, getEncoded( authenticator ), KeyUsage.TGS_REQ_PA_TGS_REQ_PADATA_AP_REQ_TGS_SESS_KEY );
        
        ApReq apReq = new ApReq();
        
        apReq.setAuthenticator( authnData );
        apReq.setTicket( srvTktReq.getTgt().getTicket() );

        apReq.setApOptions( srvTktReq.getApOptions() );
        
        KdcReqBody tgsReqBody = new KdcReqBody();
        tgsReqBody.setKdcOptions( srvTktReq.getKdcOptions() );
        tgsReqBody.setRealm( KdcClientUtil.extractRealm( serverPrincipal ) );
        tgsReqBody.setTill( getDefaultTill() );
        int currentNonce = nonceGenerator.nextInt();
        tgsReqBody.setNonce( currentNonce );
        tgsReqBody.setEType( encryptionTypes );
        
        KerberosPrincipal principal = new KerberosPrincipal( serverPrincipal, KerberosPrincipal.KRB_NT_SRV_HST );
        PrincipalName principalName = new PrincipalName( principal );
        tgsReqBody.setSName( principalName );
        
        TgsReq tgsReq = new TgsReq();
        tgsReq.setKdcReqBody( tgsReqBody );
        
        PaData authnHeader = new PaData();
        authnHeader.setPaDataType( PaDataType.PA_TGS_REQ );
        authnHeader.setPaDataValue( getEncoded( apReq ) );
        
        tgsReq.addPaData( authnHeader );
        
        // Get the result from the future
        try
        {
            connect();
            
            // Read the response, waiting for it if not available immediately
            // Get the response, blocking
            KerberosMessage kdcRep = sendAndReceiveKrbMsg( tgsReq );

            if ( kdcRep == null )
            {
                // We didn't received anything : this is an error
                LOG.error( "TGT request failed : timeout occured" );
                throw new KerberosException( ErrorType.KRB_ERR_GENERIC, TIME_OUT_ERROR );
            }

            if ( kdcRep instanceof KrbError )
            {
                // We have an error
                LOG.debug( "TGT request failed : {}", kdcRep );
                throw new KerberosException( ( KrbError ) kdcRep );
            }

            TgsRep rep = ( TgsRep ) kdcRep;
            byte[] decryptedData = cipherTextHandler.decrypt( sessionKey, rep.getEncPart(), KeyUsage.TGS_REP_ENC_PART_TGS_SESS_KEY );
            EncTgsRepPart encTgsRepPart = KerberosDecoder.decodeEncTgsRepPart( decryptedData );
            
            if ( currentNonce != encTgsRepPart.getEncKdcRepPart().getNonce() )
            {
                throw new KerberosException( ErrorType.KRB_ERR_GENERIC, "received nonce didn't match with the nonce sent in the request" );
            }
            
            
            // Everything is fine, return the response
            LOG.debug( "TGT request successful : {}", rep );

            ServiceTicket srvTkt = new ServiceTicket( rep.getTicket(), encTgsRepPart.getEncKdcRepPart() );
            
            return srvTkt;
        }
        catch( KerberosException e )
        {
            throw e;
        }
        catch ( Exception te )
        {
            // We didn't receive anything : this is an error
            LOG.error( "TGT request failed : timeout occured" );
            throw new KerberosException( ErrorType.KRB_ERR_GENERIC, TIME_OUT_ERROR );
        }
        finally
        {
            if ( channel != null )
            {
                try
                {
                    channel.close();
                }
                catch( IOException e )
                {
                    LOG.warn( "Failed to close the channel", e );
                }
            }
        }
    }
    
    
    public void changePassword( String clientPrincipal, String oldPassword, String newPassword, String host, int port, boolean isUdp ) throws ChangePasswordException
    {
        KerberosChannel channel = null;
        
        try
        {
            TgtRequest clientTgtReq = new TgtRequest();
            clientTgtReq.setClientPrincipal( clientPrincipal );
            clientTgtReq.setPassword( oldPassword );
            clientTgtReq.setServerPrincipal( "kadmin/changepw@" + KdcClientUtil.extractRealm( clientPrincipal ) );
            
            TgTicket tgt = getTgt( clientTgtReq );
            
            ApReq apReq = new ApReq();
            ApOptions options = new ApOptions();
            apReq.setApOptions( options );
            apReq.setTicket( tgt.getTicket() );
            
            Authenticator authenticator = new Authenticator();
            authenticator.setCName( new PrincipalName( tgt.getClientName(), PrincipalNameType.KRB_NT_PRINCIPAL ) );
            authenticator.setCRealm( tgt.getRealm() );
            KerberosTime ctime = new KerberosTime();
            authenticator.setCTime( ctime );
            authenticator.setSeqNumber( nonceGenerator.nextInt() );
            
            EncryptionKey subKey = RandomKeyFactory.getRandomKey( getEncryptionTypes().iterator().next() );
            
            authenticator.setSubKey( subKey );
            
            EncryptedData authData = cipherTextHandler.encrypt( tgt.getSessionKey(), getEncoded( authenticator ), KeyUsage.AP_REQ_AUTHNT_SESS_KEY );
            apReq.setAuthenticator( authData );
            
            
            ChangePasswdData chngPwdData = new ChangePasswdData();
            chngPwdData.setNewPasswd( Strings.getBytesUtf8( newPassword ) );
            
            EncryptedData  chngPwdEncData = cipherTextHandler.encrypt( subKey, getEncoded( chngPwdData ), KeyUsage.KRB_PRIV_ENC_PART_CHOSEN_KEY );
            
            KrbPriv privateMessage = new KrbPriv();
            privateMessage.setEncPart( chngPwdEncData );
            
            ChangePasswordRequest req = new ChangePasswordRequest( apReq, privateMessage );
            
            channel = new KerberosChannel();
            channel.openConnection( host, port, timeout, !isUdp );
            
            AbstractPasswordMessage reply = sendAndReceiveChngPwdMsg( req, channel );
            
            if ( reply instanceof ChangePasswordError )
            {
                ChangePasswordError err = ( ChangePasswordError ) reply;
                
                throw new ChangePasswordException( err.getResultCode(), err.getResultString() );
            }
        }
        catch( ChangePasswordException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            LOG.warn( "failed to change the password", e );
            throw new ChangePasswordException( ChangePasswdErrorType.KRB5_KPASSWD_HARDERROR, e );
        }
        finally
        {
            if ( channel != null )
            {
                try
                {
                    channel.close();
                }
                catch( IOException e )
                {
                    LOG.warn( "Failed to close the channel", e );
                }
            }
        }
    }
    
    
    private byte[] getEncoded( Asn1Object obj )
    {
        try
        {
            ByteBuffer buf = ByteBuffer.allocate( obj.computeLength() );
            obj.encode( buf );
            
            return buf.array();
        }
        catch( Exception e )
        {
            // shouldn't happen, but if it does then log it and give  up
            LOG.error( "Failed to encode the ASN.1 object {}", obj );
            throw new RuntimeException( e );
        }
    }

    
    private KerberosTime getDefaultTill()
    {
        return new KerberosTime( System.currentTimeMillis() + ( KerberosTime.MINUTE * 60 ) );
    }
    
    
    public Set<EncryptionType> getEncryptionTypes()
    {
        return encryptionTypes;
    }


    public void setEncryptionTypes( Set<EncryptionType> encryptionTypes )
    {
        this.encryptionTypes = KerberosUtils.orderEtypesByStrength( encryptionTypes );
    }


    public long getTimeout()
    {
        return timeout;
    }


    public void setTimeout( int timeout )
    {
        this.timeout = timeout;
    }
    
    private KerberosMessage sendAndReceiveKrbMsg( KerberosMessage req ) throws Exception
    {
        ByteBuffer encodedBuf = KerberosEncoder.encode( req, channel.isUseTcp() );
        encodedBuf.flip();
        
        ByteBuffer repData = channel.sendAndReceive( encodedBuf );
        
        KerberosMessageContainer kerberosMessageContainer = new KerberosMessageContainer();
        kerberosMessageContainer.setStream( repData );
        kerberosMessageContainer.setGathering( true );
        kerberosMessageContainer.setTCP( channel.isUseTcp() );

        return ( KerberosMessage ) KerberosDecoder.decode( kerberosMessageContainer, new Asn1Decoder() );
    }
    
    
    private AbstractPasswordMessage sendAndReceiveChngPwdMsg( AbstractPasswordMessage req, KerberosChannel chngPwdChannel ) throws Exception
    {
        ByteBuffer encodedBuf = ChangePasswordEncoder.encode( req, chngPwdChannel.isUseTcp() );
        encodedBuf.flip();
        
        ByteBuffer repData = chngPwdChannel.sendAndReceive( encodedBuf );
        
        return ChangePasswordDecoder.decode( repData, chngPwdChannel.isUseTcp() );
    }
}
