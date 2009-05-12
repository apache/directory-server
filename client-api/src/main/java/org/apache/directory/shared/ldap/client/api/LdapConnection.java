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
package org.apache.directory.shared.ldap.client.api;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.naming.InvalidNameException;
import javax.naming.ldap.Control;
import javax.net.ssl.SSLContext;

import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.ldap.client.api.exception.InvalidConnectionException;
import org.apache.directory.shared.ldap.client.api.exception.LdapException;
import org.apache.directory.shared.ldap.client.api.listeners.BindListener;
import org.apache.directory.shared.ldap.client.api.listeners.IntermediateResponseListener;
import org.apache.directory.shared.ldap.client.api.listeners.SearchListener;
import org.apache.directory.shared.ldap.client.api.messages.AbandonRequest;
import org.apache.directory.shared.ldap.client.api.messages.AbandonRequestImpl;
import org.apache.directory.shared.ldap.client.api.messages.BindRequest;
import org.apache.directory.shared.ldap.client.api.messages.BindRequestImpl;
import org.apache.directory.shared.ldap.client.api.messages.BindResponse;
import org.apache.directory.shared.ldap.client.api.messages.BindResponseImpl;
import org.apache.directory.shared.ldap.client.api.messages.IntermediateResponse;
import org.apache.directory.shared.ldap.client.api.messages.IntermediateResponseImpl;
import org.apache.directory.shared.ldap.client.api.messages.LdapResult;
import org.apache.directory.shared.ldap.client.api.messages.LdapResultImpl;
import org.apache.directory.shared.ldap.client.api.messages.Referral;
import org.apache.directory.shared.ldap.client.api.messages.ReferralImpl;
import org.apache.directory.shared.ldap.client.api.messages.SearchRequest;
import org.apache.directory.shared.ldap.client.api.messages.SearchRequestImpl;
import org.apache.directory.shared.ldap.client.api.messages.SearchResponse;
import org.apache.directory.shared.ldap.client.api.messages.SearchResultDone;
import org.apache.directory.shared.ldap.client.api.messages.SearchResultDoneImpl;
import org.apache.directory.shared.ldap.client.api.messages.SearchResultEntry;
import org.apache.directory.shared.ldap.client.api.messages.SearchResultEntryImpl;
import org.apache.directory.shared.ldap.client.api.messages.SearchResultReference;
import org.apache.directory.shared.ldap.client.api.messages.SearchResultReferenceImpl;
import org.apache.directory.shared.ldap.client.api.messages.future.BindFuture;
import org.apache.directory.shared.ldap.client.api.protocol.LdapProtocolCodecFactory;
import org.apache.directory.shared.ldap.codec.ControlCodec;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapMessageCodec;
import org.apache.directory.shared.ldap.codec.LdapMessageContainer;
import org.apache.directory.shared.ldap.codec.LdapResultCodec;
import org.apache.directory.shared.ldap.codec.TwixTransformer;
import org.apache.directory.shared.ldap.codec.abandon.AbandonRequestCodec;
import org.apache.directory.shared.ldap.codec.bind.BindRequestCodec;
import org.apache.directory.shared.ldap.codec.bind.BindResponseCodec;
import org.apache.directory.shared.ldap.codec.bind.LdapAuthentication;
import org.apache.directory.shared.ldap.codec.bind.SaslCredentials;
import org.apache.directory.shared.ldap.codec.bind.SimpleAuthentication;
import org.apache.directory.shared.ldap.codec.intermediate.IntermediateResponseCodec;
import org.apache.directory.shared.ldap.codec.search.Filter;
import org.apache.directory.shared.ldap.codec.search.SearchRequestCodec;
import org.apache.directory.shared.ldap.codec.search.SearchResultDoneCodec;
import org.apache.directory.shared.ldap.codec.search.SearchResultEntryCodec;
import org.apache.directory.shared.ldap.codec.search.SearchResultReferenceCodec;
import org.apache.directory.shared.ldap.codec.unbind.UnBindRequestCodec;
import org.apache.directory.shared.ldap.cursor.Cursor;
import org.apache.directory.shared.ldap.cursor.ListCursor;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.FilterParser;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.LdapURL;
import org.apache.directory.shared.ldap.util.StringTools;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.ssl.SslFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO : implement a pool
// TODO : handle the MessageId for an abandonRequest
// TODO : return the created request, instead of an LdapResponse

