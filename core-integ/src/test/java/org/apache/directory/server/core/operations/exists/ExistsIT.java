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
package org.apache.directory.server.core.operations.exists;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test the exists operation
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "ExistsIT")
@ApplyLdifs(
    {
        // Entry # 1
        "dn: cn=test,ou=system", 
        "objectClass: person", 
        "cn: test", 
        "sn: sn_test" 
    })
public class ExistsIT extends AbstractLdapTestUnit
{
    /** The ldap connection */
    private LdapConnection connection;


    @Before
    public void setup() throws Exception
    {
        connection = IntegrationUtils.getAdminConnection( getService() );
    }


    @After
    public void shutdown() throws Exception
    {
        connection.close();
    }


    /**
     * Test a exists( Dn ) operation
     */
    @Test
    public void testExists() throws Exception
    {
        assertTrue( connection.exists( "cn=test,ou=system" ) );
    }


    /**
     * Test a wrong exists( Dn ) operation
     */
    @Test
    public void testNotExists() throws Exception
    {
        assertFalse( connection.exists( "cn=test2,ou=system" ) );
    }
}
