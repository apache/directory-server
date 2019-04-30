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

import org.apache.directory.api.ldap.model.message.*;
import org.apache.directory.api.util.exception.NotImplementedException;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.ldap.handlers.request.AbandonRequestHandler;
import org.apache.directory.server.ldap.handlers.request.AddRequestHandler;
import org.apache.directory.server.ldap.handlers.request.BindRequestHandler;
import org.apache.directory.server.ldap.handlers.request.CompareRequestHandler;
import org.apache.directory.server.ldap.handlers.request.DeleteRequestHandler;
import org.apache.directory.server.ldap.handlers.request.ModifyDnRequestHandler;
import org.apache.directory.server.ldap.handlers.request.ModifyRequestHandler;
import org.apache.directory.server.ldap.handlers.request.SearchRequestHandler;
import org.apache.directory.server.ldap.handlers.request.UnbindRequestHandler;
import org.apache.directory.server.ldap.handlers.response.AddResponseHandler;
import org.apache.directory.server.ldap.handlers.response.BindResponseHandler;
import org.apache.directory.server.ldap.handlers.response.CompareResponseHandler;
import org.apache.directory.server.ldap.handlers.response.DeleteResponseHandler;
import org.apache.directory.server.ldap.handlers.response.ModifyDnResponseHandler;
import org.apache.directory.server.ldap.handlers.response.ModifyResponseHandler;
import org.apache.directory.server.ldap.handlers.response.SearchResultDoneHandler;
import org.apache.directory.server.ldap.handlers.response.SearchResultEntryHandler;
import org.apache.directory.server.ldap.handlers.response.SearchResultReferenceHandler;
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
        assertEquals( LdapServer.SERVICE_NAME, ldapServer.getName() );
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
        ldapServer.setAddHandlers( new BogusAddRequestHandler(), new BogusAddResponseHandler() );
        ldapServer.setBindHandlers( new BogusBindRequestHandler(), new BogusBindResponseHandler() );
        ldapServer.setCompareHandlers( new BogusCompareRequestHandler(), new BogusCompareResponseHandler() );
        ldapServer.setDeleteHandlers( new BogusDeleteRequestHandler(), new BogusDeleteResponseHandler() );
        ldapServer.setModifyDnHandlers( new BogusModifyDnRequestHandler(), new BogusModifyDnResponseHandler() );
        ldapServer.setModifyHandlers( new BogusModifyRequestHandler(), new BogusModifyResponseHandler() );
        ldapServer.setSearchHandlers( new BogusSearchRequestHandler(), 
            new BogusSearchResultEntryHandler(),
            new BogusSearchResultReferenceHandler(),
            new BogusSearchResultDoneHandler() );
        ldapServer.setUnbindHandler( new BogusUnbindRequestHandler() );

        assertEquals( ldapServer.getAbandonRequestHandler().getClass(), BogusAbandonHandler.class );

        assertEquals( ldapServer.getAddRequestHandler().getClass(), BogusAddRequestHandler.class );
        assertEquals( ldapServer.getAddResponseHandler().getClass(), BogusAddResponseHandler.class );
        
        assertEquals( ldapServer.getBindRequestHandler().getClass(), BogusBindRequestHandler.class );
        assertEquals( ldapServer.getBindResponseHandler().getClass(), BogusBindResponseHandler.class );
        
        assertEquals( ldapServer.getCompareRequestHandler().getClass(), BogusCompareRequestHandler.class );
        assertEquals( ldapServer.getCompareResponseHandler().getClass(), BogusCompareResponseHandler.class );

        assertEquals( ldapServer.getSearchRequestHandler().getClass(), BogusSearchRequestHandler.class );
        assertEquals( ldapServer.getSearchResultEntryHandler().getClass(), BogusSearchResultEntryHandler.class );
        assertEquals( ldapServer.getSearchResultReferenceHandler().getClass(), BogusSearchResultReferenceHandler.class );
        assertEquals( ldapServer.getSearchResultDoneHandler().getClass(), BogusSearchResultDoneHandler.class );

        assertEquals( ldapServer.getDeleteRequestHandler().getClass(), BogusDeleteRequestHandler.class );
        assertEquals( ldapServer.getDeleteResponseHandler().getClass(), BogusDeleteResponseHandler.class );

        assertEquals( ldapServer.getModifyDnRequestHandler().getClass(), BogusModifyDnRequestHandler.class );
        assertEquals( ldapServer.getModifyDnResponseHandler().getClass(), BogusModifyDnResponseHandler.class );
        
        assertEquals( ldapServer.getModifyRequestHandler().getClass(), BogusModifyRequestHandler.class );
        assertEquals( ldapServer.getModifyResponseHandler().getClass(), BogusModifyResponseHandler.class );
        
        assertEquals( ldapServer.getUnbindRequestHandler().getClass(), BogusUnbindRequestHandler.class );
        
        assertEquals( LdapServer.SERVICE_NAME, ldapServer.getName() );
    }

    public static class BogusAbandonHandler extends AbandonRequestHandler
    {
        public void abandonRequest( IoSession session, AbandonRequest request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusUnbindRequestHandler extends UnbindRequestHandler
    {
        public void unbindRequest( IoSession session, UnbindRequest request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusAddRequestHandler extends AddRequestHandler
    {
        public void addRequest( IoSession session, AddRequest request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusAddResponseHandler extends AddResponseHandler
    {
        public void addResponse( IoSession session, AddResponse request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusBindRequestHandler extends BindRequestHandler
    {
        public void setDirectoryService( DirectoryService directoryService )
        {
        }


        public void bindRequest( IoSession session, BindRequest request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusBindResponseHandler extends BindResponseHandler
    {
        public void setDirectoryService( DirectoryService directoryService )
        {
        }


        public void bindResponse( IoSession session, BindResponse request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusCompareRequestHandler extends CompareRequestHandler
    {
        public void compareRequest( IoSession session, CompareRequest request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusCompareResponseHandler extends CompareResponseHandler
    {
        public void compareResponse( IoSession session, CompareResponse response )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusDeleteRequestHandler extends DeleteRequestHandler
    {
        public void deleteRequest( IoSession session, DeleteRequest request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusDeleteResponseHandler extends DeleteResponseHandler
    {
        public void deleteResponse( IoSession session, DeleteResponse response )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusModifyDnRequestHandler extends ModifyDnRequestHandler
    {
        public void modifyDnRequest( IoSession session, ModifyDnRequest request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusModifyDnResponseHandler extends ModifyDnResponseHandler
    {
        public void modifyDnResponse( IoSession session, ModifyDnResponse response )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusModifyRequestHandler extends ModifyRequestHandler
    {
        public void modifyRequest( IoSession session, ModifyRequest request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusModifyResponseHandler extends ModifyResponseHandler
    {
        public void modifyResponse( IoSession session, ModifyResponse response )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusSearchRequestHandler extends SearchRequestHandler
    {
        public void searchRequest( IoSession session, SearchRequest request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusSearchResultEntryHandler extends SearchResultEntryHandler
    {
        public void searchResultEntry( IoSession session, SearchResultEntry searchResultEntry )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusSearchResultReferenceHandler extends SearchResultReferenceHandler
    {
        public void searchResultReference( IoSession session, SearchResultReference searchResultReference )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }

    public static class BogusSearchResultDoneHandler extends SearchResultDoneHandler
    {
        public void searchResultDone( IoSession session, SearchResultDone searchResultDone )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }
}
