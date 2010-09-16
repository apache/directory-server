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

package org.apache.directory.server;


import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tanukisoftware.wrapper.WrapperListener;
import org.tanukisoftware.wrapper.WrapperManager;


/**
 * The bootstrapper used by Tanuki Wrapper.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ApacheDsTanukiWrapper implements WrapperListener
{
    private static final Logger log = LoggerFactory.getLogger( ApacheDsTanukiWrapper.class );

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
        service = new ApacheDsService();
        InstallationLayout installationLayout = new InstallationLayout( ( File ) null ); // TODO
        InstanceLayout instanceLayout = new InstanceLayout( ( File ) null ); // TODO
        try
        {
            service.init( installationLayout, instanceLayout );
        }
        catch ( Exception e )
        {
            log.error( "Failed to start", e );
            System.exit( ExitCodes.START );
        }
        service.start();
        return null;
    }


    public int stop( int exitCode )
    {
        log.info( "Attempting graceful shutdown of this server instance" );

        try
        {
            service.stop();
        }
        catch ( Exception e )
        {
            log.error( "Failed to stop", e );
            System.exit( ExitCodes.STOP );
        }

        log.info( "Completed graceful shutdown..." );

        return exitCode;
    }


    public void controlEvent( int event )
    {
        log.error( "Recvd Event: " + event );
        if ( WrapperManager.isControlledByNativeWrapper() )
        {
            // The Wrapper will take care of this event
        }
        else
        {
            // We are not being controlled by the Wrapper, so
            //  handle the event ourselves.
            if ( ( event == WrapperManager.WRAPPER_CTRL_C_EVENT ) ||
                 ( event == WrapperManager.WRAPPER_CTRL_CLOSE_EVENT ) ||
                 ( event == WrapperManager.WRAPPER_CTRL_SHUTDOWN_EVENT ) )
            {
                WrapperManager.stop( 0 );
            }
        }
    }
}
