package org.apache.eve.jndi;


import org.apache.eve.RootNexus;
import org.apache.eve.PartitionNexus;
import org.apache.eve.EveBackendSubsystem;

import java.util.Hashtable;

import java.lang.reflect.Proxy;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationHandler;

import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;


/**
 * EveBackendSubsystem service implementing block.
 * 
 */
public class EveJndiProvider implements EveBackendSubsystem, InvocationHandler
{
    /** Singleton instance of this class */
    private static EveJndiProvider s_singleton = null;
    
    /** Interceptor of interceptors in post-invocation pipeline */
    private InterceptorPipeline after = new FailFastPipeline();
    /** Interceptor of interceptors in pre-invocation pipeline */
    private InterceptorPipeline before = new FailFastPipeline();
    /** Interceptor of interceptors in post-invocation pipeline failure */
    private InterceptorPipeline afterFailure  = new AfterFailurePipeline();
    /** RootNexus as it was given to us by the ServiceManager */
    private RootNexus nexus = null;
    /** PartitionNexus proxy wrapping nexus to inject services */
    private PartitionNexus proxy = null;


    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------


    /**
     * Creates a singlton instance of the EveBackendSubsystem.  In the words of
     * the Highlander, "there can only be one."
     *
     * @throws IllegalStateException if another EveJndiProvider has already
     * been instantiated.
     */
    public EveJndiProvider( RootNexus nexus )
    {
        if ( s_singleton != null )
        {
            throw new IllegalStateException(
                "Cannot instantiate more than one EveJndiProvider!" );
        }

        s_singleton = this;
        this.nexus = nexus;
        this.proxy = ( PartitionNexus ) Proxy.newProxyInstance(
            nexus.getClass().getClassLoader(),
            nexus.getClass().getInterfaces(), this );

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
        a_factory.setProvider( s_singleton );
    }


    // ------------------------------------------------------------------------
    // EveBackendSubsystem Interface Method Implemetations
    // ------------------------------------------------------------------------


    /**
     * @see org.apache.eve.EveBackendSubsystem#getLdapContext(Hashtable)
     */
    public LdapContext getLdapContext( Hashtable an_env ) throws NamingException
    {
        return new EveLdapContext( proxy, an_env );
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
        Invocation invocation = new Invocation();
        invocation.setMethod( a_method );
        invocation.setProxy( a_proxy );
        invocation.setParameters( a_args );
        
        try
        {
            before.invoke( invocation );
        }
        catch ( Throwable a_throwable )
        {
            /*
             * On errors we need to continue into the failure handling state
             * of Invocation processing and not throw anything just record it.
             */
            if ( a_throwable instanceof InterceptorException )
            {
                invocation.setBeforeFailure( ( InterceptorException )
                    a_throwable );
            }
            else 
            {
                InterceptorException ie =
                    new InterceptorException( before, invocation );
                invocation.setBeforeFailure( ie );
                ie.setRootCause( a_throwable );
            }
            
            invocation.setState( InvocationStateEnum.FAILUREHANDLING );
        }

        
        /*
         * If before pipeline succeeds invoke the target and change state to 
         * POSTINVOCATION on success but on failure record exception and set 
         * state to FAILUREHANDLING.
         * 
         * If before pipeline failed then we invoke the after failure pipeline
         * and throw the before failure exception.
         */
        if ( InvocationStateEnum.PREINVOCATION == invocation.getState() )
        {
            try
            {
                invocation.setReturnValue( a_method.invoke( nexus,
                    invocation.getParameters() ) );
                invocation.setState( InvocationStateEnum.POSTINVOCATION );
            }
            catch ( Throwable a_throwable )
            {
                invocation.setThrowable( a_throwable );
                invocation.setState( InvocationStateEnum.FAILUREHANDLING );
            }

            invocation.setComplete( true );
        }
        else if ( 
            InvocationStateEnum.FAILUREHANDLING == invocation.getState() )
        {
            afterFailure.invoke( invocation );
            throw invocation.getBeforeFailure();
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
        if ( InvocationStateEnum.POSTINVOCATION == invocation.getState() )
        {
            try
            {
                after.invoke( invocation );
                return invocation.getReturnValue();
            }
            catch ( Throwable a_throwable )
            {
                invocation.setState( InvocationStateEnum.FAILUREHANDLING );
                
                if ( a_throwable instanceof InterceptorException )
                {
                    invocation.setAfterFailure( ( InterceptorException )
                        a_throwable );
                }
                else 
                {
                    InterceptorException ie =
                        new InterceptorException( after, invocation );
                    ie.setRootCause( a_throwable );
                    invocation.setAfterFailure( ie );
                }
                
                afterFailure.invoke( invocation );
                throw invocation.getAfterFailure();
            }
        }
        else if ( 
            InvocationStateEnum.FAILUREHANDLING == invocation.getState()
            )
        {
            afterFailure.invoke( invocation );
            
            if ( null != invocation.getThrowable() )
            {
                throw invocation.getThrowable();
            }
            else if ( null != invocation.getBeforeFailure() )
            {
                throw invocation.getBeforeFailure();
            }
            else if ( null != invocation.getAfterFailure() )
            {
                throw invocation.getAfterFailure();
            }
        }
        
        throw new IllegalStateException( "The EveJndiProvider's invocation "
            + "handler method invoke should never have reached this line" );
    }
}
