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

import java.net.InetAddress;
import java.net.UnknownHostException;


/**
 * An LDAP specific server listener.
 *
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class LdapServerListener extends DefaultServerListener
{
    /**
     * Creates an LDAP specific server listener.
     *  
     * @param a_hostname the hostname
     * @param a_port the TCP port 
     * @param a_backlog the connection backlog
     * @param a_secure whether or not ssl is used
     */
    public LdapServerListener( String a_hostname, int a_port, int a_backlog,
        boolean a_secure )
        throws UnknownHostException
    {
        super( InetAddress.getByName( a_hostname ).getAddress(), 
                a_secure ? "ldaps" : "ldap" , a_port, a_backlog,
				a_secure, true ) ;
    }


    /**
     * Creates a default LDAP specific server listener configured for TCP port 
     * 389 using the address of the local host whatever that may be as defined 
     * by the naming system.
     */
    public LdapServerListener()
        throws UnknownHostException
    {
        super( InetAddress.getLocalHost().getAddress(), 
                "ldap", 389, 0, false, true ) ;
    }
}
