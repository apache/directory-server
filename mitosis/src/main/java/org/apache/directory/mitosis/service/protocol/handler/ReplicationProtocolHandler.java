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
package org.apache.directory.mitosis.service.protocol.handler;


import org.apache.directory.mitosis.configuration.ReplicationConfiguration;
import org.apache.directory.mitosis.service.ReplicationContext;
import org.apache.directory.mitosis.service.ReplicationService;
import org.apache.directory.mitosis.service.DefaultReplicationContext;
import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;

/**
 * A MINA {@link IoHandler} that wraps a {@link ReplicationContextHandler}
 * and provides a default implementation of {@link ReplicationContext}.
 *  
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class ReplicationProtocolHandler implements IoHandler
{
    private static final String CONTEXT = "context";

    private final ReplicationService service;
    private final ReplicationConfiguration configuration;
    private final DirectoryServiceConfiguration serviceCfg;
    private final ReplicationContextHandler contextHandler;


    public ReplicationProtocolHandler( ReplicationService service, ReplicationContextHandler contextHandler )
    {
        assert service != null;
        assert contextHandler != null;

        this.service = service;
        this.configuration = service.getConfiguration();
        this.serviceCfg = service.getFactoryConfiguration();
        this.contextHandler = contextHandler;
    }


    public ReplicationContext getContext( IoSession session )
    {
        return ( ReplicationContext ) session.getAttribute( CONTEXT );
    }
    
    public ReplicationContextHandler getContextHandler() {
    	return contextHandler;
    }


    public void sessionCreated( IoSession session ) throws Exception
    {
        session.setAttribute( CONTEXT, new DefaultReplicationContext( service, serviceCfg, configuration, session ) );
    }


    public void exceptionCaught( IoSession session, Throwable cause ) throws Exception
    {
        contextHandler.exceptionCaught( getContext( session ), cause );
    }


    public void messageReceived( IoSession session, Object message ) throws Exception
    {
        contextHandler.messageReceived( getContext( session ), message );
    }


    public void messageSent( IoSession session, Object message ) throws Exception
    {
        contextHandler.messageSent( getContext( session ), message );
    }


    public void sessionClosed( IoSession session ) throws Exception
    {
        ReplicationContext ctx = getContext( session );
        contextHandler.contextEnd( ctx );
        ctx.cancelAllExpirations();
    }


    public void sessionIdle( IoSession session, IdleStatus status ) throws Exception
    {
        contextHandler.contextIdle( getContext( session ), status );
    }


    public void sessionOpened( IoSession session ) throws Exception
    {
        contextHandler.contextBegin( getContext( session ) );
    }
}
