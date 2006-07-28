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
package org.apache.directory.server.tools.commands.importcmd;


import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.directory.daemon.AvailablePortFinder;
import org.apache.directory.server.tools.execution.ToolCommandExecutorStub;
import org.apache.directory.server.tools.request.BaseToolCommandCL;
import org.apache.directory.server.tools.util.Parameter;


/**
 * A command to import data into a server. The data to be imported must be
 * stored in a Ldif File, and they could be added entries or modified entries.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 406112 $
 */
public class ImportCommandCL extends BaseToolCommandCL
{

    private File ldifFile;

    private boolean ignoreErrors = false;


    /**
     * The constructor save the command's name into it's super class
     */
    public ImportCommandCL()
    {
        super( "import" );
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
        op = new Option( "u", "user", true, "the user: default to uid=admin, ou=system" );
        op.setRequired( false );
        opts.addOption( op );
        op = new Option( "w", "password", true, "the apacheds administrator's password: defaults to secret" );
        op.setRequired( false );
        opts.addOption( op );
        op = new Option( "a", "auth", true, "the authentication mode: defaults to 'simple'" );
        op.setRequired( false );
        opts.addOption( op );
        op = new Option( "f", "file", true, "the ldif file to import" );
        op.setRequired( true );
        opts.addOption( op );
        op = new Option( "e", "ignore", false, "continue to process the file even if errors are encountered " );
        op.setRequired( false );
        opts.addOption( op );

        return opts;
    }


    /**
     * Read the command line and get the options : 'h' : host 'p' : port 'u' :
     * user 'w' : password 'a' : authentication type 'e' : ignore errors 'f' :
     * ldif file to import
     * 
     * @param cmd
     *            The command line
     */
    public void processOptions( CommandLine cmd )
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

            parameters.add( new Parameter( ImportCommandExecutor.HOST_PARAMETER, host ) );
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

            parameters.add( new Parameter( ImportCommandExecutor.PORT_PARAMETER, new Integer( port ) ) );
        }

        // -------------------------------------------------------------------
        // figure out the user value
        // -------------------------------------------------------------------
        if ( cmd.hasOption( 'u' ) )
        {
            user = cmd.getOptionValue( 'u' );

            if ( isDebugEnabled() )
            {
                System.out.println( "user overriden by -u option: " + user );
            }

            parameters.add( new Parameter( ImportCommandExecutor.USER_PARAMETER, user ) );
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

            parameters.add( new Parameter( ImportCommandExecutor.PASSWORD_PARAMETER, password ) );
        }

        // -------------------------------------------------------------------
        // figure out the authentication type
        // -------------------------------------------------------------------
        if ( cmd.hasOption( 'a' ) )
        {
            auth = cmd.getOptionValue( 'a' );

            if ( isDebugEnabled() )
            {
                System.out.println( "authentication type overriden by -a option: " + auth );
            }

            parameters.add( new Parameter( ImportCommandExecutor.AUTH_PARAMETER, auth ) );
        }

        // -------------------------------------------------------------------
        // figure out the 'ignore-errors' flag
        // -------------------------------------------------------------------
        if ( cmd.hasOption( 'e' ) )
        {
            ignoreErrors = true;

            if ( isDebugEnabled() )
            {
                System.out.println( "ignore-errors overriden by -e option: true" );
            }

            parameters.add( new Parameter( ImportCommandExecutor.IGNOREERRORS_PARAMETER, new Boolean( ignoreErrors ) ) );
        }

        // -------------------------------------------------------------------
        // figure out the ldif file to import
        // -------------------------------------------------------------------
        if ( cmd.hasOption( 'f' ) )
        {
            String ldifFileName = cmd.getOptionValue( 'f' );

            ldifFile = new File( ldifFileName );

            if ( ldifFile.exists() == false )
            {
                System.err.println( "ldif file '" + ldifFileName + "' does not exist" );
                System.exit( 1 );
            }

            if ( ldifFile.canRead() == false )
            {
                System.err.println( "ldif file '" + ldifFileName + "' can't be read" );
                System.exit( 1 );
            }

            if ( isDebugEnabled() )
            {
                try
                {
                    System.out.println( "ldif file to import: " + ldifFile.getCanonicalPath() );
                }
                catch ( IOException ioe )
                {
                    System.out.println( "ldif file to import: " + ldifFileName );
                }
            }
        }
        else
        {
            System.err.println( "ldif file name must be provided" );
            System.exit( 1 );
        }

        parameters.add( new Parameter( ImportCommandExecutor.FILE_PARAMETER, ldifFile ) );

        // -------------------------------------------------------------------
        // figure out the 'install-path'
        // and verify if the -i option is present when the -c option is used
        // -------------------------------------------------------------------
        if ( cmd.hasOption( 'i' ) )
        {
            parameters.add( new Parameter( ImportCommandExecutor.INSTALLPATH_PARAMETER, cmd.getOptionValue( 'i' ) ) );
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
            parameters.add( new Parameter( ImportCommandExecutor.CONFIGURATION_PARAMETER, new Boolean( true ) ) );
        }

        // -------------------------------------------------------------------
        // Transform other options into params
        // -------------------------------------------------------------------
        parameters.add( new Parameter( ImportCommandExecutor.DEBUG_PARAMETER, new Boolean( isDebugEnabled() ) ) );
        parameters.add( new Parameter( ImportCommandExecutor.QUIET_PARAMETER, new Boolean( isQuietEnabled() ) ) );
        parameters.add( new Parameter( ImportCommandExecutor.VERBOSE_PARAMETER, new Boolean( isVerboseEnabled() ) ) );
    }


    public ToolCommandExecutorStub getStub()
    {
        return new ImportCommandExecutorStub();
    }

}
