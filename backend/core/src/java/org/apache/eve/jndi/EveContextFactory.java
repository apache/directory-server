package org.apache.eve.jndi;


import java.util.Hashtable ;

import javax.naming.Context ;
import javax.naming.NamingException ;
import javax.naming.spi.InitialContextFactory;

import org.apache.ldap.common.NotImplementedException;


/**
 * An LDAPd server-side provider implementation of a InitialContextFactory.
 *
 *         Hashtable env = new Hashtable() ;
        env.put( Context.PROVIDER_URL, "ou=system" ) ;
        env.put( Context.INITIAL_CONTEXT_FACTORY,
                "org.apache.eve.jndi.EveContextFactory" ) ;
        InitialContext initialContext = new InitialContext( env ) ;

 * @see javax.naming.spi.InitialContextFactory
 */
public class EveContextFactory implements InitialContextFactory
{
    /** The singleton EveJndiProvider instance */
    private EveJndiProvider m_provider = null ;

    
    /**
     * Default constructor that sets the provider of this EveContextFactory.
     */
    public EveContextFactory()
    {
        EveJndiProvider.setProviderOn( this ) ;
    }
    
    
    /**
     * Enables this EveContextFactory with a handle to the EveJndiProvider
     * singleton.
     * 
     * @param a_provider the system's singleton EveBackendSubsystem service.
     */
    void setProvider( EveJndiProvider a_provider )
    {
        m_provider = a_provider ;
    }
    
    
    /**
     * @see javax.naming.spi.InitialContextFactory#getInitialContext(
     * java.util.Hashtable)
     */
    public Context getInitialContext( Hashtable an_envoronment )
        throws NamingException
    {
        // fire up the backend subsystem if we need to
        if ( null == m_provider )
        {
            throw new NotImplementedException() ;
        }
        
        return m_provider.getLdapContext( an_envoronment ) ;
    }
}
