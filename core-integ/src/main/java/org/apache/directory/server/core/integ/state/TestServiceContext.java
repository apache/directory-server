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


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.integ.DirectoryServiceFactory;
import org.apache.directory.server.core.integ.ServiceScope;
import org.apache.directory.server.core.integ.SetupMode;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;


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
    private static final ThreadLocal<TestServiceContext> CONTEXTS = new ThreadLocal<TestServiceContext>();

    private final TestServiceState nonExistentState = new NonExistentState( this );
    private final TestServiceState startedDirtyState = new StartedDirtyState( this );
    private final TestServiceState startedPristineState = new StartedPristineState( this );
    private final TestServiceState startedRevertedState = new StartedRevertedState( this );
    private final TestServiceState stoppedDirtyState = new StoppedDirtyState( this );
    private final TestServiceState stoppedPristineState = new StoppedPristineState( this );


    /** current service state with respect to the testing life cycle */
    private TestServiceState state;
    private ServiceScope scope;
    private SetupMode mode;
    private DirectoryService service;
    private DirectoryServiceFactory factory;
    private RunNotifier notifier;
    private Description description;


    /**
     * Gets the TestServiceContext associated with the current thread of execution.
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
     */
    public void create()
    {
        state.create();
    }


    /**
     * Action where an attempt is made to destroy the service.  This
     * entails nulling out reference to it and triggering garbage
     * collection.
     */
    public void destroy()
    {
        state.destroy();
    }


    /**
     * Action where an attempt is made to erase the contents of the
     * working directory used by the service for various files including
     * partition database files.
     */
    public void cleanup()
    {
        state.cleanup();
    }


    /**
     * Action where an attempt is made to start up the service.
     */
    public void startup()
    {
        state.startup();
    }


    /**
     * Action where an attempt is made to shutdown the service.
     */
    public void shutdown()
    {
        state.shutdown();
    }


    /**
     * Action where an attempt is made to run a test against the service.
     */
    public void test()
    {
        state.test();
    }


    /**
     * Action where an attempt is made to revert the service to it's
     * initial start up state by using a previous snapshot.
     */
    public void revert()
    {
        state.revert();
    }


    void setState( TestServiceState state )
    {
        this.state = state;
    }


    public TestServiceState getState()
    {
        return state;
    }


    public TestServiceState getNonExistentState()
    {
        return nonExistentState;
    }


    public TestServiceState getStartedDirtyState()
    {
        return startedDirtyState;
    }


    public TestServiceState getStartedPristineState()
    {
        return startedPristineState;
    }


    public TestServiceState getStartedRevertedState()
    {
        return startedRevertedState;
    }


    public TestServiceState getStoppedDirtyState()
    {
        return stoppedDirtyState;
    }


    public TestServiceState getStoppedPristineState()
    {
        return stoppedPristineState;
    }


    public ServiceScope getScope()
    {
        return scope;
    }


    public void setScope( ServiceScope scope )
    {
        this.scope = scope;
    }


    public DirectoryService getService()
    {
        return service;
    }


    public void setService( DirectoryService service )
    {
        this.service = service;
    }


    public SetupMode getMode()
    {
        return mode;
    }


    public void setMode( SetupMode mode )
    {
        this.mode = mode;
    }


    public DirectoryServiceFactory getFactory()
    {
        return factory;
    }


    public void setFactory( DirectoryServiceFactory factory )
    {
        this.factory = factory;
    }


    public RunNotifier getNotifier()
    {
        return notifier;
    }


    public void setNotifier( RunNotifier notifier )
    {
        this.notifier = notifier;
    }


    public Description getDescription()
    {
        return description;
    }


    public void setDescription( Description description )
    {
        this.description = description;
    }
}
