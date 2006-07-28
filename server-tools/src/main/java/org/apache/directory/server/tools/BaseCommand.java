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
package org.apache.directory.server.tools;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.AlreadySelectedException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.directory.server.tools.commands.diagnosticcmd.DiagnosticCommandCL;
import org.apache.directory.server.tools.commands.disconnectnotificationcmd.DisconnectNotificationCommandCL;
import org.apache.directory.server.tools.commands.dumpcmd.DumpCommandCL;
import org.apache.directory.server.tools.commands.exportcmd.ExportCommandCL;
import org.apache.directory.server.tools.commands.gracefulshutdowncmd.GracefulShutdownCommandCL;
import org.apache.directory.server.tools.commands.importcmd.ImportCommandCL;
import org.apache.directory.server.tools.commands.storedprocedurecmd.StoredProcedureCommandCL;
import org.apache.directory.server.tools.request.BaseToolCommandCL;


/**
 * The primary command base class.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BaseCommand
{
    private Map commands = new HashMap();

    private List commandsOrdered = new ArrayList();

    private Options global = new Options();

    private String productCommand;

    private String productVersion;

    private String productDisplayName;

    private String productUrl;

    private String productBanner;


    public BaseCommand()
    {
        init();
    }


    protected void init()
    {
        BaseToolCommandCL command;

        command = new DiagnosticCommandCL();
        commands.put( command.getName(), command );
        commandsOrdered.add( command.getName() );

        command = new DumpCommandCL();
        commands.put( command.getName(), command );
        commandsOrdered.add( command.getName() );

        command = new GracefulShutdownCommandCL();
        commands.put( command.getName(), command );
        commandsOrdered.add( command.getName() );

        command = new ImportCommandCL();
        commands.put( command.getName(), command );
        commandsOrdered.add( command.getName() );

        command = new DisconnectNotificationCommandCL();
        commands.put( command.getName(), command );
        commandsOrdered.add( command.getName() );

        command = new ExportCommandCL();
        commands.put( command.getName(), command );
        commandsOrdered.add( command.getName() );
        
        command = new StoredProcedureCommandCL();
        commands.put( command.getName(), command );
        commandsOrdered.add( command.getName() );

        Option op = new Option( "i", "install-path", true, "path to installation directory" );
        getGlobal().addOption( op );
        op = new Option( "b", "banner", false, "suppress banner print outs" );
        getGlobal().addOption( op );
        op = new Option( "d", "debug", false, "toggle debug mode" );
        getGlobal().addOption( op );
        op = new Option( "v", "verbose", false, "toggle verbose debugging" );
        getGlobal().addOption( op );
        op = new Option( "q", "quiet", false, "keep the noise down to a minimum" );
        getGlobal().addOption( op );
        op = new Option( "c", "configuration", false, "force loading the server.xml (requires -i)" );
        getGlobal().addOption( op );
        op = new Option( "version", false, "print the version information and exit" );
        getGlobal().addOption( op );
    }


    public static boolean hasBannerOption( String[] args )
    {
        for ( int ii = 0; ii < args.length; ii++ )
        {
            if ( args[ii].equals( "-b" ) || args[ii].equals( "-banner" ) )
            {
                return true;
            }
        }
        return false;
    }


    public CommandLine getCommandLine( String command, String[] args )
    {
        Options all = allOptions( command );
        CommandLineParser parser = new PosixParser();
        CommandLine cmdline = null;
        try
        {
            cmdline = parser.parse( all, args );
        }
        catch ( AlreadySelectedException ase )
        {
            System.err.println( "Command line parsing failed for " + command + ".  Reason: already selected "
                + ase.getMessage() );
            System.exit( 1 );
        }
        catch ( MissingArgumentException mae )
        {
            System.err.println( "Command line parsing failed for " + command + ".  Reason: missing argument "
                + mae.getMessage() );
            System.exit( 1 );
        }
        catch ( MissingOptionException moe )
        {
            System.err.println( "Command line parsing failed for " + command + ".  Reason: missing option "
                + moe.getMessage() );
            System.exit( 1 );
        }
        catch ( UnrecognizedOptionException uoe )
        {
            System.err.println( "Command line parsing failed for " + command + ".  Reason: unrecognized option"
                + uoe.getMessage() );
            System.exit( 1 );
        }
        catch ( ParseException pe )
        {
            System.err.println( "Command line parsing failed for " + command + ".  Reason: " + pe.getClass() );
            System.exit( 1 );
        }

        return cmdline;
    }


    public Options allOptions( String command )
    {
        if ( command.equals( "help" ) )
        {
            return getGlobal();
        }

        Options all = new Options();
        BaseToolCommandCL cmd = ( BaseToolCommandCL ) getCommands().get( command );

        for ( Iterator ii = getGlobal().getOptions().iterator(); ii.hasNext(); )
        {
            all.addOption( ( Option ) ii.next() );
        }

        for ( Iterator ii = cmd.getOptions().getOptions().iterator(); ii.hasNext(); )
        {
            all.addOption( ( Option ) ii.next() );
        }
        return all;
    }


    public static void dumpArgs( String msg, String[] args )
    {
        if ( args.length == 0 )
        {
            System.out.println( msg );
            System.out.println( "\t NONE" );
            return;
        }

        StringBuffer buf = new StringBuffer();
        buf.append( msg ).append( "\n" );

        for ( int ii = 0; ii < args.length; ii++ )
        {
            buf.append( "\targs[" + ii + "] = " ).append( args[ii] ).append( "\n" );
        }
        System.out.println( buf );
    }


    public void helpOnCommand( String command )
    {
        if ( command.equals( "help" ) )
        {
            printUsage();
            System.exit( 0 );
        }

        if ( getCommands().containsKey( command ) )
        {
            BaseToolCommandCL cmd = ( BaseToolCommandCL ) getCommands().get( command );
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( getProductCommand() + " " + cmd + " [options]", cmd.getOptions() );
        }
        else
        {
            System.err.println( command + ": unknown command" );
            System.exit( 1 );
        }
    }


    public void printUsage()
    {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( getProductCommand() + " <command> [options]", "\nGlobal options:", getGlobal(),
            "\nType \"" + getProductCommand() + " help <command>\" for help on a command." );
        System.out.println( "\nAvailable commands:" );

        Iterator it = commandsOrdered.iterator();
        System.out.println( "\thelp" );

        while ( it.hasNext() )
        {
            System.out.println( "\t" + it.next() );
        }

        System.out.println( "\nThese tools are used to manage " + getProductDisplayName() + "." );
        System.out.println( "For additional information, see " + getProductUrl() );
    }

    static final String BANNER = "       _                     _          ____  ____    _____           _      \n"
        + "      / \\   _ __   __ _  ___| |__   ___|  _ \\/ ___|  |_   _|__   ___ | |___  \n"
        + "     / _ \\ | '_ \\ / _` |/ __| '_ \\ / _ \\ | | \\___ \\    | |/ _ \\ / _ \\| / __| \n"
        + "    / ___ \\| |_) | (_| | (__| | | |  __/ |_| |___) |   | | (_) | (_) | \\__ \\ \n"
        + "   /_/   \\_\\ .__/ \\__,_|\\___|_| |_|\\___|____/|____/    |_|\\___/ \\___/|_|___/ \n"
        + "           |_|                                                               \n";


    public void printBanner()
    {
        System.out.println( getProductBanner() );
    }


    public void setProductCommand( String productCommand )
    {
        this.productCommand = productCommand;
    }


    public String getProductCommand()
    {
        return productCommand;
    }


    public void setProductVersion( String productVersion )
    {
        this.productVersion = productVersion;
    }


    public String getProductVersion()
    {
        return productVersion;
    }


    public void setProductDisplayName( String productDisplayName )
    {
        this.productDisplayName = productDisplayName;
    }


    public String getProductDisplayName()
    {
        return productDisplayName;
    }


    public void setProductUrl( String productUrl )
    {
        this.productUrl = productUrl;
    }


    public String getProductUrl()
    {
        return productUrl;
    }


    public void setProductBanner( String productBanner )
    {
        this.productBanner = productBanner;
    }


    public String getProductBanner()
    {
        return productBanner;
    }


    public void setCommands( Map commands )
    {
        this.commands = commands;
    }


    public Map getCommands()
    {
        return commands;
    }


    public void setGlobal( Options global )
    {
        this.global = global;
    }


    public Options getGlobal()
    {
        return global;
    }
}
