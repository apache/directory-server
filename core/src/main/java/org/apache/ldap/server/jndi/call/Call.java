package org.apache.ldap.server.jndi.call;

import java.util.Stack;

import javax.naming.NamingException;

import org.apache.ldap.server.BackingStore;

public abstract class Call {

    protected final BackingStore store;
    protected Object response;
    protected Stack contextStack;

    protected Call( BackingStore store )
    {
        if( store == null )
        {
            throw new NullPointerException( "store" );
        }

        this.store = store;
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
    
    public void execute() throws NamingException
    {
        setResponse( doExecute() );
    }
    
    protected abstract Object doExecute() throws NamingException;
}
