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
package org.apache.directory.server;


import javax.naming.Context;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.SocketAcceptor;
import org.apache.mina.util.AvailablePortFinder;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

import junit.framework.TestCase;


/**
 * Various add scenario tests.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ManyServersITest extends TestCase
{
    private static final String CTX_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";

    protected DirectoryService directoryService0;
    protected SocketAcceptor socketAcceptor0;
    protected LdapServer ldapServer0;
    protected int port0;
    
    protected DirectoryService directoryService1;
    protected SocketAcceptor socketAcceptor1;
    protected LdapServer ldapServer1;
    protected int port1;
    
    
    /**
     * Starts two separate LdapServers.
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        
        /*
         * Start the first LDAP server.
         */
        
        directoryService0 = new DefaultDirectoryService();
        directoryService0.setShutdownHookEnabled( false );
        socketAcceptor0 = new SocketAcceptor( null );
        ldapServer0 = new LdapServer();
        ldapServer0.setSocketAcceptor( socketAcceptor0 );
        ldapServer0.setDirectoryService( directoryService0 );
        ldapServer0.setIpPort( port0 = AvailablePortFinder.getNextAvailable( 1024 ) );

        doDelete( directoryService0.getWorkingDirectory() );
        directoryService0.startup();

        ldapServer0.start();

        /*
         * Start the second LDAP server.
         */
        
        directoryService1 = new DefaultDirectoryService();
        directoryService1.setShutdownHookEnabled( false );
        socketAcceptor1 = new SocketAcceptor( null );
        ldapServer1 = new LdapServer();
        ldapServer1.setSocketAcceptor( socketAcceptor1 );
        ldapServer1.setDirectoryService( directoryService1 );
        ldapServer1.setIpPort( port1 = AvailablePortFinder.getNextAvailable( 1024 ) );

        doDelete( directoryService1.getWorkingDirectory() );
        directoryService1.startup();

        ldapServer1.start();
    }


    /**
     * Sets the system context root to null.
     *
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
        
        try
        {
            ldapServer0.stop();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        try
        {
            directoryService0.shutdown();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        try
        {
            ldapServer1.stop();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        try
        {
            directoryService1.shutdown();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }


    /**
     * Deletes the Eve working directory.
     * @param wkdir the directory to delete
     * @throws IOException if the directory cannot be deleted
     */
    protected void doDelete( File wkdir ) throws IOException
    {
        if ( wkdir.exists() )
        {
            FileUtils.deleteDirectory( wkdir );
        }

        if ( wkdir.exists() )
        {
            throw new IOException( "Failed to delete: " + wkdir );
        }
    }


    /**
     * Try to start two servers in the same vm: for DIRSERVER-1151.
     * 
     * @see https://issues.apache.org/jira/browse/DIRSERVER-1151
     */
    public void testBothServers() throws Exception
    {
        LdapContext ctx0 = null;
        try
        {
            ctx0 = getLdapContext( port0 );
            assertNotNull( ctx0 );
            assertNotNull( ctx0.getAttributes( "" ) );
        }
        finally
        {
            if ( ctx0 != null )
            {
                ctx0.close();
            }
        }

        LdapContext ctx1 = null;
        
        try
        {
            ctx1 = getLdapContext( port1 );
            assertNotNull( ctx1 );
            assertNotNull( ctx1.getAttributes( "" ) );
        }
        finally
        {
            if ( ctx1 != null )
            {
                ctx1.close();
            }
        }
    }
    
    
    public LdapContext getLdapContext( int port ) throws Exception
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, CTX_FACTORY );
        env.put( Context.PROVIDER_URL, "ldap://localhost:" + port );
        env.put( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        return new InitialLdapContext( env, null );
    }
}
