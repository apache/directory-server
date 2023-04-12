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
package org.apache.directory.server.core.operations.search;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapInvalidSearchFilterException;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.GreaterEqNode;
import org.apache.directory.api.ldap.model.filter.LessEqNode;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.LoadSchema;
import org.apache.directory.server.core.api.LdapCoreSessionConnection;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Tests the search() methods of the provider.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( { ApacheDSTestExtension.class } )
@CreateDS(name = "SearchDS",
    loadedSchemas =
        { @LoadSchema(name = "nis", enabled = true) })
@ApplyLdifs(
    {
        "dn: m-oid=2.2.0, ou=attributeTypes, cn=apachemeta, ou=schema",
        "objectclass: metaAttributeType",
        "objectclass: metaTop",
        "objectclass: top",
        "m-oid: 2.2.0",
        "m-name: integerAttribute",
        "m-description: the precursor for all integer attributes",
        "m-equality: integerMatch",
        "m-ordering: integerOrderingMatch",
        "m-syntax: 1.3.6.1.4.1.1466.115.121.1.27",
        "m-length: 0",
        "",
        "dn: m-oid=1.1.1.1.1.1, ou=attributeTypes, cn=other, ou=schema",
        "objectclass: metaAttributeType",
        "objectclass: metaTop",
        "objectclass: top",
        "m-oid: 1.1.1.1.1.1",
        "m-name: testInt",
        "m-description: An attributeType used for testing the ORDERING MR",
        "m-equality: integerMatch",
        "m-ordering: integerOrderingMatch",
        "m-syntax: 1.3.6.1.4.1.1466.115.121.1.27",
        "m-length: 0",
        "",
        "dn: ou=testing00,ou=system",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "objectClass: extensibleObject",
        "ou: testing00",
        "integerAttribute: 0",
        "",
        "dn: ou=testing01,ou=system",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "objectClass: extensibleObject",
        "ou: testing01",
        "integerAttribute: 1",
        "",
        "dn: ou=testing02,ou=system",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "objectClass: extensibleObject",
        "ou: testing02",
        "integerAttribute: 2",
        "",
        "dn: ou=testing03,ou=system",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "objectClass: extensibleObject",
        "ou: testing03",
        "integerAttribute: 3",
        "",
        "dn: ou=testing04,ou=system",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "objectClass: extensibleObject",
        "ou: testing04",
        "integerAttribute: 4",
        "",
        "dn: ou=testing05,ou=system",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "objectClass: extensibleObject",
        "ou: testing05",
        "integerAttribute: 5",
        "",
        "dn: ou=subtest,ou=testing01,ou=system",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "ou: subtest",
        "",
        "dn: cn=Heather Nova, ou=system",
        "objectClass: top",
        "objectClass: person",
        "cn: Heather Nova",
        "sn: Nova",
        "telephoneNumber: 1 801 555 1212 ",
        "",
        "dn: cn=with-dn, ou=system",
        "objectClass: top",
        "objectClass: person",
        "objectClass: organizationalPerson",
        "objectClass: inetorgPerson",
        "cn: singer",
        "sn: manager",
        "telephoneNumber: 1 801 555 1212 ",
        "manager: cn=Heather Nova, ou=system",
        "",
        "dn: cn=testGroup0,ou=groups,ou=system",
        "objectClass: top",
        "objectClass: posixGroup",
        "objectClass: extensibleObject",
        "cn: testGroup0",
        "gidNumber: 0",
        "testInt: 0",
        "",
        "dn: cn=testGroup1,ou=groups,ou=system",
        "objectClass: top",
        "objectClass: posixGroup",
        "objectClass: extensibleObject",
        "cn: testGroup1",
        "gidNumber: 1",
        "testInt: 1",
        "",
        "dn: cn=testGroup2,ou=groups,ou=system",
        "objectClass: top",
        "objectClass: posixGroup",
        "objectClass: extensibleObject",
        "cn: testGroup2",
        "gidNumber: 2",
        "testInt: 2",
        "",
        "dn: cn=testGroup4,ou=groups,ou=system",
        "objectClass: top",
        "objectClass: posixGroup",
        "objectClass: extensibleObject",
        "cn: testGroup4",
        "gidNumber: 4",
        "testInt: 4",
        "",
        "dn: cn=testGroup5,ou=groups,ou=system",
        "objectClass: top",
        "objectClass: posixGroup",
        "objectClass: extensibleObject",
        "cn: testGroup5",
        "gidNumber: 5",
        "testInt: 5"
        })
public class SearchIT extends AbstractLdapTestUnit
{
    private static final String RDN = "cn=Heather Nova";
    private static final String FILTER = "(objectclass=*)";

    @Test
    public void testSearchOneLevel() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            Map<String, Entry> entries = new HashMap<>();
    
