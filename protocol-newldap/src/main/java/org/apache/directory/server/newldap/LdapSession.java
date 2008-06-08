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
package org.apache.directory.server.newldap;


import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.directory.server.core.CoreSession;
import org.apache.directory.shared.ldap.message.AbandonableRequest;
import org.apache.mina.common.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An object representing an LdapSession.  Any connection established with the
 * LDAP server forms a session.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class LdapSession
{
    private static final Logger LOG = LoggerFactory.getLogger( LdapSession.class );
    private static final AbandonableRequest[] EMPTY_ABANDONABLES = new AbandonableRequest[0]; 
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();
    
    private final String outstandingLock;
    private final IoSession ioSession;
    private CoreSession coreSession;
    private Map<Integer, AbandonableRequest> outstandingRequests;
    
 
    /**
     * Creates a new instance of LdapSession associated with the underlying
     * connection (MINA IoSession) to the server.
     *
     * @param ioSession the MINA session associated this LdapSession
     */
    public LdapSession( IoSession ioSession )
    {
        this.ioSession = ioSession;
        this.outstandingLock = "OutstandingRequestLock: " + ioSession.toString();
        this.outstandingRequests = new ConcurrentHashMap<Integer, AbandonableRequest>();
    }
    
    
    public boolean isAuthenticated()
    {
        return coreSession != null;
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
     */
    public AbandonableRequest abandonOutstandingRequest( Integer messageId )
    {
        AbandonableRequest request = null;
        
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
            LOG.warn( "AbandonableRequest with messageId {} has already been abandoned", messageId );
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
    public void unregisterOutstandingRequest( AbandonableRequest request )
    {
        synchronized( outstandingLock )
        {
            outstandingRequests.remove( request.getMessageId() );
        }
    }
    
    
    public Map<Integer, AbandonableRequest> getOutstandingRequests()
    {
        synchronized( outstandingLock )
        {
            return Collections.unmodifiableMap( outstandingRequests );
        }
    }
}
