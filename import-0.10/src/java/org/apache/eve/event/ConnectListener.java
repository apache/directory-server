/*
 * $Id: ConnectListener.java,v 1.3 2003/03/13 18:27:18 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.event ;


import java.util.EventListener ;


public interface ConnectListener
    extends EventListener
{
    void connectPerformed(ConnectEvent an_event) ;
}

