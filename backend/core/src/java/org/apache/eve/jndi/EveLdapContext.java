package org.apache.eve.jndi ;


import java.util.Hashtable ;

import javax.naming.NamingException ;
import javax.naming.ldap.Control ;
import javax.naming.ldap.ExtendedRequest ;
import javax.naming.ldap.ExtendedResponse ;
import javax.naming.ldap.LdapContext ;

import org.apache.ldap.common.name.LdapName ;

import org.apache.eve.PartitionNexus;


/**
 *
 */
public class EveLdapContext extends EveDirContext implements LdapContext
{

    /**
     * TODO Document me!
     *
     * @param a_nexusProxy TODO
     * @param a_env TODO
     */
    public EveLdapContext( PartitionNexus a_nexusProxy, Hashtable a_env ) throws NamingException
    {
        super( a_nexusProxy, a_env ) ;
    }


    /**
     * Creates a new EveDirContext with a distinguished name which is used to
     * set the PROVIDER_URL to the distinguished name for this context.
     * 
     * @param a_nexusProxy the intercepting proxy to the nexus
     * @param a_env the environment properties used by this context
     * @param a_dn the distinguished name of this context
     */
    EveLdapContext( PartitionNexus a_nexusProxy, Hashtable a_env, LdapName a_dn )
    {
        super( a_nexusProxy, a_env, a_dn ) ;
    }


    /**
     * @see javax.naming.ldap.LdapContext#extendedOperation(
     * javax.naming.ldap.ExtendedRequest)
     */
    public ExtendedResponse extendedOperation( ExtendedRequest a_request )
    {
        // TODO Auto-generated method stub
        return null ;
    }


    /**
     * @see javax.naming.ldap.LdapContext#newInstance(
     * javax.naming.ldap.Control[])
     */
    public LdapContext newInstance( Control[] a_requestControls )
        throws NamingException
    {
        // TODO Auto-generated method stub
        return null ;
    }


    /**
     * @see javax.naming.ldap.LdapContext#reconnect(javax.naming.ldap.Control[])
     */
    public void reconnect( Control[] a_connCtls ) throws NamingException
    {
        // TODO Auto-generated method stub
    }


    /**
     * TODO Document me! 
     *
     * @see javax.naming.ldap.LdapContext#getConnectControls()
     */
    public Control[] getConnectControls() throws NamingException
    {
        // TODO Auto-generated method stub
        return null ;
    }


    /**
     * TODO Document me! 
     *
     * @see javax.naming.ldap.LdapContext#setRequestControls(
     * javax.naming.ldap.Control[])
     */
    public void setRequestControls( Control[] a_requestControls )
        throws NamingException
    {
        // TODO Auto-generated method stub
    }


    /**
     * TODO Document me! 
     *
     * @see javax.naming.ldap.LdapContext#getRequestControls()
     */
    public Control[] getRequestControls() throws NamingException
    {
        // TODO Auto-generated method stub
        return null ;
    }


    /**
     * TODO Document me! 
     *
     * @see javax.naming.ldap.LdapContext#getResponseControls()
     */
    public Control[] getResponseControls() throws NamingException
    {
        // TODO Auto-generated method stub
        return null ;
    }

}
