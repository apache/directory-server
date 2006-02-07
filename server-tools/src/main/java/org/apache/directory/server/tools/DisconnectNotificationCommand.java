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

import javax.naming.directory.SearchControls;
import javax.naming.event.EventContext;
import javax.naming.event.NamingExceptionEvent;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.UnsolicitedNotification;
import javax.naming.ldap.UnsolicitedNotificationListener;
import javax.naming.ldap.UnsolicitedNotificationEvent;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.directory.daemon.AvailablePortFinder;
import org.apache.ldap.common.message.extended.GracefulDisconnect;
import org.apache.ldap.common.message.extended.NoticeOfDisconnect;


/**
 * Responds to unsolicited notifications by launching an external process.  Also 
 * reconnects to the server an launches another process to notify that the server
 * is back up.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DisconnectNotificationCommand extends ToolCommand implements UnsolicitedNotificationListener
{
    UnsolicitedNotification notification;
    boolean canceled = false;
    private String host = "localhost";
    private int port = 10389;
    private String bindDn = "uid=admin,ou=system";
    private String password = "secret";
//    private String shutdownCommand = "echo"; 
//    private String[] shutdownCommandArgs = new String[] { 
//        "server $HOST:$PORT will shutdown for $OFFLINE minutes in $DELAY seconds" };

    
    protected DisconnectNotificationCommand()
    {
        super( "notifications" );
    }


    public void notificationReceived( UnsolicitedNotificationEvent evt )
    {
        notification = evt.getNotification();
        
        if ( notification.getID().equals( NoticeOfDisconnect.EXTENSION_OID ) )
        {
            System.out.println( "\nRecieved NoticeOfDisconnect: " + NoticeOfDisconnect.EXTENSION_OID );
            System.out.println( "Expect to loose this connection without further information." );
            canceled = true;
        }
        else if ( notification.getID().equals( GracefulDisconnect.EXTENSION_OID ) )
        {
            System.out.println( "Recieved GracefulDisconnect: " + GracefulDisconnect.EXTENSION_OID );
            GracefulDisconnect gd = new GracefulDisconnect( notification.getEncodedValue() );
            System.out.println( "LDAP server will shutdown in " + gd.getDelay() + " seconds." );
            System.out.println( "LDAP server will be back online in " + gd.getTimeOffline() + " minutes." );
            
            if ( gd.getDelay() > 0 )
            {
                Thread t = new Thread( new Counter( gd.getDelay() ) );
                t.start();
            }
        }
        else 
        {
            System.out.println( "Unknown event recieved with OID: " + evt.getNotification().getID() );
        }
    }


    public void namingExceptionThrown( NamingExceptionEvent evt )
    {
        canceled = true;
        System.out.println( "Got an excption event: " + evt.getException().getMessage() );
        System.out.println( "Process shutting down abruptly." );
        System.exit( 1 );
    }
    
    
    class Counter implements Runnable
    {
        int delay;
        
        Counter( int delay )
        {
            this.delay = delay;
        }
        
        public void run()
        {
            System.out.println( "Starting countdown until server shutdown:" );
            System.out.print( "[" ); 
            long delayMillis = delay * 1000 - 1000; // 1000 is for setup costs
            long startTime = System.currentTimeMillis();
            while ( System.currentTimeMillis() - startTime < delayMillis && !canceled )
            {
                try{ Thread.sleep( 1000 ); }catch ( InterruptedException e ){}
                System.out.print( "." );
            }
            
            if ( canceled )
            {
                System.out.println( " -- countdown canceled -- " );
            }
            else
            {
                System.out.println( "]" );
                System.out.println( "Client shutting down gracefully." );
                System.exit( 0 );
            }
        }
    }


    public void execute( CommandLine cmd ) throws Exception
    {
        processOptions( cmd );
        
        Hashtable env = new Hashtable();
        env.put("java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory");
        env.put("java.naming.provider.url", "ldap://" + host + ":" + port ); 
        env.put("java.naming.security.principal", bindDn ); 
        env.put("java.naming.security.credentials", password );
        env.put("java.naming.security.authentication", "simple" );

        LdapContext ctx = new InitialLdapContext( env, null );
        ctx = ctx.newInstance( null );
        UnsolicitedNotificationListener listener = new DisconnectNotificationCommand();
        ( ( EventContext ) ctx ).addNamingListener( "", SearchControls.SUBTREE_SCOPE, listener );
        
        System.out.println( "Listening for notifications." );
        System.out.println( "Press any key to terminate." );
        System.in.read();
        ctx.close();
        System.out.println( "Process terminated!!!" );
    }


    private void processOptions( CommandLine cmd )
    {
        if ( isDebugEnabled() )
        {
            System.out.println( "Processing options for disconnect notifications ..." );
        }
        
        // -------------------------------------------------------------------
        // figure out and error check the port value
        // -------------------------------------------------------------------

        if ( cmd.hasOption( 'p' ) )   // - user provided port w/ -p takes precedence
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
        // figure out the binddn value
        // -------------------------------------------------------------------

        if ( cmd.hasOption( 'u' ) )
        {
            bindDn = cmd.getOptionValue( 'u' );

            if ( isDebugEnabled() )
            {
                System.out.println( "binddn overriden by -u option: " + bindDn );
            }
        }
        else if ( isDebugEnabled() )
        {
            System.out.println( "binddn set to default: " + bindDn );
        }
    }


    public Options getOptions()
    {
        Options opts = new Options();
        Option op = new Option( "h", "host", true, "server host: defaults to localhost" );
        op.setRequired( false );
        opts.addOption( op );
        op = new Option(  "p", "port", true, "server port: defaults to 10389 or server.xml specified port" );
        op.setRequired( false );
        opts.addOption( op );
        op = new Option( "w", "password", true, "the apacheds administrator's password: defaults to secret" );
        op.setRequired( false );
        opts.addOption( op );
        op = new Option( "u", "binddn", true, "an apacheds user's dn: defaults to " + bindDn );
        op.setRequired( false );
        opts.addOption( op );
        op = new Option( "i", "install-path", true, "path to apacheds installation directory" );
        op.setRequired( false );
        opts.addOption( op );
        return opts;
    }
}
