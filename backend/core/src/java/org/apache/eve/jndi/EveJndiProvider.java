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
package org.apache.eve.jndi;


import org.apache.eve.RootNexus;
import org.apache.eve.PartitionNexus;
import org.apache.eve.EveBackendSubsystem;
import org.apache.eve.exception.EveNamingException;
import org.apache.ldap.common.message.ResultCodeEnum;

import java.util.Hashtable;

import java.lang.reflect.Proxy;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationHandler;

import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;


/**
 * The EveBackendSubsystem service implementation.
 * 
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
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
    private InterceptorPipeline afterFailure = new OnErrorPipeline();
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
     * @param factory the EveContextFactory to enable
     */
    static void setProviderOn( EveContextFactory factory )
    {
        factory.setProvider( s_singleton );
    }


    // ------------------------------------------------------------------------
    // EveBackendSubsystem Interface Method Implemetations
    // ------------------------------------------------------------------------


    /**
     * @see org.apache.eve.EveBackendSubsystem#getLdapContext(Hashtable)
     */
    public LdapContext getLdapContext( Hashtable aenv ) throws NamingException
    {
        return new EveLdapContext( proxy, aenv );
    }


    // ------------------------------------------------------------------------
    // Invokation Handler Implementation
    // ------------------------------------------------------------------------


    /**
     * @see java.lang.reflect.InvocationHandler#invoke(Object,Method,Object[])
     */
    public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable
    {
        // Setup the invocation and populate: remember aspect sets context stack
        Invocation invocation = new Invocation();
        invocation.setMethod( method );
        invocation.setProxy( proxy );
        invocation.setParameters( args );

        // used for an optimization
        BaseInterceptor.setInvocation( invocation );

        try
        {
            before.invoke( invocation );
        }
        catch ( Throwable throwable )
        {
            /*
             * On errors we need to continue into the failure handling state
             * of Invocation processing and not throw anything just record it.
             */
            if ( invocation.getBeforeFailure() == null )
            {
                invocation.setBeforeFailure( throwable );
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
        if ( invocation.getState() == InvocationStateEnum.PREINVOCATION )
        {
            try
            {
                Object retVal = method.invoke( nexus, invocation.getParameters() );
                invocation.setReturnValue( retVal );
                invocation.setState( InvocationStateEnum.POSTINVOCATION );
            }
            catch ( Throwable throwable )
            {
                invocation.setThrowable( throwable );
                invocation.setState( InvocationStateEnum.FAILUREHANDLING );
            }

            invocation.setComplete( true );
        }
        else if ( invocation.getState() == InvocationStateEnum.FAILUREHANDLING )
        {
            afterFailure.invoke( invocation );
            BaseInterceptor.setInvocation( null );
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
        if ( invocation.getState() == InvocationStateEnum.POSTINVOCATION )
        {
            try
            {
                after.invoke( invocation );
                BaseInterceptor.setInvocation( null );
                return invocation.getReturnValue();
            }
            catch ( Throwable throwable )
            {
                invocation.setState( InvocationStateEnum.FAILUREHANDLING );
                
                if ( invocation.getAfterFailure() == null )
                {
                    invocation.setAfterFailure( throwable );
                }

                afterFailure.invoke( invocation );
                BaseInterceptor.setInvocation( null );
                throw invocation.getAfterFailure();
            }
        }
        else if ( invocation.getState() == InvocationStateEnum.FAILUREHANDLING )
        {
            afterFailure.invoke( invocation );

            if ( invocation.getThrowable() == null )
            {
                throw new EveNamingException( "Interceptor Framework Failure: "
                        + "failures on the proxied call should have a non null "
                        + "throwable associated with the Invocation object.",
                        ResultCodeEnum.OTHER );
            }

            BaseInterceptor.setInvocation( null );
            throw invocation.getThrowable();
        }

        // used for an optimization
        BaseInterceptor.setInvocation( null );
        throw new EveNamingException( "Interceptor Framework Failure: "
                + "invocation handling should never have reached this line",
                ResultCodeEnum.OTHER );
    }


    /**
     * Allows the addition of an interceptor to pipelines based on invocation
     * processing states.
     *
     * @param interceptor the interceptor to add to pipelines
     * @param states the states (pipelines) where the interceptor should be applied
     */
    public void addInterceptor( Interceptor interceptor, InvocationStateEnum states[] )
    {
        for ( int ii = 0; ii < states.length; ii++ )
        {
            switch( states[ii].getValue() )
            {
                case( InvocationStateEnum.PREINVOCATION_VAL ):
                    before.add( interceptor );
                    break;
                case( InvocationStateEnum.POSTINVOCATION_VAL ):
                    after.add( interceptor );
                    break;
                case( InvocationStateEnum.FAILUREHANDLING_VAL ):
                    afterFailure.add( interceptor );
                    break;
                default:
                    throw new IllegalStateException( "unexpected invocation state: "
                            + states[ii].getName() );
            }
        }
    }
}
