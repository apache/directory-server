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
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


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

    
    /**
     * Test a server.xml with replicationInterceptor validated
     *
    @Test
    public void testSpringServerReplicationInterceptor() throws Exception {
        ClassLoader classLoader = this.getClass().getClassLoader();
        URL configURL = classLoader.getResource( "serverReplicationInterceptor.xml" );

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
        
        Interceptor interceptor = map.get( "replicationService" );
        assertNotNull( interceptor );
        ReplicationInterceptor replicationInterceptor = (ReplicationInterceptor)interceptor;
        assertNotNull( replicationInterceptor );
        assertNotNull( replicationInterceptor.getConfiguration() );
        
        SyncreplConfiguration config = replicationInterceptor.getConfiguration();
        assertEquals( 5, config.getLogMaxAge() );
        assertEquals( "instance_a", config.getReplicaId() );
        assertEquals( 2, config.getReplicationInterval() );
        assertEquals( 10, config.getResponseTimeout() );
        assertEquals( 10390, config.getServerPort() );
        assertNotNull( config.getPeerReplicas() );
        assertEquals( 2, config.getPeerReplicas().size() );
        
        Set<String> expectedReplicas = new HashSet<String>();
        
        expectedReplicas.add( "instance_b@localhost:1234" );
        expectedReplicas.add( "instance_c@localhost:1234" );
        
        for ( Replica replica:config.getPeerReplicas() )
        {
            String peer = replica.getId() + '@' + replica.getAddress().getHostName() + ':' + replica.getAddress().getPort();
            
            assert( expectedReplicas.contains( peer ) );
            expectedReplicas.remove( peer );
        }
        
        assertEquals( 0, expectedReplicas.size() );
    }

    
    /**
     * Test a server.xml for JdbmPartition configuration
     */
    @Test
    public void testSpringServerJdbmPartition() throws Exception {
        // NOTE : This test will only work on systems which have a /tmp temp dir
        File tmpDir = getTmpDir();
        if ( "/tmp".equals( tmpDir.getCanonicalPath() ) )
        {
            ClassLoader classLoader = this.getClass().getClassLoader();
            URL configURL = classLoader.getResource( "serverJdbmPartition.xml" );
    
            File configF = new File( configURL.toURI() );
            ApplicationContext factory = new FileSystemXmlApplicationContext( configF.toURI().toURL().toString() );
            ApacheDS apacheDS = ( ApacheDS ) factory.getBean( "apacheDS" );
            File workingDirFile = new File( configF.getParentFile(), "work" );
            apacheDS.getDirectoryService().setWorkingDirectory( workingDirFile );
            
            // Now, launch the server, and check that the ObjectClass index has been created in OS' tmp directory
            apacheDS.startup();
            
            File tmpOCdb = new File( tmpDir, "0.9.2342.19200300.100.1.25.db" );
            assertTrue( tmpOCdb.exists() );
    
            // if transactions are disabled there is no dc.lg file
            //File tmpOClg = new File( tmpDir, "dc.lg" );
            //assertTrue( tmpOClg.exists() );
            
            // Shutdown and cleanup
            apacheDS.shutdown();
            
            // Clean the /tmp/objectClass.* files
            tmpOCdb.delete();
            //tmpOClg.delete();
        }
        else
        {
            assertTrue( true );
        }
    }
    
    /**
     * Get tmp dir
     * 
     * @return
     *      The OS specific tmp dir
     */
    private File getTmpDir() throws IOException
    {
        return new File( System.getProperty( "java.io.tmpdir" ) );
    }
}
