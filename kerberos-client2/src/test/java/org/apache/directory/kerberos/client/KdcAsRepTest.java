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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;

import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.kerberos.client.KdcConnection;
import org.apache.directory.kerberos.client.TgTicket;
import org.apache.directory.kerberos.client.TgtRequest;
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
import org.apache.directory.shared.kerberos.codec.methodData.MethodDataContainer;
import org.apache.directory.shared.kerberos.codec.types.PaDataType;
import org.apache.directory.shared.kerberos.components.MethodData;
import org.apache.directory.shared.kerberos.exceptions.ErrorType;
import org.apache.directory.shared.kerberos.exceptions.KerberosException;
import org.apache.directory.shared.kerberos.messages.KrbError;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(FrameworkRunner.class)
@CreateDS(name = "KerberosTcpIT-class", enableChangeLog = false,
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
            @CreateTransport(protocol = "TCP")
    })
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
    
    //app service
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
public class KdcAsRepTest extends AbstractLdapTestUnit
{
    public static final String USERS_DN = "dc=example,dc=com";
    
    private static CoreSession session;

    private static KdcConnection conn;
    
    private String userPassword = "secret";
    
    private String principalName = "will@EXAMPLE.COM";
    
    @Before
    public void setup() throws Exception
    {
        if ( session == null )
        {
            kdcServer.setSearchBaseDn( USERS_DN );
            session = kdcServer.getDirectoryService().getAdminSession();
            createPrincipal( "will", userPassword, principalName );
        }
        
        if ( conn == null )
        {
            conn = KdcConnection.createTcpConnection( "localhost", kdcServer.getTcpPort() );
            conn.setTimeout( Integer.MAX_VALUE );
        }
    }
    
    
    @Test
    public void testKrbErrUnknwonClientPrincipal() throws Exception
    {
        try
        {
            conn.getTgt( "unknown@EXAMPLE.COM", userPassword );
        }
        catch( KerberosException e )
        {
            KrbError err = e.getError();
            assertNotNull( err );
            assertEquals( ErrorType.KDC_ERR_C_PRINCIPAL_UNKNOWN, err.getErrorCode() );
        }
    }
    
    
    @Test
    public void testKrbErrPreAuthRequired() throws Exception
    {
        TgtRequest tgtReq = new TgtRequest();
        tgtReq.setClientPrincipal( principalName );
        tgtReq.setPassword( userPassword );
        tgtReq.setPreAuthEnabled( false );

        try
        {
            conn.getTgt( tgtReq );
        }
        catch( KerberosException e )
        {
            KrbError err = e.getError();
            assertNotNull( err );
            assertEquals( ErrorType.KDC_ERR_PREAUTH_REQUIRED, err.getErrorCode() );
            byte[] eData = err.getEData();
            ByteBuffer stream = ByteBuffer.allocate( eData.length );
            stream.put( eData );
            stream.flip();
            
            Asn1Decoder decoder = new Asn1Decoder();
            MethodDataContainer container = new MethodDataContainer();
            container.setStream( stream );
            decoder.decode( stream, container );
            MethodData padata = container.getMethodData();
            assertEquals( 2, padata.getPaDatas().length );
            assertEquals( PaDataType.PA_ENCTYPE_INFO2, padata.getPaDatas()[1].getPaDataType() );
            assertEquals( PaDataType.PA_ENC_TIMESTAMP, padata.getPaDatas()[0].getPaDataType() );
        }
    }

    
    @Test
    public void testKrbErrCantPostdate() throws Exception
    {
        TgtRequest tgtReq = new TgtRequest();
        tgtReq.setClientPrincipal( principalName );
        tgtReq.setPassword( userPassword );
        tgtReq.setStartTime( System.currentTimeMillis() + 600000 ); // now + 10 min
        
        try
        {
            conn.getTgt( tgtReq );
            fail("should fail with KDC_ERR_CANNOT_POSTDATE");
        }
        catch( KerberosException e )
        {
            KrbError err = e.getError();
            assertNotNull( err );
            assertEquals( ErrorType.KDC_ERR_CANNOT_POSTDATE, err.getErrorCode() );
        }
        
        tgtReq.setPostdated( true );
        TgTicket tgt = conn.getTgt( tgtReq );
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
}
