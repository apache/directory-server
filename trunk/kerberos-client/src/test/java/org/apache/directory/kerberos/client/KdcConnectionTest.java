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
package org.apache.directory.kerberos.client;


import static org.apache.directory.kerberos.client.ChangePasswordResultCode.KRB5_KPASSWD_SUCCESS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.kerberos.client.Kinit;
import org.apache.directory.kerberos.credentials.cache.CredentialsCache;
import org.apache.directory.server.annotations.CreateChngPwdServer;
import org.apache.directory.server.annotations.CreateKdcServer;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.kerberos.KeyDerivationInterceptor;
import org.apache.directory.server.kerberos.kdc.KerberosTestUtils;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.apache.directory.server.protocol.shared.transport.UdpTransport;
import org.apache.directory.shared.kerberos.exceptions.KerberosException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(FrameworkRunner.class)
@CreateDS(name = "KdcConnectionTest-class", enableChangeLog = false,
    partitions =
        {
            @CreatePartition(
                name = "example",
                suffix = "dc=example,dc=com",
                contextEntry=@ContextEntry( entryLdif = 
                    "dn: dc=example,dc=com\n" +
                    "objectClass: domain\n" +
                    "dc: example" ) )
    },
    additionalInterceptors =
        {
            KeyDerivationInterceptor.class
    })
@CreateLdapServer(
    transports =
        {
            @CreateTransport(protocol = "LDAP")
    })
@CreateKdcServer(
    searchBaseDn = "dc=example,dc=com",
    transports =
        {
            @CreateTransport(protocol = "TCP"),
            @CreateTransport(protocol = "UDP")
    },
    chngPwdServer = @CreateChngPwdServer
    (
        transports =
        {
            @CreateTransport(protocol = "TCP"),
            @CreateTransport(protocol = "UDP")
        }    
    ))
