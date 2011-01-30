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


import static org.apache.directory.server.integ.ServerIntegrationUtils.getClientApiConnection;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.directory.Attributes;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.message.Control;
import org.apache.directory.shared.ldap.model.message.SearchResultEntry;
import org.apache.directory.shared.ldap.util.JndiUtils;
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
public class LookupPerfIT extends AbstractLdapTestUnit
{
    public static LdapServer ldapServer;


    /**
     * Evaluate the lookup operation performances
     */
    @Test
    public void testLookupPerfAPI() throws Exception
    {
        LdapConnection connection = getClientApiConnection( ldapServer );

        Entry entry = connection.lookup( "uid=admin,ou=system" );
        assertNotNull( entry );
        assertTrue( entry instanceof SearchResultEntry);

        long t0 = System.currentTimeMillis();

        for ( int i = 0; i < 50; i++ )
        {
            for ( int j = 0; j < 10000; j++ )
            {
                entry = connection.lookup( "uid=admin,ou=system", "+" );
            }

            System.out.print( "." );
        }

        long t1 = System.currentTimeMillis();

        System.out.println( "Delta : " + ( t1 - t0 ) );
        connection.close();
    }


    public static LdapContext getWiredContext( LdapServer ldapServer, Control[] controls ) throws Exception
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, "ldap://localhost:" + ldapServer.getPort() );
        env.put( Context.SECURITY_PRINCIPAL, ServerDNConstants.ADMIN_SYSTEM_DN );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );

        return new InitialLdapContext( env, JndiUtils.toJndiControls( 
            ldapServer.getDirectoryService().getLdapCodecService(),
            controls ) );
    }


    /**
     * Evaluate the lookup operation performances
     */
    @Test
    public void testLookupPerfJNDI() throws Exception
    {
        LdapContext ctx = getWiredContext( ldapServer, null );

        Attributes result = ctx.getAttributes( "uid=admin,ou=system" );

        assertNotNull( result );

        long t0 = System.currentTimeMillis();

        for ( int i = 0; i < 50; i++ )
        {
            for ( int j = 0; j < 10000; j++ )
            {
                ctx.getAttributes( "uid=admin,ou=system" );
            }

            System.out.print( "." );
        }

        long t1 = System.currentTimeMillis();

        System.out.println( "Delta : " + ( t1 - t0 ) );

        ctx.close();
    }
}
