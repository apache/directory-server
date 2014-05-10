/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.integ;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapNoSuchObjectException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;


/**
 * Tests the CreateDsRule.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@CreateDS(name = "classDS",
    enableChangeLog = true,
    partitions =
        {
            @CreatePartition(
                name = "example",
                suffix = "dc=example,dc=com",
                contextEntry = @ContextEntry(
                    entryLdif =
                    "dn: dc=example,dc=com\n" +
                        "objectClass: domain\n" +
                        "objectClass: top\n" +
                        "dc: example\n\n"
                ),
                indexes =
                    {
                        @CreateIndex(attribute = "objectClass"),
                        @CreateIndex(attribute = "dc"),
                        @CreateIndex(attribute = "ou")
                }
            )
    })
@ApplyLdifs(
    {
        "dn: cn=class,ou=system",
        "objectClass: person",
        "cn: class",
        "sn: sn_class"
    })
public class TestCreateDsRule
{
    @ClassRule
    public static CreateDsRule classCreateDsRule = new CreateDsRule();

    public CreateDsRule createDsRule = new CreateDsRule( classCreateDsRule );
    @Rule
    public TestRule ruleChain = RuleChain.outerRule( createDsRule );


    @Test
    @ApplyLdifs(
        {
            "dn: cn=classDsOnly,ou=system",
            "objectClass: person",
            "cn: classDsOnly",
            "sn: sn_classDsOnly"
        })
    public void testClassDsOnly()
    {
        assertEquals( classCreateDsRule.getDirectoryService(), createDsRule.getDirectoryService() );
        try
        {
            Dn dn = new Dn( "cn=class,ou=system" );
            Entry entry = createDsRule.getDirectoryService().getAdminSession().lookup( dn );
            assertNotNull( entry );
            assertEquals( "class", entry.get( "cn" ).get().getValue() );

            dn = new Dn( "cn=classDsOnly,ou=system" );
            entry = createDsRule.getDirectoryService().getAdminSession().lookup( dn );
            assertNotNull( entry );
            assertEquals( "classDsOnly", entry.get( "cn" ).get().getValue() );
        }
        catch ( LdapException e )
        {
            fail( e.getMessage() );
        }
    }


    @Test
    @CreateDS(name = "methodDS",
        enableChangeLog = true)
    @ApplyLdifs(
        {
            "dn: cn=classAndMethodDs,ou=system",
            "objectClass: person",
            "cn: classAndMethodDs",
            "sn: sn_classAndMethodDs"
        })
    public void testClassAndMethodDs()
    {
        assertNotEquals( classCreateDsRule.getDirectoryService(), createDsRule.getDirectoryService() );
        try
        {
            Dn dn = new Dn( "cn=classAndMethodDs,ou=system" );
            Entry entry = createDsRule.getDirectoryService().getAdminSession().lookup( dn );
            assertNotNull( entry );
            assertEquals( "classAndMethodDs", entry.get( "cn" ).get().getValue() );
        }
        catch ( LdapException e )
        {
            fail( e.getMessage() );
        }

        try
        {
            Dn dn = new Dn( "cn=class,ou=system" );
            Entry entry = createDsRule.getDirectoryService().getAdminSession().lookup( dn );
            assertNull( entry );
        }
        catch ( LdapNoSuchObjectException e )
        {
            // expected
        }
        catch ( LdapException e ) 
        {
            fail( e.getClass().getName() );
        }

        try {
            Dn dn = new Dn( "cn=class,ou=system" );
            Entry entry = classCreateDsRule.getDirectoryService().getAdminSession().lookup( dn );
            assertNotNull( entry );
            assertEquals( "class", entry.get( "cn" ).get().getValue() );
        }
        catch ( LdapException e ) 
        {
            fail( e.getClass().getName() );
        }
    }
}
