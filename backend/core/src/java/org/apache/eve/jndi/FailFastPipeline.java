package org.apache.eve.jndi ;


/**
 * A fast failing InterceptorPipeline implementation where the first Interceptor
 * to fail within the invocation chain of the pipeline shorts the invocation of
 * interceptors downstream of the error.
 *
 */
public class FailFastPipeline extends InterceptorPipeline
{
    /**
     * Returns true all the time! 
     *
     * @see org.apache.ldap.server.jndi.InterceptorPipeline#isFailFast()
     */
    public final boolean isFailFast() 
    {
        return true ;
    }


    /**
     * This invoke method fails and throws at the first failure within the 
     * pipeline.  If an unexpected Throwable other than an InterceptorException
     * results this method catches it and wraps it within an 
     * InterceptorException and rethrows the new exception.
     *
     * @see org.apache.ldap.server.jndi.Interceptor#invoke(org.apache.ldap.server.jndi.Invocation)
     */
    public void invoke( Invocation a_invocation ) throws InterceptorException
    {
        for ( int ii = 0 ; ii < getList().size(); ii++ )
        {
            Interceptor l_service = ( Interceptor ) getList().get( ii ) ;
            
            try
            {
                l_service.invoke( a_invocation ) ;
            }
            catch ( Throwable a_throwable )
            {
                if ( a_throwable instanceof InterceptorException )
                {
                    throw ( InterceptorException ) a_throwable ;
                }
                
                InterceptorException l_ie = 
                    new InterceptorException( l_service, a_invocation ) ;
                l_ie.setRootCause( a_throwable ) ;
                throw l_ie ;
            }
        }
    }
}
