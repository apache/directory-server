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
package org.apache.directory.server;

import org.apache.directory.server.configuration.ApacheDS;
import org.apache.directory.server.core.authn.AuthenticationInterceptor;
import org.apache.directory.server.core.authn.Authenticator;
import org.apache.directory.server.core.authn.SimpleAuthenticator;
import org.apache.directory.server.core.authn.StrongAuthenticator;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.xbean.spring.context.FileSystemXmlApplicationContext;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;


/**
 * @version $Rev$ $Date$
 */
public class SpringServerTest
{
    /**
     * Test a default server.xml file 
     * @throws Exception
     */
    @Test
    public void testSpringServerDefault() throws Exception {
        ClassLoader classLoader = this.getClass().getClassLoader();
        URL configURL = classLoader.getResource( "server.xml" );

        File configF = new File( configURL.toURI() );
        ApplicationContext factory = new FileSystemXmlApplicationContext( configF.toURI().toURL().toString() );
        ApacheDS apacheDS = ( ApacheDS ) factory.getBean( "apacheDS" );
        File workingDirFile = new File( configF.getParentFile(), "work" );
        apacheDS.getDirectoryService().setWorkingDirectory( workingDirFile );
    }

    /**
     * Test a server.xml with Authenticator in the authenticationInterceptor
     */
    @Test
    public void testSpringServerAuthenticatorInAuthenticationInterceptor() throws Exception {
        ClassLoader classLoader = this.getClass().getClassLoader();
        URL configURL = classLoader.getResource( "serverAuthenticatorInAuthenticationInterceptor.xml" );

        File configF = new File( configURL.toURI() );
        ApplicationContext factory = new FileSystemXmlApplicationContext( configF.toURI().toURL().toString() );
        ApacheDS apacheDS = ( ApacheDS ) factory.getBean( "apacheDS" );
        File workingDirFile = new File( configF.getParentFile(), "work" );
        apacheDS.getDirectoryService().setWorkingDirectory( workingDirFile );
        
        List<Interceptor> interceptors = apacheDS.getDirectoryService().getInterceptors();
        
        Map<String, Interceptor> map = new HashMap<String, Interceptor>();
        
        for ( Interceptor interceptor:interceptors )
        {
            map.put( interceptor.getName(), interceptor );
        }
        
        Interceptor authentication = map.get( AuthenticationInterceptor.class.getName() );
        assertNotNull( authentication );
        Set<Authenticator> authenticators = ((AuthenticationInterceptor)authentication).getAuthenticators();
        assertNotNull( authenticators );
        assertEquals( 2, authenticators.size() );
        int count = 2;
        
        for ( Authenticator authenticator: authenticators )
        {
            if ( authenticator instanceof SimpleAuthenticator )
            {
                count--;
            }
            
            if ( authenticator instanceof StrongAuthenticator )
            {
                count--;
            }
        }
        
        assertEquals( 0, count );
    }
}
