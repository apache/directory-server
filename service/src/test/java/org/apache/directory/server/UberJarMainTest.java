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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Calendar;

import org.apache.directory.api.ldap.codec.api.SchemaBinaryAttributeDetector;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.Network;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.api.partition.PartitionNexus;
import org.apache.directory.server.core.security.CertificateUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import sun.security.x509.X500Name;

import static org.junit.Assert.assertEquals;


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
    
    private KeyStore keyStore;
    private File keyStoreFile;

    @Before
    public void create()
    {
        // Getting tmp directory
        File tmpDirectory = new File( System.getProperty( "java.io.tmpdir" ) );
        tmpDirectory.deleteOnExit();

        // Creating an instance directory
        Calendar calendar = Calendar.getInstance();
        instanceDirectory = new File( tmpDirectory, "ApacheDS-" + calendar.get( Calendar.YEAR )
            + calendar.get( Calendar.MONTH ) + calendar.get( Calendar.DATE ) + calendar.get( Calendar.HOUR )
            + calendar.get( Calendar.MINUTE ) + calendar.get( Calendar.SECOND ) );
        instanceDirectory.mkdir();

        // Creating the UberjarMain
        uberjarMain = new UberjarMain();
        
        try
        {
            // Create a temporary keystore, be sure to remove it when exiting the test
            File keyStoreFile = File.createTempFile( "testStore", "ks" );
            keyStoreFile.deleteOnExit();

            
            keyStore = KeyStore.getInstance( KeyStore.getDefaultType() );
            char[] keyStorePassword = "secret".toCharArray();
            
            try ( InputStream keyStoreData = new FileInputStream( keyStoreFile ) )
            {
                keyStore.load( null, keyStorePassword );
            }

            // Generate the asymmetric keys, using EC algorithm
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance( "EC" );
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            
            // Generate the subject's name
            @SuppressWarnings("restriction")
            X500Name owner = new X500Name( "apacheds", "directory", "apache", "US" );

            // Create the self-signed certificate
            X509Certificate certificate = CertificateUtil.generateSelfSignedCertificate( owner, keyPair, 365, "SHA256WithECDSA" );
            
            keyStore.setKeyEntry( "apachedsKey", keyPair.getPrivate(), keyStorePassword, new X509Certificate[] { certificate } );
            
            FileOutputStream out = new FileOutputStream( keyStoreFile );
            keyStore.store( out, keyStorePassword );
        }
        catch ( Exception e )
        {
            
        }

    }

    
    @After
    public void delete() throws Exception
    {
        if ( uberjarMain != null )
        {
            uberjarMain.stop();
        }
    }
    
    
    private LdapConnection createConnection() throws LdapException, UnknownHostException
    {
        LdapConnectionConfig configuration = new LdapConnectionConfig();
        configuration.setLdapHost( Network.LOOPBACK_HOSTNAME );
        configuration.setLdapPort( 10389 );
        configuration.setName( ServerDNConstants.ADMIN_SYSTEM_DN );
        configuration.setCredentials( PartitionNexus.ADMIN_PASSWORD_STRING );
        configuration.setBinaryAttributeDetector( new SchemaBinaryAttributeDetector( null ) );
        LdapConnection connection = new LdapNetworkConnection( configuration );
        connection.loadSchema();

        // Binding on the connection
        connection.bind();
        
        return connection;
    }
    
    
    private Thread createServer()
    {
        // First start the server to initialize the example partition 
        uberjarMain.start( instanceDirectory.toString() );

        // Creating a separate thread for the connection verification
        Thread connectionVerificationThread = new Thread()
        {
            public void run()
            {
                LdapConnection connection = null;
                
                try
                {
                    // Creating a connection on the created server
                    connection = createConnection();

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

        return connectionVerificationThread;
    }


    /**
     * Tests the creation of a new ApacheDS Service instance.
     *
     * @throws Exception
     */
    @Test
    public void serviceInstanceTest() throws Exception
    {   
        Thread connectionVerificationThread = createServer();
        
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


    /**
     * Tests the repair of an existing ApacheDS Service instance.
     *
     * @throws Exception
     */
    @Test
    public void repairTest() throws Exception
    {   
        // First start the server to initialize the example partition 
        Thread connectionVerificationThread = createServer();
        
        // Starting the connection verification thread
        // and waiting for the termination of it
        connectionVerificationThread.start();
        connectionVerificationThread.join();

        // Checking if verification is successful
        if ( !verified )
        {
            fail();
        }
        
        // Add a few entries to create a more complex hierarchy
        // We will have :
        // dc=example,dc=com
        //    ou=people
        //      ou=committers
        //        cn=emmanuel
        //        cn=kiran
        //        cn=stefan
        //        cn=radovan
        //      ou=pmcs
        //        cn=emmanuel
        //        cn=kiran
        //        cn=stefan
        //   ou=groups
        //     cn=users
        LdapConnection connection = createConnection();
        
        // First level
        Entry people = new DefaultEntry( 
            "ou=People,dc=example,dc=com",
            "objectClass: organizationalUnit",
            "objectClass: top",
            "ou: People"
            );
        
        connection.add( people );
        
        Entry groups = new DefaultEntry( 
            "ou=Groups,dc=example,dc=com",
            "objectClass: organizationalUnit",
            "objectClass: top",
            "ou: Groups"
            );
        
        connection.add( groups );
        
        // Second level
        Entry committers  = new DefaultEntry( 
            "ou=Committers,ou=people,dc=example,dc=com",
            "objectClass: organizationalUnit",
            "objectClass: top",
            "ou: Committers"
            );

        connection.add( committers );

        Entry pmcs  = new DefaultEntry( 
            "ou=Pmcs,ou=people,dc=example,dc=com",
            "objectClass: organizationalUnit",
            "objectClass: top",
            "ou: Pmcs"
            );

        connection.add( pmcs );

        Entry users  = new DefaultEntry( 
            "ou=Users,ou=people,dc=example,dc=com",
            "objectClass: organizationalUnit",
            "objectClass: top",
            "ou: Users"
            );

        connection.add( users );

        // Third level, committers
        Entry emmanuelCommitter  = new DefaultEntry( 
            "cn=emmanuel,ou=Committers,ou=people,dc=example,dc=com",
            "objectClass: person",
            "objectClass: top",
            "cn: emmanuel",
            "sn: Emmanuel Lecharny"
            );

        connection.add( emmanuelCommitter );

        Entry kiranCommitter  = new DefaultEntry( 
            "cn=kiran,ou=Committers,ou=people,dc=example,dc=com",
            "objectClass: person",
            "objectClass: top",
            "cn: kiran",
            "sn: Kiran Ayyagari"
            );

        connection.add( kiranCommitter );

        Entry stefanCommitter  = new DefaultEntry( 
            "cn=stefan,ou=Committers,ou=people,dc=example,dc=com",
            "objectClass: person",
            "objectClass: top",
            "cn: stefan",
            "sn: Stefan Seelmann"
            );

        connection.add( stefanCommitter );
        
        Entry radovanCommitter  = new DefaultEntry( 
            "cn=radovan,ou=Committers,ou=people,dc=example,dc=com",
            "objectClass: person",
            "objectClass: top",
            "cn: radovan",
            "sn: Radovan Semancik"
            );

        connection.add( radovanCommitter );

        // Third level, PMCs
        Entry emmanuelPmc = new DefaultEntry( 
            "cn=emmanuel,ou=Pmcs,ou=people,dc=example,dc=com",
            "objectClass: person",
            "objectClass: top",
            "cn: emmanuel",
            "sn: Emmanuel Lecharny"
            );

        connection.add( emmanuelPmc );

        Entry kiranPmc = new DefaultEntry( 
            "cn=kiran,ou=Pmcs,ou=people,dc=example,dc=com",
            "objectClass: person",
            "objectClass: top",
            "cn: kiran",
            "sn: Kiran Ayyagari"
            );

        connection.add( kiranPmc );

        Entry stefanPmc = new DefaultEntry( 
            "cn=stefan,ou=Pmcs,ou=people,dc=example,dc=com",
            "objectClass: person",
            "objectClass: top",
            "cn: stefan",
            "sn: Stefan Seelmann"
            );

        connection.add( stefanPmc );
        
        // Now, check that we have 13 entries
        int entryCount = 0;
        
        EntryCursor cursor = connection.search( "dc=example, dc=com","(ObjectClass=*)", SearchScope.SUBTREE, "*" );
        
        while ( cursor.next() )
        {
            cursor.get();
            entryCount++;
        }
        
        assertEquals( 13, entryCount );

        // Stop the server
        uberjarMain.stop();

        // Try to repair it (also starts and stops the server)
        uberjarMain.repair( instanceDirectory.toString() );

        // And restart it
        connectionVerificationThread = createServer();
        
        // Starting the connection verification thread
        // and waiting for the termination of it
        connectionVerificationThread.start();
        connectionVerificationThread.join();

        // Checking if verification is successful
        if ( !verified )
        {
            fail();
        }

        // Check the content
        connection = createConnection();

        entryCount = 0;
        
        cursor = connection.search( "dc=example, dc=com","(ObjectClass=*)", SearchScope.SUBTREE, "*" );
        
        while ( cursor.next() )
        {
            cursor.get();
            entryCount++;
        }
        
        assertEquals( 13, entryCount );
    }
}
