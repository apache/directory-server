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
 * The state of a test service where it has been started and has been used for
 * integration tests which has changed it's contents.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class StartedDirtyState implements TestServiceState
{
    private static final Logger LOG = LoggerFactory.getLogger( StartedDirtyState.class );
    private final TestServiceContext context;
    private static final String CREATE_ERROR = "Cannot create new instance while service is running.";
    private static final String DESTROY_ERROR = "Cannot destroy started service.";
    private static final String CLEANUP_ERR = "Cannot cleanup started service.";
    private static final String STARTUP_ERR = "Cannot startup started service.";


    public StartedDirtyState( TestServiceContext context )
    {
        this.context = context;
    }


    public void create( DirectoryServiceFactory factory )
    {
        LOG.error( CREATE_ERROR );
        throw new IllegalStateException( CREATE_ERROR );
    }


    public void destroy()
    {
        LOG.error( DESTROY_ERROR );
        throw new IllegalStateException( DESTROY_ERROR );
    }


    public void cleanup()
    {
        LOG.error( CLEANUP_ERR );
        throw new IllegalStateException( CLEANUP_ERR );
    }


    public void startup()
    {
        LOG.error( STARTUP_ERR );
        throw new IllegalStateException( STARTUP_ERR );
    }


    public void shutdown() throws NamingException
    {
        LOG.debug( "calling shutdown() " );
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
                context.getState().destroy();
            }
            catch ( Exception e )
            {
                LOG.error( "Failed to reach pristine restart for test: "
                        + settings.getDescription().getDisplayName(), e );
                notifier.testAborted( settings.getDescription(), e );
                return;
            }

            // state object what ever it is will handle tagging before test method
            // invocation and it will also change the state so we can just return
            context.getState().test( testClass, testMethod, notifier, settings );
            return;
        }

        if ( settings.getMode() == SetupMode.RESTART )
        {
            try
            {
                context.getState().shutdown();
                context.getState().startup();
            }
            catch ( Exception e )
            {
                LOG.error( "Failed to restart for test: " + settings.getDescription().getDisplayName(), e );
                notifier.testAborted( settings.getDescription(), e );
                return;
            }

            // right now the state has cycled back to started dirty so we cannot
            // call test() on the state or we'll loop forever shutting down and
            // starting the service so we simply invoke the method directly and
            // and don't mess with the state because it will still be dirty.

            // @TODO a restart will whipe the in memory changelog so we will not
            // know what revision we are at.  The changelog needs to persist the
            // current revision across restarts even if it uses an inmemory store.
            // So this means if there is are the following order of test runs
            // with respective modes:
            //
            // test0 -> ! ( NOSERVICE )         # tagged at t0 then changed
            // test1 -> RESTART                 # shutdown/startup no cleanup
            // test2 -> ROLLBACK                # no t0 to rollback to
            //
            // To prevent this tags should be persisted across restarts really.
            // This is easy to do and can be implemented quickly.  We can
            // perstist both the current revision and tags.
            //
            // This basically makes the premis that a cleanup is required after
            // a shutdown not needed in how we handle rollbacks.

            TestServiceContext.invokeTest( testClass, testMethod, notifier, settings.getDescription() );
            return;
        }

        if ( settings.getMode() == SetupMode.ROLLBACK )
        {
            try
            {
//                // @todo check if the factory changed here
//                if ( true ) // change this to check if factory changed since the last run
//                {
//                     context.getState().shutdown();
//                     context.getState().cleanup();
//                     context.getState().destroy();
//                     context.getState().create( settings.getFactory() );
//                     context.getState().test( testClass, testMethod, notifier, settings );
//                     return;
//                }
//                else
//                {
                    context.getState().revert();
//                }
            }
            catch ( Exception e )
            {
                LOG.error( "Failed to revert for test: " + settings.getDescription().getDisplayName(), e );
                notifier.testAborted( settings.getDescription(), e );
                return;
            }

            // state object what ever it is will handle tagging before test method
            // invocation and it will also change the state so we can just return
            context.getState().test( testClass, testMethod, notifier, settings );
            return;
        }

        // at this point only CUMULATIVE tests get to here and they don't care
        // what we do so we can just invoke the test and the state stays same
        TestServiceContext.invokeTest( testClass, testMethod, notifier, settings.getDescription() );
    }


    public void revert() throws NamingException
    {
        LOG.debug( "calling revert()" );
        context.getService().revert();
        context.setState( context.getStartedRevertedState() );
    }
}