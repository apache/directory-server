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

package org.apache.directory.server.factory;

import static org.junit.Assert.assertEquals;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.annotations.CreateKdcServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.factory.DSAnnotationProcessor;
import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.Test;

/**
 * Test the Kerberos Server annotation processing
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@CreateDS(name = "CreateKdcServerAnnotationTest-class")
@CreateKdcServer(primaryRealm = "apache.org",
                 kdcPrincipal = "krbtgt/apache.org@apache.org",
                 maxTicketLifetime = 1000,
                 maxRenewableLifetime = 2000,
                 transports = 
                 { 
                     @CreateTransport(protocol = "TCP"),
                     @CreateTransport(protocol = "UDP")
                 })
public class CreateKdcServerAnnotationIT
{
    @Test
    public void testCreateKdcServer() throws Exception
    {
        DirectoryService directoryService = DSAnnotationProcessor.getDirectoryService();
        
        assertEquals( "CreateKdcServerAnnotationTest-class", directoryService.getInstanceId() );
        
        KdcServer server = ServerAnnotationProcessor.getKdcServer( directoryService, AvailablePortFinder.getNextAvailable( 1024 ) );

        assertEquals( 2, server.getTransports().length );
        
        assertEquals( directoryService, server.getDirectoryService() );
        assertEquals( "apache.org", server.getPrimaryRealm() );
        assertEquals( "krbtgt/apache.org@apache.org", server.getServicePrincipal().getName() );
        assertEquals( 1000, server.getMaximumTicketLifetime() );
        assertEquals( 2000, server.getMaximumRenewableLifetime() );
        
        server.stop();
        directoryService.shutdown();
        
        FileUtils.deleteDirectory( directoryService.getInstanceLayout().getInstanceDirectory() );
    }
}
