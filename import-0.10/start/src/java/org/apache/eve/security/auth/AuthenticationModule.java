/*
 * $Id: AuthenticationModule.java,v 1.8 2003/08/22 21:15:56 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.security.auth ;


import java.util.Iterator ;
import java.security.Principal ;

import javax.naming.Name ;
import javax.naming.NamingException ;
import javax.naming.NameNotFoundException ;

import org.apache.eve.AbstractModule ;
import org.apache.eve.backend.LdapEntry ;
import org.apache.eve.backend.AtomicBackend ;
import org.apache.eve.security.LdapPrincipal ;
import org.apache.eve.backend.UnifiedBackend ;
import org.apache.eve.backend.BackendException ;

import org.apache.avalon.framework.CascadingException ;
import org.apache.avalon.framework.service.ServiceManager ;
import org.apache.avalon.framework.service.ServiceException ;
import org.apache.avalon.framework.configuration.Configuration ;
import org.apache.avalon.framework.configuration.ConfigurationException ;


/**
 * Authenticates users against the backend.
 * 
 * @phoenix:block
 * @phoenix:service name="org.apache.eve.security.auth.AuthenticationManager"
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.8 $
 */
public class AuthenticationModule
    extends AbstractModule
    implements AuthenticationManager
{
	public static final String SIMPLE_PASSWORD_ATTR = "userPassword" ;
    /**
     * @todo Make sure we add a configuration parameter to enable and disable
     * annonymous binds.
     */
	private boolean allowsAnnonymousBind = true ;
	UnifiedBackend m_nexus = null ;


    public LdapPrincipal loginSimple( Name a_dn, String a_password )
        throws AuthenticationException, BackendException, NamingException
    {
        LdapPrincipal l_principal = null ;

        if(a_dn.toString().trim().equals("")) {
            if(allowsAnnonymousBind) {
                getLogger().info("Annonymous bind granted!") ;
				return new LdapPrincipal () ;
            } else {
                getLogger().info("Annoymous bind refused!") ;
                throw new AuthenticationException("Annonymous binds disabled") ;
            }
        }

		//
        // First let's check if the user is a backend admin user before pulling
        // records.
        //

        if( m_nexus.isAdminUser( a_dn ) ) {
            if(getLogger().isDebugEnabled()) {
                getLogger().debug(a_dn + " is an admin user.") ;
            }

            String l_password =
                m_nexus.getBackend( a_dn ).getAdminUserPassword() ;

            if(l_password.equals(a_password)) {
                l_principal = new LdapPrincipal (a_dn) ;
                return l_principal ;
            } else {
                String l_msg = "Authentication Failed: recognized admin "
                    + a_dn + " had invalid credentials!" ;
                getLogger().info(l_msg) ;
                throw new AuthenticationException(l_msg) ;
            }
        } else if(getLogger().isDebugEnabled()) {
            getLogger().debug(a_dn + " not recognized as an admin user.") ;
        }

        try {
			LdapEntry l_entry = m_nexus.read(a_dn) ;

            if(!l_entry.hasAttribute(SIMPLE_PASSWORD_ATTR)) {
                String l_msg = "Cannot authenticate " + a_dn +
                    " without a " + SIMPLE_PASSWORD_ATTR + " attribute!" ;
                getLogger().info(l_msg) ;
                throw new AuthenticationException(l_msg) ;
            }

            String l_password = (String)
                l_entry.getSingleValue(SIMPLE_PASSWORD_ATTR) ;

            if(null == l_password) {
                String l_msg = "User " + a_dn + " cannot be authenticated with"
                    + " with a null password!" ;
                getLogger().info(l_msg) ;
                throw new AuthenticationException(l_msg) ;
            }

			if(l_password.equals(a_password)) {
				l_principal = new LdapPrincipal (a_dn) ;
            } else {
                String l_msg = "User " + a_dn + " had invalid credentials!" ;
                getLogger().info(l_msg) ;

                if(getLogger().isDebugEnabled()) {
                	getLogger().debug(a_dn + " user provided password = " +
                        a_password + " entry password = " + l_password) ;
                }

                throw new AuthenticationException(l_msg) ;
            }
        } catch(BackendException e) {
            getLogger().error("Authentication failure due to backend: ", e) ;
            throw e ;
        } catch(NameNotFoundException e) {
            getLogger().error("Authentication failed: " + a_dn
                + " does not exist!") ;
            throw e ;
        }

        return l_principal ;
    }


    public void configure(Configuration a_config)
        throws ConfigurationException
    {
		// Does nothing - no configuration need right now.
    }


    public String getImplementationRole()
    {
        return ROLE ;
    }


    public String getImplementationName()
    {
        return "Simple Authentication Module" ;
    }


    public String getImplementationClassName()
    {
        return this.getClass().getName() ;
    }


    /**
     * Need handle on the nexus module - the unified backend.
     *
     * @phoenix:dependency name="org.apache.eve.backend.UnifiedBackend"
     */
    public void service(ServiceManager a_manager)
        throws ServiceException
    {
		m_nexus = (UnifiedBackend) a_manager.lookup(UnifiedBackend.ROLE) ;
    }
}

