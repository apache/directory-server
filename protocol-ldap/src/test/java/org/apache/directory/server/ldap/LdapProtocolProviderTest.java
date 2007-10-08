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
import org.apache.mina.common.IoSession;


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
        LdapProtocolProvider provider = new LdapProtocolProvider( directoryService, new LdapConfiguration() );
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
        DirectoryService directoryService = new DefaultDirectoryService();
        LdapProtocolProvider provider = new LdapProtocolProvider( directoryService, new LdapConfiguration() );
        provider.setAbandonHandler( new BogusAbandonHandler() );
        provider.setAddHandler( new BogusAddHandler() );
        provider.setBindHandler( new BogusBindHandler() );
        provider.setCompareHandler( new BogusCompareHandler() );
        provider.setDeleteHandler( new BogusDeleteHandler() );
        provider.setModifyDnHandler( new BogusModifyDnHandler() );
        provider.setModifyHandler( new BogusModifyHandler() );
        provider.setSearchHandler( new BogusSearchHandler() );
        provider.setUnbindHandler( new BogusUnbindHandler() );

        assertNotNull( provider.getCodecFactory() );
        assertEquals( provider.getName(), LdapProtocolProvider.SERVICE_NAME );
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
