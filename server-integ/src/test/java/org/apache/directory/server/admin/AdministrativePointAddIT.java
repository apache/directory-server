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


import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapUnwillingToPerformException;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test cases for the AdministrativePoint Addition operation
 * 
 * We will create the following data structure :
 * <pre>
 * ou=system
 *  |
 *  +-ou=AAP1
 *  |  |
 *  |  +-ou=IAP-CA1
 *  |  |
 *  |  +-ou=IAP-AC1
 *  |  |
 *  |  +-ou=SAP-CA1
 *  |  |
 *  |  +-ou=SAP-AC1
 *  | 
 *  +-ou=SAP-CA2
 *  |
 *  +-ou=SAP-AC2
 *  |
 *  +-ou=entry
 * </pre>
 * 
 * and check that it's present when the server is stopped and restarted
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP") })
@ApplyLdifs(
    {
        // Entry # 1
        "dn: ou=AAP1,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: AAP1",
        "administrativeRole: autonomousArea",
        "",
        // Entry # 2
        "dn: ou=IAP-CA1,ou=AAP1,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: IAP-CA1",
        "administrativeRole: collectiveAttributeInnerArea",
        "",
        // Entry # 3
        "dn: ou=IAP-AC1,ou=AAP1,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: IAP-AC1",
        "administrativeRole: accessControlInnerArea",
        "",
        // Entry # 4
        "dn: ou=SAP-CA1,ou=AAP1,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: SAP-CA1",
        "administrativeRole: collectiveAttributeSpecificArea",
        "",
        // Entry # 5
        "dn: ou=SAP-AC1,ou=AAP1,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: SAP-AC1",
        "administrativeRole: accessControlSpecificArea",
        "",
        // Entry # 6
        "dn: ou=SAP-CA2,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: SAP-CA2",
        "administrativeRole: collectiveAttributeSpecificArea",
        "",
        // Entry # 7
        "dn: ou=SAP-AC2,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: SAP-AC2",
        "administrativeRole: accessControlSpecificArea",
        "",
        // Entry # 8
        "dn: ou=entry,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: entry",
        ""
})
public class AdministrativePointAddIT extends AbstractLdapTestUnit
{
// The shared LDAP connection
private static LdapConnection connection;

// A reference to the schema manager
private static SchemaManager schemaManager;


@Before
public void init() throws Exception
{
    connection = IntegrationUtils.getAdminConnection( getService() );
    schemaManager = getLdapServer().getDirectoryService().getSchemaManager();
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
 * Test the addition of IAPs
 */
@Test
public void testAddIAP() throws Exception
{
    assertTrue( getLdapServer().isStarted() );

    // First check that we can't add an IAP in the DIT if there is no
    // parent AAP or SAP with the same role
    Entry entry = new DefaultEntry(
        "ou=IAP-CANew,ou=entry,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: IAP-CANew",
        "administrativeRole: collectiveAttributeInnerArea"
        );

    try
    {
        connection.add( entry );
        fail();
    }
    catch ( LdapUnwillingToPerformException lutpe )
    {
        assertTrue( true );
    }

    // Add the entry under a SAP with the same role which has no parent AAP
    entry = new DefaultEntry(
        "ou=IAP-CANew,ou=SAP-CA2,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: IAP-CANew",
        "administrativeRole: collectiveAttributeInnerArea"
        );

    connection.add( entry );

    // It should succeed
    assertTrue( connection.exists( "ou=IAP-CANew,ou=SAP-CA2,ou=system" ) );

    // Add the entry under a SAP with a different role which has no parent AAP
    entry = new DefaultEntry(
        "ou=IAP-CANew,ou=SAP-AC2,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: IAP-CANew",
        "administrativeRole: collectiveAttributeInnerArea"
        );

    try
    {
        connection.add( entry );
        fail();
    }
    catch ( LdapUnwillingToPerformException lutpe )
    {
        assertTrue( true );
    }

    // Add the entry under an AAP
    entry = new DefaultEntry(
        "ou=IAP-CANew,ou=AAP1,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: IAP-CANew",
        "administrativeRole: collectiveAttributeInnerArea"
        );

    connection.add( entry );

    // It should succeed
    assertTrue( connection.exists( "ou=IAP-CANew,ou=AAP1,ou=system" ) );

    // Add the entry under an IAP with the same role which has a parent AAP
    entry = new DefaultEntry(
        "ou=IAP-CANew,ou=IAP-CA1,ou=AAP1,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: IAP-CANew",
        "administrativeRole: collectiveAttributeInnerArea"
        );

    connection.add( entry );

    // It should succeed
    assertTrue( connection.exists( "ou=IAP-CANew,ou=IAP-CA1,ou=AAP1,ou=system" ) );

    // Add the entry under an IAP with a different role which has a parent AAP
    entry = new DefaultEntry(
        "ou=IAP-CANew,ou=IAP-AC1,ou=AAP1,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: IAP-CANew",
        "administrativeRole: collectiveAttributeInnerArea"
        );

    connection.add( entry );

    // It should succeed
    assertTrue( connection.exists( "ou=IAP-CANew,ou=IAP-AC1,ou=AAP1,ou=system" ) );

    // Add the entry under an SAP with the same role which has a parent AAP
    entry = new DefaultEntry(
        "ou=IAP-CANew,ou=SAP-CA1,ou=AAP1,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: IAP-CANew",
        "administrativeRole: collectiveAttributeInnerArea"
        );

    connection.add( entry );

    // It should succeed
    assertTrue( connection.exists( "ou=IAP-CANew,ou=SAP-CA1,ou=AAP1,ou=system" ) );

    // Add the entry under an SAP with a different role which has a parent AAP
    entry = new DefaultEntry(
        "ou=IAP-CANew,ou=SAP-AC1,ou=AAP1,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: IAP-CANew",
        "administrativeRole: collectiveAttributeInnerArea"
        );

    connection.add( entry );

    // It should succeed
    assertTrue( connection.exists( "ou=IAP-CANew,ou=SAP-AC1,ou=AAP1,ou=system" ) );
}


/**
 * Test the addition of SAPs
 */
@Test
public void testAddSAP() throws Exception
{
    assertTrue( getLdapServer().isStarted() );

    // First check that we can add a SAP in the DIT if there is no
    // parent AAP or SAP
    Entry entry = new DefaultEntry(
        "ou=SAP-CANew,ou=entry,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: SAP-CANew",
        "administrativeRole: collectiveAttributeSpecificArea"
        );

    connection.add( entry );

    // It should succeed
    assertTrue( connection.exists( "ou=SAP-CANew,ou=entry,ou=system" ) );

    // Add the entry under a SAP with the same role which has no parent AAP
    entry = new DefaultEntry(
        "ou=SAP-CANew,ou=SAP-CA2,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: SAP-CANew",
        "administrativeRole: collectiveAttributeSpecificArea"
        );

    connection.add( entry );

    // It should succeed
    assertTrue( connection.exists( "ou=SAP-CANew,ou=SAP-CA2,ou=system" ) );

    // Add the entry under a SAP with a different role which has no parent AAP
    entry = new DefaultEntry(
        "ou=SAP-CANew,ou=SAP-AC2,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: SAP-CANew",
        "administrativeRole: collectiveAttributeSpecificArea"
        );

    connection.add( entry );

    // It should succeed
    assertTrue( connection.exists( "ou=SAP-CANew,ou=SAP-AC2,ou=system" ) );

    // Add the entry under an AAP
    entry = new DefaultEntry(
        "ou=SAP-CANew,ou=AAP1,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: SAP-CANew",
        "administrativeRole: collectiveAttributeSpecificArea"
        );

    connection.add( entry );

    // It should succeed
    assertTrue( connection.exists( "ou=SAP-CANew,ou=AAP1,ou=system" ) );

    // Add the entry under an IAP with the same role which has a parent AAP
    entry = new DefaultEntry(
        "ou=SAP-CANew,ou=IAP-CA1,ou=AAP1,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: SAP-CANew",
        "administrativeRole: collectiveAttributeSpecificArea"
        );

    connection.add( entry );

    // It should succeed
    assertTrue( connection.exists( "ou=SAP-CANew,ou=IAP-CA1,ou=AAP1,ou=system" ) );

    // Add the entry under an IAP with a different role which has a parent AAP
    entry = new DefaultEntry(
        "ou=SAP-CANew,ou=IAP-AC1,ou=AAP1,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: SAP-CANew",
        "administrativeRole: collectiveAttributeSpecificArea"
        );

    connection.add( entry );

    // It should succeed
    assertTrue( connection.exists( "ou=SAP-CANew,ou=IAP-AC1,ou=AAP1,ou=system" ) );

    // Add the entry under an SAP with the same role which has a parent AAP
    entry = new DefaultEntry(
        "ou=SAP-CANew,ou=SAP-CA1,ou=AAP1,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: SAP-CANew",
        "administrativeRole: collectiveAttributeSpecificArea"
        );

    connection.add( entry );

    // It should succeed
    assertTrue( connection.exists( "ou=SAP-CANew,ou=SAP-CA1,ou=AAP1,ou=system" ) );

    // Add the entry under an SAP with a different role which has a parent AAP
    entry = new DefaultEntry(
        "ou=SAP-CANew,ou=SAP-AC1,ou=AAP1,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: SAP-CANew",
        "administrativeRole: collectiveAttributeSpecificArea"
        );

    connection.add( entry );

    // It should succeed
    assertTrue( connection.exists( "ou=SAP-CANew,ou=SAP-AC1,ou=AAP1,ou=system" ) );
}
}
