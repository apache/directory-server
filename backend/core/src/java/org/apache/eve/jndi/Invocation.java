package org.apache.eve.jndi;


import java.util.List;
import java.util.Stack ;
import java.util.Iterator ;
import java.util.ArrayList ;
import java.util.Collections ;

import java.io.Serializable ;
import java.lang.reflect.Method ;


/**
 * Object representing a nexus method invocation through the Server Side JNDI
 * provider.
 * 
 * This class was originally written by Peter Donald for XInvoke from the Spice 
 * Group.
 * 
 * @author <a href="mailto:peter at realityforge.org">Peter Donald</a>
 * @author <a href="mailto:aok123 at bellsouth.net">Alex Karasulu</a>
 */
public class Invocation
    implements Serializable
{
    /**
     * The interceptor processing state of this Invocation.
     */
    private InvocationStateEnum m_state = InvocationStateEnum.PREINVOCATION ; 
    
    /**
     * The invokation state: when call has completed m_isCompleted will be true
     */
    private boolean m_isComplete = false ;
    
    /**
     * InterceptorException thrown by the first interceptor to fail within the 
     * after invocation InterceptorPipeline which is fail fast.
     */
    private InterceptorException m_before ;
    
    /**
     * InterceptorException thrown by the first interceptor to fail within the 
     * after invocation InterceptorPipeline which is fail fast.
     */
    private InterceptorException m_after ;
    
    /**
     * Exceptions thrown by interceptors within the failure pipeline which is 
     * NOT fail fast - hence the use of a list for potentially many exceptions.
     */
    private List m_failures ;
    
    /**
     * The proxy on which the method was invoked.
     */
    private Object m_proxy ;

    /**
     * The actual method that is being invoked.
     */
    private Method m_method ;

    /**
     * The parameters of method invocation.
     */
    private Object[] m_parameters ;

    /**
     * The return value of the invocation.
     */
    private Object m_returnValue ;

    /**
     * The exception thrown by the invocation if any.
     */
    private Throwable m_throwable ;

    /**
     * The context in which invocation occurs.
     */
    private Stack m_contextStack ;
    
    
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
        return m_state ;
    }
    

    /**
     * Sets the state of this Invocation.
     *
     * @param a_enum the new state to set
     */
    void setState( InvocationStateEnum a_enum )
    {
        m_state = a_enum ;
    }


    /**
     * Gets the completion state of this invokation.  
     *
     * @return true if the call on the proxied object has returned, false 
     * otherwise 
     */
    public boolean isComplete()
    {
        return m_isComplete ;
    }


    /**
     * Sets the completion state of this invocation.  The invocation is complete
     * after the target method on the proxied object returns.
     *
     * @param a_isComplete whether or not the call on the proxied object 
     * returned
     */
    void setComplete( boolean a_isComplete )
    {
        m_isComplete = a_isComplete ;
    }
    

    /**
     * Lists  the InterceptorExceptions thrown by interceptors within the 
     * failure pipeline which is NOT fail fast - hence the use of a list for 
     * potentially many exceptions.
     *
     * @return an Iterator over the exceptions produced by Interceptors
     */
    public Iterator listFailures()
    {
        if ( null == m_failures )
        {
            return Collections.EMPTY_LIST.iterator() ;
        }
        
        return Collections.unmodifiableList( m_failures ).iterator() ;
    }
    
    
    /**
     * Adds a throwable to the list of throwables resulting from the execution
     * of Interceptors in the order they were thrown.
     *
     * @param a_throwable Throwable resulting from an Interceptor invoke call
     */
    public void addFailure( InterceptorException a_throwable )
    {
        if ( null == m_failures )
        {
            m_failures = new ArrayList() ;
        }
        
        m_failures.add( a_throwable ) ; 
    }
    
    
    /**
     * Gets the InterceptorException thrown if at all within the before
     * InterceptorPipeline.
     *
     * @return the InterceptorException thrown if at all within the before 
     * InterceptorPipeline 
     */
    public InterceptorException getBeforeFailure()
    {
        return m_before ;
    }
    
    
    /**
     * Sets the InterceptorException thrown if at all within the before
     * InterceptorPipeline.
     *
     * @param a_before the InterceptorException thrown if at all within the 
     * before InterceptorPipeline 
     */
    public void setBeforeFailure( InterceptorException a_before )
    {
        m_before = a_before ;
    }
    
    
    /**
     * Gets the InterceptorException thrown if at all within the after
     * InterceptorPipeline.
     *
     * @return the InterceptorException thrown if at all within the after 
     * InterceptorPipeline 
     */
    public InterceptorException getAfterFailure()
    {
        return m_after ;
    }
    
    
    /**
     * Sets the InterceptorException thrown if at all within the after
     * InterceptorPipeline.
     *
     * @param a_after the InterceptorException thrown if at all within the 
     * after InterceptorPipeline 
     */
    public void setAfterFailure( InterceptorException a_after )
    {
        m_after = a_after ;
    }
    
    
    /**
     * Return the proxy on which the method was invoked.
     *
     * @return the proxy on which the method was invoked.
     */
    public Object getProxy()
    {
        return m_proxy ;
    }


    /**
     * Set the proxy on which the method was invoked.
     *
     * @param a_proxy the proxy on which the method was invoked.
     */
    public void setProxy( final Object a_proxy )
    {
        m_proxy = a_proxy ;
    }


    /**
     * Return the method that was invoked.
     *
     * @return the method that was invoked.
     */
    public Method getMethod()
    {
        return m_method ;
    }


    /**
     * Set the method that was invoked.
     *
     * @param a_method the method that was invoked.
     */
    public void setMethod( final Method a_method )
    {
        m_method = a_method ;
    }


    /**
     * Return the parameters passed to method for invocation.
     *
     * @return the parameters passed to method for invocation.
     */
    public Object[] getParameters()
    {
        return m_parameters ;
    }


    /**
     * Set the parameters passed to method for invocation.
     *
     * @param a_parameters the parameters passed to method for invocation.
     */
    public void setParameters( final Object[] a_parameters )
    {
        m_parameters = a_parameters ;
    }


    /**
     * Return the return value of the invocation.
     *
     * @return the return value of the invocation.
     */
    public Object getReturnValue()
    {
        return m_returnValue ;
    }


    /**
     * Set the return value of the invocation.
     *
     * @param a_returnValue the return value of the invocation.
     */
    public void setReturnValue( final Object a_returnValue )
    {
        m_returnValue = a_returnValue ;
    }


    /**
     * Return the exception thrown by the invocation if any.
     *
     * @return the exception thrown by the invocation if any.
     */
    public Throwable getThrowable()
    {
        return m_throwable ;
    }


    /**
     * Set the exception thrown by the invocation if any.
     *
     * @param a_throwable the exception thrown by the invocation if any.
     */
    public void setThrowable( Throwable a_throwable )
    {
        m_throwable = a_throwable ;
    }


    /**
     * Gets the context stack in which this invocation occurs.  The context 
     * stack is a stack of LdapContexts.
     *
     * @return a stack of LdapContexts in which the invocation occurs
     */
    public Stack getContextStack()
    {
        return m_contextStack ;
    }


    /**
     * Sets the context stack in which this invocation occurs.  The context 
     * stack is a stack of LdapContexts.
     *
     * @param a_contextStack a stack of LdapContexts in which the invocation 
     * occurs
     */
    public void setContextStack( final Stack a_contextStack )
    {
        m_contextStack = a_contextStack ;
    }
}
