/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.ldap.server.jndi.invocation;

import java.util.Stack;

import javax.naming.NamingException;

import org.apache.ldap.server.BackingStore;

/**
 * Represents a method invocation on {@link BackingStore}s.
 * You can perform any {@link BackingStore} calls by invoking
 * {@link org.apache.ldap.server.jndi.JndiProvider#invoke(Invocation)}.
 * <p>
 * This class is abstract, and developers should extend this class
 * to represent the actual method invocations.
 * 
 * @author Apache Directory Project (dev@directory.apache.org)
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public abstract class Invocation {

    protected Object response;
    protected Stack contextStack;

	/**
	 * Creates a new instance.  This constructor does nothing.
	 */
    protected Invocation()
    {
    }
    
    /**
     * Returns the response object for this invocation.
     */
    public Object getResponse()
    {
        return response;
    }
    
    /**
     * Sets the response object for this invocation.
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
    
	/**
	 * Executes this invocation on the specified <code>store</code>.
	 * The default implementation calls an abstract method
	 * {@link #doExecute(BackingStore)} and sets the 
	 * <code>response</code> property of this invocation to its
	 * return value.
	 * 
	 * @throws NamingException if the operation failed
	 */
    public void execute( BackingStore store ) throws NamingException
    {
        setResponse( doExecute( store ) );
    }
    
	/**
	 * Implement this method to invoke the appropriate operation
	 * on the specified <code>store</code>.  Returned value will be
	 * set as the <code>response</code> proeprty of this invocation.
	 * 
	 * @throws NamingException if the operation failed
	 */
    protected abstract Object doExecute( BackingStore store ) throws NamingException;
}
