/*
 * $Id: ClientManager.java,v 1.7 2003/08/22 21:15:55 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.client ;


import java.net.Socket ;
import java.io.IOException ;
import java.io.InputStream ;

import org.apache.eve.event.OutputListener ;
import org.apache.eve.event.ConnectListener ;
import org.apache.eve.security.LdapPrincipal ;
import javax.naming.ldap.LdapContext;


/**
 * Service interface used to manage clients, their connections and their
 * stateful sessions.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.7 $
 */
public interface ClientManager
    extends ConnectListener
{
    /** role played by this service interface */
    String ROLE = ClientManager.class.getName() ;

    /**
     * Drops a client connection and destroys the client's session.
     *
     * @param a_clientKey the key used to uniquely identify this client.
     */
    void drop( ClientKey a_clientKey ) ;

    /**
     * Drops a client connection and destroys the client's session associated
     * with the calling thread.
     */
    void drop() ;

    /**
     * Creates a client session using the client's unique key.
     * 
     * @param a_clientKey the client key used to uniquely identify the client.
     * @param a_socket a client socket to the server.
     */
    ClientSession add( ClientKey a_clientKey, Socket a_socket )
        throws IOException ;

    /**
     * Sets the user principal for a client on a bind operation.  The protocol
     * states that a bind operation requires the destruction of all outstanding
     * operations.  Hence all outstanding operations refered to by the client's
     * key must be terminated, the session parameters are flushed, then the
     * princal is set.  The client affectively changes its role without dropping
     * the socket connection.
     *
     * @param a_clientKey the client's unique key.
     * @param a_principal the new principal to be taken on by the client using
     * the existing socket connection.
     */
    ClientSession
        setUserPrincipal( ClientKey a_clientKey, LdapPrincipal a_principal ) ;

    /**
     * Explicit access to a client's session by the client's ClientKey.
     *
     * @param a_clientKey the client's unique key
     * @return the session associated with the client's key.
     */
    ClientSession getClientSession( ClientKey a_clientKey ) ;

    /**
     * Gets the client session associated with the calling thread's context.
     *
     * @return the client's session which the current thread is doing work for.
     */
    ClientSession getClientSession() ;

    /**
     * Gets the key of the client on whose behalf the current thread is
     * executing.
     *
     * @return the ClientKey associated with the callers thread or null if none
     * exists.
     */
	ClientKey getClientKey() ;

    /**
     * Gets the LdapContext associated with the calling thread.
     *
     * @return the context of the caller thread
     */
    LdapContext getLdapContext() ;

    /**
     * Associates an external thread using the JNDI provider with a LdapContext.
     *
     * @param a_ctx the LdapContext to be associated with the calling thread.
     */
    void threadAssociate( LdapContext a_ctx ) ;

    /**
     * Associates a protocol request handler driving thread with a client.
     *
     * @param a_clientKey the unique ClientKey associated with the request.
     */
    void threadAssociate( ClientKey a_clientKey ) ;

    /**
     * Disassociates a protocol request handler driving thread with a client.
     */
    void threadDisassociate() ;
}
