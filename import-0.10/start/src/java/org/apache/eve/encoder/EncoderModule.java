/*
 * $Id: EncoderModule.java,v 1.6 2003/08/22 21:15:55 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.encoder ;


import java.util.EventObject ;
import java.io.ByteArrayInputStream ;

import org.apache.avalon.framework.logger.Logger ;
import org.apache.avalon.framework.service.ServiceManager ;
import org.apache.avalon.framework.service.ServiceException ;
import org.apache.avalon.framework.CascadingRuntimeException ;

import org.apache.ldap.common.message.Response ;
import org.apache.ldap.common.message.MessageEncoder ;
import org.apache.ldap.common.message.MessageException ;

import org.apache.eve.client.ClientKey ;
import org.apache.eve.event.OutputEvent ;
import org.apache.eve.seda.AbstractStage ;
import org.apache.eve.event.ResponseEvent ;
import org.apache.eve.output.OutputManager ;
import org.apache.eve.event.AbstractEventHandler ;


/**
 * Encodes Protocol Data Unit (PDU) using Basic Encoding (BER) Rules.
 * 
 * @phoenix:block
 * @phoenix:service name="org.apache.eve.encoder.Encoder"
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.6 $
 */
public class EncoderModule
    extends AbstractStage
    implements Encoder
{
    /** Handle on the output manager which recieves events from this module. */
    private OutputManager m_outputManager = null ;

    /** Encoding machinery from the message/BERlib provider framework. */
    private MessageEncoder m_msgEncoder = null ;


    // ------------------------------------------------------------------------
    // Default Constructor
    // ------------------------------------------------------------------------


    /**
     * Constructor initializes this stage's event handler by instantiating a
     * ResponseEventHandler which is a named inner class.  The event handler is
     * used to process events after they are dequeued off of the stage's event
     * queue.  The handler's handleEvent is called with an EventObject by stage
     * worker threads which drive event handling.  Pesently this is instantiated
     * here because rather than within the initialize method because the handler
     * is LogEnabled when the EncoderModule is in the enableLogging life-cycle
     * method.
     *
     * @todo look into the correct way to enable logging in these handlers while
     * making their instantiation reside within the initialize life-cycle
     * method.
     */
    public EncoderModule()
    {
        m_handler = new ResponseEventHandler() ;
    }


    // ------------------------------------------------------------------------
    // Encoder Service Interface Method Implementations
    // ------------------------------------------------------------------------


    /**
     * Synchronously encodes an LDAPv3 protocol Response message into a byte
     * buffer that can be written to a Stream as an BER encoded PDU.
     *
     * @param a_response the LDAP Response message to be encoded.
     */
    public byte [] encode( Response a_response )
        throws EncoderException
    {
        byte [] l_buf = null ;

        try
        {
            l_buf = m_msgEncoder.encode( a_response ) ;
        }
        catch( MessageException me )
        {
            getLogger().error( "Encoder error: ", me ) ;
            throw new EncoderException( "Encoder error: ", me ) ;
        }

        return l_buf ;
    }


    // ------------------------------------------------------------------------
    // ResponseListener Interface Method Implementations
    // ------------------------------------------------------------------------


    /**
     * Listener's handler method simply enqueues the ResponseEvent on this
     * stage's event queue for asynchronous processing by stage worker threads.
     *
     * @param a_event the ResponseEvent to handle.
     */
    public void responseComposed( ResponseEvent a_event )
        throws CascadingRuntimeException
    {
	    enqueue( a_event ) ;
    }


    // ------------------------------------------------------------------------
    // Module Interface Method Implementations
    // ------------------------------------------------------------------------


    /**
     * Gets the service interface name of this module.
     *
     * @return the role of this module's implemented service.
     * @phoenix:mx-attribute
     * @phoenix:mx-description Returns the service role name.
     * @phoenix:mx-isWriteable no
     */
    public String getImplementationRole()
    {
        return ROLE ;
    }


    /**
     * Gets the name of the implementation.  For example the name of the
     * Berkeley DB Backend module is "Berkeley DB Backend".
     *
     * @return String representing the module implementation type name.
     * @phoenix:mx-attribute
     * @phoenix:mx-description Returns the implementation name.
     * @phoenix:mx-isWriteable no
     */
    public String getImplementationName()
    {
        return "ASN.1 BER Encoder Module" ;
    }


    /**
     * Gets the name of the implementation class.  For example the name of the
     * Berkeley DB Backend implementation class is <code>
     * "ldapdd.backend.berkeley.BackendBDb" </code>.
     *
     * @return String representing the module implementation's class name.
     * @phoenix:mx-attribute
     * @phoenix:mx-description Returns the implementation class name.
     * @phoenix:mx-isWriteable no
     */
    public String getImplementationClassName()
    {
        return this.getClass().getName() ;
    }


    // ------------------------------------------------------------------------
    // Avalon Framework Life-Cycle Methods
    // ------------------------------------------------------------------------


    /**
     * Log enables the handler right after calling the super method.
     *
     * @param a_logger the logger used by this module.
     */
    public void enableLogging( Logger a_logger )
    {
        super.enableLogging( a_logger ) ;
        m_handler.enableLogging( a_logger ) ;
    }


    /**
     * Initialization instantiates the MessageEncoder.
     *
     * @todo see if there is any value in adding an optional configuration
     * parameter to switch the BER library provider based on a configuration
     * property for this module.
     */
    public void initialize() throws Exception
    {
        m_msgEncoder = new MessageEncoder() ;
    }


    /**
     * Service method grabs a handle on the OutputManager directly and its
     * super method implementation grabs a handler on the cornerstone
     * ThreadManager for getting the stage's thread pool.
     * 
     * @phoenix:dependency name="org.apache.eve.output.OutputManager"
     * @phoenix:dependency name="org.apache.avalon.cornerstone.services.threads.ThreadManager"
	 */
    public void service( ServiceManager a_manager )
        throws ServiceException
    {
        super.service( a_manager ) ;
        m_outputManager = ( OutputManager )
            a_manager.lookup( OutputManager.ROLE ) ;
    }


    // ------------------------------------------------------------------------
    // Stage EventHandler Class Definition
    // ------------------------------------------------------------------------


    /**
     * Stage event handler class definition.
     */
    class ResponseEventHandler extends AbstractEventHandler
    {
        /**
         * Check and cast a response event. Then processes event by encoding
         * the response into a byte [] buffer using the parent class' encode
         * method.  Builds the OutputEvent and delivers it to the OutputManager.
         *
         * @param an_event the ResponseEvent to process.
         */
	    public void handleEvent( EventObject an_event )
        {
	        if( ! ( an_event instanceof ResponseEvent ) )
            {
	            throw new EncoderException( "Unrecognized event: "
                    + an_event ) ;
	        }

            ResponseEvent l_event = ( ResponseEvent ) an_event ;

            try {
	            if( ! ( an_event instanceof ResponseEvent ) )
                {
	                throw new EncoderException("Unrecognized event: " +
                        l_event) ;
	            }
	    
	            ClientKey l_client = ( ClientKey ) an_event.getSource() ;
                byte [] l_buf = encode( l_event.getResponse() ) ;
	            ByteArrayInputStream l_in = new ByteArrayInputStream( l_buf ) ;
	    
	            if( getLogger().isDebugEnabled() )
                {
	                getLogger().debug( "Response encoded for client "
                        + l_client ) ;
                    getLogger().debug( "About to hand off response to client "
                        + "manager within encoder event") ;
                }

                OutputEvent l_outEvent = new OutputEvent( l_client, l_in ) ;
                m_outputManager.writeResponse( l_outEvent ) ;
	    
	            if( getLogger().isDebugEnabled() )
                {
	                getLogger().debug( "Handed off OutputEvent for client "
                        + l_client + " to OutputManager" ) ;
	            }
	        } catch(Throwable t) {
	            getLogger().error( "Encoder Stage Handler: ", t ) ;
	        }
	    }
    }
}
