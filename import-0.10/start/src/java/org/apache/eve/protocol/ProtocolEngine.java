/*
 * $Id: ProtocolEngine.java,v 1.5 2003/08/22 21:15:56 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.protocol ;


import org.apache.eve.event.RequestListener ;
import org.apache.eve.client.ClientManagerSlave ;


/**
 * Service interface for an LDAP protocol engine.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.5 $
 */
public interface ProtocolEngine
    extends RequestListener, ClientManagerSlave
{
    /** The fully qualified name of this service interface */
    String ROLE = ProtocolEngine.class.getName() ;

    /**
     * Gets the highest LDAP protocol version supported by an implementation.
     * 
     * @return the highest supported protocol version.
     */
    int getProtocolVersion() ;
}
