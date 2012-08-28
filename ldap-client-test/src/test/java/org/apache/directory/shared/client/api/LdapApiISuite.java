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
package org.apache.directory.shared.client.api;


import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.FrameworkSuite;
import org.apache.directory.shared.client.api.operations.ClientAbandonRequestIT;
import org.apache.directory.shared.client.api.operations.ClientAddRequestIT;
import org.apache.directory.shared.client.api.operations.ClientCompareRequestIT;
import org.apache.directory.shared.client.api.operations.ClientDeleteRequestIT;
import org.apache.directory.shared.client.api.operations.ClientExtendedRequestIT;
import org.apache.directory.shared.client.api.operations.ClientModifyDnRequestIT;
import org.apache.directory.shared.client.api.operations.ClientModifyRequestIT;
import org.apache.directory.shared.client.api.operations.bind.SimpleBindRequestIT;
import org.apache.directory.shared.client.api.operations.search.ClientSearchRequestIT;
import org.apache.directory.shared.client.api.operations.search.SearchRequestReturningAttributesIT;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;


/**
 * Stock (default configuration) server integration test suite.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkSuite.class)
@Suite.SuiteClasses(
    {
        LdapConnectionIT.class,
        LdapSSLConnectionIT.class,
        NetworkSchemaLoaderIT.class,

        ClientAbandonRequestIT.class,
        ClientAddRequestIT.class,
        ClientCompareRequestIT.class,
        ClientDeleteRequestIT.class,
        ClientExtendedRequestIT.class,
        ClientModifyDnRequestIT.class,
        ClientModifyRequestIT.class,

        SimpleBindRequestIT.class,

        ClientSearchRequestIT.class,
        SearchRequestReturningAttributesIT.class

    })
@CreateDS(
    name = "SuiteDS",
    allowAnonAccess = true)
@CreateLdapServer(
    allowAnonymousAccess = true,
    transports =
    {
        @CreateTransport(protocol = "LDAP"),
        @CreateTransport(protocol = "LDAPS")
    })
public class LdapApiISuite
{
}
