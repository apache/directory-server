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


import java.io.File;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.naming.NamingException;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.shared.ldap.ldif.LdifEntry;
import org.apache.directory.shared.ldap.ldif.LdifReader;
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

    /** The DSBuilder for this class, if any */
    private DSBuilder classDSBuilder;

    /** The DirectoryService for this class, if any */
    private DirectoryService classService;

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
        // Get the class DSBuilder, if any
        classDSBuilder = getDescription().getAnnotation( DSBuilder.class );

        try
        {
            super.run( notifier );
            
            // cleanup classService if it is not the same as suite service or
            // it is not null (this second case happens in the absence of a suite)
            if( suite != null ) 
            {
                if ( ( classService != null ) && ( classService != suite.getSuiteService() ) )
                {
                    classService.shutdown();
                    FileUtils.deleteDirectory( classService.getWorkingDirectory() );
                }
                else if ( testCount.get() == suite.testCount() )
                {
                    suite.getSuiteService().shutdown();
                    FileUtils.deleteDirectory( suite.getSuiteService().getWorkingDirectory() );
                    testCount.set( 0 );
                }
            }
            else if ( classService != null )
            {
                classService.shutdown();
                FileUtils.deleteDirectory( classService.getWorkingDirectory() );
            }
            
            
            if ( suite == null )
            {
                testCount.set( 0 );
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
            DirectoryService service = null;
            DirectoryService methodDS = null;
            long revision = 0L;

            // First check if this method has a dedicated DSBuilder
            DSBuilder methodDSBuilder = method.getAnnotation( DSBuilder.class );

            if ( methodDSBuilder != null )
            {
                // yes : instantiate the factory for this method then
                service = getDirectoryServer( methodDSBuilder );
                methodDS = service;

                // Apply all the LDIFs
                applyLdifs( suiteDescription, service );
                applyLdifs( classDescription, service );
                applyLdifs( methodDescription, service );
            }
            else if ( classDSBuilder != null )
            {
                // Use the class DS. We now have to see if it's started
                if ( classService != null )
                {
                    service = classService;

                    // get the current revision, we need it to revert the modifications
                    revision = service.getChangeLog().getCurrentRevision();

                    // Only apply the method LDIF, the class and suite(if present) LDIFs have
                    // already been applied
                    applyLdifs( methodDescription, service );
                }
                else
                {
                    // let's instantiate this class DS
                    classService = getDirectoryServer( classDSBuilder );
                    service = classService;

                    // Apply all the LDIFs
                    applyLdifs( suiteDescription, service );
                    applyLdifs( classDescription, service );

                    // get the current revision, we need it to revert the modifications
                    revision = service.getChangeLog().getCurrentRevision();
                 
                    applyLdifs( methodDescription, service );
                }
            }
            else if ( ( suite != null ) && ( suite.getSuiteDSBuilder() != null ) )
            {
                // Use the suite DS. We now have to see if it's started
                if ( suite.getSuiteService() != null )
                {
                    service = suite.getSuiteService();

                    // get the current revision, we need it to revert the modifications
                    revision = service.getChangeLog().getCurrentRevision();

                    // Apply the method and class LDIFs, if the class
                    // LDIFs haven't been applied yet
                    if ( !classStarted )
                    {
                        applyLdifs( classDescription, service );
                    }

                    // And also apply the method's LDIFs
                    applyLdifs( methodDescription, service );
                }
                else if ( suite.getSuiteDSBuilder() != null ) 
                {
                    service = getDirectoryServer( suite.getSuiteDSBuilder() );
                    
                    suite.setSuiteService( service );

                    // apply the suite LDIFs first, these will never be reverted
                    // during the running time of a test suite
                    applyLdifs( suiteDescription, service );

                    // get the current revision, we need it to revert the modifications
                    revision = service.getChangeLog().getCurrentRevision();

                    // Apply all the other LDIFs
                    applyLdifs( classDescription, service );
                    applyLdifs( methodDescription, service );
                }
            }
            else if ( ( suite != null ) && ( suite.getSuiteService() == null ) )
            {
                // Use the default DS
                DirectoryServiceFactory dsf = FrameworkDirectoryServiceFactory.DEFAULT;
                dsf.init( "default" + UUID.randomUUID().toString() );
                service = dsf.getDirectoryService();
                
                // same as in above else-if condition 
                // apply the suite LDIFs first, these will never be reverted
                // during the running time of a test suite
                applyLdifs( suiteDescription, service );

                suite.setSuiteService( service );
                // get the current revision, we need it to revert the modifications
                revision = service.getChangeLog().getCurrentRevision();

                // Apply all the other LDIFs
                applyLdifs( classDescription, service );
                applyLdifs( methodDescription, service );
            }
            // FIXME the below else if is kind of supication as
            // the above else if ( classDSBuilder != null ) condition
            else if ( classService == null ) // finally just create a default DS for class alone
            {
                // Use the default DS
                DirectoryServiceFactory dsf = FrameworkDirectoryServiceFactory.DEFAULT;
                dsf.init( "class-" + UUID.randomUUID().toString() );
                classService = dsf.getDirectoryService();
                service = classService;
                
                // apply only class LDIFs, no need to apply suite LDIFs casue if this block is executing
                // means there is no suite associated with the class
                applyLdifs( classDescription, service );

                // get the current revision, we need it to revert the modifications
                revision = service.getChangeLog().getCurrentRevision();

                applyLdifs( methodDescription, service );
            }
            else if ( classService != null )
            {
                service = classService;
                // get the current revision, we need it to revert the modifications
                revision = service.getChangeLog().getCurrentRevision();

                applyLdifs( methodDescription, service );
            }

            // At this point, we know which service to use.
            // Inject it into the class
            Field field = getTestClass().getJavaClass().getField( DIRECTORY_SERVICE_FIELD_NAME );
            field.set( getTestClass().getJavaClass(), service );
            
            // if we run this class in a suite, tell it to the test
            field = getTestClass().getJavaClass().getField( IS_RUN_IN_SUITE_FIELD_NAME );
            field.set( getTestClass().getJavaClass(), suite != null );


            super.runChild( method, notifier );

            // Cleanup the methodDS if it has been created
            if ( methodDS != null )
            {
                methodDS.shutdown();
                FileUtils.deleteDirectory( methodDS.getWorkingDirectory() );
            }
            else if( revision < service.getChangeLog().getCurrentRevision() )
            {
                // We use a class or suite DS, just revert the current test's modifications
                service.revert( revision );
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


    /**
     * Apply the LDIF entries to the given service
     */
    private void applyLdifs( Description desc, DirectoryService service ) throws Exception
    {
        if( desc == null )
        {
            return;
        }
        
        ApplyLdifFiles applyLdifFiles = desc.getAnnotation( ApplyLdifFiles.class );

        if( applyLdifFiles != null )
        {
            injectLdifFiles( service, applyLdifFiles.value() );
        }
        
        ApplyLdifs applyLdifs = desc.getAnnotation( ApplyLdifs.class ); 
        
        if ( ( applyLdifs != null ) && ( applyLdifs.value() != null ) )
        {
            String[] ldifs = applyLdifs.value();

            for ( String s : ldifs )
            {
                injectEntries( service, s );
            }
        }
    }

    /**
     * injects the LDIF entries present in a LDIF file
     * 
     * @param service the DirectoryService 
     * @param ldifFiles the array of LDIF file names (only )
     * @throws Exception
     */
    public void injectLdifFiles( DirectoryService service, String[] ldifFiles ) throws Exception
    {
        if ( ldifFiles != null && ldifFiles.length > 0 )
        {
            for ( String ldifFile : ldifFiles )
            {
                try
                {
                    Class<?> klaz = getTestClass().getJavaClass();

                    LdifReader ldifReader = new LdifReader( klaz.getClassLoader().getResourceAsStream( ldifFile ) ); 
    
                    for ( LdifEntry entry : ldifReader )
                    {
                        injectEntry( entry, service );
                    }
                    
                    ldifReader.close();
                }
                catch ( Exception e )
                {
                    LOG.error( "Cannot inject the following entry : {}. Error : {}.", ldifFile, e.getMessage() );
                }
            }
        }
    }
    
    
    /**
     * Inject an ldif String into the server. DN must be relative to the
     * root.
     *
     * @param service the directory service to use 
     * @param ldif the ldif containing entries to add to the server.
     * @throws NamingException if there is a problem adding the entries from the LDIF
     */
    public void injectEntries( DirectoryService service, String ldif ) throws Exception
    {
        LdifReader reader = new LdifReader();
        List<LdifEntry> entries = reader.parseLdif( ldif );

        for ( LdifEntry entry : entries )
        {
            injectEntry( entry, service );
        }

        // And close the reader
        reader.close();
    }

    
    /**
     * injects an LDIF entry in the given DirectoryService
     * 
     * @param entry the LdifEntry to be injected
     * @param service the DirectoryService
     * @throws Exception
     */
    private void injectEntry( LdifEntry entry, DirectoryService service ) throws Exception
    {
        if ( entry.isChangeAdd() )
        {
            service.getAdminSession().add( new DefaultServerEntry( service.getSchemaManager(), entry.getEntry() ) );
        }
        else if ( entry.isChangeModify() )
        {
            service.getAdminSession().modify( entry.getDn(), entry.getModificationItems() );
        }
        else
        {
            String message = "Unsupported changetype found in LDIF: " + entry.getChangeType();
            throw new NamingException( message );
        }
    }
}
