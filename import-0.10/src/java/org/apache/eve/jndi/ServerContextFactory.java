/*
 * $Id: ServerContextFactory.java,v 1.3 2003/08/22 21:15:55 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.jndi ;


import java.util.Hashtable ;

import javax.naming.Context ;
import javax.naming.NamingException ;


public class ServerContextFactory
    implements javax.naming.spi.InitialContextFactory
{
    public Context getInitialContext( Hashtable an_envoronment )
        throws NamingException
    {
        JndiProviderModule l_module = JndiProviderModule.getInstance() ;
        return l_module.getContext( an_envoronment ) ;
    }
}
