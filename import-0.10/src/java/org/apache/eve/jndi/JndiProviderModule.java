/*
 * $Id: JndiProviderModule.java,v 1.7 2003/08/22 21:15:55 akarasulu Exp $
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
import javax.naming.ldap.LdapContext ;

import org.apache.eve.AbstractModule ;
import org.apache.eve.backend.UnifiedBackend ;

import org.apache.avalon.framework.service.ServiceManager ;
import org.apache.avalon.framework.service.ServiceException ;
import org.apache.avalon.framework.configuration.Configuration ;
import org.apache.eve.client.ClientManager;


/**
 * Protocol engine stage: the request processing stage of the pipeline.
 * 
 * @phoenix:block
 * @phoenix:service name="org.apache.eve.jndi.JndiProvider"
 * @phoenix:mx-topic name="JndiProviderModule"
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.7 $
 */
public class JndiProviderModule
    extends AbstractModule
    implements JndiProvider
{
    private static JndiProviderModule s_singleton = null ;
    private UnifiedBackend m_nexus = null ;
    private ClientManager m_clientManager = null ;


    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------


    /**
     * Creates a singlton instance of the JndiProvider for the entire JVM.
     * Multiple calls to this method will raise an exception.
     *
     * @throws IllegalStateException if another JndiProvider has already been
     * initialized.
     */
    public JndiProviderModule()
    {
        if( s_singleton != null )
        {
            throw new IllegalStateException(
                "Cannot instantiate more than one Server-Side JndiProvider!" ) ;
        }

        s_singleton = this ;
    }


    static JndiProviderModule getInstance()
    {
        if( null == s_singleton )
        {
            throw new IllegalStateException(
                "Cannot return singleton instance without initialization!" ) ;
        }

        return s_singleton ;
    }


	/**
     * Creates a default context at the root DSE.
     */
    public LdapContext getContext( Hashtable an_environment )
        throws NamingException
    {
        return new UnifiedLdapContext( an_environment ) ;
    }



    // ------------------------------------------------------------------------
    // Avalon Lifecycle Methods & Other Module Interface Method Implementations
    // ------------------------------------------------------------------------


    /**
     * Gets a descriptive String for this JndiProvider implementation.
     * 
     * @phoenix:mx-attribute
     * @phoenix:mx-description Returns the implementation name.
     * @phoenix:mx-isWriteable no
     */
    public String getImplementationName()
    {
        return "Server-Side JNDI Provider Module" ;
    }


    /**
     * Gets the ROLE or fully qualified class name of the service interface
     * associated with this implementation.
     * 
     * @phoenix:mx-attribute
     * @phoenix:mx-description Returns the implementation role.
     * @phoenix:mx-isWriteable no
     */
    public String getImplementationRole()
    {
        return ROLE ;
    }


    /**
     * Gets the fully qualified class name implementation class.
     *
     * @phoenix:mx-attribute
     * @phoenix:mx-description Returns the implementation class name.
     * @phoenix:mx-isWriteable no
     */
    public String getImplementationClassName()
    {
        return this.getClass().getName() ;
    }


    public void configure( Configuration a_config )
    {
        // Does nothing for now.
    }


    /**
     * Gets a handle on the UnifiedBackend service aka NexusModule.
     *
     * @phoenix:dependency name="org.apache.eve.backend.UnifiedBackend"
     */
    public void service( ServiceManager a_manager )
        throws ServiceException
    {
        m_nexus = ( UnifiedBackend ) a_manager.lookup( UnifiedBackend.ROLE ) ;
    }


    public void stop()
    {
        s_singleton = null ;
    }


    // ------------------------------------------------------------------------
    // Package friendly methods used by contexts and their helpers
    // ------------------------------------------------------------------------


    /**
     * Gets the ClientManager this ClientManagerSlave subordinates to.
     *
     * @return the ClientManager master
     */
    ClientManager getClientManager()
    {
        return m_clientManager ;
    }


    /**
     * Gets a handle on the UnifiedBackend this module and its Contexts and
     * helpers need to perform operations against backends.
     *
     * @return a handle on the backend nexus
     */
    UnifiedBackend getNexus()
    {
        return m_nexus ;
    }
}
