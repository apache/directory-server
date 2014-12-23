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
package org.apache.directory.server.osgi.integ;


import org.apache.directory.server.ldap.LdapProtocolUtils;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.server.ldap.LdapSessionManager;
import org.apache.directory.server.ldap.handlers.extended.StartTlsHandler;
import org.apache.directory.server.ldap.handlers.request.AddRequestHandler;
import org.apache.directory.server.ldap.handlers.request.SearchRequestHandler;
import org.apache.directory.server.ldap.handlers.response.SearchResultEntryHandler;
import org.apache.directory.server.ldap.handlers.sasl.gssapi.GssapiMechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.plain.PlainMechanismHandler;
import org.apache.directory.server.ldap.replication.consumer.ReplicationConsumerImpl;
import org.apache.directory.server.ldap.replication.provider.SyncReplRequestHandler;
import org.apache.mina.core.session.DummySession;


public class ServerProtocolLdapOsgiTest extends ServerOsgiTestBase
{

    @Override
    protected String getBundleName()
    {
        return "org.apache.directory.server.protocol.ldap";
    }


    @Override
    protected void useBundleClasses() throws Exception
    {
        LdapProtocolUtils.createCookie( 23, "foo" );
        new LdapSessionManager();
        new LdapSession( new DummySession() );
        new LdapServer();
        new AddRequestHandler();
        new SearchRequestHandler();
        new SearchResultEntryHandler();
        new StartTlsHandler();
        new PlainMechanismHandler();
        new GssapiMechanismHandler();
        new ReplicationConsumerImpl();
        new SyncReplRequestHandler();
    }

}
