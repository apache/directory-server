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
package org.apache.ldap.server.invocation;


import java.io.Serializable;
import java.util.Stack;

import javax.naming.NamingException;

import org.apache.ldap.server.partition.BackingStore;


/**
 * Represents a method invocation on {@link BackingStore}s.
 * <p/>
 * This class is abstract, and developers should extend this class to
 * represent the actual method invocations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class Invocation implements Serializable
{

    protected transient Object returnValue;

    protected transient Stack contextStack;

    
    /**
     * Creates a new instance.  This constructor does nothing.
     */
    protected Invocation()
    {
    }


    /**
     * Returns the returnValue object for this invocation.
     */
    public Object getReturnValue()
    {
        return returnValue;
    }


    /**
     * Sets the returnValue object for this invocation.
     */
    public void setReturnValue( Object returnValue )
    {
        this.returnValue = returnValue;
    }


    /**
     * Gets the context stack in which this invocation occurs.  The
     * context stack is a stack of LdapContexts.
     *
     * @return a stack of LdapContexts in which the invocation occurs
     */
    public Stack getContextStack()
    {
        return contextStack;
    }


    /**
     * Sets the context stack in which this invocation occurs.  The context stack
     * is a stack of LdapContexts.
     *
     * @param contextStack a stack of LdapContexts in which the invocation occurs
     */
    public void setContextStack( Stack contextStack )
    {
        this.contextStack = contextStack;
    }


    /**
     * Executes this invocation on the specified <code>store</code>. The default
     * implementation calls an abstract method {@link #doExecute(BackingStore)}
     * and sets the <code>returnValue</code> property of this invocation to its return value.
     *
     * @throws NamingException if the operation failed
     */
    public void execute( BackingStore store ) throws NamingException
    {
        setReturnValue( doExecute( store ) );
    }


    /**
     * Implement this method to invoke the appropriate operation on the specified
     * <code>store</code>.  Returned value will be set as the <code>returnValue</code> proeprty of this invocation.
     *
     * @throws NamingException if the operation failed
     */
    protected abstract Object doExecute( BackingStore store ) throws NamingException;
}
