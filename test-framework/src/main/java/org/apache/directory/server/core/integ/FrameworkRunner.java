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


import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.DefaultLdapServerFactory;
import org.apache.directory.server.annotations.LdapServerBuilder;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.annotations.DSBuilder;
import org.apache.directory.server.core.factory.DSBuilderAnnotationProcessor;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.core.factory.DirectoryServiceFactory;
import org.junit.Ignore;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The class responsible for running all the tests. t read the annotations, 
 * initialize the DirectoryService, call each test and do the cleanup at the end.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class FrameworkRunner extends BlockJUnit4ClassRunner
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( FrameworkRunner.class );

    /** The 'service' field in the run tests */
    private static final String DIRECTORY_SERVICE_FIELD_NAME = "service";
    
    /** The filed used to tell the test that it is run in a suite */
    private static final String IS_RUN_IN_SUITE_FIELD_NAME = "isRunInSuite";

    /** The suite this class depend on, if any */
    private FrameworkSuite suite;

    /** The LdapServerBuilder for this class, if any */
    private LdapServerBuilder classLdapServerBuilder;

    /** The DirectoryService for this class, if any */
    private DirectoryService classDS;

    /** A flag set to true when the class has been started */
    private boolean classStarted = false;

    /** Is it usefull ? */
    private static AtomicInteger testCount = new AtomicInteger();


    /**
     * Creates a new instance of FrameworkRunner.
     */
    public FrameworkRunner( Class<?> clazz ) throws InitializationError
    {
        super( clazz );
    }


    /**
     * Instantiate the DirectoryService we found in an annotation
     */
    private DirectoryService getDirectoryServer( DSBuilder factory ) throws Exception
    {
        DirectoryServiceFactory dsf = ( DirectoryServiceFactory ) factory.factory().newInstance();
        dsf.init( factory.name() );
        
        return dsf.getDirectoryService();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void run( final RunNotifier notifier )
    {
        // Before running any test, check to see if we must create a class DS
        // Get the LdapServerBuilder, if any
        classLdapServerBuilder = getDescription().getAnnotation( LdapServerBuilder.class );

        try
        {
            classDS = DSBuilderAnnotationProcessor.getDirectoryService( getDescription() );
            long revision = 0L;
            DirectoryService directoryService = null;

            
            if ( classDS != null )
            {
                // We have a class DS defined, update it
                directoryService = classDS;
                
                // Get the applyLdifs for each level and apply them
                if ( suite != null )
                {
                    DSBuilderAnnotationProcessor.applyLdifs( suite.getDescription(), classDS );
                }
                
                DSBuilderAnnotationProcessor.applyLdifs( getDescription(), classDS );
            }
            else
            {
                // No class DS. Do we have a Suite ?
                if ( suite != null ) 
                {
                    // yes. Do we have a suite DS ?
                    directoryService = suite.getDirectoryService();

                    if ( directoryService != null )
                    {
                        // yes : apply the class LDIFs only, and tag for reversion
                        revision = directoryService.getChangeLog().getCurrentRevision();
                        LOG.debug( "Create revision {}", revision );

                        // apply the class LDIFs
                        DSBuilderAnnotationProcessor.applyLdifs( getDescription(), directoryService );
                    }
                    else
                    {
                        // No : define a default DS for the suite then
                        DirectoryServiceFactory dsf = DefaultDirectoryServiceFactory.DEFAULT;
                        dsf.init( "default" + UUID.randomUUID().toString() );
                        directoryService = dsf.getDirectoryService();
                        
                        // Stores it into the suite
                        suite.setDirectoryService( directoryService );
                        
                        // Apply the suite LDIF first
                        DSBuilderAnnotationProcessor.applyLdifs( suite.getDescription(), directoryService );
                        
                        // Then tag for reversion and apply the class LDIFs
                        revision = directoryService.getChangeLog().getCurrentRevision();
                        LOG.debug( "Create revision {}", revision );
                        
                        DSBuilderAnnotationProcessor.applyLdifs( getDescription(), directoryService );
                    }
                }
                else
                {
                    // No : define a default class DS then
                    DirectoryServiceFactory dsf = DefaultDirectoryServiceFactory.DEFAULT;
                    dsf.init( "default" + UUID.randomUUID().toString() );
                    directoryService = dsf.getDirectoryService();
                    
                    // Stores the defaultDS in the classDS
                    classDS = directoryService;

                    // Apply the class LDIFs
                    DSBuilderAnnotationProcessor.applyLdifs( getDescription(), directoryService );
                }
            }

            super.run( notifier );
            
            // cleanup classService if it is not the same as suite service or
            // it is not null (this second case happens in the absence of a suite)
            if ( classDS != null )
            {
                LOG.debug( "Shuting down DS for {}", classDS.getInstanceId() );
                classDS.shutdown();
                FileUtils.deleteDirectory( classDS.getWorkingDirectory() );
            }
            else
            {
                // Revert the ldifs
                if ( revision < directoryService.getChangeLog().getCurrentRevision() )
                {
                    LOG.debug( "Revert revision {}", revision );
                    // We use a class or suite DS, just revert the current test's modifications
                    directoryService.revert( revision );
                }
            }
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to run the class {}", getTestClass().getName() );
            LOG.error( e.getMessage() );
            e.printStackTrace();
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void runChild( FrameworkMethod method, RunNotifier notifier )
    {
        testCount.incrementAndGet();

        // Don't run the test if the @Ignored annotation is used
        if ( method.getAnnotation( Ignore.class ) != null )
        {
            Description description = describeChild( method );
            notifier.fireTestIgnored( description );
            return;
        }

        // Get the applyLdifs for each level
        Description suiteDescription = null;
        
        if ( suite != null )
        {
            suiteDescription = suite.getDescription();
        }
        
        Description classDescription = getDescription();
        Description methodDescription = describeChild( method );

        // Ok, ready to run the test
        try
        {
            DirectoryService directoryService = null;
            
            // Set the revision to 0, we will revert only if it's set to another value
            long revision = 0L;

            // Check if this method has a dedicated DSBuilder
            DirectoryService methodDS = DSBuilderAnnotationProcessor.getDirectoryService( methodDescription );

            if ( methodDS != null )
            {
                // Apply all the LDIFs
                DSBuilderAnnotationProcessor.applyLdifs( suiteDescription, methodDS );
                DSBuilderAnnotationProcessor.applyLdifs( classDescription, methodDS );
                DSBuilderAnnotationProcessor.applyLdifs( methodDescription, methodDS );
                
                directoryService = methodDS;
            }
            else if ( classDS != null )
            {
                directoryService = classDS;
                
                // apply the method LDIFs, and tag for reversion
                revision = directoryService.getChangeLog().getCurrentRevision();
                LOG.debug( "Create revision {}", revision );
                
                DSBuilderAnnotationProcessor.applyLdifs( methodDescription, directoryService );
            }
            else if ( suite != null )
            {
                directoryService = suite.getDirectoryService();
                
                // apply the method LDIFs, and tag for reversion
                revision = directoryService.getChangeLog().getCurrentRevision();
                LOG.debug( "Create revision {}", revision );
                
                DSBuilderAnnotationProcessor.applyLdifs( methodDescription, directoryService );
            }

            // At this point, we know which service to use.
            // Inject it into the class
            Field field = getTestClass().getJavaClass().getField( DIRECTORY_SERVICE_FIELD_NAME );
            field.set( getTestClass().getJavaClass(), directoryService );
            
            // if we run this class in a suite, tell it to the test
            field = getTestClass().getJavaClass().getField( IS_RUN_IN_SUITE_FIELD_NAME );
            field.set( getTestClass().getJavaClass(), suite != null );

            // Last not least, see if we have to start a server
            if ( ( suite != null ) && ( suite.getSuiteLdapServerBuilder() != null ) )
            {
                LdapServerBuilder ldapServerBuilder = suite.getSuiteLdapServerBuilder();
                
                DefaultLdapServerFactory ldapServerFactory = (DefaultLdapServerFactory)ldapServerBuilder.factory().newInstance();
                ldapServerFactory.setDirectoryService( directoryService );
            }

            super.runChild( method, notifier );

            // Cleanup the methodDS if it has been created
            if ( methodDS != null )
            {
                LOG.debug( "Shuting down DS for {}", methodDS.getInstanceId() );
                methodDS.shutdown();
                FileUtils.deleteDirectory( methodDS.getWorkingDirectory() );
            }
            else if ( revision < directoryService.getChangeLog().getCurrentRevision() )
            {
                // We use a class or suite DS, just revert the current test's modifications
                directoryService.revert( revision );
            }
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to run the method {}", method );
            LOG.error( "", e );
            e.printStackTrace();
        }

        classStarted = true;
    }


    public void setSuite( FrameworkSuite suite )
    {
        this.suite = suite;
    }


    public FrameworkSuite getSuite()
    {
        return suite;
    }
}
