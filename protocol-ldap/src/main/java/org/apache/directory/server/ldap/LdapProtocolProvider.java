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


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.ldap.support.*;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.shared.asn1.codec.Asn1CodecDecoder;
import org.apache.directory.shared.asn1.codec.Asn1CodecEncoder;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.message.*;
import org.apache.directory.shared.ldap.message.extended.NoticeOfDisconnect;
import org.apache.directory.shared.ldap.message.spi.BinaryAttributeDetector;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.SSLFilter;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.handler.demux.DemuxingIoHandler;
import org.apache.mina.util.SessionLog;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.Control;
import java.util.*;


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

    /** a set of supported controls */
    private Set<String> supportedControls;

    /** configuration for the LDAP protocol provider **/
    private LdapConfiguration ldapConfiguration;

    private DirectoryService directoryService;

    private AbandonHandler abandonHandler;
    private AddHandler addHandler;
    private BindHandler bindHandler;
    private CompareHandler compareHandler;
    private DeleteHandler deleteHandler;
    private ExtendedHandler extendedHandler;
    private ModifyHandler modifyHandler;
    private ModifyDnHandler modifyDnHandler;
    private SearchHandler searchHandler;
    private UnbindHandler unbindHandler;


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
     * underlying codec providers if any
     * @param cfg the ldap configuration
     * @param directoryService the directory service core
     *
     * @throws LdapNamingException if there are problems setting up the protocol provider
     */
    public LdapProtocolProvider( DirectoryService directoryService, LdapConfiguration cfg ) throws LdapNamingException
    {
        this.ldapConfiguration = cfg;
        this.directoryService = directoryService;

        Hashtable<String,Object> copy = new Hashtable<String,Object>();
        copy.put( Context.PROVIDER_URL, "" );
        copy.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.directory.server.core.jndi.CoreContextFactory" );
        copy.put( DirectoryService.JNDI_KEY, directoryService );

        SessionRegistry.releaseSingleton();
        new SessionRegistry( cfg, copy );

        this.supportedControls = new HashSet<String>();
        this.supportedControls.add( PersistentSearchControl.CONTROL_OID );
        this.supportedControls.add( EntryChangeControl.CONTROL_OID );
        this.supportedControls.add( SubentriesControl.CONTROL_OID );
        this.supportedControls.add( ManageDsaITControl.CONTROL_OID );
        this.supportedControls.add( CascadeControl.CONTROL_OID );

        setAbandonHandler( new DefaultAbandonHandler() );
        setAddHandler( new DefaultAddHandler() );
        setBindHandler( new DefaultBindHandler() );
        setCompareHandler( new DefaultCompareHandler() );
        setDeleteHandler( new DefaultDeleteHandler() );
        setExtendedHandler( new DefaultExtendedHandler() );
        setModifyHandler( new DefaultModifyHandler() );
        setModifyDnHandler( new DefaultModifyDnHandler() );
        setSearchHandler( new DefaultSearchHandler() );
        setUnbindHandler( new DefaultUnbindHandler() );

        this.codecFactory = new ProtocolCodecFactoryImpl( directoryService );
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
     *
     * @param eoh an extended operation handler
     */
    public void addExtendedOperationHandler( ExtendedOperationHandler eoh )
    {
        extendedHandler.addHandler( eoh );
    }


    /**
     * Deregisteres an {@link ExtendedOperationHandler} with the specified <tt>oid</tt>
     * from this protocol provider.
     *
     * @param oid the numeric identifier for the extended operation associated with
     * the handler to remove
     */
    public void removeExtendedOperationHandler( String oid )
    {
        extendedHandler.removeHandler( oid );
    }


    /**
     * Returns an {@link ExtendedOperationHandler} with the specified <tt>oid</tt>
     * which is registered to this protocol provider.
     *
     * @param oid the oid of the extended request of associated with the extended
     * request handler
     * @return the exnteded operation handler
     */
    public ExtendedOperationHandler getExtendedOperationHandler( String oid )
    {
        return extendedHandler.getHandler( oid );
    }


    /**
     * Returns a {@link Map} of all registered OID-{@link ExtendedOperationHandler}
     * pairs.
     *
     * @return map of all extended operation handlers
     */
    public Map<String,ExtendedOperationHandler> getExtendedOperationHandlerMap()
    {
        return extendedHandler.getHandlerMap();
    }


    public DirectoryService getDirectoryService()
    {
        return directoryService;
    }


    public void setDirectoryService( DirectoryService directoryService )
    {
        this.directoryService = directoryService;

        if ( bindHandler != null )
        {
            this.bindHandler.setDirectoryService( directoryService );
        }
    }


    public Set<String> getSupportedControls()
    {
        return supportedControls;
    }


    public void setSupportedControls( Set<String> supportedControls )
    {
        this.supportedControls = supportedControls;
    }


    public AbandonHandler getAbandonHandler()
    {
        return abandonHandler;
    }


    public void setAbandonHandler( AbandonHandler abandonHandler )
    {
        this.handler.removeMessageHandler( AbandonRequest.class );
        this.abandonHandler = abandonHandler;
        //noinspection unchecked
        this.handler.addMessageHandler( AbandonRequest.class, this.abandonHandler );
    }


    public AddHandler getAddHandler()
    {
        return addHandler;
    }


    public void setAddHandler( AddHandler addHandler )
    {
        this.handler.removeMessageHandler( AddRequest.class );
        this.addHandler = addHandler;
        //noinspection unchecked
        this.handler.addMessageHandler( AddRequest.class, this.addHandler );
    }


    public BindHandler getBindHandler()
    {
        return bindHandler;
    }


    public void setBindHandler( BindHandler bindHandler )
    {
        this.handler.removeMessageHandler( BindRequest.class );
        this.bindHandler = bindHandler;
        if ( directoryService != null )
        {
            this.bindHandler.setDirectoryService( directoryService );
        }
        //noinspection unchecked
        this.handler.addMessageHandler( BindRequest.class, this.bindHandler );
    }


    public CompareHandler getCompareHandler()
    {
        return compareHandler;
    }


    public void setCompareHandler( CompareHandler compareHandler )
    {
        this.handler.removeMessageHandler( CompareRequest.class );
        this.compareHandler = compareHandler;
        //noinspection unchecked
        this.handler.addMessageHandler( CompareRequest.class, this.compareHandler );
    }


    public DeleteHandler getDeleteHandler()
    {
        return deleteHandler;
    }


    public void setDeleteHandler( DeleteHandler deleteHandler )
    {
        this.handler.removeMessageHandler( DeleteRequest.class );
        this.deleteHandler = deleteHandler;
        //noinspection unchecked
        this.handler.addMessageHandler( DeleteRequest.class, this.deleteHandler );
    }


    public ExtendedHandler getExtendedHandler()
    {
        return extendedHandler;
    }


    public void setExtendedHandler( ExtendedHandler extendedHandler )
    {
        this.handler.removeMessageHandler( ExtendedRequest.class );
        this.extendedHandler = extendedHandler;
        //noinspection unchecked
        this.handler.addMessageHandler( ExtendedRequest.class, this.extendedHandler );
    }


    public ModifyHandler getModifyHandler()
    {
        return modifyHandler;
    }


    public void setModifyHandler( ModifyHandler modifyHandler )
    {
        this.handler.removeMessageHandler( ModifyRequest.class );
        this.modifyHandler = modifyHandler;
        //noinspection unchecked
        this.handler.addMessageHandler( ModifyRequest.class, this.modifyHandler );
    }


    public ModifyDnHandler getModifyDnHandler()
    {
        return modifyDnHandler;
    }


    public void setModifyDnHandler( ModifyDnHandler modifyDnHandler )
    {
        this.handler.removeMessageHandler( ModifyDnRequest.class );
        this.modifyDnHandler = modifyDnHandler;
        //noinspection unchecked
        this.handler.addMessageHandler( ModifyDnRequest.class, this.modifyDnHandler );
    }


    public SearchHandler getSearchHandler()
    {
        return searchHandler;
    }


    public void setSearchHandler( SearchHandler searchHandler )
    {
        this.handler.removeMessageHandler( SearchRequest.class );
        this.searchHandler = searchHandler;
        //noinspection unchecked
        this.handler.addMessageHandler( SearchRequest.class, this.searchHandler );
    }


    public UnbindHandler getUnbindHandler()
    {
        return unbindHandler;
    }


    public void setUnbindHandler( UnbindHandler unbindHandler )
    {
        this.handler.removeMessageHandler( UnbindRequest.class );
        this.unbindHandler = unbindHandler;
        //noinspection unchecked
        this.handler.addMessageHandler( UnbindRequest.class, this.unbindHandler );
    }


    /**
     * A snickers based BER Decoder factory.
     */
    private static final class ProtocolCodecFactoryImpl implements ProtocolCodecFactory
    {
        final DirectoryService directoryService;


        public ProtocolCodecFactoryImpl( DirectoryService directoryService )
        {
            this.directoryService = directoryService;
        }


        public ProtocolEncoder getEncoder()
        {
            return new Asn1CodecEncoder( new MessageEncoder() );
        }


        public ProtocolDecoder getDecoder()
        {
            return new Asn1CodecDecoder( new MessageDecoder( new BinaryAttributeDetector()
            {
                public boolean isBinary( String id )
                {
                    AttributeTypeRegistry attrRegistry = directoryService.getRegistries().getAttributeTypeRegistry();
                    try
                    {
                        AttributeType type = attrRegistry.lookup( id );
                        return ! type.getSyntax().isHumanReadable();
                    }
                    catch ( NamingException e )
                    {
                        return false;
                    }
                }
            }) );
        }
    }

    private class LdapProtocolHandler extends DemuxingIoHandler
    {
        public void sessionCreated( IoSession session ) throws Exception
        {
            session.setAttribute( LdapConfiguration.class.toString(), ldapConfiguration );
            IoFilterChain filters = session.getFilterChain();
            filters.addLast( "codec", new ProtocolCodecFilter( codecFactory ) );
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

            if ( message == SSLFilter.SESSION_SECURED )
            {
                ExtendedRequest req = new ExtendedRequestImpl( 0 );
                req.setOid( "1.3.6.1.4.1.1466.20037" );
                req.setPayload( "SECURED".getBytes( "ISO-8859-1" ) );
                message = req;
            }
            else if ( message == SSLFilter.SESSION_UNSECURED )
            {
                ExtendedRequest req = new ExtendedRequestImpl( 0 );
                req.setOid( "1.3.6.1.4.1.1466.20037" );
                req.setPayload( "UNSECURED".getBytes( "ISO-8859-1" ) );
                message = req;
            }

            if ( ( ( Request ) message ).getControls().size() > 0 && message instanceof ResultResponseRequest )
            {
                ResultResponseRequest req = ( ResultResponseRequest ) message;
                for ( Control control1 : req.getControls().values() )
                {
                    MutableControl control = ( MutableControl ) control1;
                    if ( control.isCritical() && !supportedControls.contains( control.getID() ) )
                    {
                        ResultResponse resp = req.getResultResponse();
                        resp.getLdapResult().setErrorMessage( "Unsupport critical control: " + control.getID() );
                        resp.getLdapResult().setResultCode( ResultCodeEnum.UNAVAILABLE_CRITICAL_EXTENSION );
                        session.write( resp );
                        return;
                    }
                }
            }

            super.messageReceived( session, message );
        }


        public void exceptionCaught( IoSession session, Throwable cause )
        {
            if ( cause.getCause() instanceof ResponseCarryingMessageException )
            {
                ResponseCarryingMessageException rcme = ( ResponseCarryingMessageException ) cause.getCause();
                session.write( rcme.getResponse() );
                return;
            }
            
            SessionLog.warn( session,
                "Unexpected exception forcing session to close: sending disconnect notice to client.", cause );
            session.write( NoticeOfDisconnect.PROTOCOLERROR );
            SessionRegistry.getSingleton().remove( session );
            session.close();
        }
    }
}
