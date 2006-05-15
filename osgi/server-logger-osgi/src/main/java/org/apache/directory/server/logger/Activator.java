/*
 *   Copyright 2006 The Apache Software Foundation
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

package org.apache.directory.server.logger;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator
{
    private static final String LOG_PROPERTIES_LOCATION = "log4j.configuration";

    private Logger log;

    public void start( BundleContext bundleContext ) throws Exception
    {
        try
        {
            resetLog4j( bundleContext );
        }
        catch ( Exception e )
        {
            //e.printStackTrace();
        }
        log = LoggerFactory.getLogger( Activator.class );
        log.debug( "Reset log configuration." );
    }

    public void stop( BundleContext arg0 ) throws Exception
    {
    }

    /**
     * @return url of the log4j.properties configuration file
     * 
     * @throws MalformedURLException
     * 
     */
    private URL getLoggingProperty( BundleContext bundleContext ) throws MalformedURLException
    {
        final String logPropertiesLocation = bundleContext.getProperty( LOG_PROPERTIES_LOCATION );
        return new URL( logPropertiesLocation );
    }

    /**
     * Reset the log4j configuration.
     * @param bundleContext
     * @throws MalformedURLException
     * @throws FileNotFoundException
     */
    private void resetLog4j( BundleContext bundleContext ) throws MalformedURLException, FileNotFoundException
    {
        LogManager.resetConfiguration();
        URL log4jprops = getLoggingProperty( bundleContext );

        if ( log4jprops != null )
        {
            PropertyConfigurator.configure( log4jprops );
        }
        else
        {
            throw new FileNotFoundException( bundleContext.getProperty( LOG_PROPERTIES_LOCATION )
                    + " could not be found. " + "Please specify the file and restart the "
                    + bundleContext.getBundle().getLocation() + " bundle." );
        }
    }
}
