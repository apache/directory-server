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
package org.apache.eve.decoder.impl ;


import java.math.BigInteger;
import java.nio.ByteBuffer ;

import java.util.Map ;
import java.util.HashMap ;
import java.util.EventObject ;

import org.apache.eve.event.InputEvent ;
import org.apache.eve.event.Subscriber ;
import org.apache.eve.seda.StageConfig ;
import org.apache.eve.event.EventRouter ;
import org.apache.eve.seda.DefaultStage ;
import org.apache.eve.listener.ClientKey ;
import org.apache.eve.event.ConnectEvent ;
import org.apache.eve.decoder.NoOpDecoder ;
import org.apache.eve.decoder.ClientDecoder ;
import org.apache.eve.event.DisconnectEvent ;
import org.apache.eve.event.InputSubscriber ;
import org.apache.eve.decoder.DecoderManager ;
import org.apache.eve.event.RequestEvent;
import org.apache.eve.event.SubscriberMonitor ;
import org.apache.eve.event.ConnectSubscriber ;
import org.apache.eve.event.AbstractSubscriber ;
import org.apache.eve.event.DisconnectSubscriber ;
import org.apache.eve.decoder.DecoderManagerMonitor ;
import org.apache.eve.decoder.DecoderManagerMonitorAdapter ;
import org.apache.ldap.common.message.BindRequestImpl;

import org.apache.commons.codec.DecoderException ;
import org.apache.commons.codec.stateful.DecoderMonitor ;
import org.apache.commons.codec.stateful.DecoderCallback ;
import org.apache.commons.codec.stateful.StatefulDecoder ;
import org.apache.commons.codec.stateful.DecoderMonitorAdapter ;


