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

package org.apache.directory.server.core.factory;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.directory.api.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateAuthenticator;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.annotations.LoadSchema;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.core.api.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.authn.AbstractAuthenticator;
import org.apache.directory.server.core.authn.AuthenticationInterceptor;
import org.apache.directory.server.core.authn.Authenticator;
import org.apache.directory.server.core.partition.impl.avl.AvlPartition;
import org.junit.Test;


/**
 * Test the creation of a DS using a factory.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@CreateDS(name = "classDS")
public class DirectoryServiceAnnotationTest
{
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
        },
        loadedSchemas =
            {
                @LoadSchema(name = "nis", enabled = true),
                @LoadSchema(name = "posix", enabled = false)
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

        assertTrue( service.getSchemaManager().isEnabled( "nis" ) );

        service.shutdown();
        FileUtils.deleteDirectory( service.getInstanceLayout().getInstanceDirectory() );
    }


    @Test
    @CreateDS(
        name = "MethodDSWithAvlPartition",
        partitions =
            {
                @CreatePartition(
                    type = AvlPartition.class,
                    name = "example",
                    suffix = "dc=example,dc=com")
        })
    public void testCreateMethodDSWithAvlPartition() throws Exception
    {
        DirectoryService service = DSAnnotationProcessor.getDirectoryService();

        assertTrue( service.isStarted() );
        assertEquals( "MethodDSWithAvlPartition", service.getInstanceId() );

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
                assertTrue( partition instanceof AvlPartition );
            }
            else if ( "schema".equalsIgnoreCase( partition.getId() ) )
            {
                assertTrue( partition.isInitialized() );
                assertEquals( "ou=schema", partition.getSuffixDn().getName() );
            }
        }

        service.shutdown();
        FileUtils.deleteDirectory( service.getInstanceLayout().getInstanceDirectory() );
    }


    @Test
    @CreateDS(
        name = "MethodDSWithAuthenticator",
        authenticators =
            { @CreateAuthenticator(type = DummyAuthenticator.class) })
    public void testCustomAuthenticator() throws Exception
    {
        final DirectoryService service = DSAnnotationProcessor.getDirectoryService();
        assertTrue( service.isStarted() );
        assertEquals( "MethodDSWithAuthenticator", service.getInstanceId() );
        final Set<Authenticator> authenticators = findAuthInterceptor( service ).getAuthenticators();
        assertEquals(
            "Expected interceptor to be configured with only one authenticator",
            1,
            authenticators.size() );
        assertEquals(
            "Expected the only interceptor to be the dummy interceptor",
            DummyAuthenticator.class,
            authenticators.iterator().next().getClass() );
        service.getSession( new Dn( "uid=non-existant-user,ou=system" ), "wrong-password".getBytes() );
        assertTrue( "Expedted dummy authenticator to have been invoked", dummyAuthenticatorCalled );
        service.shutdown();
        FileUtils.deleteDirectory( service.getInstanceLayout().getInstanceDirectory() );
    }

    private static volatile boolean dummyAuthenticatorCalled = false;

    private static class DummyAuthenticator extends AbstractAuthenticator
    {
        protected DummyAuthenticator()
        {
            super( AuthenticationLevel.SIMPLE, Dn.ROOT_DSE );
        }


        @Override
        public LdapPrincipal authenticate( BindOperationContext ctx ) throws Exception
        {
            dummyAuthenticatorCalled = true;
            return new LdapPrincipal(
                super.getDirectoryService().getSchemaManager(),
                ctx.getDn(),
                AuthenticationLevel.SIMPLE );
        }
    }


    private static AuthenticationInterceptor findAuthInterceptor( DirectoryService service )
    {
        for ( Interceptor interceptor : service.getInterceptors() )
        {
            if ( interceptor instanceof AuthenticationInterceptor )
            {
                return ( AuthenticationInterceptor ) interceptor;
            }
        }
        return null;
    }
}
