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
package org.apache.mina;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.mina.registry.ServiceRegistry;
import org.apache.mina.registry.SimpleServiceRegistry;
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

    private ServiceRegistry registry;
    private ServiceRegistration registration;

    /**
     * Implements BundleActivator.start().
     * Logs that this service is starting and starts this service.
     * @param context the framework context for the bundle.
     */
    public void start( BundleContext context ) throws BundleException
    {
        log.debug( "Starting Apache MINA Service Registry." );

        registry = new SimpleServiceRegistry();

        Dictionary parameters = new Hashtable();
        registration = context.registerService( ServiceRegistry.class.getName(), registry, parameters );
    }

    /**
     * Implements BundleActivator.stop().
     * Logs that this service has stopped.
     * @param context the framework context for the bundle.
     */
    public void stop( BundleContext context )
    {
        log.debug( "Stopping Apache MINA Service Registry." );

        registration.unregister();
        registration = null;

        registry = null;
    }
}
