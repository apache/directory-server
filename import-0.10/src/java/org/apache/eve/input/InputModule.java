/*
 * $Id: InputModule.java,v 1.7 2003/03/24 13:22:29 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.input ;


import java.util.Map ;
import java.util.HashMap ;

import java.io.InputStream ;
import java.io.IOException ;
import java.io.PushbackInputStream ;

import org.apache.eve.AbstractModule ;
import org.apache.eve.decoder.Decoder ;
import org.apache.eve.client.ClientKey ;
import org.apache.eve.event.InputEvent ;
import org.apache.eve.client.ClientManager ;

import org.apache.avalon.framework.service.ServiceManager ;
import org.apache.avalon.framework.service.ServiceException ;
import org.apache.avalon.framework.configuration.Configuration ;
import org.apache.avalon.cornerstone.services.threads.ThreadManager ;
import org.apache.avalon.framework.configuration.ConfigurationException ;
import org.apache.eve.client.KeyExpiryException;
import org.apache.avalon.framework.logger.Logger;





/**
 * The InputModule implements the InputManager service interface and in
 * doing so is responsible for detecting input on the client Socket's
 * InputStream.
 *
 * @phoenix:block
 * @phoenix:mx-topic name="InputModule"
 * @phoenix:service name="org.apache.eve.input.InputManager"
 */
public class InputModule
    extends AbstractModule
    implements InputManager
{
    /** name of the socket listener pool used for this module: 'client' */
    public static final String SOCKETLISTENER_POOL = "client" ;

    /** forward map of ClientKeys to client InputStreams */
    private Map m_streams = new HashMap() ;
    /** handle on the ClientManager service */
    private ClientManager m_clientManager = null ;
    /** handle on the ThreadManager service */
    private ThreadManager m_threadManager = null ;
    /** handle on the Decoder service */
    private Decoder m_decoder = null ;


    /**
     * Registers a client with this module so that input detection can occur.
     *
     * @param a_clientKey the unique key identifing a client
     * @param a_clientIn the client InputStream to be monitored.
     */
    public synchronized
        void register(final ClientKey a_clientKey, final InputStream a_clientIn)
    {
        m_streams.put(a_clientKey, a_clientIn) ;
        Runnable l_monitor = new InputStreamMonitor(a_clientKey, a_clientIn) ;
        m_threadManager.getThreadPool(SOCKETLISTENER_POOL).execute(l_monitor) ;
    }


    class InputStreamMonitor implements Runnable
    {
        final ClientKey m_clientKey ;
        final InputStream m_clientIn ;

        InputStreamMonitor(final ClientKey a_clientKey,
            final InputStream a_clientIn)
        {
            m_clientKey = a_clientKey ;
            m_clientIn = a_clientIn ;
        }


        /**
         * Runnable implementation which monitor's a client connection for
         * incomming LDAP request data.  The loop runs indefinately until the
         * ClientKey expires or an IOException occurs on the clients
         * InputStream.  IOExceptions drop client connections, log an error and
         * request that the ClientManager drop the client if it already has not
         * done so.
         */
		public void run() {
            Logger l_log = InputModule.this.getLogger() ;
			Object l_lock = null ;

            // Obtain input lock object or return.
            try {
                l_lock = m_clientKey.getInputLock() ;
            } catch(KeyExpiryException e) {
                if(l_log.isWarnEnabled()) {
					l_log.warn("ClientKey for " + m_clientKey + " has expired "
                        + "immediately after server accept:", e) ;
                }
                return ;
            }

        	// Start input detection loop
			try {
				// l_in is used for the life of the connection and hence
				// this client listener.  All InputEvents to the decoder
				// carry this object.
				PushbackInputStream l_in =
					new PushbackInputStream(m_clientIn) ;

				// Character is read then pushed back before event delivery
				// so the decoder can read a complete PDU with this first byte.
				int ch = -1 ;

				synchronized(l_lock) {
					while((ch = l_in.read()) != -1
                        && !m_clientKey.hasExpired())
                    {
						l_in.unread(ch) ;
						InputEvent l_event =
							new InputEvent(m_clientKey, l_in) ;
						m_decoder.inputReceived(l_event) ;

						try {
							// Wait until we are notified by event handler
							// of the decoder to resume listening for more
							// input from the client.  Decoder event handler
							// needs to suck down a request message before
							// giving us the io stream back.

							l_lock.wait() ;
						} catch(InterruptedException e) {
							m_clientManager.drop(m_clientKey) ;
							l_log.error("Client " + m_clientKey + " dropped on"
                                + " InputStreamMonitor's thread error: ", e) ;
						}
					}
				}
			} catch(IOException e) {
				m_clientManager.drop(m_clientKey) ;
				l_log.error("Client " + m_clientKey
                    + " dropped due to exception on read from client "
                    + "InputStream: ", e) ;
			}
		}
    }


    /**
     * Unregisters a client with this module so that input detection is not
     * enabled for a client's InputStream previously enabled via the register
     * method of this service.
     *
     * @param a_clientKey the unique key identifing a client
     */
    public synchronized void unregister(ClientKey a_clientKey)
    {
        m_streams.remove(a_clientKey) ;
    }


    /**
     * ClientManagerSlave method implementation which prevents a cyclic
     * dependency from this module back to the ClientManager.  This method sets
     * the handle on the ClientManager service rather than the <code>service()
     * </code> lifecycle method.
     *
     * @param a_manager the ClientManager service handle
     */
    public void registerClientManager(ClientManager a_manager)
    {
        m_clientManager = a_manager ;
    }


    /////////////////////////////////
    // Module & Life-Cycle Methods //
    /////////////////////////////////


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
        return "Client Input Manager Module" ;
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
     * Gets a handle on the Decoder and the ThreadManager services which this
     * module depends on.
     * 
     * @phoenix:dependency name="org.apache.eve.decoder.Decoder"
     * @phoenix:dependency name="org.apache.avalon.cornerstone.services.threads.ThreadManager"
     */
    public void service(ServiceManager a_manager)
        throws ServiceException
    {
        super.service(a_manager) ;
        m_decoder = (Decoder) a_manager.lookup(Decoder.ROLE) ;
        m_threadManager = (ThreadManager) a_manager.lookup(ThreadManager.ROLE) ;
    }


    /**
     * Does nothing.
     *
     * @param a_config the configuration for this module.
     */
    public void configure(Configuration a_config)
        throws ConfigurationException
    {
    }
}
