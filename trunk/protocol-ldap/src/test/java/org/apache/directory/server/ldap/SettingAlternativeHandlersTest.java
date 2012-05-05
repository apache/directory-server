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


import static org.junit.Assert.assertEquals;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;

import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.ldap.handlers.AbandonHandler;
import org.apache.directory.server.ldap.handlers.AddHandler;
import org.apache.directory.server.ldap.handlers.BindHandler;
import org.apache.directory.server.ldap.handlers.CompareHandler;
import org.apache.directory.server.ldap.handlers.DeleteHandler;
import org.apache.directory.server.ldap.handlers.ModifyDnHandler;
import org.apache.directory.server.ldap.handlers.ModifyHandler;
import org.apache.directory.server.ldap.handlers.SearchHandler;
import org.apache.directory.server.ldap.handlers.UnbindHandler;
import org.apache.directory.shared.ldap.model.message.*;
import org.apache.directory.shared.util.exception.NotImplementedException;
import org.apache.directory.shared.ldap.model.message.DeleteRequest;
import org.apache.directory.shared.ldap.model.message.ModifyRequest;
import org.apache.directory.shared.ldap.model.message.SearchRequest;
import org.apache.mina.core.session.IoSession;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * This test is simply used to test that handlers can be set properly.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class SettingAlternativeHandlersTest
{
    /**
     * Tests to make sure all the default handlers are kicking in properly with
     * the right request type.
     *
     * @throws LdapNamingException if there are problems initializing the
     * provider
     */
    @Test
    public void testDefaultOperation()
    {
        LdapServer ldapServer = new LdapServer();
        assertEquals( ldapServer.getName(), LdapServer.SERVICE_NAME );
    }


    /**
     * Tests to make sure handlers for alternative configurations are kicking
     * in properly with the right request type.
     *
     * @throws LdapNamingException if there are problems initializing the
     * provider
     */
    @Test
    public void testAlternativeConfiguration()
    {
        LdapServer ldapServer = new LdapServer();
        ldapServer.setAbandonHandler( new BogusAbandonHandler() );
        ldapServer.setAddHandler( new BogusAddHandler() );
        ldapServer.setBindHandler( new BogusBindHandler() );
        ldapServer.setCompareHandler( new BogusCompareHandler() );
        ldapServer.setDeleteHandler( new BogusDeleteHandler() );
        ldapServer.setModifyDnHandler( new BogusModifyDnHandler() );
        ldapServer.setModifyHandler( new BogusModifyHandler() );
        ldapServer.setSearchHandler( new BogusSearchHandler() );
        ldapServer.setUnbindHandler( new BogusUnbindHandler() );

        assertEquals( ldapServer.getAbandonHandler().getClass(), BogusAbandonHandler.class );
        assertEquals( ldapServer.getAddHandler().getClass(), BogusAddHandler.class );
        assertEquals( ldapServer.getBindHandler().getClass(), BogusBindHandler.class );
        assertEquals( ldapServer.getCompareHandler().getClass(), BogusCompareHandler.class );
        assertEquals( ldapServer.getDeleteHandler().getClass(), BogusDeleteHandler.class );
        assertEquals( ldapServer.getModifyDnHandler().getClass(), BogusModifyDnHandler.class );
        assertEquals( ldapServer.getModifyHandler().getClass(), BogusModifyHandler.class );
        assertEquals( ldapServer.getSearchHandler().getClass(), BogusSearchHandler.class );
        assertEquals( ldapServer.getUnbindHandler().getClass(), BogusUnbindHandler.class );
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

    public static class BogusDeleteHandler extends DeleteHandler
    {
        public void deleteMessageReceived( IoSession session, DeleteRequest request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusModifyDnHandler extends ModifyDnHandler
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
