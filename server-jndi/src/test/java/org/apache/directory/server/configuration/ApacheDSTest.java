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
package org.apache.directory.server.configuration;


import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * TODO ApacheDSTest.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ApacheDSTest
{
    private static final Logger LOG = LoggerFactory.getLogger( ApacheDSTest.class );

   
    @Test
    public void testBogus()
    {
        System.out.println( "TODO: Fix this the real test case and delete me!" );
    }

    
    /* 
    public void testLdifLoading() throws Exception
    {
        DirectoryService directoryService = new DefaultDirectoryService();
        directoryService.setDenormalizeOpAttrsEnabled( true );
        directoryService.setAllowAnonymousAccess( false );
        directoryService.setExitVmOnShutdown( false );

        JdbmPartition example = new JdbmPartition();
        example.setId( "example" );
        example.setSuffixDn( "dc=example,dc=com" );
        Dn contextDn = new Dn( "dc=example,dc=com" );
        contextDn.normalize( directoryService.getRegistries().getAttributeTypeRegistry().getNormalizerMapping() );
        ServerEntry contextEntry = new DefaultServerEntry( directoryService.getRegistries(), contextDn );
        contextEntry.add( "objectClass", "top", "domain" );
        contextEntry.add( "dc", "example" );
        example.setContextEntry( contextEntry );
        directoryService.addPartition( example );
        directoryService.startup();
        
        LdapServer ldapServer = new LdapServer();
        ldapServer.setDirectoryService( directoryService );
        ldapServer.setAllowAnonymousAccess( false );
        ldapServer.setSocketAcceptor( new SocketAcceptor( null ) );
        ldapServer.setEnabled( true );
        ldapServer.setIpPort( 20389 );

        ApacheDS ads = new ApacheDS( directoryService, ldapServer, null );
        File f = new File( System.getProperty( "ldifFile" ) );
        ads.setLdifDirectory( f );
        
        try
        {
            ads.startup();
        }
        catch ( Throwable t )
        {
            LOG.error( "Failed to start up ApacheDS!", t );
        }
        
        Dn dn = new Dn( "uid=aeinstein,ou=Users,dc=example,dc=com" );
        assertNotNull( directoryService.getAdminSession().lookup( dn ) );
    }
    */
}
