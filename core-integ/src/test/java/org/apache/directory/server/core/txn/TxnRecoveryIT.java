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

package org.apache.directory.server.core.txn;


import static org.apache.directory.server.core.integ.IntegrationUtils.getSystemContext;
import static org.junit.Assert.*;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(FrameworkRunner.class)
@CreateDS(name = "MovePerfDS",
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
    enableChangeLog = false)
public class TxnRecoveryIT extends AbstractLdapTestUnit
{
    @Test
    public void testRecovery() throws Exception
    {
        getService().getTxnManager().setDoNotFlush();

        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        String oldDn = "cn=test,ou=system";

        try
        {
            Dn dn = new Dn( oldDn );
            Entry entry = new DefaultEntry( getService().getSchemaManager(), dn );
            entry.add( "ObjectClass", "top", "person" );
            entry.add( "sn", "TEST" );
            entry.add( "cn", "test" );

            // First add the entry
            connection.add( entry );

            // Do a move
            String newDn = "cn=test,ou=users,ou=system";
            String newSuperior = "ou=users,ou=system";
            connection.move( oldDn, newSuperior );

            // Do a modify
            LdapContext sysRoot = getSystemContext( getService() );
            Attributes attrs = new BasicAttributes( "telephoneNumber", "1 650 300 6088", true );
            sysRoot.modifyAttributes( "cn=test,ou=users", DirContext.ADD_ATTRIBUTE, attrs );

            //Restart the service
            getService().shutdown();
            getService().startup();

            Entry oldEntry = connection.lookup( oldDn );
            Entry newEntry = connection.lookup( newDn );

            assertTrue( oldEntry == null );
            assertTrue( newEntry != null );

            attrs = sysRoot.getAttributes( "cn=test,ou=users" );
            Attribute attr = attrs.get( "telephoneNumber" );
            assertNotNull( attr );
            assertTrue( attr.contains( "1 650 300 6088" ) );

        }
        catch ( Exception e )
        {
            e.printStackTrace();
            throw e;
        }
    }
}
