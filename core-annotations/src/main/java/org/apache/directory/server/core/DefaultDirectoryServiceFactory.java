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
package org.apache.directory.server.core;


import org.apache.directory.server.core.builder.DirectoryServiceBuilder;


/**
 * A Default factory for DirectoryService
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DefaultDirectoryServiceFactory implements DirectoryServiceFactory
{
    private DirectoryService service;

    /**
     * The default factory returns stock instances of a directory
     * service with smart defaults
     */
    public static final DirectoryServiceFactory DEFAULT = new DefaultDirectoryServiceFactory();

    public void init( String name ) throws Exception
    {
        if ( ( service != null ) && (service.isStarted() ) )
        {
            return;
        }

        DirectoryServiceBuilder builder = new DirectoryServiceBuilder();
        builder.build( name );
        
        service = builder.getDirectoryService();
    }

    
    public DirectoryService getDirectoryService() throws Exception
    {
        return service;
    }
}
