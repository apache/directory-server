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
package org.apache.directory.server.integ.state;


import static org.apache.directory.server.core.integ.IntegrationUtils.doDelete;

import java.io.IOException;

import org.apache.directory.server.integ.InheritableServerSettings;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



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
public class StartedNormalState extends AbstractState
{
    private static final Logger LOG = LoggerFactory.getLogger( StartedNormalState.class );


    /**
     * 
     * Creates a new instance of StartedNormalState.
     *
     * @param context the test's context
     */
    public StartedNormalState( TestServerContext context )
    {
        super( context );
    }


    /**
     * Action where an attempt is made to destroy the service. This
     * entails nulling out reference to it and triggering garbage
     * collection.
     */
    public void destroy()
    {
        LOG.debug( "calling destroy()" );
        context.getLdapServer().setDirectoryService( null );
        context.setLdapServer( null );
        context.setState( context.getNonExistentState() );
        System.gc();
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
        doDelete( context.getLdapServer().getDirectoryService().getWorkingDirectory() );
    }


    /**
     * Action where an attempt is made to start up the service.
     *
     * @throws Exception on failures to start the core directory service
     */
    public void startup() throws Exception
    {
        LOG.debug( "calling start()" );
        context.getLdapServer().getDirectoryService().startup();
        context.getLdapServer().start();
    }


    /**
     * Action where an attempt is made to shutdown the service.
     *
     * @throws Exception on failures to stop the core directory service
     */
    public void shutdown() throws Exception
    {
        LOG.debug( "calling shutdown()" );
        context.getLdapServer().stop();
        context.getLdapServer().getDirectoryService().shutdown();
    }


    /**
     * Action where an attempt is made to revert the service to it's
     * initial start up state by using a previous snapshot.
     *
     * @throws Exception on failures to revert the state of the core
     * directory service
     */
    public void revert() throws Exception
    {
        LOG.debug( "calling revert()" );
        context.getLdapServer().getDirectoryService().revert();
    }


    /**
     * Action where an attempt is made to run a test against the service.
     *
     * All annotations should have already been processed for
     * InheritableServerSettings yet they and others can be processed since we have
     * access to the method annotations below
     *
     * @param testClass the class whose test method is to be run
     * @param statement the test method which is to be run
     * @param notifier a notifier to report failures to
     * @param settings the inherited settings and annotations associated with
     * the test method
     */
    public void test( TestClass testClass, Statement statement, RunNotifier notifier, InheritableServerSettings settings )
    {
        LOG.debug( "calling test(): {}, mode {}", settings.getDescription().getDisplayName(), settings.getMode() );

        switch ( settings.getMode() )
        {
            case ROLLBACK:
                try
                {
                    context.getLdapServer().getDirectoryService().getChangeLog().tag();

                    // Inject the LDIFs, if any 
                    injectLdifs( context.getLdapServer().getDirectoryService(), settings );
                }
                catch ( Exception e )
                {
                    // @TODO - we might want to check the revision of the service before
                    // we presume that it has been soiled.  Some tests may simply perform
                    // some read operations or checks on the service and may not alter it
                    testAborted( notifier, settings.getDescription(), e );
                    return;
                }

                TestServerContext.invokeTest( testClass, statement, notifier, settings.getDescription() );
                
                try
                {
                    revert();
                }
                catch ( Exception e )
                {
                    // @TODO - we might want to check the revision of the service before
                    // we presume that it has been soiled.  Some tests may simply perform
                    // some read operations or checks on the service and may not alter it
                    testAborted( notifier, settings.getDescription(), e );
                    return;
                }
                
                return;
                
            case RESTART :
                // Inject the LDIFs, if any 
                try
                {
                    injectLdifs( context.getLdapServer().getDirectoryService(), settings );
                }
                catch ( Exception e )
                {
                    // @TODO - we might want to check the revision of the service before
                    // we presume that it has been soiled.  Some tests may simply perform
                    // some read operations or checks on the service and may not alter it
                    testAborted( notifier, settings.getDescription(), e );
                    return;
                }

                TestServerContext.invokeTest( testClass, statement, notifier, settings.getDescription() );

                try
                {
                    shutdown();
                }
                catch ( Exception e )
                {
                    // @TODO - we might want to check the revision of the service before
                    // we presume that it has been soiled.  Some tests may simply perform
                    // some read operations or checks on the service and may not alter it
                    testAborted( notifier, settings.getDescription(), e );
                    return;
                }
                
                try
                {
                    startup();
                }
                catch ( Exception e )
                {
                    LOG.error( "Failed to create and start new server instance: " + e );
                    testAborted( notifier, settings.getDescription(), e );
                    return;
                }
                
                return;
                
            default:
                return;
        }
    }
}
