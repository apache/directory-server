/*
 * $Id: InputEvent.java,v 1.2 2003/03/13 18:27:20 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.event ;

import java.util.EventObject ;
import java.io.InputStream ;
import org.apache.eve.client.ClientKey;


public class InputEvent
    extends EventObject
{
    private final InputStream m_in ;


    public InputEvent(ClientKey a_client, InputStream a_in)
    {
        super(a_client) ;
        m_in = a_in ;
    }


    public InputStream getInputStream()
    {
        return m_in ;
    }
}

