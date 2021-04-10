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

package org.apache.directory.server.core.operations.getRootDse;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.LdapCoreSessionConnection;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * A class to test the GetRootDse operation with a returningAttributes parameter
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( { ApacheDSTestExtension.class } )
public class GetRootDseIT extends AbstractLdapTestUnit
{
    private LdapConnection connection;


    @BeforeEach
    public void setup() throws Exception
    {
        connection = IntegrationUtils.getAdminConnection( getService() );
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

        assertEquals( 1, rootDse.size() );
        assertTrue( rootDse.containsAttribute( "objectClass" ) );
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

        assertEquals( 9, rootDse.size() );
        assertTrue( rootDse.containsAttribute( "entryUUID" ) );
        assertTrue( rootDse.containsAttribute( "namingContexts" ) );
        assertTrue( rootDse.containsAttribute( "subschemaSubentry" ) );
        assertTrue( rootDse.containsAttribute( "supportedControl" ) );
        assertTrue( rootDse.containsAttribute( "supportedExtension" ) );
        assertTrue( rootDse.containsAttribute( "supportedFeatures" ) );
        assertTrue( rootDse.containsAttribute( "supportedLDAPVersion" ) );
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
     * and all user attributes (*)
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

        assertEquals( 10, rootDse.size() );
        assertTrue( rootDse.containsAttribute( "objectClass" ) );
        assertTrue( rootDse.containsAttribute( "entryUUID" ) );
        assertTrue( rootDse.containsAttribute( "namingContexts" ) );
        assertTrue( rootDse.containsAttribute( "subschemaSubentry" ) );
        assertTrue( rootDse.containsAttribute( "supportedControl" ) );
        assertTrue( rootDse.containsAttribute( "supportedExtension" ) );
        assertTrue( rootDse.containsAttribute( "supportedFeatures" ) );
        assertTrue( rootDse.containsAttribute( "supportedLDAPVersion" ) );
        assertTrue( rootDse.containsAttribute( "vendorName" ) );
        assertTrue( rootDse.containsAttribute( "vendorVersion" ) );
    }


    /**
     * Test a lookup requesting a all user attributes (*)
     * and all operational attributes (+)
     *
     * @throws Exception
     */
    @Test
    public void testGetRooDseAllUserAttributesAllOpAttributes() throws Exception
    {
        Entry rootDse = connection.getRootDse( "+", "*" );

        assertNotNull( rootDse );

        assertEquals( 10, rootDse.size() );
        assertTrue( rootDse.containsAttribute( "objectClass" ) );
        assertTrue( rootDse.containsAttribute( "entryUUID" ) );
        assertTrue( rootDse.containsAttribute( "namingContexts" ) );
        assertTrue( rootDse.containsAttribute( "subschemaSubentry" ) );
        assertTrue( rootDse.containsAttribute( "supportedControl" ) );
        assertTrue( rootDse.containsAttribute( "supportedExtension" ) );
        assertTrue( rootDse.containsAttribute( "supportedFeatures" ) );
        assertTrue( rootDse.containsAttribute( "supportedLDAPVersion" ) );
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


    /**
     * Check that we cannot access the RootDSE with an anonymous user and default access control
     * @throws Exception
     */
    @Test
    public void testGetRootDSEAnonymousNoAccessControl() throws Exception
    {
        DirectoryService service = getService();
        service.setAccessControlEnabled( false );
        
        try ( LdapCoreSessionConnection connection = new LdapCoreSessionConnection( service ) )
        {
            Entry rootDse = connection.getRootDse();
    
            assertNotNull( rootDse );
        }
    }


    /**
     * Check that we can access the RootDSE with an anonymous user and access control set (but no ACI)
     * @throws Exception
     */
    @Test
    public void testGetRootDSEAnonymousWithAccessControl() throws Exception
    {
        DirectoryService service = getService();
        service.setAccessControlEnabled( true );
        
        try ( LdapCoreSessionConnection connection = new LdapCoreSessionConnection( service ) )
        {
            Entry rootDse = connection.getRootDse();
    
            assertNotNull( rootDse );
        }
    }


    /**
     * Check the supportedFeatures attribute
     *
     * @throws Exception
     */
    @Test
    public void testGetSupportedFeatures() throws Exception
    {
        DirectoryService service = getService();
        service.setAccessControlEnabled( true );
        
        try ( LdapCoreSessionConnection connection = new LdapCoreSessionConnection( service ) )
        {
            Entry rootDse = connection.getRootDse( SchemaConstants.SUPPORTED_FEATURES_AT );
            
            assertNotNull( rootDse );
            assertTrue( rootDse.containsAttribute( SchemaConstants.SUPPORTED_FEATURES_AT ) );
            
            Attribute supportedFeatures = rootDse.get( SchemaConstants.SUPPORTED_FEATURES_AT );
            
            assertEquals( 2, supportedFeatures.size() );
            assertTrue( supportedFeatures.contains( 
                SchemaConstants.FEATURE_ALL_OPERATIONAL_ATTRIBUTES, 
                SchemaConstants.FEATURE_MODIFY_INCREMENT ) );
        }
    }
}
