/*
 *   Copyright 2004 The Apache Software Foundation
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
package org.apache.directory.server.standalone.daemon;


import java.io.FileInputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The bootstrapped application is managed by this class.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class LifecycleInvoker
{
    private static Logger log = LoggerFactory.getLogger( LifecycleInvoker.class );
    private static final String BOOTSTRAP_START_CLASS_PROP = "bootstrap.start.class";
    private static final String BOOTSTRAP_STOP_CLASS_PROP = "bootstrap.stop.class";

    private DaemonApplication startObject;
    private DaemonApplication stopObject;
    private String startClassName;
    private String stopClassName;
    private final ClassLoader application;
    private final InstallationLayout layout;


    public LifecycleInvoker( String installationBase, ClassLoader parent )
    {
        layout = new InstallationLayout( installationBase );
        
        // -------------------------------------------------------------------
        // Verify correct installation layout
        // -------------------------------------------------------------------

        try
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Verifying installation layout:\n" + installationBase );
            }
            layout.verifyInstallation();
        }
        catch( Throwable t )
        {
            log.error( "Installation verification failure!", t );
            System.exit( ExitCodes.VERIFICATION );
        }
        
        // -------------------------------------------------------------------
        // Load the properties from the bootstrap configuration file
        // -------------------------------------------------------------------

        try
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Loading bootstrap configuration:\n" + layout.getBootstrapperConfigurationFile() );
            }
            Properties props = new Properties();
            props.load( new FileInputStream( layout.getBootstrapperConfigurationFile() ) );

            startClassName = props.getProperty( BOOTSTRAP_START_CLASS_PROP );
            if ( startClassName == null )
            {
                log.error( "Start class not found in " + layout.getBootstrapperConfigurationFile() );
                System.exit( ExitCodes.PROPLOAD );
            }
            else if ( log.isDebugEnabled() )
            {
                log.debug( "Start class set to: " + startClassName );
            }

            stopClassName = props.getProperty( BOOTSTRAP_STOP_CLASS_PROP );
            if ( stopClassName == null )
            {
                log.error( "Stop class not found in " + layout.getBootstrapperConfigurationFile() );
                System.exit( ExitCodes.PROPLOAD );
            }
            else if ( log.isDebugEnabled() )
            {
                log.debug( "Stop class set to: " + stopClassName );
            }
        }
        catch ( Exception e )
        {
            log.error( "Failed while loading: " + layout.getBootstrapperConfigurationFile(), e );
            System.exit( ExitCodes.PROPLOAD );
        }

        // -------------------------------------------------------------------
        // Setup the application ClassLoader using the dependencies in layout
        // -------------------------------------------------------------------

        URL[] jars = layout.getAllJars();
        this.application = new URLClassLoader( jars, parent );
        if ( log.isDebugEnabled() )
        {
            StringBuffer buf = new StringBuffer();
            buf.append( "Depenencies in application ClassLoader: \n" );
            for ( int ii = 0; ii < jars.length; ii++ )
            {
                buf.append( "\t" ).append( jars[ii] ).append( "\n" );
            }
            log.debug( buf.toString() );
        }
    }


    /**
     * Invokes the init(InstallationLayout) method of the start class via reflection 
     * using the application ClassLoader.  The application ClassLoader is set as the 
     * context ClassLoader then the method is invoked.
     */
    public void callInit( String[] args )
    {
        Class clazz = null;
        Thread.currentThread().setContextClassLoader( application );
        try
        {
            clazz = application.loadClass( startClassName );
        }
        catch ( ClassNotFoundException e )
        {
            log.error( "Could not find " + startClassName, e );
            System.exit( ExitCodes.CLASS_LOOKUP );
        }
        
        try
        {
            startObject = ( DaemonApplication ) clazz.newInstance();
        }
        catch ( Exception e )
        {
            log.error( "Could not instantiate " + startClassName, e );
            System.exit( ExitCodes.INSTANTIATION );
        }

        try
        {
            startObject.init( layout, args );
        }
        catch ( Exception e )
        {
            log.error( "Could not instantiate " + startClassName, e );
            System.exit( ExitCodes.INVOCATION );
        }
    }


    public void callStart( boolean nowait )
    {
        Thread.currentThread().setContextClassLoader( application );
        startObject.start( nowait );
    }
    

    public void callStop( String[] args )
    {
        Thread.currentThread().setContextClassLoader( application );
        Class clazz = null;

        // Reuse the startObject if it is the same class
        if ( ! startClassName.equals( stopClassName ) )
        {
            try
            {
                clazz = application.loadClass( stopClassName );
            }
            catch ( ClassNotFoundException e )
            {
                log.error( "Could not find " + stopClassName, e );
                System.exit( ExitCodes.CLASS_LOOKUP );
            }
            
            try
            {
                stopObject = ( DaemonApplication ) clazz.newInstance();
            }
            catch ( Exception e )
            {
                log.error( "Could not instantiate " + stopClassName, e );
                System.exit( ExitCodes.INSTANTIATION );
            }
        }
        else
        {
            stopObject = startObject;
            clazz = startObject.getClass();
        }

        try
        {
            stopObject.stop( args );
        }
        catch ( Exception e )
        {
            log.error( "Could not instantiate " + startClassName, e );
            System.exit( ExitCodes.INVOCATION );
        }
    }

    
    public void callDestroy()
    {
        Thread.currentThread().setContextClassLoader( application );
        stopObject.destroy();
    }
}
