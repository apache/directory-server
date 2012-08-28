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

package org.apache.directory.shared.client.api;


import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.NetworkSchemaLoader;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.shared.util.exception.Exceptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * A test class for NetworkSchemaLoader.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP"), @CreateTransport(protocol = "LDAPS") })
public class NetworkSchemaLoaderIT extends AbstractLdapTestUnit
{
    private LdapNetworkConnection connection;


    @Before
    public void setup() throws Exception
    {
        connection = LdapApiIntegrationUtils.getPooledAdminConnection( getLdapServer() );
    }


    @After
    public void shutdown() throws Exception
    {
        LdapApiIntegrationUtils.releasePooledAdminConnection( connection, getLdapServer() );
    }


    @Test
    public void testNetworkSchemaLoader() throws Exception
    {
        NetworkSchemaLoader loader = new NetworkSchemaLoader( connection );

        SchemaManager sm = new DefaultSchemaManager( loader );

        boolean loaded = sm.loadAllEnabled();

        if ( !loaded )
        {
            fail( "Schema load failed : " + Exceptions.printErrors( sm.getErrors() ) );
        }

        assertTrue( sm.getRegistries().getAttributeTypeRegistry().contains( "cn" ) );
    }

}
