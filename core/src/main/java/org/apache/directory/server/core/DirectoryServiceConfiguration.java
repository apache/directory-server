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
package org.apache.directory.server.core;


import java.util.Hashtable;

import javax.naming.Context;

import org.apache.directory.server.core.configuration.StartupConfiguration;
import org.apache.directory.server.core.interceptor.InterceptorChain;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.core.schema.global.GlobalRegistries;


/**
 * Represents the global configuration of currently running
 * {@link DirectoryService}.  You can access all properties of
 * {@link DirectoryService} and get JNDI {@link Context}s it provides
 * via this interface.
 */
public interface DirectoryServiceConfiguration
{

    /**
     * Returns the {@link DirectoryService} for this configuration.
     */
    DirectoryService getService();


    /**
     * Returns the instance ID of the {@link DirectoryService}.
     */
    String getInstanceId();


    /**
     * Returns the listener that listens to service events.
     */
    DirectoryServiceListener getServiceListener();


    /**
     * Returns the initial context environment of the {@link DirectoryService}.
     */
    Hashtable getEnvironment();


    /**
     * Returns the startup configuration of the {@link DirectoryService}.
     */
    StartupConfiguration getStartupConfiguration();


    /**
     * Returns the registries for system schema objects of the {@link DirectoryService}.
     */
    GlobalRegistries getGlobalRegistries();


    /**
     * Returns the {@link PartitionNexus} of the {@link DirectoryService}
     * which bypasses the interceptor chain.
     */
    PartitionNexus getPartitionNexus();


    /**
     * Returns the interceptor chain of the {@link DirectoryService}.
     */
    InterceptorChain getInterceptorChain();


    /**
     * Returns <tt>true</tt> if this service is started
     * and bootstrap entries have been created for the first time.
     */
    boolean isFirstStart();
}
