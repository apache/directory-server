package org.apache.eve.jndi;

import javax.naming.NamingException ;


/**
 * Exception thrown by an Interceptor while intercepting an Invocation.  
 * Interceptor failures caught from the 
 * {@link EveBackendSubsystem#invoke(Object, Method, Object[])}
 * method are bundled as InterceptorExceptions and rethrown.
 * 
 */
public class InterceptorException extends NamingException
{
    /** The Invokation the Interceptor failed on */
    private final Invocation m_invocation ;
    /** The Interceptor causing the failure */
    private final Interceptor m_interceptor ;
    
    
    /**
     * Creates an InterceptorException without a message.
     *
     * @param a_interceptor the Interceptor causing the failure
     * @param a_invocation the Invocation the Interceptor failed on
     */
    public InterceptorException( Interceptor a_interceptor,
        Invocation a_invocation )
    {
        super() ;
        m_invocation = a_invocation ;
        m_interceptor = a_interceptor ;
    }


    /**
     * Creates an InterceptorException with a custom message.
     *
     * @param a_interceptor the Interceptor causing the failure
     * @param a_invocation the Invocation the Interceptor failed on
     * @param a_explanation String explanation of why the Interceptor failed
     */
    public InterceptorException( Interceptor a_interceptor,
        Invocation a_invocation, String a_explanation )
    {
        super( a_explanation ) ;
        m_invocation = a_invocation ;
        m_interceptor = a_interceptor ;
    }
}