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


import java.util.EventObject ;

import java.nio.channels.Selector ;

import org.apache.avalon.framework.activity.Startable ;
import org.apache.avalon.framework.service.Serviceable ;
import org.apache.avalon.framework.activity.Initializable ;
import org.apache.avalon.framework.service.ServiceManager ;
import org.apache.avalon.framework.service.ServiceException ;
import org.apache.avalon.framework.logger.AbstractLogEnabled ;

import org.apache.eve.buffer.BufferPool ;
import org.apache.eve.event.EventRouter ;
import org.apache.eve.event.ConnectEvent ;
import org.apache.eve.event.DisconnectEvent ;


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
    /** the buffer pool to get buffers from */
    private BufferPool m_bp = null ;
    /** event router used to decouple source to sink relationships */
    private EventRouter m_router = null ;
    /** selector used to select a ready socket channel */
    private Selector m_selector = null ;
    /** the wrapped input manager implementation */
    private DefaultInputManager m_inputManager = null ;
    
    
    // ------------------------------------------------------------------------
    // Listener Interfaces
    // ------------------------------------------------------------------------
    
    
    /**
     * @see org.apache.eve.event.ConnectListener#
     * connectPerformed(org.apache.eve.event.ConnectEvent)
     */
    public void inform( ConnectEvent an_event )
    {
    }

    
    /**
     * @see org.apache.eve.event.DisconnectListener#
     * inform(org.apache.eve.event.DisconnectEvent)
     */
    public void inform( DisconnectEvent an_event )
    {
    }
    
    
    /**
     * 
     */
    public void inform( EventObject an_event )
    {
    }
    

    // ------------------------------------------------------------------------
    // Life Cycle Methods
    // ------------------------------------------------------------------------
    
    
    /**
     * Starts up this module.
     * 
     * @see org.apache.avalon.framework.activity.Startable#start()
     */
    public void start() throws Exception
    {
        m_inputManager.start() ;
    }
    
    
    /**
     * Blocks calling thread until this module gracefully stops.
     * 
     * @see org.apache.avalon.framework.activity.Startable#stop()
     */
    public void stop() throws Exception
    {
        m_inputManager.stop() ;
    }
    
    
    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception
    {
        m_inputManager = new DefaultInputManager( m_router, m_bp ) ;
        
    }

    
    /**
     * @avalon.dependency type="org.apache.eve.event.EventRouter"
     *         key="event-router" version="1.0" 
     * @avalon.dependency type="org.apache.eve.buffer.BufferPool"
     *         key="buffer-pool" version="1.0" 
     * 
     * @see org.apache.avalon.framework.service.Serviceable#service(
     * org.apache.avalon.framework.service.ServiceManager)
     */
    public void service( ServiceManager a_manager ) throws ServiceException
    {
        m_bp = ( BufferPool ) a_manager.lookup( "buffer-pool" ) ;
        m_router = ( EventRouter ) a_manager.lookup( "event-router" ) ;
    }
}
