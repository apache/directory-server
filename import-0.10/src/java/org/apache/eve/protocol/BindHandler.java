/*
 * $Id: BindHandler.java,v 1.2 2003/08/22 21:15:56 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.protocol ;


import javax.naming.Name ;
import javax.naming.NamingException ;
import javax.naming.InvalidNameException ;
import javax.naming.NameNotFoundException ;

import org.apache.ldap.common.message.BindRequest ;
import org.apache.ldap.common.message.BindResponse ;
import org.apache.ldap.common.message.ResultCodeEnum ;
import org.apache.ldap.common.message.ResultResponse ;
import org.apache.ldap.common.message.MessageTypeEnum ;
import org.apache.ldap.common.message.BindResponseImpl ;
import org.apache.ldap.common.message.SingleReplyRequest ;

import org.apache.eve.client.ClientKey ;
import org.apache.eve.client.ClientManager ;
import org.apache.eve.security.LdapPrincipal ;
import org.apache.eve.backend.BackendException ;
import org.apache.eve.security.auth.AuthenticationManager ;
import org.apache.eve.security.auth.AuthenticationException ;


/**
 * BindRequest handler for bind protocol requests.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.2 $
 */
public class BindHandler
	implements SingleReplyHandler
{
    /** The protocol module this request handler is part of */
    private final ProtocolModule m_module ;


    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------


    /**
     * Creates a BindRequest protocol data unit handler.
     *
     * @param a_module the module this handler is associated with.
     */
    public BindHandler( ProtocolModule a_module )
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
		return MessageTypeEnum.BINDREQUEST ;
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
        return MessageTypeEnum.BINDRESPONSE ;
    }


    /**
     * Handles a request that generates a sole response by returning the
     * response object back to the caller.
     *
     * @param a_request the request to handle.
     * @return the response to the request argument.
     * @throws ClassCastException if a_request is not a BindRequest
     */
    public ResultResponse handle( SingleReplyRequest a_request )
    {
        Name l_dn = null ;
		BindRequest l_request = ( BindRequest ) a_request ;

        // Prepare the response wrapper ahead of time.
        BindResponse l_response =
            new BindResponseImpl( l_request.getMessageId() ) ;

		try
		{
			l_dn = m_module.getName( l_request.getName() ) ;
		}
		// Respond to the user with an INVALIDDNSYNTAX error response
		catch( NamingException ne )
		{
			String l_msg = "'" + l_request.getName() + "' for the bind"
				+ " user's DN does not conform to the DN syntax." ;
			m_module.setResult( l_response, ResultCodeEnum.INVALIDDNSYNTAX,
				l_msg, ne ) ;
			return l_response ;
		}

		// Check if unsupported SASL mechanisms are being used.  Use of
        // SASL mechanisms result in a AUTHMETHODNOTSUPPORTED error.
		if( ! l_request.isSimple() )
        {
            m_module.setResult( l_response,
                ResultCodeEnum.AUTHMETHODNOTSUPPORTED, l_dn,
                "SASL based authentication methods not supported!", null ) ;
    		return l_response ;
        }

		// Perform simple authentication using the authentication manager
		try
        {
            AuthenticationManager l_am = m_module.getAuthenticationManager() ;
            LdapPrincipal l_principal = l_am.loginSimple( l_dn,
                new String( l_request.getCredentials() ) ) ;
            ClientManager l_cm = m_module.getClientManager() ;
            ClientKey l_clientKey = l_cm.getClientSession().getClientKey() ;
            l_cm.setUserPrincipal( l_clientKey, l_principal ) ;
            m_module.setResult( l_response, l_dn ) ;
        }
        catch( InvalidNameException ine )
        {
            String l_msg = "InvalidNameException should never be thrown by the "
                + "AuthenticationManager since we have already verified a "
                + "correct Dn syntax." ;
            m_module.setResult( l_response, ResultCodeEnum.INVALIDDNSYNTAX,
                l_dn, l_msg, ine ) ;
        }
        catch( NameNotFoundException nnfe )
        {
            String l_msg = "NameNotFoundException should never be thrown by the"
                + " AuthenticationManager since we have already verified the "
                + "exsitiance of the entry '" + l_request.getName() + "'" ;
            m_module.setResult( l_response, ResultCodeEnum.NOSUCHOBJECT, l_dn,
                l_msg, nnfe ) ;
        }
        catch( NamingException ne )
        {
            m_module.setResult( l_response, l_dn, ne ) ;
        }
        catch( BackendException be )
        {
            m_module.setResult( l_response, l_dn, be ) ;
        }
        catch( AuthenticationException ae )
        {
            String l_msg = "'" + l_request.getName() + "' is either not the "
                + "correct username associated with the supplied password." ;
            m_module.setResult( l_response, ResultCodeEnum.INVALIDCREDENTIALS,
                l_dn, l_msg, ae ) ;
        }
        catch( IllegalArgumentException iae )
        {
            String l_msg = "No backend for the namespace associated with '"
                + l_request.getName() + "' exists.  Check your backend "
                + "configuration to determine the suffix to use." ;
            m_module.setResult( l_response, ResultCodeEnum.NOSUCHOBJECT,
                l_dn, l_msg, iae ) ;
        }

        return l_response ;
    }
}
