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


import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapUnwillingToPerformException;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Test cases for the AdministrativePoint Delete operation
 * 
 * We will create the following data structure :
 * <pre>
 * ou=system
 *  |
 *  +-ou=SAP-AC
 *  |  |
 *  |  +-ou=SAP-CA
 *  |  |  |
 *  |  |  +-ou=AAP --> to be deleted
 *  |  |     |
 *  |  |     +-ou=IAP-CA : OK
 *  |  |     |
 *  |  |     +-ou=IAP-AC : OK
 *  |  |     |
 *  |  |     +-ou=IAP-SS : KO
 *  |  |
 *  |  +-ou=AAP --> to be deleted
 *  |     |
 *  |     +-ou=AAP : OK
 *  |     |
 *  |     +-ou=SAP-AC : OK 
 *  |     |
 *  |     +-ou=SAP-CA : OK
 *  |     |
 *  |     +-ou=IAC-AC : OK
 *  |     |
 *  |     +-ou=IAC-CA : KO
 *  | 
 *  +-ou=AAP
 *  |  |
 *  |  +-ou=AAP --> to be deleted
 *  |     |
 *  |     +-ou=AAP : OK
 *  |     |
 *  |     +-ou=SAP-CA : OK
 *  |     |
 *  |     +-ou=IAP-CA : OK
 *  |
 *  +-ou=AAP1 --> to be deleted
 *     |
 *     +-ou=AAP : OK
 *     |
 *     +-ou=SAP-CA : OK
 *     |
 *     +-ou=IAP-CA : KO
 * </pre>
 * 
 * and check that removing entries from this data structure does not break the server
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( { ApacheDSTestExtension.class } )
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP") })
@ApplyLdifs(
    {
        // Entry # 1
        "dn: ou=SAP-AC,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: SAP-AC",
        "administrativeRole: accessControlSpecificArea",
        "",
        // Entry # 2
        "dn: ou=SAP-CA,ou=SAP-AC,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: SAP-CA",
        "administrativeRole: collectiveAttributeSpecificArea",
        "",
        // Entry # 3
        "dn: ou=AAP,ou=SAP-CA,ou=SAP-AC,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: AAP",
        "administrativeRole: autonomousArea",
        "",
        // Entry # 4
        "dn: ou=IAP-CA,ou=AAP,ou=SAP-CA,ou=SAP-AC,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: IAP-CA",
        "administrativeRole: collectiveAttributeInnerArea",
        "",
        // Entry # 5
        "dn: ou=IAP-AC,ou=AAP,ou=SAP-CA,ou=SAP-AC,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: IAP-AC",
        "administrativeRole: accessControlInnerArea",
        "",
        // Entry # 6
        "dn: ou=IAP-TE,ou=AAP,ou=SAP-CA,ou=SAP-AC,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: IAP-TE",
        "administrativeRole: triggerExecutionInnerArea",
        "",
        // Entry # 7
        "dn: ou=AAP,ou=SAP-AC,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: AAP",
        "administrativeRole: autonomousArea",
        "",
        // Entry # 8
        "dn: ou=AAP,ou=AAP,ou=SAP-AC,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: AAP",
        "administrativeRole: autonomousArea",
        "",
        // Entry # 9
        "dn: ou=SAP-AC,ou=AAP,ou=SAP-AC,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: SAP-AC",
        "administrativeRole: accessControlSpecificArea",
        "",
        // Entry # 10
        "dn: ou=SAP-CA,ou=AAP,ou=SAP-AC,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: SAP-CA",
        "administrativeRole: collectiveAttributeSpecificArea",
        "",
        // Entry # 11
        "dn: ou=IAP-AC,ou=AAP,ou=SAP-AC,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: IAP-AC",
        "administrativeRole: accessControlInnerArea",
        "",
        // Entry # 12
        "dn: ou=IAP-CA,ou=AAP,ou=SAP-AC,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: IAP-CA",
        "administrativeRole: collectiveAttributeInnerArea",
        "",
        // Entry # 13
        "dn: ou=AAP,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: AAP",
        "administrativeRole: autonomousArea",
        "",
        // Entry # 14
        "dn: ou=AAP,ou=AAP,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: AAP",
        "administrativeRole: autonomousArea",
        "",
        // Entry # 15
        "dn: ou=AAP,ou=AAP,ou=AAP,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: AAP",
        "administrativeRole: autonomousArea",
        "",
        // Entry # 16
        "dn: ou=SAP-CA,ou=AAP,ou=AAP,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: SAP-CA",
        "administrativeRole: collectiveAttributeSpecificArea",
        "",
        // Entry # 17
        "dn: ou=IAP-CA,ou=AAP,ou=AAP,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: IAP-CA",
        "administrativeRole: collectiveAttributeInnerArea",
        "",
        // Entry # 18
        "dn: ou=AAP1,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: AAP1",
        "administrativeRole: autonomousArea",
        "",
        // Entry # 19
        "dn: ou=AAP,ou=AAP1,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: AAP",
        "administrativeRole: autonomousArea",
        "",
        // Entry # 20
        "dn: ou=SAP-CA,ou=AAP1,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: SAP-CA",
        "administrativeRole: collectiveAttributeSpecificArea",
        "",
        "",
        // Entry # 21
        "dn: ou=IAP-CA,ou=AAP1,ou=system",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: IAP-CA",
        "administrativeRole: collectiveAttributeInnerArea",
        ""
})
public class AdministrativePointModifyDelIT extends AbstractLdapTestUnit
{
    // The shared LDAP connection
    private static LdapConnection connection;
    
    // A reference to the schema manager
    private static SchemaManager schemaManager;
    
    
    @BeforeEach
    public void init() throws Exception
    {
        connection = IntegrationUtils.getAdminConnection( getService() );
        schemaManager = getLdapServer().getDirectoryService().getSchemaManager();
    }
    
    
    @AfterEach
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
    // Test the Delete operation
    // -------------------------------------------------------------------
    /**
     * Test the deletion of the AAP role
     */
    @Test
    @Disabled
    public void testModifyRemoveAAP() throws Exception
    {
        assertTrue( getLdapServer().isStarted() );
    
        // Remove the AAP
        Modification modification = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE,
            new DefaultAttribute( "administrativeRole" ) );
    
        try
        {
            connection.modify( "ou=AAP,ou=SAP-CA,ou=SAP-AC,ou=system", modification );
            fail();
        }
        catch ( LdapUnwillingToPerformException lutpe )
        {
            assertTrue( true );
        }
    }
    
    
    /**
     * Test the addition of SAPs
     */
    @Test
    public void testDeleteSAP() throws Exception
    {
        assertTrue( getLdapServer().isStarted() );
    }
    
    
    /**
     * Test the deletion of IAPs
     */
    @Test
    public void testDeleteIAP() throws Exception
    {
        assertTrue( getLdapServer().isStarted() );
    }
}
