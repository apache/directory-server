/*
 * $Id: AuthenticationManager.java,v 1.5 2003/08/22 21:15:56 akarasulu Exp $
 * $Prologue$
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.security.auth ;


import org.apache.avalon.framework.component.Component ;
import java.security.Principal ;
import javax.naming.NamingException ;
import org.apache.avalon.framework.CascadingException;
import org.apache.eve.backend.BackendException;
import javax.naming.Name ;
import org.apache.eve.security.LdapPrincipal ;


/**
 * A service interface for centralized authentication management.  At this point
 * this is a very simple stub whose interface will definately change to take
 * advantage of JAAS LoginModules and stack PAM via Java extended security
 * serivices.  For now we enable simple clear text authentication for simple
 * binds via a loginSimple() interface.
 */
public interface AuthenticationManager
    extends Component
{
    public static final String ROLE = AuthenticationManager.class.getName() ;

    LdapPrincipal loginSimple( Name a_dn, String a_clearTextPassword )
        throws AuthenticationException, BackendException, NamingException ;
}

