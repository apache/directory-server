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
package org.apache.directory.server.kerberos.kdc;


import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosTicket;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.core.api.LdapCoreSessionConnection;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.crypto.checksum.ChecksumType;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;


/**
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AbstractKerberosITest extends AbstractLdapTestUnit
{
    public static final String USERS_DN = "ou=users,dc=example,dc=com";
    public static final String REALM = "EXAMPLE.COM";
    public static final String USER_UID = "hnelson";
    public static final String USER_PASSWORD = "secret";
    public static final String LDAP_SERVICE_NAME = "ldap";
    public static final String HOSTNAME = KerberosTestUtils.getHostName();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    protected LdapCoreSessionConnection conn;


    @Before
    public void setUp() throws Exception
    {
        conn = new LdapCoreSessionConnection( service );

        enableKerberosSchema();
    }


    @After
    public void tearDown() throws Exception
    {
        conn.close();
    }

    class ObtainTicketParameters
    {
        Class<? extends Transport> transport;
        EncryptionType encryptionType;
        ChecksumType checksumType;
        Integer oldUdpPrefLimit;
        Integer oldCksumtypeDefault;


        public ObtainTicketParameters( Class<? extends Transport> transport, EncryptionType encryptionType,
            ChecksumType checksumType )
        {
            this.transport = transport;
            this.encryptionType = encryptionType;
            this.checksumType = checksumType;
        }
    }


    /**
     * Obtains a TGT and service tickets for the user.
     * Also makes some assertions on the received tickets.
     *
     * @param encryptionType the encryption type to use
     * @throws Exception
     */
    protected void testObtainTickets( ObtainTicketParameters parameters ) throws Exception
    {
        setupEnv( parameters );
        Subject subject = new Subject();

        KerberosTestUtils.obtainTGT( subject, USER_UID, USER_PASSWORD );

        assertEquals( 1, subject.getPrivateCredentials().size() );
        assertEquals( 0, subject.getPublicCredentials().size() );

        KerberosTestUtils.obtainServiceTickets( subject, USER_UID, LDAP_SERVICE_NAME, HOSTNAME );

        assertEquals( 2, subject.getPrivateCredentials().size() );
        assertEquals( 0, subject.getPublicCredentials().size() );
        
        for ( KerberosTicket kt : subject.getPrivateCredentials( KerberosTicket.class ) )
        {
            // System.out.println( kt.getClient() );
            // System.out.println( kt.getServer() );
            // System.out.println( kt.getSessionKeyType() );
            assertEquals( parameters.encryptionType.getValue(), kt.getSessionKeyType() );
        }
    }


    private void enableKerberosSchema() throws LdapException
    {
        Modification mod = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE,
            "m-disabled", "FALSE" );
        conn.modify( "cn=Krb5kdc,ou=schema", mod );
    }


    protected void setupEnv( ObtainTicketParameters parameters )
        throws Exception
    {
        // create krb5.conf with proper encryption type
        String krb5confPath = createKrb5Conf( parameters.checksumType, parameters.encryptionType, parameters.transport == TcpTransport.class );
        System.setProperty( "java.security.krb5.conf", krb5confPath );
        
        // change encryption type in KDC
        kdcServer.getConfig().setEncryptionTypes( Collections.singleton( parameters.encryptionType ) );

        // create principals
        createPrincipal( "uid=" + USER_UID, "Last", "First Last",
            USER_UID, USER_PASSWORD, USER_UID + "@" + REALM );

        createPrincipal( "uid=krbtgt", "KDC Service", "KDC Service",
            "krbtgt", "secret", "krbtgt/" + REALM + "@" + REALM );

        String servicePrincipal = LDAP_SERVICE_NAME + "/" + HOSTNAME + "@" + REALM;
        createPrincipal( "uid=ldap", "Service", "LDAP Service",
            "ldap", "randall", servicePrincipal );
    }


    /**
     * Creates the krb5.conf file for the test.
     * 
     * It looks similar to this:
     * 
     * <pre>
     * [libdefaults]
     *     default_realm = EXAMPLE.COM
     *     default_tkt_enctypes = aes256-cts-hmac-sha1-96
     *     default_tgs_enctypes = aes256-cts-hmac-sha1-96
     *     permitted_enctypes = aes256-cts-hmac-sha1-96
     * 
     * [realms]
     *     EXAMPLE.COM = {
     *         kdc = localhost:6088
     *     }
     * 
     * [domain_realm]
     *     .example.com = EXAMPLE.COM
     *     example.com = EXAMPLE.COM
     * </pre>
     *
     * @param encryptionType
     * @param checksumType
     * @return the path to the krb5.conf file
     * @throws IOException
     */
    private String createKrb5Conf( ChecksumType checksumType, EncryptionType encryptionType, boolean isTcp ) throws IOException
    {
        File file = folder.newFile( "krb5.conf" );

        String data = "";

        data += "[libdefaults]" + SystemUtils.LINE_SEPARATOR;
        data += "default_realm = " + REALM + SystemUtils.LINE_SEPARATOR;
        data += "default_tkt_enctypes = " + encryptionType.getName() + SystemUtils.LINE_SEPARATOR;
        data += "default_tgs_enctypes = " + encryptionType.getName() + SystemUtils.LINE_SEPARATOR;
        data += "permitted_enctypes = " + encryptionType.getName() + SystemUtils.LINE_SEPARATOR;
        //        data += "default_checksum = " + checksumType.getName() + SystemUtils.LINE_SEPARATOR;
        //        data += "ap_req_checksum_type = " + checksumType.getName() + SystemUtils.LINE_SEPARATOR;
        data += "default-checksum_type = " + checksumType.getName() + SystemUtils.LINE_SEPARATOR;
        
        if ( isTcp )
        {
            data += "udp_preference_limit = 1" + SystemUtils.LINE_SEPARATOR;
        }
            

        data += "[realms]" + SystemUtils.LINE_SEPARATOR;
        data += REALM + " = {" + SystemUtils.LINE_SEPARATOR;
        data += "kdc = " + HOSTNAME + ":" + kdcServer.getTransports()[0].getPort() + SystemUtils.LINE_SEPARATOR;
        data += "}" + SystemUtils.LINE_SEPARATOR;

        data += "[domain_realm]" + SystemUtils.LINE_SEPARATOR;
        data += "." + Strings.toLowerCase( REALM ) + " = " + REALM + SystemUtils.LINE_SEPARATOR;
        data += Strings.toLowerCase( REALM ) + " = " + REALM + SystemUtils.LINE_SEPARATOR;

        FileUtils.writeStringToFile( file, data );

        return file.getAbsolutePath();
    }


    private void createPrincipal( String rdn, String sn, String cn,
        String uid, String userPassword, String principalName ) throws LdapException
    {
        Entry entry = new DefaultEntry();
        entry.setDn( rdn + "," + USERS_DN );
        entry.add( "objectClass", "top", "person", "inetOrgPerson", "krb5principal", "krb5kdcentry" );
        entry.add( "cn", cn );
        entry.add( "sn", sn );
        entry.add( "uid", uid );
        entry.add( "userPassword", userPassword );
        entry.add( "krb5PrincipalName", principalName );
        entry.add( "krb5KeyVersionNumber", "0" );
        conn.add( entry );
    }

}