@ApplyLdifs({
    // krbtgt
    "dn: uid=krbtgt,dc=example,dc=com",
    "objectClass: top",
    "objectClass: person",
    "objectClass: inetOrgPerson",
    "objectClass: krb5principal",
    "objectClass: krb5kdcentry",
    "cn: KDC Service",
    "sn: Service",
    "uid: krbtgt",
    "userPassword: secret",
    "krb5PrincipalName: krbtgt/EXAMPLE.COM@EXAMPLE.COM",
    "krb5KeyVersionNumber: 0",
    
    // changepwd
    "dn: uid=kadmin,dc=example,dc=com",
    "objectClass: top",
    "objectClass: person",
    "objectClass: inetOrgPerson",
    "objectClass: krb5principal",
    "objectClass: krb5kdcentry",
    "cn: changepw Service",
    "sn: Service",
    "uid: kadmin",
    "userPassword: secret",
    "krb5PrincipalName: kadmin/changepw@EXAMPLE.COM",
    "krb5KeyVersionNumber: 0",

    // app service
    "dn: uid=ldap,dc=example,dc=com",
    "objectClass: top",
    "objectClass: person",
    "objectClass: inetOrgPerson",
    "objectClass: krb5principal",
    "objectClass: krb5kdcentry",
    "cn: LDAP",
    "sn: Service",
    "uid: ldap",
    "userPassword: randall",
    "krb5PrincipalName: ldap/localhost@EXAMPLE.COM",
    "krb5KeyVersionNumber: 0"
})
/**
 * KDC connection tests
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KdcConnectionTest extends AbstractLdapTestUnit
{
    public static final String USERS_DN = "dc=example,dc=com";
    
    private static CoreSession session;

    private static KdcConnection conn;
    
    private static String userPassword = "secret";
    
    private static String principalName = "will@EXAMPLE.COM";
    
    private static String serverPrincipal;

    
    @Before
    public void setup() throws Exception
    {
        kdcServer.setSearchBaseDn( USERS_DN );
        if ( session == null )
        {
            session = kdcServer.getDirectoryService().getAdminSession();
            createPrincipal( "will", userPassword, principalName );
        }
        
        if ( conn == null )
        {
            KdcConfig config = KdcConfig.getDefaultConfig();
            config.setUseUdp( false );
            config.setKdcPort( kdcServer.getTcpPort() );
            config.setPasswdPort( kdcServer.getChangePwdServer().getTcpPort() );
            config.setEncryptionTypes( kdcServer.getConfig().getEncryptionTypes() );
            config.setTimeout( Integer.MAX_VALUE );
            conn = new KdcConnection( config );
        }
        if ( serverPrincipal == null )
        {
            serverPrincipal = KerberosTestUtils.fixServicePrincipalName( "ldap/localhost@EXAMPLE.COM", new Dn(
                "uid=ldap,dc=example,dc=com" ), getLdapServer() );
        }
    }
    
    
    @Test
    public void testGettingInitialTicketTcp() throws Exception
    {
        TgTicket tgt = conn.getTgt( principalName, userPassword );
        assertNotNull( tgt );
        assertFalse( tgt.isForwardable() );
    }

    
    @Test
    public void testGettingInitialTicketUdp() throws Exception
    {
        KdcConfig config = new KdcConfig();
        config.setKdcPort( getUdpPort() );
        config.setEncryptionTypes( kdcServer.getConfig().getEncryptionTypes() );
        config.setTimeout( Integer.MAX_VALUE );
        KdcConnection udpConn = new KdcConnection( config );
        
        TgTicket tgt = udpConn.getTgt( principalName, userPassword );
        assertNotNull( tgt );
        assertFalse( tgt.isForwardable() );
    }

    
    @Test
    public void testTgtFlags() throws Exception
    {
        TgtRequest tgtReq = new TgtRequest();
        tgtReq.setClientPrincipal( principalName );
        tgtReq.setPassword( userPassword );
        tgtReq.setForwardable( true );
        
        TgTicket tgt = conn.getTgt( tgtReq );
        assertNotNull( tgt );
        assertTrue( tgt.isForwardable() );
    }
    
    @Test
    public void testGetServiceTicket() throws Exception
    {
        ServiceTicket rep = conn.getServiceTicket( principalName, userPassword, serverPrincipal );
        System.out.println( rep );
        assertNotNull( rep );
    }
    
    @Test
    public void testKinit() throws Exception
    {
    	File ccFile = File.createTempFile( "credCache-", ".cc" );
    	Kinit kinit = new Kinit( conn );
    	kinit.setCredCacheFile( ccFile );
    	
    	kinit.kinit(principalName, userPassword);
    	System.out.println( "Kinit generated file " + ccFile.getAbsolutePath() );
    	
    	CredentialsCache credCache = CredentialsCache.load( ccFile );    	
        assertNotNull( credCache );
    }
    
    @Test
    public void testChangePassword() throws Exception
    {
        String uid = "kayyagari";
        String principal = uid + "@EXAMPLE.COM";
        createPrincipal( uid, userPassword, principal );
        
        String newPassword = "newPassword";
        
        ChangePasswordResult result = conn.changePassword( principal, userPassword, newPassword );
        assertNotNull( result );
        assertTrue( KRB5_KPASSWD_SUCCESS.getVal() == result.getCode().getVal() );
        
        try
        {
            conn.getTgt( principal, userPassword );
            fail( "should fail with kerberos exception cause of invalid password" );
        }
        catch( KerberosException e )
        {
            e.printStackTrace();
        }
        
        TgTicket tgt = conn.getTgt( principal, newPassword );
        assertNotNull( tgt );
    }
    
    
    private String createPrincipal( String uid, String userPassword, String principalName ) throws Exception
    {
        Entry entry = new DefaultEntry( session.getDirectoryService().getSchemaManager() );
        entry.setDn( "uid=" + uid + "," + USERS_DN );
        entry.add( "objectClass", "top", "person", "inetOrgPerson", "krb5principal", "krb5kdcentry" );
        entry.add( "cn", uid );
        entry.add( "sn", uid );
        entry.add( "uid", uid );
        entry.add( "userPassword", userPassword );
        entry.add( "krb5PrincipalName", principalName );
        entry.add( "krb5KeyVersionNumber", "0" );
        session.add( entry );
        
        return entry.getDn().getName();
    }

    private int getUdpPort()
    {
        for ( Transport t : kdcServer.getTransports() )
        {
            if ( t instanceof UdpTransport )
            {
                return t.getPort();
            }
        }

        return -1;
    }

}
