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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.UUID;

import org.apache.directory.api.util.FileUtils;
import org.apache.directory.server.annotations.CreateKdcServer;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.changelog.ChangeLog;
import org.apache.directory.server.core.factory.DSAnnotationProcessor;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.core.factory.DirectoryServiceFactory;
import org.apache.directory.server.core.factory.PartitionFactory;
import org.apache.directory.server.factory.ServerAnnotationProcessor;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.directory.server.ldap.LdapServer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApacheDSTestExtension implements BeforeEachCallback, AfterEachCallback, BeforeAllCallback, AfterAllCallback
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( ApacheDSTestExtension.class );

    private static final String CLASS_DS = "service";
    private static final String CLASS_LS = "ldapServer";
    private static final String CLASS_KDC = "kdcServer";
    private static final String METHOD_DS = "methodService";
    private static final String REVISION = "revision";

    private DirectoryService getDirectoryService( ExtensionContext context, String fieldName ) 
        throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        Class<?> testClass = context.getTestClass().get();
        Field field = testClass.getField( fieldName );
        
        if ( field != null )
        {
            return ( DirectoryService ) field.get( testClass );
        }
        else
        {
            return null;
        }
    }

    
    private void setDirectoryService( ExtensionContext context, String fieldName, DirectoryService directoryService ) 
        throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        Class<?> testClass = context.getTestClass().get();
        Field field = testClass.getField( fieldName );
        field.set( null, directoryService );
    }
    
    
    private LdapServer getLdapServer( ExtensionContext context, String fieldName ) 
        throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        Class<?> testClass = context.getTestClass().get();
        Field field = testClass.getField( fieldName );
        
        if ( field != null )
        {
            return ( LdapServer ) field.get( testClass );
        }
        else
        {
            return null;
        }
    }


    private void setLdapServer( ExtensionContext context, String fieldName, LdapServer ldapServer ) 
        throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        Class<?> testClass = context.getTestClass().get();
        Field field = testClass.getField( fieldName );
        field.set( null, ldapServer );
    }
    
    
    private KdcServer getKdcServer( ExtensionContext context, String fieldName ) 
        throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        Class<?> testClass = context.getTestClass().get();
        Field field = testClass.getField( fieldName );
        
        if ( field != null )
        {
            return ( KdcServer ) field.get( testClass );
        }
        else
        {
            return null;
        }
    }


    private void setKdcServer( ExtensionContext context, String fieldName, KdcServer kdcServer ) 
        throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        Class<?> testClass = context.getTestClass().get();
        Field field = testClass.getField( fieldName );
        field.set( null, kdcServer );
    }

    
    private long getRevision( ExtensionContext context, String fieldName ) 
        throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        Class<?> testClass = context.getTestClass().get();
        Field field = testClass.getField( fieldName );
        
        if ( field != null )
        {
            return ( long ) field.get( testClass );
        }
        else
        {
            return 0L;
        }
    }


    private void setRevision( ExtensionContext context, long revision ) 
        throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        Class<?> testClass = context.getTestClass().get();
        Field field = testClass.getField( REVISION );
        field.set( null, revision );
    }


    @Override
    public void beforeAll( ExtensionContext context ) throws Exception
    {
        // Don't run the test if the @Disabled annotation is used
        if ( context.getTestClass().get().getAnnotation( Disabled.class ) != null )
        {
            return;
        }
        
        try
        {
            AnnotatedElement annotations = context.getTestClass().get();

            CreateDS dsBuilder = annotations.getDeclaredAnnotation( CreateDS.class );

            DirectoryService classDS = DSAnnotationProcessor.getDirectoryService( dsBuilder );
            long revision = 0L;

            if ( classDS == null )
            {
                // No : define a default class DS then
                DirectoryServiceFactory dsf = DefaultDirectoryServiceFactory.class.newInstance();

                classDS = dsf.getDirectoryService();
                
                // enable CL explicitly cause we are not using DSAnnotationProcessor
                classDS.getChangeLog().setEnabled( true );

                dsf.init( "default" + UUID.randomUUID().toString() );

                // Load the schemas
                DSAnnotationProcessor.loadSchemas( context, classDS );
            }

            // Apply the class LDIFs
            DSAnnotationProcessor.applyLdifs( annotations, context.getDisplayName(), classDS );
            
            setDirectoryService( context, CLASS_DS, classDS );
            
            // Store the revision
            //revision = classDS.getChangeLog().getCurrentRevision();
            
            //setRevision( context, revision );
            
            // check if it has a LdapServerBuilder
            // then use the DS created above
            CreateLdapServer classLdapServerBuilder = annotations.getDeclaredAnnotation( CreateLdapServer.class );

            if ( classLdapServerBuilder != null )
            {
                LdapServer classLdapServer = ServerAnnotationProcessor.createLdapServer( annotations, classDS );
                setLdapServer( context, CLASS_LS, classLdapServer );
            }

            CreateKdcServer classKdcServerBuilder = annotations.getDeclaredAnnotation( CreateKdcServer.class );

            if ( classKdcServerBuilder != null )
            {
                int minPort = getMinPort();

                KdcServer classKdcServer = ServerAnnotationProcessor.getKdcServer( annotations, classDS, minPort + 1 );
                setKdcServer( context, CLASS_LS, classKdcServer );
            }

            // print out information which partition factory we use
            DirectoryServiceFactory dsFactory = DefaultDirectoryServiceFactory.class.newInstance();
            PartitionFactory partitionFactory = dsFactory.getPartitionFactory();
            LOG.debug( "Using partition factory {}", partitionFactory.getClass().getSimpleName() );

        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        finally
        {
            // help GC to get rid of the directory service with all its references
            System.out.println( "" );
        }
    }
    
    
    public void afterAll( ExtensionContext context )
    {
        try
        {
            LdapServer classLdapServer = getLdapServer( context, CLASS_LS );
            
            if ( classLdapServer != null )
            {
                classLdapServer.stop();
            }
    
            LdapServer classKdcServer = getLdapServer( context, CLASS_KDC );

            if ( classKdcServer != null )
            {
                classKdcServer.stop();
            }
    
            // cleanup classService if it is not the same as suite service or
            // it is not null (this second case happens in the absence of a suite)
            DirectoryService classDS = getDirectoryService( context, CLASS_DS );
            
            if ( classDS != null )
            {
                LOG.debug( "Shuting down DS for {}", classDS.getInstanceId() );
                classDS.shutdown();
                FileUtils.deleteDirectory( classDS.getInstanceLayout().getInstanceDirectory() );
            }
            else
            {
                // Revert the ldifs
                // We use a class or suite DS, just revert the current test's modifications
                long revision = getRevision( context, REVISION );

                revert( classDS, revision );
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            LOG.error( I18n.err( I18n.ERR_181, context.getTestClass().get().getName() ) );
            LOG.error( e.getLocalizedMessage() );
        }
    }


    /**
     * We may have a method DS. In this case, it will be used for the method, the class
     * DS will simply be ignored.
     * 
     * As we don't allow method LdapServers, we will use the class LS if there is one declared.
     * 
     * In any case, the method test is fully standalone.
     * 
     * Note that we will apply all the ldifs, including the class level ones.
     */
    @Override
    public void beforeEach( ExtensionContext context ) throws Exception
    {
        // Don't run the test if the @Disabled annotation is used
        if ( context.getTestMethod().get().getAnnotation( Disabled.class ) != null )
        {
            return;
        }

        // Get the applyLdifs for each level
        AnnotatedElement classAnnotation = context.getTestClass().get();
        AnnotatedElement methodAnnotation = context.getTestMethod().get();

        // Before running any test, check to see if we must create a method DS
        CreateDS dsBuilder = methodAnnotation.getDeclaredAnnotation( CreateDS.class );

        // Get the class LS if there is one
        LdapServer classLS = getLdapServer( context, CLASS_LS );
        DirectoryService classDS = getDirectoryService( context, CLASS_DS );
        
        DirectoryService service = classDS;
        
        if ( dsBuilder != null )
        {
            // Build the DS server now
            try
            {
                // Check if this method has a dedicated DSBuilder
                DirectoryService methodDS = DSAnnotationProcessor.getDirectoryService( dsBuilder );
    
                // give #1 priority to method level DS if present
                // Apply all the LDIFs, class and method ones

                // we don't support method level LdapServer so
                // we check for the presence of Class level LdapServer first 
                service = methodDS;
            }
            catch ( Exception e )
            {
            }
        }
        
        DSAnnotationProcessor.applyLdifs( methodAnnotation, context.getDisplayName(), service );
        
        // Get current revision
        if ( ( service != null ) && ( service.getChangeLog().isEnabled() ) )
        {
            long revision = service.getChangeLog().getCurrentRevision();
            setRevision( context, revision );
        }
    }


    @Override
    public void afterEach( ExtensionContext context ) throws Exception
    {
        DirectoryService methodDS = getDirectoryService( context, METHOD_DS );
        DirectoryService classDS = getDirectoryService( context, CLASS_DS );
        long revision = getRevision( context, REVISION );
        
        try
        {
            // Cleanup the methodDS if it has been created
            if ( methodDS != null )
            {
                LOG.debug( "Shuting down DS for {}", methodDS.getInstanceId() );
                methodDS.shutdown();
                FileUtils.deleteDirectory( methodDS.getInstanceLayout().getInstanceDirectory() );
            }
            else
            {
                // We use a class or suite DS, just revert the current test's modifications
                revert( classDS, revision );
            }
        }
        catch ( Exception e )
        {
            
        }
    }


    /**
     * Get the lower port out of all the transports
     */
    private int getMinPort()
    {
        return 0;
    }


    private void revert( DirectoryService dirService, long revision ) throws Exception
    {
        if ( dirService == null )
        {
            return;
        }

        ChangeLog cl = dirService.getChangeLog();
        if ( cl.isEnabled() && ( revision < cl.getCurrentRevision() ) )
        {
            LOG.debug( "Revert revision {}", revision );
            dirService.revert( revision );
        }
    }
}
