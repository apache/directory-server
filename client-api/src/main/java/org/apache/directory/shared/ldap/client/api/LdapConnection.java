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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
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
import org.apache.directory.shared.ldap.client.api.listeners.AddListener;
import org.apache.directory.shared.ldap.client.api.listeners.BindListener;
import org.apache.directory.shared.ldap.client.api.listeners.DeleteListener;
import org.apache.directory.shared.ldap.client.api.listeners.IntermediateResponseListener;
import org.apache.directory.shared.ldap.client.api.listeners.ModifyDnListener;
import org.apache.directory.shared.ldap.client.api.listeners.ModifyListener;
import org.apache.directory.shared.ldap.client.api.listeners.OperationResponseListener;
import org.apache.directory.shared.ldap.client.api.listeners.SearchListener;
import org.apache.directory.shared.ldap.client.api.messages.AbandonRequest;
import org.apache.directory.shared.ldap.client.api.messages.AddResponse;
import org.apache.directory.shared.ldap.client.api.messages.BindRequest;
import org.apache.directory.shared.ldap.client.api.messages.BindResponse;
import org.apache.directory.shared.ldap.client.api.messages.DeleteRequest;
import org.apache.directory.shared.ldap.client.api.messages.DeleteResponse;
import org.apache.directory.shared.ldap.client.api.messages.IntermediateResponse;
import org.apache.directory.shared.ldap.client.api.messages.LdapResult;
import org.apache.directory.shared.ldap.client.api.messages.ModifyDnRequest;
import org.apache.directory.shared.ldap.client.api.messages.ModifyDnResponse;
import org.apache.directory.shared.ldap.client.api.messages.ModifyRequest;
import org.apache.directory.shared.ldap.client.api.messages.ModifyResponse;
import org.apache.directory.shared.ldap.client.api.messages.Referral;
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
import org.apache.directory.shared.ldap.codec.add.AddRequestCodec;
import org.apache.directory.shared.ldap.codec.add.AddResponseCodec;
import org.apache.directory.shared.ldap.codec.bind.BindRequestCodec;
import org.apache.directory.shared.ldap.codec.bind.BindResponseCodec;
import org.apache.directory.shared.ldap.codec.bind.LdapAuthentication;
import org.apache.directory.shared.ldap.codec.bind.SaslCredentials;
import org.apache.directory.shared.ldap.codec.bind.SimpleAuthentication;
import org.apache.directory.shared.ldap.codec.del.DelRequestCodec;
import org.apache.directory.shared.ldap.codec.del.DelResponseCodec;
import org.apache.directory.shared.ldap.codec.intermediate.IntermediateResponseCodec;
import org.apache.directory.shared.ldap.codec.modify.ModifyRequestCodec;
import org.apache.directory.shared.ldap.codec.modify.ModifyResponseCodec;
import org.apache.directory.shared.ldap.codec.modifyDn.ModifyDNRequestCodec;
import org.apache.directory.shared.ldap.codec.modifyDn.ModifyDNResponseCodec;
import org.apache.directory.shared.ldap.codec.search.Filter;
import org.apache.directory.shared.ldap.codec.search.SearchRequestCodec;
import org.apache.directory.shared.ldap.codec.search.SearchResultDoneCodec;
import org.apache.directory.shared.ldap.codec.search.SearchResultEntryCodec;
import org.apache.directory.shared.ldap.codec.search.SearchResultReferenceCodec;
import org.apache.directory.shared.ldap.codec.unbind.UnBindRequestCodec;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.cursor.Cursor;
import org.apache.directory.shared.ldap.cursor.ListCursor;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.FilterParser;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.message.control.CascadeControl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
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
// TODO : return the created request, instead of an LdapResponse ( partly completed )

