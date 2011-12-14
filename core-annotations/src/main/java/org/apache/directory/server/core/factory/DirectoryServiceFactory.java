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
package org.apache.directory.server.core.factory;


import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.shared.partition.OperationExecutionManagerFactory;
import org.apache.directory.server.core.shared.txn.TxnManagerFactory;


/**
 * A factory used to generate differently configured DirectoryService objects.
 * Since the DirectoryService itself is what is configured then a factory for
 * these objects acts as a configurator.  Tests can provide different factory
 * methods to be used.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface DirectoryServiceFactory
{

    /**
     * Inits the directory service factory.
     * 
     * @param name the name
     */
    void init( String name ) throws Exception;


    /**
     * Gets the directory service.
     * 
     * @return the directory service
     */
    DirectoryService getDirectoryService() throws Exception;


    /**
     * Gets the partition factory.
     * 
     * @return the partition factory
     */
    PartitionFactory getPartitionFactory() throws Exception;
    
    
    TxnManagerFactory getTxnManagerFactory();
    
    
    OperationExecutionManagerFactory getOperationExecutionManagerFactory();
}
