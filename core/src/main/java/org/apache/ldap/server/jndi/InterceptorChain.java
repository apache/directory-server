package org.apache.ldap.server.jndi;

/**
 * Manages {@link Interceptor} stack.  The first {@link Interceptor} is
 * invoked and then invocation chain starts.
 * 
 * TODO imeplement me.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class InterceptorChain
{
    public Interceptor getInterceptor( String name )
    {
    }

    public void addFirst( String name, Interceptor interceptor )
    {
    }

    public void addLast( String name, Interceptor interceptor )
    {
    }

    public void addBefore( Interceptor other, String name, Interceptor interceptor )
    {
    }

    public void addAfter( Interceptor other, String name, Interceptor interceptor )
    {
    }
}
