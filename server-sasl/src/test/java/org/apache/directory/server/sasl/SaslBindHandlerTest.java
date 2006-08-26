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

package org.apache.directory.server.sasl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;

import org.apache.directory.server.unit.AbstractServerTest;

/**
 * Tests {@link SaslBindHandler}.
 *
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev: 27 $, $Date: 2005-11-14 17:10:05 +0900 (Mon, 14 Nov 2005) $
 */
public class SaslBindHandlerTest extends AbstractServerTest
{
    private LdapContext ctx = null;

    public SaslBindHandlerTest()
    {
    }

    public void setUp() throws Exception
    {
        configuration.get
        configuration.setExtendedOperationHandlers( extendedHandlers );
        
        super.setUp();

        Hashtable env = new Hashtable();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://localhost:" + port + "/ou=system" );
        env.put( "java.naming.security.principal", "uid=admin,ou=system" );
        env.put( "java.naming.security.credentials", "secret" );
        env.put( "java.naming.security.authentication", "simple" );
        ctx = new InitialLdapContext( env, new Control[ 0 ] );
    }

    public void tearDown() throws Exception
    {
        ctx.close();
        ctx = null;
        super.tearDown();
    }
    
    public void testStartTLS() throws Exception
    {
        StartTlsResponse res =
            ( StartTlsResponse ) ctx.extendedOperation( new StartTlsRequest() );
        
        // Set the fake hostname verifier to pass the negotiation process.
        res.setHostnameVerifier( new HostnameVerifier()
        {
            public boolean verify( String arg0, SSLSession arg1 )
            {
                return true;
            }
    
        });

        SSLSocketFactory socketFactory = BogusSSLContextFactory.getInstance( false ).getSocketFactory();
        res.negotiate( socketFactory );
        
        res.close();
    }
}
