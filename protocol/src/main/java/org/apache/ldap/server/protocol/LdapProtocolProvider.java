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
package org.apache.ldap.server.protocol;

import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.naming.Context;

import org.apache.ldap.common.exception.LdapNamingException;
import org.apache.ldap.common.message.AbandonRequest;
import org.apache.ldap.common.message.AbandonRequestImpl;
import org.apache.ldap.common.message.AddRequest;
import org.apache.ldap.common.message.AddRequestImpl;
import org.apache.ldap.common.message.BindRequest;
import org.apache.ldap.common.message.BindRequestImpl;
import org.apache.ldap.common.message.CompareRequest;
import org.apache.ldap.common.message.CompareRequestImpl;
import org.apache.ldap.common.message.DeleteRequest;
import org.apache.ldap.common.message.DeleteRequestImpl;
import org.apache.ldap.common.message.ExtendedRequest;
import org.apache.ldap.common.message.ExtendedRequestImpl;
import org.apache.ldap.common.message.MessageDecoder;
import org.apache.ldap.common.message.MessageEncoder;
import org.apache.ldap.common.message.ModifyDnRequest;
import org.apache.ldap.common.message.ModifyDnRequestImpl;
import org.apache.ldap.common.message.ModifyRequest;
import org.apache.ldap.common.message.ModifyRequestImpl;
import org.apache.ldap.common.message.ResultCodeEnum;
import org.apache.ldap.common.message.SearchRequest;
import org.apache.ldap.common.message.SearchRequestImpl;
import org.apache.ldap.common.message.UnbindRequest;
import org.apache.ldap.common.message.UnbindRequestImpl;
import org.apache.ldap.common.message.spi.Provider;
import org.apache.mina.protocol.ProtocolCodecFactory;
import org.apache.mina.protocol.ProtocolDecoder;
import org.apache.mina.protocol.ProtocolEncoder;
import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolHandlerAdapter;
import org.apache.mina.protocol.ProtocolProvider;
import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.protocol.codec.Asn1CodecDecoder;
import org.apache.mina.protocol.codec.Asn1CodecEncoder;

