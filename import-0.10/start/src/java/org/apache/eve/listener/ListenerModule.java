/*
 * $Id: ListenerModule.java,v 1.5 2003/03/13 18:27:38 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.listener ;


import java.io.IOException ;

import java.net.Socket ;
import java.net.InetAddress ;
import java.net.ServerSocket ;
import java.net.UnknownHostException ;

import java.io.IOException ;

import java.net.Socket ;
import java.net.InetAddress ;
import java.net.ServerSocket ;
import java.net.UnknownHostException ;

import org.apache.eve.AbstractModule ;
import org.apache.eve.event.ConnectEvent ;
import org.apache.eve.client.ClientManager ;

import org.apache.avalon.framework.service.ServiceManager ;
import org.apache.avalon.framework.service.ServiceException ;
import org.apache.avalon.framework.configuration.Configuration ;
import org.apache.avalon.cornerstone.services.threads.ThreadManager ;
import org.apache.avalon.framework.configuration.ConfigurationException ;


/**
 * A server listener module represents a single server socket bound listening
 * for client connections on a tcp port off an interface.  This implementation
 * is rather primitive in that only one server socket is used for the entire
 * module.  In the near future expect the implementation to support multiple
 * server sockets and to optionally sepecify whether SSL is used.
 * 
 * @phoenix:block
 * @phoenix:mx-topic name="ListenerModule"
 * @phoenix:service name="org.apache.eve.listener.ServerListener"
 */
