/*
 * $Id: RequestHandler.java,v 1.2 2003/08/22 21:15:56 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.protocol ;


import org.apache.ldap.common.message.MessageTypeEnum ;


/**
 * Root of all request handler types.
 *
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.2 $
 */
public interface RequestHandler
{
    /**
     * Gets the handler type.
     *
     * @return a HandlerTypeEnum constant.
     */
    HandlerTypeEnum getHandlerType() ;

    /**
     * Gets the request message type handled by this handler.
     *
     * @return a MessageTypeEnum constant associated with the request message.
     */
    MessageTypeEnum getRequestType() ;
}