/**
 * An LDAP protocol provider implementation which dynamically associates
 * handlers.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class LdapProtocolProvider implements ProtocolProvider
{
    /** the constant service name of this ldap protocol provider **/
    public static final String SERVICE_NAME = "ldap";

    /** a map of the default request object class name to the handler class name */
    public static final Map DEFAULT_HANDLERS;

    static
    {
        HashMap map = new HashMap();

        /*
         * Note:
         *
         * By mapping the implementation class in addition to the interface
         * for the request type to the handler Class we're bypassing the need
         * to iterate through Interface[] looking for handlers.  For the default
         * cases here the name of the request object's class will look up
         * immediately.
         */

        map.put( AbandonRequest.class.getName(), AbandonHandler.class );
        map.put( AbandonRequestImpl.class.getName(), AbandonHandler.class );

        map.put( AddRequest.class.getName(), AddHandler.class );
        map.put( AddRequestImpl.class.getName(), AddHandler.class );

        map.put( BindRequest.class.getName(), BindHandler.class );
        map.put( BindRequestImpl.class.getName(), BindHandler.class );

        map.put( CompareRequest.class.getName(), CompareHandler.class );
        map.put( CompareRequestImpl.class.getName(), CompareHandler.class );

        map.put( DeleteRequest.class.getName(), DeleteHandler.class );
        map.put( DeleteRequestImpl.class.getName(), DeleteHandler.class );

        map.put( ExtendedRequest.class.getName(), ExtendedHandler.class );
        map.put( ExtendedRequestImpl.class.getName(), ExtendedHandler.class );

        map.put( ModifyRequest.class.getName(), ModifyHandler.class );
        map.put( ModifyRequestImpl.class.getName(), ModifyHandler.class );

        map.put( ModifyDnRequest.class.getName(), ModifyDnHandler.class );
        map.put( ModifyDnRequestImpl.class.getName(), ModifyDnHandler.class );

        map.put( SearchRequest.class.getName(), SearchHandler.class );
        map.put( SearchRequestImpl.class.getName(), SearchHandler.class );

        map.put( UnbindRequest.class.getName(), UnbindHandler.class );
        map.put( UnbindRequestImpl.class.getName(), UnbindHandler.class );

        DEFAULT_HANDLERS = Collections.unmodifiableMap( map );
    }

    /** the underlying provider codec factory */
    private final ProtocolCodecFactory codecFactory;

    /** the MINA protocol handler */
    private final LdapProtocolHandler handler = new LdapProtocolHandler();

    /** the handlers to use while processing requests */
    private final Map commandHandlers;

    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    /**
     * Creates a SEDA LDAP protocol provider.
     *
     * @param env environment properties used to configure the provider and
     * underlying codec providers if any
     */
    public LdapProtocolProvider( Hashtable env ) throws LdapNamingException
    {
        Hashtable copy = ( Hashtable ) env.clone();
        this.commandHandlers = new HashMap();

        copy.put( Context.PROVIDER_URL, "" );
        SessionRegistry.releaseSingleton();
        new SessionRegistry( copy );

        Iterator requestTypes = DEFAULT_HANDLERS.keySet().iterator();
        while( requestTypes.hasNext() )
        {
            CommandHandler handler = null;
            String type = ( String ) requestTypes.next();
            Class clazz = null;

            if( copy.containsKey( type ) )
            {
                try
                {
                    clazz = Class.forName( ( String ) copy.get( type ) );
                }
                catch( ClassNotFoundException e )
                {
                    LdapNamingException lne;
                    String msg = "failed to load class " + clazz;
                    msg += " for processing " + type + " objects.";
                    lne = new LdapNamingException( msg, ResultCodeEnum.OTHER );
                    lne.setRootCause( e );
                    throw lne;
                }
            }
            else
            {
                clazz = ( Class ) DEFAULT_HANDLERS.get( type );
            }

            try
            {
                handler = ( CommandHandler ) clazz.newInstance();
            }
            catch( Exception e )
            {
                LdapNamingException lne;
                String msg = "failed to create handler instance of " + clazz;
                msg += " for processing " + type + " objects.";
                lne = new LdapNamingException( msg, ResultCodeEnum.OTHER );
                lne.setRootCause( e );
                throw lne;
            }

            this.commandHandlers.put( type, handler );
        }

        this.codecFactory = new ProtocolCodecFactoryImpl( copy );
    }

    /**
     * Creates a SEDA LDAP protocol provider.
     */
    public LdapProtocolProvider() throws LdapNamingException
    {
        this.commandHandlers = new HashMap();
        SessionRegistry.releaseSingleton();
        new SessionRegistry( null );

        Iterator requestTypes = DEFAULT_HANDLERS.keySet().iterator();
        while( requestTypes.hasNext() )
        {
            CommandHandler handler = null;
            String type = ( String ) requestTypes.next();
            Class clazz = null;

            clazz = ( Class ) DEFAULT_HANDLERS.get( type );

            try
            {
                handler = ( CommandHandler ) clazz.newInstance();
            }
            catch( Exception e )
            {
                LdapNamingException lne;
                String msg = "failed to create handler instance of " + clazz;
                msg += " for processing " + type + " objects.";
                lne = new LdapNamingException( msg, ResultCodeEnum.OTHER );
                lne.setRootCause( e );
                throw lne;
            }

            this.commandHandlers.put( type, handler );
        }

        this.codecFactory = new ProtocolCodecFactoryImpl();
    }

    // ------------------------------------------------------------------------
    // ProtocolProvider Methods
    // ------------------------------------------------------------------------

    public String getName()
    {
        return SERVICE_NAME;
    }

    public ProtocolCodecFactory getCodecFactory()
    {
        return codecFactory;
    }

    public ProtocolHandler getHandler()
    {
        return handler;
    }

    public CommandHandler getCommandHandler( Object request )
    {
        if( this.commandHandlers.containsKey( request.getClass().getName() ) )
        {
            return ( CommandHandler ) this.commandHandlers.get( request
                    .getClass().getName() );
        }

        Class[] interfaces = request.getClass().getInterfaces();
        for( int ii = 0; ii < interfaces.length; ii ++ )
        {
            if( this.commandHandlers.containsKey( interfaces[ ii ].getName() ) )
            {
                return ( CommandHandler ) this.commandHandlers
                        .get( interfaces[ ii ].getName() );
            }
        }

        String msg = "cannot find a handler for request: " + request;
        throw new IllegalArgumentException( msg );
    }

    /**
     * A snickers based BER Decoder factory.
     */
    private static final class ProtocolCodecFactoryImpl implements
            ProtocolCodecFactory
    {
        final Hashtable env;

        public ProtocolCodecFactoryImpl()
        {
            this.env = null;
        }

        ProtocolCodecFactoryImpl( Hashtable env )
        {
            this.env = env;
        }

        public ProtocolEncoder newEncoder()
        {
            if( env == null || env.get( Provider.BERLIB_PROVIDER ) == null )
            {
                return new Asn1CodecEncoder( new MessageEncoder() );
            }
            else
            {
                return new Asn1CodecEncoder( new MessageEncoder( env ) );
            }
        }

        public ProtocolDecoder newDecoder()
        {
            if( env == null || env.get( Provider.BERLIB_PROVIDER ) == null )
            {
                return new Asn1CodecDecoder( new MessageDecoder() );
            }
            else
            {
                return new Asn1CodecDecoder( new MessageDecoder( env ) );
            }
        }
    }

    private class LdapProtocolHandler extends ProtocolHandlerAdapter
    {

        public void messageReceived( ProtocolSession session, Object request )
        {
            CommandHandler handler = getCommandHandler( request );
            handler.handle( session, request );
        }

        public void sessionClosed( ProtocolSession session )
        {
            SessionRegistry.getSingleton().remove( session );
        }

        public void exceptionCaught( ProtocolSession session, Throwable cause )
        {
            cause.printStackTrace();
        }
    }
}