/**
 * 
 *  Describe the methods to be implemented by the LdapConnection class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class LdapConnection  extends IoHandlerAdapter
{
    /** logger for reporting errors that might not be handled properly upstream */
    private static final Logger LOG = LoggerFactory.getLogger( LdapConnectionImpl.class );

    /** Define the default ports for LDAP and LDAPS */
    private static final int DEFAULT_LDAP_PORT = 389; 
    private static final int DEFAULT_LDAPS_PORT = 636;
    
    /** The default host : localhost */
    private static final String DEFAULT_LDAP_HOST = "127.0.0.1";
    
    /** The LDAP version */
    private static int LDAP_V3 = 3;
    
    private static final String LDAP_RESPONSE = "LdapReponse";
    
    /** A flag indicating if we are using SSL or not */
    private boolean useSsl = false;
    
    /** The default timeout for operation : 30 seconds */
    private static final long DEFAULT_TIMEOUT = 30000L;
    
    /** The timeout used for response we are waiting for */ 
    private long timeOut = DEFAULT_TIMEOUT;
    
    /** The selected LDAP port */
    private int ldapPort;
    
    /** the remote LDAP host */
    private String ldapHost;
    
    /** The connector open with the remote server */
    private IoConnector connector;
    
    /** A flag set to true when we used a local connector */ 
    private boolean localConnector;
    
    /** The Ldap codec */
    private IoFilter ldapProtocolFilter = new ProtocolCodecFilter(
            new LdapProtocolCodecFactory() );

    /**  
     * The created session, created when we open a connection with
     * the Ldap server.
     */
    private IoSession ldapSession;
    
    /** A Message ID which is incremented for each operation */
    private AtomicInteger messageId;
    
    /** A queue used to store the incoming add responses */
    private BlockingQueue<LdapMessageCodec> addResponseQueue;
    
    /** A queue used to store the incoming bind responses */
    private BlockingQueue<BindResponse> bindResponseQueue;
    
    /** A queue used to store the incoming compare responses */
    private BlockingQueue<LdapMessageCodec> compareResponseQueue;
    
    /** A queue used to store the incoming delete responses */
    private BlockingQueue<LdapMessageCodec> deleteResponseQueue;
    
    /** A queue used to store the incoming extended responses */
    private BlockingQueue<LdapMessageCodec> extendedResponseQueue;
    
    /** A queue used to store the incoming modify responses */
    private BlockingQueue<LdapMessageCodec> modifyResponseQueue;
    
    /** A queue used to store the incoming modifyDN responses */
    private BlockingQueue<LdapMessageCodec> modifyDNResponseQueue;
    
    /** A queue used to store the incoming search responses */
    private BlockingQueue<LdapMessageCodec> searchResponseQueue;
    
    /** A queue used to store the incoming intermediate responses */
    private BlockingQueue<LdapMessageCodec> intermediateResponseQueue;

    /** An operation mutex to guarantee the operation order */
    private Semaphore operationMutex;
    
    /** The listeners used to get results back */
    private SearchListener searchListener;
    private BindListener bindListener;
    private IntermediateResponseListener intermediateResponseListener;

    //--------------------------- Helper methods ---------------------------//
    /**
     * Check if the connection is valid : created and connected
     *
     * @return <code>true</code> if the session is valid.
     */
    private boolean isSessionValid()
    {
        return ( ldapSession != null ) && ldapSession.isConnected();
    }

    
    /**
     * Check that a session is valid, ie we can send requests to the
     * server
     *
     * @throws Exception If the session is not valid
     */
    private void checkSession() throws InvalidConnectionException
    {
        if ( !isSessionValid() )
        {
            throw new InvalidConnectionException( "Cannot connect on the server, the connection is invalid" );
        }
    }
    
    /**
     * Return the response stored into the current session.
     *
     * @return The last request response
     */
    public LdapMessageCodec getResponse()
    {
        return (LdapMessageCodec)ldapSession.getAttribute( LDAP_RESPONSE );
    }
    
    
    /**
     * Handle the lock mechanism on session. It's only a temporary lock,
     * just for the necessary time to send the request. As we are using
     * internally an asynchronous mechanism, once the data are written, 
     * we can release the lock. Thus one can send a search request
     * followed by an abandon request as soon as the search request
     * has been written, even before having received the first response. 
     */
    private void lockSession() throws LdapException
    {
        try
        {
            operationMutex.acquire();
        }
        catch ( InterruptedException ie )
        {
            String message = "Cannot acquire the session lock";
            LOG.error(  message );
            LdapException ldapException = 
                new LdapException( message );
            ldapException.initCause( ie );
            
            throw ldapException;
        }
    }

    
    /**
     * Unlock the session
     */
    private void unlockSession()
    {
        operationMutex.release();
    }

    
    /**
     * Inject the client Controls into the message
     */
    private void setControls( Map<String, Control> controls, LdapMessageCodec message )
    {
        // Add the controls
        if ( controls != null )
        {
            for ( Control control:controls.values() )
            {
                ControlCodec ctrl = new ControlCodec();
                
                ctrl.setControlType( control.getID() );
                ctrl.setControlValue( control.getEncodedValue() );
                
                message.addControl( ctrl );
            }
        }
    }
    
    
    /**
     * Get the smallest timeout from the client timeout and the connection
     * timeout.
     */
    private long getTimeout( long clientTimeOut )
    {
        if ( clientTimeOut <= 0 )
        {
            return ( timeOut <= 0 ) ? Long.MAX_VALUE : timeOut;
        }
        else if ( timeOut <= 0 )
        {
            return clientTimeOut;
        }
        else
        {
            return timeOut < clientTimeOut ? timeOut : clientTimeOut; 
        }
    }
    
    
    /**
     * Convert a BindResponseCodec to a BindResponse message
     */
    private BindResponse convert( BindResponseCodec bindResponseCodec )
    {
        BindResponse bindResponse = new BindResponseImpl();
        
        bindResponse.setMessageId( bindResponseCodec.getMessageId() );
        bindResponse.setServerSaslCreds( bindResponseCodec.getServerSaslCreds() );
        bindResponse.setLdapResult( convert( bindResponseCodec.getLdapResult() ) );

        return bindResponse;
    }


    /**
     * Convert a IntermediateResponseCodec to a IntermediateResponse message
     */
    private IntermediateResponse convert( IntermediateResponseCodec intermediateResponseCodec )
    {
        IntermediateResponse intermediateResponse = new IntermediateResponseImpl();
        
        intermediateResponse.setMessageId( intermediateResponseCodec.getMessageId() );
        intermediateResponse.setResponseName( intermediateResponseCodec.getResponseName() );
        intermediateResponse.setResponseValue( intermediateResponseCodec.getResponseValue() );

        return intermediateResponse;
    }


    /**
     * Convert a LdapResultCodec to a LdapResult message
     */
    private LdapResult convert( LdapResultCodec ldapResultCodec )
    {
        LdapResult ldapResult = new LdapResultImpl();
        
        ldapResult.setErrorMessage( ldapResultCodec.getErrorMessage() );
        ldapResult.setMatchedDn( ldapResultCodec.getMatchedDN() );
        
        // Loop on the referrals
        Referral referral = new ReferralImpl();
        
        if (ldapResultCodec.getReferrals() != null )
        {
            for ( LdapURL url:ldapResultCodec.getReferrals() )
            {
                referral.addLdapUrls( url );
            }
        }
        
        ldapResult.setReferral( referral );
        ldapResult.setResultCode( ldapResultCodec.getResultCode() );

        return ldapResult;
    }


    /**
     * Convert a SearchResultEntryCodec to a SearchResultEntry message
     */
    private SearchResultEntry convert( SearchResultEntryCodec searchEntryResultCodec )
    {
        SearchResultEntry searchResultEntry = new SearchResultEntryImpl();
        
        searchResultEntry.setMessageId( searchEntryResultCodec.getMessageId() );
        searchResultEntry.setEntry( searchEntryResultCodec.getEntry() );
        
        return searchResultEntry;
    }


    /**
     * Convert a SearchResultDoneCodec to a SearchResultDone message
     */
    private SearchResultDone convert( SearchResultDoneCodec searchResultDoneCodec )
    {
        SearchResultDone searchResultDone = new SearchResultDoneImpl();
        
        searchResultDone.setMessageId( searchResultDoneCodec.getMessageId() );
        searchResultDone.setLdapResult( convert( searchResultDoneCodec.getLdapResult() ) );
        
        return searchResultDone;
    }


    /**
     * Convert a SearchResultReferenceCodec to a SearchResultReference message
     */
    private SearchResultReference convert( SearchResultReferenceCodec searchEntryReferenceCodec )
    {
        SearchResultReference searchResultReference = new SearchResultReferenceImpl();
        
        searchResultReference.setMessageId( searchEntryReferenceCodec.getMessageId() );

        // Loop on the referrals
        Referral referral = new ReferralImpl();
        
        if (searchEntryReferenceCodec.getSearchResultReferences() != null )
        {
            for ( LdapURL url:searchEntryReferenceCodec.getSearchResultReferences() )
            {
                referral.addLdapUrls( url );
            }
        }
        
        searchResultReference.setReferral( referral );

        return searchResultReference;
    }


    //------------------------- The constructors --------------------------//
    /**
     * Create a new instance of a LdapConnection on localhost,
     * port 389.
     */
    public LdapConnection()
    {
        useSsl = false;
        ldapPort = DEFAULT_LDAP_PORT;
        ldapHost = DEFAULT_LDAP_HOST;
        messageId = new AtomicInteger();
        operationMutex = new Semaphore(1);
    }
    
    
    /**
     * Create a new instance of a LdapConnection on localhost,
     * port 389 if the SSL flag is off, or 636 otherwise.
     * 
     * @param useSsl A flag to tell if it's a SSL connection or not.
     */
    public LdapConnection( boolean useSsl )
    {
        this.useSsl = useSsl;
        ldapPort = ( useSsl ? DEFAULT_LDAPS_PORT : DEFAULT_LDAP_PORT );
        ldapHost = DEFAULT_LDAP_HOST;
        messageId = new AtomicInteger();
        operationMutex = new Semaphore(1);
    }
    
    
    /**
     * Create a new instance of a LdapConnection on a given
     * server, using the default port (389).
     *
     * @param server The server we want to be connected to
     */
    public LdapConnection( String server )
    {
        useSsl = false;
        ldapPort = DEFAULT_LDAP_PORT;
        ldapHost = server;
        messageId = new AtomicInteger();
        operationMutex = new Semaphore(1);
    }
    
    
    /**
     * Create a new instance of a LdapConnection on a given
     * server, using the default port (389) if the SSL flag 
     * is off, or 636 otherwise.
     *
     * @param server The server we want to be connected to
     * @param useSsl A flag to tell if it's a SSL connection or not.
     */
    public LdapConnection( String server, boolean useSsl )
    {
        this.useSsl = useSsl;
        ldapPort = ( useSsl ? DEFAULT_LDAPS_PORT : DEFAULT_LDAP_PORT );
        ldapHost = DEFAULT_LDAP_HOST;
        messageId = new AtomicInteger();
        operationMutex = new Semaphore(1);
    }
    
    
    /**
     * Create a new instance of a LdapConnection on a 
     * given server and a given port. We don't use ssl.
     *
     * @param server The server we want to be connected to
     * @param port The port the server is listening to
     */
    public LdapConnection( String server, int port )
    {
        useSsl = false;
        ldapPort = port;
        ldapHost = server;
        messageId = new AtomicInteger();
        operationMutex = new Semaphore(1);
    }
    
    
    /**
     * Create a new instance of a LdapConnection on a given
     * server, and a give port. We set the SSL flag accordingly
     * to the last parameter.
     *
     * @param server The server we want to be connected to
     * @param port The port the server is listening to
     * @param useSsl A flag to tell if it's a SSL connection or not.
     */
    public LdapConnection( String server, int port, boolean useSsl )
    {
        this.useSsl = useSsl;
        ldapPort = port;
        ldapHost = server;
        messageId = new AtomicInteger();
        operationMutex = new Semaphore(1);
    }

    
    //-------------------------- The methods ---------------------------//
    /**
     * Connect to the remote LDAP server.
     *
     * @return <code>true</code> if the connection is established, false otherwise
     * @throws LdapException if some error has occured
     */
    private boolean connect() throws LdapException
    {
        if ( ( ldapSession != null ) && ldapSession.isConnected() ) 
        {
            return true;
        }

        // Create the connector if needed
        if ( connector == null ) 
        {
            connector = new NioSocketConnector();
            localConnector = true;
            
            // Add the codec to the chain
            connector.getFilterChain().addLast( "ldapCodec", ldapProtocolFilter );
    
            // If we use SSL, we have to add the SslFilter to the chain
            if ( useSsl ) 
            {
                SSLContext sslContext = null; // BogusSslContextFactory.getInstance( false );
                SslFilter sslFilter = new SslFilter( sslContext );
                sslFilter.setUseClientMode(true);
                connector.getFilterChain().addLast( "sslFilter", sslFilter );
            }
    
            // Inject the protocolHandler
            connector.setHandler( this );
        }
        
        // Build the connection address
        SocketAddress address = new InetSocketAddress( ldapHost , ldapPort );
        
        // And create the connection future
        ConnectFuture connectionFuture = connector.connect( address );
        
        // Wait until it's established
        connectionFuture.awaitUninterruptibly();
        
        if ( !connectionFuture.isConnected() ) 
        {
            return false;
        }
        
        // Get back the session
        ldapSession = connectionFuture.getSession();
        
        // And inject the current Ldap container into the session
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();
        
        // Store the container into the session 
        ldapSession.setAttribute( "LDAP-Container", ldapMessageContainer );
        
        // Create the responses queues
        addResponseQueue = new LinkedBlockingQueue<LdapMessageCodec>();
        bindResponseQueue = new LinkedBlockingQueue<BindResponse>();
        compareResponseQueue = new LinkedBlockingQueue<LdapMessageCodec>();
        deleteResponseQueue = new LinkedBlockingQueue<LdapMessageCodec>();
        extendedResponseQueue = new LinkedBlockingQueue<LdapMessageCodec>();
        modifyResponseQueue = new LinkedBlockingQueue<LdapMessageCodec>();
        modifyDNResponseQueue = new LinkedBlockingQueue<LdapMessageCodec>();
        searchResponseQueue = new LinkedBlockingQueue<LdapMessageCodec>();
        intermediateResponseQueue = new LinkedBlockingQueue<LdapMessageCodec>();
        
        // And return
        return true;
    }
    
    
    /**
     * Disconnect from the remote LDAP server
     *
     * @return <code>true</code> if the connection is closed, false otherwise
     * @throws IOException if some I/O error occurs
     */
    public boolean close() throws IOException 
    {
        // Close the session
        if ( ( ldapSession != null ) && ldapSession.isConnected() )
        {
            ldapSession.close( true );
        }
        
        // clean the queues
        addResponseQueue.clear();
        bindResponseQueue.clear();
        compareResponseQueue.clear();
        deleteResponseQueue.clear();
        extendedResponseQueue.clear();
        modifyResponseQueue.clear();
        modifyDNResponseQueue.clear();
        searchResponseQueue.clear();
        intermediateResponseQueue.clear();
        
        // And close the connector if it has been created locally
        if ( localConnector ) 
        {
            // Release the connector
            connector.dispose();
        }
        
        return true;
    }
    
    //------------------------ The LDAP operations ------------------------//
    // Add operations                                                      //
    //---------------------------------------------------------------------//
    /**
     * Add an entry to the server. This is a blocking add : the user has 
     * to wait for the response until the AddResponse is returned.
     * 
     * @param entry The entry to add
     * @result AddResponse The resulting response 
     *
    public AddResponse add( Entry entry )
    {
        if ( entry == null ) 
        {
            LOG.debug( "Cannot add empty entry" );
            return null;
        }
        
        
    }
    
    
    public void add( AddRequest addRequest ) throws InvalidConnectionException
    {
        // If the session has not been establish, or is closed, we get out immediately
        checkSession();

        // Guarantee that for this session, we don't have more than one operation
        // running at the same time
        lockSession();
        
        // Create the AddRequest
        LdapDN dn = new LdapDN( name );
        
        InternalAddRequest addRequest = new InternalBindRequest();
        bindRequest.setName( dn );
        bindRequest.setVersion( LDAP_V3 );
        
        // Create the Simple authentication
        SimpleAuthentication simpleAuth = new SimpleAuthentication();
        simpleAuth.setSimple( credentials );

        bindRequest.setAuthentication( simpleAuth );
        
        // Encode the request
        LdapMessage message = new LdapMessage();
        message.setMessageId( messageId++ );
        message.setProtocolOP( bindRequest );
        
        LOG.debug( "-----------------------------------------------------------------" );
        LOG.debug( "Sending request \n{}", message );

        // Send the request to the server
        ldapSession.write( message );

        // Read the response, waiting for it if not available immediately
        LdapMessage response = bindResponseQueue.poll( timeOut, TimeUnit.MILLISECONDS );
    
        // Check that we didn't get out because of a timeout
        if ( response == null )
        {
            // We didn't received anything : this is an error
            LOG.error( "Bind failed : timeout occured" );
            unlockSession();
            throw new Exception( "TimeOut occured" );
        }
        
        operationMutex.release();
        
        // Everything is fine, return the response
        LdapResponse resp = response.getBindResponse();
        
        LOG.debug( "Bind successful : {}", resp );
        
        return resp;
    }
    */
    
    //------------------------ The LDAP operations ------------------------//
    // Abandon operations                                                  //
    //  The Abandon request just have one parameter : the MessageId to     //
    // abandon. We also have to allow controls and a client timeout.       //
    // The abandonRequest is always non-blocking, because no response is   //
    // expected                                                            //
    //---------------------------------------------------------------------//
    /**
     * A simple abandon request. 
     */
    public void abandon( int messageId ) throws LdapException
    {
        AbandonRequest abandonRequest = new AbandonRequestImpl();
        abandonRequest.setAbandonedMessageId( messageId );
        
        abandonInternal( abandonRequest );
    }

    
    /**
     * An abandon request with potentially some controls and timeout. 
     */
    public void abandon( AbandonRequest abandonRequest ) throws LdapException
    {
        abandonInternal( abandonRequest );
    }
    
    
    /**
     * Internal AbandonRequest handling
     */
    private void abandonInternal( AbandonRequest abandonRequest )
    {
        // Create the new message and update the messageId
        LdapMessageCodec message = new LdapMessageCodec();

        // Creates the messageID and stores it into the 
        // initial message and the transmitted message.
        int newId = messageId.incrementAndGet();
        abandonRequest.setMessageId( newId );
        message.setMessageId( newId );

        // Create the inner abandonRequest
        AbandonRequestCodec request = new AbandonRequestCodec();
        
        // Inject the data into the request
        request.setAbandonedMessageId( abandonRequest.getAbandonedMessageId() );
        
        // Inject the request into the message
        message.setProtocolOP( request );
        
        // Inject the controls
        setControls( abandonRequest.getControls(), message );
        
        LOG.debug( "-----------------------------------------------------------------" );
        LOG.debug( "Sending request \n{}", message );

        // Send the request to the server
        ldapSession.write( message );
    }
    
    
    //------------------------ The LDAP operations ------------------------//
    // Bind operations                                                     //
    //---------------------------------------------------------------------//
    /**
     * Anonymous Bind on a server. 
     *
     * @return The BindResponse LdapResponse 
     */
    public BindResponse bind() throws LdapException
    {
        return bind( (String)null, (byte[])null );
    }
    
    
    /**
     * An Unauthenticated Authentication Bind on a server. (cf RFC 4513,
     * par 5.1.2)
     *
     * @param name The name we use to authenticate the user. It must be a 
     * valid DN
     * @return The BindResponse LdapResponse 
     */
    public BindResponse bind( String name ) throws Exception
    {
        return bind( name, (byte[])null );
    }

    
    /**
     * Simple Bind on a server.
     *
     * @param name The name we use to authenticate the user. It must be a 
     * valid DN
     * @param credentials The password. It can't be null 
     * @return The BindResponse LdapResponse 
     */
    public BindResponse bind( String name, String credentials ) throws LdapException
    {
        return bind( name, StringTools.getBytesUtf8( credentials ) );
    }


    /**
     * Simple Bind on a server.
     *
     * @param name The name we use to authenticate the user. It must be a 
     * valid DN
     * @param credentials The password.
     * @return The BindResponse LdapResponse 
     */
    public BindResponse bind( String name, byte[] credentials )  throws LdapException
    {
        LOG.debug( "Bind request : {}", name );

        // Create the BindRequest
        BindRequest bindRequest = new BindRequestImpl();
        bindRequest.setName( name );
        bindRequest.setCredentials( credentials );
        
        BindResponse response = bindInternal( bindRequest );

        if ( response.getLdapResult().getResultCode() == ResultCodeEnum.SUCCESS )
        {
            LOG.debug( " Bind successfull" );
        }
        else
        {
            LOG.debug( " Bind failure {}", response );
        }

        return response;
    }
    
    
    /**
     * Bind to the server using a BindRequest object.
     *
     * @param bindRequest The BindRequest POJO containing all the needed
     * parameters 
     * @return A LdapResponse containing the result
     */
    public BindResponse bind( BindRequest bindRequest ) throws LdapException
    {
        return bindInternal( bindRequest );
    }
        

    /**
     * Do a non-blocking bind non-blocking
     *
     * @param bindRequest The BindRequest to send
     * @param listener The listener 
     */
    public BindFuture bind( BindRequest bindRequest, BindListener bindListener ) throws LdapException 
    {
        return bindAsyncInternal( bindRequest, bindListener );
    }
    
    
    /**
     * Do the bind blocking or non-blocking, depending on the listener value.
     *
     * @param bindRequest The BindRequest to send
     * @param listener The listener (Can be null) 
     */
    private BindResponse bindInternal( BindRequest bindRequest ) throws LdapException 
    {
        // Create the future to get the result
        BindFuture bindFuture = bindAsyncInternal( bindRequest, null );
        
        // And get the result
        try
        {
            // Read the response, waiting for it if not available immediately
            long timeout = getTimeout( bindRequest.getTimeout() );
            
            // Get the response, blocking
            BindResponse bindResponse = bindFuture.get( timeout, TimeUnit.MILLISECONDS );

            // Release the session lock
            unlockSession();
            
            // Everything is fine, return the response
            LOG.debug( "Bind successful : {}", bindResponse );
            
            return bindResponse;
        }
        catch ( TimeoutException te )
        {
            // Send an abandon request
            abandon( bindRequest.getMessageId() );
            
            // We didn't received anything : this is an error
            LOG.error( "Bind failed : timeout occured" );
            unlockSession();
            throw new LdapException( "TimeOut occured" );
        }
        catch ( Exception ie )
        {
            // Catch all other exceptions
            LOG.error( "The response queue has been emptied, no response will be find." );
            LdapException ldapException = new LdapException();
            ldapException.initCause( ie );
            unlockSession();
            
            // Send an abandon request
            abandon( bindRequest.getMessageId() );
            throw ldapException;
        }
    }

    
    /**
     * Do the bind non-blocking
     *
     * @param bindRequest The BindRequest to send
     * @param listener The listener (Can be null) 
     */
    private BindFuture bindAsyncInternal( BindRequest bindRequest, BindListener bindListener ) throws LdapException 
    {
        // First try to connect, if we aren't already connected.
        connect();
        
        // If the session has not been establish, or is closed, we get out immediately
        checkSession();

        // Guarantee that for this session, we don't have more than one operation
        // running at the same time
        lockSession();
        
        // Create the new message and update the messageId
        LdapMessageCodec bindMessage = new LdapMessageCodec();

        // Creates the messageID and stores it into the 
        // initial message and the transmitted message.
        int newId = messageId.incrementAndGet();
        bindRequest.setMessageId( newId );
        bindMessage.setMessageId( newId );
        
        // Create a new codec BindRequest object
        BindRequestCodec request =  new BindRequestCodec();
        
        // Set the version
        request.setVersion( LDAP_V3 );
        
        // Set the name
        try
        {
            LdapDN dn = new LdapDN( bindRequest.getName() );
            request.setName( dn );
        }
        catch ( InvalidNameException ine )
        {
            LOG.error( "The given dn '{}' is not valid", bindRequest.getName() );
            LdapException ldapException = new LdapException();
            ldapException.initCause( ine );
            unlockSession();
            throw ldapException;
        }
        
        // Set the credentials
        LdapAuthentication authentication = null;
        
        if ( bindRequest.isSimple() )
        {
            // Simple bind
            authentication = new SimpleAuthentication();
            ((SimpleAuthentication)authentication).setSimple( bindRequest.getCredentials() );
        }
        else
        {
            // SASL bind
            authentication = new SaslCredentials();
            ((SaslCredentials)authentication).setCredentials( bindRequest.getCredentials() );
            ((SaslCredentials)authentication).setMechanism( bindRequest.getSaslMechanism() );
        }
        
        // The authentication
        request.setAuthentication( authentication );
        
        // Stores the BindRequest into the message
        bindMessage.setProtocolOP( request );
        
        // Add the controls
        setControls( bindRequest.getControls(), bindMessage );

        LOG.debug( "-----------------------------------------------------------------" );
        LOG.debug( "Sending request \n{}", bindMessage );

        // Send the request to the server
        ldapSession.write( bindMessage );

        // Return the associated future
        return new BindFuture( bindResponseQueue );
    }
    

    //------------------------ The LDAP operations ------------------------//
    // Search operations                                                   //
    //---------------------------------------------------------------------//
    /**
     * Do a search, on the base object, using the given filter. The
     * SearchRequest parameters default to :
     * Scope : ONE
     * DerefAlias : ALWAYS
     * SizeLimit : none
     * TimeLimit : none
     * TypesOnly : false
     * Attributes : all the user's attributes.
     * This method is blocking.
     * 
     * @param baseDn The base for the search. It must be a valid
     * DN, and can't be emtpy
     * @param filterString The filter to use for this search. It can't be empty
     * @param scope The sarch scope : OBJECT, ONELEVEL or SUBTREE 
     * @return A cursor on the result. 
     */
    public Cursor<SearchResponse> search( String baseDn, String filter, SearchScope scope, 
        String... attributes ) throws LdapException
    {
        // Create a new SearchRequest object
        SearchRequest searchRequest = new SearchRequestImpl();
        
        searchRequest.setBaseDn( baseDn );
        searchRequest.setFilter( filter );
        searchRequest.setScope( scope );
        searchRequest.addAttributes( attributes );
        
        // Process the request in blocking mode
        return searchInternal( searchRequest, null );
    }
    
    
    /**
     * Do a search, on the base object, using the given filter. The
     * SearchRequest parameters default to :
     * Scope : ONE
     * DerefAlias : ALWAYS
     * SizeLimit : none
     * TimeLimit : none
     * TypesOnly : false
     * Attributes : all the user's attributes.
     * This method is blocking.
     * 
     * @param listener a SearchListener used to be informed when a result 
     * has been found, or when the search is done
     * @param baseObject The base for the search. It must be a valid
     * DN, and can't be emtpy
     * @param filter The filter to use for this search. It can't be empty
     * @return A cursor on the result. 
     */
    public void search( SearchRequest searchRequest, SearchListener listener ) throws LdapException
    {
        searchInternal( searchRequest, listener );
    }
    
    
    /**
     * {@inheritDoc}
     */
    public Cursor<SearchResponse> search( SearchRequest searchRequest ) throws LdapException
    {
        return searchInternal( searchRequest, null );
    }

    
    private Cursor<SearchResponse> searchInternal( SearchRequest searchRequest, SearchListener searchListener )
        throws LdapException
    {
        // If the session has not been establish, or is closed, we get out immediately
        checkSession();
    
        // Guarantee that for this session, we don't have more than one operation
        // running at the same time
        lockSession();
        
        // Create the new message and update the messageId
        LdapMessageCodec searchMessage = new LdapMessageCodec();
        
        // Creates the messageID and stores it into the 
        // initial message and the transmitted message.
        int newId = messageId.incrementAndGet();
        searchRequest.setMessageId( newId );
        searchMessage.setMessageId( newId );
        
        // Create a new codec SearchRequest object
        SearchRequestCodec request =  new SearchRequestCodec();
        
        // Set the name
        try
        {
            LdapDN dn = new LdapDN( searchRequest.getBaseDn() );
            request.setBaseObject( dn );
        }
        catch ( InvalidNameException ine )
        {
            LOG.error( "The given dn '{}' is not valid", searchRequest.getBaseDn() );
            LdapException ldapException = new LdapException();
            ldapException.initCause( ine );
            unlockSession();
            throw ldapException;
        }
        
        // Set the scope
        request.setScope( searchRequest.getScope() );
        
        // Set the typesOnly flag
        request.setDerefAliases( searchRequest.getDerefAliases().getValue() );
        
        // Set the timeLimit
        request.setTimeLimit( searchRequest.getTimeLimit() );
        
        // Set the sizeLimit
        request.setSizeLimit( searchRequest.getSizeLimit() );
        
        // Set the typesOnly flag
        request.setTypesOnly( searchRequest.getTypesOnly() );
    
        // Set the filter
        Filter filter = null;
        
        try
        {
            ExprNode filterNode = FilterParser.parse( searchRequest.getFilter() );
            
            filter = TwixTransformer.transformFilter( filterNode );
        }
        catch ( ParseException pe )
        {
            LOG.error( "The given filter '{}' is not valid", searchRequest.getFilter() );
            LdapException ldapException = new LdapException();
            ldapException.initCause( pe );
            unlockSession();
            throw ldapException;
        }
        
        request.setFilter( filter );
        
        // Set the attributes
        Set<String> attributes = searchRequest.getAttributes();
        
        if ( attributes != null )
        {
            for ( String attribute:attributes )
            {
                request.addAttribute( attribute );
            }
        }
        
        // Stores the SearchRequest into the message
        searchMessage.setProtocolOP( request );
        
        // Add the controls
        setControls( searchRequest.getControls(), searchMessage );
    
        LOG.debug( "-----------------------------------------------------------------" );
        LOG.debug( "Sending request \n{}", searchMessage );
    
        // Send the request to the server
        ldapSession.write( searchMessage );
    
        if ( searchListener == null )
        {
            // Read the response, waiting for it if not available immediately
            try
            {
                long timeout = getTimeout( searchRequest.getTimeout() );
                LdapMessageCodec response = null;
                List<SearchResponse> searchResponses = new ArrayList<SearchResponse>();

                // We may have more than one response, so loop on the queue
                do 
                {
                    response = searchResponseQueue.poll( timeout, TimeUnit.MILLISECONDS );

                    // Check that we didn't get out because of a timeout
                    if ( response == null )
                    {
                        // Send an abandon request
                        abandon( searchMessage.getBindRequest().getMessageId() );
                        
                        // We didn't received anything : this is an error
                        LOG.error( "Bind failed : timeout occured" );
                        unlockSession();
                        throw new LdapException( "TimeOut occured" );
                    }
                    else
                    {
                        if ( response instanceof SearchResultEntryCodec )
                        {
                            searchResponses.add( convert( (SearchResultEntryCodec)response ) );
                        }
                        else if ( response instanceof SearchResultReference )
                        {
                            searchResponses.add( convert( (SearchResultReferenceCodec)response ) );
                        }
                    }
                }
                while ( !( response instanceof SearchResultDone ) );

                // Release the session lock
                unlockSession();
                
                LOG.debug( "Search successful, {} elements found", searchResponses.size() );
                
                return new ListCursor<SearchResponse>( searchResponses );
            }
            catch ( InterruptedException ie )
            {
                LOG.error( "The response queue has been emptied, no response will be find." );
                LdapException ldapException = new LdapException();
                ldapException.initCause( ie );
                
                // Send an abandon request
                abandon( searchMessage.getBindRequest().getMessageId() );
                throw ldapException;
            }
        }
        else
        {
            // The listener will be called on a MessageReceived event,
            // no need to create a cursor
            return null;
        }
    }

    
    //------------------------ The LDAP operations ------------------------//
    // Unbind operations                                                   //
    //---------------------------------------------------------------------//
    /**
     * UnBind from a server. this is a request which expect no response.
     */
    public void unBind() throws Exception
    {
        // First try to connect, if we aren't already connected.
        connect();
        
        // If the session has not been establish, or is closed, we get out immediately
        checkSession();
        
        // Guarantee that for this session, we don't have more than one operation
        // running at the same time
        lockSession();
        
        // Create the UnbindRequest
        UnBindRequestCodec unbindRequest = new UnBindRequestCodec();
        
        // Encode the request
        LdapMessageCodec unbindMessage = new LdapMessageCodec();

        // Creates the messageID and stores it into the 
        // initial message and the transmitted message.
        int newId = messageId.incrementAndGet();
        unbindRequest.setMessageId( newId );
        unbindMessage.setMessageId( newId );

        unbindMessage.setProtocolOP( unbindRequest );
        
        LOG.debug( "-----------------------------------------------------------------" );
        LOG.debug( "Sending Unbind request \n{}", unbindMessage );
        
        // Send the request to the server
        ldapSession.write( unbindMessage );

        // Release the LdapSession
        operationMutex.release();
        
        // And get out
        LOG.debug( "Unbind successful" );
    }
    
    
    /**
     * 
     * Adds a SearchListener which can handle the results of a search request.
     *
     * @param listener an instance of SearchListener implementation.
     */
    public void addListener( SearchListener searchListener )
    {
        this.searchListener = searchListener;
    }
    
    
    /**
     * Set the connector to use.
     *
     * @param connector The connector to use
     */
    public void setConnector( IoConnector connector )
    {
        this.connector = connector;
    }


    /**
     * Set the timeOut for the responses. We wont wait longer than this 
     * value.
     *
     * @param timeOut The timeout, in milliseconds
     */
    public void setTimeOut( long timeOut )
    {
        this.timeOut = timeOut;
    }
    
    
    /**
     * Handle the incoming LDAP messages. This is where we feed the cursor for search 
     * requests, or call the listener. 
     */
    public void messageReceived( IoSession session, Object message) throws Exception 
    {
        // Feed the response and store it into the session
        LdapMessageCodec response = (LdapMessageCodec)message;

        LOG.debug( "-------> {} Message received <-------", response.getMessageTypeName() );
        
        switch ( response.getMessageType() )
        {
            case LdapConstants.ADD_RESPONSE :
                // Store the response into the responseQueue
                addResponseQueue.add( response ); 
                break;
                
            case LdapConstants.BIND_RESPONSE: 
                // Store the response into the responseQueue
                BindResponseCodec bindResponseCodec = response.getBindResponse();
                bindResponseCodec.addControl( response.getCurrentControl() );
                BindResponse bindResponse = convert( bindResponseCodec );
                
                if ( bindListener != null )
                {
                    bindListener.bindCompleted( this, bindResponse );
                }
                else
                {
                    // Store the response into the responseQueue
                    bindResponseQueue.add( bindResponse );
                }
                
                break;
                
            case LdapConstants.COMPARE_RESPONSE :
                // Store the response into the responseQueue
                compareResponseQueue.add( response ); 
                break;
                
            case LdapConstants.DEL_RESPONSE :
                // Store the response into the responseQueue
                deleteResponseQueue.add( response ); 
                break;
                
            case LdapConstants.EXTENDED_RESPONSE :
                // Store the response into the responseQueue
                extendedResponseQueue.add( response ); 
                break;
                
            case LdapConstants.INTERMEDIATE_RESPONSE:
                // Store the response into the responseQueue
                IntermediateResponseCodec intermediateResponseCodec = 
                    response.getIntermediateResponse();
                intermediateResponseCodec.addControl( response.getCurrentControl() );
                
                if ( intermediateResponseListener != null )
                {
                    intermediateResponseListener.responseReceived( this, 
                        convert( intermediateResponseCodec ) );
                }
                else
                {
                    // Store the response into the responseQueue
                    intermediateResponseQueue.add( intermediateResponseCodec );
                }
                
                break;
     
            case LdapConstants.MODIFY_RESPONSE :
                // Store the response into the responseQueue
                modifyResponseQueue.add( response ); 
                break;
                
            case LdapConstants.MODIFYDN_RESPONSE :
                // Store the response into the responseQueue
                modifyDNResponseQueue.add( response ); 
                break;
                
            case LdapConstants.SEARCH_RESULT_DONE:
                // Store the response into the responseQueue
                SearchResultDoneCodec searchResultDoneCodec = 
                    response.getSearchResultDone();
                searchResultDoneCodec.addControl( response.getCurrentControl() );
                
                if ( searchListener != null )
                {
                    searchListener.searchDone( this, convert( searchResultDoneCodec ) );
                }
                else
                {
                    searchResponseQueue.add( searchResultDoneCodec );
                }
                
                break;
            
            case LdapConstants.SEARCH_RESULT_ENTRY:
                // Store the response into the responseQueue
                SearchResultEntryCodec searchResultEntryCodec = 
                    response.getSearchResultEntry();
                searchResultEntryCodec.addControl( response.getCurrentControl() );
                
                if ( searchListener != null )
                {
                    searchListener.entryFound( this, convert( searchResultEntryCodec ) );
                }
                else
                {
                    searchResponseQueue.add( searchResultEntryCodec );
                }
                
                break;
                       
            case LdapConstants.SEARCH_RESULT_REFERENCE:
                // Store the response into the responseQueue
                SearchResultReferenceCodec searchResultReferenceCodec = 
                    response.getSearchResultReference();
                searchResultReferenceCodec.addControl( response.getCurrentControl() );

                if ( searchListener != null )
                {
                    searchListener.referralFound( this, convert( searchResultReferenceCodec ) );
                }
                else
                {
                    searchResponseQueue.add( searchResultReferenceCodec );
                }

                break;
                       
             default: LOG.error( "~~~~~~~~~~~~~~~~~~~~~ Unknown message type {} ~~~~~~~~~~~~~~~~~~~~~", response.getMessageTypeName() );
        }
    }
}
