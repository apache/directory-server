/*
 * $Id: DecoderModule.java,v 1.8 2003/08/22 21:15:55 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.decoder ;


import java.io.InputStream ;
import java.util.EventObject ;

import org.apache.ldap.common.message.Request ;
import org.apache.ldap.common.message.MessageDecoder ;

import org.apache.eve.event.InputEvent ;
import org.apache.eve.client.ClientKey ;
import org.apache.eve.event.RequestEvent ;
import org.apache.eve.seda.AbstractStage ;
import org.apache.eve.client.KeyExpiryException ;
import org.apache.eve.protocol.ProtocolEngine ;
import org.apache.eve.event.AbstractEventHandler ;

import org.apache.avalon.framework.logger.Logger ;
import org.apache.avalon.framework.service.ServiceManager ;
import org.apache.avalon.framework.service.ServiceException ;


/**
 * Decoder service implemented as a stage for ASN.1 Binary encoded LDAPv3 data.
 * 
 * @phoenix:block
 * @phoenix:mx-topic name="DecoderModule"
 * @phoenix:service name="org.apache.eve.decoder.Decoder"
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.8 $
 */
public class DecoderModule
    extends AbstractStage
    implements Decoder
{
    /** Handle on the ldap request protocol processing engine */
    private ProtocolEngine m_engine = null ;

    /** MessageDecoder used to decode messages in a provider independent way. */
    private MessageDecoder m_msgDecoder = null ;


    // ------------------------------------------------------------------------
    // Default Constructor
    // ------------------------------------------------------------------------


    /**
     * Creates an instance of the Decoder service module.  Initializes the
     * stage's event handler so the next life-cycle method can enable logging.
     * 
     * @todo look into the correct way to enable logging in these handlers while
     * making their instantiation reside within the initialize life-cycle
     * method.
     */
    public DecoderModule()
    {
        m_handler = new InputEventHandler() ;
    }


    // ------------------------------------------------------------------------
    // InputListener Interface Method Implementations
    // ------------------------------------------------------------------------


    /**
     * InputListener interface method implementation enabling this DecoderModule
     * to detect the reception of InputEvents.  Simply enqueues the event onto
     * this Stage's event queue returning immediately thereafter.  The event is
     * processed by this Stage's event handler.
     *
     * @param a_event InputEvent encapsulating the ClientKey and the client
     * Socket's InputStream.
     */
    public void inputReceived( InputEvent a_event )
    {
        enqueue( a_event ) ;
    }


    // ------------------------------------------------------------------------
    // Module Interface Method Implementations
    // ------------------------------------------------------------------------


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
        return "ASN.1 LDAPv3 BER Decoder Module" ;
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


    // ------------------------------------------------------------------------
    // Avalon Life-Cycle Interface Method Implementations
    // ------------------------------------------------------------------------


    /**
     * Method override to make sure that the handler's logger is initialized
     * after calling the super method.
     *
     * @param a_logger the Logger for this module.
     */
    public void enableLogging( Logger a_logger )
    {
        super.enableLogging( a_logger ) ;
        m_handler.enableLogging( a_logger ) ;
    }


    /**
     * Initialization instantiates the MessageDecoder.
     *
     * @todo see if there is any value in adding an optional configuration
     * parameter to switch the BER library provider based on a configuration
     * property for this module.
     */
    public void initialize() throws Exception
    {
        m_msgDecoder = new MessageDecoder() ;
    }


	/**
     * Grabs handle directly on ProtocolEngine yet super call grabs the
     * ThreadManager.
     * 
     * @phoenix:dependency name="org.apache.eve.protocol.ProtocolEngine"
     * @phoenix:dependency name="org.apache.avalon.cornerstone.services.threads.ThreadManager"
     */
    public void service( ServiceManager a_manager )
        throws ServiceException
    {
        super.service( a_manager ) ;
        m_engine = ( ProtocolEngine ) a_manager.lookup( ProtocolEngine.ROLE ) ;
    }


    // ------------------------------------------------------------------------
    // Decoder EventHandler Implementation Class
    // ------------------------------------------------------------------------


    /**
     * Decoder stage EventHandler implementation whose handleEvent method is the
     * primary work function driven by this stage's worker threads.
     */
	class InputEventHandler extends AbstractEventHandler
    {
        /**
         * Handles an InputEvent by decoding the binary encoded byte stream
         * contained within a_event parameter in a BER Library provider
         * independant fashion.
         *
         * @param a_event the InputEvent as an EventObject.
         */
        public void handleEvent( EventObject a_event )
        {
            Object l_lock = null ;
            InputStream l_in = null ;
	        InputEvent l_event = null ;
            ClientKey l_clientKey = null ;

            // Check first event type is correct
	        if( ! ( a_event instanceof InputEvent ) )
            {
	            throw new DecoderException( "Unrecognized event: " + a_event ) ;
	        }

            // Extract event info and init local vars
	        l_event = ( InputEvent ) a_event ;
	        l_clientKey = ( ClientKey ) l_event.getSource() ;
	        l_in = l_event.getInputStream() ;

            // Obtain input lock object or return.
            try
            {
                l_lock = l_clientKey.getInputLock() ;
            }
            catch( KeyExpiryException e )
            {
                if( getLogger().isWarnEnabled() )
                {
	                getLogger().warn( "ClientKey for " + l_clientKey
                        + " has expired right after server accept: ", e ) ;
                }

                return ;
            }

            // Decode the request, build the event and deliver to the engine
            Request l_request = ( Request )
                m_msgDecoder.decode( l_lock, l_in ) ;
            RequestEvent l_requestEvent =
                new RequestEvent( l_clientKey, l_request ) ;
	        m_engine.requestReceived( l_requestEvent ) ;
        }
    }
}
