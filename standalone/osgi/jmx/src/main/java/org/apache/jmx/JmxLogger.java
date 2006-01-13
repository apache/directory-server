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

package org.apache.jmx;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JmxLogger implements BundleActivator, JmxLoggerMBean
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( JmxLogger.class );

    private ObjectName osgiName = null;

    public void start( BundleContext context ) throws Exception
    {
        ServiceReference sr = context.getServiceReference( MBeanServer.class.getName() );

        if ( sr != null )
        {
            MBeanServer server = (MBeanServer) context.getService( sr );
            osgiName = new ObjectName( "OSGI:name=OSGi Server" );
            server.registerMBean( this, osgiName );
        }
        else
        {
            throw new BundleException( "No JMX Agent" );
        }
    }

    public void stop( BundleContext context )
    {
        ServiceReference sr = context.getServiceReference( MBeanServer.class.getName() );

        if ( sr != null )
        {
            MBeanServer server = (MBeanServer) context.getService( sr );

            try
            {
                server.unregisterMBean( osgiName );
            }
            catch ( Exception e )
            {
                log.error( e.getMessage(), e );
            }
        }

        osgiName = null;
    }

    public void log()
    {
        log.debug( "JMX Logger says SUCCESS." );
    }
}
