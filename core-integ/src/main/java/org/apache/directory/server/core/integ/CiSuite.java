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
package org.apache.directory.server.core.integ;


import org.apache.directory.server.core.DirectoryService;
import static org.apache.directory.server.core.integ.AnnotationUtils.getMode;
import static org.apache.directory.server.core.integ.AnnotationUtils.newFactory;
import static org.apache.directory.server.core.integ.IntegrationUtils.doDelete;
import org.apache.directory.server.core.integ.annotations.Factory;
import org.apache.directory.server.core.integ.annotations.ForceCleanup;
import org.apache.directory.server.core.integ.annotations.Mode;
import org.junit.internal.runners.InitializationError;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Suite;


/**
 * A replacement for standard JUnit 4 suites. Note that this test suite
 * will not startup an DirectoryService instance but will clean it up if
 * one remains.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class CiSuite extends Suite
{
    public static final SetupMode DEFAULT_MODE = SetupMode.NOSERVICE;

    /**
     * The suite level setup mode.  This is the mode that is inherited
     * by test classes included in this suite.  It determines the life
     * cycle of a directory service as tests are run against it.
     */
    private SetupMode mode = DEFAULT_MODE;

    /**
     * The suite level service factory. This is the factory that is
     * inherited by test classes included in this suite.  It is used
     * to instantiate services for suite's, test clases, and test
     * cases depending on the setup mode utilized.
     */
    private DirectoryServiceFactory factory = DirectoryServiceFactory.DEFAULT;

    /**
     * The suite level reference to the directory service.  This
     * service object may be used as a shared service across all tests.
     * Sometimes each test may create it's own service and be run in
     * parallel.  Likewise for tests.  It all depends on the threading
     * model and on the lifecycle of the tested service.
     */
    private DirectoryService service;


    public CiSuite( Class<?> clazz ) throws InitializationError
    {
        super( clazz );
        factory = newFactory( clazz.getAnnotation( Factory.class ), factory );
        mode = getMode( clazz.getAnnotation( Mode.class ), mode );
    }


    protected CiSuite( Class<?> clazz, Class<?>[] classes ) throws InitializationError
    {
        super( clazz, classes );
    }


    @Override
    public void run( final RunNotifier notifier )
    {
        for ( Runner runner : getRunners() )
        {
            if ( runner instanceof CiRunner )
            {
                CiRunner cir = ( CiRunner) runner;
                cir.setSuite( this );
            }
        }

        super.run( notifier );

        if ( service != null && service.isStarted() )
        {
            try
            {
                service.shutdown();
            }
            catch ( Exception e )
            {
                notifier.fireTestFailure( new Failure( getDescription(), e ) );
            }
        }

        if ( service != null )
        {
            try
            {
                doDelete( service.getWorkingDirectory() );
            }
            catch ( Exception e )
            {
                notifier.fireTestFailure( new Failure( getDescription(), e ) );
            }
        }
    }


    public SetupMode getSetupMode()
    {
        return mode;
    }


    public void setSetupMode( SetupMode mode )
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


    public DirectoryService getService()
    {
        return service;
    }


    public void setService( DirectoryService service )
    {
        this.service = service;
    }
}
