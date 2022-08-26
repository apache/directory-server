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
package org.apache.directory.server.core.schema;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.directory.api.ldap.model.constants.MetaSchemaConstants;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


@ExtendWith( ApacheDSTestExtension.class )
@CreateDS(name = "ObjectClassCreateIT")
public class ObjectClassCreateIT extends AbstractLdapTestUnit
{
    private String testOID = "1.3.6.1.4.1.18060.0.4.0.3.1.555555.5555.5555555";


    private void injectSchema() throws Exception
    {
        try ( LdapConnection conn = IntegrationUtils.getAdminConnection( getService() ) )
        {
            //--------------------------------------------------------------------
            // The accountStatus AT
            //--------------------------------------------------------------------
            conn.add( new DefaultEntry(
                MetaSchemaConstants.M_OID_AT + "=2.16.840.1.113730.3.2.22.249,ou=attributeTypes,cn=apachemeta,ou=schema",
                "objectClass", "top",
                "objectClass", "metaTop",
                "objectClass", "metaAttributeType",
                "m-oid", "2.16.840.1.113730.3.2.22.249",
                
                // The name
                "m-name", "accountStatus",
        
                // The Obsolete flag
                "m-obsolete", "FALSE",
        
                // The single value flag
                "m-singleValue", "TRUE",
        
                // The collective flag
                "m-collective", "FALSE",
        
                // The noUserModification flag
                "m-noUserModification", "FALSE",
        
                // The usage
                "m-usage", "USER_APPLICATIONS",
        
                // The equality matching rule
                "m-equality", "caseIgnoreMatch",
        
                // The substr matching rule
                "m-substr", "caseIgnoreSubstringsMatch",
        
                // The syntax
                "m-syntax", "1.3.6.1.4.1.1466.115.121.1.15",
        
                // The superior
                "m-supAttributeType", "name",
        
                // The description
                "m-description", "Account Status"
                ) );                
    
            //--------------------------------------------------------------------
            // The extendPerson OC
            //--------------------------------------------------------------------    
            conn.add( new DefaultEntry(
                MetaSchemaConstants.M_OID_AT + "=2.16.840.1.113730.3.2.22,ou=objectClasses,cn=apachemeta,ou=schema",
                "objectClass", "top",
                "objectClass", "metaTop",
                "objectClass", "metaObjectClass",
                "m-oid", "2.16.840.1.113730.3.2.22",
                
                // The name
                "m-name", "extendPerson",
        
                // The Obsolete flag
                "m-obsolete", "FALSE",
        
                // The Type list
                "m-typeObjectClass", "STRUCTURAL",
        
                // The superiors
                "m-supObjectClass", "inetOrgPerson",
        
                // The description
                "m-description", "Extended InetOrgPerson",
        
                // The MAY list
                "m-may", "accountStatus" 
                ) );
        }
    }


    /*
     * Test that I cannot create an ObjectClass entry with an invalid name
     */
    @Test
    public void testCannotCreateObjectClassWithInvalidNameAttribute() throws Exception
    {
        Assertions.assertThrows( LdapInvalidAttributeValueException.class, () -> 
        {
            try ( LdapConnection conn = IntegrationUtils.getAdminConnection( getService() ) )
            {
                conn.add( new DefaultEntry(
                    MetaSchemaConstants.M_OID_AT + "=" + testOID + ",ou=objectClasses,cn=apachemeta,ou=schema",
                    "objectClass", "top",
                    "objectClass", "metaTop",
                    "objectClass", "metaObjectClass",
                    "m-oid", "testOID",
                    "m-name", "http://example.com/users/accounts/L0" 
                    ) );
            }
        } );
    }


    /*
     * Test that I canotn create an ObjectClass entry with an invalid name
     */
    @Test
    public void testCannotCreateObjectClassWithNoObjectClass() throws Exception
    {
        Assertions.assertThrows( LdapInvalidAttributeValueException.class, () -> 
        {
            try ( LdapConnection conn = IntegrationUtils.getAdminConnection( getService() ) )
            {
                conn.add( new DefaultEntry(
                    MetaSchemaConstants.M_OID_AT + "=" + testOID + ",ou=objectClasses,cn=apachemeta,ou=schema",
                    "m-oid", "testOID",
                    "m-name", "no-objectClasses" 
                    ) );
            }
        } );
    }


    /**
     * Test that if we create an OC with a superior OC then the AT are correctly
     * inherited.
     */
    @Test
    public void testCreateOCWithSuperior() throws Exception
    {
        injectSchema();
        
        try ( LdapConnection conn = IntegrationUtils.getAdminConnection( getService() ) )
        {
            // Now, check that we can add entries with this new OC
            conn.add( new DefaultEntry(
                "cn=test,ou=system",
                "objectClass", "top",
                "objectClass", "extendPerson",
                "uid", "test",
                "sn", "test",
                "givenName", "test",
                "cn", "test",
                "displayName", "test-test",
                "initials", "tt",
                "accountStatus", "test"
                ) );
        }
    }


    /**
     * Test that if we create an AT with an ancestor, we can search and
     * get back the entry using its ancestor
     */
    @Test
    public void testCreateATWithSuperior() throws Exception
    {
        injectSchema();
        
        try ( LdapConnection conn = IntegrationUtils.getAdminConnection( getService() ) )
        {
            // Now, check that we can add entries with this new AT
            conn.add( new DefaultEntry(
                "cn=test,ou=system",
                "objectClass", "top",
                "objectClass", "extendPerson",
                "uid", "test",
                "sn", "test",
                "givenName", "test",
                "cn", "test",
                "displayName", "test-test",
                "initials", "tt",
                "accountStatus", "accountStatusValue"
                ) );

            boolean found = false;

            try ( EntryCursor cursor = conn.search( "ou=system", "(name=accountStatusValue)", SearchScope.ONELEVEL, "*" ) )
            {
                while ( cursor.available() )
                {
                    assertFalse( found );
                    Entry entry = cursor.get();
                    assertTrue( entry.contains( "accountStatus", "accountStatusValue" ) );
                    found = true;
                }
            }
    
            found = false;

            try ( EntryCursor cursor = conn.search( "ou=system", "(accountStatus=accountStatusValue)", SearchScope.ONELEVEL, "*" ) )
            {
                while ( cursor.available() )
                {
                    assertFalse( found );
                    Entry entry = cursor.get();
                    assertTrue( entry.contains( "accountStatus", "accountStatusValue" ) );
                    found = true;
                }
            }
        }
    }
}
