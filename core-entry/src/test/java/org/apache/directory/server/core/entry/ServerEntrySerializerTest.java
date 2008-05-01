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
package org.apache.directory.server.core.entry;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;

import org.apache.directory.server.schema.bootstrap.ApacheSchema;
import org.apache.directory.server.schema.bootstrap.ApachemetaSchema;
import org.apache.directory.server.schema.bootstrap.BootstrapSchemaLoader;
import org.apache.directory.server.schema.bootstrap.CoreSchema;
import org.apache.directory.server.schema.bootstrap.CosineSchema;
import org.apache.directory.server.schema.bootstrap.InetorgpersonSchema;
import org.apache.directory.server.schema.bootstrap.Schema;
import org.apache.directory.server.schema.bootstrap.SystemSchema;
import org.apache.directory.server.schema.registries.DefaultOidRegistry;
import org.apache.directory.server.schema.registries.DefaultRegistries;
import org.apache.directory.server.schema.registries.OidRegistry;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.DeepTrimToLowerNormalizer;
import org.apache.directory.shared.ldap.schema.OidNormalizer;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Test the ServerEntry serialization/deserialization class
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ServerEntrySerializerTest
{
    private static BootstrapSchemaLoader loader;
    private static Registries registries;
    private static OidRegistry oidRegistry;
    private static Map<String, OidNormalizer> oids;
    private static Map<String, OidNormalizer> oidOids;

    /**
     * Initialize the registries once for the whole test suite
     */
    @BeforeClass
    public static void setup() throws NamingException
    {
        loader = new BootstrapSchemaLoader();
        oidRegistry = new DefaultOidRegistry();
        registries = new DefaultRegistries( "bootstrap", loader, oidRegistry );
        
        // load essential bootstrap schemas 
        Set<Schema> bootstrapSchemas = new HashSet<Schema>();
        bootstrapSchemas.add( new ApachemetaSchema() );
        bootstrapSchemas.add( new ApacheSchema() );
        bootstrapSchemas.add( new CoreSchema() );
        bootstrapSchemas.add( new SystemSchema() );
        bootstrapSchemas.add( new InetorgpersonSchema() );
        bootstrapSchemas.add( new CosineSchema() );
        loader.loadWithDependencies( bootstrapSchemas, registries );
        
        oids = new HashMap<String, OidNormalizer>();

        oids.put( "dc", new OidNormalizer( "dc", new DeepTrimToLowerNormalizer() ) );
        oids.put( "domaincomponent", new OidNormalizer( "dc", new DeepTrimToLowerNormalizer() ) );
        oids.put( "0.9.2342.19200300.100.1.25", new OidNormalizer( "dc", new DeepTrimToLowerNormalizer() ) );
        oids.put( "ou", new OidNormalizer( "ou", new DeepTrimToLowerNormalizer() ) );
        oids.put( "organizationalUnitName", new OidNormalizer( "ou", new DeepTrimToLowerNormalizer() ) );
        oids.put( "2.5.4.11", new OidNormalizer( "ou", new DeepTrimToLowerNormalizer() ) );
    
    
        // Another map where we store OIDs instead of names.
        oidOids = new HashMap<String, OidNormalizer>();

        oidOids.put( "dc", new OidNormalizer( "0.9.2342.19200300.100.1.25", new DeepTrimToLowerNormalizer() ) );
        oidOids.put( "domaincomponent", new OidNormalizer( "0.9.2342.19200300.100.1.25", new DeepTrimToLowerNormalizer() ) );
        oidOids.put( "0.9.2342.19200300.100.1.25", new OidNormalizer( "0.9.2342.19200300.100.1.25", new DeepTrimToLowerNormalizer() ) );
        oidOids.put( "ou", new OidNormalizer( "2.5.4.11", new DeepTrimToLowerNormalizer() ) );
        oidOids.put( "organizationalUnitName", new OidNormalizer( "2.5.4.11", new DeepTrimToLowerNormalizer() ) );
        oidOids.put( "2.5.4.11", new OidNormalizer( "2.5.4.11", new DeepTrimToLowerNormalizer() ) );
    }

    
    @Test public void testSerializeEmtpyServerEntry() throws IOException, NamingException, ClassNotFoundException
    {
        LdapDN dn = LdapDN.EMPTY_LDAPDN;
        ServerEntry entry = new DefaultServerEntry( registries, dn );

        ServerEntrySerializer ses = new ServerEntrySerializer( registries );
        
        byte[] data = ses.serialize( entry );
        
        ServerEntry result = (ServerEntry)ses.deserialize( data );
        
        assertEquals( entry, result );
    }

    @Test public void testSerializeDNServerEntry() throws IOException, NamingException, ClassNotFoundException
    {
        LdapDN dn = new LdapDN( "cn=text, dc=example, dc=com" );
        dn.normalize( oids );
        
        ServerEntry entry = new DefaultServerEntry( registries, dn );

        ServerEntrySerializer ses = new ServerEntrySerializer( registries );
        
        byte[] data = ses.serialize( entry );
        
        ServerEntry result = (ServerEntry)ses.deserialize( data );
        
        assertEquals( entry, result );
    }


    @Test public void testSerializeServerEntryOC() throws IOException, NamingException, ClassNotFoundException
    {
        LdapDN dn = new LdapDN( "cn=text, dc=example, dc=com" );
        dn.normalize( oids );
        
        ServerEntry entry = new DefaultServerEntry( registries, dn );
        entry.add( "objectClass", "top", "person", "inetOrgPerson", "organizationalPerson" );

        ServerEntrySerializer ses = new ServerEntrySerializer( registries );

        byte[] data = ses.serialize( entry );
        
        ServerEntry result = (ServerEntry)ses.deserialize( data );
        
        assertEquals( entry, result );
    }


    @Test public void testSerializeServerEntry() throws IOException, NamingException, ClassNotFoundException
    {
        LdapDN dn = new LdapDN( "cn=text, dc=example, dc=com" );
        dn.normalize( oids );
        
        ServerEntry entry = new DefaultServerEntry( registries, dn );
        entry.add( "objectClass", "top", "person", "inetOrgPerson", "organizationalPerson" );
        entry.add( "cn", "text", "test" );
        entry.add( "SN", (String)null );
        entry.add( "userPassword", StringTools.getBytesUtf8( "password" ) );

        ServerEntrySerializer ses = new ServerEntrySerializer( registries );
        
        byte[] data = ses.serialize( entry );
        
        ServerEntry result = (ServerEntry)ses.deserialize( data );
        
        assertEquals( entry, result );
    }
}
