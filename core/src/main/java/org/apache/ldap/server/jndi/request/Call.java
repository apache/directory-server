package org.apache.ldap.server.jndi.request;

import java.util.Stack;

public abstract class Call {

    private Object response;
    private Stack contextStack;

    protected Call()
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
}
