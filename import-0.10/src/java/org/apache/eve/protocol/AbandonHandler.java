/*
 * $Id: AbandonHandler.java,v 1.2 2003/08/22 21:15:55 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.protocol ;


import java.math.BigInteger ;

import org.apache.ldap.common.message.Request ;
import org.apache.ldap.common.message.AbandonRequest ;
import org.apache.ldap.common.message.MessageTypeEnum ;
import org.apache.ldap.common.NotImplementedException ;


/**
 * Handles the processing of AbandonRequests.  Not presently implemented.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.2 $
 */
public final class AbandonHandler
    implements NoReplyHandler
{
    /** Reference to the protocol module this handler is part of */
	private final ProtocolModule m_module ;


    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------


    /**
     * Creates a handler for AbandonRequests to work on behalf of a
     * ProtocolModule.
     *
     * @param a_module the ProtocolModule this handler is part of.
     */
	public AbandonHandler( ProtocolModule a_module )
    {
		m_module = a_module ;
    }


    // ------------------------------------------------------------------------
    // RequestHandler Interface Method Implementations
    // ------------------------------------------------------------------------


    /**
     * Gets the handler type enumeration constant associated with this handler.
     *
     * @return HandlerTypeEnum.NOREPLY
     */
	public final HandlerTypeEnum getHandlerType()
    {
        return HandlerTypeEnum.NOREPLY ;
    }


    /**
     * Gets the message type enumeration constant associated with this handler.
     *
     * @return HandlerTypeEnum.NOREPLY
     */
	public final MessageTypeEnum getRequestType()
    {
        return MessageTypeEnum.ABANDONREQUEST ;
    }


    /**
     * Handles an AbandonRequest by stopping the outstanding request specified.
     *
     * @param a_request the AbandonRequest to handle.
     * @throws ClassCastException if the a_request argument is not an
     * AbandonRequest.
     */
    public final void handle( Request a_request )
    {
        try
        {
			throw new NotImplementedException( "Handler not complete!" ) ;
        }
        catch( Throwable t )
        {
            m_module.getLogger().error( "Failed on AbandonRequest!", t ) ;
        }
    }
}
