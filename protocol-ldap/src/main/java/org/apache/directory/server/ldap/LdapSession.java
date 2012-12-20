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
package org.apache.directory.server.ldap;


import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.message.AbandonableRequest;
import org.apache.directory.api.ldap.model.message.BindStatus;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.SearchRequestContainer;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.handlers.controls.PagedSearchContext;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An object representing an LdapSession. Any connection established with the
 * LDAP server forms a session.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapSession
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( LdapSession.class );

    /** A speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The list of requests we can abandon */
    private static final AbandonableRequest[] EMPTY_ABANDONABLES = new AbandonableRequest[0];

    /** A lock to protect the abandonableRequests against concurrent access */
    private final String outstandingLock;

    /**
     * The associated IoSession. Usually, a LdapSession is established
     * at the user request, which means we have a IoSession.
     */
    private final IoSession ioSession;

    /** The CoreSession */
    private CoreSession coreSession;

    /** A reference on the LdapServer instance */
    private LdapServer ldapServer;

    /** A map of all the running requests */
    private Map<Integer, AbandonableRequest> outstandingRequests;

    /** A map of all the pending search requests */
    private Map<Integer, SearchRequestContainer> searchRequests;

    /** The current Bind status */
    private BindStatus bindStatus;

    /** The current mechanism used to authenticate the user */
    private String currentMechanism;

    /**
     * A Map containing Objects used during the SASL negotiation
     */
    private Map<String, Object> saslProperties;

    /** A map containing all the paged search context */
    private Map<Integer, PagedSearchContext> pagedSearchContexts;


    /**
     * Creates a new instance of LdapSession associated with the underlying
     * connection (MINA IoSession) to the server.
     *
     * @param ioSession the MINA session associated this LdapSession
     */
    public LdapSession( IoSession ioSession )
    {
        this.ioSession = ioSession;
        outstandingLock = "OutstandingRequestLock: " + ioSession.toString();
        outstandingRequests = new ConcurrentHashMap<Integer, AbandonableRequest>();
        searchRequests = new ConcurrentHashMap<Integer, SearchRequestContainer>();
        bindStatus = BindStatus.ANONYMOUS;
        saslProperties = new HashMap<String, Object>();
        pagedSearchContexts = new ConcurrentHashMap<Integer, PagedSearchContext>();
    }


    /**
     * Check if the session is authenticated. There are two conditions for
     * a session to be authenticated :<br>
     * - the coreSession must not be null<br>
     * - and the state should be Authenticated.
     * 
     * @return <code>true</code> if the session is not anonymous
     */
    public boolean isAuthenticated()
    {
        return ( coreSession != null ) && bindStatus == BindStatus.AUTHENTICATED;
    }


    /**
     * Check if the session is authenticated. There are two conditions for
     * a session to be authenticated :<br>
     * - it has to exist<br>
     * - and the session should not be anonymous.
     * 
     * @return <code>true</code> if the session is not anonymous
     */
    public boolean isAnonymous()
    {
        return bindStatus == BindStatus.ANONYMOUS;
    }


    /**
     * Check if the session is processing a BindRequest, either Simple
     * or SASL
     * 
     * @return <code>true</code> if the session is in AuthPending state
     */
    public boolean isAuthPending()
    {
        return ( bindStatus == BindStatus.SIMPLE_AUTH_PENDING ) ||
            ( bindStatus == BindStatus.SASL_AUTH_PENDING );
    }


    /**
     * Check if the session is processing a Simple BindRequest
     * 
     * @return <code>true</code> if the session is in AuthPending state
     */
    public boolean isSimpleAuthPending()
    {
        return ( bindStatus == BindStatus.SIMPLE_AUTH_PENDING );
    }


    /**
     * Check if the session is processing a SASL BindRequest
     * 
     * @return <code>true</code> if the session is in AuthPending state
     */
    public boolean isSaslAuthPending()
    {
        return ( bindStatus == BindStatus.SASL_AUTH_PENDING );
    }


    /**
     * Gets the MINA IoSession associated with this LdapSession.
     *
     * @return the MINA IoSession
     */
    public IoSession getIoSession()
    {
        return ioSession;
    }


    /**
     * Gets the logical core DirectoryService session associated with this
     * LdapSession.
     *
     * @return the logical core DirectoryService session
     */
    public CoreSession getCoreSession()
    {
        return coreSession;
    }


    /**
     * Sets the logical core DirectoryService session.
     * 
     * @param coreSession the logical core DirectoryService session
     */
    public void setCoreSession( CoreSession coreSession )
    {
        this.coreSession = coreSession;
    }


    /**
     * Abandons all outstanding requests associated with this session.
     */
    public void abandonAllOutstandingRequests()
    {
        synchronized ( outstandingLock )
        {
            AbandonableRequest[] abandonables = outstandingRequests.values().toArray( EMPTY_ABANDONABLES );

            for ( AbandonableRequest abandonable : abandonables )
            {
                abandonOutstandingRequest( abandonable.getMessageId() );
            }
        }
    }


    /**
     * Abandons a specific request by messageId.
     * 
     * @param messageId The request ID to abandon
     */
    public AbandonableRequest abandonOutstandingRequest( int messageId )
    {
        AbandonableRequest request = null;

        synchronized ( outstandingLock )
        {
            request = outstandingRequests.remove( messageId );
        }
        
        // Remove the PagedSearch cursors now
        try
        {
            closeAllPagedSearches();
        }
        catch ( Exception e )
        {
            LOG.error( I18n.err( I18n.ERR_172, e.getLocalizedMessage() ) );
        }

        if ( request == null )
        {
            LOG.warn( "AbandonableRequest with messageId {} not found in outstandingRequests.", messageId );
            return null;
        }

        if ( request.isAbandoned() )
        {
            LOG.info( "AbandonableRequest with messageId {} has already been abandoned", messageId );
            return request;
        }

        request.abandon();

        if ( IS_DEBUG )
        {
            LOG.debug( "AbandonRequest on AbandonableRequest wth messageId {} was successful.", messageId );
        }

        return request;
    }


    /**
     * Registers an outstanding request which can be abandoned later.
     *
     * @param request an outstanding request that can be abandoned
     */
    public void registerOutstandingRequest( AbandonableRequest request )
    {
        synchronized ( outstandingLock )
        {
            outstandingRequests.put( request.getMessageId(), request );
        }
    }


    /**
     * Unregisters an outstanding request.
     *
     * @param request the request to unregister
     */
    public void unregisterOutstandingRequest( AbandonableRequest request )
    {
        synchronized ( outstandingLock )
        {
            outstandingRequests.remove( request.getMessageId() );
        }
    }
    
    
    /**
     * @return A list of all the abandonable requests for this session.
     */
    public Map<Integer, AbandonableRequest> getOutstandingRequests()
    {
        synchronized ( outstandingLock )
        {
            return Collections.unmodifiableMap( outstandingRequests );
        }
    }

    
    /**
     * Registers a new searchRequest
     *
     * @param searchRequest a new searchRequest
     */
    public void registerSearchRequest( SearchRequest searchRequest, Cursor<Entry> cursor )
    {
        synchronized ( outstandingLock )
        {
            SearchRequestContainer searchRequestContainer = new SearchRequestContainer( searchRequest, cursor );
            searchRequests.put( searchRequest.getMessageId(), searchRequestContainer );
        }
    }


    /**
     * Unregisters a completed search request.
     *
     * @param searchRequest the searchRequest to unregister
     */
    public void unregisterSearchRequest( SearchRequest searchRequest )
    {
        searchRequests.remove( searchRequest.getMessageId() );
    }


    /**
     * Find the searchRequestContainer associated with a MessageID
     *
     * @param messageId the SearchRequestContainer MessageID we are looking for
     */
    public SearchRequestContainer getSearchRequest( int messageId )
    {
        return searchRequests.get( messageId );
    }


    /**
     * @return the current bind status for this session
     */
    public BindStatus getBindStatus()
    {
        return bindStatus;
    }


    /**
     * Set the current BindStatus to Simple authentication pending
     */
    public void setSimpleAuthPending()
    {
        bindStatus = BindStatus.SIMPLE_AUTH_PENDING;
    }


    /**
     * Set the current BindStatus to SASL authentication pending
     */
    public void setSaslAuthPending()
    {
        bindStatus = BindStatus.SASL_AUTH_PENDING;
    }


    /**
     * Set the current BindStatus to Anonymous
     */
    public void setAnonymous()
    {
        bindStatus = BindStatus.ANONYMOUS;
    }


    /**
     * Set the current BindStatus to authenticated
     */
    public void setAuthenticated()
    {
        bindStatus = BindStatus.AUTHENTICATED;
    }


    /**
     * Get the mechanism selected by a user during a SASL Bind negotiation.
     * 
     * @return The used mechanism, if any
     */
    public String getCurrentMechanism()
    {
        return currentMechanism;
    }


    /**
     * Add a Sasl property and value
     * 
     * @param property the property to add
     * @param value the value for this property
     */
    public void putSaslProperty( String property, Object value )
    {
        saslProperties.put( property, value );
    }


    /**
     * Get a Sasl property's value
     * 
     * @param property the property to get
     * @return the associated value, or null if we don't have such a property
     */
    public Object getSaslProperty( String property )
    {
        return saslProperties.get( property );
    }


    /**
     * Clear all the Sasl values stored into the Map
     */
    public void clearSaslProperties()
    {
        saslProperties.clear();
    }


    /**
     * Remove a property from the SaslProperty map
     *
     * @param property the property to remove
     */
    public void removeSaslProperty( String property )
    {
        saslProperties.remove( property );
    }


    /**
     *  @return The LdapServer reference
     */
    public LdapServer getLdapServer()
    {
        return ldapServer;
    }


    /**
     * Store a reference on the LdapServer intance
     *
     * @param ldapServer the LdapServer instance
     */
    public void setLdapServer( LdapServer ldapServer )
    {
        this.ldapServer = ldapServer;
    }


    /**
     * Add a new Paged Search context into the stored context. If some
     * context with the same id already exists, it will be closed and
     * removed.
     * 
     * @param context The context to add
     */
    public void addPagedSearchContext( PagedSearchContext context ) throws Exception
    {
        PagedSearchContext oldContext = pagedSearchContexts.put( context.getCookieValue(), context );

        if ( oldContext != null )
        {
            // ??? Very unlikely to happen ...
            EntryFilteringCursor cursor = oldContext.getCursor();

            if ( cursor != null )
            {
                try
                {
                    cursor.close();
                }
                catch ( Exception e )
                {
                    LOG.error( I18n.err( I18n.ERR_172, e.getLocalizedMessage() ) );
                }
            }
        }
    }


    /**
     * Remove a Paged Search context from the map storing all of them.
     * 
     * @param contextId The context ID to remove
     * @return The removed context if any found
     */
    public PagedSearchContext removePagedSearchContext( int contextId )
    {
        return pagedSearchContexts.remove( contextId );
    }

    
    /**
     * Close all the pending cursors for all the pending PagedSearches
     * 
     * @throws Exception If we've got an exception.
     */
    public void closeAllPagedSearches() throws Exception
    {
        for ( int contextId : pagedSearchContexts.keySet() )
        {
            PagedSearchContext context = pagedSearchContexts.get( contextId );
            
            EntryFilteringCursor cursor = context.getCursor();
            
            if ( cursor != null )
            {
                cursor.close();
            }
        }
    }

    /**
     * Get paged search context associated with an ID
     * @param contextId The id for teh context we want to get
     * @return The associated context, if any
     */
    public PagedSearchContext getPagedSearchContext( int contextId )
    {
        PagedSearchContext ctx = pagedSearchContexts.get( contextId );

        return ctx;
    }


    /**
     * The principal and remote address associated with this session.
     * @see Object#toString()
     */
    public String toString()
    {
        if ( coreSession == null )
        {
            return "LdapSession : No Ldap session ...";
        }

        StringBuilder sb = new StringBuilder();

        LdapPrincipal principal = coreSession.getAuthenticatedPrincipal();
        SocketAddress address = coreSession.getClientAddress();

        sb.append( "LdapSession : <" );

        if ( principal != null )
        {
            sb.append( principal.getName() );
            sb.append( "," );
        }

        if ( address != null )
        {
            sb.append( address );
        }
        else
        {
            sb.append( "..." );
        }

        sb.append( ">" );

        return sb.toString();
    }
}
