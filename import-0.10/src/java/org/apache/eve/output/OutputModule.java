/*
 * $Id: OutputModule.java,v 1.10 2003/04/21 19:13:51 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.output ;


import org.apache.eve.seda.AbstractStage ;
import org.apache.eve.client.ClientKey;
import java.io.OutputStream;
import org.apache.eve.event.OutputEvent;
import java.util.HashMap;
import java.util.Map;
import java.io.InputStream;
import java.io.IOException;
import org.apache.eve.event.AbstractEventHandler;
import org.apache.eve.client.ClientManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.logger.Logger;
import java.util.EventObject;
import org.apache.eve.client.KeyExpiryException;
import java.io.BufferedOutputStream;

/**
 * @phoenix:block
 * @phoenix:service name="org.apache.eve.output.OutputManager"
 */
public class OutputModule
    extends AbstractStage
    implements OutputManager
{
    private Map m_streams = new HashMap() ;
    private ClientManager m_clientManager = null ;


    public OutputModule()
    {
        m_handler = new OutputEventHandler() ;
    }


    class OutputEventHandler extends AbstractEventHandler
    {
		public void handleEvent(EventObject an_event)
		{
			if(an_event instanceof OutputEvent) {
				OutputEvent l_event = (OutputEvent) an_event ;
				ClientKey l_clientKey = l_event.getClientKey() ;

				try {
					write(l_clientKey, l_event.getInputStream()) ;
				} catch(IOException e) {
					getLogger().error("Abruptly dropping client "
						+ l_clientKey + ": ", e) ;
					m_clientManager.drop(l_clientKey) ;
				}
			}
		}
    }


    public void register( ClientKey a_clientKey, OutputStream l_clientOut )
    {
        synchronized( m_streams )
        {
	        m_streams.put( a_clientKey, l_clientOut ) ;
        }
    }


    public void unregister( ClientKey a_clientKey )
    {
        synchronized( m_streams )
        {
	        m_streams.remove( a_clientKey ) ;
        }
    }


    public void registerClientManager(ClientManager a_manager)
    {
        m_clientManager = a_manager ;
    }


    public void writeResponse(OutputEvent an_event)
    {
        enqueue(an_event) ;
    }


    public void write(ClientKey a_clientKey, InputStream an_in)
        throws IOException
    {
        Object l_lock = null ;
        OutputStream l_out = null ;

        synchronized( m_streams )
        {
            l_out = new BufferedOutputStream((OutputStream)
                m_streams.get(a_clientKey)) ;
        }

        if(null == l_out) {
            getLogger().error("Write to client " + a_clientKey +
                " aborted: client output stream not registered!") ;
            return ;
        }

        if(getLogger().isDebugEnabled()) {
            getLogger().debug("Output stream lookup succeeded for client: "
                + a_clientKey) ;
            getLogger().debug("About to lock on client " + a_clientKey
                + " outputLock") ;
        }

        // Obtain output lock for write to client.
        try {
            l_lock = a_clientKey.getOutputLock() ;
        } catch(KeyExpiryException e) {
			// Log inability to deliver response to disconnected client and exit
			if(getLogger().isInfoEnabled()) {
				getLogger().info("Client " + a_clientKey
                    + " disconnected or was dropped before response delivery") ;
			}
            return ;
        }

        // Synchronize on client output stream lock object.
		synchronized(l_lock) {
			if(getLogger().isDebugEnabled()) {
				getLogger().debug("Successfully locked on socket "
					+ "output stream - writing to client: " + a_clientKey) ;
			}

            //
            // Cycle writes to output stream while the client is connected and
            // the response content input stream still has data to read from.
            // Use 512 byte buffer since buffered output stream by default uses
            // a 512 byte buffer.
            //

            byte [] l_buf = new byte[512] ;
            int l_length = -1 ;
			while(!a_clientKey.hasExpired() &&
                ((l_length = an_in.read(l_buf)) != -1))
            {
				l_out.write(l_buf, 0, l_length) ;
            }

			l_out.flush() ;
			l_lock.notifyAll() ;
		}

        if(getLogger().isDebugEnabled()) {
            getLogger().debug("Write to client " + a_clientKey +
                " socket output stream complete - PDU delivered!") ;
        }
    }


    /////////////////////////////////
    // Module & Life-Cycle Methods //
    /////////////////////////////////


    public void enableLogging(Logger a_logger)
    {
        super.enableLogging(a_logger) ;
        m_handler.enableLogging(a_logger) ;
    }


    public String getImplementationRole()
    {
        return ROLE ;
    }


    public String getImplementationName()
    {
        return "Client Output Manager Module" ;
    }


    public String getImplementationClassName()
    {
        return this.getClass().getName() ;
    }


    /**
     * Needed for the javadoclet!
     * 
     * @phoenix:dependency name="org.apache.avalon.cornerstone.services.threads.ThreadManager"
     */
    public void service(ServiceManager a_manager)
        throws ServiceException
    {
        super.service(a_manager) ;
    }
}
