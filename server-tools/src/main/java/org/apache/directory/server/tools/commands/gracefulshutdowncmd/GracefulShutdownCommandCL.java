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
package org.apache.directory.server.tools.commands.gracefulshutdowncmd;


import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.directory.daemon.AvailablePortFinder;
import org.apache.directory.server.tools.execution.ToolCommandExecutorStub;
import org.apache.directory.server.tools.request.BaseToolCommandCL;
import org.apache.directory.server.tools.util.Parameter;

/**
 * A command used to send a graceful disconnect to established clients 
 * while allowing them time to complete operations already in progress.
 * 
 * @see <a href="http://docs.safehaus.org/display/APACHEDS/LDAP+Extensions+for+Graceful+Shutdown">
 * Graceful Shutdown</a>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 380697 $
 */
public class GracefulShutdownCommandCL extends BaseToolCommandCL
{
    private static final int DELAY_MAX = 86400;
    private int delay;

    private static final int TIME_OFFLINE_MAX = 720;
    private int timeOffline;


    public GracefulShutdownCommandCL()
    {
        super( "graceful" );
    }


    public Options getOptions()
    {
        Options opts = new Options();
        Option op = new Option( "h", "host", true, "server host: defaults to localhost" );
        op.setRequired( false );
        opts.addOption( op );
        op = new Option( "p", "port", true, "server port: defaults to 10389 or server.xml specified port" );
        op.setRequired( false );
        opts.addOption( op );
        op = new Option( "e", "delay", true, "delay (seconds) before shutdown: defaults to 0" );
        op.setRequired( false );
        opts.addOption( op );
        op = new Option( "w", "password", true, "the apacheds administrator's password: defaults to secret" );
        op.setRequired( false );
        opts.addOption( op );
        op = new Option( "t", "time-offline", true, "server offline time (minutes): defaults to 0 (indefinate)" );
        op.setRequired( false );
        opts.addOption( op );
        op = new Option( "i", "install-path", true, "path to apacheds installation directory" );
        op.setRequired( false );
        opts.addOption( op );
        return opts;
    }


    public ToolCommandExecutorStub getStub()
    {
        return new GracefulShutdownCommandExecutorStub();
    }


