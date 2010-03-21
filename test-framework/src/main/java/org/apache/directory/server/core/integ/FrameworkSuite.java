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


import org.apache.commons.io.FileUtils;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.factory.DSAnnotationProcessor;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.core.factory.PartitionFactory;
import org.apache.directory.server.factory.ServerAnnotationProcessor;
import org.apache.directory.server.ldap.LdapServer;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A class to read and store the Suite's annotations. It's called when we start
 * running a Suite, and will call all the classes contained in the suite.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class FrameworkSuite extends Suite
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( FrameworkSuite.class );

    /** The suite DS, if any */
    private DirectoryService directoryService;
    
    /** The LdapServerBuilder for this class, if any */
    private CreateLdapServer ldapServerBuilder;

    /** The LdapServer for this class, if any */
    private LdapServer ldapServer;

    /**
     * Creates a new instance of FrameworkSuite.
     */
    public FrameworkSuite( Class<?> clazz, RunnerBuilder builder ) throws InitializationError
    {
        super( clazz, builder );
    }
    
    
    /**
     * Start and initialize the DS
     */
    private void startDS( Description description ) throws Exception
    {
        // Initialize and start the DS before running any test, if we have a DS annotation
        directoryService = DSAnnotationProcessor.getDirectoryService( description );
        
        // and inject LDIFs if needed
        if ( directoryService != null )
        {
            DSAnnotationProcessor.applyLdifs( description, directoryService );
        }
    }
    
    
    /**
     * Stop and clean the DS
     */
    private void stopDS()
    {
        if ( directoryService != null )
        {
            try
            {
                LOG.debug( "Shuting down DS for {}", directoryService.getInstanceId() );
                directoryService.shutdown();
                FileUtils.deleteDirectory( directoryService.getWorkingDirectory() );
            }
            catch ( Exception e )
            {
                // Do nothing
            }
        }
    }
    
    
    private void addPartitions( Description description )
    {
        CreatePartition createPartition = description.getAnnotation( CreatePartition.class );
        
        if ( createPartition != null )
        {
            
        }
    }

    
    private void startLdapServer( Description description ) throws Exception
    {
        ldapServer = ServerAnnotationProcessor.getLdapServer( description, directoryService, 1024 );
    }
    
    
    private void stopLdapServer()
    {
        if ( ( ldapServer != null ) && ( ldapServer.isStarted() ) )
        {
            ldapServer.stop();
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void run( final RunNotifier notifier )
    {
        try
        {
            // print out information which partition factory we use
            PartitionFactory partitionFactory = DefaultDirectoryServiceFactory.DEFAULT.getPartitionFactory();
            System.out.println( "Using partition factory " + partitionFactory.getClass().getSimpleName() );

            // Create and initialize the Suite DS
            startDS( getDescription() );
            
            // Add the partitions to this DS
            addPartitions( getDescription() );
            
            // create and initialize the suite LdapServer
            startLdapServer( getDescription() );
            
            // Run the suite
            super.run( notifier );
            
            // Stop the LdapServer
            stopLdapServer();
            
            // last, stop the DS if we have one
            stopDS();
        }
        catch ( Exception e )
        {
            notifier.fireTestFailure(new Failure(getDescription(), e));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void runChild( Runner runner, RunNotifier notifier )
    {
        // Store the suite into the class we will run
        if( runner instanceof FrameworkRunner )
        {
            ( ( FrameworkRunner ) runner ).setSuite( this );
            
            // Now, call the class containing the tests
            super.runChild( runner, notifier );
        }
        else
        {
            // there is something called org.junit.internal.builders.IgnoredClassRunner
            super.runChild( runner, notifier );
        }
    }


    /**
     * @return the DirectoryService instance
     */
    public DirectoryService getDirectoryService()
    {
        return directoryService;
    }


    /**
     * @param directoryService the directoryService to set
     */
    public void setDirectoryService( DirectoryService directoryService )
    {
        this.directoryService = directoryService;
    }


    /**
     * @return the suiteLdapServerBuilder
     */
    public CreateLdapServer getLdapServerBuilder()
    {
        return ldapServerBuilder;
    }


    /**
     * @return the suiteLdapServer
     */
    public LdapServer getLdapServer()
    {
        return ldapServer;
    }
}
