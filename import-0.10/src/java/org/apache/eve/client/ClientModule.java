/*
 * $Id: ClientModule.java,v 1.13 2003/08/22 21:15:55 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.client ;


import java.util.Map ;
import java.util.HashMap ;
import java.util.EventObject ;

import java.net.Socket ;

import java.io.IOException ;
import java.io.InputStream ;
import java.io.OutputStream ;
import java.io.PushbackInputStream ;
import java.io.BufferedOutputStream ;

import org.apache.eve.decoder.Decoder ;
import org.apache.eve.event.InputEvent ;
import org.apache.eve.event.OutputEvent ;
import org.apache.eve.input.InputModule ;
import org.apache.eve.seda.AbstractStage ;
import org.apache.eve.event.ConnectEvent ;
import org.apache.eve.event.EventHandler ;
import org.apache.eve.input.InputManager ;
import org.apache.eve.output.OutputModule ;
import org.apache.eve.output.OutputManager ;
import org.apache.eve.security.LdapPrincipal ;
import org.apache.eve.backend.UnifiedBackend ;
import org.apache.eve.protocol.ProtocolEngine ;
import org.apache.eve.event.AbstractEventHandler ;

import org.apache.avalon.framework.logger.Logger ;
import org.apache.avalon.framework.service.ServiceManager ;
import org.apache.avalon.framework.service.ServiceException ;
import org.apache.avalon.framework.CascadingRuntimeException ;
import org.apache.avalon.framework.configuration.Configuration ;
import org.apache.avalon.cornerstone.services.threads.ThreadManager ;
import org.apache.avalon.framework.configuration.ConfigurationException ;
import javax.naming.ldap.LdapContext;


/**
 * ClientManager service implementation used to manage client sessions and
 * their socket connections.
 * 
 * @phoenix:block
 * @phoenix:service name="org.apache.eve.client.ClientManager"
 * @phoenix:mx-topic name="client-manager"
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.13 $
 */
