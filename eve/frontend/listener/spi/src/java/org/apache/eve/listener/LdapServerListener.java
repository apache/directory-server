/*

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 1999-2002 The Apache Software Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:

 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.

 4. The names "Eve Directory Server", "Apache Directory Project", "Apache Eve" 
    and "Apache Software Foundation"  must not be used to endorse or promote
    products derived  from this  software without  prior written
    permission. For written permission, please contact apache@apache.org.

 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation. For more  information on the
 Apache Software Foundation, please see <http://www.apache.org/>.

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
