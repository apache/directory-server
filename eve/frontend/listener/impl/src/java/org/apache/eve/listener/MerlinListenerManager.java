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


import java.io.IOException ;
import java.util.EventObject ;

import org.apache.avalon.framework.activity.Startable ;
import org.apache.avalon.framework.service.Serviceable ;
import org.apache.avalon.framework.activity.Initializable ;
import org.apache.avalon.framework.service.ServiceManager ;
import org.apache.avalon.framework.service.ServiceException ;
import org.apache.avalon.framework.logger.AbstractLogEnabled ;

import org.apache.eve.event.EventRouter ;
import org.apache.eve.event.DisconnectEvent ;


/**
 * A listener manager that uses non-blocking NIO based constructs to detect
 * client connections on server socket listeners.
 * 
 * @avalon.component name="listener-manager" lifestyle="singleton"
 * @avalon.service type="org.apache.eve.listener.ListenerManager" version="1.0"
 * 
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
 * @version $Rev$
 */
public class MerlinListenerManager extends AbstractLogEnabled
    implements 
    ListenerManager,
    Initializable, 
    Serviceable,
    Startable
{
    /** the listener manager implementation wrapped by this service */
    private DefaultListenerManager m_manager ;
    /** a temporary handle on the event router to bridge life-cycle methods */
    private EventRouter m_router ;
    
    
    // ------------------------------------------------------------------------
    // ListenerManager delegating methods ...
    // ------------------------------------------------------------------------
    
    
    /**
     * @see org.apache.eve.listener.ListenerManager#register(org.apache.eve.
     * listener.ServerListener)
     */
    public void bind( ServerListener a_listener ) throws IOException
    {
        m_manager.bind( a_listener ) ;
    }
    
    
    /**
     * @see org.apache.eve.listener.ListenerManager#unregister(org.apache.eve.
     * listener.ServerListener)
     */
    public void unbind( ServerListener a_listener ) throws IOException
    {
        m_manager.unbind( a_listener ) ;
    }
    

    /*
     *  (non-Javadoc)
     * @see org.apache.eve.event.DisconnectSubscriber#inform(
     * org.apache.eve.event.DisconnectEvent)
     */
    public void inform( DisconnectEvent an_event )
    {
        m_manager.inform( an_event ) ;
    }
    
    
    /*
     *  (non-Javadoc)
     * @see org.apache.eve.event.Subscriber#inform(java.util.EventObject)
     */
    public void inform( EventObject an_event )
    {
        m_manager.inform( an_event ) ;
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
        m_manager.start() ;
    }
    
    
    /**
     * Blocks calling thread until this module gracefully stops.
     * 
     * @see org.apache.avalon.framework.activity.Startable#stop()
     */
    public void stop() throws Exception
    {
        m_manager.stop() ;
    }
    
    
    /**
     * @avalon.dependency type="org.apache.eve.event.EventRouter"
     *         key="event-router" version="1.0" 
     * @see org.apache.avalon.framework.service.Serviceable#service(
     * org.apache.avalon.framework.service.ServiceManager)
     */
    public void service( ServiceManager a_manager )
        throws ServiceException
    {
       m_router = ( EventRouter ) a_manager.lookup( "event-router" ) ;
    }
    
    
    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception
    {
        m_manager = new DefaultListenerManager( m_router ) ;
    }
}
