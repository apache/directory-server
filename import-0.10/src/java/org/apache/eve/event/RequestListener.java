/*
 * $Id: RequestListener.java,v 1.3 2003/03/13 18:27:24 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.event ;


import java.util.EventListener ;
import org.apache.avalon.framework.CascadingException ;


/**
 * Generic interface extended by the ProtocolEngine service interface to enable
 * the receipt of RequestEvents encapsulating a client's request PDU.
 */
public interface RequestListener
    extends EventListener
{
    /**
     * RequestEvent handler of the listener.
     *
     * @param an_event a RequestEvent encapsulating the client's request PDU.
     */
    void requestReceived(RequestEvent an_event) ;
}
