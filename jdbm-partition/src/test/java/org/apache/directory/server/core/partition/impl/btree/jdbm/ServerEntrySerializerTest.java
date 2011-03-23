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
package org.apache.directory.server.core.partition.impl.btree.jdbm;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.DefaultEntryAttribute;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.EntryAttribute;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.model.schema.normalizers.DeepTrimToLowerNormalizer;
import org.apache.directory.shared.ldap.model.schema.normalizers.OidNormalizer;
import org.apache.directory.shared.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.shared.util.Strings;
import org.apache.directory.shared.util.exception.Exceptions;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;


/**
 * Test the Entry serialization/deserialization class
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class ServerEntrySerializerTest
{
    private static LdifSchemaLoader loader;
    private static SchemaManager schemaManager;
    private static Map<String, OidNormalizer> oids;
    private static Map<String, OidNormalizer> oidOids;


    /**
     * Initialize the registries once for the whole test suite
     */
    @BeforeClass
    public static void setup() throws Exception
    {
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = ServerEntrySerializerTest.class.getResource( "" ).getPath();
            int targetPos = path.indexOf( "target" );
            workingDirectory = path.substring( 0, targetPos + 6 );
        }

        File schemaRepository = new File( workingDirectory, "schema" );
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy( true );
        loader = new LdifSchemaLoader( schemaRepository );

        schemaManager = new DefaultSchemaManager( loader );
        schemaManager.loadAllEnabled();

        List<Throwable> errors = schemaManager.getErrors();

        if ( errors.size() != 0 )
        {
            fail( "Schema load failed : " + Exceptions.printErrors(errors) );
        }

        oids = new HashMap<String, OidNormalizer>();

        // DC normalizer
        OidNormalizer dcOidNormalizer = new OidNormalizer( "dc", new DeepTrimToLowerNormalizer(
            SchemaConstants.DOMAIN_COMPONENT_AT_OID ) );

        oids.put( "dc", dcOidNormalizer );
        oids.put( "domaincomponent", dcOidNormalizer );
        oids.put( "0.9.2342.19200300.100.1.25", dcOidNormalizer );

        // OU normalizer
        OidNormalizer ouOidNormalizer = new OidNormalizer( "ou", new DeepTrimToLowerNormalizer(
            SchemaConstants.OU_AT_OID ) );

        oids.put( "ou", ouOidNormalizer );
        oids.put( "organizationalUnitName", ouOidNormalizer );
        oids.put( "2.5.4.11", ouOidNormalizer );

        // Another map where we store OIDs instead of names.
        oidOids = new HashMap<String, OidNormalizer>();

        oidOids.put( "dc", dcOidNormalizer );
        oidOids.put( "domaincomponent", dcOidNormalizer );
        oidOids.put( "0.9.2342.19200300.100.1.25", dcOidNormalizer );

        oidOids.put( "ou", ouOidNormalizer );
        oidOids.put( "organizationalUnitName", ouOidNormalizer );
        oidOids.put( "2.5.4.11", ouOidNormalizer );
    }


    @Test
    public void testSerializeEmtpyServerEntry() throws Exception
    {
        Dn dn = Dn.EMPTY_DN;
        Entry entry = new DefaultEntry( schemaManager, dn );

        ServerEntrySerializer ses = new ServerEntrySerializer( schemaManager );

        byte[] data = ses.serialize( entry );

        Entry result = ( Entry ) ses.deserialize( data );

        assertEquals( entry, result );
    }


    @Test
    public void testSerializeDNServerEntry() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=text, dc=example, dc=com" );

        Entry entry = new DefaultEntry( schemaManager, dn );

        ServerEntrySerializer ses = new ServerEntrySerializer( schemaManager );

        byte[] data = ses.serialize( entry );

        Entry result = ( Entry ) ses.deserialize( data );

        Dn newDn = new Dn( dn.getRdn() );
        entry.setDn( newDn );

        assertEquals( entry, result );
    }


    @Test
    public void testSerializeServerEntryOC() throws Exception
    {
        Dn dn = new Dn( "cn=text, dc=example, dc=com" );
        dn.applySchemaManager( schemaManager );

        Entry entry = new DefaultEntry( schemaManager, dn );
        entry.add( "objectClass", "top", "person", "inetOrgPerson", "organizationalPerson" );

        ServerEntrySerializer ses = new ServerEntrySerializer( schemaManager );

        byte[] data = ses.serialize( entry );

        Entry result = ( Entry ) ses.deserialize( data );

        Dn newDn = new Dn();
        newDn = newDn.add( dn.getRdn() );
        entry.setDn( newDn );

        assertEquals( entry, result );
    }


    @Test
    public void testSerializeServerEntry() throws Exception
    {
        Dn dn = new Dn( "cn=text, dc=example, dc=com" );
        dn.applySchemaManager( schemaManager );

        Entry entry = new DefaultEntry( schemaManager, dn );
        entry.add( "objectClass", "top", "person", "inetOrgPerson", "organizationalPerson" );
        entry.add( "cn", "text", "test" );
        entry.add( "SN", ( String ) null );
        entry.add( "userPassword", Strings.getBytesUtf8("password") );

        ServerEntrySerializer ses = new ServerEntrySerializer( schemaManager );

        byte[] data = ses.serialize( entry );

        Entry result = ( Entry ) ses.deserialize( data );

        Dn newDn = new Dn();
        newDn = newDn.add( dn.getRdn() );
        entry.setDn( newDn );

        assertEquals( entry, result );
    }


    @Test
    public void testSerializeServerEntryWithEmptyDN() throws Exception
    {
        Dn dn = new Dn( "" );
        dn.applySchemaManager( schemaManager );

        Entry entry = new DefaultEntry( schemaManager, dn );
        entry.add( "objectClass", "top", "person", "inetOrgPerson", "organizationalPerson" );
        entry.add( "cn", "text", "test" );
        entry.add( "SN", ( String ) null );
        entry.add( "userPassword", Strings.getBytesUtf8("password") );

        ServerEntrySerializer ses = new ServerEntrySerializer( schemaManager );

        byte[] data = ses.serialize( entry );

        Entry result = ( Entry ) ses.deserialize( data );

        assertEquals( entry, result );
    }


    @Test
    public void testSerializeServerEntryWithNoAttributes() throws Exception
    {
        Dn dn = new Dn( "" );
        dn.applySchemaManager( schemaManager );

        Entry entry = new DefaultEntry( schemaManager, dn );

        ServerEntrySerializer ses = new ServerEntrySerializer( schemaManager );

        byte[] data = ses.serialize( entry );

        Entry result = ( Entry ) ses.deserialize( data );

        assertEquals( entry, result );
    }


    @Test
    public void testSerializeServerEntryWithAttributeNoValue() throws Exception
    {
        Dn dn = new Dn( "" );
        dn.applySchemaManager( schemaManager );

        Entry entry = new DefaultEntry( schemaManager, dn );

        ServerEntrySerializer ses = new ServerEntrySerializer( schemaManager );
        EntryAttribute oc = new DefaultEntryAttribute( "ObjectClass", schemaManager
            .lookupAttributeTypeRegistry( "objectclass" ) );
        entry.add( oc );

        byte[] data = ses.serialize( entry );

        Entry result = ( Entry ) ses.deserialize( data );

        assertEquals( entry, result );
    }


    @Test
    public void testSerializeServerEntryWithAttributeStringValue() throws Exception
    {
        Dn dn = new Dn( "" );
        dn.applySchemaManager( schemaManager );

        Entry entry = new DefaultEntry( schemaManager, dn );

        ServerEntrySerializer ses = new ServerEntrySerializer( schemaManager );
        entry.add( "ObjectClass", "top", "person" );

        byte[] data = ses.serialize( entry );

        Entry result = ( Entry ) ses.deserialize( data );

        assertEquals( entry, result );
    }


    @Test
    public void testSerializeServerEntryWithAttributeBinaryValue() throws Exception
    {
        Dn dn = new Dn( "" );
        dn.applySchemaManager( schemaManager );

        Entry entry = new DefaultEntry( schemaManager, dn );

        ServerEntrySerializer ses = new ServerEntrySerializer( schemaManager );
        entry.add( "userPassword", Strings.getBytesUtf8("secret") );

        byte[] data = ses.serialize( entry );

        Entry result = ( Entry ) ses.deserialize( data );

        assertEquals( entry, result );
    }
}
