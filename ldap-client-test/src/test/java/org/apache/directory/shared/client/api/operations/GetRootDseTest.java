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

package org.apache.directory.shared.client.api.operations;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.ldap.client.api.PooledLdapConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.shared.client.api.LdapApiIntegrationUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * A class to test the GetRootDse operation with a returningAttributes parameter
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( { ApacheDSTestExtension.class } )
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP"), @CreateTransport(protocol = "LDAPS") })
public class GetRootDseTest extends AbstractLdapTestUnit
{
    private PooledLdapConnection connection;


    @BeforeEach
    public void setup() throws Exception
    {
        connection = (PooledLdapConnection)LdapApiIntegrationUtils.getPooledAdminConnection( getLdapServer() );
    }


    @AfterEach
    public void shutdown() throws Exception
    {
        LdapApiIntegrationUtils.releasePooledAdminConnection( connection, getLdapServer() );
    }


    /**
     * Test a lookup requesting all the attributes (* and +)
     *
     * @throws Exception
     */
    @Test
    public void testGetRootDse() throws Exception
    {
        Entry rootDse = connection.getRootDse();

        assertNotNull( rootDse );

        assertEquals( 11, rootDse.size() );
        assertTrue( rootDse.containsAttribute( "objectClass" ) );
        assertTrue( rootDse.containsAttribute( "entryUUID" ) );
        assertTrue( rootDse.containsAttribute( "namingContexts" ) );
        assertTrue( rootDse.containsAttribute( "subschemaSubentry" ) );
        assertTrue( rootDse.containsAttribute( "supportedControl" ) );
        assertTrue( rootDse.containsAttribute( "supportedExtension" ) );
        assertTrue( rootDse.containsAttribute( "supportedFeatures" ) );
        assertTrue( rootDse.containsAttribute( "supportedLDAPVersion" ) );
        assertTrue( rootDse.containsAttribute( "supportedSASLMechanisms" ) );
        assertTrue( rootDse.containsAttribute( "vendorName" ) );
        assertTrue( rootDse.containsAttribute( "vendorVersion" ) );
    }


    /**
     * Test a lookup requesting all the user attributes (*)
     *
     * @throws Exception
     */
    @Test
    public void testGetRooDseAllUserAttributes() throws Exception
    {
        Entry rootDse = connection.getRootDse( "*" );

        assertNotNull( rootDse );

        assertEquals( 1, rootDse.size() );
        assertTrue( rootDse.containsAttribute( "objectClass" ) );
    }


    /**
     * Test a lookup requesting all the operational attributes (+)
     *
     * @throws Exception
     */
    @Test
    public void testGetRooDseAllOperationalAttributes() throws Exception
    {
        Entry rootDse = connection.getRootDse( "+" );

        assertNotNull( rootDse );

        assertEquals( 10, rootDse.size() );
        assertTrue( rootDse.containsAttribute( "entryUUID" ) );
        assertTrue( rootDse.containsAttribute( "namingContexts" ) );
        assertTrue( rootDse.containsAttribute( "subschemaSubentry" ) );
        assertTrue( rootDse.containsAttribute( "supportedControl" ) );
        assertTrue( rootDse.containsAttribute( "supportedExtension" ) );
        assertTrue( rootDse.containsAttribute( "supportedFeatures" ) );
        assertTrue( rootDse.containsAttribute( "supportedLDAPVersion" ) );
        assertTrue( rootDse.containsAttribute( "supportedSASLMechanisms" ) );
        assertTrue( rootDse.containsAttribute( "vendorName" ) );
        assertTrue( rootDse.containsAttribute( "vendorVersion" ) );
    }


    /**
     * Test a lookup requesting a few attributes (Objectclass, vendorName and vendorVersion)
     *
     * @throws Exception
     */
    @Test
    public void testGetRooDseSelectedAttributes() throws Exception
    {
        Entry rootDse = connection.getRootDse( "objectClass", "vendorName", "vendorVersion" );

        assertNotNull( rootDse );

        assertEquals( 3, rootDse.size() );
        assertTrue( rootDse.containsAttribute( "objectClass" ) );
        assertTrue( rootDse.containsAttribute( "vendorName" ) );
        assertTrue( rootDse.containsAttribute( "vendorVersion" ) );
    }


