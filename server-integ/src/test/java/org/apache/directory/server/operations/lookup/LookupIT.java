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
package org.apache.directory.server.operations.lookup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.integ.ServerIntegrationUtils;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Testcase for the lookup operation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP") })
public class LookupIT extends AbstractLdapTestUnit
{
    /**
     * Evaluate the lookup operation when using a inherited Attribute
     */
    @Test
    public void testLookupPerfAPI() throws Exception
    {
        LdapConnection connection = ServerIntegrationUtils.getAdminConnection( getLdapServer() );

        Entry entry = connection.lookup( "uid=admin,ou=system", "name" );
        assertNotNull( entry );

        assertEquals( 2, entry.size() );
        assertTrue( entry.containsAttribute( "cn", "sn" ) );
        assertTrue( entry.contains( "cn", "system administrator" ) );
        assertTrue( entry.contains( "sn", "administrator" ) );

        connection.close();
    }
}
