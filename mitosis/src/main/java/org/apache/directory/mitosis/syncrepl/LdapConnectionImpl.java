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
package org.apache.directory.mitosis.syncrepl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.naming.InvalidNameException;
import javax.net.ssl.SSLContext;

import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapMessage;
import org.apache.directory.shared.ldap.codec.LdapMessageContainer;
import org.apache.directory.shared.ldap.codec.LdapResponse;
import org.apache.directory.shared.ldap.codec.TwixTransformer;
import org.apache.directory.shared.ldap.codec.bind.BindRequest;
import org.apache.directory.shared.ldap.codec.bind.SimpleAuthentication;
import org.apache.directory.shared.ldap.codec.intermediate.IntermediateResponse;
import org.apache.directory.shared.ldap.codec.search.Filter;
import org.apache.directory.shared.ldap.codec.search.SearchRequest;
import org.apache.directory.shared.ldap.codec.search.SearchResultDone;
import org.apache.directory.shared.ldap.codec.search.SearchResultEntry;
import org.apache.directory.shared.ldap.codec.search.SearchResultReference;
import org.apache.directory.shared.ldap.codec.unbind.UnBindRequest;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.FilterParser;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.message.ExtendedResponseImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.StringTools;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.ssl.SslFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * An implementation of a Ldap connection
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class LdapConnectionImpl extends IoHandlerAdapter implements LdapConnection
{
    /** logger for reporting errors that might not be handled properly upstream */
    private static final Logger LOG = LoggerFactory.getLogger( LdapConnectionImpl.class );

    /** Define the default ports for LDAP and LDAPS */
    private static final int DEFAULT_LDAP_PORT = 389; 
    private static final int DEFAULT_LDAPS_PORT = 686;
    
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
    private NioSocketConnector connector;
    
    /** The Ldap codec */
    private IoFilter ldapProtocolFilter = new ProtocolCodecFilter(
            new LdapProtocolCodecFactory() );

    /**  
     * The created session, created when we open a connection with
     * the Ldap server.
     */
    private IoSession ldapSession;
    
    /** A Message ID which is incremented for each operation */
    private int messageId;
    
    /** A queue used to store the incoming responses */
    private BlockingQueue<LdapMessage> responseQueue;
    
    /** An operation mutex to guarantee the operation order */
    private Semaphore operationMutex;
    
    /** the agent which created this connection */
    private ConsumerCallback consumer;

    //------------------------- The constructors --------------------------//
    /**
     * Create a new instance of a LdapConnection on localhost,
     * port 389.
     */
    public LdapConnectionImpl()
    {
        setSsl( false );
        ldapPort = DEFAULT_LDAP_PORT;
        ldapHost = DEFAULT_LDAP_HOST;
        messageId = 1;
        operationMutex = new Semaphore(1);
    }
    
    
    /**
     * Create a new instance of a LdapConnection on localhost,
     * port 389 if the SSL flag is off, or 686 otherwise.
     * 
     * @param useSsl A flag to tell if it's a SSL connection or not.
     */
    public LdapConnectionImpl( boolean ssl )
    {
        setSsl( ssl );
        ldapPort = ( ssl ? DEFAULT_LDAPS_PORT : DEFAULT_LDAP_PORT );
        ldapHost = DEFAULT_LDAP_HOST;
        messageId = 1;
        operationMutex = new Semaphore(1);
    }
    
    
    /**
     * Create a new instance of a LdapConnection on a given
     * server, using the default port (389)
     *
     * @param server The server we want to be connected to
     * @param port The port the server is listening to
     */
    public LdapConnectionImpl( String server )
    {
        setSsl( false );
        ldapPort = DEFAULT_LDAP_PORT;
        ldapHost = server;
        messageId = 1;
        operationMutex = new Semaphore(1);
    }
    
    
    /**
     * Create a new instance of a LdapConnection on a given
     * server, using the default port (389)
     *
     * @param server The server we want to be connected to
     * @param port The port the server is listening to
     * @param useSsl A flag to tell if it's a SSL connection or not.
     */
    public LdapConnectionImpl( String server, boolean ssl )
    {
        setSsl( ssl );
        ldapPort = ( ssl ? DEFAULT_LDAPS_PORT : DEFAULT_LDAP_PORT );
        ldapHost = DEFAULT_LDAP_HOST;
        messageId = 1;
        operationMutex = new Semaphore(1);
    }
    
    
    /**
     * Create a new instance of a LdapConnection
     *
     * @param server The server we want to be connected to
     * @param port The port the server is listening to
     */
    public LdapConnectionImpl( String server, int port )
    {
        setSsl( false );
        ldapPort = port;
        ldapHost = server;
        messageId = 1;
        operationMutex = new Semaphore(1);
    }
    
    
    /**
     * Create a new instance of a LdapConnection
     *
     * @param server The server we want to be connected to
     * @param port The port the server is listening to
     * @param useSsl A flag to tell if it's a SSL connection or not.
     */
    public LdapConnectionImpl( String server, int port, boolean ssl )
    {
        setSsl( ssl );
        ldapPort = port;
        ldapHost = server;
        messageId = 1;
        operationMutex = new Semaphore(1);
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
     * {@inheritDoc}
     */
    public boolean connect() throws IOException 
    {
        // Create the connector
        connector = new NioSocketConnector();
        
        if ( ( ldapSession != null ) && ldapSession.isConnected() ) 
        {
            throw new IllegalStateException( "Already connected. Disconnect first." );
        }

        // Add the codec to the chain
        connector.getFilterChain().addLast( "ldapCodec", ldapProtocolFilter );

        // If we use SSL, we have to add the SslFilter to the chain
        if (useSsl) 
        {
            SSLContext sslContext = null; // BogusSslContextFactory.getInstance( false );
            SslFilter sslFilter = new SslFilter( sslContext );
            sslFilter.setUseClientMode(true);
            connector.getFilterChain().addLast( "sslFilter", sslFilter );
        }

        // Inject the protocolHandler
        connector.setHandler( this );
        
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
        
        // Create the responses queue
        responseQueue = new LinkedBlockingQueue<LdapMessage>();
        
        // And return
        return true;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public boolean close() throws IOException 
    {
        // Release the connector
        connector.dispose();
        
        return true;
    }
    
    
    //------------------------ The LDAP operations ------------------------//
    // Bind operations                                                     //
    //---------------------------------------------------------------------//
    /**
     * Anonymous Bind on a server. 
     *
     * @return The BindResponse LdapResponse 
     */
    public LdapResponse bind() throws Exception
    {
        LOG.debug( " Unauthenticated Authentication bind" );
        
        LdapResponse response = bind( (String)null, (byte[])null );
        
        if (response.getLdapResult().getResultCode() == ResultCodeEnum.SUCCESS )
        {
            LOG.debug( " Unauthenticated Authentication bind successfull" );
        }
        else
        {
            LOG.debug( " Unauthenticated Authentication bind failure {}", response );
        }
        
        return response;
    }
    
    
    /**
     * An Unauthenticated Authentication Bind on a server. (cf RFC 4513,
     * par 5.1.2)
     *
     * @param name The name we use to authenticate the user. It must be a 
     * valid DN
     * @return The BindResponse LdapResponse 
     */
    public LdapResponse bind( String name ) throws Exception
    {
        LOG.debug( "Anonymous bind" );
        
        LdapResponse response = bind( name, (byte[])null );
        
        if (response.getLdapResult().getResultCode() == ResultCodeEnum.SUCCESS )
        {
            LOG.debug( "Anonymous bind successfull" );
        }
        else
        {
            LOG.debug( "Anonymous bind failure {}", response );
        }
        
        return response;
    }
    
    
    /**
     * Simple Bind on a server
     *
     * @param name The name we use to authenticate the user. It must be a 
     * valid DN
     * @param credentials The password.
     * @return The BindResponse LdapResponse 
     */
    public LdapResponse bind( String name, String credentials ) throws Exception
    {
        return bind( name, StringTools.getBytesUtf8( credentials ) );
    }
    
    
    /**
     * Simple Bind on a server
     *
     * @param name The name we use to authenticate the user. It must be a 
     * valid DN
     * @param credentials The password.
     * @return The BindResponse LdapResponse 
     */
    public LdapResponse bind( String name, byte[] credentials ) throws Exception
    {
        // If the session has not been establish, or is closed, we get out immediately
        checkSession();

        // Guarantee that for this session, we don't have more than one operation
        // running at the same time
        operationMutex.acquire();
        
        // Create the BindRequest
        LdapDN dn = new LdapDN( name );
        
        BindRequest bindRequest = new BindRequest();
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
        LdapMessage response = responseQueue.poll( timeOut, TimeUnit.MILLISECONDS );
    
        // Check that we didn't get out because of a timeout
        if ( response == null )
        {
            // We didn't received anything : this is an error
            operationMutex.release();
            throw new Exception( "TimeOut occured" );
        }
        
        operationMutex.release();
        
        // Everything is fine, return the response
        return response.getBindResponse();
    }

    
    /**
     * {@inheritDoc}
     */
    public void unBind() throws Exception
    {
        // If the session has not been establish, or is closed, we get out immediately
        checkSession();
        
        // Guarantee that for this session, we don't have more than one operation
        // running at the same time
        operationMutex.acquire();
        
        // Create the UnBindRequest
        UnBindRequest unBindRequest = new UnBindRequest();
        
        // Encode the request
        LdapMessage message = new LdapMessage();
        message.setMessageId( messageId );
        message.setProtocolOP( unBindRequest );
        
        LOG.debug( "-----------------------------------------------------------------" );
        LOG.debug( "Sending request \n{}", message );
        
        // Send the request to the server
        ldapSession.write( message );

        // We don't have to wait for a response. Reset the messageId counter to 0
        messageId = 0;
        
        // We also have to reset the response queue
        responseQueue.clear();
        
        operationMutex.release();
    }
    

    /**
     * {@inheritDoc}
     */
    public void search( String baseObject, String filterString ) throws Exception
    {
        // If the session has not been establish, or is closed, we get out immediately
        checkSession();
        
        LdapDN baseDN = null;
        Filter filter = null;
        
        // Check that the baseObject is not null or empty, 
        // and is a valid DN
        if ( StringTools.isEmpty( baseObject ) )
        {
            throw new Exception( "Cannot search on RootDSE when the scope is not BASE" );
        }
        
        try
        {
            baseDN = new LdapDN( baseObject );
        }
        catch ( InvalidNameException ine )
        {
            throw new Exception( "The baseObject is not a valid DN" );
        }
        
        // Check that the filter is valid
        try
        {
            ExprNode filterNode = FilterParser.parse( filterString );
            
            filter = TwixTransformer.transformFilter( filterNode );
        }
        catch ( ParseException pe )
        {
            throw new Exception( "The filter is invalid" );
        }
        
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setBaseObject( baseDN );
        searchRequest.setFilter( filter );
        
        // Fill the default values
        searchRequest.setSizeLimit( 0 );
        searchRequest.setTimeLimit( 0 );
        searchRequest.setDerefAliases( LdapConstants.DEREF_ALWAYS );
        searchRequest.setScope( SearchScope.ONELEVEL );
        searchRequest.setTypesOnly( false );
        searchRequest.addAttribute( SchemaConstants.ALL_USER_ATTRIBUTES );

        search( searchRequest );
    }
    

    /**
     * {@inheritDoc}
     */
    public void search( SearchRequest searchRequest ) throws Exception
    {
        // First check the session
        checkSession();
        
        // Guarantee that for this session, we don't have more than one operation
        // running at the same time
        operationMutex.acquire();
        
        // Encode the request
        LdapMessage message = new LdapMessage();
        message.setMessageId( messageId++ );
        message.setProtocolOP( searchRequest );
        message.addControl( searchRequest.getCurrentControl() );
        
        //LOG.debug( "-----------------------------------------------------------------" );
        //LOG.debug( "Sending request \n{}", message );
    
        // Loop and get all the responses
        // Send the request to the server
        ldapSession.write( message );

        operationMutex.release();
//        int i = 0;
//        
//        List<SearchResultEntry> searchResults = new ArrayList<SearchResultEntry>();
        
        // Now wait for the responses
        // Loop until we got all the responses
        
/*        do
        {
            // If we get out before the timeout, check that the response 
            // is there, and get it
            LdapMessage response = responseQueue.poll( timeOut, TimeUnit.MILLISECONDS );
            
            if ( response == null )
            {
                // No response, get out
                operationMutex.release();
                
                // We didn't received anything : this is an error
                throw new Exception( "TimeOut occured" );
            }
            
            i++;

            // Print the response
//            System.out.println( "Result[" + i + "]" + response );
            
            if( response.getMessageType() == LdapConstants.INTERMEDIATE_RESPONSE )
            {
                consumer.handleSyncInfo( response.getIntermediateResponse().getResponseValue() );
            }
            
            if ( response.getMessageType() == LdapConstants.SEARCH_RESULT_DONE )
            {
                SearchResultDone resDone = response.getSearchResultDone();
                resDone.addControl( response.getCurrentControl() );
                
                operationMutex.release();
                consumer.handleSearchDone( resDone );
                
                return;
            }
       
            if( response.getMessageType() == LdapConstants.SEARCH_RESULT_ENTRY )
            {
                SearchResultEntry sre = response.getSearchResultEntry();
                sre.addControl( response.getCurrentControl() );
                consumer.handleSearchResult( sre );
            }
            
            if( response.getMessageType() == LdapConstants.SEARCH_RESULT_REFERENCE )
            {
                SearchResultReference searchRef = response.getSearchResultReference();
                searchRef.addControl( response.getCurrentControl() );
                
                consumer.handleSearchReference( searchRef );
            }
        }
        while ( true );
*/    }

    /**
     * A helper method to set the useSsl flag
     * 
     * @param ssl The ssl value flag to set
     */
    private void setSsl( boolean ssl )
    {
        this.useSsl = ssl;

        if ( ssl )
        {
            ldapPort = DEFAULT_LDAPS_PORT;
        }
        else
        {
            ldapPort = DEFAULT_LDAP_PORT;
        }
    }
    
    
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
    private void checkSession() throws Exception
    {
        if ( !isSessionValid() )
        {
            throw new Exception( "Cannot connect on the server, the connection is invalid" );
        }
    }
    
    /**
     * Return the response stored into the current session.
     *
     * @return The last request response
     */
    public LdapMessage getResponse()
    {
        return (LdapMessage)ldapSession.getAttribute( LDAP_RESPONSE );
    }

    
    //----------------- ProtocolHandler implemented methods -----------------//
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionCreated( IoSession session ) throws Exception 
    {
        LOG.debug( "-------> New session created <-------" );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionOpened( IoSession session ) throws Exception 
    {
        LOG.debug( "-------> Session opened <-------" );
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionClosed(IoSession session) throws Exception 
    {
        LOG.debug( "-------> Session Closed <-------" );
    }

    
    /**
     * {@inheritDoc}
     */
    public void messageReceived( IoSession session, Object message) throws Exception 
    {
        // Feed the response and store it into the session
        LdapMessage response = (LdapMessage)message;

        LOG.debug( "-------> Messagessage received <-------" + response );
        
        switch( response.getMessageType() )
        {
            case LdapConstants.BIND_RESPONSE: 
                       
                       responseQueue.add( response ); // Store the response into the responseQueue
                       break;

            case LdapConstants.INTERMEDIATE_RESPONSE:
            
                       consumer.handleSyncInfo( response.getIntermediateResponse().getResponseValue() );
                       break;
            
            case LdapConstants.SEARCH_RESULT_DONE:
            
                       SearchResultDone resDone = response.getSearchResultDone();
                       resDone.addControl( response.getCurrentControl() );
                       consumer.handleSearchDone( resDone );
                       break;
            
            case LdapConstants.SEARCH_RESULT_ENTRY:
            
                       SearchResultEntry sre = response.getSearchResultEntry();
                       sre.addControl( response.getCurrentControl() );
                       consumer.handleSearchResult( sre );
                       break;
                       
            case LdapConstants.SEARCH_RESULT_REFERENCE:
            
                       SearchResultReference searchRef = response.getSearchResultReference();
                       searchRef.addControl( response.getCurrentControl() );
                       consumer.handleSearchReference( searchRef );
                       break;
                       
             default: LOG.error( "~~~~~~~~~~~~~~~~~~~~~ Unknown message type {} ~~~~~~~~~~~~~~~~~~~~~", response.getMessageTypeName() );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void addConsumer( ConsumerCallback consumer )
    {
        this.consumer = consumer;
    }
}
