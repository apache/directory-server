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

import java.net.InetAddress ;
import java.net.UnknownHostException ;

import java.util.ArrayList ;

import org.apache.avalon.framework.logger.Logger ;
import org.apache.avalon.framework.activity.Startable ;
import org.apache.avalon.framework.service.Serviceable ;
import org.apache.avalon.framework.activity.Initializable ;
import org.apache.avalon.framework.service.ServiceManager ;
import org.apache.avalon.framework.service.ServiceException ;
import org.apache.avalon.framework.logger.AbstractLogEnabled ;
import org.apache.avalon.framework.configuration.Configurable ;
import org.apache.avalon.framework.configuration.Configuration ;
import org.apache.avalon.framework.configuration.ConfigurationException ;

import org.apache.commons.lang.StringUtils ;

import org.apache.eve.event.EventRouter ;


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
    Configurable,
    Serviceable,
    Startable
{
    /** the listener manager's avalon based monitor */
    private AvalonListenerManagerMonitor m_monitor ;
    /** the listener manager implementation wrapped by this service */
    private DefaultListenerManager m_manager ;
    /** a temporary handle on the event router to bridge life-cycle methods */
    private EventRouter m_router ;
    /** the set of listeners */
    private ArrayList m_listeners = new ArrayList() ;
    
    
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
    

    // ------------------------------------------------------------------------
    // Life Cycle Methods
    // ------------------------------------------------------------------------
    
    
    /**
     * Set's up the monitor with a logger.
     * 
     * @param a_logger a logger.
     */
    public void enableLogging( Logger a_logger ) 
    {
        super.enableLogging( a_logger ) ;
        m_monitor = new AvalonListenerManagerMonitor() ;
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
                "About to call delegate start() from merlin wrapper!" ) ;
        m_manager.start() ;
        getLogger().debug( 
                "Completed call to delegate start() from merlin wrapper!" ) ;

        for( int ii = 0; ii < m_listeners.size(); ii++ )
        {    
            ServerListener l_listener = ( ServerListener ) 
                m_listeners.get( ii ) ;
            m_manager.bind( l_listener ) ;
            
            if ( getLogger().isInfoEnabled() )
            {
                getLogger().info( "Listener " + l_listener + " bound!" ) ;
                getLogger().info( "Interface: " + l_listener.getAddress()[0] 
                    + "." + l_listener.getAddress()[1] 
                    + "." + l_listener.getAddress()[2] 
                    + "." + l_listener.getAddress()[3] 
                        ) ;
                getLogger().info( "Port: " + l_listener.getPort() ) ;
                getLogger().info( "Backlog: " + l_listener.getBacklog() ) ;
                getLogger().info( "Secure: " + l_listener.isSecure() ) ;
                getLogger().info( "URL: " + l_listener.getURL() ) ;
            }
        }
    }
    
    
    /**
     * Blocks calling thread until this module gracefully stops.
     * 
     * @see org.apache.avalon.framework.activity.Startable#stop()
     */
    public void stop() throws Exception
    {
        getLogger().debug( 
                "About to call delegate stop() from merlin wrapper!" ) ;
        
        if ( m_manager != null )
        {    
            m_manager.stop() ;
        }
        
        getLogger().debug( 
                "Completed call to delegate stop() from merlin wrapper!" ) ;
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
        m_manager.setMonitor( m_monitor ) ;
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.avalon.framework.configuration.Configurable#
     * configure(org.apache.avalon.framework.configuration.Configuration)
     */
    public void configure( Configuration a_config ) 
        throws ConfigurationException
    {
        if ( a_config.getChild( "listeners" ).getChildren().length == 0 )
        {
            try 
            {
                m_listeners.add( new LdapServerListener() ) ;
            }
            catch ( UnknownHostException e )
            {
                throw new ConfigurationException( "No configuration provided "
                        + "for listener configuration and default listener "
                        + "failed due to exception", e ) ;
            }
        }
        
        Configuration[] l_listeners = a_config
            .getChild( "listeners" ).getChildren() ;
        for ( int ii = 0; ii < l_listeners.length; ii++ )
        {
            int l_port = l_listeners[ii].getChild( "port" )
                .getValueAsInteger( 389 ) ;
            int l_backlog = l_listeners[ii].getChild( "backlog" )
                .getValueAsInteger( 50 ) ;
            boolean l_isSecure = l_listeners[ii].getChild( "isSecure" )
                .getValueAsBoolean( false ) ;
            String l_host = null ;
            Configuration l_hostConf = l_listeners[ii]
                .getChild( "host", false ) ;
            Configuration l_addressConf = l_listeners[ii]
                .getChild( "address", false ) ;
            
            if ( l_hostConf == null && l_addressConf == null )
            {
                try 
                {
                    l_host = InetAddress.getLocalHost().getHostName() ;
                }
                catch ( UnknownHostException e )
                {
                    throw new ConfigurationException( "No configuration address"
                            + " or hostname provided and using localhost "
                            + "failed due to exception", e ) ;
                }
            }
            else if ( l_hostConf != null ) 
            {
                l_host = l_hostConf.getValue() ;
            }
            else if ( l_addressConf != null )
            {
                String l_addrStr = l_addressConf.getValue() ;
                // split appart and build byte array
                String[] l_octets = StringUtils.split( l_addrStr, '.' ) ;
                byte[] l_address = new byte[ l_octets.length ] ;
                for ( int jj =0; jj < l_octets.length; jj++ )
                {
                    l_address[jj] = Byte.parseByte( l_octets[jj] ) ;
                }

                try
                {
                    l_host = InetAddress.getByAddress( l_address )
                        .getHostName() ;
                }
                catch ( UnknownHostException e )
                {
                    throw new ConfigurationException( "Could not find hostname "
                            + "for address " + l_addrStr, e ) ;
                }
            }
            
            
            try
            {
                m_listeners.add( new LdapServerListener( l_host, l_port, 
                                l_backlog, l_isSecure ) ) ;
            }
            catch ( UnknownHostException e )
            {
                throw new ConfigurationException( "Could not find hostname "
                        + "for host " + l_host, e ) ;
            }
            
            if ( getLogger().isInfoEnabled() )
            {
                getLogger().info( " Configured a listener for host " + 
                        l_host + " on port " + l_port + " with a backlog of "
                        + l_backlog
                        ) ;
            }
        }
    }
}
