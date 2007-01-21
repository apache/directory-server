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
package org.apache.directory.server.tools.commands.exportcmd;


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
 * A command to export data from a server.
 * The data is exported as a Ldif File.
 */
public class ExportCommandCL extends BaseToolCommandCL
{
    private String ldifFileName;
    private String baseDN;
    private String exportPoint;
    private String scope;
    private static final String SCOPE_OBJECT = "object";
    private static final String SCOPE_ONELEVEL = "onelevel";
    private static final String SCOPE_SUBTREE = "subtree";


    public ExportCommandCL()
    {
        super( "export" );
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
        op = new Option( "b", "base-dn", true, "the base DN: defaults to ''" );
        op.setRequired( false );
        opts.addOption( op );
        op = new Option( "e", "exportpoint", true, "the export point: defaults to ''" );
        op.setRequired( false );
        opts.addOption( op );
        op = new Option( "s", "scope", true, "the export scope ('" + SCOPE_OBJECT + "', '" + SCOPE_ONELEVEL + "' or '"
            + SCOPE_SUBTREE + "'): defaults to '" + SCOPE_SUBTREE + "'" );
        op.setRequired( false );
        opts.addOption( op );
        op = new Option( "f", "file", true, "the ldif file to export data to" );
        op.setRequired( true );
        opts.addOption( op );

        return opts;
    }


    public ToolCommandExecutorStub getStub()
    {
        return new ExportCommandExecutorStub();
    }


    public void processOptions( CommandLine cmd ) throws Exception
    {
        if ( isDebugEnabled() )
        {
            System.out.println( "Processing options for launching export ..." );
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

            parameters.add( new Parameter( ExportCommandExecutor.HOST_PARAMETER, host ) );
        }

        // -------------------------------------------------------------------
        // figure out and error check the port value
        // -------------------------------------------------------------------
        if ( cmd.hasOption( 'p' ) ) // - user provided port w/ -p takes precedence
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

            parameters.add( new Parameter( ExportCommandExecutor.PORT_PARAMETER, new Integer( port ) ) );
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

            parameters.add( new Parameter( ExportCommandExecutor.USER_PARAMETER, user ) );
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

            parameters.add( new Parameter( ExportCommandExecutor.PASSWORD_PARAMETER, password ) );
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

            parameters.add( new Parameter( ExportCommandExecutor.AUTH_PARAMETER, auth ) );
        }

        // -------------------------------------------------------------------
        // figure out the Base DN value
        // -------------------------------------------------------------------
        if ( cmd.hasOption( 'b' ) )
        {
            baseDN = cmd.getOptionValue( 'b' );

            if ( isDebugEnabled() )
            {
                System.out.println( "base DN overriden by -b option: " + baseDN );
            }

            parameters.add( new Parameter( ExportCommandExecutor.BASEDN_PARAMETER, baseDN ) );
        }

        // -------------------------------------------------------------------
        // figure out the Export Point value
        // -------------------------------------------------------------------
        if ( cmd.hasOption( 'e' ) )
        {
            exportPoint = cmd.getOptionValue( 'e' );

            if ( isDebugEnabled() )
            {
                System.out.println( "export point overriden by -e option: " + exportPoint );
            }

            parameters.add( new Parameter( ExportCommandExecutor.EXPORTPOINT_PARAMETER, exportPoint ) );
        }

        // -------------------------------------------------------------------
        // figure out the scope value
        // -------------------------------------------------------------------
        if ( cmd.hasOption( 's' ) )
        {
            scope = cmd.getOptionValue( 's' );

            if ( ( scope.equals( SCOPE_OBJECT ) ) || ( scope.equals( SCOPE_ONELEVEL ) )
                || ( scope.equals( SCOPE_SUBTREE ) ) )
            {
                if ( isDebugEnabled() )
                {
                    System.out.println( "scope overriden by -s option: " + scope );
                }
            }
            else
            {
                System.err.println( "unknown scope. Scope must be '" + SCOPE_OBJECT + "', '" + SCOPE_ONELEVEL
                    + "' or '" + SCOPE_SUBTREE + "'" );
                System.exit( 1 );
            }

            parameters.add( new Parameter( ExportCommandExecutor.SCOPE_PARAMETER, scope ) );
        }

        // -------------------------------------------------------------------
        // figure out the ldif file to export entries to
        // -------------------------------------------------------------------
        if ( cmd.hasOption( 'f' ) )
        {
            ldifFileName = cmd.getOptionValue( 'f' );

            if ( ( ldifFileName == null ) || ( "".equals( ldifFileName ) ) )
            {
                System.err.println( "ldif file name must be provided" );
                System.exit( 1 );
            }

            File ldifFile = new File( ldifFileName );

            if ( isDebugEnabled() )
            {
                try
                {
                    System.out.println( "ldif file to export entries to: " + ldifFile.getCanonicalPath() );
                }
                catch ( IOException ioe )
                {
                    System.out.println( "ldif file to export entries to: " + ldifFile );
                }
            }
        }
        else
        {
            System.err.println( "ldif file name must be provided" );
            System.exit( 1 );
        }

        parameters.add( new Parameter( ExportCommandExecutor.FILE_PARAMETER, ldifFileName ) );

        parameters.add( new Parameter( ExportCommandExecutor.DEBUG_PARAMETER, new Boolean( isDebugEnabled() ) ) );
        parameters.add( new Parameter( ExportCommandExecutor.QUIET_PARAMETER, new Boolean( isQuietEnabled() ) ) );
        parameters.add( new Parameter( ExportCommandExecutor.VERBOSE_PARAMETER, new Boolean( isVerboseEnabled() ) ) );
    }
}
