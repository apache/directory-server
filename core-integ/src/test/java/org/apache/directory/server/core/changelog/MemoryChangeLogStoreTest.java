/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.changelog;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.directory.api.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.ldif.ChangeType;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifRevertor;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.schema.extractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.DateUtils;
import org.apache.directory.api.util.Strings;
import org.apache.directory.api.util.TimeProvider;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.changelog.ChangeLogEvent;
import org.apache.directory.server.core.api.changelog.ChangeLogEventSerializer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Tests the MemoryChangeLogStore.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MemoryChangeLogStoreTest
{
    private static MemoryChangeLogStore store;

    private static SchemaManager schemaManager;


    @BeforeClass
    public static void setUp() throws Exception
    {
        // setup working directory
        DirectoryService directoryService = new DefaultDirectoryService();
        String tmpDirPath = System.getProperty( "workingDirectory", System.getProperty( "java.io.tmpdir" ) );
        File workingDirectory = new File( tmpDirPath + "/server-work-"
            + MemoryChangeLogStoreTest.class.getSimpleName() );
        InstanceLayout instanceLayout = new InstanceLayout( workingDirectory );
        directoryService.setInstanceLayout( instanceLayout );

        if ( !workingDirectory.exists() )
        {
            workingDirectory.mkdirs();
        }

        directoryService.getInstanceLayout().setPartitionsDir( workingDirectory );

        // --------------------------------------------------------------------
        // Load the bootstrap schemas to start up the schema partition
        // --------------------------------------------------------------------

        File schemaRepository = new File( workingDirectory, "schema" );
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( workingDirectory );
        extractor.extractOrCopy( true );
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        schemaManager = new DefaultSchemaManager( loader );

        boolean loaded = schemaManager.loadAllEnabled();

        if ( !loaded )
        {
            fail( "Schema load failed : " + Exceptions.printErrors( schemaManager.getErrors() ) );
        }

        directoryService.setSchemaManager( schemaManager );

        schemaManager = new DefaultSchemaManager();

        store = new MemoryChangeLogStore();
        
        store.init( directoryService );
    }


    @AfterClass
    public static void tearDown() throws Exception
    {
        store = null;
    }


    @Test
    public void testLogCheckRevision() throws Exception
    {
        assertEquals( 0, store.getCurrentRevision(), "first revision is always 0" );

        LdifEntry forward = new LdifEntry();
        forward.setDn( "ou=system" );
        forward.setChangeType( ChangeType.Add );
        forward.putAttribute( "objectClass", "organizationalUnit" );
        forward.putAttribute( "ou", "system" );

        LdifEntry reverse = LdifRevertor.reverseAdd( forward.getDn() );
        assertEquals( 1, store.log( new LdapPrincipal( schemaManager ), forward, reverse ).getRevision() );
        assertEquals( 1, store.getCurrentRevision() );
    }


    @Test
    public void testChangeLogSerialization() throws LdapException, IOException, ClassNotFoundException
    {
        Dn systemDn = new Dn( schemaManager, "ou=system" );

        Dn adminDn = new Dn( schemaManager, "uid=admin, ou=system" );

        LdifEntry forward = new LdifEntry();
        forward.setDn( systemDn );
        forward.setChangeType( ChangeType.Add );
        forward.putAttribute( "objectClass", "organizationalUnit" );
        forward.putAttribute( "ou", "system" );

        Dn reverseDn = forward.getDn();

        LdifEntry reverse = LdifRevertor.reverseAdd( reverseDn );

        String zuluTime = DateUtils.getGeneralizedTime( TimeProvider.DEFAULT );
        long revision = 1L;

        LdapPrincipal principal = new LdapPrincipal( schemaManager, adminDn, AuthenticationLevel.SIMPLE,
            Strings.getBytesUtf8( "secret" ) );
        ChangeLogEvent event = new ChangeLogEvent( revision, zuluTime, principal, forward, reverse );

        byte[] data = null;
        try ( ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream( baos ) )
        {
            ChangeLogEventSerializer.serialize( event, out );
            data = baos.toByteArray();
        }

        ObjectInputStream in = new ObjectInputStream( new ByteArrayInputStream( data ) );

        ChangeLogEvent read = ChangeLogEventSerializer.deserialize( schemaManager, in );

        // The read event should not be equal to the written event, as
        // the principal's password has not been stored
        assertNotSame( event, read );

        LdapPrincipal readPrincipal = read.getCommitterPrincipal();

        assertEquals( principal.getAuthenticationLevel(), readPrincipal.getAuthenticationLevel() );
        assertEquals( principal.getName(), readPrincipal.getName() );
        assertEquals( principal.getDn(), readPrincipal.getDn() );
        assertNull( readPrincipal.getUserPasswords() );

        assertEquals( zuluTime, read.getZuluTime() );
        assertEquals( revision, read.getRevision() );
        assertEquals( forward, read.getForwardLdif() );
        assertEquals( reverse, read.getReverseLdifs().get( 0 ) );
    }
}
