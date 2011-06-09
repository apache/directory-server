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
import org.apache.directory.server.core.factory.DSAnnotationProcessor;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.core.factory.PartitionFactory;
import org.apache.directory.server.factory.ServerAnnotationProcessor;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.shared.ldap.codec.standalone.StandaloneLdapApiService;
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
     * Initialize the codec service
     */
    static 
    {
        // Load the extension points
        System.setProperty( StandaloneLdapApiService.DEFAULT_CONTROLS_LIST, 
            "org.apache.directory.shared.ldap.codec.controls.cascade.CascadeFactory," +
            "org.apache.directory.shared.ldap.codec.controls.manageDsaIT.ManageDsaITFactory," +
            "org.apache.directory.shared.ldap.codec.controls.search.entryChange.EntryChangeFactory," +
            "org.apache.directory.shared.ldap.codec.controls.search.pagedSearch.PagedResultsFactory," +
            "org.apache.directory.shared.ldap.codec.controls.search.persistentSearch.PersistentSearchFactory," +
            "org.apache.directory.shared.ldap.codec.controls.search.subentries.SubentriesFactory" );

        System.setProperty( StandaloneLdapApiService.EXTRA_CONTROLS_LIST, 
            "org.apache.directory.shared.ldap.extras.controls.ppolicy.PasswordPolicyFactory," +
            "org.apache.directory.shared.ldap.extras.controls.syncrepl_impl.SyncDoneValueFactory," +
            "org.apache.directory.shared.ldap.extras.controls.syncrepl_impl.SyncInfoValueFactory," +
            "org.apache.directory.shared.ldap.extras.controls.syncrepl_impl.SyncModifyDnFactory," +
            "org.apache.directory.shared.ldap.extras.controls.syncrepl_impl.SyncRequestValueFactory," +
            "org.apache.directory.shared.ldap.extras.controls.syncrepl_impl.SyncStateValueFactory" );
        
        System.setProperty( StandaloneLdapApiService.DEFAULT_EXTENDED_OPERATION_REQUESTS_LIST, 
            "org.apache.directory.shared.ldap.extras.extended.ads_impl.cancel.CancelFactory," +
            "org.apache.directory.shared.ldap.extras.extended.ads_impl.certGeneration.CertGenerationFactory," +
            "org.apache.directory.shared.ldap.extras.extended.ads_impl.gracefulShutdown.GracefulShutdownFactory," +
            "org.apache.directory.shared.ldap.extras.extended.ads_impl.storedProcedure.StoredProcedureFactory" );

        System.setProperty( StandaloneLdapApiService.DEFAULT_EXTENDED_OPERATION_RESPONSES_LIST, 
            "org.apache.directory.shared.ldap.extras.extended.ads_impl.gracefulDisconnect.GracefulDisconnectFactory" );
    }

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
    // This will suppress PMD.EmptyCatchBlock warnings in this method
    @SuppressWarnings("PMD.EmptyCatchBlock")
    private void stopDS()
    {
        if ( directoryService != null )
        {
            try
            {
                LOG.debug( "Shuting down DS for {}", directoryService.getInstanceId() );
                directoryService.shutdown();
                FileUtils.deleteDirectory( directoryService.getInstanceLayout().getInstanceDirectory() );
            }
            catch ( Exception e )
            {
                // Do nothing
            }
        }
    }

    
    private void startLdapServer( Description description ) throws Exception
    {
        ldapServer = ServerAnnotationProcessor.createLdapServer( description, directoryService, 1024 );
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
            LOG.debug( "Using partition factory {}", partitionFactory.getClass().getSimpleName() );

            // Create and initialize the Suite DS
            startDS( getDescription() );
            
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
        finally
        {
            // help GC to get rid of the directory service with all its references
            directoryService = null;
            ldapServerBuilder = null;
            ldapServer = null;
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
