/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.apache.directory.server.wrapper;


import org.apache.directory.api.util.Strings;
import org.apache.directory.server.ApacheDsService;
import org.apache.directory.server.core.api.InstanceLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tanukisoftware.wrapper.WrapperListener;
import org.tanukisoftware.wrapper.WrapperManager;


/**
 * A Tanuki Wrapper implementation for the ApacheDS service.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class ApacheDsTanukiWrapper implements WrapperListener
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( ApacheDsTanukiWrapper.class );

    /** The ApacheDS service*/
    private ApacheDsService service;


    /**
     * Creates a new instance of ApacheDsTanukiWrapper.
     */
    private ApacheDsTanukiWrapper()
    {
    }


    public static void main( String[] args )
    {
        WrapperManager.start( new ApacheDsTanukiWrapper(), args );
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
            System.out.println( "Service started." );
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to start the service.", e );
            stop( 1 );
            System.exit( ExitCodes.START );
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
            stop( 1 );
            System.exit( ExitCodes.START );
        }

        // Stop the service
        stop( 0 );
    }


    /**
     * Implemented the start() method from the WrapperListener class.
     * 
     * The possible arguments are the instance layout directory and the command, one of :
     * <ul>
     *   <li>START (default) : starts the server</li>
     *   <li>STOP : stops the server</li>
     *   <li>REPAIR : repairs the index</li>
     * </ul>
     */
    public Integer start( String[] args )
    {
        LOG.info( "Starting the service..." );

        if ( args != null )
        {
            int argNb = 0;
            
            for ( String arg : args )
            {
                LOG.info( "Args[{}] : {}", argNb, arg );
                argNb++;
            }
        }

        if ( args != null )
        {
            // the default action
            String action = "START";
            String instanceDirectory = args[0];

            switch ( args.length )
            {
                case 2 :
                    action = args[1];
                    /* Passthrough...*/
                    
                case 1 :
                    // Creating ApacheDS service
                    service = new ApacheDsService();

                    // Creating instance layouts from the argument
                    InstanceLayout instanceLayout = new InstanceLayout( instanceDirectory );
                    
                    // Process the action
                    switch ( Strings.toLowerCaseAscii( action ) )
                    {
                        case "stop":
                            // Stops the server
                            LOG.debug( "Stopping runtime" );
                            stop( 1 );

                            break;

                        case "repair":
                            // Try to fix the JDBM database
                            LOG.debug( "Fixing the database runtime" );
                            repair( instanceDirectory );

                            break;

                        default:
                            // Starts the server
                            LOG.debug( "Starting runtime" );

                            try
                            {
                                service.start( instanceLayout );
                            }
                            catch ( Exception e )
                            {
                                LOG.error( "Failed to start the service.", e );
                                System.exit( ExitCodes.START );
                            }

                            break;
                    }
                    
                    break;
                    
                default :
                    throw new IllegalArgumentException(
                        "Program must be launched with at least 1 argument, the path to the instance directory." );
            }
        }

        return null;
    }


    public int stop( int exitCode )
    {
        LOG.info( "Attempting graceful shutdown of the service..." );

        // Stopping the service
        try
        {
            service.stop();
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to stop the service.", e );
            System.exit( ExitCodes.STOP );
        }

        LOG.info( "Completed graceful shutdown of the service..." );

        return exitCode;
    }


    public void controlEvent( int event )
    {
        if ( !WrapperManager.isControlledByNativeWrapper() )
        {
            // We are not being controlled by the Wrapper, so
            // handle the event ourselves.
            if ( ( event == WrapperManager.WRAPPER_CTRL_C_EVENT )
                || ( event == WrapperManager.WRAPPER_CTRL_CLOSE_EVENT )
                || ( event == WrapperManager.WRAPPER_CTRL_SHUTDOWN_EVENT ) )
            {
                WrapperManager.stop( 0 );
            }
        }
    }
}
