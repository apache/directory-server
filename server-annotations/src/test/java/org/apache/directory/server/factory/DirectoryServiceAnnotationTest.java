/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.factory;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.factory.DSAnnotationProcessor;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.shared.ldap.codec.api.LdapApiServiceFactory;
import org.apache.directory.shared.ldap.model.message.Control;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.util.JndiUtils;
import org.junit.Test;


/**
 * Test the creation of a DS using a factory.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@CreateDS(name = "classDS")
public class DirectoryServiceAnnotationTest
{
    /**
     * Creates a JNDI LdapContext with a connection over the wire using the 
     * SUN LDAP provider.  The connection is made using the administrative 
     * user as the principalDN.  The context is to the rootDSE.
     *
     * @param ldapServer the LDAP server to get the connection to
     * @param controls the controls to use to connect to the ldapServer
     * @return an LdapContext as the administrative user to the RootDSE
     * @throws Exception if there are problems creating the context
     */
    private LdapContext getWiredContext( LdapServer ldapServer, Control[] controls ) throws Exception
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, "ldap://localhost:" + ldapServer.getPort() );
        env.put( Context.SECURITY_PRINCIPAL, ServerDNConstants.ADMIN_SYSTEM_DN );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        return new InitialLdapContext( env, JndiUtils.toJndiControls( LdapApiServiceFactory.getSingleton(), controls ) );
    }


    @Test
    public void testCreateDS() throws Exception
    {
        DirectoryService service = DSAnnotationProcessor.getDirectoryService();

        assertTrue( service.isStarted() );
        assertEquals( "classDS", service.getInstanceId() );

        service.shutdown();
        FileUtils.deleteDirectory( service.getInstanceLayout().getInstanceDirectory() );
    }


    @Test
    @CreateDS(name = "methodDS")
    public void testCreateMethodDS() throws Exception
    {
        DirectoryService service = DSAnnotationProcessor.getDirectoryService();

        assertTrue( service.isStarted() );
        assertEquals( "methodDS", service.getInstanceId() );

        service.shutdown();
        FileUtils.deleteDirectory( service.getInstanceLayout().getInstanceDirectory() );
    }


    @Test
    @CreateDS(
        name = "MethodDSWithPartition",
        partitions =
            {
                @CreatePartition(
                    name = "example",
                    suffix = "dc=example,dc=com",
                    contextEntry = @ContextEntry(
                        entryLdif =
                        "dn: dc=example,dc=com\n" +
                            "dc: example\n" +
                            "objectClass: top\n" +
                            "objectClass: domain\n\n"),
                    indexes =
                        {
                            @CreateIndex(attribute = "objectClass"),
                            @CreateIndex(attribute = "dc"),
                            @CreateIndex(attribute = "ou")
                    })
        })
    public void testCreateMethodDSWithPartition() throws Exception
    {
        DirectoryService service = DSAnnotationProcessor.getDirectoryService();

        assertTrue( service.isStarted() );
        assertEquals( "MethodDSWithPartition", service.getInstanceId() );

        Set<String> expectedNames = new HashSet<String>();

        expectedNames.add( "example" );
        expectedNames.add( "schema" );

        assertEquals( 2, service.getPartitions().size() );

        for ( Partition partition : service.getPartitions() )
        {
            assertTrue( expectedNames.contains( partition.getId() ) );

            if ( "example".equalsIgnoreCase( partition.getId() ) )
            {
                assertTrue( partition.isInitialized() );
                assertEquals( "dc=example,dc=com", partition.getSuffixDn().getName() );
            }
            else if ( "schema".equalsIgnoreCase( partition.getId() ) )
            {
                assertTrue( partition.isInitialized() );
                assertEquals( "ou=schema", partition.getSuffixDn().getName() );
            }
        }

        assertTrue( service.getAdminSession().exists( new Dn( "dc=example,dc=com" ) ) );

        service.shutdown();
        FileUtils.deleteDirectory( service.getInstanceLayout().getInstanceDirectory() );
    }


    @Test
    @CreateDS(
        name = "MethodDSWithPartitionAndServer",
        partitions =
            {
                @CreatePartition(
                    name = "example",
                    suffix = "dc=example,dc=com",
                    contextEntry = @ContextEntry(
                        entryLdif =
                        "dn: dc=example,dc=com\n" +
                            "dc: example\n" +
                            "objectClass: top\n" +
                            "objectClass: domain\n\n"),
                    indexes =
                        {
                            @CreateIndex(attribute = "objectClass"),
                            @CreateIndex(attribute = "dc"),
                            @CreateIndex(attribute = "ou")
                    })
        })
    @CreateLdapServer(
        transports =
            {
                @CreateTransport(protocol = "LDAP"),
                @CreateTransport(protocol = "LDAPS")
        })
    public void testCreateLdapServer() throws Exception
    {
        // First, get the service
        DirectoryService service = DSAnnotationProcessor.getDirectoryService();

        // Check that the service is running
        assertTrue( service.isStarted() );
        assertEquals( "MethodDSWithPartitionAndServer", service.getInstanceId() );

        Set<String> expectedNames = new HashSet<String>();

        expectedNames.add( "example" );
        expectedNames.add( "schema" );

        assertEquals( 2, service.getPartitions().size() );

        for ( Partition partition : service.getPartitions() )
        {
            assertTrue( expectedNames.contains( partition.getId() ) );

            if ( "example".equalsIgnoreCase( partition.getId() ) )
            {
                assertTrue( partition.isInitialized() );
                assertEquals( "dc=example,dc=com", partition.getSuffixDn().getName() );
            }
            else if ( "schema".equalsIgnoreCase( partition.getId() ) )
            {
                assertTrue( partition.isInitialized() );
                assertEquals( "ou=schema", partition.getSuffixDn().getName() );
            }
        }

        assertTrue( service.getAdminSession().exists( new Dn( "dc=example,dc=com" ) ) );

        // Now, get the server
        LdapServer ldapServer = ServerAnnotationProcessor.getLdapServer( service );

        // Check that the server is running
        assertTrue( ldapServer.isStarted() );

        // Try to read an entry in the server
        LdapContext ctx = ( LdapContext ) getWiredContext( ldapServer, null ).lookup( "dc=example,dc=com" );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        controls.setReturningAttributes( new String[]
            { "*" } );

        NamingEnumeration<SearchResult> enumeration = ctx.search( "", "(objectClass=*)", controls );

        // collect all results 
        HashSet<String> results = new HashSet<String>();

        while ( enumeration.hasMore() )
        {
            SearchResult result = enumeration.next();
            results.add( result.getNameInNamespace() );
        }

        assertEquals( 1, results.size() );
        assertTrue( results.contains( "dc=example,dc=com" ) );

        enumeration.close();
        ctx.close();
        ldapServer.stop();
        service.shutdown();

        FileUtils.deleteDirectory( service.getInstanceLayout().getInstanceDirectory() );
    }
}
