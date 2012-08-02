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


import java.util.List;

import org.apache.directory.server.hub.api.component.DcConfiguration;
import org.apache.directory.server.hub.api.component.DirectoryComponent;
import org.apache.directory.server.hub.api.exception.HubStoreException;
import org.apache.directory.server.hub.api.exception.StoreNotValidException;
import org.apache.directory.server.hub.api.meta.DcMetadataDescriptor;



public interface HubStore
{

    public void init( ComponentHub hub ) throws StoreNotValidException;


    List<DcMetadataDescriptor> getMetadataDescriptors() throws HubStoreException;


    List<DirectoryComponent> getComponents() throws HubStoreException;


    void installMetadataDescriptor( DcMetadataDescriptor metadata ) throws HubStoreException;


    void updateMetadataDescriptor( DcMetadataDescriptor oldMetadata, DcMetadataDescriptor newMetadata )
        throws HubStoreException;


    void uninstallMetadataDescriptor( DcMetadataDescriptor metadata ) throws HubStoreException;


    void installComponent( DirectoryComponent component ) throws HubStoreException;


    void updateComponent( DirectoryComponent component, DcConfiguration newConfiguration ) throws HubStoreException;


    void uninstallComponent( DirectoryComponent component ) throws HubStoreException;
}
