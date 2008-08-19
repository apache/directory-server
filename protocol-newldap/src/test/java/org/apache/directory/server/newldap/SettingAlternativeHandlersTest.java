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
package org.apache.directory.server.newldap;


import junit.framework.TestCase;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.newldap.LdapServer;
import org.apache.directory.server.newldap.handlers.NewAbandonHandler;
import org.apache.directory.server.newldap.handlers.NewAddHandler;
import org.apache.directory.server.newldap.handlers.NewBindHandler;
import org.apache.directory.server.newldap.handlers.NewCompareHandler;
import org.apache.directory.server.newldap.handlers.NewDeleteHandler;
import org.apache.directory.server.newldap.handlers.NewModifyDnHandler;
import org.apache.directory.server.newldap.handlers.NewModifyHandler;
import org.apache.directory.server.newldap.handlers.NewSearchHandler;
import org.apache.directory.server.newldap.handlers.NewUnbindHandler;
import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.message.AbandonRequest;
import org.apache.directory.shared.ldap.message.AddRequest;
import org.apache.directory.shared.ldap.message.BindRequest;
import org.apache.directory.shared.ldap.message.CompareRequest;
import org.apache.directory.shared.ldap.message.DeleteRequest;
import org.apache.directory.shared.ldap.message.ModifyDnRequest;
import org.apache.directory.shared.ldap.message.ModifyRequest;
import org.apache.directory.shared.ldap.message.SearchRequest;
import org.apache.directory.shared.ldap.message.UnbindRequest;
import org.apache.mina.common.IoSession;


/**
 * This test is simply used to test that handlers can be set properly.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SettingAlternativeHandlersTest extends TestCase
{
    LdapServer ldapServer;


    public void setUp() throws Exception
    {
        ldapServer = new LdapServer();
        
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
        assertEquals( ldapServer.getAbandonHandler().getClass(), BogusAbandonHandler.class  );
        assertEquals( ldapServer.getAddHandler().getClass(), BogusAddHandler.class  );
        assertEquals( ldapServer.getBindHandler().getClass(), BogusBindHandler.class  );
        assertEquals( ldapServer.getCompareHandler().getClass(), BogusCompareHandler.class  );
        assertEquals( ldapServer.getDeleteHandler().getClass(), BogusDeleteHandler.class  );
        assertEquals( ldapServer.getModifyDnHandler().getClass(), BogusModifyDnHandler.class  );
        assertEquals( ldapServer.getModifyHandler().getClass(), BogusModifyHandler.class  );
        assertEquals( ldapServer.getSearchHandler().getClass(), BogusSearchHandler.class  );
        assertEquals( ldapServer.getUnbindHandler().getClass(), BogusUnbindHandler.class  );
        assertEquals( ldapServer.getName(), LdapServer.SERVICE_NAME );
    }

    
    public static class BogusAbandonHandler extends NewAbandonHandler
    {
        public void abandonMessageReceived( IoSession session, AbandonRequest request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    
    public static class BogusUnbindHandler extends NewUnbindHandler
    {
        public void unbindMessageReceived( IoSession session, UnbindRequest request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusAddHandler extends NewAddHandler
    {
        public void addMessageReceived( IoSession session, AddRequest request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusBindHandler extends NewBindHandler
    {
        public void setDirectoryService( DirectoryService directoryService )
        {
        }


        public void bindMessageReceived( IoSession session, BindRequest request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusCompareHandler extends NewCompareHandler
    {
        public void compareMessageReceived( IoSession session, CompareRequest request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusDeleteHandler extends  NewDeleteHandler
    {
        public void deleteMessageReceived( IoSession session, DeleteRequest request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusModifyDnHandler extends  NewModifyDnHandler
    {
        public void modifyDnMessageReceived( IoSession session, ModifyDnRequest request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusModifyHandler extends NewModifyHandler
    {
        public void modifyMessageReceived( IoSession session, ModifyRequest request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusSearchHandler extends NewSearchHandler
    {
        public void searchMessageReceived( IoSession session, SearchRequest request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }
}
