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


import java.nio.channels.Selector ;

import org.apache.avalon.framework.logger.Logger ;
import org.apache.avalon.framework.activity.Startable ;
import org.apache.avalon.framework.service.Serviceable ;
import org.apache.avalon.framework.activity.Initializable ;
import org.apache.avalon.framework.service.ServiceManager ;
import org.apache.avalon.framework.service.ServiceException ;
import org.apache.avalon.framework.logger.AbstractLogEnabled ;
import org.apache.avalon.cornerstone.services.threads.ThreadManager ;

import org.apache.eve.buffer.BufferPool ;
import org.apache.eve.event.EventRouter ;


/**
 * A non-blocking input manager.
 * 
 * @avalon.component name="input-manager" lifestyle="singleton"
 * @avalon.service type="org.apache.eve.input.InputManager" version="1.0"
 * 
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class MerlinInputManager extends AbstractLogEnabled 
    implements
    InputManager,
    Serviceable,
    Startable,
    Initializable
{
    /** the thread manager we get thread pools from */
    private ThreadManager m_tm = null ;
    /** the buffer pool to get buffers from */
    private BufferPool m_bp = null ;
    /** event router used to decouple source to sink relationships */
    private EventRouter m_router = null ;
    /** selector used to select a ready socket channel */
    private Selector m_selector = null ;
    /** the wrapped input manager implementation */
    private DefaultInputManager m_delegate = null ;
    /** the monitor for the delegate */
    private AvalonInputManagerMonitor m_monitor = null ;
    
    
    // ------------------------------------------------------------------------
    // Life Cycle Methods
    // ------------------------------------------------------------------------
    
    
    public void enableLogging( Logger a_logger )
    {
        super.enableLogging( a_logger ) ;
        m_monitor = new AvalonInputManagerMonitor() ;
        m_monitor.enableLogging( a_logger ) ;
    }
    
    /**
     * Starts up this module.
     * 
     * @see org.apache.avalon.framework.activity.Startable#start()
     */
    public void start() throws Exception
    {
        getLogger().debug( 
                "Merlin wrapper about to invoke delegate start()" ) ;
        m_delegate.start() ;
        getLogger().debug( 
                "Merlin wrapper invoked delegate start()" ) ;
    }
    
    
    /**
     * Blocks calling thread until this module gracefully stops.
     * 
     * @see org.apache.avalon.framework.activity.Startable#stop()
     */
    public void stop() throws Exception
    {
        if ( m_delegate != null )
        {    
            getLogger().debug( 
                    "Merlin wrapper about to invoke delegate stop()" ) ;
            m_delegate.stop() ;
            getLogger().debug( 
                    "Merlin wrapper invoked delegate stop()" ) ;
        }
    }
    
    
    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception
    {
        getLogger().debug( "Delegate constructed" ) ;
        m_delegate = new DefaultInputManager( m_router, m_bp ) ;
        m_delegate.setMonitor( m_monitor ) ;
    }

    
    /**
     * @avalon.dependency type="org.apache.eve.event.EventRouter"
     *      key="event-router" version="1.0" 
     * @avalon.dependency type="org.apache.eve.buffer.BufferPool"
     *      key="buffer-pool" version="1.0" 
     * @avalon.dependency key="thread-manager" 
     *      type="org.apache.avalon.cornerstone.services.threads.ThreadManager"
     * 
     * @see org.apache.avalon.framework.service.Serviceable#service(
     * org.apache.avalon.framework.service.ServiceManager)
     */
    public void service( ServiceManager a_manager ) throws ServiceException
    {
        m_tm = ( ThreadManager ) a_manager.lookup( "thread-manager" ) ;
        m_bp = ( BufferPool ) a_manager.lookup( "buffer-pool" ) ;
        m_router = ( EventRouter ) a_manager.lookup( "event-router" ) ;
    }
}
