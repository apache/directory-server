/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.eve.listener ;



/**
 * A server listener specification interface.
 * 
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
 * @version $Rev$
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
    public byte[] getAddress() ;
    
    /**
     * Gets whether or not the listener is for establishing secure connections.
     * 
     * @return true if ssl is used to secure the connection, false otherwise.
     */
    public boolean isSecure() ;
    
    /**
     * Get whether or not the transport is reliable or not.
     * 
     * @return true if it is reliable (TCP), false if it is not.
     */
    public boolean isReliable() ;
    
    /**
     * Gets the protocol's URL scheme.
     * 
     * @return the URL scheme for the protocol
     */
    public String getProtocolUrlScheme() ;

    /**
     * Gets the URL for this ServerListener using the specified ip address and 
     * the tcp port number listened to.  The ipaddress is resolved to a host 
     * name.
     *
     * @return the LDAP URL like so ldap://localhost:389
     */
    public String getURL() ;
}
