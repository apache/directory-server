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
package org.apache.directory.server.tools.commands.disconnectnotificationcmd;


import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.event.EventContext;
import javax.naming.event.NamingExceptionEvent;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.UnsolicitedNotification;
import javax.naming.ldap.UnsolicitedNotificationEvent;
import javax.naming.ldap.UnsolicitedNotificationListener;

import org.apache.directory.server.configuration.ServerStartupConfiguration;
import org.apache.directory.server.tools.ToolCommandListener;
import org.apache.directory.server.tools.execution.BaseToolCommandExecutor;
import org.apache.directory.server.tools.util.ListenerParameter;
import org.apache.directory.server.tools.util.Parameter;
import org.apache.directory.shared.ldap.message.extended.GracefulDisconnect;
import org.apache.directory.shared.ldap.message.extended.NoticeOfDisconnect;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;


/**
 * This is the Executor Class of the Disconnect Notification Command.
 * 
 * The command can be called using the 'execute' method.
 */
public class DisconnectNotificationCommandExecutor extends BaseToolCommandExecutor implements
    UnsolicitedNotificationListener
{
    // Additional Parameter
    public static final String BINDDN_PARAMETER = "bindDN";

    private String bindDN;
    public final static String DEFAULT_BINDDN = "uid=admin,ou=system";
    UnsolicitedNotification notification;
    boolean canceled = false;


    public DisconnectNotificationCommandExecutor()
    {
        super( "notifications" );
    }


    /**
     * Executes the command.
     * <p>
     * Use the following Parameters and ListenerParameters to call the command.
     * <p>
     * Parameters : <ul>
     *      <li>"HOST_PARAMETER" with a value of type 'String', representing server host</li>
     *      <li>"PORT_PARAMETER" with a value of type 'Integer', representing server port</li>
     *      <li>"BINDDN_PARAMETER" with a value of type 'String', representing an apacheds user's dn</li>
     *      <li>"PASSWORD_PARAMETER" with a value of type 'String', representing user password</li>
     *      <li>"DEBUG_PARAMETER" with a value of type 'Boolean', true to enable debug</li>
     *      <li>"QUIET_PARAMETER" with a value of type 'Boolean', true to enable quiet</li>
     *      <li>"VERBOSE_PARAMETER" with a value of type 'Boolean', true to enable verbose</li>
     *      <li>"INSTALLPATH_PARAMETER" with a value of type 'String', representing the path to installation
     *          directory</li>
     *      <li>"CONFIGURATION_PARAMETER" with a value of type "Boolean"</li>   
     * </ul>
     * <br />
     * ListenersParameters : <ul>
     *      <li>"OUTPUTLISTENER_PARAMETER", a listener that will receive all output messages. It returns
     *          messages as a String.</li>
     *      <li>"ERRORLISTENER_PARAMETER", a listener that will receive all error messages. It returns messages
     *          as a String.</li>
     *      <li>"EXCEPTIONLISTENER_PARAMETER", a listener that will receive all exception(s) raised. It returns
     *          Exceptions.</li>
     * </ul>
     * <b>Note:</b> "HOST_PARAMETER", "PORT_PARAMETER", "BINDDN_PARAMETER" and "PASSWORD_PARAMETER" are required.
     */
    public void execute( Parameter[] params, ListenerParameter[] listeners )
    {
        processParameters( params );
        processListeners( listeners );

        try
        {
            execute();
        }
        catch ( Exception e )
        {
            notifyExceptionListener( e );
        }
    }


    private void execute() throws Exception
    {
        Hashtable env = new Hashtable();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://" + host + ":" + port );
        env.put( "java.naming.security.principal", bindDN );
        env.put( "java.naming.security.credentials", password );
        env.put( "java.naming.security.authentication", "simple" );

        LdapContext ctx = new InitialLdapContext( env, null );
        ctx = ctx.newInstance( null );
        UnsolicitedNotificationListener listener = new DisconnectNotificationCommandExecutor();
        ( ( EventContext ) ctx ).addNamingListener( "", SearchControls.SUBTREE_SCOPE, listener );

        notifyOutputListener( "Listening for notifications." );
        notifyOutputListener( "Press any key to terminate." );
        System.in.read();
        ctx.close();
        notifyOutputListener( "Process terminated!!!" );
    }


    private void processParameters( Parameter[] params )
    {
        Map parameters = new HashMap();
        for ( int i = 0; i < params.length; i++ )
        {
            Parameter parameter = params[i];
            parameters.put( parameter.getName(), parameter.getValue() );
        }

        // Quiet param
        Boolean quietParam = ( Boolean ) parameters.get( QUIET_PARAMETER );
        if ( quietParam != null )
        {
            setQuietEnabled( quietParam.booleanValue() );
        }

        // Debug param
        Boolean debugParam = ( Boolean ) parameters.get( DEBUG_PARAMETER );
        if ( debugParam != null )
        {
            setDebugEnabled( debugParam.booleanValue() );
        }

        // Verbose param
        Boolean verboseParam = ( Boolean ) parameters.get( VERBOSE_PARAMETER );
        if ( verboseParam != null )
        {
            setVerboseEnabled( verboseParam.booleanValue() );
        }

        // Install-path param
        String installPathParam = ( String ) parameters.get( INSTALLPATH_PARAMETER );
        if ( installPathParam != null )
        {
            try
            {
                setLayout( installPathParam );
                if ( !isQuietEnabled() )
                {
                    notifyOutputListener( "loading settings from: " + getLayout().getConfigurationFile() );
                }
                ApplicationContext factory = null;
                URL configUrl;

                configUrl = getLayout().getConfigurationFile().toURL();
                factory = new FileSystemXmlApplicationContext( configUrl.toString() );
                setConfiguration( ( ServerStartupConfiguration ) factory.getBean( "configuration" ) );
            }
            catch ( MalformedURLException e )
            {
                notifyErrorListener( e.getMessage() );
                notifyExceptionListener( e );
            }
        }

        // Host param
        String hostParam = ( String ) parameters.get( HOST_PARAMETER );
        if ( hostParam != null )
        {
            host = hostParam;
        }
        else
        {
            host = DEFAULT_HOST;

            if ( isDebugEnabled() )
            {
                notifyOutputListener( "host set to default: " + host );
            }
        }

        // Port param
        Integer portParam = ( Integer ) parameters.get( PORT_PARAMETER );
        if ( portParam != null )
        {
            port = portParam.intValue();
        }
        else if ( getConfiguration() != null )
        {
            port = getConfiguration().getLdapPort();

            if ( isDebugEnabled() )
            {
                notifyOutputListener( "port overriden by server.xml configuration: " + port );
            }
        }
        else
        {
            port = DEFAULT_PORT;

            if ( isDebugEnabled() )
            {
                notifyOutputListener( "port set to default: " + port );
            }
        }

        // Password param
        String passwordParam = ( String ) parameters.get( PASSWORD_PARAMETER );
        if ( passwordParam != null )
        {
            password = passwordParam;
        }
        else
        {
            password = DEFAULT_PASSWORD;

            if ( isDebugEnabled() )
            {
                notifyOutputListener( "password set to default: " + password );
            }
        }

        // BindDn param
        String bindDNParam = ( String ) parameters.get( BINDDN_PARAMETER );
        if ( bindDNParam != null )
        {
            bindDN = bindDNParam;
        }
        else
        {
            bindDN = DEFAULT_BINDDN;

            if ( isDebugEnabled() )
            {
                notifyOutputListener( "binddn set to default: " + bindDN );
            }
        }
    }


    private void processListeners( ListenerParameter[] listeners )
    {
        Map parameters = new HashMap();
        for ( int i = 0; i < listeners.length; i++ )
        {
            ListenerParameter parameter = listeners[i];
            parameters.put( parameter.getName(), parameter.getListener() );
        }

        // OutputListener param
        ToolCommandListener outputListener = ( ToolCommandListener ) parameters.get( OUTPUTLISTENER_PARAMETER );
        if ( outputListener != null )
        {
            this.outputListener = outputListener;
        }

        // ErrorListener param
        ToolCommandListener errorListener = ( ToolCommandListener ) parameters.get( ERRORLISTENER_PARAMETER );
        if ( errorListener != null )
        {
            this.errorListener = errorListener;
        }

        // ExceptionListener param
        ToolCommandListener exceptionListener = ( ToolCommandListener ) parameters.get( EXCEPTIONLISTENER_PARAMETER );
        if ( exceptionListener != null )
        {
            this.exceptionListener = exceptionListener;
        }
    }


    public void notificationReceived( UnsolicitedNotificationEvent evt )
    {
        notification = evt.getNotification();

        if ( notification.getID().equals( NoticeOfDisconnect.EXTENSION_OID ) )
        {
            notifyOutputListener( "\nRecieved NoticeOfDisconnect: " + NoticeOfDisconnect.EXTENSION_OID );
            notifyOutputListener( "Expect to loose this connection without further information." );
            canceled = true;
        }
        else if ( notification.getID().equals( GracefulDisconnect.EXTENSION_OID ) )
        {
            notifyOutputListener( "Recieved GracefulDisconnect: " + GracefulDisconnect.EXTENSION_OID );
            GracefulDisconnect gd = null;
			try {
				gd = new GracefulDisconnect( notification.getEncodedValue() );
			} catch (NamingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            notifyOutputListener( "LDAP server will shutdown in " + gd.getDelay() + " seconds." );
            notifyOutputListener( "LDAP server will be back online in " + gd.getTimeOffline() + " minutes." );

            if ( gd.getDelay() > 0 )
            {
                Thread t = new Thread( new Counter( gd.getDelay() ) );
                t.start();
            }
        }
        else
        {
            notifyOutputListener( "Unknown event recieved with OID: " + evt.getNotification().getID() );
        }
    }


    public void namingExceptionThrown( NamingExceptionEvent evt )
    {
        canceled = true;
        notifyOutputListener( "Got an excption event: " + evt.getException().getMessage() );
        notifyOutputListener( "Process shutting down abruptly." );
        notifyExceptionListener( evt );
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
            notifyOutputListener( "Starting countdown until server shutdown:" );
            notifyOutputListener( "[" );
            long delayMillis = delay * 1000 - 1000; // 1000 is for setup costs
            long startTime = System.currentTimeMillis();
            while ( System.currentTimeMillis() - startTime < delayMillis && !canceled )
            {
                try
                {
                    Thread.sleep( 1000 );
                }
                catch ( InterruptedException e )
                {
                }
                notifyOutputListener( "." );
            }

            if ( canceled )
            {
                notifyOutputListener( " -- countdown canceled -- " );
            }
            else
            {
                notifyOutputListener( "]" );
                notifyOutputListener( "Client shutting down gracefully." );
                return;
            }
        }
    }
}
