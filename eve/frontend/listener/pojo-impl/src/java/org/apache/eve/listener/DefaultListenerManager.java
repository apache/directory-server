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

import java.net.Socket ;
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
    private final EventRouter router ;
    /** selector used to select a acceptable socket channel */
    private final Selector selector ;
    /** the client keys for accepted connections */
    private final Set clients ; 
    /** the set of listeners managed */
    private final Set listeners ;
    /** new listeners waiting to be bound */
    private final Set bindListeners ;
    /** old listeners waiting to be unbound */
    private final Set unbindListeners ;
    
    /** the thread driving this Runnable */ 
    private Thread thread = null ;
    /** parameter used to politely stop running thread */
    private Boolean hasStarted = null ;
    /** the listner manager's monitor */
    private ListenerManagerMonitor monitor = null ;
    
    
    /**
     * Creates a default listener manager using an event router.
     * 
     * @param router the router to publish events to
     * @throws IOException
     */
    public DefaultListenerManager( EventRouter router ) throws IOException
    {
        this.router = router ;
        this.clients = new HashSet() ;
        this.selector = Selector.open() ;
        this.listeners = new HashSet() ;
        this.hasStarted = new Boolean( false ) ;
        this.bindListeners = new HashSet() ;
        this.unbindListeners = new HashSet() ;
        
        this.router.subscribe( DisconnectEvent.class, null, this ) ;
        this.monitor = new ListenerManagerMonitorAdapter() ; 
    }
    
    
    /**
     * Gets the monitor.
     * 
     * @return Returns the monitor.
     */
    public ListenerManagerMonitor getMonitor()
    {
        return monitor ;
    }

    
    /**
     * Sets the monitor.
     * 
     * @param monitor The monitor to set.
     */
    public void setMonitor( ListenerManagerMonitor monitor )
    {
        this.monitor = monitor ;
    }


    /**
     * @see org.apache.eve.listener.ListenerManager#register(org.apache.eve.
     * listener.ServerListener)
     */
    public void bind( ServerListener listener ) throws IOException
    {
        synchronized ( bindListeners )
        {
            bindListeners.add( listener ) ;
        }
        
        selector.wakeup() ;
    }
    
    
    /**
     * @see org.apache.eve.listener.ListenerManager#unregister(org.apache.eve.
     * listener.ServerListener)
     */
    public void unbind( ServerListener listener ) throws IOException
    {
        synchronized ( unbindListeners )
        {
            unbindListeners.add( listener ) ;
        }

        selector.wakeup() ;
    }
    

    /**
     * Binds all the listeners that have been collecting up waiting to be bound.
     * This is not fail fast - meaning it will try all the connections in the
     * ready to bind set even if one fails.
     */
    private void bind()
    {
        synchronized ( bindListeners )
        {
            Iterator list = bindListeners.iterator() ;
            while ( list.hasNext() )
            {
                ServerListener listener = ( ServerListener ) list.next() ;
                    
                try
                {
                    ServerSocketChannel channel = ServerSocketChannel.open() ;
                    InetSocketAddress address = new InetSocketAddress( 
                            InetAddress.getByAddress( listener.getAddress() ), 
                            listener.getPort() ) ;
                    channel.socket().bind( address, listener.getBacklog() ) ;
                    channel.configureBlocking( false ) ;
                    channel.register( selector, SelectionKey.OP_ACCEPT, 
                            listener ) ;
                    
                    synchronized ( listeners )
                    {
                        listeners.add( listener ) ;
                    }
                    
                    bindListeners.remove( listener ) ;
                }
                catch ( IOException e )
                {
                    monitor.failedToBind( listener, e ) ;
                }
            
                monitor.bindOccured( listener ) ;
            }
        }
    }
    
    
    /**
     * Unbinds listeners that have been collecting up waiting to be unbound.
     * This is not fail fast - meaning it will try all the connections in the
     * ready to unbind set even if one fails.
     */
    private void unbind()
    {
        SelectionKey key = null ;
        
        synchronized ( unbindListeners ) 
        {
            Iterator keys = selector.keys().iterator() ;
            while ( keys.hasNext() )
            {
                key = ( SelectionKey ) keys.next() ;
                ServerListener listener = ( ServerListener ) key.attachment() ;
    
                if ( unbindListeners.contains( listener ) )
                {    
                    try
                    {
                        key.channel().close() ;
                    }
                    catch ( IOException e )
                    {
                        monitor.failedToUnbind( listener, e ) ;
                    }
                
                    key.cancel() ;
                    
                    synchronized ( listeners )
                    {
                        listeners.remove( listener ) ;
                    }
                    
                    unbindListeners.remove( listener ) ;
                    monitor.unbindOccured( listener ) ;
                }
            }
        }
    }


    // ------------------------------------------------------------------------
    // DisconnectSubscriber Implementation
    // ------------------------------------------------------------------------
    
    
    /**
     * Disconnects a client by removing the clientKey from the listener.
     * 
     * @param event the disconnect event
     */
    public void inform( DisconnectEvent event )
    {
        clients.remove( event.getClientKey() ) ;
        
        try
        {
            event.getClientKey().expire() ;
        }
        catch ( IOException e ) 
        {
            monitor.failedToExpire( event.getClientKey(), e ) ;
        }
    }
    
    
    /*
     *  (non-Javadoc)
     * @see org.apache.eve.event.Subscriber#inform(java.util.EventObject)
     */
    public void inform( EventObject event )
    {
        inform( ( DisconnectEvent ) event ) ;
    }
    
    
    // ------------------------------------------------------------------------
    // Runnable implementation and start/stop controls
    // ------------------------------------------------------------------------
    
    
    /**
     * @see java.lang.Runnable#run()
     */
    public void run()
    {
        while ( hasStarted.booleanValue() ) 
        {
            int count = 0 ;
            
            try
            {
                monitor.enteringSelect( selector ) ;
                
                bind() ;
                unbind() ;
                
                if ( 0 == ( count = selector.select() ) )
                {
                    monitor.selectTimedOut( selector ) ;
                    continue ;
                }
            } 
            catch( IOException e )
            {
                monitor.failedToSelect( selector, e ) ;
                continue ;
            }
            
            
            Iterator list = selector.selectedKeys().iterator() ;
            while ( list.hasNext() )
            {
                SelectionKey key = ( SelectionKey ) list.next() ;
                
                if ( key.isAcceptable() )
                {
                    SocketChannel channel = null ;
                    ServerSocketChannel server = ( ServerSocketChannel )
                        key.channel() ;
                    
                    try
                    {
                        channel = server.accept() ;
                        list.remove() ;
                        monitor.acceptOccured( key ) ;
                    }
                    catch ( IOException e )
                    {
                        monitor.failedToAccept( key, e ) ;
                        continue ;
                    }
                    
                    ClientKey clientKey = new ClientKey( channel.socket() ) ;
                    ConnectEvent event = new ConnectEvent( this, clientKey ) ;
                    router.publish( event ) ;
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
        if ( hasStarted.booleanValue() )
        {
            throw new IllegalStateException( "Already started!" ) ;
        }
        
        hasStarted = new Boolean( true ) ;
        thread = new Thread( this ) ;
        thread.start() ;
        monitor.started() ;
    }
    
    
    /**
     * Gracefully stops this ListenerManager service.  Blocks calling thread 
     * until the service has fully stopped.
     * 
     * @throws InterruptedException if this service's driver thread cannot start
     */
    public void stop() throws InterruptedException
    {
        hasStarted = new Boolean( false ) ;
        selector.wakeup() ;

        /*
         * First lets shutdown the listeners so we're not open to having new
         * connections created while we are trying to shutdown.  Plus we want 
         * to make the thread for this component do the work to prevent locking
         * issues with the selector.
         */
        if ( ! listeners.isEmpty() )
        {
            Iterator list = listeners.iterator() ;
            while( list.hasNext() )
            {
                ServerListener listener = ( ServerListener ) list.next() ;
                    
                try
                {
                    /*
                     * put the listening in the set ready to be unbound by
                     * the runnable's thread of execution
                     */
                    unbind( listener ) ;
                }
                catch( IOException e )
                {
                    // monitor.doSomthing( e ) ;
                    e.printStackTrace() ;
                }
            }
        }
        
        /*
         * Now we gracefully disconnect the clients that are already connected 
         * so they can complete their current requests and recieve a 
         * notification of disconnect.  At this point we don't know how we're 
         * going to do that so we just do it abruptly for the time being.  This
         * will need to be changed in the future. 
         */
        if ( ! clients.isEmpty() )
        {
            synchronized( clients )
            {
                Iterator list = clients.iterator() ;
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

        /*
         * now wait until the thread of execution for this runnable dies
         */
        if ( this.thread.isAlive() )
        {
            Thread.sleep( 100 ) ;
            selector.wakeup() ;
        }

        monitor.stopped() ;
    }
}
