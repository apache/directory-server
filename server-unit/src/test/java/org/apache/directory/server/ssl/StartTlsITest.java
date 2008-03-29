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
package org.apache.directory.server.ssl;


import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.directory.Attributes;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.apache.directory.server.unit.AbstractServerTest;
import org.apache.directory.shared.ldap.util.DummySSLSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Test case for StartTls.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class StartTlsITest extends AbstractServerTest
{
    private static final Logger LOG = LoggerFactory.getLogger( StartTlsITest.class );
    
    
    public void testStartTls() throws Exception
    {
//        // Set up environment for creating initial context
//        Hashtable<String, Object> env = new Hashtable<String,Object>();
//        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
//        env.put( "java.naming.ldap.factory.socket", DummySSLSocketFactory.class.getName() );
//        
//        // Must use the name of the server that is found in its certificate
//        env.put( Context.PROVIDER_URL, "ldap://localhost:" + port );
//
//        // Create initial context
//        LOG.error( "About to get initial context" );
//        LdapContext ctx = new InitialLdapContext( env, null );
//
//        // Start TLS
//        LOG.error( "About send startTls extended operation" );
//        StartTlsResponse tls = ( StartTlsResponse ) ctx.extendedOperation( new StartTlsRequest() );
//        LOG.error( "Extended operation issued" );
//        tls.setHostnameVerifier( new HostnameVerifier() {
//            public boolean verify( String hostname, SSLSession session )
//            {
//                return true;
//            } 
//        } );
//        LOG.error( "TLS negotion about to begin" );
//        SSLSession session = tls.negotiate( new DummySSLSocketFactory() );
//        
//        ctx.addToEnvironment( "java.naming.security.principal", "uid=admin,ou=system" );
//        ctx.addToEnvironment( "java.naming.security.credentials", "secret" );
//        ctx.addToEnvironment( "java.naming.security.authentication", "simple" );
//
//        Attributes attrs = ctx.getAttributes( "ou=system" ); 
//        System.out.println( attrs.toString() );
    }
}
