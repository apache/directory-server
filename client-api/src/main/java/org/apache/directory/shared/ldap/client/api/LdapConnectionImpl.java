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

import java.text.ParseException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

import javax.naming.InvalidNameException;

import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapMessageCodec;
import org.apache.directory.shared.ldap.codec.TwixTransformer;
import org.apache.directory.shared.ldap.codec.search.Filter;
import org.apache.directory.shared.ldap.codec.search.SearchRequestCodec;
import org.apache.directory.shared.ldap.codec.search.SearchResultDoneCodec;
import org.apache.directory.shared.ldap.codec.search.SearchResultEntryCodec;
import org.apache.directory.shared.ldap.codec.search.SearchResultReferenceCodec;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.FilterParser;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.StringTools;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * An implementation of a Ldap connection
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class LdapConnectionImpl extends IoHandlerAdapter
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
    
    /** The Ldap codec */
    private IoFilter ldapProtocolFilter = new ProtocolCodecFilter(
            null );

    /**  
     * The created session, created when we open a connection with
     * the Ldap server.
     */
    private IoSession ldapSession;
    
    /** A Message ID which is incremented for each operation */
    private int messageId;
    
    /** A queue used to store the incoming responses */
    private BlockingQueue<LdapMessageCodec> responseQueue;
    
    /** An operation mutex to guarantee the operation order */
    private Semaphore operationMutex;
    
    /** the agent which created this connection */
    //private ConsumerCallback consumer;

    
    
    /**
     * {@inheritDoc}
     */
    public void search( String baseObject, String filterString ) throws Exception
    {
        // If the session has not been establish, or is closed, we get out immediately
        //checkSession();
        
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
        
        SearchRequestCodec searchRequest = new SearchRequestCodec();
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
    private void search( SearchRequestCodec searchRequest ) throws Exception
    {
        // First check the session
        //checkSession();
        
        // Guarantee that for this session, we don't have more than one operation
        // running at the same time
        operationMutex.acquire();
        
        // Encode the request
        LdapMessageCodec message = new LdapMessageCodec();
        message.setMessageId( messageId++ );
        message.setProtocolOP( searchRequest );
        message.addControl( searchRequest.getCurrentControl() );
        
        //LOG.debug( "-----------------------------------------------------------------" );
        //LOG.debug( "Sending request \n{}", message );
    
        // Loop and get all the responses
        // Send the request to the server
        ldapSession.write( message );

        operationMutex.release();
        
        // The search request has been sent, we now have to wait for the result to come back.
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
        //consumer.handleSessionClosed();
    }

    
    /**
     * {@inheritDoc}
     */
    public void messageReceived( IoSession session, Object message) throws Exception 
    {
        // Feed the response and store it into the session
        LdapMessageCodec response = (LdapMessageCodec)message;

        LOG.debug( "-------> {} Message received <-------", response.getMessageTypeName() );
        
        switch( response.getMessageType() )
        {
            case LdapConstants.BIND_RESPONSE: 
                       
                       responseQueue.add( response ); // Store the response into the responseQueue
                       break;

            case LdapConstants.INTERMEDIATE_RESPONSE:
            
                       //consumer.handleSyncInfo( response.getIntermediateResponse().getResponseValue() );
                       break;
            
            case LdapConstants.SEARCH_RESULT_DONE:
            
                       SearchResultDoneCodec resDone = response.getSearchResultDone();
                       resDone.addControl( response.getCurrentControl() );
                       //consumer.handleSearchDone( resDone );
                       break;
            
            case LdapConstants.SEARCH_RESULT_ENTRY:
            
                       SearchResultEntryCodec sre = response.getSearchResultEntry();
                       sre.addControl( response.getCurrentControl() );
                       //consumer.handleSearchResult( sre );
                       break;
                       
            case LdapConstants.SEARCH_RESULT_REFERENCE:
            
                       SearchResultReferenceCodec searchRef = response.getSearchResultReference();
                       searchRef.addControl( response.getCurrentControl() );
                       //consumer.handleSearchReference( searchRef );
                       break;
                       
             default: LOG.error( "~~~~~~~~~~~~~~~~~~~~~ Unknown message type {} ~~~~~~~~~~~~~~~~~~~~~", response.getMessageTypeName() );
        }
    }
}
