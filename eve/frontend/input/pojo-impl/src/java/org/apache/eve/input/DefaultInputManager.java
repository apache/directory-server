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
package org.apache.eve.input ;


import java.util.Iterator ;
import java.util.ArrayList ;

import java.io.IOException ;
import java.nio.ByteBuffer ;
import java.nio.channels.Selector ;
import java.nio.channels.SelectionKey ;
import java.nio.channels.SocketChannel ;

import org.apache.eve.event.InputEvent ;
import org.apache.eve.ResourceException ;
import org.apache.eve.buffer.BufferPool ;
import org.apache.eve.event.EventRouter ;
import org.apache.eve.listener.ClientKey ;
import org.apache.eve.event.ConnectEvent ;
import org.apache.eve.event.DisconnectEvent ;
import org.apache.eve.event.ConnectSubscriber ;
import org.apache.eve.event.AbstractSubscriber ;
import org.apache.eve.event.DisconnectSubscriber ;
import org.apache.eve.listener.KeyExpiryException ;


/**
 * Default InputManager implementation based on NIO selectors and channels.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev: 1452 $
 */
public class DefaultInputManager extends AbstractSubscriber
    implements InputManager, ConnectSubscriber, DisconnectSubscriber
{
    /** the thread driving this Runnable */ 
    private Thread m_thread = null ;
    /** parameter used to politely stop running thread */
    private Boolean m_hasStarted = null ;
    /** the buffer pool we get direct buffers from */
    private BufferPool m_bp = null ;
    /** event router used to decouple source to sink relationships */
    private EventRouter m_router = null ;
    /** selector used to select a ready socket channel */
    private Selector m_selector = null ;
    /** contains the batch of new connect events and channels to register */
    private final ArrayList m_connectEvents = new ArrayList() ;
    /** contains the batch of disconnect events & selection keys to cancel */
    private final ArrayList m_disconnectEvents = new ArrayList() ;
    /** the input manager's monitor */
    private InputManagerMonitor m_monitor = new InputManagerMonitorAdapter() ;

    
    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------
    
    
    /**
     * Creates a default InputManager implementation
     *  
     * @param a_router an event router service
     * @param a_bp a buffer pool service
     */
    public DefaultInputManager( EventRouter a_router, BufferPool a_bp )
        throws IOException
    {
        m_bp = a_bp ;
        m_hasStarted = new Boolean( false ) ;
        m_selector = Selector.open() ;

        m_router = a_router ;
        m_router.subscribe( ConnectEvent.class, null, this ) ;
        m_router.subscribe( DisconnectEvent.class, null, this ) ;
    }
    

    // ------------------------------------------------------------------------
    // start, stop and runnable code
    // ------------------------------------------------------------------------
    
    
    /**
     * Runnable used to drive the selection loop. 
     *
     * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
     * @author $Author: akarasulu $
     * @version $Revision$
     */
    class SelectionDriver implements Runnable
    {
        public void run()
        {
            while ( m_hasStarted.booleanValue() ) 
            {
                int l_count = 0 ;
                
                /*
                 * check if we have input waiting and continue if there is
                 * nothing to read from any of the registered channels  
                 */
                try
                {
                    m_monitor.enteringSelect( m_selector ) ;
                    maintainConnections() ;
                    
                    if ( 0 == ( l_count = m_selector.select() ) )
                    {
                        /*
                         * Loop here solves a bug where some lingering phantom
                         * clients (long gone) have sockets and channels that 
                         * appear valid and connected.  These sockets and 
                         * channels are cleaned up here in this loop.  Keep in
                         * mind they have triggered a wakeup on select() and 
                         * are appear ready for reads yet were not selected w/i
                         * the last iteration since the selected count was zero.
                         * 
                         * For more information on this you can refer to the 
                         * Jira Issue: 
                 http://nagoya.apache.org/jira/secure/ViewIssue.jspa?key=DIR-18
                         */
                        Iterator l_list = m_selector.selectedKeys().iterator() ;
                        while( l_list.hasNext() )
                        {
                            SelectionKey l_key = ( SelectionKey ) 
                                l_list.next() ;
                            l_key.channel().close() ;
                            l_key.cancel() ;
                            l_list.remove() ;
                        }
                        
                        m_monitor.selectTimedOut( m_selector ) ;
                        continue ;
                    }
                } 
                catch( IOException e )
                {
                    m_monitor.selectFailure( m_selector, e ) ;
                    continue ;
                }
                
                processInput() ;
            }
        }
    }


    /**
     * Starts up this module.
     */
    public void start() 
    {
        synchronized( m_hasStarted )
        {
            if ( m_hasStarted.booleanValue() )
            {
                throw new IllegalStateException( "Already started!" ) ;
            }
            
            m_hasStarted = new Boolean( true ) ;
            m_thread = new Thread( new SelectionDriver() ) ;
            m_thread.start() ;
        }
    }
    
    
    /**
     * Blocks calling thread until this module gracefully stops.
     */
    public void stop() throws InterruptedException
    {
        synchronized( m_hasStarted )
        {
            m_hasStarted = new Boolean( false ) ;
            m_selector.wakeup() ;
            
            while ( m_thread.isAlive() )
            {
                Thread.sleep( 100 ) ;
            }
        }
    }
    
    
    // ------------------------------------------------------------------------
    // subscriber methods
    // ------------------------------------------------------------------------
    
    
    /**
     * @see org.apache.eve.event.ConnectListener#
     * connectPerformed(org.apache.eve.event.ConnectEvent)
     */
    public void inform( ConnectEvent an_event )
    {
        synchronized ( m_connectEvents )
        {
            m_connectEvents.add( an_event ) ;
        }
        
        m_selector.wakeup() ;
    }

    
    /**
     * @see org.apache.eve.event.DisconnectListener#
     * inform(org.apache.eve.event.DisconnectEvent)
     */
    public void inform( DisconnectEvent an_event )
    {
        synchronized ( m_disconnectEvents )
        {
            m_connectEvents.add( an_event ) ;
        }
        
        m_selector.wakeup() ;
    }
    

    // ------------------------------------------------------------------------
    // private utilities
    // ------------------------------------------------------------------------
    
    
    /**
     * Maintains connections by registering newly established connections within
     * ConnectEvents and cancelling the selection keys of dropped connections.
     * 
     * @see created in response to a <a href=
     * "http://nagoya.apache.org/jira/secure/ViewIssue.jspa?id=13574">JIRA Issue
     * </a>
     */
    private void maintainConnections()
    {
        /* Register New Connections 
         * ========================
         * 
         * Here we perform a synchronized transfer of newly arrived events 
         * which are batched in the list of ConnectEvents.  This is done to
         * minimize the chances of contention.  Next we cycle through each
         * event registering the new connection's channel with the selector.
         */
        
        // copy all events into a separate list first and clear
        ConnectEvent[] l_connectEvents = null ;
        synchronized( m_connectEvents ) 
        {
            l_connectEvents = new ConnectEvent[m_connectEvents.size()] ;
            l_connectEvents = ( ConnectEvent[] ) 
                m_connectEvents.toArray( l_connectEvents ) ;
            m_connectEvents.clear() ;
        }

        // cycle through connections and register them with the selector
        for ( int ii = 0; ii < l_connectEvents.length ; ii++ )
        {    
            ClientKey l_key = null ;
            SocketChannel l_channel = null ;
            
            try
            {
                l_key = l_connectEvents[ii].getClientKey() ;
                l_channel = l_key.getSocket().getChannel() ;
                
                // hands-off blocking sockets!
                if ( null == l_channel )
                {
                    continue ;
                }
                
                l_channel.configureBlocking( false ) ;
                l_channel.register( m_selector, SelectionKey.OP_READ, l_key ) ;
                m_monitor.registeredChannel( l_key, m_selector ) ;
            }
            catch ( KeyExpiryException e )
            {
                m_monitor.keyExpiryFailure( l_key, e ) ;
            }
            catch ( IOException e )
            {
                m_monitor.channelRegistrationFailure( m_selector, l_channel, 
                        SelectionKey.OP_READ, e ) ;
            }
        }
        
        
        /* Cancel/Unregister Dropped Connections 
         * =====================================
         *
         * To do this we simply cancel the selection key for the client the
         * disconnect event is associated with.  
         */
        
        // copy all events into a separate list first and clear
        DisconnectEvent[] l_disconnectEvents = null ;
        synchronized( m_disconnectEvents ) 
        {
            l_disconnectEvents = new DisconnectEvent[m_disconnectEvents.size()] ;
            l_disconnectEvents = ( DisconnectEvent[] ) 
                m_disconnectEvents.toArray( l_disconnectEvents ) ;
            m_disconnectEvents.clear() ;
        }

        SelectionKey l_key = null ;
        for ( int ii = 0; ii < l_disconnectEvents.length; ii++ )
        {
            Iterator l_keys = m_selector.keys().iterator() ;
            ClientKey l_clientKey = l_disconnectEvents[ii].getClientKey() ;

            while ( l_keys.hasNext() )
            {
                l_key = ( SelectionKey ) l_keys.next() ;
                if ( l_key.attachment().equals( l_clientKey ) )
                {
                    break ;
                }
            }

            if ( null == l_key )
            {
                return ;
            }
            
            try
            {
                l_key.channel().close() ;
            }
            catch ( IOException e )
            {
                m_monitor.channelCloseFailure( 
                        ( SocketChannel ) l_key.channel(), e ) ;
            }
        
            l_key.cancel() ;
            m_monitor.disconnectedClient( l_clientKey ) ;
        }
    }
    
    
    /**
     * Processes input on channels of the read ready selected keys.
     */
    private void processInput()
    {
        /*
         * Process the selectors that are ready.  For each selector that
         * is ready we read some data into a buffer we claim from a buffer
         * pool.  Next we create an InputEvent using the buffer and publish
         * it using the event notifier/router.
         */
        Iterator l_list = m_selector.selectedKeys().iterator() ;
        while ( l_list.hasNext() )
        {
            SelectionKey l_key = ( SelectionKey ) l_list.next() ;
            ClientKey l_client = ( ClientKey ) l_key.attachment() ; 
                
            if ( l_key.isReadable() )
            {
                ByteBuffer l_buf = null ;
                SocketChannel l_channel = ( SocketChannel ) l_key.channel() ;
                
                /*
                 * claim a buffer, read from channel into it and remove 
                 * the current selection key from selected set
                 */ 
                try
                {
                    l_buf = m_bp.getBuffer( this ) ;
                    int l_count = 0 ;
                    
                    if ( ( l_count = l_channel.read( l_buf ) ) == -1 )
                    {
                        l_channel.socket().close() ;
                        l_channel.close() ;
                        l_key.cancel() ;
                        return ;
                    }
                    
                    l_buf.flip() ;
                    m_monitor.inputRecieved( 
                            l_buf.asReadOnlyBuffer(), l_client ) ;
                    l_list.remove() ;
                }
                catch ( ResourceException e )
                {
                    m_monitor.bufferUnavailable( m_bp, e ) ;
                    continue ;
                }
                catch ( IOException e )
                {
                    m_monitor.readFailed( l_client, e ) ;
                    m_bp.releaseClaim( l_buf, this ) ;
                    continue ;
                }
                    
                /*
                 * Monitor input and create the event publishing it.  Note that
                 * releasing claim to the buffer does not free it.  It just 
                 * removes this object as an interested party from the list
                 * of interested parties.  After synchronously publishing the 
                 * input event there are other interesed parties that have 
                 * increased the reference count.
                 */ 
                m_monitor.inputRecieved( l_client ) ;
                InputEvent l_event = new ConcreteInputEvent( l_client, l_buf ) ;
                m_router.publish( l_event ) ;
                m_bp.releaseClaim( l_buf, this ) ;
            }
        }
    }
    
    
    /**
     * A concrete InputEvent that uses the buffer pool to properly implement
     * the interest claim and release methods.
     *
     * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
     * @author $Author: akarasulu $
     * @version $Revision$
     */
    class ConcreteInputEvent extends InputEvent
    {
        ConcreteInputEvent( ClientKey a_key, ByteBuffer a_buffer )
        {
            super( DefaultInputManager.this, a_key, a_buffer ) ;
        }
        
        public ByteBuffer claimInterest( Object a_party )
        {
            m_bp.claimInterest( getBuffer(), a_party ) ;
            return getBuffer().asReadOnlyBuffer() ;
        }
        
        public void releaseInterest( Object a_party )
        {
            m_bp.releaseClaim( getBuffer(), a_party ) ;
        }
    }
    
    
    /**
     * Gets the monitor associated with this InputManager.
     * 
     * @return returns the monitor
     */
    public InputManagerMonitor getMonitor()
    {
        return m_monitor ;
    }

    
    /**
     * Sets the monitor associated with this InputManager.
     * 
     * @param a_monitor the monitor to set
     */
    public void setMonitor( InputManagerMonitor a_monitor )
    {
        m_monitor = a_monitor ;
    }
}
