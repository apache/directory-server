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

import org.apache.directory.server.core.integ.InheritableSettings;
import static org.apache.directory.server.core.integ.IntegrationUtils.doDelete;
import org.junit.internal.runners.TestClass;
import org.junit.internal.runners.TestMethod;
import org.junit.runner.notification.RunNotifier;
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
    public StartedNormalState( TestServiceContext context )
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
        context.setService( null );
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
        doDelete( context.getService().getWorkingDirectory() );
    }


    /**
     * Action where an attempt is made to start up the service.
     *
     * @throws NamingException on failures to start the core directory service
     */
    public void startup() throws NamingException
    {
        LOG.debug( "calling startup()" );
        context.getService().startup();
    }


    /**
     * Action where an attempt is made to shutdown the service.
     *
     * @throws NamingException on failures to stop the core directory service
     */
    public void shutdown() throws NamingException
    {
        LOG.debug( "calling shutdown()" );
        context.getService().shutdown();
    }


    /**
     * Action where an attempt is made to revert the service to it's
     * initial start up state by using a previous snapshot.
     *
     * @throws NamingException on failures to revert the state of the core
     * directory service
     */
    public void revert() throws NamingException
    {
        LOG.debug( "calling revert()" );
        context.getService().revert();
    }


    /**
     * Action where an attempt is made to run a test against the service.
     *
     * All annotations should have already been processed for
     * InheritableSettings yet they and others can be processed since we have
     * access to the method annotations below
     *
     * @param testClass the class whose test method is to be run
     * @param testMethod the test method which is to be run
     * @param notifier a notifier to report failures to
     * @param settings the inherited settings and annotations associated with
     * the test method
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
            case ROLLBACK:
                try
                {
                    context.getService().getChangeLog().tag();
                }
                catch ( NamingException e )
                {
                    // @TODO - we might want to check the revision of the service before
                    // we presume that it has been soiled.  Some tests may simply perform
                    // some read operations or checks on the service and may not alter it
                    notifier.testAborted( settings.getDescription(), e );
                    return;
                }

                TestServiceContext.invokeTest( testClass, testMethod, notifier, settings.getDescription() );
                
                try
                {
                    revert();
                }
                catch ( NamingException ne )
                {
                    // @TODO - we might want to check the revision of the service before
                    // we presume that it has been soiled.  Some tests may simply perform
                    // some read operations or checks on the service and may not alter it
                    notifier.testAborted( settings.getDescription(), ne );
                    return;
                }
                
                return;
                
            case RESTART :
                TestServiceContext.invokeTest( testClass, testMethod, notifier, settings.getDescription() );

                try
                {
                    shutdown();
                }
                catch ( NamingException ne )
                {
                    // @TODO - we might want to check the revision of the service before
                    // we presume that it has been soiled.  Some tests may simply perform
                    // some read operations or checks on the service and may not alter it
                    notifier.testAborted( settings.getDescription(), ne );
                    return;
                }
                
                try
                {
                    startup();
                }
                catch ( NamingException ne )
                {
                    LOG.error( "Failed to create and start new server instance: " + ne );
                    notifier.testAborted( settings.getDescription(), ne );
                    return;
                }

                return;
                
            default:
                return;
        }
    }
}