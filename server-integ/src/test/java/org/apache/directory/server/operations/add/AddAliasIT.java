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
package org.apache.directory.server.operations.add;


import static org.apache.directory.server.integ.ServerIntegrationUtils.getClientApiConnection;
import static org.junit.Assert.assertNotNull;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Integration tests for add operations on Alias.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS( 
    enableChangeLog = false,
    name = "DSAddAlias" )
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP") })
public class AddAliasIT extends AbstractLdapTestUnit
{
    private LdapConnection conn;

    @Test
    public void testAddAliasOnParent() throws Exception
    {
        conn = getClientApiConnection( getLdapServer() );
        conn.setTimeOut( -1L );
        
        conn.add( new DefaultEntry( 
            "cn=foo,ou=system", 
            "objectClass: person",
            "objectClass: top",
            "cn: foo",
            "sn: Foo" ) );

        assertNotNull( conn.lookup( "cn=foo,ou=system" ) );

        conn.add( new DefaultEntry( 
            "ou=alias,cn=foo,ou=system", 
            "objectClass: top",
            "objectClass: extensibleObject",
            "objectClass: alias",
            "ou: alias" ,
            "aliasedObjectName: cn=foo,ou=system",
            "description: alias to father (branch)" ) );

        assertNotNull( conn.lookup( "ou=alias,cn=foo,ou=system" ) );
    }
}
