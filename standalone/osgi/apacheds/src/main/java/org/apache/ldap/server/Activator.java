/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.ldap.server;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

import org.apache.ldap.common.message.LockableAttributeImpl;
import org.apache.ldap.common.message.LockableAttributesImpl;
import org.apache.ldap.server.configuration.MutableDirectoryPartitionConfiguration;
import org.apache.ldap.server.configuration.MutableStartupConfiguration;
import org.apache.ldap.server.configuration.ShutdownConfiguration;
import org.apache.ldap.server.jndi.CoreContextFactory;
import org.apache.ldap.server.schema.bootstrap.ApacheSchema;
import org.apache.ldap.server.schema.bootstrap.ApachednsSchema;
import org.apache.ldap.server.schema.bootstrap.CoreSchema;
import org.apache.ldap.server.schema.bootstrap.CosineSchema;
import org.apache.ldap.server.schema.bootstrap.InetorgpersonSchema;
import org.apache.ldap.server.schema.bootstrap.Krb5kdcSchema;
import org.apache.ldap.server.schema.bootstrap.SystemSchema;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( Activator.class );

    private InitialContextFactory factory;
    private ServiceRegistration registration;

    /**
     * Implements BundleActivator.start().
     * Logs that this service is starting and starts this service.
     * @param context the framework context for the bundle.
     */
    public void start( BundleContext context ) throws BundleException
    {
        log.debug( "Starting Apache Backing Store." );

        Hashtable env = new Hashtable( setUpPartition() );

        env.put( Context.PROVIDER_URL, "dc=example,dc=com" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );
        env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.ldap.server.jndi.CoreContextFactory" );

        factory = new CoreContextFactory();

        try
        {
            factory.getInitialContext( env );
        }
        catch ( NamingException ne )
        {
            log.error( ne.getMessage(), ne );
            throw new BundleException( "Initial context load failed." );
        }

        Dictionary parameters = new Hashtable();

        registration = context.registerService( InitialContextFactory.class.getName(), factory, parameters );
    }

    /**
     * Implements BundleActivator.stop().
     * Logs that this service has stopped.
     * @param context the framework context for the bundle.
     */
    public void stop( BundleContext context ) throws BundleException
    {
        log.debug( "Stopping Apache Backing Store." );

        Hashtable env = new ShutdownConfiguration().toJndiEnvironment();
        env.put( Context.INITIAL_CONTEXT_FACTORY, CoreContextFactory.class.getName() );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.PROVIDER_URL, "dc=example,dc=com" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );

        try
        {
            factory.getInitialContext( env );
        }
        catch ( NamingException ne )
        {
            log.error( ne.getMessage(), ne );
            throw new BundleException( "Initial context shutdown failed." );
        }

        factory = null;

        registration.unregister();
        registration = null;
    }

    private Hashtable setUpPartition()
    {
        MutableStartupConfiguration config = new MutableStartupConfiguration();

        MutableDirectoryPartitionConfiguration partConfig = new MutableDirectoryPartitionConfiguration();
        partConfig.setName( "example" );

        HashSet indices = new HashSet();
        indices.add( "dc" );
        indices.add( "ou" );
        indices.add( "objectClass" );
        indices.add( "krb5PrincipalName" );
        indices.add( "uid" );
        partConfig.setIndexedAttributes( indices );

        partConfig.setSuffix( "dc=example,dc=com" );

        LockableAttributesImpl attrs = new LockableAttributesImpl();
        LockableAttributeImpl attr = new LockableAttributeImpl( "objectClass" );
        attr.add( "top" );
        attr.add( "domain" );
        attrs.put( attr );
        attrs.put( "dc", "example" );
        partConfig.setContextEntry( attrs );

        Set schemas = new HashSet();
        schemas.add( new CoreSchema() );
        schemas.add( new CosineSchema() );
        schemas.add( new ApacheSchema() );
        schemas.add( new InetorgpersonSchema() );
        schemas.add( new Krb5kdcSchema() );
        schemas.add( new SystemSchema() );
        schemas.add( new ApachednsSchema() );
        config.setBootstrapSchemas( schemas );
        config.setContextPartitionConfigurations( Collections.singleton( partConfig ) );

        partConfig.setSuffix( "dc=example,dc=com" );

        return config.toJndiEnvironment();
    }
}
