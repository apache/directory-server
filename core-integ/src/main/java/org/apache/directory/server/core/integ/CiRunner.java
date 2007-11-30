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
import org.apache.directory.server.core.changelog.Tag;
import static org.apache.directory.server.core.integ.AnnotationUtils.getMode;
import static org.apache.directory.server.core.integ.AnnotationUtils.newFactory;
import static org.apache.directory.server.core.integ.IntegrationUtils.doDelete;
import org.apache.directory.server.core.integ.annotations.Factory;
import org.apache.directory.server.core.integ.annotations.ForceCleanup;
import org.apache.directory.server.core.integ.annotations.Mode;
import org.junit.internal.runners.InitializationError;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.internal.runners.MethodRoadie;
import org.junit.internal.runners.TestMethod;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

import javax.naming.NamingException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * A test runner for ApacheDS Core integration tests.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class CiRunner extends JUnit4ClassRunner
{
    public static final SetupMode DEFAULT_MODE = SetupMode.NOSERVICE;

    private Class<?> clazz;
    private CiSuite suite;
    private SetupMode setupMode;
    private DirectoryService service;
    private DirectoryServiceFactory factory;
    private boolean cleanup;


    public CiRunner( Class<?> clazz ) throws InitializationError
    {
        super( clazz );
        this.clazz = clazz;
    }


    @Override
    public void run( final RunNotifier notifier )
    {
        if ( suite == null )
        {
            setupMode = getMode( clazz.getAnnotation( Mode.class ), DEFAULT_MODE );
            factory = newFactory( clazz.getAnnotation( Factory.class ), DirectoryServiceFactory.DEFAULT );
        }
        else
        {
            service = suite.getService();
            setupMode = getMode( clazz.getAnnotation( Mode.class ), suite.getSetupMode() );
            factory = newFactory( clazz.getAnnotation( Factory.class ), suite.getFactory() );
        }

        if ( clazz.getAnnotation( ForceCleanup.class ) != null )
        {
            cleanup = true;
        }

        // if the suite is null that means it's up to this class to cleanup
        if ( suite == null || cleanup )
        {
            cleanup( notifier );
        }

        super.run( notifier );

        // if the suite is null that means it's up to this class to cleanup
        if ( suite == null || cleanup )
        {
            cleanup( notifier );
            service = null;
        }
    }


    private void cleanup( final RunNotifier notifier )
    {
        if ( service != null && service.isStarted() )
        {
            try
            {
                service.shutdown();
            }
            catch ( NamingException e )
            {
                notifier.fireTestFailure( new Failure( getDescription(), e ) );
            }
        }
        else if ( service != null )
        {
            try
            {
                doDelete( service.getWorkingDirectory() );
            }
            catch ( IOException e )
            {
                notifier.fireTestFailure( new Failure( getDescription(), e ) );
            }
        }
    }


    public static final String INVALID_SERVICE_FIELD_MSG =
            "You must create a static DirectoryService field named service in your test class: ";


    @Override
    protected void invokeTestMethod( Method method, final RunNotifier notifier )
    {
        Description description = methodDescription( method );
        Object test;

        try
        {
            test = createTest();
        }
        catch ( InvocationTargetException e )
        {
            notifier.testAborted( description, e.getCause() );
            return;
        }
        catch ( Exception e )
        {
            notifier.testAborted( description, e );
            return;
        }

        TestMethod testMethod = wrapMethod( method );

        if ( ! testMethod.isIgnored() )
        {
            SetupMode methodMode = getMode( method.getAnnotation( Mode.class ), setupMode );
            DirectoryServiceFactory methodFactory = newFactory( method.getAnnotation( Factory.class ), factory );

            if ( method.getAnnotation( ForceCleanup.class ) != null )
            {
                if ( service != null && service.isStarted() )
                {
                    try
                    {
                        service.shutdown();
                    }
                    catch ( NamingException e )
                    {
                        notifier.testAborted( description, e );
                    }
                }

                if ( service != null )
                {
                    cleanup( notifier );
                    service = null;
                }
            }

            switch( methodMode.ordinal )
            {
                case( SetupMode.NOSERVICE_ORDINAL ):
                case( SetupMode.CUMULATIVE_ORDINAL ):
                    if ( service == null )
                    {
                        service = factory.newInstance();

                        try
                        {
                            service.startup();
                        }
                        catch ( NamingException e )
                        {
                            notifier.testAborted( description, e );
                        }
                    }

                    if ( service != null )
                    {
                        try
                        {
                            service.getChangeLog().tag();
                        }
                        catch ( NamingException e )
                        {
                            notifier.testAborted( description, e );
                        }
                    }
                    break;
                case( SetupMode.ROLLBACK_ORDINAL ):
                    // if the service is null instantiate it w/ factory
                    if ( service == null )
                    {
                        service = methodFactory.newInstance();
                        cleanup( notifier );
                        service.getChangeLog().setEnabled( true );
                        
                        try
                        {
                            service.startup();
                        }
                        catch ( NamingException e )
                        {
                            notifier.testAborted( description, e );
                        }
                    }
                    // if the service is not null but is stopped then we need to cleanup and start
                    else if ( ! service.isStarted() )
                    {
                        cleanup( notifier );
                        try
                        {
                            service.startup();
                        }
                        catch ( NamingException e )
                        {
                            notifier.testAborted( description, e );
                        }
                    }
                    // server is up and running so we just need to revert it to the last tag
                    else
                    {
                        Tag latest;

                        try
                        {
                            latest = service.getChangeLog().getLatest();
                            if ( latest == null && ! ( service.getChangeLog().getCurrentRevision() <= 0 ) )
                            {
                                service.revert( 0 );
                            }
                            else if ( latest != null && 
                                ! ( service.getChangeLog().getCurrentRevision() <= latest.getRevision() ) )
                            {
                                service.revert( latest.getRevision() );
                            }

                            service.getChangeLog().tag();
                        }
                        catch ( NamingException e )
                        {
                            notifier.testAborted( description, e );
                        }
                    }
                    break;
                case( SetupMode.RESTART_ORDINAL ):
                    // if the service is null instantiate it w/ factory
                    if ( service == null )
                    {
                        service = methodFactory.newInstance();
                        service.getChangeLog().setEnabled( true );

                        try
                        {
                            service.startup();
                            service.getChangeLog().tag();
                        }
                        catch ( NamingException e )
                        {
                            notifier.testAborted( description, e );
                        }
                    }
                    // if the service is not null but is stopped then we need to just start
                    else if ( ! service.isStarted() )
                    {
                        try
                        {
                            service.startup();
                            service.getChangeLog().tag();
                        }
                        catch ( NamingException e )
                        {
                            notifier.testAborted( description, e );
                        }
                    }
                    // server is up and running so we just need to restart it
                    else
                    {
                        try
                        {
                            service.shutdown();
                            service.startup();
                            service.getChangeLog().tag();
                        }
                        catch ( NamingException e )
                        {
                            notifier.testAborted( description, e );
                        }
                    }
                    break;
                case( SetupMode.PRISTINE_ORDINAL ):
                    // if the service is null instantiate it w/ factory
                    if ( service == null )
                    {
                        service = methodFactory.newInstance();
                        service.getChangeLog().setEnabled( true );

                        try
                        {
                            doDelete( service.getWorkingDirectory() );
                            service.startup();
                            service.getChangeLog().tag();
                        }
                        catch ( Exception e )
                        {
                            notifier.testAborted( description, e );
                        }
                    }
                    // if the service is not null but is stopped then we need to cleanup and start
                    else if ( ! service.isStarted() )
                    {
                        cleanup( notifier );
                        service = methodFactory.newInstance();
                        service.getChangeLog().setEnabled( true );
                        cleanup( notifier );
                        try
                        {
                            service.startup();
                            service.getChangeLog().tag();
                        }
                        catch ( NamingException e )
                        {
                            notifier.testAborted( description, e );
                        }
                    }
                    // server is up and running so we just need to revert it to the last tag
                    else
                    {
                        try
                        {
                            service.shutdown();
                        }
                        catch ( NamingException e )
                        {
                            notifier.testAborted( description, e );
                        }
                        cleanup( notifier );
                        service = methodFactory.newInstance();
                        service.getChangeLog().setEnabled( true );
                        cleanup( notifier );
                        try
                        {
                            service.startup();
                            service.getChangeLog().tag();
                        }
                        catch ( NamingException e )
                        {
                            notifier.testAborted( description, e );
                        }
                    }
                    break;
                default:
                    throw new IllegalStateException( "Unidentified method mode: " + methodMode );
            }

            try
            {
                Field field = test.getClass().getDeclaredField( "service" );
                if ( ! DirectoryService.class.isAssignableFrom( field.getType() ) )
                {
                    //noinspection ThrowableInstanceNeverThrown
                    notifier.testAborted( description,
                            new RuntimeException( INVALID_SERVICE_FIELD_MSG ).fillInStackTrace() );
                }

                field.set( test.getClass(), service );
            }
            catch ( Exception e )
            {
                notifier.testAborted( description, e );
            }
        }

        new MethodRoadie( test, testMethod, notifier, description ).run();
    }


    public void setSuite( CiSuite suite )
    {
        this.suite = suite;
    }


    public SetupMode getSetupMode()
    {
        return setupMode;
    }


    public DirectoryServiceFactory getFactory()
    {
        return factory;
    }


    public DirectoryService getService()
    {
        return service;
    }
}
