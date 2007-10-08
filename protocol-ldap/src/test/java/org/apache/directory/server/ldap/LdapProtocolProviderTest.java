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
import org.apache.directory.server.ldap.support.ExtendedHandler;
import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.message.*;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.demux.MessageHandler;

import java.util.Hashtable;


/**
 * Tests the .
 * FIXME: This test case doesn't test enough now.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class LdapProtocolProviderTest extends TestCase
{
    /**
     * Tests to make sure all the default handlers are kicking in properly with
     * the right request type.
     *
     * @throws LdapNamingException if there are problems initializing the
     * provider
     */
    public void testDefaultOperation() throws LdapNamingException
    {
        DirectoryService directoryService = new DefaultDirectoryService();
        LdapProtocolProvider provider = new LdapProtocolProvider( directoryService,
                new LdapConfiguration(), new Hashtable<String,Object>() );
        assertNotNull( provider.getCodecFactory() );
        assertEquals( provider.getName(), LdapProtocolProvider.SERVICE_NAME );
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
        Hashtable<String,Object> props = new Hashtable<String,Object>();

        props.put( AbandonRequest.class.getName(), BogusAbandonHandler.class.getName() );
        props.put( AbandonRequestImpl.class.getName(), BogusAbandonHandler.class.getName() );

        props.put( AddRequest.class.getName(), BogusAddHandler.class.getName() );
        props.put( AddRequestImpl.class.getName(), BogusAddHandler.class.getName() );

        props.put( BindRequest.class.getName(), BogusBindHandler.class.getName() );
        props.put( BindRequestImpl.class.getName(), BogusBindHandler.class.getName() );

        props.put( CompareRequest.class.getName(), BogusCompareHandler.class.getName() );
        props.put( CompareRequestImpl.class.getName(), BogusCompareHandler.class.getName() );

        props.put( DeleteRequest.class.getName(), BogusDeleteHandler.class.getName() );
        props.put( DeleteRequestImpl.class.getName(), BogusDeleteHandler.class.getName() );

        props.put( ExtendedRequest.class.getName(), ExtendedHandler.class.getName() );
        props.put( ExtendedRequestImpl.class.getName(), ExtendedHandler.class.getName() );

        props.put( ModifyRequest.class.getName(), BogusModifyHandler.class.getName() );
        props.put( ModifyRequestImpl.class.getName(), BogusModifyHandler.class.getName() );

        props.put( ModifyDnRequest.class.getName(), BogusModifyDnHandler.class.getName() );
        props.put( ModifyDnRequestImpl.class.getName(), BogusModifyDnHandler.class.getName() );

        props.put( SearchRequest.class.getName(), BogusSearchHandler.class.getName() );
        props.put( SearchRequestImpl.class.getName(), BogusSearchHandler.class.getName() );

        props.put( UnbindRequest.class.getName(), BogusUnbindHandler.class.getName() );
        props.put( UnbindRequestImpl.class.getName(), BogusUnbindHandler.class.getName() );

        DirectoryService directoryService = new DefaultDirectoryService();
        LdapProtocolProvider provider = new LdapProtocolProvider( directoryService, new LdapConfiguration(), props );
        assertNotNull( provider.getCodecFactory() );
        assertEquals( provider.getName(), LdapProtocolProvider.SERVICE_NAME );
    }

    public static class BogusAbandonHandler implements MessageHandler
    {
        public void messageReceived( IoSession session, Object request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusUnbindHandler implements MessageHandler
    {
        public void messageReceived( IoSession session, Object request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusAddHandler implements MessageHandler
    {
        public void messageReceived( IoSession session, Object request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusBindHandler implements MessageHandler
    {
        public void messageReceived( IoSession session, Object request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusCompareHandler implements MessageHandler
    {
        public void messageReceived( IoSession session, Object request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusDeleteHandler implements MessageHandler
    {
        public void messageReceived( IoSession session, Object request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusModifyDnHandler implements MessageHandler
    {
        public void messageReceived( IoSession session, Object request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusModifyHandler implements MessageHandler
    {
        public void messageReceived( IoSession session, Object request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusSearchHandler implements MessageHandler
    {
        public void messageReceived( IoSession session, Object request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }
}
