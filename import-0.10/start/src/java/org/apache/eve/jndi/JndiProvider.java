/*
 * $Id: JndiProvider.java,v 1.3 2003/03/13 18:27:32 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.jndi ;


import java.util.Hashtable ;

import javax.naming.NamingException ;
import javax.naming.ldap.LdapContext ;


public interface JndiProvider
{
    public static final String ROLE = JndiProvider.class.getName() ;

    LdapContext getContext(Hashtable an_environment)
        throws NamingException ;
}
