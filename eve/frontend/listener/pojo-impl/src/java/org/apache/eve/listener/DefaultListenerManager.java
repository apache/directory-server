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
package org.apache.eve.listener ;


import java.util.Set ;
import java.util.HashSet ;
import java.util.Iterator ;
import java.util.EventObject ;

import java.io.IOException ;
import java.net.InetAddress ;
import java.net.InetSocketAddress ;
import java.net.Socket;

import java.nio.channels.Selector ;
import java.nio.channels.SelectionKey ;
import java.nio.channels.SocketChannel ;
import java.nio.channels.ServerSocketChannel ;

import org.apache.eve.event.EventRouter ;
import org.apache.eve.event.ConnectEvent ;
import org.apache.eve.event.DisconnectEvent ;
import org.apache.eve.event.DisconnectSubscriber ;


/**
 * A listener manager that uses non-blocking NIO based constructs to detect
 * client connections on server socket listeners.
 * 
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultListenerManager 
    implements
    DisconnectSubscriber,
    ListenerManager,
    Runnable
{
    /** event manager used to decouple source to sink relationships */
    private final EventRouter m_router ;
    /** selector used to select a acceptable socket channel */
    private final Selector m_selector ;
    /** the client keys for accepted connections */
    private final Set m_clients ; 
    
    /** the thread driving this Runnable */ 
    private Thread m_thread = null ;
    /** parameter used to politely stop running thread */
    private Boolean m_hasStarted = null ;
    /** the listner manager's monitor */
    private ListenerManagerMonitor m_monitor = 
        new ListenerManagerMonitorAdapter() ;

    
    /**
     * Creates a default listener manager using an event router.
     * 
     * @param a_router the router to publish events to
     * @throws IOException
     */
    public DefaultListenerManager( EventRouter a_router ) throws IOException
    {
        m_clients = new HashSet() ;
        m_selector = Selector.open() ;
        m_hasStarted = new Boolean( false ) ;

        m_router = a_router ;
        m_router.subscribe( DisconnectEvent.class, null, this ) ;
    }
    
    
    /**
     * Gets the monitor.
     * 
     * @return Returns the monitor.
     */
    public ListenerManagerMonitor getMonitor()
    {
        return m_monitor ;
    }

    
    /**
     * Sets the monitor.
     * 
     * @param a_monitor The monitor to set.
     */
    public void setMonitor( ListenerManagerMonitor a_monitor )
    {
        m_monitor = a_monitor ;
    }


    /**
     * @see org.apache.eve.listener.ListenerManager#register(org.apache.eve.
     * listener.ServerListener)
     */
    public void bind( ServerListener a_listener ) throws IOException
    {
        try
        {
            ServerSocketChannel l_channel = ServerSocketChannel.open() ;
            InetSocketAddress l_address = new InetSocketAddress( 
                    InetAddress.getByAddress( a_listener.getAddress() ), 
                    a_listener.getPort() ) ;
            l_channel.socket().bind( l_address, a_listener.getBacklog() ) ;
            l_channel.configureBlocking( false ) ;
            m_selector.wakeup() ;
            l_channel.register( m_selector, SelectionKey.OP_ACCEPT, 
                    a_listener ) ;
        }
        catch ( IOException e )
        {
            m_monitor.failedToBind( a_listener, e ) ;
            throw e ;
        }
        
        m_monitor.bindOccured( a_listener ) ;
    }
    
    
    /**
     * @see org.apache.eve.listener.ListenerManager#unregister(org.apache.eve.
     * listener.ServerListener)
     */
    public void unbind( ServerListener a_listener ) throws IOException
    {
        SelectionKey l_key = null ;
        Iterator l_keys = m_selector.keys().iterator() ;
        
        while ( l_keys.hasNext() )
        {
            l_key = ( SelectionKey ) l_keys.next() ;
            if ( l_key.attachment().equals( a_listener ) )
            {
                break ;
            }
        }

        try
        {
            l_key.channel().close() ;
        }
        catch ( IOException e )
        {
            m_monitor.failedToUnbind( a_listener, e ) ;
            throw e ;
        }
        
        l_key.cancel() ;
        m_monitor.unbindOccured( a_listener ) ;
    }
    
    
    // ------------------------------------------------------------------------
    // DisconnectSubscriber Implementation
    // ------------------------------------------------------------------------
    
    
    /**
     * Disconnects a client by removing the clientKey from the listener.
     * 
     * @param an_event the disconnect event
     */
    public void inform( DisconnectEvent an_event )
    {
        m_clients.remove( an_event.getClientKey() ) ;
        
        try
        {
            an_event.getClientKey().expire() ;
        }
        catch ( IOException e ) 
        {
            m_monitor.failedToExpire( an_event.getClientKey(), e ) ;
        }
    }
    
    
    /*
     *  (non-Javadoc)
     * @see org.apache.eve.event.Subscriber#inform(java.util.EventObject)
     */
    public void inform( EventObject an_event )
    {
        inform( ( DisconnectEvent ) an_event ) ;
    }
    
    
    // ------------------------------------------------------------------------
    // Runnable implementation and start/stop controls
    // ------------------------------------------------------------------------
    
    
    /**
     * @see java.lang.Runnable#run()
     */
    public void run()
    {
        while ( m_hasStarted.booleanValue() ) 
        {
            int l_count = 0 ;
            
            try
            {
                m_monitor.enteringSelect( m_selector ) ;
                if ( 0 == ( l_count = m_selector.select() ) )
                {
                    m_monitor.selectTimedOut( m_selector ) ;
                    continue ;
                }
            } 
            catch( IOException e )
            {
                m_monitor.failedToSelect( m_selector, e ) ;
                continue ;
            }
            
            
            Iterator l_list = m_selector.selectedKeys().iterator() ;
            while ( l_list.hasNext() )
            {
                SelectionKey l_key = ( SelectionKey ) l_list.next() ;
                
                if ( l_key.isAcceptable() )
                {
                    SocketChannel l_channel = null ;
                    ServerSocketChannel l_server = ( ServerSocketChannel )
                        l_key.channel() ;
                    
                    try
                    {
                        l_channel = l_server.accept() ;
                        l_list.remove() ;
                        m_monitor.acceptOccured( l_key ) ;
                    }
                    catch ( IOException e )
                    {
                        m_monitor.failedToAccept( l_key, e ) ;
                        continue ;
                    }
                    
                    ClientKey l_clientKey = 
                        new ClientKey( l_channel.socket() ) ;
                    ConnectEvent l_event = 
                        new ConnectEvent( this, l_clientKey ) ;
                    m_router.publish( l_event ) ;
                }
            }
        }
    }


    /**
     * Starts up this ListnerManager service.
     * 
     * @throws InterruptedException if this service's driver thread cannot start
     * @throws IllegalStateException if this service has already started
     */
    public void start() throws InterruptedException
    {
        if ( m_hasStarted.booleanValue() )
        {
            throw new IllegalStateException( "Already started!" ) ;
        }
        
        m_hasStarted = new Boolean( true ) ;
        m_thread = new Thread( this ) ;
        m_thread.start() ;
        m_monitor.started() ;
    }
    
    
    /**
     * Gracefully stops this ListenerManager service.  Blocks calling thread 
     * until the service has fully stopped.
     * 
     * @throws InterruptedException if this service's driver thread cannot start
     */
    public void stop() throws InterruptedException
    {
        m_hasStarted = new Boolean( false ) ;
        m_selector.wakeup() ;
        
        while ( m_thread.isAlive() || ! m_clients.isEmpty() )
        {
            if ( ! m_clients.isEmpty() )
            {
                synchronized( m_clients )
                {
                    Iterator list = m_clients.iterator() ;
                    while ( list.hasNext() )
                    {
                        ClientKey key = ( ClientKey ) list.next() ;
                        
                        try
                        {
                            Socket socket = key.getSocket() ;
                            socket.close() ;
                            list.remove() ;
                        }
                        catch( IOException e )
                        {
                            // monitor.doSomthing( e ) ;
                            e.printStackTrace() ;
                        }
                    }
                }
            }
            
            Thread.sleep( 100 ) ;
            m_selector.wakeup() ;
        }
        
        m_monitor.stopped() ;
    }
}
