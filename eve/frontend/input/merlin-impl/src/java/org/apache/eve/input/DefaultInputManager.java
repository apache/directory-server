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
package org.apache.eve.input ;


import java.util.Iterator ;
import java.util.EventObject ;

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
import org.apache.eve.listener.KeyExpiryException ;


/**
 * Default InputManager implementation based on NIO selectors and channels.
 *
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
 * @version $Rev$
 */
public class DefaultInputManager implements InputManager
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
        m_router = a_router ;
        m_hasStarted = new Boolean( false ) ;
        m_selector = Selector.open() ;
    }
    

        // ------------------------------------------------------------------------
        // start, stop and runnable code
        // ------------------------------------------------------------------------
    
    
    /**
     * Runnable used to drive the selection loop. 
     *
     * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
     * @author $Author$
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
                    if ( 0 == ( l_count = m_selector.select( 100 ) ) )
                    {
                        m_monitor.selectorReturned( m_selector ) ;
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
        ClientKey l_key = null ;
        SocketChannel l_channel = null ;
        
        try
        {
            l_key = an_event.getClientKey() ;
            l_channel = l_key.getSocket().getChannel() ;
            
            // hands-off blocking sockets!
            if ( null == l_channel )
            {
                return ;
            }
            
            l_channel.configureBlocking( false ) ;
            l_channel.register( m_selector, SelectionKey.OP_READ, l_key ) ;
            m_monitor.registeredChannel( l_key, m_selector ) ;
        }
        catch ( KeyExpiryException e )
        {
            String l_msg = "Attempting session creation using expired key for "
                + an_event.getClientKey() ;
            m_monitor.keyExpiryFailure( l_key, e ) ;
        }
        catch ( IOException e )
        {
            String l_msg = "Input managmer registration failure for " +
                an_event.getClientKey() + " due to exception." ;
            m_monitor.channelRegistrationFailure( m_selector, l_channel, 
                    SelectionKey.OP_READ, e ) ;
        }
    }

    
    /**
     * @see org.apache.eve.event.DisconnectListener#
     * inform(org.apache.eve.event.DisconnectEvent)
     */
    public void inform( DisconnectEvent an_event )
    {
        SelectionKey l_key = null ;
        Iterator l_keys = m_selector.keys().iterator() ;
        
        while ( l_keys.hasNext() )
        {
            l_key = ( SelectionKey ) l_keys.next() ;
            if ( l_key.attachment().equals( an_event.getClientKey() ) )
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
        m_monitor.disconnectedClient( an_event.getClientKey() ) ;
    }
    

    /**
     * @see org.apache.eve.event.Subscriber#inform(java.util.EventObject)
     */
    public void inform( EventObject an_event )
    {
        Class l_clazz = an_event.getClass() ;
        
        if ( l_clazz.isAssignableFrom( ConnectEvent.class ) )
        {
            inform( ( ConnectEvent ) an_event ) ;
        }
        else if ( l_clazz.isAssignableFrom( DisconnectEvent.class ) ) ;
        {
            inform( ( DisconnectEvent ) an_event ) ;
        }
    }
    
    
    // ------------------------------------------------------------------------
    // private utilities
    // ------------------------------------------------------------------------
    
    
    /**
     * Processes input on channels of the read ready selected keys.
     */
    void processInput()
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

                // claim a buffer and read & return buffer on errors 
                try
                {
                    l_buf = m_bp.getBuffer( this ) ;
                    l_channel.read( l_buf ) ;
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
                    
                // report to monitor, create the event, and publish it
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
     * @author $Author$
     * @version $Revision$
     */
    class ConcreteInputEvent extends InputEvent
    {
        ConcreteInputEvent( ClientKey a_key, ByteBuffer a_buffer )
        {
            super( a_key, a_buffer ) ;
        }
        
        public ByteBuffer claimInterest( Object a_party )
        {
            m_bp.claimInterest( m_buffer, a_party ) ;
            return m_buffer.asReadOnlyBuffer() ;
        }
        
        public void releaseInterest( Object a_party )
        {
            m_bp.releaseClaim( m_buffer, a_party ) ;
        }
    }
    
    
    /**
     * Gets the monitor associated with this InputManager.
     * 
     * @return returns the monitor
     */
    InputManagerMonitor getMonitor()
    {
        return m_monitor ;
    }

    
    /**
     * Sets the monitor associated with this InputManager.
     * 
     * @param a_monitor the monitor to set
     */
    void setMonitor( InputManagerMonitor a_monitor )
    {
        m_monitor = a_monitor ;
    }
}
