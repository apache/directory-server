/*
 * $Id: ResponseEvent.java,v 1.3 2003/08/22 21:15:55 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.event ;


import java.util.EventObject ;

import org.apache.eve.client.ClientKey ;
import org.apache.ldap.common.message.Response ;


/**
 * An event representing the composition of a response to a client request.
 *
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.3 $
 */
public class ResponseEvent
    extends EventObject
{
    /** The Reponse composed for a client request. */
    private final Response m_response ;


    /**
     * Creates a ResponseEvent for a client identified by a key on a response.
     *
     * @param a_clientKey a unique client identifying key.
     * @param a_response the response this event announces the composition of.
     */
    public ResponseEvent( final ClientKey a_clientKey,
        final Response a_response )
    {
        super( a_clientKey ) ;
        m_response = a_response ;
    }


    /**
     * Gets the response object associated with this event.
     *
     * @return the LDAPv3 Response message.
     */
    public Response getResponse()
    {
        return m_response ;
    }
}
