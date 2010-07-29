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
package org.apache.directory.server.core.admin;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.message.AddResponse;
import org.apache.directory.ldap.client.api.message.SearchResponse;
import org.apache.directory.ldap.client.api.message.SearchResultEntry;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.ldif.LdifUtils;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test cases for the AdministrativePoint interceptor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith ( FrameworkRunner.class )
public class AdministrativePointServiceIT extends AbstractLdapTestUnit
{
    // The shared LDAP connection
    private static LdapConnection connection;


    @Before
    public void init() throws Exception
    {
        connection = IntegrationUtils.getAdminConnection( service );
    }


    @After
    public void shutdown() throws Exception
    {
        connection.close();
    }


    /**
     * Test the addition of an autonomous area
     * @throws Exception
     */
    @Test
    public void testAddAutonomousArea() throws Exception
    {
        // -------------------------------------------------------------------
        // Inject an AA alone
        // -------------------------------------------------------------------
        Entry autonomousArea = LdifUtils.createEntry(
            "ou=autonomousArea, ou=system",
            "ObjectClass: top",
            "ObjectClass: organizationalUnit",
            "ou: autonomousArea",
            "administrativeRole: autonomousArea"
            );

        // It should fail, as we haven't injected all the roles
        AddResponse response = connection.add( autonomousArea );

        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );

        // Check that the entry is containing all the roles
        SearchResponse lookup = connection.lookup( "ou=autonomousArea, ou=system", "administrativeRole" );

        assertTrue( lookup instanceof SearchResultEntry );

        Entry result = ((SearchResultEntry)lookup).getEntry();

        assertTrue( result.contains( "administrativeRole", "autonomousArea" ) );
        assertFalse( result.contains( "administrativeRole", "accessControlSpecificArea" ) );
        assertFalse( result.contains( "administrativeRole", "collectiveAttributeSpecificArea" ) );
        assertFalse( result.contains( "administrativeRole", "2.5.23.4" ) );
        assertFalse( result.contains( "administrativeRole", "triggerExecutionSpecificArea" ) );

        // -------------------------------------------------------------------
        // Inject a AA with specific A
        // -------------------------------------------------------------------
        autonomousArea = LdifUtils.createEntry(
            "ou=autonomousArea2, ou=system",
            "ObjectClass: top",
            "ObjectClass: organizationalUnit",
            "ou: autonomousArea2",
            "administrativeRole: autonomousArea",
            "administrativeRole: accessControlSpecificArea",
            "administrativeRole: collectiveAttributeInnerArea",
            "administrativeRole: 2.5.23.4", // This is the subSchemaSpecificArea OID
            "administrativeRole: TRIGGEREXECUTIONSPECIFICAREA"
            );

        // It should fail, as an autonomous area is already defining the specific areas
        response = connection.add( autonomousArea );

        assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, response.getLdapResult().getResultCode() );
    }


    /**
     * Test the addition of some specific area
     * @throws Exception
     */
    @Test
    public void testAddSpecificAreas() throws Exception
    {
        Entry autonomousArea = LdifUtils.createEntry(
            "ou=autonomousArea, ou=system",
            "ObjectClass: top",
            "ObjectClass: organizationalUnit",
            "ou: autonomousArea",
            "administrativeRole: accessControlSpecificArea",
            "administrativeRole: TRIGGEREXECUTIONSPECIFICAREA"
            );

        AddResponse response = connection.add( autonomousArea );

        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );

        // Check that the entry is containing all the roles
        SearchResponse lookup = connection.lookup( "ou=autonomousArea, ou=system", "administrativeRole" );

        assertTrue( lookup instanceof SearchResultEntry );

        Entry result = ((SearchResultEntry)lookup).getEntry();

        assertFalse( result.contains( "administrativeRole", "autonomousArea" ) );
        assertTrue( result.contains( "administrativeRole", "accessControlSpecificArea" ) );
        assertFalse( result.contains( "administrativeRole", "collectiveAttributeSpecificArea" ) );
        assertFalse( result.contains( "administrativeRole", "2.5.23.4" ) );
        assertTrue( result.contains( "administrativeRole", "triggerExecutionSpecificArea" ) );
    }


    /**
     * Test the addition of some inner area
     * @throws Exception
     */
    @Test
    public void testAddInnerAreas() throws Exception
    {
        Entry autonomousArea = LdifUtils.createEntry(
            "ou=autonomousArea, ou=system",
            "ObjectClass: top",
            "ObjectClass: organizationalUnit",
            "ou: autonomousArea",
            "administrativeRole: accessControlINNERArea",
            "administrativeRole: TRIGGEREXECUTIONINNERAREA"
            );

        AddResponse response = connection.add( autonomousArea );

        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );

        // Check that the entry is containing all the roles
        SearchResponse lookup = connection.lookup( "ou=autonomousArea, ou=system", "administrativeRole" );

        assertTrue( lookup instanceof SearchResultEntry );

        Entry result = ((SearchResultEntry)lookup).getEntry();

        assertFalse( result.contains( "administrativeRole", "autonomousArea" ) );
        assertTrue( result.contains( "administrativeRole", "accessControlInnerArea" ) );
        assertTrue( result.contains( "administrativeRole", "triggerExecutionInnerArea" ) );
    }


    /**
     * Test the addition of some invalid role
     * @throws Exception
     */
    @Test
    public void testAddInvalidRole() throws Exception
    {
        Entry autonomousArea = LdifUtils.createEntry(
            "ou=autonomousArea, ou=system",
            "ObjectClass: top",
            "ObjectClass: organizationalUnit",
            "ou: autonomousArea",
            "administrativeRole: accessControlBadArea",
            "administrativeRole: TRIGGEREXECUTIONINNERAREA"
            );

        AddResponse response = connection.add( autonomousArea );

        assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, response.getLdapResult().getResultCode() );
    }


    /**
     * Test the addition of some specific and inner for the same role at the same place
     * @throws Exception
     */
    @Test
    public void testAddInnerAndSpecificRole() throws Exception
    {
        Entry autonomousArea = LdifUtils.createEntry(
            "ou=autonomousArea, ou=system",
            "ObjectClass: top",
            "ObjectClass: organizationalUnit",
            "ou: autonomousArea",
            "administrativeRole: accessControlSpecificArea",
            "administrativeRole: accessControlInnerArea"
            );

        AddResponse response = connection.add( autonomousArea );

        assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, response.getLdapResult().getResultCode() );
    }
}
