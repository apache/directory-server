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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


import org.apache.directory.server.core.api.InstanceLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The command line main for the server.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory
 *         Project</a>
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
        if ( (args == null) || (args.length < 1) )
        {
            throw new IllegalArgumentException(
                    "Instance directory argument is missing" );
        }

        final String instanceDirectory = args[0];
        Action action = (args.length == 2) ? Action.fromString( args[1] ) : Action.START;

        final UberjarMain instance = new UberjarMain();
        final int shutdownPort = getShutdownPort();
        switch ( action )
        {
            case START:
                LOG.debug( "Staring runtime" );
                final String shutdownPassword = writeShutdownPassword( instanceDirectory,
                        UUID.randomUUID().toString() );
                try {
                    new Thread( new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try (ServerSocket shutdownSocket = new ServerSocket( shutdownPort ))
                            {
                                writeShutdownPort( instanceDirectory, shutdownSocket.getLocalPort() );

                                Socket socket;
                                LOG.info( "Start the shutdown listener on port [{}]", shutdownPort );
                                while ( (socket = shutdownSocket.accept()) != null )
                                {
                                    if ( shutdownPassword == null || shutdownPassword.isEmpty() ) {
                                        instance.stop();
                                        break;
                                    }
                                    else
                                    {
                                        try (InputStreamReader reader = new InputStreamReader( socket.getInputStream() ))
                                        {
                                            CharBuffer buffer = CharBuffer.allocate( 2048 );
                                            while ( reader.read( buffer ) >= 0 );
                                            buffer.flip();
                                            String password = buffer.toString();
                                            if ( shutdownPassword.equals( password ) )
                                            {
                                                instance.stop();
                                                break;
                                            }
                                            else
                                            {
                                                LOG.warn( "Illegal attempt to shutdown, incorrect password [{}]", password );
                                            }
                                        }
                                    }
                                }
                            }
                            catch ( IOException e )
                            {
                                e.printStackTrace();
                                LOG.error( "Failed to start the shutdown listener.", e );
                            }

                        }
                    } ).start();
                }
                catch ( Exception e ) {
                    e.printStackTrace();
                    LOG.error( "Failed to start the service.", e );
                    System.exit( 1 );
                }
                instance.start( instanceDirectory );
                break;
            case STOP:
                LOG.debug( "Stopping runtime" );
                try (Socket socket = new Socket( "localhost", readShutdownPort( instanceDirectory ) );
                        PrintWriter writer = new PrintWriter( socket.getOutputStream() ))
                {
                    writer.print( readShutdownPassword( instanceDirectory ) );
                }
                break;
        }
    }

    private static int getShutdownPort()
    {
        int shutdownPort = Integer.parseInt( System.getProperty( PROPERTY_SHUTDOWN_PORT, "0" ) );
        if ( shutdownPort < 1024 || shutdownPort > 65536 )
        {
            throw new IllegalArgumentException( "Shutdown port [" + shutdownPort + "] is an illegal port number" );
        }
        return shutdownPort;
    }

    private static int readShutdownPort( String instanceDirectory ) throws IOException 
    {
        return Integer.parseInt( new String( Files.readAllBytes( 
                Paths.get( new InstanceLayout( instanceDirectory ).getRunDirectory().getAbsolutePath(), 
                        ".shutdown.port" ) ),
                Charset.forName( "utf-8" ) ) );
    }

    private static String readShutdownPassword( String instanceDirectory ) throws IOException 
    {
        return new String( Files.readAllBytes( 
                Paths.get( new InstanceLayout( instanceDirectory ).getRunDirectory().getAbsolutePath(),
                        ".shutdown.pwd" ) ),
                Charset.forName( "utf-8" ) );
    }

    public void start( String... args )
    {
        // Creating ApacheDS service
        service = new ApacheDsService();

        // Creating instance layouts from the argument
        InstanceLayout instanceLayout = new InstanceLayout( args[0] );

        // Initializing the service
        try
        {
            LOG.info( "Starting the service." );
            service.start( instanceLayout );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            LOG.error( "Failed to start the service.", e );
            System.exit( 1 );
        }
    }

    public void stop()
    {
        if ( service != null )
        {
            try
            {
                LOG.info( "Stopping the service." );
                service.stop();
            }
            catch ( Exception e )
            {
                e.printStackTrace();
                LOG.error( "Failed to start the service.", e );
                System.exit( 1 );
            }
        }
    }
    
    private static String writeShutdownPassword( String instanceDirectory, String password ) throws IOException 
    {
        Files.write(
                Paths.get( new InstanceLayout( instanceDirectory ).getRunDirectory().getAbsolutePath(), 
                        ".shutdown.pwd" ),
                password.getBytes( Charset.forName( "utf-8" ) ) );
        return password;
    }
    
    private static int writeShutdownPort( String instanceDirectory, int portNumber ) throws IOException 
    {
        Files.write(
                Paths.get( new InstanceLayout( instanceDirectory ).getRunDirectory().getAbsolutePath(), 
                        ".shutdown.port" ),
                Integer.toString( portNumber ).getBytes( Charset.forName( "utf-8" ) ) );
        return portNumber;
    }

    private static enum Action
    {
        START, STOP;

        private static Map<String, Action> lookup;

        static
        {
            lookup = new HashMap<String, Action>();
            for ( Action action : values() )
            {
                lookup.put( action.name(), action );
            }
        }

        public static Action fromString( String actionString )
        {
            return lookup.get( actionString.toUpperCase() );
        }
    }
}
