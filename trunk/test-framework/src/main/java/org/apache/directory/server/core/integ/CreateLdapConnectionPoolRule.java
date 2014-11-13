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


import org.apache.commons.pool.impl.GenericObjectPool.Config;
import org.apache.directory.api.ldap.codec.api.DefaultConfigurableBinaryAttributeDetector;
import org.apache.directory.ldap.client.api.DefaultLdapConnectionFactory;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapConnectionPool;
import org.apache.directory.ldap.client.api.ValidatingPoolableLdapConnectionFactory;
import org.apache.directory.ldap.client.template.LdapConnectionTemplate;
import org.apache.directory.server.annotations.CreateLdapConnectionPool;
import org.apache.directory.server.ldap.LdapServer;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A {@link TestRule} for creating connection pools.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class CreateLdapConnectionPoolRule extends CreateLdapServerRule
{
    private static Logger LOG = LoggerFactory.getLogger( CreateLdapConnectionPoolRule.class );
    private CreateLdapConnectionPoolRule classCreateLdapConnectionPoolRule;
    private CreateLdapConnectionPool createLdapConnectionPool;
    private LdapConnectionPool ldapConnectionPool;
    private LdapConnectionTemplate ldapConnectionTemplate;


    public CreateLdapConnectionPoolRule()
    {
        this( null );
    }


    public CreateLdapConnectionPoolRule(
        CreateLdapConnectionPoolRule classCreateLdapConnectionPoolRule )
    {
        super( classCreateLdapConnectionPoolRule );
        this.classCreateLdapConnectionPoolRule = classCreateLdapConnectionPoolRule;
    }


    @Override
    public Statement apply( final Statement base, final Description description )
    {
        return super.apply( buildStatement( base, description ), description );
    }


    private Statement buildStatement( final Statement base, final Description description )
    {
        createLdapConnectionPool = description.getAnnotation( CreateLdapConnectionPool.class );
        if ( createLdapConnectionPool == null )
        {
            return new Statement()
            {
                @Override
                public void evaluate() throws Throwable
                {
                    LdapServer ldapServer = getLdapServer();
                    if ( classCreateLdapConnectionPoolRule != null
                        && classCreateLdapConnectionPoolRule.getLdapServer() != ldapServer )
                    {
                        LOG.trace( "Creating connection pool to new ldap server" );

                        LdapConnectionPool oldLdapConnectionPool = ldapConnectionPool;
                        LdapConnectionTemplate oldLdapConnectionTemplate = ldapConnectionTemplate;

                        ldapConnectionPool = classCreateLdapConnectionPoolRule
                            .createLdapConnectionPool( ldapServer );
                        ldapConnectionTemplate = new LdapConnectionTemplate( ldapConnectionPool );

                        try
                        {
                            base.evaluate();
                        }
                        finally
                        {
                            LOG.trace( "Reverting to old connection pool" );
                            ldapConnectionPool = oldLdapConnectionPool;
                            ldapConnectionTemplate = oldLdapConnectionTemplate;
                        }
                    }
                    else
                    {
                        LOG.trace( "no @CreateLdapConnectionPool on: {}", description );
                        base.evaluate();
                    }
                }
            };
        }
        else
        {
            return new Statement()
            {
                @Override
                public void evaluate() throws Throwable
                {
                    LOG.trace( "Creating ldap connection pool" );
                    ldapConnectionPool = createLdapConnectionPool( getLdapServer() );
                    ldapConnectionTemplate = new LdapConnectionTemplate( ldapConnectionPool );

                    try
                    {
                        base.evaluate();
                    }
                    finally
                    {
                        LOG.trace( "Closing ldap connection pool" );
                        ldapConnectionPool.close();
                        ldapConnectionTemplate = null;
                    }
                }
            };
        }
    }


    private LdapConnectionPool createLdapConnectionPool( LdapServer ldapServer )
    {
        LdapConnectionConfig config = new LdapConnectionConfig();
        config.setLdapHost( "localhost" );
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

        DefaultLdapConnectionFactory factory = new DefaultLdapConnectionFactory( config );
        factory.setTimeOut( createLdapConnectionPool.timeout() );

        Config poolConfig = new Config();
        poolConfig.lifo = createLdapConnectionPool.lifo();
        poolConfig.maxActive = createLdapConnectionPool.maxActive();
        poolConfig.maxIdle = createLdapConnectionPool.maxIdle();
        poolConfig.maxWait = createLdapConnectionPool.maxWait();
        poolConfig.minEvictableIdleTimeMillis = createLdapConnectionPool
            .minEvictableIdleTimeMillis();
        poolConfig.minIdle = createLdapConnectionPool.minIdle();
        poolConfig.numTestsPerEvictionRun = createLdapConnectionPool
            .numTestsPerEvictionRun();
        poolConfig.softMinEvictableIdleTimeMillis = createLdapConnectionPool
            .softMinEvictableIdleTimeMillis();
        poolConfig.testOnBorrow = createLdapConnectionPool.testOnBorrow();
        poolConfig.testOnReturn = createLdapConnectionPool.testOnReturn();
        poolConfig.testWhileIdle = createLdapConnectionPool.testWhileIdle();
        poolConfig.timeBetweenEvictionRunsMillis = createLdapConnectionPool
            .timeBetweenEvictionRunsMillis();
        poolConfig.whenExhaustedAction = createLdapConnectionPool
            .whenExhaustedAction();

        return new LdapConnectionPool(
            new ValidatingPoolableLdapConnectionFactory( factory ), poolConfig );
    }


    public LdapConnectionPool getLdapConnectionPool()
    {
        return ldapConnectionPool == null
            ? ( classCreateLdapConnectionPoolRule == null
                ? null
                : classCreateLdapConnectionPoolRule.getLdapConnectionPool() )
            : ldapConnectionPool;
    }


    public LdapConnectionTemplate getLdapConnectionTemplate()
    {
        return ldapConnectionTemplate == null
            ? ( classCreateLdapConnectionPoolRule == null
                ? null
                : classCreateLdapConnectionPoolRule.getLdapConnectionTemplate() )
            : ldapConnectionTemplate;
    }
}
