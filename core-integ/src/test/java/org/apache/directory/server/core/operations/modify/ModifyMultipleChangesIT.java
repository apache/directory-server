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
package org.apache.directory.server.core.operations.modify;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Test case with multiple modifications on a person entry.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( { ApacheDSTestExtension.class } )
@CreateDS(name = "ModifyMultipleChangesIT")
@ApplyLdifs( {
    // Entry # 1
    "dn: cn=Tori Amos,ou=system",
    "objectClass: inetOrgPerson",
    "objectClass: organizationalPerson",
    "objectClass: person",
    "objectClass: top",
    "description: an American singer-songwriter",
    "cn: Tori Amos",
    "sn: Amos", 
    // Entry # 2
    "dn: cn=Debbie Harry,ou=system",
    "objectClass: inetOrgPerson",
    "objectClass: organizationalPerson",
    "objectClass: person",
    "objectClass: top",
    "cn: Debbie Harry",
    "sn: Harry" 
    }
)
public class ModifyMultipleChangesIT extends AbstractLdapTestUnit 
{
    private static final String DN_TORI_AMOS = "cn=Tori Amos,ou=system";
    private static final String DN_TEST = "cn=test, ou=system";


    /**
     * @throws Exception on errors
     */
    @BeforeAll
    public static void createData() throws Exception
    {
        try ( LdapConnection conn = IntegrationUtils.getAdminConnection( classDirectoryService ) )
        {
            // -------------------------------------------------------------------
            // Enable the nis schema
            // -------------------------------------------------------------------
            // check if nis is disabled
            String nisDn = "cn=nis," + SchemaConstants.OU_SCHEMA;
            Entry entry = conn.lookup( nisDn );
            Attribute disabled = entry.get( "m-disabled" );
            boolean isNisDisabled = false;
    
            if ( disabled != null )
            {
                isNisDisabled = disabled.getString().equalsIgnoreCase( "TRUE" );
            }
    
            // if nis is disabled then enable it
            if ( isNisDisabled )
            {
                conn.modify( nisDn, new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, "m-disabled" ) );
            }
    
            // -------------------------------------------------------------------
            // Add a bunch of nis groups
            // -------------------------------------------------------------------
            addNisPosixGroup( conn, "testGroup0", 0 );
            addNisPosixGroup( conn, "testGroup1", 1 );
            addNisPosixGroup( conn, "testGroup2", 2 );
            addNisPosixGroup( conn, "testGroup4", 4 );
            addNisPosixGroup( conn, "testGroup5", 5 );
    
            // Create a test account
            Entry testAccount = conn.lookup( DN_TEST );
            
            if ( testAccount == null )
            {
                conn.add( new DefaultEntry(
                    DN_TEST, 
                    "ObjectClass", "top",
                    "ObjectClass", "account",
                    "ObjectClass", "posixAccount",
                    "cn", "test",
                    "uid", "1",
                    "uidNumber", "1",
                    "gidNumber", "1",
                     "homeDirectory", "/",
                    "description", "A test account"
                    ) );
            }
        }
    }


    /**
     * Create a NIS group
     */
    private static void addNisPosixGroup( LdapConnection connection, String name, int gid ) throws Exception
    {
        String posixGroupDn = "cn=" + name + ",ou=groups, ou=system";
        Entry posixGroup = connection.lookup( posixGroupDn );
        
        if ( posixGroup == null )
        {
            connection.add( new DefaultEntry( posixGroupDn,
                "objectClass", "top",
                "objectClass", "posixGroup",
                "cn", name, 
                "gidNumber", Integer.toString( gid ) ) );
        }
    }


    /**
     * Create a person entry and perform a modify op, in which
     * we modify an attribute two times.
     */
    @Test
    public void testModifyMultipleChangeDeleteAddSnInMust() throws Exception 
    {
        try ( LdapConnection conn = IntegrationUtils.getAdminConnection( getService() ) )
        {
            createData();
        
            // Try to delete and add the SN which is in MUST
            conn.modify( DN_TORI_AMOS, 
                new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, "sn", "Amos" ),
                new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, "sn", "TAmos" ));

            // Verify that the attribute value has been added
            Entry entry = conn.lookup( DN_TORI_AMOS );
            Attribute sn = entry.get( "sn" );
            assertNotNull( sn );
            assertTrue( sn.contains( "TAmos" ) );
            assertEquals( 1, sn.size() );
        }
    }
}
