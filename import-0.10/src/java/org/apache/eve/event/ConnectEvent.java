/*
 * $Id: ConnectEvent.java,v 1.2 2003/03/13 18:27:17 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.event ;


import java.net.Socket ;
import java.util.EventObject;


public class ConnectEvent
    extends EventObject
{
    public ConnectEvent(Socket a_socket)
    {
        super(a_socket) ;
    }
}
