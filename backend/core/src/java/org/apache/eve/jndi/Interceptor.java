package org.apache.eve.jndi ;


/**
 * The Interceptor is a component through which invocations pass thru.  In 
 * most cases the invocations pass thru a series of Interceptor objects 
 * before the target object is invoked.
 * 
 * <p>Interceptors should not store any data relevent to a particular invocation
 * and potentially should not store any data relevent to a particular target 
 * object (It depends on the policy via which Interceptors are created via the
 * {@link org.realityforge.xinvoke.spi.ProxyManager}).</p>
 *
 * Peter Donald originally wrote this class for XInvoke from the Spice Project.
 * 
 * @author <a href="mailto:peter@realityforge.org">Peter Donald</a>
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 */
public interface Interceptor
{
    /**
     * Process a particular invocation.
     * The method must NEVER throw an exception and any exceptions should be 
     * caught and placed into the invocation via {@link Invocation#setThrowable}
     * or {@link Invocation#addInterceptorThrowable}.
     *
     * <p>Note: most Interceptors should pass control to the next Interceptor
     * in the series.</p>
     *
     * @param a_invocation the invocation to process
     * @throws InterceptorException on failures while handling the invokation
     */
    void invoke( Invocation a_invocation ) throws InterceptorException ;
}
