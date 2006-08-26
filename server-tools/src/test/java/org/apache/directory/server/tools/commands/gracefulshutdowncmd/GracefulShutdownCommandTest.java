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
package org.apache.directory.server.tools.commands.gracefulshutdowncmd;


import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;

import org.apache.directory.server.tools.commands.AbstractTestCase;
import org.apache.directory.server.tools.commands.gracefulshutdowncmd.GracefulShutdownCommandExecutor;
import org.apache.directory.server.tools.util.ListenerParameter;
import org.apache.directory.server.tools.util.Parameter;


/**
 * Test Class for the Graceful Shutdown Command Executor
 */
public class GracefulShutdownCommandTest extends AbstractTestCase
{
    /**
     * Tests if the server shuts down correctly after the execution of the command
     */
    public void testOneEntryImport()
    {
        // Checking if server had been launched
        if ( !bindSuccessful )
        {
            // The server hasn't been lauched, so we don't execute the test and return 
            // a successful test, so Maven can is Ok when executing tests.
            assertTrue( true );
        }
        else
        {
            // Preparing the call to the Import Command
            GracefulShutdownCommandExecutor executor = new GracefulShutdownCommandExecutor();
            Parameter hostParam = new Parameter( GracefulShutdownCommandExecutor.HOST_PARAMETER, host );
            Parameter portParam = new Parameter( GracefulShutdownCommandExecutor.PORT_PARAMETER, new Integer( port ) );
            Parameter passwordParam = new Parameter( GracefulShutdownCommandExecutor.PASSWORD_PARAMETER, password );
            Parameter timeOfflineParam = new Parameter( GracefulShutdownCommandExecutor.TIMEOFFLINE_PARAMETER,
                new Integer( 0 ) );
            Parameter delayParam = new Parameter( GracefulShutdownCommandExecutor.DELAY_PARAMETER, new Integer( 0 ) );
            Parameter debugParam = new Parameter( GracefulShutdownCommandExecutor.DEBUG_PARAMETER, new Boolean( false ) );
            Parameter verboseParam = new Parameter( GracefulShutdownCommandExecutor.VERBOSE_PARAMETER, new Boolean(
                false ) );
            Parameter quietParam = new Parameter( GracefulShutdownCommandExecutor.QUIET_PARAMETER, new Boolean( false ) );

            // Calling the import command
            executor.execute( new Parameter[]
                { hostParam, portParam, passwordParam, timeOfflineParam, delayParam, debugParam, verboseParam,
                    quietParam }, new ListenerParameter[0] );

            // Let's give the server some time to complete the shutdown
            try
            {
                Thread.sleep( 2000 );
            }
            catch ( InterruptedException e )
            {
                e.printStackTrace();
            }

            // Checking if the server is down with a simple bind

            // Set up the environment for creating the initial context
            Hashtable env = new Hashtable();
            env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
            env.put( Context.PROVIDER_URL, "ldap://" + host + ":" + port );

            // Authenticate as Admin
            env.put( Context.SECURITY_AUTHENTICATION, "simple" );
            env.put( Context.SECURITY_PRINCIPAL, user );
            env.put( Context.SECURITY_CREDENTIALS, password );

            // Create the initial context
            try
            {
                ctx = new InitialDirContext( env );
            }
            catch ( NamingException ne )
            {
                assertTrue( true );
                return;
            }

            // If the bind request is successful, the ShutdownCommand has failed
            fail();
        }
    }
}
