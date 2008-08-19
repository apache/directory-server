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

import javax.naming.NamingException;

import org.apache.directory.server.core.integ.DirectoryServiceFactory;
import org.apache.directory.server.core.integ.InheritableSettings;
import static org.apache.directory.server.core.integ.IntegrationUtils.doDelete;
import org.junit.internal.runners.TestClass;
import org.junit.internal.runners.TestMethod;
import org.junit.runner.notification.RunNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The state of a test service when it has not yet been created.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class NonExistentState extends AbstractState
{
    private static final Logger LOG = LoggerFactory.getLogger( NonExistentState.class );


    /**
     * Creates a new instance of NonExistentState.
     *
     * @param context the test context
     */
    public NonExistentState( TestServiceContext context )
    {
        super( context );
    }


    /**
     * Action where an attempt is made to create the service.  Service
     * creation in this system is the combined instantiation and
     * configuration which takes place when the factory is used to get
     * a new instance of the service.
     *
     * @param settings The inherited settings
     * @throws NamingException if we can't create the service
     */
    public void create( InheritableSettings settings ) throws NamingException
    {
        LOG.debug( "calling create()" );

        try
        {
            DirectoryServiceFactory factory = settings.getFactory();
            context.setService( factory.newInstance() );
        }
        catch ( InstantiationException ie )
        {
            throw new NamingException( ie.getMessage() );
        }
        catch ( IllegalAccessException iae )
        {
            throw new NamingException( iae.getMessage() );
        }
        catch ( Exception e )
        {
            throw new NamingException( e.getMessage() );
        }
    }


    /**
     * Action where an attempt is made to erase the contents of the
     * working directory used by the service for various files including
     * partition database files.
     *
     * @throws IOException on errors while deleting the working directory
     */
    public void cleanup() throws IOException
    {
        LOG.debug( "calling cleanup()" );
        doDelete( context.getService().getWorkingDirectory() );
    }


    /**
     * Action where an attempt is made to start up the service.
     *
     * @throws Exception on failures to start the core directory service
     */
    public void startup() throws Exception
    {
        LOG.debug( "calling startup()" );
        context.getService().startup();
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
     * @see TestServiceState#test(TestClass, TestMethod, RunNotifier, InheritableSettings) 
     */
    public void test( TestClass testClass, TestMethod testMethod, RunNotifier notifier, InheritableSettings settings )
    {
        LOG.debug( "calling test(): {}, mode {}", settings.getDescription().getDisplayName(), settings.getMode() );

        if ( testMethod.isIgnored() )
        {
            // The test is ignored
            return;
        }

        switch ( settings.getMode() )
        {
            case CUMULATIVE:
            case RESTART:
                try
                {
                    create( settings );
                }
                catch ( NamingException ne )
                {
                    LOG.error( "Failed to create and start new server instance: " + ne );
                    notifier.testAborted( settings.getDescription(), ne );
                    return;
                }

                try
                {
                    startup();
                }
                catch ( Exception e )
                {
                    LOG.error( "Failed to create and start new server instance: " + e );
                    notifier.testAborted( settings.getDescription(), e );
                    return;
                }

                
                context.setState( context.getStartedNormalState() );
                context.getState().test( testClass, testMethod, notifier, settings );
                return;


            case PRISTINE:
            case ROLLBACK:
                try
                {
                    create( settings );
                }
                catch ( NamingException ne )
                {
                    LOG.error( "Failed to create and start new server instance: " + ne );
                    notifier.testAborted( settings.getDescription(), ne );
                    return;
                }

                try
                {
                    cleanup();
                }
                catch ( IOException ioe )
                {
                    LOG.error( "Failed to create and start new server instance: " + ioe );
                    notifier.testAborted( settings.getDescription(), ioe );
                    return;
                }

                try
                {
                    startup();
                }
                catch ( Exception e )
                {
                    LOG.error( "Failed to create and start new server instance: " + e );
                    notifier.testAborted( settings.getDescription(), e );
                    return;
                }

                context.setState( context.getStartedPristineState() );
                context.getState().test( testClass, testMethod, notifier, settings );
                return;

            default:
                return;
        }
    }
}
