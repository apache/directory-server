/*
 * $Id: OutputManager.java,v 1.4 2003/03/13 18:27:41 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.output ;


import java.io.OutputStream ;
import org.apache.eve.client.ClientKey ;
import org.apache.eve.event.OutputListener;
import org.apache.eve.client.ClientManagerSlave;
import java.io.InputStream;
import java.io.IOException;


public interface OutputManager
    extends OutputListener, ClientManagerSlave
{
    public static final String ROLE = OutputManager.class.getName() ;

    /**
     * Synchronous write which reads from the InputStream argument writing
     * it to the client's OutputStream.
     */
    public void write(ClientKey a_clientKey, InputStream an_in)
        throws IOException ;
    void register(ClientKey a_clientKey, OutputStream l_clientOut) ;
    void unregister(ClientKey a_clientKey) ;
}
