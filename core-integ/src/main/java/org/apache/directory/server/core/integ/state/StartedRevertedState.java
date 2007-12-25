/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.integ.state;


import org.apache.directory.server.core.integ.DirectoryServiceFactory;
import org.apache.directory.server.core.integ.InheritableSettings;
import org.apache.directory.server.core.integ.SetupMode;
import org.junit.internal.runners.TestClass;
import org.junit.internal.runners.TestMethod;
import org.junit.runner.notification.RunNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;


/**
 * The state of a running test service which has been used for running
 * integration tests and has been reverted to contain the same content as it
 * did when created and started.  It is not really pristine however for all
 * practical purposes of integration testing it appears to be the same as
 * when first started.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class StartedRevertedState implements TestServiceState
{
    private static final Logger LOG = LoggerFactory.getLogger( StartedRevertedState.class );
    private final TestServiceContext context;


    public StartedRevertedState( TestServiceContext context )
    {
        this.context = context;
    }


    public void create( DirectoryServiceFactory factory )
    {
        throw new IllegalStateException( "Cannot create new instance while service is running." );
    }


    public void destroy()
    {
        throw new IllegalStateException( "Cannot destroy started service." );
    }


    public void cleanup()
    {
        throw new IllegalStateException( "Cannot cleanup started service." );
    }


    public void startup()
    {
        throw new IllegalStateException( "Cannot startup started service." );
    }


    public void shutdown() throws NamingException
    {
        LOG.debug( "calling shutdown()" );
        context.getService().shutdown();
        context.setState( context.getStoppedDirtyState() );
    }


    public void test( TestClass testClass, TestMethod testMethod, RunNotifier notifier, InheritableSettings settings )
    {
        LOG.debug( "calling test(): {}", settings.getDescription().getDisplayName() );
        if ( settings.getMode() == SetupMode.NOSERVICE || testMethod.isIgnored() )
        {
            // no state change here
            TestServiceContext.invokeTest( testClass, testMethod, notifier, settings.getDescription() );
            return;
        }

        if ( settings.getMode() == SetupMode.PRISTINE )
        {
            try
            {
                context.getState().shutdown();
                context.getState().cleanup();

                // @todo check if the factory changed here
                if ( true ) // change this to check if factory changed since the last run
                {
                     context.getState().destroy();
                     context.getState().create( settings.getFactory() );
                }

                context.getState().startup();
            }
            catch ( Exception e )
            {
                notifier.testAborted( settings.getDescription(), e );
                return;
            }

            // state object what ever it is will handle tagging before test method
            // invocation and it will also change the state so we can just return
            context.getState().test( testClass, testMethod, notifier, settings );
            return;
        }

        try
        {
            context.getService().getChangeLog().tag();
        }
        catch ( NamingException e )
        {
            // @TODO - we might want to check the revision of the service before
            // we presume that it has been soiled.  Some tests may simply peform
            // some read operations or checks on the service and may not alter it
            context.setState( context.getStartedDirtyState() );

            notifier.testAborted( settings.getDescription(), e );
            return;
        }

        TestServiceContext.invokeTest( testClass, testMethod, notifier, settings.getDescription() );

        // @TODO - we might want to check the revision of the service before
        // we presume that it has been soiled.  Some tests may simply peform
        // some read operations or checks on the service and may not alter it
        context.setState( context.getStartedDirtyState() );
    }


    public void revert() throws NamingException
    {
        throw new IllegalStateException( "Cannot revert already reverted service." );
    }
}