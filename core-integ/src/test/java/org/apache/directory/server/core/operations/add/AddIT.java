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
package org.apache.directory.server.core.operations.add;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test the add operation
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(
    name = "AddITDS",
    partitions =
        {
            @CreatePartition(
                name = "example",
                suffix = "dc=example,dc=com",
                contextEntry = @ContextEntry(
                    entryLdif =
                    "dn: dc=example,dc=com\n" +
                        "dc: example\n" +
                        "objectClass: top\n" +
                        "objectClass: domain\n\n"),
                indexes =
                    {
                        @CreateIndex(attribute = "objectClass"),
                        @CreateIndex(attribute = "sn"),
                        @CreateIndex(attribute = "cn")
                })

    },
    enableChangeLog = false)
public class AddIT extends AbstractLdapTestUnit
{
    /**
     * Test an add operation with a value that needs to be normalized
     */
    @Test
    public void testAddNotNormalized() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        Dn dn = new Dn( "cn=test,ou=system" );
        Entry entry = new DefaultEntry( dn,
            "ObjectClass: top",
            "ObjectClass: person",
            "sn:  TEST    ",
            "cn: test" );

        connection.add( entry );
        
        entry = connection.lookup( entry.getDn(), SchemaConstants.ALL_ATTRIBUTES_ARRAY );
        Entry contextEntry = connection.lookup( "ou=system", SchemaConstants.ALL_ATTRIBUTES_ARRAY );
        
        String expectedCsn = entry.get( SchemaConstants.ENTRY_CSN_AT ).getString();
        String contextCsn = contextEntry.get( SchemaConstants.CONTEXT_CSN_AT ).getString();
        assertEquals( expectedCsn, contextCsn );
    }


    /**
     * Test an add operation where an attribute with an Integer syntax has an value
     * above MAX_INTEGER.
     */
    @Test
    public void testAddIntegerTooBig() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        Dn dn = new Dn( "ads-directoryServiceId=test,ou=system" );
        Entry entry = new DefaultEntry( dn,
            "ObjectClass: top",
            "ObjectClass: ads-base",
            "ObjectClass: ads-directoryService",
            "ads-directoryServiceId: test",
            "ads-dsReplicaId: test",
            "ads-dsDenormalizeOpAttrsEnabled: TRUE",
            "ads-dsAccessControlEnabled: TRUE",
            "ads-dsAllowAnonymousAccess: TRUE",
            "ads-dsPasswordHidden: TRUE",
            "ads-dsSyncPeriodMillis: 2147483648"
            );

        connection.add( entry );

        entry = connection.lookup( dn );

        assertEquals( "2147483648", entry.get( "ads-dsSyncPeriodMillis" ).getString() );

        getService().shutdown();

        entry = connection.lookup( dn );

        assertNull( entry );

        getService().startup();

        entry = connection.lookup( dn );

        assertEquals( "2147483648", entry.get( "ads-dsSyncPeriodMillis" ).getString() );
    }


    /**
     * Test for DIRSERVER-2109.
     */
    @Test
    @Ignore
    public void test_DIRSERVER_2109_1() throws Exception
    {
        Dn dn = new Dn( "cn=\\#\\\\\\+\\, \\\"\u00F6\u00E9\\\",ou=users,ou=system" );
        Entry entry = new DefaultEntry( dn,
            "objectClass: top",
            "objectClass: person",
            "sn:  #\\+, \"\u00F6\u00E9\"    ",
            "cn: #\\+, \"\u00F6\u00E9\"" );

        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );
        connection.add( entry );

        entry = connection.lookup( entry.getDn(), SchemaConstants.ALL_USER_ATTRIBUTES );
        System.out.println( entry );
        assertEquals( 1, entry.get( "cn" ).size() );
        assertEquals( "#\\+, \"\u00F6\u00E9\"", entry.get( "cn" ).get().getString() );
    }


    /**
     * Test for DIRSERVER-2109.
     */
    @Test
    @Ignore
    public void test_DIRSERVER_2109_2() throws Exception
    {
        Entry entry = new DefaultEntry( new Dn( "cn=a\\\\b,ou=users,ou=system" ) );
        entry.add( "objectClass", "top", "person" );
        entry.add( "cn", "a\\b" );
        entry.add( "sn", "test" );

        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );
        connection.add( entry );

        entry = connection.lookup( entry.getDn(), SchemaConstants.ALL_USER_ATTRIBUTES );
        System.out.println( entry );
        assertEquals( 1, entry.get( "cn" ).size() );
        assertEquals( "a\\b", entry.get( "cn" ).get().getString() );
    }

}
