/*
 * $Id: DeleteHandler.java,v 1.2 2003/08/22 21:15:56 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.protocol ;


import org.apache.ldap.common.message.DeleteRequest ;
import org.apache.ldap.common.message.ResultResponse ;
import org.apache.ldap.common.message.MessageTypeEnum ;
import org.apache.ldap.common.message.SingleReplyRequest ;
import org.apache.ldap.common.message.DeleteResponse;
import org.apache.ldap.common.message.DeleteResponseImpl;
import javax.naming.NamingException;
import javax.naming.InitialContext;


/**
 * DeleteRequest handler for delete protocol requests.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.2 $
 */
public class DeleteHandler
	implements SingleReplyHandler
{
    /** The protocol module this request handler is part of */
    private final ProtocolModule m_module ;


    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------


    /**
     * Creates a DeleteRequest protocol data unit handler.
     *
     * @param a_module the module this handler is associated with.
     */
    public DeleteHandler( ProtocolModule a_module )
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
		return MessageTypeEnum.DELREQUEST ;
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
        return MessageTypeEnum.DELRESPONSE ;
    }


    /**
     * Handles a request that generates a sole response by returning the
     * response object back to the caller.
     *
     * @param a_request the request to handle.
     * @return the response to the request argument.
     * @throws ClassCastException if a_request is not a DeleteRequest
     */
    public ResultResponse handle( SingleReplyRequest a_request )
    {
        InitialContext l_initCtx ;
        DeleteRequest l_request = ( DeleteRequest ) a_request ;
        DeleteResponse l_response =
            new DeleteResponseImpl( l_request.getMessageId() ) ;

		try
		{
            l_initCtx = Utils.getInitialContext() ;
            l_initCtx.destroySubcontext( l_request.getName() ) ;
            Utils.setResult( l_response, l_request.getName(), false ) ;
        }
        // Could be anything here.
        catch( NamingException ne )
        {
			Utils.setResult( l_response, l_request.getName(), ne ) ;
        }

		return l_response ;
    }
}