    public void processOptions( CommandLine cmd ) throws Exception
    {
        if ( isDebugEnabled() )
        {
            System.out.println( "Processing options for launching import ..." );
        }

        // -------------------------------------------------------------------
        // figure out the host value
        // -------------------------------------------------------------------
        if ( cmd.hasOption( 'h' ) )
        {
            host = cmd.getOptionValue( 'h' );

            if ( isDebugEnabled() )
            {
                System.out.println( "host overriden by -h option: true" );
            }

            parameters.add( new Parameter( GracefulShutdownCommandExecutor.HOST_PARAMETER, host ) );
        }

        // -------------------------------------------------------------------
        // figure out and error check the port value
        // -------------------------------------------------------------------
        if ( cmd.hasOption( 'p' ) ) // - user provided port w/ -p takes
        // precedence
        {
            String val = cmd.getOptionValue( 'p' );

            try
            {
                port = Integer.parseInt( val );
            }
            catch ( NumberFormatException e )
            {
                System.err.println( "port value of '" + val + "' is not a number" );
                System.exit( 1 );
            }

            if ( port > AvailablePortFinder.MAX_PORT_NUMBER )
            {
                System.err.println( "port value of '" + val + "' is larger than max port number: "
                    + AvailablePortFinder.MAX_PORT_NUMBER );
                System.exit( 1 );
            }
            else if ( port < AvailablePortFinder.MIN_PORT_NUMBER )
            {
                System.err.println( "port value of '" + val + "' is smaller than the minimum port number: "
                    + AvailablePortFinder.MIN_PORT_NUMBER );
                System.exit( 1 );
            }

            if ( isDebugEnabled() )
            {
                System.out.println( "port overriden by -p option: " + port );
            }

            parameters.add( new Parameter( GracefulShutdownCommandExecutor.PORT_PARAMETER, new Integer( port ) ) );
        }

        // -------------------------------------------------------------------
        // figure out the password value
        // -------------------------------------------------------------------
        if ( cmd.hasOption( 'w' ) )
        {
            password = cmd.getOptionValue( 'w' );

            if ( isDebugEnabled() )
            {
                System.out.println( "password overriden by -w option: " + password );
            }

            parameters.add( new Parameter( GracefulShutdownCommandExecutor.PASSWORD_PARAMETER, password ) );
        }

        // -------------------------------------------------------------------
        // figure out the delay value
        // -------------------------------------------------------------------
        if ( cmd.hasOption( 'e' ) )
        {
            String val = cmd.getOptionValue( 'e' );
            try
            {
                delay = Integer.parseInt( val );
            }
            catch ( NumberFormatException e )
            {
                System.err.println( "delay value of '" + val + "' is not a number" );
                System.exit( 1 );
            }

            if ( delay > DELAY_MAX )
            {
                System.err.println( "delay value of '" + val + "' is larger than max delay (seconds) allowed: "
                    + DELAY_MAX );
                System.exit( 1 );
            }
            else if ( delay < 0 )
            {
                System.err.println( "delay value of '" + val + "' is less than zero and makes no sense" );
                System.exit( 1 );
            }

            if ( isDebugEnabled() )
            {
                System.out.println( "delay seconds overriden by -e option: " + delay );
            }

            parameters.add( new Parameter( GracefulShutdownCommandExecutor.DELAY_PARAMETER, new Integer( delay ) ) );
        }

        // -------------------------------------------------------------------
        // figure out the timeOffline value
        // -------------------------------------------------------------------
        if ( cmd.hasOption( 't' ) )
        {
            String val = cmd.getOptionValue( 't' );
            try
            {
                timeOffline = Integer.parseInt( val );
            }
            catch ( NumberFormatException e )
            {
                System.err.println( "timeOffline value of '" + val + "' is not a number" );
                System.exit( 1 );
            }

            if ( timeOffline > TIME_OFFLINE_MAX )
            {
                System.err.println( "timeOffline value of '" + val
                    + "' is larger than max timeOffline (minutes) allowed: " + TIME_OFFLINE_MAX );
                System.exit( 1 );
            }
            else if ( timeOffline < 0 )
            {
                System.err.println( "timeOffline value of '" + val + "' is less than zero and makes no sense" );
                System.exit( 1 );
            }

            if ( isDebugEnabled() )
            {
                System.out.println( "timeOffline seconds overriden by -t option: " + timeOffline );
            }

            parameters.add( new Parameter( GracefulShutdownCommandExecutor.TIMEOFFLINE_PARAMETER, new Integer(
                timeOffline ) ) );
        }

        // -------------------------------------------------------------------
        // figure out the 'install-path'
        // and verify if the -i option is present when the -c option is used
        // -------------------------------------------------------------------
        if ( cmd.hasOption( 'i' ) )
        {
            parameters.add( new Parameter( GracefulShutdownCommandExecutor.INSTALLPATH_PARAMETER, cmd
                .getOptionValue( 'i' ) ) );
        }
        else if ( cmd.hasOption( 'c' ) )
        {
            System.err.println( "forced configuration load (-c) requires the -i option" );
            System.exit( 1 );
        }

        // -------------------------------------------------------------------
        // figure out the 'configuration' flag
        // -------------------------------------------------------------------
        if ( cmd.hasOption( 'c' ) )
        {
            parameters
                .add( new Parameter( GracefulShutdownCommandExecutor.CONFIGURATION_PARAMETER, new Boolean( true ) ) );
        }

        // -------------------------------------------------------------------
        // Transform other options into params
        // -------------------------------------------------------------------
        parameters
            .add( new Parameter( GracefulShutdownCommandExecutor.DEBUG_PARAMETER, new Boolean( isDebugEnabled() ) ) );
        parameters
            .add( new Parameter( GracefulShutdownCommandExecutor.QUIET_PARAMETER, new Boolean( isQuietEnabled() ) ) );
        parameters.add( new Parameter( GracefulShutdownCommandExecutor.VERBOSE_PARAMETER, new Boolean(
            isVerboseEnabled() ) ) );
    }

}
