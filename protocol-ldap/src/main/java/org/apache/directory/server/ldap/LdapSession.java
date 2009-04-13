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

import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.LdapPrincipal;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.ldap.handlers.controls.PagedSearchContext;
import org.apache.directory.shared.ldap.message.InternalAbandonableRequest;
import org.apache.directory.shared.ldap.message.BindStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An object representing an LdapSession. Any connection established with the
 * LDAP server forms a session.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class LdapSession
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( LdapSession.class );
    
    /** A speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The list of requests we can abandon */
    private static final InternalAbandonableRequest[] EMPTY_ABANDONABLES = new InternalAbandonableRequest[0]; 
    
    /** A lock to protect the abandonableRequests against concurrent access */
    private final String outstandingLock;
    
    /**
     * The associated IoSession. Usually, a LdapSession is established
     * at the user request, which means we have a IoSession.
     */
    private final IoSession ioSession;
    
    /** The CoreSession */
    private CoreSession coreSession;
    
    /** A reference on the LdapService instance */
    private LdapService ldapService;
    
    /** A map of all the running requests */
    private Map<Integer, InternalAbandonableRequest> outstandingRequests;
    
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
        outstandingRequests = new ConcurrentHashMap<Integer, InternalAbandonableRequest>();
        bindStatus = BindStatus.ANONYMOUS;
        saslProperties = new HashMap<String, Object>();
        pagedSearchContexts = new HashMap<Integer, PagedSearchContext>();
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
     * Check if the session is in the middle of a SASL negotiation.
     * 
     * @return <code>true</code> if the session is in AuthPending state
     */
    public boolean isAuthPending()
    {
        return bindStatus == BindStatus.AUTH_PENDING;
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
            InternalAbandonableRequest[] abandonables = outstandingRequests.values().toArray( EMPTY_ABANDONABLES );
            
            for ( InternalAbandonableRequest abandonable : abandonables )
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
    public InternalAbandonableRequest abandonOutstandingRequest( int messageId )
    {
        InternalAbandonableRequest request = null;
        
        synchronized ( outstandingLock )
        {
            request = outstandingRequests.remove( messageId );
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
    public void registerOutstandingRequest( InternalAbandonableRequest request )
    {
        synchronized( outstandingLock )
        {
            outstandingRequests.put( request.getMessageId(), request );
        }
    }

    
    /**
     * Unregisters an outstanding request.
     *
     * @param request the request to unregister
     */
    public void unregisterOutstandingRequest( InternalAbandonableRequest request )
    {
        synchronized( outstandingLock )
        {
            outstandingRequests.remove( request.getMessageId() );
        }
    }
    
    
    /**
     * @return A list of all the abandonable requests for this session. 
     */
    public Map<Integer, InternalAbandonableRequest> getOutstandingRequests()
    {
        synchronized( outstandingLock )
        {
            return Collections.unmodifiableMap( outstandingRequests );
        }
    }


    /**
     * @return the current bind status for this session
     */
    public BindStatus getBindStatus()
    {
        return bindStatus;
    }
    
    
    /**
     * Set the current BindStatus to authentication pending
     */
    public void setAuthPending()
    {
        bindStatus = BindStatus.AUTH_PENDING;
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
     *  @return The LdapService reference
     */
    public LdapService getLdapServer()
    {
        return ldapService;
    }


    /**
     * Store a reference on the LdapService intance
     *
     * @param ldapService the LdapService instance
     */
    public void setLdapServer( LdapService ldapService )
    {
        this.ldapService = ldapService;
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
        synchronized ( pagedSearchContexts )
        {
            PagedSearchContext oldContext = pagedSearchContexts.put( context.getCookieValue(), context );
            
            if ( oldContext != null )
            {
                EntryFilteringCursor cursor = oldContext.getCursor();
                
                if ( cursor != null )
                {
                    try
                    {
                        cursor.close();
                    }
                    catch ( Exception e )
                    {
                        LOG.error( "Failing on cursor close : {}", e.getMessage() );
                    }
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
        synchronized ( pagedSearchContexts )
        {
            return pagedSearchContexts.remove( contextId );
        }
    }
    
    
    /**
     * Get paged search context associated with an ID 
     * @param contextId The id for teh context we want to get 
     * @return The associated context, if any
     */
    public PagedSearchContext getPagedSearchContext( int contextId )
    {
        synchronized ( pagedSearchContexts )
        {
            return pagedSearchContexts.get( contextId );
        }
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
