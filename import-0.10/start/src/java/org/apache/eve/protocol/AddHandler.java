/*
 * $Id: AddHandler.java,v 1.2 2003/08/22 21:15:55 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.protocol ;


import javax.naming.Name ;
import javax.naming.ldap.LdapContext ;
import javax.naming.directory.Attributes ;

import org.apache.ldap.common.message.AddRequest ;
import org.apache.ldap.common.message.AddResponse ;
import org.apache.ldap.common.message.ResultResponse ;
import org.apache.ldap.common.message.MessageTypeEnum ;
import org.apache.ldap.common.message.AddResponseImpl ;
import org.apache.ldap.common.message.SingleReplyRequest ;



/**
 * AddRequest handler for add protocol requests.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.2 $
 */
public class AddHandler
	implements SingleReplyHandler
{
    /** The protocol module this request handler is part of */
    private final ProtocolModule m_module ;


    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------


    /**
     * Creates a AddRequest protocol data unit handler.
     *
     * @param a_module the module this handler is associated with.
     */
    public AddHandler( ProtocolModule a_module )
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
		return MessageTypeEnum.ADDREQUEST ;
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
        Name l_dn = null ;
        AddRequest l_request = ( AddRequest ) a_request ;
		AddResponse l_response =
            new AddResponseImpl( a_request.getMessageId() ) ;
        Attributes l_attributes = l_request.getEntry() ;

        try
        {
            l_dn = m_module.getName( l_request.getName() ) ;

            // Get the context to the parent and create new subcontext for
            // the entry we are currently adding.
			LdapContext l_parent = ( LdapContext )
                m_module.getContext( l_dn.getPrefix( l_dn.size() - 1 ) ) ;
			l_parent.createSubcontext( l_dn.get( l_dn.size()-1 ),
                    l_attributes ) ;

            // Set response result to success
			m_module.setResult( l_response, l_dn ) ;
        }
        catch( Throwable t )
        {
            // Set response result to error code based on the error.
            m_module.setResult( l_response, l_dn, t ) ;
        }

        return l_response ;
    }
}
