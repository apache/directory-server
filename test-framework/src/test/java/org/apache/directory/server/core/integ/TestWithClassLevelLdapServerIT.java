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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(FrameworkRunner.class)
@CreateLdapServer(
    transports =
        {
            @CreateTransport(protocol = "LDAP")
    })
public class TestWithClassLevelLdapServerIT extends AbstractLdapTestUnit
{
    @Test
    @ApplyLdifFiles("test-entry.ldif")
    public void testWithApplyLdifFiles() throws Exception
    {
        assertTrue( getService().getAdminSession().exists( new Dn( "cn=testPerson1,ou=system" ) ) );

        if ( isRunInSuite )
        {
            assertTrue( getService().getAdminSession().exists( new Dn( "dc=example,dc=com" ) ) );
            // the SuiteDS is the name given to the DS instance in the enclosing TestSuite
            assertEquals( "SuiteDS", getLdapServer().getDirectoryService().getInstanceId() );
        }
        else
        // should run with a default DS created in FrameworkRunner
        {
            assertTrue( getLdapServer().getDirectoryService().getInstanceId().startsWith( "default" ) ); // after 'default' a UUID follows
        }

        assertTrue( getService().getAdminSession().exists( new Dn( "cn=testPerson2,ou=system" ) ) );

        assertNotNull( getLdapServer() );
    }
}
