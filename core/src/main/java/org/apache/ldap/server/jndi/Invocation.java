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
package org.apache.ldap.server.jndi;


import java.util.List;
import java.util.Stack;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.apache.ldap.server.jndi.InvocationStateEnum;
import org.apache.ldap.server.jndi.InvocationMethodEnum;


/**
 * Object representing a nexus method invocation through the Server Side JNDI
 * provider.
 * 
 * This class was originally written by Peter Donald for XInvoke from the Spice 
 * Group.
 * 
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class Invocation implements Serializable
{
    /**
     * The interceptor processing state of this Invocation.
     */
    private InvocationStateEnum state = InvocationStateEnum.PREINVOCATION;

    /**
     * The interceptor processing state of this Invocation.
     */
    private InvocationMethodEnum methodEnum;

    /**
     * The invokation state: when call has completed isCompleted will be true
     */
    private boolean isComplete = false;

    /**
     * When set to true the invocation on the target is bypassed.
     */
    private boolean bypass = false;

    /**
     * Thrown by the first interceptor to fail within the before invocation
     * InterceptorPipeline which is fail fast.
     */
    private Throwable before;
    
    /**
     * Thrown by the first interceptor to fail within the after invocation
     * InterceptorPipeline which is fail fast.
     */
    private Throwable after;
    
    /**
     * Exceptions thrown by interceptors within the failure pipeline which is 
     * NOT fail fast - hence the use of a list for potentially many exceptions.
     */
    private List failures;
    
    /**
     * The proxy on which the method was invoked.
     */
    private Object proxy;

    /**
     * The actual method that is being invoked.
     */
    private Method method;

    /**
     * The parameters of method invocation.
     */
    private Object[] parameters;

    /**
     * The return value of the invocation.
     */
    private Object returnValue;

    /**
     * The exception thrown by the invocation if any.
     */
    private Throwable throwable;

    /**
     * The context in which invocation occurs.
     */
    private Stack contextStack;
    
    
    // ------------------------------------------------------------------------
    // Accessor Mutator Methods 
    // ------------------------------------------------------------------------


    /**
     * Gets the state of this Invocation.
     *
     * @return the state 
     */
    public InvocationStateEnum getState()
    {
        return state;
    }
    

    /**
     * Sets the state of this Invocation.
     *
     * @param enum the new state to set
     */
    void setState( InvocationStateEnum enum )
    {
        state = enum;
    }


    /**
     * Gets the completion state of this invokation.
     *
     * @return true if the call on the proxied object has returned, false
     * otherwise
     */
    public boolean isComplete()
    {
        return isComplete;
    }


    /**
     * Sets the completion state of this invocation.  The invocation is complete
     * after the target method on the proxied object returns.
     *
     * @param isComplete whether or not the call on the proxied object
     * returned
     */
    void setComplete( boolean isComplete )
    {
        this.isComplete = isComplete;
    }


    /**
     * Gets whether or not this invokation is to be bypassed.
     *
     * @return true if the call on the proxied object is to be bypassed, false
     * otherwise
     */
    public boolean doBypass()
    {
        return bypass;
    }


    /**
     * Sets the whether or not the invocation on the proxied object is bypassed.
     *
     * @param bypass whether or not the call on the proxied object is bypassed
     */
    public void setBypass( boolean bypass )
    {
        this.bypass = bypass;
    }


    /**
     * Lists the Throwables thrown by interceptors within the failure pipeline
     * which is NOT fail fast - hence the use of a list for potentially many
     * exceptions.
     *
     * @return an Iterator over the exceptions produced by Interceptors
     */
    public Iterator listFailures()
    {
        if ( null == failures )
        {
            return Collections.EMPTY_LIST.iterator();
        }
        
        return Collections.unmodifiableList( failures ).iterator();
    }
    
    
    /**
     * Adds a throwable to the list of throwables resulting from the execution
     * of Interceptors in the order they were thrown.
     *
     * @param throwable Throwable resulting from an Interceptor invoke call
     */
    public void addFailure( Throwable throwable )
    {
        if ( null == failures )
        {
            failures = new ArrayList();
        }
        
        failures.add( throwable );
    }
    
    
    /**
     * Gets the Throwable thrown if at all within the before InterceptorPipeline.
     *
     * @return the Throwable thrown if at all within the before InterceptorPipeline
     */
    public Throwable getBeforeFailure()
    {
        return before;
    }
    
    
    /**
     * Sets the Throwable thrown if at all within the before InterceptorPipeline.
     *
     * @param before the Throwable thrown if at all within the before InterceptorPipeline
     */
    public void setBeforeFailure( Throwable before )
    {
        this.before = before;
    }
    
    
    /**
     * Gets the Throwable thrown if at all within the after InterceptorPipeline.
     *
     * @return the Throwable thrown if at all within the after InterceptorPipeline
     */
    public Throwable getAfterFailure()
    {
        return after;
    }
    
    
    /**
     * Sets the Throwable thrown if at all within the after InterceptorPipeline.
     *
     * @param after the Throwable thrown if at all within the after InterceptorPipeline
     */
    public void setAfterFailure( Throwable after )
    {
        this.after = after;
    }
    
    
    /**
     * Return the proxy on which the method was invoked.
     *
     * @return the proxy on which the method was invoked.
     */
    public Object getProxy()
    {
        return proxy;
    }


    /**
     * Set the proxy on which the method was invoked.
     *
     * @param proxy the proxy on which the method was invoked.
     */
    public void setProxy( final Object proxy )
    {
        this.proxy = proxy;
    }


    /**
     * Return the method that was invoked.
     *
     * @return the method that was invoked.
     */
    public InvocationMethodEnum getInvocationMethodEnum()
    {
        return methodEnum;
    }


    /**
     * Return the method that was invoked.
     *
     * @return the method that was invoked.
     */
    public Method getMethod()
    {
        return method;
    }


    /**
     * Set the method that was invoked.
     *
     * @param method the method that was invoked.
     */
    public void setMethod( final Method method )
    {
        this.method = method;
        this.methodEnum = InvocationMethodEnum.getInvocationMethodEnum( method );
    }


    /**
     * Return the parameters passed to method for invocation.
     *
     * @return the parameters passed to method for invocation.
     */
    public Object[] getParameters()
    {
        return parameters;
    }


    /**
     * Set the parameters passed to method for invocation.
     *
     * @param parameters the parameters passed to method for invocation.
     */
    public void setParameters( final Object[] parameters )
    {
        this.parameters = parameters;
    }


    /**
     * Return the return value of the invocation.
     *
     * @return the return value of the invocation.
     */
    public Object getReturnValue()
    {
        return returnValue;
    }


    /**
     * Set the return value of the invocation.
     *
     * @param returnValue the return value of the invocation.
     */
    public void setReturnValue( final Object returnValue )
    {
        this.returnValue = returnValue;
    }


    /**
     * Return the exception thrown by the invocation if any.
     *
     * @return the exception thrown by the invocation if any.
     */
    public Throwable getThrowable()
    {
        return throwable;
    }


    /**
     * Set the exception thrown by the invocation if any.
     *
     * @param throwable the exception thrown by the invocation if any.
     */
    public void setThrowable( Throwable throwable )
    {
        this.throwable = throwable;
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
    public void setContextStack( final Stack contextStack )
    {
        this.contextStack = contextStack;
    }
}
