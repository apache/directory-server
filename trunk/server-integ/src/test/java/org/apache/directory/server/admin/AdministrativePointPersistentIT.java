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
package org.apache.directory.server.admin;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.util.tree.DnNode;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.api.administrative.AccessControlAdministrativePoint;
import org.apache.directory.server.core.api.administrative.AdministrativePoint;
import org.apache.directory.server.core.api.administrative.CollectiveAttributeAdministrativePoint;
import org.apache.directory.server.core.api.administrative.SubschemaAdministrativePoint;
import org.apache.directory.server.core.api.administrative.TriggerExecutionAdministrativePoint;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test cases for the AdministrativePoint interceptor, checking that the cache is correctly updated
 * when the server is started.
 * 
 * We will create the following data structure :
 * <pre>
 * ou=system
 *  |
 *  +-ou=noAP1
 *  |  |
 *  |  +-<ou=AAP1>
 *  |     |
 *  |     +-ou=noAP2
 *  +-<ou=AAP2>
 *     |
 *     +-ou=noAP3
 *        |
 *        +-<ou=subAAP1>
 *           |
 *           +-ou=noAP4
 * </pre>
 * 
 * and check that it's present when the server is stopped and restarted
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(
    name = "TestDS",
    enableAccessControl = true)
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP") })
@ApplyLdifs(
    {
        // Entry # 1
        "dn: ou=noAP1,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: noAP1",
        "",
        // Entry # 2
        "dn: ou=AAP1,ou=noAP1,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: AAP1",
        "administrativeRole: autonomousArea",
        "",
        // Entry # 3
        "dn: ou=noAP2,ou=AAP1,ou=noAP1,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: noAP2",
        "",
        // Entry # 4
        "dn: ou=AAP2,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: AAP2",
        "administrativeRole: autonomousArea",
        "",
        // Entry # 5
        "dn: ou=noAP3,ou=AAP2,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: noAP3",
        "",
        // Entry # 6
        "dn: ou=subAAP1,ou=noAP3,ou=AAP2,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: subAAP1",
        "administrativeRole: autonomousArea",
        "",
        // Entry # 7
        "dn: ou=noAP4,ou=subAAP1,ou=noAP3,ou=AAP2,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: noAP4",
        ""
})
public class AdministrativePointPersistentIT extends AbstractLdapTestUnit
{
// The shared LDAP connection
private static LdapConnection connection;

// A reference to the schema manager
private static SchemaManager schemaManager;


@Before
public void init() throws Exception
{
    getService().setAccessControlEnabled( true );
    connection = IntegrationUtils.getAdminConnection( getService() );
    schemaManager = getService().getSchemaManager();
}


@After
public void shutdown() throws Exception
{
    connection.close();
}


private Attribute getAdminRole( String dn ) throws Exception
{
    Entry lookup = connection.lookup( dn, "administrativeRole" );

    assertNotNull( lookup );

    return lookup.get( "administrativeRole" );
}


// -------------------------------------------------------------------
// Test the Add operation
// -------------------------------------------------------------------
/**
 * Test the persistence of autonomous areas across a server stop and start
 */
@Test
public void testPersistAutonomousArea() throws Exception
{
    assertTrue( getLdapServer().isStarted() );

    // Check the caches
    DnNode<AccessControlAdministrativePoint> acCache = getLdapServer().getDirectoryService().getAccessControlAPCache();
    AdministrativePoint aap1 = acCache.getElement( new Dn( schemaManager, "ou=AAP1,ou=noAP1,ou=system" ) );
    assertNotNull( aap1 );

    // Stop the server now, we will restart it immediately 
    getLdapServer().stop();
    assertFalse( getLdapServer().isStarted() );

    // And shutdown the DS too
    getLdapServer().getDirectoryService().shutdown();
    assertFalse( getLdapServer().getDirectoryService().isStarted() );

    // And restart
    getLdapServer().getDirectoryService().startup();
    getLdapServer().start();
    schemaManager = getLdapServer().getDirectoryService().getSchemaManager();

    assertTrue( getService().isStarted() );
    assertTrue( getLdapServer().getDirectoryService().isStarted() );

    // Check that the roles are present
    assertEquals( "autonomousArea", getAdminRole( "ou=AAP1,ou=noAP1,ou=system" ).getString() );
    assertEquals( "autonomousArea", getAdminRole( "ou=AAP2,ou=system" ).getString() );
    assertEquals( "autonomousArea", getAdminRole( "ou=subAAP1,ou=noAP3,ou=AAP2,ou=system" ).getString() );

    // Check the caches
    acCache = getLdapServer().getDirectoryService().getAccessControlAPCache();
    DnNode<CollectiveAttributeAdministrativePoint> caCache = getLdapServer().getDirectoryService()
        .getCollectiveAttributeAPCache();
    DnNode<TriggerExecutionAdministrativePoint> teCache = getLdapServer().getDirectoryService()
        .getTriggerExecutionAPCache();
    DnNode<SubschemaAdministrativePoint> ssCache = getLdapServer().getDirectoryService().getSubschemaAPCache();

    // The ACs
    aap1 = acCache.getElement( new Dn( schemaManager, "ou=AAP1,ou=noAP1,ou=system" ) );
    assertNotNull( aap1 );

    AdministrativePoint aap2 = acCache.getElement( new Dn( schemaManager, "ou=AAP2,ou=system" ) );
    assertNotNull( aap2 );

    AdministrativePoint subAap1 = acCache.getElement( new Dn( schemaManager, "ou=subAAP1,ou=noAP3,ou=AAP2,ou=system" ) );
    assertNotNull( subAap1 );

    // The ACs
    aap1 = caCache.getElement( new Dn( schemaManager, "ou=AAP1,ou=noAP1,ou=system" ) );
    assertNotNull( aap1 );

    aap2 = caCache.getElement( new Dn( schemaManager, "ou=AAP2,ou=system" ) );
    assertNotNull( aap2 );

    subAap1 = caCache.getElement( new Dn( schemaManager, "ou=subAAP1,ou=noAP3,ou=AAP2,ou=system" ) );
    assertNotNull( subAap1 );

    // The TEs
    aap1 = teCache.getElement( new Dn( schemaManager, "ou=AAP1,ou=noAP1,ou=system" ) );
    assertNotNull( aap1 );

    aap2 = teCache.getElement( new Dn( schemaManager, "ou=AAP2,ou=system" ) );
    assertNotNull( aap2 );

    subAap1 = teCache.getElement( new Dn( schemaManager, "ou=subAAP1,ou=noAP3,ou=AAP2,ou=system" ) );
    assertNotNull( subAap1 );

    // The SSs
    aap1 = ssCache.getElement( new Dn( schemaManager, "ou=AAP1,ou=noAP1,ou=system" ) );
    assertNotNull( aap1 );

    aap2 = ssCache.getElement( new Dn( schemaManager, "ou=AAP2,ou=system" ) );
    assertNotNull( aap2 );

    subAap1 = ssCache.getElement( new Dn( schemaManager, "ou=subAAP1,ou=noAP3,ou=AAP2,ou=system" ) );
    assertNotNull( subAap1 );
}
}