/**
 * Default decoder managing component implementation.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultDecoderManager extends DefaultStage
    implements 
    DecoderManager,
    DecoderCallback, 
    InputSubscriber,
    ConnectSubscriber, 
    DisconnectSubscriber
{
    /** event router or bus this component subscribes and publishes events on */
    private final EventRouter router ;
    /** map of decoders for client keys */
    private final Map decoders = new HashMap() ;
    /** the monitor used for this decoder manager */
    private DecoderManagerMonitor monitor ;
    /** the monitor used for this subscriber */
    private SubscriberMonitor subscriberMonitor ;
    /** the monitor for this decoder */
    private DecoderMonitor decoderMonitor ;


    /**
     * Creates a instance of the default decoder manager implementation.
     * 
     * @param router the event bus or router component depended upon
     * @param bp the buffer this decoder depends upon
     */
    public DefaultDecoderManager( EventRouter router, StageConfig config )
    {
        super( config ) ;
        
        this.router = router ;
        this.monitor = new DecoderManagerMonitorAdapter() ;
        this.subscriberMonitor = new SubscriberMonitor()
        {
            public void failedOnInform( Subscriber subscriber, 
                                        EventObject eventObject,
										Throwable throwable ) 
            {
                if ( monitor == null )
                {    
                    return ;
                }
                
                monitor.failedOnInform( subscriber, eventObject, throwable ) ;
            }
        } ;
        
        this.decoderMonitor = new DecoderMonitorAdapter() ;

        router.subscribe( InputEvent.class, this ) ;
        router.subscribe( ConnectEvent.class, this ) ;
        router.subscribe( DisconnectEvent.class, this ) ;
    }
    
    
    // ------------------------------------------------------------------------
    // Subscriber Methods
    // ------------------------------------------------------------------------

    
    /**
     * Routes the event to the appropriate typed <code>inform()</code> method.
     * 
     * @see org.apache.eve.event.Subscriber#inform(java.util.EventObject)
     * @see org.apache.eve.event.AbstractSubscriber.inform( 
     * org.apache.eve.event.Subscriber, java.util.EventObject, 
     * org.apache.eve.event.SubscriberMonitor ) ;
     */
    public void inform( EventObject event )
    {
        AbstractSubscriber.inform( this, event, subscriberMonitor ) ;
    }

    
    /**
     * Enqueues the event onto this Stages event queue for processing. 
     * 
     * @see org.apache.eve.event.InputSubscriber#inform(
     * org.apache.eve.event.InputEvent)
     */
    public void inform( InputEvent event )
    {
        // claim interest and release after asynchronous processing of event
        ByteBuffer buffer = event.claimInterest( this ) ;
        enqueue( event ) ;
    }
    

    /**
     * Removes the clients decoder from the map of decoders.
     * 
     * @see org.apache.eve.event.DisconnectSubscriber#inform(
     * org.apache.eve.event.DisconnectEvent)
     */
    public void inform( DisconnectEvent event )
    {
        decoders.remove( event.getClientKey() ) ;
    }
    

    /**
     * We basically create a new client decoder and put it into a map for
     * use later when we are processing input events from the client.
     * 
     * @see org.apache.eve.event.ConnectSubscriber#inform(
     * org.apache.eve.event.ConnectEvent)
     */
    public void inform( ConnectEvent event )
    {
        ClientKey key = event.getClientKey() ;
        StatefulDecoder noop = new NoOpDecoder() ;
        StatefulDecoder decoder = new ClientDecoder( key, noop ) ; 
        decoders.put( key, decoder ) ;
    }
    
    
    /**
     * Here the decoder informs us that a unit of data is decoded.  In the case
     * of the snickers decoder we're using decoding an LDAP message envelope 
     * for a request.  We use this request to create a RequestEvent and publish
     * the event on the queue.
     * 
     * We are just hard coding the generation of a bind request for the moment.
     * 
     * @see org.apache.commons.codec.stateful.DecoderCallback#decodeOccurred(
     * org.apache.commons.codec.stateful.StatefulDecoder, java.lang.Object)
     */
    public void decodeOccurred( StatefulDecoder decoder, Object decoded )
    {
        /*
         * This is where the decoded object is really a request Message 
         * enveloper object which we just need to package as a RequestEvent
         * and publish on the EventRouter. 
         */
        BindRequestImpl bind = new BindRequestImpl( BigInteger.ONE ) ;
        bind.setCredentials( "password".getBytes() ) ;
        bind.setName( "uid=akarasulu,dc=example,dc=com" ) ;
        bind.setSimple( true ) ;
        bind.setVersion3( true ) ;
        
        ClientKey key = ( ( ClientDecoder ) decoder ).getClientKey() ;
        RequestEvent event = new RequestEvent( this, key, bind ) ;
    }
    

    // ------------------------------------------------------------------------
    // Service Interface Methods
    // ------------------------------------------------------------------------

    
    /* (non-Javadoc)
     * @see org.apache.eve.decoder.DecoderManager#setCallback(
     * org.apache.eve.listener.ClientKey, 
     * org.apache.commons.codec.stateful.DecoderCallback)
     */
    public void setCallback( ClientKey key, DecoderCallback cb )
    {
        StatefulDecoder decoder = ( StatefulDecoder ) decoders.get( key ) ;
        decoder.setCallback( cb ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.decoder.DecoderManager#setDecoderMonitor(
     * org.apache.eve.listener.ClientKey, 
     * org.apache.commons.codec.stateful.DecoderMonitor)
     */
    public void setDecoderMonitor( ClientKey key, DecoderMonitor monitor )
    {
        StatefulDecoder decoder = ( StatefulDecoder ) decoders.get( key ) ;
        decoder.setDecoderMonitor( monitor ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.decoder.DecoderManager#disable(
     * org.apache.eve.listener.ClientKey)
     */
    public boolean disable( ClientKey key )
    {
        StatefulDecoder decoder = ( StatefulDecoder ) decoders.remove( key ) ;
        return decoder != null ;
    }
    

    /* (non-Javadoc)
     * @see org.apache.eve.decoder.DecoderManager#decode(
     * org.apache.eve.listener.ClientKey, java.nio.ByteBuffer)
     */
    public void decode( ClientKey key, ByteBuffer buffer ) 
        throws DecoderException
    {
        StatefulDecoder decoder = ( StatefulDecoder ) decoders.remove( key ) ;
        decoder.decode( buffer ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.decoder.DecoderManager#decode(java.nio.ByteBuffer)
     */
    public Object decode( ByteBuffer buffer ) throws DecoderException
    {
        // replace this decoder with a real one later
        StatefulDecoder decoder = new NoOpDecoder() ;
        // used array to set a value on final variable and get by compiler
        final Object[] decoded = new Object[1] ;
        
        decoder.setCallback( new DecoderCallback() 
        {
            public void decodeOccurred( StatefulDecoder decoder, Object obj )
            {
                decoded[0] = obj ;
            }
        });
        
        // force synchronous callback 
        decoder.decode( buffer ) ;
        
        // the decoded value should be set
        if ( decoded[0] == null )
        {
            throw new DecoderException( "Expected a complete encoded unit of "
                    + "data but got a partial" ) ;
        }
        
        return decoded[0] ;
    }


    // ------------------------------------------------------------------------
    // Additional Methods
    // ------------------------------------------------------------------------

    
    /**
     * Gets the monitor for this DecoderManager.
     * 
     * @return the monitor
     */
    public DecoderManagerMonitor getMonitor()
    {
        return monitor ;
    }
    

    /**
     * @param monitor the monitor to set
     */
    public void setMonitor( DecoderManagerMonitor monitor )
    {
        this.monitor = monitor ;
    }


    /**
     * Gets a stateful decoder for a particular client.
     * 
     * @param key the client's key
     * @return the stateful decoder for the client
     */
    StatefulDecoder getDecoder( ClientKey key )
    {
        return ( StatefulDecoder ) decoders.get( key ) ;
    }
}
