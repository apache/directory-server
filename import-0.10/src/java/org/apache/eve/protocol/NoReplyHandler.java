/*
 * $Id: NoReplyHandler.java,v 1.2 2003/08/22 21:15:56 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.protocol ;


import org.apache.ldap.common.message.Request ;


/**
 * Represents handlers that do not return a response to the sender.
 *
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.2 $
 */
public interface NoReplyHandler
    extends RequestHandler
{
    /**
     * Handles requests that do not reply to the requesting client with a
     * response.
     *
     * @param a_request the request without a response.
     */
    void handle( Request a_request ) ;
}
