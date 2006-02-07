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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.naming.Context;

import org.apache.mina.filter.codec.asn1.Asn1CodecDecoder;
import org.apache.mina.filter.codec.asn1.Asn1CodecEncoder;
import org.apache.ldap.common.exception.LdapNamingException;
import org.apache.ldap.common.message.AbandonRequest;
import org.apache.ldap.common.message.AbandonRequestImpl;
import org.apache.ldap.common.message.AddRequest;
import org.apache.ldap.common.message.AddRequestImpl;
import org.apache.ldap.common.message.BindRequest;
import org.apache.ldap.common.message.BindRequestImpl;
import org.apache.ldap.common.message.CompareRequest;
import org.apache.ldap.common.message.CompareRequestImpl;
import org.apache.ldap.common.message.Control;
import org.apache.ldap.common.message.DeleteRequest;
import org.apache.ldap.common.message.DeleteRequestImpl;
import org.apache.ldap.common.message.EntryChangeControl;
import org.apache.ldap.common.message.ExtendedRequest;
import org.apache.ldap.common.message.ExtendedRequestImpl;
import org.apache.ldap.common.message.ManageDsaITControl;
import org.apache.ldap.common.message.MessageDecoder;
import org.apache.ldap.common.message.MessageEncoder;
import org.apache.ldap.common.message.ModifyDnRequest;
import org.apache.ldap.common.message.ModifyDnRequestImpl;
import org.apache.ldap.common.message.ModifyRequest;
import org.apache.ldap.common.message.ModifyRequestImpl;
import org.apache.ldap.common.message.PersistentSearchControl;
import org.apache.ldap.common.message.Request;
import org.apache.ldap.common.message.ResultCodeEnum;
import org.apache.ldap.common.message.ResultResponse;
import org.apache.ldap.common.message.ResultResponseRequest;
import org.apache.ldap.common.message.SearchRequest;
import org.apache.ldap.common.message.SearchRequestImpl;
import org.apache.ldap.common.message.UnbindRequest;
import org.apache.ldap.common.message.UnbindRequestImpl;
import org.apache.ldap.common.message.extended.NoticeOfDisconnect;
import org.apache.ldap.common.message.spi.Provider;
import org.apache.ldap.server.protocol.support.AbandonHandler;
import org.apache.ldap.server.protocol.support.AddHandler;
import org.apache.ldap.server.protocol.support.BindHandler;
import org.apache.ldap.server.protocol.support.CompareHandler;
import org.apache.ldap.server.protocol.support.DeleteHandler;
import org.apache.ldap.server.protocol.support.ExtendedHandler;
import org.apache.ldap.server.protocol.support.ModifyDnHandler;
import org.apache.ldap.server.protocol.support.ModifyHandler;
import org.apache.ldap.server.protocol.support.SearchHandler;
import org.apache.ldap.server.protocol.support.UnbindHandler;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.LoggingFilter;
import org.apache.mina.filter.SSLFilter;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.handler.demux.DemuxingIoHandler;
import org.apache.mina.handler.demux.MessageHandler;
import org.apache.mina.util.SessionLog;


