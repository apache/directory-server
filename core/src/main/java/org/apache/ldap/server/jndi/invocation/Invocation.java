package org.apache.ldap.server.jndi.invocation;

import java.util.Stack;

import javax.naming.NamingException;

import org.apache.ldap.server.BackingStore;

public abstract class Invocation {

    protected Object response;
    protected Stack contextStack;

    protected Invocation()
    {
    }
    
    /**
     * Returns the response object for this request.
     */
    public Object getResponse()
    {
        return response;
    }
    
    /**
     * Sets the response object for this request.
     */
    public void setResponse( Object response )
    {
        this.response = response;
    }
    
    /**
     * Gets the context stack in which this invocation occurs.  The context 
     * stack is a stack of LdapContexts.
     *
     * @return a stack of LdapContexts in which the invocation occurs
     */
    public Stack getContextStack()
    {
        return contextStack;
    }

    /**
     * Sets the context stack in which this invocation occurs.  The context 
     * stack is a stack of LdapContexts.
     *
     * @param contextStack a stack of LdapContexts in which the invocation
     * occurs
     */
    public void setContextStack( Stack contextStack )
    {
        this.contextStack = contextStack;
    }
    
    public void execute( BackingStore store ) throws NamingException
    {
        setResponse( doExecute( store ) );
    }
    
    protected abstract Object doExecute( BackingStore store ) throws NamingException;
}
