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

package org.apache.directory.server.core.factory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.junit.Test;


/**
 * Test the creation of a DS using a factory.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
@CreateDS( name = "classDS" )
public class DirectoryServiceAnnotationTest
{
    @Test
    public void testCreateDS() throws Exception
    {
        DirectoryService service = DSAnnotationProcessor.getDirectoryService();
        
        assertTrue( service.isStarted() );
        assertEquals( "classDS", service.getInstanceId() );
        
        service.shutdown();
        FileUtils.deleteDirectory( service.getWorkingDirectory() );
    }


    @Test
    @CreateDS( name = "methodDS" )
    public void testCreateMethodDS() throws Exception
    {
        DirectoryService service = DSAnnotationProcessor.getDirectoryService();
        
        assertTrue( service.isStarted() );
        assertEquals( "methodDS", service.getInstanceId() );
        
        service.shutdown();
        FileUtils.deleteDirectory( service.getWorkingDirectory() );
    }
    
    
    @Test
    @CreateDS( 
        name = "MethodDSWithPartition",
        partitions =
        {
            @CreatePartition(
                name = "example",
                suffix = "dc=example,dc=com",
                contextEntry = @ContextEntry( 
                    entryLdif =
                        "dn: dc=example,dc=com\n" +
                        "dc: example\n" +
                        "objectClass: top\n" +
                        "objectClass: domain\n\n" ),
                indexes = 
                {
                    @CreateIndex( attribute = "objectClass" ),
                    @CreateIndex( attribute = "dc" ),
                    @CreateIndex( attribute = "ou" ),
                } )
        } )
    public void testCreateMethodDSWithPartition() throws Exception
    {
        /*
        DirectoryService service = DSAnnotationProcessor.getDirectoryService();
        
        assertTrue( service.isStarted() );
        assertEquals( "methodDS", service.getInstanceId() );
        
        service.shutdown();
        FileUtils.deleteDirectory( service.getWorkingDirectory() );
        */
    }
}
