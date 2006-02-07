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


import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.ldap.server.configuration.ServerStartupConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;


/**
 * The main() application which executes command targets.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ApachedsTools
{
    private final static Map commands = new HashMap();
    private final static Options global = new Options();
    private final static Properties props = new Properties();
    private final static String version;
    
    static
    {
        ToolCommand command;
        command = new DumpCommand();
        commands.put( command.getName(), command );
        command = new GracefulShutdownCommand();
        commands.put( command.getName(), command );
        command = new DiagnosticCommand();
        commands.put( command.getName(), command );
        command = new DisconnectNotificationCommand();
        commands.put( command.getName(), command );

        Option op = new Option( "i", "install-path", true, "path to apacheds installation directory" );
        global.addOption( op );
        op = new Option( "b", "banner", false, "suppress banner print outs" );
        global.addOption( op );
        op = new Option( "d", "debug", false, "toggle debug mode" );
        global.addOption( op );
        op = new Option( "v", "verbose", false, "toggle verbose debugging" );
        global.addOption( op );
        op = new Option( "q", "quiet", false, "keep the noise down to a minimum" );
        global.addOption( op );
        op = new Option( "c", "configuration", false, "force loading the server.xml (requires -i)" );
        global.addOption( op );
        op = new Option( "version", false, "print the version information and exit" );
        global.addOption( op );
        
        try
        {
            props.load( ApachedsTools.class.getResourceAsStream( "ApachedsTools.properties" ) );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }
        
        version = props.getProperty( "apacheds.tools.version" );
    }
    
    
    private static boolean hasBannerOption( String[] args )
    {
        for ( int ii = 0; ii < args.length; ii++ )
        {
            if ( args[ii].equals( "-b" ) || args[ii].equals( "-banner" ))
            {
                return true;
            }
        }
        return false;
    }
    

    public static void main( String [] args ) throws Exception
    {
        if ( ! hasBannerOption( args ) )
        {
            printBanner();
        }

        if ( args.length == 0 )
        {
            System.err.println( "Type apacheds-tools help for usage." );
            System.exit( 1 );
        }
        
        // help is a special command 
        String command = args[0].toLowerCase();
        if ( "help".equals( command ) )
        {
            CommandLine cmdline = getCommandLine( command, args );
            if ( cmdline.getArgs().length > 1 )
            {
                helpOnCommand( cmdline.getArgs()[1] );
                System.exit( 0 );
            }
            else
            {
                printUsage();
                System.exit( 0 );
            }
        }
        else if ( command.equals( "-version" ) ) 
        {
            System.out.println( "apacheds-tools version " + version );
            System.exit( 0 );
        }
        
        ToolCommand cmd = ( ToolCommand ) commands.get( command );
        if ( cmd == null )
        {
            System.err.println( "Unknown command: " + args[0] );
            System.err.println( "Type apacheds-tools help for usage." );
            System.exit( 1 );
        }        
        
        CommandLine cmdline = getCommandLine( command, args );
        if ( cmdline.hasOption( 'd' ) )
        {
            cmd.setDebugEnabled( true );
            dumpArgs( "raw command line arguments: ", args );
            dumpArgs( "parsed arguments: ", cmdline.getArgs() );
        }
        
        cmd.setQuietEnabled( cmdline.hasOption( 'q' ) );
        cmd.setDebugEnabled( cmdline.hasOption( 'd' ) );
        cmd.setVerboseEnabled( cmdline.hasOption( 'v' ) );
        cmd.setVersion( version );
        if ( cmdline.getOptionValue( 'i' ) != null )
        {
            cmd.setLayout( cmdline.getOptionValue( 'i' ) );
            if ( ! cmd.isQuietEnabled() )
            {
                System.out.println( "loading settings from: " + cmd.getLayout().getConfigurationFile() );
            }
            ApplicationContext factory = null;
            URL configUrl = configUrl = cmd.getLayout().getConfigurationFile().toURL();
            factory = new FileSystemXmlApplicationContext( configUrl.toString() );
            cmd.setConfiguration( ( ServerStartupConfiguration ) factory.getBean( "configuration" ) );
        }
        else if ( cmdline.hasOption( 'c' ) )
        {
            System.err.println( "forced configuration load (-c) requires the -i option" );
            System.exit( 1 );
        }
        
        cmd.execute( cmdline );
    }
    

    private static CommandLine getCommandLine( String command, String[] args )
    {
        Options all = allOptions( command );
        CommandLineParser parser = new PosixParser();
        CommandLine cmdline = null;
        try
        {
            cmdline = parser.parse( all, args );
        }
        catch( ParseException e )
        {
            System.err.println( "Command line parsing failed for " + command 
                + ".  Reason: " + e.getMessage() );
            System.exit( 1 );
        }
        return cmdline;
    }
    
    
    private static Options allOptions( String command )
    {
        if ( command.equals( "help" ) )
        {
            return global;
        }
        
        Options all = new Options();
        ToolCommand cmd = ( ToolCommand ) commands.get( command );
        for ( Iterator ii = global.getOptions().iterator(); ii.hasNext(); )
        {
            all.addOption( ( Option ) ii.next() );
        }
        
        for ( Iterator ii = cmd.getOptions().getOptions().iterator(); ii.hasNext(); )
        {
            all.addOption( ( Option ) ii.next() );
        }
        return all;
    }

    
    private static void dumpArgs( String msg, String[] args )
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
    
    
    private static void helpOnCommand( String command )
    {
        if ( command.equals( "help" ) )
        {
            printUsage();
            System.exit( 0 );
        }
        if ( commands.containsKey( command ) )
        {
            ToolCommand cmd = ( ToolCommand ) commands.get( command );
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "apacheds-tools "+cmd+" [options]", cmd.getOptions() );
        }
        else
        {
            System.err.println( command + ": unknown command" );
            System.exit( 1 );
        }
    }


    private static void printUsage()
    {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "apacheds-tools <command> [options]", 
            "\nGlobal options:", global, "\nType \"apacheds-tools help <command>\" for help on a command." );
        System.out.println( "\nAvalable commands:" );
        Iterator it = commands.values().iterator();
        System.out.println( "\thelp" );
        while( it.hasNext() )
        {
            System.out.println( "\t" + it.next() );
        }
        
        System.out.println( "\nThese apacheds-tools are used to manage the Apache Directory Server." );
        System.out.println( "For additional information, see http://directory.apache.org/" );
    }
    

    public static final String BANNER = 
        "       _                     _          ____  ____    _____           _      \n" +
        "      / \\   _ __   __ _  ___| |__   ___|  _ \\/ ___|  |_   _|__   ___ | |___  \n" +
        "     / _ \\ | '_ \\ / _` |/ __| '_ \\ / _ \\ | | \\___ \\    | |/ _ \\ / _ \\| / __| \n" +
        "    / ___ \\| |_) | (_| | (__| | | |  __/ |_| |___) |   | | (_) | (_) | \\__ \\ \n" +
        "   /_/   \\_\\ .__/ \\__,_|\\___|_| |_|\\___|____/|____/    |_|\\___/ \\___/|_|___/ \n" +
        "           |_|                                                               \n";

    public static void printBanner()
    {
        System.out.println( BANNER );
    }
}
