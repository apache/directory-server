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


import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.CommunicationException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.configuration.ServerStartupConfiguration;
import org.apache.directory.server.tools.ToolCommandListener;
import org.apache.directory.server.tools.execution.BaseToolCommandExecutor;
import org.apache.directory.server.tools.util.ListenerParameter;
import org.apache.directory.server.tools.util.Parameter;
import org.apache.directory.server.tools.util.ToolCommandException;
import org.apache.directory.shared.ldap.message.extended.GracefulShutdownRequest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;


/**
 * This is the Executor Class of the GracefulShutdown Command.
 * 
 * The command can be called using the 'execute' method.
 */
public class GracefulShutdownCommandExecutor extends BaseToolCommandExecutor
{
    // Additional Parameters
    public static final String DELAY_PARAMETER = "delay";
    public static final String TIMEOFFLINE_PARAMETER = "time-offline";

    private int delay;

    private int timeOffline;

    private boolean isWaiting;
    private boolean isSuccess = false;
    private Thread executeThread = null;


    public GracefulShutdownCommandExecutor()
    {
        super( "graceful" );
    }


    /**
     * Executes the command.
     * <p>
     * Use the following Parameters and ListenerParameters to call the command.
     * <p>
     * Parameters : <ul>
     *      <li>"HOST_PARAMETER" with a value of type 'String', representing server host</li>
     *      <li>"PORT_PARAMETER" with a value of type 'Integer', representing server port</li>
     *      <li>"PASSWORD_PARAMETER" with a value of type 'String', representing user password</li>
     *      <li>"DELAY_PARAMETER" with a value of type 'Integer', representing the delay (seconds) before shutdown</li>
     *      <li>"TIMEOFFLINE_PARAMETER" with a value of type 'Integer', representing the server offline time (minutes)</li>
     *      <li>"DEBUG_PARAMETER" with a value of type 'Boolean', true to enable debug</li>
     *      <li>"QUIET_PARAMETER" with a value of type 'Boolean', true to enable quiet</li>
     *      <li>"VERBOSE_PARAMETER" with a value of type 'Boolean', true to enable verbose</li>
     *      <li>"INSTALLPATH_PARAMETER" with a value of type 'String', representing the path to installation
     *          directory</li>
     *      <li>"CONFIGURATION_PARAMETER" with a value of type "Boolean", true to force loading the server.xml
     *          (requires "install-path")</li>
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
     * <b>Note:</b> "HOST_PARAMETER", "PORT_PARAMETER", "PASSWORD_PARAMETER", "DELAY_PARAMETER" and "TIMEOFFLINE_PARAMETER" are required.
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
        executeThread = Thread.currentThread();

        if ( isDebugEnabled() )
        {
            notifyOutputListener( "Parameters for GracefulShutdown extended request:" );
            notifyOutputListener( "port = " + port );
            notifyOutputListener( "host = " + host );
            notifyOutputListener( "password = " + password );
            notifyOutputListener( "delay = " + delay );
            notifyOutputListener( "timeOffline = " + timeOffline );
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
            notifyOutputListener( "Connection to the server established.\n"
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
                notifyErrorListener( "shutdown request failed with error: " + t.getMessage() );
            }
            catch ( CommunicationException e )
            {
                isSuccess = true;
            }
        }
        isWaiting = false;
        ctx.close();
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

        // Delay param
        Integer delayParam = ( Integer ) parameters.get( DELAY_PARAMETER );
        if ( delayParam != null )
        {
            delay = delayParam.intValue();
        }
        else
        {
            delay = 0;

            if ( isDebugEnabled() )
            {
                notifyOutputListener( "Using default delay value of " + delay );
            }
        }

        // Time-Offline param
        Integer timeOfflineParam = ( Integer ) parameters.get( TIMEOFFLINE_PARAMETER );
        if ( timeOfflineParam != null )
        {
            timeOffline = timeOfflineParam.intValue();
        }
        else
        {
            timeOffline = 0;

            if ( isDebugEnabled() )
            {
                notifyOutputListener( "Using default timeOffline value of " + timeOffline );
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

    class Ticker implements Runnable
    {
        public void run()
        {
            if ( !isQuietEnabled() )
                notifyOutputListener( "[waiting for shutdown] " );
            while ( isWaiting )
            {
                try
                {
                    Thread.sleep( 1000 );
                }
                catch ( InterruptedException e )
                {
                    e.printStackTrace();
                }
                if ( !isQuietEnabled() )
                    notifyOutputListener( "." );
            }
            if ( isSuccess )
            {
                if ( !isQuietEnabled() )
                    notifyOutputListener( "\n[shutdown complete]" );
                try
                {
                    executeThread.join( 1000 );
                }
                catch ( InterruptedException e )
                {
                    e.printStackTrace();
                }
                return;
            }
            else
            {
                if ( !isQuietEnabled() )
                    notifyOutputListener( "\n[shutdown failed]" );
                try
                {
                    executeThread.join( 1000 );
                }
                catch ( InterruptedException e )
                {
                    e.printStackTrace();
                }
                notifyExceptionListener( new ToolCommandException( "Shutdown failed" ) );
                return;
            }
        }
    }
}
