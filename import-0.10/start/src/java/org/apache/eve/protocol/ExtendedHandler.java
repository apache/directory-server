/*
 * $Id: ExtendedHandler.java,v 1.2 2003/08/22 21:15:56 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.protocol ;


import org.apache.ldap.common.message.AddRequest ;
import org.apache.ldap.common.message.ResultResponse ;
import org.apache.ldap.common.message.MessageTypeEnum ;
import org.apache.ldap.common.NotImplementedException ;
import org.apache.ldap.common.message.SingleReplyRequest ;


/**
 * ExtendedRequest handler for extended protocol requests.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.2 $
 */
public class ExtendedHandler
	implements SingleReplyHandler
{
    /** The protocol module this request handler is part of */
    private final ProtocolModule m_module ;


    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------


    /**
     * Creates a ExtendedRequest protocol data unit handler.
     *
     * @param a_module the module this handler is associated with.
     */
    public ExtendedHandler( ProtocolModule a_module )
    {
        m_module = a_module ;
    }


    // ------------------------------------------------------------------------
    // RequestHandler Interface Method Implementations
    // ------------------------------------------------------------------------


    /**
     * Gets the handler type.
     *
     * @return a HandlerTypeEnum constant.
     */
    public HandlerTypeEnum getHandlerType()
    {
        return HandlerTypeEnum.SINGLEREPLY ;
    }


    /**
     * Gets the request message type handled by this handler.
     *
     * @return a MessageTypeEnum constant associated with the request message.
     */
    public MessageTypeEnum getRequestType()
    {
		return MessageTypeEnum.EXTENDEDREQ ;
    }


    // ------------------------------------------------------------------------
    // RequestHandler Interface Method Implementations
    // ------------------------------------------------------------------------


    /**
     * Gets the response message type for this SingleReplyHandler.
     *
     * @return the MessageTypeEnum constant associated with this handler.
     */
    public MessageTypeEnum getResponseType()
    {
        return MessageTypeEnum.ADDRESPONSE ;
    }


    /**
     * Handles a request that generates a sole response by returning the
     * response object back to the caller.
     *
     * @param a_request the request to handle.
     * @return the response to the request argument.
     * @throws ClassCastException if a_request is not a AddRequest
     */
    public ResultResponse handle( SingleReplyRequest a_request )
    {
        AddRequest l_request = ( AddRequest ) a_request ;
        throw new NotImplementedException() ;
    }
}
