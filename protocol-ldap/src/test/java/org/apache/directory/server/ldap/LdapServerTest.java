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
package org.apache.directory.server.ldap;


import junit.framework.TestCase;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.ldap.support.*;
import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.message.*;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.SimpleByteBufferAllocator;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.util.AvailablePortFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * Tests the .
 * FIXME: This test case doesn't test enough now.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class LdapServerTest extends TestCase
{
    private static final Logger LOG = LoggerFactory.getLogger( LdapServerTest.class.getName() );
    LdapServer ldapServer;
    DirectoryService directoryService;
    ExecutorService ioExecutor;
    ExecutorService logicExecutor;
    SocketAcceptor tcpAcceptor;


    public void setUp() throws Exception
    {
        directoryService = new DefaultDirectoryService();
        directoryService.startup();

        if ( getName().equals( "testAlternativeConfiguration" ) )
        {
            ldapServer.setAbandonHandler( new BogusAbandonHandler() );
            ldapServer.setAddHandler( new BogusAddHandler() );
            ldapServer.setBindHandler( new BogusBindHandler() );
            ldapServer.setCompareHandler( new BogusCompareHandler() );
            ldapServer.setDeleteHandler( new BogusDeleteHandler() );
            ldapServer.setModifyDnHandler( new BogusModifyDnHandler() );
            ldapServer.setModifyHandler( new BogusModifyHandler() );
            ldapServer.setSearchHandler( new BogusSearchHandler() );
            ldapServer.setUnbindHandler( new BogusUnbindHandler() );
        }

        ByteBuffer.setAllocator( new SimpleByteBufferAllocator() );
        ByteBuffer.setUseDirectBuffers( false );
        ioExecutor = Executors.newCachedThreadPool();
        logicExecutor = Executors.newFixedThreadPool( 8 );
        tcpAcceptor = new SocketAcceptor( Runtime.getRuntime().availableProcessors(), ioExecutor );
        tcpAcceptor.getFilterChain().addLast( "executor", new ExecutorFilter( logicExecutor ) );

        ldapServer = new LdapServer( tcpAcceptor );
        ldapServer.setIpPort( AvailablePortFinder.getNextAvailable( 1024 ) );
        ldapServer.setDirectoryService( directoryService );
        System.err.println( "********** => " + ldapServer.getIpPort() );
        ldapServer.start();
    }


    public void tearDown() throws Exception
    {
        ldapServer.stop();
        logicExecutor.shutdown();
        for (;;) {
            try {
                if ( logicExecutor.awaitTermination( Integer.MAX_VALUE, TimeUnit.SECONDS ) )
                {
                    break;
                }
            }
            catch ( InterruptedException e )
            {
                LOG.error( "Failed to terminate logic executor", e );
            }
        }

        ioExecutor.shutdown();
        for (;;) {
            try {
                if ( ioExecutor.awaitTermination( Integer.MAX_VALUE, TimeUnit.SECONDS ) )
                {
                    break;
                }
            }
            catch ( InterruptedException e )
            {
                LOG.error( "Failed to terminate io executor", e );
            }
        }
        directoryService.shutdown();
    }


    /**
     * Tests to make sure all the default handlers are kicking in properly with
     * the right request type.
     *
     * @throws LdapNamingException if there are problems initializing the
     * provider
     */
    public void testDefaultOperation() throws LdapNamingException
    {
        assertEquals( ldapServer.getAbandonHandler().getClass(), BogusAbandonHandler.class  );
        assertEquals( ldapServer.getAddHandler().getClass(), BogusAddHandler.class  );
        assertEquals( ldapServer.getBindHandler().getClass(), BogusBindHandler.class  );
        assertEquals( ldapServer.getCompareHandler().getClass(), BogusCompareHandler.class  );
        assertEquals( ldapServer.getDeleteHandler().getClass(), BogusDeleteHandler.class  );
        assertEquals( ldapServer.getModifyDnHandler().getClass(), BogusModifyDnHandler.class  );
        assertEquals( ldapServer.getModifyHandler().getClass(), BogusModifyHandler.class  );
        assertEquals( ldapServer.getSearchHandler().getClass(), BogusSearchHandler.class  );
        assertEquals( ldapServer.getUnbindHandler().getClass(), BogusUnbindHandler.class  );
        assertNotNull( ldapServer.getCodecFactory() );
        assertEquals( ldapServer.getName(), LdapServer.SERVICE_NAME );
    }


    /**
     * Tests to make sure handlers for alternative configurations are kicking
     * in properly with the right request type.
     *
     * @throws LdapNamingException if there are problems initializing the
     * provider
     */
    public void testAlternativeConfiguration() throws LdapNamingException
    {
        assertNotNull( ldapServer.getCodecFactory() );
        assertEquals( ldapServer.getName(), LdapServer.SERVICE_NAME );
    }

    public static class BogusAbandonHandler extends AbandonHandler
    {
        public void abandonMessageReceived( IoSession session, AbandonRequest request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusUnbindHandler extends UnbindHandler
    {
        public void unbindMessageReceived( IoSession session, UnbindRequest request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusAddHandler extends AddHandler
    {
        public void addMessageReceived( IoSession session, AddRequest request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusBindHandler extends BindHandler
    {
        public void setDirectoryService( DirectoryService directoryService )
        {
        }


        public void bindMessageReceived( IoSession session, BindRequest request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusCompareHandler extends CompareHandler
    {
        public void compareMessageReceived( IoSession session, CompareRequest request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusDeleteHandler extends  DeleteHandler
    {
        public void deleteMessageReceived( IoSession session, DeleteRequest request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusModifyDnHandler extends  ModifyDnHandler
    {
        public void modifyDnMessageReceived( IoSession session, ModifyDnRequest request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusModifyHandler extends ModifyHandler
    {
        public void modifyMessageReceived( IoSession session, ModifyRequest request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusSearchHandler extends SearchHandler
    {
        public void searchMessageReceived( IoSession session, SearchRequest request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }
}
