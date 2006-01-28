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


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessControlException;
import java.util.Properties;
import java.util.Random;

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

    /** Shutdown command to use for await() */
    private static final String SHUTDOWN = "SHUTDOWN";

    private static final String SHUTDOWN_FILE = "shutdownPort";
    /** The Port to Listen on for Shutdown commands */
    private int shutdownPort = -1;
    /** Random number generator */
    private Random random;
    
    
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

    
    public void sendShutdownCommand() throws IOException
    {
        Socket socket = null;
        OutputStream stream = null;
        
        if ( shutdownPort == -1 )
        {
            File shutdownPortFile = new File( layout.getRunDirectory(), SHUTDOWN_FILE );
            if ( shutdownPortFile.exists() )
            {
                BufferedReader in = new BufferedReader( new FileReader( shutdownPortFile ) );
                shutdownPort = Integer.parseInt( in.readLine() );
                in.close();
            }
        }
        
        // this stops the main thread listening for shutdown requests
        try {
            socket = new Socket( "127.0.0.1", shutdownPort );
            stream = socket.getOutputStream();

            for (int i = 0; i < SHUTDOWN.length(); i++ )
            {
                stream.write( SHUTDOWN.charAt(i) );
            }

            stream.flush();
        }
        finally
        {
            if ( stream != null ) stream.close();
            if ( socket != null ) socket.close();
        }
    }
    

    /**
     * Wait until a proper shutdown command is received, then return.
     */
    public void waitForShutdown()
    {
        try
        {
            shutdownPort = AvailablePortFinder.getNextAvailable( 30003 );
            File shutdownPortFile = new File( layout.getRunDirectory(), SHUTDOWN_FILE );
            if ( shutdownPortFile.exists() )
            {
                String msg = "Shutdown port file " + shutdownPortFile + " exists. ";
                msg += "\nEither the server is already running or a previous run existed abruptly.";
                msg += "\nIf the server is not running remove this file and try again.";
                log.error( msg );
                throw new IllegalStateException( msg );
            }
            FileWriter writer = new FileWriter( shutdownPortFile );
            writer.write( shutdownPort + "\n" );
            writer.close();
        }
        catch ( IOException e )
        {
            log.error( "Failed to setup shutdown port", e );
            System.exit( ExitCodes.START );
        }

        // Set up a server socket to wait on
        ServerSocket serverSocket = null;
        try
        {
            serverSocket = new ServerSocket( shutdownPort, 1, InetAddress.getByName( "127.0.0.1" ) );
            log.debug( "waiting for shutdown command on port = " + shutdownPort );
        }
        catch ( IOException e )
        {
            log.error( "server waitForShutdown: create[" + shutdownPort + "]: ", e );
            System.exit( 1 );
        }

        // Loop waiting for a connection and a valid command
        while ( true )
        {
            // Wait for the next connection
            Socket socket = null;
            InputStream stream = null;

            try
            {
                socket = serverSocket.accept();
                socket.setSoTimeout(10 * 1000);  // Ten seconds
                stream = socket.getInputStream();
            }
            catch ( AccessControlException ace )
            {
                log.warn( "Standard Server.accept security exception: " + ace.getMessage(), ace );
                continue;
            }
            catch ( IOException e )
            {
                log.error( "Server.await: accept: ", e );
                System.exit( 5 );
            }

            // Read a set of characters from the socket
            StringBuffer command = new StringBuffer();
            int expected = 1024; // Cut off to avoid DoS attack

            while ( expected < SHUTDOWN.length() )
            {
                if ( random == null )
                {
                    random = new Random(System.currentTimeMillis());
                }
                expected += ( random.nextInt() % 1024 );
            }
            while ( expected > 0 )
            {
                int ch;
                try
                {
                    ch = stream.read();
                }
                catch ( IOException e )
                {
                    log.warn( "StandardServer.await: read: ", e );
                    ch = -1;
                }

                if ( ch < 32 )  // Control character or EOF terminates loop
                {
                    break;
                }

                command.append( ( char ) ch );
                expected--;
            }

            // Close the socket now that we are done with it
            try
            {
                socket.close();
            }
            catch ( IOException e )
            {
                log.debug( "Failed on socket close", e );
            }

            // Match against our command string
            boolean match = command.toString().equals( SHUTDOWN );
            if ( match )
            {
                break;
            }
            else
            {
                log.warn( "Server.await: Invalid command '" + command.toString() + "' received" );
            }
        }

        // Close the server socket and return
        try
        {
            serverSocket.close();
        }
        catch ( IOException e )
        {
            log.debug( "Failed on socket close", e );
        }
        
        File shutdownPortFile = new File( layout.getRunDirectory(), SHUTDOWN_FILE );
        if ( shutdownPortFile.exists() )
        {
            shutdownPortFile.delete();
        }
    }
}