/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.eve.protocol;


import java.util.Properties;
import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.ldap.common.exception.LdapNamingException;
import org.apache.ldap.common.message.*;
import org.apache.ldap.common.NotImplementedException;
import org.apache.seda.protocol.*;
import org.apache.seda.listener.ClientKey;


/**
 * Tests the LdapProtocolProvider.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
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
        LdapProtocolProvider provider = new LdapProtocolProvider();
        assertNotNull( provider.getDecoderFactory() );
        assertNotNull( provider.getEncoderFactory() );
        assertTrue( provider.getName() == LdapProtocolProvider.SERVICE_NAME );

        Object req = null;

        req = new AbandonRequestImpl( 0 );
        assertTrue( provider.getHandler( null, req ) instanceof AbandonHandler );

        req = new AddRequestImpl( 0 );
        assertTrue( provider.getHandler( null, req ) instanceof AddHandler );

        req = new BindRequestImpl( 0 );
        assertTrue( provider.getHandler( null, req ) instanceof BindHandler );

        req = new CompareRequestImpl( 0 );
        assertTrue( provider.getHandler( null, req ) instanceof CompareHandler );

        req = new DeleteRequestImpl( 0 );
        assertTrue( provider.getHandler( null, req ) instanceof DeleteHandler );

        req = new ExtendedRequestImpl( 0 );
        assertTrue( provider.getHandler( null, req ) instanceof ExtendedHandler );

        req = new ModifyDnRequestImpl( 0 );
        assertTrue( provider.getHandler( null, req ) instanceof ModifyDnHandler );

        req = new ModifyRequestImpl( 0 );
        assertTrue( provider.getHandler( null, req ) instanceof ModifyHandler );

        req = new SearchRequestImpl( 0 );
        assertTrue( provider.getHandler( null, req ) instanceof SearchHandler );

        req = new UnbindRequestImpl( 0 );
        assertTrue( provider.getHandler( null, req ) instanceof UnbindHandler );
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
        Properties props = new Properties();

        props.setProperty( AbandonRequest.class.getName(), BogusAbandonHandler.class.getName() );
        props.setProperty( AbandonRequestImpl.class.getName(), BogusAbandonHandler.class.getName() );

        props.setProperty( AddRequest.class.getName(), BogusAddHandler.class.getName() );
        props.setProperty( AddRequestImpl.class.getName(), BogusAddHandler.class.getName() );

        props.setProperty( BindRequest.class.getName(), BogusBindHandler.class.getName() );
        props.setProperty( BindRequestImpl.class.getName(), BogusBindHandler.class.getName() );

        props.setProperty( CompareRequest.class.getName(), BogusCompareHandler.class.getName() );
        props.setProperty( CompareRequestImpl.class.getName(), BogusCompareHandler.class.getName() );

        props.setProperty( DeleteRequest.class.getName(), BogusDeleteHandler.class.getName() );
        props.setProperty( DeleteRequestImpl.class.getName(), BogusDeleteHandler.class.getName() );

        props.setProperty( ExtendedRequest.class.getName(), BogusExtendedHandler.class.getName() );
        props.setProperty( ExtendedRequestImpl.class.getName(), BogusExtendedHandler.class.getName() );

        props.setProperty( ModifyRequest.class.getName(), BogusModifyHandler.class.getName() );
        props.setProperty( ModifyRequestImpl.class.getName(), BogusModifyHandler.class.getName() );

        props.setProperty( ModifyDnRequest.class.getName(), BogusModifyDnHandler.class.getName() );
        props.setProperty( ModifyDnRequestImpl.class.getName(), BogusModifyDnHandler.class.getName() );

        props.setProperty( SearchRequest.class.getName(), BogusSearchHandler.class.getName() );
        props.setProperty( SearchRequestImpl.class.getName(), BogusSearchHandler.class.getName() );

        props.setProperty( UnbindRequest.class.getName(), BogusUnbindHandler.class.getName() );
        props.setProperty( UnbindRequestImpl.class.getName(), BogusUnbindHandler.class.getName() );

        LdapProtocolProvider provider = new LdapProtocolProvider( props );
        assertNotNull( provider.getDecoderFactory() );
        assertNotNull( provider.getEncoderFactory() );
        assertTrue( provider.getName() == LdapProtocolProvider.SERVICE_NAME );

        Object req = null;
        RequestHandler handler = null;

        req = new AbandonRequestImpl( 0 );
        handler = provider.getHandler( null, req );
        assertEquals( HandlerTypeEnum.NOREPLY, handler.getHandlerType() );
        assertTrue( handler instanceof BogusAbandonHandler );

        req = new AddRequestImpl( 0 );
        handler = provider.getHandler( null, req );
        assertEquals( HandlerTypeEnum.SINGLEREPLY, handler.getHandlerType() );
        assertTrue( handler instanceof BogusAddHandler );

        req = new BindRequestImpl( 0 );
        handler = provider.getHandler( null, req );
        assertEquals( HandlerTypeEnum.SINGLEREPLY, handler.getHandlerType() );
        assertTrue( handler instanceof BogusBindHandler );

        req = new CompareRequestImpl( 0 );
        handler = provider.getHandler( null, req );
        assertEquals( HandlerTypeEnum.SINGLEREPLY, handler.getHandlerType() );
        assertTrue( handler instanceof BogusCompareHandler );

        req = new DeleteRequestImpl( 0 );
        handler = provider.getHandler( null, req );
        assertEquals( HandlerTypeEnum.SINGLEREPLY, handler.getHandlerType() );
        assertTrue( handler instanceof BogusDeleteHandler );

        req = new ExtendedRequestImpl( 0 );
        handler = provider.getHandler( null, req );
        assertEquals( HandlerTypeEnum.SINGLEREPLY, handler.getHandlerType() );
        assertTrue( handler instanceof BogusExtendedHandler );

        req = new ModifyDnRequestImpl( 0 );
        handler = provider.getHandler( null, req );
        assertEquals( HandlerTypeEnum.SINGLEREPLY, handler.getHandlerType() );
        assertTrue( handler instanceof BogusModifyDnHandler );

        req = new ModifyRequestImpl( 0 );
        handler = provider.getHandler( null, req );
        assertEquals( HandlerTypeEnum.SINGLEREPLY, handler.getHandlerType() );
        assertTrue( handler instanceof BogusModifyHandler );

        req = new SearchRequestImpl( 0 );
        handler = provider.getHandler( null, req );
        assertEquals( HandlerTypeEnum.MANYREPLY, handler.getHandlerType() );
        assertTrue( handler instanceof BogusSearchHandler );

        req = new UnbindRequestImpl( 0 );
        handler = provider.getHandler( null, req );
        assertEquals( HandlerTypeEnum.NOREPLY, handler.getHandlerType() );
        assertTrue( handler instanceof BogusUnbindHandler );
    }


    public static class BogusAbandonHandler extends AbstractNoReplyHandler
    {
        public void handle( ClientKey key, Object request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }


    public static class BogusUnbindHandler extends AbstractNoReplyHandler
    {
        public void handle( ClientKey key, Object request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }


    public static class BogusAddHandler extends AbstractSingleReplyHandler
    {
        public Object handle( ClientKey key, Object request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }


    public static class BogusBindHandler extends AbstractSingleReplyHandler
    {
        public Object handle( ClientKey key, Object request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }


    public static class BogusCompareHandler extends AbstractSingleReplyHandler
    {
        public Object handle( ClientKey key, Object request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }


    public static class BogusDeleteHandler extends AbstractSingleReplyHandler
    {
        public Object handle( ClientKey key, Object request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }


    public static class BogusExtendedHandler extends AbstractSingleReplyHandler
    {
        public Object handle( ClientKey key, Object request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }


    public static class BogusModifyDnHandler extends AbstractSingleReplyHandler
    {
        public Object handle( ClientKey key, Object request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }


    public static class BogusModifyHandler extends AbstractSingleReplyHandler
    {
        public Object handle( ClientKey key, Object request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }


    public static class BogusSearchHandler extends AbstractManyReplyHandler
    {
        public BogusSearchHandler()
        {
            super( true );
        }

        public Iterator handle( ClientKey key, Object request )
        {
            throw new NotImplementedException( "handler not implemented!" );
        }
    }
}
