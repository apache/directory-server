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
package org.apache.eve ;


import org.apache.avalon.framework.service.Serviceable ;
import org.apache.avalon.framework.service.ServiceManager ;
import org.apache.avalon.framework.service.ServiceException ;

import org.apache.avalon.cornerstone.services.threads.ThreadManager ;

import org.apache.eve.event.EventRouter ;
import org.apache.eve.buffer.BufferPool ;
import org.apache.eve.input.InputManager ;
import org.apache.eve.output.OutputManager ;
import org.apache.eve.decoder.DecoderManager ;
import org.apache.eve.encoder.EncoderManager ;
import org.apache.eve.listener.ListenerManager ;
import org.apache.eve.processor.RequestProcessor ;


/**
 * Eve's frontend wrapper for Merlin.  There really is nothing here but this 
 * will evolve as we determine the use cases for applications embedding the 
 * frontend and accessing it outside of the container.
 *
 * @avalon.component name="frontend" lifestyle="singleton"
 * @avalon.service type="org.apache.eve.Frontend" version="1.0"
 * 
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class MerlinFrontend 
    implements 
    Frontend,
    Serviceable
{
    BufferPool m_bufferPool = null ;
    EventRouter m_eventRouter = null ;
    InputManager m_inputManager = null ;
    OutputManager m_outputManager = null ;
    ThreadManager m_threadManager = null ;
    EncoderManager m_encoderManager = null ;
    DecoderManager m_decoderManager = null ;
    ListenerManager m_listenerManager = null ;
    RequestProcessor m_requestProcessor = null ;
    
    
    // ------------------------------------------------------------------------
    // A V A L O N   L I F E C Y C L E   M E T H O D S 
    // ------------------------------------------------------------------------
    

    /**
     * @avalon.dependency key="thread-manager" 
     *      type="org.apache.avalon.cornerstone.services.threads.ThreadManager"
     * @avalon.dependency type="org.apache.eve.event.EventRouter"
     *      key="event-router" version="1.0" 
     * @avalon.dependency type="org.apache.eve.buffer.BufferPool"
     *      key="buffer-pool" version="1.0" 
     * @avalon.dependency type="org.apache.eve.listener.ListenerManager"
     *      key="listener-manager" version="1.0" 
     * @avalon.dependency type="org.apache.eve.input.InputManager"
     *      key="input-manager" version="1.0"
     * @avalon.dependency type="org.apache.eve.output.OutputManager"
     *      key="output-manager" version="1.0"
     * @avalon.dependency type="org.apache.eve.decoder.DecoderManager"
     *      key="decoder-manager" version="1.0"
     * @avalon.dependency type="org.apache.eve.encoder.EncoderManager"
     *      key="encoder-manager" version="1.0"
     * @avalon.dependency type="org.apache.eve.processor.RequestProcessor"
     *      key="request-processor" version="1.0"
     * 
     * @see org.apache.avalon.framework.service.Serviceable#service(
     * org.apache.avalon.framework.service.ServiceManager)
     */
    public void service( ServiceManager a_manager ) throws ServiceException
    {
        m_threadManager = ( ThreadManager ) a_manager
            .lookup( "thread-manager" ) ;
        
        m_eventRouter = ( EventRouter ) a_manager
            .lookup( "event-router" ) ;

        m_bufferPool = ( BufferPool ) a_manager
            .lookup( "buffer-pool" ) ;

        m_listenerManager = ( ListenerManager ) a_manager 
            .lookup( "listener-manager" ) ;

        m_inputManager = ( InputManager ) a_manager
            .lookup( "input-manager" ) ;
        
        m_outputManager = ( OutputManager ) a_manager
            .lookup( "output-manager" ) ;

        m_decoderManager = ( DecoderManager ) a_manager
            .lookup( "decoder-manager" ) ;

        m_encoderManager = ( EncoderManager ) a_manager
            .lookup( "encoder-manager" ) ;

        m_requestProcessor = ( RequestProcessor ) a_manager
            .lookup( "request-processor" ) ;
    }
}
