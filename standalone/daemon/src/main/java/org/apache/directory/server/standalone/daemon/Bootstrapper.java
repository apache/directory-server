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

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;


/**
 * The base bootstrapper extended by all frameworks and java applications.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class Bootstrapper
{
    public static final String[] EMPTY_STRARRAY = new String[0];
    
    private static final Logger log = LoggerFactory.getLogger( Bootstrapper.class );
    private static final String START_CLASS_PROP = "bootstrap.start.class";
    private static final String STOP_CLASS_PROP = "bootstrap.stop.class";
    
    private InstallationLayout layout;
    private ClassLoader application;
    private ClassLoader parent;
    private String startClassName;
    private String stopClassName;
    private Class startClass;
    private DaemonApplication start;
    private DaemonApplication stop;

    
    public void setInstallationLayout( String installationBase )
    {
        log.debug( "Setting layout in Bootstrapper using base: " + installationBase );
        layout = new InstallationLayout( installationBase );
        
        try
        {
            layout.verifyInstallation();
        }
        catch( Throwable t )
        {
            log.error( "Installation verification failure!", t );
        }
        
        try
        {
            Properties props = new Properties();
            props.load( new FileInputStream( layout.getBootstrapperConfigurationFile() ) );
            startClassName = props.getProperty( START_CLASS_PROP );
            stopClassName = props.getProperty( STOP_CLASS_PROP );
        }
        catch ( Exception e )
        {
            log.error( "Failed while loading: " + layout.getBootstrapperConfigurationFile(), e );
            System.exit( ExitCodes.PROPLOAD );
        }
    }
    
    
    public void setParentLoader( ClassLoader parentLoader )
    {
        this.parent = parentLoader;
        URL[] jars = layout.getAllJars();
        this.application = new URLClassLoader( jars, parentLoader );
        
        if ( log.isDebugEnabled() )
        {
            StringBuffer buf = new StringBuffer();
            buf.append( "Dependencies loaded by the application ClassLoader: \n" );
            for ( int ii = 0; ii < jars.length; ii++ )
            {
                buf.append( "\t" ).append( jars[ii] ).append( "\n" );
            }
            log.debug( buf.toString() );
        }
    }


    public void callInit( String[] args )
    {
        Thread.currentThread().setContextClassLoader( application );
        try
        {
            startClass = application.loadClass( startClassName );
        }
        catch ( ClassNotFoundException e )
        {
            log.error( "Could not find " + startClassName, e );
            System.exit( ExitCodes.CLASS_LOOKUP );
        }
        
        try
        {
            start = ( DaemonApplication ) startClass.newInstance();
        }
        catch ( Exception e )
        {
            log.error( "Could not instantiate " + startClassName, e );
            System.exit( ExitCodes.INSTANTIATION );
        }
        
        try
        {
            start.init( this.layout, args );
        }
        catch ( Exception e )
        {
            log.error( "Failed on " + startClassName + ".init(InstallationLayout, String[])", e );
            System.exit( ExitCodes.INITIALIZATION );
        }
        Thread.currentThread().setContextClassLoader( parent );
    }

    
    public void callStart( boolean nowait )
    {
        Thread.currentThread().setContextClassLoader( application );
        try
        {
            start.start( nowait );
        }
        catch ( Exception e )
        {
            log.error( "Failed on " + startClass.getName() + ".start()", e );
            System.exit( ExitCodes.START );
        }
        Thread.currentThread().setContextClassLoader( parent );
    }
    

    public void callStop( String[] args )
    {
        Thread.currentThread().setContextClassLoader( application );
        Class clazz = null;
        
        if ( startClassName.equals( stopClassName ) && start != null )
        {
            clazz = startClass;
            stop = start;
        }
        else
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
                stop = ( DaemonApplication ) clazz.newInstance();
            }
            catch ( Exception e )
            {
                log.error( "Could not instantiate " + stopClassName, e );
                System.exit( ExitCodes.INSTANTIATION );
            }
        }
        
        try
        {
            stop.stop( args );
        }
        catch ( Exception e )
        {
            log.error( "Failed on " + stopClassName + ".stop()", e );
            System.exit( ExitCodes.STOP );
        }
        Thread.currentThread().setContextClassLoader( parent );
    }

    
    public void callDestroy()
    {
        Thread.currentThread().setContextClassLoader( application );
        try
        {
            stop.destroy();
        }
        catch ( Exception e )
        {
            log.error( "Failed on " + stopClassName + ".destroy()", e );
            System.exit( ExitCodes.STOP );
        }
        Thread.currentThread().setContextClassLoader( parent );
    }
    
    
    public static String[] shift( String[]args, int amount )
    {
        if ( args.length > amount )
        {
            String[] shifted = new String[args.length-1];
            System.arraycopy( args, 1, shifted, 0, shifted.length );
            return shifted;
        }
        
        return EMPTY_STRARRAY;
    }
}