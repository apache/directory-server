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
import org.junit.internal.runners.TestClass;
import org.junit.internal.runners.TestMethod;
import org.junit.runner.notification.RunNotifier;


/**
 * Document me!
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class NonExistentState implements TestServiceState
{
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
        // do nothing!
    }


    public void cleanup()
    {
        // do nothing
    }


    public void startup()
    {
        throw new IllegalStateException( "Attempting to start up a test service that does not exist." );
    }


    public void shutdown()
    {
        throw new IllegalStateException( "Attempting to shut down a test service that does not exist." );
    }


    public void test( TestClass testClass, TestMethod testMethod, RunNotifier notifier, InheritableSettings settings )
    {
        throw new IllegalStateException( "Attempting to run integration tests on a service that does not exist." );
    }


    public void revert()
    {
        throw new IllegalStateException( "Attempting to revert a test service that does not exist." );
    }
}
