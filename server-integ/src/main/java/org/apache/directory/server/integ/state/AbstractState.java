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
package org.apache.directory.server.integ.state;


import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.server.integ.InheritableServerSettings;
import org.apache.directory.shared.ldap.ldif.LdifEntry;
import org.apache.directory.shared.ldap.ldif.LdifReader;
import org.junit.internal.runners.TestClass;
import org.junit.internal.runners.TestMethod;
import org.junit.runner.notification.RunNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The abstract state of a test service, containing the default state 
 * transitions
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class AbstractState implements TestServerState
{
    /** The class logger */
    private static final Logger LOG = LoggerFactory.getLogger( AbstractState.class );

    /** The context for this test */
    protected final TestServerContext context;

    /** Error message when we can't destroy the service */
    private static final String DESTROY_ERR = "Cannot destroy when service is in NonExistant state";
    private static final String CLEANUP_ERROR = "Cannot cleanup when service is in NonExistant state";
    private static final String STARTUP_ERR = "Cannot startup when service is in NonExistant state";
    private static final String SHUTDOWN_ERR = "Cannot shutdown service in NonExistant state.";
    private static final String REVERT_ERROR = "Cannot revert when service is in NonExistant state";

    /**
     * 
     * Creates a new instance of AbstractState.
     *
     * @param context The associated context
     */
    protected AbstractState( TestServerContext context )
    {
        this.context = context;
    }


    /**
     * Action where an attempt is made to create the service.  Service
     * creation in this system is the combined instantiation and
     * configuration which takes place when the factory is used to get
     * a new instance of the service.
     *
     * @param settings The inherited settings
     * @throws NamingException if we can't create the service
     */
    public void create( InheritableServerSettings settings ) throws NamingException
    {
    }


    /**
     * Action where an attempt is made to destroy the service. This
     * entails nulling out reference to it and triggering garbage
     * collection.
     */
    public void destroy()
    {
        LOG.error( DESTROY_ERR );
        throw new IllegalStateException( DESTROY_ERR );
    }


    /**
     * Action where an attempt is made to erase the contents of the
     * working directory used by the service for various files including
     * partition database files.
     *
     * @throws IOException on errors while deleting the working directory
     */
    public void cleanup() throws  IOException
    {
        LOG.error( CLEANUP_ERROR );
        throw new IllegalStateException( CLEANUP_ERROR );
    }


    /**
     * Action where an attempt is made to start up the service.
     *
     * @throws Exception on failures to start the core directory service
     */
    public void startup() throws Exception
    {
        LOG.error( STARTUP_ERR );
        throw new IllegalStateException( STARTUP_ERR );
    }


    /**
     * Action where an attempt is made to shutdown the service.
     *
     * @throws Exception on failures to stop the core directory service
     */
    public void shutdown() throws Exception
    {
        LOG.error( SHUTDOWN_ERR );
        throw new IllegalStateException( SHUTDOWN_ERR );
    }


    /**
     * Action where an attempt is made to run a test against the service.
     *
     * All annotations should have already been processed for
     * InheritableServerSettings yet they and others can be processed since we have
     * access to the method annotations below
     *
     * @param testClass the class whose test method is to be run
     * @param testMethod the test method which is to be run
     * @param notifier a notifier to report failures to
     * @param settings the inherited settings and annotations associated with
     * the test method
     */
    public void test( TestClass testClass, TestMethod testMethod, RunNotifier notifier, InheritableServerSettings settings )
    {
    }


    /**
     * Action where an attempt is made to revert the service to it's
     * initial start up state by using a previous snapshot.
     *
     * @throws Exception on failures to revert the state of the core
     * directory service
     */
    public void revert() throws Exception
    {
        LOG.error( REVERT_ERROR );
        throw new IllegalStateException( REVERT_ERROR );
    }

    
    /**
     * Inject the Ldifs if any
     *
     * @param service the instantiated directory service
     * @param settings the settings containing the ldif
     */
    protected void injectLdifs( DirectoryService service, InheritableServerSettings settings )
    {
        List<String> ldifs = new ArrayList<String>();

        ldifs =  settings.getLdifs( ldifs );
        
        if ( ldifs.size() != 0 )
        {
            for ( String ldif:ldifs )
            {
                try
                {
                    StringReader in = new StringReader( ldif );
                    LdifReader ldifReader = new LdifReader( in );
                    LdifEntry entry = ldifReader.next();
                    
                    LdapContext root = IntegrationUtils.getRootContext( service );
                    root.createSubcontext( entry.getDn(), entry.getAttributes() );
                    LOG.debug( "Injected LDIF for test {}: {}", settings.getDescription(), ldif );
                }
                catch ( Exception e )
                {
                    LOG.error( "Cannot inject the following entry : {}. Error : {}.", ldif, e.getMessage() );
                }
            }
        }
    }
}
