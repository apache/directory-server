/*
 * $Id: AuthenticationEvent.java,v 1.2 2003/03/13 18:27:14 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.event ;


import java.util.EventObject ;

import org.apache.eve.client.ClientKey ;
import java.security.Principal ;


public class AuthenticationEvent
    extends EventObject
{
    final Principal m_principal ;


    public AuthenticationEvent(ClientKey a_client, Principal a_principal)
    {
        super(a_client) ;
        m_principal = a_principal ;
    }


    public Principal getPrincipal()
    {
        return m_principal ;
    }
}
