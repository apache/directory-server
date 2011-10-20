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
public class ApacheDsTanukiWrapper implements WrapperListener
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( ApacheDsTanukiWrapper.class );

    /** The ApacheDS service*/
    private ApacheDsService service;


    private ApacheDsTanukiWrapper()
    {
    }


    public static void main( String[] args )
    {
        WrapperManager.start( new ApacheDsTanukiWrapper(), args );
    }


    public Integer start( String[] args )
    {
        log.info( "Starting the service..." );

        if ( ( args != null ) && ( args.length == 1 ) )
        {
            // Creating ApacheDS service
            service = new ApacheDsService();

            // Creating instance layouts from the argument
            InstanceLayout instanceLayout = new InstanceLayout( args[0] );

            // Starting the service
            try
            {
                service.start( instanceLayout );
            }
            catch ( Exception e )
            {
                log.error( "Failed to start the service.", e );
                System.exit( ExitCodes.START );
            }
        }
        else
        {
            throw new IllegalArgumentException(
                "Program must be launched with 1 arguement, the path to the instance directory." );
        }

        return null;
    }


    public int stop( int exitCode )
    {
        log.info( "Attempting graceful shutdown of the service..." );

        // Stopping the service
        try
        {
            service.stop();
        }
        catch ( Exception e )
        {
            log.error( "Failed to stop the service.", e );
            System.exit( ExitCodes.STOP );
        }

        log.info( "Completed graceful shutdown of the service..." );

        return exitCode;
    }


    public void controlEvent( int event )
    {
        if ( WrapperManager.isControlledByNativeWrapper() )
        {
            // The Wrapper will take care of this event
        }
        else
        {
            // We are not being controlled by the Wrapper, so
            // handle the event ourselves.
            if ( ( event == WrapperManager.WRAPPER_CTRL_C_EVENT ) ||
                 ( event == WrapperManager.WRAPPER_CTRL_CLOSE_EVENT ) ||
                 ( event == WrapperManager.WRAPPER_CTRL_SHUTDOWN_EVENT ) )
            {
                WrapperManager.stop( 0 );
            }
        }
    }
}
