/*
 * $Id: InputManager.java,v 1.4 2003/03/13 18:27:30 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.input ;


import java.io.InputStream ;
import org.apache.eve.client.ClientKey ;
import org.apache.eve.client.ClientManagerSlave ;


/**
 * Service interface for server module that monitors incomming PDU requests on
 * a single client InputStream.  Only one stream per client can be registered
 * for input detection.  This service pervents a cyclic component dependency by
 * extending the ClientManagerSlave interface.
 */
public interface InputManager
    extends ClientManagerSlave
{
    /** Role played by this service as specified by Avalon */
    public static final String ROLE = InputManager.class.getName() ;

    /**
     * Registers a client with this module so that input detection can occur.
     *
     * @param a_clientKey the unique key identifing a client
     * @param a_clientIn the client InputStream to be monitored.
     */
    void register(ClientKey a_clientKey, InputStream a_clientIn) ;

    /**
     * Unregisters a client with this module so that input detection is not
     * enabled for a client's InputStream previously enabled via the register
     * method of this service.
     *
     * @param a_clientKey the unique key identifing a client
     */
    void unregister(ClientKey a_clientKey) ;
}
