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
package org.apache.directory.server.core.operations.rename;

import java.util.HashSet;
import java.util.Set;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test the rename operation performances
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "RenamePerfDS",
    partitions =
    {
        @CreatePartition(
            name = "example",
            suffix = "dc=example,dc=com",
            contextEntry =
                @ContextEntry(
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
    enableChangeLog = true)
public class RenameIT extends AbstractLdapTestUnit
{
    @Test
    public void testRenameUperCaseRdn() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        String oldDn = "cn=test,ou=system";

        Dn dn = new Dn( oldDn );
        Entry entry = new DefaultEntry( getService().getSchemaManager(), dn,
            "ObjectClass: top",
            "ObjectClass: person",
            "sn: TEST",
            "cn: test0" );

        connection.add( entry );
        
        Entry original = connection.lookup( oldDn );
        
        assertNotNull( original );

        connection.rename( oldDn, "cn=TEST" );
        
        Entry renamed = connection.lookup( oldDn );
        
        assertNotNull( renamed );
        assertEquals( original.getDn(), renamed.getDn() );
        Attribute attribute = renamed.get( "cn" );
        Set<String> expected = new HashSet<String>();
        expected.add( "test0" );
        expected.add( "test" );
        int found = 0;
        
        for ( Value<?> value : attribute )
        {
            String val = value.getString();
            
            assertTrue( expected.contains( val ) );
            found++;
        }
        
        assertEquals( 2, found );
    }
}