    /**
     * Test a lookup requesting a few operational attributes (vendorName and vendorVersion)
     * and all user attrinutes (*)
     *
     * @throws Exception
     */
    @Test
    public void testGetRooDseSomeOpAttributesAllUserAttributes() throws Exception
    {
        Entry rootDse = connection.getRootDse( "*", "vendorName", "vendorVersion" );

        assertNotNull( rootDse );

        assertEquals( 3, rootDse.size() );
        assertTrue( rootDse.containsAttribute( "objectClass" ) );
        assertTrue( rootDse.containsAttribute( "vendorName" ) );
        assertTrue( rootDse.containsAttribute( "vendorVersion" ) );
    }


    /**
     * Test a lookup requesting a few user attributes (objectClass)
     * and all operational attributes (+)
     *
     * @throws Exception
     */
    @Test
    public void testGetRooDseSomeUserAttributesAllOpAttributes() throws Exception
    {
        Entry rootDse = connection.getRootDse( "+", "Objectclass" );

        assertNotNull( rootDse );

        assertEquals( 11, rootDse.size() );
        assertTrue( rootDse.containsAttribute( "objectClass" ) );
        assertTrue( rootDse.containsAttribute( "entryUUID" ) );
        assertTrue( rootDse.containsAttribute( "namingContexts" ) );
        assertTrue( rootDse.containsAttribute( "subschemaSubentry" ) );
        assertTrue( rootDse.containsAttribute( "supportedControl" ) );
        assertTrue( rootDse.containsAttribute( "supportedExtension" ) );
        assertTrue( rootDse.containsAttribute( "supportedFeatures" ) );
        assertTrue( rootDse.containsAttribute( "supportedLDAPVersion" ) );
        assertTrue( rootDse.containsAttribute( "supportedSASLMechanisms" ) );
        assertTrue( rootDse.containsAttribute( "vendorName" ) );
        assertTrue( rootDse.containsAttribute( "vendorVersion" ) );
    }


    /**
     * Test a                     if ( attributeType.getUsage() != UsageEnum.USER_APPLICATIONS )
    lookup requesting a all user attributes (*)
     * and all operational attributes (+)
     *
     * @throws Exception
     */
    @Test
    public void testGetRooDseAllUserAttributesAllOpAttributes() throws Exception
    {
        Entry rootDse = connection.getRootDse( "+", "*" );

        assertNotNull( rootDse );

        assertEquals( 11, rootDse.size() );
        assertTrue( rootDse.containsAttribute( "objectClass" ) );
        assertTrue( rootDse.containsAttribute( "entryUUID" ) );
        assertTrue( rootDse.containsAttribute( "namingContexts" ) );
        assertTrue( rootDse.containsAttribute( "subschemaSubentry" ) );
        assertTrue( rootDse.containsAttribute( "supportedControl" ) );
        assertTrue( rootDse.containsAttribute( "supportedExtension" ) );
        assertTrue( rootDse.containsAttribute( "supportedFeatures" ) );
        assertTrue( rootDse.containsAttribute( "supportedLDAPVersion" ) );
        assertTrue( rootDse.containsAttribute( "supportedSASLMechanisms" ) );
        assertTrue( rootDse.containsAttribute( "vendorName" ) );
        assertTrue( rootDse.containsAttribute( "vendorVersion" ) );
    }


    /**
     * Test a lookup requesting no attributes (1.1)
     *
     * @throws Exception
     */
    @Test
    public void testGetRooDseNoAttribute() throws Exception
    {
        Entry rootDse = connection.getRootDse( "1.1" );

        assertNotNull( rootDse );

        assertEquals( 0, rootDse.size() );
    }


    /**
     * Test a lookup requesting no attributes (1.1) with some attributes
     *
     * @throws Exception
     */
    @Test
    public void testGetRooDseNoAttributeSomeUserAttributes() throws Exception
    {
        Entry rootDse = connection.getRootDse( "1.1", "objectClass" );

        assertNotNull( rootDse );

        assertEquals( 1, rootDse.size() );
        assertTrue( rootDse.containsAttribute( "objectClass" ) );
    }
}