/**
 * An LDAP protocol provider implementation which dynamically associates
 * handlers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class LdapProtocolProvider
{
    /** the constant service name of this ldap protocol provider **/
    public static final String SERVICE_NAME = "ldap";
    /** a map of the default request object class name to the handler class name */
    private static final Map DEFAULT_HANDLERS;
    /** a set of supported controls */
    private static final Set SUPPORTED_CONTROLS;
    
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
        
        HashSet set = new HashSet();
        set.add( PersistentSearchControl.CONTROL_OID );  
        set.add( EntryChangeControl.CONTROL_OID ); 
        set.add( ManageDsaITControl.CONTROL_OID );
        SUPPORTED_CONTROLS = Collections.unmodifiableSet( set );
    }

    /** the underlying provider codec factory */
    private final ProtocolCodecFactory codecFactory;

    /** the MINA protocol handler */
    private final LdapProtocolHandler handler = new LdapProtocolHandler();

    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    /**
     * Creates a MINA LDAP protocol provider.
     *
     * @param env environment properties used to configure the provider and
     * underlying codec providers if any
     */
    public LdapProtocolProvider( Hashtable env ) throws LdapNamingException
    {
        Hashtable copy = ( Hashtable ) env.clone();
        copy.put( Context.PROVIDER_URL, "" );
        SessionRegistry.releaseSingleton();
        new SessionRegistry( copy );

        Iterator requestTypes = DEFAULT_HANDLERS.keySet().iterator();
        while( requestTypes.hasNext() )
        {
            MessageHandler handler = null;
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
                Class typeClass = Class.forName( type );
                handler = ( MessageHandler ) clazz.newInstance();
                this.handler.addMessageHandler( typeClass, handler );
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
        }

        this.codecFactory = new ProtocolCodecFactoryImpl( copy );
    }

    /**
     * Creates a MINA LDAP protocol provider.
     */
    public LdapProtocolProvider() throws LdapNamingException
    {
        SessionRegistry.releaseSingleton();
        new SessionRegistry( null );

        Iterator requestTypes = DEFAULT_HANDLERS.keySet().iterator();
        while( requestTypes.hasNext() )
        {
            MessageHandler handler = null;
            String type = ( String ) requestTypes.next();
            Class clazz = null;

            clazz = ( Class ) DEFAULT_HANDLERS.get( type );

            try
            {
                Class typeClass = Class.forName( type );
                handler = ( MessageHandler ) clazz.newInstance();
                this.handler.addMessageHandler( typeClass, handler );
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

    public IoHandler getHandler()
    {
        return handler;
    }
    
    /**
     * Registeres the specified {@link ExtendedOperationHandler} to this
     * protocol provider to provide a specific LDAP extended operation.
     */
    public void addExtendedOperationHandler( ExtendedOperationHandler eoh )
    {
        ExtendedHandler eh = ( ExtendedHandler ) handler.getMessageHandler( ExtendedRequest.class );
        eh.addHandler( eoh );
        eh = ( ExtendedHandler ) handler.getMessageHandler( ExtendedRequestImpl.class );
        eh.addHandler( eoh );
    }
    
    /**
     * Deregisteres an {@link ExtendedOperationHandler} with the specified <tt>oid</tt>
     * from this protocol provider.
     */
    public void removeExtendedOperationHandler( String oid )
    {
        ExtendedHandler eh = ( ExtendedHandler ) handler.getMessageHandler( ExtendedRequest.class );
        eh.removeHandler( oid );
        eh = ( ExtendedHandler ) handler.getMessageHandler( ExtendedRequestImpl.class );
        eh.removeHandler( oid );
    }
    
    /**
     * Returns an {@link ExtendedOperationHandler} with the specified <tt>oid</tt>
     * which is registered to this protocol provider.
     */
    public ExtendedOperationHandler getExtendedOperationHandler( String oid )
    {
        ExtendedHandler eh = ( ExtendedHandler ) handler.getMessageHandler( ExtendedRequest.class );
        return eh.getHandler( oid );
    }
    
    /**
     * Returns a {@link Map} of all registered OID-{@link ExtendedOperationHandler}
     * pairs.
     */
    public Map getExtendedOperationHandlerMap()
    {
        ExtendedHandler eh = ( ExtendedHandler ) handler.getMessageHandler( ExtendedRequest.class );
        return eh.getHandlerMap();
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

        public ProtocolEncoder getEncoder()
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

        public ProtocolDecoder getDecoder()
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

    private class LdapProtocolHandler extends DemuxingIoHandler
    {
        public void sessionCreated( IoSession session ) throws Exception
        {
            IoFilterChain filters = session.getFilterChain();
            filters.addLast( "codec", new ProtocolCodecFilter( codecFactory ) );
            filters.addLast( "logger", new LoggingFilter() );
        }
        
        public void sessionClosed( IoSession session )
        {
            SessionRegistry.getSingleton().remove( session );
        }

        public void messageReceived( IoSession session, Object message ) throws Exception
        {
            // Translate SSLFilter messages into LDAP extended request
            // defined in RFC #2830, 'Lightweight Directory Access Protocol (v3):
            // Extension for Transport Layer Security'.
            // 
            // The RFC specifies the payload should be empty, but we use
            // it to notify the TLS state changes.  This hack should be
            // OK from the viewpoint of security because StartTLS
            // handler should react to only SESSION_UNSECURED message
            // and degrade authentication level to 'anonymous' as specified
            // in the RFC, and this is no threat.
            
            if( message == SSLFilter.SESSION_SECURED )
            {
                ExtendedRequest req = new ExtendedRequestImpl( 0 );
                req.setOid( "1.3.6.1.4.1.1466.20037" );
                req.setPayload( "SECURED".getBytes( "ISO-8859-1" ) );
                message = req;
            }
            else if( message == SSLFilter.SESSION_UNSECURED )
            {
                ExtendedRequest req = new ExtendedRequestImpl( 0 );
                req.setOid( "1.3.6.1.4.1.1466.20037" );
                req.setPayload( "UNSECURED".getBytes( "ISO-8859-1" ) );
                message = req;
            }
            
            if ( ( ( Request ) message ).getControls().size() > 0 && message instanceof ResultResponseRequest )
            {
                ResultResponseRequest req = ( ResultResponseRequest ) message;
                Iterator controls = req.getControls().values().iterator();
                while ( controls.hasNext() )
                {
                    Control control = ( Control ) controls.next();
                    if ( control.isCritical() && ! SUPPORTED_CONTROLS.contains( control.getID() ) )
                    {
                        ResultResponse resp = req.getResultResponse();
                        resp.getLdapResult().setErrorMessage( "Unsupport critical control: " + control.getID() );
                        resp.getLdapResult().setResultCode( ResultCodeEnum.UNAVAILABLECRITICALEXTENSION );
                        session.write( resp );
                        return;
                    }
                }
            }
            
            super.messageReceived( session, message );
        }
        

        public void exceptionCaught( IoSession session, Throwable cause )
        {
            SessionLog.warn( session, 
                "Unexpected exception forcing session to close: sending disconnect notice to client.", cause );
            session.write( NoticeOfDisconnect.PROTOCOLERROR );
            SessionRegistry.getSingleton().remove( session );
            session.close();
        }
    }
}
