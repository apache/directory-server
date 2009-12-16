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


import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import javax.naming.NamingException;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.integ.InheritableSettings;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The context for managing the state of an integration test service.
 * Each thread of execution driving tests manages it's own service context.
 * Hence parallelism can be achieved while running integration tests.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class TestServiceContext
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( TestServiceContext.class );
    
    /** The ThreadLocal containing the contexts */
    private static final ThreadLocal<TestServiceContext> CONTEXTS = new ThreadLocal<TestServiceContext>();

    /** The NonExistant state instance */
    private final TestServiceState nonExistentState = new NonExistentState( this );

    /** The StartedPristine state instance */
    private final TestServiceState startedPristineState = new StartedPristineState( this );
    
    /** The StartedNormal state instance */
    private final TestServiceState startedNormalState = new StartedNormalState( this );


    /** current service state with respect to the testing life cycle */
    private TestServiceState state = nonExistentState;

    /** the core directory service managed by this context */
    private DirectoryService service;


    /**
     * A private constructor, the clas contains only static methods, 
     * no need to construct an instance.
     */
    private TestServiceContext()
    {
        
    }


    /**
     * Gets the TestServiceContext associated with the current thread of
     * execution.  If one does not yet exist it will be created.
     *
     * @return the context associated with the calling thread
     */
    public static TestServiceContext get()
    {
        TestServiceContext context = CONTEXTS.get();

        if ( context == null )
        {
            context = new TestServiceContext();
            CONTEXTS.set( context );
        }

        return context;
    }
    

    /**
     * Sets the TestServiceContext for this current thread
     *
     * @param context the context associated with the calling thread
     */
    public static void set( TestServiceContext context )
    {
        CONTEXTS.set( context );
    }


    /**
     * Action where an attempt is made to create the service.  Service
     * creation in this system is the combined instantiation and
     * configuration which takes place when the factory is used to get
     * a new instance of the service.
     *
     * @param settings the settings for this test
     * @throws NamingException if we can't create the service
     */
    public static void create( InheritableSettings settings ) throws NamingException
    {
        get().state.create( settings );
    }


    /**
     * Action where an attempt is made to destroy the service.  This
     * entails nulling out reference to it and triggering garbage
     * collection.
     */
    public static void destroy()
    {
        get().state.destroy();
    }


    /**
     * Action where an attempt is made to erase the contents of the
     * working directory used by the service for various files including
     * partition database files.
     *
     * @throws IOException on errors while deleting the working directory
     */
    public static void cleanup() throws IOException
    {
        get().state.cleanup();
    }


    /**
     * Action where an attempt is made to start up the service.
     *
     * @throws Exception on failures to start the core directory service
     */
    public static void startup() throws Exception
    {
        get().state.startup();
    }


    /**
     * Action where an attempt is made to shutdown the service.
     *
     * @throws Exception on failures to stop the core directory service
     */
    public static void shutdown() throws Exception
    {
        get().state.shutdown();
    }


    /**
     * Action where an attempt is made to run a test against the service.
     *
     * @param testClass the class whose test method is to be run
     * @param statement the test method which is to be run
     * @param notifier a notifier to report failures to
     * @param settings the inherited settings and annotations associated with
     * the test method
     */
    public static void test( TestClass testClass, Statement statement, RunNotifier notifier,
                             InheritableSettings settings )
    {
        LOG.debug( "calling test(): {}", settings.getDescription().getDisplayName() );
        get().getState().test( testClass, statement, notifier, settings );
    }


    /**
     * Action where an attempt is made to revert the service to it's
     * initial start up state by using a previous snapshot.
     *
     * @throws Exception on failures to revert the state of the core
     * directory service
     */
    public static void revert() throws Exception
    {
        get().state.revert();
    }


    static void invokeTest( TestClass testClass, Statement statement, RunNotifier notifier, Description description )
    {
        try
        {
            Field field = testClass.getJavaClass().getDeclaredField( "service" );
            field.set( testClass.getJavaClass(), get().getService() );
            
            notifier.fireTestStarted( description );
            statement.evaluate();
            notifier.fireTestFinished( description );
        }
        catch ( InvocationTargetException e )
        {
            LOG.error( "Failed to invoke test method: " + description.getDisplayName(), e.getCause() );
            testAborted( notifier, description, e.getCause() );
            return;
        }
        catch ( InstantiationException ie )
        {
            LOG.error( "Failed to invoke test method: " + description.getDisplayName(), ie );
            testAborted( notifier, description, ie );
            return;
        }
        catch ( IllegalAccessException iae )
        {
            LOG.error( "Failed to invoke test method: " + description.getDisplayName(), iae );
            testAborted( notifier, description, iae );
            return;
        }
        catch ( NoSuchMethodException nsme )
        {
            LOG.error( "Failed to invoke test method: " + description.getDisplayName(), nsme );
            testAborted( notifier, description, nsme );
            return;
        }
        catch ( NoSuchFieldException nsfe )
        {
            LOG.error( "Failed to invoke test method: " + description.getDisplayName(), nsfe );
            testAborted( notifier, description, nsfe );
            return;
        }
        catch ( Throwable t )
        {
            LOG.error( "Failed to invoke test method: " + description.getDisplayName(), t );
            testAborted( notifier, description, t );
            return;
        }
    }


    // -----------------------------------------------------------------------
    // Package Friendly Instance Methods
    // -----------------------------------------------------------------------


    void setState( TestServiceState state )
    {
        this.state = state;
    }


    TestServiceState getState()
    {
        return state;
    }


    TestServiceState getNonExistentState()
    {
        return nonExistentState;
    }


    TestServiceState getStartedPristineState()
    {
        return startedPristineState;
    }


    TestServiceState getStartedNormalState()
    {
        return startedNormalState;
    }


    DirectoryService getService()
    {
        return service;
    }


    void setService( DirectoryService service )
    {
        this.service = service;
    }


    private static void testAborted( RunNotifier notifier, Description description, Throwable cause )
    {
        notifier.fireTestStarted( description );
        notifier.fireTestFailure( new Failure( description, cause ) );
        notifier.fireTestFinished( description );
    }
}
