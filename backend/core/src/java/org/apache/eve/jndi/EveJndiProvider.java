package org.apache.eve.jndi ;


import org.apache.eve.RootNexus;
import org.apache.eve.PartitionNexus;
import org.apache.eve.EveBackendSubsystem;

import java.util.Hashtable ;

import java.lang.reflect.Proxy ;
import java.lang.reflect.Method ;
import java.lang.reflect.InvocationHandler ;

import javax.naming.NamingException ;
import javax.naming.ldap.LdapContext ;


/**
 * EveBackendSubsystem service implementing block.
 * 
 */
public class EveJndiProvider implements EveBackendSubsystem, InvocationHandler
{
    /** Singleton instance of this class */
    private static EveJndiProvider s_singleton = null ;
    
    /** Interceptor of interceptors in post-invocation pipeline */
    private InterceptorPipeline m_after = new FailFastPipeline() ;
    /** Interceptor of interceptors in pre-invocation pipeline */
    private InterceptorPipeline m_before = new FailFastPipeline() ;
    /** Interceptor of interceptors in post-invocation pipeline failure */
    private InterceptorPipeline m_afterFailure  = new AfterFailurePipeline() ;
    /** RootNexus as it was given to us by the ServiceManager */
    private RootNexus m_nexus = null ;
    /** PartitionNexus proxy wrapping m_nexus to inject services */
    private PartitionNexus m_proxy = null ;


    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------


    /**
     * Creates a singlton instance of the EveBackendSubsystem.  In the words of the
     * Highlander, "there can only be one."
     *
     * @throws IllegalStateException if another EveJndiProvider has already
     * been instantiated.
     */
    public EveJndiProvider( RootNexus nexus )
    {
        if ( s_singleton != null )
        {
            throw new IllegalStateException(
                "Cannot instantiate more than one EveJndiProvider!" ) ;
        }

        s_singleton = this ;
        this.m_nexus = nexus;
        this.m_proxy = ( PartitionNexus ) Proxy.newProxyInstance(
            m_nexus.getClass().getClassLoader(),
            m_nexus.getClass().getInterfaces(), this ) ;

    }


    // ------------------------------------------------------------------------
    // Static Package Friendly Methods
    // ------------------------------------------------------------------------


    /**
     * Enables a EveContextFactory with a handle to the system wide
     * EveJndiProvider instance.
     *
     * @param a_factory the EveContextFactory to enable
     */
    static void setProviderOn( EveContextFactory a_factory )
    {
        a_factory.setProvider( s_singleton ) ;
    }


    // ------------------------------------------------------------------------
    // EveBackendSubsystem Interface Method Implemetations
    // ------------------------------------------------------------------------


    /**
     * @see org.apache.eve.EveBackendSubsystem#getLdapContext(Hashtable)
     */
    public LdapContext getLdapContext( Hashtable an_env ) throws NamingException
    {
        return new EveLdapContext( m_proxy, an_env ) ;
    }


    // ------------------------------------------------------------------------
    // Invokation Handler Implementation
    // ------------------------------------------------------------------------


    /**
     * @see java.lang.reflect.InvocationHandler#invoke(Object,Method,Object[])
     */
    public Object invoke( Object a_proxy, Method a_method, Object[] a_args )
        throws Throwable
    {
        // Setup the invocation and populate: remember aspect sets context stack
        Invocation l_invocation = new Invocation() ;
        l_invocation.setMethod( a_method ) ;
        l_invocation.setProxy( a_proxy ) ;
        l_invocation.setParameters( a_args ) ;
        
        try
        {
            m_before.invoke( l_invocation ) ;
        }
        catch ( Throwable a_throwable )
        {
            /*
             * On errors we need to continue into the failure handling state
             * of Invocation processing and not throw anything just record it.
             */
            if ( a_throwable instanceof InterceptorException )
            {
                l_invocation.setBeforeFailure( ( InterceptorException ) 
                    a_throwable ) ;
            }
            else 
            {
                InterceptorException l_ie = 
                    new InterceptorException( m_before, l_invocation ) ;
                l_invocation.setBeforeFailure( l_ie ) ;
                l_ie.setRootCause( a_throwable ) ;
            }
            
            l_invocation.setState( InvocationStateEnum.FAILUREHANDLING ) ;
        }

        
        /*
         * If before pipeline succeeds invoke the target and change state to 
         * POSTINVOCATION on success but on failure record exception and set 
         * state to FAILUREHANDLING.
         * 
         * If before pipeline failed then we invoke the after failure pipeline
         * and throw the before failure exception.
         */
        if ( InvocationStateEnum.PREINVOCATION == l_invocation.getState() )
        {
            try
            {
                l_invocation.setReturnValue( a_method.invoke( m_nexus, 
                    l_invocation.getParameters() ) ) ;
                l_invocation.setState( InvocationStateEnum.POSTINVOCATION ) ;
            }
            catch ( Throwable a_throwable )
            {
                l_invocation.setThrowable( a_throwable ) ;
                l_invocation.setState( InvocationStateEnum.FAILUREHANDLING ) ;
            }

            l_invocation.setComplete( true ) ;
        }
        else if ( 
            InvocationStateEnum.FAILUREHANDLING == l_invocation.getState() )
        {
            m_afterFailure.invoke( l_invocation ) ;
            throw l_invocation.getBeforeFailure() ;
        }


        /*
         * If we have gotten this far then the before pipeline succeeded.  If 
         * the target invocation succeeded then we should be in the 
         * POSTINVOCATION state in which case we invoke the after pipeline.
         * 
         * If the target invocation failed then we should run the after failure
         * pipeline since we will be in the FAILUREHANDLINE state and after 
         * doing so we throw the original throwable raised by the target.
         */
        if ( InvocationStateEnum.POSTINVOCATION == l_invocation.getState() )
        {
            try
            {
                m_after.invoke( l_invocation ) ;
                return l_invocation.getReturnValue() ;
            }
            catch ( Throwable a_throwable )
            {
                l_invocation.setState( InvocationStateEnum.FAILUREHANDLING ) ;
                
                if ( a_throwable instanceof InterceptorException )
                {
                    l_invocation.setAfterFailure( ( InterceptorException ) 
                        a_throwable ) ;
                }
                else 
                {
                    InterceptorException l_ie = 
                        new InterceptorException( m_after, l_invocation ) ;
                    l_ie.setRootCause( a_throwable ) ;
                    l_invocation.setAfterFailure( l_ie ) ;
                }
                
                m_afterFailure.invoke( l_invocation ) ;
                throw l_invocation.getAfterFailure() ;
            }
        }
        else if ( 
            InvocationStateEnum.FAILUREHANDLING == l_invocation.getState() 
            )
        {
            m_afterFailure.invoke( l_invocation ) ;
            
            if ( null != l_invocation.getThrowable() )
            {
                throw l_invocation.getThrowable() ;
            }
            else if ( null != l_invocation.getBeforeFailure() )
            {
                throw l_invocation.getBeforeFailure() ;
            }
            else if ( null != l_invocation.getAfterFailure() )
            {
                throw l_invocation.getAfterFailure() ;
            }
        }
        
        throw new IllegalStateException( "The EveJndiProvider's invocation "
            + "handler method invoke should never have reached this line" ) ;
    }
}
