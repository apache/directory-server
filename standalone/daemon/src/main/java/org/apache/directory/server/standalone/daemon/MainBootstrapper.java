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


import org.slf4j.LoggerFactory;
import org.slf4j.Logger;


/**
 * The main() based application bootstrapper used as the entry point for the 
 * executable bootstrapper.jar so it can be launched as a simple java application.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class MainBootstrapper extends Bootstrapper
{
    private static final Logger log = LoggerFactory.getLogger( MainBootstrapper.class );


    // ------------------------------------------------------------------------
    // Java application main() entry point
    // ------------------------------------------------------------------------

    
    public static void main( String[] args )
    {
        log.debug( "main(String[]) called" );
        
        if ( log.isDebugEnabled() )
        {
            log.debug( "main() recieved args:" );
            for ( int ii = 0; ii < args.length; ii++ )
            {
                log.debug( "args[" + ii + "] = " + args[ii] );
            }
        }

        MainBootstrapper instance = new MainBootstrapper();
        if ( args.length > 1 )
        {
            log.debug( "main(String[]) initializing Bootstrapper ... )" );
            instance.setInstallationLayout( args[0] );
            instance.setParentLoader( Bootstrapper.class.getClassLoader() );
            log.debug( "Bootstrapper initialized" );
        }
        else
        {
            String msg = "Server exiting without required installation.home or command.name.";
            System.err.println( msg );
            log.error( msg );
            printHelp();
            System.exit( 1 );
        }

        String command = args[args.length - 1];
        try
        {
            if ( command.equalsIgnoreCase( "start" ) )
            {
                log.debug( "calling callInit(String[]) from main(String[])" );
                instance.callInit();

                log.debug( "calling callStart() from main(String[])" );
                instance.callStart();
            }
            else if ( command.equalsIgnoreCase( "stop" ) )
            {
                log.debug( "calling callStop() from main(String[])" );
                instance.callStop();
                log.debug( "calling callDestroy() from main(String[])" );
                instance.callDestroy();
            }
            else
            {
                log.error( "Unrecognized command " + command );
                printHelp();
                System.exit( 3 );
            }
        }
        catch ( Throwable t )
        {
            log.error( "Encountered error while processing command: " + command );
            t.printStackTrace();
            System.exit( ExitCodes.UNKNOWN );
        }
    }


    private static void printHelp()
    {
        System.err.println("java -jar bootstrap.jar <app.home> <command.name>");
    }
}
