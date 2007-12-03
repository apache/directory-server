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
import org.junit.internal.runners.MethodRoadie;
import org.junit.runner.notification.RunNotifier;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;


/**
 * The state of a test service when it has not yet been created.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class NonExistentState implements TestServiceState
{
    private static final Logger LOG = LoggerFactory.getLogger( NonExistentState.class );
    private final TestServiceContext context;


    public NonExistentState( TestServiceContext context )
    {
        this.context = context;
    }


    public void create( DirectoryServiceFactory factory )
    {
        context.setService( factory.newInstance() );
        context.setState( context.getStoppedDirtyState() );
    }


    public void destroy()
    {
        throw new IllegalStateException( "Cannot destroy when service is in NonExistant state" );
    }


    public void cleanup()
    {
        throw new IllegalStateException( "Cannot cleanup when service is in NonExistant state" );
    }


    public void startup()
    {
        throw new IllegalStateException( "Cannot startup when service is in NonExistant state" );
    }


    public void shutdown()
    {
        throw new IllegalStateException( "Cannot shutdown service in NonExistant state." );
    }


    /**
     * This method is a bit different.  Consider this method to hold the logic
     * which is needed to shift the context state from the present state to a
     * started state so we can call test on the current state of the context.
     *
     * Basically if the service is not needed or the test is ignored, then we
     * just invoke the test: if ignored the test is not dealt with by the
     * MethodRoadie run method.
     *
     * In tests not ignored requiring setup modes RESTART and CUMULATIVE we
     * simply create the service and start it up without a cleanup.  In the
     * PRISTINE and ROLLBACK modes we do the same but cleanup() before a
     * restart.
     *
     *
     * @param testClass
     * @param testMethod
     * @param notifier
     * @param settings
     */
    public void test( TestClass testClass, TestMethod testMethod, RunNotifier notifier, InheritableSettings settings )
    {
        if ( settings.getMode() == SetupMode.NOSERVICE || testMethod.isIgnored() )
        {
            // no state change here
            TestServiceContext.invokeTest( testClass, testMethod, notifier, settings.getDescription() );
            return;
        }

        if ( settings.getMode() == SetupMode.RESTART || settings.getMode() == SetupMode.CUMULATIVE )
        {
            try
            {
                context.getState().create( settings.getFactory() );
                context.getState().startup();
            }
            catch ( Exception e )
            {
                notifier.testAborted( settings.getDescription(), e );
                return;
            }
        }

        if ( settings.getMode() == SetupMode.PRISTINE || settings.getMode() == SetupMode.ROLLBACK )
        {
            try
            {
                context.getState().create( settings.getFactory() );
                context.getState().cleanup();
                context.getState().startup();
            }
            catch ( Exception e )
            {
                notifier.testAborted( settings.getDescription(), e );
                return;
            }
        }

        // state object what ever it is will change state so we just return
        context.getState().test( testClass, testMethod, notifier, settings );
    }


    public void revert()
    {
        throw new IllegalStateException( "Cannot revert when service is in NonExistant state" );
    }
}