public class ListenerModule
    extends AbstractModule
    implements ServerListener, Runnable
{
    /** config.xml tag name for port number to listen to */
    public static final String PORT_TAG = "port" ;
    /** config.xml tag name for the ip interface (or hostname) to listen on */
    public static final String HOST_TAG = "host" ;
    /** config.xml tag name for the server socket connection backlog */
	public static final String BACKLOG_TAG = "backlog" ;

    /** default tcp port to listen on (389) */
    public static final int DEFAULT_PORT = 389 ;
    /** default server socket backlog (50) */
    public static final int DEFAULT_BACKLOG = 50 ;
    /** default interface to listen on (localhost) */
    public static final String DEFAULT_HOST = "localhost" ;

    /** tcp port value set by reading config.xml */
    private int m_port ;
    /** server socket backlog value set by reading config.xml */
    private int m_backlog ;
    /** inet address to listen on set by reading config.xml */
    private InetAddress m_address ;
    /** ldap url composed by using port and host values */
	private String m_ldapUrl = null ;
    /** handle on the client manager service */
    private ClientManager m_clientManager = null ;
    /** server socket used to listen for incomming connections */
	private ServerSocket m_serverSocket = null ;


	////////////////////////
    // Some MBean Methods //
	////////////////////////


    /**
     * Gets the tcp port this listener listens to for client connections.
     * 
     * @phoenix:mx-attribute
     * @phoenix:mx-description gets the tcp port listened to.
     * @phoenix:mx-isWriteable no
     */
    public int getPort()
    {
        return m_port ;
    }


    /**
     * Gets the ip interface listened to for client connections as an address
     * 
     * @phoenix:mx-attribute
     * @phoenix:mx-description gets the ip address listened to.
     * @phoenix:mx-isWriteable no
     */
    public String getAddress()
    {
        return m_address.getHostAddress() ;
    }


    /**
     * Gets the effect ldap url of this listener using the port and address
     * 
     * @phoenix:mx-attribute
     * @phoenix:mx-description gets the ldapd URL of this listener
     * @phoenix:mx-isWriteable no
     */
    public String getLdapURL()
    {
        return m_ldapUrl ;
    }


    /**
     * Gets the maximum possible number of client connections backlogged on this
     * listener's server socket before rejecting client connections.
     * 
     * @phoenix:mx-attribute
     * @phoenix:mx-description gets the server socket backlog
     * @phoenix:mx-isWriteable no
     */
    public int getBacklog()
    {
        return m_backlog ;
    }


    ////////////////////////
    // Life-Cycle Methods //
    ////////////////////////


    /**
     * Runnable implementation which runs in a loop until this module is
     * stopped.  The loop waits blocked for client connections to come in via
     * the accept socket call on the server socket.  Once a connection comes in
     * a ConnectEvent is created containing the client socket returned from the
     * accept call.  The event is then enqueued onto the event queue of the
     * client manager stage using the connectPerformed method on the
     * ClientManager.  Once the event is enqueued the loop sits waiting for the
     * next client connection.
     */
	public void run()
    {
		if(getLogger().isDebugEnabled()) {
			getLogger().debug("Listening for LDAP clients at " + m_ldapUrl) ;
		}


        while(hasStarted()) {
            try {
				Socket l_clientSocket = m_serverSocket.accept() ;

				if(getLogger().isDebugEnabled()) {
                    getLogger().debug("Client connected from " +
                        l_clientSocket.getInetAddress()) ;
                }

                ConnectEvent l_event = new ConnectEvent(l_clientSocket) ;
                m_clientManager.connectPerformed(l_event) ;
            } catch(IOException e) {
                getLogger().error("IO failure on accept(): ", e) ;
                throw new ListenerException("IO failure on accept(): ", e) ;
            }
        }
    }


    /**
     * Module lifecycle method that calls <code> super.start() </code> first
     * then creates a new thread using this Runnable Module.  The thread is
     * then started.
     *
     * @throws Exception if superclass <code>start()</code> call fails or this
     * Module's thread <code>start()</code> call fails.
     */
    public void start()
        throws Exception
    {
        super.start() ;
        new Thread(this).start() ;
    }


    /**
     * Initializes this module by creating and binding the server socket.
     *
     * @throws Exception if the bind fails for any reason.
     */
    public void initialize()
        throws Exception
    {
        try {
        	m_serverSocket = new ServerSocket(m_port, m_backlog, m_address) ;
        } catch(IOException e) {
            getLogger().error("Error on bind: " + e) ;
            throw e ;
        }
    }


    /*
	<pre>
    E X A M P L E   C O N F I G U R A T I O N
	&lt;listener&gt;
		&lt;port&gt;389&lt;/port&gt;
        &lt;host&gt;localhost&lt;/host&gt;
        &lt;backlog&gt;50&lt;/backlog&gt;
	&lt;/listener&gt;
    </pre>
    */
    public void configure(Configuration a_config)
        throws ConfigurationException
    {
        String l_host = a_config.getChild(HOST_TAG).getValue(DEFAULT_HOST) ;
        m_port = a_config.getChild(PORT_TAG).getValueAsInteger(DEFAULT_PORT) ;
		m_backlog =
            a_config.getChild(BACKLOG_TAG).getValueAsInteger(DEFAULT_BACKLOG) ;

        try {
			m_address = InetAddress.getByName(l_host) ;
        } catch(UnknownHostException e) {
			throw new ConfigurationException("Could not resolve host " +
                l_host, e) ;
        }

        m_ldapUrl = "ldap://" + l_host + ":" + m_port ;
        if(getLogger().isDebugEnabled()) {
			getLogger().debug("Configured to listen on port "+ m_port
				+ " with host address " + m_address) ;
        }
    }


	/**
     * Extracts a handle on the system ClientManager from the ServiceManager.
     * 
     * @phoenix:dependency name="org.apache.eve.client.ClientManager"
     */
    public void service(ServiceManager a_manager)
        throws ServiceException
    {
        m_clientManager = (ClientManager) a_manager.lookup(ClientManager.ROLE) ;
    }


    /**
     * Gets this Module implementation's service interface (a.k.a its role)
     *
     * @return the service interface name
     * @phoenix:mx-attribute
     * @phoenix:mx-description gets the service interface (ROLE)
     * @phoenix:mx-isWriteable no
     */
    public String getImplementationRole()
    {
        return ROLE ;
    }


    /**
     * Gets this Module implementation's descriptive name.
     *
     * @return descriptive module name
     * @phoenix:mx-attribute
     * @phoenix:mx-description gets the descriptive name
     * @phoenix:mx-isWriteable no
     */
    public String getImplementationName()
    {
        return "Server Socket Listener Module" ;
    }


    /**
     * Gets this Module implementation's fully qualified class name.
     *
     * @return the fqcn
     * @phoenix:mx-attribute
     * @phoenix:mx-description gets the FQCN
     * @phoenix:mx-isWriteable no
     */
    public String getImplementationClassName()
    {
        return this.getClass().getName() ;
    }
}
