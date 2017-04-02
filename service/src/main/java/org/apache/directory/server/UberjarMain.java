/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.directory.server;


import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import org.apache.directory.api.util.Network;
import org.apache.directory.server.core.api.InstanceLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The command line main for the server.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class UberjarMain
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( UberjarMain.class );
    
    /** The key of the property use to specify the shutdown port */
    private static final String PROPERTY_SHUTDOWN_PORT = "apacheds.shutdown.port";

    /** The ApacheDS service */
    private ApacheDsService service;

    /**
     * Takes a single argument, the path to the installation home, which
     * contains the configuration to load with server startup settings.
     *
     * @param args
     *            the arguments
     */
    public static void main( String[] args ) throws Exception
    {
        if ( ( args == null ) || ( args.length < 1 ) )
        {
            throw new IllegalArgumentException( "Instance directory argument is missing" );
        }

        String instanceDirectory = args[0];
        Action action = ( args.length == 2 ) ? Action.fromString( args[1] ) : Action.START;

        UberjarMain instance = new UberjarMain();
        
        switch ( action )
        {
            case START :
                // Starts the server
                LOG.debug( "Starting runtime" );
                instance.start( instanceDirectory );

                break;

            case STOP :
                // Stops the server
                LOG.debug( "Stopping runtime" );
                InstanceLayout layout = new InstanceLayout( instanceDirectory );
                try ( Socket socket = new Socket( Network.LOOPBACK, readShutdownPort( layout ) );
                        PrintWriter writer = new PrintWriter( socket.getOutputStream() ) )
                {
                    writer.print( readShutdownPassword( layout ) );
                }
                
                break;
                
            case REPAIR :
                // Try to fix the JDBM database
                LOG.debug( "Fixing the database runtime" );
                instance.repair( instanceDirectory );
                
                break;

            default:
                throw new IllegalArgumentException( "Unexpected action " + action );
        }

        LOG.trace( "Exiting main" );
    }

    
    private int getShutdownPort()
    {
        int shutdownPort = Integer.parseInt( System.getProperty( PROPERTY_SHUTDOWN_PORT, "0" ) );
        if ( shutdownPort < 0 || ( shutdownPort > 0 && shutdownPort < 1024 ) || shutdownPort > 65536 )
        {
            throw new IllegalArgumentException( "Shutdown port [" + shutdownPort + "] is an illegal port number" );
        }
        return shutdownPort;
    }

    
    private static int readShutdownPort( InstanceLayout layout ) throws IOException 
    {
        return Integer.parseInt( new String( Files.readAllBytes( 
                Paths.get( layout.getRunDirectory().getAbsolutePath(), ".shutdown.port" ) ),
                Charset.forName( "utf-8" ) ) );
    }
    

    private static String readShutdownPassword( InstanceLayout layout ) throws IOException 
    {
        return new String( Files.readAllBytes( 
                Paths.get( layout.getRunDirectory().getAbsolutePath(), ".shutdown.pwd" ) ),
                Charset.forName( "utf-8" ) );
    }

    
    /**
     * Try to start the databases
     *
     * @param instanceDirectory The directory containing the server instance 
     */
    public void start( String instanceDirectory )
    {
        InstanceLayout layout = new InstanceLayout( instanceDirectory );

        // Creating ApacheDS service
        service = new ApacheDsService();

        // Initializing the service
        try
        {
            LOG.info( "Starting the service." );
            service.start( layout );

            startShutdownListener( layout );
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to start the service.", e );
            stop();
            System.exit( 1 );
        }
    }

    
    /**
     * Try to repair the databases
     *
     * @param instanceDirectory The directory containing the server instance 
     */
    public void repair( String instanceDirectory )
    {
        System.out.println( "Trying to repair the following data :" + instanceDirectory );
        InstanceLayout layout = new InstanceLayout( instanceDirectory );

        // Creating ApacheDS service
        service = new ApacheDsService();

        // Initializing the service
        try
        {
            System.out.println( "Starting the service." );
            // must start servers otherwise stop() won't work
            service.start( layout, true );
            // no need to start the shutdown listener
            System.out.println( "Service started." );
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to start the service.", e );
            stop();
            System.exit( 1 );
        }

        // Repairing the database
        try
        {
            System.out.println( "Repairing the database." );
            service.repair( layout );
            System.out.println( "Database repaired." );
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to repair the database.", e );
            stop();
            System.exit( 1 );
        }

        // Stop the service
        stop();
    }


    public void stop()
    {
        if ( service != null )
        {
            try
            {
                LOG.info( "Stopping the service." );
                service.stop();
                LOG.info( "Service stopped successfully." );
            }
            catch ( Exception e )
            {
                LOG.error( "Failed to start the service.", e );
                System.exit( 1 );
            }
        }
    }
    
    
    /**
     * Starts a thread that creates a ServerSocket which listens for shutdown command.
     *
     * @param layout the InstanceLayout
     * @throws IOException
     */
    private void startShutdownListener( final InstanceLayout layout ) throws IOException
    {
        final int shutdownPort = getShutdownPort();
        final String shutdownPassword = writeShutdownPassword( layout, UUID.randomUUID().toString() );
        
        new Thread( new Runnable()
        {
            @Override
            public void run()
            {
                // bind to localhost only to prevent connections from outside the box
                try ( ServerSocket shutdownSocket = new ServerSocket( shutdownPort, 1, Network.LOOPBACK ) )
                {
                    writeShutdownPort( layout, shutdownSocket.getLocalPort() );
                    
                    LOG.info( "Start the shutdown listener on port {}", shutdownSocket.getLocalPort() );
                    
                    Socket socket;
                    while ( ( socket = shutdownSocket.accept() ) != null )
                    {
                        if ( shutdownPassword == null || shutdownPassword.isEmpty() ) 
                        {
                            stop();
                            break;
                        }
                        else
                        {
                            try
                            {
                                InputStreamReader reader = new InputStreamReader( socket.getInputStream() );
                                
                                CharBuffer buffer = CharBuffer.allocate( 2048 );
                                while ( reader.read( buffer ) >= 0 )
                                {
                                    // read till end of stream
                                }
                                buffer.flip();
                                String password = buffer.toString();
                                
                                reader.close();
                                
                                if ( shutdownPassword.equals( password ) )
                                {
                                    stop();
                                    break;
                                }
                                else
                                {
                                    LOG.warn( "Illegal attempt to shutdown, incorrect password {}", password );
                                }
                            }
                            catch ( IOException e )
                            {
                                LOG.warn( "Failed to handle the shutdown request", e );
                            }
                        }
                    }
                }
                catch ( IOException e )
                {
                    LOG.error( "Failed to start the shutdown listener, stopping the server", e );
                    stop();
                }
                
            }
        } ).start();
    }
    
    
    private static String writeShutdownPassword( InstanceLayout layout, String password ) throws IOException 
    {
        Files.write(
                Paths.get( layout.getRunDirectory().getAbsolutePath(), ".shutdown.pwd" ),
                password.getBytes( Charset.forName( "utf-8" ) ) );
        return password;
    }
    
    
    private static int writeShutdownPort( InstanceLayout layout, int portNumber ) throws IOException 
    {
        Files.write(
                Paths.get( layout.getRunDirectory().getAbsolutePath(), ".shutdown.port" ),
                Integer.toString( portNumber ).getBytes( Charset.forName( "utf-8" ) ) );
        return portNumber;
    }

    
    private enum Action
    {
        START, STOP, REPAIR;

        public static Action fromString( String actionString )
        {
            for ( Action action : values() )
            {
                if ( action.name().equalsIgnoreCase( actionString ) )
                {
                    return action;
                }
            }
            
            throw new IllegalArgumentException( "Unknown action " + actionString );
        }
    }
}
