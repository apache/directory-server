/*
 * $Id: ClientEvent.java,v 1.2 2003/03/13 18:27:16 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.event ;


import java.net.Socket ;
import java.util.EventObject ;
import org.apache.eve.client.ClientKey ;


public class ClientEvent
    extends EventObject
{
    public final static int DROP_EVENT = 0 ;
    public final static int ADD_EVENT = 1 ;
    public final static int OUTPUT_EVENT = 2 ;
    public final static int INPUT_EVENT = 3 ;
    public final static int REQUEST_EVENT = 4 ;
    public final static int RESPONSE_EVENT = 5 ;
    public final static int AUTH_EVENT = 6 ;

    public final int type ;


    public ClientEvent(ClientKey a_client, int a_type)
    {
        super(a_client) ;
        type = a_type ;
    }


    public ClientKey getClientKey()
    {
        return (ClientKey) this.source ;
    }


    public int getType()
    {
        return type ;
    }
}
