/*
 * $Id: ServerListener.java,v 1.4 2003/03/13 18:27:39 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.listener ;



import org.apache.eve.event.ConnectListener ;


/**
 * The ServiceListener service interface is implemented by server socket
 * listner modules in the LDAPd server.  It currently is designed to only
 * support one server socket.  Expect this interface to change in the near
 * future to accomodate multiple socket listeners and SSL enabled server
 * sockets.
 */
public interface ServerListener
{
    /** role of this service interface */
    public static final String ROLE = ServerListener.class.getName() ;

    /**
     * Gets the server socket backlog for this ServerListener
     *
     * @return client connection backlog
     */
    public int getBacklog() ;

    /**
     * Gets the TCP port number this ServerListener listens on.
     *
     * @return the tcp port number
     */
    public int getPort() ;

    /**
     * Gets the ip interface address this ServerListener listens on.
     *
     * @return the ip address octets as a String i.e. 127.0.0.1
     */
    public String getAddress() ;

    /**
     * Gets the LDAP URL for this ServerListener using the specified ip address
     * and the tcp port number listened to.  The ipaddress is resolved to a host
     * name.
     *
     * @return the LDAP URL like so ldap://localhost:389
     */
    public String getLdapURL() ;
}
