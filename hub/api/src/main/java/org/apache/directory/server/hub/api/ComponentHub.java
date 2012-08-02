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

package org.apache.directory.server.hub.api;


import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.apache.directory.server.hub.api.component.DcConfiguration;
import org.apache.directory.server.hub.api.component.DirectoryComponent;
import org.apache.directory.server.hub.api.exception.HubAbortException;
import org.apache.directory.server.hub.api.exception.StoreNotValidException;
import org.apache.directory.server.hub.api.meta.DcMetadataDescriptor;
import org.apache.directory.server.hub.api.meta.DcOperationsManager;
import org.apache.directory.server.hub.api.registry.DcMetadataRegistry;
import org.apache.directory.server.hub.api.registry.DirectoryComponentRegistry;
import org.apache.directory.server.hub.api.registry.InjectionRegistry;
import org.apache.directory.server.hub.api.registry.PidHandlerRegistry;


public interface ComponentHub
{

    public abstract void init() throws StoreNotValidException;


    public abstract void connectHandler( DcMetadataDescriptor metadata, DcOperationsManager operationsManager )
        throws HubAbortException;


    public abstract void disconnectHandler( String handlerPID );


    public abstract void updateComponentName( DirectoryComponent component, String newPID ) throws HubAbortException;


    public abstract void updateComponent( DirectoryComponent component, DcConfiguration newConfiguration )
        throws HubAbortException;


    public abstract void addComponent( DirectoryComponent component ) throws HubAbortException;


    public abstract void removeComponent( DirectoryComponent component ) throws HubAbortException;


    public abstract void addInjection( String injectionType, Object injection );


    public abstract void removeInjection( String injectionType );


    public abstract void registerClient( ComponentListener hubClient, String type );


    public abstract void unregisterClient( ComponentListener hubClient, String type );


    public abstract DirectoryComponentRegistry getDCRegistry();


    public abstract DcMetadataRegistry getMetaRegistry();


    public abstract InjectionRegistry getInjectionRegistry();


    public abstract PidHandlerRegistry getPIDHandlerRegistry();

}