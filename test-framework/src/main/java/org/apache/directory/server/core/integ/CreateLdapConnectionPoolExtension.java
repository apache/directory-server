/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.core.integ;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.directory.api.ldap.codec.api.DefaultConfigurableBinaryAttributeDetector;
import org.apache.directory.api.util.Network;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapConnectionFactory;
import org.apache.directory.ldap.client.api.LdapConnectionPool;
import org.apache.directory.ldap.client.api.LdapConnectionValidator;
import org.apache.directory.ldap.client.template.LdapConnectionTemplate;
import org.apache.directory.server.annotations.CreateLdapConnectionPool;
import org.apache.directory.server.ldap.LdapServer;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateLdapConnectionPoolExtension implements BeforeAllCallback, AfterAllCallback
{
    private static final Logger LOG = LoggerFactory.getLogger( CreateLdapConnectionPoolExtension.class );
    private static final String LDAP_CONNECTION_TEMPLATE = "ldapConnectionTemplate";
    private static final String LDAP_CONNECTION_FACTORY = "ldapConnectionFactory";
    private static final String LDAP_CONNECTION_POOL = "ldapConnectionPool";
    
    private LdapConnectionFactory ldapConnectionFactory;

    private void setLdapConnectionTemplate( ExtensionContext context, LdapConnectionTemplate ldapConnectionTemplate ) 
        throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        Class<?> testClass = context.getTestClass().get();

        try
        {
            Field field = testClass.getField( LDAP_CONNECTION_TEMPLATE );
            field.set( null, ldapConnectionTemplate );
        }
        catch ( NoSuchFieldException nsfe )
        {
            // Ignore
        }
    }
    
    
    private void setLdapConnectionPool( ExtensionContext context, LdapConnectionPool ldapConnectionPool ) 
        throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        Class<?> testClass = context.getTestClass().get();
        
        try
        {
            Field field = testClass.getField( LDAP_CONNECTION_POOL );
            field.set( null, ldapConnectionPool );
        }
        catch ( NoSuchFieldException nsfe )
        {
            // Ignore
        }
    }
    
    
    private void setLdapConnectionFactory( ExtensionContext context, LdapConnectionFactory ldapConnectionFactory ) 
        throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        Class<?> testClass = context.getTestClass().get();
        
        try
        {
            Field field = testClass.getField( LDAP_CONNECTION_FACTORY );
        
            field.set( null, ldapConnectionFactory );
        }
        catch ( NoSuchFieldException nsfe )
        {
            // Ignore
        }
    }
    
    
    private LdapServer getLdapServer( ExtensionContext context ) 
        throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        Class<?> testClass = context.getTestClass().get();
        Field field = testClass.getField( ApacheDSTestExtension.CLASS_LS );
        
        if ( field != null )
        {
            return ( LdapServer ) field.get( testClass );
        }
        else
        {
            return null;
        }
    }

    @Override
    public void beforeAll( ExtensionContext context ) throws Exception
    {
        AnnotatedElement annotations = context.getTestClass().get();
        CreateLdapConnectionPool createLdapConnectionPool = annotations.getAnnotation( CreateLdapConnectionPool.class );
        
        LdapConnectionTemplate ldapConnectionTemplate;
        LdapServer ldapServer = getLdapServer( context );

        if ( createLdapConnectionPool != null )
        {
            LOG.trace( "Creating connection pool to new ldap server" );

            Class<? extends PooledObjectFactory<LdapConnection>> factoryClass =
                    createLdapConnectionPool.factoryClass();
            Class<? extends LdapConnectionFactory> connectionFactoryClass =
                    createLdapConnectionPool.connectionFactoryClass();
            Class<? extends LdapConnectionValidator> validatorClass =
                    createLdapConnectionPool.validatorClass();
            LdapConnectionPool ldapConnectionPool = createLdapConnectionPool( createLdapConnectionPool, ldapServer, factoryClass, 
                        connectionFactoryClass, validatorClass );
            ldapConnectionTemplate = new LdapConnectionTemplate( ldapConnectionPool );

            setLdapConnectionTemplate( context, ldapConnectionTemplate );
            setLdapConnectionFactory( context, ldapConnectionFactory );
            setLdapConnectionPool( context, ldapConnectionPool );
        }
    }


    private LdapConnectionPool createLdapConnectionPool( 
        CreateLdapConnectionPool createLdapConnectionPool,
        LdapServer ldapServer, 
        Class<? extends PooledObjectFactory<LdapConnection>> factoryClass,
        Class<? extends LdapConnectionFactory> connectionFactoryClass,
        Class<? extends LdapConnectionValidator> validatorClass )
    {
        LdapConnectionConfig config = new LdapConnectionConfig();
        
        config.setLdapHost( Network.LOOPBACK_HOSTNAME );
        
        config.setLdapPort( ldapServer.getPort() );
        config.setName( "uid=admin,ou=system" );
        config.setCredentials( "secret" );

        if ( ( createLdapConnectionPool.additionalBinaryAttributes() != null )
            && ( createLdapConnectionPool.additionalBinaryAttributes().length > 0 ) )
        {
            DefaultConfigurableBinaryAttributeDetector binaryAttributeDetector =
                new DefaultConfigurableBinaryAttributeDetector();
            binaryAttributeDetector.addBinaryAttribute(
                createLdapConnectionPool.additionalBinaryAttributes() );
            config.setBinaryAttributeDetector( binaryAttributeDetector );
        }

        GenericObjectPoolConfig<LdapConnection> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setLifo( createLdapConnectionPool.lifo() );
        poolConfig.setMaxTotal( createLdapConnectionPool.maxActive() );
        poolConfig.setMaxIdle( createLdapConnectionPool.maxIdle() );
        poolConfig.setMaxWait( Duration.ofMillis( createLdapConnectionPool.maxWait() ) );
        poolConfig.setMinEvictableIdleDuration( Duration.ofMillis( createLdapConnectionPool
            .minEvictableIdleTimeMillis() ) );
        poolConfig.setMinIdle( createLdapConnectionPool.minIdle() );
        poolConfig.setNumTestsPerEvictionRun( createLdapConnectionPool
            .numTestsPerEvictionRun() );
        poolConfig.setSoftMinEvictableIdleDuration( Duration.ofMillis( createLdapConnectionPool
            .softMinEvictableIdleTimeMillis() ) );
        poolConfig.setTestOnBorrow( createLdapConnectionPool.testOnBorrow() );
        poolConfig.setTestOnReturn( createLdapConnectionPool.testOnReturn() );
        poolConfig.setTestWhileIdle( createLdapConnectionPool.testWhileIdle() );
        poolConfig.setTimeBetweenEvictionRuns( Duration.ofMillis( createLdapConnectionPool
            .timeBetweenEvictionRunsMillis() ) );
        poolConfig.setBlockWhenExhausted( createLdapConnectionPool
            .whenExhaustedAction() == 1 );
        
        PooledObjectFactory<LdapConnection> poolableLdapConnectionFactory;
        
        try
        {
            Constructor<? extends LdapConnectionFactory> constructor = 
                    connectionFactoryClass.getConstructor( LdapConnectionConfig.class );
            ldapConnectionFactory = constructor.newInstance( config );
        }
        catch ( Exception e )
        {
            throw new IllegalArgumentException( "invalid connectionFactoryClass " 
                    + connectionFactoryClass.getName() + ": " + e.getMessage(), e );
        }
        try
        {
            Method timeoutSetter = connectionFactoryClass.getMethod( "setTimeOut", Long.TYPE );
            
            if ( timeoutSetter != null )
            {
                timeoutSetter.invoke( ldapConnectionFactory, createLdapConnectionPool.timeout() );
            }
        }
        catch ( Exception e )
        {
            throw new IllegalArgumentException( "invalid connectionFactoryClass "
                    + connectionFactoryClass.getName() + ", missing setTimeOut(long): " 
                    + e.getMessage(), e );
        }
        
        try
        {
            Constructor<? extends PooledObjectFactory<LdapConnection>> constructor = 
                    factoryClass.getConstructor( LdapConnectionFactory.class );
            poolableLdapConnectionFactory = constructor.newInstance( ldapConnectionFactory );
        }
        catch ( Exception e )
        {
            throw new IllegalArgumentException( "invalid factoryClass " 
                    + factoryClass.getName() + ": " + e.getMessage(), e );
        }
        try
        {
            Method setValidator = factoryClass.getMethod( "setValidator", LdapConnectionValidator.class );
            
            if ( setValidator != null )
            {
                setValidator.invoke( poolableLdapConnectionFactory, validatorClass.newInstance() );
            }
        }
        catch ( Exception e )
        {
            throw new IllegalArgumentException( "invalid connectionFactoryClass "
                    + connectionFactoryClass.getName() + ", missing setTimeOut(long): " 
                    + e.getMessage(), e );
        }

        return new LdapConnectionPool( poolableLdapConnectionFactory, poolConfig );
    }


    @Override
    public void afterAll( ExtensionContext context ) throws Exception
    {
        // TODO Auto-generated method stub
        
    }
}
