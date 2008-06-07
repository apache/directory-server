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
package org.apache.directory.server.newldap.handlers;


import java.util.HashMap;
import java.util.Map;

import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.newldap.LdapProtocolConstants;
import org.apache.directory.server.newldap.LdapServer;
import org.apache.directory.shared.ldap.message.AbandonableRequest;
import org.apache.directory.shared.ldap.message.Request;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.demux.MessageHandler;


/**
 * A base class for all handlers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 541827 $
 */
public abstract class LdapRequestHandler<T extends Request> implements MessageHandler<T>, LdapProtocolConstants
{
    private Object outstandingLock;
    private LdapServer ldapServer;


    public final LdapServer getLdapServer()
    {
        return ldapServer;
    }


    public final void setLdapServer( LdapServer provider )
    {
        this.ldapServer = provider;
    }
    
    
    public final CoreSession getCoreSession( IoSession session )
    {
        return ( CoreSession ) session.getAttribute( CORE_SESSION_KEY );
    }
    
    
    public final void setCoreSession( IoSession session, CoreSession coreSession )
    {
        session.setAttribute( CORE_SESSION_KEY, coreSession );
    }
    
    
    @SuppressWarnings("unchecked")
    public final AbandonableRequest getOutstandingRequest( IoSession session, Integer id )
    {
        synchronized( outstandingLock )
        {
            Map<Integer, AbandonableRequest> outstanding = ( Map<Integer, AbandonableRequest> ) session.getAttribute( OUTSTANDING_KEY );
            
            if ( outstanding == null )
            {
                return null;
            }
            
            return outstanding.get( id );
        }
    }

    
    @SuppressWarnings("unchecked")
    public final AbandonableRequest removeOutstandingRequest( IoSession session, Integer id )
    {
        synchronized( outstandingLock )
        {
            Map<Integer, AbandonableRequest> outstanding = ( Map<Integer, AbandonableRequest> ) session.getAttribute( OUTSTANDING_KEY );
            
            if ( outstanding == null )
            {
                return null;
            }
            
            return outstanding.remove( id );
        }
    }

    
    @SuppressWarnings("unchecked")
    public void setOutstandingRequest( IoSession session, AbandonableRequest request )
    {
        synchronized( outstandingLock )
        {
            Map<Integer, AbandonableRequest> outstanding = ( Map<Integer, AbandonableRequest> ) session.getAttribute( OUTSTANDING_KEY );
            
            if ( outstanding == null )
            {
                outstanding = new HashMap<Integer, AbandonableRequest>();
                session.setAttribute( OUTSTANDING_KEY, outstanding );
            }
            
            outstanding.put( request.getMessageId(), request );
        }
    }
}