            try ( EntryCursor cursor = connection.search( "ou=system", "(ou=*)", SearchScope.ONELEVEL ) )
            {
                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }
            }
        
            assertEquals( 9, entries.size(), "Expected number of results returned was incorrect!" );
            assertTrue( entries.containsKey( "ou=testing00,ou=system" ) );
            assertTrue( entries.containsKey( "ou=testing01,ou=system" ) );
            assertTrue( entries.containsKey( "ou=testing02,ou=system" ) );
        }
    }


    @Test
    public void testSearchWithTop() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            Map<String, Entry> entries = new HashMap<>();
            
            try ( EntryCursor cursor = connection.search( "ou=system", "(&(objectClass=top)(objectClass=person)"
                + "(objectClass=organizationalPerson)(objectClass=inetOrgPerson)(cn=si*))", SearchScope.SUBTREE ) )
            {
                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }
            }

            assertEquals( 1, entries.size(), "Expected number of results returned was incorrect!" );
            assertTrue( entries.containsKey( "cn=with-dn,ou=system" ) );
        } 
    }


    @Test
    public void testSearchSubTreeLevel() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            Map<String, Entry> entries = new HashMap<>();
            
            try ( EntryCursor cursor = connection.search( "ou=system", "(ou=*)", SearchScope.SUBTREE ) )
            {
                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }
            }
    
            assertEquals( 14, entries.size(), "Expected number of results returned was incorrect!" );
            assertTrue( entries.containsKey( "ou=system" ) );
            assertTrue( entries.containsKey( "ou=testing00,ou=system" ) );
            assertTrue( entries.containsKey( "ou=testing01,ou=system" ) );
            assertTrue( entries.containsKey( "ou=testing02,ou=system" ) );
            assertTrue( entries.containsKey( "ou=subtest,ou=testing01,ou=system" ) );
        }
    }


    @Test
    public void testSearchSubTreeLevelNoAttributes() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            Map<String, Entry> entries = new HashMap<>();
            
            try ( EntryCursor cursor = connection.search( "ou=system", "(ou=testing02)", SearchScope.SUBTREE, "1.1" ) )
            {
                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }
            }
    
            assertEquals( 1, entries.size(), "Expected number of results returned was incorrect!" );
            assertTrue( entries.containsKey( "ou=testing02,ou=system" ) );
            Entry entry = entries.get( "ou=testing02,ou=system" );
            assertEquals( 0, entry.size() );
        }
    }


    @Test
    public void testSearchSubstringSubTreeLevel() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            Map<String, Entry> entries = new HashMap<>();
            
            try ( EntryCursor cursor = connection.search( "ou=system", "(objectClass=organisation)", SearchScope.SUBTREE ) )
            {
                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }
            }

            // 0 because the filter does not have a SUBSTRING MR
            assertEquals( 0, entries.size(), "Expected number of results returned was incorrect!" );
    
            // 
            
            entries.clear();
            
            try ( EntryCursor cursor = connection.search( "ou=system", "(ou=*es*)", SearchScope.SUBTREE ) )
            {
                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }
            }
    
            assertEquals( 8, entries.size(), "Expected number of results returned was incorrect!" );
            
            assertTrue( entries.containsKey( "ou=testing00,ou=system" ) );
            assertTrue( entries.containsKey( "ou=testing01,ou=system" ) );
            assertTrue( entries.containsKey( "ou=testing02,ou=system" ) );
            assertTrue( entries.containsKey( "ou=testing03,ou=system" ) );
            assertTrue( entries.containsKey( "ou=testing04,ou=system" ) );
            assertTrue( entries.containsKey( "ou=testing05,ou=system" ) );
            assertTrue( entries.containsKey( "ou=services,ou=configuration,ou=system" ) );
            assertTrue( entries.containsKey( "ou=subtest,ou=testing01,ou=system" ) );
        }
    }


    /**
     * Tests to make sure undefined attributes in filter assertions are pruned and do not
     * result in exceptions.
     */
    @Test
    public void testBogusAttributeInSearchFilter() throws Exception
    {
        boolean oldSetAllowAnnonymousAccess = getService().isAllowAnonymousAccess();
        getService().setAllowAnonymousAccess( true );

        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            try ( EntryCursor cursor = connection.search( "ou=system", "(bogusAttribute=abc123)", SearchScope.SUBTREE ) )
            {
                assertFalse( cursor.next() );
            }

            try ( EntryCursor cursor = connection.search( "ou=system", "(!(bogusAttribute=abc123))", SearchScope.SUBTREE ) )
            {
                assertFalse( cursor.next() );
            }
    
            try ( EntryCursor cursor = connection.search( "ou=system", "(|(bogusAttribute=abc123)(bogusAttribute=abc123))", SearchScope.SUBTREE ) )
            {
                assertFalse( cursor.next() );
            }
    
            try ( EntryCursor cursor = connection.search( "ou=system", "(|(bogusAttribute=abc123)(ou=abc123))", SearchScope.SUBTREE ) )
            {
                assertFalse( cursor.next() );
            }
    
            try ( EntryCursor cursor = connection.search( "ou=system", "(OBJECTclass=*)", SearchScope.SUBTREE ) )
            {
                assertTrue( cursor.next() );
            }
    
            try ( EntryCursor cursor = connection.search( "ou=system", "(objectclass=*)", SearchScope.SUBTREE ) )
            {
                assertTrue( cursor.next() );
            }

            getService().setAllowAnonymousAccess( oldSetAllowAnnonymousAccess );
        }
    }


    @Test
    public void testSearchFilterArgs() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            Map<String, Entry> entries = new HashMap<>();
            
            try ( EntryCursor cursor = connection.search( "ou=system", "(|(ou=testing00)(ou=testing01))", SearchScope.ONELEVEL ) )
            {
                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }
            }
    
            assertEquals( 2, entries.size(), "Expected number of results returned was incorrect!" );
            assertTrue( entries.containsKey( "ou=testing00,ou=system" ) );
            assertTrue( entries.containsKey( "ou=testing01,ou=system" ) );
        }
    }


    @Test
    public void testFilterExpansion0() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            Map<String, Entry> entries = new HashMap<>();
            
            try ( EntryCursor cursor = connection.search( "ou=system", "(name=testing00)", SearchScope.SUBTREE ) )
            {
                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }
            }
            
            assertEquals( 1, entries.size(), "size of results" );
            assertTrue( entries.containsKey( "ou=testing00,ou=system" ), "contains ou=testing00,ou=system" );
        }
    }


    @Test
    public void testFilterExpansion1() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            Map<String, Entry> entries = new HashMap<>();
            
            try ( EntryCursor cursor = connection.search( "ou=system", "(name=*)", SearchScope.SUBTREE ) )
            {
                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }
            }
    
            assertEquals( 23, entries.size(), "size of results" );
            assertTrue( entries.containsKey( "ou=testing00,ou=system" ), "contains ou=testing00,ou=system" );
            assertTrue( entries.containsKey( "ou=testing01,ou=system" ), "contains ou=testing01,ou=system" );
            assertTrue( entries.containsKey( "ou=testing01,ou=system" ), "contains ou=testing02,ou=system" );
            assertTrue( entries.containsKey( "ou=configuration,ou=system" ), "contains ou=configuration,ou=system" );
            assertTrue( entries.containsKey( "ou=groups,ou=system" ), "contains ou=groups,ou=system" );
            assertTrue( entries.containsKey( "ou=interceptors,ou=configuration,ou=system" ), "contains ou=interceptors,ou=configuration,ou=system" );
            assertTrue( entries.containsKey( "ou=partitions,ou=configuration,ou=system" ), "contains ou=partitions,ou=configuration,ou=system" );
            assertTrue( entries.containsKey( "ou=services,ou=configuration,ou=system" ), "contains ou=services,ou=configuration,ou=system" );
            assertTrue( entries.containsKey( "ou=subtest,ou=testing01,ou=system" ), "contains ou=subtest,ou=testing01,ou=system" );
            assertTrue( entries.containsKey( "ou=system" ), "contains ou=system" );
            assertTrue( entries.containsKey( "ou=users,ou=system" ), "contains ou=users,ou=system" );
            assertTrue( entries.containsKey( "uid=admin,ou=system" ), "contains uid=admin,ou=system" );
            assertTrue( entries.containsKey( "cn=Administrators,ou=groups,ou=system" ), "contains cn=administrators,ou=groups,ou=system" );
        }
    }


    @Test
    public void testFilterExpansion2() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            Map<String, Entry> entries = new HashMap<>();
            
            try ( EntryCursor cursor = connection.search( "ou=system", "(|(name=testing00)(name=testing01))", SearchScope.SUBTREE ) )
            {
                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }
            }
    
            assertEquals( 2, entries.size(), "size of results" );
            assertTrue( entries.containsKey( "ou=testing00,ou=system" ), "contains ou=testing00,ou=system" );
            assertTrue( entries.containsKey( "ou=testing01,ou=system" ), "contains ou=testing01,ou=system" );
        }
    }


    @Test
    public void testFilterExpansion4() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            Map<String, Entry> entries = new HashMap<>();
            
            try ( EntryCursor cursor = connection.search( "ou=system", "(name=testing*)", SearchScope.SUBTREE ) )
            {
                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }
            }
    
            assertEquals( 6, entries.size(), "size of results" );
            assertTrue( entries.containsKey( "ou=testing00,ou=system" ), "contains ou=testing00,ou=system" );
            assertTrue( entries.containsKey( "ou=testing01,ou=system" ), "contains ou=testing01,ou=system" );
            assertTrue( entries.containsKey( "ou=testing02,ou=system" ), "contains ou=testing02,ou=system" );
            assertTrue( entries.containsKey( "ou=testing03,ou=system" ), "contains ou=testing03,ou=system" );
            assertTrue( entries.containsKey( "ou=testing04,ou=system" ), "contains ou=testing04,ou=system" );
            assertTrue( entries.containsKey( "ou=testing05,ou=system" ), "contains ou=testing05,ou=system" );
        }
    }


    @Test
    public void testFilterExpansion5() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            Map<String, Entry> entries = new HashMap<>();
            
            String filter = "(|(2.5.4.11.1=testing*)(2.5.4.54=testing*)(2.5.4.10=testing*)"
                + "(2.5.4.6=testing*)(2.5.4.43=testing*)(2.5.4.7.1=testing*)(2.5.4.10.1=testing*)"
                + "(2.5.4.44=testing*)(2.5.4.11=testing*)(2.5.4.4=testing*)(2.5.4.8.1=testing*)"
                + "(2.5.4.12=testing*)(1.3.6.1.4.1.18060.0.4.1.2.3=testing)"
                + "(2.5.4.7=testing*)(2.5.4.3=testing*)(2.5.4.8=testing*)(2.5.4.42=testing*))";

            try ( EntryCursor cursor = connection.search( "ou=system", filter, SearchScope.SUBTREE ) )
            {
                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }
            }
    
            assertEquals( 6, entries.size(), "size of results" );
            assertTrue( entries.containsKey( "ou=testing00,ou=system" ), "contains ou=testing00,ou=system" );
            assertTrue( entries.containsKey( "ou=testing01,ou=system" ), "contains ou=testing01,ou=system" );
            assertTrue( entries.containsKey( "ou=testing02,ou=system" ), "contains ou=testing02,ou=system" );
            assertTrue( entries.containsKey( "ou=testing03,ou=system" ), "contains ou=testing03,ou=system" );
            assertTrue( entries.containsKey( "ou=testing04,ou=system" ), "contains ou=testing04,ou=system" );
            assertTrue( entries.containsKey( "ou=testing05,ou=system" ), "contains ou=testing05,ou=system" );
        }
    }


    @Test
    public void testOpAttrDenormalizationOff() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            Map<String, Entry> entries = new HashMap<>();
            
            try ( EntryCursor cursor = connection.search( "ou=system", "(ou=testing00)", SearchScope.ONELEVEL, "creatorsName" ) )
            {
                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }
            }
            
            assertEquals( 1, entries.size(), "Expected number of results returned was incorrect!" );
            assertTrue( entries.containsKey( "ou=testing00,ou=system" ) );
            Entry entry = entries.get( "ou=testing00,ou=system" );
            assertTrue( entry.get( "creatorsName" ).contains( "uid=admin,ou=system" ) );
            assertEquals( "0.9.2342.19200300.100.1.1= admin ,2.5.4.11= system ", entry.get(
                "creatorsName" ).get().getNormalized(), "normalized creator's name" );
        }
    }


    @Test
    public void testOpAttrDenormalizationOn() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            Map<String, Entry> entries = new HashMap<>();
            
            try ( EntryCursor cursor = connection.search( "ou=system", "(ou=testing00)", SearchScope.ONELEVEL, "creatorsName" ) )
            {
                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }
            }

            getService().setDenormalizeOpAttrsEnabled( true );
            assertEquals( 1, entries.size(), "Expected number of results returned was incorrect!" );
            assertTrue( entries.containsKey( "ou=testing00,ou=system" ) );
            Entry entry = entries.get( "ou=testing00,ou=system" );
            assertTrue( entry.get( "creatorsName" ).contains( "uid=admin,ou=system" ) );
        }
    }


    @Test
    public void testBinaryAttributesInFilter() throws Exception
    {
        byte[] certData = new byte[]
            { 0x34, 0x56, 0x4e, 0x5f };

        // First let's add a some binary data representing a userCertificate
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            connection.add( new DefaultEntry(
                "cn=Kate Bush,ou=system",
                "objectClass", "top", 
                "objectClass", "person",
                "objectClass", "strongAuthenticationUser",
                "cn", "Bush",
                "sn", "Kate Bush",
                "userCertificate", certData
                ) );
            
            // Search for kate by cn first
            try ( EntryCursor cursor = connection.search( "ou=system", "(cn=Kate Bush)", SearchScope.ONELEVEL ) )
            {
                assertTrue( cursor.next() );
                
                Entry entry = cursor.get();

                assertNotNull( entry );
                assertEquals( "cn=Kate Bush,ou=system", entry.getDn().getName() );
                
                assertFalse( cursor.next() );
            }

            try ( EntryCursor cursor = connection.search( "ou=system", "(userCertificate=\\34\\56\\4E\\5F)", SearchScope.ONELEVEL ) )
            {
                assertTrue( cursor.next() );
                
                Entry entry = cursor.get();
                assertNotNull( entry );
                assertEquals( "cn=Kate Bush,ou=system", entry.getDn().getName() );
                
                assertFalse( cursor.next() );
            }
        }
    }


    @Test
    public void testSearchOperationalAttr() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            Map<String, Entry> entries = new HashMap<>();
            
            try ( EntryCursor cursor = connection.search( "ou=system", "(ou=testing01)", SearchScope.ONELEVEL, "+" ) )
            {
                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }
            }
    
            assertEquals( 1, entries.size(), "Expected number of results returned was incorrect!" );
    
            Entry entry = entries.get( "ou=testing01,ou=system" );
    
            assertNotNull( entry.get( "createTimestamp" ) );
            assertNotNull( entry.get( "creatorsName" ) );
            assertNull( entry.get( "objectClass" ) );
            assertNull( entry.get( "ou" ) );
        }
    }


    @Test
    public void testSearchUserAttr() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            Map<String, Entry> entries = new HashMap<>();
            
            try ( EntryCursor cursor = connection.search( "ou=system", "(ou=testing01)", SearchScope.ONELEVEL, "*" ) )
            {
                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }
            }
    
            assertEquals( 1, entries.size(), "Expected number of results returned was incorrect!" );
    
            Entry entry = entries.get( "ou=testing01,ou=system" );
    
            assertNotNull( entry.get( "objectClass" ) );
            assertNotNull( entry.get( "ou" ) );
            assertNull( entry.get( "createTimestamp" ) );
            assertNull( entry.get( "creatorsName" ) );
        }
    }


    @Test
    public void testSearchUserAttrAndOpAttr() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            Map<String, Entry> entries = new HashMap<>();
            
            try ( EntryCursor cursor = connection.search( "ou=system", "(ou=testing01)", SearchScope.ONELEVEL, "*",  "creatorsName" ) )
            {
                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }
            }
    
            assertEquals( 1, entries.size(), "Expected number of results returned was incorrect!" );
    
            Entry entry = entries.get( "ou=testing01,ou=system" );
    
            assertNotNull( entry.get( "objectClass" ) );
            assertNotNull( entry.get( "ou" ) );
            assertNotNull( entry.get( "creatorsName" ) );
            assertNull( entry.get( "createTimestamp" ) );
        }
    }


    @Test
    public void testSearchUserAttrAndNoAttr() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            Map<String, Entry> entries = new HashMap<>();
            
            try ( EntryCursor cursor = connection.search( "ou=system", "(ou=testing01)", SearchScope.ONELEVEL, "1.1", "ou" ) )
            {
                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }
            }
    
            assertEquals( 1, entries.size(), "Expected number of results returned was incorrect!" );
    
            Entry entry = entries.get( "ou=testing01,ou=system" );
    
            assertNull( entry.get( "objectClass" ) );
            assertNotNull( entry.get( "ou" ) );
            assertNull( entry.get( "creatorsName" ) );
            assertNull( entry.get( "createTimestamp" ) );
        }
    }


    @Test
    public void testSearchNoAttr() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            Map<String, Entry> entries = new HashMap<>();
            
            try ( EntryCursor cursor = connection.search( "ou=system", "(ou=testing01)", SearchScope.ONELEVEL, "1.1" ) )
            {
                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }
            }

            assertEquals( 1, entries.size(), "Expected number of results returned was incorrect!" );
    
            Entry entry = entries.get( "ou=testing01,ou=system" );
    
            assertNull( entry.get( "objectClass" ) );
            assertNull( entry.get( "ou" ) );
            assertNull( entry.get( "creatorsName" ) );
            assertNull( entry.get( "createTimestamp" ) );
        }
    }


    @Test
    public void testSearchAllAttr() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            Map<String, Entry> entries = new HashMap<>();
            
            try ( EntryCursor cursor = connection.search( "ou=system", "(ou=testing01)", SearchScope.ONELEVEL, "+", "*" ) )
            {
                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }
            }
    
            assertEquals( 1, entries.size(), "Expected number of results returned was incorrect!" );
    
            Entry entry = entries.get( "ou=testing01,ou=system" );
    
            assertNotNull( entry.get( "createTimestamp" ) );
            assertNotNull( entry.get( "creatorsName" ) );
            assertNotNull( entry.get( "objectClass" ) );
            assertNotNull( entry.get( "ou" ) );
        }
    }


    /**
     * Search an entry and fetch an attribute with unknown option
     * @throws Exception if there are errors
     */
    @Test
    @Disabled("We don't support options")
    public void testSearchFetchNonExistingAttributeOption() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            try ( EntryCursor cursor = connection.search( RDN + ",ou=system", FILTER, SearchScope.OBJECT, 
                "cn", "sn;unknownOption", "badAttr" ) )
            {
                if ( cursor.next() )
                {
                    Entry entry = cursor.get();
                    
                    assertNotNull( entry.get( "cn" ) );
                    assertEquals( "Heather Nova", entry.get( "cn" ).getString() );
                    assertNull( entry.get( "sn" ) );
                }
            }
        }
    }


    /**
     * Search an entry and fetch an attribute and all its subtypes
     * @throws Exception if there are errors
     */
    @Test
    public void testSearchFetchAttributeAndSubTypes() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            try ( EntryCursor cursor = connection.search( RDN + ",ou=system", FILTER, SearchScope.OBJECT, "name" ) )
            {
                assertTrue( cursor.next() );
                
                Entry entry = cursor.get(); 

                // We should have get cn and sn only
                assertEquals( 2, entry.size() );

                // Check CN
                Attribute cn = entry.get( "cn" );
                assertNotNull( cn );
                assertEquals( "Heather Nova", cn.get().toString() );

                // Assert SN
                Attribute sn = entry.get( "sn" );
                assertNotNull( sn );
                assertEquals( "Nova", sn.get().toString() );
                
                // No more entry expected
                assertFalse( cursor.next() );
            }
        }
    }


    /**
     * Search an entry and fetch an attribute with twice the same attributeType
     * @throws Exception if there are errors
     */
    @Test
    public void testSearchFetchTwiceSameAttribute() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            try ( EntryCursor cursor = connection.search( RDN + ",ou=system", FILTER, SearchScope.OBJECT, "cn", "cn" ) )
            {
                assertTrue( cursor.next() );
                
                Entry entry = cursor.get(); 
                
                // We should have get cn
                assertEquals( 1, entry.size() );

                // Check CN
                Attribute cn = entry.get( "cn" );
                assertNotNull( cn );
                assertEquals( "Heather Nova", cn.get().toString() );
                
                // No more entry expected
                assertFalse( cursor.next() );
            }
        }
    }


    // this one is failing because it returns the admin user twice: count = 15
    //    public void testFilterExpansion3() throws Exception
    //    {
    //        SearchControls controls = new SearchControls();
    //        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
    //        controls.setDerefLinkFlag( false );
    //        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, AliasDerefMode.NEVER_DEREF_ALIASES );
    //
    //        List entries = new ArrayList();
    //        NamingEnumeration list = sysRoot.search( "", "(name=*)", controls );
    //        while ( list.hasMore() )
    //        {
    //            SearchResult result = ( SearchResult ) list.next();
    //            entries.add( result.getName() );
    //        }
    //        assertEquals( "size of results", 14, entries.size() );
    //        assertTrue( "contains ou=testing00,ou=system", entries.contains( "ou=testing00,ou=system" ) );
    //        assertTrue( "contains ou=testing01,ou=system", entries.contains( "ou=testing01,ou=system" ) );
    //        assertTrue( "contains ou=testing02,ou=system", entries.contains( "ou=testing01,ou=system" ) );
    //        assertTrue( "contains uid=akarasulu,ou=users,ou=system", entries.contains( "uid=akarasulu,ou=users,ou=system" ) );
    //        assertTrue( "contains ou=configuration,ou=system", entries.contains( "ou=configuration,ou=system" ) );
    //        assertTrue( "contains ou=groups,ou=system", entries.contains( "ou=groups,ou=system" ) );
    //        assertTrue( "contains ou=interceptors,ou=configuration,ou=system", entries.contains( "ou=interceptors,ou=configuration,ou=system" ) );
    //        assertTrue( "contains ou=partitions,ou=configuration,ou=system", entries.contains( "ou=partitions,ou=configuration,ou=system" ) );
    //        assertTrue( "contains ou=services,ou=configuration,ou=system", entries.contains( "ou=services,ou=configuration,ou=system" ) );
    //        assertTrue( "contains ou=subtest,ou=testing01,ou=system", entries.contains( "ou=subtest,ou=testing01,ou=system" ) );
    //        assertTrue( "contains ou=system", entries.contains( "ou=system" ) );
    //        assertTrue( "contains ou=users,ou=system", entries.contains( "ou=users,ou=system" ) );
    //        assertTrue( "contains uid=admin,ou=system", entries.contains( "uid=admin,ou=system" ) );
    //        assertTrue( "contains cn=administrators,ou=groups,ou=system", entries.contains( "cn=administrators,ou=groups,ou=system" ) );
    //    }

    /**
     *  Convenience method that performs a one level search using the
     *  specified filter returning their DNs as Strings in a set.
     *
     * @param controls the search controls
     * @param filter the filter expression
     * @return the set of groups
     * @throws Exception if there are problems conducting the search
     */
    public Set<String> searchGroups( String filter, SearchScope scope ) throws Exception
    {
        try ( LdapConnection connection = new LdapCoreSessionConnection( classDirectoryService.getAdminSession() ) )
        {
            try ( EntryCursor cursor = connection.search( "ou=groups,ou=system", filter, scope ) )
            {
                Set<String> results = new HashSet<>();

                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    results.add( entry.getDn().getName() );
                }

                return results;
            }
        }
    }


    /**
     *  Convenience method that performs a one level search using the
     *  specified filter returning their DNs as Strings in a set.
     *
     * @param filter the filter expression
     * @return the set of group names
     * @throws Exception if there are problems conducting the search
     */
    public Set<String> searchGroups( String filter ) throws Exception
    {
        return searchGroups( filter, SearchScope.SUBTREE );
    }


    /**
     *  Convenience method that performs a one level search using the
     *  specified filter returning their DNs as Strings in a set.
     *
     * @param controls the search controls
     * @param filter the filter expression
     * @return the set of groups
     * @throws Exception if there are problems conducting the search
     */
    private Set<String> searchUnits( String filter, SearchScope scope ) throws Exception
    {
        try ( LdapConnection connection = new LdapCoreSessionConnection( classDirectoryService.getAdminSession() ) )
        {
            try ( EntryCursor cursor = connection.search( "ou=system", filter, scope ) )
            {
                Set<String> results = new HashSet<>();

                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    results.add( entry.getDn().getName() );
                }

                return results;
            }
        }
    }


    @Test
    public void testSetup() throws Exception
    {
        Set<String> results = searchGroups( "(objectClass=posixGroup)" );
        assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );
    }


    @Test
    public void testLessThanSearch() throws Exception
    {
        Set<String> results = searchGroups( "(testInt<=5)" );
        assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(testInt<=4)" );
        assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(testInt<=3)" );
        assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(testInt<=0)" );
        assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(testInt<=-1)" );
        assertFalse( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );
    }


    @Test
    public void testGreaterThanSearch() throws Exception
    {
        Set<String> results = searchGroups( "(testInt>=0)" );
        assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(testInt>=1)" );
        assertFalse( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(testInt>=3)" );
        assertFalse( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(testInt>=6)" );
        assertFalse( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );
    }


    @Test
    public void testNotOperator() throws Exception
    {
        Set<String> results = searchGroups( "(!(testInt=4))" );
        assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );
    }


    @Test
    public void testNotOperatorSubtree() throws Exception
    {
        Set<String> results = searchGroups( "(!(gidNumber=4))", SearchScope.SUBTREE );
        assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );
    }


    @Test
    public void testSearchWithEscapedCharsInFilter() throws Exception
    {
        try ( LdapConnection connection = new LdapCoreSessionConnection( classDirectoryService.getAdminSession() ) )
        {
            // Create entry cn=Sid Vicious, ou=system
            connection.add( new DefaultEntry( 
                "cn=Sid Vicious,ou=system",
                "objectClass: top", 
                "objectClass: person",
                "cn: Sid Vicious",
                "sn: Vicious", 
                "description: (sex*pis\\tols)"
                ) );
    
            Entry sid = connection.lookup( "cn=Sid Vicious,ou=system" );
            
            assertNotNull( sid );
            assertEquals( "(sex*pis\\tols)", sid.get( "description" ).getString() );
    
            // Now, search for the description
            try ( EntryCursor cursor = connection.search( "ou=system", "(description=\\28sex\\2Apis\\5Ctols\\29)", SearchScope.SUBTREE, "*" ) )
            {
                Map<String, Entry> results = new HashMap<>();

                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    results.put( entry.getDn().getName(), entry );
                }
    
                assertEquals( 1, results.size(), "Expected number of results returned was incorrect!" );
        
                Entry entry = results.get( "cn=Sid Vicious,ou=system" );
        
                assertNotNull( entry.get( "objectClass" ) );
                assertNotNull( entry.get( "cn" ) );
            }
        }
    }


    @Test
    public void testSubstringSearchWithEscapedCharsInFilter() throws Exception
    {
        try ( LdapConnection connection = new LdapCoreSessionConnection( classDirectoryService.getAdminSession() ) )
        {
            // Create entry cn=Sid Vicious, ou=system
            connection.add( new DefaultEntry( 
                "cn=Sid Vicious,ou=system",
                "objectClass: top", 
                "objectClass: person",
                "cn: Sid Vicious",
                "sn: Vicious", 
                "description: (sex*pis\\\\tols)"
                ) );
    
            Entry sid = connection.lookup( "cn=Sid Vicious,ou=system" );
            
            assertNotNull( sid );
            assertEquals( "(sex*pis\\\\tols)", sid.get( "description" ).getString() );


            // Now, search for the description
            try ( EntryCursor cursor = connection.search( "ou=system", 
                /*"(description=*\\28*)", "(description=*\\29*)", "(description=*\\2A*)",*/ "(description=*\\5C*)", 
                SearchScope.SUBTREE, "*" ) )
            {
                Map<String, Entry> results = new HashMap<>();

                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    results.put( entry.getDn().getName(), entry );
                }

                assertEquals( 1, results.size(), "Expected number of results returned was incorrect!" );
    
                Entry entry = results.get( "cn=Sid Vicious,ou=system" );
    
                assertNotNull( entry.get( "objectClass" ) );
                assertNotNull( entry.get( "cn" ) );
            }
        }
    }


    @Test
    public void testSubstringSearchWithEscapedAsterisksInFilter_DIRSERVER_1181() throws Exception
    {
        try ( LdapConnection connection = new LdapCoreSessionConnection( classDirectoryService.getAdminSession() ) )
        {
            connection.add( new DefaultEntry(
                "cn=x*y*z*,ou=system",
                "objectClass: top", 
                "objectClass: person", 
                "cn: x*y*z*",
                "sn: x*y*z*", 
                "description: (sex*pis\\tols)"
                ) );
        
            try ( EntryCursor cursor = connection.search( "ou=system", "(cn=*x\\2Ay\\2Az\\2A*)", SearchScope.ONELEVEL, "cn" ) )
            {
                Entry entry = null;

                while ( cursor.next() )
                {
                    if ( entry == null )
                    {
                        entry = cursor.get();
                    }
                    else
                    {
                        fail( "Cannot have 2 entries" );
                    }
                }

                assertNotNull( entry );
                assertEquals( "x*y*z*", entry.get( "cn" ).getString() );
            }

            try ( EntryCursor cursor = connection.search( "ou=system", "(cn=*x*y*z*)", SearchScope.ONELEVEL, "cn" ) )
            {
                Entry entry = null;

                while ( cursor.next() )
                {
                    if ( entry == null )
                    {
                        entry = cursor.get();
                    }
                    else
                    {
                        fail( "Cannot have 2 entries" );
                    }
                }

                assertNotNull( entry );
                assertEquals( "x*y*z*", entry.get( "cn" ).getString() );
            }
        }
    }


    /**
     * Test a search with a bad filter : there is a missing closing ')'
     */
    @Test
    public void testBadFilter() throws Exception
    {
        // With LDAP API 2.1.3
        //assertThrows( LdapInvalidSearchFilterException.class, () ->
        // With LDAP API 2.1.2
        assertThrows( LdapInvalidSearchFilterException.class, () ->
        {
            try ( LdapConnection connection = new LdapCoreSessionConnection( classDirectoryService.getAdminSession() ) )
            {
                try ( EntryCursor cursor = connection.search( "ou=system", "(|(name=testing00)(name=testing01)", SearchScope.SUBTREE ) )
                {
                    fail();
                }
            }
        } );
    }


    /**
     * Search operation with a base Dn with quotes
     * Commented as it's not valid by RFC 5514
    @Test
    public void testSearchWithQuotesInBase() throws Exception
    {
        LdapContext sysRoot = getSystemContext( getService() );
        createData( sysRoot );
    
        SearchControls ctls = new SearchControls();
        ctls.setSearchScope( SearchControls.OBJECT_SCOPE );
        String filter = "(cn=Tori Amos)";
        ctls.setReturningAttributes( new String[]
            { "cn", "cn" } );
    
        // Search for cn="Tori Amos" (with quotes)
        String base = "cn=\"Tori Amos\"";
    
        try {
            // Check entry
            NamingEnumeration<SearchResult> result = sysRoot.search( base, filter, ctls );
            assertTrue( result.hasMore() );
    
            while ( result.hasMore() )
            {
                SearchResult sr = result.next();
                Attributes attrs = sr.getAttributes();
                Attribute sn = attrs.get( "cn" );
                assertNotNull(sn);
                assertTrue( sn.contains( "Amos" ) );
            }
        } catch (Exception e)
        {
            fail( e.getMessage() );
        }
    }
    */

    /**
    * Added to test correct comparison of integer attribute types when searching.
    * testGreaterThanSearch and testLesserThanSearch fail to detect the problem using values less than 10.
    * Ref. DIRSERVER-1296
    *
    * @throws Exception
    */
    @Test
    public void testIntegerComparison() throws Exception
    {
        Set<String> results = searchUnits( "(&(objectClass=organizationalUnit)(integerAttribute<=2))", SearchScope.SUBTREE );
        assertTrue( results.contains( "ou=testing00,ou=system" ) );
        assertTrue( results.contains( "ou=testing01,ou=system" ) );
        assertTrue( results.contains( "ou=testing02,ou=system" ) );
        assertFalse( results.contains( "ou=testing03,ou=system" ) );
        assertFalse( results.contains( "ou=testing04,ou=system" ) );
        assertFalse( results.contains( "ou=testing05,ou=system" ) );
    }


    /**
     * Added to test correct comparison of integer attribute types when searching.
     * testGreaterThanSearch and testLesserThanSearch fail to detect the problem using values less than 10.
     * Ref. DIRSERVER-1296
     *
     * @throws Exception
     */
    @Test
    public void testIntegerComparison2() throws Exception
    {
        Set<String> results = searchUnits( "(&(objectClass=organizationalUnit)(integerAttribute>=3))", SearchScope.SUBTREE );
        assertFalse( results.contains( "ou=testing00,ou=system" ) );
        assertFalse( results.contains( "ou=testing01,ou=system" ) );
        assertFalse( results.contains( "ou=testing02,ou=system" ) );
        assertTrue( results.contains( "ou=testing03,ou=system" ) );
        assertTrue( results.contains( "ou=testing04,ou=system" ) );
        assertTrue( results.contains( "ou=testing05,ou=system" ) );
    }


    /**
     * Added to test correct comparison of integer attribute types when searching.
     * testGreaterThanSearch and testLesserThanSearch fail to detect the problem using values less than 10.
     * Ref. DIRSERVER-1296
     *
     * @throws Exception
     */
    @Test
    public void testIntegerComparison3() throws Exception
    {
        Set<String> results = searchUnits( "(&(objectClass=organizationalUnit)(integerAttribute<=42))", SearchScope.SUBTREE );
        assertTrue( results.contains( "ou=testing00,ou=system" ) );
        assertTrue( results.contains( "ou=testing01,ou=system" ) );
        assertTrue( results.contains( "ou=testing02,ou=system" ) );
        assertTrue( results.contains( "ou=testing03,ou=system" ) );
        assertTrue( results.contains( "ou=testing04,ou=system" ) );
        assertTrue( results.contains( "ou=testing05,ou=system" ) );
    }


    /**
     * Added to test correct comparison of integer attribute types when searching.
     * testGreaterThanSearch and testLesserThanSearch fail to detect the problem using values less than 10.
     * Ref. DIRSERVER-1296
     *
     * @throws Exception
     */
    @Test
    public void testIntegerComparison4() throws Exception
    {
        Set<String> results = searchUnits(
            "(&(objectClass=organizationalUnit)(|(integerAttribute<=1)(integerAttribute>=5)))", SearchScope.SUBTREE );
        assertTrue( results.contains( "ou=testing00,ou=system" ) );
        assertTrue( results.contains( "ou=testing01,ou=system" ) );
        assertFalse( results.contains( "ou=testing02,ou=system" ) );
        assertFalse( results.contains( "ou=testing03,ou=system" ) );
        assertFalse( results.contains( "ou=testing04,ou=system" ) );
        assertTrue( results.contains( "ou=testing05,ou=system" ) );
    }

    @Test
    public void testSearchTelephoneNumber() throws Exception
    {
        try ( LdapConnection connection = new LdapCoreSessionConnection( classDirectoryService.getAdminSession() ) )
        {
            try ( EntryCursor cursor = connection.search( "ou=system", "(telephoneNumber=18015551212)", SearchScope.ONELEVEL ) )
            {
                Map<String, Entry> entries = new HashMap<>();

                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }

                assertEquals( 2, entries.size(), "Expected number of results returned was incorrect!" );
                assertTrue( entries.containsKey( "cn=Heather Nova, ou=system" ) || entries.containsKey( "cn=Heather Nova,ou=system" ) );
            }
        }
    }


    @Test
    public void testSearchDN() throws Exception
    {
        try ( LdapConnection connection = new LdapCoreSessionConnection( classDirectoryService.getAdminSession() ) )
        {
            try ( EntryCursor cursor = connection.search( "ou=system", "(manager=cn=Heather Nova, ou=system)", SearchScope.SUBTREE ) )
            {
                Map<String, Entry> entries = new HashMap<>();

                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }
        
                assertEquals( 1, entries.size(), "Expected number of results returned was incorrect!" );
                assertTrue( entries.containsKey( "cn=with-dn, ou=system" ) || entries.containsKey( "cn=with-dn,ou=system" ) );
            }
        }
    }


    @Test
    public void testComplexFilter() throws Exception
    {
        try ( LdapConnection connection = new LdapCoreSessionConnection( classDirectoryService.getAdminSession() ) )
        {
            String filter = "(|(&(|(2.5.4.0=posixgroup)(2.5.4.0=groupofuniquenames)(2.5.4.0=groupofnames)(2.5.4.0=group))(!(|(2.5.4.50=uid=admin,ou=system)(2.5.4.31=0.9.2342.19200300.100.1.1=admin,2.5.4.11=system))))(objectClass=referral))";

            // Create an entry which does not match
            connection.add( new DefaultEntry( 
                "cn=testGroup3,ou=groups,ou=system",
                "objectClass: top", 
                "objectClass: groupOfUniqueNames",
                "cn: testGroup3", 
                "uniqueMember: uid=admin,ou=system" ) );

            try ( EntryCursor cursor = connection.search( "ou=system", filter, SearchScope.SUBTREE ) )
            {
                Map<String, Entry> entries = new HashMap<>();

                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }

                assertEquals( 5, entries.size(), "size of results" );
                assertTrue( entries.containsKey( "cn=testGroup0,ou=groups,ou=system" ) );
                assertTrue( entries.containsKey( "cn=testGroup1,ou=groups,ou=system" ) );
                assertTrue( entries.containsKey( "cn=testGroup2,ou=groups,ou=system" ) );
                assertTrue( entries.containsKey( "cn=testGroup4,ou=groups,ou=system" ) );
                assertTrue( entries.containsKey( "cn=testGroup5,ou=groups,ou=system" ) );
                assertFalse( entries.containsKey( "cn=testGroup3,ou=groups,ou=system" ) );
            }
        }
    }
    


    /**
     *  NO attributes should be returned
     */
    @Test
    public void testSearchTypesOnlyAndNoAttr() throws Exception
    {
        try ( LdapConnection connection = new LdapCoreSessionConnection( classDirectoryService.getAdminSession() ) )
        {
            SearchRequest searchRequest = new SearchRequestImpl();
            searchRequest.setBase( new Dn( "ou=system" ) );
            searchRequest.setFilter( "(ou=testing01)" );
            searchRequest.setScope( SearchScope.ONELEVEL );
            searchRequest.setTypesOnly( true );
            searchRequest.addAttributes( "1.1" );
            
            try ( SearchCursor cursor = connection.search( searchRequest ) )
            {
                Map<String, Entry> entries = new HashMap<>();

                while ( cursor.next() )
                {
                    Entry entry = cursor.getEntry(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }
        
                assertEquals( 1, entries.size(), "Expected number of results returned was incorrect!" );
        
                Entry entry = entries.get( "ou=testing01,ou=system" );
        
                assertEquals( 0, entry.size() );
            }
        }
    }


    /**
     * operational attributes with no values must be returned
     */
    @Test
    public void testSearchTypesOnlyWithNoAttrAndOperationalAttr() throws Exception
    {
        try ( LdapConnection connection = new LdapCoreSessionConnection( classDirectoryService.getAdminSession() ) )
        {
            SearchRequest searchRequest = new SearchRequestImpl();
            searchRequest.setBase( new Dn( "ou=system" ) );
            searchRequest.setFilter( "(ou=testing01)" );
            searchRequest.setScope( SearchScope.ONELEVEL );
            searchRequest.setTypesOnly( true );
            searchRequest.addAttributes( "1.1", "+" );
            
            try ( SearchCursor cursor = connection.search( searchRequest ) )
            {
                Map<String, Entry> entries = new HashMap<>();

                while ( cursor.next() )
                {
                    Entry entry = cursor.getEntry(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }

                assertEquals( 1, entries.size(), "Expected number of results returned was incorrect!" );
        
                Entry entry = entries.get( "ou=testing01,ou=system" );
        
                assertNotNull( entry.get( SchemaConstants.ENTRY_UUID_AT ) );
                assertNotNull( entry.get( SchemaConstants.CREATORS_NAME_AT ) );
        
                assertEquals( 0, entry.get( SchemaConstants.ENTRY_UUID_AT ).size() );
                assertEquals( 0, entry.get( SchemaConstants.CREATORS_NAME_AT ).size() );
            }
        }
    }


    /**
     * all user attributes with no values must be returned
     */
    @Test
    public void testSearchTypesOnlyWithNullReturnAttr() throws Exception
    {
        try ( LdapConnection connection = new LdapCoreSessionConnection( classDirectoryService.getAdminSession() ) )
        {
            SearchRequest searchRequest = new SearchRequestImpl();
            searchRequest.setBase( new Dn( "ou=system" ) );
            searchRequest.setFilter( "(ou=testing01)" );
            searchRequest.setScope( SearchScope.ONELEVEL );
            searchRequest.setTypesOnly( true );
            
            try ( SearchCursor cursor = connection.search( searchRequest ) )
            {
                Map<String, Entry> entries = new HashMap<>();

                while ( cursor.next() )
                {
                    Entry entry = cursor.getEntry(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }
        
                assertEquals( 1, entries.size(), "Expected number of results returned was incorrect!" );
        
                Entry entry = entries.get( "ou=testing01,ou=system" );
        
                assertNotNull( entry.get( SchemaConstants.OU_AT ) );
                assertNotNull( entry.get( "integerAttribute" ) );
        
                assertEquals( 0, entry.get( SchemaConstants.OU_AT ).size() );
                assertEquals( 0, entry.get( "integerAttribute" ).size() );
        
                assertNull( entry.get( SchemaConstants.ENTRY_UUID_AT ) );
                assertNull( entry.get( SchemaConstants.CREATORS_NAME_AT ) );
            }
        }
    }


    @Test
    public void testSearchEmptyDNWithOneLevelScope() throws Exception
    {
        try ( LdapConnection connection = new LdapCoreSessionConnection( classDirectoryService.getAdminSession() ) )
        {
            try ( EntryCursor cursor = connection.search( "", "(objectClass=*)", SearchScope.ONELEVEL ) )
            {
                Map<String, Entry> entries = new HashMap<>();

                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }
        
                assertEquals( 2, entries.size() );
                assertTrue( entries.containsKey( "ou=system" ) );
                assertTrue( entries.containsKey( "ou=schema" ) );
            }
        }
    }


    @Test
    public void testSearchEmptyDNWithSubLevelScope() throws Exception
    {
        try ( LdapConnection connection = new LdapCoreSessionConnection( classDirectoryService.getAdminSession() ) )
        {
            try ( EntryCursor cursor = connection.search( "", "(objectClass=organizationalUnit)", SearchScope.SUBTREE ) )
            {
                Map<String, Entry> entries = new HashMap<>();

                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }
        
                assertTrue( entries.size() > 2 );
                assertTrue( entries.containsKey( "ou=system" ) );
                assertTrue( entries.containsKey( "ou=schema" ) );
            }
        }
    }


    @Test
    public void testSearchEmptyDNWithObjectScopeAndNoObjectClassPresenceFilter() throws Exception
    {
        try ( LdapConnection connection = new LdapCoreSessionConnection( classDirectoryService.getAdminSession() ) )
        {
            try ( EntryCursor cursor = connection.search( "", "(objectClass=domain)", SearchScope.OBJECT ) )
            {
                Map<String, Entry> entries = new HashMap<>();

                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }

                assertEquals( 0, entries.size() );
        
                assertFalse( entries.containsKey( "ou=system" ) );
                assertFalse( entries.containsKey( "ou=schema" ) );
            }
        }
    }


    @Test
    public void testSearchRootDSE() throws Exception
    {
        try ( LdapConnection connection = new LdapCoreSessionConnection( classDirectoryService.getAdminSession() ) )
        {
            try ( EntryCursor cursor = connection.search( "", "(objectClass=*)", SearchScope.OBJECT, "*", "+" ) )
            {
                Entry rootDse = null;

                while ( cursor.next() )
                {
                    if ( rootDse == null )
                    {
                        rootDse = cursor.get();
                    }
                    else
                    {
                        fail( "Cannot have 2 root DSE" );
                    }
                }

                assertNotNull( rootDse );
        
                assertEquals( 10, rootDse.size() );
                assertNotNull( rootDse.get( "objectClass" ) );
                assertNotNull( rootDse.get( "entryUUID" ) );
                assertNotNull( rootDse.get( "namingContexts" ) );
                assertNotNull( rootDse.get( "subschemaSubentry" ) );
                assertNotNull( rootDse.get( "supportedControl" ) );
                assertNotNull( rootDse.get( "supportedExtension" ) );
                assertNotNull( rootDse.get( "supportedFeatures" ) );
                assertNotNull( rootDse.get( "supportedLDAPVersion" ) );
                assertNotNull( rootDse.get( "vendorName" ) );
                assertNotNull( rootDse.get( "vendorVersion" ) );
            }
        }
    }


    @Test
    public void testSearchEmptyDNWithOneLevelScopeAndNoObjectClassPresenceFilter() throws Exception
    {
        try ( LdapConnection connection = new LdapCoreSessionConnection( classDirectoryService.getAdminSession() ) )
        {
            try ( EntryCursor cursor = connection.search( "", "(cn=*)", SearchScope.ONELEVEL ) )
            {
                Map<String, Entry> entries = new HashMap<>();

                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }

                assertEquals( 0, entries.size() );
                assertFalse( entries.containsKey( "ou=system" ) );
                assertFalse( entries.containsKey( "ou=schema" ) );
            }
        }
    }


    @Test
    public void testCsnLessEqualitySearch() throws Exception
    {
        try ( LdapConnection connection = new LdapCoreSessionConnection( classDirectoryService.getAdminSession() ) )
        {
            Dn dn = new Dn( "cn=testLowerCsnAdd,ou=system" );
            
            Entry entry = new DefaultEntry( 
                dn,
                "objectClass", SchemaConstants.PERSON_OC,
                "cn", "testLowerCsnAdd_cn",
                "sn", "testLowerCsnAdd_sn" );
    
            connection.add( entry );
    
            // add an entry to have a entry with higher CSN value
            Dn dn2 = new Dn( "cn=testHigherCsnAdd,ou=system" );
            Entry entry2 = new DefaultEntry( 
                dn2,
                "objectClass: person",
                "cn: testHigherCsnAdd_cn",
                "sn: testHigherCsnAdd_sn" );
    
            connection.add( entry2 );
    
            entry = connection.lookup( dn.getName(), "+" );
            entry2 = connection.lookup( dn2.getName(), "+" );
    
            String lowerCsn = entry.get( "entryCsn" ).getString();
            String higherCsn = entry2.get( "entryCsn" ).getString();
    
            // usecases
            // 1.1 Less than or Equal ( with the lower csn value)
            testUseCases( lowerCsn, new String[]
                { lowerCsn }, connection, 1 );
    
            // 1.2 Less than or equals with a highest csn value
            testUseCases( higherCsn, new String[]
                { higherCsn, lowerCsn }, connection, 1 );
    
            // 2.1 Greater than or Equal ( with the highest csn value )
            testUseCases( higherCsn, new String[]
                { higherCsn }, connection, 2 );
    
            // 2.2 Greater than or Equal ( with lower csn value )
            testUseCases( lowerCsn, new String[]
                { higherCsn, lowerCsn }, connection, 2 );
        }
    }


    private void testUseCases( String filterCsnVal, String[] expectedCsns, LdapConnection connection, int useCaseNum )
        throws Exception
    {
        Value val = new Value( filterCsnVal );
        AttributeType entryCsnAt = getService().getSchemaManager().getAttributeType( SchemaConstants.ENTRY_CSN_AT );
        ExprNode filter = null;

        if ( useCaseNum == 1 )
        {
            filter = new LessEqNode<>( entryCsnAt, val );
        }
        else if ( useCaseNum == 2 )
        {
            filter = new GreaterEqNode<>( entryCsnAt, val );
        }

        Entry loadedEntry = null;

        Set<String> csnSet = new HashSet<>( expectedCsns.length );
        
        try ( EntryCursor cursor = connection.search( "ou=system", filter.toString(), SearchScope.ONELEVEL, "*", "+" ) )
        {
            while ( cursor.next() )
            {
                loadedEntry = cursor.get();
                csnSet.add( loadedEntry.get( SchemaConstants.ENTRY_CSN_AT ).getString() );
            }
        }
        
        assertTrue( csnSet.size() >= expectedCsns.length );

        for ( String csn : expectedCsns )
        {
            assertTrue( csnSet.contains( csn ) );
        }
    }


    @Test
    public void testSearchFilterWithBadAttributeType() throws Exception
    {
        try ( LdapConnection con = new LdapCoreSessionConnection( classDirectoryService.getAdminSession() ) )
        {
            try ( EntryCursor cursor = con.search( "ou=system", "(|(badAttr=testing00)(ou=testing01))", SearchScope.ONELEVEL ) )
            {
                Map<String, Entry> entries = new HashMap<>();

                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }
    
                assertEquals( 1, entries.size(), "Expected number of results returned was incorrect!" );
                assertTrue( entries.containsKey( "ou=testing01,ou=system" ) );
            }
        }
    }


    @Test
    public void testSearchFilterBadAttributeType() throws Exception
    {
        try ( LdapConnection con = new LdapCoreSessionConnection( classDirectoryService.getAdminSession() ) )
        {
            try ( EntryCursor cursor = con.search( "ou=system", "(badAttr=*)", SearchScope.ONELEVEL ) )
            {
                Map<String, Entry> entries = new HashMap<>();

                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }
                
                assertEquals( 0, entries.size(), "Expected number of results returned was incorrect!" );
            }
        }
    }


    @Test
    public void testSearchRootDSESubtree() throws Exception
    {
        try ( LdapConnection con = new LdapCoreSessionConnection( classDirectoryService.getAdminSession() ) )
        {
            try ( EntryCursor cursor = con.search( "", "(ou=testing01)", SearchScope.SUBTREE, "+", "*" ) )
            {
                Entry rootDse = null;

                while ( cursor.next() )
                {
                    if ( rootDse == null )
                    {
                        rootDse = cursor.get();
                    }
                    else
                    {
                        fail( "Cannot have 2 root DSE" );
                    }
                }

                assertNotNull( rootDse );
            }
        }
    }


    @Test
    public void testSearchOrGidNumber() throws Exception
    {
        try ( LdapConnection con = new LdapCoreSessionConnection( classDirectoryService.getAdminSession() ) )
        {
            try ( EntryCursor cursor = con.search( "ou=system", 
                "(|(&(objectclass=posixGroup)(|(gidnumber=1)(gidnumber=1)))(objectClass=posixGroupp))", SearchScope.SUBTREE, "+", "*" ) )
            {
                Entry rootDse = null;

                while ( cursor.next() )
                {
                    if ( rootDse == null )
                    {
                        rootDse = cursor.get();
                    }
                    else
                    {
                        fail( "Cannot have 2 root DSE" );
                    }
                }

                assertNotNull( rootDse );
            }
        } 
    }
    
    
    /**
     * Test for DIRSERVER-1922
     */
    @Test
    public void testNotEvaluator() throws Exception
    {
        try ( LdapConnection con = new LdapCoreSessionConnection( classDirectoryService.getAdminSession() ) )
        {
            try ( EntryCursor cursor = con.search( "ou=groups,ou=system", "(!(gidNumber=1))", SearchScope.ONELEVEL, "+", "*" ) )
            {
                int count = 0;
        
                while ( cursor.next() )
                {
                    count++;
                }
                
                assertEquals( 5, count );
            }
        }
    }

    
    /**
     * DIRSERVER-1961
     *
     * @throws Exception
     */
    @Test
    public void testSearchRootDSEOneLevel() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( classDirectoryService ) )
        {
            try ( EntryCursor cursor = connection.search( "ou=schema", "(|(objectClass=*)(cn=x))", SearchScope.OBJECT, "*" ) )
            {
                assertTrue( cursor.next() );
            }

            try ( EntryCursor cursor = connection.search( "ou=schema", "(objectClass=person)", SearchScope.OBJECT, "*" ) )
            {
                assertFalse( cursor.next() );
            }

            try ( EntryCursor cursor = connection.search( "", "(objectClass=person)", SearchScope.ONELEVEL, "*" ) )
            {
                assertFalse( cursor.next() );
            }
        
            try ( EntryCursor cursor = connection.search( "", "(objectClass=person)", SearchScope.SUBTREE, "*" ) )
            {
                int count = 0;

                while( cursor.next() )
                {
                    count++;
                }
                
                assertEquals(3, count);
            }
        }
    }
    

    @Test
    public void testSearchHasSubordinates() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            Map<String, Entry> entries = new HashMap<>();
            
            try ( EntryCursor cursor = connection.search( "ou=testing01,ou=system", FILTER, SearchScope.OBJECT, "+", "*" ) )
            {
                while ( cursor.next() )
                {
                    Entry entry = cursor.get(); 
                    
                    entries.put( entry.getDn().getName(), entry );
                }
            }
    
            assertEquals( 1, entries.size(), "Expected number of results returned was incorrect!" );
    
            Entry entry = entries.get( "ou=testing01,ou=system" );
    
            assertNotNull( entry.get( "createTimestamp" ) );
            assertNotNull( entry.get( "creatorsName" ) );
            assertNotNull( entry.get( "objectClass" ) );
            assertNotNull( entry.get( "ou" ) );
            assertNotNull( entry.get( "hasSubordinates" ) );
            assertEquals( "TRUE", entry.get( "hasSubordinates" ).getString() );
        }
    }
}
