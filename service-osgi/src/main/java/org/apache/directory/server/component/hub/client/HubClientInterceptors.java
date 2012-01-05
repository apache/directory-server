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
package org.apache.directory.server.component.hub.client;


import java.util.List;

import org.apache.directory.server.component.hub.listener.AbstractHubListener;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HubClientInterceptors extends AbstractHubListener
{
    /** A logger for this class */
    private final Logger LOG = LoggerFactory.getLogger( HubClientInterceptors.class );

    /** BundleContext reference for OSGI Events. */
    public static BundleContext bundleContext;


    public List<Interceptor> getCoreInterceptors()
    {
        return null;
    }
}
