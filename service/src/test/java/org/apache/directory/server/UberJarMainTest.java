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


import static org.junit.Assert.fail;

import java.io.File;
import java.net.InetAddress;
import java.util.Calendar;

import org.apache.commons.io.FileUtils;
import org.apache.directory.api.ldap.codec.api.SchemaBinaryAttributeDetector;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.api.partition.PartitionNexus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * Test case for the UberJarMain class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class UberJarMainTest
{
    /** Flag used by connection verification thread */
    private boolean verified = true;

    /** The instance directory */
    private File instanceDirectory;
    
    /** The UberjarMain */
    private UberjarMain uberjarMain;

    @Before
    public void create()
    {
        // Getting tmp directory
        File tmpDirectory = new File( System.getProperty( "java.io.tmpdir" ) );

        // Creating an instance directory
        Calendar calendar = Calendar.getInstance();
        instanceDirectory = new File( tmpDirectory, "ApacheDS-" + calendar.get( Calendar.YEAR )
            + calendar.get( Calendar.MONTH ) + calendar.get( Calendar.DATE ) + calendar.get( Calendar.HOUR )
            + calendar.get( Calendar.MINUTE ) + calendar.get( Calendar.SECOND ) );
        instanceDirectory.mkdir();

        // Creating the UberjarMain
        uberjarMain = new UberjarMain();
    }

    
    @After
    public void delete() throws Exception
    {
        if ( uberjarMain != null )
        {
            uberjarMain.stop();
        }

        if ( instanceDirectory != null )
        {
            FileUtils.deleteDirectory( instanceDirectory );
        }
    }


    /**
     * Tests the creation of a new ApacheDS Service instance.
     *
     * @throws Exception
     */
    @Test
    public void serviceInstanceTest() throws Exception
    {   
        uberjarMain.start( new String[]
            { instanceDirectory.toString() } );

        // Creating a separate thread for the connection verification
        Thread connectionVerificationThread = new Thread()
        {
            public void run()
            {
                LdapNetworkConnection connection = null;
                
                try
                {
                    // Creating a connection on the created server
                    LdapConnectionConfig configuration = new LdapConnectionConfig();
                    configuration.setLdapHost( InetAddress.getLocalHost().getHostName() );
                    configuration.setLdapPort( 10389 );
                    configuration.setName( ServerDNConstants.ADMIN_SYSTEM_DN );
                    configuration.setCredentials( PartitionNexus.ADMIN_PASSWORD_STRING );
                    configuration.setBinaryAttributeDetector( new SchemaBinaryAttributeDetector( null ) );
                    connection = new LdapNetworkConnection( configuration );
                    connection.loadSchema();

                    // Binding on the connection
                    connection.bind();

                    // Looking for the Root DSE entry
                    Entry rootDseEntry = connection.lookup( Dn.ROOT_DSE );
                    
                    if ( rootDseEntry == null )
                    {
                        // This isn't good
                        verified = false;
                        return;
                    }
                }
                catch ( Exception e )
                {
                    verified = false;
                    e.printStackTrace();
                }
                finally
                {
                    try
                    {
                        connection.close();
                    }
                    catch ( Exception e )
                    {
                        // nothing we can do
                    }
                }
            };
        };
        
        // Starting the connection verification thread
        // and waiting for the termination of it
        connectionVerificationThread.start();
        connectionVerificationThread.join();

        // Checking if verification is successful
        if ( !verified )
        {
            fail();
        }
    }
}
