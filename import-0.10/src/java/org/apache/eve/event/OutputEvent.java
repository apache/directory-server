/*
 * $Id: OutputEvent.java,v 1.2 2003/03/13 18:27:21 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.event ;

import java.io.InputStream ;
import java.util.EventObject;
import org.apache.eve.client.ClientKey;


public class OutputEvent
    extends ClientEvent
{
    private final InputStream m_in ;

    public OutputEvent(final ClientKey a_client, final InputStream a_dataStream)
    {
        super(a_client, OUTPUT_EVENT) ;
        m_in = a_dataStream ;
    }


    public InputStream getInputStream()
    {
        return m_in ;
    }
}