public class ClientModule
    extends AbstractStage
    implements ClientManager
{
    public static final String SOCKETLISTENER_POOL = "client" ;

    private Map m_sockets = new HashMap() ;
    private Map m_sessions = new HashMap() ;
	private Decoder m_decoder = null ;
    private InputManager m_inputManager = null ;
    private OutputManager m_outputManager = null ;
    private UnifiedBackend m_nexus = null ;
    private ProtocolEngine m_engine = null ;

    /**
     * We are using a ThreadLocal just as a means to key into a session based
     * on the current thread of execution which has been associated with a
     * session using the threadAssociate method.  This is done to enable backend
     * modules to access the session of the user performing operations without
     * having to pass around the user session.  The ThreadLocal actually maps
     * the Thread to the ClientKey.  <code>getClientSession()</code> without
     * arguments simply accesses the ClientKey using the current executing
     * Thread as the key into this ThreadLocal.  The ClientKey is then used to
     * return the ClientSession via another lookup into the m_sessions Map.
     */
    private ThreadLocal m_threadKeys = new ThreadLocal() ;

    /**
     * Thread outside of the protocol manager making calling nexus and backend
     * methods through the JNDI provider need to propagate context environment
     * parameters without passing is as an argument one each call.  This Thread
     * Local maps threads to JNDI contexts.
     */
    private ThreadLocal m_threadCtxs = new ThreadLocal() ;


    // ---------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------


    /**
     * Default constructor creates the client module by initializing a
     * ConnectEventHandler for it.
     */
    public ClientModule()
    {
        m_handler = new ConnectEventHandler() ;
    }


    // ---------------------------------------------------------
    // Stage Event Handler Class: ConnectionEventHandler
    // ---------------------------------------------------------


    /**
     * The stage event handler for the ClientModule.  This class processes
     * incomming ConnectEvents thrown by the ServerListener service.
     */
    class ConnectEventHandler extends AbstractEventHandler
    {
        /**
         * Primary event handling method to process a ConnectEvent.
         *
         * @param an_event must be a ConnectEvent subtype of EventObject
         */
        public void handleEvent( EventObject an_event )
        {
            Socket l_socket = null ;
            Logger l_log = ClientModule.this.getLogger() ;
            ClientKey l_clientKey = null ;
            ConnectEvent l_event = null ;

            try
            {
                if( an_event instanceof ConnectEvent )
                {
                    l_event = ( ConnectEvent ) an_event ;
                    l_socket = ( Socket ) l_event.getSource() ;
                    l_clientKey = new ClientKey( l_socket ) ;
                    add( l_clientKey, l_socket ) ;
                }
                else
                {
                    l_log.error( "Unknown event " + an_event + " ignored" ) ;
                }
            }
            catch( Throwable t )
            {
                l_log.error( "Droping client " + l_clientKey
                    + " on handler error: ", t ) ;
                drop( l_clientKey ) ;
            }
        }
    }


    // ---------------------------------------------------------
    // Add/Drop/Get ClientSession Management Methods
    // ---------------------------------------------------------


    /**
     * Drops a client connection and destroys the client's session associated
     * with the calling thread.
     */
    public void drop()
    {
    	drop( getClientKey() ) ;
    }


    /**
     * Drops a client connection by unregistering the client with io managers
     * and closing the client socket connection.
     *
     * @param a_clientKey the key of the client to drop
     */
    public void drop( ClientKey a_clientKey )
    {
        Socket l_socket = null ;
        ClientSession l_session = null ;

        // Unregister client key with io manager and expire it.
        m_inputManager.unregister( a_clientKey ) ;
        m_outputManager.unregister( a_clientKey ) ;
        a_clientKey.expire() ;

        // Remove client session from session map
        synchronized( m_sessions )
        {
            l_session = ( ClientSession ) m_sessions.remove( a_clientKey ) ;
        }

        // Invalidate removed session to disable use by held handles to session
        if( l_session != null )
        {
            l_session.invalidate() ;
        }

        // Remove client socket
        synchronized( m_sockets )
        {
            l_socket = ( Socket ) m_sockets.remove( a_clientKey ) ;
        }

        // Close the client socket
        if( null != l_socket )
        {
            try
            {
	            l_socket.close() ;
            }
            catch( IOException e )
            {
                String l_msg = "Could not close client " + a_clientKey
                    + " socket!" ;
                getLogger().error( l_msg, e ) ;
                throw new ClientException( l_msg, e ) ;
            }
        }
    }


    /**
     * Adds a client connection by registering the client io streams with the
     * io managers and tracking the client using the supplied client key.  The
     * client's session is initialized using an anonymous principal.  Later
     * bind operations can change the session principal to represent the
     * appropriate user.
     *
     * @param a_clientKey the key of the client to add.
     * @param a_socket the client's socket.
     */
    public ClientSession add( ClientKey a_clientKey, Socket a_socket )
        throws IOException
    {
        LdapClientSession l_session = null ;

        // map client key to socket.
        synchronized( m_sockets )
        {
            m_sockets.put( a_clientKey, a_socket ) ;
        }

        // Register Socket IO streams with the respective IO manager
        m_inputManager.register( a_clientKey, a_socket.getInputStream() ) ;
        m_outputManager.register( a_clientKey, a_socket.getOutputStream() ) ;

        // Create a client session using the default principal (anonymous)
        // which corresponds to an empty string for the distinguished name.
        l_session = new LdapClientSession( a_clientKey, new LdapPrincipal() ) ;
	    m_sessions.put( a_clientKey, l_session ) ;
        return l_session ;
    }


    /**
     * Sets the user principal for a client on a bind operation.  The protocol
     * states that a bind operation requires the destruction of all outstanding
     * operations.  Hence all outstanding operations refered to by the client's
     * key must be terminated, the session parameters are flushed, then the
     * princal is set.  The client affectively changes its role without dropping
     * the socket connection.
     *
     * @todo need to make sure we stop all outstanding request operations before
     * changing the principal of the user.
     * @param a_clientKey the client's unique key.
     * @param a_principal the new principal to be taken on by the client using
     * the existing socket connection.
     */
    public ClientSession
        setUserPrincipal( ClientKey a_clientKey, LdapPrincipal a_principal )
    {
        /** @todo */
        // Must destroy existing operations here before changing the principal
        LdapClientSession l_session = ( LdapClientSession )
            m_sessions.get( a_clientKey ) ;

        // We need to completely replace the session, expire the old key and
        // create a new one and go from there rather than reseting the session
        // we want old handles to be useless to both these objects and we
        // want the code possessing it to be aware when it tries to use it.

        // This is the only way to stop outstanding requests that are in stages
        // other than those that are in the protocol engine stage.  We basically
        // need to destroy and recreate everything except the socket connection.

        l_session.reset() ;
        l_session.setPrincipal( a_principal ) ;
        return l_session ;
    }


    /**
     * Gets the ClientSession associated with the ClientKey argument.
     *
     * @param a_clientKey the unique client primary key
     * @return the session of the client
     */
    public ClientSession getClientSession( ClientKey a_clientKey )
    {
        return ( ClientSession ) m_sessions.get( a_clientKey ) ;
    }


    /**
     * Gets the key of the client on whose behalf the current thread is
     * executing.
     *
     * @return the ClientKey associated with the callers thread or null if none
     * exists.
     */
	public ClientKey getClientKey()
    {
		return ( ClientKey ) m_threadKeys.get() ;
    }


    /**
     * Gets the ClientKey associated within the context of the calling Thread.
     *
     * @see threadAssociate()
     * @see threadDisassociate()
     * @return the session of the client associated with the calling Thread
     */
    public ClientSession getClientSession()
    {
        ClientKey l_key = ( ClientKey ) m_threadKeys.get() ;
        return ( ClientSession ) m_sessions.get( l_key ) ;
    }


    /**
     * Gets the LdapContext associated with the calling thread.
     *
     * @return the context of the caller thread
     */
    public LdapContext getLdapContext()
    {
        return ( LdapContext ) m_threadCtxs.get() ;
    }


    /**
     * Associates the calling thread with a client using a ClientKey.  A call to
     * this method enables calls to getClientSession() without arguments to
     * return a non-null ClientSession handle.
     *
     * @param a_clientKey the unique client primary key
     */
    public void threadAssociate( ClientKey a_clientKey )
    {
	    m_threadKeys.set( a_clientKey ) ;
    }


    /**
     * Associates the calling thread with a JNDI LdapContext.  A call to
     * this method enables calls to getLdapContext() without arguments to
     * return an LdapContext handle.
     *
     * @param a_ctx the LdapContext to associate with the calling thread
     */
    public void threadAssociate( LdapContext a_ctx )
    {
	    this.m_threadCtxs.set( a_ctx ) ;
    }


    /**
     * Disassociates the calling thread with a client.  After a call to this
     * method the calling thread cannot acquire a non-null handle on a
     * ClientSession object through a call to getClientSession() without
     * arguments.
     */
    public void threadDisassociate()
    {
        if( m_threadKeys.get() != null )
        {
        	m_threadKeys.set( null ) ;
        }
        else
        {
        	m_threadCtxs.set( null ) ;
        }
    }


    // ---------------------------------------------------------
    // Listener Implementations
    // ---------------------------------------------------------


    /**
     * ConnectListener interface implementation which asynchronously processes
     * the ConnectEvent generated by the ServerListener service.  It merely
     * enqueues the event onto this Stage's event queue and returns immediately
     * without processing the event in the thread of the caller.
     *
     * @param an_event a client connection event.
     */
    public void connectPerformed( ConnectEvent an_event )
        throws CascadingRuntimeException
    {
        enqueue( an_event ) ;
    }


    // ---------------------------------------------------------
    // Module & Life-Cycle Methods
    // ---------------------------------------------------------


    /**
     * Overriden to enable handler after enabling this Module via a super
     * method invokation.
     *
     * @param a_logger the logger to set this module and its handler to use
     */
    public void enableLogging( Logger a_logger )
    {
        super.enableLogging( a_logger ) ;
        m_handler.enableLogging( a_logger ) ;
    }


    /**
     * Gets the ROLE of the service this Module implements.
     *
     * @phoenix:mx-attribute
     * @phoenix:mx-description gets the service interface (ROLE)
     * @phoenix:mx-isWriteable no
     * @return the role of this service.
     */
    public String getImplementationRole()
    {
        return ROLE ;
    }


    /**
     * Gets a descriptive name for this module's implementation.
     *
     * @phoenix:mx-attribute
     * @phoenix:mx-description gets the implementation name
     * @phoenix:mx-isWriteable no
     * @return descriptive implementation name.
     */
    public String getImplementationName()
    {
        return "Client Manager Module" ;
    }


    /**
     * Gets the fully qualified class name of this service implementation class.
     *
     * @phoenix:mx-attribute
     * @phoenix:mx-description gets the implementation class name
     * @phoenix:mx-isWriteable no
     * @return FQCN of this class.
     */
    public String getImplementationClassName()
    {
        return this.getClass().getName() ;
    }


	/**
     * Gets a handle on various services minux the ThreadManager which is
     * accessed by the service method of the AbstractStage superclass.
     * 
     * @phoenix:dependency name="org.apache.eve.decoder.Decoder"
     * @phoenix:dependency name="org.apache.eve.input.InputManager"
     * @phoenix:dependency name="org.apache.eve.output.OutputManager"
     * @phoenix:dependency name="org.apache.eve.backend.UnifiedBackend"
     * @phoenix:dependency name="org.apache.eve.protocol.ProtocolEngine"
     * @phoenix:dependency name="org.apache.avalon.cornerstone.services.threads.ThreadManager"
     */
    public void service( ServiceManager a_manager )
        throws ServiceException
    {
        super.service( a_manager ) ;
        m_decoder = ( Decoder ) a_manager.lookup( Decoder.ROLE ) ;
        m_nexus = ( UnifiedBackend ) a_manager.lookup( UnifiedBackend.ROLE ) ;
        m_engine = ( ProtocolEngine ) a_manager.lookup( ProtocolEngine.ROLE ) ;
        m_inputManager = ( InputManager )
            a_manager.lookup( InputManager.ROLE ) ;
        m_outputManager = ( OutputManager )
            a_manager.lookup( OutputManager.ROLE ) ;
    }


    /**
     * Initializes this module by registering it with various slave modules like
     * the InputManager, OutputManager and ProtocolEngine services.
     */
    public void initialize()
        throws Exception
    {
        m_nexus.registerClientManager( this ) ;
        m_engine.registerClientManager( this ) ;
        m_inputManager.registerClientManager( this ) ;
        m_outputManager.registerClientManager( this ) ;
    }
}
