package org.apache.eve.jndi ;


/**
 * A slow failing InterceptorPipeline implementation where all Interceptors 
 * within the invocation chain are invoked regardless of errors upstream - 
 * exceptions are added to the afterFailure list of exceptions on the 
 * Invocation.
 *
 */
public class AfterFailurePipeline extends InterceptorPipeline
{
    /**
     * Returns false all the time! 
     *
     * @see InterceptorPipeline#isFailFast()
     */
    public final boolean isFailFast() 
    {
        return false ;
    }


    /**
     * This invoke method does not fail and throw exceptions until all 
     * Interceptors within the pipeline have been invoked.  If an unexpected 
     * Throwable other than an InterceptorException results this method catches 
     * it and wraps it within an InterceptorException and adds it to the
     * Invocation's list of afterFailure exceptions.  The last error if any to 
     * result after all Interceptors have run is thrown.
     *
     * @see Interceptor#invoke(Invocation)
     */
    public void invoke( Invocation a_invocation ) throws InterceptorException
    {
        InterceptorException l_last = null ;
        
        for ( int ii = 0 ; ii < getList().size(); ii++ )
        {
            Interceptor l_service = ( Interceptor ) getList().get( ii ) ;
            
            try
            {
                l_service.invoke( a_invocation ) ;
            }
            catch ( Throwable a_throwable )
            {
                /*
                 * If exception is InterceptorException we add it to the list
                 * of afterFailure exceptions on the Invocation.  Otherwise we
                 * wrap the unexpected exception as an InterceptorException and
                 * add it to the list of afterFailure exceptions on the 
                 * Invocation
                 */

                if ( a_throwable instanceof InterceptorException )
                {
                    l_last = ( InterceptorException ) a_throwable ;
                    a_invocation.addFailure( l_last ) ;
                }
                else
                {
                    l_last = 
                        new InterceptorException( l_service, a_invocation ) ;
                    l_last.setRootCause( a_throwable ) ;
                    a_invocation.addFailure( l_last ) ;
                }
            }
        }
        
        // Throw the last excepts if any after all Interceptors are invoked
        if ( null != l_last )
        {
            throw l_last ;
        }
    }
}
