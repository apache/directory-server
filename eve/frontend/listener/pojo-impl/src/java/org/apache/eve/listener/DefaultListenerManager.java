/*

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 1999-2002 The Apache Software Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:

 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.

 4. The names "Eve Directory Server", "Apache Directory Project", "Apache Eve" 
    and "Apache Software Foundation"  must not be used to endorse or promote
    products derived  from this  software without  prior written
    permission. For written permission, please contact apache@apache.org.

 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation. For more  information on the
 Apache Software Foundation, please see <http://www.apache.org/>.

*/
package org.apache.eve.listener ;


import java.util.Set ;
import java.util.HashSet ;
import java.util.Iterator ;
import java.util.EventObject ;

import java.io.IOException ;
import java.net.InetAddress ;
import java.net.InetSocketAddress ;

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
        m_router = a_router ;
        m_clients = new HashSet() ;
        m_selector = Selector.open() ;
        m_hasStarted = new Boolean( false ) ;
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
        synchronized( m_hasStarted )
        {
            if ( m_hasStarted.booleanValue() )
            {
                throw new IllegalStateException( "Already started!" ) ;
            }
            
            m_hasStarted = new Boolean( true ) ;
            m_thread = new Thread( this ) ;
            m_thread.start() ;
        }
        
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
        synchronized( m_hasStarted )
        {
            m_hasStarted = new Boolean( false ) ;
            m_selector.wakeup() ;
            
            while ( m_thread.isAlive() )
            {
                Thread.sleep( 100 ) ;
            }
        }
        
        m_monitor.stopped() ;
    }
}
