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


import java.util.Hashtable;

import javax.naming.CommunicationException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.directory.daemon.AvailablePortFinder;
import org.apache.directory.shared.ldap.message.extended.GracefulShutdownRequest;


/**
 * A command used to send a graceful disconnect to established clients 
 * while allowing them time to complete operations already in progress.
 * 
 * @see <a href="http://docs.safehaus.org/display/APACHEDS/LDAP+Extensions+for+Graceful+Shutdown">
 * Graceful Shutdown</a>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class GracefulShutdownCommand extends ToolCommand
{
    public static final String PORT_RANGE = "(" + AvailablePortFinder.MIN_PORT_NUMBER + ", "
        + AvailablePortFinder.MAX_PORT_NUMBER + ")";

    private static final int DELAY_MAX = 86400;

    private static final int TIME_OFFLINE_MAX = 720;

    private int port = 10389;
    private String host = "localhost";
    private String password = "secret";
    private int delay;
    private int timeOffline;


    protected GracefulShutdownCommand()
    {
        super( "graceful" );
    }

    private boolean isWaiting;
    private boolean isSuccess = false;
    private Thread executeThread = null;


    public void execute( CommandLine cmd ) throws Exception
    {
        executeThread = Thread.currentThread();
        processOptions( cmd );

        if ( isDebugEnabled() )
        {
            System.out.println( "Parameters for GracefulShutdown extended request:" );
            System.out.println( "port = " + port );
            System.out.println( "host = " + host );
            System.out.println( "password = " + password );
            System.out.println( "delay = " + delay );
            System.out.println( "timeOffline = " + timeOffline );
        }

        Hashtable env = new Hashtable();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://" + host + ":" + port );
        env.put( "java.naming.security.principal", "uid=admin,ou=system" );
        env.put( "java.naming.security.credentials", password );
        env.put( "java.naming.security.authentication", "simple" );

        LdapContext ctx = new InitialLdapContext( env, null );
        if ( !isQuietEnabled() )
        {
            System.out.println( "Connection to the server established.\n"
                + "Sending extended request and blocking for shutdown:" );
            isWaiting = true;
            Thread t = new Thread( new Ticker() );
            t.start();
        }
        try
        {
            ctx.extendedOperation( new GracefulShutdownRequest( 0, timeOffline, delay ) );
            isSuccess = true;
        }
        catch ( Throwable t )
        {
            /*
             * Sometimes because of timing issues we show a failure when the 
             * shutdown has succeeded so we should check if the server is up
             * before we set success to false.
             */
            try
            {
                new InitialLdapContext( env, null );
                isSuccess = false;
                System.err.print( "shutdown request failed with error: " + t.getMessage() );
            }
            catch( CommunicationException e )
            {
                isSuccess = true;
            }
        }
        isWaiting = false;
        ctx.close();
    }

    class Ticker implements Runnable
    {
        public void run()
        {
            if ( !isQuietEnabled() )
                System.out.print( "[waiting for shutdown] " );
            while ( isWaiting )
            {
                try
                {
                    Thread.sleep( 1000 );
                }
                catch ( InterruptedException e )
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if ( !isQuietEnabled() )
                    System.out.print( "." );
            }
            if ( isSuccess )
            {
                if ( !isQuietEnabled() )
                    System.out.println( "\n[shutdown complete]" );
                try
                {
                    executeThread.join( 1000 );
                }
                catch ( InterruptedException e )
                {
                    e.printStackTrace();
                }
                System.exit( 0 );
            }
            else
            {
                if ( !isQuietEnabled() )
                    System.out.println( "\n[shutdown failed]" );
                try
                {
                    executeThread.join( 1000 );
                }
                catch ( InterruptedException e )
                {
                    e.printStackTrace();
                }
                System.exit( 1 );
            }
        }
    }


    private void processOptions( CommandLine cmd )
    {
        if ( isDebugEnabled() )
        {
            System.out.println( "Processing options for graceful shutdown ..." );
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
        }
        else if ( getConfiguration() != null )
        {
            port = getConfiguration().getLdapPort();

            if ( isDebugEnabled() )
            {
                System.out.println( "port overriden by server.xml configuration: " + port );
            }
        }
        else if ( isDebugEnabled() )
        {
            System.out.println( "port set to default: " + port );
        }

        // -------------------------------------------------------------------
        // figure out the host value
        // -------------------------------------------------------------------

        if ( cmd.hasOption( 'h' ) )
        {
            host = cmd.getOptionValue( 'h' );

            if ( isDebugEnabled() )
            {
                System.out.println( "host overriden by -h option: " + host );
            }
        }
        else if ( isDebugEnabled() )
        {
            System.out.println( "host set to default: " + host );
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
        }
        else if ( isDebugEnabled() )
        {
            System.out.println( "password set to default: " + password );
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
        }
        else if ( isDebugEnabled() )
        {
            System.out.println( "Using default delay value of " + delay );
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
        }
        else if ( isDebugEnabled() )
        {
            System.out.println( "Using default timeOffline value of " + delay );
        }
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
}
