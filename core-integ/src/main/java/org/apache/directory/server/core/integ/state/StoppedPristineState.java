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
import static org.apache.directory.server.core.integ.IntegrationUtils.doDelete;
import org.apache.directory.shared.ldap.NotImplementedException;
import org.junit.internal.runners.TestClass;
import org.junit.internal.runners.TestMethod;
import org.junit.runner.notification.RunNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import java.io.IOException;


/**
 * A state representing a test service that has either just been created,
 * or a running pristine instance has been stopped.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class StoppedPristineState implements TestServiceState
{
    private static final Logger LOG = LoggerFactory.getLogger( StoppedPristineState.class );
    private final TestServiceContext context;


    public StoppedPristineState( TestServiceContext context )
    {
        this.context = context;
    }


    public void create( DirectoryServiceFactory factory )
    {
        throw new IllegalStateException( "Cannot create a stopped instance." );
    }


    public void destroy()
    {
        context.setService( null );
        context.setState( context.getNonExistentState() );
        System.gc();
    }


    /**
     * Even though this method should ignore the call it will attempt to delete any
     * working directory if at all present.
     *
     * @see TestServiceState#cleanup()
     */
    public void cleanup() throws IOException
    {
        doDelete( context.getService().getWorkingDirectory() );
    }


    public void startup() throws NamingException
    {
        context.getService().startup();
        context.setState( context.getStartedPristineState() );
    }


    public void shutdown() throws NamingException
    {
        throw new IllegalStateException( "Cannot shutdown stopped pristine service." );
    }


    public void test( TestClass testClass, TestMethod testMethod, RunNotifier notifier, InheritableSettings settings )
    {
        if ( settings.getMode() == SetupMode.NOSERVICE || testMethod.isIgnored() )
        {
            // no state change here
            TestServiceContext.invokeTest( testClass, testMethod, notifier, settings.getDescription() );
            return;
        }

        try
        {
            context.getState().startup();
        }
        catch ( Exception e )
        {
            notifier.testAborted( settings.getDescription(), e );
            return;
        }

        // state object what ever it is will change state so we just return
        context.getState().test( testClass, testMethod, notifier, settings );
    }


    public void revert() throws NamingException
    {
        throw new IllegalStateException( "Cannot revert stopped service." );        
    }
}