/**
 * 
 *  Describe the methods to be implemented by the LdapConnection class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class LdapConnection  extends IoHandlerAdapter
{
    private static final String TIME_OUT_ERROR = "TimeOut occured";

    private static final String NO_RESPONSE_ERROR = "The response queue has been emptied, no response was found.";

    /** logger for reporting errors that might not be handled properly upstream */
    private static final Logger LOG = LoggerFactory.getLogger( LdapConnection.class );

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
    private BlockingQueue<AddResponse> addResponseQueue;
    
    /** A queue used to store the incoming bind responses */
    private BlockingQueue<BindResponse> bindResponseQueue;
    
    /** A queue used to store the incoming compare responses */
    private BlockingQueue<LdapMessageCodec> compareResponseQueue;
    
    /** A queue used to store the incoming delete responses */
    private BlockingQueue<DeleteResponse> deleteResponseQueue;
    
    /** A queue used to store the incoming extended responses */
    private BlockingQueue<LdapMessageCodec> extendedResponseQueue;
    
    /** A queue used to store the incoming modify responses */
    private BlockingQueue<ModifyResponse> modifyResponseQueue;
    
    /** A queue used to store the incoming modifyDN responses */
    private BlockingQueue<ModifyDnResponse> modifyDNResponseQueue;
    
    /** A queue used to store the incoming search responses */
    private BlockingQueue<SearchResponse> searchResponseQueue;
    
    /** A queue used to store the incoming intermediate responses */
    private BlockingQueue<IntermediateResponse> intermediateResponseQueue;

    /** An operation mutex to guarantee the operation order */
    private Semaphore operationMutex;

    /** a map to hold the response listeners based on the operation id */
    private Map<Integer, OperationResponseListener> listenerMap = new ConcurrentHashMap<Integer, OperationResponseListener>();
    
    /** list of controls supported by the server */
    private List<String> supportedControls;

    private Entry rootDSE;
    
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
        BindResponse bindResponse = new BindResponse();
        
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
        IntermediateResponse intermediateResponse = new IntermediateResponse();
        
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
        LdapResult ldapResult = new LdapResult();
        
        ldapResult.setErrorMessage( ldapResultCodec.getErrorMessage() );
        ldapResult.setMatchedDn( ldapResultCodec.getMatchedDN() );
        
        // Loop on the referrals
        Referral referral = new Referral();
        
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
        Referral referral = new Referral();
        
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
        addResponseQueue = new LinkedBlockingQueue<AddResponse>();
        bindResponseQueue = new LinkedBlockingQueue<BindResponse>();
        compareResponseQueue = new LinkedBlockingQueue<LdapMessageCodec>();
        deleteResponseQueue = new LinkedBlockingQueue<DeleteResponse>();
        extendedResponseQueue = new LinkedBlockingQueue<LdapMessageCodec>();
        modifyResponseQueue = new LinkedBlockingQueue<ModifyResponse>();
        modifyDNResponseQueue = new LinkedBlockingQueue<ModifyDnResponse>();
        searchResponseQueue = new LinkedBlockingQueue<SearchResponse>();
        intermediateResponseQueue = new LinkedBlockingQueue<IntermediateResponse>();
        
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
     * @result the add operation's response 
     */
    public AddResponse add( Entry entry ) throws LdapException
    {
        if ( entry == null ) 
        {
            String msg = "Cannot add empty entry";
            LOG.debug( msg );
            throw new NullPointerException( msg );
        }
        
        return add( new AddRequest( entry ), null );
    }
    
    
    /**
     * Add an entry present in the AddRequest to the server.
     * @param addRequest the request object containing an entry and controls(if any)
     * @return the add operation's response, null if non-null listener is provided
     * @throws LdapException
     */
    public AddResponse add( AddRequest addRequest, AddListener listener ) throws LdapException
    {
        checkSession();

        lockSession();
        
        AddRequestCodec addReqCodec = new AddRequestCodec();
        
        int newId = messageId.incrementAndGet();
        LdapMessageCodec message = new LdapMessageCodec();
        message.setMessageId( newId );
        addReqCodec.setMessageId( newId );

        addReqCodec.setEntry( addRequest.getEntry() );
        addReqCodec.setEntryDn( addRequest.getEntry().getDn() );
        setControls( addRequest.getControls(), addReqCodec );
        
        message.setProtocolOP( addReqCodec );
        
        // Send the request to the server
        ldapSession.write( message );
        
        AddResponse response = null;
        if( listener == null )
        {
            try
            {
                long timeout = getTimeout( addRequest.getTimeout() );
                response = addResponseQueue.poll( timeout, TimeUnit.MILLISECONDS );
                
                if ( response == null )
                {
                    LOG.error( "Add failed : timeout occured" );
                    unlockSession();
                    throw new LdapException( TIME_OUT_ERROR );
                }
            }
            catch( Exception e )
            {
                LOG.error( NO_RESPONSE_ERROR );
                unlockSession();
                throw new LdapException( e );
            }
        }
        else
        {
            listenerMap.put( newId, listener );            
        }

        unlockSession();
        
        return response;
    }

    
    /**
     * converts the AddResponseCodec to AddResponse.
     */
    private AddResponse convert( AddResponseCodec addRespCodec )
    {
        AddResponse addResponse = new AddResponse();
        
        addResponse.setMessageId( addRespCodec.getMessageId() );
        addResponse.setLdapResult( convert( addRespCodec.getLdapResult() ) );
        
        return addResponse;
    }

    
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
        AbandonRequest abandonRequest = new AbandonRequest();
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
        BindRequest bindRequest = new BindRequest();
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
            throw new LdapException( TIME_OUT_ERROR );
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
        
        if( bindListener != null )
        {
            listenerMap.put( newId, bindListener );
        }
        
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
        searchRequest.setDerefAliases( AliasDerefMode.NEVER_DEREF_ALIASES );

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
                SearchResponse response = null;
                List<SearchResponse> searchResponses = new ArrayList<SearchResponse>();

                // We may have more than one response, so loop on the queue
                do 
                {
                    response = searchResponseQueue.poll( timeout, TimeUnit.MILLISECONDS );

                    // Check that we didn't get out because of a timeout
                    if ( response == null )
                    {
                        // Send an abandon request
                        abandon( searchMessage.getSearchRequest().getMessageId() );
                        
                        // We didn't received anything : this is an error
                        LOG.error( "Bind failed : timeout occured" );
                        unlockSession();
                        throw new LdapException( TIME_OUT_ERROR );
                    }
                    else
                    {
                        if ( response instanceof SearchResultEntry )
                        {
                            searchResponses.add( ( SearchResultEntry )response );
                        }
                        else if ( response instanceof SearchResultReference )
                        {
                            searchResponses.add( ( SearchResultReference )response );
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
            listenerMap.put( newId, searchListener );
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
        
        SearchListener searchListener = null;
        
        switch ( response.getMessageType() )
        {
            case LdapConstants.ADD_RESPONSE :
                // Store the response into the responseQueue
                AddResponseCodec addRespCodec = response.getAddResponse();
                addRespCodec.addControl( response.getCurrentControl() );
                addRespCodec.setMessageId( response.getMessageId() );
                
                AddListener addListener = ( AddListener ) listenerMap.get( addRespCodec.getMessageId() );
                AddResponse addResp = convert( addRespCodec );
                if( addListener != null )
                {
                    addListener.entryAdded( this, addResp );
                }
                else
                {
                    addResponseQueue.add( addResp ); 
                }
                break;
                
            case LdapConstants.BIND_RESPONSE: 
                // Store the response into the responseQueue
                BindResponseCodec bindResponseCodec = response.getBindResponse();
                bindResponseCodec.addControl( response.getCurrentControl() );
                BindResponse bindResponse = convert( bindResponseCodec );

                // remove the listener from the listener map
                BindListener bindListener = ( BindListener ) listenerMap.remove( bindResponseCodec.getMessageId() );
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
                DelResponseCodec delRespCodec = response.getDelResponse();
                delRespCodec.setMessageId( response.getMessageId() );
                delRespCodec.addControl( response.getCurrentControl() );
                DeleteResponse delResp = convert( delRespCodec );
                DeleteListener delListener = ( DeleteListener ) listenerMap.get( delResp.getMessageId() );
                
                if( delListener != null )
                {
                    delListener.entryDeleted( this, delResp );
                }
                else
                {
                    deleteResponseQueue.add( delResp ); 
                }
                break;
                
            case LdapConstants.EXTENDED_RESPONSE :
                // Store the response into the responseQueue
                extendedResponseQueue.add( response ); 
                break;
                
            case LdapConstants.INTERMEDIATE_RESPONSE:
                // Store the response into the responseQueue
                IntermediateResponseCodec intermediateResponseCodec = 
                    response.getIntermediateResponse();
                intermediateResponseCodec.setMessageId( response.getMessageId() );
                intermediateResponseCodec.addControl( response.getCurrentControl() );
                
                IntermediateResponse intrmResp = convert( intermediateResponseCodec );                
                IntermediateResponseListener intrmListener = ( IntermediateResponseListener ) listenerMap.get( intermediateResponseCodec.getMessageId() ); 
                if ( intrmListener != null )
                {
                    intrmListener.responseReceived( this, intrmResp );
                }
                else
                {
                    // Store the response into the responseQueue
                    intermediateResponseQueue.add( intrmResp );
                }
                
                break;
     
            case LdapConstants.MODIFY_RESPONSE :
                ModifyResponseCodec modRespCodec = response.getModifyResponse();
                modRespCodec.setMessageId( response.getMessageId() );
                modRespCodec.addControl( response.getCurrentControl() );
                
                ModifyResponse modResp = convert( modRespCodec );
                ModifyListener modListener = ( ModifyListener ) listenerMap.get( modResp.getMessageId() );
                
                if( modListener != null )
                {
                    modListener.modifyCompleted( this, modResp );
                }
                else
                {
                    modifyResponseQueue.add( modResp ); 
                }
                break;
                
            case LdapConstants.MODIFYDN_RESPONSE :
                
                ModifyDNResponseCodec modDnCodec = response.getModifyDNResponse();
                modDnCodec.addControl( response.getCurrentControl() );
                modDnCodec.setMessageId( response.getMessageId() );
                ModifyDnResponse modDnResp = convert( modDnCodec );
                ModifyDnListener modDnListener = ( ModifyDnListener ) listenerMap.get( modDnCodec.getMessageId() );
                if( modDnListener != null )
                {
                    modDnListener.modifyDnCompleted( this, modDnResp );
                }
                else
                {
                    // Store the response into the responseQueue
                    modifyDNResponseQueue.add( modDnResp );
                }
                break;
                
            case LdapConstants.SEARCH_RESULT_DONE:
                // Store the response into the responseQueue
                SearchResultDoneCodec searchResultDoneCodec = 
                    response.getSearchResultDone();
                searchResultDoneCodec.setMessageId( response.getMessageId() );
                searchResultDoneCodec.addControl( response.getCurrentControl() );
                SearchResultDone srchDone = convert( searchResultDoneCodec );
                // search listener has to be removed from listener map only here
                searchListener = ( SearchListener ) listenerMap.remove( searchResultDoneCodec.getMessageId() );
                if ( searchListener != null )
                {
                    searchListener.searchDone( this, srchDone );
                }
                else
                {
                    searchResponseQueue.add( srchDone );
                }
                
                break;
            
            case LdapConstants.SEARCH_RESULT_ENTRY:
                // Store the response into the responseQueue
                SearchResultEntryCodec searchResultEntryCodec = 
                    response.getSearchResultEntry();
                searchResultEntryCodec.setMessageId( response.getMessageId() );
                searchResultEntryCodec.addControl( response.getCurrentControl() );
                
                SearchResultEntry srchEntry = convert( searchResultEntryCodec );
                searchListener = ( SearchListener ) listenerMap.get( searchResultEntryCodec.getMessageId() );
                if ( searchListener != null )
                {
                    searchListener.entryFound( this, srchEntry );
                }
                else
                {
                    searchResponseQueue.add( srchEntry );
                }
                
                break;
                       
            case LdapConstants.SEARCH_RESULT_REFERENCE:
                // Store the response into the responseQueue
                SearchResultReferenceCodec searchResultReferenceCodec = 
                    response.getSearchResultReference();
                searchResultReferenceCodec.setMessageId( response.getMessageId() );
                searchResultReferenceCodec.addControl( response.getCurrentControl() );

                SearchResultReference srchRef = convert( searchResultReferenceCodec );
                searchListener = ( SearchListener ) listenerMap.get( searchResultReferenceCodec.getMessageId() );
                if ( searchListener != null )
                {
                    searchListener.referralFound( this, srchRef );
                }
                else
                {
                    searchResponseQueue.add( srchRef );
                }

                break;
                       
             default: LOG.error( "~~~~~~~~~~~~~~~~~~~~~ Unknown message type {} ~~~~~~~~~~~~~~~~~~~~~", response.getMessageTypeName() );
        }
    }
    
    
    /**
     * 
     * modifies all the attributes present in the entry by applying the same operation.
     *
     * @param entry the entry whise attributes to be modified
     * @param modOp the operation to be applied on all the attributes of the above entry
     * @return the modify operation's response
     * @throws LdapException in case of modify operation failure or timeout happens
     */
    public ModifyResponse modify( Entry entry, ModificationOperation modOp ) throws LdapException
    {
        if( entry == null )
        {
            LOG.debug( "received a null entry for modification" );
            throw new NullPointerException( "Entry to be modified cannot be null" );
        }
        
        ModifyRequest modReq = new ModifyRequest( entry.getDn() );
        
        Iterator<EntryAttribute> itr = entry.iterator();
        while( itr.hasNext() )
        {
            modReq.addModification( itr.next(), modOp );
        }
        
        return modify( modReq, null );
    }
    
    
    /**
     * 
     * performs modify operation based on the modifications present in the ModifyRequest.
     *
     * @param modRequest the request for modify operation
     * @param listener callback listener which will be called after the operation is completed
     * @return the modify operation's response, null if non-null listener is provided
     * @throws LdapException in case of modify operation failure or timeout happens
     */
    public ModifyResponse modify( ModifyRequest modRequest, ModifyListener listener )  throws LdapException
    {
        checkSession();
    
        lockSession();
        
        LdapMessageCodec modifyMessage = new LdapMessageCodec();
        
        int newId = messageId.incrementAndGet();
        modRequest.setMessageId( newId );
        modifyMessage.setMessageId( newId );
        
        ModifyRequestCodec modReqCodec = new ModifyRequestCodec();
        modReqCodec.setModifications( modRequest.getMods() );
        modReqCodec.setObject( modRequest.getDn() );

        modifyMessage.setProtocolOP( modReqCodec );
        setControls( modRequest.getControls(), modifyMessage );
        
        ldapSession.write( modifyMessage );

        ModifyResponse response = null;
        if( listener == null )
        {
            try
            {
                long timeout = getTimeout( modRequest.getTimeout() );
                response = modifyResponseQueue.poll( timeout, TimeUnit.MILLISECONDS );
                
                if ( response == null )
                {
                    LOG.error( "Modify failed : timeout occured" );
                    unlockSession();
                    throw new LdapException( TIME_OUT_ERROR );
                }
            }
            catch( Exception e )
            {
                LOG.error( NO_RESPONSE_ERROR );
                unlockSession();
                throw new LdapException( e );
            }
        }
        else
        {
            listenerMap.put( newId, listener );
        }
        
        unlockSession();
        
        return response;
    }
    
    
    /**
     * converts the ModifyResponseCodec to ModifyResponse.
     */
    private ModifyResponse convert( ModifyResponseCodec modRespCodec )
    {
        ModifyResponse modResponse = new ModifyResponse();
        
        modResponse.setMessageId( modRespCodec.getMessageId() );
        modResponse.setLdapResult( convert( modRespCodec.getLdapResult() ) );

        return modResponse;
    }


    /**
     * renames the given entryDn with new Rdn and deletes the old RDN.
     * @see #rename(String, String, boolean)
     */
    public ModifyDnResponse rename( String entryDn, String newRdn ) throws LdapException
    {
        return rename( entryDn, newRdn, true );
    }

    
    /**
     * renames the given entryDn with new RDN and deletes the old RDN.
     * @see #rename(LdapDN, Rdn, boolean)
     */
    public ModifyDnResponse rename( LdapDN entryDn, Rdn newRdn ) throws LdapException
    {
        return rename( entryDn, newRdn, true );
    }
    
    
    /**
     * @see #rename(LdapDN, Rdn, boolean)
     */
    public ModifyDnResponse rename( String entryDn, String newRdn, boolean deleteOldRdn ) throws LdapException
    {
        try
        {
            return rename( new LdapDN( entryDn ), new Rdn( newRdn ), deleteOldRdn );
        }
        catch( InvalidNameException e )
        {
            LOG.error( e.getMessage(), e );
            throw new LdapException( e.getMessage(), e );
        }
    }
    
    
    /**
     * 
     * renames the given entryDn with new RDN and deletes the old Rdn if 
     * deleteOldRdn is set to true.
     *
     * @param entryDn the target DN
     * @param newRdn new Rdn for the target DN
     * @param deleteOldRdn flag to indicate whether to delete the old Rdn
     * @return modifyDn operations response
     * @throws LdapException
     */
    public ModifyDnResponse rename( LdapDN entryDn, Rdn newRdn, boolean deleteOldRdn ) throws LdapException
    {
        ModifyDnRequest modDnRequest = new ModifyDnRequest();
        modDnRequest.setEntryDn( entryDn );
        modDnRequest.setNewRdn( newRdn );
        modDnRequest.setDeleteOldRdn( deleteOldRdn );
        
        return modifyDn( modDnRequest, null );
    }
    

    /**
     * @see #move(LdapDN, LdapDN) 
     */
    public ModifyDnResponse move( String entryDn, String newSuperiorDn ) throws LdapException
    {
        try
        {
            return move( new LdapDN( entryDn ), new LdapDN( newSuperiorDn ) );
        }
        catch( InvalidNameException e )
        {
            LOG.error( e.getMessage(), e );
            throw new LdapException( e.getMessage(), e );
        }
    }
    

    /**
     * moves the given entry DN under the new superior DN
     *
     * @param entryDn the DN of the target entry
     * @param newSuperiorDn DN of the new parent/superior
     * @return modifyDn operations response
     * @throws LdapException
     */
    public ModifyDnResponse move( LdapDN entryDn, LdapDN newSuperiorDn ) throws LdapException
    {
        ModifyDnRequest modDnRequest = new ModifyDnRequest();
        modDnRequest.setEntryDn( entryDn );
        modDnRequest.setNewSuperior( newSuperiorDn );
        
        //TODO not setting the below value is resulting in error
        modDnRequest.setNewRdn( entryDn.getRdn() );
        
        return modifyDn( modDnRequest, null );
    }

    
    /**
     * 
     * performs the modifyDn operation based on the given ModifyDnRequest.
     *
     * @param modDnRequest the request
     * @param listener callback listener which will be called after the operation is completed
     * @return modifyDn operations response, null if non-null listener is provided
     * @throws LdapException
     */
    public ModifyDnResponse modifyDn( ModifyDnRequest modDnRequest, ModifyDnListener listener ) throws LdapException
    {
        checkSession();
    
        lockSession();
        
        LdapMessageCodec modifyDnMessage = new LdapMessageCodec();
        
        int newId = messageId.incrementAndGet();
        modDnRequest.setMessageId( newId );
        modifyDnMessage.setMessageId( newId );

        ModifyDNRequestCodec modDnCodec = new ModifyDNRequestCodec();
        modDnCodec.setEntry( modDnRequest.getEntryDn() );
        modDnCodec.setNewRDN( modDnRequest.getNewRdn() );
        modDnCodec.setDeleteOldRDN( modDnRequest.isDeleteOldRdn() );
        modDnCodec.setNewSuperior( modDnRequest.getNewSuperior() );
        
        modifyDnMessage.setProtocolOP( modDnCodec );
        setControls( modDnRequest.getControls(), modifyDnMessage );
        
        ldapSession.write( modifyDnMessage );
        
        if( listener == null )
        {
            ModifyDnResponse response = null;
            try
            {
                long timeout = getTimeout( modDnRequest.getTimeout() );
                response = modifyDNResponseQueue.poll( timeout, TimeUnit.MILLISECONDS );
                
                if ( response == null )
                {
                    LOG.error( "Modifying DN failed : timeout occured" );
                    unlockSession();
                    throw new LdapException( TIME_OUT_ERROR );
                }
            }
            catch( Exception e )
            {
                LOG.error( NO_RESPONSE_ERROR );
                unlockSession();
                LdapException ldapException = new LdapException();
                ldapException.initCause( e );
                throw ldapException;
            }
            
            unlockSession();
            
            return response;
        }
        else
        {
            listenerMap.put( newId, listener );
            return null;
        }
    }
    
    
    /**
     * converts the ModifyDnResponseCodec to ModifyResponse.
     */
    private ModifyDnResponse convert( ModifyDNResponseCodec modDnRespCodec )
    {
        ModifyDnResponse modDnResponse = new ModifyDnResponse();
        
        modDnResponse.setMessageId( modDnRespCodec.getMessageId() );
        modDnResponse.setLdapResult( convert( modDnRespCodec.getLdapResult() ) );
        
        return modDnResponse;
    }


    /**
     * @see #delete(LdapDN)
     */
    public DeleteResponse delete( String dn )  throws LdapException
    {
        try
        {
            return delete( new LdapDN( dn ) );
        }
        catch( InvalidNameException e )
        {
            LOG.error( e.getMessage(), e );
            throw new LdapException( e.getMessage(), e );
        }
    }

    
    /**
     * deletes the entry with the given DN
     *  
     * @param dn the target entry's DN
     * @throws LdapException
     */
    public DeleteResponse delete( LdapDN dn )  throws LdapException
    {
        return delete( dn, false ); 
    }


    /**
     * deletes the entry with the given DN
     *  
     * @param dn the target entry's DN
     * @param listener  the delete operation response listener
     * @return operation's response, null if a non-null listener value is provided
     * @throws LdapException
     */
    public DeleteResponse delete( LdapDN dn, DeleteListener listener )  throws LdapException
    {
        return delete( new DeleteRequest( dn ), listener ); 
    }
    
    
    /**
     * deletes the entry with the given DN and all its children
     * 
     * @param dn the target entry DN
     * @param deleteAllChildren flag to indicate whether to delete the children
     * @return delete operation's response
     * @throws LdapException
     */
    public DeleteResponse delete( LdapDN dn, boolean deleteAllChildren )  throws LdapException
    {
        DeleteRequest delRequest = new DeleteRequest( dn );
        
        if( deleteAllChildren )
        {
            if( isControlSupported( CascadeControl.CONTROL_OID ) )
            {
                delRequest.add( new CascadeControl() );
            }
            else
            {
                return deleteChildren( dn, new HashMap() );
            }
        }
            
        return delete( delRequest, null );
    }

    
    /**
     * removes all child entries present under the given DN and finally the DN itself
     * 
     * Working:
     *          This is a recursive function which maintains a Map<LdapDN,Cursor>.
     *          The way the cascade delete works is by checking for children for a 
     *          given DN(i.e opening a search cursor) and if the cursor is empty
     *          then delete the DN else for each entry's DN present in cursor call
     *          deleteChildren() with the DN and the reference to the map.
     *          
     *          The reason for opening a search cursor is based on an assumption
     *          that an entry *might* contain children, consider the below DIT fragment
     *          
     *          parent
     *          /     \
     *        child1   child2
     *                 /     \
     *               grand21  grand22
     *               
     *           The below method works better in the case where the tree depth is >1 
     *          
     *  //FIXME provide another method for optimizing delete operation for a tree with depth <=1
     *          
     * @param dn the DN which will be removed after removing its children
     * @param map a map to hold the Cursor related to a DN 
     * @throws LdapException
     */
    private DeleteResponse deleteChildren( LdapDN dn, Map<LdapDN, Cursor<SearchResponse>> cursorMap ) throws LdapException
    {
        LOG.debug( "searching for {}", dn.getUpName() );
        DeleteResponse delResponse = null;
        Cursor<SearchResponse> cursor = null;
        try
        {
            if( cursorMap == null )
            {
                cursorMap = new HashMap<LdapDN, Cursor<SearchResponse>>();
            }

            cursor = cursorMap.get( dn ); 
            if( cursor == null )
            {
                cursor = search( dn.getUpName(), "(objectClass=*)", SearchScope.ONELEVEL, null ); 
                LOG.debug( "putting curosr for {}", dn.getUpName() );
                cursorMap.put( dn, cursor );
            }
            
            if( ! cursor.next() ) // if this is a leaf entry's DN
            {
                LOG.debug( "deleting {}", dn.getUpName() );
                cursorMap.remove( dn );
                cursor.close();
                delResponse = delete( new DeleteRequest( dn ), null );
            }
            else
            {
                do
                {
                    SearchResponse searchResp = ( SearchResponse ) cursor.get();
                    if( searchResp instanceof SearchResultEntry )
                    {
                        SearchResultEntry searchResult = ( SearchResultEntry ) searchResp;
                        deleteChildren( searchResult.getEntry().getDn(), cursorMap );
                    }
                }
                while( cursor.next() );
                
                cursorMap.remove( dn );
                cursor.close();
                LOG.debug( "deleting {}", dn.getUpName() );
                delResponse = delete( new DeleteRequest( dn ), null );
            }
        }
        catch( Exception e )
        {
            String msg = "Failed to delete child entries under the DN " + dn.getUpName();
            LOG.error( msg, e );
            throw new LdapException( msg, e );
        }
        
        return delResponse;
    }
    
    
    /**
     * performs a delete operation based on the delete request object.
     *  
     * @param delRequest the delete operation's request
     * @param listener the delete operation response listener
     * @return delete operation's response, null if a non-null listener value is provided
     * @throws LdapException
     */
    public DeleteResponse delete( DeleteRequest delRequest, DeleteListener listener )  throws LdapException
    {
        checkSession();
    
        lockSession();
        
        LdapMessageCodec deleteMessage = new LdapMessageCodec();
        
        int newId = messageId.incrementAndGet();
        delRequest.setMessageId( newId );
        deleteMessage.setMessageId( newId );

        DelRequestCodec delCodec = new DelRequestCodec();
        delCodec.setEntry( delRequest.getTargetDn() );

        deleteMessage.setProtocolOP( delCodec );
        setControls( delRequest.getControls(), deleteMessage );
        
        ldapSession.write( deleteMessage );
        
        DeleteResponse response = null;
        if( listener == null )
        {
            try
            {
                long timeout = getTimeout( delRequest.getTimeout() );
                response = deleteResponseQueue.poll( timeout, TimeUnit.MILLISECONDS );
                
                if ( response == null )
                {
                    LOG.error( "Delete DN failed : timeout occured" );
                    unlockSession();
                    throw new LdapException( TIME_OUT_ERROR );
                }
            }
            catch( Exception e )
            {
                LOG.error( NO_RESPONSE_ERROR );
                unlockSession();
                LdapException ldapException = new LdapException();
                ldapException.initCause( e );
                throw ldapException;
            }
            
        }
        else
        {
            listenerMap.put( newId, listener );
        }

        unlockSession();
        
        return response;
    }

    
    /**
     * converts the DeleteResponseCodec to DeleteResponse object.
     */
    private DeleteResponse convert( DelResponseCodec delRespCodec )
    {
        DeleteResponse response = new DeleteResponse();
        
        response.setMessageId( delRespCodec.getMessageId() );
        response.setLdapResult( convert( delRespCodec.getLdapResult() ) );
        
        return response;
    }
    
    
    /**
     * checks if a control with the given OID is supported
     * 
     * @param controlOID the OID of the control
     * @return true if the control is supported, false otherwise
     */
    public boolean isControlSupported( String controlOID ) throws LdapException
    {
        return getSupportedConrols().contains( controlOID );
    }
    
    
    /**
     * get the Conrols supported by server.
     *
     * @return a list of control OIDs supported by server
     * @throws LdapException
     */
    public List<String> getSupportedConrols() throws LdapException
    {
        if( supportedControls != null )
        {
            return supportedControls;
        }
        
        if( rootDSE == null )
        {
            fetchRootDSE();
        }

        supportedControls = new ArrayList<String>();

        EntryAttribute attr = rootDSE.get( SchemaConstants.SUPPORTED_CONTROL_AT );
        Iterator<Value<?>> itr = attr.getAll();
        
        while( itr.hasNext() )
        {
            supportedControls.add( ( String ) itr.next().get() );
        }

        return supportedControls;
    }
    

    /**
     * fetches the rootDSE from the server
     * @throws LdapException
     */
    private void fetchRootDSE() throws LdapException
    {
        Cursor<SearchResponse> cursor = null;
        try
        {
            cursor = search( "", "(objectClass=*)", SearchScope.OBJECT, "*", "+" );
            cursor.next();
            SearchResultEntryImpl searchRes = ( SearchResultEntryImpl ) cursor.get();
            
            rootDSE = searchRes.getEntry();
        }
        catch( Exception e )
        {
            String msg = "Failed to fetch the RootDSE";
            LOG.error( msg );
            throw new LdapException( msg, e );
        }
        finally
        {
            if( cursor != null )
            {
                try
                {
                    cursor.close();
                }
                catch( Exception e )
                {
                    LOG.error( "Failed to close open cursor", e );
                }
            }
        }
    }
}
