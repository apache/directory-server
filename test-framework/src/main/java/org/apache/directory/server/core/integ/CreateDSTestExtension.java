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
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;

import org.apache.directory.api.util.FileUtils;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.changelog.Tag;
import org.apache.directory.server.core.factory.DSAnnotationProcessor;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.core.factory.DirectoryServiceFactory;
import org.apache.directory.server.factory.ServerAnnotationProcessor;
import org.apache.directory.server.ldap.LdapServer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateDSTestExtension extends TypeBasedParameterResolver<DirectoryService> implements BeforeEachCallback, AfterEachCallback, BeforeAllCallback, AfterAllCallback
{
    private static final Logger LOG = LoggerFactory.getLogger( CreateDSTestExtension.class );
    
    private static final String CLASS_DS = "classDirectoryService";
    private static final String METHOD_DS = "methodDirectoryService";

    private static final Namespace TEST_NAMESPACE = Namespace.create( "apacheds-extension" );

    private void setDirectoryService( ExtensionContext context, String fieldName, DirectoryService directoryService )
        throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        // Tests that extend AbstractLdapTestUnit, or have similarly named static variables
        if ( testClassContainsField( context, fieldName ) )
        {
            Class<?> testClass = context.getRequiredTestClass();
            Field field = testClass.getField( fieldName );
            field.set( null, directoryService );
        }
    }

    private boolean testClassContainsField( ExtensionContext context, String fieldName )
    {
        Class<?> testClass = context.getRequiredTestClass();
        return Arrays.stream( testClass.getFields() )
                .anyMatch( field -> field.getName().equals( fieldName ) );
    }

    /**
     * In the BeforeALl method, we will create a DirectoryService instance that will be used by all
     * the tests for this class. This DirectoryService will be destroyed in the AfterAll callback.
     * 
     * Either we create it from the given description, or we create a default one
     * 
     * In any case, we will inject the instance in the test class.
     */
    @Override
    public void beforeAll( ExtensionContext context ) throws Exception
    {
        // Don't run the test if the @Disabled annotation is used
        if ( context.getTestClass().get().getAnnotation( Disabled.class ) != null )
        {
            return;
        }
        
        AnnotatedElement classAnnotation = context.getTestClass().get();
        
        // Check if we have a CreateS annotation. If not, we will create the DirectoryService instance
        CreateDS createDs = classAnnotation.getAnnotation( CreateDS.class );
        DirectoryService directoryService;
        
        if ( createDs == null )
        {
            // No description: create a default DS
            DirectoryServiceFactory dsf = DefaultDirectoryServiceFactory.class.newInstance();

            directoryService = dsf.getDirectoryService();

            // enable CL explicitly cause we are not using DSAnnotationProcessor
            directoryService.getChangeLog().setEnabled( true );

            dsf.init( "default" + UUID.randomUUID().toString() );

            if ( directoryService != null )
            {
                Tag tag = directoryService.getChangeLog().tag();
                DSAnnotationProcessor.applyLdifs( classAnnotation, classAnnotation.getClass().getName(), directoryService );
                LOG.debug( "Tagged change log: {}", tag );
            }
            else
            {
                LOG.trace( "no @CreateDS and no outer @CreateDS on: {}", classAnnotation.getClass().getName() );
            }
        }
        else
        {
            // We have a description, use it
            LOG.trace( "Creating directory service" );
            directoryService = DSAnnotationProcessor.getDirectoryService( createDs );
            DSAnnotationProcessor.applyLdifs( classAnnotation, classAnnotation.getClass().getName(), directoryService );
        }
        
        // Check if we have a LdapServer annotation
        CreateLdapServer createLapServer = classAnnotation.getAnnotation( CreateLdapServer.class );

        if ( createLapServer == null )
        {
            System.out.println( "" );
        }
        else
        {
            LdapServer classLdapServer = ServerAnnotationProcessor.createLdapServer( createLapServer, directoryService );
        }

        // store the DS instance in the test context
        getStore( context ).put( CLASS_DS, directoryService );

        // The created DS is now stored in the test class
        setDirectoryService( context, CLASS_DS, directoryService );
    }
    

    /** 
     * We have to shutdown the global DS now
     */
    @Override
    public void afterAll( ExtensionContext context ) throws Exception
    {
        LOG.trace( "Shutting down global directory service" );
        DirectoryService directoryService = getStore( context ).get( CLASS_DS, DirectoryService.class );
        Method shutdownMethod = directoryService.getClass().getDeclaredMethod( "shutdown", new Class[]{} );
        shutdownMethod.invoke( directoryService );
        
        FileUtils.deleteDirectory( directoryService.getInstanceLayout().getInstanceDirectory() );
    }

    
    /**
     * Here, we will create a local directoryService if needed, and only if needed
     */
    @Override
    public void beforeEach( ExtensionContext context ) throws Exception
    {
        // Don't run the test if the @Disabled annotation is used
        if ( context.getRequiredTestMethod().getAnnotation( Disabled.class ) != null )
        {
            return;
        }
        
        AnnotatedElement methodAnnotation = context.getRequiredTestMethod();
        AnnotatedElement classAnnotation = context.getRequiredTestClass();
        DirectoryService directoryService;
        
        CreateDS createDs = methodAnnotation.getAnnotation( CreateDS.class );
        
        if ( createDs != null )
        {
            LOG.trace( "Creating directory service" );
            directoryService = DSAnnotationProcessor.getDirectoryService( createDs );
            DSAnnotationProcessor.applyLdifs( classAnnotation, classAnnotation.getClass().getName(), directoryService );
            DSAnnotationProcessor.applyLdifs( methodAnnotation, methodAnnotation.getClass().getName(), directoryService );

            setDirectoryService( context, METHOD_DS, directoryService );

            // store the DS instance in the test context
            getMethodStore( context ).put( METHOD_DS, directoryService );
        }
        else
        {
            // We don't have a local DS, so use the global one
            directoryService = getStore( context ).get( CLASS_DS, DirectoryService.class );
            
            DSAnnotationProcessor.applyLdifs( methodAnnotation, methodAnnotation.getClass().getName(), directoryService );
        }
    }
    

    @Override
    public void afterEach( ExtensionContext context ) throws Exception
    {
        LOG.trace( "Shutting down directory service" );
        ExtensionContext.Store store = getMethodStore( context );
        DirectoryService directoryService = getMethodStore( context ).get( METHOD_DS, DirectoryService.class );

        if ( directoryService != null )
        {
            Method shutdownMethod = directoryService.getClass().getDeclaredMethod( "shutdown", new Class[]{} );
            shutdownMethod.invoke( directoryService );
            FileUtils.deleteDirectory( directoryService.getInstanceLayout().getInstanceDirectory() );

            // Remove instance from context
            setDirectoryService( context, METHOD_DS, null );
            store.remove( METHOD_DS );
        }
    }

    @Override
    public DirectoryService resolveParameter( ParameterContext parameterContext, ExtensionContext extensionContext ) throws ParameterResolutionException
    {
        DirectoryService directoryService = getMethodStore( extensionContext ).get( METHOD_DS, DirectoryService.class );
        if ( directoryService == null )
        {
            directoryService = getStore( extensionContext ).get( CLASS_DS, DirectoryService.class );
        }
        if ( directoryService == null )
        {
            throw new ParameterResolutionException( "Failed to resolve DirectoryService parameter" );
        }
        return directoryService;
    }

    private ExtensionContext.Store getStore( ExtensionContext extensionContext )
    {
        return extensionContext.getStore( TEST_NAMESPACE );
    }

    private ExtensionContext.Store getMethodStore( ExtensionContext extensionContext )
    {
        return extensionContext.getStore( TEST_NAMESPACE.append( extensionContext.getUniqueId() ) );
    }
}
