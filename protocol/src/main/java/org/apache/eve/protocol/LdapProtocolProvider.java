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


import java.util.*;

import javax.naming.Context;

import org.apache.seda.protocol.ProtocolProvider;
import org.apache.seda.protocol.RequestHandler;
import org.apache.seda.listener.ClientKey;
import org.apache.seda.event.EventRouter;

import org.apache.ldap.common.message.*;
import org.apache.ldap.common.message.spi.Provider;
import org.apache.ldap.common.exception.LdapNamingException;

import org.apache.asn1.codec.stateful.StatefulDecoder;
import org.apache.asn1.codec.stateful.StatefulEncoder;
import org.apache.asn1.codec.stateful.DecoderFactory;
import org.apache.asn1.codec.stateful.EncoderFactory;
import org.apache.asn1.codec.stateful.*;


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

    /** a handle on the SEDA event router */
    public final EventRouter router;
    /** the underlying provider decoder factory */
    public final DecoderFactory decoderFactory;
    /** the underlying provider encoder factory */
    public final EncoderFactory encoderFactory;
    /** the handlers to use while processing requests */
    public final Map handlers;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * Creates a SEDA LDAP protocol provider.
     *
     * @param env environment properties used to configure the provider and
     * underlying codec providers if any
     */
    public LdapProtocolProvider( Hashtable env, EventRouter router )
            throws LdapNamingException
    {
        this.router = router;
        Hashtable copy = ( Hashtable ) env.clone();
        this.handlers = new HashMap();

        copy.put( Context.PROVIDER_URL, "" );
        SessionRegistry.releaseSingleton();
        new SessionRegistry( copy, router );

        Iterator requestTypes = DEFAULT_HANDLERS.keySet().iterator();
        while ( requestTypes.hasNext() )
        {
            RequestHandler handler = null;
            String type = ( String ) requestTypes.next();
            Class clazz = null;

            if ( copy.containsKey( type ) )
            {
                try
                {
                    clazz = Class.forName( ( String ) copy.get( type ) );
                }
                catch ( ClassNotFoundException e )
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
                handler = ( RequestHandler ) clazz.newInstance();
            }
            catch ( Exception e )
            {
                LdapNamingException lne;
                String msg = "failed to create handler instance of " + clazz;
                msg += " for processing " + type + " objects.";
                lne = new LdapNamingException( msg, ResultCodeEnum.OTHER );
                lne.setRootCause( e );
                throw lne;
            }

            this.handlers.put( type, handler );
        }

        this.decoderFactory = new DecoderFactoryImpl( copy );
        this.encoderFactory = new EncoderFactoryImpl( copy );
    }


    /**
     * Creates a SEDA LDAP protocol provider.
     */
    public LdapProtocolProvider( EventRouter router ) throws LdapNamingException
    {
        this.router = router;
        this.handlers = new HashMap();
        SessionRegistry.releaseSingleton();
        new SessionRegistry( null, router );

        Iterator requestTypes = DEFAULT_HANDLERS.keySet().iterator();
        while ( requestTypes.hasNext() )
        {
            RequestHandler handler = null;
            String type = ( String ) requestTypes.next();
            Class clazz = null;

            clazz = ( Class ) DEFAULT_HANDLERS.get( type );

            try
            {
                handler = ( RequestHandler ) clazz.newInstance();
            }
            catch ( Exception e )
            {
                LdapNamingException lne;
                String msg = "failed to create handler instance of " + clazz;
                msg += " for processing " + type + " objects.";
                lne = new LdapNamingException( msg, ResultCodeEnum.OTHER );
                lne.setRootCause( e );
                throw lne;
            }

            this.handlers.put( type, handler );
        }

        this.decoderFactory = new DecoderFactoryImpl();
        this.encoderFactory = new EncoderFactoryImpl();
    }


    // ------------------------------------------------------------------------
    // ProtocolProvider Methods
    // ------------------------------------------------------------------------


    /**
     * @see ProtocolProvider#getName()
     * @return ldap every time
     */
    public String getName()
    {
        return SERVICE_NAME;
    }


    /**
     * @see ProtocolProvider#getDecoderFactory()
     */
    public DecoderFactory getDecoderFactory()
    {
        return this.decoderFactory;
    }


    /**
     * @see ProtocolProvider#getEncoderFactory()
     */
    public EncoderFactory getEncoderFactory()
    {
        return this.encoderFactory;
    }


    /**
     * @see ProtocolProvider#getHandler(ClientKey, Object)
     */
    public RequestHandler getHandler( ClientKey key, Object request )
    {
        if ( this.handlers.containsKey( request.getClass().getName() ) )
        {
            return ( RequestHandler ) this.handlers.get( request.getClass().getName() );
        }

        Class[] interfaces = request.getClass().getInterfaces();
        for ( int ii = 0; ii < interfaces.length; ii++ )
        {
            if ( this.handlers.containsKey( interfaces[ii].getName() ) )
            {
                return ( RequestHandler ) this.handlers.get( interfaces[ii].getName() );
            }
        }

        String msg = "cannot find a handler for request: " + request;
        throw new IllegalArgumentException( msg );
    }


    /**
     * A snickers based BER Decoder factory.
     */
    private static final class DecoderFactoryImpl implements DecoderFactory
    {
        final Hashtable env;


        public DecoderFactoryImpl()
        {
            this.env = null;
        }


        DecoderFactoryImpl( Hashtable env )
        {
            this.env = env;
        }


        public StatefulDecoder createDecoder()
        {
            if ( env == null || env.get( Provider.BERLIB_PROVIDER ) == null )
            {
                return new MessageDecoder();
            }
            else
            {
                return new MessageDecoder( env );
            }
        }
    }


    /**
     * A snickers based BER Encoder factory.
     */
    private static final class EncoderFactoryImpl implements EncoderFactory
    {
        final Hashtable env;


        public EncoderFactoryImpl()
        {
            this.env = null;
        }


        public EncoderFactoryImpl( Hashtable env )
        {
            this.env = env;
        }


        public StatefulEncoder createEncoder()
        {
            if ( env == null || env.get( Provider.BERLIB_PROVIDER ) == null )
            {
                return new MessageEncoder();
            }
            else
            {
                return new MessageEncoder( env );
            }
        }
    }
}
