/*
 * $Id: RequestEvent.java,v 1.3 2003/08/22 21:15:55 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.event ;


import java.util.EventObject ;

import org.apache.ldap.common.message.Request ;
import org.apache.eve.client.ClientKey ;


/**
 * Event which announces the arrival of a LDAPv3 client request.
 *
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.3 $
 */
public class RequestEvent
    extends EventObject
{
    /** The decoded request associated with this event. */
    private final Request m_request ;


    /**
     * Creates an event wrapper around a client key and a request for that
     * client.
     *
     * @param a_clientKey the unique key identifying a client.
     * @param a_request the request object associated with this event.
     */
    public RequestEvent( final ClientKey a_clientKey, final Request a_request )
    {
        super( a_clientKey ) ;
        m_request = a_request ;
    }


    /**
     * Gets the client request associated with this event.
     *
     * @return the client Request.
     */
    public Request getRequest()
    {
        return m_request ;
    }
}
