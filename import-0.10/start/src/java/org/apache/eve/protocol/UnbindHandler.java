/*
 * $Id: UnbindHandler.java,v 1.2 2003/08/22 21:15:56 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.protocol ;


import org.apache.ldap.common.message.Request ;
import org.apache.ldap.common.message.UnbindRequest ;
import org.apache.ldap.common.message.MessageTypeEnum ;

/**
 * Handles the processing of UnbindRequests.  Not presently implemented.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.2 $
 */
public class UnbindHandler
    implements NoReplyHandler
{
    /** Reference to the protocol module this handler is part of */
	private final ProtocolModule m_module ;


    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------


    /**
     * Creates a handler for UnbindRequests to work on behalf of a
     * ProtocolModule.
     *
     * @param a_module the ProtocolModule this handler is part of.
     */
	public UnbindHandler( ProtocolModule a_module )
    {
		m_module = a_module ;
    }


    // ------------------------------------------------------------------------
    // RequestHandler Interface Method Implementations
    // ------------------------------------------------------------------------


    /**
     * Gets the handler type for this handler which will always be the
     * HandlerTypeEnum.NOREPLY enumeration constant.
     *
     * @return the HandlerTypeEnum.NOREPLY enumeration constant
     */
	public final HandlerTypeEnum getHandlerType()
    {
        return HandlerTypeEnum.NOREPLY ;
    }


    /**
     * Gets the message type handled by this handler which will always be the
     * MessageTypeEnum.UNBINDREQUEST enumeration constant.
     *
     * @return the MessageTypeEnum.UNBINDREQUEST enumeration constant
     */
	public MessageTypeEnum getRequestType()
    {
        return MessageTypeEnum.UNBINDREQUEST ;
    }


    /**
     * Handles an unbind request by disconnecting a client and terminating their
     * session on the server.
     *
     * @param a_request the UnbindRequest.
     * @throws ClassCastException if the a_request argument is not an
     * UnbindRequest
     */
    public void handle( Request a_request )
    {
        try
        {
			UnbindRequest l_request = ( UnbindRequest) a_request ;
            m_module.getClientManager().drop() ;
        }
        catch( Throwable t )
        {
            m_module.getLogger().error( "Failed on UnbindRequest!", t ) ;
        }
    }
}
